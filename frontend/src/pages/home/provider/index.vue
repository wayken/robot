<template>
  <div class="a-provider inline-flex-r-n-n" v-loading="progression.loading">
    <!-- 模型厂商列表 -->
    <div class="source inline-flex-c-n-n">
      <div class="wrapper inline-flex-c-n-n">
        <draggable itemKey="name" group="layout" :animation="300" v-model="dataList"
          @change="handleSort"
        >
          <template #item="{ element, index }">
            <div class="provider"
              :class="{
                'is-actived': index === loadActivedMenuIndex
              }"
              @click="handleClick(element, index)"
            >
              <div class="image">
                <img :src="loadProviderIcon(element.name)" />
              </div>
              <div class="name">{{ $t('provider.' + element.name) }}</div>
              <div v-if="element.status == 1" class="on">ON</div>
            </div>
          </template>
        </draggable>
      </div>
      <div class="add inline-flex-r-c-c">
        <a-button :icon="Plus"
          @click="isAddProviderView = true"
        >
          {{ $t('provider.add-provider') }}
        </a-button>
      </div>
    </div>
    <!-- 模型商详情数据 -->
    <div class="context" v-if="loadActivedData">
      <div class="head inline-flex-r-c-b">
        <div class="head--left inline-flex-r-c-n">
          <div class="name">{{ $t('provider.' + loadActivedData.name) }}</div>
          <el-icon><Promotion /></el-icon>
        </div>
        <div class="head--right inline-flex-r-c-n">
          <el-popconfirm v-if="!loadActivedData.is_system" placement="top" :width="218" trigger="click"
            :title="$t('provider.delete-provider-placeholder')"
            @confirm="handleDeleteProvider"
          >
            <template #reference>
              <el-icon><Delete /></el-icon>
            </template>
          </el-popconfirm>
          <el-icon v-if="!loadActivedData.is_system">
            <Edit />
          </el-icon>
          <el-switch size="large" :model-value="loadActivedData.status === 1"
            @change="handleToggleOn"
          />
        </div>
      </div>
      <div class="main">
        <div class="title">
          <div class="name">{{ $t('provider.api-setting') }}</div>
        </div>
        <div class="card">
          <div class="option">
            <div class="key inline-flex-r-c-b">
              <div class="label">{{ $t('provider.app-secret-key') }}</div>
              <div class="link" v-if="loadActivedData.website">
                {{ $t('provider.get-api-key') }}
              </div>
            </div>
            <div class="value">
              <el-input v-model="loadKeysInput" type="password" show-password
                @change="handleUpdate"
              >
                <template #append>
                  <el-icon v-if="isUrlLoading" class="is-loading"><Loading /></el-icon>
                  <el-button v-else @click="isUrlCheckView = true">{{ $t('provider.url-check') }}</el-button>
                </template>
              </el-input>
            </div>
          </div>
          <div class="option">
            <div class="key">{{ $t('provider.api-url') }}</div>
            <div class="value">
              <el-input :model-value="loadActivedData.url" @change="(val: string) => { loadPendingUrl = val; handleUpdate() }"></el-input>
            </div>
            <div class="description">
              {{ loadActivedData.url }}/v1/chat/completions
            </div>
          </div>
        </div>
        <div class="title inline-flex-r-c-b">
          <div class="name">{{ $t('provider.model-setting') }}</div>
          <a-button :icon="Plus" @click="isAddModelView = true">
            {{ $t('provider.add-model') }}
          </a-button>
        </div>
        <div v-if="loadActivedData.models.length === 0" class="wrapper">
          <a-nodata :loading="false" :success="true"></a-nodata>
        </div>
        <div v-for="(data, index) in loadActivedData.models" :key="index" class="model">
          <div class="name">{{ data.name }}</div>
          <div class="children">
            <div v-for="(child, childIndex) in data.model" :key="childIndex" class="child">
              <div class="image">
                <img :src="loadProviderIcon(child.type)" />
              </div>
              <div class="text">{{ child.name }}</div>
              <div class="metadata" v-if="child.properties && child.properties.length">
                <span class="a-model-marker" v-for="prop in child.properties" :key="prop"
                  :class="loadModelPropertyClass(prop)"
                >
                  <el-icon><component :is="loadModelPropertyIcon(prop)" /></el-icon>
                  {{ $t('provider.model-property-' + loadModelPropertyLabel(prop)) }}
                </span>
              </div>
              <div class="operation">
                <el-icon class="icon" @click="handleEditModel(index, childIndex, child, data.name)">
                  <Edit />
                </el-icon>
                <el-popconfirm placement="top" :width="218" trigger="click" style="padding: 12px;"
                  :title="$t('provider.delete-model-placeholder')"
                  @confirm="handleDeleteModel(index, childIndex)"
                >
                  <template #reference>
                    <el-icon class="icon"><Remove /></el-icon>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <!-- 添加模型弹窗 -->
  <a-window-model v-if="loadActivedData" :provider="loadActivedData.name"
    :visible="isAddModelView"
    :addition="true"
    @add="handleAddModel"
    @close="isAddModelView = false"
  />
  <!-- 添加提供商弹窗 -->
  <a-window-provider :visible="isAddProviderView"
    @add="handleAddProvider"
    @close="isAddProviderView = false"
  />
  <!-- 编辑模型弹窗 -->
  <a-window-model v-if="loadActivedData" :provider="loadActivedData.name"
    :visible="isEditModelView"
    :addition="false"
    :infomation="loadEditModelData"
    @update="handleUpdateModelSubmit"
    @close="isEditModelView = false"
  />
</template>

