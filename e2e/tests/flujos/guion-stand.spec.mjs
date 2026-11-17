import { test, expect } from '@playwright/test';
import { entrarDemo, reiniciarDemo } from '../apoyo.mjs';

// el guion de 2 a 3 minutos del stand, de punta a punta: talento se postula → empresa acepta → talento ve la campana y chatea
test.describe.configure({ mode: 'serial' });

const OFERTA = 'Full Stack Developer (Java + React)';
let talento, empresa;

test.beforeAll(async ({ browser, request }) => {
  await reiniciarDemo(request);
  talento = await (await browser.newContext()).newPage();
  empresa = await (await browser.newContext()).newPage();
  await entrarDemo(talento, 'talento');
  await entrarDemo(empresa, 'empresa');
});
test.afterAll(async () => { await talento.context().close(); await empresa.context().close(); });

test('1. el talento ve primero la oferta de mejor match y se postula', async () => {
  await talento.goto('/empleos');
  const destacada = talento.locator('main').getByRole('heading', { name: OFERTA }).first();
  await expect(destacada).toBeVisible();
  await talento.getByRole('button', { name: 'Postularme' }).first().click();
  await expect(talento.locator('#toastTxt')).toContainText('Solicitud enviada a DevCoast Labs');
});

test('2. la empresa ve llegar la postulación y la acepta', async () => {
  await empresa.goto('/candidatos');
  const tarjeta = empresa.locator('article.rq[data-nombre="Carlos Andrés Ramírez"]').filter({ hasText: 'se postuló hoy' });
  await expect(tarjeta).toHaveCount(1);
  await expect(tarjeta).toHaveAttribute('data-estado', 'revision');
  await tarjeta.locator('summary', { hasText: 'Aceptar' }).click();
  await tarjeta.getByRole('button', { name: /Sí, aceptar/ }).click();
  await expect(tarjeta).toHaveAttribute('data-estado', 'aprobada');
});

test('3. la campana del talento avisa y lleva a la solicitud', async () => {
  await talento.goto('/dashboard');
  await talento.locator('#bellBtn').click();
  const aviso = talento.locator('#bellPanel a', { hasText: 'aceptó tu postulación a ' + OFERTA }).first();
  await expect(aviso).toBeVisible();
  await aviso.click();
  await expect(talento).toHaveURL(/\/solicitudes\/[0-9a-f]{24}/);
  await expect(talento.locator('main')).toContainText(OFERTA);
});

test('4. el talento abre el chat y escribe; la empresa lo recibe', async () => {
  await talento.locator('main').getByRole('link', { name: /Escribir a la empresa/ }).click();
  await expect(talento).toHaveURL(/\/chat\/[0-9a-f]{24}/);
  const url = talento.url();
  const texto = 'Hola, gracias por aceptarme. ¡Quedo atento! ' + Date.now();
  await talento.locator('#mxTexto').fill(texto);
  await talento.keyboard.press('Enter');
  await expect(talento.locator('#mxFlow')).toContainText(texto);

  await empresa.goto(url);
  await expect(empresa.locator('#mxFlow')).toContainText(texto);
});
