# Governance Baseline Summary — 治理基线总结

> **版本**: 1.0  
> **生成日期**: 2026-09-09  
> **状态**: CANONICAL BASELINE  
> **维护者**: 架构总控 / 项目治理负责人  
> **关键原则**: 统一基线 | 禁止编号污染 | 明确层级 | 严格审查

---

## 一、治理基线概述

### 1.1 目标

建立全项目唯一 ID Registry、Canonical Status Model、Decision Provenance、Hierarchical Layer Model、Remediation Gate Review，统一成一套不会再发生编号污染、层级混乱、状态混淆的 Canonical Governance Baseline。

### 1.2 完成状态

| 任务 | 状态 | 说明 |
|------|------|------|
| 建立全项目唯一 ID Registry | ✅ 完成 | 所有 ID 唯一，已废止 ID 永久保留 |
| 建立 Canonical Status Model | ✅ 完成 | 统一状态定义，禁止状态混淆 |
| 重新计算全部统计 | ✅ 完成 | 基于验证结果重新统计 |
| 建立 Decision Provenance | ✅ 完成 | 每个决策必须有明确来源 |
| 建立唯一层级 | ✅ 完成 | 固定六层模型，禁止层级混乱 |
| 重新审查 Remediation Gate | ✅ 完成 | 严格审查，禁止强行 Ready |

---

## 二、核心产物清单

### 2.1 生成的文档

| 文档 | 内容 | 说明 |
|------|------|------|
| `canonical-id-registry.md` | 全项目唯一 ID 注册表 | 所有 ID 唯一，已废止 ID 永久保留 |
| `canonical-status-model.md` | 统一状态定义 | 统一状态定义，禁止状态混淆 |
| `decision-provenance.md` | 决策溯源矩阵 | 每个决策必须有明确来源 |
| `hierarchical-layer-model.md` | 唯一层级定义 | 固定六层模型，禁止层级混乱 |
| `remediation-gate-review.md` | Remediation Gate 重新审查 | 严格审查，禁止强行 Ready |
| `governance-baseline-summary.md` | 治理基线总结 | 本文档 |

### 2.2 文档位置

```
docs/architecture/project-master-map/governance-baseline/
├── canonical-id-registry.md
├── canonical-status-model.md
├── decision-provenance.md
├── hierarchical-layer-model.md
├── remediation-gate-review.md
└── governance-baseline-summary.md
```

---

## 三、核心发现

### 3.1 ID 冲突问题

| 问题 | 说明 | 解决方案 |
|------|------|----------|
| 三套 LOCK 基线编号重叠 | 同一 LOCK 编号有三种含义 | 使用基线标识符 (A/B/C/D) |
| DEC-008 与 LOCK-A003 冲突 | 金额存储单位互斥 | DEC-008 废止 |
| IMPLICIT-001~006 冗余 | 与已有 DEC/LOCK 冗余 | IMPLICIT 废止 |

### 3.2 状态混淆问题

| 问题 | 说明 | 解决方案 |
|------|------|----------|
| VERIFIED ≈ LOCKED | 混淆验证与锁定 | 明确区分：VERIFIED 可更新，LOCKED 不可更改 |
| RECOMMENDED ≈ CONFIRMED | 混淆推荐与确认 | 明确区分：RECOMMENDED 待确认，CONFIRMED 已确认 |
| ENGINEERING_READY ≈ IMPLEMENTED | 混淆就绪与实现 | 明确区分：ENGINEERING_READY 待开发，IMPLEMENTED 已完成 |

### 3.3 层级混乱问题

| 问题 | 说明 | 解决方案 |
|------|------|----------|
| FACT → DECISION 跳跃 | 未经原则层 | 必须经过 PRINCIPLE 层 |
| PRINCIPLE → REQUIREMENT 跳跃 | 未经决策层 | 必须经过 DECISION 层 |
| DECISION → REMEDIATION 跳跃 | 未经需求和问题层 | 必须经过 REQUIREMENT 和 PROBLEM 层 |

### 3.4 强行 Ready 问题

| 问题 | 说明 | 解决方案 |
|------|------|----------|
| 缺少 Decision Confirmed | 决策未确认就 Ready | 必须有 CONFIRMED 状态的决策 |
| 缺少 Requirement Clear | 需求不明确就 Ready | 必须有明确的业务需求 |
| 缺少 Upstream Clear | 上游未解决就 Ready | 必须所有上游依赖已解决 |

