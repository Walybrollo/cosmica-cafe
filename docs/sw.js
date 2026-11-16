// Permite instalar el pasaporte como app y abrirlo aunque la señal sea mala.
// Siempre intenta traer la versión nueva; si no hay internet, usa la guardada.
const CACHE = "pasaporte-v1";
const ARCHIVOS = ["./", "index.html", "config.js", "qrcode.js", "logo.png", "icono-192.png", "manifest.webmanifest"];

self.addEventListener("install", (e) => e.waitUntil(caches.open(CACHE).then((c) => c.addAll(ARCHIVOS))));
self.addEventListener("activate", (e) => e.waitUntil(self.clients.claim()));
self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== "GET" || url.origin !== location.origin) return;
  e.respondWith(
    fetch(e.request)
      .then((r) => {
        const copia = r.clone();
        caches.open(CACHE).then((c) => c.put(e.request, copia));
        return r;
      })
      .catch(() => caches.match(e.request, { ignoreSearch: true }))
  );
});
