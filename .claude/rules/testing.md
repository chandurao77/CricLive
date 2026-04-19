# Testing Rules — CrickLive

## Coverage Targets

- **≥ 80% line coverage**, **≥ 70% branch coverage** per service.
- Enforced in CI via JaCoCo (`./mvnw verify`).

## Java

- Unit tests: JUnit 5 + Mockito. Test classes mirror source package structure.
- Integration tests: **Testcontainers only** — never mock databases, Kafka, or Redis in integration tests.
- Use `@SpringBootTest` + `@Testcontainers` for full-context integration tests.
- Contract tests: Pact for cross-service consumer-driven contracts.
- Include at least one property-based test per domain service (jqwik).
- Name tests: `methodName_scenario_expectedOutcome`.

## TypeScript

- Unit + component tests: Vitest + React Testing Library.
- E2E: Playwright (`pnpm test:e2e`).
- Mock API calls with MSW (Mock Service Worker), never with `jest.mock` on axios.
- Include at least one property-based test per utility function (fast-check).

## Test Data

- Builders or `@Builder`-annotated domain objects for Java test fixtures.
- Factory functions (not raw object literals) for TypeScript test data.
- Never use real PII in test fixtures — use `faker` / Instancio.
