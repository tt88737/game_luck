<script setup lang="ts">
import AdminNav from './AdminNav.vue'
</script>

<template>
  <main class="admin-shell">
    <AdminNav />
    <section class="admin-content">
      <slot />
    </section>
  </main>
</template>
