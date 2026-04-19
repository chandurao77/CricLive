import { Link } from 'react-router-dom'
import type { MatchSummary } from '@/types/match'
import { cn, formatMatchTime, formatScore, isLive, statusColor } from '@/lib/utils'

interface MatchCardProps {
  match: MatchSummary
}

export default function MatchCard({ match }: MatchCardProps) {
  const live = isLive(match.status)

  return (
    <Link
      to={`/match/${match.id}`}
      className={cn(
        'block rounded-xl border p-4 transition-all hover:shadow-md',
        'bg-white dark:bg-slate-800',
        'border-slate-200 dark:border-slate-700',
        live && 'border-red-200 dark:border-red-800 ring-1 ring-red-100 dark:ring-red-900',
      )}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs text-slate-400 truncate max-w-[180px]">{match.seriesName}</span>
        <div className="flex items-center gap-1.5">
          {live && <span className="live-dot" />}
          <span className={cn('text-xs font-semibold', statusColor(match.status))}>
            {match.statusText}
          </span>
        </div>
      </div>

      {/* Teams */}
      <div className="space-y-2">
        <TeamRow
          team={match.homeTeam}
          innings={match.firstInnings?.battingTeam?.id === match.homeTeam.id ? match.firstInnings : match.secondInnings}
          isCurrentInnings={
            match.firstInnings?.battingTeam?.id === match.homeTeam.id ||
            match.secondInnings?.battingTeam?.id === match.homeTeam.id
          }
        />
        <TeamRow
          team={match.awayTeam}
          innings={match.firstInnings?.battingTeam?.id === match.awayTeam.id ? match.firstInnings : match.secondInnings}
          isCurrentInnings={
            match.firstInnings?.battingTeam?.id === match.awayTeam.id ||
            match.secondInnings?.battingTeam?.id === match.awayTeam.id
          }
        />
      </div>

      {/* Footer */}
      <div className="mt-3 pt-2 border-t border-slate-100 dark:border-slate-700 text-xs text-slate-400">
        {match.venueName}, {match.venueCity} · {formatMatchTime(match.scheduledStart)}
      </div>
    </Link>
  )
}

interface TeamRowProps {
  team: MatchSummary['homeTeam']
  innings: MatchSummary['firstInnings']
  isCurrentInnings: boolean
}

function TeamRow({ team, innings, isCurrentInnings }: TeamRowProps) {
  return (
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-2">
        {team.flagUrl ? (
          <img src={team.flagUrl} alt={team.name} className="w-5 h-4 object-cover rounded-sm" />
        ) : (
          <div className="w-5 h-4 bg-slate-200 dark:bg-slate-600 rounded-sm" />
        )}
        <span className="font-semibold text-sm">{team.shortName}</span>
      </div>
      {innings ? (
        <span className={cn('text-sm font-mono font-bold', isCurrentInnings && 'text-brand-600')}>
          {formatScore(innings.totalRuns, innings.wickets, innings.overs)}
        </span>
      ) : (
        <span className="text-xs text-slate-400">Yet to bat</span>
      )}
    </div>
  )
}
