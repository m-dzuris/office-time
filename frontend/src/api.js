export class ApiError extends Error {
  constructor(status, serverMessage = '') {
    super(serverMessage || `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.serverMessage = serverMessage
  }
}

let onUnauthorized = () => {}

/** Called when a request other than sign-in/sign-up comes back 401, i.e. the session has ended. */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

/**
 * The server's own message when it is a 4xx written for people ("Wrong email or password."), otherwise
 * the fallback. 5xx and network failures never show raw server text.
 */
export function describeError(error, fallback) {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500 && error.serverMessage) {
    return error.serverMessage
  }
  return fallback
}

// The server sets an XSRF-TOKEN cookie; changing requests must send it back in a header.
function csrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : ''
}

/**
 * Signing out makes the server delete the token cookie, and the sign-in form that follows needs one. Any
 * GET to the API hands out a fresh cookie, so ask for one when it is missing.
 */
async function ensureCsrfToken() {
  if (csrfToken()) return
  try {
    await fetch('/api/auth/me', { credentials: 'same-origin' })
  } catch {
    // Offline: the request that follows fails on its own and reports it.
  }
}

async function request(path, { method = 'GET', body } = {}) {
  const headers = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    await ensureCsrfToken()
    const token = csrfToken()
    if (token) headers['X-XSRF-TOKEN'] = token
  }

  const response = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    let serverMessage = ''
    try {
      serverMessage = (await response.json()).message ?? ''
    } catch {
      // no JSON body
    }
    // A 401 from the sign-in form just means wrong credentials; anywhere else it means the session ended.
    if (response.status === 401 && !path.startsWith('/api/auth/')) {
      onUnauthorized()
    }
    throw new ApiError(response.status, serverMessage)
  }
  return response.status === 204 ? null : response.json()
}

// ---- accounts ---------------------------------------------------------------------------------------------

/** The signed-in user, or null when nobody is signed in. */
export const fetchMe = () => request('/api/auth/me')
export const register = ({ email, displayName, password }) =>
  request('/api/auth/register', { method: 'POST', body: { email, displayName, password } })
export const login = ({ email, password }) =>
  request('/api/auth/login', { method: 'POST', body: { email, password } })
export const logout = () => request('/api/auth/logout', { method: 'POST' })

// ---- my own days ------------------------------------------------------------------------------------------

/** Entries between two ISO dates, both inclusive. Days without an entry are simply absent. */
export const fetchRange = (from, to) => request(`/api/presence?from=${from}&to=${to}`)

export const savePresence = (date, status, note) =>
  request(`/api/presence/${date}`, { method: 'PUT', body: { status, note: note || null } })

export const clearPresence = (date) => request(`/api/presence/${date}`, { method: 'DELETE' })

// ---- groups -----------------------------------------------------------------------------------------------

export const listGroups = () => request('/api/groups')
export const createGroup = (name) => request('/api/groups', { method: 'POST', body: { name } })
export const joinGroup = (code) => request('/api/groups/join', { method: 'POST', body: { code } })
export const fetchGroup = (id) => request(`/api/groups/${id}`)
export const deleteGroup = (id) => request(`/api/groups/${id}`, { method: 'DELETE' })
export const newJoinCode = (id) => request(`/api/groups/${id}/join-code`, { method: 'POST' })
export const inviteToGroup = (id, email) =>
  request(`/api/groups/${id}/invitations`, { method: 'POST', body: { email } })
export const revokeInvitation = (groupId, invitationId) =>
  request(`/api/groups/${groupId}/invitations/${invitationId}`, { method: 'DELETE' })
export const removeMember = (groupId, userId) =>
  request(`/api/groups/${groupId}/members/${userId}`, { method: 'DELETE' })

/** Everyone's statuses in a group between two ISO dates. Statuses only, never notes. */
export const fetchGroupPresence = (id, from, to) => request(`/api/groups/${id}/presence?from=${from}&to=${to}`)

// ---- invitations to me ------------------------------------------------------------------------------------

export const listInvitations = () => request('/api/invitations')
export const acceptInvitation = (id) => request(`/api/invitations/${id}/accept`, { method: 'POST' })
export const declineInvitation = (id) => request(`/api/invitations/${id}`, { method: 'DELETE' })
