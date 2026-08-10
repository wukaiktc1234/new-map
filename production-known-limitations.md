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
| KL-001 | BLOCKED_PRODUCT_RULE 待产品确认项（4 项） | PD-001~PD-004 → P0-FIN-001 / P0-FIN-002 / P0-HR-002（报名子项）/ P0-HR-005 | 高 | 是 | 待产品 |
| KL-002 | 财务科目期初结转规则未定义 | PD-003 → P0-FIN-001（余额聚合公式无法落地） | 高 | 是 | 待财务 |
| KL-003 | 演示 mock 数据源残留（HR 智能模块） | P0-HR-005（Sprint-2 卡）/ 任务池 P1-MOCK-005 | 中 | 否（有条件：生产保留入口即建议阻断） | 待产品决策 |
| KL-004 | 排班真实接口未接入 | P0-HR-012（Sprint-2 卡）/ 任务池 P1-MOCK-002 / P0-HR-011 | 中 | 否（入口已禁用，功能暂不可用） | 待后端排期 |
| KL-005 | 部分展示字段硬编码（下拉/字典） | 任务池 P1-DATA-001~010、P2-DATA-004（3.3/3.4 字典治理） | 低 | 否 | 待治理 |
| KL-006 | /workspace/material-request 域级并集超放行（菜单隐藏 URL 可直达） | P0-SEC-004（Sprint-3A 卡，QA 限制 L-1） | 低 | 否 | 待治理 |
| KL-007 | 渗透用例（伪造 token 被拒）未执行；JWT 读取死代码残留 | P0-SEC-006（Sprint-3A 卡，QA 限制 L-2） | 中 | 否 | **已关闭（2026-08-10：P1-SEC-009 活体渗透 PASS，伪造 token 被后端拒绝等 5 目标 HTTP 实测通过）** |
| KL-008 | 权限码静态校验脚本缺失（注册表漂移无法自动拦截） | P1-SEC-003（Sprint-3A 卡，QA 限制 L-3） | 低-中 | 否 | 已拆卡（P2-SEC-007） |
| KL-009 | 非 admin 按钮可见性依赖 role_permission 表数据，代码层无法证明 | P1-SEC-003（Sprint-3A 卡，QA 限制 L-4） | 低-中 | 否 | 待联调 |
| KL-010 | /forgot-password 路由基线即缺（老问题，非本次引入） | P1-SEC-002（Sprint-3A 卡，QA 观察 O-1） | 低 | 否 | 已拆卡（P2-SEC-008） |
| KL-011 | 401 防重标记 finally 重置窗口极短 | P1-SEC-004（Sprint-3A 卡，QA 观察 O-2） | 低 | 否 | 观察（不拆卡） |
| KL-012 | CostAnalysisController 类级 cost:view 与导出码 cost:export 不一致 | P0-SEC-001 范围（Sprint-3A 第二阶段，QA 观察 O-3） | 低-中 | 否 | 随 P0-SEC-001 |
| KL-013 | 真实登录联调（登录→/me→路由跳转→按钮渲染全链路）未在运行环境执行（/me 权限源端到端验证缺口） | P0-SEC-006（Sprint-3A 卡，QA 复审限制 L-5） | 中 | 否 | 待联调（回归阶段补，与 KL-009 同批） |
| KL-014 | 后端 4 文件 admin 权限码清单不一致（AdminPermissions 多 finance:tax:* 4 码 + purchase:order:* 6 码，未同步其余 3 个发放文件） | P1-SEC-003（Sprint-3A 卡，QA 复审限制 L-6 / 观察 O-5） | 低 | 否 | 随 P0-SEC-001（统一 4 文件清单） |
| KL-015 | 前端注册表超范围新增 purchase:order:terminate/freeze + cancel 描述修正（良性，与页面使用点对齐） | P1-SEC-003（Sprint-3A 卡，QA 复审观察 O-4） | 低 | 否 | 观察（记录，不拆卡） |
| KL-016 | system:init:* 权限码不在前端注册表（前后端权限码跨端不一致） | P0-SEC-005 关联（Sprint-3A 卡，QA 复审观察 O-6） | 低 | 否 | 随 P0-SEC-001（标注关联） |
| KL-017 | SmartRestock「建议记录」Tab 5 条硬编码假历史（双汇/益海嘉里假供应商名）仍在页面可见；handleSuggestionSubmit 仅本地无持久化 | P1-DATA-002（Sprint-3B-1 卡，QA 限制 L-1） | 中 | 否（有条件：3B-2 完成前发布需评估，页面仍可见假数据） | **已解除（3B-2 P1-MOCK-004 承接闭环，2026-08-10）** |
| KL-018 | AI 服务商字典接口后端缺失（无字典接口）+ 统计卡 todayCalls/successRate 显示 0（无统计接口） | P1-DATA-008（Sprint-3B-1 卡，QA 限制 L-3） | 中 | 否 | 待后端排期（同 KL-005 类治理；排期前前端可将 0 改 `--` 防误导） |
| KL-019 | 会员统计 3 字段口径缺失（consumeRatio/avgOrderAmount/repurchaseRate）：无文档化计算口径 → 临时移除、前端 `--` 降级；types/member.ts 类型未对齐 | P1-DATA-009（Sprint-3B-1 卡，QA 限制 L-4） | 低-中 | 否（已降级移除，无假数据） | 待产品/财务定义口径（PD-005）；恢复字段前禁止猜测实现 |
| KL-020 | 运行期联调未执行（无 DB 环境）：DATA-009 聚合正确性、DATA-010 切换刷新未做浏览器级验证 | P1-DATA-009/010（Sprint-3B-1 卡，QA 验证缺口 V-1/V-2） | 中 | 否（代码侧已 PASS，验证缺口） | 待联调（回归阶段闭环，与 KL-009/KL-013 同批） |
| KL-021 | InventoryAdjust 会签状态展示为本地 mock（"已签/待签"非后端真实状态） | P1-DATA-003（Sprint-3B-1 卡，QA 限制 L-2） | 低 | 否（仅展示层，不影响提交数据） | 待治理（后端补会签状态字段或移除状态展示） |
| KL-022 | DATA-010 无组织切换 UI（setOrg 零调用），仅 DecisionBoard 门店切换入口 | P1-DATA-010（Sprint-3B-1 卡，QA 限制 L-5） | 低 | 否（机制已就绪，功能范围问题） | 待产品决策（组织切换 UI 属产品功能范围；非规则缺失，不进 PD 队列） |
| KL-023 | 3B-1 批次无法按 commit 分离（工作区无提交），16 文件 diff 混入跨批次 UI 改动（teleported/lock-scroll） | P1-DATA-001~010（Sprint-3B-1 卡，QA 限制 L-6） | 低 | 否 | 工程流程项（建议本 Sprint 验收后按模块分批 commit，便于回归归因） |
| KL-024 | InventoryOutbound 假供应商列表残留（双汇/益海嘉里/蒙牛硬编码，页面可见假名） | QA 观察 O-2（Sprint-3B-1，未拆卡遗留） | 低-中 | 否（有条件：假数据可见，建议 3B 收口前完成） | 已拆卡 P1-DATA-011（3B-2 未执行，建议并入 3B-3 窗口） |
| KL-025 | SignLinkManagement handleRenew/handleDelete 无 API 直接 success；handleSubmit 载荷缺 linkName/supplierId、templateId 写 eContractId 语义待核对 | QA 观察 O-3（Sprint-3B-1，P1-API-001 范畴既有） | 低 | 否 | 挂 P1-API-001 既有范畴（随收尾批次评估，不拆新卡） |
| KL-026 | InventoryLocation getByLocationId 失败回退 Mock | QA 观察 O-4（Sprint-3B-1，P1-API-003 范畴既有） | 低 | 否 | 挂 P1-API-003 既有范畴（随后端排期） |
| KL-027 | HRContract 编辑 probationMonths: 3 硬编码覆盖 | QA 观察 O-5（Sprint-3B-1，P0-DATA-008 既有问题） | 低 | 否 | 随 P0-DATA-008 独立收尾批次 |
| KL-028 | MemberOverview 等级分布/客户分层/增长 Tab 依赖 overview 接口未返回的分布字段 → 空展示 | QA 观察 O-6（Sprint-3B-1，P1-DATA-009 关联） | 低 | 否（空展示非假数据，既有降级） | 观察（关联 KL-019/overview 后端字段扩展） |
| KL-029 | SmartRestock 采购申请运行期联调未执行（真实 POST 落库/采购侧可见未验证） | P1-MOCK-004（Sprint-3B-2 卡，QA 验证缺口 V-1） | 中 | 否（代码侧已 PASS，验证缺口） | 待联调（回归阶段闭环，同 KL-013/KL-020 联调类） |
| KL-030 | 导出 CSV 文件内容运行期验证未执行（10 页生成逻辑代码级通过，未做下载文件内容解析） | P1-EXPORT-002（Sprint-3B-2 卡，QA 验证缺口 V-2） | 中 | 否（代码侧已 PASS，验证缺口） | 待联调（回归阶段闭环） |
| KL-031 | TopNavbar 通知已读/全部已读运行期联调未执行（端点/实体契约代码级通过，未做真实登录态验证） | P1-MOCK-010（Sprint-3B-2 卡，QA 验证缺口 V-3） | 中 | 否（代码侧已 PASS，验证缺口） | 待联调（回归阶段闭环，与 KL-029/KL-030 同批） |
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

