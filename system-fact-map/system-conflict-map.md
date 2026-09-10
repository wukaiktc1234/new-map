# System Conflict Map（冲突图）

> - 从 `system-fact-map-v1.md` §9 + 全文 CONFLICT 标记提取。
> - 按 裁决已定 / 需产品决策 / 技术整改 三类分组。
> - 每项标注责任层（Domain/API/UI/DB）。

---

## 一、裁决已定（VERIFIED / CONFLICT resolved）

| # | 冲突点 | 涉及域 | 结论 | 责任层 | PADR/决策 |
|---|---|---|---|---|---|
| C-01 | 可售口径 | 产品/库存/订单 | POS 可售=菜品层 foods.stock；物料库存不参与 | Domain | PADR-003（D8） |
| C-02 | 记账口径 | 订单/财务 | 双口径均流水级（POS=支付态/新表=完成事件）；凭证=Level 3 演进 | Domain | PADR-005（Level 2） |
| C-03 | 成本口径 | 库存/订单/财务 | 分口径一致；双写待确认 | Domain+DB | VERIFIED（口径）；双写=待确认 |
| C-04 | 订单成立 | 订单 | 成立=写行 status=0；三套状态模型（PD-015 两阶段） | Domain | PD-015 |
| C-05 | 应付触发 | 采购/财务 | 同事务幂等 AP；链路完整（应付→付款→凭证→余额→作废红冲） | Domain | VERIFIED |
| C-06 | 门店经营范围 | 产品/库存 | 库存≠经营范围；不把库存当替代物 | Domain | PADR-002（D2） |
| C-07 | 订单状态机 | 订单（跨域） | 三套并存（orders 0-6 / legacy 0-7 / sales_order String） | Domain+DB | PD-015/017~021 |

## 二、需产品决策（BLOCKED_PRODUCT_DECISION）

| # | 冲突点 | 缺什么 | 决策方 | 决策状态 |
|---|---|---|---|---|
| C-08 | 反向链空白 | 取消已支付退款/凭证红冲/收款冲正 | 产品+财务 | PD-033 待决 |
| C-09 | 销售凭证链路 | generateSalesVoucher 零调用（Level 3 演进边界） | 产品 | PADR-005 Level 3=演进 |
| C-10 | 采购凭证链路 | generatePurchaseVoucher 零调用 | 产品 | Level 3=演进 |
| C-11 | 发布审计 | 发布动作无 @AuditLog | 产品/架构 | 登记（非 BLOCKED，技术可先行） |

## 三、技术整改（与产品决策隔离）

| # | 冲突点 | 方案 | 责任层 | 状态 |
|---|---|---|---|---|
| C-12 | 金额单位双轨（分/元） | Batch0-方案2 批 A/B/C/D | DB+UI | 批 A（小程序转换）/B（finance_record 口径）已列 |
| C-13 | POS 双源（food/foods） | Batch0-方案1 G1 | API+Domain | 待执行 |
| C-14 | 凭证状态映射（后端 0-3 vs 前端 1-4） | Batch0-方案3 | UI | P1-UI-FIN-002 待执行 |
| C-15 | 科目余额双轨 | PD-031 已决（accounting_subjects 为准；account_balance 冻结） | DB+Domain | 已裁决，实施中 |
| C-16 | 资金流水无 status | PD-028 已决（fund_flows 增 status，同 Batch0-方案2 批 C） | DB | 待落库 |
| C-17 | 成本双写 | persistOrderCost + recordOrderCost 同事件 | Domain | 待确认（业务语义判定） |

## 四、双写/一致性缺口（跨域系统性模式）

| # | 缺口 | 详情 | 责任层 |
|---|---|---|---|
| C-18 | food+foods 双写 | 下单/退款/超时同时操作两表；启动同步 foods→food | Domain+API |
| C-19 | inventory+store_inventory 双写 | 采购入库/调拨/收货按位置类型双写 | Domain |
| C-20 | stores 双表 | stores / stores_new 并存（V20260723_001 已重指） | DB |
| C-21 | 科目余额双轨 | accounting_subjects.balance（分/自动）vs account_balance（元/人工） | DB（PD-031 冻结） |

## 五、状态机 CONFLICT 汇总

| 对象 | CONFLICT 类型 | 详情 |
|---|---|---|
| 订单状态 | 三套模型 | orders 0-6 + legacy 0-7 + sales_order String |
| 库存状态词表 | 4 套词表 | INV §6（4 套不同 normal/warning/expired/frozen 组合） |
| 凭证状态 | 前后端映射错位 | 后端 0-3 vs 前端 1-4 |
| 发布/售罄 | status 混叠 | foods.status ≥4 语义（发布/门店销售/库存售罄/分类） |
| 库存盘点计划 Tab | 前端有 Tab 后端缺 | KL-033（Tab 已禁用） |
| 预警 status=1 | 两处语义相反 | 未处理 vs 已处理 |

---

*本冲突图为 Fact Map §9 的聚焦提取，与 `cross-domain-conflict-summary.md`（12 点原始）同源。*
*文档生成：架构总控（会话1）· 2026-09-08*
