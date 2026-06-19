<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { AcceptedDocument, ComplianceDocument, RegisterRequest, RegisterResponse } from '../../api/contracts'
import { useSessionStore } from '../../stores/session'
import { i18n } from '../../i18n'

const router = useRouter()
const session = useSessionStore()
const loadingDocs = ref(true)
const submitting = ref(false)
const error = ref('')
const documents = ref<ComplianceDocument[]>([])
const accepted = ref<Record<string, boolean>>({})
const form = reactive({
  email: 'player.ca@example.com',
  password: 'Password123!',
  birthDate: '1990-01-01',
  stateCode: 'CA',
})

const requiredAccepted = computed(() => documents.value
  .filter((doc) => ['terms', 'sweepstakes_rules', 'privacy'].includes(doc.documentType))
  .every((doc) => accepted.value[doc.documentType]))

const selectedDocuments = computed<AcceptedDocument[]>(() => documents.value
  .filter((doc) => accepted.value[doc.documentType])
  .map((doc) => ({ documentType: doc.documentType, version: doc.version })))

onMounted(loadDocuments)

async function loadDocuments() {
  loadingDocs.value = true
  error.value = ''
  try {
    documents.value = await apiGet<ComplianceDocument[]>('/compliance/documents')
    accepted.value = Object.fromEntries(documents.value.map((doc) => [doc.documentType, false]))
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loadingDocs.value = false
  }
}

async function register() {
  error.value = ''
  if (!requiredAccepted.value) {
    error.value = i18n.t('register.mustAccept')
    return
  }

  submitting.value = true
  try {
    const payload: RegisterRequest = {
      email: form.email,
      password: form.password,
      birthDate: form.birthDate,
      countryCode: 'US',
      stateCode: form.stateCode,
      acceptedDocuments: selectedDocuments.value,
      utmSource: 'web',
      deviceId: `web-${form.stateCode.toLowerCase()}`,
    }
    const response = await apiPost<RegisterResponse>('/auth/register', payload)
    session.setSession(response.token, String(response.user.userId))
    await router.push('/app')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    submitting.value = false
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError) return err.message
  if (err instanceof Error) return err.message
  return 'Registration failed.'
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">{{ $t('register.createAccount') }}</p>
        <h1>{{ $t('register.heading') }}</h1>
      </div>
      <RouterLink class="plain-link" to="/app">{{ $t('nav.home') }}</RouterLink>
    </header>

    <section v-if="loadingDocs" class="status-panel">{{ $t('register.loadingDocuments') }}</section>

    <form v-else class="section-block form-stack" @submit.prevent="register">
      <label>
        {{ $t('common.email') }}
        <input v-model="form.email" type="email" required autocomplete="email" />
      </label>
      <label>
        {{ $t('common.password') }}
        <input v-model="form.password" type="password" required autocomplete="new-password" />
      </label>
      <label>
        {{ $t('common.birthDate') }}
        <input v-model="form.birthDate" type="date" required />
      </label>
      <label>
        {{ $t('common.state') }}
        <select v-model="form.stateCode">
          <option value="CA">California</option>
          <option value="TX">Texas</option>
          <option value="NJ">New Jersey</option>
          <option value="WA">Washington</option>
        </select>
      </label>

      <section class="legal-checks" :aria-label="$t('register.legalDocuments')">
        <label v-for="doc in documents" :key="doc.documentType" class="check-row">
          <input v-model="accepted[doc.documentType]" type="checkbox" />
          <span>{{ doc.title }} <small>{{ doc.version }}</small></span>
        </label>
      </section>

      <p v-if="error" class="notice danger">{{ error }}</p>
      <button data-test="register-submit" :disabled="submitting || !requiredAccepted">
        {{ submitting ? $t('register.submitting') : $t('register.submit') }}
      </button>
    </form>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app/register">{{ $t('nav.register') }}</RouterLink>
      <RouterLink to="/app">{{ $t('nav.home') }}</RouterLink>
      <RouterLink to="/app/wallet">{{ $t('common.wallet') }}</RouterLink>
    </nav>
  </main>
</template>
