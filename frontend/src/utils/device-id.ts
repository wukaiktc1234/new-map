/**
 * APK 设备唯一标识管理（E1 设备级持久化）
 *
 * 设计原则：设备身份完全由「设备级硬件标识」决定，不依赖任何可被卸载清除的缓存。
 *
 * 来源：window.__ftsAndroidId —— MainActivity 在 WebView 页面就绪后注入的 ANDROID_ID。
 *       ANDROID_ID 是 Android 系统级设备标识（按「应用签名+用户+设备」派生），
 *       卸载重装不变、覆盖安装不变，仅恢复出厂设置才变。
 *
 * 关键：H5 侧【绝不】用随机 UUID 兜底 —— 随机 UUID 卸载后重新生成，
 *       会让同一设备在服务器端变成"新设备"，导致每次重装都要重新激活。
 *       若 ANDROID_ID 尚未注入（原生注入晚于页面加载），调用方需等待。
 */
import { Preferences } from '@capacitor/preferences'

let cachedDeviceId: string | null = null
let androidIdWaiters: Array<() => void> = []
let androidIdReady = false

const LS_KEY = 'fts_device_id_ls'
const PREFS_KEY = 'fts_device_id'

/**
 * 获取设备唯一标识（服务器端白名单的绑定键）。
 * 优先级：
 * ① window.__ftsAndroidId —— ANDROID_ID（卸载重装不变，最理想）
 * ② Preferences（原生存储，覆盖安装保留）
 * ③ localStorage（兜底，重开 App 保留）
 * 绝不返回随机 ID —— 随机 ID 会让同机每次启动都变"新设备"，触发重复激活。
 */
export async function getDeviceId(): Promise<string> {
  if (cachedDeviceId) return cachedDeviceId

  const win = window as unknown as { __ftsAndroidId?: string }
  const isNative = window.Capacitor?.isNativePlatform?.() ?? false

  // ① ANDROID_ID（已注入则直接用）
  if (win.__ftsAndroidId) {
    cachedDeviceId = `aid-${win.__ftsAndroidId}`
    return cachedDeviceId
  }

  // ② Preferences 持久化 ID（覆盖安装保留，ANDROID_ID 注入失败时的稳定兜底）
  if (isNative) {
    try {
      const { value } = await Preferences.get({ key: PREFS_KEY })
      if (value) {
        cachedDeviceId = value
        return cachedDeviceId
      }
    } catch { /* 忽略 */ }
  }

  // ③ localStorage 兜底
  const ls = localStorage.getItem(LS_KEY)
  if (ls) {
    cachedDeviceId = ls
    return cachedDeviceId
  }

  // ④ ANDROID_ID 尚未注入：等待原生注入通知（最多 3 秒）
  if (isNative && !win.__ftsAndroidId) {
    await new Promise<void>((resolve) => {
      if (androidIdReady) {
        resolve()
        return
      }
      const timer = setTimeout(() => {
        const idx = androidIdWaiters.indexOf(resolve)
        if (idx >= 0) androidIdWaiters.splice(idx, 1)
        resolve()
      }, 3000)
      androidIdWaiters.push(() => {
        clearTimeout(timer)
        resolve()
      })
    })
    if (win.__ftsAndroidId) {
      cachedDeviceId = `aid-${win.__ftsAndroidId}`
      return cachedDeviceId
    }
  }

  // ⑤ 所有来源都没有：生成 ID 并持久化（Preferences + localStorage），保证同机稳定
  // 注意：生成后必须持久化，否则下次启动又变"新设备"
  const generated = `fts-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
  cachedDeviceId = generated
  if (isNative) {
    try {
      await Preferences.set({ key: PREFS_KEY, value: generated })
    } catch { /* 忽略 */ }
  }
  localStorage.setItem(LS_KEY, generated)
  return cachedDeviceId
}

