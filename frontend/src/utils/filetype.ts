// 文件后缀图标映射集合
const FILE_TYPE_MAP: {
  [key: string]: {
    icon: string
  }
} = {
  txt: {
    icon: 'document-text'
  },
  sh: {
    icon: 'document-shell'
  },
  vue: {
    icon: 'document-vue'
  },
  zip: {
    icon: 'document-rar'
  },
  rar: {
    icon: 'document-rar'
  },
  md: {
    icon: 'document-markdown'
  },
  ts: {
    icon: 'document-ts'
  },
  java: {
    icon: 'document-java'
  },
  py: {
    icon: 'document-python'
  },
  immp: {
    icon: 'document-mind'
  },
  html: {
    icon: 'document-html'
  },
  xml: {
    icon: 'document-xml'
  },
  yml: {
    icon: 'document-yaml'
  },
  yaml: {
    icon: 'document-yaml'
  },
  pdf: {
    icon: 'document-pdf'
  },
  json: {
    icon: 'document-json'
  },
  xls: {
    icon: 'document-xls'
  },
  xlsx: {
    icon: 'document-xls'
  },
  doc: {
    icon: 'document-doc'
  },
  docx: {
    icon: 'document-doc'
  },
  exe: {
    icon: 'document-exe'
  }
}

/**
 * 判断文件名后缀是否为图片
 *
 * @param  filename 文件名
 * @returns 是否为图片
 */
export const isImageType = (filename: string) => {
  return /\.(jpg|jpeg|png|gif|bmp)$/.test(filename)
}

/**
 * 判断文件名后缀是否为常用文本类型
 *
 * @param filename 文件名
 * @returns 是否为文本类型
 */
export const isTextType = (filename: string) => {
  return /\.(txt|md|json|js|ts|jsx|tsx|vue|html|css|scss|less|xml|yaml|yml|sh|bat|py|java|go|rs|c|cpp|h|hpp|sql|log|env|gitignore|conf|ini|toml)$/i.test(filename)
}

/**
 * 根据文件名获取 Monaco Editor 对应的语言标识
 *
 * @param filename 文件名
 * @returns Monaco Editor 语言标识
 */
export const useMonacoLanguage = (filename: string): string => {
  const suffix = filename.split('.').pop()?.toLowerCase() || ''
  const languageMap: Record<string, string> = {
    js: 'javascript',
    jsx: 'javascript',
    ts: 'typescript',
    tsx: 'typescript',
    vue: 'html',
    html: 'html',
    htm: 'html',
    css: 'css',
    scss: 'scss',
    less: 'less',
    json: 'json',
    md: 'markdown',
    xml: 'xml',
    yaml: 'yaml',
    yml: 'yaml',
    sh: 'shell',
    bat: 'bat',
    py: 'python',
    java: 'java',
    go: 'go',
    rs: 'rust',
    c: 'c',
    cpp: 'cpp',
    h: 'c',
    hpp: 'cpp',
    sql: 'sql',
    ini: 'ini',
    toml: 'ini',
    conf: 'ini'
  }
  return languageMap[suffix] || 'plaintext'
}

/**
 * 根据文件名获取图片 MIME 类型
 *
 * @param filename 文件名
 * @returns MIME 类型字符串
 */
export const useImageMimeType = (filename: string): string => {
  const suffix = filename.split('.').pop()?.toLowerCase() || ''
  switch (suffix) {
    case 'jpg':
    case 'jpeg':
      return 'image/jpeg'
    case 'gif':
      return 'image/gif'
    case 'bmp':
      return 'image/bmp'
    case 'png':
    default:
      return 'image/png'
  }
}

/**
 * 根据文件名获取文件后缀
 * 
 * @param filename 文件名
 * @returns 文件后缀
 */
export const loadFileSuffixByName = (filename: string) => {
  const suffix = filename.split('.').pop()
  if (!suffix) {
    return 'unknow'
  }
  return suffix.toUpperCase()
}

/**
 * 根据文件后缀获取对应的图标
 * 
 * @param {string} filename 文件名
 */
export function loadFileIconByName(filename: string, directory: boolean) {
  if (directory) {
    return 'document-folder'
  }
  const suffix = filename.split('.').pop()
  if (!suffix) {
    return 'document-unknow'
  }
  if (isImageType(filename)) {
    return 'document-image'
  }
  return FILE_TYPE_MAP[suffix]?.icon || 'document-unknow'
}
