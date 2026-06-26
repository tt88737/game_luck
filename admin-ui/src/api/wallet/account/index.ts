import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { AccountQuery, AccountVO } from './types';

export function listAccount(query: AccountQuery): AxiosPromise<AccountVO[]> {
  return request({
    url: '/wallet/account/list',
    method: 'get',
    params: query
  });
}

export function getAccount(id: string | number): AxiosPromise<AccountVO> {
  return request({
    url: '/wallet/account/' + id,
    method: 'get'
  });
}
