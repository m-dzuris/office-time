<script setup>
defineProps({
  isCurrentWeek: { type: Boolean, default: false },
})

defineEmits(['previous', 'next', 'today'])
</script>

<template>
  <nav class="week-nav" aria-label="Choose week">
    <button type="button" class="nav-button icon" aria-label="Previous week" @click="$emit('previous')">
      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path d="M15 5l-7 7 7 7" />
      </svg>
    </button>
    <button type="button" class="nav-button" :disabled="isCurrentWeek" @click="$emit('today')">This week</button>
    <button type="button" class="nav-button icon" aria-label="Next week" @click="$emit('next')">
      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path d="M9 5l7 7-7 7" />
      </svg>
    </button>
  </nav>
</template>

<style scoped>
.week-nav {
  display: flex;
  gap: 8px;
}

.nav-button {
  min-height: 2.75rem;
  padding: 0 16px;
  border: 1.5px solid var(--ink);
  border-radius: 4px;
  background: transparent;
  color: var(--ink);
  font-weight: 600;
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.nav-button:hover:not(:disabled) {
  background: var(--ink);
  color: var(--wall);
}

.nav-button:disabled {
  opacity: 0.45;
}

.icon {
  display: grid;
  place-items: center;
  width: 2.75rem;
  padding: 0;
}

.icon svg {
  width: 1.5rem;
  height: 1.5rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
