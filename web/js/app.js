/*
 * Sudoku Solver - user interface.
 *
 * Three ways to enter a sudoku:
 *   1. take a photo (live camera, or the camera app of the phone as a fallback);
 *   2. upload a photo or a text file;
 *   3. type it in the grid (tap a cell + number pad, or the keyboard; 81 digits can also be pasted).
 * After a photo, the user can adjust the four corners of the grid, then checks the digits that have been read.
 *
 * The sudoku is solved by the Java solver compiled to JavaScript (js/solver-worker.js), which first checks that the
 * solution is unique and then solves it with human techniques: every step of its log can be replayed on the grid.
 */
import { loadImage, detectGrid, readGrid, homography, project, parseText } from './ocr.js';

// ---------------------------------------------------------------------------
//  Texts (English / Italian)
// ---------------------------------------------------------------------------

const TEXT = {
  en: {
    install: 'Install app', takePhoto: 'Take a photo', upload: 'Upload file or photo', manual: 'Enter manually',
    clear: 'Clear', edit: 'Edit', solve: 'Solve', steps: 'Solution steps', allSteps: 'All steps',
    comparePhoto: 'Show the photo under the grid', cancel: 'Cancel', gallery: 'Gallery',
    footnote: 'The solver runs on your device: your photos never leave it.',
    cameraHint: 'Frame the sudoku inside the square', cropHint: 'Drag the four corners onto the corners of the grid',
    readDigits: 'Read digits', solving: 'Solving…', reading: 'Reading the photo…', opening: 'Opening the image…',
    manualTitle: 'Enter the sudoku',
    manualText: 'Tap a cell and choose a digit. On a computer you can also type the digits, move with the arrows and paste 81 digits.',
    reviewTitle: 'Check the digits read from the photo',
    reviewText: (n) => (n ? `${n} uncertain digit${n > 1 ? 's are' : ' is'} highlighted in yellow. ` : '')
      + 'Correct any mistake, then press Solve.',
    reviewNoSolution: 'With these digits the sudoku has no solution: some digit was probably read wrong.',
    reviewMultiple: 'With these digits the sudoku has more than one solution: some digit is probably missing.',
    gridNotFound: 'The grid was not found automatically: move the corners by hand.',
    noCamera: 'The camera is not available: choose a photo instead.',
    fileError: 'This file does not contain a sudoku.', imageError: 'This image cannot be read.',
    textLoaded: 'Sudoku loaded from the file.',
    conflictsTitle: 'Repeated digits', conflicts: 'Some digits are repeated in the same row, column or box (in red).',
    emptyTitle: 'Empty grid', empty: 'Enter the digits of the sudoku first.',
    solvedTitle: 'Solved!', solvedText: (n, s) => `${n} steps, ${s} s.`,
    solvedLogic: 'Only logical techniques were needed.',
    solvedNet: 'A forcing net was needed (an assumption tested with every technique).',
    solvedTrial: 'Trial and error was needed.',
    unsolvedTitle: 'Not completed', unsolved: 'The techniques were not enough: the grid shows how far the solver got.',
    multipleTitle: 'More than one solution',
    multiple: 'A proper sudoku has exactly one solution, so it cannot be solved by logic. Check the digits.',
    noneTitle: 'No solution', none: 'This sudoku cannot be solved. Check the digits.',
    invalidTitle: 'Invalid sudoku', invalid: 'The grid is not valid.',
    errorTitle: 'Error', error: 'Something went wrong while solving.',
    stepCounter: (i, n) => `Step ${i} / ${n}`, start: 'Starting grid', startText: 'Use the arrows to follow the solution step by step.',
    solutionView: 'Solution', solutionText: 'All the steps applied.',
    iosInstall: 'To install: tap the Share button, then "Add to Home Screen".',
    offlineReady: 'Ready to work offline.',
  },
  it: {
    install: 'Installa app', takePhoto: 'Scatta una foto', upload: 'Carica file o foto', manual: 'Inserisci a mano',
    clear: 'Cancella', edit: 'Modifica', solve: 'Risolvi', steps: 'Passaggi della soluzione', allSteps: 'Tutti i passaggi',
    comparePhoto: 'Mostra la foto sotto la griglia', cancel: 'Annulla', gallery: 'Galleria',
    footnote: 'Il risolutore funziona sul tuo dispositivo: le foto non vengono inviate a nessuno.',
    cameraHint: 'Inquadra il sudoku dentro il quadrato', cropHint: 'Trascina i quattro angoli sugli angoli della griglia',
    readDigits: 'Leggi le cifre', solving: 'Risoluzione in corso…', reading: 'Lettura della foto…', opening: 'Apertura dell\'immagine…',
    manualTitle: 'Inserisci il sudoku',
    manualText: 'Tocca una cella e scegli una cifra. Dal computer puoi anche scrivere le cifre, spostarti con le frecce e incollare 81 cifre.',
    reviewTitle: 'Controlla le cifre lette dalla foto',
    reviewText: (n) => (n ? `${n === 1 ? 'Una cifra incerta è evidenziata' : n + ' cifre incerte sono evidenziate'} in giallo. ` : '')
      + 'Correggi eventuali errori, poi premi Risolvi.',
    reviewNoSolution: 'Con queste cifre il sudoku non ha soluzione: probabilmente qualche cifra è stata letta male.',
    reviewMultiple: 'Con queste cifre il sudoku ha più soluzioni: probabilmente manca qualche cifra.',
    gridNotFound: 'Non ho trovato la griglia automaticamente: sposta gli angoli a mano.',
    noCamera: 'La fotocamera non è disponibile: scegli una foto.',
    fileError: 'Questo file non contiene un sudoku.', imageError: 'Questa immagine non si può leggere.',
    textLoaded: 'Sudoku caricato dal file.',
    conflictsTitle: 'Cifre ripetute', conflicts: 'Alcune cifre sono ripetute nella stessa riga, colonna o riquadro (in rosso).',
    emptyTitle: 'Griglia vuota', empty: 'Inserisci prima le cifre del sudoku.',
    solvedTitle: 'Risolto!', solvedText: (n, s) => `${n} passaggi, ${s} s.`,
    solvedLogic: 'Sono bastate le tecniche logiche.',
    solvedNet: 'È servita una forcing net (un\'ipotesi verificata con tutte le tecniche).',
    solvedTrial: 'Sono serviti dei tentativi.',
    unsolvedTitle: 'Non completato', unsolved: 'Le tecniche non sono bastate: la griglia mostra fin dove è arrivato il risolutore.',
    multipleTitle: 'Più di una soluzione',
    multiple: 'Un sudoku corretto ha una sola soluzione, quindi non si può risolvere con la logica. Controlla le cifre.',
    noneTitle: 'Nessuna soluzione', none: 'Questo sudoku non si può risolvere. Controlla le cifre.',
    invalidTitle: 'Sudoku non valido', invalid: 'La griglia non è valida.',
    errorTitle: 'Errore', error: 'Qualcosa è andato storto durante la risoluzione.',
    stepCounter: (i, n) => `Passo ${i} / ${n}`, start: 'Griglia iniziale', startText: 'Usa le frecce per seguire la soluzione passo per passo.',
    solutionView: 'Soluzione', solutionText: 'Tutti i passaggi applicati.',
    iosInstall: 'Per installarla: tocca il pulsante Condividi, poi "Aggiungi alla schermata Home".',
    offlineReady: 'Pronta per funzionare offline.',
  },
};

