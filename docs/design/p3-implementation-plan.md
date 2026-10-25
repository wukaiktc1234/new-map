# P3 实现清单 · 影响面 · 验收映射（待 Owner 确认，**未开始写业务代码**）

> 状态：**v0.1 草案（2026-10-25）**。本文只做计划，不含实现。Owner 确认后再逐项开工。
> 前提前提：P0–P2 后端已落地（I1–I6 导入、R1–R10 跑批/差异/勾稽、S1–S9 调节表/日记账、SL1–SL11 缴款单），
> 测试数据已清空并由 `scripts/qa/seed_p3_fixture.py --reset` 可复现；断言 A1–A22 零容忍、TOTAL FAIL=0。

## 0. 全局约定（P3 全程）

| 项 | 约定 |
|---|---|
| 写 `fund_flows` / `bank_accounts.balance` | **只能**经 `AtomicBalanceUpdater#addBalanceGuarded`（乐观锁 + 下限守卫）；禁止裸 SET；禁止先 select 再 update |
| 新断言 | 只增不删，编号接 **A23** 起；观测型一律 INFO 不进 TOTAL；判定型必须 `>0` 即红 |
| 白名单/基线 | **不引入**。若发现某断言在干净数据上必然红，先报告断言设计问题 |
| 权限 | 复用既有码（`finance:bank:view/import/recon/reconcile`、`finance:deposit-slip:*`）；**新增码先报告，不自建** |
| 前端 | Vue 3 + 现有 `views/finance` 目录习惯；路由 `meta.domain='finance'` + `roles`；**每项必须跑 Playwright E2E** |
| 数据 | 验收前统一 `python scripts/qa/seed_p3_fixture.py --reset`，不手工造数 |
| 文档 | 只追加/精确替换；AC 编号接 **AC-46** 起 |

## 1. 第一项：财务对账工作台（前端）

**缺口**：R1–R10 已可用，但财务只能用接口/SQL —— 「算对」还没变成「在用」。

### 1.1 实现清单
| # | 内容 | 文件（新增） |
|---|---|---|
| 1.1 | API 层：跑批/差异/勾稽/调节表封装 | `frontend/src/api/finance-recon.ts` |
| 1.2 | 类型：VO/DTO 与后端字段一一对应 | `frontend/src/types/finance-recon.ts` |
| 1.3 | 三区工作台页（顶部摘要 / 左队列 / 右详情） | `frontend/src/views/finance/recon/ReconWorkbench.vue` |
| 1.4 | 摘要卡（笔数/金额/差异条数+金额/较上次是否收敛） | `.../components/ReconSummaryCard.vue` |
| 1.5 | 队列表格（状态标签、筛选、7 天默认窗口、分页） | `.../components/ReconQueueTable.vue` |
| 1.6 | 详情面板（内部/外部并排 + 差异类型 + 建议动作 + 审计轨迹） | `.../components/ReconDetailPanel.vue` |
| 1.7 | 处置对话框（claim→resolve，**reason 必填**、WRITE_OFF/EXEMPT/REVERSE 三选一） | `.../components/ReconResolveDialog.vue` |
| 1.8 | 路由注册（`/finance/recon`，`domain:'finance'`，`roles:[FINANCE_DIRECTOR, FINANCE_MANAGER, AUDITOR]`）+ 侧边栏入口 | `frontend/src/router/index.ts`（精确追加） |

### 1.2 复用的后端接口（**不新增后端**）
`POST /v1/finance/recon-runs`（R1）、`GET /v1/finance/recon-runs`（R2）、`GET /v1/finance/recon-runs/{id}`（R3）、`GET /v1/finance/recon-items`（R5）、`POST /recon-items/{id}/match|unmatch`（R6/R7）、`GET /v1/finance/recon-differences`（R8）、`POST .../claim`（R9）、`POST .../resolve`（R10）。

### 1.3 影响面
| 维度 | 内容 |
|---|---|
| 表 | **不涉及**（只读 + 既有写接口；无新表、无 DDL） |
| 写路径 | 前端不直接写库；resolve 写入 `recon_differences` + `finance_audit_logs`，由后端事务保证 |
| 权限 | 读：`finance:bank:view`；写（跑批/认领/核销/勾稽）：`finance:bank:recon`；**不新增码** |
| 事务边界 | 后端既有：R1 跑批单事务；R9/R10 各自事务；前端只负责一次调用一次刷新的语义 |
| 风险 | ① 差异池量大（当前种子数据 0，真实数据上千）→ 必须服务端分页 + 默认 7 天窗；② 「无声更改」→ 处置对话框强制 `reason` 非空、详情面板展示 `resolution_by/time/remark` 与审计时间线 |

