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
  multiple?: boolean
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
    inputDom.click()
  })
}

/**
 * 触发文本复制，属于将文本复制后能够直接粘贴到富文本编辑器
 */
export function useTextCopy(content: string): Promise<void> {
  return new Promise((resolve) => {
    // 获取 input，input 不能用 CSS 隐藏，必须在页面内存在
    let input = document.getElementById('use-copy-input') as HTMLInputElement
    if (!input) {
      input = document.createElement('input')
      input.id = 'use-copy-input'
      input.style.left = '-1000px'
      input.style.zIndex = '-1000'
      input.style.position = 'absolute'
      document.body.appendChild(input)
    }
    // 让 input 选中一个字符，无所谓那个字符
    input.value = 'NOTHING'
    input.setSelectionRange(0, 1)
    input.focus()
    // 复制触发
    document.addEventListener('copy', function handleCopyCall(e) {
      e.preventDefault()
      e.clipboardData?.setData('text/html', content)
      e.clipboardData?.setData('text/plain', content)
      document.removeEventListener('copy', handleCopyCall)
      resolve()
    })
    document.execCommand('copy')
  })
}

/**
 * 复制文本到剪贴板，属于将文本复制到剪贴板，粘贴到富文本编辑器也是原始代码
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
