<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import AuthModal from '../../components/AuthModal.vue'
import { useSessionStore } from '../../stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const authOpen = ref(false)
const authMode = ref<'register' | 'login'>('register')
const bootError = ref('')

const accountLabel = computed(() => session.isGuest ? 'Guest' : session.email || `User ${session.userId}`)

onMounted(() => {
  void bootGuest()
  window.addEventListener('open-auth-modal', onOpenAuthModal as EventListener)
})

onBeforeUnmount(() => {
  window.removeEventListener('open-auth-modal', onOpenAuthModal as EventListener)
})

watch(() => route.query.auth, (value) => {
  if (value === 'register' || value === 'login') openAuth(value)
}, { immediate: true })

async function bootGuest() {
  bootError.value = ''
  try {
    await session.ensureGuestSession()
  } catch (err) {
    bootError.value = err instanceof Error ? err.message : 'Guest session failed.'
  }
}

function openAuth(mode: 'register' | 'login') {
  authMode.value = mode
  authOpen.value = true
}

function onOpenAuthModal(event: CustomEvent<{ mode?: 'register' | 'login' }>) {
  openAuth(event.detail?.mode ?? 'register')
}

async function onModalChange(open: boolean) {
  authOpen.value = open
  if (!open && route.query.auth) {
    const query = { ...route.query }
    delete query.auth
    await router.replace({ path: route.path, query })
  }
}
</script>

<template>
  <div class="app-shell">
    <header class="app-account-bar">
      <RouterLink class="brand-mark" to="/app">Tang Luck</RouterLink>
      <div class="account-strip">
        <span class="status-tag" :class="{ active: !session.isGuest }">{{ accountLabel }}</span>
        <span v-if="session.userId" class="account-id">ID {{ session.userId }}</span>
      </div>
      <div class="account-actions">
        <button v-if="session.isGuest" type="button" class="small-action" @click="openAuth('register')">Bind account</button>
        <button type="button" class="small-action ghost" @click="openAuth('login')">Sign in</button>
      </div>
    </header>

    <p v-if="bootError" class="app-boot-error">{{ bootError }}</p>
    <RouterView />

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app">{{ $t('nav.home') }}</RouterLink>
      <RouterLink to="/app/slots/lucky_slots">{{ $t('nav.slots') }}</RouterLink>
      <RouterLink to="/app/activity">{{ $t('nav.activity') }}</RouterLink>
      <RouterLink to="/app/inbox">Inbox</RouterLink>
      <RouterLink to="/app/wallet">{{ $t('common.wallet') }}</RouterLink>
    </nav>

    <AuthModal
      :model-value="authOpen"
      :initial-mode="authMode"
      @update:model-value="onModalChange"
    />
  </div>
</template>
