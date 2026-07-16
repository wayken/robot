<template>
  <div class="a-robot"
    :class="[
      `is-kind-${kind}`,
      status === 'progression' ? 'is-progression' : ''
    ]"
    :style="{ width: size + 'px', height: size + 'px' }"
  >
    <!-- 光晕层（会话进行中状态） -->
    <div class="aura" v-if="status === 'progression'">
      <div class="ring ring--1"></div>
      <div class="ring ring--2"></div>
      <div class="ring ring--3"></div>
    </div>
    <!-- kind=0：图片头像 -->
    <div v-if="kind === 0" class="image">
      <img :src="src" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'RobotHead'
})

defineProps({
  kind: {
    type: Number,
    default: 0
  },
  src: {
    type: String,
    default: ''
  },
  size: {
    type: Number,
    default: 38
  },
  status: {
    type: String,
    default: ''
  }
})
</script>
