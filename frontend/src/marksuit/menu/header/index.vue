<template>
  <el-tooltip effect="dark" placement="top-start" virtual-triggering
    :virtual-ref="loadTriggerRef"
    :content="$t('marksuit.menu.' + option.name)"
  />
  <el-popover trigger="click" transition="el-zoom-in-top" :teleported="false" placement="bottom"
    :width="182" v-model:visible="isViewVisible"
  >
    <template #reference>
      <div ref="loadTriggerRef" class="block is-text" @mousedown.prevent
        :class="{
          'is-opened': isViewVisible
        }"
      >
        <div class="label">{{ $t('marksuit.command.' + loadLevelList[loadActivedLevel].name) }}</div>
        <el-icon><CaretBottom /></el-icon>
      </div>
    </template>
    <ul class="dropdown">
      <li v-for="(data, index) in loadLevelList" :key="index"
        @click="handleClassNameSet(data)"
      >
        <a-svg-icon class="icon" :icon-class="data.icon" size="18px" />
        <div class="name">{{ $t('marksuit.command.' + data.name) }}</div>
        <el-icon class="marked" v-if="loadLevelList[loadActivedLevel].name === data.name"><Check /></el-icon>
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
type Level = 0 | 1 | 2 | 3 | 4 | 5 | 6
const {
  useRichEditor
} = useMarksuitContext()

const loadLevelList = [
  {
    name: 'text',
    value: 0 as Level,
    icon: 'marksuit-text'
  },
  {
    name: 'h1',
    value: 1 as Level,
    icon: 'marksuit-heading-h1'
  },
  {
    name: 'h2',
    value: 2 as Level,
    icon: 'marksuit-heading-h2'
  },
  {
    name: 'h3',
    value: 3 as Level,
    icon: 'marksuit-heading-h3'
  },
  {
    name: 'h4',
    value: 4 as Level,
    icon: 'marksuit-heading-h4'
  }
]
const isViewVisible = ref(false)
const loadTriggerRef = ref<HTMLElement>()

const loadActivedLevel = computed(() => {
  const instance = useRichEditor.useInstance()
  for (let i = 1; i <= 6; i++) {
    if (instance.isActive('heading', { level: i })) {
      return i
    }
  }
  return 0
})

const handleClassNameSet = (data: any) => {
  const value = data.value
  isViewVisible.value = false
  if (value === 0) {
    useRichEditor.useCommands().setParagraph().run()
  } else {
    useRichEditor.useCommands().toggleHeading({ level: value }).run()
  }
}
</script>
