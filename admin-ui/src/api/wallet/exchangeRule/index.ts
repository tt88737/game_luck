import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ExchangeRuleForm, ExchangeRuleQuery, ExchangeRuleVO } from './types';

export function listExchangeRule(query: ExchangeRuleQuery): AxiosPromise<ExchangeRuleVO[]> {
  return request({
    url: '/wallet/exchange-rule/list',
    method: 'get',
    params: query
  });
}

export function getExchangeRule(id: string | number): AxiosPromise<ExchangeRuleVO> {
  return request({
    url: '/wallet/exchange-rule/' + id,
    method: 'get'
  });
}

export function addExchangeRule(data: ExchangeRuleForm) {
  return request({
    url: '/wallet/exchange-rule',
    method: 'post',
    data
  });
}

export function updateExchangeRule(data: ExchangeRuleForm) {
  return request({
    url: '/wallet/exchange-rule',
    method: 'put',
    data
  });
}
