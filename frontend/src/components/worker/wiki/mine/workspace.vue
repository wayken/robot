<template>
  <div class="workspace" v-loading="progression.loading"
    :style="{
      width: loadWidth + 'px'
    }"
    :class="{
      'is-dragging': isDragging
    }"
  >
    <div class="wrapper inline-flex-c-n-n"
      :style="{
        width: loadWidth + 'px'
      }"
    >
      <div class="head inline-flex-r-c-n">
        <el-input v-model="loadSearchKeyword" :prefix-icon="Search" class="is-round" clearable />
        <a-svg-icon icon-class="sort-down" />
      </div>
      <div class="navigation inline-flex-r-c-n">
        <el-icon v-if="!useParentPath().root"
          @click="handleBack"
        ><ArrowLeft /></el-icon>
        <div class="root">
          {{ useParentPath().path === '/' ? $t('wiki.mine-wiki') : useParentPath().path }}
        </div>
      </div>
      <div class="content inline-flex-c-n-n">
        <ul class="source">
          <li v-for="(data, index) in loadFilteredList" :key="index"
            :class="{
              'is-actived': loadActivedIndex === index
            }"
            @click="handleClick(data, index)"
            @dblclick="handleDbClick(data)"
          >
            <div class="name inline-flex-r-c-n">
              <a-svg-icon :icon-class="loadFileIconByName(data.name, data.isDirectory)" />
              <div v-if="loadEditNode.path === data.path" class="rename">
                <input ref="loadRenameRef" v-model="loadEditNode.name" type="text"
                  @click.stop
                  @blur="handleXhrEdit(data)"
                  @keydown.escape="loadEditNode = {}"
                  @keyup.enter="(<HTMLElement> $event.target).blur()"
                />
              </div>
              <div v-else class="text" :title="data.name">{{ data.name }}</div>
              <el-icon v-show="loadEditNode.path !== data.path" @click.stop="handleContextView($event, data)">
                <MoreFilled />
              </el-icon>
            </div>
            <div class="metadata">
              <div class="date">{{ dateminuteftfn(data.date) }}</div>
            </div>
          </li>
        </ul>
        <a-nodata v-if="loadFilteredList.length === 0" :loading="false" :success="true"
          :description="$t('wiki.no-document')"
        >
          <a-button :icon="Plus">
            {{ $t('wiki.new-document') }}
          </a-button>
        </a-nodata>
      </div>
    </div>
    <div class="flexible" @mousedown="handleResize"></div>
    <!-- 右键菜单 -->
    <a-contextmenu ref="loadContextmenuRef" :data="loadContextNode" :visible="isContextmenuView"
      @close="isContextmenuView = false"
      @add="handleContextAdd"
      @rename="handleContextRename"
      @moveto="handleContextMoveTo"
      @delete="handleContextDelete"
    ></a-contextmenu>
    <!-- 移动到弹窗 -->
    <a-wiki-window :title="$t('wiki.move-to')" :visible="isWindowView" :wid="useWid()"
      @close="isWindowView = false"
      @submit="handleMoveToSubmit"
    ></a-wiki-window>
  </div>
</template>

<script setup lang="ts">
import {
  Plus,
  Search,
  ArrowLeft,
  MoreFilled
} from '@element-plus/icons-vue'
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import {
  useDraggingResize
} from '@/hooks/useDraggingResize'
import useSocketIO from '@/hooks/useSocketIO'
import { useDocumentParentPath } from '@/utils/tool'
import { loadFileIconByName } from '@/utils/filetype'
import { dateminuteftfn } from '@/hooks/useDataFormatter'
import AContextmenu from '../contextmenu.vue'
import AWikiWindow from '../window.vue'

const {
  ioRequest,
  progression
} = useSocketIO()
const {
  width: loadWidth,
  isDragging,
  handleResize
} = useDraggingResize({ initialWidth: 282, min: 178, max: 420 })

const route = useRoute()
const router = useRouter()
const dataList = ref<any[]>([])
const loadActivedPath = ref('')
const loadActivedIndex = ref(-1)
const loadSearchKeyword = ref('')
const useWid = () => route.params.id as string

const i18n = useI18n()
const loadEditNode = ref<any>({})
const isContextmenuView = ref(false)
const loadContextNode = ref<any>({})
const loadContextmenuRef = ref<InstanceType<typeof AContextmenu>>()
const loadRenameRef = ref<HTMLInputElement | HTMLInputElement[]>()
// 移动到弹窗状态
const isWindowView = ref(false)
const loadMoveData = ref<any>(null)

