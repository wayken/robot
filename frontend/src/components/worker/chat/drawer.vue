<template>
  <div class="drawer" :class="{
      'is-dragging': isDragging,
      'is-collapsed': isCollapsed
    }"
    :style="{
      width: loadMenuWidth + 'px'
    }"
  >
    <div class="wrapper inline-flex-c-n-n"
      :style="{
        width: loadMenuWidth + 'px'
      }"
    >
      <div class="head inline-flex-r-n-n">
        <div class="menu inline-flex-r-c-n">
          <a-svg-icon icon-class="chat-lines" size="18px" />
          <div class="name">{{ $t('menu.chat') }}</div>
        </div>
      </div>
      <div class="main">
        <el-scrollbar>
          <draggable class="mission-list" itemKey="key" :animation="200" v-model="loadSortedMissionList"
            handle=".mission-head"
            :filter="'.mission-default'"
            @end="handleMissionSortEnd"
          >
            <template #item="{ element: mission }">
              <div class="mission" :class="{ 'mission-default': mission.key === '0' }">
                <!-- 项目分组标题 -->
                <div class="mission-head inline-flex-r-c-n"
                  @click="handleMissionSwitch(mission.key)"
                >
                  <a-svg-icon v-if="loadCollapsedMissions[mission.key]" icon-class="folder-user" class="icon" />
                  <a-svg-icon v-else icon-class="folder-open" class="icon" />
                  <div class="name inline-text-ellipsis" :title="mission.name">
                    {{ mission.name }}
                  </div>
                  <div class="operate" v-if="mission.key !== '0'">
                    <el-icon @click.stop="handleMissionContextMenuView($event, mission)"
                      :class="{
                        'is-actived': isMissionContextMenuView && mission.key === loadActivedMissionData.key
                      }"
                    ><MoreFilled /></el-icon>
                  </div>
                </div>
                <!-- 项目下按日期分组的会话列表 -->
                <div class="mission-body" :class="{
                  'is-collapsed': loadCollapsedMissions[mission.key]
                }">
                  <div class="mission-body__wrapper">
                    <div class="root" v-for="(data, index) in mission.dateGroups" :key="index">
                    <div class="label">{{ $t(`chat.${data.label}`) }}</div>
                    <div class="source inline-flex-r-c-n" v-for="info in data.datas" :key="info.id"
                      :class="{
                        'is-actived': useSessionId() === String(info.id)
                      }"
                      @click="handleChatClick(info)"
                    >
                      <div class="icon" :class="{
                        'is-loading': loadStreamingSessions.includes(String(info.id))
                      }">
                        <el-icon v-if="!loadStreamingSessions.includes(String(info.id))">
                          <ChatDotSquare />
                        </el-icon>
                        <div class="spinner" v-else></div>
                      </div>
                      <div class="info inline-flex-c-n-n">
                        <div class="name inline-text-ellipsis" :title="info.name">{{ info.name }}</div>
                        <div class="meta inline-flex-r-c-n">
                          <span>{{ $t('chat.messages-size', { placeholder: info.size }) }}</span>
                          <span class="dot">-</span>
                          <span class="date">{{ dateftfn(info.date) }}</span>
                        </div>
                      </div>
                      <div class="operate inline-flex-r-c-n">
                        <el-icon @click.stop="handleContextMenuView($event, info)"
                          :class="{
                            'is-actived': isContextMenuView && info.id === loadActivedContextData.id
                          }"
                        ><MoreFilled /></el-icon>
                      </div>
                    </div>
                  </div>
                  </div>
                </div>
              </div>
            </template>
          </draggable>
          <a-nodata v-if="infomation.length === 0" :loading="progression.loading" :success="progression.success" />
        </el-scrollbar>
      </div>
      <div class="chat inline-flex-r-c-c" @click="handleSessionNew">
        <el-icon><ChatDotRound /></el-icon>
        {{ $t('chat.new-chat') }}
      </div>
    </div>
    <div class="flexible" @mousedown="handleResize"></div>
    <div class="caret" @click="handleCollapse">
      <el-icon v-if="!isCollapsed"><CaretLeft /></el-icon>
      <el-icon v-else><CaretRight /></el-icon>
    </div>
    <!-- 右键上下文 -->
    <transition name="el-zoom-in-top">
      <div class="contextmenu" ref="loadContextMenuRef" v-show="isContextMenuView"
        v-clickoutside="handleContextMenuHide"
      >
        <div class="arrow"></div>
        <div class="menu" @click="handleRenameSession">
          <el-icon><Edit /></el-icon>
          <div>{{ $t('common.edit') }}</div>
        </div>
        <div class="menu">
          <el-icon><DocumentCopy /></el-icon>
          <div>{{ $t('common.copy') }}</div>
        </div>
        <div class="menu" @click="handleRemoveMessage">
          <el-icon><ChatLineSquare /></el-icon>
          <div>{{ $t('chat.clear-message-history') }}</div>
        </div>
        <div class="divider"></div>
        <div class="menu" @click="handleDeleteSession">
          <el-icon><Delete /></el-icon>
          <div>{{ $t('common.delete') }}</div>
        </div>
      </div>
    </transition>
    <!-- 项目右键上下文 -->
    <transition name="el-zoom-in-top">
      <div class="contextmenu" ref="loadMissionContextMenuRef" v-show="isMissionContextMenuView"
        v-clickoutside="handleMissionContextMenuHide"
      >
        <div class="arrow"></div>
        <div class="menu" @click="handleRenameMission">
          <el-icon><Edit /></el-icon>
          <div>{{ $t('common.edit') }}</div>
        </div>
        <div class="divider"></div>
        <div class="menu" @click="handleDeleteMission">
          <el-icon><Delete /></el-icon>
          <div>{{ $t('common.delete') }}</div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'
