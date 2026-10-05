# CrickLive — REST API Reference

Base URL: `https://api.cricklive.dev/api/v1`  
Auth: `Authorization: Bearer <JWT>` (omit for public read endpoints). Write endpoints need a `roles` claim containing `SCORER` or `ADMIN` (HS256, see ADR-010; `./scripts/mint-token.sh`).
There is no gateway: each path prefix is served by one service (`/matches` → 8082, `/score` → 8083, `/commentary` + `/ws` → 8084).  
Errors: RFC 7807 Problem Details

---

## Match Endpoints (match-service :8082)

### GET /matches/live
Returns all currently LIVE matches.

**Response 200**
```json
[
  {
    "id": "d1000000-0000-0000-0000-000000000001",
    "format": "TEST",
    "status": "LIVE",
    "seriesName": "ICC Test Championship 2024-25",
    "homeTeam": { "id": "...", "name": "Australia", "shortName": "AUS", "flagUrl": null },
    "awayTeam": { "id": "...", "name": "India", "shortName": "IND", "flagUrl": null },
    "venueName": "Melbourne Cricket Ground",
    "venueCity": "Melbourne",
    "scheduledStart": "2024-12-26T01:00:00Z",
    "statusText": "Live",
    "firstInnings": {
      "id": "...",
      "inningsNumber": 1,
      "battingTeam": { "shortName": "AUS" },
      "totalRuns": 311,
      "wickets": 9,
      "overs": "87.3",
      "runRate": 3.56
    },
    "secondInnings": null
  }
]
```

---

### GET /matches/upcoming?page=0&size=20
Paginated list of upcoming matches.

**Query params:** `page` (default 0), `size` (default 20), `sort` (default `scheduledStart,asc`)

**Response 200**
```json
{
  "content": [ /* MatchSummaryDto[] */ ],
  "totalElements": 48,
  "totalPages": 3,
  "page": 0,
  "size": 20,
  "last": false
}
```

---

### GET /matches/series/{seriesId}
All matches for a series (paginated).

---

### GET /matches/{matchId}
Full match detail with all innings scorecards.

**Response 200**
```json
{
  "id": "...",
  "format": "TEST",
  "status": "LIVE",
  "venue": {
    "id": "...", "name": "Melbourne Cricket Ground",
    "city": "Melbourne", "country": "Australia",
    "capacity": 100024, "pitchType": "Pace-friendly"
  },
  "toss": { "winnerId": "...", "winnerName": "Australia", "decision": "BAT" },
  "result": null,
  "dlsApplied": false,
  "innings": [
    {
      "inningsNumber": 1,
      "battingTeam": { "shortName": "AUS" },
      "totalRuns": 474,
      "wickets": 10,
      "overs": "112.3",
      "extras": { "total": 18, "wides": 4, "noBalls": 3, "byes": 7, "legByes": 4, "penalty": 0 },
      "batting": [
        {
          "playerName": "David Warner", "position": 1,
          "runs": 38, "ballsFaced": 72, "fours": 5, "sixes": 0,
          "strikeRate": 52.77, "dismissalType": "CAUGHT",
          "dismissalDescription": "c Rohit b Bumrah"
        }
      ],
      "bowling": [
        {
          "playerName": "Jasprit Bumrah", "overs": "24.3",
          "maidens": 5, "runs": 74, "wickets": 6, "wides": 1, "noBalls": 0,
          "economy": 3.02
        }
      ]
    }
  ]
}
```

---

### POST /matches *(admin)*
Create a new match fixture.

**Request**
```json
{
  "seriesId": "c1000000-0000-0000-0000-000000000001",
  "matchNumber": 1,
  "format": "TEST",
  "venueId": "a1000000-0000-0000-0000-000000000001",
  "scheduledStart": "2024-12-26T01:00:00Z",
  "homeTeamId": "b1000000-0000-0000-0000-000000000002",
  "awayTeamId": "b1000000-0000-0000-0000-000000000001"
}
```

**Response 201** — full MatchDetailDto, `Location: /api/v1/matches/{id}`

---

### PATCH /matches/{matchId}/status?status=LIVE *(scorer/admin)*
Transition match status.

**Response 200** — MatchSummaryDto

---

