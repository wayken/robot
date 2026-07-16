import { createI18n } from 'vue-i18n'
import { getLocalLang } from '../util/locale'
import zhLocale from './zh-CN.json'
import enLocale from './en-US.json'

const messages = {
  zh: Object.assign(
    zhLocale
  ),
  en: Object.assign(
    enLocale
  )
}

const i18n = createI18n({
  allowComposition: true,
  globalInjection: true,
  legacy: false,
  locale: getLocalLang(),
  messages: messages
})

export default i18n
