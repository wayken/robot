<template>
  <div class="a-captcha-points"
    :class="{
      'is-error': isValidError,
      'is-success': isValidSuccess
    }"
  >
    <div class="a-captcha-points--loading"
      v-if="!data"
      :style="{
        width: parseInt(width as string) + 'px',
        height: parseInt(height as string) + 'px'
      }"
    >
      <div class="loading">
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
      </div>
    </div>
    <div v-else class="a-captcha-points--image"
      :style="{
        width: parseInt(`${width}`, 10) + 'px',
        height: parseInt(`${height}`, 10) + 'px'
      }"
    >
      <el-icon @click="handleRefresh"><RefreshRight /></el-icon>
      <img ref="canvas" class="orginal" :src="'data:image/png;base64,' + data.imgBase64"
        @click="handlePoints"
      />
      <div class="points" v-for="(data, index) in loadClickPoints" :key="index"
        :style="{
          top: parseInt(`${data.y - 10}`, 10) + 'px',
          left: parseInt(`${data.x - 10}`, 10) + 'px'
        }"
      >
        {{ index + 1 }}
      </div>
    </div>
    <div class="a-captcha-points--bar" :style="{
      width: parseInt(`${width}`, 10) + 'px'
    }">{{ loadPickTips }}</div>
  </div>
</template>

<script setup lang="ts">
import {
  RefreshRight
} from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  data: {
    type: Object
  },
  width: {
    type: [String, Number],
    default: 310
  },
  height: {
    type: [String, Number],
    default: 155
  }
})

const loadClickPoints = ref<any[]>([])
// 验证是否通过
const isValidError = ref(false)
const isValidSuccess = ref(false)
const loadPickTips = ref('')

const i18n = useI18n()

onMounted(() => {
  handleReset()
})

const handleReset = () => {
  isValidError.value = false
  isValidSuccess.value = false
  loadClickPoints.value.splice(0, loadClickPoints.value.length)
  if (props.data) {
    loadPickTips.value = i18n.t('captcha.pickTips') + `【${props.data.wordList.join('，')}】`
  } else {
    loadPickTips.value = i18n.t('captcha.loading')
  }
}

const handleEmit = defineEmits(['load', 'validate'])
const handleRefresh = () => {
  handleEmit('load')
}

const handlePoints = (event: MouseEvent) => {
  const data = props.data as any
  let wordCount = data.wordList.length
  if (loadClickPoints.value.length < wordCount) {
    // 获取坐标
    let x = event.offsetX
    let y = event.offsetY
    let pos = {x, y}
    // 创建坐标点
    loadClickPoints.value.push(pos)
    if (loadClickPoints.value.length === wordCount) {
      let request = {
        'points': loadClickPoints.value,
        'validateSign': data.validateSign
      }
      // 触发父组件的验证函数
      handleEmit('validate', request, (success: boolean) => {
        if (success) {
          isValidSuccess.value = true
          loadPickTips.value = i18n.t('captcha.pickSuccess')
        } else {
          isValidError.value = true
          loadPickTips.value = i18n.t('captcha.pickError')
        }
      })
    }
  }
}

watch(() => props.data, () => {
  handleReset()
})

defineExpose({
  handleReset
})
</script>
