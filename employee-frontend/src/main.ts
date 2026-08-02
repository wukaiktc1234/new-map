// ========== 入口：确保在 import 前有同步执行代码 ==========
;(window as any).__empInit = true

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import './styles/index.scss'

// 注册全局指令
import vRipple from './directives/vRipple'
import vLazyLoad from './directives/vLazyLoad'

/** 是否为开发模式（含 APP 调试模式） */
const isDev = import.meta.env.DEV
/** 是否为 APP 打包模式 */
const isAppMode = import.meta.env.VITE_APP_MODE === '1'

try {
  // ========== 步骤1：主题初始化（必须在 mount 前同步执行）==========
  const THEME_STORAGE_KEY = 'emp-theme'
  type ThemeMode = 'system' | 'light' | 'dark'
  const saved = localStorage.getItem(THEME_STORAGE_KEY) as ThemeMode | null
  const mode: ThemeMode = (saved === 'light' || saved === 'dark' || saved === 'system') ? saved : 'system'
  const root = document.documentElement
  const isDarkMode = mode === 'dark' || (mode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)
  root.classList.toggle('dark', isDarkMode)
  root.setAttribute('data-theme', isDarkMode ? 'dark' : 'light')
  const metaThemeColor = document.querySelector('meta[name="theme-color"]')
  if (metaThemeColor) metaThemeColor.setAttribute('content', isDarkMode ? '#1a1a2e' : '#ffffff')

  // ========== 步骤2：创建 Vue 应用实例 ==========
  const app = createApp(App)

  // ========== 步骤3：注册插件 ==========
  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })
  app.directive('ripple', vRipple)
  app.directive('lazy-load', vLazyLoad)

  // ========== 步骤4：错误处理 ==========
  if (isDev) {
    app.config.errorHandler = (err, _instance, info) => {
      const detail = `错误: ${err instanceof Error ? err.message : String(err)}\n来源: ${info}\n${err instanceof Error ? err.stack || '' : ''}`
      console.error('[GlobalErrorHandler]', detail)
    }
    window.onerror = (message, source, lineno, colno, error) => {
      const detail = `消息: ${message}\n文件: ${source}\n行号: ${lineno}:${colno}\n${error?.stack || ''}`
      console.error('[WindowOnError]', detail)
      return false
    }
    window.onunhandledrejection = (event) => {
      console.error('[UnhandledRejection]', event.reason)
      event.preventDefault()
    }
  }

  // ========== 步骤5：挂载应用 ==========
  // 必须等待路由初始导航完成后再挂载，否则 route.meta 尚未填充，
  // isPublicPage 等计算属性返回错误值，RouterView 无匹配组件，导致空白
  router.isReady().then(() => {
    app.mount('#app')

    // 标记 Vue 已成功挂载，阻止 index.html 中的空白自愈脚本触发
    ;(window as any).__blankHealAttempted = true

    if (isDev) {
      console.log('[Emp] App mounted successfully! #app length:', (document.getElementById('app')?.innerHTML || '').length)
    }
  }).catch((mountErr: unknown) => {
    console.error('[Emp] router.isReady() failed:', mountErr)
    // 降级：即使路由初始化失败也尝试挂载，避免永久空白
    app.mount('#app')
    ;(window as any).__blankHealAttempted = true
  })

  // ========== 步骤6：PWA 空白页面自动修复（仅 iOS PWA 模式） ==========
  // iOS PWA "添加到主屏幕"后首次打开可能因缓存不完整导致空白
  // 检测到空白内容时自动重载一次（sessionStorage 防止无限循环）
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches
    || (window.navigator as any).standalone === true
  if (isStandalone && !navigator.onLine) {
    setTimeout(() => {
      const appEl = document.getElementById('app')
      const contentLen = (appEl?.innerHTML || '').length
      if (contentLen < 100) {
        const key = 'emp_pwa_reload_' + Date.now().toString().slice(0, 8)
        if (!sessionStorage.getItem(key)) {
          sessionStorage.setItem(key, '1')
          console.warn('[Emp] PWA 检测到空白页面(content=' + contentLen + ')，自动重载...')
          window.location.reload()
        }
      }
    }, 1500)
  }

  // ========== 步骤7：Service Worker 管理 ==========
  // APP 模式下绝对禁止注册或保留任何 Service Worker
  // 浏览器/PWA 模式下由 VitePWA 插件自动注入注册代码（injectRegister: 'auto'）
  const isCapacitorNative = !!(window as any).Capacitor?.isNativePlatform?.()

  if (isCapacitorNative || isAppMode) {
    // APP 环境：主动卸载所有已注册的 Service Worker（防御性清理）
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.getRegistrations().then(registrations => {
        registrations.forEach(reg => {
          console.warn('[Emp] APP模式: 卸载残留SW:', reg.scope)
          reg.unregister()
        })
      }).catch(() => {})
    }
  }
  // 浏览器/PWA 模式：无需手动注册，VitePWA 插件通过 injectRegister:'auto' 自动注入

} catch(e: unknown) {
  const errMsg = e instanceof Error ? `${e.message}\n${e.stack || ''}` : String(e)
  console.error('[Emp] FATAL:', errMsg)
}
