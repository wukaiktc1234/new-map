# System Truth Source Map（真相源图）

> - 从 `system-fact-map-v1.md` §1.1 + PADR §九 + CDDR §三 提取。
> - 标注每个业务对象的真相源、语义裁决、读写责任域、遗留/待迁移项。

---

## 一、已裁决真相源（PADR §九 + CDDR §三 确认）

| 对象 | 语义真相 | 数据真相源 | 读写责任域 | 遗留/待迁移 | 状态 |
|---|---|---|---|---|---|
| 菜品/菜单 | **foods**（销售菜品主档） | `foods`（status=1=集团发布） | 产品域写 / 订单域读（POS 可售） | food 旧表（POS 扫码 G1） | VERIFIED |
| 物料 | **material_archives**（全局采购物料） | `material_archives`（material_id） | 供应链域写 / 库存·配方读 | product 表（只读遗留） | VERIFIED |
| 库存 | **物料×位置**（库存事实对象） | `inventory`（仓）/`store_inventory`（店） | 库存域写（采购/收货/订单消耗） | product_id 列（ETM-106 收敛） | VERIFIED |
| 批次 | **material_trace_code.batch_no**（追溯码层） | `material_trace_code` | 库存/追溯域 | inventory.batch_no=属性 | VERIFIED |
| 订单 | **orders**（新核心） | `orders`（分，BIGINT） | 订单域写 / POS·管理端读 | orders_legacy（POS 历史 PD-015） | VERIFIED |
| 订单状态 | **单一业务语义（按层拆分）** | orders.order_status（0-6）+ payment_status | 订单域 | sales_order String（无映射 PD-017） | VERIFIED |
| 财务 | **finance_vouchers**（新凭证体系）+ payables/payment/receipt/fund_flows/finance_records | — | 财务域写 | voucher_header（死体系）/根包 stub（假链路） | VERIFIED |
| 成本 | **分口径（最新入库价法）** | cost_records + inventory/store_inventory.unit_cost | 库存/订单写 / 财务读 | 双写待确认 | VERIFIED（口径） |
| 金额 | **新写一律分** | 各业务表（分） | 各域 | tax_record（元）/account_balance（元）待统一 | VERIFIED（方向） |

## 二、双口径真相源（CONFLICT——需裁决/已裁决）

| 对象 | 口径 A（真相源） | 口径 B（遗留） | 裁决 | 状态 |
|---|---|---|---|---|
| ID：product_id vs material_id | material_id（真相语义） | product_id（遗留列名/别名穿透） | 保留不删，语义收敛（ETM-106） | CONFLICT（已裁决处置方式） |
| 金额：分 vs 元 | 分（新写一律分） | 元（legacy/tax_record/account_balance） | PD-031/032 已裁决部分；批 C 统一 | CONFLICT（处置进行中） |
| 科目余额 | accounting_subjects.balance（分/自动） | account_balance（元/人工） | PD-031：以 accounting_subjects 为准；account_balance 冻结停用 | CONFLICT（已裁决） |
| 凭证体系 | finance_vouchers（新/活跃） | voucher_header（旧/死体系） | 冻结停用（不删除） | CONFLICT（已裁决处置） |
| 门店库存 | store_inventory（门店维度） | inventory（仓库维度） | 同概念双维度，按位置类型分写 | VERIFIED（双维度成立） |
| POS 菜单 | foods（新表，on-sale） | food（旧表，/v1/pos/api/*） | G1 POS 双源统一（Batch0-方案1） | CONFLICT（技术整改） |

## 三、不存在的真相源（C 不存在 / 演进）

| 对象 | 现状 | PADR 裁决 | 状态 |
|---|---|---|---|
| 门店经营范围 | 全局统一菜单，无门店维度 | PADR-002=正式概念，演进实现 | 不存在（演进 PADR-002） |
| 门店销售状态 | 与发布/售罄混叠在 foods.status | D3 演进 | 不存在（演进 D3） |
| 区域 | 无区域层概念 | D6 方向已定，大型连锁启用 | 不存在（EVOLUTION） |
| 总账 | 无会计总账表 | Level 3 增强层（PADR-005） | 不存在（Level 3 演进） |
| 统一单位主数据 | 无 units 表；unit 为各表 VARCHAR 列 | PD 候选（V2 §1.A） | 不存在（待确认） |
| 客户主数据 | 无 customers 表；客户以列内嵌 | PD 候选（V2 §1.A） | 不存在（待确认） |

## 四、真相源归属总览（按责任域）

| 域 | 负责写入的真相源 | 不负责 |
|---|---|---|
| **产品域** | foods / dish_combos / dish_recipes / food_categories | 库存事实（inventory/store_inventory）；门店可售/经营范围（PADR-003） |
| **库存域** | inventory / store_inventory / 单据（调拨/盘点/调整/报损/出库） | POS 可售判定（可售=菜品层）；门店经营范围（PADR-003 原则 5） |
| **订单域** | orders / order_items / order_payment_records | 凭证生成（销售凭证未接线 PADR-005） |
| **财务域** | finance_vouchers / payables / payment / receipt / fund_flows / finance_records / cost_records | Level 3（凭证级/总账/法定报表 PADR-005） |
| **供应链域** | material_archives / purchase_* / receipt_confirmations | — |

---

*本真相源图为 Fact Map §1.1 + §3 的聚焦提取，与 `cross-domain-architecture-review.md` §三 同源。*
*文档生成：架构总控（会话1）· 2026-09-08*
