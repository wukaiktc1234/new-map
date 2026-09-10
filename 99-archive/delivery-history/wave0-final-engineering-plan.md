# Wave 0 Final Engineering Plan（最终裁决）

> - 文档编号：W0-FEP-V1.0
> - 日期：2026-09-08
> - 输入：SFR-001（Wave 0 初版）+ 代码探索验证（6 项事实核验）
> - 角色：架构总控（会话1）
> - **阶段声明**：暂不进入 Developer 实施。本计划为最终裁决版本，等待产品负责人确认后方可启动。

---

## 一、Wave 0 原则（严格边界）

> Wave 0 只允许：**Truth Source / Semantic consistency / Legacy truth isolation / Data-unit normalization**。不可绕过底层事实的安全基础整改。

**Wave 0 不允许**：一般 UI 优化 / 页面重构 / 体验改善 / 业务新规则。

---

## 二、7 项原始任务 → 5 WP 完整映射

| 原始任务 | 归入 WP | 映射理由 |
|---|---|---|
| ① 凭证映射 | **W0-WP-01** | 前端语义键对齐（凭证状态=Level 2 事实的一部分） |
| ② 发布审计 | **W0-WP-02** | Domain/API 层审计缺口（不涉及 UI） |
| ③ account_balance 冻结 | **W0-WP-03** | DB 层真相源隔离（P0 核心） |
| ④ fund_flows status | **W0-WP-04** | DB 层真相源补全（status 字段缺失=事实不完整） |
| ⑤ stores 清理 | **→ 移出 Wave 0** | 旧表仍有大量活跃读写（探索确认），不可在 Wave 0 冻结 |
| ⑥ 小程序转换 | **→ 移出 Wave 0** | 纯前端展示层修复，不涉及 Truth Source |
| ⑦ 计价方法标注 / mock 移除 | **W0-WP-02**（标注）/ **→ 移出 Wave 0**（mock 移除） | 标注=语义一致性（Wave 0）；mock 移除=体验改善（Wave 4） |

### 重分类依据

| 原始任务 | 重分类 | 代码证据 |
|---|---|---|
| ⑤ stores 清理 | **→ Wave 5**（Legacy） | 探索确认：StoreMapper/StoreServiceImpl 对旧表 `stores` 提供完整 CRUD（selectList/selectById/insert/updateById/deleteById）；DataPermissionServiceImpl L597 读旧表；ReceiptConfirmationServiceImpl L574 读旧表；SystemInitServiceImpl L173 写旧表。**两套门店表并存且均有活跃消费**，不可在 Wave 0 冻结 |
| ⑥ 小程序转换 | **→ Wave 4**（Frontend） | 纯前端展示层（fenToYuan 转换），不涉及 Truth Source / DB / Domain |
| ⑦ mock 移除 | **→ Wave 4**（Frontend） | 体验改善（PD-011），不涉及 Truth Source |
| ⑦ 计价方法标注 | **保留 Wave 0**（标注部分） | 成本分析页标注「管理口径：最新入库价」=语义一致性（PD-032 已裁决） |

---

## 三、WP 详细规格

### W0-WP-01：凭证状态映射对齐（前端语义键）

| 项 | 内容 |
|---|---|
| **范围** | `frontend/src/api/finance/converters.ts` VoucherStatusMap + `FinanceLedger.vue` 状态标签 + 按钮可用性核对 |
| **根因** | 前后端凭证状态映射曾错位（后端 0-3 vs 前端 1-4），导致状态标签显示错误 |
| **当前状态** | 探索确认：converters.ts VoucherStatusMap **已对齐 0-3**（0=draft/1=audited/2=posted/3=cancelled）；FinanceVoucherStateMachine 后端值 0-3 一致。**此修复可能已在之前 UI 批次中完成** |
| **是否解决 Truth Source** | ⚠️ **否**——这是展示层修复，不涉及 DB/Domain 层真相源。凭证状态的真相源（finance_vouchers.voucher_status 0-3）从未被 UI 修改影响 |
| **是否存在 Domain/API/DB 前置** | 后端 FinanceVoucherStateMachine 已正确（0-3）；前端映射已对齐。**无前置依赖** |
| **UI 先于底层是否安全** | ✅ 安全——后端已正确，前端只是修正展示映射 |
| **判定** | **降级为语义一致性验证**（非 Truth Source 修复）：确认映射正确 + 按钮可用性 + 状态标签显示；若已修复则验证通过即可 |
| **风险** | 低（纯前端展示层） |
| **QA Gate** | 账本页 4 状态标签显示正确（暂存/已审核/已过账/已作废）；审核/过账/反过账/作废按钮可用性正确 |
| **Rollback** | 前端单文件回退（零风险） |
| **Engineering-Ready** | ✅ **是**（若已修复则验证；若未修复则纯前端修复） |
| **分类** | **A. Engineering-Ready** |

