<template>
  <div class="a-popup" v-if="show">
    <div class="mask"
      :class="{
        'is-animate': animate
      }"
      @click="handleModalClose()"
    ></div>
    <div class="wrapper"
      :class="['is-' + direction, {
        'is-animate': animate
      }]"
    >
      <div class="wrapper-box">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'APopup'
})

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  direction: {
    type: String,
    default: 'bottom'
  },
  closeOnClickModal: {
    type: Boolean,
    default: false
  }
})

const show = ref(false)
const animate = ref(false)

const handleEmit = defineEmits(['open', 'close'])
const handleOpen = () => {
  show.value = true
  window.setTimeout(() => {
    animate.value = true
    handleEmit('open')
  }, 100)
}
const handleClose = () => {
  animate.value = false
  window.setTimeout(() => {
    show.value = false
    handleEmit('close')
  }, 300)
}
const handleModalClose = () => {
  if (props.closeOnClickModal) {
    handleClose()
  }
}

watch(() => props.visible, (value) => {
  if (value) {
    handleOpen()
  } else {
    if (animate.value) {
      handleClose()
    }
  }
})
</script>
