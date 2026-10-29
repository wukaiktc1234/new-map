/**
 * POS 收银端个人偏好（纯前端展示层，不涉及后端逻辑）
 *
 * 只放"改了立刻能感知、且不影响金额/账务"的开关：
 *   ① enterConfirmCheckout —— 结账弹窗内回车即确认（默认开，高频键盘流依赖它）；
 *   ② fontScale            —— 界面缩放（标准 / 大），收银机屏幕小、站立视距远时放大；
 *   ③ confirmDestructive   —— 高危操作二次确认，安全策略，恒为开启且不可关闭。
 *
 * 存储：localStorage(pos-preferences)，跨刷新保持；解析失败回落默认值，绝不阻塞收银。
 */

import { reactive, readonly } from 'vue'

const PREFS_KEY = 'pos-preferences'

export type FontScale = 'normal' | 'large'

export interface PosPreferences {
  /** 结账弹窗内回车直接确认 */
  enterConfirmCheckout: boolean
  /** 界面缩放 */
  fontScale: FontScale
  /** 高危操作二次确认：安全策略，只读恒 true */
  confirmDestructive: boolean
}

const DEFAULTS: PosPreferences = {
  enterConfirmCheckout: true,
  fontScale: 'normal',
  confirmDestructive: true,
}

const state = reactive<PosPreferences>({ ...DEFAULTS })

/** 把缩放落到 DOM（html[data-pos-font-scale]），CSS 在 styles/pos/base.css 里生效 */
function applyFontScale(scale: FontScale): void {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-pos-font-scale', scale)
}

function persist(): void {
  try {
    localStorage.setItem(
      PREFS_KEY,
      JSON.stringify({
        enterConfirmCheckout: state.enterConfirmCheckout,
        fontScale: state.fontScale,
      }),
    )
  } catch {
    // 隐私模式 / 配额满：只在内存生效，不阻断收银
  }
}

/** 应用启动时调用一次：从 localStorage 恢复偏好并作用到 DOM */
export function initPosPreferences(): void {
  try {
    const raw = localStorage.getItem(PREFS_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<PosPreferences> | null
      if (parsed && typeof parsed === 'object') {
        if (typeof parsed.enterConfirmCheckout === 'boolean') {
          state.enterConfirmCheckout = parsed.enterConfirmCheckout
        }
        if (parsed.fontScale === 'large' || parsed.fontScale === 'normal') {
          state.fontScale = parsed.fontScale
        }
      }
    }
  } catch {
    // 脏数据回落到默认值
  }
  // 二次确认永远开启，忽略任何持久化的关闭意图
  state.confirmDestructive = true
  applyFontScale(state.fontScale)
}

export function usePosPreferences() {
  function setEnterConfirmCheckout(value: boolean): void {
    state.enterConfirmCheckout = value
    persist()
  }

  function setFontScale(value: FontScale): void {
    state.fontScale = value
    applyFontScale(value)
    persist()
  }

  function reset(): void {
    state.enterConfirmCheckout = DEFAULTS.enterConfirmCheckout
    state.fontScale = DEFAULTS.fontScale
    state.confirmDestructive = true
    applyFontScale(state.fontScale)
    persist()
  }

  return {
    prefs: readonly(state) as Readonly<PosPreferences>,
    setEnterConfirmCheckout,
    setFontScale,
    reset,
  }
}
