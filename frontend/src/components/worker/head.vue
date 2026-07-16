<template>
  <div class="a-head inline-flex-r-c-b" v-loading="progression.loading">
    <div class="head--left inline-flex-r-c-n">
      <div class="drawer" @click="handleGoHome">
        <a-svg-icon class="icon" icon-class="home" size="22px" />
      </div>
      <div class="image">
        <img v-if="infomation.image" :src="infomation.image" />
      </div>
      <div class="name">{{ infomation.name || '-' }}</div>
    </div>
    <div class="menu inline-flex-r-c-n">
      <div class="inline-flex-r-c-n" v-for="(data, index) in loadMenuList" :key="index"
        :class="{
          'is-actived': isPathMatched(data)
        }"
        @click="handleModuleClick(data)"
      >
        <a-svg-icon :icon-class="data.icon"
          :style="{
            color: data.color
          }"
        />
        <span class="text">{{ $t('menu.' + data.name) }}</span>
      </div>
    </div>
    <div class="head--right">
      <a-button :icon="Promotion">
        {{ $t('worker.release') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Promotion
} from '@element-plus/icons-vue'
import runtime from '@/platform/runtime'
import { useRequest } from '@/hooks/useRequest'

const infomation = ref<any>({})
const loadMenuList = [
  {
    name: 'chat',
    path: '/worker/chat/:id',
    icon: 'chat-lines',
    color: '#22d7bb'
  },
  {
    name: 'disk',
    path: '/worker/disk/:id',
    icon: 'disk',
    color: '#ed3388'
  },
  {
    name: 'wiki',
    path: '/worker/wiki/:id',
    icon: 'knowledge',
    color: '#ff8801'
  },
  {
    name: 'task',
    path: '/worker/task/:id',
    icon: 'cell',
    color: '#7d4dff'
  },
  {
    name: 'subworker',
    path: '/worker/subworker/:id',
    icon: 'telegram',
    color: '#30b8ea'
  },
  {
    name: 'summary',
    path: '/worker/summary/:id',
    icon: 'data',
    color: '#22d7bb'
  },
  {
    name: 'worker',
    path: '/worker/setting/:id',
    icon: 'settings',
    color: '#ff4d4f'
  }
]
const router = useRouter()
const { ioload, progression } = useRequest()
const loadSavedQuery = ref<Record<string, Record<string, any>>>({})

onMounted(() => {
  handleDataLoad()
})

const isPathMatched = (data: any) => {
  const route = router.currentRoute.value
  const id = route.params.id as string
  const realPath = data.path.replace(':id', id)
  if (route.path === realPath) {
    return true
  }
  const parent = route.matched[1]
  if (!parent) {
    return false
  }
  const parentRealPath = parent.path.replace(':id', id)
  return parentRealPath === realPath
}

const handleDataLoad = () => {
  const params = {
    id: router.currentRoute.value.params.id
  }
  ioload('assistant', 'loadAssistantInfomation', params).then((result) => {
    infomation.value = result
  })
}
const handleGoHome = () => {
  if (runtime.isApplication()) {
    runtime.handleSwitchTabWindow({
      id: 'index'
    })
  } else {
    router.push({ path: '/' })
  }
}
const handleModuleClick = (data: any) => {
  if (!isPathMatched(data)) {
    const route = router.currentRoute.value
    const id = route.params.id
    const realPath = data.path.replace(':id', id)
    const savedQuery = loadSavedQuery.value[realPath]
      ?? Object.entries(loadSavedQuery.value).find(([key]) => key.startsWith(realPath))?.[1]
      ?? {}
    router.push({ path: realPath, query: savedQuery })
  }
}

watch(
  () => router.currentRoute.value,
  (route) => {
    if (Object.keys(route.query).length > 0) {
      loadSavedQuery.value[route.path] = { ...route.query }
      const menuItem = loadMenuList.find((item) => {
        const id = route.params.id as string
        const menuRealPath = item.path.replace(':id', id)
        return route.path.startsWith(menuRealPath)
      })
      if (menuItem) {
        const id = route.params.id as string
        const menuRealPath = menuItem.path.replace(':id', id)
        loadSavedQuery.value[menuRealPath] = { ...route.query }
      }
    }
  },
  { immediate: true, deep: true }
)
</script>