let lang = 'en';
try { lang = localStorage.getItem('lang') || ''; } catch (e) { /* storage not available */ }
if (!TEXT[lang]) lang = (navigator.language || 'en').toLowerCase().startsWith('it') ? 'it' : 'en';
const t = (key, ...args) => {
  const value = TEXT[lang][key] ?? TEXT.en[key];
  return typeof value === 'function' ? value(...args) : value;
};

// ---------------------------------------------------------------------------
//  Technique names (as written in the log of the solver) in order of difficulty
// ---------------------------------------------------------------------------

const TECHNIQUES = [
  ['hidden single', 'Hidden Single'], ['naked single', 'Naked Single'], ['locked candidates', 'Locked Candidates'],
  ['naked subset', 'Naked Subset'], ['subset', 'Subset'], ['x-wing', 'X-Wing / Swordfish / Jellyfish'],
  ['unique rectangle', 'Unique Rectangle'], ['remote pairs', 'Remote Pairs'], ['skyscraper', 'Skyscraper'],
  ['two-string kite', 'Two-String Kite'], ['finned fish', 'Finned / Sashimi Fish'], ['simple coloring', 'Simple Coloring'],
  ['w-wing', 'W-Wing'], ['xy-wing', 'XY-Wing'], ['xyz-wing', 'XYZ-Wing'], ['multi-coloring', 'Multi-Coloring'],
  ['x-cycle', 'X-Cycle'], ['XY-Chain', 'XY-Chain'], ['aic', 'AIC'], ['als-xz', 'ALS-XZ'],
  ['forcing chains', 'Forcing Chains'], ['forcing net', 'Forcing Net'], ['trial and error', 'Trial and Error'],
];
const TECH_NAME = new Map(TECHNIQUES);
const TECH_ORDER = new Map(TECHNIQUES.map(([k], i) => [k, i]));

// ---------------------------------------------------------------------------
//  State
// ---------------------------------------------------------------------------

const state = {
  givens: new Array(81).fill(0), // the sudoku entered by the user
  selected: 40,
  view: 'edit',                  // 'edit' or 'solved'
  result: null,                  // answer of the solver
  steps: [],                     // grouped steps
  grids: [],                     // grids[i] = grid after the first i steps
  stepIndex: 0,                  // step shown (steps.length = solution)
  uncertain: new Set(),          // cells read from a photo with low confidence
  photo: null,                   // straightened photo of the grid (data URL)
};

