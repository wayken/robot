/**
 * 深度拷贝JSON对象
 *
 * @param {Object} source 要拷贝的对象
 */
export function useDeepClone(source: any) {
  return JSON.parse(JSON.stringify(source))
}

/**
 * 生成唯一ID
 */
export function uuid() {
  const compose = []
  const hexDigits = '0123456789abcdef'
  for (let i = 0; i < 36; i++) {
    const index = Math.floor(Math.random() * 0x10)
    compose[i] = hexDigits.substring(index, index + 1)
  }
  compose[14] = '4'
  compose[8] = compose[13] = compose[18] = compose[23] = '-'
  return compose.join('')
}

/**
 * 生成导入文件对话框
 */
export function useFileDialog(options: {
  accept?: string,
  multiple?: boolean,
  directory?: boolean
}): Promise<FileList | null> {
  return new Promise((resolve) => {
    const inputDom = document.createElement('input')
    inputDom.type = 'file'
    inputDom.onchange = (event: Event) => {
      const result = event.target as HTMLInputElement
      resolve(result.files)
    }
    inputDom.accept = options.accept || '*'
    inputDom.multiple = options.multiple || true
    if (options.directory) {
      inputDom.setAttribute('webkitdirectory', '')
    }
    inputDom.click()
  })
}

/**
 * 读取图片文件的dataURL
 */
export const useImageDataURLRead = (file: File): Promise<string> => {
  return new Promise(resolve => {
    const reader = new FileReader()
    reader.addEventListener('load', () => {
      resolve(reader.result as string)
    })
    reader.readAsDataURL(file)
  })
}

/**
 * 复制文本到剪贴板
 */
export const useTextClipboard = (text: string) => {
  return new Promise((resolve, reject) => {
    navigator.clipboard.writeText(text).then(() => {
      resolve(true)
    }).catch(err => {
      reject(err)
    })
  })
}
