<template>
  <transition name="skill-panel-fade">
    <div v-if="visible" ref="loadPanelRef" class="skill-panel">
      <div class="skill-panel__header inline-flex-r-c-b">
        <div class="skill-panel__title">{{ $t('skill-setting.title') }}</div>
        <el-icon class="skill-panel__close" @click="handleClose">
          <Close />
        </el-icon>
      </div>
      <div class="skill-panel__body">
        <div v-if="loading" class="skill-panel__loading">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div v-else-if="dataList.length === 0" class="skill-panel__empty">
          {{ $t('skill-setting.no-description') }}
        </div>
        <div v-else v-for="skill in dataList" :key="skill.name" class="skill-panel__module"
          :class="{
            'is-selected': isSelected(skill)
          }"
          @click="handleSelect(skill)"
        >
          <a-svg-icon icon-class="skill" size="20px" class="skill-panel__icon" />
          <div class="skill-panel__info">
            <div class="skill-panel__name">{{ skill.name }}</div>
            <div class="skill-panel__description">
              {{ skill.description || $t('skill-setting.no-description') }}
            </div>
          </div>
          <el-icon v-if="isSelected(skill)" class="skill-panel__check">
            <Check />
          </el-icon>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import {
  Close,
  Check,
  Loading
} from '@element-plus/icons-vue'
import useSocketIO from '@/hooks/useSocketIO'

interface SkillItem {
  name: string
  description: string
  always: boolean
  enabled: boolean
}

const props = defineProps<{
  visible: boolean
  selectedSkills: SkillItem[]
}>()

const handleEmit = defineEmits<{
  (e: 'select', skill: SkillItem): void
  (e: 'close'): void
}>()

const route = useRoute()
const { ioRequest } = useSocketIO()

const loadPanelRef = ref<HTMLElement | null>(null)
const dataList = ref<SkillItem[]>([])
const loading = ref(false)

const useWid = () => route.params.id as string

// 判断技能是否已选中
const isSelected = (skill: SkillItem) => {
  return props.selectedSkills.some(s => s.name === skill.name)
}

// 选择技能
const handleSelect = (skill: SkillItem) => {
  handleEmit('select', skill)
}

// 关闭面板
const handleClose = () => {
  handleEmit('close')
}

// 加载技能列表
const handleDataLoad = () => {
  loading.value = true
  const params = {
    wid: useWid()
  }
  ioRequest('skill.index', params).then((result: any) => {
    dataList.value = (result[0] || []).filter((data: SkillItem) => data.enabled)
  }).finally(() => {
    loading.value = false
  })
}

// 点击面板外区域自动关闭
const handleClickOutside = (event: MouseEvent) => {
  if (!props.visible) return
  const panelEl = loadPanelRef.value
  const dom = event.target as HTMLElement
  if (panelEl && panelEl.contains(dom)) return
  if (dom.closest('.skill-btn')) return
  handleEmit('close')
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside, true)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside, true)
})

watch(() => props.visible, (val) => {
  if (val) {
    handleDataLoad()
  }
})
</script>
