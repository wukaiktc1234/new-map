/**
 * usePwaInstall — PWA 安装提示 Composable
 *
 * 检测 PWA 是否可安装，提供安装方法。
 * 覆盖两种场景：
 *   1. Android/Chrome: beforeinstallprompt 事件（原生安装弹窗）
 *   2. iOS/Safari: 引导用户手动"添加到主屏幕"
 *
 * 使用方式：
 *   const { canInstall, isIOS, showInstallPrompt, dismissInstall } = usePwaInstall()
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface UsePwaInstallReturn {
  /** 是否显示安装提示 */
  canInstall: ref<boolean>
  /** 是否为 iOS 设备 */
  isIOS: boolean
  /** 是否已作为 PWA 运行（已安装） */
  isStandalone: boolean
  /** 显示安装提示 */
  showInstallPrompt(): Promise<void>
  /** 关闭安装提示 */
  dismissInstall(): void
}

export function usePwaInstall(): UsePwaInstallReturn {
  const deferredPrompt = ref<Event | null>(null)
  const canInstall = ref(false)
  const dismissed = ref(false)
  const userDismissedKey = 'pwa-install-dismissed'

  // 检测是否 iOS
  const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent)

  // 检测是否已作为独立应用运行
  const isStandalone = computed(() => {
    return (
      window.matchMedia('(display-mode: standalone)').matches ||
      // @ts-expect-error — iOS Safari 特有属性
      window.navigator.standalone === true ||
      document.referrer.includes('android-app://')
    )
  })

  function handleBeforeInstallPrompt(e: Event) {
    // 阻止 Chrome 自动弹出安装横幅（我们自定义 UI）
    e.preventDefault()
    deferredPrompt.value = e
    canInstall.value = true
  }

  function handleAppInstalled() {
    canInstall.value = false
    deferredPrompt.value = null
  }

  async function showInstallPrompt(): Promise<void> {
    // Android/Chrome：触发原生安装弹窗
    if (deferredPrompt.value) {
      const promptEvent = deferredPrompt.value as any
      await promptEvent.prompt()
      const result = await promptEvent.userChoice
      if (result.outcome === 'accepted') {
        canInstall.value = false
      }
      deferredPrompt.value = null
      return
    }

    // iOS/Safari：无法自动触发安装，打开引导说明
    if (isIOS) {
      openIOSGuide()
    }
  }

  function dismissInstall() {
    dismissed.value = true
    canInstall.value = false
    // 记录用户关闭，24小时内不再提示
    try {
      sessionStorage.setItem(userDismissedKey, String(Date.now()))
    } catch {
      // 静默处理
    }
  }

  function openIOSGuide() {
    // iOS 无法通过 JS 触发安装，
    // 这里可以弹出一个引导对话框或跳转到帮助页面
    const guideText =
      '安装步骤：\n' +
      '1. 点击底部的 分享按钮\n' +
      '2. 选择"添加到主屏幕"\n' +
      '3. 点击"添加"\n' +
      '\n' +
      '即可将应用安装到桌面！'
    alert(guideText)
  }

  onMounted(() => {
    // 检查是否之前关闭过（24小时内）
    try {
      const dismissedTime = sessionStorage.getItem(userDismissedKey)
      if (dismissedTime) {
        const elapsed = Date.now() - Number(dismissedTime)
        if (elapsed < 24 * 60 * 60 * 1000) return // 24小时内不重复提示
      }
    } catch {
      // 静默处理
    }

    // 如果已经以 standalone 模式运行，不需要提示
    if (isStandalone.value) return

    // 监听 Chrome 安装事件
    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
    window.addEventListener('appinstalled', handleAppInstalled)

    // iOS Safari：没有 beforeinstallprompt 事件，
    // 但可以通过检测是否非 standalone 来判断是否需要引导安装
    if (isIOS && !isStandalone.value) {
      // 延迟 5 秒后显示提示（给用户时间浏览页面）
      setTimeout(() => {
        if (!dismissed.value && !isStandalone.value) {
          canInstall.value = true
        }
      }, 5000)
    }
  })

  onUnmounted(() => {
    window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
    window.removeEventListener('appinstalled', handleAppInstalled)
  })

  return {
    canInstall,
    isIOS,
    isStandalone: isStandalone.value,
    showInstallPrompt,
    dismissInstall,
  }
}
