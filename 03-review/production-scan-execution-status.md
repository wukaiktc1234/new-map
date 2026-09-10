# Production Scan Execution Status

> - 文档编号：W1-PROD-SCAN-EXEC-001
> - 日期：2026-09-09
> - 角色：架构总控（会话1）

---

## 状态：PRODUCTION_SCAN_BLOCKED_EXTERNAL_DEPENDENCY

### 原因
Agent（架构总控）运行在开发环境（P:\my-new-project），**无法直接连接生产 PostgreSQL 数据库**。生产扫描必须由 DBA/运维在真实生产环境执行。

### 缺什么权限/条件

| 项目 | 说明 |
|---|---|
| 生产 PostgreSQL 连接 | 需 DBA/运维提供（不可暴露敏感连接信息） |
| SELECT only 权限 | 生产 DB 只读账户 |
| DBA/运维执行人 | 需人工执行 SQL 脚本并保存原始输出 |
| 网络/VPN | 生产环境网络可达 |

### 需要谁执行
DBA 或运维工程师，在生产 PostgreSQL 上执行 5 个只读 SQL 脚本。

### 脚本版本
- `docs/quality/01-orders-production-scan.sql`
- `docs/quality/02-legacy-consumer-classification.sql`
- `docs/quality/03-finance-production-scan.sql`
- `docs/quality/04-amount-unit-sampling.sql`
- `docs/quality/05-legacy-consumer-production-confirm.sql`
- `docs/quality/README.md`（执行指南）

### 输出要求
DBA/运维执行后，将原始 SQL 输出（文本）回传给架构总控。架构总控负责：
1. 逐项分析原始输出
2. Risk Classification（BLOCKER/PRE-GRAY/MONITOR/POST-FREEZE/DEAD CODE/EXEMPTION）
3. Gray Readiness Decision

### 预计下一步动作
1. DBA/运维执行 5 个脚本 → 保存原始输出
2. 回传架构总控
3. 架构总控整理为 Production Data Snapshot + Risk Classification + Gray Readiness Decision
4. 产品负责人最终确认 → 启动 Gray Release

### 禁止
- ❌ 在开发环境模拟/伪造生产结果
- ❌ 使用开发环境 orders_legacy=0 推断生产环境
- ❌ 在未获得真实生产数据前做 Gray Readiness 判定

---

*等待 DBA/运维生产执行。*
*文档生成：架构总控（会话1）· 2026-09-09*
