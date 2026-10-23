import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/variables.css'
import './styles/index.css'
import './styles/pos/variables.css'
import './styles/pos/base.css'

// 检测是否为触摸设备
const isTouchDevice = () => {
  return 'ontouchstart' in window || 
         navigator.maxTouchPoints > 0 || 
         (navigator as any).msMaxTouchPoints > 0
}

// 只在触摸设备上注册双击防抖
if (isTouchDevice()) {
  let lastTouchEnd = 0
  document.addEventListener('touchend', (event) => {
    const target = event.target as HTMLElement
    if (target.closest('.qty-number')) {
      return
    }
    const now = Date.now()
    if (now - lastTouchEnd <= 300) {
      event.preventDefault()
    }
    lastTouchEnd = now
  }, { passive: false })
}

const app = createApp(App)

// ============================================================
// 全局兜底错误处理
// 业务代码统一走 src/utils/logger（dev 打印 / prod 静默）；
// 这里是保留 console.error 的兜底出口，保证生产环境也能看到真实堆栈。
// ============================================================
app.config.errorHandler = (err, _instance, info) => {
  console.error('[global-error] 未捕获的组件错误:', info, err)
}

window.addEventListener('unhandledrejection', (event) => {
  console.error('[global-error] 未处理的 Promise 拒绝:', event.reason)
})

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
