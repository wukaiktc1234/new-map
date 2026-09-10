# Architecture Decision Pack — PROJECT-DECISION-RECON-001

> **Status:** ACTIVE  
> **Scope:** 架构层决策  
> **Authority:** 项目架构组

---

## 1. Truth Source 边界

每个业务对象的 Truth Source 已确认，LOCK-001~010 已锁定。

| 对象 | Truth Source | LOCK 编号 | 状态 |
|------|-------------|-----------|------|
| *(待填写)* | *(待填写)* | LOCK-001 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-002 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-003 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-004 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-005 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-006 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-007 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-008 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-009 | 🔒 Locked |
| *(待填写)* | *(待填写)* | LOCK-010 | 🔒 Locked |

**决策:** Truth Source 不可跨域写入，读取必须走 API。

---

## 2. Domain Boundary

系统划分为以下核心域：

| 域 | 职责 | 域标识 |
|----|------|--------|
| Management Core | 管理端核心域，全局配置与核心业务编排 | `DOMAIN_MANAGEMENT_CORE` |
| POS | 收银与交易 | `DOMAIN_POS` |
| Kitchen | 厨房出单与制作流程 | `DOMAIN_KITCHEN` |
| Employee | 员工管理与考勤 | `DOMAIN_EMPLOYEE` |
| MiniProgram | 小程序端用户交互 | `DOMAIN_MINI_PROGRAM` |
| Inventory | 库存与供应链 | `DOMAIN_INVENTORY` |
| Finance | 财务与结算 | `DOMAIN_FINANCE` |

**决策:** 域内自治，跨域交互仅通过 API 或 Event。

---

## 3. Cross-Domain Access

### 3.1 禁止行为

- 禁止直接 DB 跨域读取
- 禁止绕过 API 的隐式数据依赖

### 3.2 强制规范

- 所有跨域访问必须走 API 或 Event
- 调用方需声明所需数据范围，不得全量拉取

### 3.3 统一规则

| 规则 | 说明 |
|------|------|
| READ | 跨域读取 → 调用目标域 API |
| WRITE | 跨域写入 → 发送 Event 由目标域消费 |
| CACHE | 缓存跨域数据时，缓存 TTL ≤ Event 延迟窗口 |

---

## 4. Service Communication

| 通信方式 | 场景 | 技术选型 |
|---------|------|---------|
| 同进程 | 域内模块调用 | Internal Service Call |
| 跨进程同步 | 强一致性请求-响应 | REST API |
| 跨进程异步 | 最终一致性、业务解耦 | RabbitMQ Event |

**决策:**
- 同一 Service 内部模块调用使用 Internal Service Call，无网络开销
- 跨 Service 同步调用统一使用 REST API，超时设置 ≤ 3s
- 跨 Service 异步通信统一使用 RabbitMQ Event，保证至少一次投递

---

## 5. Event Architecture

### 5.1 核心原则

| 原则 | 说明 |
|------|------|
| 业务事实必须 Event | 所有产生业务状态变更的操作必须发布 Event |
| Event 不是 Truth Source | Event 是事实的快照/通知，不作为权威数据源 |
| 消费失败补偿 | Event 消费失败必须有补偿机制（重试 + 死信队列 + 人工介入） |

### 5.2 Event 设计约束

- Event 必须包含: `eventId`, `eventType`, `timestamp`, `payload`
- Event 不可变（Immutability）
- 消费者幂等设计，重复消费不产生副作用

---

## 6. Security Boundary

### 6.1 权限补全

- 所有裸接口必须补全权限校验
- 新增接口必须在权限框架内注册

### 6.2 供应商 H5

- 供应商 H5 入口强制 Token 认证
- Token 有效期 ≤ 24h，支持主动吊销

### 6.3 跨域访问

- 跨域访问强制走 API，禁止通过 DB 直连绕过安全边界
- 跨域 API 调用需携带调用方身份标识

---

## 7. Permission Model

### 7.1 注解驱动

```java
@RequiresPermission("order.create")
public Result createOrder(OrderDTO dto) { ... }
```

### 7.2 数据范围（8 级）

| 级别 | 说明 | 示例 |
|------|------|------|
| 1 | 全系统 | 超级管理员 |
| 2 | 全组织 | 总部管理 |
| 3 | 区域 | 区域经理 |
| 4 | 门店 | 店长 |
| 5 | 部门 | 部门主管 |
| 6 | 班组 | 班组长 |
| 7 | 个人 | 普通员工 |
| 8 | 自定义 | 灵活授权 |

### 7.3 权限模板

- 按角色预置权限模板，新员工入职自动分配
- 权限变更需审计日志记录

---

## 8. Identity Propagation

| 规则 | 说明 |
|------|------|
| Canonical Source | Business Identity 由 Canonical Source 生成 |
| 下游传播 | 下游通过 Event/API 携带 Identity，不得重新生成 |
| 不可变性 | Identity 在整个生命周期内不可变 |

**示例:**
```
Canonical Source: Management Core (订单创建)
  → POS 消费 Event，携带 orderId
  → Kitchen 消费 Event，携带 orderId
  → Inventory 消费 Event，携带 orderId
  → Finance 消费 Event，携带 orderId
```

**决策:** 下游系统收到 Identity 后直接使用，任何"重新生成"行为视为 BUG。

---

> **文档版本:** v1.0  
> **生成时间:** 2026-09-09  
> **维护方:** 架构组
