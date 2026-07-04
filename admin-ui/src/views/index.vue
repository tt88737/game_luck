<template>
  <div class="app-container home">
    <section class="hero">
      <div>
        <h1>{{ t('dashboardHome.title') }}</h1>
        <p>{{ t('dashboardHome.subtitle') }}</p>
      </div>
      <el-tag type="success" effect="light">{{ t('dashboardHome.edition') }}</el-tag>
    </section>

    <el-row :gutter="16">
      <el-col v-for="item in metrics" :key="item.key" :xs="24" :sm="12" :lg="6">
        <div class="metric">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :lg="14">
        <el-table :data="tasks" border>
          <el-table-column prop="module" :label="t('dashboardHome.columns.module')" min-width="150" />
          <el-table-column prop="task" :label="t('dashboardHome.columns.focus')" min-width="220" />
          <el-table-column prop="status" :label="t('dashboardHome.columns.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="row.type">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="panel">
          <h2>{{ t('dashboardHome.boundary.title') }}</h2>
          <ul>
            <li v-for="item in boundaryItems" :key="item">{{ item }}</li>
          </ul>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index" lang="ts">
import { useI18n } from 'vue-i18n';

const { t, tm } = useI18n();

const metricKeys = ['foundation', 'wallet', 'game', 'risk'];
const taskKeys = ['platform', 'wallet', 'game', 'frontend'];

const metrics = computed(() =>
  metricKeys.map((key) => ({
    key,
    label: t(`dashboardHome.metrics.${key}.label`),
    value: t(`dashboardHome.metrics.${key}.value`),
    hint: t(`dashboardHome.metrics.${key}.hint`)
  }))
);

const tasks = computed(() =>
  taskKeys.map((key, index) => ({
    module: t(`dashboardHome.tasks.${key}.module`),
    task: t(`dashboardHome.tasks.${key}.task`),
    status: t(`dashboardHome.tasks.${key}.status`),
    type: index === 1 ? 'warning' : index === 2 ? 'info' : 'success'
  }))
);

const boundaryItems = computed(() => tm('dashboardHome.boundary.items') as string[]);
</script>

<style lang="scss" scoped>
.home {
  color: var(--el-text-color-primary);
}

.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  h1 {
    margin: 0 0 8px;
    font-size: 24px;
    font-weight: 650;
  }

  p {
    max-width: 720px;
    margin: 0;
    color: var(--el-text-color-secondary);
    line-height: 1.6;
  }
}

.metric {
  min-height: 108px;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-md);
  background: var(--el-bg-color);

  span,
  small {
    display: block;
    color: var(--el-text-color-secondary);
  }

  strong {
    display: block;
    margin: 10px 0 8px;
    font-size: 22px;
    font-weight: 650;
  }
}

.section-row {
  row-gap: 16px;
}

.panel {
  min-height: 100%;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-md);
  background: var(--el-bg-color);

  h2 {
    margin: 0 0 12px;
    font-size: 16px;
    font-weight: 650;
  }

  ul {
    padding-left: 18px;
    margin: 0;
    color: var(--el-text-color-regular);
    line-height: 1.8;
  }
}
</style>
