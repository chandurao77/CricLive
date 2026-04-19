import { useState } from 'react'
import { useLiveMatches, useUpcomingMatches } from '@/hooks/useMatches'
import MatchCard from '@/components/match/MatchCard'

export default function HomePage() {
  const { data: liveMatches, isLoading: liveLoading } = useLiveMatches()
  const { data: upcomingPage, isLoading: upcomingLoading } = useUpcomingMatches()

  return (
    <div className="space-y-8">
      {/* Live matches */}
      <section>
        <div className="flex items-center gap-2 mb-4">
          <span className="live-dot" />
          <h2 className="text-lg font-bold">Live</h2>
        </div>

        {liveLoading ? (
          <MatchCardSkeleton count={2} />
        ) : liveMatches?.length === 0 ? (
          <p className="text-slate-400 text-sm">No matches live right now.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {liveMatches?.map((m) => <MatchCard key={m.id} match={m} />)}
          </div>
        )}
      </section>

      {/* Upcoming matches */}
      <section>
        <h2 className="text-lg font-bold mb-4">Upcoming</h2>

        {upcomingLoading ? (
          <MatchCardSkeleton count={4} />
        ) : upcomingPage?.content.length === 0 ? (
          <p className="text-slate-400 text-sm">No upcoming matches scheduled.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {upcomingPage?.content.map((m) => <MatchCard key={m.id} match={m} />)}
          </div>
        )}
      </section>
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
