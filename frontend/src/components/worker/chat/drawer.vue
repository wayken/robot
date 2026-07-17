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
        <div class="operate inline-flex-r-c-n">
          <el-tooltip :content="$t('mission.create')" placement="bottom">
            <el-icon class="icon" @click="handleAddMission">
              <Folder />
            </el-icon>
          </el-tooltip>
          <el-tooltip :content="$t('chat.new-chat')" placement="bottom">
            <el-icon class="icon" @click="handleNewSession">
              <Plus />
            </el-icon>
          </el-tooltip>
        </div>
      </div>
      <div class="main">
        <el-scrollbar>
          <draggable class="mission-list" itemKey="key" :animation="200" v-model="loadSortedMissionList"
            handle=".mission-head"
            :move="handleMissionMoveCheck"
            @end="handleMissionSortEnd"
          >
            <template #item="{ element: mission }">
              <div class="mission" :class="{ 'mission-default': mission.key === '0' }"
                @dragover.prevent="handleSessionDragOver($event, mission.key)"
                @dragleave="handleSessionDragLeave($event, mission.key)"
                @drop="handleSessionDrop($event, mission.key)"
              >
                <!-- 项目分组标题 -->
                <div class="mission-head inline-flex-r-c-n"
                  :class="{ 'is-drop-target': sessionDragOverMissionKey === mission.key }"
                  @click="handleMissionSwitch(mission.key)"
                >
                  <a-svg-icon v-if="loadCollapsedMissions[mission.key]" icon-class="folder-user" class="icon" />
                  <a-svg-icon v-else icon-class="folder-open" class="icon" />
                  <div class="name inline-text-ellipsis" :title="mission.name">
                    {{ mission.name }}
                  </div>
                  <div class="operate inline-flex-r-c-n" v-if="mission.key !== '0'">
                    <el-tooltip :content="$t('chat.new-chat-in-group')" placement="top">
                      <el-icon @click.stop="handleNewSessionInMission(mission)"><Edit /></el-icon>
                    </el-tooltip>
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
                      draggable="true"
                      @dragstart="handleSessionDragStart($event, info)"
                      @dragend="handleSessionDragEnd"
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
          <a-nodata v-if="infomation.length === 0 && missionList.length === 0" :loading="progression.loading" :success="progression.success" />
        </el-scrollbar>
      </div>
      <div class="chat inline-flex-r-c-c" @click="handleNewSession">
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
  Plus,
  Folder,
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
const missionList = ref<any[]>([])
const loadStreamingSessions = ref<string[]>([])
const isCollapsed = ref(false)
const isContextMenuView = ref(false)
const isMissionContextMenuView = ref(false)
const loadActivedContextData = ref<any>({})
const loadActivedMissionData = ref<any>({})
const loadCollapsedMissions = ref<Record<string, boolean>>({})
const sessionDragOverMissionKey = ref<string | null>(null)
const sessionDragData = ref<any>(null)
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
  const missionMap = new Map<string, { name: string; key: string; sortOrder: number; sessions: any[] }>()
  // 先从独立加载的 mission 列表初始化所有分组（确保即使无会话也展示）
  for (const mission of missionList.value) {
    const key = String(mission.id)
    missionMap.set(key, {
      name: mission.name,
      key,
      sortOrder: mission.sortOrder || 0,
      sessions: []
    })
  }
  // 再将会话分配到对应分组
  for (const data of infomation.value) {
    const missionId = data.missionId || 0
    const missionName = data.missionName || i18n.t('mission.default')
    const sortOrder = data.missionSortOrder || 0
    const key = String(missionId)
    if (!missionMap.has(key)) {
      missionMap.set(key, { name: missionName, key, sortOrder, sessions: [] })
    }
    missionMap.get(key)!.sessions.push(data)
  }
  // 转换为数组，默认项目（id=0）放最后
  const result: { name: string; key: string; total: number; dateGroups: { label: string; datas: any[] }[] }[] = []
  const defaultMission = missionMap.get('0')
  missionMap.delete('0')
  // 按 sort_order 排序项目
  const sortedMissions = Array.from(missionMap.values()).sort((a, b) => a.sortOrder - b.sortOrder)
  for (const mission of sortedMissions) {
    result.push({
      name: mission.name,
      key: mission.key,
      total: mission.sessions.length,
      dateGroups: groupByDate(mission.sessions)
    })
  }
  if (defaultMission) {
    result.push({
      name: defaultMission.name,
      key: defaultMission.key,
      total: defaultMission.sessions.length,
      dateGroups: groupByDate(defaultMission.sessions)
    })
  }
  return result
})

