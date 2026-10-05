import { useCompletedMatches, useLiveMatches, useUpcomingMatches } from '@/hooks/useMatches'
import MatchCard from '@/components/match/MatchCard'
import type { MatchSummary } from '@/types/match'

export default function HomePage() {
  const { data: liveMatches, isLoading: liveLoading, isError: liveError } = useLiveMatches()
  const { data: upcomingPage, isLoading: upcomingLoading } = useUpcomingMatches()
  const { data: completedPage, isLoading: completedLoading } = useCompletedMatches()

  return (
    <div className="space-y-8">
      <section>
        <div className="flex items-center gap-2 mb-4">
          <span className="live-dot" />
          <h2 className="text-lg font-bold">Live</h2>
        </div>
        {liveError ? (
          <p className="text-red-500 text-sm">Could not load live matches. Is the match service running?</p>
        ) : (
          <MatchGrid
            loading={liveLoading}
            matches={liveMatches}
            skeletons={2}
            empty="No matches live right now."
          />
        )}
      </section>

      <section>
        <h2 className="text-lg font-bold mb-4">Upcoming</h2>
        <MatchGrid
          loading={upcomingLoading}
          matches={upcomingPage?.content}
          skeletons={3}
          empty="No upcoming matches scheduled."
        />
      </section>

      <section>
        <h2 className="text-lg font-bold mb-4">Recent results</h2>
        <MatchGrid
          loading={completedLoading}
          matches={completedPage?.content}
          skeletons={3}
          empty="No completed matches yet."
        />
      </section>
    </div>
  )
}

interface MatchGridProps {
  loading: boolean
  matches: MatchSummary[] | undefined
  skeletons: number
  empty: string
}

function MatchGrid({ loading, matches, skeletons, empty }: MatchGridProps) {
  if (loading) return <MatchCardSkeleton count={skeletons} />
  if (!matches || matches.length === 0) return <p className="text-slate-400 text-sm">{empty}</p>
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      {matches.map((m) => <MatchCard key={m.id} match={m} />)}
    </div>
  )
}

function MatchCardSkeleton({ count }: { count: number }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          className="h-40 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-100 dark:bg-slate-800 animate-pulse"
        />
      ))}
    </div>
  )
}
