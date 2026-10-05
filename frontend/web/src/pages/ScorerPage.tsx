import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import TokenGate from '@/components/scorer/TokenGate'
import { useLiveMatches, useMatchDetail, useSquads, useUpcomingMatches, matchKeys } from '@/hooks/useMatches'
import { TOKEN_KEY, errorMessage, matchApi, scoringApi } from '@/lib/api'
import {
  isLegalBall,
  shouldSwapStrike,
  toBallInput,
  validateDraft,
  type BallDraft,
  type Modifier,
} from '@/lib/scorer'
import type { BallAck, InningsDetail, MatchDetail, Player, ScorerDismissal, Squad } from '@/types/match'
import { cn } from '@/lib/utils'

const RUN_BUTTONS = [0, 1, 2, 3, 4, 5, 6]
const DISMISSALS: ScorerDismissal[] = ['BOWLED', 'CAUGHT', 'LBW', 'RUN_OUT', 'STUMPED', 'HIT_WICKET']
const NEEDS_FIELDER: ScorerDismissal[] = ['CAUGHT', 'RUN_OUT', 'STUMPED']
const MODIFIERS: { id: Modifier; label: string }[] = [
  { id: 'none', label: 'Normal' },
  { id: 'wide', label: 'Wide' },
  { id: 'noBall', label: 'No ball' },
  { id: 'bye', label: 'Bye' },
  { id: 'legBye', label: 'Leg bye' },
]

export default function ScorerPage() {
  const { matchId } = useParams<{ matchId: string }>()
  const [signedIn, setSignedIn] = useState(() => !!localStorage.getItem(TOKEN_KEY))

  if (!signedIn) return <TokenGate onSaved={() => setSignedIn(true)} />

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Scorer console</h1>
        <button
          className="text-sm text-slate-500 hover:text-red-500"
          onClick={() => {
            localStorage.removeItem(TOKEN_KEY)
            setSignedIn(false)
          }}
        >
          Sign out
        </button>
      </div>
      {matchId ? <Console matchId={matchId} /> : <MatchPicker />}
    </div>
  )
}

function MatchPicker() {
  const { data: live } = useLiveMatches()
  const { data: upcoming } = useUpcomingMatches()
  const matches = [...(live ?? []), ...(upcoming?.content ?? [])]

  return (
    <div className="space-y-2">
      <p className="text-sm text-slate-500">Choose a match to score.</p>
      {matches.length === 0 && <p className="text-sm text-slate-400">No live or upcoming matches.</p>}
      {matches.map((m) => (
        <Link
          key={m.id}
          to={`/score/${m.id}`}
          className="flex items-center justify-between rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-4 py-3 hover:border-brand-600"
        >
          <span className="font-medium">
            {m.homeTeam.name} v {m.awayTeam.name} <span className="text-slate-400 text-sm">· {m.format}</span>
          </span>
          <span className="text-sm text-slate-500">{m.statusText}</span>
        </Link>
      ))}
    </div>
  )
}

function Console({ matchId }: { matchId: string }) {
  const { data: match, error } = useMatchDetail(matchId, 3000)
  const { data: squads } = useSquads(matchId)

  if (error) return <p className="text-red-500 text-sm">Could not load the match: {errorMessage(error)}</p>
  if (!match || !squads) return <p className="text-slate-400 text-sm">Loading…</p>

  const active = match.innings.find((i) => i.status === 'IN_PROGRESS')

  return (
    <div className="space-y-4">
      <Header match={match} />
      {active ? <ScoringPad match={match} innings={active} squads={squads} /> : <StartInnings match={match} />}
    </div>
  )
}

function Header({ match }: { match: MatchDetail }) {
  return (
    <div className="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-4">
      <div className="flex items-center justify-between">
        <Link to={`/match/${match.id}`} className="font-semibold hover:text-brand-600">
          {match.homeTeam.name} v {match.awayTeam.name}
        </Link>
        <span className="text-sm text-slate-500">{match.statusText}</span>
      </div>
      <div className="mt-2 flex flex-wrap gap-x-6 gap-y-1 font-mono text-sm">
        {match.innings.map((i) => (
          <span key={i.id} className={cn(i.status === 'IN_PROGRESS' && 'font-bold text-brand-600')}>
            {i.battingTeam.shortName} {i.totalRuns}/{i.wickets} ({i.overs})
          </span>
        ))}
      </div>
    </div>
  )
}

