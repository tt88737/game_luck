import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { PromotionClaimForm, PromotionClaimQuery, PromotionClaimVO, PromotionRewardForm, PromotionRewardQuery, PromotionRewardVO } from './types';

export function listPromotionReward(query: PromotionRewardQuery): AxiosPromise<PromotionRewardVO[]> {
  return request({
    url: '/promotion/reward/list',
    method: 'get',
    params: query
  });
}

export function getPromotionReward(id: string | number): AxiosPromise<PromotionRewardVO> {
  return request({
    url: '/promotion/reward/' + id,
    method: 'get'
  });
}

export function addPromotionReward(data: PromotionRewardForm) {
  return request({
    url: '/promotion/reward',
    method: 'post',
    data
  });
}

export function updatePromotionReward(data: PromotionRewardForm) {
  return request({
    url: '/promotion/reward',
    method: 'put',
    data
  });
}

export function delPromotionReward(id: string | number | Array<string | number>) {
  return request({
    url: '/promotion/reward/' + id,
    method: 'delete'
  });
}

export function updatePromotionRewardStatus(id: string | number, status: string): AxiosPromise<PromotionRewardVO> {
  return request({
    url: '/promotion/reward/' + id + '/status/' + status,
    method: 'post'
  });
}

export function claimPromotionReward(id: string | number, data: PromotionClaimForm): AxiosPromise<PromotionClaimVO> {
  return request({
    url: '/promotion/reward/' + id + '/claim',
    method: 'post',
    data
  });
}

export function listPromotionClaim(query: PromotionClaimQuery): AxiosPromise<PromotionClaimVO[]> {
  return request({
    url: '/promotion/claim/list',
    method: 'get',
    params: query
  });
}
