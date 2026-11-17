import { test, expect } from '@playwright/test';
import { entrarDemo, reiniciarDemo } from '../apoyo.mjs';

test.beforeAll(async ({ request }) => { await reiniciarDemo(request); });

test.describe('empresa', () => {
  test.beforeEach(async ({ page }) => { await entrarDemo(page, 'empresa'); });

  test('publica una oferta nueva (todos los pasos) y aparece en Mis ofertas', async ({ page }) => {
    await page.goto('/crearEmpleo');
    const titulo = 'Oferta de prueba automática ' + Date.now();
    // paso 1: empresa
    await page.locator('#empresa-nombre').fill('DevCoast Labs');
    await page.selectOption('#empresa-sector', 'SaaS / Software');
    await page.selectOption('#empresa-tamaño', { index: 3 });
    await page.locator('#empresa-desc').fill('Plataformas de talento tech.');
    await page.locator('#btn-siguiente').click();
    // paso 2: rol
    await page.locator('#rol-titulo').fill(titulo);
    await page.selectOption('#rol-categoria', 'Programacion · Backend');
    await page.selectOption('#rol-seniority', { index: 2 });
    await page.selectOption('#rol-modalidad', 'Remoto');
    await page.selectOption('#rol-contrato', 'Tiempo completo');
    await page.locator('#rol-vacantes').fill('1');
    await page.locator('#btn-siguiente-1').click();
    // paso 3: requisitos
    await page.locator('#req-descripcion').fill('Descripción de la oferta creada por la prueba automática.');
    await page.locator('#stack-input').fill('Java, Spring Boot');
    await page.locator('#btn-siguiente-2').click();
    // paso 4: resumen y publicar
    await expect(page.locator('#pv-titulo')).toContainText(titulo);
    await page.locator('#btn-publicar').click();
    await page.waitForURL(/misEmpleos|empleos/);
    await page.goto('/misEmpleos');
    await expect(page.locator('main')).toContainText(titulo);
  });

  test('cierra una oferta y la vuelve a abrir', async ({ page }) => {
    await page.goto('/misEmpleos');
    const cierre = page.locator('form[action*="/empleos/cerrar/"]').first();
    const id = (await cierre.getAttribute('action')).split('/').pop();
    await cierre.locator('summary').click();
    await cierre.getByRole('button', { name: /^Sí, cerrar/ }).click();
    await page.waitForLoadState('networkidle');
    await page.goto('/misEmpleos?tab=cerradas');
    const reabrir = page.locator(`form[action$="/empleos/reabrir/${id}"]`);
    await expect(reabrir).toHaveCount(1);
    await reabrir.getByRole('button').first().click();
    await page.waitForLoadState('networkidle');
    await page.goto('/misEmpleos');
    await expect(page.locator(`form[action$="/empleos/cerrar/${id}"]`)).toHaveCount(1);
  });

  test('rechaza una postulación y luego la devuelve a revisión', async ({ page }) => {
    await page.goto('/candidatos');
    const id = await page.locator('article.rq[data-estado="revision"]').first().getAttribute('data-id');
    const tarjeta = page.locator(`article.rq[data-id="${id}"]`);
    await tarjeta.getByRole('button', { name: /^Marcar como no avanza/ }).click();
    await expect(tarjeta).toHaveAttribute('data-estado', 'rechazada');
    await tarjeta.getByRole('button', { name: /a revisi[oó]n/i }).first().click();
    await expect(tarjeta).toHaveAttribute('data-estado', 'revision');
  });

  test('invita a un talento desde su perfil', async ({ page }) => {
    await page.goto('/talentos?q=Natalia');
    await page.locator('main a[href^="/talentos/"]').first().click();
    await page.locator('summary', { hasText: 'Invitar a este talento' }).click();
    await page.locator('#invMsg').fill('Hola, nos encantaría conversar contigo. (prueba automática)');
    await page.getByRole('button', { name: /enviar invitaci/i }).click();
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toContainText(/invitaci[oó]n/i);
  });

  test('el plan PRO no cobra colocaciones', async ({ page }) => {
    await page.goto('/facturacion');
    await expect(page.locator('main')).toContainText('Sin cobros');
  });
});

test('la empresa del plan gratis ve su cobro de colocación', async ({ page }) => {
  await entrarDemo(page, 'startup');
  await page.goto('/facturacion');
  await expect(page.locator('main')).toContainText('$5,00');
});
