# CrickLive — Real-Time Cricket Scoring Platform

A production-grade, microservices-based live cricket scoring platform modelled after Cricbuzz.  
Built with **Java 21 + Spring Boot 3.3**, **React 18 + TypeScript**, Apache Kafka, and a polyglot data layer.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Repository Layout](#repository-layout)
4. [Service Ports](#service-ports)
5. [Quick Start (Local)](#quick-start-local)
6. [Environment Variables](#environment-variables)
7. [Documentation](#documentation)
8. [Contributing](#contributing)

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────┐
│                       CLIENT LAYER                          │
│   React + TypeScript Web  │  React Native (Expo) Mobile    │
└───────────────────────────┬────────────────────────────────┘
                            │ HTTPS / WSS
┌───────────────────────────▼────────────────────────────────┐
│   Edge routing: Vite proxy (dev) / nginx (Docker)           │
│   (Spring Cloud Gateway is planned, not yet built)          │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                      MICROSERVICES                          │
│  match-service  │  scoring-service  │  commentary-service   │
│  stats-service  │  player-service   │  team-service         │
│  series-service │  venue-service    │  news-service         │
│  user-service   │  notification-svc │  search-service       │
│  media-service                                              │
└────────────┬──────────────────────────┬────────────────────┘
             │                          │
┌────────────▼──────────┐  ┌────────────▼─────────────────────┐
│  Apache Kafka          │  │  Redis Pub/Sub                   │
│  (event streaming)     │  │  (WebSocket fanout cache)        │
└───────────────────────┘  └──────────────────────────────────┘
             │
┌────────────▼───────────────────────────────────────────────┐
│                        DATA LAYER                           │
│  PostgreSQL  │  MongoDB  │  Redis  │  Elasticsearch         │
│  Cassandra   │  MinIO (S3-compatible)                       │
└────────────────────────────────────────────────────────────┘
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed Mermaid diagrams.

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.3, Spring Cloud 2023.x |
| Reactive | Spring WebFlux, Project Reactor |
| Security | Spring Security, HS256 JWT resource server (see ADR-010) |
| Messaging | Apache Kafka 3.7, Kafka Streams |
| Real-time | WebSocket (STOMP over SockJS) |
| Frontend | React 18, TypeScript, Vite, TailwindCSS, shadcn/ui |
| State | TanStack Query (server), Zustand (client) |
| Charts | Recharts, D3.js |
| Mobile | React Native + Expo |
| RDBMS | PostgreSQL 16 |
| Document | MongoDB 7 |
| Cache | Redis 7.2 |
| Search | Elasticsearch 8 |
| Object store | MinIO (S3-compatible) |
| Migrations | Flyway |
| Kafka payloads | JSON (see ADR-009) |
| Containers | Docker, Docker Compose |
| Orchestration | Kubernetes + Helm |
| CI/CD | GitHub Actions |
| Observability | Prometheus, Grafana, ELK, Jaeger |

---

## Repository Layout

```
cricklive/
├── services/
│   ├── match-service/          # Live match state & scorecards
│   ├── scoring-service/        # Ball-by-ball ingestion (Kafka producer)
│   ├── commentary-service/     # Text commentary + WebSocket (MongoDB)
│   ├── stats-service/          # Aggregated player/team statistics
│   ├── player-service/         # Player profiles & career data
│   ├── team-service/           # Team metadata & squads
│   ├── series-service/         # Tournaments, fixtures, standings
│   ├── venue-service/          # Grounds, pitch reports, weather
│   ├── news-service/           # Articles & editorial content
│   ├── user-service/           # Auth, profiles, preferences
│   ├── notification-service/   # Push/email/in-app alerts
│   ├── search-service/         # Elasticsearch-backed full-text search
│   └── media-service/          # Image/video metadata, CDN links
├── frontend/
│   ├── web/                    # React + Vite + TS + Tailwind SPA
│   └── mobile/                 # React Native (Expo)
├── infra/
│   ├── k8s/                    # Helm charts
│   ├── terraform/              # AWS/GCP IaC
│   ├── postgres/               # Init SQL scripts
│   └── grafana/                # Dashboard JSON
├── docs/
│   ├── ARCHITECTURE.md         # Component diagrams (Mermaid)
│   ├── DATA_MODEL.md           # ERDs & field-level specs
│   ├── API.md                  # REST endpoint reference
│   ├── ROADMAP.md              # 12-week delivery plan
│   └── DECISIONS.md            # Architecture decision log
├── .github/
│   └── workflows/
│       └── ci.yml
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Service Ports

| Service | Port |
|---------|------|
| Web (Docker/nginx) | **8080** |
| Web (Vite dev) | 5173 |
| match-service | 8082 |
| scoring-service | 8083 |
| commentary-service | 8084 |
| stats-service | 8085 |
| player-service | 8086 |
| team-service | 8087 |
| series-service | 8088 |
| venue-service | 8089 |
| Kafka UI | 8090 |
| news-service | 8091 |
| user-service | 8092 |
| notification-service | 8093 |
| search-service | 8094 |
| media-service | 8095 |
| PostgreSQL | 5432 |
| pgAdmin | 5050 |
| MongoDB | 27017 |
| Redis | 6379 |
| Kafka (external) | 29092 |
| Elasticsearch | 9200 |
| Kibana | 5601 |
| MinIO API | 9000 |
| MinIO Console | 9001 |
| Zookeeper | 2181 |

---

## Quick Start (Local)

### What works today

`match-service` (:8082), `scoring-service` (:8083) and `commentary-service` (:8084) plus the React web app form a complete, working vertical slice: schedule/live/results pages, a live scorecard and commentary feed over WebSocket, and a scorer console to enter ball-by-ball data. The other services listed above are planned (see the roadmap). There is no API gateway; the web app talks to the three services through a dev proxy (Vite) or nginx (Docker).

### Prerequisites

- Docker with Compose v2
- Java 21 and Maven 3.9 (only for running services outside Docker)
- Node.js 20+

### Option A: everything in Docker

```bash
cp .env.example .env                       # set JWT_SECRET (>= 32 bytes) and passwords
docker compose --env-file .env -f infra/docker/docker-compose.yml up -d --build
# Web UI: http://localhost:8080
```

The `docker` profile loads demo data (fictional players, an India v Australia T20I scheduled for ~1 hour after first start). Optional tooling (pgAdmin, Kafka UI, Elasticsearch, Kibana, MinIO) is behind the `tools` profile: add `--profile tools`.

### Option B: infrastructure in Docker, services on the host

```bash
./scripts/dev-up.sh          # postgres, mongo, redis, kafka + 3 services + web (http://localhost:5173)
```

### Try it

```bash
./scripts/mint-token.sh SCORER          # prints a JWT; paste it into http://localhost:5173/score
node scripts/simulate-match.mjs         # or play a whole T20I automatically via the API
```

Open the match page in another tab to watch the score and commentary update live. The dev profile uses the secret `dev-only-jwt-secret-change-me-0123456789`; in Docker you must set `JWT_SECRET` and pass the same value to `mint-token.sh`.

### Tests

```bash
for s in match scoring commentary; do (cd services/$s-service && mvn test); done
cd frontend/web && npm ci && npm test && npm run lint && npm run build
```

### Useful local URLs

| URL | Purpose |
|-----|---------|
| http://localhost:5173 | React frontend (dev server) |
| http://localhost:8080 | React frontend (Docker/nginx) |
| http://localhost:8082/swagger-ui.html | match-service API |
| http://localhost:5050 / 8090 / 5601 / 9001 | pgAdmin / Kafka UI / Kibana / MinIO (`tools` profile) |

---

## Environment Variables

Copy `.env.example` to `.env` and fill in the secrets.  
**Never commit `.env`** — it is in `.gitignore`.

All services read configuration from environment variables following the  
[12-Factor App](https://12factor.net/) methodology. Production secrets must be  
injected via Kubernetes Secrets or a vault (e.g., HashiCorp Vault / AWS Secrets Manager).

---

## Documentation

| Document | Description |
|----------|-------------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Detailed component & sequence diagrams |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | ERDs and field-level database schemas |
| [docs/API.md](docs/API.md) | REST API reference with examples |
| [docs/ROADMAP.md](docs/ROADMAP.md) | 12-week phased delivery plan |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Architecture decision records (ADRs) |

---

## Contributing

1. Branch off `main`: `git checkout -b feat/your-feature`
2. Follow Google Java Style Guide for backend, AirBnB ESLint config for frontend
3. Write tests — unit with JUnit 5/Mockito, integration with Testcontainers
4. Open a PR; CI must pass before merge
