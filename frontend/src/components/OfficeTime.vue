<script setup>
import { computed, ref, watch } from 'vue'
import DayColumn from './DayColumn.vue'
import GroupStart from './GroupStart.vue'
import GroupView from './GroupView.vue'
import InvitationBanner from './InvitationBanner.vue'
import WeekNav from './WeekNav.vue'
import WeekSummary from './WeekSummary.vue'
import { formatWeekRange, isoWeekNumber } from '../dates.js'
import { useGroups } from '../useGroups.js'
import { usePresence } from '../usePresence.js'

defineProps({
  user: { type: Object, required: true },
})

defineEmits(['sign-out'])

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

const { groups, invitations, notice, refresh, accept, decline, dismissNotice } = useGroups()

// Which part of the app is showing: 'me' (my week), 'new' (create or join a group), or a group's id.
const view = ref('me')
const openGroupId = computed(() => (typeof view.value === 'number' ? view.value : null))

const weekRange = computed(() => formatWeekRange(weekStart.value))
const weekNumber = computed(() => isoWeekNumber(weekStart.value))

refresh()

// If the open group disappears (left, deleted, or removed by its owner), go back to My week.
watch(groups, (list) => {
  if (openGroupId.value !== null && !list.some((group) => group.id === openGroupId.value)) {
    view.value = 'me'
  }
})

async function openGroup(id) {
  await refresh() // so the new group is in the list before it is selected
  view.value = id
}

async function acceptInvitation(id) {
  const group = await accept(id)
  if (group) view.value = group.id
}

async function onLeft() {
  view.value = 'me'
  await refresh()
}
</script>

<template>
  <div class="app">
    <header class="masthead">
      <div class="bar">
        <h1 class="brand">Office time</h1>
        <div class="account">
          <span class="who">{{ user.displayName }}</span>
          <button type="button" class="link" @click="$emit('sign-out')">Sign out</button>
        </div>
      </div>

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
    </header>

    <InvitationBanner
      v-if="invitations.length"
      :invitations="invitations"
      @accept="acceptInvitation"
      @decline="decline"
    />

    <p v-if="notice" class="notice" role="alert">
      <span>{{ notice }}</span>
      <span class="notice-actions">
        <button type="button" class="link" @click="dismissNotice">Dismiss</button>
      </span>
    </p>

    <nav class="views" aria-label="Views">
      <button type="button" class="view" :aria-current="view === 'me' ? 'page' : undefined" @click="view = 'me'">
        My week
      </button>
      <button
        v-for="group in groups"
        :key="group.id"
        type="button"
        class="view"
        :aria-current="openGroupId === group.id ? 'page' : undefined"
        @click="openGroup(group.id)"
      >
        {{ group.name }}
      </button>
      <button type="button" class="view" :aria-current="view === 'new' ? 'page' : undefined" @click="view = 'new'">
        Add a group
      </button>
    </nav>

    <main>
      <template v-if="view === 'me'">
        <WeekSummary class="summary" :summary="summary" />

        <p v-if="error" class="notice" role="alert">
          <span>{{ error }}</span>
          <span class="notice-actions">
            <button type="button" class="link" @click="reload">Reload week</button>
            <button type="button" class="link" @click="dismissError">Dismiss</button>
          </span>
        </p>

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
      </template>

      <GroupView
        v-else-if="openGroupId !== null"
        :key="openGroupId"
        :group-id="openGroupId"
        :days="days"
        :me-id="user.id"
        @changed="refresh"
        @left="onLeft"
      />

      <GroupStart v-else @opened="openGroup" />
    </main>
  </div>
</template>

<style scoped>
.app {
  max-width: 1240px;
  margin: 0 auto;
  padding: 20px 24px 56px;
}

.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 4px 16px;
}

.brand {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.5rem;
  letter-spacing: 0.01em;
}

.account {
  display: flex;
  align-items: baseline;
  gap: 14px;
}

.who {
  font-weight: 600;
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

.views {
  display: flex;
  gap: 4px;
  margin: 20px 0 18px;
  overflow-x: auto;
  border-bottom: 1.5px solid color-mix(in srgb, var(--ink) 25%, transparent);
}

.view {
  flex: none;
  max-width: 15rem;
  min-height: 2.75rem;
  margin-bottom: -1.5px;
  padding: 0 14px;
  border: 0;
  border-bottom: 4px solid transparent;
  background: none;
  color: var(--ink-soft);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: background-color 0.18s ease;
}

.view:hover {
  background: color-mix(in srgb, var(--ink) 8%, transparent);
}

.view[aria-current='page'] {
  border-bottom-color: var(--ink);
  color: var(--ink);
}

.view:focus-visible {
  outline-offset: -3px;
}

.summary {
  margin: 0 0 20px;
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
