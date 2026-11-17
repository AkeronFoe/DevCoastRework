import { test, expect } from '@playwright/test';
import { vigilar, sinDesborde } from '../apoyo.mjs';

const PUBLICAS = ['/about-us', '/login', '/register-options', '/register/talento', '/register/empresa', '/demo'];

for (const ruta of PUBLICAS) {
  test(`pública ${ruta}: carga sin errores`, async ({ page }) => {
    const problemas = vigilar(page);
    const r = await page.goto(ruta);
    expect(r.status()).toBe(200);
    await page.waitForLoadState('networkidle');
    expect(problemas, problemas.join('\n')).toEqual([]);
  });

  test(`pública ${ruta}: sin desborde en celular (375 px)`, async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 760 });
    await page.goto(ruta);
    await page.waitForLoadState('networkidle');
    await sinDesborde(page);
  });
}

test('landing: ofrece probar como talento y como empresa', async ({ page }) => {
  await page.goto('/about-us');
  await expect(page.getByRole('button', { name: 'Probar como talento' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Probar como empresa' })).toBeVisible();
});

test('página inexistente muestra la 404 de la app', async ({ page }) => {
  const r = await page.goto('/esto-no-existe');
  expect(r.status()).toBe(404);
  await expect(page.locator('body')).toContainText(/404|no encontr/i);
});

test('cabeceras de seguridad presentes', async ({ request }) => {
  const r = await request.get('/login');
  const h = r.headers();
  expect(h['content-security-policy']).toContain("frame-ancestors 'none'");
  expect(h['x-content-type-options']).toBe('nosniff');
  expect(h['referrer-policy']).toBeTruthy();
});

test('rutas privadas mandan al login sin sesión', async ({ page }) => {
  for (const ruta of ['/dashboard', '/empleos', '/candidatos', '/mensajes']) {
    await page.goto(ruta);
    await expect(page, ruta).toHaveURL(/\/login/);
  }
});

test('/salud responde con la hora y los conteos', async ({ request }) => {
  const r = await request.get('/salud');
  expect(r.ok()).toBeTruthy();
  const j = await r.json();
  expect(j.estado).toBe('ok');
  expect(j.ofertas).toBeGreaterThan(0);
});
