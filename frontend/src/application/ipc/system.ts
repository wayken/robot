import Logger from 'electron-log'
import { app, ipcMain } from 'electron'
import { machineIdSync } from 'node-machine-id'

function listenOn(mainWindow: Electron.BrowserWindow) {
  // 监听渲染进程发送的请求获取系统信息
  ipcMain.on('send-load-system-info', (event) => {
    const os = require('os')
    const path = require('path')
    event.reply('reply-load-system-info', {
      platform: os.platform(),
      arch: os.arch(),
      version: app.getVersion(),
      release: os.release(),
      type: os.type(),
      cpus: os.cpus(),
      appDataPath: app.getPath('userData'),
      fileDataPath: path.join(app.getPath('userData'), 'Data', 'Files'),
      logsPath: Logger.transports.file.getFile().path,
      totalmem: os.totalmem(),
      freemem: os.freemem()
    })
  }),
  // 监听渲染进程发送的请求获取网卡地址
  ipcMain.on('send-load-macid', (event) => {
    const networkInterfaces = require('os').networkInterfaces()
    // 遍历网络接口列表，查找物理网卡的 MAC 地址
    Object.keys(networkInterfaces).forEach((interfaceName) => {
      const networkInterface = networkInterfaces[interfaceName]
      // 过滤出物理网卡的信息
      const physicalInterfaces = networkInterface.filter((iface: any) => iface.mac && iface.mac !== '00:00:00:00:00:00')
      if (physicalInterfaces.length > 0) {
        // 返回第一个物理网卡的 MAC 地址
        event.returnValue = physicalInterfaces[0].mac
      } else {
        event.returnValue = '00:00:00:00:00:00'
      }
    })
  }),
  // 监听渲染进程发送的请求获取机器码
  ipcMain.on('send-load-machine-id', (event) => {
    event.returnValue = machineIdSync(true)
  })
}

export default {
  listenOn
}
