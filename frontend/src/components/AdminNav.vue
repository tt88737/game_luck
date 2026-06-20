<script setup lang="ts">
import { RouterLink } from 'vue-router'

const primaryItems = [
  { to: '/admin', label: 'Dashboard' },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/regions', label: 'Regions' },
  { to: '/admin/legal-documents', label: 'Legal Docs' },
  { to: '/admin/lobby', label: 'Lobby' },
  { to: '/admin/campaigns', label: 'Campaigns' },
  { to: '/admin/packages', label: 'Packages' },
  { to: '/admin/orders', label: 'Orders' },
  { to: '/admin/kyc', label: 'KYC Review' },
  { to: '/admin/redemptions', label: 'Redemptions' },
  { to: '/admin/wallet-ledger', label: 'Wallet Ledger' },
  { to: '/admin/amoe', label: 'AMOE' },
  { to: '/admin/support', label: 'Support' },
  { to: '/admin/audit-logs', label: 'Audit logs' },
]
</script>

<template>
  <aside class="admin-nav" aria-label="Admin navigation">
    <strong>Tang Luck Ops</strong>
    <RouterLink v-for="item in primaryItems" :key="item.to" :to="item.to">{{ item.label }}</RouterLink>
  </aside>
</template>
