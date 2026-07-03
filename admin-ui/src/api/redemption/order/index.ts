import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { RedemptionOrderForm, RedemptionOrderQuery, RedemptionOrderVO } from './types';

export function listRedemptionOrder(query: RedemptionOrderQuery): AxiosPromise<RedemptionOrderVO[]> {
  return request({
    url: '/redemption/order/list',
    method: 'get',
    params: query
  });
}

export function getRedemptionOrder(id: string | number): AxiosPromise<RedemptionOrderVO> {
  return request({
    url: '/redemption/order/' + id,
    method: 'get'
  });
}

export function addRedemptionOrder(data: RedemptionOrderForm) {
  return request({
    url: '/redemption/order',
    method: 'post',
    data
  });
}

export function approveRedemptionOrder(id: string | number, data: RedemptionOrderForm): AxiosPromise<RedemptionOrderVO> {
  return request({
    url: '/redemption/order/' + id + '/approve',
    method: 'post',
    data
  });
}

export function rejectRedemptionOrder(id: string | number, data: RedemptionOrderForm): AxiosPromise<RedemptionOrderVO> {
  return request({
    url: '/redemption/order/' + id + '/reject',
    method: 'post',
    data
  });
}
