# Wave 0 Engineering Plan（真相源工程化）

> - 文档编号：WAVE0-EP
> - 日期：2026-09-08
> - 基于：SFR-001 裁决 + system-fact-map-v1.md + system-conflict-map.md + system-remediation-dependency-map.md
> - 角色：架构总控（会话1）
> - **阶段声明**：Wave 0 目标=冻结真相源、消除数据双源；本阶段只做规划，不写代码。

---

## 0. Wave 0 目标与范围

### 0.1 核心目标
为 Wave 1 Data/State 统一准备可靠基础，确保：
- 真相源已冻结
- 数据双源已消除
- 状态机语义已对齐
- 审计基线已建立

### 0.2 Wave 0 七项任务分析

| # | 原始任务 | 根因分析 | 独立性判定 | 合并建议 |
|---|---|---|---|---|
| 1 | 凭证映射 | 凭证状态机前后端映射错位（后端 0-3 vs 前端 1-4） | ✅ 独立（纯前端 UI 层） | 独立执行 |
| 2 | 发布审计 | 发布动作无 @AuditLog（C-11） | ✅ 独立（纯后端 API 层） | 独立执行 |
| 3 | account_balance 冻结 | PD-031 已决：accounting_subjects.balance 为管理口径真相，account_balance 冻结停用（C-15） | ✅ 独立（DB 层冻结） | 独立执行 |
| 4 | fund_flows status | PD-028 已决：fund_flows 增加 status 字段（C-16），同批 C 落库 | ⚠️ 依赖批 C（金额单位） | 与批 C 同步 |
| 5 | stores 清理 | V20260723_001 已重指 stores_new，旧表可清理（C-20） | ✅ 独立（DB 清理） | 独立执行 |
| 6 | 小程序转换 | Batch0-方案2 批 A：小程序金额单位转换（T-02） | ✅ 独立（纯前端 UI 层） | 独立执行 |
| 7 | 计价方法标注 / mock 移除 | PD-032 已决：计价方法标注「管理口径」（T-09）+ PD-011 已决：移除生产环境 mock | ✅ 独立（纯前端 UI 层） | 合并为 UI 标注批次 |

### 0.3 合并后 Wave 0 Work Packages（5 个）

| WP ID | 名称 | 层级 | 独立性 |
|---|---|---|---|
| W0-WP-01 | 凭证状态映射对齐 | UI | ✅ 独立 |
| W0-WP-02 | 发布审计补全 | API | ✅ 独立 |
| W0-WP-03 | account_balance 冻结停用 | DB | ✅ 独立 |
| W0-WP-04 | fund_flows status + 金额单位批 C | DB | ⚠️ 依赖批 C |
| W0-WP-05 | stores 旧表清理 + UI 标注批次 | DB+UI | ✅ 独立 |

---

## 1. W0-WP-01：凭证状态映射对齐

### 1.1 唯一 ID
`W0-WP-01`

### 1.2 当前事实
- 后端 FinanceVoucher.status: 0=DRAFT, 1=APPROVED, 2=POSTED, 3=VOID
- 前端 VoucherStatusMap: 1=草稿, 2=已审核, 3=已过账, 4=已作废
- 冲突：前后端映射错位（C-14）

### 1.3 根因
Batch0-方案3 未执行，前端状态映射与后端不一致

### 1.4 Truth Source
- 后端真相：`finance_vouchers.status`（0-3）
- 前端必须对齐后端真相

### 1.5 影响对象
- FinanceLedger.vue（财务总账页）
- FinanceVoucherVO.java（VO 层）
- VoucherStatusMap（converters.ts）

### 1.6 依赖
- 无前置依赖

### 1.7 是否需要 Product Decision
- ❌ 不需要（PD-028/031/032 已决，凭证状态机已确认）

### 1.8 DB 影响
- ❌ 无（纯前端映射修复）

### 1.9 API 影响
- ❌ 无（后端返回值不变）

### 1.10 Frontend 影响
- ✅ converters.ts: VoucherStatusMap 映射修正
- ✅ FinanceLedger.vue: StatusTag 显示修正
- ✅ 凭证详情对话框状态显示修正

### 1.11 Legacy 影响
- ❌ 无

### 1.12 风险等级
- **P1**（影响正常财务操作/追溯）

### 1.13 推荐实施顺序
- **Batch 0-1**（最高确定性，纯前端）

### 1.14 可验证验收条件
1. VoucherStatusMap 后端 0-3 → 前端正确映射
2. 凭证列表状态列显示正确（草稿/已审核/已过账/已作废）
3. 凭证详情对话框状态显示正确
4. 状态筛选功能正确

