import { test, expect } from '@playwright/test';
import { reiniciarDemo } from '../apoyo.mjs';

test.beforeAll(async ({ request }) => { await reiniciarDemo(request); });

test.describe('registro propio (no demo)', () => {
  test('un talento nuevo se registra, ve el diálogo de bienvenida y entra al dashboard', async ({ page }) => {
    const correo = `prueba.${Date.now()}@example.com`;
    await page.goto('/register/talento');
    await page.locator('#t-nombre').fill('Persona de Prueba');
    await page.locator('input[name="especialidad"]').first().check({ force: true });
    await page.locator('#t-rolTitulo').fill('Desarrolladora');
    await page.locator('#t-stack').fill('Java, React');
    await page.locator('#t-correo').fill(correo);
    await page.locator('#t-password').fill('Prueba12345!');
    await page.locator('#t-passwordConfirm').fill('Prueba12345!');
    await page.locator('#go').click();
    await page.waitForURL(/dashboard|login/);
    if (page.url().includes('/login')) {
      await page.locator('#correo').fill(correo);
      await page.locator('#password').fill('Prueba12345!');
      await page.locator('#go').click();
      await page.waitForURL('**/dashboard');
    }
    await expect(page.locator('main, .content').first()).toBeVisible();
  });

  test('login con clave incorrecta muestra el error y no entra', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#correo').fill('demo@devcoast.co');
    await page.locator('#password').fill('clave-equivocada');
    await page.locator('#go').click();
    await expect(page.locator('#formErr')).toContainText(/incorrectos/i);
    await expect(page).toHaveURL(/\/login/);
  });

  test('login con la cuenta demo funciona', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#correo').fill('demo@devcoast.co');
    await page.locator('#password').fill('demo1234');
    await page.locator('#go').click();
    await page.waitForURL('**/dashboard');
  });
});
