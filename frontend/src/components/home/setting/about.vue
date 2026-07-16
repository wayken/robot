<template>
  <el-dialog draggable width="580" :model-value="visible" class="a-setting-about"
    :title="$t('setting.about')"
    @close="handleClose"
  >
    <div class="main">
      <div class="dashboard inline-flex-r-c-n">
        <div class="logo">
          <img :src="logoSvg" />
        </div>
        <div class="infomation inline-flex-c-n-n">
          <div class="name">{{ $t('portal.name') }}</div>
          <div class="version inline-flex-r-c-n">
            {{ $t('setting.version') }}：{{ loadAppVersion }}
            <el-icon class="refresh"><RefreshRight /></el-icon>
          </div>
        </div>
      </div>
      <div class="metadata">
        <div class="link">
          <div class="title">{{ $t('setting.link') }}</div>
          <a target="_blank" @click="handleOpenLink">
            {{ $t('setting.product-website') }}
          </a>
        </div>
        <div class="wechat">
          <div class="title">{{ $t('setting.wechat') }}</div>
          <div class="description">{{ $t('setting.wechat-description') }}</div>
          <div class="image">
            <el-image src="https://waybuket01.oss-cn-beijing.aliyuncs.com/home/wechat_qrcode.jpg" />
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  RefreshRight
} from '@element-plus/icons-vue'
import runtime from '@/platform/runtime'
import logoSvg from '@/assets/logo.svg'

defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const loadAppVersion = ref('1.0.0')

onMounted(async () => {
  const result = await runtime.handleLoadSystemInfo()
  if (result) {
    loadAppVersion.value = result.version
  }
})

const handleEmit = defineEmits(['close'])
const handleClose = () => {
  handleEmit('close')
}
const handleOpenLink = () => {
  const website = 'https://www.teambeit.com'
  if (runtime.isApplication()) {
    runtime.handleOpenExternalLink(website)
  } else {
    window.open(website, '_blank')
  }
}
</script>
