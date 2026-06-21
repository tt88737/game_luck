<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ApiError, apiPost } from '../../api/http'
import type { LoginRequest, RegisterResponse } from '../../api/contracts'
import { i18n } from '../../i18n'
import { useSessionStore } from '../../stores/session'

const router = useRouter()
const session = useSessionStore()
const submitting = ref(false)
const error = ref('')
const form = reactive<LoginRequest>({
  email: '',
  password: '',
})

async function login() {
  submitting.value = true
  error.value = ''
  try {
    const response = await apiPost<RegisterResponse>('/auth/login', form)
    session.applyAuthResponse(response)
    await router.push('/lobby')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    submitting.value = false
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('login.failed')
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">{{ $t('login.heading') }}</p>
        <h1>{{ $t('login.heading') }}</h1>
      </div>
      <RouterLink class="plain-link" to="/lobby?auth=register">{{ $t('login.noAccount') }}</RouterLink>
    </header>

    <section class="lobby-hero auth-hero">
      <p class="eyebrow">Tang Luck</p>
      <h2>{{ $t('login.heading') }}</h2>
      <p>{{ $t('home.registerBody') }}</p>
    </section>

    <form class="section-block form-stack" @submit.prevent="login">
      <label>
        {{ $t('common.email') }}
        <input v-model="form.email" type="email" required autocomplete="email" />
      </label>
      <label>
        {{ $t('common.password') }}
        <input v-model="form.password" type="password" required autocomplete="current-password" />
      </label>

      <p v-if="error" class="notice danger">{{ error }}</p>
      <button data-test="login-submit" :disabled="submitting">
        {{ submitting ? $t('login.submitting') : $t('login.submit') }}
      </button>
    </form>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/lobby?auth=register">{{ $t('nav.register') }}</RouterLink>
      <RouterLink to="/lobby?auth=login">{{ $t('login.submit') }}</RouterLink>
      <RouterLink to="/lobby">{{ $t('nav.lobby') }}</RouterLink>
      <RouterLink to="/store">{{ $t('nav.store') }}</RouterLink>
      <RouterLink to="/me">{{ $t('nav.me') }}</RouterLink>
    </nav>
  </main>
</template>
