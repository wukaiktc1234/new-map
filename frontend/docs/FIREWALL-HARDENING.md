# Windows防火墙安全加固方案 - 门店招聘系统专用

## 🚨 当前问题诊断

### 发现的安全隐患：

| 端口 | 服务 | 防火墙状态 | 风险等级 |
|------|------|-----------|---------|
| **3002** | 管理前端(Vue) | ⚠️ 已允许访问 | 🔴 高危 |
| **8081** | 后端API(Spring Boot) | ⚠️ 已允许访问 | 🔴 高危 |
| **3003** | H5招聘表单(Node.js) | ❌ 未添加（导致无法使用） | 🟢 正常（应开放）|
| **5173** | Vite开发服务器 | 未检测 | 🟡 待确认 |

### 核心矛盾：

```
❌ 错误配置：
   - 管理端(3002)和后端(8081)对所有人开放
   - H5服务(3003)反而被阻止
   - 本末倒置！

✅ 正确配置应该是：
   - H5服务(3003)：对所有网络开放（求职者需要访问）
   - 管理端(3002)：仅对内网特定IP开放（仅店员电脑）
   - 后端(8081)：仅对本地或管理端IP开放（不对外暴露）
```

---

## 🎯 加固方案（三步走）

### Step 1: 删除现有的危险规则（5分钟）

#### 操作目的：
移除不当开放的3002和8081端口规则

#### 操作步骤：

**方法A：通过图形界面**

1. 打开"高级安全 Windows Defender 防火墙"
2. 左侧点击"**入站规则**"
3. 在列表中查找包含以下内容的规则：
   - 端口号：`3002`
   - 端口号：`8081`
4. 右键点击找到的规则 → "**删除**"
5. 确认删除

**方法B：通过命令行（管理员PowerShell）**

```powershell
# 查看所有涉及这些端口的规则
Get-NetFirewallRule | Where-Object { $_.Enabled -eq 'True' } | 
  Get-NetFirewallPortFilter | 
  Where-Object { $_.LocalPort -match '3002|8081' } |
  ForEach-Object {
    Write-Host "发现规则: 端口 $($_.LocalPort)"
  }

# 删除所有允许3002端口的入站规则
Get-NetFirewallRule | Where-Object { $_.Enabled -eq 'True' -and $_.Direction -eq 'Inbound' } |
  Get-NetFirewallPortFilter |
  Where-Object { $_.LocalPort -match '^3002$' } |
  ForEach-Object {
    $rule = Get-NetFirewallRule -AssociatedPortFilter $_
    Remove-NetFirewallRule -InputObject $rule
    Write-Host "✅ 已删除端口3002的入站规则"
  }

# 删除所有允许8081端口的入站规则
Get-NetFirewallRule | Where-Object { $_.Enabled -eq 'True' -and $_.Direction -eq 'Inbound' } |
  Get-NetFirewallPortFilter |
  Where-Object { $_.LocalPort -match '^8081$' } |
  ForEach-Object {
    $rule = Get-NetFirewallRule -AssociatedPortFilter $_
    Remove-NetFirewallRule -InputObject $rule
    Write-Host "✅ 已删除端口8081的入站规则"
  }
```

---

### Step 2: 添加正确的H5服务规则（5分钟）

#### 操作目的：
仅开放H5招聘表单服务给访客网络

#### 操作步骤：

**创建规则：仅允许H5服务(3003)**

```powershell
# 以管理员身份运行PowerShell

New-NetFirewallRule `
  -DisplayName "H5招聘表单服务-端口3003" `
  -Direction Inbound `
  -LocalPort 3003 `
  -Protocol TCP `
  -Action Allow `
  -Profile Any `
  -Description "允许访客WiFi设备访问门店招聘H5表单(仅此端口)"

Write-Host "✅ H5服务防火墙规则已创建"
```

**或者使用图形界面**（参考 FIREWALL-CONFIG-GUIDE.md）：
- 名称：`H5招聘表单服务-端口3003`
- 端口：`3003/TCP`
- 操作：`允许连接`
- 配置文件：全部勾选（域、专用、公用）

---

### Step 3: （可选）限制管理端访问来源（10分钟）

#### 操作目的：
即使管理端需要远程访问，也仅允许特定IP

#### 适用场景：
- 店员需要在其他设备上访问管理端
- 或者需要从家庭网络远程管理

#### 操作步骤：

**方案A：限制为特定IP段（推荐）**

```powershell
# 假设店内网段是 192.168.0.0/24
# 仅允许此网段访问管理端

New-NetFirewallRule `
  -DisplayName "管理前端-仅限内网" `
  -Direction Inbound `
  -LocalPort 3002 `
  -Protocol TCP `
  -Action Allow `
  -RemoteAddress 192.168.0.0/24 `
  -Profile Any `
  -Description "仅允许内网(192.168.0.x)访问管理前端"

Write-Host "✅ 管理端访问限制规则已创建（仅限内网）"
```

**方案B：完全禁止外部访问管理端（最安全）**

```powershell
# 如果不需要远程访问管理端，直接不创建任何规则即可
# Windows防火墙默认阻止所有未明确允许的入站连接

