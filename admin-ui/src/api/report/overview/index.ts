import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ReportOverviewSummaryVO } from './types';

export function getReportOverviewSummary(): AxiosPromise<ReportOverviewSummaryVO> {
  return request({
    url: '/report/overview/summary',
    method: 'get'
  });
}

