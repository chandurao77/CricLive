# CLAUDE.md — CrickLive Project Guide

> This file is automatically loaded by Claude Code at session start. It defines the project's identity, tech stack, commands, and conventions. Claude should treat these instructions as authoritative for all work in this repository.

---

## 1. Project Identity

**Name:** CrickLive
**Description:** A production-grade, real-time cricket scoring and statistics platform modeled after Cricbuzz (https://www.cricbuzz.com/).
**Owner:** Chandu Marru
**Repository Type:** Polyglot monorepo (Java backend microservices + React frontend + infra as code)
**Current Phase:** Scaffolding & Foundation

---

## 2. Mission & Scope

CrickLive delivers live ball-by-ball cricket scoring, commentary, deep player/team statistics, news, and fixtures across Test, ODI, T20I, and franchise T20 formats. It must handle millions of concurrent WebSocket viewers during marquee matches (IPL finals, World Cup, Ashes) while keeping stat pages sub-second on cold loads.

**In-scope:**
- Live match state, scorecards, commentary, partnerships, fall-of-wickets
- Player and team career statistics (all formats, all time)
- Series, tournaments, points tables, fixtures, results
- News articles, match previews/reports, photo galleries
- User accounts with follows, notifications, and predictions
- Admin scoring console for manual ball-by-ball entry
- Third-party feed adapters (optional, pluggable)

**Out-of-scope for v1:**
- Fantasy league gameplay (only fantasy *points* calculation hooks)
- Live video streaming (we store metadata only)
- Betting odds or gambling integrations
- Native iOS/Android apps (React Native mobile is Phase 3)

---

## 3. High-Level Architecture

```
Client (React Web / React Native) 
    → API Gateway (Spring Cloud Gateway)
        → Microservices (Spring Boot 3)
            → Kafka (event bus) + Redis (cache & pub/sub)
                → PostgreSQL + MongoDB + Elasticsearch + Cassandra + S3
```

**Core microservices:**
`match-service`, `scoring-service`, `commentary-service`, `stats-service`, `player-service`, `team-service`, `series-service`, `venue-service`, `news-service`, `user-service`, `notification-service`, `search-service`, `media-service`.

**Architectural patterns:**
- **Event sourcing** for ball-by-ball data (every ball = immutable event)
- **CQRS** — write path via Kafka, read path via materialized Redis/Postgres projections
- **Reactive streams** (Spring WebFlux) for live match endpoints
- **Multi-tier caching** — CDN → Redis → DB, with event-driven invalidation
- **Hexagonal architecture** inside each service — domain, application, adapters

See `docs/ARCHITECTURE.md` for diagrams and deep-dive.

---

## 4. Tech Stack

### Backend
- **Language:** Java 21 (LTS, use records, sealed types, pattern matching, virtual threads)
- **Framework:** Spring Boot 3.3+, Spring Cloud 2023+
- **Reactive:** Spring WebFlux for live endpoints, Spring MVC for admin/CRUD
- **Security:** Spring Security 6 + OAuth2 Resource Server (JWT)
- **Data:** Spring Data JPA (Postgres), Spring Data MongoDB, Spring Data Redis, Spring Data Elasticsearch
- **Messaging:** Apache Kafka 3.x + Kafka Streams, Avro schemas in Confluent Schema Registry
- **Resilience:** Resilience4j (circuit breaker, retry, rate limiter, bulkhead)
- **Migrations:** Flyway (Postgres), Mongock (MongoDB)
- **Build:** Maven 3.9+ with BOM parent for version alignment
- **Utilities:** MapStruct, Lombok, Jackson, Micrometer, SLF4J + Logback

### Frontend
- **Web:** React 18 + TypeScript 5 + Vite 5
- **Styling:** TailwindCSS 3 + shadcn/ui + Radix primitives
- **State:** TanStack Query (server), Zustand (client), Jotai for atomic UI state
- **Routing:** React Router v6 with data routers
- **Realtime:** Native WebSocket + `reconnecting-websocket` fallback
- **Charts:** Recharts for standard, D3.js for wagon wheel / Manhattan
- **Testing:** Vitest + React Testing Library + Playwright E2E

### Mobile (Phase 3)
- React Native with Expo SDK 51+, shared TypeScript types from `packages/shared-types`

### Infrastructure
- **Containers:** Docker, multi-stage builds, distroless runtime images
- **Orchestration:** Kubernetes 1.29+, Helm 3 charts per service
- **Ingress:** NGINX Ingress Controller + cert-manager
- **Observability:** Prometheus + Grafana, OpenTelemetry → Tempo/Jaeger, Loki for logs
- **CI/CD:** GitHub Actions → build → test → container scan (Trivy) → push to GHCR → ArgoCD deploy
- **IaC:** Terraform (AWS primary; GCP alternative)
- **Secrets:** AWS Secrets Manager / HashiCorp Vault — never in code, never in env files committed

---

## 5. Repository Layout

```
cricklive/
├── CLAUDE.md                    ← you are here
├── CLAUDE.local.md              ← local, gitignored overrides
├── .mcp.json                    ← MCP integrations (GitHub, Jira, DBs)
├── .claude/                     ← Claude Code configuration
│   ├── settings.json
│   ├── settings.local.json
│   ├── rules/                   ← modular coding rules
│   ├── commands/                ← custom slash commands
│   ├── skills/                  ← context-loaded skills
│   ├── agents/                  ← specialized sub-agents
│   └── hooks/                   ← pre/post tool hooks
├── services/                    ← Spring Boot microservices
│   ├── match-service/
│   ├── scoring-service/
│   ├── commentary-service/
│   └── ...
├── frontend/
│   ├── web/                     ← React + Vite
│   ├── mobile/                  ← React Native (Phase 3)
│   └── admin/                   ← Scorer/CMS console
├── packages/                    ← shared TS types, OpenAPI clients
├── infra/
│   ├── docker/                  ← docker-compose, Dockerfiles
│   ├── k8s/                     ← Helm charts
│   └── terraform/               ← AWS/GCP IaC
├── docs/
│   ├── ARCHITECTURE.md
│   ├── DATA_MODEL.md
│   ├── API.md
│   ├── DECISIONS.md             ← ADRs
│   └── ROADMAP.md
└── scripts/                     ← automation shell scripts
```

---

## 6. Common Commands

Claude should use these exact commands when asked to build, test, run, or deploy.

### Bootstrap
```bash
./scripts/bootstrap.sh              # installs toolchain, pulls images
docker compose -f infra/docker/docker-compose.yml up -d
```

### Backend (per service)
```bash
cd services/<service-name>
./mvnw clean verify                  # full build with tests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw test                          # unit tests only
./mvnw verify -Pintegration          # with Testcontainers
./mvnw spotless:apply                # auto-format
```

### Frontend
```bash
cd frontend/web
pnpm install
pnpm dev                             # vite dev server :5173
pnpm build
pnpm test                            # vitest
pnpm test:e2e                        # playwright
pnpm lint && pnpm typecheck
```

### Full-stack dev
```bash
./scripts/dev-up.sh                  # brings up infra + all services + web
./scripts/dev-down.sh
./scripts/logs.sh <service-name>
```

### Database
```bash
./scripts/db-migrate.sh <service>    # runs Flyway
./scripts/db-reset.sh <service>      # drops and recreates (dev only)
./scripts/seed-sample-match.sh       # loads an IPL 2024 final replay
```

### Kafka
```bash
./scripts/kafka-topics-create.sh
./scripts/kafka-console-consumer.sh ball-events
```

### Release
```bash
./scripts/release.sh <service> <version>
```

---

## 7. Coding Conventions (Summary — full details in `.claude/rules/`)

- **Java:** Google Java Style + Spotless. Records for DTOs. No field injection — constructor only. No `@Autowired` on fields ever. Package-by-feature, not by layer.
- **TypeScript:** Strict mode always. No `any` (use `unknown` + narrowing). Functional components only. Props interfaces named `<Component>Props`.
- **Testing:** Every service must have ≥80% line coverage, ≥70% branch. Integration tests use Testcontainers — never mocks for infra. Contract tests via Pact for cross-service calls.
- **API design:** REST verbs + nouns, `/api/v1/...`, JSON only, RFC 7807 Problem Details for errors, cursor-based pagination. WebSocket for live data. GraphQL is *not* used in v1.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`). PRs squash-merge with the feature ticket ID.
- **Security:** No secrets in code or config files. All inputs validated. OWASP Top 10 reviewed per PR. SAST via CodeQL in CI.
- **Logging:** Structured JSON logs via Logback + Logstash encoder. MDC carries `traceId`, `matchId`, `userId`.
- **Feature flags:** Unleash or LaunchDarkly — no hardcoded `if (ENV === 'prod')` branches.

See `.claude/rules/code-style.md`, `.claude/rules/testing.md`, `.claude/rules/api-conventions.md`.

---

## 8. Claude's Working Agreement

When Claude is working on this repo, it must:

1. **Read before writing.** Always inspect existing patterns before adding new code. Match the style you see.
2. **Prefer editing over creating.** Only create new files when the functionality clearly doesn't belong in an existing one.
3. **Work phase-by-phase.** Don't jump ahead. Finish what you started before opening a new thread.
4. **Document decisions.** Any non-obvious trade-off goes in `docs/DECISIONS.md` as an ADR.
5. **Never commit secrets, test data with PII, or large binaries.** Use `.gitignore` aggressively.
6. **Run the validation hook** (`.claude/hooks/validate-bash.sh`) before executing shell commands that mutate state.
7. **Ask for confirmation** before: deleting files, force-pushing, altering production configs, or running migrations outside dev.
8. **Use sub-agents** from `.claude/agents/` for code review and security audits on any non-trivial change.
9. **Load skills on demand.** Skills in `.claude/skills/` auto-trigger; don't bloat the main context with their contents.
10. **Respect `CLAUDE.local.md`.** If it exists, its instructions override this file for the local developer.

---

## 9. Current Priorities

1. Finish Phase 1 scaffolding (infra, docs, compose).
2. Stand up `match-service`, `scoring-service`, `commentary-service` skeletons.
3. Ship a working demo: ingest one synthetic match, stream to React client over WebSocket.
4. Wire CI, Helm charts, and a single staging environment on a small K8s cluster.

Ship order: **correctness → observability → performance → features**.

---

## 10. Contact & Ownership

- **Tech lead:** Chandu Marru
- **Escalation for prod incidents:** follow `docs/RUNBOOK.md` (TBD)
- **Ticket tracker:** Jira project `CRICK` (via MCP)

---

*Last updated: 2026-04-19. When this file changes materially, bump the date and note it in `docs/DECISIONS.md`.*
