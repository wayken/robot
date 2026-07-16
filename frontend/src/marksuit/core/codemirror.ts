import EventEmitter from 'eventemitter3'
import {
  keymap,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter
} from '@codemirror/view'
import {
  EditorState
} from '@codemirror/state'
import {
  history,
  defaultKeymap,
  historyKeymap
} from '@codemirror/commands'
import {
  markdown,
  markdownLanguage
} from '@codemirror/lang-markdown'
import {
  languages
} from '@codemirror/language-data'
import {
  syntaxHighlighting,
  defaultHighlightStyle
} from '@codemirror/language'
import {
  MarksuitCodemirrorOption
} from '../interface'

class MarksuitCodemirror extends EventEmitter {
  private codemirror: EditorView | null = null

  public mount(options: MarksuitCodemirrorOption) {
    if (this.codemirror) {
      this.codemirror.destroy()
    }
    const updateListener = EditorView.updateListener.of((update) => {
      if (update.docChanged) {
        this.emit('update', this.codemirror?.state.doc.toString() ?? '')
      }
    })
    const state = EditorState.create({
      doc: options.content || '',
      extensions: [
        history(),
        highlightActiveLine(),
        highlightActiveLineGutter(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        markdown({ base: markdownLanguage, codeLanguages: languages }),
        syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
        EditorView.lineWrapping,
        updateListener
      ]
    })
    this.codemirror = new EditorView({ state, parent: options.workspace })
  }

  public focus() {
    this.codemirror?.focus()
  }

  public useContent(): string {
    return this.codemirror?.state.doc.toString() ?? ''
  }

  /**
   * 更新编辑器内容（不触发 update 事件）
   * 
   * @param content 文本内容
   */
  public setContent(content: string) {
    if (!this.codemirror) return
    const currentContent = this.codemirror.state.doc.toString()
    if (currentContent === content) return
    this.codemirror.dispatch({
      changes: { from: 0, to: this.codemirror.state.doc.length, insert: content }
    })
  }

  public destroy() {
    this.codemirror?.destroy()
    this.codemirror = null
  }
}

export default MarksuitCodemirror
