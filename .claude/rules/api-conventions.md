# API Conventions — CrickLive

## REST

- Base path: `/api/v1/`
- Resources use plural nouns: `/matches`, `/players`, `/series`
- HTTP verbs: GET (read), POST (create), PUT (full replace), PATCH (partial update), DELETE
- Response: always JSON, `Content-Type: application/json`
- Errors: RFC 7807 Problem Details (`type`, `title`, `status`, `detail`, `instance`)
- Pagination: cursor-based for real-time feeds; offset+limit for static lists
  - Response envelope: `{ content: T[], totalElements, page, size, last }`
- Timestamps: ISO 8601 UTC strings (`2024-12-26T01:00:00Z`)
- IDs: UUIDs as strings

## WebSocket (STOMP)

- Connect endpoint: `ws://<host>/ws` (SockJS fallback at `/ws`)
- Subscribe: `/topic/match/{matchId}/live`
- Publish (client ping): `/app/match/{matchId}/ping`
- Message format: JSON with `{ type, matchId, ... }` discriminated union

## Versioning

- URL versioning: `/api/v1/`, `/api/v2/` — never break v1 endpoints.
- Kafka schemas versioned via Confluent Schema Registry (backward compatible only).

## Security

- All write endpoints require `Authorization: Bearer <JWT>`.
- Public read endpoints: live scores, scorecards, commentary, player profiles.
- Admin/scorer endpoints require `ROLE_ADMIN` or `ROLE_SCORER` claim in JWT.
