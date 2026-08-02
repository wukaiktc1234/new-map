# 仓储/溯源链路操作矩阵

> 范围：仓储管理（库存概览、入库、出库、调拨、盘点、报损、调整、预警、补货、报表、库位）与溯源管理（追溯查询、原料追溯码、食品追溯码、临期预警、召回、质量记录、检验记录、标签模板、供应商追溯）
> 用途：梳理每个页面对外暴露的操作入口、触发条件、调用的 API、请求参数及数据刷新策略，为后续权限收口与接口统一改造提供依据
> 生成日期：2026-07-30

---

## 1. 仓储管理操作矩阵

### 1.1 库存概览 `WarehouseOverview.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 切换仓库/时间 | 顶部筛选栏 | `change` 事件 | `warehouseApi.getActiveList`<br>`inventoryStatsApi.getOverview` | `warehouseId`, `dateRange` | 联动刷新统计卡片与图表 | 仅查询类操作 |
| 查看库存趋势图 | 图表区域 | 页面 `onMounted` | `inventoryStatsApi.getTrend` | `warehouseId`, 近30天 | 初始化加载 | 仅查询类操作 |
| 查看 TOP10 预警 | 预警列表 | 页面 `onMounted` | `inventoryWarningApi.getPage` | `warehouseId`, `size=10` | 初始化加载 | 仅查询类操作 |
| 查看近期动态 | 近期动态列表 | 页面 `onMounted` | `inventoryLogApi.getRecent` | `warehouseId`, `size` | 初始化加载 | 仅查询类操作 |
| 快捷跳转 | 快捷功能区 | 点击按钮 | — | — | 路由跳转至出库/调拨/盘点等页面 | `[待修复]` 快捷入口未与权限矩阵联动 |

### 1.2 库存入库 `InventoryStockin.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询待收货 | 待收货 Tab 搜索栏 | 点击查询 / 回车 / 清空 | `purchaseArrivalApi.getList` | `arrivalCode`, `orderCode`, `status`, `receiverType='WAREHOUSE'` | 重置到第1页后刷新 | 复用采购到货单数据 |
| 重置待收货查询 | 待收货 Tab 搜索栏 | 点击重置 | — | — | 重置表单并查询 | — |
| 确认收货 | 待收货列表操作列 | `canConfirm(row)` 为 true | `purchaseArrivalApi.getById`<br>`receiptConfirmationApi.create`（由 `ReceiptConfirmationFormDialog` 内部调用） | `arrivalId` / 确认明细 | 成功后 `refreshArrivals()` + `refreshConfirmations()` | 需先加载含明细的到货单，无明细则阻止 |
| 查看到货详情 | 操作列 | 点击 | `purchaseArrivalApi.getById` | `arrivalId` | 打开详情弹窗 | — |
| 查看确认单详情 | 已确认 Tab 操作列 | 点击 | `receiptConfirmationApi.getById` | `confirmationId` | 打开详情弹窗 | — |
| 查询已确认 | 已确认 Tab 搜索栏 | 点击查询 | `receiptConfirmationApi.getList` | `confirmationCode`, `receiverType='WAREHOUSE'` | 重置到第1页后刷新 | — |

### 1.3 门店库存查看 `StoreInventory.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询门店库存 | 搜索栏 | 点击查询 / 回车 | `storeInventoryApi.getList`（归属 `store-ops`） | `storeId`, `materialName`, `status` 等 | 刷新表格 | `[待修复]` 该页面归属门店运营 API，与仓储模块命名边界需明确 |
| 库存调整 | 操作列 | 点击 | `storeInventoryApi.adjust` | `inventoryId`, 调整数量/原因 | 刷新列表 | `[待修复]` 按钮权限未校验 |
| 批量调拨 | 工具栏 | 选择多行后点击 | — | — | — | `[待修复]` 代码中标记“功能开发中”，无实际 API 调用 |
| 批量报损 | 工具栏 | 选择多行后点击 | — | — | — | `[待修复]` 代码中标记“功能开发中”，无实际 API 调用 |

