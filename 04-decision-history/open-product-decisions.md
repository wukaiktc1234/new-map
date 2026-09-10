> **DOCUMENT STATUS:** ACTIVE_REFERENCE
> **SOURCE TASK:** DECISION-RECON-001

# Open Product Decisions

> **文档类型**: 产品决策清单  
> **文档版本**: v1.0  
> **生成日期**: 2026-09-09  
> **状态**: OPEN  
> **目标受众**: Product Owner / 业务决策者

---

## 决策概览

| 决策ID | 决策标题 | 优先级 | 截止日期 | 负责人 |
|--------|----------|--------|----------|--------|
| DEC-004 | Customer/Member 关系定义 | P0 | 2026-09-30 | Product Owner |
| DEC-006 | Product/Food/Material 边界 | P1 | 2026-10-15 | Product Owner |

---

## DEC-004: Customer/Member 关系定义

| 字段 | 值 |
|------|-----|
| **决策ID** | DEC-004 |
| **决策标题** | Customer/Member 关系定义 |
| **状态** | 🟡 OPEN |
| **优先级** | P0 |
| **负责人** | Product Owner |
| **截止日期** | 2026-09-30 |
| **阻塞影响** | 阻塞用户域积分、等级功能 |

---

### 1. 问题描述

当前系统中 Customer（客户）与 Member（会员）概念混用，无法区分已注册会员与未注册散客。业务场景中存在大量散客消费（如餐厅点餐、零售零售），这些消费行为无法被有效追踪和管理。同时会员营销需要区分新客、散客、会员，当前模型无法支撑差异化运营策略。

---

### 2. 当前现实

- Customer 与 Member 使用同一数据模型，所有客户必须注册才能被系统识别
- 散客消费无法被追踪，导致客户画像不完整
- 会员表已建立，但等级升降规则未定
- 多个 Story 各自假设 Customer/Member 关系，尚未统一

**数据模型现状**:
```sql
-- 当前 Customer/Member 混合模型
CREATE TABLE member (
    id BIGINT PRIMARY KEY,
    phone VARCHAR(20) UNIQUE,
    name VARCHAR(100),
    level VARCHAR(20),  -- 会员等级
    points INT DEFAULT 0,  -- 积分
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

### 3. 业务需求

| 需求 | 描述 | 优先级 |
|------|------|--------|
| 散客追踪 | 未注册客户消费行为可被记录和分析 | P0 |
| 会员营销 | 区分新客、散客、会员进行差异化营销 | P0 |
| 客户画像 | 完整的客户消费行为数据，支撑精准营销 | P1 |
| 积分体系 | 会员积分累积、兑换、有效期管理 | P0 |
| 等级体系 | 会员等级升降规则，支撑会员权益差异化 | P0 |

---

### 4. 选项分析

#### Option A: Customer = Member（保持现状）

| 维度 | 评估 |
|------|------|
| **描述** | 保持 Customer = Member 的现有模型，所有客户都是会员 |
| **优点** | 零改动，保持现有逻辑简单 |
| **缺点** | 散客无法被追踪，无法支撑散客营销，客户数据不完整 |
| **影响** | 散客消费数据丢失，客户画像不完整，营销策略受限 |
| **迁移成本** | 零 |
| **风险** | 高 — 无法支撑业务增长需求 |

#### Option B: Customer 独立

| 维度 | 评估 |
|------|------|
| **描述** | Customer 作为独立实体，Member 继承或关联 Customer |
| **优点** | Customer 概念独立，散客可被追踪，扩展性强 |
| **缺点** | 需要重构客户相关所有模块，改动范围大 |
| **影响** | 全面重构客户模块，影响会员、订单、营销等多个模块 |
| **迁移成本** | 高 — 预估 80-120 人天 |
| **风险** | 中 — 迁移期间数据一致性风险 |

#### Option C: Guest Member（散客类型）

| 维度 | 评估 |
|------|------|
| **描述** | 在 Member 模型中增加 guest 类型，散客作为特殊 Member 存在 |
| **优点** | 保持 Member 为统一入口，通过类型区分散客/会员，改动最小 |
| **缺点** | Customer 和 Member 概念仍然混用，未来扩展可能需要再分离 |
| **影响** | 散客可被追踪，会员营销可差异化，迁移成本低 |
| **迁移成本** | 低 — 预估 15-25 人天 |
| **风险** | 低 — 渐进式改造，可逐步验证 |

---

### 5. 推荐方案

**推荐方案**: Option C — Guest Member（散客类型）

**推荐理由**:
1. **改动最小**: 保持现有 Member 模型，仅增加 guest 类型字段
2. **快速见效**: 散客消费可立即被追踪，支撑营销分析
3. **风险可控**: 渐进式改造，现有业务无感知
4. **扩展预留**: 为未来独立 Customer 预留扩展空间

**实施要点**:
```sql
-- 会员表增加类型字段
ALTER TABLE member ADD COLUMN member_type VARCHAR(20) DEFAULT 'guest';
-- member_type: 'guest' | 'member' | 'vip'

