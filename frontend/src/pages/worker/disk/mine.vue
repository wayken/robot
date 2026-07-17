<template>
  <div class="a-disk-mine inline-flex-c-n-n">
    <!-- 顶部工具栏 -->
    <div class="head inline-flex-r-c-b">
      <div class="head--left breadcrumb inline-flex-r-c-n">
        <!-- 面包屑导航 -->
        <el-icon class="icon"><MessageBox /></el-icon>
        <div class="path" v-for="(data, index) in loadPathList" :key="index"
          :class="{
            'is-actived': loadPathList.length === index + 1
          }"
          @click="handlePathOpen(data)"
        >
          <div>{{ index === 0 ? $t('disk.all-files') : data.name }}</div>
          <el-icon v-if=" index < loadPathList.length - 1"><ArrowRight /></el-icon>
        </div>
      </div>
      <div class="head--right inline-flex-r-c-n">
        <a-button :icon="FolderAdd" @click="handleNewFolder">
          {{ $t('disk.new-folder') }}
        </a-button>
        <el-dropdown trigger="click" @command="handleUploadCommand">
          <a-button type="success" :icon="Upload">
            {{ $t('disk.upload') }}
          </a-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="file" :icon="Document">
                {{ $t('disk.upload-file') }}
              </el-dropdown-item>
              <el-dropdown-item command="folder" :icon="Folder">
                {{ $t('disk.upload-folder') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <a-button type="primary" :icon="Refresh" @click="handleDataLoad">
          {{ $t('common.refresh') }}
        </a-button>
      </div>
    </div>
    <!-- 文件列表 -->
    <div class="content">
      <div class="card">
        <div class="root inline-flex-c-c" v-for="(data, index) in infomation" :key="index"
          :class="{
            'is-selected': loadSelectedSet.has(index)
          }"
          @contextmenu.prevent="handleContextMenu($event, data, index)"
        >
          <div class="icon" @click="handleRootClick(data)">
            <img v-if="isImageType(data.name) && !data.directory && data._thumbnail" class="thumbnail"
              :src="data._thumbnail"
            />
            <a-svg-icon v-else :icon-class="loadFileIconByName(data.name, data.directory)" size="82px" />
          </div>
          <div class="name inline-text-ellipsis" :title="data.name" @dblclick="handleStartRename(index)">
            <input v-if="loadRenamingIndex === index" ref="loadRenameInputRef" class="rename" v-model="loadRenamingValue"
              @blur="handleConfirmRename"
              @keydown.enter="handleConfirmRename"
              @keydown.escape="handleCancelRename"
            />
            <span v-else>{{ data.name }}</span>
          </div>
          <a-checkbox size="small" :value="loadSelectedSet.has(index)" @change="(e, val) => handleSelect(index, val)" />
          <div class="control">
            <el-icon><Close /></el-icon>
          </div>
        </div>
      </div>
      <a-nodata v-if="infomation.length === 0" :loading="progression.loading" :success="progression.success" />
    </div>
    <!-- 底部批量操作栏 -->
    <transition name="el-zoom-in-bottom">
      <div class="operation inline-flex-r-c-c" v-if="loadSelectedSet.size > 0">
        <a-button :icon="Download" @click="handleBatchDownload">
          {{ $t('disk.batch-download') }}
        </a-button>
        <a-button v-if="loadSelectedSet.size === 1" :icon="Edit" @click="handleBatchRename">
          {{ $t('extension.rename') }}
        </a-button>
        <a-button :icon="Rank" @click="handleBatchMoveTo">
          {{ $t('disk.move-to') }}
        </a-button>
        <a-button type="danger" :icon="Delete" @click="handleBatchDelete">
          {{ $t('disk.batch-delete') }}
        </a-button>
      </div>
    </transition>
    <!-- 图片预览弹窗 -->
    <a-view-image :visible="loadImagePreview.visible"
      :image="loadImagePreview.current"
      :single="loadImageList.length <= 1"
      @prev="handleImagePreviewPrev"
      @next="handleImagePreviewNext"
      @close="handleImagePreviewClose"
    />
    <!-- 文本编辑弹窗 -->
    <a-view-monaco :visible="loadMonacoPreview.visible"
      :content="loadMonacoPreview.content"
      :filename="loadMonacoPreview.filename"
      @save="handleMonacoSave"
      @close="handleMonacoClose"
    />
    <!-- 右键菜单 -->
    <a-disk-contextmenu ref="loadContextMenuRef"
      :visible="loadContextMenu.visible"
      @open="handleContextMenuOpen"
      @close="handleContextMenuClose"
      @rename="handleContextMenuRename"
      @move-to="handleContextMenuMoveTo"
      @delete="handleContextMenuDelete"
    />
    <!-- 移动到弹窗 -->
    <a-disk-window
      :title="$t('disk.move-to')"
      :visible="loadMoveWindow.visible"
      :wid="useWid()"
      @close="handleMoveWindowClose"
      @submit="handleMoveWindowSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import {
  Close,
  Edit,
  Rank,
  Upload,
  Folder,
  Delete,
  Refresh,
  Document,
  Download,
  FolderAdd,
  MessageBox,
  ArrowRight
} from '@element-plus/icons-vue'
import {
  isImageType,
  isTextType,
  loadFileIconByName
} from '@/utils/filetype'
import useSocketIO from '@/hooks/useSocketIO'
import { useFileDialog } from '@/utils/dom'
import AViewImage from '@/components/worker/view/image.vue'
import ACheckbox from '@/components/common/checkbox/index.vue'
import AViewMonaco from '@/components/worker/view/monaco.vue'
import ADiskContextmenu from '@/components/worker/disk/mine/contextmenu.vue'
import ADiskWindow from '@/components/worker/disk/mine/window.vue'

