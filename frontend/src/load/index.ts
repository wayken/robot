import router from '@/router/index'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 配置页面加载进度条动画
NProgress.inc(0.2)
NProgress.configure({
  easing: 'ease',
  speed: 200,
  showSpinner: false
})
router.beforeEach((to, from, next) => {
  NProgress.start()
  next()
})

router.afterEach(() => {
  NProgress.done()
})
