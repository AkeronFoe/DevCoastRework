import { test, expect } from '@playwright/test';
import { entrarDemo, reiniciarDemo } from '../apoyo.mjs';

// el modo presentación maneja la app real: aquí se comprueba que cuenta la historia completa y que deja los efectos reales en los datos
test.describe.configure({ mode: 'serial' });
test.beforeEach(async ({ request }) => { await reiniciarDemo(request); });

const RITMO_RAPIDO = () => sessionStorage.setItem('dc_presentacion_ritmo', '0.2');
const TITULOS_EJEMPLO = /Backend Developer \(Kotlin\)|Frontend Developer \(React\)|Diseñador UX\/UI|Data Analyst/;

async function efectosDeLaHistoria(browser) {
  const ctxT = await browser.newContext();
  const talento = await ctxT.newPage();
  await entrarDemo(talento, 'talento');
  await talento.goto('/solicitudes');
  const tarjeta = talento.locator('article', { hasText: 'Full Stack Developer (Java + React)' }).first();
  await expect(tarjeta).toContainText('Aceptada');
  await ctxT.close();

  const ctxE = await browser.newContext();
  const empresa = await ctxE.newPage();
  await entrarDemo(empresa, 'empresa');
  await empresa.goto('/misEmpleos');
  await expect(empresa.locator('main')).toContainText(TITULOS_EJEMPLO);
  await empresa.goto('/mensajes');
  await expect(empresa.locator('main')).toContainText('Carlos Andrés Ramírez');
  await empresa.goto('/candidatos');
  await expect(empresa.locator('main')).toContainText('Muchas gracias por aceptarme').catch(() => {});
  await ctxE.close();
}

test('manual: «Siguiente» recorre la historia real de principio a fin y deja sus efectos', async ({ page, browser }) => {
  test.setTimeout(150_000);
  await page.addInitScript(RITMO_RAPIDO);
  await entrarDemo(page, 'talento');
  await page.goto('/dashboard');
  await page.getByRole('button', { name: '▶ Presentar' }).click();
  const panel = page.locator('.pres-panel');
  await expect(panel).toContainText('PASO 1 DE');
  for (let i = 0; i < 60; i++) {
    if (await panel.getByRole('button', { name: 'Repetir' }).isVisible().catch(() => false)) break;
    await panel.getByRole('button', { name: 'Siguiente' }).click({ timeout: 4000 }).catch(() => { });
    await page.waitForTimeout(450);
  }
  await expect(panel).toContainText('¡Eso es DevCoast!');
  await efectosDeLaHistoria(browser);
});

test('automático desde /demo: cuenta la historia sola, se puede pausar y salir', async ({ page, browser }) => {
  test.setTimeout(150_000);
  await page.addInitScript(RITMO_RAPIDO);
  await page.goto('/demo');
  await page.getByRole('button', { name: /Ver la demo automática/ }).click();
  const panel = page.locator('.pres-panel');
  await expect(panel).toContainText('PASO 2 DE', { timeout: 40_000 });
  // sin bucle para poder revisar los efectos al terminar (con bucle la demo se reinicia sola y los borraría)
  await page.evaluate(() => { const s = JSON.parse(sessionStorage.getItem('dc_presentacion')); s.loop = false; sessionStorage.setItem('dc_presentacion', JSON.stringify(s)); });

  // pausa: el paso no cambia mientras está en pausa
  await panel.getByRole('button', { name: 'Pausa' }).click();
  const enPausa = await panel.locator('.pres-top small').innerText();
  await page.waitForTimeout(3000);
  expect(await panel.locator('.pres-top small').innerText()).toBe(enPausa);
  await panel.getByRole('button', { name: 'Seguir' }).click();

  await expect(panel).toContainText('FIN', { timeout: 100_000 });
  await efectosDeLaHistoria(browser);
  await panel.getByRole('button', { name: 'Salir' }).click().catch(() => { });
});

