<template>
  <transition name="el-zoom-in-top">
    <div class="a-wiki-contextmenu" ref="instance" v-show="visible" v-clickoutside="handleHide">
      <div class="arrow"></div>
      <ul>
        <li class="icon-1" @click="handleAdd('markdown')">
          <el-icon><Memo /></el-icon>
          <span>{{ $t('wiki.new-markdown') }}</span>
        </li>
        <li class="icon-2" @click="handleAdd('directory')">
          <el-icon><Folder /></el-icon>
          <span>{{ $t('wiki.new-folder') }}</span>
        </li>
        <template v-if="mode === 'full'">
          <div class="divider"></div>
          <li class="icon-3" @click="handleRename">
            <el-icon><EditPen /></el-icon>
            <span>{{ $t('extension.rename') }}</span>
          </li>
          <li class="icon-4" @click="handleMoveTo">
            <el-icon><Rank /></el-icon>
            <span>{{ $t('wiki.move-to') }}</span>
          </li>
          <div class="divider"></div>
          <li class="icon-6" @click="handleDelete">
            <el-icon><Delete /></el-icon>
            <span>{{$t('common.delete')}}</span>
          </li>
        </template>
      </ul>
    </div>
  </transition>
</template>

<script setup lang="ts">
import {
  Memo,
  Rank,
  Folder,
  Delete,
  EditPen
} from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String,
    default: 'full'
  },
  data: {
    type: Object,
    default: function() {
      return {
      }
    }
  }
})

const instance = ref<HTMLElement | null>(null)

const handlePositionUpdate = (left: number, top: number) => {
  if (instance.value) {
    instance.value.style.top = `${top}px`
    instance.value.style.left = `${left}px`
  }
}
const handleEmit = defineEmits(['close', 'add', 'rename', 'moveto', 'delete'])
const handleHide = () => {
  handleEmit('close')
}
const handleAdd = (type: string) => {
  handleEmit('add', props.data, type)
}
const handleRename = () => {
  handleEmit('rename', props.data)
}
const handleMoveTo = () => {
  handleEmit('moveto', props.data)
}
const handleDelete = () => {
  handleEmit('delete', props.data)
}

defineExpose({
  handlePositionUpdate
})
</script>
