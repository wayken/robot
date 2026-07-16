import fs from 'fs'
import path from 'path'
import AdmZip from 'adm-zip'
import Logger from 'electron-log'
import { app, ipcMain } from 'electron'
import { loadDataFilePath } from '../setting'

const onLocalDataFileRead = (
  event: any,
  filePath: string,
  eventName: string,
  createIfNotExists: boolean
) => {
  // 通过主进程读取指定文件数据
  const dataDir = loadDataFilePath
  const dataPath = path.join(dataDir, filePath)
  // 先判断文件是否存在，不存在则创建
  if (!fs.existsSync(dataPath)) {
    if (createIfNotExists) {
      fs.mkdirSync(path.dirname(dataPath), { recursive: true })
      fs.writeFileSync(dataPath, '', 'utf-8')
    } else {
      event.reply(eventName, {
        success: false,
        error: 'ENOENT: no such file or directory of ' + dataPath
      })
      return
    }
  }
  fs.readFile(dataPath, 'utf-8', (error: any, buffer: string) => {
    if (error) {
      event.reply(eventName, {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply(eventName, {
      success: true,
      data: buffer
    })
  })
}
const onLocalDataFileListRead = (
  event: any,
  filePath: string,
  start: number,
  limit: number,
  eventName: string
) => {
  // 通过主进程读取文件数据列表
  const dataDir = loadDataFilePath
  const dirPath = path.join(dataDir, filePath)
  // 先判断目录是否存在，不存在则创建
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true })
  }
  // 读取文件列表
  fs.readdir(dirPath, (error: any, files: string[]) => {
    if (error) {
      event.reply(eventName, {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    // 遍历读取文件数据，并按时间倒序排序，只读取文件，不读取文件夹
    let fileList = files.map((file) => {
      const dataPath = path.join(dirPath, file)
      const stat = fs.statSync(dataPath)
      return {
        name: file,
        isDir: stat.isDirectory(),
        mtime: stat.mtime
      }
    }).filter((file) => {
      return !file.isDir
    }).sort((a, b) => {
      return b.mtime.getTime() - a.mtime.getTime()
    })
    const total = fileList.length
    // 如果有分页参数，则进行分页
    if (limit > 0) {
      fileList = fileList.slice(start, start + limit)
    }
    // 读取文件数据，避免一次性读取太多文件数据
    fileList = fileList.map((file) => {
      const dataPath = path.join(dirPath, file.name)
      const buffer = fs.readFileSync(dataPath, 'utf-8')
      return {
        ...file,
        data: buffer
      }
    })
    const dataDir = loadDataFilePath
    event.reply(eventName, {
      success: true,
      dataDir: dataDir,
      total: total,
      fileList: fileList
    })
  })
}
const onLocalDataFolderListRead = (
  event: any,
  dirPath: string,
  eventName: string
) => {
  // 通过主进程获取指定路径下所有文件夹和文件
  const dataDir = loadDataFilePath
  const directory = path.join(dataDir, dirPath)
  // 先判断目录是否存在，不存在则创建
  if (!fs.existsSync(directory)) {
    fs.mkdirSync(directory, { recursive: true })
  }
  // 读取文件夹列表
  fs.readdir(directory, (error: any, files: string[]) => {
    if (error) {
      event.reply(eventName, {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    // 只获取类型为文件夹的文件，并按时间倒序排序
    const folderList = files.filter((file) => {
      const dataPath = path.join(directory, file)
      const stat = fs.statSync(dataPath)
      return stat.isDirectory()
    }).map((file) => {
      const dataPath = path.join(directory, file)
      const stat = fs.statSync(dataPath)
      return {
        name: file,
        path: dataPath,
        isDir: stat.isDirectory(),
        mtime: stat.mtime
      }
    }).sort((a, b) => {
      return b.mtime.getTime() - a.mtime.getTime()
    })
    event.reply(eventName, {
      success: true,
      dataDir: dataDir,
      folderList: folderList
    })
  })
}
const onLocalDataFolderListRecursiveRead = (
  event: any,
  dirPath: string,
  eventName: string
) => {
  // 通过主进程递归获取指定路径下所有文件夹和文件
  const dataDir = loadDataFilePath.replace(/\\/g, '/')
  const directory = path.join(dataDir, dirPath)
  // 先判断目录是否存在，不存在则创建
  if (!fs.existsSync(directory)) {
    fs.mkdirSync(directory, { recursive: true })
  }
  // 递归读取文件夹列表
  const readFolderList = (directory: string) => {
    const id = directory.replace(/\\/g, '/').replace(dataDir, '').replace(/\//g, '$')
    const folderTree: any = {
      id: id,
      name: path.basename(directory),
      isDir: true,
      path: directory,
      children: []
    }
    const files = fs.readdirSync(directory)
    files.forEach((file: string) => {
      const fullPath = path.join(directory, file)
      // 检查是否为文件夹
      if (fs.statSync(fullPath).isDirectory()) {
        // 递归获取子文件夹并添加到 children 数组
        const childFolder = readFolderList(fullPath)
        folderTree.children.push(childFolder)
      }
    })
    return folderTree
  }
  const folderList = readFolderList(directory)
  event.reply(eventName, {
    success: true,
    dataDir: dataDir,
    folderList: folderList
  })
}
const onLocalDataAllFileRead = (
  event: any,
  dirPath: string,
  isAutoCreate: boolean,
  eventName: string
) => {
  // 通过主进程获取指定路径下所有文件夹和文件
  const dataDir = loadDataFilePath.replace(/\\/g, '/')
  const directory = path.join(dataDir, dirPath)
  // 先判断目录是否存在，不存在则创建
  if (!fs.existsSync(directory)) {
    if (!isAutoCreate) {
      event.reply(eventName, {
        success: false,
        error: `Directory ${directory} does not exist`
      })
      return
    } else {
      fs.mkdirSync(directory, { recursive: true })
    }
  }
  // 读取文件夹/文件列表
  fs.readdir(directory, (error: any, files: string[]) => {
    if (error) {
      event.reply(eventName, {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    // 读取文件数据，并按时间倒序排序
    const fileList = files.map((file) => {
      const dataPath = path.join(directory, file)
      directory.replace(/\\/g, '/').replace(dataDir, '').replace(/\//g, '$')
      const id = dataPath.replace(/\\/g, '/').replace(dataDir, '').replace(/\//g, '$')
      const stat = fs.statSync(dataPath)
      return {
        id: id,
        name: file,
        path: dataPath,
        isDir: stat.isDirectory(),
        mtime: stat.mtime
      }
    }).sort((a, b) => {
      return b.mtime.getTime() - a.mtime.getTime()
    })
    event.reply(eventName, {
      success: true,
      dataDir: dataDir,
      fileList: fileList
    })
  })
}
const onLocalDataFileMake = (event: any, filePath: string, data: string) => {
  // 通过主进程创建本地数据文件
  const dataDir = loadDataFilePath
  const dataPath = path.join(dataDir, filePath)
  // 先判断文件是否存在，存在则返回
  if (fs.existsSync(dataPath)) {
    event.reply('reply-make-local-data-file', {
      success: false,
      error: `File ${dataPath} already exists`
    })
    return
  }
  // 先判断目录是否存在，不存在则创建
  fs.mkdirSync(path.dirname(dataPath), { recursive: true })
  // 创建文件
  fs.writeFileSync(dataPath, data, 'utf-8')
  event.reply('reply-make-local-data-file', {
    success: true,
    dataDir: dataDir
  })
}
const onLocalDataFileWrite = (event: any, filePath: string, data: string) => {
  // 通过主进程写入数据到本地数据文件
  const dataDir = loadDataFilePath
  const dataPath = path.join(dataDir, filePath)
  // 先判断文件是否存在，不存在则返回
  if (!fs.existsSync(dataPath)) {
    event.reply('reply-write-local-data-file', {
      success: false,
      error: `File ${dataPath} does not exist`
    })
    return
  }
  // 写入文件
  fs.writeFileSync(dataPath, data, 'utf-8')
  event.reply('reply-write-local-data-file', {
    success: true
  })
}
const onLocalDataDirectoryMake = (event: any, dirPath: string) => {
  // 通过主进程创建本地数据目录文件夹
  const dataDir = loadDataFilePath
  const directory = path.join(dataDir, dirPath)
  // 先判断目录是否存在，不存在则创建
  if (fs.existsSync(directory)) {
    event.reply('reply-make-local-data-directory', {
      success: false,
      error: `Directory ${directory} already exists`
    })
  }
  fs.mkdirSync(directory, { recursive: true })
  event.reply('reply-make-local-data-directory', {
    success: true,
    dataDir: dataDir
  })
}
const onLocalDataFileRename = (event: any, oldPath: string, newPath: string) => {
  // 通过主进程重命名本地数据目录文件
  const dataDir = loadDataFilePath
  const fullOldPath = path.join(dataDir, oldPath)
  const fullNewPath = path.join(dataDir, newPath)
  // 先判断源文件是否存在
  if (!fs.existsSync(fullOldPath)) {
    event.reply('reply-rename-local-data-file', {
      success: false,
      error: 'Source file ' + fullOldPath + ' does not exist'
    })
    return
  }
  // 先判断目标文件的父目录是否存在，不存在则创建
  fs.mkdirSync(path.dirname(fullNewPath), { recursive: true })
  // 重命名文件
  fs.rename(fullOldPath, fullNewPath, (error: any) => {
    if (error) {
      event.reply('reply-rename-local-data-file', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply('reply-rename-local-data-file', {
      success: true
    })
  })
}
const onLocalDataDirectoryRename = (event: any, oldPath: string, newPath: string) => {
  // 通过主进程重命名本地数据目录文件
  const dataDir = loadDataFilePath
  const fullOldPath = path.join(dataDir, oldPath)
  const fullNewPath = path.join(dataDir, newPath)
  // 先判断源文件是否存在
  if (!fs.existsSync(fullOldPath)) {
    event.reply('reply-rename-local-data-directory', {
      success: false,
      error: 'Source file ' + fullOldPath + ' does not exist'
    })
    return
  }
  // 先判断目标文件的父目录是否存在，不存在则创建
  fs.mkdirSync(path.dirname(fullNewPath), { recursive: true })
  // 重命名文件
  fs.rename(fullOldPath, fullNewPath, (error: any) => {
    if (error) {
      event.reply('reply-rename-local-data-directory', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply('reply-rename-local-data-directory', {
      success: true
    })
  })
}
const onLocalDataFileDelete = (event: any, filePath: string) => {
  // 通过主进程删除本地数据目录文件
  const destination = path.join(loadDataFilePath, filePath)
  // 删除文件
  fs.unlink(destination, (error: any) => {
    if (error) {
      event.reply('reply-delete-local-data-file', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply('reply-delete-local-data-file', {
      success: true
    })
  })
}
const onLocalDataDirectoryDelete = (event: any, dirPath: string) => {
  // 通过主进程删除本地数据目录
  const directory = path.join(loadDataFilePath, dirPath)
  // 判断目录是否存在，不存在则直接返回
  if (!fs.existsSync(directory)) {
    event.reply('reply-delete-local-data-directory', {
      success: true
    })
    return
  }
  // 删除目录
  fs.rm(directory, { recursive: true }, (error: any) => {
    if (error) {
      event.reply('reply-delete-local-data-directory', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply('reply-delete-local-data-directory', {
      success: true
    })
  })
}
const onLocalDataDirectoryMove = (event: any, oldPath: string, newPath: string) => {
  // 通过主进程将本地数据目录移动到指定目录
  const dataDir = loadDataFilePath
  const fullOldPath = path.join(dataDir, oldPath)
  const fullNewPath = path.join(dataDir, newPath)
  // 先判断源文件是否存在
  if (!fs.existsSync(fullOldPath)) {
    event.reply('reply-move-local-data-directory', {
      success: false,
      error: 'Source directory ' + fullOldPath + ' does not exist'
    })
    return
  }
  // 先判断目标文件的父目录是否存在，不存在则创建
  fs.mkdirSync(path.dirname(fullNewPath), { recursive: true })
  // 移动文件
  fs.rename(fullOldPath, fullNewPath, (error: any) => {
    if (error) {
      event.reply('reply-move-local-data-directory', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply('reply-move-local-data-directory', {
      success: true
    })
  })
}
const onLocalDataFileCopy = (event: any, srcPath: string, newPath: string) => {
  // 通过主进程复制指定文件到本地数据目录文件
  const dataDir = loadDataFilePath
  const fullSrcPath = srcPath
  const fullNewPath = path.join(dataDir, newPath)
  // 先判断源文件是否存在
  if (!fs.existsSync(fullSrcPath)) {
    event.reply('reply-copy-local-data-file', {
      success: false,
      error: 'Source file ' + fullSrcPath + ' does not exist'
    })
    return
  }
  // 先判断目标文件的父目录是否存在，不存在则创建
  fs.mkdirSync(path.dirname(fullNewPath), { recursive: true })
  // 复制文件
  fs.copyFile(fullSrcPath, fullNewPath, (error: any) => {
    if (error) {
      event.reply('reply-copy-local-data-file', {
        success: false,
        error: error
      })
      console.error(error)
      return
    }
    event.reply('reply-copy-local-data-file', {
      success: true
    })
  })
}
// 监听渲染进程发送的请求解析文件内容，filePath为文件绝对路径
const onLocalDataFileParse = async (event: any, filePath: string) => {
  const officeParser = require('officeparser')
  const dataDir = loadDataFilePath
  const fullPath = path.join(dataDir, filePath)
  const extname = path.extname(fullPath).toLowerCase()
  const loadDocumentExts = ['.pdf', '.docx', '.pptx', '.xlsx', '.odt', '.odp', '.ods']
  if (loadDocumentExts.includes(extname)) {
    const data = await officeParser.parseOfficeAsync(fullPath)
    event.reply('reply-parse-local-data-file', {
      success: true,
      data: data
    })
  }
  const data = fs.readFileSync(fullPath, 'utf8')
  event.reply('reply-parse-local-data-file', {
    success: true,
    data: data
  })
}
const onLocalDataFileBufferSave = (event: any, filePath: string, fileData: any) => {
  // 通过主进程将文件图片数据保存到影子目录
  const directory = path.join(loadDataFilePath, filePath)
  // 先判断目录是否存在，不存在则创建
  if (!fs.existsSync(directory)) {
    fs.mkdirSync(path.dirname(directory), { recursive: true })
  }
  const buffer = Buffer.from(fileData)
  // 写入文件
  fs.writeFileSync(directory, buffer)
  event.reply('reply-save-local-data-buffer-file', {
    success: true
  })
}
const onLocalDataDirectoryBackup = (event: any, fileName: string, backupPath: string) => {
  const appTempDir = path.join(app.getPath('temp'), 'TeambeitWiki', 'Temp')
  // 先判断目录是否存在，不存在则创建
  if (!fs.existsSync(appTempDir)) {
    fs.mkdirSync(appTempDir, { recursive: true })
  }
  try {
    // 复制 Data 目录到临时目录
    const dataSourcePath = path.join(app.getPath('userData'), 'Data')
    const dataTempDir = path.join(appTempDir, 'Data')
    if (!fs.existsSync(dataTempDir)) {
      fs.mkdirSync(dataTempDir, { recursive: true })
    }
    handleDirCopyStreamable(dataSourcePath, dataTempDir)
    // 压缩数据文件
    const zip = new AdmZip()
    zip.addLocalFolder(appTempDir)
    const backupedFilePath = path.join(backupPath, fileName)
    zip.writeZip(backupedFilePath)
    Logger.info(`Backup ${dataSourcePath} To ${backupedFilePath} Successfully`)
    // 清理临时目录
    fs.rmSync(dataTempDir, { recursive: true, force: true })
    event.reply('reply-backup-local-data-directory', {
      success: true
    })
  } catch (error) {
    console.error('Error during backup:', error)
    event.reply('reply-backup-local-data-directory', {
      success: false,
      error: error
    })
  }
}
const onLocalDataDirectoryRestore = (event: any, backupPath: string) => {
  const appTempDir = path.join(app.getPath('temp'), 'TeambeitWiki', 'Temp')
  // 先判断目录是否存在，不存在则创建
  if (!fs.existsSync(appTempDir)) {
    fs.mkdirSync(appTempDir, { recursive: true })
  }
  try {
    const zip = new AdmZip(backupPath)
    zip.extractAllTo(appTempDir, true) // true 表示覆盖已存在的文件
    // 恢复 Data 目录
    const dataSourcePath = path.join(appTempDir, 'Data')
    const dataDestPath = path.join(app.getPath('userData'), 'Data')
    handleDirCopyStreamable(dataSourcePath, dataDestPath)
    Logger.info(`Restore ${backupPath} To ${dataDestPath} Successfully`)
    // 清理临时目录
    fs.rmSync(appTempDir, { recursive: true, force: true })
    event.reply('reply-restore-local-data-directory', {
      success: true
    })
  } catch (error) {
    console.error('Error during restore:', error)
    event.reply('reply-restore-local-data-directory', {
      success: false,
      error: error
    })
  }
}
const handleDirCopyStreamable = (source: string, destination: string) => {
  const modules = fs.readdirSync(source, { withFileTypes: true })
  for (const module of modules) {
    const sourcePath = path.join(source, module.name)
    const destPath = path.join(destination, module.name)
    if (module.isDirectory()) {
      if (!fs.existsSync(destPath)) {
        fs.mkdirSync(destPath, { recursive: true })
      }
      handleDirCopyStreamable(sourcePath, destPath)
    } else {
      fs.copyFileSync(sourcePath, destPath)
    }
  }
}

function listenOn(mainWindow: Electron.BrowserWindow) {
  // 监听渲染进程发送的请求读取指定文件数据
  ipcMain.on('send-read-local-data-file', onLocalDataFileRead)
  // 监听渲染进程发送的请求读取文件数据列表
  ipcMain.on('send-read-local-data-file-list', onLocalDataFileListRead)
  // 监听渲染进程发送的请求获取指定路径下所有文件夹
  ipcMain.on('send-read-local-data-folder-list', onLocalDataFolderListRead)
  // 监听渲染进程发送的请求递归获取指定路径下所有文件夹
  ipcMain.on('send-read-local-data-folder-list-recursive', onLocalDataFolderListRecursiveRead)
  // 监听渲染进程发送的请求获取指定路径下所有文件夹和文件
  ipcMain.on('send-read-local-data-all-file', onLocalDataAllFileRead)
  // 监听渲染进程发送的请求创建本地数据文件
  ipcMain.on('send-make-local-data-file', onLocalDataFileMake)
  // 监听渲染进程发送的请求写入数据到本地数据文件
  ipcMain.on('send-write-local-data-file', onLocalDataFileWrite)
  // 监听渲染进程发送的请求创建本地数据目录文件夹
  ipcMain.on('send-make-local-data-directory', onLocalDataDirectoryMake)
  // 监听渲染进程发送的请求重命名本地数据目录文件
  ipcMain.on('send-rename-local-data-file', onLocalDataFileRename)
  // 监听渲染进程发送的请求重命名本地数据目录
  ipcMain.on('send-rename-local-data-directory', onLocalDataDirectoryRename)
  // 监听渲染进程发送的请求删除本地数据目录文件
  ipcMain.on('send-delete-local-data-file', onLocalDataFileDelete)
  // 监听渲染进程发送的请求删除本地数据目录
  ipcMain.on('send-delete-local-data-directory', onLocalDataDirectoryDelete)
  // 监听渲染进程发送的请求将本地数据目录移动到指定目录
  ipcMain.on('send-move-local-data-directory', onLocalDataDirectoryMove)
   // 监听渲染进程发送的请求复制指定文件到本地数据目录文件
   ipcMain.on('send-copy-local-data-file', onLocalDataFileCopy)
   // 监听渲染进程发送的请求解析文件内容
  ipcMain.on('send-parse-local-data-file', onLocalDataFileParse)
  // 监听渲染进程发送的请求保存二进制数据到本地数据文件
  ipcMain.on('send-save-local-data-buffer-file', onLocalDataFileBufferSave)
  // 监听渲染进程发送的请求对本地目录数据进行备份
  ipcMain.on('send-backup-local-data-directory', onLocalDataDirectoryBackup)
  // 监听渲染进程发送的请求对本地目录数据进行还原
  ipcMain.on('send-restore-local-data-directory', onLocalDataDirectoryRestore)
}

export default {
  listenOn
}
