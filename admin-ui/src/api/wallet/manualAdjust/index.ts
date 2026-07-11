import request from '@/utils/request';
import { ApiDataResponse, ManualAdjustForm, ManualAdjustResult } from './types';

export function manualAdjust(data: ManualAdjustForm): Promise<ApiDataResponse<ManualAdjustResult>> {
  return request({
    url: '/wallet/manual-adjust',
    method: 'post',
    data
  });
}
