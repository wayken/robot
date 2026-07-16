import {
  common,
  createLowlight
} from 'lowlight'
import {
  VueNodeViewRenderer
} from '@tiptap/vue-3'
import {
  textblockTypeInputRule
} from '@tiptap/core'
import { Component } from 'vue'
import 'highlight.js/styles/github.css'
import ACodeBlockView from './view.vue'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'

const lowlight = createLowlight(common)
const backtickInputRegex = /^```(\w+)?\s$/

export const CodeBlockLights = CodeBlockLowlight.configure({
  lowlight
}).extend({
  name: 'codeBlock',
  addAttributes() {
    return {
      ...this.parent?.(),
      language: {
        default: 'html'
      }
    }
  },
  addNodeView() {
    return VueNodeViewRenderer(ACodeBlockView as Component)
  },
  addInputRules() {
    return [
      textblockTypeInputRule({
        find: backtickInputRegex,
        type: this.type,
        getAttributes: (match) => ({
          language: match[1] ?? 'html'
        })
      })
    ]
  }
})