test('en bucle, al terminar la demo se reinicia y vuelve a empezar sola', async ({ page }) => {
  test.setTimeout(150_000);
  await page.addInitScript(RITMO_RAPIDO);
  await page.goto('/demo');
  await page.getByRole('button', { name: /Ver la demo automática/ }).click();
  const panel = page.locator('.pres-panel');
  await expect(panel).toContainText('FIN', { timeout: 110_000 });
  // tras el FIN vuelve a contar la historia por sí sola (los primeros pasos duran fracciones de segundo, así que se mira que ya no esté en FIN)
  await expect(panel).toContainText('PASO', { timeout: 30_000 });
  await expect(panel).not.toContainText('FIN');
  await page.keyboard.press('Escape');
});

test('Esc sale del modo presentación y no deja nada guardado', async ({ page }) => {
  await entrarDemo(page, 'talento');
  await page.goto('/dashboard');
  await page.getByRole('button', { name: '▶ Presentar' }).click();
  await expect(page.locator('.pres-panel')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.locator('.pres-panel')).toHaveCount(0);
  expect(await page.evaluate(() => sessionStorage.getItem('dc_presentacion'))).toBeNull();
  await page.goto('/empleos');
  await expect(page.locator('.pres-panel')).toHaveCount(0);
});

test('en celular el panel cabe en la pantalla', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 760 });
  await entrarDemo(page, 'talento');
  await page.goto('/dashboard');
  await page.evaluate(() => { window.dcSolo = true; });
  // en celular el menú está plegado: se arranca desde la pantalla /demo
  await page.goto('/demo');
  await page.getByRole('button', { name: /Ver la demo automática/ }).click();
  const panel = page.locator('.pres-panel');
  await expect(panel).toBeVisible({ timeout: 20_000 });
  const caja = await panel.boundingBox();
  expect(caja.x).toBeGreaterThanOrEqual(0);
  expect(caja.x + caja.width).toBeLessThanOrEqual(375);
  await page.keyboard.press('Escape');
});

test('«Rellenar con un ejemplo» llena Publicar oferta y deja todo listo para publicar', async ({ page }) => {
  await entrarDemo(page, 'empresa');
  await page.goto('/crearEmpleo');
  await page.getByRole('button', { name: /Rellenar con un ejemplo/ }).click();
  await expect(page.locator('#paso-3')).toBeVisible();
  const titulo = (await page.locator('#pv-titulo').innerText()).trim();
  expect(titulo).toMatch(TITULOS_EJEMPLO);
  await page.locator('#btn-publicar').click();
  await page.waitForURL(/misEmpleos|empleos/);
  await page.goto('/misEmpleos');
  await expect(page.locator('main')).toContainText(titulo);
});

test('una sesión normal (no demo) no ve ni el botón Presentar ni «Rellenar con un ejemplo» fuera de la demo', async ({ page }) => {
  await page.goto('/login');
  await page.locator('#correo').fill('empresa@devcoast.co');
  await page.locator('#password').fill('demo1234');
  await page.locator('#go').click();
  await page.waitForURL('**/dashboard');
  await expect(page.locator('.demo-card')).toHaveCount(0);
  await expect(page.locator('[data-presentar]')).toHaveCount(0);
  // arrancar la presentación con un estado guardado a mano no hace nada: sin sesión de demo se descarta
  await page.evaluate(() => sessionStorage.setItem('dc_presentacion', JSON.stringify({ activo: true, paso: 3, modo: 'auto' })));
  await page.goto('/empleos');
  await page.waitForTimeout(800);
  await expect(page.locator('.pres-panel')).toHaveCount(0);
  expect(await page.evaluate(() => sessionStorage.getItem('dc_presentacion'))).toBeNull();
});

