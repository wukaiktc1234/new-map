/**
 * Capacitor 运行时全局声明
 *
 * @capacitor/core 只在内部提供 WindowCapacitor 类型，未对 window.Capacitor 做全局声明，
 * 而本项目的 web 包在纯浏览器环境（非原生 App）下运行时该对象不存在，
 * 因此按实际使用面（仅做原生平台判定）补充最小化的可选声明。
 */
export {}

declare global {
  interface Window {
    Capacitor?: {
      /** 是否运行在原生 App（APK/IPA）容器内 */
      isNativePlatform?: () => boolean
      /** 当前平台标识，如 android / ios / web */
      getPlatform?: () => string
    }
  }
}
