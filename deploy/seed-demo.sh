#!/usr/bin/env bash
# Creates the two demo companies and the six demo accounts (safe to run again).
set -uo pipefail
cd "$(dirname "$0")/../backend"
GW="${GW:-http://127.0.0.1:8080}"
envval(){ grep -E "^$1=" .env | head -1 | cut -d= -f2-; }
ADMIN_EMAIL="$(envval BOOTSTRAP_ADMIN_EMAIL)"; ADMIN_EMAIL="${ADMIN_EMAIL:-admin@test.local}"
ADMIN_PASSWORD="$(envval BOOTSTRAP_ADMIN_PASSWORD)"; ADMIN_PASSWORD="${ADMIN_PASSWORD:-TestAdmin123!}"
JAR="$(mktemp)"
J='Content-Type: application/json'
curl -sf -c "$JAR" -H "$J" -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" "$GW/api/v1/auth/login" >/dev/null || { echo "Admin login failed"; exit 1; }

company(){ # name address lat lon
  local id
  id="$(curl -s -b "$JAR" "$GW/api/v1/companies" | jq -r --arg n "$1" '.[]|select(.name==$n)|.id' | head -1)"
  if [ -z "$id" ]; then
    id="$(curl -s -b "$JAR" -H "$J" -d "{\"name\":\"$1\",\"address\":\"$2\",\"industryType\":\"EDUCATION_DEMO\",\"contactEmail\":\"demo@optigrid.local\",\"contactPhone\":\"0000000000\",\"latitude\":$3,\"longitude\":$4}" "$GW/api/v1/companies" | jq -r .id)"
    echo "company created: $1" >&2
  else
    echo "company exists : $1" >&2
  fi
  echo "$id"
}
A="$(company 'OptiGrid School Demo' Pretoria -25.75 28.23)"
B="$(company 'OptiGrid Other Demo' Durban -29.86 31.02)"
[ -n "$A" ] && [ "$A" != null ] || { echo "Could not get company A id"; exit 1; }

user(){ # first last email password role companyId
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' -b "$JAR" -H "$J" -d "{\"firstName\":\"$1\",\"lastName\":\"$2\",\"email\":\"$3\",\"password\":\"$4\",\"role\":\"$5\",\"companyId\":\"$6\"}" "$GW/api/v1/auth/register")"
  case "$code" in 200|201) echo "user created   : $3";; 409) echo "user exists    : $3";; *) echo "user FAILED ($code): $3";; esac
}
user Demo Manager   demo-manager@optigrid.local    'DemoManager!2026'  ENERGY_MANAGER "$A"
user Demo Owner     demo-owner@optigrid.local      'DemoOwner!2026'    BUSINESS_OWNER "$A"
user Demo Technician demo-technician@optigrid.local 'DemoTech!2026'     TECHNICIAN     "$A"
user Demo Viewer    demo-viewer@optigrid.local     'DemoViewer!2026'   VIEWER         "$A"
user Demo ManagerB  demo-manager-b@optigrid.local  'DemoManagerB!2026' ENERGY_MANAGER "$B"
echo "Company A id: $A"; echo "Company B id: $B"
