import { test, expect } from '@playwright/test';
import { entrarDemo, reiniciarDemo } from '../apoyo.mjs';

test.beforeEach(async ({ request }) => { await reiniciarDemo(request); });

test('«Reiniciar mi demo» deshace lo que hizo el visitante', async ({ page }) => {
  await entrarDemo(page, 'empresa');
  // el visitante cierra una oferta y la demo la deja como estaba
  await page.goto('/misEmpleos');
  const cierre = page.locator('form[action*="/empleos/cerrar/"]').first();
  const id = (await cierre.getAttribute('action')).split('/').pop();
  await cierre.locator('summary').click();
  await cierre.getByRole('button', { name: /^Sí, cerrar/ }).click();
  await page.waitForLoadState('networkidle');
  await page.goto('/misEmpleos?tab=cerradas');
  await expect(page.locator(`form[action$="/empleos/reabrir/${id}"]`)).toHaveCount(1);

  await page.getByRole('button', { name: 'Reiniciar mi demo' }).click();
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.locator('main')).toContainText('Demo reiniciada');
  await page.goto('/misEmpleos');
  await expect(page.locator(`form[action$="/empleos/cerrar/${id}"]`)).toHaveCount(1);
});

test('el reinicio por script exige el token correcto', async ({ request }) => {
  const r = await request.post('/interno/demo/reiniciar', { headers: { 'X-Feria-Token': 'token-que-no-es' } });
  expect([403, 404]).toContain(r.status());
});

test('el recorrido guiado aparece al entrar a la demo, se navega con teclado y se cierra con Esc', async ({ page }) => {
  await page.goto('/demo');
  await page.getByRole('button', { name: /Probar como talento/i }).first().click();
  const tarjeta = page.locator('.tour-card');
  await expect(tarjeta).toBeVisible();
  await expect(tarjeta).toContainText('PASO 1 DE 6');
  await page.keyboard.press('ArrowRight');
  await expect(tarjeta).toContainText('PASO 2 DE 6');
  await page.keyboard.press('ArrowLeft');
  await expect(tarjeta).toContainText('PASO 1 DE 6');
  await page.keyboard.press('Escape');
  await expect(tarjeta).toHaveCount(0);
  // se puede volver a ver desde el botón del menú
  await page.getByRole('button', { name: 'Recorrido' }).click();
  await expect(page.locator('.tour-card')).toBeVisible();
});

test('en celular el recorrido se muestra centrado y no rompe la página', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 760 });
  await page.goto('/demo');
  await page.getByRole('button', { name: /Probar como empresa/i }).first().click();
  const tarjeta = page.locator('.tour-card');
  await expect(tarjeta).toBeVisible();
  const caja = await tarjeta.boundingBox();
  expect(caja.x).toBeGreaterThanOrEqual(0);
  expect(caja.x + caja.width).toBeLessThanOrEqual(375);
  await page.keyboard.press('Escape');
});
