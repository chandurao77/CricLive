import type { MatchDetail } from '@/types/match'
import type { ConnectionState } from '@/hooks/useLiveMatch'
import { cn, isLive } from '@/lib/utils'

interface LiveScoreBoardProps {
  match: MatchDetail
  connection: ConnectionState
}

export default function LiveScoreBoard({ match, connection }: LiveScoreBoardProps) {
  const currentInnings = match.innings.find((i) => i.status === 'IN_PROGRESS') ?? match.innings.at(-1)

  return (
    <div className="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          {isLive(match.status) && (
            <span className={cn('w-2 h-2 rounded-full', connection === 'connected' ? 'bg-green-500 animate-pulse' : 'bg-slate-300')} />
          )}
          <span className="text-sm font-semibold">{match.statusText}</span>
        </div>
        {isLive(match.status) && (
          <span className="text-xs text-slate-400">{connection === 'connected' ? 'Live updates on' : connection}</span>
        )}
      </div>

      <div className="space-y-3 mb-4">
        {match.innings.length === 0 && (
          <p className="text-sm text-slate-400">Waiting for the first innings to start.</p>
        )}
        {match.innings.map((inn) => (
          <div key={inn.id} className="flex items-center justify-between">
            <span className="font-semibold text-sm">{inn.battingTeam.shortName}</span>
            <span className={cn('font-mono font-bold text-lg', inn.status === 'IN_PROGRESS' && 'text-brand-600')}>
              {inn.totalRuns}/{inn.wickets}
              <span className="text-xs font-normal text-slate-400 ml-1">({inn.overs})</span>
            </span>
          </div>
        ))}
      </div>

      {currentInnings && (
        <div className="grid grid-cols-3 gap-2 pt-3 border-t border-slate-100 dark:border-slate-700">
          <StatCell label="CRR" value={(currentInnings.runRate ?? 0).toFixed(2)} />
          {currentInnings.requiredRunRate != null && (
            <StatCell label="RRR" value={currentInnings.requiredRunRate.toFixed(2)} highlight />
          )}
          {currentInnings.target != null && <StatCell label="Target" value={String(currentInnings.target)} />}
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
