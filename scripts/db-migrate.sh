#!/usr/bin/env bash
# Run Flyway migrations for a given service.
# Usage: ./scripts/db-migrate.sh match-service
set -euo pipefail

SERVICE="${1:-match-service}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Running Flyway migration for $SERVICE..."
cd "$ROOT/services/$SERVICE"
mvn flyway:migrate -Dspring.profiles.active=dev
echo "Migration complete for $SERVICE."
