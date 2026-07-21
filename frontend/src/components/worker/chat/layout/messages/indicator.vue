<template>
  <div class="message is-assistant">
    <a-robot-head :kind="0" :size="42" :src="IconImage" v-if="showAvatar" />
    <div
      class="indicator"
      :class="{
        'has-context': hasContext,
        'is-standalone': standalone
      }"
    >
      <a-loading size="small" />
      <div class="progress">
        <span class="label">{{ statusText }}</span>
        <span class="dots">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ARobotHead from '@/components/common/robot/head.vue'
import ALoading from '@/components/common/loading/index.vue'
import IconImage from '@/assets/images/providers/deepseek.png'

withDefaults(defineProps<{
  statusText: string
  hasContext?: boolean
  showAvatar?: boolean
  standalone?: boolean
}>(), {
  hasContext: false,
  showAvatar: false,
  standalone: false
})
</script>
