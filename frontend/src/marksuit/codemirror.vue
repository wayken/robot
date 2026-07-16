<template>
  <div class="codemirror" ref="loadContainerRef"></div>
</template>

<script setup lang="ts">
import {
  useMarksuitContext
} from './hook/useMarksuitContext'

const {
  useRichEditor,
  useCodemirror,
  isSourceMode
} = useMarksuitContext()
const loadContainerRef = ref<HTMLElement | null>(null)

// 挂载时初始化 CodeMirror，内容从富文本编辑器同步
onMounted(() => {
  if (!loadContainerRef.value) return
  const content = useRichEditor.useMarkdownContent()
  useCodemirror.mount({
    content: content,
    workspace: unref(loadContainerRef) as HTMLElement
  })
  // 监听 CodeMirror 内容变化，同步回富文本编辑器
  useCodemirror.on('update', (markdown: string) => {
    const instance = useRichEditor.useInstance()
    if (instance.getMarkdown() !== markdown) {
      instance.commands.setContent(markdown, { emitUpdate: true, contentType: 'markdown' })
    }
  })
})
// 退出源码模式时销毁实例
onBeforeUnmount(() => {
  useCodemirror.off('update')
  useCodemirror.destroy()
})

// 每次切换回源码模式时，将最新的富文本内容同步过来
watch(isSourceMode, (value) => {
  if (value && loadContainerRef.value) {
    const content = useRichEditor.useMarkdownContent()
    useCodemirror.setContent(content)
  }
})
</script>
