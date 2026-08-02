# 菜单-路由-页面标题一致性核查报告

> 生成时间：2026/7/30 20:10:00
> 报告版本：v2（覆盖 v1，补充用户新提及遗漏项）

## 一、核查方法说明

1. **菜单配置**：读取 `frontend/src/modules/*/menu.ts`，提取每个菜单组的标题、路径及子菜单项的标题与路径。
2. **路由配置**：读取 `frontend/src/router/index.ts`，提取 `path` 与 `meta.title`。
3. **页面标题**：遍历 `frontend/src/views/**/*.vue`，优先提取 `PageHeader` 的 `title` 属性，其次 `h1`，再次 `h2`。
4. **一致性判定**：菜单名称、路由 `meta.title`、页面标题三者完全相同视为一致；任一不同视为不一致。
5. **差异对比**：与上次报告 `docs/alignment/menu-title-consistency-report.md`（生成时间 2026/7/30 18:27:51）逐条比对，修正误判并补充新增遗漏项。

## 二、完整对照表

| 类型 | 菜单组 | 菜单名称 | 路由 path | 路由 meta.title | 页面标题 | 是否一致 | 建议统一名称 |
|------|--------|----------|-----------|-----------------|----------|----------|--------------|
| 菜单组 | 工作台 | 工作台 | /home | - | - | 有路由 | - |
| 菜单组 | 仓储管理 | 仓储管理 | /warehouse | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 食品追溯 | 食品追溯 | /traceability | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 系统管理 | 系统管理 | /system | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 门店管理 | 门店管理 | /store-management | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 电子签章 | 电子签章 | /seal | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 采购管理 | 采购管理 | /purchase | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 产品中心 | 产品中心 | /product | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 订单管理 | 订单管理 | /order | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 运营中心 | 运营中心 | /operations | - | - | 有路由 | - |
| 菜单组 | 会员管理 | 会员管理 | /marketing/member-overview | - | - | 有路由 | - |
| 菜单组 | 人事管理 | 人事管理 | /hr | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 财务中心 | 财务中心 | /finance | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 设备管理 | 设备管理 | /device | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单组 | 资产管理 | 资产管理 | /asset | - | - | **无路由** | 菜单组路径通常无需单独路由 |
| 菜单项 | 工作台 | 工作台 | /home | 工作台 | `工作台 · ${profile.displayName}` | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库存概览 | /warehouse/overview | 库存概览 | 仓储总览 | ❌ 不一致 | 库存概览 |
| 菜单项 | 仓储管理 | 库存管理 | /warehouse/inventory | 库存管理 | 库存管理 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库存入库 | /warehouse/inventory-stockin | 库存入库 | 库存入库 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库存出库 | /warehouse/outbound | 库存出库 | 库存出库 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库存调拨 | /warehouse/transfer | 库存调拨 | 库存调拨 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库存调整 | /warehouse/adjust | 库存调整 | 库存调整 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库存报损 | /warehouse/inventory-loss | 库存报损 | 库存报损 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库存盘点 | /warehouse/check | 库存盘点 | 库存盘点 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库存预警 | /warehouse/warning | 库存预警 | 库存预警 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 库位管理 | /warehouse/location | 库位管理 | 库位管理 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 门店库存查看 | /warehouse/store-inventory | 门店库存查看 | 门店库存 | ❌ 不一致 | 门店库存查看 |
| 菜单项 | 仓储管理 | 库存报表 | /warehouse/report | 库存报表 | 库存报表 | ✅ 一致 | - |
| 菜单项 | 仓储管理 | 智能补货建议 | /warehouse/smart-restock | 智能补货建议 | 智能补货建议 | ✅ 一致 | - |
| 菜单项 | 食品追溯 | 临期预警 | /traceability/expiry-warning | 临期预警 | 临期预警 | ✅ 一致 | - |
| 菜单项 | 食品追溯 | 追溯查询 | /traceability/query | 追溯查询 | 溯源查询 | ❌ 不一致 | 追溯查询 |
| 菜单项 | 食品追溯 | 原料追溯 | /traceability/material-code | 原料追溯 | 物料溯源码 | ❌ 不一致 | 原料追溯 |
| 菜单项 | 食品追溯 | 食品追溯 | /traceability/food-trace-code | 食品追溯 | 食品溯源码 | ❌ 不一致 | 食品追溯 |
| 菜单项 | 食品追溯 | 追溯链展示 | /traceability/chain | 追溯链展示 | 追溯链展示 | ✅ 一致 | - |
| 菜单项 | 食品追溯 | 供应商追溯 | /traceability/supplier | 供应商追溯 | 供应商追溯 | ✅ 一致 | - |
| 菜单项 | 食品追溯 | 检验记录 | /traceability/inspection | 检验记录 | 抽检管理 | ❌ 不一致 | 检验记录 |
| 菜单项 | 食品追溯 | 标签模板 | /traceability/label-template | 标签模板 | 标签模板 | ✅ 一致 | - |
| 菜单项 | 食品追溯 | 召回管理 | /traceability/recall | 召回管理 | 召回管理 | ✅ 一致 | - |
| 菜单项 | 食品追溯 | 质量追溯 | /traceability/quality | 质量追溯 | 质检管理 | ❌ 不一致 | 质量追溯 |
| 菜单项 | 系统管理 | 权限中心 | /system/permission-center | 权限中心 | 权限中心 | ✅ 一致 | - |
| 菜单项 | 系统管理 | 系统配置 | /system/permission | 系统配置 | 系统配置 | ✅ 一致 | - |
| 菜单项 | 系统管理 | AI模型配置 | /system/ai-model-config | AI模型配置 | AI模型配置 | ✅ 一致 | - |
| 菜单项 | 系统管理 | 操作审计 | /system/operation-audit | 操作审计 | 操作审计 | ✅ 一致 | - |
| 菜单项 | 门店管理 | 待办事项 | /store-management/pending-tasks | 待办事项 | 待办事项 | ✅ 一致 | - |
| 菜单项 | 门店管理 | 门店收货 | /store-management/store-receiving | 门店收货 | 门店收货 | ✅ 一致 | - |
| 菜单项 | 门店管理 | 门店库存 | /store-management/store-inventory | 门店库存 | 门店库存 | ✅ 一致 | - |
| 菜单项 | 门店管理 | 物资需求 | /store-management/material-request | 物资需求 | 门店要货 | ❌ 不一致 | 物资需求 |
| 菜单项 | 门店管理 | 日结对账 | /store-management/daily-settlement | 日结对账 | 门店日结 | ❌ 不一致 | 日结对账 |
| 菜单项 | 门店管理 | 证件管理 | /store-management/certificate | 证件管理 | 证照管理 | ❌ 不一致 | 证件管理 |
| 菜单项 | 门店管理 | 排班管理 | /store-management/shift | 排班管理 | 班次管理 | ❌ 不一致 | 排班管理 |
| 菜单项 | 门店管理 | 门店招聘 | /store-management/recruitment | 门店招聘 | 门店招聘 | ✅ 一致 | - |
| 菜单项 | 门店管理 | 桌台记录 | /store-management/table-usage | 桌台记录 | 桌台管理 | ❌ 不一致 | 桌台记录 |
| 菜单项 | 门店管理 | 叫号记录 | /store-management/queue-call-history | 叫号记录 | 排队历史 | ❌ 不一致 | 叫号记录 |
| 菜单项 | 电子签章 | 印章管理 | /seal/management | 印章管理 | 印章管理 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 商品分类 | /purchase/material-category | 商品分类 | 物资分类 | ❌ 不一致 | 商品分类 |
| 菜单项 | 采购管理 | 商品档案 | /purchase/archive | 商品档案 | 商品档案 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 供应商档案 | /purchase/supplier | 供应商档案 | 供应商档案 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 物资需求提报 | /purchase/material-request | 物资需求提报 | 物资需求 | ❌ 不一致 | 物资需求提报 |
| 菜单项 | 采购管理 | 采购申请 | /purchase/request | 采购申请 | 采购申请 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 采购计划 | /purchase/plan | 采购计划 | 采购计划 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 采购订单 | /purchase/orders | 采购订单 | 采购订单 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 到货登记 | /purchase/stockin | 到货登记 | 到货登记 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 采购退货 | /purchase/return | 采购退货 | 采购退货 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 采购合同 | /purchase/contract | 采购合同 | 采购合同 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 电子合同 | /purchase/electronic-contract | 电子合同 | 电子合同 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 签署链接 | /supplier-portal/links | 签署链接管理 | 签约链接管理 | ❌ 不一致 | 签署链接 |
| 菜单项 | 采购管理 | 采购结算 | /purchase/settlement | 采购结算 | 采购结算 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 采购报表 | /purchase/report | 采购报表 | 采购报表 | ✅ 一致 | - |
| 菜单项 | 采购管理 | 采购数据分析 | /purchase/analysis | 采购数据分析 | 采购数据分析 | ✅ 一致 | - |
| 菜单项 | 产品中心 | 菜品管理 | /product/food | 菜品管理 | 菜品管理 | ✅ 一致 | - |
| 菜单项 | 产品中心 | 菜品分类 | /product/category | 菜品分类 | 菜品分类 | ✅ 一致 | - |
| 菜单项 | 产品中心 | 套餐管理 | /product/combo | 套餐管理 | 套餐管理 | ✅ 一致 | - |
| 菜单项 | 产品中心 | 菜品定价 | /product/pricing | 菜品定价 | 菜品定价 | ✅ 一致 | - |
| 菜单项 | 产品中心 | 菜品成本分析 | /product/cost-analysis | 菜品成本分析 | 菜品成本分析 | ✅ 一致 | - |
| 菜单项 | 订单管理 | 订单查询 | /order/query | 订单查询 | 订单查询 | ✅ 一致 | - |
| 菜单项 | 订单管理 | 订单统计 | /order/statistics | 订单统计 | 订单统计 | ✅ 一致 | - |
| 菜单项 | 订单管理 | 退款管理 | /order/refund | 退款管理 | 退款管理 | ✅ 一致 | - |
| 菜单项 | 订单管理 | 预约管理 | /order/reservation | 预约管理 | 预约管理 | ✅ 一致 | - |
| 菜单项 | 运营中心 | 运营总览 | /operations | 运营总览 | 运营总览 | ✅ 一致 | - |
| 菜单项 | 运营中心 | 门店档案 | /operations/store-archive | 门店档案 | 门店档案 | ✅ 一致 | - |
| 菜单项 | 运营中心 | 实时监控 | /operations/live-monitor | 实时监控 | 实时监控 | ✅ 一致 | - |
| 菜单项 | 运营中心 | 经营分析 | /operations/decision-board | 经营分析 | 决策看板 | ❌ 不一致 | 经营分析 |
| 菜单项 | 运营中心 | 运营策略 | /operations/strategy-workshop | 运营策略 | 运营策略 | ✅ 一致 | - |
| 菜单项 | 运营中心 | 预警管理 | /operations/alert-command-center | 预警管理 | 预警管理 | ✅ 一致 | - |
| 菜单项 | 运营中心 | 经营报表 | /operations/reports | 经营报表 | 经营报表 | ✅ 一致 | - |
| 菜单项 | 会员管理 | 会员概览 | /marketing/member-overview | 会员概览 | 会员概览 | ✅ 一致 | - |
| 菜单项 | 会员管理 | 会员列表 | /marketing/member-list | 会员列表 | 会员列表 | ✅ 一致 | - |
| 菜单项 | 会员管理 | 会员等级 | /marketing/member-level | 会员等级 | 会员等级 | ✅ 一致 | - |
| 菜单项 | 会员管理 | 储值管理 | /marketing/recharge | 储值管理 | 充值管理 | ❌ 不一致 | 储值管理 |
| 菜单项 | 会员管理 | 储值系统设置 | /marketing/recharge-settings | 储值系统设置 | 储值系统设置 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 员工管理 | /hr/employee | 员工管理 | 员工管理 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 组织架构 | /hr/organization | 组织架构 | 组织架构 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 岗位管理 | /hr/position | 岗位管理 | 岗位管理 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 考勤排班 | /hr/attendance | 考勤排班 | 考勤管理 | ❌ 不一致 | 考勤排班 |
| 菜单项 | 人事管理 | 招聘管理 | /hr/recruitment | 招聘管理 | 招聘管理 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 入职办理 | /hr/onboarding | 入职办理 | 入职管理 | ❌ 不一致 | 入职办理 |
| 菜单项 | 人事管理 | 培训发展 | /hr/training | 培训管理 | 培训管理 | ❌ 不一致 | 培训管理 |
| 菜单项 | 人事管理 | 健康证管理 | /hr/health-certificate | 健康证管理 | 健康证管理 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 合同智能管理 | /hr/contract-dashboard | 合同智能管理 | 合同智能管理 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 合同管理 | /hr/contract | 合同管理 | 合同管理 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 合同模板 | /hr/contract-template | 合同模板 | 合同模板 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 合同模板库 | /hr/contract-template-library | 合同模板库 | 合同模板库 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 薪资管理 | /hr/salary | 薪资管理 | 薪资管理 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 合规审批 | /hr/approval | 审批管理 | 审批管理 | ❌ 不一致 | 合规审批 |
| 菜单项 | 人事管理 | 知识库智能 | /hr/knowledge-intelligence | 知识库智能 | 知识库智能化 | ❌ 不一致 | 知识库智能 |
| 菜单项 | 人事管理 | 人事分析 | /hr/analytics | 人事分析 | 人事分析 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 员工画像 | /hr/employee-intelligence | 员工智能画像 | 员工智能画像 | ❌ 不一致 | 员工画像 |
| 菜单项 | 人事管理 | 邀请码管理 | /hr/invitation-code | 邀请码管理 | 邀请码管理 | ✅ 一致 | - |
| 菜单项 | 人事管理 | 配置中心 | /hr/config-center | 配置中心 | HR配置中心 | ❌ 不一致 | 配置中心 |
| 菜单项 | 财务中心 | 财务总账 | /finance/ledger | 财务总账 | 财务总账 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 会计科目 | /finance/subject | 会计科目管理 | 会计科目管理 | ❌ 不一致 | 会计科目管理 |
| 菜单项 | 财务中心 | 会计期间 | /finance/period | 会计期间管理 | 会计期间管理 | ❌ 不一致 | 会计期间管理 |
| 菜单项 | 财务中心 | 应收账款 | /finance/receivable | 应收账款 | 应收账款 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 应付账款 | /finance/payable | 应付账款 | 应付账款 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 成本管理 | /finance/cost | 成本管理 | 成本管理 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 税务管理 | /finance/tax | 税务管理 | 税务管理 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 发票报销 | /finance/invoice-reimbursement | 发票报销 | 发票报销 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 财务报表 | /finance/report | 财务报表 | 财务报表 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 预算管理 | /finance/budget | 预算管理 | 预算管理 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 资金管理 | /finance/fund | 资金管理 | 资金管理 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 财务审批 | /finance/approval | 财务审批 | 财务审批 | ✅ 一致 | - |
| 菜单项 | 财务中心 | 自动凭证管理 | /finance/auto-voucher | 自动凭证管理 | 自动凭证 | ❌ 不一致 | 自动凭证管理 |
| 菜单项 | 设备管理 | 设备列表 | /device/list | 设备列表 | 设备管理 | ❌ 不一致 | 设备列表 |
| 菜单项 | 设备管理 | 设备监控 | /device/monitor | 设备监控 | 设备监控 | ✅ 一致 | - |
| 菜单项 | 设备管理 | 设备告警 | /device/alerts | 设备告警 | 设备告警 | ✅ 一致 | - |
| 菜单项 | 设备管理 | 状态历史 | /device/status-history | 状态历史 | 设备状态历史 | ✅ 一致 | - |
| 菜单项 | 资产管理 | 资产概览 | /asset/overview | 资产概览 | 资产总览 | ❌ 不一致 | 资产概览 |
| 菜单项 | 资产管理 | 资产台账 | /asset/ledger | 资产台账 | 资产台账 | ✅ 一致 | - |
| 菜单项 | 资产管理 | 资产分类 | /asset/category | 资产分类 | 资产分类 | ✅ 一致 | - |
| 菜单项 | 资产管理 | 资产折旧 | /asset/depreciation | 资产折旧 | 资产折旧 | ✅ 一致 | - |
| 菜单项 | 资产管理 | 资产盘点 | /asset/inventory-check | 资产盘点 | 资产盘点 | ✅ 一致 | - |
| 菜单项 | 资产管理 | 资产维护 | /asset/maintenance | 资产维护 | 资产维护 | ✅ 一致 | - |
| 菜单项 | 资产管理 | 资产处置 | /asset/disposal | 资产处置 | 资产处置 | ✅ 一致 | - |
| 菜单项 | 资产管理 | 资产报表 | /asset/report | 资产报表 | 资产报表 | ✅ 一致 | - |

