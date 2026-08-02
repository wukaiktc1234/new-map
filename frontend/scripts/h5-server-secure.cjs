/**
 * H5表单独立服务器 - 企业级安全版 v2.0
 *
 * 安全特性：
 * ✅ 端口隔离（与管理端完全分离）
 * ✅ API白名单（只允许特定接口）
 * ✅ 请求频率限制（防刷/防DDoS）
 * ✅ 请求体大小限制（防DoS）
 * ✅ HTTP方法限制（禁止危险操作）
 * ✅ 路径遍历防护
 * ✅ 安全响应头
 * ✅ 访问日志审计
 * ✅ IP黑名单支持
 *
 * 启动方式：
 *   node scripts/h5-server-secure.cjs
 */

const http = require('http')
const fs = require('fs')
const path = require('path')
const os = require('os')
const crypto = require('crypto')

// ==================== 配置 ====================
const PORT = process.env.H5_PORT || 3003
const PUBLIC_DIR = path.join(__dirname, '../public')
const API_TARGET = process.env.API_TARGET || 'http://localhost:8081'

// 安全配置
const SECURITY_CONFIG = {
  // 请求频率限制（同一IP每分钟最多请求次数）
  rateLimit: {
    windowMs: 60 * 1000,  // 1分钟窗口
    maxRequests: 30,       // 最多30次请求
    message: '请求过于频繁，请稍后再试'
  },

  // 请求体大小限制（防止大文件上传攻击）
  maxRequestBodySize: 1024 * 1024,  // 1MB

  // 允许的HTTP方法
  allowedMethods: ['GET', 'HEAD', 'OPTIONS', 'POST'],

  // API白名单（只允许这些路径被代理）
  apiWhitelist: [
    '/api/v1/recruitment/applicants',     // 提交应聘信息
    '/api/v1/recruitment/jobs'            // 查询岗位列表（可选）
  ],

  // 危险路径黑名单（即使匹配也不允许）
  dangerousPaths: [
    '/admin',
    '/manage',
    '/login',
    '/auth',
    '/user',
    '/system',
    '/config',
    '.env',
    '.git',
    'wp-admin',
    'phpmyadmin'
  ],

  // IP黑名单（可从文件加载）
  ipBlacklist: new Set(),

  // 是否启用详细日志
  enableAuditLog: true
}

// ==================== 内存存储 ====================
// 请求计数器（用于限流）
const requestCounts = new Map()

// ==================== 工具函数 ====================

/** 获取客户端真实IP */
function getClientIP(req) {
  const forwarded = req.headers['x-forwarded-for']
  if (forwarded) {
    return forwarded.split(',')[0].trim()
  }
  return req.socket.remoteAddress || 'unknown'
}

/** 检查IP是否在黑名单中 */
function isIPBlacklisted(ip) {
  if (SECURITY_CONFIG.ipBlacklist.has(ip)) {
    console.log(`[🚫 黑名单拦截] ${ip}`)
    return true
  }
  return false
}

/** 检查请求频率是否超限 */
function checkRateLimit(ip) {
  const now = Date.now()
  const { windowMs, maxRequests } = SECURITY_CONFIG.rateLimit

  if (!requestCounts.has(ip)) {
    requestCounts.set(ip, { count: 1, startTime: now })
    return { allowed: true }
  }

  const record = requestCounts.get(ip)

  // 检查窗口是否过期
  if (now - record.startTime > windowMs) {
    record.count = 1
    record.startTime = now
    return { allowed: true }
  }

  // 检查是否超限
  if (record.count >= maxRequests) {
    return {
      allowed: false,
      retryAfter: Math.ceil((windowMs - (now - record.startTime)) / 1000)
    }
  }

  record.count++
  return { allowed: true }
}

/** 清理过期的请求记录（定期执行） */
function cleanupRateLimits() {
  const now = Date.now()
  const { windowMs } = SECURITY_CONFIG.rateLimit

  for (const [ip, record] of requestCounts.entries()) {
    if (now - record.startTime > windowMs * 2) {
      requestCounts.delete(ip)
    }
  }
}
setInterval(cleanupRateLimits, 60000)  // 每分钟清理一次

/** 检查HTTP方法是否允许 */
function isMethodAllowed(method) {
  return SECURITY_CONFIG.allowedMethods.includes(method.toUpperCase())
}

