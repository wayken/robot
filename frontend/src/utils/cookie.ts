// 获取指定 Cookie 的值
export function getCookie(cookieName: string) {
  const allCookies: string = document.cookie
  const cookiesArray: string[] = allCookies.split(';')
  const cookies: { [key: string]: string } = {}
  for (const cookie of cookiesArray) {
    const [name, value] = cookie.trim().split('=')
    cookies[name] = value
  }
  return cookies[cookieName]
}

/**
 * 写入浏览器cookie信息
 *
 * @param {String} key   Cookie键
 * @param {String} value Cookie值
 * @param {String} value Cookie域名
 */
export function setCookie(key: string, value: string, domain: string) {
  const days = 30
  const expire = new Date()
  expire.setTime(expire.getTime() + days * 24 * 60 * 60 * 1000)
  document.cookie = key + '=' + escape(value) + ';path=/;domain=' + domain + ';expires=' + expire.toString()
}
