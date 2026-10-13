<script setup>
import { toRef } from 'vue'
import GroupPeople from './GroupPeople.vue'
import GroupTable from './GroupTable.vue'
import { useGroupPresence } from '../useGroupPresence.js'

const props = defineProps({
  groupId: { type: Number, required: true },
  days: { type: Array, required: true },
  meId: { type: Number, required: true },
})

const emit = defineEmits(['changed', 'left'])

const { detail, index, loading, error, reload } = useGroupPresence(toRef(props, 'groupId'), toRef(props, 'days'))

function onChanged() {
  reload()
  emit('changed')
}
</script>

<template>
  <section class="group" :aria-busy="loading ? 'true' : 'false'">
    <p v-if="error" class="notice" role="alert">
      <span>{{ error }}</span>
      <span class="notice-actions">
        <button type="button" class="link" @click="reload">Reload</button>
      </span>
    </p>

    <template v-if="detail">
      <h3 class="group-name">{{ detail.name }}</h3>
      <p class="note">
        You see everyone's status here. To change yours, use the My week view. Notes are never shared.
      </p>
      <GroupTable :members="detail.members" :index="index" :days="days" :me-id="meId" />
      <GroupPeople :detail="detail" :me-id="meId" @changed="onChanged" @left="emit('left')" />
    </template>
  </section>
</template>

<style scoped>
.group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group[aria-busy='true'] {
  opacity: 0.7;
}

.group-name {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 2rem;
  line-height: 1;
  overflow-wrap: anywhere;
}

.note {
  margin: -8px 0 0;
  color: var(--ink-soft);
}
</style>
