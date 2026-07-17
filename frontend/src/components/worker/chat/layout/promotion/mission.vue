<template>
  <div class="mission-panel" ref="loadPanelRef">
    <div class="mission-btn inline-flex-r-c-n" @click="handleWindowView">
      <a-svg-icon icon-class="folder-user" size="18px" />
      <div class="label">{{ loadCurrentMissionName }}</div>
      <el-icon class="arrow"><ArrowDown /></el-icon>
    </div>
    <!-- 任务分组下拉面板 -->
    <transition name="mission-panel-fade">
      <div v-if="isDropdownVisible" class="mission-panel">
        <div class="mission-panel__body">
          <!-- 默认任务分组 -->
          <div class="mission-panel__info"
            :class="{
              'is-selected': loadCurrentMissionId === 0 
            }"
            @click="handleMissionSwitch(0, $t('mission.default'))"
          >
            <a-svg-icon icon-class="folder-user" size="18px" class="mission-panel__icon" />
            <div class="mission-panel__name">{{ $t('mission.default') }}</div>
            <el-icon v-if="loadCurrentMissionId === 0" class="mission-panel__check">
              <Select />
            </el-icon>
          </div>
          <!-- 任务分组列表 -->
          <div v-for="mission in dataList" :key="mission.id" class="mission-panel__info"
            :class="{
              'is-selected': loadCurrentMissionId === mission.id
            }"
            @click="handleMissionSwitch(mission.id, mission.name)"
          >
            <a-svg-icon icon-class="folder-user" size="18px" class="mission-panel__icon" />
            <div class="mission-panel__name">{{ mission.name }}</div>
            <el-icon v-if="loadCurrentMissionId === mission.id" class="mission-panel__check">
              <Select />
            </el-icon>
          </div>
        </div>
        <!-- 分割线 -->
        <div class="mission-panel__divider"></div>
        <!-- 创建空白任务分组 -->
        <div class="mission-panel__info mission-panel__add"
          @click="handleAddMission"
        >
          <el-icon class="mission-panel__icon"><Plus /></el-icon>
          <div class="mission-panel__name">{{ $t('mission.create') }}</div>
        </div>
        <!-- 选择现有任务分组 -->
        <div class="mission-panel__info mission-panel__add"
          @click="isDiskWindowView = true"
        >
          <el-icon class="mission-panel__icon"><FolderOpened /></el-icon>
          <div class="mission-panel__name">{{ $t('mission.select-existing') }}</div>
        </div>
      </div>
    </transition>
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
import Mitter from '@/utils/mitt'

interface MissionItem {
  id: number
  name: string
  date: number
  description: string
}

const props = defineProps<{
  sessionId: string | undefined
}>()

const handleEmit = defineEmits<{
  (e: 'update', missionId: number, missionName: string): void
}>()

const i18n = useI18n()
const route = useRoute()
const { ioRequest } = useSocketIO()

const isDropdownVisible = ref(false)
const dataList = ref<MissionItem[]>([])
const loadCurrentMissionId = ref(0)
const skipMissionReset = ref(false)
const isDiskWindowView = ref(false)
const loadPanelRef = ref<HTMLElement | null>(null)
const useWid = () => route.params.id as string

const loadCurrentMissionName = computed(() => {
  if (loadCurrentMissionId.value === 0) {
    return i18n.t('mission.default')
  }
  const mission = dataList.value.find(p => p.id === loadCurrentMissionId.value)
  return mission ? mission.name : i18n.t('mission.default')
})

