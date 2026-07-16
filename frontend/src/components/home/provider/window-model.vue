<template>
  <el-dialog draggable width="540px" top="12vh" class="a-provider-window-model" append-to-body
    :title="addition ? $t('provider.add-model') : $t('provider.edit-model')"
    :model-value="visible"
    @opened="handleOpen"
    @close="handleClose"
  >
    <el-form ref="loadFormRef" :model="loadFormData" :rules="loadFormRules" label-width="80px"
      @keyup.enter="handleSubmit"
    >
      <el-form-item :label="$t('provider.model-name')" required>
        <el-col :span="7">
          <el-form-item prop="provider">
            <el-select v-model="loadFormData.type" :teleported="false" prop="provider">
              <el-option v-for="(data, index) in loadProviderList" :key="index" :value="data.type">
                <div class="provider inline-flex-r-c-n">
                  <img class="icon" :src="data.icon" />
                  <div class="name">{{ $t('provider.' + data.type) }}</div>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="1"></el-col>
        <el-col :span="16">
          <el-form-item prop="name">
            <el-input ref="loadNameRef" v-model="loadFormData.name"></el-input>
          </el-form-item>
        </el-col>
      </el-form-item>
      <el-form-item :label="$t('provider.group-name')" prop="group">
        <el-input v-model="loadFormData.group"></el-input>
      </el-form-item>
      <el-form-item :label="$t('provider.model-properties')">
        <el-checkbox-group v-model="loadFormData.properties">
          <el-checkbox :value="1">{{ $t('provider.model-property-vision') }}</el-checkbox>
          <el-checkbox :value="2">{{ $t('provider.model-property-inference') }}</el-checkbox>
          <el-checkbox :value="3">{{ $t('provider.model-property-tool') }}</el-checkbox>
          <el-checkbox :value="4">{{ $t('provider.model-property-bedrock') }}</el-checkbox>
        </el-checkbox-group>
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
import { loadProviderList } from '@/config/data/model'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  addition: {
    type: Boolean,
    default: true
  },
  provider: {
    type: String,
    required: true
  },
  infomation: {
    type: Object,
    default: () => ({})
  }
})

const loadFormData = reactive({
  name: '',
  type: 'silicon',
  group: '',
  properties: [] as number[]
})
const loadFormRules = reactive<FormRules<any>>({
  name: [
    {
      trigger: 'blur',
      required: true
    }
  ],
  group: [
    {
      trigger: 'blur',
      required: true
    }
  ]
})
const loadFormRef = ref()
const loadNameRef = ref<HTMLInputElement | null>(null)

const handleEmit = defineEmits(['close', 'add', 'update'])
const handleClose = () => {
  handleEmit('close')
  loadFormRef.value?.resetFields()
}
const handleOpen = () => {
  if (!props.addition) {
    loadFormData.name = props.infomation.name || ''
    loadFormData.type = props.infomation.type || 'silicon'
    loadFormData.group = props.infomation.group || ''
    loadFormData.properties = props.infomation.properties ? [...props.infomation.properties] : []
  }
  nextTick(() => {
    loadNameRef.value?.focus()
  })
}
const handleSubmit = () => {
  loadFormRef.value.validate((valid: boolean) => {
    if (valid) {
      const data = {
        provider: props.provider,
        name: loadFormData.name,
        type: loadFormData.type,
        group: loadFormData.group,
        properties: loadFormData.properties
      }
      if (props.addition) {
        handleEmit('add', data)
      } else {
        handleEmit('update', data)
      }
      handleClose()
    }
  })
}
</script>
