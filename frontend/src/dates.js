// All date handling uses local calendar dates. Never go through toISOString(): it converts to UTC and
// shifts the day for anyone east or west of Greenwich.

const MS_PER_DAY = 24 * 60 * 60 * 1000

const WEEKDAY = new Intl.DateTimeFormat('en-GB', { weekday: 'long' })
const WEEKDAY_FULL = new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })
const DAY_MONTH = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'long' })
const MONTH = new Intl.DateTimeFormat('en-GB', { month: 'long' })

/** 2026-10-07 style string for the local calendar date. */
export function toISODate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function addDays(date, days) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days)
}

/** Monday (local midnight) of the week that contains `date`. */
export function startOfWeek(date) {
  const sinceMonday = (date.getDay() + 6) % 7 // Mon = 0 ... Sun = 6
  return addDays(date, -sinceMonday)
}

/** The seven days Monday to Sunday starting at `weekStart`. */
export function weekDays(weekStart, today = new Date()) {
  const todayISO = toISODate(today)
  return Array.from({ length: 7 }, (_, index) => {
    const date = addDays(weekStart, index)
    const iso = toISODate(date)
    return {
      iso,
      date,
      number: date.getDate(),
      weekday: WEEKDAY.format(date),
      fullLabel: WEEKDAY_FULL.format(date),
      isToday: iso === todayISO,
      isWeekend: index >= 5,
    }
  })
}

/** ISO 8601 week number (weeks start on Monday; week 1 contains the year's first Thursday). */
export function isoWeekNumber(date) {
  const thursday = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  thursday.setDate(thursday.getDate() - ((thursday.getDay() + 6) % 7) + 3)

  const firstThursday = new Date(thursday.getFullYear(), 0, 4)
  firstThursday.setDate(firstThursday.getDate() - ((firstThursday.getDay() + 6) % 7) + 3)

  return 1 + Math.round((thursday - firstThursday) / (7 * MS_PER_DAY))
}

/** "5 – 11 October 2026", "28 September – 4 October 2026" or "28 December 2026 – 3 January 2027". */
export function formatWeekRange(weekStart) {
  const start = weekStart
  const end = addDays(weekStart, 6)

  if (start.getFullYear() !== end.getFullYear()) {
    return `${DAY_MONTH.format(start)} ${start.getFullYear()} – ${DAY_MONTH.format(end)} ${end.getFullYear()}`
  }
  if (start.getMonth() !== end.getMonth()) {
    return `${DAY_MONTH.format(start)} – ${DAY_MONTH.format(end)} ${end.getFullYear()}`
  }
  return `${start.getDate()} – ${end.getDate()} ${MONTH.format(end)} ${end.getFullYear()}`
}