### 1.4 库存管理 `Inventory.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询库存 | 搜索栏 | 点击查询 | `inventoryApi.getList` | `warehouseId`, `materialId`, `materialName`, `batchNo`, `status` | 刷新分页表格 | `[待修复]` 路由 `/warehouse/inventory` 指向 `views/product/Inventory.vue`，文件位置与路由 domain 不一致 |
| 锁定库存 | 操作列 | 点击 | `inventoryApi.lock` | `inventoryId`, `quantity`, `referenceNo` | 刷新列表 | `[待修复]` 操作按钮未绑定权限 |
| 解锁库存 | 操作列 | 点击 | `inventoryApi.unlock` | `inventoryId`, `quantity` | 刷新列表 | `[待修复]` 操作按钮未绑定权限 |
| 扣减库存 | 操作列 | 点击 | `inventoryApi.deduct` | `inventoryId`, `quantity`, `transactionType` | 刷新列表 | `[待修复]` 操作按钮未绑定权限 |
| 增加库存 | 操作列 | 点击 | `inventoryApi.increase` | `materialId`, `warehouseId`, `quantity`, `unitCost` | 刷新列表 | `[待修复]` 操作按钮未绑定权限 |

### 1.5 库存出库 `InventoryOutbound.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询出库单 | 搜索栏 | 点击查询 / 回车 / 清空 | `inventoryOutboundApi.getPage` | `outboundCode`, `outboundType`, `status`, `warehouseId`, `dateRange` | 重置到第1页后刷新 | — |
| 重置查询 | 搜索栏 | 点击重置 | — | — | 重置表单并查询 | — |
| 新建出库单 | 页面头部 | 点击“新建出库单” | `inventoryOutboundApi.create` | 出库类型、仓库、目标、物料明细等 | 关闭弹窗并 `loadData()` | `[待修复]` 创建入口未做权限校验 |
| 快捷扫码出库 | 页面头部 | 点击“扫码出库” | `inventoryOutboundApi.create` | 扫描后填充物料 | 关闭弹窗并刷新 | `[待修复]` 扫码逻辑内部调用 `handleOpenCreate`，权限校验缺失 |
| 扫码添加物料 | 新建弹窗 | 点击扫码 | — | 条码值 | 追加到 `scannedItems` | 客户端临时状态 |
| 手动添加物料 | 新建弹窗 | 点击手动添加 | — | — | 追加空行 | 客户端临时状态 |
| 审批出库单 | 操作列 | `row.status === 'pending'` | `inventoryOutboundApi.approve` | `outboundCode`, `{ approved, opinion }` | `loadData()` | `[待修复]` 审批入口未绑定权限 |
| 执行出库 | 操作列 | `row.status === 'approved'` | `inventoryOutboundApi.execute` | `outboundCode` | `loadData()` | `[待修复]` 执行入口未绑定权限 |
| 查看详情 | 操作列 | 点击 | `inventoryOutboundApi.getById` | `outboundCode` | 打开详情弹窗 | — |

### 1.6 库存调拨 `InventoryTransfer.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询调拨单 | 搜索栏 | 点击查询 / 回车 / 清空 | `inventoryTransferApi.getPage` | `transferCode`, `fromWarehouseId`, `toWarehouseId`, `status`, `dateRange` | 重置到第1页后刷新 | — |
| 新建调拨单 | 页面头部 | 点击“新建调拨单” | `inventoryTransferApi.create` | 调出/调入仓库、物料明细 | `loadData()` | `[待修复]` 创建入口未做权限校验 |
| 审批调拨单 | 操作列 | `row.status === 'pending'` | `inventoryTransferApi.approve` | `transferId`, `{ approved, remark }` | `loadData()` | `[待修复]` 审批入口未做权限校验；驳回后无 `rejected` 目标状态 |
| 确认收货 | 操作列 | `row.status === 'shipped'` | `inventoryTransferApi.execute` | `transferId` | `loadData()` | `[待修复]` 执行入口未做权限校验；`received` 状态在类型中存在但 UI 未使用 |
| 查看详情 | 操作列 | 点击 | `inventoryTransferApi.getById` | `transferId` | 打开详情弹窗 | — |

