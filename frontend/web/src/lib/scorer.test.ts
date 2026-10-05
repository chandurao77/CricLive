import { describe, expect, it } from 'vitest'
import { shouldSwapStrike, toBallInput, validateDraft, isLegalBall, type BallDraft } from '@/lib/scorer'

const makeDraft = (over: Partial<BallDraft> = {}): BallDraft => ({
  matchId: 'm1',
  inningsId: 'i1',
  batterId: 'b1',
  bowlerId: 'w1',
  runs: 0,
  modifier: 'none',
  wicket: false,
  dismissalType: 'BOWLED',
  dismissedBatterId: '',
  fielderId: '',
  ...over,
})

describe('toBallInput', () => {
  it('maps runs off the bat for a normal ball', () => {
    const b = toBallInput(makeDraft({ runs: 4 }), 'k')
    expect(b).toMatchObject({ runsScored: 4, extraRuns: 0, wide: false, noBall: false, idempotencyKey: 'k' })
  })

  it('counts the wide penalty plus runs run', () => {
    const b = toBallInput(makeDraft({ modifier: 'wide', runs: 2 }), 'k')
    expect(b).toMatchObject({ wide: true, runsScored: 0, extraRuns: 3 })
  })

  it('credits bat runs on a no-ball with the penalty as an extra', () => {
    const b = toBallInput(makeDraft({ modifier: 'noBall', runs: 6 }), 'k')
    expect(b).toMatchObject({ noBall: true, runsScored: 6, extraRuns: 1 })
  })

  it('treats byes as extras not bat runs', () => {
    const b = toBallInput(makeDraft({ modifier: 'bye', runs: 2 }), 'k')
    expect(b).toMatchObject({ bye: true, runsScored: 0, extraRuns: 2 })
  })

  it('only sends dismissal fields for a wicket', () => {
    expect(toBallInput(makeDraft({ dismissalType: 'CAUGHT', fielderId: 'f' }), 'k').dismissalType).toBeUndefined()
    const w = toBallInput(makeDraft({ wicket: true, dismissalType: 'CAUGHT', fielderId: 'f' }), 'k')
    expect(w).toMatchObject({ wicket: true, dismissalType: 'CAUGHT', fielderId: 'f' })
  })
})

describe('validateDraft', () => {
  it('requires batter and bowler', () => {
    expect(validateDraft(makeDraft({ batterId: '' }))).toMatch(/batter/i)
    expect(validateDraft(makeDraft({ bowlerId: '' }))).toMatch(/bowler/i)
  })

  it('rejects zero byes', () => {
    expect(validateDraft(makeDraft({ modifier: 'bye', runs: 0 }))).not.toBeNull()
  })

  it('rejects a bowled dismissal off a wide or no-ball', () => {
    expect(validateDraft(makeDraft({ modifier: 'wide', wicket: true }))).not.toBeNull()
    expect(validateDraft(makeDraft({ modifier: 'noBall', wicket: true }))).not.toBeNull()
    expect(validateDraft(makeDraft({ modifier: 'noBall', wicket: true, dismissalType: 'RUN_OUT' }))).toBeNull()
  })

  it('accepts a plain valid ball', () => {
    expect(validateDraft(makeDraft())).toBeNull()
  })
})

describe('isLegalBall / shouldSwapStrike', () => {
  it('wides and no-balls are not legal', () => {
    expect(isLegalBall({ modifier: 'wide' })).toBe(false)
    expect(isLegalBall({ modifier: 'noBall' })).toBe(false)
    expect(isLegalBall({ modifier: 'legBye' })).toBe(true)
  })

  it('swaps on odd runs mid-over', () => {
    expect(shouldSwapStrike(makeDraft({ runs: 1 }), false)).toBe(true)
    expect(shouldSwapStrike(makeDraft({ runs: 2 }), false)).toBe(false)
  })

  it('swaps at end of over, but cancels out with an odd run', () => {
    expect(shouldSwapStrike(makeDraft({ runs: 0 }), true)).toBe(true)
    expect(shouldSwapStrike(makeDraft({ runs: 1 }), true)).toBe(false)
  })

  it('never swaps on a wicket', () => {
    expect(shouldSwapStrike(makeDraft({ runs: 1, wicket: true }), false)).toBe(false)
  })
})
