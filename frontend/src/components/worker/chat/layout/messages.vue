<template>
  <div class="messages" ref="loadMessagesRef">
    <div class="wrapper inline-flex-c-n-n" v-loading="loading">
      <!-- 消息欢迎页 -->
      <a-messages-welcome :messages="messages" :streaming="streaming" />
      <!-- 消息列表页 -->
      <div class="sources" v-if="messages.length > 0 || isShowStandaloneStreamingIndicator">
        <!-- 数据库消息详情 -->
        <div class="round" v-for="(round, ridx) in loadFormatMessages" :key="ridx">
          <!-- 用户发送消息 -->
          <div class="message is-user">
            <el-avatar class="avatar" :size="42" :icon="UserFilled" shape="square" />
            <div class="context">
              <template v-if="isUserMessageEditing(round.user)">
                <div class="inbox">
                  <textarea class="input" :ref="handleEditInputRefSet" v-model="loadEditingMessageContent" rows="3"
                    @keydown="handleUserMessageEditKeydown($event, round.user)"
                  ></textarea>
                  <div class="operation" v-if="!streaming">
                    <el-tooltip effect="dark" placement="top" :content="$t('common.cancel')">
                      <el-icon class="icon" @click="handleUserMessageEditCancel">
                        <Close />
                      </el-icon>
                    </el-tooltip>
                    <el-tooltip effect="dark" placement="top" :content="$t('common.confirm')">
                      <el-icon class="icon is-primary" @click="handleUserMessageEditSubmit(round.user)">
                        <Select />
                      </el-icon>
                    </el-tooltip>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="markdown" v-html="markdownItIntance.render(round.user?.message?.content || '')"></div>
                <div class="operation" v-if="!streaming">
                  <el-tooltip effect="dark" placement="top" :content="$t('common.copy')">
                    <el-icon class="icon" @click="handleMessageCopy(useMessageActionKey(round.user, 'user'), round.user?.message?.content || '')">
                      <Check v-if="isMessageCopied(useMessageActionKey(round.user, 'user'))" />
                      <DocumentCopy v-else />
                    </el-icon>
                  </el-tooltip>
                  <el-tooltip v-if="isLastUserMessage(round.user)" effect="dark" placement="top" :content="$t('common.edit')">
                    <el-icon class="icon" @click="handleUserMessageEdit(round.user)">
                      <Edit />
                    </el-icon>
                  </el-tooltip>
                </div>
              </template>
            </div>
          </div>
          <!-- 智能体回复消息 -->
          <div class="message is-assistant" v-if="round.common.length > 0">
            <a-robot-head :kind="0" :size="42" :src="IconImage" />
            <div class="context">
              <template v-for="(msg, midx) in round.common" :key="midx">
                <!-- 推理结果文本 -->
                <div class="reasoning" v-if="msg.message.reasoning">
                  <el-collapse v-model="loadOpenedReasonings">
                    <el-collapse-item :name="msg.id">
                      <template #title>
                        <div class="label inline-flex-r-c-n">
                          <el-icon><Opportunity /></el-icon>
                          <div>{{ $t('chat.thinking') }}</div>
                        </div>
                      </template>
                      <div class="markdown" v-html="markdownItIntance.render(msg.message.reasoning)"></div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
                <!-- 文件修改统计 -->
                <a-messages-filesystem v-if="midx === round.common.length - 1 && useRoundHasApproval(round)"
                  :round="round"
                  :action-key="useRoundActionKey(round, ridx)"
                  :streaming="isStreamMessageComplete(ridx)"
                />
                <!-- 正常文本回复 -->
                <template v-if="isRoleAssistant(msg.message.role) && useAssistantFailureInfo(msg)">
                  <div class="failure">
                    <div class="name inline-flex-r-c-n">
                      <el-icon><WarningFilled /></el-icon>
                      <div>{{ useAssistantFailureInfo(msg)?.title }}</div>
                    </div>
                    <div class="meta">{{ useAssistantFailureInfo(msg)?.message }}</div>
                    <div class="code" v-if="useAssistantFailureInfo(msg)?.meta">{{ useAssistantFailureInfo(msg)?.meta }}</div>
                  </div>
                </template>
                <!-- 审批弹窗操作 -->
                <template v-else-if="isRoleAssistant(msg.message.role) && msg.message.approval">
                  <div class="approval">
                    <div class="title inline-flex-r-c-n">
                      <el-icon><WarningFilled /></el-icon>
                      <div>{{ $t('chat.approval-required-title') }}</div>
                    </div>
                    <div class="meta">{{ msg.message.approval.reason }}</div>
                    <pre class="command">{{ msg.message.approval.command }}</pre>
                    <div class="operate" v-if="isApprovalPending(msg)">
                      <a-button size="small" :icon="Close"
                        @click="handleApprovalSubmit(msg, false)"
                      >
                        {{ $t('chat.approval-reject') }}
                      </a-button>
                      <a-button size="small" type="success" :icon="Check"
                        @click="handleApprovalSubmit(msg, true)"
                      >
                        {{ $t('chat.approval-approve') }}
                      </a-button>
                    </div>
                    <div class="result" v-else :class="{ 'is-approved': msg.message.approval.status === 'approved' }">
                      {{ useApprovalStatusText(msg) }}
                    </div>
                  </div>
                </template>
                <!-- 错误页面展现 -->
                <template v-else-if="isRoleAssistant(msg.message.role) && msg.message.content?.trim()">
                  <div class="markdown" v-html="markdownItIntance.render(msg.message.content)"
                    :class="{
                      'is-failed': msg.message.status === 'failed'
                    }"
                  ></div>
                </template>
                <!-- 工具调用消息 -->
                <div class="tool" v-if="isRoleAssistant(msg.message.role) && useVisibleTools(msg.message.tools).length">
                  <div class="unit" v-for="(tool, tidx) in useVisibleTools(msg.message.tools)" :key="tidx">
                    <div class="step inline-flex-r-c-n"
                      :class="{
                        'is-success': tool.success,
                        'is-opened': loadOpenedSteps.has(`${midx}-${tidx}`)
                      }"
                      @click="handleStepSwitch(`${midx}-${tidx}`)"
                    >
                      <el-icon class="status"><Select /></el-icon>
                      <el-icon class="icon"><Setting /></el-icon>
                      <div class="name">{{ $t(`chat.tool.${tool.name}`) }}</div>
                      <el-icon class="arrow"><ArrowRight /></el-icon>
                    </div>
                    <transition name="metadata-slide">
                      <div class="metadata" v-show="loadOpenedSteps.has(`${midx}-${tidx}`)">
                        <pre>{{ handleToolArgumentsFormat(tool.arguments) }}</pre>
                      </div>
                    </transition>
                  </div>
                </div>
              </template>
              <!-- 文件修改统计汇总 -->
              <a-messages-filesystem v-if="!useRoundHasApproval(round)"
                :round="round"
                :action-key="useRoundActionKey(round, ridx)"
                :streaming="isStreamMessageComplete(ridx)"
              />
              <!-- 会话消息操作 -->
              <div class="operation" v-if="!streaming">
                <el-tooltip effect="dark" placement="top" :content="$t('common.copy')">
                  <el-icon class="icon" @click="handleMessageCopy(useRoundActionKey(round, ridx), useAssistantMessageText(round))">
                    <Check v-if="isMessageCopied(useRoundActionKey(round, ridx))" />
                    <DocumentCopy v-else />
                  </el-icon>
                </el-tooltip>
                <el-tooltip effect="dark" placement="top" :content="$t('chat.fork-session')">
                  <el-icon class="icon" @click="handleMessageFork(round, ridx)">
                    <KnifeFork />
                  </el-icon>
                </el-tooltip>
              </div>
            </div>
          </div>
          <!-- 指令流式消息指示器 -->
          <a-messages-indicator v-if="isRoundStreamingIndicatorVisible(round, ridx)"
            :status-text="loadStreamingStatusText"
            :has-context="round.common.length > 0"
            :show-avatar="round.common.length === 0"
          />
        </div>
        <!-- 通用流式消息指示器 -->
        <a-messages-indicator v-if="isShowStandaloneStreamingIndicator" standalone
          :status-text="loadStreamingStatusText"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ElMessage
} from 'element-plus'
import {
  Edit,
  Check,
  Close,
  Select,
  Setting,
  ArrowRight,
  KnifeFork,
  UserFilled,
  Opportunity,
  WarningFilled,
  DocumentCopy
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { useTextClipboard } from '@/utils/dom'
import useMarkdownIt from '@/marksuit/hook/useMarkdownIt'
import AMessagesWelcome from './messages/welcome.vue'
import AMessagesIndicator from './messages/indicator.vue'
import AMessagesFilesystem from './messages/filesystem.vue'
import ARobotHead from '@/components/common/robot/head.vue'
import IconImage from '@/assets/images/providers/deepseek.png'

interface FormatRound {
  user: any
  common: any[]
}

const props = defineProps<{
  messages: any[]
  loading: boolean
  streaming: boolean
}>()

const handleEmit = defineEmits<{
  (e: 'resend', payload: { message: any, content: string }): void
  (e: 'fork', payload: { round: FormatRound, ridx: number }): void
  (e: 'approval', payload: { message: any, approved: boolean }): void
}>()

const i18n = useI18n()
let loadCopiedMessageTimer: number | undefined
const loadOpenedSteps = ref(new Set<string>())
const loadOpenedReasonings = ref<string[]>([])
const loadCopiedMessageKey = ref('')
const loadEditingMessageKey = ref('')
const loadEditingMessageContent = ref('')
const loadMessagesRef = ref<HTMLDivElement | null>(null)
const loadEditInputRef = ref<HTMLTextAreaElement | null>(null)

let markdownItIntance: MarkdownIt = useMarkdownIt({
  isCodeCopy: true
})

// 按会话迭代ID分组，每组包含一条用户消息 + 若干迭代消息
const loadFormatMessages = computed<FormatRound[]>(() => {
  const mapping = new Map<string, FormatRound>()
  const order: string[] = []
  for (const data of props.messages) {
    const rid = data.rid
    if (!mapping.has(rid)) {
      mapping.set(rid, { user: null, common: [] })
      order.push(rid)
    }
    const round = mapping.get(rid)!
    if (data.message.role === 'user') {
      round.user = data
    } else {
      round.common.push(data)
    }
  }
  return order.map(rid => mapping.get(rid)!)
})
const loadLastUserMessage = computed(() => {
  return [...props.messages].reverse().find(data => data.message?.role === 'user')
})
const isRoleAssistant = (role: string) => {
  return role === 'assistant'
}
const isApprovalPending = (message: any): boolean => {
  return message?.message?.approval?.status === 'pending'
}
const useRoundHasApproval = (round: FormatRound): boolean => {
  return round.common.some(data => data?.message?.approval)
}
const useApprovalStatusText = (message: any): string => {
  const status = message?.message?.approval?.status
  if (status === 'approved') return i18n.t('chat.approval-approved')
  if (status === 'rejected') return i18n.t('chat.approval-rejected')
  if (status === 'expired') return i18n.t('chat.approval-expired')
  return i18n.t('chat.approval-processed')
}
const handleApprovalSubmit = (message: any, approved: boolean) => {
  if (!isApprovalPending(message)) return
  handleEmit('approval', { message, approved })
}

const handleStepSwitch = (key: string) => {
  const next = new Set(loadOpenedSteps.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  loadOpenedSteps.value = next
}
const handleToolArgumentsFormat = (args: any): string => {
  if (!args) return ''
  try {
    const parsed = typeof args === 'string' ? JSON.parse(args) : args
    return JSON.stringify(parsed, null, 2)
  } catch {
    return typeof args === 'string' ? args : JSON.stringify(args, null, 2)
  }
}
const isFileUpdateTool = (tool: any): boolean => {
  if (tool?.fileUpdate?.path) return true
  const name = String(tool?.name || '').toLowerCase()
  return name === 'edit_file' || name === 'write_file' || name.includes('delete_file') || name.includes('remove_file')
}
const useVisibleTools = (tools: any[] = []) => {
  return tools.filter(tool => !isFileUpdateTool(tool))
}
const handleMaybeJsonParse = (value: string): any => {
  const text = (value || '').trim()
  if (!text) return null
  const ssePayloads = text
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => line.startsWith('data:'))
    .map(line => line.slice(5).trim())
    .filter(line => line && line !== '[DONE]')
  const candidates = ssePayloads.length > 0 ? ssePayloads : [text]
  for (const candidate of candidates) {
    const jsonCandidate = candidate.startsWith('{') || candidate.startsWith('[')
      ? candidate
      : candidate.slice(candidate.indexOf('{'), candidate.lastIndexOf('}') + 1)
    if (!jsonCandidate.startsWith('{') && !jsonCandidate.startsWith('[')) {
      continue
    }
    try {
      return JSON.parse(jsonCandidate)
    } catch {
      continue
    }
  }
  return null
}
const useAssistantFailureInfo = (data: any) => {
  const message = data?.message || {}
  const status = Number(message.status)
  const hasHttpStatusError = Number.isFinite(status) && status > 0 && status !== 200
  const hasFailedStatus = message.status === 'failed'
  const content = message.content || ''
  const payload = typeof content === 'string' ? handleMaybeJsonParse(content) : null
  const code = Number(payload?.code)
  const hasBusinessError = payload && Number.isFinite(code) && code !== 0 && code !== 200
  const hasStructuredError = Boolean(payload?.error)
  if (!hasHttpStatusError && !hasFailedStatus && !hasBusinessError && !hasStructuredError) {
    return null
  }
  const detail = payload?.message || payload?.error?.message || payload?.error || content || i18n.t('error.network-error')
  const meta = [
    hasHttpStatusError ? `HTTP ${status}` : '',
    hasBusinessError ? `Code ${code}` : '',
    hasStructuredError && payload?.error?.code ? `Code ${payload.error.code}` : '',
    hasStructuredError && payload?.error?.type ? payload.error.type : '',
    hasStructuredError && payload?.error?.trace_id ? `Trace ${payload.error.trace_id}` : ''
  ].filter(Boolean).join(' · ')
  return {
    title: i18n.t('common.error'),
    message: typeof detail === 'string' ? detail : JSON.stringify(detail),
    meta
  }
}
const useAssistantMessageText = (round: FormatRound): string => {
  return round.common
    .filter(data => isRoleAssistant(data.message?.role))
    .map(data => {
      const failure = useAssistantFailureInfo(data)
      if (failure) {
        return [failure.title, failure.message, failure.meta].filter(Boolean).join('\n')
      }
      return [data.message?.reasoning, data.message?.content].filter(Boolean).join('\n\n')
    })
    .filter(Boolean)
    .join('\n\n')
}
const useMessageActionKey = (message: any, fallback: string): string => {
  return `${fallback}:${message?.id || message?.rid || ''}`
}
const useRoundActionKey = (round: FormatRound, ridx: number): string => {
  const last = round.common[round.common.length - 1]
  return useMessageActionKey(last, `assistant:${round.user?.rid || ridx}`)
}
const isMessageCopied = (key: string): boolean => {
  return Boolean(key) && loadCopiedMessageKey.value === key
}
const isUserMessageEditing = (message: any): boolean => {
  return Boolean(loadEditingMessageKey.value) && loadEditingMessageKey.value === useMessageActionKey(message, 'user')
}
const isLastUserMessage = (message: any): boolean => {
  if (!message?.id || props.streaming) {
    return false
  }
  const last = loadLastUserMessage.value
  if (!last) {
    return false
  }
  if (message.id && last.id) {
    return message.id === last.id
  }
  return message.rid === last.rid
}
const handleMessageCopy = (key: string, value: string) => {
  if (!value || !value.trim()) return
  useTextClipboard(value).then(() => {
    loadCopiedMessageKey.value = key
    if (loadCopiedMessageTimer) {
      window.clearTimeout(loadCopiedMessageTimer)
    }
    loadCopiedMessageTimer = window.setTimeout(() => {
      loadCopiedMessageKey.value = ''
      loadCopiedMessageTimer = undefined
    }, 1200)
    ElMessage.success(i18n.t('extension.copy-success'))
  })
}
const handleMessageFork = (round: FormatRound, ridx: number) => {
  handleEmit('fork', { round, ridx })
}
// 用户消息编辑重发
const handleUserMessageEdit = (message: any) => {
  const content = message?.message?.content || ''
  if (!content.trim()) return
  loadEditingMessageKey.value = useMessageActionKey(message, 'user')
  loadEditingMessageContent.value = content
  nextTick(() => {
    loadEditInputRef.value?.focus()
  })
}
const handleEditInputRefSet = (el: Element | any) => {
  loadEditInputRef.value = el as HTMLTextAreaElement | null
}
const handleUserMessageEditCancel = () => {
  loadEditingMessageKey.value = ''
  loadEditingMessageContent.value = ''
  loadEditInputRef.value = null
}
const handleUserMessageEditSubmit = (message: any) => {
  const content = loadEditingMessageContent.value.trim()
  if (!content) return
  handleEmit('resend', { message, content })
  handleUserMessageEditCancel()
}
const handleUserMessageEditKeydown = (event: KeyboardEvent, message: any) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleUserMessageEditSubmit(message)
    return
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    handleUserMessageEditCancel()
  }
}
// 添加推理折叠面板展开
const addOpenedReasoning = (id: string) => {
  if (!loadOpenedReasonings.value.includes(id)) {
    loadOpenedReasonings.value.push(id)
  }
}
// 判断当前轮是否正在流式传输
const isStreamMessageComplete = (ridx: number) => {
  return props.streaming && ridx === loadFormatMessages.value.length - 1
}
// 判断当前轮是否需要显示指示器
const isShowStreamingIndicator = (round: FormatRound): boolean => {
  if (round.common.length === 0) {
    return true
  }
  const last = round.common[round.common.length - 1]
  // 如果最后一条消息还没有内容
  if (!last.message.content?.trim()) {
    return true
  }
  // 如果有工具调用正在执行
  if (last.message.tools?.length) {
    return true
  }
  return false
}
const isRoundStreamingIndicatorVisible = (round: FormatRound, ridx: number): boolean => {
  return isStreamMessageComplete(ridx) && isShowStreamingIndicator(round)
}
const isShowStandaloneStreamingIndicator = computed(() => {
  if (!props.streaming) {
    return false
  }
  const isMessageSending = props.messages.some(message => {
    return isRoleAssistant(message.message?.role) && message.finished === false
  })
  if (isMessageSending) {
    return false
  }
  const rounds = loadFormatMessages.value
  if (rounds.length === 0) {
    return true
  }
  const lastIndex = rounds.length - 1
  return !isRoundStreamingIndicatorVisible(rounds[lastIndex], lastIndex)
})
// 根据当前执行阶段动态切换
const loadStreamingStatusText = computed(() => {
  if (!props.streaming) return ''
  const messages = props.messages
  if (messages.length === 0) {
    return i18n.t('chat.streaming.thinking')
  }
  const last = messages[messages.length - 1]
  if (!last?.message) {
    return i18n.t('chat.streaming.thinking')
  }
  // 有工具调用中
  if (last.message.tools?.length) {
    const lastTool = last.message.tools[last.message.tools.length - 1]
    if (lastTool && !lastTool.success) {
      const toolKey = `chat.tool.${lastTool.name}`
      const toolName = i18n.t(toolKey) !== toolKey ? i18n.t(toolKey) : lastTool.name
      return i18n.t('chat.streaming.tool-executing', { placeholder: toolName })
    }
  }
  // 有推理正在进行
  if (last.message.reasoning && !last.message.content?.trim()) {
    return i18n.t('chat.streaming.reasoning')
  }
  // 默认思考中
  return i18n.t('chat.streaming.thinking')
})

onUnmounted(() => {
  if (loadCopiedMessageTimer) {
    window.clearTimeout(loadCopiedMessageTimer)
  }
})

const useInstance = () => loadMessagesRef.value
defineExpose({
  addOpenedReasoning,
  useInstance
})
</script>
