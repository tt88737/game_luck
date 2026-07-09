export const playerState = {
  isLoggedIn: false,
  kycStatus: '待完成',
  riskStatus: '正常',
  cooldownMinutes: 0,
}

export const balances = [
  { code: 'SC', name: 'Sweep 币', available: '108.000000', locked: '20.000000', status: '可用' },
  { code: 'GC', name: '金币', available: '12500.000000', locked: '0.000000', status: '可用' },
  { code: 'RC', name: '奖励积分', available: '18.500000', locked: '0.000000', status: '仅限活动' },
]

export const ledgerRows = [
  { time: '20:18', type: '活动奖励', amount: '+8.000000 SC', status: '成功' },
  { time: '19:42', type: '游戏投注', amount: '-10.000000 SC', status: '已结算' },
  { time: '19:46', type: '游戏退款', amount: '+10.000000 SC', status: '成功' },
]

export const games = [
  { code: 'SIM-SLOT', name: '模拟老虎机', currency: 'GC', state: '开放中', minBet: '100 GC' },
  { code: 'SIM-WHEEL', name: '奖励转盘', currency: 'SC', state: '余额不足', minBet: '150 SC' },
  { code: 'SIM-TABLE', name: '桌面冲刺', currency: 'SC', state: '维护中', minBet: '10 SC' },
]

export const promotions = [
  { name: '每日 SC 奖励', amount: '8.000000 SC', state: '可领取' },
  { name: '首局游戏奖励', amount: '500.000000 GC', state: '已领取' },
  { name: 'KYC 奖励', amount: '20.000000 RC', state: '需要 KYC' },
]

export const redemptions = [
  { no: 'RD-LOCAL-1001', amount: '20.000000 SC', status: '待审核' },
  { no: 'RD-LOCAL-1000', amount: '5.000000 SC', status: '已释放' },
]
