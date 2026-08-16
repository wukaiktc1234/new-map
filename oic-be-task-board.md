# OIC-BE 任务池（后端整改池治理轨 — Backend Remediation）

> **阶段定位**：后端整改池治理轨。后端窗口解锁（管理端 Batch 2 observe 收口 PASS，2026-08-16 12:55）→ OIC-BE ACTIVE（用户 2026-08-16 指令，`remediation-roadmap.md` §5.5/§5.6 关联）。
> **定位**：OIC-BE =「后端整改池轨」，承接 **Entity-Table Consistency Remediation 家族专项**（DEF-3 / KL-050/051 / KL-054 / KL-055 / ETM-106）与后端池联动项（EMP-BE-004/005、KIT-BE-004 等）。**ETM-106（inventory）已在 RM-B2 轨，不重复排**。
> **治理输入**：`docs/quality/entity-table-mapping-audit.md`（ETM 清单，A 类 93 项）+ `production-known-limitations.md`（KL-054/055 家族登记）+ `docs/quality/rm-b2-readiness-check.md`（家族治理状态）。
> **维护者**：planner（会话3）。**注意**：本文件创建时（2026-08-16）opencode 会话配置尚未热加载 `oic-be-task-board.md` 白名单，由 architect 代为落盘；重启 opencode 后 planner 正常维护。
> **节奏（用户指令）**：每批 2~3 卡：developer → qa → 部署 → observe。

---

## 〇、OIC-BE 红线（全程有效，每张卡必读）

| # | 红线 |
|---|---|
| R-1 | **执行门禁（规则 6，强制）**：后端卡必须 **实体-表三向核对**（实体 @TableName/字段 ↔ migration ↔ 真实表 information_schema 只读实测）→ **migration 先行**（先建表/先对齐，后改码）→ **REV：后端改动必须更新/新增 regression 基线**（基线条目由 regression 角色维护，卡面标注 REG-XXX 要求） |
| R-2 | **分批迁移，不全仓一次性改**（家族专项治理原则）；每批 2~3 卡 |
| R-3 | **文件归属隔离（RM 轨）**：不触碰 RM 轨专属文件——`ReceiptConfirmationServiceImpl.java`（已提交 4fc4f64）/ `InventoryServiceImpl.java` / `entity/Inventory.java` / inventory 相关 migration（V20260816_001，提交 852b53f）等 |
| R-4 | **不猜测业务规则/契约**：表结构归属、同表合并等决策点 → 登记待裁决（架构/契约确认），不猜测实现；与规则无关部分（catch 失败透传、错误明确化）允许先行 |
| R-5 | **ETM-106 不重复排**（RM-B2 轨已含 RM-B1-001 + 实施卡 1/2/3） |
| R-6 | 标准流程：developer → qa 独立验收 → 部署 → observe；开发不得自验，QA 不改代码 |
| R-7 | 新 migration 版本号从 **V20260816_002** 起（避让 RM 轨 V20260816_001）；不创建 Sprint 编号、不进 Release Gate（OIC 轨口径） |
| R-8 | **禁止新增 sales_order 引用（PD-015 阶段一裁决，2026-08-16 生效）**：后续开发不得新增 sales_order/sales_order_detail 表名、SalesOrder/SalesOrderDetail 实体、SalesOrderMapper 系列引用（@TableName/硬编码 SQL/Mapper 方法/QueryWrapper 列名/前端数据源注释/测试/脚本）；**新增订单相关代码一律走 orders/order_items（OrderNew/OrderItemNew 体系）**；存量引用只减不增（随阶段二按批迁移收敛，`docs/quality/sales-order-orders-mapping.md` §五清单）；违反 = QA 直接 FAIL |

---

## 一、批次状态

| 批次 | 范围 | 状态 | 说明 |
|---|---|---|---|
| **Batch 1** | **OICBE-B1-001（ETM-001）+ OICBE-B1-002（KL-054）+ OICBE-B1-003（KL-055）** | **QA PASS 3/3（2026-08-16）** | 三卡全 PASS、无 FAIL、无 -R1（`docs/quality/oicbe-b1-qa-report.md`；卡 3 附限制 L-1）；**可进入部署流程**；PD-014/015 待架构/契约确认、PD-016 BLOCKED（决策前不落库）；migration 均未落库；commit 72f0d5a（4 文件，无新增 migration、无 RM 轨触碰）；勘误已确认（ETM-001 硬编码 SQL 在 Mapper.java 注解，XML 为空文件）；REV：REG-ETM-001/002/003、REG-KL055 转正由 regression 执行 |
| Batch 2+（后续候选） | 家族专项其余项（ETM-002~093 A 类）、后端池联动项（EMP-BE-004/005、KIT-BE-004 等） | 占位（未排卡） | 由架构/planner 按序放卡，每批 2~3 卡；同源只登记不顺手修 |

