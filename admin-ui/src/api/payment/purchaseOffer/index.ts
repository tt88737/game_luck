import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { PurchaseOfferForm, PurchaseOfferQuery, PurchaseOfferVO } from './types';

export function listPurchaseOffer(query: PurchaseOfferQuery): AxiosPromise<PurchaseOfferVO[]> {
  return request({
    url: '/payment/purchase-offer/list',
    method: 'get',
    params: query
  });
}

export function getPurchaseOffer(id: string | number): AxiosPromise<PurchaseOfferVO> {
  return request({
    url: '/payment/purchase-offer/' + id,
    method: 'get'
  });
}

export function addPurchaseOffer(data: PurchaseOfferForm) {
  return request({
    url: '/payment/purchase-offer',
    method: 'post',
    data
  });
}

export function updatePurchaseOffer(data: PurchaseOfferForm) {
  return request({
    url: '/payment/purchase-offer',
    method: 'put',
    data
  });
}