const $ = (id) => document.getElementById(id);
const els = {
  board: $('board'), numpad: $('numpad'), status: $('status'), solve: $('solve-btn'), clear: $('clear-btn'),
  edit: $('edit-btn'), stepsCard: $('steps-card'), chips: $('technique-chips'), counter: $('step-counter'),
  current: $('step-current'), list: $('steps-list'), photoToggle: $('photo-toggle'), photoCheck: $('photo-check'),
  busy: $('busy'), busyText: $('busy-text'), toast: $('toast'),
};
const cells = [];

// ---------------------------------------------------------------------------
//  Helpers
// ---------------------------------------------------------------------------

const rowOf = (p) => Math.floor(p / 9), colOf = (p) => p % 9;
const boxOf = (p) => Math.floor(rowOf(p) / 3) * 3 + Math.floor(colOf(p) / 3);
const rc = (p) => `r${rowOf(p) + 1}c${colOf(p) + 1}`;
const sees = (a, b) => a !== b && (rowOf(a) === rowOf(b) || colOf(a) === colOf(b) || boxOf(a) === boxOf(b));
/** position of the i-th cell of a house of the solver ("row", "column" or "box" with index h) */
function housePosition(type, h, i) {
  if (type === 'row') return h * 9 + i;
  if (type === 'column') return i * 9 + h;
  return (Math.floor(h / 3) * 3 + Math.floor(i / 3)) * 9 + (h % 3) * 3 + (i % 3);
}
function save() {
  try { localStorage.setItem('grid', state.givens.join('')); } catch (e) { /* ignore */ }
}
function escapeHtml(text) {
  return text.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
}
let toastTimer = 0;
function toast(message) {
  els.toast.textContent = message;
  els.toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { els.toast.hidden = true; }, 3500);
}
function setStatus(kind, title, text) {
  if (!kind) { els.status.hidden = true; return; }
  els.status.className = 'status ' + kind;
  els.status.innerHTML = `<strong>${escapeHtml(title)}</strong>${escapeHtml(text || '')}`;
  els.status.hidden = false;
}
function busy(text, onCancel) {
  if (!text) { els.busy.hidden = true; return; }
  els.busyText.textContent = text;
  $('busy-cancel').hidden = !onCancel;
  $('busy-cancel').onclick = onCancel || null;
  els.busy.hidden = false;
}
const nextFrame = () => new Promise((resolve) => requestAnimationFrame(() => setTimeout(resolve, 20)));

/** cells whose digit is repeated in a row, column or box */
function conflicts(grid) {
  const bad = new Set();
  for (let a = 0; a < 81; a++)
    if (grid[a])
      for (let b = a + 1; b < 81; b++)
        if (grid[b] === grid[a] && sees(a, b)) { bad.add(a); bad.add(b); }
  return bad;
}

// ---------------------------------------------------------------------------
//  Board
// ---------------------------------------------------------------------------

function buildBoard() {
  for (let p = 0; p < 81; p++) {
    const cell = document.createElement('button');
    cell.type = 'button';
    cell.className = 'cell';
    if (colOf(p) === 2 || colOf(p) === 5) cell.classList.add('br');
    if (rowOf(p) === 2 || rowOf(p) === 5) cell.classList.add('bb');
    if (rowOf(p) === 8) cell.classList.add('last-row');
    cell.setAttribute('role', 'gridcell');
    cell.addEventListener('click', () => { state.selected = p; render(); });
    els.board.appendChild(cell);
    cells.push(cell);
  }
  for (let d = 1; d <= 9; d++) {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = d;
    b.addEventListener('click', () => setDigit(state.selected, d));
    els.numpad.appendChild(b);
  }
  const erase = document.createElement('button');
  erase.type = 'button';
  erase.className = 'erase';
  erase.innerHTML = '&#x232B;';
  erase.setAttribute('aria-label', 'Erase');
  erase.addEventListener('click', () => setDigit(state.selected, 0));
  els.numpad.appendChild(erase);
}

/** the grid to show and the highlights of the current step */
function currentView() {
  if (state.view === 'edit') return { grid: state.givens, step: null };
  const n = state.steps.length;
  if (state.stepIndex >= n) {
    const solution = state.result.solution.split('').map(Number);
    return { grid: solution, step: null };
  }
  return { grid: state.grids[state.stepIndex + 1], step: state.steps[state.stepIndex] };
}

