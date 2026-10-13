<script setup>
import { ref } from 'vue'
import { createGroup, describeError, joinGroup } from '../api.js'

const emit = defineEmits(['opened'])

const name = ref('')
const code = ref('')
const busy = ref(false)
const createError = ref('')
const joinError = ref('')

async function create() {
  busy.value = true
  createError.value = ''
  try {
    const group = await createGroup(name.value)
    name.value = ''
    emit('opened', group.id)
  } catch (failure) {
    createError.value = describeError(failure, "Couldn't create the group. Try again.")
  } finally {
    busy.value = false
  }
}

async function join() {
  busy.value = true
  joinError.value = ''
  try {
    const group = await joinGroup(code.value)
    code.value = ''
    emit('opened', group.id)
  } catch (failure) {
    joinError.value = describeError(failure, "Couldn't join the group. Try again.")
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="start">
    <form class="panel form" @submit.prevent="create">
      <h3 class="title">Create a group</h3>
      <p class="hint">
        Members of a group can see each other's days: in office, remote or away. Your days stay yours; a group
        only decides who can see them.
      </p>
      <label class="field">
        <span>Group name</span>
        <input v-model.trim="name" class="input" type="text" required maxlength="80" autocomplete="off" />
      </label>
      <p v-if="createError" class="form-error" role="alert">{{ createError }}</p>
      <button type="submit" class="btn btn-solid" :disabled="busy">Create group</button>
    </form>

    <form class="panel form" @submit.prevent="join">
      <h3 class="title">Join with a code</h3>
      <p class="hint">Ask someone in the group for its join code. Invitations sent to your email appear at the top of the page.</p>
      <label class="field">
        <span>Join code</span>
        <input
          v-model.trim="code"
          class="input code"
          type="text"
          required
          maxlength="64"
          autocomplete="off"
          autocapitalize="characters"
          spellcheck="false"
        />
      </label>
      <p v-if="joinError" class="form-error" role="alert">{{ joinError }}</p>
      <button type="submit" class="btn btn-solid" :disabled="busy">Join group</button>
    </form>
  </div>
</template>

<style scoped>
.start {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
  gap: 6px;
  align-items: start;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.75rem;
}

.code {
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
</style>
