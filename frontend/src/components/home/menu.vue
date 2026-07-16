<template>
  <div class="a-menu inline-flex-c-n-n" :class="{
    'is-collapsed': isMenuCollapsed
  }">
    <div class="logo inline-flex-r-c-n"
      @click="handleHomeLink"
    >
      <img :src="logoSvg" />
      <div class="name">{{ $t('portal.name') }}</div>
    </div>
    <div class="navigation">
      <ul>
        <li v-for="(data, index) in loadMenuList" :key="index"
          :class="{
            'is-docked': data.dock,
            'is-actived': isPathMatched(data)
          }"
          @click="handleMenuClick(data)"
        >
          <template v-if="data.dock">
            <span class="title inline-text-ellipsis">{{ $t('menu.' + data.name) }}</span>
          </template>
          <template v-else>
            <a-svg-icon :icon-class="data.icon"
              :style="{
                color: data.color
              }"
            />
            <span class="text">{{ $t('menu.' + data.name) }}</span>
          </template>
        </li>
      </ul>
    </div>
    <div class="interface">
      <ul>
        <li :class="{
            'is-actived': isPathMatched({ path: '/setting' })
          }"
          @click="handleMenuClick({ path: '/setting' })"
        >
          <a-svg-icon icon-class="settings"
            :style="{
              color: '#67c23a'
            }"
          />
          <span class="text">{{ $t('menu.setting') }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import logoSvg from '@/assets/logo.svg'
import runtime from '@/platform/runtime'
import { useMenuStore } from '@/store/modules/menu'

const {
  isMenuCollapsed
} = storeToRefs(useMenuStore())

const loadMenuList = [
  {
    name: 'overview',
    dock: true
  },
  {
    name: 'dashboard',
    path: '/dashboard',
    icon: 'modern-tv',
    color: '#409efc'
  },
  {
    name: 'assistant',
    path: '/assistant',
    icon: 'partener',
    color: '#9884ff'
  },
  {
    name: 'node',
    path: '/node',
    icon: 'node',
    color: '#30b8ea'
  },
  {
    name: 'management',
    dock: true
  },
  {
    name: 'provider',
    path: '/provider',
    icon: 'model',
    color: '#30b8ea'
  },
  {
    name: 'channel',
    path: '/channel',
    icon: 'cloud',
    color: '#34c724'
  },
  {
    name: 'authority',
    path: '/authority',
    icon: 'shield-user',
    color: '#e4433e'
  },
  {
    name: 'database',
    dock: true
  },
  {
    name: 'wiki',
    path: '/wiki',
    icon: 'knowledge',
    color: '#ff6155'
  },
  {
    name: 'summary',
    path: '/summary',
    icon: 'data',
    color: '#ed3388'
  },
  {
    name: 'skill',
    path: '/skill',
    icon: 'skill',
    color: '#4d88ff'
  },
  {
    name: 'logger',
    path: '/logger',
    icon: 'logger',
    color: '#ff8801'
  }
]

const router = useRouter()
const isPathMatched = (menu: any) => {
  const path = menu.path
  const route = router.currentRoute.value
  if (route.path === path) {
    return true
  }
  return false
}
const handleHomeLink = () => {
  runtime.handleOpenExternalLink('https://www.teambeit.com')
}
const handleMenuClick = (data: any) => {
  const link = data.meta?.link
  if (link) {
    runtime.handleOpenExternalLink(link)
    return
  }
  const path = data.path
  if (!isPathMatched(path)) {
    router.push(path)
  }
}
</script>
