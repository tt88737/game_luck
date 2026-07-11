import { MenuTreeOption } from '@/api/system/menu/types';
import { translateTitle } from './i18nTitle';

export const localizeMenuTreeLabels = (tree: MenuTreeOption[] = []): MenuTreeOption[] => {
  return tree.map((item) => ({
    ...item,
    label: translateTitle(item.label),
    children: item.children ? localizeMenuTreeLabels(item.children) : item.children
  }));
};