function render() {
  const { grid, step } = currentView();
  const bad = state.view === 'edit' ? conflicts(grid) : new Set();
  const sel = state.selected;
  for (let p = 0; p < 81; p++) {
    const cell = cells[p];
    const value = grid[p];
    cell.textContent = value ? value : '';
    const cls = cell.classList;
    cls.toggle('solved', state.view === 'solved' && !state.givens[p] && value !== 0);
    cls.toggle('conflict', bad.has(p));
    cls.toggle('uncertain', state.view === 'edit' && state.uncertain.has(p) && !bad.has(p));
    cls.toggle('selected', state.view === 'edit' && p === sel);
    cls.toggle('peer', state.view === 'edit' && p !== sel && sees(p, sel));
    cls.toggle('step-place', !!(step && step.place && step.place.cell === p));
    cls.toggle('step-elim', !!(step && step.elim.has(p) && !(step.place && step.place.cell === p)));
    cls.toggle('step-ref', !!(step && step.refs.has(p) && !step.elim.has(p) && !(step.place && step.place.cell === p)));
    cell.setAttribute('aria-label', `${rc(p)} ${value || ''}`);
  }
  els.board.classList.toggle('photo', !!(state.photo && els.photoCheck.checked));
  els.board.style.backgroundImage = state.photo && els.photoCheck.checked ? `url(${state.photo})` : '';
  els.photoToggle.hidden = !state.photo;
  els.numpad.hidden = state.view !== 'edit';
  els.solve.hidden = state.view !== 'edit';
  els.clear.hidden = state.view !== 'edit';
  els.edit.hidden = state.view === 'edit';
  els.stepsCard.hidden = state.view !== 'solved' || state.steps.length === 0;
  if (state.view === 'solved') renderStep();
}

function setDigit(p, d) {
  if (state.view !== 'edit') backToEdit();
  state.givens[p] = d;
  state.uncertain.delete(p);
  if (d && els.status.classList.contains('warn') === false) setStatus(null);
  save();
  render();
}

function setGrid(digits) {
  state.givens = digits.slice();
  state.uncertain.clear();
  state.photo = null;
  backToEdit();
  save();
  render();
}

function backToEdit() {
  state.view = 'edit';
  state.result = null;
  setStatus(null);
}

// ---------------------------------------------------------------------------
//  Keyboard and paste (computer)
// ---------------------------------------------------------------------------

function overlayOpen() {
  return !$('camera-view').hidden || !$('crop-view').hidden || !els.busy.hidden;
}
document.addEventListener('keydown', (e) => {
  if (overlayOpen() || e.ctrlKey || e.metaKey || e.altKey) return;
  if (state.view === 'solved') {
    if (e.key === 'ArrowRight') { gotoStep(state.stepIndex + 1); e.preventDefault(); }
    if (e.key === 'ArrowLeft') { gotoStep(state.stepIndex - 1); e.preventDefault(); }
    return;
  }
  const p = state.selected;
  const moves = { ArrowUp: -9, ArrowDown: 9, ArrowLeft: -1, ArrowRight: 1 };
  if (moves[e.key] !== undefined) {
    const q = p + moves[e.key];
    if (q >= 0 && q < 81 && (Math.abs(moves[e.key]) === 9 || rowOf(q) === rowOf(p))) state.selected = q;
    render();
    e.preventDefault();
  } else if (/^[1-9]$/.test(e.key)) {
    setDigit(p, Number(e.key));
    if (p < 80) state.selected = p + 1;
    render();
  } else if (['0', 'Backspace', 'Delete', ' ', '.'].includes(e.key)) {
    setDigit(p, 0);
    e.preventDefault();
  }
});
document.addEventListener('paste', (e) => {
  const digits = parseText(e.clipboardData.getData('text') || '');
  if (digits) { setGrid(digits); e.preventDefault(); }
});

// ---------------------------------------------------------------------------
//  Solver (Web Worker)
// ---------------------------------------------------------------------------

let worker = null, requestId = 0;
const pending = new Map();
function getWorker() {
  if (!worker) {
    worker = new Worker(new URL('./solver-worker.js', import.meta.url), { type: 'module' });
    worker.onmessage = (e) => {
      const callback = pending.get(e.data.id);
      pending.delete(e.data.id);
      if (callback) callback(e.data);
    };
    worker.onerror = () => {
      for (const callback of pending.values()) callback({ error: 'worker' });
      pending.clear();
      worker = null;
    };
  }
  return worker;
}
function ask(message) {
  return new Promise((resolve) => {
    const id = ++requestId;
    pending.set(id, resolve);
    getWorker().postMessage({ id, ...message });
  });
}
function cancelWorker() {
  if (worker) worker.terminate();
  worker = null;
  for (const callback of pending.values()) callback({ cancelled: true });
  pending.clear();
}

async function solveCurrent() {
  const grid = state.givens;
  if (!grid.some((d) => d)) { setStatus('warn', t('emptyTitle'), t('empty')); return; }
  if (conflicts(grid).size) { setStatus('error', t('conflictsTitle'), t('conflicts')); render(); return; }
  busy(t('solving'), () => { cancelWorker(); busy(null); });
  const answer = await ask({ solve: grid.join('') });
  busy(null);
  if (answer.cancelled) return;
  if (answer.error) { setStatus('error', t('errorTitle'), t('error')); return; }
  showResult(answer.result, answer.ms);
}

