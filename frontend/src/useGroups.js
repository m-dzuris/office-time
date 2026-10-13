import { ref } from 'vue'
import { acceptInvitation, declineInvitation, describeError, listGroups, listInvitations } from './api.js'

export const LOAD_GROUPS_ERROR = "Couldn't load your groups and invitations."

/** The signed-in user's groups and the invitations waiting for them. */
export function useGroups() {
  const groups = ref([])
  const invitations = ref([])
  const notice = ref('')

  async function refresh() {
    try {
      const [mine, waiting] = await Promise.all([listGroups(), listInvitations()])
      groups.value = mine
      invitations.value = waiting
    } catch {
      notice.value = LOAD_GROUPS_ERROR
    }
  }

  /** Returns the group that was joined, or null when it didn't work. */
  async function accept(invitationId) {
    try {
      const group = await acceptInvitation(invitationId)
      await refresh()
      return group
    } catch (failure) {
      await refresh()
      notice.value = describeError(failure, "Couldn't accept the invitation. Try again.")
      return null
    }
  }

  async function decline(invitationId) {
    try {
      await declineInvitation(invitationId)
      await refresh()
    } catch (failure) {
      await refresh()
      notice.value = describeError(failure, "Couldn't decline the invitation. Try again.")
    }
  }

  function dismissNotice() {
    notice.value = ''
  }

  return { groups, invitations, notice, refresh, accept, decline, dismissNotice }
}
