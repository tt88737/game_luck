<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ApiError, apiGet, apiPost } from '../api/http'
import type { AcceptedDocument, ComplianceDocument, LoginRequest, RegisterResponse } from '../api/contracts'
import { i18n } from '../i18n'
import { useSessionStore } from '../stores/session'

const props = defineProps<{
  modelValue: boolean
  initialMode?: 'register' | 'login'
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  authenticated: []
}>()

const session = useSessionStore()
const mode = ref<'register' | 'login'>(props.initialMode ?? 'register')
const loadingDocs = ref(false)
const submitting = ref(false)
const error = ref('')
const documents = ref<ComplianceDocument[]>([])
const accepted = ref<Record<string, boolean>>({})
const bindForm = reactive({
  email: '',
  password: '',
  birthDate: '1990-01-01',
  stateCode: 'CA',
})
const loginForm = reactive<LoginRequest>({
  email: '',
  password: '',
})

const requiredAccepted = computed(() => documents.value
  .filter((doc) => ['terms', 'sweepstakes_rules', 'privacy'].includes(doc.documentType))
  .every((doc) => accepted.value[doc.documentType]))

const selectedDocuments = computed<AcceptedDocument[]>(() => documents.value
  .filter((doc) => accepted.value[doc.documentType])
  .map((doc) => ({ documentType: doc.documentType, version: doc.version })))

watch(() => props.initialMode, (value) => {
  if (value) mode.value = value
})

watch(() => props.modelValue, (open) => {
  if (open && mode.value === 'register') void loadDocuments()
})

onMounted(() => {
  if (props.modelValue && mode.value === 'register') void loadDocuments()
})

async function loadDocuments() {
  if (documents.value.length || loadingDocs.value) return
  loadingDocs.value = true
  error.value = ''
  try {
    documents.value = await apiGet<ComplianceDocument[]>('/compliance/documents')
    accepted.value = Object.fromEntries(documents.value.map((doc) => [doc.documentType, false]))
  } catch (err) {
    error.value = messageFrom(err, i18n.t('register.failed'))
  } finally {
    loadingDocs.value = false
  }
}

function selectMode(nextMode: 'register' | 'login') {
  mode.value = nextMode
  error.value = ''
  if (nextMode === 'register') void loadDocuments()
}

async function bindAccount() {
  error.value = ''
  if (!requiredAccepted.value) {
    error.value = i18n.t('register.mustAccept')
    return
  }
  submitting.value = true
  try {
    await session.bindEmail({
      email: bindForm.email,
      password: bindForm.password,
      birthDate: bindForm.birthDate,
      countryCode: 'US',
      stateCode: bindForm.stateCode,
      acceptedDocuments: selectedDocuments.value,
    })
    emit('authenticated')
    close()
  } catch (err) {
    error.value = messageFrom(err, i18n.t('register.failed'))
  } finally {
    submitting.value = false
  }
}

async function login() {
  error.value = ''
  submitting.value = true
  try {
    const response = await apiPost<RegisterResponse>('/auth/login', loginForm)
    session.applyAuthResponse(response)
    emit('authenticated')
    close()
  } catch (err) {
    error.value = messageFrom(err, i18n.t('login.failed'))
  } finally {
    submitting.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}

function messageFrom(err: unknown, fallback: string) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return fallback
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="auth-modal-backdrop" role="presentation" @click.self="close">
      <section class="auth-modal" role="dialog" aria-modal="true" aria-label="Account access">
        <header class="auth-modal-header">
          <div>
            <p class="eyebrow">Tang Luck</p>
            <h2>{{ mode === 'register' ? $t('auth.bindAccount') : $t('auth.signIn') }}</h2>
          </div>
          <button type="button" class="icon-button" :aria-label="$t('auth.close')" @click="close">x</button>
        </header>

        <div class="auth-tabs" role="tablist">
          <button
            type="button"
            data-test="auth-bind-tab"
            :class="{ active: mode === 'register' }"
            @click="selectMode('register')"
          >
            {{ $t('auth.bindAccount') }}
          </button>
          <button
            type="button"
            data-test="auth-login-tab"
            :class="{ active: mode === 'login' }"
            @click="selectMode('login')"
          >
            {{ $t('auth.signIn') }}
          </button>
        </div>

        <section v-if="mode === 'register'">
          <p v-if="loadingDocs" class="notice">{{ $t('register.loadingDocuments') }}</p>
          <form v-else class="form-stack" @submit.prevent="bindAccount">
            <label>
              {{ $t('common.email') }}
              <input v-model="bindForm.email" type="email" required autocomplete="email" />
            </label>
            <label>
              {{ $t('common.password') }}
              <input v-model="bindForm.password" type="password" required autocomplete="new-password" />
            </label>
            <label>
              {{ $t('common.birthDate') }}
              <input v-model="bindForm.birthDate" type="date" required />
            </label>
            <label>
              {{ $t('common.state') }}
              <select v-model="bindForm.stateCode">
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
            <button data-test="auth-bind-submit" :disabled="submitting || !requiredAccepted">
              {{ submitting ? $t('common.submitting') : $t('auth.bindAccount') }}
            </button>
          </form>
        </section>

        <form v-else class="form-stack" @submit.prevent="login">
          <p class="notice">{{ $t('auth.signInSwitchHint') }}</p>
          <label>
            {{ $t('common.email') }}
            <input v-model="loginForm.email" type="email" required autocomplete="email" />
          </label>
          <label>
            {{ $t('common.password') }}
            <input v-model="loginForm.password" type="password" required autocomplete="current-password" />
          </label>
          <p v-if="error" class="notice danger">{{ error }}</p>
          <button data-test="auth-login-submit" :disabled="submitting">
            {{ submitting ? $t('login.submitting') : $t('login.submit') }}
          </button>
        </form>
      </section>
    </div>
  </Teleport>
</template>