function showResult(result, ms) {
  state.result = result;
  state.ms = ms;
  const status = result.status;
  if (status === 'multiple') { setStatus('error', t('multipleTitle'), t('multiple')); return; }
  if (status === 'none') { setStatus('error', t('noneTitle'), t('none')); return; }
  if (status === 'invalid') { setStatus('error', t('invalidTitle'), t('invalid')); return; }
  buildSteps(result.steps);
  state.view = 'solved';
  state.stepIndex = state.steps.length;
  if (status === 'solved') {
    const techniques = result.techniques || {};
    let note = t('solvedLogic');
    if (techniques['trial and error']) note = t('solvedTrial');
    else if (techniques['forcing net']) note = t('solvedNet');
    setStatus('ok', t('solvedTitle'), t('solvedText', state.steps.length, (ms / 1000).toFixed(1)) + ' ' + note);
  } else {
    setStatus('warn', t('unsolvedTitle'), t('unsolved'));
  }
  renderChips();
  renderList();
  render();
}

// ---------------------------------------------------------------------------
//  Steps: parsing of the log of the solver
// ---------------------------------------------------------------------------

/** technique of an "Applied ..." line, e.g. "Applied naked subset in row 3: ..." -> "naked subset" */
function techniqueOf(line) {
  let name = line.slice(8);
  const cut = [name.indexOf(':'), name.indexOf(' (')].filter((i) => i >= 0);
  if (cut.length) name = name.slice(0, Math.min(...cut));
  if (name.startsWith('naked subset')) name = 'naked subset';
  return name;
}
/** the digit placed by a line of the log, if any */
function placementOf(line) {
  let m;
  if ((m = line.match(/^Applied hidden single: digit (\d) in cell (\d+)/))) return { digit: +m[1], cell: +m[2] };
  if ((m = line.match(/^Applied naked single: place (\d) in cell: (\d+)/))) return { digit: +m[1], cell: +m[2] };
  if ((m = line.match(/^Applied forcing net: placing \d in cell (\d+) .*so (\d) goes there/))) return { digit: +m[2], cell: +m[1] };
  if ((m = line.match(/^Applied trial and error: placing (\d) in cell (\d+) solves/))) return { digit: +m[1], cell: +m[2] };
  return null;
}
/** cells mentioned by a line: eliminated cells and the other referenced cells */
function cellsOf(line, elim, refs) {
  let m;
  const houseList = /cells ((?:\d+ +)+)of (row|column|box) (\d+)/g;
  while ((m = houseList.exec(line)))
    for (const i of m[1].trim().split(/ +/)) elim.add(housePosition(m[2], +m[3], +i));
  const cannot = /(?:cannot go in|candidates from) cell (\d+)/g;
  while ((m = cannot.exec(line))) elim.add(+m[1]);
  const list = /\bcells?:? (\d+(?:(?:, ?| and | or )\d+)*)(?! *\d* *of )/g;
  while ((m = list.exec(line)))
    for (const n of m[1].split(/, ?| and | or /)) refs.add(+n);
  const or = /\bcell (\d+)/g;
  while ((m = or.exec(line))) refs.add(+m[1]);
  const fromTo = /from (\d+) to (\d+)/g;
  while ((m = fromTo.exec(line))) { refs.add(+m[1]); refs.add(+m[2]); }
  const eureka = /r(\d)c(\d)/g;
  while ((m = eureka.exec(line))) refs.add((+m[1] - 1) * 9 + (+m[2] - 1));
}
/** rewrites a line of the log for people: cells as r1c1 and rows/columns/boxes counted from 1 */
function friendly(line) {
  return line
    .replace(/^Applied /, '')
    .replace(/cells ((?:\d+ +)+)of (row|column|box) (\d+)/g,
      (all, list, type, h) => list.trim().split(/ +/).map((i) => rc(housePosition(type, +h, +i))).join(', ') + ` of ${type} ${h}`)
    .replace(/\b(cells?):? (\d+(?:(?:, ?| and | or )\d+)*)/g,
      (all, word, list) => word + ' ' + list.replace(/\d+/g, (n) => rc(+n)))
    .replace(/from (\d+) to (\d+)/g, (all, a, b) => `from ${rc(+a)} to ${rc(+b)}`)
    .replace(/\b(rows?|columns?|box(?:es)?) ((?:\d+ ?)+)/g,
      (all, word, list) => word + ' ' + list.trim().split(/ +/).map((n) => +n + 1).join(' ') + (list.endsWith(' ') ? ' ' : ''))
    .replace(/\bcell (r\d)/g, '$1')
    .replace(/^./, (c) => c.toUpperCase());
}

