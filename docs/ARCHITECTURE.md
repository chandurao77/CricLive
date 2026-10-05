# CrickLive — Architecture

> **Implementation note (2026-10):** the diagrams below show the target architecture. Currently built: match-, scoring- and commentary-service + web. Kafka payloads are JSON (not Avro, no Schema Registry), auth is a shared-secret JWT, and there is no API gateway; see ADR-009 to ADR-012 in [DECISIONS.md](DECISIONS.md).


## 1. High-Level Component Diagram

```mermaid
graph TB
    subgraph Clients
        WEB[React Web App]
        MOB[React Native Mobile]
        ADMIN[Admin CMS]
    end

    subgraph Gateway["API Gateway (Spring Cloud Gateway :8080)"]
        GW[Rate Limit · JWT Auth · Routing]
    end

    subgraph Services["Microservices (Spring Boot 3.3 / Java 21)"]
        MS[match-service\n:8082]
        SC[scoring-service\n:8083]
        CM[commentary-service\n:8084]
        ST[stats-service\n:8085]
        PL[player-service\n:8086]
        TM[team-service\n:8087]
        SR[series-service\n:8088]
        VN[venue-service\n:8089]
        NW[news-service\n:8091]
        US[user-service\n:8092]
        NT[notification-service\n:8093]
        SE[search-service\n:8094]
        MD[media-service\n:8095]
    end

    subgraph Messaging
        KF[Apache Kafka]
        SK[Schema Registry\nAvro]
        RP[Redis Pub/Sub]
    end

    subgraph Data
        PG[(PostgreSQL 16\nMatches · Users · Series)]
        MG[(MongoDB 7\nBall events · Commentary)]
        RD[(Redis 7.2\nLive cache · Sessions)]
        ES[(Elasticsearch 8\nSearch index)]
        MN[(MinIO\nMedia assets)]
    end

    subgraph Observability
        PR[Prometheus]
        GR[Grafana]
        ELK[ELK Stack]
        JG[Jaeger Tracing]
    end

    WEB & MOB & ADMIN --> GW
    GW --> MS & SC & CM & ST & PL & TM & SR & VN & NW & US & NT & SE & MD

    SC -->|BallEvent Avro| KF
    KF --> MS
    KF --> CM
    KF --> ST
    KF --> NT
    KF --> SE

    MS -->|score update| RP
    RP --> CM

    MS --> PG
    SC --> MG
    CM --> MG
    ST --> PG
    PL --> PG
    TM --> PG
    SR --> PG
    VN --> PG
    NW --> PG
    US --> PG
    SE --> ES
    MD --> MN
    MS & PL & TM --> RD

    MS & SC & CM & ST --> PR
    PR --> GR
    Services --> ELK
    Services --> JG
```

---

## 2. Real-Time WebSocket Fanout

```mermaid
sequenceDiagram
    participant Scorer as Scorer (Admin UI)
    participant SC as scoring-service
    participant KF as Kafka Topic<br/>ball-events
    participant MS as match-service
    participant RP as Redis Pub/Sub
    participant CM as commentary-service<br/>(WS Gateway)
    participant Client as Viewer Browser

    Scorer->>SC: POST /api/v1/score/ball (BallInputDTO)
    SC->>SC: Validate + enrich event
    SC->>KF: Produce BallEvent (Avro)
    SC-->>Scorer: 202 Accepted

    KF->>MS: Consume BallEvent
    MS->>MS: Update scorecard projection
    MS->>MS: Recalc RRR, partnerships, wagon wheel
    MS->>RP: PUBLISH channel=match:{matchId}:score

    KF->>CM: Consume BallEvent
    CM->>CM: Generate commentary text
    CM->>RP: PUBLISH channel=match:{matchId}:commentary

    RP->>CM: Subscribe fan-out to WS clients
    CM->>Client: STOMP /topic/match/{matchId}/live (JSON)
```

---

## 3. Event Sourcing — Ball-by-Ball (CQRS)

