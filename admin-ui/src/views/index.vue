<template>
  <div class="app-container home">
    <section class="hero">
      <div>
        <h1>GameLuck Admin</h1>
        <p>Package network operations console for platform, tenant, wallet, game, promotion, and risk workflows.</p>
      </div>
      <el-tag type="success" effect="light">MVP</el-tag>
    </section>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6" v-for="item in metrics" :key="item.label">
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
          <el-table-column prop="module" label="Module" min-width="150" />
          <el-table-column prop="task" label="Current Task" min-width="220" />
          <el-table-column prop="status" label="Status" width="120">
            <template #default="{ row }">
              <el-tag :type="row.type">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="panel">
          <h2>Build Boundary</h2>
          <ul>
            <li>Keep framework permissions, tenant isolation, audit logs, and data scope intact.</li>
            <li>Put GameLuck business modules behind clear wallet, game, promotion, and settlement APIs.</li>
            <li>Remove unused upstream entry points from the admin UI before adding new business pages.</li>
          </ul>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index" lang="ts">
const metrics = [
  { label: 'Admin Shell', value: 'Ready', hint: 'Auth, tenant, menu, log' },
  { label: 'Wallet Center', value: 'Planned', hint: 'Multi-currency switchable' },
  { label: 'Game Access', value: 'Reserved', hint: 'Cocos / provider bridge' },
  { label: 'Risk Control', value: 'Planned', hint: 'Audit and approval rules' }
];

const tasks = [
  { module: 'Platform Admin', task: 'Keep core permissions and tenant boundary stable', status: 'Active', type: 'success' },
  { module: 'Wallet Center', task: 'Define account, ledger, freeze, recharge, and withdrawal flows', status: 'Next', type: 'warning' },
  { module: 'Game Integration', task: 'Reserve provider callbacks and game session entry', status: 'Reserved', type: 'info' },
  { module: 'Frontend Cleanup', task: 'Remove unused upstream links and branding', status: 'Active', type: 'success' }
];
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