> 状态字典：`待产品` / `待财务` / `待产品决策` / `待后端排期` / `待后端组` / `待联调` / `待渗透` / `待治理` / `观察` / `已拆卡` / `已解除`

---

## 2. 明细

### KL-001｜BLOCKED_PRODUCT_RULE 待产品确认项（4 项）
- **来源任务**：PD-001 缴税重复缴纳（→P0-FIN-002）、PD-002 培训重复报名（→P0-HR-002 报名子项）、PD-003 科目期初结转（→P0-FIN-001）、PD-004 HR 智能模块页面处置（→P0-HR-005）
- **风险等级**：高
- **是否阻断发布**：**是**（PD-001/PD-003 涉资金与合规，上线前必须决策；PD-004 涉生产展示虚构数据）
- **处置**：等待产品/财务负责人决策，决策后回写对应任务卡验收标准（见 `product-decision-backlog.md` 决策流转）；规则落地前不得实现猜测业务逻辑。

### KL-002｜财务科目期初结转规则未定义
- **来源任务**：PD-003 → P0-FIN-001
- **风险等级**：高
- **是否阻断发布**：**是**（余额聚合公式（期初 + 借方 − 贷方）中的期初余额来源依赖结转规则，规则未定义则假账修复无法收口）
- **处置**：财务负责人确认结转方式（如：期初余额取数口径、结转周期、历史凭证追溯范围）后，P0-FIN-001 聚合逻辑方可放行。

