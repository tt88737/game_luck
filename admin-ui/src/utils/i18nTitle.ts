import i18n from '@/lang';

const titleKeyMap: Record<string, string> = {
  首页: 'route.dashboard',
  Dashboard: 'route.dashboard',
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
