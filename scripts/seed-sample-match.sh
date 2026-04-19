#!/usr/bin/env bash
# Seed a synthetic IPL 2024 Final match for development/demo.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"

echo "==> Seeding sample match data..."

# The seed SQL (V2 migration) creates teams, venue, series, and one match.
# This script additionally POSTs sample ball events via scoring-service.
echo "  Infrastructure seed data already loaded by Flyway V2 migration."
echo "  Sending 6 sample ball events to scoring-service..."

for ball in 1 2 3 4 5 6; do
  curl -s -X POST "$BASE_URL/score/ball" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${DEV_TOKEN:-dev-token-placeholder}" \
    -d "{
      \"matchId\": \"d1000000-0000-0000-0000-000000000001\",
      \"inningsId\": \"e1000000-0000-0000-0000-00000000000${ball}\",
      \"batterId\": \"f1000000-0000-0000-0000-000000000001\",
      \"bowlerId\": \"f1000000-0000-0000-0000-000000000002\",
      \"runsScored\": ${ball},
      \"wide\": false,
      \"noBall\": false,
      \"bye\": false,
      \"legBye\": false,
      \"extraRuns\": 0,
      \"wicket\": false,
      \"idempotencyKey\": \"seed-ball-${ball}\"
    }" || true
  echo "  Sent ball $ball"
done

echo "Seed complete."
