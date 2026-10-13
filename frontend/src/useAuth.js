import { ref } from 'vue'
import { describeError, fetchMe, login, logout, register } from './api.js'

export const SIGN_IN_ERROR = "Couldn't sign in. Check your connection and try again."
export const SIGN_UP_ERROR = "Couldn't create the account. Check your connection and try again."

/** Who is signed in. `ready` turns true once the first "who am I" answer is back, so the page doesn't flash. */
export function useAuth() {
  const user = ref(null)
  const ready = ref(false)
  const busy = ref(false)
  const error = ref('')

  async function restore() {
    try {
      user.value = await fetchMe()
    } catch {
      user.value = null
    } finally {
      ready.value = true
    }
  }

  async function submit(action, fallback) {
    busy.value = true
    error.value = ''
    try {
      user.value = await action()
    } catch (failure) {
      error.value = describeError(failure, fallback)
    } finally {
      busy.value = false
    }
  }

  function signIn(email, password) {
    return submit(() => login({ email, password }), SIGN_IN_ERROR)
  }

  function signUp(email, displayName, password) {
    return submit(() => register({ email, displayName, password }), SIGN_UP_ERROR)
  }

  async function signOut() {
    try {
      await logout()
    } catch {
      // Offline: the page still returns to the sign-in screen, and the session ends on the server by itself.
    } finally {
      user.value = null
      error.value = ''
    }
  }

  /** The server says the session has ended. */
  function expire() {
    user.value = null
  }

  function clearError() {
    error.value = ''
  }

  return { user, ready, busy, error, restore, signIn, signUp, signOut, expire, clearError }
}
