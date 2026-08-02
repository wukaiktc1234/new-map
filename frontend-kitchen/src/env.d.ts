/// <reference types="vite/client" />

declare module 'element-plus/dist/locale/zh-cn.mjs' {
  import type { Language } from 'element-plus/es/locale'
  const zhCn: Language
  export default zhCn
}

declare module 'sockjs-client' {
  class SockJS {
    constructor(url: string, options?: Record<string, unknown>)
    close(): void
    onopen: ((event: Event) => void) | null
    onclose: ((event: CloseEvent) => void) | null
    onerror: ((event: Event) => void) | null
    onmessage: ((event: MessageEvent) => void) | null
    readyState: number
  }
  export default SockJS
}
