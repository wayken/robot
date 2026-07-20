<template>
  <div class="layout inline-flex-c-n-n"
    :class="{
      'is-narrow': isWindowNarrow
    }"
  >
    <a-head @narrow="handleLayoutNarrow"></a-head>
    <a-content ref="loadContentRef" :loading="progression.loading"
      :messages="infomation"
      :streaming="isStreaming"
      @resend="handleMessageResend"
      @fork="handleMessageFork"
    />
    <a-promotion ref="loadPromotionRef"
      :streaming="isStreaming"
      @cancel="handleMessageCancel"
      @submit="handleMessageSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import {
  MessageRoleEnum
} from '@/types/index'
import { ElMessage } from 'element-plus'
import { uuid } from '@/utils/dom'
import Mitter from '@/utils/mitt'
import AHead from './layout/head.vue'
import AContent from './layout/content.vue'
import APromotion from './layout/promotion.vue'
import useSocketIO from '@/hooks/useSocketIO'
import useLocalProfile from '@/hooks/useLocalProfile'
import {
  useStreamingState,
  isSessionStreaming,
  useBufferConsume
} from '@/hooks/useStreamingState'

const {
  ioOn,
  ioCommit,
  ioRequest,
  progression
} = useSocketIO()
const {
  useBufferPush,
  useBufferRemove,
  useStreamingStop,
  useStreamingStart
} = useStreamingState()
const route = useRoute()
const router = useRouter()
const i18n = useI18n()

const infomation = ref<any[]>([])
const localProfile = useLocalProfile('worker')
const isWindowNarrow = ref(localProfile.get('chat.isWindowNarrow'))
const isContentManualScrolled = ref(false)
const isStreaming = ref(false)
const isMessageSending = ref(false)
const isSessionBuilding = ref(false)
const useWid = () => route.params.id as string
const useSessionId = () => route.query.id as string
const loadContentRef = ref<InstanceType<typeof AContent> | null>(null)
const loadPromotionRef = ref<InstanceType<typeof APromotion> | null>(null)

onActivated(() => {
  handlePromotionFocus()
})
onMounted(() => {
  const sessionId = useSessionId()
  if (sessionId) {
    handleDataLoad(sessionId)
  }
  handleMessageListenOn()
  // 监听清空消息事件
  Mitter.on('mitt-chat-messages-remove', handleMessagesRemove)
})
onUnmounted(() => {
  const instance = loadContentRef.value?.useInstance()
  instance?.removeEventListener('scroll', handleContentScroll)
  Mitter.off('mitt-chat-messages-remove', handleMessagesRemove)
})

