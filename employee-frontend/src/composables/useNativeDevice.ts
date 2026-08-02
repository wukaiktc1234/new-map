/**
 * useNativeDevice - 原生设备能力封装
 *
 * 统一封装 Capacitor 原生插件与 Web API，实现双模式运行：
 * - APP 环境：使用 Capacitor 原生插件（高精度定位、真实网络状态等）
 * - 浏览器环境：降级为 Web API（HTML5 Geolocation、Network Info 等）
 *
 * @last-modified 2026-06-05
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { Capacitor, PluginListenerHandle } from '@capacitor/core'
import { Network as CapNetwork } from '@capacitor/network'
import { Haptics, ImpactStyle, NotificationType } from '@capacitor/haptics'
import { StatusBar, Style as StatusBarStyle } from '@capacitor/status-bar'
import { App as CapApp } from '@capacitor/app'

// ============================================
// 类型定义
// ============================================

export interface NativeLocation {
  latitude: number
  longitude: number
  accuracy?: number        // 精度（米）
  altitude?: number         // 海拔（米）
  timestamp: number         // 时间戳
}

export interface NetworkInfo {
  connected: boolean
  connectionType: 'wifi' | 'cellular' | 'none' | 'unknown'
  effectiveType?: '4g' | '3g' | '2g' | 'slow-2g'
}

export interface DeviceInfo {
  platform: 'android' | 'ios' | 'web'
  model: string              // 设备型号（如 "Pixel 8", "iPhone 15"）
  osVersion: string          // 操作系统版本
  isNative: boolean          // 是否运行在原生容器中
  appVersion: string         // APP 版本号
  uuid: string               // 设备唯一标识
}

// ============================================
// 平台检测
// ============================================

/** 是否运行在 Capacitor 原生容器中 */
export function isNativePlatform(): boolean {
  return Capacitor.isNativePlatform()
}

/** 获取当前平台标识 */
export function getPlatform(): DeviceInfo['platform'] {
  if (Capacitor.isNativePlatform()) {
    return Capacitor.getPlatform() === 'android' ? 'android' : 'ios'
  }
  return 'web'
}

// ============================================
// 定位服务（双模式）
// ============================================

const locationState = ref<'idle' | 'locating' | 'success' | 'error'>('idle')
const currentLocation = ref<NativeLocation | null>(null)
let locationWatchId: number | null = null

/**
 * 获取当前位置（单次）
 * APP 模式：使用系统原生定位（更高精度、更省电）
 * Web 模式：降级为 HTML5 Geolocation API
 */
export async function getCurrentLocation(options?: {
  enableHighAccuracy?: boolean
  timeout?: number
}): Promise<{ success: boolean; location?: NativeLocation; error?: string }> {
  locationState.value = 'locating'

  try {
    if (isNativePlatform()) {
      // TODO: 集成 @capacitor/geolocation 插件后启用
      // const coordinates = await Geolocation.getCurrentPosition(options)
      // return { success: true, location: { ... } }
      // 降级到 Web API
      return await getWebLocation(options)
    } else {
      return await getWebLocation(options)
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : '定位失败'
    locationState.value = 'error'
    return { success: false, error: msg }
  }
}

/** Web 定位降级方案 */
async function getWebLocation(options?: {
  enableHighAccuracy?: boolean
  timeout?: number
}): Promise<{ success: boolean; location?: NativeLocation; error?: string }> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      locationState.value = 'error'
      resolve({ success: false, error: '浏览器不支持定位服务' })
      return
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        currentLocation.value = {
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
          accuracy: pos.coords.accuracy,
          altitude: pos.coords.altitude ?? undefined,
          timestamp: pos.timestamp,
        }
        locationState.value = 'success'
        resolve({ success: true, location: currentLocation.value })
      },
      (err) => {
        let errorMsg = '定位失败'
        switch (err.code) {
          case err.PERMISSION_DENIED:
            errorMsg = '定位权限被拒绝，请在设置中开启'
            break
          case err.POSITION_UNAVAILABLE:
            errorMsg = '定位信息不可用'
            break
          case err.TIMEOUT:
            errorMsg = '定位超时，请重试'
            break
        }
        locationState.value = 'error'
        resolve({ success: false, error: errorMsg })
      },
      {
        enableHighAccuracy: options?.enableHighAccuracy ?? true,
        timeout: options?.timeout ?? 10000,
        maximumAge: 30000,
      },
    )
  })
}

