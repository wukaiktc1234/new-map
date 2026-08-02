# 食品溯源系统安全应急响应预案

## 文档信息

| 项目 | 内容 |
|------|------|
| 版本 | 1.0.0 |
| 生效日期 | 2024-01-01 |
| 最后更新 | 2024-01-01 |
| 负责人 | 运维团队 |
| 审批人 | 技术总监 |

---

## 一、应急响应级别定义

### 级别分类

| 级别 | 名称 | 影响范围 | 响应时间 | 示例场景 |
|------|------|----------|----------|----------|
| P0 | 灾难级 | 系统完全不可用 | 5分钟 | 数据库宕机、大规模DDoS攻击 |
| P1 | 严重级 | 核心功能不可用 | 15分钟 | 登录服务故障、支付功能异常 |
| P2 | 一般级 | 部分功能异常 | 30分钟 | 单个API异常、性能下降 |
| P3 | 轻微级 | 非关键问题 | 2小时 | UI显示异常、非核心功能报错 |

---

## 二、应急响应流程

### 2.1 标准响应流程

```
发现异常 → 初步评估 → 分级上报 → 启动预案 → 执行处置 → 验证恢复 → 总结归档
```

### 2.2 响应时间要求

| 级别 | 响应时间 | 处置时间 | 恢复时间 |
|------|----------|----------|----------|
| P0 | 5分钟 | 30分钟 | 1小时 |
| P1 | 15分钟 | 1小时 | 2小时 |
| P2 | 30分钟 | 2小时 | 4小时 |
| P3 | 2小时 | 4小时 | 8小时 |

---

## 三、常见安全事件处置方案

### 3.1 账户被盗处理流程

#### 发现途径
- 用户举报账户异常
- 监控告警：异地登录、异常操作
- 安全审计：异常权限变更

#### 处置步骤

```bash
# 1. 立即锁定账户
curl -X POST http://localhost:8081/api/v1/admin/users/{userId}/lock \
  -H "Authorization: Bearer {admin-token}"

# 2. 强制下线所有设备
curl -X DELETE http://localhost:8081/api/v1/sessions/user/{userId}/all \
  -H "Authorization: Bearer {admin-token}"

# 3. 将当前Token加入黑名单
redis-cli SET "jwt:blacklist:{token}" 1 EX 86400

# 4. 查询登录日志
curl http://localhost:8081/api/v1/audit/logs?userId={userId}&type=LOGIN \
  -H "Authorization: Bearer {admin-token}"

# 5. 通知用户
# 发送短信/邮件通知用户账户异常，引导重置密码
```

#### 恢复步骤
1. 核实用户身份（手机号/邮箱验证）
2. 重置用户密码
3. 检查并撤销异常权限变更
4. 恢复账户状态
5. 记录事件并归档

---

### 3.2 权限泄露回滚步骤

#### 发现途径
- 权限变更审计告警
- 用户举报数据泄露
- 异常数据访问日志

#### 处置步骤

```bash
# 1. 查询最近的权限变更记录
SELECT * FROM permission_audit_log 
WHERE created_time > NOW() - INTERVAL '24 hours'
ORDER BY created_time DESC;

# 2. 撤销异常权限
DELETE FROM user_role WHERE user_id = {userId} AND role_id = {roleId};
DELETE FROM role_permission WHERE role_id = {roleId} AND permission_id = {permissionId};

# 3. 刷新权限缓存
redis-cli DEL "user:permissions:{userId}"
redis-cli DEL "role:permissions:{roleId}"

# 4. 记录回滚操作
INSERT INTO permission_audit_log (operation, target, operator, reason)
VALUES ('ROLLBACK', '{details}', '{admin}', '权限泄露回滚');
```

#### 恢复步骤
1. 确认权限变更是否合法
2. 非法变更立即回滚
3. 通知相关用户
4. 加强权限审批流程
5. 更新权限审计日志

---

### 3.3 系统攻击响应

#### 3.3.1 DDoS攻击

**识别指标**：
- 请求量突增超过正常值10倍
- 大量429限流响应
- 服务器CPU/内存飙升

**处置步骤**：

```bash
# 1. 查看当前连接状态
netstat -an | grep :8081 | wc -l
netstat -an | grep :8081 | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -nr | head -10

# 2. 启用紧急限流（Nginx）
# 修改 nginx.conf，降低限流阈值
limit_req zone=ddos_global burst=5 nodelay;

# 3. 封禁攻击IP
iptables -A INPUT -s {攻击IP} -j DROP

# 4. 联系CDN/WAF服务商
# 启用Cloudflare或阿里云WAF的高防模式

# 5. 扩容服务器（如需要）
docker-compose up -d --scale backend=3
```

