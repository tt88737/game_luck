export type BusinessLabelCategory =
  | 'sourceType'
  | 'method'
  | 'walletOperation'
  | 'walletTransactionStatus'
  | 'walletExchangeOrderStatus'
  | 'walletReleaseMode'
  | 'walletReleaseStatus'
  | 'walletFreezeStatus'
  | 'depositStatus'
  | 'purchaseOrderStatus'
  | 'purchaseReversalType'
  | 'purchaseReversalStatus'
  | 'purchaseReversalDispositionStatus'
  | 'purchaseReversalReviewOperationType'
  | 'purchasePaymentEventType'
  | 'purchasePaymentEventStatus'
  | 'paymentSessionStatus'
  | 'paymentWebhookEventType'
  | 'paymentWebhookStatus'
  | 'reconciliationBatchStatus'
  | 'reconciliationLineStatus'
  | 'reconciliationIssueStatus'
  | 'reconciliationIssueType'
  | 'reconciliationResolutionType'
  | 'gameBetStatus'
  | 'redemptionStatus'
  | 'normalStatus';

export type BusinessLabelTranslator = (text: string) => string;

type LabelItem = {
  value: string;
  label: string;
};

const standardSourceTypeLabels: LabelItem[] = [
  { value: 'REGISTER_BONUS', label: '\u6ce8\u518c\u8d60\u9001' },
  { value: 'DAILY_REWARD', label: '\u6bcf\u65e5\u5956\u52b1' },
  { value: 'TASK_REWARD', label: '\u4efb\u52a1\u5956\u52b1' },
  { value: 'DEPOSIT', label: '\u5145\u503c' },
  { value: 'GAME_BET', label: '\u6e38\u620f\u4e0b\u6ce8' },
  { value: 'GAME_PROFIT', label: '\u6e38\u620f\u6d3e\u5956' },
  { value: 'GAME_REFUND', label: '\u6e38\u620f\u9000\u6b3e' },
  { value: 'GAME_SETTLE', label: '\u6e38\u620f\u7ed3\u7b97' },
  { value: 'REDEMPTION', label: '\u5151\u6362' },
  { value: 'EXCHANGE', label: '\u5e01\u79cd\u5151\u6362' },
  { value: 'PROMOTION', label: '\u6d3b\u52a8\u5956\u52b1' },
  { value: 'MANUAL_ADJUST', label: '\u4eba\u5de5\u8c03\u8d26' },
  { value: 'TURNOVER', label: '\u6d41\u6c34' }
];

