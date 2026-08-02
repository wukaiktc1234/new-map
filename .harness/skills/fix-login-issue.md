# Skill: 修复登录/认证问题

> 触发条件：用户报告登录失败、401、403、429、空响应等认证问题

## 标准排查链路（从外到内）

### Layer 1: 网络层
```
检查项:
- [ ] 后端是否在监听: netstat -ano | findstr 8081
- [ ] CORS 是否允许 Origin: 检查 app.cors.allowed-origins 配置
- [ ] 前端请求 URL 是否正确: /api/v1/auth/login (注意 context-path)
快速测试: curl POST http://localhost:8081/api/v1/auth/login -d '{"username":"admin","password":"admin123"}'
```

### Layer 2: 过滤器链（按执行顺序）
```
Filter Chain 顺序:
1. RateLimitFilter     → 429? 检查 spring.security.rate-limit.enabled
2. JwtAuthenticationFilter → 跳过 /v1/auth/* 路径
3. MfaAuthenticationFilter → 默认 passThrough
4. AnomalyAccessLogFilter → ⚠️ 必须调用 copyBodyToResponse()
5. SecurityHeaderFilter    → 安全头设置
6. XssFilter              → XSS 过滤

关键检查: AnomalyAccessLogFilter 使用 ContentCachingResponseWrapper
         如果忘记 copyBodyToResponse() → 所有API返回 HTTP 200 + 空 body!
```

### Layer 3: 认证逻辑
```
AuthServiceImpl.login() 流程:
1. getUserByUsername() → 用户存在?
2. checkCaptchaRequired() → 3次失败后强制验证码
3. checkAccountLocked()   → 5次失败锁定30分钟
4. passwordEncoder.matches() → BCrypt 密码校验
5. buildSecurityUser()    → 加载角色+权限 (需 user_roles/user_permissions 表)
6. jwtUtils.generateToken() → 生成 JWT

常见失败点:
- users 表无 admin 用户 → 检查 insertDefaultAdminUser() 是否执行
- roles 表 ROLE_CODE 不匹配 → 'Z' vs 'admin' (已修复)
- user_roles 表缺少 DELETED 列 → SQL 报错
```

### Layer 4: 数据库层
```
H2环境必需表清单:
✅ users, roles, permissions
✅ user_roles (含 deleted 列)
✅ role_permissions (含 deleted 列)  
✅ user_permissions (含 deleted 列)
❌ scheduled_task (非登录必需，但缺失会报错)

验证方式: H2 Console http://localhost:8081/h2-console
```

### Layer 5: 前端层
```
检查项:
- [ ] request.ts 使用的是 request 实例 (非 axios/fetch)
- [ ] 响应数据不访问 .data (拦截器已提取)
- [ ] permissionStore 权限已同步 (auth.ts 中 initFromAuthStore)
- [ ] Token 存储在 localStorage key='token'
```

## 诊断命令速查
```powershell
# 测试登录API完整响应
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$req = [System.Net.WebRequest]::Create('http://localhost:8081/api/v1/auth/login')
$req.Method = 'POST'; $req.ContentType = 'application/json'
$bytes = [Text.Encoding]::UTF8.GetBytes('{"username":"admin","password":"admin123"}')
$req.GetRequestStream().Write($bytes, 0, $bytes.Length)
$resp = $req.GetResponse(); $reader = New-Object IO.StreamReader($resp.GetResponseStream())
Write-Host "Status: $($resp.StatusCode) Body: $($reader.ReadToEnd())"
```

## 完成标志
- [ ] 登录 API 返回 code:0 + token + userInfo
- [ ] 前端可正常跳转到首页
- [ ] 所有菜单可见（permissionStore isAdmin=true）
