/*
 * Reading a sudoku from a photo, entirely in the browser (no external library, works offline).
 *
 * Pipeline:
 *  1. loadImage()   - decodes the file (respecting the EXIF orientation of phone photos) into a canvas.
 *  2. detectGrid()  - finds the grid: after an adaptive threshold, the grid lines form the biggest connected
 *                     blob of dark pixels; its four extreme points are the corners of the grid.
 *                     The user can then move the corners by hand.
 *  3. readGrid()    - straightens the grid with a perspective transform (homography), splits it into 81 cells,
 *                     isolates the digit of every cell and recognizes it by comparing it with templates of the
 *                     digits 1-9 drawn in several fonts (nearest neighbour on normalized 28x28 images).
 *
 * Every cell also gets a confidence value, so the app can ask the user to double check the uncertain digits.
 */

const WARP_CELL = 48;              // size in pixels of a cell in the straightened grid
const WARP_SIZE = 9 * WARP_CELL;   // size of the straightened grid
const NORM = 28;                   // size of the normalized digit images
const NORM_BOX = 20;               // the digit is scaled to fit a NORM_BOX x NORM_BOX box inside the NORM image

// ---------------------------------------------------------------------------
//  Loading
// ---------------------------------------------------------------------------

/**
 * Decodes an image file into a canvas whose longest side is at most maxSize pixels.
 */
export async function loadImage(file, maxSize = 1400) {
  let source;
  try {
    source = await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch (e) {
    source = await new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error('unreadable image'));
      img.src = URL.createObjectURL(file);
    });
  }
  const w = source.width, h = source.height;
  const scale = Math.min(1, maxSize / Math.max(w, h));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(w * scale);
  canvas.height = Math.round(h * scale);
  canvas.getContext('2d').drawImage(source, 0, 0, canvas.width, canvas.height);
  return canvas;
}

// ---------------------------------------------------------------------------
//  Image helpers
// ---------------------------------------------------------------------------

/** @return the gray levels (0 = black, 255 = white) of a canvas as a Float32Array */
function grayOf(canvas) {
  const { width: w, height: h } = canvas;
  const data = canvas.getContext('2d', { willReadFrequently: true }).getImageData(0, 0, w, h).data;
  const gray = new Float32Array(w * h);
  for (let i = 0, p = 0; i < gray.length; i++, p += 4)
    gray[i] = 0.299 * data[p] + 0.587 * data[p + 1] + 0.114 * data[p + 2];
  return gray;
}

/**
 * Bradley adaptive threshold: a pixel is "ink" if it is darker than the mean of the window around it by more than
 * a fraction t. It works with uneven light and shadows, which are common in photos.
 * @return Uint8Array with 1 for ink pixels
 */
function adaptiveThreshold(gray, w, h, windowSize, t = 0.15) {
  const integral = new Float64Array((w + 1) * (h + 1));
  for (let y = 0; y < h; y++) {
    let rowSum = 0;
    for (let x = 0; x < w; x++) {
      rowSum += gray[y * w + x];
      integral[(y + 1) * (w + 1) + x + 1] = integral[y * (w + 1) + x + 1] + rowSum;
    }
  }
  const half = Math.max(1, Math.floor(windowSize / 2));
  const ink = new Uint8Array(w * h);
  for (let y = 0; y < h; y++) {
    const y0 = Math.max(0, y - half), y1 = Math.min(h - 1, y + half);
    for (let x = 0; x < w; x++) {
      const x0 = Math.max(0, x - half), x1 = Math.min(w - 1, x + half);
      const count = (x1 - x0 + 1) * (y1 - y0 + 1);
      const sum = integral[(y1 + 1) * (w + 1) + x1 + 1] - integral[y0 * (w + 1) + x1 + 1]
                - integral[(y1 + 1) * (w + 1) + x0] + integral[y0 * (w + 1) + x0];
      if (gray[y * w + x] * count <= sum * (1 - t)) ink[y * w + x] = 1;
    }
  }
  return ink;
}

/**
 * Labels the 8-connected components of the ink pixels inside the rectangle [x0,x1) x [y0,y1).
 * @return the list of components with pixel count and bounding box, and the label image
 */