/** 检查API路径是否在白名单中 */
function isApiPathAllowed(pathname) {
  // 先检查危险路径
  const lowerPath = pathname.toLowerCase()
  for (const dangerous of SECURITY_CONFIG.dangerousPaths) {
    if (lowerPath.includes(dangerous)) {
      console.log(`[⛔ 危险路径拦截] ${pathname} (包含 ${dangerous})`)
      return false
    }
  }

  // 检查白名单
  if (!pathname.startsWith('/api/')) {
    return true  // 非API请求允许（静态文件）
  }

  // API请求必须精确匹配白名单
  const isWhitelisted = SECURITY_CONFIG.apiWhitelist.some(allowed =>
    pathname === allowed || pathname.startsWith(allowed + '/')
  )

  if (!isWhitelisted) {
    console.log(`[🔒 API白名单拒绝] ${pathname}`)
  }

  return isWhitelisted
}

/** 检查路径是否存在目录遍历攻击 */
function isPathTraversal(pathname) {
  const normalized = path.normalize(pathname)
  return normalized.includes('..') || pathname.includes('%2e%2e') || pathname.includes('..%2f')
}

/** 安全日志记录 */
function auditLog(req, statusCode, message = '') {
  if (!SECURITY_CONFIG.enableAuditLog) return

  const timestamp = new Date().toISOString()
  const ip = getClientIP(req)
  const method = req.method
  const url = req.url.substring(0, 100)  // 截断长URL
  const userAgent = (req.headers['user-agent'] || '').substring(0, 80)

  console.log(
    `[${timestamp}] ${method} ${url} | IP: ${ip} | Status: ${statusCode}` +
    (message ? ` | ${message}` : '') +
    ` | UA: ${userAgent}`
  )
}

// ==================== MIME类型映射 ====================
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
}

// ==================== 响应函数 ====================

/** 返回JSON错误响应 */
function sendJsonError(res, statusCode, errorCode, message) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'X-Content-Type-Options': 'nosniff'
  })
  res.end(JSON.stringify({
    code: errorCode,
    message: message,
    timestamp: Date.now()
  }))
}