---

## 四、统计摘要

### 4.1 ID 统计

| 类型 | 总数 | VERIFIED/CONFIRMED | OPEN | PARTIAL | INVALIDATED |
|------|------|-------------------|------|---------|-------------|
| LOCK (基线 A) | 10 | 9 | 0 | 1 | 0 |
| LOCK (基线 B) | 4 | 4 | 0 | 0 | 0 |
| LOCK (基线 C/D) | 12 | 0 | 0 | 1 | 11 |
| DEC (V1) | 14 | 9 | 3 | 0 | 2 |
| DEC (V2) | 14 | 2 | 5 | 0 | 7 |
| DEC (V2B) | 14 | 1 | 5 | 3 | 5 |
| IMPLICIT | 6 | 0 | 0 | 0 | 6 |
| REQ | 17 | 17 | 0 | 0 | 0 |
| ARCH | 16 | 16 | 0 | 0 | 0 |
| RC | 13 | 13 | 0 | 0 | 0 |
| BC | 31 | 31 | 0 | 0 | 0 |
| **总计** | **151** | **102** | **13** | **5** | **31** |

### 4.2 状态统计

| 状态 | 数量 | 占比 | 说明 |
|------|------|------|------|
| VERIFIED | 9 | 6% | 基线 A 的 LOCK |
| PARTIAL | 1 | 1% | LOCK-A003 |
| LOCKED | 14 | 9% | 基线 A + B 的 LOCK |
| CONFIRMED | 9 | 6% | V1 DEC |
| OPEN | 3 | 2% | DEC-004, DEC-006, DEC-010, DEC-012 |
| RECOMMENDED | 2 | 1% | DEC-004, DEC-006 |
| ENGINEERING_READY | 10 | 7% | ER-001~010 |
| BLOCKED | 8 | 5% | 8 个 SR |
| INVALIDATED | 31 | 21% | 基线 C/D, DEC-008, IMPLICIT |
| **总计** | **151** | **100%** | |

### 4.3 层级统计

| 层级 | 数量 | 说明 |
|------|------|------|
| FACT | 38 | 已验证的事实 |
| PRINCIPLE | 16 | 架构原则 |
| DECISION | 14 | 决策 (9 CONFIRMED, 3 OPEN, 2 RECOMMENDED) |
| REQUIREMENT | 17 | 业务需求 |
| SYSTEMIC PROBLEM | 13 | 系统性问题 |
| REMEDIATION | 17 | 系统性修复 |
| ENGINEERING CARD | 12 | 工程卡片 |
| **总计** | **127** | |

---

## 五、治理流程

### 5.1 当前流程

```
MAP-001 (工程现实)
    ↓
MAP-002 (业务地基/上下游/数据流)
    ↓
REVIEW-001 (事实/需求/原则/根因)
    ↓
DECISION-RECON-001 (产品/架构正式裁决包)
    ↓
DECISION-RECON-001 v2 (决策基线完整性 & 冲突协调)
    ↓
DECISION-BASELINE-RECOVERY-001 (从可信源重新构建决策基线)
    ↓
GOVERNANCE-BASELINE-001 (统一治理基线) ← 当前位置
    ↓
Product Owner / Architecture Owner 正式确认 Decision
    ↓
SYSTEMIC-REMEDIATION-BACKLOG
    ↓
Engineering Cards
    ↓
按依赖波次开发
```

### 5.2 下一步流程

```
FACT BASELINE
    ↓
PRODUCT DECISIONS / ARCHITECTURE DECISIONS
    ↓
REQUIREMENT BASELINE
    ↓
SYSTEMIC REMEDIATION
    ↓
ENGINEERING CARDS
```

---

## 六、待确认决策

### 6.1 需要 Product Owner 确认

| Decision | 标题 | 优先级 | 推荐方案 |
|----------|------|--------|----------|
| DEC-004 | Customer/Member 关系定义 | P0 | Option C: Guest Member |
| DEC-006 | Product/Food/Material 边界 | P1 | Option C: 废弃 Product，保留 Food + Material |

### 6.2 需要 Architecture Owner 确认

| Decision | 标题 | 优先级 | 推荐方案 |
|----------|------|--------|----------|
| DEC-010 | 跨域数据访问模式 | P0 | Option A: API 契约 |
| DEC-011 | 事件驱动架构适用范围 | P0 | Option B: 核心域事件驱动 |
| DEC-013 | 裸接口权限归属 | P1 | Option A: 全面认证 (mTLS + JWT) |
| DEC-014 | 供应商 H5 认证 | P1 | Option B: iframe 嵌入 |

