<template>
  <div class="panel inline-flex-c-n-n">
    <template v-if="currentFile">
      <!-- 编辑器头部 -->
      <div class="head inline-flex-r-c-b">
        <div class="name inline-flex-r-c-n">
          <el-icon class="icon"><Document /></el-icon>
          <template v-if="isRenaming">
            <el-input ref="loadRenameRef" v-model="loadRenameValue"
              @blur="handleDocumentXhrRename"
              @keyup.enter="handleDocumentXhrRename"
              @keyup.escape="handleDocumentRenameCancel"
            >
              <template #append>.md</template>
            </el-input>
          </template>
          <template v-else>
            <div>{{ currentFile.filename }}</div>
            <el-icon v-if="!currentFile.core" class="rename" @click="handleDocumentRename">
              <Edit />
            </el-icon>
          </template>
          <el-tag v-if="isModified" type="warning" class="modified">
            {{ $t('memory.unsaved') }}
          </el-tag>
        </div>
        <div class="operation inline-flex-r-c-n">
          <a-button type="success" :icon="Check" size="small" :disabled="!isModified"
            @click="handleDocumentSave"
          >
            {{ $t('memory.save') }}
          </a-button>
          <a-button v-if="!currentFile.core" type="danger" size="small" :icon="Delete"
            @click="handleDocumentRemove(currentFile)"
          >
            {{ $t('common.delete') }}
          </a-button>
        </div>
      </div>
      <!-- 编辑器内容 -->
      <div class="content">
        <a-marksuit v-if="isContentReady" ref="loadMarksuitRef" kind="markdown"
          :content="loadContent"
          :option="{
            maxWidth: '1024px',
            isAutoCompletion: false
          }"
          @update:content="handleContentChange"
        />
      </div>
    </template>
    <template v-else>
      <a-nodata :loading="false" :success="true" size="large"
        :description="$t('memory.document-empty-description')"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import {
  Check,
  Delete,
  Document,
  Edit
} from '@element-plus/icons-vue'
import {
  ElInput
} from 'element-plus'
import AMarksuit from '@/marksuit/index.vue'

interface MemoryFile {
  filename: string
  core: boolean
  size: number
  date: number
}

const props = defineProps<{
  currentFile: MemoryFile | null
}>()

const handleEmit = defineEmits<{
  (e: 'save', content: string): void
  (e: 'remove', file: MemoryFile): void
  (e: 'rename', file: MemoryFile, newFilename: string): void
}>()

const loadContent = ref('')
const isModified = ref(false)
const isContentReady = ref(false)
const isIgnoreNextChange = ref(false)
const loadMarksuitRef = ref<InstanceType<typeof AMarksuit> | null>(null)
const loadRenameRef = ref<InstanceType<typeof ElInput> | null>(null)

// 重命名相关状态
const isRenaming = ref(false)
const loadRenameValue = ref('')

// 开始重命名
const handleDocumentRename = () => {
  if (!props.currentFile || props.currentFile.core) return
  // 去掉 .md 后缀作为编辑值
  const name = props.currentFile.filename
  loadRenameValue.value = name.endsWith('.md') ? name.slice(0, -3) : name
  isRenaming.value = true
  nextTick(() => {
    loadRenameRef.value?.focus()
  })
}
// 确认重命名
const handleDocumentXhrRename = () => {
  if (!isRenaming.value) return
  const value = loadRenameValue.value.trim()
  if (!value || !props.currentFile) {
    handleDocumentRenameCancel()
    return
  }
  let newFilename = value
  if (!newFilename.endsWith('.md')) {
    newFilename = newFilename + '.md'
  }
  // 文件名没有变化则直接取消
  if (newFilename === props.currentFile.filename) {
    handleDocumentRenameCancel()
    return
  }
  handleEmit('rename', props.currentFile, newFilename)
  isRenaming.value = false
}
// 取消重命名
const handleDocumentRenameCancel = () => {
  isRenaming.value = false
  loadRenameValue.value = ''
}

// 内容变化时标记为已修改
const handleContentChange = () => {
  if (isIgnoreNextChange.value) {
    isIgnoreNextChange.value = false
    return
  }
  isModified.value = true
}
// 获取当前编辑器中的 markdown 内容
const loadEditorContent = (): string => {
  if (!loadMarksuitRef.value) return ''
  return loadMarksuitRef.value.useRichEditor.useInstance().getMarkdown()
}
const handleDocumentSave = () => {
  if (!props.currentFile) return
  const content = loadEditorContent()
  handleEmit('save', content)
}
const handleDocumentRemove = (file: MemoryFile) => {
  handleEmit('remove', file)
}

// 加载文件内容（由父组件调用）
const setContent = (content: string) => {
  isContentReady.value = false
  loadContent.value = content
  isModified.value = false
  isIgnoreNextChange.value = true
  nextTick(() => {
    isContentReady.value = true
  })
}
// 保存成功后重置修改状态（由父组件调用）
const markSaved = () => {
  isModified.value = false
}
// 重置编辑器状态（由父组件调用）
const reset = () => {
  loadContent.value = ''
  isModified.value = false
  isContentReady.value = false
}

defineExpose({
  setContent,
  markSaved,
  reset
})
</script>