-- 散客消费记录表
CREATE TABLE guest_transaction (
    id BIGINT PRIMARY KEY,
    member_id BIGINT,  -- 关联 member 表，guest 也有记录
    order_id BIGINT,
    amount DECIMAL(10,2),
    created_at TIMESTAMP
);
```

---

### 6. 依赖关系

| 依赖类型 | 依赖项 | 说明 |
|----------|--------|------|
| ⚡ 强依赖 | DEC-006 (Product/Food/Material) | 需联合确认商品与客户的关系 |
| ⚡ 强依赖 | 会员等级体系设计 | 等级升降规则需在此决策后确定 |
| 🔗 影响 | 订单模块 | 订单需关联 guest 类型会员 |
| 🔗 影响 | 营销模块 | 营销策略需区分 guest/member |
| 🔗 影响 | 数据分析模块 | 客户画像需包含散客数据 |

---

### 7. 影响范围

| 影响域 | 影响模块 | 影响程度 |
|--------|----------|----------|
| 用户域 | 会员模块 | 高 — 数据模型变更 |
| 订单域 | 订单模块 | 中 — 客户标识逻辑调整 |
| 营销域 | 营销模块 | 中 — 营销策略差异化 |
| 数据域 | 数据分析 | 中 — 客户画像扩展 |
| 基础设施 | 数据库 | 低 — 仅增加字段 |

---

### 8. 证据状态

| 证据类型 | 状态 | 说明 |
|----------|------|------|
| 业务需求 | ✅ CONFIRMED | 散客追踪需求已确认 |
| 技术可行性 | ✅ CONFIRMED | Guest Member 方案已验证 |
| 数据迁移 | 🔄 IN PROGRESS | 散客数据清洗方案制定中 |
| 性能影响 | ⏳ PENDING | 需评估散客表增长对查询性能的影响 |

---

### 9. 决策记录

| 版本 | 日期 | 决策者 | 决策内容 |
|------|------|--------|----------|
| v1.0 | 2026-09-09 | 架构委员会 | 初始提案，标记为 OPEN |

---

## DEC-006: Product/Food/Material 边界

| 字段 | 值 |
|------|-----|
| **决策ID** | DEC-006 |
| **决策标题** | Product/Food/Material 边界 |
| **状态** | 🟡 OPEN |
| **优先级** | P1 |
| **负责人** | Product Owner |
| **截止日期** | 2026-10-15 |
| **阻塞影响** | 影响多态数据建模，需 PO 与架构共同确认 |

---

### 1. 问题描述

当前系统中 Product（通用商品）、Food（食品/菜品）、Material（原材料）三个概念边界模糊，导致商品管理混乱、数据冗余、业务逻辑分散。餐饮业务中，菜品、原材料、可售商品三者的管理需求不同但数据模型重叠，维护成本高且容易出现数据不一致。

**需修订原因**: 原 DEC-006（服务网格技术选型）与 LOCK-001（事件驱动架构）存在冲突，需重新评估以匹配 LOCK-001。当前 Product/Food/Material 边界决策需考虑事件驱动架构下的数据建模约束。

---

### 2. 当前现实

- Product 作为通用商品概念存在，Food 和 Material 分别表示食品和原材料
- 三者之间存在大量重叠字段和业务逻辑
- 部分模块使用 Product，部分使用 Food/Material，数据模型不统一
- 同一商品可能在两个系统中重复维护

**数据模型现状**:
```sql
-- Product 通用商品表
CREATE TABLE product (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200),
    category VARCHAR(50),
    price DECIMAL(10,2),
    unit VARCHAR(20),
    status VARCHAR(20)
);

