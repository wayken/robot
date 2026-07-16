<template>
  <a-popup direction="center" :visible="visible" close-on-click-modal
    @open="handleOpen"
    @close="handleClose"
  >
    <div class="a-link-modal">
      <div class="head">
        <div class="name">
          <a-svg-icon icon-class="marksuit-link" />
          {{ $t('marksuit.link.add-link') }}
        </div>
        <a-svg-icon class="close" icon-class="close"
          @click="handleClose"
        />
      </div>
      <div class="source">
        <div class="field">
          <div class="key">{{ $t('marksuit.link.link-url') }}</div>
          <div class="value">
            <input type="text" ref="urlRef" v-model="loadFormData.url"
              @keydown.enter="handleSubmit"
            />
          </div>
        </div>
        <div class="field">
          <div class="key">{{ $t('marksuit.link.link-name') }}</div>
          <div class="value">
            <input type="text" v-model="loadFormData.name"
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

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  defaultUrl: {
    type: String,
    default: ''
  }
})
const loadFormData = reactive({
  url: 'http://',
  name: ''
})
const urlRef = ref<HTMLInputElement | null>(null)

const handleEmit = defineEmits(['close', 'submit'])
const handleOpen = () => {
  if (props.defaultUrl) {
    loadFormData.url = props.defaultUrl
  }
  nextTick(() => {
    urlRef.value?.focus()
  })
}
const handleClose = () => {
  handleEmit('close')
  nextTick(() => handleReset())
}
const handleReset = () => {
  loadFormData.url = 'http://'
  loadFormData.name = ''
}
const handleSubmit = () => {
  // 检查参数是否为空
  if (!loadFormData.url) {
    return
  }
  if (!loadFormData.name) {
    loadFormData.name = loadFormData.url
  }
  handleEmit('submit', {
    url: loadFormData.url,
    name: loadFormData.name
  })
  handleEmit('close')
}
</script>