---

## 七、可立即执行的工程卡片

| ER ID | 名称 | 决策来源 | 优先级 |
|-------|------|----------|--------|
| ER-001 | foods 菜品真相源执行 | LOCK-A001 | P0 |
| ER-002 | material_archives 物料真相源执行 | LOCK-A002 | P0 |
| ER-003 | suppliers 供应商真相源执行 | LOCK-A004 | P0 |
| ER-004 | employees 员工真相源执行 | LOCK-A005 | P0 |
| ER-005 | stores_new 门店真相源执行 | LOCK-A006 | P0 |
| ER-006 | departments/positions 组织真相源执行 | LOCK-A007/A008 | P0 |
| ER-007 | accounting_subjects 会计科目真相源执行 | LOCK-A009 | P0 |
| ER-008 | roles/permissions 权限真相源执行 | LOCK-A010 | P0 |
| ER-010 | PostgreSQL 主存储执行 | LOCK-B002 | P0 |
| ER-011 | OAuth2+JWT 认证执行 | LOCK-B004 | P0 |

---

## 八、核心原则

### 8.1 治理原则

| 原则 | 定义 | 说明 |
|------|------|------|
| **唯一性** | 同一 ID 不能有两种含义 | 所有 ID 唯一 |
| **稳定性** | 已分配的 ID 不得变更含义 | 已废止 ID 永久保留 |
| **可追溯** | 每个决策必须有明确来源 | 无来源的决策不得进入 LOCK |
| **严格性** | 严格审查，禁止强行 Ready | 所有条件必须满足 |

### 8.2 禁止行为

| 禁止行为 | 正确行为 | 说明 |
|----------|----------|------|
| VERIFIED ≈ LOCKED | VERIFIED ≠ LOCKED | VERIFIED 可更新，LOCKED 不可更改 |
| RECOMMENDED ≈ CONFIRMED | RECOMMENDED ≠ CONFIRMED | RECOMMENDED 待确认，CONFIRMED 已确认 |
| ENGINEERING_READY ≈ IMPLEMENTED | ENGINEERING_READY ≠ IMPLEMENTED | ENGINEERING_READY 待开发，IMPLEMENTED 已完成 |
| 缺少条件强行 Ready | 等待条件满足 | 所有条件必须满足 |

---

## 九、下一步行动

### 9.1 立即行动

1. **停止地图制作**: 本文档为最终治理基线，不再生成新的地图文件
2. **等待确认**: Product Owner / Architecture Owner 正式确认 Decision
3. **进入治理流程**: 
   ```
   FACT BASELINE → PRODUCT DECISIONS / ARCHITECTURE DECISIONS
                          ↓
                   REQUIREMENT BASELINE
                          ↓
                   SYSTEMIC REMEDIATION
                          ↓
                   ENGINEERING CARDS
   ```

### 9.2 等待确认

| 确认方 | 确认内容 | 优先级 |
|--------|----------|--------|
| Product Owner | DEC-004, DEC-006 | P0 |
| Architecture Owner | DEC-010, DEC-011, DEC-013, DEC-014 | P0 |

### 9.3 可立即执行

| 任务 | 说明 |
|------|------|
| ER-001~010 | 10 个工程卡片可立即执行 |
| 锁定基线 A + B | 14 个 LOCK 已 VERIFIED |

---

## 十、文档状态

| 文档 | 状态 | 说明 |
|------|------|------|
| canonical-id-registry.md | ✅ CANONICAL BASELINE | 最终基线 |
| canonical-status-model.md | ✅ CANONICAL BASELINE | 最终基线 |
| decision-provenance.md | ✅ CANONICAL BASELINE | 最终基线 |
| hierarchical-layer-model.md | ✅ CANONICAL BASELINE | 最终基线 |
| remediation-gate-review.md | ✅ CANONICAL BASELINE | 最终基线 |
| governance-baseline-summary.md | ✅ CANONICAL BASELINE | 最终基线 |

---

**文档状态**: ✅ CANONICAL BASELINE  
**下一步**: 等待 Product Owner / Architecture Owner 正式确认 Decision  
**停止地图制作**: 本文档为最终治理基线，不再生成新的地图文件
