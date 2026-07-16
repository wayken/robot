<template>
  <div class="sidebar inline-flex-c-n-n">
    <div class="head inline-flex-r-c-b">
      <div class="name">{{ $t('rules.title') }}</div>
      <el-icon class="icon" @click="handleDocumentAdd">
        <Plus />
      </el-icon>
    </div>
    <div class="content">
      <div class="document inline-flex-r-c-b" v-for="(data, index) in dataList" :key="index"
        :class="{
          'is-disabled': !data.enabled,
          'is-actived': currentFile?.filename === data.filename
        }"
        @click="handleDocumentSelect(data)"
      >
        <div class="infomation inline-flex-r-c-n">
          <el-icon class="icon"><Tickets /></el-icon>
          <span class="name">{{ data.filename }}</span>
        </div>
        <div class="metadata inline-flex-r-c-n">
          <el-switch v-if="!data.core" size="small"
            :model-value="data.enabled"
            @click.stop
            @change="handleDocumentSwitch(data, $event)"
          />
          <div v-if="data.core" class="mark">
            {{ $t('rules.core') }}
          </div>
        </div>
      </div>
      <a-nodata v-if="dataList.length === 0" :loading="loading" :success="success" />
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Plus,
  Tickets
} from '@element-plus/icons-vue'

interface RuleFile {
  core: boolean
  size: number
  date: number
  filename: string
  enabled: boolean
}

defineProps<{
  dataList: RuleFile[]
  currentFile: RuleFile | null
  loading: boolean
  success: boolean
}>()

const handleEmit = defineEmits<{
  (e: 'add'): void
  (e: 'select', file: RuleFile): void
  (e: 'switch', file: RuleFile, enabled: boolean): void
}>()

const handleDocumentAdd = () => {
  handleEmit('add')
}
const handleDocumentSelect = (data: RuleFile) => {
  handleEmit('select', data)
}
const handleDocumentSwitch = (data: RuleFile, value: boolean) => {
  handleEmit('switch', data, value)
}
</script>
