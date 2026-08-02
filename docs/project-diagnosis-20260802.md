# 项目诊断文档 — 连锁模式 C0 基线

- 日期：2026-08-02
- 依据：`docs/business-mode-design.md`（批 C0~C4 定义）、`docs/data-chain-fix-tracking.md`（批 1~23）
- 目标：批 C0 = 梳理标准连锁全量能力点清单（capability-registry）+ 菜单归属 + 数据契约完整性；本文档为 C0 开工前诊断

## 1. 商业模式基线（摘录自 business-mode-design.md）

- 双模式：连锁模式 = 能力超集（全量基线），单店模式 = 从超集裁剪投影
- C0：capability-registry 能力点清单 + 菜单归属 + 数据契约完整性
- C2：visibleModes + 静态校验 + 守卫检查
- C4：Playwright 端到端验证

## 2. 能力点清单现状

| 项目 | 状态 |
|---|---|
| capability-registry（前端静态注册） | **不存在**，C0 待建 |
| 菜单注册 | 已有 `src/modules/*/menu.ts`（16 模块）+ `stores/permission.ts` registerMenuGroup/getVisibleMenus；无 visibleModes、无模式可达性检查 |
| 能力点清单实际形态 | `docs/alignment/` 22 份矩阵（2026-07-30 ~ 07-31 生成），即"能力点×动作×状态×字段×权限"的权威参考 |

`docs/alignment/` 矩阵分布：

| 域 | 矩阵 | 最新时间 |
|---|---|---|
| 采购 | purchase-action-matrix / purchase-state-machine / purchase-field-matrix / purchase-permission-matrix | 07-30~31 |
| 门店订单 | store-order-*（5 份） | 07-31 |
| 财务 | finance-*（4 份） | 07-31 |
| 仓储追溯 | warehouse-trace-*（4 份） | 07-31 |
| 人事资产 | hr-asset-*（4 份） | 07-31 |
| 报告 | missing-pages-report / menu-title-consistency-report | 07-31 |

> 注：`docs/spec/17-采购管理模块设计规格.md`（06-27）与 `docs/验收清单/系统验收清单.md`（07-05）已被用户确认为旧文件，本诊断不再以它们为准（且 spec/17 为 INDEX 未引用的孤儿规范）。

## 3. 业务链数据契约审计（后端，批 23 后实况）

| 环节 | 现状 | 契约状态 |
|---|---|---|
| 组织 | **无 organizations 表**，仅 departments | 缺口：连锁多组织层级缺失 |
| 门店 | stores_new 完整 | 完整 |
| 开业准备 | **环节缺失**（无表无 API） | 缺口：连锁标准流程断环 |
| 商品/菜品 | dish 体系齐全 | 完整 |
| POS 订单 | 写 `orders_legacy`（MERCHANT_ID=storeId），POS/门店订单/日结三端同轨 | 自洽 |
| 日结 | daily_settlement 读 orders_legacy 聚合，自洽；但**状态枚举三处不一致** | 基本自洽 |
| 班次 | 5 个 API 前端零接入；统计查询不存在的 `pos_orders` 表 | **断链** |
| 追溯 | 双轨：food_trace_codes vs 业务表 material_trace_code/food_trace_code | 待统一 |
| 成本 | cost_records vs dish-cost 双口径 | 待统一 |

## 4. POS 端断链明细

- 登录接口不返回 storeId，前端丢弃 shiftInfo
- 桌面订单兜底 storeId=STORE_A（PosOrderCreateServiceImpl L161-166）
- 桌台订单 createTableOrder 不设置 storeId

## 5. 采购域：矩阵 vs 批 23 后代码差异

已修复（代码已符合矩阵）：
- 订单持久化字段（LK-PURCHASE-03）、到货质量/确认、计划 executePlan、订单独立状态 STATUS_REJECTED=7

残留差异：
| 差异 | 矩阵 | 代码实况 |
|---|---|---|
| 订单状态近似映射 | 独立状态机 | received/terminated 与后端映射仍为近似（前端 11 态 ↔ 后端 7 态） |
| 订单终止 | 有 action | 无终止端点 |
| 审批权限 | 待定案 | approve/reject 现为 `hasAnyRole('admin','finance_manager')`，连锁模板下角色模型未定 |

## 6. 文档清理计划（本次已执行/建议）

很老（2025 年度）——已删除：
- `docs/archive/` 整目录（110 文件，2025-11-27 ~ 2026-06-27，全仓无引用）
- `docs/dev/development.md`（2025-11-17）、`docs/dev/development-guide.md`（2025-12-01）

过时输出文件——已删除：
- `docs/验收清单/scan-result*.txt`（4 份）、`docs/smoke-test-last-output.txt`
- 根目录 smoke-test-results.* / ui-acceptance-results.*（7-04~07-05 运行输出）

已确认旧、本次删除：
- `docs/spec/17-采购管理模块设计规格.md`（06-27 孤儿规范）
- `docs/验收清单/系统验收清单.md`（07-05，V1 阶段）

保留（可能复用）：
- `docs/验收清单/` 下 3 个测试脚本（full-scan-test / smoke-test-runner / ui-acceptance-api-test）与 V1 报告（归档用）
- `docs/spec/00-24` 规范体系、`docs/alignment/` 矩阵、7-30 后全部文档

## 7. 建议实施顺序（C0）

1. 以 `docs/alignment/` 矩阵为底稿，产出能力点清单骨架 → 按采购域试点（能力点 13 项清单已在会话中列出：供应商/申请/计划/订单/入库/退货/评价/对账/付款/统计/预警/权限/追溯关联）
2. 补数据契约缺口（优先级：开业准备 > 组织 > 班次 > 追溯 > 成本）
3. 修 POS 断链（storeId 下传）
4. 采购域残留差异收口（终止端点、审批角色定案）
5. 逐域展开 → C2（visibleModes 静态校验）→ C4（Playwright）

## 8. 待用户确认的问题

1. 能力点清单骨架的落盘位置与格式：新建 `docs/capability-registry.md`（承接 alignment 矩阵）？
2. 采购审批角色定案：连锁模板下 approve/reject 归属（admin 全量 / 按门店域隔离 / 财务经理兼任）？
