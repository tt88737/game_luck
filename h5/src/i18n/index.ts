import { computed, ref } from 'vue'
import { defaultLocale, messages, type Locale, type MessageKey } from './messages'

export type { Locale, MessageKey } from './messages'

const storageKey = 'gameluck:h5:locale'

const isLocale = (value: string | null): value is Locale => value === 'zh-CN' || value === 'en-US'

const getInitialLocale = (): Locale => {
  if (typeof window === 'undefined') {
    return defaultLocale
  }

  const savedLocale = window.localStorage.getItem(storageKey)
  return isLocale(savedLocale) ? savedLocale : defaultLocale
}

export const locale = ref<Locale>(getInitialLocale())

export const currentLocale = computed(() => locale.value)

export const setLocale = (nextLocale: Locale) => {
  locale.value = nextLocale

  if (typeof window !== 'undefined') {
    window.localStorage.setItem(storageKey, nextLocale)
  }
}

export const t = (key: MessageKey): string => messages[locale.value][key] ?? messages[defaultLocale][key]
