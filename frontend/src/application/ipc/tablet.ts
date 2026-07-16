import {
  app,
  ipcMain,
  BrowserView
} from 'electron'
import dotenv from 'dotenv'
import { resolve } from 'path'
import setting from '../setting'

type WindowTab = {
  id: string;     // 工作空间ID，主要用于打开/关闭工作空间新选项卡时的匹配
  wid: number;    // 窗口ID，主要用于判断哪些选项卡窗口被激活
  root: boolean;  // 是否为主页
  name: string;   // 窗口名称
  path: string;   // 窗口路径
  parameters?: string;  // 窗口传递参数，默认为空
}
// 窗口实例对象，包含窗口对象和窗口数据
export interface IWindowInstance {
  window: BrowserView;  // 窗口对象，方便切换、销毁窗口等操作直接获取窗口对象
  data: WindowTab;      // 窗口数据，进行渲染进程和主进程之间的数据通信
}

// 主窗口对象
let loadMainWindow: Electron.BrowserWindow
// 子选项卡窗口列表
let loadTabWindowList: IWindowInstance[] = []

// 获取当前窗口和选项卡窗口列表数据，用于渲染进程获取当前打开的选项卡窗口列表
const handleWindowTabDataLoad = (): {
  activeWindowId: number;
  windowList: WindowTab[];
} => {
  return {
    activeWindowId: loadMainWindow.getBrowserView()?.webContents?.id as number,
    windowList: loadTabWindowList.map((instance) => instance.data)
  }
}
// 创建新的选项卡窗口实例
const handleWindowViewInit = (route: string) => {
  const settingData = setting.loadSettingData()
  const development = settingData.development
  const preload = resolve(__dirname, '../preload.js')
  const window = new BrowserView({
    webPreferences: {
      webSecurity: false,
      enableWebSQL: false,
      contextIsolation: true,
      nodeIntegration: true,
      devTools: development,
      preload: preload
    }
  })
  if (app.isPackaged) {
    const path = resolve(__dirname, '../../index.html')
    window.webContents.frameRate = 60;
    window.webContents.loadFile(path, {
      hash: `${route}`
    })
  } else {
    // 读取.env文件中的VITE_APP_PORT
    dotenv.config()
    const port = process.env.VITE_APP_PORT
    // 本地启动的vue项目路径
    const url = `http://localhost:${port}/#${route}`
    window.webContents.loadURL(url)
  }
  return window
}
// 创建首页视图窗口
const handleMainViewNew = () => {
  const window = handleWindowViewInit('/')
  loadTabWindowList.push({
    window: window,
    data: {
      id: 'index',
      wid: window.webContents.id,
      root: true,
      name: 'home',
      path: '/'
    }
  })
  handleTabWindowActive(window)
}
// 创建新的选项卡窗口
const handleTabWindowNew = (options: WindowTab) => {
  // 判断options.parameters是否存在，如果存在则拼接到path后面，用于传递参数
  if (options.parameters) {
    options.path = `${options.path}?${options.parameters}`
  }
  const window = handleWindowViewInit(options.path)
  loadTabWindowList.push({
    window: window,
    data: {
      id: options.id,
      wid: window.webContents.id,
      root: false,
      name: options.name,
      path: options.path
    }
  })
  handleTabWindowActive(window)
}
// 激活当前选项卡窗口对象
const handleTabWindowActive = (instance: BrowserView) => {
  loadMainWindow.setBrowserView(instance)
  const onWindowResize = () => {
    instance.setBounds({
      x: 0,
      y: 42,
      width: loadMainWindow.getContentBounds().width,
      height: loadMainWindow.getContentBounds().height - 42
    })
  }
  onWindowResize()
  // 监听主窗口缩放事件，同步调整选项卡窗口大小
  loadMainWindow.removeAllListeners('resize')
  loadMainWindow.on('resize', onWindowResize)
  // 监听当选项卡切换时，同步更新当前选项卡窗口数据到渲染进程
  loadMainWindow.webContents.send('on-window-tablet-switch', handleWindowTabDataLoad())
}
const handleAllWindowEventListen = () => {
  // 监听应用程序外观切换多语言，同步更新所有选项卡窗口主题
  ipcMain.on('on-window-language-switch', (event: any, data: any) => {
    loadMainWindow.webContents.send('on-window-language-switch', data)
    loadTabWindowList.forEach((instance) => {
      instance.window.webContents.send('on-window-language-switch', data);
    })
  })
  // 监听应用程序外观切换事件，同步更新所有选项卡窗口主题
  ipcMain.on('on-window-appearance-switch', (event: any, data: any) => {
    loadMainWindow.webContents.send('on-window-appearance-switch', data)
    loadTabWindowList.forEach((instance) => {
      instance.window.webContents.send('on-window-appearance-switch', data);
    })
  })
  // 监听应用程序主题切换事件，同步更新所有选项卡窗口主题
  ipcMain.on('on-window-theme-switch', (event: any, data: any) => {
    loadMainWindow.webContents.send('on-window-theme-switch', data)
    loadTabWindowList.forEach((instance) => {
      instance.window.webContents.send('on-window-theme-switch', data);
    })
  })
  // 监听创建新的选项卡窗口事件，通知主页面渲染进程创建新的选项卡窗口
  ipcMain.on('on-window-tab-add', (event: any, data: any) => {
    loadTabWindowList[0].window.webContents.send('on-window-tab-add', data)
  })
  // 监听渲染进程发送的请求在主窗口传递参数
  ipcMain.on('on-window-parameter-transfer', (event: any, data: any) => {
    loadMainWindow.getBrowserView()?.webContents.send('on-window-parameter-transfer', data)
  })
  // 监听用户会话登录数据
  ipcMain.on('on-login-session-data', (event: any, data: any) => {
    loadMainWindow.webContents.send('on-login-session-data', data)
    loadTabWindowList.forEach((instance) => {
      instance.window.webContents.send('on-login-session-data', data);
    })
  })
}
const onNewTabWindowOpen = (event: any, options: any) => {
  // 先检查是否已经打开了该选项卡窗口
  let instance = loadTabWindowList.find((item) => item.data.id === options.id)
  if (instance) {
    // 如果已经打开，则激活该窗口
    handleTabWindowActive(instance.window)
  } else {
    // 如果没有打开，则创建新的选项卡窗口
    handleTabWindowNew(options)
  }
}
const onTabWindowSwitch = (event: any, options: any) => {
  const instance = loadTabWindowList.find((item) => item.data.id === options.id)
  if (instance) {
    handleTabWindowActive(instance.window)
  }
}
const onTabWindowClose = (event: any, options: any) => {
  // 获取匹配的选项卡窗口实例和是否为当前窗口，用于关闭后激活上一个选项卡窗口
  const matchedInstance = loadTabWindowList.find((item) => item.data.id === options.id)
  if (!matchedInstance) return
  const isCurrentWindow = matchedInstance?.window.webContents.id === loadMainWindow.getBrowserView()?.webContents.id
  let preInstance
  if (isCurrentWindow) {
    const index = loadTabWindowList.findIndex((item) => item.data.id === options.id)
    preInstance = loadTabWindowList[index - 1]
  }
  // 关闭选项卡窗口
  loadTabWindowList = loadTabWindowList.filter((item) => item.data.id !== options.id)
  const matchedWindow = matchedInstance.window.webContents as any
  matchedWindow.destroy()
  // 如果关闭的是当前激活的选项卡窗口，则激活上一个选项卡窗口，否则激活当前选项卡窗口
  if (isCurrentWindow && preInstance) {
    handleTabWindowActive(preInstance.window)
  } else {
    handleTabWindowActive(loadMainWindow.getBrowserView() as BrowserView)
  }
}
const onTabWindowUpdate = (event: any, data: any) => {
  const instance = loadTabWindowList.find((item) => item.data.id === data.id)
  if (instance) {
    instance.data.name = data.name
  }
  handleTabWindowActive(loadMainWindow.getBrowserView() as BrowserView)
}
const onTabWindowSort = (event: any, data: any) => {
  loadTabWindowList = data.map((item: any) => {
    return loadTabWindowList.find((instance) => instance.data.id === item.id)
  })
  handleTabWindowActive(loadMainWindow.getBrowserView() as BrowserView)
}

