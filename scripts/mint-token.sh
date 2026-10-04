#!/usr/bin/env bash
# Mint a short-lived HS256 JWT for local development / demos.
#
# Usage:  ./scripts/mint-token.sh [ROLE ...]      (default role: SCORER)
#         JWT_SECRET=... TTL_SECONDS=3600 ./scripts/mint-token.sh ADMIN SCORER
#
# JWT_SECRET must match the one the services run with (the dev profile default is used if unset).
set -euo pipefail

SECRET="${JWT_SECRET:-dev-only-jwt-secret-change-me-0123456789}"
TTL="${TTL_SECONDS:-3600}"
SUBJECT="${TOKEN_SUBJECT:-00000000-0000-0000-0000-00000000dev1}"
ROLES=("${@:-SCORER}")

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

now=$(date +%s)
roles_json=$(printf '"%s",' "${ROLES[@]}")
roles_json="[${roles_json%,}]"

header=$(printf '{"alg":"HS256","typ":"JWT"}' | b64url)
payload=$(printf '{"sub":"%s","roles":%s,"iat":%s,"exp":%s}' "$SUBJECT" "$roles_json" "$now" "$((now + TTL))" | b64url)
signature=$(printf '%s.%s' "$header" "$payload" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64url)

printf '%s.%s.%s\n' "$header" "$payload" "$signature"
