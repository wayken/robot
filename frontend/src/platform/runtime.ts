const runtime = window as any

// 获取系统信息
function handleLoadSystemInfo() {
  return runtime.system?.handleLoadSystemInfo()
}

// 获取应用程序各属性
function handleLoadProperties() {
  return runtime.properties
}

// 判断当前服务是否为应用程序
function isApplication() {
  return runtime.properties || false
}

// 判断是否为全屏窗口
function isFullscreen(windowId?: number) {
  return runtime.properties?.isFullscreen(windowId)
}

// 切换全屏
function handleFullscreenSwitch() {
  runtime.application?.handleFullscreenSwitch()
}

// 判断是否为窗口最大化
function isMaximizedWindow(windowId?: number) {
  return runtime.properties?.isMaximizedWindow(windowId)
}

// 监听窗口最大化状态变化
function onWindowMaximized(callback: (event: any, isMaximized: boolean) => void) {
  runtime.properties?.onWindowMaximized(callback)
}

// 打开外部链接
function handleOpenExternalLink(url: string) {
  return runtime.application?.handleOpenExternalLink(url)
}

// 新建窗口
function handleCreateNewWindow(options: any) {
  return runtime.application?.handleCreateNewWindow(options)
}

// 最小化窗口
function handleMinimizeWindow(windowId?: number) {
  return runtime.application?.handleMinimizeWindow(windowId)
}

// 最大化窗口
function handleMaximizeWindow(windowId?: number) {
  return runtime.application?.handleMaximizeWindow(windowId)
}

// 还原窗口
function handleUnmaximizeWindow(windowId?: number) {
  return runtime.application?.handleUnmaximizeWindow(windowId)
}

// 刷新窗口
function handleReloadWindow(windowId?: number) {
  return runtime.application?.handleReloadWindow(windowId)
}

// 将图片路径转换为Base64编码，路径为图片绝对路径
function handleImageToBase64(imagePath: string) {
  return runtime.application?.handleImageToBase64(imagePath)
}

// 将Base64编码图片保存为文件，路径为图片绝对路径
function handleBase64ImageSave(base64: string, filePath: string) {
  return runtime.application?.handleBase64ImageSave(base64, filePath)
}

// 打开文件/文件夹对话框
function handleImportFileDialog(options: {
  properties?: string[],
  filters?: { name: string, extensions: string[] }[],
  defaultPath?: string
}) {
  return runtime.application?.handleImportFileDialog(options)
}

// 打开文件所在目录，目录为绝对路径
function handleOpenFileDirectory(filePath: string) {
  return runtime.application?.handleOpenFileDirectory(filePath)
}

// 复制图片文件到剪贴板，路径为图片绝对路径
function handleCopyImageFile(imagePath: string) {
  return runtime.application?.handleCopyImageFile(imagePath)
}

// 关闭窗口
function handleCloseWindow(windowId?: number) {
  return runtime.application?.handleCloseWindow(windowId)
}

// 获取网卡地址
function handleLoadMacId() {
  return runtime.system?.handleLoadMacId()
}

// 获取机器码
function handleLoadMachineId() {
  return runtime.system?.handleLoadMachineId()
}

// 打开开发者工具
function handleOpenDevTools(windowId?: number) {
  return runtime.application?.handleOpenDevTools(windowId)
}

// 打开新选项卡窗口
function handleOpenNewTabWindow(options: any) {
  return runtime.tablet?.handleOpenNewTabWindow(options)
}

// 切换选项卡窗口
function handleSwitchTabWindow(options: any) {
  return runtime.tablet?.handleSwitchTabWindow(options)
}

// 关闭选项卡窗口
function handleCloseTabWindow(options: any) {
  return runtime.tablet?.handleCloseTabWindow(options)
}

// 更新选项卡窗口
function handleUpdateTabWindow(data: any) {
  return runtime.tablet?.handleUpdateTabWindow(data)
}

// 拖动选项卡窗口进行排序
function handleSortTabWindow(data: any) {
  return runtime.tablet?.handleSortTabWindow(data)
}

// 打开选项卡开发者工具
function handleOpenTabWindowDevTools() {
  return runtime.tablet?.handleOpenTabWindowDevTools()
}

// 刷新选项卡页面
function handleReloadTabWindow() {
  return runtime.tablet?.handleReloadTabWindow()
}

