// Shared TypeScript types used by web, mobile, and admin frontends.
// Generated OpenAPI clients live in packages/api-client/ (Phase 2).

export type MatchFormat = 'TEST' | 'ODI' | 'T20I' | 'T20' | 'LIST_A' | 'FIRST_CLASS'

export type MatchStatus =
  | 'UPCOMING' | 'TOSS' | 'LIVE' | 'INNINGS_BREAK'
  | 'RAIN_DELAY' | 'COMPLETED' | 'ABANDONED' | 'NO_RESULT'

export type DismissalType =
  | 'BOWLED' | 'CAUGHT' | 'LBW' | 'RUN_OUT' | 'STUMPED'
  | 'HIT_WICKET' | 'HIT_TWICE' | 'HANDLED_BALL'
  | 'OBSTRUCTING_FIELD' | 'TIMED_OUT' | 'NOT_OUT' | 'DNB'

export type PlayerRole = 'BATTER' | 'BOWLER' | 'ALL_ROUNDER' | 'WICKET_KEEPER'

export type BattingStyle = 'RIGHT_HAND' | 'LEFT_HAND'

export interface TeamRef {
  id: string
  name: string
  shortName: string
  flagUrl: string | null
}

export interface LiveUpdate {
  type: 'SCORE' | 'COMMENTARY' | 'WICKET' | 'BOUNDARY' | 'SIX' | 'PONG'
  matchId: string
  inningsId?: string
  runs?: number
  wickets?: number
  overs?: string
  runRate?: number
  requiredRunRate?: number
  commentary?: string
  eventType?: string
  timestamp?: string
}

export interface ApiPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  last: boolean
}

export interface ApiError {
  type: string
  title: string
  status: number
  detail: string
  instance: string
}