### 1.4 验收映射
| AC | 断言 | 方法 |
|---|---|---|
| AC-46（新） | 工作台摘要的差异条数/金额 = `GET /recon-runs/{id}` 的 `difference_count` 与差异池合计 | E2E + 接口对拍 |
| AC-47（新） | 完整链路「差异池 → 认领 → 核销 → 状态变化 → 审计出现」在页面可走通 | E2E |
| AC-48（新） | 处置未填 reason → 前端阻塞提交（不发请求） | E2E |
| AC-7/AC-13（既有） | 幂等与 claim→resolve 语义回归（后端不变） | 既有脚本 |

### 1.5 E2E
`scripts/qa/e2e_recon_workbench.py`（Playwright，复用 `ui_e2e_payment.py` 的登录/截图/断言骨架）：登录 → 打开工作台 → 断言摘要数字与接口一致 → 选一条差异 → 认领 → 核销（填 reason）→ 断言状态标签变化 → 断言审计时间线出现该条 → 截图。

## 2. 第二项：门店对账视图 + 平台佣金/手续费可见

**缺口**：门店只能被动接收；手续费差异淹没在差异池里。

### 2.1 实现清单
| # | 内容 | 位置 |
|---|---|---|
| 2.1 | 后端只读接口：`GET /v1/finance/store-recon/daily?storeId&date` → 该店当日 `expected/actual/difference/remark` + 缴款单状态 | 新增 `StoreReconController`（**只读，无写路径**） |
| 2.2 | 前端门店视图（店长可看本店；财务可切店） | `frontend/src/views/finance/recon/StoreReconView.vue` |
| 2.3 | 手续费/佣金维度：在差异池增加 `differenceType=CHANNEL_FEE` 筛选 + 按渠道汇总卡（笔数/金额） | `ReconWorkbench` 增组件 `ChannelFeePanel.vue` |

### 2.2 影响面
| 维度 | 内容 |
|---|---|
| 表 | 不新建；只读 `deposit_slips`、`bank_transactions(fee)`、`recon_differences(type=CHANNEL_FEE)` |
| 权限 | 门店视图：**复用** `finance:deposit-slip:view`（店长已有，数据范围按 `store_id` 过滤）；渠道汇总：`finance:bank:view` |
| 数据范围 | **必须**按登录用户 `store_id` 过滤；跨店需 `FINANCE_*` 角色；无 `store_id` 的用户直接 403（不返回默认值） |
| 风险 | 数据范围越权是本期最大风险 → 服务端强制过滤，E2E 用两个不同门店账号对拍 |

### 2.3 验收映射
| AC | 断言 |
|---|---|
| AC-49（新） | 店长 A 只能看到 A 店当日数据；访问 B 店 → 403 |
| AC-50（新） | `CHANNEL_FEE` 汇总的笔数/金额 = SQL `recon_differences` 对应聚合 |

### 2.4 E2E
`scripts/qa/e2e_store_recon.py`：店长登录 → 门店视图断言三值（expected/actual/difference）与接口一致 → 切到非本店 → 断言 403 提示 → 财务登录 → 渠道汇总与 SQL 对拍。

## 3. 第三项：调节表闭环收口（生成 → 提交 → 审核 → 导出）

**缺口**：S3–S9 可用，但缺少「四类下钻、四眼可见、可导出单据」三个使用闭环。

### 3.1 实现清单
| # | 内容 | 位置 |
|---|---|---|
| 3.1 | 四类未达账项**下钻**：点 A/B/C/D 显示 `reconciliation_items` 明细 + 引用侧原记录 | 前端 `StatementDetail.vue` + 复用 `GET /recon/statements/{id}` |
| 3.2 | 四眼门禁 UI 可见：`submitted_by == 当前用户` → 审核按钮禁用并提示；`ROLE_TELLER` → 同样禁用并提示 | 前端权限判断（**以后端 409 为最终依据**） |
| 3.3 | 调节表导出：后端已有 `GET /recon/statements/{id}/export`（CSV）→ 前端下载按钮；**额外**生成可打印版（浏览器打印样式） | 前端 + 路由 |
| 3.4 | 《调节表》打印模板（表头/期间/四类明细/两侧余额/审核人签字栏） | `.../components/StatementPrintView.vue` |

### 3.2 影响面
| 维度 | 内容 |
|---|---|
| 表 | 不新建；写 `reconciliation_statements`（submit/approve）沿用现有事务与 V5/V8/A19 门禁 |
| 权限 | `finance:bank:recon`（生成/提交）、`finance:bank:reconcile`（审核，仅 FD） |
| 事务 | 后端既有；前端不得「先本地改状态再调接口」 |
| 风险 | ① 门禁后置体验问题 → 前端前置判断 + 后端仍强校验（双保险，后端为准）；② 打印版数字必须来自接口原值，**不得前端重算** |

