/**
 * 设备标识（P0：挂单「浏览器崩溃后取不回来」的根因）
 *
 * 历史实现：terminalId = `pos-${hostname}-${Date.now()}` —— **每次登录都变**。
 * 而后端 pos_held_orders 按 employee_id + terminal_id 双条件隔离，
 * 于是「浏览器崩溃 → 重新登录 → 新 terminalId → 看不到刚才挂的单」，
 * 正好把 Owner 立项要解决的问题（崩溃/清缓存/跨终端不丢挂单）废掉。
 *
 * 现在：terminalId 来自**设备级持久 ID**，生成一次后永久复用（存在 pos-device-id）。
 * 键名用 pos-device-id 而不是 pos-terminal：
 *   pos-terminal 保留为「本次登录传给后端的终端号」，值等于设备 ID（兼容既有读取方）。
 */

const DEVICE_ID_KEY = 'pos-device-id'

function randomPart(): string {
  try {
    const c = globalThis.crypto as Crypto | undefined
    if (c?.randomUUID) return c.randomUUID().replace(/-/g, '').slice(0, 16)
  } catch {
    /* ignore */
  }
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-6)
}

function buildDeviceId(): string {
  const hostname = (typeof window !== 'undefined' && window.location?.hostname) || 'localhost'
  return 'pos-' + hostname + '-' + randomPart()
}

/**
 * 取设备 ID：没有就生成一次并持久化；之后永远返回同一个值。
 * 迁移：若只有老的 pos-terminal（每次登录变）而没有 pos-device-id，则生成本设备 ID，
 * 并在控制台提示「本机旧挂单需重新登录一次才能取回」。
 */
export function getDeviceId(): string {
  try {
    const existing = (localStorage.getItem(DEVICE_ID_KEY) || '').trim()
    if (existing) return existing

    const id = buildDeviceId()
    localStorage.setItem(DEVICE_ID_KEY, id)

    const legacyTerminal = (localStorage.getItem('pos-terminal') || '').trim()
    if (legacyTerminal && legacyTerminal !== id) {
      // eslint-disable-next-line no-console
      console.info(
        '[DEVICE-ID] 已为本机生成稳定设备标识 ' + id +
          '（旧终端号 ' + legacyTerminal + ' 为每次登录生成，其挂单需重新登录一次后由后端按新终端号隔离）',
      )
    }
    return id
  } catch {
    // localStorage 不可用（隐私模式等）→ 退回临时 ID，至少本次会话内稳定
    return buildDeviceId()
  }
}

/** 仅「明确重置设备」时使用（换机/设备号污染），会丢弃设备级标识 */
export function resetDeviceId(): string {
  try {
    localStorage.removeItem(DEVICE_ID_KEY)
  } catch {
    /* ignore */
  }
  return getDeviceId()
}

export default getDeviceId