### 1.15 回滚/风险控制
- 回滚：git revert 前端修改
- 风险：极低（纯前端映射）

### 1.16 对应引用
- Fact Map: §5.2（凭证状态机）
- Conflict Map: C-14（凭证状态映射）
- Dependency Map: T-06
- SFR-001: §6 Wave 0

---

## 2. W0-WP-02：发布审计补全

### 2.1 唯一 ID
`W0-WP-02`

### 2.2 当前事实
- 发布动作（foods.status 更新）无 @AuditLog（C-11）
- AuditLogAspect 仅拦截 @annotation 标注的方法

### 2.3 根因
发布方法未标注 @AuditLog 注解

### 2.4 Truth Source
- 审计日志真相源：`sys_operation_logs` 表
- 发布动作必须产生审计记录

### 2.5 影响对象
- FoodNewController.java（发布方法）
- 其他可能的发布方法（grep 确认）

### 2.6 依赖
- 无前置依赖

### 2.7 是否需要 Product Decision
- ❌ 不需要（审计为技术规范，非业务规则）

### 2.8 DB 影响
- ❌ 无（仅写入现有 sys_operation_logs）

### 2.9 API 影响
- ❌ 无（仅增加审计日志，不影响返回值）

### 2.10 Frontend 影响
- ❌ 无

### 2.11 Legacy 影响
- ❌ 无

### 2.12 风险等级
- **P2**（审计缺口，不阻塞核心流程）

### 2.13 推荐实施顺序
- **Batch 0-2**（独立，无依赖）

### 2.14 可验证验收条件
1. 发布方法标注 @AuditLog
2. 发布操作后 sys_operation_logs 有记录
3. 审计日志包含操作人/时间/动作/对象

### 2.15 回滚/风险控制
- 回滚：git revert 后端修改
- 风险：极低（仅增加审计）

### 2.16 对应引用
- Fact Map: §4.2（事件断链）
- Conflict Map: C-11（发布审计）
- Dependency Map: T-13
- SFR-001: §6 Wave 0

---

## 3. W0-WP-03：account_balance 冻结停用

### 3.1 唯一 ID
`W0-WP-03`

### 3.2 当前事实
- account_balance 表（元，人工刷新）与 accounting_subjects.balance（分，自动）双轨并存
- PD-031 已决：accounting_subjects.balance 为管理口径真相，account_balance 冻结停用

### 3.3 根因
科目余额双轨（C-15/C-21），PD-031 已裁决处置方式

### 3.4 Truth Source
- 管理口径真相：`accounting_subjects.balance`（分，自动）
- 冻结对象：`account_balance`（元，人工）

### 3.5 影响对象
- account_balance 表（停止写入）
- 相关 Service（停止调用 updateBalance）
- 页面标注（「管理台账口径」）

### 3.6 依赖
- PD-031 已决（✅ 可立即执行）

### 3.7 是否需要 Product Decision
- ❌ 不需要（PD-031 已决）

### 3.8 DB 影响
- ✅ 停止写入 account_balance 表
- ❌ 不删除表（冻结停用）

### 3.9 API 影响
- ❌ 无（仅停止调用）

### 3.10 Frontend 影响
- ✅ 相关页面标注「管理台账口径」
- ✅ 移除 account_balance 相关刷新逻辑

### 3.11 Legacy 影响
- account_balance 表保留不删（冻结）

### 3.12 风险等级
- **P0**（财务事实风险，PD-031 已决）

### 3.13 推荐实施顺序
- **Batch 0-3**（高优先级，PD 已决）

### 3.14 可验证验收条件
1. account_balance 表停止写入
2. 无 Service 调用 updateBalance
3. 页面标注「管理台账口径」
4. accounting_subjects.balance 为唯一真相源

### 3.15 回滚/风险控制
- 回滚：恢复 updateBalance 调用
- 风险：中（需确认无遗漏调用点）

### 3.16 对应引用
- Fact Map: §1.1（科目余额）
- Conflict Map: C-15/C-21（科目余额双轨）
- Dependency Map: T-08
- SFR-001: §6 Wave 0

---

## 4. W0-WP-04：fund_flows status + 金额单位批 C

### 4.1 唯一 ID
`W0-WP-04`

### 4.2 当前事实
- fund_flows 表无 status 字段（C-16）
- 前端 FundFlowStatusMap 声明悬空
- 金额单位双轨：tax_record/account_balance 元，其他分

### 4.3 根因
- PD-028 已决：fund_flows 增加 status
- Batch0-方案2 批 C：tax_record/account_balance 元口径统一

### 4.4 Truth Source
- 资金流水真相源：`fund_flows`
- status 字段必须存在

