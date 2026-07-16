<template>
  <el-tooltip effect="dark" placement="top-start" virtual-triggering
    :virtual-ref="loadTriggerRef"
    :content="$t('marksuit.menu.' + option.name)"
  />
  <el-popover v-model:visible="isViewVisible" trigger="click" transition="el-zoom-in-top" :teleported="false" placement="bottom"
    :width="182"
  >
    <template #reference>
      <div ref="loadTriggerRef" class="block"
        :class="{
          'is-actived': isMenuActived
        }"
        @mousedown.prevent
        @click="option.onClick"
      >
        <a-svg-icon :icon-class="`marksuit-${option.icon}`" />
      </div>
    </template>
    <ul class="dropdown">
      <el-popover placement="right" :teleported="false" trigger="hover">
        <template #reference>
          <li class="bottom">
            <div>{{ $t('marksuit.table.insert-table') }}</div>
          </li>
        </template>
        <a-sheet @update="handleTableEmit" />
      </el-popover>
      <li @click="handleAddRowBefore()">{{ $t('marksuit.table.add-row-above') }}</li>
      <li @click="handleAddRowAfter()">{{ $t('marksuit.table.add-row-below') }}</li>
      <li class="bottom" @click="handleDeleteRow()">{{ $t('marksuit.table.delete-row') }}</li>
      <li @click="handleAddColumnLeft()">{{ $t('marksuit.table.add-column-left') }}</li>
      <li @click="handleAddColumnRight()">{{ $t('marksuit.table.add-column-right') }}</li>
      <li class="bottom" @click="handleDeleteColumn()">{{ $t('marksuit.table.delete-column') }}</li>
      <li @click="handleMergeCells()">{{ $t('marksuit.table.merge-cells') }}</li>
      <li @click="handleSplitCell()">{{ $t('marksuit.table.cancel-merge-cells') }}</li>
    </ul>
  </el-popover>
</template>

<script setup lang="ts">
import {
  ToolTemplateProps
} from '../../interface'
import { 
  useMarksuitContext
} from '../../hook/useMarksuitContext'
import ASvgIcon from '../../common/svgicon.vue'
import ASheet from './sheet.vue'

const {
  useRichEditor
} = useMarksuitContext()

interface TableType {
	row: number
	column: number
}

defineProps<ToolTemplateProps>()

const isViewVisible = ref(false)
const loadTriggerRef = ref<HTMLElement>()

const isMenuActived = computed(() => {
  return useRichEditor.useInstance()?.isActive('table')
})
const handleTableEmit = ({ row, column }: TableType) => {
  useRichEditor.useInstance()?.chain().focus().insertTable({
    rows: row,
    cols: column,
    withHeaderRow: true
  }).run()
  isViewVisible.value = false
}
const handleAddRowBefore = () => {
  useRichEditor.useInstance()?.chain().focus().addRowBefore().run()
  isViewVisible.value = false
}
const handleAddRowAfter = () => {
  useRichEditor.useInstance()?.chain().focus().addRowAfter().run()
  isViewVisible.value = false
}
const handleDeleteRow = () => {
  useRichEditor.useInstance()?.chain().focus().deleteRow().run()
  isViewVisible.value = false
}
const handleAddColumnLeft = () => {
  useRichEditor.useInstance()?.chain().focus().addColumnBefore().run()
  isViewVisible.value = false
}
const handleAddColumnRight = () => {
  useRichEditor.useInstance()?.chain().focus().addColumnAfter().run()
  isViewVisible.value = false
}
const handleDeleteColumn = () => {
  useRichEditor.useInstance()?.chain().focus().deleteColumn().run()
  isViewVisible.value = false
}
const handleMergeCells = () => {
  useRichEditor.useInstance()?.chain().focus().mergeCells().run()
  isViewVisible.value = false
}
const handleSplitCell = () => {
  useRichEditor.useInstance()?.chain().focus().splitCell().run()
  isViewVisible.value = false
}
</script>
