# E 最终建表清单（triage × registry 交叉核对，decision 为唯一口径）

生成：2026-10-28 ｜ **判定字段只有三个值**：`BUILD` / `BUILD_PENDING_PRODUCT` / `SKIP`（**不用括号备注**）
机器可读版本：`docs/audit/e-final-table-list.json`（脚本统计以它为准）

## 判据差异（冲突根源）

| 清单 | 判据 |
|------|------|
| `missing-tables-triage.md` | 有写入代码（.insert/.update/.save 出现）→ 宽松 |
| `unfinished-features-registry.txt` | 三无（无表+无 Controller 入口+无调度），半成品还要求 Mapper 调用 0 → 严格 |

→ 冲突时以 registry 为准：**有写入代码但没有入口 = 半成品，不建表**。

## 统计（当场纠正过口径）

```text
BUILD                    : 12 张  ← **实际要建**
BUILD_PENDING_PRODUCT    : 2 张  ← 等产品回复，批次里留空位但不建
SKIP                     : 16 张
合计候选                 : 30 张

历史口径澄清：25（我最初的估）→ 22（registry 过滤后）→ 17（再扣今天判定）→ **12 实际建 + 2 待产品**
```

## 分批（逐张列名）

```text
第 1 批（5 张）：contract_document ｜ file_attachment ｜ finance_report ｜ hardware_config ｜ hardware_config_version
第 2 批（5 张）：inventory_code ｜ plan_item ｜ receivable ｜ requisition_requests ｜ swap_request_details
第 3 批（2 张）：travel_requests ｜ weighing_record
待产品确认（不进任何批次）：self_purchase ｜ self_purchase_item
```

## 逐张判定

| # | 表 | triage | 在 registry | decision | 依据 |
|---|----|--------|-------------|----------|------|
| 1 | `contract_document` | 核心必做 | 否 | **BUILD** | 有实体+Mapper+写入点，前端合同模块在用 |
| 2 | `file_attachment` | 核心必做 | 否 | **BUILD** | 附件通用基建；只存元数据+URL，禁 Blob/本地磁盘 |
| 3 | `finance_report` | 核心必做 | 否 | **BUILD** | 有入口；detail 不建 |
| 4 | `hardware_config` | 核心必做 | 否 | **BUILD** | 设备配置页在用 |
| 5 | `hardware_config_version` | 核心必做 | 否 | **BUILD** | 配置版本化 |
| 6 | `inventory_code` | 核心必做 | 否 | **BUILD** | 一物一码，销售出库/扫码核销在写 |
| 7 | `plan_item` | 核心必做 | 否 | **BUILD** | 采购计划明细 |
| 8 | `receivable` | 核心必做 | 否 | **BUILD** | 应收，POS 支付链路 |
| 9 | `requisition_requests` | 核心必做 | 否 | **BUILD** | 审批域单据 |
| 10 | `swap_request_details` | 核心必做 | 否 | **BUILD** | 审批域明细 |
| 11 | `travel_requests` | 核心必做 | 否 | **BUILD** | 审批域单据 |
| 12 | `weighing_record` | 核心必做 | 否 | **BUILD** | 称重记录是业务数据（设备表不建，记录要建） |
| 13 | `self_purchase` | 核心必做 | 否 | **BUILD_PENDING_PRODUCT** | 前端 0 命中，属「写了没上线」，等产品决定 |
| 14 | `self_purchase_item` | 核心必做 | 否 | **BUILD_PENDING_PRODUCT** | 随主表 |
| 15 | `operation_logs` | 核心必做 | 否（误标已修） | **SKIP** | 实际查 sys_operation_logs；OperationLog 实体无调用方 |
| 16 | `scan_device` | 核心必做 | 否（只读配置） | **SKIP** | 写入方在仓库外 |
| 17 | `weighing_device` | 核心必做 | 否（只读配置） | **SKIP** | 同上 |
| 18 | `dish_ingredient` | 核心必做 | 否（保留不删） | **SKIP** | 有活引用但生产从未建过，建表=让死链路变活 |
| 19 | `role_departments` | 核心必做 | 否（保留不删） | **SKIP** | 同上（数据权限方案未定） |
| 20 | `role_stores` | 核心必做 | 否（保留不删） | **SKIP** | 同上 |
| 21 | `sales_order` | 核心必做 | 否（重复映射） | **SKIP** | 实体已指向 orders |
| 22 | `sales_order_detail` | 核心必做 | 否（重复映射） | **SKIP** | 同上（order_items） |
| 23 | `department_permissions` | 核心必做 | 是（重复设计） | **SKIP** | role_permissions+role_departments 已覆盖 |
| 24 | `finance_approval` | 核心必做 | 是（未完成功能） | **SKIP** | 无 Controller 入口+无调度 |
| 25 | `finance_report_detail` | 核心必做 | 是（未完成功能） | **SKIP** | 同上 |
| 26 | `hardware_operation_log` | 核心必做 | 是（半成品） | **SKIP** | Mapper insert0/select0 |
| 27 | `registration_code_log` | 核心必做 | 是（半成品） | **SKIP** | Mapper insert0/select0 |
| 28 | `user_sessions` | 核心必做 | 是（半成品） | **SKIP** | 会话走 JWT+内存 |
| 29 | `finance_account` | 未列 | 是（未完成功能） | **SKIP** | 与 bank_accounts/period_balance 重叠 |
| 30 | `data_change_history` | 未列 | 是（半成品） | **SKIP** | 已有 finance_audit_logs/operation_logs |
