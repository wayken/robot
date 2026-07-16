<template>
  <el-dialog v-model="isVisible" width="560px"
    :title="$t('toolkit.edit-properties-title', { placeholder: loadTool?.name || '' })"
    :close-on-click-modal="false"
    @opened="handleWindowOpened"
  >
    <div class="properties">
      <div class="hint">{{ $t('toolkit.properties-hint') }}</div>
      <div class="source">
        <div class="module inline-flex-r-c-n" v-for="(data, index) in loadPropertiesList" :key="index">
          <el-input v-model="data.key" class="key"
            :ref="(el: any) => { if (index === 0) loadFirstInputRef = el }"
            :placeholder="$t('toolkit.property-key')"
          />
          <span class="separator">=</span>
          <el-input v-model="data.value" class="value"
            :placeholder="$t('toolkit.property-value')"
          />
          <el-icon class="icon" @click="handleRemoveProperty(index)">
            <Close />
          </el-icon>
        </div>
      </div>
      <el-button type="primary" plain @click="handleAddProperty" class="add-btn">
        + {{ $t('toolkit.add-property') }}
      </el-button>
    </div>
    <template #footer>
      <el-button @click="isVisible = false">
        {{ $t('common.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleSubmit" :loading="isSaving">
        {{ $t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  Close
} from '@element-plus/icons-vue'

interface ToolData {
  name: string
  properties: Record<string, any>
}

interface PropertyEntry {
  key: string
  value: string
}

const handleEmit = defineEmits<{
  (e: 'submit', name: string, properties: Record<string, string>): void
}>()

const isVisible = ref(false)
const isSaving = ref(false)
const loadTool = ref<ToolData | null>(null)
const loadPropertiesList = ref<PropertyEntry[]>([])
const loadFirstInputRef = ref<any>(null)

const open = (data: ToolData) => {
  loadTool.value = data
  const props = data.properties || {}
  loadPropertiesList.value = Object.entries(props).map(([key, value]) => ({
    key,
    value: String(value)
  }))
  if (loadPropertiesList.value.length === 0) {
    loadPropertiesList.value.push({ key: '', value: '' })
  }
  isVisible.value = true
}
const close = () => {
  isVisible.value = false
}
const setSaving = (val: boolean) => {
  isSaving.value = val
}
const handleWindowOpened = () => {
  loadFirstInputRef.value?.focus()
}
const handleAddProperty = () => {
  loadPropertiesList.value.push({ key: '', value: '' })
}
const handleRemoveProperty = (index: number) => {
  loadPropertiesList.value.splice(index, 1)
}
const handleSubmit = () => {
  if (!loadTool.value) return
  const properties: Record<string, string> = {}
  for (const data of loadPropertiesList.value) {
    const key = data.key.trim()
    if (key) {
      properties[key] = data.value
    }
  }
  handleEmit('submit', loadTool.value.name, properties)
}

defineExpose({
  open,
  close,
  setSaving
})
</script>
