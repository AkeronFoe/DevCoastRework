#!/usr/bin/env bash
# Restaura un respaldo hecho con respaldo.sh sobre la base de MONGODB_URI (REEMPLAZA lo que haya: --drop).
#   ./feria/restaurar.sh feria/respaldos/2026-10-30_0800.archive.gz
set -eu
cd "$(dirname "$0")/.."
if [ -f .env ]; then set -a; . ./.env; set +a; fi
: "${MONGODB_URI:?Falta MONGODB_URI}"
ARCHIVO="${1:-}"
[ -f "$ARCHIVO" ] || { echo "Uso: $0 <archivo.archive.gz>" >&2; exit 2; }
command -v mongorestore >/dev/null 2>&1 || { echo "No encuentro mongorestore. Instala las MongoDB Database Tools." >&2; exit 2; }
echo "Esto REEMPLAZA las colecciones de la base en MONGODB_URI con las del respaldo."
read -r -p "Escribe «si» para continuar: " R
[ "$R" = "si" ] || { echo "Cancelado."; exit 1; }
mongorestore --uri="$MONGODB_URI" --gzip --archive="$ARCHIVO" --drop
echo "Restaurado."
