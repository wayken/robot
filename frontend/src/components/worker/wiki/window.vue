<template>
   <el-dialog draggable width="580px" append-to-body class="a-wiki-window" :title="title"
    :model-value="visible"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @open="handleOpen"
    @close="handleClose"
  >
    <div class="main inline-flex-c-n-n">
      <!-- 面包屑导航 -->
      <div class="breadcrumb inline-flex-r-c-n">
        <div class="menu inline-flex-r-c-n" v-for="(data, index) in loadPathList" :key="index"
          :class="{
            'is-active': index === loadPathList.length - 1 
          }"
          @click="handlePathNavigate(data.path)"
        >
          <div>{{ index === 0 ? $t('wiki.mine-wiki') : data.name }}</div>
          <el-icon class="icon" v-if="index < loadPathList.length - 1">
            <ArrowRight />
          </el-icon>
        </div>
      </div>
      <!-- 文件夹列表 -->
      <div class="content" v-loading="progression.loading">
        <div v-for="(data, index) in dataList" :key="index" class="module inline-flex-r-c-n"
          :class="{
            'is-selected': loadSelectedFolder === data.name
          }"
          @click="handleSelectFolder(data.name)"
          @dblclick="handleEnterFolder(data.name)"
        >
          <a-svg-icon icon-class="folder-user" size="22px" class="icon" />
          <div class="name">{{ data.name }}</div>
          <el-icon v-if="loadSelectedFolder === data.name" class="check">
            <Select />
          </el-icon>
          <div class="open-btn" @click.stop="handleEnterFolder(data.name)">
            {{ $t('common.open') }}
          </div>
        </div>
        <!-- 无子目录提示 -->
        <a-nodata v-if="dataList.length === 0" :loading="progression.loading" :success="true"
          :description="$t('project.no-subdirectories')"
        />
      </div>
      <!-- 已选路径 -->
      <div class="marked">
        <span class="label">{{ $t('project.selected-path') }}：</span>
        <span class="value">{{ loadSelectedPath || '/' }}</span>
      </div>
    </div>
    <template #footer>
      <span class="footer">
        <el-button @click="handleClose">
          {{ $t('common.cancel') }}
        </el-button>
        <el-button type="primary"
          @click="handleSubmit"
        >
          {{ $t('common.confirm') }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { Select, ArrowRight } from '@element-plus/icons-vue'
import useSocketIO from '@/hooks/useSocketIO'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  visible: {
    type: Boolean,
    default: false
  },
  wid: {
    type: String,
    required: true
  }
})

interface FolderItem {
  name: string
  path: string
  isDirectory: boolean
}

const { ioRequest, progression } = useSocketIO()
const handleEmit = defineEmits(['close', 'submit'])

const loadCurrentPath = ref('')
const loadSelectedFolder = ref('')
const dataList = ref<FolderItem[]>([])

// 面包屑路径列表
const loadPathList = computed(() => {
  const list: { name: string; path: string }[] = [{ name: 'root', path: '' }]
  if (loadCurrentPath.value) {
    const parts = loadCurrentPath.value.split(/[/\\]+/)
    let accumulated = ''
    for (const part of parts) {
      accumulated = accumulated ? `${accumulated}/${part}` : part
      list.push({ name: part, path: accumulated })
    }
  }
  return list
})
// 最终选中的完整路径
const loadSelectedPath = computed(() => {
  if (!loadSelectedFolder.value) {
    return loadCurrentPath.value || ''
  }
  if (loadCurrentPath.value) {
    return `${loadCurrentPath.value}/${loadSelectedFolder.value}`
  }
  return loadSelectedFolder.value
})
// 打开弹窗时加载根目录
const handleOpen = () => {
  loadCurrentPath.value = ''
  loadSelectedFolder.value = ''
  handleDataLoad()
}
// 加载指定路径下的文件夹列表
const handleDataLoad = () => {
  const params = {
    wid: props.wid,
    path: loadCurrentPath.value || '/'
  }
  ioRequest('wiki.source', params).then((result: any) => {
    const list = result[0] || []
    // 只显示文件夹
    dataList.value = list.filter((data: FolderItem) => data.isDirectory)
  })
}
// 单击选中文件夹
const handleSelectFolder = (name: string) => {
  loadSelectedFolder.value = loadSelectedFolder.value === name ? '' : name
}
// 双击进入文件夹
const handleEnterFolder = (name: string) => {
  loadCurrentPath.value = loadCurrentPath.value ? `${loadCurrentPath.value}/${name}` : name
  loadSelectedFolder.value = ''
  handleDataLoad()
}
// 面包屑导航点击
const handlePathNavigate = (path: string) => {
  if (path === loadCurrentPath.value) return
  loadCurrentPath.value = path
  loadSelectedFolder.value = ''
  handleDataLoad()
}
const handleClose = () => {
  handleEmit('close')
}
const handleSubmit = () => {
  handleEmit('submit', loadSelectedPath.value)
}
</script>
