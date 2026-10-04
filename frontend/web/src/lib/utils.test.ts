import { describe, expect, it } from 'vitest'
import { formatScore, isLive } from '@/lib/utils'

describe('utils', () => {
  it('formats a score line', () => {
    expect(formatScore(221, 5, '20.0')).toContain('221')
  })

  it('treats live-like statuses as live', () => {
    expect(isLive('LIVE')).toBe(true)
    expect(isLive('UPCOMING')).toBe(false)
  })
})
