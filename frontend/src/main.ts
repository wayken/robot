import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import App from './App.vue'
import router from './router'
import i18n from './locale'
import './load'
// import './mock'
import pinia from '@/store/index'
import directives from '@/directives/index'
import ASvgIcon from '@/components/common/svgicon/index.vue'
import ANodata from '@/components/common/nodata/index.vue'
import ADivider from '@/components/common/divider/index.vue'
import AButton from '@/components/common/button/index.vue'
import '@/theme/index.scss'
import 'element-plus/dist/index.css'
import 'virtual:svg-icons-register'

const app = createApp(App)
app.component(<string> ASvgIcon.name, ASvgIcon)
app.component(<string> ANodata.name, ANodata)
app.component(<string> ADivider.name, ADivider)
app.component(<string> AButton.name, AButton)
app.use(router).use(i18n).use(pinia).use(directives).use(ElementPlus).mount('#app')
