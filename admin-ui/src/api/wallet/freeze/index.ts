import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FreezeQuery, FreezeVO } from './types';

export function listFreeze(query: FreezeQuery): AxiosPromise<FreezeVO[]> {
  return request({
    url: '/wallet/freeze/list',
    method: 'get',
    params: query
  });
}

export function getFreeze(id: string | number): AxiosPromise<FreezeVO> {
  return request({
    url: '/wallet/freeze/' + id,
    method: 'get'
  });
}
