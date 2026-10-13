<script setup>
defineProps({
  invitations: { type: Array, required: true },
})

defineEmits(['accept', 'decline'])
</script>

<template>
  <section class="invites" aria-label="Invitations to groups">
    <ul class="list">
      <li v-for="invitation in invitations" :key="invitation.id" class="invite">
        <span>
          <strong>{{ invitation.invitedBy }}</strong> invited you to <strong>{{ invitation.groupName }}</strong>.
        </span>
        <span class="actions">
          <button type="button" class="btn btn-small btn-solid" @click="$emit('accept', invitation.id)">Accept</button>
          <button type="button" class="btn btn-small" @click="$emit('decline', invitation.id)">Decline</button>
        </span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.invites {
  margin-bottom: 16px;
  border-left: 6px solid var(--office);
  border-radius: 6px;
  background: var(--board);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  padding: 8px 14px;
  list-style: none;
}

.invite {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
  padding: 4px 0;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>
