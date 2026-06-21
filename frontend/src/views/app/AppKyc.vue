<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { KycStatus } from '../../api/contracts'
import { i18n } from '../../i18n'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const status = ref<KycStatus | null>(null)
const form = reactive({
  legalName: '',
  birthDate: '1994-03-02',
  addressLine: '',
  stateCode: 'CA',
})

const statusLabel = computed(() => status.value?.status ?? 'not_started')

onMounted(loadStatus)

async function loadStatus() {
  if (!session.userId || session.isGuest) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    status.value = await apiGet<KycStatus>('/kyc/status')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function submitKyc() {
  if (session.isGuest) {
    openBindAccount()
    return
  }
  submitting.value = true
  error.value = ''
  try {
    status.value = await apiPost<KycStatus>('/kyc/applications', form)
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    submitting.value = false
  }
}

function openBindAccount() {
  window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: { mode: 'register' } }))
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('kyc.requestFailed')
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">{{ $t('common.kyc') }}</p>
        <h1>{{ $t('kyc.heading') }}</h1>
      </div>
      <RouterLink class="plain-link" to="/app/redemption">{{ $t('nav.redeem') }}</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('kyc.loading') }}</section>
    <section v-else-if="!session.userId || session.isGuest" class="status-panel">
      <strong>{{ $t('register.heading') }}</strong>
      <span>{{ session.isGuest ? 'Bind account before submitting identity verification.' : $t('kyc.signInRequired') }}</span>
      <button v-if="session.isGuest" type="button" class="small-action" @click="openBindAccount">Bind account</button>
      <RouterLink v-else class="plain-link" to="/app?auth=register">{{ $t('register.submit') }}</RouterLink>
    </section>
    <section v-else>
      <div class="section-block">
        <div class="section-title">
          <h2>{{ $t('common.currentStatus') }}</h2>
          <span class="status-tag" :class="{ active: statusLabel === 'approved', pending: statusLabel === 'reviewing' }">{{ statusLabel }}</span>
        </div>
        <p class="notice">{{ $t('kyc.notice') }}</p>
        <p v-if="status?.reviewReason" class="notice success">{{ status.reviewReason }}</p>
      </div>

      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('common.application') }}</h2>
          <span>{{ $t('admin.manualApproval') }}</span>
        </div>
        <form class="form-stack" @submit.prevent="submitKyc">
          <label>
            {{ $t('common.legalName') }}
            <input v-model="form.legalName" required />
          </label>
          <label>
            {{ $t('common.birthDate') }}
            <input v-model="form.birthDate" type="date" required />
          </label>
          <label>
            {{ $t('common.address') }}
            <input v-model="form.addressLine" required />
          </label>
          <label>
            {{ $t('common.state') }}
            <select v-model="form.stateCode">
              <option value="CA">CA</option>
              <option value="FL">FL</option>
              <option value="NY">NY</option>
            </select>
          </label>
          <button data-test="submit-kyc" :disabled="submitting">{{ submitting ? $t('common.submitting') : $t('common.submitForReview') }}</button>
        </form>
      </section>
    </section>
  </main>
</template>
