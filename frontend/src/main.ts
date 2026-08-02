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
