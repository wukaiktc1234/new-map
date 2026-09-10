# System Fact Review 001（产品评审与整改启动裁决）

> - 文档编号：SFR-001
> - 日期：2026-09-08
> - 基于：SYSTEM-FACT-MAP-002（4 份 Fact Map）
> - 角色：架构总控（会话1）
> - **阶段声明**：本阶段**不做开发、不做 UI 重构、不新增业务规则、不创建大量 Developer 任务**。目标=确认 Fact Map 为 Canonical AS-IS，完成冲突分流与整改波次定义。
> - **冻结原则**：历史审计 = Candidate Evidence；**Fact Map = Current AS-IS Truth**；Product Decision = To-Be Business Rule；Design = To-Be Solution；Code = Implementation。

---

## 1. Fact Map Acceptance（事实底座冻结）

### 1.1 四份文档冻结为 Canonical AS-IS

| 文件 | 角色 | 冻结状态 |
|---|---|---|
| `system-fact-map-v1.md`（468 行，12 节） | **唯一事实底座**——所有后续设计/整改/决策以此为据 | **✅ FROZEN** |
| `system-conflict-map.md`（64 行） | 冲突分流源（21 冲突点） | **✅ FROZEN** |
| `system-truth-source-map.md`（57 行） | 真相源裁决记录 | **✅ FROZEN** |
| `system-remediation-dependency-map.md`（94 行） | 整改依赖链 | **✅ FROZEN** |

### 1.2 事实底座权威性声明

- 本 Fact Map 覆盖范围：V2（1093 行全系统逆向建模）+ 四域审查（产品/库存/订单/财务）+ PADR（5 项顶层决策）+ CDDR（跨域责任矩阵）+ 跨域冲突汇总（12 点）+ 产品决策池（PD-001~039）+ 历史审计（ETM/KL/AUDIT/BFA/OBS）。
- **任何新审计/新发现必须先匹配 Fact Map**（§8 Issue Lifecycle）；不得因换了页面/Agent/审计编号就创建独立问题。
- Fact Map 更新需走正式变更流程（审批 + 版本号递增）。

---

## 2. Truth Source Acceptance（真相源裁决确认）

> 以下真相源裁决**已由产品负责人确认（PADR-001~005 + PD-022~032）**，不可工程自行变更。

| 对象 | 真相源裁决 | 决策编号 | 状态 |
|---|---|---|---|
| 菜品 | **foods**（food 旧表=LEGACY） | PADR §九-A | ✅ 已确认 |
| 物料 | **material_archives**（product 表=LEGACY） | PADR §九-A | ✅ 已确认 |
| 库存 | **inventory（仓）+ store_inventory（店）**=物料×位置双维度 | 库存域设计 | ✅ 已确认 |
| 订单 | **orders**（orders_legacy=LEGACY；sales_order=虚实体） | PD-015 两阶段 | ✅ 已确认（阶段一） |
| 凭证 | **finance_vouchers**（voucher_header=死体系；根包 stub=假链路） | FIN 审查 | ✅ 已确认 |
| 科目余额 | **accounting_subjects.balance**（分/自动）=管理口径真相；account_balance 冻结停用 | PD-031 | ✅ 已确认 |
| 金额 | **新写一律分**（tax_record/account_balance 元=待统一） | PD-032 + Batch0-方案2 | ✅ 已确认方向 |
| 计价方法 | **最新入库价**=当前口径（移动加权平均=Level 3 演进） | PD-032 | ✅ 已确认 |
| 资金流水 | fund_flows **增 status**（待核销/已核销）| PD-028 | ✅ 已确认（待落库） |
| 记账触发 | **支付成功=收款流水**（钱到账）；挂账/信用账=演进 | PD-029 | ✅ 已确认 |
| 扣减分层 | **菜品层=下单即扣；物料层=完成时按 BOM 扣** | PD-030 | ✅ 已确认方向 |

---

## 3. Conflict Classification（21 冲突最终分流）

> 每个 Conflict 进入唯一主责任归属：A=DECIDED / B=PRODUCT_DECISION_REQUIRED / C=TECHNICAL_REMEDIATION / D=LEGACY/DECOMMISSION / E=UNVERIFIED。

### 3.1 分流总表

