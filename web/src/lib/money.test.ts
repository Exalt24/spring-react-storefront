import { describe, expect, it } from 'vitest'

import { formatMoney, formatMoneyOrFree } from './money'

describe('formatMoney', () => {
  it('renders minor units as a currency amount', () => {
    expect(formatMoney(14900)).toBe('$149.00')
  })

  it('keeps trailing cents rather than rounding them away', () => {
    expect(formatMoney(3576)).toBe('$35.76')
  })

  it('handles zero', () => {
    expect(formatMoney(0)).toBe('$0.00')
  })

  /**
   * The whole reason money crosses the wire as an integer. 0.1 + 0.2 style
   * float error cannot happen if the client never sees a float.
   */
  it('does not accumulate float error across a large amount', () => {
    expect(formatMoney(99999999)).toBe('$999,999.99')
  })

  it('respects a non-default currency', () => {
    expect(formatMoney(120000, 'PHP', 'en-US')).toContain('1,200.00')
  })
})

describe('formatMoneyOrFree', () => {
  it('says Free for zero rather than showing a zero amount', () => {
    expect(formatMoneyOrFree(0)).toBe('Free')
  })

  it('formats a real shipping charge normally', () => {
    expect(formatMoneyOrFree(799)).toBe('$7.99')
  })
})
