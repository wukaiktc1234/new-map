# 财务模块 P1 架构补齐 + 功能补全 — 设计文档

> **文档定位**：基于 FINANCE_MODULE_INVESTIGATION.md（2026-06-25 调研）与 DEVELOPMENT_STATUS.md §1.9（财务模块完成度 70% 评估），为 Sprint 3.2 P1 阶段提供架构补齐 + 功能补全的完整设计依据。
> **生成时间**：2026-06-26
> **配套文档**：
> - [FINANCE_MODULE_INVESTIGATION.md](FINANCE_MODULE_INVESTIGATION.md) — 财务模块入口缺失调研
> - [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md) §1.9 — 财务模块完成度评估（12 项严重问题 + 9 大类专业缺口）
> - [IMPLEMENTATION_DESIGN.md](IMPLEMENTATION_DESIGN.md) 第 6 章 — 第四批财务专业设计
> - `.specify/specs/051-sprint3-finance-overhaul/spec.md` — SDD Pipeline 功能规格（48 项）
> - `.specify/specs/051-sprint3-finance-overhaul/plan-p1.md` — P1 技术方案
> - `.specify/specs/051-sprint3-finance-overhaul/tasks-p1.md` — P1 任务拆解

---

## 1. 文档目的

Sprint 3.2 P1 阶段在 SDD Pipeline spec.md 中定义为"16 项架构补齐"（F-018 ~ F-033），但调研发现 P1 范围还需补充**财务功能补全**项，来源于：

1. **DEVELOPMENT_STATUS.md §1.9 的 12 项 API/对接层严重问题**：mock fallback 残留、字段不一致、凭证过账未更新科目余额、StatCard 类型错误
2. **FINANCE_MODULE_INVESTIGATION.md §六 成熟度评估**：12 个子模块中 7 个完成度低于 50%（费用报销/自动凭证/资产管理/财务审批/统计/发票管理/凭证管理）
3. **IMPLEMENTATION_DESIGN.md 第 6 章第四批**：财务报表/期末结账/财务共享（部分属 P2 范围，但基础工作可在 P1 启动）

本文档明确 P1 阶段的**架构补齐 + 功能补全**完整范围，作为 spec.md P1 段落的补充。

---

## 2. P1 完整范围（架构补齐 16 项 + 功能补全 8 项 = 24 项）

### 2.1 架构补齐（16 项，源自 spec.md F-018 ~ F-033）

| 项 | spec 编号 | 类型 | 简述 |
|---|---|---|---|
| 1 | F-018 | 后端 | AutoVoucherController 补齐（13 端点） |
| 2 | F-019 | 后端 | ManualInvoiceController 补齐（7 端点） |
| 3 | F-020 | 后端 | FinanceWarning Service+Controller 三层补齐 |
| 4 | F-021 | 后端 | 5 个 DataService 缓存层 |
| 5 | F-022 | 后端 | 财务事件持久化+重试机制 |
| 6 | F-023 | 后端 | FinanceVoucherStateMachine 状态机 |
| 7 | F-024 | 跨层 | 6 个旧 Controller 路径统一 + 18 条剩余路径 |
| 8 | F-025 | 后端 | 14 个重复实体清理 |
| 9 | F-026 | 前端 | 14 个 API 文件归集到 api/finance/ |
| 10 | F-027 | 前端 | 6 对重复页面清理 |
| 11 | F-028 | 前端 | FinanceProfit.vue 重命名 |
| 12 | F-029 | 前端 | 删除 menu-config.ts |
| 13 | F-030 | 后端+配置 | DatabaseInitConfig 拆分 + .env 规范化 |
| 14 | F-031 | 数据库 | 历史 Flyway 脚本 PostgreSQL 化 |
| 15 | F-032 | 前端 | 14 个孤儿页面路由补齐 |
| 16 | F-033 | 跨层 | 发票报销 ↔ 财务审批联动 |

### 2.2 功能补全（8 项，源自 DEVELOPMENT_STATUS.md §1.9 + FINANCE_MODULE_INVESTIGATION.md）

