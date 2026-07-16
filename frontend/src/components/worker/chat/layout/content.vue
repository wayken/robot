<template>
  <div class="content" ref="loadContentRef">
    <div class="wrapper inline-flex-c-n-n" v-loading="loading">
      <div class="welcome inline-flex-c-c-c" v-if="messages.length === 0">
        <img class="logo" :src="logoSvg" />
        <div class="domain">
          Team<span class="highlight">Robot</span>
        </div>
        <div class="slogan">
          {{ $t('chat.slogan') }}
        </div>
        <div class="suggestion">
          <div class="card inline-flex-r-c-n" v-for="(data, index) in loadSuggestionList" :key="index">
              <div class="name">{{ $t(`chat.${data.name}`) }}</div>
              <el-icon class="icon"><Right /></el-icon>
          </div>
        </div>
      </div>
      <div class="sources" v-if="messages.length > 0">
        <div class="round" v-for="(round, ridx) in loadFormatMessages" :key="ridx">
          <!-- 用户消息 -->
          <div class="message is-user">
            <el-avatar class="avatar" :size="42" :icon="UserFilled" shape="square" />
            <div class="context">
              <div class="markdown" v-html="markdownItIntance.render(round.user?.message?.content || '')"></div>
            </div>
          </div>
          <!-- AI 回复（含工具调用过程） -->
          <div class="message is-assistant" v-if="round.common.length > 0">
            <a-robot-head :kind="0" :size="42" :src="IconImage" />
            <div class="context">
              <template v-for="(msg, midx) in round.common" :key="midx">
                <!-- 推理结果文本 -->
                <div class="reasoning" v-if="msg.message.reasoning">
                  <el-collapse v-model="openedReasonings">
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
                <!-- 正常文本回复 -->
                <template v-if="msg.message.role === 'assistant' && msg.message.content?.trim()">
                  <div class="markdown" v-html="markdownItIntance.render(msg.message.content)"
                    :class="{
                      'is-failed': msg.message.status === 'failed'
                    }"
                  ></div>
                </template>
                <!-- 工具调用消息 -->
                <div class="tool" v-if="msg.message.role === 'assistant' && msg.message.tools?.length">
                  <div class="unit" v-for="(tool, tidx) in msg.message.tools" :key="tidx">
                    <div class="step inline-flex-r-c-n"
                      :class="{
                        'is-success': tool.success,
                        'is-opened': openedSteps.has(`${midx}-${tidx}`)
                      }"
                      @click="handleStepSwitch(`${midx}-${tidx}`)"
                    >
                      <el-icon class="status"><Select /></el-icon>
                      <el-icon class="icon"><Setting /></el-icon>
                      <div class="name">{{ $t(`chat.tool.${tool.name}`) }}</div>
                      <el-icon class="arrow"><ArrowRight /></el-icon>
                    </div>
                    <transition name="metadata-slide">
                      <div class="metadata" v-show="openedSteps.has(`${midx}-${tidx}`)">
                        <pre>{{ handleToolArgumentsFormat(tool.arguments) }}</pre>
                      </div>
                    </transition>
                  </div>
                </div>
              </template>
            </div>
          </div>
          <!-- 流式消息指示器 -->
          <div class="message is-assistant" v-if="isStreamMessageComplete(ridx) && isShowStreamingIndicator(round)">
            <a-robot-head :kind="0" :size="42" :src="IconImage" v-if="round.common.length === 0" />
            <div class="indicator"
              :class="{
                'has-context': round.common.length > 0
              }"
            >
              <a-loading size="small" />
              <div class="progress">
                <span class="label">{{ loadStreamingStatusText }}</span>
                <span class="dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Right,
  Select,
  Setting,
  ArrowRight,
  UserFilled,
  Opportunity
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import logoSvg from '@/assets/logo.svg'
import useMarkdownIt from '@/marksuit/hook/useMarkdownIt'
import ARobotHead from '@/components/common/robot/head.vue'
import ALoading from '@/components/common/loading/index.vue'
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

const loadSuggestionList = [
  {
    name: 'suggestion-0'
  },
  {
    name: 'suggestion-1'
  },
  {
    name: 'suggestion-2'
  },
  {
    name: 'suggestion-3'
  }
]

const i18n = useI18n()
const openedSteps = ref(new Set<string>())
const openedReasonings = ref<string[]>([])
const loadContentRef = ref<HTMLDivElement | null>(null)

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
const handleStepSwitch = (key: string) => {
  const next = new Set(openedSteps.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  openedSteps.value = next
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
// 添加推理折叠面板展开
const addOpenedReasoning = (id: string) => {
  if (!openedReasonings.value.includes(id)) {
    openedReasonings.value.push(id)
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
// 根据当前执行阶段动态切换
const loadStreamingStatusText = computed(() => {
  if (!props.streaming) return ''
  const msgs = props.messages
  if (msgs.length === 0) {
    return i18n.t('chat.streaming.thinking')
  }
  const last = msgs[msgs.length - 1]
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

const useInstance = () => loadContentRef.value

defineExpose({
  addOpenedReasoning,
  useInstance
})
</script>
