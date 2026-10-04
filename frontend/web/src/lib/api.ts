import axios from 'axios'
import type { BallAck, BallInput, MatchDetail, MatchSummary, Squad } from '@/types/match'

export const TOKEN_KEY = 'access_token'

export interface Page<T> {
  content: T[]
  totalElements: number
}

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
    }
    return Promise.reject(err)
  },
)

/** Human-readable message from an API error (RFC 7807 or the match-service error body). */
export function errorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { detail?: string; message?: string } | undefined
    if (err.response?.status === 401) return 'Not signed in or the token has expired.'
    if (err.response?.status === 403) return 'This token is not allowed to score (needs the SCORER or ADMIN role).'
    return data?.detail ?? data?.message ?? err.message
  }
  return err instanceof Error ? err.message : 'Unexpected error'
}

export const matchApi = {
  getLive: (): Promise<MatchSummary[]> => http.get('/matches/live').then((r) => r.data),

  getUpcoming: (page = 0, size = 20): Promise<Page<MatchSummary>> =>
    http.get('/matches/upcoming', { params: { page, size } }).then((r) => r.data),

  getCompleted: (page = 0, size = 10): Promise<Page<MatchSummary>> =>
    http.get('/matches/completed', { params: { page, size } }).then((r) => r.data),

  getDetail: (matchId: string): Promise<MatchDetail> =>
    http.get(`/matches/${matchId}`).then((r) => r.data),

  getSquads: (matchId: string): Promise<Squad[]> =>
    http.get(`/matches/${matchId}/squads`).then((r) => r.data),

  startInnings: (matchId: string, battingTeamId: string): Promise<MatchDetail> =>
    http.post(`/matches/${matchId}/innings`, { battingTeamId }).then((r) => r.data),

  getBySeries: (seriesId: string, page = 0): Promise<Page<MatchSummary>> =>
    http.get(`/matches/series/${seriesId}`, { params: { page } }).then((r) => r.data),
}

export interface CommentaryRecord {
  id: string
  text: string
  eventType?: string
  overNumber: number
  ballNumber: number
  timestamp: string
}

export const commentaryApi = {
  getLatest: (matchId: string, limit = 20): Promise<CommentaryRecord[]> =>
    http.get(`/commentary/${matchId}/latest`, { params: { limit } }).then((r) => r.data),
}

export const scoringApi = {
  recordBall: (input: BallInput): Promise<BallAck> =>
    http.post('/score/ball', input).then((r) => r.data),
}