| 项 | 补全编号 | 类型 | 简述 | 来源 |
|---|---|---|---|---|
| 17 | F-018-A | 前端 | 10 个财务 API 文件 withMockFallback 清理 | §1.9 #1/4/5/6/7/8/9/10 |
| 18 | F-018-B | 后端 | 凭证过账更新科目余额逻辑补全 | §1.9 #11 |
| 19 | F-018-C | 前端 | StatCard colorType 类型修复 | §1.9 #12 |
| 20 | F-018-D | 后端 | FinanceInvoice 发票管理端点补齐（update/delete/issue/void/redFlush） | §调研 §4.1 |
| 21 | F-018-E | 前端 | FinanceInvoice 发票管理路由入口补齐 | §调研 §5.2 |
| 22 | F-018-F | 前端 | 凭证管理 3 页面路由入口补齐（VoucherManagement/ElectronicVoucher/FinanceVoucher） | §调研 §5.5 |
| 23 | F-018-G | 前端 | 财务首页 FinanceIndex.vue 路由补齐（作为 /finance 默认重定向） | §调研 P1 #9 |
| 24 | F-018-H | 后端 | FinanceAssetController mock 数据真实化（11 端点） | §调研 §4.6 |

### 2.3 不在 P1 范围（属 P2 专业功能）

以下属 P2 范围，P1 不执行：
- F-034: 8 类财务报表（资产负债表/利润表/现金流量表等）
- F-035-F-036: 电子凭证前端 UI
- F-037: 自动凭证规则引擎完善
- F-038: 期末调整与结账
- F-039: 会计核算方法配置
- F-040: 外币业务
- F-041: 财务档案管理
- F-042: 审计与内控
- F-043: 财务共享服务

---

## 3. 功能补全项详细设计

### 3.1 F-018-A：10 个财务 API 文件 withMockFallback 清理

**问题**：DEVELOPMENT_STATUS.md §1.9 #1/4/5/6/7/8/9/10 列出 8 个财务 API 文件使用 `withMockFallback` 掩盖后端调用失败，另有 2 个文件（budget/cost）字段不一致。

**涉及文件**（10 个）：
1. `frontend/src/api/finance/voucher.ts` — 移除 withMockFallback
2. `frontend/src/api/finance/budget.ts` — 移除 withMockFallback + status 字段 DataConverter
3. `frontend/src/api/finance/cost.ts` — 移除 withMockFallback + status 字段 DataConverter
4. `frontend/src/api/finance/payable.ts` — 移除 withMockFallback
5. `frontend/src/api/finance/receivable.ts` — 移除 withMockFallback
6. `frontend/src/api/finance/payment.ts` — 移除 withMockFallback
7. `frontend/src/api/finance/receipt.ts` — 移除 withMockFallback
8. `frontend/src/api/finance/bank-account.ts` — 移除 withMockFallback
9. `frontend/src/api/finance/fund-flow.ts` — 移除 withMockFallback
10. `frontend/src/api/finance/accounting-subject.ts` — 移除 withMockFallback

**修复方案**：
- 移除 `withMockFallback` 函数及其调用
- 失败时正常抛出错误，由前端 catch 处理
- budget/cost 的 status 字段通过 DataConverter 转换（字符串 ↔ 数字）

**验收**：10 个文件无 withMockFallback 残留，前端构建通过

### 3.2 F-018-B：凭证过账更新科目余额逻辑补全

**问题**：DEVELOPMENT_STATUS.md §1.9 #11 指出 `VoucherServiceImpl.post()` 凭证过账时未更新科目余额，导致科目余额表数据不准。

**涉及文件**：
- `backend/src/main/java/com/example/demo/service/finance/impl/VoucherServiceImpl.java`
- `backend/src/main/java/com/example/demo/mapper/finance/AccountingSubjectBalanceMapper.java`（若不存在则新建）

