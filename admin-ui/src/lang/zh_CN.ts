export default {
  route: {
    dashboard: '首页',
    document: '项目文档',
    reportCenter: '报表中心',
    reportOverview: '数据总览',
    reportOverviewQuery: '报表总览查询',
    memberCenter: '会员中心',
    memberProfiles: '会员资料',
    promotionCenter: '促销中心',
    promotionRewards: '促销奖励',
    redemptionCenter: '兑换中心',
    redemptionOrders: '兑换订单'
  },
  login: {
    selectPlaceholder: '请选择/输入公司名称',
    username: '用户名',
    password: '密码',
    login: '登 录',
    logging: '登 录 中...',
    code: '验证码',
    rememberPassword: '记住我',
    switchRegisterPage: '立即注册',
    rule: {
      tenantId: {
        required: '请输入您的租户编号'
      },
      username: {
        required: '请输入您的账号'
      },
      password: {
        required: '请输入您的密码'
      },
      code: {
        required: '请输入验证码'
      }
    }
  },
  register: {
    selectPlaceholder: '请选择/输入公司名称',
    username: '用户名',
    password: '密码',
    confirmPassword: '确认密码',
    register: '注 册',
    registering: '注 册 中...',
    registerSuccess: '恭喜您，您的账号 {username} 注册成功！',
    code: '验证码',
    switchLoginPage: '使用已有账户登录',
    rule: {
      tenantId: {
        required: '请输入您的租户编号'
      },
      username: {
        required: '请输入您的账号',
        length: '用户账号长度必须介于 {min} 和 {max} 之间'
      },
      password: {
        required: '请输入您的密码',
        length: '用户密码长度必须介于 {min} 和 {max} 之间',
        pattern: '不能包含非法字符：{strings}'
      },
      code: {
        required: '请输入验证码'
      },
      confirmPassword: {
        required: '请再次输入您的密码',
        equalToPassword: '两次输入的密码不一致'
      }
    }
  },
  navbar: {
    full: '全屏',
    language: '语言',
    dashboard: '首页',
    document: '项目文档',
    message: '消息',
    layoutSize: '布局大小',
    selectTenant: '选择租户',
    layoutSetting: '布局设置',
    personalCenter: '个人中心',
    logout: '退出登录'
  },
  reportOverview: {
    title: '数据总览',
    subtitle: '实时汇总会员、钱包、支付、游戏、促销和兑换模块的 MVP 指标。',
    refresh: '刷新',
    empty: '暂无报表数据',
    sections: {
      walletPayment: '钱包与支付',
      gamePromotion: '游戏与促销',
      redemptionReview: '兑换审核'
    },
    columns: {
      metric: '指标',
      value: '数值',
      state: '状态',
      meaning: '运营含义'
    },
    cards: {
      members: '会员数',
      registeredProfiles: '已建会员资料',
      walletAccounts: '钱包账户',
      currencyAccounts: '币种账户',
      depositAmount: '充值金额',
      successfulDeposits: '成功充值',
      gameNet: '游戏净额',
      payoutMinusBet: '派彩减投注',
      rewards: '奖励金额',
      successfulClaims: '成功领取',
      pendingRedeem: '待审兑换',
      needsReview: '需要审核'
    },
    metrics: {
      walletAvailableTotal: '钱包可用余额合计',
      walletFrozenTotal: '钱包冻结余额合计',
      depositOrders: '充值订单数',
      successfulDepositAmount: '成功充值金额',
      gameOrders: '游戏订单数',
      totalBetAmount: '总投注金额',
      totalPayoutAmount: '总派彩金额',
      promotionClaims: '促销领取数',
      successfulRewardAmount: '成功奖励金额',
      redemptionOrders: '兑换订单数',
      pendingReview: '待审核',
      approved: '已通过',
      rejected: '已拒绝',
      approvedAmount: '通过金额'
    },
    states: {
      available: '可用',
      frozen: '冻结',
      clear: '正常',
      orders: '订单',
      credited: '已入账',
      debit: '扣款',
      credit: '入账',
      claims: '领取',
      action: '待处理',
      settled: '已结算',
      released: '已释放',
      amount: '金额'
    },
    meanings: {
      redemptionOrders: '所有已提交的兑换请求。',
      pendingReview: '仍需要运营审核的冻结资金。',
      approved: '已从冻结钱包余额中结算的请求。',
      rejected: '已拒绝并释放回可用余额的请求。',
      approvedAmount: '已审核通过的兑换总金额。'
    },
    messages: {
      loadFailed: '报表总览加载失败'
    }
  }
};