function components(ink, w, x0, y0, x1, y1) {
  const labels = new Int32Array(ink.length).fill(-1);
  const result = [];
  const queue = new Int32Array((x1 - x0) * (y1 - y0));
  for (let y = y0; y < y1; y++)
    for (let x = x0; x < x1; x++) {
      const start = y * w + x;
      if (!ink[start] || labels[start] !== -1) continue;
      const id = result.length;
      const comp = { id, count: 0, minX: x, maxX: x, minY: y, maxY: y };
      let head = 0, tail = 0;
      queue[tail++] = start;
      labels[start] = id;
      while (head < tail) {
        const p = queue[head++], px = p % w, py = (p - px) / w;
        comp.count++;
        if (px < comp.minX) comp.minX = px;
        if (px > comp.maxX) comp.maxX = px;
        if (py < comp.minY) comp.minY = py;
        if (py > comp.maxY) comp.maxY = py;
        for (let dy = -1; dy <= 1; dy++)
          for (let dx = -1; dx <= 1; dx++) {
            const nx = px + dx, ny = py + dy;
            if (nx < x0 || nx >= x1 || ny < y0 || ny >= y1) continue;
            const q = ny * w + nx;
            if (ink[q] && labels[q] === -1) {
              labels[q] = id;
              queue[tail++] = q;
            }
          }
      }
      result.push(comp);
    }
  return { list: result, labels };
}

// ---------------------------------------------------------------------------
//  Grid detection
// ---------------------------------------------------------------------------

/**
 * Finds the four corners of the sudoku grid in the canvas.
 * @return {{corners: number[][], found: boolean}} corners in canvas coordinates: top-left, top-right,
 *         bottom-right, bottom-left. If no grid is found, a default square in the middle of the image is returned.
 */
export function detectGrid(canvas) {
  // work on a small copy: it is faster and the grid is big anyway
  const scale = Math.min(1, 600 / Math.max(canvas.width, canvas.height));
  const small = document.createElement('canvas');
  small.width = Math.max(1, Math.round(canvas.width * scale));
  small.height = Math.max(1, Math.round(canvas.height * scale));
  small.getContext('2d').drawImage(canvas, 0, 0, small.width, small.height);
  const w = small.width, h = small.height;
  const gray = grayOf(small);
  const ink = adaptiveThreshold(gray, w, h, Math.max(15, Math.round(Math.min(w, h) / 25)), 0.12);
  const { list, labels } = components(ink, w, 0, 0, w, h);

  // the grid: the component with the biggest bounding box that is roughly square and big enough
  let best = null, bestArea = 0;
  for (const c of list) {
    const bw = c.maxX - c.minX + 1, bh = c.maxY - c.minY + 1;
    if (bw < 0.25 * Math.min(w, h) || bh < 0.25 * Math.min(w, h)) continue;
    const ratio = bw / bh;
    if (ratio < 0.5 || ratio > 2) continue;
    if (c.count < 0.5 * (bw + bh)) continue; // too few pixels to be a grid
    if (bw * bh > bestArea) { bestArea = bw * bh; best = c; }
  }
  if (!best) {
    const m = 0.1;
    return {
      found: false,
      corners: [[m * canvas.width, m * canvas.height], [(1 - m) * canvas.width, m * canvas.height],
                [(1 - m) * canvas.width, (1 - m) * canvas.height], [m * canvas.width, (1 - m) * canvas.height]],
    };
  }
  // corners = extreme points of the blob along the two diagonals
  let tl = null, tr = null, br = null, bl = null;
  let minSum = Infinity, maxSum = -Infinity, minDiff = Infinity, maxDiff = -Infinity;
  for (let y = best.minY; y <= best.maxY; y++)
    for (let x = best.minX; x <= best.maxX; x++) {
      if (labels[y * w + x] !== best.id) continue;
      const s = x + y, d = x - y;
      if (s < minSum) { minSum = s; tl = [x, y]; }
      if (s > maxSum) { maxSum = s; br = [x, y]; }
      if (d > maxDiff) { maxDiff = d; tr = [x, y]; }
      if (d < minDiff) { minDiff = d; bl = [x, y]; }
    }
  const back = (p) => [(p[0] + 0.5) / scale, (p[1] + 0.5) / scale];
  return { found: true, corners: [back(tl), back(tr), back(br), back(bl)] };
}

// ---------------------------------------------------------------------------
//  Perspective transform
// ---------------------------------------------------------------------------

/**
 * Computes the homography that maps the four points src[i] to dst[i] (solving an 8x8 linear system).
 * @return the 9 coefficients (the last one is 1)
 */
