import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { CurrencyPolicyForm, CurrencyPolicyQuery, CurrencyPolicyVO } from './types';

export function listCurrencyPolicy(query: CurrencyPolicyQuery): AxiosPromise<CurrencyPolicyVO[]> {
  return request({
    url: '/wallet/currency-policy/list',
    method: 'get',
    params: query
  });
}

export function getCurrencyPolicy(id: string | number): AxiosPromise<CurrencyPolicyVO> {
  return request({
    url: '/wallet/currency-policy/' + id,
    method: 'get'
  });
}

export function addCurrencyPolicy(data: CurrencyPolicyForm) {
  return request({
    url: '/wallet/currency-policy',
    method: 'post',
    data
  });
}

export function updateCurrencyPolicy(data: CurrencyPolicyForm) {
  return request({
    url: '/wallet/currency-policy',
    method: 'put',
    data
  });
}
