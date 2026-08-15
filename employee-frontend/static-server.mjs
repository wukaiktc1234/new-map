/**
 * 员工门户 - 本地静态文件服务器（支持 SPA 路由回退 + /api 反向代理）
 *
 * 解决 Python http.server 的两个问题：
 * 1. 不识别 .js/.css/.woff2 等文件的 MIME 类型
 * 2. 不支持 SPA 路由回退（/login 等 path 返回 404）
 *
 * /api 反向代理（2026-08-15 拓扑修正，参照 apk-server.cjs :3003 模式）：
 * - /api/* → API_TARGET（默认 http://localhost:8081，后端 context-path=/api）
 * - 登录/业务链路真实打通，不再 SPA 回退
 *
 * 使用方法: node static-server.mjs [port] [dir]
 * 默认端口: 8080, 默认目录: ./dist
 */

import { readFileSync, existsSync, statSync } from 'fs'
import { join, extname, dirname } from 'path'
import { createServer, request as httpRequest } from 'http'
import { networkInterfaces } from 'os'

const PORT = parseInt(process.argv[2]) || 8080
const DIR = process.argv[2] && !isNaN(parseInt(process.argv[2]))
  ? './dist'
  : (process.argv[2] || './dist')

// /api 反向代理目标（后端 :8081；后端 Spring context-path=/api，透传原始 URL 即可）
const API_TARGET = process.env.API_TARGET || 'http://localhost:8081'

// MIME 类型映射（Python http.server 缺失的关键类型）
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject',
  '.mp3': 'audio/mpeg',
  '.mp4': 'video/mp4',
  '.webm': 'video/webm',
  '.pdf': 'application/pdf',
  '.apk': 'application/vnd.android.package-archive',
  '.xml': 'application/xml',
  '.txt': 'text/plain; charset=utf-8',
  '.manifest': 'application/manifest+json',
}

function getMimeType(filepath) {
  const ext = extname(filepath).toLowerCase()
  return MIME_TYPES[ext] || 'application/octet-stream'
}

// /api 反向代理：/api/* 转发到后端（参照 apk-server.cjs proxyApi 模式，保持链路一致）
function proxyApi(req, res) {
  const apiReq = httpRequest(
    API_TARGET + req.url,
    { method: req.method, headers: req.headers },
    (apiRes) => {
      res.writeHead(apiRes.statusCode || 502, {
        'Content-Type': apiRes.headers['content-type'] || 'application/json',
      })
      apiRes.pipe(res)
    },
  )
  apiReq.on('error', (err) => {
    console.error(`[proxy] /api 代理失败: ${err.code || err.message}`)
    if (!res.headersSent) {
      res.writeHead(502, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({ code: -1, message: 'Backend unavailable', data: null }))
    }
  })
  req.pipe(apiReq)
}

// SPA 路由回退：对于非文件路径，返回 index.html
const server = createServer((req, res) => {
  // 解析 URL 路径（去掉 query string）
  let urlPath = req.url.split('?')[0]
  // 安全处理：防止路径穿越
  if (urlPath.includes('..')) {
    res.writeHead(400)
    res.end('Bad Request')
    return
  }

  // API 反向代理（优先于静态/SPA 回退：登录/业务链路真实打到后端，不落 SPA 回退）
  if (urlPath.startsWith('/api/')) {
    proxyApi(req, res)
    return
  }

  let filePath = join(DIR, urlPath === '/' ? 'index.html' : urlPath)

  // 检查文件是否存在
  if (!existsSync(filePath) || !statSync(filePath).isFile()) {
    // SPA 回退：返回 index.html（让 Vue Router 处理路由）
    filePath = join(DIR, 'index.html')
    if (!existsSync(filePath)) {
      res.writeHead(404)
      res.end('Not Found')
      return
    }
  }

  try {
    const content = readFileSync(filePath)
    const mime = getMimeType(filePath)

    // CORS 和缓存头
    res.writeHead(200, {
      'Content-Type': mime,
      'Cache-Control': urlPath.includes('/assets/')
        ? 'public, max-age=31536000'   // 带哈希的资源长期缓存
        : 'no-cache',                  // HTML 等不缓存
      'Access-Control-Allow-Origin': '*',
      'X-Content-Type-Options': 'nosniff',
    })
    res.end(content)

    // 日志输出
    const method = req.method.padEnd(6)
    const size = (content.length / 1024).toFixed(1).padStart(7) + 'KB'
    console.log(`${method} ${urlPath} → 200 ${size}`)
  } catch (err) {
    console.error(`Error serving ${urlPath}:`, err.message)
    res.writeHead(500)
    res.end('Internal Server Error')
  }
})

server.listen(PORT, '0.0.0.0', () => {
  console.log('')
  console.log('╔══════════════════════════════════════╗')
  console.log('║  员工门户 - 静态文件服务器             ║')
  console.log('╠══════════════════════════════════════╣')
  console.log(`║  地址:  http://localhost:${String(PORT).padStart(5)}          ║`)
  console.log(`║  目录:  ${DIR.padEnd(28)}║`)
  console.log('║                                      ║')
  console.log('║  功能:                                ║')
  console.log('║  ✓ 正确 MIME 类型 (.js/.css/.woff2)   ║')
  console.log('║  ✓ SPA 路由回退 (/login → index.html) ║')
  console.log(`║  ✓ /api 反向代理 → ${API_TARGET.padEnd(24)}║`)
  console.log('║  ✓ CORS 支持                         ║')
  console.log('╚══════════════════════════════════════╝')
  console.log('')

  // 显示局域网地址
  const interfaces = networkInterfaces()
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        console.log(`  局域网地址: http://${iface.address}:${PORT}/`)
      }
    }
  }
  console.log('')
})
