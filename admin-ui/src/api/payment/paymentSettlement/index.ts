import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type {
  SettlementBatchQuery,
  SettlementBatchVO,
  SettlementCloseCommand,
  SettlementCreateCommand,
  SettlementDetailVO,
  SettlementItemQuery,
  SettlementItemVO
} from './types';

const base = '/payment/settlement';
export const listSettlementBatches = (params: SettlementBatchQuery): AxiosPromise<SettlementBatchVO[]> =>
  request({ url: `${base}/list`, method: 'get', params });
export const createSettlementBatch = (data: SettlementCreateCommand): AxiosPromise<SettlementDetailVO> =>
  request({ url: base, method: 'post', data });
export const getSettlementBatch = (batchId: string): AxiosPromise<SettlementDetailVO> => request({ url: `${base}/${batchId}`, method: 'get' });
export const listSettlementItems = (batchId: string, params: SettlementItemQuery): AxiosPromise<SettlementItemVO[]> =>
  request({ url: `${base}/${batchId}/items`, method: 'get', params });
export const calculateSettlementBatch = (batchId: string): AxiosPromise<SettlementDetailVO> =>
  request({ url: `${base}/${batchId}/calculate`, method: 'post', headers: { exposeBusinessCode: 'true' } });
export const closeSettlementBatch = (batchId: string, data: SettlementCloseCommand): AxiosPromise<SettlementDetailVO> =>
  request({ url: `${base}/${batchId}/close`, method: 'post', headers: { exposeBusinessCode: 'true' }, data });
