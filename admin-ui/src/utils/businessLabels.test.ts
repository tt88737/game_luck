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
    expect(businessLabel('sourceType', 'GAME_BET')).toBe('\u6e38\u620f\u4e0b\u6ce8');
    expect(businessLabel('sourceType', 'GAME_PAYOUT')).toBe('\u6e38\u620f\u6d3e\u5956\uff08\u5386\u53f2\u6765\u6e90\uff09');
    expect(businessLabel('sourceType', 'GAME_PROFIT')).toBe('\u6e38\u620f\u6d3e\u5956');
    expect(businessLabel('sourceType', 'GAME_REFUND')).toBe('\u6e38\u620f\u9000\u6b3e');
    expect(businessLabel('sourceType', 'ADJUST')).toBe('\u4eba\u5de5\u8c03\u8d26\uff08\u5386\u53f2\u6765\u6e90\uff09');
    expect(businessLabel('sourceType', 'ADJUSTMENT')).toBe('\u4eba\u5de5\u8c03\u8d26\uff08\u5386\u53f2\u6765\u6e90\uff09');
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

  it('renders seeded wallet rule names without changing custom names', () => {
    expect(walletRuleNameLabel('RC manual adjustment', 'MANUAL_ADJUST', 'RC')).toBe('RC \u4eba\u5de5\u8c03\u8d26');
    expect(walletRuleNameLabel('SC game refund', 'GAME_REFUND', 'SC')).toBe('SC \u6e38\u620f\u9000\u6b3e');
    expect(walletRuleNameLabel('VIP custom rule', 'MANUAL_ADJUST', 'RC')).toBe('VIP custom rule');
  });

  it('builds select options with readable labels and raw enum values', () => {
    expect(businessOptions('depositStatus')).toContainEqual({ label: '\u5f85\u652f\u4ed8', value: 'PENDING' });
    expect(businessOptions('sourceType')).toContainEqual({ label: '\u5145\u503c', value: 'DEPOSIT' });
  });

  it('keeps historical source aliases available for historical queries', () => {
    expect(businessOptions('sourceType')).toEqual(
      expect.arrayContaining([
        { value: 'GAME_PAYOUT', label: '\u6e38\u620f\u6d3e\u5956\uff08\u5386\u53f2\u6765\u6e90\uff09' },
        { value: 'ADJUST', label: '\u4eba\u5de5\u8c03\u8d26\uff08\u5386\u53f2\u6765\u6e90\uff09' },
        { value: 'ADJUSTMENT', label: '\u4eba\u5de5\u8c03\u8d26\uff08\u5386\u53f2\u6765\u6e90\uff09' }
      ])
    );
  });

  it('limits wallet rule source options to standard source types', () => {
    const options = businessOptions('walletRuleSourceType');
    const values = options.map((option) => option.value);

    expect(values).toEqual([
      'REGISTER_BONUS',
      'DAILY_REWARD',
      'TASK_REWARD',
      'DEPOSIT',
      'GAME_BET',
      'GAME_PROFIT',
      'GAME_REFUND',
      'GAME_SETTLE',
      'REDEMPTION',
      'PROMOTION',
      'MANUAL_ADJUST',
      'TURNOVER'
    ]);
    expect(values).not.toContain('GAME_PAYOUT');
    expect(values).not.toContain('ADJUST');
    expect(values).not.toContain('ADJUSTMENT');
  });

  it('does not duplicate standard source labels in historical source options', () => {
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