const {
  ioRequest,
  progression
} = useSocketIO()
const i18n = useI18n()
const route = useRoute()
const router = useRouter()
const context: any = inject('context')

const infomation = ref<any[]>([])
const loadSelectedSet = ref<Set<number>>(new Set())
const useWid = () => route.params.id as string
const loadRenamingValue = ref('')
const loadRenamingIndex = ref<number | null>(null)
const loadRenameInputRef = ref<HTMLInputElement[] | null>(null)

// 使用会话存储持久化当前目录路径，防止路由切换后丢失
const STORAGE_KEY = computed(() => `disk_path_${useWid()}`)
const loadCurrentPath = computed(() => {
  // 不在磁盘路由时直接返回空，避免影响其他路由
  if (route.name !== 'worker-disk-mine') {
    return sessionStorage.getItem(STORAGE_KEY.value) || ''
  }
  const queryPath = route.query.path as string
  if (queryPath) {
    // 有 query.path 时同步到 sessionStorage
    sessionStorage.setItem(STORAGE_KEY.value, queryPath)
    return queryPath
  }
  // 没有 query.path 时尝试从 sessionStorage 恢复
  return sessionStorage.getItem(STORAGE_KEY.value) || ''
})
const loadPathList = computed(() => {
  const pathList: { name: string; path: string }[] = [{ name: 'root', path: '' }]
  if (loadCurrentPath.value) {
    // 路径可能是/也可能是\
    const parts = loadCurrentPath.value.split(/[/\\]+/)
    let accumulated = ''
    for (const part of parts) {
      accumulated = accumulated ? `${accumulated}/${part}` : part
      pathList.push({ name: part, path: accumulated })
    }
  }
  return pathList
})
// 获取当前目录下的图片文件列表
const loadImageList = computed(() => {
  return infomation.value.filter((data: any) => !data.directory && isImageType(data.name))
})

// 图片预览状态
const loadImagePreview = reactive({
  visible: false,
  current: { name: '', url: '' },
  currentIndex: 0
})
// Monaco 文本编辑状态
const loadMonacoPreview = reactive({
  path: '',
  content: '',
  filename: '',
  visible: false
})

