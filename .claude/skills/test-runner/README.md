# Test Runner Skill

## Overview
How to run, filter, debug, and interpret tests across the full stack.
Claude loads this context automatically when you ask about running, fixing, or writing tests.

## Stack Coverage
- **Backend:** Java 21 + JUnit 5 + Mockito + Testcontainers (Gradle)
- **Frontend:** TypeScript + Vitest + React Testing Library + MSW
- **E2E:** Playwright

---

## Backend Tests (Java / Gradle)

### Run Everything
```bash
./gradlew test
./gradlew test integrationTest    # unit + integration
```

### Targeted Runs
```bash
# Single test class
./gradlew test --tests "com.example.service.UserServiceTest"

# Single test method
./gradlew test --tests "com.example.service.UserServiceTest.should_createUser_when_validInput"

# All tests in a package
./gradlew test --tests "com.example.service.*"

# By tag (JUnit 5 @Tag)
./gradlew test -Dgroups="fast"
./gradlew test -Dgroups="integration"
```

### Skip / Isolate
```bash
# Skip integration tests (faster local feedback)
./gradlew test -x integrationTest

# Exclude a specific class
./gradlew test --exclude-tests "com.example.SlowTest"

# Re-run failed tests only
./gradlew test --rerun-tasks
```

### Coverage Report
```bash
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
# Minimum threshold enforced: 80% line coverage
```

### Testcontainers Tips
```bash
# Speed up by reusing containers (add to ~/.testcontainers.properties)
testcontainers.reuse.enable=true

# Pre-pull images to avoid CI timeout
docker pull postgres:15
docker pull confluentinc/cp-kafka:7.5.0
```

### Common Flags

| Flag | Purpose |
|---|---|
| `--info` | Verbose output |
| `--stacktrace` | Full stack trace on failure |
| `--continue` | Don't stop on first failure |
| `--parallel` | Run subprojects in parallel |
| `-PmaxParallelForks=4` | Run test classes in parallel |

---

## Frontend Tests (TypeScript / Vitest)

### Run Everything
```bash
npm run test              # single run
npm run test:watch        # watch mode
npm run test:ui           # Vitest UI in browser
```

### Targeted Runs
```bash
# Single file
npx vitest run src/components/UserCard.test.tsx

# Match by test name
npx vitest run -t "should render user name"

# Watch a specific file
npx vitest src/hooks/useAuth.test.ts
```

### Coverage
```bash
npm run test:coverage
# Report: coverage/index.html
# Thresholds enforced in vitest.config.ts
```

### MSW Setup
```ts
// src/mocks/handlers.ts
import { http, HttpResponse } from 'msw'
export const handlers = [
  http.get('/api/v1/users', () =>
    HttpResponse.json([{ id: '1', name: 'Ada' }])
  ),
]

// src/mocks/server.ts
import { setupServer } from 'msw/node'
import { handlers } from './handlers'
export const server = setupServer(...handlers)

// vitest.setup.ts
import { server } from './src/mocks/server'
beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

### RTL Query Priority (use in this order)
1. `getByRole` — most accessible, preferred
2. `getByLabelText` — form inputs
3. `getByPlaceholderText` — only if no label
4. `getByText` — non-interactive text
5. `getByTestId` — last resort

### Useful Assertions
```ts
expect(screen.getByRole('button', { name: /submit/i })).toBeEnabled()
expect(screen.getByText('Error message')).toBeInTheDocument()
expect(screen.queryByText('Loading...')).not.toBeInTheDocument()
await waitFor(() => expect(screen.getByText('Success')).toBeVisible())
```

---

## E2E Tests (Playwright)

### Run All
```bash
npx playwright test
npx playwright test --project=chromium
npx playwright test --project=mobile-chrome
```

### Targeted Runs
```bash
# Single spec file
npx playwright test tests/auth/login.spec.ts

# Match by test name
npx playwright test -g "should login with valid credentials"

# Headed mode (see the browser)
npx playwright test --headed

# Debug mode
npx playwright test tests/login.spec.ts --debug
```

### Reports & Traces
```bash
npx playwright show-report          # open last HTML report
npx playwright test --trace on      # record trace
npx playwright show-trace test-results/trace.zip
```

### Page Object Pattern (required for all E2E tests)
```ts
// tests/pages/LoginPage.ts
export class LoginPage {
  constructor(private page: Page) {}

  async goto() { await this.page.goto('/login') }
  async login(email: string, password: string) {
    await this.page.getByLabel('Email').fill(email)
    await this.page.getByLabel('Password').fill(password)
    await this.page.getByRole('button', { name: 'Sign in' }).click()
  }
}
```

---

## CI Pipeline Reference

| Job | Trigger | Command | Timeout |
|---|---|---|---|
| Unit tests | Every push | `./gradlew test` + `npm run test` | 10 min |
| Integration tests | Every push | `./gradlew integrationTest` | 15 min |
| E2E tests | PR to main | `npx playwright test` | 20 min |
| Coverage check | Every push | JaCoCo + Vitest thresholds | — |

---

## Debugging Slow / Flaky Tests

```bash
# Find slow backend tests
./gradlew test --info 2>&1 | grep "passed\|failed" | sort -t' ' -k3 -nr | head -20

# Find slow frontend tests
npx vitest run --reporter=verbose 2>&1 | grep "✓\|×" | sort -t' ' -k2 -nr | head -20

# Reproduce flakiness — run N times
npx playwright test tests/checkout.spec.ts --repeat-each=5
for i in 1 2 3; do ./gradlew test --tests "com.example.FlakyTest"; done
```