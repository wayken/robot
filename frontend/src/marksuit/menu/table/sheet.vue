<template>
  <div class="sheet" v-select>
    <table>
      <tr v-for="r in ROW" :key="r">
        <td class="cell" v-for="c in COLUMN" :key="c"></td>
      </tr>
    </table>
    <div class="suspension" ref="loadSelectModal"></div>
  </div>
  <div class="column">{{ row }} x {{ column }}</div>
</template>

<script setup lang="ts">
const ROW = 6
const COLUMN = 6
const RECT = 20
const loadSelectModal = ref<HTMLElement | null>(null)
const row = ref(0)
const column = ref(0)
const handleEmit = defineEmits<{
  (event: 'update', payload: { row: number; column: number }): void
}>()
const vSelect = {
  mounted(el: HTMLElement) {
    el.addEventListener('mousemove', e => {
      if (loadSelectModal.value) {
        const { width, height, left, top } = el.getBoundingClientRect()
        const currentX = Math.min(e.clientX - left, width)
        const currentY = Math.min(e.clientY - top, height)

        loadSelectModal.value.style.width = `${Math.abs(currentX)}px`
        loadSelectModal.value.style.height = `${Math.abs(currentY)}px`
        loadSelectModal.value.style.left = `${currentX}px`
        loadSelectModal.value.style.top = `${currentY}px`

        const selectedItems = el.querySelectorAll('.cell')
        selectedItems.forEach(item => {
          item.classList.remove('selected')
        })
        row.value = Math.min(Math.floor(currentY / RECT) + 1, ROW)
        column.value = Math.min(Math.floor(currentX / RECT) + 1, COLUMN)

        for (let r = 0; r <= row.value - 1; r++) {
          for (let c = 0; c <= column.value - 1; c++) {
            const index = r * ROW + c
            selectedItems[index].classList.add('selected')
          }
        }
      }
    })
    el.addEventListener('mouseup', () => {
      handleEmit('update', { row: row.value, column: column.value })
      row.value = column.value = 0
      if (loadSelectModal.value) {
        loadSelectModal.value.style.left = '0px'
        loadSelectModal.value.style.top = '0px'
      }
    })
  },
  unmounted(el: HTMLElement) {
    el.removeEventListener('mousemove', () => {})
    el.removeEventListener('mouseup', () => {})
  }
}
</script>
