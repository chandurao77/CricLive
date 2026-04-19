import { useQuery } from '@tanstack/react-query'
import { matchApi, commentaryApi } from '@/lib/api'

export const matchKeys = {
  live: ['matches', 'live'] as const,
  upcoming: (page: number) => ['matches', 'upcoming', page] as const,
  detail: (id: string) => ['matches', id] as const,
  commentary: (matchId: string) => ['commentary', matchId] as const,
}

export function useLiveMatches() {
  return useQuery({
    queryKey: matchKeys.live,
    queryFn: matchApi.getLive,
    refetchInterval: 30_000,
  })
}

export function useUpcomingMatches(page = 0) {
  return useQuery({
    queryKey: matchKeys.upcoming(page),
    queryFn: () => matchApi.getUpcoming(page),
    staleTime: 60_000,
  })
}

export function useMatchDetail(matchId: string) {
  return useQuery({
    queryKey: matchKeys.detail(matchId),
    queryFn: () => matchApi.getDetail(matchId),
    enabled: !!matchId,
    staleTime: 10_000,
  })
}

export function useCommentary(matchId: string) {
  return useQuery({
    queryKey: matchKeys.commentary(matchId),
    queryFn: () => commentaryApi.getLatest(matchId, 50),
    enabled: !!matchId,
    staleTime: 0,
  })
}
