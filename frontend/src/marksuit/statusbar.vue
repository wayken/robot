<template>
  <div class="statusbar">
    <!-- 左侧：字符数 -->
    <div class="statusbar__left">
      <span class="words">{{ $t('marksuit.statusbar.word-count') }}：{{ loadCharCount }}</span>
    </div>
    <!-- 右侧：切换源码模式 -->
    <div class="statusbar__right">
      <el-tooltip effect="dark" placement="top-end"
        :content="isSourceMode ? $t('marksuit.statusbar.switch-richeditor-mode') : $t('marksuit.statusbar.switch-source-mode')"
      >
        <div class="operation"
          :class="{
            'is-actived': isSourceMode
          }"
          @click="onSourceModeSwitch"
        >
          <a-svg-icon icon-class="marksuit-code" size="18px" />
        </div>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  useMarksuitContext
} from './hook/useMarksuitContext'
import ASvgIcon from './common/svgicon.vue'

const {
  useRichEditor,
  isSourceMode,
  onSourceModeSwitch
} = useMarksuitContext()

const loadCharCount = computed(() => {
  const instance = useRichEditor.useInstance()
  return instance.state.doc.textContent.length
})
</script>
