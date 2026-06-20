<script setup lang="ts">
import { RouterLink } from 'vue-router'

const primaryItems = [
  { to: '/admin', key: 'admin.dashboard' },
  { to: '/admin/users', key: 'admin.users' },
  { to: '/admin/regions', key: 'admin.regions' },
  { to: '/admin/legal-documents', key: 'admin.legalDocs' },
  { to: '/admin/lobby', key: 'admin.lobby' },
  { to: '/admin/campaigns', key: 'admin.campaigns' },
  { to: '/admin/packages', key: 'admin.packages' },
  { to: '/admin/orders', key: 'admin.orders' },
  { to: '/admin/kyc', key: 'admin.kycApplications' },
  { to: '/admin/redemptions', key: 'admin.redemptionRequests' },
  { to: '/admin/wallet-ledger', key: 'admin.walletLedger' },
  { to: '/admin/amoe', key: 'nav.amoe' },
  { to: '/admin/support', key: 'admin.support' },
  { to: '/admin/audit-logs', key: 'admin.auditLogs' },
]
</script>

<template>
  <aside class="admin-nav" aria-label="Admin navigation">
    <strong>{{ $t('admin.operations') }}</strong>
    <RouterLink v-for="item in primaryItems" :key="item.to" :to="item.to">{{ $t(item.key) }}</RouterLink>
  </aside>
</template>
