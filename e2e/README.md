# Pruebas de recorrido (E2E)

Un navegador real (Chromium, vía Playwright) recorre la app como lo haría una persona y falla si algo se rompe.

```bash
cd e2e && npm ci
# la app tiene que estar corriendo en modo feria: DEVCOAST_PERFIL=feria (y DEVCOAST_FERIA_TOKEN=xxxx para poder reiniciar la demo)
BASE_URL=http://localhost:8080 FERIA_TOKEN=xxxx npm run humo      # ~1 min: todas las páginas cargan sin errores, sin desborde en 375 y 320 px, cabeceras, 404, rutas privadas
BASE_URL=http://localhost:8080 FERIA_TOKEN=xxxx npm run flujos    # ~2 min: postular, aceptar, campana, chat, guardar, invitaciones, publicar oferta, cerrar/reabrir, reinicio, recorrido…
BASE_URL=... FERIA_TOKEN=... npm run todo
```

- **`FERIA_TOKEN`** (el mismo valor que `DEVCOAST_FERIA_TOKEN` de la app) reinicia la demo al empezar cada archivo de flujos, así no dependen del orden ni de corridas anteriores. Sin él corren sobre el estado actual y algunos fallarán si ya se tocó la demo.
- Los flujos **modifican datos** de la demo: no los corras contra un entorno real con gente usándolo. Úsalos contra una copia o antes de abrir el stand (y reinicia después).
- `CHROMIUM=/ruta/a/chromium` usa un Chromium propio en vez del que descarga Playwright (`npx playwright install chromium`).
- Fallos: capturas y trazas en `resultados/`, informe HTML en `informe/` (`npx playwright show-report informe`).
- Para probar con una base que no sea MongoDB de verdad: **FerretDB no implementa `$first`**, y `/mensajes` y `/chat` lo usan, así que fallarán ahí aunque la app esté bien. Usa MongoDB.