### KL-003｜演示 mock 数据源残留（HR 智能模块）
- **来源任务**：P0-HR-005（HR 智能 3 页面 100% Mock：员工画像/知识库智能/合同智能看板）
- **风险等级**：中
- **是否阻断发布**：否（有条件）——若生产保留入口展示虚构员工/绩效/学习数据，建议按"阻断"处理；决策前至少隐藏入口或加"演示数据"标识
- **处置**：产品决策"下线 vs 接真实 vs 演示标识"（PD-004）后执行。

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

### KL-031｜TopNavbar 通知已读运行期联调未执行（V-3）
- **来源任务**：P1-MOCK-010（Sprint-3B-2 卡，QA 验证缺口 V-3，`docs/quality/sprint-3b2-qa-report.md` §4.1 及 R1 复验节）
- **风险等级**：中（验证缺口，非活跃缺陷——`api/notification.ts` 端点（GET /notifications、PUT /{id}/read、PUT /read-all）与后端 `NotificationController.java:435-508` 逐一匹配；实体契约 `Notification.java` isRead(Integer 0/1)/id(notification_id) 与前端 `read: isRead===1` 一致；仅缺真实登录态接口验证）
- **是否阻断发布**：否（代码侧已 PASS；P1-MOCK-010 已 R1 复验整卡 PASS）
- **现状**：TopNavbar 3 条假通知已删除，接真实 API；未做运行环境登录态验证
- **处置**：回归阶段闭环（回归 Agent 职责，与 KL-029/KL-030 同批），验证结果回写 P1-MOCK-010 收口；不拆卡

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

