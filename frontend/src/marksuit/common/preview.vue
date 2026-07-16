<template>
  <div class="a-image-preview" v-if="visible">
    <div class="mask"></div>
    <div class="source" @mousewheel.prevent="handleImageZoom">
      <img :src="image.url"
        :style="{
          top: `${imageMovePosition.y}px`,
          left: `${imageMovePosition.x}px`,
          transform: `scale(${imageScale}) rotate(${imageRotate}deg)`
        }"
        @mousedown.stop.prevent="handleImageMove"
      />
    </div>
    <div class="close" @click="handleClose">
      <el-icon><Close /></el-icon>
    </div>
    <div class="control">
      <el-icon @click="handleImageDownload"><Download /></el-icon>
      <span class="divider"></span>
      <el-icon @click="handleImageReset"><FullScreen /></el-icon>
      <el-icon @click="handleImageScale(false)"><ZoomOut /></el-icon>
      <el-icon @click="handleImageScale(true)"><ZoomIn /></el-icon>
      <el-icon @click="handleImageRotate"><RefreshRight /></el-icon>
      <span class="divider"></span>
      <span class="orginal" @click="handleImageView">{{ $t('marksuit.preview.view-image') }}</span>
    </div>
    <div class="prev" v-if="!single" @click="handlePrevImage">
      <el-icon><ArrowLeft /></el-icon>
    </div>
    <div class="next" v-if="!single" @click="handleNextImage">
      <el-icon><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ZoomIn,
  ZoomOut,
  Close,
  FullScreen,
  Download,
  ArrowLeft,
  ArrowRight,
  RefreshRight
} from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  image: {
    type: Object,
    default() {
      return {
        name: '',
        url: ''
      }
    }
  },
  single: {
    type: Boolean,
    default: false
  }
})

// 显示比例
const imageScale = ref(1)
// 旋转角度
const imageRotate = ref(0)
const imageMovePosition = ref({
  x: 0,
  y: 0,
  url: ''
})

const handleEmit = defineEmits(['close', 'prev', 'next'])
const handleClose = () => {
  handleEmit('close')
}
const handleImageScale = (zoom: boolean) => {
  if (zoom) {
    imageScale.value += 0.1
  } else {
    if (imageScale.value > 0.2) {
      imageScale.value -= 0.1
    }
  }
}
const handleImageRotate = () => {
  imageRotate.value += 90
}
const handlePrevImage = () => {
  handleEmit('prev')
}
const handleNextImage = () => {
  handleEmit('next')
}
const handleImageMove = (event: MouseEvent) => {
  const startY = event.clientY
  const startX = event.clientX
  const startTop = imageMovePosition.value.y
  const startLeft = imageMovePosition.value.x
  const move = (moveEvent: MouseEvent) => {
    const curPosX = moveEvent.clientX
    const curPosY = moveEvent.clientY
    imageMovePosition.value.x = curPosX - startX + startLeft
    imageMovePosition.value.y = curPosY - startY + startTop
  }
  const up = () => {
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
  }
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}
const handleImageZoom = (event: WheelEvent) => {
  if (event.deltaY < 0) {
    handleImageScale(true)
  } else {
    handleImageScale(false)
  }
}
const handleImageView = () => {
  window.open(props.image.url, '_blank')
}
const handleImageDownload = () => {
  const link = document.createElement('a')
  link.href = props.image.url
  link.download = 'webimage'
  document.body.appendChild(link)
  link.click()
  link.remove()
}
const handleImageReset = () => {
  imageScale.value = 1
  imageMovePosition.value.x = 0
  imageMovePosition.value.y = 0
}
</script>
