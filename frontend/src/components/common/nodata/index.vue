<template>
  <div class="a-nodata" v-if="visible">
    <div class="a-nodata__load" v-if="loading && spinner" :class="{
      'is-mask': mask
    }">
      <div class="spinner">
        <svg class="circular" viewBox="25 25 50 50">
          <circle class="path" cx="50" cy="50" r="20" fill="none" />
        </svg>
      </div>
    </div>
    <div class="a-nodata__main" v-if="!loading"
      :class="[
        size ? `size-${size}` : ''
      ]"
    >
      <template v-if="success">
        <img v-if="imgType === 0" :src="handleImageLoad(theme)" />
        <img v-else :src="handleImageLoad(theme)" />
        <div class="description" v-if="description">{{ description }}</div>
        <div class="description" v-else>{{ $t('nodata.empty') }}</div>
        <slot></slot>
      </template>
      <template v-else>
        <img :src="handleErrorImageLoad()" />
        <div class="description">{{ $t('nodata.error') }}</div>
        <el-button type="primary" @click="handleRefresh">
          {{ $t('nodata.refresh') }}
        </el-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/store/modules/app'

defineOptions({
  name: 'ANodata'
})
const props = defineProps({
  loading: {
    type: Boolean,
    default: true
  },
  success: {
    type: Boolean,
    default: true
  },
  spinner: {
    type: Boolean,
    default: true
  },
  mask: {
    type: Boolean,
    default: false
  },
  size: {
    type: String
  },
  imgType: {
    type: Number,
    default: 0
  },
  description: {
    type: String
  },
  throttle: {
    type: Number,
    default: 200
  }
})

const appStore = useAppStore()
const visible = ref(false)
const theme = computed(() => {
  return appStore.theme
})

let timeoutHandler = 0
onMounted(() => {
  if (props.throttle > 0) {
    clearTimeout(timeoutHandler)
    timeoutHandler = window.setTimeout(() => {
      visible.value = true
    }, props.throttle)
  } else {
    visible.value = true
  }
})

const handleImageLoad = (theme: string) => {
  return new URL(`/src/assets/nodata/nodata__${theme}.svg`, import.meta.url).href
}
const handleErrorImageLoad = () => {
  return new URL('/src/assets/nodata/nodata__rose.svg', import.meta.url).href
}

const handleRefresh = () => {
  window.location.reload()
}
</script>
