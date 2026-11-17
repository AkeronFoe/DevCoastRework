import { defineConfig, devices } from '@playwright/test';

// BASE_URL: dónde corre la app (por defecto la local). FERIA_TOKEN: si está, cada corrida empieza reiniciando la demo.
// CHROMIUM: ruta de un Chromium propio, si no quieres el que descarga Playwright.
const baseURL = process.env.BASE_URL || 'http://localhost:8080';
const ejecutable = process.env.CHROMIUM || undefined;

export default defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'informe' }]],
  outputDir: 'resultados',
  globalSetup: './global-setup.mjs',
  use: {
    baseURL,
    locale: 'es-CO',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    launchOptions: ejecutable ? { executablePath: ejecutable } : {},
  },
  projects: [
    { name: 'humo', testDir: './tests/humo', use: { ...devices['Desktop Chrome'] } },
    { name: 'flujos', testDir: './tests/flujos', use: { ...devices['Desktop Chrome'] } },
  ],
});
