import { describe, expect, it } from 'vitest'
import { addDays, formatWeekRange, isoWeekNumber, startOfWeek, toISODate, weekDays } from './dates.js'

describe('toISODate', () => {
  it('uses the local calendar date with zero padding', () => {
    expect(toISODate(new Date(2026, 0, 5, 23, 59))).toBe('2026-01-05')
    expect(toISODate(new Date(2026, 9, 11, 0, 0))).toBe('2026-10-11')
  })
})

describe('startOfWeek', () => {
  it('returns the Monday of the same week', () => {
    expect(toISODate(startOfWeek(new Date(2026, 9, 7)))).toBe('2026-10-05') // Wednesday
  })

  it('treats Sunday as the last day of the week, not the first', () => {
    expect(toISODate(startOfWeek(new Date(2026, 9, 11)))).toBe('2026-10-05')
  })

  it('leaves a Monday unchanged', () => {
    expect(toISODate(startOfWeek(new Date(2026, 9, 5)))).toBe('2026-10-05')
  })
})

describe('addDays', () => {
  it('crosses month and year boundaries', () => {
    expect(toISODate(addDays(new Date(2026, 11, 30), 3))).toBe('2027-01-02')
    expect(toISODate(addDays(new Date(2026, 9, 5), -7))).toBe('2026-09-28')
  })

  it('stays on the right calendar day across a clock change', () => {
    // Europe moves its clocks back on 25 October 2026.
    expect(toISODate(addDays(new Date(2026, 9, 24), 2))).toBe('2026-10-26')
  })
})

describe('weekDays', () => {
  const days = weekDays(new Date(2026, 9, 5), new Date(2026, 9, 7, 15, 30))

  it('returns Monday to Sunday', () => {
    expect(days.map((day) => day.iso)).toEqual([
      '2026-10-05',
      '2026-10-06',
      '2026-10-07',
      '2026-10-08',
      '2026-10-09',
      '2026-10-10',
      '2026-10-11',
    ])
    expect(days[0].weekday).toBe('Monday')
    expect(days[6].weekday).toBe('Sunday')
  })

  it('flags only Saturday and Sunday as weekend', () => {
    expect(days.map((day) => day.isWeekend)).toEqual([false, false, false, false, false, true, true])
  })

  it('flags only the given day as today', () => {
    expect(days.filter((day) => day.isToday).map((day) => day.iso)).toEqual(['2026-10-07'])
  })
})

describe('isoWeekNumber', () => {
  it('numbers the current week', () => {
    expect(isoWeekNumber(new Date(2026, 9, 5))).toBe(41)
    expect(isoWeekNumber(new Date(2026, 9, 11))).toBe(41)
  })

  it('handles years with 53 weeks, including days that belong to the previous year', () => {
    expect(isoWeekNumber(new Date(2026, 11, 31))).toBe(53)
    expect(isoWeekNumber(new Date(2027, 0, 1))).toBe(53)
    expect(isoWeekNumber(new Date(2027, 0, 4))).toBe(1)
  })
})

describe('formatWeekRange', () => {
  it('collapses a week inside one month', () => {
    expect(formatWeekRange(new Date(2026, 9, 5))).toBe('5 – 11 October 2026')
  })

  it('names both months when the week spans two', () => {
    expect(formatWeekRange(new Date(2026, 8, 28))).toBe('28 September – 4 October 2026')
  })

  it('names both years when the week spans New Year', () => {
    expect(formatWeekRange(new Date(2026, 11, 28))).toBe('28 December 2026 – 3 January 2027')
  })
})
