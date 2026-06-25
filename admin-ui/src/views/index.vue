<template>
  <div class="app-container home">
    <section class="hero">
      <div>
        <h1>GameLuck Admin</h1>
        <p>包网平台运营后台，用于管理平台、租户、钱包、游戏、活动、风控和审核流程。</p>
      </div>
      <el-tag type="success" effect="light">基础版</el-tag>
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
          <el-table-column prop="module" label="模块" min-width="150" />
          <el-table-column prop="task" label="当前重点" min-width="220" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.type">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="panel">
          <h2>建设边界</h2>
          <ul>
            <li>保留权限、租户隔离、审计日志和数据权限，不破坏后台底座能力。</li>
            <li>GameLuck 业务模块通过钱包、游戏、活动和结算接口逐步接入。</li>
            <li>新增业务页面前，先清理无关入口，保持后台菜单和职责清晰。</li>
          </ul>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index" lang="ts">
const metrics = [
  { label: '后台底座', value: '已就绪', hint: '登录、租户、菜单、日志' },
  { label: '钱包中心', value: '规划中', hint: '多币种可配置开关' },
  { label: '游戏接入', value: '已预留', hint: 'Cocos / 三方游戏桥接' },
  { label: '风控审核', value: '规划中', hint: '审批、审计、限制规则' }
];

const tasks = [
  { module: '平台后台', task: '保持核心权限和租户边界稳定', status: '进行中', type: 'success' },
  { module: '钱包中心', task: '定义账户、流水、冻结、充值和提现流程', status: '下一步', type: 'warning' },
  { module: '游戏接入', task: '预留游戏入口、回调和会话接入能力', status: '已预留', type: 'info' },
  { module: '前端减法', task: '清理无关链接、默认品牌和无用入口', status: '进行中', type: 'success' }
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
