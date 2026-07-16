<template>
  <el-tooltip effect="dark" placement="top-start" virtual-triggering
    :virtual-ref="loadTriggerRef"
    :content="$t('marksuit.menu.' + option.name)"
  />
  <el-popover trigger="click" transition="el-zoom-in-top" :teleported="false" placement="bottom"
    :width="160" v-model:visible="isViewVisible"
  >
    <template #reference>
      <div ref="loadTriggerRef" class="block is-text" @mousedown.prevent
        :class="{
          'is-opened': isViewVisible
        }"
      >
        <a-svg-icon :icon-class="`marksuit-${option.icon}`" />
        <el-icon><CaretBottom /></el-icon>
      </div>
    </template>
    <ul class="dropdown">
      <li v-for="(data) in loadLevelList" :key="data"
        @click="handleLineHeightSet(data)"
      >
        <div class="name">{{ data }}</div>
        <el-icon class="marked" v-if="loadActiveValue == data"><Check /></el-icon>
      </li>
    </ul>
  </el-popover>
</template>

<script setup lang="ts">
import {
  Check,
  CaretBottom
} from '@element-plus/icons-vue'
import {
  ToolTemplateProps
} from '../../interface'
import {
  useMarksuitContext
} from '../../hook/useMarksuitContext'
import ASvgIcon from '../../common/svgicon.vue'

defineProps<ToolTemplateProps>()

const {
  useRichEditor
} = useMarksuitContext()

const loadLevelList = ['1', '1.15', '1.5', '2', '2.5', '3']

const isViewVisible = ref(false)
const loadTriggerRef = ref<HTMLElement>()

const loadActiveValue = computed(() => {
  const instance = useRichEditor.useInstance()
  const attrs = instance.getAttributes('paragraph')
  return attrs.lineHeight || ''
})

const handleLineHeightSet = (value: string) => {
  isViewVisible.value = false
  useRichEditor.useCommands().setLineHeight(value).run()
}
</script>
