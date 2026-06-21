<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import AuthModal from '../../components/AuthModal.vue'
import { useSessionStore } from '../../stores/session'
import { i18n } from '../../i18n'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const authOpen = ref(false)
const authMode = ref<'register' | 'login'>('register')
const bootError = ref('')
const booting = ref(false)

const accountLabel = computed(() => {
  if (booting.value && !session.userId) return i18n.t('account.loading')
  if (session.isGuest) return i18n.t('account.guest')
  if (session.email) return session.email
  if (session.userId) return `${i18n.t('account.formal')} ${session.userId}`
  return i18n.t('account.loading')
})

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
  booting.value = true
  bootError.value = ''
  try {
    await session.ensureGuestSession()
  } catch {
    bootError.value = i18n.t('account.sessionError')
  } finally {
    booting.value = false
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
      <RouterLink class="brand-mark" to="/lobby">Tang Luck</RouterLink>
      <div class="account-strip">
        <span class="status-tag" :class="{ active: !session.isGuest }">{{ accountLabel }}</span>
        <span v-if="session.userId" class="account-id">ID {{ session.userId }}</span>
      </div>
      <div class="account-actions">
        <button v-if="bootError" type="button" class="small-action" @click="bootGuest">{{ $t('account.retry') }}</button>
        <button v-if="session.isGuest" type="button" class="small-action" @click="openAuth('register')">{{ $t('auth.bindAccount') }}</button>
        <button type="button" class="small-action ghost" @click="openAuth('login')">{{ $t('auth.signIn') }}</button>
      </div>
    </header>

    <p v-if="bootError" class="app-boot-error">{{ bootError }}</p>
    <RouterView />

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/store">{{ $t('nav.store') }}</RouterLink>
      <RouterLink to="/promo">{{ $t('nav.promo') }}</RouterLink>
      <RouterLink to="/lobby">{{ $t('nav.lobby') }}</RouterLink>
      <RouterLink to="/inbox">{{ $t('nav.inbox') }}</RouterLink>
      <RouterLink to="/me">{{ $t('nav.me') }}</RouterLink>
    </nav>

    <AuthModal
      :model-value="authOpen"
      :initial-mode="authMode"
      @update:model-value="onModalChange"
    />
  </div>
</template>
