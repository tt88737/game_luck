import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { SettlementReportBatchVO, SettlementReportPageVO, SettlementReportQuery } from './types';

const base = '/payment/settlement-report';

export const settlementReportPermissions = {
  list: 'payment:settlementReport:list',
  query: 'payment:settlementReport:query',
  export: 'payment:settlementReport:export'
} as const;

export const listSettlementReport = (params: SettlementReportQuery): AxiosPromise<SettlementReportPageVO> =>
  request({ url: `${base}/list`, method: 'get', params });

export const listSettlementReportBatches = (
  date: string,
  providerCode: string,
  currencyCode: string
): AxiosPromise<SettlementReportBatchVO[]> =>
  request({ url: `${base}/${date}/${providerCode}/${currencyCode}/batches`, method: 'get' });

export const exportSettlementReport = (
  params: Omit<SettlementReportQuery, 'pageNum' | 'pageSize'>
): AxiosPromise<Blob> => request({ url: `${base}/export`, method: 'get', params, responseType: 'blob' });