---

## 二、流程（每批强制，developer → qa → 部署 → observe）

1. **planner** 排卡（编号 `OICBE-B{n}-xxx`；规则 6 门禁 + REV 写入卡面）
2. **developer** 按卡实现：三向核对（只读）→ migration 先行 → 改码 → 提交证据（diff + 落库记录 + 冒烟 + REV 基线材料）
3. **qa** 独立验收（三向一致断言 + migration 落库 + 链路冒烟 + REG 条目），输出 PASS/FAIL/PASS_WITH_LIMITATION 至 `docs/quality/`
4. **部署**：验收通过后由发布负责人执行（后端部署窗口已解锁）
5. **observe**：部署后观察；**回归基线更新（REV）**：由 regression 角色将新基线（REG-ETM-001/002/003、REG-KL055 等）转正至 `production-regression-test.md`

---

## 三、Batch 1 任务卡（2026-08-16 排卡）

---

### 任务卡 1/3：OICBE-B1-001

| 字段 | 内容 |
|---|---|
| **编号** | `OICBE-B1-001`（QA FAIL 复验时为 `OICBE-B1-001-R{n}`） |
| **优先级** | **P0**（ETM-001：registration_code 无建表无 catch，入职注册码链路缺表 500） |
| **来源审计项** | **ETM-001**（`docs/quality/entity-table-mapping-audit.md` §2.1.1，A 类唯一 P0）：`entity/RegistrationCode.java` @TableName("registration_code") 无 CREATE TABLE；V20260625_004 将 registration_code 改为 `onboarding_records` 的列（L710/742）；`RegistrationCodeMapper.xml` L19/25/31/37 硬编码 SELECT/UPDATE registration_code；`RegistrationCodeServiceImpl` L340/379 catch 后直接 throw BusinessException（无兜底）→ **缺表 500**；`OnboardingRecordServiceImpl` L95-104 相关 |
| **文件** | `RegistrationCode.java` / `RegistrationCodeMapper.xml` / `RegistrationCodeServiceImpl.java` / `OnboardingRecordServiceImpl.java` + 新 migration（≥V20260816_002） |
| **现状** | registration_code 表不存在（仅 onboarding_records 列形态）；注册码生成/校验/核销链路缺表即 500；ServiceImpl 无 catch 兜底（错误直接抛） |
| **风险** | 入职注册码核心链路不可用；500 无用户可理解反馈 |
| **修复目标** | ① 三向核对（RegistrationCode 实体 ↔ migration 现状 ↔ 真实表）→ **契约决策点：registration_code 独立建表 vs 对齐 onboarding_records 列**（**待裁决 PD-014，不猜测**）；② migration 先行（建表或对齐后落库）；③ **catch 失败透传可先行**（与规则无关部分：错误明确化，不吞错、不假成功）；④ 链路冒烟 |
| **执行方式** | 规则 6 门禁：三向核对（只读）→ 决策点登记待裁决（不猜测，catch 先行不受阻）→ migration 先行 → 代码对齐 → 冒烟 |
| **验收标准（qa 独立验收）** | ① 三向核对单产出（实体↔migration↔information_schema）；② **migration 落库**（**PD-014 已裁决（2026-08-16）：代码引用优先建表**——建 registration_code 表（含 code_expiry_time 列）+ onboarding_records 补 code_expiry_time 列，flyway 新版本 success=t、checksum 匹配）；③ 注册码**生成/校验/核销链路真实数据冒烟**（业务成功路径可用，不再 500）；④ 无 RM 轨文件触碰；⑤ **REV：REG-ETM-001 基线条目转正 + 真实数据断言扩展**（regression 维护） |
| **红线** | R-1~R-7（决策点不猜测；catch 语义先行；分批） |
| **状态** | **QA PASS（2026-08-16，`docs/quality/oicbe-b1-qa-report.md` §一）**：勘误真实（硬编码 SQL 在 Mapper.java 注解，XML 为空文件）；code_expiry_time 列缺失新发现独立证实（第一失败点判断正确）；catch 先行结论成立（L340/379 为业务校验 throw，非 catch 吞错，无需改码）；migration 未落库合规（flyway 189/189 无新版本）；无 RM 轨文件触碰；REV：REG-ETM-001 转正由 regression 执行 |
| **本批执行证据（developer，2026-08-16）** | 见下方「Batch 1 执行证据」OICBE-B1-001 |

---

### 任务卡 2/3：OICBE-B1-002

