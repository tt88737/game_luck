<script setup lang="ts">
import { RouterLink } from 'vue-router'

type NavItem = {
  to?: string
  key: string
  status?: 'live' | 'planned'
}

const navGroups: Array<{ titleKey: string, impactKey: string, items: NavItem[] }> = [
  { titleKey: 'admin.nav.dashboard', impactKey: 'admin.impact.operations', items: [{ to: '/admin', key: 'admin.dashboard' }, { key: 'admin.nav.biSummary', status: 'planned' }] },
  { titleKey: 'admin.nav.userGuest', impactKey: 'admin.impact.account', items: [{ to: '/admin/users', key: 'admin.users' }, { key: 'admin.nav.guestSessions', status: 'planned' }, { key: 'admin.nav.bindingHistory', status: 'planned' }] },
  { titleKey: 'admin.nav.storePackages', impactKey: 'admin.impact.store', items: [{ to: '/admin/packages', key: 'admin.packages' }, { to: '/admin/orders', key: 'admin.orders' }, { key: 'admin.nav.paymentConfig', status: 'planned' }] },
  { titleKey: 'admin.nav.promoRewards', impactKey: 'admin.impact.promo', items: [{ to: '/admin/campaigns', key: 'admin.campaigns' }, { to: '/admin/activity-dashboard', key: 'admin.activityCenter' }, { key: 'admin.nav.vipReferralTournament', status: 'planned' }] },
  { titleKey: 'admin.nav.gameLobby', impactKey: 'admin.impact.lobby', items: [{ to: '/admin/lobby', key: 'admin.lobby' }, { to: '/admin/games', key: 'admin.games' }, { to: '/admin/game-rounds', key: 'admin.gameRounds' }, { key: 'admin.nav.providerJackpot', status: 'planned' }] },
  { titleKey: 'admin.nav.inboxNotification', impactKey: 'admin.impact.inbox', items: [{ to: '/admin/notifications', key: 'admin.notifications' }, { key: 'admin.nav.messageTemplates', status: 'planned' }] },
  { titleKey: 'admin.nav.walletLedger', impactKey: 'admin.impact.wallet', items: [{ to: '/admin/wallet-ledger', key: 'admin.walletLedger' }, { key: 'admin.nav.scLotsPlaythrough', status: 'planned' }] },
  { titleKey: 'admin.nav.redemption', impactKey: 'admin.impact.redeem', items: [{ to: '/admin/redemptions', key: 'admin.redemptionRequests' }, { key: 'admin.nav.redemptionConfig', status: 'planned' }] },
  { titleKey: 'admin.nav.kycRisk', impactKey: 'admin.impact.kycRisk', items: [{ to: '/admin/kyc', key: 'admin.kycApplications' }, { key: 'admin.nav.riskQueue', status: 'planned' }] },
  { titleKey: 'admin.nav.compliance', impactKey: 'admin.impact.compliance', items: [{ to: '/admin/regions', key: 'admin.regions' }, { key: 'admin.nav.scPolicy', status: 'planned' }] },
  { titleKey: 'admin.nav.cmsRulesAmoe', impactKey: 'admin.impact.legal', items: [{ to: '/admin/legal-documents', key: 'admin.legalDocs' }, { to: '/admin/amoe', key: 'nav.amoe' }] },
  { titleKey: 'admin.nav.support', impactKey: 'admin.impact.support', items: [{ to: '/admin/support', key: 'admin.support' }] },
  { titleKey: 'admin.nav.auditLogs', impactKey: 'admin.impact.audit', items: [{ to: '/admin/audit-logs', key: 'admin.auditLogs' }] },
  { titleKey: 'admin.nav.systemRbac', impactKey: 'admin.impact.system', items: [{ key: 'admin.nav.rolesPermissions', status: 'planned' }] },
]
</script>

<template>
  <aside class="admin-nav" aria-label="Admin navigation">
    <strong>{{ $t('admin.operations') }}</strong>
    <section v-for="group in navGroups" :key="group.titleKey" class="admin-nav-group">
      <div class="admin-nav-group-title">
        <span>{{ $t(group.titleKey) }}</span>
        <small>{{ $t(group.impactKey) }}</small>
      </div>
      <template v-for="item in group.items" :key="`${group.titleKey}-${item.key}`">
        <RouterLink v-if="item.to" :to="item.to">{{ $t(item.key) }}</RouterLink>
        <span v-else class="planned-nav-item">{{ $t(item.key) }} · {{ $t('admin.nav.planned') }}</span>
      </template>
    </section>
  </aside>
</template>
