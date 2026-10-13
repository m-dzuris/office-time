import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./api.js', () => ({
  fetchRange: vi.fn(),
  savePresence: vi.fn(),
  clearPresence: vi.fn(),
}))

import { clearPresence, fetchRange, savePresence } from './api.js'
import { SAVE_ERROR, usePresence } from './usePresence.js'

const SUNDAY_11_OCT_2026 = () => new Date(2026, 9, 11, 12, 0)
const settle = () => new Promise((resolve) => setTimeout(resolve, 0))

async function openWeek(initialEntries = []) {
  fetchRange.mockResolvedValueOnce(initialEntries)
  const presence = usePresence(SUNDAY_11_OCT_2026)
  await settle()
  return presence
}

beforeEach(() => {
  vi.resetAllMocks()
  savePresence.mockResolvedValue({})
  clearPresence.mockResolvedValue(null)
})

describe('usePresence', () => {
  it('starts on the Monday–Sunday week containing today and loads it', async () => {
    const presence = await openWeek([{ date: '2026-10-07', status: 'REMOTE', note: null }])

    expect(fetchRange).toHaveBeenCalledWith('2026-10-05', '2026-10-11')
    expect(presence.entries.value).toEqual({ '2026-10-07': { status: 'REMOTE', note: '' } })
    expect(presence.isCurrentWeek.value).toBe(true)
  })

  it('shows a chosen status immediately and saves it in the background', async () => {
    const presence = await openWeek()

    const saving = presence.setStatus('2026-10-07', 'OFFICE')
    expect(presence.entries.value['2026-10-07']).toEqual({ status: 'OFFICE', note: '' })

    await saving
    expect(savePresence).toHaveBeenCalledWith('2026-10-07', 'OFFICE', '')
  })

  it('keeps the note when the status changes', async () => {
    const presence = await openWeek([{ date: '2026-10-07', status: 'OFFICE', note: 'Team lunch' }])

    await presence.setStatus('2026-10-07', 'REMOTE')

    expect(savePresence).toHaveBeenCalledWith('2026-10-07', 'REMOTE', 'Team lunch')
  })

  it('clears the day when the same status is picked again', async () => {
    const presence = await openWeek([{ date: '2026-10-07', status: 'OFFICE', note: null }])

    await presence.setStatus('2026-10-07', 'OFFICE')

    expect(presence.entries.value['2026-10-07']).toBeUndefined()
    expect(clearPresence).toHaveBeenCalledWith('2026-10-07')
    expect(savePresence).not.toHaveBeenCalled()
  })

  it('saves a trimmed note, and ignores a note that did not change', async () => {
    const presence = await openWeek([{ date: '2026-10-07', status: 'OFFICE', note: 'Team lunch' }])

    await presence.setNote('2026-10-07', '  Team lunch  ')
    expect(savePresence).not.toHaveBeenCalled()

    await presence.setNote('2026-10-07', '  Offsite  ')
    expect(savePresence).toHaveBeenCalledWith('2026-10-07', 'OFFICE', 'Offsite')
  })

  it('ignores a note on a day that has no status', async () => {
    const presence = await openWeek()

    await presence.setNote('2026-10-07', 'Hello')

    expect(savePresence).not.toHaveBeenCalled()
    expect(presence.entries.value['2026-10-07']).toBeUndefined()
  })

  it('puts the day back to what the server has when saving fails', async () => {
    const presence = await openWeek()
    savePresence.mockRejectedValueOnce(new Error('offline'))
    fetchRange.mockResolvedValueOnce([]) // the re-read of that one day

    await presence.setStatus('2026-10-07', 'OFFICE')

    expect(fetchRange).toHaveBeenLastCalledWith('2026-10-07', '2026-10-07')
    expect(presence.entries.value['2026-10-07']).toBeUndefined()
    expect(presence.error.value).toBe(SAVE_ERROR)
  })

  it('runs saves for the same day in the order they were made', async () => {
    const presence = await openWeek()
    const order = []
    savePresence.mockImplementation(async (date, status) => {
      await settle()
      order.push(`save ${status}`)
    })
    clearPresence.mockImplementation(async () => {
      order.push('clear')
    })

    const first = presence.setStatus('2026-10-07', 'OFFICE')
    const second = presence.setStatus('2026-10-07', 'OFFICE') // same status again: clears
    await Promise.all([first, second])

    expect(order).toEqual(['save OFFICE', 'clear'])
  })

  it('loads the adjacent weeks when navigating', async () => {
    const presence = await openWeek()

    fetchRange.mockResolvedValueOnce([])
    presence.goToNextWeek()
    await settle()
    expect(fetchRange).toHaveBeenLastCalledWith('2026-10-12', '2026-10-18')
    expect(presence.isCurrentWeek.value).toBe(false)

    fetchRange.mockResolvedValueOnce([])
    presence.goToCurrentWeek()
    await settle()
    expect(fetchRange).toHaveBeenLastCalledWith('2026-10-05', '2026-10-11')
    expect(presence.isCurrentWeek.value).toBe(true)
  })

  it('reports a load failure', async () => {
    fetchRange.mockRejectedValueOnce(new Error('offline'))
    const presence = usePresence(SUNDAY_11_OCT_2026)
    await settle()

    expect(presence.error.value).not.toBe('')
    expect(presence.loading.value).toBe(false)
  })

  it('counts statuses, and counts only weekdays as "not set"', async () => {
    const presence = await openWeek([
      { date: '2026-10-05', status: 'OFFICE', note: null },
      { date: '2026-10-06', status: 'OFFICE', note: null },
      { date: '2026-10-07', status: 'REMOTE', note: null },
      { date: '2026-10-10', status: 'AWAY', note: null }, // Saturday
    ])

    expect(presence.summary.value).toEqual({ OFFICE: 2, REMOTE: 1, AWAY: 1, unset: 2 })
  })
})
