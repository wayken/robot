import {
  app
} from 'electron'
import path from 'path'

// 默认设置
const defaultSetting = {
  development: false
}

// 配置文件类型
export type SettingConfig = {
  development: boolean
}

// 获取应用程序数据目录
export const loadDataFilePath = path.join(app.getPath('userData'), 'Data')

/**
 * 读取当前软件的配置文件，即`./setting.json`文件，如果不存在则创建
 *
 * @returns {object} 返回当前软件的配置
 */
function loadSettingData(): SettingConfig {
  const fs = require('fs')
  let settingData = null
  const settingPath = process.cwd() + '/setting.json'
  if (fs.existsSync(settingPath)) {
    const data = fs.readFileSync(settingPath, 'utf-8')
    settingData = JSON.parse(data)
  } else {
    settingData = defaultSetting
    fs.writeFileSync(settingPath, JSON.stringify(defaultSetting, null, 2))
  }
  return settingData
}

export default {
  loadSettingData
}
