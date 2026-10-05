import type { BallInput, ScorerDismissal } from '@/types/match'

export type Modifier = 'none' | 'wide' | 'noBall' | 'bye' | 'legBye'

export interface BallDraft {
  matchId: string
  inningsId: string
  batterId: string
  bowlerId: string
  /**
   * The number on the pressed run button. Its meaning depends on the modifier:
   * runs off the bat (none, noBall), runs run on top of the wide, or byes/leg-byes taken.
   */
  runs: number
  modifier: Modifier
  wicket: boolean
  dismissalType: ScorerDismissal
  dismissedBatterId: string
  fielderId: string
}

/** True when the delivery counts towards the six balls of an over. */
export const isLegalBall = (d: Pick<BallDraft, 'modifier'>) => d.modifier !== 'wide' && d.modifier !== 'noBall'

/** Why the draft cannot be submitted, or null when it is valid. */
export function validateDraft(d: BallDraft): string | null {
  if (!d.batterId) return 'Select the batter on strike'
  if (!d.bowlerId) return 'Select the bowler'
  if ((d.modifier === 'bye' || d.modifier === 'legBye') && d.runs < 1) return 'Byes and leg-byes need at least 1 run'
  if (d.wicket && d.modifier === 'noBall' && d.dismissalType !== 'RUN_OUT') return 'Only a run-out is possible off a no-ball'
  if (d.wicket && d.modifier === 'wide' && !['STUMPED', 'RUN_OUT', 'HIT_WICKET'].includes(d.dismissalType)) {
    return 'Only stumped, run-out or hit-wicket are possible off a wide'
  }
  return null
}

/** Builds the scoring-service request body from the console's draft. */
export function toBallInput(d: BallDraft, idempotencyKey: string): BallInput {
  const batRuns = d.modifier === 'none' || d.modifier === 'noBall' ? d.runs : 0
  let extraRuns = 0
  if (d.modifier === 'wide') extraRuns = 1 + d.runs
  else if (d.modifier === 'noBall') extraRuns = 1
  else if (d.modifier === 'bye' || d.modifier === 'legBye') extraRuns = d.runs

  return {
    matchId: d.matchId,
    inningsId: d.inningsId,
    batterId: d.batterId,
    bowlerId: d.bowlerId,
    runsScored: batRuns,
    wide: d.modifier === 'wide',
    noBall: d.modifier === 'noBall',
    bye: d.modifier === 'bye',
    legBye: d.modifier === 'legBye',
    extraRuns,
    wicket: d.wicket,
    dismissalType: d.wicket ? d.dismissalType : undefined,
    dismissedBatterId: d.wicket && d.dismissedBatterId ? d.dismissedBatterId : undefined,
    fielderId: d.wicket && d.fielderId ? d.fielderId : undefined,
    idempotencyKey,
  }
}

/** After a delivery, do the batters swap ends? An odd number of runs, or the end of an over (not both). */
export function shouldSwapStrike(d: BallDraft, endsOver: boolean): boolean {
  if (d.wicket) return false
  return (d.runs % 2 === 1) !== endsOver
}
