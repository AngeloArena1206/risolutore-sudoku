/*
 * Service worker: keeps a copy of the app on the device so that it works offline and can be installed.
 * Strategy: the files of the app are cached at installation; requests are answered from the cache first and
 * refreshed from the network in the background, so a new version is used from the next start.
 * Change VERSION whenever the files of the app change.
 */
const VERSION = 'sudoku-v1';
const FILES = [
  './',
  'index.html',
  'css/app.css',
  'js/app.js',
  'js/ocr.js',
  'js/solver-worker.js',
  'solver/solver.js',
  'manifest.webmanifest',
  'icons/icon.svg',
  'icons/icon-192.png',
  'icons/icon-512.png',
  'icons/icon-maskable-512.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(VERSION).then((cache) => cache.addAll(FILES)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== VERSION).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
  event.respondWith(
    caches.open(VERSION).then(async (cache) => {
      const cached = await cache.match(request, { ignoreSearch: true });
      const network = fetch(request)
        .then((response) => {
          if (response.ok) cache.put(request, response.clone());
          return response;
        })
        .catch(() => cached);
      return cached || network;
    }),
  );
});
