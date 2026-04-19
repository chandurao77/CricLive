# CrickLive — 12-Week Delivery Roadmap

**Principle:** correctness → observability → performance → features

---

## Week 1–2: Foundation (Phase 1) ✅

| Task | Status |
|------|--------|
| Monorepo layout (services/, frontend/, infra/, docs/, packages/, scripts/) | ✅ Done |
| `infra/docker/docker-compose.yml` wiring all infra services | ✅ Done |
| README, ARCHITECTURE.md, DATA_MODEL.md, DECISIONS.md | ✅ Done |
| `.env.example`, `.gitignore`, `.mcp.json` | ✅ Done |
| `.claude/` configuration (settings, rules, hooks) | ✅ Done |

---

## Week 3–4: Core Backend Services (Phase 2) ✅

| Task | Status |
|------|--------|
| `match-service`: entities, repositories, service, REST controller, Flyway migrations | ✅ Done |
| `match-service`: Redis pub/sub scorecard publisher | ✅ Done |
| `scoring-service`: BallEvent Avro schema + Kafka producer | ✅ Done |
| `scoring-service`: MongoDB event store, idempotency guard | ✅ Done |
| `commentary-service`: WebSocket (STOMP/SockJS) gateway | ✅ Done |
| `commentary-service`: Kafka consumer + Redis subscriber + fanout | ✅ Done |
| Dockerfiles (multi-stage) for all 3 services | ✅ Done |

---

## Week 5: Frontend Shell (Phase 3) ✅

| Task | Status |
|------|--------|
| React + Vite + TypeScript + TailwindCSS scaffold | ✅ Done |
| Home page: live matches carousel + upcoming grid | ✅ Done |
| Match detail page: scorecard, commentary, stats tabs | ✅ Done |
| WebSocket hook (`useLiveMatch`) with STOMP + auto-reconnect | ✅ Done |
| API client (Axios + TanStack Query) | ✅ Done |
| Dark mode toggle (Zustand + localStorage) | ✅ Done |
| `packages/shared-types` | ✅ Done |

---

## Week 6: Operational Assets (Phase 4) ✅

| Task | Status |
|------|--------|
| GitHub Actions CI: build + test + Trivy scan + GHCR push | ✅ Done |
| Helm chart for `match-service` (Deployment, Service, HPA) | ✅ Done |
| `docs/API.md` — 15+ REST + WebSocket endpoints | ✅ Done |
| `docs/ROADMAP.md` | ✅ Done |
| `scripts/` — bootstrap, dev-up, kafka-topics, db-migrate, seed | ✅ Done |

---

## Week 7: Remaining Microservices

| Task | Owner |
|------|-------|
| `player-service`: player profiles + career stats REST API | Backend |
| `team-service`: team metadata, squad management | Backend |
| `series-service`: series, fixtures, points table | Backend |
| `venue-service`: ground info, pitch reports | Backend |
| `stats-service`: Kafka Streams aggregator → PostgreSQL materialized views | Backend |

---

## Week 8: User & Notification Services

| Task | Owner |
|------|-------|
| `user-service`: registration, JWT auth, OAuth2 (Google) | Backend |
| `user-service`: follow teams/players, preferences | Backend |
| `notification-service`: push (FCM/APNs), in-app alerts | Backend |
| `news-service`: articles CRUD, editorial CMS hooks | Backend |
| `search-service`: Elasticsearch indexing for players, matches, news | Backend |

---

## Week 9: Admin Scoring Console

| Task | Owner |
|------|-------|
| `frontend/admin`: React scorer console (ball entry form) | Frontend |
| Real-time validation: no-ball/wide rules, DLS calculator UI | Frontend |
| Dual-scorer reconciliation UI | Frontend |
| Undo/correction flow | Frontend |
| Auth: ROLE_SCORER JWT flow | Full-stack |

---

## Week 10: Statistics & Visualizations

| Task | Owner |
|------|-------|
| Career batting/bowling stat pages | Full-stack |
| ICC-style team rankings table | Full-stack |
| Head-to-head records | Full-stack |
| Recharts: over-by-over run rate graph, Manhattan chart | Frontend |
| D3.js: wagon wheel visualization | Frontend |
| Worm chart (cumulative runs comparison) | Frontend |

---

## Week 11: Performance & Observability

| Task | Owner |
|------|-------|
| OpenTelemetry tracing → Tempo/Jaeger | DevOps |
| Prometheus metrics + Grafana dashboards | DevOps |
| Loki structured log aggregation | DevOps |
| Redis cache hit rate optimization | Backend |
| Load test: Gatling for WebSocket (1M concurrent) | QA |
| CDN configuration (CloudFront) for static assets | DevOps |

---

## Week 12: Production Readiness

| Task | Owner |
|------|-------|
| Kubernetes staging cluster (EKS/GKE) via Terraform | DevOps |
| ArgoCD GitOps deployment pipeline | DevOps |
| Helm charts for all services | DevOps |
| cert-manager + NGINX ingress with TLS | DevOps |
| Security review: OWASP Top 10, CodeQL findings | Security |
| `docs/RUNBOOK.md` — incident playbooks | All |
| Synthetic match demo: ingest full IPL match replay | Backend |
| Phase 3 kick-off: React Native mobile (Expo) | Mobile |

---

## Post-Launch Backlog

- Fantasy points calculation hooks
- Third-party feed adapters (CricAPI, SportMonks)
- DLS calculator integration
- React Native mobile app (Expo SDK 51)
- Multi-language commentary (Hindi, Tamil)
- Personalized push notifications
- Live video highlights metadata linking
