/**
 * 订单 / 后厨状态码 → 中文与语义色（POS 端统一口径）
 *
 * 背景：后端 order_status 的规范码是数字（0待确认 1已确认 2已完成 3已取消 5全额退款），
 * 而客服端/历史版本又可能返回文本（paid / pending / refunded…）。
 * 之前两个对话框各写一份只认文本的映射，遇到数字码就把 "2" 原样显示给收银员——
 * 这里收敛成唯一实现，避免再次漂移。
 */

export type StatusTone = 'success' | 'warning' | 'danger' | 'primary' | 'info'

export interface StatusMeta {
  text: string
  tone: StatusTone
}

const ORDER_STATUS: Record<string, StatusMeta> = {
  // 数字规范码
  '0': { text: '待确认', tone: 'warning' },
  '1': { text: '已确认', tone: 'primary' },
  '2': { text: '已完成', tone: 'success' },
  '3': { text: '已取消', tone: 'danger' },
  '4': { text: '已完成', tone: 'success' },
  '5': { text: '已退款', tone: 'danger' },
  '6': { text: '退款中', tone: 'warning' },
  '7': { text: '已退款', tone: 'danger' },
  // 文本同义词
  pending: { text: '待确认', tone: 'warning' },
  unpaid: { text: '待支付', tone: 'warning' },
  paid: { text: '已支付', tone: 'success' },
  confirmed: { text: '已确认', tone: 'primary' },
  making: { text: '制作中', tone: 'primary' },
  completed: { text: '已完成', tone: 'success' },
  served: { text: '已出餐', tone: 'success' },
  cancelled: { text: '已取消', tone: 'danger' },
  canceled: { text: '已取消', tone: 'danger' },
  refunding: { text: '退款中', tone: 'warning' },
  refunded: { text: '已退款', tone: 'danger' },
  partial_refunded: { text: '部分退款', tone: 'warning' }
}

/**
 * 后厨（餐品制作）状态。
 *
 * ⚠️ 数字口径以后端 OrderItemNew.kitchen_state 为准：
 *    0 待制作 / 1 制作中 / 2 已完成 / 3 已上菜（= 已出餐）/ 4 已退款
 * 旧实现把 3 写成"已取消"、也没有 4，于是"已上菜"的单子被显示成取消/待处理，
 * 退款单又被当成"待制作"——审计报的正是这两条。
 */
const KITCHEN_STATUS: Record<string, StatusMeta> = {
  '0': { text: '待制作', tone: 'warning' },
  '1': { text: '制作中', tone: 'primary' },
  '2': { text: '已完成', tone: 'success' },
  '3': { text: '已上菜', tone: 'success' },
  '4': { text: '已退款', tone: 'danger' },
  '5': { text: '已取消', tone: 'danger' },
  pending: { text: '待制作', tone: 'warning' },
  received: { text: '已接单', tone: 'info' },
  making: { text: '制作中', tone: 'primary' },
  completed: { text: '已完成', tone: 'success' },
  served: { text: '已上菜', tone: 'success' },
  delivered: { text: '已上菜', tone: 'success' },
  refunding: { text: '退款中', tone: 'warning' },
  refunded: { text: '已退款', tone: 'danger' },
  partial_refunded: { text: '部分退款', tone: 'warning' },
  cancelled: { text: '已取消', tone: 'danger' },
  canceled: { text: '已取消', tone: 'danger' }
}

const normalize = (code: string | number | null | undefined): string =>
  String(code ?? '').trim().toLowerCase()

/** 订单状态（未知码回退为「未知」而不是把数字直接甩给收银员） */
export function getOrderStatus(code: string | number | null | undefined): StatusMeta {
  const key = normalize(code)
  if (!key) return { text: '未知', tone: 'info' }
  return ORDER_STATUS[key] ?? { text: `未知状态（${code}）`, tone: 'info' }
}

/** 后厨状态（仅按码值翻译；订单级推断见 resolveKitchenStatus） */
export function getKitchenStatus(code: string | number | null | undefined): StatusMeta {
  const key = normalize(code)
  if (!key) return { text: '未进入后厨', tone: 'info' }
  return KITCHEN_STATUS[key] ?? { text: `未知状态（${code}）`, tone: 'info' }
}

/**
 * 订单详情里的「后厨状态」：以订单为上下文做一次兜底，避免"元数据缺失就乱说"。
 *
 * 规则（按优先级）：
 *   ① 订单本身已退款 / 已取消 → 后厨不可能还在"待制作"，直接跟随订单状态
 *   ② 后端给了 kitchenStatusName（中文名）→ 直接用（唯一真相在后端）
 *   ③ 有 kitchenStatus 码 → 按表翻译
 *   ④ 都没有 → 按订单状态推断（已完成→已上菜，其余→待制作）
 */
export function resolveKitchenStatus(order: {
  status?: string | number | null
  kitchenStatus?: string | number | null
  kitchenStatusName?: string | null
} | null | undefined): StatusMeta {
  const orderMeta = getOrderStatus(order?.status)

  if (orderMeta.text === '已退款' || orderMeta.text === '部分退款') {
    return { text: orderMeta.text, tone: orderMeta.tone }
  }
  if (orderMeta.text === '退款中') {
    return { text: '退款中', tone: 'warning' }
  }
  if (orderMeta.text === '已取消') {
    return { text: '已取消', tone: 'danger' }
  }

  const name = (order?.kitchenStatusName || '').trim()
  if (name) return { text: name, tone: 'success' }

  const code = order?.kitchenStatus
  const raw = getKitchenStatus(code)
  if (!raw.text.startsWith('未知状态') && raw.text !== '未进入后厨') return raw

  if (orderMeta.text === '已完成') return { text: '已上菜', tone: 'success' }
  return { text: '待制作', tone: 'warning' }
}
