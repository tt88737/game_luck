import { computed, readonly, ref, type App } from 'vue'
import { messages, type Locale } from './messages'

const supportedLocales: Locale[] = ['en', 'zh-CN']

export function detectLocale(languages: readonly string[] = navigator.languages): Locale {
  const preferred = languages.find((language) => language.toLowerCase().startsWith('zh'))
  return preferred ? 'zh-CN' : 'en'
}

export function createI18n(languages?: readonly string[]) {
  const saved = localStorage.getItem('tangluck_locale') as Locale | null
  const initial = saved && supportedLocales.includes(saved) ? saved : detectLocale(languages)
  const locale = ref<Locale>(initial)

  function setLocale(nextLocale: Locale) {
    locale.value = nextLocale
    localStorage.setItem('tangluck_locale', nextLocale)
    document.documentElement.lang = nextLocale
  }

  function t(key: string, params: Record<string, string | number> = {}) {
    const template = messages[locale.value][key] ?? messages.en[key] ?? key
    return Object.entries(params).reduce(
      (text, [name, value]) => text.replaceAll(`{${name}}`, String(value)),
      template,
    )
  }

  document.documentElement.lang = initial

  return {
    locale: readonly(locale),
    isChinese: computed(() => locale.value === 'zh-CN'),
    setLocale,
    t,
  }
}

export const i18n = createI18n()

export function installI18n(app: App) {
  app.config.globalProperties.$t = i18n.t
}

export type { Locale }