| 字段 | 内容 |
|---|---|
| **编号** | `OICBE-B1-002`（QA FAIL 复验时为 `OICBE-B1-002-R{n}`） |
| **优先级** | **P1**（KL-054 家族：sales_order 漂移） |
| **来源审计项** | **KL-054 / ETM-002 / ETM-003**：`SalesOrder.java` @TableName("sales_order") 无建表，真实业务表为 `orders`（`OrderNew.java` 映射）；`SalesOrderMapper.xml` L28 硬编码 FROM sales_order（L52/56/61 sales_order_detail）；`DashboardServiceImpl.java` L96 查询 sales_order → **BadSqlGrammarException**（catch 吞掉致今日销售概览空） |
| **文件** | `SalesOrder.java` / `SalesOrderDetail.java` / `SalesOrderMapper.xml` / `DashboardServiceImpl.java` + 新 migration（≥V20260816_002） |
| **现状** | sales_order/sales_order_detail 表不存在（真实表 orders）；Dashboard 销售概览 catch 降级为空；SalesOrderMapper 硬编码 SQL 全部 500 |
| **风险** | dashboard 今日销售概览失真（假空）；销售订单域接口 500（KL-054 家族） |
| **修复目标** | ① 三向核对（SalesOrder ↔ migration ↔ orders 真实表）→ **契约决策点：SalesOrder 与 OrderNew 是否同表合并（待裁决 PD-015，不猜测）**；② migration 先行（对齐后落库）；③ Dashboard 查询修复（BadSqlGrammarException catch 处置：真实数据/明确错误态，不吞错）；④ 链路冒烟 |
| **执行方式** | 规则 6 门禁：三向核对 → 决策点登记待裁决（不猜测）→ migration 先行 → 代码对齐 → 冒烟 |
| **验收标准（qa 独立验收）** | **阶段一（本卡验收）**：① orders 唯一生产真相源确认书（信息核对：orders 51 列双结构、SalesOrder 8 列无表等事实）；② **sales_order↔orders 字段映射表**（19 字段逐列映射：列名/类型/状态机/明细归属，含命名漂移与类型漂移标注）；③ **禁止新增 sales_order 引用**（现状引用扫描清单 + 声明）；④ 阶段二批次计划（实体/查询/接口/报表/统计分批，每批 QA+基线）；⑤ catch 先行改码保持（dashboard 明确错误态无假空）；⑥ **REV：REG-ETM-002/003 维持明确错误态断言（阶段二批次迁移后扩展真实数据断言）** |
| **红线** | R-1~R-8（同表合并决策不猜测；分批；禁止新增 sales_order 引用） |
| **状态** | **QA PASS（2026-08-16，`docs/quality/oicbe-b1-qa-report.md` §二）**：orders 51 列双结构、SalesOrder-only 8 列无表独立实测一致；catch 先行改码三方验证（git diff + 运行时日志 BadSqlGrammarException@L97 + live 冒烟 code:500，不再假空）；未补业务数据来源（不改业务规则）；migration 未落库合规（PD-015 待裁决）；观察：总览 overview{1,2} 随销售子项 500（明确错误态优先，接受）；REV：REG-ETM-002/003 转正由 regression 执行 |
| **本批执行证据（developer，2026-08-16）** | 见下方「Batch 1 执行证据」OICBE-B1-002 |
| **阶段一产出证据（developer，2026-08-16）** | **PD-015 阶段一已执行（只读核对 + 文档产出，无代码/无表变更）**：① **orders 唯一生产真相源确认书**（`docs/quality/sales-order-orders-mapping.md` §一：sales_order/sales_order_detail 生产表不存在、orders 51 列双结构全清单实测、OrderNew 33/33 列 0 缺失、flyway 仍为 20260816.001、orders 1 行/order_items 0 行；边界事实：orders_legacy 32 列/order_items_legacy 12 列为 POS 旧链路独立表，不在合并面）；② **sales_order↔orders 字段映射表**（§二/§三：19 字段逐列映射——可直映 4 / 需语义转换 7 / **表无对应列 8**（status/order_time/estimated_time/completed_time/table_no/people_count/created_by/updated_by，其中 7 列有语义候选、completed_time 无候选）；状态机 'completed' vs 2、主键 Long AUTO vs String、金额 Integer vs bigint/numeric 三大漂移点；SalesOrderDetail 13 字段 ↔ order_items 归属核对，明细归属待契约）；③ **禁止新增 sales_order 引用**（§五全仓引用扫描清单：主代码 9 文件 + 前端注释 1 + 脚本/资源 3 + 治理文档约 19 处；前端零运行时消费、报表/统计面零 sales_order 引用；声明登记 §六 + 红线 R-8）；④ **阶段二批次计划**（§七 + 本板 §四·2：Batch 2 实体+查询 / Batch 3 接口 / Batch 4 报表统计+清理，每批 QA+基线，**不执行**，等 planner 放卡+契约确认）；⑤ catch 先行改码保持（dashboard 明确错误态无假空）✓；⑥ REV：REG-ETM-002/003 维持明确错误态断言 ✓；新发现观察：order_items.food_id varchar vs OrderItemNew.foodId Long 既存类型漂移（非本卡范围，登记观察） |

