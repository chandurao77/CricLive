-- ──────────────────────────────────────────────
-- V1: Core tables for match-service
-- ──────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Series ───────────────────────────────────
CREATE TABLE series (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(200) NOT NULL,
    short_name   VARCHAR(50),
    format       VARCHAR(20) NOT NULL,
    season       VARCHAR(10),
    start_date   DATE NOT NULL,
    end_date     DATE,
    host_country VARCHAR(100),
    status       VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Venue ────────────────────────────────────
CREATE TABLE venue (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    short_name  VARCHAR(80),
    city        VARCHAR(100) NOT NULL,
    country     VARCHAR(100) NOT NULL,
    capacity    INTEGER,
    pitch_type  VARCHAR(50),
    latitude    DECIMAL(9,6),
    longitude   DECIMAL(9,6),
    timezone    VARCHAR(50),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Team ─────────────────────────────────────
CREATE TABLE team (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(150) NOT NULL,
    short_name     VARCHAR(10) NOT NULL UNIQUE,
    country_code   CHAR(3),
    team_type      VARCHAR(20) NOT NULL,
    flag_url       TEXT,
    home_ground_id UUID REFERENCES venue(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Match ────────────────────────────────────
CREATE TABLE match (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id        UUID REFERENCES series(id) ON DELETE RESTRICT,
    match_number     INTEGER,
    format           VARCHAR(20) NOT NULL,
    venue_id         UUID REFERENCES venue(id),
    scheduled_start  TIMESTAMPTZ NOT NULL,
    actual_start     TIMESTAMPTZ,
    status           VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    home_team_id     UUID REFERENCES team(id),
    away_team_id     UUID REFERENCES team(id),
    toss_winner_id   UUID REFERENCES team(id),
    toss_decision    VARCHAR(10),
    result_type      VARCHAR(20),
    winning_team_id  UUID REFERENCES team(id),
    win_margin       INTEGER,
    win_margin_unit  VARCHAR(10),
    dls_applied      BOOLEAN NOT NULL DEFAULT FALSE,
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_match_status ON match(status);
CREATE INDEX idx_match_series ON match(series_id);
CREATE INDEX idx_match_scheduled_start ON match(scheduled_start);

-- ── Innings ──────────────────────────────────
CREATE TABLE innings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id         UUID NOT NULL REFERENCES match(id) ON DELETE CASCADE,
    innings_number   SMALLINT NOT NULL CHECK (innings_number BETWEEN 1 AND 4),
    batting_team_id  UUID NOT NULL REFERENCES team(id),
    bowling_team_id  UUID NOT NULL REFERENCES team(id),
    total_runs       INTEGER NOT NULL DEFAULT 0,
    wickets          SMALLINT NOT NULL DEFAULT 0,
    overs_completed  DECIMAL(5,1) NOT NULL DEFAULT 0,
    extras_total     INTEGER NOT NULL DEFAULT 0,
    extras_wides     INTEGER NOT NULL DEFAULT 0,
    extras_no_balls  INTEGER NOT NULL DEFAULT 0,
    extras_byes      INTEGER NOT NULL DEFAULT 0,
    extras_leg_byes  INTEGER NOT NULL DEFAULT 0,
    extras_penalty   INTEGER NOT NULL DEFAULT 0,
    declared         BOOLEAN NOT NULL DEFAULT FALSE,
    follow_on        BOOLEAN NOT NULL DEFAULT FALSE,
    status           VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    target           INTEGER,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (match_id, innings_number)
);

-- ── Batting Scorecard ─────────────────────────
CREATE TABLE batting_scorecard (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    innings_id              UUID NOT NULL REFERENCES innings(id) ON DELETE CASCADE,
    player_id               UUID NOT NULL,
    position                SMALLINT NOT NULL,
    runs                    INTEGER NOT NULL DEFAULT 0,
    balls_faced             INTEGER NOT NULL DEFAULT 0,
    fours                   SMALLINT NOT NULL DEFAULT 0,
    sixes                   SMALLINT NOT NULL DEFAULT 0,
    dismissal_type          VARCHAR(30),
    dismissed_by_bowler_id  UUID,
    dismissed_by_fielder_id UUID,
    did_not_bat             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (innings_id, player_id)
);

-- ── Bowling Scorecard ─────────────────────────
CREATE TABLE bowling_scorecard (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    innings_id UUID NOT NULL REFERENCES innings(id) ON DELETE CASCADE,
    player_id  UUID NOT NULL,
    overs      DECIMAL(5,1) NOT NULL DEFAULT 0,
    maidens    SMALLINT NOT NULL DEFAULT 0,
    runs       INTEGER NOT NULL DEFAULT 0,
    wickets    SMALLINT NOT NULL DEFAULT 0,
    wides      SMALLINT NOT NULL DEFAULT 0,
    no_balls   SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (innings_id, player_id)
);
