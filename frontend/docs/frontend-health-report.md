# frontend 餐饮管理端 全面体检报告

- 体检日期：2026-08-09
- 体检对象：`P:\my-new-project\frontend`（Vue 3 + Element Plus + Pinia + Vue Router + Vite + TS）
- 体检范围：240 个 Vue 文件、424 个 TS 文件、25 个业务模块目录
- 体检方法：逐模块代码审读 + `vue-tsc --noEmit` 实测 + `npm run build` 实测 + 运行时日志核对
- 备注：所有问题均标注 文件:行号，可独立复核

---

## 一、总体健康度（模块评分）

| 模块 | 健康度 | 一句话结论 |
|---|---|---|
| **采购主链**（档案/供应商/订单/到货/验收） | ★★★★☆ | 真实打通，但有运行时警告 |
| 采购外围（合同/结算/计划/报表） | ★★☆☆☆ | 合同签署断链、结算有严重编辑 Bug |
| 商品档案（product） | ★★★☆☆ | CRUD 可用；搜索分页失效、与采购双体系 |
| 订单 | ★★☆☆☆ | 能看不能办：退款入口缺失、筛选失效 |
| 会员营销 | ★☆☆☆☆ | **最差**：充值假成功、编辑变新建、点按钮就崩 |
| 溯源 | ★★★☆☆ | 4 页健康，5 页有假删除/假导出/功能不可用 |
| 仓储 | ★★★☆☆ | 单证齐全但盘点/补货/报表是假的 |
| 财务 | ★★★★☆ | 最真实：总账/应收应付/凭证/报表全真 |
| 资产管理 | ★★☆☆☆ | 盘点页全挂（注释吞 import）、维修报废假成功 |
| 人事 HR | ★★☆☆☆ | 智能画像/知识库/排班 100% 演示数据 |
| 运营中心 | ★★☆☆☆ | 报表中心真，决策板 60% 空壳 |
| 门店运营 | ★★☆☆☆ | 收货/要货真；排班整页 Mock、招聘假成功 |
| 系统管理 | ★★★★☆ | 最健康，仅少量假用户兜底 |
| 设备/签章 | ★★★☆☆ | 设备监控真；签章字段仅存内存 |
| 登录/工作台/个人中心 | ★★★★★ | 全部真实 |

---

## 二、工程基线实测

| 项目 | 结果 |
|---|---|
| `npm run build` | ✅ 通过（3.84s，仅 chunk 体积警告） |
| `vue-tsc --noEmit` | ❌ **187 个错误**，分布在 ~60 个文件，无 typecheck 脚本、无 CI 门禁 |
| 路由→页面文件 | ✅ 130/130 零断链 |
| 菜单→路由 | ✅ 132/132 零死链 |
| 运行时日志 | ⚠️ 主链页面（到货单详情）报 `lockScroll` 类型警告、ResizeObserver 循环警告 |

---

## 三、S级问题（数据污染 / 资金 / 合规 / 全站不可用）——必须最先处理

### 1. 编辑保存走错分支 → 生成重复单据（全站同款 Bug 至少 5 处）

判断条件 `isEdit && xxx.value` 中的对象只在"查看"时赋值，"编辑"时为空 → 保存落入 create 分支新建重复记录：

- `src/views/marketing/MemberList.vue:282` — 编辑会员会**新建重复会员**
- `src/views/purchase/PurchaseSettlement.vue:334` — 编辑结算单**生成重复结算单**，若看过详情还会更新错单
- `src/views/traceability/TraceabilityQuality.vue:281` — 编辑质检记录新建重复记录
- `src/views/purchase/MaterialRequest.vue:342`（已下线页）+ `src/views/purchase/PurchaseRequest.vue`（已修复，可参考其 `editRequestId` 方案）

### 2. 资金操作"假成功"

- `src/views/marketing/MemberDetail.vue:399-416` — 充值**不调用任何 API**，直接弹"充值成功"（真实资金）
- `src/views/marketing/MemberList.vue:337` — 点"充值"调用未定义函数 `loadRechargePlans()` → **运行时崩溃**

