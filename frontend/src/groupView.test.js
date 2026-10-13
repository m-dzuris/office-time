import { describe, expect, it } from 'vitest'
import { formatJoinCode, groupRows, indexEntries, officeCount, statusFor } from './groupView.js'

const entries = [
  { userId: 1, date: '2026-10-05', status: 'OFFICE' },
  { userId: 1, date: '2026-10-06', status: 'REMOTE' },
  { userId: 2, date: '2026-10-05', status: 'OFFICE' },
  { userId: 3, date: '2026-10-05', status: 'AWAY' },
]

describe('indexEntries', () => {
  it('groups the flat list by person and day', () => {
    expect(indexEntries(entries)).toEqual({
      1: { '2026-10-05': 'OFFICE', '2026-10-06': 'REMOTE' },
      2: { '2026-10-05': 'OFFICE' },
      3: { '2026-10-05': 'AWAY' },
    })
  })

  it('is empty for an empty list', () => {
    expect(indexEntries([])).toEqual({})
  })
})

describe('statusFor', () => {
  const index = indexEntries(entries)

  it('returns the status a person set', () => {
    expect(statusFor(index, 1, '2026-10-06')).toBe('REMOTE')
  })

  it('returns null for a day not set, and for a person with no days at all', () => {
    expect(statusFor(index, 2, '2026-10-06')).toBe(null)
    expect(statusFor(index, 99, '2026-10-05')).toBe(null)
  })
})

describe('officeCount', () => {
  const index = indexEntries(entries)

  it('counts only the people in the office that day', () => {
    expect(officeCount(index, [1, 2, 3], '2026-10-05')).toBe(2)
    expect(officeCount(index, [1, 2, 3], '2026-10-06')).toBe(0)
  })

  it('ignores people who are not in the list', () => {
    expect(officeCount(index, [3], '2026-10-05')).toBe(0)
  })
})

describe('groupRows', () => {
  const members = [
    { userId: 3, displayName: 'zoe' },
    { userId: 1, displayName: 'Ben' },
    { userId: 2, displayName: 'Ada' },
  ]

  it('puts you first, then everyone else alphabetically ignoring case', () => {
    expect(groupRows(members, 3).map((member) => member.displayName)).toEqual(['zoe', 'Ada', 'Ben'])
  })

  it('does not change the list it was given', () => {
    groupRows(members, 3)
    expect(members.map((member) => member.userId)).toEqual([3, 1, 2])
  })
})

describe('formatJoinCode', () => {
  it('splits a code into groups of four', () => {
    expect(formatJoinCode('ABCDEFGHJKLM')).toBe('ABCD-EFGH-JKLM')
  })

  it('leaves a short code alone and never ends with a dash', () => {
    expect(formatJoinCode('ABCD')).toBe('ABCD')
    expect(formatJoinCode('ABCDE')).toBe('ABCD-E')
  })
})
