export const playerState = {
  isLoggedIn: false,
  kycStatus: 'Required',
  riskStatus: 'Normal',
  cooldownMinutes: 0,
}

export const balances = [
  { code: 'SC', name: 'Sweeps Coins', available: '108.000000', locked: '20.000000', status: 'Available' },
  { code: 'GC', name: 'Gold Coins', available: '12500.000000', locked: '0.000000', status: 'Available' },
  { code: 'RC', name: 'Reward Credits', available: '18.500000', locked: '0.000000', status: 'Promotion only' },
]

export const ledgerRows = [
  { time: '20:18', type: 'Promotion', amount: '+8.000000 SC', status: 'Success' },
  { time: '19:42', type: 'Game Bet', amount: '-10.000000 SC', status: 'Settled' },
  { time: '19:46', type: 'Game Refund', amount: '+10.000000 SC', status: 'Success' },
]

export const games = [
  { code: 'SIM-SLOT', name: 'Simulated Slot', currency: 'GC', state: 'Open', minBet: '100 GC' },
  { code: 'SIM-WHEEL', name: 'Reward Wheel', currency: 'SC', state: 'Insufficient balance', minBet: '150 SC' },
  { code: 'SIM-TABLE', name: 'Table Sprint', currency: 'SC', state: 'Maintenance', minBet: '10 SC' },
]

export const promotions = [
  { name: 'Daily SC Reward', amount: '8.000000 SC', state: 'Claim available' },
  { name: 'First Game Bonus', amount: '500.000000 GC', state: 'Already claimed' },
  { name: 'KYC Reward', amount: '20.000000 RC', state: 'KYC required' },
]

export const redemptions = [
  { no: 'RD-LOCAL-1001', amount: '20.000000 SC', status: 'Pending review' },
  { no: 'RD-LOCAL-1000', amount: '5.000000 SC', status: 'Released' },
]