/**
 * 监听应用程序事件
 *
 * @param event 事件名称
 * @param callback 事件回调函数
 */
function handleApplicationEventOn(event: string, callback: (data: any) => void) {
  return runtime.electron?.on(event, callback)
}

/**
 * 发送应用程序事件
 *
 * @param event 事件名称
 * @param data 事件数据
 */
function handleApplicationEventSend(event: string, data: any) {
  return runtime.electron?.send(event, data)
}

/**
 * 取消监听应用程序事件
 *
 * @param event 事件名称
 */
function handleApplicationEventOff(event: string) {
  return runtime.electron?.removeAllListeners(event)
}

/**
 * 读取指定本地数据文件，数据存储的父目录是`${user.dir}/data`
 *
 * @param path 文件路径，相对于本地数据目录
 * @param createIfNotExists 如果文件不存在是否创建，默认为 `false`
 * @returns 文件数据
 */
function handleReadLocalDataFile(path: string, createIfNotExists = false) {
  return runtime.localdata?.handleReadLocalDataFile(path, createIfNotExists)
}

/**
 * 从指定本地数据目录读取文件数据列表，数据存储的父目录是`${user.dir}/data`
 * 
 * @param path  文件路径，相对于本地数据目录
 * @param start 起始位置，默认 `0`
 * @param limit 读取数量，默认 `-1`，读取全部
 * @returns 文件数据列表，数据结构 `[{ name: 'file1.json', data: 'file1 data' }]`
 */
function handleReadLocalDataFileList(path: string, start = 0, limit = -1) {
  return runtime.localdata?.handleReadLocalDataFileList(path, start, limit)
}

/**
 * 读取指定本地数据目录文件夹列表，数据存储的父目录是`${user.dir}/data`
 * 
 * @param path 文件夹路径，相对于本地数据目录
 * @returns 文件夹列表，数据结构 `[{ name: 'folder1' }]`
 */
function handleReadLocalDataFolderList(path: string) {
  return runtime.localdata?.handleReadLocalDataFolderList(path)
}

/**
 * 递归获取指定本地数据目录文件夹列表，数据存储的父目录是`${user.dir}/data`
 * 
 * @param path 文件夹路径，相对于本地数据目录
 * @returns 文件夹列表，数据结构 `[{ name: 'folder1', path: 'application/preset/space/folder1', children: [...] }]`
 */
function handleReadLocalDataFolderListRecursive(path: string) {
  return runtime.localdata?.handleReadLocalDataFolderListRecursive(path)
}

/**
 * 读取指定本地数据目录所有文件夹和文件，数据存储的父目录是`${user.dir}/data`
 * 
 * @param dirPath 文件夹路径，相对于本地数据目录
 * @returns 文件夹和文件列表，数据结构 `[{ name: 'file1.json', isDir: false, mtime: '2021-01-01 00:00:00' }]`
 */
function handleReadLocalDataAllFile(dirPath: string, isAutoCreate = false) {
  return runtime.localdata?.handleReadLocalDataAllFile(dirPath, isAutoCreate)
}

/**
 * 创建本地数据文件，数据存储的父目录是`${user.dir}/data`
 * 
 * @param path 文件路径，相对于本地数据目录
 * @param data 文件数据
 */
function handleMakeLocalDataFile(path: string, data: any) {
  return runtime.localdata?.handleMakeLocalDataFile(path, data)
}

/**
 * 写入本地数据文件，数据存储的父目录是`${user.dir}/data`
 * 
 * @param path 文件路径，相对于本地数据目录
 * @param data 文件数据
 */
function handleWriteLocalDataFile(path: string, data: any) {
  return runtime.localdata?.handleWriteLocalDataFile(path, data)
}

/**
 * 创建本地数据目录，数据存储的父目录是`${user.dir}/data`
 *
 * @param path 文件路径，相对于本地数据目录
 */
function handleMakeLocalDataDirectory(path: string) {
  return runtime.localdata?.handleMakeLocalDataDirectory(path)
}

/**
 * 重命名本地数据目录文件
 */
const handleRenameLocalDataFile = (oldPath: string, newPath: string) => {
  return runtime.localdata?.handleRenameLocalDataFile(oldPath, newPath)
}

/**
 * 重命名本地数据目录文件夹
 */
function handleRenameLocalDataDirectory(oldPath: string, newPath: string) {
  return runtime.localdata?.handleRenameLocalDataDirectory(oldPath, newPath)
}

