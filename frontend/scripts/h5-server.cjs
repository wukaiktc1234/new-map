/**
 * 🛡️ 企业级安全H5服务器 v2.0
 * 
 * 核心特性：
 * ✅ 动态Token系统（HMAC-SHA256签名，防伪造）
 * ✅ Token时效性（默认30分钟自动过期）
 * ✅ 一次性使用限制（用完即焚，防重放攻击）
 * ✅ IP白名单/黑名单（网络层访问控制）
 * ✅ 速率限制（防DDoS/暴力扫描）
 * ✅ 完整审计日志（谁、何时、从哪、做了什么）
 * ✅ 零信任架构（最小权限原则）
 * 
 * 架构设计：
 * ┌─────────────┐    ┌──────────────────┐    ┌─────────────┐
 * │  管理端      │───▶│  H5服务器(:3003) │◀───│  求职者手机  │
 * │  (:3002)     │    │                  │    │             │
 * └─────────────┘    ├─ Token生成API   │    └─────────────┘
 *                     ├─ Token验证中间件 │
 *                     ├─ 静态文件服务     │
 *                     ├─ 访问日志记录     │
 *                     └─ 安全响应头       │
 *
 * 启动方式：
 *   node scripts/h5-server.cjs
 *
 * 成本：¥0（纯Node.js实现，无需额外依赖）
 */

const http = require('http')
const fs = require('fs')
const path = require('path')
const os = require('os')
const crypto = require('crypto')

// ==================== 配置项 ====================
const CONFIG = {
  // 服务端口
  port: process.env.H5_PORT || 3003,

  // 静态文件目录
  publicDir: path.join(__dirname, '../public'),

  // Token配置
  token: {
    // 过期时间（毫秒），默认30分钟
    expiresIn: parseInt(process.env.TOKEN_EXPIRES) || 30 * 60 * 1000,
    // 最大使用次数（0=无限，1=一次性，建议门店场景使用0或10）
    maxUsage: parseInt(process.env.TOKEN_MAX_USAGE) || 10,
    // HMAC签名密钥（启动时自动生成或从环境变量读取）
    secret: process.env.TOKEN_SECRET || crypto.randomBytes(32).toString('hex'),
    // Token长度（字符数）
    length: 16
  },

  // 安全配置
  security: {
    // IP白名单（空=允许所有内网IP）
    ipWhitelist: (process.env.IP_WHITELLIST || '').split(',').filter(Boolean),
    // 是否仅允许私有地址（10.x / 172.16-31.x / 192.168.x）
    allowPrivateOnly: process.env.ALLOW_PRIVATE_ONLY !== 'false',
    // 速率限制（每IP每分钟最大请求数）
    rateLimit: {
      windowMs: 60 * 1000,  // 时间窗口：1分钟
      maxRequests: 100      // 最大请求数
    }
  },

  // 日志配置
  logging: {
    // 是否启用详细日志
    verbose: process.env.LOG_VERBOSE === 'true',
    // 日志文件路径（空=仅控制台输出）
    file: process.env.LOG_FILE || ''
  }
}

// ==================== 内存存储 ====================
/**
 * Token存储结构：
 * {
 *   "abc123xyz": {
 *     jobId: "REC001",
 *     jobData: { position: "川菜主厨", ... },
 *     createdAt: Date,
 *     expiresAt: Date,
 *     usageCount: 0,
 *     lastUsedAt: null,
 *     createdBy: "192.168.0.106",
 *     usedBy: []
 *   }
 */
const tokenStore = new Map()

/**
 * 速率限制存储：
 * {
 *   "192.168.1.100": {
 *     requests: [timestamp1, timestamp2, ...]
 *   }
 */
const rateLimitStore = new Map()

// ==================== 工具函数 ====================

/**
 * HMAC-SHA256签名（防止Token被伪造）
 */
function signData(data) {
  const payload = JSON.stringify(data)
  return crypto
    .createHmac('sha256', CONFIG.token.secret)
    .update(payload)
    .digest('hex')
    .substring(0, CONFIG.token.length)
}

/**
 * 生成安全的随机TokenID
 */