### 1.7 库存盘点 `InventoryCheck.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询盘点记录 | 搜索栏 | 点击查询 / 回车 / 清空 | `inventoryCheckApi.getList` | `checkStatus`, `startDate`, `endDate`, `checkCode` | 刷新表格（注意：当前返回数组，前端本地分页） | `[待修复]` `getList` 返回数组而非统一分页对象，与 `useStandardPage` 期望不一致 |
| 新建盘点单 | 盘点记录 Tab 头部 | 点击 | `inventoryCheckApi.create` | 仓库、盘点类型、盘点日期、盘点团队、冻结库存、差异阈值 | `loadData()` | `[待修复]` 创建入口未做权限校验 |
| 审核盘点单 | 操作列 | `row.checkStatus === 'checking'` | `inventoryCheckApi.approve` | `checkId`, `approved`, `remark` | `loadData()` | `[待修复]` 入口未做权限校验；驳回后无 `rejected` 状态 |
| 查看盘点详情 | 操作列 | 点击 | `inventoryCheckApi.getById` | `checkId` | 打开详情弹窗 | — |
| 生成调整单 | 详情弹窗 | 点击“生成调整单” | `inventoryAdjustApi.create` | 基于差异项分别生成盘盈/盘亏调整单 | `loadData()` | `[待修复]` 详情弹窗操作未校验 `warehouse:check:manage` 权限 |
| 新建盘点计划 | 盘点计划 Tab 头部 | 点击 | `inventoryCheckApi.createPlan` | 计划周期、仓库、盘点类型等 | 刷新计划列表 | `[待修复]` API 与字段定义分散，未在类型层统一 |
| 编辑盘点计划 | 计划列表操作列 | 点击 | `inventoryCheckApi.updatePlan` | 计划 ID + 更新字段 | 刷新计划列表 | `[待修复]` 入口未做权限校验 |
| 启用/停用计划 | 计划列表操作列 | 点击 | `inventoryCheckApi.togglePlanStatus` | 计划 ID, `status` | 刷新计划列表 | `[待修复]` 入口未做权限校验 |
| 立即执行计划 | 计划列表操作列 | 点击 | `inventoryCheckApi.executePlan` | 计划 ID | 刷新计划列表 | `[待修复]` 入口未做权限校验 |

### 1.8 库存报损 `InventoryLoss.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询报损单 | 搜索栏 | 点击查询 / 回车 / 清空 | `inventoryLossApi.getPage` | `lossCode`, `lossType`, `status`, `warehouseId`, `dateRange` | 重置到第1页后刷新 | — |
| 新建报损单 | 页面头部 | 点击“新建报损单” | `inventoryLossApi.create` | 仓库、报损类型、物料明细、金额 | `loadData()` | `[待修复]` 创建入口未做权限校验 |
| 审批报损单 | 操作列 | `row.status === 'pending'` | `inventoryLossApi.approve` | `lossId`, `{ approved, remark }` | `loadData()` | `[待修复]` 审批入口未做权限校验；类型中无 `rejected` 状态 |
| 处理报损单 | 操作列 | `row.status === 'approved'` | `inventoryLossApi.process` | `lossId` | `loadData()` | `[待修复]` 处理入口未做权限校验 |
| 查看详情 | 操作列 | 点击 | `inventoryLossApi.getById` | `lossId` | 打开详情弹窗 | — |

### 1.9 库存调整 `InventoryAdjust.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询调整单 | 搜索栏 | 点击查询 / 回车 / 清空 | `inventoryAdjustApi.getPage` | `adjustCode`, `adjustType`, `status`, `warehouseId`, `dateRange` | 重置到第1页后刷新 | — |
| 新建调整单 | 页面头部 | 点击“新建调整单” | `inventoryAdjustApi.create` | 调整类型、仓库、物料明细 | `loadData()` | `[待修复]` 创建入口未做权限校验 |
| 审批调整单 | 操作列 | `row.status === 'pending'` | `inventoryAdjustApi.approve` | `adjustCode`, `{ approved, opinion }` | `loadData()` | `[待修复]` 审批入口未做权限校验 |
| 执行调整 | 操作列 | `row.status === 'approved'` | `inventoryAdjustApi.execute` | `adjustCode` | `loadData()` | `[待修复]` 执行入口未做权限校验 |
| 查看详情 | 操作列 | 点击 | `inventoryAdjustApi.getById` | `adjustCode` | 打开详情弹窗 | — |