**修复方案**：
- post() 方法过账时，根据凭证明细借贷方向更新 `accounting_subject_balance` 表
- unpost() 方法反向过账时，回滚科目余额
- 借方科目：余额 += 金额；贷方科目：余额 -= 金额（资产类）或反向（负债/权益类）

**验收**：凭证过账后科目余额表数据正确，反过账后余额回滚

### 3.3 F-018-C：StatCard colorType 类型修复

**问题**：DEVELOPMENT_STATUS.md §1.9 #12 指出 FinanceBudget.vue 等 StatCard 的 colorType 类型错误。

**涉及文件**：
- `frontend/src/views/Finance/Budget.vue`
- `frontend/src/views/Finance/Cost.vue`
- `frontend/src/views/Finance/Fund.vue`
- 其他使用 StatCard 的财务页面

**修复方案**：
- StatTag colorType 应为联合类型 `'primary' | 'success' | 'warning' | 'danger' | 'info'`
- 修复所有财务页面的 colorType 类型定义

**验收**：无 TypeScript 类型错误

### 3.4 F-018-D：FinanceInvoice 发票管理端点补齐

**问题**：FINANCE_MODULE_INVESTIGATION.md §4.1 指出发票管理后端仅 3 端点（getPage/getDetail/create），缺 update/delete/issue/void/redFlush 5 个端点。

**涉及文件**：
- `backend/src/main/java/com/example/demo/controller/finance/InvoiceController.java`

**注**：此项在 spec.md P0 F-004 中已规划，需核对 P0 阶段是否已完成。若未完成则在 P1 补齐。

**修复方案**：补齐 5 个端点
- PUT /v1/finance/invoices/{id} — 更新发票
- DELETE /v1/finance/invoices/{id} — 删除发票
- POST /v1/finance/invoices/{id}/issue — 开票发出
- POST /v1/finance/invoices/{id}/void — 作废
- POST /v1/finance/invoices/{id}/red-flush — 红冲

**验收**：5 个端点可调用

### 3.5 F-018-E：FinanceInvoice 发票管理路由入口补齐

**问题**：FINANCE_MODULE_INVESTIGATION.md §5.2 指出发票管理流程严重断链，FinanceInvoice.vue 无菜单入口，详情页路由未定义。

**涉及文件**：
- `frontend/src/router/modules/finance.ts`

**注**：此项与 F-032（孤儿页面路由补齐）部分重叠，F-018-E 专门针对发票管理。

**修复方案**：
- 新增路由 `/finance/invoices` → FinanceInvoice.vue（列表）
- 新增路由 `/finance/invoices/:id` → FinanceInvoiceDetail.vue（详情，hidden）
- 新增路由 `/finance/ocr-upload` → OcrUpload.vue（OCR 上传）
- 新增路由 `/finance/manual-invoice` → ManualInvoiceInput.vue（手动录入）

**验收**：发票管理 4 个页面有路由入口，跳转无 404

### 3.6 F-018-F：凭证管理 3 页面路由入口补齐

**问题**：FINANCE_MODULE_INVESTIGATION.md §5.5 指出凭证管理流程严重断链，VoucherManagement/ElectronicVoucher/FinanceVoucher 3 个页面无菜单入口。

**涉及文件**：
- `frontend/src/router/modules/finance.ts`

**修复方案**：
- 新增路由 `/finance/voucher-management` → VoucherManagement.vue
- 新增路由 `/finance/electronic-voucher` → ElectronicVoucher.vue
- FinanceVoucher.vue 路由已在 F-032 中规划（`/finance/vouchers`）

**验收**：3 个凭证管理页面有路由入口

### 3.7 F-018-G：财务首页 FinanceIndex.vue 路由补齐

**问题**：FINANCE_MODULE_INVESTIGATION.md P1 #9 指出 FinanceIndex.vue 应作为 `/finance` 默认重定向目标。

**涉及文件**：
- `frontend/src/router/modules/finance.ts`

**修复方案**：
- 新增路由 `/finance/index` → FinanceIndex.vue
- 修改 `/finance` 重定向目标从 `/finance/ledger` 改为 `/finance/index`

**验收**：访问 `/finance` 显示财务首页

