import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ExchangeOrderQuery, ExchangeOrderVO } from './types';

export function listExchangeOrder(query: ExchangeOrderQuery): AxiosPromise<ExchangeOrderVO[]> {
  return request({
    url: '/wallet/exchange-order/list',
    method: 'get',
    params: query
  });
}

export function getExchangeOrder(id: string | number): AxiosPromise<ExchangeOrderVO> {
  return request({
    url: '/wallet/exchange-order/' + id,
    method: 'get'
  });
}