### 1.10 库存预警 `InventoryWarning.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询预警记录 | 搜索栏 | 点击查询 / 回车 / 清空 | `inventoryWarningApi.getPage` | `warningType`, `warehouseId`, `materialName` | 刷新表格 | — |
| 处理预警 | 操作列 | 点击“处理” | `inventoryWarningApi.handle` | `warningId`, `{ handleRemark }` | `loadData()` + `loadStats()` | `[待修复]` 处理入口未做权限校验；处理方式仅记录备注，未关联具体业务动作 |
| 手动生成预警 | 页面头部 | 点击“生成预警” | `inventoryWarningApi.generate` | — | `loadData()` + `loadStats()` | `[待修复]` 生成入口未做权限校验 |
| 跳转智能补货 | 操作列 | 点击“补货” | — | `materialName` | 路由跳转 `/warehouse/smart-restock` | `[待修复]` 跳转未校验 `warehouse:warning:manage` 与 `warehouse:consumption:manage` 权限 |

### 1.11 智能补货 `SmartRestock.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询采购建议 | 搜索栏 | 页面加载 / 查询 | `inventoryWarningApi.getPurchaseSuggestions` | — | 刷新建议列表 | — |
| 生成单行建议 | 建议列表操作列 | 点击 | — | — | 打开建议提交弹窗 | `[待修复]` 仅本地弹窗，未调用后端创建采购申请/订单 API |
| 批量生成建议 | 工具栏 | 选择行后点击 | — | — | 打开建议提交弹窗 | `[待修复]` 同上，无后端写入 |
| 提交采购建议 | 弹窗 | 点击确定 | — | `items`, `supplierId`, `remark` | 写入本地 `suggestionHistory` | `[待修复]` `handleSuggestionSubmit` 仅在本地 mock 数组中追加，未对接 `inventoryWarningApi.createPurchaseOrder` 或采购模块 API |
| 查看建议记录 | Tab 切换 | 点击 | — | — | 展示本地 mock 数据 | `[待修复]` 建议记录为本地静态数据 |

### 1.12 库存报表 `InventoryReport.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 切换报表 Tab | Tab 栏 | 点击 | — | `activeTab` | 按需加载 `trend/category/cost` 数据 | `cost` 为本地静态数据 |
| 导出报表 | 页面头部 | 点击“导出报表” | — | 当前 Tab 数据 | 客户端生成 CSV 并下载 | `[待修复]` 导出为前端 CSV 拼接，未调用后端导出接口，大数据量性能差 |

### 1.13 库位管理 `InventoryLocation.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询库位 | 搜索栏 | 点击查询 / 回车 / 清空 | `inventoryLocationApi.getList` | `warehouseId`, `locationCode` | 刷新分页表格 | — |
| 新建库位 | 页面头部 | 点击“新建库位” | `inventoryLocationApi.create` | `warehouseId`, `locationCode`, `locationName`, `locationType`, `maxCapacity`, `remark` | `loadData()` | `[待修复]` 创建入口未做权限校验 |
| 编辑库位 | 操作列 | 点击“编辑” | `inventoryLocationApi.update` | `locationId`, 可更新字段 | `loadData()` | `[待修复]` 编辑入口未做权限校验 |
| 启用/停用库位 | 操作列 | 点击状态切换 | `inventoryLocationApi.toggleStatus` | `locationId`, `enabled=0/1` | `loadData()` | `[待修复]` 状态切换入口未做权限校验 |
| 批量启用/停用 | 工具栏 | 选择多行后点击 | `inventoryLocationApi.toggleStatus` | 循环调用每个 `locationId` | `loadData()` | `[待修复]` 批量操作入口未做权限校验；循环调用未做事务保证 |

---

## 2. 溯源管理操作矩阵

### 2.1 追溯查询 `TraceQuery.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 扫码/输入追溯码查询 | 搜索区 | 点击查询 / 回车 / 扫码回调 | `traceCodeApi.queryByTraceCode` | `traceCode` | 展示追溯结果 | — |
| 重置查询 | 搜索区 | 点击重置 | — | — | 清空结果 | — |
| 扫码枪输入 | 搜索区 | 扫码设备触发 `handleScanCode` | `traceCodeApi.queryByTraceCode` | `traceCode` | 展示追溯结果 | — |

### 2.2 追溯链展示 `TraceChainView.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询追溯链 | 搜索区 | 点击“查询追溯链” | `traceCodeApi.queryByTraceCode` | `traceCode` | 渲染链式节点 | — |
| 切换追溯方向 | 方向选择器 | `change` 事件 | — | `direction` | 重新渲染链 | 客户端状态 |
| 查看节点详情 | 链节点卡片 | 点击节点 | — | 节点数据 | 打开节点详情弹窗 | 仅展示，无额外 API |