---

### 任务卡 3/3：OICBE-B1-003

| 字段 | 内容 |
|---|---|
| **编号** | `OICBE-B1-003`（QA FAIL 复验时为 `OICBE-B1-003-R{n}`） |
| **优先级** | **P1**（KL-055：budgets 表结构漂移） |
| **来源审计项** | **KL-055**（`production-known-limitations.md`，来源 O-20260815-02）：budgets 旧采购预算结构（V20260704_001 建表）vs `finance/Budget` 实体 → HTTP 200 + code:500「系统繁忙」+ 分页 COUNT 短路假空列表 |
| **文件** | 按 KL-055 明细：budgets 相关实体 / migration（V20260704_001 等）/ Service + 新 migration（≥V20260816_002） |
| **现状** | budgets 表结构与 finance/Budget 实体不一致 → 接口 200 + code:500 + COUNT 短路假空列表（假空形态） |
| **风险** | 财务预算域假空列表（数据不真实）；HTTP 200 携带业务 500 的误导性契约形态 |
| **修复目标** | ① 读 KL-055 明细 → 三向核对（实体 ↔ migration ↔ 真实表）→ migration 对齐（**涉业务结构取舍时登记 BLOCKED 待裁决，不猜测**；纯 schema 事实对齐可直接执行）；② 假空形态处置（COUNT 短路移除/明确错误态）；③ 链路冒烟 |
| **执行方式** | 规则 6 门禁：三向核对 →（决策点登记）→ migration 先行 → 代码对齐 → 冒烟 |
| **验收标准（qa 独立验收）** | **PD-016 已裁决（2026-08-16）：migration 为真相源，三步收敛**——① 实体字段差异清单（实体-only 9 列 + 表-only 9 列 + budget_id 类型漂移）；② 影响接口扫描（/v1/finance/budgets 及关联面）；③ **migration 收敛对齐落库**（补实体 9 列 + budget_id 类型对齐；**不直接删字段**，剩余差异表落任务池；flyway 新版本 success=t、checksum 匹配）；④ budgets 链路**真实数据冒烟**（无假成功、无 COUNT 短路假空）；⑤ **REV：REG-KL055 基线条目转正 + 真实数据断言扩展** |
| **红线** | R-1~R-7（业务结构取舍不猜测） |
| **状态** | **QA PASS（2026-08-16，`docs/quality/oicbe-b1-qa-report.md` §三，附限制 L-1）**：budgets 15 列=migration、实体 9 列漂移、budget_id VARCHAR(32) vs Long 类型漂移独立实测一致；结构护栏触发有日志铁证（traceId=6b5600cf1885 与冒烟响应对账闭环）；PD-016 BLOCKED 不落库合规；L-1：护栏抛 finance 包 BusinessException 未被 common 包 GlobalExceptionHandler 捕获 → 响应为通用「系统繁忙」（仍属明确错误态，不阻塞；finance 域双包异常类并存建议评估）；REV：REG-KL055 转正由 regression 执行 |
| **本批执行证据（developer，2026-08-16）** | 见下方「Batch 1 执行证据」OICBE-B1-003 |

---

## 四、待裁决点登记（BLOCKED，不猜测，不占通过数）

| 编号 | 来源卡 | 决策点 | 状态 | 先行部分 |
|---|---|---|---|---|
| PD-014 | OICBE-B1-001 | registration_code 归属：独立建表 vs 对齐 onboarding_records 列 | **✅ 已决策（2026-08-16 架构裁决）：代码引用优先建表**（建 registration_code 表含 code_expiry_time + onboarding_records 补列，不删代码）——立即落库 | catch 先行已确认无需改码（现状即明确错误态） |
| PD-015 | OICBE-B1-002 | SalesOrder 与 OrderNew 是否同表合并（sales_order vs orders） | **✅ 已决策（2026-08-16 架构裁决）：两阶段**——阶段一=orders 唯一真相源 + 映射表 + 禁止新增引用（立即）；阶段二=按批迁移（实体/查询/接口/报表/统计，每批 QA+基线） | Dashboard catch 吞错处置已完成（明确错误态替代假空） |
| PD-016 | OICBE-B1-003 | budgets 对齐方向：旧采购预算结构 vs 财务预算实体 | **✅ 已决策（2026-08-16 架构裁决）：migration 为真相源，三步收敛**（差异清单→接口扫描→migration 收敛对齐；不删字段，差异表落任务池）——立即落库 | 假空形态处置已完成（getPage 结构护栏 → 明确错误态） |

