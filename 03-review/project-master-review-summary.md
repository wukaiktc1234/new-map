# PROJECT-MASTER-REVIEW-001 — Executive Summary

> **文档类型**: Review Summary  
> **审查编号**: PROJECT-MASTER-REVIEW-001  
> **状态**: 待治理启动  
> **生成日期**: 2026-09-09  

---

## 1. 核心事实确认

| 类别 | 数量 | 说明 |
|------|------|------|
| Foundation Objects | 19 | 6 ROOT / 8 VERIFIED / 5 MISSING |
| Master Data Objects | 5 | 核心主数据对象 |
| Capability Domains | 5 | 核心业务能力域 |
| Known Conflicts | 10 | 已识别的系统级冲突 |

### Foundation Object 分布

| 类别 | 数量 | 状态 |
|------|------|------|
| ROOT（根对象） | 6 | 已确认，不可合并 |
| VERIFIED（已验证） | 8 | 已验证归属 |
| MISSING（缺失） | 5 | 未在系统中找到，阻断能力 |

---

## 2. Root Cause 分类

13 个 Root Cause 归并为 **5 大类**：

| # | Root Cause 类别 | 影响范围 | 严重度 |
|---|----------------|---------|--------|
| RC-1 | Identity / Truth Source 冲突 | 全局 — 多处定义同一实体 | **CRITICAL** |
| RC-2 | 数据模型问题 | 核心实体 — 字段、类型、关系不一致 | **HIGH** |
| RC-3 | Foundation 缺失 | 能力层 — 必要对象不存在 | **CRITICAL** |
| RC-4 | Cross-Domain 问题 | 跨域 — food/foods 双写、金额双轨、状态并存 | **HIGH** |
| RC-5 | Legacy 问题 | 遗留系统 — 历史包袱、兼容性 | **MEDIUM** |

---

## 3. 必须做的 Product Decisions

### Foundation 决策

| 决策编号 | 决策主题 | 状态 | 推荐 |
|----------|---------|------|------|
| DEC-001 | Warehouse 对象归属与定义 | 待决策 | — |
| DEC-002 | PaymentMethod 对象归属与定义 | 待决策 | — |
| DEC-003 | Customer 对象归属与定义 | 待决策 | — |
| DEC-004 | Product 主键策略（food vs foods） | 待决策 | — |
| DEC-005 | Order 主键与状态模型统一 | 待决策 | — |

### Data Model 决策

| 决策编号 | 决策主题 | 状态 | 推荐 |
|----------|---------|------|------|
| DEC-006 | 金额单位统一 | 待决策 | **有推荐方案** |
| DEC-007 | 订单状态机统一 | 待决策 | — |
| DEC-008 | 数据模型字段规范 | 待决策 | — |
| DEC-009 | 数据归属边界定义 | 待决策 | — |

---

## 4. 必须做的 Architecture Decisions

### Architecture 决策

| 决策编号 | 决策主题 | 状态 | 推荐 |
|----------|---------|------|------|
| DEC-010 | 跨域数据访问模式 | 待决策 | — |
| DEC-011 | 服务间通信协议统一 | 待决策 | — |
| DEC-012 | 事件驱动架构引入策略 | 待决策 | — |

### Security 决策

| 决策编号 | 决策主题 | 状态 | 推荐 |
|----------|---------|------|------|
| DEC-013 | 裸接口权限分配 | 待决策 | **有推荐方案** |
| DEC-014 | 数据访问控制策略 | 待决策 | — |

---

## 5. 已确认的 Business Requirements

共 **17 个**业务需求：

| 类别 | 数量 | 需求编号范围 |
|------|------|-------------|
| Master Data | 5 | BR-001 ~ BR-005 |
| Data | 5 | BR-006 ~ BR-010 |
| Core | 3 | BR-011 ~ BR-013 |
| Security | 2 | BR-014 ~ BR-015 |
| Cross-Domain | 2 | BR-016 ~ BR-017 |

---

## 6. Foundation 缺失阻断的能力

