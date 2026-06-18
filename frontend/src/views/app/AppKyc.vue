<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { KycStatus } from '../../api/contracts'

const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const status = ref<KycStatus | null>(null)
const form = reactive({
  legalName: 'P1 Demo User',
  birthDate: '1994-03-02',
  addressLine: '100 Demo Street',
  stateCode: 'CA',
})

const statusLabel = computed(() => status.value?.status ?? 'not_started')

onMounted(loadStatus)

async function loadStatus() {
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

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'KYC request failed.'
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">P1 KYC</p>
        <h1>Identity review</h1>
      </div>
      <RouterLink class="plain-link" to="/app/redemption">Redeem</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">Loading KYC status...</section>
    <section v-else>
      <div class="section-block">
        <div class="section-title">
          <h2>Current status</h2>
          <span class="status-tag" :class="{ active: statusLabel === 'approved', pending: statusLabel === 'reviewing' }">{{ statusLabel }}</span>
        </div>
        <p class="notice">Sandbox KYC stores a review state only. Admin approval is required before redemption requests can be created.</p>
        <p v-if="status?.reviewReason" class="notice success">{{ status.reviewReason }}</p>
      </div>

      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>Application</h2>
          <span>Manual review</span>
        </div>
        <form class="form-stack" @submit.prevent="submitKyc">
          <label>
            Legal name
            <input v-model="form.legalName" required />
          </label>
          <label>
            Birth date
            <input v-model="form.birthDate" type="date" required />
          </label>
          <label>
            Address
            <input v-model="form.addressLine" required />
          </label>
          <label>
            State
            <select v-model="form.stateCode">
              <option value="CA">CA</option>
              <option value="FL">FL</option>
              <option value="NY">NY</option>
            </select>
          </label>
          <button data-test="submit-kyc" :disabled="submitting">{{ submitting ? 'Submitting' : 'Submit for review' }}</button>
        </form>
      </section>
    </section>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app">Home</RouterLink>
      <RouterLink to="/app/store">Store</RouterLink>
      <RouterLink to="/app/kyc">KYC</RouterLink>
      <RouterLink to="/app/redemption">Redeem</RouterLink>
      <RouterLink to="/app/wallet">Wallet</RouterLink>
    </nav>
  </main>
</template>