// 可排序的项目列表（供 draggable 使用）
const loadSortedMissionList = ref<any[]>([])
watch(loadFormatDataList, (val) => {
  loadSortedMissionList.value = [...val]
  // 没有会话的分组默认折叠（仅初始化时自动设置，不覆盖用户手动切换的状态）
  for (const mission of val) {
    if (mission.total === 0 && !(mission.key in loadCollapsedMissions.value)) {
      loadCollapsedMissions.value[mission.key] = true
    }
  }
}, { immediate: true })

// 阻止默认分组被拖动排序（返回 false 取消排序移动）
const handleMissionMoveCheck = (evt: any) => {
  const draggedElement = evt.draggedContext?.element
  if (draggedElement && draggedElement.key === '0') {
    return false
  }
  return true
}

// 拖拽排序结束后保存排序
const handleMissionSortEnd = () => {
  // 收集排序后的项目ID列表（排除默认项目 key=0）
  const missionIds = loadSortedMissionList.value
    .filter(m => m.key !== '0')
    .map(m => parseInt(m.key))
  if (missionIds.length === 0) return
  const params = {
    wid: useWid(),
    missionIds
  }
  ioRequest('mission.sort', params)
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
  Mitter.off('mitt-session-mission-update', handleSessionMissionUpdate)
})

