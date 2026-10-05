# CrickLive — Architecture Decision Records (ADR)

## ADR-001: Polyglot Persistence Strategy

**Date:** 2024-01-01  
**Status:** Accepted

**Decision:** Use multiple databases — PostgreSQL for transactional data, MongoDB for ball events & commentary, Redis for caching & pub/sub, Elasticsearch for full-text search, MinIO for media.

**Rationale:** Each store is optimised for its access pattern. Ball events are append-only documents; forcing them into PostgreSQL rows would hurt write throughput. Commentary requires full-text flexibility. Transactional integrity (match results, user accounts) needs ACID guarantees.

**Trade-off:** Operational complexity of running multiple databases. Mitigated by managed cloud services (RDS, DocumentDB, ElastiCache) in production and Docker Compose for local development.

---

## ADR-002: Event Sourcing for Ball Deliveries

**Date:** 2024-01-01  
**Status:** Accepted

**Decision:** Every ball delivery is stored as an immutable event in MongoDB (`ball_events`). The scorecard is a read-model (projection) derived by replaying or streaming events.

**Rationale:** Cricket scoring is inherently event-driven. Scorers frequently correct the last ball (wide, no-ball, etc.). With event sourcing the undo/correction is a new compensating event, not a destructive UPDATE. Full replay capability enables audit trails.

**Trade-off:** Scorecard reads require projection materialisation. Solved by Kafka Streams aggregator that maintains a live materialized view in PostgreSQL + Redis.

---

## ADR-003: Kafka for Inter-Service Messaging

**Date:** 2024-01-01  
**Status:** Accepted

**Decision:** Apache Kafka is the primary event bus. `scoring-service` produces `BallEvent` records (Avro + Schema Registry); `match-service`, `commentary-service`, `stats-service`, and `notification-service` consume independently.

**Rationale:** Decouples producers from consumers; enables replay; Schema Registry enforces backward-compatible schema evolution; Kafka Streams provides stateful aggregation without external state stores.

**Trade-off:** Operational overhead of Kafka + ZooKeeper (or KRaft). Acceptable given 1M+ concurrent viewer target. AWS MSK is used in production.

---

## ADR-004: Redis Pub/Sub for WebSocket Fanout

**Date:** 2024-01-01  
**Status:** Accepted

**Decision:** `match-service` publishes score updates to Redis Pub/Sub channels. Multiple instances of `commentary-service` (the WebSocket gateway) subscribe and broadcast to connected clients via STOMP.

**Rationale:** Horizontal scaling of WebSocket servers requires shared message bus to avoid sticky-session-only architectures. Redis Pub/Sub provides low-latency fanout with sub-millisecond delivery at scale.

**Trade-off:** Redis Pub/Sub is fire-and-forget (no persistence). Missed events on reconnect are recovered via REST endpoint that returns last N balls. For guaranteed delivery, migration to Kafka consumer groups is documented as a future enhancement.

---

## ADR-005: JWT with RS256 (Asymmetric)

**Date:** 2024-01-01  
**Status:** Superseded by ADR-010 for the current implementation

**Decision:** JWTs are signed with RS256. The private key lives only in `user-service`; all other services hold the public key and verify locally without round-tripping to `user-service`.

**Rationale:** Eliminates a synchronous dependency on `user-service` per request. Services are independently deployable and resilient to `user-service` downtime for read operations.

**Trade-off:** Token revocation requires a deny-list in Redis (checked at gateway). Key rotation requires re-distribution of the public key — handled via Spring Cloud Config.

---

## ADR-006: Avro for Kafka Message Schema

**Date:** 2024-01-01  
**Status:** Superseded by ADR-009 for the current implementation

**Decision:** All Kafka messages use Apache Avro with Confluent Schema Registry.

**Rationale:** Enforces schema evolution contracts; provides backward/forward compatibility checks; binary serialisation is ~5× smaller than JSON for high-frequency ball events.

**Trade-off:** Requires Schema Registry as a runtime dependency. Fallback: topics can be switched to JSON during Schema Registry outage if a JSON deserialiser fallback is configured.

---