function listenOn(mainWindow: Electron.BrowserWindow) {
  loadMainWindow = mainWindow
  mainWindow.on('close', () => {
    loadTabWindowList.forEach((instance) => {
      (instance.window.webContents as any)?.destroy()
    })
    loadTabWindowList = []
  })
  // 应用程序首次启动时创建首页视图窗口
  handleMainViewNew()
  // 监听应用程序所有窗口自定义事件
  handleAllWindowEventListen()
  // 监听渲染进程发送的请求在当前窗口打开新的选项卡窗口
  ipcMain.on('send-open-new-tab-window', onNewTabWindowOpen)
  // 监听渲染进程发送的请求切换选项卡窗口
  ipcMain.on('send-switch-tab-window', onTabWindowSwitch)
  // 监听渲染进程发送的请求关闭选项卡窗口
  ipcMain.on('send-close-tab-window', onTabWindowClose)
  // 监听渲染进程发送的请求更新选项卡窗口数据
  ipcMain.on('send-update-tab-window', onTabWindowUpdate)
  // 监听渲染进程发送的请求对当前选项卡窗口列表进行排序
  ipcMain.on('send-sort-tab-window', onTabWindowSort)
  // 监听渲染进程发送的请求打开选项卡开发者工具
  ipcMain.on('send-open-tab-devtools', () => {
    loadMainWindow.getBrowserView()?.webContents.openDevTools({
      mode: 'detach'
    })
    loadMainWindow.getBrowserView()?.webContents.devToolsWebContents?.focus()
  })
  // 监听渲染进程发送的请求刷新当前选项卡窗口
  ipcMain.on('send-reload-tab-window', () => {
    loadMainWindow.getBrowserView()?.webContents.reload()
  })
}

export default {
  listenOn
}