import {
  Edit,
  Delete,
  CaretLeft,
  CaretRight,
  MoreFilled,
  ChatDotRound,
  DocumentCopy,
  ChatDotSquare,
  ChatLineSquare
} from '@element-plus/icons-vue'
import Mitter from '@/utils/mitt'
import Draggable from 'vuedraggable'
import useSocketIO from '@/hooks/useSocketIO'
import { dateftfn } from '@/hooks/useDataFormatter'
import { useDraggingResize } from '@/hooks/useDraggingResize'

const {
  ioOn,
  ioRequest,
  progression
} = useSocketIO()
const {
  width: loadMenuWidth,
  isDragging,
  handleResize
} = useDraggingResize({ initialWidth: 282, min: 282, max: 480 })

const i18n = useI18n()
const route = useRoute()
const router = useRouter()

const infomation = ref<any[]>([])
const loadStreamingSessions = ref<string[]>([])
const isCollapsed = ref(false)
const isContextMenuView = ref(false)
const isMissionContextMenuView = ref(false)
const loadActivedContextData = ref<any>({})
const loadActivedMissionData = ref<any>({})
const loadCollapsedMissions = ref<Record<string, boolean>>({})
const useWid = () => route.params.id as string
const useSessionId = () => route.query.id as string
const loadContextMenuRef = ref<HTMLDivElement | null>(null)
const loadMissionContextMenuRef = ref<HTMLDivElement | null>(null)

// 按日期将会话分组的工具函数
const groupByDate = (sessions: any[]) => {
  const now = new Date()
  const dateToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const dateYesterday = dateToday - 86400000
  const dateLast7 = dateToday - 7 * 86400000
  const formatDataList: { label: string; datas: any[] }[] = [
    { label: 'date-today', datas: [] },
    { label: 'date-yesterday', datas: [] },
    { label: 'date-last7days', datas: [] },
    { label: 'date-earlier', datas: [] }
  ]
  for (const data of sessions) {
    const loadTimeStamp = data.date ? new Date(data.date).getTime() : 0
    if (loadTimeStamp >= dateToday) {
      formatDataList[0].datas.push(data)
    } else if (loadTimeStamp >= dateYesterday) {
      formatDataList[1].datas.push(data)
    } else if (loadTimeStamp >= dateLast7) {
      formatDataList[2].datas.push(data)
    } else {
      formatDataList[3].datas.push(data)
    }
  }
  return formatDataList.filter(formatData => formatData.datas.length > 0)
}

// 按项目分组 + 日期范围子分组的会话列表
const loadFormatDataList = computed(() => {
  const projectMap = new Map<string, { name: string; key: string; sortOrder: number; sessions: any[] }>()
  for (const data of infomation.value) {
    const projectId = data.projectId || 0
    const projectName = data.projectName || i18n.t('project.default')
    const sortOrder = data.projectSortOrder || 0
    const key = String(projectId)
    if (!projectMap.has(key)) {
      projectMap.set(key, { name: projectName, key, sortOrder, sessions: [] })
    }
    projectMap.get(key)!.sessions.push(data)
  }
  // 转换为数组，默认项目（id=0）放最后
  const result: { name: string; key: string; total: number; dateGroups: { label: string; datas: any[] }[] }[] = []
  const defaultProject = projectMap.get('0')
  projectMap.delete('0')
  // 按 sort_order 排序项目
  const sortedProjects = Array.from(projectMap.values()).sort((a, b) => a.sortOrder - b.sortOrder)
  for (const project of sortedProjects) {
    result.push({
      name: project.name,
      key: project.key,
      total: project.sessions.length,
      dateGroups: groupByDate(project.sessions)
    })
  }
  if (defaultProject) {
    result.push({
      name: defaultProject.name,
      key: defaultProject.key,
      total: defaultProject.sessions.length,
      dateGroups: groupByDate(defaultProject.sessions)
    })
  }
  return result
})

