<template>
  <section class="form-screen">
    <div>
      <p class="eyebrow">玩家注册</p>
      <h1>租户规则加载后再开放注册</h1>
      <p class="muted">注册入口将在渠道开关和租户策略接入后开放。</p>
    </div>

    <form class="panel-form">
      <label>
        用户名
        <input placeholder="请输入用户名" />
      </label>
      <label>
        邀请码
        <input placeholder="选填" />
      </label>
      <label class="check-row">
        <input type="checkbox" />
        我已满足本地资格要求
      </label>
      <button class="btn primary" type="button" disabled>注册暂未开放</button>
    </form>
  </section>
</template>
