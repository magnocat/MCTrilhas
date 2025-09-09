// O nome do cache. Mude a versão (v1, v2, etc.) quando atualizar os arquivos estáticos.
const CACHE_NAME = 'mctrilhas-cache-v1';

// Lista de todos os arquivos que compõem o "app shell" e devem funcionar offline.
const urlsToCache = [
    '/',
    '/index.html',
    '/manifest.json',
    'https://cdn.tailwindcss.com',
    'https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&family=Montserrat:wght@600&display=swap',
    'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/js/all.min.js',
    'https://cdn.jsdelivr.net/npm/chart.js',
    'https://cdn.jsdelivr.net/npm/chartjs-adapter-date-fns/dist/chartjs-adapter-date-fns.bundle.min.js',
    '/bg.jpg',
    // Ícones para a tela inicial
    '/android-chrome-192x192.png',
    '/android-chrome-512x512.png',
    '/android-icon-70x70.png',
    '/favicon-96x96.png',
    '/favicon-32x32.png',
    '/favicon-16x16.png',
    '/apple-icon-57x57.png',
    '/apple-icon-60x60.png',
    '/apple-icon-72x72.png',
    '/apple-icon-76x76.png'
];

// Evento de instalação: abre o cache e adiciona os arquivos principais.
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => {
                console.log('Cache aberto e arquivos do app shell adicionados.');
                return cache.addAll(urlsToCache);
            })
    );
});

// Evento de fetch: intercepta as requisições de rede.
self.addEventListener('fetch', event => {
    // Não fazemos cache da API de dados, pois ela é dinâmica.
    if (event.request.url.includes('/api/v1/data')) {
        return; // Deixa a requisição seguir para a rede normalmente.
    }

    event.respondWith(
        caches.match(event.request)
            .then(response => {
                // Se o recurso estiver no cache, retorna do cache (super rápido).
                if (response) {
                    return response;
                }
                // Se não, busca na rede.
                return fetch(event.request);
            })
    );
});

// Evento de ativação: limpa caches antigos para evitar conflitos.
self.addEventListener('activate', event => {
    const cacheWhitelist = [CACHE_NAME];
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cacheName => {
                    if (cacheWhitelist.indexOf(cacheName) === -1) {
                        // Se o nome do cache não está na lista de permitidos, apaga ele.
                        console.log('Limpando cache antigo:', cacheName);
                        return caches.delete(cacheName);
                    }
                })
            );
        })
    );
});