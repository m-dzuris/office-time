import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { ApiError, describeError, fetchRange, login, savePresence, setUnauthorizedHandler } from './api.js'

// api.js reads document.cookie and calls fetch, so both are replaced with small fakes for each test.
let cookie
let calls
let respond

function reply(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => {
      if (body === undefined) throw new Error('no body')
      return body
    },
  }
}

beforeEach(() => {
  cookie = ''
  calls = []
  respond = () => reply(204)
  globalThis.document = {
    get cookie() {
      return cookie
    },
  }
  globalThis.fetch = async (path, options = {}) => {
    calls.push({ path, method: options.method ?? 'GET', headers: options.headers ?? {} })
    return respond(path, calls.length)
  }
  setUnauthorizedHandler(() => {})
})

afterEach(() => {
  delete globalThis.document
  delete globalThis.fetch
})

describe('CSRF token', () => {
  it('is sent back from the cookie on changing requests', async () => {
    cookie = 'theme=dark; XSRF-TOKEN=abc%3D%3D'

    await savePresence('2026-10-07', 'OFFICE', '')

    expect(calls).toHaveLength(1)
    expect(calls[0].headers['X-XSRF-TOKEN']).toBe('abc==')
  })

  it('is fetched first when the cookie is missing, as it is right after signing out', async () => {
    respond = (path) => {
      if (path === '/api/auth/me') cookie = 'XSRF-TOKEN=fresh'
      return reply(204)
    }

    await login({ email: 'ada@example.com', password: 'correct horse battery' })

    expect(calls.map((call) => `${call.method} ${call.path}`)).toEqual([
      'GET /api/auth/me',
      'POST /api/auth/login',
    ])
    expect(calls[1].headers['X-XSRF-TOKEN']).toBe('fresh')
  })

  it('is not fetched for reads', async () => {
    respond = () => reply(200, [])

    await fetchRange('2026-10-05', '2026-10-11')

    expect(calls).toHaveLength(1)
    expect(calls[0].method).toBe('GET')
  })
})

describe('expired sessions', () => {
  it('are reported when the API answers 401', async () => {
    let expired = 0
    setUnauthorizedHandler(() => {
      expired += 1
    })
    cookie = 'XSRF-TOKEN=t'
    respond = () => reply(401)

    await expect(savePresence('2026-10-07', 'OFFICE', '')).rejects.toBeInstanceOf(ApiError)

    expect(expired).toBe(1)
  })

  it('are not reported for a failed sign-in, which is just a wrong password', async () => {
    let expired = 0
    setUnauthorizedHandler(() => {
      expired += 1
    })
    cookie = 'XSRF-TOKEN=t'
    respond = () => reply(401, { message: 'Wrong email or password.' })

    await expect(login({ email: 'ada@example.com', password: 'nope' })).rejects.toBeInstanceOf(ApiError)

    expect(expired).toBe(0)
  })
})

describe('describeError', () => {
  it("shows the server's own message for a 4xx", () => {
    expect(describeError(new ApiError(401, 'Wrong email or password.'), 'fallback')).toBe('Wrong email or password.')
  })

  it('never shows server text for a 5xx, or anything that is not an API error', () => {
    expect(describeError(new ApiError(500, 'NullPointerException at ...'), 'fallback')).toBe('fallback')
    expect(describeError(new TypeError('Failed to fetch'), 'fallback')).toBe('fallback')
  })

  it('falls back when a 4xx has no message', () => {
    expect(describeError(new ApiError(403), 'fallback')).toBe('fallback')
  })
})