export function homography(src, dst) {
  const a = [], b = [];
  for (let i = 0; i < 4; i++) {
    const [x, y] = src[i], [u, v] = dst[i];
    a.push([x, y, 1, 0, 0, 0, -u * x, -u * y]); b.push(u);
    a.push([0, 0, 0, x, y, 1, -v * x, -v * y]); b.push(v);
  }
  // Gaussian elimination with partial pivoting
  for (let col = 0; col < 8; col++) {
    let pivot = col;
    for (let r = col + 1; r < 8; r++) if (Math.abs(a[r][col]) > Math.abs(a[pivot][col])) pivot = r;
    [a[col], a[pivot]] = [a[pivot], a[col]];
    [b[col], b[pivot]] = [b[pivot], b[col]];
    for (let r = 0; r < 8; r++) {
      if (r === col || a[col][col] === 0) continue;
      const f = a[r][col] / a[col][col];
      for (let k = col; k < 8; k++) a[r][k] -= f * a[col][k];
      b[r] -= f * b[col];
    }
  }
  const h = b.map((v, i) => v / a[i][i]);
  h.push(1);
  return h;
}

/** Applies a homography to the point (x,y). */
export function project(h, x, y) {
  const w = h[6] * x + h[7] * y + h[8];
  return [(h[0] * x + h[1] * y + h[2]) / w, (h[3] * x + h[4] * y + h[5]) / w];
}

/**
 * Straightens the quadrilateral {@code corners} of the canvas into a WARP_SIZE x WARP_SIZE gray image
 * (bilinear interpolation).
 */
function warp(canvas, corners) {
  const w = canvas.width, h = canvas.height;
  const gray = grayOf(canvas);
  const square = [[0, 0], [WARP_SIZE, 0], [WARP_SIZE, WARP_SIZE], [0, WARP_SIZE]];
  const H = homography(square, corners); // from the straight grid to the photo
  const out = new Float32Array(WARP_SIZE * WARP_SIZE);
  for (let v = 0; v < WARP_SIZE; v++)
    for (let u = 0; u < WARP_SIZE; u++) {
      let [x, y] = project(H, u + 0.5, v + 0.5);
      x -= 0.5; y -= 0.5;
      const x0 = Math.floor(x), y0 = Math.floor(y);
      if (x0 < 0 || y0 < 0 || x0 >= w - 1 || y0 >= h - 1) { out[v * WARP_SIZE + u] = 255; continue; }
      const fx = x - x0, fy = y - y0, i = y0 * w + x0;
      out[v * WARP_SIZE + u] = gray[i] * (1 - fx) * (1 - fy) + gray[i + 1] * fx * (1 - fy)
                             + gray[i + w] * (1 - fx) * fy + gray[i + w + 1] * fx * fy;
    }
  return out;
}

// ---------------------------------------------------------------------------
//  Digit recognition
// ---------------------------------------------------------------------------

/**
 * Turns the ink pixels of a bounding box into a normalized NORM x NORM image: the digit is scaled (keeping its
 * proportions) to fit a NORM_BOX box, centred, blurred a little and normalized to zero mean and unit length,
 * so that two images can be compared with a dot product.
 * @param isInk function (x,y) -> boolean
 */
function normalize(isInk, minX, minY, maxX, maxY) {
  const bw = maxX - minX + 1, bh = maxY - minY + 1;
  const scale = NORM_BOX / Math.max(bw, bh);
  const tw = bw * scale, th = bh * scale;
  const ox = (NORM - tw) / 2, oy = (NORM - th) / 2;
  const img = new Float32Array(NORM * NORM);
  const S = 3; // supersampling
  for (let ty = 0; ty < NORM; ty++)
    for (let tx = 0; tx < NORM; tx++) {
      let hits = 0;
      for (let sy = 0; sy < S; sy++)
        for (let sx = 0; sx < S; sx++) {
          const fx = (tx + (sx + 0.5) / S - ox) / scale, fy = (ty + (sy + 0.5) / S - oy) / scale;
          if (fx < 0 || fy < 0 || fx >= bw || fy >= bh) continue;
          if (isInk(minX + Math.floor(fx), minY + Math.floor(fy))) hits++;
        }
      img[ty * NORM + tx] = hits / (S * S);
    }
  // 3x3 blur: makes the comparison tolerant to small shifts and to the thickness of the strokes
  const blurred = new Float32Array(NORM * NORM);
  for (let y = 0; y < NORM; y++)
    for (let x = 0; x < NORM; x++) {
      let s = 0, n = 0;
      for (let dy = -1; dy <= 1; dy++)
        for (let dx = -1; dx <= 1; dx++) {
          const xx = x + dx, yy = y + dy;
          if (xx < 0 || yy < 0 || xx >= NORM || yy >= NORM) continue;
          s += img[yy * NORM + xx]; n++;
        }
      blurred[y * NORM + x] = s / n;
    }
  let mean = 0;
  for (const v of blurred) mean += v;
  mean /= blurred.length;
  let norm = 0;
  for (let i = 0; i < blurred.length; i++) { blurred[i] -= mean; norm += blurred[i] * blurred[i]; }
  norm = Math.sqrt(norm) || 1;
  for (let i = 0; i < blurred.length; i++) blurred[i] /= norm;
  // the aspect ratio is a strong hint (a "1" is narrow): it is kept as an extra feature
  return { img: blurred, aspect: bw / bh };
}

