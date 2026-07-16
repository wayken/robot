<template>
  <el-tooltip effect="dark" placement="top-start" virtual-triggering
    :virtual-ref="loadTriggerRef"
    :content="$t('marksuit.menu.' + option.name)"
  />
  <el-popover trigger="click" transition="el-zoom-in-top" :teleported="false" placement="bottom"
    width="auto" v-model:visible="isViewVisible"
  >
    <template #reference>
      <div ref="loadTriggerRef" class="block" @mousedown.prevent
        :class="{
          'is-opened': isViewVisible
        }"
      >
        <a-svg-icon :icon-class="`marksuit-${option.icon}`" />
        <span class="picker" :style="{
          backgroundColor: loadCurrentColor
        }"></span>
      </div>
    </template>
    <div class="fontcolor">
      <a-color-picker v-model="loadPickerColor"
        @change="handleColorUpdate"
      />
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import {
  ToolTemplateProps
} from '../../interface'
import {
  useMarksuitContext
} from '../../hook/useMarksuitContext'
import ASvgIcon from '../../common/svgicon.vue'
import AColorPicker from '../../common/colorpicker.vue'

defineProps<ToolTemplateProps>()

const {
  useRichEditor
} = useMarksuitContext()
const isViewVisible = ref(false)
const loadTriggerRef = ref<HTMLElement>()
const loadPickerColor = ref<string>('#000000')

const loadCurrentColor = computed(() => {
  return useRichEditor.useInstance().getAttributes('textStyle').color || '#000000'
})

const handleColorUpdate = (color: string, source: 'preset' | 'advanced') => {
  if (!color) {
    return
  }
  if (source === 'preset') {
    isViewVisible.value = false
  }
  useRichEditor.useCommands().setColor(color).run()
}

// 打开面板时同步当前颜色到选择器
watch(isViewVisible, (value) => {
  if (value) {
    loadPickerColor.value = loadCurrentColor.value
  }
})
</script>