### 4.5 影响对象
- fund_flows 表（新增 status 列）
- FundFlow 实体（新增 status 字段）
- FundFlowVO（新增 status 字段）
- 前端 FundFlowStatusMap（接真实字段）
- tax_record 表（元→分转换）
- account_balance 表（冻结，见 W0-WP-03）

### 4.6 依赖
- ⚠️ 依赖 Batch0-方案2 批 C（金额单位统一）
- ⚠️ 依赖 W0-WP-03（account_balance 冻结）

### 4.7 是否需要 Product Decision
- ❌ 不需要（PD-028 已决）

### 4.8 DB 影响
- ✅ fund_flows 表新增 status 列
- ✅ tax_record 表金额字段元→分
- ✅ account_balance 表冻结（见 W0-WP-03）

### 4.9 API 影响
- ✅ FundFlow API 返回 status 字段

### 4.10 Frontend 影响
- ✅ FundFlowStatusMap 接真实字段
- ✅ FinanceFund.vue 状态显示修正

### 4.11 Legacy 影响
- ❌ 无

### 4.12 风险等级
- **P0**（财务事实风险，PD-028 已决）

### 4.13 推荐实施顺序
- **Batch 0-4**（依赖批 C + W0-WP-03）

### 4.14 可验证验收条件
1. fund_flows.status 字段存在
2. FundFlow 实体/VO 包含 status
3. 前端 FundFlowStatusMap 接真实字段
4. FinanceFund.vue 状态显示正确（待核销/已核销）
5. tax_record 金额为分
6. account_balance 停止写入

### 4.15 回滚/风险控制
- 回滚：migration 回滚 + 代码 revert
- 风险：中（涉及 DB 变更）

### 4.16 对应引用
- Fact Map: §1.1（资金流水）
- Conflict Map: C-16（资金流水无 status）
- Dependency Map: T-07 + T-04
- SFR-001: §6 Wave 0

---

## 5. W0-WP-05：stores 旧表清理 + UI 标注批次

### 5.1 唯一 ID
`W0-WP-05`

### 5.2 当前事实
- stores 表已重指 stores_new（V20260723_001）
- stores 旧表仍存在（C-20）
- 计价方法需标注「管理口径」（T-09）
- 生产环境 mock 需移除（PD-011）

### 5.3 根因
- stores 双表：V20260723_001 已重指，旧表可清理
- 计价方法：PD-032 已决，需标注
- mock：PD-011 已决，需移除

### 5.4 Truth Source
- 门店真相源：`stores_new`
- 计价方法真相：最新入库价（PD-032）

### 5.5 影响对象
- stores 旧表（清理）
- 成本分析页（计价方法标注）
- 生产环境 mock（移除）

### 5.6 依赖
- ✅ 无前置依赖

### 5.7 是否需要 Product Decision
- ❌ 不需要（PD-011/032 已决）

### 5.8 DB 影响
- ✅ stores 旧表清理（可选：删除或标记废弃）

### 5.9 API 影响
- ❌ 无

### 5.10 Frontend 影响
- ✅ 成本分析页标注「管理口径：最新入库价」
- ✅ 移除生产环境 mock/假数据

### 5.11 Legacy 影响
- stores 旧表清理

### 5.12 风险等级
- **P2**（效率/可发现性问题）

### 5.13 推荐实施顺序
- **Batch 0-5**（独立，低风险）

### 5.14 可验证验收条件
1. stores 旧表清理完成
2. 成本分析页标注「管理口径：最新入库价」
3. 生产环境无演示登录/假 openid/支付 mock

### 5.15 回滚/风险控制
- 回滚：stores 表恢复 + mock 恢复
- 风险：低

### 5.16 对应引用
- Fact Map: §8.1（Legacy 依赖）
- Conflict Map: C-20（stores 双表）
- Dependency Map: T-09 + PD-011
- SFR-001: §6 Wave 0

---

## 6. Dependency Graph（依赖图）

```
W0-WP-01（凭证状态映射）  ─────────────────────────────────┐
                                                            │
W0-WP-02（发布审计）  ──────────────────────────────────────┤
                                                            │
W0-WP-03（account_balance 冻结）  ──→ W0-WP-04（fund_flows status + 批 C）│
                                                            │
W0-WP-05（stores 清理 + UI 标注）  ────────────────────────┘
```

### 6.1 依赖关系表

| WP | 前置依赖 | 后置依赖 |
|---|---|---|
| W0-WP-01 | 无 | 无 |
| W0-WP-02 | 无 | 无 |
| W0-WP-03 | 无 | W0-WP-04 |
| W0-WP-04 | W0-WP-03 + 批 C | 无 |
| W0-WP-05 | 无 | 无 |

