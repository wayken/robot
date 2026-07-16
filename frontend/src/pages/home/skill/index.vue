<template>
  <div class="a-skill inline-flex-c-n-n">
    <div class="title">{{ $t('menu.skill') }}</div>
    <div class="header inline-flex-r-c-b">
      <div class="header--left">
        <el-segmented v-model="loadActivedMenu" :options="loadMenuList" size="large"
          @change="handleMenuChange"
        >
          <template #default="scope">
            <div class="inline-flex-r-c-c menu">
              <el-icon>
                <component :is="scope.item.icon" />
              </el-icon>
              <div>{{ $t('skill.' + scope.item.name) }}</div>
            </div>
          </template>
        </el-segmented>
      </div>
      <div class="header--right inline-flex-r-c-n">
        <el-input :prefix-icon="Search" class="search is-round"
          :placeholder="$t('skill.search-skill')"
        ></el-input>
        <a-button :icon="Promotion">
          {{ $t('skill.install-skill') }}
        </a-button>
        <el-tooltip effect="dark" :content="$t('skill.viewport')">
          <el-icon class="icon" v-if="!isGridView"
            @click="handleGridViewSwitch"
          ><Grid /></el-icon>
          <el-icon class="icon" v-else
            @click="handleGridViewSwitch"
          ><Memo /></el-icon>
        </el-tooltip>
        <el-icon class="icon"><Refresh /></el-icon>
      </div>
    </div>
    <div class="content">
      <!-- 卡片视图 -->
      <div class="card" v-if="isGridView">
        <el-row :gutter="12">
          <el-col :xl="6" :lg="8" :md="12" :sm="12" :xs="24" v-for="(data, index) in dataList" :key="index">
            <div class="skill">
              <div class="head inline-flex-r-c-n">
                <el-image class="image" :src="data.image" fit="cover"></el-image>
                <div class="name">{{ data.name }}</div>
                <el-switch class="switch" v-model="data.enabled" />
              </div>
              <div class="metadata">
                {{ data.description }}
              </div>
              <div class="version">V {{ data.version }}</div>
            </div>
          </el-col>
        </el-row>
      </div>
      <!-- 列表视图 -->
      <div class="list" v-else>
        <div class="skill inline-flex-r-c-n" v-for="(data, index) in dataList" :key="index">
          <el-image class="image" :src="data.image" fit="cover"></el-image>
          <div class="metadata">
            <div class="name">{{ data.name }}</div>
            <div class="description">{{ data.description }}</div>
          </div>
          <div class="version">V {{ data.version }}</div>
          <el-switch v-model="data.enabled" />
        </div>
      </div>
      <a-nodata v-if="dataList.length === 0" :loading="progression.loading" :success="progression.success" />
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Grid,
  Memo,
  Search,
  Refresh,
  Promotion,
  Location,
  ChromeFilled
} from '@element-plus/icons-vue'
import { useRequest } from '@/hooks/useRequest'

const isGridView = ref(false)
const dataList = ref<any[]>([])
const loadActivedMenu = ref('installed')
const loadMenuList = [
  {
    name: 'installed',
    value: 'installed',
    icon: Location
  },
  {
    name: 'market',
    value: 'market',
    icon: ChromeFilled
  }
]

const { ioload, progression } = useRequest()

onMounted(() => {
  handleMenuChange(unref(loadActivedMenu))
})

const handleGridViewSwitch = () => {
  isGridView.value = !isGridView.value
}
const handleMenuChange = (value: string) => {
  loadActivedMenu.value = value
  dataList.value = []
  if (value === 'installed') {
    handleInstallSkillLoad()
  } else {
    handleMarketSkillLoad()
  }
}
const handleInstallSkillLoad = () => {
  ioload('skill', 'loadInstallSkillList', null).then((result) => {
    dataList.value = result
  })
}
const handleMarketSkillLoad = () => {
  ioload('skill', 'loadInstallSkillList', null).then((result) => {
    dataList.value = result
  })
}
</script>