function StartInnings({ match }: { match: MatchDetail }) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const finished = ['COMPLETED', 'ABANDONED', 'NO_RESULT'].includes(match.status)
  const firstBatter = match.innings[0]?.battingTeam.id
  const candidates = [match.homeTeam, match.awayTeam].filter((t) => t.id !== firstBatter)

  if (finished) return <p className="text-sm text-slate-500">This match is finished: {match.statusText}.</p>
  if (match.innings.length >= 2 && match.format !== 'TEST') {
    return <p className="text-sm text-slate-500">Both innings are done; waiting for the result.</p>
  }

  const start = async (teamId: string) => {
    setBusy(true)
    setError(null)
    try {
      await matchApi.startInnings(match.id, teamId)
      await queryClient.invalidateQueries({ queryKey: matchKeys.detail(match.id) })
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-4">
      <h2 className="font-semibold">Start innings {match.innings.length + 1}</h2>
      <p className="text-sm text-slate-500">Which team bats?</p>
      <div className="flex gap-2">
        {candidates.map((t) => (
          <button
            key={t.id}
            disabled={busy}
            onClick={() => start(t.id)}
            className="px-4 py-2 rounded-lg bg-brand-600 text-white text-sm font-medium disabled:opacity-50"
          >
            {t.name} bat
          </button>
        ))}
      </div>
      {error && <p className="text-sm text-red-500">{error}</p>}
    </div>
  )
}

interface ScoringPadProps {
  match: MatchDetail
  innings: InningsDetail
  squads: Squad[]
}

