#!/usr/bin/env bash
# Plan B si falla internet o el hosting: la app corriendo en este equipo, con su propia base, en modo feria.
# Necesita Docker (docker compose) y el .jar construido:   ./mvnw -DskipTests package
#   ./feria/plan-b-local.sh            levanta todo en http://localhost:8080
#   ./feria/plan-b-local.sh down       lo apaga (la base queda guardada en un volumen)
set -eu
cd "$(dirname "$0")/.."
command -v docker >/dev/null 2>&1 || { echo "Necesitas Docker instalado para el plan B." >&2; exit 2; }
if [ "${1:-}" = "down" ]; then docker compose -f feria/docker-compose.local.yml down; exit 0; fi
ls target/devcoast-*.jar >/dev/null 2>&1 || { echo "Falta el .jar. Corre primero: ./mvnw -DskipTests package" >&2; exit 2; }
docker compose -f feria/docker-compose.local.yml up -d
echo "Esperando a la app..."
for _ in $(seq 1 60); do
  if curl -fsS -m 3 http://localhost:8080/salud >/dev/null 2>&1; then
    echo "Listo: http://localhost:8080  (prueba el chequeo con ./feria/chequeo.sh)"; exit 0
  fi
  sleep 2
done
echo "La app no respondió a tiempo. Mira los registros: docker compose -f feria/docker-compose.local.yml logs app" >&2
exit 1
