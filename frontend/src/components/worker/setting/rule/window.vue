<template>
  <el-dialog v-model="visible" :title="$t('rules.create-title')" width="520px" :close-on-click-modal="false"
    @opened="handleWindowOpened"
  >
    <el-form :model="loadFormData" label-width="80px" @submit.prevent>
      <el-form-item :label="$t('rules.filename')">
        <el-input ref="loadInputRef" v-model="loadFormData.filename" :placeholder="$t('rules.filename-placeholder')" clearable
          @keyup.enter="handleSubmit"
        >
          <template #append>.md</template>
        </el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :disabled="!loadFormData.filename"
        @click="handleSubmit"
      >
        {{ $t('rules.create') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
const handleEmit = defineEmits<{
  (e: 'submit', filename: string): void
}>()

const visible = ref(false)
const loadFormData = reactive({ filename: '' })
const loadInputRef = ref<any>(null)

const open = () => {
  loadFormData.filename = ''
  visible.value = true
}
const close = () => {
  visible.value = false
}
const handleWindowOpened = () => {
  loadInputRef.value?.focus()
}
const handleSubmit = () => {
  let filename = loadFormData.filename.trim()
  if (!filename) return
  if (!filename.endsWith('.md')) {
    filename = filename + '.md'
  }
  handleEmit('submit', filename)
}

defineExpose({
  open,
  close
})
</script>
