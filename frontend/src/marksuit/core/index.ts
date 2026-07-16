import {
  Editor,
  Extensions
} from '@tiptap/vue-3'
import {
  Table,
  TableRow,
  TableHeader
} from '@tiptap/extension-table'
import {
  TaskList,
  TaskItem
} from '@tiptap/extension-list'
import {
  LinkHover
} from './extensions/link/index'
import {
  Markdown
} from '@tiptap/markdown'
import {
  Placeholder
} from './extensions/placeholder/index'
import {
  SlashCommand
} from './extensions/slashcommand/index'
import {
  CustomTableCell
} from './extensions/table/index'
import {
  CodeBlockLights
} from './extensions/codeblock/index'
import {
  Images
} from './extensions/image/index'
import {
  FormatBrush
} from './extensions/brush/index'
import {
  LineHeight
} from './extensions/lineheight/index'
import {
  Color
} from '@tiptap/extension-color'
import {
  TextStyle
} from '@tiptap/extension-text-style'
import {
  Completion
} from './extensions/completion/index'
import {
  MarksuitEditorOption
} from '../interface'
import EventEmitter from 'eventemitter3'
import StarterKit from '@tiptap/starter-kit'

class MarksuitRichEditor extends EventEmitter {
  private instance: Editor

  private extensions: Extensions

  constructor(options: MarksuitEditorOption) {
    super()
    this.extensions = [
      StarterKit.configure({
        link: false,
        codeBlock: false,
        blockquote: {
          HTMLAttributes: {
            class: 'x-blockquote'
          }
        }
      }),
      Table.configure({ resizable: true }),
      TableRow,
      TableHeader,
      LinkHover.configure({
        openOnClick: true,
        HTMLAttributes: {
          class: 'x-link'
        }
      }),
      CustomTableCell,
      TaskList,
      TaskItem.configure({ nested: true }),
      CodeBlockLights,
      Images,
      FormatBrush,
      LineHeight,
      TextStyle,
      Color,
      ...(options.isAutoCompletion !== false ? [Completion.configure({
        onCompletion: (context) => {
          this.emit('completion', context)
        }
      })] : []),
      Markdown.configure({
        markedOptions: {
          gfm: true,
          breaks: false
        }
      }),
      Placeholder.configure({
        showOnlyCurrent: true,
        includeChildren: false,
        showOnlyWhenEditable: true,
        placeholder: options.placeholder
      }),
      SlashCommand.configure({})
    ]
    this.instance = new Editor({
      content: options.content,
      contentType: 'markdown',
      extensions: this.extensions,
      editorProps: {
        handlePaste: (view, event) => {
          const items = event.clipboardData?.items
          if (!items || !options.onImageUpload) return false
          for (const item of Array.from(items)) {
            if (!item.type.startsWith('image/')) {
              continue
            }
            const file = item.getAsFile()
            if (!file) continue
            event.preventDefault()
            options.onImageUpload(file).then((url) => {
              if (!url) return
              // 获取图片自身宽度，插入时以原始宽度为准，超出容器宽度则等比缩小
              const img = new Image()
              img.onload = () => {
                let w = img.naturalWidth
                let h = img.naturalHeight
                // 取编辑器容器的实际可用宽度作为上限
                const containerWidth = this.instance.view.dom.clientWidth
                if (containerWidth > 0 && w > containerWidth) {
                  w = containerWidth
                  h = Math.round(h * containerWidth / w)
                }
                this.instance.chain().focus().setImage({
                  src: url,
                  width: w,
                  height: h
                } as any).run()
              }
              img.onerror = () => {
                // 加载失败时不传宽高，由 extension 默认值接管
                this.instance.chain().focus().setImage({ src: url } as any).run()
              }
              img.src = url
            }).catch((err) => {
              console.warn('[Marksuit] Image upload failed:', err)
            })
            return true
          }
          return false
        }
      },
      onCreate: ({ editor: currentEditor }) => {
        try {
          // 直接通过 ProseMirror view 设置初始光标，并标记 addToHistory: false
          // 避免触发 TipTap 的 onUpdate 事件以及污染 undo/redo 历史
          const { view, state } = currentEditor
          const { tr } = state
          tr.setMeta('addToHistory', false)
          tr.setMeta('preventUpdate', true)
          view.dispatch(tr)
          view.focus()
        } catch (error) {
          console.warn('Could not set cursor to start:', error as Error)
        }
      },
      onUpdate: ({ editor: currentEditor, transaction }) => {
        if (transaction.getMeta('preventUpdate')) return
        const content = options.kind === 'markup' ? currentEditor.getHTML() : currentEditor.getMarkdown()
        this.emit('update', content)
        // Ctrl+A 全选后删除内容，文档变为空但文档仍是全选状态
        // 检测到内容为空时将光标折叠到文档起始位置，避免残留全选态
        const { state } = currentEditor
        const { selection, doc } = state
        const isEmpty = doc.textContent.trim() === '' && doc.childCount <= 1
        if (isEmpty && !selection.empty) {
          currentEditor.commands.focus('start')
        }
      }
    })
  }

  public useInstance() {
    return this.instance
  }

  public useCommands() {
    return this.instance.chain().focus()
  }

  public useMarkdownContent() {
    return this.instance.getMarkdown()
  }

  public useHtmlContent() {
    return this.instance.getHTML()
  }

  public destroy() {
    this.instance.destroy()
  }
}

export default MarksuitRichEditor