| # | 冲突点 | 分流 | 责任层 | 可进入开发？ | 说明 |
|---|---|---|---|---|---|
| C-01 | 可售口径 | **A DECIDED** | Domain | ✅（设计约束） | PADR-003；可售=菜品层 stock，物料不参与 |
| C-02 | 记账口径 | **A DECIDED** | Domain | ✅（设计约束） | PADR-005；流水级真相，Level 3=演进 |
| C-03 | 成本口径 | **A DECIDED**（口径）+ **C 技术整改**（双写） | Domain+DB | 部分 | 口径分=已确认；双写=待确认（T-14） |
| C-04 | 订单成立 | **A DECIDED** | Domain | ✅（设计约束） | 成立=写行 status=0；三套状态=PD-015 |
| C-05 | 应付触发 | **A DECIDED** | Domain | ✅ | 同事务幂等；链路完整 |
| C-06 | 门店经营范围 | **A DECIDED**（概念）+ **B 待产品**（实现） | Domain | 部分 | PADR-002 已决概念；实现方案=演进待专项决策 |
| C-07 | 订单状态机 | **C 技术整改**（PD-015 阶段二） | Domain+DB | ⚠️ 依赖 PD-015 阶段二 | 三套→一套 |
| C-08 | 反向链空白 | **B PRODUCT_DECISION_REQUIRED** | Domain | ❌ 阻塞 | PD-033 待决（收款冲正/凭证红冲/取消退款） |
| C-09 | 销售凭证链路 | **D LEGACY/DECOMMISSION** | Domain | ❌（Level 3 演进，不进入当前） | PADR-005 Level 3=增强层 |
| C-10 | 采购凭证链路 | **D LEGACY/DECOMMISSION** | Domain | ❌（Level 3 演进） | 同 C-09 |
| C-11 | 发布审计 | **C TECHNICAL_REMEDIATION** | API | ✅ | 补 @AuditLog，无产品决策依赖 |
| C-12 | 金额单位双轨 | **C TECHNICAL_REMEDIATION** | DB+UI | ✅（分批：A/B/C/D） | Batch0-方案2 |
| C-13 | POS 双源 | **C TECHNICAL_REMEDIATION** | API+Domain | ⚠️ 依赖 G1-1/G1-2 核实 | Batch0-方案1 |
| C-14 | 凭证状态映射 | **C TECHNICAL_REMEDIATION** | UI | ✅（最高确定性） | Batch0-方案3 |
| C-15 | 科目余额双轨 | **A DECIDED** | DB | ✅（冻结 account_balance） | PD-031 |
| C-16 | 资金流水无 status | **C TECHNICAL_REMEDIATION** | DB | ⚠️ 依赖批 C 落库 | PD-028 |
| C-17 | 成本双写 | **B PRODUCT_DECISION_REQUIRED**（业务语义判定） | Domain | ❌ 阻塞（判定前） | 待确认：合理分层 or 合并 |
| C-18 | food+foods 双写 | **C TECHNICAL_REMEDIATION** | Domain+API | ⚠️ 依赖 G1 | Batch0-方案1 |
| C-19 | inventory+store_inventory 双写 | **A DECIDED**（双维度成立） | Domain | ✅（设计约束） | 同概念双维度，不合并表 |
| C-20 | stores 双表 | **C TECHNICAL_REMEDIATION** | DB | ✅ | V20260723_001 已重指 |
| C-21 | 科目余额双轨（重复 C-15） | **A DECIDED**（同 C-15） | DB | — | 去重 |

### 3.2 分流统计

| 分流 | 数量 | 说明 |
|---|---|---|
| **A DECIDED** | 8 | 可直接作为设计约束/开发依据 |
| **B PRODUCT_DECISION_REQUIRED** | 3 | 阻塞（C-08 反向链/C-17 成本双写/C-06 实现方案） |
| **C TECHNICAL_REMEDIATION** | 9 | 可进入 Engineering Backlog（部分有前置依赖） |
| **D LEGACY/DECOMMISSION** | 2 | 不进入普通修复（C-09/C-10 Level 3 演进） |
| **E UNVERIFIED** | 0 | 无未验证冲突（全部有证据裁决） |

---

