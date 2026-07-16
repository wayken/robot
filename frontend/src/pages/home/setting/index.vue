<template>
  <div class="a-setting inline-flex-c-n-n">
    <div class="card">
      <div class="field">
        <div class="title">{{ $t('setting.language') }}</div>
        <div class="value">
          <el-dropdown size="large" :teleported="false"
            @command="handleLangSwitch"
          >
          <span class="el-dropdown-link">
            {{ loadCurrentLocalLang }}<el-icon><ArrowRight /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="(data, key) in langList" :key="key"
                :command="data.key"
              >
                {{ data.description }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    <div class="card">
      <div class="field">
        <div class="title">{{ $t('setting.appearance') }}</div>
        <div class="value">
          <a-appearance @click="handleAppearanceSwitch"></a-appearance>
        </div>
      </div>
      <div class="field">
        <div class="label">{{ $t('setting.theme') }}</div>
        <div class="value theme">
          <div :class="key" v-for="(key) in Object.keys(themeList)" :key="key"
            @click="handleThemeSwitch(key)"
          >
            <el-icon :size="20" v-if="loadCurrentTheme == key">
              <Check />
            </el-icon>
          </div>
        </div>
      </div>
    </div>
    <div class="card">
      <div class="field">
        <div class="title">{{ $t('setting.about') }}</div>
        <div class="value" @click="isAboutView = true">
          <span class="name">{{ $t('extension.view') }}</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
    <!-- 关于弹窗 -->
    <a-about :visible="isAboutView" @close="isAboutView = false" />
  </div>
</template>

<script setup lang="ts">
import {
  Check,
  ArrowRight
} from '@element-plus/icons-vue'
import Mitter from '@/utils/mitt'
import langList from '@/locale/lang.json'
import Constant from '@/config/constant'
import runtime from '@/platform/runtime'
import { setLocalLang } from '@/utils/locale'
import AAppearance from '@/components/common/appearance/index.vue'
import AAbout from '@/components/home/setting/about.vue'

// 应用程序配置
const loadCurrentTheme = ref(localStorage.getItem(Constant.LOCAL_THEME) || 'blue')
const loadCurrentAppearance = ref(localStorage.getItem(Constant.LOCAL_APPERANCE) || 'light')
// 主题配置列表
const themeList: any = {
  'blue': {
    '--var-theme-primary-1': 'rgba(64, 158, 255, 0.1)',
    '--var-theme-primary-2': 'rgba(64, 158, 255, 0.5)',
    '--var-theme-primary-3': 'rgba(64, 158, 255, 0.8)',
    '--var-theme-primary-4': '#409eff',
    '--var-theme-primary-5': '#2563eb',
    '--var-theme-primary-6': '#1d4ed8',
    '--var-theme-primary-7': '#1e40af',
    '--var-theme-primary-8': '#1e3a8a'
  },
  'emerald': {
    '--var-theme-primary-1': 'rgba(6, 185, 129, 0.1)',
    '--var-theme-primary-2': 'rgba(6, 185, 129, 0.5)',
    '--var-theme-primary-3': 'rgba(6, 185, 129, 0.8)',
    '--var-theme-primary-4': '#10b981',
    '--var-theme-primary-5': '#059669',
    '--var-theme-primary-6': '#047857',
    '--var-theme-primary-7': '#065F46',
    '--var-theme-primary-8': '#064E3B'
  },
  'rose': {
    '--var-theme-primary-1': 'rgba(255, 29, 72, 0.1)',
    '--var-theme-primary-2': 'rgba(255, 29, 72, 0.5)',
    '--var-theme-primary-3': 'rgba(255, 29, 72, 0.8)',
    '--var-theme-primary-4': '#F43F5E',
    '--var-theme-primary-5': '#E11D48',
    '--var-theme-primary-6': '#BE123C',
    '--var-theme-primary-7': '#9F1239',
    '--var-theme-primary-8': '#881337'
  },
  'orange': {
    '--var-theme-primary-1': 'rgba(234, 88, 12, 0.1)',
    '--var-theme-primary-2': 'rgba(234, 88, 12, 0.5)',
    '--var-theme-primary-3': 'rgba(234, 88, 12, 0.8)',
    '--var-theme-primary-4': '#f97316',
    '--var-theme-primary-5': '#EA580C',
    '--var-theme-primary-6': '#C2410C',
    '--var-theme-primary-7': '#9A3412',
    '--var-theme-primary-8': '#7C2D12'
  }
}
const isAboutView = ref(false)

// 页面加载时的设置初始化
onMounted(() => {
  nextTick(() => {
    const dayNightCheckbox = document.querySelector('#day-night-checkbox') as HTMLInputElement
    if (dayNightCheckbox) {
      dayNightCheckbox.checked = loadCurrentAppearance.value === 'dark'
    }
  })
})

// 切换多语言
const i18n = useI18n()
const loadCurrentLocalLang = computed(() => {
  for (const data of langList) {
    if (data.key === i18n.locale.value) {
      return data.description
    }
  }
  return langList[0].description
})
const handleLangSwitch = (locale: string) => {
  if (runtime.isApplication()) {
    runtime.handleApplicationEventSend('on-window-language-switch', locale)
  } else {
    i18n.locale.value = locale
    setLocalLang(locale)
  }
}
// 变换外观
const handleAppearanceSwitch = (payload: boolean) => {
  if (runtime.isApplication()) {
    runtime.handleApplicationEventSend('on-window-appearance-switch', payload)
  } else {
    Mitter.emit('mitt-appearance-switch', payload)
  }
}
// 变换主题
const handleThemeSwitch = (payload: any) => {
  if (runtime.isApplication()) {
    runtime.handleApplicationEventSend('on-window-theme-switch', payload)
  } else {
    Mitter.emit('mitt-theme-switch', payload)
  }
  loadCurrentTheme.value = payload
}
</script>