// 可排序的项目列表（供 draggable 使用）
const loadSortedMissionList = ref<any[]>([])
watch(loadFormatDataList, (val) => {
  loadSortedMissionList.value = [...val]
}, { immediate: true })

// 拖拽排序结束后保存排序
const handleMissionSortEnd = () => {
  // 收集排序后的项目ID列表（排除默认项目 key=0）
  const projectIds = loadSortedMissionList.value
    .filter(m => m.key !== '0')
    .map(m => parseInt(m.key))
  if (projectIds.length === 0) return
  const params = {
    wid: useWid(),
    projectIds
  }
  ioRequest('project.sort', params)
}

// 折叠/展开项目
const handleMissionSwitch = (key: string) => {
  loadCollapsedMissions.value[key] = !loadCollapsedMissions.value[key]
}

onMounted(() => {
  handleDataLoad()
  handleDataListenOn()
})
onUnmounted(() => {
  Mitter.off('mitt-chat-streaming', handleStreamingUpdate)
})

const handleDataLoad = () => {
  const params = {
    wid: useWid()
  }
  ioRequest('sessions.index', params).then((result) => {
    infomation.value = result[0] || []
  })
}
const handleDataListenOn = () => {
  ioOn('message.index.broadcast', (result) => {
    const wid = result[0]
    if (wid !== useWid()) {
      return
    }
    infomation.value = result[1]
  })
  Mitter.on('mitt-chat-streaming', handleStreamingUpdate)
}
const handleStreamingUpdate = (payload: any) => {
  const sid = String(payload.sid)
  if (payload.streaming) {
    if (!loadStreamingSessions.value.includes(sid)) {
      loadStreamingSessions.value.push(sid)
    }
  } else {
    const index = loadStreamingSessions.value.indexOf(sid)
    if (index !== -1) {
      loadStreamingSessions.value.splice(index, 1)
    }
  }
}
const handleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
const handleSessionNew = () => {
  router.replace({
    path: route.path,
    query: {}
  })
}
const handleChatClick = (data: any) => {
  router.replace({
    path: route.path,
    query: { id: data.id }
  })
}
// 右键上下文
const handleContextMenuView = (event: MouseEvent, data: any) => {
  loadActivedContextData.value = data
  isContextMenuView.value = true
  nextTick(() => {
    const rect = (<HTMLElement> event.currentTarget).getBoundingClientRect()
    let posY = rect.top + 32
    let posX = rect.left - 20
    const maxHeight = 200
    if ((posY + maxHeight) > document.body.clientHeight) {
      posY = document.body.clientHeight - maxHeight
    }
    const contextmenu = loadContextMenuRef.value as HTMLDivElement
    contextmenu.style.left = `${posX}px`
    contextmenu.style.top = `${posY}px`
  })
}
const handleContextMenuHide = () => {
  isContextMenuView.value = false
}
// 编辑会话名称
const handleRenameSession = () => {
  const data = loadActivedContextData.value
  if (!data || !data.id) return
  handleContextMenuHide()
  ElMessageBox.prompt(
    i18n.t('chat.rename-session-prompt'),
    i18n.t('chat.rename-session-title'),
    {
      inputValue: data.name,
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('chat.rename-session-empty'),
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(({ value }) => {
    const newName = value.trim()
    if (newName === data.name) return
    handleXhrSessionRename(data, newName)
  }).catch(() => {
  })
}
const handleXhrSessionRename = (data: any, newName: string) => {
  const params = {
    wid: useWid(),
    sid: data.id,
    name: newName
  }
  ioRequest('sessions.rename', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('chat.rename-session-failed'))
    }
    ElMessage.success(i18n.t('chat.rename-session-success'))
    // 更新本地列表中的名称
    const matched = infomation.value.find((info: any) => info.id === data.id)
    if (matched) {
      matched.name = newName
    }
  })
}
// 清空聊天记录
const handleRemoveMessage = () => {
  const data = loadActivedContextData.value
  if (!data || !data.id) return
  handleContextMenuHide()
  ElMessageBox.confirm(
    i18n.t('chat.clear-message-confirm', { placeholder: data.name }),
    i18n.t('chat.clear-message-title'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(() => {
    handleXhrMessageRemove(data)
  }).catch(() => {
  })
}
const handleXhrMessageRemove = (data: any) => {
  const params = {
    wid: useWid(),
    sid: data.id
  }
  ioRequest('message.clear', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('chat.clear-message-failed'))
    }
    ElMessage.success(i18n.t('chat.clear-message-success'))
    // 更新会话列表中的消息数量
    const matched = infomation.value.find((info: any) => info.id === data.id)
    if (matched) {
      matched.size = 0
    }
    // 通知消息列表组件清空当前显示的消息
    Mitter.emit('mitt-chat-messages-remove', data.id)
  })
}
// 删除会话
const handleDeleteSession = () => {
  const data = loadActivedContextData.value
  if (!data || !data.id) return
  handleContextMenuHide()
  ElMessageBox.confirm(
    i18n.t('chat.delete-session-confirm', { placeholder: data.name }),
    i18n.t('chat.delete-session-title'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(() => {
    handleXhrSessionDelete(data)
  }).catch(() => {
  })
}
const handleXhrSessionDelete = (data: any) => {
  const params = {
    wid: useWid(),
    sid: data.id
  }
  ioRequest('sessions.remove', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('chat.delete-session-failed'))
    }
    ElMessage.success(i18n.t('chat.delete-session-success'))
    const index = infomation.value.findIndex((info: any) => info.id === data.id)
    if (index !== -1) {
      infomation.value.splice(index, 1)
    }
    // 如果当前正在查看被删除的会话，则跳转到空白状态
    if (String(route.query.id) === String(data.id)) {
      router.replace({
        path: route.path,
        query: {}
      })
    }
  })
}
// ==================== 项目右键操作 ====================
const handleMissionContextMenuView = (event: MouseEvent, mission: any) => {
  loadActivedMissionData.value = mission
  isMissionContextMenuView.value = true
  nextTick(() => {
    const rect = (<HTMLElement> event.currentTarget).getBoundingClientRect()
    let posY = rect.top + 32
    let posX = rect.left - 20
    const maxHeight = 120
    if ((posY + maxHeight) > document.body.clientHeight) {
      posY = document.body.clientHeight - maxHeight
    }
    const contextmenu = loadMissionContextMenuRef.value as HTMLDivElement
    contextmenu.style.left = `${posX}px`
    contextmenu.style.top = `${posY}px`
  })
}
const handleMissionContextMenuHide = () => {
  isMissionContextMenuView.value = false
}
// 重命名项目
const handleRenameMission = () => {
  const mission = loadActivedMissionData.value
  if (!mission || mission.key === '0') return
  handleMissionContextMenuHide()
  ElMessageBox.prompt(
    i18n.t('project.rename-placeholder'),
    i18n.t('project.rename-title'),
    {
      inputValue: mission.name,
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('project.create-empty'),
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(({ value }) => {
    const newName = value.trim()
    if (newName === mission.name) return
    handleXhrMissionRename(mission, newName)
  }).catch(() => {
  })
}
const handleXhrMissionRename = (mission: any, newName: string) => {
  const params = {
    wid: useWid(),
    projectId: mission.key,
    name: newName
  }
  ioRequest('project.rename', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('project.rename-failed'))
      return
    }
    ElMessage.success(i18n.t('project.rename-success'))
    // 更新本地列表中会话的项目名称
    for (const info of infomation.value) {
      if (String(info.projectId) === mission.key) {
        info.projectName = newName
      }
    }
  })
}
// 删除项目
const handleDeleteMission = () => {
  const mission = loadActivedMissionData.value
  if (!mission || mission.key === '0') return
  handleMissionContextMenuHide()
  ElMessageBox.confirm(
    i18n.t('project.delete-confirm', { placeholder: mission.name }),
    i18n.t('project.delete-title'),
    {
      type: 'warning',
      cancelButtonText: i18n.t('common.cancel'),
      confirmButtonText: i18n.t('common.confirm')
    }
  ).then(() => {
    handleXhrMissionDelete(mission)
  }).catch(() => {
  })
}
const handleXhrMissionDelete = (mission: any) => {
  const params = {
    wid: useWid(),
    projectId: mission.key
  }
  ioRequest('project.remove', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('project.delete-failed'))
      return
    }
    ElMessage.success(i18n.t('project.delete-success'))
    // 将该项目下的会话归入默认项目
    for (const info of infomation.value) {
      if (String(info.projectId) === mission.key) {
        info.projectId = 0
        info.projectName = null
        info.projectSortOrder = 0
      }
    }
  })
}
</script>