const STORAGE_KEY = computed(() => `wiki_path_${useWid()}`)

const loadFilteredList = computed(() => {
  const kw = loadSearchKeyword.value.trim().toLowerCase()
  if (!kw) return dataList.value
  return dataList.value.filter((data: any) =>
    data.name?.toLowerCase().includes(kw)
  )
})
const loadSessionState = () => {
  try {
    const value = sessionStorage.getItem(STORAGE_KEY.value)
    if (value) return JSON.parse(value)
  } catch {}
  return null
}

const handleDataLoad = () => {
  const params = {
    wid: useWid(),
    path: route.query.path || '/'
  }
  progression.loading = true
  loadActivedPath.value = (route.query.path as string) || ''
  ioRequest('wiki.source', params).then((result) => {
    dataList.value = result[0]
    if (route.query.file) {
      nextTick(() => {
        handleDocumentClick(<string> route.query.file)
      })
    } else {
      handleAutoSelectFirstFile()
    }
  })
}
const handleSessionStateSave = () => {
  const state = {
    path: route.query.path || '/',
    file: route.query.file || ''
  }
  sessionStorage.setItem(STORAGE_KEY.value, JSON.stringify(state))
}
const handleAutoSelectFirstFile = () => {
  const list = dataList.value
  if (!list || list.length === 0) return
  const firstFile = list.find((item: any) => !item.isDirectory)
  if (firstFile) {
    // 有文件则选中第一个文件
    nextTick(() => {
      const index = list.indexOf(firstFile)
      loadActivedIndex.value = index
      const currentPath = route.query.path as string || '/'
      router.replace({
        path: route.path,
        query: {
          path: currentPath,
          file: firstFile.name
        }
      })
    })
  } else {
    // 只有目录则选中第一个目录
    nextTick(() => {
      loadActivedIndex.value = 0
    })
  }
}
const useParentPath = () => {
  if (!loadActivedPath.value) {
    return {
      root: true,
      path: ''
    }
  }
  const parts = loadActivedPath.value.split('/').filter(Boolean)
  if (parts.length <= 1) {
    return {
      root: true,
      path: loadActivedPath.value
    }
  }
  return {
    root: false,
    path: parts[parts.length - 1]
  }
}
const handleBack = () => {
  const parentPath = useDocumentParentPath(unref(loadActivedPath)) || '/'
  router.push({
    path: route.path,
    query: {
      path: parentPath
    }
  })
}
const handleClick = (data: any, index: number) => {
  loadActivedIndex.value = index
  if (!data.isDirectory) {
    const parentPath = useDocumentParentPath(data.path)
    router.push({
      path: route.path,
      query: {
        path: parentPath || '/',
        file: data.name,
        random: Math.random()
      }
    })
  } else {
    router.push({
      path: route.path,
      query: {
        path: route.query.path || '/'
      }
    })
  }
}
const handleDbClick = (data: any) => {
  if (data.isDirectory) {
    router.push({
      path: route.path,
      query: { path: data.path }
    })
  }
}
const handleReset = () => {
  loadActivedIndex.value = -1
}
const handleDocumentClick = (name: string) => {
  const index = dataList.value.findIndex((data: any) => data.name === name)
  loadActivedIndex.value = index
}

