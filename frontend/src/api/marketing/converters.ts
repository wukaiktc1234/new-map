/**
 * 营销管理模块数据转换器
 * 负责后端数据与前端展示数据之间的双向转换
 * 遵循规范：禁止在组件中直接做状态映射或金额元分转换
 */

import type { MemberStatus, RegisterChannel, CustomerSegment, RechargePaymentStatus } from '@/types/member'
import { MemberStatusText, RegisterChannelText, CustomerSegmentText, PaymentMethodText } from '@/types/member'
import { LevelCodeSuggestions } from '@/types/member-level'
import { fenToYuan as utilsFenToYuan, yuanToFen } from '@/utils/money'

// ============================================================
// 金额转换（分 ↔ 元，统一委托给 utils/money）
// ============================================================

/**
 * 分转元（接受 number 或 string，与原签名兼容）
 * 内部委托给 utils/money.fenToYuan，仅做参数归一化
 */
function fenToYuan(fen: number | string | null | undefined): string {
  if (fen == null) return '0.00'
  const num = typeof fen === 'string' ? parseInt(fen, 10) : fen
  if (Number.isNaN(num)) return '0.00'
  return utilsFenToYuan(num)
}

// yuanToFen 直接使用 utils/money 导出的实现，签名兼容（接受 string/number/null/undefined）

// ============================================================
// 会员状态转换
// ============================================================

/** 会员状态 → StatusTag status */
function toStatusTagStatus(status: MemberStatus): string {
  const map: Record<string, string> = {
    active: 'active',
    frozen: 'frozen',
    blacklisted: 'error',
    cancelled: 'inactive',
  }
  return map[status] || 'default'
}

/** 会员状态 → 显示文本 */
function toStatusLabel(status: MemberStatus): string {
  return MemberStatusText[status] || '未知'
}

/** 后端状态码 → 前端状态 */
function toFrontendStatus(code: number): MemberStatus {
  const map: Record<number, MemberStatus> = { 1: 'active', 2: 'frozen', 3: 'blacklisted', 4: 'cancelled' }
  return map[code] || 'active'
}

/** 前端状态 → 后端状态码 */
function toBackendStatus(status: MemberStatus): number {
  const map: Record<string, number> = { active: 1, frozen: 2, blacklisted: 3, cancelled: 4 }
  return map[status] || 1
}

// ============================================================
// 性别转换
// ============================================================

/** 后端性别码 → 前端性别 */
function toFrontendGender(code: number): string {
  const map: Record<number, string> = { 0: 'unknown', 1: 'male', 2: 'female' }
  return map[code] || 'unknown'
}

/** 前端性别 → 后端性别码 */
function toBackendGender(gender: string): number {
  const map: Record<string, number> = { unknown: 0, male: 1, female: 2 }
  return map[gender] || 0
}

// ============================================================
// 注册渠道转换
// ============================================================

/** 后端注册渠道码 → 前端渠道 */
function toFrontendChannel(code: number): RegisterChannel {
  const map: Record<number, RegisterChannel> = { 1: 'app', 2: 'pos', 3: 'qr', 4: 'import', 5: 'mini_program' }
  return map[code] || 'manual'
}

/** 前端渠道 → 后端渠道码 */
function toBackendChannel(channel: RegisterChannel): number {
  const map: Record<string, number> = { app: 1, pos: 2, qr: 3, import: 4, mini_program: 5, manual: 6 }
  return map[channel] || 6
}

/** 注册渠道 → 显示文本 */
function toChannelLabel(channel: RegisterChannel): string {
  return RegisterChannelText[channel] || '其他'
}

// ============================================================
// 客户分层转换
// ============================================================

/** 客户分层 → StatusTag status */
function toSegmentStatus(segment: CustomerSegment): string {
  const map: Record<string, string> = {
    champion: 'primary', loyal: 'success', potential: 'warning',
    new: 'info', at_risk: 'error', lost: 'inactive',
  }
  return map[segment] || 'default'
}

/** 客户分层 → 显示文本 */
function toSegmentLabel(segment: CustomerSegment): string {
  return CustomerSegmentText[segment] || '未知'
}

// ============================================================
// 等级转换
// ============================================================

/** 等级编码 → StatusTag status */
function toLevelStatus(code: string): string {
  const map: Record<string, string> = { NORMAL: 'info', SILVER: 'default', GOLD: 'warning', DIAMOND: 'primary' }
  return map[code] || 'default'
}

/** 等级编码 → 显示文本 */
function toLevelLabel(code: string): string {
  const found = LevelCodeSuggestions.find(s => s.label === code)
  return found ? found.description : code
}

// ============================================================
// 支付方式转换
// ============================================================

/** 支付方式 → 显示文本 */
function toPaymentMethodLabel(method: string): string {
  return PaymentMethodText[method] || '其他'
}

// ============================================================
// 储值支付状态转换
// ============================================================

/** 储值支付状态 → StatusTag status */
function toRechargePaymentStatus(status: RechargePaymentStatus): string {
  const map: Record<string, string> = { pending: 'pending', paid: 'active', refunded: 'warning', failed: 'error' }
  return map[status] || 'default'
}

/** 储值支付状态 → 显示文本 */
function toRechargePaymentLabel(status: RechargePaymentStatus): string {
  const map: Record<string, string> = { pending: '待支付', paid: '已支付', refunded: '已退款', failed: '支付失败' }
  return map[status] || '未知'
}

// ============================================================
// 统一导出
// ============================================================

export const memberConverter = {
  toStatusTagStatus,
  toStatusLabel,
  toFrontendStatus,
  toBackendStatus,
  toFrontendGender,
  toBackendGender,
  toFrontendChannel,
  toBackendChannel,
  toChannelLabel,
  toSegmentStatus,
  toSegmentLabel,
  toLevelStatus,
  toLevelLabel,
  toPaymentMethodLabel,
  toRechargePaymentStatus,
  toRechargePaymentLabel,
  fenToYuan,
  yuanToFen,
}
