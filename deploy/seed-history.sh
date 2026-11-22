#!/usr/bin/env bash
# Generates extra history THROUGH the running system: for each company it sets different load-shedding
# stages, asks the model for a recommendation (stored as a prediction record) and applies it to the
# virtual battery. Run once, after seed-data.sh. Usage: bash deploy/seed-history.sh [rounds-per-company]
set -uo pipefail
cd "$(dirname "$0")/../backend"
GW="${GW:-http://127.0.0.1:8080}"; ROUNDS="${1:-12}"
envval(){ grep -E "^$1=" .env | head -1 | cut -d= -f2-; }
EMAIL="$(envval BOOTSTRAP_ADMIN_EMAIL)"; EMAIL="${EMAIL:-admin@test.local}"
PASS="$(envval BOOTSTRAP_ADMIN_PASSWORD)"; PASS="${PASS:-TestAdmin123!}"
JAR="$(mktemp)"; J='Content-Type: application/json'
api(){ curl -s -b "$JAR" -H "$J" "$@"; }
curl -sf -c "$JAR" -H "$J" -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" "$GW/api/v1/auth/login" >/dev/null || { echo "Admin login failed"; exit 1; }
STAGES=(0 0 2 4 0 6 1 8 0 3 0 5)
for name in 'OptiGrid School Demo' 'Pretoria Bakery' 'Durban Cold Storage' 'Cape Town Clinic'; do
  id="$(api "$GW/api/v1/companies" | jq -r --arg n "$name" '.[]|select(.name==$n)|.id' | head -1)"
  [ -z "$id" ] && { echo "skip (not found): $name"; continue; }
  echo "== $name"
  for ((i=0;i<ROUNDS;i++)); do
    st=${STAGES[$((i % ${#STAGES[@]}))]}
    api -o /dev/null -X POST "$GW/api/v1/etl/demo/grid-status?stage=$st"
    rec="$(api "$GW/api/v1/prediction/recommend/$id")"
    act="$(echo "$rec" | jq -r '.action // "HOLD"')"; mode="$(echo "$rec" | jq -r '.operatingMode // "HYBRID"')"
    case "$act" in CHARGE) kwh=3;; SOLAR_PRIORITY) kwh=1;; DISCHARGE) kwh=2;; *) kwh=0;; esac
    [ "$act" = SOLAR_PRIORITY ] && act=CHARGE
    [ "$kwh" != 0 ] && api -o /dev/null -d "{\"companyId\":\"$id\",\"action\":\"$act\",\"kwh\":$kwh,\"mode\":\"$mode\"}" "$GW/api/v1/simulation/run"
    echo "  stage $st -> $act $kwh kWh ($mode)"; sleep 1
  done
done
api -o /dev/null -X POST "$GW/api/v1/etl/demo/grid-status?stage=0"; echo "Stage reset to 0. Recharge School Demo to 100% before presenting."
