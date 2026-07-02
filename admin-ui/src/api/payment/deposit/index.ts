import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { DepositOrderForm, DepositOrderQuery, DepositOrderVO } from './types';

export function listDeposit(query: DepositOrderQuery): AxiosPromise<DepositOrderVO[]> {
  return request({
    url: '/payment/deposit/list',
    method: 'get',
    params: query
  });
}

export function getDeposit(id: string | number): AxiosPromise<DepositOrderVO> {
  return request({
    url: '/payment/deposit/' + id,
    method: 'get'
  });
}

export function addDeposit(data: DepositOrderForm) {
  return request({
    url: '/payment/deposit',
    method: 'post',
    data
  });
}

export function simulateDepositSuccess(id: string | number): AxiosPromise<DepositOrderVO> {
  return request({
    url: '/payment/deposit/' + id + '/simulate-success',
    method: 'post'
  });
}

export function cancelDeposit(id: string | number) {
  return request({
    url: '/payment/deposit/' + id + '/cancel',
    method: 'post'
  });
}
