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
    />
    <a-promotion ref="loadPromotionRef"
      @submit="handleMessageSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import {
  MessageRoleEnum
} from '@/types/index'
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
  useConsumeBuffer
} from '@/hooks/useStreamingState'

const {
  ioOn,
  ioCommit,
  ioRequest,
  progression
} = useSocketIO()
const {
  useClearBuffer,
  usePushToBuffer,
  useStopStreaming,
  useStartStreaming
} = useStreamingState()
const route = useRoute()
const router = useRouter()

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
  Mitter.on('mitt-chat-messages-remove', handleMessagesClear)
})
onUnmounted(() => {
  loadContentRef.value?.useInstance()?.removeEventListener('scroll', handleContentScroll)
  Mitter.off('mitt-chat-messages-remove', handleMessagesClear)
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
    const buffered = useConsumeBuffer(sessionId)
    if (buffered.length > 0) {
      // message.index 返回的是数据库权威数据（后端在广播 completion 前已落库）。
      // 收集其中已包含 AI 回复(assistant/tool)的轮次 rid：这些轮次的流式增量
      // 已经持久化，缓冲区里的副本是重复数据，必须丢弃。
      // 注意：SSE 消息的 id 是模型响应 id(字符串)，而持久化消息的 id 是数据库自增整型，
      // 两者永远无法通过 id 匹配去重，因此改用 rid 维度做对账。
      const persistedRids = new Set(
        infomation.value
          .filter(info => info.message?.role !== MessageRoleEnum.User)
          .map(info => info.rid)
      )
      for (const msg of buffered) {
        // 该轮 AI 回复已落库 => 缓冲区副本重复，跳过（避免消息重复出现）
        if (persistedRids.has(msg.rid)) {
          continue
        }
        // 仅合并尚未落库的"进行中"增量（通常是最后一轮）
        const matched = infomation.value.find(info => info.id === msg.id)
        if (matched) {
          matched.message = { ...matched.message, ...msg.message }
        } else {
          infomation.value.push(msg)
        }
      }
    }
    // 恢复流式传输状态
    if (isSessionStreaming(sessionId)) {
      isStreaming.value = true
    }
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
const handleMessagesClear = (sessionId: any) => {
  const currentSid = useSessionId()
  if (String(sessionId) === String(currentSid)) {
    infomation.value = []
  }
}
// 判断滚动容器是否已到底部（允许一定的误差）
const isScrollAtBottom = () => {
  const el = loadContentRef.value?.useInstance()
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight <= 20
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
    loadContentRef.value?.useInstance()?.addEventListener('scroll', handleContentScroll, {
      passive: true
    })
  })
  ioOn('message.response', (data) => {
    const response = data[0]
    const currentSid = useSessionId()
    const responseSid = response.sid || currentSid
    // 非当前会话的消息存入缓冲区，不丢弃
    if (responseSid && responseSid !== currentSid) {
      usePushToBuffer(responseSid, response)
      if (response.completion) {
        useStopStreaming(responseSid)
        // 该会话已完成，消息已全部落库，清空缓冲区，避免切回时与 message.index 返回的持久化消息重复
        useClearBuffer(responseSid)
        Mitter.emit('mitt-chat-streaming', { sid: responseSid, streaming: false })
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
      isStreaming.value = true
      useStartStreaming(responseSid)
      Mitter.emit('mitt-chat-streaming', { sid: responseSid, streaming: true })
    }
    if (!unref(isContentManualScrolled)) {
      handleContentAutoScroll()
    }
    if (response.completion) {
      isContentManualScrolled.value = false
      isStreaming.value = false
      useStopStreaming(responseSid)
      if (responseSid) {
        Mitter.emit('mitt-chat-streaming', { sid: responseSid, streaming: false })
      }
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
  const loadContentDom = loadContentRef.value?.useInstance()
  if (!loadContentDom) return
  nextTick(() => {
    loadContentDom.scrollTo({
      top: loadContentDom.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto'
    })
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
    isStreaming.value = true
    useStartStreaming(sessionId)
    Mitter.emit('mitt-chat-streaming', { sid: sessionId, streaming: true })
  } finally {
    isMessageSending.value = false
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
