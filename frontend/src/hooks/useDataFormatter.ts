import i18n from '@/locale'

/**
 * 输出"年-月-日"格式
 *
 * @param {Date, Number} timestamp 时间戳或者日期
 * @returns 2022-01-01
 */
export function dateftfn(timestamp: Date | number | null) {
  if (!timestamp) {
    return
  }
  const date = (timestamp instanceof Date) ? timestamp : new Date(timestamp)
  const year = date.getFullYear()
  const month = ('0' + (date.getMonth() + 1)).slice(-2)
  const day = ('0' + date.getDate()).slice(-2)
  return year + '/' + month + '/' + day
}

/**
 * 输出"年-月-日"格式，当日期为今年时只输出"月-日"格式
 *
 * @param {Date, Number} timestamp 时间戳或者日期
 * @returns 2022-01-01 / 10-01
 */
export function dateconciseftfn(timestamp: Date | number | null) {
  if (!timestamp) {
    return
  }
  const now = new Date()
  const date = (timestamp instanceof Date) ? timestamp : new Date(timestamp)
  const year = date.getFullYear()
  const month = ('0' + (date.getMonth() + 1)).slice(-2)
  const day = ('0' + date.getDate()).slice(-2)
  if (date.getFullYear() === now.getFullYear()) {
    return month + '-' + day
  }
  return year + '-' + month + '-' + day
}

/**
 * 输出"月-日"格式
 *
 * @param {Date, Number} timestamp 时间戳或者日期
 * @returns 10-01
 */
export function mondateftfn(timestamp: Date | number | null) {
  if (!timestamp) {
    return
  }
  const date = (timestamp instanceof Date) ? timestamp : new Date(timestamp)
  const month = ('0' + (date.getMonth() + 1)).slice(-2)
  const day = ('0' + date.getDate()).slice(-2)
  return month + '-' + day
}

/**
 * 输出"年-月-日 时:分:秒"格式
 *
 * @param {Date, Number} timestamp 时间戳或者日期
 * @returns 2022-10-01 10:10:24
 */
export function datetimeftfn(timestamp: Date | number | null) {
  if (!timestamp) {
    return
  }
  const date = (timestamp instanceof Date) ? timestamp : new Date(timestamp)
  const year = date.getFullYear()
  const month = ('0' + (date.getMonth() + 1)).slice(-2)
  const day = ('0' + date.getDate()).slice(-2)
  const hour = ('0' + date.getHours()).slice(-2)
  const minute = ('0' + date.getMinutes()).slice(-2)
  const second = ('0' + date.getSeconds()).slice(-2)
  return year + '-' + month + '-' + day + ' ' + hour + ':' + minute + ':' + second
}

/**
 * 输出"年-月-日 时:分"格式
 *
 * @param {Date, Number} timestamp 时间戳或者日期
 * @returns 2022-10-01 10:10
 */
export function dateminuteftfn(timestamp: Date | number | null) {
  if (!timestamp) {
    return
  }
  const date = (timestamp instanceof Date) ? timestamp : new Date(timestamp)
  const year = date.getFullYear()
  const month = ('0' + (date.getMonth() + 1)).slice(-2)
  const day = ('0' + date.getDate()).slice(-2)
  const hour = ('0' + date.getHours()).slice(-2)
  const minute = ('0' + date.getMinutes()).slice(-2)
  return year + '-' + month + '-' + day + ' ' + hour + ':' + minute
}

/**
 * 输出"时:分"格式
 *
 * @param {Date, Number} timestamp 时间戳或者日期
 * @returns 10:10
 */
export function hourminuteftfn(timestamp: Date | number | null) {
  if (!timestamp) {
    return
  }
  const date = (timestamp instanceof Date) ? timestamp : new Date(timestamp)
  const hour = ('0' + date.getHours()).slice(-2)
  const minute = ('0' + date.getMinutes()).slice(-2)
  return hour + ':' + minute
}

/**
 * 根据当前日期对比，输出"今天"、"明天"、"昨天"、"年-月-日"格式
 *
 * @param {Date, Number} timestamp 时间戳或者日期
 * @returns 今天 / 明天 / 昨天 / 2022-10-10 / 10-10
 */
export function datedescftfn(timestamp: Date | number | null) {
  if (!timestamp) {
    return
  }
  const now = new Date()
  const { t } = i18n.global
  const date = (timestamp instanceof Date) ? timestamp : new Date(timestamp)
  if (date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()) {
    return t('extension.today')
  } else if (date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate() + 1) {
    return t('extension.tomorrow')
  } else if (date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate() - 1) {
    return t('extension.yesterday')
  }
  const year = date.getFullYear()
  const month = ('0' + (date.getMonth() + 1)).slice(-2)
  const day = ('0' + date.getDate()).slice(-2)
  if (date.getFullYear() === now.getFullYear()) {
    return month + '-' + day
  }
  return year + '-' + month + '-' + day
}

/**
 * 根据字节大小输出对应的单位，如：1KB、1MB、1GB，保留两位小数
 *
 * @param value 字节大小
 * @returns 字节大小+单位
 */
export function sizeftfn(value: number) {
  if (value < 1024) {
    return value + 'B'
  } else if (value < 1024 * 1024) {
    return (value / 1024).toFixed(2) + 'KB'
  } else if (value < 1024 * 1024 * 1024) {
    return (value / 1024 / 1024).toFixed(2) + 'MB'
  }
  return (value / 1024 / 1024 / 1024).toFixed(2) + 'GB'
}