## 4. Top 10 Critical Conflicts（逐项裁决）

### 4.1 orders vs orders_legacy

| 维度 | 结论 |
|---|---|
| **生产真相** | `orders`（新核心，分）为真相源；`orders_legacy`（元）=POS 历史遗留 |
| **产品决策** | PD-015 已决（阶段一：orders 为唯一真相源 + 字段映射表；阶段二：按批迁移） |
| **Legacy** | orders_legacy=LEGACY（POS 收银/扫码/支付/退款仍写此表） |
| **双写** | 下单双写 food+foods（C-18）；但 orders/legacy 不双写（各写各表） |
| **风险** | **P0**（orders_legacy 不迁移=金额双轨持续；POS 链路与管理端链路永久分裂） |
| **允许开发** | ⚠️ 阶段一已完成（映射表建立）；**阶段二（代码迁移）依赖 PD-015 阶段二执行批次** |

### 4.2 products / foods / food 多套产品事实

| 维度 | 结论 |
|---|---|
| **生产真相** | foods=销售菜品真相（PADR §九-A）；food=旧表 LEGACY；product=只读遗留 LEGACY |
| **产品决策** | PADR-002/003 已确认；D8 售罄与库存分离 |
| **Legacy** | food（POS 扫码消费）+ product（inventory FK）=LEGACY |
| **双写** | food+foods 双写（下单/退款/超时）=C-18 |
| **风险** | **P1**（POS 扫码链路仍在消费 food 旧表=可售口径不一致） |
| **允许开发** | ⚠️ G1 POS 双源统一（Batch0-方案1）依赖 G1-1/G1-2 核实后执行 |

### 4.3 inventory 多套 ledger / product_id / material_id 语义

| 维度 | 结论 |
|---|---|
| **生产真相** | inventory（仓）+ store_inventory（店）=物料×位置双维度；material_id=真相语义；product_id=遗留列名/别名穿透 |
| **产品决策** | 无专项决策（双维度成立已确认） |
| **Legacy** | product_id 列保留不删（ETM-106 收敛） |
| **双写** | 采购入库/调拨/收货按位置类型双写（C-19=已确认双维度成立） |
| **风险** | **P1**（ETM-106 列漂移未收敛=查询歧义） |
| **允许开发** | ✅ ETM-106 收敛为技术整改（C TECHNICAL_REMEDIATION） |

### 4.4 finance balance truth

| 维度 | 结论 |
|---|---|
| **生产真相** | accounting_subjects.balance（分/自动）=管理口径真相（PD-031） |
| **产品决策** | PD-031 已决（account_balance 冻结停用）；PD-028 已决（fund_flows 增 status） |
| **Legacy** | account_balance（元/人工）=冻结 |
| **双写** | accounting_subjects.balance（过账自动）vs account_balance（人工刷新）=双轨（PD-031 冻结后消除） |
| **风险** | **P0**（account_balance 冻结前=财务数据双源；冻结后风险消除） |
| **允许开发** | ✅ 冻结 account_balance 为技术整改（C TECHNICAL_REMEDIATION） |

### 4.5 voucher 双体系

| 维度 | 结论 |
|---|---|
| **生产真相** | finance_vouchers=新体系（活跃，3 通道）；voucher_header=死体系（无调用）；根包 stub=假链路 |
| **产品决策** | 无专项（死体系冻结停用已确认） |
| **Legacy** | voucher_header + 根包 FinanceVoucherService=LEGACY/DECOMMISSION |
| **双写** | 无双写（两体系独立，新体系不写旧表） |
| **风险** | **P2**（死体系不删除=代码膨胀，但不影响运行时） |
| **允许开发** | ✅ 冻结停用（不删除）；Level 3 凭证级=演进 |

### 4.6 stores / data scope

| 维度 | 结论 |
|---|---|
| **生产真相** | stores_new=新真相（V20260723_001 已重指）；stores=旧（遗留） |
| **产品决策** | 门店经营范围=演进（PADR-002）；数据范围=角色/门店正交 |
| **Legacy** | stores=旧表（遗留） |
| **双写** | 无（已重指 stores_new） |
| **风险** | **P2**（stores 旧表清理=低优先级） |
| **允许开发** | ✅ stores 旧表可后续清理 |

