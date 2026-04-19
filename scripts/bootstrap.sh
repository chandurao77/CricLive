#!/usr/bin/env bash
# Bootstrap local development environment.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Checking toolchain..."
command -v java   >/dev/null 2>&1 || { echo "ERROR: Java 21 not found. Install via SDKMAN or Homebrew."; exit 1; }
command -v mvn    >/dev/null 2>&1 || { echo "ERROR: Maven not found."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "ERROR: Docker not found."; exit 1; }
command -v pnpm   >/dev/null 2>&1 || { echo "ERROR: pnpm not found. Run: npm i -g pnpm"; exit 1; }

echo "==> Copying .env.example -> .env (if not present)..."
[ -f .env ] || cp .env.example .env && echo "  Created .env — edit secrets before starting."

echo "==> Pulling Docker images..."
docker compose -f infra/docker/docker-compose.yml pull

echo "==> Installing frontend dependencies..."
cd frontend/web && pnpm install && cd "$ROOT"

echo ""
echo "Bootstrap complete. Next steps:"
echo "  1. Edit .env with your passwords"
echo "  2. docker compose -f infra/docker/docker-compose.yml up -d"
echo "  3. ./scripts/dev-up.sh"