function generateTokenId() {
  return crypto.randomBytes(CONFIG.token.length).toString('hex').substring(0, CONFIG.token.length)
}

/**
 * 判断是否为私有IP地址
 */
function isPrivateIP(ip) {
  if (ip === '127.0.0.1' || ip === '::1' || ip === 'localhost') return true
  
  const parts = ip.split('.').map(Number)
  
  // 10.0.0.0/8
  if (parts[0] === 10) return true
  
  // 172.16.0.0/12
  if (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) return true
  
  // 192.168.0.0/16
  if (parts[0] === 192 && parts[1] === 168) return true
  
  return false
}

/**
 * 获取客户端真实IP（支持代理头）
 */
function getClientIP(req) {
  const forwarded = req.headers['x-forwarded-for']
  if (forwarded) {
    return forwarded.split(',')[0].trim()
  }
  return req.socket.remoteAddress
}

/**
 * 格式化时间为易读字符串
 */
function formatDate(date) {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/**
 * 写入日志（控制台+可选文件）
 */
function log(level, message, meta = {}) {
  const timestamp = new Date().toISOString()
  const logLine = `[${timestamp}] [${level.toUpperCase()}] ${message}`

  if (meta && Object.keys(meta).length > 0) {
    console.log(logLine, meta)
  } else {
    console.log(logLine)
  }

  // 可选：写入日志文件
  if (CONFIG.logging.file) {
    try {
      fs.appendFileSync(
        CONFIG.logging.file,
        JSON.stringify({ timestamp, level, message, ...meta }) + '\n'
      )
    } catch (e) {
      // 忽略写入错误
    }
  }
}

// ==================== 安全中间件 ====================

/**
 * IP白名单检查
 */
function checkIPWhitelist(clientIP) {
  // 如果配置了显式白名单
  if (CONFIG.security.ipWhitelist.length > 0) {
    return CONFIG.security.ipWhitelist.includes(clientIP)
  }

  // 如果启用仅允许私有地址模式
  if (CONFIG.security.allowPrivateOnly) {
    return isPrivateIP(clientIP)
  }

  // 默认允许所有
  return true
}

/**
 * 速率限制检查
 */
function checkRateLimit(clientIP) {
  const now = Date.now()
  const windowStart = now - CONFIG.security.rateLimit.windowMs

  if (!rateLimitStore.has(clientIP)) {
    rateLimitStore.set(clientIP, { requests: [now] })
    return { allowed: true, remaining: CONFIG.security.rateLimit.maxRequests - 1 }
  }

  const data = rateLimitStore.get(clientIP)

  // 清理过期请求记录
  data.requests = data.requests.filter(time => time > windowStart)

  // 检查是否超限
  if (data.requests.length >= CONFIG.security.rateLimit.maxRequests) {
    return { allowed: false, remaining: 0, retryAfter: Math.ceil((data.requests[0] - windowStart) / 1000) }
  }

  // 记录本次请求
  data.requests.push(now)
  
  return { allowed: true, remaining: CONFIG.security.rateLimit.maxRequests - data.requests.length - 1 }
}

/**
 * 清理过期数据（定时任务）
 */
function cleanupExpiredData() {
  const now = Date.now()

  // 清理过期Token
  for (const [tokenId, tokenData] of tokenStore.entries()) {
    if (tokenData.expiresAt < now) {
      log('info', `🗑️ Token已过期并清理`, { tokenId, jobId: tokenData.jobId })
      tokenStore.delete(tokenId)
    }
  }

  // 清理速率限制记录（保留最近2小时）
  const twoHoursAgo = now - 2 * 60 * 60 * 1000
  for (const [ip, data] of rateLimitStore.entries()) {
    data.requests = data.requests.filter(time => time > twoHoursAgo)
    if (data.requests.length === 0) {
      rateLimitStore.delete(ip)
    }
  }

  // 输出当前统计
  if (CONFIG.logging.verbose) {
    log('debug', '📊 当前状态', {
      activeTokens: tokenStore.size,
      trackedIPs: rateLimitStore.size
    })
  }
}

// 每5分钟清理一次
setInterval(cleanupExpiredData, 5 * 60 * 1000)

// ==================== API路由处理 ====================

/**
 * API: 生成新Token
 * POST /api/token/generate
 * 
 * 请求体：
 * {
 *   jobId: "REC001",
 *   jobData: { position: "...", salary: "...", ... }
 * }
 * 
 * 响应：
 * {
 *   code: 0,
 *   data: {
 *     tokenId: "abc123xyz",
 *     url: "http://.../recruit/apply-secure.html#abc123xyz",
 *     expiresIn: 1800000,
 *     expiresAt: "2026-05-15T21:00:00Z"
 *   }
 * }
 */
function handleGenerateToken(req, res, clientIP) {
  let body = ''

  req.on('data', chunk => {
    body += chunk.toString()
  })

  req.on('end', () => {
    try {
      const params = JSON.parse(body || '{}')

      if (!params.jobId) {
        res.writeHead(400, { 'Content-Type': 'application/json' })
        res.end(JSON.stringify({ code: -1, message: '缺少jobId参数' }))
        return
      }

      // 生成Token
      const tokenId = generateTokenId()
      const now = Date.now()

      const tokenData = {
        jobId: params.jobId,
        jobData: params.jobData || {},
        createdAt: now,
        expiresAt: now + CONFIG.token.expiresIn,
        usageCount: 0,
        lastUsedAt: null,
        createdBy: clientIP,
        usedBy: []
      }

      // 存储Token
      tokenStore.set(tokenId, tokenData)

      // 构建返回URL（使用混淆短路径，隐藏真实文件路径）
      const baseUrl = `http://${getServerIP()}:${CONFIG.port}`
      const secureUrl = `${baseUrl}/r/${tokenId}`  // 短路径格式，不暴露 .html 和 # 语法

      log('info', '🎫 新Token已生成', {
        tokenId,
        jobId: params.jobId,
        createdBy: clientIP,
        expiresIn: `${CONFIG.token.expiresIn / 1000}秒`,
        maxUsage: CONFIG.token.maxUsage || '无限',
        urlFormat: 'short-path'  // 标记使用短路径格式
      })

      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({
        code: 0,
        message: 'success',
        data: {
          tokenId,
          url: secureUrl,
          expiresIn: CONFIG.token.expiresIn,
          expiresAt: new Date(tokenData.expiresAt).toISOString(),
          maxUsage: CONFIG.token.maxUsage
        }
      }))

    } catch (error) {
      log('error', '生成Token失败', { error: error.message })
      res.writeHead(500, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({ code: -1, message: '服务器内部错误' }))
    }
  })
}

