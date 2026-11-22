#!/usr/bin/env bash
# One-shot deployment of OptiGrid ZA on a fresh Ubuntu VM.
# Usage (from the repo root):  bash deploy/deploy.sh <public-ip-or-domain>
set -euo pipefail
HOSTNAME_OR_IP="${1:?usage: bash deploy/deploy.sh <public-ip-or-domain>}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/backend"

command -v jq >/dev/null || sudo apt-get install -y jq
command -v curl >/dev/null || sudo apt-get install -y curl

echo "== 1. environment file"
[ -f .env ] || cp .env.example .env
setenv(){ if grep -q "^$1=" .env; then sed -i "s#^$1=.*#$1=$2#" .env; else echo "$1=$2" >> .env; fi; }
setenv CORS_ALLOWED_ORIGIN "http://$HOSTNAME_OR_IP"
setenv APP_DATA_FETCH_ENABLED true
setenv COOKIE_SECURE false
echo "   Review backend/.env now: DB_PASSWORD, JWT_SECRET and BOOTSTRAP_ADMIN_PASSWORD must be strong values."

echo "== 2. start the backend in stages (first build takes a while)"
docker compose up -d --build postgres eureka-service
sleep 60
docker compose up -d --build auth-service api-gateway company-service
sleep 90
docker compose up -d --build simulation-service notification-service etl-service prediction-service

echo "== 3. wait until 7 services are registered"
until [ "$(curl -s http://127.0.0.1:8761/eureka/apps -H 'Accept: application/json' | grep -o '"instanceId"' | wc -l)" -ge 7 ]; do echo "   waiting..."; sleep 15; done
sleep 60

echo "== 4. build the website (same-origin API, so no CORS problems)"
cd "$ROOT/frontend"
docker run --rm -v "$PWD":/app -w /app -e VITE_API_URL= node:20-alpine sh -c "npm ci || npm install; npm run build"

echo "== 5. serve the website and proxy /api to the gateway on port 80"
docker rm -f web >/dev/null 2>&1 || true
docker run -d --name web --restart unless-stopped --network backend_optigrid-network -p 80:80 \
  -v "$PWD/dist":/usr/share/nginx/html:ro \
  -v "$ROOT/deploy/nginx.conf":/etc/nginx/conf.d/default.conf:ro nginx:alpine

echo "== 6. demo accounts"
bash "$ROOT/deploy/seed-demo.sh"

echo "== 7. extra demo data (more companies, tickets, history)"
bash "$ROOT/deploy/seed-data.sh" || echo "seed-data.sh had a problem; the system still works"

echo "DONE. Open http://$HOSTNAME_OR_IP"