### 3.8 F-018-H：FinanceAssetController mock 数据真实化

**问题**：FINANCE_MODULE_INVESTIGATION.md §4.6 指出 FinanceAssetController 11 个端点全部返回硬编码 mock 数据。

**涉及文件**：
- `backend/src/main/java/com/example/demo/controller/FinanceAssetController.java`（待移动到 controller/finance/）
- `backend/src/main/java/com/example/demo/service/finance/AssetService.java`（若不存在则新建）

**注**：此项在 spec.md P0 F-002 中已规划（3 个 mock Controller 真实化），需核对 P0 阶段是否已完成。若未完成则在 P1 补齐。

**修复方案**：
- 对接 AssetMasterService（若存在）或新建 AssetService
- 11 个端点返回真实数据
- 移动 Controller 到 controller/finance/FinanceAssetController.java
- 路径统一为 `/v1/finance/assets`

**验收**：11 个端点返回真实数据，无 mock 残留

---

## 4. P1 执行顺序（更新版）

```
阶段 1：清理与规范化（10 任务 + 2 新增 = 12 任务）
  ├─ T-P1-001 ~ T-P1-006: 重复实体清理
  ├─ T-P1-007: menu-config.ts 删除
  ├─ T-P1-008, T-P1-009: .env 修复
  ├─ T-P1-010: 重复页面清理
  ├─ T-P1-010-A（新）: 10 个 API 文件 withMockFallback 清理 [F-018-A]
  └─ T-P1-010-B（新）: StatCard colorType 类型修复 [F-018-C]

阶段 2：后端架构补齐（18 任务 + 2 新增 = 20 任务）
  ├─ T-P1-011 ~ T-P1-030: 原架构补齐任务
  ├─ T-P1-030-A（新）: 凭证过账更新科目余额逻辑 [F-018-B]
  └─ T-P1-030-B（新）: FinanceAssetController mock 真实化 [F-018-H]

阶段 3：路径统一 + 发票端点补齐（6 任务 + 1 新增 = 7 任务）
  ├─ T-P1-031 ~ T-P1-036: 原路径统一任务
  └─ T-P1-036-A（新）: FinanceInvoice 5 端点补齐 [F-018-D]

阶段 4：前端 API 归集与路由补齐（12 任务 + 3 新增 = 15 任务）
  ├─ T-P1-037 ~ T-P1-048: 原前端归集任务
  ├─ T-P1-048-A（新）: 发票管理 4 路由入口补齐 [F-018-E]
  ├─ T-P1-048-B（新）: 凭证管理 3 路由入口补齐 [F-018-F]
  └─ T-P1-048-C（新）: 财务首页路由补齐 [F-018-G]

阶段 5：跨层联动与数据库规范化（6 任务，不变）

阶段 6：整体验收（4 任务，不变）

总任务数：58 + 8 = 66 任务
```

---

## 5. P1 与 P0 的衔接核对

| P0 已完成 | P1 衔接 | 核对状态 |
|---|---|---|
| F-001 InvoiceReimbursement 三层补齐 | F-033 审批联动 | ✓ |
| F-002 3 个 mock Controller 真实化 | F-018-H FinanceAsset mock 真实化 | ⚠ 需核对 P0 是否完成 FinanceAsset |
| F-003 路径统一 7 条 | F-024 剩余 18 条 | ✓ |
| F-004 Invoice 端点补齐 | F-018-D Invoice 5 端点补齐 | ⚠ 需核对 P0 是否完成 |
| F-005 20 个 API 封装 | F-026 API 文件归集 | ✓ |
| F-006 4 处路由跳转修复 | F-032 孤儿页面路由 | ✓ |
| F-013 7 条孤儿页面路由 | F-032 剩余 14 条 | ✓ |
| F-030 FinanceTableInitializer 移除 | F-030 DatabaseInitConfig 拆分 | ✓ |

**核对结论**：F-018-D（Invoice 端点）和 F-018-H（FinanceAsset mock）需在 P1 启动前核对 P0 完成状态，避免重复工作。

