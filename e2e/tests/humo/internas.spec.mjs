import { test, expect } from '@playwright/test';
import { vigilar, sinDesborde, entrarDemo } from '../apoyo.mjs';

const TALENTO = ['/dashboard', '/empleos', '/empleos/guardados', '/solicitudes', '/proyectos', '/talentos', '/perfil', '/curriculum', '/ajustes',
  '/analytics', '/mensajes', '/subirProyecto', '/cambiarPassword'];
const EMPRESA = ['/dashboard', '/misEmpleos', '/candidatos', '/crearEmpleo', '/talentos', '/perfil', '/analytics', '/mensajes', '/ajustes', '/facturacion'];

for (const [rol, rutas] of [['talento', TALENTO], ['empresa', EMPRESA]]) {
  test.describe(`como ${rol}`, () => {
    test.beforeEach(async ({ page }) => { await entrarDemo(page, rol); });

    for (const ruta of rutas) {
      test(`${ruta}: carga sin errores`, async ({ page }) => {
        const problemas = vigilar(page);
        const r = await page.goto(ruta);
        expect(r.status()).toBe(200);
        await page.waitForLoadState('networkidle');
        expect(problemas, problemas.join('\n')).toEqual([]);
      });

      test(`${ruta}: sin desborde en celular (375 px) y en 320 px`, async ({ page }) => {
        for (const ancho of [375, 320]) {
          await page.setViewportSize({ width: ancho, height: 760 });
          await page.goto(ruta);
          await page.waitForLoadState('networkidle');
          await sinDesborde(page);
        }
      });
    }

    test('la barra lateral muestra el modo demo', async ({ page }) => {
      await page.goto('/dashboard');
      await expect(page.locator('.demo-card')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Reiniciar mi demo' })).toBeVisible();
    });
  });
}
