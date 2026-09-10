# PROJECT-DECISION-RECON-001 — 产品决策包

> **文档类型**: Product Owner Decision Pack  
> **文档版本**: v1.0  
> **生成日期**: 2026-09-08  
> **目标受众**: Product Owner / 业务决策者

---

## 使用说明

本文档仅包含需要 **Product Owner** 做出决定的事项。每项决策均已评估选项、影响与建议，请直接选择。

决策状态标记：`PENDING` / `DECIDED` / `DEFERRED`

---

## DEC-001: Warehouse 是否需要独立管理

### Decision: Warehouse 是否需要独立管理

**Why**: 当前系统中 Warehouse 与 Facility 的边界不清晰，影响库存管理粒度、仓库运营效率及多仓场景的扩展性。

**Current Reality**: Warehouse 作为 Facility 的子类型存在，无法独立配置独立库存策略、独立出入库规则，所有仓库行为耦合在 Facility 级别。

**Business Problem**: 多仓管理需求下，不同仓库需要独立的库存策略、独立的出入库流程、独立的盘点规则。当前 Facility 级别的统一配置无法满足差异化仓库管理需求，导致业务流程僵化。

**Option A**: config (配置项)
- 将 Warehouse 作为 Facility 的配置属性，通过配置字段控制仓库行为
- 优点：改动最小，保持现有数据模型，无额外表维护成本
- 缺点：无法支撑复杂仓库场景（独立库存、独立出入库规则），配置膨胀后管理困难
- 影响：仅适用于简单仓库管理，未来扩展需要大规模重构

**Option B**: independent_table (独立表)
- 创建独立的 warehouse 表，与 Facility 完全解耦
- 优点：仓库管理完全独立，可扩展性强，支持复杂仓库业务场景
- 缺点：数据模型变更大，Facility-Warehouse 关系需重新设计，迁移成本高
- 影响：全面重构仓库相关模块，影响现有数据关联逻辑

**Option C**: E_hybrid (混合)
- Warehouse 保持与 Facility 的关联关系，但扩展独立的仓库配置表 warehouse_config
- 优点：兼顾简单场景（通过 Facility 配置）和复杂场景（独立配置表），迁移成本可控
- 缺点：需要维护两层配置逻辑，一定程度增加复杂度
- 影响：渐进式支持，现有简单仓库无感知，复杂仓库逐步迁移

**Business Impact**: 仓库管理能力直接影响库存准确性、出入库效率、盘点效率，是仓储运营的核心基础。

**Operational Impact**: 需要逐步制定仓库配置迁移计划，确保现有仓库平稳过渡到新模型。

**Downstream Impact**: 影响库存模块、出入库模块、盘点模块、供应链模块的数据结构和业务逻辑。

**Recommendation**: E_hybrid — 混合方案兼顾当前需求与未来扩展，迁移成本可控。

**Decision Status**: `PENDING`

---

## DEC-004: Customer 是否需要独立于 Member

### Decision: Customer 是否需要独立于 Member

**Why**: 当前 Customer 与 Member 概念混用，无法区分已注册会员与未注册散客，影响会员体系设计、营销策略制定和客户数据管理。

**Current Reality**: Customer 与 Member 使用同一数据模型，所有客户必须注册才能被系统识别，散客消费无法被有效追踪和管理。

**Business Problem**: 业务场景中存在大量散客消费（如餐厅点餐、零售零售），这些消费行为无法被追踪。同时会员营销需要区分新客、散客、会员，当前模型无法支撑差异化运营。

**Option A**: customer_member合一
- 保持 Customer = Member 的现有模型，所有客户都是会员
- 优点：零改动，保持现有逻辑简单
- 缺点：散客无法被追踪，无法支撑散客营销，客户数据不完整
- 影响：散客消费数据丢失，客户画像不完整，营销策略受限

**Option B**: customer独立
- Customer 作为独立实体，Member 继承或关联 Customer
- 优点：Customer 概念独立，散客可被追踪
- 缺点：需要重构客户相关所有模块，改动范围大
- 影响：全面重构客户模块，影响会员、订单、营销等多个模块