| 能力域 | 缺失的 Foundation Object | 影响 |
|--------|------------------------|------|
| Inventory Management | **Warehouse** | 无法建立库存位置管理、仓库间调拨、库存盘点 |
| Payment Processing | **PaymentMethod** | 无法支持多支付方式、支付记录、退款流程 |
| Customer Management | **Customer** | 无法建立客户档案、客户分析、会员体系 |

> **结论**: 这 3 个 MISSING Foundation Object 直接阻断了 3 个核心能力域的完整实现。必须优先完成 DEC-001 ~ DEC-003 决策。

---

## 7. 跨域影响分析

| 跨域问题 | 涉及领域 | 影响描述 | 优先级 |
|----------|---------|---------|--------|
| food/foods 双写 | Product / Order / Inventory | 同一实体在两套表中各有一份，数据不同步 | **P0** |
| 金额单位双轨 | 全局 | 部分用分、部分用元，换算不一致 | **P0** |
| 订单状态三套并存 | Order / POS / Management | 三处各自维护状态枚举，映射关系不完整 | **P1** |

---

## 8. 可直接工程化的问题

以下决策已有推荐方案，可直接进入工程实施：

| 决策编号 | 决策主题 | 推荐方案 | 工程化可行性 |
|----------|---------|---------|-------------|
| DEC-006 | 金额单位统一 | 全系统统一为**分（cent）**存储，展示层做格式化 | ✅ 可直接实施 |
| DEC-013 | 裸接口权限分配 | 按角色矩阵分配，无权限接口标记为 PUBLIC/INTERNAL | ✅ 可直接实施 |

---

## 9. 必须先决策的问题

以下决策阻断后续所有工程化工作，必须在 Phase 1 完成：

| 决策编号 | 决策主题 | 阻断原因 |
|----------|---------|---------|
| DEC-001 | Warehouse 对象定义 | 库存管理能力依赖此决策 |
| DEC-002 | PaymentMethod 对象定义 | 支付处理能力依赖此决策 |
| DEC-003 | Customer 对象定义 | 客户管理能力依赖此决策 |
| DEC-004 | Product 主键策略 | food/foods 合并前提 |
| DEC-005 | Order 主键与状态模型 | 订单状态统一前提 |
| DEC-007 | 订单状态机统一 | 三套状态并存必须解决 |

---

## 10. 推荐治理顺序

```
Phase 1: Foundation Stabilization
  ├─ 完成 DEC-001 ~ DEC-005（Foundation 决策）
  ├─ 补全 5 个 MISSING Foundation Objects
  └─ 验证所有 ROOT/VERIFIED 对象归属

Phase 2: Data Integrity
  ├─ 完成 DEC-006 ~ DEC-009（Data Model 决策）
  ├─ 执行金额单位统一（DEC-006 工程化）
  └─ 统一订单状态机（DEC-007）

Phase 3: Cross-Domain Coordination
  ├─ 完成 DEC-010 ~ DEC-012（Architecture 决策）
  ├─ 解决 food/foods 双写
  └─ 建立跨域数据访问模式

Phase 4: Legacy Cleanup
  ├─ 完成 DEC-013 ~ DEC-014（Security 决策）
  ├─ 执行裸接口权限分配（DEC-013 工程化）
  └─ 清理遗留兼容代码
```

### 各阶段依赖关系

```
Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 4
  │              │              │
  │ DEC-006/013  │              │
  │ 可并行工程化  │              │
  └──────────────┘              │
                                │
                    DEC-010~012 依赖 Phase 2 数据完整性
```

---

## 治理启动检查清单

- [ ] 确认 19 个 Foundation Object 归属表
- [ ] 确认 10 个 Conflict 清单
- [ ] 启动 DEC-001 ~ DEC-005 产品决策流程
- [ ] 分配 DEC-006 / DEC-013 工程化任务
- [ ] 建立跨域协调机制
- [ ] 设立治理节奏（周会/双周 Review）

---

> **下一步**: 按 Phase 1 启动 Foundation Stabilization，优先完成 DEC-001 ~ DEC-005 决策。
