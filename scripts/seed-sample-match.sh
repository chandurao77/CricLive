#!/usr/bin/env bash
# Plays a complete simulated T20I (India v Australia) through the real scoring API.
# The fixture itself (teams, players, venue, match) is created by the dev Flyway migration.
# Env: MATCH_API, SCORING_API, JWT_SECRET, MATCH_ID, DELAY_MS, SEED (see simulate-match.mjs)
set -euo pipefail
exec node "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/simulate-match.mjs" "$@"
