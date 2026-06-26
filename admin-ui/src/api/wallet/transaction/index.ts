import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { TransactionQuery, TransactionVO } from './types';

export function listTransaction(query: TransactionQuery): AxiosPromise<TransactionVO[]> {
  return request({
    url: '/wallet/transaction/list',
    method: 'get',
    params: query
  });
}

export function getTransaction(id: string | number): AxiosPromise<TransactionVO> {
  return request({
    url: '/wallet/transaction/' + id,
    method: 'get'
  });
}
