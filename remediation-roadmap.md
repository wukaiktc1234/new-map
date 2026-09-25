# 整体整改路线图（Remediation Roadmap）

> 更新日期：2026-08-10
> 维护人：研发负责人 + QA Lead
> 任务源：`production-remediation-task-board.md`（72 任务池：P0=20 / P1=31 / P2=21）
> 本文档是整改工程的**总调度视图**：Sprint 状态、流程规则、待决策队列、后续规划。单次 Sprint 明细见各 Sprint 文档。

---

## 一、当前状态总览

| 阶段 | 状态 | 结果 | 依据 |
|---|---|---|---|
| 生产风险审计（第一轮） | ✅ 完成 | 审计报告归档 | `docs/quality/data-authenticity-audit-20260810.md` |
| 任务池拆解 | ✅ 完成 | 72 任务 | `production-remediation-task-board.md` |
| Sprint-1（数据污染专项） | ✅ 完成 | 独立验收 5/5 PASS | `production-regression-test.md`（REG-DATA-001~005） |
| Sprint-2（财务真实性 + HR 假成功） | ✅ 执行完成 | **15/15 通过（含限制），FAIL 0** | `sprint-2c-execution-list.md` |
| Sprint-2 回归 | ✅ 完成 | PASS（REG-FIN-001~004、REG-HR-001~011、REG-RULE-001~003，FAIL 0） | `production-regression-test.md`（2026-08-10） |
| Release Gate | ✅ PASS | Sprint-1 + Sprint-2 均 FAIL=0 放行 | `production-regression-test.md`（2026-08-10） |
| BLOCKED_PRODUCT_RULE 队列 | ⏳ 等待产品 | 4 项 | `product-decision-backlog.md`（PD-001~004） |
| Sprint-3 → Sprint-4 | ✅ Sprint-3 CLOSED（2026-08-10/11）→ ✅ **Sprint-4 Release Execution CLOSED（2026-08-11）→ Operational ACTIVE（2026-08-13 00:47 收口确认）** | PD-001~004 决策落地全链闭环（基线 64 条，Release Gate PASS）→ 上线批准 → 部署执行 → Production LIVE → **Operational ACTIVE（35h 连续运行收口全 PASS）**；验收记录 `docs/quality/production-acceptance-record-1.0.0.md`；系统保持运行（未关机）；发布后治理批次（17 项 + KL-050~053）下一周期 | 见第五章 |

---

## 二、Sprint 执行记录

### Sprint-1（数据污染专项）
- 范围：P0-DATA-001 ~ P0-DATA-005（编辑变新增 5 页）
- 结果：独立验收 5/5 PASS
- 回归基线：已固化 REG-DATA-001 ~ 005

### Sprint-2（财务真实性 + HR 业务假成功）
- 范围：P0-FIN-003/004、P0-HR-001/003/004/006/007/008/009/010/011(禁用部分)/012(禁用部分) + P0-HR-002 部分
- 结果：**15/15 通过（含限制）**，FAIL 0；P0-HR-002-R1 = PASS_WITH_LIMITATION（报名唯一性子项受 PD-002 约束，未猜测实现）
- 回归：Sprint-2 回归 PASS（REG-FIN-001~004、REG-HR-001~011、REG-RULE-001~003），Release Gate PASS（FAIL=0），2026-08-10
- 遗留：4 项 BLOCKED_PRODUCT_RULE 保持等待产品（详见第四章）；P0-HR-011/012 完整修复依赖后端接口（技术排期）；验收遗留待办（过时注释 3 处、closeProfitAndLoss/closeYearProfit 假实现死代码、ContractCreateDialog 正文成功文案）待归卡

---

## 三、研发流程规则（正式纳入）

### BLOCKED_PRODUCT_RULE —— 业务规则缺失阻断规则（2026-08-10 正式生效）

**适用场景**：任何修复/开发任务涉及**业务规则缺失**（如：幂等键定义、唯一性约束、审批归属、状态机语义、期初结转方式等），且产品/业务方未给出结论时。

**强制行为**：

| 场景 | 模型必须 |
|---|---|
| 规则相关部分 | **禁止猜测实现**。不得自行假设业务逻辑、不得用默认值/占位逻辑冒充规则 |
| 可开发边界 | 仅"与规则无关"的部分允许先行（如删除硬编码数据、接口接入、失败透传、禁用入口） |
| 任务状态 | 标记 `BLOCKED_PRODUCT_RULE`，登记至 `product-decision-backlog.md` 决策池 |
| 决策落地后 | 产品结论回写至任务卡"验收标准"，状态更新为 `已决策` 后方可放行实现 |
| 验收口径 | BLOCKED 项不纳入 Sprint 通过数；通过数须注明"含限制（PASS_WITH_LIMITATION）" |

**配套规则**（沿用）：
1. 回归基线只允许基于已验收结论新增案例，禁止推测（`production-regression-test.md` 维护规则 4）
2. 模型执行前先审计 → 人工确认 → 执行 → 测试验证四步流程

---

## 四、等待产品决策队列（BLOCKED_PRODUCT_RULE，10 项）

| 编号 | 阻塞任务 | 缺失规则 | 状态 |
|---|---|---|---|
| PD-001 | P0-FIN-002 缴税真实链路 | 缴税重复缴纳的幂等规则（幂等键=税种+所属期+凭证号） | ✅ 已决策（2026-08-11） |
| PD-002 | P0-HR-002 培训报名子项 | 重复报名唯一性约束（键=员工+课程+期次） | ✅ 已决策（2026-08-11） |
| PD-003 | P0-FIN-001 AccountBalance 聚合 | 科目期初结转（不自动结转+人工录入+审计） | ✅ 已决策（2026-08-11） |
| PD-004 | P0-HR-005 HR 智能 3 页面 | 下线 vs 接真实 vs 演示标识（决策：**下线**） | ✅ 已决策（2026-08-11） |
| PD-005 | P1-DATA-009 会员统计 3 字段 | 消费占比/客单价/复购率口径定义 | 待产品+财务（不阻断，字段已临时移除） |
| PD-006 | P0-SEC-001 裸接口 25 方法（SelfPurchase 6 / Task 12 / PlanItem 7） | 操作归属角色 + 权限码定义 | 待产品（不阻断） |
| PD-007 | P0-SEC-001 AppealController 5 | 处罚申诉域归属（疑 HR）+ 权限码 | 待产品（不阻断；影响 SEC-001-B 边界） |
| PD-008 | SupplierPortal H5 6 端点 | 外部供应商签署认证方案（token 即凭证） | 待产品+架构（不阻断） |
| PD-009 | ApprovalWorkflow 13 方法 | 跨域通用审批权限模型（审批归属） | 待产品+架构（不阻断） |
| PD-010 | 退款审批 approveUserId | 审批归属：服务端会话派生 vs 客户端指定 | 待产品（不阻断） |

> 决策流转：产品/财务给出结论 → 回写验收标准 → 决策池状态更新 → 任务放行。见 `product-decision-backlog.md`。

---

## 五、后续规划

### 5.1 Sprint-3（2026-08-10 已批准，2026-08-11 冻结）

> 状态：✅ **已批准**（架构裁决 2026-08-10）；**CLOSED（2026-08-10/11）**。任务卡 25 张：P1 业务组 22 卡 + 新增卫生卡 3 张（P1-API-005 / P2-CLEAN-005 / P2-CLEAN-006），见任务池第 7 章。
> **发布就绪交付物（2026-08-11）**：`release-readiness-report.md`（发布就绪一页判断）+ `product-decision-request.md`（**正式交付物**：产品决策请求，含 Decision Owner / Deadline / Impact / Default 字段，BLOCKED_PRODUCT_RULE 保持）。
> **Sprint-3 冻结声明（2026-08-11）**：不开启新开发、不新增 P0/P1 任务；仅允许：a. 产品决策后的实现　b. 发布阻断修复。
> **红线约束（开发开工即生效）**：
> 1. BLOCKED_PRODUCT_RULE 不解除：PD-001~004 不进入任何开发。
> 2. P2-CLEAN-006：只允许删除死代码，**禁止以实现方式补结转/损益逻辑**（与 PD-003 同域）。
> 3. P2-CLEAN-005：只允许修正注释口径，**不新增任何业务规则断言**。
> **固定交付节奏（每批强制）**：developer 完成 → qa 独立验收 → regression 转基线 → roadmap 状态更新 → 下一批；开发完成后不得直接进入下一批。

**原则**：与 BLOCKED 4 项零重叠；先「禁用入口/移除假数据」，后「接真实接口」；权限基础设施先行，业务组按 3B-1/2/3 编排执行，卫生项随时插入。

#### Sprint-3A｜权限基础设施（P0-SEC-001~006，先行/串行组）

