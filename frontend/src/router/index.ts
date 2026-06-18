import { createRouter, createWebHistory } from 'vue-router'

const AppHome = () => import('../views/app/AppHome.vue')
const AppWallet = () => import('../views/app/AppWallet.vue')
const AppActivity = () => import('../views/app/AppActivity.vue')
const AdminDashboard = () => import('../views/admin/AdminDashboard.vue')
const AdminCampaigns = () => import('../views/admin/AdminCampaigns.vue')
const AdminAuditLogs = () => import('../views/admin/AdminAuditLogs.vue')

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/app' },
    { path: '/app', component: AppHome },
    { path: '/app/wallet', component: AppWallet },
    { path: '/app/activity', component: AppActivity },
    { path: '/admin', component: AdminDashboard },
    { path: '/admin/campaigns', component: AdminCampaigns },
    { path: '/admin/wallet-ledger', component: AdminDashboard },
    { path: '/admin/audit-logs', component: AdminAuditLogs },
  ],
})
