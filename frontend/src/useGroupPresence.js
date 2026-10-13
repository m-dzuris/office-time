import { ref, watch } from 'vue'
import { fetchGroup, fetchGroupPresence } from './api.js'
import { indexEntries } from './groupView.js'

export const LOAD_GROUP_ERROR = "Couldn't load this group. Check that the server is running, then reload."

/**
 * One group's details and everyone's statuses for the shown week.
 *
 * @param groupId a ref holding the group id
 * @param days a ref holding the seven days of the shown week (as produced by weekDays)
 */
export function useGroupPresence(groupId, days) {
  const detail = ref(null)
  const index = ref({}) // { userId: { isoDate: status } }
  const loading = ref(false)
  const error = ref('')

  // Only the most recent load may write its result, so paging through weeks quickly can't show stale data.
  let latestLoad = 0

  async function load() {
    const loadId = ++latestLoad
    const id = groupId.value
    const from = days.value[0].iso
    const to = days.value[6].iso
    loading.value = true
    error.value = ''
    try {
      const [group, entries] = await Promise.all([fetchGroup(id), fetchGroupPresence(id, from, to)])
      if (loadId !== latestLoad) return
      detail.value = group
      index.value = indexEntries(entries)
    } catch {
      if (loadId === latestLoad) error.value = LOAD_GROUP_ERROR
    } finally {
      if (loadId === latestLoad) loading.value = false
    }
  }

  watch([groupId, () => days.value[0].iso], load, { immediate: true })

  return { detail, index, loading, error, reload: load }
}
