#!/usr/bin/env bash
# Start infrastructure + all services in dev mode.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Starting infrastructure..."
docker compose -f infra/docker/docker-compose.yml up -d

echo "==> Waiting for Postgres to be ready..."
until docker compose -f infra/docker/docker-compose.yml exec -T postgres \
  pg_isready -U cricklive >/dev/null 2>&1; do sleep 2; done
echo "  Postgres ready."

echo "==> Waiting for Kafka to be ready..."
sleep 10  # Kafka needs a few seconds after ZooKeeper

echo "==> Starting match-service..."
cd services/match-service
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -q &
MATCH_PID=$!
cd "$ROOT"

echo "==> Starting scoring-service..."
cd services/scoring-service
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -q &
SCORING_PID=$!
cd "$ROOT"

echo "==> Starting commentary-service..."
cd services/commentary-service
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -q &
COMMENTARY_PID=$!
cd "$ROOT"

echo "==> Starting frontend..."
cd frontend/web
pnpm dev &
FRONTEND_PID=$!
cd "$ROOT"

echo ""
echo "All services started. Press Ctrl+C to stop."
echo "  Frontend:          http://localhost:5173"
echo "  match-service API: http://localhost:8082/swagger-ui.html"
echo "  Kafka UI:          http://localhost:8090"
echo "  pgAdmin:           http://localhost:5050"

trap "kill $MATCH_PID $SCORING_PID $COMMENTARY_PID $FRONTEND_PID 2>/dev/null; exit 0" SIGINT SIGTERM
wait
