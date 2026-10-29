/**
 * 终端标识展示工具（收银终端脱敏）
 *
 * 背景：Login.vue 的 generateTerminalId() 生成的 terminalId 形如
 *   pos-localhost-1793174348494
 * 直接把原始串渲染到开班弹窗，收银员看到的是主机名 + 毫秒时间戳：
 *   ① 对一线毫无意义，还会让人误以为系统出错；
 *   ② 暴露内网主机名 / 终端指纹，属于不该出现在业务界面上的技术细节。
 *
 * 因此统一映射成「前台收银机A / B / C…」这样的稳定别名：
 *   - 别名写入 localStorage（pos-terminal-alias），同一台机器刷新、跨页面、跨班次都一致；
 *   - 别名只增不改，避免同一终端在两次开班时显示成不同名字；
 *   - 需要排障时只暴露尾部 4 位（maskTerminalId），不泄露主机名。
 */

const ALIAS_STORAGE_KEY = 'pos-terminal-alias'
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
const FALLBACK_NAME = '前台收银机'

type AliasMap = Record<string, string>

function readAliasMap(): AliasMap {
  try {
    const raw = localStorage.getItem(ALIAS_STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as unknown
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
    const map: AliasMap = {}
    for (const [key, value] of Object.entries(parsed as Record<string, unknown>)) {
      if (typeof value === 'string' && value.trim()) map[key] = value
    }
    return map
  } catch {
    // localStorage 不可用（隐私模式 / 配额满）时退化为不缓存，不影响收银
    return {}
  }
}

function writeAliasMap(map: AliasMap): void {
  try {
    localStorage.setItem(ALIAS_STORAGE_KEY, JSON.stringify(map))
  } catch {
    // 写失败不阻断业务
  }
}

/**
 * 终端展示名：「前台收银机A」
 * 同一个 terminalId 多次调用返回同一个名字；新终端按 A→B→C 顺延。
 */
export function terminalDisplayName(terminalId: string): string {
  const id = (terminalId || '').trim()
  if (!id) return FALLBACK_NAME

  const map = readAliasMap()
  const cached = map[id]
  if (cached) return cached

  const used = new Set(Object.values(map))
  let name = ''
  for (let i = 0; i < LETTERS.length; i += 1) {
    const candidate = `${FALLBACK_NAME}${LETTERS[i]}`
    if (!used.has(candidate)) {
      name = candidate
      break
    }
  }
  // 超过 26 台终端（连锁大店）时用序号兜底，仍然保持唯一
  if (!name) name = `${FALLBACK_NAME}${Object.keys(map).length + 1}`

  map[id] = name
  writeAliasMap(map)
  return name
}

/**
 * 终端脱敏标识：terminalId → 「****8494」
 * 只保留尾部 4 位，用于现场排障口头核对，不暴露主机名与完整时间戳。
 */
export function maskTerminalId(terminalId: string): string {
  const id = (terminalId || '').trim()
  if (!id) return '未绑定终端'
  const tail = id.slice(-4)
  return `****${/^\d+$/.test(tail) ? tail : id.slice(-4).toUpperCase()}`
}

/** 悬浮提示文案：展示名 + 脱敏尾号，不含原始串 */
export function terminalTooltip(terminalId: string): string {
  const id = (terminalId || '').trim()
  if (!id) return '本机尚未绑定收银终端，请重新登录'
  return `终端：${terminalDisplayName(id)}（终端标识 ${maskTerminalId(id)}）`
}