### 3. 食品安全/合规场景"假成功/假删除"

- `src/views/traceability/FoodTraceCode.vue:331-350` — 删除追溯码不调 API 直接弹"删除成功"
- `src/views/traceability/TraceabilityInspection.vue:524-529` — 报告上传只把文件名塞进字段，文件从未上传
- `src/views/traceability/LabelTemplate.vue:203-216` — "预览打印"实际直接下发空数据打印指令
- `src/views/store-ops/StoreCertificate.vue:93-114`（自注 TODO P0）— 审计日志仅存前端内存，刷新即丢
- `src/views/seal/SealManagement.vue:317-331` — 印章编号/保管部门仅存内存；使用登记 7 个字段提交时丢弃 5 个
- `src/views/supplier-portal/SignLinkManagement.vue:379-409` — 签约记录/访问记录为硬编码假数据
- `src/stores/permission.ts:193-243` — 操作审计内置 5 条伪造审计日志

### 4. 权限守卫角色矩阵缺失 → 三类角色用户全站被拦截

- `src/router/guards.ts:46-60`：`PURCHASE_MANAGER`、`WAREHOUSE_MANAGER`、`DEPARTMENT_MANAGER` 三个已注册角色未进 `roleFullDomains`，菜单可见但所有页面被拦截（只能进 /home）

### 5. 资产盘点页面全挂

- `src/api/asset/inventory.ts:19-28` — 块注释未闭合吞掉全部 import（38 个 TS 错误），运行即 `ReferenceError: get is not defined`
- `src/views/asset/AssetDisposal.vue:21` — `import type` 导入的枚举按值使用（10 个 TS 错误），状态判断全部失效

### 6. 公开页面被错误拦截 / 无 404 兜底

- `/verify-code`、`/device-activation` 声明了 `requireAuth:false` 但不在守卫白名单 → 未登录被弹回登录页
- `/forgot-password`、`/403`、`/404` 在守卫白名单里但**路由不存在**，无 catch-all → 404 空白页

---

## 四、A级问题（功能完全不可用或大面积占位）

### 采购外围

- `src/views/purchase/PurchaseContract.vue` — 无"签署"按钮（API 有 `sign()` 全库零调用），草稿永远无法生效；表单 8 个字段提交时静默丢弃；附件从未上传（`action="#"`）；详情附件 `:data="[]"` 恒空
- `src/views/purchase/PurchaseSettlement.vue:210-221` — 关联入库单下拉空实现 → 新增结算无法关联订单；付款记录三处恒空；批量结算占位
- `src/views/purchase/ElectronicContract.vue:298-311` — "归档"无 API 假成功；存证/签署日志返回空壳；短信验证码固定 "123456"

### 订单/营销/追溯

- `src/views/order/OrderQuery.vue:259` — 无任何发起退款入口（TODO）；支付状态筛选被 API 层静默丢弃；导出/打印/批量操作全占位
- `src/views/order/OrderReservation.vue:662` — 确认/到店/取消全是死代码，列表只有"详情"
- `src/views/order/OrderStatistics.vue` — 2 个 Tab 空壳、统计维度选择器是死控件、前端假分页
- `src/views/marketing/MemberOverview.vue` — 渠道分布硬编码全 0；时间范围 radio 死控件
- `src/views/traceability/MaterialTraceCode.vue:137-154` — 物料/供应商下拉空实现 → 生成表单必填校验永远过不去，**功能不可用**
- `src/views/marketing/RechargeManage.vue:795` — "充值活动设置"打开空占位对话框，旁边有完整页面却不跳转

### 仓储

