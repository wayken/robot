<template>
  <div class="document inline-flex-c-n-n" v-loading="progression.loading">
    <div v-if="!isDocumentActived" class="welcome inline-flex-c-c-c">
      <a-nodata :loading="false" :success="true" size="large" />
    </div>
    <div v-else class="source inline-flex-c-n-n">
      <a-marksuit v-if="loadDocumentType === 'md' && loadDocumentContent !== null" ref="loadMarksuitRef" kind="markdown"
        :content="loadDocumentContent"
        :option="{
          maxWidth: '1024px',
          isAutoCompletion: false
        }"
        @update:content="handleContentUpdate"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import AMarksuit from '@/marksuit/index.vue'
import useSocketIO from '@/hooks/useSocketIO'
import { ElMessage } from 'element-plus'

const route = useRoute()
const {
  ioRequest,
  progression
} = useSocketIO()

const loadMarksuitRef = ref<InstanceType<typeof AMarksuit>>()
const loadDocumentType = ref('')
const loadDocumentContent = ref<string | null>(null)
const loadDocumentPath = ref('')
const loadSupportTypeList = ['md']
let loadSaveTimer: ReturnType<typeof setTimeout> | null = null
const isDocumentActived = computed(() => {
  return loadDocumentType.value !== '' && loadSupportTypeList.includes(loadDocumentType.value)
})

const handleContentUpdate = (content: string) => {
  if (!loadDocumentPath.value) return
  if (loadSaveTimer) {
    clearTimeout(loadSaveTimer)
  }
  loadSaveTimer = setTimeout(() => {
    handleAutoSave(content)
  }, 1200)
}
const handleAutoSave = (content: string) => {
  const params = {
    wid: route.params.id,
    path: loadDocumentPath.value,
    content: content
  }
  ioRequest('wiki.document.write', params)
}
const handleManualSave = () => {
  if (!loadDocumentPath.value || !loadMarksuitRef.value) return
  // 清除自动保存定时器，避免重复保存
  if (loadSaveTimer) {
    clearTimeout(loadSaveTimer)
    loadSaveTimer = null
  }
  const content = loadMarksuitRef.value.useRichEditor.useMarkdownContent()
  const params = {
    wid: route.params.id,
    path: loadDocumentPath.value,
    content: content
  }
  ioRequest('wiki.document.write', params).then(() => {
    ElMessage.success('保存成功')
  })
}
const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    e.stopPropagation()
    handleManualSave()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown, true)
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown, true)
  if (loadSaveTimer) {
    clearTimeout(loadSaveTimer)
    loadSaveTimer = null
  }
})
const handleDataReset = () => {
  loadDocumentType.value = ''
  loadDocumentContent.value = null
  loadDocumentPath.value = ''
  if (loadSaveTimer) {
    clearTimeout(loadSaveTimer)
    loadSaveTimer = null
  }
}
const handleDataLoad = (path: string, name: string) => {
  const filePath = (!path || path === '/') ? name : path + '/' + name
  // 切换文档时清除上一个定时器
  if (loadSaveTimer) {
    clearTimeout(loadSaveTimer)
    loadSaveTimer = null
  }
  const params = {
    wid: route.params.id,
    path: filePath
  }
  loadDocumentContent.value = null
  loadDocumentPath.value = ''
  ioRequest('wiki.document.read', params).then((result) => {
    const content = result[0]
    loadDocumentType.value = name.split('.').pop() as string
    loadDocumentPath.value = filePath
    nextTick(() => {
      if (loadDocumentType.value === 'md') {
        loadDocumentContent.value = content
      }
    })
  })
}

watch(
  () => [route.query.path, route.query.file] as [string | undefined, string | undefined],
  ([path, file]) => {
    if (file && path !== undefined && path !== null) {
      handleDataLoad(path as string, file as string)
    } else if (!file) {
      handleDataReset()
    }
  },
  { immediate: true }
)

defineExpose({
  handleDataLoad,
  handleDataReset
})
</script>
