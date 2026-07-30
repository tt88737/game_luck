<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { ClientExchangeOption, ClientExchangeOrder, WalletAccount, WalletLedger } from '../types/client'

const accounts = ref<WalletAccount[]>([])
const ledgers = ref<WalletLedger[]>([])
const exchangeOptions = ref<ClientExchangeOption[]>([])
const lastExchange = ref<ClientExchangeOrder | null>(null)
const loading = ref(false)
const submittingExchange = ref(false)
const error = ref('')
const exchangeSuccess = ref('')
const exchangeForm = reactive({
  ruleId: '',
  amount: '10.00',
})

const selectedExchangeRule = computed(() =>
  exchangeOptions.value.find((item) => String(item.exchangeRuleId) === exchangeForm.ruleId) || exchangeOptions.value[0],
)

const sourceAccount = computed(() =>
  accounts.value.find((account) => account.currencyCode === selectedExchangeRule.value?.fromCurrencyCode),
)

const sourceBalance = computed(() => Number(sourceAccount.value?.availableBalance || 0))
const exchangeAmount = computed(() => Number(exchangeForm.amount || 0))
const minExchangeAmount = computed(() => Number(selectedExchangeRule.value?.minFromAmount || 0))
const maxExchangeAmount = computed(() => Number(selectedExchangeRule.value?.maxFromAmount || 0))

const estimatedFee = computed(() => {
  const rule = selectedExchangeRule.value
  if (!rule || exchangeAmount.value <= 0) {
    return '0.00'
  }
  if (rule.feeType === 'FIXED') {
    return Number(rule.feeValue || 0).toFixed(2)
  }
  if (rule.feeType === 'PERCENT') {
    return (exchangeAmount.value * Number(rule.feeValue || 0) / 100).toFixed(2)
  }
  return '0.00'
})

const exchangeDebitAmount = computed(() => exchangeAmount.value + Number(estimatedFee.value))

const estimatedTargetAmount = computed(() => {
  const rule = selectedExchangeRule.value
  if (!rule || exchangeAmount.value <= 0) {
    return '0.00'
  }
  const target = exchangeAmount.value * Number(rule.rateValue || 0) - Number(estimatedFee.value)
  return Math.max(target, 0).toFixed(2)
})

const feeLabel = computed(() => {
  const rule = selectedExchangeRule.value
  if (!rule || rule.feeType === 'NONE') {
    return '无'
  }
  if (rule.feeType === 'PERCENT') {
    return `${Number(rule.feeValue || 0).toFixed(2)}%`
  }
  return `${Number(rule.feeValue || 0).toFixed(2)} ${rule.fromCurrencyCode}`
})

const exchangeAmountOutOfRange = computed(() => {
  if (!selectedExchangeRule.value || exchangeAmount.value <= 0) {
    return false
  }
  return exchangeAmount.value < minExchangeAmount.value || (maxExchangeAmount.value > 0 && exchangeAmount.value > maxExchangeAmount.value)
})

const canSubmitExchange = computed(() =>
  Boolean(
    sessionState.member
    && selectedExchangeRule.value
    && exchangeAmount.value > 0
    && !exchangeAmountOutOfRange.value
    && sourceBalance.value >= exchangeDebitAmount.value
    && !submittingExchange.value,
  ),
)

async function loadLedger(currencyCode?: string) {
  const currency = currencyCode || accounts.value[0]?.currencyCode || 'GC'
  ledgers.value = (await clientApi.walletLedgers(currency)).records
}

async function loadWallet() {
  if (!sessionState.member) {
    accounts.value = []
    ledgers.value = []
    exchangeOptions.value = []
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [walletAccounts, options] = await Promise.all([
      clientApi.walletAccounts(),
      clientApi.walletExchangeOptions(),
    ])
    accounts.value = walletAccounts
    exchangeOptions.value = options
    if (!exchangeForm.ruleId && options.length) {
      exchangeForm.ruleId = String(options[0].exchangeRuleId)
    }
    await loadLedger()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '钱包加载失败'
  } finally {
    loading.value = false
  }
}

