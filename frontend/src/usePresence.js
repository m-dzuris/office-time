import { computed, ref, watch } from 'vue'
import { clearPresence, fetchRange, savePresence } from './api.js'
import { addDays, startOfWeek, weekDays } from './dates.js'

export const LOAD_ERROR = "Couldn't load this week. Check that the server is running, then reload."
export const SAVE_ERROR = "Couldn't save that change. The day is back to what the server has."

function toLocal(entry) {
  return { status: entry.status, note: entry.note ?? '' }
}

function without(object, key) {
  const { [key]: _removed, ...rest } = object
  return rest
}

/**
 * State and actions for the weekly calendar.
 *
 * Changes are applied to the screen immediately and saved in the background. If a save fails, the
 * affected day is re-read from the server so the screen never keeps showing something that isn't stored.
 *
 * @param now supplies the current date; injectable so tests can pin "today".
 */
export function usePresence(now = () => new Date()) {
  const weekStart = ref(startOfWeek(now()))
  const entries = ref({}) // ISO date -> { status, note }; days that are not set are absent
  const loading = ref(false)
  const error = ref('')

  const days = computed(() => weekDays(weekStart.value, now()))
  const isCurrentWeek = computed(() => weekStart.value.getTime() === startOfWeek(now()).getTime())

  const summary = computed(() => {
    const counts = { OFFICE: 0, REMOTE: 0, AWAY: 0, unset: 0 }
    for (const day of days.value) {
      const entry = entries.value[day.iso]
      if (entry) {
        counts[entry.status] += 1
      } else if (!day.isWeekend) {
        counts.unset += 1 // an empty weekend is normal, so only weekdays count as "not set"
      }
    }
    return counts
  })

  // Only the most recent load may write its result, so quickly paging through weeks can't show stale data.
  let latestLoad = 0

  async function load() {
    const loadId = ++latestLoad
    const from = days.value[0].iso
    const to = days.value[6].iso
    loading.value = true
    error.value = ''
    try {
      const list = await fetchRange(from, to)
      if (loadId !== latestLoad) return
      const next = {}
      for (const [iso, entry] of Object.entries(entries.value)) {
        if (iso < from || iso > to) next[iso] = entry // keep other weeks already fetched
      }
      for (const entry of list) {
        next[entry.date] = toLocal(entry)
      }
      entries.value = next
    } catch {
      if (loadId === latestLoad) error.value = LOAD_ERROR
    } finally {
      if (loadId === latestLoad) loading.value = false
    }
  }

  async function refreshDay(iso) {
    try {
      const [entry] = await fetchRange(iso, iso)
      entries.value = entry ? { ...entries.value, [iso]: toLocal(entry) } : without(entries.value, iso)
    } catch {
      // Offline too: the error banner already says the change didn't save.
    }
  }

  // Saves for the same day run one after another so a quick "set, then clear" can't arrive out of order.
  const queues = new Map()
  function enqueue(iso, task) {
    const next = (queues.get(iso) ?? Promise.resolve()).then(task)
    queues.set(iso, next)
    return next
  }

  function failed(iso) {
    return refreshDay(iso).then(() => {
      error.value = SAVE_ERROR
    })
  }

  /** Picks a status for the day. Picking the status the day already has clears the day. */
  function setStatus(iso, status) {
    const before = entries.value[iso]
    const clearing = before?.status === status
    const note = before?.note ?? ''

    error.value = ''
    entries.value = clearing ? without(entries.value, iso) : { ...entries.value, [iso]: { status, note } }

    return enqueue(iso, async () => {
      try {
        if (clearing) {
          await clearPresence(iso)
        } else {
          await savePresence(iso, status, note)
        }
      } catch {
        await failed(iso)
      }
    })
  }

  /** Changes the note on a day that already has a status. */
  function setNote(iso, rawNote) {
    const current = entries.value[iso]
    const note = rawNote.trim()
    if (!current || current.note === note) return Promise.resolve()

    error.value = ''
    entries.value = { ...entries.value, [iso]: { ...current, note } }

    return enqueue(iso, async () => {
      try {
        await savePresence(iso, current.status, note)
      } catch {
        await failed(iso)
      }
    })
  }

  function goToPreviousWeek() {
    weekStart.value = addDays(weekStart.value, -7)
  }

  function goToNextWeek() {
    weekStart.value = addDays(weekStart.value, 7)
  }

  function goToCurrentWeek() {
    weekStart.value = startOfWeek(now())
  }

  function dismissError() {
    error.value = ''
  }

  watch(weekStart, load, { immediate: true })

  return {
    weekStart,
    days,
    entries,
    loading,
    error,
    isCurrentWeek,
    summary,
    setStatus,
    setNote,
    goToPreviousWeek,
    goToNextWeek,
    goToCurrentWeek,
    reload: load,
    dismissError,
  }
}
