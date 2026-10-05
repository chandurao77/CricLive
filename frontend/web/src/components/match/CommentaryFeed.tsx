import { cn } from '@/lib/utils'
import type { CommentaryEntry } from '@/lib/commentary'

interface CommentaryFeedProps {
  entries: CommentaryEntry[]
}

export default function CommentaryFeed({ entries }: CommentaryFeedProps) {
  return (
    <div className="space-y-2 max-h-[600px] overflow-y-auto pr-1">
      {entries.length === 0 && (
        <div className="text-center text-slate-400 text-sm py-8">
          Commentary will appear here once the match starts.
        </div>
      )}
      {entries.map((entry) => (
        <CommentaryItem key={entry.id} entry={entry} />
      ))}
    </div>
  )
}

const BADGES: Record<string, { label: string; className: string }> = {
  WICKET: { label: 'OUT', className: 'bg-red-600 text-white' },
  SIX: { label: 'SIX', className: 'bg-purple-600 text-white' },
  BOUNDARY: { label: 'FOUR', className: 'bg-green-600 text-white' },
  WIDE: { label: 'WD', className: 'bg-slate-500 text-white' },
  NO_BALL: { label: 'NB', className: 'bg-slate-500 text-white' },
}

function CommentaryItem({ entry }: { entry: CommentaryEntry }) {
  const badge = entry.eventType ? BADGES[entry.eventType] : undefined

  return (
    <div
      className={cn(
        'flex items-start gap-3 rounded-lg px-3 py-2.5 text-sm border',
        entry.eventType === 'WICKET' && 'bg-red-50 dark:bg-red-950 border-red-200 dark:border-red-800',
        entry.eventType === 'SIX' && 'bg-purple-50 dark:bg-purple-950 border-purple-200 dark:border-purple-800',
        entry.eventType === 'BOUNDARY' && 'bg-green-50 dark:bg-green-950 border-green-200 dark:border-green-800',
        !badge && 'bg-white dark:bg-slate-800 border-slate-100 dark:border-slate-700',
        badge && !['WICKET', 'SIX', 'BOUNDARY'].includes(entry.eventType!) && 'bg-white dark:bg-slate-800 border-slate-100 dark:border-slate-700',
      )}
    >
      <span className="font-mono text-xs text-slate-400 w-9 shrink-0 pt-0.5">{entry.over}</span>
      {badge && (
        <span className={cn('text-[10px] font-bold px-1.5 py-0.5 rounded shrink-0', badge.className)}>{badge.label}</span>
      )}
      <span>{entry.text}</span>
    </div>
  )
}
