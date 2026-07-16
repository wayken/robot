<template>
  <div class="menu">
    <div v-for="(data, index) in loadBlockList" :key="index"
      :class="{
        'is-divider': data && data.name === 'divider'
      }"
    >
      <!-- 通用模块 -->
      <ACommonBlock :option="data" v-if="data && data.buildin" />
      <!-- 表格模块 -->
      <ATableBlock :option="data" v-if="data && data.name === 'table'" />
      <!-- 笔刷模块 -->
      <ABrush :option="data" v-if="data && data.name === 'brush'" />
      <!-- 标题模块 -->
      <AHeader :option="data" v-if="data && data.name === 'header'" />
      <!-- 图片模块 -->
      <AImage :option="data" v-if="data && data.name === 'image'" />
      <!-- 链接模块 -->
       <ALink :option="data" v-if="data && data.name === 'link'" />
      <!-- 字体颜色模块 -->
      <AFontColor :option="data" v-if="data && data.name === 'font-color'" />
      <!-- 行高模块 -->
      <ALineHeight :option="data" v-if="data && data.name === 'line-height'" />
      <!-- 全屏模块 -->
      <AFullScreen :option="data" v-if="data && data.name === 'fullscreen'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  useMenuOption
} from './menu/index'
import {
  useMarksuitContext
} from './hook/useMarksuitContext'
import ACommonBlock from './menu/common.vue'
import ATableBlock from './menu/table/index.vue'
import ABrush from './menu/brush/index.vue'
import AHeader from './menu/header/index.vue'
import AImage from './menu/image/index.vue'
import ALink from './menu/link/index.vue'
import AFontColor from './menu/fontcolor/index.vue'
import ALineHeight from './menu/lineheight/index.vue'
import AFullScreen from './menu/fullscreen/index.vue'

const props = defineProps({
  value: {
    type: String,
    default: 'undo redo bold linethrough underline italic | ul ol quote hr unchecked checked unlink link font-color line-height | image table | code header brush eraser format | fullscreen'
  }
})

const {
  useRichEditor
} = useMarksuitContext()
const loadMenuOption = useMenuOption(useRichEditor)
const loadBlockMapping = loadMenuOption.reduce((mapping: Record<string, any>, value) => {
  mapping[value.name] = value
  return mapping
}, {})
const loadBlockList = props.value.split(/\s+/).map((name: string) => {
  if (name === '|') {
    return {
      name: 'divider',
      icon: ''
    }
  }
  return loadBlockMapping[name]
})
</script>
