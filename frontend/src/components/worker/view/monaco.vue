<template>
  <div class="a-view-monaco" v-if="visible">
    <div class="mask"></div>
    <div class="main inline-flex-c-n-n"
      :class="{
        'is-fullscreen': isFullscreen
      }"
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
      <div class="monaco" ref="loadEditorRef"></div>
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
  }
})

const isEditing = ref(false)
const isFullscreen = ref(false)
const loadEditorRef = ref<HTMLElement>()
let instance: monaco.editor.IStandaloneCodeEditor | null = null

onUnmounted(() => {
  handleEditorDestroy()
})

const handleEmit = defineEmits(['close', 'save'])
const handleEditorInit = () => {
  if (!loadEditorRef.value) return
  const language = useMonacoLanguage(props.filename)
  instance = monaco.editor.create(loadEditorRef.value, {
    value: props.content,
    language,
    tabSize: 2,
    fontSize: 18,
    wordWrap: 'on',
    readOnly: true,
    lineHeight: 28,
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
  handleEmit('close')
}

// 监听可见性变化
watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => {
      handleEditorInit()
    })
  } else {
    handleEditorDestroy()
  }
})
// 监听内容变化（外部更新）
watch(() => props.content, (value) => {
  if (instance && !isEditing.value) {
    instance.setValue(value)
  }
})
</script>
