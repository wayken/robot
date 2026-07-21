<template>
  <div class="a-setting-toolkit inline-flex-c-n-n" v-loading="progression.loading">
    <!-- 顶部操作栏 -->
    <div class="head inline-flex-r-c-b">
      <div class="title inline-flex-r-c-n">
        <div class="name">{{ $t('toolkit.title') }}</div>
        <div class="count">{{ dataList.length }}</div>
      </div>
      <div class="operations inline-flex-r-c-n">
        <el-tooltip :content="$t('toolkit.view-card')" placement="top">
          <el-icon class="icon"
            :class="{
              'is-actived': loadViewMode === 'card'
            }"
            @click="handleViewModeSwitch('card')"
          >
            <Grid />
          </el-icon>
        </el-tooltip>
        <el-tooltip :content="$t('toolkit.view-table')" placement="top">
          <el-icon class="icon"
            :class="{
              'is-actived': loadViewMode === 'table'
            }"
            @click="handleViewModeSwitch('table')"
          >
            <List />
          </el-icon>
        </el-tooltip>
      </div>
    </div>
    <!-- 卡片视图 -->
    <div class="content" v-if="loadViewMode === 'card'">
      <div class="card">
        <div class="module" v-for="data in dataList" :key="data.name"
          :class="{
            'is-disabled': !data.enabled
          }"
        >
          <div class="header inline-flex-r-c-b">
            <div class="infomation inline-flex-r-c-n">
              <div class="emoji">{{ useToolEmoji(data.name) }}</div>
              <div class="name">{{ $t('toolkit.tool.' + data.name) }}</div>
            </div>
            <div class="operations inline-flex-r-c-n">
              <el-tooltip :content="$t('toolkit.edit-properties')" placement="top" :show-after="300">
                <el-icon class="icon" @click="handleEditProperties(data)">
                  <Edit />
                </el-icon>
              </el-tooltip>
              <el-switch v-model="data.enabled" size="small"
                @change="handleSwitchTool(data, $event)"
              />
            </div>
          </div>
          <div class="body">
            <p class="description">{{ $t('toolkit.tool-desc.' + data.name) }}</p>
          </div>
          <div class="footer inline-flex-r-c-n">
            <el-tag v-if="data.builtin" size="small" type="info">
              {{ $t('toolkit.builtin') }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
    <!-- 表格视图 -->
    <div class="content inline-flex-c-n-n" v-else>
      <div class="table">
        <el-table :data="dataList">
          <el-table-column :label="$t('toolkit.col-name')" min-width="200">
            <template #default="{ row }">
              <div class="infomation inline-flex-r-c-n">
                <div class="emoji">{{ useToolEmoji(row.name) }}</div>
                <div class="name" v-if="row.name">
                  {{ $t('toolkit.tool.' + row.name) }}
                </div>
                <el-tag size="small" type="info" class="builtin">
                  {{ $t('toolkit.builtin') }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="$t('toolkit.col-description')" min-width="300">
            <template #default="{ row }">
              <div class="table-description" v-if="row.name">
                {{ $t('toolkit.tool-desc.' + row.name) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="$t('toolkit.col-status')" width="100" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" size="small"
                @change="handleSwitchTool(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="$t('extension.operation')" width="80" align="center">
            <template #default="{ row }">
              <el-tooltip :content="$t('toolkit.edit-properties')" placement="top" :show-after="300">
                <el-icon class="icon" @click="handleEditProperties(row)">
                  <Edit />
                </el-icon>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    <!-- 编辑环境变量弹窗 -->
    <a-window ref="loadWindowRef" @submit="handleSaveProperties" />
  </div>
</template>

<script setup lang="ts">
import {
  ElMessage
} from 'element-plus'
import {
  Grid,
  List,
  Edit
} from '@element-plus/icons-vue'
import useSocketIO from '@/hooks/useSocketIO'
import useLocalProfile from '@/hooks/useLocalProfile'
import AWindow from '@/components/worker/setting/toolkit/window.vue'

interface ToolItem {
  name: string
  enabled: boolean
  builtin: boolean
  properties: Record<string, any>
}

const i18n = useI18n()
const route = useRoute()
const localProfile = useLocalProfile('toolkit')
const { ioRequest, progression } = useSocketIO()

const dataList = ref<ToolItem[]>([])
const loadViewMode = ref<'card' | 'table'>(localProfile.get('viewMode'))
const loadWindowRef = ref<InstanceType<typeof AWindow> | null>(null)

const useWid = () => route.params.id as string

const loadToolEmojiMap: Record<string, string> = {
  read_file: '📖',
  write_file: '✏️',
  edit_file: '📝',
  remove_file: '🗑️',
  glob: '🔍',
  exec_shell: '💻',
  schedule: '⏰',
  web_search: '🌐',
  session_search: '🔎',
  add_delegate_worker: '👥',
  list_delegate_worker: '📋',
  assign_delegate_worker: '📨'
}

onMounted(() => {
  handleDataLoad()
})

const handleViewModeSwitch = (mode: 'card' | 'table') => {
  loadViewMode.value = mode
  localProfile.set('viewMode', mode)
}
const useToolEmoji = (name: string): string => {
  return loadToolEmojiMap[name] || '🔧'
}
const handleDataLoad = () => {
  const params = {
    wid: useWid()
  }
  ioRequest('toolkit.index', params).then((result: any) => {
    dataList.value = result[0] || []
  })
}
const handleSwitchTool = (data: ToolItem, enabled: string | number | boolean) => {
  ioRequest('toolkit.switch', {
    wid: useWid(),
    name: data.name,
    enabled: enabled
  }).then((result: any) => {
    const success = result[0]
    if (success) {
      ElMessage.success(enabled ? i18n.t('toolkit.enabled-success') : i18n.t('toolkit.disabled-success'))
    } else {
      data.enabled = !enabled
      ElMessage.error(i18n.t('toolkit.switch-failed'))
    }
  }).catch(() => {
    data.enabled = !enabled
  })
}
const handleEditProperties = (data: ToolItem) => {
  loadWindowRef.value?.open(data)
}
const handleSaveProperties = (name: string, properties: Record<string, string>) => {
  loadWindowRef.value?.setSaving(true)
  ioRequest('toolkit.properties.update', {
    wid: useWid(),
    name: name,
    properties: properties
  }).then((result: any) => {
    const success = result[0]
    if (success) {
      ElMessage.success(i18n.t('toolkit.properties-save-success'))
      const tool = dataList.value.find(t => t.name === name)
      if (tool) {
        tool.properties = properties
      }
      loadWindowRef.value?.close()
    } else {
      ElMessage.error(i18n.t('toolkit.properties-save-failed'))
    }
  }).finally(() => {
    loadWindowRef.value?.setSaving(false)
  })
}
</script>
