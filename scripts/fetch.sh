#!/usr/bin/env bash
# Download and decrypt one Everybody Codes input.
#
#   scripts/fetch.sh <year> <quest> <part>
#
# Verified against the live site:
#   input  https://everybody.codes/assets/<year>/<quest>/input/<seed>.json
#          JSON {"1": hex, "2": hex, "3": hex} — all parts, locked or not
#   keys   https://api.everybody.codes/event/<year>/quest/<quest>
#          cookie everybody-codes=<EC_TOKEN>; key<P> appears once part P is unlocked
#   cipher AES-256-CBC, key = the 32-char key string, IV = its first 16 chars
#
# Key source, in order:
#   1. keys/<year>/quest<NN>_part<P>.key   saved with: make key Q=NN P=P
#   2. the keys API
set -euo pipefail

YEAR="$1"; QUEST="$2"; PART="$3"
QNUM=$((10#$QUEST))
OUT="inputs/$YEAR/quest${QUEST}_part${PART}.txt"
KEYFILE="keys/$YEAR/quest${QUEST}_part${PART}.key"

for tool in curl openssl xxd python3; do
  command -v "$tool" >/dev/null || { echo "missing tool: $tool"; exit 1; }
done

if [ -f .env ]; then set -a; . ./.env; set +a; fi
: "${EC_SEED:?set EC_SEED in .env}"

if [ -s "$OUT" ] && [ "${FORCE:-}" != "1" ]; then
  echo "$OUT already present. FORCE=1 to overwrite."
  exit 0
fi

tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT

# field <file> <name> <what> — JSON lookup that fails with a sentence, not a traceback.
field() {
  python3 - "$1" "$2" "$3" <<'PY'
import json, sys
path, name, what = sys.argv[1:]
raw = open(path, encoding="utf-8", errors="replace").read()
try:
    data = json.loads(raw)
except json.JSONDecodeError:
    start = raw.strip()[:1] or "(empty)"
    sys.exit(f"{what}: response is not JSON (starts with {start!r})")
print(data.get(name, ""))
PY
}

# Seconds until EC_TOKEN (a JWT) expires; prints nothing if it can't be read.
token_seconds_left() {
  python3 - <<'PY'
import base64, json, os, time
try:
    payload = os.environ["EC_TOKEN"].split(".")[1]
    payload += "=" * (-len(payload) % 4)
    print(int(json.loads(base64.urlsafe_b64decode(payload))["exp"] - time.time()))
except Exception:
    pass
PY
}

# ---------------------------------------------------------------- input ----
curl -fsS -A "${EC_USER_AGENT:-everybody-codes-kotlin}" "https://everybody.codes/assets/$YEAR/$QNUM/input/$EC_SEED.json" -o "$tmp/blob.json"
CIPHER=$(field "$tmp/blob.json" "$PART" "input blob")
[ -n "$CIPHER" ] || { echo "No ciphertext for part $PART in the input blob."; exit 1; }

# ------------------------------------------------------------------ key ----
if [ -s "$KEYFILE" ]; then
  KEY=$(<"$KEYFILE")
  echo "key   $KEYFILE"
else
  if [ -z "${EC_TOKEN:-}" ]; then
    echo "No saved key and no EC_TOKEN. Set EC_TOKEN in .env, or run: make key Q=$QUEST P=$PART"
    exit 1
  fi
  left=$(token_seconds_left)
  if [ -n "$left" ] && [ "$left" -le 0 ]; then
    echo "EC_TOKEN has expired. Copy a fresh everybody-codes cookie value into .env."
    exit 1
  fi
  if [ -n "$left" ] && [ "$left" -lt 86400 ]; then
    echo "note  EC_TOKEN expires in $((left / 3600))h — refresh it soon"
  fi
  curl -fsS -A "${EC_USER_AGENT:-everybody-codes-kotlin}" --cookie "${EC_COOKIE:-everybody-codes}=$EC_TOKEN" \
       "https://api.everybody.codes/event/$YEAR/quest/$QNUM" -o "$tmp/keys.json"
  KEY=$(field "$tmp/keys.json" "key$PART" \
        "keys API (check EC_TOKEN in .env, or run: make key Q=$QUEST P=$PART)")
  [ -n "$KEY" ] || { echo "No key$PART yet — solve and submit part $((PART - 1)) on the site first."; exit 1; }
  echo "key   API"
fi

[ "${#KEY}" -eq 32 ] || { echo "Key is ${#KEY} characters; expected 32."; exit 1; }

# -------------------------------------------------------------- decrypt ----
if ! printf '%s' "$CIPHER" | xxd -r -p \
     | openssl enc -d -aes-256-cbc \
         -K  "$(printf '%s' "$KEY"        | xxd -p -c 256)" \
         -iv "$(printf '%s' "${KEY:0:16}" | xxd -p -c 256)" \
         > "$tmp/plain" 2> "$tmp/err"; then
  echo "Decrypt failed: $(head -1 "$tmp/err")"
  exit 1
fi

mkdir -p "inputs/$YEAR"
mv "$tmp/plain" "$OUT"
echo "wrote $OUT ($(awk 'END { print NR }' "$OUT") lines)"
