export type BusinessLabelCategory =
  | 'sourceType'
  | 'method'
  | 'walletOperation'
  | 'walletTransactionStatus'
  | 'walletReleaseMode'
  | 'walletReleaseStatus'
  | 'walletFreezeStatus'
  | 'depositStatus'
  | 'gameBetStatus'
  | 'redemptionStatus'
  | 'normalStatus';

export type BusinessLabelTranslator = (text: string) => string;

type LabelItem = {
  value: string;
  label: string;
};

const labelGroups: Record<BusinessLabelCategory, LabelItem[]> = {
  sourceType: [
    { value: 'REGISTER_BONUS', label: '\u6ce8\u518c\u8d60\u9001' },
    { value: 'DAILY_REWARD', label: '\u6bcf\u65e5\u5956\u52b1' },
    { value: 'TASK_REWARD', label: '\u4efb\u52a1\u5956\u52b1' },
    { value: 'DEPOSIT', label: '\u5145\u503c' },
    { value: 'GAME_BET', label: '\u6e38\u620f\u4e0b\u6ce8' },
    { value: 'GAME_PAYOUT', label: '\u6e38\u620f\u6d3e\u5f69' },
    { value: 'GAME_PROFIT', label: '\u6e38\u620f\u6d3e\u5f69' },
    { value: 'GAME_REFUND', label: '\u6e38\u620f\u9000\u6b3e' },
    { value: 'GAME_SETTLE', label: '\u6e38\u620f\u7ed3\u7b97' },
    { value: 'REDEMPTION', label: '\u5151\u6362' },
    { value: 'PROMOTION', label: '\u6d3b\u52a8\u5956\u52b1' },
    { value: 'ADJUST', label: '\u4eba\u5de5\u8c03\u6574' },
    { value: 'ADJUSTMENT', label: '\u4eba\u5de5\u8c03\u8d26' },
    { value: 'MANUAL_ADJUST', label: '\u4eba\u5de5\u8c03\u8d26' },
    { value: 'TURNOVER', label: '\u6d41\u6c34' }
  ],
  method: [{ value: 'SIMULATED', label: '\u5e73\u53f0\u6a21\u62df' }],
  walletOperation: [
    { value: 'CREDIT', label: '\u5165\u8d26' },
    { value: 'DEBIT', label: '\u51fa\u8d26' },
    { value: 'FREEZE', label: '\u51bb\u7ed3' },
    { value: 'UNFREEZE', label: '\u89e3\u51bb' },
    { value: 'SETTLE', label: '\u7ed3\u7b97' },
    { value: 'ADJUST', label: '\u8c03\u6574' },
    { value: 'REVERSE', label: '\u51b2\u6b63' },
    { value: 'TURNOVER', label: '\u6d41\u6c34\u7d2f\u8ba1' }
  ],
  walletTransactionStatus: [
    { value: 'PENDING', label: '\u5904\u7406\u4e2d' },
    { value: 'SUCCESS', label: '\u6210\u529f' },
    { value: 'FAILED', label: '\u5931\u8d25' },
    { value: 'REVERSED', label: '\u5df2\u51b2\u6b63' }
  ],
  walletReleaseMode: [
    { value: 'IMMEDIATE', label: '\u7acb\u5373\u91ca\u653e' },
    { value: 'AFTER_TURNOVER', label: '\u6ee1\u8db3\u6d41\u6c34\u540e\u91ca\u653e' },
    { value: 'NEVER', label: '\u4e0d\u91ca\u653e' },
    { value: 'MANUAL_REVIEW', label: '\u4eba\u5de5\u5ba1\u6838' }
  ],
  walletReleaseStatus: [
    { value: 'RELEASED', label: '\u5df2\u91ca\u653e' },
    { value: 'LOCKED', label: '\u9501\u5b9a\u4e2d' },
    { value: 'NEVER', label: '\u4e0d\u91ca\u653e' },
    { value: 'REVIEWING', label: '\u5f85\u4eba\u5de5\u5ba1\u6838' },
    { value: 'REJECTED', label: '\u5ba1\u6838\u62d2\u7edd' },
    { value: 'CONSUMED', label: '\u5df2\u6d88\u8017' }
  ],
  walletFreezeStatus: [
    { value: 'FROZEN', label: '\u51bb\u7ed3\u4e2d' },
    { value: 'SETTLED', label: '\u5df2\u7ed3\u7b97' },
    { value: 'RELEASED', label: '\u5df2\u91ca\u653e' }
  ],
  depositStatus: [
    { value: 'PENDING', label: '\u5f85\u652f\u4ed8' },
    { value: 'SUCCESS', label: '\u6210\u529f' },
    { value: 'CANCELLED', label: '\u5df2\u53d6\u6d88' },
    { value: 'FAILED', label: '\u5931\u8d25' }
  ],
  gameBetStatus: [
    { value: 'PENDING', label: '\u5f85\u4e0b\u6ce8' },
    { value: 'BET_SUCCESS', label: '\u5df2\u6263\u6b3e' },
    { value: 'BET_FAILED', label: '\u6263\u6b3e\u5931\u8d25' },
    { value: 'SETTLED', label: '\u5df2\u7ed3\u7b97' },
    { value: 'SETTLE_FAILED', label: '\u7ed3\u7b97\u5931\u8d25' },
    { value: 'CANCELLED', label: '\u5df2\u53d6\u6d88' }
  ],
  redemptionStatus: [
    { value: 'PENDING', label: '\u5f85\u5ba1\u6838' },
    { value: 'APPROVED', label: '\u5df2\u901a\u8fc7' },
    { value: 'REJECTED', label: '\u5df2\u62d2\u7edd' },
    { value: 'FAILED', label: '\u5931\u8d25' }
  ],
  normalStatus: [
    { value: '0', label: '\u542f\u7528' },
    { value: '1', label: '\u505c\u7528' }
  ]
};

