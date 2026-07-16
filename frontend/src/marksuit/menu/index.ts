import { MenuOption } from '../interface'
import MarksuitRichEditor from '../core/index'

export const useMenuOption = (useRichEditor: MarksuitRichEditor, methods?: Record<string, any>): MenuOption[] => [
  {
    name: 'undo',
    icon: 'undo',
    buildin: true,
    isDisabled() {
      return !useRichEditor.useInstance().can().undo()
    },
    onClick() {
      useRichEditor.useCommands().undo().run()
    }
  },
  {
    name: 'redo',
    icon: 'redo',
    buildin: true,
    isDisabled() {
      return !useRichEditor.useInstance().can().redo()
    },
    onClick() {
      useRichEditor.useCommands().redo().run()
    }
  },
  {
    name: 'bold',
    icon: 'bold',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('bold')
    },
    onClick() {
      useRichEditor.useCommands().toggleBold().run()
    }
  },
  {
    name: 'linethrough',
    icon: 'strikethrough',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('strike')
    },
    onClick() {
      useRichEditor.useCommands().toggleStrike().run()
    }
  },
  {
    name: 'underline',
    icon: 'underline',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('underline')
    },
    onClick() {
      useRichEditor.useCommands().toggleUnderline().run()
    }
  },
  {
    name: 'italic',
    icon: 'italic',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('italic')
    },
    onClick() {
      useRichEditor.useCommands().toggleItalic().run()
    }
  },
  {
    name: 'quote',
    icon: 'double-quotes',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('blockquote')
    },
    onClick() {
      useRichEditor.useCommands().toggleBlockquote().run()
    }
  },
  {
    name: 'hr',
    icon: 'horizontal',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('horizontalRule')
    },
    onClick() {
      useRichEditor.useCommands().setHorizontalRule().run()
    }
  },
  {
    name: 'unchecked',
    icon: 'unchecked',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('taskList')
    },
    onClick() {
      useRichEditor.useCommands().toggleTaskList().run()
    }
  },
  {
    name: 'link',
    icon: 'link',
    buildin: false
  },
  {
    name: 'unlink',
    icon: 'unlink',
    buildin: true,
    isActive() {
      return false
    },
    onClick() {
      useRichEditor.useCommands().unsetLink().run()
    }
  },
  {
    name: 'font-color',
    icon: 'font-color',
    buildin: false
  },
  {
    name: 'line-height',
    icon: 'line-height',
    buildin: false
  },
  {
    name: 'image',
    icon: 'image',
    buildin: false
  },
  {
    name: 'table',
    icon: 'table',
    buildin: false
  },
  {
    name: 'ol',
    icon: 'list-ordered',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('orderedList')
    },
    onClick() {
      useRichEditor.useCommands().toggleOrderedList().run()
    }
  },
  {
    name: 'ul',
    icon: 'list-unordered',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('bulletList')
    },
    onClick() {
      useRichEditor.useCommands().toggleBulletList().run()
    }
  },
  {
    name: 'code',
    icon: 'window-code',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('codeBlock')
    },
    onClick() {
      useRichEditor.useCommands().toggleCodeBlock().run()
    }
  },
  {
    name: 'eraser',
    icon: 'eraser',
    buildin: true,
    onClick() {
      useRichEditor.useCommands().unsetAllMarks().run()
    }
  },
  {
    name: 'brush',
    icon: 'brush',
    buildin: false
  },
  {
    name: 'header',
    icon: 'header',
    buildin: false
  },
  {
    name: 'search',
    icon: 'search',
    buildin: true,
    onClick() {
      methods && methods[this.name]()
    }
  },
  {
    name: 'fullscreen',
    icon: 'expand',
    buildin: false
  }
]
