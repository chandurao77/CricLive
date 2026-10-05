import { useQuery } from '@tanstack/react-query'
import { matchApi, commentaryApi } from '@/lib/api'

export const matchKeys = {
  live: ['matches', 'live'] as const,
  upcoming: (page: number) => ['matches', 'upcoming', page] as const,
  completed: ['matches', 'completed'] as const,
  squads: (id: string) => ['matches', id, 'squads'] as const,
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

export function useCompletedMatches() {
  return useQuery({
    queryKey: matchKeys.completed,
    queryFn: () => matchApi.getCompleted(),
    staleTime: 60_000,
  })
}

export function useSquads(matchId: string) {
  return useQuery({
    queryKey: matchKeys.squads(matchId),
    queryFn: () => matchApi.getSquads(matchId),
    enabled: !!matchId,
    staleTime: Infinity,
  })
}

export function useMatchDetail(matchId: string, refetchInterval?: number) {
  return useQuery({
    queryKey: matchKeys.detail(matchId),
    queryFn: () => matchApi.getDetail(matchId),
    enabled: !!matchId,
    staleTime: 10_000,
    refetchInterval,
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
