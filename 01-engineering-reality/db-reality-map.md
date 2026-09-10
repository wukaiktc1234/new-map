# 数据库现实地图 (Database Reality Map)

> **项目**: 食品溯源系统 (Food Traceability System)  
> **生成时间**: 2026-09-09  
> **数据库引擎**: PostgreSQL 18 (生产), H2 (开发)  
> **迁移文件数**: 160 Flyway迁移文件  
> **表数量**: 100+ 张表  

---

## 1. 权限与用户体系 (8张表)

| 表名 | 用途 | 备注 |
|------|------|------|
| `users` | 用户主表 | |
| `roles` | 角色定义 | |
| `permissions` | 权限定义 | |
| `user_roles` | 用户-角色关联 | 多对多 |
| `role_permissions` | 角色-权限关联 | 多对多 |
| `user_permissions` | 用户直接权限 | 旁路授权 |
| `permission_templates` | 权限模板 | |
| `user_permission_overrides` | 用户权限覆盖 | 例外处理 |

**关系**: 用户 → (user_roles) → 角色 → (role_permissions) → 权限

---

## 2. 组织架构 (4张表)

| 表名 | 用途 | 备注 |
|------|------|------|
| `departments` | 部门信息 | |
| `positions` | 职位定义 | |
| `position_levels` | 职级体系 | |
| `stores_new` | 门店信息 | `stores`已废弃 |

---

## 3. HR人力资源 (7张表)

| 表名 | 用途 | 备注 |
|------|------|------|
| `employees` | 员工主表 | |
| `attendance_record` | 考勤记录 | |
| `salary_records` | 薪资记录 | |
| `onboarding_archive` | 入职档案 | |
| `approval_record` | 审批记录 | |
| `invitation_send_record` | 邀请发送记录 | |

---

## 4. 产品与菜品体系 (10+张表)

| 表名 | 用途 | 冗余问题 |
|------|------|----------|
| `food` | 食材(旧) | ⚠️ 与 `foods` 重复 |
| `foods` | 食材(新) | ⚠️ 与 `food` 重复 |
| `food_category` | 食材分类(旧) | ⚠️ 与 `food_categories` 重复 |
| `food_categories` | 食材分类(新) | ⚠️ 与 `food_category` 重复 |
| `product` | 产品主表 | |
| `dish_combo` | 菜品组合(旧) | ⚠️ 与 `dish_combos` 重复 |
| `dish_combos` | 菜品组合(新) | ⚠️ 与 `dish_combo` 重复 |
| `combo_ingredients` | 组合菜品配料 | |
| `dish_recipe` | 菜品配方(旧) | ⚠️ 与 `dish_recipes` 重复 |
| `dish_recipes` | 菜品配方(新) | ⚠️ 与 `dish_recipe` 重复 |

**⚠️ 重复表问题**: `food`/`foods`, `food_category`/`food_categories`, `dish_combo`/`dish_combos`, `dish_recipe`/`dish_recipes` 均存在新旧版本并存问题。

---

## 5. 库存体系 (10+张表)

| 表名 | 用途 |
|------|------|
| `inventory` | 库存主表 |
| `inventory_log` | 库存日志 |
| `inventory_loss` | 库存损耗 |
| `inventory_transfer` | 库存调拨 |
| `inventory_check` | 库存盘点 |
| `inventory_transactions` | 库存事务 |
| `inventory_locations` | 库存仓位 |
| `store_inventory` | 门店库存 |

---

## 6. 采购体系 (12+张表)

| 表名 | 用途 |
|------|------|
| `suppliers` | 供应商 |
| `purchase_request` | 采购申请 |
| `purchase_orders` | 采购订单 |
| `purchase_stockins` | 采购入库 |
| `purchase_returns` | 采购退货 |
| `purchase_arrivals` | 采购到货 |
| `material_archives` | 物料档案 |
| `material_categories` | 物料分类 |

**流程**: 采购申请 → 采购订单 → 到货 → 入库 / 退货

---

## 7. 财务体系 (15+张表)

| 表名 | 用途 | 冗余问题 |
|------|------|----------|
| `finance_record` | 财务记录(旧) | ⚠️ 与 `finance_records` 重复 |
| `finance_records` | 财务记录(新) | ⚠️ 与 `finance_record` 重复 |
| `payment` | 付款 | |
| `receipt` | 收款 | |
| `payables` | 应付账款 | |
| `receivables` | 应收账款 | |
| `accounting_subject` | 会计科目 | |
| `voucher_header` | 凭证头 | |
| `voucher_line` | 凭证行 | |
| `account_balance` | 科目余额 | |
| `cost_record` | 成本记录 | |
| `tax_record` | 税务记录 | |
| `bank_accounts` | 银行账户 | |

**⚠️ 重复表问题**: `finance_record`/`finance_records` 新旧版本并存。

---

## 8. 订单与销售 (8+张表)

| 表名 | 用途 | 冗余问题 |
|------|------|----------|
| `orders` | 订单(V6) | ⚠️ V1版本已废弃但可能残留 |
| `order_items` | 订单明细 | |
| `order_payment_records` | 订单支付记录 | |
| `orders_legacy` | 旧版订单归档 | |
| `dining_tables` | 餐桌 | |
| `table_reservations` | 桌位预约 | |
| `call_number_queues` | 叫号队列 | |

