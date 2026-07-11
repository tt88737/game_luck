import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ManualAdjustForm } from './types';

export function manualAdjust(data: ManualAdjustForm): AxiosPromise<void> {
  return request({
    url: '/wallet/manual-adjust',
    method: 'post',
    data
  });
}
