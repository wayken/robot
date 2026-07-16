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
          <a-svg-icon icon-class="disk" size="18px" />
          <div class="name">{{ $t('menu.disk') }}</div>
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
          <span class="name">{{ $t('disk.' + data.name) }}</span>
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
  Delete,
  CaretLeft,
  CaretRight,
  FolderOpened
} from '@element-plus/icons-vue'
import { useDraggingResize } from '@/hooks/useDraggingResize'

const loadMenuList = [
  {
    name: 'mine',
    path: '/worker/disk/mine/:id',
    icon: FolderOpened,
    color: '#22d7bb'
  },
  {
    name: 'trash',
    path: '/worker/disk/trash/:id',
    icon: Delete,
    color: '#ed3388'
  }
]

const {
  width: loadMenuWidth,
  isDragging,
  handleResize
} = useDraggingResize({ initialWidth: 220, min: 158, max: 420 })
const isCollapsed = ref(false)

const router = useRouter()
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