// ============================================
// 网络状态（双模式）
// ============================================

const networkInfo = ref<NetworkInfo>({
  connected: navigator.onLine,
  connectionType: 'unknown',
})

let networkListener: PluginListenerHandle | null = null

/**
 * 初始化网络状态监听
 * APP 模式：使用 Capacitor Network 插件（实时准确）
 * Web 模式：降级为 navigator.onLine + connection API
 */
export async function initNetworkListener(): Promise<void> {
  if (isNativePlatform()) {
    networkListener = await CapNetwork.addListener('networkStatusChange', (status) => {
      networkInfo.value = {
        connected: status.connected,
        connectionType: mapCapConnectionType(status.connectionType),
        effectiveType: undefined,
      }
    })

    // 获取初始状态
    const status = await CapNetwork.getStatus()
    networkInfo.value = {
      connected: status.connected,
      connectionType: mapCapConnectionType(status.connectionType),
    }
  } else {
    // Web 降级：监听 online/offline 事件
    const updateFromWeb = () => {
      const conn = (navigator as unknown as Record<string, unknown>).connection as
        | { type?: string; effectiveType?: string; rtt?: number; downlink?: number }
        | undefined

      networkInfo.value = {
        connected: navigator.onLine,
        connectionType: conn?.type === 'wifi' ? 'wifi'
          : conn?.type?.startsWith('cellular') ? 'cellular'
            : navigator.onLine ? 'unknown' : 'none',
        effectiveType: conn?.effectiveType as NetworkInfo['effectiveType'],
      }
    }

    window.addEventListener('online', updateFromWeb)
    window.addEventListener('offline', updateFromWeb)
    updateFromWeb()
  }
}

/** 清理网络监听 */
export function removeNetworkListener(): void {
  if (networkListener) {
    networkListener.remove()
    networkListener = null
  }
}

function mapCapConnectionType(type: string): NetworkInfo['connectionType'] {
  const map: Record<string, NetworkInfo['connectionType']> = {
    wifi: 'wifi',
    cell: 'cellular',
    cellular: 'cellular',
    none: 'none',
    unknown: 'unknown',
  }
  return map[type] ?? 'unknown'
}

// ============================================
// 触觉反馈（仅 APP / 支持 Vibration API 的浏览器）
// ============================================

/**
 * 触发触觉反馈
 * - APP：使用 Capacitor Haptics（精确控制反馈类型）
 * - Web：降级为 Vibration API（如果支持）
 */
export async function hapticImpact(style: 'light' | 'medium' | 'heavy' = 'light'): Promise<void> {
  try {
    if (isNativePlatform()) {
      const styleMap: Record<string, ImpactStyle> = {
        light: ImpactStyle.Light,
        medium: ImpactStyle.Medium,
        heavy: ImpactStyle.Heavy,
      }
      await Haptics.impact({ style: styleMap[style] ?? ImpactStyle.Light })
    } else if ('vibrate' in navigator) {
      // Web Vibration API
      const durationMap = { light: 10, medium: 20, heavy: 30 }
      navigator.vibrate(durationMap[style] ?? 10)
    }
  } catch {
    // 静默失败，不影响主流程
  }
}

// ============================================
// 状态栏（仅 APP）
// ============================================

/**
 * 设置状态栏样式
 * 仅在 APP 环境下生效
 */
export async function setStatusBarStyle(darkContent: boolean): Promise<void> {
  if (!isNativePlatform()) return

  try {
    await StatusBar.setStyle({
      style: darkContent ? StatusBarStyle.Dark : StatusBarStyle.Light,
    })
  } catch {
    // 静默失败
  }
}

/**
 * 设置状态栏背景色
 * 仅在 APP 环境下生效
 */
