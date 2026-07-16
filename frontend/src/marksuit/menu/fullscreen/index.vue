<template>
  <el-tooltip effect="dark" placement="top-start"
    :content="$t('marksuit.menu.' + (isFullscreen ? 'partscreen' : 'fullscreen'))"
  >
    <div class="block" @click="handleToggle">
      <a-svg-icon :icon-class="isFullscreen ? 'marksuit-shrink' : 'marksuit-expand'" />
    </div>
  </el-tooltip>
</template>

<script setup lang="ts">
import {
  ToolTemplateProps
} from '../../interface'
import {
  useMarksuitContext
} from '../../hook/useMarksuitContext'
import ASvgIcon from '../../common/svgicon.vue'

defineProps<ToolTemplateProps>()

const {
  isFullscreen,
  onFullscreenInvoke
} = useMarksuitContext()

const handleToggle = () => {
  onFullscreenInvoke()
}
</script>
