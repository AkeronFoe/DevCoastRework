import { expect } from '@playwright/test';

// escucha errores de consola y de red propios de la app (ignora lo externo: fuentes, CDN) y los entrega al final
export function vigilar(page) {
  const problemas = [];
  const propio = (url) => { try { return new URL(url).origin === new URL(page.url() === 'about:blank' ? 'http://localhost' : page.url()).origin; } catch { return true; } };
  page.on('pageerror', (e) => problemas.push('JS: ' + e.message));
  page.on('console', (m) => {
    if (m.type() !== 'error') return;
    const url = m.location().url;
    if (url && !url.startsWith('http://localhost') && !url.startsWith(process.env.BASE_URL || 'http://localhost')) return;
    problemas.push('consola: ' + m.text());
  });
  page.on('response', (r) => {
    const u = r.url();
    if (r.status() >= 400 && (u.startsWith(process.env.BASE_URL || 'http://localhost'))) {
      if (u.endsWith('/favicon.ico')) return;
      problemas.push(`HTTP ${r.status()}: ${u}`);
    }
  });
  return problemas;
}

export async function sinDesborde(page) {
  const [ancho, cliente] = await page.evaluate(() => [document.documentElement.scrollWidth, document.documentElement.clientWidth]);
  expect(ancho, `la página se desborda horizontalmente (${ancho}px > ${cliente}px)`).toBeLessThanOrEqual(cliente + 1);
}

export async function entrarDemo(page, rol) {
  await page.goto('/demo');
  if (rol === 'startup' || rol === 'diseno') {
    await page.locator('summary', { hasText: 'Más cuentas' }).click();
    await page.getByRole('button', { name: rol === 'startup' ? /Marea Studio/ : /Laura/ }).click();
  } else {
    await page.getByRole('button', { name: new RegExp(`Probar como ${rol}`, 'i') }).first().click();
  }
  await page.waitForURL('**/dashboard');
  // el recorrido guiado aparece solo la primera vez: se cierra para no tapar nada
  const saltar = page.locator('.tour-card .tour-skip');
  if (await saltar.isVisible({ timeout: 2500 }).catch(() => false)) await saltar.click();
}

// deja la demo como el primer día. Se llama al empezar cada archivo de flujos, para que no dependan del orden en que corren
export async function reiniciarDemo(request) {
  const token = process.env.FERIA_TOKEN;
  if (!token) return;
  const r = await request.post('/interno/demo/reiniciar', { headers: { 'X-Feria-Token': token } });
  expect(r.ok(), 'no se pudo reiniciar la demo: ' + r.status()).toBeTruthy();
}