/**
 * Groups the lines of the log into steps: every placement is a step; consecutive eliminations of the same technique
 * (with the description of the pattern, if any) form one step. Then computes the grid after every step.
 */
function buildSteps(lines) {
  const steps = [];
  let group = null;
  for (const line of lines) {
    if (/^(The sudoku has|Sudoku solved|The available techniques)/.test(line)) continue;
    if (line.startsWith('Applied ')) {
      const tech = techniqueOf(line);
      const place = placementOf(line);
      if (place) {
        group = null;
        steps.push({ tech, title: null, items: [line], place });
        continue;
      }
      if (group && (group.tech === null || group.tech === tech)) {
        group.tech = tech;
        group.items.push(line);
        continue;
      }
      group = { tech, title: null, items: [line], place: null };
      steps.push(group);
    } else {
      group = { tech: null, title: line, items: [], place: null };
      steps.push(group);
    }
  }
  for (const step of steps) {
    step.elim = new Set();
    step.refs = new Set();
    for (const line of [step.title, ...step.items]) if (line) cellsOf(line, step.elim, step.refs);
    if (!step.tech) step.tech = 'forcing net';
  }
  state.steps = steps;
  // grids[i] = grid after the first i steps
  const grids = [state.givens.slice()];
  for (const step of steps) {
    const next = grids[grids.length - 1].slice();
    if (step.place) next[step.place.cell] = step.place.digit;
    grids.push(next);
  }
  state.grids = grids;
}

function renderChips() {
  const counts = new Map();
  for (const step of state.steps) counts.set(step.tech, (counts.get(step.tech) || 0) + 1);
  const ordered = [...counts.entries()].sort((a, b) => (TECH_ORDER.get(a[0]) ?? 99) - (TECH_ORDER.get(b[0]) ?? 99));
  els.chips.innerHTML = ordered.map(([tech, n]) => `<span class="chip">${escapeHtml(TECH_NAME.get(tech) || tech)} <b>${n}</b></span>`).join('');
}

function renderList() {
  els.list.innerHTML = state.steps.map((step, i) => {
    const text = step.place ? friendly(step.items[0]) : friendly(step.title || step.items[0]);
    return `<li data-i="${i}"><span class="tech">${escapeHtml(TECH_NAME.get(step.tech) || step.tech)}</span> — ${escapeHtml(text)}</li>`;
  }).join('');
  els.list.querySelectorAll('li').forEach((li) => li.addEventListener('click', () => gotoStep(Number(li.dataset.i))));
}

function renderStep() {
  const n = state.steps.length, i = state.stepIndex;
  els.counter.textContent = i >= n ? t('solutionView') : t('stepCounter', i + 1, n);
  $('prev-btn').disabled = i <= 0;
  $('first-btn').disabled = i <= 0;
  $('next-btn').disabled = $('last-btn').disabled = i >= n;
  if (i >= n) {
    els.current.innerHTML = `<div class="title">${escapeHtml(t('solutionView'))}</div>${escapeHtml(t('solutionText'))} ${escapeHtml(t('startText'))}`;
  } else {
    const step = state.steps[i];
    let html = `<div class="title">${escapeHtml(TECH_NAME.get(step.tech) || step.tech)}</div>`;
    if (step.title) html += `<div>${escapeHtml(friendly(step.title))}</div>`;
    if (step.items.length === 1 && !step.title) html += `<div>${escapeHtml(friendly(step.items[0]))}</div>`;
    else if (step.items.length) html += '<ul>' + step.items.map((line) => `<li>${escapeHtml(friendly(line))}</li>`).join('') + '</ul>';
    els.current.innerHTML = html;
  }
  els.list.querySelectorAll('li').forEach((li) => li.classList.toggle('current', Number(li.dataset.i) === i));
}

function gotoStep(i) {
  state.stepIndex = Math.max(0, Math.min(state.steps.length, i));
  render();
}

// ---------------------------------------------------------------------------
//  Photo: camera, upload, corner adjustment, reading
// ---------------------------------------------------------------------------

let stream = null;
async function openCamera() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) { $('capture-input').click(); return; }
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' }, width: { ideal: 1920 }, height: { ideal: 1440 } }, audio: false,
    });
  } catch (e) {
    $('capture-input').click(); // e.g. permission denied: use the camera app of the phone
    return;
  }
  const video = $('camera-video');
  video.srcObject = stream;
  $('camera-view').hidden = false;
}
function closeCamera() {
  if (stream) stream.getTracks().forEach((track) => track.stop());
  stream = null;
  $('camera-view').hidden = true;
}
function shoot() {
  const video = $('camera-video');
  if (!video.videoWidth) return;
  const canvas = document.createElement('canvas');
  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;
  canvas.getContext('2d').drawImage(video, 0, 0);
  closeCamera();
  startCrop(canvas);
}

