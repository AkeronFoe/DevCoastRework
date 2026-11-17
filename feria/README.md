# DevCoast · versión para la feria

Todo lo que existe para la expo-feria: datos curados, demo sin registro, reinicio en un comando, chequeo previo, plan B y material gráfico.
Se activa con una sola variable: **`DEVCOAST_PERFIL=feria`**. Sin ella la app se comporta como siempre.

## Qué hace el modo feria
- **Carga los datos de `feria/datos/`** (empresas, ofertas, talentos, proyectos e imágenes) al arrancar, sin duplicar si se repite. No se siembran los miles de datos
  inventados del seeder: el directorio solo muestra lo curado. Ver `feria/datos/LEEME.md`.
- **Cuentas demo con historia lista** (clave `demo1234`): `demo@devcoast.co` (talento), `diseno@devcoast.co` (talento), `empresa@devcoast.co` (empresa PRO),
  `startup@devcoast.co` (empresa gratis). Cada una trae postulaciones en varios estados, invitaciones, un chat con mensajes sin leer, avisos en la campana y,
  en la empresa gratis, un cobro de colocación.
- **Entrada sin registro**: la landing y `/demo` ofrecen «Probar como talento» y «Probar como empresa». Dentro, el menú lateral muestra «MODO DEMO» con
  **Recorrido** (globos guiados, también en Ajustes) y **Reiniciar** (deja las cuentas demo como al principio).
- **`/salud`** (estado de la app y la base), páginas de **privacidad** y **términos**, pie de página con contacto y **vista previa al compartir el enlace**.

## Variables de entorno

| Variable | Para qué |
| --- | --- |
| `DEVCOAST_PERFIL=feria` | Activa todo lo anterior. |
| `DEVCOAST_FERIA_TOKEN` | Clave para `reiniciar-demo.sh` (POST `/interno/demo/reiniciar`). Sin ella ese endpoint no existe. Usa algo largo y secreto. |
| `DEVCOAST_FERIA_DATOS` | Carpeta de datos (por defecto `feria/datos`, relativa a donde corres la app). |
| `DEVCOAST_URL_PUBLICA` | Dirección pública (`https://mi-dominio.example`) para la imagen al compartir el enlace. Si no, se deduce de la petición. |
| `DEVCOAST_CONTACTO` | Correo que sale en el pie y en las páginas legales. Vacío = no se muestra. |
| `FERIA_REINICIAR=true` | Rehace la historia de las cuentas demo al arrancar (si no, solo se arma la primera vez). |
| `FERIA_SOLO_VALIDAR=true` | Revisa los archivos de datos, imprime el informe y apaga la app (salida 1 si hay errores). |
| `FERIA_SIMULADOR=true` | Actividad en vivo: cada ~45 s (`FERIA_SIMULADOR_SEGUNDOS`) una postulación, un mensaje o una invitación nueva en las cuentas demo. |

Además, como siempre: `MONGODB_URI`, `CLOUDINARY_*`, y detrás de un proxy `server.forward-headers-strategy=framework` (si no, los límites de intentos
cuentan la IP del proxy y no la de cada persona).

## Comandos

```bash
./feria/chequeo.sh                    # chequeo previo: app, base, datos, hora, HTTPS, entradas demo, páginas, Cloudinary → «TODO LISTO» o la lista de fallas
./feria/reiniciar-demo.sh             # demo como el primer día (~1 s). Con --registros también borra las cuentas que crearon los visitantes
./feria/respaldo.sh                   # volcado de la base (necesita mongodump)      →  feria/respaldos/
./feria/restaurar.sh <archivo>        # restaura un volcado (pide confirmación)
./feria/plan-b-local.sh               # la app y su propia base en este equipo (Docker), sin internet
cd e2e && npm run qr -- https://mi-dominio/demo   # QR para el stand y el póster  →  feria/material/
cd e2e && npm run capturas            # capturas de las pantallas clave (pc y celular) →  feria/material/capturas/
cd e2e && npm run humo | npm run flujos           # pruebas de recorrido en un navegador real (ver e2e/README.md)
```

Las variables que usan los scripts (`BASE_URL`, `DEVCOAST_FERIA_TOKEN`, `MONGODB_URI`, `CLOUDINARY_*`) se leen del entorno o de un `.env` en la raíz.

## Lista para el día del evento

**Una semana antes**
1. Reemplaza los datos de ejemplo por los reales (`feria/datos/LEEME.md`), con el permiso por escrito de cada persona. Valida con `FERIA_SOLO_VALIDAR=true`.
2. Despliega con HTTPS y las variables de arriba; define `DEVCOAST_URL_PUBLICA` y `DEVCOAST_CONTACTO`.
3. Revisa los textos de `/privacidad` y `/terminos` (son un borrador del equipo) y los datos de contacto.
4. Corre `e2e` contra el despliegue real (`BASE_URL=... FERIA_TOKEN=... npm run todo`) y arregla lo que salga.
5. Genera el QR con la dirección pública e imprímelo.
6. Prepara el plan B en el equipo del stand: descarga las imágenes de Docker (`docker compose -f feria/docker-compose.local.yml pull`), construye el `.jar`, y ensáyalo **sin internet**.

**La víspera / la mañana del evento**
1. `./feria/respaldo.sh` de la base ya preparada (y cópialo a un pendrive).
2. `./feria/reiniciar-demo.sh` (las fechas de la historia son relativas al momento del reinicio).
3. `./feria/chequeo.sh` desde la red del evento. Debe terminar en «TODO LISTO».
4. Revisa en la red real los límites de intentos de login (5 por cuenta y equipo) y de registro (40 cada 15 min por equipo): en una feria muchas personas salen por la misma IP.
5. Con `FERIA_SIMULADOR=true` el dashboard de la empresa y la campana se ven vivos; apágalo si prefieres una demo estática.

**Durante:** `/salud` te dice si algo se cae; «Reiniciar» en el menú o el script si los visitantes dejan la demo desordenada.

**Después**
1. `./feria/respaldo.sh` para conservar lo que pasó (registros, mensajes).
2. Quita `DEVCOAST_PERFIL=feria` (o `./feria/reiniciar-demo.sh --registros` si solo quieres limpiar). Las cuentas demo tienen clave pública (`demo1234`): cámbiala o bórralas.
3. Decide qué se conserva de lo cargado; las cuentas curadas con contraseña aleatoria no se pueden usar para entrar.

## Notas y límites
- La demo es **compartida**: dos visitantes en la misma cuenta ven los cambios del otro. «Reiniciar» (con un límite de uno cada 20 s) lo arregla para todos.
- Los **registros** que hagan los visitantes quedan guardados: decide con la organización si se conservan; `--registros` los borra.
- La **subida de imágenes** usa Cloudinary y no funciona sin internet. Las imágenes de `feria/datos/imagenes` se sirven desde el disco, sin Cloudinary.
- El simulador solo toca cuentas demo y talentos de los archivos; nunca cuentas creadas por visitantes.
- Si cambias el nombre del `.jar` o la versión, ajusta `feria/docker-compose.local.yml`.
