<template>
  <div class="project-panel" ref="loadPanelRef">
    <div class="project-btn inline-flex-r-c-n" @click="handleWindowView">
      <a-svg-icon icon-class="folder-user" size="18px" />
      <div class="label">{{ loadCurrentProjectName }}</div>
      <el-icon class="arrow"><ArrowDown /></el-icon>
    </div>
    <!-- 项目下拉面板 -->
    <transition name="project-panel-fade">
      <div v-if="isDropdownVisible" class="project-panel">
        <div class="project-panel__body">
          <!-- 默认项目 -->
          <div class="project-panel__info"
            :class="{
              'is-selected': loadCurrentProjectId === 0 
            }"
            @click="handleProjectSwitch(0, $t('project.default'))"
          >
            <a-svg-icon icon-class="folder-user" size="18px" class="project-panel__icon" />
            <div class="project-panel__name">{{ $t('project.default') }}</div>
            <el-icon v-if="loadCurrentProjectId === 0" class="project-panel__check">
              <Select />
            </el-icon>
          </div>
          <!-- 项目列表 -->
          <div v-for="project in dataList" :key="project.id" class="project-panel__info"
            :class="{
              'is-selected': loadCurrentProjectId === project.id
            }"
            @click="handleProjectSwitch(project.id, project.name)"
          >
            <a-svg-icon icon-class="folder-user" size="18px" class="project-panel__icon" />
            <div class="project-panel__name">{{ project.name }}</div>
            <el-icon v-if="loadCurrentProjectId === project.id" class="project-panel__check">
              <Select />
            </el-icon>
          </div>
        </div>
        <!-- 分割线 -->
        <div class="project-panel__divider"></div>
        <!-- 创建空白项目 -->
        <div class="project-panel__info project-panel__add"
          @click="handleAddProject"
        >
          <el-icon class="project-panel__icon"><Plus /></el-icon>
          <div class="project-panel__name">{{ $t('project.create') }}</div>
        </div>
        <!-- 选择现有项目 -->
        <div class="project-panel__info project-panel__add"
          @click="isDiskWindowView = true"
        >
          <el-icon class="project-panel__icon"><FolderOpened /></el-icon>
          <div class="project-panel__name">{{ $t('project.select-existing') }}</div>
        </div>
      </div>
    </transition>
    <a-disk-window :visible="isDiskWindowView"
      :wid="useWid()"
      :title="$t('project.select-existing-title')"
      @close="isDiskWindowView = false"
      @submit="handleSelectExistingProject"
    />
  </div>
</template>

<script setup lang="ts">
import {
  Plus,
  Select,
  ArrowDown,
  FolderOpened
} from '@element-plus/icons-vue'
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import useSocketIO from '@/hooks/useSocketIO'
import ADiskWindow from '@/components/worker/disk/mine/window.vue'

interface ProjectItem {
  id: number
  name: string
  date: number
  description: string
}

const props = defineProps<{
  sessionId: string | undefined
}>()

const handleEmit = defineEmits<{
  (e: 'change', projectId: number, projectName: string): void
}>()

const i18n = useI18n()
const route = useRoute()
const { ioRequest } = useSocketIO()

const isDropdownVisible = ref(false)
const dataList = ref<ProjectItem[]>([])
const loadCurrentProjectId = ref(0)
const isDiskWindowView = ref(false)
const loadPanelRef = ref<HTMLElement | null>(null)
const useWid = () => route.params.id as string

const loadCurrentProjectName = computed(() => {
  if (loadCurrentProjectId.value === 0) {
    return i18n.t('project.default')
  }
  const project = dataList.value.find(p => p.id === loadCurrentProjectId.value)
  return project ? project.name : i18n.t('project.default')
})

