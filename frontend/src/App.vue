<script setup lang="ts">
import { onMounted } from 'vue'
import { useSessionStore } from './stores/session'

const session = useSessionStore()

onMounted(() => {
  void session.hydrate()
})
</script>

<template>
  <RouterView />
</template>
