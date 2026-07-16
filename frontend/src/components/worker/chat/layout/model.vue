<template>
  <el-dialog draggable width="820px" top="4vh" class="a-worker-chat-model" :title="$t('provider.model-setting')"
    :model-value="visible"
    @open="handleOpen"
    @close="handleClose"
  >
    <div class="main" v-loading="progression.loading">
      <div class="field">
        <div class="key">{{ $t('chat.main-model') }}</div>
        <div class="label inline-flex-r-c-n">
          <div class="icon">
            <img :src="resolveProviderIcon(loadFormData.model)" />
          </div>
          <el-select v-model="loadFormData.model" filterable style="width: 528px;" :teleported="false">
            <el-option-group v-for="provider in loadProviderList" :key="provider.name"
              :label="$t('provider.' + provider.name)"
            >
              <el-option v-for="data in provider.models" :key="data.name" :label="data.name"
                :value="provider.name + ':' + data.name"
              >
                <div class="module inline-flex-r-c-n">
                  <div class="image">
                    <img :src="loadProviderIcon(data.type)" />
                  </div>
                  <div class="name">{{ data.name }}</div>
                  <div class="a-model-marker" v-for="prop in (data.properties || [])" :key="prop"
                    :class="loadModelPropertyClass(prop)"
                  >
                  <el-icon><component :is="loadModelPropertyIcon(prop)" /></el-icon>
                  {{ $t('provider.model-property-' + loadModelPropertyLabel(prop)) }}
                  </div>
                </div>
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </div>
      <div class="field is-flexable backup">
        <div class="key">
          <span>{{ $t('chat.backup-model') }}</span>
          <el-button type="primary" link size="large"
            @click="handleBackupAdd"
          >
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <div class="models">
          <div class="provider" v-for="(data, index) in loadFormData.backups" :key="index">
            <div class="icon">
              <img :src="resolveProviderIcon(data.model)" />
            </div>
            <el-select v-model="data.model" filterable :placeholder="$t('chat.select-backup-model-placeholder')" :teleported="false">
              <el-option-group v-for="provider in loadProviderList" :key="provider.name"
                :label="$t('provider.' + provider.name)"
              >
                <el-option v-for="data in provider.models" :key="data.name" :label="data.name"
                  :value="provider.name + ':' + data.name"
                >
                  <div class="module inline-flex-r-c-n">
                    <div class="image">
                      <img :src="loadProviderIcon(data.type)" />
                    </div>
                    <div class="name">{{ data.name }}</div>
                    <div class="a-model-marker" v-for="prop in (data.properties || [])" :key="prop"
                      :class="loadModelPropertyClass(prop)"
                    >
                      <el-icon><component :is="loadModelPropertyIcon(prop)" /></el-icon>
                      {{ $t('provider.model-property-' + loadModelPropertyLabel(prop)) }}
                    </div>
                  </div>
                </el-option>
              </el-option-group>
            </el-select>
            <el-button type="danger" link @click="handleBackupRemove(index)">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      <div class="field is-column">
        <div class="key">{{ $t('chat.temperature') }}</div>
        <div class="value">
          <el-slider v-model="loadFormData.temperature" show-input :step="0.01" :min="0" :max="2"
            :marks="{ 0: '0', 0.7: '0.7', 2: '2' }"
          />
        </div>
      </div>
      <div class="field is-column">
        <div class="key">{{ $t('chat.topP') }}</div>
        <div class="value">
          <el-slider v-model="loadFormData.topP" show-input :step="0.01" :min="0" :max="1"
            :marks="{ 0: '0', 1: '1' }"
          />
        </div>
      </div>
      <div class="field is-column">
        <div class="key">{{ $t('chat.context-num') }}</div>
        <div class="value">
          <el-slider v-model="loadFormData.contextNum" show-input :step="1" :min="0" :max="20"
            :marks="{ 0: '0', 5: '5', 10: '10', 15: '15', 20: '20' }"
          />
        </div>
      </div>
      <div class="field">
        <div class="key">{{ $t('chat.max-token-limit') }}</div>
        <div class="value">
          <el-switch v-model="loadFormData.tokenLimit" />
        </div>
      </div>
      <div class="field">
        <div class="key">{{ $t('chat.is-stream') }}</div>
        <div class="value">
          <el-switch v-model="loadFormData.stream" />
        </div>
      </div>
      <div class="field">
        <div class="key">{{ $t('chat.reasoning-effort') }}</div>
        <div class="value">
          <el-radio-group v-model="loadFormData.effort">
            <el-radio-button :label="$t('chat.low')" value="low" />
            <el-radio-button :label="$t('chat.medium')" value="medium" />
            <el-radio-button :label="$t('chat.high')" value="high" />
            <el-radio-button :label="$t('chat.off')" value="off" />
          </el-radio-group>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="footer">
        <el-button @click="handleClose">
          {{ $t('common.cancel') }}
        </el-button>
        <el-button type="primary" @click="handleSubmit">
          {{ $t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  Plus,
  Close
} from '@element-plus/icons-vue'
import {
  loadProviderIcon,
  loadModelPropertyLabel,
  loadModelPropertyClass,
  loadModelPropertyIcon
} from '@/config/data/model'
import { useDeepClone } from '@/utils/dom'
import useSocketIO from '@/hooks/useSocketIO'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  infomation: {
    type: Object,
    default: () => ({})
  }
})

