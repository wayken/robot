<template>
  <div class="a-setting-rule inline-flex-r-n-n" v-loading="progression.loading">
    <a-sidebar :data-list="dataList" :current-file="loadCurrentFile"
      :loading="progression.loading"
      :success="progression.success"
      @add="handleDocumentAdd"
      @select="handleDocumentSelect"
      @switch="handleDocumentSwitch"
    />
    <a-panel ref="loadPanelRef" :current-file="loadCurrentFile"
      @save="handleDocumentSave"
      @remove="handleDocumentRemove"
      @rename="handleDocumentRename"
    />
    <a-window ref="loadWindowRef" @submit="handleDocumentXhrAdd" />
  </div>
</template>

<script setup lang="ts">
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import useSocketIO from '@/hooks/useSocketIO'
import APanel from '@/components/worker/setting/rule/panel.vue'
import AWindow from '@/components/worker/setting/rule/window.vue'
import ASidebar from '@/components/worker/setting/rule/sidebar.vue'

interface RuleFile {
  filename: string
  core: boolean
  enabled: boolean
  size: number
  date: number
}

const i18n = useI18n()
const route = useRoute()
const { ioRequest, progression } = useSocketIO()

// 文件列表
const dataList = ref<RuleFile[]>([])
const loadCurrentFile = ref<RuleFile | null>(null)
const loadPanelRef = ref<InstanceType<typeof APanel> | null>(null)
const loadWindowRef = ref<InstanceType<typeof AWindow> | null>(null)

const useWid = () => route.params.id as string

onMounted(() => {
  handleDataLoad()
})

// 加载文件列表
const handleDataLoad = () => {
  ioRequest('rules.index', { wid: useWid() }).then((result: any) => {
    dataList.value = result[0] || []
  })
}
const handleDocumentAdd = () => {
  loadWindowRef.value?.open()
}
const handleDocumentSelect = (data: RuleFile) => {
  handleFileContentLoad(data)
}
const handleFileContentLoad = (data: RuleFile) => {
  loadCurrentFile.value = data
  ioRequest('rules.read', { wid: useWid(), filename: data.filename }).then((result: any) => {
    const content = result[0] || ''
    loadPanelRef.value?.setContent(content)
  })
}
const handleDocumentSave = (content: string) => {
  if (!loadCurrentFile.value) return
  ioRequest('rules.write', {
    wid: useWid(),
    filename: loadCurrentFile.value.filename,
    content: content
  }).then((result: any) => {
    const success = result[0]
    if (success) {
      loadPanelRef.value?.markSaved()
      ElMessage.success(i18n.t('rules.save-success'))
    } else {
      ElMessage.error(i18n.t('rules.save-failed'))
    }
  })
}
const handleDocumentRemove = (data: RuleFile) => {
  ElMessageBox.confirm(
    i18n.t('rules.delete-confirm', { placeholder: data.filename }),
    i18n.t('rules.delete-title'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.delete')
    }
  ).then(() => {
    const params = {
      wid: useWid(),
      filename: data.filename
    }
    ioRequest('rules.delete', params).then((result: any) => {
      const success = result[0]
      if (success) {
        ElMessage.success(i18n.t('rules.delete-success'))
        if (loadCurrentFile.value?.filename === data.filename) {
          loadCurrentFile.value = null
          loadPanelRef.value?.reset()
        }
        handleDataLoad()
      } else {
        ElMessage.error(i18n.t('rules.delete-failed'))
      }
    })
  }).catch(() => {})
}
const handleDocumentRename = (data: RuleFile, newFilename: string) => {
  if (data.core) return
  if (dataList.value.some(f => f.filename === newFilename)) {
    ElMessage.warning(i18n.t('rules.filename-exists'))
    return
  }
  const params = {
    wid: useWid(),
    filename: data.filename,
    newFilename: newFilename
  }
  ioRequest('rules.rename', params).then((result: any) => {
    const success = result[0]
    if (success) {
      ElMessage.success(i18n.t('rules.rename-success'))
      data.filename = newFilename
      handleDataLoad()
    } else {
      ElMessage.error(i18n.t('rules.rename-failed'))
    }
  })
}
const handleDocumentSwitch = (data: RuleFile, enabled: boolean) => {
  const params = {
    wid: useWid(),
    filename: data.filename,
    enabled: enabled
  }
  ioRequest('rules.switch', params).then((result: any) => {
    const success = result[0]
    if (success) {
      data.enabled = enabled
      ElMessage.success(enabled ? i18n.t('rules.enabled-success') : i18n.t('rules.disabled-success'))
    } else {
      ElMessage.error(i18n.t('rules.switch-failed'))
    }
  })
}
const handleDocumentXhrAdd = (filename: string) => {
  if (dataList.value.some(f => f.filename === filename)) {
    ElMessage.warning(i18n.t('rules.filename-exists'))
    return
  }
  const params = {
    wid: useWid(),
    filename: filename,
    content: ''
  }
  ioRequest('rules.write', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('rules.create-failed'))
      return
    }
    loadWindowRef.value?.close()
    ElMessage.success(i18n.t('rules.create-success'))
    handleDataLoad()
    nextTick(() => {
      const newFile = dataList.value.find(f => f.filename === filename)
      if (newFile) {
        handleFileContentLoad(newFile)
      }
    })
  })
}
</script>
