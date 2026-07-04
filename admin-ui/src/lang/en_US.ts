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
    reportCenter: 'Report Center',
    reportOverview: 'Overview',
    reportOverviewQuery: 'Report Overview Query',
    memberCenter: 'Member Center',
    memberProfiles: 'Member Profiles',
    promotionCenter: 'Promotion Center',
    promotionRewards: 'Promotion Rewards',
    redemptionCenter: 'Redemption Center',
    redemptionOrders: 'Redemption Orders'
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
    },
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
    logout: 'Logout'
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
      memberNo: 'Member No.',
      username: 'Username',
      nickname: 'Nickname',
      riskLevel: 'Risk Level',
      registerChannel: 'Register Channel',
      lastLoginTime: 'Last Login'
    },
    placeholders: {
      memberNo: 'Enter member no.',
      username: 'Enter username',
      nickname: 'Enter nickname',
      status: 'Select status',
      riskLevel: 'Select risk level',
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
      riskLevel: 'Risk level is required'
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
      rewardAmount: 'Reward Amount',
      startTime: 'Start Time',
      endTime: 'End Time',
      claimNo: 'Claim No.',
      memberId: 'Member ID',
      walletTransactionNo: 'Wallet Transaction No.',
      failReason: 'Failure Reason',
      claimTime: 'Claimed At'
    },
    placeholders: {
      promotionNo: 'Enter promotion no.',
      promotionName: 'Enter promotion name',
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
    actions: {
      edit: 'Edit configuration',
      enable: 'Enable promotion',
      disable: 'Disable promotion',
      claim: 'Member claim',
      claims: 'Claim records',
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
      claimSuccess: 'Claimed successfully'
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
      amount: 'Amount is required'
    },
    confirm: {
      audit: 'Confirm {action} this redemption order?'
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
  }
};