- `src/views/warehouse/InventoryCheck.vue` — 盘点闭环三断：实盘录入 UI 缺失（`submitCheckItems` 零调用）、创建参数被吞（团队/模式/阈值全丢）、盘点计划 5 条硬编码假数据
- `src/views/warehouse/SmartRestock.vue:295-326` — 提交补货建议只 push 本地数组，采购模块永远收不到
- `src/views/warehouse/InventoryReport.vue` — 统计卡和"成本分析"Tab 全硬编码
- `src/views/product/Inventory.vue:41-54` — 搜索是前端过滤当前页，后端支持参数却从不传 → 翻页失效、搜索失效；扫码入库是字符串匹配假扫码
- `src/views/warehouse/InventoryOutbound.vue:399-423` — 扫码把条码字符串直接当 materialId 提交
- `src/views/warehouse/StoreInventory.vue:313-424` — 5 个操作按钮占位、调整类型被退化丢弃

### HR

- `src/views/store-ops/ShiftManagement.vue` — **整页 100% 本地 Mock**，刷新全丢；而真实排班 API（`src/api/schedule/` 13 个文件）全库零引用（孤儿代码）
- `src/views/store-ops/StoreRecruitment.vue` — 增删改发布全部假成功，筛选条件全忽略
- `src/views/hr/HRSalary.vue` — 核算/导出工资条是 setTimeout 假操作；`batchPay` 调不存在的后端接口
- `src/api/employee-intelligence.ts` / `src/api/knowledge-intelligence.ts` / `src/api/contract-intelligence.ts` — 三个 API 文件 100% 纯 Mock（含假员工数据），相关 3 个页面全是演示数据
- `src/views/hr/HROnboarding.vue` — 工号字段收集了但不提交；状态机条件与后端不符；职位筛选传 `page` 而类型是 `current`
- `src/views/hr/HRPosition.vue:244-264` — 页面暴露"清理 POS_ 前缀岗位"按钮，会物理删数据库数据

### 资产/运营

- `src/views/asset/AssetLedger.vue` — 维修/报废/领用归还全假成功（后端接口已存在，纯接线遗漏）
- `src/views/asset/AssetOverview.vue` — 5/6 数据源是空实现，图表永远空白；金额统计硬编码 0
- `src/views/operations/DecisionBoard.vue` — KPI 卡/趋势图/成本图空实现永远空白；时间范围+门店筛选收集了但从未传给 API
- `src/views/store-ops/StoreTableUsage.vue` — 详情硬编码假订单/假顾客；开台提交丢姓名电话

---

## 五、B级问题（半成品/占位/隐性失效，量大）

- **筛选控件与 API 脱节 ≥10 处**：OrderQuery 支付状态、OrderReservation 门店、RechargeManage 日期、MaterialTraceCode/FoodTraceCode 日期供应商、TraceabilityQuality 类型/结果、WarehouseOverview 日期、AssetDepreciation 分类/部门、StoreDailySettlement 关键词、InventoryWarning 低库存口径复用错误
- **统计卡基于当前页计算 ≥20 处**（分页后数字变化）
- **导入/导出占位 ≥15 处**：PurchaseArchive、SupplierArchive、MaterialCategory、AssetLedger、DeviceList、HRContractTemplate 等（部分用 setTimeout 假成功）
- **9 个不存在的 Element Plus 图标**：`Transfer/Copy/DatePicker/TrendingUp/BankCard/Refund/Points/Coupon/Time`（vue-tsc 与运行时双实证），涉及 StoreInventory、MemberDetail、MemberList、OrderStatistics、PurchaseReport、SignLinkManagement 等页面，构建不阻断但运行时图标空白/警告
- **死代码**：MaterialRequest.vue（已下线未删含 Bug）、PurchaseTraceDialog、CostManagementDialog、BomReportDialog、MemberDetailDialog、MemberPreferenceDialog、OrderDetailDrawer/RefundDetailDrawer（还带 `::close-on-click-modal` 语法错误）、overAgeWorkerApi、6 个财务 API 模块零调用、13 个排班 API 零调用、mockTemplateList 等

  > 注：以上条目已于 2026-10-24 清理（删除 21+3 个孤儿组件，保留 6 个待裁决项）
