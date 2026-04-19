import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useMatchDetail, useCommentary } from '@/hooks/useMatches'
import LiveScoreBoard from '@/components/match/LiveScoreBoard'
import ScorecardTable from '@/components/match/ScorecardTable'
import CommentaryFeed from '@/components/match/CommentaryFeed'
import type { LiveUpdate } from '@/types/match'
import { cn } from '@/lib/utils'

type Tab = 'scorecard' | 'commentary' | 'stats'

export default function MatchDetailPage() {
  const { matchId } = useParams<{ matchId: string }>()
  const [activeTab, setActiveTab] = useState<Tab>('scorecard')
  const [liveUpdate, setLiveUpdate] = useState<LiveUpdate | null>(null)

  const { data: match, isLoading, error } = useMatchDetail(matchId!)
  const { data: commentary } = useCommentary(matchId!)

  if (isLoading) return <MatchDetailSkeleton />
  if (error || !match) return (
    <div className="text-center py-16 text-slate-400">Match not found.</div>
  )

  const tabs: { id: Tab; label: string }[] = [
    { id: 'scorecard', label: 'Scorecard' },
    { id: 'commentary', label: 'Commentary' },
    { id: 'stats', label: 'Stats' },
  ]

  return (
    <div className="space-y-6">
      {/* Match header */}
      <div>
        <p className="text-sm text-slate-400 mb-1">{match.seriesName}</p>
        <h1 className="text-xl font-bold">
          {match.homeTeam.name} vs {match.awayTeam.name}
        </h1>
        <p className="text-sm text-slate-400">
          {match.venue.name}, {match.venue.city} · {match.format}
        </p>
      </div>

      {/* Live scoreboard */}
      <LiveScoreBoard match={match} onLiveUpdate={setLiveUpdate} />

      {/* Tabs */}
      <div className="border-b border-slate-200 dark:border-slate-700">
        <nav className="flex gap-1">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={cn(
                'px-4 py-2.5 text-sm font-medium border-b-2 transition-colors',
                activeTab === tab.id
                  ? 'border-brand-600 text-brand-600'
                  : 'border-transparent text-slate-500 hover:text-slate-900 dark:hover:text-slate-100',
              )}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab content */}
      {activeTab === 'scorecard' && (
        <div className="space-y-8">
          {match.innings.map((inn) => (
            <div key={inn.id}>
              <h2 className="font-semibold mb-3">
                {inn.battingTeam.name} Innings
                {inn.target && (
                  <span className="ml-2 text-sm text-slate-400 font-normal">
                    (Target: {inn.target})
                  </span>
                )}
              </h2>
              <ScorecardTable innings={inn} />
            </div>
          ))}
          {match.innings.length === 0 && (
            <p className="text-slate-400 text-sm">Scorecard will be available once the match starts.</p>
          )}
        </div>
      )}

      {activeTab === 'commentary' && (
        <CommentaryFeed
          initialEntries={(commentary ?? []).map((c: { id: string; text: string; eventType?: string; timestamp: string }) => ({
            id: c.id,
            text: c.text,
            eventType: c.eventType,
            timestamp: new Date(c.timestamp).getTime(),
          }))}
          liveUpdate={liveUpdate}
        />
      )}

      {activeTab === 'stats' && (
        <div className="text-center py-16 text-slate-400">
          <p>Match stats will be available at the end of each innings.</p>
        </div>
      )}
    </div>
  )
}

function MatchDetailSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      <div className="h-6 bg-slate-200 dark:bg-slate-700 rounded w-2/3" />
      <div className="h-40 bg-slate-200 dark:bg-slate-700 rounded-xl" />
      <div className="h-10 bg-slate-200 dark:bg-slate-700 rounded" />
      <div className="h-64 bg-slate-200 dark:bg-slate-700 rounded" />
    </div>
  )
}
