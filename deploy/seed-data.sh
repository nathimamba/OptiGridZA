#!/usr/bin/env bash
# Adds realistic demo data: three more companies at different battery states, a resolved ticket with an
# acknowledged alert for the School Demo company, open and in-progress tickets elsewhere, and prediction records.
# The School Demo battery is charged back to 100% at the end, ready for the live demo. Safe to run again.
set -uo pipefail
cd "$(dirname "$0")/../backend"
GW="${GW:-http://127.0.0.1:8080}"
envval(){ grep -E "^$1=" .env | head -1 | cut -d= -f2-; }
ADMIN_EMAIL="$(envval BOOTSTRAP_ADMIN_EMAIL)"; ADMIN_EMAIL="${ADMIN_EMAIL:-admin@test.local}"
ADMIN_PASSWORD="$(envval BOOTSTRAP_ADMIN_PASSWORD)"; ADMIN_PASSWORD="${ADMIN_PASSWORD:-TestAdmin123!}"
JAR="$(mktemp)"; J='Content-Type: application/json'
api(){ curl -s -b "$JAR" -H "$J" "$@"; }
curl -sf -c "$JAR" -H "$J" -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" "$GW/api/v1/auth/login" >/dev/null || { echo "Admin login failed"; exit 1; }

if [ -z "${FORCE:-}" ] && api "$GW/api/v1/companies" | jq -e '.[]|select(.name=="Pretoria Bakery")' >/dev/null 2>&1; then echo 'Seed data already present. Set FORCE=1 to run anyway.'; exit 0; fi
company(){ local id; id="$(api "$GW/api/v1/companies" | jq -r --arg n "$1" '.[]|select(.name==$n)|.id' | head -1)"
  if [ -z "$id" ]; then id="$(api -d "{\"name\":\"$1\",\"address\":\"$2\",\"industryType\":\"$5\",\"contactEmail\":\"demo@optigrid.local\",\"contactPhone\":\"0000000000\",\"latitude\":$3,\"longitude\":$4}" "$GW/api/v1/companies" | jq -r .id)"; echo "company created: $1" >&2; else echo "company exists : $1" >&2; fi; echo "$id"; }
user(){ local c; c="$(api -o /dev/null -w '%{http_code}' -d "{\"firstName\":\"$1\",\"lastName\":\"$2\",\"email\":\"$3\",\"password\":\"$4\",\"role\":\"$5\",\"companyId\":\"$6\"}" "$GW/api/v1/auth/register")"; echo "  user $3 -> $c"; }
run(){ api -o /dev/null -d "{\"companyId\":\"$1\",\"action\":\"$2\",\"kwh\":$3,\"mode\":\"$4\"}" "$GW/api/v1/simulation/run"; echo "  $2 $3 kWh"; sleep 3; }
firstopen(){ api "$GW/api/v1/tickets/company/$1" | jq -r '[.[]|select(.status=="OPEN")][0].id // empty'; }

A="$(company 'OptiGrid School Demo' Pretoria -25.75 28.23 EDUCATION_DEMO)"
BAK="$(company 'Pretoria Bakery' Pretoria -25.74 28.19 RETAIL)"
COLD="$(company 'Durban Cold Storage' Durban -29.86 31.02 LIGHT_MANUFACTURING)"
CLI="$(company 'Cape Town Clinic' 'Cape Town' -33.92 18.42 HEALTHCARE)"

echo "Users"
user Bakery Manager   bakery-manager@optigrid.local 'BakeryMgr!2026' ENERGY_MANAGER "$BAK"
user Cold   Manager   cold-manager@optigrid.local   'ColdMgr!2026'   ENERGY_MANAGER "$COLD"
user Cold   Technician cold-tech@optigrid.local     'ColdTech!2026'  TECHNICIAN     "$COLD"
user Clinic Manager   clinic-manager@optigrid.local 'ClinicMgr!2026' ENERGY_MANAGER "$CLI"

echo "School Demo: history, then reset to 100%"
run "$A" DISCHARGE 3 HYBRID; run "$A" CHARGE 2 HYBRID; run "$A" DISCHARGE 8 HYBRID
T="$(firstopen "$A")"
if [ -n "$T" ]; then
  api -o /dev/null -X PUT -d '{"email":"demo-technician@optigrid.local"}' "$GW/api/v1/tickets/$T/assign"
  api -o /dev/null -X PUT -d '{"status":"RESOLVED","resolutionNotes":"Battery inspected, charge restored"}' "$GW/api/v1/tickets/$T/status"
  echo "  ticket $T resolved"
fi
for a in $(api "$GW/api/v1/notifications/alerts/$A" | jq -r '.[]|select(.acknowledged==false)|.id'); do api -o /dev/null -X PUT "$GW/api/v1/notifications/alerts/$a/acknowledge"; done
run "$A" CHARGE 10 HYBRID; run "$A" CHARGE 10 HYBRID

echo "Pretoria Bakery: partly discharged (WARNING range, no alert)"
run "$BAK" DISCHARGE 5 HYBRID

echo "Durban Cold Storage: two critical events, one ticket in progress, one open"
run "$COLD" DISCHARGE 8 HYBRID; run "$COLD" CHARGE 4 HYBRID; run "$COLD" DISCHARGE 7 HYBRID
T="$(api "$GW/api/v1/tickets/company/$COLD" | jq -r '[.[]|select(.status=="OPEN")]|last|.id // empty')"
[ -n "$T" ] && api -o /dev/null -X PUT -d '{"email":"cold-tech@optigrid.local"}' "$GW/api/v1/tickets/$T/assign" && echo "  ticket $T in progress"

echo "Cape Town Clinic: healthy, untouched at 100%"

echo "Prediction records (one recommendation per company)"
for id in "$A" "$BAK" "$COLD" "$CLI"; do api "$GW/api/v1/prediction/recommend/$id" | jq -r '"  \(.companyId // "company") -> \(.action) \(.confidence) \(.operatingMode)"'; done
echo "Done. New logins: bakery-manager / cold-manager / cold-tech / clinic-manager @optigrid.local"