### 2.3 原料追溯码 `MaterialTraceCode.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询原料追溯码 | 搜索栏 | 点击查询 / 回车 / 清空 | `materialTraceCodeApi.getList` | `traceCode`, `productName`, `status`, `dateRange` | 刷新分页表格 | — |
| 批量生成 | 页面头部 | 点击“批量生成” | `materialTraceCodeApi.generate` | `purchaseStockinId`, `materialId`, `quantity` 等 | `loadData()` | `[待修复]` 生成入口未做权限校验 |
| 生成提交 | 生成弹窗 | 点击确定 | `materialTraceCodeApi.generate` | 生成表单 | `loadData()` | — |
| 打印标签 | 操作列 | 点击“打印” | `materialTraceCodeApi.batchPrint` / `materialTraceCodeApi.generateQrCode` | `traceCodeId` | 调用浏览器打印 | `[待修复]` 单条打印复用 batchPrint，未调用单条打印 API |
| 批量打印 | 工具栏 | 选择行后点击 | `materialTraceCodeApi.batchPrint` | `traceCodeIds[]`, `printerId` | — | `[待修复]` 批量操作入口未做权限校验 |
| 导出 | 工具栏 | 点击 | `materialTraceCodeApi.export`（如存在）或本地导出 | — | 下载文件 | `[待修复]` 导出实现需确认是否对接后端 |
| 查看详情 | 操作列 | 点击 | `materialTraceCodeApi.getById` | `traceCodeId` | 打开详情弹窗 | — |
| 追溯链 | 操作列 | 点击“追溯” | — | — | 跳转 `/traceability/chain` | — |
| 删除 | 操作列 | 点击“删除” | `materialTraceCodeApi.delete`（如存在） | `traceCodeId` | `loadData()` | `[待修复]` 删除入口未做权限校验；API 未在 `index.ts` 明确导出 |

### 2.4 食品追溯码 `FoodTraceCode.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询食品追溯码 | 搜索栏 | 点击查询 / 回车 / 清空 | `foodTraceCodeApi.getList` | `traceCode`, `dishName`, `status`, `dateRange` | 刷新分页表格 | — |
| 生成追溯码 | 页面头部 | 点击“生成追溯码” | `foodTraceCodeApi.generate` | `orderId`, `dishId`, `quantity` 等 | `loadData()` | `[待修复]` 生成入口未做权限校验 |
| 打印标签 | 操作列 | 点击“打印” | `foodTraceCodeApi.printLabel` / `generateQrCode` | `traceCodeId`, `printerId` | 调用浏览器打印 | `[待修复]` 入口未做权限校验 |
| 批量打印 | 工具栏 | 选择行后点击 | `foodTraceCodeApi.batchPrint` | `traceCodeIds[]`, `printerId` | — | `[待修复]` 入口未做权限校验 |
| 导出 | 工具栏 | 点击 | `foodTraceCodeApi.export`（如存在）或本地导出 | — | 下载文件 | `[待修复]` 导出实现需确认是否对接后端 |
| 查看详情 | 操作列 | 点击 | `foodTraceCodeApi.getById` | `traceCodeId` | 打开详情弹窗 | — |
| 追溯 | 操作列 | 点击“追溯” | — | — | 跳转 `/traceability/chain` | — |
| 删除 | 操作列 | 点击“删除” | `foodTraceCodeApi.delete`（如存在） | `traceCodeId` | `loadData()` | `[待修复]` 删除入口未做权限校验；API 未在 `index.ts` 明确导出 |
| 更新制作状态 | 详情/操作 | 状态变更 | `foodTraceCodeApi.updateMakeStatus` | `traceCodeId`, `makeStatus` | `loadData()` | `[待修复]` 视图中未观察到直接入口，API 闲置 |

### 2.5 临期预警 `ExpiryWarning.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 切换预警 Tab | Tab 栏 | 点击 | `expiryAlertApi.getExpiringSoon`<br>`expiryAlertApi.getExpired` | `tab` 对应的查询参数 | 刷新表格 | — |
| 查询预警 | 搜索栏 | 点击查询 / 回车 / 清空 | `expiryAlertApi.getExpiringSoon` / `getExpired` | `materialName`, `batchNo`, `dateRange` | 刷新表格 | — |
| 报损处理 | 操作列 | 点击“报损” | `expiryAlertApi.scrap` | `traceCodeId`, `remark` | 刷新当前 Tab 数据 | `[待修复]` 报损入口未做权限校验；未与库存报损单联动 |
| 退货处理 | 操作列 | 点击“退货” | `expiryAlertApi.returnGoods` | `traceCodeId`, `remark` | 刷新当前 Tab 数据 | `[待修复]` 退货入口未做权限校验；未与采购退货单联动 |

