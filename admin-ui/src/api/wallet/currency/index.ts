import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { CurrencyForm, CurrencyQuery, CurrencyVO } from './types';

export function listCurrency(query: CurrencyQuery): AxiosPromise<CurrencyVO[]> {
  return request({
    url: '/wallet/currency/list',
    method: 'get',
    params: query
  });
}

export function getCurrency(id: string | number): AxiosPromise<CurrencyVO> {
  return request({
    url: '/wallet/currency/' + id,
    method: 'get'
  });
}

export function updateCurrency(data: CurrencyForm) {
  return request({
    url: '/wallet/currency',
    method: 'put',
    data
  });
}
