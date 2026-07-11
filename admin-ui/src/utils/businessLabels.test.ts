import { describe, expect, it } from 'vitest';
import {
  businessLabel,
  businessOptions,
  depositStatusType,
  gameBetStatusType,
  walletRuleNameLabel,
  walletStatusType
} from './businessLabels';

describe('businessLabels', () => {
  it('renders operator-readable labels for business enum values', () => {
    expect(businessLabel('sourceType', 'REGISTER_BONUS')).toBe('\u6ce8\u518c\u8d60\u9001');
    expect(businessLabel('sourceType', 'DAILY_REWARD')).toBe('\u6bcf\u65e5\u5956\u52b1');
    expect(businessLabel('sourceType', 'TASK_REWARD')).toBe('\u4efb\u52a1\u5956\u52b1');
    expect(businessLabel('sourceType', 'GAME_BET')).toBe('游戏下注');
    expect(businessLabel('sourceType', 'GAME_PAYOUT')).toBe('\u6e38\u620f\u6d3e\u5f69');
    expect(businessLabel('sourceType', 'GAME_PROFIT')).toBe('\u6e38\u620f\u6d3e\u5f69');
    expect(businessLabel('sourceType', 'GAME_REFUND')).toBe('\u6e38\u620f\u9000\u6b3e');
    expect(businessLabel('sourceType', 'ADJUSTMENT')).toBe('\u4eba\u5de5\u8c03\u8d26');
    expect(businessLabel('sourceType', 'MANUAL_ADJUST')).toBe('\u4eba\u5de5\u8c03\u8d26');
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

  it('renders seeded wallet rule names without changing custom names', () => {
    expect(walletRuleNameLabel('RC manual adjustment', 'MANUAL_ADJUST', 'RC')).toBe('RC \u4eba\u5de5\u8c03\u8d26');
    expect(walletRuleNameLabel('SC game refund', 'GAME_REFUND', 'SC')).toBe('SC \u6e38\u620f\u9000\u6b3e');
    expect(walletRuleNameLabel('VIP custom rule', 'MANUAL_ADJUST', 'RC')).toBe('VIP custom rule');
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
