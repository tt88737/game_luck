import { defineStore } from 'pinia'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('tangluck_token') ?? '',
    userId: localStorage.getItem('tangluck_user_id') ?? '',
  }),
  actions: {
    setSession(token: string, userId: string) {
      this.token = token
      this.userId = userId
      localStorage.setItem('tangluck_token', token)
      localStorage.setItem('tangluck_user_id', userId)
    },
  },
})
