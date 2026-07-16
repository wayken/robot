import {
  Link
} from '@tiptap/extension-link'
import {
  Plugin,
  PluginKey
} from '@tiptap/pm/state'

export const LINK_HOVER_EVENT = 'marksuit:link-hover'
export const LINK_LEAVE_EVENT = 'marksuit:link-leave'

export interface LinkHoverPayload {
  to: number
  href: string
  from: number
  domRect: DOMRect
}

const linkHoverPluginKey = new PluginKey('linkHover')

export const LinkHover = Link.extend({
  addProseMirrorPlugins() {
    const parent = this.parent?.() ?? []

    const hoverPlugin = new Plugin({
      key: linkHoverPluginKey,
      props: {
        handleDOMEvents: {
          mouseout(view, event) {
            const target = event.target as HTMLElement
            if (!target.closest('a.x-link')) return false
            const related = (event as MouseEvent).relatedTarget as HTMLElement | null
            if (related && target.closest('a.x-link') === related.closest('a.x-link')) {
              return false
            }
            view.dom.dispatchEvent(new CustomEvent(LINK_LEAVE_EVENT, { bubbles: true }))
            return false
          },
          mouseover(view, event) {
            const target = event.target as HTMLElement
            const linkEl = target.closest('a.x-link') as HTMLAnchorElement | null
            if (!linkEl) return false
            const pos = view.posAtDOM(linkEl, 0)
            if (pos < 0) return false
            const linkMark = view.state.schema.marks.link
            if (!linkMark) return false
            const node = view.state.doc.nodeAt(pos)
            if (!node) return false
            const mark = node.marks.find(m => m.type === linkMark)
            if (!mark) return false
            let from = pos
            let to = pos + node.nodeSize
            let searchPos = pos - 1
            while (searchPos >= 0) {
              const n = view.state.doc.nodeAt(searchPos)
              if (n && n.marks.some(m => m.type === linkMark && m.attrs.href === mark.attrs.href)) {
                from = searchPos
                searchPos -= n.nodeSize
              } else {
                break
              }
            }
            searchPos = pos + node.nodeSize
            while (searchPos <= view.state.doc.content.size) {
              const n = view.state.doc.nodeAt(searchPos)
              if (n && n.marks.some(m => m.type === linkMark && m.attrs.href === mark.attrs.href)) {
                to = searchPos + n.nodeSize
                searchPos += n.nodeSize
              } else {
                break
              }
            }
            const payload: LinkHoverPayload = {
              href: mark.attrs.href ?? '',
              from,
              to,
              domRect: linkEl.getBoundingClientRect()
            }
            view.dom.dispatchEvent(
              new CustomEvent<LinkHoverPayload>(LINK_HOVER_EVENT, {
                bubbles: true,
                detail: payload
              })
            )
            return false
          }
        }
      }
    })
    return [...parent, hoverPlugin]
  }
})
