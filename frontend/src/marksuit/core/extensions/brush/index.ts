import {
  Extension
} from '@tiptap/core'
import type {
  Mark
} from '@tiptap/pm/model'
import {
  Plugin,
  PluginKey
} from '@tiptap/pm/state'

interface FormatBrushOptions {
  lastSelectionMarks: Mark[] | null
}

interface FormatBrushStorage {
  isDoubleClick: boolean
  isBrushActive: boolean
  isBrushActiveRef: ReturnType<typeof ref<boolean>>
  formatBrushPluginKey: PluginKey
}

interface CopyFormatOptions {
  type: 'click' | 'dblclick'
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    formatBrush: {
      copyFormat: (data: CopyFormatOptions) => ReturnType
      applyFormat: () => ReturnType
      cancelFormat: () => ReturnType
    }
  }
  interface Storage {
    formatBrush: FormatBrushStorage
  }
}

// 从选区中收集所有 Mark
function handleCollectMarksFromSelection(editor: any): Mark[] {
  const { state } = editor
  const { from, to, empty } = state.selection
  const marks: Mark[] = []
  const seen = new Set<string>()
  if (empty) {
    // 无选区时取光标处的 storedMarks 或当前节点的 marks
    const storedMarks = state.storedMarks || state.selection.$from.marks()
    storedMarks.forEach((mark: Mark) => {
      const key = mark.type.name + JSON.stringify(mark.attrs)
      if (!seen.has(key)) {
        seen.add(key)
        marks.push(mark)
      }
    })
  } else {
    state.doc.nodesBetween(from, to, (node: any) => {
      node.marks.forEach((mark: Mark) => {
        const key = mark.type.name + JSON.stringify(mark.attrs)
        if (!seen.has(key)) {
          seen.add(key)
          marks.push(mark)
        }
      })
    })
  }
  return marks
}

// 设置编辑器容器的光标样式
function useEditorCursor(editor: any, cursor: string) {
  const el = editor.view?.dom as HTMLElement | undefined
  if (el) {
    el.style.cursor = cursor
  }
}

export const FormatBrush = Extension.create<FormatBrushOptions, FormatBrushStorage>({
  name: 'formatBrush',

  addOptions() {
    return {
      lastSelectionMarks: null // 存储最后一次选中格式信息
    }
  },

  addStorage() {
    return {
      isDoubleClick: false,          // 双击状态（持续模式）
      isBrushActive: false,          // 格式刷模式是否激活
      isBrushActiveRef: ref(false),  // Vue 响应式引用，供菜单组件订阅
      formatBrushPluginKey: new PluginKey('formatBrush')
    }
  },

  addCommands() {
    return {
      // 复制当前选区格式，激活格式刷，操作如下：1. 单次应用后自动退出、2. 双击时持续应用，直到手动取消
      copyFormat: ({ type }: CopyFormatOptions) => ({ editor }) => {
        const marks = handleCollectMarksFromSelection(editor)
        // 没有可复制的格式时直接退出
        if (marks.length === 0) return false
        this.options.lastSelectionMarks = marks
        this.storage.isBrushActive = true
        this.storage.isBrushActiveRef.value = true
        this.storage.isDoubleClick = type === 'dblclick'
        useEditorCursor(editor, 'copy')
        return true
      },
      // 将已复制的格式应用到当前选区
      applyFormat: () => ({ editor, tr, dispatch }) => {
        if (!this.storage.isBrushActive) return false
        const marks = this.options.lastSelectionMarks
        if (!marks || marks.length === 0) return false
        const { from, to, empty } = editor.state.selection
        if (empty) return false
        if (dispatch) {
          // 先清除选区内已有的所有 marks，再应用新格式
          marks.forEach((mark: Mark) => {
            tr.removeMark(from, to, mark.type)
          })
          marks.forEach((mark: Mark) => {
            tr.addMark(from, to, mark)
          })
          dispatch(tr)
        }
        // 单次模式：应用后自动退出
        if (!this.storage.isDoubleClick) {
          this.storage.isBrushActive = false
          this.storage.isBrushActiveRef.value = false
          this.options.lastSelectionMarks = null
          useEditorCursor(editor, '')
        }
        return true
      },
      // 取消格式刷模式
      cancelFormat: () => ({ editor }) => {
        this.storage.isBrushActive = false
        this.storage.isBrushActiveRef.value = false
        this.storage.isDoubleClick = false
        this.options.lastSelectionMarks = null
        useEditorCursor(editor, '')
        return true
      }
    }
  },
  addProseMirrorPlugins() {
    const storage = this.storage
    const extensionThis = this
    return [
      new Plugin({
        key: this.storage.formatBrushPluginKey,
        props: {
          // 监听鼠标抬起：格式刷激活时，在用户完成选区后应用格式
          handleDOMEvents: {
            mouseup: () => {
              if (!storage.isBrushActive) return false
              // 延迟一帧，确保 ProseMirror 已更新选区
              setTimeout(() => {
                const { selection } = extensionThis.editor.state
                if (!selection.empty) {
                  extensionThis.editor.commands.applyFormat()
                }
              }, 0)
              return false
            },
            // 监听键盘 Escape：退出格式刷模式
            keydown: (_view, event) => {
              if (storage.isBrushActive && event.key === 'Escape') {
                extensionThis.editor.commands.cancelFormat()
                return true
              }
              return false
            }
          }
        }
      })
    ]
  }
})
