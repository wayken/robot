<template>
  <div class="a-assistant inline-flex-c-n-n">
    <div class="head inline-flex-r-c-b">
      <div class="head--left inline-flex-r-c-n">
        <el-input>
          <template #prepend>
            <el-button :icon="Search" />
          </template>
          <template #append>
            <el-button>
              {{ $t('common.search') }}
            </el-button>
          </template>
        </el-input>
      </div>
      <div class="head--right">
        <a-button :icon="Plus" @click="handleAdd">
          {{ $t('assistant.add-assistant') }}
        </a-button>
        <a-button type="success" :icon="Download">
          {{ $t('assistant.import-assistant') }}
        </a-button>
      </div>
    </div>
    <div class="content inline-flex-c-n-n">
      <div class="table">
        <el-table :data="dataList">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="infomation">
                <div class="field inline-flex-r-c-n">
                  <div class="label">
                    <el-icon class="icon"><Box /></el-icon>{{ $t('assistant.model') }}：
                  </div>
                  <div class="value">{{ row.model }}</div>
                </div>
                <div class="field inline-flex-r-c-n">
                  <div class="label">
                    <el-icon class="icon"><Files /></el-icon>{{ $t('assistant.node') }}：
                  </div>
                  <div class="value">{{ row.node }}</div>
                </div>
                <div class="field inline-flex-r-c-n">
                  <div class="label">
                    <el-icon class="icon"><Monitor /></el-icon>{{ $t('assistant.workspace') }}：
                  </div>
                  <div class="value">{{ row.workspace }}</div>
                </div>
                <div class="field inline-flex-r-c-n">
                  <div class="label">
                    <el-icon class="icon"><Box /></el-icon>{{ $t('assistant.date') }}：
                  </div>
                  <div class="value">{{ datetimeftfn(row.date) }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column width="48">
            <template #default="{ $index }">
              <div class="index">{{ $index + 1 }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="id" :label="$t('assistant.id')" width="220">
            <template #default="{ row }">
              <div class="id inline-pointer inline-text-ellipsis"
                @click.stop="handleWindowOpen(row)"
              >{{ row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="name" :label="$t('assistant.name')" width="auto" min-width="220">
            <template #default="{ row }">
              <div class="name inline-pointer inline-flex-r-c-n"
                @click.stop="handleWindowOpen(row)"
              >
                <el-avatar class="image" hape="square" :icon="UserFilled" fit="fill" :src="row.image" />
                <div class="text">{{ row.name || '-' }}</div>
                <div class="promotion">
                  <el-tooltip effect="dark" placement="top"
                    :content="$t('extension.openin-new-window')"
                  >
                    <el-icon @click.stop="handleWindowOpen(row)"><Link /></el-icon>
                  </el-tooltip>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="mode" :label="$t('assistant.mode')" width="280">
            <template #default="{ row }">
              <div class="mode inline-flex-r-c-n" v-if="row.mode === 0">
                <el-icon class="icon"><Sunrise /></el-icon>
                {{ $t('assistant.mode-standard') }}
              </div>
              <div class="mode inline-flex-r-c-n" v-else>
                <el-icon class="icon"><Promotion /></el-icon>
                {{ $t('assistant.mode-agent') }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('extension.status')" width="180">
            <template #default="{ row }">
              <div class="status is-actived" v-if="row.status === 1">
                <el-icon><Check /></el-icon>
                <div class="name">{{ $t('assistant.status-running') }}</div>
              </div>
              <div class="status is-holded" v-if="row.status === 0">
                <el-icon><WarnTriangleFilled /></el-icon>
                <div class="name">{{ $t('assistant.status-offline') }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column fixed="right" :label="$t('extension.operation')" width="320">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click.stop="handleEdit(row)">
                {{ $t('common.edit') }}
              </el-button>
              <el-button v-if="row.status === 0" link type="success" :icon="Check"
                @click.stop="handleStatusUpdate(row, 1)"
              >
                {{ $t('extension.enable') }}
              </el-button>
              <el-button v-if="row.status === 1" link type="danger" :icon="WarnTriangleFilled"
                @click.stop="handleStatusUpdate(row, 0)"
              >
                {{ $t('extension.disable') }}
              </el-button>
              <el-popconfirm :title="$t('extension.delete-item-tips')" width="258"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button link type="danger" :icon="Delete">
                    {{ $t('common.delete') }}
                  </el-button>
                </template>
              </el-popconfirm>
              <el-button link type="primary" :icon="Download">
                {{ $t('extension.export') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <a-nodata v-if="dataList.length === 0"
        :loading="progression.loading"
        :success="progression.success"
      />
    </div>
  </div>
  <!-- 添加 / 编辑智能体弹窗 -->
  <AssistantWindow
    :visible="loadWindowState.visible"
    :addition="loadWindowState.addition"
    :infomation="loadWindowState.infomation"
    :submitting="progression.sending"
    @close="handleWindowClose"
    @update="handleWindowUpdate"
  />
</template>

<script setup lang="ts">
import {
  Box,
  Edit,
  Plus,
  Link,
  Files,
  Check,
  Monitor,
  Delete,
  Search,
  Sunrise,
  Download,
  Promotion,
  UserFilled,
  WarnTriangleFilled
} from '@element-plus/icons-vue'
import runtime from '@/platform/runtime'
import { useRequest } from '@/hooks/useRequest'
import { datetimeftfn } from '@/hooks/useDataFormatter'
import AssistantWindow from '@/components/home/assistant/window.vue'

const dataList = ref<any[]>([])

const router = useRouter()
const { ioload, iopost, progression } = useRequest()

// 弹窗状态
const loadWindowState = reactive({
  visible: false,
  addition: true,
  infomation: {} as any
})

onMounted(() => {
  handleDataLoad()
})

const handleDataLoad = async () => {
  ioload('assistant', 'loadAssistantList', null).then((result) => {
    dataList.value = result
  })
}
const handleAdd = () => {
  loadWindowState.addition = true
  loadWindowState.infomation = {}
  loadWindowState.visible = true
}
const handleEdit = (data: any) => {
  loadWindowState.addition = false
  loadWindowState.infomation = {
    ...data,
    nodeId: data.node_id
  }
  loadWindowState.visible = true
}
const handleWindowClose = () => {
  loadWindowState.visible = false
}
const handleWindowUpdate = () => {
  loadWindowState.visible = false
  handleDataLoad()
}
const handleWindowOpen = (data: any) => {
  const path = `/worker/${data.id}`
  if (runtime.isApplication()) {
    runtime.handleOpenNewTabWindow({
      id: String(data.id),
      path: path,
      name: data.name
    })
  } else {
    let routeData = router.resolve({
      path: path
    })
    window.open(routeData.href, '_blank')
  }
}
const handleStatusUpdate = (data: any, status: number) => {
  iopost('assistant', 'xhrUpdateAssistantStatus', { id: data.id, status }, {
    onMessage: true
  }).then(() => {
    handleDataLoad()
  })
}
const handleDelete = (data: any) => {
  iopost('assistant', 'xhrDeleteAssistant', { id: data.id }, {
    onMessage: true
  }).then(() => {
    handleDataLoad()
  })
}
</script>
