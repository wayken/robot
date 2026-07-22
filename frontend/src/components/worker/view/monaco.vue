<template>
  <div class="a-monaco-view" v-if="isRendered"
    :class="{
      'is-drawer': drawer,
      'is-closing': isClosing
    }"
  >
    <div class="mask" @click="handleMaskClick"></div>
    <div class="main inline-flex-c-n-n"
      :class="{
        'is-fullscreen': isFullscreen
      }"
      :style="loadMainStyle"
    >
      <div class="header inline-flex-r-c-b">
        <div class="header--left inline-flex-r-c-n">
          <div class="filename">{{ filename }}</div>
          <div class="badge"
            :class="{
              'is-editing': isEditing
            }"
          >
            {{ isEditing ? $t('monaco.editing') : $t('monaco.preview') }}
          </div>
        </div>
        <div class="header--right">
          <a-button v-if="!isEditing" :icon="Edit" size="small"
            @click="handleEditMode"
          >
            {{ $t('monaco.edit') }}
          </a-button>
          <a-button v-if="isEditing" type="success" :icon="Check" size="small"
            @click="handleSave"
          >
            {{ $t('monaco.save') }}
          </a-button>
          <a-button v-if="isEditing" size="small"
            @click="handleCancelEdit"
          >
            {{ $t('monaco.cancel') }}
          </a-button>
          <div class="close" @click="handleFullscreen">
            <a-svg-icon :icon-class="isFullscreen ? 'shrink' : 'expand'" size="20px" />
          </div>
          <div class="close" @click="handleClose">
            <el-icon><Close /></el-icon>
          </div>
        </div>
      </div>
      <div class="monaco" ref="loadMonacoRef"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Close,
  Edit,
  Check
} from '@element-plus/icons-vue'
import {
  useMonacoLanguage
} from '@/utils/filetype'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  filename: {
    type: String,
    default: ''
  },
  content: {
    type: String,
    default: ''
  },
  drawer: {
    type: Boolean,
    default: false
  },
  width: {
    type: [String, Number],
    default: ''
  },
  closeOnClickModal: {
    type: Boolean,
    default: false
  },
  closeOnPressEscape: {
    type: Boolean,
    default: false
  }
})

self.MonacoEnvironment = {
  getWorker(_: any, label: string) {
    if (label === 'json') {
      return new jsonWorker()
    }
    if (label === 'css' || label === 'scss' || label === 'less') {
      return new cssWorker()
    }
    if (label === 'html' || label === 'handlebars' || label === 'razor') {
      return new htmlWorker()
    }
    if (label === 'typescript' || label === 'javascript') {
      return new tsWorker()
    }
    return new editorWorker()
  }
}

const isEditing = ref(false)
const isFullscreen = ref(false)
const isRendered = ref(false)
const isClosing = ref(false)
const loadMonacoRef = ref<HTMLElement>()
const loadDrawerAnimationDuration = 280
let loadCloseTimer: number | undefined
let instance: monaco.editor.IStandaloneCodeEditor | null = null

const handleEmit = defineEmits(['close', 'save'])
const loadMainStyle = computed(() => {
  if (!props.drawer || isFullscreen.value) {
    return {}
  }
  if (typeof props.width === 'number') {
    return { width: `${props.width}px` }
  }
  if (props.width) {
    return { width: props.width }
  }
  return {}
})
const handleEditorInit = () => {
  if (!loadMonacoRef.value) return
  handleEditorDestroy()
  const language = useMonacoLanguage(props.filename)
  instance = monaco.editor.create(loadMonacoRef.value, {
    value: props.content,
    language,
    tabSize: 2,
    fontSize: 17,
    wordWrap: 'on',
    readOnly: true,
    lineHeight: 22,
    theme: 'vs-dark',
    lineNumbers: 'on',
    automaticLayout: true,
    minimap: { enabled: true },
    scrollBeyondLastLine: false
  })
}
const handleEditorDestroy = () => {
  if (instance) {
    instance.dispose()
    instance = null
  }
}
// 切换到编辑模式
const handleEditMode = () => {
  isEditing.value = true
  if (instance) {
    instance.updateOptions({ readOnly: false })
  }
}
// 取消编辑模式
const handleCancelEdit = () => {
  isEditing.value = false
  if (instance) {
    instance.setValue(props.content)
    instance.updateOptions({ readOnly: true })
  }
}
// 保存
const handleSave = () => {
  if (instance) {
    const value = instance.getValue()
    handleEmit('save', value)
  }
  isEditing.value = false
  if (instance) {
    instance.updateOptions({ readOnly: true })
  }
}
// 全屏切换
const handleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}
// 关闭
const handleClose = () => {
  isEditing.value = false
  isFullscreen.value = false
  if (props.drawer) {
    handleDrawerClose(true)
    return
  }
  handleEmit('close')
}
const handleMaskClick = () => {
  if (!props.closeOnClickModal || isClosing.value) return
  handleClose()
}
const handleKeydown = (event: KeyboardEvent) => {
  if (!props.closeOnPressEscape || !isRendered.value || isClosing.value) return
  if (event.key !== 'Escape') return
  handleClose()
}
const handleDrawerClose = (fired: boolean) => {
  if (isClosing.value) return
  isClosing.value = true
  if (loadCloseTimer) {
    window.clearTimeout(loadCloseTimer)
  }
  loadCloseTimer = window.setTimeout(() => {
    handleEditorDestroy()
    isRendered.value = false
    isClosing.value = false
    loadCloseTimer = undefined
    if (fired) {
      handleEmit('close')
    }
  }, loadDrawerAnimationDuration)
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  if (loadCloseTimer) {
    window.clearTimeout(loadCloseTimer)
  }
  window.removeEventListener('keydown', handleKeydown)
  handleEditorDestroy()
})

// 监听可见性变化
watch(() => props.visible, (val) => {
  if (val) {
    if (loadCloseTimer) {
      window.clearTimeout(loadCloseTimer)
      loadCloseTimer = undefined
    }
    isClosing.value = false
    isRendered.value = true
    nextTick(() => {
      handleEditorInit()
    })
  } else {
    if (props.drawer && isRendered.value) {
      handleDrawerClose(false)
    } else {
      handleEditorDestroy()
      isRendered.value = false
      isClosing.value = false
    }
  }
}, { immediate: true })
// 监听内容变化（外部更新）
watch(() => props.content, (value) => {
  if (instance && !isEditing.value) {
    instance.setValue(value)
  }
})
</script>