### 4.7 订单三套状态机

| 维度 | 结论 |
|---|---|
| **生产真相** | orders.order_status（0-6）+ payment_status=新核心；legacy status（0-7）=遗留；sales_order String=虚实体 |
| **产品决策** | PD-015（两阶段迁移）+ PD-017~021（状态语义待决） |
| **Legacy** | legacy status + sales_order String=LEGACY |
| **双写** | 无（各表各状态机） |
| **风险** | **P0**（三套并存=跨域事件/财务/前端状态映射持续混乱） |
| **允许开发** | ⚠️ 阶段二依赖 PD-017~021 产品决策 |

### 4.8 event chain 断链

| 维度 | 结论 |
|---|---|
| **生产真相** | 核心事件链（9 事件 VERIFIED）；6 个断链/空白（发布审计/createTableOrder 不发事件/取消未退款/销售凭证未接线/采购凭证未接线/收款冲正无端点） |
| **产品决策** | 反向链（C-08）=PD-033 待决；Level 3 凭证（C-09/C-10）=演进 |
| **Legacy** | 无（断链≠遗留，是功能缺口） |
| **双写** | 无 |
| **风险** | **P1**（发布审计缺口=可技术先行；反向链缺口=阻塞产品决策） |
| **允许开发** | 发布审计补 @AuditLog=✅ 可先行；反向链=❌ 阻塞 PD-033 |

### 4.9 migration coverage 缺口

| 维度 | 结论 |
|---|---|
| **生产真相** | ETM 107 条中 6 表无 migration 建表（inventory_category/unit/supplier_evaluations/purchase_plan_item/requisition_requests/travel_requests/invoice_reimbursement 3 表） |
| **产品决策** | 无专项（属于数据层技术债务） |
| **Legacy** | 部分（无 migration 表可能存在运行时创建） |
| **双写** | 无 |
| **风险** | **P1**（无 migration=部署不可重现；但代码中实体存在可能运行时 OK） |
| **允许开发** | ⚠️ 补 migration 为技术整改（C TECHNICAL_REMEDIATION），优先级低于核心链路 |

### 4.10 management frontend mock/fallback 是否代表真实能力缺失

| 维度 | 结论 |
|---|---|
| **生产真相** | 前端 mock/硬编码/假成功/空态占位多处存在（V2 §2 + AUDIT + INV §5/6） |
| **产品决策** | PD-011 已决（生产环境禁止演示/mock）；其余 mock=设计阶段临时方案 |
| **Legacy** | 部分 mock=应下线（PD-011）；部分=合理降级（无后端 API 的功能） |
| **双写** | 无 |
| **风险** | **P1**（mock 代表后端能力缺失的前端占位；需逐个确认哪些有后端 API 可接、哪些无） |
| **允许开发** | PD-011 相关 mock=✅ 可先行移除；其余 mock=⚠️ 需逐个确认 |

---

## 5. Product Decision Priority（产品决策优先级排序）

> 16 已决 / 7 待产品 / 2 待评估 → 分 P0/P1/P2。

### 5.1 P0：必须先裁决，否则核心整改无法开始

| PD | 决策项 | 阻塞什么 | 已有部分 |
|---|---|---|---|
| **PD-015** | 订单双表迁移（阶段二执行批次） | T-05（金额随表）、T-10（状态统一）、T-11（统一流水） | 阶段一完成（映射表建立） |
| **PD-017~021** | 订单状态语义（三套→一套的语义定义） | T-10（状态统一） | — |
| **PD-033** | 收款冲正实现方式 | 财务反向链闭环（C-08） | — |

### 5.2 P1：可在整改过程中裁决

| PD | 决策项 | 阻塞什么 | 已有部分 |
|---|---|---|---|
| PD-006 | 裸接口权限归属（25 方法） | 权限层验收 | — |
| PD-007 | 申诉域归属（5 方法） | 域归属确认 | — |
| PD-008 | 供应商 H5 外部认证 | 外部认证方案 | — |
| PD-009 | 通用审批权限模型 | 通用审批权限 | — |
| PD-010 | 退款审批身份归因 | approveUserId 校验 | — |
| PD-034 | 预算审批流归属 | FinanceBudget 审批动作 | — |
| **PD-035** | 导出规则（格式/上限/口径） | 统一导出服务 | — |
| **PD-036** | 账户余额真值方案 | T-04（部分） | ✅ 已决策（余额=派生） |
| C-17 | 成本双写业务语义 | T-14（合并/分层） | — |

