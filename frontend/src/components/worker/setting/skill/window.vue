<template>
  <el-dialog v-model="isVisible" width="720px"
    :title="$t('skill-setting.edit-title', { placeholder: loadSkillName })"
    :close-on-click-modal="false"
    @opened="handleDialogOpened"
  >
    <div class="main">
      <el-input ref="loadEditorRef" v-model="loadContent" type="textarea" :rows="18"
        :placeholder="$t('skill-setting.editor-placeholder')"
      />
    </div>
    <template #footer>
      <el-button @click="isVisible = false">
        {{ $t('common.cancel') }}
      </el-button>
      <el-button type="primary" :loading="isSaving"
        @click="handleSubmit"
      >
        {{ $t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
const handlEmit = defineEmits<{
  (e: 'submit', name: string, content: string): void
}>()

const isVisible = ref(false)
const isSaving = ref(false)
const loadSkillName = ref('')
const loadContent = ref('')
const loadEditorRef = ref<any>(null)

const open = (name: string, content: string) => {
  loadSkillName.value = name
  loadContent.value = content
  isVisible.value = true
}
const close = () => {
  isVisible.value = false
}
const setSaving = (val: boolean) => {
  isSaving.value = val
}
const handleDialogOpened = () => {
  loadEditorRef.value?.focus()
}
const handleSubmit = () => {
  handlEmit('submit', loadSkillName.value, loadContent.value)
}

defineExpose({
  open,
  close,
  setSaving
})
</script>
