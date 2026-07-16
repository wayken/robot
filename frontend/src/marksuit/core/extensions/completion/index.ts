import {
  Extension
} from '@tiptap/core'
import {
  Plugin,
  PluginKey
} from '@tiptap/pm/state'
import {
  Decoration,
  DecorationSet
} from '@tiptap/pm/view'
import {
  CompletionContext
} from '../../../interface'

interface CompletionOptions {
  // 触发补全时由上层业务消费的回调
  onCompletion?: (context: CompletionContext) => void
}

interface CompletionStorage {
  ghostText: string         // 当前展示的 ghost 文本
  position: number          // ghost 锚点位置（光标位置）
  requestId: number         // 用于判定上层异步回调是否已过期
  finished: boolean         // 上层是否已经显式 finish
  cancelHandlers: (() => void)[] // 上层注册的取消回调
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    completion: {
      triggerCompletion: () => ReturnType
      appendCompletion: (chunk: string) => ReturnType
      replaceCompletion: (text: string) => ReturnType
      finishCompletion: () => ReturnType
      acceptCompletion: () => ReturnType
      cancelCompletion: () => ReturnType
    }
  }
  interface Storage {
    completion: CompletionStorage
  }
}

const completionPluginKey = new PluginKey('completion')
// 元数据标记，表示该 transaction 来自补全扩展自身（仅刷新装饰，不应触发取消逻辑）
const META_INTERNAL = 'completion-internal'

