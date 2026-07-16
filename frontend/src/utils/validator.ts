/**
 * 判断是否为邮箱格式
 */
export function isEmail(value: string) {
  const pattern = /^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+(\.[a-zA-Z0-9_-])+/
  return pattern.test(value)
}

/**
 * 判断是否为手机格式
 */
export function isMobile(value: string) {
  const pattern = /^1\d{10}$/
  return pattern.test(value)
}
