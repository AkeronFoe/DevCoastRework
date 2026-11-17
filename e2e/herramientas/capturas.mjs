// Capturas automáticas de las pantallas clave (computador y celular), para el póster, las diapositivas o redes.
//   BASE_URL=https://mi-dominio.example FERIA_TOKEN=xxxx node herramientas/capturas.mjs
// Deja los PNG en feria/material/capturas/. Con FERIA_TOKEN reinicia la demo antes, para que se vea como el primer día.
import { chromium } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const base = (process.env.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const salida = resolve(dirname(fileURLToPath(import.meta.url)), '../../feria/material/capturas');
mkdirSync(salida, { recursive: true });

if (process.env.FERIA_TOKEN) {
  const r = await fetch(base + '/interno/demo/reiniciar', { method: 'POST', headers: { 'X-Feria-Token': process.env.FERIA_TOKEN } });
  if (!r.ok) throw new Error('No se pudo reiniciar la demo: ' + r.status);
}

const PANTALLAS = {
  publico: [['landing', '/about-us'], ['entrar', '/demo'], ['login', '/login']],
  talento: [['talento-dashboard', '/dashboard'], ['talento-empleos', '/empleos'], ['talento-solicitudes', '/solicitudes'],
    ['talento-proyectos', '/proyectos'], ['talento-perfil', '/perfil'], ['directorio', '/talentos'], ['talento-mensajes', '/mensajes']],
  empresa: [['empresa-dashboard', '/dashboard'], ['empresa-ofertas', '/misEmpleos'], ['empresa-postulaciones', '/candidatos'],
    ['empresa-analytics', '/analytics'], ['empresa-mensajes', '/mensajes']],
};
const TAMANOS = { pc: { width: 1440, height: 900 }, cel: { width: 390, height: 844 } };

const navegador = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
let total = 0;
async function tomar(grupo, entrar) {
  for (const [tam, viewport] of Object.entries(TAMANOS)) {
    const ctx = await navegador.newContext({ viewport, deviceScaleFactor: tam === 'cel' ? 2 : 1, locale: 'es-CO', isMobile: tam === 'cel', hasTouch: tam === 'cel' });
    const page = await ctx.newPage();
    if (entrar) {
      await page.goto(base + '/demo');
      await page.getByRole('button', { name: new RegExp(`Probar como ${entrar}`, 'i') }).first().click();
      await page.waitForURL('**/dashboard');
      const saltar = page.locator('.tour-card .tour-skip');
      if (await saltar.isVisible({ timeout: 2500 }).catch(() => false)) await saltar.click();
    }
    for (const [nombre, ruta] of PANTALLAS[grupo]) {
      await page.goto(base + ruta);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(1600); // deja terminar las animaciones de entrada
      await page.screenshot({ path: resolve(salida, `${nombre}-${tam}.png`), fullPage: false });
      total++;
    }
    await ctx.close();
  }
}
await tomar('publico', null);
await tomar('talento', 'talento');
await tomar('empresa', 'empresa');
await navegador.close();
console.log(`${total} capturas en ${salida}`);
