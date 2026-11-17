#!/usr/bin/env bash
# Guarda un volcado de la base ya preparada (feria/respaldos/AAAA-MM-DD_HHMM.archive.gz) y se restaura con restaurar.sh.
# Necesita las «MongoDB Database Tools» (mongodump): https://www.mongodb.com/docs/database-tools/installation/
#   ./feria/respaldo.sh                 usa MONGODB_URI (del entorno o de .env)
set -eu
cd "$(dirname "$0")/.."
if [ -f .env ]; then set -a; . ./.env; set +a; fi
: "${MONGODB_URI:?Falta MONGODB_URI}"
command -v mongodump >/dev/null 2>&1 || { echo "No encuentro mongodump. Instala las MongoDB Database Tools." >&2; exit 2; }
mkdir -p feria/respaldos
ARCHIVO="feria/respaldos/$(date +%Y-%m-%d_%H%M).archive.gz"
mongodump --uri="$MONGODB_URI" --gzip --archive="$ARCHIVO"
echo "Respaldo listo: $ARCHIVO ($(du -h "$ARCHIVO" | cut -f1))"