test.describe('robustez ante páginas vencidas y clics rápidos', () => {
  test('entrar a la demo desde una página que quedó abierta con la sesión vencida no da 403', async ({ page, context }) => {
    await page.goto('/demo');
    await context.clearCookies(); // como si la app se hubiera reiniciado o la sesión hubiera vencido
    await page.getByRole('button', { name: /Probar como talento/i }).first().click();
    await page.waitForURL('**/dashboard');
    await expect(page.locator('.demo-card')).toBeVisible();
  });

  test('POST /demo/{rol} no exige token (solo entra a cuentas de ejemplo públicas)', async ({ request }) => {
    const r = await request.post('/demo/empresa', { maxRedirects: 0 });
    expect(r.status()).toBe(302);
    expect(r.headers()['location']).toContain('/dashboard');
  });

  test('un 403 de verdad (otra ruta sin token) muestra una página amable, no la de error por defecto', async ({ request }) => {
    const r = await request.post('/login', { form: { correo: 'x@x.co', password: 'x' }, headers: { Accept: 'text/html' }, maxRedirects: 0 });
    expect(r.status()).toBe(403);
    const html = await r.text();
    expect(html).toContain('Esta pantalla ya venció');
    expect(html).not.toContain('Whitelabel');
  });

  test('clics rápidos y mezclados (Siguiente, Manual, Automático, Pausa) nunca rompen la presentación', async ({ page }) => {
    test.setTimeout(120_000);
    const malas = [];
    page.on('response', (r) => { if (r.status() >= 400 && !/favicon/.test(r.url())) malas.push(`${r.status()} ${r.request().method()} ${r.url()}`); });
    await page.addInitScript(() => sessionStorage.setItem('dc_presentacion_ritmo', '0.3'));
    await page.goto('/demo');
    await page.getByRole('button', { name: /Ver la demo automática/ }).click();
    const panel = page.locator('.pres-panel');
    const nombres = ['Siguiente', 'Siguiente', 'Siguiente', 'Manual', 'Automático', 'Pausa', 'Seguir'];
    const fin = Date.now() + 45_000;
    while (Date.now() < fin) {
      const n = nombres[Math.floor(Math.random() * nombres.length)];
      await panel.getByRole('button', { name: n, exact: true }).click({ timeout: 250 }).catch(() => { });
      await page.waitForTimeout(80 + Math.random() * 500);
      expect(page.url()).not.toMatch(/\/demo\/\w+$/);
    }
    expect(malas, malas.join('\n')).toEqual([]);
    await page.keyboard.press('Escape');
  });

  test('volver con «atrás» a mitad de la presentación y seguir no rompe nada', async ({ page }) => {
    test.setTimeout(120_000);
    const malas = [];
    page.on('response', (r) => { if (r.status() >= 400 && !/favicon/.test(r.url())) malas.push(`${r.status()} ${r.request().method()} ${r.url()}`); });
    await page.addInitScript(() => sessionStorage.setItem('dc_presentacion_ritmo', '0.2'));
    await entrarDemo(page, 'talento');
    await page.goto('/dashboard');
    await page.getByRole('button', { name: '▶ Presentar' }).click();
    const panel = page.locator('.pres-panel');
    for (let i = 0; i < 40 && !/\/candidatos/.test(page.url()); i++) {
      await panel.getByRole('button', { name: 'Siguiente' }).click({ timeout: 3000 }).catch(() => { });
      await page.waitForTimeout(400);
    }
    expect(page.url()).toContain('/candidatos');
    await page.goBack();
    await page.waitForTimeout(1200);
    await page.goBack().catch(() => { });
    await page.waitForTimeout(1200);
    for (let i = 0; i < 60; i++) {
      if (await panel.getByRole('button', { name: 'Repetir' }).isVisible().catch(() => false)) break;
      await panel.getByRole('button', { name: 'Siguiente' }).click({ timeout: 3000 }).catch(() => { });
      await page.waitForTimeout(400);
    }
    expect(malas, malas.join('\n')).toEqual([]);
    expect(page.url()).not.toMatch(/\/demo\/\w+$/);
  });
});
