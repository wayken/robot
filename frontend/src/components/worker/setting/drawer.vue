<template>
  <div class="drawer"
    :class="{
      'is-dragging': isDragging,
      'is-collapsed': isCollapsed
    }"
    :style="{
      width: loadMenuWidth + 'px'
    }"
  >
    <div class="wrapper inline-flex-c-n-n"
      :style="{
        width: loadMenuWidth + 'px'
      }"
    >
      <!-- 顶部标题 -->
      <div class="head inline-flex-r-n-n">
        <div class="menu inline-flex-r-c-n">
          <a-svg-icon icon-class="settings" size="18px" />
          <div class="name">{{ $t('menu.setting') }}</div>
        </div>
      </div>
      <!-- 导航菜单 -->
      <div class="main">
        <div class="menu inline-flex-r-c-n" v-for="(data, index) in loadMenuList" :key="index"
          :class="{
            'is-actived': isPathMatched(data)
          }"
          @click="handleMenuClick(data)"
        >
          <el-icon class="icon" :style="{
            color: data.color
          }">
            <component :is="data.icon"></component>
          </el-icon>
          <span class="name">{{ $t('submenu.setting.' + data.name) }}</span>
        </div>
      </div>
    </div>
    <div class="flexible" @mousedown="handleResize"></div>
    <div class="caret" @click="handleCollapse">
      <el-icon v-if="!isCollapsed"><CaretLeft /></el-icon>
      <el-icon v-else><CaretRight /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  CaretLeft,
  CaretRight,
  Document,
  Notebook,
  MagicStick,
  Suitcase,
  Connection
} from '@element-plus/icons-vue'
import { useDraggingResize } from '@/hooks/useDraggingResize'

const router = useRouter()

const loadMenuList = [
  {
    name: 'toolkit',
    path: '/worker/setting/toolkit/:id',
    icon: Suitcase,
    color: '#22d7bb'
  },
  {
    name: 'skill',
    path: '/worker/setting/skill/:id',
    icon: MagicStick,
    color: '#e6a23c'
  },
  {
    name: 'rule',
    path: '/worker/setting/rule/:id',
    icon: Document,
    color: '#409eff'
  },
  {
    name: 'memory',
    path: '/worker/setting/memory/:id',
    icon: Notebook,
    color: '#67c23a'
  },
  {
    name: 'plugin',
    path: '/worker/setting/plugin/:id',
    icon: Connection,
    color: '#9b59b6'
  }
]

const isCollapsed = ref(false)
const { width: loadMenuWidth, isDragging, handleResize } = useDraggingResize({ initialWidth: 220, min: 158, max: 420 })

const isPathMatched = (data: any) => {
  const route = router.currentRoute.value
  const id = route.params.id
  let realPath = data.path.replace(':id', id)
  return route.path === realPath
}
const handleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
const handleMenuClick = (data: any) => {
  const route = router.currentRoute.value
  const id = route.params.id
  const realPath = data.path.replace(':id', id)
  router.push(realPath)
}
</script>
