import {
  app,
  dialog,
  ipcMain,
  BrowserWindow
} from 'electron'
import fs from 'fs'
import path from 'path'
import dotenv from 'dotenv'
import { resolve } from 'path'
import setting, { loadDataFilePath } from '../setting'

// 子窗口列表，以窗口ID为键，窗口对象为值
const childWindowList = new Map<number, BrowserWindow>();

const onNewWindowCreate = (event: any, options: any) => {
  const preload = resolve(__dirname, '../preload.js')
  const childWindow = new BrowserWindow({
    width: options.width || 800,
    height: options.height || 600,
    frame: false,
    center: true,
    webPreferences: {
      webSecurity: false,
      enableWebSQL: false,
      contextIsolation: true,
      nodeIntegration: true,
      preload: preload
    }
  })
  childWindowList.set(childWindow.id, childWindow)
  options.maximize && childWindow.maximize()
  const params = options.route ? options.route : ''
  if (app.isPackaged) {
    const path = resolve(__dirname, '../../index.html')
    childWindow.loadFile(path, {
      hash: `${options.route}?${options.query}&windowId=${childWindow.id}`
    })
  } else {
    // 读取.env文件中的VITE_APP_PORT
    dotenv.config()
    const port = process.env.VITE_APP_PORT
    // 本地启动的vue项目路径
    const url = `http://localhost:${port}/#${params}?${options.query}&windowId=${childWindow.id}`
    childWindow.loadURL(url)
    // 打开开发者工具
    childWindow.webContents.openDevTools({
      mode: 'detach'
    })
  }
}
// 监听渲染进程发送的请求将图片路径转换为Base64编码，imagePath为图片绝对路径
const onImageBase64 = (event: any, imagePath: string) => {
  fs.readFile(imagePath, (error: any, data: any) => {
    if (error) {
      event.reply('reply-image-to-base64', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    const base64 = 'data:image/png;base64,' + Buffer.from(data).toString('base64')
    event.reply('reply-image-to-base64', {
      success: true,
      base64: base64
    })
  })
}
// 监听渲染进程发送的请求将Base64图片编码保存为图片文件，imagePath为图片绝对路径
const onBase64ImageSave = (event: any, base64: string, imagePath: string) => {
  const base64Data = base64.replace(/^data:image\/\w+;base64,/, '')
  const dataBuffer = Buffer.from(base64Data, 'base64')
  const directory = path.dirname(imagePath)
  if (!fs.existsSync(directory)) {
    fs.mkdirSync(directory, {
      recursive: true
    })
  }
  fs.writeFile(imagePath, dataBuffer, (error: any) => {
    if (error) {
      event.reply('reply-base64-image-save', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply('reply-base64-image-save', {
      success: true
    })
  })
}
const onFileDialogImport = (event: any, options: any) => {
  // 通过主进程打开文件对话框
  const openDialogOptions: Electron.OpenDialogOptions = {
    properties: options.properties
  }
  if (options.filters) {
    openDialogOptions.filters = options.filters
  }
  if (options.defaultPath) {
    openDialogOptions.defaultPath = options.defaultPath
  }
  dialog.showOpenDialog(openDialogOptions).then(result => {
    event.reply('reply-import-file-dialog', result.filePaths)
  })
}
const onFileDirectoryOpen = (event: any, filePath: string) => {
  fs.stat(filePath, (error: any, stats: any) => {
    if (error) {
      event.reply('reply-open-file-directory', {
        success: false,
        error: error
      })
      return
    }
    let directory = filePath
    if (!stats.isDirectory()) {
      directory = path.dirname(filePath)
    }
    // 使用 Shell 模块打开文件所在文件夹
    // 在 MacOS 和 Linux 上使用 'open' 命令，在 Windows 上使用 'start' 命令
    if (process.platform === 'darwin' || process.platform === 'linux') {
      require('child_process').exec(`open "${directory}"`);
    } else if (process.platform === 'win32') {
      require('child_process').exec(`start "" "${directory}"`)
    }
    event.reply('reply-open-file-directory', {
      success: true
    })
  })
}
const onFileImageCopy = (event: any, imagePath: string) => {
  fs.stat(imagePath, (error: any, stats: any) => {
    if (error) {
      event.reply('reply-copy-image-file', {
        success: false,
        error: error
      })
      return
    }
    if (!stats.isFile()) {
      event.reply('reply-copy-image-file', {
        success: false,
        error: 'not a file'
      })
      return
    }
    const { clipboard, nativeImage } = require('electron')
    const image = nativeImage.createFromPath(imagePath)
    clipboard.writeImage(image)
    event.reply('reply-copy-image-file', {
      success: true
    })
  })
}
const onAppSettingSave = (event: any, settingData: any) => {
  const settingPath = process.cwd() + '/setting.json'
  fs.writeFile(settingPath, settingData, (error: any) => {
    if (error) {
      event.reply('reply-save-app-setting', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply('reply-save-app-setting', {
      success: true
    })
  })
}

function listenOn(mainWindow: Electron.BrowserWindow) {
  // 监听渲染进程发送的请求获取当前窗口是否全屏
  ipcMain.on('is-fullscreen', (event, windowId) => {
    const childWindow = childWindowList.get(windowId)
    event.returnValue = childWindow ? childWindow.isFullScreen() : mainWindow.isFullScreen()
  })
  // 监听渲染进程发送的请求获取当前窗口是否最大化
  ipcMain.on('is-window-maximized', (event, windowId) => {
    const childWindow = childWindowList.get(windowId)
    event.returnValue = childWindow ? childWindow.isMaximized() : mainWindow.isMaximized()
  })
  // 监听渲染进程发送的请求切换全屏状态
  ipcMain.on('send-fullscreen-switch', () => {
    if (mainWindow.isFullScreen()) {
      mainWindow.setFullScreen(false)
    } else {
      mainWindow.setFullScreen(true)
    }
  })
  // 监听渲染进程发送的请求打开外部链接
  ipcMain.on('send-open-external-link', (event, url) => {
    require('electron').shell.openExternal(url)
  })
  // 监听渲染进程发送的请求最小化窗口
  ipcMain.on('send-minimize-window', (event, windowId) => {
    if (windowId) {
      const childWindow = childWindowList.get(windowId)
      childWindow && childWindow.minimize()
    } else {
      mainWindow.minimize()
    }
  })
  // 监听渲染进程发送的请求最大化窗口
  ipcMain.on('send-maximize-window', (event, windowId) => {
    if (windowId) {
      const childWindow = childWindowList.get(windowId)
      childWindow && childWindow.maximize()
    } else {
      mainWindow.maximize()
    }
  })
  // 监听渲染进程发送的请求还原窗口
  ipcMain.on('send-unmaximize-window', (event, windowId) => {
    if (windowId) {
      const childWindow = childWindowList.get(windowId)
      childWindow && childWindow.unmaximize()
    } else {
      mainWindow.unmaximize()
    }
  })
  // 监听渲染进程发送的请求关闭窗口
  ipcMain.on('send-close-window', (event, windowId) => {
    if (windowId) {
      const childWindow = childWindowList.get(windowId)
      childWindow && childWindow.close()
    } else {
      mainWindow.close()
    }
  })
  // 监听渲染进程发送的请求监听窗口最大化状态变化
  ipcMain.on('on-window-maximized', (event, callback) => {
    mainWindow.on('maximize', () => {
      callback(event, true)
    })
    mainWindow.on('unmaximize', () => {
      callback(event, false)
    })
  })
  // 监听渲染进程发送的请求监听刷新窗口
  ipcMain.on('send-reload-window', (event, windowId) => {
    if (windowId) {
      const childWindow = childWindowList.get(windowId)
      childWindow && childWindow.reload()
    } else {
      mainWindow.reload()
    }
  })
  // 监听渲染进程发送的请求创建新窗口
  ipcMain.on('send-create-new-window', onNewWindowCreate)
  // 监听渲染进程发送的请求将图片路径转换为Base64编码
  ipcMain.on('send-image-to-base64', onImageBase64)
  // 监听渲染进程发送的请求将Base64图片编码保存为图片文件
  ipcMain.on('send-base64-image-save', onBase64ImageSave)
  // 监听渲染进程发送的请求打开开发者工具
  ipcMain.on('send-open-devtools', (event, windowId) => {
    const childWindow = childWindowList.get(windowId)
    if (childWindow) {
      if (!childWindow.webContents.isDevToolsOpened()) {
        childWindow.webContents.openDevTools({
          mode: 'detach'
        })
      } else {
        childWindow.webContents.devToolsWebContents?.focus()
      }
    } else {
      if (!mainWindow.webContents.isDevToolsOpened()) {
        mainWindow.webContents.openDevTools({
          mode: 'detach'
        })
      } else {
        mainWindow.webContents.devToolsWebContents?.focus()
      }
    }
  })
  // 监听渲染进程发送的请求打开文件/文件夹对话框
  ipcMain.on('send-import-file-dialog', onFileDialogImport)
  // 监听渲染进程发送的请求打开文件所在目录
  ipcMain.on('send-open-file-directory', onFileDirectoryOpen)
  // 监听渲染进程发送的请求复制图片
  ipcMain.on('send-copy-image-file', onFileImageCopy)
  // 监听渲染进程发送的请求获取当前应用程序配置，即`./setting.json`文件
  ipcMain.on('send-load-app-setting', (event) => {
    const settingData = setting.loadSettingData() as any
    settingData.dataDir = loadDataFilePath
    event.returnValue = settingData
  })
  // 监听当主窗口关闭时，关闭所有子窗口
  mainWindow.on('closed', () => {
    childWindowList.forEach(childWindow => {
      if (childWindow && !childWindow.isDestroyed()) {
        childWindow.close();
      }
    })
  })
  // 监听渲染进程发送的请求保存应用程序配置，即`./setting.json`文件
  ipcMain.on('send-save-app-setting', onAppSettingSave)
}

export default {
  listenOn
}
