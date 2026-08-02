/**
 * Vitest 全局测试 Setup
 *
 * 在每个测试文件执行前运行，负责：
 * 1. 注册 Element Plus 组件全局可用
 * 2. 安装 Pinia 状态管理（组件内 useXxxStore() 需要）
 * 3. 清理挂载残留
 *
 * @last-modified 2026-06-04
 */
import { beforeAll, afterEach } from 'vitest'
import { config } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// ===== Pinia 实例 =====
const pinia = createPinia()

// ===== Element Plus + Pinia 全局注册 =====
beforeAll(() => {
  config.global.plugins = [ElementPlus, pinia]
})

// ===== afterEach 清理 =====
afterEach(() => {
  // 清理 document.body 上残留的挂载元素
  document.body.innerHTML = ''
})
