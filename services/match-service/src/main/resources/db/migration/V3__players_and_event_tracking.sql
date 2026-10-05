-- ──────────────────────────────────────────────
-- V3: Players, and per-innings event sequence tracking
-- ──────────────────────────────────────────────

CREATE TABLE player (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    team_id    UUID REFERENCES team(id),
    role       VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_player_team ON player(team_id);

-- Highest ball-event sequence applied to the innings; lets the Kafka consumer ignore duplicates.
ALTER TABLE innings ADD COLUMN last_event_seq BIGINT NOT NULL DEFAULT 0;

-- Runs the bowler has conceded in the over in progress; lets maidens be detected when the over ends.
ALTER TABLE bowling_scorecard ADD COLUMN current_over_runs SMALLINT NOT NULL DEFAULT 0;
