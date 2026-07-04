import i18n from '@/lang';

const titleKeyMap: Record<string, string> = {
  首页: 'route.dashboard',
  Dashboard: 'route.dashboard',
  系统管理: 'route.systemManagement',
  'System Management': 'route.systemManagement',
  租户管理: 'route.tenantManagement',
  'Tenant Management': 'route.tenantManagement',
  系统监控: 'route.systemMonitor',
  'System Monitor': 'route.systemMonitor',
  系统工具: 'route.systemTools',
  'System Tools': 'route.systemTools',
  钱包中心: 'route.walletCenter',
  'Wallet Center': 'route.walletCenter',
  支付中心: 'route.paymentCenter',
  'Payment Center': 'route.paymentCenter',
  游戏交易: 'route.gameTrading',
  'Game Trading': 'route.gameTrading',
  'Report Center': 'route.reportCenter',
  报表中心: 'route.reportCenter',
  Overview: 'route.reportOverview',
  数据总览: 'route.reportOverview',
  'Report Overview Query': 'route.reportOverviewQuery',
  报表总览查询: 'route.reportOverviewQuery',
  'Member Center': 'route.memberCenter',
  会员中心: 'route.memberCenter',
  'Member Profiles': 'route.memberProfiles',
  会员资料: 'route.memberProfiles',
  'Promotion Center': 'route.promotionCenter',
  促销中心: 'route.promotionCenter',
  'Promotion Rewards': 'route.promotionRewards',
  促销奖励: 'route.promotionRewards',
  'Redemption Center': 'route.redemptionCenter',
  兑换中心: 'route.redemptionCenter',
  'Redemption Orders': 'route.redemptionOrders',
  兑换订单: 'route.redemptionOrders'
};

export const translateTitle = (title?: string) => {
  if (!title) {
    return '';
  }
  const key = titleKeyMap[title];
  return key ? i18n.global.t(key) : title;
};
