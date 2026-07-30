import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import WalletView from '../views/WalletView.vue'
import GamesView from '../views/GamesView.vue'
import PromotionsView from '../views/PromotionsView.vue'
import PurchaseView from '../views/PurchaseView.vue'
import RedemptionsView from '../views/RedemptionsView.vue'
import HelpView from '../views/HelpView.vue'
import SimulatedCheckoutView from '../views/SimulatedCheckoutView.vue'
import PurchaseResultView from '../views/PurchaseResultView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/wallet', name: 'wallet', component: WalletView },
    { path: '/purchase', name: 'purchase', component: PurchaseView },
    { path: '/simulated-checkout/:providerSessionNo', name: 'simulated-checkout', component: SimulatedCheckoutView },
    { path: '/purchase-result/:sessionNo', name: 'purchase-result', component: PurchaseResultView },
    { path: '/games', name: 'games', component: GamesView },
    { path: '/promotions', name: 'promotions', component: PromotionsView },
    { path: '/redemptions', name: 'redemptions', component: RedemptionsView },
    { path: '/help', name: 'help', component: HelpView },
  ],
})

export default router
