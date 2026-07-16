<template>
  <node-view-wrapper as="div" class="x-code-block"
    :class="{
      'is-focused': isFocused
    }"
  >
    <div class="head" contenteditable="false">
      <div class="language">
        <el-select v-model="loadSelectedLanguage" filterable
          @change="handleLanguageUpdate"
        >
          <el-option v-for="lang in loadLanguageList" :key="lang.value"
            :label="lang.label"
            :value="lang.value"
          />
        </el-select>
      </div>
    </div>
    <div class="wrapper">
      <pre><code><node-view-content /></code></pre>
    </div>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import {
  nodeViewProps,
  NodeViewWrapper,
  NodeViewContent
} from '@tiptap/vue-3'

const props = defineProps(nodeViewProps)

const isFocused = ref(false)
const loadSelectedLanguage = ref<string>(props.node.attrs.language)
const loadLanguageList = computed(() => {
  const langs: string[] = props.extension.options.lowlight.listLanguages()
  return langs.map((lang) => ({
    label: lang.toUpperCase(),
    value: lang
  }))
})

onMounted(() => {
  props.editor.on('blur', handleBlockBlur)
  props.editor.on('focus', handleBlockFocus)
  props.editor.on('selectionUpdate', handleBlockFocus)
})
onBeforeUnmount(() => {
  props.editor.off('blur', handleBlockBlur)
  props.editor.off('focus', handleBlockFocus)
  props.editor.on('selectionUpdate', handleBlockFocus)
})

const handleBlockFocus = () => {
  const { from, to } = props.editor.state.selection
  const pos = props.getPos()
  if (typeof pos !== 'number') return
  const nodeSize = props.node.nodeSize
  isFocused.value = from >= pos && to <= pos + nodeSize
}
const handleBlockBlur = () => {
  isFocused.value = false
}

function handleLanguageUpdate(value: string) {
  props.updateAttributes({ language: value })
}
</script>
