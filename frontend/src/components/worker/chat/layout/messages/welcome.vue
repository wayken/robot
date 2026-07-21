<template>
  <div class="welcome inline-flex-c-c-c" v-if="messages.length === 0 && !streaming">
    <img class="logo" :src="logoSvg" />
    <div class="domain">
      Team<span class="highlight">Robot</span>
    </div>
    <div class="slogan">
      {{ $t('chat.slogan') }}
    </div>
    <div class="suggestion">
      <div class="card inline-flex-r-c-n" v-for="(data, index) in loadSuggestionList" :key="index">
          <div class="name">{{ $t(`chat.${data.name}`) }}</div>
          <el-icon class="icon"><Right /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Right
} from '@element-plus/icons-vue'
import logoSvg from '@/assets/logo.svg'

defineProps<{
  messages: any[]
  streaming: boolean
}>()

const loadSuggestionList = [
  {
    name: 'suggestion-0'
  },
  {
    name: 'suggestion-1'
  },
  {
    name: 'suggestion-2'
  },
  {
    name: 'suggestion-3'
  }
]
</script>
