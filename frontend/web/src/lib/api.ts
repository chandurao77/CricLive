import axios from 'axios'
import type { MatchDetail, MatchSummary } from '@/types/match'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('access_token')
    }
    return Promise.reject(err)
  },
)

export const matchApi = {
  getLive: (): Promise<MatchSummary[]> =>
    http.get('/matches/live').then((r) => r.data),

  getUpcoming: (page = 0, size = 20): Promise<{ content: MatchSummary[]; totalElements: number }> =>
    http.get('/matches/upcoming', { params: { page, size } }).then((r) => r.data),

  getDetail: (matchId: string): Promise<MatchDetail> =>
    http.get(`/matches/${matchId}`).then((r) => r.data),

  getBySeries: (seriesId: string, page = 0): Promise<{ content: MatchSummary[] }> =>
    http.get(`/matches/series/${seriesId}`, { params: { page } }).then((r) => r.data),
}

export const commentaryApi = {
  getLatest: (matchId: string, limit = 20) =>
    http.get(`/commentary/${matchId}/latest`, { params: { limit } }).then((r) => r.data),

  getInnings: (matchId: string, inningsId: string) =>
    http.get(`/commentary/${matchId}/innings/${inningsId}`).then((r) => r.data),
}