### 2.6 召回管理 `RecallManagement.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 分析影响范围 | 搜索区 | 点击“分析影响范围” | `recallApi.analyzeRecall` | `batchNo`, `supplierId`, `targetName` 等 | 渲染受影响追溯码列表 | `[待修复]` 分析入口未做权限校验 |
| 执行召回 | 结果区 | 点击“执行召回” | `recallApi.batchRecall` | `traceCodeIds[]`, `recallReason`, `operatorName` | 成功后清空选择并提示 | `[待修复]` 执行入口未做权限校验；未二次确认 |
| 重置分析 | 搜索区 | 点击“重置” | — | — | 清空表单与结果 | — |

### 2.7 质量追溯 `TraceabilityQuality.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询质量记录 | 搜索栏 | 点击查询 / 回车 / 清空 | `qualityApi.getRecordList` | `materialName`, `abnormalLevel`, `handlingStatus`, `dateRange` | 刷新表格 | — |
| 新增质量记录 | 页面头部 | 点击“新增” | `qualityApi.createRecord` | 质量记录表单 | `loadData()` | `[待修复]` 新增入口未做权限校验 |
| 编辑质量记录 | 操作列 | 点击“编辑” | `qualityApi.updateRecord` | `qualityRecordId`, 更新字段 | `loadData()` | `[待修复]` 编辑入口未做权限校验 |
| 删除质量记录 | 操作列 | 点击“删除” | `qualityApi.deleteRecord` | `qualityRecordId` | `loadData()` | `[待修复]` 删除入口未做权限校验 |
| 查看详情 | 操作列 | 点击“详情” | `qualityApi.getRecordById` | `qualityRecordId` | 打开详情弹窗 | — |
| 处理异常 | 详情/操作 | 点击“处理” | `qualityApi.handleAbnormal` | `qualityRecordId`, `remark` | `loadData()` | `[待修复]` 入口未做权限校验 |
| 导入质量记录 | 工具栏 | 上传文件 | `qualityApi.importRecords`（如存在） | 文件流 | `loadData()` | `[待修复]` 导入实现依赖前端 `UploadFile` 解析，后端接口需确认 |
| 导出质量记录 | 工具栏 | 点击“导出” | `qualityApi.exportRecords`（如存在） | 查询参数 | 下载文件 | `[待修复]` 导出实现需确认是否对接后端 |

### 2.8 检验记录 `TraceabilityInspection.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询检验记录 | 搜索栏 | 点击查询 / 回车 / 清空 | `inspectionApi.getList` | `materialName`, `inspectionType`, `inspectionResult`, `dateRange` | 刷新分页表格 | — |
| 新增检验记录 | 页面头部 | 点击“新增” | `inspectionApi.create` | 检验表单 | `loadData()` | `[待修复]` 新增入口未做权限校验 |
| 编辑检验记录 | 操作列 | 点击“编辑” | `inspectionApi.update` | `inspectionId`, 更新字段 | `loadData()` | `[待修复]` 编辑入口未做权限校验 |
| 删除检验记录 | 操作列 | 点击“删除” | `inspectionApi.delete` | `inspectionId` | `loadData()` | `[待修复]` 删除入口未做权限校验 |
| 查看详情 | 操作列 | 点击“详情” | `inspectionApi.getById` | `inspectionId` | 打开详情弹窗 | — |
| 导出检验记录 | 工具栏 | 点击“导出” | `inspectionApi.export`（如存在） | 查询参数 | 下载文件 | `[待修复]` 导出实现需确认是否对接后端 |
| 上传检验报告 | 表单 | 文件上传 | `inspectionApi.uploadReport`（如存在） | 文件流 | 表单关闭后刷新 | `[待修复]` 上传使用 `http-request` 自定义，后端接口需确认 |

