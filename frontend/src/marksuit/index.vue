<template>
  <div class="a-marksuit"
    :style="{
      height: height
    }"
    :class="{
      'is-preview': !isEditable,
      'is-fullscreen': isFullscreen
    }"
  >
    <!-- 顶部工具栏 -->
    <Menu v-if="!isSourceMode && isEditable" :value="menus"></Menu>
    <!-- 浮动工具栏 -->
    <Bubble v-if="!isSourceMode && isEditable"></Bubble>
    <!-- 编辑器内容：富文本模式 -->
    <div class="a-marksuit-content" v-show="!isSourceMode">
      <EditorContent :editor="useEditor" class="wrapper"
        :style="{
          maxWidth: option.maxWidth
        }"
      />
    </div>
    <!-- 编辑器内容：源码模式 -->
    <div class="a-marksuit-content" v-if="isSourceMode">
      <Codemirror v-if="isSourceMode"
        :style="{
          maxWidth: option.maxWidth
        }"
      ></Codemirror>
    </div>
    <!-- 底部状态栏 -->
    <Statusbar v-if="isEditable"></Statusbar>
  </div>
</template>

<script setup lang="ts">
import {
  EditorContent
} from '@tiptap/vue-3'
import {
  provideMarksuitContext
} from './hook/useMarksuitContext'
import {
  CompletionContext
} from './interface'
import Menu from './menu.vue'
import Bubble from './bubble.vue'
import Statusbar from './statusbar.vue'
import Codemirror from './codemirror.vue'

const props = defineProps({
  // 编辑器高度
  height: {
    type: [String],
    default: '100%'
  },
  // 内容类型
  kind: {
    type: String as PropType<'markdown' | 'markup'>,
    default: 'markdown'
  },
  // 编辑器内容
  content: {
    type: String,
    default: ''
  },
  // 配置工具栏
  menus: String,
  // 是否可编辑
  isEditable: {
    type: Boolean,
    default: true
  },
  // 编辑器配置
  option: {
    type: Object,
    default: () => ({
      maxWidth: '100%',
      placeholder: true,
      isAutoCompletion: false
    })
  }
})

const i18n = useI18n()

const handleEmit = defineEmits<{
  'update:content': [value: string]
  'upload': [file: File, resolve: (url: string) => void]
  'completion': [context: CompletionContext]
}>()
const handleContentUpdate = (content: string) => {
  handleEmit('update:content', content)
}
const handleImageUpload = (file: File): Promise<string> => {
  return new Promise((resolve) => {
    handleEmit('upload', file, resolve)
  })
}
const handleAutoCompletion = (context: CompletionContext) => {
  handleEmit('completion', context)
}

const {
  useRichEditor,
  useCodemirror,
  isFullscreen,
  isSourceMode
} = provideMarksuitContext(
  {
    kind: props.kind,
    content: props.content,
    placeholder: i18n.t('marksuit.editor.placeholder-command'),
    isAutoCompletion: props.option?.isAutoCompletion || false
  },
  handleContentUpdate,
  handleImageUpload,
  handleAutoCompletion
)
const useEditor = useRichEditor.useInstance()
useEditor.setEditable(props.isEditable)

onUnmounted(() => {
  useRichEditor.destroy()
})

// 监听外部内容变化时更新编辑器内容
watch(() => props.content,
  (value) => {
    console.log('content change:', value)
    const instance = useRichEditor.useInstance()
    if (props.kind === 'markup') {
      if (instance && instance.getHTML() !== value) {
        instance.commands.setContent(value, { emitUpdate: false })
        instance.commands.focus('start')
      }
    } else {
      if (instance && instance.getMarkdown() !== value) {
        instance.commands.setContent(value, { emitUpdate: false, contentType: 'markdown' })
        instance.commands.focus('start')
      }
    }
  }
)
// 监听切换编辑器的可编辑状态
watch(() => props.isEditable,
  (value) => {
    const instance = useRichEditor.useInstance()
    if (instance) {
      instance.setEditable(value)
    }
  }
)
// 源码/富文本模式切换时自动获取焦点
watch(isSourceMode, (value) => {
  if (value) {
    nextTick(() => {
      useCodemirror.focus()
    })
  } else {
    nextTick(() => {
      useRichEditor.useInstance().commands.focus('start')
    })
  }
})

defineExpose({ useRichEditor })
</script>