### 5.3 P2：可后置，不阻塞 Management Core

| PD | 决策项 | 说明 |
|---|---|---|
| PD-005 | 会员统计 3 字段口径 | 不阻断发布（已降级 --） |
| PD-012 | 自动签名存证 | RM 域，BLOCKED |
| PD-013 | APK 分发模式 | RM 域，BLOCKED |
| PD-022 | 千店千面方向 | 已决（方向确认），实现=演进 |
| PD-024~027 | PADR 五项决策 | 已决，不影响当前整改 |

### 5.4 关键阻塞路径（P0 裁决链）

```
PD-015 阶段二执行 → 解锁 T-05/T-10/T-11 → 解锁金额统一/状态统一/统一流水
PD-017~021 状态语义 → 解锁 T-10 状态机统一
PD-033 收款冲正 → 解锁财务反向链闭环
```

**结论：PD-015 阶段二 + PD-017~021 是当前最高优先级产品决策——不裁决则核心数据整改无法开始。**

---

## 6. Remediation Wave Priority（整改波次定义）

> 原则：后一个 Wave 不得绕过前一个 Wave 的核心依赖。每波独立交付、独立验证。

### Wave 0：Truth / Source-of-Truth（真相源确立）

**目标**：冻结真相源、消除数据双源。

| 整改项 | 内容 | 依赖 | 状态 |
|---|---|---|---|
| C-15/§10.8 | account_balance 冻结停用 | PD-031 已决 | ✅ 可立即执行 |
| C-16/§10.7 | fund_flows 增 status 字段 | PD-028 已决；同批 C | ✅ 可立即执行（同 T-07） |
| C-14/§10.6 | 凭证状态映射对齐前端 0-3 | 无 | ✅ 可立即执行（T-06） |
| C-11/§10.13 | 发布动作补 @AuditLog | 无 | ✅ 可立即执行（T-13） |
| C-20/§10.20 | stores 旧表清理（重指后删除） | 低风险 | ✅ 可执行 |

### Wave 1：Core Data / State / Event（核心数据/状态/事件统一）

**目标**：订单双表迁移、状态机统一、金额统一。

| 整改项 | 内容 | 依赖 | 状态 |
|---|---|---|---|
| T-10 | 订单状态机统一（三套→一套） | **PD-015 阶段二 + PD-017~021** | ⚠️ 阻塞 |
| T-05 | orders_legacy 金额随迁移（元→分） | **PD-015 阶段二** + T-04 部分 | ⚠️ 阻塞 |
| T-11 | 统一流水模型（transactions 只写不读修复） | T-10（状态统一后） | ⚠️ 依赖 T-10 |
| T-04 | tax_record/account_balance 元口径统一 | T-07（fund_flows status）+ 财务口径 | ⚠️ 部分依赖 |
| T-17 | 锁定量完整策略 | 产品决策联动扣减时机 | ⚠️ 待产品决策 |

### Wave 2：Permission / Data Scope（权限/数据范围）

**目标**：权限缺口闭环、数据范围清晰化。

| 整改项 | 内容 | 依赖 | 状态 |
|---|---|---|---|
| PD-006 | 裸接口权限归属（25 方法） | PD-006 产品决策 | ⚠️ 待产品 |
| PD-007 | 申诉域归属（5 方法） | PD-007 产品决策 | ⚠️ 待产品 |
| PD-008 | 供应商 H5 外部认证 | PD-008 产品决策 | ⚠️ 待产品 |
| PD-009 | 通用审批权限模型 | PD-009 产品决策 | ⚠️ 待产品 |
| PD-010 | 退款审批身份归因 | PD-010 产品决策 | ⚠️ 待产品 |

### Wave 3：Management API（管理端 API）

**目标**：API 链路完整、零消费 API 处理。

