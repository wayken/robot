<template>
  <el-tooltip effect="dark" placement="top-start"
    :content="$t('marksuit.menu.' + option.name)"
  >
    <div class="block"
      :class="{
        'is-actived': isLinkActived
      }"
      @mousedown.prevent
      @click="handleOpenLink"
    >
      <a-svg-icon :icon-class="`marksuit-${option.icon}`" />
    </div>
  </el-tooltip>
  <a-link :visible="isLinkView" :default-url="loadActiveLinkUrl"
    @close="isLinkView = false"
    @submit="handleLinkAdd"
  ></a-link>
</template>

<script setup lang="ts">
import {
  ToolTemplateProps
} from '../../interface'
import {
  useMarksuitContext
} from '../../hook/useMarksuitContext'
import ALink from './modal.vue'
import ASvgIcon from '../../common/svgicon.vue'

defineProps<ToolTemplateProps>()

const {
  useRichEditor
} = useMarksuitContext()

const isLinkView = ref(false)
const snapshotIsLinkActived = ref(false)
const snapshotSelection = ref<{ from: number; to: number } | null>(null)

const isLinkActived = computed(() => {
  return useRichEditor.useInstance().isActive('link')
})

const loadActiveLinkUrl = computed(() => {
  if (!isLinkActived.value) return ''
  const attributes = useRichEditor.useInstance().getAttributes('link')
  return attributes?.href ?? ''
})

const handleOpenLink = () => {
  const editor = useRichEditor.useInstance()
  const { selection } = editor.state
  // 快照：是否在链接内、当前选区范围
  snapshotIsLinkActived.value = isLinkActived.value
  snapshotSelection.value = { from: selection.from, to: selection.to }
  isLinkView.value = true
}

const handleLinkAdd = (data: { url: string; name: string }) => {
  isLinkView.value = false
  const editor = useRichEditor.useInstance()
  const snapshot = snapshotSelection.value
  // 光标在已有链接内 —— 扩展选区到整个链接节点后更新 href
  if (snapshotIsLinkActived.value && snapshot) {
    editor
      .chain()
      .focus()
      .setTextSelection(snapshot)
      .extendMarkRange('link')
      .setLink({ href: data.url, target: '_blank' })
      .run()
    return
  }
  const hasSelection = snapshot ? snapshot.from !== snapshot.to : false
  if (hasSelection) {
    editor
      .chain()
      .focus()
      .setTextSelection(snapshot!)
      .setLink({ href: data.url, target: '_blank' })
      .run()
  } else {
    editor
      .chain()
      .focus()
      .insertContent(`<a href="${data.url}" target="_blank">${data.name}</a>`)
      .run()
  }
}
</script>
