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
    walletFundProperties: '资金属性',
    walletCurrencyPolicies: '币种策略',
    walletExchangeRules: '币种兑换规则',
    walletExchangeOrders: '币种兑换订单',
    paymentSessions: '支付会话',
    paymentWebhookEvents: '支付回调事件',
    reportCenter: '报表中心',
    reportOverview: '数据总览',
    reportOverviewQuery: '报表总览查询',
    reportTrends: '趋势看板',
    paymentReconciliation: '支付对账',
    reportTrendsQuery: '趋势看板查询',
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
      memberNo: '会员ID',
      username: '用户名',
      nickname: '昵称',
      riskLevel: '风险等级',
      riskReason: '风险原因',
      riskSource: '风险来源',
      riskUpdatedTime: '风险更新时间',
      kycStatus: 'KYC状态',
      kycReviewReason: 'KYC备注',
      kycReviewedBy: 'KYC操作人',
      kycReviewTime: 'KYC操作时间',
      registerChannel: '注册渠道',
      countryCode: '国家',
      stateCode: '州/省',
      countryState: '国家/州',
      complianceConsent: '合规确认',
      lastLoginTime: '最后登录'
    },
    placeholders: {
      memberNo: '请输入会员ID',
      username: '请输入用户名',
      nickname: '请输入昵称',
      status: '请选择状态',
      riskLevel: '请选择风险',
      kycStatus: '请选择KYC状态',
      kycReviewReason: '请输入KYC审核备注',
      countryCode: '国家码',
      stateCode: '州/省码',
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
    consent: {
      age: '年龄',
      terms: '条款',
      privacy: '隐私',
      rules: '规则',
      accepted: '已确认',
      notAccepted: '未确认'
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
      riskLevel: '风险等级不能为空',
      kycStatus: 'KYC状态不能为空'
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
      promotionType: '活动类型',
      rewardItems: '奖励配置',
      rewardAmount: '奖励金额',
      startTime: '开始时间',
      endTime: '结束时间',
      claimNo: '领取单号',
      memberId: '会员ID',
      claimDate: '领取日期',
      rewardSnapshot: '实际发放奖励',
      walletTransactionNo: '钱包交易号',
      failReason: '失败原因',
      claimTime: '领取时间'
    },
    placeholders: {
      promotionNo: '请输入活动编号',
      promotionName: '请输入活动名称',
      promotionType: '请选择活动类型',
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
    types: {
      GENERAL: '普通奖励',
      DAILY_LOGIN: '每日登录'
    },
    actions: {
      edit: '编辑配置',
      enable: '启用活动',
      disable: '停用活动',
      claim: '会员领取',
      claims: '领取记录',
      addRewardItem: '新增奖励项',
      removeRewardItem: '删除奖励项',
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
      promotionType: '活动类型不能为空',
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
      claimSuccess: '领取成功',
      rewardItemsRequired: '请至少配置一项奖励'
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
  paymentSession: {
    title: '支付会话',
    empty: '暂无符合筛选条件的支付会话',
    fields: {
      sessionNo: '平台会话号',
      purchaseOrderNo: '购买订单号',
      providerSessionNo: '支付方会话号',
      member: '会员',
      providerCode: '支付方',
      amount: '支付金额',
      createdRange: '创建时间',
      expireTime: '过期时间',
      completedTime: '完成时间',
      checkoutUrl: '收银台地址'
    },
    range: { to: '至' },
    actions: { retryLoad: '重新加载' },
    detail: { title: '支付会话详情' },
    messages: { loadFailed: '支付会话加载失败', detailFailed: '支付会话详情加载失败', permissionDenied: '当前账号没有支付会话查询权限' }
  },
  paymentWebhookEvent: {
    title: '支付回调事件',
    empty: '暂无符合筛选条件的支付回调事件',
    fields: {
      providerEventId: '支付方事件号',
      purchaseOrderNo: '购买订单号',
      sessionNo: '平台会话号',
      providerSessionNo: '支付方会话号',
      eventType: '事件类型',
      providerCode: '支付方',
      receivedRange: '接收时间',
      receivedTime: '接收时间',
      attempts: '处理次数',
      failureReason: '失败原因',
      lastProcessingTime: '最近处理时间',
      linkedReversal: '关联追偿'
    },
    range: { to: '至' },
    actions: { retryLoad: '重新加载', retry: '重试处理', openReversal: '打开追偿复核列表' },
    detail: { title: '支付回调事件详情', signatureDigest: '签名摘要（只读）', rawPayload: '原始请求体（只读）' },
    retry: { confirm: '确认重试事件 {eventId}？本操作只会重新处理不可变的原始事件，并写入操作日志。' },
    messages: {
      loadFailed: '支付回调事件加载失败',
      detailFailed: '事件详情加载失败',
      permissionDenied: '当前账号没有支付回调事件查询权限',
      retrySuccess: '回调事件重试完成',
      retryFailed: '回调事件重试失败',
      rawFormatFallback: '原始请求体不是有效 JSON，已按原文只读展示'
    }
  },
  purchaseReversalReview: {
    empty: '暂无符合条件的追偿审核案件',
    fields: {
      reversalNo: '追偿单号',
      purchaseOrderNo: '购买订单号',
      member: '会员',
      reversalType: '追偿类型',
      shortfall: '逐币种缺口',
      riskLevel: '风险等级',
      waiting: '等待时长',
      dispositionStatus: '处置状态',
      createTime: '创建时间',
      orderStatus: '订单状态',
      retryCount: '重试次数',
      reason: '处理原因',
      reviewReason: '复核原因',
      currency: '币种',
      required: '应追偿',
      available: '可用余额',
      recovered: '已追回',
      walletTransactionNo: '钱包交易号',
      riskReason: '风险原因',
      grantType: '发放类型',
      amount: '金额',
      fundProperty: '资金属性',
      turnoverTaskNo: '流水任务号',
      eventKey: '事件键',
      eventType: '事件类型',
      eventStatus: '事件状态',
      operationType: '操作类型',
      operator: '操作人',
      reviewNote: '审核意见'
    },
    placeholders: { reversalNo: '请输入追偿单号', purchaseOrderNo: '请输入购买订单号', member: '请输入会员编号', reversalType: '请选择追偿类型' },
    filters: { pending: '待复核', recovered: '已追回', loss: '已确认损失' },
    actions: { detail: '查看审核详情', retry: '再次全额追偿', acceptLoss: '确认损失结案' },
    detail: {
      title: '拒付审核详情',
      caseAndOrder: '案件与订单',
      recoveryItems: '逐币种追偿',
      memberRisk: '会员风险',
      grants: '发放快照',
      paymentEvents: '支付事件',
      history: '操作历史',
      noHistory: '暂无审核操作记录'
    },
    duration: { minutes: '{count} 分钟', hours: '{count} 小时', days: '{count} 天' },
    retry: { confirm: '确认再次执行全币种全额追偿？任一币种余额不足时将全部不扣款。' },
    loss: {
      title: '确认逐币种损失',
      notePlaceholder: '请填写确认损失的依据',
      noteRequired: '确认损失时必须填写审核意见',
      confirm: '确认损失并结案',
      secondConfirm: '确认按上方逐币种缺口结案？此操作不会扣款，也不会取消流水任务。'
    },
    messages: { retrySuccess: '全币种追偿成功，案件已结案', retryInsufficient: '余额仍不足，未发生任何扣款', lossAccepted: '已确认逐币种损失并结案' }
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
  },
  paymentReconciliation: {
    title: '支付对账',
    provider: '支付渠道',
    date: '账单日期',
    file: '文件名',
    counts: '总数 / 无效 / 异常',
    creator: '创建人',
    created: '创建时间',
    upload: '上传账单',
    execute: '执行对账',
    detail: '批次详情',
    issueDetail: '异常详情',
    invalid: '无效行',
    matched: '已匹配',
    issues: '异常',
    resolve: '确认结论',
    ignore: '忽略异常',
    retry: '重试',
    empty: '暂无对账批次',
    filteredEmpty: '当前筛选条件下无结果',
    permissionDenied: '无权查看支付对账批次',
    loadFailed: '对账批次加载失败',
    uploadFailed: '账单上传失败',
    uploaded: '账单已上传并完成校验',
    csvOnly: '仅支持 CSV 文件',
    tooLarge: '文件不得超过 10 MiB',
    failed: '对账失败',
    reconciling: '正在执行对账，操作暂不可用',
    completed: '对账已完成',
    resolved: '对账结论已记录',
    conflict: '记录已被其他操作员更新，请刷新后重试',
    issueChanged: '当前异常记录已切换，请重新打开后操作',
    executeConfirm: '确认执行该批次对账？包含无效行的批次不可执行。',
    noMutation: '只记录对账结论，不改变支付状态、冲正或钱包余额',
    invalidExecuteBlocked: '批次包含无效行，修正源文件并重新上传后才能执行',
    detailFailed: '批次详情加载失败',
    tabLoadFailed: '当前分页数据加载失败',
    issueLoadFailed: '异常详情加载失败',
    emptyLines: '暂无行记录',
    emptyIssues: '暂无异常记录',
    issueType: '异常类型',
    orderNo: '订单号',
    sessionNo: '会话号',
    providerRecord: '渠道记录号',
    rowNumber: '源行号',
    amount: '金额',
    validation: '校验结果',
    validationPassed: '校验通过',
    duplicateProviderRecord: '渠道记录号重复',
    invalidEventType: '事件类型无效',
    invalidAmount: '金额无效',
    invalidCurrency: '币种无效',
    invalidTimestamp: '时间格式无效',
    actionUpload: '上传',
    actionValidate: '校验',
    actionExecute: '执行对账',
    actionFail: '执行失败',
    actionResolve: '确认结论',
    actionIgnore: '忽略异常',
    providerPlatformAmount: '渠道 / 平台金额',
    providerPlatformStatus: '渠道 / 平台状态',
    related: '关联记录',
    reversal: '冲正复核',
    diagnostics: '诊断快照',
    canonicalFields: '规范化原始字段',
    platformOnly: '该异常仅来源于平台记录，没有对应的渠道源行',
    classification: '结论分类',
    remark: '备注',
    providerRequired: '请选择支付渠道',
    dateRequired: '请选择账单日期',
    fileRequired: '请选择 CSV 文件',
    classificationRequired: '请选择结论分类',
    remarkRequired: '请填写备注',
    executeFailed: '执行对账失败，请重试',
    resolutionFailed: '记录对账结论失败，请重试'
  },
  paymentSettlement: {
    title: '支付结算',
    create: '创建结算批次',
    detail: '结算详情',
    number: '结算批次号',
    provider: '支付渠道',
    currency: '币种',
    window: 'UTC 结算时段',
    gross: '支付总额',
    refunds: '退款',
    chargebacks: '拒付',
    fees: '费用',
    net: '净结算额',
    payable: '应付渠道',
    receivable: '应收渠道',
    calculate: '计算',
    close: '关闭结算',
    events: '条事件',
    items: '事件明细',
    evidence: '对账证据',
    history: '操作历史',
    eventType: '事件类型',
    orderNo: '订单号',
    amount: '金额',
    feePercent: '支付费率',
    fixedFee: '支付固定费',
    chargebackFee: '拒付固定费',
    remark: '关闭备注',
    openReconciliation: '打开支付对账工作台',
    noMutation: '关闭结算只记录财务确认，不会修改支付订单、冲正或钱包余额',
    permissionDenied: '无权查看支付结算',
    loadFailed: '支付结算加载失败',
    detailFailed: '结算详情加载失败',
    retry: '重试',
    empty: '暂无结算批次',
    filteredEmpty: '当前筛选条件下无结算批次',
    emptyItems: '暂无结算事件',
    emptyHistory: '暂无操作历史',
    noEvidence: '暂无对账证据',
    processing: '结算正在计算，请稍后刷新',
    failed: '结算计算失败',
    providerRequired: '请输入支付渠道',
    currencyRequired: '请输入三位币种代码',
    windowRequired: '请选择 UTC 结算时段',
    windowInvalid: '结算时段必须大于 0 且不超过 31 天',
    remarkRequired: '请填写关闭备注',
    calculateConfirm: '将按已处理支付事件生成不可变财务快照，是否继续？',
    calculateAccepted: '结算计算已完成',
    closed: '结算批次已关闭',
    status: { CREATED: '已创建', CALCULATING: '计算中', CALCULATED: '已计算', CLOSED: '已关闭', FAILED: '失败' }
  },
  paymentSettlementReport: {
    title: '支付结算报表',
    range: 'UTC 日期',
    last7: '最近 7 天',
    last31: '最近 31 天',
    provider: '支付渠道',
    currency: '币种',
    date: '报表日期',
    batchCount: '批次数',
    eventCount: '事件数',
    gross: '支付总额',
    refunds: '退款',
    chargebacks: '拒付',
    fees: '费用',
    net: '净结算额',
    payable: '应付渠道',
    receivable: '应收渠道',
    batches: '个批次',
    export: '导出 CSV',
    exporting: '导出中',
    exported: '报表已导出',
    exportFailed: '报表导出失败，请缩小范围或重试',
    generatedAt: '生成时间',
    groupBatches: '分组结算批次',
    number: '结算批次号',
    periodStart: '周期开始',
    periodEnd: '周期结束',
    openBatch: '打开结算详情',
    retry: '重试',
    permissionDenied: '无权查看支付结算报表',
    loadFailed: '支付结算报表加载失败',
    detailFailed: '分组批次加载失败',
    empty: '暂无已关闭结算报表数据',
    filteredEmpty: '当前筛选条件下无报表数据',
    emptyBatches: '该分组暂无可见结算批次',
    rangeRequired: '请选择 UTC 日期范围',
    rangeInvalid: 'UTC 日期范围必须为 1 至 31 天',
    currencyInvalid: '币种必须是三位字母代码'
  },
  reportTrends: {
    title: '每日趋势',
    subtitle: '按日期查看近期全业务经营趋势。',
    refresh: '刷新',
    empty: '暂无趋势数据',
    range: {
      seven: '最近 7 天',
      thirty: '最近 30 天'
    },
    cards: {
      depositAmount: '充值金额',
      gameNet: '游戏净额',
      rewards: '奖励金额',
      approvedRedeem: '通过兑换',
      pendingRedeem: '待审兑换'
    },
    columns: {
      date: '日期',
      members: '新增会员',
      depositOrders: '充值订单',
      depositAmount: '充值金额',
      gameOrders: '投注订单',
      betAmount: '投注额',
      payoutAmount: '派彩额',
      gameNet: '游戏净额',
      promotionClaims: '领取次数',
      rewardAmount: '奖励金额',
      redemptionOrders: '兑换订单',
      pendingRedeem: '待审核',
      approvedRedeemAmount: '通过金额'
    },
    messages: {
      loadFailed: '每日趋势加载失败'
    }
  }
};