> 状态：**第一阶段（权限组）= CLOSED PASS_WITH_LIMITATION**（8/8，FAIL=0，3 项含限制；2026-08-10 复审 QA：PASS 5 / PASS_WITH_LIMITATION 3，报告 `docs/quality/sprint-3a-security-qa-report.md`）。Release Gate PASS；回归基线 REG-SEC-001~008 已固化（当前总基线 46 条）。限制逐卡关联：P0-SEC-004→L-1（KL-006）；P0-SEC-006→L-2（KL-007，验证卡 **P1-SEC-009** 承接，qa 责任，3B 期间执行、发布门禁前关闭）+ L-5（KL-013）；P1-SEC-003→L-3（P2-SEC-007）+ L-4（KL-009）+ L-6（KL-014，治理卡 **P2-SEC-009** 承接，下一周期执行）。治理/验证卡已登记任务池 §7.6。
> **第二阶段（后端安全组）✅ 完成（2026-08-10）**：P0-SEC-002 ✅ PASS_WITH_LIMITATION（QA `p0-sec-002-qa-report.md`，REG-SEC-009 固化）；P0-SEC-001 **整卡 ✅ PASS_WITH_LIMITATION**（A/B/C/D 子批全部 QA 通过，p0-sec-001a/b/c/d 报告 + B 复审 `P0-SEC-001-B-qa-report.md`；REG-SEC-010 固化）。整卡排除项（不纳入通过数）：35 设计豁免（KL-043，含 /v1/mp/** 架构裁决确认）+ 36 BLOCKED 方法（PD-006~008）+ 范围外 17 方法（ApprovalWorkflow 13 → PD-009 / QuickStockIn 3 → P2-SEC-010 / MiniProgram 1 → KL-043）。L-6/KL-014 统一随 P2-SEC-009（下一周期）。P1-SEC-009（渗透验证，qa 责任）✅ PASS（2026-08-10 活体渗透 14 用例全过，KL-007 关闭）；KL-013/KL-020 联调缺口为发布前收口待办。

| 任务 | 说明 |
|---|---|
| P0-SEC-003 / 004 / 005 / 006 | ✅ 已验收（复审 PASS×2 + PASS_WITH_LIMITATION×2）：「默认拒绝」基础设施 + 权限源统一（/me）；P1-SEC-001 已随同批完成 |
| P0-SEC-002 | ✅ PASS_WITH_LIMITATION（QA `p0-sec-002-qa-report.md`；REG-SEC-009 已固化） |
| P0-SEC-001 | ✅ 整卡 PASS_WITH_LIMITATION（A/B/C/D 子批全过 + B 复审，p0-sec-001a/b/c/d + P0-SEC-001-B 报告；REG-SEC-010 固化；排除项：35 豁免 KL-043（含 /v1/mp/** 裁决确认）+ 36 BLOCKED PD-006~008 + 范围外 17（KL-044~046）；L-6/KL-014 统一随 P2-SEC-009） |

#### Sprint-3B｜P1 业务组（22 卡，按 3B-1/2/3 顺序执行；范围与任务池 §7 一致，仅调整执行顺序）

> 状态：**3B-1 ✅ 完成**（QA PASS 4/5/0，REG-DATA-006~014 固化，KL-017~028 + PD-005）；**3B-2 ✅ 完成**（QA PASS 3/3/0 + P1-MOCK-010-R1 复验闭环，REG-MOCK-001~005/REG-EXPORT-002 固化，KL-029~042 + P1-DATA-012 拆卡）；**3B-3 ✅ 完成**（QA PASS 4/4，REG-API-001~003/REG-DATA-015 固化，基线 51 条 Release Gate PASS）。

| 编排 | 分组 | 任务 |
|---|---|---|
| **3B-1** | 数据真实性治理 | ✅ P1-DATA-001/002/003/004/005/006/008/009/010（007 已验收剔除） |
| **3B-2** | Mock / EXPORT 治理 | ✅ P1-MOCK-001/003/004/009/010 + P1-EXPORT-002（P1-MOCK-010 经 -R1 复验转 PASS；P1-MOCK-004 承接 L-1 已闭环） |
| **3B-3** | API 断流治理 | ✅ P1-API-002/004/005 + P1-DATA-011（并入执行） |
| 附注 | 权限剩余 | P1-SEC-001~004 ✅ 已于 3A 第一阶段完成；P1-SEC-005（部署红线）待后端组排期（随 3A 第二阶段或 3B 后端并行） |

#### Sprint-3C｜代码卫生（✅ 已完成 2026-08-10）

> 状态：✅ QA 验收 PASS（2/2，FAIL=0，报告 `docs/quality/sprint-3c-qa-report.md`）；回归基线 REG-CLEAN-001/002 固化（总基线 54 条）。红线保持：未实现任何业务规则（PD-001/002/003 未触碰）。

| 任务 | 说明 |
|---|---|
| P2-CLEAN-005 | ✅ 过时注释修正（tax-calculation.ts:102 / TrainingController.java:249 / TrainingService.java:104；training.ts:301 核验已无措辞关闭）；仅注释口径，无行为变更 |
| P2-CLEAN-006 | ✅ closeProfitAndLoss / closeYearProfit 死代码删除（接口 2 声明 + impl 2 假实现）；未实现结转逻辑 |

**不进本轮**：P0-HR-011/012 后端接口（技术排期，KL-004）；P1-MOCK-002（KL-004）；P1-MOCK-005（PD-004）；P0-AUD-001/002、P0-DATA-006/007/008（独立收尾批次，Sprint-3 后、发布前）。

### 5.2 P2 治理期（Sprint-4+）

- 双版本合并：P2-MERGE-001~007（**须待 P0/P1 冻结、全量回归通过后执行**，避免修复落在将被删除的版本上）
- 清理：P2-CLEAN-001~004（含备份目录移出仓库，需运维确认；P2-CLEAN-005/006 已提前至 Sprint-3C）
- 权限 LOW 组：P2-SEC-001/003/005；P2-SEC-002/004/006 依赖业务/资源确认
- 数据真实性 LOW 组：P2-DATA-001/002/003；P2-DATA-004 依赖字典接口

### 5.3 收口条件

- [ ] BLOCKED_PRODUCT_RULE 队列清空（4 项全部决策并回写）
- [ ] 72 任务池全部 DONE 或明确关闭
- [ ] 回归基线覆盖全部已验收模块且全 PASS
- [ ] 鉴权覆盖率 100% + CI 静态检查（Controller 无注解即构建失败）
- [ ] 假成功 grep 清零（全仓无 mock 写操作 + 无"成功"误导措辞）

### 5.4 Sprint-4 预案（Release Execution，2026-08-11 登记，未激活）

> 状态：🔄 **已激活（2026-08-11）**——PD-001~004 决策返回，BLOCKED_PRODUCT_RULE 解除。
> **范围**：PD 决策落地 → 实现闭环 → 上线 + 首日观察。
> **进度（首批闭环 ✅）**：P0-FIN-002 幂等 ✅ QA PASS；P0-HR-002 唯一性 ✅ QA PASS（限制：并发竞态/期次键差异，不阻断）；P0-FIN-001 期初录入+审计 ✅ QA PASS；P0-HR-005 下线 ✅ QA PASS（8/8）。**回归基线 64 条（REG-FIN-005/006、REG-HR-012/013），Release Gate PASS（FAIL=0）**；REG-RULE-001/002/003 防回归点已随决策更新；PWL 6 项销号；KL-001/002 阻断解除、KL-003 处置完成。
> **流转**：回写验收标准 ✅ → developer ✅ → qa ✅ → regression 转基线 ✅ → **门禁解除 ✅（技术条件全部满足）** → 上线放行（发布负责人）→ 首日观察。
> **发布状态机（Release Governance，2026-08-11 登记）**：`Release Approved → Deployment Execution → Production Verified → Sprint-4 CLOSED`。**状态：✅ 全链完成（2026-08-11）**——上线批准 ✅ → 部署执行 ✅（五步全过）→ **Production Status = LIVE（13:47）→ Operational ACTIVE（观察收口）** → **Sprint-4 Release Execution CLOSED**。
> **观察收口（2026-08-13 00:47 补执行）**：35h 连续运行证据全 PASS——关键链路双通道 UP / PID 7960·14452·12440 全程无重启 / 运行期应用级 ERROR=0 / DB 行数与备份基线一致 / flyway 188/188 / 幂等唯一性保持（08-11 冒烟实测，窗口内无写流量）。中间轮次（01:47/07:47/13:47）因会话中断未执行，**如实登记不补造**。**系统保持运行（未关机——发布负责人在场，默认不关机）**；回滚未触发；恢复指引（`production-acceptance-record-1.0.0.md` §六）备用。
> 参考：后续治理批次（KL-050/051/052、任务池 §8 共 17 项）随 Sprint-4 或其后续批次消化，不属本预案激活范围；Sprint-3 冻结声明持续有效。

### 5.5 多客户端治理（OIC-2 + RM 手持专项，2026-08-15 立项）

> 状态：🔄 **OIC-2 Batch 1 QA 全 PASS（3/3，2026-08-15）；RM Batch 1 QA 通过（PASS×2 + PASS_WITH_LIMITATION×1，无 FAIL）；批次待部署（发布负责人；RM-B1-002 构建门禁 08-16 12:55 收口后执行）**。
> 依据：`docs/quality/multi-client-audit-summary.md`（5 审计对象：pos/kitchen/employee/miniprogram/手持收货域；前端 142 项 P0×36/P1×74/P2×32 + 后端依赖 27 项）。单客户端明细：`docs/quality/client-audit-{pos,kitchen,employee,miniprogram,receiving-mobile}.md`。

**四项决策（2026-08-15 确认）**：

| # | 决策 | 说明 |
|---|---|---|
| D-1 | PD 候选立即进产品决策流程 | 立即签署（业务常识类，无需等待）：**「生产环境不允许演示登录/假 openid/支付 mock/演示数据」**；待评估（业务语义类，走 PD 流程登记，决策前不实现）：自动签名存证是否允许（RM-DATA-001/002）、APK 分发模式（RM-CAP-004/005） |
| D-2 | **ETM-106 提升 P0 后端专项**（Entity-Table Consistency Remediation） | 家族样本：DEF-3 / KL-050/051 / KL-054 / **KL-055** / **ETM-106**；**系统性专项治理，不全仓一次性改——分批迁移+验收**。执行窗口：**管理端 Batch 2 observe 收口（2026-08-16 12:55）后开工；观察期内不动后端** |
| D-3 | **双轨确认**：OIC-2（多客户端）+ RM（手持专项）各自独立 backlog/批次/QA/部署/observe | RM 为 frontend/ 内独立域，与 OIC-1 管理端批次隔离 |
| D-4 | planner 按**每批 2~3 卡**、developer → qa → deploy → observe 推进 | 首批：OIC-2 Batch 1 = POS 支付真实性 / Employee 演示登录 / Miniprogram openid（A 类信任风险先行）；RM Batch 1 = ETM-106 后端整改启动（窗口内先出 migration 方案，**不落库**）/ server.url 配置化 / cleartext 关闭规划 |

**执行约束（红线，全周期保持）**：
1. 发现 → 归类 → 入池；**不顺手修**
2. **不无 PD 定规则**（业务语义类决策前不实现）
3. **不大批量重构**（分批迁移+验收，每批 2~3 卡）
4. 后端观察窗口内不动后端（Batch 2 observe 收口 08-16 12:55 前，后端专项仅出方案不落库）

**双轨结构**：
- **OIC-2（多客户端）**：backlog `oic-2-task-board.md`（planner 维护）；覆盖 pos/kitchen/employee/miniprogram 4 客户端前端问题
- **RM（手持专项）**：backlog `rm-receiving-task-board.md`（planner 维护）；覆盖 Capacitor 壳 + ReceivingMobile + receipt-confirmation 域 + ETM-106 后端联动
- 两条轨道独立：backlog / 批次 / QA（docs/quality/）/ 部署 / observe；批次节奏沿用 OIC-1：planner 放卡 → developer（证据+build）→ qa（PASS/FAIL）→ 前端灰度 deploy → observe → 后端专项（D-2 窗口后）→ regression → Release Gate

### 5.6 双轨批次执行记录（2026-08-15 起）

| 批次 | 范围 | QA 结论 | 证据 | 状态 |
|---|---|---|---|---|
| OIC-2 Batch 1（A 类信任风险） | OIC2-B1-001 POS 支付真实性 / 002 Employee 演示登录 / 003 Miniprogram openid 阻断 | **3/3 PASS** | `docs/quality/oic2-b1-001/002/003-qa-report.md` | ✅ QA 通过（2026-08-15）；**已部署**（frontend-pos :4173 / employee-frontend :8080，2026-08-15 18:50 冒烟 PASS：页面可达 + POS 登录链路 401 真实响应 + 产物 grep 0 命中）→ **observe 进行中**：employee :8080 拓扑修正（反代 /api → :8081 已上线，commit 5d8b695，登录链路复验真实响应 200/json）→ **observe 起点重定义 CP1'=2026-08-15 19:18**（旧 CP1 标注不作数，证据不混合；观察至 08-16 12:55 收口）；POS CP1 PASS 持续；miniprogram 不出包，指引 `docs/quality/miniprogram-deploy-guide.md` 待发布负责人执行回执（**AppID 不可用 → 登记 OIC2-B1-003-BED-001（BLOCKED_EXTERNAL_DEPENDENCY，不伪造）**） |
| RM Batch 1 | RM-B1-001 ETM-106 方案卡 / 002 server.url 配置化 / 003 cleartext 规划卡 | **PASS×2 + PWL×1（无 FAIL）** | `docs/quality/rm-b1-qa-report.md` + `rm-b1-001-inventory-alignment-plan.md`（§十 架构评审通过） | ✅ QA 通过（2026-08-15）；**RM-B1-002 L-1 构建门禁**（构建 EXIT=0 + 双参数产物 diff）08-16 12:55 收口后由发布负责人执行回填；**RM-B2 Readiness Check 已产出**（`docs/quality/rm-b2-readiness-check.md`，判定 **PARTIALLY READY**：技术面全绿（inventory 35 列与方案一致、migration 链 188/188、V20260816_001 可安全新增）；**G-1 ✅ 已闭环**（ReceiptConfirmationServiceImpl +396/-12 精确提交 commit `4fc4f64`，发布基线固化为 git 基线）；**G-2 ✅ 已闭环**（8 个 untracked migration 审查五项通过——内容/连续性/checksum 8/8 一致/已执行固化/无冲突，精确提交 commit `b9a4aeb`）；G-3 生产独立实测待实施窗口） |

**已登记新发现（qa/developer 独立产出，批次 3+ 或后续窗口处理）**：
- OIC-2：OIC2-B1-002-QA-R1（守卫仅结构校验，认证真边界在后端签名）/ R4（mock 审批数据仍在产物）；OIC2-B1-003-QA-01（存量 temp_xxx 清理待 auditor 复核）；DEV-001~006（同源登记）
- RM：OBS-4/5/6（inventory 手工索引漂移 3 项，migration 零声明，实施卡 B 范围扩展至索引核对）；OBS-1/2/3（11 列手工漂移治理缺陷 / 双口径并存 / ReceiptConfirmationServiceImpl 未提交改动——**实施卡前置门禁**，架构 §十 事项 6）；**Readiness 新增 G-2：8 个 untracked migration（rank 181~188）入库决策**（新环境全量重建将缺表，风险面扩散）

**下一批（Batch 2）**：OIC-2 Batch 2 三卡已排卡（OIC2-B2-001 员工端断流前端侧 / 002 MP baseURL 接线 / 003 厨房路径契约前端侧；后端联动标注 `DEPENDENCY: Backend KL Pool`，不阻塞前端排期）——**执行窗口统一 08-16 12:55 管理端 Batch 2 observe 收口后开启**；RM-B2 实施卡 1/2 同窗口，前置门禁 G-1/G-2 须先闭环。

### 5.7 后端窗口解锁后批次记录（2026-08-16，管理端 Batch 2 observe 收口 PASS 后）

| 批次 | 范围 | QA 结论 | 证据 | 状态 |
|---|---|---|---|---|
| OIC-2 Batch 2（断流组） | B2-001 员工端断流前端侧 / B2-002 MP baseURL 接线 / B2-003 厨房路径契约前端侧 | **3/3 PASS** | `docs/quality/oic2-b2-001/002/003-qa-report.md` | ✅ QA 通过（08-16）；**待部署**：employee :8080 产物更新、kitchen/pos 双端构建、miniprogram 上传前 localhost 扫描 + 合法域名白名单 → observe |
| RM-B2 实施卡 1 + G-3 | ETM-106 inventory 补列落库（V20260816_001，rank 189） | **PASS + G-3 生产实测通过** | `docs/quality/rm-b2-impl1-qa-report.md` | ✅ 落库闭环（幂等 no-op + 冒烟 3 单不再 500）；**REG-ETM-106 基线转正（正式基线 65 条）**；实施卡 2（生产实测）核心已由 G-3 先行覆盖、实施卡 3（索引落库）待排 |
| OIC-BE Batch 1（后端整改池） | OICBE-B1-001 ETM-001 / 002 KL-054 / 003 KL-055（三向核对 + catch 先行 + 明确错误态） | **3/3 PASS（卡 3 附 L-1）** | `docs/quality/oicbe-b1-qa-report.md` | ✅ QA 通过（08-16，commit 72f0d5a）；**migration 均未落库**（PD-014/015 待架构/契约确认、PD-016 BLOCKED 待产品，决策前不落库不占通过数）；REG-ETM-001/002/003、REG-KL055 候选转正中；勘误确认（ETM-001 硬编码 SQL 在 Mapper.java） |

**待裁决点（决策池，决策前不落库）**：~~PD-014（registration_code 归属）~~ ✅ **已裁决 2026-08-16：代码引用优先建表**（V20260816_002 落库 rank 190）~~/ PD-015（SalesOrder 同表合并）~~ ✅ **已裁决：两阶段**（阶段一完成：orders 唯一真相源 + 映射表 `docs/quality/sales-order-orders-mapping.md` + 红线 R-8 禁新增引用；阶段二按批迁移待契约确认）~~/ PD-016（budgets 对齐方向）~~ ✅ **已裁决：migration 为真相源三步收敛**（V20260816_003/004 落库 rank 191/192，护栏放行）。
**裁决落地 QA（2026-08-16）**：三卡 **PASS_WITH_LIMITATION**（`docs/quality/oicbe-b1-impl2-qa-report.md` + `oicbe-b1-phase1-qa-report.md`）——migration checksum 独立重算 MATCH、SQL 层链路独立验证通过、护栏放行 GET code:0 独立复验；限制如实登记：**DR-01**（OnboardingRecord 实体 createdAt/updated_at/interview_id 既有列漂移 → 注册码 HTTP 成功路径阻断，Batch 2+ 排卡）、**DR-02**（budgets.budget_name NOT NULL → 写入面阻断，Batch 2+ 排卡）；RegistrationCodeMapper 2 处 updated_at→update_time 修复**需部署重启生效**（R-2，发布负责人知悉）。
**部署待办（发布负责人）**：OIC-2 Batch 2 三端产物（pos :4173 / kitchen :4174 / employee :8080，observe 起点 18:01:56 CP1 PASS）；OIC-BE 后端（含 72f0d5a + c697c95 Mapper 修复部署生效）；miniprogram 上传（AppID/BED-001 约束，扫描就绪）；REV 转正（REG-ETM-001/002/003、REG-KL055 真实数据断言扩展受限 DR-01/02，SQL 层断言为基线材料）。

### 5.8 OIC-2 Batch 2 observe 收口（2026-08-17 18:00）

> **判定：Observe CLOSED / Batch PASS（附中断标注）**——无新增 ERROR 模式（0）+ 无批次回归（0）+ 恢复后行为一致；管理端 Batch 2 同判 PASS。证据落 `docs/quality/observation-log.md` §7 + `oic-2-task-board.md`（CP5 收口行）。
> **观察覆盖（实测）**：前段 08-16 18:01:56~01:46（CP1/CP2 PASS，零新增 ERROR）+ 主机崩溃中断（日志截断 11:08:07，实际中断 ~11:08~11:15，**主机级事件非应用缺陷**：PG 自启/崩溃前零 ERROR/截断无堆栈/无修复即恢复；CP3/CP4 中断未执行如实登记不补造；用户口径 01:45~11:10 与实测差异已如实登记，01:45~11:08 为零异常运行段）+ 恢复后段（R-1~R-6 6/6 PASS 11:22 + 11:1x~18:00 零新增 ERROR）。**不重置计时、收口 18:00 保持**。
> **收口后解锁**：Batch 4/5 排卡（oic-2-task-board.md §五·2 占位：Batch 4=employee 假数据组 B3-候选-01~03，Batch 5=剩余项）+ RM 实施卡 3 待放卡 + OIC-BE 阶段二（前置=映射契约确认）+ 设计深谈（架构/产品）。

### 5.8 Frontend Product Design 阶段（2026-08-16 启动，Batch4+ 设计输入）

> 状态：🔄 **设计阶段进行中**（只做设计文档，不写代码、不构建、不部署）。OIC-1 管理端 Batch 3 已部署 Observe 中，收口 2026-08-17 18:00；收口后按设计文档拆 Batch4/5 实施卡。
> 验收依据：**前端设计原型验收标准 1~17（合并三轮设计评审结论，2026-08-16 定稿）+ Demo 构建追加验收要求 1~7（2026-08-16 并入，不扩大范围）**；17+7 条已全部落到四份设计文档。
> 追加验收落点：R-1 效率指标（认知路径 ≤5 跳转/新人上手/错误下一步）→ demo-spec §0.1 + business-flow §10.2；R-2 信息密度三模式 L1/L2/L3 → ui-spec §13 + demo-spec Demo-1/4；R-3 权限透明（数据不可见+原因说明）→ interaction §3.3 + demo-spec Demo-4；R-4 异常闭环（发现→影响范围→责任人→处理→结果→审计，非日志查看器）→ business-flow §10.3 + demo-spec Demo-5；R-5 引导型空状态（禁裸「暂无数据」）→ ui-spec §7 + demo-spec Demo-2；R-6 手持端弱网络独立验收 → demo-spec Demo-3；R-7 评审发现只回流设计文档（不进入开发任务）→ demo-spec §10 回流机制（例外：生产红线问题按既有审计流程归卡）。
> Demo 构建参考指引（2026-08-16 并入，不改变构建范围）：融合型参考只取模式不取代码（SAP Fiori 五原则/ProTable 模式/角色工作台/数据范围三视角），**禁止第三方 UI 框架**（复用 core + Element Plus）；Demo-1 = 企业运营驾驶舱（早班运营中心五区块 + 三模式同模板切换）；视觉约束=禁大圆角/渐变/大面积留白/装饰动画（10 年 500 家门店定位）→ ui-spec §15 + demo-spec §2。
> 产出（文件即真相源）：

| 文档 | 内容 | 状态 |
|---|---|---|
| `docs/design/ui-spec.md` | 渐进式 UI 设计规范（**DS-1.0 版本头：Token→Component→Pattern 三层**）；color/typography/spacing/button/table/form/empty/loading-error；**新增 §11 DS 结构 / §12 大数据模式预留 / §13 高密度列表 / §14 企业扩展预留 / §15 动效约束**；实证统计 + 收敛项 UI-1~7 | ✅ 已产出（2026-08-16，DS-1.0 定稿） |
| `docs/design/interaction-spec.md` | 交互状态机：6 基础场景（登录/会话过期/权限/危险确认/表单失败/收货）+ **新增 §7 统一查询 / §8 批量安全模型（部分成功）/ §9 离线 Level 0~4（现状 L1~L2）/ §10 可观测性（traceId 双视角）/ §11 审计展示**；收敛/决策点 IN-1~20 + UI-2/3 | ✅ 已产出（2026-08-16，定稿） |
| `docs/design/business-flow-analysis.md` | 业务链路分析：4 链路 + 收货确认；**新增 §6 权限模型 / §7 组织模型演进附录（现状 store_id 绑定如实→组织树 P1~P3）/ §8 状态机体系索引（采购 10 态等，系统化不重造）/ §9 角色化工作台 / §10 多端密度与异常治理边界**；A 类 12 项 / B 类 / C 类 PD-NEW-01~12 | ✅ 已产出（2026-08-16，定稿） |
| `docs/design/demo-spec.md` | **5 个设计原型验收规格（V1.1）**（Demo-1 驾驶舱→2 高密度列表→3 收货→4 权限组织→5 异常治理 mock）：每个含验收点/组件清单/运行说明/截图素材/规范对应关系；**评审维度 R-1~7 + §2 构建参考体系（SAP Fiori/ProTable/角色工作台/三视角，禁第三方框架）+ §10 回流机制 + 问题清单 DSN-01~15** | ✅ 已产出（2026-08-16，V1.1） |

**Batch4/5 输入要点（更新）**：
- 前端收敛（A 类，可直接排卡）：表格 loading 统一（UI-1）、8 处 confirm 文案补齐（UI-2）、EmptyState 推广（UI-3）、移除 devAutoLogin（AUTH-01）、导出失败反馈（ORD-04）、**工作台数据源审计（WS-01）、状态枚举映射审计（SM-01）**、**批量结果面板组件化（IN-15）、错误提示统一模板含 traceId（IN-18）**、保存查询条件推广（IN-13）。
- PD 候选（C 类，决策前冻结）：PD-NEW-01~12（含散点授权审计/有效期 PERM-01、组织树实体关系 PD-NEW-12）；决策池现有项持续有效（RM-DATA-001/002、PD-009/010 家族）。
- 后端依赖（B 类）：批量接口幂等/失败原因码（IN-16）、traceId 跨服务透传（IN-19）、创建接口幂等确认（IN-17）、大结果集异步导出（IN-14）→ 后端整改池排期。

**红线保持**：设计阶段不修改代码/不构建/不部署；C 类规则缺口不猜测；Demo-5 仅 mock 展示未来能力（不进整改批次）；Batch4+ 拆卡走既有节奏（planner → developer → qa → regression → architect 收口）。

### 5.9 Frontend Design Prototype 构建（2026-08-16 冻结后启动）

> 状态：🧊 **冻结待命（2026-08-16 冻结令生效）**——Demo 与设计文档全部冻结：不修改设计文档、不构建、不评审、不新增要求；`frontend-design-demo/` 保留现状（勿删勿改）；等待发布负责人真实系统体验后的定向指令。
> 冻结前已交付（V0.1，未评审）：5 Demo 可运行、22 张三态截图、R-1~7 勾选表×5、竞品对照×5、浏览路径、决策记录 D-01~18、零污染复核通过；DSN-16~26 已登记 `docs/design/demo-spec.md` §9（评审后统一处理）。
> 解冻触发：发布负责人真实系统体验后给出定向指令 → 按指令恢复评审/修订/构建。
> 任务：构建 `frontend-design-demo/` 可运行原型（**不开发生产功能**）。构建规格：`docs/design/prototype-build-spec.md`（文件即真相源）。
> **硬约束**：独立目录 `frontend-design-demo/`，禁止修改 frontend/ 与生产代码；禁止第三方 UI 框架/依赖（复用现有 core 组件模式 + mock 数据）；视觉遵守 ui-spec §15（禁大圆角/渐变/大面积留白/装饰动画）。
> **执行顺序（分批）**：Phase 1 骨架 → Phase 2 Demo-1 驾驶舱 → Phase 3 Demo-2 高密度列表 → Phase 4 Demo-3/4/5。保真度 V0.1（布局/信息层级/操作路径/状态，固定页面简化交互）。
> **交付检查（architect 独立验证，2026-08-16）**：`npm run build` EXIT=0 ✅；5 Demo 路由可达（playwright 独立验证，零 console error）✅；三态截图 22 张（Demo-1×4/2×4/3×6/4×5/5×3，含手持 390×844）✅；R-1~7 勾选表 + 竞品对照 + 浏览路径 + 决策记录（D-01~18）齐备 ✅；**零污染复核通过**（近 3h backend/frontend 源码零修改，唯一新目录 frontend-design-demo/）✅；评审表七项已在各 Demo checklist 覆盖。
> **每 Demo 交付**：可运行页面 + 三态截图（正常/异常/高压力：300 门店/10000 SKU/批量 200）+ R-1~7 验收勾选表 + 竞品对照说明（参考 X → 调整 → 原因）。
> **完成标准**：✅ 可启动、5 Demo 可浏览、交付物齐备 → **提交产品评审（下一步）**。评审发现按 R-7 回流（DSN-27+ 续号）；评审通过后再定 V0.2 细化。
> 不要求（规范预留，不实现）：真实 API / 大数据 / 离线 / 国际化。

### 5.10 UI 一致性约束 + 财务专业调整专项（2026-08-31 产品负责人确认）

> 状态：✅ **三步走专项全链闭环（2026-08-31）**：第一步盘点 ✅ → 第二步 core 增强 7 卡 ✅（P1-UI-CORE-001~007，007 经 -R1 复验）→ 第三步财务 4 页 ✅（P1-UI-FIN-001~004）→ 全部 QA PASS（FAIL=0、无 -R 遗留）+ 回归基线 **70→81（11 条新增全部在案）**，Release Gate PASS（治理口径）。全链路文件：盘点 `docs/quality/core-capability-inventory-20260831.md`；QA `docs/quality/ui-core-b1/b2/b2-r1/ui-fin-b1/b2-qa-report.md`；回归 `production-regression-test.md`（REG-UI-CORE-001~007 + REG-UI-FIN-001~004）。
> **专项遗留（如实登记，不占通过数）**：~~PD-028「待核销」~~ **✅ 已决策（2026-08-31，见 §5.11）**；P1-CORE-008（StandardPage 接入 vs 废弃）待架构裁决；P2-UI-CORE-008（row-dblclick 双击绑定静默失效）独立排期；WCAG 对比度未实测 + typecheck 存量 136 条建议治理（观察登记）。

### 5.11 五项裁决 + 财务数据血缘/勾稽专项（2026-08-31 产品负责人指令）

> 状态：✅ **数据血缘+勾稽 UI 专项全链闭环（2026-09-03）**：裁决落池 ✅（PD-028 已决 + PD-029~032）→ 第一步盘点 ✅（`docs/quality/finance-traceability-inventory-20260831.md`）→ 第二步实现 ✅（§9.3 共 8 卡，3 批：TRACE-1 契约对齐 C01/C02/C03 → TRACE-2 来源穿透 A01+口径标注 CL01 → TRACE-3 异常醒目 D01 + 勾稽占位 B01 + 后端缺口 BE01）→ 第三步 QA 独立验收 ✅（8 卡全部合规：7 卡 PASS + B01/BE01 登记合规，无 FAIL、无 -R）+ 回归基线 **81→87（6 条新增：REG-UI-TRACE-C01/C02/C03/A01/CL01/D01 全部在案）**，Release Gate PASS（治理口径）。全链路文件：QA `docs/quality/ui-trace-b1/b2/b3-qa-report.md`；回归 `production-regression-test.md`。
> **专项完成声明（硬约束 4 生效）**：**停止，等待产品负责人走查，不自动开新批**。
> **依赖登记（不占通过数）**：B01 勾稽列 = BLOCKED_EXTERNAL_DEPENDENCY（Batch0-方案2 同批落库后放行，届时追加基线）；BE01 后端缺口（GAP-B1/B2/B3）= BLOCKED（后端池排期）；#2 分层扣减 = P1-STOCK-001 独立方案卡（先方案→评审→实施→QA→回归，严禁混批）。

### 5.12 财务模块任务型重构专项（2026-09-03 产品负责人立项）

> 状态：✅ **阶段三全链闭环（2026-09-04）：6 批 10 卡全部 QA 通过（REG-FIN-WS-001~010 全部在案，基线 87→97 = 10 条新增，只增不减、FAIL=0）→ 阶段四就绪：等待产品负责人走查**（硬约束 4，不自动开新批）。
> **阶段三成果摘要**：① 6 工作台重组（对账/核销/记录/合规/毛利核算/决策 + 4 归位页）；② **C 类假筛选根除 6 处**（Subject 3 + Approval B-1/B-2 + 凭证 voucherStatus/voucherType 键名——QA 经 -R1 抓出 2 处断点：Batch 2 应付到期日【Service 未传 mapper】/ Batch 3 凭证类型【API 映射层丢弃】）；③ **死参数登记处置 6 处**（期间年份/responsibleDeptId/categoryId/referenceNo/批量审核/凭证导入——消费链核验纪律全程落实）；④ **真实入口补齐**（新建流水/writeOff/批量过账/红冲作废/联动 CRUD/审批 CRUD/toggleEnabled 契约修正）；⑤ **KL-058 身份伪造根除** + KL-059 税期动态化 + KL-061 伪语义列移除 + **WS-006 金额口径错误经 -R1 修复**（缩小 100 倍→统一）；⑥ 后端零改动（全部既有端点 UI 接入）、登记依赖 8 类不混入（P0 报表断流/导出/导入/后端校验缺口等 → 后端池 + PD-034/035 待产品）。

### 5.13 结构回退专项（2026-09-04 产品负责人裁决：回退结构、保留修复）

> 状态：✅ **结构回退专项全链闭环（2026-09-05）**：裁决登记 ✅ → 拆卡 ✅（§12 四卡）→ Batch RVT-1 全链闭环（基线 97→99）→ Batch RVT-2 全链闭环（QA PASS，穿透瞬态修复闭环，基线 99→100）→ Batch RVT-3 QA PASS（`docs/quality/ui-rvt-b3-qa-report.md`）→ REG-FIN-RVT-003 固化（基线 100→101，专项累计 97→101 = 4 条回退基线；WS 基线 10 条标注接管不删除）。
> **回退专项成果**：① 13 页菜单/路由恢复（工作台入口彻底移除，grep/dist 零残留）；② 标题原命名恢复（fund 资金管理/ledger 财务总账/…/report 财务报表）；③ **功能修复全部保留**（C 类假筛选根除 6 处/死参数修复/真实入口 9 项/打印/导出登记/KL-058/KL-059/KL-061/来源穿透/口径标注——QA 逐批保留项核验零回退）；④ 穿透修复闭环（A01→FinanceLedger + drillTo 13 处→独立页）；⑤ 待办区保留为页面级增强（真实字段派生不虚构）。
> **当前状态：停止，等待产品负责人逐页走查**（§5.13 硬约束，不自动开新批）。

### 5.14 财务模块布局级回退专项（2026-09-05 产品负责人指令）

> 状态：✅ **布局级回退专项全链闭环（2026-09-05）**：指令登记 ✅ → 盘点 ✅（9 页 A+B / 4 页纯 B）→ 产品裁决 ✅（9 页全回退 + 处置 6 项）→ 回退卡启用 ✅（LRVT-001~009，4 批；纯 B LRVT-010~013 CLOSED）→ 4 批全链闭环（QA 9/9 PASS，REG-FIN-LRVT-001~009 全部在案，基线 101→110 = 9 条新增，只增不减、FAIL=0）。
> **专项成果**：9 页布局恢复与 git 重构前基线（HEAD）**区块级同构**（待办区/工具栏区/页签化/SearchPanel 化/expand/分页并入全部移除恢复原结构）；**功能修复全部最小侵入重放**（C 类假筛选真实参数 6 处/入口 9 项挂回原布局入口位置/口径标注嵌入原布局/穿透成对不断链/金额口径 WS-006-R1 保持【分→元一次转换】/KL-059 税期动态化/KL-061 列/KL-060 登记/失败透传/打印——QA 逐批零回退核验）；应付到期日登记处置保持（不因回退变回假筛选）；能力层（密度/合计/固定列/逾期高亮）组件层自动保留零重放成本；后端与 api 层零改动。
> **当前状态：停止，等待产品负责人逐页走查**（§5.14 硬约束，不自动开新批）。
> **重放处置（产品裁决，按盘点风险清单逐项）**：① 入口承载位——工具栏删除后入口挂回 PageHeader#extra 或行内 actions（原布局入口位置），与 B 修复点同卡处理；② 筛选区——恢复原手写筛选区结构，重放后端真实参数 + 保存逻辑（Cost monthrange 在恢复布局内重做）；③ 详情——expand 行内详情回迁为原详情对话框（Ledger 分录 11-13 字段 + displayRecords 转换保留）；④ 穿透成对重放——Fund「查看凭证」↔ Ledger 接收端同批处理不得断链；⑤ 待办区——随区块删除直接丢弃（纯展示无副作用）；⑥ DataTable 能力层——组件层自动保留零重放成本（密度/合计/固定列/逾期高亮不回退）。

**一、盘点（只读，先出清单）**：对比「专题前版本」（git 重构前基线 = HEAD）与当前，列出所有发生【整体布局变更】的页面——区分：
- **A 布局级变更**：页面骨架/区块结构/布局重排（如五区骨架、待办区、页签合并改布局）
- **B 功能级修复**：筛选真实化/入口补齐/口径标注/穿透（不动布局的修复）
- 输出：页面清单（每页标 A/B 分类 + 变更摘要）→ `docs/quality/finance-layout-revert-inventory-20260905.md`

**二、回退（待产品负责人勾选清单后执行）**：对被勾选页面：
1. 恢复该页【上一版本布局】（git 恢复重构前版本）
2. 功能修复以【最小侵入】方式重放——只保留筛选真实参数/入口/口径标注等修复点，不改变原布局结构
3. 与 B 类功能冲突时：**优先保持原布局，修复点嵌入原布局内**

**三、纪律**：每页独立回退卡 + QA + 回归（101 只增不减）+ before/after 证据 + 逐页走查核验（修复不回退）。

完成后停止，等待产品负责人逐页确认清单与走查。

---

**裁决**：
- **一、恢复原 13 页菜单/路由**（撤销 6 工作台合并与页签重组）：fund(资金管理) / ledger(财务总账) / receivable(应收账款) / payable(应付账款) / tax(税务管理) / cost(成本管理) / report(财务报表) / approval(财务审批) / auto-voucher(自动凭证) / period(会计期间) / subject(科目) / budget(预算) / invoice-reimbursement(发票报销)；**菜单标题/图标恢复原命名；工作台入口彻底移除（不保留可选入口）**——/finance/reconciliation、/finance/records 路由移除，FinanceReconciliation.vue、FinanceRecords.vue 壳组件删除。
- **二、保留全部功能修复（页面级，与结构无关）**：C 类假筛选根除 6 处 / 死参数修复（金额×100 倍、应付到期日、凭证类型、期间）/ 真实入口 9 项（writeOff、批量过账、CRUD、红冲等）/ 打印补齐 / 导出保持登记（后端 P0 桩待排期）/ KL-058 前端身份伪造修复 / 来源穿透、口径标注、勾稽状态（PD-028 落库后生效）。
- **三、回退纪律**：每页独立回退卡 + QA 独立验收 + 回归（97 基线调整，回退卡新增编号，只增不减）+ before/after 证据；回退后逐页走查与功能核验（修复不回退）。
- **解读（总控，planner 按此拆卡）**：回退 = 菜单/路由/合并/页签/命名（结构层）；保留 = 筛选修复/真实入口/口径/穿透/打印/安全修复（功能层）。页面内工作台布局元素（待办区/五区骨架/统计卡片——基于真实字段的功能增强）**保留为页面级增强**，不属菜单级结构回退范围；如有歧义以产品走查为准。

**教训登记（roadmap §教训，2026-09-04）**：
> **「专业调整」≠「结构重组」**——菜单级合并/重命名/重组必须产品负责人**逐项确认**后才可执行；本专题因未逐项确认导致返工。**确认门禁（后续生效）**：任何页面结构变更（菜单/路由/合并/重命名/重组）必须产品负责人逐项确认后才能拆卡实施；未确认的结构变更一律冻结。

---
> **导出盘点结论（2026-09-03，auditor `docs/quality/finance-export-inventory-20260903.md`）**：财务域唯一导出端点 /vouchers/export = P0 桩（byte[0] 假成功 + 无 UI 消费断流，KL-062）；12 页需新增端点；审计 0 注解；口径反例（KL-063）；后端池 6 任务（P0-FIN-EXPORT-001/002、P1-003/004/005、P2-006）登记排期；**PD-035 导出规则（文件格式/行数上限/口径）待产品**；Batch 1 导出按钮 = disabled + tooltip 明示依赖（不伪造）。
> **阶段二评审修正（产品确认，回写设计 V1.1）**：
> 修正① 收付款执行动作归**核销工作台**（单据驱动）；**对账工作台 = 流水 + 勾稽 + 日结**；
> 修正② **记录工作台 = 页签化合并**（报销保留审批流，不做统一表单）；**KL-060 纳入批 3**；
> 修正③ **导出 = 筛选结果全部 + 统一服务 + 口径一致 + 审计**（统一导出服务原则）。
> **9 项确认（按设计 §6 上表）**：① 路由重定向（新路由+旧路由保留）✅ ② 命名不变 ✅ ③ 预算审批嵌入 ✅ ④ 冲正演进标注 ✅ ⑤ scaleLevel 折叠 ✅ ⑥ 报销页签（保留审批流）✅ ⑦ KL-060 批 3 ✅ ⑧ Approval 独立 ✅ ⑨ 日结 = 勾稽锁定 ✅。
> **阶段一评审补充确认（产品负责人 2026-09-03，回写阶段三范围）**：
> ① 筛选：C 类 3 处假筛选修复（前端接真实后端参数——FinanceSubject 已有 AccountingSubjectQueryDTO+getList 改造通道就绪）+ B 类 5 处参数映射修正；
> ② 工具栏：导出（13 页，**先盘点后端导出接口再补 UI**）+ 打印（8 页）补齐；
> ③ 入口：只读页 CRUD 入口 + 后端就绪 3 项前端入口（坏账核销 writeOff / 凭证批量过账 / 预算审批）；
> ④ 断流：P0 报表 404 = **唯一后端工作，后端池排期（小）**，不混入本专题实现。
> **阶段二设计原则（产品确认）**：以【前端任务驱动】——6 工作台映射是页面结构与交互重组；**后端缺口仅登记为依赖，不混入本专题实现（P0 除外）**。

**目标**：财务模块从「表格集合」重组为「任务型工作台」，解决使用体验问题（工具栏缺失 / 筛选无效 / 数据无感）。

**阶段划分**：
- **阶段一 · 诊断（只读，先查清再动）**：
  1. 逐页诊断清单：① 工具栏缺失——每页现有工具项 vs 任务需要（新建/导入/导出/批量/打印/日结/核销……）；② 筛选无效根因——逐页筛选器 → 是否接后端参数 / 字段缺失 / 前端本地过滤假筛选 → 根因分类
  2. 输出：**财务模块诊断报告**（缺口 + 根因清单，只诊断不修改）→ `docs/quality/finance-module-diagnosis-20260903.md`
- **阶段二 · 任务型设计（基于设计链，非样式补丁）**：按 **UTM T-FIN 8 任务**（`docs/design/unified-task-model.md` T-FIN-01~08 + FIA `docs/design/final-information-architecture.md` + IA `docs/design/interaction-architecture.md`）重组页面结构：
  - 资金流水 → **对账工作台**（来源穿透 + 勾稽状态 + 异常）
  - 应收/应付 → **核销工作台**（单据驱动 + 核销进度）
  - 账本/凭证 → **记录工作台**（三通道凭证 + 流水关联）
  - 缴税 → **合规工作台**（税种幂等）
  - 成本 → **毛利核算**（库存价值 + 口径标注）
  - 报表 → **决策工作台**（Level 2 管理报表）
  - 输出：**财务模块信息架构设计**（页合并/重构/保持清单）
- **阶段三 · 分批实现**：按设计拆卡（**每页独立卡**）：核心页重构 + 工具栏补齐 + 筛选修复（接真实参数）+ **既有增强能力保留**（密度/固定列/批量 N+M/来源穿透/口径标注）
- **阶段四 · QA + 回归（87 基线只增不减）+ 证据 + 走查**

**硬约束（全周期保持）**：
1. 口径已决（PD-028~032）照执行；**#2 分层扣减（P1-STOCK-001）独立方案卡不混入**；
2. 不碰其他模块；视觉同源 core/`_tokens.scss`；V3-A 仅结构参考；
3. **诊断阶段只读**；**每阶段交付 → 产品负责人评审 → 再进下阶段**；
4. 完成后停止，等待产品负责人评审。

---

**五项裁决（产品负责人 2026-08-31 确认，落决策池 `product-decision-backlog.md`）**：

| 编号 | 裁决内容 | 状态 |
|---|---|---|
| PD-028（更新） | **= 是**：fund_flows 增加 status（待核销/已核销），**与金额单位方案（Batch0-方案2）同批落库** | ✅ 已决策（2026-08-31）；BLOCKED 解除；P1-UI-CORE-007「待核销」子项与 P1-UI-FIN-001 约束解除 |
| PD-029 | #1 记账触发点 = 支付成功记收款流水（**资金事实 = 钱到账**）；挂账/信用账 = 演进项（应收机制） | ✅ 已决策（2026-08-31） |
| PD-030 | #2 库存扣减 = **分层扣减**：菜品层可售量（foods.stock）= 下单即扣、取消/支付失败回补；物料层实物 = 出餐/完成时按 BOM 扣（成本真实发生点）；统一现有三路径为双层时机 | ✅ 已决策方向（2026-08-31）；**架构级单独立项 P1-STOCK-001**（先方案→评审→实施→QA→回归，**严禁混入本批**） |
| PD-031 | #5 科目余额 = accounting_subjects.balance（分）= **管理口径真相**；account_balance（元，人工）**冻结停用**；页面统一标注「管理台账口径」；Level 3 总账再统一 | ✅ 已决策（2026-08-31） |
| PD-032 | #6 计价方法 = **最新入库价**为当前口径（页面标注「管理口径」）；移动加权平均 = Level 3 演进，**不迁移历史数据** | ✅ 已决策（2026-08-31） |

**数据血缘 + 勾稽 UI 专项（三步走）**：
- **第一步 盘点（只读）✅**：auditor 输出「来源字段可用性表」（`docs/quality/finance-traceability-inventory-20260831.md`）。核心结论：
  - 正向穿透：流水→凭证 **可行**（fund_flows.voucherId）；凭证→业务单据 **数据可行**（sourceType/sourceId/referenceNo 真实填充，但**无统一聚合接口**）；流水→单据**直达不可行**（fund_flows 无来源键，仅两跳）
  - 反向穿透：付款单→流水 **可行**（payment.fund_flow_id）；收款单→流水 **半可行**（ReceiptVO.fundFlowNo 无数据源待核实）；凭证→流水 **不可行**（FundFlowQueryDTO 无 voucherId 维度，需接口扩展）
  - 勾稽：fund_flows **无 status 列**（finance_tables.sql:405-422），须 PD-028 落库后接（**未落库前不悬空不伪造**）
  - 缺口：GAP-A1（fund_flows.status，Batch0 同批）/ GAP-A2（来源键）/ GAP-A3（应收核心订单来源键）/ GAP-B1（凭证→流水查询）/ GAP-B2（sourceType+sourceId 聚合查单据）/ GAP-B3（receipt 补 fund_flow_no）/ GAP-C1~C4（纯前端契约漂移等）
- **第二步 实现（planner 拆卡 → developer，视觉同源 core，V3-A 仅清单参考）**：
  - a. 流水每行显示来源（来源单据号+类型+时间），点击穿透：流水 ↔ 单据 **双向可查**——按盘点表可行范围实现；接口缺口（GAP-B1/B2/B3）拆卡标注依赖，**不伪造不猜测**
  - b. 勾稽状态列（**PD-028 落库后**）：待核销/已核销——字段未落库前不悬空（拆卡标注 Batch0 依赖，不占通过数）
  - c. 口径标注条：「分/元」+「管理台账口径」标签（PD-031/032 决策支撑，纯展示层）
  - d. 异常行醒目：未勾稽/逾期/对不上——仅基于真实字段（勾稽依赖 status 落库；逾期已有应收/应付 status 4 依据）
- **第三步 QA 独立验收 + 回归基线（81 只增不减）+ 证据提交（before/after + 代码引用）**。

**硬约束（全周期保持）**：
1. 只加来源展示/状态字段/口径标注，**不改变既有业务语义**；
2. **#2 扣减分层 = 单独方案卡（P1-STOCK-001），严禁混入本批**；
3. 未决事项清零后，财务页「待核销/来源/口径」均有事实支撑，**不再悬空**（PD-028/031/032 已决 = 未决清零）；
4. 完成后停止，等待产品负责人走查（不自动开新批）。

---
> **基线口径核对（2026-08-31，regression 以文件为真相源）**：追加前正式基线 **70 条**（用户口径 69 遗漏 REG-KL055 转正 + REG-EMP-001 增量，文件头部注记「正式 65→69」实含该条，08-18 REG-EMP-001 转正后 69→70）；追加 3 条后 = **73 条**；只增不减达成（既有 70 条零修改）。
> 来源：产品负责人确认（2026-08-31）。衔接任务池 §9（P1-UI-FIN-001~004，2026-08-30 登记，来源 Prototype V3-A 走查）。
> 盘点结论速览：固定列 ⚠️部分 / 密度切换 ❌（MainLayout 设置 UI 假联动）/ 保存筛选 ❌（SearchPanel 0 消费）/ 批量 N/M ❌（死控件+TableActionBar 假功能）/ 行内详情 ⚠️（无 expand）/ 合计行 ❌（无 show-summary）/ 分页 ⚠️（无封装）/ 列宽省略 ✅；附带假联动发现 P1-CORE-001/004/010（planner 排卡处置）；115 个 DataTable 消费方 → **新增 prop 必须默认关闭、opt-in，向后兼容红线**。
> **架构裁决（2026-08-31，总控）**：密度唯一真相源 = layoutStore；compactMode / tableDensity 双开关收敛为单一 store 字段（设置 UI 行为不变、DataTable 实际生效），属技术重构非业务规则；若收敛需改变现有页面行为，developer 停止并回报架构，不猜测。
> **待裁决登记（planner，不占通过数）**：P1-CORE-008（StandardPage 接入 vs 废弃）待架构裁决；「待核销」状态语义 → 业务规则预案（实现时发现缺失即走 BLOCKED_PRODUCT_RULE）。
> **PD-028 已登记（2026-08-31，Batch CORE-2 developer 触发，`product-decision-backlog.md`）**：「待核销」状态语义缺失——后端 `FundFlow` 实体 / `FundFlowVO` / `fund_flows` 表**均无 status 字段**，前端 `FundFlowStatusMap`（`converters.ts:256-267`）「1~3 ↔ pending/completed/cancelled」为悬空声明，后端不返回 status → 资金流水状态恒「未知」（`FinanceFund.vue:35-42` 兜底）。决策面：fund_flows 是否补 status 字段 / 后端判定 vs 前端派生（产品+财务共同确认）。决策前不实现不猜测（不占通过数）；overdue/abnormal 纯展示键已先行；影响：P1-UI-CORE-007「待核销」子项停止、第三步 P1-UI-FIN-001 异常状态醒目接入。

**约束登记（产品确认，先记录，全周期有效）**：

| 层 | 范围 | 强制要求 |
|---|---|---|
| ① 全局统一层 | 所有模块 | 布局骨架 / tokens / 导航 / Element Plus 组件语言 / 反馈语义 / 破坏性操作确认——**强制一致** |
| ② 模块专精层 | 模块内专业增强 | 以共享 core 组件增强实现（DataTable 等**一次增强、全局受益**）；**禁止每页自造样式**；增强能力在模块内一致使用；视觉仍同源 `frontend/src/styles/global/_tokens.scss` |
| ③ 判定标准 | 新增页面样式 | 无法说明属于「全局统一层 or 模块专精层」的，**不得实施** |

**财务流水/账本专业调整（三步走）**：
- **第一步 · core 能力盘点（先查后改）**：核对 DataTable / MainLayout / StandardPage 现有能力 vs 清单（固定列✅ / 密度切换？ / 保存筛选？ / 批量部分成功？ / 行内详情？ / 合计行？ / 分页？）→ 输出缺口表（**只盘点不修改**）。盘点执行：auditor，输出 `docs/quality/core-capability-inventory-20260831.md`。
- **第二步 · core 增强（一次实现全局受益）**：按缺口表增强共享组件（DataTable 增加 density 联动 MainLayout、expand 行内详情、show-summary 合计等），**向后兼容现有页面（不破坏 69 基线回归）**；任务卡由 planner 拆（P1-UI-CORE-00x 组）。
- **第三步 · 财务 4 页应用（模块内一致）**：FinanceFund / FinanceLedger / FinanceReceivable / FinancePayable 统一接入增强能力（衔接任务池 §9 P1-UI-FIN-001~004，**执行方式升级为「接入 core 增强能力」，禁止逐页自造**）；**不改变业务语义 / 金额口径 / 未决 #5/#6 标注保持现状**。

**纪律（本专项强制）**：任务卡编号（planner 拆卡，禁无编号修复）+ QA 独立验收（PASS/FAIL/PASS_WITH_LIMITATION，报告 `docs/quality/`）+ 回归基线 **69 条只增不减**（新基线 REG-UI-CORE-xxx / REG-UI-FIN-xxx 由 regression 固化）+ 证据提交（before/after + 代码引用）；V3-A（`frontend-design-v3/`）**仅作能力清单参考，不借样式**（禁止照搬 V3-A 样式/实现）。

**红线（保持任务池 §9.1）**：① 不新增后端接口、不猜业务规则；② 批量操作/筛选仅基于既有接口能力，接口不支持项保持现状并登记；③ Batch0-方案2 金额单位治理不触碰、方案3 凭证状态映射对齐按原卡范围执行；④ 开发者不得自行宣布通过（QA 独立验收）；⑤ core 增强必须向后兼容（现有 69 条基线页面零回归）。

---

## 六、文档索引

| 文档 | 职责 |
|---|---|
| `production-remediation-task-board.md` | 72 任务池总表（任务卡 + 执行方式 + 验收标准） |
| `production-audit-report.md` | 前端 664 文件审计报告 |
| `docs/quality/data-authenticity-audit-20260810.md` | 前后端联合审计（5 项清单 + 优先级） |
| `sprint-2-task-cards.md` | Sprint-2 任务卡（16 卡拆解） |
| `sprint-2c-execution-list.md` | Sprint-2C 执行清单与结果（15/15 含限制） |
| `product-decision-backlog.md` | 产品决策池（PD-001~004，BLOCKED_PRODUCT_RULE 登记处） |
| `production-regression-test.md` | 回归测试基线（23 项：Sprint-1 5 项 + Sprint-2 18 项；Release Gate PASS） |
| `production-known-limitations.md` | 已知限制清单（KL-001~003） |
| `docs/design/ui-spec.md` | Batch4+ UI 设计输入（渐进式 UI 规范 + 实证统计） |
| `docs/design/interaction-spec.md` | Batch4+ 交互设计输入（6 场景状态机 + 收敛项） |
| `docs/design/business-flow-analysis.md` | Batch4+ 业务链路分析（4 链路 A/B/C 标注 + PD 候选） |
| `docs/design/demo-spec.md` | 5 个设计原型验收规格 V1.1（验收点/组件清单/运行说明/截图素材/问题清单 DSN-01~15 + 评审维度 R-1~7） |

---

### 5.15 UI-DESIGN-001：V3 回退后 UI/交互失败复盘 + 设计基线重建（2026-09-05）

> 状态：🔄 **D-01 完成，待产品负责人评审**（2026-09-05）。
> **范围**：UI 失败复盘 + UI Design Baseline + 映射表 + 4 个样板设计说明 + 待评审事项。
> **性质**：设计规范与验证任务，不修改生产代码、不修改业务逻辑、不修改数据库、不调整现有生产菜单结构。
> **产出物**：
> 1. `docs/design/ui-failure-analysis.md` —— UI 失败复盘文档（基于历史真实证据）
> 2. `docs/design/ui-design-baseline.md` —— UI Design Baseline 文档（可验证的设计规范）
> 3. `docs/design/ui-failure-baseline-mapping.md` —— 失败案例→基线要求→验收标准映射表
> 4. `docs/design/ui-sample-designs.md` —— 4 个跨业务域样板设计说明
> 5. `docs/design/ui-product-review-items.md` —— 待产品负责人评审事项
> **评审门槛**：D-01 完成后，停止继续 UI 扩展，进入产品负责人评审。只有 UI 基线通过评审后，才进入 4 样板 → 横向评审 → V4 Prototype → 生产前端。
> **当前阶段不得因为"某个财务页面很急"而绕过该门槛。**

### 5.15 FIN-UI-003 付款/资金账户真实前端链路系统化修复专项（2026-09-08）

> 状态：✅ **全链闭环（2026-09-08）**——审计完成（4项发现）→ 任务卡已创建（P1-FIN-PAY-001~002, P2-FIN-PAY-003~004）→ 开发执行完成 → QA独立验收通过（1 PASS + 3 PASS_WITH_LIMITATION，FAIL=0，`docs/quality/fin-ui-003-qa-report.md`）→ 回归测试完成（REG-FIN-PAY-001~004 固化，正式基线 110→114 条，Release Gate PASS，FAIL=0）→ **专项完成，等待产品负责人走查**。
> **专项成果**：① 逾期应付付款参数类型不匹配修复（前端 string→number + Number() 显式转换）；② B模式回填银行记录入口打通（BankPaymentRecordController 新建 + payment.ts 补充 API 方法）；③ 银行账户列表加载失败修复（N+1 查询优化 + 错误处理增强）；④ 付款账户显示异常修复（accountLabel() 防御性处理）。
> **限制登记**：① B模式回填无UI入口（API断流已修复，UI入口缺失）；② 双层状态机语义不统一（Payment.status 1/2 vs BankPaymentRecord.status 0~5）；③ 银行账户无种子数据（migration 未含 INSERT）；④ 付款账户空态无提示文案。
> **BLOCKED_PRODUCT_RULE**：PD-038（付款单状态机前后端对齐）、PD-039（银行记录确认权限定义）已登记决策池，待产品确认。
> **停止条件**：P1~P4全部真实验证通过后停止。任何一项FAIL生成-R修复项返回开发。完成后等待产品负责人再次业务走查。本任务不自动进入其他财务功能开发。
> **范围**：付款/资金账户真实前端链路系统化修复，解决逾期付款参数类型不匹配、B模式回填银行记录入口缺失、银行账户列表加载失败、付款账户显示异常4个问题。
> **性质**：独立的前后端真实链路修复任务，不是UI美化，而是让已经确认的财务业务语义真正能够通过管理端完成。
> **必须遵循**：问题复现 → 证据采集 → 根因定位 → 最小修复 → 重启/编译 → 真实页面验证 → 独立QA → 回归。
> **禁止事项**：不得顺带重做财务UI、新增设计语言、修改资金真值模型、修改余额推导逻辑、修改应付来源模型、扩展付款业务、新增无关权限、修改其他财务模块。
> **验收要求**：每个问题必须提供修改前真实表现、根因证据、修改文件+行号、修改后代码证据、后端重启/编译证据、真实页面操作证据、API实际响应、DB断言。
> **独立QA**：开发完成后交独立QA验收，P1/P2/P3/P4全部真实验证通过后停止。任何一项FAIL生成-R修复项返回开发。
> **回归**：受影响REG-FIN基线必须复跑。

### 5.16 FIN-ACCOUNT-002-A 测试账户替换与资金账户前后一致性专项（2026-09-08）

> 状态：✅ **全链闭环（2026-09-08）**——当前账户状态确认 ✅ → 建立受控测试账户 ✅ → 付款500根因分析 ✅ → 修复付款500 ✅ → 修复 bank_payment_record 创建 ✅ → 账户管理页面验证 ✅ → 旧账户处置 ✅ → QA 验收（PASS_WITH_LIMITATION）✅ → 修复现金付款DTO校验 ✅ → QA 复验（PASS）✅ → 交付证据 ✅。
> **专项成果**：① 付款 500 根因定位（bank_payment_record 表缺少 BaseEntity 字段）；② bank_payment_record 创建逻辑补齐；③ 现金付款 DTO 校验修复；④ 旧测试账户停用；⑤ 新测试账户建立并验证。
> **修改文件**：V20260908_003__fix_bank_payment_record_base_entity_columns.sql（新增）、Payment.java、PaymentServiceImpl.java、PaymentCreateDTO.java。
> **QA 结论**：首验 PASS_WITH_LIMITATION（1 FAIL：现金付款）→ 复验 PASS（现金付款修复验证通过）→ 整体 PASS。
> **停止条件**：P1~P4 全部真实验证通过后停止。任何一项 FAIL 生成 -R 修复项返回开发。完成后等待产品负责人再次业务走查。

### 5.17 FIN-ACCOUNT-002-B 账户测试数据卫生与负余额根因确认（2026-09-08）

> 状态：✅ **全链闭环（2026-09-08）**——P0-BANK-001 修复 ✅ → 异常测试账户逐笔核实 ✅ → 乱码账户处理 ✅ → 基准测试账户建立 ✅ → 数据清理 ✅ → QA 验收（PASS_WITH_LIMITATION）✅ → 补验退回 ✅ → account_id=6 停用 ✅ → 孤儿流水清理 ✅ → 最终终态验收 ✅ → QA 复验（PASS）✅。
> **专项成果**：① P0-BANK-001 修复（FundFlowServiceImpl 增加余额校验）；② 异常测试账户逐笔核实（account_id=2,5 分类完成）；③ 乱码账户处理（account_id=1,6 均已停用）；④ 基准测试账户建立（account_id=4 期初资金 100000 分）；⑤ 数据清理（12 个付款单作废，5 个应付账款清理，account_id=NULL 孤儿流水清理）；⑥ 余额重算验证通过（所有启用账户 variance=0）。
> **修改文件**：FundFlowServiceImpl.java（余额校验）、bank_accounts 表（停用 account_id=1,6）、fund_flows 表（期初资金 + 清理孤儿流水）。
> **QA 结论**：首验 PASS_WITH_LIMITATION → 补验通过 → 复验 PASS。
> **BLOCKED_PRODUCT_RULE**：P0-BANK-002（现金账户语义）待产品决策。
> **停止条件**：所有 10 项验收标准均通过，负余额账户为 0，乱码账户启用数为 0，account_id=NULL 流水为 0，余额重算 variance=0。完成后等待产品负责人走查。

### 5.18 FIN-DOMAIN-001 产品负责人确认（2026-09-07）

> 状态：✅ **产品负责人确认完成**——余额真值方案确认 + P0 第一批优先级确认 + P1 总体方向确认 + 实施纪律确认。

**一、余额真值方案确认**：

| 项 | 确认结论 |
|---|---|
| 资金流水 | **资金事实真值** |
| 账户余额 | 派生/缓存数据，**不得作为独立事实来源** |
| updateBalance | **禁止直接修改财务余额事实** |
| 影响余额事件 | 期初、调整、收款、付款、退款/冲销、作废等**必须产生对应资金流水** |
| 实施前置 | **必须先完成余额影响事件全量盘点**并证明没有"无流水改余额"路径 |
| balance 字段 | **不以简单删除作为本方案的强制前提** |

**二、P0 第一批优先级确认**：

| 优先级 | 任务 | 说明 |
|---|---|---|
| 1 | P0-FIN-001 | 余额漂移/资金真值治理 |
| 2 | P0-FIN-002/003 | 真实报表接口 |
| 3 | P0-FIN-004 | 税务端点真实性核验 |

**三、P1 总体方向确认（不锁死细化排期）**：

原则：**先资金基础设施 → 再债权债务 → 再财务记录 → 最后高级财务规则**

- 账户实施依赖 P0-FIN-001
- 应收退款/冲正、凭证收口、账龄、导出、预算等根据 FIN-DOMAIN-001 真值矩阵进一步拆卡

**四、实施纪律确认**：

1. 每卡独立 QA
2. 回归基线只增不减
3. 每项必须有真实代码/数据/API证据
4. 不因 UI 页面需要绕过财务真值治理
5. 不得自动修改/清洗历史财务事实
6. 历史异常先盘点、分类、制定处置方案，再执行

**五、决策池状态更新**：

- PD-036（余额真值方案）：✅ 已决策
- PD-037（FIN-DOMAIN-001 产品确认）：✅ 已决策

**六、下一步**：完成确认后进入 P0-FIN-001 技术拆卡。不要同时启动 P1 实施卡。

---

---

*追加：2026-09-23 KDS 扣料 P0 收口 + P1 启动定义（planner，见 03-review）：P0-KDS-DEDUCT 状态 **CLOSED_WITH_REGISTERED_RISKS**（`p0-kds-deduct-closure-001.md`，验收 33/33，残余→KL-065~067，requiredQty→PD-043）；P1-ORDER-NUMBER Scope 已成文（`p1-order-number-scope-001.md`，推荐方案 A，回滚演练 §6.1，完成标准 5 条，KL-068 阻断项待其关闭）。PD-043/KL-065~068 已登记。未改业务代码、未做 requiredQty 业务裁定。*
*追加：2026-09-23 P1 Scope Amendment：有效 Scope → **`p1-order-number-scope-002.md`**（001 SUPERSEDED）；G1–G4 已裁决（P2 独立/residual、E1a-c、KL-068 范围 PENDING_CONFIRMATION、A/B/C/D 原始定义恢复）；CC-1~CC-9 冻结；回滚=实施中真实演练；实施仍待 Amendment **独立审查**通过。零业务代码。*

*追加：2026-09-23 DS 六项治理澄清（planner）：KL-068 发布列 **Current=UNKNOWN/PENDING_CONFIRMATION（唯一）**，原行内「阻断发布=是」降为 Historical/superseded interpretation（provenance 保留；正文 :509「KL-068 阻断项待其关闭」仅指 **E2E 限制随 P1 关闭**，**不是**发布单元=current 是）。CC 拆 GATE（CC-1~7,9）/REG（CC-8）；E1c 须 H-03 独立验证；Gate ✅≠问题解决。Scope-002 待 Independent Review；无 003、零业务代码。*

### 5.19 Batch ORDER-A1（P1-ORDER-NUMBER-002 / A1）— 本地放行

| 项 | 状态 |
|---|---|
| 任务编号 | **P1-ORDER-NUMBER-002 / A1** |
| Scope | `docs/architecture/03-review/p1-order-number-scope-002.md`（**FROZEN**，FREEZE_ID=P1-OSN-002-FREEZE-20260923T155544+0800） |
| 实施 | `buildOrderEntity` 内 `order.setOrderNumber(orderCode)` 单行 + `OrderNewServiceImplOrderNumberA1Test`（3/3 PASS） |
| Git | `7f40ab7` 首次实施 → `6f0f2b9` revert（CC-7 回滚五 Phase PASS）→ `0acf99b` reapply |
| 实施记录 | `docs/architecture/03-review/p1-order-number-002-implementation-record-001.md`（COMPLETE）+ `.../p1-order-number-002-evidence/` |
| qa | **PASS_WITH_LIMITATION** — `docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md`（H-01~H-06 独立复跑 6/6 PASS，无 -R） |
| regression | **PASS，FAIL=0，ALLOW_LOCAL** — `docs/quality/P1-ORDER-NUMBER-002-A1-release-gate.md`；REG-ORDER-001~007 入库，`production-regression-test.md` 基线 114→121 |
| planner | 任务池 §22.1 状态回写；KL-069~073；新卡 **P1-POS-FOODID-MAP-001**（**2026-09-24 CLOSED_WITH_REGISTERED_LIMITATIONS**，KL-071→已解除，残余 KL-074~076） |
| 三处对齐 | qa=PWL / regression=PASS / roadmap=**本地放行** ✅ |
| 限制 | L-01 生产不可达 → **PROVISIONAL_PENDING_PRODUCTION_EVIDENCE**（解阻=生产抽验 H-01/H-02/H-04）；L-02 回滚窗口 API 探针；L-03→已建卡；L-04 文档 untracked（解阻=入库，FROZEN 仍禁改内容）；L-05 OrderVO 展示层 |
| 下游 | 生产抽验待 L-01；~~L-03 由 developer 执行 P1-POS-FOODID-MAP-001~~（**2026-09-24 已收口 CLOSED_WITH_REGISTERED_LIMITATIONS**，QA PASS，残余 OBS-S1/OBS-B2-500/ENV-1=KL-074~076）；**本状态 ≠ 生产发布批准** |
| 红线 | 未改 generateOrderCode/MAX+1、Schema/Flyway、POS T/W 语义、P0 扣料、requiredQty/PD-043、P2-A/B/C；无 Scope-003 |

---

*本文档为路线图总视图；修改任何状态须同步更新对应明细文档。*

*追加：2026-09-23 Batch ORDER-A1 收口（architect 主会话代写；子代理 architect 因 `remediation-roadmap.md` edit 被 deny 未能写入）：qa/regression/planner 三处已由对应角色落盘，本节补齐 roadmap 状态 = **本地放行 / Gate PASS / 生产 PROVISIONAL**。与 `docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md`、`...-release-gate.md`、`production-regression-test.md` REG-ORDER-001~007、任务池 §22、KL-069~073 对齐。零业务代码、未动 FROZEN Scope-002、未 commit。*

*追加：2026-09-24 P1-POS-FOODID-MAP-001 收口回写：状态 = **CLOSED_WITH_REGISTERED_LIMITATIONS**（V1–V4/V6/V8/V9=PASS，V5=PWL，V7=PARTIAL→OBS-S1；QA 抽检 PASS FAIL=0）。与任务池 §22.3、`p1-pos-foodid-map-001-implementation-record-001.md` §0/§11/§12、`docs/quality/P1-POS-FOODID-MAP-001-qa-report.md`、KL-071（已解除）→KL-074~076（OBS-S1/OBS-B2-500/ENV-1）对齐。零业务代码、未动 FROZEN Scope-001、未 commit。*

*追加：2026-09-24 P1-COMBO-ORDER-001 收口回写（Owner 路径 A 治本，改读 `combo_ingredients`）：状态 = **本地放行 / 可进下一阶段**（实施报告 Stage **QA_PASS_WITH_LIMITATION → REGRESSION_CANDIDATE**，§0）。QA = **PASS_WITH_LIMITATION**（`docs/quality/P1-COMBO-ORDER-001-qa-report.md`，验收 6/6 PASS，FAIL=0，无 `-R`；L-01 UI 浏览器目检未做、L-02 生产证据缺失 → `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`）；DS 抽检 §11 = **READY_FOR_QA**；回归 = **PASS**（FAIL=0，**REG-ORDER-008~011** 转正，正式基线 **121→125**）。与任务池 §23（PASS_WITH_LIMITATION + 回归 PASS；P1=66→67、合计 133→134）、**KL-078~080**、实施报告 §0 对齐；证据链 commit `aec5c45`（15 文件业务）/ `40fc776`（DS §11）/ `24a1f60`（QA 报告）。**生产仍 PROVISIONAL；本状态 ≠ 生产发布批准**。下一步批次待规划：legacy 三文件 `PosApiServiceImpl`/`KitchenScanServiceImpl`/`OrderMaterialRequirementServiceImpl` 仍读旧表 `combo_ingredient` → **第二步卡**（实施报告 §7.1，KL-080）。零业务代码、未 commit。*

*追加：2026-09-24 P1-COMBO-ORDER-001 收口（PWL）：状态终态 = **CLOSED_WITH_REGISTERED_LIMITATION**（QA PASS_WITH_LIMITATION 维持 6/6 FAIL=0；抽样复核 3 PASS + 1 SCOPE_CONTAMINATION_FOUND → **ENV-2** 登记，commit `aec5c45` 内容混合，不 rewrite history；预防规则 **PG-001** `docs/project-context/process-guards.md`）。与任务池 §23.1/§23.5、实施记录 §0/§10/§12、KL-078~080、ENV-2 对齐；回归 REG-ORDER-008~011 转正基线 121→125。生产仍 PROVISIONAL。下一步卡预告：P1-COMBO-LEGACY-CLEANUP-001（废弃旧表，任务池待办区，未启动）。零业务代码、未 commit。*

*追加：2026-09-25 P1-COMBO-LEGACY-CLEANUP-001 收口：状态终态 = **CLOSED_WITH_REGISTERED_LIMITATION**（DS 抽检 5/5 PASS；QA PASS_WITH_LIMITATION 活体 4/4 + DB 3/3，FAIL=0；REG-ORDER-012 转正基线 125→126；PG-001 v2 首卡全流程实践）。与任务板 §24.2 收口回写块、实施记录 §6、QA/DS 报告对齐。RESIDUALS：L-01/L-02/OBS-3/OBS-4 + OBS-1 升级独立卡 **P1-POS-MENU-500-001**（同日已实施修复 commit 872c874）。遗留债路线图：P0-WORKSPACE-WIP-CONSOLIDATION-001（分五批入库工作区 WIP，PENDING）待排期。生产侧证据缺口 OBS-3 同族（KL-069/076/079）。零业务代码（收口轮）、未 commit 工作区 WIP、未动 git 历史。*

*追加：2026-09-25 P0-SCHEMA-SINGLE-SOURCE-001 收口：状态终态 = **CLOSED_WITH_REGISTERED_LIMITATION**（Flyway 唯一真相源 + PG-003/PG-004 治理规则落盘；41 遗留脚本归档；对齐基线 v2：MISMATCH 93 / NO-TABLE 70 / NO-ENTITY 57 = KL-081；NO-TABLE 溯源：16 归档 scripts + 1 DatabaseFixConfig + 6 其他 Java 类 + **47 真黑箱 = KL-082**，应急 DDL 快照已导出 PROVISIONAL）。下游 **P0-FLYWAY-COVERAGE-001**（P0：23 张转写 + 47 张活体导出+生产比对）与 **P0-WORKSPACE-WIP-CONSOLIDATION-001**（PENDING）为两大待启动卡。零业务代码、未动 Flyway、未改实体、未 commit WIP。*

*追加：2026-09-25 F4 卡（P1-PURCHASE-SUPPLIER-BINDING-001）正式收口：**CLOSED_WITH_REGISTERED_LIMITATION**（Owner 裁决 DS/QA 豁免——1 文件 ~26 行 + 单测 2/2 + 活体 + warn 文案完整；历史 6 条审计 = 当时档案绑定一致）。同轮 F6 卡（P1-NEW-FOOD-LEGACY-SYNC-001）状态按 Owner 指令回退 CLOSED → **IMPLEMENTED_QA_PENDING**（QA 独立验收 5 项进行中，通过后 CLOSED）。零业务代码、未动 git 历史。*
