<template>
  <div ref="loadCommandEl" class="a-command">
    <!-- 搜索结果：扁平展示，无分组 -->
    <template v-if="isFiltered">
      <div v-if="modules.length === 0" class="is-empty">
        {{ $t('marksuit.command.no-results') }}
      </div>
      <template v-else>
        <div v-for="(module, index) in modules" :key="'flat-' + index" class="module"
          :ref="(el) => handleModuleSetRef(el, index)"
          :class="{
            'is-actived': index === loadSelectedIndex
          }"
          @click="handleModuleSelect(index)"
          @mouseenter="loadSelectedIndex = index"
        >
          <span class="icon">
            <a-svg-icon :icon-class="`marksuit-${module.icon}`" :color="module.color" />
          </span>
          <div class="infomation">
            <div class="name">{{ $t('marksuit.command.' + module.name) }}</div>
            <div class="description">{{ $t('marksuit.command.' + module.name + '-description') }}</div>
          </div>
          <span v-if="module.keywords?.[0]" class="keyword">/{{ module.keywords[0] }}</span>
        </div>
      </template>
    </template>
    <!-- 默认：按分组展示 -->
    <template v-else>
      <template v-for="section in loadSlashCommandList" :key="section.label">
        <div class="label">
          {{ $t('marksuit.command.' + section.label) }}
        </div>
        <div v-for="(module, index) in section.modules" :key="'module-' + index" class="module"
          :ref="(el) => handleModuleSetRef(el, useFlatIndex(module))"
          :class="{
            'is-actived': useFlatIndex(module) === loadSelectedIndex
          }"
          @click="handleModuleSelect(useFlatIndex(module))"
          @mouseenter="loadSelectedIndex = useFlatIndex(module)"
        >
          <span class="icon">
            <a-svg-icon :icon-class="`marksuit-${module.icon}`" :color="module.color" />
          </span>
          <div class="infomation">
            <div class="name">{{ $t('marksuit.command.' + module.name) }}</div>
            <div class="description">{{ $t('marksuit.command.' + module.name + '-description') }}</div>
          </div>
          <span v-if="module.keywords?.[0]" class="keyword">/{{ module.keywords[0] }}</span>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { loadSlashCommandList, loadSlashCommandFlatMap } from './commands'
import type { SlashCommandModule } from './commands'
import ASvgIcon from '../../../common/svgicon.vue'

const props = defineProps<{
  modules: SlashCommandModule[]
  command?: (module: SlashCommandModule) => void
}>()

const loadSelectedIndex = ref(0)
const loadCommandEl = ref<HTMLElement | null>(null)
const loadModuleRefs: HTMLElement[] = []

/** 是否处于搜索过滤状态（模块数量少于全量时） */
const isFiltered = computed(() => props.modules.length !== loadSlashCommandFlatMap.length)

/** 获取某个模块在当前列表（过滤态用 props.modules，默认态用全量）中的索引 */
const useFlatIndex = (module: SlashCommandModule): number => {
  return loadSlashCommandFlatMap.indexOf(module)
}
const handleModuleSetRef = (el: unknown, index: number) => {
  if (el instanceof HTMLElement) {
    loadModuleRefs[index] = el
  }
}
const handleScrollActiveIntoView = () => {
  nextTick(() => {
    const el = loadModuleRefs[loadSelectedIndex.value]
    if (el && loadCommandEl.value) {
      el.scrollIntoView({ block: 'nearest', inline: 'nearest' })
    }
  })
}
const handleModuleSelect = (index: number) => {
  // 分组视图下 index 是全量索引，过滤视图下是 props.modules 的索引
  const module = isFiltered.value
    ? props.modules[index]
    : loadSlashCommandFlatMap[index]
  if (module) {
    props.command?.(module)
  }
}
const onKeyDown = (event: KeyboardEvent): boolean => {
  const total = isFiltered.value ? props.modules.length : loadSlashCommandFlatMap.length
  if (total === 0) {
    return false
  }
  if (event.key === 'ArrowUp') {
    loadSelectedIndex.value = (loadSelectedIndex.value - 1 + total) % total
    handleScrollActiveIntoView()
    return true
  }
  if (event.key === 'ArrowDown') {
    loadSelectedIndex.value = (loadSelectedIndex.value + 1) % total
    handleScrollActiveIntoView()
    return true
  }
  if (event.key === 'Enter') {
    handleModuleSelect(loadSelectedIndex.value)
    return true
  }
  return false
}

defineExpose({ onKeyDown })

watch(
  () => props.modules,
  () => {
    loadSelectedIndex.value = 0
    loadModuleRefs.length = 0
  }
)
</script>