const TEMPLATE_FONTS = [
  'Arial', 'Helvetica', 'Verdana', 'Tahoma', 'Trebuchet MS', 'Times New Roman', 'Georgia',
  'Courier New', 'Roboto', 'Noto Sans', 'DejaVu Sans', 'Liberation Sans', 'San Francisco', 'Segoe UI',
  'sans-serif', 'serif', 'monospace',
];
let templates = null;

/**
 * Draws the digits 1-9 in several fonts, weights and small rotations and normalizes them: these are the
 * examples the digits of the photo are compared with. Built once, the first time a photo is read.
 */
function buildTemplates() {
  const size = 96;
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = size;
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  const list = [];
  const seen = new Set();
  for (const font of TEMPLATE_FONTS)
    for (const weight of ['400', '700'])
      for (const angle of [-4, 0, 4])
        for (let d = 1; d <= 9; d++) {
          ctx.setTransform(1, 0, 0, 1, 0, 0);
          ctx.fillStyle = '#fff';
          ctx.fillRect(0, 0, size, size);
          ctx.translate(size / 2, size / 2);
          ctx.rotate(angle * Math.PI / 180);
          ctx.fillStyle = '#000';
          ctx.font = `${weight} 64px "${font}"`;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText(String(d), 0, 0);
          const data = ctx.getImageData(0, 0, size, size).data;
          let minX = size, minY = size, maxX = -1, maxY = -1;
          const ink = new Uint8Array(size * size);
          for (let i = 0; i < size * size; i++)
            if (data[i * 4] < 128) {
              ink[i] = 1;
              const x = i % size, y = (i - x) / size;
              if (x < minX) minX = x; if (x > maxX) maxX = x;
              if (y < minY) minY = y; if (y > maxY) maxY = y;
            }
          if (maxX < 0) continue;
          // fonts that are not installed fall back to the same default font: skip the duplicates
          let key = d + ':';
          for (let i = 0; i < ink.length; i += 7) key += ink[i];
          if (seen.has(key)) continue;
          seen.add(key);
          const n = normalize((x, y) => ink[y * size + x] === 1, minX, minY, maxX, maxY);
          list.push({ digit: d, ...n });
        }
  return list;
}

/**
 * Recognizes a normalized digit.
 * @return {{digit: number, confidence: number}} confidence is the margin between the best digit and the second best
 */
function classify(sample) {
  if (!templates) templates = buildTemplates();
  const best = new Array(10).fill(-Infinity);
  for (const t of templates) {
    let dot = 0;
    const a = sample.img, b = t.img;
    for (let i = 0; i < a.length; i++) dot += a[i] * b[i];
    // penalty for a different aspect ratio
    dot -= 0.6 * Math.abs(Math.log(sample.aspect / t.aspect));
    if (dot > best[t.digit]) best[t.digit] = dot;
  }
  let first = 1, second = 0;
  for (let d = 2; d <= 9; d++) {
    if (best[d] > best[first]) { second = first; first = d; }
    else if (second === 0 || best[d] > best[second]) second = d;
  }
  return { digit: first, confidence: best[first] - best[second], score: best[first] };
}

/**
 * Reads the 81 digits of the grid delimited by {@code corners}.
 * @return {{digits: number[], confidence: number[], image: string}} digits (0 = empty cell), confidence of each
 *         digit (1 for empty cells) and the straightened grid as a data URL (shown behind the board for checking)
 */