## 三、不一致列表

| 菜单组 | 菜单名称 | 路由 path | 路由 meta.title | 页面标题 | 建议统一名称 |
|--------|----------|-----------|-----------------|----------|--------------|
| 仓储管理 | 库存概览 | /warehouse/overview | 库存概览 | 仓储总览 | 库存概览 |
| 仓储管理 | 门店库存查看 | /warehouse/store-inventory | 门店库存查看 | 门店库存 | 门店库存查看 |
| 食品追溯 | 追溯查询 | /traceability/query | 追溯查询 | 溯源查询 | 追溯查询 |
| 食品追溯 | 原料追溯 | /traceability/material-code | 原料追溯 | 物料溯源码 | 原料追溯 |
| 食品追溯 | 食品追溯 | /traceability/food-trace-code | 食品追溯 | 食品溯源码 | 食品追溯 |
| 食品追溯 | 检验记录 | /traceability/inspection | 检验记录 | 抽检管理 | 检验记录 |
| 食品追溯 | 质量追溯 | /traceability/quality | 质量追溯 | 质检管理 | 质量追溯 |
| 门店管理 | 物资需求 | /store-management/material-request | 物资需求 | 门店要货 | 物资需求 |
| 门店管理 | 日结对账 | /store-management/daily-settlement | 日结对账 | 门店日结 | 日结对账 |
| 门店管理 | 证件管理 | /store-management/certificate | 证件管理 | 证照管理 | 证件管理 |
| 门店管理 | 排班管理 | /store-management/shift | 排班管理 | 班次管理 | 排班管理 |
| 门店管理 | 桌台记录 | /store-management/table-usage | 桌台记录 | 桌台管理 | 桌台记录 |
| 门店管理 | 叫号记录 | /store-management/queue-call-history | 叫号记录 | 排队历史 | 叫号记录 |
| 采购管理 | 商品分类 | /purchase/material-category | 商品分类 | 物资分类 | 商品分类 |
| 采购管理 | 物资需求提报 | /purchase/material-request | 物资需求提报 | 物资需求 | 物资需求提报 |
| 采购管理 | 签署链接 | /supplier-portal/links | 签署链接管理 | 签约链接管理 | 签署链接 |
| 运营中心 | 经营分析 | /operations/decision-board | 经营分析 | 决策看板 | 经营分析 |
| 会员管理 | 储值管理 | /marketing/recharge | 储值管理 | 充值管理 | 储值管理 |
| 人事管理 | 考勤排班 | /hr/attendance | 考勤排班 | 考勤管理 | 考勤排班 |
| 人事管理 | 入职办理 | /hr/onboarding | 入职办理 | 入职管理 | 入职办理 |
| 人事管理 | 培训发展 | /hr/training | 培训管理 | 培训管理 | 培训管理 |
| 人事管理 | 合规审批 | /hr/approval | 审批管理 | 审批管理 | 合规审批 |
| 人事管理 | 知识库智能 | /hr/knowledge-intelligence | 知识库智能 | 知识库智能化 | 知识库智能 |
| 人事管理 | 员工画像 | /hr/employee-intelligence | 员工智能画像 | 员工智能画像 | 员工画像 |
| 人事管理 | 配置中心 | /hr/config-center | 配置中心 | HR配置中心 | 配置中心 |
| 财务中心 | 会计科目 | /finance/subject | 会计科目管理 | 会计科目管理 | 会计科目管理 |
| 财务中心 | 会计期间 | /finance/period | 会计期间管理 | 会计期间管理 | 会计期间管理 |
| 财务中心 | 自动凭证管理 | /finance/auto-voucher | 自动凭证管理 | 自动凭证 | 自动凭证管理 |
| 设备管理 | 设备列表 | /device/list | 设备列表 | 设备管理 | 设备列表 |
| 资产管理 | 资产概览 | /asset/overview | 资产概览 | 资产总览 | 资产概览 |

