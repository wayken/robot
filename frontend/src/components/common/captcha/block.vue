<template>
  <div class="a-captcha-block"
    :class="{
      'is-error': isValidError,
      'is-success': isValidSuccess
    }"
  >
    <div class="a-captcha-block--loading"
      v-if="!data"
      :style="{
        width: parseInt(`${width}`, 10) + 'px',
        height: parseInt(`${height}`, 10) + 'px'
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
    <!-- 背景图片+滑块图片区域 -->
    <div class="a-captcha-block--image" v-else
      :style="{
        width: parseInt(`${width}`, 10) + 'px',
        height: parseInt(`${height}`, 10) + 'px'
      }"
      @click="handleRefresh(true)"
    >
      <img class="orginal" @dragstart.prevent :src="'data:image/png;base64,' + data.imgBase64" />
      <img class="slider" @dragstart.prevent
        :src="'data:image/png;base64,' + data.sliderBase64"
        :style="{
          top: data.Y + 'px',
          left: loadMoveImageLeft + 'px',
          width: data.sw + 'px',
          height: data.sh + 'px',
          transition: loadSlideRollback
        }"
        @click.stop
        @touchstart="handleSlideStart" @mousedown="handleSlideStart"
      />
      <transition name="tips">
        <span class="tips" v-if="loadSlideTips">{{ loadSlideTips }}</span>
      </transition>
    </div>
    <!-- 滑动滑块+滑动拖块区域 -->
    <div class="a-captcha-block--bar" ref="barRef"
      :style="{
        width: parseInt(`${width}`, 10) + 'px'
      }"
    >
      <div class="track"
        :style="{
          width: loadSlideBarWidth + 'px',
          transition: loadSlideRollback
        }"
      >
        <div class="anchor" @touchstart="handleSlideStart" @mousedown="handleSlideStart"></div>
      </div>
      <div class="label">{{ $t('captcha.slideTips') }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
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

// 滑块鼠标拖动状态
const isSlideDragging = ref(false)
// 滑块鼠标开始拖动位置
const loadStartSlideLeft = ref(0)
// 滑块图片正在拖动位置
const loadMoveImageLeft = ref(0)
// 滑块拖动时的宽度
const loadSlideBarWidth = ref(35)
// 验证是否通过
const isValidError = ref(false)
const isValidSuccess = ref(false)
// 滑块移动开始、结束的时间
const loadStartMoveTime = ref(0)
const loadEndMovetime = ref(0)
// 验证文字提示
const loadSlideTips = ref('')
// 滑动回滚的动画
const loadSlideRollback = ref('')

const barRef = ref()
const i18n = useI18n()

onMounted(() => {
  window.addEventListener('touchmove', function (e) {
    handleSlideMove(e)
  })
  window.addEventListener('mousemove', function (e) {
    handleSlideMove(e)
  })
  // 滑块松开监听
  window.addEventListener('touchend', function () {
    handleSlideEnd()
  })
  window.addEventListener('mouseup', function () {
    handleSlideEnd()
  })
})

const handleEmit = defineEmits(['load', 'validate'])
// 重置滑动验证码
const handleReset = () => {
  loadMoveImageLeft.value = 0
  loadSlideBarWidth.value = 35
  loadSlideRollback.value = 'left 1s ease, width 1s ease'
  window.setTimeout(() => {
    loadSlideRollback.value = ''
    isValidError.value = false
    isValidSuccess.value = false
  }, 1000)
}
// 刷新滑动图片验证码
const handleRefresh = (quiet: boolean) => {
  // 拖动条回滚的过程中不允许点击重新加载图形验证码
  if (quiet) {
    if (isValidError || isValidSuccess) {
      return
    }
  }
  handleEmit('load')
}
// 滑块鼠标按下
const handleSlideStart = (event: any) => {
  if (props.data) {
    event = event || window.event
    event.preventDefault()
    let x = event.clientX
    if (event.touches) {
      // 兼容移动端
      x = event.touches[0].pageX
    }
    loadStartSlideLeft.value = Math.floor(x - barRef.value.getBoundingClientRect().left)
    loadStartMoveTime.value = new Date().getTime()
    isSlideDragging.value = true
  }
}
// 滑块鼠标移动
const handleSlideMove = (event: any) => {
  if (isSlideDragging.value) {
    event = event || window.event
    event.preventDefault()
    let x = event.clientX
    if (event.touches) {
      // 兼容移动端
      x = event.touches[0].pageX
    }
    let barLeft = barRef.value.getBoundingClientRect().left
    // 设定图片方块和拖动滑块的边界
    let moveSlideLeft = x - barLeft - loadStartSlideLeft.value
    // 设定图片方块边界
    let moveImageLeft = moveSlideLeft
    if (moveImageLeft <= 0) {
      moveImageLeft = 0
    }
    const data = props.data as any
    if (moveImageLeft >= barRef.value.offsetWidth - data.sw) {
      moveImageLeft = barRef.value.offsetWidth - data.sw
    }
    loadMoveImageLeft.value = moveImageLeft
    // 设定拖动滑块边界
    let slideBarWidth = moveSlideLeft
    if (slideBarWidth <= 0) {
      slideBarWidth = 0
    }
    if (slideBarWidth >= barRef.value.offsetWidth - 35) {
      slideBarWidth = barRef.value.offsetWidth - 35
    }
    loadSlideBarWidth.value = slideBarWidth + 35
  }
}
// 滑块鼠标松开
const handleSlideEnd = () => {
  if (isSlideDragging.value) {
    isSlideDragging.value = false
    loadEndMovetime.value = new Date().getTime()
    // 滑块没有移动距离为0时不触发验证，防止单纯单击时触发验证
    const moveLeftDistance = loadMoveImageLeft.value
    if (moveLeftDistance <= 0) {
      return
    }
    const data = props.data as any
    // 判断是滑动图片是否与图片水印重合
    const request = {
      'x': moveLeftDistance,
      'y': data.Y,
      'validateSign': data.validateSign
    }
    // 触发父组件的验证函数
    handleEmit('validate', request, (success: boolean) => {
      if (success) {
        isValidSuccess.value = true
        const costTime = ((loadEndMovetime.value - loadStartMoveTime.value) / 1000).toFixed(2)
        loadSlideTips.value = i18n.t('captcha.validSuccess') + costTime + 's'
        setTimeout(() => {
          loadSlideTips.value = ''
        }, 1000)
      } else {
        isValidError.value = true
        loadSlideTips.value = i18n.t('captcha.validError')
        setTimeout(() => {
          loadSlideTips.value = ''
        }, 1000)
      }
    })
  }
}

defineExpose({
  handleReset
})
</script>
