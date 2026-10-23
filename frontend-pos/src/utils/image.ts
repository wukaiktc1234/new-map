/**
 * POS 端图片地址统一入口（单一实现，禁止各处自行拼接）
 *
 * 事实依据（已实测）：
 *   1. 后端 `server.servlet.context-path=/api`，WebMvcConfig 注册 `/uploads/**`
 *      资源处理器 → 真实可访问地址是 `http://<origin>/api/uploads/xxx.png`。
 *   2. 数据库 / 接口返回的 imageUrl 是相对路径，实测样例：
 *      food.food_image_url = "/uploads/20260801013947_e9f319b1.png"（菜品「生菜串」）。
 *   3. POS 端 `frontend-pos` 没有任何 .env 文件 → `import.meta.env.VITE_API_BASE_URL`
 *      为 undefined，旧实现 `${VITE_API_BASE_URL || ''}${url}` 直接产出 "/uploads/xxx.png"，
 *      被 vite dev server 当成 SPA 路由回退成 index.html（Content-Type: text/html），
 *      浏览器就显示为破图。管理端父目录有 VITE_API_BASE_URL=/api，且另有 `/api` 前缀补全，
 *      因此管理端图片正常。
 *   4. vite 代理只转发 `/api` 前缀（vite.config.ts server.proxy），所以 POS 端必须用
 *      `/api/uploads/**` 同源相对路径，或「后端 origin + /api/uploads/**」绝对地址。
 *
 * 兼容策略：
 *   - 空值 / 空白 → ''（调用方据此回退占位图标）
 *   - data: / blob: → 原样返回（摄像头截图等本地资源）
 *   - http(s):// 绝对地址 → 原样返回，只做两项修正：
 *       a) 本机回环 host 与页面 host 不一致时统一成页面 host（127.0.0.1 ↔ localhost 混用）
 *       b) 指向后端但漏了 context-path 的 `/uploads/...` 补成 `/api/uploads/...`
 *   - 相对路径 `/uploads/...` → 补 context-path 成 `/api/uploads/...`，
 *     若 VITE_API_BASE_URL 配成绝对地址（如 http://localhost:8081/api）则拼上其 origin
 */

/** 本机回环地址（大小写不敏感比较） */
const LOOPBACK_HOSTS = new Set(['127.0.0.1', 'localhost', '::1', '[::1]', '0.0.0.0'])

/** 后端 context-path，见 backend/src/main/resources/application*.yml */
const API_CONTEXT_PATH = '/api'

/** 默认 API 基地址（与 src/api/request.ts 的 baseURL 保持一致） */
const DEFAULT_API_BASE = '/api'

/** 读取 API 基地址：frontend-pos 目前无 .env，默认回落到 '/api' */
function readApiBase(): string {
  const raw = (import.meta.env.VITE_API_BASE_URL || '').trim()
  return raw || DEFAULT_API_BASE
}

/** 是否带协议头的完整 URL */
function isAbsoluteUrl(value: string): boolean {
  return /^[a-z][a-z0-9+.-]*:\/\//i.test(value)
}

/** 当前页面 hostname（非浏览器环境返回空串） */
function pageHostname(): string {
  if (typeof window === 'undefined' || !window.location) return ''
  return window.location.hostname || ''
}

/** 是否本机回环 host */
function isLoopback(hostname: string): boolean {
  return LOOPBACK_HOSTS.has(hostname.toLowerCase())
}

/** 该 URL 是否指向本系统后端（回环地址，或与 API 基地址同源） */
function isBackendOrigin(parsed: URL): boolean {
  if (isLoopback(parsed.hostname)) return true
  const base = readApiBase()
  if (isAbsoluteUrl(base)) {
    try {
      return new URL(base).origin === parsed.origin
    } catch {
      return false
    }
  }
  return false
}

/** 绝对 URL 修正：回环 host 统一 + 后端静态资源补齐 context-path */
function normalizeAbsoluteUrl(url: string): string {
  try {
    const parsed = new URL(url)

    const pageHost = pageHostname()
    if (pageHost && isLoopback(parsed.hostname) && isLoopback(pageHost) && pageHost !== parsed.hostname) {
      // localhost ↔ 127.0.0.1 混用：统一改成页面自己的 host，避免 cookie/CORS/端口判断不一致
      parsed.hostname = pageHost
    }

    // 后端静态资源漏了 context-path（如 http://localhost:8081/uploads/x.png）→ 补 /api
    if (
      isBackendOrigin(parsed) &&
      (parsed.pathname === '/uploads' || parsed.pathname.startsWith('/uploads/'))
    ) {
      parsed.pathname = `${API_CONTEXT_PATH}${parsed.pathname}`
    }

    return parsed.toString()
  } catch {
    return url
  }
}

/**
 * 把后端返回的图片地址解析成浏览器可直接加载的 URL。
 *
 * @param url 后端 imageUrl（/uploads/xxx.png）、完整 URL、data:/blob: 或空值
 * @returns 可用的图片 URL；无可用值时返回 ''
 */
export function getFullImageUrl(url?: string | null): string {
  if (url === null || url === undefined) return ''
  const raw = String(url).trim()
  if (!raw) return ''

  // 本地内联资源：原样返回
  if (/^(data|blob):/i.test(raw)) return raw

  // 协议相对地址 //host/path
  if (raw.startsWith('//')) {
    const protocol = typeof window !== 'undefined' && window.location ? window.location.protocol : 'http:'
    return normalizeAbsoluteUrl(`${protocol}${raw}`)
  }

  // 完整 URL：原样返回（仅做必要修正）
  if (isAbsoluteUrl(raw)) return normalizeAbsoluteUrl(raw)

  // 相对路径：统一成 /xxx
  let path = raw.startsWith('/') ? raw : `/${raw}`

  // /uploads/** → /api/uploads/**（后端 context-path）
  if (path === '/uploads' || path.startsWith('/uploads/')) {
    path = `${API_CONTEXT_PATH}${path}`
  }

  // VITE_API_BASE_URL 配成绝对地址时（跨域直连后端），用其 origin 拼完整地址
  const base = readApiBase()
  if (isAbsoluteUrl(base)) {
    try {
      const baseUrl = new URL(base)
      return normalizeAbsoluteUrl(`${baseUrl.origin}${path}`)
    } catch {
      // 解析失败则继续走同源相对路径
    }
  }

  // 默认：同源相对路径，由 vite 的 '/api' 代理转发到后端 8081
  return path
}

export default getFullImageUrl
