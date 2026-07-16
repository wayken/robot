<template>
  <Teleport to="body">
    <Transition name="x-link-menu">
      <div v-if="visible" class="x-link-menu" :style="loadMenuStyle"
        @mouseenter="handleMenuMouseenter"
        @mouseleave="handleMenuMouseleave"
      >
        <template v-if="!isEditing">
          <div class="preview">
            <a-svg-icon icon-class="marksuit-link" class="icon" />
            <span class="name" :title="loadCurrentHref">{{ loadTruncatedHref }}</span>
          </div>
          <div class="divider"></div>
          <el-tooltip effect="dark" placement="top" :content="$t('marksuit.link.open-link')">
            <div class="button" @mousedown.prevent @click="handleOpen">
              <a-svg-icon icon-class="marksuit-open-link" />
            </div>
          </el-tooltip>
          <el-tooltip effect="dark" placement="top" :content="$t('marksuit.common.edit')">
            <div class="button" @mousedown.prevent @click="handleEdit">
              <a-svg-icon icon-class="marksuit-edit-line" />
            </div>
          </el-tooltip>
          <el-tooltip effect="dark" placement="top" :content="$t('marksuit.menu.unlink')">
            <div class="button" @mousedown.prevent @click="handleUnlink">
              <a-svg-icon icon-class="marksuit-unlink" />
            </div>
          </el-tooltip>
        </template>
        <template v-else>
          <a-svg-icon icon-class="marksuit-link" class="icon" />
          <input ref="loadEditInputRef" v-model="loadEditUrl" type="text" class="marked"
            :placeholder="$t('marksuit.link.link-url')"
            @keydown.esc.prevent="handleEditCancel"
            @keydown.enter.prevent="handleEditConfirm"
          />
          <el-tooltip effect="dark" placement="top" :content="$t('marksuit.common.confirm')">
            <div class="button is-primary" @mousedown.prevent @click="handleEditConfirm">
              <a-svg-icon icon-class="marksuit-save" />
            </div>
          </el-tooltip>
          <el-tooltip effect="dark" placement="top" :content="$t('marksuit.common.cancel')">
            <div class="button" @mousedown.prevent @click="handleEditCancel">
              <a-svg-icon icon-class="marksuit-close" />
            </div>
          </el-tooltip>
        </template>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import {
  useMarksuitContext
} from '../../hook/useMarksuitContext'
import {
  LINK_HOVER_EVENT,
  LINK_LEAVE_EVENT,
  type LinkHoverPayload
} from '../../core/extensions/link/index'

const {
  useRichEditor
} = useMarksuitContext()

const visible = ref(false)
const isEditing = ref(false)
const loadCurrentHref = ref('')
const loadEditUrl = ref('')
const markTo = ref(0)
const markFrom = ref(0)
const loadAnchorRect = ref<DOMRect | null>(null)
const loadEditInputRef = ref<HTMLInputElement | null>(null)

let hideTimer: ReturnType<typeof setTimeout> | null = null
let showTimer: ReturnType<typeof setTimeout> | null = null

const POPUP_OFFSET = 8
const POPUP_HEIGHT = 36

onMounted(() => {
  const dom = useRichEditor.useInstance().view.dom
  dom.addEventListener(LINK_HOVER_EVENT, handleLinkHover)
  dom.addEventListener(LINK_LEAVE_EVENT, handleLinkLeave)
  dom.addEventListener('mouseleave', handleLinkLeave)
})
onBeforeUnmount(() => {
  const dom = useRichEditor.useInstance().view.dom
  dom.removeEventListener(LINK_HOVER_EVENT, handleLinkHover)
  dom.removeEventListener(LINK_LEAVE_EVENT, handleLinkLeave)
  dom.removeEventListener('mouseleave', handleLinkLeave)
  if (hideTimer) clearTimeout(hideTimer)
  if (showTimer) clearTimeout(showTimer)
})

const loadMenuStyle = computed(() => {
  const rect = loadAnchorRect.value
  if (!rect) return {}
  const spaceAbove = rect.top
  const posY = spaceAbove >= POPUP_HEIGHT + POPUP_OFFSET
    ? rect.top - POPUP_OFFSET - POPUP_HEIGHT + window.scrollY
    : rect.bottom + POPUP_OFFSET + window.scrollY
  const rawLeft = rect.left + window.scrollX
  const maxLeft = window.innerWidth - 320
  const posX = Math.max(8, Math.min(rawLeft, maxLeft))
  return {
    top: `${posY}px`,
    left: `${posX}px`
  }
})
const loadTruncatedHref = computed(() => {
  const url = loadCurrentHref.value
  return url.length > 40 ? url.slice(0, 38) + '…' : url
})
const handleLinkHover = (event: Event) => {
  const e = event as CustomEvent<LinkHoverPayload>
  const { href, from, to, domRect } = e.detail
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  loadCurrentHref.value = href
  markTo.value = to
  markFrom.value = from
  loadAnchorRect.value = domRect
  if (!visible.value) {
    if (showTimer) {
      clearTimeout(showTimer)
    }
    showTimer = setTimeout(() => {
      visible.value = true
    }, 80)
  }
}
const handleLinkLeave = () => {
  handleScheduleHide()
}
const handleMenuMouseenter = () => {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
}
const handleMenuMouseleave = () => {
  handleScheduleHide()
}
const handleScheduleHide = () => {
  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    if (!isEditing.value) {
      visible.value = false
    }
  }, 200)
}
const handleOpen = () => {
  window.open(loadCurrentHref.value, '_blank', 'noopener,noreferrer')
}
const handleEdit = () => {
  loadEditUrl.value = loadCurrentHref.value
  isEditing.value = true
  nextTick(() => {
    loadEditInputRef.value?.focus()
    loadEditInputRef.value?.select()
  })
}
const handleEditConfirm = () => {
  const url = loadEditUrl.value.trim()
  if (!url) return
  useRichEditor.useCommands()
    .setTextSelection({
      to: markTo.value,
      from: markFrom.value
    })
    .setLink({ href: url, target: '_blank' })
    .run()
  loadCurrentHref.value = url
  isEditing.value = false
  visible.value = false
}
const handleEditCancel = () => {
  isEditing.value = false
  loadEditUrl.value = ''
}
const handleUnlink = () => {
  useRichEditor.useCommands()
    .setTextSelection({ from: markFrom.value, to: markTo.value })
    .unsetLink()
    .run()
  visible.value = false
  isEditing.value = false
}
</script>