const normalizeDiskEndpoint = (endpoint: string) => {
  if (!endpoint) return ''
  const normalized = endpoint.replace(/\/+$/, '')
  if (/^https?:\/\//.test(normalized)) {
    return normalized
  }
  return `http://${normalized}`
}
const encodeDiskPath = (path: string) => {
  return path.split(/[/\\]+/).filter(Boolean).map(encodeURIComponent).join('/')
}
const buildDiskFileUrl = async (filePath: string) => {
  const [endpoint, authorization] = await Promise.all([
    context?.diskEndpoint || Promise.resolve(''),
    context?.diskAuthorization || Promise.resolve('')
  ])
  const baseUrl = normalizeDiskEndpoint(endpoint)
  const query = new URLSearchParams()
  if (authorization) {
    query.set('authorization', authorization)
  }
  const url = `${baseUrl}/disk/${encodeURIComponent(useWid())}/${encodeDiskPath(filePath)}`
  const queryString = query.toString()
  return queryString ? `${url}?${queryString}` : url
}
const showImagePreviewFile = (file: any) => {
  if (!file) return
  if (file._thumbnail) {
    loadImagePreview.current = { name: file.name, url: file._thumbnail }
    loadImagePreview.visible = true
    return
  }
  const filePath = file.path || (unref(loadCurrentPath) ? `${unref(loadCurrentPath)}/${file.name}` : file.name)
  buildDiskFileUrl(filePath).then((url: string) => {
    file.url = url
    file._thumbnail = url
    loadImagePreview.current = { name: file.name, url }
    loadImagePreview.visible = true
  })
}

// 加载当前目录下的文件列表
const handleDataLoad = () => {
  const wid = useWid()
  const params = {
    wid: wid,
    path: unref(loadCurrentPath)
  }
  ioRequest('disk.index', params).then((result) => {
    const list = result[0] || []
    // 文件夹排前面，文件排后面
    list.sort((a: any, b: any) => {
      if (a.directory && !b.directory) return -1
      if (!a.directory && b.directory) return 1
      return 0
    })
    infomation.value = list
    loadSelectedSet.value = new Set()
    // 加载图片缩略图
    handleLoadThumbnails()
  })
}
// 新建文件夹
const handleNewFolder = () => {
  ElMessageBox.prompt(
    i18n.t('disk.new-folder-prompt'),
    i18n.t('disk.new-folder-title'),
    {
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('disk.new-folder-empty'),
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(({ value }) => {
    const name = value.trim()
    const wid = useWid()
    const params = {
      wid: wid,
      path: unref(loadCurrentPath),
      name: name
    }
    ioRequest('disk.mkdir', params).then((result: any) => {
      const success = Array.isArray(result) ? result[0] : result
      if (success) {
        ElMessage.success(i18n.t('disk.new-folder-success'))
        handleDataLoad()
      } else {
        ElMessage.error(i18n.t('disk.new-folder-failed'))
      }
    })
  }).catch(() => {})
}
// 上传文件/文件夹
const handleUploadCommand = async (command: string) => {
  const isFolder = command === 'folder'
  const files = await useFileDialog({
    multiple: true,
    directory: isFolder
  })
  if (!files || files.length === 0) return
  const wid = useWid()
  const currentPath = unref(loadCurrentPath)
  if (isFolder) {
    // 文件夹上传：保留相对路径结构
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const relativePath = (file as any).webkitRelativePath || file.name
      const filePath = currentPath ? `${currentPath}/${relativePath}` : relativePath
      const reader = new FileReader()
      reader.onload = () => {
        const arrayBuffer = reader.result as ArrayBuffer
        const base64 = btoa(
          new Uint8Array(arrayBuffer).reduce((data, byte) => data + String.fromCharCode(byte), '')
        )
        ioRequest('disk.write', { wid, path: filePath, content: base64 })
      }
      reader.readAsArrayBuffer(file)
    }
    ElMessage.success(i18n.t('disk.upload-success'))
    setTimeout(() => handleDataLoad(), 500)
  } else {
    // 文件上传
    let uploadCount = 0
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const filePath = currentPath ? `${currentPath}/${file.name}` : file.name
      const reader = new FileReader()
      reader.onload = () => {
        const arrayBuffer = reader.result as ArrayBuffer
        const base64 = btoa(
          new Uint8Array(arrayBuffer).reduce((data, byte) => data + String.fromCharCode(byte), '')
        )
        ioRequest('disk.write', { wid, path: filePath, content: base64 }).then(() => {
          uploadCount++
          if (uploadCount >= files!.length) {
            ElMessage.success(i18n.t('disk.upload-success'))
            handleDataLoad()
          }
        })
      }
      reader.readAsArrayBuffer(file)
    }
  }
}
// 文件选择
const handleSelect = (index: number, checked: boolean) => {
  const newSet = new Set(loadSelectedSet.value)
  if (checked) {
    newSet.add(index)
  } else {
    newSet.delete(index)
  }
  loadSelectedSet.value = newSet
}
// 批量下载
const handleBatchDownload = () => {
  const wid = useWid()
  for (const index of loadSelectedSet.value) {
    const data = infomation.value[index]
    if (!data || data.directory) continue
    const filePath = data.path || (unref(loadCurrentPath) ? `${unref(loadCurrentPath)}/${data.name}` : data.name)
    ioRequest('disk.read', { wid, path: filePath }).then((base64: string) => {
      if (base64) {
        const byteCharacters = atob(base64)
        const byteNumbers = new Array(byteCharacters.length)
        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i)
        }
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray])
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = data.name
        link.click()
        URL.revokeObjectURL(url)
      }
    })
  }
}
// 批量重命名（仅单选时可用）- 触发行内编辑
const handleBatchRename = () => {
  if (loadSelectedSet.value.size !== 1) return
  const index = Array.from(loadSelectedSet.value)[0]
  handleStartRename(index)
}

