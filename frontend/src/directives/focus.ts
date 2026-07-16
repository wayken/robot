import { ObjectDirective } from 'vue'

const Focus: ObjectDirective = {
  mounted(el) {
    let dom = el.querySelector('input')
    if (dom) {
      dom.focus()
    }
    dom = el.querySelector('textarea')
    if (dom) {
      dom.focus()
    }
  }
}

export default Focus
