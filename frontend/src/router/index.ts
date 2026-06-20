import { createRouter, createWebHistory } from 'vue-router'

const AppHome = () => import('../views/app/AppHome.vue')
const AppWallet = () => import('../views/app/AppWallet.vue')
const AppActivity = () => import('../views/app/AppActivity.vue')
const AppRegister = () => import('../views/app/AppRegister.vue')
const AppLogin = () => import('../views/app/AppLogin.vue')
const AppStore = () => import('../views/app/AppStore.vue')
const AppKyc = () => import('../views/app/AppKyc.vue')
const AppRedemption = () => import('../views/app/AppRedemption.vue')
const AdminDashboard = () => import('../views/admin/AdminDashboard.vue')
const AdminCampaigns = () => import('../views/admin/AdminCampaigns.vue')
const AdminAuditLogs = () => import('../views/admin/AdminAuditLogs.vue')
const AdminP1Operations = () => import('../views/admin/AdminP1Operations.vue')
const AdminPlaceholder = () => import('../views/admin/AdminPlaceholder.vue')

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/app' },
    { path: '/app/register', component: AppRegister },
    { path: '/app/login', component: AppLogin },
    { path: '/app', component: AppHome },
    { path: '/app/wallet', component: AppWallet },
    { path: '/app/activity', component: AppActivity },
    { path: '/app/store', component: AppStore },
    { path: '/app/kyc', component: AppKyc },
    { path: '/app/redemption', component: AppRedemption },
    { path: '/admin', component: AdminDashboard },
    { path: '/admin/users', component: AdminPlaceholder, props: { title: 'Users' } },
    { path: '/admin/regions', component: AdminPlaceholder, props: { title: 'Regions' } },
    { path: '/admin/legal-documents', component: AdminPlaceholder, props: { title: 'Legal Docs' } },
    { path: '/admin/lobby', component: AdminPlaceholder, props: { title: 'Lobby' } },
    { path: '/admin/campaigns', component: AdminCampaigns },
    { path: '/admin/packages', component: AdminPlaceholder, props: { title: 'Packages' } },
    { path: '/admin/orders', component: AdminPlaceholder, props: { title: 'Orders' } },
    { path: '/admin/kyc', component: AdminPlaceholder, props: { title: 'KYC Review' } },
    { path: '/admin/redemptions', component: AdminPlaceholder, props: { title: 'Redemptions' } },
    { path: '/admin/wallet-ledger', component: AdminPlaceholder, props: { title: 'Wallet Ledger' } },
    { path: '/admin/amoe', component: AdminPlaceholder, props: { title: 'AMOE' } },
    { path: '/admin/support', component: AdminPlaceholder, props: { title: 'Support' } },
    { path: '/admin/audit-logs', component: AdminAuditLogs },
    { path: '/admin/p1', component: AdminP1Operations },
  ],
})
