<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, sessionState } from '../stores/session'

const router = useRouter()
const form = reactive({
  username: 'demo_player',
  password: 'Demo123456',
})

async function submit() {
  await login(form.username, form.password)
  await router.push('/wallet')
}
</script>

<template>
  <section class="form-screen">
    <div>
      <p class="eyebrow">玩家登录</p>
      <h1>登录后查看钱包和游戏状态</h1>
      <p class="muted">使用已初始化的演示玩家账号验证玩家端 API 闭环。</p>
    </div>

    <form class="panel-form" @submit.prevent="submit">
      <label>
        用户名
        <input v-model="form.username" autocomplete="username" />
      </label>
      <label>
        密码
        <input v-model="form.password" type="password" autocomplete="current-password" />
      </label>
      <p v-if="sessionState.error" class="error-text">{{ sessionState.error }}</p>
      <button class="btn primary" type="submit" :disabled="sessionState.loading">
        {{ sessionState.loading ? '登录中' : '登录' }}
      </button>
      <RouterLink class="text-link" to="/register">创建玩家账号</RouterLink>
    </form>
  </section>
</template>