-- Food 菜品表（与 Product 字段重叠）
CREATE TABLE food (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200),
    category VARCHAR(50),
    price DECIMAL(10,2),
    unit VARCHAR(20),
    recipe JSONB,  -- 配方信息
    status VARCHAR(20)
);

-- Material 原材料表（与 Product 字段重叠）
CREATE TABLE material (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200),
    category VARCHAR(50),
    cost DECIMAL(10,2),
    unit VARCHAR(20),
    supplier_id BIGINT,
    status VARCHAR(20)
);
```

---

### 3. 业务需求

| 需求 | 描述 | 优先级 |
|------|------|--------|
| 概念清晰 | Food 专注可售菜品管理，Material 专注原材料采购管理 | P0 |
| 数据统一 | 消除同一商品在多个系统中的重复维护 | P0 |
| 业务解耦 | 不同类型商品的差异化需求可独立满足 | P1 |
| 扩展性 | 非餐饮场景（如零售）可按需扩展商品模型 | P1 |
| 事件兼容 | 数据模型需兼容 LOCK-001 事件驱动架构 | P0 |

---

### 4. 选项分析

#### Option A: 保持双 Master

| 维度 | 评估 |
|------|------|
| **描述** | 保持 Product 和 Food 的双主数据模式，各自维护独立数据 |
| **优点** | 保持现有逻辑不变，各业务线独立管理 |
| **缺点** | 数据冗余严重，同一商品可能在两个系统中重复维护，一致性难以保证 |
| **影响** | 持续的维护成本，数据不一致风险长期存在 |
| **迁移成本** | 零 |
| **风险** | 高 — 数据不一致风险持续存在 |
| **事件兼容** | ⚠️ 中 — 需为每个实体定义独立事件 |

#### Option B: 合并到 Product

| 维度 | 评估 |
|------|------|
| **描述** | 将 Food 和 Material 统一合并到 Product 中，通过字段区分类型 |
| **优点** | 统一数据模型，消除冗余 |
| **缺点** | Product 模型变得臃肿，不同类型的业务逻辑耦合在一起 |
| **影响** | Product 模型复杂度大幅增加，不同类型商品的差异化需求难以满足 |
| **迁移成本** | 中 — 预估 40-60 人天 |
| **风险** | 中 — 模型复杂度增加，维护成本上升 |
| **事件兼容** | ⚠️ 中 — 单一实体事件，但事件结构复杂 |

#### Option C: 废弃 Product，保留 Food + Material

| 维度 | 评估 |
|------|------|
| **描述** | 废弃通用 Product 概念，保留 Food（可售菜品）和 Material（原材料）作为两个独立主数据 |
| **优点** | 概念清晰，Food 专注可售菜品管理，Material 专注原材料采购管理，业务逻辑解耦 |
| **缺点** | 需要重新梳理现有 Product 数据的归属，可能需要数据迁移 |
| **影响** | 餐饮场景下概念清晰，但非餐饮场景（如零售）需要额外定义商品模型 |
| **迁移成本** | 中 — 预估 30-50 人天 |
| **风险** | 低 — 概念清晰，维护成本降低 |
| **事件兼容** | ✅ 高 — Food/Material 各自定义事件，结构清晰 |

---

### 5. 推荐方案

**推荐方案**: Option C — 废弃 Product，保留 Food + Material

**推荐理由**:
1. **概念清晰**: Food 专注可售菜品，Material 专注原材料，业务逻辑解耦
2. **事件驱动友好**: 每个实体可独立定义事件，符合 LOCK-001 架构约束
3. **扩展性**: 非餐饮场景可按需扩展新的商品类型（如 Retail Product）
4. **维护成本降低**: 消除数据冗余，减少一致性维护成本

**实施要点**:
```sql
-- 保留 Food 表（增加必要字段）
ALTER TABLE food ADD COLUMN sku VARCHAR(100) UNIQUE;
ALTER TABLE food ADD COLUMN barcode VARCHAR(100);
ALTER TABLE food ADD COLUMN shelf_life INT;

-- 保留 Material 表（增加必要字段）
ALTER TABLE material ADD COLUMN sku VARCHAR(100) UNIQUE;
ALTER TABLE material ADD COLUMN barcode VARCHAR(100);
ALTER TABLE material ADD COLUMN min_stock INT;

