You are a senior full-stack architect with deep expertise in Java, Spring Boot, 
real-time systems, and sports data platforms. I need you to design and scaffold 
a production-grade live cricket scoring application modeled after Cricbuzz 
(https://www.cricbuzz.com/).

═══════════════════════════════════════════════════════════════════
PROJECT: CrickLive — Real-Time Cricket Scoring Platform
═══════════════════════════════════════════════════════════════════

## 1. CORE FEATURES TO IMPLEMENT

### Live Match Experience
- Real-time ball-by-ball commentary (WebSocket streaming)
- Live scorecard (batting, bowling, partnerships, fall of wickets)
- Over-by-over breakdown with run-rate graphs
- Current match status (LIVE, UPCOMING, COMPLETED, ABANDONED)
- Manhattan chart, worm chart, wagon wheel visualizations
- Required run rate (RRR) and projected scores
- DLS (Duckworth-Lewis-Stern) calculator for rain-affected matches

### Match & Series Management
- Fixtures, results, points tables
- Multi-format support: Test, ODI, T20I, T20 leagues (IPL, BBL, PSL, etc.)
- Tournament brackets and group stages
- Venue details with pitch reports and weather

### Statistics Engine
- Player profiles (batting/bowling/fielding career stats)
- Team rankings (ICC-style)
- Head-to-head records
- Records & milestones (fastest 100, highest partnership, etc.)
- Historical data archive (searchable by year/format/venue)

### News & Content
- News articles, match previews/reports
- Photo galleries, video highlights metadata
- Expert analysis, editorial pieces

### User Features
- Follow favorite teams/players
- Push notifications (match start, wicket, milestone, result)
- Fantasy points calculation hooks
- User predictions and polls

═══════════════════════════════════════════════════════════════════

## 2. SYSTEM ARCHITECTURE

Design a microservices architecture with these services:

┌─────────────────────────────────────────────────────────────┐
│                    CLIENT LAYER                              │
│  React + TypeScript Web | React Native Mobile | Admin CMS   │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              API GATEWAY (Spring Cloud Gateway)              │
│        Rate limiting | Auth | Routing | Load balancing      │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                   MICROSERVICES                              │
├─────────────────────────────────────────────────────────────┤
│ • match-service        → Live match state, scorecards       │
│ • scoring-service      → Ball-by-ball ingestion engine      │
│ • commentary-service   → Text commentary + events           │
│ • stats-service        → Player/team statistics aggregator  │
│ • player-service       → Player profiles & career data      │
│ • team-service         → Team metadata & squads             │
│ • series-service       → Tournaments, fixtures, standings   │
│ • venue-service        → Grounds, pitch reports, weather    │
│ • news-service         → Articles, editorial content        │
│ • user-service         → Auth, profiles, preferences        │
│ • notification-service → Push/email/in-app alerts           │
│ • search-service       → Elasticsearch-backed search        │
│ • media-service        → Images, video metadata, CDN links  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              MESSAGING & STREAMING LAYER                     │
│   Apache Kafka (event bus) | Redis Pub/Sub (WebSocket fan)  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    DATA LAYER                                │
├─────────────────────────────────────────────────────────────┤
│ • PostgreSQL   → Transactional: matches, users, series      │
│ • MongoDB      → Ball-by-ball events, commentary docs       │
│ • Redis        → Live scorecard cache, leaderboards, session│
│ • Elasticsearch→ Full-text search (players, news, stats)    │
│ • Cassandra    → Time-series stats, historical archives     │
│ • S3/MinIO     → Images, video thumbnails, static assets    │
└─────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════

## 3. TECHNOLOGY STACK

### Backend
- Java 21 + Spring Boot 3.3+
- Spring Cloud (Gateway, Config, Eureka/Consul)
- Spring WebFlux (reactive streams for live data)
- Spring Security + OAuth2/JWT
- Spring Data JPA, Spring Data MongoDB, Spring Data Redis
- Apache Kafka + Kafka Streams
- WebSocket (STOMP over SockJS)
- MapStruct, Lombok, Resilience4j

### Frontend
- React 18 + TypeScript + Vite
- TailwindCSS + shadcn/ui
- TanStack Query for server state
- Zustand for client state
- Socket.IO / native WebSocket client
- Recharts / D3.js for visualizations
- React Native (Expo) for mobile

### Data Ingestion
- Scoring admin panel (manual ball entry by scorers)
- Third-party feed adapters (optional: CricAPI, SportMonks)
- Scheduled jobs (Quartz) for fixtures sync

### Infrastructure & DevOps
- Docker + Docker Compose (local)
- Kubernetes (Helm charts) for production
- NGINX/Traefik ingress
- Prometheus + Grafana (metrics)
- ELK stack (logs)
- Jaeger/Zipkin (distributed tracing)
- GitHub Actions CI/CD
- Terraform for IaC (AWS/GCP)

═══════════════════════════════════════════════════════════════════

## 4. CRITICAL DATA MODELS

Design ERDs and schemas for:

- Match (id, format, status, teams, venue, toss, innings[], result)
- Innings (battingTeam, score, wickets, overs, extras, batters[], bowlers[])
- Ball (overNum, ballNum, batter, bowler, runs, extras, wicket, commentary)
- Player (id, name, role, battingStyle, bowlingStyle, country, stats)
- Team (id, name, shortName, flag, squad[], captain)
- Series (id, name, format, startDate, endDate, teams[], matches[])
- Venue (id, name, city, country, capacity, pitchType)
- Commentary (matchId, over, ball, text, eventType, timestamp)

═══════════════════════════════════════════════════════════════════

## 5. KEY TECHNICAL CHALLENGES TO SOLVE

1. **Real-time fanout at scale**
   - WebSocket connections for 1M+ concurrent viewers
   - Redis Pub/Sub → WebSocket gateway pattern
   - Sticky sessions, graceful reconnection

2. **Event sourcing for ball-by-ball**
   - Every ball is an immutable event
   - Scorecard is a projection (CQRS pattern)
   - Replay capability for audits & corrections

3. **Stat aggregation pipelines**
   - Kafka Streams to compute career stats incrementally
   - Materialized views in PostgreSQL for leaderboards

4. **Caching strategy**
   - Multi-tier: CDN → Redis → DB
   - Cache invalidation on ball events
   - Stale-while-revalidate for stats pages

5. **Scorer workflow & conflict resolution**
   - Undo/correction of last ball
   - Dual-scorer reconciliation
   - Optimistic locking

═══════════════════════════════════════════════════════════════════

## 6. DELIVERABLES I WANT FROM YOU

Execute in this order and create actual files on disk:

### Phase 1 — Foundation
1. Create a root project folder `cricklive/` with a monorepo layout
   (services/, frontend/, infra/, docs/)
2. Generate `docker-compose.yml` wiring Postgres, MongoDB, Redis, 
   Kafka (with Zookeeper), Elasticsearch, and pgAdmin
3. Create `README.md` with full architecture overview, setup steps, 
   and service ports table
4. Create `docs/ARCHITECTURE.md` with detailed component diagrams 
   (Mermaid syntax)
5. Create `docs/DATA_MODEL.md` with full ERD and field-level specs

### Phase 2 — Core Services (scaffold 3 priority services)
6. Scaffold `services/match-service/` as a complete Spring Boot 3 project:
   - `pom.xml` with all needed dependencies
   - Entity classes (Match, Innings, Team)
   - Repository interfaces
   - Service layer with business logic stubs
   - REST controllers with OpenAPI annotations
   - `application.yml` with profiles (dev, docker, prod)
   - Dockerfile (multi-stage build)
   - Sample Flyway migration scripts
7. Scaffold `services/scoring-service/` with Kafka producer setup 
   and ball-event schema (Avro)
8. Scaffold `services/commentary-service/` with WebSocket endpoint 
   and MongoDB integration

### Phase 3 — Frontend Shell
9. Scaffold `frontend/web/` React + Vite + TS + Tailwind project with:
   - Home page with live matches carousel
   - Match detail page skeleton (scorecard, commentary, stats tabs)
   - WebSocket hook for live updates
   - API client with Axios + TanStack Query setup
   - Dark mode toggle

### Phase 4 — Operational Assets
10. Create `infra/k8s/` with Helm chart templates for one service
11. Create `.github/workflows/ci.yml` with build + test + docker push
12. Create `docs/API.md` documenting 15+ core REST endpoints with 
    request/response examples
13. Create `docs/ROADMAP.md` with a 12-week phased delivery plan

═══════════════════════════════════════════════════════════════════

## 7. CONSTRAINTS & PREFERENCES

- Java 21, Spring Boot 3.3+, no deprecated APIs
- Production-ready patterns only (no toy code)
- Every service must have: health checks, metrics, structured logging, 
  graceful shutdown, configuration externalization
- Include Javadoc on public APIs
- Follow 12-factor app principles
- Use Testcontainers for integration tests
- All secrets via environment variables — never hardcoded

═══════════════════════════════════════════════════════════════════

## 8. EXECUTION RULES

- Work autonomously — don't ask me to confirm each step
- Build incrementally: finish Phase 1 completely before Phase 2
- After each phase, give me a short status summary (what's done, 
  what's next, any decisions I need to make)
- If you hit ambiguity, make a reasonable assumption, document it 
  in `docs/DECISIONS.md`, and move on
- When scaffolding code, generate real compilable skeletons — not 
  pseudocode placeholders
- Prefer industry-standard libraries over custom implementations

Begin with Phase 1. Confirm you understand the scope, then start 
creating files.