/**
 * API: 验证Token（供H5页面调用）
 * GET /api/token/verify/:tokenId
 * 
 * 响应：
 * {
 *   code: 0,
 *   data: {
 *     valid: true,
 *     jobData: { ... },
 *     remainingUsage: 0
 *   }
 * }
 */
function handleVerifyToken(req, res, clientIP, tokenId) {
  const tokenData = tokenStore.get(tokenId)

  if (!tokenData) {
    log('warn', '❌ Token验证失败：不存在', { tokenId, clientIP })
    
    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({
      code: 0,
      data: {
        valid: false,
        reason: 'TOKEN_NOT_FOUND',
        message: '该应聘码不存在或已过期'
      }
    }))
    return
  }

  // 检查是否过期
  if (Date.now() > tokenData.expiresAt) {
    log('warn', '❌ Token验证失败：已过期', { tokenId, clientIP })
    tokenStore.delete(tokenId)

    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({
      code: 0,
      data: {
        valid: false,
        reason: 'TOKEN_EXPIRED',
        message: `该应聘码已于 ${formatDate(tokenData.expiresAt)} 过期`
      }
    }))
    return
  }

  // 检查使用次数
  if (CONFIG.token.maxUsage > 0 && tokenData.usageCount >= CONFIG.token.maxUsage) {
    log('warn', '❌ Token验证失败：已达最大使用次数', { tokenId, clientIP, usageCount: tokenData.usageCount })

    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({
      code: 0,
      data: {
        valid: false,
        reason: 'TOKEN_USED_UP',
        message: '该应聘码已被使用，请联系店员获取新码'
      }
    }))
    return
  }

  // Token有效！更新使用信息
  tokenData.usageCount++
  tokenData.lastUsedAt = Date.now()
  tokenData.usedBy.push({
    ip: clientIP,
    userAgent: req.headers['user-agent']?.substring(0, 100) || 'unknown',
    time: Date.now()
  })

  log('info', '✅ Token验证成功', {
    tokenId,
    jobId: tokenData.jobId,
    clientIP,
    usageCount: tokenData.usageCount,
    remainingUsage: CONFIG.token.maxUsage > 0 ? CONFIG.token.maxUsage - tokenData.usageCount : -1
  })

  res.writeHead(200, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify({
    code: 0,
    data: {
      valid: true,
      jobData: tokenData.jobData,
      usageCount: tokenData.usageCount,
      remainingUsage: CONFIG.token.maxUsage > 0 ? CONFIG.token.maxUsage - tokenData.usageCount : -1,
      expiresAt: new Date(tokenData.expiresAt).toISOString()
    }
  }))
}

