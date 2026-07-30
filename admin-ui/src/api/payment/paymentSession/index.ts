import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type { PaymentSessionQuery, PaymentSessionVO } from './types';

export function listPaymentSession(query: PaymentSessionQuery): AxiosPromise<PaymentSessionVO[]> {
  return request({
    url: '/payment/payment-session/list',
    method: 'get',
    params: query
  });
}

export function getPaymentSession(id: string | number): AxiosPromise<PaymentSessionVO> {
  return request({
    url: '/payment/payment-session/' + id,
    method: 'get'
  });
}
