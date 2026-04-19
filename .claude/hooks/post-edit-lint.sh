#!/bin/bash
# ============================================================
# post-edit-lint.sh — Post-edit linting & formatting hook
# Runs automatically after Claude writes or edits a file.
# ============================================================

FILE="$1"

if [[ -z "$FILE" ]]; then
  exit 0
fi

pass()  { echo "  ✅  $1"; }
warn()  { echo "  ⚠️   $1"; }
fail()  { echo "  ❌  $1"; }
skip()  { echo "  ⏭️   $1 (not installed)"; }

echo ""
echo "🔍  Post-edit lint: $FILE"
echo "────────────────────────────────────────────────────"

# ── TypeScript / JavaScript ───────────────────────────────────
if [[ "$FILE" =~ \.(ts|tsx|js|jsx|mts|cts)$ ]]; then

  if command -v npx &>/dev/null && npx eslint --version &>/dev/null 2>&1; then
    if npx eslint "$FILE" --fix --quiet 2>/dev/null; then
      pass "ESLint — no issues"
    else
      warn "ESLint — warnings remain (run: npx eslint \"$FILE\")"
    fi
  else
    skip "ESLint"
  fi

  if command -v npx &>/dev/null && npx prettier --version &>/dev/null 2>&1; then
    npx prettier --write "$FILE" --log-level silent 2>/dev/null
    pass "Prettier — formatted"
  else
    skip "Prettier"
  fi

  if [[ "$FILE" =~ \.(ts|tsx|mts|cts)$ ]]; then
    if command -v npx &>/dev/null && npx tsc --version &>/dev/null 2>&1; then
      if npx tsc --noEmit --skipLibCheck 2>/dev/null; then
        pass "TypeScript — no type errors"
      else
        warn "TypeScript — type errors found (run: npx tsc --noEmit)"
      fi
    else
      skip "TypeScript compiler"
    fi
  fi
fi

# ── Java ─────────────────────────────────────────────────────
if [[ "$FILE" =~ \.java$ ]]; then

  if command -v google-java-format &>/dev/null; then
    google-java-format --replace "$FILE" 2>/dev/null
    pass "google-java-format — formatted"
  else
    skip "google-java-format (install: brew install google-java-format)"
  fi

  if [[ -f "./gradlew" ]]; then
    if ./gradlew checkstyleMain -q 2>/dev/null; then
      pass "Checkstyle — passed"
    else
      warn "Checkstyle — violations found (run: ./gradlew checkstyleMain)"
    fi
  fi
fi

# ── Shell scripts ─────────────────────────────────────────────
if [[ "$FILE" =~ \.sh$ ]]; then

  if command -v shellcheck &>/dev/null; then
    if shellcheck "$FILE" 2>/dev/null; then
      pass "ShellCheck — no issues"
    else
      warn "ShellCheck — issues found (run: shellcheck \"$FILE\")"
    fi
  else
    skip "ShellCheck (install: brew install shellcheck)"
  fi

  if [[ ! -x "$FILE" ]]; then
    chmod +x "$FILE"
    pass "chmod +x applied"
  fi
fi

# ── Python ────────────────────────────────────────────────────
if [[ "$FILE" =~ \.py$ ]]; then

  if command -v ruff &>/dev/null; then
    ruff check "$FILE" --fix --quiet 2>/dev/null
    ruff format "$FILE" --quiet 2>/dev/null
    pass "Ruff — linted and formatted"
  elif command -v flake8 &>/dev/null; then
    if flake8 "$FILE" --max-line-length=120 2>/dev/null; then
      pass "flake8 — no issues"
    else
      warn "flake8 — issues found"
    fi
  else
    skip "Ruff / flake8"
  fi
fi

# ── YAML ──────────────────────────────────────────────────────
if [[ "$FILE" =~ \.(yaml|yml)$ ]]; then

  if command -v yamllint &>/dev/null; then
    if yamllint -d relaxed "$FILE" 2>/dev/null; then
      pass "yamllint — valid"
    else
      warn "yamllint — issues found"
    fi
  elif command -v python3 &>/dev/null; then
    if python3 -c "import yaml; yaml.safe_load(open('$FILE'))" 2>/dev/null; then
      pass "YAML syntax — valid"
    else
      fail "YAML syntax — INVALID! Fix before committing."
    fi
  else
    skip "yamllint"
  fi
fi

# ── JSON ──────────────────────────────────────────────────────
if [[ "$FILE" =~ \.json$ ]]; then

  if command -v python3 &>/dev/null; then
    if python3 -m json.tool "$FILE" > /dev/null 2>&1; then
      pass "JSON syntax — valid"
    else
      fail "JSON syntax — INVALID! Fix before committing."
    fi
  fi
fi

# ── SQL / Flyway migrations ───────────────────────────────────
if [[ "$FILE" =~ \.sql$ ]]; then

  BASENAME=$(basename "$FILE")
  if [[ ! "$BASENAME" =~ ^V[0-9]+(_[0-9]+)*__[a-zA-Z0-9_]+\.sql$ ]]; then
    warn "Flyway naming — '$BASENAME' may not follow V{version}__{description}.sql"
  else
    pass "Flyway naming — convention OK"
  fi

  FIRST_COMMIT=$(git log --follow --oneline "$FILE" 2>/dev/null | tail -1)
  if [[ -n "$FIRST_COMMIT" ]]; then
    warn "Flyway migration — already in git history. Never modify existing migrations; create a new one."
  fi
fi

# ── Markdown ──────────────────────────────────────────────────
if [[ "$FILE" =~ \.md$ ]]; then

  if command -v markdownlint &>/dev/null; then
    if markdownlint "$FILE" 2>/dev/null; then
      pass "markdownlint — passed"
    else
      warn "markdownlint — style issues (run: markdownlint \"$FILE\")"
    fi
  else
    skip "markdownlint (install: npm i -g markdownlint-cli)"
  fi
fi

echo "────────────────────────────────────────────────────"
echo ""
exit 0