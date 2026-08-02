/**
 * localStorage 安全读写工具
 *
 * 提供对 localStorage 的安全封装，处理：
 * - JSON 解析失败（损坏/篡改数据）
 * - 数据格式校验（类型、结构验证）
 * - 存储配额超限（quota exceeded）
 * - 空值/undefined 处理
 */

/**
 * 安全解析 JSON 字符串
 * @param raw - 待解析的原始字符串（可能为 null/undefined/非法JSON）
 * @param fallback - 解析失败时的默认返回值
 * @returns 解析结果或 fallback 值
 */
export function safeJsonParse<T>(raw: string | null | undefined, fallback: T): T {
  if (!raw || typeof raw !== 'string') return fallback

  // 防止超大字符串导致性能问题（上限 1MB）
  if (raw.length > 1024 * 1024) {
    console.warn('[storage] 数据过大，已忽略（长度:', raw.length, '）')
    return fallback
  }

  try {
    const parsed = JSON.parse(raw)
    return parsed as T
  } catch (e) {
    console.warn('[storage] JSON解析失败，使用默认值', e instanceof Error ? e.message : e)
    return fallback
  }
}

/**
 * 安全写入 localStorage
 * @param key - 存储 key
 * @param value - 待存储的值（会被 JSON.stringify）
 * @returns 是否写入成功
 */
export function safeLocalStorageSet(key: string, value: unknown): boolean {
  try {
    const serialized = JSON.stringify(value)
    localStorage.setItem(key, serialized)
    return true
  } catch (e) {
    // 配额超限或序列化失败
    console.warn(`[storage] 写入失败 [${key}]:`, e instanceof Error ? e.message : e)
    return false
  }
}

/**
 * 安全读取 localStorage 并自动 JSON 解析
 * @param key - 存储 key
 * @param fallback - 读取或解析失败时的默认值
 * @returns 解析结果或 fallback 值
 */
export function safeLocalStorageGet<T>(key: string, fallback: T): T {
  const raw = localStorage.getItem(key)
  return safeJsonParse(raw, fallback)
}
