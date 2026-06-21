import { createRouter, createWebHistory } from 'vue-router'

const AppHome = () => import('../views/app/AppHome.vue')
const AppShell = () => import('../views/app/AppShell.vue')
const AppWallet = () => import('../views/app/AppWallet.vue')
const AppActivity = () => import('../views/app/AppActivity.vue')
const AppStore = () => import('../views/app/AppStore.vue')
const AppKyc = () => import('../views/app/AppKyc.vue')
const AppRedemption = () => import('../views/app/AppRedemption.vue')
const AppSlots = () => import('../views/app/AppSlots.vue')
const AppInbox = () => import('../views/app/AppInbox.vue')
const AdminDashboard = () => import('../views/admin/AdminDashboard.vue')
const AdminCampaigns = () => import('../views/admin/AdminCampaigns.vue')
const AdminAuditLogs = () => import('../views/admin/AdminAuditLogs.vue')
const AdminP1Operations = () => import('../views/admin/AdminP1Operations.vue')
const AdminPlaceholder = () => import('../views/admin/AdminPlaceholder.vue')
const AdminRegions = () => import('../views/admin/AdminRegions.vue')
const AdminLegalDocuments = () => import('../views/admin/AdminLegalDocuments.vue')
const AdminLobby = () => import('../views/admin/AdminLobby.vue')
const AdminPackages = () => import('../views/admin/AdminPackages.vue')
const AdminOrders = () => import('../views/admin/AdminOrders.vue')
const AdminUsers = () => import('../views/admin/AdminUsers.vue')
const AdminKycReview = () => import('../views/admin/AdminKycReview.vue')
const AdminRedemptions = () => import('../views/admin/AdminRedemptions.vue')
const AdminWalletLedger = () => import('../views/admin/AdminWalletLedger.vue')
const AdminGames = () => import('../views/admin/AdminGames.vue')
const AdminGameRounds = () => import('../views/admin/AdminGameRounds.vue')
const AdminActivityDashboard = () => import('../views/admin/AdminActivityDashboard.vue')
const AdminNotifications = () => import('../views/admin/AdminNotifications.vue')

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/app' },
    {
      path: '/app',
      component: AppShell,
      children: [
        { path: '', component: AppHome },
        { path: 'register', redirect: { path: '/app', query: { auth: 'register' } } },
        { path: 'login', redirect: { path: '/app', query: { auth: 'login' } } },
        { path: 'wallet', component: AppWallet },
        { path: 'activity', component: AppActivity },
        { path: 'store', component: AppStore },
        { path: 'kyc', component: AppKyc },
        { path: 'redemption', component: AppRedemption },
        { path: 'slots/:gameCode', component: AppSlots },
        { path: 'inbox', component: AppInbox },
      ],
    },
    { path: '/admin', component: AdminDashboard },
    { path: '/admin/users', component: AdminUsers },
    { path: '/admin/regions', component: AdminRegions },
    { path: '/admin/legal-documents', component: AdminLegalDocuments },
    { path: '/admin/lobby', component: AdminLobby },
    { path: '/admin/campaigns', component: AdminCampaigns },
    { path: '/admin/packages', component: AdminPackages },
    { path: '/admin/orders', component: AdminOrders },
    { path: '/admin/kyc', component: AdminKycReview },
    { path: '/admin/redemptions', component: AdminRedemptions },
    { path: '/admin/wallet-ledger', component: AdminWalletLedger },
    { path: '/admin/games', component: AdminGames },
    { path: '/admin/game-rounds', component: AdminGameRounds },
    { path: '/admin/activity-dashboard', component: AdminActivityDashboard },
    { path: '/admin/notifications', component: AdminNotifications },
    { path: '/admin/amoe', component: AdminPlaceholder, props: { title: 'AMOE' } },
    { path: '/admin/support', component: AdminPlaceholder, props: { title: 'Support' } },
    { path: '/admin/audit-logs', component: AdminAuditLogs },
    { path: '/admin/p1', component: AdminP1Operations },
  ],
})
