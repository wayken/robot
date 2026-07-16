<template>
  <router-view v-slot="{ Component }">
    <keep-alive>
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<script setup lang="ts">
import Mitter from '@/utils/mitt'
import Constant from '@/config/constant'
import runtime from '@/platform/runtime'
import { setLocalLang } from '@/utils/locale'
import { useAppStore } from '@/store/modules/app'

// 外观配置列表
const appearanceList: any = {
  'dark': {
    '--var-page-background-color': '#111217',
    '--var-menu-border-color': '#24272d',
    '--var-menu-actived-color': '#111217',
    '--var-default-font-color': '#f0f0f5',
    '--var-module-hover-color': '#000000',
    '--var-module-background-color': '#111217',
    '--var-card-background-color': '#181b1f',
    '--var-canvas-background0-color': '#111217',
    '--var-canvas-background1-color': '#282828',
    '--var-selection-background-color': '#2c3440',
    '--var-scrollbar-background-color': '#2f3036',
    '--var-loading-background-color': 'rgba(24, 27, 31, 0.8)'
  },
  'light': {
    '--var-page-background-color': '#fafbfc',
    '--var-menu-border-color': '#eeeeee',
    '--var-menu-actived-color': '#fafafa',
    '--var-default-font-color': '#24292e',
    '--var-module-hover-color': '#f4f5f5',
    '--var-module-background-color': '#f4f5f5',
    '--var-card-background-color': '#ffffff',
    '--var-canvas-background0-color': '#e3e7e9',
    '--var-canvas-background1-color': '#f6fafc',
    '--var-selection-background-color': '#ebf5ff',
    '--var-scrollbar-background-color': '#d3d5d7',
    '--var-loading-background-color': 'rgba(255, 255, 255, 0.8)'
  }
}

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

const i18n = useI18n()
const router = useRouter()
const loadCurrentTheme = ref(localStorage.getItem(Constant.LOCAL_THEME) || 'blue')
const currentAppearance = ref(localStorage.getItem(Constant.LOCAL_APPERANCE) || 'light')

onMounted(() => {
  // 监听语言切换
  Mitter.on('mitt-appearance-switch', handleAppearanceSwitch)
  runtime.handleApplicationEventOn('on-window-language-switch', (data: any) => {
    handleLangSwitch(data)
  })
  // 监听主题、外观切换
  Mitter.on('mitt-theme-switch', handleThemeSwitch)
  runtime.handleApplicationEventOn('on-window-appearance-switch', (data: any) => {
    handleAppearanceSwitch(data)
  })
  runtime.handleApplicationEventOn('on-window-theme-switch', (data: any) => {
    handleThemeSwitch(data)
  })
  // 监听跨窗口参数传递，进行逻辑解析
  runtime.handleApplicationEventOn('on-window-parameter-transfer', (data: any) => {
    const parameters = JSON.parse(data)
    if (parameters.preset) {
      router.push({ name: 'draw-txt2img' })
    }
  })
  // 初始化窗口样式
  window.document.title = i18n.t('portal.name')
  handleAppearanceSwitch(currentAppearance.value === 'dark')
  handleThemeSwitch(loadCurrentTheme.value)
  // 加载应用程序配置
  const settingData = runtime.handleLoadAppSetting()
  if (settingData) {
    useAppStore().dispatchSetSettingData(settingData)
  }
  // 程序启动时初始化设备ID，建立唯一标识方便和服务端通信
  handleDeviceIdInit()
})

const declaration = (<CSSStyleRule> document.styleSheets[0].cssRules[0]).style
// 切换语言
const handleLangSwitch = (locale: string) => {
  i18n.locale.value = locale
  setLocalLang(locale)
}
// 初始化设备ID
const handleDeviceIdInit = () => {
  // 获取本地设备ID
  let deviceId = runtime.handleLoadMachineId()
  const appStore = useAppStore()
  appStore.dispatchSetDeviceId(deviceId)
  // 获取本地MAC地址
  const macId = runtime.handleLoadMacId()
  appStore.dispatchSetMacId(macId)
}
// 变换外观
const handleAppearanceSwitch = (payload: any) => {
  const theme = payload ? 'dark' : 'light'
  const appearance = appearanceList[theme]
  for (let key in appearance) {
    const value = appearance[key]
    declaration.setProperty(key, value)
  }
  localStorage.setItem(Constant.LOCAL_APPERANCE, theme)
}
// 变换主题
const handleThemeSwitch = (payload: any) => {
  const theme = themeList[payload]
  for (let key in theme) {
    const value = theme[key]
    declaration.setProperty(key, value)
  }
  const appStore = useAppStore()
  loadCurrentTheme.value = payload
  appStore.dispatchSetTheme(payload)
  localStorage.setItem(Constant.LOCAL_THEME, payload)
}
// 路由切换时更新浏览器标题
const handleDocumentTitleChange = () => {
  const currentRoute = router.currentRoute.value
  let route: any = currentRoute
  if (currentRoute.matched[1]) {
    route = currentRoute.matched[1]
  }
  if (route.meta.lang) {
    window.document.title = i18n.t(route.meta.lang) + ' - ' + i18n.t('portal.name')
  }
}

// 路由切换时更新浏览器标题
watch(() => router.currentRoute.value, () => {
  handleDocumentTitleChange()
}, { immediate: true })
</script>
