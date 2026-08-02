# H5招聘表单 - WiFi环境安全防护方案

## 🎯 核心安全目标

在WiFi局域网环境下，确保：
1. **求职者无法访问管理后台**（端口3002）
2. **H5表单服务器（3003）不被滥用**
3. **API接口只允许合法操作**
4. **防止各种网络攻击**

---

## 🏗️ 安全架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                    外部威胁（WiFi网络）                       │
│  恶意用户、扫描工具、攻击脚本、数据嗅探                      │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              第1层：网络层隔离                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────┐     ┌─────────────────┐              │
│   │  管理端 :3002    │     │ H5表单 :3003    │              │
│   │                 │     │                 │              │
│   │ • Vue SPA       │     │ • 静态HTML      │              │
│   │ • 需要登录       │     │ • API代理       │              │
│   │ • 路由守卫       │     │ • 白名单限制     │              │
│   │ • Token验证      │     │ • 速率限制       │              │
│   └────────┬────────┘     └────────┬────────┘              │
│            │                       │                       │
│            ▼                       ▼                       │
│   ┌──────────────────────────────────────┐               │
│   │           后端服务 :8081             │               │
│   │                                      │               │
│   │ • Spring Security                   │               │
│   │ • JWT认证                            │               │
│   │ • RBAC权限控制                       │               │
│   └──────────────────────────────────────┘               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔒 安全防护层级

### **第1层：端口隔离** ✅ 已实现

| 服务 | 端口 | 访问权限 | 功能 |
|------|------|---------|------|
| 管理端 | 3002 | 仅内部员工 | 完整管理系统 |
| H5表单 | 3003 | 公开访问 | 应聘登记表单 |
| 后端API | 8081 | 不对外暴露 | 数据处理 |

**隔离效果**：
- 求职者只能访问3003端口的静态页面
- 即使知道3002端口地址，也无法绕过登录
- 两个服务完全独立，无共享状态

---

### **第2层：H5服务器安全加固** ✅ 已实现（v2.0）

#### 2.1 API白名单机制

```javascript
// 只允许这些API被代理
apiWhitelist: [
  '/api/v1/recruitment/applicants',  // 提交应聘信息
  '/api/v1/recruitment/jobs'         // 查询岗位列表
]
```

**拦截示例**：
```
✅ 允许：POST /api/v1/recruitment/applicants
✅ 允许：GET /api/v1/recruitment/jobs?status=recruiting
❌ 拦截：GET /api/v1/users          （不在白名单）
❌ 拦截：GET /api/v1/admin/config    （包含危险路径"admin"）
❌ 拦截：DELETE /api/v1/xxx          （方法不允许）
```

#### 2.2 请求频率限制（防刷）

```javascript
rateLimit: {
  windowMs: 60 * 1000,  // 1分钟窗口
  maxRequests: 30        // 每IP最多30次请求
}
```

**防护效果**：
- 防止恶意提交大量垃圾数据
- 防止DDoS攻击导致服务器宕机
- 超限返回 `429 Too Many Requests` + `Retry-After` 头

#### 2.3 HTTP方法限制

```javascript
allowedMethods: ['GET', 'HEAD', 'OPTIONS', 'POST']
// 禁止：PUT、DELETE、PATCH、TRACE、CONNECT等危险方法
```

**原因**：
- PUT/DELETE可能修改或删除数据
- TRACE可能泄露服务器信息
- CONNECT可用于建立隧道攻击

#### 2.4 请求体大小限制

```javascript
maxRequestBodySize: 1024 * 1024  // 1MB上限
```

**防护**：
- 防止上传大文件耗尽服务器内存
- 防止DoS攻击（发送超大请求体）

#### 2.5 路径遍历攻击防护

```javascript
// 检测并阻止类似这样的请求：
// GET /../../../etc/passwd
// GET /..%2f..%2f..%2fetc/passwd
if (isPathTraversal(pathname)) {
  return sendError(400, '非法路径')
}
```

#### 2.6 危险路径黑名单

```javascript
dangerousPaths: [
  '/admin', '/manage', '/login', '/auth',
  '/user', '/system', '/config',
  '.env', '.git', 'wp-admin'
]
```

即使URL包含这些关键词也会被拦截。

---

### **第3层：安全响应头** ✅ 已实现

```http
X-Frame-Options: DENY                          # 禁止iframe嵌套（防止点击劫持）
X-Content-Type-Options: nosniff                  # 禁止MIME类型嗅探
X-XSS-Protection: 1; mode=block                  # XSS过滤器
Referrer-Policy: strict-origin-when-cross-origin # Referrer策略
Content-Security-Policy: ...                     # 内容安全策略
Permissions-Policy: camera=(), microphone=()...   # 权限策略
```