const {
  ioRequest,
  progression
} = useSocketIO()
const router = useRouter()
const loadFormData = ref<any>({})
const loadProviderList = ref<any[]>([])

const resolveProviderIcon = (value: string) => {
  if (!value) return loadProviderIcon('')
  const providerName = value.split(':')[0]
  const matched = loadProviderList.value.find((p: any) => p.name === providerName)
  return loadProviderIcon(matched?.name || '')
}
const handleEmit = defineEmits(['close', 'update'])
const handleOpen = () => {
  const data = useDeepClone(props.infomation)
  const modelName = data.model?.name || data.model
  data.model = data.name + ':' + modelName
  if (!data.backups) {
    data.backups = []
  }
  loadFormData.value = data
  const params = {
    wid: router.currentRoute.value.params.id
  }
  ioRequest('management.provider.index', params).then((result) => {
    loadProviderList.value = result[0]
  })
}
const handleClose = () => {
  handleEmit('close')
}
const handleBackupAdd = () => {
  loadFormData.value.backups.push({ model: '' })
}
const handleBackupRemove = (index: number) => {
  loadFormData.value.backups.splice(index, 1)
}
const handleProviderBuild = (value: string, primary: boolean) => {
  const [providerName, ...modelParts] = value.split(':')
  const modelName = modelParts.join(':')
  const matched = loadProviderList.value.find((p: any) => p.name === providerName)
  // 从provider模型列表中查找对应模型的properties
  let modelProperties: number[] = []
  if (matched?.models) {
    const matchedModel = matched.models.find((m: any) => m.name === modelName)
    if (matchedModel?.properties) {
      modelProperties = matchedModel.properties
    }
  }
  return {
    name: providerName,
    type: matched?.type || 'openai',
    primary: primary,
    link: matched?.link || '',
    model: { name: modelName, properties: modelProperties },
    temperature: loadFormData.value.temperature,
    topP: loadFormData.value.topP,
    stream: loadFormData.value.stream,
    timeout: 60
  }
}
const handleSubmit = () => {
  const provider: any[] = []
  if (loadFormData.value.model) {
    provider.push(handleProviderBuild(loadFormData.value.model, true))
  }
  for (const backup of loadFormData.value.backups) {
    if (backup.model) {
      provider.push(handleProviderBuild(backup.model, false))
    }
  }
  const params = {
    wid: router.currentRoute.value.params.id,
    provider: provider
  }
  ioRequest('worker.profile.provider.update', params).then(() => {
    handleEmit('update', loadFormData.value)
    handleClose()
  })
}
</script>
