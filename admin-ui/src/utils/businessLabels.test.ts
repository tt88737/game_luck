import { describe, expect, it } from 'vitest';
import {
  businessLabel,
  businessOptions,
  depositStatusType,
  gameBetStatusType,
  walletStatusType
} from './businessLabels';

describe('businessLabels', () => {
  it('renders operator-readable labels for business enum values', () => {
    expect(businessLabel('sourceType', 'GAME_BET')).toBe('游戏下注');
    expect(businessLabel('method', 'SIMULATED')).toBe('平台模拟');
    expect(businessLabel('walletOperation', 'CREDIT')).toBe('入账');
    expect(businessLabel('walletReleaseMode', 'IMMEDIATE')).toBe('立即释放');
    expect(businessLabel('walletTransactionStatus', 'PENDING')).toBe('处理中');
    expect(businessLabel('walletFreezeStatus', 'FROZEN')).toBe('冻结中');
  });

  it('keeps unknown values visible instead of returning blank text', () => {
    expect(businessLabel('sourceType', 'NEW_SOURCE')).toBe('NEW_SOURCE');
    expect(businessLabel('sourceType', '')).toBe('');
  });

  it('builds select options with readable labels and raw enum values', () => {
    expect(businessOptions('depositStatus')).toContainEqual({ label: '待支付', value: 'PENDING' });
    expect(businessOptions('sourceType')).toContainEqual({ label: '充值', value: 'DEPOSIT' });
  });

  it('maps status values to Element Plus tag types', () => {
    expect(walletStatusType('SUCCESS')).toBe('success');
    expect(walletStatusType('FAILED')).toBe('danger');
    expect(depositStatusType('CANCELLED')).toBe('info');
    expect(gameBetStatusType('BET_SUCCESS')).toBe('primary');
  });
});
