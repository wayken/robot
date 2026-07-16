<template>
  <el-tooltip effect="dark" placement="top-start"
    :content="$t('marksuit.menu.' + option.name)"
  >
    <div class="block"
      :class="{
        'is-actived': option.isActive ? option.isActive() : false,
        'is-disabled': option.isDisabled ? option.isDisabled() : false
      }"
      @mousedown.prevent
      @click="option.onClick"
    >
      <a-svg-icon :icon-class="`marksuit-${option.icon}`" />
    </div>
  </el-tooltip>
</template>

<script setup lang="ts">
import {
  ToolTemplateProps
} from '../interface'
import ASvgIcon from '../common/svgicon.vue'

defineProps<ToolTemplateProps>()
</script>
