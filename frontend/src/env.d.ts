/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// WangEditor for Vue3 类型声明
declare module '@wangeditor/editor-for-vue' {
  import type { DefineComponent } from 'vue'
  export const Editor: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export const Toolbar: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
}

// 扩展 import.meta.env 类型，支持自定义环境变量
interface ImportMetaEnv {
  /** 应用标题 */
  readonly VITE_APP_TITLE?: string
  /** API 基础路径 */
  readonly VITE_API_BASE_URL?: string
  /** 是否启用调试模式 */
  readonly VITE_DEBUG?: string
  /**
   * 是否启用 Mock 模式（显式 opt-in）
   * - 'true' 时启用：后端不可用自动回退到 mock 数据
   * - 默认未设置或 'false'：始终使用真实后端 API
   * - 生产环境构建时此变量被忽略（生产永不启用 mock）
   */
  readonly VITE_USE_MOCK?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
