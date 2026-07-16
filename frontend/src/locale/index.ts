import { createI18n } from 'vue-i18n'
import { getLocalLang } from '@/utils/locale'
import uiZhLocale from 'element-plus/es/locale/lang/zh-cn'
import uiEnLocale from 'element-plus/es/locale/lang/en'
import drZhLocale from './zh-CN.json'
import drEnLocale from './en-US.json'
import marksuitZhLocale from '@/marksuit/locale/zh-CN.json'
import marksuitEnLocale from '@/marksuit/locale/en-US.json'

const messages = {
  zh: Object.assign(
    uiZhLocale,
    drZhLocale,
    marksuitZhLocale
  ),
  en: Object.assign(
    uiEnLocale,
    drEnLocale,
    marksuitEnLocale
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
