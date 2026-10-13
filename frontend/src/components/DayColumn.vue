<script setup>
import { computed } from 'vue'
import { STATUSES } from '../statuses.js'

const props = defineProps({
  day: { type: Object, required: true },
  entry: { type: Object, default: undefined },
})

const emit = defineEmits(['set-status', 'set-note'])

const statusClass = computed(() => (props.entry ? `is-${props.entry.status.toLowerCase()}` : 'is-unset'))

function commitNote(event) {
  const input = event.target
  input.value = input.value.trim()
  emit('set-note', input.value)
}
</script>

<template>
  <section
    class="day"
    :class="[statusClass, { today: day.isToday, weekend: day.isWeekend }]"
    :aria-label="day.fullLabel"
    :aria-current="day.isToday ? 'date' : undefined"
  >
    <div class="head">
      <span class="number">{{ day.number }}</span>
      <span class="meta">
        <span class="name">{{ day.weekday }}</span>
        <span v-if="day.isToday" class="today-chip">Today</span>
      </span>
    </div>

    <div class="options" role="group" :aria-label="`Where you'll be on ${day.fullLabel}`">
      <button
        v-for="status in STATUSES"
        :key="status.value"
        type="button"
        class="option"
        :class="`option-${status.value.toLowerCase()}`"
        :aria-pressed="entry?.status === status.value ? 'true' : 'false'"
        @click="emit('set-status', status.value)"
      >
        <span class="swatch" aria-hidden="true"></span>
        {{ status.label }}
      </button>
    </div>

    <label v-if="entry" class="note">
      <span class="visually-hidden">Note for {{ day.fullLabel }}</span>
      <input
        class="note-input"
        type="text"
        maxlength="200"
        placeholder="Add a note"
        autocomplete="off"
        :value="entry.note"
        @change="commitNote"
      />
    </label>
  </section>
</template>

<style scoped>
.day {
  --fill: var(--board);
  --on-fill: var(--ink);

  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 25rem;
  padding: 12px 12px 14px;
  border-top: 6px solid transparent;
  border-radius: 6px;
  background: var(--fill);
  color: var(--on-fill);
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.day.weekend.is-unset {
  --fill: var(--board-weekend);
}

.day.is-office {
  --fill: var(--office);
  --on-fill: #fff;
}

.day.is-remote {
  --fill: var(--remote);
  --on-fill: var(--ink);
}

.day.is-away {
  --fill: var(--away);
  --on-fill: #fff;
}

.day.today {
  border-top-color: var(--ink);
}

.number {
  display: block;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: clamp(3.75rem, 6.2vw, 5.75rem);
  line-height: 0.85;
  letter-spacing: -0.01em;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
  margin-top: 8px;
}

.name {
  font-weight: 600;
}

.today-chip {
  padding: 0 8px;
  border-radius: 3px;
  background: var(--ink);
  color: #fff;
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1.5;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: auto;
}

.option {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 2.75rem;
  padding: 0 12px;
  border: 1.5px solid color-mix(in srgb, var(--on-fill) 38%, transparent);
  border-radius: 4px;
  background: transparent;
  color: var(--on-fill);
  font-weight: 600;
  text-align: left;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    border-color 0.18s ease;
}

.option:hover {
  background: color-mix(in srgb, var(--on-fill) 14%, transparent);
}

.day.is-unset .option:hover {
  background: color-mix(in srgb, var(--swatch) 22%, transparent);
}

.option[aria-pressed='true'] {
  border-color: var(--on-fill);
  background: var(--on-fill);
  color: var(--fill);
}

.option:focus-visible {
  outline-color: var(--on-fill);
}

.swatch {
  flex: none;
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 2px;
  background: var(--swatch);
  outline: 1.5px solid color-mix(in srgb, var(--on-fill) 55%, transparent);
}

.option-office {
  --swatch: var(--office);
}

.option-remote {
  --swatch: var(--remote);
}

.option-away {
  --swatch: var(--away);
}

.note {
  display: block;
}

.note-input {
  width: 100%;
  min-height: 2.5rem;
  padding: 0 2px;
  border: 0;
  border-bottom: 1.5px solid color-mix(in srgb, var(--on-fill) 45%, transparent);
  border-radius: 0;
  background: transparent;
  color: var(--on-fill);
  font-size: 0.9375rem;
}

.note-input::placeholder {
  color: color-mix(in srgb, var(--on-fill) 75%, transparent);
  opacity: 1;
}

.note-input:focus-visible {
  outline-color: var(--on-fill);
}

/* Narrow screens: each day becomes a row, with the date on the left and the choices beside it. */
@media (max-width: 900px) {
  .day {
    display: grid;
    grid-template-columns: 6.5rem minmax(0, 1fr);
    align-items: start;
    gap: 8px 14px;
    min-height: 0;
    padding: 12px 12px 12px 10px;
    border-top: 0;
    border-left: 6px solid transparent;
  }

  .day.today {
    border-left-color: var(--ink);
  }

  .head {
    grid-row: 1 / span 2;
  }

  .number {
    font-size: 3.25rem;
  }

  .options {
    grid-column: 2;
    flex-direction: row;
    flex-wrap: wrap;
    margin-top: 0;
  }

  .option {
    flex: 1 1 6.5rem;
  }

  .note {
    grid-column: 2;
  }
}

@media (max-width: 520px) {
  .day {
    grid-template-columns: minmax(0, 1fr);
  }

  .head {
    grid-row: auto;
    display: flex;
    align-items: baseline;
    gap: 12px;
  }

  .meta {
    margin-top: 0;
  }

  .options,
  .note {
    grid-column: 1;
  }

  .options {
    flex-wrap: nowrap;
  }

  .option {
    flex: 1 1 0;
    justify-content: center;
    gap: 6px;
    padding: 0 6px;
  }
}
</style>
