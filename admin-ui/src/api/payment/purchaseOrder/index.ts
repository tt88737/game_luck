import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { PurchaseManualActionForm, PurchaseOrderDetailVO, PurchaseOrderQuery, PurchaseOrderVO } from './types';

export function listPurchaseOrder(query: PurchaseOrderQuery): AxiosPromise<PurchaseOrderVO[]> {
  return request({
    url: '/payment/purchase-order/list',
    method: 'get',
    params: query
  });
}

export function getPurchaseOrder(id: string | number): AxiosPromise<PurchaseOrderDetailVO> {
  return request({
    url: '/payment/purchase-order/' + id,
    method: 'get'
  });
}

export function markPurchaseOrderFailed(id: string | number, data: PurchaseManualActionForm): AxiosPromise<PurchaseOrderDetailVO> {
  return request({
    url: '/payment/purchase-order/' + id + '/mark-failed',
    method: 'post',
    data
  });
}

export function cancelPurchaseOrder(id: string | number, data: PurchaseManualActionForm): AxiosPromise<PurchaseOrderDetailVO> {
  return request({
    url: '/payment/purchase-order/' + id + '/cancel',
    method: 'post',
    data
  });
}

export function refundPurchaseOrder(id: string | number, data: PurchaseManualActionForm): AxiosPromise<PurchaseOrderDetailVO> {
  return request({
    url: '/payment/purchase-order/' + id + '/refund',
    method: 'post',
    data
  });
}

export function chargebackPurchaseOrder(id: string | number, data: PurchaseManualActionForm): AxiosPromise<PurchaseOrderDetailVO> {
  return request({
    url: '/payment/purchase-order/' + id + '/chargeback',
    method: 'post',
    data
  });
}