- **误导性注释**：≥8 处声称"失败时 API 层回退 Mock"，实际无回退（InventoryLocation、LiveMonitor、OperationsDashboard、StrategyWorkshop、FoodManagement 等）
- **类型体系混乱**：三份 UserInfo 定义互不兼容；permission 与 auth 双权限实现；`mapOrgLevel` 大小写匹配错位
- **入口配置**：main.ts 全量注册 294 个图标；`layouts/`、`router/modules/` 是空目录

---

## 六、采购主链专项确认（已验证通过的部分）

结论：**主线真实可用，工程质量在全部模块中属于上乘**——API 全部真实、金额分/元转换规范、状态映射集中。但存在以下可修问题：

- 运行时警告：`ArrivalDetailDialog → ReceiptConfirmationFormDialog` 的 `lockScroll="false"` 字符串传给布尔 prop（vite-dev.log 实证，主链页面就有）
- `src/views/purchase/PurchaseOrder.vue:872-877` — 从计划"生成订单"后跳转用 `requestNo=planNo` 过滤，但计划单号≠订单申请号，跳转大概率落空
- `src/views/purchase/PurchaseStockin.vue:277` — 超期判断用 UTC `toISOString()`，东八区 8 点前差一天
- 新老双轨：`purchaseStockinApi` 标注"旧版逐步替换"，但 PurchaseReturn 仍依赖旧接口
- `PurchaseArchive.vue` / `SupplierArchive.vue` 统计卡基于当前页、导入导出占位、低库存硬编码 0

---

## 七、给诊断/修复大模型的建议

1. **优先顺序**：S级（5 处编辑变新建 → 资金/合规假成功 → 权限矩阵 → asset/inventory.ts 注释）→ 采购外围（合同签署、结算关联）→ 各模块假成功清理 → 筛选参数接线 → 死代码清理
2. **复现环境**：`npm run dev`（前端）+ 后端；`npx vue-tsc --noEmit` 得 187 错误基线；`npm run build` 当前通过（修错后需保持）
3. **修复参照**：`src/views/purchase/PurchaseRequest.vue:57` 的 `editRequestId` 方案可作"编辑变新建"的统一修法；`DeviceStatusHistory.vue:244`、`FinanceTax.vue:190` 已诚实修过 Mock 问题，可作"假数据清理"的范式
4. **建议建立**：typecheck 脚本接入 CI 门禁，防止 187 个错误继续累积
5. **高频反模式清单**（修复时全局搜索）：`isEdit &&` 分支误判、`setTimeout` 假成功、`ElMessage.success` 后无 API 调用、"开发中"提示、统计卡 `tableData` 过滤、筛选条件收集但不传参

---

## 附：vue-tsc 错误分布（187 条，Top 20 文件）

| 错误数 | 文件 |
|---|---|
| 38 | src/api/asset/inventory.ts |
| 10 | src/views/asset/AssetDisposal.vue |
| 9 | src/views/marketing/MemberDetail.vue |
| 8 | src/views/purchase/PurchasePlan.vue |
| 7 | src/views/marketing/MemberList.vue |
| 7 | src/views/purchase/MaterialCategory.vue |
| 5 | src/views/device/DeviceList.vue |
| 4 | src/views/order/OrderStatistics.vue |
| 4 | src/views/asset/AssetOverview.vue |
| 4 | src/views/order/OrderQuery.vue |
| 4 | src/views/warehouse/WarehouseOverview.vue |
| 4 | src/views/hr/HROnboarding.vue |
| 4 | src/api/purchase/stockin.ts |
| 4 | src/components/core/StatusTag.vue |
| 3 | src/views/purchase/PurchaseReport.vue |
| 3 | src/views/seal/SealManagement.vue |
| 3 | src/views/purchase/SupplierArchive.vue |
| 3 | src/views/purchase/PurchaseContract.vue |
| 3 | src/utils/permissions.ts |
| 3 | src/views/purchase/ElectronicContract.vue |

其余 ~60 个错误分布于 hr / store-ops / traceability / system / utils / composables / router 等文件。
