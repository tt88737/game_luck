<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, sessionState } from '../stores/session'

const router = useRouter()
const form = reactive({
  username: 'demo_player',
  password: 'Demo123456',
})

async function submit() {
  await login(form.username, form.password)
  await router.push('/wallet')
}
</script>

<template>
  <section class="form-screen">
    <div>
      <p class="eyebrow">Player login</p>
      <h1>Sign in before wallet changes.</h1>
      <p class="muted">Use the seeded demo player to verify the client API loop.</p>
    </div>

    <form class="panel-form" @submit.prevent="submit">
      <label>
        Username
        <input v-model="form.username" autocomplete="username" />
      </label>
      <label>
        Password
        <input v-model="form.password" type="password" autocomplete="current-password" />
      </label>
      <p v-if="sessionState.error" class="error-text">{{ sessionState.error }}</p>
      <button class="btn primary" type="submit" :disabled="sessionState.loading">
        {{ sessionState.loading ? 'Signing in' : 'Login' }}
      </button>
      <RouterLink class="text-link" to="/register">Create player account</RouterLink>
    </form>
  </section>
</template>
