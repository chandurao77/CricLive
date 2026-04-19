'use client'

import { useState } from 'react'
import type { MatchDetail, LiveUpdate } from '@/types/match'
import { useLiveMatch } from '@/hooks/useLiveMatch'
import { cn, isLive } from '@/lib/utils'

interface LiveScoreBoardProps {
  match: MatchDetail
  onLiveUpdate?: (update: LiveUpdate) => void
}

export default function LiveScoreBoard({ match, onLiveUpdate }: LiveScoreBoardProps) {
  const [liveScore, setLiveScore] = useState<{ runs: number; wickets: number; overs: string; runRate: number } | null>(null)

  const { connectionState } = useLiveMatch({
    matchId: match.id,
    enabled: isLive(match.status),
    onUpdate: (update) => {
      if (update.runs !== undefined) {
        setLiveScore({
          runs: update.runs,
          wickets: update.wickets ?? 0,
          overs: update.overs ?? '0.0',
          runRate: update.runRate ?? 0,
        })
      }
      onLiveUpdate?.(update)
    },
  })

  const currentInnings = match.innings.find((i) => i.status === 'IN_PROGRESS') ?? match.innings.at(-1)
  const displayed = liveScore ?? (currentInnings ? {
    runs: currentInnings.totalRuns,
    wickets: currentInnings.wickets,
    overs: currentInnings.overs,
    runRate: currentInnings.runRate,
  } : null)

  return (
    <div className="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5">
      {/* Connection indicator */}
      {isLive(match.status) && (
        <div className="flex items-center gap-2 mb-4">
          <span className={cn(
            'w-2 h-2 rounded-full',
            connectionState === 'connected' ? 'bg-green-500 animate-pulse' : 'bg-slate-300',
          )} />
          <span className="text-xs text-slate-400">
            {connectionState === 'connected' ? 'Live' : connectionState}
          </span>
        </div>
      )}

      {/* Team scores */}
      <div className="space-y-3 mb-4">
        {match.innings.map((inn) => (
          <div key={inn.id} className="flex items-center justify-between">
            <span className="font-semibold text-sm">{inn.battingTeam.shortName}</span>
            <span className={cn(
              'font-mono font-bold text-lg',
              inn.status === 'IN_PROGRESS' && 'text-brand-600',
            )}>
              {inn.totalRuns}/{inn.wickets}
              <span className="text-xs font-normal text-slate-400 ml-1">({inn.overs})</span>
            </span>
          </div>
        ))}
      </div>

      {/* Live stats bar */}
      {displayed && (
        <div className="grid grid-cols-3 gap-2 pt-3 border-t border-slate-100 dark:border-slate-700">
          <StatCell label="CRR" value={displayed.runRate.toFixed(2)} />
          {currentInnings?.requiredRunRate && (
            <StatCell label="RRR" value={currentInnings.requiredRunRate.toFixed(2)} highlight />
          )}
          {currentInnings?.target && (
            <StatCell label="Target" value={String(currentInnings.target)} />
          )}
        </div>
      )}
    </div>
  )
}

function StatCell({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="text-center">
      <div className="text-xs text-slate-400">{label}</div>
      <div className={cn('font-bold text-sm', highlight && 'text-red-500')}>{value}</div>
    </div>
  )
}