export async function setStatusBarBackgroundColor(color: string): Promise<void> {
  if (!isNativePlatform()) return

  try {
    await StatusBar.setBackgroundColor({ color })
  } catch {
    // 静默失败
  }
}

// ============================================
// 设备信息（双模式）
// ============================================

/**
 * 获取设备信息
 * APP 模式：返回真实的设备型号、系统版本
 * Web 模式：返回基于 UserAgent 的估算值
 */
export async function getDeviceInfo(): Promise<DeviceInfo> {
  if (isNativePlatform()) {
    try {
      const { Device } = await import('@capacitor/device')
      const info = await Device.getInfo()
      const deviceId = (info as unknown as Record<string, unknown>).uuid as string | undefined
      return {
        platform: getPlatform(),
        model: info.model || 'Unknown',
        osVersion: info.osVersion || '',
        isNative: true,
        appVersion: '', // 从 config 获取
        uuid: deviceId || '',
      }
    } catch {
      return getWebDeviceInfo()
    }
  }

  return getWebDeviceInfo()
}

/** Web 模式的设备信息（基于 UA 解析） */
function getWebDeviceInfo(): DeviceInfo {
  const ua = navigator.userAgent
  let model = 'Browser'

  const iPhoneRegex = /iPhone/
  const androidRegex = /Android/
  const windowsRegex = /Windows/
  const macRegex = /Mac/

  if (iPhoneRegex.test(ua)) {
    const match = ua.match(/iPhone OS ([\d_]+)/)
    model = 'iPhone (' + (match?.[1] ?? 'Unknown') + ')'
  } else if (androidRegex.test(ua)) {
    const match = ua.match(/Android ([\d.]+);? (\S+)?/)
    model = match?.[2] ?? ('Android (' + (match?.[1] ?? 'Unknown') + ')')
  } else if (windowsRegex.test(ua)) {
    model = 'Windows PC'
  } else if (macRegex.test(ua)) {
    model = 'macOS'
  }

  return {
    platform: 'web',
    model,
    osVersion: navigator.platform || '',
    isNative: false,
    appVersion: '1.0.0 (Web)',
    uuid: '',
  }
}

// ============================================
// APP 退出确认（仅 APP）
// ============================================

/**
 * 注册返回键拦截（Android 物理返回键）
 * 在需要确认退出的页面调用此方法
 */
export async function registerBackButtonHandler(
  callback: () => Promise<boolean>,
): Promise<(() => void) | null> {
  if (!isNativePlatform()) return null

  let removed = false
  const handle = await CapApp.addListener('backButton', async ({ canGoBack }) => {
    if (canGoBack) {
      // 有历史记录，执行默认返回行为（不阻止）
      return
    }
    // 无历史记录，调用自定义处理
    const shouldExit = await callback()
    if (shouldExit) {
      CapApp.exitApp()
    }
  })

  return () => {
    if (!removed) {
      handle.remove()
      removed = true
    }
  }
}

// ============================================
// Vue Composable 封装
// ============================================

/**
 * useNativeDevice - Vue 组合式函数
 *
 * 提供响应式的设备能力状态，可在组件中直接使用。
 *
 * @example
 * ```vue
 * <script setup>
 * const { network, locationStatus, device, isApp } = useNativeDevice()
 * </script>
 *
 * <template>
 *   <div v-if="!network.connected">网络不可用</div>
 *   <span>{{ device.model }}</span>
 * </template>
 * ```
 */
export function useNativeDevice() {
  onMounted(() => {
    initNetworkListener()
  })

  onUnmounted(() => {
    removeNetworkListener()
  })

  return {
    /** 网络状态（响应式） */
    network: networkInfo,
    /** 定位状态（响应式） */
    locationStatus: locationState,
    /** 当前位置（响应式） */
    location: currentLocation,
    /** 是否运行在原生容器中 */
    isApp: isNativePlatform(),
    /** 平台类型 */
    platform: getPlatform(),

    // 方法
    getCurrentLocation,
    initNetworkListener,
    hapticImpact,
    setStatusBarStyle,
    setStatusBarBackgroundColor,
    getDeviceInfo,
    registerBackButtonHandler,
  }
}
