import {
  VueNodeViewRenderer
} from '@tiptap/vue-3'
import ImageView from './view.vue'
import TiptapImage from '@tiptap/extension-image'

export const Images = TiptapImage.extend({
  inline() {
    return true
  },
  group() {
    return 'inline'
  },
  addAttributes() {
    return {
      ...this.parent?.(),
      width: {
        default: null,
        parseHTML: element => {
          const width = element.style.width || element.getAttribute('width') || null
          return width == null ? null : parseInt(width, 10)
        },
        renderHTML: attributes => {
          return {
            width: attributes.width
          }
        }
      },
      height: {
        default: null,
        parseHTML: element => {
          const height = element.style.height || element.getAttribute('height') || null
          return height == null ? null : parseInt(height, 10)
        },
        renderHTML: attributes => {
          return {
            height: attributes.height
          }
        }
      }
    }
  },
  addNodeView() {
    return VueNodeViewRenderer(ImageView)
  },
  parseHTML() {
    return [
      {
        tag: 'img[src]'
      }
    ]
  }
})
