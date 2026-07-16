import { BrowserWindow } from 'electron'
import { autoUpdater } from 'electron-updater'

/**
 * 初始化自动更新
 */
export function initAutoUpdate(window: BrowserWindow) {
  // 是否自动更新，默认为true，手动触发更新要设置为false
  autoUpdater.autoDownload = false
  autoUpdater.on('error', (err) => {
    console.error('There was a problem updating the application: ' + err.message)
  })
}