> 决策流转：架构/契约确认 → 回写验收标准 → 任务放行（不猜测实现）。

---

## 四·1、Batch 1 执行证据（developer 2026-08-16，OICBE-B1-001/002/003）

### OICBE-B1-001（ETM-001，registration_code）

**三向核对单（规则 6）**：

| 维度 | 证据 |
|---|---|
| 实体 | `RegistrationCode.java` @TableName("registration_code")，字段：id(String ASSIGN_UUID)/code/type/validity_start/validity_end/status/created_by/onboarding_record_id/create_time/update_time/use_time |
| migration | 全仓无 registration_code 建表；V20260625_004 L710/711 将 registration_code VARCHAR(50)/registration_code_status VARCHAR(20) 作为 `onboarding_records` 列（L742/743 COMMENT 佐证）+ idx_onboarding_records_registration_code 索引 |
| 真实表（information_schema 实测 2026-08-16） | **registration_code 表不存在**；onboarding_records 存在（15+ 列，registration_code/registration_code_status 列存在）；flyway 最新版本=20260816.001 |
| Mapper 硬编码面 | `RegistrationCodeMapper.java` 注解 SQL 5 处引用 registration_code 表（L19 findByCode/L25 findByOnboardingRecordId/L31 countValidCodes/L37 updateToUsed/L43 updateExpiredCodes）；`RegistrationCodeMapper.xml` 为空文件（审计引用的「XML L19/25/31/37」实为 Mapper.java 注解行——位置一致，表名硬编码事实不变） |
| ServiceImpl 使用面 | insert（generateCode L92/batchGenerateCode L118）、findByCode（validateCode L132/useCode L154/useCodeWithOnboarding L205）、updateToUsed（L171/222）、selectById（updateCodeStatus L338）、selectPage（getCodesByPage L300） |
| 依赖链 | `OnboardingRecordServiceImpl.generateRegistrationCode` L104 insert（入口 POST /v1/onboarding-records/{id}/registration-code，OnboardingRecordController L58）+ completeOnboarding L137 调用；**实测第一失败点=OnboardingRecord 实体 code_expiry_time 字段在 onboarding_records 表无此列（V20260625_004 未建该列）→ selectById 列漂移 500，先于 registration_code 表缺失**（新发现事实，与 PD-014 关联） |

**决策事实与建议口径（不裁决，登记 PD-014）**：独立建表=需 DDL + 与 onboarding_records.registration_code 双写冗余；对齐 onboarding_records 列=实体改 @TableName + 字段映射（code→registration_code），但 onboarding_records 每行仅 1 列（1:1 当前码），RegistrationCode 实体为独立记录（支持码历史/批量生成）→ 语义差异（列存当前码 vs 表存码记录），需架构确认归属。

**catch 先行结论**：RegistrationCodeServiceImpl 全部方法无 catch 吞错（L340/379 为业务校验 throw：NOT_FOUND/PARAM_ERROR，非 catch）；失败透传 → GlobalExceptionHandler code:500+traceId 明确错误态（冒烟实测：POST registration-code → code:500「系统繁忙…追踪ID」success:false，无假成功）→ **无需改码，证据登记**。附：useCode 无效码分支写 registration_code_log（表缺，ETM-040 范畴，不在本批）→ 登记观察不扩大范围。

**migration**：**不落库**（PD-014 待裁决）。**REV**：REG-ETM-001（注册码生成/校验/核销链路含 OnboardingRecordServiceImpl 依赖链）待回归转正。

### OICBE-B1-002（KL-054，sales_order/orders）

**三向核对单（规则 6）**：

| 维度 | 证据 |
|---|---|
| 实体 | `SalesOrder.java` @TableName("sales_order") 19 字段（Long id AUTO）；`SalesOrderDetail.java` @TableName("sales_order_detail") 13 字段 |
| migration | 全仓无 sales_order/sales_order_detail 建表；orders 建表于 V6.0.0 L120（+V1.0.0.100 旧列集 + V20260629_003 store_id + V20260406 idempotency_key + V20260707_001 补列） |
| 真实表（information_schema 实测） | **sales_order/sales_order_detail 不存在**；orders 存在且 51 列=新旧双结构并存（旧列集 order_number/order_amount/actual_amount/contact_name/contact_phone/remarks/user_id/merchant_id/payment_method/transaction_id… + 新列集 order_code/order_status/total_amount/final_amount/paid_amount/…）；order_items 存在 17 列 |
| SalesOrder 使用面 | SalesOrderController（CRUD 端点）、SalesOrderServiceImpl（confirmDelivery/createReceivableForOrder/delete 等）、SalesOrderMapper（BaseMapper+XML 4 语句：L28 SELECT FROM sales_order/L52 SELECT sales_order_detail/L56 INSERT/L61 DELETE）、DashboardServiceImpl L96 |
| 字段对照 | SalesOrder 19 字段 vs orders：同名列 9（customer_id/customer_name/customer_phone/order_type/total_amount/discount_amount/actual_amount/remark/store_id）；**SalesOrder-only 8 列表无**：status/order_time/estimated_time/completed_time/table_no/people_count/created_by/updated_by；订单号 order_no↔orders.order_number（旧列）；状态机 status='completed' vs order_status=2；类型 Integer vs BIGINT（total_amount）；XML 引用 deleted/create_time 列存在 |

