/**
 * [M10] 敏感页面截屏/录屏防护（Android FLAG_SECURE）
 *
 * 在 Capacitor 原生环境中，通过原生插件设置 FLAG_SECURE 防止截屏/录屏。
 * 在 Web 环境中无法阻止浏览器截图，但可通过 CSS 类添加视觉水印提示。
 *
 * @param enabled - 是否启用防护模式
 *
 * 使用方式：
 * ```ts
 * import { useScreenSecurity } from '@/composables/useScreenSecurity'
 *
 * onMounted(() => { useScreenSecurity(true) })
 * onUnmounted(() => { useScreenSecurity(false) })
 * ```
 */
export function useScreenSecurity(enabled: boolean): void {
  if (enabled) {
    document.body.classList.add('screen-secure-mode')
  } else {
    document.body.classList.remove('screen-secure-mode')
  }
}
