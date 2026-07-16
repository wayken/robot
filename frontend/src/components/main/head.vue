<template>
  <div class="a-main-head inline-flex-r-c-b">
    <div class="head--left inline-flex-r-n-n">
      <draggable class="windows inline-flex-r-n-n" itemKey="id" draggable=".draggable" filter=".forbid" group="layout"
        v-model="loadWindowsList"
        :animation="300"
        @change="handleWindowSort"
      >
        <template #item="{ element }">
          <div class="window inline-flex-r-c-n"
            :class="[
              !element.root ? 'draggable' : 'forbid',
              {
                'is-actived': element.wid === activedWindowId
              }
            ]"
            @click="handleWindowSwitch(element)"
          >
            <template v-if="element.root">
              <a-svg-icon class="icon" icon-class="home" />
              <div class="name">{{ $t('header.home') }}</div>
            </template>
            <template v-else>
              <a-svg-icon class="icon" icon-class="modern-tv" />
              <div class="name inline-text-ellipsis" :title="element.name">
                {{ element.name }}
              </div>
              <el-icon class="close"
                @click.stop="handleWindowClose(element)"
              ><Close /></el-icon>
            </template>
          </div>
        </template>
      </draggable>
    </div>
    <div class="head--right inline-flex-r-n-n">
      <div class="icon inline-flex-r-c-c" @click="handleWindowReload">
        <a-svg-icon icon-class="refresh" />
      </div>
      <div v-if="settingData.development" class="icon inline-flex-r-c-c"
        @click="handleOpenDevTools"
      >
        <a-svg-icon icon-class="terminal" />
      </div>
      <div class="icon inline-flex-r-c-c" @click="handleMinimizeWindow">
        <a-svg-icon icon-class="minus-window" />
      </div>
      <div class="icon inline-flex-r-c-c" @click="handleMaximizeWindow">
        <a-svg-icon v-if="isMaximizeWindow" icon-class="minimize-window" />
        <a-svg-icon v-if="!isMaximizeWindow" icon-class="maximize-window" />
      </div>
      <div class="icon close inline-flex-r-c-c" @click="handleCloseWindow">
        <a-svg-icon icon-class="close-window" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Close
} from '@element-plus/icons-vue'
import Draggable from 'vuedraggable'
import runtime from '@/platform/runtime'
import { useAppStore } from '@/store/modules/app'

const {
  settingData
} = storeToRefs(useAppStore())

const activedWindowId = ref<number>(0)
const loadWindowsList = ref<any>([
  {
    id: 'index',
    wid: 0,
    root: true,
    name: 'home',
    path: '/'
  }
])
const isMaximizeWindow = ref(false)

onMounted(() => {
  isMaximizeWindow.value = runtime.isMaximizedWindow()
  // 监听窗口resize事件
  window.addEventListener('resize', handleWindowResizeListen)
  // 监听窗口切换事件
  runtime.handleApplicationEventOn('on-window-tablet-switch', handleWindowUpdate)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResizeListen)
  runtime.handleApplicationEventOff('on-window-tablet-switch')
})

const handleWindowSwitch = (data: any) => {
  runtime.handleSwitchTabWindow({
    id: data.id
  })
}
const handleWindowUpdate = (data: any) => {
  activedWindowId.value = data.activeWindowId
  loadWindowsList.value = data.windowList
}
const handleWindowSort = () => {
  const sortList = JSON.parse(JSON.stringify(loadWindowsList.value))
  runtime.handleSortTabWindow(sortList)
}
const handleWindowClose = (data: any) => {
  runtime.handleCloseTabWindow({
    id: data.id,
    wid: data.wid
  })
}
// 刷新选项卡页面
const handleWindowReload = () => {
  runtime.handleReloadTabWindow()
}
// 打开开发者工具
const handleOpenDevTools = () => {
  runtime.handleOpenTabWindowDevTools()
}
// 窗口最小化
const handleMinimizeWindow = () => {
  runtime.handleMinimizeWindow()
}
// 窗口最大化
const handleMaximizeWindow = () => {
  isMaximizeWindow.value = runtime.isMaximizedWindow()
  if (isMaximizeWindow.value) {
    runtime.handleUnmaximizeWindow()
  } else {
    runtime.handleMaximizeWindow()
  }
  isMaximizeWindow.value = !isMaximizeWindow.value
}
// 关闭窗口
const handleCloseWindow = () => {
  runtime.handleCloseWindow()
}
// 监听窗口resize事件
const handleWindowResizeListen = () => {
  isMaximizeWindow.value = runtime.isMaximizedWindow()
}
</script>