**⚠️ 版本冲突**: `orders` 在V1.0.0.100和V6.0.0中均存在定义，V6覆盖了V1同名表。

---

## 9. 营销CRM (12张表)

| 表名 | 用途 |
|------|------|
| `members` | 会员主表 |
| `member_level` | 会员等级 |
| `member_points_log` | 积分流水 |
| `member_coupon` | 会员优惠券 |
| `coupon_template` | 优惠券模板 |
| `marketing_promotion` | 营销活动 |
| `recharge_plan` | 充值方案 |
| `recharge_record` | 充值记录 |

**流程**: 会员 → 等级 → 积分/优惠券/充值

---

## 10. 食品溯源体系 (10+张表)

| 表名 | 用途 |
|------|------|
| `material_trace_code` | 原材料溯源码 |
| `food_trace_code` | 成品溯源码 |
| `material_consumption` | 原材料消耗 |
| `food_inspection` | 食品检验 |
| `food_quality_standard` | 质量标准 |
| `trace_scan_record` | 溯源扫码记录 |

**溯源链路**: 原材料溯源码 → 原材料消耗 → 成品溯源码 → 扫码记录

---

## 11. 设备管理 (4张表)

| 表名 | 用途 | 冗余问题 |
|------|------|----------|
| `devices` | 设备主表 | |
| `device_status_log` | 设备状态日志(旧) | ⚠️ 与 `device_status_logs` 重复 |
| `device_status_logs` | 设备状态日志(新) | ⚠️ 与 `device_status_log` 重复 |
| `device_templates` | 设备模板 | |

---

## 12. 资产管理 (6张表)

| 表名 | 用途 |
|------|------|
| `asset_categories` | 资产分类 |
| `asset_masters_enhanced` | 资产主数据 |
| `asset_flow_records` | 资产流转记录 |
| `asset_depreciation_records` | 资产折旧记录 |

---

## 13. 重复表问题汇总

| 旧表 | 新表 | 建议 |
|------|------|------|
| `food` | `foods` | 迁移至 `foods`，废弃 `food` |
| `food_category` | `food_categories` | 迁移至 `food_categories`，废弃 `food_category` |
| `dish_combo` | `dish_combos` | 迁移至 `dish_combos`，废弃 `dish_combo` |
| `dish_recipe` | `dish_recipes` | 迁移至 `dish_recipes`，废弃 `dish_recipe` |
| `finance_record` | `finance_records` | 迁移至 `finance_records`，废弃 `finance_record` |
| `stores` | `stores_new` | 迁移至 `stores_new`，废弃 `stores` |
| `orders` (V1) | `orders` (V6) | V6已覆盖V1，确认无V1残留数据 |
| `device_status_log` | `device_status_logs` | 迁移至 `device_status_logs`，废弃 `device_status_log` |

**总计**: 8组重复表，涉及16张表。

---

## 14. Migration/Schema冲突记录

### 14.1 schema.sql vs V1.0.0.100 冲突

| 问题 | 详情 |
|------|------|
| **冲突类型** | schema.sql 与首个Flyway迁移脚本表结构冲突 |
| **影响范围** | 基础表结构定义不一致 |
| **建议** | schema.sql应作为唯一真源，或完全依赖Flyway迁移链 |

### 14.2 V20260717_030 TRUNCATE数据破坏

| 问题 | 详情 |
|------|------|
| **冲突类型** | 迁移脚本执行TRUNCATE清空生产数据 |
| **影响范围** | 生产环境数据丢失 |
| **建议** | 迁移脚本禁止TRUNCATE操作，数据清理需走独立审批流程 |

### 14.3 V6.0.0 覆盖 V1.0.0.100 同名表

| 问题 | 详情 |
|------|------|
| **冲突类型** | V6.0.0迁移重新定义V1.0.0.100已创建的表 |
| **影响范围** | `orders` 等表结构变更，可能导致数据丢失 |
| **建议** | 同名表结构变更应通过ALTER迁移，而非DROP+CREATE |

---

## 15. 迁移版本时间线 (关键节点)

| 版本 | 说明 |
|------|------|
| `schema.sql` | 初始schema定义 |
| `V1.0.0.100` | 基础表结构建立 |
| `V20260717_030` | ⚠️ TRUNCATE数据破坏事件 |
| `V6.0.0` | 大规模重构，覆盖多张表 |

---

## 16. 风险与建议

### 高风险项
1. **重复表数据不一致** — 8组重复表可能导致业务逻辑混乱
2. **TRUNCATE数据破坏** — 迁移脚本安全机制缺失
3. **Schema冲突** — schema.sql与Flyway迁移链未对齐

### 改进建议
1. **清理重复表** — 制定迁移计划，统一使用新版本表名
2. **迁移脚本审查** — 禁止TRUNCATE/DROP操作，增加CI检查
3. **Schema对齐** — schema.sql与Flyway迁移链保持一致
4. **添加表注释** — 为每张表添加COMMENT，明确用途
5. **建立数据字典** — 统一管理表结构变更历史