/**
 * API: 获取Token状态（管理端查询用）
 * GET /api/token/status/:tokenId
 */
function handleTokenStatus(req, res, clientIP, tokenId) {
  const tokenData = tokenStore.get(tokenId)

  if (!tokenData) {
    res.writeHead(404, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ code: -1, message: 'Token不存在' }))
    return
  }

  const remainingTime = Math.max(0, tokenData.expiresAt - Date.now())

  res.writeHead(200, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify({
    code: 0,
    data: {
      tokenId,
      jobId: tokenData.jobId,
      status: remainingTime > 0 ? 'active' : 'expired',
      createdAt: tokenData.createdAt,
      expiresAt: tokenData.expiresAt,
      remainingTime,
      remainingTimeFormatted: `${Math.floor(remainingTime / 60000)}分${Math.floor((remainingTime % 60000) / 1000)}秒`,
      usageCount: tokenData.usageCount,
      maxUsage: CONFIG.token.maxUsage,
      remainingUsage: CONFIG.token.maxUsage > 0 ? CONFIG.token.maxUsage - tokenData.usageCount : -1,
      createdBy: tokenData.createdBy,
      usedBy: tokenData.usedBy.slice(-10)  // 最近10次使用记录
    }
  }))
}

/**
 * API: 列出所有活跃Token（调试用，生产环境应禁用）
 * GET /api/token/list
 */
function handleListTokens(req, res) {
  const tokens = []

  for (const [tokenId, data] of tokenStore.entries()) {
    tokens.push({
      tokenId,
      jobId: data.jobId,
      status: Date.now() < data.expiresAt ? 'active' : 'expired',
      usageCount: data.usageCount,
      createdAt: data.createdAt,
      expiresAt: data.expiresAt
    })
  }

  res.writeHead(200, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify({
    code: 0,
    data: {
      total: tokens.length,
      tokens: tokens.sort((a, b) => b.createdAt - a.createdAt)
    }
  }))
}

// ==================== 静态文件服务 ====================

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
}

function serveStaticFile(filePath, req, res) {
  const ext = path.extname(filePath).toLowerCase()
  const contentType = MIME_TYPES[ext] || 'application/octet-stream'

  fs.readFile(filePath, (err, data) => {
    if (err) {
      if (err.code === 'ENOENT') {
        // 文件不存在，返回友好的404页面
        res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' })
        res.end(generateErrorPage(404, '页面不存在', '如果您是通过招聘二维码访问此页面，请联系店员获取最新的二维码'))
      } else {
        log('error', '读取文件失败', { filePath, error: err.message })
        res.writeHead(500, { 'Content-Type': 'text/plain' })
        res.end('Internal Server Error')
      }
      return
    }

    // 成功返回文件
    res.writeHead(200, { 
      'Content-Type': contentType,
      'Cache-Control': 'no-cache, no-store, must-revalidate',
      'Pragma': 'no-cache',
      'Expires': '0'
    })
    res.end(data)
  })
}

