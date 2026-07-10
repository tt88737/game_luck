export default {
  route: {
    dashboard: '首页',
    document: '项目文档',
    systemManagement: '系统管理',
    tenantManagement: '租户管理',
    systemMonitor: '系统监控',
    systemTools: '系统工具',
    walletCenter: '钱包中心',
    paymentCenter: '支付中心',
    gameTrading: '游戏交易',
    userManagement: '用户管理',
    roleManagement: '角色管理',
    menuManagement: '菜单管理',
    deptManagement: '部门管理',
    postManagement: '岗位管理',
    dictManagement: '字典管理',
    configManagement: '参数设置',
    noticeManagement: '通知公告',
    logManagement: '日志管理',
    onlineUser: '在线用户',
    cacheMonitor: '缓存监控',
    codeGenerator: '代码生成',
    tenantPackage: '租户套餐管理',
    clientManagement: '客户端管理',
    ossConfig: '文件配置管理',
    adminMonitor: 'Admin监控',
    fileManagement: '文件管理',
    jobCenter: '任务调度中心',
    operLog: '操作日志',
    loginLog: '登录日志',
    currencyConfig: '币种配置',
    walletAccounts: '钱包账户',
    walletTransactions: '账变流水',
    walletReleaseRecords: '释放记录',
    walletFreezeRecords: '冻结记录',
    depositOrders: '充值订单',
    gameBetOrders: '模拟下注订单',
    walletRules: '钱包规则',
    reportCenter: '报表中心',
    reportOverview: '数据总览',
    reportOverviewQuery: '报表总览查询',
    memberCenter: '会员中心',
    memberProfiles: '会员资料',
    memberQuery: '会员资料查询',
    memberAdd: '会员资料新增',
    memberEdit: '会员资料编辑',
    memberRemove: '会员资料删除',
    promotionCenter: '促销中心',
    promotionRewards: '促销奖励',
    promotionQuery: '促销奖励查询',
    promotionAdd: '促销奖励新增',
    promotionEdit: '促销奖励编辑',
    promotionRemove: '促销奖励删除',
    promotionClaim: '促销奖励领取',
    redemptionCenter: '兑换中心',
    redemptionOrders: '兑换订单',
    redemptionQuery: '兑换订单查询',
    redemptionAdd: '兑换订单新增',
    redemptionApprove: '兑换订单审核通过',
    redemptionReject: '兑换订单审核拒绝',
    personalCenter: '个人中心'
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
    logout: '退出登录',
    search: '搜索',
    logoutConfirm: '确定注销并退出系统吗？',
    prompt: '提示'
  },
  dashboardHome: {
    title: 'GameLuck Admin',
    subtitle: '包网平台运营后台，用于管理平台、租户、钱包、游戏、活动、风控和审核流程。',
    edition: '基础版',
    columns: {
      module: '模块',
      focus: '当前重点',
      status: '状态'
    },
    metrics: {
      foundation: {
        label: '后台底座',
        value: '已就绪',
        hint: '登录、租户、菜单、日志'
      },
      wallet: {
        label: '钱包中心',
        value: '规划中',
        hint: '多币种可配置开关'
      },
      game: {
        label: '游戏接入',
        value: '已预留',
        hint: 'Cocos / 三方游戏桥接'
      },
      risk: {
        label: '风控审核',
        value: '规划中',
        hint: '审批、审计、限制规则'
      }
    },
    tasks: {
      platform: {
        module: '平台后台',
        task: '保持核心权限和租户边界稳定',
        status: '进行中'
      },
      wallet: {
        module: '钱包中心',
        task: '定义账户、流水、冻结、充值和提现流程',
        status: '下一步'
      },
      game: {
        module: '游戏接入',
        task: '预留游戏入口、回调和会话接入能力',
        status: '已预留'
      },
      frontend: {
        module: '前端减法',
        task: '清理无关链接、默认品牌和无用入口',
        status: '进行中'
      }
    },
    boundary: {
      title: '建设边界',
      items: [
        '保留权限、租户隔离、审计日志和数据权限，不破坏后台底座能力。',
        'GameLuck 业务模块通过钱包、游戏、活动和结算接口逐步接入。',
        '新增业务页面前，先清理无关入口，保持后台菜单和职责清晰。'
      ]
    }
  },
  common: {
    search: '搜索',
    reset: '重置',
    add: '新增',
    delete: '删除',
    confirm: '确定',
    cancel: '取消',
    edit: '编辑',
    detail: '详情',
    operation: '操作',
    createTime: '创建时间',
    updateTime: '更新时间',
    remark: '备注',
    status: '状态',
    currency: '币种',
    amount: '金额',
    success: {
      add: '新增成功',
      edit: '修改成功',
      delete: '删除成功',
      operate: '操作成功',
      statusUpdated: '状态已更新'
    }
  },
  memberProfile: {
    fields: {
      memberNo: '会员编号',
      username: '用户名',
      nickname: '昵称',
      riskLevel: '风险等级',
      registerChannel: '注册渠道',
      lastLoginTime: '最后登录'
    },
    placeholders: {
      memberNo: '请输入会员编号',
      username: '请输入用户名',
      nickname: '请输入昵称',
      status: '请选择状态',
      riskLevel: '请选择风险',
      remark: '请输入备注'
    },
    status: {
      ACTIVE: '正常',
      FROZEN: '冻结',
      DISABLED: '禁用'
    },
    risk: {
      NORMAL: '正常',
      WATCH: '观察',
      HIGH: '高风险'
    },
    actions: {
      view: '查看详情',
      edit: '编辑资料',
      setActive: '设为正常',
      freeze: '冻结会员',
      disable: '禁用会员',
      delete: '删除资料'
    },
    dialog: {
      add: '新增会员',
      edit: '编辑会员',
      detail: '会员详情'
    },
    rules: {
      username: '用户名不能为空',
      status: '状态不能为空',
      riskLevel: '风险等级不能为空'
    },
    confirm: {
      status: '确认将会员 {username} 状态改为 {status}？',
      delete: '确认删除选中的会员资料？'
    }
  },
  promotionReward: {
    fields: {
      promotionNo: '活动编号',
      promotionName: '活动名称',
      rewardAmount: '奖励金额',
      startTime: '开始时间',
      endTime: '结束时间',
      claimNo: '领取单号',
      memberId: '会员ID',
      walletTransactionNo: '钱包交易号',
      failReason: '失败原因',
      claimTime: '领取时间'
    },
    placeholders: {
      promotionNo: '请输入活动编号',
      promotionName: '请输入活动名称',
      currency: '请选择币种',
      status: '请选择状态',
      startTime: '不填则立即可用',
      endTime: '不填则长期有效',
      remark: '请输入备注',
      memberId: '请输入会员ID'
    },
    status: {
      ACTIVE: '启用',
      INACTIVE: '停用',
      SUCCESS: '成功',
      FAILED: '失败'
    },
    actions: {
      edit: '编辑配置',
      enable: '启用活动',
      disable: '停用活动',
      claim: '会员领取',
      claims: '领取记录',
      delete: '删除配置'
    },
    dialog: {
      add: '新增促销奖励',
      edit: '编辑促销奖励',
      claim: '会员领取奖励',
      claimRecords: '{name} - 领取记录'
    },
    rules: {
      promotionName: '活动名称不能为空',
      currency: '币种不能为空',
      rewardAmount: '奖励金额不能为空',
      status: '状态不能为空',
      memberId: '会员ID不能为空'
    },
    confirm: {
      status: '确认{action}该促销奖励？',
      delete: '确认删除选中的促销奖励？'
    },
    messages: {
      claimSuccess: '领取成功'
    }
  },
  redemptionOrder: {
    fields: {
      redemptionOrderNo: '订单号',
      memberId: '会员ID',
      redemptionMethod: '方式',
      accountRef: '账户备注',
      freezeNo: '冻结单号',
      freezeWalletTransactionNo: '冻结交易',
      settleWalletTransactionNo: '结算交易',
      releaseWalletTransactionNo: '释放交易',
      auditTime: '审核时间',
      auditReason: '审核原因',
      failReason: '失败原因'
    },
    placeholders: {
      redemptionOrderNo: '请输入订单号',
      memberId: '请输入会员ID',
      currency: '请选择币种',
      status: '请选择状态',
      redemptionMethod: '默认 SIMULATED',
      accountRef: '仅填写模拟或脱敏账户信息',
      remark: '请输入备注',
      auditReason: '请输入审核原因'
    },
    status: {
      PENDING: '待审核',
      APPROVED: '已通过',
      REJECTED: '已拒绝',
      FAILED: '失败'
    },
    filters: {
      pending: '待审核',
      approved: '已通过',
      rejected: '已拒绝',
      failed: '失败',
      all: '全部'
    },
    actions: {
      view: '查看详情',
      approve: '审核通过',
      reject: '审核拒绝'
    },
    dialog: {
      add: '新增模拟兑换订单',
      approve: '审核通过',
      reject: '审核拒绝',
      detail: '兑换订单详情'
    },
    rules: {
      memberId: '会员ID不能为空',
      currency: '币种不能为空',
      amount: '金额不能为空',
      rejectReason: '审核拒绝时必须填写原因'
    },
    confirm: {
      audit: '确认{action}该兑换订单？'
    }
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