async function openFile(file) {
  if (!file) return;
  const isText = /^text\//.test(file.type) || /\.(txt|sdk|ss|sdm|csv)$/i.test(file.name);
  if (isText) {
    const digits = parseText(await file.text());
    if (!digits) { toast(t('fileError')); return; }
    setGrid(digits);
    toast(t('textLoaded'));
    return;
  }
  busy(t('opening'));
  try {
    const canvas = await loadImage(file);
    busy(null);
    startCrop(canvas);
  } catch (e) {
    busy(null);
    toast(t('imageError'));
  }
}

// corner adjustment
const crop = { canvas: null, corners: null, scale: 1, drag: -1 };
function startCrop(image) {
  const detection = detectGrid(image);
  crop.image = image;
  crop.corners = detection.corners;
  $('crop-view').hidden = false;
  layoutCrop();
  if (!detection.found) toast(t('gridNotFound'));
}
function layoutCrop() {
  const stage = $('crop-stage'), canvas = $('crop-canvas');
  const maxW = stage.clientWidth, maxH = stage.clientHeight;
  const scale = Math.min(maxW / crop.image.width, maxH / crop.image.height);
  const dpr = window.devicePixelRatio || 1;
  canvas.style.width = Math.round(crop.image.width * scale) + 'px';
  canvas.style.height = Math.round(crop.image.height * scale) + 'px';
  canvas.width = Math.round(crop.image.width * scale * dpr);
  canvas.height = Math.round(crop.image.height * scale * dpr);
  crop.scale = scale * dpr; // image pixels -> canvas pixels
  drawCrop();
}
function drawCrop() {
  const canvas = $('crop-canvas'), ctx = canvas.getContext('2d'), s = crop.scale;
  ctx.drawImage(crop.image, 0, 0, canvas.width, canvas.height);
  const pts = crop.corners.map(([x, y]) => [x * s, y * s]);
  // the 10 x 10 lines of the grid as they will be read
  const H = homography([[0, 0], [9, 0], [9, 9], [0, 9]], pts);
  ctx.lineWidth = Math.max(1, s * 0.8);
  for (let i = 0; i <= 9; i++) {
    ctx.strokeStyle = i % 3 === 0 ? 'rgba(59,130,246,.95)' : 'rgba(59,130,246,.45)';
    ctx.beginPath();
    let [x, y] = project(H, i, 0); ctx.moveTo(x, y);
    [x, y] = project(H, i, 9); ctx.lineTo(x, y);
    [x, y] = project(H, 0, i); ctx.moveTo(x, y);
    [x, y] = project(H, 9, i); ctx.lineTo(x, y);
    ctx.stroke();
  }
  const r = 14 * (window.devicePixelRatio || 1);
  for (const [x, y] of pts) {
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(59,130,246,.35)';
    ctx.fill();
    ctx.lineWidth = 3;
    ctx.strokeStyle = '#fff';
    ctx.stroke();
  }
}
function cropPoint(e) {
  const rect = $('crop-canvas').getBoundingClientRect();
  const dpr = window.devicePixelRatio || 1;
  return [(e.clientX - rect.left) * dpr / crop.scale, (e.clientY - rect.top) * dpr / crop.scale];
}
function setupCropEvents() {
  const canvas = $('crop-canvas');
  canvas.addEventListener('pointerdown', (e) => {
    const [x, y] = cropPoint(e);
    const tolerance = 40 * (window.devicePixelRatio || 1) / crop.scale;
    let best = -1, bestDist = Infinity;
    crop.corners.forEach(([cx, cy], i) => {
      const d = Math.hypot(cx - x, cy - y);
      if (d < bestDist) { bestDist = d; best = i; }
    });
    if (bestDist <= tolerance) {
      crop.drag = best;
      canvas.setPointerCapture(e.pointerId);
    }
  });
  canvas.addEventListener('pointermove', (e) => {
    if (crop.drag < 0) return;
    const [x, y] = cropPoint(e);
    crop.corners[crop.drag] = [Math.max(0, Math.min(crop.image.width, x)), Math.max(0, Math.min(crop.image.height, y))];
    drawCrop();
  });
  const end = () => { crop.drag = -1; };
  canvas.addEventListener('pointerup', end);
  canvas.addEventListener('pointercancel', end);
  window.addEventListener('resize', () => { if (!$('crop-view').hidden) layoutCrop(); });
}
async function readCrop() {
  $('crop-view').hidden = true;
  busy(t('reading'));
  await nextFrame();
  let reading;
  try {
    reading = readGrid(crop.image, crop.corners);
  } catch (e) {
    busy(null);
    toast(t('imageError'));
    return;
  }
  setGrid(reading.digits);
  state.photo = reading.image;
  els.photoCheck.checked = true;
  reading.confidence.forEach((c, p) => { if (reading.digits[p] && c < UNCERTAIN) state.uncertain.add(p); });
  // a quick check of the digits that have been read
  const answer = await ask({ count: state.givens.join('') });
  busy(null);
  let text = t('reviewText', state.uncertain.size);
  let kind = 'warn';
  if (conflicts(state.givens).size === 0 && answer.count === 0) text = t('reviewNoSolution') + ' ' + text;
  else if (answer.count === 2) text = t('reviewMultiple') + ' ' + text;
  setStatus(kind, t('reviewTitle'), text);
  render();
}
const UNCERTAIN = 0.06; // confidence below which a digit read from a photo is highlighted

