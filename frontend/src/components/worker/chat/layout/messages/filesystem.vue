<template>
  <div class="filesystem" v-if="loadSummary.items.length">
    <div class="head">
      <div class="icon">
        <el-icon size="24px"><Document /></el-icon>
      </div>
      <div class="summary">
        <div class="title">{{ loadTitle }}</div>
        <div class="stat">
          <div class="is-addition">+{{ loadSummary.additions }}</div>
          <div class="is-deletion">-{{ loadSummary.deletions }}</div>
        </div>
      </div>
    </div>
    <div class="body" v-show="isOpened">
      <div class="unit"
        v-for="file in loadSummary.items"
        :key="file.path"
        :class="{
          'is-disabled': !isFilePreviewable(file)
        }"
        @click="handleFilePreviewOpen(file)"
      >
        <a-svg-icon class="file-icon" :icon-class="loadFileIconByName(file.path, false)" size="42px" />
        <div class="path">{{ file.path }}</div>
        <div class="numbers">
          <div class="is-addition">+{{ file.additions }}</div>
          <div class="is-deletion">-{{ file.deletions }}</div>
        </div>
      </div>
    </div>
    <div class="foot"
      :class="{
        'is-opened': isOpened
      }"
      @click="handleSwitch"
    >
      <el-icon><ArrowDown /></el-icon>
      <div class="name">
        {{ isOpened ? $t('chat.close-update-file') : $t('chat.open-update-file') }}
      </div>
    </div>
  </div>
  <a-monaco-view drawer close-on-click-modal close-on-press-escape width="860px"
    :visible="loadMonacoPreview.visible"
    :filename="loadMonacoPreview.filename"
    :content="loadMonacoPreview.content"
    @close="handleMonacoClose"
    @save="handleMonacoSave"
  />
</template>

<script setup lang="ts">
import {
  ElMessage
} from 'element-plus'
import {
  Document,
  ArrowDown
} from '@element-plus/icons-vue'
import { loadFileIconByName } from '@/utils/filetype'
import useSocketIO from '@/hooks/useSocketIO'
import AMonacoView from '@/components/worker/view/monaco.vue'

interface FormatRound {
  user: any
  common: any[]
}

interface FileUpdateItem {
  path: string
  operation: 'edit' | 'write' | 'delete'
  additions: number
  deletions: number
  pending: boolean
}

interface FileUpdateSummary {
  items: FileUpdateItem[]
  editing: number
  deleting: number
  additions: number
  deletions: number
  pending: boolean
}

const props = defineProps<{
  round: FormatRound
  actionKey: string
  streaming?: boolean
}>()

const i18n = useI18n()
const route = useRoute()
const { ioRequest } = useSocketIO()
const loadClosedFileUpdate = ref(new Set<string>())
const loadMonacoPreview = reactive({
  visible: false,
  path: '',
  filename: '',
  content: ''
})

