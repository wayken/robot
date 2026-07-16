<template>
  <div class="a-color-picker" @mousedown.stop>
    <!-- 预设颜色面板 -->
    <div class="preset">
      <div class="predefine">
        <div v-for="(color, index) in loadPresetColorList" :key="index" class="cell"
          :title="color"
          :style="{
            'backgroundColor': color
          }"
          :class="{
            'is-actived': isActiveColor(color)
          }"
          @click="handlePresetClick(color)"
        ></div>
      </div>
      <div class="more-colors-btn" @click="isAdvancedShow = !isAdvancedShow">
        <el-icon size="18px"><Location /></el-icon>
        <span>{{ $t('marksuit.picker.more-colors') }}</span>
        <svg class="arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
          :class="{
            open: isAdvancedShow
          }"
        >
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </div>
    </div>
    <!-- 高级颜色面板（展开） -->
    <Transition name="picker-slide">
      <div v-if="isAdvancedShow" class="advanced">
        <!-- 色域画布 -->
        <div ref="loadCanvasRef" class="canvas"
          @mousedown="handleCanvasMousedown"
        >
          <div class="hue" :style="{
            backgroundColor: hueColor
          }"></div>
          <div class="white"></div>
          <div class="black"></div>
          <div class="cursor" :style="{
            top: cursorY + '%',
            left: cursorX + '%'
          }"
          ></div>
        </div>
        <!-- 色相 & 透明度滑块 -->
        <div class="preview">
          <div class="box">
            <div class="background" :style="{
              opacity: alpha,
              backgroundColor: currentHex
            }"
            ></div>
          </div>
          <div class="sliders">
            <div ref="hueTrackRef" class="slider hue-slider" @mousedown="handleHueMousedown">
              <div class="thumb" :style="{ left: hue / 360 * 100 + '%' }" />
            </div>
            <div ref="alphaTrackRef" class="slider alpha-slider" @mousedown="handleAlphaMousedown">
              <div class="alpha-bg" />
              <div
                class="alpha-gradient"
                :style="{ background: `linear-gradient(to right, transparent, ${hueColor})` }"
              />
              <div class="thumb" :style="{ left: alpha * 100 + '%' }" />
            </div>
          </div>
        </div>
        <!-- 颜色输入框 -->
        <div class="holder">
          <div class="input-group hex-group">
            <span class="input-prefix">#</span>
            <input
              class="color-input"
              :value="hexInputValue"
              maxlength="6"
              @input="handleHexInput"
              @blur="handleHexBlur"
            />
            <label class="input-label">Hex</label>
          </div>
          <div class="input-group">
            <input class="color-input" :value="r" maxlength="3" @input="e => handleRgbInput('r', e)" />
            <label class="input-label">R</label>
          </div>
          <div class="input-group">
            <input class="color-input" :value="g" maxlength="3" @input="e => handleRgbInput('g', e)" />
            <label class="input-label">G</label>
          </div>
          <div class="input-group">
            <input class="color-input" :value="b" maxlength="3" @input="e => handleRgbInput('b', e)" />
            <label class="input-label">B</label>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import {
  Location
} from '@element-plus/icons-vue'

interface Props {
  modelValue?: string
}

interface Emits {
  (e: 'update:modelValue', color: string): void
  (e: 'change', color: string, source: 'preset' | 'advanced'): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '#000000'
})
const handleEmit = defineEmits<Emits>()

