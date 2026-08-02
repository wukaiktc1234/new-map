/// <reference types="vite/client" />

declare module 'sockjs-client' {
  const SockJS: new (url: string, options?: Record<string, unknown>) => {
    onopen: (() => void) | null
    onclose: (() => void) | null
    onmessage: ((event: { data: string }) => void) | null
    close: () => void
  }
  export default SockJS
}

declare module 'element-plus/dist/locale/zh-cn.mjs' {
  import type { Language } from 'element-plus/es/locale'
  const zhCn: Language
  export default zhCn
}