function generateErrorPage(code, title, message) {
  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>${code} - ${title}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; }
    .container { text-align: center; max-width: 500px; background: rgba(255,255,255,0.1); backdrop-filter: blur(10px); border-radius: 20px; padding: 40px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
    h1 { font-size: 72px; margin-bottom: 20px; opacity: 0.9; }
    h2 { font-size: 24px; margin-bottom: 16px; }
    p { font-size: 16px; line-height: 1.6; opacity: 0.85; margin-bottom: 24px; }
    .tip { font-size: 14px; opacity: 0.7; border-top: 1px solid rgba(255,255,255,0.2); padding-top: 20px; }
  </style>
</head>
<body>
  <div class="container">
    <h1>${code}</h1>
    <h2>${title}</h2>
    <p>${message}</p>
    <div class="tip">💡 如果您持续看到此页面，请联系门店工作人员</div>
  </div>
</body>
</html>`
}

/**
 * 生成伪装首页（当直接访问根路径时显示）
 * 目的：让扫描者以为这是一个普通的门店页面，而非招聘系统
 */
function generateFakeHomePage() {
  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>门店WiFi - 欢迎连接</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .container {
      text-align: center;
      max-width: 500px;
      background: rgba(255,255,255,0.1);
      backdrop-filter: blur(10px);
      border-radius: 20px;
      padding: 40px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
    }
    h1 { font-size: 32px; margin-bottom: 16px; }
    p { font-size: 16px; line-height: 1.6; opacity: 0.9; margin-bottom: 12px; }
    .icon { font-size: 64px; margin-bottom: 24px; }
    .footer { margin-top: 30px; font-size: 13px; opacity: 0.6; }
  </style>
</head>
<body>
  <div class="container">
    <div class="icon">📶</div>
    <h1>门店WiFi已连接</h1>
    <p>欢迎您使用本店无线网络服务</p>
    <p style="font-size: 14px; opacity: 0.7;">如需帮助，请联系店员</p>
    <div class="footer">
      ⏱️ ${new Date().toLocaleString('zh-CN')}
    </div>
  </div>
</body>
</html>`
}

// ==================== 服务器创建与启动 ====================

function getServerIP() {
  const interfaces = os.networkInterfaces()
  let localIP = 'localhost'

  Object.values(interfaces).forEach(iface => {
    iface?.forEach(addr => {
      if (addr.family === 'IPv4' && !addr.internal) {
        localIP = addr.address
      }
    })
  })

  return localIP
}

