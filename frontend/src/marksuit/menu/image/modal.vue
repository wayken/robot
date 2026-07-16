<template>
  <a-popup direction="center" :visible="visible" close-on-click-modal
    @open="handleOpen"
    @close="handleClose"
  >
    <div class="a-image-modal">
      <div class="head">
        <div class="name">
          <a-svg-icon icon-class="marksuit-image" />
          {{ $t('marksuit.image.add-image') }}
        </div>
        <a-svg-icon class="close" icon-class="close"
          @click="handleClose"
        />
      </div>
      <div class="source">
        <div class="field">
          <div class="key">{{ $t('marksuit.image.image-url') }}</div>
          <div class="value">
            <input type="text" ref="loadUrlRef" v-model="loadFormData.url"
              @keydown.enter="handleSubmit"
            />
          </div>
        </div>
        <div class="field">
          <div class="key">{{ $t('marksuit.image.image-name') }}</div>
          <div class="value">
            <input type="text" v-model="loadFormData.name"
              @keydown.enter="handleSubmit"
            />
          </div>
        </div>
        <div class="field">
          <div class="key">{{ $t('marksuit.image.image-description') }}</div>
          <div class="value">
            <input type="text" v-model="loadFormData.description"
              @keydown.enter="handleSubmit"
            />
          </div>
        </div>
      </div>
      <div class="metadata">
        <div class="button" @click="handleClose">
          {{ $t('marksuit.common.cancel') }}
        </div>
        <div class="button primary" @click="handleSubmit">
          {{ $t('marksuit.common.confirm') }}
        </div>
      </div>
    </div>
  </a-popup>
</template>

<script setup lang="ts">
import APopup from '../../common/popup.vue'

defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const loadFormData = reactive({
  url: 'http://',
  name: '',
  description: ''
})
const loadUrlRef = ref<HTMLInputElement | null>(null)

const handleEmit = defineEmits(['close', 'submit'])
const handleOpen = () => {
  nextTick(() => {
    loadUrlRef.value?.focus()
  })
}
const handleClose = () => {
  handleReset()
  handleEmit('close')
}
const handleReset = () => {
  loadFormData.url = 'http://'
  loadFormData.name = ''
  loadFormData.description = ''
}
const handleSubmit = () => {
  // 检查参数是否为空
  if (!loadFormData.url || !loadFormData.name) {
    return
  }
  handleEmit('submit', {
    url: loadFormData.url,
    name: loadFormData.name,
    description: loadFormData.description
  })
  handleEmit('close')
}
</script>
