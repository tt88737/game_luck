<script setup lang="ts">
import { reactive } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { t } from '../i18n'
import { register, sessionState } from '../stores/session'

const router = useRouter()

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  countryCode: 'US',
  stateCode: '',
  ageConfirmed: false,
  termsAccepted: false,
  privacyAccepted: false,
  sweepstakesRulesAccepted: false,
})

async function submit() {
  await register({
    username: form.username,
    nickname: form.nickname || undefined,
    password: form.password,
    countryCode: form.countryCode,
    stateCode: form.stateCode || undefined,
    ageConfirmed: form.ageConfirmed,
    termsAccepted: form.termsAccepted,
    privacyAccepted: form.privacyAccepted,
    sweepstakesRulesAccepted: form.sweepstakesRulesAccepted,
  })
  await router.push('/wallet')
}
</script>

<template>
  <section class="form-screen">
    <div>
      <p class="eyebrow">{{ t('registerEyebrow') }}</p>
      <h1>{{ t('registerTitle') }}</h1>
      <p class="muted">{{ t('registerIntro') }}</p>
    </div>

    <form class="panel-form" @submit.prevent="submit">
      <label>
        {{ t('registerUsername') }}
        <input v-model.trim="form.username" :placeholder="t('registerUsernamePlaceholder')" required maxlength="64" />
      </label>
      <label>
        {{ t('registerNickname') }}
        <input v-model.trim="form.nickname" :placeholder="t('registerNicknamePlaceholder')" maxlength="128" />
      </label>
      <label>
        {{ t('registerPassword') }}
        <input v-model="form.password" type="password" :placeholder="t('registerPasswordPlaceholder')" required minlength="8" maxlength="64" />
      </label>
      <label>
        {{ t('registerCountry') }}
        <input v-model.trim="form.countryCode" :placeholder="t('registerCountryPlaceholder')" required maxlength="16" />
      </label>
      <label>
        {{ t('registerState') }}
        <input v-model.trim="form.stateCode" :placeholder="t('registerStatePlaceholder')" maxlength="32" />
      </label>
      <label class="check-row">
        <input v-model="form.ageConfirmed" type="checkbox" required />
        {{ t('registerAgeConfirm') }}
      </label>
      <label class="check-row">
        <input v-model="form.termsAccepted" type="checkbox" required />
        {{ t('registerTermsConfirm') }}
      </label>
      <label class="check-row">
        <input v-model="form.privacyAccepted" type="checkbox" required />
        {{ t('registerPrivacyConfirm') }}
      </label>
      <label class="check-row">
        <input v-model="form.sweepstakesRulesAccepted" type="checkbox" required />
        {{ t('registerRulesConfirm') }}
      </label>
      <p v-if="sessionState.error" class="error-text">{{ sessionState.error }}</p>
      <button class="btn primary" type="submit" :disabled="sessionState.loading">
        {{ sessionState.loading ? t('registerSubmitting') : t('registerSubmit') }}
      </button>
      <RouterLink class="text-link" to="/login">{{ t('registerLoginLink') }}</RouterLink>
    </form>
  </section>
</template>