---

### W0-WP-02：发布动作补审计 + 计价方法标注

| 项 | 内容 |
|---|---|
| **范围** | `FoodServiceImpl.updateStatus()` + `FoodServiceImpl.batchUpdateStatus()` 补 `@AuditLog`；成本分析页标注「管理口径：最新入库价」 |
| **根因** | AuditLogAspect 使用 `@Around("@annotation(auditLog)")` 模式，仅拦截标注了 @AuditLog 的方法；FoodServiceImpl 的 updateStatus/batchUpdateStatus **均未标注**（探索确认：全项目 52 处 @AuditLog，FoodController 未使用） |
| **当前状态** | 菜品发布/停售操作**无审计日志**（Fact Map C-11 / §4 事件断链） |
| **是否解决 Truth Source** | ✅ **是**——审计日志=事实可追溯性的基础（发布动作是 Level 1 生命周期事件，无审计=事实链断裂） |
| **Domain/API/DB 前置** | 无（纯注解标注） |
| **风险** | 低（仅加注解，不改业务逻辑） |
| **QA Gate** | 发布/停售操作后 sys_operation_logs 有审计记录；成本分析页标注可见 |
| **Rollback** | 移除 @AuditLog 注解（零风险） |
| **Engineering-Ready** | ✅ **是** |
| **分类** | **A. Engineering-Ready** |

---

### W0-WP-03：account_balance 冻结停用（P0 核心）

| 项 | 内容 |
|---|---|
| **范围** | 冻结 `account_balance` 表写入；页面统一标注「管理台账口径」 |
| **根因** | account_balance（元/人工）与 accounting_subjects.balance（分/自动）双轨并存（Fact Map C-15/C-21） |
| **当前写入入口**（探索确认） | **仅 2 个活跃写入路径**：① `POST /v1/finance/account-balance/refresh`（recalculateAllBalances，**无 @OperationLog**）；② `PUT /v1/finance/account-balance/opening`（updateOpeningBalance，**有 @OperationLog**）。**无定时任务写入；无 Event Listener 写入；VoucherServiceImpl 不写 account_balance**（它写 accounting_subject.balance）。`updateBalancesFromVoucher()` 虽存在但**无外部调用方**（dead code） |
| **冻结后唯一余额真相** | `accounting_subjects.balance`（分，凭证过账自动写入，乐观锁） |
| **历史数据兼容策略** | account_balance 表数据保留（不删除）；页面标注「管理台账口径（历史）」；未来报表以 accounting_subjects.balance 为准 |
| **旧 API / legacy path** | 无旧版 API 路径；唯一路径为 `/v1/finance/account-balance/` |
| **QA 如何证明无新 direct balance mutation** | ① POST /refresh 端点冻结（返回 403/禁用提示）；② 灰度期间监控 account_balance 表 update_time（无新写入=冻结成功）；③ accounting_subjects.balance 的过账写入不受影响（独立链路） |
| **是否阻塞现有生产业务** | **不阻塞**——POST /refresh 为手工刷新（非自动链路）；PUT /opening 为期初录入（一次性操作，PD-003 已决策）；冻结后业务链路（凭证过账→accounting_subjects.balance）**不受影响** |
| **风险** | **低**（写入入口极少、无自动链路依赖、dead code 已确认） |
| **QA Gate** | POST /refresh 返回禁用提示；account_balance 表 update_time 无新变化；accounting_subjects.balance 过账写入正常；页面标注正确 |
| **Rollback** | 恢复 POST /refresh 端点（零风险，无数据变更） |
| **Engineering-Ready** | ✅ **是** |
| **分类** | **A. Engineering-Ready** |