const optionCache = new Map<string, LabelItem[]>();

export const businessOptions = (category: BusinessLabelCategory, translate?: BusinessLabelTranslator) => {
  const cacheKey = `${category}:${translate ? 'translated' : 'raw'}`;
  if (!translate && optionCache.has(cacheKey)) {
    return optionCache.get(cacheKey)!;
  }
  const options = (labelGroups[category] || []).map((item) => ({
    label: translate ? translate(item.label) : item.label,
    value: item.value
  }));
  if (!translate) {
    optionCache.set(cacheKey, options);
  }
  return options;
};

export const businessLabel = (category: BusinessLabelCategory, value?: string | number, translate?: BusinessLabelTranslator) => {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  const rawValue = String(value);
  const item = labelGroups[category]?.find((option) => option.value === rawValue);
  const label = item?.label || rawValue;
  return translate ? translate(label) : label;
};

const seededWalletRuleNames = new Set([
  'GC game profit',
  'SC game profit',
  'SC promotion',
  'RC deposit',
  'RC manual adjustment',
  'SC game refund',
  'GC registration bonus',
  'SC registration bonus'
]);

export const walletRuleNameLabel = (
  ruleName?: string,
  sourceType?: string,
  currencyCode?: string,
  translate?: BusinessLabelTranslator
) => {
  if (!ruleName) {
    return '';
  }
  if (!seededWalletRuleNames.has(ruleName)) {
    return ruleName;
  }
  const sourceLabel = businessLabel('sourceType', sourceType, translate);
  return [currencyCode, sourceLabel].filter(Boolean).join(' ');
};

export const walletStatusType = (status?: string) => {
  if (status === 'SUCCESS' || status === 'RELEASED' || status === 'SETTLED') return 'success';
  if (status === 'FAILED' || status === 'REJECTED') return 'danger';
  if (status === 'PENDING' || status === 'LOCKED' || status === 'REVIEWING' || status === 'FROZEN') return 'warning';
  return 'info';
};

export const depositStatusType = (status?: string) => {
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'PENDING') return 'warning';
  if (status === 'CANCELLED') return 'info';
  return '';
};

export const gameBetStatusType = (status?: string) => {
  if (status === 'SETTLED') return 'success';
  if (status === 'BET_SUCCESS') return 'primary';
  if (status === 'BET_FAILED' || status === 'SETTLE_FAILED') return 'danger';
  if (status === 'PENDING') return 'warning';
  if (status === 'CANCELLED') return 'info';
  return '';
};

export const redemptionStatusType = (status?: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'PENDING') return 'warning';
  if (status === 'REJECTED') return 'info';
  return '';
};
