<script setup lang="ts">
import AdminLayout from '../../components/AdminLayout.vue'

defineProps<{
  title: string
  eyebrow?: string
}>()
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">{{ eyebrow ?? 'Operations module' }}</p>
        <h1>{{ title }}</h1>
      </div>
      <span class="status-tag pending">Planned</span>
    </header>

    <section class="status-panel">
      <strong>{{ title }}</strong>
      <span>This module is scheduled in the front/back-office closure plan.</span>
    </section>
  </AdminLayout>
</template>