---

### W0-WP-04：fund_flows 增加 status 字段

| 项 | 内容 |
|---|---|
| **范围** | fund_flows 表增加 `status` 列（INTEGER，值=0 待核销 / 1 已核销）；FundFlow 实体增加 status 字段；FundFlowVO 增加 status；页面展示 status |
| **根因** | fund_flows 表**无 status 字段**（探索确认：实体/migration 均无）；前端 FundFlowStatusMap 声明的「后端 1~3 ↔ pending/completed/cancelled」是**悬空声明**（PD-028） |
| **"批 C"具体是什么** | Batch0-方案2 批 C = **tax_record / account_balance 元口径统一**（元→分或明确边界）。**与 fund_flows status 无关**——批 C 是金额单位问题，fund_flows status 是状态字段问题。**两者不可混淆** |
| **对应 Fact Map / Remediation item** | Fact Map §2.3 C-16（fund_flows 无 status）；PD-028（已决：增 status）；Remediation T-07 |
| **是否有 Product Decision** | ✅ PD-028 已决（2026-08-31，产品负责人确认：fund_flows 增 status，与金额单位方案同批落库） |
| **为什么与 W0-WP-03 同批** | **不应同批**——两者无数据依赖：W0-WP-03 冻结 account_balance（元/人工），W0-WP-04 增 fund_flows status（新字段）。唯一关联是同属财务域真相源补全，但**可独立执行** |
| **是否可拆开执行** | ✅ **可以拆开**——fund_flows status 仅依赖 migration（ALTER TABLE ADD COLUMN），不依赖 account_balance 冻结 |
| **是否影响 Wave 1 T-11** | **不影响**——T-11（统一流水模型）是库存域 transactions 只写不读修复；fund_flows 是财务域资金流水，两者不同表 |
| **风险** | 低（ALTER TABLE ADD COLUMN 无数据变更） |
| **QA Gate** | migration 执行成功；FundFlow 实体 status 字段可读写；前端流水列表状态列显示正确（待核销/已核销） |
| **Rollback** | ALTER TABLE DROP COLUMN（低风险） |
| **Engineering-Ready** | ✅ **是** |
| **分类** | **A. Engineering-Ready** |

---

## 四、WP 间 Dependency

```
W0-WP-01（凭证映射）      ← 无依赖，可立即执行
W0-WP-02（发布审计+标注）  ← 无依赖，可立即执行
W0-WP-03（account_balance 冻结） ← 无依赖，可立即执行
W0-WP-04（fund_flows status）   ← 无依赖，可立即执行（不依赖 W0-WP-03）

结论：4 个 WP 全部无相互依赖，可并行执行。
```

## 五、Product Decision Dependency

| WP | 依赖 PD | 状态 |
|---|---|---|
| W0-WP-01 | 无 | ✅ 无阻塞 |
| W0-WP-02 | PD-032（计价方法标注） | ✅ 已决 |
| W0-WP-03 | PD-031（科目余额冻结） | ✅ 已决 |
| W0-WP-04 | PD-028（fund_flows status） | ✅ 已决 |

## 六、Wave 1 Unlock Relation

| Wave 1 整改项 | 解锁条件 | W0 是否解锁 |
|---|---|---|
| T-10（订单状态机统一） | PD-015 阶段二 + PD-017~021 | ❌ Wave 0 不解锁（需产品决策） |
| T-05（legacy 金额迁移） | PD-015 阶段二 | ❌ Wave 0 不解锁 |
| T-11（统一流水模型） | T-10 完成 | ❌ Wave 0 不解锁 |
| T-04（tax/balance 统一） | W0-WP-03 冻结 + 财务口径 | ⚠️ W0-WP-03 冻结 account_balance=**部分解锁**（batch C 中 account_balance 元→分可执行；tax_record 元→分仍需财务口径确认） |

## 七、Risk

