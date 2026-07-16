<template>
  <el-dialog draggable width="640px" append-to-body class="a-assistant--window"
    :title="addition ? $t('assistant.add-assistant') : $t('assistant.edit-assistant')"
    :model-value="visible"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @opened="handleOpen"
    @close="handleClose"
  >
    <el-form ref="loadFormReference" label-position="right" label-width="100px"
      :model="loadFormData"
      :rules="loadFormRules"
      @keyup.enter="handleSubmit"
    >
      <el-form-item :label="$t('assistant.form-name')" prop="name">
        <el-input ref="loadNameReference" v-model="loadFormData.name" maxlength="64" show-word-limit
          :placeholder="$t('assistant.form-name-placeholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('assistant.form-mode')" prop="mode">
        <div class="mode">
          <div class="unit"
            :class="{
              'is-active': loadFormData.mode === 0
            }"
            @click="loadFormData.mode = 0"
          >
            <div class="head inline-flex-r-c-n">
              <el-icon class="icon"><Monitor /></el-icon>
              <span class="name">{{ $t('assistant.mode-standard') }}</span>
              <el-icon v-if="loadFormData.mode === 0" class="check"><Check /></el-icon>
            </div>
            <div class="description">{{ $t('assistant.mode-standard-description') }}</div>
          </div>
          <div class="unit"
            :class="{
              'is-active': loadFormData.mode === 1
            }"
            @click="loadFormData.mode = 1"
          >
            <div class="head inline-flex-r-c-n">
              <el-icon class="icon"><Cpu /></el-icon>
              <span class="name">{{ $t('assistant.mode-agent') }}</span>
              <el-icon v-if="loadFormData.mode === 1" class="check"><Check /></el-icon>
            </div>
            <div class="description">{{ $t('assistant.mode-agent-description') }}</div>
          </div>
        </div>
      </el-form-item>
      <el-form-item :label="$t('assistant.form-node')" prop="nodeId">
        <el-select v-model="loadFormData.nodeId" style="width: 100%"
          :placeholder="$t('assistant.form-node-placeholder')"
        >
          <el-option v-for="node in loadNodeList" :key="node.id"
            :value="node.id"
            :label="node.hostname"
          >
            <div class="inline-flex-r-c-n" style="gap: 8px;">
              <div class="dot"
                :style="{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  flexShrink: 0,
                  background: node.status === 1 ? 'var(--el-color-success)' : node.status === 2 ? 'var(--el-color-danger)' : 'var(--el-color-info)'
                }"
              />
              <span>{{ node.hostname }}</span>
              <span style="color: var(--el-text-color-secondary); font-size: 12px;">{{ node.address }}:{{ node.port }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('assistant.form-remark')" prop="remark">
        <el-input v-model="loadFormData.remark" type="textarea" maxlength="200" show-word-limit
          :rows="3"
          :placeholder="$t('assistant.form-remark-placeholder')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="footer">
        <el-button @click="handleClose">
          {{ $t('common.cancel') }}
        </el-button>
        <el-button type="primary" :loading="progression.sending"
          @click="handleSubmit"
        >
          {{ $t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  Monitor,
  Cpu,
  Check
} from '@element-plus/icons-vue'
import type {
  FormRules,
  FormInstance
} from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRequest } from '@/hooks/useRequest'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  addition: {
    type: Boolean,
    default: true
  },
  infomation: {
    type: Object,
    default: () => ({})
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const loadDefaultForm = () => ({
  id: '',
  name: '',
  mode: 0,
  node: '',
  remark: ''
})

const {
  ioload,
  iopost,
  progression
} = useRequest()
const loadNodeList = ref<any[]>([])

const i18n = useI18n()
const loadFormData = reactive<any>(loadDefaultForm())
const loadFormReference = ref<FormInstance>()
const loadFormRules: FormRules = {
  name: [
    {
      required: true,
      trigger: 'blur',
      message: i18n.t('extension.please-input-placeholder') + i18n.t('assistant.form-name')
    }
  ],
  mode: [
    {
      required: true,
      trigger: 'change',
      message: i18n.t('extension.please-input-placeholder') + i18n.t('assistant.form-mode')
    }
  ],
  nodeId: [
    {
      required: true,
      trigger: 'change',
      message: i18n.t('extension.please-input-placeholder') + i18n.t('assistant.form-node')
    }
  ]
}
const loadNameReference = ref<HTMLElement>()

const handleOpen = () => {
  if (!props.addition && props.infomation) {
    Object.assign(loadFormData, { ...loadDefaultForm(), ...props.infomation })
  }
  loadNameReference.value?.focus()
  ioload('node', 'loadNodeList', null).then((result) => {
    loadNodeList.value = result || []
    if (props.addition && loadNodeList.value.length > 0 && !loadFormData.nodeId) {
      loadFormData.nodeId = loadNodeList.value[0].id
    }
  })
}

const handleEmit = defineEmits(['close', 'update'])
const handleClose = () => {
  handleEmit('close')
  loadFormReference.value?.resetFields()
  Object.assign(loadFormData, loadDefaultForm())
}
const handleSubmit = () => {
  if (!loadFormReference.value) return
  const node = loadNodeList.value.find((n) => String(n.id) === String(loadFormData.nodeId))
  if (!node) {
    ElMessage.error(i18n.t('assistant.node-not-found'))
    return
  }
  loadFormReference.value.validate(async (valid) => {
    if (!valid) return
    try {
      progression.sending = true
    } catch (err: any) {
      progression.sending = false
      ElMessage.error(err?.message ?? i18n.t('assistant.node-connect-failed'))
      return
    }
    if (props.addition) {
      handleNodeAdd(loadFormData)
    } else {
      handleNodeUpdate(loadFormData)
    }
  })
}
const handleNodeAdd = (data: any) => {
  const params: any = {
    name: data.name,
    mode: data.mode,
    nodeId: data.nodeId,
    status: 0,
    remark: data.remark
  }
  iopost('assistant', 'xhrAddAssistant', params, {
    onMessage: true
  }).then(() => {
    handleEmit('update')
  })
}
const handleNodeUpdate = (data: any) => {
  const params: any = {
    id: data.id,
    name: data.name,
    mode: data.mode,
    nodeId: data.nodeId,
    remark: data.remark
  }
  iopost('assistant', 'xhrUpdateAssistant', params, {
    onMessage: true
  }).then(() => {
    handleEmit('update')
  })
}
</script>
