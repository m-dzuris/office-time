// Pure helpers for the group calendar, kept free of Vue so they are easy to test.

/** Turns the server's flat list into { userId: { 'YYYY-MM-DD': 'OFFICE' | 'REMOTE' | 'AWAY' } }. */
export function indexEntries(list) {
  const index = {}
  for (const entry of list) {
    ;(index[entry.userId] ??= {})[entry.date] = entry.status
  }
  return index
}

/** A person's status on a day, or null when they haven't set one. */
export function statusFor(index, userId, iso) {
  return index[userId]?.[iso] ?? null
}

/** How many of these people are in the office on the day. */
export function officeCount(index, userIds, iso) {
  return userIds.filter((userId) => index[userId]?.[iso] === 'OFFICE').length
}

/** You first, then everyone else alphabetically. */
export function groupRows(members, meId) {
  return [...members].sort((a, b) => {
    if (a.userId === meId) return -1
    if (b.userId === meId) return 1
    return a.displayName.localeCompare(b.displayName, undefined, { sensitivity: 'base' })
  })
}

/** "ABCDEFGHJKLM" becomes "ABCD-EFGH-JKLM" so it is easier to read out and type. */
export function formatJoinCode(code) {
  return code.replace(/(.{4})(?=.)/g, '$1-')
}