export const Completion = Extension.create<CompletionOptions, CompletionStorage>({
  name: 'completion',

  // 高优先级以确保 Tab/Escape 键位拦截先于列表等扩展
  priority: 1000,

  addOptions() {
    return {
      onCompletion: undefined
    }
  },

  addStorage() {
    return {
      ghostText: '',
      position: -1,
      requestId: 0,
      finished: false,
      cancelHandlers: []
    }
  },

  addCommands() {
    return {
      // 触发补全：收集上下文并交给上层业务处理
      triggerCompletion: () => ({ editor }) => {
        const { state } = editor
        const { selection } = state
        // 仅在光标折叠（非选区）时触发
        if (!selection.empty) return false
        // 补全进行中时忽略重复触发
        if (this.storage.position >= 0) return false
        const pos = selection.from

        // 上下文信息：当前块文本 + 整文档前后文
        const $pos = state.doc.resolve(pos)
        const blockStart = $pos.start($pos.depth)
        const prompt = state.doc.textBetween(blockStart, pos, '\n', '\n')
        const before = state.doc.textBetween(0, pos, '\n', '\n')
        const after = state.doc.textBetween(pos, state.doc.content.size, '\n', '\n')

        const requestId = this.storage.requestId + 1
        this.storage.requestId = requestId
        this.storage.position = pos
        this.storage.ghostText = ''
        this.storage.finished = false
        this.storage.cancelHandlers = []

        const isStale = () => this.storage.requestId !== requestId

        // 刷新 decoration 状态 —— 必须在当前 dispatch 结束后异步执行，否则嵌套 dispatch 报错
        const dispatchUpdate = () => {
          // 使用 requestAnimationFrame 确保不会嵌套 dispatch
          requestAnimationFrame(() => {
            if (isStale()) return
            try {
              const tr = editor.state.tr.setMeta(completionPluginKey, { type: 'update' })
              tr.setMeta(META_INTERNAL, true)
              tr.setMeta('addToHistory', false)
              tr.setMeta('preventUpdate', true)
              editor.view.dispatch(tr)
            } catch (e) {
              // 编辑器可能已销毁
            }
          })
        }

        const context: CompletionContext = {
          prompt,
          before,
          after,
          append: (chunk: string) => {
            if (isStale() || typeof chunk !== 'string' || chunk.length === 0) return
            this.storage.ghostText += chunk
            dispatchUpdate()
          },
          replace: (text: string) => {
            if (isStale() || typeof text !== 'string') return
            this.storage.ghostText = text
            dispatchUpdate()
          },
          finish: () => {
            if (isStale()) return
            this.storage.finished = true
          },
          cancel: () => {
            if (isStale()) return
            editor.commands.cancelCompletion()
          },
          onCancel: (callback: () => void) => {
            if (isStale() || typeof callback !== 'function') return
            this.storage.cancelHandlers.push(callback)
          }
        }

        try {
          this.options.onCompletion?.(context)
        } catch (error) {
          console.warn('[Marksuit] Completion onCompletion handler failed:', error)
          editor.commands.cancelCompletion()
          return false
        }
        // 触发初始 dispatch 显示 loading 动画
        dispatchUpdate()
        return true
      },

      // 直接由编辑器命令追加补全（与 context.append 等价，便于扩展使用）
      appendCompletion: (chunk: string) => ({ editor }) => {
        if (this.storage.position < 0) return false
        if (typeof chunk !== 'string' || chunk.length === 0) return false
        this.storage.ghostText += chunk
        requestAnimationFrame(() => {
          try {
            const tr = editor.state.tr.setMeta(completionPluginKey, { type: 'update' })
            tr.setMeta(META_INTERNAL, true)
            tr.setMeta('addToHistory', false)
            tr.setMeta('preventUpdate', true)
            editor.view.dispatch(tr)
          } catch (e) { /* editor destroyed */ }
        })
        return true
      },

      replaceCompletion: (text: string) => ({ editor }) => {
        if (this.storage.position < 0) return false
        this.storage.ghostText = text
        requestAnimationFrame(() => {
          try {
            const tr = editor.state.tr.setMeta(completionPluginKey, { type: 'update' })
            tr.setMeta(META_INTERNAL, true)
            tr.setMeta('addToHistory', false)
            tr.setMeta('preventUpdate', true)
            editor.view.dispatch(tr)
          } catch (e) { /* editor destroyed */ }
        })
        return true
      },

      finishCompletion: () => () => {
        if (this.storage.position < 0) return false
        this.storage.finished = true
        return true
      },

      // 接受补全：将 ghost 文本插入文档并清空状态
      acceptCompletion: () => ({ tr, dispatch }) => {
        const text = this.storage.ghostText
        const pos = this.storage.position
        if (!text || pos < 0) return false
        if (dispatch) {
          tr.insertText(text, pos)
          dispatch(tr)
        }
        // 增加 requestId 让在途异步回调全部失效
        this.storage.requestId += 1
        this.storage.position = -1
        this.storage.ghostText = ''
        this.storage.finished = false
        this.storage.cancelHandlers = []
        return true
      },

      // 取消补全：清空 ghost 文本并触发上层 onCancel 回调
      cancelCompletion: () => ({ editor }) => {
        if (this.storage.position < 0 && !this.storage.ghostText && this.storage.cancelHandlers.length === 0) {
          return false
        }
        const handlers = this.storage.cancelHandlers
        this.storage.requestId += 1
        this.storage.position = -1
        this.storage.ghostText = ''
        this.storage.finished = false
        this.storage.cancelHandlers = []
        // 触发上层取消回调（如清理 setInterval / abort fetch 等）
        handlers.forEach((handler) => {
          try {
            handler()
          } catch (error) {
            console.warn('[Marksuit] completion cancel handler failed:', error)
          }
        })
        // 异步刷新 decoration，避免嵌套 dispatch
        requestAnimationFrame(() => {
          try {
            const tr = editor.state.tr.setMeta(completionPluginKey, { type: 'update' })
            tr.setMeta(META_INTERNAL, true)
            tr.setMeta('addToHistory', false)
            tr.setMeta('preventUpdate', true)
            editor.view.dispatch(tr)
          } catch (e) {
            // 编辑器可能已销毁
          }
        })
        return true
      }
    }
  },

  // 键位绑定：Ctrl/Cmd+I 触发，Tab 接受，Escape 取消
  addKeyboardShortcuts() {
    return {
      'Mod-/': () => this.editor.commands.triggerCompletion(),
      'Tab': () => {
        if (this.storage.position >= 0 && this.storage.ghostText) {
          return this.editor.commands.acceptCompletion()
        }
        return false
      },
      'Escape': () => {
        if (this.storage.position >= 0) {
          return this.editor.commands.cancelCompletion()
        }
        return false
      }
    }
  },

  addProseMirrorPlugins() {
    const storage = this.storage
    const editor = this.editor
    return [
      new Plugin({
        key: completionPluginKey,
        state: {
          init() {
            return DecorationSet.empty
          },
          apply(tr, _old, _oldState, newState) {
            // 未激活：保持空集
            if (storage.position < 0) {
              return DecorationSet.empty
            }
            // 文档变更（且非补全自身的更新）→ 视为用户输入，取消补全
            const isInternal = !!tr.getMeta(META_INTERNAL)
            if (tr.docChanged && !isInternal) {
              // 异步触发 cancel，避免在 apply 中再次 dispatch
              setTimeout(() => editor.commands.cancelCompletion(), 0)
              return DecorationSet.empty
            }
            // 同步映射 ghost 锚点
            const newPos = tr.mapping.map(storage.position)
            storage.position = newPos
            // 光标偏离锚点 → 取消
            const sel = newState.selection
            if (!sel.empty || sel.from !== newPos) {
              setTimeout(() => editor.commands.cancelCompletion(), 0)
              return DecorationSet.empty
            }
            // 还没有任何文本时显示 loading 动画
            if (!storage.ghostText) {
              const loadingWidget = Decoration.widget(newPos, () => {
                const span = document.createElement('span')
                span.className = 'x-completion-loading'
                // 三个跳动的点
                for (let i = 0; i < 3; i++) {
                  const dot = document.createElement('span')
                  dot.className = 'dot'
                  dot.style.animationDelay = `${i * 0.15}s`
                  span.appendChild(dot)
                }
                return span
              }, {
                side: 1,
                ignoreSelection: true,
                key: `loading-${storage.requestId}`
              })
              return DecorationSet.create(newState.doc, [loadingWidget])
            }
            // 用 widget decoration 在光标位置渲染 ghost 文本
            const widget = Decoration.widget(newPos, () => {
              const span = document.createElement('span')
              span.className = 'x-completion-shadow'
              span.textContent = storage.ghostText
              return span
            }, {
              side: 1,
              ignoreSelection: true,
              key: `ghost-${storage.requestId}-${storage.ghostText.length}`
            })
            return DecorationSet.create(newState.doc, [widget])
          }
        },
        props: {
          decorations(state) {
            return completionPluginKey.getState(state)
          },
          handleDOMEvents: {
            // 编辑器失焦：取消补全，避免 ghost 残留
            blur: () => {
              if (storage.position >= 0) {
                setTimeout(() => editor.commands.cancelCompletion(), 0)
              }
              return false
            }
          }
        }
      })
    ]
  }
})
