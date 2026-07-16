<template>
  <LinkHoverMenu v-if="useEditor" />
  <BubbleMenu v-if="useEditor && loadScrollTarget" class="bubble"
    :editor="useEditor"
    :should-show="isTextBubbleShouldShow"
    :options="loadBubbleOptions"
  >
    <template v-for="(data) in loadTextNodeList" :key="data.name">
      <el-tooltip v-if="data.buildin" effect="dark" placement="top-start"
        :content="$t(`marksuit.menu.${data.name}`)"
      >
        <div class="block"
          :class="{
            'is-actived': data.isActive(),
          }"
          @click="data.onClick()"
        >
          <a-svg-icon :icon-class="data.icon" />
        </div>
      </el-tooltip>
      <div class="is-divider" v-if="data.name === 'divider'"></div>
    </template>
  </BubbleMenu>
  <BubbleMenu v-if="useEditor && loadScrollTarget" class="bubble"
    :editor="useEditor"
    :should-show="isImageBubbleShouldShow"
    :options="loadBubbleOptions"
  >
    <template v-for="(data) in loadImageNodeList" :key="data.name">
      <el-tooltip v-if="data.buildin" effect="dark" placement="top-start"
        :content="$t(`marksuit.image.${data.name}`)"
      >
        <div class="block"
          :class="{
            'is-actived': data.isActive(),
          }"
          @click="data.onClick()"
        >
          <a-svg-icon :icon-class="data.icon" />
        </div>
      </el-tooltip>
      <div class="is-divider" v-if="data.name === 'divider'"></div>
    </template>
  </BubbleMenu>
</template>

<script setup lang="ts">
import {
  BubbleMenu
} from '@tiptap/vue-3/menus'
import {
  useMarksuitContext
} from './hook/useMarksuitContext'
import ASvgIcon from './common/svgicon.vue'
import LinkHoverMenu from './menu/link/menu.vue'
import { NodeSelection } from '@tiptap/pm/state'

const {
  useRichEditor
} = useMarksuitContext()
const useEditor = useRichEditor.useInstance()

const loadScrollTarget = ref<HTMLElement | undefined>(undefined)

onMounted(() => {
  if (resolveScrollTarget()) return
  let tries = 0
  const tick = () => {
    if (resolveScrollTarget() || tries++ >= 30) return
    requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
})

const loadTextNodeList = [
  {
    name: 'bold',
    icon: 'marksuit-bold',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('bold')
    },
    onClick() {
      useRichEditor.useCommands().toggleBold().run()
    }
  },
  {
    name: 'italic',
    command: 'italic',
    icon: 'marksuit-italic',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('italic')
    },
    onClick() {
      useRichEditor.useCommands().toggleItalic().run()
    }
  },
  {
    name: 'underline',
    command: 'underline',
    icon: 'marksuit-underline',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('underline')
    },
    onClick() {
      useRichEditor.useCommands().toggleUnderline().run()
    }
  },
  {
    name: 'divider'
  },
  {
    name: 'linethrough',
    command: 'strike',
    icon: 'marksuit-strikethrough',
    buildin: true,
    isActive() {
      return useRichEditor.useInstance().isActive('strike')
    },
    onClick() {
      useRichEditor.useCommands().toggleStrike().run()
    }
  }
]
const loadImageNodeList = [
  {
    name: 'download-image',
    icon: 'marksuit-download',
    buildin: true,
    isActive() {
      return false
    },
    onClick() {
      const { selection } = useRichEditor.useInstance().state
      const imageNode = selection instanceof NodeSelection ? selection.node : null
      if (imageNode) {
        const src = imageNode.attrs.src
        const link = document.createElement('a')
        link.href = src
        link.download = src.split('/').pop() || 'image'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
      }
    }
  }
]

const loadBubbleOptions = computed<{ placement: 'top'; scrollTarget?: HTMLElement }>(() => ({
  placement: 'top',
  ...(loadScrollTarget.value ? { scrollTarget: loadScrollTarget.value } : {})
}))
const resolveScrollTarget = (): boolean => {
  const dom = useEditor?.view?.dom
  const target = dom?.closest('.a-marksuit-content') as HTMLElement | null
  if (target) {
    loadScrollTarget.value = target
    return true
  }
  return false
}
const isTextBubbleShouldShow = ({ editor }: { editor: any }) => {
  const { selection } = editor.state
  if (selection instanceof NodeSelection) return false
  return !selection.empty
}
const isImageBubbleShouldShow = ({ editor }: { editor: any }) => {
  const { selection } = editor.state
  if (!(selection instanceof NodeSelection)) return false
  return selection.node?.type?.name === 'image'
}
</script>