// 批量删除
const handleBatchDelete = () => {
  const selectedItems = Array.from(loadSelectedSet.value).map(i => infomation.value[i]).filter(Boolean)
  if (selectedItems.length === 0) return
  ElMessageBox.confirm(
    i18n.t('disk.batch-delete-confirm', { count: selectedItems.length }),
    i18n.t('common.tips'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(async () => {
    const wid = useWid()
    let successCount = 0
    for (const data of selectedItems) {
      const filePath = data.path || (unref(loadCurrentPath) ? `${unref(loadCurrentPath)}/${data.name}` : data.name)
      const result: any = await ioRequest('disk.delete', { wid, path: filePath })
      const success = Array.isArray(result) ? result[0] : result
      if (success) successCount++
    }
    if (successCount > 0) {
      ElMessage.success(i18n.t('disk.batch-delete-success', { count: successCount }))
      handleDataLoad()
    } else {
      ElMessage.error(i18n.t('disk.delete-failed'))
    }
  }).catch(() => {})
}

// 右键菜单
const loadContextMenuRef = ref<InstanceType<typeof ADiskContextmenu> | null>(null)
const loadContextMenu = reactive({
  visible: false,
  data: null as any
})
const handleContextMenu = (event: MouseEvent, data: any, index: number) => {
  loadContextMenu.data = data
  loadContextMenu.visible = true
  // 右键自动选中该项
  if (!loadSelectedSet.value.has(index)) {
    loadSelectedSet.value = new Set([index])
  }
  nextTick(() => {
    loadContextMenuRef.value?.show(event)
  })
}
const handleContextMenuClose = () => {
  loadContextMenu.visible = false
}
const handleContextMenuOpen = () => {
  if (loadContextMenu.data) {
    handleRootClick(loadContextMenu.data)
  }
}
const handleContextMenuRename = () => {
  const data = loadContextMenu.data
  if (!data) return
  const index = infomation.value.indexOf(data)
  if (index >= 0) {
    handleStartRename(index)
  }
}
// 行内重命名：开始编辑
const handleStartRename = (index: number) => {
  const data = infomation.value[index]
  if (!data) return
  loadRenamingIndex.value = index
  loadRenamingValue.value = data.name
  nextTick(() => {
    const inputs = loadRenameInputRef.value
    if (inputs && inputs.length > 0) {
      const input = inputs[0]
      input.focus()
      // 选中文件名（不包含扩展名），方便修改
      const dotIndex = data.name.lastIndexOf('.')
      if (!data.directory && dotIndex > 0) {
        input.setSelectionRange(0, dotIndex)
      } else {
        input.select()
      }
    }
  })
}
// 行内重命名：确认提交
const handleConfirmRename = () => {
  if (loadRenamingIndex.value === null) return
  const index = loadRenamingIndex.value
  const data = infomation.value[index]
  if (!data) {
    loadRenamingIndex.value = null
    return
  }
  const newName = loadRenamingValue.value.trim()
  // 如果名称为空或未改变则取消
  if (!newName || newName === data.name) {
    loadRenamingIndex.value = null
    return
  }
  loadRenamingIndex.value = null
  const wid = useWid()
  const params = {
    wid: wid,
    path: data.path || (unref(loadCurrentPath) ? `${unref(loadCurrentPath)}/${data.name}` : data.name),
    name: newName
  }
  ioRequest('disk.rename', params).then((result: any) => {
    const success = Array.isArray(result) ? result[0] : result
    if (success) {
      ElMessage.success(i18n.t('disk.rename-success'))
      handleDataLoad()
    } else {
      ElMessage.error(i18n.t('disk.rename-failed'))
    }
  })
}
// 行内重命名：取消
const handleCancelRename = () => {
  loadRenamingIndex.value = null
}
const handleContextMenuDelete = () => {
  const data = loadContextMenu.data
  if (!data) return
  ElMessageBox.confirm(
    i18n.t('disk.delete-confirm', { name: data.name }),
    i18n.t('common.tips'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(() => {
    const wid = useWid()
    const params = {
      wid: wid,
      path: data.path || (unref(loadCurrentPath) ? `${unref(loadCurrentPath)}/${data.name}` : data.name)
    }
    ioRequest('disk.delete', params).then((result: any) => {
      const success = Array.isArray(result) ? result[0] : result
      if (success) {
        ElMessage.success(i18n.t('disk.delete-success'))
        handleDataLoad()
      } else {
        ElMessage.error(i18n.t('disk.delete-failed'))
      }
    })
  }).catch(() => {})
}

// 移动到弹窗
const loadMoveWindow = reactive({
  visible: false
})
const handleContextMenuMoveTo = () => {
  loadMoveWindow.visible = true
}
const handleBatchMoveTo = () => {
  loadMoveWindow.visible = true
}
const handleMoveWindowClose = () => {
  loadMoveWindow.visible = false
}
const handleMoveWindowSubmit = async (targetPath: string) => {
  const wid = useWid()
  // 收集要移动的文件列表：优先使用批量选中项，否则使用右键菜单项
  let moveItems: any[] = []
  if (loadSelectedSet.value.size > 0) {
    moveItems = Array.from(loadSelectedSet.value).map(i => infomation.value[i]).filter(Boolean)
  } else if (loadContextMenu.data) {
    moveItems = [loadContextMenu.data]
  }
  if (moveItems.length === 0) return
  let successCount = 0
  for (const data of moveItems) {
    const sourcePath = data.path || (unref(loadCurrentPath) ? `${unref(loadCurrentPath)}/${data.name}` : data.name)
    const params = {
      wid: wid,
      path: sourcePath,
      target: targetPath
    }
    const result: any = await ioRequest('disk.move', params)
    const success = Array.isArray(result) ? result[0] : result
    if (success) successCount++
  }
  if (successCount > 0) {
    ElMessage.success(i18n.t('disk.move-success'))
    loadMoveWindow.visible = false
    handleDataLoad()
  } else {
    ElMessage.error(i18n.t('disk.move-failed'))
  }
}
// 加载图片缩略图
const handleLoadThumbnails = () => {
  const imageFiles = infomation.value.filter(
    (item: any) => !item.directory && isImageType(item.name)
  )
  for (const file of imageFiles) {
    const filePath = file.path || (unref(loadCurrentPath) ? `${unref(loadCurrentPath)}/${file.name}` : file.name)
    buildDiskFileUrl(filePath).then((url: string) => {
      file.url = url
      file._thumbnail = url
    })
  }
}
// 打开文件夹
const handlePathOpen = (data: any) => {
  if (data.path) {
    sessionStorage.setItem(STORAGE_KEY.value, data.path)
  } else {
    sessionStorage.removeItem(STORAGE_KEY.value)
  }
  router.push({
    path: route.path,
    query: data.path ? { path: data.path } : {}
  })
}
// 打开文件或文件夹
const handleRootClick = (data: any) => {
  if (data.directory) {
    handlePathOpen(data)
  } else if (isImageType(data.name)) {
    handleImagePreviewOpen(data)
  } else if (isTextType(data.name)) {
    handleMonacoOpen(data)
  }
}
// 打开图片预览
const handleImagePreviewOpen = (data: any) => {
  const index = loadImageList.value.findIndex((item: any) => item.name === data.name)
  loadImagePreview.currentIndex = index >= 0 ? index : 0
  const file = loadImageList.value[loadImagePreview.currentIndex]
  showImagePreviewFile(file)
}
// 关闭图片预览
const handleImagePreviewClose = () => {
  loadImagePreview.visible = false
}
// 预览上一张图片
const handleImagePreviewPrev = () => {
  if (loadImageList.value.length <= 1) return
  loadImagePreview.currentIndex = (loadImagePreview.currentIndex - 1 + loadImageList.value.length) % loadImageList.value.length
  const file = loadImageList.value[loadImagePreview.currentIndex]
  showImagePreviewFile(file)
}
// 预览下一张图片
const handleImagePreviewNext = () => {
  if (loadImageList.value.length <= 1) return
  loadImagePreview.currentIndex = (loadImagePreview.currentIndex + 1) % loadImageList.value.length
  const file = loadImageList.value[loadImagePreview.currentIndex]
  showImagePreviewFile(file)
}
// 打开文本编辑器
const handleMonacoOpen = (data: any) => {
  const wid = useWid()
  const filePath = data.path || (unref(loadCurrentPath) ? `${unref(loadCurrentPath)}/${data.name}` : data.name)
  ioRequest('disk.read.text', { wid, path: filePath }).then((result: any) => {
    const content = Array.isArray(result) ? result[0] : result
    if (content != null) {
      loadMonacoPreview.content = content
      loadMonacoPreview.filename = data.name
      loadMonacoPreview.path = filePath
      loadMonacoPreview.visible = true
    }
  })
}
// 关闭文本编辑器
const handleMonacoClose = () => {
  loadMonacoPreview.visible = false
}
// 保存文本文件
const handleMonacoSave = (content: string) => {
  const wid = useWid()
  const params = {
    wid,
    path: loadMonacoPreview.path,
    content
  }
  ioRequest('disk.write.text', params).then(() => {
    loadMonacoPreview.content = content
  })
}

onMounted(() => {
  handleDataLoad()
})
// 组件被 keep-alive 重新激活时，恢复路径并刷新数据
onActivated(() => {
  if (route.name !== 'worker-disk-mine') return
  const savedPath = sessionStorage.getItem(STORAGE_KEY.value) || ''
  const queryPath = route.query.path as string || ''
  // 如果 query.path 与保存的路径不一致，恢复路由 query
  if (savedPath && savedPath !== queryPath) {
    router.replace({
      path: route.path,
      query: { path: savedPath }
    })
  } else {
    handleDataLoad()
  }
})

watch(() => route.query.path,
  (newPath, oldPath) => {
    // 仅在路径确实发生变化时刷新（排除组件恢复时 query 从 undefined 变成空的情况）
    if (newPath !== oldPath && route.name === 'worker-disk-mine') {
      handleDataLoad()
    }
  }
)
</script>