/** 返回HTML错误页面 */
function sendHtmlError(res, statusCode, title, message) {
  res.writeHead(statusCode, { 'Content-Type': 'text/html; charset=utf-8' })
  res.end(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>${statusCode}</title>
      <style>
        body { font-family: -apple-system, sans-serif; display: flex; justify-content: center;
               align-items: center; min-height: 100vh; margin: 0; background: #f5f7fa; color: #606266; }
        .container { text-align: center; padding: 40px; background: white; border-radius: 12px;
                   box-shadow: 0 2px 12px rgba(0,0,0,0.08); margin: 20px; max-width: 400px; }
        h1 { color: #909399; font-size: 48px; margin-bottom: 16px; }
        p { font-size: 14px; line-height: 1.6; color: #909399; }
      </style>
    </head>
    <body>
      <div class="container">
        <h1>${statusCode}</h1>
        <p><strong>${title}</strong></p>
        <p>${message}</p>
      </div>
    </body>
    </html>
  `)
}

// ==================== API代理（安全版） ====================
function proxyApiRequest(req, res, pathname) {
  // 再次验证白名单（双重检查）
  if (!isApiPathAllowed(pathname)) {
    auditLog(req, 403, 'API路径不在白名单中')
    sendJsonError(res, 403, -1, '该接口不可用')
    return
  }

  // 构建目标URL
  const targetUrl = `${API_TARGET}${pathname}${req.url.includes('?') ? req.url.substring(req.url.indexOf('?')) : ''}`
  const parsedUrl = new URL(targetUrl)

  console.log(`[API代理] ${req.method} ${pathname} -> ${targetUrl}`)

  const options = {
    hostname: parsedUrl.hostname,
    port: parsedUrl.port || 80,
    path: parsedUrl.pathname + parsedUrl.search,
    method: req.method,
    headers: {
      ...req.headers,
      host: parsedUrl.host,
      'connection': 'keep-alive',
      // 移除可能泄露信息的头
      'x-real-ip': undefined,
      'x-forwarded-for': undefined,
      'x-forwarded-proto': undefined
    },
    timeout: 10000  // 10秒超时
  }

  // 删除undefined的头
  Object.keys(options.headers).forEach(key => {
    if (options.headers[key] === undefined) delete options.headers[key]
  })

  const proxyReq = http.request(options, (proxyRes) => {
    console.log(`[API响应] ${proxyRes.statusCode} ${pathname}`)

    // 过滤敏感响应头
    const safeHeaders = { ...proxyRes.headers }
    delete safeHeaders['x-powered-by']
    delete safeHeaders['server']

    res.writeHead(proxyRes.statusCode, safeHeaders)
    proxyRes.pipe(res)
  })

  proxyReq.on('error', (err) => {
    console.error(`[API代理错误] ${pathname}:`, err.message)
    auditLog(req, 502, `代理错误: ${err.message}`)
    sendJsonError(res, 502, -1, '服务暂时不可用，请稍后重试')
  })

  proxyReq.on('timeout', () => {
    console.error(`[API超时] ${pathname}`)
    proxyReq.destroy()
    auditLog(req, 504, '后端服务响应超时')
    sendJsonError(res, 504, -1, '服务响应超时，请稍后重试')
  })

  // 收集请求体并检查大小
  let bodySize = 0
  req.on('data', (chunk) => {
    bodySize += chunk.length
    if (bodySize > SECURITY_CONFIG.maxRequestBodySize) {
      console.error(`[请求体过大] ${pathname}: ${bodySize} bytes`)
      req.destroy()  // 中断连接
      sendJsonError(res, 413, -1, '请求数据过大')
      return
    }
  })

  req.pipe(proxyReq)
}

// ==================== 主服务器逻辑 ====================
const server = http.createServer((req, res) => {
  // ========== 1. 设置基础安全头 ==========
  res.setHeader('X-Frame-Options', 'DENY')           // 禁止iframe嵌套
  res.setHeader('X-Content-Type-Options', 'nosniff')  // 禁止MIME嗅探
  res.setHeader('X-XSS-Protection', '1; mode=block')  // XSS保护
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin')  // Referrer策略
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")  // CSP
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()')  // 禁用敏感权限

  // ========== 2. 解析URL ==========
  let pathname
  try {
    const urlObj = new URL(req.url, `http://${req.headers.host}`)
    pathname = urlObj.pathname
  } catch (e) {
    auditLog(req, 400, '无效的URL')
    sendHtmlError(res, 400, 'Bad Request', '请求格式错误')
    return
  }

  // ========== 3. IP黑名单检查 ==========
  const clientIP = getClientIP(req)
  if (isIPBlacklisted(clientIP)) {
    auditLog(req, 403, 'IP在黑名单中')
    sendHtmlError(res, 403, 'Forbidden', '您的IP已被限制访问')
    return
  }

  // ========== 4. HTTP方法检查 ==========
  if (!isMethodAllowed(req.method)) {
    auditLog(req, 405, `不允许的方法: ${req.method}`)
    res.setHeader('Allow', SECURITY_CONFIG.allowedMethods.join(', '))
    sendHtmlError(res, 405, 'Method Not Allowed', '该请求方法不被支持')
    return
  }

  // ========== 5. OPTIONS预检请求处理（CORS） ==========
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
      'Access-Control-Max-Age': '86400'
    })
    res.end()
    auditLog(req, 204, 'CORS预检')
    return
  }

  // ========== 6. 请求频率限制 ==========
  const rateLimitResult = checkRateLimit(clientIP)
  if (!rateLimitResult.allowed) {
    auditLog(req, 429, '请求频率超限')
    res.setHeader('Retry-After', rateLimitResult.retryAfter)
    res.setHeader('X-RateLimit-Limit', SECURITY_CONFIG.rateLimit.maxRequests.toString())
    sendJsonError(res, 429, -2, SECURITY_CONFIG.rateLimit.message)
    return
  }

  // ========== 7. 路径遍历攻击检测 ==========
  if (isPathTraversal(pathname)) {
    console.error(`[⚠️ 路径遍历攻击尝试] IP: ${clientIP} Path: ${pathname}`)
    auditLog(req, 400, '路径遍历攻击尝试')
    sendHtmlError(res, 400, 'Bad Request', '非法的请求路径')
    return
  }

  // ========== 8. API路由处理 ==========
  if (pathname.startsWith('/api/')) {
    // API白名单检查
    if (!isApiPathAllowed(pathname)) {
      auditLog(req, 403, 'API路径未授权')
      sendJsonError(res, 403, -1, '该接口不可用')
      return
    }

    // 转发到API代理
    return proxyApiRequest(req, res, pathname)
  }

  // ========== 9. 静态文件服务 ==========
  let filePath = path.join(PUBLIC_DIR, pathname === '/' ? '/recruit/index.html' : pathname)

  // 安全检查：确保路径在PUBLIC_DIR内
  const resolvedPath = path.resolve(filePath)
  if (!resolvedPath.startsWith(path.resolve(PUBLIC_DIR))) {
    console.error(`[⚠️ 目录遍历尝试] IP: ${clientIP} Path: ${filePath}`)
    auditLog(req, 403, '目录遍历攻击尝试')
    sendHtmlError(res, 403, 'Forbidden', '无权访问此资源')
    return
  }

  // 获取文件扩展名
  const ext = path.extname(filePath).toLowerCase()
  const contentType = MIME_TYPES[ext] || 'application/octet-stream'

  // 读取文件
  fs.readFile(filePath, (err, data) => {
    if (err) {
      if (err.code === 'ENOENT') {
        // 文件不存在
        auditLog(req, 404, '文件不存在')
        sendHtmlError(res, 404, '页面不存在', `
          如果这是招聘二维码，请联系店员重新扫码<br><br>
          如需帮助，请联系店内工作人员
        `)
      } else {
        // 其他错误
        console.error('[服务器错误]:', err)
        auditLog(req, 500, `服务器错误: ${err.code}`)
        sendHtmlError(res, 500, 'Internal Server Error', '服务器内部错误')
      }
      return
    }

    // 成功返回文件
    res.writeHead(200, {
      'Content-Type': contentType,
      'Cache-Control': 'public, max-age=3600',  // 缓存1小时
      'ETag': `"${crypto.createHash('md5').update(data).digest('hex')}"`
    })
    res.end(data)

    auditLog(req, 200, `静态文件: ${pathname}`)
  })
})