| 整改项 | 内容 | 依赖 | 状态 |
|---|---|---|---|
| T-01 | POS 双源统一（food→foods） | G1-1/G1-2 核实 | ⚠️ 待核实 |
| T-16 | 收货/入库双入口职责收敛 | 产品决策（职责） | ⚠️ 待产品 |
| T-14 | 成本双写确认/合并 | 产品/财务判定 | ⚠️ 待确认 |
| T-15 | 盘点差异自动落地 | PD-030 方向已决 | ✅ 可执行 |

### Wave 4：Management Frontend（管理端前端）

**目标**：UI 专业调整、mock 移除、体验一致性。

| 整改项 | 内容 | 依赖 | 状态 |
|---|---|---|---|
| T-02 | 小程序金额转换 | 无 | ✅ 可执行 |
| T-03 | finance_record 口径确认 | 无 | ✅ 可执行 |
| T-09 | 计价方法标注（管理口径） | 无 | ✅ 可执行 |
| UI 批次 | P1-UI-FIN-001~004（财务 4 页专业调整） | 无 | ✅ 已在任务池 |
| PD-011 | 移除生产环境 mock/假数据 | PD-011 已决 | ✅ 可执行 |

### Wave 5：Legacy / Client Integration（遗留/客户端集成）

**目标**：遗留表清理、客户端一致性。

| 整改项 | 内容 | 依赖 | 状态 |
|---|---|---|---|
| food 旧表冻结/清理 | G1 完成后 | T-01 完成 | ⚠️ 依赖 T-01 |
| orders_legacy 清理 | PD-015 阶段二完成后 | T-05 完成 | ⚠️ 依赖 T-05 |
| voucher_header 清理 | 冻结停用（不删除） | 无 | ✅ 已冻结 |
| 根包 stub 清理 | 停止调用 | 无 | ✅ 可执行 |
| stores 旧表删除 | 重指后 | Wave 0 | ✅ 可执行 |

### 波次依赖拓扑

```
Wave 0 (Truth)  →  Wave 1 (Data/State)  →  Wave 2 (Permission)
                                              ↓
                                         Wave 3 (API)
                                              ↓
                                         Wave 4 (Frontend)
                                              ↓
                                         Wave 5 (Legacy)
```

---

## 7. Blocked Items（阻塞项清单）

| 编号 | 阻塞项 | 阻塞来源 | 影响范围 | 优先级 |
|---|---|---|---|---|
| B-01 | PD-015 阶段二执行批次 | 产品决策未裁决执行节奏 | T-05/T-10/T-11/Wave 1 全部 | **P0** |
| B-02 | PD-017~021 订单状态语义 | 产品决策未裁决 | T-10 状态统一 | **P0** |
| B-03 | PD-033 收款冲正 | 产品决策未裁决 | 财务反向链闭环 | **P0** |
| B-04 | G1-1 food 表批次/溯源字段消费盘点 | 开发前核实未完成 | T-01 POS 双源 | P1 |
| B-05 | G1-2 foodMapper 全量消费点 grep | 开发前核实未完成 | T-01 POS 双源 | P1 |
| B-06 | 成本双写业务语义判定 | 产品/财务未确认 | T-14 | P1 |
| B-07 | PD-006~010 权限缺口 | 产品决策未裁决 | Wave 2 全部 | P1 |
| B-08 | 收货/入库双入口职责决策 | 产品决策未裁决 | T-16 | P1 |

---

## 8. Engineering-Ready Items（工程就绪项——可立即进入开发）

| 编号 | 项目 | Wave | 验收标准 |
|---|---|---|---|
| T-06 | 凭证状态映射对齐（前端 0-3） | Wave 0 | converters.ts VoucherStatusMap 对齐；按钮可用性正确 |
| T-13 | 发布动作补 @AuditLog | Wave 0 | 发布方法标注 @AuditLog；审计日志可查 |
| account_balance 冻结 | PD-031 冻结停用 | Wave 0 | account_balance 表停止写入；页面标注管理台账口径 |
| fund_flows 增 status | PD-028 + 批 C | Wave 0 | fund_flows.status 字段存在；页面展示正确 |
| T-02 | 小程序金额转换 | Wave 4 | 下单/支付传元→后端转分；列表分→元展示 |
| T-09 | 计价方法标注 | Wave 4 | 成本分析页标注「管理口径：最新入库价」 |
| PD-011 相关 mock 移除 | Wave 4 | 生产环境无演示登录/假 openid/支付 mock |

