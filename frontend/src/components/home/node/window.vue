<template>
  <el-dialog draggable width="720px" append-to-body class="a-node--window"
    :title="addition ? $t('node.add-node') : $t('node.edit-node')"
    :model-value="visible"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @opened="handleOpen"
    @close="handleClose"
  >
    <el-form ref="loadFormReference" label-position="right" label-width="120px"
      :model="loadFormData"
      :rules="loadFormRules"
      @keyup.enter="handleSubmit"
    >
      <el-form-item :label="$t('node.form-name')" prop="name">
        <el-input ref="loadNameReference" v-model="loadFormData.name" :placeholder="$t('node.form-name-placeholder')" maxlength="64" show-word-limit />
      </el-form-item>
      <el-form-item :label="$t('node.form-ip')" prop="ip" class="ip">
        <el-input v-model="loadFormData.ip" :placeholder="$t('node.form-ip-placeholder')" />
        <span class="separator">:</span>
        <el-form-item prop="port" class="port">
          <el-input-number v-model="loadFormData.port" :min="1" :max="65535" style="width: 140px;" controls-position="right" />
        </el-form-item>
        <el-button :loading="isLinkChecking" style="margin-left: 10px;"
          @click="handleLinkCheck"
        >
          {{ $t('node.check-node') }}
        </el-button>
      </el-form-item>
      <el-form-item :label="$t('node.form-signature')" prop="signature">
        <el-input v-model="loadFormData.signature" :placeholder="$t('node.form-signature-placeholder')" type="password" show-password />
      </el-form-item>
      <el-form-item :label="$t('node.form-remark')" prop="remark">
        <el-input v-model="loadFormData.remark" :placeholder="$t('node.form-remark-placeholder')" type="textarea" :rows="3" maxlength="200" show-word-limit />
      </el-form-item>
      <div class="notification inline-flex-r-s-n">
        <div class="icon">
          <el-icon><BellFilled /></el-icon>Tips：
        </div>
        <p>{{ $t('node.form-notification') }}</p>
      </div>
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
  BellFilled
} from '@element-plus/icons-vue'
import type {
  FormInstance, FormRules
} from 'element-plus'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useDeepClone } from '@/utils/dom'
import { useRequest } from '@/hooks/useRequest'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  addition: {
    type: Boolean,
    default: false
  },
  infomation: {
    type: Object,
    default: () => ({})
  }
})

const i18n = useI18n()
const { iopost, progression } = useRequest()
const isLinkChecking = ref(false)
const loadFormData = reactive<any>({
  id: '',
  name: '',
  ip: '127.0.0.1',
  port: 8162,
  signature: '',
  remark: ''
})
const loadFormReference = ref<FormInstance>()
const loadFormRules: FormRules = {
  name: [
    {
      required: true,
      trigger: 'blur',
      message: i18n.t('extension.please-input-placeholder') + i18n.t('node.form-name')
    }
  ],
  ip: [
    {
      required: true,
      trigger: 'blur',
      message: i18n.t('extension.please-input-placeholder') + i18n.t('node.form-ip')
    },
    {
      trigger: 'blur',
      pattern: /^(\d{1,3}\.){3}\d{1,3}$/,
      message: i18n.t('extension.please-input-placeholder') + i18n.t('node.form-ip')
    }
  ],
  port: [
    { 
      required: true, 
      trigger: 'blur', 
      message: i18n.t('extension.please-input-placeholder') + i18n.t('node.form-port')
    }
  ],
  signature: [
    { 
      required: true, 
      trigger: 'blur', 
      message: i18n.t('extension.please-input-placeholder') + i18n.t('node.form-signature')
    }
  ]
}
const loadNameReference = ref<HTMLElement>()

const handleLinkCheck = () => {
  if (!loadFormReference.value) return
  loadFormReference.value.validateField(['ip', 'port'], (valid) => {
    if (!valid) return
    isLinkChecking.value = true
    const remoteEndpoint = `http://${loadFormData.ip}:${loadFormData.port}/api/worker/status`
    axios.get(remoteEndpoint).then(() => {
      ElMessage.success(i18n.t('node.link-success'))
    }).catch(() => {
      ElMessage.error(i18n.t('node.link-failed'))
    }).finally(() => {
      isLinkChecking.value = false
    })
  })
}

const handleOpen = () => {
  if (!props.addition) {
    const info = useDeepClone(props.infomation)
    info.ip = info.address
    Object.assign(loadFormData, info)
  }
  loadNameReference.value?.focus()
}
const handleEmit = defineEmits(['close', 'update'])
const handleClose = () => {
  handleEmit('close')
  loadFormReference.value?.resetFields()
}
const handleSubmit = () => {
  if (!loadFormReference.value) return
  loadFormReference.value.validate((valid) => {
    if (!valid) return
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
    address: data.ip,
    port: data.port,
    signature: data.signature,
    remark: data.remark
  }
  iopost('node', 'xhrAddNode', params, {
    onMessage: true
  }).then(() => {
    handleEmit('update')
  })
}
const handleNodeUpdate = (data: any) => {
  const params: any = {
    id: data.id,
    name: data.name,
    address: data.ip,
    port: data.port,
    signature: data.signature,
    remark: data.remark
  }
  iopost('node', 'xhrUpdateNode', params, {
    onMessage: true
  }).then(() => {
    handleEmit('update')
  })
}
</script>