**决策事实与建议口径（不裁决，登记 PD-015）**：SalesOrder 与 OrderNew 语义上同为订单主表但字段命名/类型/状态机全异（SalesOrder 偏旧列集，OrderNew 偏新列集），orders 表新旧双结构并存——同表合并需契约（字段映射+状态机+类型+created_by/updated_by 无列）+订单明细归属（order_items vs sales_order_detail 不存在）；合并方案属架构决策。附 KL-054 家族上下文：DEF-3/KL-050/051 同族，ETM-002/003 归档。

**catch 先行（已改码）**：`DashboardServiceImpl.getTodaySalesOverview` L107-111——catch 吞错降级 code:0+0 值假空（改前实测 HTTP200 code:0 todayOrderCount=0）→ **改为日志明确 + throw BusinessException(500,「今日销售概览数据暂不可用」)** → Controller catch → code:500 明确错误态（改后实测 HTTP200 code:500 success:false）。影响范围：/v1/dashboard/today-sales 与 /v1/dashboard/overview/{1,2}（总览/销售）在数据源未对齐期间返回明确错误态（不再假空）；getInventoryAlerts/getMemberGrowthTrend 的同类 catch 吞错不在本卡（RM 轨 ETM-106/会员域）→ 登记观察。

**migration**：**不落库**（PD-015 待裁决）。**REV**：REG-ETM-002（dashboard 链路）+ REG-ETM-003（SalesOrderDetail mapper 链路）待回归转正。

**阶段一执行证据（developer，2026-08-16，只读核对+文档产出，零代码零表变更）**：① 真相源确认书 + ② 19 字段映射表 + ③ 全仓引用扫描清单与禁止新增声明（红线 R-8）+ ④ 阶段二批次计划（§四·2）→ 全部落于 **`docs/quality/sales-order-orders-mapping.md`**；information_schema 实测（2026-08-16）：sales_order/sales_order_detail 不存在、orders 51 列双结构全清单、OrderNew 33/33 列 0 缺失、flyway 仍 20260816.001、orders 1 行/order_items 0 行；新发现边界事实：orders_legacy 32 列/order_items_legacy 12 列（POS 旧链路独立表，不在合并面）、order_items.food_id varchar vs OrderNew 系 foodId Long 既存类型漂移（登记观察，非本卡范围）。**阶段二不启动。**

### OICBE-B1-003（KL-055，budgets）

**三向核对单（规则 6）**：

| 维度 | 证据 |
|---|---|
| 实体 | `finance/Budget.java` @TableName("budgets")：budget_id(Long AUTO)/budget_year/budget_month/budget_type/category_id/budget_amount(Long 分)/actual_amount/variance/variance_rate/responsible_dept_id/remark + BaseEntity(create_time/update_time/version/deleted) |
| migration | V20260704_001 L347-370 budgets 建表（旧采购预算结构）：budget_id VARCHAR(32) PK/budget_name/budget_amount DECIMAL(15,2)/used_amount/remaining_amount/budget_period/status/create_time/update_time/create_by/update_by/deleted |
| 真实表（information_schema 实测） | budgets 存在 15 列=migration 完全一致（含 V20260707_002 补 created_by/updated_by/version）；**行数=0**；实体-only 9 列表无：budget_year/budget_month/budget_type/category_id/actual_amount/variance/variance_rate/responsible_dept_id/remark；表-only 9 列实体无：budget_name/used_amount/remaining_amount/budget_period/status/create_by/update_by/created_by/updated_by；budget_id 类型漂移（VARCHAR(32) vs Long AUTO） |
| 使用面 | BudgetController /v1/finance/budgets 5 端点（BudgetServiceImpl BaseMapper 操作）；BudgetCheckScheduler L47（调度有 catch 仅日志，不伪装数据→观察）；BudgetMapper 3 个自定义方法（selectBudgetPage/selectByYearAndType/selectBudgetSummary）无 XML 无注解无调用方→死代码观察 |

