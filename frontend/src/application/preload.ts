const { contextBridge, ipcRenderer } = require('electron')

// 注册系统相关接口 `system`，供渲染进程调用
contextBridge.exposeInMainWorld('system', {
  // 请求获取系统信息
  handleLoadSystemInfo: () => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-load-system-info')
      ipcRenderer.on('reply-load-system-info', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求获取网卡地址
  handleLoadMacId: () => {
    return ipcRenderer.sendSync('send-load-macid')
  },
  // 请求获取机器码
  handleLoadMachineId: () => {
    return ipcRenderer.sendSync('send-load-machine-id')
  }
})

// 注册系统属性接口 `properties`，供渲染进程调用
contextBridge.exposeInMainWorld('properties', {
  node: () => process.versions.node,                          // 获取当前 Node 版本
  chrome: () => process.versions.chrome,                      // 获取当前 Chrome 版本
  electron: () => process.versions.electron,                  // 获取当前 Electron 版本
  appRoot: () => process.cwd(),                               // 获取应用程序根目录
  // 当前窗口是否全屏
  isFullscreen: (windowId?: number) => {
    return ipcRenderer.sendSync('is-fullscreen', windowId)
  },
  // 当前窗口是否最大化
  isMaximizedWindow: (windowId?: number) => {
    return ipcRenderer.sendSync('is-window-maximized', windowId)
  },
  // 监听窗口最大化状态变化
  onWindowMaximized: (callback: (event: any, isMaximized: boolean) => void) => {
    ipcRenderer.on('on-window-maximized', callback)
  }
})

// 注册应用属性接口 `localdata`，供渲染进程调用
contextBridge.exposeInMainWorld('localdata', {
  // 请求读取指定本地数据文件，数据存储的父目录是`${user.dir}/${dataDir}`
  handleReadLocalDataFile: (path: string, createIfNotExists = false) => {
    return new Promise((resolve) => {
      const eventName = 'reply-read-local-data-file-' + Date.now() + '-' + Math.random()
      ipcRenderer.send('send-read-local-data-file', path, eventName, createIfNotExists)
      ipcRenderer.on(eventName, (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求从指定本地数据目录读取文件数据列表，数据存储的父目录是`${user.dir}/${dataDir}`
  handleReadLocalDataFileList: (path: string, start: number, limit: number) => {
    return new Promise((resolve) => {
      // 有可能会有个多个组件同时请求读取本地数据文件列表，事件名需要唯一
      const eventName = 'reply-read-local-data-file-list-' + Date.now() + '-' + Math.random()
      ipcRenderer.send('send-read-local-data-file-list', path, start, limit, eventName)
      ipcRenderer.on(eventName, (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求获取指定路径下所有文件夹，数据存储的父目录是`${user.dir}/${dataDir}`
  handleReadLocalDataFolderList: (path: string) => {
    return new Promise((resolve) => {
      // 有可能会有个多个组件同时请求获取指定路径下所有文件夹，事件名需要唯一
      const eventName = 'reply-read-local-data-folder-list-' + Date.now() + '-' + Math.random()
      ipcRenderer.send('send-read-local-data-folder-list', path, eventName)
      ipcRenderer.on(eventName, (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求递归获取指定路径下所有文件夹，数据存储的父目录是`${user.dir}/${dataDir}`
  handleReadLocalDataFolderListRecursive: (path: string) => {
    return new Promise((resolve) => {
      // 有可能会有个多个组件同时请求递归获取指定路径下所有文件夹，事件名需要唯一
      const eventName = 'reply-read-local-data-folder-list-recursive' + Date.now() + '-' + Math.random()
      ipcRenderer.send('send-read-local-data-folder-list-recursive', path, eventName)
      ipcRenderer.on(eventName, (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求获取指定路径下所有文件夹和文件
  handleReadLocalDataAllFile: (dirPath: string, isAutoCreate: boolean) => {
    return new Promise((resolve) => {
      // 有可能会有个多个组件同时请求获取指定路径下所有文件夹和文件，事件名需要唯一
      const eventName = 'reply-read-local-data-all-file-' + Date.now() + '-' + Math.random()
      ipcRenderer.send('send-read-local-data-all-file', dirPath, isAutoCreate, eventName)
      ipcRenderer.on(eventName, (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求创建本地数据目录文件
  handleMakeLocalDataFile: (filePath: string, data: any) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-make-local-data-file', filePath, data)
      ipcRenderer.on('reply-make-local-data-file', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求写入数据到本地数据文件
  handleWriteLocalDataFile: (filePath: string, data: any) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-write-local-data-file', filePath, data)
      ipcRenderer.on('reply-write-local-data-file', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求创建本地数据目录文件夹，数据存储的父目录是`${user.dir}/${dataDir}`
  handleMakeLocalDataDirectory: (dirPath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-make-local-data-directory', dirPath)
      ipcRenderer.on('reply-make-local-data-directory', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求重命名本地数据目录文件，数据存储的父目录是`${user.dir}/${dataDir}`
  handleRenameLocalDataFile: (oldPath: string, newPath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-rename-local-data-file', oldPath, newPath)
      ipcRenderer.on('reply-rename-local-data-file', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求重命名本地数据目录文件夹，数据存储的父目录是`${user.dir}/${dataDir}`
  handleRenameLocalDataDirectory: (oldPath: string, newPath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-rename-local-data-directory', oldPath, newPath)
      ipcRenderer.on('reply-rename-local-data-directory', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求删除本地数据目录文件
  handleDeleteLocalDataFile: (path: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-delete-local-data-file', path)
      ipcRenderer.on('reply-delete-local-data-file', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求删除本地数据目录
  handleDeleteLocalDataDirectory: (path: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-delete-local-data-directory', path)
      ipcRenderer.on('reply-delete-local-data-directory', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求移动本地数据目录及其所有文件
  handleMoveLocalDataDirectory: (oldPath: string, newPath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-move-local-data-directory', oldPath, newPath)
      ipcRenderer.on('reply-move-local-data-directory', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求复制指定文件到本地数据目录
  handleCopyLocalDataFile: (srcPath: string, newPath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-copy-local-data-file', srcPath, newPath)
      ipcRenderer.on('reply-copy-local-data-file', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求解析本地数据文件内容
  handleParseLocalDataFile: (path: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-parse-local-data-file', path)
      ipcRenderer.on('reply-parse-local-data-file', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求保存二进制数据到本地数据文件
  handleSaveLocalDataBufferFile: (path: string, data: any) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-save-local-data-buffer-file', path, data)
      ipcRenderer.on('reply-save-local-data-buffer-file', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求对本地目录数据进行备份
  handleBackupLocalDataDirectory: (fileName: string, backupPath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-backup-local-data-directory', fileName, backupPath)
      ipcRenderer.on('reply-backup-local-data-directory', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求对本地目录数据进行还原
  handleRestoreLocalDataDirectory: (fileName: string, restorePath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-restore-local-data-directory', fileName, restorePath)
      ipcRenderer.on('reply-restore-local-data-directory', (event, result) => {
        resolve(result)
      })
    })
  }
})

// 注册应用属性接口 `application`，供渲染进程调用
contextBridge.exposeInMainWorld('application', {
  // 请求切换全屏状态
  handleFullscreenSwitch: () => {
    ipcRenderer.send('send-fullscreen-switch')
  },
  // 请求最小化窗口
  handleMinimizeWindow: (windowId?: number) => {
    ipcRenderer.send('send-minimize-window', windowId)
  },
  // 请求最大化窗口
  handleMaximizeWindow: (windowId?: number) => {
    ipcRenderer.send('send-maximize-window', windowId)
  },
  // 请求还原窗口
  handleUnmaximizeWindow: (windowId?: number) => {
    ipcRenderer.send('send-unmaximize-window', windowId)
  },
  // 请求刷新窗口
  handleReloadWindow: (windowId?: number) => {
    ipcRenderer.send('send-reload-window', windowId)
  },
  // 请求将图片路径转换为Base64编码
  handleImageToBase64: (path: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-image-to-base64', path)
      ipcRenderer.on('reply-image-to-base64', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求将Base64编码图片保存为文件
  handleBase64ImageSave: (base64: string, path: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-base64-image-save', base64, path)
      ipcRenderer.on('reply-base64-image-save', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求打开文件/文件夹对话框
  handleImportFileDialog: (options: {
    properties?: string[],
    filters?: { name: string, extensions: string[] }[],
    defaultPath?: string
  }) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-import-file-dialog', options)
      ipcRenderer.on('reply-import-file-dialog', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求打开文件所在目录
  handleOpenFileDirectory: (filePath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-open-file-directory', filePath)
      ipcRenderer.on('reply-open-file-directory', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求复制图片文件
  handleCopyImageFile: (imagePath: string) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-copy-image-file', imagePath)
      ipcRenderer.on('reply-copy-image-file', (event, result) => {
        resolve(result)
      })
    })
  },
  // 请求打开外部链接
  handleOpenExternalLink: (url: string) => {
    ipcRenderer.send('send-open-external-link', url)
  },
  // 请求关闭窗口
  handleCloseWindow: (windowId?: number) => {
    ipcRenderer.send('send-close-window', windowId)
  },
  // 请求创建新窗口
  handleCreateNewWindow: (options: any) => {
    ipcRenderer.send('send-create-new-window', options)
  },
  // 请求打开开发者工具
  handleOpenDevTools: (windowId?: number) => {
    ipcRenderer.send('send-open-devtools', windowId)
  },
  // 请求读取应用程序配置
  handleLoadAppSetting: () => {
    return ipcRenderer.sendSync('send-load-app-setting')
  },
  // 请求保存应用程序配置
  handleSaveAppSetting: (data: any) => {
    return new Promise((resolve) => {
      ipcRenderer.send('send-save-app-setting', data)
      ipcRenderer.on('reply-save-app-setting', (event, result) => {
        resolve(result)
      })
    })
  }
})

// 注册应用属性接口 `electron`，供渲染进程调用
contextBridge.exposeInMainWorld('electron', {
  on: (channel: string, callback: (...args: any[]) => void) => {
    ipcRenderer.on(channel, (event, ...args) => callback(...args));
  },
  send: (channel: string, data: any) => {
    ipcRenderer.send(channel, data);
  },
  removeAllListeners: (channel: string) => {
    ipcRenderer.removeAllListeners(channel);
  }
})

// 注册应用属性接口 `tablet`，供渲染进程调用
contextBridge.exposeInMainWorld('tablet', {
  // 请求打开新的选项卡窗口
  handleOpenNewTabWindow: (options: any) => {
    ipcRenderer.send('send-open-new-tab-window', options)
  },
  // 请求切换选项卡窗口
  handleSwitchTabWindow: (options: any) => {
    ipcRenderer.send('send-switch-tab-window', options)
  },
  // 请求关闭选项卡窗口
  handleCloseTabWindow: (options: any) => {
    ipcRenderer.send('send-close-tab-window', options)
  },
  // 请求更新选项卡窗口
  handleUpdateTabWindow: (data: any) => {
    ipcRenderer.send('send-update-tab-window', data)
  },
  // 请求拖动选项卡窗口进行排序
  handleSortTabWindow: (data: any) => {
    ipcRenderer.send('send-sort-tab-window', data)
  },
  // 请求打开窗口开发者工具
  handleOpenTabWindowDevTools: () => {
    ipcRenderer.send('send-open-tab-devtools')
  },
  // 请求刷新选项卡页面
  handleReloadTabWindow: () => {
    ipcRenderer.send('send-reload-tab-window')
  }
})
