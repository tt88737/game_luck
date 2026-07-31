import type { SettlementBatchVO } from '@/api/payment/paymentSettlement/types';

export const settlementReportPermissions = {
  list: 'payment:settlementReport:list',
  query: 'payment:settlementReport:query',
  export: 'payment:settlementReport:export'
} as const;

export interface SettlementReportQuery extends PageQuery {
  startDate: string;
  endDate: string;
  providerCode?: string;
  currencyCode?: string;
}

export interface SettlementReportRowVO {
  settlementDate: string;
  providerCode: string;
  currencyCode: string;
  batchCount: number;
  eventCount: number;
  paymentCount: number;
  refundCount: number;
  chargebackCount: number;
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
  paymentCount: number;
  refundCount: number;
  chargebackCount: number;
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

export interface SettlementReportBatchVO extends SettlementBatchVO {}
