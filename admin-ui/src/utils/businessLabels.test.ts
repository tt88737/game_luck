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
    expect(businessLabel('sourceType', 'REGISTER_BONUS')).toBe('\u6ce8\u518c\u8d60\u9001');
    expect(businessLabel('sourceType', 'DAILY_REWARD')).toBe('\u6bcf\u65e5\u5956\u52b1');
    expect(businessLabel('sourceType', 'TASK_REWARD')).toBe('\u4efb\u52a1\u5956\u52b1');
    expect(businessLabel('sourceType', 'GAME_BET')).toBe('\u6e38\u620f\u4e0b\u6ce8');
    expect(businessLabel('sourceType', 'GAME_PROFIT')).toBe('\u6e38\u620f\u6d3e\u5956');
    expect(businessLabel('sourceType', 'GAME_REFUND')).toBe('\u6e38\u620f\u9000\u6b3e');
    expect(businessLabel('sourceType', 'MANUAL_ADJUST')).toBe('\u4eba\u5de5\u8c03\u8d26');
    expect(businessLabel('method', 'SIMULATED')).toBe('\u5e73\u53f0\u6a21\u62df');
    expect(businessLabel('walletOperation', 'CREDIT')).toBe('\u5165\u8d26');
    expect(businessLabel('walletReleaseMode', 'IMMEDIATE')).toBe('\u7acb\u5373\u91ca\u653e');
    expect(businessLabel('walletTransactionStatus', 'PENDING')).toBe('\u5904\u7406\u4e2d');
    expect(businessLabel('walletFreezeStatus', 'FROZEN')).toBe('\u51bb\u7ed3\u4e2d');
  });

  it('keeps unknown values visible instead of returning blank text', () => {
    expect(businessLabel('sourceType', 'NEW_SOURCE')).toBe('NEW_SOURCE');
    expect(businessLabel('sourceType', '')).toBe('');
  });

  it('builds select options with readable labels and raw enum values', () => {
    expect(businessOptions('depositStatus')).toContainEqual({ label: '\u5f85\u652f\u4ed8', value: 'PENDING' });
    expect(businessOptions('sourceType')).toContainEqual({ label: '\u5145\u503c', value: 'DEPOSIT' });
  });

  it('keeps source options limited to current foundation values', () => {
    expect(businessOptions('sourceType')).not.toEqual(
      expect.arrayContaining([
        expect.objectContaining({ value: 'GAME_PAYOUT' }),
        expect.objectContaining({ value: 'ADJUST' }),
        expect.objectContaining({ value: 'ADJUSTMENT' })
      ])
    );
  });

  it('does not duplicate standard source labels in source options', () => {
    const options = businessOptions('sourceType');
    const gamePayoutStandardOptions = options.filter((option) => option.label === '\u6e38\u620f\u6d3e\u5956');
    const manualAdjustStandardOptions = options.filter((option) => option.label === '\u4eba\u5de5\u8c03\u8d26');

    expect(gamePayoutStandardOptions).toHaveLength(1);
    expect(manualAdjustStandardOptions).toHaveLength(1);
  });

  it('maps status values to Element Plus tag types', () => {
    expect(walletStatusType('SUCCESS')).toBe('success');
    expect(walletStatusType('FAILED')).toBe('danger');
    expect(depositStatusType('CANCELLED')).toBe('info');
    expect(gameBetStatusType('BET_SUCCESS')).toBe('primary');
  });
});