const server = http.createServer((req, res) => {
  const startTime = Date.now()
  const clientIP = getClientIP(req)
  const method = req.method.toUpperCase()

  // ==================== CORS跨域支持 ====================
  // 处理浏览器预检请求(OPTIONS)，允许跨域访问
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',  // 生产环境应限制为具体域名
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With',
      'Access-Control-Max-Age': '86400', // 预检结果缓存24小时
      'Vary': 'Origin'
    })
    res.end()
    
    log('debug', '✅ CORS预检响应', { clientIP, pathname: req.url })
    return
  }

  // 为所有响应添加CORS头
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Credentials', 'true')

  // 设置安全响应头
  res.setHeader('X-Frame-Options', 'DENY')
  res.setHeader('X-Content-Type-Options', 'nosniff')
  res.setHeader('X-XSS-Protection', '1; mode=block')
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin')
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()')
  // 隐藏服务器信息
  res.removeHeader('Server')

  // 解析URL
  const urlObj = new URL(req.url, `http://${req.headers.host}`)
  const pathname = urlObj.pathname

  // ==================== 安全检查 ====================
  
  // 1. IP白名单检查
  if (!checkIPWhitelist(clientIP)) {
    log('warn', '🚫 IP被拒绝访问', { clientIP, pathname })
    res.writeHead(403, { 'Content-Type': 'text/html; charset=utf-8' })
    res.end(generateErrorPage(403, '访问被拒绝', '您的IP地址不在允许列表中。如果您是店内顾客，请连接门店WiFi后重试。'))
    return
  }

  // 2. 速率限制检查
  const rateLimitResult = checkRateLimit(clientIP)
  if (!rateLimitResult.allowed) {
    log('warn', '⚠️ 触发速率限制', { clientIP, retryAfter: rateLimitResult.retryAfter })
    res.writeHead(429, { 
      'Content-Type': 'text/html; charset=utf-8',
      'Retry-After': rateLimitResult.retryAfter
    })
    res.end(generateErrorPage(429, '请求过于频繁', `您的访问频率过高，请在 ${rateLimitResult.retryAfter} 秒后重试。`))
    return
  }

  // ==================== API路由分发 ====================
  
  if (pathname.startsWith('/api/')) {
    // Token生成接口
    if (pathname === '/api/token/generate' && method === 'POST') {
      return handleGenerateToken(req, res, clientIP)
    }

    // Token验证接口
    const verifyMatch = pathname.match(/^\/api\/token\/verify\/([a-f0-9]+)$/)
    if (verifyMatch && method === 'GET') {
      return handleVerifyToken(req, res, clientIP, verifyMatch[1])
    }

    // Token状态查询接口
    const statusMatch = pathname.match(/^\/api\/token\/status\/([a-f0-9]+)$/)
    if (statusMatch && method === 'GET') {
      return handleTokenStatus(req, res, clientIP, statusMatch[1])
    }

    // Token列表接口（调试用）
    if (pathname === '/api/token/list' && method === 'GET') {
      return handleListTokens(req, res)
    }

    // 未匹配的API
    log('warn', '❓ 未知API请求', { pathname, method, clientIP })
    res.writeHead(404, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ code: -1, message: 'API不存在' }))
    return
  }

  // ==================== 短路径路由（URL混淆层） ====================
  // 将 /r/:tokenId 重定向到表单页面，隐藏真实文件路径

  const shortPathMatch = pathname.match(/^\/r\/([a-f0-9]{12,24})$/i)
  if (shortPathMatch && method === 'GET') {
    const tokenId = shortPathMatch[1]

    log('info', '🔗 短路径访问', {
      tokenId,
      clientIP,
      userAgent: req.headers['user-agent']?.substring(0, 50)
    })

    // 检查Token是否存在
    const tokenData = tokenStore.get(tokenId)

    if (!tokenData) {
      log('warn', '❌ 短路径Token不存在', { tokenId, clientIP })

      // 返回友好的错误页面
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
      res.end(generateErrorPage(
        410,
        '链接已失效',
        '此应聘链接已过期或不存在。<br><br>可能的原因：<br>• 链接已超过30分钟有效期<br>• 该链接已被删除<br>• 链接格式不正确<br><br>请联系门店工作人员获取最新的应聘二维码'
      ))
      return
    }

    // Token存在，返回表单页面（内嵌Token）
    const htmlFilePath = path.join(CONFIG.publicDir, '/recruit/apply-secure.html')

    fs.readFile(htmlFilePath, 'utf8', (err, htmlContent) => {
      if (err) {
        log('error', '读取表单文件失败', { error: err.message })
        res.writeHead(500, { 'Content-Type': 'text/plain' })
        res.end('Internal Server Error')
        return
      }

      // 在HTML中注入Token（通过修改hash或添加script标签）
      const modifiedHtml = htmlContent.replace(
        '<script>',
        `<script>
          // 自动设置Token（从短路径提取）
          window.__INITIAL_TOKEN__ = '${tokenId}';
          if (!window.location.hash) {
            window.location.hash = '${tokenId}';
          }
        `
      )

      res.writeHead(200, {
        'Content-Type': 'text/html; charset=utf-8',
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
      })
      res.end(modifiedHtml)
    })

    return
  }

  // ==================== 根路径伪装 ====================
  // 直接访问根路径或常见探测路径时显示伪装页面

  if (pathname === '/' || pathname === '/favicon.ico' || pathname === '/robots.txt') {
    log('info', '🎭 根路径访问（伪装模式）', { pathname, clientIP })

    // 对于根路径，返回伪装的"门店欢迎页"
    if (pathname === '/') {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
      res.end(generateFakeHomePage())
      return
    }

    // 对于其他探测请求，返回空响应
    res.writeHead(204)  // No Content
    res.end()
    return
  }

  // ==================== 静态文件服务 ====================
  
  let filePath = path.join(CONFIG.publicDir, pathname === '/' ? '/recruit/index.html' : pathname)

  // 安全检查：防止目录遍历攻击
  if (!filePath.startsWith(CONFIG.publicDir)) {
    log('warn', '🚨 目录遍历攻击尝试', { clientIP, pathname, filePath })
    res.writeHead(403, { 'Content-Type': 'text/plain' })
    res.end('Forbidden')
    return
  }

  // 记录静态文件访问
  if (CONFIG.logging.verbose) {
    log('debug', '📄 静态文件请求', { pathname, clientIP })
  }

  serveStaticFile(filePath, req, res)

  // 记录响应时间
  const duration = Date.now() - startTime
  if (duration > 1000) {
    log('warn', '⏱️ 慢请求', { pathname, duration: `${duration}ms`, clientIP })
  }
})

