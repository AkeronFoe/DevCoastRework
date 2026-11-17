// Genera el código QR de la demo para el stand y el póster.
//   node herramientas/qr.mjs https://mi-dominio.example/demo      (por defecto: BASE_URL + /demo)
// Deja feria/material/qr-demo.png (para imprimir) y feria/material/qr-demo.svg (vectorial, para el diseño).
import QRCode from 'qrcode';
import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const aqui = dirname(fileURLToPath(import.meta.url));
const salida = resolve(aqui, '../../feria/material');
const url = process.argv[2] || (process.env.BASE_URL || 'http://localhost:8080').replace(/\/$/, '') + '/demo';
mkdirSync(salida, { recursive: true });
const opciones = { errorCorrectionLevel: 'H', margin: 4, color: { dark: '#101114', light: '#FFFFFF' } };
await QRCode.toFile(resolve(salida, 'qr-demo.png'), url, { ...opciones, width: 1200 });
await QRCode.toFile(resolve(salida, 'qr-demo.svg'), url, { ...opciones, type: 'svg' });
console.log(`QR para ${url}\n  ${resolve(salida, 'qr-demo.png')}\n  ${resolve(salida, 'qr-demo.svg')}`);
if (url.startsWith('http://localhost')) console.warn('OJO: la dirección es local. Para el evento pásale la dirección pública: node herramientas/qr.mjs https://tu-dominio/demo');
