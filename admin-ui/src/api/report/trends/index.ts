import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ReportDailyTrendVO } from './types';

export function listReportDailyTrends(range: number): AxiosPromise<ReportDailyTrendVO[]> {
  return request({
    url: '/report/trends/daily',
    method: 'get',
    params: { range }
  });
}
