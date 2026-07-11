import { describe, expect, it } from 'vitest';
import i18n from '@/lang';
import { localizeMenuTreeLabels } from './menuTreeLabels';

describe('menuTreeLabels', () => {
  it('localizes seeded English menu labels in tree data', () => {
    i18n.global.locale.value = 'zh_CN';

    const tree = [
      {
        id: 1980,
        label: 'Member Center',
        parentId: 0,
        weight: 11,
        children: [
          {
            id: 1981,
            label: 'Member Profiles',
            parentId: 1980,
            weight: 1
          }
        ]
      },
      {
        id: 2000,
        label: 'Report Center',
        parentId: 0,
        weight: 12,
        children: [
          {
            id: 2002,
            label: 'Trends',
            parentId: 2000,
            weight: 2
          }
        ]
      }
    ];

    expect(localizeMenuTreeLabels(tree)).toEqual([
      {
        id: 1980,
        label: '\u4f1a\u5458\u4e2d\u5fc3',
        parentId: 0,
        weight: 11,
        children: [
          {
            id: 1981,
            label: '\u4f1a\u5458\u8d44\u6599',
            parentId: 1980,
            weight: 1
          }
        ]
      },
      {
        id: 2000,
        label: '\u62a5\u8868\u4e2d\u5fc3',
        parentId: 0,
        weight: 12,
        children: [
          {
            id: 2002,
            label: '\u8d8b\u52bf\u770b\u677f',
            parentId: 2000,
            weight: 2
          }
        ]
      }
    ]);
  });
});
