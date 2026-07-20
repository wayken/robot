<template>
  <div class="promotion">
    <div class="holder inline-flex-c-n-n">
      <div class="wrapper">
        <!-- 技能标签区域 -->
        <div v-if="loadSelectedSkills.length" class="skills inline-flex-r-c-n">
          <div v-for="(skill, index) in loadSelectedSkills" :key="skill.name" class="skill"
            :class="{
              'is-active': loadActiveSkillIndex === index
            }"
            @mouseenter="loadActiveSkillIndex = index"
            @mouseleave="loadActiveSkillIndex = -1"
            @click="handleRemoveSkill(index)"
          >
            <div class="name inline-flex-r-c-n">
              <a-svg-icon icon-class="skill" size="16px" />
              <div class="label">{{ skill.name }}</div>
            </div>
            <el-icon class="close"><Close /></el-icon>
          </div>
        </div>
        <textarea ref="loadPromotionRef" class="prompt" v-model="loadPromptValue" rows="2"
          :placeholder="$t('chat.message-placeholder')"
          @keydown="handlePromptSubmit"
        ></textarea>
      </div>
      <div class="footer inline-flex-r-c-b">
        <div class="addition inline-flex-r-c-n">
          <el-icon class="icon"><Plus /></el-icon>
          <div class="skill-btn inline-flex-r-c-n" @click="handleSkillSwitch">
            <a-svg-icon icon-class="skill" size="18px" />
            <div class="label">{{ $t('skill.use') }}</div>
          </div>
          <a-mission :session-id="loadSessionId" @update="handleMissionUpdate" />
        </div>
        <div class="summation inline-flex-r-c-n">
          <el-icon class="icon"><Microphone /></el-icon>
          <el-icon class="icon submit"
            :class="{
              'is-stop': streaming
            }"
            @click="handleSubmitClick"
          >
            <Close v-if="streaming" />
            <Promotion v-else />
          </el-icon>
        </div>
      </div>
    </div>
    <!-- 技能列表弹出层 -->
    <a-skill :visible="isSkillPanelVisible"
      :selected-skills="loadSelectedSkills"
      @select="handleSelectSkill"
      @close="isSkillPanelVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import {
  Plus,
  Close,
  Promotion,
  Microphone
} from '@element-plus/icons-vue'
import ASkill from './promotion/skill.vue'
import AMission from './promotion/mission.vue'

interface SkillItem {
  name: string
  description: string
  always: boolean
  enabled: boolean
}

const handleEmit = defineEmits<{
  (e: 'submit', message: string, display: string, missionId: number): void
  (e: 'cancel'): void
}>()

const props = defineProps<{
  streaming?: boolean
}>()

const route = useRoute()
const loadPromptValue = ref('')
const loadActiveSkillIndex = ref(-1)
const isSkillPanelVisible = ref(false)
const loadSelectedSkills = ref<SkillItem[]>([])
const loadCurrentMissionId = ref(0)
const loadPromotionRef = ref<HTMLTextAreaElement | null>(null)
const loadSessionId = computed(() => route.query.id as string | undefined)

// 暴露 focus 方法给父组件
const focus = () => {
  nextTick(() => {
    loadPromotionRef.value?.focus()
  })
}
// 切换技能面板显隐
const handleSkillSwitch = () => {
  isSkillPanelVisible.value = !isSkillPanelVisible.value
}
// 选择技能
const handleSelectSkill = (skill: SkillItem) => {
  const index = loadSelectedSkills.value.findIndex(s => s.name === skill.name)
  if (index >= 0) {
    loadSelectedSkills.value.splice(index, 1)
  } else {
    loadSelectedSkills.value.push(skill)
  }
  isSkillPanelVisible.value = false
  focus()
}
// 删除已选技能
const handleRemoveSkill = (index: number) => {
  loadSelectedSkills.value.splice(index, 1)
  loadActiveSkillIndex.value = -1
  focus()
}
// 发送消息
const handleMessageSend = () => {
  const text = loadPromptValue.value.trim()
  if (!text && loadSelectedSkills.value.length === 0) return
  const skillPrefix = loadSelectedSkills.value.map(s => `$${s.name}`).join(' ')
  const message = skillPrefix ? `${skillPrefix} ${text}` : text
  if (!message.trim()) return
  handleEmit('submit', message, text, loadCurrentMissionId.value)
  handleDataReset()
}
const handleSubmitClick = () => {
  if (props.streaming) {
    handleEmit('cancel')
    return
  }
  handleMessageSend()
}
const handleDataReset = () => {
  loadPromptValue.value = ''
  loadSelectedSkills.value = []
  isSkillPanelVisible.value = false
}
const handlePromptSubmit = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    if (!props.streaming) {
      handleMessageSend()
    }
  }
}
const handleMissionUpdate = (missionId: number) => {
  loadCurrentMissionId.value = missionId
}

defineExpose({
  focus
})
</script>
