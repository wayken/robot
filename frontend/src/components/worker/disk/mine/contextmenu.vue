<template>
  <transition name="el-zoom-in-top">
    <div class="a-disk-contextmenu" ref="loadMenuRef" v-show="visible" v-clickoutside="handleClose">
      <div class="menu" @click="handleOpen">
        <el-icon><FolderOpened /></el-icon>
        <div>{{ $t('common.open') }}</div>
      </div>
      <div class="divider"></div>
      <div class="menu" @click="handleRename">
        <el-icon><Edit /></el-icon>
        <div>{{ $t('extension.rename') }}</div>
      </div>
      <div class="menu" @click="handleMoveTo">
        <el-icon><Rank /></el-icon>
        <div>{{ $t('disk.move-to') }}</div>
      </div>
      <div class="menu menu--danger" @click="handleDelete">
        <el-icon><Delete /></el-icon>
        <div>{{ $t('common.delete') }}</div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import {
  Edit,
  Rank,
  Delete,
  FolderOpened
} from '@element-plus/icons-vue'

defineProps<{
  visible: boolean
}>()

const loadMenuRef = ref<HTMLDivElement | null>(null)

const handleEmit = defineEmits<{
  (e: 'close'): void
  (e: 'open'): void
  (e: 'rename'): void
  (e: 'move-to'): void
  (e: 'delete'): void
}>()

const show = (event: MouseEvent) => {
  nextTick(() => {
    const el = loadMenuRef.value
    if (!el) return
    let posX = event.clientX
    let posY = event.clientY
    const menuWidth = 160
    const menuHeight = 160
    if (posX + menuWidth > document.body.clientWidth) {
      posX = document.body.clientWidth - menuWidth
    }
    if (posY + menuHeight > document.body.clientHeight) {
      posY = document.body.clientHeight - menuHeight
    }
    el.style.left = `${posX}px`
    el.style.top = `${posY}px`
  })
}

const handleClose = () => {
  handleEmit('close')
}
const handleOpen = () => {
  handleEmit('open')
  handleEmit('close')
}
const handleRename = () => {
  handleEmit('rename')
  handleEmit('close')
}
const handleMoveTo = () => {
  handleEmit('move-to')
  handleEmit('close')
}
const handleDelete = () => {
  handleEmit('delete')
  handleEmit('close')
}

defineExpose({ show })
</script>