### GET /matches/completed?page=0&size=20
Paginated finished matches, newest first. `statusText` carries the result, e.g. `Australia won by 6 wickets`.

### GET /matches/{id}/squads
Both teams' players (`id`, `name`, `role`) for the match, used by the scorer console.

### POST /matches/{id}/innings *(scorer/admin)*
Start the next innings. Body: `{ "battingTeamId": "..." }`. Returns 409 if the previous innings is still open or the match limit is reached. The first call also moves the match to LIVE.

---

## Scoring Endpoints (scoring-service :8083)

### POST /score/ball *(scorer/admin)*
Record a single ball delivery. Idempotent via `idempotencyKey`.

**Request**
```json
{
  "matchId": "d1000000-0000-0000-0000-000000000001",
  "inningsId": "e1000000-0000-0000-0000-000000000001",
  "batterId": "f1000000-0000-0000-0000-000000000001",
  "bowlerId": "f1000000-0000-0000-0000-000000000002",
  "runsScored": 4,
  "wide": false,
  "noBall": false,
  "bye": false,
  "legBye": false,
  "extraRuns": 0,
  "wicket": false,
  "dismissalType": null,
  "idempotencyKey": "over-14-ball-3-unique-uuid"
}
```

**Response 202** — Accepted. The event is persisted and published to Kafka (the call waits for the broker ack).
```json
{ "eventId": "...", "sequence": 7, "overNumber": 0, "ballNumber": 4, "legalBall": true, "duplicate": false }
```
Resending the same `idempotencyKey` returns the original ack with `"duplicate": true` and does not score the ball twice.
Over/ball numbers are derived by the server from the innings' legal-ball count.

**Response 400** — Rule violation (ProblemDetail), e.g. runs off the bat on a wide, bye without extras, a dismissal not possible on that delivery.
**Response 503** — Kafka publish failed; the ball was rolled back, retry with the same key.

---

## Commentary Endpoints (commentary-service :8084)

### GET /commentary/{matchId}/latest?limit=20
Last N commentary entries (newest first). Use on reconnect to catch up.

**Response 200**
```json
[
  {
    "id": "...",
    "overNumber": 14,
    "ballNumber": 3,
    "text": "FOUR! Kohli drives beautifully through the covers.",
    "htmlText": "<span class='badge badge-green'>FOUR</span> Kohli drives...",
    "eventType": "BOUNDARY",
    "tags": ["BOUNDARY", "DRIVE"],
    "timestamp": "2024-12-26T04:32:11Z"
  }
]
```

---

### GET /commentary/{matchId}/innings/{inningsId}
Full chronological commentary for an innings.

---

### GET /commentary/{matchId}/innings/{inningsId}/over/{over}
Commentary for a specific over.

---

## WebSocket — Live Updates

**Connect:** `ws://api.cricklive.dev/ws` (SockJS fallback)  
**Subscribe:** `/topic/match/{matchId}/live`  
**Ping:** `SEND /app/match/{matchId}/ping`

**Score update message**
```json
{
  "type": "SCORE",
  "matchId": "d1000000-0000-0000-0000-000000000001",
  "inningsId": "...",
  "runs": 315,
  "wickets": 3,
  "overs": "87.4",
  "runRate": 3.59
}
```

**Commentary message**
```json
{
  "type": "COMMENTARY",
  "matchId": "...",
  "commentary": "<span class='badge badge-green'>FOUR</span> Kohli drives...",
  "eventType": "BOUNDARY"
}
```

**Wicket message**
```json
{
  "type": "WICKET",
  "matchId": "...",
  "commentary": "OUT! Caught at third slip.",
  "eventType": "WICKET"
}
```

---

## Error Format (RFC 7807)

```json
{
  "type": "https://cricklive.dev/errors/match-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Match not found: d1000000-0000-0000-0000-000000000099",
  "instance": "/api/v1/matches/d1000000-0000-0000-0000-000000000099"
}
```

---

## Common HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created (POST success) |
| 202 | Accepted (async operation queued) |
| 204 | No content (DELETE) |
| 400 | Bad request / validation error |
| 401 | Missing or invalid JWT |
| 403 | Insufficient role |
| 404 | Resource not found |
| 409 | Conflict (duplicate idempotency key) |
| 422 | Unprocessable entity |
| 500 | Internal server error |
| 503 | Service unavailable |
