import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FundPropertyForm, FundPropertyQuery, FundPropertyVO } from './types';

export function listFundProperty(query: FundPropertyQuery): AxiosPromise<FundPropertyVO[]> {
  return request({
    url: '/wallet/fund-property/list',
    method: 'get',
    params: query
  });
}

export function getFundProperty(id: string | number): AxiosPromise<FundPropertyVO> {
  return request({
    url: '/wallet/fund-property/' + id,
    method: 'get'
  });
}

export function addFundProperty(data: FundPropertyForm) {
  return request({
    url: '/wallet/fund-property',
    method: 'post',
    data
  });
}

export function updateFundProperty(data: FundPropertyForm) {
  return request({
    url: '/wallet/fund-property',
    method: 'put',
    data
  });
}
