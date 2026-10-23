import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import './styles/index.scss'

import App from './App.vue'
import router from './router'
import permissionDirective from './directives/permission'

// 导入所有模块注册（菜单+未来可能的路由注册）
import { registerAllMenus } from '@/modules'

const app = createApp(App)

// ============================================================
// 全局兜底错误处理
// 业务代码统一走 @/utils/logger（dev 打印 / prod 静默）；
// 这里是唯一保留 console.error 的兜底出口之一（另一个是 api/request 拦截器），
// 用于保证生产环境也能在控制台看到真实堆栈，不影响业务逻辑。
// ============================================================
app.config.errorHandler = (err, _instance, info) => {
  console.error('[global-error] 未捕获的组件错误:', info, err)
}

window.addEventListener('unhandledrejection', (event) => {
  console.error('[global-error] 未处理的 Promise 拒绝:', event.reason)
})

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
registerAllMenus()
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 注：fixed-popper 插件已移除
// 原因：强制 strategy: 'fixed' 会导致普通页面下拉菜单在滚动时不停重算位置（飘动）
// Element Plus 默认 strategy: 'absolute' 已能正确处理滚动场景
// Dialog 内的 popper 漂移问题通过 :teleported="false" + .el-dialog__body { position: relative } 解决

// 注册全局权限控制指令
app.directive('permission', permissionDirective)

app.mount('#app')