**判定结论**：漂移**涉业务结构取舍**（旧采购预算结构 vs 财务预算实体语义差异：表列=采购预算名称/总额/已用/剩余/期间/状态；实体=财务预算年/月/类型/科目/部门/差异率）→ **登记 BLOCKED（PD-016），不落库**（卡面触发式规则命中）。

**假空形态处置（已改码）**：改前实测 GET /v1/finance/budgets（无筛选）→ HTTP200 code:0 records:[] total:0（分页 COUNT 短路假空）；改后 `BudgetServiceImpl.getPage` 先执行结构护栏 `BudgetMapper.countEntityColumnsPresent`（information_schema 校验实体 9 列）→ 未对齐抛 BusinessException(500) → 实测 HTTP200 code:500 success:false 明确错误态（无假空、无假成功）。护栏在 PD-016 裁决表结构对齐后自动放行（9 列齐备），无残留风险。

**migration**：**不落库**（PD-016 BLOCKED）。**REV**：REG-KL055（/v1/finance/budgets 无假成功、无 COUNT 短路假空列表）待回归转正。

**冒烟记录（改前/改后，2026-08-16 真实 HTTP 链路 localhost:8081）**：

| 端点 | 改前 | 改后 |
|---|---|---|
| POST /api/v1/onboarding-records/1/registration-code | HTTP200 code:500 明确错误态（追踪ID） | 不变（明确错误态；根因=code_expiry_time 列缺失先行） |
| GET /api/v1/dashboard/today-sales | **HTTP200 code:0 todayOrderCount:0（假空）** | **HTTP200 code:500「今日销售概览数据暂不可用」（明确错误态）** |
| GET /api/v1/finance/budgets?current=1&size=20 | **HTTP200 code:0 records:[] total:0（COUNT 短路假空）** | **HTTP200 code:500（明确错误态，结构护栏触发）** |

---

## 四·2、阶段二批次计划（PD-015 阶段二，2026-08-16 developer 阶段一产出后回填，**不执行**）

> **依据**：PD-015 已裁决两阶段（§四 待裁决点登记）——阶段一 = orders 唯一真相源 + 字段映射表 + 禁止新增引用（**已完成**：`docs/quality/sales-order-orders-mapping.md`）；阶段二 = 按批迁移（实体/查询/接口/报表/统计，每批 QA + 基线）。
> **回填来源（developer 阶段一产出）**：映射表 `docs/quality/sales-order-orders-mapping.md` §二/§三（19 字段逐列映射 + 8 无对应列 + 明细归属）+ §五 引用面扫描结论。
> **启动前置**：① 映射契约确认（状态机映射/主键策略/类型转换/候选列选边/8 无对应列处置/明细归属逐项闭环）；② planner 按映射表拆正式任务卡（编号 `OICBE-B2-xxx` 起）。
> **并行候选**：RM-B2 实施卡 3（idx_inventory_batch_no 索引落库）已排期 2026-08-16，与阶段二第一批并行候选（批约束按轨独立计）。

| 批次 | 范围 | 建议卡（占位） | 契约依赖 | QA + 基线 |
|---|---|---|---|---|
| Batch 2 | 实体+查询迁移 | OICBE-B2-001 实体对齐（SalesOrder/SalesOrderDetail @TableName+字段映射+8 无对应列处置）；OICBE-B2-002 Mapper.xml 4 语句（L28/52/56/61）+Mapper 接口+DashboardServiceImpl 查询改走 orders（今日销售概览恢复真实数据）；OICBE-B2-003 SalesOrderServiceImplTest 测试对齐（实体构造/状态机） | §二/§三映射契约（状态机/主键/类型/候选列选边） | QA 三向核对+冒烟；REG-ETM-002/003 扩展真实数据断言 |
| Batch 3 | 接口/服务迁移 | OICBE-B3-001 /v1/sales/order 8 端点语义迁移（SalesOrderController+SalesOrderService 层）；OICBE-B3-002 T-039 应收联动（confirmDelivery→ReceivableService）与财务凭证链路迁移 orders 体系 | 接口契约（端点/字段形态）、应收触发契约 | QA 接口+联动冒烟；REG-接口/T039 基线 |
| Batch 4 | 报表/统计+清理 | OICBE-B4-001 报表/统计面核对（OperationsReport/DailySettlement 等 orders 面确认零 sales_order 引用 + dashboard 统计真实数据断言）；OICBE-B4-002 遗留清理（废弃 sql/sales_order.sql、clear-data.sql 表名修正、前端注释同步 L44/191、测试脚本） | — | QA 报表/清理核对；REG-统计/回归基线 |

