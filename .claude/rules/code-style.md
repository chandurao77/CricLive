# Code Style Rules — CrickLive

## Java (Spring Boot services)

- Java 21. Use records, sealed types, pattern matching, and virtual threads where appropriate.
- **Constructor injection only.** Never `@Autowired` on fields. Prefer `@RequiredArgsConstructor`.
- **Package-by-feature**, not by layer. E.g., `dev.cricklive.match.scoring` not `dev.cricklive.controller`.
- Records for all DTOs. No mutable POJOs for data transfer.
- `Optional<T>` only at service boundaries — never pass Optional as a parameter.
- No `null` returns from public methods — use `Optional` or throw.
- Google Java Style Guide via Spotless. Run `./mvnw spotless:apply` before committing.
- Javadoc on all `public` interfaces and service methods. Skip for obvious getters/setters.

## TypeScript (React frontend)

- Strict mode always. Never `any` — use `unknown` with type narrowing.
- Functional components only. No class components.
- Props interfaces named `<Component>Props`.
- Co-locate component, test, and story files: `MatchCard.tsx`, `MatchCard.test.tsx`.
- No barrel re-exports (`index.ts`) — import directly by path.
- ESLint AirBnB config + Prettier. Run `pnpm lint` before committing.

## General

- No magic numbers — extract named constants.
- No commented-out code in committed files.
- Every TODO must have a ticket reference: `// TODO(CRICK-123): ...`
- Conventional Commits: `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`.
