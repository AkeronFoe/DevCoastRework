#!/usr/bin/env bash
# Chequeo previo al evento. Termina con «TODO LISTO» (código 0) o con la lista de lo que falla (código 1).
#   BASE_URL=https://mi-dominio.example ./feria/chequeo.sh
# Opcional: CLOUDINARY_CLOUD_NAME / CLOUDINARY_API_KEY / CLOUDINARY_API_SECRET para probar la subida de imágenes (o un archivo .env en la raíz).
set -u
cd "$(dirname "$0")/.."
if [ -f .env ]; then set -a; . ./.env; set +a; fi
BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
FALLAS=()
AVISOS=()

ok()    { printf '  \033[32m✔\033[0m %s\n' "$1"; }
falla() { printf '  \033[31m✘\033[0m %s\n' "$1"; FALLAS+=("$1"); }
aviso() { printf '  \033[33m!\033[0m %s\n' "$1"; AVISOS+=("$1"); }
campo() { sed -n "s/.*\"$1\":\"\{0,1\}\([^\",}]*\).*/\1/p" <<<"$2" | head -1; }

echo "Chequeo de DevCoast en $BASE_URL"

echo "1. La app y la base"
SALUD="$(curl -sS -m 15 -D "$TMP/cab" "$BASE_URL/salud" 2>"$TMP/err")" || { falla "La app no responde en $BASE_URL ($(head -c 120 "$TMP/err"))"; SALUD=""; }
if [ -n "$SALUD" ]; then
  [ "$(campo estado "$SALUD")" = "ok" ] && ok "/salud responde ok" || falla "/salud no está ok: $SALUD"
  [ "$(campo mongo "$SALUD")" = "ok" ] && ok "MongoDB responde" || falla "MongoDB no responde"
  [ "$(campo perfil "$SALUD")" = "feria" ] && ok "perfil feria activo" || falla "La app no corre con DEVCOAST_PERFIL=feria (perfil: $(campo perfil "$SALUD"))"
  E="$(campo empresas "$SALUD")"; T="$(campo talentos "$SALUD")"; O="$(campo ofertas "$SALUD")"
  csvfilas() { awk -F, 'BEGIN{n=0} /^#/ {next} NF>0 && !/^[[:space:]]*$/ {n++} END{print n-1}' "feria/datos/$1" 2>/dev/null || echo 0; }
  EE="$(csvfilas empresas.csv)"; TT="$(csvfilas talentos.csv)"
  [ "${E:-0}" -ge "${EE:-0}" ] && ok "empresas en la base: $E (archivo: ~$EE)" || falla "Hay $E empresas en la base y se esperaban al menos ~$EE"
  [ "${T:-0}" -ge 10 ] && ok "talentos en la base: $T" || falla "Pocos talentos en la base ($T)"
  [ "${O:-0}" -ge 10 ] && ok "ofertas en la base: $O" || falla "Pocas ofertas en la base ($O)"
fi

if [ -z "$SALUD" ]; then
  echo
  printf '\033[31mNO SE PUEDE SEGUIR:\033[0m la app no responde, así que no tiene sentido revisar lo demás.\n'
  echo "Revisa que esté encendida, la dirección (BASE_URL) y la red; luego vuelve a correr el chequeo."
  exit 1
fi

echo "2. Hora del servidor"
if [ -s "$TMP/cab" ]; then
  FECHA="$(grep -i '^date:' "$TMP/cab" | head -1 | cut -d' ' -f2- | tr -d '\r')"
  if [ -n "$FECHA" ]; then
    SRV="$(date -u -d "$FECHA" +%s 2>/dev/null || echo 0)"; LOC="$(date -u +%s)"
    DIF=$(( LOC > SRV ? LOC - SRV : SRV - LOC ))
    [ "$SRV" -gt 0 ] && [ "$DIF" -le 120 ] && ok "la hora del servidor coincide con la de este equipo (diferencia ${DIF} s)" || falla "La hora del servidor difiere ${DIF} s de este equipo"
  else aviso "El servidor no envió la cabecera Date"; fi
fi

echo "3. HTTPS"
case "$BASE_URL" in
  https://*)
    HOST="${BASE_URL#https://}"; HOST="${HOST%%/*}"
    if curl -sS -m 15 -o /dev/null "$BASE_URL/salud" 2>"$TMP/err"; then ok "el certificado HTTPS es válido"; else falla "HTTPS falla: $(head -c 150 "$TMP/err")"; fi
    if command -v openssl >/dev/null 2>&1; then
      FIN="$(echo | openssl s_client -servername "${HOST%%:*}" -connect "${HOST%%:*}:443" 2>/dev/null | openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)"
      if [ -n "$FIN" ]; then
        DIAS=$(( ($(date -u -d "$FIN" +%s) - $(date -u +%s)) / 86400 ))
        [ "$DIAS" -ge 7 ] && ok "el certificado vence en $DIAS días" || falla "El certificado vence en $DIAS días"
      fi
    fi;;
  *) aviso "Se está probando por HTTP ($BASE_URL). En el evento usa HTTPS.";;
