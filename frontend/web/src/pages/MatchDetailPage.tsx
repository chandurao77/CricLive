import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useCommentary, useMatchDetail, matchKeys } from '@/hooks/useMatches'
import { useLiveMatch } from '@/hooks/useLiveMatch'
import LiveScoreBoard from '@/components/match/LiveScoreBoard'
import ScorecardTable from '@/components/match/ScorecardTable'
import CommentaryFeed from '@/components/match/CommentaryFeed'
import { fromLiveUpdate, fromRecord, mergeCommentary, type CommentaryEntry } from '@/lib/commentary'
import type { LiveUpdate } from '@/types/match'
import { cn } from '@/lib/utils'

type Tab = 'scorecard' | 'commentary'

const REFETCH_DEBOUNCE_MS = 250

export default function MatchDetailPage() {
  const { matchId } = useParams<{ matchId: string }>()
  const queryClient = useQueryClient()
  const [activeTab, setActiveTab] = useState<Tab>('scorecard')
  const [liveEntries, setLiveEntries] = useState<CommentaryEntry[]>([])
  const refetchTimer = useRef<ReturnType<typeof setTimeout>>()

  const { data: match, isLoading, error } = useMatchDetail(matchId!)
  const { data: stored } = useCommentary(matchId!)

  const onUpdate = useCallback(
    (update: LiveUpdate) => {
      if (update.type === 'SCORE') {
        // The scorecard is the source of truth: re-fetch it (debounced) when the score changes.
        clearTimeout(refetchTimer.current)
        refetchTimer.current = setTimeout(
          () => queryClient.invalidateQueries({ queryKey: matchKeys.detail(matchId!) }),
          REFETCH_DEBOUNCE_MS,
        )
      }
      const entry = fromLiveUpdate(update)
      if (entry) setLiveEntries((prev) => [...prev, entry])
    },
    [matchId, queryClient],
  )

  useEffect(() => () => clearTimeout(refetchTimer.current), [])

  const finished = match?.status === 'COMPLETED' || match?.status === 'ABANDONED' || match?.status === 'NO_RESULT'
  const { connectionState } = useLiveMatch({ matchId: matchId!, onUpdate, enabled: !!match && !finished })

  const entries = useMemo(
    () => mergeCommentary((stored ?? []).map(fromRecord), liveEntries),
    [stored, liveEntries],
  )

  if (isLoading) return <MatchDetailSkeleton />
  if (error || !match) return <div className="text-center py-16 text-slate-400">Match not found.</div>

  const tabs: { id: Tab; label: string }[] = [
    { id: 'scorecard', label: 'Scorecard' },
    { id: 'commentary', label: 'Commentary' },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm text-slate-400 mb-1">{match.seriesName}</p>
          <h1 className="text-xl font-bold">
            {match.homeTeam.name} vs {match.awayTeam.name}
          </h1>
          <p className="text-sm text-slate-400">
            {match.venue.name}, {match.venue.city} · {match.format}
          </p>
        </div>
        {!finished && (
          <Link
            to={`/score/${match.id}`}
            className="shrink-0 text-sm font-medium px-3 py-1.5 rounded-lg border border-slate-300 dark:border-slate-600 hover:border-brand-600 hover:text-brand-600 transition-colors"
          >
            Score this match
          </Link>
        )}
      </div>

      <LiveScoreBoard match={match} connection={connectionState} />

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

      {activeTab === 'scorecard' && (
        <div className="space-y-8">
          {match.innings.map((inn) => (
            <div key={inn.id}>
              <h2 className="font-semibold mb-3">
                {inn.battingTeam.name} Innings
                {inn.target && (
                  <span className="ml-2 text-sm text-slate-400 font-normal">(Target: {inn.target})</span>
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

      {activeTab === 'commentary' && <CommentaryFeed entries={entries} />}
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
