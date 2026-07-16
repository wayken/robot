<template>
  <el-dialog draggable width="540px" top="12vh" class="a-provider-window-provider" append-to-body
    :title="$t('provider.add-provider')"
    :model-value="visible"
    @opened="handleOpen"
    @close="handleClose"
  >
    <el-form ref="loadFormRef" :model="loadFormData" :rules="loadFormRules" label-width="100px"
      @keyup.enter="handleSubmit"
    >
      <el-form-item :label="$t('provider.provider-name')" prop="name">
        <el-input ref="loadNameRef" v-model="loadFormData.name"></el-input>
      </el-form-item>
      <el-form-item :label="$t('provider.provider-type')" prop="provider">
        <el-select v-model="loadFormData.type" :teleported="false" prop="provider">
          <el-option v-for="(data, index) in loadProviderTypeList" :key="index"
            :label="data.name"
            :value="data.type"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">
        {{ $t('common.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleSubmit">
        {{ $t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormRules } from 'element-plus'
import { loadProviderTypeList } from '@/config/data/provider'

defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const loadFormData = reactive({
  name: '',
  type: 'openai'
})
const loadFormRules = reactive<FormRules<any>>({
  name: [
    {
      trigger: 'blur',
      required: true
    }
  ]
})
const loadFormRef = ref()
const loadNameRef = ref<HTMLInputElement | null>(null)

const handleEmit = defineEmits(['close', 'add'])
const handleClose = () => {
  handleEmit('close')
  handleReset()
}
const handleOpen = () => {
  nextTick(() => {
    loadNameRef.value?.focus()
  })
}
const handleReset = () => {
  loadFormRef.value.resetFields()
}
const handleSubmit = () => {
  loadFormRef.value.validate((valid: boolean) => {
    if (valid) {
      handleEmit('add', loadFormData)
      handleClose()
    }
  })
}
</script>