#### 3.3.2 暴力破解攻击

**识别指标**：
- 登录失败次数异常
- 验证码频繁触发
- 同一IP多次尝试不同账户

**处置步骤**：

```bash
# 1. 查看登录失败日志
grep "登录失败" /var/log/food-traceability/security-audit.log | tail -100

# 2. 封禁攻击IP
redis-cli SET "ip:blocked:{攻击IP}" 1 EX 86400

# 3. 强制启用验证码
# 系统已自动在3次失败后启用验证码

# 4. 通知可能受影响的用户
# 发送安全提醒邮件/短信
```

#### 3.3.3 SQL注入攻击

**识别指标**：
- 异常SQL语句日志
- 数据库慢查询告警
- WAF拦截记录

**处置步骤**：

```bash
# 1. 查看数据库慢查询
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 20;

# 2. 分析应用日志中的异常请求
grep -i "union\|select\|insert\|delete\|drop\|update" /var/log/food-traceability/access.log

# 3. 封禁攻击来源
iptables -A INPUT -s {攻击IP} -j DROP

# 4. 检查数据完整性
# 对比数据校验和，确认是否有数据被篡改
```

---

### 3.4 限流绕过处理

**识别指标**：
- 同一用户使用多个IP
- 分布式攻击特征
- 限流阈值失效

**处置步骤**：

```bash
# 1. 分析请求模式
redis-cli KEYS "rate-limit:*" | head -100

# 2. 启用更严格的限流策略
# 用户级别限流 + IP级别限流组合

# 3. 添加行为分析
# 检测机器人行为特征

# 4. 启用CAPTCHA验证
# 强制所有请求通过验证码
```

---

## 四、数据恢复流程

### 4.1 数据库恢复

```bash
# 1. 停止应用服务
docker-compose stop backend

# 2. 恢复最近备份
pg_restore -h localhost -U postgres -d food_traceability /backup/db_20240101.dump

# 3. 验证数据完整性
psql -h localhost -U postgres -d food_traceability -c "SELECT COUNT(*) FROM users;"

# 4. 重启服务
docker-compose start backend
```

### 4.2 Redis缓存恢复

```bash
# 1. 清除所有缓存
redis-cli FLUSHALL

# 2. 重启应用自动重建缓存
docker-compose restart backend
```

---

## 五、联系方式

### 5.1 应急响应团队

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 应急指挥 | 技术总监 | 138-xxxx-xxxx | cto@company.com |
| 安全负责人 | 安全工程师 | 139-xxxx-xxxx | security@company.com |
| 运维负责人 | 运维工程师 | 137-xxxx-xxxx | ops@company.com |
| 开发负责人 | 后端开发 | 136-xxxx-xxxx | dev@company.com |

### 5.2 外部支持

| 服务商 | 用途 | 联系方式 |
|--------|------|----------|
| 阿里云 | 云服务器支持 | 95187 |
| Cloudflare | DDoS防护 | support@cloudflare.com |
| SSL证书商 | 证书问题 | - |

---

## 六、应急演练计划

### 6.1 演练频率

| 类型 | 频率 | 参与人员 |
|------|------|----------|
| 桌面演练 | 每月 | 全体技术人员 |
| 功能演练 | 每季度 | 运维+开发 |
| 全面演练 | 每年 | 全体人员 |

### 6.2 演练记录

每次演练需记录：
- 演练时间、参与人员
- 演练场景
- 发现的问题
- 改进措施
- 预案更新内容

---

## 七、附录

### A. 常用命令速查

```bash
# 查看服务状态
docker-compose ps
systemctl status food-traceability

# 查看日志
tail -f /var/log/food-traceability/food-traceability.log
tail -f /var/log/food-traceability/security-audit.log

# 重启服务
docker-compose restart backend

# 查看系统资源
top -p $(pgrep -f food-traceability)
df -h
free -m

# 数据库连接
psql -h localhost -U foodtrace_app -d food_traceability

# Redis连接
redis-cli -h localhost -p 6379
```

### B. 关键配置文件位置

| 文件 | 路径 |
|------|------|
| 应用配置 | /opt/food-traceability/application.yml |
| Nginx配置 | /etc/nginx/nginx.conf |
| 数据库配置 | /etc/postgresql/18/main/postgresql.conf |
| 日志配置 | /opt/food-traceability/logback-spring.xml |
| SSL证书 | /etc/nginx/ssl/ |

---

**文档结束**

*本预案应定期更新，确保与实际系统配置保持一致。*
