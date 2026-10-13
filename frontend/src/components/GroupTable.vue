<script setup>
import { computed } from 'vue'
import { STATUSES } from '../statuses.js'
import { groupRows, officeCount, statusFor } from '../groupView.js'

const props = defineProps({
  members: { type: Array, required: true },
  index: { type: Object, required: true },
  days: { type: Array, required: true },
  meId: { type: Number, required: true },
})

const labels = Object.fromEntries(STATUSES.map((status) => [status.value, status.label]))

const rows = computed(() => groupRows(props.members, props.meId))
const memberIds = computed(() => props.members.map((member) => member.userId))

function status(userId, iso) {
  return statusFor(props.index, userId, iso)
}

function cellClass(userId, day) {
  const value = status(userId, day.iso)
  return [value ? `is-${value.toLowerCase()}` : 'is-unset', { weekend: day.isWeekend }]
}
</script>

<template>
  <div class="scroller">
    <table class="grid">
      <caption class="visually-hidden">
        Where each person in the group plans to be this week
      </caption>
      <thead>
        <tr>
          <th scope="col"><span class="visually-hidden">Person</span></th>
          <th
            v-for="day in days"
            :key="day.iso"
            scope="col"
            class="day"
            :class="{ today: day.isToday }"
            :aria-current="day.isToday ? 'date' : undefined"
          >
            <span class="day-number">{{ day.number }}</span>
            <span class="day-name">{{ day.weekday }}</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="member in rows" :key="member.userId">
          <th scope="row" class="person">
            {{ member.displayName }}<span v-if="member.userId === meId" class="you"> (you)</span>
          </th>
          <td v-for="day in days" :key="day.iso" class="cell" :class="cellClass(member.userId, day)">
            <template v-if="status(member.userId, day.iso)">{{ labels[status(member.userId, day.iso)] }}</template>
            <template v-else>
              <span aria-hidden="true">–</span>
              <span class="visually-hidden">Not set</span>
            </template>
          </td>
        </tr>
      </tbody>
      <tfoot>
        <tr>
          <th scope="row" class="person">In office</th>
          <td v-for="day in days" :key="day.iso" class="count">{{ officeCount(index, memberIds, day.iso) }}</td>
        </tr>
      </tfoot>
    </table>
  </div>
</template>

<style scoped>
.scroller {
  overflow-x: auto;
}

.grid {
  width: 100%;
  min-width: 46rem;
  border-collapse: separate;
  border-spacing: 4px;
  table-layout: fixed;
}

.grid th:first-child {
  width: 10rem;
}

.day {
  padding: 8px 6px 6px;
  border-top: 6px solid transparent;
  text-align: left;
  vertical-align: bottom;
}

.day.today {
  border-top-color: var(--ink);
}

.day-number {
  display: block;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 2.25rem;
  line-height: 0.9;
}

.day-name {
  display: block;
  margin-top: 4px;
  font-weight: 600;
}

.person {
  position: sticky;
  left: 0;
  z-index: 1;
  padding: 0 12px 0 0;
  background: var(--wall);
  font-weight: 600;
  text-align: left;
  overflow-wrap: anywhere;
}

.you {
  color: var(--ink-soft);
  font-weight: 500;
}

.cell {
  --fill: var(--board);
  --on-fill: var(--ink);

  height: 3.25rem;
  padding: 0 6px;
  border-radius: 4px;
  background: var(--fill);
  color: var(--on-fill);
  font-size: 0.9375rem;
  font-weight: 600;
  text-align: center;
}

.cell.weekend.is-unset {
  --fill: var(--board-weekend);
}

.cell.is-office {
  --fill: var(--office);
  --on-fill: #fff;
}

.cell.is-remote {
  --fill: var(--remote);
  --on-fill: var(--ink);
}

.cell.is-away {
  --fill: var(--away);
  --on-fill: #fff;
}

.count {
  padding: 4px 6px 0;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.75rem;
  line-height: 1;
}
</style>
