import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type {
  PurchaseReversalReviewActionForm,
  PurchaseReversalReviewActionResultVO,
  PurchaseReversalReviewDetailVO,
  PurchaseReversalReviewQuery,
  PurchaseReversalReviewVO
} from './types';

export function listPurchaseReversalReview(query: PurchaseReversalReviewQuery): AxiosPromise<PurchaseReversalReviewVO[]> {
  return request({
    url: '/payment/purchase-reversal-review/list',
    method: 'get',
    params: query
  });
}

export function getPurchaseReversalReview(reversalNo: string): AxiosPromise<PurchaseReversalReviewDetailVO> {
  return request({
    url: `/payment/purchase-reversal-review/${reversalNo}`,
    method: 'get'
  });
}

export function retryPurchaseReversalReview(
  reversalNo: string,
  data: PurchaseReversalReviewActionForm
): AxiosPromise<PurchaseReversalReviewActionResultVO> {
  return request({
    url: `/payment/purchase-reversal-review/${reversalNo}/retry`,
    method: 'post',
    data
  });
}

export function acceptPurchaseReversalLoss(
  reversalNo: string,
  data: PurchaseReversalReviewActionForm
): AxiosPromise<PurchaseReversalReviewActionResultVO> {
  return request({
    url: `/payment/purchase-reversal-review/${reversalNo}/accept-loss`,
    method: 'post',
    data
  });
}