// ─── 预设颜色 ────────────────────────────────────────────────────────────────
const loadPresetColorList = [
  '#000000', '#434343', '#666666', '#999999', '#b7b7b7', '#cccccc', '#d9d9d9', '#efefef', '#f3f3f3', '#ffffff',
  '#ff0000', '#ff4500', '#ff9900', '#ffff00', '#00ff00', '#00ffff', '#4a86e8', '#0000ff', '#9900ff', '#ff00ff',
  '#f4cccc', '#fce5cd', '#fff2cc', '#d9ead3', '#d0e0e3', '#c9daf8', '#cfe2f3', '#d9d2e9', '#ead1dc', '#fce4ec',
  '#ea9999', '#f9cb9c', '#ffe599', '#b6d7a8', '#a2c4c9', '#a4c2f4', '#9fc5e8', '#b4a7d6', '#d5a6bd', '#f48fb1',
  '#e06666', '#f6b26b', '#ffd966', '#93c47d', '#76a5af', '#6d9eeb', '#6fa8dc', '#8e7cc3', '#c27ba0', '#f06292',
  '#cc0000', '#e69138', '#f1c232', '#6aa84f', '#45818e', '#3c78d8', '#3d85c8', '#674ea7', '#a61c00', '#c2185b',
  '#990000', '#b45f06', '#bf9000', '#38761d', '#134f5c', '#1155cc', '#0b5394', '#351c75', '#741b47', '#880e4f'
]

// ─── 状态 ────────────────────────────────────────────────────────────────────
const isAdvancedShow = ref(false)
const loadCanvasRef = ref<HTMLElement>()
const hueTrackRef = ref<HTMLElement>()
const alphaTrackRef = ref<HTMLElement>()

// HSV 模型
const hue = ref(0)         // 0-360
const saturation = ref(1)  // 0-1
const value = ref(1)       // 0-1 (brightness)
const alpha = ref(1)       // 0-1

// 光标位置（百分比）
const cursorX = computed(() => saturation.value * 100)
const cursorY = computed(() => (1 - value.value) * 100)

// ─── 颜色转换工具 ─────────────────────────────────────────────────────────────
function hsvToRgb(h: number, s: number, v: number) {
  const hi = Math.floor(h / 60) % 6
  const f = h / 60 - Math.floor(h / 60)
  const p = v * (1 - s)
  const q = v * (1 - f * s)
  const t = v * (1 - (1 - f) * s)
  const map = [
    [v, t, p], [q, v, p], [p, v, t],
    [p, q, v], [t, p, v], [v, p, q]
  ]
  const [r, g, b] = map[hi]
  return {
    r: Math.round(r * 255),
    g: Math.round(g * 255),
    b: Math.round(b * 255)
  }
}

function rgbToHsv(r: number, g: number, b: number) {
  r /= 255; g /= 255; b /= 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const d = max - min
  let h = 0
  const s = max === 0 ? 0 : d / max
  const v = max
  if (d !== 0) {
    if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6
    else if (max === g) h = ((b - r) / d + 2) / 6
    else h = ((r - g) / d + 4) / 6
  }
  return { h: h * 360, s, v }
}

