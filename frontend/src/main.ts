import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import { router } from './router'
import { installI18n } from './i18n'

const app = createApp(App)
installI18n(app)
app.use(createPinia()).use(router).mount('#app')