const handleDataLoad = () => {
  const params = {
    wid: useWid()
  }
  ioRequest('mission.index', params).then((result: any) => {
    dataList.value = result[0] || []
  })
}
const handleWindowView = () => {
  isDropdownVisible.value = !isDropdownVisible.value
  if (isDropdownVisible.value) {
    handleDataLoad()
  }
}
const handleMissionSwitch = (missionId: number, missionName: string) => {
  if (loadCurrentMissionId.value === missionId) {
    isDropdownVisible.value = false
    return
  }
  const sid = props.sessionId
  if (!sid) {
    // 无会话时仅更新本地状态
    loadCurrentMissionId.value = missionId
    isDropdownVisible.value = false
    handleEmit('update', missionId, missionName)
    return
  }
  const params = {
    wid: useWid(),
    sid: sid,
    missionId: missionId
  }
  ioRequest('sessions.mission.update', params).then((result: any) => {
    const success = result[0]
    if (success) {
      loadCurrentMissionId.value = missionId
      handleEmit('update', missionId, missionName)
      Mitter.emit('mitt-session-mission-update', { sid, missionId, missionName })
    } else {
      ElMessage.error(i18n.t('mission.switch-failed'))
    }
  }).finally(() => {
    isDropdownVisible.value = false
  })
}
const handleAddMission = () => {
  isDropdownVisible.value = false
  ElMessageBox.prompt(
    i18n.t('mission.create-placeholder'),
    i18n.t('mission.create-title'),
    {
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('mission.create-empty'),
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(({ value }) => {
    const name = value.trim()
    handleXhrAddMission(name)
  }).catch(() => {})
}
const handleXhrAddMission = async (name: string) => {
  // 创建任务分组
  let params: any = {
    wid: useWid(),
    name: name,
    description: ''
  }
  let result = await ioRequest('mission.add', params)
  const newMissionId = result[0]
  if (!newMissionId) {
    ElMessage.error(i18n.t('mission.create-failed'))
    return
  }
  ElMessage.success(i18n.t('mission.create-success'))
  const missionId = parseInt(newMissionId)
  // 自动将当前会话切换到新创建的任务分组
  const sessionId = props.sessionId
  if (sessionId) {
    params = {
      wid: useWid(),
      sid: sessionId,
      missionId: missionId
    }
    result = await ioRequest('sessions.mission.update', params)
    if (!result[0]) {
      ElMessage.error(i18n.t('mission.create-failed'))
      return
    }
  }
  loadCurrentMissionId.value = missionId
  dataList.value.unshift({
    id: missionId,
    name: name,
    description: '',
    date: Date.now()
  })
  handleEmit('update', missionId, name)
  // 通知会话组件更新分组
  if (props.sessionId) {
    Mitter.emit('mitt-session-mission-update', { sid: props.sessionId, missionId, missionName: name })
  }
}
// 加载当前会话的任务分组ID
const handleSessionMissionLoad = (sid: string) => {
  if (!sid) {
    loadCurrentMissionId.value = 0
    return
  }
  const params = {
    wid: useWid(),
    sid: sid
  }
  ioRequest('sessions.mission.infomation', params).then((result: any) => {
    const mission = result[0]
    loadCurrentMissionId.value = mission ? (mission.id || 0) : 0
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
// 拖拽更新分组后同步输入框的分组按钮
const handleDrawerMissionUpdate = (payload: any) => {
  const { sid, missionId } = payload
  if (String(sid) === String(props.sessionId)) {
    loadCurrentMissionId.value = missionId
  }
}
// 在指定分组下新建会话时，预设分组
const handleNewSessionMission = (payload: any) => {
  const { missionId, missionName } = payload
  const found = dataList.value.find(p => p.id === missionId)
  if (!found) {
    dataList.value.unshift({
      id: missionId,
      name: missionName,
      description: '',
      date: Date.now()
    })
  }
  skipMissionReset.value = true
  loadCurrentMissionId.value = missionId
  handleEmit('update', missionId, missionName)
}

// drawer 重命名分组后同步更新本地 dataList
const handleMissionRename = (payload: any) => {
  const { missionId, missionName } = payload
  const mission = dataList.value.find(p => p.id === missionId)
  if (mission) {
    mission.name = missionName
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside, true)
  handleDataLoad()
  if (props.sessionId) {
    handleSessionMissionLoad(props.sessionId)
  }
  Mitter.on('mitt-drawer-mission-update', handleDrawerMissionUpdate)
  Mitter.on('mitt-new-session-mission', handleNewSessionMission)
  Mitter.on('mitt-mission-rename', handleMissionRename)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside, true)
  Mitter.off('mitt-drawer-mission-update', handleDrawerMissionUpdate)
  Mitter.off('mitt-new-session-mission', handleNewSessionMission)
  Mitter.off('mitt-mission-rename', handleMissionRename)
})

watch(() => props.sessionId, (newId) => {
  if (newId) {
    handleSessionMissionLoad(newId)
  } else {
    if (skipMissionReset.value) {
      skipMissionReset.value = false
      return
    }
    loadCurrentMissionId.value = 0
  }
})
</script>