export function readGrid(canvas, corners) {
  const gray = warp(canvas, corners);
  const W = WARP_SIZE;
  const ink = adaptiveThreshold(gray, W, W, Math.round(WARP_CELL * 1.2), 0.18);
  const digits = new Array(81).fill(0), confidence = new Array(81).fill(1);
  const margin = Math.round(WARP_CELL * 0.12);
  const inner = WARP_CELL - 2 * margin;
  for (let r = 0; r < 9; r++)
    for (let c = 0; c < 9; c++) {
      const x0 = c * WARP_CELL + margin, y0 = r * WARP_CELL + margin;
      const { list } = components(ink, W, x0, y0, x0 + inner, y0 + inner);
      // the digit: the biggest blob of a reasonable size near the centre of the cell (not a piece of a grid line)
      let digit = null;
      for (const comp of list) {
        const bw = comp.maxX - comp.minX + 1, bh = comp.maxY - comp.minY + 1;
        if (bh < 0.3 * inner || bh > 0.98 * inner || bw > 0.9 * inner) continue;
        if (comp.count < 0.025 * inner * inner) continue;
        const cx = (comp.minX + comp.maxX) / 2 - x0, cy = (comp.minY + comp.maxY) / 2 - y0;
        if (Math.abs(cx - inner / 2) > 0.3 * inner || Math.abs(cy - inner / 2) > 0.3 * inner) continue;
        const touches = comp.minX === x0 || comp.minY === y0 || comp.maxX === x0 + inner - 1 || comp.maxY === y0 + inner - 1;
        if (touches && (bw < 0.12 * inner || bh < 0.12 * inner)) continue; // thin line along the border
        if (!digit || comp.count > digit.count) digit = comp;
      }
      if (!digit) continue;
      // also take the small pieces that overlap the digit (a digit printed with thin strokes can break apart)
      let { minX, minY, maxX, maxY } = digit;
      for (const comp of list)
        if (comp !== digit && comp.count >= 4 && comp.minX <= maxX + 2 && comp.maxX >= minX - 2
            && comp.minY <= maxY + 2 && comp.maxY >= minY - 2) {
          minX = Math.min(minX, comp.minX); maxX = Math.max(maxX, comp.maxX);
          minY = Math.min(minY, comp.minY); maxY = Math.max(maxY, comp.maxY);
        }
      const sample = normalize((x, y) => ink[y * W + x] === 1, minX, minY, maxX, maxY);
      const result = classify(sample);
      digits[r * 9 + c] = result.digit;
      confidence[r * 9 + c] = result.confidence;
    }
  // the straightened grid as an image
  const out = document.createElement('canvas');
  out.width = out.height = W;
  const octx = out.getContext('2d');
  const imageData = octx.createImageData(W, W);
  for (let i = 0; i < gray.length; i++) {
    const g = Math.max(0, Math.min(255, gray[i]));
    imageData.data[i * 4] = imageData.data[i * 4 + 1] = imageData.data[i * 4 + 2] = g;
    imageData.data[i * 4 + 3] = 255;
  }
  octx.putImageData(imageData, 0, 0);
  return { digits, confidence, image: out.toDataURL('image/jpeg', 0.85) };
}

// ---------------------------------------------------------------------------
//  Text files
// ---------------------------------------------------------------------------

/**
 * Reads a sudoku from a text file. Accepted formats: 81 characters on one line, or 9 lines of 9 characters,
 * with digits for the givens and 0 . - _ * x for the empty cells; separator lines (+---+, |, =) are ignored.
 * If the file contains several puzzles, the first one is used.
 * @return an array of 81 digits, or null if the file does not contain a sudoku
 */
export function parseText(text) {
  const cells = [];
  for (let line of text.split(/\r?\n/)) {
    line = line.trim();
    if (!line || /^[-+=|\s]+$/.test(line)) continue; // separator line
    if (/^[#;]/.test(line)) continue;                  // comment
    for (const ch of line) {
      if (ch >= '1' && ch <= '9') cells.push(Number(ch));
      else if ('0._*xX'.includes(ch)) cells.push(0);
      else if (ch === '-' && !line.includes('+')) cells.push(0);
      if (cells.length === 81) return cells;
    }
  }
  return null;
}
