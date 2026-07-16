<template>
  <svg class="a-svgicon"
    :style="{
      width: size,
      height: size
    }"
  >
    <use :xlink:href="symbolId" :fill="color" />
  </svg>
</template>

<script setup lang="ts">
defineOptions({
  name: 'ASvgIcon'
})

const props = defineProps({
  prefix: {
    type: String,
    default: 'icon'
  },
  iconClass: {
    type: String,
    required: false
  },
  color: {
    type: String
  },
  size: {
    type: String
  }
})

const symbolId = computed(() => `#${props.prefix}-${props.iconClass}`)
</script>
