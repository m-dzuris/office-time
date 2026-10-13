<script setup>
import { computed, ref } from 'vue'
import {
  deleteGroup,
  describeError,
  inviteToGroup,
  newJoinCode,
  removeMember,
  revokeInvitation,
} from '../api.js'
import { formatJoinCode } from '../groupView.js'

const props = defineProps({
  detail: { type: Object, required: true },
  meId: { type: Number, required: true },
})

const emit = defineEmits(['changed', 'left'])

const inviteEmail = ref('')
const invitedMessage = ref('')
const busy = ref(false)
const error = ref('')
const copied = ref(false)

const isOwner = computed(() => props.detail.role === 'OWNER')
const code = computed(() => formatJoinCode(props.detail.joinCode))

/** Runs one change on the server. Resolves true when it worked, and otherwise shows why it didn't. */
async function run(action, fallback) {
  busy.value = true
  error.value = ''
  try {
    await action()
    return true
  } catch (failure) {
    error.value = describeError(failure, fallback)
    return false
  } finally {
    busy.value = false
  }
}

async function invite() {
  const email = inviteEmail.value.trim()
  if (!email) return
  invitedMessage.value = ''
  if (await run(() => inviteToGroup(props.detail.id, email), "Couldn't send the invitation. Try again.")) {
    invitedMessage.value = `${email} will see the invitation when they sign in.`
    inviteEmail.value = ''
    emit('changed')
  }
}

async function revoke(invitation) {
  if (await run(() => revokeInvitation(props.detail.id, invitation.id), "Couldn't remove the invitation. Try again.")) {
    emit('changed')
  }
}

async function remove(member) {
  if (!window.confirm(`Remove ${member.displayName} from ${props.detail.name}? They will stop seeing this group.`)) return
  if (await run(() => removeMember(props.detail.id, member.userId), "Couldn't remove that person. Try again.")) {
    emit('changed')
  }
}

async function leave() {
  if (!window.confirm(`Leave ${props.detail.name}? You'll stop seeing this group, and it will stop seeing your days.`)) return
  if (await run(() => removeMember(props.detail.id, props.meId), "Couldn't leave the group. Try again.")) {
    emit('left')
  }
}

async function makeNewCode() {
  if (!window.confirm('Make a new join code? The current code will stop working.')) return
  if (await run(() => newJoinCode(props.detail.id), "Couldn't make a new code. Try again.")) {
    emit('changed')
  }
}

async function removeGroup() {
  if (!window.confirm(`Delete ${props.detail.name} for everyone? Nobody's own days are deleted.`)) return
  if (await run(() => deleteGroup(props.detail.id), "Couldn't delete the group. Try again.")) {
    emit('left')
  }
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(code.value)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {
    error.value = "Couldn't copy the code. Select it and copy it by hand."
  }
}
</script>

<template>
  <div class="people">
    <p v-if="error" class="form-error wide" role="alert">{{ error }}</p>

    <section class="panel block" aria-labelledby="people-title">
      <h3 id="people-title" class="title">People</h3>
      <ul class="list">
        <li v-for="member in detail.members" :key="member.userId" class="row">
          <span>
            {{ member.displayName }}
            <span v-if="member.userId === meId" class="muted">(you)</span>
            <span v-if="member.role === 'OWNER'" class="muted">· owner</span>
          </span>
          <button
            v-if="isOwner && member.userId !== meId"
            type="button"
            class="btn btn-small btn-danger"
            :disabled="busy"
            @click="remove(member)"
          >
            Remove
          </button>
        </li>
      </ul>
    </section>

    <section class="panel block" aria-labelledby="invite-title">
      <h3 id="invite-title" class="title">Invite by email</h3>
      <form class="invite-form" @submit.prevent="invite">
        <label class="field">
          <span>Email address</span>
          <input v-model.trim="inviteEmail" class="input" type="email" required maxlength="254" autocomplete="off" />
        </label>
        <button type="submit" class="btn btn-solid" :disabled="busy">Send invitation</button>
      </form>
      <p class="hint">They'll see the invitation when they sign in with this address, even if they sign up later.</p>
      <p v-if="invitedMessage" class="hint" role="status">{{ invitedMessage }}</p>

      <template v-if="detail.invitations.length">
        <h4 class="subtitle">Waiting for a reply</h4>
        <ul class="list">
          <li v-for="invitation in detail.invitations" :key="invitation.id" class="row">
            <span>
              {{ invitation.email }}
              <span class="muted">· invited by {{ invitation.invitedBy }}</span>
            </span>
            <button type="button" class="btn btn-small" :disabled="busy" @click="revoke(invitation)">Withdraw</button>
          </li>
        </ul>
      </template>
    </section>

    <section class="panel block" aria-labelledby="code-title">
      <h3 id="code-title" class="title">Join code</h3>
      <p class="code" aria-label="Join code">{{ code }}</p>
      <p class="hint">Anyone with this code can join and see everyone's days. Share it only with people you trust.</p>
      <div class="actions">
        <button type="button" class="btn btn-small" @click="copyCode">{{ copied ? 'Copied' : 'Copy code' }}</button>
        <button v-if="isOwner" type="button" class="btn btn-small" :disabled="busy" @click="makeNewCode">
          Make a new code
        </button>
      </div>
    </section>

    <section class="panel block" aria-labelledby="leave-title">
      <h3 id="leave-title" class="title">Leave this group</h3>
      <p class="hint">
        You'll stop seeing this group and it will stop seeing you. Your own days stay as they are.
        <template v-if="isOwner">If you're the owner, the person who has been here longest takes over.</template>
      </p>
      <div class="actions">
        <button type="button" class="btn btn-small btn-danger" :disabled="busy" @click="leave">Leave group</button>
        <button v-if="isOwner" type="button" class="btn btn-small btn-danger" :disabled="busy" @click="removeGroup">
          Delete group
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.people {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(19rem, 1fr));
  gap: 6px;
  align-items: start;
}

.wide {
  grid-column: 1 / -1;
}

.block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.5rem;
}

.subtitle {
  margin: 4px 0 0;
  font-weight: 600;
}

.list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 6px 12px;
  padding: 6px 0;
  border-bottom: 1px solid color-mix(in srgb, var(--ink) 15%, transparent);
  overflow-wrap: anywhere;
}

.row:last-child {
  border-bottom: 0;
}

.muted {
  color: var(--ink-soft);
}

.invite-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.code {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 2.25rem;
  letter-spacing: 0.06em;
  line-height: 1;
  user-select: all;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
