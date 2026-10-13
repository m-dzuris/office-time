<script setup>
import { computed } from 'vue'
import { STATUSES } from '../statuses.js'

const props = defineProps({
  summary: { type: Object, required: true },
})

const items = computed(() => [
  ...STATUSES.map((status) => ({
    key: status.value,
    label: status.label.toLowerCase(),
    count: props.summary[status.value],
  })),
  { key: 'unset', label: 'not set', count: props.summary.unset },
])
</script>

<template>
  <ul class="summary-list" aria-label="This week at a glance">
    <li v-for="item in items" :key="item.key" class="item">
      <span class="swatch" :class="`swatch-${item.key.toLowerCase()}`" aria-hidden="true"></span>
      <span class="count">{{ item.count }}</span>
      <span>{{ item.label }}</span>
    </li>
  </ul>
</template>

<style scoped>
.summary-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 22px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.count {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.swatch {
  width: 0.875rem;
  height: 0.875rem;
  border-radius: 2px;
}

.swatch-office {
  background: var(--office);
}

.swatch-remote {
  background: var(--remote);
}

.swatch-away {
  background: var(--away);
}

.swatch-unset {
  border: 1.5px solid var(--ink-soft);
}
</style>
