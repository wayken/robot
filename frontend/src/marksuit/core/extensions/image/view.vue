<template>
	<node-view-wrapper as="div" class="x-image"
    :class="{
      'is-actived': isImageSelected
    }"
    @dblclick="handleImagePreview(true)"
  >
      <img :src="src" :width="width" :height="height" :alt="node.attrs.alt" :title="node.attrs.title" />
      <div class="resizer" v-show="isImageSelected || isImageResizing">
        <div class="point" v-for="point in (loadPointList)" :key="point" :class="point"
          @mousedown.stop.prevent="handleImageResize(point, $event)"
        >
        </div>
      </div>
    <!-- 图片预览组件 -->
    <a-image-preview :image="loadActivedImage" :visible="isImagePreview" single
      @close="handleImagePreview(false)"
    ></a-image-preview>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import {
  nodeViewProps,
  NodeViewWrapper
} from '@tiptap/vue-3'
import { NodeSelection } from '@tiptap/pm/state'
import AImagePreview from '../../../common/preview.vue'

const props = defineProps(nodeViewProps)

const isImageSelected = ref(false)
const isImageResizing = ref(false)
const isImagePreview = ref(false)
const loadActivedImage = ref({})
const loadPointList = ['nwre', 'swre', 'nere', 'sere']

const src = computed(() => {
  return props.node.attrs.src
})
// 拖拽过程中用本地变量驱动 UI，避免频繁触发 tiptap update
const resizingWidth = ref<number | null>(null)
const resizingHeight = ref<number | null>(null)
const width = computed(() => resizingWidth.value ?? props.node.attrs.width)
const height = computed(() => resizingHeight.value ?? props.node.attrs.height)

onMounted(() => {
  props.editor?.on('selectionUpdate', handleSelectionStateUpdate)
})
onBeforeUnmount(() => {
  props.editor?.off('selectionUpdate', handleSelectionStateUpdate)
})

// 监听选择状态变化
const handleSelectionStateUpdate = () => {
  const { editor } = props
  if (!editor) return
  const { selection } = editor.state
  const pos = props.getPos()
  isImageSelected.value = selection instanceof NodeSelection && typeof pos === 'number' && selection.from === pos
}
const handleImagePreview = (view: boolean) => {
  if (view) {
    isImagePreview.value = true
    loadActivedImage.value = {
      url: props.node.attrs.src
    }
  } else {
    isImagePreview.value = false
  }
}
const handleImageResize = (point: string, event: MouseEvent) => {
  isImageResizing.value = true
  const MIN_SIZE = 20
  const startX = event.clientX
  const startY = event.clientY
  // 优先取当前渲染尺寸，避免 naturalWidth/Height 返回原始分辨率导致跳变
  const imgEl = (event.target as HTMLElement).closest('.x-image')?.querySelector('img') as HTMLImageElement | null
  const startWidth = props.node.attrs.width ?? imgEl?.offsetWidth ?? imgEl?.naturalWidth ?? 200
  const startHeight = props.node.attrs.height ?? imgEl?.offsetHeight ?? imgEl?.naturalHeight ?? startWidth
  // 宽高比，用于等比缩放
  const aspectRatio = startWidth / startHeight
  const move = (moveEvent: MouseEvent) => {
    const posX = moveEvent.clientX - startX
    const posY = moveEvent.clientY - startY
    let newWidth = startWidth
    let newHeight = startHeight
    // 根据拖拽点方向计算宽高变化
    if (point === 'sere') {
      newWidth = startWidth + posX
      newHeight = startHeight + posY
    } else if (point === 'swre') {
      newWidth = startWidth - posX
      newHeight = startHeight + posY
    } else if (point === 'nere') {
      newWidth = startWidth + posX
      newHeight = startHeight - posY
    } else if (point === 'nwre') {
      newWidth = startWidth - posX
      newHeight = startHeight - posY
    }
    // 未按住Shift时等比缩放（以宽度为基准）
    if (!moveEvent.shiftKey) {
      newHeight = Math.round(newWidth / aspectRatio)
    }
    newWidth = Math.max(MIN_SIZE, Math.round(newWidth))
    newHeight = Math.max(MIN_SIZE, Math.round(newHeight))
    resizingWidth.value = newWidth
    resizingHeight.value = newHeight
  }
  const up = () => {
    isImageResizing.value = false
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
    if (resizingWidth.value !== null || resizingHeight.value !== null) {
      props.updateAttributes({
        width: resizingWidth.value ?? props.node.attrs.width,
        height: resizingHeight.value ?? props.node.attrs.height
      })
      // 缩放过程会触发重渲染导致选区丢失，重新选中该节点
      props.editor?.commands.setNodeSelection(<number>props.getPos())
    }
    resizingWidth.value = null
    resizingHeight.value = null
  }
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}
</script>
