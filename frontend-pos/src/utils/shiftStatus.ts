/**
 * 班次状态的三态区分（前端侧）
 *
 * 背景：后端 `GET /v1/pos/auth/current-shift` 在「没有在班班次」时返回 200 + data=null，
 * 这是**正常业务态**；而接口不存在/网络失败是**故障**。过去两者都被吞进同一个 catch，
 * 表现为「什么都不显示」或「无限等待」，用户以为系统坏了 —— 这就是「无班次状态崩塌」。
 *
 * 约定（前端唯一口径，不改后端契约）：
 *   a) 404 / 路径不存在 → 'not-implemented' → 文案「该功能未上线（V2.0）」，不阻塞其它操作
 *   b) 200 + data=null   → 'no-shift'        → 文案「当前没有在班班次，请先开班」+ 开班入口
 *   c) 网络异常 / 5xx    → 'error'           → 文案「班次状态加载失败，可重试」+ 重试按钮
 */

/**
 * 金额单位（同一个交班面板里两种单位并存，务必分清）：
 *   · 读：GET /v1/pos/shift/stats、/v1/pos/shift/opening-cash 返回的都是**元**
 *        （如 openingCash: 0.00、cashIn: 22.00）
 *   · 写：POST /v1/pos/exceptions/{id}/resolve 传的是**分**
 *        （amountFen / cashReceivedFen，字段名带 Fen）
 * 因此「元 → 分」只在调用写接口前换算，统一用 yuanToFen()，不要各处裸写 *100。
 */
export function yuanToFen(yuan: number | null | undefined): number {
  const n = typeof yuan === 'number' ? yuan : Number(yuan)
  // 非数值（null / undefined / '' / NaN / Infinity）一律 0：宁可 0，也不要把 NaN 发给后端
  if (!Number.isFinite(n)) return 0
  return Math.round(n * 100)
}

/**
 * 分 → 元：仅用于**展示**。
 * 异常单 ExceptionOrder.amount 就是分（2200 = ¥22.00），旧代码直接 toFixed(2) 会显示成 ¥2200.00。
 */
export function fenToYuan(fen: number | null | undefined): number {
  const n = typeof fen === 'number' ? fen : Number(fen)
  if (!Number.isFinite(n)) return 0
  return Math.round(n) / 100
}

export type ShiftStateKind = 'no-shift' | 'bad-request' | 'not-implemented' | 'forbidden' | 'error'

export const SHIFT_STATE_TEXT: Record<ShiftStateKind, { title: string; hint: string }> = {
  'no-shift': {
    title: '当前没有在班班次，请先开班',
    hint: '开班后才能交班、查备用金与班次统计。',
  },
  'bad-request': {
    title: '班次查询参数有误（缺少 terminalId）',
    hint: '请检查终端号后重试；这是请求参数问题，不是「没有班次」。',
  },
  'not-implemented': {
    title: '该功能未上线（V2.0）',
    hint: '该能力后端尚未提供，不影响收银与其它操作。',
  },
  forbidden: {
    title: '当前账号无权限查询班次数据',
    hint: '请联系店长授权（班次/收银数据权限），授权后点重试即可，无需重新登录。',
  },
  error: {
    title: '班次状态加载失败，可重试',
    hint: '网络或服务异常，稍后重试；若持续失败请联系管理员。',
  },
}

/** 从各种错误形态里抽取 HTTP 状态码（POS request 抛出的可能是对象、AxiosError 或 Error） */
function extractStatus(error: unknown): number | null {
  if (!error || typeof error !== 'object') return null
  const e = error as {
    status?: unknown
    code?: unknown
    response?: { status?: unknown }
  }
  const candidates = [e.status, e.code, e.response?.status]
  for (const c of candidates) {
    const n = Number(c)
    if (Number.isFinite(n) && n >= 100 && n < 600) return n
  }
  return null
}

/** 故障分类：只区分「接口不存在」与「其它故障」 */
export function classifyShiftFailure(error: unknown): ShiftStateKind {
  const status = extractStatus(error)
  // 注意：POS 拦截器把 body.code 放进 `status` 抛出（HTTP 200 + body.code=400/404 的情况都走这里）
  if (status === 404) return 'not-implemented'
  if (status === 400 || status === 422) return 'bad-request'
  // 403 是"要授权"，不是"网络坏了"——文案必须让人知道该找店长，而不是反复重试
  if (status === 403) return 'forbidden'
  return 'error'
}

/** 便于 UI 判定的错误文案 */
export function shiftFailureMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as { message: unknown }).message) || '未知错误'
  }
  return '未知错误'
}