-- 废弃 Product 表（保留历史数据）
ALTER TABLE product RENAME TO product_archived;

-- 数据迁移脚本
-- 将 Product 中的可售商品迁移到 Food
-- 将 Product 中的原材料迁移到 Material
```

**事件定义示例**:
```json
// Food 事件
{
  "eventType": "food.created",
  "payload": {
    "foodId": "F001",
    "name": "宫保鸡丁",
    "price": 38.00,
    "recipe": {...}
  }
}

// Material 事件
{
  "eventType": "material.purchased",
  "payload": {
    "materialId": "M001",
    "name": "鸡肉",
    "quantity": 100,
    "unit": "kg",
    "supplierId": "S001"
  }
}
```

---

### 6. 依赖关系

| 依赖类型 | 依赖项 | 说明 |
|----------|--------|------|
| ⚡ 强依赖 | LOCK-001 (事件驱动架构) | 数据模型需兼容事件驱动架构 |
| ⚡ 强依赖 | DEC-004 (Customer/Member) | 需联合确认商品与客户的关系 |
| 🔗 影响 | 订单模块 | 订单需支持 Food/Material 类型商品 |
| 🔗 影响 | 库存模块 | 库存需区分 Food/Material 类型 |
| 🔗 影响 | 采购模块 | 采购模块仅关联 Material |
| 🔗 影响 | 菜品模块 | 菜品模块仅关联 Food |

---

### 7. 影响范围

| 影响域 | 影响模块 | 影响程度 |
|--------|----------|----------|
| 商品域 | 商品模块 | 高 — 数据模型重构 |
| 订单域 | 订单模块 | 中 — 商品类型适配 |
| 库存域 | 库存模块 | 中 — 库存类型区分 |
| 采购域 | 采购模块 | 低 — 仅关联 Material |
| 菜品域 | 菜品模块 | 低 — 仅关联 Food |
| 数据域 | 数据分析 | 中 — 商品数据迁移 |
| 基础设施 | 数据库 | 中 — 表结构变更 + 数据迁移 |

---

### 8. 证据状态

| 证据类型 | 状态 | 说明 |
|----------|------|------|
| 业务需求 | ✅ CONFIRMED | Food/Material 概念清晰需求已确认 |
| 技术可行性 | ✅ CONFIRMED | Food + Material 独立模型已验证 |
| 事件驱动兼容 | ✅ CONFIRMED | 符合 LOCK-001 架构约束 |
| 数据迁移 | 🔄 IN PROGRESS | 数据清洗方案制定中 |
| 性能影响 | ⏳ PENDING | 需评估拆分后查询性能影响 |

---

### 9. 决策记录

| 版本 | 日期 | 决策者 | 决策内容 |
|------|------|--------|----------|
| v1.0 | 2026-09-08 | 架构委员会 | 初始提案，标记为 OPEN |
| v1.1 | 2026-09-09 | 架构委员会 | 标记需修订以匹配 LOCK-001 |

---

## 决策汇总

| 决策ID | 决策标题 | 推荐方案 | 状态 | 截止日期 |
|--------|----------|----------|------|----------|
| DEC-004 | Customer/Member 关系定义 | Guest Member（散客类型） | 🟡 OPEN | 2026-09-30 |
| DEC-006 | Product/Food/Material 边界 | 废弃 Product，保留 Food + Material | 🟡 OPEN | 2026-10-15 |

---

## 下一步行动

| 行动 | 负责人 | 截止时间 | 优先级 |
|------|--------|----------|--------|
| 确认 DEC-004 Guest Member 方案 | Product Owner | 2026-09-30 | P0 |
| 确认 DEC-006 Food + Material 方案 | Product Owner | 2026-10-15 | P1 |
| 制定数据迁移计划 | 技术团队 | 2026-10-01 | P1 |
| 评估事件驱动架构兼容性 | 架构团队 | 2026-09-25 | P0 |

---

## 相关文档

- [Decision Baseline v2](../decision-recon/v2/decision-baseline-v2.md)
- [Decision Recon Summary](../decision-recon/v2/decision-recon-v2-summary.md)
- [Product Decision Pack](../decision-recon/product-decision-pack.md)

---

**文档维护**: 此文档由 Product Owner 维护，每月至少审查一次，重大决策变更时即时更新。
