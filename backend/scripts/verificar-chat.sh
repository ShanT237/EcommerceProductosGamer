#!/usr/bin/env bash
# Verificación del chat en vivo: seguridad, aislamiento y flujo en tiempo real.
# Requiere: bash, curl, jq y el backend arrancado.
#   BASE_URL  (por defecto http://localhost:8080/api)
#   FASE      1 = verificación completa (por defecto) · 2 = solo releer el historial tras reiniciar el backend
# Salida: "PASS|FAIL <check>" por comprobación y código de salida != 0 si alguna falla.

set -u

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
FASE="${FASE:-1}"
ESTADO=".chat-verif-state"
PRODUCTO_ID="bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"
VENDEDOR_CORREO="demo@gamerzone.com"
VENDEDOR_CLAVE="GamerZone2026"
CLAVE_COMPRADOR="ChatTest2026"
LOG_V="/tmp/sse-vendedor.log"
LOG_A="/tmp/sse-a.log"
LOG_C="/tmp/sse-c.log"

FALLOS=0
PIDS=()

for dependencia in curl jq; do
  command -v "$dependencia" >/dev/null 2>&1 || { echo "FAIL falta la dependencia: $dependencia"; exit 2; }
done

limpiar() {
  for pid in "${PIDS[@]:-}"; do
    [ -n "$pid" ] && kill "$pid" 2>/dev/null
  done
}
trap limpiar EXIT

pasa() { echo "PASS $1"; }
falla() { echo "FAIL $1${2:+ — $2}"; FALLOS=$((FALLOS + 1)); }

# comprobar <nombre> <esperado> <obtenido>
comprobar() {
  if [ "$2" = "$3" ]; then pasa "$1"; else falla "$1" "esperado '$2', obtenido '$3'"; fi
}