### 2.9 标签模板 `LabelTemplate.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询标签模板 | 页面加载 | `onMounted` | `labelTemplateApi.getList` | — | 刷新表格 | — |
| 编辑模板 | 操作列 | 点击“编辑” | `labelTemplateApi.update` | `templateId`, `layoutConfig` 等 | `loadData()` | `[待修复]` 编辑入口未做权限校验 |
| 预览打印 | 操作列 | 点击“预览打印” | `labelTemplateApi.generatePreview`<br>`labelTemplateApi.printByTemplateId` | `templateId`, 样例数据 | 打开预览弹窗 | `[待修复]` 入口未做权限校验 |
| 设为默认 | 操作列 | 点击“设为默认” | `labelTemplateApi.setDefault`（如存在） | `templateId` | `loadData()` | `[待修复]` 设为默认入口未做权限校验；API 未在 `index.ts` 明确导出 |
| 删除模板 | 操作列 | 点击“删除” | `labelTemplateApi.delete` | `templateId` | `loadData()` | `[待修复]` 删除入口未做权限校验 |
| 新增模板 | 页面头部 | 点击“新增” | — | — | — | `[待修复]` 视图中未提供新增模板入口 |

### 2.10 供应商追溯 `SupplierTrace.vue`

| 操作名称 | 入口位置 | 触发条件 | 调用 API | 请求参数 | 数据刷新策略 | 备注 |
|---------|---------|---------|---------|---------|-------------|------|
| 查询供应商追溯 | 搜索区 | 点击查询 | `supplierTraceApi.getSupplierTrace` | `supplierId` | 渲染汇总信息 | — |
| 切换信息 Tab | Tab 栏 | 点击 | `supplierTraceApi.getBatches`<br>`getQualityRate`<br>`getRecalls`<br>`getInspections`<br>`getStatistics` | `supplierId` | 切换后加载对应数据 | — |

---

## 3. 操作矩阵共性问题

### 3.1 权限校验缺失

- `[待修复]` 仓储/溯源所有视图的操作按钮均未使用 `v-permission` 或 `hasPermission`，任何能进入页面的用户均可点击创建、审批、执行、删除等敏感操作。
- `[待修复]` 路由守卫仅检查域访问级别（`FULL/READ_ONLY/LIMITED/HIDDEN`），但页面内部未根据级别禁用写操作，`READ_ONLY` 角色仍可执行写入。

### 3.2 审批型单据操作入口不完整

- `[待修复]` 出库、调拨、盘点、报损、调整均缺少“取消/撤回”操作，申请后无法主动终止流程。
- `[待修复]` 出库/调拨/盘点/报损/调整的驳回状态未在类型层定义，驳回后用户无重新编辑/提交入口。
- `[待修复]` 盘点单 `pending` → `checking` 的“开始盘点”入口在页面未明确提供，创建后直接进入 `pending`。

### 3.3 部分操作为客户端本地逻辑

- `[待修复]` `SmartRestock.vue` 的采购建议提交仅在本地 `suggestionHistory` 中追加，未调用任何后端 API，业务闭环断裂。
- `[待修复]` `InventoryReport.vue` 的导出报表为前端 CSV 拼接，未对接后端导出接口，无法保证数据一致性与大数据量性能。
- `[待修复]` `TraceabilityQuality.vue` / `TraceabilityInspection.vue` 的导入/导出实现依赖前端 `UploadFile`，后端接口是否支持需确认。

### 3.4 跨模块操作未联动

- `[待修复]` `ExpiryWarning.vue` 的“报损”未生成库存报损单，`InventoryLoss.vue` 无法查看溯源侧发起的报损。
- `[待修复]` `ExpiryWarning.vue` 的“退货”未生成采购退货单，与采购退货流程未打通。
- `[待修复]` `InventoryWarning.vue` 的“补货”跳转至智能补货页面，但参数传递与权限衔接未明确。

### 3.5 API 与视图操作映射不完整

- `[待修复]` `materialTraceCodeApi.updateStatus`、`foodTraceCodeApi.updateMakeStatus` 等 API 已定义，但视图中未观察到对应操作入口。
- `[待修复]` `LabelTemplate.vue` 缺少“新增模板”入口，与 `labelTemplateApi.create` 能力不匹配。
- `[待修复]` 部分 API（如 `materialTraceCodeApi.delete`、`foodTraceCodeApi.delete`）在 `index.ts` 中未明确导出，视图已调用但来源不明。
