import {
  app,
  BrowserWindow
} from 'electron'
import { resolve } from 'path'

let loadingScreen: Electron.BrowserWindow | null

export function handleLoadingScreenShow() {
  loadingScreen = new BrowserWindow({
    width: 480,
    height: 200,
    frame: false,
    resizable: false,
    transparent: false,
    backgroundColor: 'white'
  })
  if (app.isPackaged) {
    const path = resolve(__dirname, '../loading.html')
    loadingScreen.loadFile(path)
  } else {
    const path = resolve(__dirname, '../../public/loading.html')
    loadingScreen.loadFile(path)
  }
  
  loadingScreen.on('closed', () => (loadingScreen = null))
  loadingScreen.show()
}

export function handleLoadingScreenClose() {
    loadingScreen && loadingScreen?.close()
}
