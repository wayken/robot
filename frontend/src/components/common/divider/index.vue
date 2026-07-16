<template>
  <div class="a-divider"
    :class="[
      direction === 'vertical' ? 'is-vertical' : ''
    ]"
    :style="{
      'height': height && height + 'px'
    }"
  ></div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'ADivider'
})

defineProps({
  direction: {
    type: String,
    default: 'horizontal'
  },
  height: {
    type: Number
  }
})
</script>
