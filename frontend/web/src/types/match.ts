export type MatchFormat = 'TEST' | 'ODI' | 'T20I' | 'T20' | 'LIST_A' | 'FIRST_CLASS'

export type MatchStatus =
  | 'UPCOMING' | 'TOSS' | 'LIVE' | 'INNINGS_BREAK'
  | 'RAIN_DELAY' | 'COMPLETED' | 'ABANDONED' | 'NO_RESULT'

export type DismissalType =
  | 'BOWLED' | 'CAUGHT' | 'LBW' | 'RUN_OUT' | 'STUMPED'
  | 'HIT_WICKET' | 'NOT_OUT' | 'DNB'

export interface TeamRef {
  id: string
  name: string
  shortName: string
  flagUrl: string | null
}

export interface Venue {
  id: string
  name: string
  city: string
  country: string
  capacity: number | null
  pitchType: string | null
}

export interface Extras {
  total: number
  wides: number
  noBalls: number
  byes: number
  legByes: number
  penalty: number
}

export interface BattingEntry {
  playerId: string
  playerName: string
  position: number
  runs: number
  ballsFaced: number
  fours: number
  sixes: number
  strikeRate: number
  dismissalType: DismissalType | null
  dismissalDescription: string | null
  didNotBat: boolean
}

export interface BowlingEntry {
  playerId: string
  playerName: string
  overs: string
  maidens: number
  runs: number
  wickets: number
  wides: number
  noBalls: number
  economy: number
}

export interface InningsSummary {
  id: string
  inningsNumber: number
  battingTeam: TeamRef
  totalRuns: number
  wickets: number
  overs: string
  runRate: number
}

export interface InningsDetail extends InningsSummary {
  bowlingTeam: TeamRef
  extras: Extras
  status: string
  requiredRunRate: number | null
  target: number | null
  batting: BattingEntry[]
  bowling: BowlingEntry[]
}

export interface MatchSummary {
  id: string
  format: MatchFormat
  status: MatchStatus
  seriesName: string | null
  homeTeam: TeamRef
  awayTeam: TeamRef
  venueName: string
  venueCity: string
  scheduledStart: string
  statusText: string
  firstInnings: InningsSummary | null
  secondInnings: InningsSummary | null
}

export interface MatchDetail extends MatchSummary {
  venue: Venue
  actualStart: string | null
  toss: { winnerId: string; winnerName: string; decision: 'BAT' | 'FIELD' } | null
  result: { type: string; winningTeamName: string | null; margin: number | null; marginUnit: string | null } | null
  dlsApplied: boolean
  notes: string | null
  innings: InningsDetail[]
}

export interface Player {
  id: string
  name: string
  role: string | null
}

export interface Squad {
  team: TeamRef
  players: Player[]
}

export type ScorerDismissal = 'BOWLED' | 'CAUGHT' | 'LBW' | 'RUN_OUT' | 'STUMPED' | 'HIT_WICKET'

/** Body of POST /api/v1/score/ball */
export interface BallInput {
  matchId: string
  inningsId: string
  batterId: string
  bowlerId: string
  runsScored: number
  wide?: boolean
  noBall?: boolean
  bye?: boolean
  legBye?: boolean
  extraRuns?: number
  wicket?: boolean
  dismissalType?: ScorerDismissal
  dismissedBatterId?: string
  fielderId?: string
  idempotencyKey: string
}

export interface BallAck {
  eventId: string
  sequence: number
  overNumber: number
  ballNumber: number
  legalBall: boolean
  duplicate: boolean
}

/** Real-time live update pushed over WebSocket */
export interface LiveUpdate {
  type: 'SCORE' | 'COMMENTARY' | 'PONG'
  matchId: string
  id?: string
  inningsId?: string
  inningsNumber?: number
  runs?: number
  wickets?: number
  overs?: string
  runRate?: number
  target?: number | null
  matchStatus?: MatchStatus
  commentary?: string
  over?: string
  eventType?: string
  timestamp?: string
}
