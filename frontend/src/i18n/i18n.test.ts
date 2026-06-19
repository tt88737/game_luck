import { beforeEach, describe, expect, it } from 'vitest'
import { createI18n, detectLocale } from './index'

describe('i18n', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('defaults to English when browser language is not Chinese', () => {
    expect(detectLocale(['en-US'])).toBe('en')
    expect(detectLocale(['fr-FR'])).toBe('en')
    expect(detectLocale([])).toBe('en')
  })

  it('uses Chinese when browser language starts with zh', () => {
    expect(detectLocale(['zh-CN', 'en-US'])).toBe('zh-CN')
    expect(detectLocale(['zh-HK'])).toBe('zh-CN')
  })

  it('allows manual locale override for future language switcher', () => {
    const i18n = createI18n(['en-US'])

    expect(i18n.t('nav.home')).toBe('Home')

    i18n.setLocale('zh-CN')

    expect(i18n.locale.value).toBe('zh-CN')
    expect(i18n.t('nav.home')).toBe('首页')
  })

  it('falls back to English and then key when translation is missing', () => {
    const i18n = createI18n(['zh-CN'])

    expect(i18n.t('test.englishOnly')).toBe('English only')
    expect(i18n.t('missing.key')).toBe('missing.key')
  })
})
