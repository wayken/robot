<template>
  <el-tooltip effect="dark" placement="top-start" virtual-triggering
    :virtual-ref="loadTriggerRef"
    :content="$t('marksuit.menu.' + option.name)"
  />
  <el-popover trigger="click" transition="el-zoom-in-top" width="auto" :teleported="false" placement="bottom"
    v-model:visible="isViewVisible"
  >
    <template #reference>
      <div ref="loadTriggerRef" class="block">
        <a-svg-icon :icon-class="`marksuit-${option.icon}`" />
      </div>
    </template>
    <ul class="dropdown">
      <li @click="isImageView = true">
        <el-icon class="icon"><Paperclip /></el-icon>
        <div class="name">{{ $t('marksuit.image.insert-url') }}</div>
      </li>
      <li @click="handleFileImport">
        <el-icon class="icon"><Upload /></el-icon>
        <div class="name">{{ $t('marksuit.image.upload-image') }}</div>
      </li>
    </ul>
  </el-popover>
  <a-image :visible="isImageView"
    @close="isImageView = false"
    @submit="handleImageInsert"
  ></a-image>
</template>

<script setup lang="ts">
import {
  Upload,
  Paperclip
} from '@element-plus/icons-vue'
import {
  ToolTemplateProps
} from '../../interface'
import {
  useMarksuitContext
} from '../../hook/useMarksuitContext'
import {
  useFileDialog
} from '../../util/util'
import AImage from './modal.vue'
import ASvgIcon from '../../common/svgicon.vue'

defineProps<ToolTemplateProps>()

const {
  useRichEditor,
  onImageUpload
} = useMarksuitContext()

const isViewVisible = ref(false)
const isImageView = ref(false)
const loadTriggerRef = ref<HTMLElement>()

const handleImageInsert = (data: any) => {
  useRichEditor.useCommands().setImage({
    src: data.url,
    alt: data.name,
    title: data.description
  }).run()
}
const handleFileImport = async () => {
  const fileList = await useFileDialog({
    accept: 'image/*',
    multiple: false
  })
  if (!fileList) return
  const file = fileList[0]
  if (!onImageUpload) return
  const url = await onImageUpload(file)
  if (!url) return
  useRichEditor.useCommands().setImage({
    src: url,
    alt: file.name,
    title: ''
  }).run()
}
</script>