// ---------------------------------------------------------------------------
//  Install (PWA) and language
// ---------------------------------------------------------------------------

let installEvent = null;
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  installEvent = e;
  $('install-btn').hidden = false;
});
const isIos = /iphone|ipad|ipod/i.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
const standalone = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone;
if (isIos && !standalone) $('install-btn').hidden = false;
$('install-btn').addEventListener('click', async () => {
  if (installEvent) {
    installEvent.prompt();
    await installEvent.userChoice;
    installEvent = null;
    $('install-btn').hidden = true;
  } else if (isIos) {
    toast(t('iosInstall'));
  }
});
window.addEventListener('appinstalled', () => { $('install-btn').hidden = true; });

function applyLanguage() {
  document.documentElement.lang = lang;
  document.querySelectorAll('[data-i18n]').forEach((el) => { el.textContent = t(el.dataset.i18n); });
  $('lang-btn').textContent = lang === 'it' ? 'EN' : 'IT';
  if (state.view === 'solved' && state.result) { const i = state.stepIndex; showResult(state.result, state.ms); gotoStep(i); }
  else if (state.view === 'edit' && !els.status.hidden && els.status.dataset.manual) showManualHint();
}
function showManualHint() {
  setStatus('info', t('manualTitle'), t('manualText'));
  els.status.dataset.manual = '1';
}
$('lang-btn').addEventListener('click', () => {
  lang = lang === 'it' ? 'en' : 'it';
  try { localStorage.setItem('lang', lang); } catch (e) { /* ignore */ }
  applyLanguage();
});

// ---------------------------------------------------------------------------
//  Start
// ---------------------------------------------------------------------------

function setMode(id) {
  document.querySelectorAll('.mode').forEach((b) => b.classList.toggle('active', b.id === id));
}
$('mode-camera').addEventListener('click', () => { setMode('mode-camera'); openCamera(); });
$('mode-upload').addEventListener('click', () => { setMode('mode-upload'); $('file-input').click(); });
$('mode-manual').addEventListener('click', () => {
  setMode('mode-manual');
  if (state.view !== 'edit') backToEdit();
  showManualHint();
  render();
  cells[state.selected].focus();
});
$('file-input').addEventListener('change', (e) => { openFile(e.target.files[0]); e.target.value = ''; });
$('capture-input').addEventListener('change', (e) => { openFile(e.target.files[0]); e.target.value = ''; });
$('camera-cancel').addEventListener('click', closeCamera);
$('camera-shoot').addEventListener('click', shoot);
$('camera-file').addEventListener('click', () => { closeCamera(); $('file-input').click(); });
$('crop-cancel').addEventListener('click', () => { $('crop-view').hidden = true; });
$('crop-read').addEventListener('click', readCrop);
els.solve.addEventListener('click', solveCurrent);
els.clear.addEventListener('click', () => { setGrid(new Array(81).fill(0)); });
els.edit.addEventListener('click', () => { backToEdit(); render(); });
els.photoCheck.addEventListener('change', render);
$('first-btn').addEventListener('click', () => gotoStep(0));
$('prev-btn').addEventListener('click', () => gotoStep(state.stepIndex - 1));
$('next-btn').addEventListener('click', () => gotoStep(state.stepIndex + 1));
$('last-btn').addEventListener('click', () => gotoStep(state.steps.length));

buildBoard();
setupCropEvents();
try {
  const saved = localStorage.getItem('grid');
  if (saved && /^\d{81}$/.test(saved)) state.givens = saved.split('').map(Number);
} catch (e) { /* ignore */ }
applyLanguage();
render();

if ('serviceWorker' in navigator && location.protocol !== 'file:') {
  navigator.serviceWorker.register('sw.js').then((registration) => {
    registration.addEventListener('updatefound', () => {
      const installing = registration.installing;
      installing && installing.addEventListener('statechange', () => {
        if (installing.state === 'activated' && !navigator.serviceWorker.controller) toast(t('offlineReady'));
      });
    });
  }).catch(() => { /* offline mode not available (e.g. http) */ });
}

// for automated tests
window.__sudoku = { state, setGrid, solveCurrent, gotoStep, readGridFromCanvas: (c) => readGrid(c, detectGrid(c).corners), detectGrid };