```mermaid
graph LR
    subgraph Write Side
        API[Scoring API] -->|Command| CH[CommandHandler]
        CH -->|Append| ES_STORE[(Event Store\nMongoDB)]
    end

    subgraph Kafka Streams
        ES_STORE -->|CDC / Produce| KF[Kafka: ball-events]
        KF --> AGG[BallAggregator\nKafka Streams]
    end

    subgraph Read Side
        AGG -->|Materialize| SC_VIEW[(Scorecard View\nPostgreSQL + Redis)]
        AGG -->|Materialize| STATS_VIEW[(Career Stats\nPostgreSQL)]
        SC_VIEW -->|Query| MATCH_API[match-service REST / WS]
        STATS_VIEW -->|Query| STATS_API[stats-service REST]
    end
```

---

## 4. Caching Strategy

```mermaid
graph LR
    Browser -->|Request| CDN[CDN / Cloudfront]
    CDN -->|Cache miss| GW[API Gateway]
    GW -->|Cache miss| SVC[Microservice]
    SVC -->|Cache miss| RD[Redis]
    RD -->|Cache miss| DB[(PostgreSQL / MongoDB)]

    DB -->|Write-through| RD
    RD -->|TTL update| SVC

    style CDN fill:#ff9
    style RD fill:#f96
```

| Data Type | Cache Layer | TTL |
|-----------|-------------|-----|
| Live scorecard | Redis (pub/sub) | Push on event |
| Player profile | Redis | 1 hour |
| Match fixtures | CDN + Redis | 5 min |
| News articles | CDN | 15 min |
| Career stats | Redis | 30 min |
| Search results | Elasticsearch | Query-time |

---

## 5. Service Discovery & Configuration

```mermaid
graph TB
    CS[Spring Cloud Config Server] -->|properties| Services
    CR[Consul / Eureka Service Registry] <-->|register + heartbeat| Services
    GW[API Gateway] -->|lookup| CR
```

- **Config Server** externalises all `application.yml` properties.  
- **Service Registry** (Consul preferred for production; Eureka for local) handles dynamic routing.  
- **API Gateway** applies circuit-breaking (Resilience4j) and rate limiting (Redis token bucket).

---

## 6. Security Architecture

```mermaid
sequenceDiagram
    participant Client
    participant GW as API Gateway
    participant US as user-service
    participant SVC as Any Microservice

    Client->>GW: POST /auth/login {credentials}
    GW->>US: Forward to user-service
    US->>US: Validate credentials, generate JWT
    US-->>GW: {accessToken, refreshToken}
    GW-->>Client: Tokens

    Client->>GW: GET /api/v1/matches (Authorization: Bearer <JWT>)
    GW->>GW: Validate JWT signature + expiry
    GW->>SVC: Forward with X-User-Id, X-Roles headers
    SVC-->>GW: Response
    GW-->>Client: Response
```

- JWT signed with RS256 (asymmetric); public key distributed to all services.  
- Refresh tokens stored in Redis with revocation capability.  
- Service-to-service calls use mTLS inside the cluster.

---

## 7. Deployment Topology (Production)

```mermaid
graph TB
    subgraph AWS
        subgraph VPC
            subgraph EKS["EKS Cluster"]
                ING[NGINX Ingress]
                GW[API Gateway pod x3]
                SVC_PODS[Service pods\nauto-scaled]
            end
            subgraph Managed
                RDS[RDS PostgreSQL\nMulti-AZ]
                DOCDB[DocumentDB\nMongoDB compat]
                REDIS_C[ElastiCache Redis\nCluster]
            end
            MSK[Amazon MSK\nManaged Kafka]
            OSS[OpenSearch Service]
        end
        CF[CloudFront CDN]
        S3[S3 Media Bucket]
    end

    Users --> CF
    CF --> ING
    ING --> GW
    GW --> SVC_PODS
    SVC_PODS --> RDS & DOCDB & REDIS_C & MSK & OSS & S3
```