const handleLayoutNarrow = (value: boolean) => {
  isWindowNarrow.value = value
}
const handleDataLoad = (sessionId: string) => {
  handlePromotionFocus()
  isStreaming.value = false
  progression.loading = true
  const params = {
    wid: useWid(),
    sid: sessionId
  }
  ioRequest('message.index', params).then((result) => {
    infomation.value = result[0]
    // 恢复缓冲区中尚未持久化的消息（切回会话时 SSE 期间产生的增量消息）
    const buffered = useBufferConsume(sessionId)
    if (buffered.length > 0) {
      // 获取数据库权威数据（后端在广播 completion 前已落库）
      const persistedRids = new Set(
        infomation.value
          .filter(info => info.message?.role !== MessageRoleEnum.User)
          .map(info => info.rid)
      )
      for (const message of buffered) {
        // 该轮 AI 回复已落库 => 缓冲区副本重复，跳过（避免消息重复出现）
        if (persistedRids.has(message.rid)) {
          continue
        }
        // 仅合并尚未落库的"进行中"增量（通常是最后一轮）
        const matched = infomation.value.find(info => info.id === message.id)
        if (matched) {
          matched.message = { ...matched.message, ...message.message }
        } else {
          infomation.value.push(message)
        }
      }
    }
    // 恢复流式传输状态
    if (isSessionStreaming(sessionId)) {
      handleSessionStreamingUpdate(sessionId, true)
    }
    handleSessionStatusRefresh(sessionId)
  }).finally(() => {
    handleContentAutoScroll()
  })
}
const handleDataReset = () => {
  infomation.value = []
  isStreaming.value = false
  handlePromotionFocus()
}
// 处理清空消息事件（由 drawer 组件触发）
const handleMessagesRemove = (sessionId: any) => {
  const currentSid = useSessionId()
  if (String(sessionId) === String(currentSid)) {
    infomation.value = []
  }
}
const handleSessionMessagesRefresh = (sessionId: string) => {
  if (!sessionId || String(sessionId) !== String(useSessionId())) return
  const params = {
    wid: useWid(),
    sid: sessionId
  }
  ioRequest('message.index', params).then((result) => {
    infomation.value = result[0] || []
    useBufferRemove(sessionId)
    if (!unref(isContentManualScrolled)) {
      handleContentAutoScroll()
    }
  })
}
// 判断滚动容器是否已到底部（允许一定的误差）
const isScrollAtBottom = () => {
  const instance = loadContentRef.value?.useInstance()
  if (!instance) return true
  return instance.scrollHeight - instance.scrollTop - instance.clientHeight <= 20
}
// 用户手动滚动时，判断是否离开了底部
const handleContentScroll = () => {
  if (isScrollAtBottom()) {
    isContentManualScrolled.value = false
  } else {
    isContentManualScrolled.value = true
  }
}
const handleMessageListenOn = () => {
  isContentManualScrolled.value = false
  nextTick(() => {
    const instance = loadContentRef.value?.useInstance()
    instance?.addEventListener('scroll', handleContentScroll, {
      passive: true
    })
  })
  ioOn('message.response', (data) => {
    const response = data[0]
    const currentSid = useSessionId()
    const responseSid = response.sid || currentSid
    // 非当前会话的消息存入缓冲区，不丢弃
    if (responseSid && responseSid !== currentSid) {
      useBufferPush(responseSid, response)
      if (response.completion) {
        handleSessionStreamingUpdate(responseSid, false)
        // 该会话已完成，消息已全部落库，清空缓冲区，避免切回时与 message.index 返回的持久化消息重复
        useBufferRemove(responseSid)
      }
      return
    }
    const matched = infomation.value.find(info => info.id === response.id)
    if (matched) {
      matched.message = { ...matched.message, ...response.message }
    } else {
      infomation.value.push(response)
    }
    if (response.message?.reasoning && response.id) {
      loadContentRef.value?.addOpenedReasoning(response.id)
    }
    // 收到非结束消息时激活流式指示器
    if (!response.completion && !isStreaming.value) {
      handleSessionStreamingUpdate(responseSid, true)
    }
    if (!unref(isContentManualScrolled)) {
      handleContentAutoScroll()
    }
    if (response.completion) {
      isContentManualScrolled.value = false
      handleSessionStreamingUpdate(responseSid, false)
      window.setTimeout(() => {
        handleSessionMessagesRefresh(responseSid)
      }, 120)
    }
  })
  ioOn('message.broadcast.truncate', (data) => {
    const response = data[0]
    if (response.wid !== useWid()) {
      return
    }
    handleMessageTruncate(response)
  })
  ioOn('message.broadcast.status', (data) => {
    const response = data[0]
    if (response.wid !== useWid()) {
      return
    }
    const isCurrentSessionStopped = String(response.sid) === String(useSessionId()) && !response.running
    handleSessionStreamingUpdate(response.sid, response.running)
    if (isCurrentSessionStopped) {
      window.setTimeout(() => {
        handleSessionMessagesRefresh(response.sid)
      }, 120)
    }
  })
}
const handlePromotionFocus = () => {
  nextTick(() => {
    loadPromotionRef.value?.focus()
  })
}
// 聊天框自动滚动到底部
const handleContentAutoScroll = (smooth = false) => {
  const instance = loadContentRef.value?.useInstance()
  if (!instance) return
  nextTick(() => {
    instance.scrollTo({
      top: instance.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto'
    })
  })
}
const handleSessionStreamingUpdate = (sid: string, streaming: boolean) => {
  if (!sid) return
  if (streaming) {
    useStreamingStart(sid)
  } else {
    useStreamingStop(sid)
  }
  if (sid === useSessionId()) {
    isStreaming.value = streaming
    if (streaming && !unref(isContentManualScrolled)) {
      handleContentAutoScroll()
    }
  }
  Mitter.emit('mitt-chat-streaming', { sid, streaming })
}
const handleSessionStatusRefresh = (sessionId: string) => {
  if (!sessionId) return
  const params = {
    wid: useWid()
  }
  ioRequest('sessions.index', params).then((result) => {
    const sessions = result[0] || []
    const currentSession = sessions.find((info: any) => String(info.id) === String(sessionId))
    handleSessionStreamingUpdate(sessionId, Boolean(currentSession?.running))
  }).catch(() => {
    handleSessionStreamingUpdate(sessionId, isSessionStreaming(sessionId))
  })
}
const handleMessageSubmit = async (message: string, display: string, missionId: number) => {
  if (!message || isMessageSending.value) return
  const rid = uuid()
  // 保存用户发送的消息数据（展示用纯文本，不含技能前缀）
  infomation.value.push({
    rid: rid,
    sid: useSessionId(),
    message: {
      role: MessageRoleEnum.User,
      content: display || message
    }
  })
  handleContentAutoScroll()
  // 调用大模型接口响应请求
  isMessageSending.value = true
  try {
    let sessionId = useSessionId()
    // 没有会话 ID 时先创建会话
    if (!sessionId) {
      const params: any = {
        wid: useWid(),
        name: (display || message).trim()
      }
      if (missionId > 0) {
        params.missionId = missionId
      }
      isSessionBuilding.value = true
      const result = await ioRequest('sessions.add', params)
      sessionId = result[0]
      await router.replace({
        query: {
          id: sessionId,
          ...route.query
        }
      })
      isSessionBuilding.value = false
    }
    // 发送消息到对应会话
    const params = {
      wid: useWid(),
      rid: rid,
      sid: sessionId,
      message: message
    }
    ioCommit('message.commit', params)
    // 消息发送后立即通知消息标题，加载动画
    handleSessionStreamingUpdate(sessionId, true)
  } finally {
    isMessageSending.value = false
  }
}
const handleMessageCancel = async () => {
  const sessionId = useSessionId()
  if (!sessionId || !isStreaming.value) return
  const params = {
    wid: useWid(),
    sid: sessionId
  }
  ioRequest('message.interrupt', params).finally(() => {
    handleSessionStreamingUpdate(sessionId, false)
    window.setTimeout(() => {
      handleSessionMessagesRefresh(sessionId)
    }, 120)
  })
}
const handleMessageTruncate = (payload: any) => {
  const sessionId = payload.sid
  if (!sessionId) return
  if (String(sessionId) !== String(useSessionId())) {
    useBufferRemove(sessionId)
    return
  }
  const index = infomation.value.findIndex(info => String(info.id) === String(payload.id))
  if (index >= 0) {
    infomation.value.splice(index)
  }
  if (payload.message) {
    const exists = infomation.value.some(info => String(info.rid) === String(payload.rid) && info.message?.role === MessageRoleEnum.User)
    if (!exists) {
      infomation.value.push({
        rid: payload.rid,
        sid: sessionId,
        message: {
          role: MessageRoleEnum.User,
          content: payload.message
        }
      })
    }
  }
  handleContentAutoScroll()
}
const handleMessageResend = async (payload: { message: any, content: string }) => {
  const sessionId = useSessionId()
  const content = payload.content.trim()
  const sourceId = payload.message?.id
  if (!sessionId || !sourceId || !content || isMessageSending.value || isStreaming.value) return
  const rid = uuid()
  const params = {
    wid: useWid(),
    sid: sessionId,
    id: sourceId,
    rid,
    message: content
  }
  isMessageSending.value = true
  try {
    const result = await ioRequest('message.truncate', params)
    if (!result[0]) {
      handleDataLoad(sessionId)
      return
    }
    handleMessageTruncate(params)
    ioCommit('message.commit', {
      wid: useWid(),
      sid: sessionId,
      rid,
      message: content
    })
    handleSessionStreamingUpdate(sessionId, true)
  } finally {
    isMessageSending.value = false
  }
}
const handleMessageFork = async (payload: { round: any, ridx: number }) => {
  const sessionId = useSessionId()
  if (!sessionId) return
  const { round } = payload
  // 获取该轮最后一条assistant消息的ID作为截止消息
  const lastMessage = round.common[round.common.length - 1]
  if (!lastMessage?.id) return
  // 用用户消息内容的前20个字作为新会话名称
  const userContent = round.user?.message?.content || ''
  const name = userContent.substring(0, 20).trim() || 'Fork'
  const params = {
    wid: useWid(),
    sid: sessionId,
    messageId: String(lastMessage.id),
    name: `Fork: ${name}`
  }
  try {
    const result = await ioRequest('sessions.fork', params)
    const newSid = result[0]
    if (!newSid) {
      ElMessage.error(i18n.t('chat.fork-session-failed'))
      return
    }
    ElMessage.success(i18n.t('chat.fork-session-success'))
    // 导航到新会话
    router.replace({
      path: route.path,
      query: { id: newSid }
    })
  } catch {
    ElMessage.error(i18n.t('chat.fork-session-failed'))
  }
}

watch(() => route.query.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      // 由发送消息创建新会话触发的路由变化，不清空消息列表
      if (isSessionBuilding.value) return
      // 触发新会话路由变化，重新加载数据
      infomation.value = []
      handleDataLoad(newId as string)
    } else if (!newId) {
      // 新建会话
      handleDataReset()
    }
  }
)
</script>
