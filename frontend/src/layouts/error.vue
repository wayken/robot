<template>
  <div class="a-error-page">
    <div class="a-error-page__title">
      <div class="header">{{ $t('error.system-tips') }}</div>
    </div>
    <div class="a-error-page__content">
      <div class="wrapper">
        <div class="error-pin"></div>
        <div class="error-code">
          <p class="tip">STATUS: {{ status }}</p>
        </div>
      </div>
      <div class="message">
        <h1>{{ $t('common.tips') }}: </h1>
        <div class="desc">{{ $t('error.http-status-' + status) }}</div>
        <div class="action">
          <div class="button primary" @click="handleToHome">
            <el-icon><Promotion /></el-icon>
            {{ $t('error.link-home') }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Promotion
} from '@element-plus/icons-vue'

defineProps({
  // 错误响应码
  status: {
    type: [String, Number],
    default: '404'
  }
})

const handleToHome = () => {
  window.location.href = '/'
}
</script>