const handleContextView = (event: MouseEvent, data: any) => {
  isContextmenuView.value = true
  loadContextNode.value = data
  nextTick(() => {
    const rect = (<HTMLElement> event.currentTarget).getBoundingClientRect()
    const maxHeight = 200
    let posY = rect.top + 32
    let posX = rect.left - 20
    if ((posY + maxHeight) > document.body.clientHeight) {
      posY = document.body.clientHeight - maxHeight
    }
    loadContextmenuRef.value?.handlePositionUpdate(posX, posY)
  })
}
const handleContextAdd = (data: any, type: string) => {
  isContextmenuView.value = false
  const parentPath = data?.isDirectory ? data.path : (useDocumentParentPath(data?.path) || (route.query.path as string) || '/')
  const isMarkdown = type === 'markdown'
  const title = isMarkdown ? i18n.t('wiki.new-markdown') : i18n.t('wiki.new-folder')
  const defaultName = isMarkdown ? i18n.t('wiki.untitled-markdown') : i18n.t('wiki.untitled')
  ElMessageBox.prompt(
    '',
    title,
    {
      inputValue: defaultName,
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('common.tips'),
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(({ value }) => {
    const name = value.trim()
    handleXhrAddDocument(name, isMarkdown, parentPath)
  }).catch(() => {})
}
const handleXhrAddDocument = async (name: string, isMarkdown: boolean, parentPath: string) => {
  const command = isMarkdown ? 'wiki.create' : 'wiki.mkdir'
  const fileName = isMarkdown && !name.endsWith('.md') ? `${name}.md` : name
  const params = {
    wid: useWid(),
    path: parentPath,
    name: fileName
  }
  const result = await ioRequest(command, params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('wiki.add-document-failed'))
    return
  }
  ElMessage.success(i18n.t(isMarkdown ? 'wiki.new-markdown' : 'wiki.new-folder') + ' ✓')
  // 刷新列表
  router.replace({ query: { ...route.query, refresh: String(Date.now()) } })
}
const handleContextRename = (data: any) => {
  isContextmenuView.value = false
  if (!data) return
  loadEditNode.value = { path: data.path, name: data.name }
  nextTick(() => {
    const refs = loadRenameRef.value
    const dom = Array.isArray(refs) ? refs[0] : refs
    dom?.focus()
  })
}
const handleXhrEdit = async (data: any) => {
  const editNode = loadEditNode.value
  const newName = editNode.name?.trim()
  loadEditNode.value = {}
  if (!newName || newName === data.name) {
    return
  }
  const params = {
    wid: useWid(),
    path: data.path,
    name: newName
  }
  const result = await ioRequest('wiki.rename', params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('wiki.rename-failed'))
    return
  }
  ElMessage.success(i18n.t('extension.rename') + ' ✓')
  router.replace({ query: { ...route.query, refresh: String(Date.now()) } })
}
const handleContextMoveTo = (data: any) => {
  isContextmenuView.value = false
  if (!data) return
  loadMoveData.value = data
  isWindowView.value = true
}
const handleMoveToSubmit = async (targetPath: string) => {
  isWindowView.value = false
  const data = loadMoveData.value
  if (!data) return
  const params = {
    wid: useWid(),
    path: data.path,
    target: targetPath || '/'
  }
  const result = await ioRequest('wiki.move', params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('wiki.move-failed'))
    return
  }
  ElMessage.success(i18n.t('wiki.move-to') + ' ✓')
  // 如果移动的是当前打开的文档，清除 file 参数
  const query: any = { ...route.query, refresh: String(Date.now()) }
  if (route.query.file === data.name) {
    delete query.file
    delete query.random
  }
  router.replace({ query })
}
const handleContextDelete = (data: any) => {
  isContextmenuView.value = false
  if (!data) return
  ElMessageBox.confirm(
    `${i18n.t('common.delete')}: ${data.name}?`,
    i18n.t('common.tips'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(() => {
    handleXhrDelete(data)
  }).catch(() => {})
}
const handleXhrDelete = async (data: any) => {
  const params = {
    wid: useWid(),
    path: data.path
  }
  const result = await ioRequest('wiki.delete', params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('common.delete-failed'))
    return
  }
  ElMessage.success(i18n.t('common.delete') + ' ✓')
  // 如果删除的是当前打开的文档，清除 file 参数让 document.vue 重置
  const query: any = { ...route.query, refresh: String(Date.now()) }
  if (route.query.file === data.name) {
    delete query.file
    delete query.random
  }
  router.replace({ query })
}

onActivated(() => {
  if (route.name !== 'worker-wiki-mine') return
  const saved = loadSessionState()
  const queryPath = route.query.path as string
  if (saved && !queryPath) {
    // URL没有path参数时，从sessionStorage恢复之前的路径和文件
    const query: any = { path: saved.path }
    if (saved.file) query.file = saved.file
    router.replace({ path: route.path, query })
  } else if (!queryPath) {
    router.replace({ path: route.path, query: { path: '/' } })
  }
})

watch(() => route.query.path, (value, oldValue) => {
  if (value === oldValue) return
  if (route.name !== 'worker-wiki-mine') return
  handleSessionStateSave()
  handleReset()
  handleDataLoad()
}, { immediate: true })
// 监听文件选择变化，同步到会话存储
watch(() => route.query.file, () => {
  if (route.name !== 'worker-wiki-mine') return
  handleSessionStateSave()
})
// 监听参数变化，用于同目录下新建/删除/重命名后强制刷新列表
watch(() => route.query.refresh, (value, oldValue) => {
  if (!value) return
  if (value === oldValue) return
  if (route.name !== 'worker-wiki-mine') return
  handleReset()
  handleDataLoad()
})
</script>
