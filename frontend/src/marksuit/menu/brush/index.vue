<template>
  <el-tooltip effect="dark" placement="top-start"
    :content="loadTooltipText"
  >
    <div class="block"
      :class="{
        'is-actived': isBrushActive
      }"
      @mousedown.prevent
      @click="handleBrushClick"
      @dblclick="handleBrushDblClick"
    >
      <a-svg-icon :icon-class="`marksuit-${option.icon}`" />
    </div>
  </el-tooltip>
</template>

<script setup lang="ts">
import {
  ToolTemplateProps
} from '../../interface'
import {
  useMarksuitContext
} from '../../hook/useMarksuitContext'

defineProps<ToolTemplateProps>()

const i18n = useI18n()
const {
  useRichEditor
} = useMarksuitContext()

const loadRunTimer = ref<ReturnType<typeof setTimeout>>()

// 读取格式刷激活状态（响应式）
const isBrushActive = computed(() => {
  return useRichEditor.useInstance().storage.formatBrush?.isBrushActiveRef?.value ?? false
})

const loadTooltipText = computed(() => {
  if (isBrushActive.value) {
    return i18n.t('marksuit.menu.brush-active')
  }
  return i18n.t('marksuit.menu.brush')
})
const handleBrushClick = () => {
  clearTimeout(loadRunTimer.value)
  loadRunTimer.value = setTimeout(() => {
    // 已激活时再次单击 → 取消格式刷
    if (isBrushActive.value) {
      useRichEditor.useCommands().cancelFormat().run()
    } else {
      useRichEditor.useCommands().copyFormat({ type: 'click' }).run()
    }
  }, 300)
}

const handleBrushDblClick = () => {
  clearTimeout(loadRunTimer.value)
  // 已激活时双击 → 取消格式刷
  if (isBrushActive.value) {
    useRichEditor.useCommands().cancelFormat().run()
  } else {
    useRichEditor.useCommands().copyFormat({ type: 'dblclick' }).run()
  }
}
</script>