## 四、遗漏项

### 4.1 有路由无菜单

以下路由在 `router/index.ts` 中有定义，但在菜单配置中未出现（多为公开页、个人中心、重定向或特殊入口）：

| 路由 path | meta.title | 组件 | 页面标题 | 说明 |
|-----------|------------|------|----------|------|
| /login | 登录 | src/views/login/LoginPage.vue | 用户登录 | 未在菜单中注册 |
| /company-init | 公司初始化向导 | src/views/company/CompanyInit.vue | 公司初始化向导 | 未在菜单中注册 |
| /demo/component-gallery | UI组件参考手册 | src/views/demo/ComponentGallery.vue | 组件库展示 | 未在菜单中注册 |
| /hr/knowledge-base | 知识库管理 | src/views/hr/HRKnowledgeBase.vue | 知识库 | 未在菜单中注册 |
| /personal-center | 个人中心 | src/views/personal/PersonalCenter.vue | 个人中心 | 未在菜单中注册 |
| /portal/sign | 供应商签署 | src/views/supplier-portal/SupplierSignPortal.vue | 动态合同名 | H5 公开签署页，未在菜单中注册 |
| /marketing/member-detail/:id | 会员详情 | src/views/marketing/MemberDetail.vue | 动态 pageTitle | 详情页，未在菜单中注册 |

