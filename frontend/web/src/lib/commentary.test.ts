import { describe, expect, it } from 'vitest'
import { fromLiveUpdate, fromRecord, mergeCommentary, type CommentaryEntry } from '@/lib/commentary'
import type { LiveUpdate } from '@/types/match'

const entry = (id: string, timestamp: number): CommentaryEntry => ({ id, over: '0.1', text: id, timestamp })

describe('fromLiveUpdate', () => {
  const base: LiveUpdate = { type: 'COMMENTARY', matchId: 'm', id: 'c1', commentary: 'FOUR!', over: '0.2' }

  it('maps a commentary message', () => {
    expect(fromLiveUpdate(base)).toMatchObject({ id: 'c1', over: '0.2', text: 'FOUR!' })
  })

  it('ignores score messages and incomplete commentary', () => {
    expect(fromLiveUpdate({ ...base, type: 'SCORE' })).toBeNull()
    expect(fromLiveUpdate({ ...base, id: undefined })).toBeNull()
    expect(fromLiveUpdate({ ...base, commentary: undefined })).toBeNull()
  })
})

describe('fromRecord', () => {
  it('formats over.ball and parses the timestamp', () => {
    const e = fromRecord({
      id: 'x', matchId: 'm', overNumber: 3, ballNumber: 4, text: 't', eventType: 'WICKET',
      timestamp: '2025-01-01T00:00:00Z',
    } as never)
    expect(e.over).toBe('3.4')
    expect(e.timestamp).toBe(Date.parse('2025-01-01T00:00:00Z'))
  })
})

describe('mergeCommentary', () => {
  it('de-duplicates by id, preferring the live copy, newest first', () => {
    const merged = mergeCommentary([entry('a', 1), entry('b', 2)], [{ ...entry('b', 2), text: 'live' }, entry('c', 3)])
    expect(merged.map((e) => e.id)).toEqual(['c', 'b', 'a'])
    expect(merged.find((e) => e.id === 'b')?.text).toBe('live')
  })

  it('respects the limit', () => {
    const many = Array.from({ length: 10 }, (_, i) => entry(String(i), i))
    expect(mergeCommentary(many, [], 3)).toHaveLength(3)
  })
})