---

## 3. 发布决策摘要

| 场景 | 结论 |
|------|------|
| KL-001 + KL-002 未决策 | **阻断上线**（资金真实性/合规无法收口） |
| KL-003 未决策且保留入口 | 建议按阻断处理（生产展示虚构数据） |
| KL-004 已禁用入口 | 不阻断，功能暂不可用 |
| KL-005 | 不阻断，属治理项 |
| KL-006 | 不阻断，低敏感治理项（8 角色可达低敏提报页） |
| KL-007 | ✅ **已关闭（2026-08-10）**——P1-SEC-009 渗透验证 PASS（活体 HTTP 实测：篡改 token → 401×3、未知角色/无权限码 → 403×3、过期 → 401×2，14 用例全 PASS）；渗透门禁前置项关闭，不再要求"发布前必须完成" |
| KL-008 | 不阻断（当前无活跃漂移）；已拆卡 P2-SEC-007 治理 |
| KL-009 | 不阻断；联调造数确认后收口 P1-SEC-003 |
| KL-010 | 不阻断（老问题）；已拆卡 P2-SEC-008，完整流程需产品决策 |
| KL-011 | 不阻断，仅观察 |
| KL-012 | 不阻断（权限过严非越权）；随 P0-SEC-001 统一 |
| KL-013 | 不阻断（前端已 PASS，验证缺口）；联调验证随回归阶段补（与 KL-009 同批执行），发布前收口 |
| KL-014 | 不阻断（运行期无权限差异，维护性问题）；随 P0-SEC-001 统一 4 文件清单 |
| KL-015 | 不阻断，仅记录（良性超范围，与页面使用点一致） |
| KL-016 | 不阻断（P0-SEC-005 已 PASS）；随 P0-SEC-001 统一前后端权限码（标注关联） |
| KL-017 | 不阻断（有条件：残留假历史已挂 P1-MOCK-004，3B-2 执行中；3B-2 完成前发布需评估） |
| KL-018 | 不阻断（0 值已注释说明，字典缺失已如实登记、未假造接口）；随后端排期 |
| KL-019 | 不阻断（3 字段已降级移除，无假数据）；字段恢复依赖 PD-005 口径决策，恢复前禁止猜测实现 |
| KL-020 | 不阻断（代码侧已 PASS，验证缺口）；联调随回归阶段闭环（与 KL-009/KL-013 同批），发布前收口 |
| KL-021 | 不阻断（仅展示层 mock，不影响提交数据）；登记治理 |
| KL-022 | 不阻断（机制已就绪，UI 属产品功能范围）；待产品排期 |
| KL-023 | 不阻断，工程流程项（建议后续按模块分批 commit） |
| KL-024 | 不阻断（有条件：假数据可见，已拆卡 P1-DATA-011，建议 3B 收口前完成） |
| KL-025 | 不阻断（挂 P1-API-001 既有范畴，已验收含限制） |
| KL-026 | 不阻断（挂 P1-API-003 既有范畴，随后端排期） |
| KL-027 | 不阻断（随 P0-DATA-008 独立收尾批次） |
| KL-028 | 不阻断（空展示非假数据，既有降级）；仅观察 |
| KL-029 | 不阻断（代码侧已 PASS，验证缺口）；联调随回归阶段闭环（与 KL-009/KL-013/KL-020 同批），发布前收口 |
| KL-030 | 不阻断（代码侧已 PASS，验证缺口）；CSV 内容解析验证随回归阶段闭环，发布前收口 |
| KL-031 | 不阻断（代码侧已 PASS，验证缺口）；通知已读联调随回归阶段闭环（与 KL-029/KL-030 同批），发布前收口 |
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

