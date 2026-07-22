import hljs from 'highlight.js'
import MarkdownIt from 'markdown-it'
import MarkdownItToc from 'markdown-it-table-of-contents'
import MarkdownItTaskList from 'markdown-it-task-lists'
import markdownItKatex from 'markdown-it-katex'
import MarkdownItMermaid from '@liradb2000/markdown-it-mermaid'
import { uuid } from '../util/util' 
import { useTextClipboard } from '@/utils/dom'
import 'highlight.js/styles/atom-one-dark.css'

interface IMarkdownItOption {
  isCodeCopy?: boolean
  onTocUpdate?: (toc: any[]) => void
  onImageLink?: (src: string) => void
  onHrefLink?: (href: string) => void
}

let isCodeCopyListenerMounted = false
const markdownCodeCopyIcon = '<svg viewBox="0 0 24 24"><path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"></path></svg>'
const markdownCodeCopiedIcon = '<svg viewBox="0 0 24 24"><path d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"></path></svg>'

const handleMarkdownCodeCopyFeedback = (button: HTMLElement) => {
  const timer = Number(button.dataset.timer || 0)
  if (timer) {
    window.clearTimeout(timer)
  }
  button.classList.add('is-copied')
  button.innerHTML = markdownCodeCopiedIcon
  button.dataset.timer = String(window.setTimeout(() => {
    button.classList.remove('is-copied')
    button.innerHTML = markdownCodeCopyIcon
    delete button.dataset.timer
  }, 1200))
}

const handleMarkdownCodeCopy = async (button: HTMLElement) => {
  const code = decodeURIComponent(button.dataset.code || '')
  if (!code) return
  try {
    await useTextClipboard(code)
    handleMarkdownCodeCopyFeedback(button)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = code
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    handleMarkdownCodeCopyFeedback(button)
  }
}
const handleMarkdownCodeCopyListenerMount = () => {
  if (isCodeCopyListenerMounted || typeof document === 'undefined') return
  isCodeCopyListenerMounted = true
  document.addEventListener('click', event => {
    const target = event.target as HTMLElement | null
    const button = target?.closest?.('.md-btn-copy') as HTMLElement | null
    if (!button) return
    event.preventDefault()
    event.stopPropagation()
    handleMarkdownCodeCopy(button)
  })
}

export default (markdownItOption?: IMarkdownItOption) => {
  if (markdownItOption?.isCodeCopy) {
    handleMarkdownCodeCopyListenerMount()
  }
  const instance = MarkdownIt()
  instance.set({
    html: true,
    breaks: true,
    linkify: true,
    typographer: true,
    highlight: (content, lang) => {
      const language = lang || 'plaintext'
      let copyButton = ''
      if (markdownItOption?.isCodeCopy) {
        copyButton = `<div class="md-btn-copy" data-code="${encodeURIComponent(content)}">${markdownCodeCopyIcon}</div>`
      }
      const languageLabel = `<span class="language">${language}</span>`
      const codeHeader = `<div class="hlhd">${languageLabel}${copyButton}</div>`
      if (lang && hljs.getLanguage(lang)) {
        const highlightContent = hljs.highlight(content, { language: lang, ignoreIllegals: true }).value
        return `<div class="blockoder">${codeHeader}<pre class="hlcx"><code>${highlightContent}</code></pre></div>`
      }
      return `<div class="blockoder">${codeHeader}<pre class="hlcx"><code>${instance.utils.escapeHtml(content)}</code></pre></div>`
    }
  })
  // 添加TOC插件
  instance.use(MarkdownItToc, {
    includeLevel: [1, 2, 3],    // 包含的标题级别
    containerClass: 'toc',      // TOC 容器的 CSS 类
    markerPattern: /^\[toc\]/im // 用于识别 TOC 插入点的模式
  })
  instance.use(MarkdownItTaskList)
  // 添加mermaid插件
  instance.use(MarkdownItMermaid)
  // 添加数学公式插件
  instance.use(markdownItKatex, {
    throwOnError: false,
    errorColor: '#cc0000'
  })
  // 重写标题渲染规则，添加锚点
  instance.renderer.rules.heading_open = (tokens, idx, options, env) => {
    const id = uuid()
    const token = tokens[idx]
    const tag = token.tag
    const level = token.tag.replace('h', '')
    const content = tokens[idx + 1].content
    env.toc = env.toc || []
    env.toc.push({
      id: id,
      level: level,
      content: content
    })
    if (markdownItOption?.onTocUpdate) {
      markdownItOption.onTocUpdate(env.toc)
    }
    return `<${tag} id="${id}"><span class="mask">`
  }
  // 重写链接渲染规则，添加target="_blank"
  instance.renderer.rules.link_open = (tokens, idx, options) => {
    const token = tokens[idx]
    const hrefIndex = token.attrIndex('href')
    const attrs = token.attrs as any
    const href = attrs[hrefIndex][1]
    if (href.startsWith('http')) {
      token.attrPush(['target', '_blank'])
    }
    return instance.renderer.renderToken(tokens, idx, options)
  }
  // 针对img标签新增image-preview类名
  instance.renderer.rules.image = (tokens, idx) => {
    const token = tokens[idx] as any
    const srcIndex = token.attrIndex('src')
    let src = token.attrs[srcIndex][1]
    if (markdownItOption?.onImageLink) {
      src = markdownItOption.onImageLink(src)
    }
    return `<img class="markdown-image-preview" src="${src}" data-src="${src}" alt="${token.content}">`
  }
  return instance
}
