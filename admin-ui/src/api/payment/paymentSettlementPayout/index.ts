import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type {
  SettlementPayoutCreateCommand,
  SettlementPayoutDetailVO,
  SettlementPayoutEditCommand,
  SettlementPayoutQuery,
  SettlementPayoutRowVO,
  SettlementPayoutStateCommand
} from './types';

const base = '/payment/settlement-payout';

export const listSettlementPayouts = (params: SettlementPayoutQuery): AxiosPromise<SettlementPayoutRowVO[]> =>
  request({ url: `${base}/list`, method: 'get', params });
export const getSettlementPayout = (id: string): AxiosPromise<SettlementPayoutDetailVO> =>
  request({ url: `${base}/${id}`, method: 'get' });
export const createSettlementPayout = (data: SettlementPayoutCreateCommand): AxiosPromise<SettlementPayoutDetailVO> =>
  request({ url: base, method: 'post', data });
export const editSettlementPayout = (id: string, data: SettlementPayoutEditCommand): AxiosPromise<SettlementPayoutDetailVO> =>
  request({ url: `${base}/${id}`, method: 'put', data });

const stateCommand = (id: string, action: 'submit' | 'approve' | 'reject' | 'cancel', data: SettlementPayoutStateCommand) =>
  request<SettlementPayoutDetailVO>({
    url: `${base}/${id}/${action}`,
    method: 'post',
    headers: { exposeBusinessCode: 'true' },
    data
  });

export const submitSettlementPayout = (id: string, data: SettlementPayoutStateCommand): AxiosPromise<SettlementPayoutDetailVO> =>
  stateCommand(id, 'submit', data);
export const approveSettlementPayout = (id: string, data: SettlementPayoutStateCommand): AxiosPromise<SettlementPayoutDetailVO> =>
  stateCommand(id, 'approve', data);
export const rejectSettlementPayout = (id: string, data: SettlementPayoutStateCommand): AxiosPromise<SettlementPayoutDetailVO> =>
  stateCommand(id, 'reject', data);
export const cancelSettlementPayout = (id: string, data: SettlementPayoutStateCommand): AxiosPromise<SettlementPayoutDetailVO> =>
  stateCommand(id, 'cancel', data);