> **批约束（D-4 / R-2 / R-8）**：每批 2~3 卡：developer → qa（PASS/FAIL）→ 部署 → observe；每批含 **REV 回归基线更新**（regression 维护）；**分批迁移，不全仓一次性改**；迁移期间禁止新增 sales_order 引用（R-8），存量只减不增。**阶段二不启动，等 planner 放卡。**

---

## 四·3、评估项与差异表登记（2026-08-16，planner 排期登记；developer PD-016 执行中，产出后回填）

### L-1 评估项（finance 双包异常类并存，qa 验收建议评估）

| 项 | 内容 |
|---|---|
| **登记编号** | `OICBE-B1-003-L1`（来源：OICBE-B1-003 QA PASS 附限制 L-1，`docs/quality/oicbe-b1-qa-report.md` §三） |
| **事实** | 结构护栏抛 **finance 包 `BusinessException`** 未被 **common 包 `GlobalExceptionHandler`** 捕获 → 响应为通用「系统繁忙」（traceId 链路仍在，仍属明确错误态，不阻塞 PASS） |
| **评估点（qa 建议，非任务卡）** | finance 域**双包异常类并存**（finance 包 BusinessException vs common 包 BusinessException/GlobalExceptionHandler）——异常类型归属/捕获面/响应语义统一性评估 |
| **状态** | **待评估（登记不阻塞）**——排期建议：随 OIC-BE 后续批评估，或并入 PD-016 落库后的 budgets 链路复核；评估结论由架构/qa 输出后决定是否拆卡 |

### 差异表占位（budgets 表-only 9 列保留清单——developer PD-016 执行中，产出后回填）

| 项 | 内容 |
|---|---|
| **登记编号** | `OICBE-B1-003-DIFF`（来源：OICBE-B1-003 三向核对，KL-055） |
| **表-only 9 列（旧采购预算结构，实体无映射）** | `budget_name` / `used_amount` / `remaining_amount` / `budget_period` / `status` / `create_by` / `update_by` / `created_by` / `updated_by` |
| **处置（PD-016 已裁决：migration 为真相源，三步收敛）** | migration 收敛对齐落库（补实体 9 列 + budget_id 类型对齐）执行中；**不直接删字段**——剩余差异（表-only 9 列保留清单）落任务池 |
| **状态** | **占位待回填**——保留清单逐列明细 + 去向标注（保留/归档/后续迁移）由 **developer PD-016 执行后回填本表**；未回填前不拆卡、不处置 |

---

## 五、REV 回归基线条目计划（regression 维护，随各卡验收后转正）

| 基线条目 | 来源卡 | 回归行为 |
|---|---|---|
| REG-ETM-001 | OICBE-B1-001 | 注册码生成/校验/核销链路（含 OnboardingRecordServiceImpl 依赖链） |
| REG-ETM-002 | OICBE-B1-002 | dashboard 今日销售概览链路（无 BadSqlGrammarException、真实数据/明确错误态） |
| REG-ETM-003 | OICBE-B1-002 | SalesOrderDetail mapper 链路（SELECT/INSERT/DELETE 对齐后行为） |
| REG-KL055 | OICBE-B1-003 | /v1/finance/budgets 链路（无假成功、无 COUNT 短路假空列表） |

---

## 六、后续候选占位区（只列编号+要点，不拆卡）

| 项 | 要点 |
|---|---|
| ETM-002~093（A 类剩余） | 家族专项分批消化（entity-table-mapping-audit.md §2.1，P0 1 项已入 Batch 1、P1 54 项、P2 35 项） |
| EMP-BE-004 / EMP-BE-005 | 员工域端点立项 / 消息端点命名统一（OIC-2 Batch 2 联动项，`DEPENDENCY: Backend KL Pool`） |
| KIT-BE-004 | kitchen 路由前缀统一 + SecurityConfig 僵尸豁免清理（OIC-2 B2-003 联动项） |
| KL-050/051（状态） | 设备域漂移家族成员，家族专项统一治理中（production-known-limitations.md 状态引用） |

---

*初始创建：2026-08-16（架构代落盘，planner 卡面备稿）。登记来源：用户 OIC-BE 轨指令 + `docs/quality/entity-table-mapping-audit.md` + `production-known-limitations.md` KL-054/055 + `docs/quality/rm-b2-readiness-check.md`。未修改任何代码。*
*排期登记回写：2026-08-16（planner——新增 §四·2 阶段二批次计划占位框架（PD-015 阶段二：实体/查询/接口/报表/统计分批，每批 2~3 卡 + QA + 基线；developer 阶段一产出映射表后回填卡面）；§四·3 登记 OICBE-B1-003-L1 评估项（finance 双包异常类并存）与 OICBE-B1-003-DIFF 差异表占位（budgets 表-only 9 列保留清单——developer PD-016 执行中，产出后回填）。仅登记排期，未写代码、未做正确性判断）。*