/**
 * 删除本地数据目录文件
 *
 * @param path 文件路径，相对于本地数据目录
 */
function handleDeleteLocalDataFile(path: string) {
  return runtime.localdata?.handleDeleteLocalDataFile(path)
}

/**
 * 删除本地数据目录
 *
 * @param path 文件路径，相对于本地数据目录
 */
function handleDeleteLocalDataDirectory(path: string) {
  return runtime.localdata?.handleDeleteLocalDataDirectory(path)
}

/**
 * 移动本地数据目录文件
 *
 * @param oldPath 旧文件路径，相对于本地数据目录
 * @param newPath 新文件路径，相对于本地数据目录
 */
function handleMoveLocalDataDirectory(oldPath: string, newPath: string) {
  return runtime.localdata?.handleMoveLocalDataDirectory(oldPath, newPath)
}

/**
 * 复制本地数据目录到指定目录
 * 
 * @param srcPath 源目录路径，为绝对路径
 * @param newPath 目标目录路径，相对于本地数据目录
 */
function handleCopyLocalDataFile(srcPath: string, newPath: string) {
  return runtime.localdata?.handleCopyLocalDataFile(srcPath, newPath)
}

/**
 * 保存本地二进制数据文件
 * 
 * @param path   文件路径
 * @param buffer 文件数据
 */
function handleSaveLocalDataBufferFile(path: string, buffer: any) {
  return runtime.localdata?.handleSaveLocalDataBufferFile(path, buffer)
}

/**
 * 备份本地数据目录到指定目录
 * 
 * @param fileName 备份文件名
 * @param backupPath 备份路径
 */
function handleBackupLocalDataDirectory(fileName: string, backupPath: string) {
  return runtime.localdata?.handleBackupLocalDataDirectory(fileName, backupPath)
}

/**
 * 恢复本地数据目录到指定目录
 * 
 * @param backupPath 备份路径
 */
function handleRestoreLocalDataDirectory(backupPath: string) {
  return runtime.localdata?.handleRestoreLocalDataDirectory(backupPath)
}

/**
 * 解析本地数据目录
 *
 * @param path 文件路径
 * @returns 解析后的文件内容
 */
function handleParseLocalDataFile(path: string) {
  return runtime.localdata?.handleParseLocalDataFile(path)
}

// 加载应用程序配置
function handleLoadAppSetting() {
  return runtime.application?.handleLoadAppSetting()
}

// 保存应用程序配置
function handleSaveAppSetting(data: any) {
  return runtime.application?.handleSaveAppSetting(data)
}

export default {
  handleLoadSystemInfo,
  handleLoadProperties,
  isApplication,
  isFullscreen,
  handleFullscreenSwitch,
  isMaximizedWindow,
  onWindowMaximized,
  handleOpenExternalLink,
  handleMinimizeWindow,
  handleMaximizeWindow,
  handleUnmaximizeWindow,
  handleReloadWindow,
  handleImageToBase64,
  handleBase64ImageSave,
  handleImportFileDialog,
  handleOpenFileDirectory,
  handleCopyImageFile,
  handleCreateNewWindow,
  handleCloseWindow,
  handleLoadMacId,
  handleLoadMachineId,
  handleOpenDevTools,
  handleOpenNewTabWindow,
  handleSwitchTabWindow,
  handleCloseTabWindow,
  handleUpdateTabWindow,
  handleSortTabWindow,
  handleOpenTabWindowDevTools,
  handleReloadTabWindow,
  handleApplicationEventOn,
  handleApplicationEventSend,
  handleApplicationEventOff,
  handleReadLocalDataFile,
  handleReadLocalDataFileList,
  handleReadLocalDataFolderList,
  handleReadLocalDataFolderListRecursive,
  handleReadLocalDataAllFile,
  handleMakeLocalDataFile,
  handleWriteLocalDataFile,
  handleMakeLocalDataDirectory,
  handleRenameLocalDataFile,
  handleRenameLocalDataDirectory,
  handleDeleteLocalDataFile,
  handleDeleteLocalDataDirectory,
  handleMoveLocalDataDirectory,
  handleCopyLocalDataFile,
  handleParseLocalDataFile,
  handleSaveLocalDataBufferFile,
  handleBackupLocalDataDirectory,
  handleRestoreLocalDataDirectory,
  handleLoadAppSetting,
  handleSaveAppSetting
}
