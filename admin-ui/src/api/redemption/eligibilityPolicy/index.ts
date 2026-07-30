import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { RedemptionEligibilityPolicyForm, RedemptionEligibilityPolicyQuery, RedemptionEligibilityPolicyVO } from './types';

export function listRedemptionEligibilityPolicy(query: RedemptionEligibilityPolicyQuery): AxiosPromise<RedemptionEligibilityPolicyVO[]> {
  return request({
    url: '/redemption/eligibility-policy/list',
    method: 'get',
    params: query
  });
}

export function getRedemptionEligibilityPolicy(id: string | number): AxiosPromise<RedemptionEligibilityPolicyVO> {
  return request({
    url: '/redemption/eligibility-policy/' + id,
    method: 'get'
  });
}

export function addRedemptionEligibilityPolicy(data: RedemptionEligibilityPolicyForm) {
  return request({
    url: '/redemption/eligibility-policy',
    method: 'post',
    data
  });
}

export function updateRedemptionEligibilityPolicy(data: RedemptionEligibilityPolicyForm) {
  return request({
    url: '/redemption/eligibility-policy',
    method: 'put',
    data
  });
}
