<template>
  <div class="a-node inline-flex-c-n-n">
    <!-- 顶部操作栏 -->
    <div class="head inline-flex-r-c-b">
      <div class="head--left inline-flex-r-c-n">
        <el-input v-model="loadSearchKeyword" :placeholder="$t('node.search-placeholder')" clearable style="width: 320px;">
          <template #prepend>
            <el-button :icon="Search" />
          </template>
          <template #append>
            <el-button>{{ $t('common.search') }}</el-button>
          </template>
        </el-input>
        <el-select v-model="loadFilterStatus" :placeholder="$t('node.filter-status')" style="width: 180px; margin-left: 12px;">
          <el-option :label="$t('node.status-all')" value="" />
          <el-option :label="$t('node.status-online')" :value="1" />
          <el-option :label="$t('node.status-offline')" :value="0" />
          <el-option :label="$t('node.status-error')" :value="2" />
        </el-select>
      </div>
      <div class="head--right inline-flex-r-c-n">
        <div class="view inline-flex-r-c-n">
          <el-tooltip :content="$t('node.view-card')" placement="top">
            <div class="icon inline-flex-r-c-c"
              :class="{
                'is-actived': loadViewMode === 'card'
              }"
              @click="handleViewModeSwitch('card')"
            >
              <el-icon><Grid /></el-icon>
            </div>
          </el-tooltip>
          <el-tooltip :content="$t('node.view-list')" placement="top">
            <div class="icon inline-flex-r-c-c"
              :class="{
                'is-actived': loadViewMode === 'table'
              }"
              @click="handleViewModeSwitch('table')"
            >
              <el-icon><Memo /></el-icon>
            </div>
          </el-tooltip>
        </div>
        <a-divider direction="vertical" :height="24" />
        <a-button :icon="Plus" @click="handleAdd">
          {{ $t('node.add-node') }}
        </a-button>
        <a-button type="success" :icon="Refresh">
          {{ $t('common.refresh') }}
        </a-button>
      </div>
    </div>
    <!-- 统计概览 -->
    <div class="status inline-flex-r-c-n" v-loading="progression.loading">
      <div class="board is-total">
        <div class="value">{{ loadStatus.total }}</div>
        <div class="label">{{ $t('node.stat-total') }}</div>
      </div>
      <div class="divider" />
      <div class="board is-online">
        <div class="value">{{ loadStatus.online }}</div>
        <div class="label">{{ $t('node.stat-online') }}</div>
      </div>
      <div class="divider" />
      <div class="board is-offline">
        <div class="value">{{ loadStatus.offline }}</div>
        <div class="label">{{ $t('node.stat-offline') }}</div>
      </div>
      <div class="divider" />
      <div class="board is-error">
        <div class="value">{{ loadStatus.error }}</div>
        <div class="label">{{ $t('node.stat-error') }}</div>
      </div>
    </div>
    <!-- 节点列表 -->
    <div class="content inline-flex-c-n-n">
      <!-- 卡片视图 -->
      <div class="card" v-if="loadViewMode === 'card'">
        <div v-for="(data, index) in loadFilteredList" :key="index" class="node"
          :class="'is-status-' + data.status"
        >
          <div class="identity inline-flex-r-c-n">
            <div class="dot" :class="loadDotStatusClass(data.status)" />
            <div class="name inline-text-ellipsis">{{ data.name }}</div>
          </div>
          <div class="address inline-flex-r-c-n">
            <el-icon class="icon"><Monitor /></el-icon>
            <span class="name">{{ data.address }}:{{ data.port }}</span>
          </div>
          <div class="metadata inline-flex-r-c-n">
            <div class="label inline-flex-r-c-n" v-if="data.status === 1">
              <el-icon ><Check /></el-icon>
              {{ $t('node.stat-online') }}
            </div>
            <div class="label inline-flex-r-c-n" v-if="data.status === 0">
              <el-icon><WarnTriangleFilled /></el-icon>
              {{ $t('node.stat-offline') }}
            </div>
            <div class="label inline-flex-r-c-n" v-if="data.status === 2">
              <el-icon><Close /></el-icon>
              {{ $t('node.stat-error') }}
            </div>
            <div class="uptime" v-if="data.status === 1">{{ datetimeftfn(data.uptime) }}</div>
          </div>
          <div class="system inline-flex-r-c-b">
            <div class="info inline-flex-r-c-n">
              <a-svg-icon icon-class="disk" size="15px" />
              <span>{{ data.os }}</span>
            </div>
            <div class="info inline-flex-r-c-n">
              <a-svg-icon icon-class="model" size="15px" />
              <span>{{ data.assistants || 0 }} {{ $t('node.assistant') }}</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 列表视图 -->
      <div class="table" v-if="loadViewMode === 'table'">
        <el-table :data="loadFilteredList">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="infomation">
                <div class="field inline-flex-r-c-n">
                  <div class="label"><el-icon class="icon"><MessageBox /></el-icon>{{ $t('node.hostname') }}：</div>
                  <div class="value">{{ row.hostname }}</div>
                </div>
                <div class="field inline-flex-r-c-n">
                  <div class="label"><el-icon class="icon"><Promotion /></el-icon>{{ $t('node.version') }}：</div>
                  <div class="value">{{ row.version }}</div>
                </div>
                <div class="field inline-flex-r-c-n">
                  <div class="label"><el-icon class="icon"><Clock /></el-icon>{{ $t('node.boot-time') }}：</div>
                  <div class="value">{{ datetimeftfn(row.uptime) }}</div>
                </div>
                <div class="field inline-flex-r-c-n">
                  <div class="label"><el-icon class="icon"><Location /></el-icon>{{ $t('node.region') }}：</div>
                  <div class="value">{{ row.region }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column width="48">
            <template #default="{ $index }">
              <div class="index">{{ $index + 1 }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="name" :label="$t('node.name')" min-width="160">
            <template #default="{ row }">
              <div class="identity inline-flex-r-c-n">
                <div class="dot" :class="loadDotStatusClass(row.status)" />
                <span class="name inline-text-ellipsis">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="$t('node.address')" width="200">
            <template #default="{ row }">
              <div class="address inline-flex-r-c-n">
                <span class="mono">{{ row.address }}:{{ row.port }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('extension.status')" width="140">
            <template #default="{ row }">
              <div class="metadata inline-flex-r-c-n" :class="'is-status-' + row.status">
                <div class="label inline-flex-r-c-n" v-if="row.status === 1">
                  <el-icon ><Check /></el-icon>
                  {{ $t('node.stat-online') }}
                </div>
                <div class="label inline-flex-r-c-n" v-if="row.status === 0">
                  <el-icon><WarnTriangleFilled /></el-icon>
                  {{ $t('node.stat-offline') }}
                </div>
                <div class="label inline-flex-r-c-n" v-if="row.status === 2">
                  <el-icon><Close /></el-icon>
                  {{ $t('node.stat-error') }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="$t('node.os')" min-width="180">
            <template #default="{ row }">
              <div class="os inline-flex-r-c-n">
                <a-svg-icon class="icon" icon-class="os-window" size="22px" />
                <span>{{ row.os || $t('node.os-unknown') }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="$t('node.assistant')" width="100" align="center">
            <template #default="{ row }">
              {{ row.assistants || 0 }}
            </template>
          </el-table-column>
          <el-table-column fixed="right" :label="$t('extension.operation')" width="260">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">
                {{ $t('common.edit') }}
              </el-button>
              <el-button v-if="row.status === 0" link type="success" :icon="Check">
                {{ $t('extension.enable') }}
              </el-button>
              <el-button v-if="row.status === 1" link type="danger" :icon="WarnTriangleFilled">
                {{ $t('extension.disable') }}
              </el-button>
              <el-popconfirm :title="$t('extension.delete-item-tips')" width="258"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button link type="danger" :icon="Delete">{{ $t('common.delete') }}</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <a-nodata v-if="loadFilteredList.length === 0"
        :loading="progression.loading"
        :success="progression.success"
      >
        <a-button :icon="Plus" @click="handleAdd">
          {{ $t('node.add-node') }}
        </a-button>
      </a-nodata>
    </div>
  </div>
  <a-node-window
    :visible="isWindowVisible"
    :addition="isWindowAddition"
    :infomation="windowInfomation"
    @close="isWindowVisible = false"
    @update="handleWindowUpdate"
  />
</template>

<script setup lang="ts">
import {
  Memo,
  Grid,
  Plus,
  Edit,
  Close,
  Check,
  Clock,
  Delete,
  Search,
  Refresh,
  Monitor,
  MessageBox,
  Location,
  Promotion,
  WarnTriangleFilled
} from '@element-plus/icons-vue'
import { useRequest } from '@/hooks/useRequest'
import { datetimeftfn } from '@/hooks/useDataFormatter'
import useLocalProfile from '@/hooks/useLocalProfile'
import ANodeWindow from '@/components/home/node/window.vue'

const dataList = ref<any[]>([])
const localProfile = useLocalProfile('node')
const loadSearchKeyword = ref('')
const loadFilterStatus = ref<number | ''>('')
const loadViewMode = ref(localProfile.get('viewMode'))
const isWindowVisible = ref(false)
const isWindowAddition = ref(true)
const windowInfomation = ref<any>({})

const { ioload, iopost, progression } = useRequest()

const handleDataLoad = () => {
  ioload('node', 'loadNodeList', null).then((result) => {
    dataList.value = result
  })
}

onMounted(() => {
  handleDataLoad()
})

const handleAdd = () => {
  isWindowAddition.value = true
  windowInfomation.value = {}
  isWindowVisible.value = true
}
const handleEdit = (row: any) => {
  isWindowAddition.value = false
  windowInfomation.value = { ...row }
  isWindowVisible.value = true
}
const handleWindowUpdate = () => {
  isWindowVisible.value = false
  handleDataLoad()
}
const handleDelete = (row: any) => {
  iopost('node', 'xhrDeleteNode', { id: row.id }, {
    onMessage: true
  }).then(() => {
    handleDataLoad()
  })
}
const loadStatus = computed(() => ({
  total: dataList.value.length,
  offline: dataList.value.filter(n => n.status === 0).length,
  online: dataList.value.filter(n => n.status === 1).length,
  error: dataList.value.filter(n => n.status === 2).length
}))
const loadFilteredList = computed(() => {
  return dataList.value.filter(node => {
    const matchKeyword = !loadSearchKeyword.value ||
      node.name.includes(loadSearchKeyword.value) ||
      node.address.includes(loadSearchKeyword.value)
    const matchStatus = loadFilterStatus.value === '' || node.status === loadFilterStatus.value
    return matchKeyword && matchStatus
  })
})
const loadDotStatusClass = (status: number) => ({
  'is-dot-online': status === 1,
  'is-dot-offline': status === 0,
  'is-dot-failure': status === 2
})
const handleViewModeSwitch = (value: string) => {
  loadViewMode.value = value
  localProfile.set('viewMode', value)
}
</script>
