/**
 * 获取本地语言版本
 */
export function getLocalLang(): string {
  let localLang = localStorage.getItem('Local-Lang')
  if (!localLang) {
    const navigatorLang = navigator.language || 'zh'
    localLang = navigatorLang.toLowerCase()
    const index = navigatorLang.indexOf('-')
    if (index !== -1) {
      localLang = navigatorLang.substring(0, index).toLowerCase()
    }
    setLocalLang(localLang)
  }
  return localLang
}

/**
 * 保存本地语言版本
 */
export function setLocalLang(lang: string): void {
  localStorage.setItem('Local-Lang', lang)
}
