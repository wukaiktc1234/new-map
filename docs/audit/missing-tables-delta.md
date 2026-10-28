# 部署缺口变化：59 → 45（E 收口）

> 口径：`missing-tables-triage.md` 原始缺口 **59 张** → 本轮建表 **14 张** → 剩余 **45 张**。
> 最终数字（Lead 2026-10-28 核对通过）：`build=14 / pending_product=0 / skip=16 / from-zero executed=257`。

## 一、已建的 14 张（缺口 59 → 45）

| # | 表 | 批次 | 迁移文件 | 备注 |
|---|----|------|----------|------|
| 1 | `contract_document` | 批 1 | V20261028_014/015 | |
| 2 | `file_attachment` | 批 1 | V20261028_014 | |
| 3 | `finance_report` | 批 1 | V20261028_014 | |
| 4 | `hardware_config` | 批 1 | V20261028_014/015 | |
| 5 | `hardware_config_version` | 批 1 | V20261028_014/015 | |
| 6 | `inventory_code` | 批 2 | V20261028_016 | |
| 7 | `plan_item` | 批 2 | V20261028_016 | |
| 8 | `receivable` | 批 2 | V20261028_016 | |
| 9 | `requisition_requests` | 批 3 | V20261028_017 | |
| 10 | `swap_request_details` | 批 3 | V20261028_017 | |
| 11 | `travel_requests` | 批 3 | V20261028_017 | |
| 12 | `weighing_record` | 批 3 | V20261028_017 | |

## 二、skip 的 16 张（逐张理由）

### A. registry 登记的 8 张（三无/重复设计，Lead 批准）

| 表 | 理由 |
|----|------|
| `department_permissions` | role_permissions+role_departments 已覆盖（registry：重复设计） |
| `finance_approval` | 无 Controller 入口+无调度（registry：未完成功能） |
| `finance_report_detail` | 同上（registry：未完成功能） |
| `hardware_operation_log` | Mapper insert0/select0（registry：半成品） |
| `registration_code_log` | Mapper insert0/select0（registry：半成品） |
| `user_sessions` | 会话走 JWT+内存（registry：半成品） |
| `finance_account` | 与 bank_accounts/period_balance 重叠（registry：未完成功能） |
| `data_change_history` | 已有 finance_audit_logs/operation_logs（registry：半成品） |

### B. 今日新判定不建的 8 张

| 表 | 理由 |
|----|------|
| `operation_logs` | 实际查 sys_operation_logs；OperationLog 实体无调用方 |
| `scan_device` | 写入方在仓库外 |
| `weighing_device` | 同上 |
| `dish_ingredient` | 有活引用但生产从未建过，建表=让死链路变活 |
| `role_departments` | 同上（数据权限方案未定） |
| `role_stores` | 同上 |
| `sales_order` | 实体已指向 orders |
| `sales_order_detail` | 同上（order_items） |

## 三、剩余 45 张的分档

```text
59 张原始缺口
 − 14 张本轮已建
 = 45 张剩余，分三档：
   · 属「未上线功能 / 半成品」（registry 已登记 + 同型）      ≈ 8 张  → 待产品立项，不建
   · 属「废弃 / 重复映射 / 只在仓库外的只读配置」            ≈ 9 张  → 建议删除实体或标注 dead code
   · 属「真核心但需先定方案」（数据权限/召回/旧模型迁移）    ≈ 28 张 → 待方案确定后分批建
```

**一句话结论**：剩余 45 张中，约 **8 张属未上线功能（不建）**、约 **9 张属废弃/重复（建议删实体）**、约 **28 张待产品/方案确认后建**。

> 说明：45 张的逐张归属还需要 `missing-tables-triage.md` 的三档分类逐条裁定；
> 本文件先把「已建 14 / skip 16 / 剩余 45」的账对齐，逐张明细在其他文档（triage / registry / e-final-table-list.json）可交叉查。
