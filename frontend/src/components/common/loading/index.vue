<template>
  <div class="a-loading"
    :class="[
      size ? `size-${size}` : ''
    ]"
  >
    <div class="spinner"></div>
    <div class="spinner"></div>
    <div class="spinner"></div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'ALoading'
})

defineProps({
  size: {
    type: String
  }
})
</script>