# UUID v4 portable: /proc (Linux), uuidgen, o RANDOM puro (Git Bash en Windows no trae ninguno de los dos).
nuevo_uuid() {
  if [ -r /proc/sys/kernel/random/uuid ]; then
    cat /proc/sys/kernel/random/uuid
  elif command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr 'A-Z' 'a-z'
  else
    local a b c d e f g h ns
    # Cada $(...) es un subproceso que podría heredar la misma semilla: se mezcla el PID y los nanosegundos.
    ns=$(date +%N 2>/dev/null); ns=${ns//[^0-9]/}
    RANDOM=$(( (BASHPID * 7919 + 10#${ns:-0}) % 32768 ))
    a=$(( (RANDOM << 1 | (RANDOM & 1)) & 65535 )); b=$(( (RANDOM << 1 | (RANDOM & 1)) & 65535 ))
    c=$(( (RANDOM << 1 | (RANDOM & 1)) & 65535 )); d=$(( (RANDOM << 1 | (RANDOM & 1)) & 65535 ))
    e=$(( (RANDOM << 1 | (RANDOM & 1)) & 65535 )); f=$(( (RANDOM << 1 | (RANDOM & 1)) & 65535 ))
    g=$(( (RANDOM << 1 | (RANDOM & 1)) & 65535 )); h=$(( (RANDOM << 1 | (RANDOM & 1)) & 65535 ))
    printf '%04x%04x-%04x-4%03x-%x%03x-%04x%04x%04x\n' "$a" "$b" "$c" $((d & 4095)) $((8 + (e & 3))) $((f & 4095)) "$g" "$h" $(( (a ^ h) & 65535 ))
  fi
}

# codigo <METHOD> <ruta> [token] [cuerpo]  → imprime solo el código HTTP
codigo() {
  local metodo="$1" ruta="$2" token="${3:-}" cuerpo="${4:-}"
  local args=(-s -o /dev/null -w "%{http_code}" --max-time 10 -X "$metodo" "$BASE_URL$ruta")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$cuerpo" ] && args+=(-H "Content-Type: application/json" -d "$cuerpo")
  curl "${args[@]}"
}

# cuerpo_http <METHOD> <ruta> <token> [cuerpo]  → imprime el cuerpo de la respuesta
cuerpo_http() {
  local metodo="$1" ruta="$2" token="$3" cuerpo="${4:-}"
  local args=(-s --max-time 10 -X "$metodo" "$BASE_URL$ruta" -H "Authorization: Bearer $token")
  [ -n "$cuerpo" ] && args+=(-H "Content-Type: application/json" -d "$cuerpo")
  curl "${args[@]}"
}

login() {
  curl -s --max-time 10 -X POST "$BASE_URL/auth/login" -H "Content-Type: application/json" \
    -d "$(jq -n --arg c "$1" --arg p "$2" '{correo:$c, contrasena:$p}')" | jq -r '.token // empty'
}

registrar_comprador() {
  curl -s -o /dev/null --max-time 10 -X POST "$BASE_URL/usuarios" -H "Content-Type: application/json" \
    -d "$(jq -n --arg n "$1" --arg c "$2" --arg p "$CLAVE_COMPRADOR" '{nombre:$n, correo:$c, contrasena:$p}')"
}

# enviar <token> <conversacionId> <clienteId> <contenido>  → cuerpo JSON
enviar() {
  cuerpo_http POST "/chat/conversaciones/$2/mensajes" "$1" \
    "$(jq -n --arg k "$3" --arg c "$4" '{clienteId:$k, contenido:$c}')"
}

# ids_historial <token> <conversacionId> <query>  → ids en una línea, separados por espacio
ids_historial() {
  cuerpo_http GET "/chat/conversaciones/$2/mensajes?$3" "$1" | jq -r '[.mensajes[].id] | join(" ")'
}

# ---------------------------------------------------------------- FASE 2
if [ "$FASE" = "2" ]; then
  [ -f "$ESTADO" ] || { echo "FAIL no existe $ESTADO: ejecuta primero la fase 1"; exit 2; }
  CONV_ID=$(sed -n '1p' "$ESTADO")
  MARCADOR=$(sed -n '2p' "$ESTADO")
  TOKEN_A=$(login "chatA@test.com" "$CLAVE_COMPRADOR")
  [ -n "$TOKEN_A" ] || { echo "FAIL login del comprador A"; exit 1; }
  historial=$(cuerpo_http GET "/chat/conversaciones/$CONV_ID/mensajes?tamano=100" "$TOKEN_A")
  if echo "$historial" | jq -e --arg m "$MARCADOR" '.mensajes | any(.contenido == $m)' >/dev/null 2>&1; then
    pasa "historial persistido tras reinicio"
  else
    falla "historial persistido tras reinicio" "no aparece el mensaje marcador"
  fi
  [ "$FALLOS" -eq 0 ]; exit $?
fi

# ---------------------------------------------------------------- FASE 1
for par in "chatA:chatA@test.com" "chatB:chatB@test.com" "chatC:chatC@test.com"; do
  registrar_comprador "${par%%:*}" "${par#*:}"
done

TOKEN_A=$(login "chatA@test.com" "$CLAVE_COMPRADOR")
TOKEN_C=$(login "chatC@test.com" "$CLAVE_COMPRADOR")
TOKEN_V=$(login "$VENDEDOR_CORREO" "$VENDEDOR_CLAVE")
for t in "$TOKEN_A" "$TOKEN_C" "$TOKEN_V"; do
  [ -n "$t" ] || { echo "FAIL no se pudo iniciar sesión (¿backend arriba en $BASE_URL?)"; exit 1; }
done

# 1. Autenticación obligatoria (sin token y con token basura)
FALSO_ID=$(nuevo_uuid)
for token in "" "basura"; do
  etiqueta=${token:-sin token}
  comprobar "401 GET /chat/stream ($etiqueta)" "401" "$(codigo GET /chat/stream "$token")"
  comprobar "401 GET /chat/conversaciones ($etiqueta)" "401" "$(codigo GET /chat/conversaciones "$token")"
  comprobar "401 POST mensajes ($etiqueta)" "401" \
    "$(codigo POST "/chat/conversaciones/$FALSO_ID/mensajes" "$token" "{\"clienteId\":\"$(nuevo_uuid)\",\"contenido\":\"x\"}")"
done

# 2. Rol: un vendedor no puede iniciar conversaciones
comprobar "403 vendedor no inicia conversaciones" "403" \
  "$(codigo POST /chat/conversaciones "$TOKEN_V" "{\"tipo\":\"PRODUCTO\",\"referenciaId\":\"$PRODUCTO_ID\"}")"

CONV=$(cuerpo_http POST /chat/conversaciones "$TOKEN_A" "{\"tipo\":\"PRODUCTO\",\"referenciaId\":\"$PRODUCTO_ID\"}")
CONV_ID=$(echo "$CONV" | jq -r '.id // empty')
ID_A=$(echo "$CONV" | jq -r '.compradorId // empty')
[ -n "$CONV_ID" ] || { echo "FAIL no se pudo crear la conversación: $CONV"; exit 1; }

# 3. Aislamiento REST: C (ajeno) no puede leer, escribir ni marcar leídos
comprobar "403 ajeno GET mensajes" "403" "$(codigo GET "/chat/conversaciones/$CONV_ID/mensajes" "$TOKEN_C")"
comprobar "403 ajeno POST mensajes" "403" \
  "$(codigo POST "/chat/conversaciones/$CONV_ID/mensajes" "$TOKEN_C" "{\"clienteId\":\"$(nuevo_uuid)\",\"contenido\":\"intruso\"}")"
comprobar "403 ajeno PUT leidos" "403" "$(codigo PUT "/chat/conversaciones/$CONV_ID/leidos" "$TOKEN_C")"

# El body no acepta remitenteId: la identidad sale del JWT
suplantacion=$(cuerpo_http POST "/chat/conversaciones/$CONV_ID/mensajes" "$TOKEN_A" \
  "$(jq -n --arg k "$(nuevo_uuid)" --arg r "$(nuevo_uuid)" '{clienteId:$k, contenido:"sin suplantar", remitenteId:$r}')")
comprobar "remitenteId del body ignorado (identidad del JWT)" "$ID_A" "$(echo "$suplantacion" | jq -r '.remitenteId // empty')"

# El vendedor limpia los pendientes de los pasos anteriores para que el conteo del paso 6 parta de 0.
cuerpo_http PUT "/chat/conversaciones/$CONV_ID/leidos" "$TOKEN_V" >/dev/null

# 4. Aislamiento del stream: solo los participantes reciben el mensaje
: > "$LOG_V"; : > "$LOG_A"; : > "$LOG_C"
curl -N -s --max-time 60 -H "Authorization: Bearer $TOKEN_V" "$BASE_URL/chat/stream" >"$LOG_V" 2>&1 & PIDS+=("$!")
curl -N -s --max-time 60 -H "Authorization: Bearer $TOKEN_A" "$BASE_URL/chat/stream" >"$LOG_A" 2>&1 & PIDS+=("$!")
curl -N -s --max-time 60 -H "Authorization: Bearer $TOKEN_C" "$BASE_URL/chat/stream" >"$LOG_C" 2>&1 & PIDS+=("$!")
sleep 1

CLIENTE_1=$(nuevo_uuid)
enviar "$TOKEN_A" "$CONV_ID" "$CLIENTE_1" "Hola desde A" >/dev/null
sleep 2
if grep -qE '^event: ?mensaje' "$LOG_V"; then pasa "el vendedor recibe event:mensaje por SSE"; else falla "el vendedor recibe event:mensaje por SSE"; fi
if grep -qE '^event: ?mensaje' "$LOG_C"; then falla "el ajeno NO recibe mensajes por SSE" "recibió event:mensaje"; else pasa "el ajeno NO recibe mensajes por SSE"; fi

# 6. No leídos: 1 antes de leer, 0 después
no_leidos() {
  cuerpo_http GET /chat/conversaciones "$TOKEN_V" | jq -r --arg id "$CONV_ID" '.[] | select(.id == $id) | .noLeidos'
}
ANTES=$(no_leidos)
comprobar "noLeidos = 1 antes de leer" "1" "$ANTES"

# 5. Tiempo real + leídos: al leer el vendedor, A recibe event:leidos
MARCADOS=$(cuerpo_http PUT "/chat/conversaciones/$CONV_ID/leidos" "$TOKEN_V" | jq -r '.marcados // empty')
comprobar "PUT leidos marca 1 mensaje" "1" "$MARCADOS"
sleep 2
if grep -qE '^event: ?leidos' "$LOG_A"; then pasa "A recibe event:leidos por SSE"; else falla "A recibe event:leidos por SSE"; fi
comprobar "noLeidos = 0 después de leer" "0" "$(no_leidos)"
comprobar "PUT leidos repetido marca 0" "0" "$(cuerpo_http PUT "/chat/conversaciones/$CONV_ID/leidos" "$TOKEN_V" | jq -r '.marcados // empty')"

# 7. Idempotencia: reenviar el mismo clienteId no crea otro mensaje
TOTAL_ANTES=$(ids_historial "$TOKEN_A" "$CONV_ID" "tamano=100" | wc -w)
R1=$(enviar "$TOKEN_A" "$CONV_ID" "$CLIENTE_1" "Hola desde A" | jq -r '.id // empty')
TOTAL_DESPUES=$(ids_historial "$TOKEN_A" "$CONV_ID" "tamano=100" | wc -w)
comprobar "reenvío idempotente: mismo número de mensajes" "$TOTAL_ANTES" "$TOTAL_DESPUES"
HISTORIAL=$(cuerpo_http GET "/chat/conversaciones/$CONV_ID/mensajes?tamano=100" "$TOKEN_A")
comprobar "reenvío idempotente: devuelve el mismo id" \
  "$(echo "$HISTORIAL" | jq -r --arg k "$CLIENTE_1" '.mensajes[] | select(.clienteId == $k) | .id')" "$R1"

# 8. Paginación por cursor
# Texto solo ASCII: en Git Bash los argumentos con tildes pasan por la página de códigos de Windows y pueden corromperse.
for i in 1 2 3 4 5; do
  respuesta=$(enviar "$TOKEN_A" "$CONV_ID" "$(nuevo_uuid)" "Mensaje de paginacion $i")
  [ -n "$(echo "$respuesta" | jq -r '.id // empty' 2>/dev/null)" ] || falla "envío del mensaje de paginación $i" "respuesta: $respuesta"
  sleep 0.05
done
TODOS=($(ids_historial "$TOKEN_A" "$CONV_ID" "tamano=100"))
N=${#TODOS[@]}
PAG1_JSON=$(cuerpo_http GET "/chat/conversaciones/$CONV_ID/mensajes?tamano=2" "$TOKEN_A")
PAG1=($(echo "$PAG1_JSON" | jq -r '[.mensajes[].id] | join(" ")'))
comprobar "tamano=2 devuelve 2 mensajes" "2" "${#PAG1[@]}"
comprobar "tamano=2 con más mensajes: hayMas=true" "true" "$(echo "$PAG1_JSON" | jq -r '.hayMas')"
comprobar "la primera página son los 2 más recientes, en orden ascendente" \
  "${TODOS[$((N-2))]} ${TODOS[$((N-1))]}" "${PAG1[*]}"
PAG2=($(ids_historial "$TOKEN_A" "$CONV_ID" "tamano=2&antesDe=${PAG1[0]}"))
comprobar "antesDe devuelve los 2 anteriores sin repetir" \
  "${TODOS[$((N-4))]} ${TODOS[$((N-3))]}" "${PAG2[*]}"
POSTERIORES=($(ids_historial "$TOKEN_A" "$CONV_ID" "tamano=2&despuesDe=${TODOS[0]}"))
comprobar "despuesDe devuelve los siguientes en ascendente" \
  "${TODOS[1]} ${TODOS[2]}" "${POSTERIORES[*]}"
comprobar "422 si se piden antesDe y despuesDe a la vez" "422" \
  "$(codigo GET "/chat/conversaciones/$CONV_ID/mensajes?antesDe=${TODOS[1]}&despuesDe=${TODOS[0]}" "$TOKEN_A")"

# 9. Unicode (detecta una MariaDB que no sea utf8mb4). El cuerpo y la comparación usan escapes \u (solo ASCII)
# para que ni la consola de Windows ni jq.exe alteren el texto: lo que se prueba es la base de datos.
CLIENTE_U=$(nuevo_uuid)
cuerpo_http POST "/chat/conversaciones/$CONV_ID/mensajes" "$TOKEN_A" \
  "{\"clienteId\":\"$CLIENTE_U\",\"contenido\":\"Hola \\ud83c\\udfae \\u00f1and\\u00fa\"}" >/dev/null
RELEIDO=$(cuerpo_http GET "/chat/conversaciones/$CONV_ID/mensajes?tamano=100" "$TOKEN_A" \
  | jq -r --arg k "$CLIENTE_U" '.mensajes[] | select(.clienteId == $k) | .contenido == "Hola \ud83c\udfae \u00f1and\u00fa"')
comprobar "Unicode (emoji, ñ, tilde) se guarda y se relee idéntico" "true" "$RELEIDO"

# Marcador para la fase 2 (persistencia tras reiniciar el backend)
MARCADOR="marcador-persistencia-$(date +%s)"
enviar "$TOKEN_A" "$CONV_ID" "$(nuevo_uuid)" "$MARCADOR" >/dev/null
printf '%s\n%s\n' "$CONV_ID" "$MARCADOR" > "$ESTADO"

echo
if [ "$FALLOS" -eq 0 ]; then echo "Todas las comprobaciones pasaron."; else echo "$FALLOS comprobación(es) fallaron."; fi
[ "$FALLOS" -eq 0 ]