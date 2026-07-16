import type {
  Range,
  Editor
} from '@tiptap/core'

export interface SlashCommandModule {
  name: string
  icon: string
  color?: string
  keywords?: string[]
  command?: (props: { editor: Editor; range: Range }) => void
}

export interface SlashCommandList {
  label: string
  modules: SlashCommandModule[]
}

export const loadSlashCommandList: SlashCommandList[] = [
  {
    label: 'basic',
    modules: [
      {
        name: 'text',
        icon: 'text',
        color: '#4954e6',
        keywords: ['text', 'paragraph'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).setParagraph().run()
        }
      },
      {
        name: 'h1',
        icon: 'heading-h1',
        color: '#4954e6',
        keywords: ['h1', 'heading1'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).setHeading({ level: 1 }).run()
        }
      },
      {
        name: 'h2',
        icon: 'heading-h2',
        color: '#4954e6',
        keywords: ['h2', 'heading2'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).setHeading({ level: 2 }).run()
        }
      },
      {
        name: 'h3',
        icon: 'heading-h3',
        color: '#4954e6',
        keywords: ['h3', 'heading3'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).setHeading({ level: 3 }).run()
        }
      },
      {
        name: 'quote',
        icon: 'double-quotes',
        color: '#8a49fe',
        keywords: ['quote', 'blockquote'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).toggleBlockquote().run()
        }
      },
      {
        name: 'code',
        icon: 'window-code',
        color: '#3ad1ff',
        keywords: ['code', 'codeblock'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).toggleCodeBlock().run()
        }
      }
    ]
  },
  {
    label: 'commonly',
    modules: [
      {
        name: 'ul',
        icon: 'list-unordered',
        color: '#f01d94',
        keywords: ['ul', 'bullet', 'list'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).toggleBulletList().run()
        }
      },
      {
        name: 'ol',
        icon: 'list-ordered',
        color: '#ff9e30',
        keywords: ['ol', 'ordered', 'list'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).toggleOrderedList().run()
        }
      },
      {
        name: 'todo',
        icon: 'unchecked',
        color: '#4954e6',
        keywords: ['todo', 'task', 'checklist'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).toggleTaskList().run()
        }
      },
      {
        name: 'image',
        icon: 'image',
        color: '#66d55a',
        keywords: ['img', 'image']
      }
    ]
  },
  {
    label: 'advanced',
    modules: [
      {
        name: 'table',
        icon: 'table',
        color: '#7f3bf5',
        keywords: ['table'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).insertTable({
            rows: 3,
            cols: 3,
            withHeaderRow: true
          }).run()
        }
      },
      {
        name: 'hr',
        icon: 'horizontal',
        color: '#2e5b8c',
        keywords: ['hr', 'divider', 'rule'],
        command: ({ editor, range }) => {
          editor.chain().focus().deleteRange(range).setHorizontalRule().run()
        }
      }
    ]
  }
]

export const loadSlashCommandFlatMap: SlashCommandModule[] = loadSlashCommandList.flatMap((s) => s.modules)
