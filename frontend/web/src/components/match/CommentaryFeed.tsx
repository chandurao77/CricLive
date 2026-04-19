import { useEffect, useRef, useState } from 'react'
import type { LiveUpdate } from '@/types/match'
import { cn } from '@/lib/utils'

interface CommentaryEntry {
  id: string
  text: string
  eventType?: string
  timestamp: number
}

interface CommentaryFeedProps {
  initialEntries: CommentaryEntry[]
  liveUpdate: LiveUpdate | null
}

export default function CommentaryFeed({ initialEntries, liveUpdate }: CommentaryFeedProps) {
  const [entries, setEntries] = useState<CommentaryEntry[]>(initialEntries)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (liveUpdate?.commentary) {
      setEntries((prev) => [
        {
          id: Date.now().toString(),
          text: liveUpdate.commentary!,
          eventType: liveUpdate.eventType,
          timestamp: Date.now(),
        },
        ...prev.slice(0, 99),  // keep last 100
      ])
    }
  }, [liveUpdate])

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
      <div ref={bottomRef} />
    </div>
  )
}

function CommentaryItem({ entry }: { entry: CommentaryEntry }) {
  const isWicket = entry.eventType === 'WICKET'
  const isBoundary = entry.eventType === 'BOUNDARY'
  const isSix = entry.eventType === 'SIX'

  return (
    <div className={cn(
      'rounded-lg px-3 py-2.5 text-sm border',
      isWicket && 'bg-red-50 dark:bg-red-950 border-red-200 dark:border-red-800',
      isSix && 'bg-purple-50 dark:bg-purple-950 border-purple-200 dark:border-purple-800',
      isBoundary && 'bg-green-50 dark:bg-green-950 border-green-200 dark:border-green-800',
      !isWicket && !isSix && !isBoundary && 'bg-white dark:bg-slate-800 border-slate-100 dark:border-slate-700',
    )}>
      <span dangerouslySetInnerHTML={{ __html: entry.text }} />
    </div>
  )
}
