import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { RuleForm, RuleQuery, RuleVO } from './types';

export function listRule(query: RuleQuery): AxiosPromise<RuleVO[]> {
  return request({
    url: '/wallet/rule/list',
    method: 'get',
    params: query
  });
}

export function getRule(id: string | number): AxiosPromise<RuleVO> {
  return request({
    url: '/wallet/rule/' + id,
    method: 'get'
  });
}

export function addRule(data: RuleForm) {
  return request({
    url: '/wallet/rule',
    method: 'post',
    data
  });
}

export function updateRule(data: RuleForm) {
  return request({
    url: '/wallet/rule',
    method: 'put',
    data
  });
}
