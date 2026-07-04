export default {
  // 路由国际化
  route: {
    dashboard: 'Dashboard',
    document: 'Document',
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