---

## 6. P1 验收标准（补充）

### 6.1 功能补全验收（新增）
- [ ] F-018-A: 10 个 API 文件无 withMockFallback 残留
- [ ] F-018-B: 凭证过账后科目余额表数据正确
- [ ] F-018-C: 无 StatCard colorType 类型错误
- [ ] F-018-D: Invoice 5 个端点可调用（若 P0 未完成）
- [ ] F-018-E: 发票管理 4 个页面有路由入口
- [ ] F-018-F: 凭证管理 3 个页面有路由入口
- [ ] F-018-G: /finance 重定向到财务首页
- [ ] F-018-H: FinanceAsset 11 端点返回真实数据（若 P0 未完成）

### 6.2 整体商用就绪度提升目标

| 维度 | P0 验收后 | P1 目标 |
|---|---|---|
| 财务模块完成度 | 70% | 85% |
| 前端页面有路由入口 | 18/67 | 40+/67 |
| 后端 Controller 路径统一 | 7/24 | 24/24 |
| API 文件 mock fallback | 10 个残留 | 0 残留 |
| 重复实体 | 14 个 | 0 |
| 重复页面 | 6 对 | 0 |
| DatabaseInitConfig 行数 | 3949 | < 500 |

---

## 7. 与其他审计文档的关系

```
docs/audit/
├── FINANCE_MODULE_INVESTIGATION.md  (调研报告，2026-06-25)
│   └─ 识别 6 大问题 → P0 6 项 + P1 6 项 + P2 5 项 + P3 4 项
│
├── DEVELOPMENT_STATUS.md §1.9  (完成度评估，2026-06-25)
│   └─ 12 项严重问题 + 9 大类专业缺口
│
├── IMPLEMENTATION_DESIGN.md 第 6 章  (P0 实现设计，2026-06-25)
│   └─ 第四批财务专业 5 项（属 P2 范围）
│
└── FINANCE_MODULE_P1_PLAN.md  (本文档，2026-06-26)
    └─ P1 架构补齐 16 项 + 功能补全 8 项 = 24 项

.specify/specs/051-sprint3-finance-overhaul/
├── spec.md  (SDD Pipeline 功能规格，48 项)
├── plan-p1.md  (P1 技术方案，10 ADR)
├── tasks-p1.md  (P1 任务拆解，58 任务)
└── acceptance-report-v3.md  (P0 验收报告，ACCEPT)
```

**文档定位说明**：
- `docs/audit/` 是项目最初的审计/调研文档，记录问题识别与完成度评估
- `.specify/specs/` 是 SDD Pipeline 的规范产物，记录 spec/plan/tasks/acceptance
- 两者互补：audit 识别问题，SDD Pipeline 执行整改
- 本文档（FINANCE_MODULE_P1_PLAN.md）是两者桥梁，将 audit 识别的功能补全项纳入 P1 范围

---

## 8. 后续工作建议

### 8.1 P1 启动前核对
1. 核对 P0 阶段 F-002 FinanceAsset mock 真实化是否完成（影响 F-018-H）
2. 核对 P0 阶段 F-004 Invoice 端点补齐是否完成（影响 F-018-D）
3. 核对 P0 阶段 10 个 API 文件 withMockFallback 是否已清理（影响 F-018-A）

### 8.2 P2 范围预告
P1 完成后，P2 专业功能（F-034 ~ F-043）将涵盖：
- 8 类财务报表（资产负债表/利润表/现金流量表等）
- 电子凭证前端 UI
- 期末调整与结账
- 会计核算方法配置
- 外币业务
- 财务档案管理
- 审计与内控
- 财务共享服务（连锁模式）

### 8.3 PostgreSQL 对接
P1 完成后，代码与配置层面完全就绪，可启动 PostgreSQL 实际搭建：
1. 历史 Flyway 脚本重写（F-031 已规范化语法）
2. DatabaseInitConfig 拆分后各 Initializer 核对 PG 兼容性
3. PostgreSQL 实际搭建与对接测试