<script setup lang="ts">
import {
  Edit,
  Plus,
  Delete,
  Remove,
  Loading,
  Promotion
} from '@element-plus/icons-vue'
import Draggable from 'vuedraggable'
import { useRequest } from '@/hooks/useRequest'
import { loadProviderIcon, loadModelPropertyLabel, loadModelPropertyClass, loadModelPropertyIcon } from '@/config/data/model'
import AWindowModel from '@/components/home/provider/window-model.vue'
import AWindowProvider from '@/components/home/provider/window-provider.vue'

const dataList = ref<any[]>([])
const loadActivedMenuIndex = ref(0)
const isUrlLoading = ref(false)
const isUrlCheckView = ref(false)
const isAddModelView = ref(false)
const isAddProviderView = ref(false)
const isEditModelView = ref(false)
const loadEditModelData = ref<any>(null)
const loadPendingUrl = ref('')
const loadKeysInput = ref('')

const loadActivedData = computed(() => {
  if (!dataList.value.length) {
    return null
  }
  return unref(dataList)[loadActivedMenuIndex.value]
})
watch(loadActivedData, (data) => {
  if (!data || !data.api_keys) {
    loadKeysInput.value = ''
    return
  }
  const keys = Array.isArray(data.api_keys) ? data.api_keys : []
  loadKeysInput.value = keys.join(',')
}, { immediate: true })

const { ioload, iopost, progression } = useRequest()
onMounted(() => {
  handleLoad()
})

const handleLoad = () => {
  ioload('provider', 'loadProviderList', null).then((result) => {
    dataList.value = result
  })
}
const handleClick = (data: any, index: number) => {
  loadActivedMenuIndex.value = index
}
const handleSort = (event: any) => {
  loadActivedMenuIndex.value = event.moved.newIndex
}
const handleToggleOn = (value: boolean) => {
  const data = loadActivedData.value
  if (!data) return
  iopost('provider', 'xhrUpdateProvider', {
    id: data.id,
    status: value ? 1 : 0
  }).then(() => {
    data.status = value ? 1 : 0
  })
}
const handleUpdate = () => {
  const data = loadActivedData.value
  if (!data) return
  const params: any = { id: data.id }
  if (loadPendingUrl.value !== '') params.url = loadPendingUrl.value
  const keysArr = loadKeysInput.value.split(',').map((k: string) => k.trim()).filter(Boolean)
  params.keys = keysArr
  iopost('provider', 'xhrUpdateProvider', params).then(() => {
    if (loadPendingUrl.value !== '') data.url = loadPendingUrl.value
    data.api_keys = keysArr
    loadPendingUrl.value = ''
  })
}
const handleAddProvider = (formData: any) => {
  iopost('provider', 'xhrAddProvider', {
    name: formData.name,
    type: formData.type,
    url: ''
  }).then(() => {
    handleLoad()
  })
}
const handleDeleteProvider = () => {
  const data = loadActivedData.value
  if (!data || data.system) return
  iopost('provider', 'xhrDeleteProvider', {
    id: data.id
  }).then(() => {
    loadActivedMenuIndex.value = 0
    handleLoad()
  })
}
const handleAddModel = (formData: any) => {
  const data = loadActivedData.value
  if (!data) return
  const newModels = JSON.parse(JSON.stringify(data.models))
  let group = newModels.find((m: any) => m.name === formData.group)
  if (!group) {
    group = { name: formData.group, model: [] }
    newModels.push(group)
  }
  group.model.push({ type: formData.type, name: formData.name, properties: formData.properties || [] })
  iopost('provider', 'xhrUpdateProvider', {
    id: data.id,
    models: newModels
  }).then(() => {
    data.models = newModels
  })
}
const handleDeleteModel = (groupIndex: number | string, modelIndex: number | string) => {
  const data = loadActivedData.value
  if (!data) return
  const newModels = JSON.parse(JSON.stringify(data.models))
  newModels[groupIndex].model.splice(modelIndex, 1)
  if (newModels[groupIndex].model.length === 0) {
    newModels.splice(groupIndex, 1)
  }
  iopost('provider', 'xhrUpdateProvider', {
    id: data.id,
    models: newModels
  }).then(() => {
    data.models = newModels
  })
}
const handleEditModel = (groupIndex: number, modelIndex: number, child: any, groupName: string) => {
  loadEditModelData.value = {
    name: child.name,
    type: child.type,
    group: groupName,
    properties: child.properties || [],
    groupIndex,
    modelIndex
  }
  isEditModelView.value = true
}
const handleUpdateModelSubmit = (formData: any) => {
  const data = loadActivedData.value
  if (!data || !loadEditModelData.value) return
  const { groupIndex, modelIndex } = loadEditModelData.value
  const newModels = JSON.parse(JSON.stringify(data.models))
  const oldGroup = newModels[groupIndex]
  const updatedModel = { type: formData.type, name: formData.name, properties: formData.properties || [] }
  // 如果分组名变了，需要从旧分组移除并添加到新分组
  if (oldGroup.name === formData.group) {
    oldGroup.model[modelIndex] = updatedModel
  } else {
    oldGroup.model.splice(modelIndex, 1)
    if (oldGroup.model.length === 0) {
      newModels.splice(groupIndex, 1)
    }
    let targetGroup = newModels.find((m: any) => m.name === formData.group)
    if (!targetGroup) {
      targetGroup = { name: formData.group, model: [] }
      newModels.push(targetGroup)
    }
    targetGroup.model.push(updatedModel)
  }
  iopost('provider', 'xhrUpdateProvider', {
    id: data.id,
    models: newModels
  }).then(() => {
    data.models = newModels
  })
}
</script>
