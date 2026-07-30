import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type {
  ReconciliationBatchDetailVO,
  ReconciliationBatchQuery,
  ReconciliationBatchVO,
  ReconciliationIssueDetailVO,
  ReconciliationIssueQuery,
  ReconciliationIssueVO,
  ReconciliationLineQuery,
  ReconciliationLineVO,
  ReconciliationResolutionCommand
} from './types';
const base = '/payment/reconciliation';
export const listReconciliationBatches = (params: ReconciliationBatchQuery): AxiosPromise<ReconciliationBatchVO[]> =>
  request({ url: `${base}/list`, method: 'get', params });
export const uploadReconciliationBatch = (providerCode: string, statementDate: string, file: File): AxiosPromise<ReconciliationBatchDetailVO> => {
  const data = new FormData();
  data.append('providerCode', providerCode);
  data.append('statementDate', statementDate);
  data.append('file', file);
  return request({ url: `${base}/upload`, method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } });
};
export const getReconciliationBatch = (batchId: string): AxiosPromise<ReconciliationBatchDetailVO> =>
  request({ url: `${base}/${batchId}`, method: 'get' });
export const listReconciliationLines = (batchId: string, params: ReconciliationLineQuery): AxiosPromise<ReconciliationLineVO[]> =>
  request({ url: `${base}/${batchId}/lines`, method: 'get', params });
export const listReconciliationIssues = (batchId: string, params: ReconciliationIssueQuery): AxiosPromise<ReconciliationIssueVO[]> =>
  request({ url: `${base}/${batchId}/issues`, method: 'get', params });
export const executeReconciliationBatch = (batchId: string): AxiosPromise<ReconciliationBatchDetailVO> =>
  request({ url: `${base}/${batchId}/execute`, method: 'post' });
export const getReconciliationIssue = (issueId: string): AxiosPromise<ReconciliationIssueDetailVO> =>
  request({ url: `${base}/issues/${issueId}`, method: 'get' });
export const resolveReconciliationIssue = (issueId: string, data: ReconciliationResolutionCommand): AxiosPromise<ReconciliationIssueDetailVO> =>
  request({
    url: `${base}/issues/${issueId}/resolve`,
    method: 'post',
    headers: { exposeBusinessCode: 'true', silentError: 'true' },
    data: { resolutionType: data.resolutionType, remark: data.remark, expectedVersion: data.expectedVersion }
  });
export const ignoreReconciliationIssue = (issueId: string, data: ReconciliationResolutionCommand): AxiosPromise<ReconciliationIssueDetailVO> =>
  request({
    url: `${base}/issues/${issueId}/ignore`,
    method: 'post',
    headers: { exposeBusinessCode: 'true', silentError: 'true' },
    data: { resolutionType: data.resolutionType, remark: data.remark, expectedVersion: data.expectedVersion }
  });