const handleDataLoad = () => {
  const params = {
    wid: useWid()
  }
  ioRequest('sessions.index', params).then((result) => {
    infomation.value = result[0] || []
  })
  ioRequest('mission.index', params).then((result) => {
    missionList.value = result[0] || []
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
  Mitter.on('mitt-session-mission-update', handleSessionMissionUpdate)
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
// 输入框修改分组后同步更新左侧列表
const handleSessionMissionUpdate = (payload: any) => {
  const { sid, missionId, missionName } = payload
  const missionKey = String(missionId)
  const matched = infomation.value.find((item: any) => String(item.id) === String(sid))
  if (matched) {
    matched.missionId = missionId
    if (missionId === 0) {
      matched.missionName = null
      matched.missionSortOrder = 0
    } else {
      matched.missionName = missionName || ''
      const targetMission = missionList.value.find((m: any) => m.id === missionId)
      matched.missionSortOrder = targetMission ? (targetMission.sortOrder || 0) : 0
    }
  }
  // 展开目标分组（如果折叠中）
  if (loadCollapsedMissions.value[missionKey]) {
    loadCollapsedMissions.value[missionKey] = false
  }
  // 如果是新建的分组，刷新 missionList
  if (missionId > 0 && !missionList.value.find((m: any) => m.id === missionId)) {
    ioRequest('mission.index', { wid: useWid() }).then((result) => {
      missionList.value = result[0] || []
    })
  }
}
// ==================== 会话拖拽到分组 ====================
const handleSessionDragStart = (event: DragEvent, info: any) => {
  sessionDragData.value = info
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(info.id))
  }
}
const handleSessionDragEnd = () => {
  sessionDragData.value = null
  sessionDragOverMissionKey.value = null
}
const handleSessionDragOver = (event: DragEvent, missionKey: string) => {
  if (!sessionDragData.value) return
  event.preventDefault()
  sessionDragOverMissionKey.value = missionKey
}
const handleSessionDragLeave = (event: DragEvent, missionKey: string) => {
  // 只在真正离开该分组时清除高亮
  const relatedTarget = event.relatedTarget as HTMLElement | null
  const currentTarget = event.currentTarget as HTMLElement
  if (relatedTarget && currentTarget.contains(relatedTarget)) return
  if (sessionDragOverMissionKey.value === missionKey) {
    sessionDragOverMissionKey.value = null
  }
}
const handleSessionDrop = (event: DragEvent, missionKey: string) => {
  event.preventDefault()
  sessionDragOverMissionKey.value = null
  const info = sessionDragData.value
  if (!info) return
  const currentMissionId = String(info.missionId || 0)
  if (currentMissionId === missionKey) {
    // 未变更分组，不处理
    sessionDragData.value = null
    return
  }
  const newMissionId = parseInt(missionKey)
  const params = {
    wid: useWid(),
    sid: String(info.id),
    missionId: newMissionId
  }
  ioRequest('sessions.mission.update', params).then((result: any) => {
    const success = result[0]
    if (success) {
      // 更新本地数据
      const matched = infomation.value.find((item: any) => item.id === info.id)
      if (matched) {
        matched.missionId = newMissionId
        // 更新 missionName
        if (newMissionId === 0) {
          matched.missionName = null
          matched.missionSortOrder = 0
        } else {
          const targetMission = missionList.value.find((m: any) => m.id === newMissionId)
          matched.missionName = targetMission ? targetMission.name : ''
          matched.missionSortOrder = targetMission ? (targetMission.sortOrder || 0) : 0
        }
      }
      // 展开目标分组（如果折叠中）
      if (loadCollapsedMissions.value[missionKey]) {
        loadCollapsedMissions.value[missionKey] = false
      }
      // 通知右侧输入框的分组按钮更新
      Mitter.emit('mitt-drawer-mission-update', { sid: String(info.id), missionId: newMissionId })
    } else {
      ElMessage.error(i18n.t('mission.move-failed'))
    }
  })
  sessionDragData.value = null
}
const handleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
const handleNewSession = () => {
  router.replace({
    path: route.path,
    query: {}
  })
}
const handleNewSessionInMission = (mission: any) => {
  // 先通知输入框预设分组（设置skip标记，防止watch重置）
  Mitter.emit('mitt-new-session-mission', {
    missionId: parseInt(mission.key),
    missionName: mission.name
  })
  router.replace({
    path: route.path,
    query: {}
  })
}
const handleAddMission = () => {
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
    const params = {
      wid: useWid(),
      name: name,
      description: ''
    }
    ioRequest('mission.add', params).then((result: any) => {
      const newMissionId = result[0]
      if (!newMissionId) {
        ElMessage.error(i18n.t('mission.create-failed'))
        return
      }
      ElMessage.success(i18n.t('mission.create-success'))
      // 刷新分组列表
      ioRequest('mission.index', { wid: useWid() }).then((res) => {
        missionList.value = res[0] || []
      })
      // 先通知输入框预设分组（设置skip标记），再开启新会话
      Mitter.emit('mitt-new-session-mission', {
        missionId: parseInt(newMissionId),
        missionName: name
      })
      router.replace({ path: route.path, query: {} })
    })
  }).catch(() => {})
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
    i18n.t('mission.rename-placeholder'),
    i18n.t('mission.rename-title'),
    {
      inputValue: mission.name,
      inputPattern: /\S+/,
      inputErrorMessage: i18n.t('mission.create-empty'),
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
    missionId: mission.key,
    name: newName
  }
  ioRequest('mission.rename', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('mission.rename-failed'))
      return
    }
    ElMessage.success(i18n.t('mission.rename-success'))
    // 更新本地分组列表中的名称
    const targetMission = missionList.value.find((m: any) => String(m.id) === mission.key)
    if (targetMission) {
      targetMission.name = newName
    }
    // 更新本地会话列表中的项目名称
    for (const info of infomation.value) {
      if (String(info.missionId) === mission.key) {
        info.missionName = newName
      }
    }
    // 通知右侧输入框分组名称更新
    Mitter.emit('mitt-mission-rename', { missionId: parseInt(mission.key), missionName: newName })
  })
}
// 删除项目
const handleDeleteMission = () => {
  const mission = loadActivedMissionData.value
  if (!mission || mission.key === '0') return
  handleMissionContextMenuHide()
  ElMessageBox.confirm(
    i18n.t('mission.delete-confirm', { placeholder: mission.name }),
    i18n.t('mission.delete-title'),
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
    missionId: mission.key
  }
  ioRequest('mission.remove', params).then((result: any) => {
    const success = result[0]
    if (!success) {
      ElMessage.error(i18n.t('mission.delete-failed'))
      return
    }
    ElMessage.success(i18n.t('mission.delete-success'))
    // 将该项目下的会话归入默认项目
    for (const info of infomation.value) {
      if (String(info.missionId) === mission.key) {
        info.missionId = 0
        info.missionName = null
        info.missionSortOrder = 0
      }
    }
    // 从 missionList 中移除
    const missionIndex = missionList.value.findIndex((m: any) => String(m.id) === mission.key)
    if (missionIndex !== -1) {
      missionList.value.splice(missionIndex, 1)
    }
  })
}
</script>
