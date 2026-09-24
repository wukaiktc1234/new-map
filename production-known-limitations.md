# Production Known Limitations（生产已知限制清单）

> 用途：收录当前 Sprint-2、Sprint-3A、Sprint-3B-1 与 Sprint-3B-2 已知限制，作为发布评审输入。
> 维护规则：只增不改；限制解除后更新"状态"，不删除历史行。
> 关联文档：`sprint-2c-execution-list.md`、`product-decision-backlog.md`、`production-remediation-task-board.md`、`docs/quality/sprint-3a-phase1-qa-report.md`、`docs/quality/sprint-3a-security-qa-report.md`、`docs/quality/sprint-3b1-qa-report.md`、`docs/quality/sprint-3b2-qa-report.md`

---

## 0. 与 Sprint-2 验收结论的关系（重要）

本清单**不改变 Sprint-2 验收结论**：
- Sprint-2 执行结果维持：15/15 通过（含限制），FAIL 0；P0-HR-002-R1 = PASS_WITH_LIMITATION。
- BLOCKED_PRODUCT_RULE 4 项维持"等待产品"，状态不变。
- 本清单仅把"验收通过但存在已知限制"的事项显式化，供发布评审决策，**不重新评判已通过的验收项**。

---

## 1. 已知限制主表

| 编号 | 限制 | 来源任务 | 风险等级 | 是否阻断发布 | 状态 |
|------|------|----------|----------|--------------|------|
| KL-001 | BLOCKED_PRODUCT_RULE 待产品确认项（4 项） | PD-001~PD-004 → P0-FIN-001 / P0-FIN-002 / P0-HR-002（报名子项）/ P0-HR-005 | 高 | 是 | **阻断判定解除（2026-08-11，PD-001~004 决策到达 + 实现 QA 验收 PASS）** |
| KL-002 | 财务科目期初结转规则未定义 | PD-003 → P0-FIN-001（余额聚合公式无法落地） | 高 | 是 | **阻断判定解除（2026-08-11，PD-003 决策到达 + 期初录入实现 QA 验收 PASS）** |
| KL-003 | 演示 mock 数据源残留（HR 智能模块） | P0-HR-005（Sprint-2 卡）/ 任务池 P1-MOCK-005 | 中 | 否（有条件：生产保留入口即建议阻断） | **处置完成（2026-08-11，PD-004 下线决策执行：入口已移除，演示数据不再暴露）** |
| KL-004 | 排班真实接口未接入 | P0-HR-012（Sprint-2 卡）/ 任务池 P1-MOCK-002 / P0-HR-011 | 中 | 否（入口已禁用，功能暂不可用） | 待后端排期 |
| KL-005 | 部分展示字段硬编码（下拉/字典） | 任务池 P1-DATA-001~010、P2-DATA-004（3.3/3.4 字典治理） | 低 | 否 | 待治理 |
| KL-006 | /workspace/material-request 域级并集超放行（菜单隐藏 URL 可直达） | P0-SEC-004（Sprint-3A 卡，QA 限制 L-1） | 低 | 否 | 待治理 |
| KL-007 | 渗透用例（伪造 token 被拒）未执行；JWT 读取死代码残留 | P0-SEC-006（Sprint-3A 卡，QA 限制 L-2） | 中 | 否 | **已关闭（2026-08-10：P1-SEC-009 活体渗透 PASS，伪造 token 被后端拒绝等 5 目标 HTTP 实测通过）** |
| KL-008 | 权限码静态校验脚本缺失（注册表漂移无法自动拦截） | P1-SEC-003（Sprint-3A 卡，QA 限制 L-3） | 低-中 | 否 | 已拆卡（P2-SEC-007） |
| KL-009 | 非 admin 按钮可见性依赖 role_permission 表数据，代码层无法证明 | P1-SEC-003（Sprint-3A 卡，QA 限制 L-4） | 低-中 | 否 | 待联调 |
| KL-010 | /forgot-password 路由基线即缺（老问题，非本次引入） | P1-SEC-002（Sprint-3A 卡，QA 观察 O-1） | 低 | 否 | 已拆卡（P2-SEC-008） |
| KL-011 | 401 防重标记 finally 重置窗口极短 | P1-SEC-004（Sprint-3A 卡，QA 观察 O-2） | 低 | 否 | 观察（不拆卡） |
| KL-012 | CostAnalysisController 类级 cost:view 与导出码 cost:export 不一致 | P0-SEC-001 范围（Sprint-3A 第二阶段，QA 观察 O-3） | 低-中 | 否 | 随 P0-SEC-001 |
| KL-013 | 真实登录联调（登录→/me→路由跳转→按钮渲染全链路）未在运行环境执行（/me 权限源端到端验证缺口） | P0-SEC-006（Sprint-3A 卡，QA 复审限制 L-5） | 中 | 否 | **已关闭（2026-08-11，浏览器冒烟）**：`docs/quality/browser-smoke-sprint3.md` §一.1 实测 5 项全 PASS（登录页渲染 / 登录+路由跳转 / 授权页可达 / 权限按钮渲染 / 401 弹框→登出 PROD 分支），浏览器缺口全部关闭，P0-SEC-006 L-5 收口 |
| KL-014 | 后端 4 文件 admin 权限码清单不一致（AdminPermissions 多 finance:tax:* 4 码 + purchase:order:* 6 码，未同步其余 3 个发放文件） | P1-SEC-003（Sprint-3A 卡，QA 复审限制 L-6 / 观察 O-5） | 低 | 否 | 随 P0-SEC-001（统一 4 文件清单） |
| KL-015 | 前端注册表超范围新增 purchase:order:terminate/freeze + cancel 描述修正（良性，与页面使用点对齐） | P1-SEC-003（Sprint-3A 卡，QA 复审观察 O-4） | 低 | 否 | 观察（记录，不拆卡） |
| KL-016 | system:init:* 权限码不在前端注册表（前后端权限码跨端不一致） | P0-SEC-005 关联（Sprint-3A 卡，QA 复审观察 O-6） | 低 | 否 | 随 P0-SEC-001（标注关联） |
| KL-017 | SmartRestock「建议记录」Tab 5 条硬编码假历史（双汇/益海嘉里假供应商名）仍在页面可见；handleSuggestionSubmit 仅本地无持久化 | P1-DATA-002（Sprint-3B-1 卡，QA 限制 L-1） | 中 | 否（有条件：3B-2 完成前发布需评估，页面仍可见假数据） | **已解除（3B-2 P1-MOCK-004 承接闭环，2026-08-10）** |
| KL-018 | AI 服务商字典接口后端缺失（无字典接口）+ 统计卡 todayCalls/successRate 显示 0（无统计接口） | P1-DATA-008（Sprint-3B-1 卡，QA 限制 L-3） | 中 | 否 | 待后端排期（同 KL-005 类治理；排期前前端可将 0 改 `--` 防误导） |
| KL-019 | 会员统计 3 字段口径缺失（consumeRatio/avgOrderAmount/repurchaseRate）：无文档化计算口径 → 临时移除、前端 `--` 降级；types/member.ts 类型未对齐 | P1-DATA-009（Sprint-3B-1 卡，QA 限制 L-4） | 低-中 | 否（已降级移除，无假数据） | 待产品/财务定义口径（PD-005）；恢复字段前禁止猜测实现 |
| KL-020 | 运行期联调未执行（无 DB 环境）：DATA-009 聚合正确性、DATA-010 切换刷新未做浏览器级验证 | P1-DATA-009/010（Sprint-3B-1 卡，QA 验证缺口 V-1/V-2） | 高 | 否（活跃缺陷已全部关闭） | **已关闭（2026-08-10）**：DEF-1/DEF-2 经 QA 复验（p0-data-009-r1 = PASS_WITH_LIMITATION）+ 回归重跑（p0-kl-020-rerun）活体闭环；重跑暴露的 **DEF-4** 经 **P0-DATA-010** 修复 + QA 复验（p0-data-010-r1 = PASS 7/7）+ 回归转基线 REG-DATA-017 闭环；V-2 维持代码级 PASS，残留浏览器切换交互缺口（KL-020-V2）登记发布前浏览器冒烟补验；**2026-08-11 冒烟补充**：V-2 浏览器切换交互缺口实测关闭（`docs/quality/browser-smoke-sprint3.md` §一.3：下拉渲染 3 项 + 4 次切换均触发重载 PASS）——KL-020 关闭链完整 |
| KL-021 | InventoryAdjust 会签状态展示为本地 mock（"已签/待签"非后端真实状态） | P1-DATA-003（Sprint-3B-1 卡，QA 限制 L-2） | 低 | 否（仅展示层，不影响提交数据） | 待治理（后端补会签状态字段或移除状态展示） |
| KL-022 | DATA-010 无组织切换 UI（setOrg 零调用），仅 DecisionBoard 门店切换入口 | P1-DATA-010（Sprint-3B-1 卡，QA 限制 L-5） | 低 | 否（机制已就绪，功能范围问题） | 待产品决策（组织切换 UI 属产品功能范围；非规则缺失，不进 PD 队列） |
| KL-023 | 3B-1 批次无法按 commit 分离（工作区无提交），16 文件 diff 混入跨批次 UI 改动（teleported/lock-scroll） | P1-DATA-001~010（Sprint-3B-1 卡，QA 限制 L-6） | 低 | 否 | 工程流程项（建议本 Sprint 验收后按模块分批 commit，便于回归归因） |
| KL-024 | InventoryOutbound 假供应商列表残留（双汇/益海嘉里/蒙牛硬编码，页面可见假名） | QA 观察 O-2（Sprint-3B-1，未拆卡遗留） | 低-中 | 否（有条件：假数据可见，建议 3B 收口前完成） | 已拆卡 P1-DATA-011（3B-2 未执行，建议并入 3B-3 窗口） |
| KL-025 | SignLinkManagement handleRenew/handleDelete 无 API 直接 success；handleSubmit 载荷缺 linkName/supplierId、templateId 写 eContractId 语义待核对 | QA 观察 O-3（Sprint-3B-1，P1-API-001 范畴既有） | 低 | 否 | 挂 P1-API-001 既有范畴（随收尾批次评估，不拆新卡） |
| KL-026 | InventoryLocation getByLocationId 失败回退 Mock | QA 观察 O-4（Sprint-3B-1，P1-API-003 范畴既有） | 低 | 否 | 挂 P1-API-003 既有范畴（随后端排期） |
| KL-027 | HRContract 编辑 probationMonths: 3 硬编码覆盖 | QA 观察 O-5（Sprint-3B-1，P0-DATA-008 既有问题） | 低 | 否 | 随 P0-DATA-008 独立收尾批次 |
| KL-028 | MemberOverview 等级分布/客户分层/增长 Tab 依赖 overview 接口未返回的分布字段 → 空展示 | QA 观察 O-6（Sprint-3B-1，P1-DATA-009 关联） | 低 | 否（空展示非假数据，既有降级） | 观察（关联 KL-019/overview 后端字段扩展） |
| KL-029 | SmartRestock 采购申请运行期联调未执行（真实 POST 落库/采购侧可见未验证） | P1-MOCK-004（Sprint-3B-2 卡，QA 验证缺口 V-1） | 中 | 否（代码侧已 PASS，验证缺口） | 待联调（回归阶段闭环，同 KL-013/KL-020 联调类） |
| KL-030 | 导出 CSV 文件内容运行期验证未执行（10 页生成逻辑代码级通过，未做下载文件内容解析） | P1-EXPORT-002（Sprint-3B-2 卡，QA 验证缺口 V-2） | 高 | **是（活跃缺陷阻断：DEF-A/DEF-B 修复前冒烟门禁 FAIL）** | **部分关闭（2026-08-11，浏览器冒烟）**：OrderQuery 导出实测 PASS（BOM/表头 13 列/真实数据行解析闭环）；折旧/告警两页抽样被活跃缺陷阻断——DEF-A（资产页白屏，注释未闭合）→ **P0-ASSET-001**、DEF-B（设备接口恒 500，实体-表漂移）→ **P0-DEVICE-001**（均发布阻断，DOING）；两缺陷修复 + QA/回归 -R 复验通过前**不关闭**；**2026-08-11 DEF-A/B QA 复验 PASS 更新**：P0-ASSET-001（p0-asset-001-r1 = PASS 5/5）/ P0-DEVICE-001（p0-device-001-r1 = PASS 6/6 含活体实测）均已复验通过——剩余闭环项=折旧/告警两页导出浏览器重跑（冒烟报告 §四 补验项 1/2/3）与资产域其余页冒烟（补验项 3），**以 regression 输出为准**，重跑通过后本 KL 方可关闭 |
| KL-031 | TopNavbar 通知已读/全部已读运行期联调未执行（端点/实体契约代码级通过，未做真实登录态验证） | P1-MOCK-010（Sprint-3B-2 卡，QA 验证缺口 V-3） | 高 | 否（活跃缺陷已关闭） | **已关闭（2026-08-10）**：DEF-3 经 **P0-MOCK-001** 修复（migration V20260810_001 补列 business_id/business_type，方案 A，QA 复验 p0-mock-001-r1 = PASS 7/7）+ 回归重跑（p0-kl-031-rerun）三端点真实登录态活体闭环（GET 200 code=0、read/read-all 落库、造数 0 残留）；残留 R1 浏览器通知面板 UI 交互登记发布前浏览器冒烟补验；**2026-08-11 冒烟补充**：R1 浏览器通知面板 UI 交互实测关闭（`docs/quality/browser-smoke-sprint3.md` §一.4：面板渲染/未读徽标/单条已读/全部已读/DB 落库 5 项 PASS）——KL-031 关闭链完整 |
| KL-032 | 门店招聘域后端缺失：招聘岗位 CRUD 接口缺失 + GET /approvals 恒返空分页占位 | P1-MOCK-001（Sprint-3B-2 待后端清单，O-2） | 中 | 否（4 写操作已禁用 + el-alert 标注） | 待后端排期（同 KL-004/KL-018 类；HR 域实体复用见 KL-038） |
| KL-033 | 盘点计划实体/接口后端缺失（grep 全库 0 命中） | P1-MOCK-003（Sprint-3B-2 待后端清单） | 中 | 否（计划 Tab 已禁用 + el-alert 标注） | 待后端排期 |
| KL-034 | 告警 severity 升级端点后端缺失（/v1/alerts 仅 acknowledge/resolve/delete/statistics） | P1-MOCK-010-1（Sprint-3B-2 待后端清单） | 中 | 否（升级已禁用 + warning 提示） | 待后端排期 |
| KL-035 | 待办任务创建端点缺失（StoreManagementTaskController 仅查询/完成，无 create）+ 代码注释口径不实（O-3） | P1-MOCK-010-2（Sprint-3B-2 待后端清单） | 中 | 否（生成预警任务已禁用 + warning 提示） | 待后端排期 + 注释修正（P2-CLEAN 类，不拆卡仅登记） |
| KL-036 | 采购建议记录查询接口后端缺失（InventoryWarningController 无历史查询端点） | P1-MOCK-004 / L-1 闭环（Sprint-3B-2 待后端清单） | 中 | 否（建议记录 Tab 空态 + el-alert） | 待后端排期 |
| KL-037 | 质检批量导入端点后端缺失（/v1/quality 无导入端点） | P1-EXPORT-002（Sprint-3B-2 待后端清单） | 中 | 否（导入已禁用 + tooltip 标注） | 待后端排期 |
| KL-038 | 门店招聘岗位实体是否复用 HR 域 recruitment-requirements 属产品/架构裁决（注释「后端无岗位 CRUD」表述过宽） | P1-MOCK-001（Sprint-3B-2，观察 O-1） | 低 | 否（本轮已按禁用 + 标注处置，未猜测接线） | 待产品决策（非规则缺失，不进 PD 队列） |
| KL-039 | HRTraining 报名员工选项 4 个硬编码员工（EMP001~004）：报名真实落库但员工身份为伪造值 | P1-EXPORT-002（Sprint-3B-2，观察 O-4；Sprint-2 HR-002 遗留，非本批引入） | 低-中 | 否（有条件：报名记录员工身份失真，建议接线真实员工 API 后收口） | 已拆卡 P1-DATA-012（建议随 3B-3 窗口） |
| KL-040 | 3B-2 批次超范围 UI 行为变更：:teleported 全局替换（约 15 文件/50+ 处）+ lock-scroll 移除 + TopNavbar CSS --fts-safe-top | P1-MOCK-001/003/004/009/010、P1-EXPORT-002（Sprint-3B-2，观察 O-5） | 低 | 否（低风险，行为回归放回归阶段） | 观察（关联 KL-023 分批 commit 归因） |
| KL-041 | StoreCertificate 既有「模拟消息推送」机制（sendFlowNotification 仅本地 flowNotifications + info 提示，无落库，非本批引入） | P1-MOCK-010（Sprint-3B-2，观察 O-6） | 低 | 否（本地提示非假成功文案） | 待后端排期（消息域接线，侧写登记） |
| KL-042 | StoreCertificate 本地过滤后弹「查询完成」成功措辞（P1-API-001 类，Sprint-2 遗留） | P1-MOCK-010（Sprint-3B-2，观察 O-7；挂 P1-API-001 范畴） | 低 | 否 | 挂 P1-API-001 既有范畴（随收尾批次评估，不拆新卡） |
| KL-043 | 设计公开端点豁免清单未文档化：SecurityConfig permitAll **35 端点**（/v1/auth/**、/v1/public/**、/uploads/**、swagger、/ws/** 等；**2026-08-10 补录 /v1/mp/**（MiniProgram getOpenId，来源 QA B 观察 O-B-3，见 KL-046）**）无权威清单；扫描脚本 EXEMPT_CLASSES 仅 AuthController/PosAuthController 2 类 | P0-SEC-001-D（35 设计豁免）/ `scripts/scan-controller-authz.py` | 低 | 否 | 待治理（豁免清单文档化 + 扫描脚本 `--strict`/PERMIT_ALL_METHODS 对齐，关联 P2-SEC-007 静态校验；/v1/mp/** 补录待架构核对） |
| KL-044 | 跨域通用审批接口无权限码：ApprovalWorkflowController 13 方法（/v1/approval/workflows，submit/approve/reject/withdraw 按 businessId 通用审批）——跨域通用组件，无单一业务域权限归属依据 | P0-SEC-001 整卡排除项·范围外 17（QA B 报告 §六）/ `controller/approval/ApprovalWorkflowController` | 中 | 否（整卡排除项，FAIL=0；收口路径=PD-009） | 待产品+架构（PD-009 已登记） |
| KL-045 | QuickStockInController 3 方法未覆盖（matchBarcode/executeStockIn/parseDate，根包 /v1/quick-stock-in，库存域）——C 批收口时不在 C 批文件清单内未纳入 | P0-SEC-001 整卡排除项·范围外 17（QA B 报告 §六）/ 根包 `QuickStockInController` | 低-中 | 否（库存域归属明确，inventory:* 码已注册，修复路径清晰） | 已拆卡（P2-SEC-010，下一周期） |
| KL-046 | MiniProgramController getOpenId（POST /v1/mp/getOpenId，h5 包）permitAll 豁免成立但原不在豁免清单：SecurityConfig L146 /v1/mp/** permitAll，加注解会破坏小程序免登流 | P0-SEC-001 整卡排除项·范围外 17（QA B 报告观察 O-B-3）/ `controller/h5/MiniProgramController` | 低 | 否（既有 permitAll 设计豁免，无越权面） | 已并入 KL-043 豁免清单（34→35）；待架构核对确认 |
| KL-047 | 退款审批 approveUserId 信任客户端参数（审批人身份归因来自前端 JWT `id` 声明，后端未校验与认证主体一致） | P1-SEC-009 QA 观察 O-1（`docs/quality/p1-sec-009-qa-report.md` §二）/ `OrderNewController.java:214-223`、`OrderNewServiceImpl.java:386-424`、`OrderRefund.vue:198` | 低-中 | 否（授权仍受 @PreAuthorize('order:manage') 管控） | 待产品决策（**PD-010 已登记**，BLOCKED_PRODUCT_RULE 类型「审批归属」） |
| KL-048 | 趋势接口（live-monitor/trend、decision-board/revenue-trend）对 **admin 返回空数据**：`resolveStoreFilter` 将 `getAccessibleStoreIds` 返回 **null**（语义=不限制，DataPermissionServiceImpl:362-367 注释明确「管理员直接返回 null」）误判为无权限 → NO_ACCESS_STORE_ID=-1 → `AND store_id=-1` → 空结果；**同根因第二表现（QA p0-data-009-r1 新观察）**：`kpi-summary?storeIds=1` admin 触发 NPE（OperationsReportServiceImpl:72 `accessibleStoreIds.contains()` 未对 null 防御）→ code=500 显式错误态（无伪装） | P0-DATA-009 观察登记 **O-DEF2-1**（developer 修复报告 §五.1）+ QA p0-data-009-r1 新观察 2 / `LiveMonitorServiceImpl.java:283-284`、`DecisionBoardServiceImpl.java:353-354`、`OperationsReportServiceImpl.java:72,450-464`、`DataPermissionServiceImpl.java:362-367` | 中-低 | **否**（修复前判定：admin 趋势空属数据可见性缺陷，非 500 级，与 DEF 的 P0 区分；不阻断发布，但发布前需架构裁决数据权限语义） | **已关闭（2026-08-11）**——修复闭环（3 文件 4 处 null 前置分支 + NPE 防御；QA 复验 kl-048-r1 = PASS_WITH_LIMITATION；REG-DATA-018 基线化）；「发布前需架构裁决」需求消除（修复前判定=不阻断维持不变，理由见 §2 明细） |
| KL-049 | **DEF-4（KL-020 重跑暴露，发布阻断·静默数据失真）**：`RechargeRecordMapper.xml:6-38` `selectStatsOverview` 9 个别名驼峰未加引号 → PostgreSQL 折叠为小写（psql 实证 rechargethismonth）→ MyBatis resultType=Map key 全小写 → `MarketingMemberServiceImpl.java:230-233` / `RechargeStatsServiceImpl.java:39-54` 按驼峰 get 取 null → 恒 0——GET /v1/members/stats/overview rechargeThisMonth 恒 0.00 + GET /v1/recharge-stats/overview 11 字段全 0（RechargeManage 充值统计卡失真） | `docs/quality/p0-kl-020-rerun.md` 三章 DEF-4（KL-020 重跑活体实证）/ 承接 P0-DATA-009 验收标准 1 非空充值缺口（QA 复验空表掩盖史） | 高 | 否（发布阻断缺陷已修复并复验关闭） | **已关闭（2026-08-10）**：DEF-4 经 **P0-DATA-010** 修复（XML 别名加双引号 + 同型 15 mapper 扫清，diff 纯引号 157/157）→ QA 复验 p0-data-010-r1 = **PASS 7/7**（非空充值造数 rechargeThisMonth=1250.00 与 DB 精确一致、recharge-stats 11 字段全非 0 对账）+ 回归转基线 **REG-DATA-017**；与 KL-020 关闭链联动闭环 |
| KL-050 | **设备域同型漂移组（DEF-B 同族，2026-08-11 P0-DEVICE-001 QA 复验 §4 超范围记录）**：① `Device.connectionType Integer` vs 活体 DB `devices.connection_type varchar(50)`（值约定为数字字符串 '1'~'4'，当前数据映射正常，**非数值数据下仍会 500**，同 DEF-B 缺陷族）；② `Device.storeId Long` vs 活体 DB `devices.store_id varchar(50)`（当前值数字字符串 '1' 映射正常，非数值时 500 风险） | P0-DEVICE-001 QA 报告 §4（`docs/quality/p0-device-001-r1-qa-report.md`）/ `backend/.../entity/Device.java` + 活体 information_schema 实测 | 中 | 否（记录存在性：当前数据均为数字字符串、映射正常，无活跃故障；风险在非数值数据场景） | 待治理（建议 audit 立项「设备域剩余同型漂移统一治理」，后续批次执行）。**2026-08-15 状态登记：并入 Entity-Table Consistency Remediation 家族专项（D-2），家族专项统一治理中（执行窗口 08-16 12:55 后）** |
| KL-051 | **设备域迁移文件列名漂移（文件级，无运行时影响）**：实体 `@TableField("create_time"/"update_time")` 与活体 DB 一致，但 migration `V1.0.0.100__init_postgresql.sql` L933-934 声明 **created_at/updated_at**——迁移文件与活体库/实体不一致（文件级漂移） | P0-DEVICE-001 QA 报告 §4（`docs/quality/p0-device-001-r1-qa-report.md`） | 低 | 否（实体与活体库一致，无运行时影响；迁移文件基线需核对修正） | 待治理（建议 audit 核对迁移文件基线并修正，后续批次执行）。**2026-08-15 状态登记：并入 Entity-Table Consistency Remediation 家族专项（D-2），家族专项统一治理中（执行窗口 08-16 12:55 后）** |
| KL-052 | **KDS 设备类型展示归属缺口（观察项）**：任务卡与驱动/模拟器惯例含 `case "KDS"`（DeviceSimulator/DeviceConnectionServiceImpl/DevicePrintServiceImpl 实证），但 KDS 不在前端设备字典 `frontend/src/types/device.ts` L10-17 与后端 getDeviceTypeName 白名单（PRINTER/SCANNER/SCALE/LOCKER/OTHER）→ KDS 设备展示为「未知」 | P0-DEVICE-001 QA 报告 §3 观察项（`docs/quality/p0-device-001-r1-qa-report.md`） | 低 | 否（展示层降级，非本卡引入） | 观察（展示字典范围，属产品功能排期，非规则缺失不进 PD 队列；产品确认 KDS 是否纳入字典后实施） |
| KL-053 | 财务凭证期间归属规则未定义：余额聚合按 voucherDate yyyy-MM 归属期间；跨月红冲/调整凭证的期间口径需财务规则确认 | P0-FIN-001（Sprint-4 首批，developer 风险提示 + QA 观察，`docs/quality/sprint-4-backend-qa-report.md`） | 低-中 | 否（当前聚合行为可运行，属口径风险） | 待财务规则确认（下一周期处置，Owner：财务规则确认） |
| KL-054 | **销售订单实体-表漂移（DEF-3/KL-050/051 同族新实例，2026-08-14 OIC-1 observe 首轮暴露）**：`SalesOrder.java` `@TableName("sales_order")` 映射的表在生产库**不存在**（活体库实际订单表为 `orders`，`OrderNew.java` `@TableName("orders")` 体系）→ DashboardServiceImpl.getTodaySalesOverview 查询 sales_order 抛 BadSqlGrammarException「relation sales_order does not exist」→ /home dashboard「今日销售概览」卡片降级为空（catch 兜底，页面不崩，前端 code=404 有处理） | OIC-1 observe 首轮（2026-08-14，`oic-1-task-board.md` §8.6 观察项 O-20260814-01，完整堆栈）/ `backend/.../entity/SalesOrder.java:8`、`entity/OrderNew.java:17`、`service/impl/DashboardServiceImpl.java:95-100` | P1（后端域） | 否（当前 catch 降级可运行，dashboard 卡片空数据；无数据损坏/越权面） | **audit 立项（2026-08-14 发布负责人指示）**：修复排后端整改池（不进 OIC-1）；随「全仓实体-表映射扫描」系统性治理（entity @TableName ↔ migration/schema ↔ 真实表，输出 mismatch 清单分类 P0/P1/P2，只扫描不修改）。**2026-08-15 状态登记：并入 Entity-Table Consistency Remediation 家族专项（D-2），家族专项统一治理中（执行窗口 08-16 12:55 后）** |
| KL-055 | **budgets 表结构漂移（实体-表漂移家族扩展，2026-08-15 补登记）**：`budgets` 表为旧采购预算结构（V20260704_001），而 `finance/Budget` 财务预算实体映射列漂移 → SQL 引用漂移列 → BadSqlGrammarException → `/v1/finance/budgets` 返回 HTTP 200 + code:500「系统繁忙」；无筛选查询被分页 COUNT 短路掩盖为 code:0 假空列表（**假成功形态**） | OIC-1 Batch 2 QA 复验观察项 **O-20260815-02**（`oic-1-task-board.md` §8.9，qa 独立实测暴露）+ auditor 复核建议编号（`docs/quality/frontend-experience-audit-draft.md` 文末，2026-08-15） | P1（后端域） | 否（OIC-1 前端批次不受影响——接口异常仅前端可见失败提示，前端逻辑正确；修复排后端整改池） | **家族专项统一治理中**（Entity-Table Consistency Remediation，随 KL-054/ETM 同批窗口，执行窗口 2026-08-16 12:55 后开工） |
| ETM-106 | **inventory 列级漂移（P0 后端专项，实体-表漂移家族成员）**：Inventory 实体/XML 20 列（material_name/specification/batch_no/locked_quantity/unit_cost/total_cost 等）vs `inventory` 表产品口径 18 列（仅 V20260730_001 补 6 列）→ ReceiptConfirmationServiceImpl:706-716 仓库收货确认写库存 INSERT/UPDATE 引用不存在列 → SQL 异常 → @Transactional 回滚 → **仓库手持收货确认全线 500**（门店 STORE 分支走 store_inventory 表已对齐，幸免） | 全仓实体-表映射扫描（`docs/quality/entity-table-mapping-audit.md`，**C 类唯一 P0**）+ 多客户端审计 RM-BE-001（`docs/quality/client-audit-receiving-mobile.md`，2026-08-15） | P0（后端域） | 否（当前观察窗口冻结后端，无发布动作；修复排 RM-B1-001 专项卡，执行窗口 2026-08-16 12:55 后开工） | **家族专项统一治理中**（Entity-Table Consistency Remediation 专项首卡 **RM-B1-001**；窗口内仅出 migration 方案不落库，方案含实体↔migration↔真实表三向核对） |
| KL-057 | **FinanceReport 资产负债表/现金流量表 API 断流（P0 断流，发布/模块可用性风险）**：`report.ts:63-64` getBalanceSheet 与 `report.ts:79-80` getCashFlowStatement 注释自认「后端 ReportController 无 /balance-sheet 端点，当前调用会 404」；`ReportController.java:30-83` 仅 7 个端点（profit-statement/income-expense-summary/receivable-statistics/payable-statistics/aging-analysis/cost-structure/budget-execution），**无 balance-sheet、无 cash-flow** → 页面切换「资产负债表」「现金流量表」即失败/空数据（「数据无感」核心根因之一） | 阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.8（FinanceReport.vue:256-258 loadData:170-200 分派；report.ts:63-64,79-80；ReportController.java:30-83） | P0（模块可用性） | 否（阶段一诊断登记，整改属财务模块任务型重构专项阶段三 P1-FIN-WS-006 范围；发布门禁以 roadmap/回归为准） | 待评审（登记 2026-09-03；诊断阶段只读，修复排阶段三实现批，方向=后端补端点或隐藏 Tab，由 developer 执行） |
| KL-058 | **FinancePeriod.vue:40 operatorId 兜底 '1'（P1 身份伪造风险）**：`String(permissionStore.userInfo?.userId ?? '1')`——取不到用户时默认 userId=1（管理员），结账/反结账/损益结转均携带此身份，有越权操作风险 | 阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.7（FinancePeriod.vue:40；结账 handleClose:290-305 / 反结账 handleReopen:308-323 / 损益结转 handleProfitTransfer:266-285） | P1 | 否（阶段一诊断登记；整改排财务模块任务型重构专项阶段三） | 待评审（登记 2026-09-03；修复方向=operatorId 强制从会话取，禁止兜底管理员身份，developer 执行） |
| KL-059 | **FinanceTax.vue:599-602 税期选项硬编码 202603~202606（E 超期失效）**：Tab1 纳税期间 el-select 选项硬编码，超期即失效（当前 2026-09 已超 202606） | 阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.5（FinanceTax.vue:598-603 硬编码选项，599-602 数据；TaxRecordDTO.taxPeriod 字段存在） | 中（E 类：超期失效） | 否（阶段一诊断登记；整改排财务模块任务型重构专项阶段三） | 待评审（登记 2026-09-03；修复方向=税期选项动态化，developer 执行） |
| KL-060 | **InvoiceReimbursement 驳回=作废共用 cancelled 状态（P2 语义混叠）**：审批通过=status 置 issued、**驳回=置 cancelled**（InvoiceReimbursement.vue:154-157）——「驳回」与「作废」共用同一状态值，驳回的发票无法区分 | 阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.13（InvoiceReimbursement.vue:154-157；invoice.ts:137-152 void/redFlush 端点存在但页面无红冲/作废入口） | P2（语义混叠） | 否（阶段一诊断登记；整改排财务模块任务型重构专项阶段三） | 待评审（登记 2026-09-03；状态机语义拆分涉及业务规则，需产品决策时登记 PD，不猜测） |
| KL-061 | **AutoVoucher「最近执行」列用 updateTime 冒充（P2 展示失真）**：AutoVoucher.vue:67「最近执行」列展示 updateTime/createTime——非真实执行时间（伪语义，P2） | 阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.12（AutoVoucher.vue:67；transfer-template.ts:75-95 有 create/update/delete 但页面无规则 CRUD 入口） | P2（展示失真） | 否（阶段一诊断登记；整改排财务模块任务型重构专项阶段三） | 待评审（登记 2026-09-03；「最近执行」真实执行时间依赖后端执行记录字段，未确认前不猜测实现） |
| KL-062 | **/vouchers/export P0 桩实现（审计真实性风险·假成功类，同 P0-FIN 家族）**：`/v1/finance/auto-vouchers/vouchers/export` 返回 `new byte[0]`（HTTP 200 + 空文件假成功）+ 凭证域端点无 UI 消费方（断流） | 导出盘点 `docs/quality/finance-export-inventory-20260903.md` §1.1/§5.3（P1-FIN-EXPORT-001 交付物，2026-09-03）/ AutoVoucherManageServiceImpl.java:380-385、AutoVoucherController.java:200-211、AutoVoucher.vue:14-16,45-57 | 高 | 否（登记即处置：后端池排期；前端 0 消费方无活跃暴露面，接线前必须完成修复） | 后端池排期（P0-FIN-EXPORT-001 桩修复 / P0-FIN-EXPORT-002 断流修复，任务池 §11.6；验收按修正③四原则） |
| KL-063 | **StoreManagementSettlementController 日结导出表头口径混用（元/分并存）**：:209-215「总营收(元)」与「现金金额(分)」并存 + :238 退款笔数/作废数据留空——口径不一致直接暴露在导出文件 | 导出盘点 `docs/quality/finance-export-inventory-20260903.md` §1.2/§4.3 口径反例（P1-FIN-EXPORT-001 交付物，2026-09-03）/ StoreManagementSettlementController.java:209-238 | 中 | 否（记录存在性：既有占位 CSV，整改排后端池；日结导出属 Batch 1 范围） | 后端池排期（P1-FIN-EXPORT-003，任务池 §11.6；统一分口径存储、输出层转元、补齐字段） |
| KL-064 | **POS 历史数据同步风险（W1-EC-04C-OrderQuery 核心风险）**：管理端订单查询迁移从 orders_legacy 改为 orders 表，核心风险为 POS 历史数据是否已全部同步到 orders 表；若未完全同步，管理端将丢失历史订单数据 | W1-EC-04C-OrderQuery（任务池 §21）/ `docs/architecture/system-fact-map/wave1-production-gate-001.md` / `docs/architecture/system-fact-map/wave1-production-gate-002.md` | 高 | **是（灰度前必须解决）** | 待数据扫描验证（生产环境执行 `SELECT COUNT(*) FROM orders_legacy WHERE deleted = 0` + `SELECT COUNT(*) FROM orders WHERE deleted = 0` 对比；legacy=0 或 orders≥legacy 方可迁移） |
| KL-065 | **扣料失败审计自身失败无结构化持久化兜底（P0-KDS-DEDUCT 残余 R1）**：审计 Bean 写 FAILED 行自身异常时仅 log.error，审计行可能丢失；P0-1 仅保证主异常传播 | P0 Closure `docs/architecture/03-review/p0-kds-deduct-closure-001.md` §5-R1 | 中 | 否（已带风险关闭） | CLOSED_WITH_REGISTERED_RISKS 项下开放项；后续独立批次设计异步/文件/二次表兜底并 QA |
| KL-066 | **REQUIRES_NEW 失败路径连接池触顶风险（P0-KDS-DEDUCT 残余 R2）**：失败 K=N×M、M=2；`application-pg.yml` maximum-pool-size=10；失败并发 N≥6 → K≥12 可达/超限；scanServe 防抖无硬并发上限 | P0 Closure §5-R2 + `application-pg.yml:31` + `TrayServiceImpl.scanServe:284` | 中 | 否（定量风险，本轮禁调参） | 待压测/调参评估；P0 关闭不宣称已消除 |
| KL-067 | **selectUndeducted/getUndeductedRecords 可能含 FAILED 行（P0-KDS-DEDUCT 残余 R3）**：消费侧查询合同未显式排除 status='FAILED' 审计行 | P0 Closure §5-R3 | 中低 | 否 | 待明确查询合同（排除/分列）+ 回归 |
| KL-068 | **orders.order_number NOT NULL 阻断非 POS 创建路径 HTTP E2E（独立 P1）**：`buildOrderEntity` 只写 order_code 不写 order_number → INSERT 违约；POS 路径双写不阻断；另有 KDS 旧 API 断链/serve-order 404 归 P2 | `docs/architecture/03-review/p1-order-number-scope-001.md` §3 E2E 阻断扫描 → **有效 Scope 已换** `p1-order-number-scope-002.md` | 高（E2E） | **Historical（原登记，superseded interpretation）**：是（从 /v1/orders 创建起的全链路）；**Current（唯一）**：`UNKNOWN / PENDING_CONFIRMATION` — 发布单元范围未证实，见页脚；**禁止**再把 Historical「是」当 current | 待 P1-ORDER-NUMBER 实施（Scope-002 已成文；回滚演练 + GATE CC 全 PASS + CC-8 登记后关闭）；**E2E 限制开放 ≠ 发布单元已裁决** |
| KL-069 | **Batch ORDER-A1 生产证据缺失（QA L-01）**：P1-ORDER-NUMBER-002/A1 验收与回归证据均为本地（H-01~H-06、REG-ORDER-001~007），生产环境不可访问；QA/Gate 状态 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE` | QA `docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md` §5 L-01 + Release Gate `ALLOW_LOCAL`（2026-09-23，FAIL=0） | 中（生产放行） | **是（仅对生产放行）**——本地 `ALLOW_LOCAL` 不受阻（Gate 已逐条判定非阻断本地放行）；生产上线前须补验 | 观察（待生产侧抽验 H-01/H-02/H-04 或等价证据后由 Release Gate 裁定解除；**不作为 A1 本地放行 FAIL**） |
| KL-070 | **A1 回滚窗口 API 探针证据缺失（QA L-02）**：CC-7 要求回滚窗口内 `POST /v1/orders` API 探针证据，evidence 03/05-* 仅有 git/代码级验证；窗口已过无法补测 | QA 报告 §5 L-02 + Gate 判定（2026-09-23） | 低-中 | 否（Gate：git 三提交链 `7f40ab7→6f0f2b9→0acf99b` + 证据文件已独立核验，不放大回滚风险） | 观察（建议下轮回滚演练补窗口内 HTTP 探针证据） |
| KL-071 | **POS 数值 id → `order_items.food_id` 静默 null（QA L-03，既有缺陷）**：POS 客户端传数值 id 时 `PosOrderCreateServiceImpl` foodCode 映射 miss 仅 `log.warn` 不阻断插入 → food_id 写 null → 后续扣料路径 500 + FAILED 审计；foodCode 载荷路径已 PASS；**NOT_CAUSED_BY_A1** | QA 报告 §5 L-03（`PosOrderCreateServiceImpl.java:447-450`，实测单 `O1790153488596` food_id=NULL）+ Gate 判定 | 中 | 否（非 A1 引入、非 A1 卡 GATE，Gate 判非阻断本地放行） | **已解除（2026-09-24）**：`P1-POS-FOODID-MAP-001` CLOSED_WITH_REGISTERED_LIMITATIONS（B2 fail-fast + 数据清理 20→0 + QA PASS）；残余转 KL-074~076 |
| KL-072 | **A1 Scope-002/实施记录/证据目录未 git 入库（QA L-04）**：`p1-order-number-scope-002.md`（FROZEN）、implementation-record-001、evidence/ 均为 untracked → FROZEN「冻结后不可改」无 git 层证明（内容已核对与验收口径一致） | QA 报告 §5 L-04 + Gate 判定（2026-09-23） | 中（治理） | 否（Gate：FROZEN 文档不可改，内容核对一致，非阻断本地放行） | 观察（建议架构/规划侧入库固化；入库≠改内容，FROZEN 文档仍禁改） |
| KL-073 | **A1 `OrderVO` 无 `orderNumber` + scan-serve `materialConsumed` 不回读（QA L-05）**：HTTP 层 order_number 断言依赖 SQL；scan-serve 响应 materialConsumed 与 DB 不一致（DB `material_consumed=1` 为准） | QA 报告 §5 L-05 + Gate 判定（2026-09-23） | 低（展示层） | 否（既有显示问题、不扩面，Gate 判非阻断本地放行） | 观察（以 DB 为准；如需 HTTP 断言转正另立卡评估） |
| KL-074 | **S1 完成路径 food_id 空时静默跳过扣料（P1-POS-FOODID-MAP-001 残余 OBS-S1）**：`OrderNewServiceImpl:599-607/:613/:1229-1237/:1243` 等 S1 完成路径，food_id 为空时无日志跳过扣料（HEAD 既有，非本卡引入）；POS 下单路径已由 B2 fail-fast 消除 | `docs/architecture/03-review/p1-pos-foodid-map-001-implementation-record-001.md` §12.6 + 任务池 §22 收口回写（2026-09-24） | 中 | 否（POS 主路径已闭合；S1 路径归观察，不扩面不启动本卡返工） | 观察（如 S1 路径需同等 fail-fast 另立卡评估；**不作 P1-POS-FOODID-MAP-001 返工条件**） |
| KL-075 | **B2 fail-fast 抛 500 而非结构化 400（P1-POS-FOODID-MAP-001 残余 OBS-B2-500）**：`PosOrderCreateServiceImpl:458-463` 改 throw 后异常穿透为 HTTP 500（原 log.warn+null 路径已消除）；拦截器/错误码映射未覆盖 BusinessException 400 → 500 降级 | 实施报告 §12.6 + `PosOrderCreateServiceImpl:458-463` | 低 | 否（拒绝下单语义正确，仅错误码展示层） | 观察（如需 400 结构化响应另立卡调全局异常映射；**不作本卡返工条件**） |
| KL-076 | **生产证据 ENV-1：POS 映射链路仅本地验证（P1-POS-FOODID-MAP-001 残余 ENV-1）**：H-06 HTTP E2E + B2 活体 400 + 单测均为本地；生产环境 POS 客户端真实载荷映射未抽验；与 KL-069 同族 | 实施报告 §12.6 + `docs/quality/P1-POS-FOODID-MAP-001-qa-report.md`（本地 PASS） | 中（生产放行） | **是（仅对生产放行）**——本地 ALLOW_LOCAL 不受阻 | 观察（生产侧抽验 POS 下单 food_id 非空 + 未知 code 拒单后由 Gate 裁定解除；**不作本地放行 FAIL**） |
| KL-077 | **Git 历史含明文凭据且已推远程，决定不重写历史（凭据治理）**：commit `dd33ee3`（init 基线）含 `backend/login.json`/`backend/login-captcha.json`（`admin/admin123`）及根目录 `login.json`/`login_body.json`/`test_login.json`/`tmp_login.json`/`.login_response.json`（JWT 全文）；HEAD 仍 tracked：`e2e/.auth/*.json`、`frontend/e2e/.auth/*.json`（admin JWT+refreshToken）、`backend/keystore.p12`、`backend/src/main/resources/keystore.p12`；多处 tracked 文档/脚本明文口令（`admin123`/`Admin@123`，含 README、launcher.bat、e2e 脚本、QA 报告）；`.env.example` 含示例 JWT_SECRET。清理 commit `883b639` 已从工作区/索引移除 `backend/login*.json` 并加 `.gitignore`，但 **remote master=`883b639` 历史仍可取回 dd33ee3 明文**。**决定：不重写 git 历史**（独立决策，第二步留待；重写需 force-push + 协作方 rebase，成本/风险另评） | `dd33ee3`/`883b639` + `git log --all` + `git ls-files` + 凭据扫描（2026-09-24） | 中（安全） | 否（本地 dev 凭据为主；生产口令已改 `Admin@123` 且不在本清单裁决内）；若仓库公开/外泄须升为**是**并重评重写 | 观察（生产侧轮换 admin 口令/JWT_SECRET/DB 口令为独立动作；若需清历史另立卡做 history rewrite 决策；**不作 P1-POS-FOODID-MAP-001 返工条件**） |
| KL-078 | **P1-COMBO-ORDER-001 限制 L-01：UI 浏览器目检未做（POS 套餐入口点击流 / KDS 卡片实拍）**：代码级 §3.5 + API live §2 + `vue-tsc` 已覆盖可自动化面；浏览器级目检未执行 | QA `docs/quality/P1-COMBO-ORDER-001-qa-report.md` §限制 L-01 + 任务池 §23.4（2026-09-24） | 低-中 | 否（不阻断本地放行；建议回归阶段补浏览器断言，与 FOODID 卡 L-01 口径一致） | 观察（回归阶段补浏览器断言后关闭；**不作 P1-COMBO-ORDER-001 返工条件**） |
| KL-079 | **P1-COMBO-ORDER-001 限制 L-02：生产证据缺失 → `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`**：验收与回归证据均为本地（H-01~H-06、REG-ORDER-008~011），生产环境不可访问；与 KL-069（A1）/KL-076（FOODID）同族 | QA 报告 §限制 L-02 + 回归 `production-regression-test.md` Batch P1-COMBO-ORDER-001 门禁（2026-09-24） | 中（生产放行） | **是（仅对生产放行）**——本地 `ALLOW_LOCAL` 不受阻（Gate 逐条判定非阻断本地放行）；生产上线前须补验 | 观察（待生产侧抽验套餐下单/KDS 展开/出餐扣料或等价证据后由 Release Gate 裁定解除；**不作本地放行 FAIL**） |
| KL-080 | **P1-COMBO-ORDER-001 限制 L-06：legacy 三文件仍读旧表 `combo_ingredient`（第二步卡范围）**：`PosApiServiceImpl` / `KitchenScanServiceImpl` / `OrderMaterialRequirementServiceImpl` 仍为 `ComboIngredientMapper`——双表并存窗口，实施报告 §7.1 明确划归**第二步卡**、Scope 内声明的后续工作，非本卡缺陷 | QA 报告 §限制 L-06 + 实施报告 §7.1 + 任务池 §23.4（2026-09-24） | 中 | 否（本卡 scope 声明的后续工作，非活跃故障；双表并存窗口由 `DatabaseFixConfig` 幂等保底同步缓解） | 待第二步卡排期（legacy 三文件改读新表 `combo_ingredients` + 废弃旧表评估；**不作 P1-COMBO-ORDER-001 返工条件**） |

> 状态字典：`待产品` / `待财务` / `待产品决策` / `待后端排期` / `待后端组` / `待联调` / `待渗透` / `待治理` / `观察` / `已拆卡` / `已解除` / `已建卡` / `家族专项统一治理中` / `待数据扫描验证` / `待第二步卡排期`

---

## 2. 明细

### KL-001｜BLOCKED_PRODUCT_RULE 待产品确认项（4 项）
- **来源任务**：PD-001 缴税重复缴纳（→P0-FIN-002）、PD-002 培训重复报名（→P0-HR-002 报名子项）、PD-003 科目期初结转（→P0-FIN-001）、PD-004 HR 智能模块页面处置（→P0-HR-005）
- **风险等级**：高
- **是否阻断发布**：**是**（PD-001/PD-003 涉资金与合规，上线前必须决策；PD-004 涉生产展示虚构数据）
- **处置**：等待产品/财务负责人决策，决策后回写对应任务卡验收标准（见 `product-decision-backlog.md` 决策流转）；规则落地前不得实现猜测业务逻辑。
- **处置更新（2026-08-11，阻断判定解除）**：**PD-001~004 全部决策到达**（`product-decision-backlog.md` §5：PD-001 幂等键=税种+所属期+凭证号 / PD-002 唯一性键=员工+课程+期次 / PD-003 不自动结转+期初人工录入+审计 / PD-004 HR 智能 3 页面下线）且**实现经 QA 独立验收 PASS**（`docs/quality/sprint-4-backend-qa-report.md`：P0-FIN-002 ✅ / P0-HR-002 ✅ / P0-FIN-001 ✅，FAIL=0 无 -R；`docs/quality/sprint-4-frontend-qa-report.md`：P0-HR-005 ✅ 8/8）——**KL-001 阻断判定解除（2026-08-11）**，不再构成发布阻断；BLOCKED 项未纳入 Sprint 通过数的历史口径维持不变。

### KL-002｜财务科目期初结转规则未定义
- **来源任务**：PD-003 → P0-FIN-001
- **风险等级**：高
- **是否阻断发布**：**是**（余额聚合公式（期初 + 借方 − 贷方）中的期初余额来源依赖结转规则，规则未定义则假账修复无法收口）
- **处置**：财务负责人确认结转方式（如：期初余额取数口径、结转周期、历史凭证追溯范围）后，P0-FIN-001 聚合逻辑方可放行。
- **处置更新（2026-08-11，阻断判定解除）**：**PD-003 已决策**——当前版本不实现自动期初结转，期初金额由授权人员人工录入，调整必须记录操作人/时间/原因，禁止系统自动推导历史余额（`product-decision-backlog.md` §5）；P0-FIN-001 期初录入+审计实现经 QA 活体验收 **PASS**（`docs/quality/sprint-4-backend-qa-report.md`：授权录入/调整生效、审计可查、新行期初 0、既有行零覆盖、非授权 403）——**KL-002 阻断判定解除（2026-08-11）**，余额聚合收口路径已落地。

### KL-003｜演示 mock 数据源残留（HR 智能模块）
- **来源任务**：P0-HR-005（HR 智能 3 页面 100% Mock：员工画像/知识库智能/合同智能看板）
- **风险等级**：中
- **是否阻断发布**：否（有条件）——若生产保留入口展示虚构员工/绩效/学习数据，建议按"阻断"处理；决策前至少隐藏入口或加"演示数据"标识
- **处置**：产品决策"下线 vs 接真实 vs 演示标识"（PD-004）后执行。
- **处置更新（2026-08-11，处置完成）**：**PD-004 已决策：下线**（待真实数据源接入后重新评估）——P0-HR-005 执行完成并经 QA 独立验收 **PASS 8/8**（`docs/quality/sprint-4-frontend-qa-report.md`）：3 页面（员工画像/知识库智能/合同智能看板）路由与菜单入口已移除、全库无入口引用、URL 直达由路由守卫拦截（不渲染演示数据）、构建 EXIT=0、dist 无 3 页面 chunk——**演示数据不再对生产用户暴露，KL-003 处置完成（2026-08-11）**。残留观察项（不阻断）：3 页面文件/API/组件死代码保留待评估（PD-004 语义）、2 个死权限码（`hr:knowledge-intelligence:view` / `hr:employee-intelligence:view`）待后续清理任务。

### KL-004｜排班真实接口未接入
- **来源任务**：P0-HR-012（ShiftManagement 整页 mock）；关联 P0-HR-011（contract/salary 断流）、P0-HR-006（scheduleApi mock 死代码）
- **风险等级**：中
- **是否阻断发布**：否（入口已禁用，功能暂不可用；若恢复假成功提示则视为阻断项）
- **处置**：后端模型排期提供排班 CRUD/交接接口后接入；此前保持按钮禁用。

### KL-005｜部分展示字段硬编码（下拉/字典）
- **来源任务**：任务池 P1-DATA-001~010（盘点人员/供应商/部门/模板/签约记录/库位记录/员工门户/AI 服务商/会员统计/下拉陈旧化）+ P2-DATA-004（3.3 硬编码业务字典、3.4 types 静态字典）
- **风险等级**：低
- **是否阻断发布**：否
- **处置**：随 P1/P2 治理批次逐项接真实数据源；字典后端化依赖后端字典接口。

### KL-006｜/workspace/material-request 域级并集超放行（菜单隐藏 URL 可直达）
- **来源任务**：P0-SEC-004（Sprint-3A 卡，QA 限制 L-1，`docs/quality/sprint-3a-phase1-qa-report.md`）
- **风险等级**：低
- **是否阻断发布**：否（QA 判定不阻断；核心敏感域 finance/hr/system/device/asset 均精确）
- **现状**：`/workspace/material-request`（`request-center/menu.ts:17-24` 精确清单 [OWNER, ADMIN, STORE_MANAGER, TEAM_LEADER, DEPARTMENT_MANAGER, EMPLOYEE]）落在 workspace 域矩阵（全 14 角色并集）下 → SCHEDULER / REGION_MANAGER / AUDITOR / OPS_DIRECTOR / FINANCE_DIRECTOR / HR_DIRECTOR / PURCHASE_MANAGER / WAREHOUSE_MANAGER 共 8 角色「菜单隐藏但 URL 可直达」；该页为需求提报表单（非管理/敏感面），越权面低
- **处置**：后续在 `/workspace/material-request` 路由声明精确 `meta.roles`（或拆分子域）——拆卡见任务池 §7.6 相邻衍生卡评估；当前仅登记治理，不新拆卡（低敏感、不阻断）

### KL-007｜渗透用例（伪造 token 被拒）未执行；JWT 读取死代码残留 —— **已关闭（2026-08-10）**
- **来源任务**：P0-SEC-006（Sprint-3A 卡，QA 限制 L-2）；验证卡 P1-SEC-009（qa 责任）承接，报告 `docs/quality/p1-sec-009-qa-report.md`
- **风险等级**：中（验证缺口，非活跃漏洞：前端已 fail-closed 且 /me 为唯一授权源；后端拒绝伪造 token 的验证依赖 P0-SEC-001/002 完成后执行）
- **是否阻断发布**：否（前端侧已 PASS；渗透门禁前置项已随 P1-SEC-009 PASS 关闭）
- **现状**：P0-SEC-006 验收标准 4「伪造 token 被后端拒绝」渗透用例本批未执行（超前端验收范围）；`utils/auth.ts` 的 hasRole/hasPermission/getUserRoles/getUserPermissions/getTokenUserInfo 等 JWT 声明读取函数已无权限判定调用方（本地解码函数 parseJWTPayload 已删除，storage 旧键不再被读）——死代码残留
- **处置**：~~渗透用例随 P0-SEC-001/002（后端安全组）完成后由回归/安全组执行，或发布前渗透；JWT 死代码清理建议随 P0-AUD 批次执行（不参与权限判定，仅残留风险）~~
- **关闭结论（2026-08-10，P1-SEC-009 QA 报告）**：活体渗透 14 用例全 PASS——篡改 token（不重签）→ `/me`/finance/hr 全部 **HTTP 401**（签名校验 `JwtUtils.validateToken:261-306`）；未知角色（roles=`['hacker']`）/无权限码角色 → finance/hr/system 全部 **HTTP 403**（`@PreAuthorize` 权限码链 fail-closed）；过期 token（exp=-1h）→ 401；正向对照（合法签名伪造 admin）→ 放行证明链路真实生效。前端 0 残留 JWT roles/permissions 声明读取（grep 证据 + 仿真 26/26：篡改 localStorage → 权限与 /me 一致）。**KL-007 满足关闭条件，状态 → 已关闭**。JWT 死代码清理仍建议随 P0-AUD 批次执行（不参与权限判定，仅残留风险，不阻断）。

### KL-008｜权限码静态校验脚本缺失（注册表漂移无法自动拦截）
- **来源任务**：P1-SEC-003（Sprint-3A 卡，QA 限制 L-3）；developer 声明 4 项限制之 2（QA 独立确认属实）
- **风险等级**：低-中（预防机制缺口；当前 2 码已人工核验对齐，无活跃漂移）
- **是否阻断发布**：否
- **现状**：`scripts/` 与 `package.json` 无权限码静态校验脚本；`frontend/scripts/test-permission-security.ts` 为权限中心浏览器控制台测试脚本，非注册表静态扫描；权限码漂移（v-permission/meta 使用未注册码 → 按钮静默隐藏/误授）只能人工发现
- **处置**：**已拆卡 P2-SEC-007「权限码静态校验脚本」**（见任务池 §7.6）——扫描 v-permission 与路由 meta 权限码必须在 `permissions.ts` 注册表，接入 CI

### KL-009｜非 admin 按钮可见性依赖 role_permission 表数据，代码层无法证明
- **来源任务**：P1-SEC-003（Sprint-3A 卡，QA 限制 L-4）
- **风险等级**：低-中（受数据影响，按钮可见性需联调确认）
- **是否阻断发布**：否（回归基线候选维持，联调确认后收口）
- **现状**：非 admin 角色（OPS_DIRECTOR/STORE_MANAGER 等）的权限码发放依赖 `role_permission` 表数据（`AuthServiceImpl.getUserPermissions` L753-767 从 DB 聚合），代码层无法证明「授权角色可见两按钮」；P1-SEC-003 验收标准 1 最终成立依赖此数据
- **处置**：记录为联调待办——联调环境造数验证（后端安全组阶段或回归阶段执行），验证结果回写 P1-SEC-003 收口

### KL-010｜/forgot-password 路由基线即缺（老问题，非本次引入）
- **来源任务**：P1-SEC-002（Sprint-3A 卡，QA 观察 O-1）
- **风险等级**：低
- **是否阻断发布**：否（登录页失效入口，无权限/数据风险）
- **现状**：基线守卫白名单成员 `/forgot-password` 路由从未注册（基线及当前 router/index.ts 均无）；`LoginPage.vue:591` 仍 `router.push('/forgot-password')`；`App.vue:11`、`stores/tab.ts:17` 独立页清单仍含该路径；无视图文件
- **处置**：**已拆卡 P2-SEC-008**（见任务池 §7.6）——先移除失效入口（禁用/隐藏，与「禁用入口」约定一致）；完整找回密码流程（验证方式/重置规则）属业务规则，需先登记产品决策（BLOCKED_PRODUCT_RULE），禁止猜测实现

### KL-011｜401 防重标记 finally 重置窗口极短
- **来源任务**：P1-SEC-004（Sprint-3A 卡，QA 观察 O-2）
- **风险等级**：低（窗口极短：弹框关闭瞬间、`clearAuthAndRedirect` 同步跳转前，若有并发 401 会重弹一次；实际影响可忽略）
- **是否阻断发布**：否
- **处置**：仅记录观察，不拆卡；如后续优化可在登出跳转前置位防重标记再清理

### KL-012｜CostAnalysisController 类级 cost:view 与导出码 cost:export 不一致
- **来源任务**：P0-SEC-001 范围（QA 观察 O-3，developer 声明项独立确认属实；Sprint-3A 第二阶段 DOING 规划中）
- **风险等级**：低-中（导出接口实际仍受 cost:view 约束——若某角色仅获 export 无 view，导出接口 403；属权限过严而非越权）
- **是否阻断发布**：否
- **现状**：`CostAnalysisController.java:26` 类级 `@PreAuthorize("hasAuthority('product:cost:view')")`，与前端导出按钮码 `product:cost:export` 不一致
- **处置**：标注关联 P0-SEC-001（后端 Controller 鉴权批次）——随该卡统一类级/方法级注解与按钮码一致；P1-SEC-003 不判失败

### KL-013｜真实登录联调未在运行环境执行（/me 权限源端到端验证缺口）
- **来源任务**：P0-SEC-006（Sprint-3A 卡，QA 复审限制 L-5，`docs/quality/sprint-3a-security-qa-report.md` 第二节）
- **风险等级**：中（验证缺口，非活跃缺陷——代码路径已静态核验：/me 为唯一授权源、mapRoles fail-closed、/me 失败登出；仅缺浏览器级全链路验证）
- **是否阻断发布**：否（QA 复审判定不阻断；P0-SEC-006 维持 PASS_WITH_LIMITATION）
- **现状**：真实登录联调（登录 → /me → 路由跳转 → 按钮渲染全链路）未在运行环境执行；与 L-2（渗透用例未执行，KL-007）同属验证缺口类，代码侧已 PASS
- **处置**：回归阶段补联调验证（与 KL-009（L-4，role_permission 按钮可见性联调）同类联调待办，回归阶段合并执行），验证结果回写 P0-SEC-006 收口；不拆卡（联调验证属 QA/回归职责，planner 不替代）
- **处置更新（2026-08-11，浏览器冒烟 `docs/quality/browser-smoke-sprint3.md` §一.1）**：PROD preview（vite preview :3003）实测 5 项全 **PASS**——①登录页渲染（`.login-page h1`/用户名/密码/登录按钮在位）；②登录+路由跳转（admin/Admin@123 → /home）；③授权页可达（/finance/ledger 渲染「财务总账」无权限提示）；④权限按钮渲染（product:pricing:batch / product:cost:export 两按钮可见）；⑤401 弹框→登出（已登录会话内篡改 token → 弹框标题「登录已过期」、无「留在本页」取消按钮 → 确认登出跳 /login，fail-closed，与 REG-SEC-008 代码级结论闭环）→ **KL-013 浏览器缺口全部关闭，状态 → 已关闭（2026-08-11）**；P0-SEC-006 L-5 收口。

### KL-014｜后端 4 文件权限码清单不一致（AdminPermissions 多 10 码未同步）
- **来源任务**：P1-SEC-003（Sprint-3A 卡，QA 复审限制 L-6 / 观察 O-5，`docs/quality/sprint-3a-security-qa-report.md` 第三节）
- **风险等级**：低（维护性问题——QA 独立核验：新增 10 码仅 admin 分支生效且 admin 另有 `'*'` 兜底（hasAuthority('*')），运行期权限无增减差异；非 admin 发放文件未含新码不影响发放逻辑）
- **是否阻断发布**：否
- **现状**：`AdminPermissions.java` 含 `finance:tax:*` 4 码 + `purchase:order:*` 6 码，其余 3 个 Service 发放文件（AuthenticationServiceImpl / TokenServiceImpl / UserDetailsServiceImpl）未同步；上轮 QA「纯字符串追加 1 行」声明不准确，本轮复审独立识别；新增码与前端注册表（finance:tax:view、purchase:order:confirm/terminate/freeze/submit 已注册）及 Controller @PreAuthorize（TaxRecordController/TaxCalculationController、PurchaseOrderController）使用点对齐
- **处置**：随 P0-SEC-001（Sprint-3A 第二阶段后端鉴权批）统一 4 文件权限码清单，不拆独立卡；要求 developer 在修改证据中注明新增码归属（本批 vs 历史叠加）

### KL-015｜前端注册表超范围新增 purchase:order:terminate/freeze + cancel 描述修正
- **来源任务**：P1-SEC-003（Sprint-3A 卡，QA 复审观察 O-4，`docs/quality/sprint-3a-security-qa-report.md` 第三节）
- **风险等级**：低（良性超范围——与 PurchaseOrder.vue 页面使用点对齐方向一致，属同类注册表漂移修复；cancel 描述修正为文案级，无功能影响）
- **是否阻断发布**：否
- **现状**：`permissions.ts` 注册表在本批申报（product:pricing:batch / product:cost:export）之外新增 `purchase:order:terminate/freeze` 权限码并修正 cancel 描述；与页面使用点一致，无活跃漂移
- **处置**：仅记录，不拆卡（现状即正确，无治理动作可执行；如后续 P2-SEC-007 静态校验脚本落地可自动维持对齐）

### KL-016｜system:init:* 权限码不在前端注册表（前后端跨端不一致）
- **来源任务**：P0-SEC-005 关联（Sprint-3A 卡，QA 复审观察 O-6，`docs/quality/sprint-3a-security-qa-report.md` 第二节）
- **风险等级**：低（CompanyInitController 使用 `system:init:view/manage`，前端注册表为 `system:company-init:manage`；非 admin 无码即 403，属权限过严而非越权；admin 经 `'*'` 放行；P0-SEC-005 本身 PASS）
- **是否阻断发布**：否
- **现状**：前后端权限码命名不一致——后端 `@PreAuthorize("hasAuthority('system:init:view'/'system:init:manage')")` vs 前端注册表 `system:company-init:manage`
- **处置**：标注关联 P0-SEC-001（后端鉴权批）——随该批统一前后端权限码命名，不拆卡；与 KL-012（O-3）、KL-014（O-5）同类「随 P0-SEC-001」处置

### KL-017｜SmartRestock「建议记录」Tab 5 条硬编码假历史仍在页面可见
- **来源任务**：P1-DATA-002（Sprint-3B-1 卡，QA 限制 L-1，`docs/quality/sprint-3b1-qa-report.md` 第三节）；关联任务池 §7.7（3B-1 登记）
- **风险等级**：中（3B-1 批次**唯一可见假数据展示残留**：L345 双汇冷鲜肉直供、L374 益海嘉里粮油等 5 条硬编码假记录，页面可见假供应商名）
- **是否阻断发布**：否（有条件）——残留已挂 P1-MOCK-004（3B-2 执行中，roadmap §3B-2 已承接）；若在 3B-2 完成前发布，页面仍展示假数据，建议按阻断评估
- **现状**：`SmartRestock.vue` L340-412「建议记录」Tab 5 条硬编码假记录（baseline 既有未清理）；下拉假供应商已删（P1-DATA-002 本体 PASS）；`handleSuggestionSubmit` 仅本地 unshift 无后端持久化（P1-MOCK-004 卡范畴）
- **处置**：**归属已确认并入 P1-MOCK-004**（不拆新卡）；验收标准追加「建议记录 Tab 不再展示硬编码假历史（改真实历史接口或隐藏区块+标注）」；3B-2 完成后回写本行状态

### KL-018｜AI 服务商字典接口后端缺失 + 统计卡 0 值展示
- **来源任务**：P1-DATA-008（Sprint-3B-1 卡，QA 限制 L-3）
- **风险等级**：中（L219-221 todayCalls/successRate 统计卡显示 0——后端无统计接口，注释已说明但 0 值仍可能误导）
- **是否阻断发布**：否（服务商字典缺失已如实登记、未假造接口；provider 派生 + allow-create 为合理兜底）
- **现状**：后端无 AI 服务商字典接口与调用统计接口；前端 L93-106 从 `aiModelApi.getAll()` 真实 provider 去重派生，L88-90/L830 注释与 UI hint 如实标注
- **处置**：待后端排期（服务商字典 + 调用统计接口，同 KL-005 类治理）；排期前前端可将 0 值改为 `--` 防误导（低风险 A 类顺手项）

### KL-019｜会员统计 3 字段口径缺失（消费占比/客单价/复购率）
- **来源任务**：P1-DATA-009（Sprint-3B-1 卡，QA 限制 L-4）；决策登记 PD-005（`product-decision-backlog.md`）
- **风险等级**：低-中（业务字段缺失展示属功能降级而非数据失真；恢复实现依赖口径决策）
- **是否阻断发布**：否（3 字段已按任务卡授权**临时移除 + 前端 `--` 降级**，无假数据残留）
- **现状**：consumeRatio/avgOrderAmount/repurchaseRate 无文档化计算口径（分母/分子、统计周期、复购判定窗口未定义）；后端 ServiceImpl 未实现、接口不返回；`MemberOverview.vue:34` repurchaseRate 显示 `--`、`MemberList.vue:468-477` 仅渲染 4 卡真实字段；`types/member.ts:230-234` 3 字段仍声明必填（类型未对齐，运行时安全）
- **处置**：**口径=计算规则，产品/财务定义前禁止猜测实现（BLOCKED_PRODUCT_RULE）**——已登记 PD-005 等待决策；决策后回写 P1-DATA-009 恢复验收标准并同步修正 types 类型；决策前保持 `--` 降级

### KL-020｜运行期联调未执行（无 DB 环境）
- **来源任务**：P1-DATA-009/010（Sprint-3B-1 卡，QA 验证缺口 V-1/V-2）
- **风险等级**：中（验证缺口，非活跃缺陷——DATA-009 SQL 口径经源码核验（RechargeRecordMapper/OrderNewMapper 同口径），DATA-010 机制经代码核验；仅缺运行期/浏览器级验证）
- **是否阻断发布**：否（代码侧已 PASS）
- **现状**：V-1 后端聚合正确性（建会员/充值/消费后 overview 与 DB 一致）与真实员工/部门/供应商数据联调未执行；V-2 DATA-010 版本切换触发重载未做浏览器交互验证
- **处置**：回归阶段闭环（回归 Agent 职责，与 KL-009/KL-013 联调待办同类合并执行），验证结果回写 P1-DATA-009/010 收口；不拆卡
- **处置更新（2026-08-10，回归联调 `docs/quality/sprint-3-closeout-regression-liaison.md`）**：联调执行证实为**活跃缺陷 DEF-1/DEF-2**（非验证缺口）——`OrderNewMapper.java:66-69` `sumMemberConsumeInRange` 与 L206 `getDailyTrendByStore` 的 @Select 注解中 `&gt;=`/`&lt;` XML 实体未反转义 → PG「字段 gt 不存在」→ GET /v1/members/stats/overview 恒 500；DEF-2 另致 DecisionBoard/OperationsReport/LiveMonitor 营收趋势被 catch 静默降级恒空。**已建卡 P0-DATA-009（发布阻断，状态 DOING，developer 修复中）**；修复 + QA/回归 -R 复验通过前本 KL **不得关闭**；V-2 代码级 PASS 结论维持不变；原「不拆卡」登记基于验证缺口假设，随活跃缺陷暴露而不再适用
- **处置更新（2026-08-10，KL-020 重跑 `docs/quality/p0-kl-020-rerun.md`，regression 活体实证）**：
  - **DEF-1 已关闭**：overview 活体重跑 200 + code=0，基线/造数 8/9 字段与 DB 精确匹配（造数增量 +500.00/+200.00 等可证）；**DEF-2 已关闭**：live-monitor/trend、decision-board/revenue-trend、kpi-summary 三接口 200 + code=0，无 500、无静默降级（admin 空数据=KL-048 已登记表现，非 DEF-2）；V-2 org-context 切换重载维持**代码级 PASS**（浏览器交互缺口单独登记，KL-022 无切换 UI 不变）。
  - **新暴露活跃缺陷 DEF-4**（静默数据失真，字段级恒 0 非 500）：KL-020 重跑**非空充值造数**（recharge_records 本月成功 +100000 分）实证 `GET /v1/members/stats/overview` rechargeThisMonth 恒 `0.00`（DB 同口径=100000 分）、`GET /v1/recharge-stats/overview` 11 个统计字段全 0/0.00——根因 `RechargeRecordMapper.xml:6-38` `selectStatsOverview` 9 个别名驼峰未加引号 → PostgreSQL 折叠为小写（psql 实证 rechargethismonth）→ MyBatis Map key 全小写 → `MarketingMemberServiceImpl.java:230-233` / `RechargeStatsServiceImpl.java:39-54` 按驼峰 get 取 null → 恒 0。QA 复验（p0-data-009-r1）时 recharge_records 表空（0 行）SUM=0 恰好掩盖；属既有代码缺陷（P1-DATA-009 修复/验收未覆盖非空充值场景），非本次修复引入。
  - **处置**：**已建卡 P0-DATA-010（发布阻断，状态 DOING）** + 独立登记 **KL-049**（与卡关联，追踪链：KL-020 → DEF-4 → KL-049 → P0-DATA-010）；P0-DATA-009 卡验收标准 1 非空充值缺口由 P0-DATA-010 承接；**本 KL 状态=部分关闭（DEF-1/DEF-2/V-2 已关闭；DEF-4 修复 + QA/回归 -R 复验通过前不得全关、发布门禁保持 FAIL）**
  - **处置更新（2026-08-10，DEF 修复批次收口）**：DEF-4 经 **P0-DATA-010** 修复（`RechargeRecordMapper.xml` selectStatsOverview 9 个别名加双引号 + 全 mapper 同型扫描清 15 文件，diff 纯引号 157/157 零语义变更）→ QA 复验 `docs/quality/p0-data-010-r1-qa-report.md` = **PASS 7/7**（非空充值造数 rechargeThisMonth=1250.00 与 DB 精确一致、recharge-stats 11 字段全非 0 逐字段对账、造数 0 残留）→ 回归转基线 **REG-DATA-017**（`production-regression-test.md`）；**KL-020 满足全关条件 → 状态更新为已关闭**（活跃缺陷 DEF-1/DEF-2/DEF-4 全部修复并经 QA 复验 + regression 重跑闭环）；残留 **V-2 浏览器切换交互缺口**（无浏览器自动化环境，代码级 PASS）登记「发布前浏览器冒烟」补验，验证缺口非代码缺陷、不构成 FAIL。
- **处置更新（2026-08-11，浏览器冒烟 `docs/quality/browser-smoke-sprint3.md` §一.3）**：**KL-020-V2 浏览器切换交互缺口实测关闭（PASS）**——DecisionBoard 门店下拉渲染 3 项（`DeepStore-e2e-deep-msb9dm2j` / 测试门店A / 测试门店B，含历史 e2e 残留门店如实登记）；选中/切换/清空 4 次操作均触发 `/v1/stores/active` 重载（请求计数 1→2→3→4），org-context version→useStoreOptions watch 重载机制浏览器级闭环（旧数据不可选）→ **KL-020 关闭链完整**（DEF-1/DEF-2/DEF-4 + V-2 全部闭环，无残留缺口）。

### KL-021｜InventoryAdjust 会签状态展示为本地 mock
- **来源任务**：P1-DATA-003（Sprint-3B-1 卡，QA 限制 L-2）
- **风险等级**：低（L383-388 `getDeptSignStatus` 会签状态展示仍为本地 mock，代码注释已标明「mock」；仅展示层，不影响提交数据）
- **是否阻断发布**：否
- **现状**：`InventoryAdjust.vue:383-388` 显示"已签/待签"非后端真实状态
- **处置**：登记治理——后端补会签状态字段或移除状态展示（低风险，可随 P2-DATA-004 字典治理窗口或独立顺手项执行）

### KL-022｜DATA-010 无组织切换 UI
- **来源任务**：P1-DATA-010（Sprint-3B-1 卡，QA 限制 L-5）
- **风险等级**：低（机制已就绪——org-context version 递增 + useStoreOptions/useDepartmentOptions 监听重载均验证正确；仅缺切换入口）
- **是否阻断发布**：否
- **现状**：`setOrg(` 零调用，无组织切换 UI；仅 DecisionBoard 一处门店切换入口，其余页面下拉在无切换入口时静默跟随 version
- **处置**：组织切换 UI 属**产品功能范围**决策（非规则缺失，不进 PD 队列）；仅登记，待产品排期功能需求；机制可先行保留

### KL-023｜3B-1 批次无法按 commit 分离
- **来源任务**：P1-DATA-001~010（Sprint-3B-1 卡，QA 限制 L-6）
- **风险等级**：低（工程/流程项——工作区 336 个未提交文件为跨批次聚合，16 个 3B-1 文件 diff 混入跨批次 UI 改动（`:teleported="false"→true`、lock-scroll 删除），回归归因困难）
- **是否阻断发布**：否
- **现状**：Sprint-1/2/3A/3B 无 commit 分隔，QA 只能按文件归因
- **处置**：工程流程建议——本 Sprint 验收后按模块分批 commit，便于后续回归归因与批次追溯

### KL-024｜InventoryOutbound 假供应商列表残留
- **来源任务**：QA 观察 O-2（Sprint-3B-1，`docs/quality/sprint-3b1-qa-report.md` 第二节残留项归属；未在 3B-1 16 文件内、审计未拆卡遗留）
- **风险等级**：低-中（页面可见假供应商名（双汇/益海嘉里/蒙牛），与 P1-DATA-002 修复前同型残留；出库单据供应商字段失真风险）
- **是否阻断发布**：否（有条件：假数据可见，建议 3B 收口前完成）
- **现状**：`InventoryOutbound.vue:54-56` 3 家硬编码假供应商
- **处置**：**已拆卡 P1-DATA-011**（任务池 §7.7）——改接真实供应商接口（复用 P1-DATA-002 已接 `supplierApi.getEnabledList()` 同模式）；建议随 3B 窗口执行

### KL-025｜SignLinkManagement 续签/删除静默成功 + 载荷语义待核对
- **来源任务**：QA 观察 O-3（Sprint-3B-1，P1-API-001 范畴既有，`docs/quality/sprint-3b1-qa-report.md` 第四节）
- **风险等级**：低（超范围既有项——P1-API-001 已验收含限制（HR-009）；本观察不改变 3B-1 结论）
- **是否阻断发布**：否
- **现状**：`SignLinkManagement.vue` handleRenew/handleDelete 无 API 调用直接 success；handleSubmit 载荷不含 linkName/supplierId，templateId 映射到 eContractId 字段语义待后端核对
- **处置**：挂 P1-API-001 既有范畴（随收尾批次评估，不拆新卡）

### KL-026｜InventoryLocation getByLocationId 失败回退 Mock
- **来源任务**：QA 观察 O-4（Sprint-3B-1，P1-API-003 范畴既有）
- **风险等级**：低（`inventoryApi.getByLocationId` 失败时 API 内部回退 Mock，L76/302 注释自述）
- **是否阻断发布**：否
- **处置**：挂 P1-API-003 既有范畴（随后端接口排期），不拆新卡

### KL-027｜HRContract 编辑 probationMonths: 3 硬编码覆盖
- **来源任务**：QA 观察 O-5（Sprint-3B-1，P0-DATA-008 既有问题，独立收尾批次）
- **风险等级**：低（编辑回填硬编码覆盖试用期字段，属 P0-DATA-008 组问题；本观察不改变 3B-1 结论）
- **是否阻断发布**：否
- **现状**：`HRContract.vue:347` handleEdit 仍 `probationMonths: 3` 覆盖
- **处置**：随 P0-DATA-008 独立收尾批次执行（任务池 §1.4 P0-DATA-008 卡）

### KL-028｜MemberOverview 分布字段空展示
- **来源任务**：QA 观察 O-6（Sprint-3B-1，P1-DATA-009 关联）
- **风险等级**：低（等级分布/客户分层/增长 Tab 依赖 overview 接口未返回的分布字段 → 空展示；属既有降级，非假数据）
- **是否阻断发布**：否
- **处置**：观察——关联 KL-019（overview 接口域）；如需展示分布字段，待后端 overview 字段扩展或产品确认范围后实施

### KL-029｜SmartRestock 采购申请运行期联调未执行（V-1）
- **来源任务**：P1-MOCK-004（Sprint-3B-2 卡，QA 验证缺口 V-1，`docs/quality/sprint-3b2-qa-report.md` §4.1）
- **风险等级**：中（验证缺口，非活跃缺陷——真实 POST /v1/purchase/requests 链路经代码级契约核验：`PurchaseRequestController.java:58` 存在、载荷与 `PurchaseRequestCreateParams` 一致、`converters.ts:180-196` 字段映射通过；仅缺运行时落库/采购侧可见验证）
- **是否阻断发布**：否（代码侧已 PASS）
- **现状**：`SmartRestock.vue:300-352` handleSuggestionSubmit 已由本地 unshift+假成功改为真实 `purchaseRequestApi.create()`；无运行环境，提交后 DB 落库与采购侧可见性未验证
- **处置**：回归阶段闭环（回归 Agent 职责，与 KL-009/KL-013/KL-020 联调待办同类合并执行），验证结果回写 P1-MOCK-004 收口；不拆卡

### KL-030｜导出 CSV 文件内容运行期验证未执行（V-2）
- **来源任务**：P1-EXPORT-002（Sprint-3B-2 卡，QA 验证缺口 V-2，`docs/quality/sprint-3b2-qa-report.md` §4.1）
- **风险等级**：中（验证缺口，非活跃缺陷——10 页 CSV 生成逻辑（BOM/引号转义/列映射/tableData 来源）代码级核验通过；仅缺下载文件内容解析验证）
- **是否阻断发布**：否（代码侧已 PASS）
- **现状**：7 卡页 + 2 附加页 + HRTraining 导入导出全部改为基于 tableData 的真实 CSV（`\ufeff` BOM + 转义 + Blob 下载）；无占位 Blob 残留；未做浏览器级下载文件内容验证
- **处置**：回归阶段闭环（回归 Agent 职责，与 KL-029/KL-031 同批），验证结果回写 P1-EXPORT-002 收口；不拆卡
- **处置更新（2026-08-11，浏览器冒烟 `docs/quality/browser-smoke-sprint3.md` §一.2 + §二）**：抽样 3 页实测——**OrderQuery 导出 PASS**（下载 `订单数据_*.csv` 370B：BOM `\ufeff` ✓ + 表头 13 列 ✓ + 1 条真实数据行 ✓，元→分→元保真，KL-030 主体闭环）；**AssetDepreciation 折旧导出 FAIL（DEF-A）**：页面白屏 `ReferenceError: InvStatus is not defined`（`frontend/src/api/asset/inventory.ts:6`；L13-18 JSDoc 注释未闭合 `/*`21 vs `*/`20，吞掉 import，**基线 commit dd33ee3 即存在**）→ 资产模块全部页面 dev+PROD 双环境白屏（/asset/depreciation、/asset/inventory-check、/asset/ledger 实测 3 页全崩）；**DeviceAlerts 告警导出 FAIL（DEF-B）**：`GET /v1/device-alerts/page` 业务 code=500（`Device.java` device_type Integer vs `devices.device_type` varchar(50) 实体-表漂移，设备存在时映射失败）→ 导出文件仅 BOM+表头 9 列、0 数据行。**处置：已建卡 P0-ASSET-001 / P0-DEVICE-001（发布阻断，状态 DOING）**；本 KL 状态=**部分关闭（OrderQuery PASS + 活跃缺陷 DEF-A/DEF-B 建卡 P0-xxx）**——两缺陷修复 + QA/回归 -R 复验通过前**不关闭**、冒烟门禁 **FAIL（禁止放行）**；修复后重跑折旧/告警两页导出（冒烟报告 §四 补验项 1/2/3：折旧导出 CSV 9 列 fenToYuan 200.00 断言、告警导出 CSV 9 列告警内容断言、资产域其余页冒烟）即可闭环本 KL。
- **处置更新（2026-08-11，DEF-A/B QA 复验收口）**：**P0-ASSET-001 = QA 复验 PASS**（`docs/quality/p0-asset-001-r1-qa-report.md`，5/5 实证：基线 dd33ee3 vue-tsc 复现 TS2304 → 工作区 0 错误、git diff 仅 inventory.ts +1 行 `*/`、业务语义零变更、npm run build EXIT=0、无夹带）；**P0-DEVICE-001 = QA 复验 PASS**（`docs/quality/p0-device-001-r1-qa-report.md`，6/6 达成含活体实测：三接口 200 code=0、deviceType 字符串往返、空表不回归、mvn EXIT=0）——**两缺陷修复闭环，QA 侧 -R 复验全部通过**；剩余闭环项=**折旧/告警两页导出浏览器重跑**（冒烟报告 §四 补验项 1/2/3，含资产域其余页冒烟）与冒烟门禁复评，**以 regression 输出为准**（`production-regression-test.md`），重跑通过后本 KL 方可关闭；同型漂移（connectionType/storeId/迁移文件列名）已登记 **KL-050/KL-051**（audit 立项建议），KDS 展示观察登记 **KL-052**。

### KL-031｜TopNavbar 通知已读运行期联调未执行（V-3）
- **来源任务**：P1-MOCK-010（Sprint-3B-2 卡，QA 验证缺口 V-3，`docs/quality/sprint-3b2-qa-report.md` §4.1 及 R1 复验节）
- **风险等级**：中（验证缺口，非活跃缺陷——`api/notification.ts` 端点（GET /notifications、PUT /{id}/read、PUT /read-all）与后端 `NotificationController.java:435-508` 逐一匹配；实体契约 `Notification.java` isRead(Integer 0/1)/id(notification_id) 与前端 `read: isRead===1` 一致；仅缺真实登录态接口验证）
- **是否阻断发布**：否（代码侧已 PASS；P1-MOCK-010 已 R1 复验整卡 PASS）
- **现状**：TopNavbar 3 条假通知已删除，接真实 API；未做运行环境登录态验证
- **处置**：回归阶段闭环（回归 Agent 职责，与 KL-029/KL-030 同批），验证结果回写 P1-MOCK-010 收口；不拆卡
- **处置更新（2026-08-10，回归联调 `docs/quality/sprint-3-closeout-regression-liaison.md`）**：联调执行——read/read-all 真实登录态落库 **PASS**（DB is_read 0→1、read_time 写入，V-3 核心闭环）；GET /v1/notification/notifications 暴露**活跃缺陷 DEF-3**（`Notification.java:31-35` 声明 business_id/business_type，但 `V20260425__create_notification_system_tables.sql` 建表从未含两列 → 实体-表漂移 → selectList 生成含不存在列的 SELECT → PG「字段 business_id 不存在」→ 恒 500）。**已建卡 P0-MOCK-001（发布阻断，状态 DOING，developer 修复中）**；修复方式待架构裁决（migration 补列 vs 移除字段）；修复 + QA/回归 -R 复验通过前本 KL **不得关闭**；原「不拆卡」登记基于验证缺口假设，随活跃缺陷暴露而不再适用
- **处置更新（2026-08-10，regression 重跑 `docs/quality/p0-kl-031-rerun.md` 收口）**：DEF-3 经 **P0-MOCK-001** 修复（migration `V20260810_001` 补列 business_id/business_type，架构裁决方案 A；QA 复验 `p0-mock-001-r1-qa-report.md` = **PASS 7/7**）+ 本重跑三端点真实登录态活体闭环（GET 200 code=0 + 造数 `KL031RR-` 3 条可见、businessId/businessType 反序列化含 NULL 兼容、read/read-all 落库 is_read 0→1 + read_time、造数 0 残留）→ **KL-031 状态更新为已关闭**；残留 **R1 浏览器通知面板 UI 交互**（TopNavbar 面板下拉/未读徽标渲染/点击已读即时刷新，无浏览器自动化环境）如实登记，随发布前浏览器冒烟补验（验证缺口，非代码缺陷、不构成 FAIL）；P0-MOCK-001 转基线 **REG-MOCK-006**。
- **处置更新（2026-08-11，浏览器冒烟 `docs/quality/browser-smoke-sprint3.md` §一.4）**：**R1 浏览器通知面板 UI 交互实测关闭（PASS）**——造数 2 条（`BSSM3-NOTIF-1/2`，notification_id=17/18，is_read=0）→ ①面板列表渲染（标题/时间格式化正确）；②未读徽标=2；③单条已读（点击 → 徽标 2→1 + DB id=17 is_read 0→1、read_time 落库）；④全部已读（徽标隐藏 + DB id=18 落库，PUT 200）；⑤清理 0 残留（user_id=1 基线恢复）——5 项全 PASS → **KL-031 关闭链完整**（DEF-3 + V-3 + R1 全部闭环，无残留缺口）。

### KL-032｜门店招聘域后端缺失（岗位 CRUD 缺失 + approvals 占位）
- **来源任务**：P1-MOCK-001（Sprint-3B-2 待后端清单，O-2 后端占位；`docs/quality/sprint-3b2-qa-report.md` §七）
- **风险等级**：中（功能不可用；前端已按「无接口→禁用+标注」兜底，无假数据）
- **是否阻断发布**：否（4 写操作已禁用 + el-alert 标注，符合兜底约定）
- **现状**：`StoreManagementRecruitmentController.java` 全端点 = approvals 审批流（submit/approve/reject/my-list/pending-me/detail），**store-management 域确无岗位 CRUD**；后端 `GET /approvals`（L67-80）为占位实现恒返空分页；前端 `StoreRecruitment.vue:216-260` 4 写操作全部 warning 禁用
- **处置**：待后端排期（同 KL-004/KL-018 类）；是否复用 HR 域招聘需求实体（`RecruitmentRequirementController.java:62-81` 真实 CRUD）属产品/架构裁决（见 KL-038），排期前不得猜测接线

### KL-033｜盘点计划实体/接口后端缺失
- **来源任务**：P1-MOCK-003（Sprint-3B-2 待后端清单；`docs/quality/sprint-3b2-qa-report.md` §七）
- **风险等级**：中（盘点计划功能整体不可用；前端已禁用 + 标注）
- **是否阻断发布**：否（计划 Tab 禁用 + el-alert/el-empty；盘点记录 Tab 真实接口保留）
- **现状**：grep 全库 **0 命中** InventoryCheckPlan/check_plan/CheckPlan 实体；`InventoryCheck.vue:426-442` 5 条硬编码计划删除（planList=[]），5 个写操作全部 warning 禁用（L528-566）；`planApiReady=false` 为死标志（仅 UI 冗余）
- **处置**：待后端排期（盘点计划实体 + CRUD/启停/执行接口）；排期后接入并移除死标志

### KL-034｜告警 severity 升级端点后端缺失
- **来源任务**：P1-MOCK-010-1（Sprint-3B-2 待后端清单；`docs/quality/sprint-3b2-qa-report.md` §七）
- **风险等级**：中（告警升级功能不可用；前端已禁用 + warning）
- **是否阻断发布**：否
- **现状**：后端 `/v1/alerts` 仅 acknowledge/resolve/delete/statistics（`AlertController.java`），确无 severity 升级端点；`AlertCommandCenter.vue:272-291` 假成功删除 → warning 禁用
- **处置**：待后端排期（severity 升级端点）；排期前保持禁用

### KL-035｜待办任务创建端点缺失 + 注释口径不实（O-3）
- **来源任务**：P1-MOCK-010-2（Sprint-3B-2 待后端清单，观察 O-3；`docs/quality/sprint-3b2-qa-report.md` §4.2/§七）
- **风险等级**：中（证照预警任务无法生成；前端已禁用 + warning；另含注释口径问题）
- **是否阻断发布**：否
- **现状**：后端**存在** `StoreManagementTaskController`（/v1/store-management/tasks：查询/完成/批量完成/跳转）但**无创建端点**；`StoreCertificate.vue:819-838` taskInfo 构造即丢弃 + 假成功已删除 → warning 禁用（行为正确）；代码注释「无 todo/待办 controller」与事实不符（O-3）
- **处置**：待后端排期（任务创建端点）；注释口径修正属 P2-CLEAN 类（仅登记不拆卡，随后端排期或顺手项修正）

### KL-036｜采购建议记录查询接口后端缺失
- **来源任务**：P1-MOCK-004 / L-1 闭环（Sprint-3B-2 待后端清单；`docs/quality/sprint-3b2-qa-report.md` §七）
- **风险等级**：中（建议历史不可查；前端已空态 + el-alert 兜底）
- **是否阻断发布**：否（建议记录 Tab 空态 + el-alert，无假数据残留——L-1/KL-017 已闭环）
- **现状**：`InventoryWarningController` 确无建议历史查询端点；`SmartRestock.vue:546-563` suggestionHistory 5 条假历史删除 → 空态 + el-alert
- **处置**：待后端排期（采购建议记录查询接口）；排期后接入（与 KL-029 采购侧可见联调关联）

### KL-037｜质检批量导入端点后端缺失
- **来源任务**：P1-EXPORT-002（Sprint-3B-2 待后端清单；`docs/quality/sprint-3b2-qa-report.md` §七）
- **风险等级**：中（质检批量导入不可用；前端已禁用 + tooltip）
- **是否阻断发布**：否
- **现状**：后端 `QualityController`（/v1/quality）确无导入端点；`TraceabilityQuality.vue` 导入按钮禁用 + tooltip（导出 CSV 真实化已 PASS）
- **处置**：待后端排期（质检批量导入端点）；排期前保持禁用

### KL-038｜门店招聘岗位实体复用属产品/架构裁决（O-1）
- **来源任务**：P1-MOCK-001（Sprint-3B-2，观察 O-1；`docs/quality/sprint-3b2-qa-report.md` §4.2）
- **风险等级**：低（表述过宽 + 接线方向未定；本轮未擅自接线，合规）
- **是否阻断发布**：否
- **现状**：注释「后端无岗位 CRUD」表述过宽——HR 域存在真实 `/v1/recruitment-requirements` CRUD（`RecruitmentRequirementController.java:62-81` + `api/hr/recruitment.ts:139-178`）；门店招聘岗位 ↔ 招聘需求实体是否复用属产品/架构裁决
- **处置**：登记待产品/架构裁决（**非业务规则缺失，不进 PD 队列**，同 KL-022 先例）；裁决前不猜测接线；裁决结论回写后按结论执行（复用或后端新建）

### KL-039｜HRTraining 报名员工选项硬编码（O-4）
- **来源任务**：P1-EXPORT-002（Sprint-3B-2，观察 O-4；Sprint-2 HR-002 遗留，非本批引入；`docs/quality/sprint-3b2-qa-report.md` §4.2）
- **风险等级**：低-中（报名真实落库但员工身份为伪造值——EMP001~004 无真实员工对应，报名记录数据真实性失真）
- **是否阻断发布**：否（有条件：报名记录员工身份失真，建议接线真实员工 API 后收口）
- **现状**：HRTraining 报名员工选项仍为 4 个硬编码员工（EMP001~004）
- **处置**：**已拆卡 P1-DATA-012**（任务池 §7.8，同 P1-DATA-001 员工接口模式）；建议随 3B-3 窗口执行

### KL-040｜3B-2 批次超范围 UI 行为变更（O-5）
- **来源任务**：P1-MOCK-001/003/004/009/010、P1-EXPORT-002（Sprint-3B-2，观察 O-5；`docs/quality/sprint-3b2-qa-report.md` §4.2）
- **风险等级**：低（`:teleported="false"→true` 全局替换约 15 文件/50+ 处 + `lock-scroll="false"` 移除 + TopNavbar CSS `--fts-safe-top`；不在任务卡范围，UI 行为变更）
- **是否阻断发布**：否
- **现状**：批次内全局替换混入任务卡范围之外；QA 判定低风险；行为回归未做
- **处置**：观察登记（关联 KL-023 分批 commit 归因）；行为回归放回归阶段（与 KL-029~031 联调验证同批）；不影响本批验收结论

### KL-041｜StoreCertificate 既有「模拟消息推送」机制（O-6）
- **来源任务**：P1-MOCK-010（Sprint-3B-2，观察 O-6；非本批引入；`docs/quality/sprint-3b2-qa-report.md` §4.2）
- **风险等级**：低（本地 flowNotifications + `[通知]` info 提示，无落库；本地提示非假成功文案）
- **是否阻断发布**：否
- **现状**：`StoreCertificate.vue:140-202` sendFlowNotification 仅本地 flowNotifications + info 提示，无落库；本批未触及
- **处置**：登记（模拟消息侧写）——待后端消息域接线（与 KL-031 通知域联调可合并评估）；不拆卡

### KL-042｜StoreCertificate 本地过滤「查询完成」成功措辞（O-7）
- **来源任务**：P1-MOCK-010（Sprint-3B-2，观察 O-7；Sprint-2 遗留，挂 P1-API-001 范畴；`docs/quality/sprint-3b2-qa-report.md` §4.2）
- **风险等级**：低（本地过滤后弹「查询完成」属 P1-API-001 类成功措辞，非假数据）
- **是否阻断发布**：否
- **现状**：`StoreCertificate.vue:942` 本地过滤后弹「查询完成」（P1-API-001 类）
- **处置**：挂 P1-API-001 既有范畴（随收尾批次评估，不拆新卡，同 KL-025 模式）

### KL-043｜设计公开端点豁免清单未文档化（SecurityConfig permitAll 34 端点）
- **来源任务**：P0-SEC-001-D（34 设计豁免）；`scripts/scan-controller-authz.py`（EXEMPT_CLASSES 仅 2 类）
- **风险等级**：低（34 端点均为既有 permitAll 设计公开端点——认证/公开查询/静态资源/设备注册/回执核验等，非业务越权面；缺口在"清单无权威文档化"，而非端点本身失守）
- **是否阻断发布**：否
- **现状**：`SecurityConfig.java:84-164` permitAll 块约 34 组端点（developer D 报告口径）；扫描脚本 `EXEMPT_CLASSES = ("AuthController", "PosAuthController")` 仅覆盖认证入口类，34 设计豁免端点未纳入脚本豁免清单——`--strict` 门禁下这些端点对应 Controller 若未加 @PreAuthorize 会被计为未覆盖，覆盖率 100% 判定需豁免清单对齐
- **处置**：登记为「设计公开端点豁免清单」——建议将 34 端点清单落地为脚本可读豁免清单（或独立文档供 `scan-controller-authz.py --strict` 参考），与 P2-SEC-007 静态校验脚本治理同批评估；P0-SEC-001 整卡「覆盖率 100%」达成口径须注明排除项（34 设计豁免 + 30 BLOCKED 候选（PD-006~008）+ 6 H5 边界端点）
- **补录（2026-08-10）**：豁免清单 34 → **35**，追加 `/v1/mp/**`（SecurityConfig L146，`MiniProgramController.getOpenId` 豁免成立——既有 permitAll 设计、无用户会话（微信 openId 换取）、加注解会破坏小程序免登流；QA B 观察 O-B-3 确认「实际免登，不构成越权面」）；补录后扫描未覆盖 53 → 52；**标注待架构核对确认**（developer B 建议"架构核对"，随 P2-SEC-007/KL-043 治理同批闭环）；全程见 KL-046

### KL-044｜跨域通用审批接口无权限码（ApprovalWorkflow 13，范围外）
- **来源任务**：P0-SEC-001 整卡排除项·范围外 17（SEC-001-B developer 报告 + QA B 报告 §六验收要点 5 确认，`docs/quality/p0-sec-001b-qa-report.md`）
- **风险等级**：中（跨域通用审批接口无权限码，审批操作面跨多个业务域；已纳入整卡排除项，闭环路径=PD-009）
- **是否阻断发布**：否（整卡 PASS_WITH_LIMITATION、FAIL=0；收口依赖产品/架构决策 PD-009）
- **现状**：`controller/approval/ApprovalWorkflowController` 13 方法（/v1/approval/workflows：submit/approve/reject/withdraw，按 businessId 通用审批）——跨域通用审批组件，非单一业务域；无已注册权限码/角色归属依据；QA B 判定属「通用审批权限设计」架构缺口（非单一 BLOCKED 产品规则类），登记建议=架构立项
- **处置**：**登记决策池 PD-009**（审批归属属业务规则（BLOCKED_PRODUCT_RULE 明示类型）+ 通用组件权限模型属架构设计，产品+架构共同确认）；决策前不猜测权限码/角色；决策后回写 P0-SEC-001 整卡排除项闭环

### KL-045｜QuickStockIn 3 方法未覆盖（库存域，范围外）
- **来源任务**：P0-SEC-001 整卡排除项·范围外 17（QA B 报告 §六；C 批验收时根包 94/101 的 7 未覆盖不含 QuickStockIn——文件不在 C 批清单内）
- **风险等级**：低-中（快速入库接口无权限码；库存域归属明确（QA B 确认属 inventory 域），inventory:* 码已注册（C 批 19 仓储 Controller 同型先例），修复路径清晰）
- **是否阻断发布**：否
- **现状**：根包 `QuickStockInController` 3 方法（matchBarcode/executeStockIn/parseDate，/v1/quick-stock-in）
- **处置**：**已拆卡 P2-SEC-010**（任务池 §7.6 追加）——补 inventory:* @PreAuthorize（复用 C 批同域码，不新增业务权限）；建议随下一周期（P2-SEC-009 权限码治理同批窗口）执行

### KL-046｜MiniProgram getOpenId permitAll 豁免成立但原不在豁免清单（范围外）
- **来源任务**：P0-SEC-001 整卡排除项·范围外 17（QA B 报告观察 O-B-3）
- **风险等级**：低（`/v1/mp/**` SecurityConfig L146 已 permitAll，实际免登，不构成越权面；缺口=豁免清单未含该端点）
- **是否阻断发布**：否
- **现状**：`controller/h5/MiniProgramController.getOpenId`（POST /v1/mp/getOpenId）无注解；SecurityConfig L146 `/v1/mp/**` permitAll 原不在 KL-043 34 豁免清单 → 扫描脚本如实计为未覆盖（53 中 1）；若加 @PreAuthorize 会破坏小程序免登流（无用户会话，微信 openId 换取）
- **处置**：**豁免成立，已补录 KL-043 豁免清单（34→35，追加 /v1/mp/**）**；**待架构核对确认**（developer B 建议"架构核对"）；扫描脚本 PERMIT_ALL_METHODS 对齐随 P2-SEC-007/KL-043 治理同批

### KL-047｜退款审批 approveUserId 信任客户端参数（审批归属）
- **来源任务**：P1-SEC-009 QA 观察 O-1（`docs/quality/p1-sec-009-qa-report.md` §二）；决策登记 **PD-010**（`product-decision-backlog.md`）
- **风险等级**：低-中（审批授权已受 `@PreAuthorize('order:manage')` 管控；但审批人**身份归因**信任客户端参数——恶意客户端可将审批记录记为他人；属业务规则问题，QA 不猜测正确性）
- **是否阻断发布**：否（QA 判定不阻断本卡与发布门禁——授权仍受 @PreAuthorize 管控；O-1 登记供 planner/产品评估）
- **现状**：`OrderNewController.java:214-223` / `OrderNewServiceImpl.java:386-424` 的退款审批 approveRefund 以客户端提交的 `approveUserId`（来自前端 `OrderRefund.vue:198` 解码 JWT `id` 声明）记录审批人，后端未校验其与认证主体一致
- **处置**：**登记 PD-010 待产品决策**——业务规则缺失（BLOCKED_PRODUCT_RULE 类型「审批归属」）：approveUserId 应从服务端会话派生（如 `SecurityUtils.getCurrentUserId()` 覆盖）还是允许客户端指定？决策前禁止猜测实现；决策后回写对应任务卡验收标准

### KL-048｜趋势接口对 admin 返回空数据（resolveStoreFilter 将 null 误判为无权限，O-DEF2-1）
- **来源任务**：P0-DATA-009 观察登记 **O-DEF2-1**（来源 developer 修复报告 §五.1——P0-DATA-009 卡修改证据「风险」行注记的独立缺陷；`production-remediation-task-board.md` P0-DATA-009 卡「观察登记」节）
- **风险等级**：中-低（admin 用户看运营看板趋势恒空——**数据可见性问题，非越权**；与 P0-DATA-009 本体 DEF-1/DEF-2（overview 恒 500，P0 级）不同类：无 500、无全用户范围故障、无越权面）
- **是否阻断发布**：**否（修复前判定）**——admin 趋势空属数据可见性缺陷，非 500 级故障，不阻断发布（与 DEF 的 P0 区分：P0 阻断口径=接口不可用/数据失真活跃缺陷；本项仅 admin 角色趋势可见性缺失）；但**发布前需架构裁决**（数据权限语义），裁决结论回写后按批次修复
- **现状**：趋势接口（`live-monitor/trend`、`decision-board/revenue-trend`）对 admin 返回空数据；根因 `resolveStoreFilter`（`LiveMonitorServiceImpl.java:283-284` / `DecisionBoardServiceImpl.java:353-354`）把 `getAccessibleStoreIds` 返回的 **null**（语义=不限制，`DataPermissionServiceImpl.java:362-367` 注释明确「管理员直接返回 null」）误判为无权限 → `NO_ACCESS_STORE_ID=-1` → `AND store_id=-1` → 空结果
- **性质**：涉**数据权限业务语义**（null=不限制 vs 无权限 的区分处理），超出 DEF-2 卡范围；不属 SQL 转义缺陷（与 DEF-1/DEF-2 不同根因）
- **处置**：**不入 PD 池**（判断见下）；登记为**待修缺陷**（挂后续批次，建议 P1/P2-DATA 收尾批次或独立卡）——修复方向明确：null → 不限制，不设 store_id 条件（等价于不对门店过滤）；**状态=待架构裁决（数据权限语义）**，裁决后放行修复
- **PD 判断结论（2026-08-10，planner 评估）**：**不入池**。理由：①语义已有注释定义（`DataPermissionServiceImpl:362-367`「管理员直接返回 null」=不限制），非业务/财务规则缺失——不触发 BLOCKED_PRODUCT_RULE 流程规则 1（产品未给结论的规则缺口）；②缺失的是调用方 `resolveStoreFilter` 对 null 的解析逻辑（代码 bug：null 分支处理缺失），按协议 §2「代码问题 → 开发 → 进入任务卡执行」归属开发；③修复方向明确（null → 不限制、不设 store_id 条件），无需产品定义新规则——与 PD-001~005（口径/规则未定义型）先例性质不同
- **处置更新（2026-08-10，QA p0-data-009-r1 复验新观察 2，并入本 KL 处置范围）**：`GET /v1/operations-reports/kpi-summary?startDate=...&endDate=...&storeIds=1`（admin 显式传门店参数）触发 **NPE**——`OperationsReportServiceImpl.java:72` `accessibleStoreIds.contains()` 对 null（getAccessibleStoreIds 返回 null=不限制）未做防御 → GlobalExceptionHandler 兜底 code=500 + traceId（QA 运行时实证 traceId=45c17b07c5ee，显式错误态无伪装）。与 admin 趋势空**同根因**（null=不限制 语义误判），修复时一并做 null 防御（null → 不限制、不设 store_id 条件 / 对 null 短路）；**状态维持「待架构裁决（数据权限语义）」**——发布前裁决，裁决后随后续批次修复，不阻断发布。
- **处置更新（2026-08-11，修复闭环收口 → 已关闭）**：**架构裁决需求消除**（语义未争议，按 `DataPermissionServiceImpl.java:362-367` 注释定义实现并通过复验）——修复=**3 文件 4 处 null 前置分支**（`LiveMonitorServiceImpl.java:283-287` / `DecisionBoardServiceImpl.java:355-359` / `OperationsReportServiceImpl.java:66-70`【NPE 防御】、`458-461`），非 null 分支零改动（`isEmpty()→-1` 最严格隔离 / `size==1→ID` / 多门店→null / 异常→-1 均保持）。QA 独立复验 `docs/quality/kl-048-r1-qa-report.md` = **PASS_WITH_LIMITATION（2026-08-11）**：验收目标 4/5 完全 PASS（①diff 范围 3 文件 4 处仅新增 null 前置分支 ②语义 null=不限制、仅空列表→-1 ③活体 4 接口 ④非 null 分支零回归）+ 编译项 KL-048 三文件本身 PASS（javac 全量错误列表不含三文件；限制=工作区 device 模块编译破坏，非 KL-048 引入，由 **P0-DEVICE-001（DEF-B）** 修复中）。活体证据：live-monitor/trend `revenue=[3000.0]` 非空（修复前空数组）、decision-board/revenue-trend 非空、kpi-summary `currentValue=3000.00` 与 DB SUM=300000 分精确一致、**kpi-summary?storeIds=1 → HTTP 200 code=0 无 500 无 NPE**（NPE 防御生效 + 过滤语义正确）。回归基线（`production-regression-test.md`）：**REG-DATA-016 唯一限制（KL-048）关闭 → 转 PASS**；新增基线 **REG-DATA-018**（运营趋势 admin 可见 + null 防御，PASS_WITH_LIMITATION）。**「发布前需架构裁决」需求消除，KL-048 满足关闭条件 → 状态 → 已关闭（2026-08-11）**。残留缺口（非本卡 FAIL，QA 缺口清单 §二 2/3/4）：非 admin 角色（店长/区域经理）路径与 storeIds 多值场景仅代码级核验（活体环境仅有 admin 凭据，随回归环境补验）；造数订单 DEV-L206-TEST-001 store_id=NULL 属造数数据质量问题（数据侧观察，建议造数/数据修复时补充门店归属）。

### KL-049｜充值统计字段恒 0（DEF-4：驼峰别名在 PostgreSQL 折叠为小写）
- **来源任务**：`docs/quality/p0-kl-020-rerun.md` 三章 **DEF-4**（KL-020 重跑活体实证，2026-08-10）；承接 **P0-DATA-009** 验收标准 1 非空充值场景缺口；关联 **KL-020**（关闭链联动）/ **P0-DATA-010**（建卡）/ P1-DATA-009（来源链）
- **风险等级**：高（发布阻断级——静默数据失真活跃缺陷：字段级恒 0 非 500，无报错无感知，与 DEF-2「静默伪装」同类危害；Release Gate FAIL 明细项）
- **是否阻断发布**：**是**（Release Gate 规则：FAIL > 0 禁止放行；DEF-4 修复 + QA/回归 -R 复验通过前，KL-020 不得全关、发布门禁保持 FAIL）
- **现象（活体实证，两次独立接口）**：
  - `GET /v1/members/stats/overview`：非空充值造数（recharge_records 本月成功 +100000 分）后 `rechargeThisMonth` 仍 `"0.00"`（DB 同口径=100000 分，应 +1000.00）；同响应其余 8 字段（totalMembers/newThisMonth/totalBalance/consumeThisMonth 等）造数前后与 DB 精确匹配
  - `GET /v1/recharge-stats/overview`（`frontend/src/views/marketing/RechargeManage.vue:201` 充值管理统计卡）：totalBalance / totalPrincipalBalance / totalBonusBalance / rechargeThisMonth / bonusThisMonth / refundThisMonth / rechargeCountThisMonth / rechargeCountToday / rechargeAmountToday / avgRechargeAmount **11 个统计字段全 0/0.00**；同响应 `weeklyTrend=[{amount:"1000.00", rechargedate:"2026-08-10", count:1}]` **金额正确** → 数据存在、查询路径不同，仅 `selectStatsOverview` 结果被错误消费
- **根因（代码 + 运行实证）**：`backend/src/main/resources/mapper/marketing/RechargeRecordMapper.xml:6-38` `selectStatsOverview`（resultType=java.util.Map）9 个别名驼峰未加引号 → **PostgreSQL 未加引号标识符折叠为小写**（psql 实证 `SELECT 1 AS rechargeThisMonth` → 列名 `rechargethismonth`）→ MyBatis resultType=Map 以 JDBC columnLabel 为 key → key 全小写；`map-underscore-to-camel-case` 仅转换下划线（无下划线不转换）→ key 保持全小写 → 消费方按驼峰 key 取值 null → 0：
  - `MarketingMemberServiceImpl.java:230-233`：`rechargeStats.get("rechargeThisMonth") instanceof Number` → null → 0L（overview.rechargeThisMonth 恒 0.00）
  - `RechargeStatsServiceImpl.java:39-54`：`toLong(stats.get("totalBalance"))` 等 **11 处** → 0（充值统计全 0）
- **暴露历史（为什么此前未发现）**：DEF-1 未修复时 overview 恒 500 → 无法比对；QA 复验（`p0-data-009-r1-qa-report.md`）时 recharge_records 表**空**（0 行）→ SUM=0 与 0.00 恰好一致未暴露；本次**非空充值造数首次暴露**——属**既有代码缺陷**（P1-DATA-009 修复/验收未覆盖非空充值场景，非本次修复引入）
- **影响面**：会员统计卡（overview rechargeThisMonth）+ 充值管理统计卡（RechargeManage.vue:201 11 字段）+ MemberList 充值卡（MemberList.vue:70，消费 overview 同源）——经营统计展示静默失真
- **处置**：**已建卡 P0-DATA-010**（发布阻断，状态 **DOING**，developer 修复中）；修复方向（供卡内参考）：① XML 别名加双引号（`AS "rechargeThisMonth"` 等，推荐，消费方驼峰 get 不变）② 消费方改小写 key 对齐（需同步 2 处消费方）——建议 ①+② 双保险，并覆盖 `selectPlanUsageDistribution` 同类驼峰别名（实测 `totalamount`/`planname` 小写 key 混入 VO 输出）；修复后 -R 复验须覆盖非空充值场景（验收标准见任务卡）。**不修改统计口径/查询条件/业务逻辑**；属代码缺陷（标识符引号），**不入 PD 池**（无业务规则缺口，修复方向明确，同 KL-048 判断逻辑）
- **处置更新（2026-08-10，DEF 修复批次收口）**：**已关闭**——DEF-4 经 **P0-DATA-010** 修复（采用方向① XML 别名加双引号：`RechargeRecordMapper.xml` 9 个别名 `AS "..."` + 全 mapper 同型扫描清 15 文件，diff 纯引号 157/157 零语义变更）→ QA 复验 `docs/quality/p0-data-010-r1-qa-report.md` = **PASS 7/7**：非空充值造数（`QA-D4-` 前缀 2 笔 125000 分）overview.rechargeThisMonth=1250.00 与 DB 精确一致、recharge-stats 11 字段全非 0 逐字段对账、planUsageDistribution 恢复驼峰 key（planName/totalAmount）、造数 0 残留、编译 EXIT=0；回归转基线 **REG-DATA-017**（`production-regression-test.md`）；与 KL-020 关闭链联动闭环（KL-020 → 已关闭）。

### KL-050｜设备域同型漂移组（connectionType / storeId 类型漂移，DEF-B 同族）
- **来源任务**：P0-DEVICE-001 QA 复验 `docs/quality/p0-device-001-r1-qa-report.md` §4「同型漂移记录」（2026-08-11，超本卡范围记录）；关联 P0-DEVICE-001（DEF-B 修复，本项为其同根因残留）
- **风险等级**：中（同 DEF-B 缺陷族——实体类型与 DB 列型不一致，非数值数据下运行时 500；当前数据均为数字字符串（'1'~'4'）映射正常，无活跃故障）
- **是否阻断发布**：**否**（记录存在性：当前无活跃故障、无发布阻断；非数值数据场景风险登记待治理）
- **现状**（活体 information_schema 实测）：
  - ① `Device.connectionType Integer` vs `devices.connection_type character varying(50)`——值约定为数字字符串（'1'~'4'，前端连接类型字典 1-4），当前映射正常；**非数值时仍会 500**（同 DEF-B 缺陷族）
  - ② `Device.storeId Long` vs `devices.store_id character varying(50)`——当前值数字字符串（'1'）映射正常；非数值时 500 风险
  - 对照：`devices.status` 实体 String+Integer 兼容转换（既有设计，非漂移）；`device_alerts.device_type int`、`print_tasks.device_type int` 与各自实体 Integer 一致（两表同名不同型为既有 schema 事实，DEF-B 修复未引入新漂移）
- **处置**：建议 audit 立项「设备域剩余同型漂移（connectionType/storeId 类型）统一治理」——修复方向同 DEF-B 先例（实体 String 化对齐 varchar(50) 或 DB 列型对齐实体，方案由 developer 执行、架构/audit 立项）；登记**后续批次**，不阻断当前发布。

### KL-051｜设备域迁移文件列名漂移（created_at/updated_at vs create_time/update_time，文件级）
- **来源任务**：P0-DEVICE-001 QA 复验 §4「同型漂移记录」（2026-08-11）；`backend/.../resources/db/migration/V1.0.0.100__init_postgresql.sql` L933-934
- **风险等级**：低（文件级漂移，无运行时影响——实体 `@TableField("create_time"/"update_time")` 与活体 DB 一致，仅迁移声明与两者不一致）
- **是否阻断发布**：否
- **现状**：迁移文件 `V1.0.0.100__init_postgresql.sql` L933-934 声明 **created_at/updated_at**；实体与活体 DB 均为 **create_time/update_time**（一致）——迁移文件与活体库/实体不一致
- **处置**：建议 audit 核对迁移文件基线并修正（迁移文件与实体/活体库三处对齐），登记后续批次，不阻断当前发布。

### KL-052｜KDS 设备类型展示归属缺口（观察项）
- **来源任务**：P0-DEVICE-001 QA 复验 `docs/quality/p0-device-001-r1-qa-report.md` §3 观察项（2026-08-11）
- **风险等级**：低（展示层降级——KDS 设备展示为「未知」，非数据失真、非本卡引入）
- **是否阻断发布**：否
- **现状**：任务卡与后端驱动/模拟器惯例含 `case "KDS"`（DeviceSimulator / DeviceConnectionServiceImpl / DevicePrintServiceImpl 实证），但 KDS 不在展示白名单（前端字典 `frontend/src/types/device.ts` L10-17、后端 getDeviceTypeName 映射 PRINTER/SCANNER/SCALE/LOCKER/OTHER）→ KDS 设备展示为「未知」
- **处置**：观察——展示字典范围（非规则缺失，**不进 PD 队列**，同 KL-022「产品功能范围」先例）；产品确认 KDS 是否纳入设备类型字典后实施（前端字典 + 后端名称映射同步补充）。

---

### KL-053｜财务凭证期间归属规则（跨月红冲/调整口径）

- **来源任务**：P0-FIN-001（Sprint-4 首批，`docs/quality/sprint-4-backend-qa-report.md` §五.2 developer 风险提示：`recalculateAllBalances` 以 voucherDate 所在月为期间，`finance_vouchers` 表无 period 列）
- **现状**：余额聚合按凭证日期（voucherDate）yyyy-MM 推导期间归属
- **风险**：跨月红冲、调整凭证的期间归属口径未定义——期初/期末余额可能归属错误期间（低-中，当前数据未暴露，属口径风险）
- **是否阻断发布**：否（当前聚合行为可运行，属口径待确认）
- **处置**：待财务规则确认（红冲凭证/调整凭证按原期间还是当前期间归属）；下一周期处置；Owner：财务规则确认
- **关联**：REG-FIN-006 / REG-FIN-001（余额聚合口径）

### KL-054｜销售订单实体-表漂移（sales_order 表不存在，DEF-3/KL-050/051 同族新实例）

- **来源任务**：OIC-1 observe 首轮（2026-08-14，观察项 **O-20260814-01**；完整堆栈与处置记录见 `oic-1-task-board.md` §8.6）
- **现状**：`SalesOrder.java:8` `@TableName("sales_order")` 映射的表在生产库**不存在**（活体库实际订单表为 `orders`——`OrderNew.java:17` `@TableName("orders")` 体系）→ `DashboardServiceImpl.java:95-100` getTodaySalesOverview 查询 sales_order 抛 `BadSqlGrammarException: relation "sales_order" does not exist` → catch 兜底记录 ERROR「获取今日销售概览失败」→ /home dashboard「今日销售概览」卡片降级为空
- **风险**：P1（后端域）；不阻断（catch 降级可运行，页面不崩，前端对 code=404 有兜底；无数据损坏/越权面）；08-11 观察期 ERROR=0 系无流量未触发，本次浏览器冒烟访问 /home 暴露
- **是否阻断发布**：否（OIC-1 前端批次不受影响；修复排后端整改池）
- **处置（2026-08-14 发布负责人指示）**：① audit 立项登记 KL（后端域 P1），修复排后端整改池，**不进 OIC-1**；② 立项执行**全仓实体-表映射扫描**：entity `@TableName` ↔ migration/schema ↔ 真实表，输出 mismatch 清单并分类 P0/P1/P2，只扫描不修改；③ 已有样本：DEF-3 / KL-050 / KL-051 / 本次 sales_order，按系统性模式治理
- **关联**：DEF-3（设备域漂移同族）/ KL-050 / KL-051 / DashboardServiceImpl / SalesOrder / OrderNew
- **状态登记（2026-08-15，D-2）**：并入 **Entity-Table Consistency Remediation 家族专项**（家族样本 DEF-3 / KL-050 / KL-051 / KL-054 / KL-055 / ETM-106），**家族专项统一治理中**——系统性专项治理、不全仓一次性改（分批迁移+验收）；执行窗口：管理端 Batch 2 observe 收口（2026-08-16 12:55）后开工，观察期内不动后端（详见下「实体-表漂移家族专项」汇总）

### KL-055｜budgets 表结构漂移（实体-表漂移家族扩展，O-20260815-02）

- **来源任务**：OIC-1 Batch 2 QA 复验观察项 **O-20260815-02**（`oic-1-task-board.md` §8.9，2026-08-15 qa 独立实测暴露）；auditor 复核并建议编号 KL-055（`docs/quality/frontend-experience-audit-draft.md` 文末，2026-08-15）
- **风险等级**：P1（后端域）
- **是否阻断发布**：否（OIC-1 前端批次不受影响——接口异常仅前端可见失败提示，前端逻辑正确；修复排后端整改池）
- **现状**：`budgets` 表为旧采购预算结构（`V20260704_001` 建表），而 `finance/Budget` 财务预算实体映射列漂移 → SQL 引用漂移列 → `BadSqlGrammarException` → `/v1/finance/budgets` 返回 **HTTP 200 + body code:500「系统繁忙」**；**无筛选查询被分页 COUNT 短路掩盖为 code:0 假空列表（假成功形态）**；12:55 观察窗口后零新增触发（`oic-1-task-board.md` §8.9 收口记录）
- **处置（2026-08-15 补登记）**：归后端整改池，**随 KL-054/ETM 同批窗口**（发布负责人指示已指定「与 ETM 同批」）；**家族专项统一治理中**（Entity-Table Consistency Remediation，执行窗口 2026-08-16 12:55 后开工）；含审计口径问题待评估：HTTP 200 携带 code:500 的「业务错误不落 HTTP 状态码」模式是否作为独立接口异常项登记（auditor 复核范围）

### ETM-106｜inventory 列级漂移（P0 后端专项，Entity-Table Consistency Remediation 家族成员）

- **来源任务**：全仓实体-表映射扫描（`docs/quality/entity-table-mapping-audit.md`，**C 类唯一 P0**，2026-08-14）+ 多客户端审计 RM-BE-001（`docs/quality/client-audit-receiving-mobile.md`，2026-08-15）
- **风险等级**：**P0（后端域）**——仓库手持收货确认**全线 500 事务回滚**（WAREHOUSE 分支写库存断流；门店 STORE 分支走 store_inventory 表已对齐，幸免）
- **是否阻断发布**：否（当前观察窗口冻结后端，无发布动作；修复排 RM-B1-001 专项卡，执行窗口 2026-08-16 12:55 后开工）
- **现状**：`Inventory.java` 实体 + `InventoryMapper.xml` 为**物料口径 20 列**（material_id/material_name/specification/quantity/batch_no/expiry_date/location_id/locked_quantity/max_stock_qty/min_safe_qty/production_date/total_cost/unit_cost 等），`inventory` 表为**产品口径 18 列**（product_id/product_name/current_stock/safety_stock/stock_value/warning_level 等），仅 `V20260730_001__add_chain_linkage_columns.sql` L48-54 补了 6 列（material_id/material_code/material_category_id/material_category_name/status/version）→ `material_name`/`specification`/`batch_no`/`locked_quantity`/`unit_cost`/`total_cost` 等**仍不存在** → `ReceiptConfirmationServiceImpl.java:706-716`（WAREHOUSE 分支）increaseInventory 全字段写表 → INSERT 必含 locked_quantity（恒 ZERO 非 null）+ batch_no（手持端必传）+ unit_cost + total_cost → 列不存在 → SQL 异常 → @Transactional 整体回滚 → **500**；`InventoryMapper.xml` L29 硬编码 `SELECT inventory_id, material_id, material_name, specification, unit... FROM inventory` → `column "material_name" does not exist` → 500；Inventory 被 17 个文件引用（InventoryController/InventoryServiceImpl/InventoryStatsServiceImpl 等核心库存链路）→ 核心库存查询/统计全链路风险
- **处置（2026-08-15，D-2）**：**提升 P0 后端专项**（Entity-Table Consistency Remediation）——**专项首卡 RM-B1-001**（`rm-receiving-task-board.md`）：窗口内（08-16 12:55 前）仅出 migration 方案（补列 vs 实体对齐三选一，数据迁移风险高，方案决策需 developer 确认口径 + 架构裁决，禁止 auditor/planner 决策）**不落库**；执行窗口 08-16 12:55 后开工；分批迁移+验收（不全仓一次性改）；验收含实体↔migration↔真实表三向核对（协议规则 6 实体-表一致性门禁）
- **关联**：RM-BE-001 / RM-B1-001 / ReceiptConfirmationServiceImpl / InventoryServiceImpl / Inventory.java / V1.0.0.100__init_postgresql.sql L425-444 / V20260730_001 L48-54

### 实体-表漂移家族专项（Entity-Table Consistency Remediation，2026-08-15 架构裁决 D-2 登记）

- **家族样本**：**DEF-3**（Notification 漂移，已闭环 P0-MOCK-001）/ **KL-050 / KL-051**（设备域漂移组）/ **KL-054**（sales_order 表缺失）/ **KL-055**（budgets 表结构漂移）/ **ETM-106**（inventory 列级漂移，P0 专项首卡）
- **治理方式**：系统性专项治理，**不全仓一次性改——分批迁移+验收**（每批 2~3 卡，D-4）；修复排**后端整改池**（不进 OIC-1/OIC-2/RM 前端批次）；执行窗口：**管理端 Batch 2 observe 收口（2026-08-16 12:55）后开工，观察期内不动后端**
- **状态字段标注**：家族成员统一标注 **「家族专项统一治理中」**（本表 KL-050/051/054/055、ETM-106 行状态）；ETM-106 沿用 ETM 审计编号（C 类唯一 P0，关联 RM-BE-001/RM-B1-001），不另设 KL 编号
- **关联登记**：`rm-receiving-task-board.md`（RM-B1-001 专项首卡）/ `product-decision-backlog.md`（PD-011~013 决策池）/ `remediation-roadmap.md` §5.5

## 3. 发布决策摘要| 场景 | 结论 |
|------|------|
| KL-001 + KL-002 | ✅ **阻断判定解除（2026-08-11）**——PD-001~004 决策到达 + 实现 QA 验收 PASS（原判定「未决策=阻断上线（资金真实性/合规无法收口）」的历史语义已消除，见下「Sprint-4 首批实现闭环」块） |
| KL-003 | ✅ **处置完成（2026-08-11）**——PD-004 下线决策执行（入口已移除、演示数据不再暴露）；原判定「未决策且保留入口=建议按阻断处理（生产展示虚构数据）」的历史语义已消除 |
| KL-004 已禁用入口 | 不阻断，功能暂不可用 |
| KL-005 | 不阻断，属治理项 |
| KL-006 | 不阻断，低敏感治理项（8 角色可达低敏提报页） |
| KL-007 | ✅ **已关闭（2026-08-10）**——P1-SEC-009 渗透验证 PASS（活体 HTTP 实测：篡改 token → 401×3、未知角色/无权限码 → 403×3、过期 → 401×2，14 用例全 PASS）；渗透门禁前置项关闭，不再要求"发布前必须完成" |
| KL-008 | 不阻断（当前无活跃漂移）；已拆卡 P2-SEC-007 治理 |
| KL-009 | 不阻断；联调造数确认后收口 P1-SEC-003 |
| KL-010 | 不阻断（老问题）；已拆卡 P2-SEC-008，完整流程需产品决策 |
| KL-011 | 不阻断，仅观察 |
| KL-012 | 不阻断（权限过严非越权）；随 P0-SEC-001 统一 |
| KL-013 | ✅ **已关闭（2026-08-11）**——浏览器冒烟实测 PASS（PROD preview 5 项：登录渲染 / 登录+路由跳转 / 授权页可达 / 权限按钮渲染 / 401 弹框→登出），浏览器缺口全部关闭，P0-SEC-006 L-5 收口 |
| KL-014 | 不阻断（运行期无权限差异，维护性问题）；随 P0-SEC-001 统一 4 文件清单 |
| KL-015 | 不阻断，仅记录（良性超范围，与页面使用点一致） |
| KL-016 | 不阻断（P0-SEC-005 已 PASS）；随 P0-SEC-001 统一前后端权限码（标注关联） |
| KL-017 | 不阻断（有条件：残留假历史已挂 P1-MOCK-004，3B-2 执行中；3B-2 完成前发布需评估） |
| KL-018 | 不阻断（0 值已注释说明，字典缺失已如实登记、未假造接口）；随后端排期 |
| KL-019 | 不阻断（3 字段已降级移除，无假数据）；字段恢复依赖 PD-005 口径决策，恢复前禁止猜测实现 |
| KL-020 | ✅ **已关闭（2026-08-10）**——活跃缺陷 DEF-1/DEF-2/DEF-4 全部修复并经 QA 复验 + regression 重跑闭环：DEF-1/DEF-2（P0-DATA-009，QA 复验 p0-data-009-r1 = PASS_WITH_LIMITATION + 重跑 p0-kl-020-rerun 活体闭环）；DEF-4（P0-DATA-010，QA 复验 p0-data-010-r1 = PASS 7/7 + 转基线 REG-DATA-017）；V-2 维持代码级 PASS，残留浏览器切换交互缺口（KL-020-V2）登记发布前浏览器冒烟补验；**2026-08-11 冒烟补充：V-2 切换交互缺口实测关闭（PASS）——KL-020 关闭链完整** |
| KL-021 | 不阻断（仅展示层 mock，不影响提交数据）；登记治理 |
| KL-022 | 不阻断（机制已就绪，UI 属产品功能范围）；待产品排期 |
| KL-023 | 不阻断，工程流程项（建议后续按模块分批 commit） |
| KL-024 | 不阻断（有条件：假数据可见，已拆卡 P1-DATA-011，建议 3B 收口前完成） |
| KL-025 | 不阻断（挂 P1-API-001 既有范畴，已验收含限制） |
| KL-026 | 不阻断（挂 P1-API-003 既有范畴，随后端排期） |
| KL-027 | 不阻断（随 P0-DATA-008 独立收尾批次） |
| KL-028 | 不阻断（空展示非假数据，既有降级）；仅观察 |
| KL-029 | 不阻断（代码侧已 PASS，验证缺口）；联调随回归阶段闭环（与 KL-009/KL-013/KL-020 同批），发布前收口 |
| KL-030 | **部分关闭（2026-08-11，浏览器冒烟）**——OrderQuery 导出实测 PASS（BOM/表头 13 列/真实数据行解析闭环）；折旧/告警两页抽样被**活跃缺陷 DEF-A/DEF-B 阻断**（非验证缺口）→ **已建卡 P0-ASSET-001 / P0-DEVICE-001（发布阻断，DOING）**；修复 + QA/回归 -R 复验通过前**不关闭**、冒烟门禁 **FAIL（禁止放行）**；修复后重跑折旧/告警导出闭环 |
| KL-031 | ✅ **已关闭（2026-08-10）**——DEF-3（实体-表漂移 → GET 通知列表恒 500）经 P0-MOCK-001 修复（migration V20260810_001 补列，QA 复验 p0-mock-001-r1 = PASS）+ 回归重跑（p0-kl-031-rerun）三端点真实登录态活体闭环；残留 R1 浏览器通知面板 UI 交互登记发布前浏览器冒烟补验（验证缺口，非 FAIL）；**2026-08-11 冒烟补充：R1 通知面板 UI 交互实测关闭（PASS，5 项全过）——KL-031 关闭链完整** |
| KL-032 | 不阻断（4 写操作已禁用 + el-alert）；招聘域接口随后端排期（HR 域实体复用见 KL-038） |
| KL-033 | 不阻断（计划 Tab 已禁用 + 标注）；盘点计划接口随后端排期 |
| KL-034 | 不阻断（升级已禁用 + warning）；severity 升级端点随后端排期 |
| KL-035 | 不阻断（生成预警任务已禁用 + warning）；任务创建端点随后端排期；注释口径待修正（不拆卡） |
| KL-036 | 不阻断（建议记录 Tab 空态 + el-alert，L-1/KL-017 已闭环）；查询接口随后端排期 |
| KL-037 | 不阻断（导入已禁用 + tooltip）；质检批量导入端点随后端排期 |
| KL-038 | 不阻断（本轮未接线，处置合规）；实体复用属产品/架构裁决，不进 PD 队列 |
| KL-039 | 不阻断（有条件：报名员工身份伪造值，已拆卡 P1-DATA-012，建议 3B-3 收口） |
| KL-040 | 不阻断（低风险 UI 行为变更）；行为回归放回归阶段（关联 KL-023） |
| KL-041 | 不阻断（本地提示非假成功文案）；消息域接线随后端排期 |
| KL-042 | 不阻断（挂 P1-API-001 既有范畴，随收尾批次评估） |
| KL-043 | 不阻断（35 端点均为既有 permitAll 设计公开端点，2026-08-10 补录 /v1/mp/**）；豁免清单需文档化供扫描脚本 `--strict` 参考，P0-SEC-001 整卡 100% 达成口径须注明排除项（35 设计豁免 + 36 BLOCKED（PD-006~008）+ 17 范围外（KL-044~046）） |
| KL-044 | 不阻断（整卡排除项，FAIL=0）；收口路径=PD-009（产品+架构确认跨域通用审批权限模型），决策前不猜测权限码/角色 |
| KL-045 | 不阻断（库存域归属明确，修复路径清晰）；已拆卡 P2-SEC-010，建议随下一周期（P2-SEC-009 同批窗口）执行 |
| KL-046 | 不阻断（既有 permitAll 设计豁免，加注解反而破坏小程序免登流）；已补录 KL-043 豁免清单（34→35），待架构核对确认 |
| KL-047 | 不阻断（授权仍受 @PreAuthorize('order:manage') 管控，QA O-1）；审批归属规则待产品决策（PD-010），决策前禁止猜测实现 |
| KL-048 | ✅ **已关闭（2026-08-11）**——修复闭环：3 文件 4 处 null 前置分支（LiveMonitorServiceImpl:283-287 / DecisionBoardServiceImpl:355-359 / OperationsReportServiceImpl:66-70【NPE 防御】、458-461），非 null 分支零改动（isEmpty→-1 最严格隔离 / size==1→ID / 多→null / 异常→-1 均保持）；QA 复验 `docs/quality/kl-048-r1-qa-report.md` = **PASS_WITH_LIMITATION**（4/5 PASS + 编译项 KL-048 三文件 PASS；限制=工作区 device 模块编译破坏非本项引入，P0-DEVICE-001 承接）；活体：live-monitor/trend revenue=[3000.0] 非空、decision-board/revenue-trend 非空、kpi-summary currentValue=3000.00 与 DB SUM=300000 分精确一致、**kpi-summary?storeIds=1 → 200 code=0 无 500 无 NPE**；回归转基线 **REG-DATA-018**（REG-DATA-016 唯一限制关闭转 PASS）；**「发布前需架构裁决」需求消除**（修复前判定=不阻断维持不变） |
| KL-049 | ✅ **已关闭（2026-08-10）**——DEF-4（充值统计字段恒 0，静默数据失真）经 P0-DATA-010 修复（XML 别名加引号 + 同型 15 mapper 扫清）→ QA 复验 p0-data-010-r1 = PASS 7/7（非空充值造数 11 字段全非 0 与 DB 对账一致）→ 回归转基线 REG-DATA-017；与 KL-020 关闭链联动闭环 |
| KL-050 | 不阻断（记录存在性：当前设备数据均为数字字符串、映射正常，无活跃故障）——设备域同型漂移（connectionType Integer vs varchar(50)、storeId Long vs varchar(50)，DEF-B 同族，非数值数据下 500 风险）；建议 audit 立项统一治理，后续批次执行 |
| KL-051 | 不阻断（文件级漂移，无运行时影响）——迁移文件 `V1.0.0.100__init_postgresql.sql` L933-934 created_at/updated_at vs 实体/活体库 create_time/update_time；建议 audit 核对迁移文件基线并修正，后续批次执行 |
| KL-052 | 不阻断（展示层降级，非本卡引入）——KDS 设备类型不在展示白名单 → 展示为「未知」；展示字典范围（非规则缺失，不进 PD 队列），产品排期确认 KDS 是否纳入字典 |
| KL-053 | 不阻断（口径风险：跨月红冲/调整凭证期间归属待财务确认；当前按 voucherDate yyyy-MM 聚合可运行）——下一周期处置，Owner：财务规则确认 |

**Sprint-4 首批实现闭环（2026-08-11，planner 登记，来源 `docs/quality/sprint-4-backend-qa-report.md` + `docs/quality/sprint-4-frontend-qa-report.md`）**
- 结论：**4 项 QA 验收 PASS，FAIL=0（无 -R 生成）**——P0-FIN-002 缴税幂等（PD-001）/ P0-HR-002 报名唯一性（PD-002）/ P0-FIN-001 期初录入+审计（PD-003）经 QA 活体验收 **PASS**（sprint-4-backend 报告：同键重复→DB 仅 1 条+返回同 id+幂等命中日志+唯一索引兜底 / 重复报名→「已报名」拒绝+DB 无第二条 / 期初授权录入+审计可查+新行期初 0+既有行零覆盖+403 实测）；P0-HR-005 HR 智能 3 页面下线（PD-004）经 QA 只读核验 **PASS 8/8**（sprint-4-frontend 报告：3 路由/3 菜单删除、0 入口、URL 直达不渲染、构建 EXIT=0、dist 无 chunk）；**P1-MOCK-005 联动 PASS**（回归基线候选）。
- **KL 状态（只增不改，历史行保留）**：**KL-001 / KL-002 → 阻断判定解除（2026-08-11）**（决策到达 + 实现 QA PASS）；**KL-003 → 处置完成（2026-08-11）**（PD-004 下线执行，入口移除、演示数据不再暴露）。
- **PWL 销号（architect 已执行，planner 确认无遗漏）**：REG-RULE-001 / REG-RULE-002 / REG-RULE-003 / REG-FIN-001 / REG-HR-002 / REG-HR-004 **已销号（2026-08-11，决策到达）**——`production-pwl-closeout.md` 当前口径：已销号 9 / 待关闭 17。
- **红线确认**：4 项均按 PD-001~004 决策结论实现，**无 AI 自补业务规则**（QA BLOCKED 审查通过：无自行编造幂等/唯一性/结转逻辑、无自补页面处置）；无新增 BLOCKED_PRODUCT_RULE。
- **Production Release Gate WAITING 剩余条件（2026-08-11 更新）**：**待 regression 转基线**（4 项 PASS 结论已同步任务池，P0-FIN-002/P0-HR-002/P0-FIN-001/P0-HR-005/P1-MOCK-005 均为回归基线候选）+ **冒烟门禁 regression 复评**（KL-030 折旧/告警两页导出重跑闭环）——PD-001~004 决策、KL-048、DEF-A/B 均已从 WAITING 条件中消除；regression 转基线 + 复评通过后**放行上线**。
- 任务池同步（`production-remediation-task-board.md` §0 + §0.1 + 相关任务卡）：P0-FIN-002 / P0-HR-002 / P0-FIN-001 / P0-HR-005 / P1-MOCK-005 → **✅ PASS**。

**DEF-A/B 复验通过（2026-08-11，planner 登记，来源 QA 复验 `docs/quality/p0-asset-001-r1-qa-report.md` + `docs/quality/p0-device-001-r1-qa-report.md`）**
- 结论：**两卡 PASS**——**P0-ASSET-001 ✅ PASS**（QA 复验 p0-asset-001-r1，5/5 实证：基线 dd33ee3 git worktree vue-tsc 复现 TS2304「Cannot find name 'InvStatus'」→ 工作区 0 错误；git diff 仅 inventory.ts +1 行 `*/`（1 file, 1 insertion(+)，0 deletion）；业务语义零变更；npm run build EXIT=0；无调试残留/无夹带）；**P0-DEVICE-001 ✅ PASS**（QA 复验 p0-device-001-r1，6/6 达成含**活体实测**：造数 QA_P0DEV001/002 后 /v1/devices/page、/v1/devices/list、/v1/device-alerts/page 全 200 code=0、deviceType 字符串往返与 DB 直查一致、空表不回归、修改范围仅设备域 10 文件、mvn 编译 EXIT=0）。**DEF-A/DEF-B 修复闭环**——两卡已从 Production Release Gate WAITING 条件中消除。
- **冒烟门禁复评以 regression 输出为准**：折旧/告警两页导出浏览器重跑（冒烟报告 §四 补验项 1/2/3，含资产域其余页冒烟）由 regression 执行，重跑通过后 **KL-030 方可关闭**（当前仍为部分关闭，不阻断口径维持=活跃缺陷已全部修复并复验关闭，剩余为浏览器级验证闭环项）。
- **Production Release Gate WAITING 剩余条件（2026-08-11 更新）**：**PD-001/003 产品决策（及 002/004）** + **冒烟门禁 regression 复评（KL-030 折旧/告警导出重跑闭环）**——KL-048、DEF-A/B 均已从 WAITING 条件消除。
- **新登记**：设备域同型漂移 3 处（connectionType / storeId 类型、迁移文件列名）→ **KL-050 / KL-051**（建议 audit 立项统一治理，不阻断）；KDS 展示观察 → **KL-052**（产品排期，不进 PD 队列）。
- 红线确认：两修复均为代码缺陷修复（前端注释闭合 / 后端实体-表对齐），未引入猜测性业务规则，**无新增 BLOCKED_PRODUCT_RULE**（deviceType 白名单源自前端既有字典与后端驱动惯例，developer 仅对齐契约）。
- 任务池同步（`production-remediation-task-board.md` §7.9 + §0 + §0.1）：P0-ASSET-001 / P0-DEVICE-001 → **✅ PASS**；WAITING 条件行同步更新；数量统计维持 P0=25 / 合计 77。

**浏览器冒烟（2026-08-11，planner 登记，来源 `docs/quality/browser-smoke-sprint3.md`，regression 会话6 实测）**
- 结论：**3/4 PASS**——**KL-013**（登录渲染 / 401 弹框→登出 PROD 分支 / 路由跳转 / 权限按钮渲染）✅ **已关闭**；**KL-020-V2**（门店下拉切换重载交互）✅ 实测关闭（KL-020 关闭链完整）；**KL-031-R1**（通知面板 UI：渲染/徽标/单条+全部已读/DB 落库）✅ 实测关闭（KL-031 关闭链完整）；**KL-030 部分关闭**（OrderQuery 导出 PASS；折旧/告警两页抽样被 2 项新活跃缺陷阻断）。
- **新活跃缺陷建卡（发布阻断，状态 DOING）**：**DEF-A** → **P0-ASSET-001**（`frontend/src/api/asset/inventory.ts` L13-18 JSDoc 注释未闭合 `/*`21 vs `*/`20，**基线 commit dd33ee3 即存在** → import 被吞 → `InvStatus is not defined` → 资产模块全部页面 dev+PROD 双环境白屏，冒烟实测 3 页全崩）；**DEF-B** → **P0-DEVICE-001**（`Device.java` device_type Integer vs `devices.device_type` varchar(50) 实体-表漂移，DEF-3 同族 → 设备存在时 `/v1/device-alerts/page`、`/v1/devices/page`、`/v1/devices/list` 恒 500，此前表 0 行未暴露）。
- **冒烟门禁 FAIL（修复前禁止放行）**：Release Gate 规则 FAIL > 0 禁止放行；两项缺陷修复 + QA/回归 -R 复验通过前，**KL-030 不关闭、禁止放行**；修复后重跑折旧/告警两页导出（冒烟报告 §四 补验项 1/2/3：折旧 CSV 9 列 fenToYuan 200.00 断言、告警 CSV 9 列告警内容断言、资产域其余页冒烟）闭环 KL-030。
- 红线确认：两项均为代码缺陷（前端注释语法 / 后端实体-表漂移），未引入猜测性业务规则，**无新增 BLOCKED_PRODUCT_RULE**。
- 关联登记：KL-013/020/031 行状态已更新（只增不改原则，历史行保留）；任务池 §7.9 新增 2 卡、§0 总览与数量统计同步（P0=25 / 合计 77）；发布首日补验项见冒烟报告 §四（含 stores_new 历史 e2e 残留门店 `DeepStore-e2e-deep-msb9dm2j` 数据治理建议）。

**KL-048 修复闭环（2026-08-11，planner 登记，来源 QA 复验 `docs/quality/kl-048-r1-qa-report.md` + 回归基线更新 `production-regression-test.md`）**
- 结论：**KL-048 → 已关闭（2026-08-11）**——修复闭环（3 文件 4 处 null 前置分支 + NPE 防御；QA 复验 kl-048-r1 = **PASS_WITH_LIMITATION**，4/5 PASS + 编译项三文件 PASS；活体 4 接口全 PASS：trend 非空 / kpi-summary 3000.00 与 DB 精确一致 / storeIds=1 无 500 无 NPE）；回归侧 **REG-DATA-016 唯一限制（KL-048）关闭 → 转 PASS**、新增基线 **REG-DATA-018**（运营趋势 admin 可见 + null 防御）。**「发布前需架构裁决（数据权限语义）」需求消除**（语义未争议、按 `DataPermissionServiceImpl.java:362-367` 注释定义实现并通过复验，未引入猜测性业务规则、无新增 BLOCKED_PRODUCT_RULE）。
- **Production Release Gate WAITING 条件更新（2026-08-11）**：剩余条件 = **PD-001/003 产品决策（及 002/004）** + **DEF-A/B（P0-ASSET-001 / P0-DEVICE-001）修复闭环**——KL-048 项已从 WAITING 条件中消除；DEF-A/B 修复证据已提交（2026-08-11：inventory.ts 注释闭合 / deviceType String 化 11 文件 + 前端契约），qa 复验进行中，复验通过前冒烟门禁 **FAIL（禁止放行）**、KL-030 不关闭。
- 任务池同步（`production-remediation-task-board.md` §7.9）：P0-ASSET-001 / P0-DEVICE-001 状态 → **待 QA 复验**（developer 证据已提交）；§0 总览两行同步。

**16 项非阻断治理移出发布链（2026-08-10，planner 登记，来源 `docs/quality/final-release-gate-sprint3.md` §3.1 最终 PWL 快照）**
- **结论**：**逐项状态保持（不关闭、不销号）**——17 行（快照口径「不阻断治理 16 项」，逐项清单实列 17 行，计数差异提请 architect 核对）均为「待关闭」PWL 且发布判定=不阻断，验收结论（PASS / PASS_WITH_LIMITATION）维持不变；**移出当前发布链**，不构成 Production Release Gate 条件（门禁 WAITING 仅由 KL-048 修复闭环 + PD-001/003 产品决策组成）；**发布后按批次执行**。
- **批次归属**（与任务池 §8 登记表一致）：
  1. **后端排期 9**：REG-HR-001（导出端点）/ REG-HR-006（到期合同）/ REG-HR-009（档案接口）/ REG-HR-011（排班接口）/ REG-DATA-008（会签字段）/ REG-DATA-012（AI 字典/统计）/ REG-MOCK-001（招聘后端）/ REG-MOCK-003（建议记录接口）/ REG-EXPORT-002（质检导入）；
  2. **产品决策 4**：REG-HR-005（PD-004 演示 mock）/ REG-DATA-013（PD-005 口径）/ REG-DATA-014（组织切换 UI，功能排期）/ REG-SEC-010（PD-006~009 排除项闭环）；
  3. **下一周期治理 2**：REG-SEC-002（KL-006 路由精确化）/ REG-SEC-007（P2-SEC-007/009 已拆卡）；
  4. **联调补验 2**：REG-SEC-004（KL-013 浏览器 UI，发布首日冒烟窗口）/ REG-SEC-009（P0-SEC-002 运行实测）。
- **KL 状态列不改写**：本登记不改动 KL-003/004/005/006/008/009/013/014/018/019/021/022/032/036/037/038/039/043/044/045/046 等行「状态」列（维持既有「待后端排期 / 待产品决策 / 待治理 / 待联调 / 观察 / 已拆卡」），批次归属以任务池 §8 为准；本文件仅追加批次归属登记，不改变任何 KL 的发布判定。

**DEF 修复批次收口（2026-08-10，planner 登记）**
- 结论：**4 项发布阻断缺陷（DEF-1/DEF-2/DEF-3/DEF-4）全部关闭**；Release Gate 复评 **PASS**（`production-regression-test.md` 门禁表「DEF 修复批次」行：独立复验 3/3 通过、此前 FAIL 明细全部消除；最终以 regression 输出为准）。
- 明细：DEF-1/DEF-2 → **P0-DATA-009** ✅ PASS_WITH_LIMITATION（转基线 REG-DATA-016）；DEF-3 → **P0-MOCK-001** ✅ PASS（转基线 REG-MOCK-006）；DEF-4 → **P0-DATA-010** ✅ PASS（转基线 REG-DATA-017，承接 P0-DATA-009 验收标准 1 非空充值缺口销号）。
- KL 状态：**KL-020 / KL-031 / KL-049 → 已关闭**；**KL-048 维持待架构裁决**（admin 趋势空 + kpi-summary?storeIds=1 NPE null 防御，发布前裁决）。
- **发布前收口待办（验证缺口类，不构成 FAIL）**：
  1. **KL-048 架构裁决**：数据权限语义（null=不限制 vs 无权限），裁决后随后续批次修复（不入 PD 池，修复方向明确）。
  2. **发布前浏览器冒烟**（无浏览器自动化环境，接口/代码层已闭环）：KL-013（登录→/me→路由→按钮渲染浏览器表现）、KL-030（导出 CSV 浏览器下载内容解析）、KL-020-V2（org-context 切换下拉刷新交互）、KL-031-R1（TopNavbar 通知面板 UI 交互）。
- 红线确认：DEF 修复均为代码缺陷修复（SQL 反转义 / 迁移补列 / 别名引号），未引入任何猜测性业务规则，**无新增 BLOCKED_PRODUCT_RULE**。

**P1-SEC-009 渗透验证收口评估（2026-08-10，`docs/quality/p1-sec-009-qa-report.md`）**
- 结论：**P1-SEC-009 = PASS**——5 项验证目标全通过（①未知角色 fail-closed 前端 [] + 后端 403；②篡改 localStorage token 声明 → 权限与 /me 一致 + 后端签名校验拒绝（活体 HTTP 401）；③token 失效/过期 → 401 处理无死会话、自动登出；④/me 异常 → 登出不降级放行；⑤越权 URL 直达 → 前端 403 + 后端拦截）。证据：活体渗透 14 用例全 PASS + 前端仿真 26/26 + grep 代码级核验；无 FAIL，无需 `-R{n}`。
- **渗透门禁前置项已关闭**：KL-007（P0-SEC-006 L-2，验收标准 4「伪造 token 被后端拒绝」）**活体实测通过** → 状态置**已关闭**（2026-08-10）。权限源相关门禁可放行。
- **发布前收口待办（不随本卡关闭，归 KL-013/KL-020 联调批次）**：
  - KL-013：PROD 构建下 401 弹框→自动登出浏览器 UI 行为（依赖 `import.meta.env.PROD` 分支）——需回归阶段联调实测（与 KL-009 同批）。
  - KL-020：/me 真实 HTTP 500/超时活体复现——需回归阶段联调批次补验。
  - 上述均为**验证缺口而非代码缺陷**（代码路径已核验 + 仿真通过），不新增阻断项。
- 观察项：O-1（退款审批 approveUserId 信任客户端参数）→ **KL-047 + PD-010**（审批归属业务规则，待产品决策，不阻断发布）；O-2（401 文案「Token已过期」无法区分无效/过期）仅记录低风险观察，不拆卡。
- 发布环境红线（既有机制）：生产 profile 下 `JwtConfig.validateSecretKey()` 对默认/开发密钥拒绝启动——发布部署必须注入强随机 `JWT_SECRET`（本卡活体验证基于开发密钥，生产语义等价）。

**Sprint-3A 第一阶段 QA 复审后评估（2026-08-10，`docs/quality/sprint-3a-security-qa-report.md`）**
- 复审结论：**PASS 5 / PASS_WITH_LIMITATION 3 / FAIL 0 / BLOCKED 0** —— **FAIL=0，无阻断项，无需生成 -R 复验任务**。
- 3 项 PASS_WITH_LIMITATION 明细：
  1. **P0-SEC-004**（L-1 保持）：/workspace/material-request 域级并集超放行（8 角色 URL 直达，低敏表单页，不阻断）→ KL-006。
  2. **P0-SEC-006**（L-2 保持 + **L-5 新增**）：渗透用例未执行（KL-007）+ 真实登录联调未在运行环境执行（KL-013）——均属验证缺口类，代码侧已 PASS，回归阶段收口。
  3. **P1-SEC-003**（L-3/L-4 保持 + **L-6 新增**）：静态校验脚本缺失（KL-008，已拆 P2-SEC-007）+ role_permission 数据待联调（KL-009）+ 后端 4 文件清单不一致（KL-014，随 P0-SEC-001 统一）。
- 观察项 O-4（KL-015）记录不拆卡；O-6（KL-016）挂 P0-SEC-001 标注关联；均不改变第一阶段验收结论（8/8 通过）。

**Sprint-3B-1 QA 验收后评估（2026-08-10，`docs/quality/sprint-3b1-qa-report.md`）**
- 验收结论：**PASS 4 / PASS_WITH_LIMITATION 5 / FAIL 0 / BLOCKED 0** —— **FAIL=0，无阻断项，不生成 -R 复验任务**；9 卡全部通过（含限制）。
- 5 项 PASS_WITH_LIMITATION 明细：
  1. **P1-DATA-002**（L-1）：SmartRestock「建议记录」Tab 5 条硬编码假历史仍可见——本批次唯一假数据展示残留，**已确认并入 P1-MOCK-004（3B-2 执行中）承接清理** → KL-017。
  2. **P1-DATA-003**（L-2）：会签状态展示本地 mock（仅展示层）→ KL-021。
  3. **P1-DATA-008**（L-3）：AI 服务商字典/统计接口后端缺失（0 值展示已注释）→ KL-018，待后端排期。
  4. **P1-DATA-009**（L-4）：3 统计字段口径缺失 → 已按任务卡授权临时移除 + `--` 降级（无假数据），**新增决策登记 PD-005**（口径=计算规则，产品未定义前禁止恢复实现）→ KL-019。
  5. **P1-DATA-010**（L-5）：无组织切换 UI（机制已就绪，功能范围问题）→ KL-022。
- 验证缺口 V-1/V-2（KL-020）：DATA-009 聚合正确性、DATA-010 切换刷新未做运行期/浏览器验证——回归阶段闭环（与 KL-009/KL-013 联调待办合并执行），**发布前收口**。
- 观察项：O-1 归 KL-005 治理范围（硬编码业务默认，关联 P2-DATA-004）；O-2 **拆卡 P1-DATA-011**（InventoryOutbound 假供应商，KL-024）；O-3/O-4/O-5/O-6 仅登记（KL-025~028，挂既有批次）。
- **PD-005 特别说明**：与 PD-001~004 不同，**不阻断当前发布**（字段已移除，页面无假数据）；登记目的是约束字段恢复必须等待产品/财务口径决策（BLOCKED_PRODUCT_RULE 精神），防止后续开发自行补口径。

**Sprint-3B-2 QA 验收后评估（2026-08-10，`docs/quality/sprint-3b2-qa-report.md`）**
- 验收结论：**PASS 2 / PASS_WITH_LIMITATION 3 / FAIL 1 → P1-MOCK-010-R1 复验 PASS** —— **FAIL 0（R1 闭环），无阻断项**；6 卡全部通过（含限制）。
- 3 项 PASS_WITH_LIMITATION 明细：
  1. **P1-MOCK-001**（4 写操作禁用 + approvals 真实列表）：招聘域后端缺失（岗位 CRUD + approvals 占位）→ KL-032；实体复用裁决 → KL-038；注释表述过宽（O-1）。
  2. **P1-MOCK-004**（真实 POST /v1/purchase/requests + L-1 承接闭环）：采购申请运行期联调缺口（V-1）→ KL-029；建议记录查询接口待后端 → KL-036。**KL-017 状态更新为已解除**（5 条硬编码假历史删除，空态 + el-alert）。
  3. **P1-EXPORT-002**（10 页真实 CSV + HRTraining 导入/报名真实化）：CSV 内容运行期验证缺口（V-2）→ KL-030；质检批量导入端点待后端 → KL-037；报名员工硬编码（O-4）→ KL-039 + 拆卡 P1-DATA-012。
- **R1 复验闭环（P1-MOCK-010）**：首验 FAIL（批次新引入缺陷：`StoreCertificate.vue:1358` `append-to-body` 误改为无值绑定 `:append-to-body`，vue-tsc TS2339 新增报错 + 弹窗 teleport 行为丢失）→ 修复后 R1 复验 **PASS**（该行恢复纯布尔、0 报错、基线对照无夹带）——整卡 6 子项合并转 PASS；通知已读联调缺口（V-3）→ KL-031（不因 PASS 消除）。
- 验证缺口 V-1~V-3（KL-029~031）：采购申请落库/采购侧可见、CSV 下载内容解析、通知已读真实登录态验证——**回归阶段闭环（与 KL-009/KL-013/KL-020 联调待办合并执行），发布前收口**。
- 观察项：O-1 → KL-038（产品/架构裁决）；O-2 → 并入 KL-032；O-3 → 并入 KL-035（注释口径）；O-4 → KL-039 + 拆卡 P1-DATA-012；O-5 → KL-040（teleported/lock-scroll 超范围，行为回归放回归阶段）；O-6 → KL-041（模拟消息侧写）；O-7 → KL-042（挂 P1-API-001）；O-8 为 QA 抽样核验结论，无限制不登记。
- **P1-DATA-011（InventoryOutbound 假供应商，3B-1 拆卡）**：QA 确认**未被 3B-2 本批 20 文件修改**，仍待独立卡执行（KL-024）——排期建议更新为**并入 3B-3 窗口**。
- 假成功清零确认：本批 20 文件/6 卡范围内无确认框后无 API 直接成功、无 setTimeout 假成功、无占位 Blob、无假成功文案残留；全部「后端缺失」处置符合「禁用入口 + 明确标注 + 失败透传」；未发现 AI 自行补充业务规则（BLOCKED 审查通过，不新增 BLOCKED_PRODUCT_RULE 项）。

**P0-SEC-001 子批推进评估（2026-08-10，任务池 §7.5 第二阶段）**
- 子批状态：A ✅ QA PASS_WITH_LIMITATION（`docs/quality/p0-sec-001a-qa-report.md`）；**C/D ✅ 已提交（developer 证据已落任务池），待 QA 验收**；B 🔄 执行中（HR 域）。
- **BLOCKED_PRODUCT_RULE 候选（30 方法 + 6 端点，已登记决策池 PD-006~008，不纳入通过数口径）**：
  1. **PD-006**（合并）：SelfPurchase 6（C 子批）/ Task 12（D）/ PlanItem 7（D）——无前端消费裸接口权限归属（操作角色 + 权限码定义）待产品。
  2. **PD-007**（独立）：Appeal 5（D）——域归属（疑为 HR，涉子批 B 边界）+ 权限归属待产品。
  3. **PD-008**（独立，架构级）：SupplierPortal H5 6 端点（D）——外部签署流认证方案（token 即凭证、无用户会话，不能加角色注解）待产品+架构。
- **覆盖率口径**：整卡 P0-SEC-001「覆盖率 100%」达成需在验收口径注明排除项——**34 设计豁免端点（KL-043）+ 30 BLOCKED 候选（PD-006~008）+ 6 H5 边界端点**；排除项未闭环（豁免清单文档化 / 产品决策 / 架构方案）前不得宣布整卡 100% 达成。
- 关联：KL-012/014/016 随本卡统一（既有登记，不重复）；BLOCKED 候选不阻断当前子批 C/D 验收（不纳入通过数），但「100% 覆盖率」口径依赖上述排除项闭环。

**P0-SEC-001 整卡收口评估（2026-08-10，架构宣布整卡 PASS_WITH_LIMITATION；任务池 §7.5 状态表 + §7.5.2 收口登记）**
- 整卡结论：**PASS_WITH_LIMITATION（A/B/C/D 子批全部 QA 通过，FAIL=0）**——不阻断发布，无 -R 复验；排除项闭环前不做「覆盖率 100%」达成宣布。
- **排除项闭环路径**（整卡 100% 达成口径 = 覆盖 1991/2044（97.4%）+ 35 设计豁免（KL-043，2026-08-10 补录 /v1/mp/**）+ 36 BLOCKED（PD-006~008）+ 17 范围外（KL-044~046））：
  1. **35 设计豁免（KL-043）**：豁免清单文档化 + 扫描脚本 PERMIT_ALL_METHODS 对齐——随 P2-SEC-007 静态校验治理同批；/v1/mp/** 补录待架构核对确认（KL-046）。
  2. **36 BLOCKED（PD-006~008）**：产品/架构决策后回写 P0-SEC-001 验收口径（决策池 §1/§3）；决策前不编造权限码。
  3. **范围外 17**：ApprovalWorkflow 13 → **PD-009**（跨域通用审批权限模型，产品+架构，KL-044）；QuickStockIn 3 → **P2-SEC-010**（已拆卡，下一周期，KL-045）；MiniProgram 1 → **已并入 KL-043 豁免**（KL-046，未覆盖 53→52）。
- 闭环后由 architect 更新 roadmap 整卡状态（当前 roadmap §4 已记录整卡 ✅ PASS_WITH_LIMITATION，REG-SEC-010 固化中）。

---

## 4. 回归联调计划（发布前收口，2026-08-10 排期）

| 联调项 | KL | 内容 | 责任 | 排期 |
|--------|----|------|------|------|
| 真实登录联调 | KL-013 | 真实登录联调：登录→/me→路由→按钮渲染（真实登录态全链路） | 回归 Agent 执行，qa 复核 | 3C 闭环后、最终 Release Gate 前 |
| DATA-009/010 运行期联调 | KL-020 | DATA-009 聚合正确性 + DATA-010 切换刷新（含 V-1/V-2 合并口径） | 回归 Agent 执行，qa 复核 | 3C 闭环后、最终 Release Gate 前 |
| SmartRestock 采购申请联调 | KL-029 | SmartRestock 采购申请落库/采购侧可见 | 回归 Agent 执行，qa 复核 | 3C 闭环后、最终 Release Gate 前 |
| 导出 CSV 内容解析 | KL-030 | 导出 CSV 文件内容解析（10 页） | 回归 Agent 执行，qa 复核 | 3C 闭环后、最终 Release Gate 前 |
| TopNavbar 通知已读联调 | KL-031 | TopNavbar 通知已读/全部已读真实登录态验证 | 回归 Agent 执行，qa 复核 | 3C 闭环后、最终 Release Gate 前 |

- 状态：**已执行（2026-08-10）**——KL-020（DATA-009/010 运行期联调，`docs/quality/p0-kl-020-rerun.md`）与 KL-031（通知已读联调，`docs/quality/p0-kl-031-rerun.md`）已闭环并转基线（REG-DATA-016/017、REG-MOCK-006）；KL-029（SmartRestock 采购申请落库）与 KL-030（导出 CSV 仿真 10/10）已由 `sprint-3-closeout-regression-liaison.md` 闭环；KL-013 主链路 PASS；**残留浏览器级缺口（KL-013 渲染 / KL-030 下载 / KL-020-V2 切换交互 / KL-031-R1 通知面板 UI）统一归入发布前浏览器冒烟待办**（验证缺口，非代码缺陷、不构成 FAIL）。
- 说明：本计划为**发布前收口**联调批次（回归 Agent 执行，qa 复核）；各联调项验证结果回写对应 KL 收口（KL-013→P0-SEC-006、KL-020→P1-DATA-009/010、KL-029→P1-MOCK-004、KL-030→P1-EXPORT-002、KL-031→P1-MOCK-010）；与 KL-009（role_permission 按钮可见性联调）待办同批合并执行。

---

*初始创建：2026-08-10。本文件仅记录已知限制，未修改任何代码，未改变 Sprint-2 验收结论。*
*追加：2026-08-10 登记 Sprint-3A 第一阶段 QA 限制项（KL-006~012，来源 `docs/quality/sprint-3a-phase1-qa-report.md` L-1~L-4 / O-1~O-3），不改变 Sprint-3A 第一阶段验收结论（PASS 6 / PASS_WITH_LIMITATION 2 / FAIL 0）。*
*追加：2026-08-10 复审登记（KL-013~016，来源 `docs/quality/sprint-3a-security-qa-report.md` 复审 L-5 / L-6(O-5) / O-4 / O-6），复审结论 PASS 5 / PASS_WITH_LIMITATION 3 / FAIL 0（FAIL=0 不阻断；3 项 PWL 明细见 §3 复审后评估），不改变 Sprint-3A 第一阶段验收结论（8/8 通过）。*
*追加：2026-08-10 登记 Sprint-3B-1 QA 限制/验证缺口/观察项（KL-017~028，来源 `docs/quality/sprint-3b1-qa-report.md` L-1~L-6 / V-1~V-2 / O-1~O-6），3B-1 结论 PASS 4 / PASS_WITH_LIMITATION 5 / FAIL 0（FAIL=0 不阻断）。L-1 并入 P1-MOCK-004 承接（3B-2 执行中）；O-2 拆卡 P1-DATA-011；P1-DATA-009 口径缺失登记 PD-005（关联 KL-019，不阻断当前发布）。不改变 Sprint-3A 及此前全部验收结论。*
*追加：2026-08-10 登记 Sprint-3B-2 QA 限制/验证缺口/观察项（KL-029~042，来源 `docs/quality/sprint-3b2-qa-report.md` V-1~V-3 / O-1~O-8 / 待后端接口清单），3B-2 结论 PASS 2 / PASS_WITH_LIMITATION 3 / FAIL 1 → R1 复验 PASS（FAIL=0 不阻断）。KL-017 状态更新为已解除（L-1 由 P1-MOCK-004 承接闭环）；KL-024 排期更新为并入 3B-3 窗口（3B-2 未执行）；O-4 拆卡 P1-DATA-012。V-1~V-3 联调缺口回归阶段闭环，发布前收口。不改变 Sprint-3A/3B-1 及此前全部验收结论。*
*追加：2026-08-10 登记 P0-SEC-001 子批 C/D 相关（KL-043：设计公开端点豁免清单未文档化，供扫描脚本豁免参考）；追加 P0-SEC-001 子批推进评估（A ✅ QA PASS_WITH_LIMITATION；C/D ✅ 已提交待 QA；B 🔄 执行中；BLOCKED 候选 30 方法 + 6 端点登记 PD-006~008，不纳入通过数）。不改变 Sprint-3A/3B 及此前全部验收结论。*
*追加：2026-08-10 整卡收口登记（架构宣布 P0-SEC-001 整卡 PASS_WITH_LIMITATION，A/B/C/D 全过 FAIL=0）：范围外 17 登记 KL-044~046（ApprovalWorkflow 13 → PD-009；QuickStockIn 3 → 拆卡 P2-SEC-010；MiniProgram 1 → 豁免成立并入 KL-043，34→35）；KL-043 补录 /v1/mp/**（待架构核对）。排除项闭环路径见 §3 整卡收口评估。不改变 Sprint-3A/3B 及此前全部验收结论。*
*追加：2026-08-10 P1-SEC-009 收口登记（来源 `docs/quality/p1-sec-009-qa-report.md`，qa PASS）：KL-007 状态更新为**已关闭**（P0-SEC-006 L-2 渗透验证活体 PASS：伪造 token 401×3 / 未知角色 403×3 / 过期 401×2 等 14 用例全 PASS）；新增 KL-047（QA 观察 O-1：退款审批 approveUserId 信任客户端参数，审批归属业务规则）→ 登记 PD-010 待产品决策；O-2（401 文案）仅记录不拆卡。发布决策摘要追加 P1-SEC-009 收口评估（渗透门禁前置项已关闭；KL-013/KL-020 联调缺口仍为发布前收口待办）。不改变 Sprint-3A/3B 及此前全部验收结论。*
*追加：2026-08-10 发布前收口排期登记「§4 回归联调计划」——KL-013（真实登录联调：登录→/me→路由→按钮渲染）/ KL-020（DATA-009 聚合正确性 + DATA-010 切换刷新，V-1/V-2 合并口径）/ KL-029（SmartRestock 采购申请落库/采购侧可见）/ KL-030（导出 CSV 内容解析，10 页）/ KL-031（TopNavbar 通知已读真实登录态）；责任=回归 Agent 执行、qa 复核；排期=3C 闭环后、最终 Release Gate 前；状态=待执行。不改变 Sprint-3A/3B 及此前全部验收结论。*
*追加：2026-08-10 观察登记 **O-DEF2-1 → KL-048**（来源 P0-DATA-009 developer 修复报告 §五.1「风险」行注记的独立缺陷）：趋势接口（live-monitor/trend、decision-board/revenue-trend）对 admin 返回空数据——`resolveStoreFilter` 将 `getAccessibleStoreIds` 返回 null（语义=不限制，DataPermissionServiceImpl:362-367 注释明确「管理员直接返回 null」）误判为 NO_ACCESS_STORE_ID=-1 → `AND store_id=-1` → 空结果。风险中-低（admin 数据可见性，非越权），**修复前判定不阻断发布**（非 500 级，与 DEF 的 P0 区分），但发布前需架构裁决（数据权限语义）；评估结论**不入 PD 池**（语义有注释依据，属代码缺陷非规则缺失，修复方向明确：null → 不限制、不设 store_id 条件），待修缺陷挂后续批次。不改变 Sprint-3A/3B、P0-DATA-009 本体验收口径及此前全部结论。*
*追加：2026-08-10 KL-020 重跑闭环登记（来源 `docs/quality/p0-kl-020-rerun.md`，regression 活体实证）：**DEF-1/DEF-2 已关闭**（QA 复验 p0-data-009-r1 = PASS_WITH_LIMITATION + 回归重跑活体闭环：overview 200 code=0 造数 8/9 字段精确匹配、三趋势接口无静默降级）；V-2 维持代码级 PASS（浏览器缺口单独登记）；**新暴露活跃缺陷 DEF-4**（`RechargeRecordMapper.xml` selectStatsOverview 9 个别名驼峰未加引号 → PostgreSQL 折叠小写 → Map key 全小写 → MarketingMemberServiceImpl.java:230-233 / RechargeStatsServiceImpl.java:39-54 按驼峰 get 取 null → 充值统计字段恒 0，静默数据失真）→ **建卡 P0-DATA-010（发布阻断，DOING）** + **新登记 KL-049**（与卡关联）；KL-020 状态更新为**部分关闭**（DEF-1/DEF-2/V-2 已关闭，DEF-4 修复 + QA/回归 -R 复验通过前不得全关、发布门禁保持 FAIL）；P0-DATA-009 卡追加验收状态与承接备注（验收标准 1 非空充值缺口 → P0-DATA-010 承接）。不改变 Sprint-3A/3B、P0-DATA-009 本体验收口径及此前全部结论。*
*追加：2026-08-10 **DEF 修复批次收口登记**（来源 QA 复验 `p0-data-009-r1-qa-report.md` = PASS_WITH_LIMITATION / `p0-mock-001-r1-qa-report.md` = PASS 7/7 / `p0-data-010-r1-qa-report.md` = PASS 7/7 + 回归重跑 `p0-kl-020-rerun.md` / `p0-kl-031-rerun.md` 活体闭环）：**KL-020 → 已关闭**（DEF-1/DEF-2/DEF-4 全部修复复验闭环，残留 V-2 浏览器切换缺口登记发布前浏览器冒烟）；**KL-031 → 已关闭**（DEF-3 修复 + QA 复验 PASS + 重跑三端点活体闭环，残留 R1 浏览器通知面板 UI 交互同登记）；**KL-049 → 已关闭**（DEF-4 QA 复验 PASS 7/7 + 转基线 REG-DATA-017）；**KL-048 维持待架构裁决**（admin 趋势空 + kpi-summary?storeIds=1 NPE null 防御并入处置范围，发布前裁决）；发布决策摘要追加「DEF 修复批次收口（2026-08-10）」（§3）；Release Gate 复评 PASS（FAIL 明细消除，以 regression 输出为准）。不改变 Sprint-3A/3B/3C 及此前全部验收结论。*
*追加：2026-08-10 §3 追加「16 项非阻断治理移出发布链（2026-08-10）」（来源 `docs/quality/final-release-gate-sprint3.md` §3.1 最终 PWL 快照，planner 会话3）：17 行（快照口径 16 项，逐项清单实列 17 行，计数差异提请 architect 核对）**移出当前发布链，不阻塞 Production Release Gate**（门禁 WAITING 仅由 KL-048 修复闭环 + PD-001/003 决策组成），发布后按批次执行（后端排期 9 / 产品决策 4 / 下一周期治理 2 / 联调补验 2）；逐项状态保持（不关闭、不销号），KL 状态列不改写，批次归属以任务池 §8 为准。不改变 Sprint-3A/3B/3C 及此前全部验收结论。*
*追加：2026-08-11 浏览器冒烟登记（来源 `docs/quality/browser-smoke-sprint3.md`，regression 会话6 实测，planner 会话3 落盘）：**KL-013 → 已关闭**（5 项浏览器缺口实测 PASS，P0-SEC-006 L-5 收口）；**KL-020-V2 → 实测关闭**（门店下拉切换重载 PASS，KL-020 关闭链完整）；**KL-031-R1 → 实测关闭**（通知面板 UI 5 项 PASS，KL-031 关闭链完整）；**KL-030 → 部分关闭**（OrderQuery 导出 PASS + 活跃缺陷 DEF-A/DEF-B 阻断 → **建卡 P0-ASSET-001 / P0-DEVICE-001**，发布阻断 DOING，修复前不关闭）；冒烟门禁 **FAIL（禁止放行）**；§3 发布决策摘要追加「浏览器冒烟（2026-08-11）」；任务池 §7.9 同步建卡。不改变 Sprint-3A/3B/3C 及此前全部验收结论。*
*追加：2026-08-11 **KL-048 修复闭环登记**（来源 QA 复验 `docs/quality/kl-048-r1-qa-report.md` = PASS_WITH_LIMITATION + 回归基线 `production-regression-test.md`，planner 会话3）：主表行 + §2 明细 + §3 摘要 → **KL-048 已关闭（2026-08-11）**（3 文件 4 处 null 前置分支 + NPE 防御；REG-DATA-016 唯一限制关闭转 PASS；新增基线 REG-DATA-018）；**「发布前需架构裁决」需求消除**；§3 追加「KL-048 修复闭环（2026-08-11）」块——**Production Release Gate WAITING 条件更新**为 PD-001/003 产品决策（及 002/004）+ DEF-A/B（P0-ASSET-001 / P0-DEVICE-001）修复闭环；任务池 §7.9 同步：P0-ASSET-001 / P0-DEVICE-001 → **待 QA 复验**（developer 修复证据已提交 2026-08-11）。不改变 Sprint-3A/3B/3C 及此前全部验收结论。*
*追加：2026-08-11 **DEF-A/B QA 复验收口登记**（来源 QA 复验 `docs/quality/p0-asset-001-r1-qa-report.md` = PASS / `docs/quality/p0-device-001-r1-qa-report.md` = PASS，planner 会话3）：**P0-ASSET-001 ✅ PASS（5/5 实证：基线 dd33ee3 复现 TS2304 → 工作区 0 错误、diff 仅 +1 行 `*/`、构建 EXIT=0）**；**P0-DEVICE-001 ✅ PASS（6/6 达成含活体实测：三接口 200 code=0 + deviceType 字符串往返 + 空表不回归 + 编译 EXIT=0）**——两卡从 Production Release Gate WAITING 条件消除；**新登记 KL-050**（设备域同型漂移：connectionType Integer vs varchar(50) / storeId Long vs varchar(50)，DEF-B 同族，非数值数据下 500 风险，不阻断，建议 audit 立项）、**KL-051**（设备域迁移文件列名漂移：V1.0.0.100 L933-934 created_at/updated_at vs 实体/活体库 create_time/update_time，文件级无运行时影响，不阻断）、**KL-052**（KDS 设备类型展示归属观察，产品排期，不进 PD 队列）；§3 追加「DEF-A/B 复验通过（2026-08-11）」块——**WAITING 剩余条件 = PD-001/003 产品决策（及 002/004）+ 冒烟门禁 regression 复评**（KL-030 折旧/告警两页导出重跑闭环，以 regression 输出为准）；任务池 §7.9/§0/§0.1 同步（两卡 → ✅ PASS，数量统计 P0=25 / 合计 77 不变）。不改变 Sprint-3A/3B/3C 及此前全部验收结论。*
*追加：2026-08-11 **Sprint-4 首批实现闭环登记**（来源 `docs/quality/sprint-4-backend-qa-report.md` = PASS 3/FAIL 0 + `docs/quality/sprint-4-frontend-qa-report.md` = PASS 8/8，planner 会话3）：**KL-001 / KL-002 → 阻断判定解除（2026-08-11）**（PD-001~004 决策到达 + 实现 QA 验收 PASS）；**KL-003 → 处置完成（2026-08-11）**（PD-004 下线决策执行：入口已移除、演示数据不再暴露）；§3 追加「Sprint-4 首批实现闭环（2026-08-11）」块——**Production Release Gate WAITING 剩余条件 = 待 regression 转基线 + 冒烟门禁复评（KL-030 折旧/告警导出重跑闭环）**，PD-001~004 决策 / KL-048 / DEF-A/B 均已从 WAITING 条件消除；PWL 六项已销号（REG-RULE-001/002/003、REG-FIN-001、REG-HR-002、REG-HR-004，architect 执行，planner 确认无遗漏）；任务池 §0/§0.1/相关卡同步 5 卡 → ✅ PASS（P0-FIN-002 / P0-HR-002 / P0-FIN-001 / P0-HR-005 / P1-MOCK-005）。不改变 Sprint-3A/3B/3C 及此前全部验收结论。*

---

## OIC-BE 轨状态登记（2026-08-16，planner 会话3 追加；只增不改，KL-054/055 历史条目内容未改动）

- **轨启动**：**OIC-BE = 后端整改池治理轨**（OIC Backend Remediation），承接 **KL-054 / KL-055 / ETM 家族专项**（Entity-Table Consistency Remediation，架构裁决 D-2）与**后端池联动项**（OIC-2/RM 轨登记的 `*‑BE‑*` 后端依赖）。用户 2026-08-16 指令排卡。
- **后端窗口状态**：**已解锁（2026-08-16 12:55，管理端 Batch 2 observe 收口）**——此前观察期内冻结后端（不改码不落库）；窗口后按 D-2 开工，分批迁移+验收（不全仓一次性改）。
- **Batch 1 首批排卡（3 卡，TODO）**：
  - **OICBE-B1-001 = ETM-001（P0）**：`registration_code` 无建表无 catch（RegistrationCodeMapper.xml L19/25/31/37 硬编码 + RegistrationCodeServiceImpl L340/379 无 catch → 缺表 500；V20260625_004 中 registration_code 仅为 onboarding_records 列）。三向核对（实体 ↔ migration ↔ information_schema）+ migration 先行（建表或对齐由架构/契约确认，**归属不确定登记 BLOCKED 待裁决**）+ catch 语义改进（失败透传，与规则无关可先行）。REV：REG-ETM-001。
  - **OICBE-B1-002 = KL-054（P1）**：sales_order 漂移（SalesOrder/SalesOrderDetail 无建表 vs 真实表 orders/OrderNew 映射；DashboardServiceImpl L96 BadSqlGrammarException catch 降级致 dashboard 今日销售概览空）。三向核对 + **契约决策点：SalesOrder 与 OrderNew 是否同表合并（待架构/契约确认，不猜测）** + migration 先行 + DashboardServiceImpl 查询修复。REV：REG-ETM-002/003。
  - **OICBE-B1-003 = KL-055（P1）**：budgets 表结构漂移（旧采购预算结构 V20260704_001 vs finance/Budget 实体 → BadSqlGrammarException → HTTP 200 + code:500「系统繁忙」；分页 COUNT 短路假空列表）。先读 KL-055 详情（O-20260815-02）→ 三向核对 → migration 对齐（涉业务结构取舍登记 BLOCKED 待裁决）→ 相关代码核对（假空形态处置）。REV：REG-KL055。
- **ETM-106 不重复排**：已在 RM-B2 轨（RM-B1-001 方案卡 + RM-B2-实施卡1 已落库 V20260816_001 + 实施卡2/3），OIC-BE 轨不重复；新 migration 版本号避让 V20260816_001（从 V20260816_002 起）。
- **文件归属隔离（RM 轨）**：ReceiptConfirmationServiceImpl.java / InventoryServiceImpl.java / entity/Inventory.java / inventory 相关 migration 为 RM 轨专属，OIC-BE 卡不得触碰。
- **契约决策点登记（待裁决，不猜测）**：① registration_code 归属（建议 PD-014）；② SalesOrder 与 OrderNew 同表合并（建议 PD-015）；③ budgets 对齐方向（触发条件式）——详见任务池。
- **任务池真相源**：`oic-be-task-board.md`（OIC-BE 轨任务池，Batch 1 三卡详情 + 红线 + 占位区；文件创建待编辑白名单放行，落盘前以本登记 + 会话产出为准）。

---

## 财务数据血缘 + 勾稽专项依赖登记（2026-08-31，planner 会话3 追加；只增不改）

- **KL-056（财务数据血缘+勾稽 UI 专项依赖，来源 `docs/quality/finance-traceability-inventory-20260831.md` 缺口清单 GAP-A1/A2/A3/B1/B2/B3 + `remediation-roadmap.md` §5.11）**：
  - ① **勾稽状态列依赖**：fund_flows.status（待核销/已核销）需 **Batch0-方案2 同批落库**（PD-028 已决，`product-decision-backlog.md`）；落库前「待核销/已核销」不悬空不伪造（占位卡 `P1-UI-TRACE-B01`，BLOCKED_EXTERNAL_DEPENDENCY，不占通过数）；
  - ② **后端接口缺口**：GAP-B1（无按 voucherId 查 fund_flows 接口，凭证→流水反向缺）/ GAP-B2（无 sourceType+sourceId 聚合查单据摘要接口）/ GAP-B3（receipt 表缺 fund_flow_no 列、ReceiptVO.fundFlowNo 未回填）——登记卡 `P1-UI-TRACE-BE01` 标注后端池排期，**决策前不实现不猜测**；
  - ③ **后端补字段待产品决策**：GAP-A2（fund_flows 来源键：流水→单据直达 vs 两跳穿透）/ GAP-A3（应收核心订单来源键，P1-FIN-TRACE-003，ReceivableServiceImpl.java:254 orderId=null）——产品确认方案前不实现；
  - ④ **待核实项**：GAP-D1~D4（PD-028 决策池状态差异 / VoucherTypeMap 口径 / ReceiptVO.fundFlowNo 数据来源 / balance vs balanceAfter 语义）——如实登记不猜测。
  - planner 仅登记，未写代码、未做正确性判断；不改变既有 KL 状态与验收结论。

---

## 财务模块任务型重构专项 · 阶段一诊断限制登记（2026-09-03，planner 会话3 追加；只增不改）

> 来源：auditor 阶段一诊断报告 `docs/quality/finance-module-diagnosis-20260903.md`（2026-09-03，只读诊断未修改生产代码）。
> 登记口径：以下 5 项为「顺带发现」风险类（诊断报告 §5 第 9~12 条），按协议登记 KL；**其余缺口（导出全缺/打印缺/假筛选 C 类 3 处/B 类 5 处/工具栏缺口/字段映射）属财务模块任务型重构专项阶段三实现范围，登记为专项待办（任务池 §11 P1-FIN-WS-001~006），不单列 KL**。编号说明：KL-056 已被 2026-08-31 财务数据血缘+勾稽 UI 专项依赖登记占用（上文节标签），本批从 **KL-057** 起编号。

### KL-057｜FinanceReport 资产负债表/现金流量表 API 断流（P0 断流）

- **来源任务**：阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.8（2026-09-03）；关联任务池 §11 P1-FIN-WS-006（决策工作台·报表）
- **风险等级**：**P0（模块可用性）**——发布/模块可用性风险：页面切换「资产负债表」「现金流量表」即失败/空数据，且 catch 静默（FinanceReport.vue:191-196）无任何错误提示，「数据无感」核心根因之一
- **是否阻断发布**：否（阶段一诊断登记，不构成发布门禁 FAIL；整改归财务模块任务型重构专项阶段三实现批，发布门禁以 roadmap/回归为准）
- **现状**：`report.ts:63-64` `getBalanceSheet` 注释「后端 ReportController 无 /balance-sheet 端点，当前调用会 404」；`report.ts:79-80` `getCashFlowStatement` 同上；`ReportController.java:30-83` 仅 profit-statement / income-expense-summary / receivable-statistics / payable-statistics / aging-analysis / cost-structure / budget-execution 7 个端点，**无 balance-sheet、无 cash-flow**；页面 `FinanceReport.vue:256-258` 报表类型切换 + loadData:170-200 分派走不同端点
- **处置**：登记待阶段三（P1-FIN-WS-006 决策工作台范围）；修复方向（供卡内参考，由 developer 执行、qa 验收）：后端补 balance-sheet/cash-flow 端点 或 隐藏失效 Tab + 补失败反馈（catch 静默改失败透传）；方向选择属阶段二设计评审输出，planner 不预判
- **关联**：P1-FIN-WS-006 / 阶段二设计卡 P1-FIN-IA-001

### KL-058｜FinancePeriod operatorId 兜底 '1'（P1 身份伪造风险）

- **来源任务**：阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.7（2026-09-03）；关联任务池 §11 阶段三实现批
- **风险等级**：**P1**——`FinancePeriod.vue:40` `String(permissionStore.userInfo?.userId ?? '1')`：取不到用户时默认 userId=1（管理员），**结账/反结账/损益结转均携带此身份**，有越权操作风险
- **是否阻断发布**：否（阶段一诊断登记；整改排专项阶段三）
- **现状**：`FinancePeriod.vue:40` operatorId 兜底 '1'；结账 handleClose:290-305 / 反结账 handleReopen:308-323 / 损益结转 handleProfitTransfer:266-285 均带此身份
- **处置**：登记待阶段三实现批；修复方向（供卡内参考）：operatorId 强制从会话取（取不到即禁止操作/报错，**禁止兜底管理员身份**），developer 执行、qa 验收
- **关联**：P1-FIN-WS-001~006（阶段三实现批）

### KL-059｜FinanceTax 税期选项硬编码（E 超期失效）

- **来源任务**：阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.5（2026-09-03）；关联任务池 §11 P1-FIN-WS-004（合规工作台·缴税）
- **风险等级**：中（E 类）——Tab1 纳税期间 el-select 选项硬编码 **202603~202606**（FinanceTax.vue:599-602），超期即失效（登记日 2026-09 已超 202606）
- **是否阻断发布**：否（阶段一诊断登记；整改排专项阶段三）
- **现状**：`FinanceTax.vue:598-603` 税期选项硬编码；筛选本身接后端有效（TaxRecordDTO.taxPeriod 字段存在，A 类），问题在选项来源
- **处置**：登记待阶段三实现批；修复方向（供卡内参考）：税期选项动态化（后端期间/税期来源或按当前日期动态生成），developer 执行、qa 验收
- **关联**：P1-FIN-WS-004

### KL-060｜InvoiceReimbursement 驳回=作废共用 cancelled（P2 语义混叠）

- **来源任务**：阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.13（2026-09-03）；关联任务池 §11 阶段三实现批
- **风险等级**：**P2（语义混叠）**——审批通过=status 置 issued、**驳回=置 cancelled**（InvoiceReimbursement.vue:154-157）：「驳回」与「作废」共用同一状态值，驳回的发票无法区分
- **是否阻断发布**：否（阶段一诊断登记；整改排专项阶段三）
- **现状**：`InvoiceReimbursement.vue:154-157` 驳回=置 cancelled；另红冲/作废入口缺失（invoice.ts:137-152 void/redFlush 端点存在但页面无入口）
- **处置**：登记待阶段三实现批；**状态机语义拆分（驳回 vs 作废 是否拆分状态/如何标识）涉及业务规则，需产品决策时登记 PD（决策前不猜测）**；红冲/作废入口补齐属工具栏缺口（阶段三范围）
- **关联**：P1-FIN-WS-002（核销工作台·收付）

### KL-061｜AutoVoucher「最近执行」用 updateTime 冒充（P2 展示失真）

- **来源任务**：阶段一诊断 `docs/quality/finance-module-diagnosis-20260903.md` §1.12（2026-09-03）；关联任务池 §11 阶段三实现批
- **风险等级**：**P2（展示失真）**——「最近执行」列展示 updateTime/createTime（AutoVoucher.vue:67），非真实执行时间（伪语义）
- **是否阻断发布**：否（阶段一诊断登记；整改排专项阶段三）
- **现状**：`AutoVoucher.vue:67`「最近执行」= updateTime/createTime；另规则 CRUD 入口全缺（transfer-template.ts:75-95 有 create/update/delete 但页面无入口，工具栏缺口）
- **处置**：登记待阶段三实现批；「最近执行」真实执行时间依赖后端执行记录字段，**未确认前不猜测实现**（后端缺字段时登记 PD 或按「不悬空不伪造」处置）
- **关联**：P1-FIN-WS-003（记录工作台·凭证/自动凭证联动）

---

## 财务导出能力盘点限制登记（2026-09-03，planner 会话3 追加；只增不改）

> 来源：auditor 导出盘点报告 `docs/quality/finance-export-inventory-20260903.md`（P1-FIN-EXPORT-001 交付物，2026-09-03 只读盘点未修改生产代码）。
> 登记口径：本批仅登记盘点暴露的 2 项限制（KL-062/063）；**其余导出缺口（13/13 全缺 / 统一服务缺失 / 审计注解缺失 / 前端 CSV 工具）登记为后端池任务（任务池 §11.6 六张卡），不单列 KL**。编号说明：KL-057~061 已用于阶段一诊断限制登记，本批从 **KL-062** 起编号。

### KL-062｜/vouchers/export P0 桩实现（假成功：new byte[0] + 无 UI 消费方断流）

- **来源任务**：导出盘点 `docs/quality/finance-export-inventory-20260903.md` §1.1/§5.3（P1-FIN-EXPORT-001 交付物，2026-09-03）；承接后端池卡 **P0-FIN-EXPORT-001 / P0-FIN-EXPORT-002**（任务池 §11.6）
- **风险等级**：**高（审计真实性风险，假成功类，同 P0-FIN 家族）**——`/v1/finance/auto-vouchers/vouchers/export` 返回 `new byte[0]`（HTTP 200 + 空文件），前端一旦接线即导出空文件且无错误提示；唯一财务导出端点不可用（盘点结论：0 页可用）
- **是否阻断发布**：否（登记即处置：后端池排期；前端 0 消费方，无活跃暴露面；接线前必须完成修复）
- **现状**：`AutoVoucherController.java:200-211` 端点（权限 `finance:auto-voucher:export`，**无 @OperationLog**）+ `AutoVoucherManageServiceImpl.java:380-385` 桩实现（返回 byte[0] 假成功）；页面 AutoVoucher.vue:14-16,45-57 消费 transfer-templates（规则筛选，非凭证筛选）→ 凭证域端点（含导出）无 UI 消费（**API 断流**），AutoVoucherQueryDTO 无消费方
- **处置**：后端池排期——**P0-FIN-EXPORT-001**（桩修复：实现真实导出【复用 buildVoucherQueryWrapper :392-408】或删除端点，**不得返回空字节冒充成功**）+ **P0-FIN-EXPORT-002**（断流修复：补凭证列表+导出入口或明确下架凭证域端点）；验收按修正③四原则（筛选结果全部 / 统一服务 / 口径一致 / 审计）
- **关联**：P1-FIN-EXPORT-001（盘点卡，✅ DONE）/ 任务池 §11.6 / KL-063

### KL-063｜日结导出表头口径混用（元/分并存）

- **来源任务**：导出盘点 `docs/quality/finance-export-inventory-20260903.md` §1.2/§4.3 口径反例（P1-FIN-EXPORT-001 交付物，2026-09-03）；承接后端池卡 **P1-FIN-EXPORT-003**（任务池 §11.6）
- **风险等级**：中——`StoreManagementSettlementController.java:209-215` 导出表头「总营收(元)」与「现金金额(分)」并存（口径混乱直接暴露在导出文件），:238 退款笔数/作废数据留空；金额存储为 Long 分（FinanceVoucher.java:47-51 注释「单位：分」）
- **是否阻断发布**：否（记录存在性：既有占位 CSV，整改排后端池；日结导出属 Batch 1 对账工作台范围，Batch 1 实现前需收口）
- **现状**：表头口径混乱（元/分并存 :209-215）；退款笔数留空（:238）；违背修正③「口径一致」（金额单位错误导向）
- **处置**：后端池排期（P1-FIN-EXPORT-003）——统一分口径存储、输出层转元（复用 `@/utils/money` 转换链 + finance/converters.ts，禁止各端点自行 /100 或 .toFixed）；补齐字段；PD-035 决策后按决策执行格式/上限
- **关联**：P1-FIN-EXPORT-001（盘点卡，✅ DONE）/ 任务池 §11.6 / 修正③「口径一致」/ PD-035

### KL-064｜POS 历史数据同步风险（W1-EC-04C-OrderQuery 核心风险）

- **来源任务**：W1-EC-04C-OrderQuery（任务池 §21）/ `docs/architecture/system-fact-map/wave1-production-gate-001.md` / `docs/architecture/system-fact-map/wave1-production-gate-002.md`
- **风险等级**：**高（灰度前必须解决）**——管理端订单查询迁移从 orders_legacy 改为 orders 表，核心风险为 POS 历史数据是否已全部同步到 orders 表；若未完全同步，管理端将丢失历史订单数据
- **是否阻断发布**：**是（灰度前必须解决）**——若 orders_legacy 有数据但 orders 无对应数据，管理端订单查询将返回不完整数据，影响财务对账和运营决策
- **现状**：
  1. `orders_legacy` 表：POS 历史订单数据（元为单位）
  2. `orders` 表：新核心订单数据（分为单位）
  3. 迁移后管理端将从 orders 表读取，若 orders_legacy 有数据但 orders 无对应数据，管理端将丢失历史订单
- **验证方法**：
  1. 生产环境只读执行：`SELECT COUNT(*) FROM orders_legacy WHERE deleted = 0`
  2. 生产环境只读执行：`SELECT COUNT(*) FROM orders WHERE deleted = 0`
  3. 对比结果：若 orders_legacy=0 或 orders≥legacy，方可迁移
  4. 详细验证脚本：`docs/architecture/system-fact-map/wave1-production-gate-002.md` §三
- **处置**：
  1. 生产环境数据扫描验证（只读执行，不修改数据）
  2. 验证通过后方可执行 W1-EC-04C-OrderQuery 迁移
  3. 验证不通过则需先完成数据迁移（PD-015 阶段二）
- **关联**：W1-EC-04C-OrderQuery（任务池 §21）/ KL-048（趋势接口数据权限问题）/ PD-015（订单迁移决策）

*追加：2026-09-03 阶段一诊断完成登记（planner 会话3，来源 auditor 只读诊断 `docs/quality/finance-module-diagnosis-20260903.md`）：登记 KL-057~061 五项顺带风险（P0 断流 FinanceReport / P1 operatorId 兜底 '1' / E 税期硬编码 / P2 驳回-作废语义混叠 / P2 AutoVoucher 最近执行伪语义）；其余缺口（导出 13/13 全缺、打印 8 页缺、C 类假筛选 3 处、B 类 5 处、工具栏缺口、字段映射）属财务模块任务型重构专项阶段三实现范围，登记为专项待办（任务池 §11 P1-FIN-WS-001~006）不单列 KL；PD 候选 2 项登记决策池（PD-033 收款冲正 / PD-034 预算审批流归属），科目余额真相源 #5 已决（PD-031）不重复登记；任务池 §11 P1-FIN-DIAG-001 DOING → **待评审**（auditor 交付物已落盘，评审通过后转 DONE 并放行 P1-FIN-IA-001）。planner 仅登记与状态更新，未写代码、未做正确性判断；历史条目零修改，不改变既有 KL 状态与验收结论。*
*追加：2026-09-03 导出盘点限制登记（planner 会话3，来源 auditor 盘点报告 `docs/quality/finance-export-inventory-20260903.md`，P1-FIN-EXPORT-001 交付物）：新增 **KL-062**（/vouchers/export P0 桩实现——AutoVoucherManageServiceImpl.java:380-385 返回 `new byte[0]` 假成功 + 凭证域端点无 UI 消费方断流，审计真实性风险假成功类同 P0-FIN 家族，承接后端池 **P0-FIN-EXPORT-001/002**）+ **KL-063**（StoreManagementSettlementController.java:209-238 日结导出表头口径混用 元/分并存 + 退款笔数留空，承接后端池 **P1-FIN-EXPORT-003**）；编号自 KL-062 起（KL-057~061 已用于阶段一诊断限制登记）；其余导出缺口（13/13 全缺 / 统一服务缺失 / 审计注解缺失 / 前端 CSV 工具）登记为后端池任务（任务池 §11.6 六张卡）不单列 KL；PD-035 导出规则缺失登记决策池；任务池 §11 P1-FIN-EXPORT-001 盘点卡 → ✅ DONE。planner 仅登记与状态更新，未写代码、未做正确性判断；历史条目零修改，不改变既有 KL 状态与验收结论。*

*追加：2026-09-09 W1-EC-04C-OrderQuery 限制登记（planner 会话3，来源 task planning）：新增 **KL-064**（POS 历史数据同步风险——管理端订单查询迁移从 orders_legacy 改为 orders 表，核心风险为 POS 历史数据是否已全部同步到 orders 表；若未完全同步，管理端将丢失历史订单数据）。编号自 KL-064 起（KL-063 已用于导出盘点限制登记）。planner 仅登记与状态更新，未写代码、未做正确性判断；历史条目零修改，不改变既有 KL 状态与验收结论。*

*追加：2026-09-23 P0-KDS-DEDUCT 收口 + P1 Scope 登记（planner，来源 `docs/architecture/03-review/p0-kds-deduct-closure-001.md` = CLOSED_WITH_REGISTERED_RISKS + `p1-order-number-scope-001.md`）：新增 **KL-065**（审计自身失败无结构化持久化兜底 R1）、**KL-066**（REQUIRES_NEW 失败路径连接池触顶定量风险 R2）、**KL-067**（selectUndeducted 可能含 FAILED 行 R3）、**KL-068**（orders.order_number NOT NULL 阻断非 POS 创建 E2E，独立 P1）。编号自 KL-065 起（KL-064 已用于 POS 历史同步）。同步 `product-decision-backlog.md` 新增 **PD-043**（requiredQty≤0 三裁定 + notes 持久化）。planner 仅登记，未写业务代码、未做正确性判断；历史条目零修改，不改变既有 KL 状态与验收结论。*
*追加：2026-09-23 Scope Amendment G3 裁决（planner，来源 `docs/architecture/03-review/p1-order-number-scope-002.md` §G3）：**KL-068「是否阻断发布=是」的发布单元范围 = `UNKNOWN / PENDING_CONFIRMATION`**——仓内 Release Gate 规则（`production-collab/SKILL.md` §五）仅约束回归 FAIL>0；KL 清单用途为「发布评审输入」；未找到将「阻断发布」映射到整系统/含 KDS/仅 /v1/orders 链的政策或 CI 门禁。行内括号「从 /v1/orders 创建起的全链路」为登记时**功能影响面注解（INFERENCE）**，非已批准发布政策。缺失证据：发布政策对 KL 阻断列语义定义、Release Gate 是否计入未关闭高风险 KL、发布负责人书面放行条件。**禁止工程侧自行改写为「阻断整系统」或「不阻断」**；历史行不删除。关联 Scope 001 → SUPERSEDED_BY 002。*

*追加：2026-09-23 DS 六项治理澄清之 KL-068 双状态消解（planner，来源 `p1-order-number-scope-002.md` §G3）：主表行已分列 **Historical**（原登记 `是（从 /v1/orders 创建起的全链路）` = `HISTORICAL / SUPERSEDED_INTERPRETATION`，provenance 保留不删除）与 **Current（唯一）** = `UNKNOWN / PENDING_CONFIRMATION`。全仓引用 KL-068「阻断发布」时 **Current 仅认 PENDING_CONFIRMATION**；把 Historical「是」继续当 current = 错误。E2E 限制「待 P1 关闭」与「发布是否阻断」为两个正交状态：前者开放、后者 UNKNOWN。缺口仍为：①发布政策语义 ②Gate 是否计入 ③发布负责人书面放行。禁止删历史、禁止凭空补政策、禁止 Current 写成「是/不阻断」。零业务代码、零 Scope-003。*

*追加：2026-09-23 Batch ORDER-A1 QA 限制登记（planner 会话3，来源 `docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md` §5 L-01~L-05 + `docs/quality/P1-ORDER-NUMBER-002-A1-release-gate.md` 逐条判定，均非 FAIL）：新增 **KL-069~073** 五行——KL-069=L-01 生产证据缺失（`PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`，**阻断仅限生产放行**，本地 ALLOW_LOCAL 不受阻，解阻=生产抽验 H-01/H-02/H-04，**不作为 A1 FAIL**）；KL-070=L-02 回滚窗口 API 探针缺失（git 链已核，非阻断）；KL-071=L-03 POS 数值 id→food_id 静默 null（既有缺陷 NOT_CAUSED_BY_A1，`PosOrderCreateServiceImpl:447-450` warn 不阻断插入；foodCode 路径 H-06 PASS；**已建卡 P1-POS-FOODID-MAP-001**，任务池 §22；禁标 BLOCKED_PRODUCT_RULE——业务规则已知，属缺陷修复，不进 decision-backlog）；KL-072=L-04 Scope-002/实施记录/evidence untracked（FROZEN 内容核对一致，解阻=文档入库，planner 不自行 commit）；KL-073=L-05 OrderVO 无 orderNumber + scan-serve materialConsumed 不回读（展示层，以 DB 为准）。QA 表内 UNKNOWN 行（OBS-02 全仓既有失败清单待回归轮比对）非 L-xx 编号限制，由 regression 在生产抽验/全仓跑测时处理，不单列 KL。编号自 KL-069 起（KL-068 已用于 order_number E2E 阻断）。主表 68 → 73 行。planner 仅登记，未写代码、未做正确性判断、未重开 Scope-003；历史条目零修改。*

*追加：2026-09-24 P1-POS-FOODID-MAP-001 收口限制登记（来源：任务池 §22 收口回写 + 实施报告 §12.6/§11 + `docs/quality/P1-POS-FOODID-MAP-001-qa-report.md` PASS）：**KL-071 → 已解除（2026-09-24）**——B2 fail-fast + 数据清理 food_id NULL 20→0 + QA 抽检 PASS FAIL=0；新残余三行 **KL-074=OBS-S1**（S1 完成路径 food_id 空静默跳过扣料，HEAD 既有，不启动本卡返工）、**KL-075=OBS-B2-500**（fail-fast 穿透为 HTTP 500 而非结构化 400，拒绝语义正确，展示层）、**KL-076=ENV-1**（生产侧 POS 映射未抽验，与 KL-069 同族，**阻断仅限生产放行**，本地 ALLOW_LOCAL 不受阻）。主表 73 → 76 行。编号自 KL-074 起（KL-073 已用）。planner 仅登记与状态回写，未写代码、未改 Scope-001、未 commit；历史条目零修改。*

*追加：2026-09-24 凭据治理限制登记（来源：`backend/login*.json` 清理会话 + 全仓凭据扫描 tracked/untracked/git-log 三路）：新增 **KL-077**——`dd33ee3` 历史含明文 `admin/admin123` 与根目录登录/JWT 文件，HEAD 仍 tracked e2e `.auth` JWT 与 `keystore.p12`，多处文档/脚本明文口令；清理 commit `883b639` 已落且 remote master=`883b639`，**历史明文仍可取回**；**决定不重写 git 历史**（第二步独立决策，未执行 force-push/改史）。另：A1 轮 `h00-login.json` 为 untracked 工作区残留，已删除且从未入 git log。主表 76 → 77 行。编号自 KL-077 起（KL-076 已用）。planner 仅登记，未改代码、未动 git 历史、未 commit；历史条目零修改。*

*追加：2026-09-24 P2-GOV-IGNORE-EVIDENCE-001 独立登记（来源：A1 收口 commit `c5cec24` 事后复核，非新 KL 编号、不入主表）：**问题**——`.gitignore:232` 全局 `*.txt` 规则误伤 `docs/architecture/**/evidence/`（A1 证据 `.txt` 默认被忽略，仅 `!README.txt`/`!LICENSE.txt` 豁免）；**本批处理**——commit `c5cec24` 使用 `git add -f` 逐条绕过 26 个 evidence `.txt`，未改 `.gitignore`；**后续建议（不本次执行）**——为 evidence 目录加例外规则（如 `!docs/architecture/**/evidence/**/*.txt`）。**不 amend `c5cec24`**（改 hash 破坏已落盘标识）；不影响 A1 收口事实。planner 仅登记，未改代码、未改 `.gitignore`、未动 git 历史。*

*追加：2026-09-24 P1-COMBO-ORDER-001 收口限制登记（来源：任务池 §23 收口回写 + `docs/quality/P1-COMBO-ORDER-001-qa-report.md` PASS_WITH_LIMITATION 6/6 FAIL=0 无 -R + 实施报告 §7.1/§11 + 回归 `production-regression-test.md` REG-ORDER-008~011 正式基线 121→125 PASS）：新增 **KL-078=QA L-01**（UI 浏览器目检未做——POS 套餐入口点击流 / KDS 卡片实拍未执行，代码级+API live+`vue-tsc` 已覆盖可自动化面；**不阻断本地放行**，建议回归阶段补浏览器断言，与 FOODID 卡 L-01 口径一致）、**KL-079=QA L-02**（生产证据缺失 → `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`，与 KL-069/076 同族，**阻断仅限生产放行**，本地 ALLOW_LOCAL 不受阻，解阻=生产侧抽验套餐下单/KDS 展开/出餐扣料或等价证据后 Gate 裁定，**不作本地放行 FAIL**）、**KL-080=QA L-06**（legacy 三文件 `PosApiServiceImpl`/`KitchenScanServiceImpl`/`OrderMaterialRequirementServiceImpl` 仍读旧表 `combo_ingredient`——实施报告 §7.1 划归**第二步卡**、Scope 内声明的后续工作，**非本卡缺陷、不作 P1-COMBO-ORDER-001 返工条件**）。其余限制不单列新编号：**L-03**（ENV 工作区混杂，既有债，同 KL-023 家族口径，本卡 commit `aec5c45` 恰 15 文件未夹带）、**L-04**（报告行号漂移，语义一致，DS §11.2.1 已登记，文档级）、**L-05**（soft 路径 `continue` 无日志，OBS-S1 同族 HEAD 既有 → 指向既有 **KL-074**）、**L-07**（HTTP `materialConsumed` 可能回 0，既有短路以 DB 为准 → 指向既有 **KL-073**）；**OBS-P1**（任务池无独立条目）已由任务池 §23 补建销项，不单列 KL。编号自 KL-078 起（KL-077 已用）。主表 77 → 80 行。planner 仅登记与状态回写，未写代码、未做正确性判断、未 commit；历史条目零修改，不改变既有 KL 状态与验收结论。*
