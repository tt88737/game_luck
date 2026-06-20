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
        <p class="eyebrow">{{ eyebrow ?? $t('admin.operationsModule') }}</p>
        <h1>{{ title }}</h1>
      </div>
      <span class="status-tag pending">{{ $t('admin.planned') }}</span>
    </header>

    <section class="status-panel">
      <strong>{{ title }}</strong>
      <span>{{ $t('admin.plannedModuleBody') }}</span>
    </section>
  </AdminLayout>
</template>
