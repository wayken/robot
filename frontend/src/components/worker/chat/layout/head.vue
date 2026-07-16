<template>
  <div class="head inline-flex-r-c-b" v-loading="progression.loading">
    <div class="head--left inline-flex-r-c-n">
      <div class="model inline-flex-r-c-n"
        @click="isModelView = true"
      >
        <img class="icon" v-if="infomation.name"
          :src="loadProviderIcon(infomation.name)"
        />
        <div class="name">{{ infomation.model?.name || infomation.model }}</div>
        <div class="a-model-marker" v-for="prop in (infomation.model?.properties || [])" :key="prop"
          :class="loadModelPropertyClass(prop)"
        >
          <el-icon><component :is="loadModelPropertyIcon(prop)" /></el-icon>
          {{ $t('provider.model-property-' + loadModelPropertyLabel(prop)) }}
        </div>
      </div>
    </div>
    <div class="head--right inline-flex-r-c-n">
      <div class="icon">
        <el-icon><Search /></el-icon>
      </div>
      <div class="icon" @click="handleLayoutNarrow">
        <a-svg-icon v-if="isWindowNarrow" icon-class="expand" />
        <a-svg-icon v-else icon-class="shrink" />
      </div>
    </div>
  </div>
  <a-model-view :visible="isModelView"
    :infomation="infomation"
    @close="isModelView = false"
    @update="handleModelUpdate"
  />
</template>

<script setup lang="ts">
import {
  Search
} from '@element-plus/icons-vue'
import useSocketIO from '@/hooks/useSocketIO'
import AModelView from './model.vue'
import useLocalProfile from '@/hooks/useLocalProfile'
import {
  loadProviderIcon,
  loadModelPropertyLabel,
  loadModelPropertyClass,
  loadModelPropertyIcon
} from '@/config/data/model'

const {
  ioRequest,
  progression
} = useSocketIO()
const router = useRouter()

const infomation = ref<any>({})
const localProfile = useLocalProfile('worker')
const isWindowNarrow = ref(localProfile.get('chat.isWindowNarrow'))
const isModelView = ref(false)

onMounted(() => {
  handleDataLoad()
})

const handleEmit = defineEmits(['narrow'])
const handleDataLoad = () => {
  const params = {
    wid: router.currentRoute.value.params.id
  }
  ioRequest('worker.profile.provider.index', params).then((result) => {
    infomation.value = result[0]
  })
}
const handleModelUpdate = () => {
  handleDataLoad()
}
const handleLayoutNarrow = () => {
  isWindowNarrow.value = !isWindowNarrow.value
  localProfile.set('chat.isWindowNarrow', isWindowNarrow.value)
  handleEmit('narrow', unref(isWindowNarrow))
}
</script>