**Option C**: E_guest_member (散客类型)
- 在 Member 模型中增加 guest 类型，散客作为特殊 Member 存在
- 优点：保持 Member 为统一入口，通过类型区分散客/会员，改动最小
- 缺点：Customer 和 Member 概念仍然混用，未来扩展可能需要再分离
- 影响：散客可被追踪，会员营销可差异化，迁移成本低

**Business Impact**: 客户识别能力直接影响营销精准度、复购率、客户生命周期价值。

**Operational Impact**: 需要为现有散客消费数据补充 Guest 身份，制定散客注册转化流程。

**Downstream Impact**: 影响会员模块、订单模块、营销模块、数据分析模块的客户标识逻辑。

**Recommendation**: E_guest_member — 散客类型方案改动最小，快速解决散客追踪问题，为未来独立 Customer 预留扩展空间。

**Decision Status**: `PENDING`

---

## DEC-005: Product/Food/Material 边界

### Decision: Product/Food/Material 边界

**Why**: 当前系统中 Product、Food、Material 三个概念边界模糊，导致商品管理混乱、数据冗余、业务逻辑分散。

**Current Reality**: Product 作为通用商品概念存在，Food 和 Material 分别表示食品和原材料，但三者之间存在大量重叠字段和业务逻辑，维护成本高且容易出现数据不一致。

**Business Problem**: 餐饮业务中，菜品（Food）、原材料（Material）、可售商品（Product）三者的管理需求不同但数据模型重叠。菜品需要配方管理，原材料需要采购管理，可售商品需要定价和库存管理。当前混合模型导致任何一个场景的变更都可能影响其他场景。

**Option A**: keep双Master
- 保持 Product 和 Food 的双主数据模式，各自维护独立数据
- 优点：保持现有逻辑不变，各业务线独立管理
- 缺点：数据冗余严重，同一商品可能在两个系统中重复维护，一致性难以保证
- 影响：持续的维护成本，数据不一致风险长期存在

**Option B**: merge
- 将 Food 和 Material 统一合并到 Product 中，通过字段区分类型
- 优点：统一数据模型，消除冗余
- 缺点：Product 模型变得臃肿，不同类型的业务逻辑耦合在一起
- 影响：Product 模型复杂度大幅增加，不同类型商品的差异化需求难以满足

**Option C**: C_deprecate_product + KEEP双Master
- 废弃通用 Product 概念，保留 Food（可售菜品）和 Material（原材料）作为两个独立主数据
- 优点：概念清晰，Food 专注可售菜品管理，Material 专注原材料采购管理，业务逻辑解耦
- 缺点：需要重新梳理现有 Product 数据的归属，可能需要数据迁移
- 影响：餐饮场景下概念清晰，但非餐饮场景（如零售）需要额外定义商品模型

**Business Impact**: 商品管理清晰度直接影响菜品管理效率、原材料采购效率、库存准确性。

**Operational Impact**: 需要梳理现有 Product 数据，将可售商品归入 Food，原材料归入 Material，制定数据迁移计划。

**Downstream Impact**: 影响菜品模块、采购模块、库存模块、订单模块的商品标识和数据结构。

**Recommendation**: C_deprecate_product + KEEP双Master — 餐饮场景下 Food 和 Material 概念清晰，废弃冗余的 Product 概念，减少维护成本。非餐饮场景可按需扩展。

**Decision Status**: `PENDING`

---

## 决策汇总

| 编号 | 决策项 | 推荐方案 | 状态 |
|------|--------|----------|------|
| DEC-001 | Warehouse 独立管理 | E_hybrid (混合) | `PENDING` |
| DEC-004 | Customer 与 Member 关系 | E_guest_member (散客类型) | `PENDING` |
| DEC-005 | Product/Food/Material 边界 | C_deprecate_product + KEEP双Master | `PENDING` |

---

> **下一步**: 请 Product Owner 对以上决策项逐一确认或调整，标记决策状态为 `DECIDED`。