esac

echo "4. Entradas de la demo"
entrar() { # rol, texto esperado
  local rol="$1" esperado="$2" jar="$TMP/jar-$1"
  local pag; pag="$(curl -sS -m 15 -c "$jar" "$BASE_URL/demo")" || { falla "No abre /demo"; return; }
  local csrf; csrf="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' <<<"$pag" | head -1)"
  [ -z "$csrf" ] && { falla "/demo no trae el formulario (¿perfil feria apagado?)"; return; }
  local code; code="$(curl -sS -m 15 -b "$jar" -c "$jar" -o /dev/null -w '%{http_code} %{redirect_url}' -X POST --data-urlencode "_csrf=$csrf" "$BASE_URL/demo/$rol")"
  case "$code" in 302*dashboard*) ;; *) falla "La entrada demo «$rol» no lleva al dashboard ($code)"; return;; esac
  local dash; dash="$(curl -sS -m 15 -b "$jar" "$BASE_URL/dashboard")"
  grep -q "$esperado" <<<"$dash" && ok "demo «$rol» entra y muestra «$esperado»" || falla "El dashboard de «$rol» no muestra «$esperado»"
}
entrar talento "Carlos"
entrar empresa "DevCoast Labs"
entrar diseno "Laura"
entrar startup "Marea Studio"

echo "5. Páginas públicas"
for ruta in /about-us /login /register-options /privacidad /terminos /demo; do
  c="$(curl -sS -m 15 -o /dev/null -w '%{http_code}' "$BASE_URL$ruta")"
  [ "$c" = "200" ] && ok "$ruta responde 200" || falla "$ruta responde $c"
done
c="$(curl -sS -m 15 -o /dev/null -w '%{http_code}' "$BASE_URL/feria-img/logos/devcoast-labs.png")"
[ "$c" = "200" ] && ok "las imágenes de la feria se sirven" || falla "No se sirven las imágenes de la feria (/feria-img → $c)"
OG="$(curl -sS -m 15 "$BASE_URL/about-us" | sed -n 's/.*property="og:image" content="\([^"]*\)".*/\1/p' | head -1)"
if [ -n "$OG" ]; then
  c="$(curl -sS -m 15 -o /dev/null -w '%{http_code}' "$OG")"
  [ "$c" = "200" ] && ok "la imagen para compartir el enlace es alcanzable ($OG)" || falla "La imagen para compartir no carga ($OG → $c). Define DEVCOAST_URL_PUBLICA"
fi

echo "6. Subida de imágenes (Cloudinary)"
if [ -n "${CLOUDINARY_CLOUD_NAME:-}" ] && [ -n "${CLOUDINARY_API_KEY:-}" ] && [ -n "${CLOUDINARY_API_SECRET:-}" ] \
   && [ "$CLOUDINARY_CLOUD_NAME" != "x" ] && command -v openssl >/dev/null 2>&1; then
  PNG="$TMP/p.png"; printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' | base64 -d > "$PNG"
  TS="$(date +%s)"; PID="chequeo-$TS"
  FIRMA="$(printf 'public_id=%s&timestamp=%s%s' "$PID" "$TS" "$CLOUDINARY_API_SECRET" | openssl dgst -sha1 | awk '{print $NF}')"
  R="$(curl -sS -m 30 -F "file=@$PNG" -F "api_key=$CLOUDINARY_API_KEY" -F "timestamp=$TS" -F "public_id=$PID" -F "signature=$FIRMA" \
        "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload" 2>&1)"
  if grep -q '"secure_url"' <<<"$R"; then
    ok "Cloudinary aceptó una imagen de prueba"
    FIRMA2="$(printf 'public_id=%s&timestamp=%s%s' "$PID" "$TS" "$CLOUDINARY_API_SECRET" | openssl dgst -sha1 | awk '{print $NF}')"
    curl -sS -m 30 -o /dev/null -F "public_id=$PID" -F "api_key=$CLOUDINARY_API_KEY" -F "timestamp=$TS" -F "signature=$FIRMA2" \
      "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/destroy" && ok "y se borró la imagen de prueba"
  else
    falla "Cloudinary rechazó la imagen de prueba: $(head -c 160 <<<"$R")"
  fi
else
  aviso "OMITIDO: faltan CLOUDINARY_CLOUD_NAME / CLOUDINARY_API_KEY / CLOUDINARY_API_SECRET (o openssl)."
fi

echo
if [ "${#FALLAS[@]}" -eq 0 ]; then
  printf '\033[32mTODO LISTO\033[0m'
  [ "${#AVISOS[@]}" -gt 0 ] && printf ' (con %d aviso(s))' "${#AVISOS[@]}"
  echo; exit 0
fi
printf '\033[31mHAY %d COSA(S) POR ARREGLAR:\033[0m\n' "${#FALLAS[@]}"
for f in "${FALLAS[@]}"; do echo " - $f"; done
exit 1
