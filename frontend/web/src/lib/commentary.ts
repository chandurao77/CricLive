import type { LiveUpdate } from '@/types/match'
import type { CommentaryRecord } from '@/lib/api'

export interface CommentaryEntry {
  id: string
  over: string
  text: string
  eventType?: string
  timestamp: number
}

export function fromRecord(r: CommentaryRecord): CommentaryEntry {
  return {
    id: r.id,
    over: `${r.overNumber}.${r.ballNumber}`,
    text: r.text,
    eventType: r.eventType,
    timestamp: new Date(r.timestamp).getTime(),
  }
}

export function fromLiveUpdate(u: LiveUpdate): CommentaryEntry | null {
  if (u.type !== 'COMMENTARY' || !u.commentary || !u.id) return null
  return {
    id: u.id,
    over: u.over ?? '',
    text: u.commentary,
    eventType: u.eventType,
    timestamp: u.timestamp ? new Date(u.timestamp).getTime() : Date.now(),
  }
}

/** Merges stored and live entries, drops duplicates by id, newest first. */
export function mergeCommentary(stored: CommentaryEntry[], live: CommentaryEntry[], limit = 100): CommentaryEntry[] {
  const byId = new Map<string, CommentaryEntry>()
  for (const e of [...stored, ...live]) byId.set(e.id, e)
  return [...byId.values()].sort((a, b) => b.timestamp - a.timestamp).slice(0, limit)
}