**防护的攻击类型**：
- 点击劫持（Clickjacking）
- MIME类型混淆攻击
- 跨站脚本攻击（XSS）
- 信息泄露（Referrer）

---

### **第4层：访问日志审计** ✅ 已实现

每条请求都会记录：

```
[2024-01-15T10:30:45.123Z] POST /api/v1/recruitment/applicants
| IP: 192.168.0.105
| Status: 200
| UA: Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)
```

**用途**：
- 安全事件追溯
- 异常行为检测
- 攻击模式分析
- 合规审计要求

---

### **第5层：前端路由守卫** ⚠️ 需确认

管理端（3002）应配置路由守卫：

```typescript
// router/index.ts
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    // 未登录，重定向到登录页
    next('/login')
  } else if (to.path === '/login' && token) {
    // 已登录，跳转首页
    next('/')
  } else {
    next()
  }
})
```

**关键点**：
- 所有管理页面都需要认证
- Token过期自动跳转登录
- 静态资源（图片/CSS/JS）不需要认证

---

## 🛡️ 威胁场景与防护对照

| 威胁场景 | 风险等级 | 防护措施 | 状态 |
|---------|---------|---------|------|
| 直接访问管理后台 | 🔴 高 | 端口隔离 + 路由守卫 + JWT认证 | ✅ |
| 通过H5服务器调用管理API | 🔴 高 | API白名单 + 方法限制 | ✅ |
| 暴力提交垃圾数据 | 🟡 中 | 请求频率限制 + 请求体大小限制 | ✅ |
| 路径遍历攻击获取敏感文件 | 🔴 高 | 路径规范化检查 | ✅ |
| XSS攻击窃取Cookie | 🟡 中 | CSP + XSS-Protection头 | ✅ |
| 点击劫持欺骗操作 | 🟡 中 | X-Frame-Options: DENY | ✅ |
| CSRF跨站请求伪造 | 🟢 低 | SameSite Cookie + CSRF Token | ⚠️ 待后端实现 |
| 中间人攻击嗅探数据 | 🟡 中 | HTTPS（生产环境必须） | ⚠️ 待部署 |
| DDoS攻击使服务不可用 | 🔴 高 | 速率限制 + 连接数限制 | ✅ |

---

## 📋 使用方式

### 开发环境（推荐使用安全版）

```bash
# 方式1：一键启动（管理端 + 安全校验版H5服务器）
npm run dev:safe

# 方式2：分别启动
npm run dev                # 启动管理端 (:3002)
npm run h5-server:secure   # 启动安全版H5服务器 (:3003)
```

### 生产环境部署

```bash
# 使用PM2进程管理器（推荐）
pm2 start scripts/h5-server-secure.cjs --name "h5-form"

# 或使用systemd（Linux）
sudo systemctl start h5-form
```

---

## 🔧 配置自定义

通过环境变量调整安全参数：

```bash
# 端口
export H5_PORT=3003

# 后端地址
export API_TARGET=http://localhost:8081

# 速率限制（可选，默认已内置）
# 在代码中修改 SECURITY_CONFIG 对象
```

---

## ✅ 安全检查清单

### 部署前必查项

- [ ] 使用安全版H5服务器 (`h5-server-secure.cjs`)
- [ ] 管理端配置了路由守卫和JWT认证
- [ ] 后端Spring Security已启用
- [ ] API白名单只包含必要接口
- [ ] 生产环境使用HTTPS
- [ ] 防火墙规则正确（仅开放必要端口）
- [ ] 定期查看访问日志

### 定期检查项

- [ ] 监控异常IP（高频请求）
- [ ] 检查是否有未授权的403/404错误
- [ ] 更新依赖包修复已知漏洞
- [ ] 备份安全日志

---

## 🚨 应急响应

### 发现攻击时

1. **立即封禁IP**（在黑名单中添加）
2. **查看日志**确定攻击类型
3. **临时提高**速率限制阈值
4. **通知安全团队**

### 日志中的攻击特征

```
# 路径遍历尝试
[⚠️ 路径遍历攻击尝试] IP: 192.168.0.xxx Path: /../../etc/passwd

# API滥用
[🔒 API白名单拒绝] /api/v1/admin/users

# 频率超限
[429] IP: 192.168.0.xxx | Status: 429 | 请求频率超限
```

---

## 📞 技术支持

如发现安全问题或需要定制化安全策略，请联系开发团队。

---

**文档版本**: v1.0
**最后更新**: 2024-01-15
**适用范围**: H5招聘表单系统 v2.0+
