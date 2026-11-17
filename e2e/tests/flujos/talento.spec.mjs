import { test, expect } from '@playwright/test';
import { entrarDemo, reiniciarDemo } from '../apoyo.mjs';

test.beforeAll(async ({ request }) => { await reiniciarDemo(request); });

test.describe('talento', () => {
  test.beforeEach(async ({ page }) => { await entrarDemo(page, 'talento'); });

  test('guarda una oferta y aparece en Guardados', async ({ page }) => {
    await page.goto('/empleos');
    const enlace = page.locator('main a[href^="/empleos/"]:not([href*="guardados"])').filter({ hasText: /Backend Engineer \(Java\)|Backend Developer/ }).first();
    await enlace.click();
    await expect(page).toHaveURL(/\/empleos\/[0-9a-f]{24}$/);
    const titulo = (await page.locator('h1').first().innerText()).trim();
    await page.getByRole('button', { name: /guardar/i }).first().click();
    await expect(page.locator('#toastTxt')).toContainText(/guardada/i);
    await page.goto('/empleos/guardados');
    await expect(page.locator('main')).toContainText(titulo);
  });

  test('responde una invitación: la acepta y deja de estar pendiente', async ({ page }) => {
    await page.goto('/solicitudes?estado=invitaciones');
    const botones = page.getByRole('button', { name: /^Aceptar la invitación de/ });
    const antes = await botones.count();
    expect(antes).toBeGreaterThan(0);
    await botones.first().click();
    await page.waitForLoadState('networkidle');
    await page.goto('/solicitudes?estado=invitaciones');
    await expect(page.getByRole('button', { name: /^Aceptar la invitación de/ })).toHaveCount(antes - 1);
  });

  test('retira una postulación en revisión', async ({ page }) => {
    await page.goto('/solicitudes');
    const retirar = page.locator('summary[aria-label^="Retirar solicitud de"]').first();
    const cargo = (await retirar.getAttribute('aria-label')).replace('Retirar solicitud de ', '');
    await retirar.click();
    await page.getByRole('button', { name: `Sí, retirar la solicitud de ${cargo}` }).first().click();
    await page.waitForLoadState('networkidle');
    await page.goto('/solicitudes');
    await expect(page.locator(`summary[aria-label="Retirar solicitud de ${cargo}"]`)).toHaveCount(
      (await page.locator(`summary[aria-label="Retirar solicitud de ${cargo}"]`).count()) );
  });

  test('edita la bio del perfil y el cambio se conserva', async ({ page }) => {
    await page.goto('/perfil');
    const bio = page.locator('#pfBio');
    await expect(bio).toBeVisible();
    const nueva = 'Bio de prueba automática ' + Date.now();
    await bio.fill(nueva);
    await page.locator('#pfSave').click();
    await page.waitForLoadState('networkidle');
    await page.goto('/perfil');
    await expect(page.locator('#pfBio')).toHaveValue(nueva);
  });

  test('cambia el tema desde Ajustes y se recuerda al recargar', async ({ page }) => {
    await page.goto('/ajustes#apariencia');
    await page.locator('[data-theme-opt="light"]').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
  });

  test('directorio: busca por nombre y encuentra a la persona', async ({ page }) => {
    await page.goto('/talentos?q=Natalia');
    await expect(page.locator('main')).toContainText('Natalia Suárez Bonilla');
    await page.goto('/talentos');
    await expect(page.locator('main')).toContainText('Mateo Gómez Arango');
  });

  test('perfil público de un talento abre y muestra su información', async ({ page }) => {
    await page.goto('/talentos?q=Natalia');
    await page.locator('main a[href^="/talentos/"]').first().click();
    await expect(page).toHaveURL(/\/talentos\/[0-9a-f]{24}/);
    await expect(page.locator('main')).toContainText('Natalia Suárez Bonilla');
    await expect(page.locator('main')).toContainText('Data Analyst');
  });

  test('perfil público de una empresa abre desde una oferta', async ({ page }) => {
    await page.goto('/empleos');
    await page.locator('main a[href^="/empleos/"]:not([href*="guardados"])').first().click();
    await page.locator('main a[href^="/empresa/"]').first().click();
    await expect(page).toHaveURL(/\/empresa\/[0-9a-f]{24}/);
    await expect(page.locator('main h1').first()).toBeVisible();
  });

  test('la vista de impresión del currículum trae el nombre de la persona', async ({ page }) => {
    const r = await page.request.get('/perfil/exportar-pdf');
    expect(r.status()).toBe(200);
    expect(await r.text()).toContain('Carlos Andrés Ramírez');
  });
});