### 4.2 有菜单无路由

以下菜单项（叶子节点）在 `router/index.ts` 中未找到精确匹配的路由：

| 菜单组 | 菜单名称 | 菜单 path | 说明 |
|--------|----------|-----------|------|
| - | - | - | 未发现 |

### 4.3 有页面组件但无对应路由

以下 `views` 页面组件未在 `router/index.ts` 的常规业务路由中直接引用（可能为子组件、抽屉、弹窗或未挂载页面）：

| 组件路径 | 页面标题 |
|----------|----------|
| src/views/store-ops/StoreOperationLog.vue | 门店日志 |
| src/views/store-ops/StoreStatusOverview.vue | 门店状态总览 |
| src/views/hr/KnowledgeStudyRecords.vue | 学习记录 |

## 五、本次与上次报告的差异

### 5.1 新增不一致项（上次误判为一致）

| 菜单组 | 菜单名称 | 路由 path | 路由 meta.title | 页面标题 | 上次判定 | 本次判定 | 差异原因 |
|--------|----------|-----------|-----------------|----------|----------|----------|----------|
| 人事管理 | 知识库智能 | /hr/knowledge-intelligence | 知识库智能 | 知识库智能化 | ✅ 一致 | ❌ 不一致 | 上次仅比对菜单与路由，遗漏页面标题「知识库智能化」多出的「化」字 |
| 人事管理 | 配置中心 | /hr/config-center | 配置中心 | HR配置中心 | ✅ 一致 | ❌ 不一致 | 上次仅比对菜单与路由，遗漏页面标题带「HR」前缀 |

