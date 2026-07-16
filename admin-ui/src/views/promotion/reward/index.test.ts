import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(__dirname, 'index.vue'), 'utf8');

describe('promotion reward page product boundary', () => {
  it('does not expose fund property as an operator-facing reward field', () => {
    expect(source).not.toContain("tt('资金属性')");
    expect(source).not.toContain('fundPropertyText(');
    expect(source).not.toContain('reward-field-auto');
    expect(source).not.toContain('reward-auto-property');
  });

  it('guides purchase and recharge campaigns to purchase center', () => {
    expect(source).toContain('配置奖励金额和流水要求。充值、首充、折扣、召回等购买类活动，请在支付/购买中心配置。');
  });
});
