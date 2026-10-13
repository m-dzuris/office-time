<script setup>
import { onMounted } from 'vue'
import AuthScreen from './components/AuthScreen.vue'
import OfficeTime from './components/OfficeTime.vue'
import { setUnauthorizedHandler } from './api.js'
import { useAuth } from './useAuth.js'

const { user, ready, busy, error, restore, signIn, signUp, signOut, expire, clearError } = useAuth()

// If the session ends while the app is open (expired, or signed out in another tab), go back to sign-in.
setUnauthorizedHandler(expire)

onMounted(restore)
</script>

<template>
  <template v-if="ready">
    <OfficeTime v-if="user" :user="user" @sign-out="signOut" />
    <AuthScreen v-else :busy="busy" :error="error" @sign-in="signIn" @sign-up="signUp" @switch="clearError" />
  </template>
</template>
