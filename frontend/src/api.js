export class ApiError extends Error {
  constructor(status) {
    super(`Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, options) {
  const response = await fetch(path, options)
  if (!response.ok) {
    throw new ApiError(response.status)
  }
  return response.status === 204 ? null : response.json()
}

/** Entries between two ISO dates, both inclusive. Days without an entry are simply absent. */
export function fetchRange(from, to) {
  return request(`/api/presence?from=${from}&to=${to}`)
}

export function savePresence(date, status, note) {
  return request(`/api/presence/${date}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status, note: note || null }),
  })
}

export function clearPresence(date) {
  return request(`/api/presence/${date}`, { method: 'DELETE' })
}