| WP | 风险 | 缓解 |
|---|---|---|
| W0-WP-01 | 低（若已修复则验证通过即可） | 先核对当前代码状态 |
| W0-WP-02 | 低（仅加注解） | 全量回归菜品发布/停售链路 |
| W0-WP-03 | **低**（探索确认：2 个写入入口、无自动链路、dead code） | 冻结 POST /refresh；监控 update_time |
| W0-WP-04 | 低（ALTER TABLE ADD COLUMN） | 灰度 migration；回退=DROP COLUMN |

## 八、QA Gate（逐 WP）

| WP | QA 验收标准 |
|---|---|
| W0-WP-01 | 账本页凭证状态标签 4 种显示正确；审核/过账/反过账/作废按钮可用性正确 |
| W0-WP-02 | 菜品发布/停售后 sys_operation_logs 有审计记录；成本分析页标注「管理口径：最新入库价」可见 |
| W0-WP-03 | POST /refresh 返回禁用提示；account_balance.update_time 无新变化；accounting_subjects.balance 过账正常；页面标注「管理台账口径」 |
| W0-WP-04 | migration 成功；FundFlow.status 字段可读写；前端流水列表状态列正确（待核销/已核销） |

## 九、Rollback Boundary

| WP | 回滚方式 | 数据影响 |
|---|---|---|
| W0-WP-01 | 前端单文件回退 | 无数据变更 |
| W0-WP-02 | 移除 @AuditLog 注解 | 无数据变更（审计日志不删除） |
| W0-WP-03 | 恢复 POST /refresh 端点 | 无数据变更 |
| W0-WP-04 | ALTER TABLE DROP COLUMN + 实体回退 | 新增 status 列数据删除（低风险，新字段无历史数据） |

---

## 十、最终分类结论

| WP | 分类 | 说明 |
|---|---|---|
| **W0-WP-01** | **A. Engineering-Ready** | 若已修复=验证通过；若未修复=纯前端修复 |
| **W0-WP-02** | **A. Engineering-Ready** | 无前置依赖，可立即执行 |
| **W0-WP-03** | **A. Engineering-Ready**（P0） | 探索确认低风险；无阻塞 |
| **W0-WP-04** | **A. Engineering-Ready** | 不依赖 W0-WP-03；可独立执行 |
| stores 清理 | **D. Deferred** → Wave 5 | 旧表仍有大量活跃读写，不可 Wave 0 冻结 |
| 小程序转换 | **C. Reclassify → Wave 4** | 纯前端展示层，不涉及 Truth Source |
| mock 移除 | **C. Reclassify → Wave 4** | 体验改善，不涉及 Truth Source |

---

## 十一、与 SFR-001 初版差异

| 项 | SFR-001 初版 | W0-FEP 最终版 | 变更理由 |
|---|---|---|---|
| stores 清理 | Wave 0 | **→ Wave 5** | 探索确认：旧表仍有大量活跃读写（StoreServiceImpl CRUD + DataPermissionServiceImpl + ReceiptConfirmationServiceImpl + SystemInitServiceImpl），不可冻结 |
| 小程序转换 | Wave 0 | **→ Wave 4** | 纯前端展示层，不涉及 Truth Source |
| mock 移除 | Wave 0 | **→ Wave 4** | 体验改善，不涉及 Truth Source |
| 凭证映射 | Wave 0（P1 UI 层） | **Wave 0（语义一致性验证）** | 探索确认：前后端已对齐 0-3；降级为验证通过/若未修复则纯前端修复 |
| W0-WP-03/04 关系 | 隐含依赖 | **明确独立** | 探索确认：fund_flows status 与 account_balance 冻结无数据依赖；batch C 是金额单位问题（非 status 问题），不可混淆 |
| 发布审计 | 未单独列出 | **合入 W0-WP-02** | 探索确认：FoodServiceImpl 52 处 @AuditLog 中未使用；AuditLogAspect=@annotation 模式——Domain/API 层缺口 |

---

*本 W0-FEP 为 Wave 0 最终工程计划，等待产品负责人确认。暂不进入 Developer 实施。*
*文档生成：架构总控（会话1）· 2026-09-08 · 基于代码探索验证（6 项事实核验）*
