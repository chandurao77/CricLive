#!/usr/bin/env bash
# Start the backing infrastructure, the three services (dev profile) and the web app.
# Requires: docker, Java 21, Maven, Node 20+.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
COMPOSE="docker compose -f infra/docker/docker-compose.yml"

echo "==> Starting infrastructure (postgres, mongo, redis, kafka)..."
$COMPOSE up -d postgres mongo redis zookeeper kafka
for svc in postgres mongo redis kafka; do
  echo -n "  waiting for $svc "
  until [ "$($COMPOSE ps --format '{{.Health}}' "$svc" 2>/dev/null)" = "healthy" ]; do echo -n "."; sleep 3; done
  echo " ok"
done

PIDS=()
for svc in match-service scoring-service commentary-service; do
  echo "==> Starting $svc..."
  (cd "services/$svc" && SPRING_PROFILES_ACTIVE=dev mvn -q spring-boot:run) &
  PIDS+=($!)
done

echo "==> Starting web..."
(cd frontend/web && { [ -d node_modules ] || npm ci; } && npm run dev -- --host) &
PIDS+=($!)

cat <<MSG

All services starting (give the JVMs ~30s). Press Ctrl+C to stop.
  Web:         http://localhost:5173
  match-service    :8082   scoring-service :8083   commentary-service :8084

Scorer token:   ./scripts/mint-token.sh SCORER
Simulate match: node scripts/simulate-match.mjs
MSG

trap 'kill "${PIDS[@]}" 2>/dev/null; exit 0' INT TERM
wait