const isFileUpdateTool = (tool: any): boolean => {
  if (tool?.fileUpdate?.path) return true
  const name = String(tool?.name || '').toLowerCase()
  return name === 'edit_file' || name === 'write_file' || name.includes('delete_file') || name.includes('remove_file')
}
const handleToolArgumentsParse = (args: any): any => {
  if (!args) return null
  if (typeof args !== 'string') return args
  try {
    return JSON.parse(args)
  } catch {
    return null
  }
}
const handleTextLines = (value: string = ''): string[] => {
  if (!value) return []
  const lines = value.split(/\r\n|\r|\n/)
  if (lines[lines.length - 1] === '') {
    lines.pop()
  }
  return lines
}
const handleLineDiffStats = (oldValue: string = '', newValue: string = '') => {
  const oldLines = handleTextLines(oldValue)
  const newLines = handleTextLines(newValue)
  if (oldLines.length * newLines.length > 200000) {
    return {
      additions: newLines.length,
      deletions: oldLines.length
    }
  }
  const lcs = Array.from({ length: oldLines.length + 1 }, () => Array(newLines.length + 1).fill(0))
  for (let i = oldLines.length - 1; i >= 0; i--) {
    for (let j = newLines.length - 1; j >= 0; j--) {
      lcs[i][j] = oldLines[i] === newLines[j] ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1])
    }
  }
  const common = lcs[0][0]
  return {
    additions: Math.max(newLines.length - common, 0),
    deletions: Math.max(oldLines.length - common, 0)
  }
}
const useToolFileUpdate = (tool: any): FileUpdateItem | null => {
  if (!tool || !isFileUpdateTool(tool)) return null
  const metadata = tool.fileUpdate || {}
  const args = handleToolArgumentsParse(tool.arguments) || {}
  const name = String(tool.name || '').toLowerCase()
  const path = metadata.path || args.path || args.file_path || args.filename || args.target_path
  if (!path) return null
  const operation = metadata.operation || (name === 'write_file' ? 'write' : (name.includes('delete') || name.includes('remove') ? 'delete' : 'edit'))
  const fallback = operation === 'write'
    ? { additions: handleTextLines(args.content || '').length, deletions: 0 }
    : operation === 'edit'
      ? handleLineDiffStats(args.oldText || args.old_string || args.old || '', args.newText || args.new_string || args.new || '')
      : { additions: 0, deletions: 0 }
  return {
    path,
    operation,
    additions: Number(metadata.additions ?? fallback.additions ?? 0),
    deletions: Number(metadata.deletions ?? fallback.deletions ?? 0),
    pending: tool.success !== true
  }
}
const loadSummary = computed<FileUpdateSummary>(() => {
  const mapping = new Map<string, FileUpdateItem>()
  for (const message of props.round.common) {
    const tools = message.message?.tools || []
    for (const tool of tools) {
      const change = useToolFileUpdate(tool)
      if (!change) continue
      const matched = mapping.get(change.path)
      if (matched) {
        matched.additions += change.additions
        matched.deletions += change.deletions
        matched.pending = matched.pending || change.pending
        if (change.operation === 'delete') {
          matched.operation = 'delete'
        }
      } else {
        mapping.set(change.path, { ...change })
      }
    }
  }
  const items = Array.from(mapping.values())
  return {
    items,
    editing: items.filter(item => item.operation !== 'delete').length,
    deleting: items.filter(item => item.operation === 'delete').length,
    additions: items.reduce((total, item) => total + item.additions, 0),
    deletions: items.reduce((total, item) => total + item.deletions, 0),
    pending: Boolean(props.streaming) && items.some(item => item.pending)
  }
})
const loadTitle = computed(() => {
  const summary = loadSummary.value
  if (summary.pending) {
    if (summary.editing > 0 && summary.deleting > 0) {
      return i18n.t('chat.file-update-pending-mixed', {
        editing: summary.editing,
        deleting: summary.deleting
      })
    }
    if (summary.deleting > 0) {
      return i18n.t('chat.file-update-pending-delete', {
        count: summary.deleting
      })
    }
    return i18n.t('chat.file-update-pending-edit', {
      count: summary.editing
    })
  }
  if (summary.editing > 0 && summary.deleting > 0) {
    return i18n.t('chat.file-update-completed-mixed', {
      editing: summary.editing,
      deleting: summary.deleting
    })
  }
  if (summary.deleting > 0) {
    return i18n.t('chat.file-update-completed-delete', {
      count: summary.deleting
    })
  }
  return i18n.t('chat.file-update-completed-edit', {
    count: summary.editing
  })
})
const isOpened = computed(() => {
  return !loadClosedFileUpdate.value.has(props.actionKey)
})
const handleSwitch = () => {
  const next = new Set(loadClosedFileUpdate.value)
  if (next.has(props.actionKey)) {
    next.delete(props.actionKey)
  } else {
    next.add(props.actionKey)
  }
  loadClosedFileUpdate.value = next
}
const useFileBasename = (path: string): string => {
  return path.split(/[\\/]/).filter(Boolean).pop() || path
}
const useDiskRelativePath = (path: string): string => {
  const normalized = String(path || '').replace(/\\/g, '/').replace(/^\/+/, '')
  const parts = normalized.split('/').filter(Boolean)
  const diskIndex = parts.indexOf('disk')
  if (diskIndex < 0 || diskIndex === parts.length - 1) {
    return ''
  }
  const relativeParts = parts.slice(diskIndex + 1)
  if (relativeParts.some(part => part === '..')) {
    return ''
  }
  return relativeParts.join('/')
}
const isFilePreviewable = (file: FileUpdateItem): boolean => {
  return file.operation !== 'delete' && Boolean(useDiskRelativePath(file.path))
}
const useWid = (): string => {
  return route.params.id as string
}
const handleFilePreviewOpen = (file: FileUpdateItem) => {
  if (!file?.path) return
  if (!isFilePreviewable(file)) {
    ElMessage.warning(i18n.t(file.operation === 'delete' ? 'chat.file-update-preview-deleted' : 'chat.file-update-preview-forbidden'))
    return
  }
  const diskPath = useDiskRelativePath(file.path)
  ioRequest('disk.read.text', {
    wid: useWid(),
    path: diskPath
  }).then((result: any) => {
    const content = Array.isArray(result) ? result[0] : result
    if (content == null) {
      ElMessage.error(i18n.t('error.network-error'))
      return
    }
    loadMonacoPreview.path = diskPath
    loadMonacoPreview.filename = useFileBasename(file.path)
    loadMonacoPreview.content = content
    loadMonacoPreview.visible = true
  })
}
const handleMonacoClose = () => {
  loadMonacoPreview.visible = false
}
const handleMonacoSave = (content: string) => {
  if (!loadMonacoPreview.path) return
  ioRequest('disk.write.text', {
    wid: useWid(),
    path: loadMonacoPreview.path,
    content
  }).then(() => {
    loadMonacoPreview.content = content
    ElMessage.success(i18n.t('chat.file-update-save-success'))
  })
}
</script>