*初始创建：2026-08-10。本文件仅记录已知限制，未修改任何代码，未改变 Sprint-2 验收结论。*
*追加：2026-08-10 登记 Sprint-3A 第一阶段 QA 限制项（KL-006~012，来源 `docs/quality/sprint-3a-phase1-qa-report.md` L-1~L-4 / O-1~O-3），不改变 Sprint-3A 第一阶段验收结论（PASS 6 / PASS_WITH_LIMITATION 2 / FAIL 0）。*
*追加：2026-08-10 复审登记（KL-013~016，来源 `docs/quality/sprint-3a-security-qa-report.md` 复审 L-5 / L-6(O-5) / O-4 / O-6），复审结论 PASS 5 / PASS_WITH_LIMITATION 3 / FAIL 0（FAIL=0 不阻断；3 项 PWL 明细见 §3 复审后评估），不改变 Sprint-3A 第一阶段验收结论（8/8 通过）。*
*追加：2026-08-10 登记 Sprint-3B-1 QA 限制/验证缺口/观察项（KL-017~028，来源 `docs/quality/sprint-3b1-qa-report.md` L-1~L-6 / V-1~V-2 / O-1~O-6），3B-1 结论 PASS 4 / PASS_WITH_LIMITATION 5 / FAIL 0（FAIL=0 不阻断）。L-1 并入 P1-MOCK-004 承接（3B-2 执行中）；O-2 拆卡 P1-DATA-011；P1-DATA-009 口径缺失登记 PD-005（关联 KL-019，不阻断当前发布）。不改变 Sprint-3A 及此前全部验收结论。*
*追加：2026-08-10 登记 Sprint-3B-2 QA 限制/验证缺口/观察项（KL-029~042，来源 `docs/quality/sprint-3b2-qa-report.md` V-1~V-3 / O-1~O-8 / 待后端接口清单），3B-2 结论 PASS 2 / PASS_WITH_LIMITATION 3 / FAIL 1 → R1 复验 PASS（FAIL=0 不阻断）。KL-017 状态更新为已解除（L-1 由 P1-MOCK-004 承接闭环）；KL-024 排期更新为并入 3B-3 窗口（3B-2 未执行）；O-4 拆卡 P1-DATA-012。V-1~V-3 联调缺口回归阶段闭环，发布前收口。不改变 Sprint-3A/3B-1 及此前全部验收结论。*
*追加：2026-08-10 登记 P0-SEC-001 子批 C/D 相关（KL-043：设计公开端点豁免清单未文档化，供扫描脚本豁免参考）；追加 P0-SEC-001 子批推进评估（A ✅ QA PASS_WITH_LIMITATION；C/D ✅ 已提交待 QA；B 🔄 执行中；BLOCKED 候选 30 方法 + 6 端点登记 PD-006~008，不纳入通过数）。不改变 Sprint-3A/3B 及此前全部验收结论。*
*追加：2026-08-10 整卡收口登记（架构宣布 P0-SEC-001 整卡 PASS_WITH_LIMITATION，A/B/C/D 全过 FAIL=0）：范围外 17 登记 KL-044~046（ApprovalWorkflow 13 → PD-009；QuickStockIn 3 → 拆卡 P2-SEC-010；MiniProgram 1 → 豁免成立并入 KL-043，34→35）；KL-043 补录 /v1/mp/**（待架构核对）。排除项闭环路径见 §3 整卡收口评估。不改变 Sprint-3A/3B 及此前全部验收结论。*
*追加：2026-08-10 P1-SEC-009 收口登记（来源 `docs/quality/p1-sec-009-qa-report.md`，qa PASS）：KL-007 状态更新为**已关闭**（P0-SEC-006 L-2 渗透验证活体 PASS：伪造 token 401×3 / 未知角色 403×3 / 过期 401×2 等 14 用例全 PASS）；新增 KL-047（QA 观察 O-1：退款审批 approveUserId 信任客户端参数，审批归属业务规则）→ 登记 PD-010 待产品决策；O-2（401 文案）仅记录不拆卡。发布决策摘要追加 P1-SEC-009 收口评估（渗透门禁前置项已关闭；KL-013/KL-020 联调缺口仍为发布前收口待办）。不改变 Sprint-3A/3B 及此前全部验收结论。*