// ==================== 启动服务器 ====================
server.listen(PORT, '0.0.0.0', () => {
  const interfaces = os.networkInterfaces()
  let localIP = 'localhost'

  Object.values(interfaces).forEach(iface => {
    iface?.forEach(addr => {
      if (addr.family === 'IPv4' && !addr.internal) {
        localIP = addr.address
      }
    })
  })

  console.log('\n' + '='.repeat(60))
  console.log('  📱 H5表单服务器 v2.0 - 企业级安全版')
  console.log('='.repeat(60))
  console.log('')
  console.log(`  本地访问：  http://localhost:${PORT}/recruit/apply.html`)
  console.log(`  内网访问：  http://${localIP}:${PORT}/recruit/apply.html`)
  console.log(`  二维码地址：http://${localIP}:${PORT}/recruit/apply.html?job=岗位ID`)
  console.log('')
  console.log('  🔒 安全特性已启用：')
  console.log('     ✓ 端口隔离（与管理端完全分离）')
  console.log('     ✓ API白名单（仅允许应聘相关接口）')
  console.log('     ✓ 请求频率限制（防刷/DDoS）')
  console.log('     ✓ 请求体大小限制（防DoS）')
  console.log('     ✓ HTTP方法限制（禁止PUT/DELETE等）')
  console.log('     ✓ 路径遍历防护')
  console.log('     ✓ 安全响应头（CSP/XSS/X-Frame等）')
  console.log('     ✓ 访问日志审计')
  console.log('     ✓ IP黑名单支持')
  console.log('')
  console.log('  📋 配置信息：')
  console.log(`     • 速率限制：${SECURITY_CONFIG.rateLimit.maxRequests}次/${SECURITY_CONFIG.rateLimit.windowMs / 1000}秒/IP`)
  console.log(`     • 请求体上限：${SECURITY_CONFIG.maxRequestBodySize / 1024}KB`)
  console.log(`     • 允许的方法：${SECURITY_CONFIG.allowedMethods.join(', ')}`)
  console.log(`     • API白名单：${SECURITY_CONFIG.apiWhitelist.length}个接口`)
  console.log('')
  console.log('  按 Ctrl+C 停止服务器')
  console.log('='.repeat(60) + '\n')
})

// 错误处理
server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`\n❌ 端口 ${PORT} 已被占用！`)
    console.error(`   请检查是否有其他程序在使用该端口\n`)
    process.exit(1)
  } else {
    throw err
  }
})

// 优雅退出
process.on('SIGINT', () => {
  console.log('\n\n正在关闭服务器...')
  server.close(() => {
    console.log('✅ 服务器已关闭')
    process.exit(0)
  })
})