function ScoringPad({ match, innings, squads }: ScoringPadProps) {
  const queryClient = useQueryClient()
  const battingSquad = squads.find((s) => s.team.id === innings.battingTeam.id)?.players ?? []
  const bowlingSquad = squads.find((s) => s.team.id === innings.bowlingTeam.id)?.players ?? []

  const [striker, setStriker] = useState('')
  const [nonStriker, setNonStriker] = useState('')
  const [bowler, setBowler] = useState('')
  const [modifier, setModifier] = useState<Modifier>('none')
  const [wicketMode, setWicketMode] = useState(false)
  const [dismissal, setDismissal] = useState<ScorerDismissal>('BOWLED')
  const [dismissedId, setDismissedId] = useState('')
  const [fielder, setFielder] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastAck, setLastAck] = useState<BallAck | null>(null)

  const outIds = useMemo(
    () => new Set(innings.batting.filter((b) => b.dismissalType).map((b) => b.playerId)),
    [innings.batting],
  )

  // Start with the first two batters who are not out; re-derive only when the innings changes.
  useEffect(() => {
    const available = battingSquad.filter((p) => !outIds.has(p.id))
    const atCrease = innings.batting.filter((b) => !b.dismissalType).map((b) => b.playerId)
    const pair = [...atCrease, ...available.map((p) => p.id).filter((id) => !atCrease.includes(id))].slice(0, 2)
    setStriker(pair[0] ?? '')
    setNonStriker(pair[1] ?? '')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [innings.id, battingSquad.length])

  const freeBatters = battingSquad.filter((p) => !outIds.has(p.id) && p.id !== striker && p.id !== nonStriker)

  const draft = (runs: number): BallDraft => ({
    matchId: match.id,
    inningsId: innings.id,
    batterId: striker,
    bowlerId: bowler,
    runs,
    modifier,
    wicket: wicketMode,
    dismissalType: dismissal,
    dismissedBatterId: dismissedId,
    fielderId: fielder,
  })

  const disabledRun = (runs: number) =>
    busy || (modifier === 'wide' && runs > 4) || ((modifier === 'bye' || modifier === 'legBye') && runs < 1)

  const submit = async (runs: number) => {
    const d = draft(runs)
    const problem = validateDraft(d)
    if (problem) {
      setError(problem)
      return
    }
    setBusy(true)
    setError(null)
    try {
      const ack = await scoringApi.recordBall(toBallInput(d, crypto.randomUUID()))
      setLastAck(ack)
      applyAfterBall(d, ack)
      await queryClient.invalidateQueries({ queryKey: matchKeys.detail(match.id) })
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const applyAfterBall = (d: BallDraft, ack: BallAck) => {
    const endsOver = ack.legalBall && ack.ballNumber === 6
    if (shouldSwapStrike(d, endsOver)) {
      setStriker(nonStriker)
      setNonStriker(striker)
    }
    if (d.wicket) {
      const out = d.dismissedBatterId || d.batterId
      if (out === striker) setStriker('')
      else if (out === nonStriker) setNonStriker('')
    }
    if (endsOver) setBowler('')
    setModifier('none')
    setWicketMode(false)
    setFielder('')
    setDismissedId('')
  }

  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-3">
        <PlayerSelect label="Striker" value={striker} onChange={setStriker} players={[...(battingSquad.filter((p) => p.id === striker)), ...freeBatters]} />
        <PlayerSelect label="Non-striker" value={nonStriker} onChange={setNonStriker} players={[...(battingSquad.filter((p) => p.id === nonStriker)), ...freeBatters]} />
        <PlayerSelect label="Bowler" value={bowler} onChange={setBowler} players={bowlingSquad} />
      </div>

      <div className="flex flex-wrap gap-2" role="group" aria-label="Delivery type">
        {MODIFIERS.map((m) => (
          <button
            key={m.id}
            onClick={() => setModifier(m.id)}
            aria-pressed={modifier === m.id}
            className={cn(
              'px-3 py-1.5 rounded-lg border text-sm',
              modifier === m.id ? 'bg-brand-600 text-white border-brand-600' : 'border-slate-300 dark:border-slate-600',
            )}
          >
            {m.label}
          </button>
        ))}
        <button
          onClick={() => setWicketMode((w) => !w)}
          aria-pressed={wicketMode}
          className={cn(
            'px-3 py-1.5 rounded-lg border text-sm ml-auto',
            wicketMode ? 'bg-red-600 text-white border-red-600' : 'border-red-300 text-red-600',
          )}
        >
          Wicket
        </button>
      </div>

      {wicketMode && (
        <div className="grid gap-3 sm:grid-cols-3 rounded-lg border border-red-200 dark:border-red-800 p-3">
          <label className="text-sm">
            <span className="block text-xs text-slate-500 mb-1">How out</span>
            <select value={dismissal} onChange={(e) => setDismissal(e.target.value as ScorerDismissal)} className={selectClass}>
              {DISMISSALS.map((d) => <option key={d} value={d}>{d.replace('_', ' ')}</option>)}
            </select>
          </label>
          {NEEDS_FIELDER.includes(dismissal) && (
            <PlayerSelect label="Fielder" value={fielder} onChange={setFielder} players={bowlingSquad} optional />
          )}
          {dismissal === 'RUN_OUT' && (
            <PlayerSelect
              label="Who is out"
              value={dismissedId}
              onChange={setDismissedId}
              players={battingSquad.filter((p) => p.id === striker || p.id === nonStriker)}
              optional
            />
          )}
        </div>
      )}

      <div className="grid grid-cols-7 gap-2" role="group" aria-label="Runs">
        {RUN_BUTTONS.map((r) => (
          <button
            key={r}
            disabled={disabledRun(r)}
            onClick={() => submit(r)}
            className={cn(
              'py-4 rounded-xl border text-lg font-bold disabled:opacity-30',
              r === 4 && 'bg-green-50 dark:bg-green-950 border-green-300',
              r === 6 && 'bg-purple-50 dark:bg-purple-950 border-purple-300',
              r !== 4 && r !== 6 && 'border-slate-300 dark:border-slate-600',
            )}
          >
            {r}
          </button>
        ))}
      </div>
      <p className="text-xs text-slate-400">
        {modifier === 'wide' && 'Runs pressed are run on top of the wide penalty run.'}
        {modifier === 'noBall' && 'Runs pressed are off the bat; the no-ball penalty run is added automatically.'}
        {(modifier === 'bye' || modifier === 'legBye') && 'Runs pressed are the byes / leg-byes taken.'}
        {modifier === 'none' && !wicketMode && 'Press the runs scored off the bat. Strike rotates automatically.'}
        {wicketMode && ' Press the runs completed to record the wicket.'}
        {!isLegalBall({ modifier }) && ' This delivery will not count towards the over.'}
      </p>

      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}
      {lastAck && !error && (
        <p className="text-sm text-slate-500">
          Recorded {lastAck.overNumber}.{lastAck.ballNumber}
          {lastAck.duplicate ? ' (duplicate ignored)' : ''}
        </p>
      )}
    </div>
  )
}

const selectClass =
  'w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-2 py-2 text-sm'

interface PlayerSelectProps {
  label: string
  value: string
  onChange: (id: string) => void
  players: Player[]
  optional?: boolean
}

function PlayerSelect({ label, value, onChange, players, optional }: PlayerSelectProps) {
  return (
    <label className="text-sm">
      <span className="block text-xs text-slate-500 mb-1">{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)} className={selectClass}>
        <option value="">{optional ? '—' : 'Select…'}</option>
        {players.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
      </select>
    </label>
  )
}
