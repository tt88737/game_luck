import { describe, expect, it } from 'vitest';
import { resolveResponseMessage } from './responseMessage';

describe('responseMessage', () => {
  it('prefers backend business message over fallback code text', () => {
    expect(resolveResponseMessage(500, '\u4f1a\u5458\u4e0d\u5b58\u5728', '\u7cfb\u7edf\u672a\u77e5\u9519\u8bef')).toBe('\u4f1a\u5458\u4e0d\u5b58\u5728');
  });

  it('uses fallback code text when backend message is empty', () => {
    expect(resolveResponseMessage(500, '', '\u7cfb\u7edf\u672a\u77e5\u9519\u8bef')).toBe('\u7cfb\u7edf\u672a\u77e5\u9519\u8bef');
  });
});