### 3.3 验收映射
| AC | 断言 |
|---|---|
| AC-51（新） | 四类下钻条数/金额 = `reconciliation_items` 按 category 聚合 |
| AC-52（新） | 自审 → 前端按钮禁用 + 后端 409（两层都验） |
| AC-44/AC-43/AC-45（既有） | V8/V6/不调账回归 |

### 3.4 E2E
`scripts/qa/e2e_statement_flow.py`：生成 → 四类下钻对拍 → 提交（qa_admin）→ 自审被禁用 → 换 qa_approver 审核通过 → 导出/打印页字段断言。

## 4. 第四项：总部资金总览看板（只读聚合）

**缺口**：老板/财务主管没有「今天先看哪家店」的入口。

### 4.1 实现清单
| # | 内容 | 位置 |
|---|---|---|
| 4.1 | 后端聚合接口 `GET /v1/finance/overview/daily`（三段：门店现金户余额与次日净变动 / 渠道到账 vs SALE 差额 / 未关闭调节表） | 新增 `FinanceOverviewController`（只读） |
| 4.2 | 前端看板页（三卡 + 门店表格，异常标红） | `frontend/src/views/finance/recon/FundOverview.vue` |

### 4.2 影响面
| 维度 | 内容 |
|---|---|
| 表 | 不新建；只读 `bank_accounts`、`fund_flows`、`recon_runs`、`reconciliation_statements` |
| 权限 | `finance:bank:view`；跨店聚合要求 `FINANCE_DIRECTOR/FINANCE_MANAGER/AUDITOR`（店长只看本店，或直接不开放） |
| 风险 | 聚合查询性能 → `fund_flows(account_id, business_date)` 已有索引，必要时限制日期窗（默认今天 + 近 7 天） |

### 4.3 验收映射
| AC | 断言 |
|---|---|
| AC-53（新） | 看板三段数字与对应 SQL 聚合逐一相等（逐字段对拍，不用截图目测） |
| 新增 **A23（INFO）** | 「未关闭调节表最长挂账天数」> 阈值（默认 7 天）时观测输出，不进 TOTAL |

### 4.4 E2E
`scripts/qa/e2e_fund_overview.py`：登录 → 看板 → 三段数字与 SQL 对拍 → 断言负余额/异常账户标红样式存在。

## 5. 第五项：技术债收口（不单独立项，随前端项完成）

| 项 | 内容 | 影响面 | 验收 |
|---|---|---|---|
| **BL-9** 时区偏移 | `CsvImportParser` 支持 `+08:00` / `Z`，统一换算 Asia/Shanghai；解析失败仍走 `TXN_TIME_INVALID` | 只动导入解析器；不改表结构 | AC-2 回归 + 新 AC-54（带偏移样本导入后 `txn_time` 正确） |
| **BL-5** 号段烧号 | 取号与 `fund_flows` 插入同事务，或失败记「烧号账」；目标：连续两次失败不产生永久缺号 | 动 `FundFlowServiceImpl` 取号路径（**不碰 call-number 的号段工具**，BL-2 仍需另放行） | 新增 **A24（判定）**：当天 FLOW 号段「取值 - 已落库条数」= 0（烧号数），>0 即红 |

> 注：A24 是判定项，会进入 TOTAL。若 Owner 认为历史洞容忍，可降级为 INFO —— **请裁决**。

## 6. 建议执行顺序与验收门

| 阶段 | 内容 | 完成门 |
|---|---|---|
| P3-1 | 工作台前端（第 1 项） | 4 条 AC + E2E 通过 + consistency 全绿 |
| P3-2 | 门店视图 + 手续费（第 2 项） | 2 条 AC + E2E + 数据范围对拍 |
| P3-3 | 调节表闭环（第 3 项） | 3 条 AC + E2E + 打印页字段对拍 |
| P3-4 | 资金总览（第 4 项） | 1 条 AC + A23 + E2E 逐字段对拍 |
| P3-5 | BL-9 / BL-5 | AC-54 + A24 + 导入回归 + 号段连续性实测 |

每阶段结束都必须：`consistency_check.py` **TOTAL FAIL=0 / exit 0**、`ui_e2e_payment.py` 7/7 不破、`ahead=0`。

## 7. 不在 P3 范围（明确排除）
- 期末结账 / 反结账（新模块）
- 退款侧勾稽（TD-31）
- **BL-2 三处发号器收敛**（需 Owner 单独放行 call-number 文件）
- 恢复 `fixture-reverse` 端点（已删除）

## 8. 待 Owner 确认的 3 个点
1. **AC 编号**：本计划新增 AC-46～AC-54（9 条），是否同意并入设计稿 §8？
2. **A24（烧号判定）**：判定项（>0 即红）还是 INFO 观测？
3. **第 4 项看板的可见范围**：店长是否可见（只看本店），还是仅 FINANCE/AUDITOR？