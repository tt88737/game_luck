import request from '@/utils/request';
import { ApiDataResponse, ManualAdjustForm, ManualAdjustResultVO } from './types';

export function manualAdjust(data: ManualAdjustForm): Promise<ApiDataResponse<ManualAdjustResultVO>> {
  return request({
    url: '/wallet/manual-adjust',
    method: 'post',
    data
  });
}