---

## 9. Deferred Items（后置项——不阻塞 Management Core）

| 编号 | 项目 | 说明 |
|---|---|---|
| C-09/C-10 | Level 3 凭证级（销售/采购凭证链路） | PADR-005 Level 3 演进；进入大型连锁阶段复评 |
| PD-005 | 会员统计 3 字段口径 | 不阻断发布（已降级 --） |
| PD-012/013 | RM 域（签名存证/APK 分发） | BLOCKED，不纳入当前整改 |
| stores 旧表删除 | 低风险清理 | Wave 5 或更后 |
| voucher_header 删除 | 冻结停用即可（不删除） | 永久冻结 |
| inventory_category/unit 补 migration | ETM-033/034 | 低优先级 |
| 迁移覆盖缺口补 migration（6 表） | ETM 家族 | P1，可 Wave 3 后执行 |

---

## 10. Issue Lifecycle 防重复规则

> 以后任何新 Audit 发现的处理流程：

### 10.1 匹配规则（先匹配，不新建）

```
新发现 → 匹配 Fact Map（system-fact-map-v1.md）
         ├─ 已有事实 → 标记 DUPLICATE（引用现有编号）
         ├─ 已有冲突 → 标记 CONFLICT（引用冲突编号 C-xx）
         ├─ 已有决策 → 标记 RESOLVED（引用 PD 编号）
         ├─ 已有遗留 → 标记 LEGACY（引用遗留对象）
         ├─ 需验证 → 标记 UNVERIFIED（补充证据后更新 Fact Map）
         └─ 真新问题 → 标记 NEW（更新 Fact Map + Conflict Map + 决策池）
```

### 10.2 允许的状态

| 状态 | 含义 |
|---|---|
| **NEW** | 真新问题（匹配后确认无重复） |
| **DUPLICATE** | 与已有事实/冲突/决策重复（必须引用原编号） |
| **REGRESSION** | 已修复问题复现（引用原任务卡） |
| **RESOLVED** | 已通过产品决策/技术整改/审计确认解决 |
| **LEGACY** | 历史遗留（冻结/停用/标注） |
| **UNVERIFIED** | 需补充证据（代码/活体/DB 核实） |
| **CONFLICT** | 与已有事实矛盾（需裁决） |

### 10.3 禁止

- ❌ 不得因换了页面/Agent/审计编号就创建新的独立问题
- ❌ 不得不匹配 Fact Map 就新建问题
- ❌ 不得同时存在两个主责任归属（每问题唯一归属）

---

## 11. Explicit Non-Goals（明确非目标）

本阶段（SFR-001）**不做**：

| 非目标 | 说明 |
|---|---|
| ❌ 修改代码 | 本阶段只归并/裁决/规划 |
| ❌ 修改数据库 | 本阶段不执行任何 migration |
| ❌ UI 重构 | 本阶段不设计新 UI（V3-A 为验证工具） |
| ❌ 新增业务规则 | 本阶段不定义新的产品规则 |
| ❌ 创建大量 Developer 任务 | 本阶段只定义 Wave，不拆具体开发卡 |
| ❌ 确定开发排期 | Wave 顺序≠排期（需 planner 拆卡后确定） |
| ❌ 确认所有产品决策 | P0/P1/P2 分级裁决，非一次性全部 |

---

## 12. 裁决待产品负责人确认

本 Review 已完成分析与分流，等待产品负责人最终确认：

1. **Fact Map 冻结**：4 份文档是否可正式成为 Canonical AS-IS？
2. **P0 决策优先级**：PD-015 阶段二 + PD-017~021 是否为最高优先级裁决？
3. **Wave 顺序**：Wave 0→1→2→3→4→5 是否合理？
4. **Blocked 项**：B-01~B-08 是否需要调整优先级？
5. **Non-Goals**：是否有遗漏的非目标需要明确？

---

*本 SFR-001 为产品评审与整改启动裁决文档，等待产品负责人最终确认。*
*文档生成：架构总控（会话1）· 2026-09-08 · 基于 SYSTEM-FACT-MAP-002（4 份 Fact Map）*
