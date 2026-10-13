<script setup>
import { computed } from 'vue'
import DayColumn from './components/DayColumn.vue'
import WeekNav from './components/WeekNav.vue'
import WeekSummary from './components/WeekSummary.vue'
import { formatWeekRange, isoWeekNumber } from './dates.js'
import { usePresence } from './usePresence.js'

const {
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
  reload,
  dismissError,
} = usePresence()

const weekRange = computed(() => formatWeekRange(weekStart.value))
const weekNumber = computed(() => isoWeekNumber(weekStart.value))
</script>

<template>
  <div class="app">
    <header class="masthead">
      <h1 class="brand">Office time</h1>

      <div class="title-row">
        <div>
          <h2 class="week-title">{{ weekRange }}</h2>
          <p class="week-number">Week {{ weekNumber }}</p>
        </div>
        <WeekNav
          :is-current-week="isCurrentWeek"
          @previous="goToPreviousWeek"
          @next="goToNextWeek"
          @today="goToCurrentWeek"
        />
      </div>

      <WeekSummary class="summary" :summary="summary" />
    </header>

    <div v-if="error" class="notice" role="alert">
      <span>{{ error }}</span>
      <span class="notice-actions">
        <button type="button" class="notice-button" @click="reload">Reload week</button>
        <button type="button" class="notice-button" @click="dismissError">Dismiss</button>
      </span>
    </div>

    <main>
      <div class="board" :aria-busy="loading ? 'true' : 'false'">
        <DayColumn
          v-for="day in days"
          :key="day.iso"
          :day="day"
          :entry="entries[day.iso]"
          @set-status="setStatus(day.iso, $event)"
          @set-note="setNote(day.iso, $event)"
        />
      </div>
    </main>
  </div>
</template>

<style scoped>
.app {
  max-width: 1240px;
  margin: 0 auto;
  padding: 20px 24px 56px;
}

.brand {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.5rem;
  letter-spacing: 0.01em;
}

.title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.week-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: clamp(2.5rem, 6vw, 4.25rem);
  line-height: 0.95;
}

.week-number {
  margin: 6px 0 0;
  color: var(--ink-soft);
  font-weight: 500;
}

.summary {
  margin: 18px 0 20px;
}

.notice {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
  margin-bottom: 16px;
  padding: 10px 14px;
  border: 1.5px solid var(--danger);
  border-radius: 4px;
  background: var(--danger-wash);
  color: var(--danger);
  font-weight: 500;
}

.notice-actions {
  display: flex;
  gap: 16px;
}

.notice-button {
  padding: 4px 0;
  border: 0;
  background: none;
  color: inherit;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.notice-button:focus-visible {
  outline-color: var(--danger);
}

.board {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr)) repeat(2, minmax(0, 0.8fr));
  gap: 6px;
  transition: opacity 0.15s ease;
}

.board[aria-busy='true'] {
  opacity: 0.6;
}

@media (max-width: 900px) {
  .board {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 520px) {
  .app {
    padding: 16px 16px 40px;
  }
}
</style>
