import {
  Extension
} from '@tiptap/core'
import {
  loadSlashCommandFlatMap,
  type SlashCommandModule
} from './commands'
import {
  flip,
  shift,
  computePosition,
  offset as floatingOffset,
  type Placement
} from '@floating-ui/dom'
import Suggestion, {
  type SuggestionOptions
} from '@tiptap/suggestion'
import type { App } from 'vue'
import i18n from '../../../locale'
import SlashCommandMenu from './command.vue'
import ASvgIcon from '../../../common/svgicon.vue'

export type SlashCommandOptions = {
  suggestion: Omit<SuggestionOptions, 'editor'>
}

class FloatingPopup {
  private el: HTMLElement
  private visible = false

  constructor(content: HTMLElement) {
    this.el = document.createElement('div')
    this.el.style.cssText = [
      'position:fixed',
      'z-index:9999',
      'top:0',
      'left:0',
      'display:none'
    ].join(';')
    this.el.appendChild(content)
    document.body.appendChild(this.el)
  }

  async show(getReferenceClientRect: () => DOMRect | null, placement: Placement = 'bottom-start') {
    const rect = getReferenceClientRect()
    if (!rect) return
    this.el.style.display = 'block'
    this.visible = true

    // 用虚拟参考元素适配 Floating UI
    const virtualEl = {
      getBoundingClientRect: () => rect
    }
    const { x, y } = await computePosition(virtualEl as Element, this.el, {
      placement,
      middleware: [
        floatingOffset(8),
        flip(),
        shift({ padding: 8 })
      ]
    })
    this.el.style.transform = `translate(${x}px,${y}px)`
  }

  async update(getReferenceClientRect: () => DOMRect | null) {
    if (!this.visible) return
    await this.show(getReferenceClientRect)
  }

  hide() {
    this.el.style.display = 'none'
    this.visible = false
  }

  destroy() {
    this.hide()
    this.el.remove()
  }
}

export const SlashCommand = Extension.create<SlashCommandOptions>({
  name: 'SlashCommand',
  addOptions() {
    return {
      suggestion: {
        char: '/',
        startOfLine: false,
        command: ({ editor, range, props }: { editor: any; range: any; props: any }) => {
          props.command({ editor, range })
        },
        items: ({ query }: { query: string }) => {
          const q = query.toLowerCase().trim()
          if (!q) return loadSlashCommandFlatMap
          return loadSlashCommandFlatMap.filter((item) => {
            const nameMatch = item.name.toLowerCase().includes(q)
            const keywordMatch = item.keywords?.some((k) => k.toLowerCase().includes(q))
            return nameMatch || keywordMatch
          })
        },
        render: () => {
          let app: App | null = null
          let mountEl: HTMLElement | null = null
          let popup: FloatingPopup | null = null
          let menuRef: { onKeyDown: (e: KeyboardEvent) => boolean } | null = null
          const modulesRef = ref<SlashCommandModule[]>([])
          const commandRef = ref<((module: SlashCommandModule) => void) | null>(null)
          const WrapperComponent = defineComponent({
            setup(_, { expose }) {
              const innerRef = ref<{ onKeyDown: (e: KeyboardEvent) => boolean } | null>(null)
              const onKeyDown = (e: KeyboardEvent) => innerRef.value?.onKeyDown(e) ?? false
              expose({ onKeyDown })
              return () => h(SlashCommandMenu, {
                ref: innerRef,
                modules: modulesRef.value,
                command: commandRef.value ?? undefined
              })
            }
          })
          let scrollEl: HTMLElement | null = null
          let scrollHandler: (() => void) | null = null
          return {
            onStart(props: any) {
              modulesRef.value = props.items as SlashCommandModule[]
              commandRef.value = (module: SlashCommandModule) => {
                module.command?.({ editor: props.editor, range: props.range })
                popup?.hide()
              }
              mountEl = document.createElement('div')
              app = createApp(WrapperComponent)
              app.use(i18n)
              app.component('ASvgIcon', ASvgIcon)
              const instance = app.mount(mountEl)
              menuRef = instance as any
              popup = new FloatingPopup(mountEl)
              popup.show(props.clientRect)
              // 监听编辑器容器滚动，实时更新菜单位置
              scrollEl = props.editor.view.dom.closest('.content') as HTMLElement ?? props.editor.view.dom.parentElement as HTMLElement
              scrollHandler = () => {
                popup?.update(props.clientRect)
              }
              scrollEl?.addEventListener('scroll', scrollHandler, { passive: true })
            },
            onUpdate(props: any) {
              modulesRef.value = props.items as SlashCommandModule[]
              commandRef.value = (module: SlashCommandModule) => {
                module.command?.({ editor: props.editor, range: props.range })
                popup?.hide()
              }
              popup?.update(props.clientRect)
            },
            onKeyDown(props: any): boolean {
              if (props.event.key === 'Escape') {
                popup?.hide()
                return true
              }
              return menuRef?.onKeyDown?.(props.event) ?? false
            },
            onExit() {
              if (scrollEl && scrollHandler) {
                scrollEl.removeEventListener('scroll', scrollHandler)
                scrollEl = null
                scrollHandler = null
              }
              popup?.destroy()
              app?.unmount()
              popup = null
              app = null
              mountEl = null
              menuRef = null
            }
          }
        }
      }
    }
  },
  addProseMirrorPlugins() {
    return [
      Suggestion({
        editor: this.editor,
        ...this.options.suggestion
      })
    ]
  }
})
