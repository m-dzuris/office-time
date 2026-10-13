<script setup>
import { computed, ref } from 'vue'

defineProps({
  busy: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['sign-in', 'sign-up', 'switch'])

const isSignUp = ref(false)
const displayName = ref('')
const email = ref('')
const password = ref('')

const title = computed(() => (isSignUp.value ? 'Create your account' : 'Sign in'))

function submit() {
  if (isSignUp.value) {
    emit('sign-up', email.value, displayName.value, password.value)
  } else {
    emit('sign-in', email.value, password.value)
  }
}

function toggleMode() {
  isSignUp.value = !isSignUp.value
  password.value = ''
  emit('switch') // lets the parent clear a leftover error
}
</script>

<template>
  <main class="auth">
    <h1 class="brand">Office time</h1>
    <p class="lead">Say where you'll be each day, and see where your team will be.</p>

    <form class="panel form" @submit.prevent="submit">
      <h2 class="form-title">{{ title }}</h2>

      <label v-if="isSignUp" class="field">
        <span>Your name</span>
        <input v-model.trim="displayName" class="input" type="text" required maxlength="60" autocomplete="name" />
      </label>

      <label class="field">
        <span>Email</span>
        <input v-model.trim="email" class="input" type="email" required maxlength="254" autocomplete="email" />
      </label>

      <label class="field">
        <span>Password</span>
        <input
          v-model="password"
          class="input"
          type="password"
          required
          maxlength="128"
          :minlength="isSignUp ? 8 : undefined"
          :autocomplete="isSignUp ? 'new-password' : 'current-password'"
        />
      </label>
      <p v-if="isSignUp" class="hint">Use at least 8 characters.</p>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>

      <button class="btn btn-solid" type="submit" :disabled="busy">
        {{ isSignUp ? 'Create account' : 'Sign in' }}
      </button>

      <p class="switch">
        {{ isSignUp ? 'Already have an account?' : 'New here?' }}
        <button type="button" class="link" @click="toggleMode">
          {{ isSignUp ? 'Sign in' : 'Create an account' }}
        </button>
      </p>
    </form>
  </main>
</template>

<style scoped>
.auth {
  max-width: 28rem;
  margin: 0 auto;
  padding: 10vh 24px 56px;
}

.brand {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: clamp(3rem, 12vw, 4.5rem);
  line-height: 0.95;
}

.lead {
  margin: 12px 0 28px;
  color: var(--ink-soft);
  font-size: 1.1875rem;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.75rem;
}

.switch {
  margin: 0;
}
</style>