const labelGroups: Record<BusinessLabelCategory, LabelItem[]> = {
  sourceType: standardSourceTypeLabels,
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
  walletExchangeOrderStatus: [
    { value: 'PENDING', label: '\u5f85\u5904\u7406' },
    { value: 'SUCCESS', label: '\u6210\u529f' },
    { value: 'FAILED', label: '\u5931\u8d25' },
    { value: 'CANCELLED', label: '\u5df2\u53d6\u6d88' }
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
  purchaseOrderStatus: [
    { value: 'CREATED', label: '\u5df2\u521b\u5efa' },
    { value: 'PENDING', label: '\u5f85\u652f\u4ed8' },
    { value: 'PAID', label: '\u5df2\u652f\u4ed8' },
    { value: 'CREDITED', label: '\u5df2\u5165\u8d26' },
    { value: 'FAILED', label: '\u5931\u8d25' },
    { value: 'CANCELLED', label: '\u5df2\u53d6\u6d88' },
    { value: 'REFUNDED', label: '\u5df2\u9000\u6b3e' },
    { value: 'CHARGEBACK', label: '\u62d2\u4ed8' },
    { value: 'REFUND_REVIEW', label: '\u9000\u6b3e\u5f85\u590d\u6838' },
    { value: 'CHARGEBACK_REVIEW', label: '\u62d2\u4ed8\u5f85\u590d\u6838' }
  ],
  purchaseReversalType: [
    { value: 'REFUND', label: '\u9000\u6b3e\u8ffd\u507f' },
    { value: 'CHARGEBACK', label: '\u62d2\u4ed8\u8ffd\u507f' }
  ],
  purchaseReversalStatus: [
    { value: 'PROCESSING', label: '\u5904\u7406\u4e2d' },
    { value: 'COMPLETED', label: '\u5df2\u5b8c\u6210' },
    { value: 'REVIEW_REQUIRED', label: '\u9700\u4eba\u5de5\u590d\u6838' }
  ],
  purchaseReversalDispositionStatus: [
    { value: 'PENDING_REVIEW', label: '\u5f85\u590d\u6838' },
    { value: 'RECOVERY_COMPLETED', label: '\u5df2\u8ffd\u56de' },
    { value: 'LOSS_ACCEPTED', label: '\u5df2\u786e\u8ba4\u635f\u5931' }
  ],
  purchaseReversalReviewOperationType: [
    { value: 'RETRY_INSUFFICIENT', label: '\u91cd\u8bd5\u4ecd\u4e0d\u8db3' },
    { value: 'RETRY_COMPLETED', label: '\u91cd\u8bd5\u5df2\u8ffd\u56de' },
    { value: 'LOSS_ACCEPTED', label: '\u786e\u8ba4\u635f\u5931' }
  ],
  purchasePaymentEventType: [
    { value: 'PAY_SUCCESS', label: '\u652f\u4ed8\u6210\u529f' },
    { value: 'PAY_FAILED', label: '\u652f\u4ed8\u5931\u8d25' },
    { value: 'CANCELLED', label: '\u5df2\u53d6\u6d88' },
    { value: 'REFUNDED', label: '\u5df2\u9000\u6b3e' },
    { value: 'CHARGEBACK', label: '\u62d2\u4ed8' }
  ],
  purchasePaymentEventStatus: [
    { value: 'RECEIVED', label: '\u5df2\u63a5\u6536' },
    { value: 'PROCESSED', label: '\u5df2\u5904\u7406' },
    { value: 'IGNORED', label: '\u5df2\u5ffd\u7565' },
    { value: 'FAILED', label: '\u5931\u8d25' }
  ],
  paymentSessionStatus: [
    { value: 'CREATED', label: '\u5df2\u521b\u5efa' },
    { value: 'PENDING', label: '\u5f85\u652f\u4ed8' },
    { value: 'SUCCEEDED', label: '\u652f\u4ed8\u6210\u529f' },
    { value: 'FAILED', label: '\u652f\u4ed8\u5931\u8d25' },
    { value: 'CANCELLED', label: '\u5df2\u53d6\u6d88' },
    { value: 'EXPIRED', label: '\u5df2\u8fc7\u671f' }
  ],
  paymentWebhookEventType: [
    { value: 'PAYMENT_SUCCEEDED', label: '\u652f\u4ed8\u6210\u529f' },
    { value: 'PAYMENT_FAILED', label: '\u652f\u4ed8\u5931\u8d25' },
    { value: 'PAYMENT_CANCELLED', label: '\u652f\u4ed8\u53d6\u6d88' },
    { value: 'REFUND_SUCCEEDED', label: '\u9000\u6b3e\u6210\u529f' },
    { value: 'CHARGEBACK_CREATED', label: '\u62d2\u4ed8\u521b\u5efa' }
  ],
  paymentWebhookStatus: [
    { value: 'RECEIVED', label: '\u5df2\u63a5\u6536' },
    { value: 'PROCESSED', label: '\u5df2\u5904\u7406' },
    { value: 'FAILED', label: '\u5904\u7406\u5931\u8d25' },
    { value: 'IGNORED', label: '\u5df2\u5ffd\u7565' }
  ],
  reconciliationBatchStatus: [
    { value: 'UPLOADED', label: '\u5df2\u4e0a\u4f20' },
    { value: 'VALIDATED', label: '\u5df2\u6821\u9a8c' },
    { value: 'RECONCILING', label: '\u5bf9\u8d26\u4e2d' },
    { value: 'COMPLETED', label: '\u5df2\u5b8c\u6210' },
    { value: 'FAILED', label: '\u5931\u8d25' }
  ],
  reconciliationLineStatus: [
    { value: 'INVALID', label: '\u65e0\u6548' },
    { value: 'MATCHED', label: '\u5df2\u5339\u914d' },
    { value: 'ISSUE', label: '\u6709\u5f02\u5e38' }
  ],
  reconciliationIssueStatus: [
    { value: 'OPEN', label: '\u5f85\u5904\u7406' },
    { value: 'RESOLVED', label: '\u5df2\u89e3\u51b3' },
    { value: 'IGNORED', label: '\u5df2\u5ffd\u7565' }
  ],
  reconciliationIssueType: [
    'PLATFORM_RECORD_MISSING',
    'PROVIDER_RECORD_MISSING',
    'ORDER_IDENTITY_MISMATCH',
    'AMOUNT_MISMATCH',
    'CURRENCY_MISMATCH',
    'EVENT_MISSING',
    'STATUS_MISMATCH',
    'DUPLICATE_PROVIDER_RECORD',
    'UNSUPPORTED_RECORD'
  ].map((value) => ({
    value,
    label: (
      {
        PLATFORM_RECORD_MISSING: '\u5e73\u53f0\u8bb0\u5f55\u7f3a\u5931',
        PROVIDER_RECORD_MISSING: '\u6e20\u9053\u8bb0\u5f55\u7f3a\u5931',
        ORDER_IDENTITY_MISMATCH: '\u8ba2\u5355\u6807\u8bc6\u4e0d\u4e00\u81f4',
        AMOUNT_MISMATCH: '\u91d1\u989d\u4e0d\u4e00\u81f4',
        CURRENCY_MISMATCH: '\u5e01\u79cd\u4e0d\u4e00\u81f4',
        EVENT_MISSING: '\u4e8b\u4ef6\u7f3a\u5931',
        STATUS_MISMATCH: '\u72b6\u6001\u4e0d\u4e00\u81f4',
        DUPLICATE_PROVIDER_RECORD: '\u6e20\u9053\u8bb0\u5f55\u91cd\u590d',
        UNSUPPORTED_RECORD: '\u6682\u4e0d\u652f\u6301\u7684\u8bb0\u5f55'
      } as Record<string, string>
    )[value]
  })),
  reconciliationResolutionType: [
    { value: 'PLATFORM_CONFIRMED', label: '\u5e73\u53f0\u6570\u636e\u786e\u8ba4' },
    { value: 'PROVIDER_CONFIRMED', label: '\u6e20\u9053\u6570\u636e\u786e\u8ba4' },
    { value: 'EXPECTED_DIFFERENCE', label: '\u9884\u671f\u5dee\u5f02' },
    { value: 'DUPLICATE_CONFIRMED', label: '\u91cd\u590d\u8bb0\u5f55\u786e\u8ba4' },
    { value: 'OTHER', label: '\u5176\u4ed6' }
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

export const purchaseOrderStatusType = (status?: string) => {
  if (status === 'CREDITED' || status === 'PAID') return 'success';
  if (status === 'FAILED' || status === 'CHARGEBACK' || status === 'CHARGEBACK_REVIEW') return 'danger';
  if (status === 'CREATED' || status === 'PENDING' || status === 'REFUND_REVIEW') return 'warning';
  if (status === 'CANCELLED' || status === 'REFUNDED') return 'info';
  return 'info';
};

export const purchaseReversalStatusType = (status?: string) => {
  if (status === 'COMPLETED') return 'success';
  if (status === 'REVIEW_REQUIRED') return 'danger';
  if (status === 'PROCESSING') return 'warning';
  return 'info';
};

export const purchaseReversalDispositionStatusType = (status?: string) => {
  if (status === 'RECOVERY_COMPLETED') return 'success';
  if (status === 'LOSS_ACCEPTED') return 'danger';
  if (status === 'PENDING_REVIEW') return 'warning';
  return 'info';
};

export const purchaseReversalReviewOperationType = (operationType?: string) => {
  if (operationType === 'RETRY_COMPLETED') return 'success';
  if (operationType === 'LOSS_ACCEPTED') return 'danger';
  if (operationType === 'RETRY_INSUFFICIENT') return 'warning';
  return 'info';
};

export const purchasePaymentEventStatusType = (status?: string) => {
  if (status === 'PROCESSED') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'RECEIVED') return 'warning';
  if (status === 'IGNORED') return 'info';
  return 'info';
};

export const paymentSessionStatusType = (status?: string) => {
  if (status === 'SUCCEEDED') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'CREATED' || status === 'PENDING') return 'warning';
  return 'info';
};

export const paymentWebhookStatusType = purchasePaymentEventStatusType;

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
