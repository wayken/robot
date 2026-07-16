<template>
  <div v-show="visible" :class="mode === 'pop' ? 'a-captcha--mask': ''">
    <div class="a-captcha"
      :class="{
        'is-pop': mode === 'pop'
      }"
      :style="{
        'max-width': loadBoxWidth + 'px',
        'padding': mode === 'pop' ? '18px' : '0'
      }"
    >
      <!-- 头部验证码标题，主要用于弹窗的方式弹出验证码验证 -->
      <div class="a-captcha--header" v-if="mode==='pop'">
        <div>{{ $t('captcha.validConfirm') }}</div>
        <el-icon class="close" @click="handleClose"><Close /></el-icon>
      </div>
      <!-- 滑块拼图验证码组件 -->
      <a-captcha-block ref="instanceRef" v-if="type == 0"
        :data="infomation"
        :mode="mode"
        @load="handleLoad"
        @validate="handleValidate"
      />
      <!-- 文字点选验证码组件 -->
      <a-captcha-points ref="instanceRef" v-else-if="type == 1"
        :data="infomation"
        :mode="mode"
        @load="handleLoad"
        @validate="handleValidate"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Close
} from '@element-plus/icons-vue'
import ACaptchaBlock from './block.vue'
import ACaptchaPoints from './points.vue'

defineOptions({
  name: 'ACaptcha'
})

const props = defineProps({
  type: {
    type: [String, Number],
    default: 0
  },
  mode: {
    type: String,
    default: 'pop'
  },
  visible: {
    type: Boolean,
    default: false
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

const infomation = ref<any>(null)
const instanceRef = ref()

const loadBoxWidth = computed(() => {
  if (props.mode === 'pop') {
    return parseInt(`${props.width}`, 10) + 40
  }
  return props.width
})

const handleEmit = defineEmits(['load', 'validate', 'close'])
// 验证码加载，由业务接口实现
const handleLoad = () => {
  infomation.value = null
  handleEmit('load', (result: any) => {
    infomation.value = result
  })
}
// 验证码校验，由业务接口实现
const handleValidate = (request: any, resolve: any) => {
  handleEmit('validate', request, resolve)
}
// 滑动图片验证码弹窗关闭
const handleClose = () => {
  handleEmit('close')
}
// 重置验证码
const handleReset = (force = false) => {
  if (force) {
    infomation.value = null
  }
  instanceRef.value.handleReset()
}

watch(() => props.visible, (value) => {
  if (value) {
    handleLoad()
  }
})

defineExpose({
  handleReset
})
</script>
