export default {
  // 路由国际化
  route: {
    dashboard: 'Dashboard',
    document: 'Document',
    systemManagement: 'System Management',
    tenantManagement: 'Tenant Management',
    systemMonitor: 'System Monitor',
    systemTools: 'System Tools',
    walletCenter: 'Wallet Center',
    paymentCenter: 'Payment Center',
    gameTrading: 'Game Trading',
    userManagement: 'User Management',
    roleManagement: 'Role Management',
    menuManagement: 'Menu Management',
    deptManagement: 'Department Management',
    postManagement: 'Post Management',
    dictManagement: 'Dictionary Management',
    configManagement: 'Parameter Settings',
    noticeManagement: 'Notice Management',
    logManagement: 'Log Management',
    onlineUser: 'Online Users',
    cacheMonitor: 'Cache Monitor',
    codeGenerator: 'Code Generator',
    tenantPackage: 'Tenant Packages',
    clientManagement: 'Client Management',
    ossConfig: 'File Config',
    adminMonitor: 'Admin Monitor',
    fileManagement: 'File Management',
    jobCenter: 'Job Center',
    operLog: 'Operation Logs',
    loginLog: 'Login Logs',
    currencyConfig: 'Currency Config',
    walletAccounts: 'Wallet Accounts',
    walletTransactions: 'Wallet Transactions',
    walletReleaseRecords: 'Release Records',
    walletFreezeRecords: 'Freeze Records',
    depositOrders: 'Deposit Orders',
    gameBetOrders: 'Simulated Bet Orders',
    walletFundProperties: 'Fund Properties',
    walletCurrencyPolicies: 'Currency Policies',
    walletExchangeRules: 'Currency Exchange Rules',
    walletExchangeOrders: 'Currency Exchange Orders',
    paymentSessions: 'Payment Sessions',
    paymentWebhookEvents: 'Payment Webhook Events',
    reportCenter: 'Report Center',
    reportOverview: 'Overview',
    reportOverviewQuery: 'Report Overview Query',
    reportTrends: 'Trends',
    paymentReconciliation: 'Payment Reconciliation',
    reportTrendsQuery: 'Report Trends Query',
    memberCenter: 'Member Center',
    memberProfiles: 'Member Profiles',
    memberQuery: 'Member Profile Query',
    memberAdd: 'Member Profile Add',
    memberEdit: 'Member Profile Edit',
    memberRemove: 'Member Profile Remove',
    promotionCenter: 'Promotion Center',
    promotionRewards: 'Promotion Rewards',
    promotionQuery: 'Promotion Reward Query',
    promotionAdd: 'Promotion Reward Add',
    promotionEdit: 'Promotion Reward Edit',
    promotionRemove: 'Promotion Reward Remove',
    promotionClaim: 'Promotion Reward Claim',
    redemptionCenter: 'Redemption Center',
    redemptionOrders: 'Redemption Orders',
    redemptionQuery: 'Redemption Order Query',
    redemptionAdd: 'Redemption Order Add',
    redemptionApprove: 'Redemption Order Approve',
    redemptionReject: 'Redemption Order Reject',
    personalCenter: 'Personal Center'
  },
  // 登录页面国际化
  login: {
    selectPlaceholder: 'Please select/enter a company name',
    username: 'Username',
    password: 'Password',
    login: 'Login',
    logging: 'Logging...',
    code: 'Verification Code',
    rememberPassword: 'Remember me',
    switchRegisterPage: 'Sign up now',
    rule: {
      tenantId: {
        required: 'Please enter your tenant id'
      },
      username: {
        required: 'Please enter your account'
      },
      password: {
        required: 'Please enter your password'
      },
      code: {
        required: 'Please enter a verification code'
      }
    }
  },
  // 注册页面国际化
  register: {
    selectPlaceholder: 'Please select/enter a company name',
    username: 'Username',
    password: 'Password',
    confirmPassword: 'Confirm Password',
    register: 'Register',
    registering: 'Registering...',
    registerSuccess: 'Congratulations, your {username} account has been registered!',
    code: 'Verification Code',
    switchLoginPage: 'Log in with an existing account',
    rule: {
      tenantId: {
        required: 'Please enter your tenant id'
      },
      username: {
        required: 'Please enter your account',
        length: 'The length of the user account must be between {min} and {max}'
      },
      password: {
        required: 'Please enter your password',
        length: 'The user password must be between {min} and {max} in length',
        pattern: "Can't contain illegal characters: {strings}"
      },
      code: {
        required: 'Please enter a verification code'
      },
      confirmPassword: {
        required: 'Please enter your password again',
        equalToPassword: 'The password entered twice is inconsistent'
      }
    }
  },
  // 导航栏国际化
  navbar: {
    full: 'Full Screen',
    language: 'Language',
    dashboard: 'Dashboard',
    document: 'Document',
    message: 'Message',
    layoutSize: 'Layout Size',
    selectTenant: 'Select Tenant',
    layoutSetting: 'Layout Setting',
    personalCenter: 'Personal Center',
    logout: 'Logout',
    search: 'Search',
    logoutConfirm: 'Are you sure you want to log out?',
    prompt: 'Prompt'
  },
  dashboardHome: {
    title: 'GameLuck Admin',
    subtitle: 'Back-office operations console for platform, tenants, wallets, games, promotions, risk control, and audit workflows.',
    edition: 'Base',
    columns: {
      module: 'Module',
      focus: 'Current Focus',
      status: 'Status'
    },
    metrics: {
      foundation: {
        label: 'Admin Foundation',
        value: 'Ready',
        hint: 'Login, tenants, menus, logs'
      },
      wallet: {
        label: 'Wallet Center',
        value: 'In Planning',
        hint: 'Multi-currency configurable switches'
      },
      game: {
        label: 'Game Integration',
        value: 'Reserved',
        hint: 'Cocos / third-party game bridge'
      },
      risk: {
        label: 'Risk Audit',
        value: 'In Planning',
        hint: 'Approval, audit, restriction rules'
      }
    },
    tasks: {
      platform: {
        module: 'Platform Admin',
        task: 'Keep core permissions and tenant boundaries stable',
        status: 'In Progress'
      },
      wallet: {
        module: 'Wallet Center',
        task: 'Define account, ledger, freeze, deposit, and withdrawal flows',
        status: 'Next'
      },
      game: {
        module: 'Game Integration',
        task: 'Reserve game entry, callback, and session integration capabilities',
        status: 'Reserved'
      },
      frontend: {
        module: 'Frontend Simplification',
        task: 'Remove unrelated links, default branding, and unused entries',
        status: 'In Progress'
      }
    },
    boundary: {
      title: 'Build Boundaries',
      items: [
        'Keep permissions, tenant isolation, audit logs, and data permissions without weakening the admin foundation.',
        'GameLuck business modules connect gradually through wallet, game, promotion, and settlement APIs.',
        'Before adding business pages, remove unrelated entries and keep menus and responsibilities clear.'
      ]
    }
  },
  common: {
    search: 'Search',
    reset: 'Reset',
    add: 'Add',
    delete: 'Delete',
    confirm: 'Confirm',
    cancel: 'Cancel',
    edit: 'Edit',
    detail: 'Detail',
    operation: 'Operation',
    createTime: 'Created At',
    updateTime: 'Updated At',
    remark: 'Remark',
    status: 'Status',
    currency: 'Currency',
    amount: 'Amount',
    success: {
      add: 'Added successfully',
      edit: 'Updated successfully',
      delete: 'Deleted successfully',
      operate: 'Operation successful',
      statusUpdated: 'Status updated'
    }
  },
  memberProfile: {
    fields: {
      memberNo: 'Member ID',
      username: 'Username',
      nickname: 'Nickname',
      riskLevel: 'Risk Level',
      riskReason: 'Risk Reason',
      riskSource: 'Risk Source',
      riskUpdatedTime: 'Risk Updated At',
      kycStatus: 'KYC Status',
      kycReviewReason: 'KYC Note',
      kycReviewedBy: 'KYC Operator',
      kycReviewTime: 'KYC Time',
      registerChannel: 'Register Channel',
      countryCode: 'Country',
      stateCode: 'State',
      countryState: 'Country/State',
      complianceConsent: 'Compliance Consent',
      lastLoginTime: 'Last Login'
    },
    placeholders: {
      memberNo: 'Enter member ID',
      username: 'Enter username',
      nickname: 'Enter nickname',
      status: 'Select status',
      riskLevel: 'Select risk level',
      kycStatus: 'Select KYC status',
      kycReviewReason: 'Enter KYC review note',
      countryCode: 'Country code',
      stateCode: 'State code',
      remark: 'Enter remark'
    },
    status: {
      ACTIVE: 'Active',
      FROZEN: 'Frozen',
      DISABLED: 'Disabled'
    },
    risk: {
      NORMAL: 'Normal',
      WATCH: 'Watch',
      HIGH: 'High Risk'
    },
    consent: {
      age: 'Age',
      terms: 'Terms',
      privacy: 'Privacy',
      rules: 'Rules',
      accepted: 'Accepted',
      notAccepted: 'Not accepted'
    },
    actions: {
      view: 'View detail',
      edit: 'Edit profile',
      setActive: 'Set active',
      freeze: 'Freeze member',
      disable: 'Disable member',
      delete: 'Delete profile'
    },
    dialog: {
      add: 'Add Member',
      edit: 'Edit Member',
      detail: 'Member Detail'
    },
    rules: {
      username: 'Username is required',
      status: 'Status is required',
      riskLevel: 'Risk level is required',
      kycStatus: 'KYC status is required'
    },
    confirm: {
      status: 'Confirm changing member {username} status to {status}?',
      delete: 'Confirm deleting the selected member profiles?'
    }
  },
  promotionReward: {
    fields: {
      promotionNo: 'Promotion No.',
      promotionName: 'Promotion Name',
      promotionType: 'Activity Type',
      rewardItems: 'Reward Items',
      rewardAmount: 'Reward Amount',
      startTime: 'Start Time',
      endTime: 'End Time',
      claimNo: 'Claim No.',
      memberId: 'Member ID',
      claimDate: 'Claim Date',
      rewardSnapshot: 'Issued Reward',
      walletTransactionNo: 'Wallet Transaction No.',
      failReason: 'Failure Reason',
      claimTime: 'Claimed At'
    },
    placeholders: {
      promotionNo: 'Enter promotion no.',
      promotionName: 'Enter promotion name',
      promotionType: 'Select activity type',
      currency: 'Select currency',
      status: 'Select status',
      startTime: 'Leave empty to start immediately',
      endTime: 'Leave empty for no end date',
      remark: 'Enter remark',
      memberId: 'Enter member ID'
    },
    status: {
      ACTIVE: 'Active',
      INACTIVE: 'Inactive',
      SUCCESS: 'Success',
      FAILED: 'Failed'
    },
    types: {
      GENERAL: 'General Reward',
      DAILY_LOGIN: 'Daily Login'
    },
    actions: {
      edit: 'Edit configuration',
      enable: 'Enable promotion',
      disable: 'Disable promotion',
      claim: 'Member claim',
      claims: 'Claim records',
      addRewardItem: 'Add reward item',
      removeRewardItem: 'Remove reward item',
      delete: 'Delete configuration'
    },
    dialog: {
      add: 'Add Promotion Reward',
      edit: 'Edit Promotion Reward',
      claim: 'Member Claim Reward',
      claimRecords: '{name} - Claim Records'
    },
    rules: {
      promotionName: 'Promotion name is required',
      promotionType: 'Activity type is required',
      currency: 'Currency is required',
      rewardAmount: 'Reward amount is required',
      status: 'Status is required',
      memberId: 'Member ID is required'
    },
    confirm: {
      status: 'Confirm {action} this promotion reward?',
      delete: 'Confirm deleting the selected promotion rewards?'
    },
    messages: {
      claimSuccess: 'Claimed successfully',
      rewardItemsRequired: 'At least one reward item is required'
    }
  },
  redemptionOrder: {
    fields: {
      redemptionOrderNo: 'Order No.',
      memberId: 'Member ID',
      redemptionMethod: 'Method',
      accountRef: 'Account Remark',
      freezeNo: 'Freeze No.',
      freezeWalletTransactionNo: 'Freeze Transaction',
      settleWalletTransactionNo: 'Settlement Transaction',
      releaseWalletTransactionNo: 'Release Transaction',
      auditTime: 'Audit Time',
      auditReason: 'Audit Reason',
      failReason: 'Failure Reason'
    },
    placeholders: {
      redemptionOrderNo: 'Enter order no.',
      memberId: 'Enter member ID',
      currency: 'Select currency',
      status: 'Select status',
      redemptionMethod: 'Default SIMULATED',
      accountRef: 'Enter simulated or masked account information only',
      remark: 'Enter remark',
      auditReason: 'Enter audit reason'
    },
    status: {
      PENDING: 'Pending',
      APPROVED: 'Approved',
      REJECTED: 'Rejected',
      FAILED: 'Failed'
    },
    filters: {
      pending: 'Pending',
      approved: 'Approved',
      rejected: 'Rejected',
      failed: 'Failed',
      all: 'All'
    },
    actions: {
      view: 'View detail',
      approve: 'Approve',
      reject: 'Reject'
    },
    dialog: {
      add: 'Add Simulated Redemption Order',
      approve: 'Approve',
      reject: 'Reject',
      detail: 'Redemption Order Detail'
    },
    rules: {
      memberId: 'Member ID is required',
      currency: 'Currency is required',
      amount: 'Amount is required',
      rejectReason: 'A rejection reason is required'
    },
    confirm: {
      audit: 'Confirm {action} this redemption order?'
    }
  },
  paymentSession: {
    title: 'Payment Sessions',
    empty: 'No payment sessions match the filters',
    fields: {
      sessionNo: 'Platform Session No.',
      purchaseOrderNo: 'Purchase Order No.',
      providerSessionNo: 'Provider Session No.',
      member: 'Member',
      providerCode: 'Provider',
      amount: 'Amount',
      createdRange: 'Created Range',
      expireTime: 'Expires At',
      completedTime: 'Completed At',
      checkoutUrl: 'Checkout URL'
    },
    range: { to: 'to' },
    actions: { retryLoad: 'Reload' },
    detail: { title: 'Payment Session Detail' },
    messages: {
      loadFailed: 'Failed to load payment sessions',
      detailFailed: 'Failed to load payment session detail',
      permissionDenied: 'This account cannot query payment sessions'
    }
  },
  paymentWebhookEvent: {
    title: 'Payment Webhook Events',
    empty: 'No webhook events match the filters',
    fields: {
      providerEventId: 'Provider Event ID',
      purchaseOrderNo: 'Purchase Order No.',
      sessionNo: 'Platform Session No.',
      providerSessionNo: 'Provider Session No.',
      eventType: 'Event Type',
      providerCode: 'Provider',
      receivedRange: 'Received Range',
      receivedTime: 'Received At',
      attempts: 'Attempts',
      failureReason: 'Failure Reason',
      lastProcessingTime: 'Last Processed At',
      linkedReversal: 'Linked Reversal'
    },
    range: { to: 'to' },
    actions: { retryLoad: 'Reload', retry: 'Retry Processing', openReversal: 'Open Reversal Review List' },
    detail: { title: 'Payment Webhook Event Detail', signatureDigest: 'Signature Digest (Read-only)', rawPayload: 'Raw Payload (Read-only)' },
    retry: { confirm: 'Retry event {eventId}? This reprocesses only the immutable original event and writes an operation log.' },
    messages: {
      loadFailed: 'Failed to load webhook events',
      detailFailed: 'Failed to load event detail',
      permissionDenied: 'This account cannot query payment webhook events',
      retrySuccess: 'Webhook event retry completed',
      retryFailed: 'Webhook event retry failed',
      rawFormatFallback: 'The raw payload is not valid JSON and is shown read-only as received'
    }
  },
  purchaseReversalReview: {
    empty: 'No reversal review cases match the filters',
    fields: {
      reversalNo: 'Reversal No.',
      purchaseOrderNo: 'Purchase Order No.',
      member: 'Member',
      reversalType: 'Reversal Type',
      shortfall: 'Shortfall By Currency',
      riskLevel: 'Risk Level',
      waiting: 'Waiting',
      dispositionStatus: 'Disposition',
      createTime: 'Created At',
      orderStatus: 'Order Status',
      retryCount: 'Retry Count',
      reason: 'Reason',
      reviewReason: 'Review Reason',
      currency: 'Currency',
      required: 'Required',
      available: 'Available',
      recovered: 'Recovered',
      walletTransactionNo: 'Wallet Transaction No.',
      riskReason: 'Risk Reason',
      grantType: 'Grant Type',
      amount: 'Amount',
      fundProperty: 'Fund Property',
      turnoverTaskNo: 'Turnover Task No.',
      eventKey: 'Event Key',
      eventType: 'Event Type',
      eventStatus: 'Event Status',
      operationType: 'Operation',
      operator: 'Operator',
      reviewNote: 'Review Note'
    },
    placeholders: {
      reversalNo: 'Enter reversal no.',
      purchaseOrderNo: 'Enter purchase order no.',
      member: 'Enter member no.',
      reversalType: 'Select reversal type'
    },
    filters: { pending: 'Pending Review', recovered: 'Recovered', loss: 'Loss Accepted' },
    actions: { detail: 'View Review Detail', retry: 'Retry Full Recovery', acceptLoss: 'Accept Loss' },
    detail: {
      title: 'Chargeback Review Detail',
      caseAndOrder: 'Case And Order',
      recoveryItems: 'Recovery By Currency',
      memberRisk: 'Member Risk',
      grants: 'Grant Snapshots',
      paymentEvents: 'Payment Events',
      history: 'Operation History',
      noHistory: 'No review operations'
    },
    duration: { minutes: '{count} min', hours: '{count} hr', days: '{count} days' },
    retry: { confirm: 'Retry full recovery for every currency? No debit occurs if any currency is insufficient.' },
    loss: {
      title: 'Accept Loss By Currency',
      notePlaceholder: 'Enter the basis for accepting the loss',
      noteRequired: 'A review note is required',
      confirm: 'Accept Loss And Close',
      secondConfirm: 'Close the case with the shortfalls shown above? This does not debit wallets or cancel turnover tasks.'
    },
    messages: {
      retrySuccess: 'Every currency was recovered and the case is closed',
      retryInsufficient: 'Balance is still insufficient; no debit occurred',
      lossAccepted: 'Currency-level losses accepted and case closed'
    }
  },
  reportOverview: {
    title: 'Report Overview',
    subtitle: 'Real-time MVP metrics from member, wallet, payment, game, promotion, and redemption modules.',
    refresh: 'Refresh',
    empty: 'No report data returned',
    sections: {
      walletPayment: 'Wallet And Payment',
      gamePromotion: 'Game And Promotion',
      redemptionReview: 'Redemption Review'
    },
    columns: {
      metric: 'Metric',
      value: 'Value',
      state: 'State',
      meaning: 'Operational Meaning'
    },
    cards: {
      members: 'Members',
      registeredProfiles: 'Registered profiles',
      walletAccounts: 'Wallet Accounts',
      currencyAccounts: 'Currency accounts',
      depositAmount: 'Deposit Amount',
      successfulDeposits: 'Successful deposits',
      gameNet: 'Game Net',
      payoutMinusBet: 'Payout minus bet',
      rewards: 'Rewards',
      successfulClaims: 'Successful claims',
      pendingRedeem: 'Pending Redeem',
      needsReview: 'Needs review'
    },
    metrics: {
      walletAvailableTotal: 'Wallet available total',
      walletFrozenTotal: 'Wallet frozen total',
      depositOrders: 'Deposit orders',
      successfulDepositAmount: 'Successful deposit amount',
      gameOrders: 'Game orders',
      totalBetAmount: 'Total bet amount',
      totalPayoutAmount: 'Total payout amount',
      promotionClaims: 'Promotion claims',
      successfulRewardAmount: 'Successful reward amount',
      redemptionOrders: 'Redemption orders',
      pendingReview: 'Pending review',
      approved: 'Approved',
      rejected: 'Rejected',
      approvedAmount: 'Approved amount'
    },
    states: {
      available: 'Available',
      frozen: 'Frozen',
      clear: 'Clear',
      orders: 'Orders',
      credited: 'Credited',
      debit: 'Debit',
      credit: 'Credit',
      claims: 'Claims',
      action: 'Action',
      settled: 'Settled',
      released: 'Released',
      amount: 'Amount'
    },
    meanings: {
      redemptionOrders: 'All submitted redemption requests.',
      pendingReview: 'Frozen funds that still need an operator decision.',
      approved: 'Requests settled from frozen wallet balance.',
      rejected: 'Requests rejected and released back to available balance.',
      approvedAmount: 'Total amount approved for redemption.'
    },
    messages: {
      loadFailed: 'Failed to load report overview'
    }
  },
  paymentReconciliation: {
    title: 'Payment Reconciliation',
    provider: 'Provider',
    date: 'Statement date',
    file: 'File name',
    counts: 'Total / invalid / issues',
    creator: 'Created by',
    created: 'Created time',
    upload: 'Upload statement',
    execute: 'Run reconciliation',
    detail: 'Batch details',
    issueDetail: 'Issue details',
    invalid: 'Invalid lines',
    matched: 'Matched lines',
    issues: 'Issues',
    resolve: 'Resolve',
    ignore: 'Ignore',
    retry: 'Retry',
    empty: 'No reconciliation batches',
    filteredEmpty: 'No results match the current filters',
    permissionDenied: 'You do not have permission to view reconciliation batches',
    loadFailed: 'Failed to load reconciliation batches',
    uploadFailed: 'Statement upload failed',
    uploaded: 'Statement uploaded and validated',
    csvOnly: 'Only CSV files are supported',
    tooLarge: 'File must not exceed 10 MiB',
    failed: 'Reconciliation failed',
    reconciling: 'Reconciliation is running; actions are temporarily unavailable',
    completed: 'Reconciliation completed',
    resolved: 'Reconciliation conclusion recorded',
    conflict: 'This record was updated by another operator. Refresh and try again.',
    issueChanged: 'The selected issue changed. Reopen it before submitting.',
    executeConfirm: 'Run reconciliation for this batch? A batch containing invalid lines cannot run.',
    noMutation: 'This records the reconciliation conclusion only. It does not change payment status, reversals, or wallet balances.',
    invalidExecuteBlocked: 'This batch contains invalid lines. Correct the source file and upload a new batch before running reconciliation.',
    detailFailed: 'Failed to load batch details',
    tabLoadFailed: 'Failed to load the current page',
    issueLoadFailed: 'Failed to load issue details',
    emptyLines: 'No line records',
    emptyIssues: 'No reconciliation issues',
    issueType: 'Issue type',
    orderNo: 'Order number',
    sessionNo: 'Session number',
    providerRecord: 'Provider record ID',
    rowNumber: 'Source row',
    amount: 'Amount',
    validation: 'Validation result',
    validationPassed: 'Valid',
    duplicateProviderRecord: 'Duplicate provider record ID',
    invalidEventType: 'Invalid event type',
    invalidAmount: 'Invalid amount',
    invalidCurrency: 'Invalid currency',
    invalidTimestamp: 'Invalid timestamp',
    actionUpload: 'Upload',
    actionValidate: 'Validate',
    actionExecute: 'Run reconciliation',
    actionFail: 'Execution failed',
    actionResolve: 'Resolve',
    actionIgnore: 'Ignore',
    providerPlatformAmount: 'Provider / platform amount',
    providerPlatformStatus: 'Provider / platform status',
    related: 'Related records',
    reversal: 'Reversal review',
    diagnostics: 'Diagnostic snapshot',
    canonicalFields: 'Canonical original fields',
    platformOnly: 'This is a platform-only issue with no corresponding provider source line.',
    classification: 'Classification',
    remark: 'Remark',
    providerRequired: 'Select a payment provider',
    dateRequired: 'Select a statement date',
    fileRequired: 'Select a CSV file',
    classificationRequired: 'Select a conclusion classification',
    remarkRequired: 'Enter a remark',
    executeFailed: 'Failed to run reconciliation. Try again.',
    resolutionFailed: 'Failed to record the reconciliation conclusion. Try again.'
  },
  paymentSettlement: {
    title: 'Payment Settlement', create: 'Create settlement batch', detail: 'Settlement details', number: 'Settlement number', provider: 'Provider', currency: 'Currency', window: 'UTC settlement window', gross: 'Gross payment', refunds: 'Refunds', chargebacks: 'Chargebacks', fees: 'Fees', net: 'Net settlement', payable: 'Payable to provider', receivable: 'Receivable from provider', calculate: 'Calculate', close: 'Close settlement', events: 'events', items: 'Event items', evidence: 'Reconciliation evidence', history: 'Action history', eventType: 'Event type', orderNo: 'Order number', amount: 'Amount', feePercent: 'Payment fee rate', fixedFee: 'Payment fixed fee', chargebackFee: 'Chargeback fixed fee', remark: 'Close remark', openReconciliation: 'Open payment reconciliation', noMutation: 'Closing a settlement records financial confirmation only and does not change payment orders, reversals, or wallet balances', permissionDenied: 'You do not have permission to view payment settlements', loadFailed: 'Failed to load payment settlements', detailFailed: 'Failed to load settlement details', retry: 'Retry', empty: 'No settlement batches', filteredEmpty: 'No settlement batches match the filters', emptyItems: 'No settlement events', emptyHistory: 'No action history', noEvidence: 'No reconciliation evidence', processing: 'Settlement calculation is in progress. Refresh later.', failed: 'Settlement calculation failed', providerRequired: 'Enter a payment provider', currencyRequired: 'Enter a three-letter currency code', windowRequired: 'Select a UTC settlement window', windowInvalid: 'The settlement window must be greater than zero and no longer than 31 days', remarkRequired: 'Enter a close remark', calculateConfirm: 'This creates immutable financial snapshots from processed payment events. Continue?', calculateAccepted: 'Settlement calculation completed', closed: 'Settlement batch closed', status: { CREATED: 'Created', CALCULATING: 'Calculating', CALCULATED: 'Calculated', CLOSED: 'Closed', FAILED: 'Failed' }
  },
  paymentSettlementReport: {
    title: 'Settlement Report',
    startDate: 'UTC start date',
    endDate: 'UTC end date',
    reportDate: 'UTC report date',
    provider: 'Provider',
    currency: 'Currency',
    quickRange: 'Date range',
    latest7: 'Latest 7 days',
    latest31: 'Latest 31 days',
    export: 'Export CSV',
    exportDenied: 'You do not have permission to export this report',
    currencyTotals: 'Totals by currency',
    batches: 'batches',
    batchCount: 'Batches',
    eventCount: 'Events',
    payments: 'Payments',
    refunds: 'Refunds',
    chargebacks: 'Chargebacks',
    gross: 'Gross payment',
    refundAmount: 'Refund amount',
    chargebackAmount: 'Chargeback amount',
    fees: 'Fees',
    netSettlement: 'Net settlement',
    negativeNet: 'Negative net',
    nonNegativeNet: 'Non-negative net',
    latestClose: 'Latest close time',
    generatedAt: 'Generated at',
    sourceBatches: 'Source settlement batches',
    settlementNo: 'Settlement number',
    periodStart: 'Period start',
    periodEnd: 'Period end',
    permissionDenied: 'You do not have permission to view settlement reports',
    exportInProgress: 'Preparing export',
    loadFailed: 'Failed to load settlement report',
    batchLoadFailed: 'Failed to load source settlement batches',
    retry: 'Retry',
    empty: 'No closed settlement report rows',
    filteredEmpty: 'No report rows match the current filters',
    emptyBatches: 'No source settlement batches',
    dateInvalid: 'Select a valid UTC range of up to 31 days that does not extend into the future'
  },
  reportTrends: {
    title: 'Daily Trends',
    subtitle: 'Recent all-business operating trends by date.',
    refresh: 'Refresh',
    empty: 'No trend data returned',
    range: {
      seven: 'Last 7 days',
      thirty: 'Last 30 days'
    },
    cards: {
      depositAmount: 'Deposit Amount',
      gameNet: 'Game Net',
      rewards: 'Rewards',
      approvedRedeem: 'Approved Redeem',
      pendingRedeem: 'Pending Redeem'
    },
    columns: {
      date: 'Date',
      members: 'New Members',
      depositOrders: 'Deposit Orders',
      depositAmount: 'Deposit Amount',
      gameOrders: 'Game Orders',
      betAmount: 'Bet Amount',
      payoutAmount: 'Payout Amount',
      gameNet: 'Game Net',
      promotionClaims: 'Claims',
      rewardAmount: 'Reward Amount',
      redemptionOrders: 'Redeem Orders',
      pendingRedeem: 'Pending',
      approvedRedeemAmount: 'Approved Amount'
    },
    messages: {
      loadFailed: 'Failed to load daily trends'
    }
  }
};