### 5.2 建议统一名称调整

| 菜单组 | 菜单名称 | 路由 path | 上次建议 | 本次建议 | 调整原因 |
|--------|----------|-----------|----------|----------|----------|
| 人事管理 | 合规审批 | /hr/approval | 审批管理 | 合规审批 | 以菜单名称为准，统一为「合规审批」 |
| 人事管理 | 员工画像 | /hr/employee-intelligence | 员工智能画像 | 员工画像 | 以菜单名称为准，统一为「员工画像」 |

### 5.3 遗漏项补充

- 上次遗漏 `/portal/sign`（供应商签署 H5 公开页）和 `/marketing/member-detail/:id`（会员详情页），本次已补充至「有路由无菜单」列表。

## 六、核心发现摘要

- **核查范围**：15 个菜单组，119 个菜单叶子节点，126 条路由（含重定向与公开页），130 个页面标题。
- **不一致项**：共 **30** 处（上次报告 28 处，本次新增 2 处）。
- **人事模块不一致**：共 **7** 处，为全系统最多，需优先治理：
  - 考勤排班 → 考勤管理
  - 入职办理 → 入职管理
  - 培训发展 → 培训管理
  - 合规审批 → 审批管理
  - 知识库智能 → 知识库智能化（本次新增）
  - 员工画像 → 员工智能画像
  - 配置中心 → HR配置中心（本次新增）
- **其他重点案例**：
  - 食品追溯模块存在 5 处不一致（追溯/溯源混用、检验/抽检、质量/质检）。
  - 门店管理模块存在 6 处不一致（排班/班次、桌台记录/管理、叫号记录/排队历史等）。
  - 采购模块「商品分类」与「物资需求提报」页面标题与菜单不一致。
- **遗留问题**：`StoreStatusOverview.vue`（门店状态总览）、`StoreOperationLog.vue`（门店日志）、`KnowledgeStudyRecords.vue`（学习记录）有页面标题但无对应路由，需确认是否废弃或补路由。