const handleDataLoad = () => {
  const params = {
    wid: useWid()
  }
  ioRequest('project.index', params).then((result: any) => {
    dataList.value = result[0] || []
  })
}
const handleWindowView = () => {
  isDropdownVisible.value = !isDropdownVisible.value
  if (isDropdownVisible.value) {
    handleDataLoad()
  }
}
const handleProjectSwitch = (projectId: number, projectName: string) => {
  if (loadCurrentProjectId.value === projectId) {
    isDropdownVisible.value = false
    return
  }
  const sid = props.sessionId
  if (!sid) {
    // 无会话时仅更新本地状态
    loadCurrentProjectId.value = projectId
    isDropdownVisible.value = false
    handleEmit('change', projectId, projectName)
    return
  }
  const params = {
    wid: useWid(),
    sid: sid,
    projectId: projectId
  }
  ioRequest('sessions.project.update', params).then((result: any) => {
    const success = result[0]
    if (success) {
      loadCurrentProjectId.value = projectId
      handleEmit('change', projectId, projectName)
    } else {
      ElMessage.error(i18n.t('project.switch-failed'))
    }
  }).finally(() => {
    isDropdownVisible.value = false
  })
}
const handleAddProject = () => {
  isDropdownVisible.value = false
  ElMessageBox.prompt(
    i18n.t('project.create-placeholder'),
    i18n.t('project.create-title'),
    {
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('project.create-empty'),
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(({ value }) => {
    const name = value.trim()
    handleXhrAddProject(name)
  }).catch(() => {})
}
const handleXhrAddProject = async (name: string) => {
  const wid = useWid()
  // 先在工作空间目录创建项目目录
  let params: any = {
    wid: wid,
    path: '',
    name: name
  }
  let result = await ioRequest('disk.mkdir', params)
  const success = Array.isArray(result) ? result[0] : result
  if (!success) {
    ElMessage.error(i18n.t('project.create-dir-failed'))
    return
  }
  // 目录创建成功，再创建项目
  params = {
    wid: useWid(),
    name: name,
    description: '',
    path: name
  }
  result = await ioRequest('project.add', params)
  const newProjectId = result[0]
  if (!newProjectId) {
    ElMessage.error(i18n.t('project.create-failed'))
    return
  }
  ElMessage.success(i18n.t('project.create-success'))
  const projectId = parseInt(newProjectId)
  // 自动将当前会话切换到新创建的项目
  const sid = props.sessionId
  if (sid) {
    params = {
      wid: useWid(),
      sid: sid,
      projectId: projectId
    }
    result = await ioRequest('sessions.project.update', params)
    if (!result[0]) {
      ElMessage.error(i18n.t('project.create-failed'))
      return
    }
  }
  loadCurrentProjectId.value = projectId
  dataList.value.unshift({
    id: projectId,
    name: name,
    description: '',
    date: Date.now()
  })
  handleEmit('change', projectId, name)
}
// 选择现有目录创建项目
const handleSelectExistingProject = async (path: string) => {
  isDiskWindowView.value = false
  // 从路径中提取最后一级目录名作为项目名
  const parts = path.split(/[/\\]+/)
  const name = parts[parts.length - 1]
  const wid = useWid()
  // 创建项目记录，关联该目录路径
  const params: any = {
    wid: wid,
    name: name,
    description: '',
    path: path
  }
  const result = await ioRequest('project.add', params)
  const newProjectId = result[0]
  if (!newProjectId) {
    ElMessage.error(i18n.t('project.create-failed'))
    return
  }
  ElMessage.success(i18n.t('project.create-success'))
  const projectId = parseInt(newProjectId)
  // 自动将当前会话切换到新创建的项目
  const sid = props.sessionId
  if (sid) {
    const switchParams = {
      wid: wid,
      sid: sid,
      projectId: projectId
    }
    const switchResult = await ioRequest('sessions.project.update', switchParams)
    if (!switchResult[0]) {
      ElMessage.error(i18n.t('project.switch-failed'))
      return
    }
  }
  loadCurrentProjectId.value = projectId
  dataList.value.unshift({
    id: projectId,
    name: name,
    description: '',
    date: Date.now()
  })
  handleEmit('change', projectId, name)
}
// 加载当前会话的项目ID
const handleSessionProjectLoad = (sid: string) => {
  if (!sid) {
    loadCurrentProjectId.value = 0
    return
  }
  const params = {
    wid: useWid(),
    sid: sid
  }
  ioRequest('sessions.project.infomation', params).then((result: any) => {
    const project = result[0]
    loadCurrentProjectId.value = project ? (project.id || 0) : 0
  })
}
// 点击面板外区域自动关闭
const handleClickOutside = (event: MouseEvent) => {
  if (!isDropdownVisible.value) return
  const el = loadPanelRef.value
  const dom = event.target as HTMLElement
  if (el && el.contains(dom)) return
  isDropdownVisible.value = false
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside, true)
  if (props.sessionId) {
    handleDataLoad()
    handleSessionProjectLoad(props.sessionId)
  }
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside, true)
})

watch(() => props.sessionId, (newId) => {
  if (newId) {
    handleSessionProjectLoad(newId)
  } else {
    loadCurrentProjectId.value = 0
  }
})
</script>