## ADR-007: React Frontend with Vite + TanStack Query

**Date:** 2024-01-01  
**Status:** Accepted

**Decision:** Frontend is React 18 + TypeScript + Vite, using TanStack Query for server state and Zustand for client state. No Next.js (SSR not required for initial scope).

**Rationale:** Vite provides fast local development. TanStack Query handles caching, background refetch, and stale-while-revalidate automatically. SSR adds complexity without significant SEO benefit for a mostly-authenticated sports app. SSR can be added later.

**Trade-off:** No SEO-friendly SSR for public pages (e.g., player profiles, news). Future phase: migrate public-facing routes to Next.js or add a lightweight meta-tag SSR proxy.

---

## ADR-008: Scorer Admin for Ball Entry (Phase 1)

**Date:** 2024-01-01  
**Status:** Accepted

**Decision:** Phase 1 uses a manual scorer admin panel for ball-by-ball entry. Third-party feed integration (CricAPI, SportMonks) is deferred to Phase 2.

**Rationale:** Third-party data feeds have licensing costs and variable data quality. Building the scoring engine first allows full control of the data model and validation logic.

**Trade-off:** Manual scoring introduces human latency (~5–10 s per ball). Acceptable for initial launch; feed adapter abstraction (`ScoreIngestionPort`) is designed-in for easy plug-in later.

---

## ADR-009: JSON instead of Avro on Kafka

**Date:** 2026-10-04  
**Status:** Accepted

**Decision:** `ball-events` messages are JSON (Spring Kafka `JsonSerializer`/`JsonDeserializer`, keyed by `matchId`, type headers disabled, trusted package pinned). The Avro plugin, Schema Registry and the committed `.avsc` were removed.

**Rationale:** The Avro setup could not build or run without a Schema Registry and an external Confluent repository, and nothing consumed the registry's guarantees yet. Ball events are small and low-volume at this stage.

**Trade-off:** No registry-enforced compatibility. Evolve the `BallEvent` record additively (new optional fields); revisit Avro when more consumers exist.

---

## ADR-010: Shared-secret HS256 JWT, no gateway

**Date:** 2026-10-04  
**Status:** Accepted (interim)

**Decision:** Services validate HS256 JWTs signed with `JWT_SECRET` (min 32 bytes, enforced at startup; no default outside the `dev` profile). Roles come from a `roles` claim and map to `ROLE_*`. GET endpoints for matches and commentary are public. There is no API gateway; Vite (dev) and nginx (Docker) route by path prefix.

**Rationale:** The earlier config pointed at an OAuth2 `issuer-uri` that no service provides, so no authenticated call could succeed. There is also no `user-service` yet, so tokens are minted with `scripts/mint-token.sh`.

**Trade-off:** Every service holds the signing secret, and there is no login UI. Replace with RS256 + `user-service` (ADR-005) when accounts are built.

---

## ADR-011: Per-innings sequence for idempotent, ordered scoring

**Date:** 2026-10-04  
**Status:** Accepted

**Decision:** `scoring-service` allocates a strictly increasing `sequence` and a legal-ball count per innings with an atomic Mongo `findOneAndUpdate`, stores the event (unique indexes on `(inningsId, sequence)` and `eventId`) and publishes it. `match-service` applies an event only if `sequence > innings.lastEventSeq`. If publishing fails, the counter is compensated and the 503 is returned; gaps are therefore allowed.

**Rationale:** Kafka redelivery and scorer retries must not double-count runs, and over/ball numbers must come from the server, not the client.

**Trade-off:** Strict ordering relies on keying by `matchId`. The previous `POST /score/undo` stub was removed rather than shipped half-working; undo (a compensating event) is future work.

---

## ADR-012: Demo data only in the dev/docker profiles

**Date:** 2026-10-04  
**Status:** Accepted

**Decision:** Players and the T20I fixture live in `db/dev` (loaded by the `dev` and `docker` profiles via `spring.flyway.locations`). Core schema changes are in `db/migration`. Test-format matches can be started and scored but only limited-overs results (win/tie) are computed; draws, follow-ons and declarations are not modelled.

