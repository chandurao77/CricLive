#!/bin/bash
# ============================================================
# validate-bash.sh — Pre-command safety hook
# Runs before every shell command Claude executes.
# Exit 1 = block the command. Exit 0 = allow it.
# ============================================================

COMMAND="$1"

# Helpers
pass()  { echo "  ✅  $1"; }
warn()  { echo "  ⚠️   $1"; }
fail()  { echo "  ❌  $1"; }

# ── 1. Block destructive commands on production ──────────────
DESTRUCTIVE_PATTERN="(delete|destroy|drop|purge|truncate|wipe|nuke|rm -rf)"
PROD_PATTERN="(production|prod\b|prd)"

if echo "$COMMAND" | grep -qiE "$PROD_PATTERN"; then
  if echo "$COMMAND" | grep -qiE "$DESTRUCTIVE_PATTERN"; then
    echo ""
    echo "❌  BLOCKED — Destructive production command detected"
    echo "────────────────────────────────────────────────────"
    echo "  Command : $COMMAND"
    echo "  Reason  : Direct destructive operations on production are not"
    echo "            allowed through Claude automation."
    echo "  Action  : Run this manually after peer review and approval."
    echo "────────────────────────────────────────────────────"
    exit 1
  fi
fi

# ── 2. Block DROP statements (must go through Flyway) ────────
if echo "$COMMAND" | grep -qiE "\bDROP\s+(TABLE|DATABASE|SCHEMA|INDEX)\b"; then
  echo ""
  echo "❌  BLOCKED — Raw DROP statement detected"
  echo "────────────────────────────────────────────────────"
  echo "  Command : $COMMAND"
  echo "  Reason  : Schema changes must go through Flyway migrations."
  echo "  Action  : Create a versioned migration file instead:"
  echo "            V$(date +%Y%m%d%H%M%S)__your_description.sql"
  echo "────────────────────────────────────────────────────"
  exit 1
fi

# ── 3. Block force pushes to protected branches ──────────────
if echo "$COMMAND" | grep -qE "git push.*--force(-with-lease)?"; then
  if echo "$COMMAND" | grep -qE "(main|master|develop|staging|production)"; then
    echo ""
    echo "❌  BLOCKED — Force push to protected branch"
    echo "────────────────────────────────────────────────────"
    echo "  Command : $COMMAND"
    echo "  Reason  : Force-pushing overwrites history and breaks teammates."
    echo "  Action  : Create a revert commit instead."
    echo "────────────────────────────────────────────────────"
    exit 1
  fi
fi

# ── 4. Warn on kubectl exec into production ──────────────────
if echo "$COMMAND" | grep -qE "kubectl exec.*-n (production|prod)"; then
  echo ""
  echo "⚠️   WARNING — kubectl exec into production namespace"
  echo "────────────────────────────────────────────────────"
  echo "  Command : $COMMAND"
  echo "  Action  : This has been logged. Proceed only for emergency debugging."
  echo "────────────────────────────────────────────────────"
  # Warning only — allow through
fi

# ── 5. Warn about broad rm -rf ───────────────────────────────
if echo "$COMMAND" | grep -qE "rm\s+-rf\s+[/~]"; then
  echo ""
  echo "⚠️   WARNING — Recursive delete from root/home path"
  echo "────────────────────────────────────────────────────"
  echo "  Command : $COMMAND"
  echo "  Action  : Double-check the path before proceeding."
  echo "────────────────────────────────────────────────────"
  # Warning only — allow through
fi

# ── 6. Block secret leaks via echo ───────────────────────────
if echo "$COMMAND" | grep -qE "echo\s+\\\$?(AWS_SECRET|DB_PASSWORD|API_KEY|JWT_SECRET|PRIVATE_KEY)"; then
  echo ""
  echo "❌  BLOCKED — Potential secret leak via echo"
  echo "────────────────────────────────────────────────────"
  echo "  Command : $COMMAND"
  echo "  Reason  : Echoing secrets exposes them in logs and terminal history."
  echo "  Action  : Use 'printenv VAR_NAME | wc -c' to verify presence only."
  echo "────────────────────────────────────────────────────"
  exit 1
fi

exit 0