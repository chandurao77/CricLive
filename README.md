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
│         API GATEWAY  (Spring Cloud Gateway :8080)           │
│     Rate limiting │ JWT Auth │ Routing │ Load balancing     │
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
| Security | Spring Security, OAuth2, JWT (JJWT) |
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
| Schema | Apache Avro + Confluent Schema Registry |
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
| API Gateway | **8080** |
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
| Schema Registry | 8081 |
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

### Prerequisites

- Docker Desktop 4.28+ (with Compose v2)
- Java 21 (OpenJDK or Eclipse Temurin)
- Node.js 20 LTS + pnpm 9
- Maven 3.9

### 1. Clone & configure

```bash
git clone https://github.com/your-org/cricklive.git
cd cricklive
cp .env.example .env
# Edit .env and set passwords before starting
```

### 2. Start infrastructure

```bash
docker compose -f infra/docker/docker-compose.yml up -d
```

Wait for all health checks to pass (~60 s):

```bash
docker compose ps
```

### 3. Run a service (example: match-service)

```bash
cd services/match-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Run the frontend

```bash
cd frontend/web
pnpm install
pnpm dev
# → http://localhost:5173
```

### 5. Useful local URLs

| URL | Purpose |
|-----|---------|
| http://localhost:8080 | API Gateway |
| http://localhost:5173 | React frontend |
| http://localhost:5050 | pgAdmin |
| http://localhost:8090 | Kafka UI |
| http://localhost:5601 | Kibana |
| http://localhost:9001 | MinIO Console |

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