function exchangeIdempotencyKey() {
  return `h5-exchange-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function submitExchange() {
  const rule = selectedExchangeRule.value
  if (!rule || !canSubmitExchange.value) {
    return
  }
  submittingExchange.value = true
  error.value = ''
  exchangeSuccess.value = ''
  try {
    const result = await clientApi.submitWalletExchange(rule.exchangeRuleId, exchangeForm.amount, exchangeIdempotencyKey())
    lastExchange.value = result
    exchangeSuccess.value = `兑换成功：${result.fromAmount} ${result.fromCurrencyCode} -> ${result.toAmount} ${result.toCurrencyCode}`
    await loadWallet()
    await loadLedger(rule.toCurrencyCode)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '兑换失败'
  } finally {
    submittingExchange.value = false
  }
}

onMounted(loadWallet)
watch(() => sessionState.member?.memberId, () => loadWallet())
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">钱包</p>
    <h1>余额与最近流水</h1>
    <p class="muted">余额、兑换和流水数据来自后端玩家端钱包 API。</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>请先登录</strong>
    <RouterLink class="btn primary" to="/login">登录</RouterLink>
  </section>

  <template v-else>
    <p v-if="loading" class="muted">正在加载钱包...</p>
    <p v-if="error" class="error-text">{{ error }}</p>

    <section class="data-grid">
      <article v-for="account in accounts" :key="account.currencyCode" class="metric-card">
        <span>{{ account.currencyName }}</span>
        <strong>{{ account.availableBalance }}</strong>
        <small>冻结：{{ account.frozenBalance }} | {{ account.playable ? '可用于游戏' : '已停用' }}</small>
      </article>
    </section>

    <section class="exchange-panel">
      <div class="exchange-copy">
        <p class="eyebrow">币种兑换</p>
        <h2>按规则兑换钱包余额</h2>
        <p v-if="selectedExchangeRule" class="muted">
          {{ selectedExchangeRule.fromCurrencyCode }} -> {{ selectedExchangeRule.toCurrencyCode }}
          | 费率 {{ Number(selectedExchangeRule.rateValue).toFixed(2) }}
          | 手续费 {{ feeLabel }}
        </p>
        <p v-else class="muted">当前暂无可用兑换规则。</p>
      </div>
      <form class="inline-form exchange-form" @submit.prevent="submitExchange">
        <select v-model="exchangeForm.ruleId" :disabled="!exchangeOptions.length || submittingExchange" aria-label="兑换规则">
          <option v-for="option in exchangeOptions" :key="option.exchangeRuleId" :value="String(option.exchangeRuleId)">
            {{ option.fromCurrencyCode }} -> {{ option.toCurrencyCode }}
          </option>
        </select>
        <input v-model="exchangeForm.amount" inputmode="decimal" aria-label="兑换金额" />
        <button class="btn primary" type="submit" :disabled="!canSubmitExchange">
          {{ submittingExchange ? '兑换中...' : '提交兑换' }}
        </button>
      </form>
      <div class="exchange-estimate">
        <span>预计手续费 <strong>{{ estimatedFee }}</strong></span>
        <span>预计扣款 <strong>{{ exchangeDebitAmount.toFixed(2) }} {{ selectedExchangeRule?.fromCurrencyCode || '' }}</strong></span>
        <span>预计到账 <strong>{{ estimatedTargetAmount }} {{ selectedExchangeRule?.toCurrencyCode || '' }}</strong></span>
        <span v-if="exchangeAmountOutOfRange" class="error-text">
          金额需在 {{ minExchangeAmount.toFixed(2) }} - {{ maxExchangeAmount.toFixed(2) }} {{ selectedExchangeRule?.fromCurrencyCode }} 内
        </span>
        <span v-if="selectedExchangeRule && sourceBalance < exchangeDebitAmount" class="error-text">余额不足</span>
      </div>
    </section>

    <p v-if="exchangeSuccess" class="success-text">{{ exchangeSuccess }}</p>
    <p v-if="lastExchange" class="muted">
      最近兑换订单 {{ lastExchange.exchangeOrderNo }} | {{ lastExchange.status }}
    </p>

    <section class="table-panel">
      <h2>最近流水</h2>
      <div v-if="ledgers.length" class="table-list">
        <div v-for="row in ledgers" :key="row.ledgerId" class="table-row">
          <span>{{ row.createdAt }}</span>
          <strong>{{ row.direction }}</strong>
          <span>{{ row.amount }}</span>
          <em>{{ row.bizType }}</em>
        </div>
      </div>
      <p v-else class="muted">暂无流水记录。</p>
    </section>
  </template>
</template>

<style scoped>
.exchange-panel {
  display: grid;
  gap: 14px;
  margin-top: 18px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.exchange-copy {
  display: grid;
  gap: 8px;
}

.exchange-form select {
  min-width: 178px;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: white;
  color: var(--text);
  font: inherit;
}

.exchange-form input {
  flex: 0 1 260px;
}

.exchange-form button {
  min-width: 112px;
}

.exchange-estimate {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--muted);
  line-height: 1.5;
}

.exchange-estimate span {
  min-height: 32px;
  padding: 5px 9px;
  border: 1px solid #eee5d7;
  border-radius: 7px;
  background: #fffaf0;
}

.exchange-estimate strong {
  color: var(--surface-strong);
}

@media (max-width: 760px) {
  .data-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .metric-card {
    min-height: 112px;
    padding: 14px;
  }

  .exchange-form {
    align-items: stretch;
    flex-direction: column;
  }

  .exchange-form select,
  .exchange-form input,
  .exchange-form button {
    width: 100%;
  }

  .exchange-form input {
    flex: none;
    height: 42px;
  }
}
</style>
