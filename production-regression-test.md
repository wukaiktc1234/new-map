# 生产系统回归测试基线

> 维护人：QA Lead（生产回归测试负责人）
>
> 用途：避免后续 Sprint 修复、重构、优化过程中，已验收问题再次回归。
>
> 更新规则：每个 Sprint 验收通过后，将验收结论转换为回归案例；状态变化需注明日期与原因。
>
> 基线来源：Sprint-1 数据污染专项（REG-DATA-001~005）；Sprint-2 财务真实性 + HR 业务假成功治理（REG-FIN-001~004、REG-HR-001~011、REG-RULE-001~003，验收日期 2026-08-10）；Sprint-3A 第一阶段 权限组（REG-SEC-001~008，验收日期 2026-08-10）；Sprint-3B-1 数据真实性治理（REG-DATA-006~014，验收日期 2026-08-10）；Sprint-3B-2 Mock/EXPORT 治理（REG-MOCK-001~005、REG-EXPORT-002，验收日期 2026-08-10）；Sprint-3B-3 前端域 4 卡（REG-API-001~003、REG-DATA-015，验收日期 2026-08-10）；Sprint-3A 第二阶段 运维高危接口鉴权（REG-SEC-009，验收日期 2026-08-10）；Sprint-3C 代码卫生（REG-CLEAN-001~002，验收日期 2026-08-10）；DEF 修复批次（P0-DATA-009 / P0-MOCK-001 / P0-DATA-010：REG-DATA-016~017、REG-MOCK-006，验收日期 2026-08-10）；KL-048 修复批次（O-DEF2-1 → KL-048：REG-DATA-018，验收日期 2026-08-11；并更新 REG-DATA-016 限制关闭）；DEF-A/B 修复批次（P0-ASSET-001 / P0-DEVICE-001：REG-ASSET-001、REG-DEVICE-001，验收日期 2026-08-11，QA 复验 + KL-030 冒烟重跑闭环，见 `docs/quality/p0-defab-kl030-rerun.md`）；Sprint-4 首批 决策落地 4 项（P0-FIN-002 缴税幂等 PD-001 / P0-HR-002 报名唯一性 PD-002 / P0-FIN-001 期初录入+审计 PD-003 / P0-HR-005 HR 智能 3 页面下线 PD-004：REG-FIN-005~006、REG-HR-012~013，验收日期 2026-08-11）；REV 后端轨（发布后独立治理编排，2026-08-16）：RM-B2 实施卡 1（ETM-106 inventory 补列落库 → REG-ETM-106 转正，QA 独立验收 PASS `docs/quality/rm-b2-impl1-qa-report.md` + G-3 生产独立实测通过，验收日期 2026-08-16）；OIC-BE Batch 1 REV 转正（REG-ETM-001/002/003、REG-KL055，QA 独立验收 PASS `docs/quality/oicbe-b1-qa-report.md` 2026-08-16：三卡全 PASS、无 FAIL、无 -R1，卡 3 附限制 L-1；验收日期 2026-08-16）；**OIC-BE 裁决落地断言扩展登记（2026-08-16：PD-014/016 落库 commit c697c95 + PD-015 阶段一 commit 7fa3e0d，QA 独立验收 PASS_WITH_LIMITATION `docs/quality/oicbe-b1-impl2-qa-report.md` + `docs/quality/oicbe-b1-phase1-qa-report.md`——REG-ETM-001 扩展 SQL 层链路断言（HTTP 成功路径面待 DR-01）、REG-KL055 扩展读面断言（写面待 DR-02）、REG-ETM-002/003 维持「明确错误态」断言（阶段二逐批扩展真实数据断言））**；**2026-08-18 REV 基线更新：RM-B2 实施卡 3（idx_inventory_batch_no 索引落库）QA 独立验收 PASS `docs/quality/rm-b2-impl3-qa-report.md` → REG-ETM-106 扩展索引落库断言（L1678 过时口径更新）；OIC-2 Batch 4 三卡（OIC2-B4-001/002/003，employee 假数据组）QA 独立验收 PASS `docs/quality/oic2-b4-qa-report.md` → REG-EMP-001 转正（断流如实提示断言，OIC 轨口径不进 Release Gate）；OIC-BE 阶段二批 1（OICBE-B2-001/002/003）developer 开发完成、QA 报告 `docs/quality/oicbe-b2-qa-report.md` 并行产出中——REG-ETM-002/003 扩展候选待 QA（如实标注））**。；**2026-09-03 更新（Batch TRACE-1，数据血缘专项前置·契约对齐 3 卡）：P1-UI-TRACE-C01/C02/C03 三卡 QA 独立验收 PASS（`docs/quality/ui-trace-b1-qa-report.md`，2026-09-03：3/3 全 PASS、无 FAIL、无 -R；PD-028 未落库合规、P1-STOCK-001 零触碰）→ 新增正式基线 REG-UI-TRACE-C01/C02/C03（81 → 84 条）**；**2026-09-03 更新（Batch TRACE-2，数据血缘专项·来源展示+口径 2 卡）：P1-UI-TRACE-A01/CL01 两卡 QA 独立验收 PASS（`docs/quality/ui-trace-b2-qa-report.md`，2026-09-03：2/2 全 PASS、无 FAIL、无 -R；穿透零新增接口、口径标注与决策池 PD-031/032 逐字一致；PD-028 未落库合规、P1-STOCK-001 零触碰）→ 新增正式基线 REG-UI-TRACE-A01/CL01（84 → 86 条）**；**2026-09-04 更新（Batch WS-3，财务模块任务型重构·阶段三 Batch 3 记录工作台）：P1-FIN-WS-003 记录工作台 QA 首验 FAIL（F-1 凭证类型筛选死参数假筛选——voucher.ts mapQueryParams 丢弃数字 voucherType）→ P1-FIN-WS-003-R1 复验 PASS 闭环（`docs/quality/ui-ws-b3-r1-qa-report.md`，2026-09-04）→ 新增正式基线 REG-FIN-WS-003（89 → 90 条）**；**2026-09-04 更新（Batch WS-4，财务模块任务型重构·阶段三 Batch 4 合规工作台）：P1-FIN-WS-004 合规工作台 QA 独立验收 PASS（`docs/quality/ui-ws-b4-qa-report.md`，2026-09-04：无 FAIL、无 -R；KL-059 行为实证 6/6；筛选全链零死参数；观察项 O-WS-B4-1~3 登记）→ 新增正式基线 REG-FIN-WS-004（90 → 91 条）**；**2026-09-23 Batch ORDER-A1 订单号唯一性专项（P1-ORDER-NUMBER-002/A1：REG-ORDER-001~007，验收日期 2026-09-23，QA 报告 `docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md` PASS_WITH_LIMITATION + 回归独立抽验 PASS）**；**2026-09-24 Batch P1-COMBO-ORDER-001 套餐下单/KDS 组合套餐专项（REG-ORDER-008~011，验收日期 2026-09-24，QA 报告 `docs/quality/P1-COMBO-ORDER-001-qa-report.md` PASS_WITH_LIMITATION + 回归 Regression Result PASS）**
> 基线口径：当前 125 条正式基线 + 候选 0 条（2026-08-16 更新：正式 65 → 69：OIC-BE Batch 1 四条目转正；正式 64 → 65：REG-ETM-106 转正）。正式 69 条 = Sprint-1 5 条 + Sprint-2 18 条（FIN 4 + HR 11 + RULE 3）+ Sprint-3A 10 条（SEC 1~8 + 第二阶段 SEC-009/SEC-010）+ Sprint-3B-1 9 条（DATA 6~14）+ Sprint-3B-2 6 条（MOCK 1~5 + EXPORT 2）+ Sprint-3B-3 4 条（API 1~3 + DATA 15）+ Sprint-3C 2 条（CLEAN 1~2）+ DEF 修复批次 3 条（DATA 16~17 + MOCK 6）+ KL-048 修复批次 1 条（DATA 18）+ DEF-A/B 修复批次 2 条（ASSET-001 + DEVICE-001）+ Sprint-4 首批 4 条（FIN 5~6 + HR 12~13）+ REV 批次 5 条（REG-ETM-106 + OIC-BE Batch 1 的 REG-ETM-001/002/003、REG-KL055，2026-08-16）；候选 0 条（OIC-BE Batch 1 4 条目已于 2026-08-16 转正）；26 为早期规划口径。**2026-08-16 断言扩展登记：4 条目（REG-ETM-001/002/003、REG-KL055）断言基线材料扩展（基于已验收 PASS_WITH_LIMITATION 结论），不新增条目、基线计数不变（69 条）、正式基线 FAIL=0 维持。2026-08-16 19:1x 追加：REG-ETM-001 R-2 Mapper 修复（updated_at→update_time）部署生效线上断言补入（主机重启后实测：运行实例 jar 字节码 + SQL 层回滚验证，见条目限制说明③与 observation-log.md §6.5），基线计数与状态不变（PASS_WITH_LIMITATION，FAIL=0 维持）。** **2026-08-18 更新：正式 69 → 70（REG-EMP-001 转正，来源 OIC-2 Batch 4 三卡 QA 独立验收 PASS `docs/quality/oic2-b4-qa-report.md`，OIC 轨口径不进 Release Gate）；候选 0 条维持；REG-ETM-106 断言扩展（RM-B2 实施卡 3 索引落库，QA 独立验收 PASS `docs/quality/rm-b2-impl3-qa-report.md`）不新增条目、计数不变；OIC-BE 阶段二批 1 QA 报告未产出（REG-ETM-002/003 扩展候选待 QA，如实标注）；正式基线 FAIL=0 维持。** **2026-08-31 更新（Batch CORE-1，core 增强治理批次）：P1-UI-CORE-001/002/003 三卡 QA 独立验收 PASS（`docs/quality/ui-core-b1-qa-report.md`，2026-08-31：FAIL=0、无 -R、无 BLOCKED）→ 新增正式基线 REG-UI-CORE-001/002/003（正式 70 → 73 条，只增不减、既有条目零修改）；候选 0 条维持；正式基线 FAIL=0 维持；本批为治理批次（非发布批次），不进上线门禁判定。** **2026-08-31 更新（Batch CORE-2，core 增强治理批次）：P1-UI-CORE-004/005/006 三卡 QA 独立验收 PASS（`docs/quality/ui-core-b2-qa-report.md`，2026-08-31）+ P1-UI-CORE-007 FAIL（失败点 F-1：_dark-mode.scss 无 overdue/abnormal 深色覆盖）→ 生成 P1-UI-CORE-007-R1 返回 DOING → 新增正式基线 REG-UI-CORE-004/005/006（正式 73 → 76 条，只增不减、既有条目零修改）；**P1-UI-CORE-007 因 FAIL 本批不转基线**，仅登记待 R1 闭环后追加 REG-UI-CORE-007（不预固化）；候选 0 条维持；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）；本批为治理批次（非发布批次），门禁 CONDITIONAL PASS（007-R1 未闭环，修复+复验 PASS 后转正式 PASS）→ **2026-08-31 R1 闭环更新：P1-UI-CORE-007-R1 QA 复验 PASS（`docs/quality/ui-core-b2-r1-qa-report.md`，F-1 闭环）→ 追加正式基线 REG-UI-CORE-007（正式 76 → 77 条，只增不减、既有条目零修改）；Batch CORE-2 门禁 CONDITIONAL PASS → 正式 PASS（4/4 卡 QA 全过、正式基线 FAIL=0、PWL=0 维持）；不进上线门禁判定。**** **2026-08-31 更新（Batch FIN-1，第三步·财务 4 页第一批）：P1-UI-FIN-001/002 两卡 QA 独立验收 PASS（`docs/quality/ui-fin-b1-qa-report.md`，2026-08-31：两卡全 PASS、无 FAIL、无 -R；P1-UI-FIN-001 业务语义零变更（git diff API 调用集合 3 处一致）+ PD-028 限制如实登记（BLOCKED 不占通过数）；P1-UI-FIN-002 Batch0-方案3 对齐 5 点全核验一致）→ 新增正式基线 REG-UI-FIN-001/002（正式 77 → 79 条，只增不减、既有条目零修改）；候选 0 条维持；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）；本批为治理批次（非发布批次），不进上线门禁判定。** **2026-08-31 更新（Batch FIN-2，第三步·财务 4 页第二批）：P1-UI-FIN-003/004 两卡 QA 独立验收 PASS（`docs/quality/ui-fin-b2-qa-report.md`，2026-08-31：两卡全 PASS、无 FAIL、无 -R；应收 overdue 键接入真实字段依据（converters.ts:153-166 后端 status 4=overdue）+ 业务语义零变更（API 调用 2 处 git diff 零变更）；应付同口径（API 3 处 git diff 零变更）+「待付款」无独立字段不发明；PD-028 限制如实登记（BLOCKED 不占通过数））→ 新增正式基线 REG-UI-FIN-003/004（正式 79 → 81 条，只增不减、既有条目零修改）；候选 0 条维持；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）；三步走专项（第一步盘点 + 第二步 core 增强 7 卡 + 第三步财务 4 页）新增基线累计 70 → 81 = 11 条全部在案；本批为治理批次（非发布批次），不进上线门禁判定。**；**2026-09-03 更新（Batch TRACE-1，数据血缘专项前置·契约对齐 3 卡）：P1-UI-TRACE-C01/C02/C03 三卡 QA 独立验收 PASS（`docs/quality/ui-trace-b1-qa-report.md`，2026-09-03：3/3 全 PASS、无 FAIL、无 -R；PD-028 未落库合规（status 相关为登记项非 FAIL）、P1-STOCK-001 零触碰）→ 新增正式基线 REG-UI-TRACE-C01/C02/C03（正式 81 → 84 条，只增不减、既有条目零修改）；候选 0 条维持；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）；本批为治理批次（非发布批次），不进上线门禁判定。**；**2026-09-03 更新（Batch TRACE-2，数据血缘专项·来源展示+口径 2 卡）：P1-UI-TRACE-A01/CL01 两卡 QA 独立验收 PASS（`docs/quality/ui-trace-b2-qa-report.md`，2026-09-03：2/2 全 PASS、无 FAIL、无 -R；穿透零新增接口（复用既有 getById + 详情弹层）、口径标注与决策池 PD-031/032 逐字一致；PD-028 未落库合规（status 相关为登记项非 FAIL）、P1-STOCK-001 零触碰）→ 新增正式基线 REG-UI-TRACE-A01/CL01（正式 84 → 86 条，只增不减、既有条目零修改）；候选 0 条维持；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）；本批为治理批次（非发布批次），不进上线门禁判定。** **2026-09-03 更新（Batch WS-1，财务模块任务型重构·阶段三 Batch 1 样板页）：P1-FIN-WS-001 对账工作台 QA 独立验收 PASS（`docs/quality/ui-ws-b1-qa-report.md`，2026-09-03：验收标准 8/8 + 强制核验 6/6 全过、无 FAIL、无 -R；五区布局（待办区 PD-028 登记不虚构计数）/ 修正①边界（收付执行动作零新增）/ 工具栏三项（新建=真实端点接线、导出/日结=disabled+tooltip 明示依赖）/ E 值格式修复 / 失败透传 + 引导空态 / create 接线与后端 FundFlowCreateDTO 逐字段一致 / 增强能力 9 项零回归 / 菜单标题变更（路径/组件/scaleLevel 零变更）；typecheck 136=136 + build EXIT=0）→ 新增正式基线 REG-FIN-WS-001（正式 87 → 88 条，只增不减、既有条目零修改）；候选 0 条维持；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）；导出/日结为登记依赖非 FAIL（EXPORT-001 盘点结论 + 确认⑨ + PD-028，按钮 disabled+tooltip 明示，不伪造动作）；本批为治理批次（非发布批次），不进上线门禁判定。** **2026-09-04 更新（Batch WS-3，财务模块任务型重构·阶段三 Batch 3 记录工作台）：P1-FIN-WS-003 记录工作台（FinanceLedger + AutoVoucher + InvoiceReimbursement 页签化合并）QA 首验 FAIL（`docs/quality/ui-ws-b3-qa-report.md`：F-1 凭证类型筛选死参数假筛选——voucher.ts mapQueryParams:36 解构丢弃 + :43-45 仅字符串分支，数字 voucherType（7 值语义 1-7）丢失，后端消费链就绪收不到参数；其余 7 大项 PASS + 强制核验 6/6 全过；观察项 O-WS-B3-1~4 登记）→ 生成 P1-FIN-WS-003-R1 → QA 复验 PASS（`docs/quality/ui-ws-b3-r1-qa-report.md`，2026-09-04：voucher.ts:43-45 补数字分支与 QA 指定方向逐字符一致、字符串分支语义零改动、端到端消费链 5 环节全链就绪、后端零改动 mtime 实锤、typecheck 136=136 + build EXIT=0；R1 观察 3 条登记）→ 新增正式基线 REG-FIN-WS-003（正式 89 → 90 条，只增不减、既有条目零修改）；候选 0 条维持；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）；PD-028/033/035 + KL-060 为 BLOCKED 登记不占通过数；本批为治理批次（非发布批次），不进上线门禁判定。** **2026-09-04 更新（Batch WS-4，合规工作台治理批次）：P1-FIN-WS-004 合规工作台 QA 独立验收 PASS（`docs/quality/ui-ws-b4-qa-report.md`，2026-09-04：验收标准 8 条全达成 + 强制核验 6/6 全过，无 FAIL、无 -R——五区布局待办区真实字段派生不虚构 / 工具栏打印 window.print 真实接入 + 导出/批量缴税登记 / KL-059 税期动态化 Node 行为实证 6/6（跨年 + 与硬编码等价）/ 筛选消费链端到端零死参数 / 增强能力接入 defaultPageSize=100 + density + useSummary unit='yuan' + CaliberNoteBar / 缴税幂等 handlePayTax（PD-001）零触碰 / 金额口径零变更；观察项 O-WS-B4-1~3 如实登记不销号）→ 新增正式基线 REG-FIN-WS-004（正式 90 → 91 条，只增不减、既有条目零修改）；候选 0 条维持；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）；PD-028/PD-035 为 BLOCKED 登记不占通过数（PD-001 为已决策落地保持零触碰）；导出/批量缴税为登记依赖非 FAIL（EXPORT-001 + P1-FIN-EXPORT-004 后端池 + 无批量端点，disabled+tooltip 明示不伪造）；本批为治理批次（非发布批次），不进上线门禁判定。** **2026-09-23 更新（Batch ORDER-A1，订单号唯一性发布批次）：正式 114 → 121（新增 REG-ORDER-001~007，来源 QA 独立验收 PASS_WITH_LIMITATION `docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md` + 回归独立抽验 PASS，只增不减、既有 114 条零修改）；候选 0 条维持；正式基线 FAIL=0 维持；PWL 5 项（L-01~L-05）逐条确认不阻断本地放行（L-01 生产不可达 → 本地放行 / 生产 PROVISIONAL）。** **2026-09-24 更新（Batch P1-COMBO-ORDER-001，套餐下单/KDS 组合套餐发布批次）：正式 121 → 125（新增 REG-ORDER-008~011，来源 QA 独立验收 PASS_WITH_LIMITATION `docs/quality/P1-COMBO-ORDER-001-qa-report.md`（验收 6/6 PASS、FAIL=0、无 -R{n}）+ DS 抽检 §11 READY_FOR_QA 6/6 + 回归 Regression Result PASS，只增不减、既有 121 条零修改）；候选 0 条维持；正式基线 FAIL=0 维持；PWL 限制逐条确认不阻断（L-01 UI 目检 → 回归阶段补浏览器断言 / L-02 生产证据 PROVISIONAL → 仅阻断生产放行 / L-06 legacy 三文件第二步卡范围）。**

---

## 回归门禁（Release Gate）

> 正式发布门禁证据。每次发布前必须核对：所有基线案例状态为 PASS / PASS_WITH_LIMITATION，且 FAIL 为 0，方可放行。

| 门禁项 | Regression Result | 依据 | 日期 |
|-------|-------------------|------|------|
| Sprint-1 数据污染专项 | PASS | 独立验收 5/5 PASS（REG-DATA-001~005） | 2026-08-10 |
| Sprint-2 财务真实性 + HR 业务假成功治理 | PASS | 独立验收 15/15 通过（含限制），FAIL 0（REG-FIN-001~004、REG-HR-001~011、REG-RULE-001~003） | 2026-08-10 |
| Sprint-3A 第一阶段（权限组） | PASS | 复审验收 8/8 通过（PASS 5 / PASS_WITH_LIMITATION 3 / FAIL 0，REG-SEC-001~008）；3 项 PASS_WITH_LIMITATION 逐条确认不阻断发布：L-1 低敏感提报表单页域级超放行（越权面低）；L-2 渗透用例已执行并通过（2026-08-10 P1-SEC-009 活体渗透 PASS：伪造 token 后端拒绝 401、未知角色 fail-closed 403、/me 失败登出、越权直达 403 均 HTTP 实测通过，KL-007 满足关闭条件，见 REG-SEC-004）；L-5 仍为联调待办（真实登录联调，KL-013 回归阶段闭环，不放大越权面）；L-3 静态校验脚本缺失、L-4 权限表数据待联调（不放大越权面）、L-6 后端 4 文件清单维护性不一致（仅 admin 分支生效 + '*' 兜底，运行期无差异） | 2026-08-10（复审）；2026-08-10 L-2 关闭更新 |
| Sprint-3B-1 数据真实性治理（P1-DATA） | PASS | 独立验收 9/9 通过（PASS 4 / PASS_WITH_LIMITATION 5 / FAIL 0，REG-DATA-006~014）；5 项 PASS_WITH_LIMITATION 逐条确认不阻断发布：L-1（建议记录假历史）已由 P1-MOCK-004 承接闭环（假历史删除→空态+标注，见 REG-MOCK-003）；L-2 会签状态本地 mock 仅展示层、不影响提交数据；L-3 统计卡 0 值已注释说明、未伪造接口；L-4 类型声明未对齐（运行时安全）；L-5 无组织切换 UI 如实声明；V-1/V-2 联调验证缺口登记待回归阶段闭环 | 2026-08-10 |
| Sprint-3B-2 Mock/EXPORT 治理（P1-MOCK / P1-EXPORT） | PASS | 独立验收 6/6 通过（PASS 3 / PASS_WITH_LIMITATION 3 / FAIL 0，REG-MOCK-001~005、REG-EXPORT-002）；P1-MOCK-010 经 -R1 复验转 PASS（复验结论追加于报告尾部）；3 项 PASS_WITH_LIMITATION 逐条确认不阻断发布：V-1（采购申请落库/采购侧可见）、V-2（CSV 文件内容解析）、V-3（通知已读运行期联调）均为无运行环境的联调验证缺口，代码级契约核验通过、无假成功、方向 fail-safe，登记待回归阶段闭环；O-1 注释过宽 / O-2 后端占位（能力缺口已禁用+标注、无假成功）、O-4 报名员工硬编码（Sprint-2 遗留非本批引入）均不放大假成功面 | 2026-08-10 |
| Sprint-3B-3 前端域 4 卡（P1-API-002 / P1-API-004 / P1-API-005 / P1-DATA-011） | PASS | 独立验收 4/4 通过（PASS 4 / FAIL 0，REG-API-001~003、REG-DATA-015）；4 项验收缺口逐条确认不阻断发布：L-1（无运行时故障注入，代码走查级证据）、L-2（正文生成成功路径运行时不可达，仅代码级验证）、L-3（供应商列表无运行库验证，契约已核验）、L-4（「详情页手动生成」能力未核验，既有文案非本卡引入）、L-5（仓库级 vue-tsc 243 个既有错误非本批引入）均不放大假成功面，登记待回归阶段补验 | 2026-08-10 |
| Sprint-3A 第二阶段（P0-SEC-002 运维高危接口鉴权） | PASS | 独立验收 1/1 通过（PASS_WITH_LIMITATION 1 / FAIL 0，REG-SEC-009）；6 组 37/37 端点 @PreAuthorize + @OperationLog 全覆盖（三重独立核验）、data-fix 4 写操作 confirm 二次确认、编译 EXIT=0，代码级闭环；限制 L-1（运行时 403/200 实测缺失）、L-2（审计落库实测缺失）为验证缺口，登记待回归/渗透阶段补验；L-3（maintenance 包 10 个未鉴权方法）由 P0-SEC-001-D 承接闭环；L-4（`*` 权限放行）为既有安全模型设计（`*` 仅 ADMIN/Z/admin 角色展开），登记待数据审计阶段核验 —— 逐条确认不阻断发布 | 2026-08-10 |
| Sprint-3A 第二阶段（P0-SEC-001 全后端 Controller 鉴权） | PASS | 独立验收 4/4 子批通过（A/B/C/D 全部 PASS_WITH_LIMITATION / FAIL 0，REG-SEC-010）；限制逐条确认不阻断发布：L-1（403 运行实测缺失，同 KL-007 处置、登记待回归/渗透阶段补验）、L-2（CI 管道未落地，--strict 门禁逻辑实测可用）；排除项均有闭环路径——34 设计豁免（KL-043 豁免清单化）、30 BLOCKED（PD-006~008 产品决策池登记）、范围外 17（ApprovalWorkflow 13 / QuickStockIn 3 / MiniProgram 1，待后续批次） | 2026-08-10 |
| Sprint-3C 代码卫生（P2-CLEAN-005 / P2-CLEAN-006） | PASS | 独立验收 2/2 通过（PASS 2 / PASS_WITH_LIMITATION 0 / FAIL 0，REG-CLEAN-001~002）；红线（BLOCKED_PRODUCT_RULE：PD-001/002/003）未触碰、未解除；观察项 O-1~O-3 逐条确认不影响放行（O-2 已同步更新 REG-FIN-001 限制说明，见「限制说明」；O-1/O-3 为其他模块既有日志/规格措辞，非本批模块） | 2026-08-10 |
| DEF 修复批次（P0-DATA-009 / P0-MOCK-001 / P0-DATA-010） | PASS | 独立复验 3/3 通过（PASS 2 / PASS_WITH_LIMITATION 1 / FAIL 0，REG-DATA-016~017、REG-MOCK-006）；**此前 FAIL 明细（DEF-1/2/3/4）全部消除**：DEF-1/DEF-2 经 QA 复验 PASS_WITH_LIMITATION（`p0-data-009-r1-qa-report.md`）+ KL-020 重跑活体闭环；DEF-3 经 QA 复验 PASS（`p0-mock-001-r1-qa-report.md`）+ KL-031 重跑活体闭环；DEF-4 经 QA 复验 PASS（`p0-data-010-r1-qa-report.md`，7/7）。PWL 逐条确认不阻断发布：KL-048（admin 趋势接口空数据=数据可见性缺陷非越权，架构裁决中，与 DEF-2 同根因区分，见 REG-DATA-016）【**2026-08-11 更新：KL-048 已修复闭环**，见下方「KL-048 修复闭环复评」行】；浏览器验证缺口 KL-013/KL-030/020-V2/031-R1（无浏览器自动化环境，为验证缺口非 FAIL，待发布前浏览器冒烟补验） | 2026-08-10 |
| KL-048 修复闭环复评（O-DEF2-1 → KL-048 修复批次） | **PASS（KL-048 项）** | QA 独立复验 `docs/quality/kl-048-r1-qa-report.md` = **PASS_WITH_LIMITATION（2026-08-11）**：4/5 PASS（diff 范围 3 文件 4 处 / 语义 null=不限制 / 活体 4 接口 / 非 null 分支零改动）；编译项 KL-048 三文件本身 PASS（javac 全量错误列表不含三文件），限制=工作区 device 模块编译破坏（非 KL-048 引入，已由 P0-DEVICE-001/DEF-B 修复中）；活体证据：live-monitor/trend revenue=[3000.0]、decision-board/revenue-trend 非空、kpi-summary currentValue=3000.00 与 DB SUM=300000 分精确一致、**kpi-summary?storeIds=1 200 code=0 无 500 无 NPE**。**REG-DATA-016 唯一限制（KL-048）关闭 → 转 PASS（2026-08-11）**；新增基线 **REG-DATA-018**（运营趋势 admin 可见 + null 防御）。**KL-048 项从 Production Release Gate WAITING 条件中消除** | 2026-08-11 |
| 浏览器冒烟补验门禁（2026-08-11，详见文末「Sprint-3 浏览器冒烟补验门禁记录」） | **FAIL** | 活跃缺陷 **DEF-A（P0-ASSET-001）**/ **DEF-B（P0-DEVICE-001）** 修复中（冒烟实测：资产页白屏 / 设备·告警接口 500）；**FAIL > 0 禁止放行**；KL-030 折旧/告警导出抽样待修复后重跑闭环 | 2026-08-11 |
| DEF-A/B 修复闭环复评 + KL-030 冒烟重跑（2026-08-11，详见文末「KL-030 冒烟重跑闭环记录」） | **PASS** | 活跃缺陷全部关闭：**DEF-A**（P0-ASSET-001，QA 复验 PASS `p0-asset-001-r1`）+ **DEF-B**（P0-DEVICE-001，QA 复验 PASS `p0-device-001-r1`）；**KL-030 冒烟重跑全抽样 PASS**（`docs/quality/p0-defab-kl030-rerun.md`：折旧导出 CSV 9 列 + fenToYuan 200.00 断言 / 告警导出 CSV 9 列 + 告警内容断言 / 资产三页冒烟 / 设备三接口 code=0 / 造数 0 残留）；新增基线 **REG-ASSET-001 / REG-DEVICE-001**（58 → 60 条）；**FAIL = 0 → 回归门禁允许放行**；剩余发布条件 = **PD-001/003 产品决策（及 002/004）**（上线门禁范畴，见 `docs/quality/final-release-gate-sprint3.md`） | 2026-08-11 |
| **最终全量回归（Sprint-3 发布收口，阶段②）** | **PASS** | **57 条基线全量执行（PASS 31 / PASS_WITH_LIMITATION 26 / FAIL 0）**：执行方式组合=代码级核验（grep/diff/静态，REG-CLEAN-001/002、REG-SEC-001/003/004、REG-FIN-001/002、REG-DATA-001/002/005 等）+ 活体抽样（登录→/me→业务接口→数据一致性：/me 340 权限、overview、recharge-stats、notifications、finance、trend 六接口 200 code=0，覆盖 REG-SEC-004/REG-DATA-013/017/REG-MOCK-006/REG-FIN-001/REG-DATA-016）+ 已验收结论复核（引用各 QA/回归报告，见 `docs/quality/final-release-gate-sprint3.md`）；PWL 26 项逐条确认不阻断发布（阻断语义 6 项 = PD-001/002/003/004 决策依赖，属**上线门禁**范畴，见 PWL 状态快照；本阶段变化：REG-DATA-013 DEF-4 子项关闭【P0-DATA-010 QA 复验 PASS 7/7 → REG-DATA-017】，REG-MOCK-005 V-3 闭环【KL-031 重跑活体 PASS】）；历史 FAIL（DEF-1/2/3/4）全部关闭且活体复验通过，回归基线无 FAIL 状态条目 | 2026-08-10 |
| Sprint-4 首批（决策落地：P0-FIN-002 缴税幂等 PD-001 / P0-HR-002 报名唯一性 PD-002 / P0-FIN-001 期初录入+审计 PD-003 / P0-HR-005 HR 智能 3 页面下线 PD-004） | **PASS** | 独立验收 4/4 通过（PASS 3 / PASS_WITH_LIMITATION 1 / FAIL 0：REG-FIN-005、REG-FIN-006、REG-HR-012、REG-HR-013，新增 60 → 64 条）；**此前 PWL 六项决策依赖全部销号（REG-RULE-001/002/003、REG-FIN-001、REG-HR-002、REG-HR-004，2026-08-11）**：PD-001 幂等键=税种+所属期+凭证号（同键→返回已有记录+命中日志+唯一索引兜底，REG-FIN-005）；PD-002 唯一性键=员工+课程（同键拒绝已报名+DB 无第二条+原状态保留，REG-HR-012）；PD-003 不自动结转+期初人工录入+审计（人/时间/原因，REG-FIN-006）；PD-004 HR 智能 3 页面下线（菜单/路由无入口+URL 拦截+dist 无产物，REG-HR-013）；PWL 逐条确认不阻断发布：REG-HR-012 限制=无 DB 唯一索引并发竞态（低概率）+ schema 无期次字段（键=员工+课程已透明记录）；REG-HR-013 观察=admin URL 直达空白页（无 catch-all 为既存状态，不渲染演示数据）；QA 缺口（并发压测/前端已报名展示/浏览器 URL 直达断言）登记待后续批次或发布前冒烟 | 2026-08-11 |
| RM-B2 实施卡 1（ETM-106）+ OIC-BE Batch 1 REV（发布后独立治理编排） | **不进 Release Gate（OIC 轨口径）** | 当前批次（OIC-2 Batch 2 / RM-B2 / OIC-BE）为**发布后独立治理编排**：按 OIC 轨口径不进 Release Gate、不构成门禁判定；REV 基线更新为**持续积累**——REG-ETM-106 已转正（RM-B2 实施卡 1 QA PASS 2026-08-16，正式 64 → 65）；REG-ETM-001/002/003、REG-KL055 **已转正**（OIC-BE Batch 1 QA 独立验收 PASS 2026-08-16 `docs/quality/oicbe-b1-qa-report.md`：三卡全 PASS、无 FAIL、无 -R1；卡 3 附限制 L-1 逐条确认不阻断发布；正式 65 → 69、候选清空）；正式基线 FAIL=0 维持，历史 Release Gate 结论（Sprint-4 首批 PASS）不变 | 2026-08-16 |
| OIC-BE 裁决落地断言扩展登记（PD-014/016 落库 + PD-015 阶段一） | **不进 Release Gate（OIC 轨口径）** | REV 基线**持续积累**：REG-ETM-001/002/003、REG-KL055 四条目断言扩展登记（基于已验收 PASS_WITH_LIMITATION 结论 `docs/quality/oicbe-b1-impl2-qa-report.md` + `docs/quality/oicbe-b1-phase1-qa-report.md`，2026-08-16 裁决落地后）——REG-ETM-001 扩展 SQL 层链路断言（insert/findByCode/updateToUsed/countValidCodes 独立验证，20260816.002 落库后）、HTTP 成功路径断言**待 DR-01**（OnboardingRecord 实体 createdAt/updated_at/interview_id 列漂移，Batch 2+ 排卡）；REG-KL055 扩展读面断言（GET /v1/finance/budgets → code:0 正常空列表，护栏放行独立复验，20260816.003/004 落库后）、写面断言**待 DR-02**（budget_name NOT NULL，Batch 2+）+ 表-only 列保留不删事实登记；REG-ETM-002/003 维持「明确错误态」断言，阶段二各批 QA PASS 后逐批扩展真实数据断言；**4 条目 PWL 逐条确认不阻断**（DR-01/02 为范围外登记项、非本批引入，不构成 FAIL；**R-2 Mapper 修复部署生效已于 2026-08-16 19:1x 主机重启后实测销号**——运行实例 jar 字节码 + SQL 层回滚验证，见 observation-log.md §6.5）；正式基线 FAIL=0 维持，历史 Release Gate 结论（Sprint-4 首批 PASS）不变 | 2026-08-16（R-2 生效 2026-08-16 19:1x 补记） |
| RM-B2 实施卡 3（idx_inventory_batch_no 索引落库）+ OIC-2 Batch 4（employee 断流如实提示）REV | **不进 Release Gate（OIC 轨口径）** | REV 基线**持续积累**：① **REG-ETM-106 断言扩展**——RM-B2 实施卡 3 QA 独立验收 PASS 2026-08-18（`docs/quality/rm-b2-impl3-qa-report.md`：rank 193=20260817.001 success=t、checksum -916957038 独立重算 4/4 MATCH、pg_indexes count=1 幂等、indisvalid=t、EXPLAIN Index Scan；L1678「本基线不覆盖索引落库断言」过时口径同步更新）；② **REG-EMP-001 转正**——OIC-2 Batch 4 三卡 QA 独立验收 PASS 2026-08-18（`docs/quality/oic2-b4-qa-report.md`：余额/考勤/锁屏/公告断流如实提示文案保留断言，dist 命中 1/1/1/2，防假数据/假成功 reintro；正式 69 → 70）；③ **OIC-BE 阶段二批 1 待 QA**——OICBE-B2-001/002/003 developer 开发完成、QA 报告 `docs/quality/oicbe-b2-qa-report.md` 并行产出中，REG-ETM-002/003 扩展候选如实标注待 QA（不推测）；正式基线 FAIL=0 维持，历史 Release Gate 结论（Sprint-4 首批 PASS）不变 | 2026-08-18 |
| Batch CORE-1 治理批次（P1-UI-CORE-001/002/003，core 增强，2026-08-31） | **PASS**（治理批次，非发布批次） | QA 独立验收 3/3 PASS（`docs/quality/ui-core-b1-qa-report.md`：FAIL=0、无 -R、无 BLOCKED；强制核验点 6 项全过；build EXIT=0、typecheck 本批 7 文件零新增错误（存量 136 条非本批引入））；新增基线 **REG-UI-CORE-001/002/003**（正式 70 → 73 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**；QA 观察项 3 条如实登记不销号（紧凑收敛副产物 / SearchPanel reset 语义边界 / 操作列 fixed 既有未提交改动 + typecheck 存量 136 条建议，详见条目「限制说明/观察项」与 `docs/quality/batch-core1-regression-gate.md`）；本批为治理批次，按治理口径不进上线门禁判定 | 2026-08-31 |
| Batch CORE-2 治理批次（P1-UI-CORE-004/005/006/007，core 增强，2026-08-31） | **PASS**（治理批次，非发布批次；2026-08-31 R1 闭环转正） | QA 独立验收 **3/4 PASS + 1 FAIL**（`docs/quality/ui-core-b2-qa-report.md`，2026-08-31：004/005/006 PASS、007 **FAIL**（F-1：_dark-mode.scss:105-145 无 overdue/abnormal 深色值，深色主题下新键回退浅色值、与既有键深色适配不同构）→ 生成 **P1-UI-CORE-007-R1** 返回 DOING）；新增基线 **REG-UI-CORE-004/005/006/007**（正式 73 → 76 → **77 条**，只增不减、既有条目零修改）；007 首验 FAIL → -R1 → **QA 复验 PASS（`docs/quality/ui-core-b2-r1-qa-report.md`，2026-08-31，F-1 闭环）**后转正固化；正式基线 FAIL=0 维持（4/4 卡全部基线化、无 FAIL 状态条目）、PWL 0 项；**转正判定依据**：007 转正条件满足（R1 修复 + QA 复验 PASS）→ 门禁 CONDITIONAL PASS 转正式 **PASS**；**PD-028**（BLOCKED：资金流水「待核销」状态语义缺失，决策前不实现）如实登记不销号——影响面=第三步 P1-UI-FIN-001 接入约束（007-R1 约束已解除，仅剩 PD-028 单项），本批基线不受影响（REG-UI-CORE-007 条目内注明限制观察）；QA 观察项 O-1/O-2/O-3 + 遗留 5 条 + R1 复验报告 R-1/R-2/R-4（WCAG 对比度未实测等）如实登记不猜测处置（详见文末 Batch CORE-2 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定 | 2026-08-31 |
| Batch FIN-1 治理批次（P1-UI-FIN-001/002，财务 4 页第一批，2026-08-31） | **PASS**（治理批次，非发布批次） | QA 独立验收 2/2 PASS（`docs/quality/ui-fin-b1-qa-report.md`，2026-08-31：两卡全 PASS、无 FAIL、无 -R；强制核验点：业务语义零变更 ✅（git diff API 调用集合 3 处一致）/ Batch0-方案3 对齐 5 点全核验一致 ✅ / 视觉同源 ✅（两页面 + 4 文件 grep 硬编码颜色零匹配、frontend-design-v3 零引用）/ typecheck 136=136 + build EXIT=0 ✅）；新增基线 **REG-UI-FIN-001/002**（正式 77 → 79 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028 为 BLOCKED 不占通过数**，如实登记不销号）；QA 观察项 O-FIN-1~4 如实登记不销号、不猜测处置（expand 入口变化 / 筛选恢复时序 / 错误文案增强 / 后端 POSTED→VOID 前端无按钮属既有行为，详见文末 Batch FIN-1 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-08-31 |
| Batch FIN-2 治理批次（P1-UI-FIN-003/004，财务 4 页第二批，2026-08-31） | **PASS**（治理批次，非发布批次） | QA 独立验收 2/2 PASS（`docs/quality/ui-fin-b2-qa-report.md`，2026-08-31：两卡全 PASS、无 FAIL、无 -R；应收 8 项清单全核验 + **overdue 键接入真实字段依据**（converters.ts:153-166 后端 status 4=overdue）+ 业务语义零变更（git diff API 调用集合 2 处一致）+ expand 与弹层同字段 11 项；应付同口径（API 3 处一致 + expand 同字段 13 项）+「待付款」无独立字段不发明（既有统计卡片 label diff 零变更）；两页视觉同源 ✅（grep 硬编码颜色零匹配、frontend-design-v3 零引用）/ typecheck 136=136 + build EXIT=0 ✅）；新增基线 **REG-UI-FIN-003/004**（正式 79 → 81 条，只增不减、既有条目零修改）；**三步走专项收口**：新增基线累计 **70 → 81 = 11 条**全部在案（REG-UI-CORE-001~007 七卡 + REG-UI-FIN-001~004 四卡）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028 为 BLOCKED 不占通过数**，如实登记不销号）；QA 观察项 O-FIN-B2-1~3 如实登记不销号、不猜测处置（expand 入口变化 / 筛选恢复时序 / 分页 size 变化回第一页属 core 006 统一契约，同 Batch FIN-1 O-FIN-1~3 口径，详见文末 Batch FIN-2 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-08-31 |
| Batch TRACE-1 治理批次（P1-UI-TRACE-C01/C02/C03，数据血缘专项前置·契约对齐，2026-09-03） | **PASS**（治理批次，非发布批次） | QA 独立验收 3/3 PASS（`docs/quality/ui-trace-b1-qa-report.md`，2026-09-03：三卡全 PASS、无 FAIL、无 -R；三卡共用强制核验 7 项全过——git diff 纯类型+转换器（无接口调用/状态判断/金额口径/业务逻辑变更）、不新增后端接口、视觉同源、金额口径复用既有 Converter（fenToYuanNumber/computeAging）、PD-028 未落库合规（status 恒「未知」兜底为登记项非 FAIL，FundFlowStatusMap 维持登记链）、P1-STOCK-001 零触碰、typecheck 136=136 + build EXIT=0 独立复跑）；新增基线 **REG-UI-TRACE-C01/C02/C03**（正式 81 → 84 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（PD-028 为 BLOCKED 不占通过数，如实登记不销号）；QA 观察项 O-TRACE-1~5 如实登记不销号、不猜测处置（凭证制单人/审核人/摘要列待后端填充 / voucherNo/status/invoiceDate/invoiceNo 兜底项 / 工作区多批次累计未提交以三重替代核验 / 凭证页既有分→元口径，详见文末 Batch TRACE-1 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-03 |
| Batch TRACE-2 治理批次（P1-UI-TRACE-A01/CL01，数据血缘专项·来源展示+口径，2026-09-03） | **PASS**（治理批次，非发布批次） | QA 独立验收 2/2 PASS（`docs/quality/ui-trace-b2-qa-report.md`，2026-09-03：两卡全 PASS、无 FAIL、无 -R；两卡共用强制核验 7 项全过——纯展示+契约映射+跳转（新增段 grep axios/request/fetch/http 零命中）、后端零改动（盘点报告引用后端行号与今日读码完全一致未漂移）、视觉同源（仅 var(--fts-*) tokens + EP 组件，V3-A 零引用）、金额口径复用既有 Converter（本批零金额逻辑）、PD-028 未落库合规（columns 无 status/勾稽列，status 恒「未知」兜底为登记项非 FAIL）、P1-STOCK-001 零触碰、typecheck 136=136 + build EXIT=0 独立复跑）；新增基线 **REG-UI-TRACE-A01/CL01**（正式 84 → 86 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（PD-028 为 BLOCKED 不占通过数，如实登记不销号）；QA 观察项 O-TRACE2-1~5 如实登记不销号、不猜测处置（developer 证据行号 ±1~±8 偏差内容一致 / 工作区多批次累计未提交以三重替代核验 / 来源单据号 remark 首个子句文本兜底不可结构化穿透（盘点 §6.1 L172 既定事实）/ 凭证→单据第二跳为文本展示+跳转登记（GAP-B2）/ 查看凭证链接 voucherId 为空时隐藏，详见文末 Batch TRACE-2 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-03 |
| Batch TRACE-3 治理批次（P1-UI-TRACE-D01/B01/BE01，数据血缘专项·异常醒目+依赖落位，2026-09-03） | **PASS**（治理批次，非发布批次） | QA 独立验收 **3/3 合规**（`docs/quality/ui-trace-b3-qa-report.md`，2026-09-03：P1-UI-TRACE-D01 异常行醒目 = **PASS**（改码卡）；P1-UI-TRACE-B01 勾稽占位 / P1-UI-TRACE-BE01 后端缺口登记 = **PASS（登记合规口径）**（占位/登记卡，不占通过数）；无 FAIL、无 -R；三卡共用强制核验 7 项全过——D01 git diff 纯展示行类+样式（rowClassName + `:row-class-name` 绑定 + scoped `.row-overdue`，grep axios/request/fetch/http 零命中，`row.status === 'overdue'` 为展示性行类选择零新增状态逻辑）、后端零改动（5 处证据行号与盘点一致未漂移）、视觉同源（仅 var(--fts-status-overdue-bg) tokens 三元组 + StatusTag overdue 既有键，V3-A 零引用）、金额口径零变更（零 fenToYuan 新增）、PD-028 未落库合规（FinanceFund.vue 零改动 + getFlowStatusTag 恒「未知」兜底 + FundFlowStatusMap 悬空声明维持登记链）、P1-STOCK-001 零触碰、typecheck 136=136 + build EXIT=0 独立复跑）；新增基线 **REG-UI-TRACE-D01**（正式 86 → 87 条，只增不减、既有条目零修改）；**B01/BE01 为占位/登记卡不固化基线条目**（无行为断言），依赖说明登记在案（B01 = Batch0-方案2 落库后放行实现并追加基线；BE01 = 后端池排期，决策前不实现不猜测）；PWL 0 项、FAIL=0 → **允许放行**（PD-028 为 BLOCKED 不占通过数，如实登记不销号；B01/BE01 登记卡不占通过数）；QA 观察项 O-TRACE3-1~5 如实登记不销号、不猜测处置（converters 行号 ±15 偏差内容一致 / 工作区多批次累计未提交以三重替代核验 / GAP-B3 fundFlowNo 创建路径瞬态填充·查询路径恒空（GAP-D3 待核实登记链如实）/ V20260625_002 简写 / D01 纯 CSS 覆盖无浏览器实测建议回归阶段冒烟补验，详见文末 Batch TRACE-3 段条目「限制说明/观察项」）；**数据血缘专项三步走收口**：新增基线累计 **81 → 87 = 6 条**全部在案（C01/C02/C03 + A01/CL01 + D01，无缺卡、无预固化条目），**专项完成，等待产品负责人走查**（§9.3 硬约束 4，不自动开新批）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-03 |
| Batch WS-1 治理批次（P1-FIN-WS-001 对账工作台·阶段三 Batch 1 样板页，2026-09-03） | **PASS**（治理批次，非发布批次） | QA 独立验收 **1/1 PASS**（`docs/quality/ui-ws-b1-qa-report.md`，2026-09-03：验收标准 8/8 + 强制核验 6/6 全过、无 FAIL、无 -R——五区布局齐备（待办区 PD-028 登记不虚构计数 + 4 卡片真实数据）/ 修正①边界合规（收付执行动作零新增，grep 零命中）/ 工具栏三项处置正确（新建=真实端点接线 fundFlowApi.create、导出=disabled+tooltip 明示 EXPORT-001 结论、日结=disabled+tooltip 明示确认⑨依赖 PD-028）/ E 值格式修复到位（value-format=YYYY-MM-DD 字符串化 + restoreDateRange 归一 + 后端 LocalDate 兼容）/ 失败透传三件套 + 引导型空态 / create 接线与后端 FundFlowCreateDTO 逐字段一致（delete status/voucherNo，金额口径零变更）/ 增强能力 9 项零回归（A01/D01/CaliberNoteBar/density/固定列/expand/useSummary/pagination/筛选保存）/ 菜单路由仅标题变更（路径/组件/scaleLevel 零变更）；typecheck 136=136（本批 5 文件零命中）+ build EXIT=0 独立复跑）；新增基线 **REG-FIN-WS-001**（正式 87 → 88 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028 为 BLOCKED 不占通过数**，如实登记不销号；**导出/日结为登记依赖非 FAIL**——EXPORT-001 盘点结论（无导出端点→后端池 P1-FIN-EXPORT-004）+ 确认⑨（日结=勾稽锁定）+ PD-035 决策待决，按钮 disabled+tooltip 明示，不伪造动作）；QA 观察项 O-WS-1~5 如实登记不销号、不猜测处置（统计卡片双口径并存 / 筛选 key 与设计稿初稿差异 / 空态 action 文案 / 工作区多批次累计未提交以三重替代核验 / permissions.ts:742 权限码描述未随标题变更，详见文末 Batch WS-1 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-03 |
| Batch WS-2 治理批次（P1-FIN-WS-002 核销工作台·阶段三 Batch 2，2026-09-03） | **PASS**（治理批次，非发布批次） | QA 独立验收 **1/1 PASS**（首验 `docs/quality/ui-ws-b2-qa-report.md`：7 大项中 6 大项 PASS + 强制核验 6/6 全过，唯一 FAIL 点 F-1 = 应付「到期日区间」筛选死参数假筛选（前端发送 startDueDate/endDueDate → PayableQueryDTO 绑定 → **PayableServiceImpl:111-113 未传 mapper → PayableMapper.xml 无 due_date 过滤**，结果零变化）→ 生成 P1-FIN-WS-002-R1；**复验 `docs/quality/ui-ws-b2-r1-qa-report.md` = PASS（F-1 闭环）**——控件 disabled+tooltip 明示（PayableTab.vue:403-418）/ loadData 零死参数发送（:199-200，grep 全文件无 params.startDueDate/endDueDate 赋值）/ handleFilterRestored+reset 三路径恒 null（:222/:242，旧存档不回填）/ 创建日期区间端到端有效保持（:195-198→Service:111-113→XML:16-21 create_time）/ 应收侧到期日区间零触碰且端到端有效（:185-188→Service:103-105→XML:16-21 due_date）/ 后端零改动（4 Java mtime=08-11、2 XML=08-31，mtime+git diff 双证据）/ 后端池登记落位（任务池 L3694：Service 补传 + Mapper 参数 + XML due_date 过滤 + O-WS-B2-5 命名统一 + 前端反向操作）/ typecheck 136=136 + build EXIT=0）；新增基线 **REG-FIN-WS-002**（正式 88 → 89 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028/033/035 为 BLOCKED 登记不占通过数**，如实登记不销号；**应付到期日筛选=登记处置非 FAIL**——后端缺口登记后端池排期（L3694），控件 disabled+tooltip 明示，不伪造筛选；导出/批量=登记依赖非 FAIL——EXPORT-001 结论 + 无批量端点登记，按钮 disabled+tooltip 明示）；QA 观察项 O-WS-B2-1~6 + R1-OBS-1/2 如实登记不销号、不猜测处置（应收分页后端硬编码 O-1 建议后端池 / 待办摘要失败态文案 O-2 / 行号注记偏差 O-3 / 多批次未提交 O-4 / mapper 参数命名 O-5（已随 F-1 同池登记）/ GAP-C4 延续 O-6 / hasActiveFilter 死分支 R1-OBS-1 / 验证方法限制 R1-OBS-2，详见文末 Batch WS-2 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-03 |
| Batch WS-3 治理批次（P1-FIN-WS-003 记录工作台·阶段三 Batch 3，2026-09-04） | **PASS**（治理批次，非发布批次） | QA 独立验收 **首验 FAIL（F-1）→ -R1 复验 PASS（F-1 闭环）**（`docs/quality/ui-ws-b3-qa-report.md`，2026-09-04：页签化合并（修正②）✅ / 路由重定向（确认①）+ A01 穿透目标修复 ✅（/finance/records + 3 旧路由带 tab 重定向 + /finance 根 redirect + FinanceFund.vue:316-319 name FinanceRecords + { tab: 'ledger', voucherId } + LedgerTab onMounted voucherId 定位）/ 工具栏处置 ✅（批量过账真实接入 batchPost 既有端点 + 仅 audited 勾选 + BatchResultFeedback 提交数口径 + tooltip 明示后端仅布尔；红冲/作废真实接入 invoice.ts 既有端点 + 二次确认，审批流保留确认⑥；联动规则 CRUD 真实接入 transfer-template.ts 既有端点 + getLeafSubjects）/ 筛选修复 4/5 ✅（voucherNo / voucherStatus 键名修正端到端有效 / 期间 ✅、referenceNo 死参数零发送登记 ✅、联动 B-3/B-4 映射修正端到端有效 ✅、报销四维 ✅）+ **唯一 FAIL 点 F-1 = 凭证类型筛选死参数假筛选**（LedgerTab.vue:141 发送数字 voucherType 7 值语义 1-7 → voucher.ts mapQueryParams:36 解构丢弃 + :43-45 仅字符串分支 → 后端收不到参数，消费链断点在**前端 API 映射层**）→ 生成 P1-FIN-WS-003-R1；**复验 `docs/quality/ui-ws-b3-r1-qa-report.md` = PASS（F-1 闭环）**——voucher.ts:43-45 补数字分支（**数字 1-7 直传后端 7 值语义、字符串分支 VoucherTypeMap 映射语义不变**）/ 端到端消费链 5 环节全链就绪（LedgerTab.vue:141 → voucher.ts:43-45 → FinanceVoucherQueryDTO.java:26 → VoucherServiceImpl.java:231-233 → FinanceVoucherMapper.xml:19-21）/ 后端零改动（6 文件 mtime 全 08-11 + git status 零修改）/ typecheck 136=136 零新增 + build EXIT=0）/ KL-060 登记不猜测 + KL-061 伪语义列移除 ✅ / 增强能力零回归（C02 键/voucherId 穿透/expand/useSummary unit='fen'/Batch0-方案3 零变更）✅ / 强制核验 6/6 全过；typecheck 136=136 + build EXIT=0 独立复跑）；新增基线 **REG-FIN-WS-003**（正式 89 → 90 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028/033/035 + KL-060 为 BLOCKED 登记不占通过数**，如实登记不销号；**referenceNo 筛选/批量审核/凭证导入/导出/batch-post N/M 明细 = 登记依赖非 FAIL**——后端池登记，disabled+tooltip 明示/零发送，不伪造）；QA 观察项 O-WS-B3-1~4 + R1 复验观察 R-WS-B3-R1-1~3 如实登记不销号、不猜测处置（developer 证据行号偏差 / 后端池登记延续 / 联动规则统计卡片派生口径 / 多批次未提交验证限制 / 空串行为既有不变 / mtime 边界替代核验 / 读码级实证边界，详见文末 Batch WS-3 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-04 |
| Batch WS-4 治理批次（P1-FIN-WS-004 合规工作台·阶段三 Batch 4，2026-09-04） | **PASS**（治理批次，非发布批次） | QA 独立验收 **1/1 PASS**（`docs/quality/ui-ws-b4-qa-report.md`，2026-09-04：验收标准 8 条全达成 + 强制核验 6/6 全过、无 FAIL、无 -R——五区布局（待办区 pendingTaxSummary 真实字段派生不虚构 + PD-001 登记卡 + 统计卡片保留）/ 工具栏处置（申报表打印 window.print 真实接入 + 导出/批量缴税 disabled+tooltip 登记，不伪造、不逐条循环）/ **KL-059 税期动态化**（buildTaxPeriodOptions 当前月起往前 4 期、YYYYMM 契约零变更、Node 行为实证 6/6【跨年 + 与硬编码等价】、Tab2 同类修复、硬编码 20260x 零残留）/ 筛选消费链端到端（taxPeriod/taxType/taxStatus 5 环全链有效，零死参数，Batch 2/3 教训零复发）/ 增强能力（分页 defaultPageSize=100 与既有一致 + density + useSummary unit='yuan' + Tab3 CaliberNoteBar + 原自定义 el-pagination 移除）/ 缴税幂等 handlePayTax（PD-001）零触碰 / 金额口径零变更 / 后端零改动（mtime 08-11/08-31 实锤）/ typecheck 136=136（本批 3 文件零命中）+ build EXIT=0 独立复跑）；新增基线 **REG-FIN-WS-004**（正式 90 → 91 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**PD-001 为已决策落地（REG-FIN-005）本批零触碰保持**；**导出/批量缴税为登记依赖非 FAIL**——EXPORT-001 结论 + 后端池 P1-FIN-EXPORT-004 + 无批量端点，按钮 disabled+tooltip 明示，不伪造动作、不逐条循环）；QA 观察项 O-WS-B4-1~3 如实登记不销号、不猜测处置（统计卡片初始加载时序【08-31 同构非本批引入】/ 工作区多批次未提交三重替代核验 / role-profiles·permissions 标题文案未扩散，详见文末 Batch WS-4 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-04 |
| Batch WS-5 治理批次（P1-FIN-WS-005 毛利核算 + P1-FIN-WS-006 决策工作台·阶段三 Batch 5，2026-09-04） | **PASS**（治理批次，非发布批次） | QA 独立验收 **1/1 PASS + 1 FAIL → -R1 复验 PASS（F-1 闭环）**（首验 `docs/quality/ui-ws-b5-qa-report.md`，2026-09-04：**P1-FIN-WS-005 毛利核算 = PASS**（无 FAIL 点、无 -R——五区布局 + 统计卡片口径与旧版逐字一致 + **B-5 月期间筛选端到端 6 环零死参数**（monthrange+value-format YYYY-MM → loadData → cost.ts mapQueryParams → CostRecordQueryDTO → CostServiceImpl ge/le(period) → SQL）+ **summarizeByPeriod 真实接入**（既有端点 GET /v1/finance/costs/summary 非新增）+ 成本结构分析视图真实 + 导出/导入 disabled+tooltip 登记（P1-FIN-EXPORT-004 + 无 import 端点）+ PD-032 口径标注（CaliberNoteBar 与决策池原文逐字一致）+ 失败透传 + 引导空态 + 增强能力零回归 + 菜单标题「成本管理」→「毛利核算」路径零变更）；**P1-FIN-WS-006 决策工作台 = FAIL（F-1）**（cost_structure 统计卡片 3/4 张金额口径错误缩小 100 倍——元值传入 formatFenToYuan 二次 /100，违反 money.ts 强制规范）→ 生成 P1-FIN-WS-006-R1；**复验 `docs/quality/ui-ws-b5-r1-qa-report.md` = PASS（F-1 闭环）**（四卡统一分→元一次转换 :274-277，Node 独立复算 ¥1,000.00/¥600.00/¥200.00/¥200.00 + 分口径自洽断言 EXIT=0，gridRows/其他 6 类报表卡片/report.ts/types 零改动，mtime 边界实锤，typecheck 136=136 + build EXIT=0）；强制核验 6/6 全过（WS-005 首验全过 + WS-006 R1 后全过）；新增基线 **REG-FIN-WS-005/006**（正式 91 → 93 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**P0 断流 P1-FIN-BE-001 / 导出 P1-FIN-EXPORT-004 / 后端占位（账龄恒 0、预算待实现）/ 穿透不可行 / PD-028/PD-035 为登记依赖非 FAIL**——仅登记不伪造、不混入；**PD-032 为已决策口径标注（与 REG-UI-TRACE-CL01 同源）**）；QA 观察项 O-WS-B5-1~3 + R1 归因限制如实登记不销号、不猜测处置（initDefaultDateRange 依赖 periodName 格式 / summary 键 7 兜底标签 / permission.ts 159 行历史未提交产物 / R1 工作树未提交以三重证据归因，详见文末 Batch WS-5 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-04 |
| Batch WS-6 治理批次（P1-FIN-WS-007 会计科目 / P1-FIN-WS-008 会计期间 / P1-FIN-WS-009 预算 / P1-FIN-WS-010 审批流配置·阶段三 Batch 6 归位页，2026-09-04） | **PASS**（治理批次，非发布批次） | QA 独立验收 **4/4 PASS**（`docs/quality/ui-ws-b6-qa-report.md`，2026-09-04：四卡全 PASS、无 FAIL、无 -R、PWL 0 项——**WS-007** C 类 3 处假筛选根除（subjectCode/subjectName 双输入 + subjectType/status 数字转换，消费链 6 环零死参数）+ pruneTreeByMatchedIds 树形剪枝 + default-expand-all + subjectId→id 断链修复 + 余额不伪造（VO 无 balance，PD-031 联动登记）+ 导出/导入登记；**WS-008** KL-058 根除（computed 会话取值零兜底 + 三动作防御 + 按钮 disabled+tooltip，`?? '1'` 零残留）+ 后端 operatorId 校验缺口登记后端池 + 年份筛选死参数登记不补 UI + 结账动作语义零变更（accounting-period.ts 零改动）+ 导出登记；**WS-009** 预算类型筛选补 UI（1~5 枚举，消费链 7 环零死参数）+ responsibleDeptId/categoryId 死参数/无源登记不补 UI（F-1 同型纪律）+ 审批入口 PD-034 登记（BLOCKED 不占通过数）+ 双轨语义漂移登记 + 导出登记；**WS-010** B-1/B-2 映射修正（enabled→isEnabled/configName→keyword，消费链 7 环零死参数）+ 字段级契约补齐（nodes/enabled 断链修复）+ CRUD 4 端点真实接入（端点集合零新增）+ toggleEnabled 契约修正（补 enabled 参数，原缺参 400）+ 审批执行链路未实现登记 + 导出登记；强制核验 6/6 全过——git diff 纯契约映射/展示/UI 接入（端点集合零新增：subject.ts 7 个、approval-flow.ts 8 个既有端点，getById 为既有 GET /{id} 封装）/ 后端零改动（13 文件 mtime 实锤全 ≤ 08-31）/ 视觉同源（var(--fts-*) 全 token、V3-A 零引用、filteredTree/filterTree 本地过滤零残留）/ 死参数零发送纪律（期间年份/responsibleDeptId/categoryId 三处登记不补 UI）+ P1-STOCK-001 零触碰 / typecheck 136=136（本批 8 文件零命中）+ build EXIT=0 独立复跑 / 开发者不自行宣布通过——本报告为唯一验收结论）；新增基线 **REG-FIN-WS-007/008/009/010**（正式 93 → 97 条，只增不减、既有条目零修改）；**阶段三收口核对**：任务型重构 6 工作台 10 卡全部 QA 通过、REG-FIN-WS-001~010 全部在案（Batch 1：001 / Batch 2：002 / Batch 3：003 / Batch 4：004 / Batch 5：005+006 / Batch 6：007~010；阶段三累计 87 → 97 = 10 条），**等待产品负责人走查**（§9.3 硬约束 4，不自动开新批）；PWL 0 项、FAIL=0 → **允许放行**（**PD-031/PD-034/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**科目余额 VO 缺 balance / KL-058 后端侧校验缺口 / 期间年份筛选 / responsibleDeptId 死参数 / categoryId 无数据源 / 审批执行链路未实现 / 科目导入无端点 / 审批人无用户目录数据源 = 登记依赖非 FAIL**——仅登记不伪造、不混入）；QA 观察项 O-B6-1~4 如实登记不销号、不猜测处置（Subject 类型枚举注释差异 6 值 vs 5 值 / Approval CRUD 按钮无 v-permission（后端 @PreAuthorize 强校验）/ 任务卡行号引用偏差（内容一致）/ getList size=1000 口径，详见文末 Batch WS-6 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-04 |
| Batch RVT-1 治理批次（P1-FIN-RVT-001 路由/菜单全局回退 + P1-FIN-RVT-004 标题回退批，2026-09-04） | **PASS**（治理批次，非发布批次） | QA 独立验收 **2/2 PASS_WITH_LIMITATION**（`docs/quality/ui-rvt-b1-qa-report.md`，2026-09-04：两卡全 PWL、无 FAIL、无 -R；RVT-001 回退范围全部达成且与 HEAD 基线逐行同构【13 页独立组件直达路由恢复 / reconciliation·records 路由 + 5 条重定向移除（grep 零命中 + dist 无工作台 chunk）/ 13 项原菜单标题图标 scaleLevel 恢复 / 工作台入口三重零残留 / 5 独立页壳新建承功能】；RVT-004 四页标题三处同步【fund 资金管理 / tax 税务管理 / cost 成本管理 / report 财务报表】+ permissions/role-profiles 文案零漂移 + 页面内增强 15/15 零回退；typecheck 136=136 本批 11 文件零命中 + build EXIT=0 独立复跑）；新增基线 **REG-FIN-RVT-001/004**（正式 97 → 99 条，只增不减、既有条目零修改；WS 基线仅标注接管不删除——§12.4 方案）；**PWL 2 项逐条确认不阻断发布**：① 穿透瞬态——QA 补充发现 FinanceReport drillTo 13 处 + developer 已登记 FinanceFund:318 A01 穿透均指向已移除路由（按钮点击无效，功能缺失非数据错误），归 **RVT-002** 承接修复，登记性质不构成 FAIL；② subject/budget 标题差异（HEAD 原命名 vs §5.13 清单简称）待产品走查，不实施不猜测；注释残留（types/finance.ts:1274 / FinanceTax.vue:974 等）登记备查；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-04 |
| Batch RVT-2 治理批次（P1-FIN-RVT-002 核销页回退 + 2 项穿透修复，2026-09-05） | **PASS**（治理批次，非发布批次） | QA 独立验收 **1/1 PASS**（`docs/quality/ui-rvt-b2-qa-report.md`，2026-09-05：无 FAIL、无 -R；壳/页签删除零残留（glob/dist 零残留 + emit 链路移除）/ 内容回迁独立页完整（575/595 行全文读码逐项对应）/ 保留项 15/15 零回退 / 待办区页面级增强（pendingSummary 真实字段派生不虚构）/ A01+drillTo 2 项穿透修复闭环（/finance/reconciliation?tab= 零残留）/ 强制核验 6/6 全过（typecheck 136=136 本批 4 文件零命中 + build EXIT=0 独立复跑））；新增基线 **REG-FIN-RVT-002**（正式 99 → 100 条，只增不减、既有条目零修改；WS 基线接管标注保持不删除——REG-FIN-WS-002 合并断言 + REG-FIN-WS-003 A01 断言由本基线接管覆盖）；**B1 PWL① 穿透瞬态限制消除**（A01/drillTo 恢复可用，B1 报告 §五.1 风险解除）；登记/观察项 4 条如实登记不销号、不猜测处置（行数证据微差 / FinanceRecords.vue:132 注释 RVT-003 范围 / FinanceTax.vue:974·TransferTemplateTab.vue:6 既往保持 / 待办区当前页口径待走查）；PWL 0 项、FAIL=0 → **允许放行**；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-05 |
| Batch RVT-3 治理批次（P1-FIN-RVT-003 记录页回退，2026-09-05） | **PASS**（治理批次，非发布批次） | QA 独立验收 **1/1 PASS**（`docs/quality/ui-rvt-b3-qa-report.md`，2026-09-05：无 FAIL、无 -R；壳/页签删除零残留（glob/dist 零残留 + :132 注释清理闭环 + emit 链路移除）/ 内容回迁独立页完整（FinanceLedger 785 行 / InvoiceReimbursement 660 行 / AutoVoucher 519 行，全文读码与 WS-003 页签承载清单一一对应）/ 保留项 12/12 零回退（batchPost 仅 audited+BatchResultFeedback 提交数口径 / 红冲作废+审批流保留 / 联动 CRUD+B-3-B4 映射 / voucherStatus 键名+voucherType 数字分支【WS-003-R1 修复逐字符保持】/ referenceNo 零发送 / 筛选 4 维 / expand+穿透接收端 / 失败透传 / KL-060·061 登记）/ scaleLevel 折叠移除由菜单档位控制（menu.ts chain-enterprise）/ 待办区页面级增强（真实字段派生不虚构）/ 强制核验 6/6 全过（typecheck 136=136 本批 3 文件零命中 + build EXIT=0 独立复跑 + dist 3 chunk 存在零残留））；新增基线 **REG-FIN-RVT-003**（正式 100 → 101 条，只增不减、既有条目零修改；WS 基线接管标注保持不删除——REG-FIN-WS-003 合并断言由本基线接管覆盖）；**回退专项收口声明**：结构回退 3 批 4 卡（RVT-001/002/003/004）全部 QA 通过、REG-FIN-RVT-001~004 全部在案（专项累计 97 → 101 = 4 条回退基线，只增不减、既有 100 条零修改；WS 基线 10 条标注接管不删除）、功能修复保留确认，**等待产品负责人逐页走查**（roadmap §5.13 硬约束，不自动开新批）；登记/观察项 5 条如实登记不销号、不猜测处置（行数证据微差 / 注释残留 7 处备查【3 回迁登记 + 4 既往备查】/ FinanceTax el-tabs 范围外 / record.ts 孤立 API 文件 / 待办区当前页口径待走查）；PWL 0 项、FAIL=0 → **允许放行**；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-05 |

| Batch LRVT-1 治理批次（P1-FIN-LRVT-001/002，布局级回退·穿透成对样板批，2026-09-05） | **PASS**（治理批次，非发布批次） | QA 独立验收 **2/2 PASS**（`docs/quality/ui-lrvt-b1-qa-report.md`，2026-09-05：两卡全 PASS、无 FAIL、无 -R；布局与 HEAD 同构【Fund：PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→详情 el-dialog→末尾 B3 弹层；Ledger：PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→VoucherFormDialog→详情 el-dialog】+ 待办区/工具栏/SearchPanel/#expand 模板样式零残留【仅注释】+ 重放修复点全有效【Fund B1~B6：E 值格式/失败透传/新建流水【#extra+既有端点 fundFlowApi.create】/来源+穿透→FinanceLedger?voucherId/筛选持久化 fts_search_/口径标注 PD-031·032；Ledger B1~B7：筛选 4 维端到端【voucherStatus 键名/voucherType 数字分支】/批量过账【batchPost 既有端点+audited 过滤+提交数口径】/打印/穿透键+接收端/失败透传/持久化/KL-061 登记】+ 处置③分录 el-table 回迁【HEAD 原 5 列+借贷合计+displayRecords 转换】+ 能力层保留【Fund：density/show-summary unit='yuan'/固定列；Ledger：density/selectable/show-summary unit='fen' 账本口径/固定列】+ 穿透成对不断链【Fund:390-393↔Ledger:547-552 同批成对】+ 登记类入口 disabled+tooltip 挂 #extra【Fund 导出 EXPORT-001/日结 PD-028；Ledger 批量审核/导出/凭证导入】+ 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）；新增基线 **REG-FIN-LRVT-001/002**（正式 101 → 103 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**导出/日结/批量审核/凭证导入/批量过账 N/M 明细为登记依赖非 FAIL**——EXPORT-001/P1-FIN-EXPORT-004/无批量端点/后端布尔契约，disabled+tooltip 明示或提交数口径，不伪造）；QA 观察项 5 条如实登记不销号、不猜测处置（存储键按 SearchPanel 前缀约定【fts_search_ 语义等价】/ 无浏览器活体【留走查】/ 批量过账提交数口径【后端布尔】/ 金额绝对值 Batch0-方案2 范围【本批零变更】/ developer 证据行号漂移，详见文末 Batch LRVT-1 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-05 |
| Batch LRVT-2 治理批次（P1-FIN-LRVT-003/004，布局级回退·应收应付同构批，2026-09-05） | **PASS**（治理批次，非发布批次） | QA 独立验收 **2/2 PASS**（`docs/quality/ui-lrvt-b2-qa-report.md`，2026-09-05：两卡全 PASS、无 FAIL、无 -R；布局与 HEAD 同构【Receivable：PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→ReceiptDialog→ReceiptHistoryDialog；Payable：PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→PaymentDialog→PaymentHistoryDialog】+ 待办区/工具栏/SearchPanel/#expand 模板样式零残留【仅注释】+ 重放修复点全有效【Receivable B1~B8：到期日筛选端到端【:161-163/:435-444→QueryDTO→ReceivableServiceImpl:103-105→Mapper due_date】/坏账核销【writeOff 既有端点 ReceivableController:72-76+二次确认+行内 actions】/核销进度列【receivedAmount 后端实锤 Service:130】/逾期高亮【能力层 overdue 键】/打印/失败透传/筛选持久化 fts_search_/导出批量登记；Payable B1~B8：创建日期筛选端到端【:172-174/:442-451→QueryDTO→PayableServiceImpl:111-113→Mapper create_time】/**到期日登记处置保持【F-1 防回归：恒 null+零死参数+不回填+disabled+tooltip，不因回退变回假筛选** :61-62/:176-177/:236/:454-469】/核销进度列【paidAmount 后端实锤 Service:138】/逾期高亮/打印/失败透传/持久化/导出批量登记】+ 处置③ 详情回迁【Receivable 11 字段 / Payable 13 字段，与 HEAD 顺序逐一一致】+ 能力层保留【density/show-summary unit='yuan'/固定列/逾期高亮】+ 口径标注 CaliberNoteBar【Receivable :454 / Payable :479】+ actions-width【Receivable 220→280 属性级承载第 4 按钮；Payable 280 与 HEAD 一致】+ 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）；新增基线 **REG-FIN-LRVT-003/004**（正式 103 → 105 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**应付到期日端到端启用为登记依赖非 FAIL**——后端池排期（QA F-1 / P1-FIN-WS-002-R1 登记链），前端登记处置保持零死参数；**导出/批量为登记依赖非 FAIL**——EXPORT-001 / P1-FIN-EXPORT-004 / 无批量端点，disabled+tooltip 明示，不伪造）；QA 观察项 6 条如实登记不销号、不猜测处置（逾期状态动态计算不持久化=HEAD 既有 / Mapper 参数名错位=后端既有 / hasActiveFilter 死检查无害 / 无浏览器活体【留走查】/ developer 证据行号漂移 / 既往条目断言过时衔接，详见文末 Batch LRVT-2 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-05 |
| Batch LRVT-3 治理批次（P1-FIN-LRVT-005/007，布局级回退·税务成本批，2026-09-05） | **PASS**（治理批次，非发布批次） | QA 独立验收 **2/2 PASS**（`docs/quality/ui-lrvt-b3-qa-report.md`，2026-09-05：两卡全 PASS、无 FAIL、无 -R；布局与 HEAD 同构【Tax：PageHeader#extra→stats 顶层→el-tabs 3 页签【Tab1/Tab2 筛选+表格、Tab3 筛选+表格+独立 pagination-wrapper 恢复】→4 详情对话框→配置对话框；Cost：PageHeader#extra→stats→advanced-search-panel【monthrange 重做】→table-section→pagination-wrapper→详情 el-dialog 8 字段→CostFormDialog→B2 分析对话框末尾】+ 待办区/工具栏/SearchPanel/:pagination 模板样式零残留【仅注释】+ 重放修复点全有效【Tax B1~B7：getRecords 端到端三 eq 实锤【TaxRecordServiceImpl:44-56】/payTax+PD-001 幂等零触碰【:79-90】/KL-059 税期动态化 YYYYMM 契约零变更【:57-86】/提交电子税务局失败透传/申报表加载失败提示/useSummary unit='yuan'/teleported+默认申报期动态化；Cost B1~B6：**monthrange 端到端**【YYYY-MM→cost.ts mapQueryParams:41-42→CostServiceImpl:63-68 ge/le 实锤，真实筛选非本地过滤】/成本结构分析【summarizeByPeriod 既有端点+fenToYuanNumber 分→元一次转换，无二次 /100】/失败透传双态/CaliberNoteBar PD-032/合计行 unit='yuan'/导出导入登记】+ 处置③ 详情回迁【Cost 8 字段与 HEAD 顺序一致】+ 能力层保留【density/show-summary unit='yuan'】+ **api 层/后端零改动**【mtime 时间戳核验：后端 8/11、8/31，api 8/31、9/4 均早于本批 9/5；9/5 10:55-10:56 仅 FinanceTax/FinanceCost 两文件被修改】+ 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）；新增基线 **REG-FIN-LRVT-005/007**（正式 105 → 107 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**导出/批量缴税/导入为登记依赖非 FAIL**——EXPORT-001 / P1-FIN-EXPORT-004 / 无批量端点 / 无 import 端点，disabled+tooltip 明示，不伪造）；QA 观察项 4 条如实登记不销号、不猜测处置（payTax 未传 voucherNo=WS 批既有状态【幂等键不完整走正常新增】/ Tab1 无分页 UI 与 HEAD 一致【pageSize=100 参数载体非死参数】/ 分析聚合金额=后端既有分口径【一次转换无二次 /100】/ 无浏览器活体【留走查】，详见文末 Batch LRVT-3 段条目「限制说明/观察项」）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-05 |
| Batch LRVT-4 治理批次（P1-FIN-LRVT-006/008/009，布局级回退·财务报表/自动凭证/发票报销批【专项最后一批】，2026-09-05） | **PASS**（治理批次，非发布批次） | QA 独立验收 **3/3 PASS**（`docs/quality/ui-lrvt-b4-qa-report.md`，2026-09-05：三卡全 PASS、无 FAIL、无 -R；布局与 HEAD 同构【Report：PageHeader#extra→stats 固定 4 卡网格【repeat(4,1fr) 逐字一致】→advanced-search-panel【9 类 select+monthrange】→table-section→pagination-wrapper 独立分页恢复；AutoVoucher：PageHeader#extra→stats→advanced-search-panel【原手写筛选区与 HEAD 逐字同构】→table-section→pagination-wrapper→详情 el-dialog→B1 新建/编辑规则对话框末尾；InvoiceReimbursement：PageHeader#extra 新建报销回原位置→stats→advanced-search-panel【手写筛选区+value-format YYYY-MM-DD】→table-section→pagination-wrapper→详情/审批/新建报销 el-dialog】+ 待办区/工具栏/SearchPanel/#expand 模板样式零残留【仅注释】+ 重放修复点全有效【Report B1~B7：9 类端点聚合【后端 ReportController 恰 7 端点 :30-83 无 balance-sheet/cash-flow = 7 有效+2 P0 断流实锤】/KL-057 P0 断流空态不伪造【P1-FIN-BE-001 登记】/后端占位说明【账龄恒 0 :156-176/预算待实现 :220-230 后端实锤】/drillTo 19 处→4 独立路由【router/index.ts:117-120 实锤】/打印/失败透传+重试/**B7 WS-006-R1 金额口径重放【cost_structure 四卡 :271-274 分→元一次转换，无二次 /100，F-1 防回归**】/导出登记；AutoVoucher B1~B6：CRUD 既有端点【transfer-template.ts:85-107 + TransferTemplateController:30-47】+getLeafSubjects+二次确认/B-3-B4 映射端到端【api 层 mapQueryParams:28-41 零改动 mtime 9/3→QueryDTO:19/:22→Controller:58-61】/KL-061 伪语义列移除【VO 断链实证】/契约对齐【templateId 数字直传+toggleEnabled enabled 参数 Controller:66 必填实锤】/失败透传/打印/筛选持久化 fts_search_；InvoiceReimbursement B1~B6：红冲/作废既有端点【invoice.ts:137-152 + InvoiceController:137-142/:152-157 + 状态机 :28】+issued 态行内入口+二次确认/E 值格式/失败透传/打印/筛选持久化/KL-060 登记不猜测【驳回=作废保持既有登记链】】+ 能力层保留【Report：density/useSummary unit='yuan'/固定列/CaliberNoteBar；AutoVoucher：density/独立分页/统计卡片/DataTable 自动操作列；Invoice：density/独立分页/统计卡片】+ 后端/API 层零改动【mtime 实锤】+ 强制核验 6/6 全过 + typecheck 136=136 三文件零命中 + build EXIT=0 独立复跑）；新增基线 **REG-FIN-LRVT-006/008/009**（正式 107 → 110 条，只增不减、既有条目零修改）；PWL 0 项、FAIL=0 → **允许放行**（**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**导出等为登记依赖非 FAIL**——P1-FIN-EXPORT-001→004 登记链，disabled+tooltip 明示，不伪造）；QA 观察项 6 条如实登记不销号、不猜测处置（income_expense 3 卡网格 1 空位 / balance·cash_flow 零值卡为 B2 修复行为 / budget rate*100 后端恒 0 / id String 化既有模式 / 审批驳回=作废既有登记链 / 无浏览器活体【留走查】，详见文末 Batch LRVT-4 段条目「限制说明/观察项」）；**布局回退专项收口声明**：9 页布局级回退 + 功能修复重放全部 QA 通过（Batch LRVT-1~4 累计 9/9 PASS，REG-FIN-LRVT-001~009 全部在案，专项累计 101 → 110 = 9 条，只增不减、既有 107 条零修改；纯 B 4 页 LRVT-010~013 关闭不产生基线），**等待产品负责人逐页走查**（roadmap §5.14 硬约束，完成后停止，不自动开新批）；本批为治理批次，按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变 | 2026-09-05 |

**Regression Result: PASS**（2026-08-10 最终全量回归，FAIL=0，回归门禁允许放行；上线门禁另见 `docs/quality/final-release-gate-sprint3.md`）

**Regression Result（2026-08-11 复评）：FAIL**——KL-048 修复闭环复评 **PASS**（KL-048 项已从 Production Release Gate WAITING 条件消除：REG-DATA-016 唯一限制关闭转 PASS、新增 REG-DATA-018），但**冒烟门禁 FAIL = DEF-A（P0-ASSET-001）/ DEF-B（P0-DEVICE-001）活跃缺陷（修复中）**——FAIL > 0 禁止放行；剩余门禁条件：**PD-001/PD-003 产品决策 + DEF-A/B 修复闭环**。

**Regression Result（2026-08-11 终评）：PASS**——DEF-A/B 修复闭环复评 **PASS**（QA 复验 `p0-asset-001-r1` / `p0-device-001-r1` = PASS；KL-030 冒烟重跑 `docs/quality/p0-defab-kl030-rerun.md` 全抽样 PASS：折旧导出 CSV 9 列 + fenToYuan 200.00、告警导出 CSV 9 列 + 告警内容断言、资产三页冒烟、设备三接口 code=0、造数 0 残留、devices QA-TEST-* 残留物理清理 0）；新增基线 REG-ASSET-001 / REG-DEVICE-001（58 → 60 条）；**FAIL = 0 → 回归门禁允许放行**；剩余发布条件：**PD-001/PD-003 产品决策（及 002/004）**（上线门禁范畴，见 `docs/quality/final-release-gate-sprint3.md`）。

**Regression Result（2026-08-11 Sprint-4 首批复评）：PASS**——Sprint-4 首批 4 项 QA 独立验收全 PASS（`docs/quality/sprint-4-backend-qa-report.md`：P0-FIN-002 / P0-HR-002 / P0-FIN-001；`sprint-4-frontend-qa-report.md`：P0-HR-005 8/8；FAIL=0，无 -R）；**PWL 六项（REG-RULE-001/002/003、REG-FIN-001、REG-HR-002、REG-HR-004）决策依赖全部销号（PD-001~004 已决策并落地，2026-08-11）**；基线 60 → **64 条**（新增 REG-FIN-005/006、REG-HR-012/013）；**FAIL = 0 → 回归门禁允许放行**；原「剩余发布条件 PD-001/PD-003（及 002/004）」已消除。

**Regression Result（2026-08-16 REV 基线更新说明，非门禁判定）**：本轮为发布后独立治理编排（OIC-2 Batch 2 / RM-B2 / OIC-BE）的 REV 回归基线更新——正式基线 64 → **65 条**（REG-ETM-106 转正，来源 RM-B2 实施卡 1 QA 独立验收 PASS `docs/quality/rm-b2-impl1-qa-report.md`，2026-08-16）；OIC-BE Batch 1 三卡 4 条目（REG-ETM-001/002/003、REG-KL055）**登记候选**（`docs/quality/oicbe-b1-qa-report.md` 未产出，待 QA PASS 转正）；按 OIC 轨口径本轮**不进 Release Gate**，正式基线 FAIL=0 维持，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-08-16 OIC-BE Batch 1 REV 转正更新，非门禁判定）**：OIC-BE Batch 1 三卡 QA 独立验收 **PASS**（`docs/quality/oicbe-b1-qa-report.md`：三卡全 PASS、无 FAIL、无 -R1；卡 3 附限制 L-1 逐条确认不阻断发布）→ **REG-ETM-001/002/003、REG-KL055 四条目由候选转正为正式基线**（状态 PASS，来源卡 OICBE-B1-001/002/003）；正式基线 65 → **69 条**、候选清空；共同限制如实登记：**migration 未落库（PD-014/015/016 待裁决）——当前回归行为为「明确错误态」断言（无假空、无吞错），决策落库后基线需扩展真实数据断言**（卡 3 另附限制 L-1：护栏自定义提示未透传为通用「系统繁忙」，明确错误态成立、不阻断）；按 OIC 轨口径本轮**不进 Release Gate**，正式基线 FAIL=0 维持，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-08-16 裁决落地断言扩展登记，非门禁判定）**：OIC-BE 裁决落地 QA 独立验收 **PASS_WITH_LIMITATION**（`docs/quality/oicbe-b1-impl2-qa-report.md`：卡 1（PD-014）/卡 3（PD-016）全 PASS_WITH_LIMITATION、无 FAIL、无 -R1；`docs/quality/oicbe-b1-phase1-qa-report.md`：PD-015 阶段一 PASS_WITH_LIMITATION，L-1~L-4 均为文档级 minor）→ **REG-ETM-001/002/003、REG-KL055 四条目断言扩展登记**（2026-08-16 裁决落地后，基于已验收结论、禁止推测）：REG-ETM-001 扩展 SQL 层链路断言（insert/findByCode/updateToUsed/countValidCodes 独立验证通过，20260816.002 落库后）+ migration 断言（rank 190 success=t + checksum 独立重算 MATCH + 12 列 + PK + 2 索引 + onboarding_records 补 code_expiry_time），**HTTP 成功路径断言待 DR-01 处置**（Batch 2+），RegistrationCodeMapper 修复（updated_at→update_time L37/L43）**已部署生效（R-2，2026-08-16 19:1x 主机重启后线上断言补入：运行实例 jar 字节码 @Update SQL 均为 update_time + SQL 层等价语句 UPDATE 1 写入、事务回滚零残留；验证边界=HTTP 面受 DR-01 阻断，以字节码 + SQL 层为准）**；REG-KL055 扩展**读面断言**（GET /v1/finance/budgets → HTTP200 code:0 success:true 正常空列表，护栏放行独立复验，20260816.003/004 落库后，无 COUNT 短路假空）+ 表-only 列保留不删事实登记，**写面断言待 DR-02 处置**（budget_name NOT NULL，Batch 2+）；REG-ETM-002/003 维持「明确错误态」断言（catch 先行改码保持、72f0d5a 未回退），**阶段二批次迁移后逐批扩展真实数据断言**（映射表 `docs/quality/sales-order-orders-mapping.md` 为阶段二输入，各批 QA PASS 后扩展）；**4 条目 PWL 逐条确认不阻断**——DR-01/02 为范围外登记项（Batch 2+ 排卡、非本批引入），不构成 FAIL；正式基线计数不变（69 条）、FAIL=0 维持；按 OIC 轨口径本轮**不进 Release Gate**，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。**（2026-08-16 19:1x 更新：R-2 Mapper 修复部署生效待办销号——主机重启后实测生效，见 observation-log.md §6.5）**

**Regression Result（2026-08-18 REV 基线更新说明，非门禁判定）**：本轮为发布后独立治理编排（RM-B2 实施卡 3 / OIC-2 Batch 4 / OIC-BE 阶段二批 1）的 REV 回归基线更新——**REG-ETM-106 断言扩展**（RM-B2 实施卡 3 QA 独立验收 PASS `docs/quality/rm-b2-impl3-qa-report.md`，2026-08-18：migration 落库 rank 193=20260817.001 success=t + checksum -916957038 独立重算 MATCH + pg_indexes 幂等 count=1 + indisvalid=t + EXPLAIN Index Scan；L1678 过时口径更新为已落库事实）；**REG-EMP-001 转正**（OIC-2 Batch 4 三卡 QA 独立验收 PASS `docs/quality/oic2-b4-qa-report.md`，2026-08-18：三卡全 PASS、无 FAIL、无 -R1——LeavePage 余额/考勤、SecurityLockPage 锁屏、HomePage 公告区断流如实提示文案保留断言，dist 命中 1/1/1/2，防假数据/假成功 reintro；正式基线 69 → **70 条**）；**OIC-BE 阶段二批 1（OICBE-B2-001/002/003）QA 报告 `docs/quality/oicbe-b2-qa-report.md` 并行产出中（截至 2026-08-18 未产出）**——REG-ETM-002/003 扩展候选如实标注待 QA（T 项迁移后实体/mapper 对齐事实断言待 QA PASS 后登记；真实数据断言扩展待 B 项 PD-017~021 决策后），按维护规则 5 禁止推测不提前登记断言；按 OIC 轨口径本轮**不进 Release Gate**，正式基线 FAIL=0 维持，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-08-31 Batch CORE-1 治理批次，非门禁判定）**：Batch CORE-1 三卡（P1-UI-CORE-001/002/003）QA 独立验收 **PASS**（`docs/quality/ui-core-b1-qa-report.md`，2026-08-31：三卡全 PASS、FAIL=0、无 -R、无 BLOCKED；强制核验点 6 项全过；build EXIT=0、typecheck 本批 7 文件零新增错误（存量 136 条非本批引入））→ **新增正式基线 REG-UI-CORE-001/002/003（正式 70 → 73 条，只增不减、既有条目零修改）**；QA 观察项 3 条如实登记不销号、不猜测处置（紧凑收敛副产物 / SearchPanel reset 语义边界 / 操作列 fixed 既有未提交改动 + typecheck 存量 136 条建议，详见条目「限制说明/观察项」与 `docs/quality/batch-core1-regression-gate.md`）；**Regression Result: PASS**（FAIL=0、PWL=0）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，正式基线 FAIL=0 维持，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-08-31 Batch CORE-2 治理批次，非门禁判定）：CONDITIONAL PASS**——Batch CORE-2 四卡 QA 独立验收 **3/4 PASS + 1 FAIL**（`docs/quality/ui-core-b2-qa-report.md`，2026-08-31：P1-UI-CORE-004/005/006 PASS；P1-UI-CORE-007 **FAIL**（失败点 F-1：`_dark-mode.scss:105-145` 覆盖全部既有 status 变量但无 overdue/abnormal 深色值，深色主题下新键回退浅色值（#bf360c/#fbe9e7 等）、与既有键深色适配不同构，developer「深浅主题自动适配」声明与实现不符）→ 生成 **P1-UI-CORE-007-R1** 返回 DOING）→ **新增正式基线 REG-UI-CORE-004/005/006（正式 73 → 76 条，只增不减、既有条目零修改）**；**P1-UI-CORE-007 因 FAIL 本批不转基线**，仅登记「待 R1 闭环后追加 REG-UI-CORE-007」（禁止预固化）；正式基线 FAIL=0 维持（PASS 卡全部基线化、无 FAIL 状态条目）、PWL 0 项；**PD-028**（BLOCKED：资金流水「待核销」状态语义缺失，`product-decision-backlog.md:44/155` + `remediation-roadmap.md` §5.10 L253）如实登记不销号——影响面=第三步 P1-UI-FIN-001 异常状态醒目接入（另受 007-R1 双重约束），本批基线不受影响；QA 观察项 O-1/O-2/O-3 + 遗留 5 条如实登记不猜测处置（详见文末 Batch CORE-2 段）；**Regression Result: CONDITIONAL PASS**（PASS 卡 FAIL=0 可固化；007-R1 修复+QA 复验 PASS 后门禁转正式 PASS）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-08-31 Batch CORE-2 R1 闭环转正更新，非门禁判定）：PASS**——P1-UI-CORE-007-R1 QA 独立复验 **PASS（F-1 闭环）**（`docs/quality/ui-core-b2-r1-qa-report.md`，2026-08-31：深色值 8/8 一致（_dark-mode.scss L137-143 + L154-155：#ff7043/#33140a/#5a2a12、#f06292/#2a0f1e/#5a2240）+ 与既有 12 键同构（color/bg/border 三元组 + purple 先例边框块复声明）+ light/solid 变体 6 变量全覆盖零悬空 + 浅色 _tokens.scss/StatusTag.vue 零改动 + typecheck 136=136 + build EXIT=0 + 构建产物 6 深色值 in html.dark block + PD-028 未触碰）→ **REG-UI-CORE-007 转正固化（正式 76 → 77 条，只增不减、既有条目零修改）**，Batch CORE-2 四卡（004/005/006/007）全部基线化、无 FAIL 状态条目；**Batch CORE-2 门禁 CONDITIONAL PASS → 正式 PASS**（007 转正条件满足：R1 修复 + QA 复验 PASS；PASS 卡 FAIL=0、PWL=0 维持）；**PD-028**（BLOCKED）继续如实登记不销号——007-R1 约束已解除，第三步 P1-UI-FIN-001 约束收窄为仅剩 **PD-028** 单项；QA 观察项（含 R1 复验报告 R-1/R-2/R-4：WCAG 对比度未实测等）如实登记不猜测处置（详见文末 Batch CORE-2 段）；**Regression Result: PASS**（治理批次口径：仅表示基线积累 76 → 77 + 正式基线 FAIL=0，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-08-31 Batch FIN-1 治理批次，非门禁判定）：PASS**——Batch FIN-1 两卡（P1-UI-FIN-001/002）QA 独立验收 **PASS**（`docs/quality/ui-fin-b1-qa-report.md`，2026-08-31：两卡全 PASS、无 FAIL、无 -R；P1-UI-FIN-001 资金流水 8 项清单全核验 + 业务语义零变更（git diff API 调用集合 3 处一致）+ 视觉同源（零硬编码颜色/零 V3-A 引用）+ typecheck 136=136 + build EXIT=0；P1-UI-FIN-002 账本/凭证 Batch0-方案3 对齐 5 点全核验一致（后端 0-3 实锤 / VoucherStatusMap 双向正确 / 错档实锤 / 按钮零改动 / 契约不变）+ 分录 expand 迁移合规（未照搬 size="small"））→ **新增正式基线 REG-UI-FIN-001/002（正式 77 → 79 条，只增不减、既有条目零修改）**；**PD-028**（BLOCKED：资金流水「待核销」状态语义缺失）继续如实登记不销号——本批 FinanceFund.vue 零新增状态判断实锤（grep + diff 双确认，getFlowStatusTag 仅注释变更），影响面= P1-UI-FIN-001 异常状态醒目接入子项，决策前不实现不猜测（不占通过数）；QA 观察项 **O-FIN-1~4** 如实登记不猜测处置（expand 入口变化 / 筛选恢复时序 / 错误文案增强 / 后端 POSTED→VOID 前端无按钮属既有行为，详见文末 Batch FIN-1 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：2/2 卡 QA PASS、FAIL=0、PWL=0（PD-028 为 BLOCKED 不占通过数），仅表示基线积累 77 → 79 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-08-31 Batch FIN-2 治理批次，非门禁判定）：PASS**——Batch FIN-2 两卡（P1-UI-FIN-003 应收 / P1-UI-FIN-004 应付）QA 独立验收 **PASS**（`docs/quality/ui-fin-b2-qa-report.md`，2026-08-31：两卡全 PASS、无 FAIL、无 -R；应收 8 项清单全核验 + **overdue 键接入真实字段依据**（converters.ts:153-166 后端 status 4=overdue 独立核验）+ 业务语义零变更（git diff API 调用集合 2 处一致）+ expand 与弹层同字段 11 项无信息丢失；应付同口径（API 3 处一致 + expand 同字段 13 项）+「待付款」无独立字段不发明（既有统计卡片 label diff 零变更）；两页视觉同源（零硬编码颜色/零 V3-A 引用）+ typecheck 136=136 + build EXIT=0）→ **新增正式基线 REG-UI-FIN-003/004（正式 79 → 81 条，只增不减、既有条目零修改）**；**三步走专项收口核对**：新增基线累计 **70 → 81 = 11 条**全部在案（REG-UI-CORE-001~007 七卡 + REG-UI-FIN-001~004 四卡，无缺卡、无预固化条目）；**PD-028**（BLOCKED：资金流水「待核销」状态语义缺失）继续如实登记不销号——两页零新增「待核销」逻辑实锤（grep + diff 双确认：应收仅注释 L45、应付零命中；「待付款」为既有统计卡片 label diff 零变更），决策前不实现不猜测（不占通过数）；QA 观察项 **O-FIN-B2-1~3** 如实登记不猜测处置（expand 入口变化 / 筛选恢复时序 / 分页 size 变化回第一页属 core 006 统一契约，同 Batch FIN-1 O-FIN-1~3 口径，详见文末 Batch FIN-2 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：2/2 卡 QA PASS、FAIL=0、PWL=0（PD-028 为 BLOCKED 不占通过数），仅表示基线积累 79 → 81 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；第三步财务 4 页（FIN-001~004）全部闭环。

**Regression Result（2026-09-03 Batch TRACE-1 治理批次，非门禁判定）：PASS**——Batch TRACE-1 三卡（P1-UI-TRACE-C01 资金流水契约对齐 / C02 凭证契约对齐 / C03 应收 Converter 补齐）QA 独立验收 **PASS**（`docs/quality/ui-trace-b1-qa-report.md`，2026-09-03：三卡全 PASS、无 FAIL、无 -R；三卡共用强制核验 7 项全过——git diff 纯类型+转换器（converters.ts 90 行 + types/finance.ts 83 行，grep axios/request/fetch/http 零命中）/ 不新增后端接口（fund-flow.ts、receivable.ts 零变更、voucher.ts 仅注释）/ 视觉同源（页面零改动）/ 金额口径复用既有 Converter（fenToYuanNumber/computeAging）/ PD-028 未落库合规（status 恒「未知」兜底为登记项非 FAIL）/ P1-STOCK-001 零触碰（grep stock/扣减/库存 零命中）/ typecheck 136=136 + build EXIT=0 独立复跑）→ **新增正式基线 REG-UI-TRACE-C01/C02/C03（正式 81 → 84 条，只增不减、既有条目零修改）**；**数据血缘专项前置核对**：Batch TRACE-1 三卡全部在案（C01/C02/C03，无缺卡、无预固化条目）；**PD-028**（BLOCKED：fund_flows.status 未落库，与 Batch0-方案2 同批落库）继续如实登记不销号——status 相关（恒「未知」兜底）为**登记项非 FAIL**（FundFlowVO 无 status 实锤 + toFrontend 不映射 status:799-801 + FundFlowStatusMap:261-272 悬空声明维持登记链），落库后由 B01 占位卡承接（不占通过数）；QA 观察项 **O-TRACE-1~5** 如实登记不猜测处置（凭证制单人/审核人/摘要列待后端填充 / voucherNo/status/invoiceDate/invoiceNo 兜底项 / 工作区多批次累计未提交以三重替代核验 / 凭证页既有分→元口径，详见文末 Batch TRACE-1 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：3/3 卡 QA PASS、FAIL=0、PWL=0（PD-028 为 BLOCKED 不占通过数），仅表示基线积累 81 → 84 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；三批全部完成后停止，等待产品负责人走查（§9.3 硬约束 4，不自动开新批）。

**Regression Result（2026-09-03 Batch TRACE-2 治理批次，非门禁判定）：PASS**——Batch TRACE-2 两卡（P1-UI-TRACE-A01 流水来源展示 + 正向穿透 / P1-UI-TRACE-CL01 口径标注条）QA 独立验收 **PASS**（`docs/quality/ui-trace-b2-qa-report.md`，2026-09-03：两卡全 PASS、无 FAIL、无 -R；两卡共用强制核验 7 项全过——纯展示+契约映射+跳转（无接口调用/状态判断/金额逻辑/业务逻辑变更，本批新增段 grep axios/request/fetch/http 零命中）/ 不新增后端接口（盘点报告引用后端行号 PaymentServiceImpl.java:162 / ReceiptServiceImpl.java:144 / FinanceVoucher.java:63 / FundFlow.java:41 与今日读码完全一致未漂移 = 后端零改动实锤）/ 视觉同源（新增样式仅 var(--fts-*) tokens + EP 组件，V3-A 零引用）/ 金额口径复用既有 Converter（本批零金额逻辑）/ PD-028 未落库合规（FinanceFund.vue columns 无 status/勾稽列，getFlowStatusTag 恒「未知」兜底保持 :61-68/:361-363 为登记项非 FAIL）/ P1-STOCK-001 零触碰（应收/应付 grep stock/扣减/库存 无本批新增命中）/ typecheck 136=136 + build EXIT=0 独立复跑）→ **新增正式基线 REG-UI-TRACE-A01/CL01（正式 84 → 86 条，只增不减、既有条目零修改）**；**数据血缘专项核对**：Batch TRACE-1 三卡 + TRACE-2 两卡全部在案（C01/C02/C03/A01/CL01，无缺卡、无预固化条目）；**PD-028**（BLOCKED：fund_flows.status 未落库，与 Batch0-方案2 同批落库）继续如实登记不销号——status 相关（恒「未知」兜底）为**登记项非 FAIL**（本批 FinanceFund.vue columns 无 status 列/无勾稽列 + getFlowStatusTag 恒「未知」兜底保持 :61-68/:361-363），落库后由 B01 占位卡承接（不占通过数）；QA 观察项 **O-TRACE2-1~5** 如实登记不猜测处置（developer 证据行号 ±1~±8 偏差内容一致 / 工作区多批次累计未提交以三重替代核验 / 来源单据号 remark 首个子句文本兜底不可结构化穿透（盘点 §6.1 L172 既定事实）/ 凭证→单据第二跳为文本展示+跳转登记（GAP-B2 无聚合接口，符合验收标准 4「可达或如实登记」路径）/ 查看凭证链接 voucherId 为空时隐藏（'-' 不虚构口径一致），详见文末 Batch TRACE-2 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：2/2 卡 QA PASS、FAIL=0、PWL=0（PD-028 为 BLOCKED 不占通过数），仅表示基线积累 84 → 86 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；三批全部完成后停止，等待产品负责人走查（§9.3 硬约束 4，不自动开新批）；下一步 Batch TRACE-3（D01 异常醒目 / B01 勾稽占位 / BE01 后端缺口登记）。

**Regression Result（2026-09-03 Batch TRACE-3 治理批次，非门禁判定）：PASS**——Batch TRACE-3 三卡（P1-UI-TRACE-D01 异常行醒目（改码卡）/ B01 勾稽状态列占位（登记合规）/ BE01 后端接口缺口登记（登记合规））QA 独立验收 **3/3 合规**（`docs/quality/ui-trace-b3-qa-report.md`，2026-09-03：D01 = **PASS**（改码卡，两页行级醒目三件套齐备 + tokens 深浅主题 + core 零改动 + FinanceFund 零改动）；B01/BE01 = **PASS（登记合规口径）**（占位/登记卡，不占通过数）；无 FAIL、无 -R；三卡共用强制核验 7 项全过——D01 git diff 纯展示行类+样式（rowClassName + `:row-class-name` 绑定 + scoped `.row-overdue`，grep axios/request/fetch/http 零命中，`row.status === 'overdue'` 为展示性行类选择零新增状态逻辑）/ 后端零改动（5 处证据行号与盘点一致未漂移 = 后端零改动实锤）/ 视觉同源（仅 var(--fts-status-overdue-bg) tokens 三元组 + StatusTag overdue 既有键，V3-A 零引用）/ 金额口径零变更（零 fenToYuan 新增）/ PD-028 未落库合规（FinanceFund.vue 零改动 columns 无 status/勾稽列 + getFlowStatusTag 恒「未知」兜底 :61-67/:362 + FundFlowStatusMap:277-281 悬空声明维持登记链）/ P1-STOCK-001 零触碰 / typecheck 136=136 + build EXIT=0 独立复跑）→ **新增正式基线 REG-UI-TRACE-D01（正式 86 → 87 条，只增不减、既有条目零修改）**；**B01/BE01 为占位/登记卡不固化基线条目（无行为断言）**，依赖说明登记在案（B01 = Batch0-方案2 落库后放行实现并追加基线；BE01 = 后端池排期，决策前不实现不猜测）；**数据血缘专项三步走收口核对**：新增基线累计 **81 → 87 = 6 条**全部在案（TRACE-1：C01/C02/C03 + TRACE-2：A01/CL01 + TRACE-3：D01，无缺卡、无预固化条目），**专项完成，等待产品负责人走查**（§9.3 硬约束 4，不自动开新批）；**PD-028**（BLOCKED：fund_flows.status 未落库，与 Batch0-方案2 同批落库）继续如实登记不销号——status 相关（恒「未知」兜底）为**登记项非 FAIL**（本批 FinanceFund.vue 零改动 + FundFlowStatusMap 悬空声明维持登记链），落库后由 B01 占位卡承接（不占通过数）；QA 观察项 **O-TRACE3-1~5** 如实登记不猜测处置（converters 行号 ±15 偏差内容一致 / 工作区多批次累计未提交以三重替代核验 / GAP-B3 fundFlowNo 创建路径瞬态填充·查询路径恒空（GAP-D3 待核实登记链如实）/ V20260625_002 简写 / D01 纯 CSS 覆盖无浏览器实测建议回归阶段冒烟补验，详见文末 Batch TRACE-3 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：3/3 卡合规、FAIL=0、PWL=0（PD-028 为 BLOCKED 不占通过数；B01/BE01 登记卡不占通过数），仅表示基线积累 86 → 87 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**数据血缘专项三步走全部完成后停止，等待产品负责人走查（§9.3 硬约束 4，不自动开新批）**。

**Regression Result（2026-09-03 Batch WS-1 治理批次，非门禁判定）：PASS**——Batch WS-1 一卡（P1-FIN-WS-001 对账工作台·阶段三 Batch 1 样板页，FinanceFund 重构）QA 独立验收 **PASS**（`docs/quality/ui-ws-b1-qa-report.md`，2026-09-03：验收标准 8/8 + 强制核验 6/6 全过、无 FAIL、无 -R——五区布局（待办区 :430-444 任务卡 + 4 统计卡片真实数据、PD-028 登记不虚构计数 / 工具栏 :446-455 / 筛选区 :457-485 / 数据区 :487-559 / 详情区 :540-557 + A01 穿透 :521-535）/ 修正①边界（paymentApi/receiptApi/writeOff 零命中）/ 新建流水真实入口（:562-597 弹层 → fundFlowApi.create fund-flow.ts:100-104 既有端点 → toCreateDTO converters.ts:843-865 与 FundFlowCreateDTO 逐字段一致，delete status/voucherNo）/ 导出 :449-451 disabled+tooltip 明示 EXPORT-001 结论 / 日结 :452-454 disabled+tooltip 明示确认⑨依赖 PD-028 / E 值格式修复（:479/:584 value-format=YYYY-MM-DD + :116-136 restoreDateRange 字符串归一，后端 FundFlowQueryDTO.java:26-32 LocalDate 兼容）/ 失败透传（:216-231 catch→loadFailed + EmptyState error 重试 :490-497）+ 引导型空态（:498-505 hasActiveFilter 区分）/ 增强能力 9 项零回归（A01/D01/CaliberNoteBar :489 / density :512 / 固定列 :99,101,105 / expand :513,540-557 / useSummary unit='yuan' :112 / pagination :516-517 / 筛选保存 :458-464 finance-fund-filter）/ 菜单标题三处「对账工作台」（:427 / menu.ts:36 / router/index.ts:119，路径/组件/scaleLevel 零变更））→ **新增正式基线 REG-FIN-WS-001（正式 87 → 88 条，只增不减、既有条目零修改）**；**PD-028**（BLOCKED：fund_flows.status 未落库）继续如实登记不销号——待办区任务卡登记不虚构计数、无勾稽状态列、日结按钮 disabled 登记态，FundFlowStatusMap 悬空声明维持登记链（B01 承接，不占通过数）；**导出/日结为登记依赖非 FAIL**（EXPORT-001 盘点结论：资金流水无导出端点→登记后端池；确认⑨：日结=勾稽锁定依赖 PD-028 落库；PD-035 导出规则待产品决策——按钮 disabled+tooltip 明示，不伪造动作）；QA 观察项 **O-WS-1~5** 如实登记不猜测处置（统计卡片「本月收入/支出」取第一个银行账户与「银行余额」全部账户之和双口径并存 / 筛选保存 key 保持 finance-fund-filter 与设计稿初稿 finance-reconciliation-filter 差异（以任务卡为准）/ 有筛选空态 action 按钮仍为「新建流水」文案 / 工作区多批次累计未提交以三重替代核验（同 O-TRACE-4）/ permissions.ts:742 权限码描述「查看资金管理」未随菜单标题变更（权限码不变，登记不改），详见文末 Batch WS-1 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：1/1 卡 QA PASS、FAIL=0、PWL=0（PD-028 为 BLOCKED 不占通过数；导出/日结为登记依赖非 FAIL），仅表示基线积累 87 → 88 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch 2 核销工作台（P1-FIN-WS-002，随批放行细化，禁止跳步）。

**Regression Result（2026-09-03 Batch WS-2 治理批次，非门禁判定）：PASS**——Batch WS-2 一卡（P1-FIN-WS-002 核销工作台·阶段三 Batch 2，FinanceReceivable + FinancePayable 合并重构）QA 独立验收 **首验 FAIL（F-1）→ -R1 复验 PASS（F-1 闭环）**（`docs/quality/ui-ws-b2-qa-report.md`，2026-09-03：7 大项中合并+双页签 / 路由重定向（确认①）/ 修正①落位 / 工具栏处置 / 应收筛选补缺 / 增强能力零回归 6 大项 **PASS** + 强制核验 **6/6 全过**（API 层零变更 / 后端零改动 mtime 实锤 / 视觉同源 V3-A 零引用 / PD-033 仅标注 + PD-028 未落库不悬空 + P1-STOCK-001 零触碰 / typecheck 136=136 + build EXIT=0 独立复跑 / 开发者不自行宣布通过）；**唯一 FAIL 点 F-1 = 应付「到期日区间」筛选死参数假筛选**——前端控件（PayableTab.vue:196-199,406-415）发送 startDueDate/endDueDate → PayableQueryDTO 绑定（字段存在）→ **PayableServiceImpl.java:111-113 未传给 mapper** → **PayableMapper.xml 无 due_date 过滤**（SQL 仅 supplier_name/status/create_time）→ 用户按到期日筛选结果零变化，违反「筛选补缺（接真实后端参数）」修复目标与「筛选无效」根治核心 → 生成 **P1-FIN-WS-002-R1** 返回 DOING；`docs/quality/ui-ws-b2-r1-qa-report.md`（2026-09-03）**复验 PASS**——① 到期日控件 disabled + el-tooltip 明示依赖（:403-418）✅ ② loadData 零死参数发送（:199-200 仅注释，grep 全文件无任何 params.startDueDate/endDueDate 赋值）✅ ③ handleFilterRestored 不回填旧存档（:242 dueDateRange: null 硬编码）+ reset 置 null（:222）——控件/恢复/重置三路径恒 null，假筛选根除 ✅ ④ 创建日期区间端到端有效保持（:195-198 → PayableServiceImpl:111-113 → PayableMapper.xml:16-21 create_time）✅ ⑤ 应收侧到期日区间零触碰且端到端有效（mtime 实锤 ReceivableTab.vue 22:51:51 早于 R1 时间窗 23:08:51 + :185-188 → ReceivableServiceImpl:103-105 → ReceivableMapper.xml:16-21 due_date）✅ ⑥ 后端零改动（4 Java 文件 mtime=08-11、2 XML=08-31 远早于本批；XML diff 仅 alias 引号化 08-31 既有内容）✅ ⑦ 后端池登记落位（任务池 L3694：PayableServiceImpl.getPage 补传 startDueDate/endDueDate + PayableMapper 加 dueDate 参数 + PayableMapper.xml 加 due_date 过滤【对照应收实现】+ O-WS-B2-5 命名统一 + 前端反向操作指引）✅ ⑧ typecheck 136=136（本批 6 文件零命中）+ build EXIT=0 ✅）→ **新增正式基线 REG-FIN-WS-002（正式 88 → 89 条，只增不减、既有条目零修改）**；**PD-028/PD-033/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号（PD-028：fund_flows.status 未落库，待办摘要为 UTM:141 任务语义基于真实 status 字段、非行状态列发明，零新增「待核销」状态列；PD-033：收款冲正仅演进标注不实现；PD-035：导出规则待决）；**应付到期日筛选为登记处置非 FAIL**（R1 闭环后控件 disabled+tooltip 明示，后端缺口登记后端池排期 L3694，不伪造筛选）；**导出/批量 = 登记依赖非 FAIL**（EXPORT-001 盘点结论：应收/应付无导出端点→登记后端池；receivable.ts 5 方法 / payable.ts 4 方法无批量端点→不接入 batchActions；按钮 disabled+tooltip 明示）；QA 观察项 **O-WS-B2-1~6 + R1-OBS-1/2** 如实登记不销号、不猜测处置（应收分页后端硬编码 current=1/size=10 O-1 建议后端池 / 待办摘要失败态「0 笔」文案 O-2 建议优化 / developer 证据行号偏差 O-3 / 工作区多批次累计未提交 O-4 以多重替代核验 / mapper 参数命名 startDate/endDate 语义实为 due_date O-5（已随 F-1 同池登记）/ GAP-C4 invoiceDate/invoiceNo 兜底延续 O-6 / PayableTab hasActiveFilter dueDateRange 死分支 R1-OBS-1（后端池就绪启用时随反向操作清理）/ 验证方法限制 R1-OBS-2，详见文末 Batch WS-2 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：1/1 卡 QA PASS（经 -R1 闭环）、FAIL=0、PWL=0（PD-028/033/035 为 BLOCKED 不占通过数；应付到期日/导出/批量为登记依赖非 FAIL），仅表示基线积累 88 → 89 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch 3 记录工作台（P1-FIN-WS-003，随批放行细化，禁止跳步）。

**Regression Result（2026-09-04 Batch WS-3 治理批次，非门禁判定）：PASS**——Batch WS-3 一卡（P1-FIN-WS-003 记录工作台·阶段三 Batch 3，FinanceLedger + AutoVoucher + InvoiceReimbursement 页签化合并）QA 独立验收 **首验 FAIL（F-1）→ -R1 复验 PASS（F-1 闭环）**（`docs/quality/ui-ws-b3-qa-report.md`，2026-09-04：页签化合并（修正②）✅（壳 = PageHeader「记录工作台」+ 待办区真实字段派生 + el-tabs 三页签 + tab query 同步 + scaleLevel 折叠确认⑤）/ 路由重定向（确认①）+ A01 穿透目标修复 ✅（/finance/records 新路由 + 3 旧路由带 tab 重定向 + /finance 根 redirect + FinanceFund.vue:316-319 name FinanceRecords + { tab: 'ledger', voucherId } 穿透修复 + LedgerTab onMounted voucherId 定位）/ 工具栏处置 ✅（批量过账真实接入 = voucherApi.batchPost 既有端点 + 仅 audited 勾选 + BatchResultFeedback 提交数口径 + tooltip 明示后端仅布尔；批量审核/导出/凭证导入 disabled+tooltip 登记；红冲/作废真实接入 = invoiceApi.void/redFlush 既有端点 + ElMessageBox 二次确认，审批流保留确认⑥；联动规则 CRUD 真实接入 = transferTemplateApi.create/update/delete 既有端点 + 科目选择器 getLeafSubjects）/ 筛选修复 4/5（voucherNo ✅ / voucherStatus 键名修正 status→voucherStatus 端到端有效 ✅ / 期间 ✅ / referenceNo 死参数零发送登记 ✅ / 联动 B-3/B-4 映射修正 enabled→isEnabled、templateName→keyword 端到端有效 ✅ / 报销四维端到端有效 ✅）+ **唯一 FAIL 点 F-1 = 凭证类型筛选死参数假筛选**——LedgerTab.vue:141 发送 number 型 voucherType（7 值语义 1-7）→ voucher.ts mapQueryParams:36 解构丢弃 + :43-45 仅字符串分支 → 后端（FinanceVoucherQueryDTO.voucherType:26 → VoucherServiceImpl.getPage:231-233 → FinanceVoucherMapper.xml:19-21）消费链就绪但收不到参数，与任务卡及 developer 证据「数字直传」不符，断点在**前端 API 映射层**（同 Batch 2 F-1 教训）→ 生成 **P1-FIN-WS-003-R1** 返回 DOING；**复验 `docs/quality/ui-ws-b3-r1-qa-report.md` = PASS（F-1 闭环）**——① voucher.ts:36 解构保留 voucherType + :43-45 补数字分支（`if (voucherType !== undefined) { result.voucherType = typeof voucherType === 'string' ? VoucherTypeMap.toBackend[voucherType] : voucherType }`，与 QA 指定修复方向逐字符一致——**数字 1-7 直传、字符串映射语义不变**）✅ ② 端到端消费链 5 环节全链就绪（LedgerTab.vue:141 → voucher.ts:43-45 → FinanceVoucherQueryDTO.java:26 → VoucherServiceImpl.java:231-233 → FinanceVoucherMapper.xml:19-21 voucher_type eq）✅ ③ 后端零改动（6 文件 mtime 全 08-11 + git status 仅 voucher.ts M）✅ ④ typecheck 136=136 零新增（finance/voucher 零命中）+ build EXIT=0 ✅）→ **新增正式基线 REG-FIN-WS-003（正式 89 → 90 条，只增不减、既有条目零修改）**；KL-060 登记不猜测（驳回=作废共用 cancelled 不发明独立状态，审批流保留既有语义）+ KL-061 伪语义列移除（「最近执行」→「创建时间」真实字段）✅；增强能力零回归（C02 契约键 referenceNo/sourceType/sourceId / voucherId 穿透 / expand 分录 / useSummary unit='fen' / 固定列 / density / 筛选保存 storage-key / CaliberNoteBar / Batch0-方案3 状态映射零变更）✅；强制核验 6/6 全过（typecheck 136=136 本批 11 文件零命中 + build EXIT=0；后端零改动 mtime 实锤；V3-A 零引用；死参数零发送；PD-028 未落库不悬空；P1-STOCK-001 零触碰；KL-059 零触碰）；**PD-028/033/035 + KL-060 为 BLOCKED 登记不占通过数**，如实登记不销号（referenceNo 筛选 / 批量审核 / 凭证导入 / 导出（P1-FIN-EXPORT-004）/ batch-post N/M 明细 = 登记依赖非 FAIL——后端池登记，disabled+tooltip 明示/零发送，不伪造）；QA 观察项 **O-WS-B3-1~4 + R-WS-B3-R1-1~3** 如实登记不销号、不猜测处置（developer 证据行号偏差（含 F-1 根因）/ 后端池登记延续 / 联动规则统计卡片当前页派生口径 / 工作区多批次累计未提交以多重替代核验 / 空串 '' 走字符串分支 = before 既有行为不触碰 / mtime 边界替代核验 / 端到端实证为读码级，详见文末 Batch WS-3 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：1/1 卡 QA PASS（经 -R1 闭环）、FAIL=0、PWL=0（PD-028/033/035 + KL-060 为 BLOCKED 不占通过数；referenceNo/批量审核/导入/导出/batch-post 明细为登记依赖非 FAIL），仅表示基线积累 89 → 90 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch 4 合规工作台（禁止跳步）。

**Regression Result（2026-09-04 Batch WS-4 治理批次，非门禁判定）：PASS**——Batch WS-4 一卡（P1-FIN-WS-004 合规工作台·阶段三 Batch 4，FinanceTax 重构 + KL-059 税期动态化）QA 独立验收 **PASS**（`docs/quality/ui-ws-b4-qa-report.md`，2026-09-04：验收标准 8 条全达成 + 强制核验 6/6 全过、无 FAIL、无 -R——五区布局（待办区 :679-702 workbench-taskboard【待缴税项任务卡 = pendingTaxSummary computed :638-650 基于 Tab1 真实字段派生【UNPAID/OVERDUE 计数 + taxAmount 合计 + 逾期摘要】，recordLoaded 未加载「加载中…」不虚构 :639/:648 + 缴税幂等 PD-001 登记卡 :686-689 纯展示 + 统计卡片 4 张保留 :691-701】/ 工具栏 :704-713【申报表打印 :706 handlePrint :655-657 = window.print 真实接入；导出 :707-709 disabled + tooltip 明示 EXPORT-001→后端池 P1-FIN-EXPORT-004；批量缴税 :710-712 disabled + tooltip 明示无批量端点】/ 筛选区三页签内保留 A 类有效 / 数据区 el-tabs 3 页签保留 :717 / 详情区既有弹层 5 个零改动）/ **KL-059 税期动态化**（buildTaxPeriodOptions :54-66 TAX_PERIOD_COUNT=4 + new Date 循环 + padStart → YYYYMM；taxPeriodOptions :70 / declarationPeriodOptions :73 / currentPeriod :76-80 + :277 默认期间=当前期；**Node vm 沙箱行为实证 6/6**【2026-09-04→202609~202606、2026-01-15 跨年→202601/202512/202511/202510、2026-06-15 与硬编码 202603~202606 集合完全等价、2025-01-10 再跨年、格式 16/16 YYYYMM 合规、Tab2 label YYYY年MM月格式一致】；YYYYMM 契约零变更【TaxRecordDTO.java:17-19 mtime 08-31 零改动 + calculate/generateReturn substring 拆分 :327-328/:367 零触碰】；硬编码 20260x 零残留【5 处命中全为注释/说明】）/ 筛选消费链端到端（taxPeriod 5 环 :116 → tax-calculation.ts:95-97 → TaxRecordController.java:44-53 → TaxRecordDTO.java:19 → TaxRecordServiceImpl.java:49-51 eq → Mapper SQL；taxType :115→Service:44-46、taxStatus :117→Service:54-56 同链；空值 undefined 不发送；**零死参数，Batch 2/3 F-1 教训零复发**）/ 增强能力（Tab1 useStandardPage defaultPageSize=100 :86【与既有 pageSize:100 一致，统计/待办金额范围零变化】+ DataTable pagination opt-in :741-751 + @page-change :222-224；density="auto" :746 + 合计 useSummary unit='yuan' :213【元→分→元同源，零自实现格式化】；Tab2 density+合计 :791-793/:318；Tab3 CaliberNoteBar :831 + density :837 + 分页统一 core :838-839【原自定义 el-pagination 移除，grep el-pagination 元素零命中，handleSizeChange/handleCurrentChange 旧代码零残留】）/ 缴税幂等 handlePayTax 零触碰（PD-001 实现 :237-268【payingRowId 单飞守卫 + ElMessageBox 二次确认 + payTax 既有端点 + finally 释放】+ :759 按钮仅 UNPAID/OVERDUE 行）/ 金额口径 formatAmount 元 toFixed(2) :167-170 零变更 / 后端零改动（TaxRecord 链 5 文件 mtime 08-31 01:20:28 + Mapper 08-11 + API 层 08-31/08-11 实锤）/ 视觉同源（core 组件 + var(--fts-*) 全 token，V3-A 零命中）/ PD-028 未落库不悬空（待缴税项摘要基于真实 taxStatus 字段 + 分页范围登记 :649，零新增状态列）/ P1-STOCK-001 零触碰 / typecheck 136=136（本批 3 文件零命中）+ build EXIT=0 独立复跑 / 独立页边界（路由 /finance/tax 保留 router/index.ts:128 + menu.ts:29 标题「合规工作台」，路径/组件/scaleLevel 零变更，role-profiles.ts:268/permissions.ts:734 零触碰））→ **新增正式基线 REG-FIN-WS-004（正式 90 → 91 条，只增不减、既有条目零修改）**；**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号（PD-028：fund_flows.status 未落库，待缴税项摘要基于真实 taxStatus 字段派生非状态列发明；PD-035：导出规则缺失待决）；**PD-001 为已决策落地（REG-FIN-005，2026-08-11）本批 handlePayTax 零触碰保持**（非 BLOCKED）；**导出/批量缴税为登记依赖非 FAIL**（EXPORT-001 盘点结论：tax 域无导出端点 → 后端池 P1-FIN-EXPORT-004（任务池 §11.6 L3951-3953）+ PD-035 决策；无批量缴税端点【tax-calculation.ts:107-109 单条 payTax + TaxRecordController.java:120-126 仅 batchDelete】→ 按钮 disabled + tooltip 明示，不伪造动作、不逐条循环调用既有端点【grep payTax 仅 handlePayTax 内 1 处 + v-for/批量循环零命中 + 无 el-table selection】）；QA 观察项 **O-WS-B4-1~3** 如实登记不销号、不猜测处置（统计卡片初始加载时序【08-31 同构非本批引入】/ 工作区多批次未提交以三重替代核验 / role-profiles·permissions 标题文案未扩散，详见文末 Batch WS-4 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：1/1 卡 QA PASS、FAIL=0、PWL=0（PD-028/PD-035 为 BLOCKED 不占通过数；导出/批量缴税为登记依赖非 FAIL），仅表示基线积累 90 → 91 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch 5 毛利核算+决策工作台（P1-FIN-WS-005，随批放行细化，禁止跳步）。

**Regression Result（2026-09-04 Batch WS-5 治理批次，非门禁判定）：PASS**——Batch WS-5 两卡（P1-FIN-WS-005 毛利核算 / P1-FIN-WS-006 决策工作台·阶段三 Batch 5）QA 独立验收 **首验 1/1 PASS + 1 FAIL（F-1）→ -R1 复验 PASS（F-1 闭环）**（`docs/quality/ui-ws-b5-qa-report.md`，2026-09-04：**P1-FIN-WS-005 毛利核算（FinanceCost 重构 + cost.ts）= PASS**——无 FAIL 点、无 -R，验收标准 8 条全达成 + 强制核验 6/6 全过（五区布局 + 统计卡片 4 张保留且计算口径与旧版 git 对照逐字一致 / **B-5 月期间筛选端到端 6 环零死参数**【monthrange+value-format YYYY-MM :308-317 → loadData :125-128 → cost.ts mapQueryParams :41-42 → CostRecordQueryDTO :26-30 → CostServiceImpl.getPage :63-67 ge/le(period) → SQL；period 格式 YYYY-MM 由 CostRecordCreateDTO @Pattern 强制，Batch 2/3 F-1 教训零复发】/ **summarizeByPeriod 真实接入**【cost.ts:112-131 补齐既有端点 GET /v1/finance/costs/summary，CostController.java:59-64 → CostServiceImpl:91-101 eq 聚合，非新增接口，后端 6 文件 mtime 08-11 零改动实锤】/ 成本结构分析视图真实 + 导出/导入 disabled+tooltip 登记【P1-FIN-EXPORT-004 + 无 import 端点】/ PD-032 口径标注【CaliberNoteBar tooltip 与 product-decision-backlog.md:48 逐字一致】/ 失败透传 + 引导空态 / 增强能力零回归【密度/合计 unit='yuan'/分页 defaultPageSize=20 与旧版一致/expand/CostFormDialog 零改动】/ 菜单标题「成本管理」→「毛利核算」路径零变更 / typecheck 136=136 + build EXIT=0 独立复跑）；**P1-FIN-WS-006 决策工作台（FinanceReport 重构 + types/finance.ts）= 首验 FAIL（F-1）**——9 类报表聚合/统计卡片仅真实数据（虚构零值消除）/利润表契约对齐（operatingIncome/details 断链修复）/穿透可行跳转+不可行「—」/P0 断流空态（KL-057/P1-FIN-BE-001 仅登记）/后端占位检测空态（账龄恒 0、预算待实现后端事实独立确认）/打印 window.print/导出登记/标题路径零变更 8 大项全 PASS + 强制核验 5/6 过，**唯一 FAIL 点 F-1 = cost_structure 统计卡片 3/4 张金额口径错误（缩小 100 倍）**：FinanceReport.vue:272-274 将已转「元」数值传入 formatFenToYuan 二次 /100（实证 totalCost=100000 分 → 总成本 ¥1,000.00 但食材成本 ¥6.00 应为 ¥600.00），违反验收标准 1/2 + money.ts 强制规范 → 生成 **P1-FIN-WS-006-R1**；**复验 `docs/quality/ui-ws-b5-r1-qa-report.md` = PASS（F-1 闭环）**——四卡统一分→元一次转换（:274-277 全部 formatFenToYuan(分)），Node 独立复算 ¥1,000.00/¥600.00/¥200.00/¥200.00 与 QA 应值一致 + 分口径自洽断言（总=食材+人工+其他）EXIT=0，gridRows cost_structure 分支/其他 6 类报表卡片/report.ts/types/finance.ts/后端 6 文件零改动（mtime 边界实锤），typecheck 136=136（FinanceReport.vue 零命中）+ build EXIT=0）→ **新增正式基线 REG-FIN-WS-005/006（正式 91 → 93 条，只增不减、既有条目零修改）**；WS-006 经 -R1 复验 PASS 转正（F-1 闭环，转正条件满足：R1 修复 + QA 复验 PASS）；**PWL 0 项、FAIL=0 → 允许放行**（**P0 断流 P1-FIN-BE-001【KL-057，仅登记不混入，后端池排期】/ 导出 P1-FIN-EXPORT-004【EXPORT-001 结论，disabled+tooltip 不伪造】/ 后端占位【账龄恒 0、预算待实现，空态说明不虚构】/ 穿透不可行【「—」登记】/ PD-028/PD-035【BLOCKED 不占通过数】为登记依赖非 FAIL**；**PD-032 为已决策口径标注【与 REG-UI-TRACE-CL01 同源，决策池原文逐字一致】**）；QA 观察项 **O-WS-B5-1~3** + R1 归因限制如实登记不销号、不猜测处置（O-WS-B5-1：initDefaultDateRange 依赖 accounting_periods.periodName 格式【DB 自由文本，非 YYYY-MM 时走失败透传兜底，buildProfitParams 正则防御，标准数据风险低】/ O-WS-B5-2：summarizeByPeriod 后端 DTO 注释类型 1~7 vs CostTypeMap 1~6【兜底标签不崩溃不伪造】/ O-WS-B5-3：permission.ts 159 行历史未提交产物【grep 标题零命中与本批无关，回归阶段确认提交归属】/ R1 归因限制：工作树未提交无法 commit 粒度隔离，以 mtime 窗口 + before/after 逐字对照 + 代码形态三重证据归因，置信度高，详见文末 Batch WS-5 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：2/2 卡 QA PASS（WS-006 经 -R1 复验闭环）、FAIL=0、PWL=0（登记依赖非 FAIL），仅表示基线积累 91 → 93 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch 6（§11.3 批次节奏，禁止跳步）。

**Regression Result（2026-09-04 Batch WS-6 治理批次，非门禁判定）：PASS**——Batch WS-6 四卡（P1-FIN-WS-007 会计科目 / P1-FIN-WS-008 会计期间 / P1-FIN-WS-009 预算 / P1-FIN-WS-010 审批流配置·阶段三 Batch 6 归位页）QA 独立验收 **4/4 PASS**（`docs/quality/ui-ws-b6-qa-report.md`，2026-09-04：四卡全 PASS、无 FAIL、无 -R、PWL 0 项；**WS-007 会计科目**——C 类 3 处假筛选根除（searchForm 拆 subjectCode/subjectName 双输入 :56-61 + subjectType/status 经 SubjectStatusMap/SubjectTypeMap.toBackend 转数字 + handleSearch 空实现→loadData :207-210）/ 消费链 6 环零死参数（FinanceSubject.vue:187-194 → subject.ts:77-90 → SubjectController.java:87 → AccountingSubjectQueryDTO.java:13/16/22/28 → AccountingSubjectServiceImpl.java:121-143 like/eq → selectPage SQL）/ 树形剪枝（pruneTreeByMatchedIds :126-135 匹配集合剪枝保留祖先链 + el-table default-expand-all :411 保留，本地 filteredTree/filterTree 零残留）/ subjectId→id 断链修复（convertTreeNode 递归 subject.ts:63-69 + converters.ts:486-490，getTree/getById/getList 全链转换——编辑/新增子科目/启停恢复可用）/ 余额不伪造（AccountingSubjectVO.java:10-54 无 balance 字段，实体 :64 有但 convertToVO 不输出，前端 grep balance 零命中——登记：待后端 VO 补 balance，PD-031 联动）/ 导出/导入登记（:390-399 disabled+tooltip：P1-FIN-EXPORT-004 + PD-035 / 无 import 端点）/ 视觉同源（仅 var(--fts-border-primary) :521，V3-A 零引用）；**WS-008 会计期间**——KL-058 根除（operatorId computed 会话取值零兜底 :41-44 + 三动作函数头部防御 :271/:299/:321 + 按钮 disabled+tooltip :428-451，grep `?? '1'`/`userId ?? '1'` 零命中）/ 后端 operatorId 校验缺口登记（Controller:57/:64/:85 收 @RequestParam，ServiceImpl:122/:148 直接写入无 SecurityContext 一致性校验→后端池，伪造面收窄未根除如实登记）/ 年份筛选死参数登记不补 UI（Controller:46-51 getPage 仅四参无 year + 前端 loadData :117-140 零 year 发送→后端池）/ 结账动作语义零变更（accounting-period.ts mtime 08-11 零改动，三动作仅 operatorId 取值+防御变更 :281/:309/:331，试算平衡/结账检查 :232-265 零变更）/ 导出登记（:392-396）；**WS-009 预算**——预算类型筛选补 UI（searchForm.budgetType :16-19 + 下拉 1~5 收入/成本/费用/利润/现金流 :34-40，消费链 7 环零死参数：:129-131 → budget.ts:28-44 rest 透传 → BudgetController.java:53 → BudgetQueryDTO.java:22 → BudgetServiceImpl.java:126-127 eq → SQL；getBudgetTypeLabel :82-101 数字→语义标签）/ responsibleDeptId 死参数登记不补 UI（DTO:28 有字段但 Service:118-131 无 eq 消费 + 前端 grep 零命中→后端池，Batch 2 F-1 同型纪律）/ categoryId 无数据源登记（Service eq 就绪 :129-130 但前端无分类目录接口）/ 审批入口 PD-034 登记（:272-276 disabled+tooltip「审批动作归属待产品决策，决策后启用；登记不猜测」，BLOCKED 不占通过数）/ 双轨语义漂移登记（BudgetFormDialog 创建链路字符串 vs 后端 1~5 枚举，既有问题本批不触碰）/ 导出登记（:277-281）；**WS-010 审批流配置**——B-1/B-2 映射修正（buildQueryParams :62-77 → approval-flow.ts:83-92 mapQueryParams 补 enabled→isEnabled :89 + configName→keyword :90，消费链 7 环零死参数：→ ApprovalFlowConfigQueryDTO.java:19/:22 → ApprovalFlowConfigServiceImpl.java:113-118 eq/like → SQL）/ 字段级契约补齐（ApprovalFlowConfigDataConverter :40-75：configId→id / approvalNodes JSON→nodes 数组 / isEnabled→enabled，getList/getById/getByDocumentType/create/update/toggleEnabled 全链路转换——nodes/enabled 列断链修复）/ CRUD 4 端点真实接入（handleCreate :204-214 / handleSubmit create|update :257-281 / handleEdit getById 回填 :217-234 / handleToggleEnabled :284-301 / handleDelete :304-319；POST/PUT/GET/{id}/DELETE/toggle-enabled 全为既有端点，**端点集合零新增**，getById=既有 GET /{id} 封装）/ toggleEnabled 契约修正（Controller:73 @RequestParam Boolean enabled 必填，前端补 (id, enabled) :181-188，页面传 row.id, !row.enabled :292——原缺参必 400 根因消除；启用冲突唯一性校验 ServiceImpl:155-162 错误透传）/ 审批人 ID 文本输入登记（:418-423 逗号分隔 + syncApproverIds :252-254，不伪造用户选择器）/ 审批执行链路后端未实现登记 / 导出登记（:344-348）；强制核验 6/6 全过（git diff 纯契约映射/展示/UI 接入、后端零改动 13 文件 mtime 实锤全 ≤ 08-31、视觉同源 V3-A 零引用 + filteredTree 零残留、死参数零发送纪律三处登记 + P1-STOCK-001 零触碰、typecheck 136=136 本批 8 文件零命中 + build EXIT=0、开发者不自行宣布通过）；新增基线 **REG-FIN-WS-007/008/009/010**（正式 93 → 97 条，只增不减、既有条目零修改）；**阶段三收口核对**：任务型重构 6 工作台 10 卡全部 QA 通过——REG-FIN-WS-001~010 全部在案（Batch 1：001 / Batch 2：002 / Batch 3：003 / Batch 4：004 / Batch 5：005+006 / Batch 6：007~010；阶段三累计 87 → 97 = 10 条，无缺卡、无预固化条目），**等待产品负责人走查**（§9.3 硬约束 4，不自动开新批）；PWL 0 项、FAIL=0 → **允许放行**（**PD-031（科目余额管理台账口径）/PD-034（预算审批归属）/PD-035（导出规则）为 BLOCKED 登记不占通过数**，如实登记不销号；**科目余额 VO 缺 balance / KL-058 后端侧校验缺口 / 期间年份筛选 / responsibleDeptId 死参数 / categoryId 无数据源 / 审批执行链路未实现 / 科目导入无端点 / 审批人无用户目录数据源 = 登记依赖非 FAIL**——仅登记不伪造、不混入，roadmap §5.12 硬约束）；QA 观察项 **O-B6-1~4** 如实登记不销号、不猜测处置（O-B6-1 SubjectTypeMap 6 值 vs 后端注释 5 值（既有契约非本批引入，eq 数字比较不影响 SQL，数据侧 subject_type=6 未核登记）/ O-B6-2 Approval CRUD 按钮无 v-permission（后端 @PreAuthorize hasAuthority('finance:approval-flow-config:manage') 强校验，与全站既有模式一致）/ O-B6-3 任务卡行号引用偏差（approval-flow.ts mapQueryParams :29-40 vs 实际 :83-92 等，内容与行为完全一致）/ O-B6-4 getList size=1000 口径（树形视图分页不适用，匹配集合仅用于剪枝祖先链），详见文末 Batch WS-6 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：4/4 卡 QA PASS、FAIL=0、PWL=0（PD-031/034/035 为 BLOCKED 不占通过数；登记依赖非 FAIL），仅表示基线积累 93 → 97 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**阶段三 6 批 10 卡全部完成后停止，等待产品负责人走查（§9.3 硬约束 4，不自动开新批）**。

**Regression Result（2026-09-04 Batch RVT-1 治理批次，非门禁判定）：PASS**——Batch RVT-1 两卡（P1-FIN-RVT-001 路由/菜单全局回退 + P1-FIN-RVT-004 标题回退批）QA 独立验收 **2/2 PASS_WITH_LIMITATION**（`docs/quality/ui-rvt-b1-qa-report.md`，2026-09-04：两卡全 PWL、无 FAIL、无 -R；RVT-001 回退范围全部达成且与 HEAD 基线逐行同构【13 页独立组件直达路由恢复（router/index.ts:116-128，name/component/meta.title/权限码与 HEAD 逐行一致）/ /finance/reconciliation、/finance/records 路由 + 5 条重定向移除（grep 零命中 + dist 无 FinanceReconciliation/FinanceRecords chunk）/ /finance 根 redirect 恢复 /finance/ledger（:115 = HEAD 原值）/ 6 个工作台菜单项彻底移除（menu.ts 工作台标题零命中、菜单/路由/dist 三重零残留）/ 13 项原菜单标题图标 scaleLevel 顺序恢复（standard+ 9 / chain-standard+ 3 / chain-enterprise 1 项 = HEAD 原值非猜测）/ 5 独立页壳新建承功能（壳 21-22 行 = PageHeader + 渲染对应 Tab 子组件，功能零回退，dist 5 独立 chunk 产出）】；RVT-004 四页标题三处同步【FinanceFund.vue:429 资金管理 / FinanceTax.vue:677 税务管理 / FinanceCost.vue:270 成本管理 / FinanceReport.vue:453 财务报表，menu/router/PageHeader 三处同步，路径/组件/scaleLevel/权限码零变更】+ permissions/role-profiles 文案零漂移（permissions.ts:722-747 + role-profiles.ts:265/:268 均原命名，M 态归因非本批）+ 页面内增强 15/15 零回退（KL-059/B-5/summarizeByPeriod/9 类报表/P0 断流/WS-006-R1 金额口径修复/新建流水/E 值/失败透传/打印/口径标注）；finance 域「对账工作台/合规工作台/毛利核算/决策工作台」grep 零命中；typecheck 136=136 本批 11 文件零命中 + build EXIT=0 独立复跑）→ **新增正式基线 REG-FIN-RVT-001/004（正式 97 → 99 条，只增不减、既有条目零修改）**；**WS 基线只标注不删除（§12.4 方案）**：REG-FIN-WS-001/004/005/006 标题断言、WS-002/003 合并断言、WS-003 A01 穿透断言已在原条目录内如实标注「由 RVT 基线接管/回退后现状」（WS-007~010 无结构断言不标注）；**PWL 2 项逐条确认不阻断发布**——① 穿透瞬态：QA 补充发现 FinanceReport drillTo 13 处（:128-133 应收 6 处 / :137-142 应付 6 处 / :150 账龄 1 处，目标 /finance/reconciliation?tab=receivable|payable 已移除）+ developer 已登记 FinanceFund:318 A01 穿透（name FinanceRecords 已移除）→ 两处均归 **RVT-002** 承接修复（drillTo 改指独立页 / A01 改回 FinanceLedger；登记性质不构成 FAIL；RVT-002/003 完成前两处穿透按钮降级为不可用=功能缺失非数据错误，由 regression/architect 在 Release Gate 评估——本批为治理批次不进上线门禁判定）；② subject/budget 标题差异（「会计科目」「预算管理」= HEAD 原命名 vs §5.13 清单简称「科目」「预算」）待产品走查，不实施不猜测；注释残留（types/finance.ts:1274「决策工作台」/ FinanceRecords.vue:10 已改写 / FinanceTax.vue:974 等）登记备查；**Regression Result: PASS**（治理批次口径：2/2 卡 QA PWL、FAIL=0（限制为登记性质不构成 FAIL），仅表示基线积累 97 → 99 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch RVT-2（P1-FIN-RVT-002 核销页回退，§12.1 批次节奏，禁止跳步）。

**Regression Result（2026-09-05 Batch RVT-2 治理批次，非门禁判定）：PASS**——Batch RVT-2 一卡（P1-FIN-RVT-002 核销页回退 + 2 项穿透修复）QA 独立验收 **1/1 PASS**（`docs/quality/ui-rvt-b2-qa-report.md`，2026-09-05：无 FAIL、无 -R；壳/页签删除零残留【FinanceReconciliation.vue 删除 glob 零命中 + dist 无 chunk / ReceivableTab·PayableTab 删除（glob `*Tab.vue` 仅剩 LedgerTab·ReimbursementTab·TransferTemplateTab，RVT-003 范围）/ el-tabs/tab query 同步逻辑移除 / emit 链路移除（grep `defineEmits|@summary-updated|\bemit(` 本批文件零命中）/ 路由直达独立组件（router/index.ts:117-118）/ 菜单独立项恢复（menu.ts:23/:25）】+ 内容回迁独立页完整【FinanceReceivable.vue 575 行 / FinancePayable.vue 595 行（developer 证据记 510/527，行数统计口径差异已登记），全文读码与页签承载功能清单一一对应，页签结构零残留】+ 保留项 15/15 逐项命中零回退（writeOff 入口+二次确认 :272-291 / 应收到期日端到端 :191-194 / 应付到期日登记处置零死参数 :205-206 注释明示 + disabled :425-435 + tooltip :421-424 + handleFilterRestored :247 恒 null / 逾期高亮 / 核销进度列 / 打印 / 失败透传 / 引导空态 / 导出批量 disabled+tooltip 登记 / CaliberNoteBar / expand 行内详情 / 合计 / 分页 / 密度 / 固定列 / 筛选保存 storage-key 保持）+ 待办区页面级增强（pendingSummary 真实字段派生不虚构 :138-148/:143-153 + PD-033 冲正标注归应收页 :368-371）+ **2 项穿透修复闭环**【① FinanceFund.vue:317 A01 改回 `name: 'FinanceLedger'`（B1 §3.2 :318 FinanceRecords 已改回 + 注释 :309-314 同步）；② FinanceReport.vue drillTo 13 处（应收 6 :128-133 / 应付 6 :137-142 / 账龄 1 :150）改指 /finance/receivable、/finance/payable 独立页 + 头注释 :23-24 同步；`/finance/reconciliation?tab=` 全 src grep 零命中】+ 强制核验 6/6 全过（git diff 归因 / 后端零改动 / 视觉同源 V3-A 零引用 / 死参数零发送 / P1-STOCK-001 零触碰 / 构建独立复跑 typecheck 136=136 本批 4 文件零命中 + build EXIT=0 + dist 4 chunk 存在无 FinanceReconciliation chunk）→ **新增正式基线 REG-FIN-RVT-002（正式 99 → 100 条，只增不减、既有条目零修改）**；**WS 基线接管标注保持不删除**（REG-FIN-WS-002 合并断言 + REG-FIN-WS-003 A01 穿透断言已由本基线接管覆盖，原条目录内标注不变）；**登记/观察项 4 条如实登记不销号、不猜测处置**（行数证据微差 developer 510/527 vs QA 实测 575/595（含 style 块，内容完整性已实读核验非功能缺陷）/ FinanceRecords.vue:132 CSS 注释（RVT-003 范围，本批不触碰）/ FinanceTax.vue:974·TransferTemplateTab.vue:6 既往登记保持（B1 §3.4，非本批文件）/ 待办区当前页口径（分页范围，是否全量待产品走查））；**B1 PWL① 穿透瞬态限制消除**（A01/drillTo 恢复可用，B1 报告 §五.1「RVT-002/003 完成前发布则穿透降级」风险在本批解除）；**Regression Result: PASS**（治理批次口径：1/1 卡 QA PASS、FAIL=0、PWL=0（登记/观察项不构成 FAIL），仅表示基线积累 99 → 100 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch RVT-3（P1-FIN-RVT-003 记录页回退，含 FinanceRecords 壳删除 + LedgerTab/ReimbursementTab/TransferTemplateTab 三页签回迁独立页）。

**Regression Result（2026-09-05 Batch RVT-3 治理批次，非门禁判定）：PASS**——Batch RVT-3 一卡（P1-FIN-RVT-003 记录页回退）QA 独立验收 **1/1 PASS**（`docs/quality/ui-rvt-b3-qa-report.md`，2026-09-05：无 FAIL、无 -R；壳/页签删除零残留【FinanceRecords.vue 删除 glob 零命中 + dist 无 chunk / LedgerTab·ReimbursementTab·TransferTemplateTab 页签删除（glob `*Tab.vue` 零命中）/ **:132 CSS 注释残留随壳删除清理（B2 §3.2 登记项闭环）** / el-tabs/tab query 同步逻辑移除（3 独立页全文读码零命中，仅头注释说明移除）/ **emit 链路移除**（grep `defineEmits|@summary-updated|\bemit(` 本批 3 文件零命中、`@summary-updated` 全 views/finance 零命中）/ 路由直达独立组件（router/index.ts:116/:123/:126 FinanceLedger/InvoiceReimbursement/AutoVoucher 直达，随 RVT-001 恢复本卡确认零改动）/ 菜单独立项恢复（menu.ts:17-18/:31-32/:43-44「财务总账」「发票报销」「自动凭证管理」，AutoVoucher=chain-enterprise 原档位）】+ 内容回迁独立页完整【FinanceLedger.vue 785 行 / InvoiceReimbursement.vue 660 行 / AutoVoucher.vue 519 行（developer 证据记 751/650/501，行数统计口径差异已登记），全文读码与 WS-003 页签承载清单 + B1 §2.5 逐项对应，页签结构零残留】+ 保留项 12/12 逐项命中零回退（batchPost 仅 audited 勾选 + BatchResultFeedback 提交数口径 + tooltip 明示 N/M 登记 / 红冲作废 + 二次确认 + 审批流保留（KL-060 登记不猜测）/ 联动 CRUD + B-3/B-4 映射端到端有效 / voucherStatus 键名 + voucherType 数字分支【WS-003-R1 修复逐字符一致】/ referenceNo 零发送 / 凭证筛选 4 维端到端有效 / expand 分录 + 来源展示 + 穿透接收端 voucherId 定位 / 失败透传 + 引导空态 / KL-060 登记卡 + KL-061 伪语义列移除 / 既有登记处置 disabled+tooltip 保持 / E 值格式修复 / CaliberNoteBar + useSummary unit='fen' 账本口径）+ 待办区页面级增强（pendingSummary 真实字段派生不虚构 :188-194/:108-112 + 登记卡 :420-423/:354-361，零虚构计数核验）+ **scaleLevel 折叠回退**（canSeeTransferTemplate 全 src grep 零命中、AutoVoucher 无条件渲染、可见性由菜单档位 menu.ts:43-44 chain-enterprise 控制）+ 强制核验 6/6 全过（git diff 归因 / 后端零改动 + API 层零改动【voucher.ts voucherStatus 键名 :41 + voucherType 数字分支 :43-45 + batchPost :185-190、transfer-template.ts B-3/B-4 :34-39 + toggleEnabled :116-122、invoice.ts void :137-141 / redFlush :149-153 = WS-003 已验收修复完整保持】/ 视觉同源 V3-A 零引用 / 死参数零发送 / P1-STOCK-001 零触碰 / 构建独立复跑 typecheck 136=136 本批 3 文件零命中 + build EXIT=0 + dist 3 chunk 存在、FinanceRecords/LedgerTab/ReimbursementTab/TransferTemplateTab chunk 零命中 + dist/assets 全部 js 字符串扫描 ZERO）→ **新增正式基线 REG-FIN-RVT-003（正式 100 → 101 条，只增不减、既有条目零修改）**；**WS 基线接管标注保持不删除**（REG-FIN-WS-003 合并断言已由本基线接管覆盖，原条目录内标注不变）；**回退专项收口声明**：结构回退 3 批 4 卡（RVT-001/002/003/004）全部 QA 通过、**REG-FIN-RVT-001~004 全部在案**（专项累计 97 → 101 = 4 条回退基线，只增不减、既有 100 条零修改；WS 基线 10 条标注接管不删除）、功能修复保留确认，**等待产品负责人逐页走查**（roadmap §5.13 硬约束，完成后停止，不自动开新批）；**登记/观察项 5 条如实登记不销号、不猜测处置**（行数证据微差 developer 751/650/501 vs QA 实测 785/660/519（含 style 块，内容完整性已实读核验非功能缺陷）/ 注释残留 7 处备查【3 处回迁登记注释 FinanceLedger:6/InvoiceReimbursement:6/AutoVoucher:6,12 + 4 处既往备查 FinanceTax:974/FinanceFund:311,313，零功能引用】/ FinanceTax el-tabs 自身页结构范围外 / api/finance/record.ts 孤立 API 文件零消费方 / 待办区当前页口径（分页范围，是否全量待产品走查））；**B2 §3.2 FinanceRecords.vue:132 注释登记项随壳删除闭环**；**Regression Result: PASS**（治理批次口径：1/1 卡 QA PASS、FAIL=0、PWL=0（登记/观察项不构成 FAIL），仅表示基线积累 100 → 101 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**回退专项全部完成，等待产品负责人逐页走查（§5.13 硬约束，不自动开新批）**。

**Regression Result（2026-09-05 Batch LRVT-1 治理批次，非门禁判定）：PASS**——Batch LRVT-1 两卡（P1-FIN-LRVT-001 资金管理 / P1-FIN-LRVT-002 财务总账，布局级回退·穿透成对样板批）QA 独立验收 **2/2 PASS**（`docs/quality/ui-lrvt-b1-qa-report.md`，2026-09-05：两卡全 PASS、无 FAIL、无 -R；**布局与 HEAD 同构**（Fund：PageHeader#extra→stats-section→advanced-search-panel（手写 div 恢复，SearchPanel 组件化容器移除）→table-section→pagination-wrapper（独立分页恢复）→详情 el-dialog（expand 行内详情回迁）→**末尾追加 B3 新建流水弹层**（B 承载非布局重排）；Ledger：PageHeader#extra→stats-section→advanced-search-panel→table-section→pagination-wrapper→VoucherFormDialog→详情 el-dialog）——待办区/工具栏/SearchPanel/#expand/expandable 仅注释提及、模板与样式零残留 + 工作台样式（.workbench-taskboard/.workbench-toolbar 等）删除；**重放修复点全有效**（Fund B1~B6：E 值格式（toDateString/restoreDateRange/value-format YYYY-MM-DD）/失败透传（catch→loadFailed+EmptyState 重试）/新建流水（PageHeader#extra 入口 + fundFlowApi.create 既有端点 + 金额元→分 Converter 统一）/A01 来源展示+正向穿透（getSourceDocNo + handleJumpToVoucher→FinanceLedger?voucherId）/筛选持久化（fts_search_finance-fund-filter）/口径标注（CaliberNoteBar 挂 PD-031/032）；Ledger B1~B7：筛选 4 维端到端（voucherNo/voucherType 数字分支/status【voucherStatus 键名修正】/dateRange，mapQueryParams 消费链闭合）/批量过账（voucherApi.batchPost 既有端点 + 仅 audited 勾选 + BatchResultFeedback 提交数口径）/打印（window.print）/穿透键透传+接收端（referenceNo/sourceType/sourceId + onMounted voucherId→voucherApi.getById）/失败透传/筛选持久化（fts_search_finance-ledger-filter）/KL-061 登记）+ **处置③分录 el-table 回迁**（detailEntryColumns = HEAD 原 5 列 + 借贷合计 + displayRecords 转换保留）+ **能力层保留（不回退）**（Fund：density="auto"/show-summary unit='yuan'/固定列；Ledger：density="auto"/selectable/show-summary unit='fen' 账本口径/固定列——金额口径与 HEAD 零变更、行/合计一致）+ **穿透成对不断链**（Fund:390-393/:583 发送端 ↔ Ledger:547-552 接收端，同批成对）+ **登记类入口不失效**（Fund 导出【EXPORT-001】/日结【PD-028】、Ledger 批量审核/导出/凭证导入 disabled+tooltip 挂 PageHeader#extra 承载）+ **强制核验 6/6 全过**（布局同构 git diff 归因（Fund +484/-14、Ledger +394/-30）/ 后端零改动+API 层零新增（LRVT 标记仅本批两文件，全仓 grep 6 命中）/ 视觉同源 V3-A 零引用 / 穿透成对 / 构建独立复跑 typecheck 136=136 两文件零错误 + build EXIT=0 / 批隔离 P1-STOCK-001 零触碰））→ **新增正式基线 REG-FIN-LRVT-001/002（正式 101 → 103 条，只增不减、既有条目零修改）**；**布局回退专项核对**：Batch LRVT-1 两卡全部在案（001/002，穿透成对样板批闭环，无缺卡、无预固化条目；后续 Batch LRVT-2~4 逐批执行，不自动开新批）；**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**登记依赖非 FAIL**（导出/日结/批量审核/凭证导入 disabled+tooltip 明示（EXPORT-001/P1-FIN-EXPORT-004/无批量端点/PD-028 依赖）；批量过账 N/M = 提交数口径——后端 batchPost 仅返回布尔，精确明细已登记后端池，不伪造）；QA 观察项 5 条如实登记不销号、不猜测处置（存储键按 SearchPanel 前缀约定【fts_search_ 与卡文 storage-key 语义等价，代码注释已注明】/ 无浏览器活体【代码路径级+构建级，运行时点击走查留本基线与产品逐页走查承接】/ 批量过账反馈提交数口径【后端布尔契约限制】/ 金额展示绝对值路径【HEAD 既有展示路径，本批零变更+行/合计一致；绝对值口径属 Batch0-方案2 既有治理范围】/ developer 证据行号少量漂移【内容全部核验存在】），详见文末 Batch LRVT-1 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：2/2 卡 QA PASS、FAIL=0、PWL=0（PD-028/PD-035 为 BLOCKED 不占通过数；登记依赖非 FAIL），仅表示基线积累 101 → 103 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-09-05 Batch LRVT-2 治理批次，非门禁判定）：PASS**——Batch LRVT-2 两卡（P1-FIN-LRVT-003 应收账款 / P1-FIN-LRVT-004 应付账款，布局级回退·应收应付同构批）QA 独立验收 **2/2 PASS**（`docs/quality/ui-lrvt-b2-qa-report.md`，2026-09-05：两卡全 PASS、无 FAIL、无 -R；**布局与 HEAD 同构**（Receivable：PageHeader#extra→stats→advanced-search-panel（手写 div 恢复）→table→pagination-wrapper→新建/详情 el-dialog→ReceiptDialog→ReceiptHistoryDialog；Payable：PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→PaymentDialog→PaymentHistoryDialog）——待办区/工具栏/SearchPanel/#expand 仅注释提及、模板与样式零残留 + `.workbench-*` 样式删除（仅保留 `.row-overdue` 随 B4 重放）；**重放修复点全有效**（Receivable B1~B8：到期日筛选端到端【:161-163/:435-444 daterange YYYY-MM-DD → ReceivableQueryDTO → ReceivableServiceImpl:103-105 → ReceivableMapper.xml:16-21 due_date 真实过滤】/坏账核销【:307-328 handleWriteOff ElMessageBox 二次确认 + receivableApi.writeOff :320 + 行内 actions :498，writeOff 既有端点 ReceivableController:72-76 + PreAuthorize 既有】/核销进度列【:127-134 getProgressPercent + :144/:489-491，**receivedAmount 后端实锤** ReceivableServiceImpl:130 累加 + :301 VO set + 分→元】/逾期高亮【overdue 键 :88-96 + rowClassName :101-103/:483 + .row-overdue :576-580，后端动态计算 4=overdue】/打印 :336-338/:409 /失败透传 :64/:169-177/:456-472 /筛选持久化 fts_search_finance-receivable-filter :225/:227-257/:274-278 /导出批量登记 :410-415 disabled+tooltip；Payable B1~B8：创建日期筛选端到端【:172-174/:442-451 → PayableQueryDTO → PayableServiceImpl:111-113 → PayableMapper.xml:16-21 create_time】/**到期日登记处置保持【F-1 防回归三重防线：:61-62 恒 null + :176-177 零死参数 + :236 旧存档不回填 + :454-469 disabled+tooltip——不因回退变回假筛选，后端 PayableServiceImpl 未传 mapper / PayableMapper.xml 无 due_date 过滤（QA F-1 属实），端到端启用待后端池】**/核销进度列【:133-140/:152/:514-516，**paidAmount 后端实锤** PayableServiceImpl:138】/逾期高亮 :94-110/:508/:605-609 /打印 :319-321/:415 /失败透传 :69/:182-188/:480-497 /持久化 fts_search_finance-payable-filter :242/:244-277/:399-404 /导出批量登记 :416-421）+ **处置③ 详情回迁**（Receivable 11 字段 :537-556 / Payable 13 字段 :563-584，与 HEAD 顺序逐一一致；#expand/expandable 全文件零命中）+ **能力层保留（不回退）**（density="auto" / show-summary + createAmountSummaryMethod unit='yuan' / 固定列 / 逾期高亮；Receivable actions-width 220→280 属性级承载第 4 个行内按钮【非区块变更】、Payable actions-width 280 与 HEAD 一致）+ **口径标注**（CaliberNoteBar Receivable :454 / Payable :479，PD-031/032 纯展示）+ **强制核验 6/6 全过**（布局同构 git diff 归因（Receivable +381、Payable +380，合计 653 insertions / 108 deletions）/ 后端零改动+API 层零新增（LRVT-003/004 标记仅本批两文件，全仓 grep 2 命中）/ 视觉同源 V3-A 零引用 / 应付到期日登记处置保持 + P1-STOCK-001 零触碰 / 构建独立复跑 typecheck 136=136 两文件零错误 + build EXIT=0 / 开发者不自行宣布通过——QA 报告为唯一验收结论））→ **新增正式基线 REG-FIN-LRVT-003/004（正式 103 → 105 条，只增不减、既有条目零修改）**；**布局回退专项核对**：Batch LRVT-2 两卡完整性核对 = **REG-FIN-LRVT-003/004 全部在案**（应收应付同构批闭环，无缺卡、无预固化条目）；专项累计 **101 → 105 = 4 条**（LRVT-1：001/002 + LRVT-2：003/004），**等待产品负责人逐页走查**（roadmap §5.14，不自动开新批）；**PWL 0 项、FAIL=0 → 允许放行**（**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**应付到期日端到端启用 / 导出 / 批量 = 登记依赖非 FAIL**——后端池排期（QA F-1 / P1-FIN-WS-002-R1 登记链）+ EXPORT-001 / P1-FIN-EXPORT-004 / 无批量端点，disabled+tooltip 明示或零死参数，不伪造）；QA 观察项 6 条如实登记不销号、不猜测处置（逾期状态动态计算不持久化=HEAD 既有 / Mapper 参数名错位=后端既有 / hasActiveFilter 死检查无害 / 无浏览器活体【留走查】/ developer 证据行号漂移 / 既往条目断言过时衔接【REG-UI-FIN-003/004 部分断言由本批覆盖】，详见文末 Batch LRVT-2 段条目「限制说明/观察项」）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-09-05 Batch LRVT-3 治理批次，非门禁判定）：PASS**——Batch LRVT-3 两卡（P1-FIN-LRVT-005 税务管理 / P1-FIN-LRVT-007 成本管理，布局级回退·税务成本批）QA 独立验收 **2/2 PASS**（`docs/quality/ui-lrvt-b3-qa-report.md`，2026-09-05：两卡全 PASS、无 FAIL、无 -R；**布局与 HEAD 同构**（Tax：PageHeader#extra→stats 顶层→el-tabs 3 页签（Tab1/Tab2 筛选+表格、Tab3 筛选+表格+独立 pagination-wrapper 恢复）→4 详情对话框→配置对话框；Cost：PageHeader#extra→stats→advanced-search-panel（monthrange 重做）→table-section→pagination-wrapper→详情 el-dialog 8 字段→CostFormDialog→B2 分析对话框末尾）——待办区/工具栏/SearchPanel/:pagination 仅注释提及、模板与样式零残留 + `.workbench-*` 样式删除；**重放修复点全有效**（Tax B1~B7：getRecords 端到端三 eq 实锤【TaxRecordServiceImpl:44-46/:49-51/:54-56】/payTax+PD-001 幂等零触碰【TaxRecordServiceImpl:79-90】/KL-059 税期动态化 YYYYMM 契约零变更【:57-86 + Service:49-51 消费实锤】/提交电子税务局失败透传/申报表加载失败提示/useSummary unit='yuan'/teleported+默认申报期动态化；Cost B1~B6：**monthrange 端到端全链闭合**【YYYY-MM 控件 → cost.ts mapQueryParams:41-42 startPeriod/endPeriod（api 层既有修复零改动 mtime 9/4）→ CostServiceImpl:63-65 ge / :66-68 le(period) 实锤，真实筛选非本地过滤】/成本结构分析【summarizeByPeriod 既有端点 CostController:60-64 + CostServiceImpl:91-101 聚合 + fenToYuanNumber 分→元一次转换 money.ts:44-47，无二次 /100，WS-006-R1 同类防回归】/失败透传+引导空态双态/CaliberNoteBar PD-032/合计行 unit='yuan'/导出导入登记）+ **处置③ 详情回迁**（Cost 详情 8 字段 :393-407 与 HEAD 顺序逐一一致；expand 全文件零命中）+ **能力层保留（不回退）**（density="auto"/show-summary unit='yuan'/actions-width 与 HEAD 一致）+ **后端零改动+api 层零改动**（mtime 时间戳批次归属实锤：TaxRecordServiceImpl/TaxRecordController 8/31、CostServiceImpl/CostController 8/11、tax-calculation.ts 8/31、cost.ts 9/4 均早于本批 9/5；9/5 10:55-10:56 时间窗口内 frontend/src 仅 FinanceTax.vue + FinanceCost.vue 两文件被修改）+ **强制核验 6/6 全过**（布局同构 git diff 归因（Tax +267/-61、Cost +280/-24，合计 547 insertions/85 deletions 与 developer 证据一致）/ 后端零改动+api 层零改动 / 视觉同源 V3-A 零引用 / 筛选端到端零死参数（taxPeriod 链 + monthrange 链两链消费实锤）+ P1-STOCK-001 零触碰（批隔离）/ 构建独立复跑 typecheck 136=136 两文件零错误 + build EXIT=0 / 开发者不自行宣布通过——本报告为唯一验收结论））→ **新增正式基线 REG-FIN-LRVT-005/007（正式 105 → 107 条，只增不减、既有条目零修改）**；**布局回退专项核对**：Batch LRVT-3 两卡完整性核对 = **REG-FIN-LRVT-005/007 全部在案**（税务成本批闭环，无缺卡、无预固化条目）；专项累计 **101 → 107 = 6 条**（LRVT-1：001/002 + LRVT-2：003/004 + LRVT-3：005/007）；专项编排 Batch LRVT-1~4 逐批执行（005/007 = LRVT-3；006/008/009 = LRVT-4）；**等待产品负责人逐页走查**（roadmap §5.14，完成后停止，不自动开新批）；**PWL 0 项、FAIL=0 → 允许放行**（**PD-028/PD-035 为 BLOCKED 登记不占通过数**，如实登记不销号；**导出/批量缴税/导入为登记依赖非 FAIL**——EXPORT-001 / P1-FIN-EXPORT-004 / 无批量端点 / 无 import 端点，disabled+tooltip 明示，不伪造）；QA 观察项 4 条如实登记不销号、不猜测处置（payTax 未传 voucherNo=WS 批既有状态【幂等键不完整走正常新增】/ Tab1 无分页 UI 与 HEAD 一致【pageSize=100 参数载体非死参数】/ 分析聚合金额=后端既有分口径【一次转换无二次 /100】/ 无浏览器活体【留走查】，详见文末 Batch LRVT-3 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：2/2 卡 QA PASS、FAIL=0、PWL=0（登记项不构成 FAIL），仅表示基线积累 105 → 107 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变。

**Regression Result（2026-09-05 Batch LRVT-4 治理批次，非门禁判定）：PASS**——Batch LRVT-4 三卡（P1-FIN-LRVT-006 财务报表 / P1-FIN-LRVT-008 自动凭证 / P1-FIN-LRVT-009 发票报销，布局级回退·专项最后一批）QA 独立验收 **3/3 PASS**（`docs/quality/ui-lrvt-b4-qa-report.md`，2026-09-05：三卡全 PASS、无 FAIL、无 -R；**布局与 HEAD 同构**（Report：PageHeader#extra【打印+导出登记】→stats 固定 4 卡网格 repeat(4,1fr)【与 HEAD 逐字一致】→advanced-search-panel【9 类 select+monthrange】→table-section【CaliberNoteBar+EmptyState×3+DataTable 异构列】→pagination-wrapper 独立分页恢复【处置⑥】；AutoVoucher：PageHeader#extra【新建规则+导出登记+打印】→stats→advanced-search-panel【原手写筛选区与 HEAD 158-165 逐字同构】→table-section→pagination-wrapper→详情 el-dialog→B1 新建/编辑规则对话框；InvoiceReimbursement：PageHeader#extra【新建报销回 HEAD 原位置】→stats→advanced-search-panel【手写筛选区+value-format YYYY-MM-DD】→table-section→pagination-wrapper→详情/审批【KL-060 说明承载】/新建报销 el-dialog）——待办区/工具栏/SearchPanel/#expand 模板样式零残留（仅注释）+ `.workbench-*` 样式删除；**重放修复点全有效**（Report B1~B7：9 类端点聚合【后端 ReportController 恰 7 端点 :30-83 无 balance-sheet/cash-flow，report.ts:64/:80 注释明示 404——7 有效+2 P0 断流实锤】/B2 KL-057 P0 断流空态【:58-59/:379-382/:486-493 EmptyState「后端端点缺失」+P1-FIN-BE-001 登记，不显示空数据不伪造】/B3 后端占位说明【账龄恒 0 :156-176/预算待实现 :220-230 后端实锤 + 占位检测 :349-356/:359-365/:494-501】/B4 drillTo 19 处→4 独立路由【:430-432 router.push + router/index.ts:117-120 实锤，不可行「—」登记】/B5 打印 window.print/B6 失败透传+重试/**B7 WS-006-R1 金额口径重放【:260-275 cost_structure 四卡 typeAmountFen/totalFen 保持分 + formatFenToYuan(分) 一次转换 :271-274，无二次 /100，与 ui-ws-b5-r1 实证 ¥1,000/¥600/¥200/¥200 同型，F-1 防回归**】/导出登记【:459-461】；AutoVoucher B1~B6：CRUD 既有端点【transfer-template.ts:85-107 + TransferTemplateController:30-33/:37-40/:44-47】+getLeafSubjects【subject.ts:142-147】+二次确认【:262-266】+DataTable 自动操作列【DataTable.vue:5-6/:103-105/:254-264】/B-3-B4 映射端到端【:133-140 + api 层 mapQueryParams:28-41 零改动 mtime 9/3 + TransferTemplateQueryDTO:19/:22 + Controller:58-61，无死参数】/KL-061 伪语义列移除【:190-191「最近执行」→「创建时间」VO.createTime:49 实锤，登记落头注释 :26-28】/契约对齐【:145-154 templateId 数字直传 :146 + :246-257 toggleEnabled 补 enabled 参数 :251 + TransferTemplateController:66 @RequestParam 必填实锤】/失败透传+空态【:47/:157-163/:420-427】/打印【:219-221/:399】/筛选持久化【:93 fts_search_finance-transfer-template-filter】/导出登记；InvoiceReimbursement B1~B6：红冲/作废【:219-235/:237-253 二次确认 + invoice.ts:137-152 + InvoiceController:137-142/:152-157 既有端点 + 状态机 :28 + 行内 issued 态入口 :492-495】/E 值格式【:159-176/:203-211/:460 value-format YYYY-MM-DD】/失败透传+引导空态【:58/:146-149/:470-477】/打印【:214-217/:440】/筛选持久化【:178 fts_search_finance-reimbursement-filter，重置即清空】/KL-060 登记不猜测【:272-296 驳回=作废共用 cancelled 语义混叠保持既有登记链 + :292 文案精确化 + :552-554 说明承载】/导出登记）+ **能力层保留（不回退）**（Report：density="auto"/show-summary unit='yuan' 仅 cost_structure/固定列 item left/CaliberNoteBar/独立分页；AutoVoucher：density="auto"/独立分页/统计卡片 4 项 isEnabled 口径/DataTable 自动操作列；Invoice：density="auto"/独立分页/统计卡片 4 项；金额口径全页复核：各分支均分→元一次转换无二次 /100）+ **后端零改动+api 层零改动**（mtime 实锤：report.ts 8/11、invoice.ts 8/11、subject.ts 9/4、transfer-template.ts 9/3 23:54 均早于本批 9/5；git status backend/ 无本批 finance 控制器/服务）+ **强制核验 6/6 全过**（布局同构 git diff 归因（Report +405/-137、AutoVoucher +349/-39、InvoiceReimbursement +194/-16 与 developer 证据一致）/ 后端零改动+api 层零改动 / 视觉同源 V3-A 零引用 / 金额口径正确 + 穿透 19 处指向独立页不失效 + P1-STOCK-001 零触碰（批隔离）/ 构建独立复跑 typecheck 136=136 三文件零错误 + build EXIT=0 / 开发者不自行宣布通过——本报告为唯一验收结论））→ **新增正式基线 REG-FIN-LRVT-006/008/009（正式 107 → 110 条，只增不减、既有条目零修改）**；**布局回退专项收口核对**：Batch LRVT-4 三卡完整性核对 = **REG-FIN-LRVT-006/008/009 全部在案**（财务报表/自动凭证/发票报销批闭环，无缺卡、无预固化条目）；**专项累计 101 → 110 = 9 条回退基线全部在案**（LRVT-1：001/002 + LRVT-2：003/004 + LRVT-3：005/007 + LRVT-4：006/008/009 = REG-FIN-LRVT-001~009 九卡完整；纯 B 4 页 LRVT-010~013 关闭不产生基线；只增不减、既有 107 条零修改）；**布局回退专项收口声明**：9 页布局级回退 + 功能修复重放全部 QA 通过，**等待产品负责人逐页走查**（roadmap §5.14 硬约束，完成后停止，不自动开新批）；**PD-028/PD-035 为 BLOCKED 登记不占通过数**（如实登记不销号）；**导出等为登记依赖非 FAIL**（P1-FIN-EXPORT-001→004 登记链，disabled+tooltip 明示，不伪造）；QA 观察项 6 条如实登记不销号、不猜测处置（income_expense 3 卡网格 1 空位 / balance·cash_flow 零值卡为 B2 修复行为 / budget rate*100 后端恒 0 / id String 化既有模式 / 审批驳回=作废既有登记链 / 无浏览器活体【留走查】，详见文末 Batch LRVT-4 段条目「限制说明/观察项」）；**Regression Result: PASS**（治理批次口径：3/3 卡 QA PASS、FAIL=0、PWL=0（PD-028/035 为 BLOCKED 不占通过数），仅表示基线积累 107 → 110 + 正式基线 FAIL=0 维持，非发布门禁）；本批为治理批次（非发布批次），按治理口径不进上线门禁判定，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**布局回退专项（任务池 §13）全部批次完成，等待产品负责人逐页走查（§5.14 硬约束，不自动开新批）**。

**最终门禁前待办（DEF 修复批次闭环后，均为验证缺口/联调项，不构成 FAIL）**：联调批次 KL-013（真实登录联调，见 REG-SEC-004 L-5）/ KL-029 / KL-030 登记待回归阶段闭环；KL-020、KL-031 活跃缺陷部分已全部闭环（见下）；浏览器级冒烟（KL-013 渲染 / KL-030 下载 / KL-020-V2 下拉交互 / KL-031-R1 通知面板 UI）待发布前浏览器环境补验。

**Regression Result（2026-09-08 Batch FIN-PAY-1 财务付款链路专项 FIN-UI-003，发布批次）：PASS**——FIN-PAY 专项四卡 QA 独立验收 **1 PASS + 3 PASS_WITH_LIMITATION**（2026-09-08）：**P1-FIN-PAY-001 PASS**（逾期应付付款参数类型匹配：前端 payableId/bankAccountId string→number Number() 显式转换 + PaymentDialog 提交验证 + 后端 Long DTO Jackson 反序列化链路完整）；**P1-FIN-PAY-002 PASS_WITH_LIMITATION**（B模式回填银行记录：BankPaymentRecordController 新建 + payment.ts 方法补充 + PaymentHistoryDialog 确认按钮调用链完整，限制=运行时联调缺口）；**P2-FIN-PAY-003 PASS_WITH_LIMITATION**（银行账户列表加载：N+1 查询优化 getReconciliationResultsBatch() + 错误处理透出 HTTP 状态码，限制=性能基准缺失）；**P2-FIN-PAY-004 PASS_WITH_LIMITATION**（付款账户显示：accountLabel() 防御性处理消除乱码，限制=空值边界未全覆盖）；新增基线 **REG-FIN-PAY-001/002/003/004**（正式 110 → 114 条，只增不减、既有条目零修改）；**PWL 3 项逐条确认不阻断发布**：① B模式回填运行时联调缺口（代码级契约完整，fail-safe 方向）② 银行账户列表性能基准缺失（功能正确，仅缺压测数据）③ 付款账户空值边界（防御性处理已兜底，仅缺极端场景）——均为验证缺口非 FAIL；**FAIL=0 → 回归门禁允许放行**；既有基线 REG-FIN-001~006 / REG-FIN-WS-001~010 / REG-UI-FIN-001~004 逐条检查无受影响（本次修改仅涉及 PaymentDialog/PaymentHistoryDialog/BankPaymentRecordController/payment.ts，不触碰既有财务模块核心逻辑）；**Regression Result: PASS**（FAIL=0，回归门禁允许放行）
**Regression Result（2026-09-23 Batch ORDER-A1 订单号唯一性专项 P1-ORDER-NUMBER-002/A1，发布批次）：PASS**——A1 卡 QA 独立验收 **PASS_WITH_LIMITATION**（`docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md`，2026-09-23：H-01~H-06 **6/6 PASS**；CC-1~CC-9 全 PASS，其中 **CC-7 = PASS_WITH_LIMITATION**（回滚窗口 API 探针证据缺失 L-02）；单测 A1 独立复跑 **3/3 PASS**；CC-6 33 套件 **33/33 PASS**（DeductTest 28 + Integration 4 + SelfFailure 1）；git 链 `7f40ab7 → 6f0f2b9(revert) → 0acf99b(reapply)` 独立核对一致；边界 PASS：diff 仅 2 文件 +206 行（业务侧仅 `order.setOrderNumber(orderCode)` L1819 +1 行）、`resources`/Flyway 零改动、`generateOrderCode`/`getMaxTodaySequence` 未改）；**回归角色独立抽验复跑（非采信 QA 自测）**：A1 单测 3/3 PASS、H-01/H-02/H-03 真实创建 `ORD202609231729170001`/`T20260923004`/`ORD202609231729170002` 且 psql `order_code=order_number`、全表 `total=36 with_on=36 null_on=0 distinct_on=36`、H-04 重复键 `orders_order_number_key` EXIT=1、H-05 KDS list 命中 T 码、**H-06 托盘全链零数据补丁复跑 PASS**（POS `T20260923007`/`O1790153488606` food_id=1：bind=bound → kin=making → kout=ready → serve code=0/served；DB `material_consumed=1`、`store_inventory_log` id=57 生菜 8.800→8.700 change=0.100 备注 `KDS出餐扣料 - 订单:T20260923007`、托盘回 idle）；**新增基线 REG-ORDER-001~007（正式 114 → 121 条，只增不减、既有条目零修改）**；**PWL 5 项（L-01~L-05）逐条确认不阻断本地放行**：① L-01 生产不可达（`PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`，本地放行/生产 PROVISIONAL）② L-02 回滚窗口 API 探针缺失（git/证据文件已核，不放大回滚风险）③ L-03 POS 数字 id→food_id 默认 null（既有缺陷 NOT_CAUSED_BY_A1，H-06 用 foodCode 路径 PASS，已登记 P 级池 P1-POS-FOODID-MAP-001）④ L-04 Scope/实施记录 untracked（FROZEN 文档不可改，内容核对一致）⑤ L-05 OrderVO 无 orderNumber + scan-serve materialConsumed 短路（HTTP 不回传，以 DB 为准，展示层问题）；OBS-01 NOT_CAUSED_BY_A1、OBS-02 全仓既有失败为范围外（A1 未触碰 Flyway/扣料核心，CC-6 33/33 保持）、OBS-03 登记 L-05——均为观察/登记非 FAIL；**FAIL=0 → 本地回归门禁允许放行（生产仍 PROVISIONAL）**；既有基线抽查无受影响（A1 diff 仅 2 文件，不触碰既有财务/权限/数据治理模块）；**Regression Result: PASS**（FAIL=0，回归门禁允许放行；生产证据缺口 L-01 登记）
**Regression Result（2026-09-24 Batch P1-COMBO-ORDER-001 套餐下单/KDS 组合套餐专项，发布批次）：PASS**——P1-COMBO-ORDER-001 卡 QA 独立验收 **PASS_WITH_LIMITATION**（`docs/quality/P1-COMBO-ORDER-001-qa-report.md`，2026-09-24：验收 6 项 **6/6 PASS**；抽检汇总 DB 断言独立复现 **5/5** + KDS live `recent/full` `components[]` PASS + 代码抽检 PASS（commit `aec5c45` 恰 15 文件与实施报告 §6 逐文件一致、无 Scope 外）+ 单测重跑 **37/37 EXIT=0**（`PosOrderCreateServiceFoodIdMapTest` 5 + `OrderNewServiceImplDeductTest` 28 + `OrderNewServiceImplOrderNumberA1Test` 3 + `MaterialDeductionAuditSelfFailureIntegrationTest` 1）；**FAIL=0、无 `-R{n}`、无 BLOCKED**；DS 抽检 §11 = **READY_FOR_QA 6/6**；证据链 commit：`aec5c45`（15 文件业务）+ `40fc776`（DS §11）+ QA 报告 commit `24a1f60`）；**关键证据**：① 套餐单 `T20260924004` `product_type=2, combo_id=1, food_id=NULL`（合法套餐 null，`food_id IS NULL AND product_type=1`=0）② KDS `GET /api/v1/kitchen/orders/recent/full` HTTP 200，套餐行 `productType=2 comboId=1 components=[{foodId:1,quantity:1,name:生菜串}]` 单卡片展开、其余 24 行单品不误展开、`combo_ingredients` 生效行（deleted=0）交叉核验一致 ③ 套餐出餐 `KO1790260472189` `material_consumed=1 status=served` + 库存 log id=62 生菜 8.500→8.400（与 log id=61 库存链连续）④ 单品零回归 `T20260924003` `product_type=1 food_id=1 combo_id 空` + `KO1790260472187` `material_consumed=1` + log id=61 8.600→8.500 ⑤ 静默点：`material_consumption` 2026-09-24 新增 FAILED=0（全表 failure_type 仅 2 条 ITEM_FOOD_ID_NULL 均为 09-23 FOODID 卡既有）⑥ 单测 37/37；**新增基线 REG-ORDER-008~011（正式 121 → 125 条，只增不减、既有条目零修改）**；**QA PWL 限制逐条确认不阻断**：① **L-01 UI 浏览器目检未做**（POS 套餐入口点击流 / KDS 卡片实拍——代码级 §3.5 + API live §2 + `vue-tsc` 已覆盖可自动化面，建议回归阶段补浏览器断言；不 FAIL，与 FOODID 口径一致）② **L-02 生产证据缺失**（`PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`，阻断仅限生产放行，不阻断 local 回归准入）③ **L-06 legacy 三文件仍读旧表**（`PosApiServiceImpl`/`KitchenScanServiceImpl`/`OrderMaterialRequirementServiceImpl` 仍为 `ComboIngredientMapper`——实施报告 §7.1 明确划归**第二步卡**、Scope 内声明的后续工作，非本卡缺陷）；另 L-03 工作区混杂（既有债，本卡 commit 恰 15 文件未夹带）/ L-04 报告行号漂移（语义一致，DS §11.2.1 登记）/ L-05 soft 路径 continue 无日志（HEAD 既有，非新增静默点）/ L-07 HTTP `materialConsumed` 可能回 0（以 DB `material_consumed=1`+库存 log 为准，已独立证实）/ OBS-P1 任务池同步（planner 项）——均非阻断；**FAIL=0 → 回归门禁允许放行（生产仍 PROVISIONAL）**；既有基线抽查无受影响（本卡 15 文件限于 POS/KDS 下单与展示链，不触碰既有财务/权限/数据治理模块；A1 的 REG-ORDER-001~007 断言语义保持）；**Regression Result: PASS**（FAIL=0，回归门禁允许放行；L-01/L-02/legacy 第二步卡登记）
**联调闭环进展（2026-08-10）**：KL-029（`sprint-3-closeout-regression-liaison.md` PASS）、KL-030（同报告仿真 10/10 PASS，浏览器下载 PWL 残留）已闭环；KL-013 主链路 PASS（浏览器渲染 PWL 残留）；KL-031 **已闭环（2026-08-10，`docs/quality/p0-kl-031-rerun.md`）**——DEF-3 修复（P0-MOCK-001 QA 复验 PASS）+ 三端点真实登录态活体重跑 PASS，残留 R1 浏览器通知面板 UI 交互待发布前冒烟；**KL-020 已全关（2026-08-10）**——DEF-1/DEF-2 经 P0-DATA-009 修复 + QA 复验 PASS_WITH_LIMITATION（`p0-data-009-r1-qa-report.md`）+ 重跑活体闭环（`p0-kl-020-rerun.md`）；重跑暴露的 **DEF-4（充值统计字段恒 0）** 已由 **P0-DATA-010 修复并经 QA 复验 PASS（`p0-data-010-r1-qa-report.md`，7/7）**，基线化为 REG-DATA-017 → 活跃缺陷全部关闭，**全局 Release Gate 由 FAIL 转为 PASS（见门禁表「DEF 修复批次」行）**。
**最终全量回归执行（阶段②，2026-08-10，`docs/quality/final-release-gate-sprint3.md`）**：57 条基线全量执行完成——PASS 31 / PASS_WITH_LIMITATION 26 / **FAIL 0**；活体抽样 6 项（/me 340 权限、members/stats/overview、recharge-stats/overview、notification/notifications、finance/accounting-periods、operations/live-monitor/trend）全部 HTTP 200 + code=0；代码级核验（grep/diff/静态）覆盖 REG-CLEAN-001/002（grep 0 残留）、REG-SEC-001/003/004（域矩阵 fail-closed、/seal 显式 roles、parseJWTPayload 0 残留）、REG-FIN-001/002（recalculateAllBalances 存在、tax-records POST + 鉴权）、REG-DATA-001/002/005（编辑走 PUT update 分流）。**阶段②变化回写**：REG-DATA-013 DEF-4 子项关闭（P0-DATA-010 QA 复验 PASS + REG-DATA-017 基线化，见 REG-DATA-013 限制说明）；REG-SEC-004 L-5 更新为「KL-013 HTTP 级已闭环（阶段①），浏览器 UI 残留待发布前冒烟」（见 REG-SEC-004 限制说明）；REG-MOCK-005 V-3 闭环（KL-031 重跑活体 PASS，见 REG-MOCK-005 限制说明）。

---

## 基线总览

| 测试编号 | 模块 | 业务场景 | 关联任务 | 测试类型 | 当前状态 |
|---------|------|---------|---------|---------|---------|
| REG-DATA-001 | 门店-物资需求 | 编辑已有物资需求不产生重复 | P0-DATA-001 | 数据一致性检查 | PASS |
| REG-DATA-002 | 营销-会员 | 会员编辑不新增、tags 不丢失 | P0-DATA-002 | 数据一致性检查 | PASS |
| REG-DATA-003 | 资产-处置 | 处置编辑不重复生成、审批不重复 | P0-DATA-003 | 数据一致性检查 | PASS |
| REG-DATA-004 | 追溯-质检 | 质检编辑不重复创建、检验项保留、abnormalLevel 不被覆盖 | P0-DATA-004 | 数据一致性检查 | PASS |
| REG-DATA-005 | 采购-物资需求 | 采购需求编辑 update 正确、新建正常、提交审批正常 | P0-DATA-005 | 数据一致性检查 | PASS |
| REG-FIN-001 | 财务-总账 | 科目余额为真实聚合，无硬编码假账 | P0-FIN-001 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-FIN-002 | 财务-缴税 | 缴税真实落库，无假成功 | P0-FIN-002 | 手工回归 | PASS |
| REG-FIN-003 | 财务-缴税 | 错误真实透传，不降级为"需配置授权" | P0-FIN-003 | 手工回归 | PASS |
| REG-FIN-004 | 财务+营销 | 资金域 12 接口越权访问被拒绝 | P0-FIN-004 | API回归 | PASS |
| REG-HR-001 | HR-薪资 | 薪资核算真实、导出真实，未就绪功能禁用 | P0-HR-001 | 手工回归 | PASS_WITH_LIMITATION |
| REG-HR-002 | HR-培训 | 导入/导出/报名真实落库 | P0-HR-002 | 手工回归 | PASS_WITH_LIMITATION |
| REG-HR-003 | HR-合同模板 | 模板保存/删除持久化 | P0-HR-003 | 手工回归 | PASS |
| REG-HR-004 | HR-员工画像 | 风险状态无假成功（禁用+明确提示） | P0-HR-004 | 手工回归 | PASS_WITH_LIMITATION |
| REG-HR-005 | HR-Mock治理 | 无失效 mock 端点被调用，残留演示源有标识 | P0-HR-006 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-HR-006 | HR-合同 | 到期提醒设置持久化 | P0-HR-007 | 手工回归 | PASS_WITH_LIMITATION |
| REG-HR-007 | HR-审批 | 审批操作无静默成功 | P0-HR-008 | 手工回归 | PASS |
| REG-HR-008 | HR-表单/列表 | 保存等待真实结果，失败不关窗且提示 | P0-HR-009 | 手工回归 | PASS_WITH_LIMITATION |
| REG-HR-009 | 员工门户 | 工资条真实、排班禁用，无假数据 | P0-HR-010 | 手工回归 | PASS_WITH_LIMITATION |
| REG-HR-010 | HR-API断流 | 桩接口无白屏，未就绪入口禁用+提示 | P0-HR-011 | 手工回归 | PASS |
| REG-HR-011 | 门店-排班 | 排班写操作无假成功（禁用+提示） | P0-HR-012 | 手工回归 | PASS_WITH_LIMITATION |
| REG-RULE-001 | 财务-缴税 | 缴税重复缴纳规则未擅自实现 | BLOCKED_PRODUCT_RULE-1 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-RULE-002 | HR-培训 | 培训重复报名限制已移除，无静默跳过 | BLOCKED_PRODUCT_RULE-2 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-RULE-003 | 财务-总账 | 科目期初结转规则未擅自实现 | BLOCKED_PRODUCT_RULE-3 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-SEC-001 | 权限安全-路由守卫 | 域矩阵默认拒绝：员工敏感域全拒 / OWNER·ADMIN 全放行 | P0-SEC-003 | 路由守卫仿真回归 | PASS |
| REG-SEC-002 | 权限安全-菜单路由 | 菜单-路由对齐：14 域对照 / URL 直达拦截 / seal 不误拦 | P0-SEC-004 | 路由守卫回归 | PASS_WITH_LIMITATION |
| REG-SEC-003 | 权限安全-敏感路由 | company-init / device-whitelist / supplier-links 路由角色 | P0-SEC-005 | 路由守卫回归 | PASS |
| REG-SEC-004 | 权限安全-权限源 | /me 唯一授权源 / 未知角色 fail-closed / /me 失败登出 | P0-SEC-006 | 权限源回归 | PASS_WITH_LIMITATION |
| REG-SEC-005 | 权限安全-业务角色 | 三角色业务域放行、其他域拒绝 | P1-SEC-001 | 路由守卫仿真回归 | PASS |
| REG-SEC-006 | 权限安全-公开路由 | requireAuth:false 判定 / verify-code、device-activation 未登录可用 | P1-SEC-002 | 路由守卫回归 | PASS |
| REG-SEC-007 | 权限安全-权限码 | 权限码注册表统一：前端注册表 + 后端发放清单 | P1-SEC-003 | 权限码核验 | PASS_WITH_LIMITATION |
| REG-SEC-008 | 权限安全-会话 | 401 无「留在本页」/ 关闭即登出 / 防重 | P1-SEC-004 | 手工回归 | PASS |
| REG-SEC-009 | 运维/系统-高危接口鉴权 | 员工/组长/排班员调用 6 组高危接口 → 403；OWNER/ADMIN → 200；data-fix 无 confirm 拒绝执行；成功调用落审计日志 | P0-SEC-002 | API回归（代码级；运行实测待回归阶段补） | PASS_WITH_LIMITATION |
| REG-SEC-010 | 全后端-Controller 鉴权 | 71 个裸 Controller 补 @PreAuthorize 至 100%（排除项口径：34 豁免 KL-043 + 30 BLOCKED PD-006~008 + 范围外 17）；已覆盖 194 接口权限码不变；扫描脚本 --strict 门禁 | P0-SEC-001 | API回归（代码级 + 扫描脚本；运行实测待回归/渗透阶段） | PASS_WITH_LIMITATION |
| REG-DATA-006 | 仓库-盘点 | 盘点团队人员真实，无假员工 | P1-DATA-001 | 数据一致性检查 | PASS |
| REG-DATA-007 | 仓库-补货 | 供应商下拉真实，无假供应商 | P1-DATA-002 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-DATA-008 | 仓库-调整 | 参与部门真实，无硬编码部门 | P1-DATA-003 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-DATA-009 | HR-合同 | 合同模板真实，无假模板 | P1-DATA-004 | 数据一致性检查 | PASS |
| REG-DATA-010 | 供应商门户-链接 | 签约模板真实、无伪造签约/访问记录 | P1-DATA-005 | 数据一致性检查 | PASS |
| REG-DATA-011 | 仓库-库位 | 库位操作记录无假记录、缺失如实标注 | P1-DATA-006 | 数据一致性检查 | PASS |
| REG-DATA-012 | 系统-AI模型 | 服务商列表真实派生，无硬编码服务商 | P1-DATA-008 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-DATA-013 | 营销-会员 | 会员统计真实聚合，无硬编码 0 | P1-DATA-009 | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-DATA-014 | 系统-组织下拉 | 组织/门店切换驱动下拉刷新，失败清空旧选项 | P1-DATA-010 | 手工回归 | PASS_WITH_LIMITATION |
| REG-MOCK-001 | 门店-招聘 | 招聘写操作无假成功（禁用+明确提示） | P1-MOCK-001 | 手工回归 | PASS_WITH_LIMITATION |
| REG-MOCK-002 | 仓库-盘点计划 | 盘点计划无假数据（空态+禁用），盘点记录走真实接口 | P1-MOCK-003 | 手工回归 | PASS |
| REG-MOCK-003 | 仓库-智能补货 | 采购建议提交真实落库，建议记录无假历史 | P1-MOCK-004 | 手工回归 | PASS_WITH_LIMITATION |
| REG-MOCK-004 | 系统-角色管理 | 角色绑定用户列表真实，失败透传错误 | P1-MOCK-009 | 手工回归 | PASS |
| REG-MOCK-005 | 多模块-假成功组 | 本地变更假成功清零：禁用/真实接口/真实字段 | P1-MOCK-010（R1 复验） | 手工回归 | PASS |
| REG-EXPORT-002 | 多模块-导入导出 | 导出文件真实可解析，导入无假成功 | P1-EXPORT-002 | 手工回归 | PASS_WITH_LIMITATION |
| REG-API-001 | 前端-召回统计 | 召回统计失败显示错误态（-- 非 0 伪装），无「已分析批次」布尔伪造 | P1-API-002 | 数据一致性检查 | PASS |
| REG-API-002 | 前端-资产维护 | 维护编辑无假操作：阻止关闭+明确未保存提示，字段映射正确 | P1-API-004 | 手工回归 | PASS |
| REG-API-003 | 前端-合同创建 | 正文生成成功文案条件化：失败仅 warning、无模板无正文文案 | P1-API-005 | 手工回归 | PASS |
| REG-DATA-015 | 仓库-出库 | 出库目标供应商真实（getEnabledList），无假供应商硬编码 | P1-DATA-011 | 数据一致性检查 | PASS |
| REG-CLEAN-001 | 代码卫生-过时注释 | 缴税/培训过时注释为事实口径（PD-00x + 后端当前不拦截），grep 0 残留，无规则断言 | P2-CLEAN-005 | 数据一致性检查 | PASS |
| REG-CLEAN-002 | 代码卫生-死代码 | closeProfitAndLoss/closeYearProfit 假实现删除，0 引用（含接口声明），聚合不破坏，编译通过 | P2-CLEAN-006 | 数据一致性检查 | PASS |
| REG-DATA-016 | 营销-会员统计/运营趋势 | overview 真实聚合可用（DEF-1）+ 趋势三接口错误透传无静默降级（DEF-2） | P0-DATA-009 | 数据一致性检查 | PASS（2026-08-11，唯一限制 KL-048 关闭） |
| REG-DATA-017 | 营销-充值统计 | 充值统计字段真实（非空充值场景）：overview.rechargeThisMonth 与 DB 一致、recharge-stats 11 字段对账 | P0-DATA-010 | 数据一致性检查 | PASS |
| REG-DATA-018 | 运营-趋势（admin 可见性 + null 防御） | 运营趋势 admin 可见：null=不限制 4 处（LiveMonitor/DecisionBoard/OperationsReport）+ kpi-summary?storeIds 无 NPE（500 消除） | KL-048（O-DEF2-1） | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-MOCK-006 | 系统-通知中心 | notification 列表可用 + read/read-all 落库 + business 列兼容（DEF-3） | P0-MOCK-001 | 数据一致性检查 | PASS |
| REG-ASSET-001 | 资产-折旧（AssetDepreciation + asset 域共享分块） | 资产页恢复渲染（折旧/盘点/台账不白屏）+ 折旧导出 CSV 可用（9 列 / fenToYuan 200.00 / 真实数据行） | P0-ASSET-001（DEF-A） | 数据一致性检查（浏览器冒烟 + CSV 解析） | PASS（2026-08-11） |
| REG-DEVICE-001 | 设备-接口/告警（DeviceController / DeviceAlertController） | 设备存在时三接口可用（deviceType 字符串往返 code=0）+ 告警导出 CSV 可用（9 列 / 告警内容 / 真实数据行），造数 0 残留 | P0-DEVICE-001（DEF-B） | 数据一致性检查（活体 API + 浏览器冒烟 + CSV 解析） | PASS（2026-08-11） |
| REG-FIN-005 | 财务-缴税 | 缴税幂等：同键重复→不新增+返回已有记录（id 相同）+幂等命中日志+唯一索引兜底 | P0-FIN-002（PD-001） | 数据一致性检查 | PASS |
| REG-FIN-006 | 财务-总账 | 期初人工录入+审计：录入/调整生效+联动重算+审计（人/时间/原因）+非授权 403+无自动结转 | P0-FIN-001（PD-003） | 数据一致性检查 | PASS |
| REG-HR-012 | HR-培训 | 报名唯一性：同键拒绝+已报名提示+DB 无第二条+原状态保留 | P0-HR-002（PD-002） | 数据一致性检查 | PASS_WITH_LIMITATION |
| REG-HR-013 | HR-智能模块 | HR 智能 3 页面下线：菜单/路由无入口+URL 拦截不渲染+dist 无产物 | P0-HR-005（PD-004） | 手工回归（代码级+构建产物扫描） | PASS |
| REG-ETM-106 | 仓库-库存（inventory 补列落库 + idx_inventory_batch_no 索引落库） | 新环境按 migration 链重建后 inventory 11 漂移列存在（类型/默认值/COMMENT 断言）+ idx_inventory_batch_no 存在且唯一（btree on batch_no、count=1、indisvalid=t、rank 193=20260817.001、checksum -916957038）+ WAREHOUSE/STORE 收货确认链路无 500 + selectInventoryPage/batchNo 筛选查询面 + inventory_transactions 写入核对 | RM-B2-实施卡1（ETM-106）+ RM-B2-实施卡3 | 数据一致性检查（psql 只读断言 + Flyway checksum 独立计算 + HTTP 实测 + git 核验） | PASS（2026-08-16；2026-08-18 实施卡 3 索引落库断言扩展） |
| REG-ETM-001 | 入职-注册码链路（registration_code） | 注册码生成/校验/核销链路（含 OnboardingRecordServiceImpl 依赖链）：PD-014 落库后扩展——SQL 层链路断言（insert/findByCode/updateToUsed/countValidCodes 独立验证）+ migration 断言（rank 190/checksum MATCH/12 列+PK+2 索引+补列）；HTTP 成功路径断言**待 DR-01**（OnboardingRecord 实体 createdAt/updated_at/interview_id 列漂移，Batch 2+）；Mapper 修复（updated_at→update_time）**已部署生效（R-2，2026-08-16 19:1x 实测：运行实例 jar 字节码 + SQL 层回滚验证）** | OICBE-B1-001（ETM-001） | 数据一致性检查（flyway/information_schema 只读断言 + 事务回滚验证 + 源码核验 + mvn compile 独立复核 + 部署产物字节码核验） | PASS_WITH_LIMITATION（2026-08-16 裁决落地后断言扩展登记；2026-08-16 19:1x R-2 生效验证补入） |
| REG-ETM-002 | dashboard-今日销售概览 | 今日销售概览链路：维持「明确错误态」断言（无 BadSqlGrammarException 吞错假空；code:500「今日销售概览数据暂不可用」明确错误态，72f0d5a 未回退）；PD-015 阶段一完成（映射表 `docs/quality/sales-order-orders-mapping.md` 为阶段二输入），阶段二各批 QA PASS 后逐批扩展真实数据断言（2026-08-18：阶段二批 1 QA 报告未产出，扩展候选待 QA） | OICBE-B1-002（KL-054） | 数据一致性检查（git diff 核验 + 运行时日志对账 + live HTTP 冒烟 + 阶段一验收结论引用） | PASS（2026-08-16 扩展登记：维持明确错误态） |
| REG-ETM-003 | 销售订单-SalesOrderDetail mapper | SalesOrderDetail mapper 链路：维持「明确错误态」断言（sales_order/sales_order_detail 缺表对齐前失败无假成功）；PD-015 阶段一完成（orders 唯一真相源 51 列/映射表 19+13 字段独立实测），阶段二各批 QA PASS 后逐批扩展真实数据断言（SELECT/INSERT/DELETE 无 500）（2026-08-18：阶段二批 1 QA 报告未产出，扩展候选待 QA） | OICBE-B1-002（KL-054） | 数据一致性检查（information_schema 只读断言 + Mapper 源码核验 + 阶段一验收结论引用） | PASS（2026-08-16 扩展登记：维持明确错误态） |
| REG-KL055 | 财务-预算（budgets） | /v1/finance/budgets 链路：PD-016 落库后扩展**读面断言**——GET /v1/finance/budgets → code:0 正常空列表（护栏放行独立复验，20260816.003/004 落库后，无 COUNT 短路假空）；写面断言**待 DR-02**（budget_name NOT NULL，Batch 2+）；表-only 列保留不删事实登记 | OICBE-B1-003（KL-055） | 数据一致性检查（flyway/information_schema 只读断言 + 护栏源码核验 + 独立 HTTP 复验 + 事务回滚验证） | PASS_WITH_LIMITATION（2026-08-16 裁决落地后断言扩展登记） |
| REG-EMP-001 | 员工端-断流如实提示（employee-frontend：LeavePage/LeaveOverview、SecurityLockPage、HomePage） | 断流如实提示文案保留断言：余额/考勤/锁屏/公告四区后端接线前维持如实提示/空态（dist 命中：余额 1/考勤 1/请假 1/锁屏 1/公告 2），假数据/假成功/静默伪装空态 0 命中（源码+dist）；构建/测试 EXIT=0 | OIC2-B4-001/002/003（OIC-2 Batch 4） | 手工回归（代码级 grep/读码/diff + dist 构建产物断言 + 构建/测试独立执行） | PASS（2026-08-18） |
| REG-UI-CORE-001 | core-DataTable 密度联动（DataTable.vue / layout.ts / MainLayout.vue） | DataTable density/size opt-in 默认关闭 + layoutStore 密度唯一真相源（compactMode 归一、无双向写残留）+ 115 消费方未 opt-in 渲染不变 + 3 处 size="small" 真实生效（P2-CORE-009） | P1-UI-CORE-001（Batch CORE-1，2026-08-31） | 手工回归（代码级读码/grep/diff + demo 联动验证位） | PASS（2026-08-31） |
| REG-UI-CORE-002 | core-SearchPanel 筛选保存（SearchPanel.vue） | SearchPanel storageKey 默认 undefined 零持久化（0 消费方现状零影响）+ fts_search_{key} 保存/恢复/reset 清空契约路径（page-scoped 页间不串） | P1-UI-CORE-002（Batch CORE-1，2026-08-31） | 手工回归（代码级读码/grep + 契约核验） | PASS（2026-08-31） |
| REG-UI-CORE-003 | core-DataTable/TableActionBar 批量动作+列显隐+N/M（TableActionBar.vue / DataTable.vue / BatchResultFeedback.vue） | TableActionBar 列显隐真实触发（column-change/update:columns）+ DataTable batchActions 默认 false（opt-in 批量条）+ BatchResultFeedback 纯展示零接口调用 + @selection-change 既有消费方不破坏 | P1-UI-CORE-003（Batch CORE-1，2026-08-31） | 手工回归（代码级读码/grep + demo 验证位） | PASS（2026-08-31） |
| REG-UI-CORE-004 | core-DataTable 行内展开（DataTable.vue） | expandable 默认 false opt-in + #expand 插槽（type="expand" 列）+ 生产消费方 0 未 opt-in 渲染不变 + treeProps 原生并存 | P1-UI-CORE-004（Batch CORE-2，2026-08-31） | 手工回归（代码级读码/grep/diff + demo 验证位） | PASS（2026-08-31） |
| REG-UI-CORE-005 | core-DataTable 合计行 + useSummary（DataTable.vue / useSummary.ts） | showSummary 默认 false + summaryMethod core 默认（首列「合计」其余空白）+ useSummary 仅复用 utils/money（fenToYuanNumber/fenToYuanDisplay/yuanToFen，零自造格式化） | P1-UI-CORE-005（Batch CORE-2，2026-08-31） | 手工回归（代码级读码/grep + demo 金额推演验证位） | PASS（2026-08-31） |
| REG-UI-CORE-006 | core-DataTable 固定列 + useStandardPage 分页（DataTable.vue / useStandardPage.ts / DishCostAnalysis.vue） | fixed 接口透传 + 操作列 fixed="right" 已具备 + pageSizes 默认 [10,20,50,100] + handleSizeChange/handleCurrentChange + pagination 默认 undefined 零渲染 + DishCostAnalysis 静默失效→真实分页（修复性行为变化） | P1-UI-CORE-006（Batch CORE-2，2026-08-31） | 手工回归（代码级读码/grep/diff + demo 验证位） | PASS（2026-08-31） |
| REG-UI-CORE-007 | core-StatusTag 语义键 + tokens 状态色（_tokens.scss / _dark-mode.scss / StatusTag.vue） | StatusTag overdue/abnormal 语义键深浅双主题：浅色 :root 值（#bf360c/#fbe9e7/#ffccbc、#ad1457/#fce4ec/#f8bbd0）+ 深色 html.dark 值（#ff7043/#33140a/#5a2a12、#f06292/#2a0f1e/#5a2240）与既有键同构渲染（color/bg/border 三元组 + 边框块复声明）+ 既有键零变化 + light/solid 变体 6 变量零悬空 + 构建产物 6 深色值 in html.dark | P1-UI-CORE-007（Batch CORE-2，R1 复验闭环，2026-08-31） | 手工回归（代码级读码/grep/diff + 构建产物静态核验 + typecheck/build） | PASS（2026-08-31，R1 闭环转正） |
| REG-UI-FIN-001 | 财务-资金流水（FinanceFund.vue） | 资金流水页接入 core 能力（固定列 日期/摘要 fixed left、余额 fixed right；density="auto" 消费 layoutStore；SearchPanel storage-key="finance-fund-filter" 持久化；expand 行内详情；pagination + show-summary unit='yuan' 复用 useSummary）+ 业务语义零变更（API 调用 3 处与改动前一致）；PD-028 待核销不实现（限制观察）；批量不支持项登记 | P1-UI-FIN-001（Batch FIN-1，2026-08-31） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-08-31） |
| REG-UI-FIN-002 | 财务-账本/凭证（FinanceLedger.vue + converters.ts + voucher.ts + types/finance.ts） | 账本页接入 core 能力（固定列/密度/SearchPanel storage-key="finance-ledger-filter"/expand 分录/分页 + 合计 unit='fen' 与行展示同口径）+ Batch0-方案3 凭证状态映射对齐（后端 0-3 = draft/audited/posted/cancelled，双向映射正确，修复前非 0 状态全错档，按钮可用性零改动，后端契约不变）；金额口径复用 Converter | P1-UI-FIN-002（Batch FIN-1，2026-08-31） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-08-31） |
| REG-UI-FIN-003 | 财务-应收（FinanceReceivable.vue） | 应收页接入 core 能力（固定列 应收编号 fixed left/状态 fixed right；density="auto" 消费 layoutStore；SearchPanel storage-key="finance-receivable-filter" 持久化；expand 行内详情同字段 11 项；pagination + show-summary unit='yuan' 复用 useSummary；**overdue 键真实字段依据** 后端 status 4=overdue）+ 业务语义零变更（API 调用 2 处与改动前一致）；PD-028 待核销不实现（限制观察）；批量不支持项登记 | P1-UI-FIN-003（Batch FIN-2，2026-08-31） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-08-31） |
| REG-UI-FIN-004 | 财务-应付（FinancePayable.vue） | 应付页同口径（固定列/密度/SearchPanel storage-key="finance-payable-filter"/expand 同字段 13 项/pagination + 合计 unit='yuan'/overdue 键同源依据）+ 业务语义零变更（API 调用 3 处与改动前一致）；「待付款」无独立字段不发明（既有统计卡片 label 零变更）；PD-028 待核销不实现（限制观察）；批量不支持项登记 | P1-UI-FIN-004（Batch FIN-2，2026-08-31） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-08-31） |
| REG-UI-TRACE-C01 | 财务-资金流水（契约对齐） | 流水页字段契约对齐：FundFlowDataConverter.toFrontend 9 项映射（businessDate→transactionDate / flowDirection→flowType 1收2支 / balanceAfter→balance / accountName→bankAccountName / counterpartyName→counterparty / voucherId→String 穿透键 / summary=remark 兜底）+ 悬空 status 映射移除（PD-028 登记链维持，落库前恒「未知」兜底）+ FundFlowVO 对照一致；8 断流字段 5 对齐/1 兜底/2 登记 | P1-UI-TRACE-C01（Batch TRACE-1，2026-09-03） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-03） |
| REG-UI-TRACE-C02 | 财务-账本/凭证（契约对齐） | 凭证页契约对齐：VoucherDataConverter.toFrontend 9 项映射（voucherStatus→status 0-3 语义不变）；referenceNo/sourceType/sourceId 穿透键入契约就绪（A01 前置达成）；后端填充缺口（操作人/details）登记不伪造 | P1-UI-TRACE-C02（Batch TRACE-1，2026-09-03） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-03） |
| REG-UI-TRACE-C03 | 财务-应收（契约对齐） | 应收 Converter 补齐：id/originalAmount→amount/balanceAmount→remainAmount/aging=computeAging 与应付同构；收款入口全链路恢复（find 命中 → ReceiptDialog receivableId 提交正确）；GAP-C4（invoiceDate/invoiceNo）登记 '-' 兜底无虚构 | P1-UI-TRACE-C03（Batch TRACE-1，2026-09-03） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-03） |
| REG-UI-TRACE-A01 | 财务-资金流水/凭证（来源展示 + 正向穿透） | 流水来源列展示：来源单据号 voucherNo 预留 → remark 首个子句文本兜底（付款单：FKxxx/收款单：SKxxx，与 PaymentServiceImpl.java:162 / ReceiptServiceImpl.java:144 写入格式逐字一致，完整文本 title 展示，不可结构化穿透）|| '-' 不虚构 + 来源类型 flowCategory（FundFlowCategoryMap 7 类=FundFlow.java:37-41）+ 来源时间 businessDate；流水→凭证穿透真实可达（v-if row.voucherId → router.push FinanceLedger query.voucherId → onMounted 读 query → 复用既有 voucherApi.getById + 详情弹层，与「查看」同路径，零新增接口）；凭证→单据文本展示（referenceNo/sourceType/sourceId + VoucherSourceTypeMap 8 类=FinanceVoucher.java:59-63）；不可行方向登记（GAP-A2/B1/B3 → BE01）不伪造 | P1-UI-TRACE-A01（Batch TRACE-2，2026-09-03） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-03） |
| REG-UI-TRACE-CL01 | 财务-4 页（口径标注条） | CaliberNoteBar 共享组件标注条三要素：金额单位「元（数据库按分存储，页面展示为元）」+「管理台账口径」标签（tooltip 挂 PD-031 决策原文）+「管理口径」标签（tooltip 挂 PD-032 决策原文，与 product-decision-backlog.md:47-48 逐字一致）；财务 4 页 PageHeader 下方接入（FinanceFund:293 / FinanceLedger:406 / FinanceReceivable:223 / FinancePayable:240）；零数据依赖/零接口调用/零金额口径变更（grep axios/request/fetch/http/fenToYuan/yuanToFen/toLocaleString 零命中） | P1-UI-TRACE-CL01（Batch TRACE-2，2026-09-03） | 手工回归（代码级读码/grep + 决策池对照 + 构建独立复跑） | PASS（2026-09-03） |
| REG-UI-TRACE-D01 | 财务-应收/应付（异常行醒目） | 应收/应付逾期行整行高亮：rowClassName（FinanceReceivable.vue:57-63 / FinancePayable.vue:59-65，`row.status === 'overdue' ? 'row-overdue' : ''`）+ `:row-class-name` 绑定（:264/:281）+ scoped `.row-overdue`（:333-339/:355-361，`var(--fts-status-overdue-bg) !important` 重复类名提升特异性覆盖 core 斑马纹/悬停）；判断复用既有 status 字段（4=overdue 由 ReceivablePayableStatusMap.toFrontend 映射 converters.ts:169-175，FIN-003/004 已验收口径，零新增状态逻辑——展示性行类选择）；tokens overdue 三元组深浅主题（_tokens.scss:124-126 + _dark-mode.scss:138-140）；core DataTable 零改动（仅消费既有 rowClassName 透传 DataTable.vue:42/84/212）；FinanceFund.vue 零改动（未勾稽=Batch0 依赖，恒「未知」兜底保持）；「对不上」不可行未标注（登记不伪造） | P1-UI-TRACE-D01（Batch TRACE-3，2026-09-03） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-03） |
| REG-FIN-WS-001 | 财务-对账工作台（FinanceFund 重构） | 对账工作台五区布局（待办区任务卡 + 4 统计卡片真实数据、PD-028 登记不虚构计数 / 工具栏=新建流水真实入口 + 导出/日结 disabled+tooltip 明示依赖 / 筛选区 finance-fund-filter / 数据区 CaliberNoteBar + 失败态 + 引导空态 + DataTable / 详情区 expand + A01 穿透）+ 新建流水真实入口（fundFlowApi.create 既有端点 + toCreateDTO 与 FundFlowCreateDTO 逐字段一致，delete status/voucherNo）+ 导出/日结按钮 disabled+tooltip 明示依赖（EXPORT-001 结论 + 确认⑨ 日结=勾稽锁定 + PD-028 落库依赖，登记非 FAIL）+ E 值格式修复（value-format=YYYY-MM-DD 与后端 LocalDate 匹配）+ 失败透传（catch→loadFailed+EmptyState 重试）+ 引导型空态 + 菜单标题「对账工作台」三处（路径/组件/scaleLevel 零变更）+ 修正①边界（收付款执行动作零新增）+ 增强能力 9 项零回归（A01/D01/CaliberNoteBar/density/固定列/expand/useSummary/pagination/筛选保存） | P1-FIN-WS-001（Batch WS-1，2026-09-03） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-03） |
| REG-FIN-WS-002 | 财务-核销工作台（FinanceReconciliation + ReceivableTab/PayableTab，应收/应付合并重构） | 核销工作台双页签 + 路由重定向（/finance/reconciliation 新路由 + 旧路由 /finance/receivable→?tab=receivable、/finance/payable→?tab=payable 带参重定向 + tab query 同步 router.replace + watch(route.query.tab) + 深度链接直达页签）+ 修正①收付执行动作本台（ReceiptDialog/PaymentDialog 行内入口保留 + 核销进度列 receivedAmount/paidAmount 真实字段派生，后端汇总字段实锤 ReceivableServiceImpl.java:130 / PayableServiceImpl.java:138）+ 坏账核销 writeOff 入口（既有端点 receivable.ts:104-106 + ElMessageBox 二次确认，未结清显示）+ 筛选补缺（**应收到期日区间端到端有效**：ReceivableTab:185-188→ReceivableServiceImpl:103-105→ReceivableMapper.xml:16-21 due_date 过滤；**应付到期日区间 = 登记处置**【R1 闭环：控件 disabled + tooltip 明示依赖 :403-418、loadData 零死参数发送 :199-200、handleFilterRestored/reset 三路径恒 null :222/:242——**F-1 假筛选防回归点**，后端池登记排期 PayableServiceImpl 补传/mapper 参数/XML due_date 过滤；**应付创建日期区间端到端有效**：:195-198→PayableServiceImpl:111-113→PayableMapper.xml:16-21 create_time）+ 打印 window.print（纯浏览器能力）+ 导出/批量 disabled+tooltip 明示（EXPORT-001 + 无批量端点登记，不伪造）+ 增强能力零回归（D01 逾期行高亮/overdue 键/expand 同字段 11+13 项/useSummary unit='yuan'/useStandardPage 分页/密度/固定列/筛选保存 storage-key 保持/CaliberNoteBar/统计卡片 4 项）+ 失败透传 + 引导型空态 + 菜单合并「核销工作台」+ 强制核验 6/6（API 层零变更、后端零改动 mtime 实锤、typecheck 136=136 + build EXIT=0） | P1-FIN-WS-002（Batch WS-2，首验 FAIL F-1 → **-R1 复验 PASS 闭环**，2026-09-03） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-03，R1 闭环转正） |
| REG-FIN-WS-003 | 财务-记录工作台（FinanceRecords.vue 新建·壳 + components/LedgerTab.vue / ReimbursementTab.vue / TransferTemplateTab.vue 新建·页签子组件【FinanceLedger.vue / AutoVoucher.vue / InvoiceReimbursement.vue 已删除，内容迁移】+ router/index.ts + modules/finance/menu.ts + api/finance/voucher.ts + transfer-template.ts + types/finance.ts + views/finance/FinanceFund.vue 穿透目标单行；后端对照 AutoVoucherController.java / FinanceVoucherQueryDTO.java / VoucherServiceImpl.java / FinanceVoucherMapper.xml / TransferTemplateServiceImpl.java / InvoiceServiceImpl.java【只读确认，零改动】） | 记录工作台三页签化合并（凭证/报销/联动规则）：页签化合并（修正②：壳 = PageHeader「记录工作台」+ 待办区真实字段派生【draft/audited 计数 + 分页范围登记，加载中不虚构】+ el-tabs 三页签 + tab query 同步 router.replace/watch + 联动规则 scaleLevel 折叠确认⑤）+ 路由重定向（确认①：/finance/records 新路由 + 旧路由 /finance/ledger→?tab=ledger、/finance/invoice-reimbursement→?tab=reimbursement、/finance/auto-voucher→?tab=transfer-template 带参重定向 + /finance 根 redirect + FinanceFund.vue:316-319 A01 穿透目标修复 name FinanceRecords + { tab: 'ledger', voucherId } + LedgerTab onMounted voucherId 定位，旧路由 name 引用零残留）+ 工具栏处置（**批量过账真实接入**：voucherApi.batchPost 既有端点 AutoVoucherController.java:178-181 + 仅 audited 勾选 + BatchResultFeedback 提交数口径 + tooltip 明示后端仅布尔【N/M 明细后端池登记】；批量审核/凭证导入/导出 disabled+tooltip 登记不伪造；**红冲/作废真实接入**：invoiceApi.void/redFlush 既有端点 invoice.ts:137-152 + ElMessageBox 二次确认 + 仅 issued 态显示，审批流保留既有语义确认⑥；**联动规则 CRUD 真实接入**：transferTemplateApi.create/update/delete 既有端点 + 契约对齐 TransferTemplateVO + 科目选择器 subjectApi.getLeafSubjects，映射缺失回退 科目#ID 不虚构）+ 筛选修复（**voucherNo / voucherStatus 键名修正 status→voucherStatus 端到端有效** / 期间端到端有效 / **voucherType 数字直传**【R1：voucher.ts:43-45 补数字分支——数字 1-7 直传后端 7 值语义、字符串分支 VoucherTypeMap 1~4 映射语义不变；**F-1 防回归点**：LedgerTab.vue:141 → voucher.ts:43-45 → FinanceVoucherQueryDTO.java:26 → VoucherServiceImpl.java:231-233 → FinanceVoucherMapper.xml:19-21 voucher_type eq 全链就绪】/ referenceNo 死参数零发送登记【后端池：Service 补传 + Mapper + XML reference_no LIKE】/ 联动 B-3/B-4 映射修正 enabled→isEnabled、templateName→keyword 端到端有效【TransferTemplateQueryDTO 绑定 → Service:118-126 消费 → MyBatis-Plus eq/like】/ 报销四维 invoiceNo/invoiceType/期间端到端有效保持 + E 值格式修复）+ KL-060 登记不猜测（驳回=作废共用 cancelled 不发明独立状态，审批流保留既有语义，壳待办区登记卡）+ KL-061 伪语义列移除（「最近执行」→「创建时间」真实字段不伪造）+ 增强能力零回归（C02 契约键 referenceNo/sourceType/sourceId / voucherId 穿透接收端 / expand 分录明细 / useSummary unit='fen' / 固定列 / density="auto" / 筛选保存 storage-key 保持+新 key / CaliberNoteBar / 失败态+重试 / Batch0-方案3 状态映射 VoucherStatusMap 0-3 零变更 / 统计卡片 / 详情弹层 / 审批流保留）+ 强制核验 6/6（typecheck 136=136 本批 11 文件零命中 + build EXIT=0 独立复跑；后端零改动 mtime 实锤【6 文件全 08-11】；V3-A 零引用；死参数零发送；PD-028 未落库不悬空；P1-STOCK-001 零触碰） | P1-FIN-WS-003（Batch WS-3，首验 FAIL F-1 → **P1-FIN-WS-003-R1 复验 PASS 闭环**，2026-09-04） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-04，R1 复验闭环转正） |
| REG-FIN-WS-004 | 财务-合规工作台（FinanceTax.vue 重构 + menu.ts + router/index.ts；后端对照 TaxRecordController.java / TaxRecordDTO.java / TaxRecordServiceImpl.java / TaxRecordMapper.java【只读确认，零改动】） | 合规工作台（FinanceTax 重构）——五区布局（待办区 :679-702 workbench-taskboard【待缴税项任务卡 = pendingTaxSummary computed :638-650 基于 Tab1 真实字段派生（UNPAID/OVERDUE 计数 + taxAmount 合计 + 逾期摘要，TaxRecordDTO.java:39 既有字段语义），recordLoaded 未加载「加载中…」不虚构 :639/:648 + 缴税幂等 PD-001 登记卡 :686-689 纯展示 + 统计卡片 4 张保留 :691-701 v-for statsCards】/ 工具栏 :704-713【申报表打印 :706 handlePrint :655-657 = **window.print 真实接入**；导出 :707-709 disabled + tooltip 明示 P1-FIN-EXPORT-004 后端池登记；批量缴税 :710-712 disabled + tooltip 明示无批量端点】/ 筛选区三页签内保留 A 类有效（Tab1 :720-738 / Tab2 :767-783 / Tab3 :809-828）/ 数据区 el-tabs 3 页签保留 :717（records :719-763 / declaration :766-805 / config :808-852）/ 详情区既有弹层 5 个零改动 :857/:875/:908/:928/:945）+ **KL-059 税期动态化**（buildTaxPeriodOptions :54-66【TAX_PERIOD_COUNT=4 :51 + new Date(now.getFullYear(), now.getMonth()-i, 1) + padStart → value=YYYYMM】+ taxPeriodOptions :70【Tab1 label=YYYYMM 与既有格式一致】+ declarationPeriodOptions :73【Tab2 label=YYYY年MM月】+ currentPeriod :76-80 + :277 默认期间=当前期；**Node vm 沙箱行为实证 6/6**【2026-09-04→["202609","202608","202607","202606"]、2026-01-15 跨年→["202601","202512","202511","202510"]、2026-06-15 与硬编码 202603~202606 集合完全等价、2025-01-10 再跨年、格式 16/16 全 `^\d{4}(0[1-9]|1[0-2])$`、Tab2 label 格式一致】；**YYYYMM 契约零变更**【TaxRecordDTO.java:17-19 String taxPeriod mtime 08-31 零改动 + calculate/generateReturn substring(0,4)/(4,6) 拆分 :327-328/:367 零触碰】；**硬编码 20260x 零残留**【5 处命中全为注释/说明 :5/:22/:50/:75/:726，无 el-option 硬编码】）+ 筛选消费链端到端（**taxPeriod 5 环**：:116 发送 → tax-calculation.ts:95-97 getRecords params 原样透传零映射丢弃 → TaxRecordController.java:44-53 DTO 绑定 → TaxRecordDTO.java:19 → TaxRecordServiceImpl.java:49-51 eq → TaxRecordMapper BaseMapper → SQL tax_period = ?；**taxType** :115 → Service:44-46、**taxStatus** :117 → Service:54-56 同链；空值 `|| undefined` 不发送【request.ts:467-478 axios 序列化丢弃 + Service null 检查】；**零死参数，Batch 2/3 F-1 教训范围零复发**）+ 增强能力（Tab1 useStandardPage defaultPageSize=100 :86【与既有 pageSize:100 一致——统计卡片/待办摘要金额范围与改动前零变化】+ DataTable pagination opt-in :741-751 + @page-change :222-224；density="auto" :746 + show-summary :747-748 + createAmountSummaryMethod :213【useSummary.ts:44-59 unit='yuan' 元→分→元同源，零自实现格式化】；Tab2 density + 合计 :791-793/:318 declarationSummaryMethod unit='yuan'；Tab3 CaliberNoteBar :831 + density :837 + 分页统一 core :838-839【原自定义 el-pagination 移除——grep el-pagination 元素零命中仅 :517 注释，handleSizeChange/handleCurrentChange 旧代码零残留，core P1-UI-CORE-006 内部管理】）+ **缴税幂等 handlePayTax 零触碰（PD-001 实现）**（:237-268【:238 payingRowId 单飞守卫 → :241-248 ElMessageBox 二次确认 → :251-260 payTax 既有端点幂等键字段齐备 → :261 成功提示 → :262-263 刷新+统计 → :264-265 错误提示 → :267 finally 释放】+ :759 缴税按钮仅 UNPAID/OVERDUE 行 + :loading 行级）+ 金额口径零变更（formatAmount 元 toFixed(2) :167-170 + 合计千分位 core useSummary）+ 强制核验 6/6（业务语义/接口/状态/金额口径零变更；后端零改动【TaxRecord 链 5 文件 mtime 2026-08-31 01:20:28 + Mapper 08-11 + tax-calculation.ts 08-31/tax-rate.ts 08-11 实锤】；视觉同源【core 组件 :30-42 + scoped 全 var(--fts-*) token，V3-A 零命中】；死参数零 + PD-028 未落库不悬空【待缴税项摘要基于真实 taxStatus 字段派生 + 「分页范围登记」标注 :649，零新增状态列/零 status 推断】+ P1-STOCK-001 零触碰；typecheck 136=136【本批 3 文件零命中】+ build EXIT=0 独立复跑）+ 独立页边界（路由 /finance/tax 保留 router/index.ts:128【路径/组件/name 零变更，仅 meta.title】+ menu.ts:29 标题「合规工作台」【路径/scaleLevel 零变更】+ role-profiles.ts:268 / permissions.ts:734 既有存量引用零触碰【本批修改文件仅 3 个，mtime 09-04 实锤】）+ 登记链（导出：EXPORT-001 盘点 finance-export-inventory-20260903.md:170 → 后端池 §11.6 L3951-3953 P1-FIN-EXPORT-004 → PD-035 product-decision-backlog.md:51 → tooltip → disabled 不伪造；批量缴税：无批量端点【tax-calculation.ts:107-109 单条 payTax + TaxRecordController.java:120-126 仅 batchDelete】→ disabled + tooltip，**不逐条循环**【grep payTax 仅 handlePayTax 内 1 处 :251 + 注释 2 处，v-for/批量循环零命中 + 无 el-table type="selection"】） | P1-FIN-WS-004（Batch WS-4，2026-09-04） | 手工回归（代码级读码/grep/diff + Node vm 行为实证 + 构建独立复跑） | PASS（2026-09-04） |
| REG-FIN-WS-005 | 财务-毛利核算（FinanceCost.vue 重构 + cost.ts + menu.ts + router/index.ts） | 毛利核算工作台五区布局 + 统计卡片 4 张保留（计算口径与旧版 git 对照逐字一致）+ **B-5 月期间筛选端到端 6 环零死参数**（monthrange+value-format YYYY-MM :308-317 → loadData :125-128 → cost.ts mapQueryParams :41-42 → CostRecordQueryDTO :26-30 → CostServiceImpl.getPage :63-67 ge/le(period) → SQL；period 格式 YYYY-MM 由 CostRecordCreateDTO @Pattern 强制）+ **summarizeByPeriod 真实接入**（cost.ts:112-131 补齐既有端点 GET /v1/finance/costs/summary，CostController.java:59-64 → CostServiceImpl:91-101 eq 聚合，非新增接口）+ 成本结构分析视图真实（期间选择 + CostTypeMap 标签 + fenToYuanNumber + useSummary 合计）+ 导出/导入 disabled+tooltip 登记（P1-FIN-EXPORT-004 + 无 import 端点）+ PD-032 口径标注（CaliberNoteBar「管理口径」与决策池原文逐字一致）+ 失败透传 + 引导空态 + 增强能力零回归（密度/合计 unit='yuan'/分页 defaultPageSize=20 与旧版一致/expand/详情弹层与 CostFormDialog 零改动）+ 菜单标题「成本管理」→「毛利核算」路径零变更 | P1-FIN-WS-005（Batch WS-5，2026-09-04） | 手工回归（代码级读码/grep/diff + git 旧版对照 + 构建独立复跑） | PASS（2026-09-04） |
| REG-FIN-WS-006 | 财务-决策工作台（FinanceReport.vue 重构 + types/finance.ts + menu.ts + router/index.ts） | 决策工作台 9 类报表聚合（reportTypeOptions 7 既有端点 + balance/cash_flow 2 P0 断流）+ 统计卡片仅真实数据展示（虚构零值卡片消除，git 旧版对照实锤）+ 利润表契约对齐（:109-119 ↔ types/finance.ts:1305-1320 ↔ FinancialReportServiceImpl:62-66，operatingIncome/details 断链修复）+ 报表→明细穿透（可行跳转 /finance/cost、/finance/reconciliation?tab=receivable|payable、/finance/budget；不可行「—」登记不伪造）+ P0 断流空态说明（KL-057/P1-FIN-BE-001 仅登记不伪造）+ 后端占位检测空态（账龄恒 0/预算待实现，后端事实独立确认）+ **cost_structure 统计卡片金额口径统一【R1 闭环：分→元一次转换，4 卡自洽（总=食材+人工+其他），F-1 二次 /100 防回归点**——FinanceReport.vue:274-277 全部 formatFenToYuan(分)，Node 独立复算 ¥1,000.00/¥600.00/¥200.00/¥200.00 与 QA 应值一致】+ 打印 window.print 真实 + 导出登记 + 菜单标题「财务报表」→「决策工作台」路径零变更 | P1-FIN-WS-006（Batch WS-5，首验 FAIL F-1 → **-R1 复验 PASS 闭环**，2026-09-04） | 手工回归（代码级读码/grep/diff + Node 口径独立复算 + 构建独立复跑） | PASS（2026-09-04，R1 闭环转正） |
| REG-FIN-WS-007 | 财务-会计科目（FinanceSubject.vue 筛选修复 + subject.ts/converters.ts 契约补齐） | 科目归位页——C 类 3 处假筛选根除（subjectCode/subjectName 双输入 + subjectType/status 数字转换，消费链 6 环零死参数）+ 树形剪枝（pruneTreeByMatchedIds 匹配集合剪枝保留祖先链 + default-expand-all 保留，filteredTree/filterTree 本地过滤零残留）+ subjectId→id 断链修复（convertTreeNode 递归 + converters.ts:486-490，编辑/新增子科目/启停恢复可用）+ 科目余额登记不伪造（AccountingSubjectVO 无 balance 字段，PD-031 联动待后端补）+ 导出/导入登记（P1-FIN-EXPORT-004 + PD-035 / 无 import 端点）+ 视觉同源 | P1-FIN-WS-007（Batch WS-6，2026-09-04） | 手工回归（代码级读码/grep/diff + 消费链逐环核验 + 构建独立复跑） | PASS（2026-09-04） |
| REG-FIN-WS-008 | 财务-会计期间（FinancePeriod.vue；后端 AccountingPeriodController/ServiceImpl 只读确认零改动） | 期间归位页——KL-058 根除（operatorId computed 会话取值零兜底 + 三动作防御 + 按钮 disabled+tooltip，`?? '1'` 零残留）+ 后端 operatorId 校验缺口登记后端池（伪造面收窄未根除如实登记）+ 年份筛选死参数登记不补 UI（getPage 无 year）+ 结账动作语义零变更（accounting-period.ts 零改动）+ 导出登记 | P1-FIN-WS-008（Batch WS-6，2026-09-04） | 手工回归（代码级读码/grep/diff + 构建独立复跑） | PASS（2026-09-04） |
| REG-FIN-WS-009 | 财务-预算（FinanceBudget.vue + types/finance.ts budgetType 数字契约；后端 BudgetController/QueryDTO/ServiceImpl 只读确认零改动） | 预算归位页——预算类型筛选补 UI（1~5 枚举收入/成本/费用/利润/现金流，消费链 7 环零死参数，getBudgetTypeLabel 数字→语义标签）+ responsibleDeptId 死参数登记不补 UI（Service 未消费，F-1 同型纪律）+ categoryId 无数据源登记 + 审批入口 PD-034 登记（BLOCKED 不占通过数）+ 双轨语义漂移登记 + 导出登记 | P1-FIN-WS-009（Batch WS-6，2026-09-04） | 手工回归（代码级读码/grep/diff + 消费链逐环核验 + 构建独立复跑） | PASS（2026-09-04） |
| REG-FIN-WS-010 | 财务-审批流配置（FinanceApproval.vue + approval-flow.ts；后端 ApprovalFlowConfigController/QueryDTO/ServiceImpl 只读确认零改动） | 审批流配置归位页——B-1/B-2 映射修正（enabled→isEnabled/configName→keyword，消费链 7 环零死参数）+ 字段级契约补齐（configId→id/approvalNodes↔nodes/isEnabled↔enabled，nodes/enabled 列断链修复）+ CRUD 4 端点真实接入（全为既有端点，端点集合零新增，getById=既有 GET /{id} 封装）+ toggleEnabled 契约修正（补 enabled 参数，原缺参必 400 根因消除）+ 审批执行链路未实现登记 + 审批人 ID 文本输入登记 + 导出登记 | P1-FIN-WS-010（Batch WS-6，2026-09-04） | 手工回归（代码级读码/grep/diff + 消费链逐环核验 + 构建独立复跑） | PASS（2026-09-04） |
| REG-FIN-RVT-001 | 财务-路由/菜单（结构回退） | 路由/菜单全局回退：13 页独立组件直达路由恢复（与 HEAD 基线同构）；/finance/reconciliation、/finance/records 路由与 5 条重定向移除（工作台入口彻底移除，grep 零命中 + dist 无 chunk）；13 项原菜单（标题/图标/scaleLevel=HEAD 原值）恢复；5 独立页壳新建承功能（内容回迁归 RVT-002/003） | P1-FIN-RVT-001（Batch RVT-1） | 手工回归（代码级 grep/diff + HEAD 基线同构对照 + dist 构建产物核验） | PASS_WITH_LIMITATION（2026-09-04） |
| REG-FIN-RVT-004 | 财务-4 页标题（结构回退） | 标题回退批：fund【资金管理】/tax【税务管理】/cost【成本管理】/report【财务报表】三处同步（menu/router/PageHeader）；permissions/role-profiles 文案零漂移；页面内增强 15/15 零回退（KL-059/B-5/summarizeByPeriod/9 类报表/P0 断流/WS-006-R1 金额口径修复/新建流水/E 值/失败透传/打印/口径标注） | P1-FIN-RVT-004（Batch RVT-1） | 手工回归（代码级 grep/diff + 保留项逐项命中 + 构建独立复跑） | PASS_WITH_LIMITATION（2026-09-04） |
| REG-FIN-RVT-002 | 财务-核销页（结构回退） | 核销页回退：FinanceReconciliation 壳 + ReceivableTab/PayableTab 页签删除（glob/dist 零残留 + emit 链路移除）；内容回迁独立 FinanceReceivable（575 行）/FinancePayable（595 行）完整页；保留项 15/15 零回退（writeOff/应收到期日端到端/应付到期日登记处置【零死参数+disabled+tooltip】/逾期高亮/核销进度列/打印/失败透传/导出批量登记/CaliberNoteBar/expand/合计/分页/密度/固定列/筛选保存）；待办区页面级增强（pendingSummary 真实字段派生不虚构，PD-033 归应收页）；穿透修复闭环（A01→FinanceLedger + drillTo 13 处→独立页，/finance/reconciliation?tab= 零残留） | P1-FIN-RVT-002（Batch RVT-2） | 手工回归（代码级 grep/diff/glob + dist 产物核验 + 保留项逐项命中 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-RVT-003 | 财务-记录页（结构回退） | 记录页回退：FinanceRecords 壳 + LedgerTab/ReimbursementTab/TransferTemplateTab 页签删除（glob/dist 零残留 + :132 注释清理闭环 + emit 链路移除）；内容回迁独立 FinanceLedger（785 行）/InvoiceReimbursement（660 行）/AutoVoucher（519 行）完整页；保留项 12/12 零回退（batchPost 仅 audited+BatchResultFeedback 提交数口径/红冲作废+审批流保留/联动 CRUD+B-3-B4 映射/voucherStatus 键名+voucherType 数字分支【WS-003-R1 修复保持】/referenceNo 零发送/筛选 4 维/expand+穿透接收端/失败透传/KL-060·061 登记/E 值格式/CaliberNoteBar/useSummary unit='fen'）；scaleLevel 折叠移除（menu 原档位 chain-enterprise 控制）；待办区页面级增强（真实字段派生不虚构） | P1-FIN-RVT-003（Batch RVT-3） | 手工回归（代码级 grep/diff/glob + dist 产物核验 + 保留项逐项命中 + 构建独立复跑） | PASS（2026-09-05） |

| REG-FIN-LRVT-001 | 财务-资金管理（FinanceFund.vue，布局级回退） | 资金管理布局级回退：模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→详情 el-dialog→末尾 B3 弹层）；待办区/工具栏/SearchPanel/#expand 零残留（仅注释）；重放修复点 B1~B6（E 值格式/失败透传/新建流水【#extra+既有端点 fundFlowApi.create】/来源+穿透【→FinanceLedger?voucherId】/筛选持久化 fts_search_/口径标注 PD-031·032）；能力层保留（density/show-summary unit='yuan'/固定列）；穿透成对不断链（Fund:390-393↔Ledger:547-552）；登记类入口 disabled+tooltip 挂 #extra（导出 EXPORT-001/日结 PD-028） | P1-FIN-LRVT-001（Batch LRVT-1，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-LRVT-002 | 财务-财务总账（FinanceLedger.vue，布局级回退） | 财务总账布局级回退：模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→VoucherFormDialog→详情 el-dialog）；待办区/工具栏/#expand 零残留（仅注释）；重放修复点 B1~B7（筛选 4 维端到端【voucherStatus 键名/voucherType 数字分支】/批量过账【batchPost+audited 过滤+提交数口径】/打印 window.print/穿透键+接收端/失败透传/持久化 fts_search_/KL-061 登记）；处置③分录 el-table 回迁（HEAD 原 5 列+借贷合计+displayRecords 转换保留）；能力层保留（density/selectable/show-summary unit='fen' 账本口径/固定列）；金额口径与 HEAD 零变更；穿透成对不断链（接收端 Ledger:547-552） | P1-FIN-LRVT-002（Batch LRVT-1，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-LRVT-003 | 财务-应收账款（FinanceReceivable.vue，布局级回退） | 应收账款布局级回退：模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→ReceiptDialog→ReceiptHistoryDialog）；待办区/工具栏/SearchPanel/#expand 零残留（仅注释）；重放修复点 B1~B8（到期日筛选端到端【due_date 真实过滤】/坏账核销【writeOff 既有端点+二次确认+行内 actions】/核销进度列【receivedAmount 后端实锤】/逾期高亮【能力层】/打印/失败透传/筛选持久化 fts_search_/导出批量登记）；能力层保留（density/show-summary unit='yuan'/固定列）；口径标注（CaliberNoteBar）；actions-width 220→280 属性级调整 | P1-FIN-LRVT-003（Batch LRVT-2，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-LRVT-004 | 财务-应付账款（FinancePayable.vue，布局级回退） | 应付账款布局级回退：模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→PaymentDialog→PaymentHistoryDialog）；待办区/工具栏/SearchPanel/#expand 零残留（仅注释）；重放修复点 B1~B8（创建日期筛选端到端【create_time 真实过滤】/**到期日登记处置保持【F-1 防回归：恒 null+零死参数+不回填+disabled+tooltip，不因回退变回假筛选】**/核销进度列【paidAmount 后端实锤】/逾期高亮/打印/失败透传/持久化/导出批量登记）；处置③ 详情 13 字段回迁；能力层保留（density/show-summary unit='yuan'/固定列）；口径标注（CaliberNoteBar）；actions-width 280 与 HEAD 一致 | P1-FIN-LRVT-004（Batch LRVT-2，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-LRVT-005 | 财务-税务管理（FinanceTax.vue，布局级回退） | 税务管理布局级回退：模板区块与 HEAD 同构（PageHeader#extra→stats 顶层→el-tabs 3 页签【Tab3 独立 pagination-wrapper 恢复】→4 详情对话框→配置对话框）；待办区/工具栏/SearchPanel/:pagination 零残留（仅注释）；重放修复点 B1~B7（**getRecords 端到端三 eq 实锤**【taxType/taxPeriod/taxStatus → TaxRecordServiceImpl:44-46/:49-51/:54-56】/payTax+PD-001 幂等零触碰【:79-90】/KL-059 税期动态化 YYYYMM 契约零变更【:57-86 + Service:49-51 消费实锤】/提交电子税务局失败透传/申报表加载失败提示/useSummary unit='yuan'/teleported+默认申报期）；附加（EmptyState 重试/打印/#extra 导出批量缴税登记）；能力层保留（density/show-summary unit='yuan'/固定列） | P1-FIN-LRVT-005（Batch LRVT-3，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-LRVT-007 | 财务-成本管理（FinanceCost.vue，布局级回退） | 成本管理布局级回退：模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel【monthrange 重做】→table-section→pagination-wrapper→详情 el-dialog 8 字段→CostFormDialog→B2 分析对话框末尾）；待办区/工具栏/#expand 零残留（仅注释）；重放修复点 B1~B6（**monthrange 端到端**【YYYY-MM→cost.ts mapQueryParams:41-42 startPeriod/endPeriod→CostServiceImpl:63-68 ge/le 实锤，真实筛选非本地过滤】/成本结构分析【summarizeByPeriod 既有端点+fenToYuanNumber 分→元一次转换，防二次 /100】/失败透传双态/CaliberNoteBar PD-032/合计行 unit='yuan'/导出导入登记）；处置③ 详情 8 字段回迁（与 HEAD 顺序一致）；能力层保留（density/show-summary unit='yuan'/actions-width 与 HEAD 一致）；api 层/后端零改动（mtime 实锤） | P1-FIN-LRVT-007（Batch LRVT-3，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-LRVT-006 | 财务-财务报表（FinanceReport.vue，布局级回退） | 财务报表布局级回退：模板区块与 HEAD 同构（PageHeader#extra→stats 固定 4 卡网格【repeat(4,1fr) 逐字一致】→advanced-search-panel【9 类 select+monthrange】→table-section→pagination-wrapper 独立分页恢复）；待办区/工具栏/统计卡片动态化/分页并入零残留（仅注释）；重放修复点 B1~B7（9 类端点聚合【后端恰 7 端点+2 P0 断流实锤】/KL-057 P0 断流空态不伪造【P1-FIN-BE-001 登记】/后端占位说明【账龄恒 0/预算待实现】/drillTo 19 处→4 独立路由【router/index.ts:117-120】/打印/失败透传+重试/**WS-006-R1 金额口径重放【cost_structure 四卡分→元一次转换，无二次 /100，F-1 防回归**】）；能力层保留（density/useSummary unit='yuan'/固定列/CaliberNoteBar）；导出登记（#extra disabled+tooltip） | P1-FIN-LRVT-006（Batch LRVT-4，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-LRVT-008 | 财务-自动凭证（AutoVoucher.vue，布局级回退） | 自动凭证布局级回退：模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel 原手写筛选区逐字同构→table-section→pagination-wrapper→详情 el-dialog→B1 新建/编辑规则对话框末尾）；待办区/工具栏/SearchPanel/分页并入零残留（仅注释）；重放修复点 B1~B6（CRUD 既有端点+getLeafSubjects+二次确认/B-3-B4 映射端到端【api 层零改动 mtime 9/3】/KL-061 伪语义列移除【VO 断链实证】/契约对齐【templateId 数字直传+toggleEnabled enabled 参数 Controller:66】/失败透传/打印/筛选持久化 fts_search_）；能力层保留（density/独立分页/统计卡片/DataTable 自动操作列）；导出登记 | P1-FIN-LRVT-008（Batch LRVT-4，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-LRVT-009 | 财务-发票报销（InvoiceReimbursement.vue，布局级回退） | 发票报销布局级回退：模板区块与 HEAD 同构（PageHeader#extra 新建报销回原位置→stats→advanced-search-panel【手写筛选区+value-format YYYY-MM-DD】→table-section→pagination-wrapper→详情/审批【KL-060 说明承载】/新建报销 el-dialog）；待办区/工具栏/SearchPanel 零残留（仅注释）；重放修复点 B1~B6（红冲/作废既有端点+issued 态行内入口+二次确认/E 值格式/失败透传/打印/筛选持久化 fts_search_/KL-060 登记不猜测【驳回=作废保持既有登记链】）；能力层保留（density/独立分页/统计卡片）；导出登记；金额口径全链一致（分→元/元→分无二次转换） | P1-FIN-LRVT-009（Batch LRVT-4，2026-09-05） | 手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤 + 构建独立复跑） | PASS（2026-09-05） |
| REG-FIN-PAY-001 | 财务-付款（逾期应付付款参数类型） | 逾期应付付款：前端 payableId/bankAccountId 类型 string→number，PaymentDialog 提交时 Number() 显式转换，后端 DTO Long 类型，Jackson 反序列化链路完整 | P1-FIN-PAY-001（FIN-UI-003 专项，2026-09-08） | 手工回归（API链路测试+前端组件测试） | PASS（2026-09-08） |
| REG-FIN-PAY-002 | 财务-付款（B模式回填银行记录） | B模式回填银行记录：BankPaymentRecordController 新建（fillBankInfo + confirm），payment.ts 补充 confirmBankRecord/fillBankRecord 方法，PaymentHistoryDialog 确认按钮调用链完整 | P1-FIN-PAY-002（FIN-UI-003 专项，2026-09-08） | 手工回归（API链路测试+前端组件测试） | PASS_WITH_LIMITATION（2026-09-08） |
| REG-FIN-PAY-003 | 财务-付款（银行账户列表加载） | 银行账户列表加载：N+1 查询优化为批量查询 getReconciliationResultsBatch()，错误处理透出具体 HTTP 状态码信息 | P2-FIN-PAY-003（FIN-UI-003 专项，2026-09-08） | 手工回归（API链路测试+前端组件测试） | PASS_WITH_LIMITATION（2026-09-08） |
| REG-FIN-PAY-004 | 财务-付款（付款账户显示异常） | 付款账户下拉：accountLabel() 防御性处理（accountName||'未知账户'，accountNumberMasked||'****'），消除"?????"乱码 | P2-FIN-PAY-004（FIN-UI-003 专项，2026-09-08） | 手工回归（API链路测试+前端组件测试） | PASS_WITH_LIMITATION（2026-09-08） |
| REG-ORDER-001 | 订单-下单（batch） | `POST /v1/orders` 批量下单 order_number=order_code 非空且全表唯一 | P1-ORDER-NUMBER-002（A1，2026-09-23） | API回归+数据一致性检查 | PASS |
| REG-ORDER-002 | 订单-POS点单 | POS `/v1/pos/orders/order` T 码双写保持（order_code=order_number=T*） | P1-ORDER-NUMBER-002（A1，2026-09-23） | API回归 | PASS |
| REG-ORDER-003 | 订单-POS快速单 | `/v1/pos/orders/create` 独立执行 order_number 非空 | P1-ORDER-NUMBER-002（A1，2026-09-23） | API回归 | PASS |
| REG-ORDER-004 | 订单-DB唯一约束 | 重复 order_number 被 `orders_order_number_key` 拒绝 | P1-ORDER-NUMBER-002（A1，2026-09-23） | 数据一致性检查 | PASS |
| REG-ORDER-005 | 订单-KDS可见性 | POS 创建后 KDS list 可见（orderNumber 回传） | P1-ORDER-NUMBER-002（A1，2026-09-23） | API回归 | PASS |
| REG-ORDER-006 | 订单-托盘出餐链 | bind → scan-kitchen-in → scan-kitchen-out → scan-serve → material_consumed=1 + 库存扣减 | P1-ORDER-NUMBER-002（A1，2026-09-23） | 手工回归（端到端+DB） | PASS_WITH_LIMITATION（2026-09-23，L-03） |
| REG-ORDER-007 | 订单-A1边界 | generateOrderCode/Schema/Flyway/POS 语义/P0 扣料零改动；单测 3/3；git 三提交链一致 | P1-ORDER-NUMBER-002（A1，2026-09-23） | 手工回归（代码级 diff + 单测复跑） | PASS |
| REG-ORDER-008 | 订单-套餐下单 | 套餐下单 `order_items` 落 `product_type=2` + `combo_id` 非空 + `food_id` NULL（合法套餐 null）；非法 null（`product_type=1` 且 `food_id` NULL）= 0 | P1-COMBO-ORDER-001（2026-09-24） | 数据一致性检查 | PASS |
| REG-ORDER-009 | 订单-KDS套餐展示 | KDS `GET /v1/kitchen/orders/recent/full` 套餐行 `productType=2` + `comboId` + `components[]` 逐字段展开；单一 dish 对象单卡片不拆；单品行不误展开 | P1-COMBO-ORDER-001（2026-09-24） | API回归 | PASS |
| REG-ORDER-010 | 订单-套餐出餐扣料 | 套餐出餐 `material_consumed=1` + `store_inventory_log` 扣减链（before→after 连续、remark 含订单 T 码）；`material_consumption` 无新增 FAILED | P1-COMBO-ORDER-001（2026-09-24） | 手工回归（端到端+DB） | PASS |
| REG-ORDER-011 | 订单-单品零回归 | 单品 foodCode 路径零回归：`product_type=1` / `food_id=1` / `combo_id` 空 + 出餐 `material_consumed=1` + 库存 log；四套件单测 37/37（托盘 E2E 链断言引用 REG-ORDER-006，A1 边界引用 REG-ORDER-007） | P1-COMBO-ORDER-001（2026-09-24） | 手工回归（DB+单测复跑） | PASS |

| Batch FIN-PAY-1（财务付款链路专项 FIN-UI-003，2026-09-08） | **PASS**（发布批次） | QA 独立验收 1/1 PASS + 3 PWL / FAIL 0，REG-FIN-PAY-001/002/003/004 四条新增基线（正式 110 → 114 条），PWL 3 项逐条确认不阻断发布，既有基线 REG-FIN-001~006 / REG-FIN-WS-001~010 / REG-UI-FIN-001~004 无受影响 | 2026-09-08 |
| Batch ORDER-A1（订单号唯一性专项 P1-ORDER-NUMBER-002/A1，2026-09-23） | **PASS**（发布批次） | QA 独立验收 PASS_WITH_LIMITATION（H-01~H-06 6/6 PASS、CC-6 33/33、单测 3/3、FAIL 0）+ 回归独立抽验复跑 PASS；REG-ORDER-001~007 七条新增基线（正式 114 → 121 条），PWL 5 项（L-01~L-05）逐条确认不阻断本地放行（L-01 生产不可达→本地放行/生产 PROVISIONAL）；既有基线零受影响 | 2026-09-23 |
| Batch P1-COMBO-ORDER-001（套餐下单/KDS 组合套餐专项 P1-COMBO-ORDER-001，2026-09-24） | **PASS**（发布批次） | QA 独立验收 PASS_WITH_LIMITATION（验收 6/6 PASS、抽检 DB 5/5 + KDS live + 代码抽检 + 单测 37/37、FAIL 0、无 -R{n}）+ 回归按 QA 已验收结论转基线；REG-ORDER-008~011 四条新增基线（正式 121 → 125 条），PWL 限制逐条确认不阻断（L-01 UI 目检→回归阶段补浏览器断言；L-02 生产证据 PROVISIONAL→仅阻断生产放行；L-06 legacy 三文件仍读旧表→报告 §7.1 声明的第二步卡范围）；既有基线零受影响 | 2026-09-24 |

---

## REG-DATA-001

- **测试编号**：REG-DATA-001
- **模块**：门店-物资需求（StoreMaterialRequest）
- **关联任务**：P0-DATA-001（Sprint-1 数据污染专项，独立验收 5/5 PASS）
- **业务场景**：编辑已有物资需求
- **测试目的**：防止「编辑变新增」回归 —— 编辑已有物资需求时，后端必须走 update 分流，不得创建新记录。
- **测试步骤**：
  1. 创建一条门店物资需求
  2. 记录当前列表总条数
  3. 打开该条需求的编辑页面
  4. 修改数量
  5. 保存
  6. 刷新列表
  7. 检查 API 调用记录（应为 update，而非 create）
- **预期结果**：
  - 数据条数：刷新后列表总条数不变（仍为原条数，不新增）
  - API 行为：编辑保存调用 update 接口，不触发 create
  - 字段保持：修改的数量已更新，其余字段（含原始标识）保持不变
  - 权限正常：仅具备相应权限的账号可编辑保存，无权限操作被拒绝
- **测试类型**：数据一致性检查
- **当前状态**：PASS

---

## REG-DATA-002

- **测试编号**：REG-DATA-002
- **模块**：营销-会员（MemberList）
- **关联任务**：P0-DATA-002（Sprint-1 数据污染专项，独立验收 5/5 PASS）
- **业务场景**：编辑已有会员
- **测试目的**：防止「会员编辑新增」与「tags 丢失」回归 —— 编辑已有会员不得创建新会员，且编辑后 tags 必须完整保留。
- **测试步骤**：
  1. 创建一条会员记录，并为其设置 tags
  2. 记录当前列表总条数
  3. 打开该会员的编辑页面
  4. 修改基本信息（如手机号/姓名）
  5. 保存
  6. 刷新列表
  7. 打开该会员详情，检查 tags
  8. 检查 API 调用记录（应为 update，而非 create）
- **预期结果**：
  - 数据条数：刷新后列表总条数不变（不新增会员）
  - API 行为：编辑保存调用 update 接口，不触发 create
  - 字段保持：tags 在编辑保存后完整保留、不丢失；其余字段按编辑内容更新
  - 权限正常：仅具备相应权限的账号可编辑保存，无权限操作被拒绝
- **测试类型**：数据一致性检查
- **当前状态**：PASS

---

## REG-DATA-003

- **测试编号**：REG-DATA-003
- **模块**：资产-处置（AssetDisposal）
- **关联任务**：P0-DATA-003（Sprint-1 数据污染专项，独立验收 5/5 PASS）
- **业务场景**：编辑已有资产处置单
- **测试目的**：防止「处置编辑重复生成」与「审批流程重复」回归 —— 编辑已有处置单不得生成重复处置数据，审批流程不得被重复触发。
- **测试步骤**：
  1. 创建一条资产处置单，并进入审批流程
  2. 记录当前处置列表总条数
  3. 打开该处置单的编辑页面
  4. 修改处置信息
  5. 保存
  6. 刷新列表
  7. 检查审批流程记录（审批任务/审批流实例条数）
  8. 检查 API 调用记录（应为 update，而非 create）
- **预期结果**：
  - 数据条数：刷新后处置列表总条数不变（不重复生成处置单）
  - API 行为：编辑保存调用 update 接口，不触发 create
  - 字段保持：修改的处置信息已更新，其余字段保持不变
  - 审批流程：审批流程不重复触发（不产生重复审批任务/重复审批流实例）
  - 权限正常：仅具备相应权限的账号可编辑保存，无权限操作被拒绝
- **测试类型**：数据一致性检查
- **当前状态**：PASS

---

## REG-DATA-004

- **测试编号**：REG-DATA-004
- **模块**：追溯-质检（TraceabilityQuality）
- **关联任务**：P0-DATA-004（Sprint-1 数据污染专项，独立验收 5/5 PASS）
- **业务场景**：编辑已有质检记录
- **测试目的**：防止质检编辑以下问题回归 —— 不重复创建质检记录、检验项保留、abnormalLevel 不被覆盖。
- **测试步骤**：
  1. 创建一条质检记录，包含检验项并设置 abnormalLevel
  2. 记录当前质检列表总条数
  3. 打开该质检记录的编辑页面
  4. 修改部分质检信息
  5. 保存
  6. 刷新列表
  7. 打开详情，检查检验项与 abnormalLevel
  8. 检查 API 调用记录（应为 update，而非 create）
- **预期结果**：
  - 数据条数：刷新后质检列表总条数不变（不重复创建质检记录）
  - API 行为：编辑保存调用 update 接口，不触发 create
  - 字段保持：检验项完整保留；abnormalLevel 不被覆盖（保持编辑前值）；修改的字段按编辑内容更新
  - 权限正常：仅具备相应权限的账号可编辑保存，无权限操作被拒绝
- **测试类型**：数据一致性检查
- **当前状态**：PASS

---

## REG-DATA-005

- **测试编号**：REG-DATA-005
- **模块**：采购-物资需求（MaterialRequest）
- **关联任务**：P0-DATA-005（Sprint-1 数据污染专项，独立验收 5/5 PASS）
- **业务场景**：采购需求编辑 / 新建 / 提交审批
- **测试目的**：防止采购需求数据污染问题回归 —— 编辑走 update 正确更新、新建正常创建、提交审批流程正常。
- **测试步骤**：
  1. 新建一条采购物资需求，提交审批，确认流程正常
  2. 记录当前采购需求列表总条数
  3. 打开已有采购需求的编辑页面
  4. 修改需求内容
  5. 保存
  6. 刷新列表
  7. 再次新建一条采购物资需求，确认新建正常
  8. 对新建需求提交审批，确认审批正常
  9. 检查 API 调用记录（编辑应为 update，新建应为 create）
- **预期结果**：
  - 数据条数：编辑后列表总条数不变；新建后列表总条数 +1
  - API 行为：编辑保存调用 update 接口；新建调用 create 接口，两条路径正确分流
  - 字段保持：编辑后修改字段已更新，其余字段保持不变
  - 审批流程：编辑不触发重复审批；新建需求提交审批正常进入流程
  - 权限正常：仅具备相应权限的账号可编辑/新建/提交审批，无权限操作被拒绝
- **测试类型**：数据一致性检查
- **当前状态**：PASS

---

## Sprint-2 回归（财务真实性 + HR 业务假成功治理）

> 验收依据：`sprint-2-final-qa-report.md`（2026-08-10，15/15 通过，FAIL 0）。
> PASS_WITH_LIMITATION：验收通过但存在系统限制；限制项见各案例「限制说明」，非失败项。

### REG-FIN-001

- **测试编号**：REG-FIN-001
- **模块**：财务-总账（AccountBalance）
- **关联任务**：P0-FIN-001（Sprint-2 财务真实性）
- **业务场景**：查看科目余额 / 刷新余额 / 导出余额
- **测试目的**：防止硬编码假账回归 —— 余额必须来自真实聚合，不得再出现手工 BigDecimal 假账。
- **测试步骤**：
  1. 打开科目余额列表，记录各科目余额与明细
  2. 新增一笔凭证（借方/贷方）
  3. 点击「刷新」按钮
  4. 重新查询余额与明细
  5. 导出余额 CSV
- **预期结果**：
  - 数据条数：余额明细按 科目|期间 聚合凭证流水逐行计算，无重复行
  - API 行为：GET 列表查询 account_balance 表；/refresh 触发真实全量重算（期初 + 借方 − 贷方 = 余额），非空操作；/export 输出真实 CSV
  - 字段保持：新增凭证后余额随借/贷变化，刷新与明细同源（聚合口径一致）
  - 权限正常：GET 需 finance:view；无权限访问被拒绝
- **测试类型**：数据一致性检查
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**（2026-08-10 更新：O-2 修复，依据 `docs/quality/sprint-3c-qa-report.md` P2-CLEAN-006 PASS；2026-08-11 更新：PD-003 决策落地，限制收窄）：
  - 新科目期间期初默认 0，上期期末结转规则——**已由 PD-003 决策落地（2026-08-11，决策=当前版本不实现自动期初结转，期初由授权人员人工录入 + 审计（人/时间/原因），禁止系统自动推导历史余额）**；本项限制收窄为「口径维持」：新行期初保持 0、不自动结转、既有行期初零覆盖（QA 活体验证 PASS，见 REG-FIN-006 / REG-RULE-003）。
  - closeProfitAndLoss / closeYearProfit 原为 return true 假实现死代码（无 controller 暴露）——**已由 P2-CLEAN-006 删除（2026-08-10，QA PASS）**，本项限制关闭。

---

### REG-FIN-002

- **测试编号**：REG-FIN-002
- **模块**：财务-缴税（FinanceTax）
- **关联任务**：P0-FIN-002（Sprint-2 财务真实性）
- **业务场景**：发起缴税
- **测试目的**：防止缴税假成功回归 —— 缴税必须真实落库，不得零 API 调用弹成功。
- **测试步骤**：
  1. 打开缴税页面，填写缴税信息
  2. 点击「缴税」
  3. 观察提示
  4. 刷新缴税列表
  5. 连续快速点击「缴税」
- **预期结果**：
  - 数据条数：刷新后列表出现该缴税记录且状态正确，DB 可查
  - API 行为：真实调用 POST /v1/finance/tax-records，成功后才提示并刷新列表
  - 字段保持：缴税记录字段与填写内容一致
  - 权限正常：接口含 finance:tax:create 鉴权，无权限操作被拒绝
  - 防重复：连点不产生重复记录（前端单飞守卫）
- **测试类型**：手工回归
- **当前状态**：PASS
- **限制说明**：无（2026-08-10 更新：原「前端 tax-calculation.ts:102 过时注释待修正」备注已由 P2-CLEAN-005 修正闭环，依据 `docs/quality/sprint-3c-qa-report.md` P2-CLEAN-005 PASS，非功能问题项关闭）。

---

### REG-FIN-003

- **测试编号**：REG-FIN-003
- **模块**：财务-缴税（FinanceTax）
- **关联任务**：P0-FIN-003（Sprint-2 财务真实性）
- **业务场景**：后端失败时的错误提示
- **测试目的**：防止错误被降级为「需配置授权」回归 —— 真实故障必须透传真实错误。
- **测试步骤**：
  1. 打开缴税页面
  2. 使后端返回 500 / 参数校验失败
  3. 触发计算 / 缴税操作
  4. 观察提示文案
  5. 使后端返回真实授权缺失错误，再次操作
- **预期结果**：
  - API 行为：错误信息透传真实错误码与 message，无成功文案复用
  - 提示：500/校验失败显示具体错误；仅当 message 含「授权/对接/authorize」时才显示配置引导
  - 数据一致性：失败无落库、无假状态
- **测试类型**：手工回归
- **当前状态**：PASS

---

### REG-FIN-004

- **测试编号**：REG-FIN-004
- **模块**：财务+营销（资金域 12 接口）
- **关联任务**：P0-FIN-004（Sprint-2 财务真实性）
- **业务场景**：资金域接口越权访问控制
- **测试目的**：防止资金域裸接口越权回归 —— 12 个接口必须保持方法级鉴权。
- **测试步骤**：
  1. 以员工/组长角色调用资金域 12 个接口（fund-flows / profits / standard-cost-cards / subjects / summary-templates / tax-rate-configs / transfer-templates / recharge-finance / recharge-plans / recharge-settings / recharge-stats / refunds）
  2. 以授权角色（OWNER/ADMIN/FINANCE_DIRECTOR 等）调用同一批接口
  3. 以无 manage 权限角色触发退款审批/执行、充值设置、转账模板、充值计划写操作
- **预期结果**：
  - 权限正常：员工/组长调用 12 接口全部 403；授权角色 200；写操作限 manage 权限 + OWNER/ADMIN/FINANCE_DIRECTOR
  - API 行为：全部接口方法级 @PreAuthorize 覆盖，权限点已在 AdminPermissions.java 注册
- **测试类型**：API回归
- **当前状态**：PASS

---

### REG-HR-001

- **测试编号**：REG-HR-001
- **模块**：HR-薪资（HRSalary）
- **关联任务**：P0-HR-001（Sprint-2 HR 业务真实性）
- **业务场景**：薪资核算与工资条导出
- **测试目的**：防止薪资核算/导出假成功回归 —— 核算必须真实执行并落库。
- **测试步骤**：
  1. 打开薪资页面
  2. 发起薪资核算
  3. 等待完成提示
  4. 查询工资表核对金额
  5. 导出工资条
  6. 点击未就绪功能（发薪/驳回/财务同步）
- **预期结果**：
  - API 行为：核算真实调用 POST /v1/salary/generate，后端真实计算并逐条落库
  - 数据条数：核算后工资表可查询，金额与薪资项一致
  - 字段保持：导出文件基于真实列表数据生成，含真实数据
  - 未就绪功能：禁用 + 明确「接口未就绪」提示，无假成功
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：导出为前端基于真实数据生成 CSV，无后端导出端点（建议后端补导出端点或归档说明）。

---

### REG-HR-002

- **测试编号**：REG-HR-002
- **模块**：HR-培训（HRTraining）
- **关联任务**：P0-HR-002（Sprint-2 HR 业务真实性）
- **业务场景**：培训导入 / 导出 / 报名
- **测试目的**：防止培训导入/导出/报名假成功回归 —— 数据必须真实落库。
- **测试步骤**：
  1. 打开培训页面
  2. 导入真实课程文件
  3. 刷新列表
  4. 导出培训列表
  5. 为员工报名课程
  6. 刷新并查看学习记录
- **预期结果**：
  - API 行为：导入真实解析落库（importCourses → createCourse → save）
  - 数据条数：导入后列表可见；报名后 DB 有记录，刷新保留
  - 字段保持：导出文件可打开且为真实数据
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**（2026-08-11 更新：PD-002 决策落地，限制关闭）：
  - 重复报名规则——**已由 PD-002 决策落地（2026-08-11，决策=唯一性键 员工+课程+期次，重复报名拒绝返回「已报名」、不生成第二条记录、保留原报名状态）**；实现键=员工+课程（training_study_record 无期次字段），QA 活体验证 PASS（同键拒绝+已报名提示+DB 无第二条+原记录零改动，见 REG-HR-012 / REG-RULE-002），本项限制关闭。剩余限制仅为 REG-HR-012 注明的并发竞态（无 DB 唯一索引，低概率）与期次键差异（已透明记录），不阻断发布。

---

### REG-HR-003

- **测试编号**：REG-HR-003
- **模块**：HR-合同模板（ContractTemplateLibrary）
- **关联任务**：P0-HR-003（Sprint-2 HR 业务真实性）
- **业务场景**：模板保存与删除
- **测试目的**：防止模板假保存回归 —— 保存/删除必须真实持久化，刷新不得还原。
- **测试步骤**：
  1. 打开合同模板库
  2. 新建/保存一个模板
  3. 刷新页面，确认模板仍在
  4. 删除该模板
  5. 刷新页面，确认已删除
  6. 构造后端失败，再次保存
- **预期结果**：
  - 数据条数：保存后刷新仍在，DB 有记录；删除后刷新已删除，DB 无记录
  - API 行为：写操作全部调真实接口（POST/PUT/DELETE/duplicate/status/statistics），mock 数组不再作为数据源
  - 后端失败：显示真实错误，无成功提示
- **测试类型**：手工回归
- **当前状态**：PASS

---

### REG-HR-004

- **测试编号**：REG-HR-004
- **模块**：HR-员工画像（EmployeeIntelligence）
- **关联任务**：P0-HR-004（Sprint-2 HR 业务真实性）
- **业务场景**：员工风险状态操作
- **测试目的**：防止风险状态假成功回归 —— 无后端能力时禁止假成功。
- **测试步骤**：
  1. 打开员工智能页面
  2. 尝试「开始处理 / 标记已解决 / 忽略」风险
  3. 观察操作入口状态与提示
- **预期结果**：
  - API 行为：无后端能力时操作入口全部禁用，明确提示「风险状态接口未就绪，暂不支持修改」
  - 提示：错误以 error 提示，无 catch 假成功
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**（2026-08-11 更新：PD-004 决策落地，限制关闭）：
  - 风险展示数据源为 mock API（api/employee-intelligence.ts:255-380）——**已由 PD-004 决策落地（2026-08-11：员工画像页随 HR 智能 3 页面整体下线，待真实数据源接入后重新评估）**：菜单/路由入口移除、URL 直达由守卫拦截不渲染、dist 无产物（QA 8/8 验收 PASS，见 REG-HR-013）——mock 数据源不再有 UI 触发点、演示数据不可达，本项限制关闭。页面文件保留为死代码（PD-004「保留待评估」口径）。

---

### REG-HR-005

- **测试编号**：REG-HR-005
- **模块**：HR-Mock 治理（attendance / recruitment / 培训 / 合同模板）
- **关联任务**：P0-HR-006（Sprint-2 HR 业务真实性）
- **业务场景**：mock 死代码清理检查
- **测试目的**：防止失效 mock 端点被调用/误接回归。
- **测试步骤**：
  1. 全仓检查训练/合同模板 mock 常量引用
  2. 检查考勤月度汇总、员工风险画像、到期合同列表的数据来源
  3. 搜索 scheduleApi / recruitment-mock 残留
- **预期结果**：
  - API 行为：训练/合同模板 mock 常量无引用（死代码清理完成）；无失效 mock 端点被调用
  - 残留演示 mock 数据源已明确标注或替换
  - 编译通过、无引用报错
- **测试类型**：数据一致性检查
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：3 处演示 mock 数据源仍被调用（考勤月度汇总 attendance.ts:214-236、员工风险画像、到期合同列表），逐一标注「演示数据」或替换真实接口。

---

### REG-HR-006

- **测试编号**：REG-HR-006
- **模块**：HR-合同（HRContract）
- **关联任务**：P0-HR-007（Sprint-2 HR 业务真实性）
- **业务场景**：合同到期提醒设置
- **测试目的**：防止提醒设置空操作回归 —— 设置必须真实持久化。
- **测试步骤**：
  1. 打开合同页面
  2. 设置到期提醒
  3. 保存
  4. 刷新页面，确认设置仍在
- **预期结果**：
  - API 行为：保存真实调用 PUT /v1/sys-config/value/{key}，加载真实回填（GET），非空操作
  - 数据条数：DB 有对应配置记录，刷新后设置保留
  - 后端失败：无成功提示
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：ContractDashboard「到期合同」列表仍用 mock API（contract-intelligence.ts 纯 mock），建议接真实合同数据或标注演示。

---

### REG-HR-007

- **测试编号**：REG-HR-007
- **模块**：HR-合同详情（ContractDetailDialog）
- **关联任务**：P0-HR-008（Sprint-2 HR 业务真实性）
- **业务场景**：抽屉内提交审批
- **测试目的**：防止审批「静默成功」回归 —— 未接入期间不得出现任何成功提示。
- **测试步骤**：
  1. 打开合同详情抽屉
  2. 点击「提交审批」
  3. 观察行为与提示
- **预期结果**：
  - API 行为：无「审批通过即成功」路径；功能未接入时点击无动作、无成功提示
  - 数据一致性：后端 /v1/hr/approvals 为占位空数据，无假审批记录生成
- **测试类型**：手工回归
- **当前状态**：PASS

---

### REG-HR-008

- **测试编号**：REG-HR-008
- **模块**：HR-表单/列表（HREmployee / HRContract / HRKnowledgeBase）
- **关联任务**：P0-HR-009（Sprint-2 HR 业务真实性）
- **业务场景**：表单保存与列表加载失败
- **测试目的**：防止提前成功/静默失败回归 —— 保存必须等待真实 API 结果，失败不得弹成功。
- **测试步骤**：
  1. 打开员工/合同表单，编辑后保存（构造后端失败）
  2. 观察弹窗行为与提示
  3. 打开续签/变更记录，构造加载失败
  4. 检查 HRKnowledgeBase 批量导入入口
- **预期结果**：
  - 提示：成功提示均在 API resolve 后；失败不关窗、显示错误、无成功提示
  - API 行为：续签/变更记录加载失败有错误提示而非空列表
  - 入口状态：HRKnowledgeBase「开发中」入口不可点击
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：ContractCreateDialog 正文生成（generateDocument 桩）失败时，仍弹「合同起草完成，已生成正式正文」成功文案（L366-378），文案与事实矛盾，建议按正文生成结果条件化。

---

### REG-HR-009

- **测试编号**：REG-HR-009
- **模块**：员工门户（SelfServicePortal）
- **关联任务**：P0-HR-010（Sprint-2 HR 业务真实性）
- **业务场景**：工资条 / 排班 / 请假
- **测试目的**：防止整页假数据回归 —— 员工不得看到假工资条/假排班。
- **测试步骤**：
  1. 打开员工门户
  2. 查看工资条，与薪资模块数据比对
  3. 查看排班区块
  4. 提交请假
- **预期结果**：
  - 数据一致性：工资条走真实 API，与薪资模块数据一致
  - 排班区块显式禁用（不展示假数据）
  - 请假提交落库，DB 有记录
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：员工档案电话（138****8888）、入司日期（2024-03-15）硬编码 mock（SelfServicePortal.vue:50-51），建议接入员工档案接口。

---

### REG-HR-010

- **测试编号**：REG-HR-010
- **模块**：HR-API 断流（contract.ts / salary.ts 桩方法）
- **关联任务**：P0-HR-011（Sprint-2 HR 业务真实性）
- **业务场景**：桩接口调用行为
- **测试目的**：防止 API 断流白屏/假成功回归 —— 桩方法必须禁用+提示或真实报错。
- **测试步骤**：
  1. 触发 salary 桩方法（无页面调用的 5 个）
  2. 触发 contract 桩方法（有页面调用的部分）
  3. 全仓搜索 catch 中的成功文案
- **预期结果**：
  - 无页面调用的桩：禁用 + warning 提示
  - 被调用的桩：try/catch 内显示 error/warning，无 throw 白屏
  - 全库无「catch 中 ElMessage.success」假成功路径
- **测试类型**：手工回归
- **当前状态**：PASS

---

### REG-HR-011

- **测试编号**：REG-HR-011
- **模块**：门店-排班（ShiftManagement）
- **关联任务**：P0-HR-012（Sprint-2 HR 业务真实性）
- **业务场景**：班次创建/编辑/删除/交接
- **测试目的**：防止排班整页 mock 假成功回归 —— 未接真实接口期间不得假成功。
- **测试步骤**：
  1. 打开排班页面
  2. 尝试创建/编辑/删除/交接班次
  3. 观察按钮状态与提示
- **预期结果**：
  - 数据条数：页面无写死排班数据（mockShiftData 为空数组）
  - 写操作全部禁用 + 明确「接口未就绪」警告，无假成功
  - 数据一致性：无本地内存变更伪装成功
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：列表基于空本地数组过滤，未接已存在的 schedule 接口（@/api/schedule），页面为占位空壳。

---

### REG-RULE-001

- **测试编号**：REG-RULE-001
- **模块**：财务-缴税（BLOCKED_PRODUCT_RULE 专项 → PD-001 决策落地）
- **关联任务**：BLOCKED_PRODUCT_RULE-1（缴税重复缴纳规则 → P0-FIN-002 按 PD-001 实现）
- **业务场景**：重复缴税请求处理
- **测试目的**：防止幂等拦截错误回归 —— 同键（税种+所属期+凭证号）重复缴税必须命中幂等：不新增记录、返回已有记录（id 相同）、记录幂等命中日志、原记录零改动；不同键正常新增；DB 唯一索引兜底。
- **测试步骤**：
  1. 后端层重复提交同一缴税请求（同 taxType/taxPeriod/voucherNo）
  2. 比较两次返回 id（应相同）与 DB 计数（应=1）
  3. 查询 sys_operation_logs 幂等命中日志（操作人/时间/幂等键/hitRecordId）
  4. 携带不同字段值重提同键，检查原记录字段（应零改动）
  5. DB 层直插同键，检查唯一索引兜底（uk_tax_record_idempotent）
  6. 不同键提交，确认正常新增
- **预期结果**：
  - 幂等拦截正确：同键第 2 次提交 → 不新增（DB 仅 1 条）+ 返回已有记录（id 相同）+ 命中日志含 hitRecordId + 原记录字段零改动（create/update_time 不变）
  - 唯一索引兜底：uk_tax_record_idempotent(tax_type, tax_period, voucher_no) 存在，直插同键违反唯一约束
  - 不同键正常新增（id 递增）
- **测试类型**：数据一致性检查
- **当前状态**：PASS（2026-08-11 更新：原 PASS_WITH_LIMITATION「决策待裁决」限制关闭——PD-001 已决策且实现经 QA 活体验收 PASS，防回归点由「不擅自实现」转为「幂等拦截正确」，详见 REG-FIN-005）
- **限制说明**：
  - 重复缴纳规则待产品裁决（PD-001）——**已到达（2026-08-11）**：PD-001 决策=幂等键 税种+所属期+凭证号、重复提交不新增/返回已有/记命中日志；实现经 QA 活体验收 PASS（`docs/quality/sprint-4-backend-qa-report.md`），本项限制关闭。
  - 边界语义（PD-001 注记保留）：voucher_no 为空 → 幂等键不完整、不适用幂等（PG 允许多 NULL，生成两条），属决策注记既有语义，非缺陷。
  - 观察项：幂等命中时 @OperationLog 切面仍记一条「创建缴税记录」status=SUCCESS（与「未新增」语义略有出入，由命中日志行补充说明）——不影响审计真实性，登记观察。

---

### REG-RULE-002

- **测试编号**：REG-RULE-002
- **模块**：HR-培训（BLOCKED_PRODUCT_RULE 专项 → PD-002 决策落地）
- **关联任务**：BLOCKED_PRODUCT_RULE-2（培训重复报名规则 → P0-HR-002 报名子项按 PD-002 实现）
- **业务场景**：同一员工重复报名同一课程
- **测试目的**：防止报名唯一性限制回退/错误实现回归 —— 同键（员工+课程）重复报名必须拒绝：返回「已报名」提示、不生成第二条记录、原报名状态保留；不同课程/员工正常报名；DB 无重复键行。
- **测试步骤**：
  1. 同一员工对同一课程重复报名
  2. 检查返回（第 1 次 {created:1, duplicated:0}；第 2 次 {created:0, duplicated:1} + 「已报名」消息）
  3. DB 检查 (employee_id, course_id) 组合是否出现第二条（GROUP BY HAVING COUNT(*)>1 = 0 行）
  4. 检查原报名记录字段零变化（create/update_time 不变）
  5. 不同课程/员工报名 → created:1；混合批次计数正确
  6. 软删记录（deleted=1）后再报名 → created:1（@TableLogic 生效）
- **预期结果**：
  - 同键拒绝：{created:0, duplicated:1} + 提示「报名成功0人，1人已报名（重复报名已拒绝，未创建新记录）」；DB 该键仅 1 条；原记录零改动
  - 异键正常：不同课程/员工 created:1；混合批次 created/duplicated 计数正确
  - 软删可重报：查重只统计未删记录
- **测试类型**：数据一致性检查
- **当前状态**：PASS_WITH_LIMITATION（2026-08-11 更新：防回归点由「无唯一性限制」转为「同键拒绝已报名」；限制=并发竞态 + 期次键差异，见下）
- **限制说明**：
  - 若产品裁决「仅报名一次」由产品正式定义后实现（PD-002）——**已到达（2026-08-11）**：PD-002 决策=唯一性键 员工+课程+期次、重复报名拒绝返回「已报名」；实现经 QA 活体验收 PASS（`docs/quality/sprint-4-backend-qa-report.md`），本项限制关闭。
  - **限制 1（并发竞态）**：training_study_record 无 (employee_id, course_id) DB 唯一索引（pg_indexes 确认），服务层 selectCount 查重存在并发双提交竞态窗口（低概率）——属限制注明，不阻断发布；建议后续批次补部分唯一索引或并发补偿（QA 验证缺口 1，登记观察）。
  - **限制 2（期次键差异）**：决策键=员工+课程+期次，实现键=员工+课程（schema 无期次字段，19 列确认）——差异已在代码注释/javadoc 透明记录（TrainingServiceImpl.java:395-399、TrainingController.java:248-251），未自行编造期次逻辑；若未来课程分期次报名需加字段+迁移+改键（登记观察项）。

---

### REG-RULE-003

- **测试编号**：REG-RULE-003
- **模块**：财务-总账（BLOCKED_PRODUCT_RULE 专项 → PD-003 决策落地）
- **关联任务**：BLOCKED_PRODUCT_RULE-3（科目期初余额结转规则 → P0-FIN-001 按 PD-003 实现）
- **业务场景**：新科目期间余额重算
- **测试目的**：防止自动结转回归 —— 期初不得自动结转：新科目期间期初默认 0、不自动结转上期期末；既有行期初不被覆盖；期初调整必须经人工录入 + 审计（操作人/时间/原因）。
- **测试步骤**：
  1. 新增科目期间并触发余额重算（POST /refresh）
  2. 检查新期间期初值（应为 0.00）
  3. 检查既有行期初值（refresh 后不被覆盖、update_time 不变）
  4. 授权调用 PUT /opening 录入/调整期初，检查生效与联动重算（beginDebit 联动 endDebit）
  5. 缺 adjustReason 提交 → 400（原因必填）；负数 → 400
  6. 查 sys_operation_logs 审计（操作人/时间/原因）
  7. 非授权角色调用 /opening → 403
- **预期结果**：
  - 不自动结转：refresh 新建行 begin_debit=begin_credit=0.00；既有行期初零覆盖
  - 人工录入可审计：PUT /opening 生效 + 期末联动重算 + 审计四要素（人/时间/原因/内容）落库；原因必填（缺省 400）
  - 权限：/opening 需 finance:edit 或 `*`，非授权 403
- **测试类型**：数据一致性检查
- **当前状态**：PASS_WITH_LIMITATION（2026-08-11 更新：防回归点由「不擅自实现」转为「保持不自动结转 + 期初人工录入可审计」；口径维持，详见 REG-FIN-006）
- **限制说明**：
  - 结转规则待产品裁决后补齐（与 REG-FIN-001 同一闭环点）——**已到达（2026-08-11）**：PD-003 决策=当前版本不实现自动期初结转、期初金额由授权人员人工录入、调整必须记录操作人/时间/原因、禁止系统自动推导历史余额、无审计记录不得修改期初数据；实现经 QA 活体验收 PASS（`docs/quality/sprint-4-backend-qa-report.md`），本项限制关闭。
  - **口径维持（决策后保留项）**：不自动结转为产品决策口径（非缺失），新行期初 0 保持；QA 观察项 O-2（recalculateAllBalances 仅 upsert「有凭证流水的科目期间」，流水全删后 refresh 不清零 current）与 O-3（明细按 create_time 排序）为已知观察、非本规则限制，不阻断发布。

---

## Sprint-3A 第一阶段回归（权限组）

> 验收依据：`docs/quality/sprint-3a-security-qa-report.md`（2026-08-10 复审，独立验收 8/8：PASS 5 / PASS_WITH_LIMITATION 3 / FAIL 0，基线 git 提交 dd33ee3；较首轮 PASS 6 / PASS_WITH_LIMITATION 2 的状态变化：P0-SEC-006 由 PASS 收紧为 PASS_WITH_LIMITATION，详见 REG-SEC-004；P1-SEC-003 新增限制 L-6，详见 REG-SEC-007）。
> PASS_WITH_LIMITATION：验收通过但存在已知限制（L-1、L-2【已关闭 2026-08-10，P1-SEC-009 活体渗透 PASS，见 REG-SEC-004】、L-3/L-4、L-5、L-6），限制项见各案例「限制说明」，经评估均不阻断发布。

### REG-SEC-001

- **测试编号**：REG-SEC-001
- **模块**：权限安全-路由守卫（guards.ts 域权限矩阵）
- **关联任务**：P0-SEC-003（Sprint-3A 第一阶段 权限组）
- **业务场景**：域权限矩阵默认拒绝 —— 员工访问业务域全拒 / OWNER·ADMIN 全放行 / 显式配置角色放行
- **测试目的**：防止域矩阵 READ_ONLY 兜底回归 —— 非显式配置角色必须默认拒绝（fail-closed），不得隐式只读放行。
- **测试步骤**：
  1. 以 EMPLOYEE 访问 /finance/*、/hr/*、/purchase/*、/warehouse/*、/system/* 等未配置域
  2. 以 OWNER/ADMIN 访问任意域（含敏感域）
  3. 以显式配置角色访问对应业务域（如 PURCHASE_MANAGER → /purchase/*）
  4. 以未知/未配置角色访问任意业务域
  5. 只读提取 DOMAIN_VISIBLE_ROLES（guards.ts L46-66）与 getRoleDomainAccessLevel/checkDomainAccess 判定逻辑，复跑 14 角色 × 14 域仿真断言
- **预期结果**：
  - 权限正常：EMPLOYEE 对 finance/hr/purchase/warehouse/system/device/asset 全拒（跳 403）；OWNER/ADMIN 全放行；未知域拒绝
  - 默认拒绝：getRoleDomainAccessLevel()（guards.ts L79-92）仅返回 'FULL' 或 undefined（无 READ_ONLY 兜底）；checkDomainAccess()（L280-300）对 undefined/空角色列表 fail-closed
  - 仿真：14 角色 × 14 域判定矩阵 + 8 组断言全部通过
- **测试类型**：路由守卫仿真回归
- **当前状态**：PASS
- **限制说明**：无。

---

### REG-SEC-002

- **测试编号**：REG-SEC-002
- **模块**：权限安全-菜单-路由对齐（guards.ts L234-249 + 14 个 modules/*/menu.ts）
- **关联任务**：P0-SEC-004（Sprint-3A 第一阶段 权限组）
- **业务场景**：菜单隐藏 URL 拦截 / 显式 roles 路由精确校验 / seal 不误拦
- **测试目的**：防止「菜单 hidden 但 URL 可直达」与「seal 被域矩阵误拦」回归 —— 守卫集合必须与菜单 visibleRoles 单源一致。
- **测试步骤**：
  1. 逐域对照 14 个 modules/*/menu.ts visibleRoles 与 DOMAIN_VISIBLE_ROLES 守卫集合
  2. 菜单隐藏 URL 用非授权角色直接输入 URL 访问
  3. 以显式 meta.roles 路由访问（/seal/management，domain=system 但 roles 含 HR/FINANCE/OPS 总监）
  4. 以授权角色访问各域菜单 URL
- **预期结果**：
  - 菜单-守卫一致：14/14 域级一致（守卫集合 = 菜单 visibleRoles 并集）
  - URL 直达拦截：菜单隐藏 URL 非授权访问被拦截（handleAccessDenied + return false）；显式 meta.roles 为单源精确校验（checkRoutePermission）
  - seal 不误拦：/seal/management 走显式 roles 分支，不被 system 域矩阵误拦
  - 已知超放行点（L-1）：/workspace/material-request 域级并集放行 8 角色，该页为需求提报表单（非管理/敏感面），越权面低
- **测试类型**：路由守卫回归（菜单对照仿真）
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：L-1 —— /workspace/material-request（request-center/menu.ts:17-24 精确清单 [OWNER, ADMIN, STORE_MANAGER, TEAM_LEADER, DEPARTMENT_MANAGER, EMPLOYEE]）落在 workspace 域矩阵（全 14 角色并集，guards.ts:47-52）下，SCHEDULER / REGION_MANAGER / AUDITOR / OPS_DIRECTOR / FINANCE_DIRECTOR / HR_DIRECTOR / PURCHASE_MANAGER / WAREHOUSE_MANAGER 共 8 角色「菜单隐藏但 URL 可直达」该提报页；根因为域级矩阵粒度大于页面级菜单清单，守卫无法在同一域内区分不同路由。QA 影响评估：该页为需求提报表单（非管理/敏感面），越权面低；核心敏感域（finance/hr/system/device/asset 等）均精确。建议后续在 /workspace/material-request 路由声明精确 meta.roles（或拆分子域）。**不阻断发布**。

---

### REG-SEC-003

- **测试编号**：REG-SEC-003
- **模块**：权限安全-敏感路由（router/index.ts meta.roles）
- **关联任务**：P0-SEC-005（Sprint-3A 第一阶段 权限组）
- **业务场景**：/company-init、/system/device-whitelist、/supplier-portal/links 路由角色控制
- **测试目的**：防止三敏感路由角色声明回退 —— 员工不得直达，目标角色正常打开。
- **测试步骤**：
  1. 以 EMPLOYEE 访问 /company-init、/system/device-whitelist、/supplier-portal/links
  2. 以 OWNER/ADMIN 访问 3 路由
  3. 以 PURCHASE_MANAGER 访问 /supplier-portal/links
  4. 核对 router/index.ts 三路由 meta.roles 声明与各 menu.ts visibleRoles 一致
  5. 后端侧核对 AppDeviceRegistrationController generate/disable/enable 的 @PreAuthorize
- **预期结果**：
  - 权限正常：EMPLOYEE 访问 3 路由 → 403 页（handleAccessDenied + return false）；OWNER/ADMIN 全豁免放行（guards.ts L215）；PURCHASE_MANAGER 命中 /supplier-portal/links 显式 roles
  - 路由声明：/company-init roles=[OWNER, ADMIN]；/system/device-whitelist roles=[OWNER, ADMIN]（与 system/menu.ts visibleRoles 一致）；/supplier-portal/links roles=[OWNER, ADMIN, PURCHASE_MANAGER]（与 purchase/menu.ts 一致）
  - 后端：APK 设备白名单接口（AppDeviceRegistrationController generate/disable/enable）均已 @PreAuthorize hasRole('admin')
- **测试类型**：路由守卫回归
- **当前状态**：PASS
- **限制说明**：company-init / supplier-portal/links 后端接口 403 属 P0-SEC-001/002（后端安全组，第二阶段）范围，本批未验收 —— 登记**待回归**：Sprint-3A 第二阶段完成后补验后端侧 403。

---

### REG-SEC-004

- **测试编号**：REG-SEC-004
- **模块**：权限安全-权限源（permission.ts / utils/auth.ts）
- **关联任务**：P0-SEC-006（Sprint-3A 第一阶段 权限组）
- **业务场景**：/me 唯一授权源 / 未知角色 fail-closed / /me 失败登出
- **测试目的**：防止权限源回退 —— 权限数据必须唯一来自 /v1/auth/me；篡改 localStorage token 不得提升权限；未知角色不得降级 EMPLOYEE；/me 失败不得降级放行。
- **测试步骤**：
  1. 篡改 localStorage token 中的 roles 后刷新页面，比对页面权限是否变化
  2. 后端 /me 返回未知 role_code / 空串，断言 mapRoles 输出
  3. 使 /v1/auth/me 失败（网络中断/401），观察页面行为
  4. grep 全库确认 parseJWTPayload 及 utils/auth.ts hasRole/hasPermission/getUserRoles/getUserPermissions/getTokenUserInfo 无权限判定调用方
  5. 核对 v-permission 指令数据源（directives/permission.ts:33-48）
- **预期结果**：
  - 权限唯一源：runInitFromToken（permission.ts L461-521）仅以 authApi.getCurrentUser()（GET /v1/auth/me）为数据源；parseJWTPayload 函数体已删除且无残留调用
  - fail-closed：mapRoles（L668-743）未知角色映射 undefined 后被过滤，不降级 EMPLOYEE（仿真 mapRoles(['unknown_role_x']) → []、mapRoles(['']) → []）；v-permission 数据源 = userInfo.permissions（/me 来源），prod 下无数据返回 []
  - /me 失败登出：catch 分支（L472-482）clearUserInfo + 移除 token/refresh_token + 跳 /login
  - 登录链路兼容：LoginPage 登录成功后 initFromToken()（无参，从 localStorage 读 token → /me）；initPromise 并发共享消除竞态
- **测试类型**：权限源回归
- **当前状态**：PASS_WITH_LIMITATION（2026-08-10 复审由 PASS 收紧；2026-08-10 L-2 关闭后维持；2026-08-10 阶段② L-5 HTTP 级闭环后维持 —— 验收标准 4 已活体实测闭环，L-2 渗透 PASS 已关闭，L-5 浏览器 UI 部分仍开放，按协议未验证项不默认通过）
- **限制说明**（2026-08-10 复审更新；2026-08-10 L-2 关闭更新）：
  - L-2 —— **已关闭（2026-08-10）**：①「伪造 token 被后端拒绝」渗透用例已由 P1-SEC-009 活体渗透补充执行并 **PASS**（依据 `docs/quality/p1-sec-009-qa-report.md`）：未知角色 fail-closed 后端 403（finance/hr/system 实测）、篡改 token（不重新签名）HTTP 401、/me 失败（无 token → 业务 401）前端登出不降级、越权 URL 直达 403 均 **HTTP 活体实测通过**；正向对照（合法签名伪造 admin token → 200）证明 claims 信任边界=签名密钥，链路真实生效；KL-007（L-2）满足关闭条件。② utils/auth.ts 遗留 JWT 读取死代码（hasRole 等已无调用方，不参与任何权限判定；仅 OrderRefund.vue:198 取 userId 作审批操作人 ID、useResizableColumns.ts:66 取 userId 作列宽存储 key，均非权限）——经 P1-SEC-009 全仓 import 检查确认无权限判定调用方，维持建议清理项（P0-AUD 范围），非验证缺口。
  - L-5（复审新增，**阶段②更新：HTTP 级已闭环，浏览器 UI 残留**）—— 真实登录联调（登录→/me→路由跳转→按钮渲染全链路）：**HTTP 级已闭环（2026-08-10 阶段①，`sprint-3-closeout-regression-liaison.md`）**：登录→/me（340 权限含 `*`）→业务接口（finance/hr/members 抽样 200 code=0）+ 401/403 失效处理全链路活体 PASS，与 P1-SEC-009 渗透结论一致；**浏览器 UI 部分（401 弹框→登出、路由跳转、按钮渲染）无浏览器自动化环境，如实登记 PWL 残留**，待发布前浏览器冒烟补验（验证缺口，非缺陷）；本阶段活体复测 /me 200 code=0 维持。
  - 状态变更原因：2026-08-10 复审按「未验证项不得默认通过」将首轮 PASS 收紧为 PASS_WITH_LIMITATION；2026-08-10 依据 P1-SEC-009 活体渗透 PASS 关闭 L-2（验收标准 4 实测闭环、KL-007 满足关闭条件）；2026-08-10 阶段① KL-013 HTTP 级活体闭环（登录→/me→业务接口→401/403 全链路），L-5 仅剩浏览器 UI 渲染残留（验证缺口），故状态保持 PASS_WITH_LIMITATION（未验证项不默认通过）。其余验证缺口均不放大越权面（fail-closed 方向），**不阻断发布**。

---

### REG-SEC-005

- **测试编号**：REG-SEC-005
- **模块**：权限安全-业务角色域配置（guards.ts DOMAIN_VISIBLE_ROLES）
- **关联任务**：P1-SEC-001（Sprint-3A 第一阶段 权限组）
- **业务场景**：PURCHASE_MANAGER / WAREHOUSE_MANAGER / DEPARTMENT_MANAGER 业务域放行、其他域拒绝
- **测试目的**：防止三大业务角色被守卫全面拒绝回归 —— 业务角色必须能进入自身业务域且不得越入其他域。
- **测试步骤**：
  1. 以 PURCHASE_MANAGER 访问 /purchase/*、/workspace/*、/finance/*
  2. 以 WAREHOUSE_MANAGER 访问 /warehouse/*、/hr/*
  3. 以 DEPARTMENT_MANAGER 访问 /workspace/*、/purchase/*、/warehouse/*
  4. 只读提取 DOMAIN_VISIBLE_ROLES（guards.ts:46-66）复跑三组仿真断言
- **预期结果**：
  - 权限正常：PURCHASE_MANAGER → purchase ✅ / workspace ✅ / finance ❌（拒绝）；WAREHOUSE_MANAGER → warehouse ✅ / hr ❌；DEPARTMENT_MANAGER → workspace ✅ / purchase ❌ / warehouse ❌
  - 配置一致：purchase 域含 PURCHASE_MANAGER、warehouse 域含 WAREHOUSE_MANAGER、workspace 域含 DEPARTMENT_MANAGER，且与菜单对照一致
- **测试类型**：路由守卫仿真回归
- **当前状态**：PASS
- **限制说明**：无。

---

### REG-SEC-006

- **测试编号**：REG-SEC-006
- **模块**：权限安全-公开路由（guards.ts meta.requireAuth 判定）
- **关联任务**：P1-SEC-002（Sprint-3A 第一阶段 权限组）
- **业务场景**：/verify-code、/device-activation 未登录可用；/login 已登录跳首页
- **测试目的**：防止公开路由双轨回归 —— 公开页判定必须以 meta.requireAuth === false / meta.public === true 为单源，未登录访问不得被重定向登录页。
- **测试步骤**：
  1. 未登录访问 /verify-code、/device-activation
  2. 已登录访问 /login（PC / APK 各一次）
  3. 旧配置兼容访问：/login、/demo/component-gallery、/portal/sign
  4. 核对 guards.ts:141-154 isPublicRoute 判定位置（initFromToken 之前）与基线硬编码 publicRoutes 双轨数组是否回退
- **预期结果**：
  - 未登录不重定向：isPublicRoute = meta.requireAuth === false || meta.public === true，在 initFromToken() 之前判定，未登录正常打开 2 公开页
  - 已登录 /login：PC → /home；APK → receiving-mobile（防 PC 首页死循环，L148-152）
  - 旧配置兼容：/login（requireAuth:false）、/demo/component-gallery（requireAuth:false）、/portal/sign（public:true）
- **测试类型**：路由守卫回归
- **当前状态**：PASS
- **限制说明**：观察项 O-1 —— /forgot-password 为基线守卫白名单成员但路由从未注册（基线 router/index.ts 无此路由，当前亦无；LoginPage.vue:591 仍有 router.push('/forgot-password')），属**基线既有缺陷**非本批引入，建议后续前端批次修复；不阻断发布。

---

### REG-SEC-007

- **测试编号**：REG-SEC-007
- **模块**：权限安全-权限码注册表（permissions.ts + 后端权限清单 4 文件）
- **关联任务**：P1-SEC-003（Sprint-3A 第一阶段 权限组）
- **业务场景**：按钮权限码（product:pricing:batch / product:cost:export）前端注册表与后端发放清单统一
- **测试目的**：防止权限码漂移回归 —— v-permission 使用的权限码必须与注册表精确匹配，后端发放清单必须同步包含，不得单侧缺码。
- **测试步骤**：
  1. 以授权角色打开菜品定价页（DishPricing）与菜品成本分析页（DishCostAnalysis），核对两按钮可见性
  2. grep 核验 v-permission 码与 permissions.ts 注册表精确匹配
  3. 逐字核验后端 4 文件（AdminPermissions.java、AuthenticationServiceImpl.java、TokenServiceImpl.java、UserDetailsServiceImpl.java）权限清单含 product:cost:view、product:cost:export
  4. 检查 scripts/ 与 package.json 是否存在权限码静态校验脚本
- **预期结果**：
  - 前端注册表闭合：permissions.ts systemModules[0] 含 product:pricing:batch、product:cost:export（L62-64）；permissionDescriptions 含 2 条（L531-533）；DishPricing.vue:11 / DishCostAnalysis.vue:38 与注册表精确匹配
  - 后端清单同步：4 文件均含 "product:cost:view", "product:cost:export"（纯字符串追加、每文件 1 行，语法风险极低）；product:pricing:batch 基线已存在无需补
- **测试类型**：权限码核验
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**（2026-08-10 复审更新，新增 L-6）：
  - L-3：scripts/ 与 package.json 无权限码/域矩阵静态校验脚本（frontend/scripts/test-permission-security.ts 为权限中心浏览器测试，非注册表静态扫描）—— 当前仅能人工核验 2 码一致，全量扫描机制缺失，建议补静态校验脚本。
  - L-4：非 admin 角色（如 OPS_DIRECTOR/STORE_MANAGER）权限码发放依赖 role_permission 表数据（AuthServiceImpl.getUserPermissions L753-767 从 DB 聚合），无法在代码层证明「授权角色可见两按钮」，需联调环境造数验证（后端安全组阶段或回归阶段执行，登记**待回归**）。该方向为 fail-closed（数据缺失 → 按钮隐藏，不放大越权面）。
  - L-6（复审新增，源自观察项 O-5）：后端 4 文件权限码清单不一致——AdminPermissions.java 含 finance:tax:* 4 码 + purchase:order:* 6 码（共 10 码），未同步至 AuthenticationServiceImpl / TokenServiceImpl / UserDetailsServiceImpl（3 文件各仅 +1 行 product:cost:export）。QA 独立评估：差异仅 admin 分支生效（3 个 Service 文件为 admin 权限初始化清单），admin 另有 '*' 兜底（hasAuthority('*') 分支），运行期权限无增减差异；10 码与前端注册表（finance:tax:view、purchase:order:confirm/terminate/freeze/submit）及 Controller @PreAuthorize 使用点对齐，属维护性问题（4 文件清单需统一）—— 处置随 P0-SEC-001 统一。
  - 观察项 O-3：CostAnalysisController.java:26 类级 @PreAuthorize 为 product:cost:view，与前端导出按钮码 product:cost:export 不一致（导出接口实际仍受 cost:view 约束；若某角色仅有 export 无 view 则导出 403）—— 属 P0-SEC-001（后端 Controller 鉴权）范围，待后端组处理。
  - 编译限制（复审更新）：复审已执行后端编译独立复核——JAVA_HOME=P:\my-new-project\JDK21 + `mvn -DskipTests compile -q` → BUILD SUCCESS（EXIT=0），target/classes 下 4 个 .class 时间戳 2026-08-10 13:57；上轮「纯字符串追加每文件 1 行」描述不准确（AdminPermissions.java 实际含 3 处 hunk，详见 L-6）。
  - 以上限制均**不阻断发布**。

---

### REG-SEC-008

- **测试编号**：REG-SEC-008
- **模块**：权限安全-会话处理（request.ts handleSessionExpired）
- **关联任务**：P1-SEC-004（Sprint-3A 第一阶段 权限组）
- **业务场景**：token 过期 401 —— 弹框无「留在本页」/ 关闭即登出 / 连续 401 只弹一次
- **测试目的**：防止「留在本页」死会话回归 —— 会话过期后页面必须 fail-closed（强制登出），不得持续可交互。
- **测试步骤**：
  1. token 过期后触发任一业务请求，观察弹框按钮（应无「留在本页」）
  2. 尝试 ESC / 点击遮罩关闭弹框
  3. 弹框展示期间连续触发多个 401
  4. 点击「重新登录」/ 关闭弹框，检查登出行为与清理的 auth key
  5. 核对静默刷新重放逻辑与登录接口业务 401 透传
- **预期结果**：
  - 无「留在本页」：handleSessionExpired（request.ts L129-175）showCancelButton:false、closeOnClickModal:false、closeOnPressEscape:false，弹框仅「重新登录」按钮
  - 关闭即登出：catch 分支同样执行 clearAuthAndRedirect()（L84-103），清理 9 个 auth key（token/refresh_token/user_id/username/token_expiry/permissions/roles/menu-overrides/menu-template/tab-bar-tabs）并跳 /login
  - 防重：sessionExpiredPrompting 标记（L116），弹框展示期间后续 401 直接 reject 不再弹框
  - 附加：401 先尝试 refreshTokenSilently 静默刷新重放（_retried 防循环，L129-145）；登录接口业务 401（HTTP 200+code 401）不误报（isLoginRequest 透出后端 message）；prod 分支才登出、dev 分支保持原行为
- **测试类型**：手工回归
- **当前状态**：PASS
- **限制说明**：观察项 O-2 —— sessionExpiredPrompting 在 finally 中重置，弹框关闭瞬间（clearAuthAndRedirect 同步跳转前）若有并发 401 会重弹一次；实际窗口极短（同步跳转），风险可忽略。不阻断发布。

---

## Sprint-3A 第二阶段回归（P0-SEC-002 运维高危接口鉴权）

> 验收依据：`docs/quality/p0-sec-002-qa-report.md`（2026-08-10，独立验收 PASS_WITH_LIMITATION：6 组 37/37 端点全覆盖 / FAIL 0，基线 git 提交 dd33ee3）。
> PASS_WITH_LIMITATION：限制 L-1/L-2（运行实测缺失，登记待回归/渗透阶段补验）、L-3（范围外 maintenance 未鉴权方法归属，由 P0-SEC-001-D 承接）、L-4（`*` 权限放行，既有安全模型设计）——逐条确认不阻断发布。

### REG-SEC-009

- **测试编号**：REG-SEC-009
- **模块**：运维/系统（高危接口鉴权；maintenance 包 6 控制器：DatabaseBackup / DataFix / SystemInit / Cache / EncodingFix / ExceptionMonitoring）
- **关联任务**：P0-SEC-002（Sprint-3A 第二阶段 运维高危接口鉴权）
- **业务场景**：员工/组长/排班员调用 6 组高危接口 → 403；OWNER/ADMIN → 200；data-fix 无 confirm 拒绝执行；成功调用落审计日志（sys_operation_logs）
- **测试目的**：防止运维高危接口裸奔回归 —— 6 组 37 端点必须保持 `@PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('*')")` + `@OperationLog` 全覆盖（DatabaseBackup 12 / DataFix 5 / SystemInit 13 / Cache 2 / EncodingFix 3 / ExceptionMonitoring 2）；data-fix 4 写操作 confirm 二次确认；成功调用可审计追踪。
- **测试步骤**：
  1. 以员工/组长/排班员 token 调用 6 组 37 端点（/v1/backup、/v1/admin/data-fix、/v1/system/init、/v1/cache、/v1/encoding-fix、/v1/exception-monitoring）→ 应全部 403
  2. 以 OWNER/ADMIN token 调用同一批端点 → 应 200
  3. 调用 data-fix 4 写操作（execute / fix-test-data / create-labor-contracts / fix-users-table）不带 confirm → 应业务拒绝、写逻辑零执行；带 confirm=true → 执行；/status 只读无 confirm
  4. 成功调用任一端点后查询 sys_operation_logs（operator_id / operator_name / operator_ip / request_url / request_params / request_result）→ 应有审计行
  5. 静态三重核验：逐文件正则计数 + 全文件 grep + `scripts/scan-controller-authz.py`（6 控制器 total=37 covered=37 uncovered=0），并复核 `mvn -DskipTests compile` EXIT=0
- **预期结果**：
  - 权限正常：员工/组长/排班员（SecurityUser.getAuthorities 仅生成 ROLE_<角色名>，无 ROLE_OWNER/ROLE_ADMIN/`*`）→ hasRole 全 false → 403；OWNER/ADMIN（生成 ROLE_OWNER/ROLE_ADMIN + `*` authority）→ 200；`@EnableMethodSecurity(prePostEnabled=true)`（SecurityConfig.java:31）+ 6 组路径不在 permitAll、落入 `anyRequest().authenticated()`（L166）
  - 二次确认：4 写操作均 `@RequestParam(defaultValue="false") boolean confirm`，`!confirm` 直接 Result.error 返回
  - 审计链路：37 端点 @OperationLog → OperationLogAspect（操作人/ID/IP/URL/参数/结果/耗时）→ OperationLogServiceImpl → OperationLogMapper.insert → sys_operation_logs（DDL 16 列与实体逐列对齐）
  - 编译：mvn compile EXIT=0；diff 无超范围（仅注解 / confirm 参数 / 描述文案）
- **测试类型**：API 回归（代码级三重核验 + 编译；运行实测待回归阶段补）
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：
  - L-1 —— 运行时 403/200 实测缺失（无运行中后端服务 + 无各角色 token），验收标准 1/2 仅代码级核验；登记**待回归**：运行环境用 3 类角色 token 实测 6 组接口 → 员工/组长/排班员 403、OWNER/ADMIN 200、data-fix 不带 confirm → 业务错误、confirm=true → 执行
  - L-2 —— 审计日志落库实测缺失（切面→Service→Mapper→DDL 链路代码级完整，未在运行库验证 sys_operation_logs 真实写入）；登记**待回归**：调 /v1/backup/list 后查 sys_operation_logs 行
  - L-3 —— maintenance 包 10 个未鉴权方法（DatabaseCleanupController 2 / HealthCheckController 3 / HealthController 1 / PerformanceMonitorController 2 / SystemDataController clearProductData 1 / SystemMonitorController getStatus 1，不在本卡 6 组范围）归属 **P0-SEC-001-D** 承接闭环，防高危运维接口遗漏
  - L-4 —— `hasAuthority('*')` 放行面依赖「普通用户 permissions 不含 `*`」既有约定（`*` 仅由 ADMIN/Z/admin 角色展开，AdminPermissions.WILDCARD；代码级确认普通员工权限清单不含）；登记待数据审计阶段核查 users 权限数据 WILDCARD 归属
  - 观察项（不阻断）：O-1 Cache stats 假统计、O-2 ExceptionMonitoring /reset GET 写语义、O-3 SystemInit /reset 生产可达、O-5 403 拒绝调用不进审计日志（运维盲区）——均建议后续独立卡/批次，非本卡引入范围
  - 以上限制**均不阻断发布**。

---

## Sprint-3A 第二阶段回归（P0-SEC-001 全后端 Controller 鉴权）

> 验收依据：`docs/quality/p0-sec-001a-qa-report.md`（财务域：finance 32 Controller 197/197、marketing 8/30/30 = 100%）、`p0-sec-001b-qa-report.md`（HR+Schedule：HR 根包 279/279、Schedule 10 Controller 56/56 = 100%，全后端 1991/2044 = 97.4%）、`p0-sec-001c-qa-report.md`（采购 8 Controller 59/59、仓储 19 Controller 129/129、根包采购 13 Controller 94/101）、`p0-sec-001d-part1-qa-report.md` + `p0-sec-001d-qa-report.md`（系统管理+其余域，全后端 1991/2044 = 97.4% 含 34 设计豁免 KL-043）——2026-08-10，子批 A/B/C/D 全部 PASS_WITH_LIMITATION / FAIL 0，架构 2026-08-10 宣布整卡 PASS_WITH_LIMITATION（remediation-roadmap.md §Sprint-3A 第二阶段）；基线 git 提交 dd33ee3。
> PASS_WITH_LIMITATION：验收通过但存在已知限制（L-1 运行实测缺失、L-2 CI 管道未落地）+ 整卡排除项（34 设计豁免 KL-043 + 30 BLOCKED PD-006~008 + 范围外 17），限制项见 REG-SEC-010「限制说明」，逐条确认均有闭环路径、**不阻断发布**。

### REG-SEC-010

- **测试编号**：REG-SEC-010
- **模块**：全后端 Controller 鉴权（财务 / HR+Schedule / 采购 / 仓储 / 系统管理 / 其余域，267 Controller / 2044 方法）
- **关联任务**：P0-SEC-001（Sprint-3A 第二阶段 全后端 Controller 鉴权，子批 A/B/C/D）
- **业务场景**：71 个裸 Controller 补 @PreAuthorize 至 100%（排除项口径：1991 覆盖 + 34 豁免 KL-043 + 30 BLOCKED PD-006~008 + 范围外 17）；已覆盖 194 接口权限码不变；覆盖率扫描脚本 --strict 门禁
- **测试目的**：防止后端 Controller 裸奔（水平越权）回归 —— 任一业务方法必须有方法级鉴权注解，或命中已登记豁免/BLOCKED/范围外清单；不得新增裸方法、不得改动既有权限码。
- **测试步骤**：
  1. 全量复跑 `python scripts/scan-controller-authz.py --strict`（归属算法已修正：注解须位于「上一方法声明行之后、本方法声明行之前」区间），核对全后端 267 Controller / 2044 方法覆盖 ≥ 1991（97.4%），未覆盖方法必须全部命中排除项清单（34 豁免 KL-043 + 30 BLOCKED PD-006~008 + 范围外 17）；新裸方法 → 退出码 1
  2. 抽查新增/改动 Controller 的 @PreAuthorize 区间归属，无借位、无假覆盖（防 O-A-1 窗口法假覆盖缺陷回归）
  3. 对涉及鉴权改动的文件执行 `git diff dd33ee3`，核对已覆盖 194 接口权限码清单不变（diff 纯新增注解行，0 删除既有 @PreAuthorize / 权限码）
  4. 以员工/组长等非授权角色 token 调用抽样接口 → 应 403；以 OWNER/ADMIN → 200（fail-closed：SecurityUser.getAuthorities() 非 admin 仅展开自身 permissions，无码即 hasAuthority 不匹配 → 403）
  5. 复核 `mvn -DskipTests compile` EXIT=0
- **预期结果**：
  - 覆盖率：1991/2044 = 97.4% + 34 豁免 + 30 BLOCKED + 17 范围外 = 整卡 100% 口径成立；--strict 门禁对新裸方法退出码 1
  - 权限码：已覆盖 194 接口权限码不变（子批 A 验收标准 3 PASS：ReceiptController 仅 +3 注解、AccountBalanceController getAccountBalanceList 注解未变、P0-FIN-004 12 文件不在 diff 内；B 批 13 文件 0 删除行）
  - 权限正常：`@EnableMethodSecurity(prePostEnabled=true)`（SecurityConfig.java:31）+ 排除项外路径全部方法级 @PreAuthorize，无权限角色 403、admin/`*` 放行
  - 编译：mvn compile EXIT=0（子批 D 报告含 .class 时间戳双重验证）
- **测试类型**：API 回归（代码级 + 扫描脚本 --strict 门禁；运行实测待回归/渗透阶段）
- **当前状态**：PASS_WITH_LIMITATION（2026-08-10 整卡验收通过，架构宣布整卡 PASS_WITH_LIMITATION）
- **限制说明**：
  - L-1 —— 运行时 403 实测缺失（无运行中后端服务 + 无各角色 token），抽样非授权角色 → 403 仅代码级核验（同 KL-007 处置、同 REG-SEC-009 L-1 模式）；登记**待回归**：运行环境以员工/组长 token 实测抽样接口 → 403、OWNER/ADMIN → 200
  - L-2 —— CI 管道未落地：脚本 `--strict` 门禁逻辑实测可用（finance/marketing 退出码 0、全量 97.4% 退出码 1），但仓库内无 CI 管道配置证据（无项目自有 .github/workflows、Jenkinsfile 等），实际 CI 接入待整卡收口确认（若为外部 CI 须补充证据）
  - 排除项（不纳入通过数，均有闭环路径）：34 设计豁免（SecurityConfig permitAll 6 组：device-registrations status/activate、receipt-confirmations verify/**、users check-username/email/phone、tray/** GET、kitchen/** GET、wecom/**，KL-043 豁免清单化）；30 BLOCKED（PD-006：SelfPurchase 6 / Task 12 / PlanItem 7；PD-007：Appeal 5；PD-008：SupplierPortal H5 6，产品决策池登记）；范围外 17（ApprovalWorkflow 13 / QuickStockIn 3 / MiniProgram 1，待后续批次）
  - 以上限制**均不阻断发布**。

---

## Sprint-3B-1 回归（P1-DATA 数据真实性治理）

> 验收依据：`docs/quality/sprint-3b1-qa-report.md`（2026-08-10，独立验收 9/9：PASS 4 / PASS_WITH_LIMITATION 5 / FAIL 0，基线 git 提交 dd33ee3）。
> PASS_WITH_LIMITATION：验收通过但存在已知限制（L-2~L-5），限制项见各案例「限制说明」，经评估均不阻断发布；V-1/V-2 联调验证缺口登记待回归阶段闭环。

### REG-DATA-006

- **测试编号**：REG-DATA-006
- **模块**：仓库-盘点（InventoryCheck）
- **关联任务**：P1-DATA-001（Sprint-3B-1 数据真实性治理）
- **业务场景**：盘点单团队人员下拉与提交
- **测试目的**：防止盘点人员假数据回归 —— 盘点团队人员必须来自真实员工接口，不得再出现硬编码假员工。
- **测试步骤**：
  1. 打开盘点单创建/编辑，展开团队人员下拉
  2. 按「仓储/财务」部门过滤选择员工
  3. 提交盘点单，检查提交载荷中人员字段
  4. 全仓 grep 假员工名（张经理/赵仓管/孙冷藏/周盘点员/吴盘点员/钱财务/郑会计/王出纳）
- **预期结果**：
  - 数据源：下拉来自 `employeeApi.getEmployees()`（GET /v1/employees/page），按 `departmentName` 含「仓储/财务」过滤
  - ID 流转：提交以真实员工 ID 回填姓名，无假值
  - 无残留：grep 无上述假员工名残留
- **测试类型**：数据一致性检查
- **当前状态**：PASS
- **限制说明**：无（观察项 O-1：`participatingDepts` 硬编码默认 ['仓储部','财务部'] 为基线既有业务默认，非假人员数据，建议后续接部门接口，关联 P2-DATA-004）。

---

### REG-DATA-007

- **测试编号**：REG-DATA-007
- **模块**：仓库-智能补货（SmartRestock）
- **关联任务**：P1-DATA-002（Sprint-3B-1 数据真实性治理）
- **业务场景**：补货建议提交时的供应商选择
- **测试目的**：防止供应商假数据回归 —— 供应商下拉必须来自真实接口，不得再出现 SUP001 双汇 / SUP002 益海嘉里 / SUP003 蒙牛 假供应商。
- **测试步骤**：
  1. 打开智能补货页，展开供应商下拉
  2. 选择真实供应商并提交补货建议
  3. 检查提交记录中供应商名
  4. 全仓 grep 假供应商名（双汇/益海嘉里/蒙牛 + SUP001~003）
- **预期结果**：
  - 数据源：下拉来自 `supplierApi.getEnabledList()`（GET /v1/suppliers/list?status=1，`SupplierController.java:53` 存在）
  - ID 流转：提交记录取真实选中供应商名，无假值
  - 无残留：grep 无假供应商名残留
- **测试类型**：数据一致性检查
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：L-1（建议记录 Tab 5 条硬编码假历史，含假供应商名）已由 P1-MOCK-004 承接闭环（假历史删除→空态+标注，见 REG-MOCK-003）；供应商接口运行期数据联调（3B-1 V-1）待回归阶段闭环。

---

### REG-DATA-008

- **测试编号**：REG-DATA-008
- **模块**：仓库-库存调整（InventoryAdjust）
- **关联任务**：P1-DATA-003（Sprint-3B-1 数据真实性治理）
- **业务场景**：库存调整单参与部门选择
- **测试目的**：防止部门假数据回归 —— 参与部门必须来自真实部门接口，不得硬编码仓储/财务/采购/运营 4 部门。
- **测试步骤**：
  1. 打开库存调整单，展开参与部门勾选组
  2. 检查预选部门与勾选组来源
  3. 提交调整单，检查部门字段
- **预期结果**：
  - 数据源：部门下拉来自 `useDepartmentOptions(true)` 真实部门接口；`participatingDepts: []` 不再预选假部门
  - ID 流转：提交部门为真实值
- **测试类型**：数据一致性检查
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：L-2 —— 会签状态展示（`getDeptSignStatus`）仍为本地 mock（代码已注释标明），「已签/待签」非后端真实状态，仅展示层、不影响提交数据；待后端补会签状态字段或移除展示。

---

### REG-DATA-009

- **测试编号**：REG-DATA-009
- **模块**：HR-合同（HRContract）
- **关联任务**：P1-DATA-004（Sprint-3B-1 数据真实性治理）
- **业务场景**：合同创建时的模板选择与详情回显
- **测试目的**：防止合同模板假数据回归 —— 模板下拉必须来自真实接口，不得出现 tpl_001~003 假模板。
- **测试步骤**：
  1. 打开合同创建，展开模板下拉
  2. 选择真实模板并提交
  3. 打开合同详情，检查模板名回显
- **预期结果**：
  - 数据源：模板下拉来自 `contractTemplateApi.getActiveTemplates()`（GET /v1/hr/contract-template/active，`ContractTemplateController.java:120` 存在）
  - ID 流转：提交载荷含真实 templateId；详情回显真实模板名（找不到显示 ID，不硬编码）
  - 无残留：grep 无 tpl_001~003 残留
- **测试类型**：数据一致性检查
- **当前状态**：PASS
- **限制说明**：无（观察项：`handleEdit` 仍 `probationMonths: 3` 覆盖，属 P0-DATA-008 既有问题，独立收尾批次，非本卡）。

---

### REG-DATA-010

- **测试编号**：REG-DATA-010
- **模块**：供应商门户-签约链接（SignLinkManagement）
- **关联任务**：P1-DATA-005（Sprint-3B-1 数据真实性治理）
- **业务场景**：签约链接模板选择、签约/访问记录展示
- **测试目的**：防止假模板与伪造签约/访问记录回归 —— 模板必须真实，无 张三 / 192.168.1.100 伪造记录。
- **测试步骤**：
  1. 打开签约链接管理，检查模板来源
  2. 查看签约/访问记录区块
  3. 全仓 grep 张三 / 192.168.1.100
- **预期结果**：
  - 数据源：模板来自真实接口；无伪造签约/访问记录
  - 缺失如实：统计区块后端无接口时 el-alert 诚实标注「接口暂未提供，区块暂不展示」（后端探测属实：`SupplierPortalController` 无统计接口）
  - 无残留：grep 无伪造记录残留
- **测试类型**：数据一致性检查
- **当前状态**：PASS
- **限制说明**：观察项（P1-API-001 范畴，非本批引入）—— `handleRenew`/`handleDelete` 无 API 调用直接 success、`handleSubmit` 载荷语义待后端核对，挂 P1-API 范畴登记。

---

### REG-DATA-011

- **测试编号**：REG-DATA-011
- **模块**：仓库-库位（InventoryLocation）
- **关联任务**：P1-DATA-006（Sprint-3B-1 数据真实性治理）
- **业务场景**：库位操作记录展示
- **测试目的**：防止库位假出入库记录回归 —— 不得再展示固定假记录（赵仓管/刘店长等）。
- **测试步骤**：
  1. 打开库位页，查看操作记录区块
  2. 全仓 grep 假记录残留
- **预期结果**：
  - 无假数据：无固定假记录展示
  - 缺失如实：接口缺失时 el-alert 诚实标注「库位操作记录接口后端暂未提供，该区块暂不展示」（后端探测属实）
- **测试类型**：数据一致性检查
- **当前状态**：PASS
- **限制说明**：观察项（P1-API-003 范畴，非本批引入）—— `inventoryApi.getByLocationId` 失败时 API 内部回退 Mock（代码注释自述），挂 P1-API 范畴登记。

---

### REG-DATA-012

- **测试编号**：REG-DATA-012
- **模块**：系统-AI 模型配置（AIModelConfig）
- **关联任务**：P1-DATA-008（Sprint-3B-1 数据真实性治理）
- **业务场景**：AI 服务商选项列表
- **测试目的**：防止硬编码服务商回归 —— 服务商列表必须从真实模型数据派生，不得硬编码 9 家服务商。
- **测试步骤**：
  1. 打开 AI 模型配置页，展开服务商下拉
  2. 检查默认值与选项来源
  3. 全仓 grep 硬编码服务商（openai/azure/anthropic/qwen/deepseek/zhipu/baidu/moonshot/local）
- **预期结果**：
  - 数据源：选项来自 `aiModelApi.getAll()`（GET /v1/ai-models，`AIModelConfigController.java:36` 存在）真实 provider 去重派生，下拉 allow-create 兜底；默认 provider 为空（非 'openai'）
  - 无残留：无 9 硬编码服务商
- **测试类型**：数据一致性检查
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：L-3 —— `todayCalls`/`successRate` 统计卡显示 0（后端无统计接口，注释已说明，未假造接口）；服务商字典接口待后端排期。

---

### REG-DATA-013

- **测试编号**：REG-DATA-013
- **模块**：营销-会员统计（MemberOverview / MemberList）
- **关联任务**：P1-DATA-009（Sprint-3B-1 数据真实性治理）
- **业务场景**：会员统计概览聚合
- **测试目的**：防止硬编码 0 假统计回归 —— 会员统计必须来自真实聚合（members / recharge_records / orders 三表）。
- **测试步骤**：
  1. 打开会员概览，核对各统计卡数值来源
  2. 新增会员/充值/消费后刷新，比对统计变化
  3. 检查后端聚合 SQL 口径（`selectStatsOverview` / `sumMemberConsumeInRange`）
- **预期结果**：
  - 真实聚合：members（total/newThisMonth/newToday/active30/dormant90/atRisk30/balance 求和）、充值按 payment_status in success,partial_refunded + payment_time 本月、消费按 customer_id 非空 + payment_status=2
  - 无硬编码：无 10 个硬编码 0；口径缺失字段（consumeRatio/avgOrderAmount/repurchaseRate）已移除，前端 '--' 降级
- **测试类型**：数据一致性检查
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**（2026-08-10 阶段②更新：DEF-4 子项关闭）：
  - L-4 —— `types/member.ts` 3 移除字段仍声明必填（类型未对齐，运行时安全）；聚合运行期正确性（3B-1 V-1）经回归联调闭环后**暴露 DEF-4**（2026-08-10，`docs/quality/p0-kl-020-rerun.md`）：`RechargeRecordMapper.xml` selectStatsOverview 驼峰别名在 PostgreSQL 下折叠为小写（psql 实证 `rechargethismonth`）→ MyBatis Map key 全小写 → `MarketingMemberServiceImpl.java:232` `get("rechargeThisMonth")` 取 null → overview.rechargeThisMonth **恒 0.00**（造数 +1000.00 未反映，同响应其余 8 字段精确匹配）；连带 `GET /v1/recharge-stats/overview` 11 个统计字段全 0（`RechargeStatsServiceImpl.java:41-54` 同根因）。**DEF-4 子项已关闭（2026-08-10，阶段②）**：由 P0-DATA-010 修复（16 个 mapper XML 纯引号变更，157 去引号对逐字符一致、Java 零改动）+ QA 复验 PASS（`docs/quality/p0-data-010-r1-qa-report.md`，7/7：overview.rechargeThisMonth=1250.00 与 DB 精确一致、recharge-stats 11 字段全非 0 对账、planUsageDistribution 驼峰 key、造数清理 0 残留、编译 EXIT=0）→ 基线化 REG-DATA-017；本阶段活体复验 overview/recharge-stats 均 200 code=0（基线空表值 0/0.00 与 DB 一致）。DEF-1（overview 500）已修复并经回归活体复验闭环（重跑：200 code=0、8/9 字段与 DB 一致、造数增量精确匹配、清理 0 残留）。
  - L-5（保持）—— PD-005 口径决策（consumeRatio/avgOrderAmount/repurchaseRate 3 字段移除 + 前端 `--` 降级）：产品/财务未决策前禁止恢复实现，**不阻断发布**（字段已降级移除、无假数据）。

---

### REG-DATA-014

- **测试编号**：REG-DATA-014
- **模块**：系统-组织下拉（stores/org-context.ts + useStoreOptions / useDepartmentOptions）
- **关联任务**：P1-DATA-010（Sprint-3B-1 数据真实性治理）
- **业务场景**：门店/部门切换后下拉选项刷新
- **测试目的**：防止下拉陈旧化回归 —— 组织/门店切换后下拉必须重载真实选项，失败时清空旧选项（不展示陈旧数据）。
- **测试步骤**：
  1. DecisionBoard 切换门店（watch(storeId)→setStore）
  2. 观察其他页面门店/部门下拉是否重载
  3. 构造加载失败，观察下拉是否清空旧选项
- **预期结果**：
  - 刷新机制：org-context version 仅在实际值变化时递增（L32-35/40-43），refresh 主动递增；useStoreOptions/useDepartmentOptions 监听 version 重载
  - 失败处理：加载失败清空旧选项（L50/121-123）
  - 接线唯一：`setStore(` 唯一调用点为 DecisionBoard（L74-77）
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：L-5 —— 无组织切换 UI（`setOrg(` 零调用），机制已就绪但无入口，如实声明（登记产品决策池）；浏览器交互验证（3B-1 V-2）经回归代码级重跑确认（2026-08-10，`docs/quality/p0-kl-020-rerun.md`）：org-context version→watch 重载机制完整（org-context.ts L30-50 / useStoreOptions L81-86 / useDepartmentOptions L151-156，失败清空旧选项），浏览器实际交互仍待浏览器环境（如实登记，不默认关闭）。

---

## Sprint-3B-2 回归（P1-MOCK / P1-EXPORT 治理）

> 验收依据：`docs/quality/sprint-3b2-qa-report.md`（2026-08-10，独立验收 6/6：PASS 3 / PASS_WITH_LIMITATION 3 / FAIL 0 + P1-MOCK-010-R1 复验 PASS 追加报告尾部，基线 git 提交 dd33ee3）。
> PASS_WITH_LIMITATION：验收通过但存在已知限制（V-1/V-2/V-3 联调验证缺口 + O 项观察），限制项见各案例「限制说明」，经评估均不阻断发布；V-1~V-3 登记待回归阶段闭环。

### REG-MOCK-001

- **测试编号**：REG-MOCK-001
- **模块**：门店-招聘（StoreRecruitment）
- **关联任务**：P1-MOCK-001（Sprint-3B-2 Mock 治理）
- **业务场景**：招聘岗位创建/更新/发布/删除 4 写操作
- **测试目的**：防止招聘写操作假成功回归 —— 后端无岗位 CRUD 能力期间，写操作必须禁用+明确提示，不得本地假成功。
- **测试步骤**：
  1. 打开门店招聘页，尝试创建/更新/发布/删除岗位
  2. 观察操作入口状态与提示文案（应为 warning 禁用 + el-alert 标注）
  3. 刷新列表，检查列表数据来源（应走真实 GET /approvals/my-list）
  4. 后端核验：store-management 域是否存在岗位 CRUD 端点（应为无，全端点=approvals 审批流）
- **预期结果**：
  - 写操作禁用：4 写操作全部替换为 `ElMessage.warning('...接口未就绪，操作已禁用')` + el-alert 标注（StoreRecruitment.vue:216-260, 369-385），无假成功
  - 列表真实：列表走 `GET /approvals/my-list`（api/store-ops/recruitment.ts:199）
  - 后端核验：`StoreManagementRecruitmentController` 全端点=approvals 审批流（submit/approve/reject/my-list/pending-me/detail），无岗位 CRUD
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：① 注释「后端无岗位 CRUD」表述过宽——HR 域存在真实 `/v1/recruitment-requirements` CRUD（`RecruitmentRequirementController`），是否复用属产品/架构裁决（O-1，登记 KL）；② 后端 `GET /v1/store-management/recruitment/approvals` 为占位实现恒返空分页（O-2，后端侧占位、非前端假成功，登记 KL）；③ benefits 编辑回填为空（注释说明无真实来源）。

---

### REG-MOCK-002

- **测试编号**：REG-MOCK-002
- **模块**：仓库-盘点计划（InventoryCheck）
- **关联任务**：P1-MOCK-003（Sprint-3B-2 Mock 治理）
- **业务场景**：盘点计划 Tab（计划列表 + 新增/编辑/保存/启停/执行）
- **测试目的**：防止盘点计划假数据回归 —— 无后端实体期间，计划 Tab 必须空态+禁用，不得展示硬编码计划。
- **测试步骤**：
  1. 打开盘点页计划 Tab，检查列表是否为空态（el-empty）
  2. 尝试新增/编辑/保存/启停/执行计划，观察按钮状态与提示
  3. 切换盘点记录 Tab，检查数据来源（应走真实接口）
  4. 全仓 grep 硬编码计划残留（CP001~005 / WH001~005 / EMP010~012 及 InventoryCheckPlan/check_plan/CheckPlan 实体）
- **预期结果**：
  - 空态+禁用：`planList=[]`（5 条硬编码计划删除，InventoryCheck.vue:426-442）；新增/编辑/保存/启停/执行 5 个写操作全部 warning 禁用（L528-566）+ el-alert + el-empty 标注（L658-672）
  - 记录真实：盘点记录 Tab 真实接口保留
  - 无残留：grep 0 命中 InventoryCheckPlan/check_plan/CheckPlan 实体（后端确无盘点计划实体）
- **测试类型**：手工回归
- **当前状态**：PASS
- **限制说明**：无（`planApiReady` 恒 false 为死标志，仅 UI 冗余；计划功能整体禁用待后端排期，符合「无接口→禁用+标注」兜底）。

---

### REG-MOCK-003

- **测试编号**：REG-MOCK-003
- **模块**：仓库-智能补货（SmartRestock）
- **关联任务**：P1-MOCK-004（Sprint-3B-2 Mock 治理；承接 P1-DATA-002 残留假历史 L-1 / KL-017）
- **业务场景**：采购建议提交与建议记录
- **测试目的**：防止采购建议假提交/假历史回归 —— 提交必须真实落库（POST /v1/purchase/requests），建议记录不得有假历史。
- **测试步骤**：
  1. 在智能补货页填写采购建议并提交，观察请求（应为真实 POST /v1/purchase/requests）
  2. 打开采购申请列表/DB，核对落库记录（title/requestType 'routine'/priority/expectedDate/description/items）
  3. 打开建议记录 Tab，检查是否有假历史（双汇/益海嘉里等假供应商名）
  4. 核对供应商下拉仍为真实接口（3B-1 改动保留）
- **预期结果**：
  - 真实落库：`handleSuggestionSubmit` 调 `purchaseRequestApi.create()`（后端 `PurchaseRequestController.java:58` 存在），载荷与 `PurchaseRequestCreateParams` 一致（materialId/quantity/estimatedPrice/plannedReceiverType 'WAREHOUSE'；`converters.ts:180-196` 映射核实通过）
  - 无假历史：`suggestionHistory` 5 条假历史删除→空态+el-alert（L546-563），grep 无假数据残留（KL-017 闭环）
  - 3B-1 保留：`supplierApi.getEnabledList` 真实供应商下拉完整保留（L252-268）
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：V-1 —— 真实 POST 落库/采购侧可见未做运行时联调（无运行环境），待回归阶段闭环（同 3B-1 KL-020 模式）；建议记录 Tab 待后端接口（已标注，符合兜底）。

---

### REG-MOCK-004

- **测试编号**：REG-MOCK-004
- **模块**：系统-角色管理（RoleManagementTab）
- **关联任务**：P1-MOCK-009（Sprint-3B-2 Mock 治理）
- **业务场景**：角色绑定用户列表、分配用户、新增用户
- **测试目的**：防止假用户降级回归 —— 用户列表必须真实，失败不得降级为假数据，必须透传错误。
- **测试步骤**：
  1. 打开角色管理，进入绑定用户视图，检查用户列表来源
  2. 构造后端失败（空列表/500），观察列表与提示
  3. 提交分配用户/新增用户，检查请求与提示
- **预期结果**：
  - 无假用户：mockUsers 5 条删除（RoleManagementTab.vue:16-23），getList 走 `userApi`（api/system/user.ts:140 真实）
  - 失败透传：`handleAssignUsers`（L356-373）两处 mock 降级（空列表/失败）均移除，catch → `ElMessage.error` + 空列表
  - 写操作真实：`handleUserSubmit`（L376-393）真实 `userApi.assignRoles`（user.ts:214 存在）
- **测试类型**：手工回归
- **当前状态**：PASS
- **限制说明**：无（失败时弹窗仍关闭（L389 warning 后关闭），错误已透传，非假成功）。

---

### REG-MOCK-005

- **测试编号**：REG-MOCK-005
- **模块**：多模块本地变更假成功组（AlertCommandCenter / StoreCertificate / TopNavbar / useDomainPermission / AssetDisposal）
- **关联任务**：P1-MOCK-010（Sprint-3B-2 Mock 治理；2026-08-10 经 -R1 复验 PASS）
- **业务场景**：5 处本地变更假成功清零 + 通知接真实接口 + 审批记录真实字段
- **测试目的**：防止本地变更伪装成功回归 —— 无后端能力必须禁用+标注，有后端能力必须接真实接口，不得本地假成功。
- **测试步骤**：
  1. 告警中心：尝试 severity 升级，观察禁用+warning（后端 /v1/alerts 无升级端点）
  2. 证照预警：触发「生成预警任务」，观察禁用（后端无任务创建端点）
  3. 顶栏通知：打开通知列表，标记已读/全部已读，检查请求与实体契约
  4. 域权限 Tab：执行「同步菜单」，观察提示（应为「预览仅本地生效」info，无「已同步」成功文案）
  5. 资产处置：打开处置记录，检查审批字段来源（应来自 detail 真实字段，无 张经理/2024-01-15 假记录）
  6. 回归核验：StoreCertificate.vue:1358 `append-to-body` 保持纯布尔（无 `:append-to-body` 无值绑定），vue-tsc 无 TS2339
- **预期结果**：
  - ① 告警升级：假成功删除→warning 禁用（AlertCommandCenter.vue:272-291）；后端核验 /v1/alerts 仅 acknowledge/resolve/delete/statistics，无 severity 升级端点 ✓
  - ② 证照预警：taskInfo 构造即丢弃+假成功→warning 禁用（StoreCertificate.vue:819-838）；后端有 `StoreManagementTaskController`（/v1/store-management/tasks：查询/完成/批量完成/跳转）但无创建端点→禁用正确 ✓
  - ③ 通知：3 条假通知删除→接真实 API（新增 `api/notification.ts`：GET /notifications、PUT /{id}/read、PUT /read-all，与 `NotificationController.java:435-508` 逐一匹配）；实体契约 `isRead===1`（`Notification.java` isRead Integer 0/1、id=notification_id）✓
  - ④ 域权限：假「已同步」成功文案移除，改「预览仅本地生效」info（useDomainPermission.ts:601-645）；后端 PermissionTemplateController 无菜单覆盖保存端点 ✓
  - ⑤ 处置记录：假记录删除→按 detail 真实字段构建（approverName/approvalTime/rejectReason/status；types/asset.ts:444-448；formatTime L430）（AssetDisposal.vue:442-468）✓
  - ⑥ R1 复验：`append-to-body` 恢复纯布尔，TS2339(1358,8) 消除、无夹带改动，整卡 6 子项合并 PASS ✓
- **测试类型**：手工回归
- **当前状态**：PASS（2026-08-10 R1 复验通过转 PASS）
- **限制说明**（2026-08-10 更新：V-3 已闭环，依据 `docs/quality/p0-kl-031-rerun.md`）：
  - V-3 —— 通知已读/全部已读真实登录态运行期联调 —— **已闭环（2026-08-10）**：KL-031 重跑活体 PASS（GET /notifications 200 code=0、PUT /{id}/read is_read 0→1+read_time 落库、PUT /read-all 全 1、造数 0 残留；DEF-3 实体-表漂移经 P0-MOCK-001 修复 + QA 复验 PASS 后本重跑确认关闭）；残留 R1 浏览器通知面板 UI 交互待发布前浏览器冒烟补验（验证缺口，非缺陷）。
  - O-5 —— 批次内 `:teleported`/`lock-scroll` 全局替换（约 15 文件/50+ 处）为超范围 UI 行为变更（低风险），行为回归放回归阶段观察（仍开放）。

---

### REG-EXPORT-002

- **测试编号**：REG-EXPORT-002（REG-EXPORT-001 保留给 P1-EXPORT-001 薪资导出——Sprint-2 已并入 REG-HR-001 验收，故本条目从 002 起编）
- **模块**：多模块导入/导出（HRTraining / TraceabilityQuality / OrderQuery / AssetDepreciation / AssetInventory / FoodTraceCode / DeviceAlerts / warehouse·store-ops StoreInventory + 附加 HRHealthCertificate）
- **关联任务**：P1-EXPORT-002（Sprint-3B-2 EXPORT 治理）
- **业务场景**：8 页导入/导出真实化（7 卡页 + 2 附加页真实 CSV）
- **测试目的**：防止导出假文件/导入假成功回归 —— 导出必须生成含真实数据的可解析 CSV，导入必须真实落库或禁用。
- **测试步骤**：
  1. 逐页执行导出，下载 CSV 文件并打开解析（检查 \ufeff BOM/引号转义/列映射/数据来源 tableData）
  2. 培训页：导入真实 xlsx → 检查 `trainingApi.importCourses` 请求与落库（后端 `TrainingController.java:263 POST /import`）
  3. 质检页：尝试导入，观察禁用+tooltip（后端 /v1/quality 确无导入端点）
  4. 全仓 grep 占位 Blob 残留（`new Blob(['占位'])`）
- **预期结果**：
  - 真实 CSV：7+2 页全部基于 tableData 真实生成（\ufeff BOM + 引号转义 + Blob 下载，无占位内容），文件可解析且含真实数据；AssetDepreciation 含 fenToYuan 金额转换
  - 导入真实/禁用：培训导入真实解析落库（importCourses→createCourse→save）；质检导入禁用+tooltip
  - 无残留：grep 无 `new Blob(['占位'])` 残留（仅 StoreRecruitment 既有真实导出 1 处）
- **测试类型**：手工回归
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**（2026-08-11 更新：V-2 已闭环，依据 `docs/quality/p0-defab-kl030-rerun.md`）：
  - V-2 —— CSV 文件内容未做运行时解析验证 —— **已闭环（2026-08-11）**：KL-030 冒烟重跑浏览器级实测 PASS（OrderQuery 13 列 PASS `browser-smoke-sprint3.md` §一.2；折旧导出 9 列 + fenToYuan 200.00 断言 + 真实数据行；告警导出 9 列 + 告警内容断言 + 真实数据行；造数 0 残留），KL-030 满足关闭条件。
  - O-4 —— HRTraining 报名员工选项仍为 4 个硬编码员工（EMP001~004，Sprint-2 HR-002 遗留、非本批引入），报名真实落库但员工身份为伪造值，建议后续接线真实员工 API（仍开放，已拆卡 P1-DATA-012）。

---

## Sprint-3B-3 回归（前端域 4 卡：P1-API / P1-DATA）

> 验收依据：`docs/quality/sprint-3b3-qa-report.md`（2026-08-10，独立验收 4/4：PASS 4 / PASS_WITH_LIMITATION 0 / FAIL 0，基线 git 提交 dd33ee3）。
> 验收结论均为 PASS，无限制项；验证缺口（L-1~L-5：运行时故障注入未做、正文生成成功路径运行时不可达、供应商运行库验证未做、既有文案未核验、仓库级 vue-tsc 既有错误）均为代码走查级证据或非本批引入，逐条确认不阻断发布，登记待回归阶段补验。

### REG-API-001

- **测试编号**：REG-API-001
- **模块**：前端-召回统计（RecallManagement.vue + types/traceability/recall.ts）
- **关联任务**：P1-API-002（Sprint-3B-3 前端域）
- **业务场景**：召回统计卡加载失败错误态与「已分析批次」布尔伪造
- **测试目的**：防止召回统计静默失败与布尔伪造回归 —— 失败必须显示错误态（非 0 伪装），「已分析批次」不得再用 `analyzeResult ? 1 : 0` 布尔伪造。
- **测试步骤**：
  1. 构造统计接口失败（GET /v1/recalls/statistics 报错/超时），观察统计卡与提示
  2. 加载成功后观察统计卡数值
  3. 检查页面统计卡数量与 grid 列数（应为 3 卡 / 3 列）
  4. 对齐核验：后端 `RecallStatisticsVO.java:12-22` 字段与前端类型、`RecallController.java:73-78` 端点、`RecallServiceImpl.java:117-145` 真实 DB count
- **预期结果**：
  - 失败错误态：catch 分支 `globalStats=null; statsError=true; ElMessage.error('召回统计加载失败')`；失败时 3 卡统一 `value: '--'`（非 0 伪装）
  - 无布尔伪造：「已分析批次」卡片（原 `analyzeResult ? 1 : 0`）整卡移除，grid 4→3 列
  - 真实计数：后端 RecallServiceImpl 真实 DB count（LambdaQueryWrapper 计数，非 stub）；前端 `api/traceability/recall.ts:43-45` 直连无 Mock
- **测试类型**：数据一致性检查
- **当前状态**：PASS
- **限制说明**：L-1 —— 未做运行时 API 故障注入（后端未拉起），错误路径验证为代码走查 + diff 证据、无隐藏分支；L-5 —— 仓库级 vue-tsc 243 个既有错误非本批引入。观察项：加载中卡片短暂显示 0（非错误态，既有模式，未列入本卡范围）。均不阻断发布。

---

### REG-API-002

- **测试编号**：REG-API-002
- **模块**：前端-资产维护（AssetMaintenance.vue + types/asset.ts）
- **关联任务**：P1-API-004（Sprint-3B-3 前端域）
- **业务场景**：维护记录编辑保存（后端无 update 端点）
- **测试目的**：防止编辑假操作回归 —— 后端无更新接口时，编辑必须阻止关闭并明确提示未保存（不得弹 info 后继续关闭+刷新，造成假操作/数据消失）。
- **测试步骤**：
  1. 打开维护记录编辑，修改内容后保存
  2. 观察提示与弹窗行为（应 warning + 不关闭、不落库、不刷新）
  3. 检查 `expectedCompleteDate` 字段映射（不得用 `completedTime` 错位填充）
  4. 后端核验：`AssetMaintenanceController` 是否存在 update 端点；`api/asset/maintenance.ts` update 实现
- **预期结果**：
  - 阻止关闭：编辑分支 `ElMessage.warning('维护记录暂不支持编辑保存：后端无更新接口，本次修改未保存，可取消放弃修改') + return`，`dialogVisible.value=false` 与 `refresh()` 均在分支之后不执行 —— 无假成功、不落库、不刷新
  - 字段映射：`expectedCompleteDate: detail.expectedCompleteDate?.slice(0,10) || ''`（L299-301）；`types/asset.ts:398-405` 新增 `expectedCompleteDate?: string` 及口径注释
  - 后端核验：`AssetMaintenanceController`（全 130 行）仅 page/getById/start/complete/records，无 update 端点；`api/asset/maintenance.ts:158-160` update 为 throw 占位 —— 阻止关闭为正确处置
- **测试类型**：手工回归
- **当前状态**：PASS
- **限制说明**：L-1 —— 无运行时故障注入，错误路径为代码走查证据。观察项：编辑功能仍不可用（后端无接口），已从「假成功+数据消失」转为「明确提示未保存」，符合任务卡验收标准（阻止关闭并提示），属能力缺口如实声明。不阻断发布。

---

### REG-API-003

- **测试编号**：REG-API-003
- **模块**：前端-合同创建（ContractCreateDialog.vue）
- **关联任务**：P1-API-005（Sprint-3B-3 前端域；承接 REG-HR-008 限制说明闭环点）
- **业务场景**：合同创建后正文生成成功文案条件化
- **测试目的**：防止成功文案误导回归 —— 正文生成失败或无模板时不得再弹「已生成正式正文」成功文案，仅 warning；闭合 REG-HR-008 记录的 ContractCreateDialog 文案矛盾限制。
- **测试步骤**：
  1. 无模板创建合同：观察提示（应「合同起草完成，可提交审批」，无正文成功文案）
  2. 有模板且正文生成失败（generateDocument stub 调用即 throw）：观察提示（应仅 warning，无成功文案）
  3. 有模板且正文生成成功（bodyGenerated=true）：观察原文案（当前运行时不可达，代码级核验）
  4. 检查两条成功文案是否在 `await contractApi.create(...)` resolve 之后
  5. 后端核验：全后端 Java grep `generateDocument`；`api/hr/contract.ts` 实现
- **预期结果**：
  - 条件化：`bodyGenerated` 标记三路径 —— 模板+成功→「合同起草完成，已生成正式正文，可提交审批」；模板+失败→仅 warning「合同已创建，但正文生成失败…」（无成功文案）；无模板→「合同起草完成，可提交审批」（无正文成功文案）
  - 时序正确：两条成功文案均在 `await contractApi.create(...)`（L363）resolve 之后（L381-385），`emit('success')`+`handleClose()` 在后
  - 后端核验：全后端 Java grep `generateDocument` = 0 命中；`api/hr/contract.ts:201-204` 为 throw stub —— 失败路径为当前真实路径
- **测试类型**：手工回归
- **当前状态**：PASS
- **限制说明**：L-2 —— 成功路径（bodyGenerated=true）运行时不可达（generateDocument 为 stub，调用即 throw），成功文案条件化仅代码级验证，待后端端点就绪后由回归补测；L-4 —— warning 文案「请稍后在详情页手动生成」对应能力未核验（既有文案，挂 P1-API-001 既有范畴）。均不阻断发布。

---

### REG-DATA-015

- **测试编号**：REG-DATA-015
- **模块**：仓库-出库（InventoryOutbound.vue + api/inventory/inventory-outbound.ts）
- **关联任务**：P1-DATA-011（Sprint-3B-3 前端域）
- **业务场景**：出库单目标供应商下拉与提交链路
- **测试目的**：防止假供应商回归 —— 目标供应商必须来自 `supplierApi.getEnabledList()` 真实接口，不得再出现 双汇/益海嘉里/蒙牛/SUP001~003 硬编码假供应商。
- **测试步骤**：
  1. 打开出库单，展开目标供应商下拉，检查选项来源
  2. 选择真实供应商并提交，检查提交载荷 targetId/targetName 流转
  3. 构造加载失败，观察下拉（应空列表 + error 提示，无假成功）
  4. 全仓 grep 假供应商名（双汇/益海嘉里/蒙牛/SUP001~003），确认本文件 0 残留
  5. 后端核验：`SupplierController.java` GET /list + status=1 过滤；`InventoryOutboundCreateDTO.targetId` 类型
- **预期结果**：
  - 数据源：`supplierApi.getEnabledList()`（GET /v1/suppliers/list?status=1，`SupplierController.java:52-64` 存在 + status 过滤），map 为 `{supplierId, supplierName}`；onMounted 注册 `loadTargetSupplierOptions()`
  - 无残留：InventoryOutbound.vue 0 残留（3 家硬编码删除，全仓其余命中为他文件注释/知识库文章，非本文件选项数据）
  - 提交链路：`handleTargetChange`（L78-92）选中真实项→targetId/targetName 入表单→`toCreateDTO`（inventory-outbound.ts:165-180）透传→后端 `InventoryOutboundCreateDTO.targetId`（String，类型兼容）
  - 失败处理：catch → 空列表 + `ElMessage.error`；`el-option-group` v-if 空列表不渲染（L573-575）
- **测试类型**：数据一致性检查
- **当前状态**：PASS
- **限制说明**：L-3 —— 未做运行库验证（供应商列表真实内容/权限 `isAuthenticated` 行为），仅静态核验接口存在与链路；观察项：`loadTargetStoreOptions` catch 仍静默（L72-74）为既有行为、属 P1-API-001 范畴，非本卡引入。均不阻断发布。

---

## Sprint-3C 回归（P2-CLEAN 代码卫生）

> 验收依据：`docs/quality/sprint-3c-qa-report.md`（2026-08-10，独立验收 2/2：PASS 2 / FAIL 0，无 PASS_WITH_LIMITATION 项；红线 BLOCKED_PRODUCT_RULE：PD-001/002/003 未触碰、未解除；观察项 O-1~O-3 登记，O-2 已在本文件 REG-FIN-001「限制说明」闭环更新）。
> 新增依据：仅基于 QA 已验收 PASS 结论（P2-CLEAN-005、P2-CLEAN-006），无推测内容。

### REG-CLEAN-001

- **测试编号**：REG-CLEAN-001
- **模块**：代码卫生-过时注释（frontend/src/api/finance/tax-calculation.ts:102 / backend TrainingController.java:249 / TrainingService.java:104）
- **关联任务**：P2-CLEAN-005（Sprint-3C 代码卫生，QA PASS 2026-08-10）
- **业务场景**：缴税重复缴纳、培训重复报名的过时注释口径
- **测试目的**：防止过时注释误导回归 —— 注释必须与后端实现一致（事实口径），不得再现「幂等拒绝 / 自动跳过 / 防重复缴纳 / 已存在报名」等过时措辞，且不得夹带业务规则断言（BLOCKED_PRODUCT_RULE 不触发）。
- **测试步骤**：
  1. 全仓 grep backend/src + frontend/src：「幂等拒绝|自动跳过|防重复缴纳|已存在报名」→ 必须 0 残留（双工具交叉：内置 grep + PowerShell Select-String）
  2. 核对 3 处注释口径：tax-calculation.ts:102「重复缴纳规则待产品裁决（PD-001），后端当前不拦截；防重由前端单飞守卫承担」；TrainingController.java:249 @Operation description 与 TrainingService.java:104 接口 javadoc「重复报名规则待产品裁决 PD-002，后端当前不拦截」
  3. 检查无新业务规则断言（PD-001/002/003 未触碰、未解除）
- **预期结果**：
  - 无残留：grep 0 匹配（代码层）
  - 注释一致：3 处注释与实现一致（TrainingServiceImpl.createStudyRecords 纯 insert 无重复检查；TaxRecordServiceImpl 口径为 BLOCKED_PRODUCT_RULE 已移除）
  - 无规则断言：无任何新业务规则实现
- **测试类型**：数据一致性检查
- **当前状态**：PASS

---

### REG-CLEAN-002

- **测试编号**：REG-CLEAN-002
- **模块**：代码卫生-死代码（AccountBalanceService 接口 + AccountBalanceServiceImpl 实现）
- **关联任务**：P2-CLEAN-006（Sprint-3C 代码卫生，QA PASS 2026-08-10）
- **业务场景**：closeProfitAndLoss / closeYearProfit return true 假实现删除
- **测试目的**：防止死代码回归 —— 两假实现（接口声明 2 处 + 实现 2 处）不得再现（含接口声明）；不得新增结转/损益逻辑；REG-FIN-001 真实聚合不得破坏；编译必须通过。
- **测试步骤**：
  1. 全仓 grep 代码层（排除 node_modules/dist/target/logs/二进制）：「closeProfitAndLoss|closeYearProfit」→ 必须 0 引用（含接口声明；治理文档历史描述允许存在）
  2. 核对 AccountBalanceService 接口仅保留 recalculateAllBalances / getByCategoryAndPeriod；AccountBalanceServiceImpl 无 return true 假实现残留
  3. 核对无结转/损益逻辑新增（期初结转注释仍为「BLOCKED_PRODUCT_RULE…未实现结转」口径）
  4. 核对 AccountBalanceController 仍调用 accountBalanceService.recalculateAllBalances()（REG-FIN-001 聚合链路不破坏）
  5. 编译复跑：mvn -DskipTests compile → EXIT=0
- **预期结果**：
  - 无引用：代码层 grep 0 匹配
  - 聚合完好：recalculateAllBalances（@Override + @Transactional(rollbackFor)）与 getByCategoryAndPeriod 存在且逻辑不变（聚合方法体与 Sprint-2 基线逐行一致）
  - 编译通过：MVN_EXIT=0
- **测试类型**：数据一致性检查
- **当前状态**：PASS

---

## KL-048 修复闭环回归（2026-08-11）

> 验收依据：`docs/quality/kl-048-r1-qa-report.md`（2026-08-11，QA 独立复验 **KL-048 = PASS_WITH_LIMITATION**：验收目标 4/5 完全 PASS + 编译项 KL-048 三文件本身 PASS；限制=工作区 device 模块编译破坏，非 KL-048 引入，已由 **P0-DEVICE-001（DEF-B）** 修复中，随冒烟门禁 FAIL 登记）。
> 新增依据：仅基于 QA 已验收结论（KL-048 PASS_WITH_LIMITATION）新增 REG-DATA-018，无推测内容。

### REG-DATA-016

- **测试编号**：REG-DATA-016
- **模块**：营销-会员统计/运营趋势（MemberOverview / LiveMonitor / DecisionBoard / OperationsReport）
- **关联任务**：P0-DATA-009（DEF 修复批次，QA 复验 `p0-data-009-r1-qa-report.md` = PASS_WITH_LIMITATION）
- **业务场景**：overview 真实聚合可用（DEF-1）+ 趋势三接口错误透传无静默降级（DEF-2）
- **测试目的**：防止「overview 恒 500 / 趋势静默空」回归 —— 会员概览必须来自真实聚合，趋势接口失败必须透传真实错误、不得静默降级。
- **测试步骤**：
  1. admin 登录（/me 权限含 `*`），GET /v1/members/stats/overview → 200 code=0，与 DB 聚合口径一致（基线/造数 8/9 字段精确匹配）
  2. GET /v1/operations/live-monitor/trend?dimension=7d → 200 code=0，无 500、无静默降级
  3. GET /v1/operations/decision-board/revenue-trend?period=7d → 200 code=0，无 500、无静默降级
  4. GET /v1/operations-reports/kpi-summary?startDate&endDate → 200 code=0，无 500、无静默降级
  5. 构造数据权限无权限分支（空列表 → NO_ACCESS_STORE_ID=-1），核对最严格隔离保持
- **预期结果**：
  - overview 真实聚合：字段与 DB 一致，无硬编码 0、无 500
  - 趋势三接口：HTTP 200 + code=0，错误透传真实错误码/message，无成功文案复用
  - 无静默降级：无「恒空数组伪装正常」路径（DEF-2 修复维持）
- **测试类型**：数据一致性检查
- **当前状态**：PASS（2026-08-11 更新，原 PASS_WITH_LIMITATION 唯一限制 KL-048 已关闭）
- **限制说明**（2026-08-11 更新：KL-048 子项关闭，依据 `docs/quality/kl-048-r1-qa-report.md`）：
  - **KL-048（admin 趋势空 + kpi-summary?storeIds=1 contains NPE）——已修复（2026-08-11，QA 复验 `kl-048-r1-qa-report.md` = PASS_WITH_LIMITATION）**，本限制关闭。修复内容：3 文件 4 处 null 前置分支（`LiveMonitorServiceImpl.resolveStoreFilter` L283-287 / `DecisionBoardServiceImpl.resolveStoreFilter` L355-359 / `OperationsReportServiceImpl.getAuthorizedStoreIds` L66-70【NPE 防御：null → 返回 requestedStoreIds，避免 L78 contains 空指针】/ `OperationsReportServiceImpl.resolveStoreFilterFromAuthorized` L458-461，全部 null=不限制语义，`DataPermissionServiceImpl.java:362-367` 注释依据）；非 null 分支零改动（isEmpty→-1 / size==1→ID / 多→null / 异常→-1 / contains 循环）。活体证据：live-monitor/trend revenue=[3000.0]、decision-board/revenue-trend 非空、kpi-summary currentValue=3000.00 与 DB SUM(final_amount)=300000 分精确一致、**kpi-summary?storeIds=1 → 200 code=0 无 500 无 NPE**（storeIds=1 返回 0 属正确过滤：唯一订单 store_id=NULL 无门店归属）。
  - **编译限制（非本条目行为限制）**：KL-048 三文件无编译错误（javac 全量错误列表不含三文件）；工作区整体 `mvn compile` 失败 = device 模块既有改动（DeviceController/DeviceServiceImpl/PrintTaskServiceImpl，非 KL-048 引入）——已由 **P0-DEVICE-001（DEF-B）** 承接修复中，随冒烟门禁 FAIL 登记，非 REG-DATA-016 限制。
  - 保留观察（非限制，不阻断）：① 造数订单 DEV-L206-TEST-001 store_id=NULL（数据质量问题，提请数据侧补充门店归属，QA 缺口 2）；② 非 admin 角色（店长/区域经理）活体未覆盖、storeIds 多值未活体（QA 缺口 3/4，代码级核验通过，验证缺口类）。

---

### REG-DATA-018

- **测试编号**：REG-DATA-018
- **模块**：运营-趋势（LiveMonitorServiceImpl / DecisionBoardServiceImpl / OperationsReportServiceImpl）
- **关联任务**：KL-048（O-DEF2-1，来源 P0-DATA-009 观察登记；QA 复验 `docs/quality/kl-048-r1-qa-report.md` = PASS_WITH_LIMITATION，2026-08-11）
- **业务场景**：运营趋势 admin 可见 + null 防御 —— admin 登录后趋势三接口非空、kpi-summary 显式传 storeIds 无 NPE（500 消除）
- **测试目的**：防止「resolveStoreFilter* 将 getAccessibleStoreIds 返回 null（=不限制）误判为 NO_ACCESS_STORE_ID=-1 → 趋势恒空」与「getAuthorizedStoreIds 对 null 未防御 → contains NPE → 500」回归 —— null 必须解析为不限制，仅空列表才走 -1 最严格隔离。
- **测试步骤**：
  1. admin 登录（/me 200 code=0，roles=[admin,user]，permissions 含 `*`），GET /v1/operations/live-monitor/trend?dimension=7d → 200 code=0，data.revenue/dates/orders 非空（修复前为空数组）
  2. GET /v1/operations/decision-board/revenue-trend?period=7d → 200 code=0，data.revenue/cost/dates/profit 非空
  3. GET /v1/operations-reports/kpi-summary?startDate=...&endDate=... → 200 code=0，kpiCards[0].currentValueFen/currentValue 与 DB（SUM(final_amount)）精确一致
  4. GET /v1/operations-reports/kpi-summary?startDate=...&endDate=...&storeIds=1 → **200 code=0，无 500、无 NPE**，按门店 1 正确过滤（0 值=该门店无订单归属，非缺陷）
  5. 代码级核验 4 处 null 前置分支存在（LiveMonitor L283-287 / DecisionBoard L355-359 / OperationsReport L66-70、L458-461）；非 null 分支零改动（isEmpty→-1 / size==1→ID / 多→null / 异常→-1 / contains 循环）
  6. 语义闭环：DataPermissionServiceImpl.java:362-367「管理员直接返回 null（表示不限制）」+ OrderNewMapper.getDailyTrendByStore `<if test='storeId != null'>` SQL 条件（null 时不设 store_id 条件）
- **预期结果**：
  - admin 趋势可见：三接口 200 code=0 且数据非空，与 DB 口径一致
  - null 防御：storeIds=1 无 500、无 NPE（GlobalExceptionHandler 不再兜底）；仅空列表 → -1 最严格隔离保持
  - 非 null 分支零回归：isEmpty/size==1/多门店/异常/contains 分支逐行不变
  - 无业务规则新增：null=不限制有注释语义依据，BLOCKED_PRODUCT_RULE 不触发
- **测试类型**：数据一致性检查（活体 4 接口 + 代码级核验）
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**：
  - **编译限制（非本条目行为限制）**：QA 复验编译项=工作区 device 模块编译破坏（javac 全量错误仅 device 模块 3 处，非 KL-048 三文件；运行中实例 2026-08-10 23:49 启动成功证明修复代码可构建可运行）——已由 **P0-DEVICE-001（DEF-B）** 修复中，随冒烟门禁 FAIL 登记；发布构建依赖其闭环。
  - 验证缺口（代码级已核验，非 FAIL）：非 admin 角色（店长 size==1 → ID / 区域经理多门店 → null）路径未活体覆盖（QA 缺口 3）；admin + storeIds 多值（size>1 → null）未活体（QA 缺口 4）。
  - 观察项：唯一订单 DEV-L206-TEST-001 store_id=NULL（造数数据质量问题，非本条目缺陷，提请数据侧补充门店归属）。

---

## DEF-A/DEF-B 修复闭环回归（2026-08-11）

> 验收依据：QA 独立复验 `docs/quality/p0-asset-001-r1-qa-report.md`（DEF-A = PASS）+ `docs/quality/p0-device-001-r1-qa-report.md`（DEF-B = PASS，活体三接口 code=0）+ KL-030 冒烟重跑 `docs/quality/p0-defab-kl030-rerun.md`（regression 浏览器/接口级闭环，2026-08-11）。
> 新增依据：仅基于 QA 已验收 PASS 结论 + 本重跑实测，无推测内容。
> 造数/清理：BSSM3-R2- 前缀（device_id=6 / alert_id=2 / asset_id=2 / record_id=2），测后全量清理 0 残留；devices 表 developer 造数残留（id=2/3 QA-TEST-*）已物理清理（DELETE 2，QA-TEST-% 残留 0）。

### REG-ASSET-001

- **测试编号**：REG-ASSET-001
- **模块**：资产-折旧（AssetDepreciation / AssetInventory / AssetLedger，asset 域共享分块 inventory.ts）
- **关联任务**：P0-ASSET-001（DEF-A，QA 复验 `p0-asset-001-r1-qa-report.md` = PASS，2026-08-11）
- **业务场景**：资产页恢复渲染（不白屏）+ 折旧导出 CSV 可用
- **测试目的**：防止「inventory.ts 注释未闭合吞 import → asset 分块崩溃 → 资产页全白屏」回归 —— 资产三页必须正常渲染，折旧导出必须生成含真实数据的可解析 CSV。
- **测试步骤**：
  1. 造数：asset_depreciation_records 1 行（period='2026-07'、this_period_depreciation=20000 分、accumulated=100000 分、net=1100000 分）+ asset_masters_enhanced 1 行（BSSM3-R2-ASSET-001）
  2. 打开 /asset/depreciation → 确认不白屏、列表含造数行、无 `InvStatus is not defined` 页面错误
  3. 点击「导出折旧表」→ 捕获下载 → 解析 CSV（BOM / 表头 9 列 / fenToYuan 断言 200.00 / 真实数据行）
  4. 打开 /asset/inventory-check、/asset/ledger → 确认不白屏（补验项 3，asset 分块不再崩溃）
  5. 清理造数 → 0 残留
- **预期结果**：
  - 页面渲染：折旧页 body 非空、「本月折旧额」列渲染 200.00（fenToYuan(20000 分)）；盘点页/台账页均非白屏；pageerror 0（无 InvStatus）
  - 导出：下载 `折旧表_*.csv`（250B），BOM=`\ufeff`，表头 9 列（资产编号/资产名称/折旧期间/本期折旧额(元)/累计折旧(元)/折旧后净值(元)/折旧方法/计算时间/操作人），数据行含 `"2026-07"` 与 `"200.00"`
  - API：GET /v1/asset/depreciation/page → HTTP 200
  - 清理：BSSM3-R2-% 0 残留
- **测试类型**：数据一致性检查（浏览器冒烟 + CSV 解析）
- **当前状态**：PASS（2026-08-11）
- **限制说明**：无（对照 `p0-asset-001-r1` §5 缺口：①浏览器渲染复验本重跑闭环 ②KL-030 折旧导出 CSV 闭环 ③前端全量 vue-tsc 136 错误/59 文件基线治理为既有中间态，建议独立立项，非本条限制）。

---

### REG-DEVICE-001

- **测试编号**：REG-DEVICE-001
- **模块**：设备-接口/告警（DeviceController / DeviceAlertController / DeviceAlerts 页）
- **关联任务**：P0-DEVICE-001（DEF-B，QA 复验 `p0-device-001-r1-qa-report.md` = PASS，活体三接口 code=0，2026-08-11）
- **业务场景**：设备存在时设备/告警接口可用（deviceType 字符串往返）+ 告警导出 CSV 可用
- **测试目的**：防止「Device.deviceType Integer vs devices.device_type varchar 实体-表漂移 → 设备存在时接口恒 500」回归 —— 非空设备数据下三接口必须 code=0，告警导出必须含真实数据行。
- **测试步骤**：
  1. 造数：devices 1 行（BSSM3-R2-DEV-001，device_type='PRINTER' 字符串、connection_type='3' 数字串规避残留漂移、store_id='1'、status='1'）+ device_alerts 1 行（BSSM3-R2-ALERT-OVERTEMP，alert_type=1、alert_level=2、alert_status=0）
  2. 打开 /device/alerts → 确认列表可用（设备行渲染、非白屏）、GET /v1/device-alerts/page 200
  3. 点击「导出告警记录」→ 捕获下载 → 解析 CSV（BOM / 表头 9 列 / 告警内容断言 / 真实数据行）
  4. 补验设备三接口（DEF-B 活体范围）：/v1/devices/page、/v1/devices/list、/v1/devices/type/PRINTER → 全 code=0、deviceType 字符串往返
  5. 清理造数 → 0 残留
- **预期结果**：
  - 接口可用：三接口 HTTP 200 + code=0；page total=1、list count=1、type/PRINTER code=0；返回 `"deviceType":"PRINTER"`（字符串，字段名不变）
  - 页面渲染：告警列表渲染设备名行，无 500 空表
  - 导出：下载 `设备告警_*.csv`（233B），BOM=`\ufeff`，表头 9 列（设备名称/告警类型/告警级别/告警内容/告警状态/触发时间/处理结果/处理时间/解决时间），数据行含 `"BSSM3-R2-ALERT-OVERTEMP"` 与 `"BSSM3-R2-ALERT-DEVICE"`
  - 清理：BSSM3-R2-% 0 残留
- **测试类型**：数据一致性检查（活体 API + 浏览器冒烟 + CSV 解析）
- **当前状态**：PASS（2026-08-11）
- **限制说明**：无（对照 `p0-device-001-r1`：KL-030 告警导出缺口本重跑闭环（缺口 1/2）；同型漂移登记（connectionType/storeId 类型、迁移文件列名）建议 audit 立项统一治理，非本条限制；KDS 设备类型展示归属待产品决策，观察项非限制）。

---

## Sprint-4 回归（首批：决策落地 4 项，2026-08-11）

> 验收依据：`docs/quality/sprint-4-backend-qa-report.md`（2026-08-11，后端 3 项独立验收：P0-FIN-002 / P0-HR-002 / P0-FIN-001 全 PASS，FAIL=0，无 -R）+ `docs/quality/sprint-4-frontend-qa-report.md`（2026-08-11，P0-HR-005 前端 8/8 PASS，FAIL=0）。
> 决策依据：PD-001~004 已决策（product-decision-backlog.md，2026-08-11 业务决策人解除 BLOCKED_PRODUCT_RULE）；实现均按回写验收标准执行，QA BLOCKED 审查通过（无 AI 自补业务规则）。
> 新增依据：仅基于 QA 已验收 PASS / PASS_WITH_LIMITATION 结论新增 4 条基线（REG-FIN-005 / REG-FIN-006 / REG-HR-012 / REG-HR-013），无推测内容。

### REG-FIN-005

- **测试编号**：REG-FIN-005
- **模块**：财务-缴税（TaxRecordServiceImpl / V20260811_001 迁移 / TaxRecordController）
- **关联任务**：P0-FIN-002（Sprint-4 首批，PD-001 缴税重复缴纳决策落地；QA 验收 PASS 2026-08-11）
- **业务场景**：同键重复缴税 → 幂等命中（不新增 + 返回已有记录 + 命中日志 + 唯一索引兜底）
- **测试目的**：防止缴税幂等拦截错误回归 —— 同键（税种+所属期+凭证号）重复提交必须命中幂等：不得新增第二条记录、不得静默覆盖/改动原记录；不同键必须正常新增；DB 唯一索引兜底。
- **测试步骤**：
  1. 同一缴税请求（同 taxType/taxPeriod/voucherNo）连续 POST /api/v1/finance/tax-records 两次
  2. 比较两次返回 id（应相同，SAME_ID=True）与 DB 计数（应=1）
  3. 查 sys_operation_logs 幂等命中日志（操作人/时间/幂等键/hitRecordId）
  4. 携带不同 taxAmount/description 重提同键，检查原记录字段（应零改动，create/update_time 不变）
  5. 不同键提交 → 正常新增（id 递增）
  6. pg_indexes 核验 uk_tax_record_idempotent；DB 直插同键 → 违反唯一约束
- **预期结果**：
  - 幂等正确：第 2 次返回 id 与第 1 次相同、DB 仅 1 条、命中日志含 hitRecordId、原记录零改动
  - 异键新增：不同键正常创建新记录
  - 兜底存在：唯一索引 uk_tax_record_idempotent(tax_type, tax_period, voucher_no) 生效
- **测试类型**：数据一致性检查（活体 HTTP + DB 直查）
- **当前状态**：PASS
- **限制说明**：无（边界语义保留：voucher_no 为空 → 幂等键不完整不适用幂等，为 PD-001 注记既有语义；观察项：幂等命中时 @OperationLog 切面另记一条「创建缴税记录」status=SUCCESS，由命中日志行补充说明，不影响审计真实性）。

---

### REG-FIN-006

- **测试编号**：REG-FIN-006
- **模块**：财务-总账（AccountBalanceController / AccountBalanceServiceImpl）
- **关联任务**：P0-FIN-001（Sprint-4 首批，PD-003 期初结转决策落地；QA 验收 PASS 2026-08-11）
- **业务场景**：期初人工录入 + 审计（录入/调整生效 + 联动重算 + 审计四要素 + 非授权 403 + 无自动结转）
- **测试目的**：防止期初处理错误回归 —— 期初必须由授权人员经 PUT /opening 人工录入/调整且生效、期末联动重算、审计（操作人/时间/原因）落库；非授权 403；系统不得自动结转（新行期初 0、既有行期初零覆盖）。
- **测试步骤**：
  1. 授权调用 PUT /api/v1/finance/account-balance/opening（subjectCode/period/openingAmount + adjustReason）→ 生效（beginDebit 变化、endDebit 联动重算）
  2. 再次调整 → beginDebit/endDebit 均更新、update_time 变更、create_time 保留
  3. 查 sys_operation_logs 审计（operator/operation_time/request_params 含 adjustReason）
  4. 缺 adjustReason 提交 → 400（原因必填）；负数 → 400（均落审计）
  5. POST /refresh 重算 → 新行 begin=0.00、既有行期初不被覆盖（update_time 不变）
  6. 非授权角色（employee/0 权限）调 PUT /opening、GET /detail → 403
- **预期结果**：
  - 录入生效：期初/期末联动重算正确，审计四要素（人/时间/原因/内容）落库
  - 校验：原因必填、金额非负（400 且落审计）
  - 无自动结转：新行期初 0、既有行期初零覆盖（REG-RULE-003 防回归点）
  - 权限：PUT /opening 需 finance:edit 或 `*`，非授权 403
- **测试类型**：数据一致性检查（活体 HTTP + DB 直查 + 审计核对）
- **当前状态**：PASS
- **限制说明**：无（QA 观察项登记不阻断：O-1 400 拒绝请求审计记 status=SUCCESS（审计四要素完整）；O-2 refresh 仅 upsert「有凭证流水的科目期间」，流水全删后不清零 current（当前无删除流水场景）；O-3 明细按 create_time 排序非凭证日期）。

---

### REG-HR-012

- **测试编号**：REG-HR-012
- **模块**：HR-培训（TrainingServiceImpl / TrainingController / training_study_record）
- **关联任务**：P0-HR-002（Sprint-4 首批，PD-002 报名唯一性决策落地；QA 验收 PASS 2026-08-11，含 2 项限制注明）
- **业务场景**：同一员工重复报名同一课程 → 拒绝（已报名提示 + DB 无第二条 + 原状态保留）
- **测试目的**：防止报名唯一性回归 —— 同键（员工+课程，training_study_record 无期次字段）重复报名必须拒绝：返回「已报名」提示、不生成第二条记录、原报名记录零改动；不同课程/员工正常报名；软删记录可重报。
- **测试步骤**：
  1. 同一员工（employeeId）对同一课程（courseId）POST /api/v1/hr/training/study-records 两次
  2. 第 1 次 → {created:1, duplicated:0}；第 2 次 → {created:0, duplicated:1} + message「报名成功0人，1人已报名（重复报名已拒绝，未创建新记录）」
  3. DB：GROUP BY employee_id, course_id HAVING COUNT(*)>1 → 0 行；该键记录仅 1 条
  4. 重复报名后检查原记录 create_time/update_time 不变
  5. 同员工报第 2 门课程 → {created:1, duplicated:0}；混合批次（1 新 + 1 重）→ 计数正确
  6. 软删记录（deleted=1）后再报名 → {created:1, duplicated:0}（@TableLogic 生效）
- **预期结果**：
  - 同键拒绝：{created:0, duplicated:1} + 已报名提示；DB 无第二条；原状态保留（字段零变化）
  - 异键正常：不同课程/员工 created:1
  - 软删可重报：查重只统计未删记录
- **测试类型**：数据一致性检查（活体 HTTP + DB 直查）
- **当前状态**：PASS_WITH_LIMITATION
- **限制说明**（限制经逐条确认**不阻断发布**）：
  - **限制 1（并发竞态）**：training_study_record 无 (employee_id, course_id) DB 唯一索引（pg_indexes 确认），服务层 selectCount 查重存在并发双提交竞态窗口（低概率）——建议后续批次补部分唯一索引或并发补偿（QA 验证缺口 1，登记观察）。
  - **限制 2（期次键差异）**：决策键=员工+课程+期次，实现键=员工+课程（schema 无期次字段，19 列确认）——差异已在代码注释/javadoc 透明记录（TrainingServiceImpl.java:395-399、TrainingController.java:248-251），未自行编造期次逻辑；若未来课程分期次报名需加字段+迁移+改键（登记观察项）。

---

### REG-HR-013

- **测试编号**：REG-HR-013
- **模块**：HR-智能模块（router/index.ts / modules/hr/menu.ts / guards.ts / dist 产物）
- **关联任务**：P0-HR-005（Sprint-4 首批，PD-004 HR 智能 3 页面下线决策执行；QA 验收 PASS 2026-08-11，8/8）
- **业务场景**：HR 智能 3 页面（员工画像 / 知识库智能 / 合同智能看板）下线 → 菜单/路由无入口 + URL 直达拦截 + dist 无产物
- **测试目的**：防止已下线页面入口回归 —— 3 页面必须保持无菜单入口、无路由注册、URL 直达不渲染（不展示演示数据）、构建产物无 3 页面 chunk。
- **测试步骤**：
  1. 全库 grep 3 个 path（/hr/knowledge-intelligence、/hr/contract-dashboard、/hr/employee-intelligence）于路由表/菜单项 → 0 匹配（仅注释命中）
  2. 检查 hr/menu.ts 无 3 菜单入口；全 frontend/src 无其他入口链接（组件名/中文名双向 grep，页面自身/API/权限码除外）
  3. URL 直达核验（guards.ts 守卫链）：未匹配路由 → 未登录跳 /login（带 redirect）；已登录非 admin → 无 meta.domain 且不在白名单 → handleAccessDenied 拦截不渲染；已登录 admin → 无匹配组件空白不渲染（均不展示演示数据；无全局 catch-all 为既存状态）
  4. npm run build → EXIT=0；dist 全量扫描（EmployeeIntelligence|KnowledgeIntelligence|ContractDashboard|employee-intelligence|knowledge-intelligence|contract-intelligence|contract-dashboard）→ 0 匹配
  5. 页面文件保留为死代码（无死引用构建错误）；死权限码（hr:knowledge-intelligence:view / hr:employee-intelligence:view）保留但无 UI 触发点
- **预期结果**：
  - 无入口：路由表/菜单 0 匹配、入口链接 0 残留
  - URL 拦截：三态（未登录/非 admin/admin）均不渲染页面与演示数据
  - 构建干净：build EXIT=0、dist 无 3 页面 chunk
- **测试类型**：手工回归（代码级 grep/diff + 构建产物扫描）
- **当前状态**：PASS
- **限制说明**：无（观察项不阻断：R1 admin URL 直达显示空白页（无 404 提示页，无 catch-all 为既存状态，不渲染演示数据目标达成）；R2 死代码保留（3 页面 + 3 API + 2 组件 + 1 composable + types，符合 PD-004「保留待评估」口径）；R3 2 个死权限码待后续清理；R6 vue-tsc 136 处存量错误非本任务引入；验证缺口：浏览器端到端 URL 直达断言待发布前冒烟补验）。

---

## REV 回归基线更新（发布后独立治理编排，2026-08-16：RM-B2 / OIC-BE）

> 背景：后端窗口解锁（2026-08-16 12:55）后，RM-B2 轨与 OIC-BE 轨后端改动进入 REV 流程（OIC-BE 指令规则 6：后端改动必须更新/新增 regression 基线）。
> 编号方式（regression 决策说明）：OIC-BE 轨 `oic-be-task-board.md` §五 已预分配 REG-ETM-001/002/003、REG-KL055（按批次顺序）；RM-B2 实施卡 1 来源任务号为 **ETM-106**，为避免与 OIC-BE 预留序号冲突并保持可追溯，采用「基线编号 = 来源任务号」约定（同 REG-KL055 模式）→ **REG-ETM-106**。
> 转正依据（维护规则 5：仅基于已验收结论，无推测）：RM-B2 实施卡 1 QA 独立验收 **PASS**（`docs/quality/rm-b2-impl1-qa-report.md`，验收标准 ①~⑤ 全过 + G-3 生产独立实测通过，无 FAIL、无 -R1）。
> 候选登记 → 转正时序：2026-08-16 报告未产出时按维护规则 5（禁止推测）仅登记候选、不转正；`docs/quality/oicbe-b1-qa-report.md` 产出（2026-08-16，三卡 QA 独立验收 PASS、无 FAIL、无 -R1，卡 3 附限制 L-1）后，本批 4 条目（REG-ETM-001/002/003、REG-KL055）**转正**为正式基线（详见下方「OIC-BE Batch 1 REV 转正（2026-08-16）」）。

### REG-ETM-106（正式转正 + 2026-08-18 实施卡 3 索引落库断言扩展）

- **测试编号**：REG-ETM-106
- **模块**：仓库-库存（Inventory / inventory 表，ETM-106 补列落库）
- **关联任务**：RM-B2-实施卡1（ETM-106；来源 RM-B1-001 方案 `docs/quality/rm-b1-001-inventory-alignment-plan.md` + QA 独立验收 PASS 2026-08-16 `docs/quality/rm-b2-impl1-qa-report.md`，含 G-3 生产独立实测）+ **RM-B2-实施卡3（idx_inventory_batch_no 索引落库；QA 独立验收 PASS 2026-08-18 `docs/quality/rm-b2-impl3-qa-report.md`，六项验收全过、无 FAIL、无 -R1、无 BLOCKED_PRODUCT_RULE）**
- **业务场景**：新环境 Flyway 全量重建后 inventory 11 漂移列存在（实体↔migration↔真实表三向一致）+ **idx_inventory_batch_no 索引随 V20260817.001 落库（幂等覆盖 OBS-4，存在且唯一）** + WAREHOUSE/STORE 收货确认链路无 500 + selectInventoryPage/batchNo 筛选查询面 + inventory_transactions 写入核对
- **测试目的**：防止「inventory 漂移列未沉淀 migration → 新环境重建缺列 → 查询/收货确认链路 500」与「实体-表再次漂移」回归 —— migration 链最终态必须与实体 24 映射列、真实表物料口径 24 列三向一致；收货确认 INSERT/UPDATE/STORE 分支不得 500；查询面无 500；事务流水真实写入。
- **测试步骤**：
  1. 新环境按 migration 链全量重建（V1.0.0.100 → V20260717_003 → V20260717_020 → V20260730_001 → V20260816_001），Flyway validate 通过、189/189 success；flyway_schema_history 断言 version=20260816.001 success=t、checksum 与工作树文件一致（QA 独立 ChecksumCalculator 计算 = -1564733106）
  2. information_schema 只读断言 inventory 11 列存在且类型/默认值匹配：material_name varchar(200) / specification varchar(200) / location_id bigint / locked_quantity numeric(12,2) DEFAULT 0 / batch_no varchar(100) / production_date date / expiry_date date / unit_cost bigint / total_cost bigint / min_safe_qty numeric(12,2) / max_stock_qty numeric(12,2)；pg_description 断言 11/11 COMMENT 非空
  3. 三向核对：Inventory.java 24 映射列 ↔ migration 链最终态 ↔ 真实表 35 列，无新漂移
  4. WAREHOUSE 收货确认：INSERT 路径（新到货）与 UPDATE 路径（追加收货）各 1 单 → HTTP 200，inventory 行字段正确（current_stock / batch_no / unit_cost / total_cost / locked_quantity=0.00 / version 乐观锁递增）；STORE 分支 1 单（status=1）无回归、store_inventory 不污染 inventory
  5. inventory_transactions 写入核对：receipt_confirmation 流水（reference_type / reference_no，before/after 余额正确，unit_cost / total_cost 透传）
  6. 查询面：GET /api/v1/inventory?warehouseId=1 → HTTP 200 无 500，records 含 11 列；GET /api/v1/inventory?batchNo={batchNo} → HTTP 200 total=1（idx_inventory_batch_no 索引路径可用）
  7. **索引落库断言（2026-08-18 实施卡 3 扩展，基于 `rm-b2-impl3-qa-report.md` 已验收结论）**：flyway_schema_history 只读断言 **rank 193 = version 20260817.001**（description=add inventory batch no index）success=t（installed 2026-08-17 23:49:58）、checksum **-916957038**（QA 独立 ChecksumCalculator 重算 MATCH，3 对照样本先行验证管线）；pg_indexes 只读断言 idx_inventory_batch_no 存在（btree on batch_no）且唯一（batch_no 列索引 count=1 无重复——CREATE INDEX IF NOT EXISTS 幂等 no-op）；pg_index×pg_class 只读断言 indisvalid=t（有效、非唯一、非主键，与架构批准口径一致）；EXPLAIN（enable_seqscan=off）→ Index Scan using idx_inventory_batch_no（常规 Seq Scan 属 11 行小表优化器正常选择）
- **预期结果**：
  - 迁移正确：189/189 success、checksum 一致（-1564733106）、11 列存在且类型/默认值/COMMENT 匹配
  - 三向一致：实体 24 映射列 = migration 最终态 = 真实表物料口径 24 列，无新漂移
  - 收货链路：INSERT/UPDATE/STORE 三单全 HTTP 200 无 500；UPDATE 路径乐观锁递增；STORE 分支无污染
  - 流水真实：inventory_transactions 两方向写入（before→after）与单据号/成本透传正确
  - 查询面：selectInventoryPage + batchNo 筛选均无 500
  - 索引落库：rank 193=20260817.001 success=t、checksum -916957038 独立重算 MATCH；idx_inventory_batch_no 存在且唯一（btree on batch_no、count=1）、indisvalid=t、EXPLAIN 走 Index Scan
- **测试类型**：数据一致性检查（psql 只读断言 + Flyway checksum 独立计算 + HTTP 实测 + git 核验 + 索引断言/EXPLAIN 计划）
- **当前状态**：PASS（2026-08-16 转正：RM-B2 实施卡 1 QA 独立验收 PASS，无 FAIL、无 -R1；**2026-08-18 断言扩展：RM-B2 实施卡 3 QA 独立验收 PASS `docs/quality/rm-b2-impl3-qa-report.md`，六项验收全过、无 FAIL、无 -R1——索引落库断言纳入**）
- **限制说明**（逐条确认不阻断发布；本轨不进 Release Gate）：
  - 回归方式如实记录：POST 收货确认写操作由 developer 冒烟执行（3 单 HTTP 200），QA 以数据库三处落库痕迹（单据表 + inventory 行 + transactions 流水）独立交叉验证 + HTTP 查询面独立执行构成闭环（`rm-b2-impl1-qa-report.md` §三 / §八 R-1）
  - 实测库与生产库为同一实例（readiness RM-B2-RC-OBS-4 已登记；独立环境定义待实施卡 2 放卡时复核）
  - **idx_inventory_batch_no 已由实施卡 3 落库（2026-08-18 QA PASS）**：V20260817.001 落库（rank 193、checksum -916957038、CREATE INDEX IF NOT EXISTS 幂等 no-op 覆盖 OBS-4）——原「本基线不覆盖索引落库断言」口径**已过时并更新**，索引落库断言已扩展（见测试步骤 7）；idx_inventory_expiry_date / idx_inventory_location_id 仍为既有手工索引、migration 无声明（OBS-5/6 只登记不处理、分批原则合规，`rm-b2-impl3-qa-report.md` §2.2 确认未落库）
  - 范围声明：ETM-039/041（app_device_registrations / receipt_print_log 建表）未并入（分批原则，RM-BE-002/003 后续卡）；本基线仅基于 QA 已验收结论，无推测

---

### OIC-BE Batch 1 REV 转正（2026-08-16）

> **转正依据（维护规则 5：仅基于已验收结论，无推测）**：OIC-BE Batch 1 三卡 QA 独立验收 **PASS**（`docs/quality/oicbe-b1-qa-report.md`，2026-08-16：三卡全 PASS、无 FAIL、无 -R1；卡 3 附限制 L-1；commit 72f0d5a 提交核验通过；红线 R-1~R-7 全满足）。以下 4 条目自 2026-08-16 候选登记**转正为正式基线**（状态 PASS，来源卡 OICBE-B1-001/002/003）；共同限制如实登记：**migration 未落库（PD-014/015/016 待裁决）——当前回归行为为「明确错误态」断言（无假空、无吞错），决策落库后基线需扩展真实数据断言**。

| 基线条目 | 来源卡 | 来源审计项 | 回归行为（基于 QA 验收结论） | 状态 |
|---|---|---|---|---|
| REG-ETM-001 | OICBE-B1-001 | ETM-001（registration_code 缺表链路） | 注册码生成/校验/核销链路（含 OnboardingRecordServiceImpl 依赖链）：失败明确错误态（code:500 + traceId + success:false、无假成功、无吞错——13 方法无 catch 吞错，L340/379 为业务校验 throw）；code_expiry_time 列缺失为第一失败点（500 先于 registration_code 表缺失）；migration 未落库（PD-014 待裁决） | **PASS**（2026-08-16） |
| REG-ETM-002 | OICBE-B1-002 | KL-054 / ETM-002（sales_order 漂移） | dashboard 今日销售概览链路：明确错误态（改后 code:500「今日销售概览数据暂不可用」success:false，不再假空 code:0；三方验证=git diff + 运行时日志 BadSqlGrammarException@L97 + live 冒烟）；未补业务数据来源（不改业务规则）；migration 未落库（PD-015 待裁决） | **PASS**（2026-08-16） |
| REG-ETM-003 | OICBE-B1-002 | KL-054 / ETM-003（sales_order_detail 漂移） | SalesOrderDetail mapper 链路：sales_order/sales_order_detail 缺表 + orders 51 列双结构 + SalesOrder-only 8 列无表（独立实测一致）；Mapper 硬编码 4 处（L28/52/56/61）属 PD-015 裁决范围——对齐前链路失败无假成功（明确错误态），决策落库后行为正确（SELECT/INSERT/DELETE 无 500） | **PASS**（2026-08-16） |
| REG-KL055 | OICBE-B1-003 | KL-055（budgets 表结构漂移） | /v1/finance/budgets 链路：无假成功、无 COUNT 短路假空列表（结构护栏 assertBudgetSchemaAligned 触发 → code:500 明确错误态，日志铁证 traceId=6b5600cf1885 与冒烟响应对账闭环；budgets 15 列=migration、实体 9 列漂移、budget_id VARCHAR(32) vs Long 独立实测一致）；不猜测（PD-016 BLOCKED 不落库） | **PASS**（2026-08-16，附限制 L-1） |

> 候选转正前置条件已满足（2026-08-16）：`docs/quality/oicbe-b1-qa-report.md` 产出且三卡 QA 独立验收 PASS（无 FAIL、无 -R1；卡 3 附限制 L-1 逐条确认不阻断发布）→ 4 条目已转正（状态 PASS，注明日期 2026-08-16 与报告依据）。

---

### OIC-BE 裁决落地后断言扩展登记（2026-08-16）

> **登记依据（维护规则 5：仅基于已验收结论，无推测）**：PD-014/016 裁决落地（commit c697c95：V20260816_002/003/004 三 migration 落库 + RegistrationCodeMapper 修复）QA 独立验收 **PASS_WITH_LIMITATION**（`docs/quality/oicbe-b1-impl2-qa-report.md`，2026-08-16：卡 1（PD-014）/卡 3（PD-016）全 PASS_WITH_LIMITATION、无 FAIL、无 -R1）；PD-015 阶段一（commit 7fa3e0d：2 文档零代码）QA 独立验收 **PASS_WITH_LIMITATION**（`docs/quality/oicbe-b1-phase1-qa-report.md`，2026-08-16：六项验收标准全 PASS，L-1~L-4 均为文档级 minor/治理观察）。以下 4 条目的「限制说明」与「断言基线材料」**就地更新**（上方转正表与历史条目零修改），共同注明：**2026-08-16 裁决落地后断言扩展登记**。

| 基线条目 | 断言扩展登记内容（基于 QA 已验收结论） | 待办断言（明确标注未达成） |
|---|---|---|
| REG-ETM-001 | ① SQL 层链路断言：insert/findByCode/updateToUsed/countValidCodes 独立验证通过（事务回滚，零残留），20260816.002 落库后；② migration 断言：rank 190 success=t + checksum 独立重算 MATCH + 12 列 + PK + 2 索引 + onboarding_records 补 code_expiry_time；③ **R-2 Mapper 修复部署生效线上断言（2026-08-16 19:1x 主机重启后补入）**：运行实例（PID 6320）加载 `target\food-traceability-1.0.0.jar`（19:12:35 构建，晚于 c697c95 提交 18:09）内 RegistrationCodeMapper.class 字节码 @Update 注解 SQL 均为 `update_time = NOW()`（javap 反编译实测）；SQL 层 updateToUsed/updateExpiredCodes 等价语句在真实表执行 UPDATE 1 + update_time 正常写入（事务回滚零残留） | HTTP 成功路径断言**待 DR-01 处置**（Batch 2+ 排卡）——HTTP 面仍无法区分 Mapper 修复与 DR-01 效应（DR-01 阻断 selectById 全列查询），验证边界如实登记；R-2 断言以字节码 + SQL 层为准 |
| REG-KL055 | 读面断言扩展：GET /v1/finance/budgets → HTTP200 code:0 success:true records:[] total:0（护栏放行独立复验，20260816.003/004 落库后，正常空列表=真实数据非 COUNT 短路假空）；migration 断言：rank 191/192 success=t + checksum MATCH + 24 列 + budget_id BIGINT + PK 重建 + 序列默认值；表-only 列保留不删事实登记（7+2 列全保留实测）；9 财务列读写（事务回滚验证） | 写面断言**待 DR-02 处置**（budget_name NOT NULL，Batch 2+ 排卡） |
| REG-ETM-002 | 维持「明确错误态」断言：catch 先行改码保持（DashboardServiceImpl L95-115 在读、72f0d5a 未回退）；PD-015 阶段一完成——映射表 `docs/quality/sales-order-orders-mapping.md` 为阶段二输入（orders 唯一真相源 51 列独立实测） | 阶段二批次迁移后逐批扩展真实数据断言（Batch 2-002 恢复真实数据后优先；各批 QA PASS 后扩展）**（2026-08-18：阶段二批 1 QA 报告未产出，扩展候选待 QA，见 REG-ETM-002 条目限制说明）** |
| REG-ETM-003 | 维持「明确错误态」断言：对齐前失败无假成功（sales_order/sales_order_detail 缺表 + orders 双结构 + 8 无对应列，独立实测）；PD-015 阶段一完成（映射表 19+13 字段全覆盖） | 阶段二各批 QA PASS 后逐批扩展真实数据断言（SELECT/INSERT/DELETE 无 500）**（2026-08-18：阶段二批 1 QA 报告未产出，扩展候选待 QA，见 REG-ETM-003 条目限制说明）** |

> **Release Gate 状态（维持 OIC 轨口径）**：本轮**不进 Release Gate**；正式基线 FAIL=0 维持；4 条目 PWL 逐条确认不阻断——DR-01/02 为范围外登记项（Batch 2+ 排卡、非本批引入），不构成 FAIL。**（2026-08-18 追加：OIC-BE 阶段二批 1（OICBE-B2-001/002/003）QA 报告 `docs/quality/oicbe-b2-qa-report.md` 未产出——REG-ETM-002/003 扩展候选如实标注待 QA，见各条目限制说明；按维护规则 5 禁止推测不提前登记断言）**

---

### REG-ETM-001（正式转正 + 2026-08-16 裁决落地后断言扩展登记）

- **测试编号**：REG-ETM-001
- **模块**：入职-注册码链路（RegistrationCode / RegistrationCodeMapper / onboarding_records / registration_code）
- **关联任务**：OICBE-B1-001（ETM-001，P0；QA 独立验收 PASS 2026-08-16 `docs/quality/oicbe-b1-qa-report.md` §一，commit 72f0d5a；**裁决落地 QA 独立验收 PASS_WITH_LIMITATION 2026-08-16 `docs/quality/oicbe-b1-impl2-qa-report.md` §一（PD-014 落库，commit c697c95：V20260816_002 migration + RegistrationCodeMapper L37/L43 修复）**）
- **业务场景**：注册码生成/校验/核销链路（RegistrationCodeMapper.java 注解 SQL 5 处 + RegistrationCodeServiceImpl 全 13 方法 + OnboardingRecordServiceImpl 依赖链）——PD-014 裁决落地（代码引用优先建表）后：**SQL 层链路断言已扩展**，HTTP 业务成功路径断言**待 DR-01 处置前置**。
- **测试目的**：防止「缺表/列漂移 → 500」与「catch 吞错/假成功」回归 —— 链路失败必须透传为明确错误态（code:500 + traceId + success:false、无假成功）；PD-014 落库后断言扩展为 SQL 层链路（insert/findByCode/updateToUsed/countValidCodes）+ migration 落库事实；HTTP 成功路径断言在 DR-01 处置（Batch 2+）后补充。
- **测试步骤**（裁决落地后断言基线材料，基于 `oicbe-b1-impl2-qa-report.md` 已验收结论）：
  1. flyway_schema_history 只读断言：**rank 190 = version 20260816.002**（create registration code table）success=t（installed 2026-08-16 18:02:12）；checksum 与 git 工作区文件一致（QA 独立重算 -259030795 **MATCH**；算法以 RM 轨对照样本 V20260816_001 双重验证）；rank 189=20260816.001（RM 轨）不变
  2. information_schema 只读断言：registration_code **12 列**存在（id/code/type/validity_start/validity_end/status/code_expiry_time/created_by/onboarding_record_id/create_time/update_time/use_time；id/code/create_time/update_time NOT NULL；status DEFAULT 'UNUSED'）；PK pk_registration_code + 2 索引（idx_registration_code_code / idx_registration_code_onboarding_record_id）；onboarding_records **补列 code_expiry_time**（timestamp，现 29 列）
  3. **SQL 层链路独立断言**（事务回滚验证，psql BEGIN…ROLLBACK 内等价语句，零持久化）：INSERT（generateCode 面）→ INSERT 0 1 ✅；SELECT by code（findByCode L19）→ code/status/has_expiry 命中 ✅；UPDATE USED（updateToUsed L37 修复后 update_time）→ UPDATE 1 ✅；COUNT UNUSED（countValidCodes L31）→ 返回 0 ✅；ROLLBACK 零残留（registration_code 0 行实测）
  4. Mapper 修复核验：RegistrationCodeMapper **L37/L43 updated_at→update_time**（c697c95 diff 实测 2 处 + HEAD 源码复核）；mvn compile 独立执行 EXIT=0；**R-2 部署生效线上断言（2026-08-16 19:1x 主机重启后补入，详见步骤 7）**
  5. Mapper 硬编码面核验：RegistrationCodeMapper.java 注解 SQL 5 处（L19/25/31/37/43）引用 registration_code 表（XML 为空文件 234B，勘误真实）；源码阅读：RegistrationCodeServiceImpl 13 方法无 try/catch 吞错（L340 NOT_FOUND/L379 PARAM_ERROR 为业务校验 throw）
  6. **HTTP 成功路径（当前受限，如实登记）**：POST /v1/onboarding-records/{id}/registration-code 仍 500——根因独立证实：OnboardingRecord.java 实体 createdAt(L100)/updatedAt(L104)/interviewId(L84) 存在，而 onboarding_records 表**无 created_at/updated_at/interview_id 列**（仅 code_expiry_time/create_time/update_time 命中）→ selectById 全列查询列漂移必 500；**DR-01 为既有漂移（V20260717_020 改名/建表后实体未同步），非本批引入**；处置 = 独立任务卡（Batch 2+）
  7. **R-2 Mapper 修复部署生效线上断言（2026-08-16 19:1x，主机重启后）**：① 运行实例（PID 6320，`java -jar target\food-traceability-1.0.0.jar --spring.profiles.active=pg`）加载 jar 构建于 19:12:35（晚于 c697c95 提交 18:09），提取 `BOOT-INF/classes/.../RegistrationCodeMapper.class` javap 反编译：常量池 #26/#29 与 @Update 注解 value 均为 `UPDATE registration_code SET status = 'USED'/'EXPIRED', update_time = NOW() ...`——**修复随重启进程加载生效（字节码级铁证）**；② 表结构前置：registration_code 有 update_time 列、无 updated_at 列（修复前引用必 500 根因成立）；③ SQL 层等价语句事务回滚验证（psql BEGIN…ROLLBACK，零残留）：updateToUsed 等价（status='USED', update_time=NOW() WHERE code=? AND status='UNUSED' AND validity_end>NOW()）→ UPDATE 1 + update_time 写入 ✅；updateExpiredCodes 等价（status='EXPIRED', update_time=NOW() WHERE status='UNUSED' AND validity_end<=NOW()）→ UPDATE 1 + update_time 写入 ✅；④ 回滚后 registration_code 总行数=0（与重启前快照一致）；⑤ **验证边界如实登记**：HTTP 业务成功路径仍受 DR-01 阻断（selectById 全列查询列漂移），无法通过 HTTP 成功响应区分 Mapper 修复与 DR-01 效应，故 R-2 断言以字节码 + SQL 层为准（a 项运行时链路证据受 DR-01 阻断）
- **预期结果**：
  - migration 断言：rank 190 success=t、checksum MATCH、12 列 + PK + 2 索引 + onboarding_records 补列齐备
  - SQL 层链路：insert/findByCode/updateToUsed/countValidCodes 四步独立验证通过、回滚零残留
  - 明确错误态：DR-01 处置前 HTTP 面失败透传 → code:500 + traceId + success:false，无假成功、无吞错、无降级
  - 部署生效：Mapper 修复（update_time）随部署重启生效（R-2）——**2026-08-16 19:1x 实测已生效**（运行实例 jar 字节码 @Update SQL 均为 update_time + SQL 层等价语句 UPDATE 1 写入，事务回滚零残留）
- **测试类型**：数据一致性检查（flyway/information_schema 只读断言 + 事务回滚验证 + 源码核验 + mvn compile 独立复核；live HTTP 冒烟限于明确错误态面）
- **当前状态**：PASS_WITH_LIMITATION（2026-08-16 裁决落地后断言扩展登记，来源 `oicbe-b1-impl2-qa-report.md` PASS_WITH_LIMITATION，无 FAIL、无 -R1）
- **限制说明**（逐条确认不阻断发布；本轨不进 Release Gate）：
  - **① SQL 层链路断言已扩展（20260816.002 落库后）**：insert/findByCode/updateToUsed/countValidCodes 独立验证通过（事务回滚验证，QA 独立执行非采信）。
  - **② HTTP 成功路径断言待 DR-01 处置**：OnboardingRecord 实体 createdAt/updated_at/interview_id 列漂移（既有漂移、非本批引入）阻断 selectById → POST registration-code 仍 500（明确错误态成立）；**DR-01 已登记差异表 Batch 2+ 排卡**，处置后补充 HTTP 成功路径真实链路断言。
  - **③ RegistrationCodeMapper 修复（updated_at→update_time，L37/L43）——已部署生效（R-2，2026-08-16 19:1x 主机重启后实测补入）**：修复已提交（c697c95）+ 编译独立复核 EXIT=0；运行实例（PID 6320）加载 jar 内 Mapper 字节码 @Update 注解 SQL 均为 `update_time = NOW()`（javap 反编译实测，jar 构建 19:12:35 晚于提交 18:09）；SQL 层 updateToUsed/updateExpiredCodes 等价语句真实表执行 UPDATE 1 + update_time 正常写入（事务回滚零残留，registration_code 回到 0 行）。**验证边界（如实登记）**：HTTP 业务成功路径仍受 DR-01 阻断（见限制②），无法经 HTTP 成功响应区分 Mapper 修复与 DR-01 效应——R-2 断言以字节码 + SQL 层为准，a 项（运行时注册码链路证据）受 DR-01 阻断未采集。
  - 附带观察（不阻断）：registration_code.code_expiry_time 无实体字段（对齐面在 onboarding_records.codeExpiryTime，当前 insert 不写该列 NULL 可空，链路不受影响）；useCode 无效码分支写 registration_code_log（表缺，ETM-040 范畴）。

---

### REG-ETM-002（正式转正 + 2026-08-16 裁决落地后断言扩展登记）

- **测试编号**：REG-ETM-002
- **模块**：dashboard-今日销售概览（DashboardServiceImpl.getTodaySalesOverview / GET /api/v1/dashboard/today-sales）
- **关联任务**：OICBE-B1-002（KL-054，P1；QA 独立验收 PASS 2026-08-16 `docs/quality/oicbe-b1-qa-report.md` §二，commit 72f0d5a；**PD-015 阶段一 QA 独立验收 PASS_WITH_LIMITATION 2026-08-16 `docs/quality/oicbe-b1-phase1-qa-report.md`（阶段一零代码、零 migration；映射表 `docs/quality/sales-order-orders-mapping.md` 为阶段二输入）**）
- **业务场景**：今日销售概览链路真实数据或明确错误态 —— 无 BadSqlGrammarException 吞错假空（改前 code:0 todayOrderCount:0 / todayAmount:0 假空降级 → 改后 code:500「今日销售概览数据暂不可用」明确错误态）
- **测试目的**：防止「catch 降级假空 0 值」回归 —— 销售概览失败必须为明确错误态（无假空、无吞错）；PD-015 阶段一完成后维持该断言，**阶段二批次迁移（Batch 2-002 恢复真实数据）后扩展真实数据断言**。
- **测试步骤**：
  1. git diff 核验（72f0d5a）：getTodaySalesOverview catch 块由 `result.put("todayOrderCount", 0); result.put("todayAmount", 0)`（假空降级）改为 `logger.error + throw new BusinessException(INTERNAL_SERVER_ERROR, "今日销售概览数据暂不可用")`；todayAmount 仍 0（TODO 注释保留，未补业务数据来源）；**72f0d5a 未回退（HEAD 含该改动，阶段一验收结论⑤独立实测）**
  2. 运行时日志对账（`backend/logs/food-traceability.json` 2026-08-16 14:08:52）：DashboardServiceImpl logger.error「获取今日销售概览失败…禁止降级为假空 0 值」+ BadSqlGrammarException at DashboardServiceImpl.java:97（salesOrderMapper.selectCount）—— 改后代码运行实例生效
  3. live 冒烟：GET /api/v1/dashboard/today-sales → HTTP200 code:500「今日销售概览数据暂不可用」success:false（不再假空 code:0）
  4. 影响面核验：getDashboardOverview(1)/(2) 因 result.putAll(getTodaySalesOverview()) 先行调用，销售子项失败 → 整卡 500（「明确错误态优先」原则接受，前端失败提示行为建议 observe 确认）
  5. **阶段一验收结论（2026-08-16）**：`oicbe-b1-phase1-qa-report.md` ⑥ 项验收标准全 PASS（orders 唯一真相源 51 列独立实测 / 映射表 19+13 字段全覆盖 / R-8 零新增引用 / 阶段二批次计划双登记一致 / catch 先行改码在读）——阶段一零代码零落库，明确错误态断言维持不变
- **预期结果**：
  - 无假空：不得出现 code:0 + todayOrderCount:0 / todayAmount:0 假空成功
  - 明确错误态：code:500 + 明确 message + success:false；三方验证一致（git diff + 运行时日志 + live 冒烟）
  - 不改业务规则：todayAmount 0 TODO 保留、未补销售数据来源；阶段一未做任何 sales_order→orders 代码迁移
- **测试类型**：数据一致性检查（git diff 核验 + 运行时日志对账 + live HTTP 冒烟 + 阶段一验收结论引用）
- **当前状态**：PASS（2026-08-16 裁决落地后断言扩展登记：维持「明确错误态」断言，来源 `oicbe-b1-phase1-qa-report.md` PASS_WITH_LIMITATION、无 FAIL、无 -R1）
- **限制说明**：
  - **维持「明确错误态」断言（无假空、无吞错）**：PD-015 裁决落地（阶段一，2026-08-16）后断言保持——catch 先行改码在读、72f0d5a 未回退、阶段一零代码改动。
  - **阶段二批次迁移后扩展真实数据断言**：PD-015 阶段一映射表 `docs/quality/sales-order-orders-mapping.md` 为阶段二输入（Batch 2 实体+查询 / Batch 3 接口 / Batch 4 报表统计，每批 QA+基线）；**阶段二各批 QA PASS 后逐批扩展**——Batch 2-002（DashboardServiceImpl 改走 orders）恢复真实数据后，REG-ETM-002 扩展「今日销售概览真实数据链路」断言；契约未确认前禁止启动阶段二迁移（阶段一验收结论⑧）。
  - **2026-08-18 阶段二批 1 状态（如实标注待 QA）**：OIC-BE 阶段二批 1（OICBE-B2-001/002/003）developer 开发完成（T 项先行，`oic-be-task-board.md` §四·6）；QA 报告 `docs/quality/oicbe-b2-qa-report.md` **并行产出中、截至 2026-08-18 未产出**——按维护规则 5（禁止推测）**不登记断言扩展**；待 QA 报告产出（PASS/PWL）后补登「T 项迁移后实体/mapper 对齐事实」断言候选（本卡 Dashboard 零改动、72f0d5a 明确错误态保持为 developer 证据，未经 QA 验收不成为基线断言）；**真实数据断言扩展仍待 B-1（PD-017）决策后**（决策前不恢复真实数据）。
  - 观察（不阻断）：总览/销售卡（overview{1,2}）随销售子项 500（明确错误态优先原则接受）；getInventoryAlerts / getMemberGrowthTrend 同类 catch 降级属 RM 轨 ETM-106 / 会员域，不在本卡。

---

### REG-ETM-003（正式转正 + 2026-08-16 裁决落地后断言扩展登记）

- **测试编号**：REG-ETM-003
- **模块**：销售订单-SalesOrderDetail mapper（SalesOrderMapper.xml L28 / L52 / L56 / L61 硬编码 sales_order / sales_order_detail）
- **关联任务**：OICBE-B1-002（KL-054，P1；QA 独立验收 PASS 2026-08-16 `docs/quality/oicbe-b1-qa-report.md` §二；本批未改 SalesOrderMapper 文件，属 PD-015 裁决范围；**PD-015 阶段一 QA 独立验收 PASS_WITH_LIMITATION 2026-08-16 `docs/quality/oicbe-b1-phase1-qa-report.md`（映射表 `docs/quality/sales-order-orders-mapping.md` 为阶段二输入）**）
- **业务场景**：SalesOrderDetail mapper 链路 —— SELECT/INSERT/DELETE 对齐后行为正确（无 500）；对齐前链路失败无假成功（明确错误态）
- **测试目的**：防止「sales_order / sales_order_detail 缺表 → mapper 硬编码 SQL 500」回归 —— PD-015 阶段二对齐前链路失败必须为明确错误态、无假成功；**阶段二各批（B2-001 实体对齐 / B2-002 Mapper 查询迁移）QA PASS 后**扩展 SELECT/INSERT/DELETE 真实链路断言。
- **测试步骤**：
  1. information_schema 只读断言：sales_order / sales_order_detail 均不存在；orders 51 列双结构并存（旧列集 order_number / order_amount(numeric) / actual_amount(numeric) + 新列集 order_code / order_status(integer) / total_amount(bigint) / final_amount(bigint) / paid_amount(bigint)；store_id / idempotency_key 亦在）；**阶段一验收（2026-08-16）独立实测复核：orders 51 列 / order_items 17 列 / OrderNew 33/33 对齐 0 缺失 / orders=1 行 / order_items=0 行（orders 唯一生产真相源确认，验收项① PASS）**
  2. SalesOrder-only 8 列无表断言：orders 中 status / order_time / estimated_time / completed_time / table_no / people_count / created_by / updated_by 查询结果为空；**映射表 19 字段全覆盖（直映 4 / 转换 7 / 无对应 8）+ 明细 13 字段（直映 2 / 转换 5 / 无对应 7）独立核验（验收项② PASS，附 L-1 文档 minor 由 developer 阶段二放卡前补正）**
  3. Mapper 硬编码面核验：SalesOrderMapper.xml L28 SELECT FROM sales_order / L52 sales_order_detail / L56 INSERT / L61 DELETE（审计原文，本批未改）；**阶段一 R-8 核验：7fa3e0d 后零新增 sales_order 代码引用（验收项③ PASS）**
- **预期结果**：
  - 三向核对事实成立：缺表 + orders 双结构 + SalesOrder-only 8 列无表（独立实测一致）
  - 无假成功：对齐前链路失败明确错误态（无吞错、无假空）
  - 阶段二对齐后（Batch 2 各批 QA PASS）行为正确：SELECT/INSERT/DELETE 不再 500
- **测试类型**：数据一致性检查（information_schema 只读断言 + Mapper 源码核验 + 阶段一验收结论引用）
- **当前状态**：PASS（2026-08-16 裁决落地后断言扩展登记：维持「明确错误态」断言，来源 `oicbe-b1-phase1-qa-report.md` PASS_WITH_LIMITATION、无 FAIL、无 -R1）
- **限制说明**：
  - **维持「明确错误态」断言**：阶段二对齐前链路失败无假成功（缺表 + 双结构 + 8 无对应列事实核对独立实测）；PD-015 阶段一零代码改动（7fa3e0d 仅 2 文档），断言不变。
  - **阶段二批次迁移后扩展真实数据断言**：PD-015 阶段一映射表 `docs/quality/sales-order-orders-mapping.md` 为阶段二输入（契约点：状态机映射 / 主键策略 / 类型转换 / 候选列选边 / 8 无对应列处置 / 明细归属，未确认前禁止迁移）；**阶段二各批 QA PASS 后逐批扩展**——B2-001 实体对齐 + B2-002 Mapper 4 语句迁移后，REG-ETM-003 扩展「SELECT/INSERT/DELETE 真实链路行为正确（无 500）」断言。
  - **2026-08-18 阶段二批 1 状态（如实标注待 QA）**：OIC-BE 阶段二批 1（OICBE-B2-001/002/003）developer 开发完成（T 项先行：SalesOrder/SalesOrderDetail @TableName → orders/order_items、字段按 T-1~T-7 映射、B 项 exist=false 标注，`oic-be-task-board.md` §四·6）；QA 报告 `docs/quality/oicbe-b2-qa-report.md` **并行产出中、截至 2026-08-18 未产出**——按维护规则 5（禁止推测）**不登记断言扩展**；待 QA 报告产出（PASS/PWL）后补登「T 项迁移后实体/mapper 对齐事实」断言候选（实体注解 19/13 字段全落在 orders 51 列/order_items 17 列清单内为 developer 证据，未经 QA 验收不成为基线断言）；**真实数据断言（SELECT/INSERT/DELETE 无 500）仍待 B 项（PD-017~021）决策后扩展**（如实：INSERT 受 order_items.product_type NOT NULL 缺口阻断、deleteOrderDetail 物理删除语义差异为 developer 登记异常，属 PD-020/Batch 3 决策输入）。

---

### REG-KL055（正式转正 + 2026-08-16 裁决落地后断言扩展登记）

- **测试编号**：REG-KL055
- **模块**：财务-预算（budgets / finance.Budget / BudgetController /v1/finance/budgets）
- **关联任务**：OICBE-B1-003（KL-055，P1；QA 独立验收 PASS 附限制 L-1 2026-08-16 `docs/quality/oicbe-b1-qa-report.md` §三，commit 72f0d5a；**裁决落地 QA 独立验收 PASS_WITH_LIMITATION 2026-08-16 `docs/quality/oicbe-b1-impl2-qa-report.md` §二（PD-016 落库，commit c697c95：V20260816_003/004 两 migration）**）
- **业务场景**：/v1/finance/budgets 链路无假成功、无 COUNT 短路假空列表 —— 改前 records:[] total:0 code:0 假空 → 改后结构护栏 → code:500 明确错误态 → **PD-016 落库后护栏使命完成自动放行 → 读面 code:0 正常空列表**；写面受 DR-02 阻断（表-only 约束，如实登记）
- **测试目的**：防止「COUNT 短路假空列表」回归 —— 结构未对齐前必须为明确错误态（结构护栏触发 → code:500 + traceId 可查，无假空、无吞错）；PD-016 裁决落库后断言扩展为**读面真实数据断言**（护栏放行空列表）；写面断言在 DR-02 处置（Batch 2+）后补充。
- **测试步骤**（裁决落地后断言基线材料，基于 `oicbe-b1-impl2-qa-report.md` 已验收结论）：
  1. flyway_schema_history 只读断言：**rank 191 = version 20260816.003**（align budgets columns，18:02:12）+ **rank 192 = version 20260816.004**（add budgets budget id sequence，18:05:19）均 success=t；checksum 独立重算分别 **MATCH**（-388535559 / 1208216516）——提交文件 == 应用文件
  2. information_schema 只读断言：budgets **24 列**（15+9）——9 补列实测存在（budget_year/budget_month/budget_type/category_id INTEGER、actual_amount/variance BIGINT、variance_rate NUMERIC(10,4)、responsible_dept_id BIGINT、remark VARCHAR(500)）；**budget_id BIGINT + nextval('budgets_budget_id_seq') 默认值**；PK 重建完成（budgets_pkey PRIMARY KEY (budget_id)）；**表-only 列保留不删事实登记**：budget_name/used_amount/remaining_amount/budget_period/status/create_by/update_by（7 列）+ created_by/updated_by（BaseEntity 提供，修正旧口径 9→7）全保留实测
  3. **读面断言（独立 HTTP 复验，只读）**：GET /v1/finance/budgets?current=1&size=20 → **HTTP200 code:0 success:true records:[] total:0**——正常空列表（表 0 行 → 空列表为真实数据，非 COUNT 短路假空；**护栏按设计完成使命后自动放行**，不再 code:500）；护栏代码未改（72f0d5a..c697c95 文件清单无 BudgetServiceImpl/BudgetMapper，assertBudgetSchemaAligned L44-49 / getPage L114 首行调用在位）
  4. **9 财务列读写断言（事务回滚验证，独立执行）**：带 budget_name 完整 INSERT（budget_id 走序列默认自动生成）+ SELECT 9 财务列（INTEGER/BIGINT/NUMERIC(10,4)=0.0000 读写正常）→ ROLLBACK 零残留（budgets 0 行实测）
  5. **写面断言（当前受限，如实登记）**：POST create 被表-only 列 **budget_name NOT NULL 阻断**（独立复现：`INSERT ... VALUES (2026,8,1,1,1000)` → null value violates not-null constraint；代码路径 BudgetServiceImpl.create L74 baseMapper.insert——实体无 budgetName 字段，INSERT 不含 budget_name）；**DR-02 为表-only 约束涉业务结构取舍（放宽 vs 实体补字段），按裁决「不直接删字段、剩余差异落任务池」Batch 2+ 排卡，不扩大范围**
  6. 护栏核验（历史基线，72f0d5a）：BUDGET_ENTITY_MAPPED_COLUMNS + assertBudgetSchemaAligned + BudgetMapper.countEntityColumnsPresent（information_schema 只读查询）；护栏触发日志铁证 traceId=6b5600cf1885 与冒烟响应对账闭环（对齐前）
- **预期结果**：
  - 读面：code:0 正常空列表（护栏放行），无 500、无 COUNT 短路假空
  - 写面：DR-02 处置前 POST create 明确错误态（NOT NULL violation 透传），无假成功；处置后补充写入面成功路径断言
  - 表-only 列保留不删（裁决「不直接删字段」落地事实，7+2 列全保留实测）
  - 不猜测：DR-02/03/04 剩余差异按裁决落任务池（Batch 2+），未顺手修
- **测试类型**：数据一致性检查（flyway/information_schema 只读断言 + 护栏源码核验 + 独立 HTTP 复验 + 事务回滚验证）
- **当前状态**：PASS_WITH_LIMITATION（2026-08-16 裁决落地后断言扩展登记，来源 `oicbe-b1-impl2-qa-report.md` PASS_WITH_LIMITATION，无 FAIL、无 -R1）
- **限制说明**（逐条确认不阻断发布；本轨不进 Release Gate）：
  - **读面断言已扩展（20260816.003/004 落库后）**：GET /v1/finance/budgets → code:0 正常空列表（护栏放行独立复验，独立 HTTP 执行非采信）；无假空、无吞错。
  - **写面断言待 DR-02 处置**：budget_name NOT NULL 阻断 POST create（独立复现 violation；表-only 约束涉业务结构取舍，Batch 2+ 排卡，不裁决处置方向）；**DR-02 处置后补充写入面成功路径断言**。
  - **L-1（来源卡 3，仍登记不阻塞）**：护栏抛 `com.foodtraceability.exception.BusinessException`（finance 包）未被 GlobalExceptionHandler 的 common 包 handler（L119）捕获 → 落入通用 RuntimeException handler（L303）→ 响应 message 为通用「系统繁忙」，护栏自定义提示未透传（日志保留完整消息）。结论：明确错误态成立（success:false + traceId 可查）；**PD-016 落库后读面已放行（code:0），该限制仅在对齐面再次触发护栏时相关**；finance 域双包异常类并存建议 architect 评估（QA 报告 R-3）。
  - 附带观察（不阻断）：表-only 列保留不删事实登记（裁决落地，DR-02/03/04 差异表 Batch 2+）；DR-03（budget_amount DECIMAL(15,2) vs 实体 Long 分，读写可用语义差异）/ DR-04（旧 entity/Budget.java 死代码）差异表登记；budgets_budget_id_seq last_value=4 序列 gap 为回滚验证正常消耗（R-3，结构交付物保留）；create/update/actual 端点未加护栏——DR-02 处置前失败透传 500（无假成功，安全）；BudgetCheckScheduler L47 catch 仅日志。

---

## OIC-2 Batch 4 REV 登记（发布后独立治理编排，2026-08-18：employee 断流如实提示断言）

> 背景：OIC-2 轨（多客户端前端治理，`oic-2-task-board.md`）红线 C-3 = 不进 Release Gate、不创建 Sprint 编号——本批按 QA 报告 §六建议（断流如实提示纳入回归基线断言，防假数据/假成功 reintro）+ REV 指令登记 REG 断言条目，维持 OIC 轨口径（不进 Release Gate、参与 REV 持续积累、正式基线 FAIL=0 维持）；OIC-2 Batch 1~3 既有结论按原口径不回写回归基线（历史口径维持，非本批范围）。
> 转正依据（维护规则 5：仅基于已验收结论，无推测）：OIC-2 Batch 4 三卡 QA 独立验收 **PASS**（`docs/quality/oic2-b4-qa-report.md`，2026-08-18：三卡全 PASS、无 FAIL、无 -R1；红线 C-1~C-4/C-6 全满足；无 BLOCKED_PRODUCT_RULE 触发）。

| 基线条目 | 来源卡 | 来源审计项 | 回归行为（基于 QA 验收结论） | 状态 |
|---|---|---|---|---|
| REG-EMP-001 | OIC2-B4-001 / OIC2-B4-002 / OIC2-B4-003 | EMP-DATA-011（LeavePage 假余额/假考勤）/ EMP-DATA-019（SecurityLockPage 本地 PIN 假成功）/ EMP-DATA-009（HomePage 假通知） | 断流如实提示文案保留断言（余额/考勤/锁屏/公告四区后端接线前维持如实提示/空态，dist 命中 1/1/1/2）；假数据/假成功/静默伪装空态 0 命中（源码+dist）；无新增 API 调用、无业务规则实现 | **PASS**（2026-08-18） |

### REG-EMP-001（正式转正）

- **测试编号**：REG-EMP-001
- **模块**：员工端-断流如实提示（employee-frontend：LeavePage/LeaveOverview.vue、SecurityLockPage.vue、HomePage.vue）
- **关联任务**：OIC2-B4-001（LeavePage 假余额/假考勤，EMP-DATA-011）/ OIC2-B4-002（SecurityLockPage 本地 PIN 假成功，EMP-DATA-019）/ OIC2-B4-003（HomePage 假通知，EMP-DATA-009）；QA 独立验收 PASS 2026-08-18 `docs/quality/oic2-b4-qa-report.md`（三卡全 PASS、无 FAIL、无 -R1；红线 C-1/C-2/C-3/C-4/C-6 合规；无 BLOCKED_PRODUCT_RULE）
- **业务场景**：断流如实提示文案保留——余额/考勤/锁屏/公告四区在后端接线（EMP-BE-004 leave 端点 / EMP-API-002 公告端点，归后端整改池）前必须维持如实提示/空态，禁止假数据（假余额/假考勤/假公告）、假成功（锁屏「安全密码已设置」）、静默伪装空态 reintro
- **测试目的**：防止「假数据/假成功/静默伪装空态」回归 —— Batch 4 三卡移除的假余额（年假 10/病假 15/调休 3 等）、假考勤（05-26~06-01）、假公告（端午节/培训考核通知）、锁屏假成功（confirm 假成功态视图）不得 reintro；B2-001 断流处置模式（如实提示）延续保持。
- **测试步骤**：
  1. 假数据 grep 0 命中（源码+dist）：LeavePage.vue + LeaveOverview.vue 对 `年假10|年假 10|病假15|病假 15|调休3|调休 3|total: 10|used: 5|remaining: 5` 与 `05-26|06-01` → 0 命中；SecurityLockPage.vue 对「安全密码修改成功/设置成功/已设置」+ `localStorage|STORAGE_KEY|btoa|setItem|ElMessage.success|switchToChangeMode|CircleCheckFilled|mode.value = 'confirm'|onMounted` → 0 命中（单文件 mock 写路径）；HomePage.vue 对「端午节」「6月食品安全培训考核通知」「关于端午节放假安排的通知」→ 0 命中（注释亦规避字面量）
  2. dist 如实提示文案保留断言（命中数恒定，防 reintro/误删）：「假期余额服务暂未开通」1 命中（LeaveOverview 余额区）+「考勤联动服务暂未开通」1 命中（LeaveOverview 考勤区）+「请假记录服务暂未开通」1 命中（LeavePage，B2-001 既有）+「锁屏功能待后端接线」1 命中（SecurityLockPage）+「公告服务暂未开通」2 命中（HomePage + MessagesPage 同域一致）
  3. 无假成功路径：LeavePage.vue + LeavePage/components/ 无 ElMessage.success；SecurityLockPage handleSetPin/handleChangePin 校验后仅 ElMessage.warning（无写操作、无成功提示、confirm 假成功态视图已删、mode 联合类型无 'confirm'）；HomePage 公告区无点击成功提示（AnnouncementList 点击仅跳转 /messages?tab=notices）
  4. 无静默伪装空态：余额/考勤区 `<template v-if="balances/recentAttendance.length > 0">` 守卫 + v-else EmptyState 如实空态（action-text 空，无操作按钮）；公告区 v-if/v-else `.announcement-unavailable` 如实提示（不静默伪装空态）
  5. 构建/测试：npm run build EXIT=0（2026 modules）+ 测试 12 passed/1 skipped TEST_EXIT=0（qa 独立执行）
  6. 范围核验：Batch 4 增量仅 employee-frontend 4 文件（HomePage.vue / LeaveOverview.vue / SecurityLockPage.vue / LeavePage.vue 增量）；AnnouncementList.vue 未触碰；无新增 API 调用（leaveApi 零调用保持，4 文件 `/v1/` 命中仅注释 3 处）
- **预期结果**：
  - 假数据/假成功 0 命中（源码+dist）；断流如实提示文案保留（dist 命中数恒定：余额 1/考勤 1/请假 1/锁屏 1/公告 2）
  - 无静默伪装空态：断流域均守卫 + 如实提示，非空数组伪装
  - 后端接线前维持如实提示态（EMP-BE-004/EMP-API-002 立项前）；接线后由真实数据链路断言承接（另行登记）
- **测试类型**：手工回归（代码级 grep/读码/diff + dist 构建产物断言 + 构建/测试独立执行）
- **当前状态**：PASS（2026-08-18，OIC-2 Batch 4 三卡 QA 独立验收 PASS `docs/quality/oic2-b4-qa-report.md`，无 FAIL、无 -R1）
- **限制说明**（逐条确认不阻断发布；OIC 轨口径不进 Release Gate）：
  - L-1（卡 2）：SecurityLockPage 遗留死 CSS（.success-icon/.security-features/.feature-*/.secondary-btn/.lock-card--center）+ mode 恒 'set' 不可达死 UI——设计冻结期保留，等设计解冻由架构/planner 决定去留（OIC2-B4-002-DEV-001 登记）
  - L-2（卡 2）：历史 localStorage 旧 mock PIN（emp_security_pin_hash）存量残留无清理/兼容路径——数据真实性方向正确（旧值不再作为有效身份），存量清理归后端/后续批次（OIC2-B4-002-QA-01 登记）
  - L-3（卡 3）：HomePage 假统计卡（出勤 22%/待办 3/预警 1）与假排班（早班 07:00-15:00）仍以业务数据展示——EMP-DATA-009 同源、卡面验收⑥范围外只登记不修（OIC2-B4-003-DEV-001 登记），后续真实化批次排卡；「公告服务暂未开通」提示与上方假统计/假排班并存存在误读风险（qa 观察已登记在案）
  - L-4（卡 1）：余额/考勤区在 EMP-BE-004（后端池）立项接线前维持如实空态；api/leave.ts leaveApi 端点仍定义但零调用（EMP-API-001 只登记不修）
  - 后端接线依赖：EMP-BE-004（/v1/employee/leave/* 端点）/ EMP-API-002（公告端点）归后端整改池（KL Pool），接线后本条断言由后端真实数据链路断言承接

---

## 回归执行说明

1. **执行时机**：每次涉及上述模块的修复、重构、优化上线前，必须执行对应回归案例。
2. **执行方式**：按「测试类型」执行（手工回归 / API 回归 / 数据一致性检查），并记录实际结果与执行日期。
3. **状态变更**：回归失败时，状态改为 FAIL 并注明：失败现象、复现步骤、关联代码变更、处理负责人；修复后重新回归，通过后恢复 PASS 并记录日期。
4. **限制状态**：PASS_WITH_LIMITATION 表示验收通过但存在已知限制（详见各案例「限制说明」与 `production-known-limitations.md`），非失败项；限制消除后更新状态并注明日期。
5. **案例新增**：仅允许基于已验收结论新增案例；无证据的推测不得写入本基线。
6. **信息不足**：任何信息不足的条目标记为「待补充」，不得自行设计业务规则。
7. **候选登记**：后端改动 REV 条目在 QA 验收前可登记为候选（状态=候选（待 QA PASS 转正），含来源卡编号 + 回归行为 + 状态三要素），候选不进入通过数统计、不参与 Release Gate 判定；QA 报告产出且验收 PASS 后由 regression 转正（状态改 PASS 并注明日期与报告）。

---

## Sprint-3 浏览器冒烟补验门禁记录（2026-08-11）

> 报告全文：`docs/quality/browser-smoke-sprint3.md`。环境能力：有（playwright + 系统 Edge，`channel:'msedge'`）；PROD 构建（vite preview:3003，`VITE_API_BASE_URL=http://localhost:8081/api`）为主 + dev(3002) 对照。

### 冒烟结果（4 项）

| 项 | 结果 | 依据 |
|---|---|---|
| KL-013（登录/路由/权限按钮/401 弹框登出） | **PASS** | 登录页渲染→admin 登录→/home→/finance/ledger 可达→批量调价/成本导出按钮渲染（v-permission+admin `*`）；篡改 token 触发业务请求→弹框「登录已过期」无取消/留在本页→确认登出跳 /login（PROD 分支实测闭环，REG-SEC-008 浏览器缺口关闭） |
| KL-030（导出下载） | **FAIL（DEF-A/DEF-B 阻断 2/3 抽样）** | OrderQuery 下载 370B：BOM+13 表头+真实数据行 PASS；AssetDepreciation 白屏（DEF-A）、DeviceAlerts 表空（DEF-B）→ 浏览器缺口部分关闭 |
| KL-020-V2（门店下拉切换） | **PASS** | DecisionBoard 下拉 3 选项渲染；选中/切换/清空每次触发 `/v1/stores/active` 重载（1→2→3→4 次），org-context version 机制交互生效（REG-DATA-014 V-2 浏览器缺口关闭） |
| KL-031-R1（通知面板） | **PASS** | 造数 2 条→面板标题/时间渲染→徽标 2→单条已读 1（DB is_read=1+read_time 落库）→全部已读徽标隐藏→造数清理 0 残留（REG-MOCK-005 浏览器缺口关闭） |

### 新暴露活跃缺陷（本冒烟暴露，非验证缺口，均需 developer 修复）

| 编号 | 位置 | 现象 | 影响 |
|---|---|---|---|
| DEF-A | `frontend/src/api/asset/inventory.ts` L13-18 JSDoc 未闭合（`/*`21 vs `*/`20，基线 commit 即存在） | import 全被注释吞掉 → `InvStatus is not defined` → asset 分块崩溃 | **资产模块全部页面（折旧/盘点/台账等）dev+PROD 双环境白屏**（实测 3 页全崩） |
| DEF-B | `backend/.../entity/Device.java` device_type(Integer) vs `devices.device_type` varchar(50) 实体-表漂移（DEF-3 同族） | 设备存在时（device_type='temperature' 等字符串）映射 int 失败 | `/v1/device-alerts/page`、`/v1/devices/page`、`/v1/devices/list` 恒 500 → 告警页表空、设备列表不可用（此前表 0 行未暴露） |

### 门禁

```text
Regression Result: FAIL（浏览器冒烟补验，2026-08-11）
规则：FAIL > 0 禁止放行

FAIL 明细（2 项活跃代码缺陷）：
1. DEF-A：inventory.ts 注释未闭合 → asset 页白屏（KL-030 折旧抽样阻断）
2. DEF-B：Device.deviceType 类型漂移 → 设备/告警接口 500（KL-030 告警抽样阻断）

缺口关闭确认：KL-013 / KL-020-V2 / KL-031-R1 浏览器缺口已实测关闭；
KL-030 浏览器缺口部分关闭（OrderQuery PASS，折旧/告警待 DEF-A/DEF-B 修复后重跑）。

发布首日补验项：DEF-A 修复后资产域各页冒烟 + 折旧导出 CSV；DEF-B 修复后设备/告警页 + 告警导出 CSV。
```

**门禁结论：冒烟 FAIL > 0 → 禁止放行；DEF-A/DEF-B 修复并复验通过后，重跑折旧/告警导出闭环 KL-030。**

---

## KL-030 冒烟重跑闭环记录（2026-08-11，DEF-A/B 修复后）

> 报告全文：`docs/quality/p0-defab-kl030-rerun.md`。环境：后端 8081 UP（db=UP）+ PROD preview:3003（dist 含 VITE_API_BASE_URL=http://localhost:8081/api）+ playwright msedge headless；登录 admin/Admin@123。
> 前置：DEF-A（P0-ASSET-001）/ DEF-B（P0-DEVICE-001）QA 复验 PASS（`p0-asset-001-r1-qa-report.md` / `p0-device-001-r1-qa-report.md`）。

### 重跑结果（KL-030 两抽样项 + 补验项 3）

| 项 | 结果 | 证据 |
|---|---|---|
| 折旧导出（页面渲染/下载/CSV/接口） | **PASS** | /asset/depreciation 不白屏（200.00 渲染、0 页面错误）；下载 `折旧表_*.csv`（250B）；CSV：BOM ✓ + 表头 9 列 ✓ + 真实数据行 `["","","2026-07","200.00","1000.00","11000.00","直线法","2026-08-11 01:10:08","系统自动"]`（**fenToYuan 200.00 = 20000 分断言精确命中**）；GET /v1/asset/depreciation/page 200 |
| 告警导出（造数/列表/下载/CSV/接口） | **PASS** | 造数 BSSM3-R2-DEV-001（device_id=6）+ BSSM3-R2-ALERT-OVERTEMP（alert_id=2）；/device/alerts 列表渲染设备行非白屏；下载 `设备告警_*.csv`（233B）；CSV：BOM ✓ + 表头 9 列 ✓ + 真实数据行 `["BSSM3-R2-ALERT-DEVICE","离线超时","警告","BSSM3-R2-ALERT-OVERTEMP","pending","","","",""]`（告警内容断言命中）；GET /v1/device-alerts/page 200；测后清理 BSSM3-R2-% 四表残留 = **0** |
| 设备三接口补验（DEF-B 范围） | **PASS** | /v1/devices/page（code=0 total=1 PRINTER）、/v1/devices/list（code=0 count=1 PRINTER）、/v1/devices/type/PRINTER（code=0）—— deviceType 字符串往返 |
| 资产三页冒烟（补验项 3） | **PASS** | /asset/depreciation、/asset/inventory-check、/asset/ledger 全部非白屏（asset 分块不再崩溃） |

### 物理清理证据

- `DELETE FROM devices WHERE device_id IN (2,3)`（QA-TEST-PRN-001 / QA-TEST-SCN-002，deleted=1 逻辑删除残留，QA 复验 §6 登记）→ **DELETE 2**；`QA-TEST-%` 残留 = **0** ✓；devices/device_alerts/asset_depreciation_records 全表归 0 基线。

### 基线固化（58 → 60 条）

- **REG-ASSET-001**（P0-ASSET-001 / DEF-A）→ **PASS**：QA 复验 + 折旧页渲染/导出 CSV 闭环（详见「DEF-A/DEF-B 修复闭环回归」小节）
- **REG-DEVICE-001**（P0-DEVICE-001 / DEF-B）→ **PASS**：QA 复验 + 三接口 code=0 + 告警导出 CSV 闭环（同上）

### 门禁（复评）

```text
Regression Result: PASS（2026-08-11 冒烟重跑复评）
规则：FAIL > 0 禁止放行

FAIL 明细（复评时点）：0
- DEF-A（P0-ASSET-001）→ QA 复验 PASS + 本重跑浏览器闭环【关闭】
- DEF-B（P0-DEVICE-001）→ QA 复验 PASS + 本重跑接口级闭环【关闭】

缺口关闭确认：KL-013 / KL-020-V2 / KL-031-R1 已关闭（browser-smoke-sprint3.md）；
KL-030 浏览器缺口全部关闭（OrderQuery + 折旧 + 告警三抽样全 PASS）。

剩余发布条件（非 FAIL 项）：PD-001/PD-003 产品决策（及 002/004）——上线门禁范畴。
```

**门禁结论：冒烟门禁 FAIL → PASS（FAIL=0，回归门禁允许放行）；活跃缺陷部分全部闭环（KL-013/020-V2/030/031-R1 全关）。**

---

## Batch CORE-1 回归（core 增强批次，2026-08-31）

> 验收依据：`docs/quality/ui-core-b1-qa-report.md`（2026-08-31，QA 独立验收：P1-UI-CORE-001/002/003 三卡全 PASS、FAIL=0、无 -R、无 BLOCKED；强制核验点 6 项全过；build EXIT=0、typecheck 本批 7 文件零新增错误（存量 136 条非本批引入））。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-UI-CORE-001/002/003（维护规则 5），无推测内容。
> 批次口径：本批为**治理批次**（第二步 core 增强，任务池 §9.2），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；第三步财务 4 页接入（P1-UI-FIN-001~004）前置已满足（任务池 L2513）。
> 基线计数核对：追加前正式基线 **70 条**（文件实况：含 REG-KL055 与 REG-EMP-001 转正；任务简报/任务池 §9.2 口径 69 条遗漏 REG-KL055，以回归文件为真相源）→ 追加 3 条后 **73 条**，只增不减纪律达成（既有 70 条条目零修改）。

### REG-UI-CORE-001

- **测试编号**：REG-UI-CORE-001
- **模块**：core-DataTable 密度联动（frontend/src/components/core/DataTable.vue + stores/layout.ts + components/layout/MainLayout.vue）
- **关联任务**：P1-UI-CORE-001（Batch CORE-1；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-core-b1-qa-report.md`；含 P2-CORE-009 size="small" 静默失效自然修复）
- **业务场景**：DataTable density/size opt-in 默认关闭 + layoutStore 密度唯一真相源（compactMode 归一、无双向写残留）+ 115 消费方未 opt-in 渲染不变 + 3 处 size="small" 真实生效
- **测试目的**：防止密度假联动（设置与渲染脱节）与 size="small" 静默失效回归 —— 未传 density/size 必须保持默认渲染（默认 12px 行高、无密度类）；density='auto' 必须真实消费 layoutStore.tableDensity；compactMode 不得出现双向写残留（唯一真相源=tableDensity，架构裁决 roadmap §5.10 L250 落地）。
- **测试步骤**：
  1. 全局 grep `<DataTable` 标签使用文件数（基线 115 = 1 demo + 114 生产）；未传 density/size 页面渲染与基线一致（resolvedDensity='default'、densityClass=''、行高 `var(--fts-space-3)`=12px、无 table-density-* 类）——零外观突变
  2. 读码核验 layout.ts：tableDensity 为唯一真相源（ref + localStorage）；compactMode 归一为 writable computed（get: tableDensity==='compact'；set: setCompactMode）；旧用户迁移（仅存 compactMode=true → 初始 'compact'）；applyTableDensity 同步驱动 html.table-density-* + html.compact-mode；全仓 compactMode 消费方仅 2 处（定义 + MainLayout 只读绑定），无双向写残留
  3. demo 位（ComponentGallery.vue density="auto"，QA 联动验证位）三档切换：compact/default/comfortable 行高随 tableDensity 真实变化（4px/12px/16px）
  4. 3 处 size="small"（AssetReport.vue:627 / InventoryLocation.vue:467,508）经 core 声明 prop 生效（small→compact 映射 → 组件级 4px 行高），调用方零改动
  5. 默认状态核验：compactMode off + tableDensity default → 渲染与基线零变化（默认状态零变化断言）
- **预期结果**：
  - 默认关闭：density/size 默认 undefined → 115 消费方未 opt-in 渲染不变（默认 12px 行高、无密度类、无渲染联动）
  - 唯一真相源：compactMode 为 computed 归一，无独立写路径；双开关不再互相打架
  - 真联动：density='auto' 三档切换真实生效；3 处 size="small" 紧凑行高真实生效（P2-CORE-009 闭环）
  - 无接口/无规则：零后端接口新增、零业务规则猜测（密度收敛为技术重构，架构裁决授权范围内）
- **测试类型**：手工回归（代码级读码/grep/diff + demo 联动验证位）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 1（QA R-2，紧凑收敛副产物）：旧组合「紧凑 ON + 行高宽松」不再可能（两开关联动，架构裁决授权）；「紧凑 ON + 三档默认」行高 6px→4px 微调——变化仅影响已开启紧凑模式的用户，方向「更紧凑」且幅度 2px，默认状态（compactMode off + tableDensity default）零变化；developer 已透明声明（任务卡证据块），QA 判定为流程瑕疵而非功能缺陷；后续回归执行含紧凑模式场景时留意（回归执行时点：紧凑 ON 行高 4px）
  - 观察 2（QA R-3）：DataTable 未 opt-in 时无条件 `useLayoutStore()`（DataTable.vue:80）——pinia 单例无副作用，不改变渲染，仅轻微实例化开销
  - 观察 3（QA R-1，已登记限制）：全局 html.table-density-compact/comfortable 以 !important 覆盖 .el-table__cell（_base.scss:203-217 既有机制，非本批改动）——store 非 default 时组件级密度（size="small" 页面）被全局类优先覆盖，与既有行为一致

---

### REG-UI-CORE-002

- **测试编号**：REG-UI-CORE-002
- **模块**：core-SearchPanel 筛选保存（frontend/src/components/core/SearchPanel.vue）
- **关联任务**：P1-UI-CORE-002（Batch CORE-1；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-core-b1-qa-report.md`）
- **业务场景**：SearchPanel storageKey 默认 undefined（零持久化）+ fts_search_{key} 保存/恢复/reset 清空契约路径（page-scoped 页间不串）
- **测试目的**：防止死组件契约误用与筛选状态丢失回归 —— storageKey 未传时必须零 localStorage 读写（0 消费方现状零影响）；传入时必须 page-scoped 保存/恢复/reset 清空可用、页间不串；契约（props/emits/持久化语义）一次到位供财务 4 页（P1-UI-FIN-001~004）复用。
- **测试步骤**：
  1. grep 全仓 SearchPanel 生产消费方（基线 0，仅组件自身注释与 demo 无引用）；storageKey 未传时三条路径（saveConditions/onMounted/handleReset）全部短路、零 localStorage 读写
  2. 契约核验（L1-32 组件头契约块）：props（modelValue/showAdvanced/storageKey）+ emits（update:modelValue/search/reset）文档化；key 规范 `fts_search_{key}`（L56-58）page-scoped
  3. demo/单测位验证契约路径：传 storage-key 后条件变化 → watch deep 落盘（L70）；刷新/重进 → onMounted 恢复回写 v-model（L73-83）；reset → removeItem 清空存储 + emit('reset')（L90-100）
  4. 无接口调用断言：SearchPanel grep `http|axios|request|fetch|XMLHttpRequest` 零匹配（零后端接口新增）
- **预期结果**：
  - 默认零行为：storageKey undefined → 无保存/恢复/清空任何副作用；0 消费方现状不破坏
  - 契约一次到位：page-scoped key 页间不串；保存=watch deep 即时落盘、恢复=挂载回写、清空=reset 删 key（刷新后不再恢复旧条件）
  - 降级安全：localStorage 不可用（隐私模式/配额）时三处 try/catch 静默降级，不影响筛选功能
- **测试类型**：手工回归（代码级读码/grep + 契约核验）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 1（QA R-4，SearchPanel reset 语义边界）：reset 删除存储后，父组件若将 modelValue 清空（置 {}），watch deep 会再次触发 saveConditions 写入空对象——刷新后恢复空条件，行为等价「不恢复旧条件」；若父组件 reset 后保留非空默认值，默认值将被持久化。契约文档（L19-23）已写明「reset 由父组件清空表单」的语义边界，属父组件职责范围，不构成缺陷；**第三步接入时由页面按契约实现**（P1-UI-FIN-001~004 承接，处置不在本批）
  - 观察 2（QA R-5）：localStorage 不可用时三处 try/catch 静默降级，不影响筛选功能（与 developer 声明一致）

---

### REG-UI-CORE-003

- **测试编号**：REG-UI-CORE-003
- **模块**：core-DataTable/TableActionBar 批量动作 + 列显隐 + N/M 部分成功反馈（TableActionBar.vue / DataTable.vue / BatchResultFeedback.vue）
- **关联任务**：P1-UI-CORE-003（Batch CORE-1；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-core-b1-qa-report.md`）
- **业务场景**：TableActionBar 列显隐真实触发（column-change/update:columns）+ DataTable batchActions 默认 false（opt-in 批量条）+ BatchResultFeedback 纯展示零接口调用 + @selection-change 既有消费方不破坏
- **测试目的**：防止列显隐装饰性假功能与批量复选框死控件回归 —— 勾选列显隐必须真实生效（emit 事件供父组件过滤列）；未 opt-in 页面不得渲染批量条（复选框列渲染与现状一致）；BatchResultFeedback 不得引入任何接口调用/业务规则判断；@selection-change 既有消费方（SmartRestock/InventoryLocation 等）能力不得破坏。
- **测试步骤**：
  1. 读码核验 TableActionBar：localColumns 内部副本（默认全部可见，不直接改 props）+ watch props 深同步（外部重置）+ handleColumnVisibleChange 真实触发 `column-change`（可见 prop 数组）+ `update:columns`；checkbox v-model 绑定副本 + @change + @click.stop.prevent（L119-124）
  2. 读码核验 DataTable：batchActions 默认 false（L50-51,67）；批量条 `v-if="batchActions && selection.length > 0"`（已选 N 项 + #batch-actions 插槽 scope: selection/clear-selection + 取消选择，L126-138）；clearSelection 调 el-table 实例方法（L101-111）
  3. BatchResultFeedback 纯展示断言：仅接收 result（BatchResult：total=M/success=N/failed 列表）渲染，成功/部分成功两种视觉（allSuccess 判定）+ 失败项明确列出 + close 事件；grep 四文件 `http|axios|request|fetch|XMLHttpRequest` 零匹配（零接口调用、零业务规则判断）
  4. @selection-change 消费方不破坏：handleSelectionChange 先更新内部 selection 再 emit('selection-change', sel)（转发参数与旧实现一致）；selectable 默认 true 保持（复选框列渲染不变）；grep 58 行匹配（含 DataTable.vue:155 组件内部转发 1 行 + 57 页面消费方，SmartRestock.vue:475,522 等）
  5. 接口不支持项核验：core 零新增批量请求封装（无既有批量端点契约）；批量操作由页面基于既有单条/批量接口执行，接口不支持项在第三步逐页核对登记（任务卡证据块 L2690）
- **预期结果**：
  - 列显隐真实生效：勾选 → 副本 visible → emit 两事件 → 父组件过滤列；旧装饰性假功能（:model-value 无 @change）不回归
  - 批量条 opt-in：未传 batchActions 页面批量条不渲染（v-if 双重短路）、复选框列渲染与现状一致；grep 生产消费方 batchActions/batch-actions = 0（仅 demo 演示位）
  - N/M 纯展示：BatchResultFeedback 零接口调用、零业务规则判断；失败原因由页面基于既有接口错误传入
  - 不破坏：@selection-change 既有 57 页面消费方能力保持；selectable 默认 true 不变
- **测试类型**：手工回归（代码级读码/grep + demo 验证位）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 1（QA R-6，数字口径）：developer 证据「全仓 58 处 @selection-change 消费方」实为 58 行匹配（含 DataTable.vue:155 组件内部转发 1 行），页面消费方 57 处——口径微差无实质影响（关键是不破坏，已核验）
  - 观察 2（交接项，QA 报告 §五.1）：DataTable.vue:188 操作列 `fixed="right"` 经 git diff 为工作区**既有未提交改动**（盘点报告 §2.1 记为既有能力，属盘点前已存在状态），**非本批证据范围**；既有基线已覆盖该状态，仅登记归因，处置不归本批
  - 观察 3（交接项，QA 报告 §五.2）：全仓 typecheck 存量 136 条错误（非本批引入，本批 7 文件零错误），建议后续按文件域拆分任务治理——登记建议不销号
  - 观察 4（QA R-7）：N/M 组件依赖页面传入正确 result（total/success/failed），core 不校验业务语义（纯展示定位，符合卡约束）

---

## Batch CORE-2 回归（core 增强批次，2026-08-31）

> 验收依据：`docs/quality/ui-core-b2-qa-report.md`（2026-08-31，QA 独立验收：P1-UI-CORE-004/005/006 三卡 PASS、P1-UI-CORE-007 FAIL→P1-UI-CORE-007-R1 返回 DOING）+ **`docs/quality/ui-core-b2-r1-qa-report.md`（2026-08-31，P1-UI-CORE-007-R1 复验 PASS，F-1 闭环）**；强制核验点 6 项：向后兼容 ✅（115 消费方实测）、零后端接口 ✅、视觉同源 ✅（R1 闭环后 F-1 例外消除）、金额口径 ✅、构建 ✅（typecheck 136=136 独立复跑一致）、开发者未自行宣布通过 ✅。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-UI-CORE-004/005/006（首轮，维护规则 5 禁止推测）+ **REG-UI-CORE-007（P1-UI-CORE-007-R1 复验 PASS 闭环后转正固化）**；既有条目零修改。
> 批次口径：本批为**治理批次**（第二步 core 增强，任务池 §9.2），非发布批次——回归结论 **PASS**（2026-08-31 R1 闭环后由 CONDITIONAL PASS 转正）表示 4/4 卡基线持续积累（正式基线 76 → 77 条）、正式基线 FAIL=0 维持；不构成上线门禁判定；第三步财务 4 页接入（P1-UI-FIN-001~004）执行前置 004/005/006 已满足（任务池 L2513 + QA 报告 §四），P1-UI-FIN-001 约束收窄为仅剩 **PD-028** 单项（007-R1 约束已解除）。
> 基线计数核对：追加前正式基线 **73 条**（文件实况）→ 首轮追加 3 条（REG-UI-CORE-004/005/006）后 **76 条** → **R1 闭环转正追加 REG-UI-CORE-007 后 77 条**，只增不减纪律达成（既有 76 条条目零修改，含首轮 3 条与历史 73 条）；候选 0 条维持；P1-UI-CORE-007 经 FAIL → -R1 → QA 复验 PASS（2026-08-31）闭环后转正（76 → 77）。

### REG-UI-CORE-004

- **测试编号**：REG-UI-CORE-004
- **模块**：core-DataTable 行内展开（frontend/src/components/core/DataTable.vue）
- **关联任务**：P1-UI-CORE-004（Batch CORE-2；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-core-b2-qa-report.md`）
- **业务场景**：DataTable expandable 默认 false opt-in + #expand 插槽（type="expand" 列），生产消费方 0，未 opt-in 渲染不变
- **测试目的**：防止 expand 行内展开能力回归 —— 未传 expandable 必须零展开列渲染（115 消费方与旧版等价）；opt-in 页面必须获得 type="expand" 列 + #expand 插槽（scope: row/$index，行内展开/收起不跳转路由）；与 treeProps 树形数据由 el-table 原生并存不冲突。
- **测试步骤**：
  1. grep 全仓 `expandable`：生产消费方 0（仅 DataTable.vue:57 prop 声明 + ComponentGallery.vue:112 demo 演示位；HROrganization.vue:111 为**本地函数名** `collectAllExpandableIds`，与 DataTable prop 无关）——115 消费方（1 demo + 114 生产）未 opt-in 时模板 `v-if="expandable"` 短路，展开列零渲染
  2. 读码核验 DataTable.vue:57,88：`expandable?: boolean` 默认 false（withDefaults L88）；模板 227-236 `<el-table-column v-if="expandable" type="expand" width="48">` + `<slot name="expand" :row="scope.row" :$index="scope.$index" />`
  3. demo 验证位（ComponentGallery.vue:108-126，QA 验证位非生产）：`expandable` + `#expand` 模板渲染成立；展开/收起由 EP 原生 expand 列承载（点击展开图标行内展开，不跳转路由）
  4. treeProps 并存核验：DataTable.vue:210 `:tree-props="treeProps"` 为 el-table 原生数据结构配置（children/hasChildren），展开列为独立列类型（type="expand"），两者由 el-table 原生并存（git diff 归因本批无 tree 改动）
- **预期结果**：
  - opt-in：传 expandable 后获得展开列（宽 48px，EP 默认，位于选择列之后、数据列之前），#expand 插槽提供 row/$index，行内展开/收起不跳转路由
  - 默认关闭：expandable 未传 → 展开列零渲染，115 消费方渲染与旧版等价（零外观突变）
  - 并存：与 treeProps 树形数据由 el-table 原生并存，core 无自定义冲突逻辑
  - 无接口/无规则：零后端接口新增、零业务规则猜测
- **测试类型**：手工回归（代码级读码/grep/diff + demo 验证位）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 1（QA R-1）：展开列固定宽 48px（EP 默认展开图标列宽），opt-in 页面接入时需核对布局；与选择列并存时展开列位于选择列之后（EP 原生顺序），语义无冲突——处置归 opt-in 页面接入时（第三步）
  - 观察 2（QA R-2）：本卡为 core 能力提供，Ledger 分录接入属第三步（P1-UI-FIN-002），不在本卡范围（任务卡证据块已声明）

---

### REG-UI-CORE-005

- **测试编号**：REG-UI-CORE-005
- **模块**：core-DataTable 合计行 + useSummary（frontend/src/components/core/DataTable.vue + frontend/src/composables/useSummary.ts）
- **关联任务**：P1-UI-CORE-005（Batch CORE-2；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-core-b2-qa-report.md`）
- **业务场景**：DataTable showSummary 默认 false + summaryMethod 默认 core 方法（首列「合计」其余空白）+ useSummary 仅复用 `@/utils/money`（fenToYuanNumber/fenToYuanDisplay/yuanToFen，零自造格式化）
- **测试目的**：防止合计行口径漂移与金额格式化自造回归 —— 未传 showSummary 必须零合计行（115 消费方无突变）；未传 summaryMethod 必须走 core 默认方法（首列「合计」其余空白，杜绝 Element Plus 默认数值求和口径漂移）；createAmountSummaryMethod 金额千分位必须复用 `@/utils/money` 既有 Converter 口径（fenToYuanDisplay 千分位 + 2 位小数），useSummary 全文禁止自造格式化（toLocaleString/toFixed/toPrecision/Intl 零匹配）。
- **测试步骤**：
  1. grep 全仓 `show-summary`/`summary-method`：DataTable 生产消费方 0（仅 2 处裸 el-table 既有使用 PurchaseOrder.vue:1346-1347 / ArrivalDetailDialog.vue:357-358，属盘点登记迁移候选不强制）——115 消费方 show-summary=false 零合计行
  2. 读码核验 DataTable.vue:59-63,89-90,213-214：`showSummary?: boolean` 默认 false、`summaryMethod?: SummaryMethod` 默认 undefined，透传 el-table `:show-summary`/`:summary-method`；173-175 core 默认方法 defaultSummaryMethod（首列「合计」、其余列空白，不做无声明数值求和）；177 `resolvedSummaryMethod = summaryMethod ?? defaultSummaryMethod`
  3. 读码核验 useSummary.ts:31：仅 import `@/utils/money`（fenToYuanNumber/fenToYuanDisplay/yuanToFen，在 utils/money.ts:44-66/98-104 实存）；44-59 summarizeAmount（`value == null || value === '' || Number.isNaN(Number(value))` 跳过 + fen 路径 `fenToYuanDisplay(total)` / yuan 路径 `fenToYuanDisplay(yuanToFen(total))` 双路径）；76-85 createAmountSummaryMethod（生成 EP summary-method，首列 label、amountProps 列千分位合计、其余空白）
  4. 口径断言：useSummary.ts 全文 `toLocaleString|toFixed|toPrecision|Intl\.` **零匹配**（自造格式化为零）；demo 验证位（ComponentGallery.vue:279-283 `createAmountSummaryMethod({ amountProps: ['incomeAmount','expenseAmount'], unit: 'yuan', label: '合计' })`）推演：12800.5+2560.75=15361.25 → yuanToFen=1536125 → fenToYuanDisplay="15,361.25" 千分位口径成立
  5. 输入约束契约：useSummary.ts:41-42 JSDoc（无有效数值时 "0.00"）；行内数值须为 number（元/分），已格式化字符串（如 "1,234.56"）Number() 解析为 NaN 被跳过 → 全部跳过 total=0 → "0.00"；契约注释已写明「第三步接入时须传原始数值行」
- **预期结果**：
  - 默认关闭：未传 showSummary → 无合计行突变；未传 summaryMethod → core 默认方法确定性展示（首列「合计」其余空白，不做无声明数值求和）
  - 金额口径：格式化全部走 `@/utils/money` 既有 Converter（千分位 + 2 位小数既有口径），零自造 toLocaleString/toFixed/toPrecision/Intl
  - 无接口/无规则：useSummary 纯前端计算（零后端接口新增）；格式化实现全部在 core，页面禁止各自实现（验收标准 4：裸 el-table 2 处迁移候选仅登记不强制）
- **测试类型**：手工回归（代码级读码/grep + demo 金额推演验证位）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 1（QA R-3，O-1）：useSummary.ts:31 `fenToYuanNumber` import 未直接使用（口径来源声明用途）——vue-tsc 零错误（无 noUnusedLocals 报错）、无功能影响，仅轻微冗余，不阻塞
  - 观察 2（QA R-4）：裸 el-table 2 处（PurchaseOrder/ArrivalDetailDialog）为迁移候选，本卡仅登记不强制（验收标准 4 ✅）
  - 观察 3（契约边界）：summarizeAmount 要求行内数值为 number（元/分）；第三步接入时须传原始数值行（属页面数据源选择，已写入 composable 契约注释，处置归第三步）

---

### REG-UI-CORE-006

- **测试编号**：REG-UI-CORE-006
- **模块**：core-DataTable 固定列 + useStandardPage 分页封装（DataTable.vue / useStandardPage.ts / DishCostAnalysis.vue）
- **关联任务**：P1-UI-CORE-006（Batch CORE-2；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-core-b2-qa-report.md`）
- **业务场景**：固定列能力核验（fixed 接口透传 + 操作列 fixed="right" 已具备）+ useStandardPage pageSizes 默认 [10,20,50,100] + handleSizeChange/handleCurrentChange + DataTable pagination 默认 undefined 零渲染 + DishCostAnalysis 静默失效→真实分页（修复性行为变化）
- **测试目的**：防止分页封装假功能与固定列能力丢失回归 —— pagination 未传必须零分页器渲染（114 消费方现状不变）；opt-in 页面必须获得 sizes 下拉/每页条数切换/总数感知（layout: total,sizes,prev,pager,next,jumper + background）；useStandardPage 必须提供 pageSizes 默认 [10,20,50,100] + handleSizeChange（size 更新+回第一页+onPageChange 回调）/handleCurrentChange；DishCostAnalysis 必须维持真实分页（size 契约对齐、total 感知、@page-change 闭环）；固定列接口（fixed 透传 + 操作列 fixed="right"）能力必须存在。
- **测试步骤**：
  1. 固定列能力核验（零改动）：DataTable.vue:19 列接口 `fixed?: 'left'|'right'`、245 `:fixed="col.fixed"` 透传、258 操作列 `fixed="right"`——core 能力已具备（对照盘点报告 §2.1 L17/L117/L130，行号偏移因 CORE-1+本批累计改动使文件 353→542 行，能力对应关系逐一核验一致；DishCostAnalysis.vue:407 已有 fixed: 'left' 使用实例）
  2. 读码核验 useStandardPage.ts:77-78（options.pageSizes）、188-189（默认 [10,20,50,100]）、100（返回值 pageSizes）、200-204（handleSizeChange：size 更新 + 回第一页 + onPageChange）、210-213（handleCurrentChange：current 更新 + onPageChange）——纯新增字段/方法（options 解构 L149-156），既有 14 个 finance 消费方解构不受影响（未新增必选参数）；onPageChange 为可选（`?.` 调用），不传零行为变化
  3. 读码核验 DataTable.vue:69-71,91-92（pagination 默认 undefined + pageSizes 默认 [10,20,50,100]）、148-155（effectivePageSizes：当前 size 不在列表自动并入排序）、157-170（handlePageSizeChange 回第一页 / handlePageCurrentChange）、271-282（`v-if="pagination"` 底部 el-pagination：layout `total, sizes, prev, pager, next, jumper` + background）、99（emit page-change）
  4. grep 全仓：`:pagination="` 生产消费方**仅 DishCostAnalysis.vue:89**（+ demo ComponentGallery.vue:115）；`@page-change` 仅 DishCostAnalysis.vue:93（+ demo）——其余 114 消费方 v-if 短路零渲染
  5. DishCostAnalysis 行为核验：420 `reactive<StandardPagination>({ current: 1, size: 20, total: 0 })`、423-426 handlePaginationChange（同步 current + fetchData）、469 fetchData 传 `page: paginationConfig.current, size: paginationConfig.size`、471 `paginationConfig.total = res.total`；git diff 归因：before = `computed(() => ({ total, current, pageSize }))` + 手工 currentPage/pageSize/total 三状态 + `:pagination` 为 DataTable 未声明 prop → fallthrough 静默失效（无分页器、无翻页能力）；after = reactive 单源（pageSize→size 契约漂移修复）+ @page-change 闭环 + fetchData 单源——**修复性行为变化**（同 CORE-1 P2-CORE-009 先例口径），非破坏性变更；resetSearch/handleSearch/handleStatusFilterChange 全部收敛为 `paginationConfig.current = 1`（diff 确认）
- **预期结果**：
  - opt-in：传 pagination 后获得分页器（layout: total,sizes,prev,pager,next,jumper + background），sizes 下拉 [10,20,50,100]，size 切换回第一页，page-change 事件驱动数据刷新，total 响应式驱动 `:total`
  - 默认关闭：pagination 未传 → 分页器零渲染，114 消费方现状不变（未 opt-in 零行为变化）
  - 契约：DataTable 直接写 pagination reactive 对象属性（单一真相源传入，prop JSDoc 已文档化）；useStandardPage 状态复用（current/size/total 单源，无重复状态）
  - 修复性变化：DishCostAnalysis 从「静默失效」恢复为真实分页（sizes 下拉/总数感知/翻页闭环），无破坏性变更
  - 无接口/无规则：零后端接口新增（分页复用既有接口参数 page/size，fetchData 未改接口契约）
- **测试类型**：手工回归（代码级读码/grep/diff + demo 验证位）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 1（QA R-5，O-2）：DishCostAnalysis 用手工 `reactive<StandardPagination>` 而非 `useStandardPage()` 推荐路径——功能行为正确（结构/响应式等价）、当前无重复状态、无双真相源漂移，但与 prop JSDoc「传入 useStandardPage 返回的 pagination」推荐路径有偏离；登记观察项（第三步或后续可收敛）
  - 观察 2（QA R-6，O-3）：该页 `statusFilter` 为前端过滤（L428-433），分页 total 来自后端（L471）——设置 statusFilter 后前端过滤行数可能少于分页 total（翻页出现空页边界）。此为**既有页面设计**（CORE 前亦存在前端过滤逻辑），非本批引入；本批行为变化（无分页器→有分页器）暴露该既有关系，第三步页面接入时核对

---

### REG-UI-CORE-007

- **测试编号**：REG-UI-CORE-007
- **模块**：core-StatusTag 语义键 + tokens 状态色（frontend/src/styles/global/_tokens.scss + frontend/src/styles/global/_dark-mode.scss + frontend/src/components/core/StatusTag.vue）
- **关联任务**：P1-UI-CORE-007（Batch CORE-2；首轮 FAIL → P1-UI-CORE-007-R1 → **QA 复验 PASS 2026-08-31** `docs/quality/ui-core-b2-r1-qa-report.md`，F-1 闭环）
- **业务场景**：StatusTag overdue/abnormal 语义键深浅双主题渲染——浅色 :root 值（overdue #bf360c/#fbe9e7/#ffccbc、abnormal #ad1457/#fce4ec/#f8bbd0）+ 深色 html.dark 值（overdue #ff7043/#33140a/#5a2a12、abnormal #f06292/#2a0f1e/#5a2240）与既有键同构渲染（color/bg/border 三元组 + purple 先例边框块复声明）；既有 12 键零变化（消费方外观不变）
- **测试目的**：防止状态语义键深浅主题适配回退 —— 深色主题下 overdue/abnormal 必须命中 html.dark 块深色三元组（不得回退浅色值 #bf360c/#fbe9e7 等）、与既有键深色适配同构；浅色值必须维持验收值；light/solid 变体 6 变量（color/bg/border ×2）零悬空；既有键与边框块不得被本卡改动触碰。
- **测试步骤**：
  1. 读码核验 `_dark-mode.scss` html.dark 块 L137-143 + L154-155：overdue 三元组 `--fts-status-overdue-color: #ff7043` / `--fts-status-overdue-bg: #33140a` / `--fts-status-overdue-border: #5a2a12`、abnormal 三元组 `--fts-status-abnormal-color: #f06292` / `--fts-status-abnormal-bg: #2a0f1e` / `--fts-status-abnormal-border: #5a2240`；边框块复声明（L154-155）同值——与 purple 先例（L125-127 + L152）同构；色值 8/8 与 QA 复验记录一致
  2. 模式同构核验：overdue 深橙 #ff7043 ≠ error 红 #ef5350 ≠ orange 琥珀 #ffab6e；abnormal 玫红 #f06292 ≠ error 红——色相分离与浅色语义（深橙/玫红）一致
  3. 覆盖冲突检查：grep 全仓 `fts-status-(overdue|abnormal)` 仅命中 _dark-mode.scss（html.dark 块）+ _tokens.scss（:root 浅色块）+ StatusTag.vue（var() 引用），无第三处覆盖源（frontend-design-v3 零匹配）
  4. 变体变量齐备：light 变体（StatusTag.vue:316-328 `&--overdue`/`&--abnormal` 引用 color/bg/border + dot 用 color）与 solid 变体（L374-375 仅用 -color）在深浅两源文件全部有定义，零悬空 var
  5. 浅色源零改动：_tokens.scss:124-129 浅色 6 值维持验收值；StatusTag.vue 零 R1 改动（git diff 归因：R1 唯一改动文件 = _dark-mode.scss 10 行）
  6. 构建产物静态核验：独立 build 产物中 6 深色值位于 html.dark 块内（括号配平定位，块尾 EP 变量上下文证明）、:root 浅色 6 值未变；typecheck 136=136 零新增、build EXIT=0
- **预期结果**：
  - 深色主题：overdue/abnormal 命中 html.dark 深色三元组，与既有键同构渲染（不回退浅色值）
  - 浅色主题：:root 浅色 6 值维持验收值，StatusTag 组件零改动
  - 既有键零变化：default/active/inactive/probation/pending/success/error/orange/warning/purple/info/primary/danger 既有键与边框块未被本卡改动触碰
  - 无接口/无规则：零后端接口新增；PD-028「待核销」子项 BLOCKED 不占通过数（见限制说明）
- **测试类型**：手工回归（代码级读码/grep/diff + 构建产物静态核验 + typecheck/build 独立复跑）
- **当前状态**：PASS（2026-08-31，R1 复验闭环转正）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：来源任务含资金流水「待核销」状态语义缺失子项（决策池 `product-decision-backlog.md:44/155`）——决策前不实现不猜测、不销号；本条目仅覆盖 overdue/abnormal 语义键 + 色值（与 PD-028 无关部分先行），限制观察登记，影响面=第三步 P1-UI-FIN-001（见「PD-028 限制登记同步」节）
  - 观察 1（QA R-1）：深色新色值（#ff7043/#f06292 等）为 developer 按既有深色系亮度档手工选取，**未做 WCAG 对比度工具实测**（如实声明）——验收标准为「与既有键同构」（结构/模式 ✅）+ 色相分离 ✅，不违反验收标准；对比度实测建议后续可访问性专项统一治理
  - 观察 2（QA R-2）：边框块复声明（L154-155）与主块（L140/143）同值冗余——与 purple 先例（L127/L152）模式一致，CSS 覆盖无害；后续可随 purple 一并去重（共 3 组）
  - 观察 3（QA R-4）：本验收为静态验收（读码 + 构建产物 + typecheck/build），未做浏览器运行时视觉截图比对——深色渲染结论基于 CSS 变量解析链（html.dark 块覆盖 → var() 引用）静态推演，与既有键同构（既有键深色适配已验证多年运行）风险低；如需运行时验证可后续补断言

---

### P1-UI-CORE-007 处理登记（FAIL → -R1 → R1 复验 PASS 闭环 → 转基线 REG-UI-CORE-007）

- **首轮验收结论**：**FAIL**（`docs/quality/ui-core-b2-qa-report.md`，2026-08-31）——失败点 F-1：`_dark-mode.scss:105-145`（html.dark 统一深色适配块）覆盖全部既有 status 变量但**无 `--fts-status-overdue-*` / `--fts-status-abnormal-*` 深色值**，深色主题下新键回退 :root 浅色值（#bf360c/#fbe9e7 等），与既有键深色适配（暗色背景体系）不同构，违反验收标准 2「与既有键同构」；developer 证据声称「深浅主题自动适配」与实现不符。
- **处理**：生成 **P1-UI-CORE-007-R1** 返回 developer DOING——在 `frontend/src/styles/global/_dark-mode.scss` 的 `html.dark` 块补充 `--fts-status-overdue-color/bg/border` 与 `--fts-status-abnormal-color/bg/border` 深色值（参照 L105-145 既有键模式：深色前景 + 暗色背景 + 深色边框）；修复后 QA 再次独立验收（QA 报告为唯一验收结论）。
- **R1 复验结论（闭环）**：**PASS**（`docs/quality/ui-core-b2-r1-qa-report.md`，2026-08-31）——深色值 8/8 一致（L137-143 + L154-155：#ff7043/#33140a/#5a2a12、#f06292/#2a0f1e/#5a2240）+ 与既有 12 键同构（color/bg/border 三元组 + purple 先例边框块复声明）+ light/solid 变体 6 变量全覆盖零悬空 + 浅色 _tokens.scss/StatusTag.vue 零改动（R1 唯一改动文件 = _dark-mode.scss）+ typecheck 136=136 + build EXIT=0 + 构建产物 6 深色值 in html.dark block + PD-028 未触碰（F-1 闭环，覆盖首轮 FAIL 判定）。
- **基线登记**：**REG-UI-CORE-007 已转正固化**（见上节条目，正式基线 76 → 77）——回归行为口径（依据 QA 复验 PASS 结论固化）：StatusTag overdue/abnormal 语义键 + tokens 状态色（浅色 :root + 深色 html.dark 同构：深色主题下渲染与既有键一致）+ 既有键零变化（消费方外观不变）。
- **与 PD-028 关系**：本卡 FAIL（F-1 深色适配实现缺陷）与 PD-028 限制（「待核销」BLOCKED 业务规则缺失）**相互独立**——QA 报告已核验 PD-028「待核销」子项正确停止（FinanceFund.vue 零修改、statusConfig 无待核销键、登记链完整），R1 复验再次确认 PD-028 未触碰（R1 修改文件仅 _dark-mode.scss）；007-R1 约束已解除，P1-UI-FIN-001 仅剩 PD-028 单项约束（决策前不实现不猜测、不销号）。

---

### PD-028 限制登记同步（BLOCKED，决策前不实现）

- **登记链**：任务卡证据块（`production-remediation-task-board.md` §9.2 L2846）→ 决策池登记（`product-decision-backlog.md:44/155`，2026-08-31）→ roadmap 引用（`remediation-roadmap.md` §5.10 L253）。
- **内容**：资金流水「待核销」状态语义缺失——后端 `FundFlow` 实体/VO/`fund_flows` 表均无 status 字段，前端 `FundFlowStatusMap`（converters.ts:256-267）「后端 1~3 ↔ pending/completed/cancelled」为悬空声明，页面流水状态实际恒为「未知」（FinanceFund.vue:35-42 兜底）；属 BLOCKED_PRODUCT_RULE，待产品/财务决策（决策面：fund_flows 是否补 status 字段 / 后端判定 vs 前端派生），决策前不实现不猜测。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-UI-CORE-004/005/006/007）**不受影响**（均与「待核销」语义无关）；影响面 = 第三步 **P1-UI-FIN-001 异常状态醒目接入**（资金流水页）——P1-UI-CORE-007-R1 已复验 PASS 闭环（2026-08-31，007-R1 约束解除），**仅剩产品决策（PD-028）单项约束**（QA 报告 §四）；决策前不实现不猜测、不销号。

---

### QA 观察项登记（Batch CORE-2，如实登记不销号、不猜测处置）

按 QA 报告 §五「遗留观察项」+ R1 复验报告 §三/§五「风险与限制 / 遗留观察项」如实登记（回归角色不猜测处置、不销号）：

1. **O-1**：useSummary.ts:31 `fenToYuanNumber` import 未直接使用（口径来源声明用途）——无类型错误、无功能影响，仅轻微冗余（同 REG-UI-CORE-005 观察 1）。
2. **O-2**：DishCostAnalysis 分页用手工 `reactive<StandardPagination>` 而非 `useStandardPage()` 推荐路径——行为正确（响应式等价），第三步或后续可收敛（同 REG-UI-CORE-006 观察 1）。
3. **O-3**：DishCostAnalysis `statusFilter` 前端过滤与后端分页 total 的既有设计关系（CORE 前已存在）——本批行为变化（静默失效 → 真实分页）将其暴露，第三步页面接入时核对（同 REG-UI-CORE-006 观察 2）。
4. **行号偏移说明**：本批证据行号以当前文件实况为准（DataTable 353→542 行为 CORE-1+本批累计改动所致）；盘点报告 §2.1 行号（L17/L117/L130）为盘点时快照，能力对应关系已逐一核验一致。
5. **StatusTag.vue 4 条存量类型错误**（primary/orange/danger/default）为历史遗留（本批前存在，git diff 归因本批未触碰 L70-73），建议后续按类型域治理（不属本批）。
6. **PD-028**（见上节）：BLOCKED 限制登记，决策前不实现不猜测，影响第三步 P1-UI-FIN-001（本批基线不受影响）。
7. **迁移候选**：裸 el-table 2 处（PurchaseOrder.vue:1346-1347、ArrivalDetailDialog.vue:357-358）show-summary/summary-method 既有使用，本批仅登记不强制（验收标准 4）。
8. **R-1（R1 复验报告，WCAG 对比度未实测）**：深色新色值（#ff7043/#f06292 等）为 developer 按既有深色系亮度档手工选取，未做 WCAG 对比度工具实测——不违反验收标准（验收标准 2=与既有键同构 ✅ + 色相分离 ✅），建议后续可访问性专项统一治理（同 REG-UI-CORE-007 观察 1）。
9. **R-2（R1 复验报告，边框块复声明冗余）**：overdue/abnormal（L154-155）与 purple（L152）共 3 组同值双声明——与 purple 先例模式一致、CSS 覆盖无害；后续可随 purple 一并去重（同 REG-UI-CORE-007 观察 2）。
10. **R-4（R1 复验报告，静态验收范围说明）**：007-R1 复验为静态验收（读码 + 构建产物 + typecheck/build），未做浏览器运行时视觉截图比对——深色渲染结论基于 CSS 变量解析链（html.dark 块覆盖 → var() 引用）静态推演、与既有键同构风险低；如需运行时验证可转基线条目补断言（同 REG-UI-CORE-007 观察 3）。

---

## Batch FIN-1 回归（第三步·财务 4 页第一批，2026-08-31）

> 验收依据：`docs/quality/ui-fin-b1-qa-report.md`（2026-08-31，QA 独立验收：P1-UI-FIN-001/002 两卡全 PASS、无 FAIL、无 -R；强制核验点：业务语义零变更 ✅（git diff API 调用集合 3 处一致）/ Batch0-方案3 对齐 5 点全核验一致 ✅（后端 0-3 实锤 / VoucherStatusMap 双向正确 / 错档实锤 / 按钮逻辑零改动 / 后端契约不变）/ 视觉同源 ✅（两页面 + 4 文件 grep 硬编码颜色零匹配、frontend-design-v3 零引用）/ 构建 ✅（typecheck 136=136 独立复跑一致 + build EXIT=0）/ 开发者未自行宣布通过 ✅（roadmap §5.10 L247「QA 独立验收中」核对一致））。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-UI-FIN-001/002（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（第三步·财务 4 页第一批，任务池 §9），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；Batch FIN-2（P1-UI-FIN-003 应收 / 004 应付）待本批全链闭环后执行（roadmap §5.10 批次节奏）。
> 基线计数核对：追加前正式基线 **77 条**（文件实况：基线总览表 77 行，含 REG-UI-CORE-001~007；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 2 条（REG-UI-FIN-001/002）后 **79 条**，只增不减纪律达成（既有 77 条条目零修改）；候选 0 条维持。

### REG-UI-FIN-001

- **测试编号**：REG-UI-FIN-001
- **模块**：财务-资金流水（frontend/src/views/finance/FinanceFund.vue）
- **关联任务**：P1-UI-FIN-001（Batch FIN-1；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-fin-b1-qa-report.md`）
- **业务场景**：资金流水页接入 core 能力——固定列（日期 width 110 / 摘要 width 180 fixed left 含 showOverflowTooltip、余额 width 120 fixed right，L82-91）、密度 `density="auto"`（L278，消费 layoutStore.tableDensity core 001 唯一真相源）、筛选保存（`<SearchPanel storage-key="finance-fund-filter">` L254-271 + handleFilterRestored/restoreDateRange L97-113，page-scoped localStorage）、行内详情 expand（L279,290-307，#expand 插槽承载 el-descriptions 同字段，与移除弹层完全同字段）、分页+合计（`:pagination` L282 core 006 + `show-summary` + `createAmountSummaryMethod({ amountProps:['income','expense'], unit:'yuan' })` L95 复用 core useSummary）；业务语义零变更（API 调用集合与改动前一致 3 处：bankAccountApi.getList L137 / fundFlowApi.getStatisticsByAccount L157 / fundFlowApi.getList L187）；PD-028 待核销标注不实现（限制观察）；批量不支持项保持现状登记（fund-flow.ts 无批量端点）
- **测试目的**：防止财务页接入 core 能力后业务语义回归 —— 页面 UI 调整不得改变接口调用/状态判断/金额口径/分页参数；金额合计口径必须复用 core useSummary（unit='yuan'：yuanToFen 浮点修正→fenToYuanDisplay，与行展示 formatYuan 同口径，零自实现格式化）；expand 承载详情无信息丢失；PD-028 决策前不实现不猜测。
- **测试步骤**：
  1. git diff 独立核验：`buildQueryParams`/`loadData`/`loadStatistics`/`updateStatsCards`/getIncome/getExpense/getBalance/getFlowTypeTag/getFlowStatusTag 零逻辑变更（仅注释新增）；API 调用集合 3 处与改动前一致（无新增/删除接口调用）
  2. 读码核验 8 项清单：固定列（L82-91）/ density="auto"（L278）/ SearchPanel storage-key + 恢复回填（L97-113,254-271）/ expand 插槽同字段（L279,290-307）/ pagination + show-summary（L282-283,95）/ 列宽省略（摘要 showOverflowTooltip L85 保持）
  3. 金额口径核验：行展示 formatYuan（L69-71 toLocaleString 千分位 2 位，与改动前同函数同输入）；合计行 summarizeAmount（unit='yuan'）仅复用 `@/utils/money` Converter（useSummary.ts:31 import）；balance 累计余额合计无业务意义留空（core 默认行为，未列入 amountProps）
  4. PD-028 合规核验：getFlowStatusTag（L58-65）为既有函数（git diff 仅注释变更 L56-57）；无待核销/overdue/abnormal 标注、无新增表格状态列、statusConfig 无待核销键
  5. 批量不支持项核验：fund-flow.ts 独立核对仅 5 方法（getList/getById/create/getByAccountId/getStatisticsByAccount）无批量端点；未接入 batchActions；复选框列保持 core 默认渲染（selectable 默认 true）
  6. 构建独立复跑：`npx vue-tsc --noEmit` = 136 条 error（与存量基线 136=136 一致，本文件零错误）；`npm run build` = EXIT=0（QA 独立执行）
- **预期结果**：
  - 业务语义零变更：API 调用集合与改动前一致；业务/金额口径函数零逻辑变更；移除项仅为展示层（手写分页样板 → core 封装、详情弹层 → expand 同字段）
  - core 接入真实生效：密度真实联动 layoutStore、筛选本地持久化恢复仅回填表单（buildQueryParams 零改动）、expand 同字段无信息丢失、合计口径复用 Converter
  - PD-028 约束合规：决策前不实现不猜测（BLOCKED 不占通过数，如实登记不销号）
  - 批量不支持项保持现状并登记（无批量端点、无逐条循环调既有端点）
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：资金流水「待核销」状态语义缺失（后端 FundFlow 实体/VO/fund_flows 表均无 status 字段，前端 FundFlowStatusMap 悬空声明、页面状态恒「未知」）——决策前不实现不猜测、不销号；overdue/abnormal 无数据字段依据不标注；本卡未借机实现 PD-028 任何内容（grep + diff 双确认）；影响面=本卡「异常状态醒目」接入子项（登记链：任务卡 `production-remediation-task-board.md` §9 L2427 → 决策池 `product-decision-backlog.md:44/155` → roadmap §5.10 L253，见「PD-028 限制登记同步」节）
  - 观察 O-FIN-1（QA 报告 §二.风险/观察，登记不判 FAIL）：expand 替代弹层后，详情入口由「详情按钮」变为 EP 原生展开列图标（48px）——信息字段与弹层完全一致（el-descriptions 同字段），无信息丢失（开发声明 + diff 核对一致）
  - 观察 O-FIN-2（QA 报告 §二.风险/观察，登记不判 FAIL）：SearchPanel 恢复的日期 ISO 字符串转回 Date 对象（el-date-picker 原生模型），查询参数格式与现状一致（无 wire 变更）；子组件 onMounted 先于父组件执行 → 恢复条件在 loadData 前回填生效（Vue 挂载顺序核验）
  - 批量接口不支持项登记（§9.1-2）：fund-flow.ts 无批量端点 → 批量+N/M 保持现状；复选框列保持 core 默认渲染（现状）

---

### REG-UI-FIN-002

- **测试编号**：REG-UI-FIN-002
- **模块**：财务-账本/凭证（frontend/src/views/finance/FinanceLedger.vue + frontend/src/api/finance/converters.ts + voucher.ts + types/finance.ts）
- **关联任务**：P1-UI-FIN-002（Batch FIN-1；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-fin-b1-qa-report.md`；承接 Batch0-方案3 凭证状态映射对齐）
- **业务场景**：账本页接入 core 能力——固定列（凭证编号 width 145 fixed left / 状态 width 95 fixed right，L133-143；操作列 core 默认 fixed=right）、密度 `density="auto"`（L403）、筛选保存（`<SearchPanel storage-key="finance-ledger-filter">` L376-386 + handleFilterRestored/restoreDateRange L152-164）、行内展开分录（L404,431-451：`expandable` + `#expand` 承载分录明细 DataTable `selectable=false` 默认密度 + 借方/贷方合计；**裸 el-table（含 size="small"）已移除、未照搬 size="small"**；弹层保留元数据 L462-482）、分页+合计（`:pagination` L408 core 006 + `show-summary` + `createAmountSummaryMethod({ amountProps:['debitTotal','creditTotal'], unit:'fen' })` L150）、列宽省略（actions-width 240 → 200 L407；摘要 showOverflowTooltip 保持 L137）；**Batch0-方案3 凭证状态映射对齐**：后端 0-3（FinanceVoucherStateMachine.java:48-54 DRAFT=0/APPROVED=1/POSTED=2/VOID=3）与前端 VoucherStatusMap 双向对齐（converters.ts:117-130 toFrontend `{0:'draft',1:'audited',2:'posted',3:'cancelled'}` / toBackend 逆映射；修复前非 0 状态全错档：before 1~4 ↔ draft~cancelled，后端 1=已审核显示 draft）；按钮可用性逻辑零改动（映射值对齐后自动正确）
- **测试目的**：防止凭证状态映射错位回归（修复前后端 APPROVED(1) → 前端 'draft' 草稿显示、非 0 状态全部错位一档）与账本页 core 接入业务语义回归 —— VoucherStatusMap 必须与后端 0-3 双向一致；按钮可用性逻辑（draft→编辑/审核/作废、audited→过账/反审核/作废、posted→反过账、cancelled 无操作）不得随映射变更而改动；后端契约不变（纯前端 4 文件）；合计口径与行展示一致（unit='fen' fenToYuanDisplay）；金额口径复用 Converter（「元值再按分格式化 ÷100」为既有展示口径，不触碰 Batch0-方案2）。
- **测试步骤**：
  1. Batch0-方案3 五核验点（QA 独立执行）：a) 后端状态常量与方案一致（FinanceVoucherStateMachine.java:48-54 独立读取 0-3 + 状态流转表 L61-68）；b) converters.ts toFrontend/toBackend 0-3 双向正确（L117-130，git diff before 1~4 → after 0-3）；c) 修复前错档实锤成立（before toFrontend[1]='draft'，非 0 状态全部错位一档，唯一恰好正确为 0）；d) 按钮可用性逻辑零改动（FinanceLedger.vue:416-428 模板 v-if 条件 git diff 零变更；handleApprove/handlePost/handleUnapprove/handleUnpost/handleVoid 接口调用零变更）；e) 后端契约不变（纯前端 4 文件，git status 确认 FinanceVoucherStateMachine.java 未修改；voucher.ts/types 仅注释变更）
  2. 影响面核验：全仓 grep VoucherStatusMap/VoucherDataConverter 消费方仅 FinanceLedger.vue + voucher.ts 链路（converters/index.ts 导出）；其余 finance 页面（AutoVoucher/FinanceBudget/FinancePeriod/FinanceTax/InvoiceReimbursement）使用各自独立状态语义，不消费凭证映射 → 对齐无旁路页面破坏
  3. 读码核验 8 项清单 core 接入（固定列 L133-143 / density L403 / SearchPanel L376-386 / expand 分录 L404,431-451 未照搬 size="small" / pagination+show-summary L408,150 unit='fen' / actions-width 200 L407）
  4. 行展示口径比对（QA 独立比对 before/after）：before `debitTotal: formatAmount(v.debitTotal)`（格式化字符串直渲染）vs after `debitTotal: v.debitTotal ?? 0`（Converter 元数值）+ 模板 slot `formatAmount(row.debitTotal)`——同函数同输入渲染结果一致；合计行 unit='fen' → fenToYuanDisplay(Σ元数值) = 与行展示逐行一致
  5. 批量不支持项核验：voucher.ts 独立核对 11 方法（getList/getById/create/update/delete/approve/post/unapprove/unpost/void）无批量端点；未接入 batchActions、未逐条循环调既有端点（超展示层红线）
  6. 构建独立复跑：typecheck 136=136（本批 4 文件零错误）；npm run build EXIT=0（QA 独立执行）
- **预期结果**：
  - 状态映射正确：后端 0-3 与前端 draft/audited/posted/cancelled 双向对齐；非 0 状态不再错档（后端 1=已审核 → audited → 显示「过账/反审核/作废」按钮，修复前错误显示 draft 按钮行为错位 → 对齐后自动正确）
  - 按钮逻辑零改动：模板 v-if 条件与 5 个 handler 接口调用 git diff 零变更；后端契约不变（后端零改动）
  - core 接入真实生效：expand 分录承载（selectable=false 默认密度）、合计与行展示逐行一致、金额格式化全部走既有 formatAmount/Converter
  - 批量不支持项保持现状并登记（无批量端点、无逐条循环调既有端点）
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 O-FIN-3（QA 报告 §三.风险/观察，登记不判 FAIL）：Ledger 错误文案增强（「审核失败」→「凭证审核失败」等 5 处）+ `loadFailed` 失败态新增（L31,107-114,389-396，错误提示 + 重试入口）——展示层增强（失败不静默，与 P1-API-001 治理方向一致），不改变接口调用/状态判断/金额口径/分页参数（git diff 确认仅 catch 分支提示与模板新增）
  - 观察 O-FIN-4（QA 报告 §三.风险/观察，登记不判 FAIL）：后端状态机允许 POSTED→VOID（FinanceVoucherStateMachine.java:65），前端 posted 态仅「反过账」无「作废」按钮——按钮逻辑为既有行为（diff 零变更），非本批引入；可达路径完整（posted→反过账→audited→作废 或 反过账→draft→作废），与任务卡声明状态机（draft→audited→posted；draft/audited→cancelled 终态）不冲突
  - 观察登记（不触碰）：账本页既有展示将 Converter 元值再按分格式化（÷100），合计行按同一口径保持一致（unit='fen'）——属 Batch0-方案2 金额单位治理范围外既有展示口径，本批明确不触碰（QA 独立确认属实，合规）
  - 批量接口不支持项登记（§9.1-2）：voucher.ts 无批量端点 → 批量+N/M 保持现状；复选框列保持 core 默认渲染（现状）
  - PD-028 与账本页无直接关系（凭证状态语义完整），未触发新登记

---

### PD-028 限制登记同步（Batch FIN-1，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §9 P1-UI-FIN-001 L2427，PD-028 风险登记）→ 决策池（`product-decision-backlog.md:44/155`，2026-08-31 登记）→ roadmap（`remediation-roadmap.md` §5.10 L253，约束 + 影响面）——三处登记链完整一致。
- **内容**：资金流水「待核销」状态语义缺失——后端 `FundFlow` 实体/VO/`fund_flows` 表均无 status 字段，前端 `FundFlowStatusMap`（converters.ts:256-267）「1~3 ↔ pending/completed/cancelled」为悬空声明，页面流水状态恒「未知」（FinanceFund.vue:35-42 兜底）；属 BLOCKED_PRODUCT_RULE，待产品/财务决策（决策面：fund_flows 是否补 status 字段 / 后端判定 vs 前端派生），决策前不实现不猜测（不占通过数）。
- **本批执行确认（QA 实锤）**：FinanceFund.vue 零新增状态判断（getFlowStatusTag 为既有函数，git diff 仅注释变更 L56-57「后端 FundFlow 无 status 字段（PD-028 已登记），状态恒为『未知』兜底，决策前不实现不猜测」）；无待核销/overdue/abnormal 标注、无新增表格状态列；grep + diff 双确认 statusConfig 无待核销键、本批未借机实现 PD-028 任何内容。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-UI-FIN-001/002）**不受影响**（REG-UI-FIN-001 的「异常状态醒目」子项在 PD-028 决策前保持现状——行内展开内状态展示与弹层同口径 getFlowStatusTag；REG-UI-FIN-002 账本页凭证状态语义完整，与 PD-028 无直接关系）；影响面 = P1-UI-FIN-001 异常状态醒目接入子项，待产品/财务决策后实现。

---

### QA 观察项登记（Batch FIN-1，如实登记不销号、不猜测处置）

按 QA 报告 §二.风险/观察（O-FIN-1/2）+ §三.风险/观察（O-FIN-3/4）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-FIN-1**：expand 替代弹层后，资金流水详情入口由「详情按钮」变为 EP 原生展开列图标（48px）——信息字段与弹层完全一致（el-descriptions 同字段），无信息丢失（同 REG-UI-FIN-001 观察 1）。
2. **O-FIN-2**：SearchPanel 恢复的日期 ISO 字符串转回 Date 对象（el-date-picker 原生模型），查询参数格式与现状一致（无 wire 变更）；子组件 onMounted 先于父组件执行 → 恢复条件在 loadData 前回填生效（Vue 挂载顺序核验）（同 REG-UI-FIN-001 观察 2）。
3. **O-FIN-3**：Ledger 错误文案增强（「审核失败」→「凭证审核失败」等 5 处）+ `loadFailed` 失败态新增（L31,107-114,389-396，错误提示 + 重试入口）——展示层增强（失败不静默，与 P1-API-001 治理方向一致），不改变接口调用/状态判断/金额口径/分页参数（同 REG-UI-FIN-002 观察 1）。
4. **O-FIN-4**：后端状态机允许 POSTED→VOID（`FinanceVoucherStateMachine.java:65`），前端 posted 态仅「反过账」无「作废」按钮——按钮逻辑为既有行为（diff 零变更），非本批引入；可达路径完整（posted→反过账→audited→作废 或 反过账→draft→作废），与任务卡声明状态机不冲突（同 REG-UI-FIN-002 观察 2）。
5. **观察登记（不触碰）**：账本页既有展示将 Converter 元值再按分格式化（÷100），合计行按同一口径保持一致（unit='fen'）——与 Batch0-方案2 金额单位治理相关，本批明确不触碰（QA 独立确认属实，合规）。

---

### Batch FIN-1 门禁

```text
Regression Result: PASS（2026-08-31 Batch FIN-1 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为第三步·财务 4 页第一批，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：2/2 卡 QA 独立验收 PASS（`docs/quality/ui-fin-b1-qa-report.md`，2026-08-31：P1-UI-FIN-001 资金流水 / P1-UI-FIN-002 账本·凭证，无 FAIL、无 -R）
- FAIL 明细：0
- PWL 明细：0（PD-028 为 BLOCKED，不占通过数——「待核销」语义缺失决策前不实现不猜测，如实登记不销号）
- 基线：新增 REG-UI-FIN-001/002（正式 77 → 79 条，只增不减、既有条目零修改）
- 观察项：O-FIN-1~4 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch FIN-2 回归（第三步·财务 4 页第二批，2026-08-31）

> 验收依据：`docs/quality/ui-fin-b2-qa-report.md`（2026-08-31，QA 独立验收：P1-UI-FIN-003/004 两卡全 PASS、无 FAIL、无 -R；应收 8 项清单全核验 + **overdue 键接入真实字段依据**（converters.ts:153-166 后端 status 4=overdue 独立核验）+ 业务语义零变更（git diff API 调用集合 2 处一致）+ expand 与弹层同字段 11 项无信息丢失；应付同口径（API 3 处一致 + expand 同字段 13 项）+「待付款」无独立字段不发明（既有统计卡片 label diff 零变更）；两页视觉同源 ✅（grep 硬编码颜色零匹配、frontend-design-v3 零引用）/ 构建 ✅（typecheck 136=136 + build EXIT=0）/ 开发者未自行宣布通过 ✅（roadmap §5.10 L247「QA 独立验收中」核对一致））。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-UI-FIN-003/004（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（第三步·财务 4 页第二批，任务池 §9），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**第三步财务 4 页（FIN-001~004）全部闭环**。
> 基线计数核对：追加前正式基线 **79 条**（文件实况：基线总览表 79 行，含 REG-UI-CORE-001~007 + REG-UI-FIN-001/002；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 2 条（REG-UI-FIN-003/004）后 **81 条**，只增不减纪律达成（既有 79 条条目零修改）；候选 0 条维持。
> 专项收口核对：三步走专项（第一步盘点 + 第二步 core 增强 7 卡 + 第三步财务 4 页）新增基线累计 **70 → 81 = 11 条**（REG-UI-CORE-001~007 七卡 + REG-UI-FIN-001~004 四卡），全部在案、无缺卡、无预固化条目。

### REG-UI-FIN-003

- **测试编号**：REG-UI-FIN-003
- **模块**：财务-应收（frontend/src/views/finance/FinanceReceivable.vue）
- **关联任务**：P1-UI-FIN-003（Batch FIN-2；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-fin-b2-qa-report.md`）
- **业务场景**：应收页接入 core 能力——固定列（应收编号 minWidth 140 fixed left / 状态 minWidth 95 fixed right 异常状态醒目，L80-87；金额列 align right；操作列 core 默认 fixed=right）、密度 `density="auto"`（L248，消费 layoutStore.tableDensity core 001 唯一真相源）、筛选保存（`<SearchPanel :model-value="searchForm" storage-key="finance-receivable-filter">` L225-241 + handleFilterRestored L115-122，page-scoped localStorage，恢复仅回填表单字段）、行内详情 expand（L249,267-283，#expand 插槽承载 el-descriptions **11 字段**，与移除弹层完全同字段）、分页+合计（`:pagination` L252 core 006 + `show-summary` + `createAmountSummaryMethod({ amountProps:['amount','receivedAmount','remainAmount'], unit:'yuan' })` L58-61 复用 core useSummary）、列宽省略（客户名称 showOverflowTooltip L81；actions-width 220 → 150）；**overdue 键接入真实字段依据**（converters.ts:153-166 `ReceivablePayableStatusMap.toFrontend = {1:'unpaid',2:'partial',3:'settled',4:'overdue'}`——后端 status 4=overdue 实锤 → getStatusTag overdue 展示键 error→overdue L50，纯展示键变更，判断条件与四态语义零改动）；业务语义零变更（API 调用集合与改动前一致 2 处：receivableApi.getList L100 / receivableApi.create L198）；PD-028 待核销不实现（限制观察）；批量不支持项保持现状登记（receivable.ts 5 方法无批量端点）
- **测试目的**：防止应收页接入 core 能力后业务语义回归 —— 页面 UI 调整不得改变接口调用/状态判断/金额口径/分页参数；overdue 异常状态醒目必须基于真实数据字段（后端 status 4=overdue），不得发明「待核销」等无依据标注；金额合计口径必须复用 core useSummary（unit='yuan'：yuanToFen 浮点修正→fenToYuanDisplay，与行展示 formatAmount 同口径，零自实现格式化）；expand 承载详情无信息丢失；PD-028 决策前不实现不猜测。
- **测试步骤**：
  1. git diff 独立核验：`loadData`/`handleSubmit`/`statsCards` 零逻辑变更（diff 无函数体 +/- 行）；`getStatusTag` 仅 overdue 展示键 error→overdue（判断条件与四态语义不变）；API 调用集合 2 处与改动前一致（无新增/删除接口调用）
  2. 读码核验 8 项清单：固定列（L80-87）/ density="auto"（L248）/ SearchPanel storage-key + 恢复回填（L115-122,225-241，loadData 查询参数构造零改动 L94-99）/ expand 插槽同字段 11 项（L249,267-283）/ pagination + show-summary（L252,58-61）/ 列宽省略（客户名称 showOverflowTooltip L81、actions-width 150）
  3. overdue 键真实字段依据核验：converters.ts:153-166 四态实锤（4=overdue）；core 007 语义键存在（StatusTag.vue:113,316-320）；「待核销」无数据字段依据不发明（状态仅四态，grep「待核销」仅命中注释 L45）
  4. 金额口径核验：行展示 formatAmount（before 预格式化字符串 → after rawRecords 元数值直绑 + 列 slot formatAmount 展示——同函数同输入渲染结果一致）；合计行 summarizeAmount（unit='yuan'）仅复用 `@/utils/money` Converter
  5. PD-028 合规核验：无待核销标注、无新增状态判断（grep + diff 双确认）
  6. 批量不支持项核验：receivable.ts 独立核对仅 5 方法（getList/getById/create/confirmPayment/writeOff）无批量端点；未接入 batchActions；复选框列保持 core 默认渲染（现状）
  7. 构建独立复跑：`npx vue-tsc --noEmit` = 136 条 error（与存量基线 136=136 一致，本文件零错误）；`npm run build` = EXIT=0（QA 独立执行）
- **预期结果**：
  - 业务语义零变更：API 调用集合与改动前一致（2 处）；业务/金额口径函数零逻辑变更；移除项仅为展示层（手写分页样板 → core 006 封装、详情弹层 → expand 同字段、手写筛选区 → SearchPanel 同字段）
  - core 接入真实生效：密度真实联动 layoutStore、筛选本地持久化恢复仅回填表单（loadData 零改动）、expand 同字段无信息丢失、合计口径复用 Converter
  - overdue 醒目有真实依据：后端 status 4=overdue（converters.ts:153-166）→ overdue 语义键深橙醒目；「待核销」无数据字段依据不发明（PD-028 语义面，决策前不实现不猜测）
  - 批量不支持项保持现状并登记（无批量端点、无逐条循环调既有端点）
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：应收页无「待核销」数据字段（状态仅四态 unpaid/partial/settled/overdue，后端 1~4 实锤 converters.ts:153-166）——无依据不发明，决策前不实现不猜测、不销号；本卡未借机实现 PD-028 任何内容（grep + diff 双确认：应收仅注释 L45）；overdue 键为真实数据字段依据（4=overdue）不属 PD-028 语义面（登记链：任务卡 `production-remediation-task-board.md` §9 L2540 → 决策池 `product-decision-backlog.md:44/155` → roadmap §5.10 L253，见「PD-028 限制登记同步」节）
  - 观察 O-FIN-B2-1（QA 报告 §二.风险/观察，登记不判 FAIL）：expand 替代弹层后，应收详情入口由「详情按钮」变为 EP 原生展开列图标（48px）——信息字段与弹层完全一致（el-descriptions 同字段 11 项），无信息丢失（diff 独立比对）
  - 观察 O-FIN-B2-2（QA 报告 §二.风险/观察，登记不判 FAIL）：SearchPanel onMounted 先于父组件执行 → 恢复条件回填（handleFilterRestored）在页面 loadData 前生效（Vue 挂载顺序），查询参数格式与现状一致（无 wire 变更）
  - 观察 O-FIN-B2-3（QA 报告 §二.风险/观察，登记不判 FAIL）：core 006 封装下 size 变化回第一页（分页通用语义），与原手写 @size-change 直接 loadData 行为微差——属 core 统一契约（同 FIN-001/002 口径）
  - 批量接口不支持项登记（§9.1-2）：receivable.ts 无批量端点 → 批量+N/M 保持现状；复选框列保持 core 默认渲染（现状）

---

### REG-UI-FIN-004

- **测试编号**：REG-UI-FIN-004
- **模块**：财务-应付（frontend/src/views/finance/FinancePayable.vue）
- **关联任务**：P1-UI-FIN-004（Batch FIN-2；QA 独立验收 PASS 2026-08-31 `docs/quality/ui-fin-b2-qa-report.md`）
- **业务场景**：应付页同口径接入 core 能力——固定列（应付款编号 minWidth 140 fixed left / 状态 minWidth 95 fixed right，L82-91；金额列 align right；操作列 core 默认 fixed=right）、密度 `density="auto"`（L265，消费 layoutStore.tableDensity core 001）、筛选保存（`<SearchPanel :model-value="searchForm" storage-key="finance-payable-filter">` L242-258 + handleFilterRestored L119-126，page-scoped localStorage，恢复仅回填表单字段）、行内详情 expand（L257,284-302，#expand 插槽承载 el-descriptions **13 字段**，与移除弹层完全同字段）、分页+合计（`:pagination` L269 core 006 + `show-summary` + `createAmountSummaryMethod({ amountProps:['amount','paidAmount','remainAmount'], unit:'yuan' })` L60-63 复用 core useSummary）、列宽省略（供应商/采购订单号/入库单号 showOverflowTooltip L83-85；actions-width 280 → 150）；**overdue 键接入同依据**（converters.ts:153-166 同源实锤，后端 status 4=overdue → getStatusTag overdue 展示键 error→overdue L52，纯展示键变更）；**「待付款」无独立状态字段不发明**（未付=unpaid 即待付语义，L47 注释声明；既有统计卡片「待付款」label 为 diff 零变更行为）；业务语义零变更（API 调用集合与改动前一致 3 处：payableApi.getList L104 / payableApi.create L210 / supplierApi.getList L183）；PD-028 待核销不实现（限制观察）；批量不支持项保持现状登记（payable.ts 4 方法无批量端点）
- **测试目的**：防止应付页接入 core 能力后业务语义回归 —— 页面 UI 调整不得改变接口调用/状态判断/金额口径/分页参数；overdue 异常状态醒目必须基于真实数据字段（同源 4=overdue），不得发明「待付款」等无独立字段标注；金额合计口径必须复用 core useSummary（unit='yuan'，与行展示 formatAmount 同口径）；expand 承载详情无信息丢失；PD-028 决策前不实现不猜测。
- **测试步骤**：
  1. git diff 独立核验：`loadData`/`handleSubmit`/`loadSupplierOptions`/`handlePay`/`statsCards` 零逻辑变更（diff 无函数体 +/- 行）；`getStatusTag` 仅 overdue 展示键 error→overdue（判断条件与四态语义不变）；API 调用集合 3 处与改动前一致（无新增/删除接口调用）
  2. 读码核验 8 项清单：固定列（L82-91）/ density="auto"（L265）/ SearchPanel storage-key="finance-payable-filter" + 恢复回填（L119-126,242-258，loadData 查询参数构造零改动 L98-103）/ expand 插槽同字段 13 项（L257,284-302）/ pagination + show-summary（L269,60-63）/ 列宽省略（供应商/采购订单号/入库单号 showOverflowTooltip L83-85、actions-width 150）
  3. overdue 键真实字段依据核验：converters.ts:153-166 同源实锤（后端 status 4=overdue）；「待付款」无独立状态字段不发明（未付=unpaid 即待付语义，L47 注释声明；grep「待付款」仅命中注释 L47 + 既有统计卡片 label L76——后者为 statsCards 既有行为 diff 零变更）
  4. 金额口径核验：行展示 formatAmount（before 预格式化字符串 → after rawRecords 元数值直绑 + 列 slot formatAmount 展示——同函数同输入渲染结果一致）；合计行 summarizeAmount（unit='yuan'）仅复用 `@/utils/money` Converter
  5. PD-028 合规核验：无待核销标注、无新增状态判断（grep「待核销|核销」零命中本文件 + diff 双确认）
  6. 批量不支持项核验：payable.ts 独立核对仅 4 方法（getList/getById/create/confirmPayment）无批量端点；未接入 batchActions；复选框列保持 core 默认渲染（现状）
  7. 构建独立复跑：`npx vue-tsc --noEmit` = 136 条 error（与存量基线 136=136 一致，本文件零错误）；`npm run build` = EXIT=0（QA 独立执行）
- **预期结果**：
  - 业务语义零变更：API 调用集合与改动前一致（3 处）；业务/金额口径函数零逻辑变更；移除项仅为展示层（手写分页样板 → core 006 封装、详情弹层 → expand 同字段、手写筛选区 → SearchPanel 同字段）
  - core 接入真实生效：密度真实联动 layoutStore、筛选本地持久化恢复仅回填表单（loadData 零改动）、expand 同字段 13 项无信息丢失、合计口径复用 Converter
  - overdue 醒目有真实依据：同源 4=overdue（converters.ts:153-166）→ overdue 语义键深橙醒目；「待付款」无独立字段不发明（既有统计卡片 label 为 diff 零变更行为）
  - 批量不支持项保持现状并登记（无批量端点、无逐条循环调既有端点）
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-08-31）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：应付页无「待核销」/「待付款」独立数据字段（状态仅四态 unpaid/partial/settled/overdue，后端 1~4 实锤）——无依据不发明，决策前不实现不猜测、不销号；本卡未借机实现 PD-028 任何内容（grep「待核销|核销」零命中本文件；「待付款」仅注释 L47 + 既有统计卡片 label L76，diff 零变更）；overdue 键为真实数据字段依据（4=overdue）不属 PD-028 语义面（登记链：任务卡 `production-remediation-task-board.md` §9 L2593 → 决策池 `product-decision-backlog.md:44/155` → roadmap §5.10 L253，见「PD-028 限制登记同步」节）
  - 观察 O-FIN-B2-1（QA 报告 §三.风险/观察，登记不判 FAIL）：expand 替代弹层后，应付详情入口由「详情按钮」变为 EP 原生展开列图标（48px）——信息字段与弹层完全一致（el-descriptions 同字段 13 项），无信息丢失（diff 独立比对）
  - 观察 O-FIN-B2-2（QA 报告 §三.风险/观察，登记不判 FAIL）：SearchPanel onMounted 先于父组件执行 → 恢复条件回填（handleFilterRestored）在页面 loadData 前生效（Vue 挂载顺序），查询参数格式与现状一致（无 wire 变更）
  - 观察 O-FIN-B2-3（QA 报告 §三.风险/观察，登记不判 FAIL）：core 006 封装下 size 变化回第一页（分页通用语义），与原手写 @size-change 直接 loadData 行为微差——developer 已登记，属 core 统一契约（同 FIN-001/002 口径）
  - 批量接口不支持项登记（§9.1-2）：payable.ts 无批量端点 → 批量+N/M 保持现状；复选框列保持 core 默认渲染（现状）

---

### PD-028 限制登记同步（Batch FIN-2，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §9 P1-UI-FIN-003 **L2540** / P1-UI-FIN-004 **L2593**，PD-028 风险登记）→ 决策池（`product-decision-backlog.md:44/155`，2026-08-31 登记）→ roadmap（`remediation-roadmap.md` §5.10 L253，约束 + 影响面）——三处登记链完整一致。
- **内容**：资金流水「待核销」状态语义缺失——后端 `FundFlow` 实体/VO/`fund_flows` 表均无 status 字段，前端 `FundFlowStatusMap`（converters.ts:256-267）「1~3 ↔ pending/completed/cancelled」为悬空声明，页面流水状态恒「未知」（FinanceFund.vue:35-42 兜底）；属 BLOCKED_PRODUCT_RULE，待产品/财务决策（决策面：fund_flows 是否补 status 字段 / 后端判定 vs 前端派生），决策前不实现不猜测（不占通过数）。
- **本批执行确认（QA 实锤）**：应收/应付两页零新增「待核销」逻辑（grep + diff 双确认：应收仅注释 L45、应付零命中）；「待付款」为既有统计卡片 label（diff 零变更）；overdue 键为**真实数据字段依据**（converters.ts:153-166 4=overdue 实锤）不属 PD-028 语义面。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-UI-FIN-003/004）**不受影响**（应收/应付页状态语义完整四态，overdue 键为真实字段依据已先行接入）；影响面维持 = P1-UI-FIN-001 异常状态醒目接入子项，待产品/财务决策后实现。

---

### QA 观察项登记（Batch FIN-2，如实登记不销号、不猜测处置）

按 QA 报告 §二.风险/观察（O-FIN-B2-1~3）+ §三.风险/观察（O-FIN-B2-1~3，同口径）如实登记（回归角色不猜测处置、不销号，均不构成验收失败；同 Batch FIN-1 O-FIN-1~3 口径）：

1. **O-FIN-B2-1**：expand 替代弹层后，应收/应付详情入口由「详情按钮」变为 EP 原生展开列图标（48px）——信息字段与弹层完全一致（应收 11 项 / 应付 13 项），无信息丢失（diff 独立比对）（同 REG-UI-FIN-003/004 观察 1）。
2. **O-FIN-B2-2**：SearchPanel onMounted 先于父组件执行 → 恢复条件回填（handleFilterRestored）在页面 loadData 前生效（Vue 挂载顺序核验），查询参数格式与现状一致（无 wire 变更）（同 REG-UI-FIN-003/004 观察 2）。
3. **O-FIN-B2-3**：core 006 封装下 size 变化回第一页（分页通用语义），与原手写 @size-change 直接 loadData 行为微差——developer 已登记，属 core 统一契约（同 FIN-001/002 口径）（同 REG-UI-FIN-003/004 观察 3）。

---

### Batch FIN-2 门禁

```text
Regression Result: PASS（2026-08-31 Batch FIN-2 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为第三步·财务 4 页第二批，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：2/2 卡 QA 独立验收 PASS（`docs/quality/ui-fin-b2-qa-report.md`，2026-08-31：P1-UI-FIN-003 应收 / P1-UI-FIN-004 应付，无 FAIL、无 -R）
- FAIL 明细：0
- PWL 明细：0（PD-028 为 BLOCKED，不占通过数——「待核销」语义缺失决策前不实现不猜测，如实登记不销号）
- 基线：新增 REG-UI-FIN-003/004（正式 79 → 81 条，只增不减、既有条目零修改）
- 专项收口核对：三步走专项新增基线累计 70 → 81 = 11 条全部在案（REG-UI-CORE-001~007 七卡 + REG-UI-FIN-001~004 四卡，无缺卡、无预固化条目）
- 观察项：O-FIN-B2-1~3 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch TRACE-1 回归（数据血缘专项前置·契约对齐 3 卡，2026-09-03）

> 验收依据：`docs/quality/ui-trace-b1-qa-report.md`（2026-09-03，QA 独立验收：P1-UI-TRACE-C01/C02/C03 三卡全 PASS、无 FAIL、无 -R；三卡共用强制核验 7 项全过——git diff 纯类型+转换器（converters.ts 90 行 + types/finance.ts 83 行，grep axios/request/fetch/http 零命中）/ 不新增后端接口（fund-flow.ts、receivable.ts 零变更、voucher.ts 仅注释）/ 视觉同源（页面零改动）/ 金额口径复用既有 Converter（fenToYuanNumber/computeAging）/ PD-028 未落库合规（status 恒「未知」兜底为登记项非 FAIL）/ P1-STOCK-001 零触碰（grep stock/扣减/库存 零命中）/ 构建独立复跑（typecheck 136=136 + build EXIT=0）；开发者未自行宣布通过 ✅）。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-UI-TRACE-C01/C02/C03（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（数据血缘专项前置·契约对齐 3 卡，任务池 §9.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；三批全部完成后停止，等待产品负责人走查（§9.3 硬约束 4，不自动开新批）。
> 基线计数核对：追加前正式基线 **81 条**（文件实况：基线总览表 81 行，含 REG-UI-CORE-001~007 + REG-UI-FIN-001~004；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 3 条（REG-UI-TRACE-C01/C02/C03）后 **84 条**，只增不减纪律达成（既有 81 条条目零修改）；候选 0 条维持。

### REG-UI-TRACE-C01

- **测试编号**：REG-UI-TRACE-C01
- **模块**：财务-资金流水（frontend/src/api/finance/converters.ts FundFlowDataConverter + frontend/src/types/finance.ts FundFlow + 后端 FundFlowVO.java 对照）
- **关联任务**：P1-UI-TRACE-C01（Batch TRACE-1；QA 独立验收 PASS 2026-09-03 `docs/quality/ui-trace-b1-qa-report.md`）
- **业务场景**：流水页字段契约对齐（GAP-C1）——FundFlowDataConverter.toFrontend 9 项映射（converters.ts:770-802）：`flowId→id`（:774 String）/ `accountId→bankAccountId`（:776 String）/ `businessDate→transactionDate`（:778）/ `flowDirection→flowType`（:780-782 FundFlowTypeMap 1→income/2→expense）/ `balanceAfter→balance`（:784）/ `accountName→bankAccountName`（:786）/ `counterpartyName→counterparty`（:788）/ `voucherId→String`（:790 穿透键）/ `summary=remark` 兜底（:792 仅当 summary undefined）；金额分→元复用 fenToYuanNumber（:794-798）；**悬空 status 映射已移除**（:799-801 toFrontend 无 status 数字→字符串映射，注释明确「FundFlowStatusMap 悬空声明登记，落库后由 B01 承接」；FundFlowStatusMap:261-272 声明保留=PD-028 登记链维持）；FundFlow 类型契约注记+字段扩充（types/finance.ts:751-798）；后端 FundFlowVO.java:19-62 对照一致（**无 status**）；页面列/expand 消费匹配（FinanceFund.vue:82-91,290-306）；8 断流字段结论：**5 对齐**（transactionDate=businessDate / flowType=flowDirection / balance=balanceAfter / bankAccountName=accountName / counterparty=counterpartyName）/**1 兜底**（summary=remark）/ **2 登记**（voucherNo 页面 '-' 兜底 :301；status 恒「未知」:302-304，PD-028 落库前）
- **测试目的**：防止资金流水页契约漂移回归 —— 转换器必须对齐后端 FundFlowVO 真实字段（断流消除），不得新增对不存在后端字段（status/flowType）的悬空映射；PD-028 落库前 status 恒「未知」兜底不伪造；与 P1-UI-FIN-001 不冲突（该批未动数据契约，本卡补齐契约层）。
- **测试步骤**：
  1. 读码逐字段核验 toFrontend 9 项映射（converters.ts:770-802）与 developer 证据、后端 VO 三方比对
  2. grep + 读码确认 toFrontend 无 status 映射（:799-801）；FundFlowStatusMap:261-272 悬空声明=PD-028 登记链（落库后 B01 承接）
  3. 后端 FundFlowVO.java:19-62 全字段与 types/finance.ts 契约注记（:752-754）对照（无 status）
  4. 8 断流字段结论逐项比对（对齐/兜底/登记）；页面列（:82-91）与 expand（:290-306）消费字段全部有转换器输出
  5. git diff 性质审查：types/finance.ts 纯类型层（契约注记+字段扩充）；converters.ts 无 axios/request/fetch/http 命中；fund-flow.ts 零变更（调用链 :74,92,103,116 均经转换器）
  6. 构建独立复跑：`npx vue-tsc --noEmit` = 136 条 error（与存量基线 136=136 一致，本文件零错误）；`npm run build` = EXIT=0（QA 独立执行）
- **预期结果**：
  - 页面列/expand 断流消除：8 断流字段全部有转换器输出（5 对齐/1 兜底/2 登记），无空白列
  - 悬空 status 映射不存在；FundFlowStatusMap 声明保留=PD-028 登记链维持（落库前不悬空不伪造，落库后 B01 承接）
  - 业务语义零变更：API/查询参数侧零改动（mapQueryParams flowType→flowDirection 保持）；金额口径与 P1-UI-FIN-001 现状一致（分→元）
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-09-03）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：fund_flows.status 未落库（与 Batch0-方案2 同批落库）——本卡 status 相关（恒「未知」兜底）为**登记项非 FAIL**（FundFlowVO 无 status 实锤 + toFrontend 不映射 status:799-801）；FundFlowStatusMap 悬空声明保留=登记链维持，落库后由 B01 占位卡承接（见「PD-028 限制登记同步」节）
  - 观察 O-TRACE-2（QA 报告 §五，登记不判 FAIL）：流水页 voucherNo 列 '-' 兜底（后端仅返回 voucherId 穿透键、无凭证号文本）；status 恒「未知」（PD-028 落库前）——与 developer 8 字段结论一致（C01 结论 2 登记）
  - P1-STOCK-001（#2 扣减分层）零触碰登记：finance 前端 diff grep stock/扣减/库存 零命中（§9.3 红线 2，严禁混批）

---

### REG-UI-TRACE-C02

- **测试编号**：REG-UI-TRACE-C02
- **模块**：财务-账本/凭证（frontend/src/api/finance/converters.ts VoucherDataConverter + frontend/src/types/finance.ts FinanceVoucherNew + 后端 FinanceVoucherVO.java/VoucherServiceImpl.java 对照）
- **关联任务**：P1-UI-TRACE-C02（Batch TRACE-1；QA 独立验收 PASS 2026-09-03 `docs/quality/ui-trace-b1-qa-report.md`）
- **业务场景**：凭证页契约对齐（GAP-C2）——VoucherDataConverter.toFrontend 9 项映射（converters.ts:484-520）：`voucherId→id`（:488 String）/ `totalDebit→debitTotal`（:490）/ `totalCredit→creditTotal`（:491）/ `createUserName→creatorName`（:493）/ `approveUserName→auditorName`（:494）/ `approveTime→auditTime`（:495）/ `voucherStatus→status`（:497-499 VoucherStatusMap 0-3 语义不变，Batch0-方案3 状态机不回归）/ `sourceId→String`（:501 穿透键）/ `details→entries`（:503-505 金额保持分）/ `summary=entries[0].summary` 兜底（:507-509）；金额分→元（:512-516）；voucherType 1~4 保持（:517-518 VoucherTypeMap:133-146 未变）；**referenceNo/sourceType/sourceId 进入类型与转换器输出**（types/finance.ts:217-222 契约注记「A01 凭证→单据穿透的数据前置键」+ spread:486 保留 + sourceId 显式 String 化:501）——A01 穿透数据前置达成；后端 FinanceVoucherVO.java:17-86 对照一致（**无 header summary**，兜底取 entries[0].summary 合理）；后端填充缺口登记属实（VoucherServiceImpl.convertToVO:469-490 未 setCreateUserName/approveUserName、getPage:229-244 未 setDetails、getDetail:208-226 才 setDetails:223）
- **测试目的**：防止凭证页契约漂移回归 —— 转换器对齐后端 FinanceVoucherVO 真实字段（断流消除）；referenceNo/sourceType/sourceId 穿透键就绪（A01 前置达成）；后端未填充数据（制单人/审核人/列表摘要）登记不伪造；Batch0-方案3 状态机语义零改动。
- **测试步骤**：
  1. 读码逐项核验 toFrontend 9 项映射（converters.ts:484-520）与 developer 证据、后端 VO 三方比对
  2. referenceNo/sourceType/sourceId 穿透键核验：类型声明（types/finance.ts:217-222）+ 转换器输出路径（spread:486 / sourceId String 化:501）
  3. 后端 FinanceVoucherVO.java:17-86 全字段对照（无 header summary）；VoucherServiceImpl.convertToVO:469-490 / getPage:229-244 / getDetail:208-226 读码核对（登记项与代码现状一致）
  4. 7 断流字段结论核验：5 对齐（id/debitTotal/creditTotal/status/auditTime）+ summary 兜底+登记 + creatorName/auditorName 对齐映射+登记（后端填充缺口）
  5. 调用链 voucher.ts:66,84,95,107,127,138,149,160,171 均经 VoucherDataConverter.toFrontend；git diff voucher.ts 仅注释更新（0~3 对齐说明，无逻辑变更）
  6. 构建独立复跑：typecheck 136=136（本文件零错误）+ npm run build EXIT=0（QA 独立执行）
- **预期结果**：
  - 凭证列表核心列断流消除（凭证编号/日期/摘要/借方/贷方/制单人/状态）；详情弹层借方/贷方/制单人/审核人/状态有真实数据
  - referenceNo/sourceType/sourceId 穿透键进入契约（A01 前置达成）
  - 后端填充缺口（制单人/审核人/列表摘要）如实登记不伪造（衔接后端整改池，后端填充后自动生效）
  - Batch0-方案3 状态机语义零改动（voucherStatus 0-3 映射保持）；voucherType 1~4 保持现状
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-09-03）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 O-TRACE-1（QA 报告 §五，登记不判 FAIL）：凭证页制单人/审核人/摘要列在 getPage 列表接口下仍为空（后端 convertToVO:469-490 未 setCreateUserName/approveUserName、getPage:229-244 未 setDetails）——后端填充缺口（登记项，衔接后端整改池）；前端映射已就绪，后端填充后自动生效；列表页 summary=entries[0].summary 兜底待 details 返回
  - 观察 O-TRACE-5（QA 报告 §五，登记不判 FAIL）：凭证页行展示/合计为既有「分→元」展示口径（FinanceLedger.vue:55-58 formatAmount + :150 summaryMethod unit='fen'）——既有口径（Batch0-方案2 范围外保持现状），本批未变更金额口径（强制项④核验通过）

---

### REG-UI-TRACE-C03

- **测试编号**：REG-UI-TRACE-C03
- **模块**：财务-应收（frontend/src/api/finance/converters.ts ReceivableDataConverter + frontend/src/views/finance/FinanceReceivable.vue + ReceiptDialog.vue + 后端 ReceivableVO.java 对照）
- **关联任务**：P1-UI-TRACE-C03（Batch TRACE-1；QA 独立验收 PASS 2026-09-03 `docs/quality/ui-trace-b1-qa-report.md`）
- **业务场景**：应收 Converter 补齐（GAP-C3）——ReceivableDataConverter.toFrontend（converters.ts:548-567）：`id=String(receivableId)`（:552）/ `originalAmount→amount`（:554）/ `balanceAmount→remainAmount`（:555）/ 金额分→元（:556-560）/ status 映射保持（:561）/ `aging=computeAging(dueDate)`（:563；computeAging:592-598 为既有实现，与 PayableDataConverter:626 同构——应付页同口径）；**收款入口全链路恢复**：FinanceReceivable.vue:143-149 handleReceipt（String(row.id ?? '') → rawRecords.find(r => String(r.id) === receivableId) → currentReceivable）→ :308-312 ReceiptDialog 接收 → ReceiptDialog.vue:168-176 handleSubmit（receivableId: props.receivable.id → receiptApi.register）——row.id 由转换器:552 产出（String(receivableId)），find 命中、提交键一致；**GAP-C4 登记**（ReceivableVO.java:14-68 全字段核对无 invoiceDate/invoiceNo，与盘点 §3.1 L98-102 一致）+ 页面 '-' 兜底（FinanceReceivable.vue:275,280）+ 转换器:564-565 注释「不虚构值」
- **测试目的**：防止应收 Converter 缺映射回归 —— 金额/未收/账龄列必须有真实数据（与应付页口径一致）；收款按钮 target 匹配必须恢复（row.id 存在，P1-FIN-TRACE-002 收款入口失效不得复现）；invoiceDate/invoiceNo 无来源不得虚构（GAP-C4 登记维持）。
- **测试步骤**：
  1. 读码核验补齐映射（converters.ts:548-567）与 PayableDataConverter:574-594 同构性；computeAging（:592-598）复用确认（非新写）
  2. 收款入口闭环核验：转换器产出 id=String(receivableId)（:552）→ FinanceReceivable.vue:143-149 find 命中 → ReceiptDialog.vue:168-176 提交键 receivableId 一致（receiptApi.register）
  3. 读 ReceivableVO.java:14-68 全字段（无 invoiceDate/invoiceNo）+ 页面 '-' 兜底（FinanceReceivable.vue:275,280）+ 转换器:564-565 注释三方一致（GAP-C4 登记无虚构）
  4. 调用链 receivable.ts:59,77,88 均经 ReceivableDataConverter.toFrontend；git diff 页面/API 零改动（类型/页面已就绪，本卡补 Converter）
  5. 构建独立复跑：typecheck 136=136（本文件零错误）+ npm run build EXIT=0（QA 独立执行）
- **预期结果**：
  - 应收列表金额/已收/未收/账龄列恢复真实数据（应付同口径）；行内详情金额/账龄真实
  - 收款入口实际可用：find→提交键全链路命中（P1-FIN-TRACE-002 闭环）
  - GAP-C4：invoiceDate/invoiceNo '-' 兜底无虚构值（与应付页同口径）
  - 账龄口径与应付一致（computeAging(dueDate)），未改变金额/账龄口径（与 P1-UI-FIN-003 现状一致）
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-09-03）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 O-TRACE-3（QA 报告 §五，登记不判 FAIL）：应收/应付页 invoiceDate/invoiceNo '-' 兜底（GAP-C4 登记，ReceivableVO/PayableVO 无字段）——页面无虚构值，与应付页同口径

---

### PD-028 限制登记同步（Batch TRACE-1，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §9.3 P1-UI-TRACE-C01 **L3055**「status 无后端来源（L44）→ FundFlowStatusMap 悬空声明（converters.ts:261-272）不修复不猜测，维持 PD-028 登记链（落库后由 B01 承接）」）→ 决策池（`product-decision-backlog.md:44/155`，2026-08-31 登记）→ roadmap（`remediation-roadmap.md` §5.11，硬约束 + PD-028 已决未落库）→ QA 报告（`docs/quality/ui-trace-b1-qa-report.md` §四 强制项⑤）——四处登记链完整一致。
- **内容**：资金流水「待核销」状态语义缺失——`fund_flows.status` 与 **Batch0-方案2 同批落库**（PD-028 已决未落库）；落库前前端不实现、不悬空、不伪造（status 恒「未知」兜底有真实依据：FundFlowVO 无 status）。
- **本批执行确认（QA 实锤）**：FundFlowVO.java 无 status（独立核验）；FundFlowDataConverter.toFrontend 不映射 status（:799-801）；FundFlowStatusMap:261-272 悬空声明保留=登记链维持；页面状态恒「未知」兜底（FinanceFund.vue:56-65 既有函数，FIN-1 已验收）；P1-STOCK-001 零触碰（finance 前端 diff grep stock/扣减/库存 零命中）。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-UI-TRACE-C01/C02/C03）**不受影响**——status 相关为**登记项非 FAIL**（落库前恒「未知」兜底，与 developer 8 字段结论一致）；勾稽状态列由 B01 占位卡承接（Batch0-方案2 同批落库后实现，不占通过数）。

---

### QA 观察项登记（Batch TRACE-1，如实登记不销号、不猜测处置）

按 QA 报告 §五 限制/观察项（O-TRACE-1~5）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-TRACE-1**：凭证页制单人/审核人/摘要列在 getPage 列表接口下仍为空（后端 convertToVO:469-490 未 setCreateUserName/approveUserName、getPage:229-244 未 setDetails）——后端填充缺口（登记项，衔接后端整改池）；前端映射已就绪，后端填充后自动生效；列表页 summary=entries[0].summary 兜底待 details 返回（同 REG-UI-TRACE-C02 观察 1）。
2. **O-TRACE-2**：流水页 voucherNo 列 '-' 兜底（后端仅返回 voucherId 穿透键、无凭证号文本）；status 恒「未知」（PD-028 落库前）——登记项（C01 结论 2 登记），与 developer 8 字段结论一致；落库后由 B01 承接（同 REG-UI-TRACE-C01 观察）。
3. **O-TRACE-3**：应收/应付页 invoiceDate/invoiceNo '-' 兜底（GAP-C4 登记，ReceivableVO/PayableVO 无字段）——页面无虚构值，与应付页同口径（同 REG-UI-TRACE-C03 观察）。
4. **O-TRACE-4**：工作区 git 为多批次累计未提交状态（CORE/FIN/TRACE 均未提交），无法以 git diff 隔离本批变更——验证方法限制；已用「developer 证据行号 + 代码现状交叉核验 + 页面 diff 无 TRACE 标记」三重替代，证据充分（登记不销号）。
5. **O-TRACE-5**：凭证页行展示/合计为既有「分→元」展示口径（FinanceLedger.vue:55-58 formatAmount + :150 summaryMethod unit='fen'）——既有口径（Batch0-方案2 范围外保持现状）；本批未变更金额口径（强制项④核验通过）（同 REG-UI-TRACE-C02 观察 2）。

---

### Batch TRACE-1 门禁

```text
Regression Result: PASS（2026-09-03 Batch TRACE-1 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为数据血缘专项前置·契约对齐 3 卡，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：3/3 卡 QA 独立验收 PASS（`docs/quality/ui-trace-b1-qa-report.md`，2026-09-03：P1-UI-TRACE-C01 资金流水契约对齐 / C02 凭证契约对齐 / C03 应收 Converter 补齐，无 FAIL、无 -R）
- FAIL 明细：0
- PWL 明细：0（PD-028 为 BLOCKED，不占通过数——fund_flows.status 未落库，status 相关（恒「未知」兜底）为登记项非 FAIL，落库后由 B01 承接）
- 基线：新增 REG-UI-TRACE-C01/C02/C03（正式 81 → 84 条，只增不减、既有条目零修改）
- 观察项：O-TRACE-1~5 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch TRACE-2 回归（数据血缘专项·来源展示 + 口径标注 2 卡，2026-09-03）

> 验收依据：`docs/quality/ui-trace-b2-qa-report.md`（2026-09-03，QA 独立验收：P1-UI-TRACE-A01/CL01 两卡全 PASS、无 FAIL、无 -R；两卡共用强制核验 7 项全过——纯展示+契约映射+跳转（无接口调用/状态判断/金额逻辑/业务逻辑变更，本批新增段 grep axios/request/fetch/http 零命中）/ 不新增后端接口（盘点报告引用后端行号与今日读码完全一致未漂移 = 后端零改动实锤）/ 视觉同源（仅 var(--fts-*) tokens + EP 组件，V3-A 零引用）/ 金额口径复用既有 Converter（本批零金额逻辑）/ PD-028 未落库合规（columns 无 status/勾稽列，getFlowStatusTag 恒「未知」兜底为登记项非 FAIL）/ P1-STOCK-001 零触碰 / 构建独立复跑（typecheck 136=136 + build EXIT=0）；开发者未自行宣布通过 ✅）。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-UI-TRACE-A01/CL01（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（数据血缘专项·来源展示 + 口径标注 2 卡，任务池 §9.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；三批全部完成后停止，等待产品负责人走查（§9.3 硬约束 4，不自动开新批）。
> 基线计数核对：追加前正式基线 **84 条**（文件实况：基线总览表 84 行，含 REG-UI-TRACE-C01/C02/C03；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 2 条（REG-UI-TRACE-A01/CL01）后 **86 条**，只增不减纪律达成（既有 84 条条目零修改）；候选 0 条维持。

### REG-UI-TRACE-A01

- **测试编号**：REG-UI-TRACE-A01
- **模块**：财务-资金流水/凭证（frontend/src/views/finance/FinanceFund.vue 来源列 + FinanceLedger.vue 凭证来源单据展示/穿透接收端 + converters.ts 映射表 + 后端 PaymentServiceImpl.java/ReceiptServiceImpl.java/FinanceVoucher.java/FundFlow.java 对照）
- **关联任务**：P1-UI-TRACE-A01（Batch TRACE-2；QA 独立验收 PASS 2026-09-03 `docs/quality/ui-trace-b2-qa-report.md`）
- **业务场景**：流水来源展示 + 正向穿透（两跳：流水→凭证→单据）——来源列（FinanceFund.vue:90 列定义 + :329-344 slot）：来源单据号 `getSourceDocNo`（:258-265）`voucherNo` 预留（C01 登记后端不返回）→ `remark.split('，')[0]` 首个子句文本兜底（与后端写入格式逐字一致：PaymentServiceImpl.java:162「付款单：FKxxx，应付：YFxxx」/ ReceiptServiceImpl.java:144「收款单：SKxxx，应收：YSxxx」，中文逗号分隔；完整文本挂 title）→ '-' 不虚构；来源类型 flowCategory（FundFlowVO.java:34 + FundFlowCategoryMap converters.ts:305-315 七类 = FundFlow.java:37-41 逐项一致）；来源时间 businessDate（FundFlowVO.java:49）；**流水→凭证穿透真实可达**（:336 `v-if="row.voucherId"` → :278-281 `router.push({ name: 'FinanceLedger', query: { voucherId } })`，路由注册 router/index.ts:114 → 接收端 FinanceLedger.vue:194-202 onMounted 读 query → fetchAndShowDetail:340-349 复用既有 `voucherApi.getById`（voucher.ts:82）→ 详情弹层，与「查看」按钮同路径——**穿透动作零新增接口调用**）；凭证→单据文本展示（FinanceLedger.vue:465-480 来源单据区 el-descriptions 三行 referenceNo/sourceType/sourceId + :525-526 详情弹层补两行；VoucherSourceTypeMap converters.ts:151-162 八类 = FinanceVoucher.java:59-63 逐项一致）；**不可行方向登记链完整**（流水→单据直达 ❌ GAP-A2 :251-257 注释 + 任务池 :3201 / 凭证→流水 ❌ GAP-B1 / 收款单→流水 ⚠️ GAP-B3 任务池 :3203 → 衔接 BE01 后端池排期），无伪造穿透
- **测试目的**：防止来源展示/穿透回归 —— 来源列必须与后端 remark 写入格式逐字匹配（首个子句兜底不虚构）；流水→凭证穿透必须真实可达且零新增接口（复用既有 getById + 详情弹层）；凭证→单据文本展示 + 跳转登记不伪造（GAP-B2）；不可行方向（GAP-A2/B1/B3）登记链完整衔接 BE01；映射表（VoucherSourceTypeMap 8 类 / FundFlowCategoryMap 7 类）与后端枚举逐项一致。
- **测试步骤**：
  1. 读码核验来源列（FinanceFund.vue:90 + :329-344 slot + getSourceDocNo :258-265）三分支（voucherNo 预留 / remark 首个子句 / '-'）与后端写入格式（PaymentServiceImpl.java:162 / ReceiptServiceImpl.java:144）逐字比对
  2. 穿透链路核验：入口条件（:336 v-if voucherId）→ router.push（:278-281）→ 路由注册（router/index.ts:114）→ 接收端 onMounted（FinanceLedger.vue:194-202）→ fetchAndShowDetail（:340-349）复用 voucherApi.getById（voucher.ts:82）→ 详情弹层（:352-357）；与「查看」按钮（:329-337）同路径比对；grep FinanceFund.vue/FinanceLedger.vue 新增段 axios/request/fetch/http 零命中
  3. 映射表对照：VoucherSourceTypeMap（converters.ts:151-162）八类 vs FinanceVoucher.java:59-63；FundFlowCategoryMap（converters.ts:305-315）七类 vs FundFlow.java:37-41；均为 toFrontend 单向纯文案（无 toBackend = 无业务判断）；未命中兜底 '-'/'未知(n)' 不虚构（FinanceLedger.vue:391-393 / FinanceFund.vue:268-271）
  4. 不可行方向登记链核验：GAP-A2（FinanceFund.vue:251-257 注释 + 任务池 :3201）/ GAP-B1/B3（任务池 :3203）→ BE01 登记卡（后端池排期）；无任何方向伪造穿透实现
  5. 后端零改动核验：盘点报告（2026-08-31）引用后端行号与今日读码完全一致未漂移（PaymentServiceImpl.java:162 / ReceiptServiceImpl.java:144 / FinanceVoucher.java:63 / FundFlow.java:41）
  6. 构建独立复跑：typecheck 136=136（本批触及文件零错误）+ npm run build EXIT=0（QA 独立执行）
- **预期结果**：
  - 来源列有真实数据：来源单据号（voucherNo 预留→remark 首个子句→'-'）、来源类型（flowCategory 7 类映射）、来源时间（businessDate）；'-' 不虚构
  - 流水→凭证穿透真实可达（跳转带参 + 既有 getById 详情弹层），零新增接口调用
  - 凭证→单据文本展示（referenceNo/sourceType/sourceId）+ 跳转登记不伪造（GAP-B2 衔接 BE01）
  - 映射表与后端枚举逐项一致；不可行方向（GAP-A2/B1/B3）登记链完整
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-09-03）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 O-TRACE2-1（QA 报告 §四，登记不判 FAIL）：developer 证据行号与代码现状存在 ±1~±8 行偏差（FinanceFund 来源列 :91→实际 :90；FinanceLedger onMounted :201-207→实际 :194-202、来源单据区 :469-480→实际 :465-480、详情弹层 :515-516→实际 :525-526）——内容完全一致（QA 逐行比对通过），不构成缺陷；后续证据行号以实际代码为准
  - 观察 O-TRACE2-3（QA 报告 §四，登记不判 FAIL）：来源单据号为 remark 首个子句文本兜底（如「付款单：FKxxx」），完整文本挂 title 展示，**不可结构化穿透**——盘点 §6.1 L172 既定事实（非本批缺陷）；两跳穿透（voucherId → 凭证 → sourceType/sourceId 文本展示）为唯一结构化路径
  - 观察 O-TRACE2-4（QA 报告 §四，登记不判 FAIL）：凭证→单据第二跳为文本展示 + 跳转登记（GAP-B2 无统一聚合接口、现有业务单据页无单证定位深链）——登记项（验收标准 4「可达或如实登记」允许路径），衔接 BE01 后端池排期，前端不伪造跳转
  - 观察 O-TRACE2-5（QA 报告 §四，登记不判 FAIL）：查看凭证链接在 row.voucherId 为空时隐藏（v-if），来源单据号显示 '-'——与「'-' 不虚构」口径一致（C01 voucherNo 登记项延续）；PD-028 落库前无新增状态字段可展示（B01 占位卡承接）

---

### REG-UI-TRACE-CL01

- **测试编号**：REG-UI-TRACE-CL01
- **模块**：财务-4 页（frontend/src/views/finance/components/CaliberNoteBar.vue 新增共享组件 + FinanceFund/FinanceLedger/FinanceReceivable/FinancePayable 4 页 PageHeader 下方接入 + product-decision-backlog.md PD-031/032 对照）
- **关联任务**：P1-UI-TRACE-CL01（Batch TRACE-2；QA 独立验收 PASS 2026-09-03 `docs/quality/ui-trace-b2-qa-report.md`）
- **业务场景**：口径标注条（纯展示层）——CaliberNoteBar.vue 三要素与决策池原文逐字一致：:20 金额单位「金额单位：元（数据库按分存储，页面展示为元）」+ :22-26「管理台账口径」标签（tooltip :22 挂 PD-031 决策原文摘要 = product-decision-backlog.md:47「accounting_subjects.balance（分）= 管理口径真相；account_balance（元，人工）冻结停用」）+ :27-32「管理口径」标签（tooltip :28 挂 PD-032 原文摘要 = :48「最新入库价为当前口径；移动加权平均 = Level 3 演进，不迁移历史数据」）；**4 页 PageHeader 下方接入**（FinanceFund.vue:293 / FinanceLedger.vue:406 / FinanceReceivable.vue:223 / FinancePayable.vue:240，均在 PageHeader 之后、统计卡片之前）；**零数据依赖/零接口调用/零金额口径变更**（grep CaliberNoteBar.vue axios/request/fetch/http/fenToYuan/yuanToFen/toLocaleString 零命中；4 页新增行仅 `<CaliberNoteBar />` 单标签，无 props/事件；页面金额列/合计行/表单零改动）；视觉同源（样式仅 var(--fts-*) :41-47 + EP 组件 el-icon/el-tooltip/el-tag，无自造色值）
- **测试目的**：防止口径标注回归 —— 标注条三要素必须与 PD-031/032 决策池原文逐字一致（tooltip 挂决策原文，不得漂移）；4 页接入位保持（PageHeader 下方）；纯展示层零数据依赖/零接口调用/零金额口径变更（Batch0-方案2 不触碰）。
- **测试步骤**：
  1. 读码 CaliberNoteBar.vue 全文件（59 行，script 仅 1 行 import = 零逻辑）三要素与 product-decision-backlog.md:47（PD-031）/ :48（PD-032）原文逐字比对
  2. grep CaliberNoteBar 4 页命中 + 读上下文确认接入位置（PageHeader 之后、统计卡片之前：FinanceFund.vue:293 / FinanceLedger.vue:406 / FinanceReceivable.vue:223 / FinancePayable.vue:240）
  3. grep 性质审查：CaliberNoteBar.vue 与 4 页新增行 axios/request/fetch/http/fenToYuan/yuanToFen/toLocaleString 零命中（零数据依赖/零接口调用/零金额逻辑）
  4. 视觉同源核验：样式仅 var(--fts-*) + EP 组件，grep 硬编码颜色零命中；frontend-design-v3|V3-A 零引用
  5. 构建独立复跑：typecheck 136=136（本批触及文件零错误）+ npm run build EXIT=0（QA 独立执行）
- **预期结果**：
  - 标注条三要素与 PD-031/032 决策池原文逐字一致（tooltip 挂决策原文摘要）
  - 4 页 PageHeader 下方接入齐全（FinanceFund/FinanceLedger/FinanceReceivable/FinancePayable）
  - 零数据依赖/零接口调用/零金额口径变更（金额展示现状零变化，Batch0-方案2 不触碰）
  - 视觉同源（仅 tokens + EP 组件，V3-A 零引用）
- **测试类型**：手工回归（代码级读码/grep + 决策池对照 + 构建独立复跑）
- **当前状态**：PASS（2026-09-03）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 标注位登记（QA 报告，登记不判 FAIL）：PD-031/032 的科目余额/计价语境页面（如会计科目页、库存/菜品计价页）不在本卡「财务 4 页」范围——如实登记**不发明新页面**（卡内修复目标「存在即标注」口径：本批仅 4 页顶部标注条）

---

### PD-028 限制登记同步（Batch TRACE-2，BLOCKED，决策前不实现）

- **登记链（完整核对）**：决策池（`product-decision-backlog.md:44/155`，2026-08-31 登记）→ roadmap（`remediation-roadmap.md` §5.11，硬约束 + PD-028 已决未落库）→ 任务池（§9.3 B01 占位卡：Batch0-方案2 同批落库后实现）→ QA 报告（`docs/quality/ui-trace-b2-qa-report.md` §三 强制项⑤：FinanceFund.vue columns :86-96 无 status 列、无勾稽列）——登记链完整一致。
- **内容**：资金流水「待核销」状态语义缺失——`fund_flows.status` 与 **Batch0-方案2 同批落库**（PD-028 已决未落库）；落库前前端不实现、不悬空、不伪造（status 恒「未知」兜底有真实依据：FundFlowVO 无 status）。
- **本批执行确认（QA 实锤）**：FinanceFund.vue columns（:86-96）无 status 列、无勾稽列；状态展示仍为既有 getFlowStatusTag 恒「未知」兜底（:61-68 注释「PD-028 已登记，决策前不实现不猜测」/ :361-363 消费）；FundFlowStatusMap 悬空声明维持登记链（B01 占位卡承接，不占通过数）；P1-STOCK-001 零触碰（应收/应付 grep stock/扣减/库存：唯一命中为注释「数据来源」与既有字段 stockinNo，无本批新增库存逻辑）。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-UI-TRACE-A01/CL01）**不受影响**——status 相关为**登记项非 FAIL**（落库前恒「未知」兜底，本批无新增状态字段展示）；勾稽状态列由 B01 占位卡承接（Batch0-方案2 同批落库后实现，不占通过数）。

---

### QA 观察项登记（Batch TRACE-2，如实登记不销号、不猜测处置）

按 QA 报告 §四 限制/观察项（O-TRACE2-1~5）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-TRACE2-1**：developer 证据行号与代码现状存在 ±1~±8 行偏差（FinanceFund 来源列 :91→实际 :90；FinanceLedger onMounted :201-207→实际 :194-202、来源单据区 :469-480→实际 :465-480、详情弹层 :515-516→实际 :525-526）——注记不精确；内容完全一致（QA 逐行比对通过），不构成缺陷；建议后续证据行号以实际代码为准。
2. **O-TRACE2-2**：工作区多批次累计未提交（git status 大量 M/??，最近提交 cd1c89b 为 OIC-BE 阶段二批1），无法 git diff 精确隔离本批变更——验证方法限制（同 TRACE-1 O-TRACE-4）；已用「developer 证据行号 + 代码现状交叉核验 + 变更段性质审查」三重替代，证据充分。
3. **O-TRACE2-3**：来源单据号为 remark 首个子句文本兜底（如「付款单：FKxxx」），完整文本挂 title 展示；**不可结构化穿透**——盘点 §6.1 L172 既定事实（非本批缺陷）；两跳穿透（voucherId → 凭证 → sourceType/sourceId 文本展示）为唯一结构化路径（同 REG-UI-TRACE-A01 观察）。
4. **O-TRACE2-4**：凭证→单据第二跳为文本展示 + 跳转登记（GAP-B2 无统一聚合接口、现有业务单据页无单证定位深链）——登记项（验收标准 4「可达或如实登记」允许路径），衔接 BE01 后端池排期；前端不伪造跳转（同 REG-UI-TRACE-A01 观察）。
5. **O-TRACE2-5**：查看凭证链接在 row.voucherId 为空时隐藏（v-if），来源单据号显示 '-'——与「'-' 不虚构」口径一致（C01 voucherNo 登记项延续）；PD-028 落库前无新增状态字段可展示，B01 占位卡承接（同 REG-UI-TRACE-A01 观察）。

---

### Batch TRACE-2 门禁

```text
Regression Result: PASS（2026-09-03 Batch TRACE-2 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为数据血缘专项·来源展示 + 口径标注 2 卡，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：2/2 卡 QA 独立验收 PASS（`docs/quality/ui-trace-b2-qa-report.md`，2026-09-03：P1-UI-TRACE-A01 流水来源展示 + 正向穿透 / P1-UI-TRACE-CL01 口径标注条，无 FAIL、无 -R）
- FAIL 明细：0
- PWL 明细：0（PD-028 为 BLOCKED，不占通过数——fund_flows.status 未落库，status 相关（恒「未知」兜底）为登记项非 FAIL，落库后由 B01 承接）
- 基线：新增 REG-UI-TRACE-A01/CL01（正式 84 → 86 条，只增不减、既有条目零修改）
- 观察项：O-TRACE2-1~5 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch TRACE-3 回归（数据血缘专项·异常醒目 + 依赖落位 3 卡，2026-09-03）

> 验收依据：`docs/quality/ui-trace-b3-qa-report.md`（2026-09-03，QA 独立验收：P1-UI-TRACE-D01 = **PASS**（改码卡）；P1-UI-TRACE-B01 / BE01 = **PASS（登记合规口径）**（占位/登记卡，不占通过数）；无 FAIL、无 -R；三卡共用强制核验 7 项全过——D01 git diff 纯展示行类+样式（无接口调用/状态判断/金额口径/业务逻辑变更，grep axios/request/fetch/http 零命中）/ 不新增后端接口（5 处证据行号与盘点一致未漂移 = 后端零改动实锤）/ 视觉同源（仅 var(--fts-*) tokens + StatusTag overdue 既有键，V3-A 零引用）/ 金额口径零变更（零 fenToYuan 新增）/ PD-028 未落库合规（FinanceFund.vue 零改动 + getFlowStatusTag 恒「未知」兜底 + FundFlowStatusMap 悬空声明维持登记链）/ P1-STOCK-001 零触碰 / 构建独立复跑（typecheck 136=136 + build EXIT=0）；开发者未自行宣布通过 ✅）。
> 新增依据：仅基于 QA 已验收 PASS 结论固化 REG-UI-TRACE-D01 一条（维护规则 5 禁止推测）；B01/BE01 为占位/登记卡（QA 验收口径 PASS 登记合规）**不固化基线条目**（无行为断言），仅登记依赖说明。
> 批次口径：本批为**治理批次**（数据血缘专项·异常醒目 + 依赖落位 3 卡，任务池 §9.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**本批为数据血缘专项最后一批（三批全部完成），专项完成，停止等待产品负责人走查（§9.3 硬约束 4，不自动开新批）**。
> 基线计数核对：追加前正式基线 **86 条**（文件实况：基线总览表 86 行，含 REG-UI-TRACE-C01/C02/C03 + A01/CL01；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 1 条（REG-UI-TRACE-D01）后 **87 条**，只增不减纪律达成（既有 86 条条目零修改）；B01/BE01 为占位/登记卡不新增基线条目（无行为断言）；候选 0 条维持。

### REG-UI-TRACE-D01

- **测试编号**：REG-UI-TRACE-D01
- **模块**：财务-应收/应付（frontend/src/views/finance/FinanceReceivable.vue + FinancePayable.vue 行级醒目 + core DataTable rowClassName 透传 + _tokens.scss/_dark-mode.scss overdue 三元组 + StatusTag overdue 键 + converters.ts ReceivablePayableStatusMap 对照）
- **关联任务**：P1-UI-TRACE-D01（Batch TRACE-3；QA 独立验收 PASS 2026-09-03 `docs/quality/ui-trace-b3-qa-report.md`）
- **业务场景**：应收/应付逾期行整行高亮（异常醒目）——两页 rowClassName 三件套：rowClassName 函数（FinanceReceivable.vue:57-63 / FinancePayable.vue:59-65，`row.status === 'overdue' ? 'row-overdue' : ''`）+ `:row-class-name="rowClassName"` 绑定（:264 / :281）+ scoped `.row-overdue` 样式（:333-339 / :355-361，`.data-table .el-table__row.row-overdue.row-overdue` 重复类名提升特异性 + `background-color: var(--fts-status-overdue-bg) !important` 覆盖 core 斑马纹/悬停，异常醒目优先级最高，同 PurchaseStockin.vue:782 行高亮先例）；**判断零新增**——`row.status === 'overdue'` 复用既有 status 字段（4=overdue 由 ReceivablePayableStatusMap.toFrontend 映射 converters.ts:169-175，:174 映射 4→overdue，FIN-003/004 已验收口径，本批零新增状态映射/判断逻辑），为**展示性行类选择**（同 TRACE-2 QA 对 `v-if="row.voucherId"`「数据存在性展示条件」判定先例，不改变按钮可用性/查询参数/金额口径/状态机）；行背景 tokens overdue 三元组深浅主题（_tokens.scss:124-126 浅色三元组 + _dark-mode.scss:138-140 深色适配，自动切换）；状态列 overdue 键为既有（StatusTag.vue:39/113/316-320，P1-UI-CORE-007 含 -R1 已验收；两页 getStatusTag :51/:53）——列标签 + 行级背景双重视觉提醒；**core DataTable 零改动**（仅消费既有 rowClassName 透传能力，DataTable.vue:41-42 prop 声明 / :84 默认 undefined / :212 透传；选择器 `.data-table .el-table__row` 与 :204 el-table 渲染类 class="data-table" 匹配性已读码确认）；**FinanceFund.vue 零改动**（git diff 零命中 rowClassName/row-overdue/勾稽/未勾稽；columns :86-96 无 status/勾稽列；getFlowStatusTag :61-67 恒「未知」兜底 :362 保持——未勾稽=Batch0 依赖 B01 承接）；**「对不上」零标注**（两页 grep「对不上」零命中——盘点 §7 L194-199 不可行：flowDirection/flowCategory 无法表达核销状态、remark 无状态语义、status=3 是单据状态非流水核销状态 → 登记不伪造）
- **测试目的**：防止逾期行醒目回归 —— 应收/应付逾期行必须整行高亮（rowClassName 三件套齐备）；判断必须复用既有 status 字段（4=overdue 既有映射，零新增状态逻辑，展示性行类选择）；视觉必须同源（tokens overdue 三元组深浅主题，V3-A 零引用）；core DataTable 零改动；FinanceFund.vue 零改动（未勾稽=Batch0 依赖）；「对不上」不可行不得标注。
- **测试步骤**：
  1. 读码核验两页 rowClassName 三件套（函数 :57-63/:59-65、绑定 :264/:281、样式 :333-339/:355-361）与 developer 证据行号完全一致
  2. 判断来源核验：converters.ts:169-175 ReceivablePayableStatusMap.toFrontend :174 映射 4→overdue 为既有（FIN-003/004 已验收），本批零新增状态映射/判断；`row.status === 'overdue'` 为展示性行类选择
  3. tokens 核验：_tokens.scss:124-126（浅色 #fbe9e7）+ _dark-mode.scss:138-140（深色 #33140a）overdue 三元组深浅主题；StatusTag.vue:39/113/316-320 overdue 键既有（P1-UI-CORE-007 含 -R1 已验收）
  4. core 零改动核验：DataTable.vue:41-42/84/212 rowClassName 为既有能力；选择器 `.data-table .el-table__row.row-overdue.row-overdue` 与 :204 el-table 渲染类匹配性读码确认
  5. FinanceFund.vue 零改动核验：git diff grep rowClassName/row-overdue/勾稽/未勾稽 零命中；columns :86-96 无 status/勾稽列；getFlowStatusTag :61-67 恒「未知」兜底 :362 保持（未勾稽=Batch0 依赖 B01 承接）
  6. 「对不上」零标注核验：两页 grep「对不上」零命中（盘点 §7 L194-199 不可行 → 登记不伪造）
  7. git diff 性质审查：两页新增段仅 rowClassName 函数 + `:row-class-name` 绑定 + `.row-overdue` scoped 样式；grep axios/request/fetch/http/fenToYuan/yuanToFen 零新增命中；无新增金额函数/业务逻辑行
  8. 构建独立复跑：typecheck 136=136（本批触及文件零错误）+ npm run build EXIT=0（QA 独立执行）
- **预期结果**：
  - 应收/应付列表页逾期行（status 4=overdue）整行浅橙背景高亮，覆盖 core 斑马纹/悬停（异常醒目优先级最高），深浅主题自动适配
  - 判断复用既有 status 字段与既有映射，零新增状态逻辑（展示性行类选择）；非逾期行零变化
  - core DataTable 零改动（仅消费既有 rowClassName 透传）；FinanceFund.vue 零改动（未勾稽=Batch0 依赖，恒「未知」兜底保持）
  - 「对不上」零标注（不可行登记不伪造）；金额口径/接口调用/后端零改动
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-09-03）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - 观察 O-TRACE3-1（QA 报告 §五，登记不判 FAIL）：developer 证据行号与代码现状存在 ±15~16 行偏差：ReceivablePayableStatusMap 证据注 converters.ts:154-167 → 实际 :169-175（4→overdue 在 :174）；FundFlowStatusMap 证据注 :261-272 → 实际 :277-281（悬空声明内容一致）——内容完全一致（逐行比对通过），不构成缺陷；同 O-TRACE2-1 类，建议后续证据行号以实际代码为准
  - 观察 O-TRACE3-5（QA 报告 §五，登记不判 FAIL）：D01 行高亮为纯 CSS 覆盖（!important + 重复类名），无法在 typecheck/build 层面验证实际渲染效果；选择器 `.data-table .el-table__row.row-overdue.row-overdue` 与 DataTable.vue:204 el-table 渲染类（class="data-table"）匹配性已读码确认——验证深度限制（无浏览器实测环境）；建议回归阶段浏览器冒烟补视觉实测（同浏览器冒烟先例）
  - 「未勾稽」（FinanceFund.vue）= Batch0 依赖登记：fund_flows.status 未落库（GAP-A1 盘点 L216；PD-028 已决 = Batch0-方案2 同批落库），本批零改动，登记链 = B01 占位卡承接（不占通过数，见下「B01/BE01 依赖登记」节）
  - 「对不上」= 不可行登记：现有字段不能支撑勾稽判定（盘点 §7 L190-199），本卡不标注任何「对不上」（登记不伪造）

---

### B01/BE01 依赖登记（占位/登记卡，不固化基线条目）

**处理原则（回归角色）**：B01（P1-UI-TRACE-B01 勾稽状态列占位）/ BE01（P1-UI-TRACE-BE01 后端接口缺口登记）为**占位/登记卡**，QA 验收口径为「PASS（登记合规口径）」——**无行为断言，不固化基线条目**（无回归行为可断言）；仅登记依赖说明与放行条件，落库/排期实现并 QA PASS 后再追加基线（不预固化，维护规则 5 禁止推测）。

- **B01 = BLOCKED_EXTERNAL_DEPENDENCY（Batch0 落库后放行实现并追加基线）**：
  - 依赖依据：fund_flows.status 未落库（GAP-A1 盘点 L216）+ PD-028 已决（`product-decision-backlog.md:44/155`）= **Batch0-方案2 同批落库**（任务板 §9.3 B01 卡 :3319-3324）
  - 落库前置条件（验收口径）：fund_flows.status 落库完成（含 **FundFlow 实体 / FundFlowVO / finance_tables.sql 表列三处同步**，规则 6 实体-表一致性门禁）+ Batch0-方案2 金额单位方案同批上线——以落库验证为准，落库前保持占位
  - 落库后实现范围（承接关系）：B01 放行转 DOING → 渲染 status（待核销/已核销，**不发明第三态**，PD-028 语义）+ 资金流水页勾稽列 + 未勾稽醒目（衔接 D01 卡「未勾稽」标注）；前端仅渲染既有 status 字段，不新增接口/不猜判定口径
  - 本批执行确认（QA 实锤）：代码零改动（本批 git 变更仅 D01 两页行级醒目；FinanceFund.vue columns :86-96 无 status/勾稽列）；FundFlowStatusMap 悬空声明（converters.ts:277-281 实测 1↔pending/2↔completed/3↔cancelled 仍在）维持 PD-028 登记链不修复不猜测
  - **追加基线条件**：落库完成 → 实现 → QA 独立验收 PASS → 追加基线（如 REG-UI-TRACE-B01，届时登记，不预固化）
- **BE01 = BLOCKED（后端池排期，决策前不实现）**：
  - 依赖依据：GAP-B1/B2/B3（盘点 L224-226）后端接口缺口在案（文件:行号 QA 读码逐一核验：GAP-B1 FundFlowQueryDTO.java:13-41 无 voucherId 维度 + FundFlowServiceImpl.getPage:89-114；GAP-B2 FinanceVoucherVO.java:53,56,59 有键无聚合消费端；GAP-B3 V20260625_002__create_receipt_table.sql:21 无 fund_flow_no 列 + ReceiptVO.java:61 查询路径无数据来源）
  - 排期归属：**Batch0/后端整改池排期**（任务板 §9.3 BE01 卡 :3353-3360；同 OIC-BE 先例：后端窗口/排期约束，决策前不实现不猜测）
  - 本批执行确认（QA 实锤）：后端零改动（5 处证据行号与盘点一致未漂移 = 后端零改动实锤）；前端无任何伪接口调用
  - **追加基线条件**：后端实现 → QA 独立验收 PASS → 追加基线（届时登记，不预固化）
- **不占本批通过数**：B01/BE01 为占位/登记卡（任务板 :3010 批次说明），验收以落库/排期实现为前提，另行放卡（QA 核验登记链）

---

### PD-028 限制登记同步（Batch TRACE-3，BLOCKED，决策前不实现）

- **登记链（完整核对）**：决策池（`product-decision-backlog.md:44/155`，2026-08-31 登记）→ roadmap（`remediation-roadmap.md` §5.11，硬约束 + PD-028 已决未落库）→ 任务池（§9.3 B01 占位卡：Batch0-方案2 同批落库后实现）→ QA 报告（`docs/quality/ui-trace-b3-qa-report.md` §〇 三卡结论表 + §二 B01）——登记链完整一致。
- **内容**：资金流水「待核销」状态语义缺失——`fund_flows.status` 与 **Batch0-方案2 同批落库**（PD-028 已决未落库）；落库前前端不实现、不悬空、不伪造（status 恒「未知」兜底有真实依据：FundFlowVO 无 status）。
- **本批执行确认（QA 实锤）**：FinanceFund.vue **零改动**（git diff 零命中 rowClassName/row-overdue/勾稽/未勾稽；columns :86-96 无 status 列、无勾稽列；getFlowStatusTag :61-67 恒「未知」兜底 :362 保持，注释明示「后端 FundFlow 无 status 字段（PD-028 已登记），决策前不实现不猜测」）；FundFlowStatusMap 悬空声明（converters.ts:277-281）维持登记链（B01 占位卡承接，不占通过数）；P1-STOCK-001 零触碰（两页 diff grep stock/扣减/库存：唯一命中 = 既有 stockinNo 列字段与先例注释，无本批新增库存逻辑）。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-UI-TRACE-D01）**不受影响**——status 相关为**登记项非 FAIL**（本批 FinanceFund.vue 零改动，恒「未知」兜底保持）；勾稽状态列由 B01 占位卡承接（Batch0-方案2 同批落库后实现，不占通过数）。

---

### QA 观察项登记（Batch TRACE-3，如实登记不销号、不猜测处置）

按 QA 报告 §五 限制/观察项（O-TRACE3-1~5）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-TRACE3-1**：developer 证据行号与代码现状存在 ±15~16 行偏差：ReceivablePayableStatusMap 证据注 converters.ts:154-167 → 实际 :169-175（4→overdue 在 :174）；FundFlowStatusMap 证据注 :261-272 → 实际 :277-281（悬空声明内容一致）——注记不精确；内容完全一致（QA 逐行比对通过），不构成缺陷；同 O-TRACE2-1 类，建议后续证据行号以实际代码为准。
2. **O-TRACE3-2**：工作区多批次累计未提交（git status 大量 M/??，最近提交 cd1c89b 为 OIC-BE 阶段二批1），无法 git diff 精确隔离本批变更——验证方法限制（同 TRACE-1 O-TRACE-4 / TRACE-2 O-TRACE2-2）；已用「developer 证据行号 + 代码现状交叉核验 + 变更段性质审查 + 后端行号未漂移」多重替代核验，证据充分。
3. **O-TRACE3-3**：GAP-B3 细节核实——ReceiptVO.fundFlowNo 在**创建响应路径**有瞬态填充（ReceiptServiceImpl.java:304 `vo.setFundFlowNo(flowVO.getFlowNo())`，flowVO!=null 时），但**查询路径**（getReceiptHistoryByReceivableId :164 / getDetail :175 传 null）恒空——收款单→流水流水号展示仍缺（receipt 表无 fund_flow_no 列无法持久化）；盘点已按 **GAP-D3「待核实（不猜测）」**登记（盘点 L243）——登记链如实（GAP-B3 核心事实 = receipt 表无 fund_flow_no 列，成立）；缺口有效性不变，BE01 登记合规（不销号、不猜测处置）。
4. **O-TRACE3-4**：V20260625_002 简写对应实际文件 `V20260625_002__create_receipt_table.sql`（flyway 全名含描述后缀）；证据行号 :21 一致（fund_flow_id，无 fund_flow_no）——文件名简写，内容/行号一致，不影响证据有效性。
5. **O-TRACE3-5**：D01 行高亮为纯 CSS 覆盖（!important + 重复类名），无法在 typecheck/build 层面验证实际渲染效果；选择器 `.data-table .el-table__row.row-overdue.row-overdue` 与 DataTable.vue:204 el-table 渲染类（class="data-table"）匹配性已读码确认——验证深度限制（无浏览器实测环境）；建议回归阶段浏览器冒烟补视觉实测（同浏览器冒烟先例）。

---

### 数据血缘专项收口核对表（三步走 6 条新增全部在案）

| 批次 | 卡（QA 验收结论） | 固化基线 | 在案状态 |
|---|---|---|---|
| Batch TRACE-1（前置·契约对齐，2026-09-03） | P1-UI-TRACE-C01 / C02 / C03（3/3 PASS） | REG-UI-TRACE-C01 / C02 / C03 | ✅ 在案（PASS，正式 81 → 84） |
| Batch TRACE-2（来源展示 + 口径，2026-09-03） | P1-UI-TRACE-A01 / CL01（2/2 PASS） | REG-UI-TRACE-A01 / CL01 | ✅ 在案（PASS，正式 84 → 86） |
| Batch TRACE-3（异常醒目 + 依赖落位，2026-09-03） | P1-UI-TRACE-D01（PASS，改码卡） | **REG-UI-TRACE-D01** | ✅ 在案（PASS，正式 86 → 87，本批固化） |
| Batch TRACE-3（同上） | P1-UI-TRACE-B01 / BE01（PASS 登记合规口径） | **不固化**（占位/登记卡，无行为断言） | 依赖登记在案（B01 = Batch0 落库后追加基线；BE01 = 后端池排期） |
| **合计** | 8 卡 QA 全部合规（6 行为卡 PASS + 2 登记卡） | **6 条新增基线** | **6 条全部在案**（81 → 87，只增不减，无缺卡、无预固化条目） |

**专项收口结论**：数据血缘专项三步走（TRACE-1/2/3）新增基线累计 **81 → 87 = 6 条**全部在案（REG-UI-TRACE-C01/C02/C03 + A01/CL01 + D01），无缺卡、无预固化条目；B01/BE01 为占位/登记卡不占通过数、依赖登记完整；既有 86 条基线零修改、只增不减纪律达成；**专项完成，等待产品负责人走查**（§9.3 硬约束 4，不自动开新批）。

---

### Batch TRACE-3 门禁

```text
Regression Result: PASS（2026-09-03 Batch TRACE-3 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为数据血缘专项·异常醒目 + 依赖落位 3 卡，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：3/3 卡 QA 独立验收合规（`docs/quality/ui-trace-b3-qa-report.md`，2026-09-03：P1-UI-TRACE-D01 异常行醒目 = **PASS**（改码卡）；P1-UI-TRACE-B01 勾稽占位 / P1-UI-TRACE-BE01 后端缺口登记 = **PASS（登记合规口径）**；无 FAIL、无 -R）
- FAIL 明细：0
- PWL 明细：0（PD-028 为 BLOCKED，不占通过数——fund_flows.status 未落库，status 相关（恒「未知」兜底）为登记项非 FAIL，落库后由 B01 承接；B01/BE01 为占位/登记卡不占通过数）
- 基线：新增 REG-UI-TRACE-D01（正式 86 → 87 条，只增不减、既有条目零修改）；B01/BE01 不固化基线条目（依赖登记在案）
- 观察项：O-TRACE3-1~5 如实登记不销号、不猜测处置（均不构成验收失败）
- 专项收口：数据血缘专项三步走 6 条新增基线全部在案（81 → 87），专项完成，等待产品负责人走查（§9.3 硬约束 4，不自动开新批）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch WS-1 回归（财务模块任务型重构·阶段三 Batch 1 样板页，2026-09-03）

> 验收依据：`docs/quality/ui-ws-b1-qa-report.md`（2026-09-03，QA 独立验收：P1-FIN-WS-001 对账工作台 = **PASS**；验收标准 8/8 + 强制核验 6/6 全过、无 FAIL、无 -R；对照基准 = 任务板 §11.3 细化卡（验收标准 8 条 + developer 修改证据）+ 设计 V1.1 §8（修正①/③、确认⑨）+ §3.1（五区布局/工具栏/筛选/增强能力接入位）+ 导出盘点结论（`docs/quality/finance-export-inventory-20260903.md`）+ roadmap §5.12 硬约束 + TRACE A01/D01/CL01 既有验收口径；开发者未自行宣布通过 ✅——本报告为唯一验收结论）。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-WS-001（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（财务模块任务型重构·阶段三 Batch 1 样板页，任务池 §11.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；Batch 2 核销工作台（P1-FIN-WS-002）随批放行细化（§11.3 批次节奏，禁止跳步）。
> 基线计数核对：追加前正式基线 **87 条**（文件实况：基线总览表 87 行，含 REG-UI-TRACE-C01/C02/C03 + A01/CL01 + D01；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 1 条（REG-FIN-WS-001）后 **88 条**，只增不减纪律达成（既有 87 条条目零修改）；候选 0 条维持。

### REG-FIN-WS-001

- **测试编号**：REG-FIN-WS-001
- **模块**：财务-对账工作台（frontend/src/views/finance/FinanceFund.vue 重构 + frontend/src/api/finance/converters.ts + types/finance.ts + modules/finance/menu.ts + router/index.ts；后端对照 FundFlowCreateDTO.java / FundFlowQueryDTO.java / FundFlowController.java）
- **关联任务**：P1-FIN-WS-001（Batch WS-1；QA 独立验收 PASS 2026-09-03 `docs/quality/ui-ws-b1-qa-report.md`）
- **业务场景**：对账工作台（FinanceFund 重构，范围 = 流水 + 勾稽 + 日结，修正①）——**五区布局**（待办区 :430-444 任务卡【未勾稽/异常 + 日结】+ 4 统计卡片真实数据（银行余额 = bankAccounts.reduce(balance) 已转元 :197-201；本月收入/支出 = fundFlowApi.getStatisticsByAccount 真实接口 :189 + toFrontendStatistics 转元 fund-flow.ts:52-62；周转率 = 支出/余额 :211），**PD-028 未落库登记不虚构计数** / 工具栏 :446-455 / 筛选区 :457-485（SearchPanel storage-key="finance-fund-filter" :460 → fts_search_finance-fund-filter 契约）/ 数据区 :487-559（CaliberNoteBar :489 + EmptyState error 重试 :490-497 + 引导型空态 :498-505 + DataTable :506-518）/ 详情区 :540-557（expand 行内 el-descriptions 10 字段）+ :521-535（A01 来源列 + 查看凭证穿透 :314-317 复用既有路由））；**新建流水真实入口**（:562-597 el-dialog + el-form 7 字段 → handleCreateFlowSubmit :389-416 → fundFlowApi.create（fund-flow.ts:100-104 既有端点，POST /v1/finance/fund-flows，FundFlowController.java:34-37 存在）→ FundFlowDataConverter.toCreateDTO（converters.ts:843-865：flowType→flowDirection / bankAccountId→accountId / counterparty→counterpartyName / summary→remark / transactionDate→businessDate / flowCategory spread / amount yuanToFen；**delete status/flowType/bankAccountId/counterparty/summary/transactionDate/voucherNo**——后端 FundFlowCreateDTO 无 status/voucherNo 实锤，与后端 DTO 逐字段一致；金额口径零变更，页面零自实现格式化）；**导出 :449-451 disabled + tooltip 明示依赖**（EXPORT-001 盘点结论：资金流水无导出端点 → 登记后端池 P1-FIN-EXPORT-004，不伪造导出，grep 导出/CSV/Blob 零命中）；**日结 :452-454 disabled + tooltip 明示登记**（确认⑨ 日结=勾稽锁定，依赖 PD-028 落库，入口先行/登记，零动作逻辑）；**E 值格式修复**（:479/:584 `value-format="YYYY-MM-DD"` 字符串化 + :116-136 restoreDateRange 字符串归一（兼容 `^\d{4}-\d{2}-\d{2}$` 直返 + 旧 ISO datetime 经 Date 归一）——消除 Date 对象 → ISO datetime vs 后端 FundFlowQueryDTO.java:26-32 @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate 400 风险）；**失败透传**（:216-231 loadData catch：console.error + ElMessage.error「资金流水加载失败，请重试」+ loadFailed=true + 清空数据 + finally loading=false；handleRetry :235-237 重试入口；EmptyState error 重试 :490-497）+ **引导型空态**（:498-505：hasActiveFilter :37-42 区分「未找到匹配流水，可调整筛选条件」/「尚未登记资金流水，可通过『新建流水』登记一笔收支」+ action「新建流水」，禁裸「暂无数据」）；**菜单标题三处「对账工作台」**（:427 PageHeader / menu.ts:36 / router/index.ts:119 meta.title；**路径 /finance/fund、组件 FinanceFund、scaleLevel chain-standard+ 零变更**——已存筛选不丢失）；**修正①边界**（paymentApi/receiptApi/writeOff/核销 9 种模式 grep 零命中，收付执行动作零新增，归 P1-FIN-WS-002）；**增强能力 9 项零回归**（TRACE-A01 来源穿透 :102/:521-535 / TRACE-D01 语义键待办区引用 :432-435（core 在案）/ CaliberNoteBar :489 / density="auto" :512 / 固定列 :99,101,105 / expand :513/:540-557 / show-summary + useSummary unit='yuan' :112/:514-515 / pagination :516-517 + useStandardPage :19/:257-259 / 筛选保存 page-scoped :458-464 + handleFilterRestored :139-145）
- **测试目的**：防止对账工作台样板页重构回归 —— 五区布局/工具栏三项处置/新建流水真实接线（与后端 DTO 逐字段一致、金额口径零变更）/ 导出·日结登记依赖不得伪造动作（disabled + tooltip 明示）/ E 值格式（YYYY-MM-DD ↔ LocalDate）不得回退 Date 对象 / 失败不得静默（catch→loadFailed + 重试）/ 菜单标题三处一致且路径·组件·scaleLevel 零变更 / 修正①边界（收付执行动作零新增）与增强能力 9 项（A01/D01/CaliberNoteBar/density/固定列/expand/useSummary/pagination/筛选保存）全部保留。
- **测试步骤**：
  1. 读码核验五区布局（FinanceFund.vue:430-444 / :446-455 / :457-485 / :487-559 / :540-557）与任务卡「工具栏清单 3 项」一致性；统计卡片真实数据源逐项核验（bankAccounts.reduce / getStatisticsByAccount / toFrontendStatistics）
  2. 新建流水全链路核验：弹层字段（:562-597）→ handleCreateFlowSubmit（:389-416）→ fundFlowApi.create（fund-flow.ts:100-104）→ toCreateDTO（converters.ts:843-865）与后端 FundFlowCreateDTO.java 全字段三方逐字段对照（delete status/voucherNo 正确性：后端 DTO 无 status/voucherNo 独立读码实锤）；金额元→分复用既有 yuanToFen 链（页面零自实现格式化）
  3. 导出/日结处置核验：:449-451 / :452-454 disabled + tooltip 明示（EXPORT-001 结论 + 确认⑨ + PD-028）；grep 导出/CSV/Blob/日结接口调用零命中（不伪造动作）
  4. E 值格式核验：:479/:584 value-format="YYYY-MM-DD" + :116-136 restoreDateRange 字符串归一（兼容旧存档 + 新值）；后端 FundFlowQueryDTO.java:26-32 / FundFlowController.java:72-73 LocalDate 匹配
  5. 失败透传 + 空态核验：:216-231 三件套 + :235-237 重试 + :490-497 EmptyState error + :498-505 引导型空态（hasActiveFilter 区分双文案，禁裸「暂无数据」）
  6. 修正①边界 grep：paymentApi/receiptApi/writeOff/核销 9 种模式零命中；「收款/付款」字样仅注释（:289-290）与 PageHeader description（:427）
  7. 增强能力 9 项逐项核验（来源穿透/异常语义键/口径标注/密度/固定列/expand/合计/分页/筛选保存）+ 菜单标题三处（:427 / menu.ts:36 / router/index.ts:119）+ 路径/组件/scaleLevel 零变更（git diff menu.ts 仅 1 行 title、router 仅 meta.title）
  8. 强制核验独立复跑：git diff 无业务语义/接口/状态/金额口径变更（fund-flow.ts 零变更、后端 diff 仅历史 @PreAuthorize、Service 零改动）；后端零改动（5 文件全前端）；视觉同源（:601-659 全 var(--fts-*) tokens、V3-A 零引用、硬编码色值零命中）；PD-028 未落库不悬空不伪造 + P1-STOCK-001 零触碰（grep stock/扣减/库存 零命中）；`npx vue-tsc --noEmit` = 136 条 error（= 存量基线 136=136，本批 5 文件零命中）；`npm run build` EXIT=0（QA 独立执行）
- **预期结果**：
  - 五区布局齐备（待办区不虚构计数 / 工具栏 3 项 / 筛选区 / 数据区 / 详情区），统计卡片全真实数据（无虚构）
  - 新建流水真实落库链路成立（既有端点 + DTO 逐字段一致，金额口径零变更）；导出/日结 disabled + tooltip 明示依赖（登记非 FAIL、不伪造动作）
  - E 值格式修复保持（YYYY-MM-DD ↔ LocalDate，不再产生 Date 对象序列化 400 风险）；失败透传 + 引导型空态保持（无静默清空、无裸「暂无数据」）
  - 修正①边界合规（收付执行动作零新增）；增强能力 9 项全部保留；菜单标题三处「对账工作台」、路径/组件/scaleLevel 零变更（已存筛选不丢失）
  - 后端零改动、视觉同源、PD-028 未落库不悬空不伪造、P1-STOCK-001 零触碰、typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-09-03）
- **RVT 回退标注（2026-09-04，只标注不删除——任务板 §12.4 方案）**：本条目菜单标题断言「对账工作台」三处（PageHeader/menu.ts/router meta.title）已过时——回退后现状：FinanceFund 标题三处恢复「资金管理」= HEAD 原命名（`docs/quality/ui-rvt-b1-qa-report.md` §2.3）；标题断言由 **REG-FIN-RVT-001/004** 接管；本条目其余断言（五区布局/新建流水真实接线/导出·日结登记处置/E 值格式/失败透传/增强能力 9 项零回归）回退后仍有效，由 REG-FIN-RVT-004 保留项断言衔接，无冲突。
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：fund_flows.status 未落库（与 Batch0-方案2 同批落库）——待办区任务卡登记不虚构计数（:432-439 明示「勾稽状态与异常流水标注依赖 PD-028 落库…落库后启用」）、无勾稽状态列、日结按钮 disabled 登记态、FundFlowStatusMap 悬空声明维持登记链（B01 承接）；本卡未借机实现 PD-028 任何内容（见「PD-028/PD-035 限制登记同步」节）
  - **导出/日结 = 登记依赖非 FAIL（不占通过数）**：导出依赖 EXPORT-001 盘点结论（资金流水无导出端点 → 后端池 P1-FIN-EXPORT-004 排期 + PD-035 导出规则待产品决策）；日结依赖 PD-028 落库（确认⑨ 勾稽锁定）；两按钮 disabled + tooltip 明示，无动作逻辑、无伪造（同 §11.3 卡内风险登记 + roadmap §5.12 硬约束「Batch 1 导出按钮 = disabled + tooltip 明示依赖（不伪造）」）
  - 观察 O-WS-1（QA 报告 §五，登记不判 FAIL）：统计卡片「本月收入/支出」取第一个银行账户的统计数据（loadStatistics :179-189，既有行为）；「银行余额」为全部账户之和（:197-201）——两口径并存，为页面既有现状（非本批引入，developer 证据声明统计调用零改动）；如需全部账户汇总统计，属后端统计接口能力演进（登记不处置）
  - 观察 O-WS-2（QA 报告 §五，登记不判 FAIL）：筛选保存 key 保持 `finance-fund-filter`（:460），与设计文档 V1.1 §3.1 初稿 `finance-reconciliation-filter` 不同——任务卡验收标准明确「storage-key 保持 finance-fund-filter，已存筛选不丢失」；路由未变（/finance/fund），保持既有 key 才能恢复已存筛选（以任务卡为准，无冲突）
  - 观察 O-WS-3（QA 报告 §五，登记不判 FAIL）：有筛选条件的空态 action 按钮仍为「新建流水」（:503）——description 已引导「可调整筛选条件」，按钮为入口统一文案；引导语义由 description 承担，无功能影响（引导型空态达成）
  - 观察 O-WS-4（QA 报告 §五，登记不判 FAIL）：工作区 git 为多批次累计未提交状态（CORE/FIN/TRACE/WS 均未提交），无法以 git diff 精确隔离本批变更——验证方法限制（同 O-TRACE-4）；已用「developer 证据行号 + 代码现状交叉核验 + fund-flow.ts/后端 diff 性质审查」三重替代，证据充分
  - 观察 O-WS-5（QA 报告 §五，登记不判 FAIL）：permissions.ts:742 权限码描述文案「查看资金管理」未随菜单标题变更——权限码 `finance:fund:view` 本身不变（权限集合零变更），描述文案留待命名统一批或后续文案治理（登记不改）

---

### PD-028/PD-035 限制登记同步（Batch WS-1，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §11.3 P1-FIN-WS-001 L3565/L3566：PD-028 未落库 → 待办区不虚构计数 + 日结入口先行/登记；导出登记 = 修正③ + P1-FIN-EXPORT-001 盘点结论）→ 决策池（`product-decision-backlog.md:44/155` PD-028 2026-08-31 登记；`:172` PD-035 导出规则 2026-09-03 登记）→ roadmap（`remediation-roadmap.md` §5.12：Batch 1 导出按钮 = disabled + tooltip 明示依赖（不伪造））→ QA 报告（`docs/quality/ui-ws-b1-qa-report.md` §〇/§四）——登记链完整一致。
- **内容**：① PD-028——资金流水「待核销」状态语义缺失，`fund_flows.status` 与 Batch0-方案2 同批落库；落库前前端不实现、不悬空、不伪造（待办区任务卡登记不虚构计数、无勾稽状态列、日结按钮 disabled 登记态）。② PD-035——导出规则缺失（文件格式/行数上限/导出口径未定义）；导出按钮 disabled + tooltip 明示 EXPORT-001 盘点结论（无导出端点 → 后端池 P1-FIN-EXPORT-004），不伪造导出。
- **本批执行确认（QA 实锤）**：待办区任务卡（:432-439）明示 PD-028 依赖与「不虚构计数」；无勾稽状态列、无日结判定逻辑（按钮 disabled 登记态）；FundFlowStatusMap 悬空声明维持登记链；导出 grep CSV/Blob 零命中（无伪造导出）；P1-STOCK-001 零触碰（grep stock/扣减/库存 零命中）。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-FIN-WS-001）**不受影响**——status/导出相关为**登记依赖非 FAIL**（按钮 disabled + tooltip 明示，无动作逻辑）；勾稽状态列由 B01 占位卡承接（Batch0-方案2 同批落库后实现）；导出端点由后端池 P1-FIN-EXPORT-004 + PD-035 决策承接，落地并 QA PASS 后再扩展断言（届时登记，不预固化）。

---

### QA 观察项登记（Batch WS-1，如实登记不销号、不猜测处置）

按 QA 报告 §五 限制/观察项（O-WS-1~5）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-WS-1**：统计卡片「本月收入/支出」取第一个银行账户的统计数据（loadStatistics FinanceFund.vue:184-189）；「银行余额」为全部账户之和（:197-201）——两口径并存，为页面既有现状（非本批引入，developer 证据声明统计调用零改动）；如需全部账户汇总统计，属后端统计接口能力演进（登记不处置）（同 REG-FIN-WS-001 观察 1）。
2. **O-WS-2**：筛选保存 key 保持 `finance-fund-filter`（FinanceFund.vue:460），与设计文档 V1.1 §3.1 初稿 `finance-reconciliation-filter` 不同——任务卡验收标准明确「storage-key 保持 finance-fund-filter，已存筛选不丢失」；路由未变（/finance/fund），保持既有 key 才能恢复已存筛选；设计文档 §3.1 为 V1.0 结构建议，任务卡为细化验收基准，无冲突（同 REG-FIN-WS-001 观察 2）。
3. **O-WS-3**：有筛选条件的空态 action 按钮仍为「新建流水」（FinanceFund.vue:503）——description 已引导「可调整筛选条件」，按钮为入口统一文案；无功能影响，引导型空态达成（同 REG-FIN-WS-001 观察 3）。
4. **O-WS-4**：工作区 git 为多批次累计未提交状态（CORE/FIN/TRACE/WS 均未提交），无法以 git diff 精确隔离本批变更——验证方法限制（同 O-TRACE-4）；已用「developer 证据行号 + 代码现状交叉核验 + fund-flow.ts/后端 diff 性质审查」三重替代，证据充分（同 REG-FIN-WS-001 观察 4）。
5. **O-WS-5**：permissions.ts:742 权限码描述文案「查看资金管理」未随菜单标题变更——权限码 `finance:fund:view` 本身不变（权限集合零变更）；描述文案留待命名统一批或后续文案治理（登记不改）（同 REG-FIN-WS-001 观察 5）。

---

### Batch WS-1 门禁

```text
Regression Result: PASS（2026-09-03 Batch WS-1 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为财务模块任务型重构·阶段三 Batch 1 样板页，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：1/1 卡 QA 独立验收 PASS（`docs/quality/ui-ws-b1-qa-report.md`，2026-09-03：P1-FIN-WS-001 对账工作台，验收标准 8/8 + 强制核验 6/6 全过，无 FAIL、无 -R）
- FAIL 明细：0
- PWL 明细：0（PD-028 为 BLOCKED，不占通过数——fund_flows.status 未落库，待办区登记不虚构计数、无勾稽状态列、日结 disabled 登记态，落库后由 B01 承接；导出/日结为登记依赖非 FAIL——EXPORT-001 盘点结论 + 确认⑨ + PD-035 决策待决，按钮 disabled+tooltip 明示，不伪造动作）
- 基线：新增 REG-FIN-WS-001（正式 87 → 88 条，只增不减、既有条目零修改）
- 观察项：O-WS-1~5 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch WS-2 回归（财务模块任务型重构·阶段三 Batch 2 核销工作台，2026-09-03）

> 验收依据：`docs/quality/ui-ws-b2-qa-report.md`（2026-09-03，QA 独立首验：P1-FIN-WS-002 核销工作台 = **FAIL**——7 大项中合并+双页签 / 路由重定向（确认①）/ 修正①落位 / 工具栏处置 / 应收筛选补缺 / 增强能力零回归 6 大项 **PASS** + 强制核验 **6/6 全过**；**唯一 FAIL 点 F-1 = 应付「到期日区间」筛选死参数假筛选**（前端发送 startDueDate/endDueDate → PayableQueryDTO 绑定 → `PayableServiceImpl.java:111-113` 未传 mapper → `PayableMapper.xml` 无 due_date 过滤 → 结果零变化）→ 生成 **P1-FIN-WS-002-R1** 返回 DOING）+ **`docs/quality/ui-ws-b2-r1-qa-report.md`（2026-09-03，QA 独立复验 = **PASS**，F-1 闭环）**；对照基准 = 任务板 §11.3 P1-FIN-WS-002 细化卡（验收标准 8 条 + developer 修改证据 L3636-3673 + R1 证据 L3675-3695）+ 设计 V1.1 §3.2/§8（修正①/确认①/②/④）+ 既有验收口径（`docs/quality/ui-fin-b2-qa-report.md` P1-UI-FIN-003/004 + `docs/quality/ui-trace-b3-qa-report.md` TRACE-D01）+ 导出盘点（`docs/quality/finance-export-inventory-20260903.md`）+ roadmap §5.12 硬约束（后端缺口仅登记不混入）；开发者未自行宣布通过 ✅——QA 报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论（首验 FAIL → -R1 复验 PASS 闭环后转正）新增 REG-FIN-WS-002（维护规则 5 禁止推测），无推测内容；F-1 属 FAIL 后闭环转正固化（同 P1-UI-CORE-007 -R1 转正先例口径）。
> 批次口径：本批为**治理批次**（财务模块任务型重构·阶段三 Batch 2 核销工作台，任务池 §11.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；Batch 3 记录工作台（P1-FIN-WS-003）随批放行细化（§11.3 批次节奏，禁止跳步）。
> 基线计数核对：追加前正式基线 **88 条**（文件实况：基线总览表 88 行，含 REG-FIN-WS-001；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 1 条（REG-FIN-WS-002）后 **89 条**，只增不减纪律达成（既有 88 条条目零修改）；候选 0 条维持。

### REG-FIN-WS-002

- **测试编号**：REG-FIN-WS-002
- **模块**：财务-核销工作台（frontend/src/views/finance/FinanceReconciliation.vue 新建·壳 + components/ReceivableTab.vue / PayableTab.vue 新建·页签子组件【FinanceReceivable.vue / FinancePayable.vue 已删除，内容迁移】+ router/index.ts + modules/finance/menu.ts + types/finance.ts；后端对照 ReceivableServiceImpl.java / PayableServiceImpl.java / ReceivableMapper.xml / PayableMapper.xml / ReceivableQueryDTO.java / PayableQueryDTO.java / ReceivableController.java【只读确认，零改动】）
- **关联任务**：P1-FIN-WS-002（Batch WS-2；QA 首验 FAIL F-1 → **P1-FIN-WS-002-R1 复验 PASS 2026-09-03** `docs/quality/ui-ws-b2-r1-qa-report.md`，F-1 闭环后转正）
- **业务场景**：核销工作台（应收/应付合并双页签）——**壳 + 双页签**（FinanceReconciliation.vue:83 PageHeader「核销工作台」+ :86-101 待办区三任务卡【应收/应付核销待办 = 子组件 emit('summary-updated') 上报 pendingSummary（status !== 'settled' 计数 + remainAmount 合计 + overdue 摘要，真实字段派生，分页范围限制与统计卡片同口径登记；未加载显示「加载中…」不虚构】+ 收款冲正演进标注 PD-033 :96-99】+ :104-111 el-tabs 双页签）；**路由重定向（确认①）**（router/index.ts:117 新路由 /finance/reconciliation + :118-119 旧路由 /finance/receivable→?tab=receivable、/finance/payable→?tab=payable 带默认页签参数重定向 + FinanceReconciliation.vue:65-69 handleTabChange router.replace 同步 URL query【不产生历史堆叠】+ :72-78 watch(route.query.tab) 外部导航同步 + :30 初始 activeTab 读 route.query.tab——书签/深度链接直达页签；role-profiles.ts:266-267 旧路径引用保留未修改 = 回归覆盖点；menu.ts:21-23 两菜单项合并「核销工作台」单项）；**修正①落位**（ReceivableTab.vue:447 收款按钮 + :492-496 ReceiptDialog / PayableTab.vue:465 付款按钮 + :513-517 PaymentDialog + :519-523 PaymentHistoryDialog 作废红冲【IA:80-81 既有语义保持】——收付款执行动作本台单据驱动；核销进度列 = receivedAmount/amount、paidAmount/amount 真实字段派生（getProgressPercent Math.min(100,...) + title 金额明细；后端汇总字段实锤 ReceivableServiceImpl.java:130 / PayableServiceImpl.java:138，零新增接口））；**坏账核销 writeOff 入口**（ReceivableTab.vue:448 行内按钮 v-if 未结清 + :267-286 handleWriteOff = ElMessageBox 二次确认【破坏性动作防护】→ receivableApi.writeOff 既有端点 receivable.ts:104-106 = POST /v1/finance/receivables/{id}/write-off + ReceivableController.java:72-76 @PreAuthorize finance:receivable:approve【权限由端点注解承担，不新增业务规则字段】；审计缺口【无审计注解】登记后端池）；**筛选补缺**（应收：到期日区间端到端有效——ReceivableTab.vue:388-397 el-date-picker daterange + value-format=YYYY-MM-DD → :185-188 params.startDueDate/endDueDate → ReceivableServiceImpl.java:103-105 实际传入 mapper → ReceivableMapper.xml:16-21 `AND due_date >= #{startDate}` 真实过滤 ✅；应付：**到期日区间 = 登记处置**【R1 闭环后：控件 disabled + el-tooltip 明示依赖 PayableTab.vue:403-418、loadData 零死参数发送 :199-200 仅注释、handleFilterRestored :242 dueDateRange: null 硬编码不回填旧存档 + resetSearchForm :222 置 null——控件/恢复/重置三路径恒 null = **F-1 假筛选防回归点**；后端缺口登记后端池（任务池 L3694：PayableServiceImpl.getPage 补传 + PayableMapper 参数 + XML due_date 过滤，对照应收实现）+ 创建日期区间端到端有效（:195-198 → PayableServiceImpl:111-113 → PayableMapper.xml:16-21 create_time）】）；**工具栏**（导出 = disabled + tooltip 明示 EXPORT-001 结论【应收/应付无导出端点→登记后端池，不伪造】ReceivableTab.vue:358-360 / PayableTab.vue:366-368；批量 = disabled + tooltip 明示无批量端点登记【receivable.ts 5 方法 / payable.ts 4 方法，ui-fin-b2 已验收实锤；不接入 batchActions、不逐条循环调用既有端点——超展示层红线】:362-364/:370-372；打印 = window.print 纯浏览器能力零接口 :295-298/:282-285）；**增强能力零回归**（D01 逾期行高亮 rowClassName + tokens 深浅主题 / overdue 键真实字段依据（converters.ts:169-175 4=overdue）/ expand 同字段（应收 11 项 :451-467、应付 13 项 :468-486）/ useSummary unit='yuan' / useStandardPage 分页 / density='auto' / 固定列（编号左 + 状态右）/ 筛选保存 storage-key 保持 finance-receivable-filter / finance-payable-filter（旧存档兼容校验）/ CaliberNoteBar / 统计卡片 4 项真实数据）；**失败透传 + 引导型空态**（loadFailed + EmptyState error 重试 + hasActiveFilter 区分「未找到匹配」/「暂无单据」）；**强制核验 6/6**（git diff 无业务语义/接口/状态/金额口径变更【API 层零变更：receivable.ts / payable.ts git diff HEAD 零命中】/ 后端零改动【finance 域证据文件 mtime ≤ 08-31 远早于本批 09-03 22:51+ 实锤】/ 视觉同源【三新文件 grep V3-A/硬编码色值零命中，scoped 仅 workbench-toolbar/workbench-taskboard/.row-overdue，全部 var(--fts-*) tokens】/ PD-033 仅标注 + PD-028 未落库不悬空 + P1-STOCK-001 零触碰【grep stock/扣减/库存 零命中（唯一命中 = 既有 stockinNo 列字段 + 先例注释）】/ typecheck 136=136【本批 6 文件零命中】+ build EXIT=0 独立复跑 / 开发者不自行宣布通过）
- **测试目的**：防止核销工作台合并重构回归 —— 应收/应付合并 + 双页签 + 路由重定向（旧路由直达/深度链接不失效）必须保持；修正①收付执行动作本台（单据驱动）+ 核销进度列真实字段派生不得回退；坏账核销 writeOff 入口 + 二次确认保持（既有端点接入，不新增业务规则）；筛选补缺必须端到端有效或如实登记（**F-1 防回归点：应付到期日区间三路径恒 null、零死参数发送——假筛选不得复活**）；导出/批量不得伪造动作（disabled + tooltip 明示登记依赖）；增强能力 10 项（D01/overdue/expand/useSummary/分页/密度/固定列/筛选保存/CaliberNoteBar/统计卡片）零回归；后端零改动、PD-028/033/035 BLOCKED 不占通过数。
- **测试步骤**：
  1. 壳 + 双页签核验：FinanceReconciliation.vue:83 PageHeader / :86-101 待办区（pendingSummary 派生口径 = 子组件 emit 上报，status !== 'settled' 计数 + remainAmount 合计 + overdue 摘要；未加载 null 态「加载中…」不虚构）/ :104-111 el-tabs + tab query 同步（handleTabChange router.replace + watch(route.query.tab) 无循环风险）
  2. 路由重定向核验：router/index.ts:117-119 新路由 + 旧路由带参重定向；:30/:65-69/:72-78 深度链接直达页签；role-profiles.ts:266-267 旧路径引用保留（git diff HEAD 零命中）；menu.ts:21-23 合并单项（git diff 实锤 -「应收账款」「应付账款」→+「核销工作台」）；全仓旧路径引用 grep 仅 router 重定向 + role-profiles + 壳文件注释（无其他消费方）
  3. 修正①落位核验：收款/付款行内入口保留（ReceivableTab.vue:447 / PayableTab.vue:465）+ ReceiptDialog/PaymentDialog 接线；核销进度列 = receivedAmount/amount、paidAmount/amount 派生（getProgressPercent Math.min + title 明细），后端汇总字段实锤（ReceivableServiceImpl.java:130 / PayableServiceImpl.java:138 独立读码行号一致）；FinanceFund.vue 零触碰（mtime 实锤：22:24:51 早于本批 6 文件 22:5x）
  4. 坏账核销核验：handleWriteOff（ReceivableTab.vue:267-286）= ElMessageBox 二次确认（取消 return）+ receivableApi.writeOff（receivable.ts:104-106）+ ReceivableController.java:72-76 既有端点 + 权限注解承担；无审计注解 → 后端池登记（任务卡 :3669）
  5. 筛选补缺核验：应收到期日区间全链（控件 :388-397 → 发送 :185-188 → Service :103-105 实传 → XML :16-21 due_date 过滤）端到端有效；应付创建日期区间全链（:195-198 → Service :111-113 → XML :16-21 create_time）端到端有效；**应付到期日 = F-1 防回归点**（R1 后）：grep PayableTab.vue startDueDate/endDueDate 全文件 10 处命中全部为注释/tooltip/恒 null 置空（:8/:68/:83/:183/:199-200/:222/:227/:242/:404/:408），**零 params 赋值**；控件 disabled（:407-417）+ tooltip 明示（:403-406）；handleFilterRestored :242 硬编码 null + resetSearchForm :222 null——三路径恒 null
  6. 工具栏处置核验：导出/批量 disabled + tooltip 明示（EXPORT-001 结论 + 无批量端点登记），grep CSV/Blob/download/batchActions 三新文件零代码接入（仅 tooltip 文本命中）；打印 window.print（:295-298/:282-285）零接口
  7. 增强能力逐项核验（D01 行高亮 + tokens 深浅主题 / overdue 键（converters.ts:169-175 4=overdue 既有）/ expand 同字段（应收 11 项 / 应付 13 项，与 FIN-003/004 既有弹层同字段）/ useSummary unit='yuan' / pagination / density='auto' / 固定列 / 筛选保存 storage-key 保持 + 旧存档兼容 / CaliberNoteBar / 统计卡片 4 项）
  8. 强制核验独立复跑：API 层零变更（receivable.ts / payable.ts git diff HEAD 零命中，5 方法 / 4 方法端点集合与 ui-fin-b2 验收时一致）；后端零改动（mtime：4 Java 文件 08-11、2 XML 08-31 远早于本批前端 09-03 22:51+）；writeOff 为既有端点 UI 接入（端点未变）；PD-033 仅演进标注（:96-99 文案明示待决不实现）；PD-028 未落库不悬空（待办摘要为 UTM:141 任务语义，零新增「待核销」状态列）；P1-STOCK-001 零触碰；`npx vue-tsc --noEmit` = 136 条 error（= 存量基线 136=136，本批 6 文件零命中）；`npm run build` EXIT=0（QA 独立执行）
- **预期结果**：
  - 合并 + 双页签保持：壳 + 待办区 + el-tabs 双页签 + tab query 同步；旧路由带参重定向直达页签（书签/深度链接不失效）；role-profiles.ts 旧路径引用保留
  - 修正①落位保持：收付款执行动作本台（单据驱动）；核销进度列真实字段派生（receivedAmount/paidAmount，零新增接口）
  - 坏账核销保持：既有端点 + 二次确认（未结清显示）；导出/批量登记处置（disabled + tooltip 明示，不伪造）；打印 window.print
  - 筛选有效或如实登记：应收到期日区间端到端有效；应付创建日期区间端到端有效；**应付到期日区间三路径恒 null、零死参数发送（F-1 假筛选不得复活）**，tooltip 明示后端池排期
  - 增强能力零回归（10 项）；后端零改动；PD-028/033/035 BLOCKED 不占通过数；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-09-03，R1 复验闭环转正）
- **RVT 回退标注（2026-09-04，只标注不删除——任务板 §12.4 方案）**：本条目**合并断言已过时**——回退后现状：/finance/reconciliation 路由已移除（grep 零命中）、菜单「核销工作台」合并单项已恢复为「应收账款」「应付账款」独立项（HEAD 原命名）、FinanceReconciliation.vue 壳文件保留未删除（删除归 RVT-002）；合并断言由 **REG-FIN-RVT-002** 接管（RVT-002 未完成前现状 = 结构已回退、双页签壳待回迁）；本条目功能断言（坏账核销 writeOff/筛选补缺 F-1 防回归点（应付到期日三路径恒 null）/逾期高亮/核销进度列/打印/失败透传）回退后仍有效，由 REG-FIN-RVT-002 保留项断言衔接，无冲突。
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：fund_flows.status 未落库（与 Batch0-方案2 同批落库）——本批零新增「待核销」状态列（待办摘要为 UTM:141 任务语义，基于真实 status 字段：status !== 'settled' 计数 + remainAmount 合计 + overdue 摘要，分页范围限制与统计卡片同口径登记）；无 status 字段推断逻辑；FundFlowStatusMap 悬空声明维持登记链（B01 承接）；本卡未借机实现 PD-028 任何内容（见「PD-028/PD-033/PD-035 限制登记同步」节）
  - **PD-033（BLOCKED，不占通过数）**：收款冲正待决（冲正处置方式与冲正后核销/对账口径未定义，UTM:201 演进空白）——待办区「收款冲正（演进标注）」任务卡（FinanceReconciliation.vue:96-99），零冲正实现代码，决策前不实现不猜测（P5）
  - **PD-035（BLOCKED，不占通过数）**：导出规则缺失（文件格式/行数上限/导出口径未定义）——导出按钮 disabled + tooltip 明示 EXPORT-001 盘点结论（应收/应付无导出端点→登记后端池），不伪造导出
  - **应付到期日筛选 = 登记处置非 FAIL（不占通过数）**：F-1 经 -R1 复验 PASS 闭环——控件 disabled + tooltip 明示后端消费链缺失（PayableServiceImpl 未传 startDueDate/endDueDate 给 mapper / PayableMapper.xml 无 due_date 过滤）+ 后端池排期（任务池 L3694：Service 补传 + Mapper 参数 + XML due_date 过滤 + O-WS-B2-5 命名统一 + 前端反向操作指引【后端就绪后启用控件 + 恢复参数映射 + 恢复旧存档回填】）；后端就绪并 QA PASS 后扩展端到端断言（届时登记，不预固化）
  - **导出/批量 = 登记依赖非 FAIL（不占通过数）**：导出依赖 EXPORT-001 盘点结论（应收/应付无导出端点 → 登记后端池）+ PD-035 决策；批量依赖 receivable.ts/payable.ts 无批量端点事实（ui-fin-b2 已验收）→ 两按钮 disabled + tooltip 明示，不接入 batchActions、不逐条循环调用既有端点（超展示层红线）；后端批量端点就绪并 QA PASS 后接（届时登记）
  - 观察 O-WS-B2-1（QA 报告 §九，登记不判 FAIL，建议后端池）：**应收分页后端硬编码**——ReceivableServiceImpl.java:100-102 `long current = 1L; long size = 10L;` 且 dto/finance ReceivableQueryDTO 无 current/size 字段 → 前端分页参数被后端静默丢弃，应收列表恒第 1 页/10 条（预存在后端缺陷 08-11 代码，本批后端零改动约束内未触碰；应付侧正常 PayableQueryDTO 有 current/size 且 PayableServiceImpl.java:110 消费）；本批前端分页交互保留（验收标准 #5 前端层面达成）
  - 观察 O-WS-B2-2（QA 报告 §九，登记不判 FAIL）：**待办摘要失败态显示「0 笔」**——loadData catch → rawRecords=[] → finally emitSummary() 上报 count=0 → 壳层显示「待核销 0 笔 · 未收 ¥0.00」（加载失败时 0 可能误导；仅未加载 null 态显示「加载中…」符合要求）——失败态摘要语义可优化（建议后续批次顺带处置，不阻塞）
  - 观察 O-WS-B2-3（QA 报告 §九，登记不判 FAIL）：developer 证据行号与实际存在偏差（writeOff 证据注 :227-253 → 实际 :267-286；核销进度列证据注 :190-196,295-297 → 实际 :154-159,169,439-441；筛选证据注 :323-331 → 实际 :388-397；批量证据注 :363 → 实际 :362-364 等）——注记不精确（内容一致，QA 逐行比对通过）；同 O-TRACE2-1 类，建议后续证据行号以实际代码为准
  - 观察 O-WS-B2-4（QA 报告 §九，登记不判 FAIL）：工作区多批次累计未提交（git status 大量 M/??，最近提交 cd1c89b 为 OIC-BE 阶段二批1），无法 git diff 精确隔离本批变更——验证方法限制（同 O-TRACE-4）；已用「developer 证据行号 + 代码现状交叉核验 + mtime 边界 + 变更段性质审查 + API 层零命中 grep」多重替代核验，证据充分
  - 观察 O-WS-B2-5（QA 报告 §九，登记不判 FAIL）：ReceivableMapper.xml/PayableMapper.xml 参数名 startDate/endDate 与实际语义不一致（应收=due_date、应付=create_time）——命名卫生（功能不受影响：应收服务把 dueDate 值传入名为 startDate 的参数，值正确）；**已随 F-1 同池登记后端整改池统一命名**（任务池 L3694）
  - 观察 O-WS-B2-6（QA 报告 §九，登记不判 FAIL）：expand 字段 invoiceDate/invoiceNo 为 GAP-C4 登记项（ReceivableVO/PayableVO 无来源，'-' 兜底展示）——既有验收口径延续（ui-trace-b1/b3 已登记），保持既有登记链不重复登记
  - 观察 R1-OBS-1（R1 复验报告 §八，登记不判 FAIL）：PayableTab.vue:78-84 hasActiveFilter 仍含 dueDateRange 判断分支——因 dueDateRange 恒 null（disabled 控件 + 不回填 + reset 置 null 三路径保证），该分支恒 false，无实际影响 = 冗余死分支（无害）；建议后端池就绪启用到期日筛选时随反向操作一并清理
  - 观察 R1-OBS-2（R1 复验报告 §八，登记不判 FAIL）：工作区多批次累计未提交（git status 大量 M/??，最近提交 cd1c89b）——无法 git diff 精确隔离 R1 变更（同 O-WS-B2-4 口径）；已用「mtime 边界（PayableTab.vue 23:08:51 为 R1 唯一晚于 Batch 2 的文件）+ 证据行号逐项对照 + grep 零发送 + 代码现状全链读码」多重替代核验，证据充分

---

### PD-028/PD-033/PD-035 限制登记同步（Batch WS-2，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §11.3 P1-FIN-WS-002 L3666【PD-033 收款冲正待决】/ L3667【导出登记 PD-035】/ L3670【待办摘要分页范围登记】+ R1 证据 L3694【应付到期日后端池登记】）→ 决策池（`product-decision-backlog.md:44/155` PD-028 2026-08-31 登记；`:172` PD-035 导出规则 2026-09-03 登记；PD-033 冲正登记在案）→ roadmap（`remediation-roadmap.md` §5.12：后端缺口仅登记不混入 + Batch 2 导出按钮 = disabled + tooltip 明示依赖）→ QA 报告（`docs/quality/ui-ws-b2-qa-report.md` §〇/§四/§七 + `docs/quality/ui-ws-b2-r1-qa-report.md` §二/§五/§七）——登记链完整一致。
- **内容**：① PD-028——资金流水「待核销」状态语义缺失，`fund_flows.status` 与 Batch0-方案2 同批落库；落库前前端不实现、不悬空、不伪造（本批待办摘要为 UTM:141 任务语义基于真实 status 字段，非行状态列发明）。② PD-033——收款冲正待决（冲正处置方式 + 冲正后核销/对账口径未定义，UTM:201 演进空白）；仅演进标注，不实现不猜测。③ PD-035——导出规则缺失（文件格式/行数上限/导出口径未定义）；导出按钮 disabled + tooltip 明示 EXPORT-001 盘点结论（应收/应付无导出端点 → 后端池登记）。
- **本批执行确认（QA 实锤）**：FinanceReconciliation.vue:96-99 收款冲正任务卡文案明示「PD-033 待产品决策…决策前不实现不猜测（P5 原则）」——零冲正实现代码；零新增「待核销」状态列（grep + diff 双确认）；待办摘要基于真实 status 字段 + 分页范围登记；导出 grep CSV/Blob 零命中（无伪造导出）；批量 batchActions 零代码接入；应付到期日三路径恒 null（F-1 闭环）；P1-STOCK-001 零触碰（grep stock/扣减/库存：唯一命中 = 既有 stockinNo 列字段 + 行高亮先例注释）。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-FIN-WS-002）**不受影响**——PD-028/033/035 相关均为**登记项非 FAIL**（待办摘要任务语义 + 演进标注 + disabled 登记态）；应付到期日筛选为**登记处置非 FAIL**（R1 闭环后控件 disabled + tooltip 明示，后端缺口登记后端池 L3694 排期，后端就绪并 QA PASS 后扩展端到端断言，届时登记不预固化）；勾稽状态列由 B01 占位卡承接（Batch0-方案2 同批落库后实现）；导出端点由后端池 + PD-035 决策承接。

---

### QA 观察项登记（Batch WS-2，如实登记不销号、不猜测处置）

按 QA 报告 §九 观察项（O-WS-B2-1~6）+ R1 复验报告 §八 风险与观察项（R1-OBS-1/2）+ §八 延续项（O-WS-B2-1~6 维持登记）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-WS-B2-1**：应收分页后端硬编码——`ReceivableServiceImpl.java:100-102` `long current = 1L; long size = 10L;` 且 dto/finance ReceivableQueryDTO 无 current/size 字段 → 前端分页参数被后端静默丢弃，应收列表恒第 1 页/10 条（应付侧正常）；预存在后端缺陷（08-11 代码，本批后端零改动约束内未触碰），**建议登记后端池**（同 BE01 先例）；本批前端分页交互保留（同 REG-FIN-WS-002 观察 1）。
2. **O-WS-B2-2**：待办摘要失败态显示「0 笔」（loadData catch → rawRecords=[] → finally emitSummary() 上报 count=0 → 壳层「待核销 0 笔 · 未收 ¥0.00」）——加载失败时 0 可能误导（真实情况未知）；仅未加载 null 态显示「加载中…」符合要求；建议后续批次顺带处置（失败态摘要显示「加载失败」或保持「加载中…」）（同 REG-FIN-WS-002 观察 2）。
3. **O-WS-B2-3**：developer 证据行号与实际存在偏差（writeOff :227-253→:267-286；核销进度列 :190-196,295-297→:154-159,169,439-441；筛选 :323-331→:388-397；批量 :363→:362-364 等）——注记不精确、内容一致（QA 逐行比对通过）；同 O-TRACE2-1 类，建议后续证据行号以实际代码为准（同 REG-FIN-WS-002 观察 3）。
4. **O-WS-B2-4**：工作区多批次累计未提交（git status 大量 M/??，最近提交 cd1c89b 为 OIC-BE 阶段二批1），无法 git diff 精确隔离本批变更——验证方法限制（同 O-TRACE-4）；已用「developer 证据行号 + 代码现状交叉核验 + mtime 边界 + 变更段性质审查 + API 层零命中 grep」多重替代核验，证据充分（同 REG-FIN-WS-002 观察 4）。
5. **O-WS-B2-5**：ReceivableMapper.xml/PayableMapper.xml 参数名 startDate/endDate 与实际语义不一致（应收=due_date、应付=create_time）——命名卫生（功能不受影响）；**已随 F-1 同池登记后端整改池统一命名**（任务池 L3694）（同 REG-FIN-WS-002 观察 5）。
6. **O-WS-B2-6**：expand 字段 invoiceDate/invoiceNo 为 GAP-C4 登记项（ReceivableVO/PayableVO 无来源，'-' 兜底展示）——既有验收口径延续（ui-trace-b1/b3 已登记），保持既有登记链不重复登记（同 REG-FIN-WS-002 观察 6）。
7. **R1-OBS-1**：PayableTab.vue:78-84 hasActiveFilter 仍含 dueDateRange 判断分支——因 dueDateRange 恒 null（disabled 控件 + 不回填 + reset 置 null 三路径保证），该分支恒 false、无实际影响 = 冗余死分支（无害）；建议后端池就绪启用到期日筛选时随 F-1 反向操作一并清理（同 REG-FIN-WS-002 观察 7）。
8. **R1-OBS-2**：工作区多批次累计未提交——无法 git diff 精确隔离 R1 变更（同 O-WS-B2-4 口径）；已用「mtime 边界（PayableTab.vue 23:08:51 为 R1 唯一晚于 Batch 2 的文件）+ 证据行号逐项对照 + grep 零发送 + 代码现状全链读码」多重替代核验，证据充分（同 REG-FIN-WS-002 观察 8）。

---

### Batch WS-2 门禁

```text
Regression Result: PASS（2026-09-03 Batch WS-2 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为财务模块任务型重构·阶段三 Batch 2 核销工作台，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：1/1 卡 QA 独立验收 PASS（`docs/quality/ui-ws-b2-qa-report.md` 首验 7 大项中 6 大项 PASS + 强制核验 6/6，唯一 FAIL 点 F-1 应付「到期日区间」假筛选 → **P1-FIN-WS-002-R1 复验 PASS 闭环** `docs/quality/ui-ws-b2-r1-qa-report.md`：零死参数发送 + 控件 disabled+tooltip + 三路径恒 null + 应收侧/创建日期区间端到端有效保持 + 后端零改动 + 后端池登记落位 + typecheck 136=136 + build EXIT=0；F-1 闭环后转正）
- FAIL 明细：0（F-1 经 -R1 复验 PASS 闭环消除，无 FAIL 状态条目）
- PWL 明细：0（PD-028/033/035 为 BLOCKED，不占通过数——待办摘要为 UTM:141 任务语义非行状态列发明 / 收款冲正仅演进标注 / 导出规则待决；应付到期日筛选、导出、批量为登记处置/登记依赖非 FAIL——disabled + tooltip 明示，不伪造，后端池排期 L3694）
- 基线：新增 REG-FIN-WS-002（正式 88 → 89 条，只增不减、既有条目零修改）
- 观察项：O-WS-B2-1~6 + R1-OBS-1/2 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch WS-3 回归（财务模块任务型重构·阶段三 Batch 3 记录工作台，2026-09-04）

> 验收依据：`docs/quality/ui-ws-b3-qa-report.md`（2026-09-04，QA 独立首验：P1-FIN-WS-003 记录工作台 = **FAIL**——页签化合并（修正②）/ 路由重定向（确认①）+ A01 穿透目标修复 / 工具栏处置 / 筛选修复 4/5（voucherNo、voucherStatus 键名修正、期间 ✅；referenceNo 死参数登记 ✅；联动 B-3/B-4 ✅；报销四维 ✅）+ **唯一 FAIL 点 F-1 = 凭证类型筛选死参数假筛选** / KL-060 登记不猜测 + KL-061 处置 / 增强能力零回归 / 强制核验 6/6 全过；F-1 = 凭证页签「凭证类型」筛选死参数假筛选（前端控件 LedgerTab.vue:122-130,141 发送 number 型 voucherType 7 值语义 1-7 → voucher.ts mapQueryParams:36 解构丢弃 + :43-45 仅字符串分支 → 后端收不到参数，消费链断点在**前端 API 映射层**，后端 DTO/Service/XML 消费链就绪）→ 生成 **P1-FIN-WS-003-R1** 返回 DOING）+ **`docs/quality/ui-ws-b3-r1-qa-report.md`（2026-09-04，QA 独立复验 = **PASS**，F-1 闭环）**；对照基准 = 任务板 §11.3 P1-FIN-WS-003 细化卡（验收标准 8 条 + developer 修改证据 L3733-3782 + R1 证据 L3784-3797）+ 设计 V1.1 §3.3/§8（修正②/确认①/②/⑤/⑥/⑦）+ 既有验收口径（`docs/quality/ui-fin-b1-qa-report.md` P1-UI-FIN-002 账本 Batch0-方案3 + `docs/quality/ui-trace-b1-qa-report.md` TRACE-C02 契约键）+ Batch 2 F-1 教训（消费链端到端核验纪律）+ 导出盘点（P1-FIN-EXPORT-001）+ roadmap §5.12 硬约束；开发者未自行宣布通过 ✅——QA 报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论（首验 FAIL → -R1 复验 PASS 闭环后转正）新增 REG-FIN-WS-003（维护规则 5 禁止推测），无推测内容；F-1 属 FAIL 后闭环转正固化（同 P1-UI-CORE-007 / P1-FIN-WS-002 -R1 转正先例口径）。
> 批次口径：本批为**治理批次**（财务模块任务型重构·阶段三 Batch 3 记录工作台，任务池 §11.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch 4 合规工作台（§11.3 批次节奏，禁止跳步）。
> 基线计数核对：追加前正式基线 **89 条**（文件实况：基线总览表 89 行，含 REG-FIN-WS-001/002；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 1 条（REG-FIN-WS-003）后 **90 条**，只增不减纪律达成（既有 89 条条目零修改）；候选 0 条维持。

### REG-FIN-WS-003

- **测试编号**：REG-FIN-WS-003
- **模块**：财务-记录工作台（frontend/src/views/finance/FinanceRecords.vue 新建·壳 + components/LedgerTab.vue / ReimbursementTab.vue / TransferTemplateTab.vue 新建·页签子组件【FinanceLedger.vue / AutoVoucher.vue / InvoiceReimbursement.vue 已删除，内容迁移】+ router/index.ts + modules/finance/menu.ts + api/finance/voucher.ts + transfer-template.ts + types/finance.ts + views/finance/FinanceFund.vue 穿透目标单行；后端对照 AutoVoucherController.java / FinanceVoucherQueryDTO.java / VoucherServiceImpl.java / FinanceVoucherMapper.xml / TransferTemplateServiceImpl.java / InvoiceServiceImpl.java【只读确认，零改动】）
- **关联任务**：P1-FIN-WS-003（Batch WS-3；QA 首验 FAIL F-1 → **P1-FIN-WS-003-R1 复验 PASS 2026-09-04** `docs/quality/ui-ws-b3-r1-qa-report.md`，F-1 闭环后转正）
- **业务场景**：记录工作台（账本/凭证/报销/联动规则页签化合并）——**页签化合并（修正②）**（FinanceRecords.vue:92 PageHeader「记录工作台」+ :95-114 待办区【凭证待办 = LedgerTab emit draft/audited 计数、报销审批待办 = ReimbursementTab emit draft 计数——真实字段派生 + 分页范围登记，未加载显示「加载中…」不虚构；KL-060 登记卡 :105-108 + KL-061 登记卡 :109-112】+ :117-127 el-tabs 三页签 + :72-76 handleTabChange router.replace 同步 query【不产生历史堆叠】+ :79-87 watch(route.query.tab) 外部导航同步 + :35-39 初始读 route.query.tab 深度链接直达 + :43/:124 scaleLevel 折叠确认⑤【仅 chain-enterprise 可见联动规则页签，三路径回落 ledger】）；**路由重定向（确认①）+ A01 穿透目标修复**（router/index.ts:116 新路由 /finance/records name FinanceRecords meta 仅 domain + :117-119 3 条旧路由带 tab 重定向 + :113 /finance 根 redirect → /finance/records；FinanceFund.vue:316-319 handleJumpToVoucher → `router.push({ name: 'FinanceRecords', query: { tab: 'ledger', voucherId: row.voucherId } })`【旧 name FinanceLedger 失效修复，FundFlowVO.java:52 穿透键】+ LedgerTab.vue:320-328 onMounted voucherId → fetchAndShowDetail 复用 voucherApi.getById 详情弹层【零新增接口】；grep 全仓旧路由 name 引用零命中；menu.ts:17-18 菜单 3 项合并「记录工作台」单项）；**工具栏处置**（凭证页签 LedgerTab.vue:527-546：**批量过账真实接入** :291-318 handleBatchPost【仅 audited 勾选 :283/:292 + ElMessageBox 二次确认 :298-302 → voucherApi.batchPost :303（voucher.ts:185-190 既有端点封装，AutoVoucherController.java:178-181 @PostMapping /vouchers/batch-post Result<Boolean>）→ BatchResultFeedback :305-309 提交数口径（total = 提交 audited 数，后端布尔 true → success=提交数 / false → failed=全部）+ tooltip 明示「后端仅返回布尔结果，精确 N/M 明细待后端返回（已登记后端池）」:531-532】+ 批量审核/导出/凭证导入 disabled+tooltip 登记（:534-543，无端点不伪造，无逐条循环调用既有端点）；报销页签 ReimbursementTab.vue:411-419：**红冲/作废真实接入** handleRedFlush :200-215 / handleVoid :218-233【ElMessageBox 二次确认 :202-206/:220-224 → invoiceApi.redFlush/void :207/:225，invoice.ts:137-152 既有端点 mtime 08-11 零改动】+ 行内按钮仅 issued 态显示 :472-475 + 审批流保留（确认⑥，:258-277 confirmApprove 通过=issued/驳回=cancelled 既有语义 + :505-521 审批对话框 + kl060-note :521）；联动规则页签 TransferTemplateTab.vue:344-352：**CRUD 真实接入** handleCreate :345-347 → :264-269 / handleEdit :398 → :272-290 回填 → submitForm :293-333【transferTemplateApi.create/update/delete，transfer-template.ts:85-105 既有端点 + 表单字段对齐 TransferTemplateCreateDTO/UpdateDTO :308-317】+ handleDelete :400 → :213-228 二次确认 :215-219 + 启停 toggleEnabled 既有 :199-210【契约对齐后 row.id = String(t.templateId) :104 不再 undefined 断流】+ 科目选择器 subjectApi.getLeafSubjects :176-185【失败不阻断，回退 科目#ID 不虚构】）；**筛选修复（消费链端到端核验——Batch 2 F-1 教训）**（凭证页签：voucherNo ✅ LedgerTab.vue:140 → voucher.ts:36-37 rest 透传 → FinanceVoucherQueryDTO.voucherNo:14 → VoucherServiceImpl.getPage:231-233 → FinanceVoucherMapper.xml:10-12 voucher_no LIKE；voucherStatus ✅ :142 status → voucher.ts:40-42 **键名修正 status→voucherStatus**（旧键名无法绑定后端 DTO = 假筛选）→ DTO:32 → Service:233 → XML:22-24 voucher_status eq；期间 ✅ :143-146 → XML:13-18 voucher_date 区间；**voucherType = F-1 防回归点【R1 闭环后】**：LedgerTab.vue:141 发送 number（7 值语义 1-7）→ voucher.ts:43-45 **数字分支**（`if (voucherType !== undefined) { result.voucherType = typeof voucherType === 'string' ? VoucherTypeMap.toBackend[voucherType] : voucherType }`——数字 1-7 直传后端 7 值语义、字符串分支 VoucherTypeMap 1~4 映射语义不变）→ FinanceVoucherQueryDTO.java:26 Integer → VoucherServiceImpl.java:231-233 第 5 参 → FinanceVoucherMapper.xml:19-21 voucher_type eq——全链 5 环节就绪；referenceNo 死参数零发送登记 ✅（DTO:35 有字段但 Service 未传 mapper、XML 无过滤 → 不补控件零发送，后端池登记：Service 补传 + Mapper 参数 + XML reference_no LIKE）；联动规则页签 **B-3/B-4 映射修正端到端有效**：TransferTemplateTab.vue:91-98 keyword→params.templateName / ruleStatus→params.enabled → transfer-template.ts:28-41 enabled→result.isEnabled、templateName→result.keyword【原无映射 = 假筛选已修正】→ TransferTemplateQueryDTO 绑定 → TransferTemplateServiceImpl.getPage:118-126 eq/like 消费 → MyBatis-Plus SQL 生效；报销页签四维端到端有效保持 ✅ ReimbursementTab.vue:139-144 → InvoiceServiceImpl.getPage:96-119 invoiceNo like / invoiceType eq / startDate ge + endDate le invoice_date + E 值格式修复 value-format=YYYY-MM-DD）；**KL-060（确认⑦，登记不猜测）**（ReimbursementTab.vue:266 status: action === 'approve' ? 'issued' : 'cancelled' 既有语义 + :521 kl060-note 明示 + FinanceRecords.vue:105-108 登记卡——未发明驳回独立状态、未改造审批表单，零 AI 补充业务规则）；**KL-061（P2 处置）**（TransferTemplateTab.vue:148-149 「最近执行」伪语义列移除 → 「创建时间」createTime 真实字段 :110，grep 无残留）；**契约对齐（联动规则）**（types/finance.ts:988-1045 FinanceTransferTemplate 对齐 TransferTemplateVO templateId/isEnabled/templateType 数字/sourceSubjectId/targetSubjectId/amountExpression/summaryTemplate + TransferTemplateTab.vue:103-113 展示转换【id: String(t.templateId) :104 / ruleName: t.templateName :105 / status: t.isEnabled ? 'active' : 'inactive' :111 / accountTpl 科目名经 getLeafSubjects 映射，缺失回退 科目#ID 不虚构】——原类型漂移 id/enabled/frequency/debitSubject 导致 toggleEnabled(row.id)=undefined 断流已修复）；**增强能力零回归**（凭证页签：来源单据展示 C02 契约键 referenceNo/sourceType/sourceId LedgerTab.vue:627-638 + 穿透说明登记 :634-637 / voucherId 穿透接收端 :324-327 / expand 分录明细 :623-657 selectable=false 未照搬 size="small" + 借/贷合计 :646-655 / 合计 useSummary unit='fen' :224 与账本既有口径一致 / 分页 :50,598-601 / density="auto" :593 / 固定列 :209,216 / 筛选保存 storage-key="finance-ledger-filter" 保持 :556 / CaliberNoteBar :578 / 失败态+重试 :579-586 / Batch0-方案3 状态映射 VoucherStatusMap 0-3 零变更 :102-118 + 按钮逻辑 :608-620 与 ui-fin-b1 验收一致 / 统计卡片 :182-192 / 详情弹层 :669-690；报销页签：审批流/统计卡片/详情/新增弹层全量迁移 :96-108/:482-530/:532-602 + 筛选保存 finance-reimbursement-filter 新 key :429 + 失败透传 :448-455；联动规则页签：启停/详情/统计卡片全量迁移 :128-140/:199-210/:406-422 + 筛选保存 finance-transfer-template-filter 新 key :361 + 失败态 :376-383 + 空 scoped style 零自造）；**强制核验 6/6**（① git diff 无业务语义/接口/状态/金额口径变更【API 端点集合与改动前一致，batchPost/void/redFlush/CRUD 均为既有端点 UI 接入，旧三页删除 git status D 实锤】② 不新增后端接口【6 后端文件 mtime 全 2026-08-11 早于本批前端 09-03 23:58~09-04 00:08】③ 视觉同源【四新文件 grep V3-A 零命中 + 硬编码颜色零命中，scoped 仅 var(--fts-*) tokens 32 处】④ 死参数零发送 + PD-028 未落库不悬空 + P1-STOCK-001 零触碰【referenceNo 零发送 9 处命中全为注释/类型/展示；startDueDate/endDueDate 三新页签零命中；待办摘要基于真实 status 字段派生 + 分页范围登记；FinanceTax.vue mtime 08-31 = KL-059 零触碰】⑤ 构建独立复跑【typecheck 136=136 本批 11 文件零命中 + build EXIT=0，新 chunk FinanceRecords-* 产出】⑥ 开发者未自行宣布通过）
- **测试目的**：防止记录工作台页签化合并重构回归 —— 三页签化合并 + 路由重定向（旧路由直达/深度链接/A01 穿透不失效）必须保持；工具栏真实接入不得回退为假入口（批量过账/红冲/作废/联动 CRUD 既有端点接线 + 二次确认）；筛选必须端到端有效或如实登记（**F-1 防回归点：voucherType 数字 1-7 直传——voucher.ts mapQueryParams 数字分支不得回退为仅字符串分支，后端 XML voucher_type eq 必须收得到参数；voucherStatus 键名 status→voucherStatus 修正不得回退**）；referenceNo 死参数零发送纪律（假筛选不得复活）；KL-060 登记不猜测（驳回独立状态不得被发明）；KL-061 伪语义列不得回退；导出/批量审核/凭证导入不得伪造动作（disabled + tooltip 明示登记依赖）；增强能力零回归（C02 键/voucherId 穿透/expand/useSummary unit='fen'/Batch0-方案3 等）；后端零改动、PD-028/033/035 + KL-060 BLOCKED 不占通过数。
- **测试步骤**：
  1. 页签化合并核验：FinanceRecords.vue:92 PageHeader / :95-114 待办区（真实字段派生 + 分页范围登记，未加载「加载中…」不虚构）/ :117-127 el-tabs 三页签 + tab query 同步（:72-76 router.replace + :79-87 watch 无循环风险）/ :43/:124 scaleLevel 折叠（非 chain-enterprise 直达 transfer-template 回落 ledger 三路径）
  2. 路由重定向核验：router/index.ts:116-119 新路由 + 3 旧路由带 tab 重定向 + :113 根 redirect；FinanceFund.vue:316-319 穿透目标修复（name FinanceRecords + { tab: 'ledger', voucherId }）；grep `name: 'FinanceLedger'|name: 'AutoVoucher'|name: 'InvoiceReimbursement'` 全仓零命中；LedgerTab.vue:320-328 onMounted voucherId 定位链路
  3. 工具栏处置核验：批量过账全链（勾选仅 audited → 二次确认 → voucherApi.batchPost（voucher.ts:185-190 既有端点封装）→ BatchResultFeedback 提交数口径 + tooltip 明示布尔口径）；红冲/作废（invoice.ts:137-152 既有端点 + 二次确认 + 仅 issued 态显示）；联动 CRUD（transfer-template.ts:85-105 既有端点 + 表单字段对齐 DTO + getLeafSubjects 科目选择器）；登记项（批量审核/导出/凭证导入）disabled + tooltip，grep 无 CSV/Blob/download 代码接入、无逐条循环调用既有端点
  4. 筛选端到端核验（Batch 2 F-1 教训）：凭证 voucherNo/voucherStatus（键名修正）/期间 三链端到端（控件 → API 映射 → DTO → Service → XML）；**voucherType F-1 防回归点**（R1 后）：voucher.ts:43-45 数字分支存在 + LedgerTab.vue:141 发送 number + 后端 DTO:26/Service:233/XML:19-21 消费链全链就绪；referenceNo 零发送（grep 9 处命中全为注释/类型/展示）；联动 B-3/B-4 映射（transfer-template.ts:28-41 enabled→isEnabled、templateName→keyword → Service:118-126 eq/like）；报销四维端到端（InvoiceServiceImpl:96-119）+ E 值格式
  5. KL-060/KL-061 核验：grep 无「驳回」独立状态值/无新状态映射；审批流保留既有语义（通过=issued/驳回=cancelled）；「最近执行」grep 无残留；创建时间真实字段
  6. 增强能力逐项核验（C02 契约键/voucherId 穿透/expand/useSummary unit='fen'/固定列/density/筛选保存 storage-key/CaliberNoteBar/失败态/Batch0-方案3 零变更/统计卡片/详情弹层/审批流）
  7. 强制核验独立复跑：后端零改动（6 文件 mtime 全 2026-08-11 + git status）；`npx vue-tsc --noEmit` = 136 条 error（= 存量基线 136=136，本批 11 文件零命中）；`npm run build` EXIT=0（QA 独立执行）
- **预期结果**：
  - 页签化合并保持：壳 + 待办区真实字段派生（不虚构）+ 三页签 + tab query 同步 + scaleLevel 折叠；旧路由带参重定向直达页签（书签/深度链接不失效）；A01 穿透跳转新 name 可达（voucherId 定位）
  - 工具栏真实接入保持：批量过账（仅 audited + 二次确认 + BatchResultFeedback 提交数口径）/ 红冲/作废（二次确认）/ 联动 CRUD（二次确认 + 科目选择器）均为既有端点接线；登记项 disabled + tooltip 明示，不伪造、不逐条循环
  - 筛选有效或如实登记：凭证 voucherNo/voucherStatus/期间端到端有效；**voucherType 数字 1-7 直传（F-1 防回归点，数字分支不得回退）**；referenceNo 零发送；联动 B-3/B-4 端到端有效；报销四维端到端有效
  - KL-060 登记不猜测（驳回独立状态不得发明）；KL-061 伪语义列移除保持；契约对齐保持（toggleEnabled 不再 undefined 断流）
  - 增强能力零回归（C02 键/voucherId 穿透/expand/useSummary unit='fen'/Batch0-方案3）；后端零改动；PD-028/033/035 + KL-060 BLOCKED 不占通过数；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + 构建独立复跑）
- **当前状态**：PASS（2026-09-04，R1 复验闭环转正）
- **RVT 回退标注（2026-09-04，只标注不删除——任务板 §12.4 方案）**：本条目**合并断言已过时**——回退后现状：/finance/records 路由已移除（grep 零命中）、菜单「记录工作台」合并单项已恢复为「财务总账」「发票报销」「自动凭证」独立项（HEAD 原命名）、FinanceRecords.vue 壳文件保留未删除（删除归 RVT-003）；合并断言由 **REG-FIN-RVT-003** 接管（RVT-003 未完成前现状 = 结构已回退、三页签壳待回迁）；**A01 穿透断言已过时**——FinanceFund.vue:318 穿透目标 FinanceRecords 已随路由移除失效，改回 FinanceLedger 归 **RVT-002** 执行（单行改动），由 **REG-FIN-RVT-002** 覆盖；本条目功能断言（voucherType 数字分支 F-1 防回归点/voucherStatus 键名/batchPost/红冲作废/联动 CRUD/KL-060·061）回退后仍有效，由 REG-FIN-RVT-003 保留项断言衔接，无冲突。
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：fund_flows.status 未落库（与 Batch0-方案2 同批落库）——本批待办摘要基于真实 status 字段派生（draft/audited 计数）+ 分页范围登记（FinanceRecords.vue:58-68 + LedgerTab:195-201 + ReimbursementTab:111-113），零新增「待核销」状态列、无 status 字段推断；FundFlowStatusMap 悬空声明维持登记链（B01 承接）；本卡未借机实现 PD-028 任何内容
  - **PD-033（BLOCKED，不占通过数）**：收款冲正待决——本批零触碰（记录工作台范围外，既有登记链维持）
  - **PD-035（BLOCKED，不占通过数）**：导出规则缺失——凭证/报销/联动规则三页签导出按钮 disabled + tooltip 明示 EXPORT-001 盘点结论（无导出端点 → 登记后端池 P1-FIN-EXPORT-004），不伪造导出
  - **KL-060（BLOCKED 口径，不占通过数）**：驳回=作废共用 cancelled（P2 语义混叠）状态机语义拆分涉及业务规则 → 无产品/后端依据登记不猜测（不发明驳回独立状态、不改造审批表单）；审批流保留既有语义（确认⑥）；产品决策后回写任务卡验收标准
  - **referenceNo 筛选 = 登记处置非 FAIL（不占通过数）**：后端 FinanceVoucherQueryDTO.referenceNo 有字段但 VoucherServiceImpl.getPage:231-233 未传 mapper（selectVoucherPage 签名无 referenceNo）、FinanceVoucherMapper.xml 无过滤 → 前端不补控件不发送死参数（grep LedgerTab.vue 9 处命中全为注释/类型/expand 展示，零 params 发送）；后端池登记：Service 补传 referenceNo + Mapper 参数 + XML reference_no LIKE（对照 voucherNo 实现），后端就绪并 QA PASS 后补控件（届时登记，不预固化）
  - **批量过账 N/M 口径 = 登记依赖非 FAIL（不占通过数）**：batch-post 端点（AutoVoucherController.java:178-181）返回 boolean 无 N/M 明细 → BatchResultFeedback 按提交数口径（total = 提交 audited 数；后端 true → success=提交数 / false → failed=全部）+ tooltip 明示；精确跳过明细登记后端池（batchPostVouchers 返回 successCount/失败明细），后端就绪并 QA PASS 后扩展断言（届时登记，不预固化）
  - **批量审核/凭证导入 = 登记依赖非 FAIL（不占通过数）**：后端无批量审核端点（仅 /vouchers/{id}/approve 单条）+ 无导入端点（诊断 §1.2「缺」）→ 按钮 disabled + tooltip 明示，不逐条循环调用既有端点（超展示层红线）
  - **voucherType 数字直传与字符串映射并存说明**：VoucherTypeMap 1~4 展示映射（receipt/payment/transfer/general）与后端 7 值筛选语义（1-手工…7-转账）口径差异为既有状态（converters.ts:140-145 零改动）；筛选数字直传后端 7 值语义（R1 未触碰展示映射）
  - 观察 O-WS-B3-1（QA 报告 §九，登记不判 FAIL）：developer 证据行号偏差——证据⑤「voucherType 数字直传（voucher.ts:46-50）」实际数字分支不存在（:43-45 仅字符串，F-1 根因）；证据⑧「density='auto'（:349）」实际 :593 等——注记不精确（F-1 已单列闭环）；其余行号以实际代码为准（同 O-WS-B2-3 类）
  - 观察 O-WS-B3-2（QA 报告 §九，登记不判 FAIL）：后端池登记延续（任务卡 L3771-3775）——referenceNo 筛选补传 / batch-post successCount 明细 / 批量审核端点 / 凭证导入端点 / 导出端点（P1-FIN-EXPORT-004）/ KL-060 状态机语义拆分（待产品决策）——全部 BLOCKED 不占通过数，前端登记处置合规（disabled+tooltip/零发送）
  - 观察 O-WS-B3-3（QA 报告 §九，登记不判 FAIL）：联动规则统计卡片「凭证模板数」= 规则总数（无独立模板数据字段，当前页派生）——展示口径（沿用 AutoVoucher 既有统计卡片语义），不虚构（真实字段派生）
  - 观察 O-WS-B3-4（QA 报告 §九，登记不判 FAIL）：工作区多批次累计未提交（git status 612 项，最近提交 cd1c89b 为 OIC-BE 阶段二批1），无法 git diff 精确隔离本批变更——验证方法限制（同 O-WS-B2-4 口径）；已用「developer 证据行号 + 代码现状交叉核验 + mtime 边界（后端全部 08-11 / FinanceTax 08-31 早于本批 09-04）+ 变更段性质审查 + grep 零发送/零残留」多重替代核验，证据充分
  - 观察 R-WS-B3-R1-1（R1 复验报告 §六，登记不判 FAIL）：字符串 '' 传入行为：`typeof '' === 'string'` → VoucherTypeMap.toBackend[''] = undefined → result.voucherType = undefined（键存在但值为 undefined，序列化时不携带）——before 既有行为（原代码同路径），R1 未改变；且 LedgerTab.vue:141 守卫 `!== ''` 已排除空串发送
  - 观察 R-WS-B3-R1-2（R1 复验报告 §六，登记不判 FAIL）：工作区累计多批次未提交（git status 612 项），无法 git diff 精确隔离 R1 单行——验证方法限制（同 O-WS-B3-4）；以 voucher.ts mtime（批内最新 00:17:26）+ diff 修复段性质 + 代码现状交叉核验替代，证据充分
  - 观察 R-WS-B3-R1-3（R1 复验报告 §六，登记不判 FAIL）：端到端实证为读码级（未起前端服务实调后端）；后端为只读核验——验证方法边界（同 Batch 2/3 既有口径）；消费链 5 环节均为既有已验收透传路径（voucherNo/voucherStatus 同链已验），R1 仅补数字分支，风险低

---

### PD-028/PD-033/PD-035/KL-060 限制登记同步（Batch WS-3，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §11.3 P1-FIN-WS-003 L3771【referenceNo 死参数登记】/ L3772【批量过账 N/M 口径登记】/ L3773【批量审核登记】/ L3774【凭证导入登记】/ L3775【导出登记 EXPORT-001 + PD-035】/ L3776【KL-060 BLOCKED 口径】/ L3777【KL-061 P2 登记】/ L3779【凭证类型筛选口径】）→ 决策池（`product-decision-backlog.md:44/155` PD-028 2026-08-31 登记；`:172` PD-035 导出规则 2026-09-03 登记；PD-033 冲正登记在案；KL-060 状态机语义拆分待产品决策登记在案）→ roadmap（`remediation-roadmap.md` §5.12：后端缺口仅登记不混入 + Batch 3 记录工作台）→ QA 报告（`docs/quality/ui-ws-b3-qa-report.md` §五/§七/§九 + `docs/quality/ui-ws-b3-r1-qa-report.md` §五/§六/§七）——登记链完整一致。
- **内容**：① PD-028——资金流水「待核销」状态语义缺失，`fund_flows.status` 与 Batch0-方案2 同批落库；落库前前端不实现、不悬空、不伪造（本批待办摘要基于真实 status 字段派生，非行状态列发明）。② PD-033——收款冲正待决（本批范围外，登记链维持）。③ PD-035——导出规则缺失（文件格式/行数上限/导出口径未定义）；三页签导出按钮 disabled + tooltip 明示 EXPORT-001 盘点结论（无导出端点 → 后端池 P1-FIN-EXPORT-004 登记）。④ KL-060——驳回=作废共用 cancelled（P2 语义混叠）状态机语义拆分涉及业务规则 → 登记不猜测（不发明驳回独立状态、不改造审批表单），产品决策后回写任务卡验收标准。
- **本批执行确认（QA 实锤）**：KL-060 零 AI 补充业务规则（grep 无「驳回」独立状态值/无新状态映射；审批流保留既有语义）；KL-061 伪语义列移除（「最近执行」grep 无残留，创建时间真实字段）；待办摘要真实字段 + 分页范围登记；referenceNo 零死参数发送（grep 9 处命中全为注释/类型/展示）；startDueDate/endDueDate 三新页签零命中（Batch 2 F-1 教训范围零复发）；批量过账/红冲/作废/CRUD 均为既有端点 UI 接入（后端 6 文件 mtime 全 08-11 零改动）；P1-STOCK-001 零触碰（grep 命中均为既有 stockinNo 字段/「库存现金」科目名）；FinanceTax.vue mtime 08-31 = KL-059 零触碰。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-FIN-WS-003）**不受影响**——PD-028/033/035 + KL-060 相关均为**登记项非 FAIL**（待办摘要任务语义 + 登记卡 + disabled 登记态）；referenceNo/批量审核/导入/导出/batch-post 明细为**登记依赖非 FAIL**（后端池登记，前端 disabled+tooltip 明示/零发送，不伪造）；勾稽状态列由 B01 占位卡承接（Batch0-方案2 同批落库后实现）；导出端点由后端池 + PD-035 决策承接。

---

### QA 观察项登记（Batch WS-3，如实登记不销号、不猜测处置）

按 QA 报告 §九 观察项（O-WS-B3-1~4）+ R1 复验报告 §六 风险与观察项（R-WS-B3-R1-1~3）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-WS-B3-1**：developer 证据行号偏差——证据⑤「voucherType 数字直传（voucher.ts:46-50）」实际数字分支不存在（:43-45 仅字符串，F-1 根因）；证据⑧「density='auto'（:349）」实际 :593 等——注记不精确（F-1 已单列闭环）；其余行号以实际代码为准（同 REG-FIN-WS-003 观察 1）。
2. **O-WS-B3-2**：后端池登记延续（任务卡 L3771-3775）——referenceNo 筛选补传 / batch-post successCount 明细 / 批量审核端点 / 凭证导入端点 / 导出端点（P1-FIN-EXPORT-004）/ KL-060 状态机语义拆分（待产品决策）——全部 BLOCKED 不占通过数，前端登记处置合规（disabled+tooltip/零发送）（同 REG-FIN-WS-003 观察 2）。
3. **O-WS-B3-3**：联动规则统计卡片「凭证模板数」= 规则总数（无独立模板数据字段，当前页派生）——展示口径（沿用 AutoVoucher 既有统计卡片语义），不虚构（真实字段派生）（同 REG-FIN-WS-003 观察 3）。
4. **O-WS-B3-4**：工作区多批次累计未提交（git status 612 项，最近提交 cd1c89b 为 OIC-BE 阶段二批1），无法 git diff 精确隔离本批变更——验证方法限制（同 O-WS-B2-4 口径）；已用「developer 证据行号 + 代码现状交叉核验 + mtime 边界（后端全部 08-11 / FinanceTax 08-31 早于本批 09-04）+ 变更段性质审查 + grep 零发送/零残留」多重替代核验，证据充分（同 REG-FIN-WS-003 观察 4）。
5. **R-WS-B3-R1-1**：字符串 '' 传入行为——`typeof '' === 'string'` → VoucherTypeMap.toBackend[''] = undefined → result.voucherType = undefined（序列化时不携带）——before 既有行为（R1 未改变），且 LedgerTab.vue:141 守卫 `!== ''` 已排除空串发送（同 REG-FIN-WS-003 观察 5）。
6. **R-WS-B3-R1-2**：工作区累计多批次未提交——无法 git diff 精确隔离 R1 单行（同 O-WS-B3-4 口径）；以 voucher.ts mtime（批内最新 00:17:26）+ diff 修复段性质 + 代码现状交叉核验替代，证据充分（同 REG-FIN-WS-003 观察 6）。
7. **R-WS-B3-R1-3**：端到端实证为读码级（未起前端服务实调后端）；后端为只读核验——验证方法边界（同 Batch 2/3 既有口径）；消费链 5 环节均为既有已验收透传路径（voucherNo/voucherStatus 同链已验），R1 仅补数字分支，风险低（同 REG-FIN-WS-003 观察 7）。

---

### Batch WS-3 门禁

```text
Regression Result: PASS（2026-09-04 Batch WS-3 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为财务模块任务型重构·阶段三 Batch 3 记录工作台，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：1/1 卡 QA 独立验收 PASS（`docs/quality/ui-ws-b3-qa-report.md` 首验：页签化合并 / 路由重定向 + A01 穿透修复 / 工具栏处置 / 筛选修复 4/5 / KL-060+KL-061 / 增强能力零回归 7 大项 PASS + 强制核验 6/6，唯一 FAIL 点 F-1 凭证类型筛选死参数假筛选 → **P1-FIN-WS-003-R1 复验 PASS 闭环** `docs/quality/ui-ws-b3-r1-qa-report.md`：voucher.ts:43-45 数字分支与 QA 指定方向逐字符一致（数字 1-7 直传、字符串映射语义不变）+ 端到端消费链 5 环节全链就绪 + 后端零改动 + typecheck 136=136 + build EXIT=0；F-1 闭环后转正）
- FAIL 明细：0（F-1 经 -R1 复验 PASS 闭环消除，无 FAIL 状态条目）
- PWL 明细：0（PD-028/033/035 + KL-060 为 BLOCKED，不占通过数——待办摘要真实字段派生非行状态列发明 / 收款冲正范围外 / 导出规则待决 / 驳回语义混叠登记不猜测；referenceNo 筛选、批量审核、凭证导入、导出、batch-post N/M 明细为登记依赖非 FAIL——后端池登记，disabled+tooltip 明示/零发送，不伪造）
- 基线：新增 REG-FIN-WS-003（正式 89 → 90 条，只增不减、既有条目零修改）
- 观察项：O-WS-B3-1~4 + R-WS-B3-R1-1~3 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch WS-4 回归（财务模块任务型重构·阶段三 Batch 4 合规工作台，2026-09-04）

> 验收依据：`docs/quality/ui-ws-b4-qa-report.md`（2026-09-04，QA 独立验收：P1-FIN-WS-004 合规工作台 = **PASS**；验收标准 8 条全达成 + 强制核验 6/6 全过、无 FAIL、无 -R；对照基准 = 任务板 §11.3 P1-FIN-WS-004 细化卡（验收标准 8 条 L3819-3827 + developer 修改证据 L3829-3857）+ 设计 FIN-WB-DESIGN-V1.1（`docs/design/finance-task-workbench-design-20260903.md` §3.4 + §2.2）+ 诊断 §1.5（`docs/quality/finance-module-diagnosis-20260903.md`）+ KL-059（`production-known-limitations.md:772-779`）+ 导出盘点（`docs/quality/finance-export-inventory-20260903.md` §5.3 P1-FIN-EXPORT-004）+ roadmap §5.12 硬约束；开发者未自行宣布通过 ✅——本报告为唯一验收结论）。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-WS-004（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（财务模块任务型重构·阶段三 Batch 4 合规工作台，任务池 §11.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch 5 毛利核算+决策工作台（P1-FIN-WS-005）随批放行细化（§11.3 批次节奏，禁止跳步）。
> 基线计数核对：追加前正式基线 **90 条**（文件实况：基线总览表 90 行，含 REG-FIN-WS-001/002/003；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 1 条（REG-FIN-WS-004）后 **91 条**，只增不减纪律达成（既有 90 条条目零修改）；候选 0 条维持。

### REG-FIN-WS-004

- **测试编号**：REG-FIN-WS-004
- **模块**：财务-合规工作台（frontend/src/views/finance/FinanceTax.vue 重构 + modules/finance/menu.ts + router/index.ts；后端对照 TaxRecordController.java / TaxRecordDTO.java / TaxRecordServiceImpl.java / TaxRecordMapper.java / TaxRecordService.java【只读确认，零改动】）
- **关联任务**：P1-FIN-WS-004（Batch WS-4；QA 独立验收 PASS 2026-09-04 `docs/quality/ui-ws-b4-qa-report.md`）
- **业务场景**：合规工作台（FinanceTax 重构，范围 = 五区布局 + 工具栏补齐 + KL-059 税期动态化 + 增强能力接入 + 零回归）——**五区布局**（① 待办区 :679-702 workbench-taskboard【待缴税项任务卡 = pendingTaxSummary computed :638-650 基于 Tab1 真实字段派生【UNPAID/OVERDUE 计数 + taxAmount 合计 + overdueCount 逾期摘要，taxStatus UNPAID/PAID/OVERDUE 为 TaxRecordDTO.java:39 既有字段语义——零虚构、零新状态发明】，recordLoaded 未加载/失败显示「待缴税项摘要加载中…」不虚构 :639/:648（recordLoaded 仅 loadTaxRecords 成功路径置 true :132）+ 缴税幂等 PD-001 登记卡 :686-689 纯展示【幂等键 = 税种+所属期+凭证号 + 后端幂等闭环 + 前端单飞守卫，零动作逻辑】+ 统计卡片 4 张保留 :691-701 v-for statsCards】/ ② 工具栏 :704-713【申报表打印 :706 handlePrint :655-657 = **window.print 真实接入**（纯浏览器能力零接口依赖，grep 无伪造打印/Blob/下载代码）；导出 :707-709 disabled + el-tooltip 明示「P1-FIN-EXPORT-001 盘点：税务记录/申报表/税率配置均无导出端点（需新增并登记后端池 P1-FIN-EXPORT-004）→ 登记，不伪造导出」；批量缴税 :710-712 disabled + tooltip 明示「批量缴税需批量端点（tax-calculation.ts 仅单条 payTax / TaxRecordController 无批量缴税端点，仅 batchDelete 删除）→ 登记…不逐条循环调用既有端点（超展示层红线）」】/ ③ 筛选区（页签内保留 A 类有效：Tab1 税种/纳税期间/状态 :720-738（recordSearchForm :91-95）、Tab2 税种/申报期间 :767-783、Tab3 税种/状态 :809-828（searchForm :433-436））/ ④ 数据区 el-tabs 3 页签保留 :717（records :719-763 / declaration :766-805 / config :808-852，activeTab 默认 'records' :45）/ ⑤ 详情区既有弹层 5 个零改动 :857/:875/:908/:928/:945（grep el-dialog 计数 = 5））；**KL-059 税期动态化（E 修复）**（buildTaxPeriodOptions :54-66【TAX_PERIOD_COUNT = 4 :51 与既有硬编码选项数一致 + `new Date(now.getFullYear(), now.getMonth() - i, 1)` 循环 + padStart → value = `${year}${month}` YYYYMM】+ taxPeriodOptions :70【Tab1 筛选，label = YYYYMM 与既有格式一致（诊断 §1.5 原硬编码 202603~202606 同为 YYYYMM）】+ declarationPeriodOptions :73【Tab2 申报期间，同一生成函数 label = YYYY年MM月】+ currentPeriod :76-80 + :277 `period: currentPeriod()` 初始化 declarationForm【Tab2 默认期间 = 当前期，默认值与动态选项脱节消除】；**Node vm 沙箱执行工作区真实源码行为实证 6/6 PASS**【① 2026-09-04 当期 → ["202609","202608","202607","202606"]（当前期在选项中）② 2026-01-15 跨年 → ["202601","202512","202511","202510"] ③ 2026-06-15 → ["202606","202605","202604","202603"] 与既有硬编码集合完全等价 ④ 2025-01-10 再跨年 → ["202501","202412","202411","202410"] ⑤ 格式 16/16 全部 `^\d{4}(0[1-9]|1[0-2])$`（YYYYMM 契约合规）⑥ Tab2 label「YYYY年MM月」格式一致】；**YYYYMM 契约零变更**【后端 TaxRecordDTO.java:17-19 taxPeriod String（注释「如202511(2025年11月)」）mtime 08-31 零改动 + calculate/generateReturn 的 period substring(0,4)/(4,6) 拆分逻辑 :327-328/:367 零触碰】；**硬编码 20260x 零残留**【grep 5 处命中全部为注释/说明 :5/:22/:50/:75/:726，无任何 el-option 硬编码选项，模板均为 v-for 动态生成】）；**筛选消费链端到端（Batch 2/3 教训，零死参数）**（**taxPeriod 5 环**：① 发送 :116 `taxPeriod: recordSearchForm.taxPeriod || undefined` → ② API 透传 tax-calculation.ts:95-97 getRecords → `get('/v1/finance/tax-records/page', params)` 原样透传零映射丢弃 → ③ Controller 绑定 TaxRecordController.java:44-53 @GetMapping("/page") + TaxRecordDTO（Spring MVC GET 参数绑定）→ ④ DTO 字段 TaxRecordDTO.java:19 → ⑤ Service eq TaxRecordServiceImpl.java:49-51（null/empty 检查 + queryWrapper.eq）→ Mapper TaxRecordMapper.java:11 BaseMapper + LambdaQueryWrapper → MyBatis-Plus SQL `tax_period = ?`——**无断点、无死参数**；**taxType** :115 → Service:44-46 同链；**taxStatus** :117 → Service:54-56 同链；**空值不发送**（`|| undefined` + request.ts:467-478 axios 序列化丢弃 undefined + Service null 检查跳过——空筛选 = 零参数发送，后端不误过滤）；Tab3 筛选 A 类有效保持 :466-478 → tax-rate.ts:54-59（mtime 08-11 零改动，本批零触碰））；**增强能力接入**（Tab1 分页封装 useStandardPage :86 defaultPageSize=100【与既有 pageSize:100 加载范围一致——统计卡片/待办摘要金额范围与改动前零变化】+ DataTable :pagination="recordPagination" :741-751 + @page-change="handleRecordPageChange" :222-224 → loadTaxRecords().then(updateStatsCards)【分页状态 total 更新 :123/:126】+ density="auto" :746 + show-summary :747-748 + createAmountSummaryMethod :213【useSummary.ts:44-59 summarizeAmount unit='yuan' 元→分→元同源格式化，页面零自实现格式化】；Tab2 density="auto" + show-summary :791-793 + declarationSummaryMethod :318（unit='yuan'，totalTaxPayable 金额列）；Tab3 CaliberNoteBar :831（口径标注条，设计 §3.4「税率配置 Tab 可加」）+ density="auto" :837 + 分页统一 core :838-839 :pagination="pagination" @page-change="handlePageChange" :519-521【原自定义 el-pagination 块已移除——grep el-pagination 元素零命中仅 :517 注释，handleSizeChange/handleCurrentChange 旧代码零残留，由 core DataTable 内部管理 :157-169，P1-UI-CORE-006】）；**缴税幂等 handlePayTax 零触碰（PD-001 实现）**（:237-268【:238 payingRowId 单飞守卫 → :241-248 ElMessageBox 二次确认 → :251-260 taxCalculationApi.payTax（既有端点，幂等键字段齐备：taxType/taxPeriod/taxAmount/taxableAmount/paidAmount/taxStatus:'PAID'/paymentDate/description）→ :261 成功提示 → :262-263 刷新+统计 → :264-265 错误提示 → :267 finally 释放】+ :759 缴税按钮仅 UNPAID/OVERDUE 行显示 + :loading="payingRowId === String(row.id)"——与 08-31 版及 sprint-2/3c QA 已验收实现一致）；**金额口径零变更**（formatAmount 元 toFixed(2) :167-170【NaN → '0.00'】+ 行内插槽 :752-753/:795 均走 formatAmount + 合计千分位 core useSummary）；**强制核验 6/6**（① 业务语义/接口/状态/金额口径零变更——接口调用集合与改动前一致 + 状态映射零新增零变更 ② 后端零改动——TaxRecord 链 5 文件 mtime 2026-08-31 01:20:28 + Mapper 08-11 + tax-calculation.ts 08-31/tax-rate.ts 08-11 实锤 ③ 视觉同源——core 组件 :30-42 + scoped 样式全 var(--fts-*) token 无自造色值 + V3-A 零命中 ④ 死参数零 + PD-028 未落库不悬空【待缴税项摘要基于真实 taxStatus 字段派生 + 「分页范围登记」标注 :649，零新增状态列/零 status 字段推断】+ P1-STOCK-001 零触碰【grep stock/库存 零命中】 ⑤ typecheck 独立复跑 = 136 条 error（= 存量基线 136=136，本批 3 文件零命中）+ build EXIT=0 ⑥ 开发者不自行宣布通过——本报告为唯一验收结论）；**独立页边界**（路由 /finance/tax 保留 router/index.ts:128【路径/组件/name 零变更，仅 meta.title「税务管理」→「合规工作台」】+ menu.ts:29 标题「合规工作台」【路径/scaleLevel 零变更】+ PageHeader :677「合规工作台」+ role-profiles.ts:268 / permissions.ts:734 为既有存量引用零触碰【本批修改文件仅 3 个，mtime 09-04 实锤】）；**登记链完整**（导出：EXPORT-001 盘点【finance-export-inventory-20260903.md:170 P1-FIN-EXPORT-004 13/13 页无导出入口】→ 任务池 §11.6 L3951-3953 后端池占位卡 → PD-035【product-decision-backlog.md:51】→ tooltip :707 → 按钮 disabled 不伪造；批量缴税：无批量端点【tax-calculation.ts:107-109 单条 payTax + TaxRecordController.java:120-126 仅 @DeleteMapping("/batch") batchDeleteTaxRecord 删除】→ disabled + tooltip :710-712，**不逐条循环调用既有端点**【grep FinanceTax.vue payTax 仅 handlePayTax 内 1 处调用 :251 + 注释 2 处，v-for/批量循环零命中，batchActions/勾选动作区零新增（无 el-table type="selection"）】）
- **测试目的**：防止合规工作台重构回归 —— 五区布局/工具栏三项处置（打印真实接入 + 导出/批量缴税登记不伪造）/ KL-059 税期动态化（当前月起往前 4 期、YYYYMM 契约零变更、跨年与硬编码等价、Tab2 同类修复、硬编码零残留）不得回退；筛选必须端到端有效（**F-1 教训防回归点：taxPeriod/taxType/taxStatus 全链零死参数——Batch 2/3 假筛选不得复发**）；增强能力接入（defaultPageSize=100 与既有范围一致 / density / useSummary unit='yuan' / CaliberNoteBar / 原自定义 el-pagination 移除）保持；缴税幂等 handlePayTax（PD-001）零触碰；金额口径零变更；后端零改动、PD-028/PD-035 BLOCKED 不占通过数、导出/批量缴税登记依赖非 FAIL。
- **测试步骤**：
  1. 五区布局核验：待办区（FinanceTax.vue:679-702，pendingTaxSummary :638-650 真实字段派生【UNPAID/OVERDUE 计数 + taxAmount 合计 + 逾期摘要，TaxRecordDTO.java:39 既有字段语义】+ recordLoaded 未加载「加载中…」不虚构 :639/:648 + 缴税幂等 PD-001 登记卡 :686-689 纯展示 + 统计卡片 4 张 :691-701）；工具栏 :704-713；筛选区三页签内保留（Tab1 :720-738 / Tab2 :767-783 / Tab3 :809-828）；数据区 el-tabs 3 页签 :717；详情区既有弹层 5 个（grep el-dialog 计数 = 5）
  2. 工具栏处置核验：申报表打印 handlePrint :655-657 = window.print 真实接入（grep 无伪造打印/Blob/下载代码）；导出 :707-709 disabled + tooltip 明示 EXPORT-001→P1-FIN-EXPORT-004 登记链（盘点 → 任务池 §11.6 → PD-035 → tooltip → disabled）；批量缴税 :710-712 disabled + tooltip 明示无批量端点；grep FinanceTax.vue payTax 仅 handlePayTax 内 1 处调用（:251），v-for/批量循环零命中，无 el-table selection（不逐条循环）
  3. KL-059 税期动态化核验：buildTaxPeriodOptions :54-66（TAX_PERIOD_COUNT=4 + new Date 循环 + padStart → YYYYMM）；taxPeriodOptions :70（Tab1 label=YYYYMM 与既有格式一致）/ declarationPeriodOptions :73（Tab2 label=YYYY年MM月）/ currentPeriod :76-80 + :277 默认期间=当前期；Node vm 沙箱行为实证 6/6（4 时点 × 4 期，含 2026-01 跨年 + 2026-06 与硬编码等价）；后端 YYYYMM 契约零变更（TaxRecordDTO.java:17-19 mtime 08-31 + substring 拆分 :327-328/:367 零触碰）；硬编码 20260x 零残留（grep 5 处全为注释/说明）
  4. 筛选端到端核验（Batch 2/3 F-1 教训）：taxPeriod 5 环（:116 发送 → tax-calculation.ts:95-97 透传 → TaxRecordController.java:44-53 绑定 → TaxRecordDTO.java:19 → TaxRecordServiceImpl.java:49-51 eq → Mapper SQL tax_period = ?）逐一读码；taxType :115→Service:44-46、taxStatus :117→Service:54-56 同链；空值 undefined 不发送（request.ts:467-478 + Service null 检查）——无断点、无死参数
  5. 增强能力核验：Tab1 useStandardPage defaultPageSize=100 :86（与既有 pageSize:100 一致，统计/待办金额范围零变化）+ DataTable pagination opt-in :741-751 + @page-change :222-224；density="auto" :746 + 合计 useSummary unit='yuan' :213（元→分→元同源，零自实现格式化）；Tab2 density + 合计 :791-793/:318；Tab3 CaliberNoteBar :831 + density :837 + 分页统一 core :838-839（原自定义 el-pagination 移除，grep el-pagination 元素零命中，handleSizeChange/handleCurrentChange 零残留）
  6. 缴税幂等核验：handlePayTax :237-268 零触碰（payingRowId 单飞守卫 :238 + ElMessageBox 二次确认 :241-248 + payTax 既有端点 :251-260 + 成功/错误提示 + finally 释放）；:759 缴税按钮仅 UNPAID/OVERDUE 行 + 行级 loading——与 08-31 版及 sprint-2/3c QA 已验收实现一致
  7. 独立页边界 + 零触碰核验：router/index.ts:128 /finance/tax 保留（路径/组件/name 零变更，仅 meta.title）；menu.ts:29 标题「合规工作台」（路径/scaleLevel 零变更）；role-profiles.ts:268 / permissions.ts:734 既有存量引用零触碰（本批修改文件仅 3 个，mtime 09-04 实锤）
  8. 强制核验独立复跑：业务语义/接口/状态/金额口径零变更（API 调用集合与改动前一致）；后端零改动（TaxRecord 链 5 文件 mtime 08-31 01:20:28 + Mapper 08-11 + API 层 08-31/08-11 实锤）；视觉同源（core 组件 + var(--fts-*) 全 token，V3-A 零命中）；PD-028 未落库不悬空（待缴税项摘要真实字段派生 + 分页范围登记 :649，零新增状态列）+ P1-STOCK-001 零触碰；`npx vue-tsc --noEmit` = 136 条 error（= 存量基线 136=136，本批 3 文件零命中）；`npm run build` EXIT=0（QA 独立执行）
- **预期结果**：
  - 五区布局齐备（待办区真实字段派生不虚构 + PD-001 登记卡 + 统计卡片保留 / 工具栏三项处置 / 筛选区 / 数据区 3 页签 / 详情区既有弹层零改动）
  - 工具栏处置保持：申报表打印 window.print 真实接入；导出/批量缴税 disabled + tooltip 明示登记依赖（登记非 FAIL、不伪造动作、不逐条循环）
  - KL-059 税期动态化保持：当前月起往前 4 期含当期、YYYYMM 契约零变更、跨年正确、与硬编码行为等价、Tab2 同类修复、硬编码 20260x 零残留、Tab2 默认期间=当前期
  - 筛选端到端有效：taxPeriod/taxType/taxStatus 全链零死参数（F-1 教训防回归点，假筛选不得复活）；空值零参数发送
  - 增强能力保持：defaultPageSize=100 与既有范围一致 / density / useSummary unit='yuan' / CaliberNoteBar / 原自定义 el-pagination 移除统一 core
  - 缴税幂等 handlePayTax（PD-001）零触碰；金额口径零变更；后端零改动；PD-028/PD-035 BLOCKED 不占通过数；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + Node vm 行为实证 + 构建独立复跑）
- **当前状态**：PASS（2026-09-04）
- **RVT 回退标注（2026-09-04，只标注不删除——任务板 §12.4 方案）**：本条目菜单标题断言「合规工作台」已过时——回退后现状：FinanceTax PageHeader/menu.ts/router meta.title 三处恢复「税务管理」= HEAD 原命名（`docs/quality/ui-rvt-b1-qa-report.md` §2.3）；标题断言由 **REG-FIN-RVT-001/004** 接管；本条目其余断言（KL-059 税期动态化/筛选消费链端到端/工具栏处置/增强能力/缴税幂等 handlePayTax 零触碰）回退后仍有效，由 REG-FIN-RVT-004 保留项断言衔接，无冲突。
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-028「待核销」子项（BLOCKED，不占通过数）**：fund_flows.status 未落库（与 Batch0-方案2 同批落库）——本批待缴税项摘要基于 Tab1 真实 taxStatus 字段（UNPAID/OVERDUE/PAID）派生 + 「分页范围登记」标注（FinanceTax.vue:649），零新增状态列、零 status 字段推断（不发明「待核销」类语义）；FundFlowStatusMap 悬空声明维持登记链（B01 承接）；本卡未借机实现 PD-028 任何内容
  - **PD-035（BLOCKED，不占通过数）**：导出规则缺失（文件格式/行数上限/导出口径未定义）——导出按钮 disabled + tooltip 明示 EXPORT-001 盘点结论（tax 域无导出端点 → 登记后端池 P1-FIN-EXPORT-004），不伪造导出
  - **PD-001 已决策落地说明（非 BLOCKED，保持零触碰）**：缴税幂等 PD-001 已于 2026-08-11 决策落地（REG-FIN-005 在案）——本批 handlePayTax（:237-268）零触碰（单飞守卫 + 二次确认 + 既有端点全部保留，与 08-31 版及 sprint-2/3c QA 已验收实现一致）；待办区缴税幂等登记卡 :686-689 纯展示说明幂等键，无动作逻辑
  - **导出 = 登记依赖非 FAIL（不占通过数）**：EXPORT-001 盘点结论（税务记录/申报表/税率配置均无导出端点，13/13 页无导出入口）→ 后端池 P1-FIN-EXPORT-004（任务池 §11.6 L3951-3953）+ PD-035 决策待决；按钮 disabled + tooltip 明示（:707-709），grep 无任何导出/CSV/Blob/download 代码（不伪造）
  - **批量缴税 = 登记依赖非 FAIL（不占通过数）**：无批量缴税端点（tax-calculation.ts:107-109 仅单条 payTax / TaxRecordController.java:120-126 仅 batchDelete 删除）→ 按钮 disabled + tooltip 明示（:710-712），**不逐条循环调用既有端点**（超展示层红线）；grep payTax 仅 handlePayTax 内 1 处调用（:251）+ 注释 2 处，v-for/批量循环零命中，无 el-table type="selection"；后端批量端点落地并 QA PASS 后反向接入（届时登记，不预固化）
  - **KL-059 动态化实现说明**：KL-059 为 E 类修复（税期选项超期失效）本批实现闭环——动态生成 + Node 行为实证 6/6（跨年 + 与硬编码等价）+ YYYYMM 契约零变更；非 BLOCKED 业务规则猜测（前端生成选项不改变后端契约，任务卡已决口径）；Tab2 申报期间同类风险（原 :636-640 硬编码 202606）同源函数修复
  - 观察 O-WS-B4-1（QA 报告 §八，登记不判 FAIL）：统计卡片 4 张在**初始加载完成后**仍显示 ¥0.00/0 笔，需用户首次交互（查询/翻页/缴税/切页签）才刷新——onMounted（:666-672）同步调用 updateStatsCards（:669）先于异步 loadTaxRecords 完成（未链 .then）；**与 08-31 版本同构非本批引入**（git diff onMounted hunk 仅 `+loadTaxRecords()` 一行，源自 08-31 sprint-4 批次，本批证据未声明 onMounted 变更）；待办区 pendingTaxText 为 computed 响应式（:638-650）即时更新**无此问题**；统计卡显示值恒为真实派生（当前页数据），非虚构；如需改进（onMounted 链 updateStatsCards）由后续批次/登记评估
  - 观察 O-WS-B4-2（QA 报告 §八，登记不判 FAIL）：工作区多批次累计未提交（git status 大量 M，最近提交 cd1c89b 为 OIC-BE 批次），无法 git diff 精确隔离本批变更——验证方法限制（同 O-WS-B2-4 / O-WS-B3-4 口径）；已用「developer 证据行号 + 代码现状交叉核验 + mtime 边界（后端全部 08-11/08-31 早于本批 09-04）+ 变更段性质审查 + grep 零残留」多重替代核验，证据充分
  - 观察 O-WS-B4-3（QA 报告 §八，登记不判 FAIL）：role-profiles.ts:268 / permissions.ts:734 标题相关文案仍为「税务管理」（未随菜单/路由/PageHeader 变更为「合规工作台」）——本批卡面明确声明零触碰不扩大范围（同 Batch 1 O-WS-5 型观察：标题文案不扩散）；登记不判 FAIL（卡面边界）；如产品要求全站文案一致，另立任务

---

### PD-028/PD-035 限制登记同步（Batch WS-4，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §11.3 P1-FIN-WS-004 L3852【批量缴税登记：无批量端点 → disabled + tooltip 明示 + 不逐条循环】/ L3853【导出登记：EXPORT-001 + 后端池 P1-FIN-EXPORT-004 + PD-035 待决】/ L3854【Tab2 申报期间同类动态化说明】/ L3856【待办摘要分页范围登记】）→ 决策池（`product-decision-backlog.md:44/155` PD-028 2026-08-31 登记；`:51` PD-035 导出规则登记）→ roadmap（`remediation-roadmap.md` §5.12：后端缺口仅登记不混入 + Batch 4 导出按钮 = disabled + tooltip 明示依赖）→ QA 报告（`docs/quality/ui-ws-b4-qa-report.md` §〇/§二/§七/§八）——登记链完整一致。
- **内容**：① PD-028——资金流水「待核销」状态语义缺失，`fund_flows.status` 与 Batch0-方案2 同批落库；落库前前端不实现、不悬空、不伪造（本批待缴税项摘要基于真实 taxStatus 字段派生，非状态列发明）。② PD-035——导出规则缺失（文件格式/行数上限/导出口径未定义）；导出按钮 disabled + tooltip 明示 EXPORT-001 盘点结论（tax 域无导出端点 → 后端池 P1-FIN-EXPORT-004 登记）。
- **本批执行确认（QA 实锤）**：待缴税项摘要基于 Tab1 真实 taxStatus 字段（UNPAID/OVERDUE/PAID）派生 + 「分页范围登记」标注（:649）+ recordLoaded 未加载「加载中…」不虚构（:639/:648）；零新增状态列/零 status 推断；缴税幂等 PD-001 登记卡纯展示零动作逻辑；导出 grep CSV/Blob/download 零命中（无伪造导出）；批量缴税 grep payTax 仅 handlePayTax 内 1 处调用 + v-for/批量循环零命中 + 无 el-table selection（不逐条循环）；P1-STOCK-001 零触碰（grep stock/库存 零命中）；后端零改动（mtime 实锤）。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-FIN-WS-004）**不受影响**——PD-028/PD-035 相关均为**登记项非 FAIL**（待办摘要任务语义 + disabled 登记态）；导出/批量缴税为**登记依赖非 FAIL**（后端池 P1-FIN-EXPORT-004 排期 + 无批量端点登记，按钮 disabled + tooltip 明示，不伪造、不逐条循环，后端就绪并 QA PASS 后反向接入并扩展断言，届时登记不预固化）；勾稽状态列由 B01 占位卡承接（Batch0-方案2 同批落库后实现）；PD-001 为已决策落地（REG-FIN-005）保持零触碰，不构成影响。

---

### QA 观察项登记（Batch WS-4，如实登记不销号、不猜测处置）

按 QA 报告 §八 观察项（O-WS-B4-1~3）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-WS-B4-1**：统计卡片 4 张在初始加载完成后仍显示 ¥0.00/0 笔，需用户首次交互（查询/翻页/缴税/切页签）才刷新——onMounted（FinanceTax.vue:666-672）同步调用 updateStatsCards（:669）先于异步 loadTaxRecords 完成（未链 .then）；**与 08-31 版本同构非本批引入**（git diff onMounted hunk 仅 `+loadTaxRecords()` 一行，源自 08-31 sprint-4 批次，本批证据未声明 onMounted 变更）；待办区 pendingTaxText 为 computed 响应式（:638-650）即时更新**无此问题**；统计卡显示值恒为真实派生（当前页数据），非虚构；如需改进（onMounted 链 updateStatsCards）由后续批次/登记评估（同 REG-FIN-WS-004 观察 1）。
2. **O-WS-B4-2**：工作区多批次累计未提交（git status 大量 M，最近提交 cd1c89b 为 OIC-BE 批次），无法 git diff 精确隔离本批变更——验证方法限制（同 O-WS-B2-4 / O-WS-B3-4 口径）；已用「developer 证据行号 + 代码现状交叉核验 + mtime 边界（后端全部 08-11/08-31 早于本批 09-04）+ 变更段性质审查 + grep 零残留」多重替代核验，证据充分（同 REG-FIN-WS-004 观察 2）。
3. **O-WS-B4-3**：role-profiles.ts:268 / permissions.ts:734 标题相关文案仍为「税务管理」（未随菜单/路由/PageHeader 变更为「合规工作台」）——本批卡面明确声明零触碰不扩大范围（同 Batch 1 O-WS-5 型观察：标题文案不扩散）；登记不判 FAIL（卡面边界）；如产品要求全站文案一致，另立任务（同 REG-FIN-WS-004 观察 3）。

---

### Batch WS-4 门禁

```text
Regression Result: PASS（2026-09-04 Batch WS-4 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为财务模块任务型重构·阶段三 Batch 4 合规工作台，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：1/1 卡 QA 独立验收 PASS（`docs/quality/ui-ws-b4-qa-report.md`，2026-09-04：P1-FIN-WS-004 合规工作台，验收标准 8 条全达成 + 强制核验 6/6 全过，无 FAIL、无 -R）
- FAIL 明细：0
- PWL 明细：0（PD-028/PD-035 为 BLOCKED，不占通过数——fund_flows.status 未落库待缴税项摘要真实字段派生非状态列发明 / 导出规则待决；PD-001 为已决策落地（REG-FIN-005，2026-08-11）本批 handlePayTax 零触碰保持（非 BLOCKED）；导出/批量缴税为登记依赖非 FAIL——EXPORT-001 结论 + P1-FIN-EXPORT-004 后端池 + 无批量端点，按钮 disabled+tooltip 明示，不伪造、不逐条循环）
- 基线：新增 REG-FIN-WS-004（正式 90 → 91 条，只增不减、既有条目零修改）
- 观察项：O-WS-B4-1~3 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch WS-5 回归（财务模块任务型重构·阶段三 Batch 5 毛利核算 + 决策工作台，2026-09-04）

> 验收依据：首验 `docs/quality/ui-ws-b5-qa-report.md`（2026-09-04，QA 独立验收：P1-FIN-WS-005 毛利核算 = **PASS**（无 FAIL 点、无 -R；验收标准 8 条全达成 + 强制核验 6/6 全过）；P1-FIN-WS-006 决策工作台 = **FAIL（F-1）**——cost_structure 统计卡片金额口径错误（缩小 100 倍）→ 生成 P1-FIN-WS-006-R1）+ 复验 `docs/quality/ui-ws-b5-r1-qa-report.md`（2026-09-04，P1-FIN-WS-006-R1 = **PASS**，F-1 闭环）；对照基准 = 任务板 §11.3 P1-FIN-WS-005 细化卡（验收标准 8 条 L3877-3885 + developer 修改证据 L3887-3916）/ P1-FIN-WS-006 细化卡（验收标准 8 条 L3937-3945 + developer 修改证据 L3947-3980 + R1 修改证据 L3982-3996）+ 设计 FIN-WB-DESIGN-V1.1（`docs/design/finance-task-workbench-design-20260903.md` §3.5/§3.6 + §4 D-1 + §8）+ 诊断（`docs/quality/finance-module-diagnosis-20260903.md` §1.8/§1.9）+ KL-057（`production-known-limitations.md:754-761`）+ PD-032（`product-decision-backlog.md:48`）+ 导出盘点（`docs/quality/finance-export-inventory-20260903.md` P1-FIN-EXPORT-004）+ Batch 2/3 F-1 教训（消费链端到端核验纪律）+ roadmap §5.12 硬约束；开发者未自行宣布通过 ✅（任务板 L3916/L3980/L3996）——QA 报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-WS-005（P1-FIN-WS-005 首验 PASS）与 REG-FIN-WS-006（P1-FIN-WS-006 首验 FAIL → P1-FIN-WS-006-R1 复验 PASS，F-1 闭环）——维护规则 5 禁止推测，无推测内容；WS-006 在 R1 复验 PASS 前不转基线（禁止预固化）。
> 批次口径：本批为**治理批次**（财务模块任务型重构·阶段三 Batch 5 毛利核算 + 决策工作台，任务池 §11.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch 6（§11.3 批次节奏，禁止跳步）。
> 基线计数核对：追加前正式基线 **91 条**（文件实况：基线总览表 91 行，含 REG-FIN-WS-001/002/003/004；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 2 条（REG-FIN-WS-005/006）后 **93 条**，只增不减纪律达成（既有 91 条条目零修改）；候选 0 条维持。

### REG-FIN-WS-005

- **测试编号**：REG-FIN-WS-005
- **模块**：财务-毛利核算（frontend/src/views/finance/FinanceCost.vue 重构 + api/finance/cost.ts + modules/finance/menu.ts + router/index.ts；后端对照 CostController.java / CostServiceImpl.java / CostRecordQueryDTO.java / CostRecordCreateDTO.java【只读确认，零改动——6 文件 mtime 全 2026-08-11，git status 均不在 M 列表】）
- **关联任务**：P1-FIN-WS-005（Batch WS-5；QA 独立验收 PASS 2026-09-04 `docs/quality/ui-ws-b5-qa-report.md`）
- **业务场景**：毛利核算工作台（FinanceCost 重构，范围 = 五区布局 + B-5 月期间筛选修复 + 成本结构分析对接 + 工具栏处置 + 增强能力零回归）——**五区布局**（① 统计区 4 张卡片保留 :86-100 statsCards【本月总成本/食材成本率/人工成本率/成本偏差，基于当前页真实数据】+ 模板 :279-281，**计算口径与 git 旧版逐字一致**【git show HEAD 旧版 statsCards 逻辑 totalCost/materialCost/laborCost/totalVariance/materialRate/laborRate 零变更】/ ② 工具栏 :283-292【成本结构分析 = 真实按钮 openAnalysis :285；导出 :286-288 disabled+tooltip 明示「P1-FIN-EXPORT-001 盘点：成本记录无导出端点（需新增并登记后端池 P1-FIN-EXPORT-004）→ 登记，不伪造导出」；导入 :289-291 disabled+tooltip 明示「CostController 仅 create/update/getDetail/getPage/summarizeByPeriod，无 import 端点 → 登记，不伪造导入」】/ ③ 筛选区 :297-325【costType el-select :300-307 A 类有效保持 + **期间 monthrange + value-format="YYYY-MM"** :308-317】/ ④ 数据区 :327-384【CaliberNoteBar :329 + EmptyState error/no-data :330-345 + DataTable :346-383（density/expandable/show-summary/pagination）】/ ⑤ 详情区 :386-403 既有详情弹层 + CostFormDialog（零改动）+ 成本结构分析视图弹层 :405-448）；**B-5 月期间筛选端到端 6 环零死参数（验收标准 2 重点）**（① searchForm.dateRange :50 + monthrange + value-format YYYY-MM :308-317 → 值恒为 "2026-01" 月期间字符串（Date 对象问题消除，诊断 §1.9:248 根因=值格式）→ ② loadData params :125-128 startDate/endDate 仅字符串非空时发送（空值 undefined 不发送，无死参数）→ ③ cost.ts mapQueryParams :41-42 startDate→startPeriod / endDate→endPeriod（rest 透传为空，params 仅 5 键零死参数）→ ④ CostRecordQueryDTO.java:26-30 startPeriod/endPeriod String「起始期间/结束期间」→ ⑤ CostServiceImpl.getPage:63-68 wrapper.ge(period, startPeriod).le(period, endPeriod) → MyBatis-Plus LambdaQueryWrapper → Mapper SQL period ge/le → ⑥ **period 格式一致性**：CostRecordCreateDTO.java:31 @Pattern `^\d{4}-(0[1-9]|1[0-2])$` 强制 YYYY-MM 带连字符 + FinancialReportServiceImpl.sumCostRecords:266-267 按 "yyyy-MM" 比较 → 全系统 period 语义 = "2026-04" 与 monthrange 输出字典序 ge/le 语义一致——**全链有效，非死参数（Batch 2/3 F-1 教训零复发）**）；**summarizeByPeriod 真实接入（验收标准 3）**（cost.ts:112-131 补齐既有端点 GET /v1/finance/costs/summary?period={YYYY-MM}，返回 Map<类型,金额分>；FinanceCost.vue:231-234 openAnalysis → :200-228 loadCostSummary（期间选择 month value-format YYYY-MM :409-416 → :208 调用 → :211 CostTypeMap.toFrontend 类型标签（食材/人工/租金/能耗/营销/其他）→ :215 fenToYuanNumber 分→元 → :197 analysisSummaryMethod useSummary unit='yuan' 合计）；后端既有：CostController.java:59-64 GET /summary → CostServiceImpl.java:91-101 eq(period) 按 costType merge 聚合——**非新增接口（mtime 实锤：CostController/CostServiceImpl 2026-08-11 零改动）**）；**PD-032 口径标注（验收标准 5）**（FinanceCost.vue:329 CaliberNoteBar + CaliberNoteBar.vue:28 tooltip =「PD-032：计价方法 = 最新入库价为当前口径；移动加权平均 = Level 3 演进，不迁移历史数据」——与 product-decision-backlog.md:48 决策原文**逐字一致**，:31「管理口径」标签）；**失败透传 + 引导空态**（:132-137 catch：rawRecords 清空 + loadFailed + console.error + ElMessage.error + EmptyState error 重试 :330-337 → handleRetry :144-146；:338-344 EmptyState no-data hasActiveFilter :60 区分「未找到匹配成本记录/调整筛选条件」vs「暂无成本记录/新增成本」；成本结构分析视图失败透传 :220-224 + :419-426）；**增强能力零回归（验收标准 6）**（密度 density="auto" :352 / 合计 createAmountSummaryMethod unit='yuan' :113 + :354-355【useSummary.ts:44-59 同源，零自实现格式化】/ 分页 useStandardPage defaultPageSize=20 :40-45 与旧版无参默认一致（git 对照实锤）+ :356-357 pagination opt-in + @page-change :153-155 / expand :353 + :370-382 行内详情与既有弹层同字段 / 详情弹层 + CostFormDialog 零改动【git status 无 CostFormDialog.vue】）；**菜单标题「成本管理」→「毛利核算」（路径零变更）**（menu.ts:27 + router/index.ts:125 meta.title + PageHeader :270，路径/scaleLevel 零变更；role-profiles.ts/permissions.ts 零触碰）；**视觉同源**（scoped 样式仅 var(--fts-*) 全 token，grep #hex 零命中、V3-A 零引用、el-pagination 元素零命中仅注释）；**构建/纪律**（typecheck 136=136 本批 5 文件零命中 + build EXIT=0 独立复跑；开发者未自行宣布通过）
- **测试目的**：防止毛利核算重构回归 —— B-5 月期间筛选必须端到端有效（**F-1 教训防回归点：6 环全链零死参数，Batch 2/3 假筛选不得复发**）；summarizeByPeriod 真实接入保持（非新增接口、非假接入）；统计卡片 4 张口径与旧版一致（金额口径零变更）；导出/导入登记不伪造（disabled+tooltip，P1-FIN-EXPORT-004 后端池 + 无 import 端点）；PD-032 口径标注保持逐字一致；失败透传 + 引导空态保持；增强能力零回归；菜单标题路径零变更；后端零改动。
- **测试步骤**：
  1. 五区布局核验：统计区（FinanceCost.vue:86-100 + :279-281，与 git 旧版 statsCards 逻辑逐字对照）/ 工具栏 :283-292 / 筛选区 :297-325 / 数据区 :327-384 / 详情区 :386-403 + 成本结构分析视图弹层 :405-448
  2. B-5 月期间筛选 6 环端到端核验：monthrange+value-format YYYY-MM :308-317 → loadData :125-128（空值 undefined 不发送）→ cost.ts mapQueryParams :41-42（rest 透传为空零死参数）→ CostRecordQueryDTO.java:26-30 → CostServiceImpl.getPage:63-68 ge/le(period) → Mapper SQL；period 格式由 CostRecordCreateDTO.java:31 @Pattern YYYY-MM 强制（与 monthrange 输出字典序比较语义一致）
  3. summarizeByPeriod 真实接入核验：cost.ts:112-131（GET /v1/finance/costs/summary 既有端点）+ CostController.java:59-64 + CostServiceImpl.java:91-101 eq(period) 聚合；后端 mtime 2026-08-11 零改动实锤（非新增接口）
  4. 工具栏处置核验：成本结构分析 = 真实按钮（openAnalysis :285 → loadCostSummary :200-228：期间选择 YYYY-MM :409-416 + CostTypeMap.toFrontend 标签 :211 + fenToYuanNumber :215 + useSummary 合计 :197）；导出 :286-288 / 导入 :289-291 disabled + tooltip 明示登记链（EXPORT-001 盘点 → 后端池 P1-FIN-EXPORT-004 / CostController 5 端点无 import）；grep 无 CSV/Blob/download 代码
  5. PD-032 口径标注核验：FinanceCost.vue:329 CaliberNoteBar + tooltip 与 product-decision-backlog.md:48 原文逐字比对（「管理口径」标签）
  6. 失败透传/空态核验：loadData catch :132-137（loadFailed + ElMessage.error + EmptyState error 重试 :330-337）；引导空态 :338-344（hasActiveFilter :60 区分）；成本结构分析失败透传 :220-224 + :419-426
  7. 增强能力零回归核验：密度 :352 / 合计 unit='yuan' :113,:354-355 / 分页 defaultPageSize=20 :40-45（git 旧版对照实锤）/ expand :353,:370-382 / 详情弹层与 CostFormDialog 零改动（git status）
  8. 强制核验独立复跑：菜单标题「毛利核算」三处（menu.ts:27 / router/index.ts:125 / PageHeader :270，路径零变更）；视觉同源（var(--fts-*) 全 token、V3-A 零引用、#hex 零命中、el-pagination 元素零命中）；typecheck 136=136（本批 5 文件零命中）+ build EXIT=0（QA 独立执行）
- **预期结果**：
  - 五区布局齐备 + 统计卡片 4 张保留且口径与旧版一致（金额口径零变更）
  - B-5 月期间筛选端到端有效：6 环全链零死参数（F-1 教训防回归点，假筛选不得复活）；空值零参数发送；period 格式契约 YYYY-MM 全系统一致
  - summarizeByPeriod 真实接入保持（既有端点非新增，分析视图类型标签 + 分→元 + useSummary 合计正确）
  - 工具栏处置保持：成本结构分析真实；导出/导入 disabled + tooltip 明示登记依赖（登记非 FAIL、不伪造）
  - PD-032 口径标注与决策池原文逐字一致（「管理口径」标签保持）
  - 失败透传 + 引导空态保持；增强能力零回归；菜单标题路径零变更；后端零改动；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + git 旧版对照 + 构建独立复跑）
- **当前状态**：PASS（2026-09-04）
- **RVT 回退标注（2026-09-04，只标注不删除——任务板 §12.4 方案）**：本条目菜单标题断言「毛利核算」已过时——回退后现状：FinanceCost PageHeader/menu.ts/router meta.title 三处恢复「成本管理」= HEAD 原命名（`docs/quality/ui-rvt-b1-qa-report.md` §2.3）；标题断言由 **REG-FIN-RVT-001/004** 接管；本条目其余断言（B-5 月期间筛选 6 环端到端/summarizeByPeriod 真实接入/PD-032 口径标注/统计卡片口径/导出导入登记）回退后仍有效，由 REG-FIN-RVT-004 保留项断言衔接，无冲突。
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **导出 = 登记依赖非 FAIL（不占通过数）**：EXPORT-001 盘点结论（成本记录无导出端点）→ 后端池 P1-FIN-EXPORT-004（任务池 §11.6）+ PD-035 导出规则待产品决策；按钮 disabled + tooltip 明示（:286-288），grep 无任何导出/CSV/Blob/download 代码（不伪造）
  - **导入 = 登记依赖非 FAIL（不占通过数）**：CostController 全 65 行 5 端点（create/update/getDetail/getPage/summarizeByPeriod）确认无 import 端点 → 按钮 disabled + tooltip 明示（:289-291），登记不伪造；后端导入端点落地并 QA PASS 后反向接入（届时登记，不预固化）
  - **成本结构分析期间口径登记**：summarizeByPeriod 按单期间（YYYY-MM）查询——分析视图期间 = 用户选择月份（默认当前月），与主表筛选区间独立（端点契约限制，非新增接口）
  - 观察 O-WS-B5-2（QA 报告 §六，登记不判 FAIL）：summarizeByPeriod 后端 DTO 注释成本类型 1~7（含折旧/包装），CostTypeMap 仅 1~6（既有契约）——前端 `CostTypeMap.toFrontend[Number(key)] || key` 兜底为「类型7」标签展示，不崩溃不伪造；标签语义由既有 6 类型契约承载（诊断 §1.9 A 类 costType 既有契约）

### REG-FIN-WS-006

- **测试编号**：REG-FIN-WS-006
- **模块**：财务-决策工作台（frontend/src/views/finance/FinanceReport.vue 重构 + types/finance.ts + modules/finance/menu.ts + router/index.ts；后端对照 ReportController.java / FinancialReportServiceImpl.java【只读确认，零改动——6 文件 mtime 全 2026-08-11，git status 均不在 M 列表】）
- **关联任务**：P1-FIN-WS-006（Batch WS-5；首验 FAIL F-1 → **P1-FIN-WS-006-R1 复验 PASS 闭环** 2026-09-04，`docs/quality/ui-ws-b5-qa-report.md` + `docs/quality/ui-ws-b5-r1-qa-report.md`）
- **业务场景**：决策工作台（FinanceReport 重构，范围 = 9 类报表聚合 + 统计卡片仅真实数据 + 利润表契约对齐 + 报表→明细穿透 + P0 断流处置 + 后端占位登记 + 工具栏处置 + 金额口径统一【R1】）——**9 类报表聚合（验收标准 1/2）**（reportTypeOptions :70-80：profit/income_expense/receivable/payable/aging/cost_structure/budget 7 个既有端点 + balance/cash_flow 2 个 P0 断流；types/finance.ts:1274-1283 FinanceReportType 9 值；报表类型切换 :467-469 el-select @change → loadData :398-400；查询区 :470 monthrange + value-format="YYYY-MM" 字符串化加固【原无 value-format → 重选后 Date 对象风险消除，诊断 §1.8:220 A 类有效保持】+ 后端 yyyy-MM-dd 由 buildProfitParams :296-315 正则补全【startDate→-01、endDate→月末】）；**统计卡片仅真实数据展示**（:204-287 statsCards computed，reportData 为 null 时返回 []）+ 模板 :453-455 v-if="statsCards.length"——加载前/失败/P0/占位均不显示；**git 旧版对照实锤**：旧版 statsCards 为 ref 硬编码 `{ value: '¥0.00' }` 虚构零值卡片（重构前 :138-167 区域）→ 本批消除）；**利润表契约对齐（验收标准 2）**（视图 :109-119 profit 分支消费 revenueAmount/cogsAmount/grossProfit/operatingExpenses/netProfit ↔ types/finance.ts:1305-1320 ProfitStatementData 5 字段 + startDate/endDate 可选 ↔ 后端 FinancialReportServiceImpl.java:62-66 真实返回 5 键——**三方一致，断链修复**【旧版消费 d.operatingIncome 后端不返回 + details[].currentPeriod/previousPeriod 后端无 details → 本批按真实契约修复，金额单位分 → fenToYuanNumber/formatFenToYuan 转元】；「上期金额/同比变动」后端不返回 → 不伪造列（登记））；**报表→明细穿透（验收标准 4）**（利润表-营业成本 :114 drillTo /finance/cost；成本结构行 :159 /finance/cost；应收统计/账龄行 :128-133,:150 /finance/reconciliation?tab=receivable【FinanceReconciliation.vue:30 route.query.tab 消费链已支持，WS-002 验收口径】；应付统计行 :137-142 ?tab=payable；预算执行行 :165-168 /finance/budget；**不可行登记**：利润表-营业收入 :113 / 运营费用 :116 / 收支汇总 :120-125 无 drillTo → 模板 :517-520 行内「—」（FinanceRecord 无页面承载，record.ts 零页面消费）登记不伪造）；**P0 断流处置（验收标准 3，KL-057）**（balance/cash_flow 分支 :366-371 调用既有 reportApi.getBalanceSheet/getCashFlowStatement【report.ts:70-92 端点声明保持，mtime 2026-08-11 零变更 = API 端点集合零变更】→ 404 透传 → catch p0Broken :375-386 + console.error（「断流报表请求失败（KL-057，已登记 P1-FIN-BE-001）」）→ 空态说明 :479-488 EmptyState error「后端端点缺失」+ description「资产负债表/现金流量表端点后端未提供（KL-057，已登记 P1-FIN-BE-001 后端池排期），不显示空数据、不伪造——端点落地后本报表自动可用」+ 重试；P0 类型不重复 ElMessage（请求层已透传）；**仅登记不混入**：P1-FIN-BE-001 任务池 §11.5 BLOCKED 后端池排期保持 + ReportController.java:30-83 仅 7 端点无 balance-sheet/cash-flow 事实确认）；**后端占位登记（验收标准 3 延伸）**（账龄分析：FinancialReportServiceImpl.java:156-176 getAgingAnalysis 恒 0 计数/金额【:169-171 注释「这里简化处理」后端事实独立确认】→ :349-356 全零检测 → placeholderNote「后端为占位实现…不展示虚构数值，已登记后端池」→ 空态 :489-496；预算执行：:220-230 getBudgetExecution 返回「预算执行数据待实现」+ budgetAmount/actualAmount/variance 恒 0 → :360-365 message 含「待实现」检测 → placeholderNote → 空态）；**工具栏（验收标准 5）**（打印 :459 → :420-422 handlePrint = window.print 真实接入【纯浏览器能力零接口依赖】；导出 :460-462 disabled + tooltip 明示「P1-FIN-EXPORT-001 盘点：报表页无导出端点（需新增并登记后端池 P1-FIN-EXPORT-004）→ 登记，不伪造导出」，grep 无 CSV/Blob/download）；**cost_structure 统计卡片金额口径统一【R1 闭环，F-1 防回归点】**（首验 F-1：:272-274 将已转「元」数值 material/labor/total 传入 formatFenToYuan 二次 /100 → 3/4 卡片缩小 100 倍（实证 totalCost=100000 分 → 总成本 ¥1,000.00 但食材成本 ¥6.00 应为 ¥600.00），违反验收标准 1/2 + money.ts 强制规范；**R1 修复**：FinanceReport.vue:261-279 四卡统一分→元**一次**转换——:274 `formatFenToYuan(totalFen)` / :275 `formatFenToYuan(totalFen > 0 ? materialFen : 0)` / :276 `formatFenToYuan(laborFen)` / :277 `formatFenToYuan(totalFen - materialFen - laborFen)`，typeAmountFen :266-269 直接取分（无 fenToYuanNumber）、totalFen :270 = fenOf 原始分；全文件 grep formatFenToYuan 32 处使用点无元值误传、fenToYuanNumber 22 处消费无未用导入；**Node 独立复算**（同输入）：¥1,000.00 / ¥600.00 / ¥200.00 / ¥200.00 与 QA 应值 4/4 一致 + 分口径自洽断言（总=食材+人工+其他）EXIT=0；gridRows cost_structure 分支 :153-161 本就一次分→元零改动、其他 6 类报表卡片（profit/income_expense/receivable/payable/aging/budget）均 formatFenToYuan(分) 一次转换零改动、report.ts 零改动（git status 空输出 + mtime 08-11）、types/finance.ts R1 零改动（mtime 00:54 早于 R1 窗口 01:06）、后端 6 文件零改动（mtime 08-11 实锤））；**增强能力 + 口径标注条（验收标准 6/7）**（密度 :509 density="auto" / 分页 :50 useStandardPage defaultPageSize=20 与旧版一致 + :512-513 pagination opt-in + :414-417 handleLocalPageChange 本地切片 :290-293 与旧版 slice 行为一致 / 固定列 :181/:188/:194 项目列 fixed left / 合计 :510-511 仅 cost_structure 显示 costSummaryMethod :201 unit='yuan' / CaliberNoteBar :478 金额分/元标注，与 PD-031/032 决策原文逐字一致 / 失败透传 :375-386 空态重试 :479-488）；**菜单标题「财务报表」→「决策工作台」（路径零变更）**（menu.ts:33 + router/index.ts:129 + PageHeader :450，路径/组件/scaleLevel 零变更；role-profiles.ts/permissions.ts 零触碰）；**构建/纪律**（typecheck 136=136 本批文件零命中 + build EXIT=0 独立复跑；开发者未自行宣布通过）
- **测试目的**：防止决策工作台重构回归 —— cost_structure 统计卡片金额口径必须保持统一（**F-1 防回归点：四卡分→元一次转换，禁止二次 /100**）；统计卡片虚构零值不得复活（仅真实数据展示）；利润表契约对齐保持（operatingIncome/details 断链不得复发）；P0 断流（KL-057/P1-FIN-BE-001）仅登记不伪造（空态说明保持）；后端占位（账龄恒 0/预算待实现）空态检测保持；报表→明细穿透可行跳转/不可行「—」保持；打印真实/导出登记不伪造；菜单标题路径零变更；后端零改动。
- **测试步骤**：
  1. 9 类报表聚合核验：reportTypeOptions :70-80（7 既有端点 + 2 P0 断流）；types/finance.ts:1274-1283 FinanceReportType 9 值；切换 :467-469 → loadData :398-400；查询区 :470 monthrange + value-format YYYY-MM（git 旧版对照：旧版无 value-format 实锤）+ buildProfitParams :296-315 正则补全
  2. 统计卡片仅真实数据核验：statsCards :204-287（reportData null 返回 []）+ 模板 :453-455 v-if；git 旧版对照：旧版 ref 硬编码 ¥0.00 虚构零值卡片消除
  3. 利润表契约对齐核验：:109-119 ↔ types/finance.ts:1305-1320 ↔ FinancialReportServiceImpl.java:62-66 三方一致（5 键）；git 旧版对照：operatingIncome/details 断链修复；「上期金额/同比变动」不伪造列（登记）
  4. P0 断流处置核验：balance/cash_flow :366-371 调用既有 reportApi 端点（report.ts 零变更 mtime 08-11）→ catch p0Broken :375-386 → 空态说明 :479-488（KL-057/P1-FIN-BE-001 仅登记不混入）；P1-FIN-BE-001 任务池 §11.5 BLOCKED 保持
  5. 后端占位登记核验：账龄恒 0（FinancialReportServiceImpl.java:156-176，:169-171 注释「这里简化处理」后端事实独立确认）→ 全零检测 :349-356 → placeholderNote 空态 :489-496；预算待实现（:220-230「待实现」）→ :360-365 检测 → 空态
  6. 穿透核验：利润表-营业成本 :114 / 成本结构 :159 → /finance/cost；应收/账龄 :128-133,:150 → /finance/reconciliation?tab=receivable（FinanceReconciliation.vue:30 tab 消费链）；应付 :137-142 → ?tab=payable；预算 :165-168 → /finance/budget；不可行（营业收入 :113 / 运营费用 :116 / 收支汇总 :120-125）→ 行内「—」:517-520（登记不伪造）
  7. **cost_structure 金额口径核验（F-1 防回归点，R1 复验项）**：:266-279 四卡输入全部为「分」（typeAmountFen :266-269 直接取分无 fenToYuanNumber / totalFen :270 = fenOf 原始分），:274-277 全部 formatFenToYuan(分) 一次转换；Node 独立复算同输入（totalCost=100000 / details[1]=60000 / details[2]=20000 / 其余 20000）→ ¥1,000.00/¥600.00/¥200.00/¥200.00 + 分口径自洽断言 EXIT=0；全文件 grep formatFenToYuan 32 处无元值误传；gridRows :153-161 与其他 6 类报表卡片零改动；report.ts/types/finance.ts/后端 mtime 边界实锤
  8. 强制核验独立复跑：工具栏（打印 window.print :420-422 真实 + 导出 :460-462 disabled+tooltip 登记）；菜单标题「决策工作台」三处（menu.ts:33 / router/index.ts:129 / PageHeader :450，路径零变更）；视觉同源（var(--fts-*) 全 token、V3-A 零引用、#hex 零命中、el-pagination 元素零命中）；typecheck 136=136（FinanceReport.vue 零命中）+ build EXIT=0（QA 独立执行）
- **预期结果**：
  - 9 类报表聚合保持（7 既有端点 + 2 P0 断流）；统计卡片仅真实数据展示（虚构零值卡片不复活）
  - 利润表契约对齐保持（revenueAmount/cogsAmount/grossProfit/operatingExpenses/netProfit 三方一致，金额分→元展示）
  - P0 断流处置保持：KL-057/P1-FIN-BE-001 仅登记不伪造（空态说明 + 重试，不显示空数据）
  - 后端占位检测空态保持（账龄恒 0/预算待实现不展示虚构数值）
  - 穿透保持：可行跳转真实可达；不可行「—」登记不伪造
  - **cost_structure 四卡金额口径统一（F-1 防回归点：分→元一次转换，禁止二次 /100；总=食材+人工+其他自洽）**
  - 打印 window.print 真实 + 导出 disabled+tooltip 登记（登记非 FAIL、不伪造）；菜单标题路径零变更；后端零改动；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + git 旧版对照 + Node 口径独立复算 + mtime 边界实锤 + 构建独立复跑）
- **当前状态**：PASS（2026-09-04，R1 闭环转正）
- **RVT 回退标注（2026-09-04，只标注不删除——任务板 §12.4 方案）**：本条目菜单标题断言「决策工作台」已过时——回退后现状：FinanceReport PageHeader/menu.ts/router meta.title 三处恢复「财务报表」= HEAD 原命名（`docs/quality/ui-rvt-b1-qa-report.md` §2.3）；标题断言由 **REG-FIN-RVT-001/004** 接管；**穿透断言 drillTo 目标已移除（瞬态）**：/finance/reconciliation 路由已随 RVT-001 移除，FinanceReport.vue drillTo 13 处（:128-133 应收 6 处 / :137-142 应付 6 处 / :150 账龄 1 处）当前点击无效——QA 补充发现登记（`docs/quality/ui-rvt-b1-qa-report.md` §3.1），由 **RVT-002** 承接修复（drillTo 改指 /finance/receivable、/finance/payable 独立页），修复后由 REG-FIN-RVT-002 覆盖；本条目其余断言（9 类报表聚合/利润表契约对齐/R1 金额口径防回归点（分→元一次转换）/P0 断流处置/后端占位空态/打印导出登记）回退后仍有效，由 REG-FIN-RVT-004 保留项断言衔接，无冲突。
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **P0 断流 P1-FIN-BE-001 = 登记依赖非 FAIL（不占通过数）**：资产负债表/现金流量表端点后端缺失（KL-057，ReportController.java:30-83 仅 7 端点无 balance-sheet/cash-flow 事实确认）→ 任务池 §11.5 BLOCKED 后端池排期保持；前端空态说明 + 重试（:479-488），不显示空数据、不伪造；**仅登记不混入**（roadmap §5.12 硬约束），端点落地后报表自动可用（成功路径已预留数据 ref，网格渲染待端点落地后接入——已登记）
  - **后端占位 = 登记依赖非 FAIL（不占通过数）**：账龄分析（FinancialReportServiceImpl.java:156-176 恒 0，:169-171「这里简化处理」后端事实独立确认）+ 预算执行（:220-230「预算执行数据待实现」）——前端全零/待实现检测 → 空态说明不展示虚构数值（:349-362），登记后端池；账龄/预算真实数据落地并 QA PASS 后页面自动切换真实渲染（届时登记，不预固化）
  - **穿透不可行 = 登记依赖非 FAIL（不占通过数）**：利润表-营业收入/运营费用、收支汇总行无 drillTo（FinanceRecord 收支记录无页面承载【record.ts 零页面消费】）→ 行内「—」（:517-520）登记不伪造；应收/应付/账龄/成本结构/预算穿透目标页均已核验路由与参数消费（reconciliation tab 参数由 P1-FIN-WS-002 消费链支持）
  - **导出 = 登记依赖非 FAIL（不占通过数）**：EXPORT-001 盘点结论（报表页无导出端点）→ 后端池 P1-FIN-EXPORT-004 + PD-035 导出规则待产品决策；按钮 disabled + tooltip 明示（:460-462），grep 无任何导出/CSV/Blob/download 代码（不伪造）
  - **F-1 防回归点说明（R1 闭环，非限制）**：cost_structure 统计卡片金额口径统一（四卡分→元一次转换）——F-1 已闭环，本项为防回归断言（禁止元值传入 formatFenToYuan 二次 /100，money.ts:8-9 强制规范）；Node 独立复算 4/4 一致 + 分口径自洽断言 EXIT=0
  - 观察 O-WS-B5-1（QA 报告 §六，登记不判 FAIL）：initDefaultDateRange（FinanceReport.vue:430-440）依赖 accounting_periods.periodName 作为结束月份——后端 AccountingPeriod 无 periodName 格式约束（DB 列自由文本）。若期间名非 YYYY-MM 格式，dateRange[1] 非标准 → buildProfitParams :303-308 正则不匹配 → 原样传后端 → 请求失败走失败透传兜底（不伪造）。标准期间数据（创建时人工录入）为 YYYY-MM，风险低；已在 buildProfitParams 有正则防御
  - 观察 O-WS-B5-3（QA 报告 §六，登记不判 FAIL）：permission.ts 工作区存在 159 行未提交变更（历史批次产物，grep「毛利核算/决策工作台/成本管理/财务报表/title」零命中，与本批无关）——回归阶段需确认其提交归属，不影响本批结论
  - **R1 归因限制（复验报告 §六，如实登记）**：工作树未提交（Batch5+R1 合并形态），R1 与 Batch5 同文件改动无法 commit 粒度隔离；归因链 = ① mtime（FinanceReport.vue 01:06 唯一写入窗口，types/finance.ts 00:54 早于 R1 窗口）② 当前代码形态与任务板 R1 after（L3988-3992）逐字一致 ③ before/after 摘要与 b5 报告 §四 根因描述精确对应（:263-266 fenToYuanNumber 已移除）——三重证据归因，置信度高

---

### PD-028/PD-035 限制登记同步（Batch WS-5，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §11.3 P1-FIN-WS-005 L3912-3914【导出/导入登记：EXPORT-001 + 后端池 P1-FIN-EXPORT-004 + PD-035 待决 + 无 import 端点】/ P1-FIN-WS-006 L3974-3979【P0 断流登记：KL-057 → P1-FIN-BE-001 后端池排期 + 导出登记 EXPORT-001 → P1-FIN-EXPORT-004 + PD-035 待决】）→ 决策池（`product-decision-backlog.md:44/155` PD-028 2026-08-31 登记；`:51` PD-035 导出规则登记；`:48` **PD-032 为已决策口径**【最新入库价 = 管理口径】）→ roadmap（`remediation-roadmap.md` §5.12：后端缺口仅登记不混入 + 导出按钮 = disabled + tooltip 明示依赖）→ QA 报告（`docs/quality/ui-ws-b5-qa-report.md` §〇/§一/§二/§六 + `docs/quality/ui-ws-b5-r1-qa-report.md` §六）——登记链完整一致。
- **内容**：① PD-028——资金流水「待核销」状态语义缺失，`fund_flows.status` 与 Batch0-方案2 同批落库；落库前前端不实现、不悬空、不伪造（本批两卡零新增状态列/零 status 推断）。② PD-035——导出规则缺失（文件格式/行数上限/导出口径未定义）；导出按钮 disabled + tooltip 明示 EXPORT-001 盘点结论（cost 域/report 域均无导出端点 → 后端池 P1-FIN-EXPORT-004 登记）。③ **PD-032 为已决策口径标注（非 BLOCKED）**——计价方法 = 最新入库价（管理口径），CaliberNoteBar tooltip 与决策池原文逐字一致（与 REG-UI-TRACE-CL01 同源）。
- **本批执行确认（QA 实锤）**：B-5 月期间筛选 6 环全链有效零死参数（monthrange YYYY-MM → mapQueryParams → DTO → Service ge/le → SQL，period @Pattern YYYY-MM 强制）；summarizeByPeriod 真实接入既有端点（非新增接口，后端 mtime 08-11 零改动实锤）；导出/导入 disabled+tooltip 明示（grep CSV/Blob/download 零命中，无伪造）；P0 断流空态说明 + 重试（KL-057/P1-FIN-BE-001 仅登记不混入，ReportController 仅 7 端点事实确认）；后端占位（账龄恒 0/预算待实现）空态检测不虚构（后端事实独立确认）；穿透不可行「—」登记（record.ts 零页面消费）；cost_structure 四卡分→元一次转换（R1 闭环，Node 复算 4/4 + 自洽断言 EXIT=0）；P1-STOCK-001 零触碰（grep stock/库存 零命中）；后端零改动（mtime 实锤）。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-FIN-WS-005/006）**不受影响**——P0 断流 P1-FIN-BE-001 / 导出 P1-FIN-EXPORT-004 / 后端占位（账龄恒 0、预算待实现）/ 穿透不可行 / PD-028/PD-035 相关均为**登记项非 FAIL**（空态说明 + disabled 登记态，不伪造、不混入，后端就绪并 QA PASS 后反向接入并扩展断言，届时登记不预固化）；PD-032 为已决策口径标注（REG-UI-TRACE-CL01 同源）本批保持；勾稽状态列由 B01 占位卡承接（Batch0-方案2 同批落库后实现）。

---

### QA 观察项登记（Batch WS-5，如实登记不销号、不猜测处置）

按 QA 报告 §六 观察项（O-WS-B5-1~3）+ R1 复验报告 §六 风险与限制 如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-WS-B5-1**（WS-006）：initDefaultDateRange（FinanceReport.vue:430-440）依赖 accounting_periods.periodName 作为结束月份——后端 AccountingPeriod 无 periodName 格式约束（DB 列自由文本）。若期间名非 YYYY-MM 格式，dateRange[1] 非标准 → buildProfitParams :303-308 正则不匹配 → 原样传后端 → 请求失败走失败透传兜底（不伪造）。标准期间数据（创建时人工录入）为 YYYY-MM，风险低；已在 buildProfitParams 有正则防御（同 REG-FIN-WS-006 观察 1）。
2. **O-WS-B5-2**（WS-005）：summarizeByPeriod 后端 DTO 注释成本类型 1~7（含折旧/包装），CostTypeMap 仅 1~6（既有契约）——前端 `CostTypeMap.toFrontend[Number(key)] || key` 兜底为「类型7」标签展示，不崩溃不伪造；标签语义由既有 6 类型契约承载（诊断 §1.9 A 类 costType 既有契约）（同 REG-FIN-WS-005 观察 1）。
3. **O-WS-B5-3**（登记观察）：permission.ts 工作区存在 159 行未提交变更（历史批次产物，grep「毛利核算/决策工作台/成本管理/财务报表/title」零命中，与本批无关）——回归阶段需确认其提交归属，不影响本批结论（同 REG-FIN-WS-006 观察 2）。
4. **R1 归因限制**（R1 复验报告 §六，如实登记）：工作树未提交（Batch5+R1 合并形态），R1 与 Batch5 同文件（FinanceReport.vue）改动无法 commit 粒度隔离；归因链 = ① mtime 边界（FinanceReport.vue 2026-09-04 01:06 为 R1 唯一写入窗口；types/finance.ts 00:54 早于 R1 窗口 = R1 未触碰；后端 6 文件 + report.ts 全 08-11）② 当前代码形态与任务板 R1 after（L3988-3992）逐字一致 ③ before/after 摘要与 b5 报告 §四 根因描述精确对应（:263-266 fenToYuanNumber 已移除）——三重证据归因，置信度高（同 REG-FIN-WS-006 观察 3）。
5. **R1 已登记风险保持（复验报告 §六，不构成 FAIL）**：P0 断流 P1-FIN-BE-001 / 后端占位（账龄恒 0、预算待实现）/ 穿透不可行「—」/ 导出登记（P1-FIN-EXPORT-004）/ O-WS-B5-1（initDefaultDateRange 依赖 periodName 格式）/ O-WS-B5-3（permission.ts 159 行历史未提交产物，回归阶段确认归属）——全部保持，与本复验无关；R1 本身无新增风险（仅消除二次 /100，展示口径与后端分单位契约一致）。

---

### Batch WS-5 门禁

```text
Regression Result: PASS（2026-09-04 Batch WS-5 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为财务模块任务型重构·阶段三 Batch 5 毛利核算 + 决策工作台，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：2/2 卡 QA 独立验收 PASS（首验 `docs/quality/ui-ws-b5-qa-report.md`，2026-09-04：P1-FIN-WS-005 毛利核算 = **PASS**（无 FAIL 点、无 -R，验收标准 8 条 + 强制核验 6/6 全过）；P1-FIN-WS-006 决策工作台 = **FAIL（F-1）**（cost_structure 统计卡片金额口径错误缩小 100 倍）→ 生成 P1-FIN-WS-006-R1；复验 `docs/quality/ui-ws-b5-r1-qa-report.md` = **PASS（F-1 闭环）**（四卡分→元一次转换，Node 独立复算 4/4 一致 + 自洽断言 EXIT=0，typecheck 136=136 + build EXIT=0））
- FAIL 明细：0（WS-006 首验 F-1 经 -R1 复验 PASS 闭环转正；正式基线无 FAIL 状态条目）
- PWL 明细：0（P0 断流 P1-FIN-BE-001 / 导出 P1-FIN-EXPORT-004 / 后端占位（账龄恒 0、预算待实现）/ 穿透不可行 / PD-028/PD-035 为登记依赖非 FAIL——仅登记不伪造、不混入；PD-032 为已决策口径标注（REG-UI-TRACE-CL01 同源）保持）
- 基线：新增 REG-FIN-WS-005/006（正式 91 → 93 条，只增不减、既有条目零修改）
- 观察项：O-WS-B5-1~3 + R1 归因限制 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch WS-6 回归（财务模块任务型重构·阶段三 Batch 6 归位页 4 卡，2026-09-04）

> 验收依据：`docs/quality/ui-ws-b6-qa-report.md`（2026-09-04，QA 独立验收：P1-FIN-WS-007 会计科目 / P1-FIN-WS-008 会计期间 / P1-FIN-WS-009 预算 / P1-FIN-WS-010 审批流配置 四卡 **4/4 PASS**、无 FAIL、无 -R、PWL 0 项；强制核验 6/6 全过）；对照基准 = 任务板 §11.3 P1-FIN-WS-007（验收标准 L4010-4015 + developer 修改证据 L4017-4042）/ WS-008（L4055-4060 + L4062-4080）/ WS-009（L4094-4098 + L4100-4124）/ WS-010（L4139-4143 + L4145-4168）+ 设计 FIN-WB-DESIGN-V1.1（`docs/design/finance-task-workbench-design-20260903.md` §3.7 归位页结构重组表 + §8 评审确认③⑧）+ 诊断（`docs/quality/finance-module-diagnosis-20260903.md` §1.6 Subject C 类 3 处 / §1.7 Period KL-058 / §1.10 Budget / §1.11 Approval B-1/B-2）+ KL-058（`production-known-limitations.md:79`）+ PD-031（`product-decision-backlog.md:47`）/ PD-034（:50）/ PD-035（:51）+ 导出盘点（`docs/quality/finance-export-inventory-20260903.md` P1-FIN-EXPORT-004）+ Batch 2/3 F-1 教训（消费链端到端核验纪律）+ roadmap §5.12 硬约束（后端缺口仅登记不混入；后端零改动；视觉同源）；开发者未自行宣布通过 ✅（任务板 L4042/L4080/L4124/L4168）——QA 报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-WS-007/008/009/010（四卡首验即 PASS，无 -R）——维护规则 5 禁止推测，无推测内容；登记依赖（余额 VO 缺 balance / KL-058 后端侧缺口 / 年份筛选 / responsibleDeptId / categoryId / 审批执行链路 / 导入无端点 / 审批人无数据源）均基于已验收结论如实登记，不预固化。
> 批次口径：本批为**治理批次**（财务模块任务型重构·阶段三 Batch 6 归位页 4 卡，任务池 §11.3），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**阶段三 6 批 10 卡全部完成后停止，等待产品负责人走查（§9.3 硬约束 4，不自动开新批）**。
> 基线计数核对：追加前正式基线 **93 条**（文件实况：基线总览表 93 行，含 REG-FIN-WS-001~006；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 4 条（REG-FIN-WS-007/008/009/010）后 **97 条**，只增不减纪律达成（既有 93 条条目零修改）；候选 0 条维持；**阶段三累计 87 → 97 = 10 条**（Batch 1：001 / Batch 2：002 / Batch 3：003 / Batch 4：004 / Batch 5：005+006 / Batch 6：007~010，无缺卡、无预固化条目）。

### REG-FIN-WS-007

- **测试编号**：REG-FIN-WS-007
- **模块**：财务-会计科目（frontend/src/views/finance/FinanceSubject.vue + api/finance/subject.ts + api/finance/converters.ts（SubjectDataConverter）+ types/finance.ts；后端对照 SubjectController.java / AccountingSubjectQueryDTO.java / AccountingSubjectServiceImpl.java / AccountingSubjectVO.java / AccountingSubject.java【只读确认，零改动——13 文件 mtime 实锤全部 ≤ 2026-08-31，远早于本批开发窗口 2026-09-04 01:19~01:23】）
- **关联任务**：P1-FIN-WS-007（Batch WS-6；QA 独立验收 PASS 2026-09-04 `docs/quality/ui-ws-b6-qa-report.md`）
- **业务场景**：会计科目归位页（FinanceSubject 筛选修复 + 契约补齐，范围 = C 类 3 处假筛选根除 + 树形剪枝保持 + subjectId→id 断链修复 + 余额核验不伪造 + 导入/导出登记）——**C 类 3 处假筛选根除（验收标准 1，诊断 §1.6:165-170）**（searchForm 拆 subjectCode/subjectName/subjectType/status（`FinanceSubject.vue:56-61`）；`handleSearch` 由空实现（:197-199「筛选由 computed 自动处理」）改为 `loadData()`（:207-210）；subjectType/status 经 `SubjectStatusMap.toBackend`（converters.ts:81-84 active→1/inactive→0）/ `SubjectTypeMap.toBackend`（converters.ts:97-104 asset→1…profit→6）转数字——**本地递归过滤 filteredTree/filterTree 删除，grep 全 finance 视图零残留**）；**消费链 6 环端到端零死参数（验收标准 1 重点，Batch 2/3 F-1 教训零复发）**（环1 `FinanceSubject.vue:187-194` loadData 传 page:1/size:1000 + 4 筛选字段 → 环2 `subject.ts:77-90` getList（page/size→current/size、status/subjectType 字符串→数字）→ 环3 GET /v1/finance/subjects `SubjectController.java:87` getPage → 环4 `AccountingSubjectQueryDTO.java:13/16/22/28`（subjectCode/subjectName/subjectType/status）→ 环5 `AccountingSubjectServiceImpl.java:121-143`（like subjectCode :125-127 / like subjectName :128-130 / eq subjectType :131-133 / eq status :137-139）→ 环6 MyBatis-Plus selectPage → SQL——**每环参数一一对应，零断点**）；**树形剪枝保持（验收标准 2）**（`pruneTreeByMatchedIds` :126-135 以后端匹配集合剪枝 + 保留祖先链；`loadData` 有筛选 → getTree 全量树 + getList 匹配集合 → 剪枝（:176-203）；el-table `default-expand-all` :411 保留——筛选结果仍为树形呈现，非扁平分页）；**subjectId→id 断链修复（验收标准 6）**（后端 AccountingSubjectVO 仅返回 subjectId（无 id）→ getTree 响应经 `convertTreeNode` 递归 `SubjectDataConverter.toFrontend`（subject.ts:63-69/:118-122）补 `if (backend.subjectId !== undefined) result.id = String(backend.subjectId)`（converters.ts:486-490）；getTree（subject.ts:132-135）/getById（:97-100）/getList（:80-82）全链经过转换——编辑（handleEdit :255-275 用 row.id → getById）/新增子科目（handleAddChild :238-252 用 row.id → parentId）/启停（handleToggleStatus :311-327 用 row.id）恢复可用，**既有工具项恢复非新功能**）；**科目余额核验不伪造（验收标准 3，PD-031）**（后端 `AccountingSubjectVO.java:10-54` **无 balance 字段**（仅 subjectId/subjectCode/subjectName/parentId/subjectType/direction/isLeaf/status/remark/createTime/children）；实体 `AccountingSubject.java:64` 有 `private Long balance` 但 convertToVO（BeanUtils.copyProperties）不输出；前端**零新增列/零发明数据源**（grep FinanceSubject.vue balance 零命中）——登记：待后端 VO 补 balance，与 PD-031 决策联动）；**导入/导出登记（验收标准 4）**（`FinanceSubject.vue:390-399` 导出 disabled + tooltip「P1-FIN-EXPORT-004 后端池排期 + PD-035 导出规则待产品决策，登记不伪造」+ 导入 disabled + tooltip「无后端端点（登记：后端池需新增 import 端点）」；grep 无 CSV/Blob/download 代码）；**视觉同源（验收标准 5）**（样式仅 var(--fts-border-primary) :521；组件用 PageHeader/StatusTag/core 体系；grep 硬编码颜色零命中、V3-A 零引用）
- **测试目的**：防止科目页 C 类 3 处假筛选回归（**F-1 教训防回归点：6 环全链零死参数，本地递归过滤不得复活**）；树形剪枝 + default-expand-all 保持（筛选结果树形呈现）；subjectId→id 断链不得复发（编辑/新增子科目/启停可用）；余额不伪造（VO 无 balance 时零新增列）；导入/导出登记保持（disabled+tooltip 明示）；后端零改动。
- **测试步骤**：
  1. C 类筛选根除核验：searchForm 双输入 + 类型/状态数字转换（FinanceSubject.vue:56-61 + converters.ts:81-84/:97-104）；handleSearch = loadData()（:207-210）；grep 全 finance 视图 filteredTree/filterTree 零命中（本地过滤零残留）
  2. 消费链 6 环端到端核验：FinanceSubject.vue:187-194 → subject.ts:77-90 → SubjectController.java:87 → AccountingSubjectQueryDTO.java:13/16/22/28 → AccountingSubjectServiceImpl.java:121-143（like/eq）→ selectPage SQL；逐环参数一一对应零断点
  3. 树形剪枝核验：pruneTreeByMatchedIds :126-135（匹配集合剪枝保留祖先链）；loadData :176-203（无筛选 getTree 全量 / 有筛选 getTree+getList 剪枝）；default-expand-all :411 保留
  4. id 断链核验：convertTreeNode 递归（subject.ts:63-69）+ converters.ts:486-490 subjectId→id；getTree/getById/getList 全链转换；编辑/新增子科目/启停用 row.id 路径可用
  5. 余额不伪造核验：AccountingSubjectVO.java:10-54 无 balance + 实体 :64 有但 convertToVO 不输出；前端 grep balance 零命中；登记（PD-031 联动）不新增列
  6. 导入/导出登记核验：:390-399 disabled + tooltip（P1-FIN-EXPORT-004 + PD-035 / 无 import 端点）；grep 无 CSV/Blob/download 代码
  7. 强制核验独立复跑：视觉同源（var(--fts-*) 全 token、V3-A 零引用、硬编码颜色零命中）；后端零改动（mtime 实锤）；typecheck 136=136（本批 8 文件零命中）+ build EXIT=0（QA 独立执行）
- **预期结果**：
  - C 类 3 处假筛选根除：4 筛选参数真实发送后端，消费链 6 环全链有效零死参数（F-1 教训防回归点）
  - 树形剪枝保持：筛选结果树形呈现（匹配集合剪枝保留祖先链），default-expand-all 保留，本地过滤零残留
  - id 断链修复保持：subjectId→id 转换全链生效，编辑/新增子科目/启停可用（既有工具项恢复）
  - 余额不伪造：VO 无 balance 时零新增列/零发明数据源（登记待后端补，PD-031 联动）
  - 导入/导出登记保持：disabled + tooltip 明示登记依赖（登记非 FAIL、不伪造）
  - 后端零改动；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + 消费链逐环核验 + mtime 边界实锤 + 构建独立复跑）
- **当前状态**：PASS（2026-09-04）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **科目余额展示 = 登记依赖非 FAIL（不占通过数）**：AccountingSubjectVO 无 balance 字段（后端事实确认）→ 前端零新增列、不伪造；「管理台账口径」余额展示待后端 VO 补 balance（后端池登记，与 PD-031 决策联动），后端就绪并 QA PASS 后接入（届时登记，不预固化）
  - **导出 = 登记依赖非 FAIL（不占通过数）**：EXPORT-001 盘点结论（科目无导出端点）→ 后端池 P1-FIN-EXPORT-004 + PD-035 导出规则待产品决策；按钮 disabled + tooltip 明示（:390-399），grep 无任何导出/CSV/Blob/download 代码（不伪造）
  - **导入 = 登记依赖非 FAIL（不占通过数）**：无 import 端点（后端池需新增 import 端点登记）；按钮 disabled + tooltip 明示，不伪造
  - **getList size=1000 口径（O-B6-4 同源，登记不判 FAIL）**：科目筛选走 getList size=1000 覆盖科目全量规模（树形视图下分页不适用），匹配集合仅用于剪枝祖先链、非展示分页——任务卡已声明口径（L4039 登记）
  - 观察 O-B6-1（QA 报告 §六，登记不判 FAIL）：SubjectTypeMap 为 6 值（converters.ts:88-104，asset/liability/equity/cost/income/profit → 1~6），后端 AccountingSubjectQueryDTO.java:22 / AccountingSubjectVO.java:29-30 注释为 5 值（1资产 2负债 3权益 4成本 5损益）——**既有契约（2026-06-30 改造时已存在，非本批引入）**；getPage eq 为数字比较，注释差异不影响 SQL 执行；数据侧是否含 subject_type=6 未核（登记不猜测）
  - 观察 O-B6-3（QA 报告 §六，登记不判 FAIL）：任务卡行号引用偏差（FinanceSubject default-expand-all 任务卡写 :409 实际 :411）——内容与行为完全一致，仅行号随文件演进漂移，不影响结论

### REG-FIN-WS-008

- **测试编号**：REG-FIN-WS-008
- **模块**：财务-会计期间（frontend/src/views/finance/FinancePeriod.vue；后端对照 AccountingPeriodController.java / AccountingPeriodServiceImpl.java【只读确认，零改动——mtime 实锤 2026-08-11】）
- **关联任务**：P1-FIN-WS-008（Batch WS-6；QA 独立验收 PASS 2026-09-04 `docs/quality/ui-ws-b6-qa-report.md`）
- **业务场景**：会计期间归位页（FinancePeriod KL-058 修复 + 筛选核验，范围 = KL-058 无兜底身份 + 后端校验缺口登记 + 年份筛选核验 + 动作语义零变更 + 导出登记）——**KL-058 根除（验收标准 1，诊断 §1.7:191）**（`const operatorId = String(permissionStore.userInfo?.userId ?? '1')`（兜底管理员）→ `const operatorId = computed<string>(() => permissionStore.userInfo?.userId != null ? String(...) : '')`（`FinancePeriod.vue:41-44`）——**零兜底**；三动作函数头部防御 `if (!operatorId.value)` 提示「登录会话缺少用户信息」（:271/:299/:321）；动作按钮 `:disabled="!operatorId"` + el-tooltip「登录会话缺少用户信息，无法执行（KL-058：不再兜底管理员身份）」（:428-451）——取不到会话时入口禁用 + 明示，零伪造；grep FinancePeriod.vue `?? '1'`/`userId ?? '1'` **零命中**）；**后端 operatorId 校验核验（验收标准 1，结论=不校验，登记后端池）**（AccountingPeriodController closePeriod/reopenPeriod/profitTransfer 收 `@RequestParam Long operatorId`（:57/:64/:85）；AccountingPeriodServiceImpl:122 `period.setCloseUserId(operatorId)` 直接写入 / :148 置空——**无 SecurityContext 认证主体一致性校验**，任一登录用户可传任意 operatorId 冒充他人 → 登记后端池（KL-058 后端侧缺口：需从 SecurityContext 取认证主体，忽略/校验请求参数））；**年份筛选核验（验收标准 2，结论=后端不支持，登记不补 UI）**（AccountingPeriodController.getPage 仅 current/size/periodType/status 四参（:46-51，**无 year**）；AccountingPeriodServiceImpl.getPage 仅 periodType/status 两个过滤（:81-86）；前端 loadData（:117-140）**零 year 发送**——补年份筛选 = 死参数假筛选 → 登记不新增（后端池：getPage 补 year 参数或按 startDate 年区间过滤））；**结账/反结账/损益结转/试算平衡/结账检查动作保留（验收标准 3）**（`accounting-period.ts` **mtime 2026-08-11 零改动**（实锤）；三动作仅 operatorId 取值方式 + 禁用防御变更，`accountingPeriodApi.close/reopen/profitTransfer(row.id, operatorId.value)` 调用形态不变（:281/:309/:331）；试算平衡/结账检查（:232-265）零变更——**动作语义/端点/请求体零变更**）；**导出登记（验收标准 4）**（`FinancePeriod.vue:392-396` 导出 disabled + tooltip「P1-FIN-EXPORT-004 后端池排期 + PD-035 待产品决策，登记不伪造」；grep 无导出代码）
- **测试目的**：防止 KL-058 身份伪造回归（**防回归点：`?? '1'` 兜底零残留，operatorId 强制会话取值**）；后端 operatorId 校验缺口登记保持（伪造面收窄未根除，如实登记不销号）；年份筛选不新增假筛选（后端不支持 → 登记不补 UI）；结账动作语义零变更（accounting-period.ts 零改动）；导出登记保持；后端零改动。
- **测试步骤**：
  1. KL-058 根除核验：operatorId computed 会话取值（FinancePeriod.vue:41-44，零兜底）；grep `?? '1'`/`userId ?? '1'` 零命中；三动作防御 :271/:299/:321；按钮 disabled + tooltip :428-451
  2. 后端校验缺口核验：Controller:57/:64/:85 收 @RequestParam Long operatorId + ServiceImpl:122/:148 直接写入/置空无一致性校验 → 登记后端池（与任务卡 L4066/:4077 一致）
  3. 年份筛选核验：Controller:46-51 getPage 四参无 year + ServiceImpl:77-100 仅 periodType/status 过滤 + 前端 loadData :117-140 零 year 发送 → 登记不补 UI（后端池登记）
  4. 动作语义核验：accounting-period.ts mtime 08-11 零改动；三动作调用形态 :281/:309/:331 不变；试算平衡/结账检查 :232-265 零变更
  5. 导出登记核验：:392-396 disabled + tooltip（P1-FIN-EXPORT-004 + PD-035）；grep 无导出代码
  6. 强制核验独立复跑：视觉同源（var(--fts-*) :582-617、V3-A 零引用）；后端零改动（mtime 实锤）；typecheck 136=136 + build EXIT=0（QA 独立执行）
- **预期结果**：
  - KL-058 根除：operatorId 强制从会话取，零兜底；`?? '1'` 零残留；取不到会话时三动作入口禁用 + tooltip 明示，零伪造
  - 后端 operatorId 校验缺口登记保持（后端池排期；伪造面收窄未根除，如实登记）
  - 年份筛选不新增假筛选（后端 getPage 无 year，登记后端池）
  - 结账动作语义零变更（端点/请求体/调用形态不变，accounting-period.ts 零改动）
  - 导出 disabled + tooltip 明示登记依赖（登记非 FAIL、不伪造）；后端零改动；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + mtime 边界实锤 + 构建独立复跑）
- **当前状态**：PASS（2026-09-04）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **KL-058 后端侧缺口 = 登记依赖非 FAIL（不占通过数）**：operatorId 与认证主体一致性校验缺失（AccountingPeriodController/ServiceImpl 事实确认）→ 登记后端池排期；前端已不再伪造（本批根除），**伪造面收窄但未根除**（如实登记，任务卡 :4077 一致）——后端从 SecurityContext 取主体并 QA PASS 后闭环（届时登记，不预固化）
  - **年份筛选 = 登记依赖非 FAIL（不占通过数）**：后端 getPage 无 year 参数（死参数）→ 补 UI 需后端端点扩展（后端池登记），前端零发送，不新增假筛选
  - **导出 = 登记依赖非 FAIL（不占通过数）**：P1-FIN-EXPORT-004 后端池排期 + PD-035 待产品决策；按钮 disabled + tooltip 明示（:392-396），grep 无任何导出代码（不伪造）
  - 观察 O-B6-3（QA 报告 §六，登记不判 FAIL）：任务卡行号引用偏差（内容与行为完全一致，仅行号随文件演进漂移，不影响结论）

### REG-FIN-WS-009

- **测试编号**：REG-FIN-WS-009
- **模块**：财务-预算（frontend/src/views/finance/FinanceBudget.vue + types/finance.ts（FinanceBudgetQueryForm.budgetType 数字契约）；后端对照 BudgetController.java / BudgetQueryDTO.java / BudgetServiceImpl.java【只读确认，零改动——mtime 实锤 2026-08-16/08-11】）
- **关联任务**：P1-FIN-WS-009（Batch WS-6；QA 独立验收 PASS 2026-09-04 `docs/quality/ui-ws-b6-qa-report.md`）
- **业务场景**：预算归位页（FinanceBudget 筛选补齐 + 审批入口登记，范围 = 预算类型筛选补 UI + responsibleDeptId/categoryId 核验登记 + 审批入口 PD-034 登记 + 双轨语义漂移登记 + 导出登记）——**预算类型筛选补 UI（验收标准 1，消费链 7 环端到端零死参数）**（环1 `FinanceBudget.vue:16-19` searchForm.budgetType + :34-40 下拉选项 1~5（收入/成本/费用/利润/现金流预算）→ 环2 :129-131 `params.budgetType = searchForm.value.budgetType` → 环3 `budget.ts:28-44` mapQueryParams rest 透传（budgetType 走 `{...rest}`）→ 环4 GET /v1/finance/budgets `BudgetController.java:53` getPage → 环5 `BudgetQueryDTO.java:22` budgetType（注释「1-收入 2-成本 3-费用 4-利润 5-现金流」与前端选项逐字一致）→ 环6 `BudgetServiceImpl.java:126-127` `wrapper.eq(Budget::getBudgetType, ...)` → 环7 selectPage → SQL——**前端数字 1~5 直传后端 Integer eq，零死参数**；`getBudgetTypeLabel`（:82-101）数字 1~5 → 收入/成本/费用/利润/现金流 + 字符串语义兼容，筛选与列表展示语义一致（原数字直出消除））；**responsibleDeptId 死参数核验（验收标准 1，结论=死参数，登记不补 UI）**（`BudgetQueryDTO.java:28` 有 responsibleDeptId；`BudgetServiceImpl.java:118-131` getPage 仅 budgetYear/budgetMonth/budgetType/categoryId 四个 eq、**无 responsibleDeptId eq**；前端 grep `responsibleDeptId` 零命中（零发送）——DTO 层支持但 Service 层死参数确认 → 登记后端池（Service 补 eq 后前端再接），不新增假筛选（Batch 2 F-1 同型纪律））；**categoryId 无数据源核验（验收标准 1，结论=无数据源，登记不补 UI）**（`BudgetServiceImpl.java:129-130` eq 就绪；前端无预算分类目录接口（grep `categoryId` 前端零命中）——Service 就绪但前端无数据源构造有效下拉 → 登记不猜测分类枚举（任务卡 :4108 一致））；**审批动作入口先行/登记（验收标准 2，PD-034 待决）**（`FinanceBudget.vue:272-276` 「审批」disabled + tooltip「审批动作归属待产品决策（PD-034：T-PUR-01 审批流 vs FinanceApproval 配置驱动），决策后启用；登记不猜测」——**不实现审批动作、不猜审批归属**（BLOCKED 不占通过数，与规则无关部分【筛选/导出】已先行，协议规则 4））；**预算类型双轨语义漂移登记（验收标准 1 附记）**（任务卡 :4119 登记：BudgetFormDialog 创建链路传字符串给后端 Integer budgetType 存在绑定风险（既有问题）——**本批不触碰创建链路**（BudgetFormDialog 零改动），列表筛选已按后端枚举对齐）；**导出登记（验收标准 3）**（`FinanceBudget.vue:277-281` 导出 disabled + tooltip「P1-FIN-EXPORT-004 后端池排期 + PD-035 待产品决策，登记不伪造」；grep 无导出代码）；**既有功能零变更（验收标准 4）**（年份筛选 :264-267 A 类有效，year→budgetYear 经 budget.ts:35-39 映射；statsCards :165-178 零变更；BudgetFormDialog 零改动；budget.ts mtime 08-11 零改动）
- **测试目的**：防止预算页筛选假参数回归（**F-1 教训防回归点：预算类型 7 环全链零死参数，死参数一律登记不新增假筛选**）；responsibleDeptId/categoryId 登记保持（不补假 UI）；审批入口 PD-034 登记保持（BLOCKED 不占通过数，不实现不猜测）；双轨语义漂移登记保持；导出登记保持；后端零改动。
- **测试步骤**：
  1. 预算类型筛选 7 环端到端核验：FinanceBudget.vue:16-19/:34-40 → :129-131 → budget.ts:28-44 rest 透传 → BudgetController.java:53 → BudgetQueryDTO.java:22 → BudgetServiceImpl.java:126-127 eq → SQL；逐环参数一一对应零断点；getBudgetTypeLabel :82-101 数字 1~5 → 语义标签
  2. responsibleDeptId 死参数核验：BudgetQueryDTO.java:28 有字段 + ServiceImpl:118-131 无 eq 消费 + 前端 grep 零命中 → 登记后端池（F-1 同型纪律）
  3. categoryId 无数据源核验：ServiceImpl:129-130 eq 就绪 + 前端无分类目录接口 → 登记不猜测
  4. 审批入口登记核验：:272-276 disabled + tooltip（PD-034 BLOCKED，决策前不实现不猜测）
  5. 双轨语义漂移核验：任务卡 :4119 登记 + BudgetFormDialog 零改动（创建链路本批不触碰）
  6. 导出登记核验：:277-281 disabled + tooltip（P1-FIN-EXPORT-004 + PD-035）；grep 无导出代码
  7. 既有功能零变更核验：年份筛选 :264-267（year→budgetYear :35-39）；statsCards :165-178；BudgetFormDialog 零改动
  8. 强制核验独立复跑：视觉同源（var(--fts-*) 全 token、V3-A 零引用）；后端零改动（mtime 实锤）；typecheck 136=136 + build EXIT=0（QA 独立执行）
- **预期结果**：
  - 预算类型筛选端到端有效：7 环全链零死参数（数字 1~5 直传后端 Integer eq），筛选与列表展示语义一致
  - responsibleDeptId / categoryId 登记保持：不补假 UI、不猜测（后端池 / 无数据源登记）
  - 审批入口 disabled + tooltip 登记保持（PD-034 BLOCKED 不占通过数，决策后启用）
  - 双轨语义漂移登记保持（创建链路本批不触碰）；导出登记保持（登记非 FAIL、不伪造）
  - 既有年份筛选/统计卡片/详情/表单零变更；后端零改动；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + 消费链逐环核验 + mtime 边界实锤 + 构建独立复跑）
- **当前状态**：PASS（2026-09-04）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **PD-034 审批入口 = BLOCKED 登记项（不占通过数）**：预算审批动作归属待产品决策（T-PUR-01 审批流 vs FinanceApproval 配置驱动）；入口 disabled + tooltip 明示（:272-276），决策前不实现不猜测；与规则无关部分（筛选/导出）已先行（协议规则 4）——决策落地并 QA PASS 后启用并扩展断言（届时登记，不预固化）
  - **responsibleDeptId 死参数 = 登记依赖非 FAIL（不占通过数）**：DTO 层支持但 Service 层未消费（后端事实确认）→ 登记后端池（Service 补 eq 后前端再接），不新增假筛选（Batch 2 F-1 同型纪律）
  - **categoryId 无数据源 = 登记依赖非 FAIL（不占通过数）**：Service eq 就绪但前端无预算分类目录接口 → 登记不猜测分类枚举
  - **双轨语义漂移 = 登记依赖非 FAIL（不占通过数）**：BudgetFormDialog 创建链路传字符串给后端 Integer budgetType 存在绑定风险（既有问题，本批不触碰创建链路）；列表筛选已按后端枚举对齐；表单侧语义统一待预算域整体整改（登记后端池/预算域，不猜测）
  - **导出 = 登记依赖非 FAIL（不占通过数）**：P1-FIN-EXPORT-004 后端池排期 + PD-035 待产品决策；按钮 disabled + tooltip 明示（:277-281），grep 无任何导出代码（不伪造）
  - 观察 O-B6-3（QA 报告 §六，登记不判 FAIL）：任务卡行号引用偏差（FinanceBudget getBudgetTypeLabel 任务卡写 :55-73 实际 :82-101）——内容与行为完全一致，仅行号随文件演进漂移，不影响结论

### REG-FIN-WS-010

- **测试编号**：REG-FIN-WS-010
- **模块**：财务-审批流配置（frontend/src/views/finance/FinanceApproval.vue + api/finance/approval-flow.ts；后端对照 ApprovalFlowConfigController.java / ApprovalFlowConfigQueryDTO.java / ApprovalFlowConfigServiceImpl.java【只读确认，零改动——mtime 实锤 2026-08-11】）
- **关联任务**：P1-FIN-WS-010（Batch WS-6；QA 独立验收 PASS 2026-09-04 `docs/quality/ui-ws-b6-qa-report.md`）
- **业务场景**：审批流配置归位页（FinanceApproval B-1/B-2 映射 + 字段级契约补齐 + CRUD 入口，范围 = B-1/B-2 映射修正 + 契约补齐 + CRUD 4 端点真实接入 + toggleEnabled 补参 + 审批执行链路登记 + 导出登记）——**B-1/B-2 映射修正（验收标准 1，消费链 7 环端到端零死参数）**（环1 `FinanceApproval.vue:62-77` buildQueryParams（enabled :70-72 转 boolean / configName :73-75）→ 环2 `approval-flow.ts:83-92` mapQueryParams 补 `enabled→isEnabled`（:89）+ `configName→keyword`（:90）→ 环3 GET /v1/finance/approval-flow-configs → 环4 `ApprovalFlowConfigQueryDTO.java:19/:22`（isEnabled/keyword，**行号与任务卡引用逐字一致**）→ 环5 `ApprovalFlowConfigServiceImpl.java:113-118`（eq isEnabled :113-115 / like configName :116-118）→ 环6 selectPage → SQL——**原两筛选无效根因 = API 映射层断点（诊断 §1.11 B-1/B-2），修复后真实生效，零死参数**）；**字段级契约补齐（验收标准 2，断链修复）**（`approval-flow.ts:40-75` ApprovalFlowConfigDataConverter：toFrontend（configId→id :44 / approvalNodes JSON 字符串→nodes 数组 :45-54 / isEnabled→enabled :55）/ toDTO（nodes→JSON.stringify→approvalNodes :62-67 / enabled→isEnabled :69）；getList（:109-122）/getById（:139-142）/getByDocumentType（:129-132）/create（:149-153）/update（:161-165）/toggleEnabled（:181-188）全链路转换——列表 nodes 列（formatNodes :136-142 真实节点链 :355）、enabled 列（:356 真实启用/禁用色）断链修复；详情 nodes 表（:380-386）真实展示；统计卡片 enabled 过滤（:80-82）真实生效）；**CRUD 入口真实接入（验收标准 2/3，全部既有端点）**（页面：handleCreate :204-214 + handleSubmit create|update :257-281；handleEdit getById 回填 :217-234；handleToggleEnabled :284-301；handleDelete :304-319；「新建配置」按钮 :326-328；行内 编辑/启停/删除/详情 :357-362；表单对话框（节点动态行）:392-437；后端端点对照：POST /v1/finance/approval-flow-configs（Controller:29-33）/ PUT /{id}（:36-40）/ GET /{id}（getDetail :49-54，**getById = 既有端点封装，非新增接口**）/ DELETE /{id}（:43-47）/ PUT /{id}/toggle-enabled（:71-75）——**端点集合零新增**）；**toggleEnabled 契约修正（验收标准 4）**（后端 `ApprovalFlowConfigController.java:73` `@RequestParam Boolean enabled` 必填；前端 `approval-flow.ts:181-188` 补 `(id, enabled)` 传 `{ params: { enabled } }`；页面传 `row.id, !row.enabled`（:292）——**原缺参调用必 400 根因消除**；启用冲突唯一性校验（`ApprovalFlowConfigServiceImpl.java:155-162` BusinessException「该单据类型已存在启用的审批流配置」）错误透传（FinanceApproval.vue:296-299 catch 含后端 message））；**审批人 ID 文本输入登记（验收标准 5）**（表单审批人输入 :418-423 placeholder「审批人ID（逗号分隔）」+ syncApproverIds :252-254（split /[,，]/ → approverIds 数组）——不伪造用户选择器；后端 approvalNodes 仅 JSON 存储无解析校验，节点语义由审批执行方消费）；**审批执行链路后端未实现登记（验收标准 6）**（任务卡 :4165 登记：approvalNodes 解析/审批人校验后端未实现，属后端池/审批域整改项）；**导出登记（验收标准 7）**（`FinanceApproval.vue:344-348` 导出 disabled + tooltip「P1-FIN-EXPORT-004 后端池排期 + PD-035 待产品决策，登记不伪造」；grep 无导出代码）
- **测试目的**：防止审批流配置页筛选假参数回归（**F-1 教训防回归点：B-1/B-2 映射 7 环全链零死参数**）；字段级契约（configId→id / approvalNodes↔nodes / isEnabled↔enabled）断链不得复发；CRUD 真实接入保持（端点集合零新增）；toggleEnabled 补参保持（缺参 400 不得复发）；审批执行链路/审批人数据源登记保持；导出登记保持；后端零改动。
- **测试步骤**：
  1. B-1/B-2 映射 7 环端到端核验：FinanceApproval.vue:62-77 → approval-flow.ts:83-92（enabled→isEnabled :89 / configName→keyword :90）→ ApprovalFlowConfigQueryDTO.java:19/:22 → ApprovalFlowConfigServiceImpl.java:113-118（eq/like）→ SQL；逐环参数一一对应零断点
  2. 字段级契约核验：ApprovalFlowConfigDataConverter :40-75（toFrontend configId→id / approvalNodes JSON→nodes / isEnabled→enabled；toDTO 反向）；getList/getById/getByDocumentType/create/update/toggleEnabled 全链路转换 :109-188；nodes/enabled 列真实展示（:355-356/:380-386）
  3. CRUD 入口核验：handleCreate :204-214 / handleSubmit create|update :257-281 / handleEdit getById 回填 :217-234 / handleToggleEnabled :284-301 / handleDelete :304-319；后端端点对照（Controller:29-33/:36-40/:43-47/:49-54/:71-75）——端点集合零新增，getById 为既有封装
  4. toggleEnabled 契约核验：Controller:73 @RequestParam Boolean enabled 必填 + approval-flow.ts:181-188 补 (id, enabled) + 页面 :292 传 !row.enabled；唯一性冲突（ServiceImpl:155-162）错误透传 :296-299
  5. 审批人输入登记核验：:418-423 逗号分隔 ID 文本输入 + syncApproverIds :252-254；不伪造用户选择器；审批执行链路后端未实现登记（任务卡 :4165 一致）
  6. 导出登记核验：:344-348 disabled + tooltip（P1-FIN-EXPORT-004 + PD-035）；grep 无导出代码
  7. 强制核验独立复跑：视觉同源（var(--fts-*) :443-446、V3-A 零引用）；后端零改动（mtime 实锤）；typecheck 136=136 + build EXIT=0（QA 独立执行）
- **预期结果**：
  - B-1/B-2 映射端到端有效：7 环全链零死参数（enabled→isEnabled / configName→keyword 真实生效）
  - 字段级契约补齐保持：nodes/enabled 列断链不得复发（真实节点链/启用禁用色展示）
  - CRUD 4 端点真实接入保持（全为既有端点，端点集合零新增；getById = 既有 GET /{id} 封装）
  - toggleEnabled 补参保持（缺参 400 不得复发；业务冲突错误透传含后端 message）
  - 审批执行链路/审批人无数据源登记保持（不伪造）；导出登记保持（登记非 FAIL、不伪造）
  - 后端零改动；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + 消费链逐环核验 + mtime 边界实锤 + 构建独立复跑）
- **当前状态**：PASS（2026-09-04）
- **限制说明 / 观察项登记（如实登记，不销号、不猜测处置）**：
  - **审批执行链路 = 登记依赖非 FAIL（不占通过数）**：approvalNodes 解析/审批人校验后端未实现（任务卡 :4165 登记一致）→ 后端池/审批域整改项；前端仅登记不伪造（不实现执行动作、不猜节点语义）
  - **审批人数据源 = 登记依赖非 FAIL（不占通过数）**：页面无用户目录数据源 → 审批人采用「逗号分隔 ID」文本输入（:418-423），不伪造用户选择器；后端 approvalNodes 仅 JSON 存储无解析校验（节点语义由审批执行方消费，执行链路未实现登记）
  - **导出 = 登记依赖非 FAIL（不占通过数）**：P1-FIN-EXPORT-004 后端池排期 + PD-035 待产品决策；按钮 disabled + tooltip 明示（:344-348），grep 无任何导出代码（不伪造）
  - 观察 O-B6-2（QA 报告 §六，登记不判 FAIL）：Approval CRUD 按钮无 v-permission 前端权限指令，但后端端点均有 `@PreAuthorize("hasAuthority('finance:approval-flow-config:manage')")` 拦截——与全站既有模式一致（如 FinanceBudget 新增预算按钮同无指令），权限由后端强校验，登记观察
  - 观察 O-B6-3（QA 报告 §六，登记不判 FAIL）：任务卡行号引用偏差（approval-flow.ts mapQueryParams 任务卡写 :29-40 实际 :83-92）——内容与行为完全一致，仅行号随文件演进漂移，不影响结论

---

### PD-031/PD-034/PD-035 限制登记同步（Batch WS-6，BLOCKED，决策前不实现）

- **登记链（完整核对）**：任务卡证据块（`production-remediation-task-board.md` §11.3 P1-FIN-WS-007 L4039【余额登记：VO 无 balance → 后端池 + PD-031 联动】/ P1-FIN-WS-008 L4066/:4077【KL-058 后端侧缺口登记 + 年份筛选登记】/ P1-FIN-WS-009 L4107-4123【responsibleDeptId/categoryId 登记 + PD-034 审批入口 BLOCKED + 双轨漂移登记】/ P1-FIN-WS-010 L4165【审批执行链路 + 审批人数据源登记】）→ 决策池（`product-decision-backlog.md:47` **PD-031** 科目余额管理台账口径；`:50` **PD-034** 预算审批归属待决；`:51` **PD-035** 导出规则待决）→ 限制库（`production-known-limitations.md:79` KL-058 身份伪造）→ roadmap（`remediation-roadmap.md` §5.12：后端缺口仅登记不混入 + 导出按钮 = disabled + tooltip 明示依赖）→ QA 报告（`docs/quality/ui-ws-b6-qa-report.md` §〇/§一~四/§六）——登记链完整一致。
- **内容**：① **PD-031**——科目余额 = 管理台账口径真相（已决策），但后端 AccountingSubjectVO 无 balance 字段（实体有、convertToVO 不输出，后端事实确认）→ 前端零新增列、不伪造；「管理台账口径」余额展示待后端 VO 补 balance（后端池登记，与 PD-031 决策联动）。② **PD-034**——预算审批动作归属待产品决策（T-PUR-01 审批流 vs FinanceApproval 配置驱动）→ 审批入口 disabled + tooltip 登记态（BLOCKED 不占通过数，决策前不实现不猜测；与规则无关部分【筛选/导出】已先行）。③ **PD-035**——导出规则缺失（文件格式/行数上限/导出口径未定义）→ 本批 4 页导出按钮全部 disabled + tooltip 明示 EXPORT-001 盘点结论（→ 后端池 P1-FIN-EXPORT-004 登记）。
- **本批执行确认（QA 实锤）**：科目 C 类 3 处假筛选根除（消费链 6 环零死参数，双输入 + 数字转换）；期间 KL-058 根除（`?? '1'` 零残留 + 三动作防御）；预算类型筛选 7 环零死参数（1~5 枚举直传）；审批 B-1/B-2 映射 7 环零死参数（enabled→isEnabled/configName→keyword）；死参数零发送纪律（期间年份/responsibleDeptId/categoryId 三处登记不补 UI，grep 零发送实锤）；CRUD 4 端点真实接入（端点集合零新增，getById 为既有封装）；toggleEnabled 补参（缺参 400 根因消除）；余额/审批执行链路/审批人数据源/导入无端点全部登记不伪造；后端零改动（13 文件 mtime 实锤全 ≤ 08-31）；typecheck 136=136 + build EXIT=0 独立复跑；P1-STOCK-001 零触碰（grep 库存域零命中）。
- **对基线影响面（如实登记，不销号、不猜测处置）**：本批基线（REG-FIN-WS-007/008/009/010）**不受影响**——科目余额 VO 缺 balance（PD-031 联动）/ KL-058 后端侧校验缺口 / 期间年份筛选 / responsibleDeptId 死参数 / categoryId 无数据源 / 审批执行链路未实现 / 科目导入无端点 / 审批人无用户目录数据源 相关均为**登记项非 FAIL**（disabled 登记态/零发送/零新增列，不伪造、不混入，后端就绪并 QA PASS 后反向接入并扩展断言，届时登记不预固化）；PD-034 为 BLOCKED 登记项（不占通过数）；PD-035 为导出规则待决（按钮 disabled 登记态）。

---

### QA 观察项登记（Batch WS-6，如实登记不销号、不猜测处置）

按 QA 报告 §六 观察项（O-B6-1~4）如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **O-B6-1**（WS-007）：Subject 类型枚举注释差异——`SubjectTypeMap`（converters.ts:88-104）为 6 值（asset/liability/equity/cost/income/profit → 1~6），后端 `AccountingSubjectQueryDTO.java:22` / `AccountingSubjectVO.java:29-30` 注释为 5 值（1资产 2负债 3权益 4成本 5损益）。此为既有契约（2026-06-30 改造时已存在，非本批引入）；getPage eq 为数字比较，注释差异不影响 SQL 执行；数据侧是否含 subject_type=6 未核（登记不猜测）（同 REG-FIN-WS-007 观察 1）。
2. **O-B6-2**（WS-010）：Approval CRUD 按钮无 v-permission 前端权限指令——新建/编辑/删除/启停按钮无前端权限指令，但后端端点均有 `@PreAuthorize("hasAuthority('finance:approval-flow-config:manage')")` 拦截；与全站既有模式一致（如 FinanceBudget 新增预算按钮同无指令），权限由后端强校验，登记观察（同 REG-FIN-WS-010 观察 1）。
3. **O-B6-3**（四卡共用）：任务卡行号引用偏差——任务卡引用的部分行号与本批实际代码行号有偏差（approval-flow.ts mapQueryParams 任务卡写 :29-40 实际 :83-92；FinanceBudget getBudgetTypeLabel 任务卡写 :55-73 实际 :82-101；FinanceSubject default-expand-all 任务卡写 :409 实际 :411）。**内容与行为完全一致**，仅行号随文件演进漂移，登记说明不影响结论（同四卡条目观察）。
4. **O-B6-4**（WS-007）：getList size=1000 口径——科目筛选走 getList size=1000 覆盖科目全量规模（树形视图下分页不适用），匹配集合仅用于剪枝祖先链、非展示分页——任务卡已声明口径，登记（同 REG-FIN-WS-007 限制说明 3）。

---

### Batch WS-6 门禁

```text
Regression Result: PASS（2026-09-04 Batch WS-6 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为财务模块任务型重构·阶段三 Batch 6 归位页 4 卡，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：4/4 卡 QA 独立验收 PASS（`docs/quality/ui-ws-b6-qa-report.md`，2026-09-04：P1-FIN-WS-007 会计科目 / WS-008 会计期间 / WS-009 预算 / WS-010 审批流配置 四卡全 PASS、无 FAIL、无 -R、PWL 0 项；强制核验 6/6 全过——git diff 纯契约映射/展示/UI 接入（端点集合零新增）/ 后端零改动（13 文件 mtime 实锤全 ≤ 08-31）/ 视觉同源（V3-A 零引用 + filteredTree 本地过滤零残留）/ 死参数零发送纪律（三处登记不补 UI）+ P1-STOCK-001 零触碰 / typecheck 136=136（本批 8 文件零命中）+ build EXIT=0 独立复跑 / 开发者不自行宣布通过）
- FAIL 明细：0（四卡首验即 PASS，无 -R；正式基线无 FAIL 状态条目）
- PWL 明细：0（PD-031 科目余额管理台账口径 / PD-034 预算审批归属 / PD-035 导出规则为 BLOCKED 登记不占通过数；科目余额 VO 缺 balance / KL-058 后端侧校验缺口 / 期间年份筛选 / responsibleDeptId 死参数 / categoryId 无数据源 / 审批执行链路未实现 / 科目导入无端点 / 审批人无用户目录数据源 = 登记依赖非 FAIL——仅登记不伪造、不混入）
- 基线：新增 REG-FIN-WS-007/008/009/010（正式 93 → 97 条，只增不减、既有条目零修改；阶段三累计 87 → 97 = 10 条）
- 阶段三收口核对：任务型重构 6 工作台 10 卡全部 QA 通过（REG-FIN-WS-001~010 全部在案：Batch 1 001 / Batch 2 002 / Batch 3 003 / Batch 4 004 / Batch 5 005+006 / Batch 6 007~010，无缺卡、无预固化条目），**等待产品负责人走查（§9.3 硬约束 4，不自动开新批）**
- 观察项：O-B6-1~4 如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0 → 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch RVT-1 回归（结构回退批次：P1-FIN-RVT-001 路由/菜单全局回退 + P1-FIN-RVT-004 标题回退批，2026-09-04）

> 验收依据：`docs/quality/ui-rvt-b1-qa-report.md`（2026-09-04，QA 独立验收：P1-FIN-RVT-001 路由/菜单全局回退 = **PASS_WITH_LIMITATION** / P1-FIN-RVT-004 标题回退批 = **PASS_WITH_LIMITATION**——两卡全 PWL、无 FAIL、无 -R；回退范围全部达成且与 HEAD 基线逐行同构；2 项瞬态登记（1 项 QA 补充发现）+ 1 项待走查，均为登记性质不构成 FAIL）；裁决依据 = `remediation-roadmap.md` §5.13（2026-09-04 产品负责人裁决：回退结构、保留修复）；回退基线 = `git show HEAD:frontend/src/router/index.ts` / `HEAD:frontend/src/modules/finance/menu.ts`（HEAD cd1c89b = 回退前原状）；对照基准 = 任务板 §12.2 P1-FIN-RVT-001/004 卡（回退范围清单 + 保留项清单 + developer 修改证据）+ §12.4（回归基线调整方案登记）；开发者未自行宣布通过 ✅——本报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS_WITH_LIMITATION 结论新增 REG-FIN-RVT-001/004（维护规则 5 禁止推测；PWL 限制项逐条确认不阻断发布后固化），无推测内容。
> 批次口径：本批为**治理批次**（结构回退，任务池 §12），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch RVT-2（P1-FIN-RVT-002 核销页回退）随批放行（§12.1 批次节奏，禁止跳步）。
> 基线计数核对：追加前正式基线 **97 条**（文件实况：基线总览表 97 行，含 REG-FIN-WS-001~010；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 2 条（REG-FIN-RVT-001/004）后 **99 条**，只增不减纪律达成（既有 97 条条目零修改；WS 基线仅作接管标注不删除）；候选 0 条维持。
> WS 基线标注落位（§12.4 方案，只标注不删除）：REG-FIN-WS-001/004/005/006 标题断言 + REG-FIN-WS-002/003 合并断言 + REG-FIN-WS-003 A01 穿透断言 → 已在各条目录内如实标注「由 RVT 基线接管/回退后现状」；WS-007~010 无结构断言不标注；功能层断言（筛选端到端/入口/口径/穿透/打印/安全）回退后仍有效，由 REG-FIN-RVT-xxx 保留项断言衔接（已核对无冲突）。

### REG-FIN-RVT-001

- **测试编号**：REG-FIN-RVT-001
- **模块**：财务-路由/菜单（frontend/src/router/index.ts + modules/finance/menu.ts + 5 独立页壳：FinanceLedger.vue / FinanceReceivable.vue / FinancePayable.vue / InvoiceReimbursement.vue / AutoVoucher.vue；后端零改动）
- **关联任务**：P1-FIN-RVT-001（Batch RVT-1；QA 独立验收 PASS_WITH_LIMITATION 2026-09-04 `docs/quality/ui-rvt-b1-qa-report.md`）
- **业务场景**：路由/菜单全局回退（与 HEAD 基线逐行同构，`git show HEAD:router/index.ts` / `HEAD:menu.ts` 对照）——**13 页独立组件直达路由恢复**（router/index.ts:116-128：ledger/receivable/payable/cost/budget/fund/tax/invoice-reimbursement/report/approval/auto-voucher/period/subject 13 条，name/component/meta.title/权限码与 HEAD 逐行一致，diff 零差异）；**/finance/reconciliation、/finance/records 路由移除**（grep router/index.ts 'reconciliation|records' 零财务命中（仅 :149 HR 学习记录非财务）；**dist 无 FinanceReconciliation/FinanceRecords chunk**（路由移除后未打包，死代码路径为零））；**5 条旧重定向移除**（ledger/invoice-reimbursement/auto-voucher/receivable/payable 的 redirect 在 HEAD 基线即不存在、当前亦不存在——git diff HEAD 财务段仅 +2 行回退登记注释 :113-114）；**/finance 根 redirect 恢复原指向**（:115 `{ path: '/finance', redirect: '/finance/ledger' }` = HEAD 基线原值）；**6 个工作台菜单项彻底移除**（grep menu.ts '工作台|记录|核销|对账|合规|毛利|决策' 零命中；menu.ts 与 HEAD diff 仅 +2 行登记注释 :15-16；工作台入口零残留 = 菜单零命中 + 路由零命中 + dist 无 chunk 三重确认，不保留可选入口）；**13 项原菜单恢复**（menu.ts:17-44：财务总账 Notebook / 会计科目 List / 会计期间 Calendar / 应收账款 Wallet / 应付账款 Wallet / 成本管理 TrendCharts / 税务管理 Coin / 发票报销 Money / 财务报表 Document / 预算管理 Calendar / 资金管理 Wallet / 财务审批 Stamp / 自动凭证管理 Connection——标题/图标/顺序与 HEAD 逐行一致）；**scaleLevel 原值**（standard+ 9 项 / chain-standard+ 3 项 / chain-enterprise 1 项（AutoVoucher）= HEAD 原值，diff 零差异，非猜测）；**5 独立页壳新建承功能**（FinanceLedger/FinanceReceivable/FinancePayable/InvoiceReimbursement/AutoVoucher 壳 21-22 行 = PageHeader + 渲染对应 Tab 子组件（LedgerTab/ReceivableTab/PayableTab/ReimbursementTab/TransferTemplateTab），功能代码全部在子组件零回退；dist 5 独立 chunk 产出；壳级内容回迁归 RVT-002/003）
- **测试目的**：防止结构回退再次漂移 —— 13 页直达路由与原菜单（标题/图标/scaleLevel）必须与 HEAD 基线保持同构；工作台路由/菜单入口不得复活（三重零残留）；壳仅承载功能（内容回迁前零功能回退）；限制项（穿透瞬态）由 RVT-002 承接不得漏修。
- **测试步骤**：
  1. 路由核验：router/index.ts:116-128 13 条直达路由与 `git show HEAD:router/index.ts` 财务段逐行对照（name/component/meta.title/权限码 diff 零差异）；:115 根 redirect = /finance/ledger
  2. 工作台路由移除核验：grep router/index.ts 'reconciliation|records' 零财务命中；dist 产物无 FinanceReconciliation/FinanceRecords chunk
  3. 菜单核验：menu.ts:17-44 13 项标题/图标/顺序与 `git show HEAD:menu.ts` 逐行一致；scaleLevel 三档计数（9/3/1）与 HEAD 一致；grep menu.ts 工作台标题零命中
  4. 入口三重零残留核验：菜单零命中 + 路由零命中 + dist 无 chunk（工作台组件未被打包）
  5. 壳文件核验：5 壳读码（21-22 行 = PageHeader + 渲染对应 Tab 子组件），功能代码在子组件零回退
  6. 强制核验独立复跑：typecheck 136=136（本批 11 文件零命中）+ build EXIT=0（QA 独立执行）
- **预期结果**：
  - 13 页直达路由 + 13 项原菜单（标题/图标/scaleLevel/顺序）与 HEAD 基线逐行同构；工作台路由/菜单入口零残留（三重确认）
  - /finance 根 redirect = /finance/ledger（HEAD 原值）；5 条旧重定向不存在（HEAD 基线亦不存在）
  - 5 独立页壳承载功能零回退（内容回迁归 RVT-002/003）；FinanceReconciliation/FinanceRecords 壳保留未删除（删除归 RVT-002/003）
  - 限制登记：FinanceReport drillTo 13 处 + FinanceFund A01 穿透瞬态归 RVT-002；subject/budget 标题差异待走查
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级 grep/diff + HEAD 基线同构对照 + dist 构建产物核验 + 构建独立复跑）
- **当前状态**：PASS_WITH_LIMITATION（2026-09-04）
- **限制说明 / 登记项（如实登记，不销号、不猜测处置）**：
  - **穿透瞬态（QA 补充发现 FinanceReport drillTo 13 处 + developer 已登记 FinanceFund:318 A01）**：两处穿透均引用已移除路由（/finance/reconciliation?tab=receivable|payable、name FinanceRecords）——当前按钮点击无效（vue-router 无匹配路径导航失败/name 未匹配静默），功能缺失非数据错误；均归 **RVT-002** 承接修复（drillTo 改指 /finance/receivable、/finance/payable 独立页；A01 单行改回 name: 'FinanceLedger'）；RVT-002/003 完成前降级为不可用，由 regression/architect 在发布门禁评估（本批为治理批次不进上线门禁判定）；不销号不猜测
  - **subject/budget 标题差异（待走查）**：菜单标题「会计科目」「预算管理」= HEAD 基线原命名，与 §5.13 裁决清单简称（「科目」「预算」）存在文案差异——登记待产品走查，本批不调整（不实施不猜测）
  - **壳文件生命周期登记**：FinanceReconciliation.vue/FinanceRecords.vue 壳保留（untracked，路由已移除、dist 不打包），删除归 RVT-002/003；5 独立页壳内容回迁归 RVT-002/003

---

### REG-FIN-RVT-004

- **测试编号**：REG-FIN-RVT-004
- **模块**：财务-4 页标题（frontend/src/views/finance/FinanceFund.vue / FinanceTax.vue / FinanceCost.vue / FinanceReport.vue PageHeader + modules/finance/menu.ts + router/index.ts meta.title；permissions.ts / role-profiles.ts 只读核对零改动）
- **关联任务**：P1-FIN-RVT-004（Batch RVT-1；QA 独立验收 PASS_WITH_LIMITATION 2026-09-04 `docs/quality/ui-rvt-b1-qa-report.md`）
- **业务场景**：标题回退批（四页标题恢复原命名，三处同步）——**fund【资金管理】**（FinanceFund.vue:429 PageHeader + menu.ts + router/index.ts:121 meta.title 三处同步；description 零变更）；**tax【税务管理】**（FinanceTax.vue:677 + menu.ts + router/index.ts:122）；**cost【成本管理】**（FinanceCost.vue:270 + menu.ts + router/index.ts:119）；**report【财务报表】**（FinanceReport.vue:453 + menu.ts + router/index.ts:124）；路径/组件/scaleLevel/权限码零变更（git diff HEAD 财务段路由与菜单零结构差异，仅注释）；**permissions/role-profiles 文案零漂移（只读核对）**（permissions.ts:722-747 财务权限描述【查看财务总账/查看·管理应收账款/查看·管理应付账款/查看成本管理/查看税务管理/查看·管理发票报销/查看财务报表/查看资金管理/查看·管理自动凭证】= 原命名 + role-profiles.ts:265「财务报表」/:268「税务管理」= 原命名；permissions.ts M 态归因 = product/purchase 域权限新增（:59-66/:528-536/:622-628）非本批；role-profiles.ts git diff 零改动）；**页面内增强 15/15 零回退**（FinanceFund：新建流水 create 入口（fundFlowApi.create :399）/ E 值格式（value-format=YYYY-MM-DD :481/:586）/ 失败透传 loadFailed（:27/:218/:225/:493）/ 合计 useSummary unit='yuan'（:11/:46/:110）/ 密度 density="auto"（:514）；FinanceTax：KL-059 税期动态化（buildTaxPeriodOptions :54/:70/:73）/ 缴税幂等 handlePayTax（:237/:759，PD-001 零触碰）/ 申报表打印 window.print（:656）；FinanceCost：B-5 monthrange（:310/:314）/ 成本结构分析 summarizeByPeriod（:199/:208/:405）/ CaliberNoteBar（PD-032，:38/:329）；FinanceReport：9 类报表聚合（reportTypeOptions :70）/ P0 断流处置 p0Broken（:62/:339/:384/:485-486，KL-057 → P1-FIN-BE-001）/ 报表→明细穿透 handleDrill（:428-430/:521 代码保留，目标失效瞬态见限制）/ **R1 金额口径修复**（:261-279 四卡分→元一次 formatFenToYuan 转换，无二次 /100）/ 利润表契约对齐（:110-117 五键真实契约消费）；通用：导出/批量/导入 disabled+tooltip 登记处置保持（PayableTab.vue:362-368 / FinanceCost.vue:289 / FinanceTax.vue:704 等，EXPORT-001/P1-FIN-EXPORT-004/PD-035 登记链））；**finance 域「对账工作台/合规工作台/毛利核算/决策工作台」grep 零命中**（views/finance + modules/finance）
- **测试目的**：防止标题回退再次漂移 —— 四页标题三处（menu/router/PageHeader）必须保持原命名；路径/组件/scaleLevel/权限码零变更；permissions/role-profiles 文案零漂移；页面内增强 15/15 不得回退（含 R1 金额口径防二次 /100）；穿透瞬态登记由 RVT-002 承接。
- **测试步骤**：
  1. 标题三处同步核验：4 页 PageHeader（FinanceFund.vue:429 / FinanceTax.vue:677 / FinanceCost.vue:270 / FinanceReport.vue:453）+ menu.ts + router meta.title（:119/:121-124）逐处一致
  2. 零结构变更核验：git diff HEAD 财务段路由/菜单零结构差异（仅注释）；路径/组件/scaleLevel/权限码零变更
  3. permissions/role-profiles 只读核对：permissions.ts:722-747 + role-profiles.ts:265/:268 文案 = 原命名（零漂移）；M 态归因核对（product/purchase 域）
  4. 保留项 15/15 逐项核验（KL-059/B-5/summarizeByPeriod/9 类报表/P0 断流/R1 金额口径/新建流水/E 值/失败透传/打印/口径标注等）
  5. grep finance 域「对账工作台/合规工作台/毛利核算/决策工作台」零命中
  6. 强制核验独立复跑：typecheck 136=136（本批 11 文件零命中）+ build EXIT=0（QA 独立执行）
- **预期结果**：
  - 四页标题三处同步恢复原命名（fund 资金管理/tax 税务管理/cost 成本管理/report 财务报表）；路径/组件/scaleLevel/权限码零变更
  - permissions/role-profiles 文案零漂移（原命名保留）；finance 域工作台标题 grep 零命中
  - 页面内增强 15/15 零回退（R1 金额口径分→元一次转换防二次 /100）
  - 限制登记：drillTo/A01 穿透瞬态归 RVT-002；注释残留（types/finance.ts:1274 等）待走查
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级 grep/diff + 保留项逐项命中 + 构建独立复跑）
- **当前状态**：PASS_WITH_LIMITATION（2026-09-04）
- **限制说明 / 登记项（如实登记，不销号、不猜测处置）**：
  - **穿透瞬态（同 REG-FIN-RVT-001 限制 1）**：FinanceReport drillTo 13 处（QA 补充发现，本卡文件清单不含 FinanceReport.vue 未扩大范围）+ FinanceFund A01 穿透 → 归 RVT-002 承接修复（drillTo 改指独立页 / A01 改回 FinanceLedger）；RVT-002 完成前穿透按钮不可用（功能缺失非数据错误）
  - **subject/budget 标题差异（待走查，同 REG-FIN-RVT-001 限制 2）**：本批不调整（不实施不猜测）
  - **注释残留登记（非本批文件，待走查）**：types/finance.ts:1274「决策工作台」注释残留（非 finance 域/非本批文件）；FinanceRecords.vue:10 注释已改写（「不对资金流水重复建页签」）；FinanceTax.vue:974、FinanceReport.vue:23-24、各页签组件头注释含「工作台」字样 = WS 批注释残留（非菜单/路由/标题结构，不属验收失败项，登记备查）

---

### Batch RVT-1 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告 §三（`docs/quality/ui-rvt-b1-qa-report.md`）限制/登记项如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **【QA 补充发现·未登记】FinanceReport.vue drillTo 引用已移除路由**（§3.1）：drillTo = /finance/reconciliation?tab=receivable|payable 共 13 处字符串（:128-133 应收 6 处 / :137-142 应付 6 处 / :150 账龄 1 处）+ 头注释 :23-24 同引用——handleDrill（:428-430）router.push 目标路由已移除 → 报表→明细穿透按钮点击无效（vue-router 无匹配路径导航失败）；定性 = 与 developer 已登记 FinanceFund:318 A01 瞬态完全同型（保留项引用已移除路由，批次编排瞬态，本批卡面文件清单不含 FinanceReport.vue 不扩大范围）；**处置 = 归 RVT-002 修复**（drillTo 改指 /finance/receivable、/finance/payable 独立页）——建议 planner 在 RVT-002 卡保留项补充该修复项（不销号不猜测）
2. **【已登记确认】FinanceFund.vue:318 A01 穿透**（§3.2）：router.push({ name: 'FinanceRecords', ... }) 引用已移除路由 name——vue-router name 未匹配静默，按钮无效；处置 = RVT-002 单行改回 name: 'FinanceLedger'（与本批登记一致，确认）
3. **【已登记确认】subject/budget 标题差异**（§3.3）：menu.ts「会计科目」「预算管理」= HEAD 基线原命名，与 §5.13 裁决清单简称（「科目」「预算」）存在文案差异——登记待产品走查，本批不调整（不实施不猜测）
4. **【已登记确认】注释残留（非本批文件）**（§3.4）：types/finance.ts:1274「决策工作台」注释残留（非 finance 域/非本批文件，登记待走查）；FinanceRecords.vue:10 已改写（原「不对账工作台流水重复建页签」）；FinanceTax.vue:974 / FinanceReport.vue:23-24 / 各页签组件头注释含「工作台」字样 = WS 批注释残留（非菜单/路由/标题结构，不属验收失败项，登记备查）

---

### Batch RVT-1 门禁

```text
Regression Result: PASS（2026-09-04 Batch RVT-1 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为结构回退批次（任务池 §12），非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：2/2 卡 QA 独立验收 PASS_WITH_LIMITATION（`docs/quality/ui-rvt-b1-qa-report.md`，2026-09-04：P1-FIN-RVT-001 路由/菜单全局回退 + P1-FIN-RVT-004 标题回退批，两卡全 PWL、无 FAIL、无 -R；回退范围全部达成且与 HEAD 基线逐行同构 + 保留项 15/15 零回退 + typecheck 136=136 本批 11 文件零命中 + build EXIT=0 独立复跑）
- FAIL 明细：0
- PWL 明细：2（逐条确认不阻断发布）——① 穿透瞬态（QA 补充发现 FinanceReport drillTo 13 处 + developer 已登记 FinanceFund:318 A01 穿透）：均指向已移除路由，按钮点击无效（功能缺失非数据错误），归 RVT-002 承接修复，登记性质不构成 FAIL；② subject/budget 标题差异：HEAD 原命名 vs §5.13 清单简称，待产品走查，不实施不猜测；注释残留登记备查
- 基线：新增 REG-FIN-RVT-001/004（正式 97 → 99 条，只增不减、既有条目零修改；WS 基线仅标注接管不删除——REG-FIN-WS-001/004/005/006 标题断言、WS-002/003 合并断言、WS-003 A01 穿透断言已标注，WS-007~010 不标注）
- 观察项：穿透瞬态 2 项 + subject/budget 差异 + 注释残留如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=2（登记性质不构成 FAIL）→ 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch RVT-2 回归（结构回退批次：P1-FIN-RVT-002 核销页回退 + 2 项穿透修复，2026-09-05）

> 验收依据：`docs/quality/ui-rvt-b2-qa-report.md`（2026-09-05，QA 独立验收：P1-FIN-RVT-002 核销页回退 + 2 项穿透修复 = **PASS**——无 FAIL、无 -R；壳/页签删除零残留 + 内容回迁独立页完整 + 保留项 15/15 逐项命中零回退 + 待办区保留为页面级增强（真实字段派生不虚构）+ A01 与 drillTo 2 项穿透修复闭环 + 强制核验 6/6 全过 + typecheck 136=136 本批 4 文件零命中 + build EXIT=0 独立复跑）；裁决依据 = `remediation-roadmap.md` §5.13（2026-09-04 产品负责人裁决：回退结构、保留修复；解读：待办区保留为页面级增强）；回退基线 = `git show HEAD:frontend/src/router/index.ts` / `HEAD:frontend/src/modules/finance/menu.ts`（HEAD cd1c89b = 回退前原始基线）+ B1 报告 §2.6（RVT-2 前壳状态 21-22 行）；前置限制闭环 = `docs/quality/ui-rvt-b1-qa-report.md` §3.1/§3.2（drillTo 13 处 + A01 穿透——本批应闭环，QA 已确认闭环）；对照基准 = 任务板 §12.2 P1-FIN-RVT-002 卡（回退范围清单 + 保留项清单 + QA 补充追加修复项 + developer 修改证据）；开发者未自行宣布通过 ✅——本报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-RVT-002（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（结构回退，任务池 §12），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；下一步 Batch RVT-3（P1-FIN-RVT-003 记录页回退）随批放行（§12.1 批次节奏，禁止跳步）。
> 基线计数核对：追加前正式基线 **99 条**（文件实况：基线总览表 99 行，含 REG-FIN-RVT-001/004；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 1 条（REG-FIN-RVT-002）后 **100 条**，只增不减纪律达成（既有 99 条条目零修改；WS 基线接管标注保持不删除）；候选 0 条维持。
> WS 基线标注落位（§12.4 方案，只标注不删除，本批仅确认保持）：REG-FIN-WS-002 合并断言已标注「由 REG-FIN-RVT-002 接管」（现状 = 结构已回退、双页签壳已回迁完成）；REG-FIN-WS-003 A01 穿透断言已标注「由 REG-FIN-RVT-002 覆盖」（FinanceFund A01 目标已改回 FinanceLedger）；功能层断言（writeOff/筛选补缺 F-1 防回归点（应付到期日三路径恒 null）/逾期高亮/核销进度列/打印/失败透传）由本基线保留项断言衔接（已核对无冲突）。

### REG-FIN-RVT-002

- **测试编号**：REG-FIN-RVT-002
- **模块**：财务-核销页（frontend/src/views/finance/FinanceReconciliation.vue 删除·壳 + components/ReceivableTab.vue / PayableTab.vue 删除·页签 + FinanceReceivable.vue / FinancePayable.vue 内容回迁独立页 + FinanceFund.vue A01 穿透目标单行 + FinanceReport.vue drillTo 13 处 + router/index.ts / modules/finance/menu.ts 随 RVT-001 全局恢复确认；后端零改动）
- **关联任务**：P1-FIN-RVT-002（Batch RVT-2；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-rvt-b2-qa-report.md`）
- **业务场景**：核销页回退（结构回退，roadmap §5.13 裁决一 + 任务板 §12 P1-FIN-RVT-002，含 QA 补充追加修复项）——
  - **壳/页签删除零残留**：`FinanceReconciliation.vue` 删除（glob `frontend/src/views/finance/**/FinanceReconciliation.vue` 零命中 + QA 独立构建后 dist 无 FinanceReconciliation chunk）；`ReceivableTab.vue` / `PayableTab.vue` 删除（glob `*Tab.vue` 仅剩 LedgerTab/ReimbursementTab/TransferTemplateTab（RVT-003 范围）+ dist 无对应 chunk）；页签结构/el-tabs/tab query 同步逻辑移除（两独立页全文读码零命中）；**emit 链路移除**（grep `defineEmits|@summary-updated|\bemit(` views/finance：FinanceReceivable/FinancePayable 零命中，剩余命中 = RVT-003 壳 + 对话框组件固有 emits 非本批文件）
  - **内容回迁独立页完整**（保留项 8）：`FinanceReceivable.vue` 575 行（developer 证据记 510，见限制登记）——PageHeader :358 + 页面级待办区 :362-373（应收核销待办 + 收款冲正 PD-033 标注）+ 工具栏 :377-388 + 统计卡片 :390-392 + 筛选 :396-422 + 数据区 :424-492 + 新增/收款/收款历史对话框 :494-524；`FinancePayable.vue` 595 行（developer 证据记 527）——PageHeader :362 + 页面级待办区 :365-372 + 工具栏 :376-387 + 统计卡片 :389-391 + 筛选 :396-438 + 数据区 :441-509 + 新增/付款/付款历史对话框 :511-544；全文读码与页签承载功能清单（应收：工具栏/统计 4 卡/筛选/数据区/expand 11 字段/对话框 3 个；应付：工具栏/统计 4 卡/筛选/数据区/expand 13 字段/对话框 3 个）一一对应，内容零丢失；页签结构零残留（无 el-tabs 包裹，内容平铺 modern-page 布局）
  - **保留项 15/15 零回退**（逐项读码/grep 命中）：writeOff 入口+二次确认（FinanceReceivable.vue:272-291 `ElMessageBox.confirm` :276 + `receivableApi.writeOff(id)` :285 + 失败透传 :288-290 + 入口按钮 :471 未结清显示）；应收到期日端到端（:191-194 `params.startDueDate/endDueDate` 由 dueDateRange 派生发送 + 控件 :411-420 value-format=YYYY-MM-DD + handleFilterRestored 回填 :226-232）；应付日期区间端到端（FinancePayable.vue:201-204 `params.startDate/endDate` 端到端发送 + 控件 :411-420）；**应付到期日登记处置零死参数**（:205-206 loadData 注释明示不发送死参数（实际 params 仅 status/supplierName/startDate/endDate）+ 控件 :425-435 disabled + tooltip :421-424 + handleFilterRestored :247 恒 null 不回填旧存档——后端消费链缺失登记后端池，QA F-1 处置保持）；逾期高亮（Receivable :110-112 + :570-574 / Payable :115-117 + :590-594 row-overdue `--fts-status-overdue-bg`）；核销进度列（getProgressPercent receivedAmount/amount 派生 :160-165 + 列 :175 + 展示 :462-464）；打印（handlePrint :301-303 + 按钮 :384）；失败透传（loadFailed + EmptyState error :73/:199-203 + :427-434）；引导空态（hasActiveFilter :76-81 + :435-442 无数据/无匹配区分 + 新增引导）；导出/批量 disabled+tooltip 登记（:381-387 / :380-386，EXPORT-001/P1-FIN-EXPORT-004/PD-035 登记链保持）；CaliberNoteBar（:426/:442）；expand 行内详情（:474-490 11 字段 / :489-507 13 字段）；合计（show-summary :451/:467）/分页（useStandardPage :57/:59 + :453/:469）/密度（density="auto" :449/:465）/固定列（fixed left/right :170/:178 应收、:175/:185 应付）；筛选保存（storage-key finance-receivable-filter / finance-payable-filter 保持 + handleFilterRestored :220-234/:234-249——旧筛选不丢失）
  - **待办区页面级增强**（roadmap §5.13 解读，不虚构计数）：应收核销待办（FinanceReceivable.vue:152-156 receivableSummaryText（loading 中文案 + 真实数据文案，明确「基于当前页真实数据（status/未收金额），分页范围登记」）+ pendingSummary :138-148 真实字段派生（status !== 'settled' 计数、remainAmount 金额、status === 'overdue' 逾期）——**零虚构计数**）；应付核销待办（FinancePayable.vue:157-161 payableSummaryText + pendingSummary :143-153 同口径真实字段派生）；收款冲正标注（PD-033）归应收页（FinanceReceivable.vue:368-371「PD-033 待产品决策：冲正处置方式…决策前不实现不猜测（P5 原则）」——仅标注不实现，合规）
  - **2 项穿透修复闭环**（QA 补充追加修复项 + 回退范围 4）：① FinanceFund.vue:317 `router.push({ name: 'FinanceLedger', query: { voucherId: row.voucherId } })`（B1 报告 §3.2 登记的 :318 FinanceRecords 已改回）+ 注释 :309-314 同步（含 RVT-002 回退登记说明）+ `router/index.ts:116` FinanceLedger 路由已恢复（RVT-1），LedgerTab 穿透接收端 voucherId 定位保持（RVT-003 最终回迁）；② FinanceReport.vue drillTo 13 处——应收 6 处 :128-133 → `/finance/receivable`、应付 6 处 :137-142 → `/finance/payable`、账龄 1 处 :150 → `/finance/receivable`；cost/budget 原独立页引用零改动（:114/:159/:165-168）；handleDrill :428-430 接收 path 参数 + :521 明细按钮；头注释 :23-24 同步（「独立页直达，工作台 tab 参数移除——P1-FIN-RVT-002 结构回退」）；**`/finance/reconciliation?tab=` 全 src grep 零命中** + views/finance `reconciliation` 零命中 + QA 构建后 dist 全部 Finance chunk 字符串扫描零命中；两处穿透按钮目标路由均为已恢复直达路由（/finance/ledger、/finance/receivable、/finance/payable）——B1 报告 §五.1 风险项闭环
  - **强制核验 6/6**：git diff 归因（本批文件清单 = 6 前端文件 + 任务板，RVT-2 自身改动与 developer before/after 逐项对应）/ 后端零改动（本批文件清单无后端文件）/ 视觉同源（views/finance V3-A 零引用，全 var(--fts-*) tokens + core 组件）/ 死参数零发送（应付到期日零死参数 + 应收到期日端到端有效）/ P1-STOCK-001 零触碰 / 构建独立复跑（typecheck 136=136 本批 4 文件零命中 + build EXIT=0 + QA 构建后 dist 复验：FinanceReceivable-CUJdWwdN.js / FinancePayable-q2e_-BLM.js / FinanceFund / FinanceReport 4 chunk 存在，无 FinanceReconciliation chunk）
- **测试目的**：防止核销页结构回退再次漂移 —— FinanceReconciliation 壳 / ReceivableTab/PayableTab 页签不得复活（glob/dist/emit 三重零残留）；内容回迁独立页完整（510+/500+ 行完整页，含 style 块实测 575/595 行）；保留项 15/15 不得回退（含应付到期日零死参数登记处置、导出/批量 disabled+tooltip、筛选保存 storage-key 保持）；待办区页面级增强保持真实字段派生不虚构；A01/drillTo 穿透目标保持已恢复直达路由（不得再指向已移除路由 /finance/reconciliation?tab=*）。
- **测试步骤**：
  1. glob 核验：FinanceReconciliation / ReceivableTab / PayableTab 零命中；FinanceReceivable/FinancePayable 存在
  2. grep 全量：`reconciliation\?tab=`（src）/ `reconciliation`（views/finance）/ `FinanceReconciliation|ReceivableTab|PayableTab`（src）/ `V3-A|v3-a|V3A` / `defineEmits|@summary-updated|\bemit(`——逐项核验零残留
  3. 读码全文：FinanceReceivable.vue（575 行）/ FinancePayable.vue（595 行）——保留项 15/15 逐项命中 + 待办区真实字段派生 + 页签结构零残留 + PD-033 归应收页
  4. 穿透闭环核验：FinanceFund.vue:317 A01 = name FinanceLedger + voucherId；FinanceReport.vue:128-150 drillTo 13 处指向独立页 + 头注释 :23-24 同步
  5. git diff 归因 + 后端零改动核验（本批文件清单无后端文件，与 B1 报告归因一致）
  6. 强制核验独立复跑：typecheck 136=136（本批 4 文件零命中、finance 域全零错误）+ build EXIT=0（QA 独立执行）+ dist 产物核验（4 chunk 存在、无 FinanceReconciliation chunk、字符串扫描零残留）
- **预期结果**：
  - FinanceReconciliation 壳 + ReceivableTab/PayableTab 页签删除零残留（glob/dist/emit 三重）；两独立页完整承载内容（575/595 行），页签结构零残留
  - 保留项 15/15 逐项命中零回退（writeOff 二次确认 / 应收到期日端到端 / 应付到期日登记处置零死参数 / 逾期高亮 / 核销进度列 / 打印 / 失败透传 / 引导空态 / 导出批量登记 / CaliberNoteBar / expand / 合计 / 分页 / 密度 / 固定列 / 筛选保存）
  - 待办区页面级增强保持（pendingSummary 真实字段派生零虚构；PD-033 冲正标注归应收页，仅标注不实现）
  - A01 穿透目标 = FinanceLedger、drillTo 13 处指向独立直达路由、`/finance/reconciliation?tab=` 零残留（穿透恢复可用）
  - typecheck 136=136 + build EXIT=0；dist 无 FinanceReconciliation chunk
- **测试类型**：手工回归（代码级 grep/diff/glob + dist 构建产物核验 + 保留项逐项命中 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 登记项（如实登记，不销号、不猜测处置）**：
  - **【证据微差·登记】独立页行数与 developer 证据不一致**：developer 证据记 FinanceReceivable 510 行 / FinancePayable 527 行；QA 实测 575 / 595 行（含 style 块）——内容完整性已全文实读核验（§2.2/§2.3 逐项命中），行数为统计口径差异（可能未含样式段/尾行），**非功能缺陷**；如实登记待 developer 在后续证据中统一行数口径
  - **【壳生命周期注释·RVT-003 范围】FinanceRecords.vue:132 CSS 注释**提及「同 FinanceReconciliation 样板」——已由 developer 登记（卡内风险/登记项第 1 条），文件属 RVT-003 卡范围，本批不触碰（不扩大范围）；RVT-003 删除壳时一并清理
  - **【既往登记保持】FinanceTax.vue:974 / TransferTemplateTab.vue:6 注释残留**：B1 报告 §3.4 与 developer 证据已登记，非本批文件——保持登记，RVT-003 与产品走查环节处理
  - **【观察项·登记】待办区口径为当前页数据（分页范围）**：pendingSummary 基于当前页 rawRecords（与统计卡片同口径），文案已如实标注「分页范围登记」——不虚构全量计数；是否改为全量口径待产品走查确认（不猜测，保持现状）
  - **【B1 限制消除确认】穿透瞬态（RVT-1 PWL①）本批闭环**：A01 改回 FinanceLedger + drillTo 13 处改指独立直达路由，两处穿透按钮恢复可用——B1 报告 §五.1「RVT-002/003 完成前发布则穿透降级」风险在本批解除

---

### Batch RVT-2 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告 §三（`docs/quality/ui-rvt-b2-qa-report.md`）限制/登记项如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **【证据微差·登记】独立页行数与 developer 证据不一致**（§3.1）：developer 证据记 510/527 行，QA 实测 575/595 行（含 style 块）——定性非功能缺陷（内容完整性已全文实读核验），待 developer 后续证据统一行数口径
2. **【壳生命周期注释·RVT-003 范围】FinanceRecords.vue:132 CSS 注释**（§3.2）：提及「同 FinanceReconciliation 样板」，文件属 RVT-003 卡范围，本批不触碰（不扩大范围），RVT-003 删除壳时一并清理
3. **【既往登记保持】FinanceTax.vue:974 / TransferTemplateTab.vue:6 注释残留**（§3.3）：B1 报告 §3.4 与 developer 证据已登记，非本批文件——RVT-003 与产品走查环节处理
4. **【观察项·登记】待办区口径为当前页数据（分页范围）**（§3.4）：pendingSummary 基于当前页 rawRecords（与统计卡片同口径），文案如实标注「分页范围登记」——不虚构全量计数；是否改全量口径待产品走查确认（不猜测，保持现状）

---

### Batch RVT-2 门禁

```text
Regression Result: PASS（2026-09-05 Batch RVT-2 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为结构回退批次（任务池 §12），非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：1/1 卡 QA 独立验收 PASS（`docs/quality/ui-rvt-b2-qa-report.md`，2026-09-05：P1-FIN-RVT-002 核销页回退 + 2 项穿透修复，无 FAIL、无 -R；壳/页签删除零残留 + 内容回迁独立页完整 + 保留项 15/15 零回退 + 待办区页面级增强（真实字段派生）+ A01/drillTo 穿透闭环 + 强制核验 6/6 全过 + typecheck 136=136 本批 4 文件零命中 + build EXIT=0 独立复跑）
- FAIL 明细：0
- PWL 明细：0（登记/观察项 4 条均为非阻断登记，不构成 FAIL；B1 PWL① 穿透瞬态已本批闭环消除）
- 基线：新增 REG-FIN-RVT-002（正式 99 → 100 条，只增不减、既有条目零修改；WS 基线接管标注保持不删除——REG-FIN-WS-002 合并断言 + REG-FIN-WS-003 A01 断言已由本基线接管覆盖）
- 观察项：行数证据微差 / FinanceRecords.vue:132 注释（RVT-003 范围）/ FinanceTax.vue:974·TransferTemplateTab.vue:6 既往登记保持 / 待办区当前页口径待走查——如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0（登记项不构成 FAIL）→ 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch RVT-3 回归（结构回退批次：P1-FIN-RVT-003 记录页回退，2026-09-05）

> 验收依据：`docs/quality/ui-rvt-b3-qa-report.md`（2026-09-05，QA 独立验收：P1-FIN-RVT-003 记录页回退 = **PASS**——无 FAIL、无 -R；壳/页签删除零残留（glob/dist/emit/路由四重）+ 内容回迁独立页完整 + 保留项 12/12 逐项命中零回退 + 待办区保留为页面级增强（真实字段派生不虚构）+ scaleLevel 折叠回退由菜单档位控制 + 强制核验 6/6 全过 + typecheck 136=136 本批 3 文件零命中 + build EXIT=0 独立复跑）；裁决依据 = `remediation-roadmap.md` §5.13（2026-09-04 产品负责人裁决：回退结构、保留修复；解读：待办区保留为页面级增强）；回退基线 = `git show HEAD:frontend/src/router/index.ts` / `HEAD:frontend/src/modules/finance/menu.ts`（HEAD cd1c89b = 回退前原始基线）+ B1 报告 §2.5（RVT-3 前页签承载清单）+ WS-003 验收报告（`docs/quality/ui-ws-b3-qa-report.md` 页签内容基线）；前置登记项闭环 = `docs/quality/ui-rvt-b2-qa-report.md` §3.2（FinanceRecords.vue:132 CSS 注释残留——本批应随壳删除清理，QA 已确认闭环）；对照基准 = 任务板 §12.2 P1-FIN-RVT-003 卡（回退范围清单 + 保留项 12 清单 + developer 修改证据）；开发者未自行宣布通过 ✅——本报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-RVT-003（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（结构回退，任务池 §12），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**回退专项三批全部完成，等待产品负责人逐页走查（roadmap §5.13：完成后停止，不自动开新批）**。
> 基线计数核对：追加前正式基线 **100 条**（文件实况：基线总览表 100 行，含 REG-FIN-RVT-001/002/004；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 1 条（REG-FIN-RVT-003）后 **101 条**，只增不减纪律达成（既有 100 条条目零修改；WS 基线接管标注保持不删除）；候选 0 条维持。
> WS 基线标注落位（§12.4 方案，只标注不删除，本批确认）：REG-FIN-WS-003 合并断言已标注「由 REG-FIN-RVT-003 接管」（现状 = 结构已回退、三页签壳已回迁完成）；REG-FIN-WS-003 A01 穿透断言已由 REG-FIN-RVT-002 覆盖（FinanceFund A01 目标已改回 FinanceLedger，RVT-2 已闭环）；功能层断言（voucherType 数字分支 F-1 防回归点/voucherStatus 键名/batchPost/红冲作废/联动 CRUD/KL-060·061 登记/E 值格式/CaliberNoteBar/useSummary unit='fen'）由本基线保留项断言衔接（已核对无冲突）。

### REG-FIN-RVT-003

- **测试编号**：REG-FIN-RVT-003
- **模块**：财务-记录页（frontend/src/views/finance/FinanceRecords.vue 删除·壳 + components/LedgerTab.vue / ReimbursementTab.vue / TransferTemplateTab.vue 删除·页签 + FinanceLedger.vue / InvoiceReimbursement.vue / AutoVoucher.vue 内容回迁独立页 + router/index.ts / modules/finance/menu.ts 随 RVT-001 全局恢复确认 + api/finance/voucher.ts / transfer-template.ts / invoice.ts 只读确认零改动；后端零改动）
- **关联任务**：P1-FIN-RVT-003（Batch RVT-3；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-rvt-b3-qa-report.md`）
- **业务场景**：记录页回退（结构回退，roadmap §5.13 裁决一 + 任务板 §12 P1-FIN-RVT-003，最后一批）——
  - **壳/页签删除零残留**：`FinanceRecords.vue` 删除（glob `frontend/src/views/finance/**/FinanceRecords.vue` 零命中 + QA 独立构建后 dist 无 FinanceRecords chunk）；`LedgerTab.vue` / `ReimbursementTab.vue` / `TransferTemplateTab.vue` 删除（glob `frontend/src/views/finance/**/*Tab.vue` 零命中 + dist 无对应 chunk）；**:132 CSS 注释残留清理**（B2 §3.2 登记项闭环——注释随壳文件删除一并消失，全 src grep `FinanceRecords` 剩余 7 处全为登记注释，无样式注释残留）；页签结构/el-tabs/tab query 同步移除（3 独立页全文读码零命中，仅头注释 :6 说明移除；views/finance 剩余 el-tabs = FinanceTax.vue:717-853 自身三页签【WS-004 前既有结构，非记录工作台壳，不在 RVT-003 范围】）；**emit 链路移除**（grep `defineEmits|@summary-updated|\bemit(` views/finance：本批 3 文件零命中，`@summary-updated` 全 views/finance 零命中，剩余命中 21 处全为对话框组件固有 emits 非本批文件）；**路由直达独立组件**（router/index.ts:116 /finance/ledger → FinanceLedger、:123 /finance/invoice-reimbursement → InvoiceReimbursement、:126 /finance/auto-voucher → AutoVoucher——随 RVT-001 恢复，本卡确认零改动）；`/finance/records` 路由零命中（grep router/ 'records' 唯一命中 = /hr/study-records 非财务）；**菜单独立项恢复**（menu.ts:17-18「财务总账」Notebook standard+ / :31-32「发票报销」Money standard+ / :43-44「自动凭证管理」Connection chain-enterprise——原 AutoVoucher 档位保持）
  - **内容回迁独立页完整**（保留项 12）：`FinanceLedger.vue` 785 行（developer 证据记 751，见限制登记）——PageHeader「财务总账」:522 + 页面级待办区 :524-532 + 工具栏 :537-554 + BatchResultFeedback :556 + 统计卡片 :558-560 + 筛选 :564-584 + 数据区 :586-669（CaliberNoteBar :588 + EmptyState :589-596 + DataTable :597-668）+ 新增/详情对话框 :672-700；`InvoiceReimbursement.vue` 660 行（developer 证据记 650）——PageHeader「发票报销」:410 + 页面级待办区 :414-425（报销审批待办 + KL-060 登记卡）+ 工具栏 :429-437 + 统计卡片 :439-441 + 筛选 :445-462 + 数据区 :464-497 + 详情/审批/新增对话框 :499-620；`AutoVoucher.vue` 519 行（developer 证据记 501）——PageHeader「自动凭证管理」:351 + 页面级待办区 :354-361（KL-061 登记卡）+ 工具栏 :365-373 + 统计卡片 :375-377 + 筛选 :380-393 + 数据区 :395-424 + 详情/新建编辑对话框 :426-485；**内容零丢失对照**（与 WS-003 验收页签承载 + B1 §2.5 清单逐项对应）：凭证页签（LedgerTab）→ FinanceLedger（批量过账 :288-315 / voucherStatus·voucherType 筛选 :45-51/:116-124/:562-584 / referenceNo 零发送 :127-141 / expand 分录 + 来源展示 :633-667 / 穿透接收端 voucherId :317-325 / EmptyState :589-596 / 打印 :269-272/:550 / disabled+tooltip 登记 :544-553）；报销页签（ReimbursementTab）→ InvoiceReimbursement（红冲/作废 :200-233/:490-493 / 审批流 :258-277 / E 值格式 :460/:567 / 新增报销 :354-405 / 详情 :288-291/:499-522 / KL-060 登记 :255-257/:420-423/:539）；联动规则页签（TransferTemplateTab）→ AutoVoucher（CRUD :328/:331/:228 / toggleEnabled :208-213 / getLeafSubjects :188/:344-346 / B-3/B-4 筛选映射 :99-106 / KL-061 列移除 :156-157 / 契约对齐 :111-121）——**全部命中**；页签结构零残留（3 页均无 el-tabs 包裹，内容平铺 modern-page 布局）
  - **保留项 12/12 零回退**（逐项读码/grep 命中）：批量过账入口（FinanceLedger.vue:288-315 handleBatchPost 仅 audited 勾选 :289 + `batchPostableCount` :280 同口径 + ElMessageBox.confirm :295-299 + `voucherApi.batchPost(ids)` :300 + BatchResultFeedback 提交数口径 :302-306【total=提交 audited 数 ids.length，后端布尔 true→success=提交数 / false→failed=全部】+ tooltip 明示 N/M 登记 :541）；红冲/作废入口 + 二次确认（InvoiceReimbursement.vue:200-215 handleRedFlush `invoiceApi.redFlush` :207 + confirm :202-206 / :218-233 handleVoid `invoiceApi.void` :225 + confirm :220-224 / 行内按钮仅 issued 态 :490-493；**审批流保留** confirmApprove :258-277（通过=issued/驳回=cancelled 既有语义，KL-060 登记不猜测 :255-257/:539）；API 层 invoice.ts:137-141 void / :149-153 redFlush 既有端点零改动）；联动规则 CRUD（AutoVoucher.vue transferTemplateApi.create :331 / update :328 / delete :228 + 二次确认 :223-227 / toggleEnabled handleToggleEnabled :208-213 传 enabled 取反 :212 / getLeafSubjects :188 + 科目选择器 :459-467，映射缺失回退 科目#ID :81-82 不虚构；契约对齐 TransferTemplateVO templateId :112 / isEnabled :119 / templateType 数字 :114 / sourceSubjectId·targetSubjectId :115 / amountExpression·summaryTemplate :116-117）；B-3/B-4 映射（transfer-template.ts:28-41 mapQueryParams enabled→isEnabled :34-36、templateName→keyword :37-39——端到端有效（AutoVoucher loadData :99-106 发送 templateName/enabled），本批未触碰 api 层）；voucherStatus 键名 + voucherType 数字分支（voucher.ts:34-47 result.voucherStatus = VoucherStatusMap.toBackend[status] :41 + voucherType 数字直传分支 :43-45 `typeof voucherType === 'string' ? VoucherTypeMap.toBackend[voucherType] : voucherType`——**与 WS-003-R1 修复逐字符一致**；FinanceLedger 发送数字 7 值语义 :47-48/:116-124/:580）；referenceNo 死参数零发送（FinanceLedger.vue:127-141 loadData params 仅 4 维 voucherNo/voucherType/status/startDate/endDate——referenceNo 零发送；展示透传 expand :640 / 详情 :693 保持；后端池登记链维持头注释 :15）；凭证筛选 4 维（:130-140 voucherNo / voucherType 数字直传 / status【voucherStatus 键名】/ dateRange→startDate·endDate 端到端有效 + 控件 :572-582）；expand 分录 + 来源展示 + 穿透接收端（:633-667 #expand 分录明细 DataTable :650-655 + 借方/贷方合计 :656-665 + 来源单据展示 referenceNo/sourceType/sourceId【A01/C02 穿透键】:637-648，穿透跳转登记不伪造 :644-647；穿透接收端 onMounted :317-325 route.query.voucherId :321 → fetchAndShowDetail :463-472 复用 voucherApi.getById 零新增接口）；失败透传 + 引导空态（3 页统一 loadFailed + EmptyState error + 重试 action——FinanceLedger :54/:162-168/:589-596、InvoiceReimbursement :50/:150-154/:466-473、AutoVoucher :46/:123-129/:397-404）；KL-060 登记不猜测 + KL-061 列移除（InvoiceReimbursement.vue:255-257 驳回=作废语义混叠无依据→登记不实现 + 待办区登记卡 :420-423 + 审批对话框 kl060-note :539；AutoVoucher.vue:156-157 「最近执行」伪语义列移除→「创建时间」真实字段 createTime）；既有登记处置保持（FinanceLedger 批量审核 :544-546 / 导出 :547-549 / 凭证导入 :551-553 disabled+tooltip 明示后端池 + EXPORT-001/P1-FIN-EXPORT-004/PD-035 登记链；InvoiceReimbursement 导出 :433-435；AutoVoucher 导出 :369-371；batch-post 布尔无 N/M 明细登记 :301/:541；凭证类型筛选两套语义差异登记 :114-115 注释）；E 值格式修复（InvoiceReimbursement.vue:460 筛选 daterange + :567 新增表单 date value-format="YYYY-MM-DD" 2 处 + toDateString/restoreDateRange 归一 :165-181）；口径标注（FinanceLedger.vue:588 CaliberNoteBar 与 WS-003 基线 LedgerTab.vue:578 一处对应——零丢失零新增）；useSummary 账本口径（FinanceLedger.vue:221 createAmountSummaryMethod({ amountProps: ['debitTotal', 'creditTotal'], unit: 'fen' })——unit='fen' 账本口径保持，注释 :216-220 明示 Batch0-方案2 范围外保持现状）
  - **待办区页面级增强**（roadmap §5.13 解读，不虚构计数）：凭证待办（FinanceLedger.vue:198-204 ledgerSummaryText loading 中文案 :200 + 真实数据文案「基于当前页真实数据（凭证状态），分页范围登记」:201 + pendingSummary :188-194 真实字段派生【待审核 = draft 数、待过账 = audited 数】——与 WS-003 壳待办语义逐项一致，继承非发明）；报销审批待办（InvoiceReimbursement.vue:114-120 reimbursementSummaryText + pendingSummary :108-112 真实字段派生【待审批 = draft 数】）；KL-060 登记卡（InvoiceReimbursement.vue:420-423 P2 登记不实现不虚构）；KL-061 登记卡（AutoVoucher.vue:354-361 P2 登记不实现不虚构）；**零虚构计数核验**（三待办计数全部派生自 records.value 当前页真实数据，文案显式标注「分页范围登记」；未加载显示「加载中…」而非 0）
  - **scaleLevel 折叠回退**（确认⑤）：canSeeTransferTemplate 折叠逻辑移除（grep 全 src 零命中）；AutoVoucher 独立页无条件渲染（无折叠/条件渲染逻辑，头注释 :11-13 说明折叠属壳结构随 FinanceRecords 删除）；可见性由菜单档位控制（menu.ts:43-44 自动凭证管理 = chain-enterprise 档位 = 原 AutoVoucher 档位，随 RVT-001 恢复，RVT-001 证据 standard+ 9 / chain-standard+ 3 / chain-enterprise 1 核对一致）
  - **强制核验 6/6**：git diff 归因（本批文件清单 = 5 前端文件 + 任务板，RVT-3 自身改动与 developer before/after 逐项对应【B1 §2.5 为 RVT-3 前页签状态参照；menu.ts diff = 2 行注释 = RVT-001 登记行】）/ 后端零改动 + API 层零改动（本批文件清单无后端文件；voucher.ts voucherStatus 键名 :41 + voucherType 数字分支 :43-45 + batchPost :185-190、transfer-template.ts B-3/B-4 :34-39 + toggleEnabled :116-122、invoice.ts void :137-141 / redFlush :149-153 = WS-003 已验收修复完整保持，本批未触碰 api 层）/ 视觉同源（views/finance V3-A 零引用，全 var(--fts-*) tokens + core 组件 PageHeader/StatCard/DataTable/SearchPanel/StatusTag/EmptyState/CaliberNoteBar/BatchResultFeedback）/ 死参数零发送（referenceNo 零发送 + batch-post 布尔口径登记不伪造 N/M :301/:541）/ P1-STOCK-001 零触碰 / 构建独立复跑（typecheck 136=136 本批 3 文件零命中 + views/finance + api/finance 全域零错误 + build EXIT=0 + QA 构建后 dist 复验：FinanceLedger-Cc0W4dvS.js / InvoiceReimbursement-kGhi1vLa.js / AutoVoucher-A82cb1wn.js 3 chunk 存在 + FinanceRecords/LedgerTab/ReimbursementTab/TransferTemplateTab chunk 零命中 + dist/assets 全部 js 字符串扫描 = ZERO）
- **测试目的**：防止记录页结构回退再次漂移 —— FinanceRecords 壳 / LedgerTab/ReimbursementTab/TransferTemplateTab 页签不得复活（glob/dist/emit/路由四重零残留）；内容回迁独立页完整（500+/650+ 行完整页，含 style 块实测 785/660/519 行）；保留项 12/12 不得回退（含 WS-003-R1 修复 voucherType 数字分支逐字符保持、referenceNo 零发送、KL-060/061 登记）；scaleLevel 折叠不得复活（菜单档位控制）；待办区页面级增强保持真实字段派生不虚构。
- **测试步骤**：
  1. glob 核验：FinanceRecords / `*Tab.vue`（LedgerTab/ReimbursementTab/TransferTemplateTab）零命中；3 独立页存在
  2. grep 全量：`/finance/records`（router）/ `FinanceRecords|LedgerTab|ReimbursementTab|TransferTemplateTab`（src，7 处全为注释）/ `canSeeTransferTemplate`（全 src）/ `defineEmits|@summary-updated|\bemit(`（views/finance）/ `V3-A|v3-a|V3A`（views/finance）/ `query.tab|activeTab|tab=`（views/finance）——逐项核验零残留
  3. 读码全文：FinanceLedger.vue（785 行）/ InvoiceReimbursement.vue（660 行）/ AutoVoucher.vue（519 行）/ voucher.ts（193 行）/ transfer-template.ts（125 行）/ router 财务段（:112-128）/ menu.ts（全 46 行）——保留项 12/12 逐项命中 + 待办区真实字段派生 + 页签结构零残留；对照 WS-003 验收报告页签承载清单 + B1 §2.5 逐项对应
  4. API 层核验：voucher.ts mapQueryParams（voucherStatus 键名 + voucherType 数字分支 = WS-003-R1 修复逐字符一致）、transfer-template.ts B-3/B-4 映射、invoice.ts void/redFlush 既有端点——零改动确认
  5. git diff 归因 + 后端零改动核验（本批文件清单无后端文件；后端/stock 域 M 态归因既往批次）
  6. 强制核验独立复跑：typecheck 136=136（本批 3 文件零命中、finance 域全零错误）+ build EXIT=0（QA 独立执行）+ dist 产物核验（3 chunk 存在、删除文件名 chunk 零命中、字符串扫描 ZERO）
- **预期结果**：
  - FinanceRecords 壳 + LedgerTab/ReimbursementTab/TransferTemplateTab 页签删除零残留（glob/dist/emit/路由四重）；3 独立页完整承载内容（785/660/519 行），页签结构零残留，:132 注释随壳清理闭环
  - 保留项 12/12 逐项命中零回退（batchPost 仅 audited + BatchResultFeedback 提交数口径 / 红冲作废+审批流保留 / 联动 CRUD+B-3-B4 映射 / voucherStatus+voucherType 数字分支【WS-003-R1 逐字符一致】/ referenceNo 零发送 / 筛选 4 维 / expand+穿透接收端 / 失败透传 / KL-060·061 登记 / E 值格式 / CaliberNoteBar / useSummary unit='fen'）
  - 待办区页面级增强保持（pendingSummary 真实字段派生零虚构；KL-060/061 登记卡仅登记不实现）
  - scaleLevel 折叠逻辑零残留（grep 零命中）；可见性由菜单档位控制（menu.ts:43-44 chain-enterprise）
  - typecheck 136=136 + build EXIT=0；dist 3 chunk 存在、删除文件名 chunk 零命中、字符串扫描 ZERO
- **测试类型**：手工回归（代码级 grep/diff/glob + dist 构建产物核验 + 保留项逐项命中 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 登记项（如实登记，不销号、不猜测处置）**：
  - **【证据微差·登记】独立页行数与 developer 证据不一致**：developer 证据记 FinanceLedger 751 行 / InvoiceReimbursement 650 行 / AutoVoucher 501 行；QA 实测 785 / 660 / 519 行（含 style 块）——内容完整性已全文实读核验（§2.2/§2.3 逐项命中），行数为统计口径差异（B2 同型 developer 510/527 vs QA 575/595 已登记），**非功能缺陷**；如实登记待 developer 在后续证据中统一行数口径
  - **【注释残留·登记】FinanceRecords/LedgerTab/ReimbursementTab/TransferTemplateTab 名称残留 7 处全为注释**：3 处回迁登记注释（FinanceLedger.vue:6 / InvoiceReimbursement.vue:6 / AutoVoucher.vue:6,12——本批文件头注释，说明回迁来源，符合「回迁登记」惯例）+ 4 处既往备查项（FinanceTax.vue:974「同 FinanceFund/FinanceRecords 样板」+ FinanceFund.vue:311/:313「由 FinanceRecords 改回」「见 LedgerTab.vue」——非本批文件，B1 §3.4/RVT-002 已登记保持，待产品走查）；零功能引用，不构成验收失败
  - **【观察项·登记】FinanceTax el-tabs 为自身页结构**：FinanceTax.vue:717-853 el-tabs 三页签（税务记录/纳税申报/税率配置）= WS-004 前既有页面结构，**不属于 RVT-003 范围**（RVT-003 壳 = FinanceRecords 三页签）；不误判为残留
  - **【观察项·登记】api/finance/record.ts 为孤立 API 文件（零消费方）**：grep `api/finance/record`（src）零消费方；文件映射后端 `/v1/finance/records` 既有端点（后端仍存在），非前端壳文件，**不在 RVT-003 卡文件清单**（卡范围 = FinanceRecords.vue 壳 + 3 页签 + 3 独立页 + router/menu 确认）——登记不判 FAIL，是否清理由产品/后续批次定夺
  - **【观察项·登记】待办区口径为当前页数据（分页范围）**：三待办 pendingSummary 基于当前页 records（与统计卡片同口径），文案已如实标注「分页范围登记」——不虚构全量计数；是否改为全量口径待产品走查确认（同 B2 §3.4，保持现状不猜测）

---

### Batch RVT-3 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告 §三（`docs/quality/ui-rvt-b3-qa-report.md`）限制/登记项如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **【证据微差·登记】独立页行数与 developer 证据不一致**（§3.1）：developer 证据记 751/650/501 行，QA 实测 785/660/519 行（含 style 块）——定性非功能缺陷（内容完整性已全文实读核验），待 developer 后续证据统一行数口径（同 B2 §3.1 模式）
2. **【注释残留·登记】名称残留 7 处全为注释**（§3.2）：3 处回迁登记注释（FinanceLedger:6 / InvoiceReimbursement:6 / AutoVoucher:6,12）+ 4 处既往备查项（FinanceTax:974「同 FinanceFund/FinanceRecords 样板」/ FinanceFund:311,:313「由 FinanceRecords 改回」「见 LedgerTab.vue」）——零功能引用，不构成验收失败；备查项待产品走查
3. **【观察项·登记】FinanceTax el-tabs 为自身页结构**（§3.3）：FinanceTax.vue:717-853 = WS-004 前既有页面结构，不属于 RVT-003 范围，不误判为残留
4. **【观察项·登记】record.ts 孤立 API 文件**（§3.4）：api/finance/record.ts 零消费方、映射后端既有端点，不在 RVT-003 卡文件清单——登记不判 FAIL，是否清理由产品/后续批次定夺
5. **【观察项·登记】待办区当前页口径**（§3.5）：pendingSummary 基于当前页 records（分页范围），文案如实标注，不虚构全量计数；是否改全量口径待产品走查确认（同 B2 §3.4，保持现状不猜测）

---

### Batch RVT-3 门禁

```text
Regression Result: PASS（2026-09-05 Batch RVT-3 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为结构回退批次（任务池 §12），非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：1/1 卡 QA 独立验收 PASS（`docs/quality/ui-rvt-b3-qa-report.md`，2026-09-05：P1-FIN-RVT-003 记录页回退，无 FAIL、无 -R；壳/页签删除零残留 + 内容回迁独立页完整 + 保留项 12/12 零回退 + 待办区页面级增强（真实字段派生）+ scaleLevel 折叠移除由菜单档位控制 + 强制核验 6/6 全过 + typecheck 136=136 本批 3 文件零命中 + build EXIT=0 独立复跑）
- FAIL 明细：0
- PWL 明细：0（登记/观察项 5 条均为非阻断登记，不构成 FAIL；B2 §3.2 FinanceRecords.vue:132 注释残留登记项本批随壳删除闭环）
- 基线：新增 REG-FIN-RVT-003（正式 100 → 101 条，只增不减、既有条目零修改；WS 基线接管标注保持不删除——REG-FIN-WS-003 合并断言已由本基线接管覆盖）
- 回退专项收口核对：RVT 四卡完整性核对 = **REG-FIN-RVT-001/002/003/004 全部在案**（Batch RVT-1：001+004 / Batch RVT-2：002 / Batch RVT-3：003）；专项累计 97 → 101 = 4 条回退基线，只增不减、既有 100 条零修改；WS 基线 10 条标注接管不删除；**等待产品负责人逐页走查**（roadmap §5.13 硬约束，完成后停止，不自动开新批）
- 观察项：行数证据微差 / 注释残留 7 处备查（3 回迁登记 + 4 既往备查）/ FinanceTax el-tabs 范围外 / record.ts 孤立 API 文件 / 待办区当前页口径待走查——如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0（登记项不构成 FAIL）→ 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

---

## Batch LRVT-1 回归（布局级回退批次：P1-FIN-LRVT-001 + 002 穿透成对样板批，2026-09-05）

> 验收依据：`docs/quality/ui-lrvt-b1-qa-report.md`（2026-09-05，QA 独立验收：P1-FIN-LRVT-001 资金管理 = **PASS** / P1-FIN-LRVT-002 财务总账 = **PASS**——无 FAIL、无 -R；布局与 HEAD 逐块比对同构 + 重放修复点 B1~B6/B1~B7 全部落位有效 + 穿透成对不断链 + 能力层保留 + 登记类入口不失效 + 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）；裁决依据 = `remediation-roadmap.md` §5.14（2026-09-05 产品负责人指令：9 页 A+B 全部回退原布局 + 重放修复 + 重放处置 6 项已定；穿透成对同批处理）；回退基线 = `git show HEAD:frontend/src/views/finance/FinanceFund.vue` / `HEAD:frontend/src/views/finance/FinanceLedger.vue`（HEAD cd1c89b = 回退前原始基线）+ 盘点报告 `docs/quality/finance-layout-revert-inventory-20260905.md`（§2.1/§2.2 before 结构）；对照基准 = 任务板 §13.4 P1-FIN-LRVT-001/002 卡（统一回退范围 7 项 + 重放修复清单 + 统一验收标准 6 条）+ §13.4.1 developer 修改证据；开发者未自行宣布通过 ✅——本报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-LRVT-001/002（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（布局级回退，任务池 §13），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**布局回退专项 Batch LRVT-1~4 逐批执行（本批 = 穿透成对样板批），全部完成后停止，等待产品负责人逐页走查（roadmap §5.14，不自动开新批）**。
> 基线计数核对：追加前正式基线 **101 条**（文件实况：基线总览表 101 行，含 REG-FIN-RVT-001~004；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 2 条（REG-FIN-LRVT-001/002）后 **103 条**，只增不减纪律达成（既有 101 条条目零删除零修改）；候选 0 条维持。
> 断言衔接登记（任务板 §13.4「回归基线衔接」原则，本批按 101 只增不减零修改执行，不删除不覆盖）：布局级回退使既往条目部分断言过时——REG-UI-FIN-001「SearchPanel 组件化接入 + expand 行内详情」断言（Fund 已恢复手写筛选区 + 详情 el-dialog 回迁）、REG-UI-FIN-002「expand 分录」断言（Ledger 分录回迁详情对话框）、REG-FIN-WS-001「对账工作台五区布局（待办区/工具栏）」断言（待办区/工具栏随布局回退移除）——现状由本批 REG-FIN-LRVT-001/002 新增断言覆盖（布局同构 + 重放修复点 + 能力层保留）；原条目录内标注留待后续批次/architect 按 §13.4 统一落位（本批零修改纪律优先）。

### REG-FIN-LRVT-001

- **测试编号**：REG-FIN-LRVT-001
- **模块**：财务-资金管理（frontend/src/views/finance/FinanceFund.vue，688 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-001（Batch LRVT-1；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b1-qa-report.md`）
- **业务场景**：资金管理布局级回退（布局回退专项·穿透成对样板批 #1，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/②/③）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（含 #extra :505-516）→ stats-section（统计卡片回顶层 :517-520，与 HEAD :221-223 同构）→ advanced-search-panel（手写 div 恢复 :521-539，与 HEAD :224-235 同构，SearchPanel 组件化容器移除）→ table-section（:540-595，与 HEAD :237-246 同构；追加 B6 CaliberNoteBar :542 + B2 EmptyState :544-560 + B4 来源列，均为 B 修复点嵌入）→ pagination-wrapper（独立分页恢复 :596-599，与 HEAD :248-250 同构，分页不并入 DataTable）→ 详情 el-dialog（:600-621，与 HEAD :252-269 同构；处置③ expand 行内详情回迁，10 个 descriptions 项顺序一致）→ **末尾追加 B3 新建流水 el-dialog（:623-659 = B 重放承载，非布局重排）**
  - **已移除确认（grep 扫描）**：`workbench-taskboard` / `workbench-toolbar` / `SearchPanel` / `#expand` / `expandable` 仅头部/行内注释提及（:9-10/:17/:168/:179/:191/:200/:496/:600/:665），模板与样式零命中；`.workbench-taskboard/.workbench-toolbar` 样式已删（:664-686 仅保留 `.source-cell` 随 B4 重放）
  - **重放修复点 B1~B6 全有效**：B1 E 值格式（:140-148 toDateString / :151-157 restoreDateRange / :159-166 handleFilterRestored 旧存档兼容 / :532 date-picker `value-format="YYYY-MM-DD"`——日期字符串化，嵌入原筛选区控件）；B2 失败透传（:47 loadFailed / :272-288 catch→loadFailed+ElMessage.error / :291-293 handleRetry / :544-551 EmptyState type="error" action-text="重试"）；B3 新建流水入口（:508 PageHeader#extra 按钮 / :459-462 openCreateDialog / :465-492 handleCreateFlowSubmit / **fundFlowApi.create 既有端点** fund-flow.ts:100-104 POST /v1/finance/fund-flows / 金额元→分由 FundFlowDataConverter.toCreateDTO 统一处理 converters.ts:801-873 / :624-659 弹层）；B4 A01 来源展示+穿透（:123 source 列 / :370-377 getSourceDocNo【voucherNo→remark 首子句兜底→'-'】/ :380-383 getFlowCategoryLabel（FundFlowCategoryMap 1~7 纯展示）/ **:390-393 handleJumpToVoucher → router.push({name:'FinanceLedger', query:{voucherId}})** / :573-587 source cell + 「查看凭证」link（v-if row.voucherId）——路由 name 'FinanceLedger' 存在 router/index.ts:116）；B5 筛选持久化（:169 FILTER_STORAGE_KEY='fts_search_finance-fund-filter' / :171-198 saveFilter/restoreSavedFilter/clearSavedFilter / :201 watch deep / :497 onMounted 先恢复后加载——同 SearchPanel 前缀约定，查询即保存、重置即清空）；B6 口径标注（:542 CaliberNoteBar 嵌入表格上方，PD-031/032 文案，纯展示零数据依赖）
  - **穿透成对不断链**：发送端 Fund :390-393/:583 → /finance/ledger?voucherId=xxx ↔ 接收端 Ledger :547-552（onMounted route.query.voucherId → fetchAndShowDetail :493-501 → voucherApi.getById 既有端点 + 详情弹层，与「查看」按钮同路径，零新增接口）——**同批成对**
  - **能力层保留（处置⑥ 不回退）**：density="auto" :567 / show-summary :568 + summaryMethod = createAmountSummaryMethod({amountProps:['income','expense'], unit:'yuan'}) :133 / 固定列 :120（日期 left）/ :122（摘要 left）/ :126（余额 right）；能力组件（DataTable/EmptyState/useSummary/CaliberNoteBar）零改动
  - **登记类入口不失效**：:509-511 导出 disabled+tooltip（P1-FIN-EXPORT-001 登记：资金流水无导出端点，不伪造）；:512-514 日结 disabled+tooltip（PD-028 依赖登记）——均挂 PageHeader#extra 承载
  - **强制核验**：布局同构（git diff = B1~B6 修复点嵌入 + 头注释 + B3 弹层末尾追加，+484/-14）/ 后端零改动（仅调用既有 api 方法：fundFlowApi.create / bankAccountApi.getList / voucherApi.getList·getById·batchPost·approve·post·unapprove·unpost·void；LRVT 标记仅本批两文件，全仓 grep 6 命中）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 穿透成对 / typecheck 136=136 零新增（两文件零错误）+ build EXIT=0 / 批隔离（P1-STOCK-001 零触碰）
- **测试目的**：防止资金管理布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→详情 el-dialog→B3 弹层）；待办区/工具栏/SearchPanel/#expand 不得复活（模板样式零残留）；重放修复点 B1~B6 不得回退（E 值格式/失败透传/新建流水既有端点/来源+穿透/筛选持久化/口径标注）；能力层（density/show-summary unit='yuan'/固定列）不回退；Fund↔Ledger 穿透成对不断链；登记类入口 disabled+tooltip 不失效。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/FinanceFund.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→详情 el-dialog→B3 弹层末尾追加）
  2. grep 扫描：`workbench-taskboard` / `workbench-toolbar` / `SearchPanel` / `#expand` / `expandable` / `V3-A`——仅注释提及、模板/样式零命中；`.workbench-taskboard/.workbench-toolbar` 样式删除确认
  3. 逐修复点核验 B1~B6 存在 + 消费链闭合（E 值格式：toDateString/restoreDateRange/value-format；新建流水：入口→fundFlowApi.create 既有端点→toCreateDTO 金额元→分；穿透：发送端 :390-393 → 路由 name 'FinanceLedger' 存在 → 接收端 Ledger :547-552 voucherId → voucherApi.getById 既有端点）
  4. 能力层核验（DataTable density/show-summary/summary-method/fixed 透传 + useSummary unit='yuan'）；登记类入口（导出/日结 disabled+tooltip 挂 #extra）
  5. 强制核验独立复跑：typecheck 136=136（FinanceFund.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + B3 弹层末尾追加）；待办区/工具栏/SearchPanel/#expand 模板样式零残留（仅注释）
  - 重放修复点 B1~B6 全部落位有效（E 值格式字符串化 / 失败透传显式失败态+重试 / 新建流水入口可点+既有端点+元→分统一 / 来源列展示+「查看凭证」穿透可达 / 筛选持久化 fts_search_ 前缀语义等价 / CaliberNoteBar 口径标注）
  - 能力层保留（density="auto" / show-summary unit='yuan' / 固定列）；登记类入口 disabled+tooltip 挂 #extra 不失效
  - 穿透成对不断链（Fund 发送端 ↔ Ledger 接收端同批成对）
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】存储键命名：卡文 storage-key="finance-fund-filter" vs 实码 `fts_search_finance-fund-filter`——按 SearchPanel 前缀约定（`fts_search_`+key）语义等价，代码注释已注明，无功能影响（QA §1.6-1）
  - 【观察项·登记】无浏览器活体：本环境无浏览器/接口运行条件，交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-001）与产品逐页走查承接（QA §1.6-2）
  - 【证据微差·登记】developer 证据行号少量漂移（如 B4 :575-584 vs 实码 :573-587）：内容全部核验存在，不影响结论（QA §1.6-3）

### REG-FIN-LRVT-002

- **测试编号**：REG-FIN-LRVT-002
- **模块**：财务-财务总账（frontend/src/views/finance/FinanceLedger.vue，773 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-002（Batch LRVT-1；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b1-qa-report.md`）
- **业务场景**：财务总账布局级回退（布局回退专项·穿透成对样板批 #2，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/②/③）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（#extra 新增凭证回原位置 :558-579，与 HEAD :323-329 同构；批量过账/打印/批量审核 disabled/导出 disabled/凭证导入 disabled = B 承载，非布局重排）→ stats-section（:580-582，与 HEAD :330-332 同构）→ advanced-search-panel（手写 div 恢复 :583-605，与 HEAD :333-342 同构；1 维→4 维控件嵌入原容器，B1）→ table-section（:606-653，与 HEAD :344-363 同构；追加 B2 BatchResultFeedback :608 + CaliberNoteBar :610 + B5 EmptyState :612-618，均为 B 修复点嵌入）→ pagination-wrapper（独立分页恢复 :654-657，与 HEAD :365-367 同构）→ VoucherFormDialog（:659-664，与 HEAD :369-375 同构，组件零改动）→ 详情 el-dialog（:666-721，与 HEAD :377-420 同构；处置③ 分录明细 el-table 回迁 + B4 来源单据展示并入）
  - **已移除确认（grep 扫描）**：`workbench-taskboard` / `workbench-toolbar` / `ledger-page` 容器 / `SearchPanel` / `#expand` / `expandable` 仅注释提及（:9-12/:22/:26/:245/:256/:268/:277/:467/:543/:666/:692），模板与样式零命中；`.workbench-taskboard/.task-item` 样式已删（:725-772 仅保留 `.detail-entries` + `.detail-source__note` 随 B4 重放，视觉同源 var(--fts-*)）
  - **重放修复点 B1~B7 全有效**：B1 筛选真实化（4 维）（:51-57 searchForm【voucherNo/voucherType/status/dateRange】/ :123-131 voucherTypeOptions 后端 7 值数字直传 / :134-180 loadData【:141 voucherNo、:142 voucherType 数字分支、:143 status、:144-147 期间】/ :588-598 4 维控件嵌入原筛选区 / voucher.ts:34-47 mapQueryParams【:40-42 `result.voucherStatus = VoucherStatusMap.toBackend[status]` 键名修正】/ VoucherStatusMap 0-3 converters.ts:117-130——端到端消费链闭合，VoucherQueryForm 类型含全部 4 维 types/finance.ts:250-260）；B2 批量过账入口（:313-315 handleSelectionChange / :318 batchPostableCount（仅 audited 可过账）/ :326-353 handleBatchPost【:338 voucherApi.batchPost(ids) = voucher.ts:185-190 **既有端点** POST /v1/finance/auto-vouchers/vouchers/batch-post；提交前 audited 过滤 :327；确认框 :333-337】/ :566 #extra 入口 `:disabled="batchPostableCount===0"` / :608 BatchResultFeedback（null 隐藏，BatchResultFeedback.vue:62）——N/M 反馈 = **提交数口径**（total=ids.length、success=ok?ids.length:0，后端仅返回布尔，精确明细已登记后端池，未伪造））；B3 打印（:308-310 handlePrint → window.print / :574 #extra 打印按钮——浏览器原生打印，零接口依赖）；B4 A01 来源展示+穿透（:82-86 VoucherDisplay 穿透键字段 / :165-167 透传映射 referenceNo/sourceType/sourceId / :537-540 getSourceTypeLabel（VoucherSourceTypeMap 8 值 converters.ts:151-162）/ :684-685 详情对话框来源单据号/来源类型 / :687-690 穿透说明登记 GAP-B2/BE01（现有业务单据页无深链，不伪造跳转）/ :547-552 onMounted route.query.voucherId 接收端）；B5 失败透传（:60 loadFailed / :170-176 catch→loadFailed+ElMessage / :612-618 EmptyState error 重试）；B6 筛选持久化（:246 FILTER_STORAGE_KEY='fts_search_finance-ledger-filter' / :248-275 saveFilter/restoreSavedFilter/clearSavedFilter / :278 watch deep / :543-545 先恢复后加载；handleFilterRestored :234-243 仅回填 4 维表单字段，不发明规则）；B7 KL-061 登记（:23-24 头部注释——纯登记不猜测业务规则；联动规则功能属 AutoVoucher 页 LRVT-008 承接）
  - **处置③ 详情回迁（分录明细）**：detailEntryColumns :520-526 = **HEAD 原列结构**（科目编码/科目名称/摘要/借方(元)/贷方(元) 5 字段，与 HEAD :418-425 完全一致）；分录 el-table :695-705（v-for 渲染列）+ 借贷合计 :706-715（借方合计/贷方合计 ¥）；displayRecords 转换保留（:153-168 映射，entries 透传 :163 + showDetail :505-517 分录 5 字段 + formatAmount 格式化）；expand 已移除（grep `#expand`/`expandable` 模板零命中，仅注释）
  - **能力层保留（处置⑥ 不回退）**：density="auto" :626 / selectable :627（批量过账勾选，DataTable.vue:30,221）/ show-summary :628 + summaryMethod = createAmountSummaryMethod({amountProps:['debitTotal','creditTotal'], unit:'fen'}) :212（**unit='fen' 账本口径保持**）/ 固定列 :197（凭证编号 left）/ :204（状态 right）
  - **金额口径零变更**：formatAmount :98-100 = `fenToYuanNumber(fen).toLocaleString`（÷100 展示路径）与 HEAD :48-50 **完全相同**；行展示 :634-635 与 HEAD 展示路径逐位一致；合计行 unit='fen'（:212, :629）与行展示同口径（summarizeAmount unit='fen' → fenToYuanDisplay，useSummary.ts:55-58；utils/money.ts:44-47/98-104 同源函数）——**行展示与合计数值一致**；分录 Converter 元数值 + formatAmount ÷100 展示 = HEAD 行为（converters.ts:537-545 注释明确）；绝对值展示语义属 Batch0-方案2 既有治理范围（观察项登记，非本批引入）
  - **穿透成对不断链**：接收端 Ledger :547-552（onMounted route.query.voucherId → fetchAndShowDetail :493-501 → voucherApi.getById 既有端点 + 详情弹层，与「查看」按钮同路径，零新增接口）↔ 发送端 Fund :390-393/:583 ——**同批成对**
  - **强制核验**：布局同构（git diff = B1~B7 修复点嵌入 + 头注释 + #extra 入口扩展，+394/-30）/ 后端零改动（仅调用既有 api 方法：voucherApi.getList·getById·batchPost·approve·post·unapprove·unpost·void；LRVT 标记仅本批两文件）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 穿透成对 / typecheck 136=136 零新增（两文件零错误）+ build EXIT=0 / 批隔离（P1-STOCK-001 零触碰）
- **测试目的**：防止财务总账布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→VoucherFormDialog→详情 el-dialog）；待办区/工具栏/#expand 不得复活（模板样式零残留）；重放修复点 B1~B7 不得回退（筛选 4 维端到端【voucherStatus 键名/voucherType 数字分支】/批量过账【batchPost+audited 过滤+提交数口径】/打印/穿透键+接收端/失败透传/持久化/KL-061 登记）；详情分录 el-table 回迁（HEAD 原 5 列+displayRecords 转换）保持；能力层（density/selectable/show-summary unit='fen'/固定列）不回退；金额口径与 HEAD 零变更；穿透成对不断链。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/FinanceLedger.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→VoucherFormDialog→详情 el-dialog）
  2. grep 扫描：`workbench-taskboard` / `workbench-toolbar` / `ledger-page` / `SearchPanel` / `#expand` / `expandable` / `V3-A`——仅注释提及、模板/样式零命中；`.workbench-taskboard/.task-item` 样式删除确认
  3. 逐修复点核验 B1~B7 存在 + 消费链闭合（筛选 4 维：控件→params→mapQueryParams（voucherStatus 键名 + voucherType 数字分支）→DTO；批量过账：仅 audited → batchPost 既有端点 → BatchResultFeedback 提交数口径；穿透：发送端 Fund:390-393 → 路由 → 接收端 :547-552 voucherId → voucherApi.getById）
  4. 处置③ 核验：detailEntryColumns 与 HEAD 原 5 列一致 + 借贷合计 + displayRecords 转换保留；expand 模板零命中
  5. 能力层核验（density/selectable/show-summary unit='fen'/固定列）；金额口径核验（formatAmount 与 HEAD 逐位一致 + 行/合计同源函数一致）
  6. 强制核验独立复跑：typecheck 136=136（FinanceLedger.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + #extra 入口扩展）；待办区/工具栏/#expand 模板样式零残留（仅注释）
  - 重放修复点 B1~B7 全部落位有效（筛选 4 维端到端 / 批量过账真实接入+提交数口径 / 打印 / 穿透键+接收端 / 失败透传 / 持久化 fts_search_ / KL-061 登记）
  - 分录 el-table 回迁（HEAD 原 5 列 + 借贷合计 + displayRecords 转换保留）；能力层保留（unit='fen' 账本口径）；金额口径与 HEAD 零变更、行/合计一致
  - 穿透成对不断链（Ledger 接收端 ↔ Fund 发送端同批成对）
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 金额口径核验 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】批量过账反馈口径：N/M = 提交数口径（后端 `batchPost` 仅返回布尔，无精确成功明细）——已登记后端池，未伪造，属既有端点契约限制（QA §2.6-1）
  - 【观察项·登记】金额展示绝对值路径：Ledger 行展示 = Converter 元数值 × formatAmount ÷100（HEAD 既有展示路径，后端 VO 金额为 Long 分 FinanceVoucherVO.java:38-41）——本批只保证「零变更 + 行/合计一致」，绝对值口径是否正确不判定（无规则依据，且属 Batch0-方案2 金额单位治理既有范围）（QA §2.6-2）
  - 【观察项·登记】无浏览器活体：同 §1.6-2 口径——交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-002）与产品逐页走查承接（QA §2.6-3）
  - 【证据微差·登记】developer 证据行号少量漂移（如 B7 证据 :35-37 vs 实码 :23-24；B5 证据 :173-184 vs 实码 :170-176）：内容全部核验存在，不影响结论（QA §2.6-4）

---

### Batch LRVT-1 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告（`docs/quality/ui-lrvt-b1-qa-report.md`）观察项如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **【观察项·登记】存储键按 SearchPanel 前缀约定（语义等价）**（§1.6-1）：卡文 `storage-key="finance-fund-filter"` vs 实码 `fts_search_finance-fund-filter` / `fts_search_finance-ledger-filter`——按 SearchPanel 约定（`fts_search_`+key）语义等价，代码注释已注明，无功能影响
2. **【观察项·登记】无浏览器活体（留走查）**（§1.6-2/§2.6-3）：本环境无浏览器/接口运行条件，交互核验为代码路径级 + 构建级；运行时点击走查留 REG-FIN-LRVT-001/002 与产品逐页走查承接（同既往批次浏览器缺口口径）
3. **【观察项·登记】批量过账提交数口径（后端布尔）**（§2.6-1）：后端 `batchPost` 仅返回布尔，N/M 反馈为提交数口径（total=ids.length、success=ok?ids.length:0）——精确明细已登记后端池，未伪造，属既有端点契约限制
4. **【观察项·登记】金额绝对值 Batch0-方案2 范围**（§2.6-2）：Ledger 行展示 = Converter 元数值 × formatAmount ÷100（HEAD 既有展示路径）；本批保证「零变更 + 行/合计一致」；绝对值口径不判定（无规则依据，属 Batch0-方案2 金额单位治理既有范围）
5. **【证据微差·登记】developer 证据行号少量漂移**（§1.6-3/§2.6-4）：如 B4 :575-584 vs 实码 :573-587、B7 :35-37 vs :23-24、B5 :173-184 vs :170-176——内容全部核验存在，不影响结论（同既往批次行号口径差异模式）
6. **【衔接·登记】既往条目断言过时衔接**（任务板 §13.4「回归基线衔接」）：布局级回退使 REG-UI-FIN-001（SearchPanel 组件化接入 + expand 行内详情）、REG-UI-FIN-002（expand 分录）、REG-FIN-WS-001（五区布局待办区/工具栏）部分断言过时——现状已由本批 REG-FIN-LRVT-001/002 新增断言覆盖；本批按 101 只增不减零修改执行（不删除不覆盖），原条目录内「断言已过时」标注留待后续批次/architect 统一落位

---

### Batch LRVT-1 门禁

```text
Regression Result: PASS（2026-09-05 Batch LRVT-1 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为布局级回退批次（任务池 §13），非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：2/2 卡 QA 独立验收 PASS（`docs/quality/ui-lrvt-b1-qa-report.md`，2026-09-05：P1-FIN-LRVT-001 资金管理 / P1-FIN-LRVT-002 财务总账，无 FAIL、无 -R；布局与 HEAD 同构 + 重放修复点 B1~B6/B1~B7 全有效 + 穿透成对不断链 + 能力层保留 + 登记类入口不失效 + 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）
- FAIL 明细：0
- PWL 明细：0（观察/登记项 6 条均为非阻断登记，不构成 FAIL；PD-028/PD-035 为 BLOCKED 不占通过数；导出/日结/批量审核/凭证导入/批量过账 N/M 明细为登记依赖非 FAIL）
- 基线：新增 REG-FIN-LRVT-001/002（正式 101 → 103 条，只增不减、既有条目零删除零修改；候选 0 条维持）
- 布局回退专项核对：Batch LRVT-1 两卡完整性核对 = **REG-FIN-LRVT-001/002 全部在案**（穿透成对样板批闭环，无缺卡、无预固化条目）；专项编排 Batch LRVT-1~4 逐批执行（001/002 = Batch LRVT-1；003/004 = LRVT-2；005/007 = LRVT-3；006/008/009 = LRVT-4）；**等待产品负责人逐页走查**（roadmap §5.14，完成后停止，不自动开新批）
- 观察项：存储键按 SearchPanel 前缀约定（语义等价）/ 无浏览器活体（留走查）/ 批量过账提交数口径（后端布尔）/ 金额绝对值 Batch0-方案2 范围 / developer 证据行号漂移 / 既往条目断言过时衔接（REG-UI-FIN-001/002、REG-FIN-WS-001 部分断言，现状由本批基线覆盖）——如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0（登记项不构成 FAIL）→ 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

## Batch LRVT-2 回归（布局级回退批次：P1-FIN-LRVT-003 + 004 应收应付同构批，2026-09-05）

> 验收依据：`docs/quality/ui-lrvt-b2-qa-report.md`（2026-09-05，QA 独立验收：P1-FIN-LRVT-003 应收账款 = **PASS** / P1-FIN-LRVT-004 应付账款 = **PASS**——无 FAIL、无 -R；布局与 HEAD 逐块比对同构 + 重放修复点 B1~B8 全部落位有效【Receivable 到期日筛选端到端 / Payable 创建日期筛选端到端 + **到期日登记处置保持（F-1 防回归）**】+ 处置③ 详情回迁（11/13 字段）+ 能力层保留 + 口径标注 + 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）；裁决依据 = `remediation-roadmap.md` §5.14（2026-09-05 产品负责人指令：9 页 A+B 全部回退原布局 + 重放修复 + 重放处置 6 项已定）；回退基线 = `git show HEAD:frontend/src/views/finance/FinanceReceivable.vue` / `HEAD:frontend/src/views/finance/FinancePayable.vue`（HEAD cd1c89b = 回退前原始基线）+ 盘点报告 `docs/quality/finance-layout-revert-inventory-20260905.md`（§2.3/§2.4 before 结构）；对照基准 = 任务板 §13.4 P1-FIN-LRVT-003/004 卡（统一验收标准 6 条 + 卡内细化验收标准）+ §13.4.2 developer 修改证据；开发者未自行宣布通过 ✅——本报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-LRVT-003/004（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（布局级回退，任务池 §13），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**布局回退专项 Batch LRVT-1~4 逐批执行（本批 = 应收应付同构批），全部完成后停止，等待产品负责人逐页走查（roadmap §5.14，不自动开新批）**。
> 基线计数核对：追加前正式基线 **103 条**（文件实况：基线总览表 103 行，含 REG-FIN-LRVT-001~002）→ 追加 2 条（REG-FIN-LRVT-003/004）后 **105 条**，只增不减纪律达成（既有 103 条条目零删除零修改）；候选 0 条维持。
> 断言衔接登记（任务板 §13.4「回归基线衔接」原则，本批按 103 只增不减零修改执行，不删除不覆盖）：布局级回退使既往条目部分断言过时——REG-UI-FIN-003「SearchPanel 组件化接入 + expand 行内详情」断言（Receivable 已恢复手写筛选区 + 详情 el-dialog 回迁）、REG-UI-FIN-004 同口径断言（Payable 恢复手写筛选区 + 详情 13 字段回迁）、REG-FIN-WS-002「核销工作台双页签布局（待办区/工具栏）」断言（待办区/工具栏随布局回退移除；WS-002 其余断言已由 RVT-002 接管标注）——现状由本批 REG-FIN-LRVT-003/004 新增断言覆盖（布局同构 + 重放修复点 B1~B8 + 能力层保留 + **应付到期日登记处置保持 F-1 防回归**）；原条目录内标注留待后续批次/architect 按 §13.4 统一落位（本批零修改纪律优先）。

### REG-FIN-LRVT-003

- **测试编号**：REG-FIN-LRVT-003
- **模块**：财务-应收账款（frontend/src/views/finance/FinanceReceivable.vue，581 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-003（Batch LRVT-2；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b2-qa-report.md`）
- **业务场景**：应收账款布局级回退（布局回退专项·应收应付同构批 #1，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/②/③）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（含 #extra :403-417）→ stats-section（统计卡片回顶层 :419-421）→ advanced-search-panel（手写 div 恢复 :423-451，与 HEAD :217-233 同构；status select + keyword input 原位；B1 到期日区间 daterange 嵌入 toolbar-left :435-444）→ table-section（:452-503，与 HEAD :234-244 同构；追加 CaliberNoteBar :454 + B6 EmptyState 双态 :456-472 + B3 核销进度列 :144/:489-491 + B2 坏账核销 actions :498，均为 B 修复点嵌入）→ pagination-wrapper（独立分页恢复 :505-514，与 HEAD :246-254 同构，分页不并入 DataTable）→ 新建 el-dialog（:516-535，与 HEAD :257-276 同构）→ 详情 el-dialog（:537-556，与 HEAD :278-297 同构；处置③ expand 回迁 11 字段，descriptions 项顺序逐一一致）→ ReceiptDialog / ReceiptHistoryDialog（:558-567，与 HEAD :299-308 同构，组件零改动）
  - **已移除确认（grep 扫描）**：`workbench-taskboard` / `workbench-toolbar` / `pendingSummary` / `receivableSummaryText` / `receivable-page` 容器仅头部注释提及（:10-14/:11/:575），模板与样式零命中；`SearchPanel` 仅注释提及（:223-275 持久化语义说明）；`#expand` / `expandable` **全文件零命中**（含注释）；`.workbench-*` 样式已删（:571-581 仅保留 `.row-overdue` 随 B4 重放）
  - **重放修复点 B1~B8 全有效**：B1 到期日筛选真实化（:56-57 searchForm.dueDateRange / :161-163 params.startDueDate/endDueDate / :435-444 daterange `value-format="YYYY-MM-DD"`——**端到端链路全闭合**：控件 → FinanceReceivableQueryForm → receivable.ts mapQueryParams 透传 :28-38 → GET /v1/finance/receivables → ReceivableQueryDTO.startDueDate/endDueDate（LocalDate :24-27）→ ReceivableServiceImpl.getPage :103-105 → ReceivableMapper.xml :16-21 `AND due_date >= #{startDate} / <= #{endDate}`（@Param 绑定）——真实筛选，非本地过滤）；B2 坏账核销入口（:304-326 handleWriteOff = ElMessageBox.confirm 二次确认 :311-315 + receivableApi.writeOff :320 / :498 行内 actions「坏账核销」link 按钮 v-if row.status!=='settled'——**端点既有**：receivable.ts:104-106 → POST /v1/finance/receivables/{id}/write-off（ReceivableController.java:72-76，@PreAuthorize finance:receivable:approve 既有）；处置① 行内 actions = 原布局入口位置；二次确认防误触；零新增接口）；B3 核销进度列（:127-134 getProgressPercent = receivedAmount/amount 派生 + :144 progress 列 + :489-491 slot + title 已收/应收明细——**后端实锤**：ReceivableServiceImpl.java:130 `long newReceived = r.getReceivedAmount() + amount;` confirmPayment 累加真实汇总字段 + :301 vo.setReceivedAmount + Converter 分→元；零新增接口调用，纯展示派生）；B4 逾期高亮（:88-96 getStatusTag overdue 键 → type:'overdue' / :101-103 rowClassName / :483 :row-class-name 透传 / :576-580 .row-overdue 浅橙背景 `var(--fts-status-overdue-bg)`——语义键真实：后端状态 4=overdue（convertToVO :307-314 按 dueDate/balance 动态计算）+ tokens 三元组 + StatusTag 支持 overdue 型；能力层保留）；B5 打印（:336-338 handlePrint → window.print / :409 #extra 打印按钮——浏览器原生打印，零接口依赖）；B6 失败透传（:64 loadFailed / :169-177 catch→loadFailed+ElMessage.error / :181-183 handleRetry / :456-472 EmptyState type="error" action-text="重试" + 引导型 no-data 双态）；B7 筛选持久化（:225 FILTER_STORAGE_KEY='fts_search_finance-receivable-filter' / :227-257 saveFilter/restoreSavedFilter/clearSavedFilter + watch deep / :274-278 onMounted 先恢复后加载 / :194-221 toDateString/restoreDateRange/handleFilterRestored 旧存档兼容——语义同 SearchPanel 前缀约定，旧存档不丢失，仅回填表单字段不发明规则）；B8 导出/批量收款登记（:410-415 #extra 导出/批量收款 disabled+tooltip，文案引用 P1-FIN-EXPORT-001 盘点 + ui-fin-b2 批量端点登记链——登记类入口隐藏不失效）
  - **处置③ 详情回迁（11 字段）**：详情 el-dialog :537-556 = **HEAD 原列结构**（应收编号/客户名称/应收金额/已收金额/未收金额/账龄/开票日期/到期日/状态/发票号/备注 = 11 个 descriptions 项，与 HEAD :278-297 顺序逐一一致）；expand 已移除（`#expand`/`expandable` 全文件零命中）
  - **能力层保留（处置⑥ 不回退）**：density="auto" :479 / show-summary :480 + summaryMethod = createAmountSummaryMethod({amountProps:['amount','receivedAmount','remainAmount'], unit:'yuan'}) :107-110（useSummary.ts:44-47 支持 'fen'/'yuan'）/ 固定列 :139（应收编号 left）/ :147（状态 right）/ 逾期高亮 :483 + :576-580 样式；能力组件（DataTable/EmptyState/useSummary/CaliberNoteBar/StatusTag）零改动
  - **actions-width 220→280 合理性核验**：HEAD :235 `:actions-width="220"`（3 按钮）→ 当前 :482 `:actions-width="280"`（4 按钮，承载 B2 新增第 4 个行内动作）——**属性级调整，非区块变更**；宽度增量 60px 与 1 个 link 按钮空间需求匹配，不改变布局骨架（QA 核验合理）
  - **口径标注**：:454 CaliberNoteBar 嵌入表格上方原布局位置（PD-031/032 文案，纯展示零数据依赖）
  - **强制核验**：布局同构（git diff 归因 = B1~B8 修复点嵌入 + CaliberNoteBar + EmptyState 双态 + 能力层；Receivable +381）/ 后端零改动（仅调用既有 api 方法：receivableApi.getList·create·writeOff / supplierApi.getList；LRVT-003/004 标记仅本批两文件，全仓 grep 2 命中；writeOff 端点 PreAuthorize 既有）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 构建独立复跑 typecheck 136=136 两文件零错误 + build EXIT=0 / 批隔离（P1-STOCK-001 零触碰）
- **测试目的**：防止应收账款布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→ReceiptDialog→ReceiptHistoryDialog）；待办区/工具栏/SearchPanel/#expand 不得复活（模板样式零残留）；重放修复点 B1~B8 不得回退（到期日筛选端到端/坏账核销既有端点/核销进度列后端实锤/逾期高亮/打印/失败透传/筛选持久化/导出批量登记）；能力层（density/show-summary unit='yuan'/固定列）不回退；口径标注不失效。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/FinanceReceivable.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→ReceiptDialog→ReceiptHistoryDialog）
  2. grep 扫描：`workbench-taskboard` / `workbench-toolbar` / `pendingSummary` / `receivable-page` / `SearchPanel` / `#expand` / `expandable` / `V3-A`——仅注释提及、模板/样式零命中；`.workbench-*` 样式删除确认
  3. 逐修复点核验 B1~B8 存在 + 消费链闭合（B1：控件→params→mapQueryParams 透传→DTO→Service:103-105→Mapper due_date；B2：writeOff 既有端点 + 二次确认 + 行内 actions；B3：receivedAmount 后端实锤 Service:130；B4：overdue 键 + rowClassName + .row-overdue 样式）
  4. 处置③ 核验：详情 11 字段与 HEAD 顺序逐一一致；expand 模板零命中
  5. 能力层核验（density/show-summary unit='yuan'/固定列）；actions-width 220→280 属性级调整合理性
  6. 强制核验独立复跑：typecheck 136=136（FinanceReceivable.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + 能力层属性级透传）；待办区/工具栏/SearchPanel/#expand 模板样式零残留（仅注释）
  - 重放修复点 B1~B8 全部落位有效（到期日筛选端到端真实过滤 / 坏账核销既有端点+二次确认+行内 actions / 核销进度列 receivedAmount 后端实锤 / 逾期高亮 / 打印 / 失败透传显式失败态+重试 / 筛选持久化 fts_search_ / 导出批量登记 disabled+tooltip）
  - 详情 el-dialog 11 字段回迁（与 HEAD 顺序一致）；能力层保留（density / show-summary unit='yuan' / 固定列）；口径标注 CaliberNoteBar 不失效
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤核验 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】状态筛选「已逾期」为动态计算值：后端 overdue(4) 由 convertToVO 按 dueDate/balance 动态计算（ReceivableServiceImpl.java:307-314），**不持久化**——`WHERE status=4` 筛选可能无结果；该行为 HEAD 既有（非本批回退引入）；B4 逾期高亮基于列表响应 computed status 正常生效不受影响；如需修复属后端状态持久化治理，另行立项（QA §1.7-1）
  - 【观察项·登记】ReceivableMapper.xml 参数名错位：mapper @Param 名为 startDate/endDate（ReceivableMapper.java:32-33），实际接收 service 传入的 startDueDate/endDueDate 值——功能正确（@Param 绑定），命名语义错位为后端既有状态，非本批引入（QA §1.7-2）
  - 【观察项·登记】无浏览器活体：本环境无浏览器/接口运行条件，交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-003）与产品逐页走查承接（QA §1.7-3）
  - 【证据微差·登记】developer 证据行号少量漂移（如 B6 :149-171 vs 实码 :152-178 区间、B7 :450-453 vs 实码 :260-264）：内容全部核验存在，不影响结论（QA §1.7-4）

### REG-FIN-LRVT-004

- **测试编号**：REG-FIN-LRVT-004
- **模块**：财务-应付账款（frontend/src/views/finance/FinancePayable.vue，610 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-004（Batch LRVT-2；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b2-qa-report.md`）
- **业务场景**：应付账款布局级回退（布局回退专项·应收应付同构批 #2，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/②/③）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（含 #extra :409-423）→ stats-section（:425-427，与 HEAD :231-233 同构）→ advanced-search-panel（手写 div 恢复 :430-476，与 HEAD :234-250 同构；status select + keyword input 原位；B1 创建日期 daterange :442-451 + **B2 到期日 disabled 控件+tooltip :454-469** 嵌入 toolbar-left）→ table-section（:477-527，与 HEAD :251-262 同构；追加 CaliberNoteBar :479 + B6 EmptyState 双态 :481-497 + B3 核销进度列 :152/:514-516，均为 B 修复点嵌入）→ pagination-wrapper（独立分页恢复 :529-538，与 HEAD :263-272 同构）→ 新建 el-dialog（:540-561，与 HEAD :274-295 同构）→ 详情 el-dialog（:563-584，与 HEAD :297-318 同构；处置③ expand 回迁 13 字段，descriptions 项顺序逐一一致）→ PaymentDialog / PaymentHistoryDialog（:586-596，与 HEAD :320-330 同构，组件零改动；PaymentHistoryDialog @void-success 保留）
  - **已移除确认（grep 扫描）**：`workbench-taskboard` / `workbench-toolbar` / `pendingSummary` / `payableSummaryText` / `payable-page` 容器仅头部注释提及（:10-14/:11/:604），模板与样式零命中；`SearchPanel` 仅注释提及；`#expand` / `expandable` **全文件零命中**；`.workbench-*` 样式已删（:600-610 仅保留 `.row-overdue` 随 B4 重放）
  - **重放修复点 B1~B8 全有效**：B1 创建日期筛选真实化（:58-59 searchForm.dateRange / :172-174 params.startDate/endDate / :442-451 daterange `value-format="YYYY-MM-DD"`——**端到端链路全闭合**：控件 → FinancePayableQueryForm → payable.ts mapQueryParams 透传 → GET /v1/finance/payables → PayableQueryDTO.startDate/endDate（LocalDate :26-30）→ PayableServiceImpl.getPage :111-113 → PayableMapper.xml :16-21 `AND create_time >= #{startDate} / <= #{endDate}`（@Param 绑定）——真实筛选（创建日期口径），非本地过滤）；**B2 到期日区间 QA F-1 登记处置保持（重点防回归，不因回退变回假筛选）**（:61-62 dueDateRange 声明恒 null / :176-177 loadData 注释明示不发送死参数，**代码零发送** / :236 handleFilterRestored 旧存档不回填统一置 null / :454-469 el-tooltip 明示后端消费链缺失 + el-date-picker disabled——**三重防线全部在案**：① 控件 disabled+tooltip（QA F-1 文案 + P1-FIN-WS-002-R1 引用）② `startDueDate/endDueDate` 在 loadData 零发送（grep 仅注释 4 处命中 :19/:160/:176/:455）③ 旧存档含 dueDateRange 也不回填（置 null 防悬空/死参数）；后端实锤：PayableServiceImpl.getPage :111-113 仅传 supplierName/status/startDate/endDate，**startDueDate/endDueDate 未传给 mapper**；PayableMapper.xml 无 due_date 过滤——QA F-1 属实，前端登记处置与后端现状一致，端到端启用待后端池就绪（不占本卡通过数））；B3 核销进度列（:133-140 getProgressPercent = paidAmount/amount 派生 + :152 progress 列 + :514-516 slot + title 已付/应付明细——**后端实锤**：PayableServiceImpl.java:138 `long newPaid = p.getPaidAmount() + amount;` confirmPayment 累加真实汇总字段 + :404 vo.setPaidAmount + Converter 分→元；零新增接口调用）；B4 逾期高亮（:94-102 getStatusTag overdue 键 / :107-109 rowClassName / :508 :row-class-name 透传 / :605-609 .row-overdue 浅橙背景 `var(--fts-status-overdue-bg)`——语义键真实（后端 convertToVO :416-422 按 dueDate 动态计算 status=4）；tokens/StatusTag 支持同 Receivable 核验；能力层保留）；B5 打印（:319-321 handlePrint → window.print / :415 #extra 打印按钮——浏览器原生打印，零接口依赖）；B6 失败透传（:69 loadFailed / :182-188 catch→loadFailed+ElMessage.error / :194-196 handleRetry / :480-497 EmptyState error 重试 + 引导型 no-data 双态）；B7 筛选持久化（:242 FILTER_STORAGE_KEY='fts_search_finance-payable-filter' / :244-277 saveFilter/restoreSavedFilter/clearSavedFilter + watch deep / :399-404 onMounted 先恢复后加载 / :207-238 toDateString/restoreDateRange/handleFilterRestored——语义同 SearchPanel 前缀约定；**handleFilterRestored 到期日统一置 null（B2 处置保持）**）；B8 导出/批量付款登记（:416-421 #extra 导出/批量付款 disabled+tooltip，P1-FIN-EXPORT-001 / ui-fin-b2 登记链——登记类入口隐藏不失效）
  - **处置③ 详情回迁（13 字段）**：详情 el-dialog :563-584 = **HEAD 原列结构**（应付编号/采购订单号/入库单号/供应商名称/应付金额/已付金额/未付金额/账龄/开票日期/到期日/状态/发票号/备注 = 13 个 descriptions 项，与 HEAD :297-318 顺序逐一一致）；expand 已移除（`#expand`/`expandable` 全文件零命中）
  - **能力层保留（处置⑥ 不回退）**：density="auto" :504 / show-summary :505 + summaryMethod = createAmountSummaryMethod({amountProps:['amount','paidAmount','remainAmount'], unit:'yuan'}) :113-116 / 固定列 :145（应付编号 left）/ :155（状态 right）/ 逾期高亮 :508；actions-width :507 = **280，与 HEAD 一致**（HEAD :252 即 280）
  - **口径标注**：:479 CaliberNoteBar 嵌入表格上方（纯展示，PD-031/032 文案）
  - **强制核验**：布局同构（git diff 归因 = B1~B8 修复点嵌入 + CaliberNoteBar + EmptyState 双态 + 能力层；Payable +380）/ 后端零改动（仅调用既有 api 方法：payableApi.getList·create / supplierApi.getList；LRVT-003/004 标记仅本批两文件）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 应付到期日登记处置保持（F-1 三重防线在案 + 后端零消费实锤）/ 构建独立复跑 typecheck 136=136 两文件零错误 + build EXIT=0 / 批隔离（P1-STOCK-001 零触碰）
- **测试目的**：防止应付账款布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→PaymentDialog→PaymentHistoryDialog）；待办区/工具栏/SearchPanel/#expand 不得复活（模板样式零残留）；重放修复点 B1~B8 不得回退（创建日期筛选端到端/**到期日登记处置保持 F-1 防回归**/核销进度列后端实锤/逾期高亮/打印/失败透传/持久化/导出批量登记）；处置③ 详情 13 字段回迁保持；能力层（density/show-summary unit='yuan'/固定列）不回退；口径标注不失效。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/FinancePayable.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats→advanced-search-panel→table→pagination-wrapper→新建/详情 el-dialog→PaymentDialog→PaymentHistoryDialog）
  2. grep 扫描：`workbench-taskboard` / `workbench-toolbar` / `pendingSummary` / `payable-page` / `SearchPanel` / `#expand` / `expandable` / `V3-A`——仅注释提及、模板/样式零命中；`.workbench-*` 样式删除确认
  3. 逐修复点核验 B1~B8 存在 + 消费链闭合（B1：控件→params→mapQueryParams 透传→DTO→Service:111-113→Mapper create_time；**B2：恒 null + loadData 零死参数发送 + 旧存档不回填 + disabled+tooltip 三重防线在案（grep startDueDate/endDueDate 仅注释 4 命中）**；B3：paidAmount 后端实锤 Service:138）
  4. 处置③ 核验：详情 13 字段与 HEAD 顺序逐一一致；expand 模板零命中
  5. 能力层核验（density/show-summary unit='yuan'/固定列）；actions-width 280 与 HEAD 一致
  6. 强制核验独立复跑：typecheck 136=136（FinancePayable.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + 能力层属性级透传）；待办区/工具栏/SearchPanel/#expand 模板样式零残留（仅注释）
  - 重放修复点 B1~B8 全部落位有效（创建日期筛选端到端真实过滤 / **到期日登记处置保持（恒 null+零死参数+不回填+disabled+tooltip，不因回退变回假筛选）** / 核销进度列 paidAmount 后端实锤 / 逾期高亮 / 打印 / 失败透传显式失败态+重试 / 筛选持久化 fts_search_ / 导出批量登记 disabled+tooltip）
  - 详情 el-dialog 13 字段回迁（与 HEAD 顺序一致）；能力层保留（density / show-summary unit='yuan' / 固定列）；口径标注 CaliberNoteBar 不失效
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤核验 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】hasActiveFilter 含 dueDateRange 死检查（:77）：dueDateRange 恒 null（B2 处置），该条件永不触发——无害冗余，不影响行为（QA §2.7-1）
  - 【观察项·登记】状态筛选「已逾期」动态计算：同 Receivable（PayableServiceImpl.convertToVO :416-422 动态计算 status=4 不持久化），HEAD 既有行为，非本批引入；B4 高亮基于列表响应正常生效（QA §2.7-2）
  - 【观察项·登记】无浏览器活体：同 §1.7-3 口径——交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-004）与产品逐页走查承接（QA §2.7-3）
  - 【证据微差·登记】developer 证据行号少量漂移（如 B6 :157-186 vs 实码 :163-191 区间、B7 :473-476 vs 实码 :277-281）：内容全部核验存在，不影响结论（QA §2.7-4）

---

### Batch LRVT-2 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告（`docs/quality/ui-lrvt-b2-qa-report.md`）观察项如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **【观察项·登记】逾期状态动态计算不持久化 = HEAD 既有**（§1.7-1/§2.7-2）：应收/应付后端 overdue(4) 由 convertToVO 按 dueDate/balance 动态计算（ReceivableServiceImpl.java:307-314 / PayableServiceImpl.java:416-422），**不持久化**——`WHERE status=4` 筛选可能无结果；该行为 HEAD 既有（非本批回退引入）；B4 逾期高亮基于列表响应 computed status 正常生效；如需修复属后端状态持久化治理，另行立项，不混入本批
2. **【观察项·登记】Mapper 参数名错位 = 后端既有**（§1.7-2）：ReceivableMapper @Param 名为 startDate/endDate（ReceivableMapper.java:32-33），实际接收 service 传入的 startDueDate/endDueDate 值——功能正确（@Param 绑定），命名语义错位为后端既有状态，非本批引入，不影响 B1 端到端真实性
3. **【观察项·登记】hasActiveFilter 死检查无害**（§2.7-1）：Payable :77 hasActiveFilter 含 dueDateRange 条件，因 dueDateRange 恒 null（B2 处置）永不触发——无害冗余，不影响行为
4. **【观察项·登记】无浏览器活体（留走查）**（§1.7-3/§2.7-3）：本环境无浏览器/接口运行条件，交互核验为代码路径级 + 构建级；运行时点击走查留 REG-FIN-LRVT-003/004 与产品逐页走查承接（同既往批次浏览器缺口口径）
5. **【证据微差·登记】developer 证据行号少量漂移**（§1.7-4/§2.7-4）：如 B6 :149-171 vs 实码 :152-178、B7 :450-453 vs :260-264、B6 :157-186 vs :163-191、B7 :473-476 vs :277-281——内容全部核验存在，不影响结论（同既往批次行号口径差异模式）
6. **【衔接·登记】既往条目断言过时衔接**（任务板 §13.4「回归基线衔接」）：布局级回退使 REG-UI-FIN-003（SearchPanel 组件化接入 + expand 行内详情）、REG-UI-FIN-004（同口径）、REG-FIN-WS-002（核销工作台双页签布局待办区/工具栏）部分断言过时——现状已由本批 REG-FIN-LRVT-003/004 新增断言覆盖（布局同构 + B1~B8 重放 + 能力层保留 + **应付到期日登记处置保持 F-1 防回归**）；本批按 103 只增不减零修改执行（不删除不覆盖），原条目录内「断言已过时」标注留待后续批次/architect 统一落位
7. **【登记依赖·非 FAIL】应付到期日区间端到端启用依赖后端池**（§2.2 B2 / QA F-1 / P1-FIN-WS-002-R1 登记链）：前端登记处置保持至后端就绪（PayableServiceImpl 补传 startDueDate/endDueDate + Mapper 参数 + XML due_date 过滤，任务池 L3694）；就绪后按既有登记卡接入；不占本批通过数

---

### Batch LRVT-2 门禁

```text
Regression Result: PASS（2026-09-05 Batch LRVT-2 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为布局级回退批次（任务池 §13），非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：2/2 卡 QA 独立验收 PASS（`docs/quality/ui-lrvt-b2-qa-report.md`，2026-09-05：P1-FIN-LRVT-003 应收账款 / P1-FIN-LRVT-004 应付账款，无 FAIL、无 -R；布局与 HEAD 同构 + 重放修复点 B1~B8 全有效【含应付到期日登记处置保持 F-1 防回归】+ 处置③ 详情回迁 11/13 字段 + 能力层保留 + 口径标注 + 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）
- FAIL 明细：0
- PWL 明细：0（观察/登记项 7 条均为非阻断登记，不构成 FAIL；PD-028/PD-035 为 BLOCKED 不占通过数；应付到期日端到端启用/导出/批量为登记依赖非 FAIL）
- 基线：新增 REG-FIN-LRVT-003/004（正式 103 → 105 条，只增不减、既有条目零删除零修改；候选 0 条维持）
- 布局回退专项核对：Batch LRVT-2 两卡完整性核对 = **REG-FIN-LRVT-003/004 全部在案**（应收应付同构批闭环，无缺卡、无预固化条目）；专项累计 **101 → 105 = 4 条**（LRVT-1：001/002 + LRVT-2：003/004）；专项编排 Batch LRVT-1~4 逐批执行（003/004 = LRVT-2；005/007 = LRVT-3；006/008/009 = LRVT-4）；**等待产品负责人逐页走查**（roadmap §5.14，完成后停止，不自动开新批）
- 观察项：逾期状态动态计算不持久化=HEAD 既有 / Mapper 参数名错位=后端既有 / hasActiveFilter 死检查无害 / 无浏览器活体（留走查）/ developer 证据行号漂移 / 既往条目断言过时衔接（REG-UI-FIN-003/004、REG-FIN-WS-002 部分断言，现状由本批基线覆盖）/ 应付到期日端到端启用=后端池登记依赖——如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0（登记项不构成 FAIL）→ 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

## Batch LRVT-3 回归（布局级回退批次：P1-FIN-LRVT-005 + 007 税务成本批，2026-09-05）

> 验收依据：`docs/quality/ui-lrvt-b3-qa-report.md`（2026-09-05，QA 独立验收：P1-FIN-LRVT-005 税务管理 = **PASS** / P1-FIN-LRVT-007 成本管理 = **PASS**——无 FAIL、无 -R；布局与 HEAD 逐块比对同构 + 重放修复点 B1~B7 / B1~B6 全部落位有效【Tax：getRecords 端到端三 eq 实锤 / payTax+PD-001 幂等零触碰 / KL-059 税期动态化 YYYYMM 契约零变更；Cost：monthrange 月期间全链 ge/le 实锤真实筛选非本地过滤 + 成本结构分析分→元一次转换无二次 /100】+ 处置③ 详情回迁（Cost 8 字段）+ 能力层保留 + 口径标注 + 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）；裁决依据 = `remediation-roadmap.md` §5.14（2026-09-05 产品负责人指令：9 页 A+B 全部回退原布局 + 重放修复 + 重放处置 6 项已定）；回退基线 = `git show HEAD:frontend/src/views/finance/FinanceTax.vue` / `HEAD:frontend/src/views/finance/FinanceCost.vue`（HEAD cd1c89b = 回退前原始基线）+ 盘点报告 `docs/quality/finance-layout-revert-inventory-20260905.md`（§2.5/§2.7 before 结构）；对照基准 = 任务板 §13.4 P1-FIN-LRVT-005/007 卡（统一验收标准 6 条 + 卡内细化验收标准）+ §13.4.3 developer 修改证据；开发者未自行宣布通过 ✅——本报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-LRVT-005/007（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（布局级回退，任务池 §13），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**布局回退专项 Batch LRVT-1~4 逐批执行（本批 = 税务成本批），全部完成后停止，等待产品负责人逐页走查（roadmap §5.14，不自动开新批）**。
> 基线计数核对：追加前正式基线 **105 条**（文件实况：基线总览表 105 行，含 REG-FIN-LRVT-001~004）→ 追加 2 条（REG-FIN-LRVT-005/007）后 **107 条**，只增不减纪律达成（既有 105 条条目零删除零修改）；候选 0 条维持。
> 断言衔接登记（任务板 §13.4「回归基线衔接」原则，本批按 105 只增不减零修改执行，不删除不覆盖）：布局级回退使既往条目部分断言过时——REG-FIN-WS-004「合规工作台五区布局（待办区/工具栏）」断言（Tax 待办区/工具栏随布局回退移除）、REG-FIN-WS-005「毛利核算工作台五区布局（待办区/工具栏）」断言（Cost 待办区/工具栏随布局回退移除；WS-005 其余断言由本批 REG-FIN-LRVT-007 覆盖）、REG-UI-FIN-001「SearchPanel 组件化接入」断言（Tax/Cost 恢复手写筛选区）——现状由本批 REG-FIN-LRVT-005/007 新增断言覆盖（布局同构 + 重放修复点 + 能力层保留 + **KL-059 税期动态化 / monthrange 端到端**）；原条目录内标注留待后续批次/architect 按 §13.4 统一落位（本批零修改纪律优先）。

### REG-FIN-LRVT-005

- **测试编号**：REG-FIN-LRVT-005
- **模块**：财务-税务管理（frontend/src/views/finance/FinanceTax.vue，965 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-005（Batch LRVT-3；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b3-qa-report.md`）
- **业务场景**：税务管理布局级回退（布局回退专项·税务成本批 #1，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/②/③）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（含 #extra :673-684，HEAD 无 extra：#extra 为 B 承载：申报表打印 :676 + 导出/批量缴税 disabled+tooltip :677-682，处置① 非布局重排）→ stats-section（统计卡片回 HEAD 顶层 :687-697，与 HEAD :497-509 同构，处置⑤ 回位，4 卡图标/文案与 HEAD 逐字一致）→ tab-section el-tabs 3 页签（:700-848，与 HEAD :511-631 同构，el-tabs 为 HEAD 自身页结构保留，非本次变更）【Tab1 税务记录：筛选区 + table-section :703-754（与 HEAD :514-535 同构；EmptyState error 重试 :725-732 = 附加失败透传；DataTable 追加 density/show-summary = 能力层；**无独立分页 UI** = HEAD 原样，recordPagination 仅作 getRecords 参数载体 pageSize=100 被 API 消费非死参数）；Tab2 纳税申报：筛选区 + table-section :757-796（与 HEAD :552-571 同构，DataTable 追加 density/show-summary = 能力层）；Tab3 税率配置：筛选区 + table-section + **独立 pagination-wrapper :836-845 恢复** :799-846（与 HEAD :587-631 同构，处置⑥；:841-844 与 HEAD 分页行为一致；CaliberNoteBar :822 = B 重放嵌入）】→ 税务记录/税款计算/申报表/税率配置详情 4 对话框（:851-936，与 HEAD 尾部同构，字段逐一比对一致）→ 新增/编辑税率配置对话框（:939-963，与 HEAD 同构，仅 `:teleported="false"→"true"` :942/:950/:953 = B7 弹层修复）
  - **已移除确认（grep 扫描）**：`workbench-taskboard` / `workbench-toolbar` / `task-item` / `pendingSummary` / `pendingTax` / `SearchPanel` / `:pagination` / `@page-change` 仅头部注释提及（:10/:11/:15），模板与样式零命中；`V3-A` 零命中；Tab1 分页封装（:pagination/@page-change）全文件零命中
  - **重放修复点 B1~B7 + 附加 全有效**：B1 税务记录列表真实 API 接入（:116-152 loadTaxRecords → taxCalculationApi.getRecords :120-126 / :664 onMounted——**端到端全链实锤**：控件(taxType/taxPeriod/taxStatus) → getRecords 参数 :123-125 → tax-calculation.ts:95-97 GET /v1/finance/tax-records/page 原样透传 → TaxRecordController.java:44-53 @GetMapping("/page") + TaxRecordDTO 绑定 → **TaxRecordServiceImpl.java:44-46 eq(taxType) / :49-51 eq(taxPeriod) / :54-56 eq(taxStatus) 三 eq 实锤** → MyBatis-Plus SQL；HEAD 原为 TODO 空列表 + 查询仅刷新统计卡，现真实加载）；B2 缴税操作真实化 PD-001 零触碰（:246-279 handlePayTax：payingRowId 单飞守卫 :246-248 + ElMessageBox 二次确认 :251-258 + taxCalculationApi.payTax :261-270 + 成功刷新 :272-273 + 错误透传 :274-275——HEAD 原 mock 假成功 → payTax 真实端点 tax-calculation.ts:107-109 POST /v1/finance/tax-records → TaxRecordController.java:77-83 → **TaxRecordServiceImpl.java:79-90 PD-001 幂等（幂等键 taxType+taxPeriod+voucherNo 命中返回已有记录）零触碰**（mtime 2026/8/31 早于本批）；缴税按钮仅 UNPAID/OVERDUE 行显示 + `:loading="payingRowId === String(row.id)"`）；B3 KL-059 税期动态化（:57-86 TAX_PERIOD_COUNT=4 :58 / buildTaxPeriodOptions :61-74 当前月往前 4 期含当期 / taxPeriodOptions :77 / declarationPeriodOptions :80 / currentPeriod :83-86 / :712-714 Tab1 税期选项 / :765-767 Tab2 申报期间选项 / :287 declarationForm.period = currentPeriod()——HEAD 原硬编码 202603~202606 → 动态生成；**值格式 YYYYMM 与 TaxRecordDTO.taxPeriod 契约零变更**；期数 4 与硬编码一致行为等价；**端到端核验**：Tab1 税期选项 → getRecords taxPeriod → TaxRecordServiceImpl:49-51 eq 消费实锤——非死参数；Tab2 默认申报期 = 当前期消除硬编码 202606 与动态选项脱节）；B4 提交电子税务局失败透传（:410-437 handleSubmitToBureau：二次确认 :412-419 + submitToBureau 真实调用 :421 + catch :428-436 透传真实错误，仅 授权/对接/authorize 关键词触发配置引导 warning——HEAD 原 catch 降级 info 吞错 → 真实错误透传不再吞错）；B5 申报表加载失败提示（:294-316 loadDeclarationRecords catch :307-315 → ElMessage.error('纳税申报表加载失败，请重试')——HEAD 原 catch 仅 console.error 静默 → 显式失败提示）；B6 useSummary 合计（:228-230 recordSummaryMethod = createAmountSummaryMethod({amountProps:['taxAmount','paidAmount'], unit:'yuan'}) / :328-330 declarationSummaryMethod = {['totalTaxPayable'], unit:'yuan'} / 模板 :740-741/:783-784 show-summary + :summary-method——能力层保留，useSummary.ts:44-59/:76-86 core 层既有，金额口径零变更）；B7 teleported + 默认申报期动态化（:942/:950/:953 configDialog 表单控件 `:teleported="true"` / :287 declarationForm.period = currentPeriod()——HEAD 原 `:teleported="false"` 三处 → true 弹层修复；Tab2 默认申报期动态化 B3 同源）；附加：失败透传 EmptyState（:97 recordLoadFailed / :116-152 catch→loadFailed+ElMessage.error :140-148 / :155-157 handleRecordRetry / :725-732 EmptyState type="error" action-text="重试"）；附加：打印 + 登记类入口（:651-653 handlePrint → window.print / :675-682 #extra：申报表打印 :676 + 导出 disabled+tooltip :677-679【P1-FIN-EXPORT-001 登记链】+ 批量缴税 disabled+tooltip :680-682【无批量端点登记，不逐条循环】）
  - **能力层保留（处置⑥ 不回退）**：density="auto" :739（Tab1）/ :782（Tab2）/ :823（Tab3）/ show-summary + useSummary unit='yuan'（B6）/ 固定列 :473（Tab3 操作列 fixed right，HEAD 同列定义）；能力组件（DataTable/EmptyState/CaliberNoteBar/useSummary/StatusTag/StatCard/PageHeader）零改动
  - **强制核验**：布局同构（git diff 归因 = B1~B7 修复点嵌入 + PageHeader#extra 承载 + CaliberNoteBar + EmptyState + 能力层 props；Tax +267/-61）/ 后端零改动 + api 层零改动（mtime 时间戳核验：TaxRecordServiceImpl/TaxRecordController 2026/8/31、tax-calculation.ts 2026/8/31 均早于本批 9/5；9/5 10:55-10:56 仅 FinanceTax/FinanceCost 两文件被修改；本文件仅调用既有 api 方法全部指向既有端点）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 筛选端到端零死参数（taxPeriod 链 控件→getRecords 参数→Service:49-51 eq 实锤）/ 构建独立复跑 typecheck 136=136 两文件零错误 + build EXIT=0 / 批隔离（P1-STOCK-001 零触碰；LRVT-005/007 标记仅本批两文件）
- **测试目的**：防止税务管理布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra→stats 顶层→el-tabs 3 页签→4 详情对话框→配置对话框）；待办区/工具栏/SearchPanel/:pagination 不得复活（模板样式零残留）；重放修复点 B1~B7 不得回退（getRecords 三 eq 端到端/payTax+PD-001 幂等零触碰/KL-059 税期动态化 YYYYMM 契约零变更/失败透传/申报表提示/useSummary unit='yuan'/teleported+默认申报期）；Tab3 独立 pagination-wrapper 恢复保持；能力层（density/show-summary unit='yuan'/固定列）不回退。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/FinanceTax.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats 顶层→el-tabs 3 页签→4 详情对话框→配置对话框）
  2. grep 扫描：`workbench-taskboard` / `workbench-toolbar` / `task-item` / `pendingSummary` / `pendingTax` / `SearchPanel` / `:pagination` / `@page-change` / `V3-A`——仅注释提及、模板/样式零命中
  3. 逐修复点核验 B1~B7 + 附加存在 + 消费链闭合（B1：控件→getRecords 参数→tax-calculation.ts:95-97→Controller:44-53→Service:44-56 三 eq；B2：payTax 真实端点 + PD-001 幂等零触碰 Service:79-90；B3：KL-059 动态税期 YYYYMM 契约零变更 + Service:49-51 消费实锤；B4/B5：失败透传不再吞错；B6：useSummary unit='yuan'；B7：teleported true + 默认申报期动态化）
  4. 处置⑥ 核验：Tab3 独立 pagination-wrapper 恢复（:836-845 与 HEAD 分页行为一致）
  5. 能力层核验（density/show-summary unit='yuan'/固定列）
  6. 强制核验独立复跑：typecheck 136=136（FinanceTax.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + 能力层属性级透传）；待办区/工具栏/SearchPanel/:pagination 模板样式零残留（仅注释）
  - 重放修复点 B1~B7 全部落位有效（getRecords 三 eq 端到端实锤 / payTax+PD-001 幂等零触碰 / KL-059 税期动态化 YYYYMM 契约零变更 / 失败透传显式失败态 / 申报表加载失败提示 / useSummary unit='yuan' 合计 / teleported+默认申报期动态化）；附加（EmptyState 重试/打印/#extra 导出批量缴税登记）不失效
  - Tab3 独立 pagination-wrapper 恢复；能力层保留（density / show-summary unit='yuan' / 固定列）
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤核验 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】handlePayTax 未传 voucherNo：:261-270 payTax 载荷无 voucherNo 字段 → 后端 PD-001 isIdempotencyKeyComplete（TaxRecordServiceImpl.java:108-113）判定幂等键不完整 → 走正常新增路径（非幂等命中）；此为 WS-004 批既有状态（ui-ws-b4-qa-report § 缴税幂等已验收同结构 :251-260，与诊断 08-31 版一致），本批重放保持未变更；PD-001 后端幂等实现零触碰属实；如产品要求缴税幂等命中，需前端补 voucherNo（另行立项，不混入本批）（QA §1.5-1）
  - 【观察项·登记】Tab1 recordPagination 无分页 UI（:93 useStandardPage defaultPageSize=100）：恢复 HEAD 原样（HEAD Tab1 本就无独立分页），pageSize=100 仅作 getRecords 参数载体被 API 消费（pageNum/pageSize 实传），非死参数；注释明示（QA §1.5-2）
  - 【观察项·登记】税期选项语义：buildTaxPeriodOptions 含当期往前 4 期（2026-09 时点 = 202609~202606），期数与 HEAD 硬编码（202603~202606）一致，行为等价；语义向前滚动（超期失效消除）为 KL-059 修复目标（QA §1.5-3）
  - 【观察项·登记】无浏览器活体：本环境无浏览器/接口运行条件，交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-005）与产品逐页走查承接（QA §1.5-4）
  - 【证据微差·登记】developer 证据行号少量漂移（如 B2 :246-287 vs 实码 :246-279 区间、B4 :410-446 vs 实码 :410-437）：内容全部核验存在，不影响结论（QA §1.5-5）

### REG-FIN-LRVT-007

- **测试编号**：REG-FIN-LRVT-007
- **模块**：财务-成本管理（frontend/src/views/finance/FinanceCost.vue，472 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-007（Batch LRVT-3；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b3-qa-report.md`）
- **业务场景**：成本管理布局级回退（布局回退专项·税务成本批 #2，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/②/③）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（含 #extra :285-296，新增成本按钮 = HEAD 原位置 :286-288 + 追加 成本结构分析 :289 + 导出/导入 disabled+tooltip :290-295 = B2/B6 承载，非布局重排）→ stats-section（:300-302，与 HEAD :153-155 同构，4 卡文案逐字一致）→ advanced-search-panel（:307-335，与 HEAD :156-171 同构；**日期控件 daterange → monthrange 重做** :318-328，处置②：type="monthrange" + value-format="YYYY-MM" + start/end-placeholder）→ table-section（:338-378，与 HEAD :175-184 同构；追加 B4 CaliberNoteBar :339 + B3 EmptyState 双态 :340-355 + B5 能力层 props :362-365，均为 B 修复点嵌入；**expand 已移除**）→ pagination-wrapper（独立分页恢复 :381-390，与 HEAD :186-188 同构，处置⑥：@size-change/@current-change → handleSizeChange/handleCurrentChange 直接 loadData :162-168）→ 详情 el-dialog（:393-407，与 HEAD :197-207 同构；处置③ expand 回迁 8 字段，descriptions 项顺序逐一一致：日期/成本类别/预算金额/实际金额/偏差/偏差率/部门/说明）→ CostFormDialog（:410，与 HEAD :209 同构，组件零改动）→ 成本结构分析 el-dialog（:413-455，B2 重放新增视图区块置于末尾，入口承载 #extra :289，任务卡允许非布局重排）
  - **已移除确认（grep 扫描）**：`workbench-toolbar` / `pendingSummary` 仅头部注释提及（:10/:17），模板与样式零命中；**`#expand` / `expandable` 全文件零命中**（含注释）；`:pagination` / `@page-change` 零命中（分页并入已移除）；`.workbench-*` 样式已删（仅 `.analysis-period-row` :461-471 随 B2 对话框保留，var(--fts-*) tokens 视觉同源）
  - **重放修复点 B1~B6 全有效**：B1 monthrange 月期间筛选端到端（处置② 重点）（:56-60 searchForm.dateRange 月期间字符串数组 / :125-150 loadData params.startDate/endDate :134-137 / :318-328 monthrange date-picker `type="monthrange"` + `value-format="YYYY-MM"` 嵌入原筛选区——**端到端链路全闭合（真实筛选，非本地过滤）**：控件(YYYY-MM) → FinanceCostQueryForm.startDate/endDate（types/finance.ts:454-458）→ **cost.ts mapQueryParams:41-42 映射 startPeriod/endPeriod（api 层既有修复零改动，mtime 2026/9/4 早于本批）** → GET /v1/finance/costs → CostController.java:53-57 → CostRecordQueryDTO → **CostServiceImpl.java:63-65 `wrapper.ge(CostRecord::getPeriod, startPeriod)` / :66-68 `le(..., endPeriod)` 后端实锤** → MyBatis-Plus SQL period 区间过滤——消费链无断点、无死参数）；B2 成本结构分析入口 + 视图（:213-241 loadCostSummary → costApi.summarizeByPeriod :221 / :244-247 openAnalysis 进入即加载 / :289 #extra「成本结构分析」入口 / :413-455 分析 el-dialog：期间选择 :416-423 + EmptyState 双态 :426-439 + DataTable show-summary :440-451——**端到端实锤**：cost.ts:122-125 GET /v1/finance/costs/summary?period=YYYY-MM → **CostController.java:60-64 既有端点** → **CostServiceImpl.java:91-101（period eq + costType merge 聚合 Map<Integer,Long> 分）** → :228 **fenToYuanNumber 分→元一次转换**（money.ts:44-47 实锤：fen/100 → toFixed(2)，**无二次 /100**，WS-006-R1 同类防回归点）→ CostTypeMap.toFrontend 类型映射 :224 → :210 analysisSummaryMethod useSummary unit='yuan' 合计——**非新增接口**）；B3 失败透传 + 引导空态（:66 loadFailed / :141-147 catch→loadFailed+ElMessage.error / :153-155 handleRetry / :340-356 EmptyState error 重试 :340-347 + no-data 引导 :348-355，hasActiveFilter :69/:351-352 区分「无筛选引导新增 / 有筛选引导调整条件」——HEAD 原 catch 仅清空 → 显式失败态 + 重试入口 + 引导空态）；B4 口径标注（:339 CaliberNoteBar 嵌入原布局表格上方，PD-031/032 文案，纯展示零数据依赖）；B5 合计行（:122-124 summaryMethod = createAmountSummaryMethod({amountProps:['budgetAmount','amount','variance'], unit:'yuan'}) / :363-364 DataTable show-summary :summary-method——能力层保留；records computed :85-92 金额保持元数值（HEAD 转字符串展示 → 现数值 + formatAmount 展示等价，且支撑 useSummary 数值合计），金额口径零变更）；B6 导出/导入登记（:290-295 #extra 导出 disabled+tooltip【P1-FIN-EXPORT-001 → 后端池 P1-FIN-EXPORT-004 登记链】+ 导入 disabled+tooltip【CostController 无 import 端点登记】——登记类入口隐藏不失效，不伪造）
  - **处置③ 详情回迁（8 字段）**：详情 el-dialog :393-407 = **HEAD 原列结构**（日期/成本类别/预算金额/实际金额/偏差/偏差率/部门/说明 = 8 个 descriptions 项，与 HEAD :197-207 顺序逐一一致）；expand 已移除（`#expand`/`expandable` 全文件零命中）；金额展示 HEAD 字符串直显 → 现数值 + formatAmount 千分位格式化（:73-75），展示等价；偏差率/部门/说明字段原样
  - **能力层保留（处置⑥ 不回退）**：density="auto" :362 / show-summary + useSummary unit='yuan'（B5）/ `:actions-width="160"` :365 = **与 HEAD 一致**；能力组件（CostFormDialog/DataTable/EmptyState/CaliberNoteBar/useSummary）零改动
  - **强制核验**：布局同构（git diff 归因 = B1~B6 修复点嵌入 + PageHeader#extra 承载 + CaliberNoteBar + EmptyState + 能力层 props；Cost +280/-24）/ 后端零改动 + api 层零改动（mtime 时间戳核验：CostServiceImpl/CostController 2026/8/11、cost.ts 2026/9/4 均早于本批 9/5；9/5 10:55-10:56 仅 FinanceTax/FinanceCost 两文件被修改；本文件仅调用既有 api 方法全部指向既有端点）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 筛选端到端零死参数（monthrange 链 控件→mapQueryParams:41-42→Service:63-68 ge/le 实锤）/ 构建独立复跑 typecheck 136=136 两文件零错误 + build EXIT=0 / 批隔离（P1-STOCK-001 零触碰；LRVT-005/007 标记仅本批两文件）
- **测试目的**：防止成本管理布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel【monthrange】→table-section→pagination-wrapper→详情 el-dialog 8 字段→CostFormDialog→B2 分析对话框末尾）；待办区/工具栏/#expand 不得复活（模板样式零残留）；重放修复点 B1~B6 不得回退（monthrange 端到端 ge/le 实锤/成本结构分析分→元一次转换/失败透传双态/CaliberNoteBar PD-032/合计行 unit='yuan'/导出导入登记）；处置③ 详情 8 字段回迁保持；能力层（density/show-summary unit='yuan'）不回退。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/FinanceCost.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats→advanced-search-panel→table-section→pagination-wrapper→详情 el-dialog→CostFormDialog→B2 分析对话框）
  2. grep 扫描：`workbench-toolbar` / `pendingSummary` / `#expand` / `expandable` / `:pagination` / `@page-change` / `V3-A`——仅注释提及、模板/样式零命中；`.workbench-*` 样式删除确认
  3. 逐修复点核验 B1~B6 存在 + 消费链闭合（B1：控件→params→cost.ts mapQueryParams:41-42→DTO→Service:63-68 ge/le 实锤；B2：summarizeByPeriod 既有端点 + fenToYuanNumber 分→元一次转换无二次 /100；B3：失败透传双态；B4：CaliberNoteBar PD-032；B5：合计 unit='yuan'；B6：导出导入登记）
  4. 处置③ 核验：详情 8 字段与 HEAD 顺序逐一一致；expand 模板零命中
  5. 能力层核验（density/show-summary unit='yuan'/actions-width 160 与 HEAD 一致）
  6. 强制核验独立复跑：typecheck 136=136（FinanceCost.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + 能力层属性级透传）；待办区/工具栏/#expand 模板样式零残留（仅注释）
  - 重放修复点 B1~B6 全部落位有效（monthrange 月期间全链 ge/le 实锤真实筛选非本地过滤 / 成本结构分析分→元一次转换无二次 /100 / 失败透传显式失败态+引导空态 / CaliberNoteBar PD-032 / 合计行 unit='yuan' / 导出导入登记 disabled+tooltip）
  - 详情 el-dialog 8 字段回迁（与 HEAD 顺序一致）；能力层保留（density / show-summary unit='yuan' / actions-width 与 HEAD 一致）；api 层/后端零改动
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤核验 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】成本结构分析聚合金额为分（后端既有口径）：CostServiceImpl:91-101 返回 Map<Integer,Long> 分 → 前端 fenToYuanNumber 一次转换（:228）；后端字段语义/前端转换均既有（ui-ws-b5 批已验收），无二次 /100 回归点（QA §2.5-1）
  - 【观察项·登记】analysisPeriod 默认当前月（:180/:199-202 currentMonth）：与后端 period 期间语义一致（YYYY-MM），非猜测规则（QA §2.5-2）
  - 【观察项·登记】无浏览器活体：同 §1.5-4 口径——交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-007）与产品逐页走查承接（QA §2.5-3）
  - 【证据微差·登记】developer 证据行号少量漂移（如 B2 :213-241 一致、B6 :289-296 vs 实码 :290-295）：内容全部核验存在，不影响结论（QA §2.5-4）

---

### Batch LRVT-3 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告（`docs/quality/ui-lrvt-b3-qa-report.md`）观察项如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **【观察项·登记】payTax 未传 voucherNo = WS 批既有状态**（§1.5-1）：handlePayTax :261-270 payTax 载荷无 voucherNo 字段 → 后端 PD-001 isIdempotencyKeyComplete（TaxRecordServiceImpl.java:108-113）判定幂等键不完整 → 走正常新增路径（非幂等命中）；此为 WS-004 批既有状态（ui-ws-b4-qa-report § 缴税幂等已验收同结构 :251-260，与诊断 08-31 版一致），本批重放保持未变更；PD-001 后端幂等实现零触碰属实；如产品要求缴税幂等命中，需前端补 voucherNo（另行立项，不混入本批）
2. **【观察项·登记】Tab1 无分页 UI 与 HEAD 一致**（§1.5-2）：recordPagination（:93 useStandardPage defaultPageSize=100）无分页 UI——恢复 HEAD 原样（HEAD Tab1 本就无独立分页），pageSize=100 仅作 getRecords 参数载体被 API 消费（pageNum/pageSize 实传），非死参数；列表首屏加载 100 条与 WS 批既有行为一致
3. **【观察项·登记】分析聚合金额为后端既有分口径**（§2.5-1）：成本结构分析 CostServiceImpl:91-101 返回 Map<Integer,Long> 分 → 前端 fenToYuanNumber 一次转换（:228）；后端字段语义/前端转换均既有（ui-ws-b5 批已验收），无二次 /100 回归点（WS-006-R1 同类防回归）
4. **【观察项·登记】无浏览器活体（留走查）**（§1.5-4/§2.5-3）：本环境无浏览器/接口运行条件，交互核验为代码路径级 + 构建级；运行时点击走查留 REG-FIN-LRVT-005/007 与产品逐页走查承接（同既往批次浏览器缺口口径）
5. **【证据微差·登记】developer 证据行号少量漂移**（§1.5-5/§2.5-4）：如 B2 :246-287 vs 实码 :246-279、B4 :410-446 vs 实码 :410-437、B6 :289-296 vs 实码 :290-295——内容全部核验存在，不影响结论（同既往批次行号口径差异模式）
6. **【衔接·登记】既往条目断言过时衔接**（任务板 §13.4「回归基线衔接」）：布局级回退使 REG-FIN-WS-004（合规工作台五区布局待办区/工具栏）、REG-FIN-WS-005（毛利核算工作台五区布局待办区/工具栏）、REG-UI-FIN-001（SearchPanel 组件化接入，Tax/Cost 恢复手写筛选区）部分断言过时——现状已由本批 REG-FIN-LRVT-005/007 新增断言覆盖（布局同构 + B1~B7/B1~B6 重放 + 能力层保留 + **KL-059 税期动态化 / monthrange 端到端**）；本批按 105 只增不减零修改执行（不删除不覆盖），原条目录内「断言已过时」标注留待后续批次/architect 统一落位

---

### Batch LRVT-3 门禁

```text
Regression Result: PASS（2026-09-05 Batch LRVT-3 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为布局级回退批次（任务池 §13），非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：2/2 卡 QA 独立验收 PASS（`docs/quality/ui-lrvt-b3-qa-report.md`，2026-09-05：P1-FIN-LRVT-005 税务管理 / P1-FIN-LRVT-007 成本管理，无 FAIL、无 -R；布局与 HEAD 同构 + 重放修复点 B1~B7/B1~B6 全有效【getRecords 三 eq 端到端实锤 / payTax+PD-001 幂等零触碰 / KL-059 税期动态化 YYYYMM 契约零变更 / monthrange 端到端 ge/le 实锤真实筛选 / 成本结构分析分→元一次转换防二次 /100】+ 处置③ 详情回迁 8 字段 + 能力层保留 + 口径标注 + 强制核验 6/6 全过 + typecheck 136=136 两文件零命中 + build EXIT=0 独立复跑）
- FAIL 明细：0
- PWL 明细：0（观察/登记项 5 条均为非阻断登记，不构成 FAIL；PD-028/PD-035 为 BLOCKED 不占通过数；导出/批量缴税/导入为登记依赖非 FAIL）
- 基线：新增 REG-FIN-LRVT-005/007（正式 105 → 107 条，只增不减、既有条目零删除零修改；候选 0 条维持）
- 布局回退专项核对：Batch LRVT-3 两卡完整性核对 = **REG-FIN-LRVT-005/007 全部在案**（税务成本批闭环，无缺卡、无预固化条目）；专项累计 **101 → 107 = 6 条**（LRVT-1：001/002 + LRVT-2：003/004 + LRVT-3：005/007）；专项编排 Batch LRVT-1~4 逐批执行（005/007 = LRVT-3；006/008/009 = LRVT-4）；**等待产品负责人逐页走查**（roadmap §5.14，完成后停止，不自动开新批）
- 观察项：payTax 未传 voucherNo=WS 批既有状态（幂等键不完整走正常新增）/ Tab1 无分页 UI 与 HEAD 一致（pageSize=100 参数载体非死参数）/ 分析聚合金额=后端既有分口径（一次转换无二次 /100）/ 无浏览器活体（留走查）/ developer 证据行号漂移 / 既往条目断言过时衔接（REG-FIN-WS-004/005、REG-UI-FIN-001 部分断言，现状由本批基线覆盖）——如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0（登记项不构成 FAIL）→ 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）
```

## Batch LRVT-4 回归（布局级回退批次：P1-FIN-LRVT-006 + 008 + 009 财务报表/自动凭证/发票报销批【专项最后一批】，2026-09-05）

> 验收依据：`docs/quality/ui-lrvt-b4-qa-report.md`（2026-09-05，QA 独立验收：P1-FIN-LRVT-006 财务报表 = **PASS** / P1-FIN-LRVT-008 自动凭证 = **PASS** / P1-FIN-LRVT-009 发票报销 = **PASS**——无 FAIL、无 -R；布局与 HEAD 逐块比对同构 + 重放修复点 B1~B7 / B1~B6 全部落位有效【Report：9 类端点聚合 7 有效+2 P0 断流实锤 / KL-057 断流空态不伪造 / 后端占位说明 / drillTo 19 处→独立页 / **WS-006-R1 金额口径重放分→元一次转换防二次 /100**；AutoVoucher：CRUD 既有端点 / B-3-B4 映射端到端 / KL-061 伪语义列移除 / 契约对齐 toggleEnabled enabled 必填；InvoiceReimbursement：红冲/作废既有端点 / E 值格式 / KL-060 登记不猜测】+ 能力层保留 + 后端/API 层零改动 + 强制核验 6/6 全过 + typecheck 136=136 三文件零命中 + build EXIT=0 独立复跑）；裁决依据 = `remediation-roadmap.md` §5.14（2026-09-05 产品负责人指令：9 页 A+B 全部回退原布局 + 重放修复 + 重放处置 6 项已定）；回退基线 = `git show HEAD:frontend/src/views/finance/FinanceReport.vue` / `HEAD:frontend/src/views/finance/AutoVoucher.vue` / `HEAD:frontend/src/views/finance/InvoiceReimbursement.vue`（HEAD cd1c89b = 回退前原始基线）+ 盘点报告 `docs/quality/finance-layout-revert-inventory-20260905.md`（§2.6/§2.8/§2.9 before 结构）；对照基准 = 任务板 §13.4 P1-FIN-LRVT-006/008/009 卡（统一验收标准 6 条 + 卡内细化验收标准）+ §13.4.4 developer 修改证据；开发者未自行宣布通过 ✅——本报告为唯一验收结论。
> 新增依据：仅基于 QA 已验收 PASS 结论新增 REG-FIN-LRVT-006/008/009（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**治理批次**（布局级回退，任务池 §13），非发布批次——回归结论 PASS 表示基线持续积累与 FAIL=0 维持，不构成上线门禁判定；历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变；**布局回退专项 Batch LRVT-1~4 全部批次完成（本批 = 最后一批），专项收口：9 页布局级回退 + 功能修复重放全部 QA 通过，等待产品负责人逐页走查（roadmap §5.14，完成后停止，不自动开新批）**。
> 基线计数核对：追加前正式基线 **107 条**（文件实况：基线总览表 107 行，含 REG-FIN-LRVT-001~005/007；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 3 条（REG-FIN-LRVT-006/008/009）后 **110 条**，只增不减纪律达成（既有 107 条条目零删除零修改）；候选 0 条维持。
> 断言衔接登记（任务板 §13.4「回归基线衔接」原则，本批按 107 只增不减零修改执行，不删除不覆盖）：布局级回退使既往条目部分断言过时——REG-FIN-WS-006「决策工作台五区布局（待办区/工具栏）」断言（Report 待办区/工具栏随布局回退移除；WS-006 其余断言【9 类报表聚合 / P0 断流空态 / 穿透 / **WS-006-R1 金额口径 R1 闭环**】由本批 REG-FIN-LRVT-006 接管覆盖）、REG-FIN-WS-003「记录工作台」AutoVoucher/InvoiceReimbursement 页签化布局断言（两页恢复独立组件直达 + 手写筛选区；WS-003 其余断言已由 RVT-003 接管标注）——现状由本批 REG-FIN-LRVT-006/008/009 新增断言覆盖（布局同构 + 重放修复点 + 能力层保留 + **WS-006-R1 金额口径重放 / KL-061 伪语义列移除 / KL-060 登记保持**）；原条目录内标注留待后续批次/architect 按 §13.4 统一落位（本批零修改纪律优先）。

### REG-FIN-LRVT-006

- **测试编号**：REG-FIN-LRVT-006
- **模块**：财务-财务报表（frontend/src/views/finance/FinanceReport.vue，539 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-006（Batch LRVT-4；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b4-qa-report.md`）
- **业务场景**：财务报表布局级回退（布局回退专项·最后一批 #1，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/①）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（含 #extra :455-463：打印 :458 + 导出 disabled+tooltip :459-461，处置① 非布局重排）→ stats-section 固定 4 卡网格（:466-468 `gridTemplateColumns: 'repeat(4, 1fr)'` 与 HEAD 逐字一致）→ advanced-search-panel（:471-479：报表类型 select 9 类 :472-474【B1 扩展 3 类→9 类，筛选区内选项扩展非布局重排】+ monthrange :475 `value-format="YYYY-MM"` = B 修复保持 + 查询/重置 :477-478）→ table-section（:482-525：CaliberNoteBar :483 + EmptyState×3 :484-507 + DataTable 异构列 :508-524【B2/B3/B6 承载 + B4 actions 列】）→ pagination-wrapper（:528-530，独立分页恢复 HEAD 原结构 @size-change/@current-change → handleSizeChange/handleCurrentChange :414-422，处置⑥）
  - **已移除确认（grep 扫描）**：`workbench-toolbar`（处置① 打印/导出挂 PageHeader#extra）/ 统计卡片动态化 v-if + 动态列数（回 HEAD 固定 4 卡网格）/ 分页并入 DataTable（处置⑥）/ 工作台样式——模板/样式零残留（仅注释 :9 命中）；`workbench-taskboard|workbench-toolbar|task-item|SearchPanel|#expand|expandable|@page-change|V3-A|xxx-page 容器` 残留扫描仅注释 1 命中
  - **布局差异如实登记**（不判 FAIL）：① HEAD 表格列 = 3 套异构列（profit：item/currentPeriod/previousPeriod/changeRate；balance：category/item/amount；cash_flow：activity/item/inflow/outflow/net），当前版 = aging/cost_structure 特化列 + 默认（item/amount/extra）——与 B1 契约对齐冲突按「B 修复点优先嵌入原布局」处置（QA 独立实证成立：FinancialReportServiceImpl.getProfitStatement :49-71 仅返回 5 聚合字段（revenueAmount/cogsAmount/grossProfit/operatingExpenses/netProfit）无 details/currentPeriod/previousPeriod/changeRate，恢复 HEAD profit 列必然断链；当前版按真实契约消费 :107-115，属修复不失效）；② income_expense 统计卡片 3 张（固定 4 卡网格 1 空位）——网格固定 4 列与 HEAD 一致，卡片数由真实数据字段决定（收入/支出/净收支 3 项），不属布局结构差异
  - **重放修复点 B1~B7 全有效**：B1 9 类报表端点聚合（:67-77 reportTypeOptions 9 类 / :342-371 loadData 分派：getProfitStatement :342 / getIncomeExpenseSummary :344 / getReceivableStatistics :346 / getPayableStatistics :348 / getAgingAnalysis :350 / getCostStructure :358 / getBudgetExecution :360 / balance :368 / cash_flow :371——**后端实锤**：ReportController.java **恰 7 端点** :30-83（profit-statement/income-expense-summary/receivable-statistics/payable-statistics/aging-analysis/cost-structure/budget-execution），**无 /balance-sheet、/cash-flow**；api 层 report.ts:64/:80 注释明示 404【TODO P1 后端无 /balance-sheet 端点】——**7 有效 + 2 P0 断流实锤**；report.ts mtime 8/11 零改动）；B2 P0 断流处置（KL-057）（:58-59 p0Broken / :379-382 catch → balance/cash_flow 置 p0Broken=true :381 + console.error 注明 KL-057/P1-FIN-BE-001 / :486-493 EmptyState error「后端端点缺失」+ description「已登记 P1-FIN-BE-001 后端池排期，不显示空数据、不伪造」——404 → 空态说明不伪造不悬空）；B3 后端占位说明（:60-61 placeholderNote / :349-356 aging 全零 → 占位说明 :353 / :359-365 budget message 含「待实现」→ 占位说明 :362 / :494-501 EmptyState no-data「后端占位数据（不展示虚构数值）」——**后端实锤**：getAgingAnalysis :156-176 恒返回 4 桶 count=0/amount=0（注释「简化处理」:169）；getBudgetExecution :220-230 返回「预算执行数据待实现」+ 恒 0——占位检测条件与后端事实一致，不显示虚构数值）；B4 报表→明细穿透（drillTo 19 处静态引用：:111 营业成本→/finance/cost / :125-130 应收 6 行→/finance/receivable / :134-139 应付 6 行→/finance/payable / :147 账龄→/finance/receivable / :156 成本结构→/finance/cost / :162-165 预算 4 行→/finance/budget；:430-432 handleDrill → router.push / :520-523 行级「明细」按钮 + 不可行「—」.drill-na :536-538——**路由实锤**：router/index.ts:117-120 /finance/receivable、/finance/payable、/finance/cost、/finance/budget 独立路由存在（§12 RVT-004 已恢复），19 处引用全部指向有效路由，无失效目标；不可行项「—」登记不伪造）；B5 打印（:425-427 handlePrint → window.print / :458 #extra 打印按钮——浏览器原生打印，零接口依赖）；B6 失败透传+重试（:56-57 loadFailed / :375-389 catch → loadFailed=true :378 + ElMessage.error :385 / :393-394 handleRetry → loadData / :484-493 EmptyState error 重试 action-text + @action——catch 静默 → 显式失败态 + 重试入口）；**B7 金额口径（WS-006-R1 重放，重点）**（:260-275 cost_structure 统计卡片分支：:263-266 typeAmountFen 直接 `Number(row?.amount || 0)` **保持分无 fenToYuanNumber** / :267 totalFen = fenOf(d,'totalCost') **分** / :271-274 四卡全部 `formatFenToYuan(分)` **一次转换**；:272 守卫 `totalFen > 0 ? materialFen : 0`——**口径链实锤**：money.ts:79-85 契约 = formatFenToYuan(分)=fenToYuanNumber(分)=分/100（:44-47），输入契约「分」；四卡输入全部为分，无「已转元值传入 formatFenToYuan」二次 /100；与 ui-ws-b5-r1-qa-report.md 同型实证（totalCost=100000 分 → ¥1,000.00 / 食材 60000 分 → ¥600.00 / 人工 20000 分 → ¥200.00 / 其他 20000 分 → ¥200.00；分口径自洽 totalFen = 三卡之和）；后端 getCostStructure :179-217 details.amount/totalCost 均为分——**F-1 防回归点保持**）
  - **导出登记**（:459-461 #extra 导出 disabled+tooltip，P1-FIN-EXPORT-001 → 后端池 P1-FIN-EXPORT-004——登记类入口隐藏不失效，不伪造）
  - **能力层保留（处置⑥ 不回退）**：density="auto" :514 / show-summary + useSummary costSummaryMethod unit='yuan' :198/:515-516（仅 cost_structure 合计）/ 固定列 item left :178/:185/:191 / CaliberNoteBar :483 嵌入原布局表格上方 / 独立分页 :528-530
  - **金额口径全页复核**：profit/income_expense/receivable/payable/aging/budget 各分支金额均 `fenToYuanNumber(分)`（表格行）或 `formatFenToYuan(分)`（卡片）一次转换（:110-114/:119-121/:125-130/:134-139/:145/:154/:162-164/:211-214/:222-224/:229-232/:237-240/:248/:271-274/:280-282）；无二次 /100；money.ts 强制规范（禁止组件内 *100//100 :8-9）零违反
  - **强制核验**：布局同构（git diff 归因 = B1~B7 修复点嵌入 + PageHeader#extra 承载 + 头部注释，Report +405/-137 与 developer 证据一致）/ 后端零改动 + api 层零改动（mtime 实锤：report.ts 8/11 早于本批 9/5；git status backend/ 无本批新增 finance 文件）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 金额口径正确 + 穿透 19 处指向独立页不失效 + P1-STOCK-001 零触碰（批隔离：frontend/src 全仓 grep 仅本批 3 文件 + FinanceLedger.vue:24 LRVT-1 前向引用注释命中，非本批污染）/ 构建独立复跑 typecheck 136=136 三文件零错误 + build EXIT=0（QA 独立执行非采信开发自测）
- **测试目的**：防止财务报表布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra→stats 固定 4 卡网格→advanced-search-panel→table-section→pagination-wrapper）；待办区/工具栏/统计卡片动态化/分页并入不得复活（模板样式零残留）；重放修复点 B1~B7 不得回退（9 类端点聚合 7 有效+2 P0 断流 / KL-057 断流空态不伪造 / 后端占位说明 / drillTo 19 处→独立页 / 打印 / 失败透传+重试 / **WS-006-R1 金额口径分→元一次转换无二次 /100**）；能力层（density/useSummary unit='yuan'/固定列/CaliberNoteBar）不回退；导出登记不失效。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/FinanceReport.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats 固定 4 卡网格→advanced-search-panel→table-section→pagination-wrapper）
  2. grep 扫描：`workbench-taskboard` / `workbench-toolbar` / `task-item` / `SearchPanel` / `#expand` / `expandable` / `@page-change` / `V3-A` / `xxx-page 容器`——仅注释提及、模板/样式零命中
  3. 逐修复点核验 B1~B7 存在 + 消费链闭合（B1：9 类分派 → 后端 ReportController :30-83 7 端点实锤 + report.ts:64/:80 404 注释（2 P0 断流）；B2：404 → EmptyState 空态说明 + P1-FIN-BE-001 登记；B3：账龄恒 0/预算待实现 → 占位说明（后端 :156-176/:220-230）；B4：drillTo 19 处逐一枚举 ↔ router/index.ts:117-120 4 路由存在性；B7：cost_structure 四卡输入为分 + formatFenToYuan(分) 一次转换（money.ts:79-85 契约），二次 /100 零命中）
  4. 能力层核验（density / show-summary unit='yuan' / 固定列 / CaliberNoteBar / 独立分页）
  5. 强制核验独立复跑：typecheck 136=136（FinanceReport.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + #extra 承载）；待办区/工具栏/统计卡片动态化/分页并入模板样式零残留（仅注释）
  - 重放修复点 B1~B7 全部落位有效（9 类端点聚合 7 有效+2 P0 断流实锤 / KL-057 断流空态不伪造 / 后端占位说明 / drillTo 19 处→独立页无失效 / 打印 / 失败透传+重试 / **WS-006-R1 金额口径重放：四卡分→元一次转换无二次 /100，F-1 防回归**）
  - 能力层保留（density / show-summary unit='yuan' / 固定列 / CaliberNoteBar）；导出登记 #extra disabled+tooltip 不失效
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤核验 + 金额口径链核验 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】income_expense 统计卡片 3 张（固定 4 卡网格 1 空位）（QA §1.1-②/§6-3）：网格结构固定 4 列与 HEAD 一致，卡片数由真实数据字段决定（收入/支出/净收支 3 项），不属布局结构差异；产品走查时确认展示预期，若需 4 卡补齐属产品决策，不在本卡范围
  - 【观察项·登记】balance/cash_flow 报表类型下统计卡片不显示（statsCards 返回 []，:286）（QA §1.5-2）：HEAD 行为为显示初始 ¥0.00 4 卡；当前为 B2「P0 断流不显示虚构零值卡片」修复行为，与 B2 空态说明配套，不判差异为回退不彻底
  - 【观察项·登记】budget 执行率计算 rate * 100（:165/:283）（QA §1.5-3）：后端 executionRate 恒 BigDecimal.ZERO（占位），当前无实际影响；后端落地后需确认 executionRate 语义（0~1 小数 vs 0~100 百分比），登记观察（P1-FIN-BE-001 落地时一并确认）
  - 【观察项·登记】表格分页为本地切片（tableData :290-293 gridRows.slice）（QA §1.5-4）：与 HEAD 同款行为（allTableData.slice HEAD），非本批引入
  - 【观察项·登记】无浏览器活体（QA §1.5-1）：本环境无浏览器/接口运行条件，交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-006）与产品逐页走查承接

### REG-FIN-LRVT-008

- **测试编号**：REG-FIN-LRVT-008
- **模块**：财务-自动凭证（frontend/src/views/finance/AutoVoucher.vue，512 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-008（Batch LRVT-4；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b4-qa-report.md`）
- **业务场景**：自动凭证布局级回退（布局回退专项·最后一批 #2，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/②/①）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（含 #extra :390-401：新建规则 :393-395 + 导出 disabled+tooltip :396-398 + 打印 :399，处置①）→ stats-section repeat(4,1fr)（:403-405）→ advanced-search-panel 手写筛选区（:409-417：ruleStatus select + keyword input + 查询/重置，**与 HEAD 158-165 逐字同构**，仅多 @keyup.enter 行为增强）→ table-section（:419-445：EmptyState error 重试 :420-427【B5 承载】+ DataTable density :428-444）→ pagination-wrapper（:447-449，独立分页恢复 HEAD 原结构 @size-change/@current-change → handleSizeChange/handleCurrentChange :207-216，处置⑥）→ 详情 el-dialog（:452-468）→ 新建/编辑规则 el-dialog（:471-510，B1 新增视图区块，处置① 入口挂 #extra，非布局重排）
  - **已移除确认（grep 扫描）**：`workbench-taskboard`（处置⑤：KL-061 登记卡随区块删除，登记说明落头部注释 :26-28）/ `workbench-toolbar`（处置①）/ `auto-voucher-page` 容器包裹 / SearchPanel 组件化筛选容器（处置②）/ 分页并入 DataTable（处置⑥）/ 工作台样式——模板/样式零残留（仅注释 :8-10/:22/:92 提及）
  - **HEAD 断链字段对照（B4 契约对齐必要性实证）**：HEAD 消费 t.id/t.enabled/t.frequency/t.debitSubject/t.creditSubject/t.formula/t.updateTime——TransferTemplateVO.java **无这些字段**（templateId :18 / templateType :24 / sourceSubjectId :27 / targetSubjectId :30 / amountExpression :33 / summaryTemplate :36 / isEnabled :39 / createTime :49）；HEAD 列（触发条件 frequency / 最近执行 updateTime / 计算公式 formula / 借:debit 贷:credit）全部断链 → 当前版契约对齐为 B 修复重放，非布局差异
  - **重放修复点 B1~B6 + 附加 全有效**：B1 联动规则 CRUD 入口（:312-316 handleCreate / :340-380 submitForm → transferTemplateApi.update :367 / create :370 / :260-275 handleDelete → delete :267 + ElMessageBox 二次确认 :262-266 / :226-231 onMounted subjectApi.getLeafSubjects 真实加载 / :470-510 新建/编辑对话框（字段对齐 DTO：templateName/templateType/sourceSubjectId/targetSubjectId/amountExpression/summaryTemplate/isEnabled/remark）/ :393 #extra 新建规则入口 / :438-443 行内 actions（详情/编辑/启停/删除）——**端点实锤**：transfer-template.ts:85-87 POST /v1/finance/transfer-templates（create）、:95-97 PUT /{id}（update）、:103-105 DEL /{id}（delete）→ TransferTemplateController.java:30-33/:37-40/:44-47 既有端点 + @PreAuthorize 既有；subject.ts:142-147 getLeafSubjects → GET /v1/finance/subjects/leaves 既有端点；**行内 actions 渲染机制实锤**：DataTable.vue:5-6（`#actions` 插槽自动在表格末尾追加操作列，无需在 columns 定义）+ :103-105/:254-264——columns 无 actions 列但 #actions 模板有效（HEAD 同机制））；B2 B-3/B-4 映射修复（:133-140 页面参数传递：keyword → params.templateName :134 / ruleStatus active/inactive → params.enabled :137-139；api 层 transfer-template.ts:28-41 mapQueryParams：enabled→isEnabled :34-36、templateName→keyword :37-39 **既有修正零改动**——**端到端实锤**：页面参数 → mapQueryParams（api 层，mtime 9/3 23:54 早于本批零改动）→ TransferTemplateQueryDTO.java:19（isEnabled）/ :22（keyword）字段绑定 → TransferTemplateController.getPage:58-61 消费，无死参数）；B3 KL-061 伪语义列移除（:190-191 columns「最近执行」→「创建时间」createTime / :144-155 displayRecords 映射 createTime = t.createTime \|\| '-' :152 / :462 详情对话框「创建时间」字段 / :26-28 头部注释 KL-061 登记——伪语义列（HEAD lastRunTime = updateTime 冒充执行时间）→ 创建时间真实字段（VO.createTime :49 实锤），不伪造；登记说明落代码注释，同 LRVT-002 B7 承载方式）；B4 契约对齐 TransferTemplateVO（:145-154 displayRecords：id = String(t.templateId) :146 / businessType = templateTypeLabelMap[t.templateType] 数字 :148 / sourceSubjectId·targetSubjectId :149 / status = isEnabled :153；:246-257 handleToggleEnabled 补 enabled 参数 :251；:58-63 templateTypeLabelMap 1-损益结转/2-定期计提/3-其他，与 VO :23 注释一致——templateId/templateType 数字直传 + toggleEnabled enabled 必填参数：transfer-template.ts:116-122（toggleEnabled(id, enabled) PUT /{id}/toggle-enabled?enabled=）+ TransferTemplateController.java:66（`@RequestParam Boolean enabled` 必填实锤）；getById 未使用（:76-78 存在）——页面无详情端点依赖）；B5 失败透传+空态（:47 loadFailed / :157-163 catch → loadFailed=true + ElMessage.error :163 / :420-427 EmptyState error 重试 @action=loadData——catch 静默 → 显式失败态 + 重试入口）；B6 打印（:219-221 handlePrint → window.print / :399 #extra 打印按钮——浏览器原生打印，零接口依赖）；附加：筛选持久化（:93 FILTER_STORAGE_KEY='fts_search_finance-transfer-template-filter' / :94-115 saveFilter/restoreSavedFilter/clearSavedFilter / :123 watch deep / :195-204 handleSearch 保存 / handleReset 清空 / :224 onMounted 先恢复后加载——键前缀 fts_search_ 与 SearchPanel 约定一致，旧存档不丢失）；导出登记（:396-398 #extra 导出 disabled+tooltip，P1-FIN-EXPORT-001 → P1-FIN-EXPORT-004——登记类入口隐藏不失效）
  - **能力层保留（处置⑥ 不回退）**：density="auto" :434 / 独立分页 :447-449 恢复 HEAD 原结构 / 统计卡片 4 项 :170-182（有效规则/规则总数/启用率/凭证模板数，当前页真实数据 isEnabled 口径）/ DataTable 自动操作列（组件层能力）
  - **强制核验**：布局同构（git diff 归因 = B1~B6 修复点嵌入 + #extra 承载 + 头注释，AutoVoucher +349/-39 与 developer 证据一致）/ 后端零改动 + api 层零改动（mtime 实锤：transfer-template.ts 9/3 23:54、subject.ts 9/4 1:19 早于本批 9/5；git status backend/ 无本批 finance 控制器）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 契约对齐（templateId 数字直传 + enabled 必填实锤）/ 构建独立复跑 typecheck 136=136 三文件零错误 + build EXIT=0 / 批隔离（P1-STOCK-001 零触碰）
- **测试目的**：防止自动凭证布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra→stats→advanced-search-panel 原手写筛选区→table-section→pagination-wrapper→详情 el-dialog→B1 新建/编辑规则对话框）；待办区/工具栏/SearchPanel/分页并入不得复活（模板样式零残留）；重放修复点 B1~B6 不得回退（CRUD 既有端点+getLeafSubjects+二次确认/B-3-B4 映射端到端/KL-061 伪语义列移除/契约对齐【templateId 数字直传+toggleEnabled enabled 参数】/失败透传/打印/筛选持久化）；能力层（density/独立分页/统计卡片/自动操作列）不回退；导出登记不失效。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/AutoVoucher.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats→advanced-search-panel→table-section→pagination-wrapper→详情 el-dialog→B1 新建/编辑规则对话框）
  2. grep 扫描：`workbench-taskboard` / `workbench-toolbar` / `auto-voucher-page` / `SearchPanel` / `#expand` / `expandable` / `@page-change` / `V3-A`——仅注释提及、模板/样式零命中
  3. 逐修复点核验 B1~B6 + 附加存在 + 消费链闭合（B1：create/update/delete 既有端点 + 二次确认 + getLeafSubjects 既有端点 + #actions 插槽机制；B2：keyword→templateName、active/inactive→enabled → mapQueryParams（api 层零改动）→ QueryDTO:19/:22 → Controller:58-61；B3：KL-061「最近执行」→「创建时间」真实字段 VO.createTime:49；B4：templateId 数字直传 + toggleEnabled enabled 参数 Controller:66 必填）
  4. 能力层核验（density / 独立分页 / 统计卡片 isEnabled 口径 / 自动操作列）
  5. 强制核验独立复跑：typecheck 136=136（AutoVoucher.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + #extra 承载 + B1 对话框末尾追加）；待办区/工具栏/SearchPanel/分页并入模板样式零残留（仅注释）
  - 重放修复点 B1~B6 全部落位有效（CRUD 既有端点端到端 / B-3-B4 映射无死参数 / KL-061 伪语义列移除 VO 断链实证 / 契约对齐 enabled 必填 / 失败透传 / 打印 / 筛选持久化 fts_search_）；附加（导出登记）不失效
  - 能力层保留（density / 独立分页 / 统计卡片 / DataTable 自动操作列）；typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤核验 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】displayRecords.id = String(t.templateId)（:146）→ toggleEnabled/delete/update 传 string id → 后端 @PathVariable Long 自动转换（既有模式，TransferTemplateController :38/:45/:66），无类型失配（QA §2.7-2）
  - 【观察项·登记】统计卡片「凭证模板数」= totalCount（当前页数）与「规则总数」同值——HEAD 同款语义（既有行为，非本批引入），登记观察不判错（QA §2.7-3）
  - 【观察项·登记】详情对话框 8 字段为 B4 契约对齐字段（金额表达式/摘要模板替代 HEAD 触发条件/计算公式——断链字段移除），行为差异有依据（QA §2.7-4）
  - 【观察项·登记】无浏览器活体：同 §1.5-1 口径——交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-008）与产品逐页走查承接（QA §2.7-1）

### REG-FIN-LRVT-009

- **测试编号**：REG-FIN-LRVT-009
- **模块**：财务-发票报销（frontend/src/views/finance/InvoiceReimbursement.vue，646 行；后端零改动）
- **关联任务**：P1-FIN-LRVT-009（Batch LRVT-4；QA 独立验收 **PASS** 2026-09-05 `docs/quality/ui-lrvt-b4-qa-report.md`）
- **业务场景**：发票报销布局级回退（布局回退专项·最后一批 #3，roadmap §5.14 + 任务板 §13.4）——
  - **布局与 HEAD 同构**（处置⑥/②/①）：模板区块顺序与 `git show HEAD`（cd1c89b）逐块一致——PageHeader（含 #extra :431-442：新建报销 :434-436 **回 HEAD 原位置** + 导出 disabled+tooltip :437-439 + 打印 :440，处置①）→ stats-section repeat(4,1fr)（:444-446）→ advanced-search-panel 手写筛选区（:450-467：invoiceNo input + invoiceType select + daterange :460【value-format="YYYY-MM-DD" = B2 嵌入】+ 查询/重置，**与 HEAD 309-326 同构**）→ table-section（:469-499：EmptyState error 重试 :470-477【B3 承载】+ DataTable density :478-498【#status :486-488 + #actions :489-497：审批/作废/红冲/详情】）→ pagination-wrapper（:501-510，独立分页恢复 HEAD 原结构 v-model:current-page/page-size + @size-change/@current-change=loadData，处置⑥）→ 详情 el-dialog（:513-535，14 字段与 HEAD 同构）→ 审批 el-dialog（:538-562【KL-060 说明 form-item :552-554 = B6 登记说明承载】）→ 新建报销 el-dialog（:565-635，与 HEAD 同构）
  - **已移除确认（grep 扫描）**：`workbench-taskboard`（处置⑤：报销审批待办卡 + KL-060 登记卡随区块删除，说明另寻承载）/ `workbench-toolbar`（处置①）/ `invoice-reimbursement-page` 容器包裹 / SearchPanel 组件化筛选容器（处置②）/ 分页并入 DataTable（处置⑥）/ 工作台样式——模板/样式零残留（仅注释 :8-11/:19/:44/:178 提及）
  - **重放修复点 B1~B6 + 附加 全有效**：B1 红冲/作废入口（:219-235 handleRedFlush：ElMessageBox 二次确认 :222-226 + invoiceApi.redFlush :227 / :237-253 handleVoid：二次确认 :239-243 + invoiceApi.void :245 / :492-495 行内 actions：issued 态作废/红冲按钮——**端点实锤**：invoice.ts:137-140 void → PUT /v1/finance/invoices/{id}/void、:149-152 redFlush → PUT /{id}/red-flush（mtime 8/11 零改动）→ InvoiceController.java:137-142（voidInvoice）/ :152-157（redFlush）既有端点 + @PreAuthorize 既有；状态机 :28「draft(0) → issued(5) → void(6) / red-flushed(4)」与行内条件 `row.status === 'issued'`（:492）一致——破坏性动作二次确认）；B2 E 值格式修复（:159-176 toDateString/restoreDateRange 字符串化 / :203-211 handleFilterRestored 日期兼容旧存档 / :460 筛选区 daterange value-format="YYYY-MM-DD"——日期字符串化与后端 LocalDate 兼容（HEAD 无 value-format = Date 对象 → ISO datetime 风险，诊断 §1.13 E 类）；嵌入原筛选区）；B3 失败透传+引导空态（:58 loadFailed / :146-149 catch → loadFailed=true + ElMessage.error :149「报销列表加载失败，请重试」/ :470-477 EmptyState error 重试 @action=loadData——catch 静默（HEAD）→ 显式失败态 + 重试入口）；B4 打印（:214-217 handlePrint → window.print / :440 #extra 打印按钮——浏览器原生打印，零接口依赖）；B5 筛选持久化（:178 FILTER_STORAGE_KEY='fts_search_finance-reimbursement-filter' / :179-201 saveFilter/restoreSavedFilter/clearSavedFilter / :212 watch deep / :42-46 onReset 重置即清空：resetSearchForm + clearSavedFilter :45 + loadData / :299-301 onMounted 先恢复后加载——键前缀 fts_search_ 与 SearchPanel 约定一致，旧存档不丢失；重置即清空（SearchPanel 同语义））；B6 KL-060 登记 + 文案精确化（:272-296 confirmApprove 注释 :274-277 KL-060 登记不猜测：驳回=作废共用 cancelled 语义混叠，无产品/后端依据 → 保持既有语义；:286 驳回 → status='cancelled' 保持既有登记链 / :292 ElMessage.error「发票审批失败，请重试」文案精确化（HEAD 为「操作失败，请重试」）/ :552-554 审批对话框 kl060-note 说明承载——登记说明承载于审批对话框内说明（待办区登记卡随区块删除后另寻承载完成，同 LRVT-002 B7 范式））；附加：导出登记（:437-439 #extra 导出 disabled+tooltip，P1-FIN-EXPORT-001 → P1-FIN-EXPORT-004——登记类入口隐藏不失效）
  - **能力层保留（处置⑥ 不回退）**：density="auto" :484 / 独立分页 :501-510 恢复 HEAD 原结构 v-model:current-page/page-size + @size-change/@current-change=loadData / 统计卡片 4 项 :100-113（发票总额/已开具/草稿/发票总数，当前页真实数据）
  - **金额口径复核**：列表展示（records computed :94-96 formatFen 分→元字符串）/ 统计卡片（:108 formatYuan(totalAmount) 分→元）/ 审批对话框（:542 formatFen(approveForm.totalAmount) 分→元，totalAmount 来自 rawRecords 分）/ 新建提交（:407-409 yuanToFen 元→分，后端分口径）——全链口径一致，无二次转换
  - **强制核验**：布局同构（git diff 归因 = B1~B6 修复点嵌入 + #extra 承载 + 头注释，InvoiceReimbursement +194/-16 与 developer 证据一致）/ 后端零改动 + api 层零改动（mtime 实锤：invoice.ts 8/11 早于本批 9/5；git status backend/ 无本批 finance 控制器）/ 视觉同源（var(--fts-*) 全 token，V3-A 零引用）/ 构建独立复跑 typecheck 136=136 三文件零错误 + build EXIT=0 / 批隔离（P1-STOCK-001 零触碰）
- **测试目的**：防止发票报销布局级回退再次漂移 —— 模板区块与 HEAD 同构（PageHeader#extra 新建报销回原位置→stats→advanced-search-panel 手写筛选区→table-section→pagination-wrapper→详情/审批/新建报销 el-dialog）；待办区/工具栏/SearchPanel/分页并入不得复活（模板样式零残留）；重放修复点 B1~B6 不得回退（红冲/作废既有端点+issued 态行内入口+二次确认/E 值格式/失败透传/打印/筛选持久化/KL-060 登记不猜测）；能力层（density/独立分页/统计卡片）不回退；导出登记不失效；金额口径全链一致。
- **测试步骤**：
  1. 模板区块顺序逐块比对 `git show HEAD:frontend/src/views/finance/InvoiceReimbursement.vue`（HEAD cd1c89b）与当前文件（PageHeader#extra→stats→advanced-search-panel→table-section→pagination-wrapper→详情/审批/新建报销 el-dialog）
  2. grep 扫描：`workbench-taskboard` / `workbench-toolbar` / `invoice-reimbursement-page` / `SearchPanel` / `#expand` / `expandable` / `@page-change` / `V3-A`——仅注释提及、模板/样式零命中
  3. 逐修复点核验 B1~B6 + 附加存在 + 消费链闭合（B1：void/redFlush 既有端点 invoice.ts:137-152 + InvoiceController:137-142/:152-157 + 状态机 :28 + 行内 issued 态条件 :492 + 二次确认；B2：value-format YYYY-MM-DD + toDateString/restoreDateRange 字符串化；B5：筛选持久化 fts_search_ 前缀 + 重置即清空；B6：KL-060 登记注释 :274-277 + :552-554 说明承载 + 驳回保持既有登记链）
  4. 能力层核验（density / 独立分页 / 统计卡片）
  5. 金额口径复核（列表 formatFen / 统计卡 formatYuan / 审批 formatFen / 新建 yuanToFen 全链无二次转换）
  6. 强制核验独立复跑：typecheck 136=136（InvoiceReimbursement.vue 零命中）+ build EXIT=0
- **预期结果**：
  - 模板区块与 HEAD 同构（顺序逐块一致，仅 B 修复点嵌入 + #extra 承载）；待办区/工具栏/SearchPanel/分页并入模板样式零残留（仅注释）
  - 重放修复点 B1~B6 全部落位有效（红冲/作废既有端点+二次确认+issued 态行内入口 / E 值格式 / 失败透传+引导空态 / 打印 / 筛选持久化 fts_search_ / KL-060 登记不猜测保持既有语义）；附加（导出登记）不失效
  - 能力层保留（density / 独立分页 / 统计卡片）；金额口径全链一致无二次转换
  - typecheck 136=136 + build EXIT=0
- **测试类型**：手工回归（代码级读码/grep/diff + HEAD 同构对照 + 后端实锤核验 + 金额口径复核 + 构建独立复跑）
- **当前状态**：PASS（2026-09-05）
- **限制说明 / 观察项（如实登记，不销号、不猜测处置）**：
  - 【观察项·登记】详情对话框金额展示 `¥{{ detailData.amountWithoutTax as string }}`（:523-525）——records computed 已格式化字符串（"1,234.56"）→ 前缀 ¥ 拼接正确；审批对话框 totalAmount 为分 → formatFen 转换正确（两处口径不同源但各自正确，登记观察）（QA §3.5-2）
  - 【观察项·登记】KL-060 驳回=作废语义为既有登记链保持（非本批新增业务判断）（QA §3.5-3）：产品决策后回写验收标准，决策前不实现不猜测
  - 【观察项·登记】无浏览器活体：同 §1.5-1 口径——交互核验为代码路径级 + 构建级；运行时点击走查留本基线（REG-FIN-LRVT-009）与产品逐页走查承接（QA §3.5-1）

---

### Batch LRVT-4 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告（`docs/quality/ui-lrvt-b4-qa-report.md`）观察项如实登记（回归角色不猜测处置、不销号，均不构成验收失败）：

1. **【观察项·登记】income_expense 3 卡网格 1 空位**（Report §1.1-②/§6-3）：固定 4 列网格与 HEAD 一致（repeat(4,1fr) 逐字），卡片数由真实数据字段决定（收入/支出/净收支 3 项），不属布局结构差异；产品走查时确认展示预期，若需 4 卡补齐属产品决策，不在本卡范围
2. **【观察项·登记】balance/cash_flow 零值卡不显示为 B2 修复行为**（Report §1.5-2）：HEAD 行为为显示初始 ¥0.00 4 卡；当前为 B2「P0 断流不显示虚构零值卡片」修复行为（statsCards 返回 [] :286），与 B2 空态说明配套，不判差异为回退不彻底
3. **【观察项·登记】budget 执行率 rate*100 后端恒 0**（Report §1.5-3）：后端 executionRate 恒 BigDecimal.ZERO（占位），当前无实际影响；后端 P1-FIN-BE-001 落地时一并确认 executionRate 语义（0~1 vs 0~100），登记观察
4. **【观察项·登记】id String 化既有模式**（AutoVoucher §2.7-2）：displayRecords.id = String(t.templateId)（:146）→ toggleEnabled/delete/update 传 string id → 后端 @PathVariable Long 自动转换（TransferTemplateController :38/:45/:66），无类型失配，既有模式非本批引入
5. **【观察项·登记】审批驳回=作废既有登记链**（Invoice §3.5-3）：KL-060 驳回=作废共用 cancelled 语义混叠为既有登记链保持（非本批新增业务判断），产品决策后回写验收标准，决策前不实现不猜测
6. **【观察项·登记】无浏览器活体（留走查）**（Report §1.5-1/AutoVoucher §2.7-1/Invoice §3.5-1）：本环境无浏览器/接口运行条件，交互核验为代码路径级 + 构建级；运行时点击走查留 REG-FIN-LRVT-006/008/009 与产品逐页走查承接（同既往批次浏览器缺口口径）
7. **【衔接·登记】既往条目断言过时衔接**（任务板 §13.4「回归基线衔接」）：布局级回退使 REG-FIN-WS-006（决策工作台五区布局待办区/工具栏）、REG-FIN-WS-003（记录工作台 AutoVoucher/InvoiceReimbursement 页签化布局）部分断言过时——现状已由本批 REG-FIN-LRVT-006/008/009 新增断言覆盖（布局同构 + B 修复点重放 + 能力层保留 + **WS-006-R1 金额口径重放 / KL-061 / KL-060**）；本批按 107 只增不减零修改执行（不删除不覆盖），原条目录内「断言已过时」标注留待后续批次/architect 统一落位

---

### Batch LRVT-4 门禁

```text
Regression Result: PASS（2026-09-05 Batch LRVT-4 治理批次）
规则：FAIL > 0 禁止放行（治理批次口径：本批为布局级回退批次（任务池 §13）最后一批，非发布批次——结论表示基线持续积累 + 正式基线 FAIL=0 维持，不进上线门禁判定）

- 验收：3/3 卡 QA 独立验收 PASS（`docs/quality/ui-lrvt-b4-qa-report.md`，2026-09-05：P1-FIN-LRVT-006 财务报表 / P1-FIN-LRVT-008 自动凭证 / P1-FIN-LRVT-009 发票报销，无 FAIL、无 -R；布局与 HEAD 同构 + 重放修复点 B1~B7/B1~B6 全有效【9 类端点聚合 7 有效+2 P0 断流实锤 / KL-057 断流空态不伪造 / drillTo 19 处→独立页 / WS-006-R1 金额口径重放防二次 /100 / CRUD 既有端点 / B-3-B4 映射端到端 / KL-061 伪语义列移除 / 契约对齐 / 红冲作废既有端点 / E 值格式 / KL-060 登记不猜测】+ 能力层保留 + 后端/API 层零改动 + 强制核验 6/6 全过 + typecheck 136=136 三文件零命中 + build EXIT=0 独立复跑）
- FAIL 明细：0
- PWL 明细：0（观察/登记项 7 条均为非阻断登记，不构成 FAIL；PD-028/PD-035 为 BLOCKED 不占通过数；导出为登记依赖非 FAIL）
- 基线：新增 REG-FIN-LRVT-006/008/009（正式 107 → 110 条，只增不减、既有条目零删除零修改；候选 0 条维持）
- 布局回退专项收口核对：Batch LRVT-4 三卡完整性核对 = **REG-FIN-LRVT-006/008/009 全部在案**（最后一批闭环，无缺卡、无预固化条目）；**专项累计 101 → 110 = 9 条回退基线全部在案**（LRVT-1：001/002 + LRVT-2：003/004 + LRVT-3：005/007 + LRVT-4：006/008/009 = REG-FIN-LRVT-001~009 九卡完整，只增不减、既有 107 条零修改；纯 B 4 页 LRVT-010~013 关闭不产生基线）；**9 页布局级回退 + 功能修复重放全部 QA 通过，等待产品负责人逐页走查**（roadmap §5.14 硬约束，完成后停止，不自动开新批）
- 观察项：income_expense 3 卡网格 1 空位（固定 4 列网格卡片数由真实字段决定，补 4 卡属产品决策）/ balance·cash_flow 零值卡不显示为 B2 修复行为（不伪造零值）/ budget 执行率 rate*100 后端恒 0（占位无实际影响，P1-FIN-BE-001 落地后确认语义）/ id String 化传 Long 端点既有模式 / 审批驳回=作废既有登记链（KL-060 产品决策后回写）/ 无浏览器活体（留走查）/ 既往条目断言过时衔接（REG-FIN-WS-006/003 部分断言，现状由本批基线覆盖）——如实登记不销号、不猜测处置（均不构成验收失败）
- 结论：FAIL=0、PWL=0（登记项不构成 FAIL）→ 允许放行（治理批次口径，历史 Release Gate（Sprint-4 首批 PASS，2026-08-11）结论不变）；**布局回退专项全部批次完成，等待产品负责人逐页走查（§5.14 硬约束，不自动开新批）**
```

---

## Batch ORDER-A1 回归（订单号唯一性专项：P1-ORDER-NUMBER-002/A1，2026-09-23）

> 验收依据：`docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md`（2026-09-23，QA 独立验收：结论 **PASS_WITH_LIMITATION**——H-01~H-06 **6/6 PASS** / CC-1~CC-9 全 PASS（CC-7 PWL）/ 单测 A1 **3/3 PASS** / CC-6 **33/33 PASS** / git 链 `7f40ab7→6f0f2b9→0acf99b` 一致 / 边界 PASS / FAIL=0）；Scope = `docs/architecture/03-review/p1-order-number-scope-002.md`（FROZEN）；实施记录 = `p1-order-number-002-implementation-record-001.md`。
> 新增依据：仅基于 QA 已验收结论（H-01~H-06 PASS / CC PASS / 边界 PASS）新增 REG-ORDER-001~007（维护规则 5 禁止推测），无推测内容。
> 批次口径：本批为**发布批次**（订单号唯一性修复 A1）——回归结论 PASS 表示本批基线转入正式回归库 + FAIL=0，进 Release Gate 判定；生产证据缺口 L-01 登记为「本地放行 / 生产 PROVISIONAL」。
> 基线计数核对：追加前正式基线 **114 条**（文件实况：基线总览表 114 行 REG-*）→ 追加 7 条（REG-ORDER-001~007）后 **121 条**，只增不减纪律达成（既有 114 条条目零删除零修改）；候选 0 条维持。
> 回归独立抽验（非采信 QA 自测，2026-09-23）：A1 单测 `mvn -Dtest=OrderNewServiceImplOrderNumberA1Test` → Tests run: 3, Failures: 0, Errors: 0；H-01 `ORD202609231729170001`、H-02 `T20260923004`、H-03 `ORD202609231729170002` psql eq=t；全表 total=36 with_on=36 null_on=0 distinct_on=36；H-04 重复键 EXIT=1；H-05 KDS list 命中；H-06 `T20260923007`/`O1790153488606` 全链 serve code=0 + material_consumed=1 + log id=57 库存 8.800→8.700。

### REG-ORDER-001

- **测试编号**：REG-ORDER-001
- **模块**：订单-下单（OrderNewServiceImpl.buildOrderEntity / POST /v1/orders）
- **关联任务**：P1-ORDER-NUMBER-002（A1；QA 独立验收 H-01 **PASS** 2026-09-23）
- **业务场景**：批量下单后 `orders.order_number` = `order_code`，非空且全表唯一（A1 修复点 L1819 `order.setOrderNumber(orderCode)`）
- **测试目的**：防止 order_number 漏赋值（NULL/空）或与 order_code 不一致回归
- **测试步骤**：
  1. 登录 `POST /v1/auth/login`（admin/Admin@123）
  2. `POST /v1/orders` 创建订单（orderType=3, storeId=1, items 含 productId/productName/unitPrice/quantity）
  3. psql：`SELECT order_code, order_number, (order_code=order_number) FROM orders WHERE order_id=...`
  4. psql 全表：`count(*) total / count(order_number) with_on / FILTER NULL or '' null_on / count(DISTINCT order_number) distinct_on`
- **预期结果**：新建单 `order_code=order_number`、eq=t、nonempty=t；全表 null_on=0、distinct_on=total（非空且唯一）
- **测试类型**：API回归 + 数据一致性检查
- **当前状态**：PASS（2026-09-23）
- **限制说明**：L-01 生产不可达（本地证据）；历史订单不由 A1 回填（CC-3 保持）

### REG-ORDER-002

- **测试编号**：REG-ORDER-002
- **模块**：订单-POS点单（PosOrderCreateServiceImpl / POST /v1/pos/orders/order）
- **关联任务**：P1-ORDER-NUMBER-002（A1；QA 独立验收 H-02 **PASS** 2026-09-23）
- **业务场景**：POS T 码双写——order_number 与 order_code 同为 T* 码，POS 语义不变
- **测试目的**：防止 A1 破坏 POS T 码双写路径（T 前缀 order_number 不得被 ORD 覆盖）
- **测试步骤**：
  1. 登录后 `POST /v1/pos/orders/order`（tableNumber, orderType=dinein, items foodCode id）
  2. 记录返回 orderId/orderNumber（T*）
  3. psql：`order_code` / `order_number` / `eq`
- **预期结果**：code=0；`order_code=order_number=T*`，eq=t
- **测试类型**：API回归
- **当前状态**：PASS（2026-09-23）
- **限制说明**：L-03（POS 数字 id 时 food_id 可能 null，与 order_number 双写无关，见 REG-ORDER-006）

### REG-ORDER-003

- **测试编号**：REG-ORDER-003
- **模块**：订单-POS快速单（POST /v1/pos/orders/create）
- **关联任务**：P1-ORDER-NUMBER-002（A1；QA 独立验收 H-03 **PASS** 2026-09-23）
- **业务场景**：快速单独立执行路径（不并入 H-01），order_number 非空
- **测试目的**：防止快速单路径漏走 A1 赋值（独立端点断言）
- **测试步骤**：
  1. `POST /v1/pos/orders/create`（orderType, storeId, items productId/productType/quantity）
  2. psql 核 `order_code=order_number` 非空
- **预期结果**：code=0；`order_code=order_number` eq=t
- **测试类型**：API回归
- **当前状态**：PASS（2026-09-23）
- **限制说明**：L-01（本地）

### REG-ORDER-004

- **测试编号**：REG-ORDER-004
- **模块**：订单-DB唯一约束（orders_order_number_key）
- **关联任务**：P1-ORDER-NUMBER-002（A1；QA 独立验收 H-04 **PASS** 2026-09-23）
- **业务场景**：重复 order_number 被 UNIQUE 约束拒绝
- **测试目的**：防止 order_number 唯一索引/约束缺失或被迁移破坏
- **测试步骤**：
  1. psql INSERT 一条与既有订单相同 order_number 的行
  2. 记录退出码与错误文本
- **预期结果**：EXIT=1；错误含 `重复键违反唯一约束 "orders_order_number_key"`
- **测试类型**：数据一致性检查
- **当前状态**：PASS（2026-09-23）
- **限制说明**：Schema 非 A1 迁移引入（A1 resources/Flyway diff 为零，见 REG-ORDER-007）

### REG-ORDER-005

- **测试编号**：REG-ORDER-005
- **模块**：订单-KDS可见性（GET /v1/kitchen/orders/list）
- **关联任务**：P1-ORDER-NUMBER-002（A1；QA 独立验收 H-05 **PASS** 2026-09-23）
- **业务场景**：POS 创建后 KDS 列表可见（orderNumber 回传）
- **测试目的**：防止 A1 断开 POS→KDS 主链（orderNumber 不回传/不可见）
- **测试步骤**：
  1. POS 创建 T 码订单
  2. `GET /v1/kitchen/orders/list?page=1&size=50`（含 status=pending 过滤）
  3. 断言命中该 T 码与 kitchenOrderId
- **预期结果**：list 命中 T 码 + KO* + status 可见
- **测试类型**：API回归
- **当前状态**：PASS（2026-09-23）
- **限制说明**：L-05（OrderVO 展示层字段缺口，HTTP 侧以 SQL/DB 断言为准）

### REG-ORDER-006

- **测试编号**：REG-ORDER-006
- **模块**：订单-托盘出餐链（TrayController bind/scan-* + deductMaterialsForServe）
- **关联任务**：P1-ORDER-NUMBER-002（A1；QA 独立验收 H-06 **PASS_WITH_LIMITATION（L-03）** 2026-09-23）
- **业务场景**：托盘 bind → scan-kitchen-in → scan-kitchen-out → scan-serve → material_consumed=1 + store_inventory 扣减
- **测试目的**：防止 A1 破坏 POS→KDS→托盘→出餐扣料主链；确认 order_number 赋值后出餐链路仍完整
- **测试步骤**：
  1. POS 创建订单（items id=`foodCode` 如 `FD202608010001`，确保 order_items.food_id=1，规避 L-03）
  2. `POST /v1/tray/create` 创建 idle 托盘
  3. `POST /v1/tray/bind-order` JSON `{trayCode, orderId}` → status=bound
  4. 防抖 ≥3s 后 `POST /v1/tray/scan-kitchen-in?trayCode=` → making
  5. 防抖 ≥5s 后 `POST /v1/tray/scan-kitchen-out?trayCode=` → ready
  6. 防抖 ≥2s 后 `POST /v1/tray/scan-serve?trayCode=` → code=0/served
  7. psql：kitchen_order.material_consumed / material_consume_time；store_inventory_log 最新行 before/after/remark；tray 回 idle
- **预期结果**：全链 code=0；`material_consumed=1`；log `before→after` 扣减且备注含 `KDS出餐扣料 - 订单:T*`；托盘 status=idle
- **测试类型**：手工回归（端到端 + DB）
- **当前状态**：PASS_WITH_LIMITATION（2026-09-23，限制 L-03）
- **限制说明 / 观察项**：
  - 【L-03·非 A1】POS 客户端传数字 id 时 `foodCodeToIdMap` 映射失败 → `order_items.food_id` 默认写 null → scan-serve 可能 500；foodCode 路径已验证 PASS（本批 `T20260923007` food_id=1、serve code=0）；NOT_CAUSED_BY_A1，已登记 P 级池 P1-POS-FOODID-MAP-001
  - 【L-05·非 A1】scan-serve 响应 `materialConsumed` 字段短路为 0，以 DB `material_consumed=1` 为准
  - 【L-01】证据基于本地 8081，生产 PROVISIONAL

### REG-ORDER-007

- **测试编号**：REG-ORDER-007
- **模块**：订单-A1边界（git diff / generateOrderCode / Schema / POS / P0 扣料）
- **关联任务**：P1-ORDER-NUMBER-002（A1；QA 独立验收边界 **PASS** 2026-09-23）
- **业务场景**：A1 改动面边界——仅 service +1 行与单测，不改 generateOrderCode/MAX+1、Schema/Flyway、POS 语义、P0 扣料逻辑
- **测试目的**：防止 A1 范围蔓延（Scope FROZEN 违反）或误改共用路径
- **测试步骤**：
  1. `git diff --name-only 7f40ab7^..0acf99b` → 仅 `OrderNewServiceImpl.java` + `OrderNewServiceImplOrderNumberA1Test.java`
  2. `git diff 7f40ab7^..0acf99b -- backend/src/main/resources` → 空（Flyway 零改动）
  3. 确认 `generateOrderCode()` / `getMaxTodaySequence` 与父提交一致；`order.setOrderNumber(` 业务赋值仅 L1819 一处
  4. 单测复跑 `mvn -Dtest=OrderNewServiceImplOrderNumberA1Test` → 3/3 PASS
  5. git 链：`7f40ab7` → `6f0f2b9` revert → `0acf99b` reapply
- **预期结果**：diff 仅 2 文件 +206 行；resources 零改动；单测 3/3；三提交链一致；POS/P0 扣料文件零触碰
- **测试类型**：手工回归（代码级 diff + 单测复跑）
- **当前状态**：PASS（2026-09-23）
- **限制说明**：
  - 【L-02·非阻断】回滚窗口内无 `POST /v1/orders` API 探针（git/证据文件已核，不放大回滚风险）
  - 【L-04·非阻断】Scope-002/实施记录 untracked（FROZEN 文档不可改，内容核对一致）
  - 【观察】工作区 `OrderNewServiceImpl.java` 相对 HEAD 另有未提交改动（P0-KDS-DEDUCT / W1-EC-04C 等，+434/-334）——非 A1 引入、属既有多批次未提交状态，如实登记不阻断

### Batch ORDER-A1 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告（`docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md` §5）如实登记：

1. **L-01 生产证据缺失**：全部证据 local，状态 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`——**非阻断本地放行**；Release Gate 标注「本地放行 / 生产 PROVISIONAL」
2. **L-02 CC-7 回滚窗口 API 探针缺失**：回滚窗口动态已被 git 提交 + 证据文件时间戳核验——**非阻断**（不放大回滚风险）
3. **L-03 POS 数字 id → food_id 默认 null**：既有缺陷 NOT_CAUSED_BY_A1；foodCode 路径 H-06 PASS——**非阻断 GATE**，登记 P 级池 P1-POS-FOODID-MAP-001
4. **L-04 Scope/实施记录 untracked**：FROZEN 文档内容核对一致——**非阻断**
5. **L-05 OrderVO 无 orderNumber / materialConsumed 短路**：以 DB 断言为准——**非阻断**（展示层）
6. **OBS-01** POS 数字 id food_id=NULL：QA 实证 + 代码走查 :448-456，NOT_CAUSED_BY_A1
7. **OBS-02** 全仓既有失败（DeductConcurrency 2 errors 等）：A1 未触碰 Flyway/扣料核心；CC-6 33/33 保持——范围外非 A1 引入
8. **OBS-03** OrderVO 字段缺口：登记 L-05

---

### Batch ORDER-A1 门禁

```text
Regression Result: PASS（2026-09-23 Batch ORDER-A1 订单号唯一性专项 P1-ORDER-NUMBER-002/A1，发布批次）
规则：FAIL > 0 禁止放行

- 验收：A1 卡 QA 独立验收 PASS_WITH_LIMITATION（`docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md`，2026-09-23：H-01~H-06 6/6 PASS；CC-1~CC-9 全 PASS（CC-7 PWL）；单测 3/3 PASS；CC-6 33/33 PASS；git 链 7f40ab7→6f0f2b9→0acf99b 一致；边界 PASS；FAIL=0）
- 回归独立抽验：A1 单测 3/3；H-01/H-02/H-03 真实创建 + psql eq；全表 total=36 null_on=0 distinct_on=36；H-04 UNIQUE EXIT=1；H-05 KDS 命中；H-06 托盘全链 serve code=0 + material_consumed=1 + 库存 8.800→8.700（零数据补丁）
- FAIL 明细：0
- PWL 明细：L-01（生产不可达→本地放行/生产 PROVISIONAL）、L-02（回滚窗口 API 探针缺失，git/证据已核）、L-03（POS 数字 id food_id null，非 A1，foodCode 路径 PASS，登记 P1-POS-FOODID-MAP-001）、L-04（Scope untracked，FROZEN 核对一致）、L-05（OrderVO/materialConsumed 展示层，以 DB 为准）——5 项逐条确认**不阻断本地放行**
- 基线：新增 REG-ORDER-001~007（正式 114 → 121 条，只增不减、既有条目零删除零修改；候选 0 条维持）
- 既有基线：抽查无受影响（A1 diff 仅 2 文件，不触碰财务/权限/数据治理既有模块）
- 结论：FAIL=0 → 允许放行（本地 Release Gate PASS；生产证据 L-01 登记为 PROVISIONAL_PENDING_PRODUCTION_EVIDENCE）
```


---

## Batch P1-COMBO-ORDER-001 回归（套餐下单/KDS 组合套餐专项：P1-COMBO-ORDER-001，2026-09-24）

> 验收依据：`docs/quality/P1-COMBO-ORDER-001-qa-report.md`（2026-09-24，QA 独立验收：结论 **PASS_WITH_LIMITATION**——验收 6 项 **6/6 PASS** / 抽检 DB 断言 5/5 + KDS live PASS + 代码抽检 PASS + 单测重跑 **37/37 EXIT=0** / FAIL=0、无 `-R{n}`、无 BLOCKED）；实施记录 = `docs/architecture/03-review/p1-combo-order-001-implementation-record-001.md`（§0–§5 验收 6 项）；DS 抽检 §11 = **READY_FOR_QA 6/6**；代码基线 commit `aec5c45`（15 文件业务）+ `40fc776`（DS §11）+ QA 报告 commit `24a1f60`。
> 新增依据：仅基于 QA 已验收结论（验收 6/6 PASS / 抽检 PASS / 单测 37/37）新增 REG-ORDER-008~011（维护规则 5 禁止推测），无推测内容、无 Scope 外断言。
> 批次口径：本批为**发布批次**（套餐下单 + KDS components 展示 + 套餐出餐扣料 + 单品零回归）——回归结论 PASS 表示本批基线转入正式回归库 + FAIL=0，进 Release Gate 判定；生产证据缺口 L-02 登记为「local 放行 / 生产 PROVISIONAL」。
> 基线计数核对：追加前正式基线 **121 条**（文件实况：基线总览表 121 行 REG-*，含 REG-ORDER-001~007；OIC 轨 REV 历史登记表不计入正式基线）→ 追加 4 条（REG-ORDER-008~011）后 **125 条**，只增不减纪律达成（既有 121 条条目零删除零修改）；候选 0 条维持。
> 关键证据（QA 独立复现）：套餐 `T20260924004` `product_type=2 combo_id=1 food_id=NULL`；KDS `recent/full` 套餐行 `components[]` live；套餐出餐 `KO1790260472189` `material_consumed=1` + 库存 log id=62（8.500→8.400）；单品 `T20260924003` `product_type=1 food_id=1 combo_id 空` + log id=61（8.600→8.500）；静默点 2026-09-24 新增 FAILED=0、`food_id IS NULL AND product_type=1`=0；单测 37/37。

### REG-ORDER-008

- **测试编号**：REG-ORDER-008
- **模块**：订单-套餐下单（PosOrderCreateServiceImpl combo 分支 / order_items 落库）
- **关联任务**：P1-COMBO-ORDER-001（QA 独立验收验收项 1 **PASS** 2026-09-24）
- **业务场景**：套餐下单成功后 `order_items` 落 `product_type=2` + `combo_id` 非空 + `food_id` NULL（合法套餐 null 语义，依据任务卡 §1 范围 1 与实施报告 §2.1，非推测）
- **测试目的**：防止套餐行被写成单品形态（product_type/combo_id/food_id 三字段错位）或非法 null（单品型 `food_id` NULL）回归
- **测试步骤**：
  1. POS 下单套餐（comboId 载荷，如「生菜套餐」combo_id=1）
  2. psql：`SELECT product_type, combo_id, food_id FROM order_items oi JOIN orders o ... WHERE o.order_number = 'T*'`
  3. psql 非法 null 扫描：`SELECT COUNT(*) FROM order_items WHERE food_id IS NULL AND product_type=1`
  4. psql 合法套餐行计数：`SELECT COUNT(*) FROM order_items WHERE product_type=2`
- **预期结果**：套餐行 `product_type=2`、`combo_id` 非空、`food_id` IS NULL；非法 null `bad_null=0`；合法套餐行存在
- **测试类型**：数据一致性检查
- **当前状态**：PASS（2026-09-24）
- **限制说明**：
  - 【L-01】浏览器 UI 目检未做（下单入口点击流），代码级 + DB 断言已覆盖，建议回归阶段补浏览器断言
  - 【L-02】证据基于 local 8081，生产 PROVISIONAL

### REG-ORDER-009

- **测试编号**：REG-ORDER-009
- **模块**：订单-KDS套餐展示（KitchenOrderController expandComboDishItems / GET /v1/kitchen/orders/recent/full）
- **关联任务**：P1-COMBO-ORDER-001（QA 独立验收验收项 2 **PASS（API live）** 2026-09-24）
- **业务场景**：KDS `recent/full` 套餐行携带 `productType=2` + `comboId` + `components[]` 配料明细，单一 dish 对象内单卡片展开（不拆卡）；单品行不误展开
- **测试目的**：防止套餐明细丢失（components 缺失/空）、拆卡渲染（N 张卡）、或单品行被误判为套餐展开
- **测试步骤**：
  1. `GET http://127.0.0.1:8081/api/v1/kitchen/orders/recent/full`（HTTP 200）
  2. 断言套餐行（`T20260924004` 型）：`productType=2`、`comboId=1`、`components=[{foodId, quantity, name}]` 逐字段一致
  3. 断言单一 dish 对象携带 components（无 N 张拆卡）
  4. 断言其余单品行 `hasComponents=false`（不误展开，含单品单 `T20260924003`）
  5. 交叉核验 `combo_ingredients` 生效行（deleted=0）：components 数量/qty 与生效配料一致，deleted=1 行不带出
- **预期结果**：HTTP 200；套餐行三字段 + components 逐字段一致；单卡片不拆；单品行零误展开；生效配料交叉核验一致
- **测试类型**：API回归
- **当前状态**：PASS（2026-09-24）
- **限制说明**：
  - 【L-01】KDS 卡片浏览器实拍未做（live API + 代码级 KitchenOrderCard/Home/ServeWindow 渲染已核），建议回归阶段补浏览器断言
  - 【L-02】生产 PROVISIONAL
  - 【L-06】前端渲染依赖已恢复（三端 components 渲染齐全），legacy 读旧表三文件不影响本断言（见批次限制登记）

### REG-ORDER-010

- **测试编号**：REG-ORDER-010
- **模块**：订单-套餐出餐扣料（deductMaterialsForServe 套餐配料路径 / kitchen_order / store_inventory_log）
- **关联任务**：P1-COMBO-ORDER-001（QA 独立验收验收项 3 **PASS** 2026-09-24）
- **业务场景**：套餐出餐后 `kitchen_order.material_consumed=1` 且 `store_inventory_log` 产生扣减记录（before→after 连续、remark 含订单 T 码）；`material_consumption` 无新增 FAILED（无新增静默失败）
- **测试目的**：防止套餐出餐扣料假成功（material_consumed 不置 1 / 无库存 log）或新增静默失败回归
- **测试步骤**：
  1. 套餐单 `T20260924004` 走 bind → scan-kitchen-in → scan-kitchen-out → scan-serve 全链（或复用已验收实测单）
  2. psql：`kitchen_order` 的 `material_consumed` / `status` / `material_consume_time`
  3. psql：`store_inventory_log` 对应行 `before_stock → after_stock`、`change_quantity`、`remark` 含 `KDS出餐扣料 - 订单:T*`；核对与前一条 log 的库存链连续（前 after = 本 before）
  4. psql：`SELECT COUNT(*) FROM material_consumption WHERE status='FAILED' AND create_time >= 当日` → 0
- **预期结果**：`material_consumed=1`、status=served；库存 log before→after 扣减且链连续、remark 含 T 码；当日新增 FAILED=0
- **测试类型**：手工回归（端到端 + DB）
- **当前状态**：PASS（2026-09-24）
- **限制说明**：
  - 【L-07·既有】HTTP 响应 `materialConsumed` 可能回 0（短路既有），以 DB `material_consumed=1` + 库存 log 为准（实施报告 §9.7，本批 DB 已独立证实）
  - 【L-02】生产 PROVISIONAL

### REG-ORDER-011

- **测试编号**：REG-ORDER-011
- **模块**：订单-单品零回归（foodCode 路径 + H-06 复跑 + 四套件单测）
- **关联任务**：P1-COMBO-ORDER-001（QA 独立验收验收项 4 **PASS** 2026-09-24）
- **业务场景**：套餐改造后单品 foodCode 路径零回归——单品单 `product_type=1`、`food_id=1`、`combo_id` 空；出餐 `material_consumed=1` + 库存 log；四套件单测 37/37 全绿
- **测试目的**：防止套餐分支改动破坏既有单品下单/扣料/单测基线（H-06 复跑）
- **引用关系（不重复固化断言）**：
  - 托盘全链 E2E（bind→scan-*→serve）行为断言由 **REG-ORDER-006** 承载（其 PASS_WITH_LIMITATION 状态不变）——本条为本卡改动后的**复跑确认**，不新增 E2E 步骤
  - A1 边界（diff 范围 / generateOrderCode / Schema 零改动）由 **REG-ORDER-007** 承载——本条不重复
- **测试步骤**：
  1. POS 下单品 foodCode 单（如 `T20260924003`）→ psql：`product_type=1`、`food_id=1`、`combo_id` 空
  2. 出餐后 psql：`KO*` `material_consumed=1` + `store_inventory_log` 对应行（8.600→8.500 型，remark 含 T 码）
  3. 单测重跑：`mvn -Dtest=PosOrderCreateServiceFoodIdMapTest,OrderNewServiceImplDeductTest,OrderNewServiceImplOrderNumberA1Test,MaterialDeductionAuditSelfFailureIntegrationTest test` → 合计 37/37、EXIT=0
  4. 非法 null 复核（与 REG-ORDER-008 共用扫描）：`food_id IS NULL AND product_type=1` = 0
- **预期结果**：单品三字段正确；扣料 log 正常；四套件 37/37 EXIT=0；非法 null=0
- **测试类型**：手工回归（DB + 单测复跑）
- **当前状态**：PASS（2026-09-24）
- **限制说明**：
  - 【范围外】surefire 中 `DeductMaterialsForServeConcurrencyTest.txt`（2 Errors，时间戳 2026-09-21）为本卡之前既有残留、不在本次 `-Dtest` 过滤内——与实施报告 §4.1 声明一致，非本卡 FAIL
  - 【L-01/L-02】同批次（UI 目检未做 / 生产 PROVISIONAL）

### Batch P1-COMBO-ORDER-001 限制项登记（如实登记，不销号、不猜测处置）

按 QA 报告（`docs/quality/P1-COMBO-ORDER-001-qa-report.md` §限制 / L-x）如实登记：

1. **L-01 UI 浏览器目检未做**：POS 套餐入口点击流 / KDS 卡片实拍未执行（QA 会话无浏览器能力）——代码级 §3.5 + API live §2 + `vue-tsc` 已覆盖可自动化部分；**非阻断进回归**，建议回归阶段补浏览器断言
2. **L-02 生产证据缺失**：全部结论 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`——**非阻断 local 回归准入**；阻断仅限生产放行 / Release Gate
3. **L-03 ENV 工作区混杂**：仓库既有大量未提交改动；本卡 commit `aec5c45` 恰 15 文件未夹带——属既有工作区债，非本卡引入
4. **L-04 报告行号漂移**：文档行号与实测方法体略有偏移，语义一致（DS §11.2.1 已登记）——非阻断
5. **L-05 soft 路径 `continue` 无日志（OBS-S1 同族）**：HEAD 既有，本卡仅换数据源非新增静默点（DS §11.2.2 已登记）——非阻断
6. **L-06 legacy 三文件仍读旧表**：`PosApiServiceImpl` / `KitchenScanServiceImpl` / `OrderMaterialRequirementServiceImpl` 仍为 `ComboIngredientMapper`——实施报告 §7.1 明确划归**第二步卡**，本卡 Scope 内声明的后续工作，**非本卡缺陷、非阻断**
7. **L-07 HTTP `materialConsumed` 可能回 0**：既有短路同族；成功以 DB `material_consumed=1` + 库存 log 为准（§9.7），本批 DB 已独立证实——非阻断
8. **OBS-P1 任务池状态同步**：`production-remediation-task-board.md` 未见独立条目，QA 按角色边界不改任务池——planner 据 QA 报告同步（流程同步项，非代码缺陷）

---

### Batch P1-COMBO-ORDER-001 门禁

```text
Regression Result: PASS（2026-09-24 Batch P1-COMBO-ORDER-001 套餐下单/KDS 组合套餐专项，发布批次）
规则：FAIL > 0 禁止放行

- 验收：P1-COMBO-ORDER-001 卡 QA 独立验收 PASS_WITH_LIMITATION（`docs/quality/P1-COMBO-ORDER-001-qa-report.md`，2026-09-24：验收 6 项 6/6 PASS；抽检 DB 5/5 + KDS live + 代码抽检 + 单测 37/37 EXIT=0；FAIL=0、无 -R{n}、无 BLOCKED；DS 抽检 §11 = READY_FOR_QA 6/6）
- 证据链：commit aec5c45（15 文件，与实施报告 §6 逐文件一致、无 Scope 外）+ 40fc776（DS §11）+ 24a1f60（QA 报告）
- 关键证据：套餐 T20260924004 product_type=2 combo_id=1 food_id=NULL；KDS recent/full 套餐行 components[] live（单卡片展开、24 行单品不误展开）；套餐出餐 KO1790260472189 material_consumed=1 + 库存 log id=62（8.500→8.400）；单品 T20260924003 product_type=1 food_id=1 + log id=61（8.600→8.500）；静默点当日新增 FAILED=0、bad_null=0；单测 37/37
- FAIL 明细：0
- PWL 明细：L-01（UI 浏览器目检未做 → 代码级+API live+vue-tsc 已覆盖，建议回归阶段补浏览器断言）、L-02（生产证据缺失 → PROVISIONAL，仅阻断生产放行）、L-06（legacy 三文件仍读旧表 → 报告 §7.1 第二步卡范围，非本卡缺陷）——逐条确认不阻断本地放行；L-03/L-04/L-05/L-07/OBS-P1 均为既有债/文档/展示层/流程同步项，非阻断
- 基线：新增 REG-ORDER-008~011（正式 121 → 125 条，只增不减、既有条目零删除零修改；候选 0 条维持）
- 引用关系：单品 E2E 托盘链断言由 REG-ORDER-006 承载（状态不变）、A1 边界由 REG-ORDER-007 承载——REG-ORDER-011 仅做本卡复跑确认，不重复固化
- 既有基线：抽查无受影响（本卡 15 文件限于 POS/KDS 下单与展示链，不触碰财务/权限/数据治理既有模块）
- 结论：FAIL=0 → 允许放行（Release Gate PASS；生产证据 L-02 登记为 PROVISIONAL_PENDING_PRODUCTION_EVIDENCE）
```
## Batch P1-COMBO-LEGACY-CLEANUP-001 回归（POS 套餐菜单数据源切换：2026-09-25）

> 来源：QA `docs/quality/P1-COMBO-LEGACY-CLEANUP-001-qa-report.md`（**PASS_WITH_LIMITATION**，活体 4/4 PASS + DB 断言 3/3，FAIL=0）+ 代码 commit `05d4404`（4 文件）+ DS 抽检 `...-sampling-review.md`（5/5 PASS）
> 基线计数核对：追加前正式基线 **125 条** → 追加 1 条（REG-ORDER-012）后 **126 条**，只增不减纪律达成（既有 125 条条目零删除零修改）。

### REG-ORDER-012

- **测试编号**：REG-ORDER-012
- **模块**：订单-POS 套餐菜单数据源切换（legacy dish_combo/combo_ingredient → dish_combos/combo_ingredients）
- **关联任务**：P1-COMBO-LEGACY-CLEANUP-001（QA 独立验收 **PASS_WITH_LIMITATION** 2026-09-25）
- **业务场景**：切表后功能真实工作——POS 菜单套餐来自 dish_combos（价格分→元、成分经 foods 反查）→ 下单落 product_type=2/combo_id/food_id=NULL → KDS components[] 展开 → scan-serve 出餐扣料
- **测试步骤**：
  1. `GET /api/v1/pos/api/combos`：套餐非空；`price=13.5`（combo_price=1350 分）；`items` foodId=food_code 且成分正确
  2. `POST /api/v1/pos/orders/order`（id=1, dishType=combo）→ code=0；psql：`order_items.product_type=2 AND combo_id 非空 AND food_id IS NULL`
  3. `GET /api/v1/kitchen/orders/recent/full`：套餐行 `productType=2` + `components=[{foodId,quantity,name}]`
  4. tray 全链 `create→bind-order→scan-kitchen-in→scan-kitchen-out→scan-serve` → psql：`kitchen_order.material_consumed=1 status=served` + `store_inventory_log` 对应行（remark 含 T 码）
- **预期结果**：四步全 code=0；三组 DB 断言全部成立
- **测试类型**：活体回归（HTTP live + DB）
- **当前状态**：PASS（2026-09-25，首跑证据 = QA 报告 §1：T20260925001 / KO1790277670325 / log id=65 生菜 8.400→8.300）
- **限制说明**：
  - 【OBS-1】`/api/v1/pos/api/menu` 聚合 500 为 HEAD 既有 `food_category` 实体/表主键不匹配缺陷（getCategories 非本卡改动），非本卡 FAIL；本条菜单断言由 `/combos` 承载
  - 【OBS-3】生产证据 PROVISIONAL（同族 KL-069/076/079），仅阻断生产放行

### Batch P1-COMBO-LEGACY-CLEANUP-001 门禁

```text
Regression Result: PASS（2026-09-25 Batch P1-COMBO-LEGACY-CLEANUP-001，POS 套餐菜单数据源切换）
规则：FAIL > 0 禁止放行

- 验收：QA 独立验收 PASS_WITH_LIMITATION（活体 4/4 + DB 3/3，FAIL=0，无 -R{n}）；DS 抽检 5/5 PASS
- 证据链：commit 05d4404（4 文件 +55/-40，DS 靶点 1 核验）+ 11e05d4/58329c8（实施记录）+ 41d6e07（DS 报告）+ 本 QA 报告
- 关键证据：/combos 套餐 生菜套餐 price=13.5（1350 分）；下单 T20260925001 order_items product_type=2/combo_id=1/food_id=NULL；KDS components=[{foodId:"1",quantity:1,name:"生菜串"}]；出餐 KO1790277670325 material_consumed=1 + log id=65（8.400→8.300）
- FAIL 明细：0
- 限制：OBS-1（既有聚合菜单 500，非本卡）/ OBS-3（生产 PROVISIONAL）/ OBS-4（UI 目检）——均不阻断本地放行
- 基线：新增 REG-ORDER-012（正式 125 → 126 条，只增不减、既有条目零删除零修改）
- 结论：FAIL=0 → 允许放行（Release Gate PASS；生产证据 OBS-3 登记为 PROVISIONAL_PENDING_PRODUCTION_EVIDENCE）
```
