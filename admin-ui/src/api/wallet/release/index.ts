import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ReleaseQuery, ReleaseVO } from './types';

export function listRelease(query: ReleaseQuery): AxiosPromise<ReleaseVO[]> {
  return request({
    url: '/wallet/release/list',
    method: 'get',
    params: query
  });
}

export function getRelease(id: string | number): AxiosPromise<ReleaseVO> {
  return request({
    url: '/wallet/release/' + id,
    method: 'get'
  });
}
