/**
 * 复制文本到剪切板
 * 
 * @param text 要复制的文本
 */
export function useClipBoardCopy(text: string) {
  const dom = document.createElement('input')
  dom.value = text
  document.body.appendChild(dom)
  dom.select()
  document.execCommand('Copy')
  document.body.removeChild(dom)
}

/**
 * 从数组的末尾开始提取指定数量的元素
 * 
 * @param data 要提取的数组
 * @param num  要提取的数量
 */
export function useArrayRightSlice<T>(data: T[], num: number): T[] {
  if (num <= 0) {
    return []
  }
  const length = data.length
  if (num >= length) {
    return data
  }
  return data.slice(length - num)
}

/**
 * 获取文件的父路径
 * 
 * @param path 文件路径
 */
export function useDocumentParentPath(path: string) {
  const paths = path.replace(/\\/g, '/').split('/')
  return paths.slice(0, paths.length - 1).join('/')
}

/**
 * 将文件全名截取为文件名和后缀名，如xxx.md => {name: 'xxx', suffix: 'md'}
 * 
 * @param name 文件名
 */
export function useDocumentNameAndSuffix(name: string): { name: string; suffix: string } {
  const index = name.lastIndexOf('.')
  if (index === -1) {
    return { name, suffix: '' }
  }
  const fileName = name.substring(0, index)
  const fileSuffix = name.substring(index + 1)
  return {
    name: fileName,
    suffix: fileSuffix
  }
}
