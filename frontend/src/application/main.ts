import {
  app,
  shell,
  BrowserWindow,
  BrowserWindowConstructorOptions
} from 'electron'
import dotenv from 'dotenv'
import {
  handleLoadingScreenShow,
  handleLoadingScreenClose
} from './loading'
import Logger from 'electron-log'
import { resolve } from 'path'
import { initAutoUpdate } from './upgrade'
import system from './ipc/system'
import localdata from './ipc/localdata'
import application from './ipc/application'
import tablet from './ipc/tablet'

process.env['ELECTRON_DISABLE_SECURITY_WARNINGS'] = 'true'

/**
 * 创建主窗口
 */
const handleMainWindow = () => {
  const preload = resolve(__dirname, '../application/preload.js')
  const windowOption: BrowserWindowConstructorOptions = {
    width: 1520,
    height: 958,
    show: false,
    frame: false,
    center: true,
    backgroundColor: "#111217",
    webPreferences: {
      webSecurity: false,     // 是否启用同源策略
      enableWebSQL: false,    // 是否启用 WebSQL
      contextIsolation: true, // 是否开启隔离上下文
      nodeIntegration: true,  // 渲染进程使用 Node API
      preload: preload        // 预加载程序，此为 `preload/src/index.ts` 使用 Vite 打包之后的 lib 文件
    }
  }
  const window = new BrowserWindow(windowOption)
  window.setMenuBarVisibility(false)

  if (app.isPackaged) {
    const path = resolve(__dirname, '../index.html')
    window.loadFile(path, {
      hash: 'main'
    })
  } else {
    // 读取.env文件中的VITE_APP_PORT
    dotenv.config()
    const port = process.env.VITE_APP_PORT
    // 本地启动的vue项目路径
    const url = `http://localhost:${port}/#main`
    window.loadURL(url)
  }
  return window
}

app.whenReady().then(() => {
  handleLoadingScreenShow()
  const mainWindow = handleMainWindow()
  app.on('activate', () => {
    !BrowserWindow.getAllWindows().length && handleMainWindow()
  })
  // 监听窗口加载完成事件
  mainWindow.webContents.on('did-finish-load', () => {
    handleLoadingScreenClose()
    mainWindow.show()
  })
  // 确保特定的 URL 在系统默认浏览器中打开
  mainWindow.webContents.setWindowOpenHandler((details) => {
    const { url } = details
    if (url.includes('http://file/')) {
      const path = require('path')
      const fileName = url.replace('http://file/', '')
      const storageDir = path.join(app.getPath('userData'), 'Data', 'Files')
      const filePath = storageDir + '/' + fileName
      shell.openPath(filePath).catch((err) => console.error('Failed to open file:', err))
    } else {
      shell.openExternal(details.url)
    }
    return { action: 'deny' }
  })
  // 加载当前软件的配置文件，即`./setting.json`文件，如果不存在则创建
  // 将标准输出和标准错误重定向到日志文件
  if (app.isPackaged) {
    // 不存在日志文件所在的目录则创建，递归创建
    const logPath = Logger.transports.file.getFile().path
    const fs = require('fs')
    const path = require('path')
    const dir = path.dirname(logPath)
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, {
        recursive: true
      })
    }
    const logFile = fs.createWriteStream(logPath, { flags: 'a' })
    process.stdout.write = logFile.write.bind(logFile)
    process.stderr.write = logFile.write.bind(logFile)
  }
  // 初始化自动更新
  initAutoUpdate(mainWindow)
  // 监听渲染进程发送的关于系统操作的请求
  system.listenOn(mainWindow)
  // 监听渲染进程发送的关于本地数据操作的请求
  localdata.listenOn(mainWindow)
  // 监听渲染进程发送的关于应用操作的请求
  application.listenOn(mainWindow)
  // 监听渲染进程发送的关于新选项卡窗口操作的请求
  tablet.listenOn(mainWindow)
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
