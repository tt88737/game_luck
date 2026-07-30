export interface SettlementReportQuery extends PageQuery {
  startDate: string;
  endDate: string;
  providerCode?: string;
  currencyCode?: string;
}

export interface SettlementReportRowVO {
  reportDate: string;
  providerCode: string;
  currencyCode: string;
  batchCount: number;
  eventCount: number;
  paymentEventCount: number;
  refundEventCount: number;
  chargebackEventCount: number;
  grossPayment: string;
  refundAmount: string;
  chargebackAmount: string;
  totalFee: string;
  netSettlement: string;
  negativeNet: boolean;
  earliestPeriodStart: string;
  latestPeriodEnd: string;
  latestCloseTime: string;
}

export interface SettlementReportCurrencyTotalVO {
  currencyCode: string;
  batchCount: number;
  eventCount: number;
  paymentEventCount: number;
  refundEventCount: number;
  chargebackEventCount: number;
  grossPayment: string;
  refundAmount: string;
  chargebackAmount: string;
  totalFee: string;
  netSettlement: string;
}

export interface SettlementReportPageVO {
  rows: SettlementReportRowVO[];
  total: number;
  currencyTotals: SettlementReportCurrencyTotalVO[];
  generatedAt: string;
}

export interface SettlementReportBatchVO {
  id: string;
  settlementNo: string;
  providerCode: string;
  currencyCode: string;
  periodStart: string;
  periodEnd: string;
  status: string;
  paymentFeeRate: string;
  paymentFixedFee: string;
  chargebackFixedFee: string;
  eventCount: number;
  paymentCount: number;
  refundCount: number;
  chargebackCount: number;
  grossPayment: string;
  refundAmount: string;
  chargebackAmount: string;
  totalFee: string;
  netSettlement: string;
  reconciliationCoverageCount: number;
  openIssueCount: number;
  failureReason: string;
  creatorId: string;
  creatorName: string;
  calculatorId: string | null;
  calculatorName: string;
  closerId: string | null;
  closerName: string;
  closeRemark: string;
  calculatedTime: string;
  closedTime: string;
  version: number;
  createTime: string;
  updateTime: string;
}
