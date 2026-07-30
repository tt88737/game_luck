import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type { PaymentWebhookEventDetailVO, PaymentWebhookEventQuery, PaymentWebhookEventVO, PaymentWebhookRetryResultVO } from './types';

export function listPaymentWebhookEvent(query: PaymentWebhookEventQuery): AxiosPromise<PaymentWebhookEventVO[]> {
  return request({
    url: '/payment/webhook-event/list',
    method: 'get',
    params: query
  });
}

export function getPaymentWebhookEvent(id: string | number): AxiosPromise<PaymentWebhookEventDetailVO> {
  return request({
    url: '/payment/webhook-event/' + id,
    method: 'get'
  });
}

export function retryPaymentWebhookEvent(id: string | number): AxiosPromise<PaymentWebhookRetryResultVO> {
  return request({
    url: '/payment/webhook-event/' + id + '/retry',
    method: 'post'
  });
}
