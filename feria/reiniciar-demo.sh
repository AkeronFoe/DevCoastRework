#!/usr/bin/env bash
# Deja la demo como el primer día: perfiles, ofertas e historia de las cuentas demo, con fechas de hoy (tarda ~1 s).
#   DEVCOAST_FERIA_TOKEN=xxxx BASE_URL=https://mi-dominio.example ./feria/reiniciar-demo.sh
#   ./feria/reiniciar-demo.sh --registros     también borra las cuentas que crearon los visitantes después de la primera carga
# El token es el mismo valor que la app tiene en DEVCOAST_FERIA_TOKEN. Se lee también de un archivo .env en la raíz.
set -eu
cd "$(dirname "$0")/.."
if [ -f .env ]; then set -a; . ./.env; set +a; fi
BASE_URL="${BASE_URL:-http://localhost:8080}"; BASE_URL="${BASE_URL%/}"
TOKEN="${DEVCOAST_FERIA_TOKEN:-}"
[ -n "$TOKEN" ] || { echo "Falta DEVCOAST_FERIA_TOKEN (el mismo valor con el que arrancó la app)." >&2; exit 2; }
REG=false; [ "${1:-}" = "--registros" ] && REG=true
CUERPO="$(mktemp)"; trap 'rm -f "$CUERPO"' EXIT
CODIGO="$(curl -sS -m 60 -o "$CUERPO" -w '%{http_code}' -X POST -H "X-Feria-Token: $TOKEN" "$BASE_URL/interno/demo/reiniciar?registros=$REG")"
cat "$CUERPO"
case "$CODIGO" in
  200) ;;
  403) echo "El token no coincide." >&2; exit 1;;
  404) echo "La app no tiene DEVCOAST_FERIA_TOKEN configurado (o no está en modo feria)." >&2; exit 1;;
  *) echo "Falló con código HTTP $CODIGO." >&2; exit 1;;
esac
