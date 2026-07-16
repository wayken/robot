<template>
  <div class="a-setting-memory inline-flex-r-n-n" v-loading="progression.loading">
    <a-sidebar :data-list="dataList" :current-file="loadCurrentFile"
      :loading="progression.loading"
      :success="progression.success"
      @add="handleDocumentAdd"
      @select="handleDocumentSelect"
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
import APanel from '@/components/worker/setting/memory/panel.vue'
import AWindow from '@/components/worker/setting/memory/window.vue'
import ASidebar from '@/components/worker/setting/memory/sidebar.vue'

interface MemoryFile {
  filename: string
  core: boolean
  size: number
  date: number
}

const i18n = useI18n()
const route = useRoute()
const { ioRequest, progression } = useSocketIO()

// 文件列表
const dataList = ref<MemoryFile[]>([])
const loadCurrentFile = ref<MemoryFile | null>(null)
const loadPanelRef = ref<InstanceType<typeof APanel> | null>(null)
const loadWindowRef = ref<InstanceType<typeof AWindow> | null>(null)

const useWid = () => route.params.id as string

onMounted(() => {
  handleDataLoad()
})

// 加载文件列表
const handleDataLoad = () => {
  ioRequest('memory.index', { wid: useWid() }).then((result: any) => {
    dataList.value = result[0] || []
  })
}
const handleDocumentAdd = () => {
  loadWindowRef.value?.open()
}
const handleDocumentSelect = (data: MemoryFile) => {
  handleFileContentLoad(data)
}
const handleFileContentLoad = (data: MemoryFile) => {
  loadCurrentFile.value = data
  ioRequest('memory.read', { wid: useWid(), filename: data.filename }).then((result: any) => {
    const content = result[0] || ''
    loadPanelRef.value?.setContent(content)
  })
}
const handleDocumentSave = (content: string) => {
  if (!loadCurrentFile.value) return
  ioRequest('memory.write', {
    wid: useWid(),
    filename: loadCurrentFile.value.filename,
    content: content
  }).then((result: any) => {
    const success = result[0]
    if (success) {
      loadPanelRef.value?.markSaved()
      ElMessage.success(i18n.t('memory.save-success'))
    } else {
      ElMessage.error(i18n.t('memory.save-failed'))
    }
  })
}
const handleDocumentRemove = (data: MemoryFile) => {
  ElMessageBox.confirm(
    i18n.t('memory.delete-confirm', { placeholder: data.filename }),
    i18n.t('memory.delete-title'),
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
    ioRequest('memory.delete', params).then((result: any) => {
      const success = result[0]
      if (success) {
        ElMessage.success(i18n.t('memory.delete-success'))
        if (loadCurrentFile.value?.filename === data.filename) {
          loadCurrentFile.value = null
          loadPanelRef.value?.reset()
        }
        handleDataLoad()
      } else {
        ElMessage.error(i18n.t('memory.delete-failed'))
      }
    })
  }).catch(() => {})
}
const handleDocumentRename = (data: MemoryFile, newFilename: string) => {
  if (data.core) return
  if (dataList.value.some(f => f.filename === newFilename)) {
    ElMessage.warning(i18n.t('memory.filename-exists'))
    return
  }
  const params = {
    wid: useWid(),
    filename: data.filename,
    newFilename: newFilename
  }
  ioRequest('memory.rename', params).then((result: any) => {
    const success = result[0]
    if (success) {
      ElMessage.success(i18n.t('memory.rename-success'))
      data.filename = newFilename
      handleDataLoad()
    } else {
      ElMessage.error(i18n.t('memory.rename-failed'))
    }
  })
}
const handleDocumentXhrAdd = (filename: string) => {
  if (dataList.value.some(f => f.filename === filename)) {
    ElMessage.warning(i18n.t('memory.filename-exists'))
    return
  }
  const params = {
    wid: useWid(),
    filename: filename,
    content: ''
  }
  ioRequest('memory.write', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('memory.create-failed'))
      return
    }
    loadWindowRef.value?.close()
    ElMessage.success(i18n.t('memory.create-success'))
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
