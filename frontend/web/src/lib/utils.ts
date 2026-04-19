import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { formatDistanceToNow, format } from 'date-fns'
import type { MatchStatus } from '@/types/match'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatMatchTime(isoString: string): string {
  const date = new Date(isoString)
  const now = new Date()
  const diffMs = date.getTime() - now.getTime()

  if (Math.abs(diffMs) < 60 * 60 * 1000) {
    return formatDistanceToNow(date, { addSuffix: true })
  }
  return format(date, 'dd MMM, HH:mm')
}

export function isLive(status: MatchStatus): boolean {
  return status === 'LIVE' || status === 'INNINGS_BREAK' || status === 'RAIN_DELAY'
}

export function formatScore(runs: number, wickets: number, overs: string): string {
  return `${runs}/${wickets} (${overs})`
}

export function statusColor(status: MatchStatus): string {
  switch (status) {
    case 'LIVE': return 'text-red-500'
    case 'UPCOMING': return 'text-brand-600'
    case 'COMPLETED': return 'text-slate-500'
    case 'RAIN_DELAY': return 'text-blue-500'
    case 'INNINGS_BREAK': return 'text-amber-500'
    default: return 'text-slate-400'
  }
}
