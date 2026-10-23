/**
 * localStorage 安全读写工具
 *
 * 提供对 localStorage 的安全封装，处理：
 * - JSON 解析失败（损坏/篡改数据）
 * - 数据格式校验（类型、结构验证）
 * - 存储配额超限（quota exceeded）
 * - 空值/undefined 处理
 */

import logger from '@/utils/logger'
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
    logger.warnMessage('[storage] 数据过大，已忽略（长度:', raw.length, '）')
    return fallback
  }

  try {
    const parsed = JSON.parse(raw)
    return parsed as T
  } catch (e) {
    logger.warnMessage('[storage] JSON解析失败，使用默认值', e instanceof Error ? e.message : e)
    return fallback
  }
}