function hexToRgb(hex: string) {
  const clean = hex.replace('#', '')
  if (clean.length !== 6) return null
  const n = parseInt(clean, 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

function rgbToHex(r: number, g: number, b: number) {
  return '#' + [r, g, b].map(v => v.toString(16).padStart(2, '0')).join('')
}

// ─── 计算属性 ─────────────────────────────────────────────────────────────────
const hueColor = computed(() => {
  const { r, g, b } = hsvToRgb(hue.value, 1, 1)
  return `rgb(${r},${g},${b})`
})

const currentRgb = computed(() => hsvToRgb(hue.value, saturation.value, value.value))

const currentHex = computed(() => {
  const { r, g, b } = currentRgb.value
  return rgbToHex(r, g, b)
})

const r = computed(() => currentRgb.value.r)
const g = computed(() => currentRgb.value.g)
const b = computed(() => currentRgb.value.b)

const hexInputValue = ref('')

// ─── 初始化 / 同步外部值 ──────────────────────────────────────────────────────
function syncFromHex(hex: string) {
  const rgb = hexToRgb(hex)
  if (!rgb) return
  const hsv = rgbToHsv(rgb.r, rgb.g, rgb.b)
  hue.value = hsv.h
  saturation.value = hsv.s
  value.value = hsv.v
  hexInputValue.value = hex.replace('#', '').toLowerCase()
}

watch(() => props.modelValue, (val) => {
  if (val) syncFromHex(val)
}, { immediate: true })

watch(currentHex, (hex) => {
  hexInputValue.value = hex.replace('#', '').toLowerCase()
})

// ─── 发射颜色变化 ─────────────────────────────────────────────────────────────
function handleColorPreview() {
  handleEmit('update:modelValue', currentHex.value)
}

function handleColorCommit(source: 'preset' | 'advanced' = 'advanced') {
  handleEmit('update:modelValue', currentHex.value)
  handleEmit('change', currentHex.value, source)
}

// ─── 预设颜色点击 ─────────────────────────────────────────────────────────────
function isActiveColor(color: string) {
  return color.toLowerCase() === currentHex.value.toLowerCase()
}

function handlePresetClick(color: string) {
  syncFromHex(color)
  handleColorCommit('preset')
}

// ─── 色域画布拖拽 ─────────────────────────────────────────────────────────────
function updateCanvasFromEvent(e: MouseEvent) {
  const rect = loadCanvasRef.value!.getBoundingClientRect()
  const x = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  const y = Math.max(0, Math.min(1, (e.clientY - rect.top) / rect.height))
  saturation.value = x
  value.value = 1 - y
}

function handleCanvasMousedown(e: MouseEvent) {
  updateCanvasFromEvent(e)
  handleColorPreview()
  const onMove = (ev: MouseEvent) => {
    updateCanvasFromEvent(ev)
    handleColorPreview()
  }
  const onUp = () => {
    handleColorCommit()
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// ─── 色相滑块拖拽 ─────────────────────────────────────────────────────────────
function updateHueFromEvent(e: MouseEvent) {
  const rect = hueTrackRef.value!.getBoundingClientRect()
  const x = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  hue.value = x * 360
}

function handleHueMousedown(e: MouseEvent) {
  updateHueFromEvent(e)
  handleColorPreview()
  const onMove = (ev: MouseEvent) => {
    updateHueFromEvent(ev)
    handleColorPreview()
  }
  const onUp = () => {
    handleColorCommit()
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// ─── 透明度滑块拖拽 ───────────────────────────────────────────────────────────
function updateAlphaFromEvent(e: MouseEvent) {
  const rect = alphaTrackRef.value!.getBoundingClientRect()
  const x = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  alpha.value = x
}

function handleAlphaMousedown(e: MouseEvent) {
  updateAlphaFromEvent(e)
  handleColorPreview()
  const onMove = (ev: MouseEvent) => {
    updateAlphaFromEvent(ev)
    handleColorPreview()
  }
  const onUp = () => {
    handleColorCommit()
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// ─── Hex 输入 ─────────────────────────────────────────────────────────────────
function handleHexInput(e: Event) {
  const val = (e.target as HTMLInputElement).value.replace(/[^0-9a-fA-F]/g, '')
  hexInputValue.value = val
  if (val.length === 6) {
    syncFromHex('#' + val)
    handleColorCommit()
  }
}

function handleHexBlur() {
  hexInputValue.value = currentHex.value.replace('#', '').toLowerCase()
}

// ─── RGB 输入 ─────────────────────────────────────────────────────────────────
function handleRgbInput(channel: 'r' | 'g' | 'b', e: Event) {
  const val = parseInt((e.target as HTMLInputElement).value)
  if (isNaN(val)) return
  const clamped = Math.max(0, Math.min(255, val))
  const rgb = { r: r.value, g: g.value, b: b.value }
  rgb[channel] = clamped
  const hsv = rgbToHsv(rgb.r, rgb.g, rgb.b)
  hue.value = hsv.h
  saturation.value = hsv.s
  value.value = hsv.v
  handleColorCommit()
}
</script>