### 6.2 关键路径
- W0-WP-03 → W0-WP-04（account_balance 冻结 → fund_flows status）

---

## 7. Execution Batches（执行批次）

### 7.1 批次划分原则
- 最大并行度
- 最小风险
- 独立可验证

### 7.2 批次设计

| 批次 | WP | 并行度 | 预计工作量 | 前置条件 |
|---|---|---|---|---|
| **Batch 0-1** | W0-WP-01 + W0-WP-02 + W0-WP-05 | 3 并行 | 2-3 天 | 无 |
| **Batch 0-2** | W0-WP-03 | 1 | 1-2 天 | 无 |
| **Batch 0-3** | W0-WP-04 | 1 | 2-3 天 | Batch 0-2 + 批 C |

### 7.3 批次依赖

```
Batch 0-1（凭证+发布+stores）  ──→  Release Gate 1
                                          ↓
Batch 0-2（account_balance）    ──→  Release Gate 2
                                          ↓
Batch 0-3（fund_flows + 批 C）  ──→  Release Gate 3（Wave 0 完成）
```

---

## 8. QA Gate（质量门禁）

### 8.1 每 WP 验收标准

| WP | QA 验收项 | 通过标准 |
|---|---|---|
| W0-WP-01 | 凭证状态映射 | 后端 0-3 → 前端正确显示；筛选正确 |
| W0-WP-02 | 发布审计 | 发布操作 → sys_operation_logs 有记录 |
| W0-WP-03 | account_balance 冻结 | 停止写入；页面标注「管理台账口径」 |
| W0-WP-04 | fund_flows status | status 字段存在；页面显示正确；tax_record 为分 |
| W0-WP-05 | stores 清理 + UI 标注 | 旧表清理；计价方法标注；mock 移除 |

### 8.2 Release Gate

| Gate | 条件 | 通过标准 |
|---|---|---|
| Gate 1 | Batch 0-1 完成 | 3 WP 全部 QA PASS |
| Gate 2 | Batch 0-2 完成 | W0-WP-03 QA PASS |
| Gate 3 | Batch 0-3 完成 | W0-WP-04 QA PASS + Wave 0 完成 |

### 8.3 回归基线

| 基线 | 覆盖范围 | 通过标准 |
|---|---|---|
| REG-W0-001 | 凭证状态映射 | 凭证列表/详情状态正确 |
| REG-W0-002 | 发布审计 | 审计日志可查 |
| REG-W0-003 | account_balance | 无新写入 |
| REG-W0-004 | fund_flows | status 字段正确 |
| REG-W0-005 | stores 清理 | 旧表不可用 |

---

## 9. 与 Wave 1 的衔接

### 9.1 Wave 0 → Wave 1 依赖

| Wave 0 完成项 | Wave 1 解锁项 |
|---|---|
| W0-WP-03（account_balance 冻结） | T-04（tax_record 元→分） |
| W0-WP-04（fund_flows status） | T-11（统一流水模型） |
| W0-WP-05（stores 清理） | 门店数据范围清晰化 |

### 9.2 Wave 1 阻塞项（Wave 0 无法解锁）

| Wave 1 项目 | 阻塞原因 |
|---|---|
| T-10（订单状态机统一） | PD-015 阶段二 + PD-017~021 待产品 |
| T-05（orders_legacy 金额迁移） | PD-015 阶段二待产品 |
| T-17（锁定量完整策略） | 产品决策联动 |

---

## 10. Non-Goals（非目标）

Wave 0 **不做**：

| 非目标 | 说明 |
|---|---|
| ❌ 订单双表迁移 | PD-015 阶段二待产品 |
| ❌ 订单状态机统一 | PD-017~021 待产品 |
| ❌ POS 双源统一 | G1-1/G1-2 待核实 |
| ❌ 权限缺口闭环 | PD-006~010 待产品 |
| ❌ UI 重构 | 仅标注/映射修复 |
| ❌ 新增业务规则 | 仅执行已决 PD |

---

## 11. 交付物清单

| # | 交付物 | 内容 |
|---|---|---|
| 1 | Wave 0 Engineering Plan | 本文档 |
| 2 | 5 个 Work Package 详细规格 | §1-5 |
| 3 | Dependency Graph | §6 |
| 4 | Execution Batches | §7 |
| 5 | QA Gate | §8 |

---

## 12. 审批记录

| 审批人 | 审批内容 | 日期 | 状态 |
|---|---|---|---|
| 产品负责人 | Wave 0 范围确认 | 2026-09-08 | ⏳ 待确认 |

---

*本 Wave 0 Engineering Plan 为真相源工程化规划文档，等待产品负责人最终确认。*
*文档生成：架构总控（会话1）· 2026-09-08*
