import profile from '@/config/profile'

/**
 * 获取用户本地配置
 * 
 * @param module 模块名称，用于区分不同模块的配置，如 mission、note
 * @param key    配置项名称，支持多级配置，如 workspace.view
 */
export default (module: string) => {
  const localKey = profile[module].key
  const localProfile = localStorage.getItem(localKey) || '{}'
  const profileObject = JSON.parse(localProfile)
  const defaultProfile = profile[module] || {}
  // 将用户配置与默认配置合并
  const profileMerged = Object.assign({}, defaultProfile, profileObject)
  // 返回配置项
  const get = (key: string) => {
    const keys = key.split('.')
    let value = profileMerged
    for (let i = 0; i < keys.length; i++) {
      value = value[keys[i]]
    }
    return value
  }
  // 设置配置项
  const set = (key: string, value: any) => {
    const keys = key.split('.')
    let target = profileMerged
    for (let i = 0; i < keys.length - 1; i++) {
      target = target[keys[i]]
    }
    target[keys[keys.length - 1]] = value
    // 保存到本地存储
    localStorage.setItem(profileMerged.key, JSON.stringify(profileMerged))
  }
  return { get, set }
}
