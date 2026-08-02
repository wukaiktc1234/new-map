/**
 * UI 动画与延迟时间常量
 * 统一管理项目中所有的时间相关魔法数字
 */

/** 极短延迟 — 用于状态切换反馈（如按钮点击后禁用瞬间） */
export const UI_DELAY_INSTANT = 50

/** 短延迟 — 用于 Toast 关闭、轻量提示消失 */
export const UI_DELAY_SHORT = 300

/** 中等延迟 — 用于页面跳转前的等待、操作成功后的跳转 */
export const UI_DELAY_MEDIUM = 800

/** 长延迟 — 用于复杂操作的等待、自动跳转 */
export const UI_DELAY_LONG = 3000

/** 轮询间隔 — 用于验证码倒计时、状态轮询 */
export const POLL_INTERVAL = 1000

/** 页面过渡离开动画时长（需与 App.vue page-slide-leave-duration 一致） */
export const PAGE_LEAVE_DURATION = 200

/** 页面过渡进入动画时长（需与 App.vue page-slide-enter-duration 一致） */
export const PAGE_ENTER_DURATION = 250

/** Stagger 动画默认间隔 */
export const STAGGER_DELAY = 80

/** Stagger 动画默认时长 */
export const STAGGER_DURATION = 400