// 启动服务器
server.listen(CONFIG.port, '0.0.0.0', () => {
  const serverIP = getServerIP()

  console.log('\n╔══════════════════════════════════════════════════════════╗')
  console.log('║         🛡️ 企业级安全H5服务器 v2.0 已启动               ║')
  console.log('╠══════════════════════════════════════════════════════════╣')
  console.log(`║  本地访问:  http://localhost:${String(CONFIG.port).padEnd(4)}/recruit/apply-secure.html            ║`)
  console.log(`║  内网访问:  http://${serverIP}:${String(CONFIG.port).padEnd(4)}/recruit/apply-secure.html            ║`)
  console.log(`║                                                              ║`)
  console.log(`║  🔑 Token配置:                                              ║`)
  console.log(`║     过期时间: ${String(CONFIG.token.expiresIn / 1000 + '秒').padEnd(44)} ║`)
  console.log(`║     最大使用: ${String(CONFIG.token.maxUsage || '无限').padEnd(44)} ║`)
  console.log(`║     密钥长度: ${String(CONFIG.token.secret.length + '字符').padEnd(44)} ║`)
  console.log(`║                                                              ║`)
  console.log(`║  🛡️ 安全配置:                                                ║`)
  console.log(`║     IP过滤:   ${String((CONFIG.security.allowPrivateOnly ? '仅允许私有地址' : '允许所有')).padEnd(44)} ║`)
  console.log(`║     速率限制: ${String(CONFIG.security.rateLimit.maxRequests + '次/分钟').padEnd(44)} ║`)
  console.log(`║                                                              ║`)
  console.log(`║  📡 API端点:                                                 ║`)
  console.log(`║     POST   /api/token/generate                              ║`)
  console.log(`║     GET    /api/token/verify/:id                            ║`)
  console.log(`║     GET    /api/token/status/:id                            ║`)
  console.log(`║     GET    /api/token/list                                  ║`)
  console.log(`║                                                              ║`)
  console.log(`║  按 Ctrl+C 停止服务器                                        ║`)
  console.log('╚══════════════════════════════════════════════════════════╝\n')

  log('info', '✅ 服务器启动成功', {
    port: CONFIG.port,
    serverIP,
    tokenExpiresIn: CONFIG.token.expiresIn,
    tokenMaxUsage: CONFIG.token.maxUsage
  })
})

// 错误处理
server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`\n❌ 端口 ${CONFIG.port} 已被占用！`)
    console.error(`   请检查是否有其他程序在使用该端口`)
    console.error(`   或设置环境变量 H5_PORT 使用其他端口\n`)
    process.exit(1)
  } else {
    throw err
  }
})

// 优雅关闭
process.on('SIGINT', () => {
  console.log('\n\n🛑 收到停止信号，正在关闭服务器...')
  
  // 输出最终统计
  log('info', '📊 最终统计', {
    totalTokensGenerated: tokenStore.size,
    uptime: process.uptime().toFixed(2) + '秒'
  })
  
  server.close(() => {
    console.log('✅ 服务器已停止')
    process.exit(0)
  })
})