# 但要确保本地可以访问（通常localhost默认允许）
# 测试：
# curl http://localhost:3002/
# 应该能正常访问
```

**方案C：需要从外网访问时（高级）**

```powershell
# 使用VPN或SSH隧道，不要直接暴露端口
# 这是生产环境的标准做法

# 或者使用反向代理 + 身份验证
# 例如：Nginx + Basic Auth + HTTPS
```

---

## ✅ 验证加固效果

### 测试清单：

#### 从访客WiFi的手机测试：

```
□ 测试1: H5服务应该可访问
   访问：http://192.168.0.106:3003/
   预期：✅ 显示伪装页面或表单页面

□ 测试2: 管理端应该不可访问（或受限）
   访问：http://192.168.0.106:3002/
   预选结果：
     A) ❌ 无法连接（超时）→ 最佳！完全隔离
     B) 🔒 显示登录页但需认证 → 可接受（有认证保护）
     C) ✅ 直接进入管理后台 → 危险！需要检查规则

□ 测试3: 后端API应该不可访问
   访问：http://192.168.0.106:8081/api/users
   预期：❌ 无法连接 或 返回401/403错误
```

#### 从店员电脑（本地）测试：

```
□ 测试4: 所有服务本地应正常
   localhost:3002 → ✅ 管理端正常
   localhost:8081 → ✅ 后端正常
   localhost:3003 → ✅ H5服务正常
```

---

## 📋 最终目标架构

### 安全加固后的理想状态：

```
访客WiFi网络（求职者手机）:
├─ ✅ 可访问：端口3003（H5招聘表单）
│   └─ 无需认证（公开服务）
│
├─ ❌ 不可访问：端口3002（管理前端）
│   └─ 即使能访问也需要登录认证
│
└─ ❌ 不可访问：端口8081（后端API）
    └─ 即使能访问也需要JWT Token认证

主WiFi/有线网络（店员设备）:
├─ ✅ 可访问：所有端口（3002, 3003, 8081）
│   └─ 受信任的内网环境
│
└─ 应用层认证仍然生效
    ├─ 管理端：Session/Cookie认证
    └─ 后端API：JWT Token认证
```

---

## 🔄 回滚方案（如果出问题）

### 如果误删了重要规则导致无法访问：

```powershell
# 临时关闭所有防火墙（紧急情况用！）
Set-NetFirewallProfile -All -Enabled False

# 测试完成后务必重新开启：
Set-NetFirewallProfile -All -Enabled True

# 然后重新按照本文档正确配置
```

---

## 📝 维护建议

### 定期检查（每月一次）：

```powershell
# 创建一个检查脚本，保存为 check-firewall.ps1

Write-Host "=== 防火墙安全检查 ===" ; Write-Host ""
Write-Host "1. 当前允许的入站端口:" ; 
Get-NetFirewallRule | Where-Object { $_.Enabled -eq 'True' -and $_.Direction -eq 'Inbound' -and $_.Action -eq 'Allow' } | 
  Get-NetFirewallPortFilter | 
  Select-Object LocalPort, Protocol -Unique | 
  Format-Table -AutoSize

Write-Host ""
Write-Host "2. 特别关注的高危端口:"
$criticalPorts = @(3002, 8081, 3306, 1433)
foreach ($port in $criticalPorts) {
  $rules = Get-NetFirewallRule | Where-Object { $_.Enabled -eq 'True' -and $_.Direction -eq 'Inbound' } | 
    Get-NetFirewallPortFilter | 
    Where-Object { $_.LocalPort -match "^${port}$" }
  
  if ($rules) {
    Write-Host "⚠️ 端口 $port : 对外开放！" -ForegroundColor Red
  } else {
    Write-Host "✅ 端口 $port : 已保护" -ForegroundColor Green
  }
}
```

运行方式：
```powershell
# 以管理员身份执行
.\check-firewall.ps1
```

---

## 🆘 应急响应预案

### 发现异常访问时：

**症状**：看到大量来自陌生IP的连接请求

**立即行动**：
```powershell
# 1. 查看当前连接状态
netstat -an | findstr ":3002|:8081|:3003"

# 2. 如发现可疑IP，临时封禁
# （需要结合路由器的IP过滤功能）

# 3. 收集证据
# 截图、记录时间戳、保留日志

# 4. 重启H5服务器清除所有Token
# 在终端1中 Ctrl+C，然后重新启动

# 5. 通知相关人员
```

---

## 📚 相关文档

- [FIREWALL-CONFIG-GUIDE.md](./FIREWALL-CONFIG-GUIDE.md) - 图形界面详细教程
- [AP-ISOLATION-FIX.md](./AP-ISOLATION-FIX.md) - AP隔离问题排查
- [SECURITY-CHECKLIST.md](./SECURITY-CHECKLIST.md) - 完整安全检查清单

---

**最后更新**: 2026-05-16
**适用版本**: Windows 10/11, TL-WDR5660路由器
**安全等级**: 实施后达到 🟢 A级（优秀）
