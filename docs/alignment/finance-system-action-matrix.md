# 全量数据对齐治理 · 动作矩阵

> 范围：会员营销、财务中心、系统管理、印章/签约、运营决策  
> 说明：梳理各模块页面按钮、表格操作、对话框操作与对应 API/事件的映射关系；缺失、命名不统一、未对接真实接口处已用 `[待修复]` 标注。

---

## 一、跨模块通用动作对齐

| 通用动作 | 推荐命名 | 会员营销 | 财务中心 | 系统管理 | 印章/签约 | 运营决策 | 对齐结论 |
|---------|---------|---------|---------|---------|----------|---------|---------|
| 新增 | `handleCreate` / `openCreate` | `handleCreate` / `openCreateDialog` | `handleCreate` | `handleCreate` | `handleCreate` | — | 命名基本统一 |
| 编辑 | `handleEdit` / `openEdit` | `handleEdit` / `openEdit` | `handleEdit`（部分缺失） | `handleEdit` | `handleEdit` | — | 财务部分页面无编辑能力 `[待修复]` |
| 删除 | `handleDelete` | `handleDelete`（未实现） | 部分无 DELETE 端点 | `handleDelete` | `handleDelete` | — | 删除能力不完整 `[待修复]` |
| 详情/查看 | `handleViewDetail` / `handleDetail` | `handleDetail` | `handleViewDetail` | `handleDetail` / `handleViewDetail` | `handleViewDetail` | — | 命名不统一 `[待修复]` |
| 搜索 | `handleSearch` | ✅ | ✅ | ✅ | ✅ | ✅ | 统一 |
| 重置 | `handleReset` | ✅ | ✅ | ✅ | ✅ | ✅ | 统一 |
| 刷新 | `handleRefresh` / `loadData` | `refresh()` | `loadData()` | `loadUsers()` 等 | `refresh()` | `loadData()` | 加载函数命名分散 `[待修复]` |
| 导出 | `handleExport` | `handleExport`（开发中） | 部分实现 | 部分实现 | `handleExport`（未实现） | `handleExport` | 导出功能多处为占位 `[待修复]` |
| 导入 | `handleImport` | `handleImport`（开发中） | — | 用户管理有导入按钮 | — | — | 导入未对接后端 `[待修复]` |
| 状态切换 | `handleToggleStatus` | `handleStatusToggle` | 各自实现 | `handleToggleStatus` | `handleToggleStatus` / `updateStatus` | — | 命名不统一 `[待修复]` |

---

## 二、会员营销 · 动作矩阵

### 2.1 会员列表（`MemberList.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 状态/权限影响 | 问题 |
|---------|---------|---------|---------|-------------|------|
| 页面头部 | 新增会员 | `handleCreate` | `memberApi.create` | 创建后刷新列表 | — |
| 页面头部 | 导入 | `handleImport` | 无真实接口 | `ElMessage.info('批量导入功能开发中')` | `[待修复]` 导入未对接 |
| 页面头部 | 导出 | `handleExport` | 无真实接口 | `ElMessage.info('导出功能开发中')` | `[待修复]` 导出未对接 |
| 页面头部 | 发送消息 | `handleSendMessage` | 无真实接口 | 仅提示开发中 | `[待修复]` 未对接 |
| 表格操作列 | 编辑 | `handleEdit` | `memberApi.getById` + `memberApi.update` | 更新后刷新 | — |
| 表格操作列 | 充值 | `handleRecharge` | `rechargeRecordApi.recharge` | 创建充值记录 | — |
| 表格操作列 | 调整等级 | `handleLevelAdjust` | 无真实接口 | `ElMessage.info('调整等级功能开发中')` | `[待修复]` 未对接 |
| 表格操作列 | 冻结/启用 | `handleStatusToggle` | `memberApi.freeze` / `memberApi.unfreeze` | active ↔ frozen | 缺少拉黑恢复操作 |
| 表格操作列 | 详情 | `handleDetail` | `memberApi.getConsumeRecords` + `memberApi.getRechargeRecords` | 弹详情抽屉 | 消费记录后端未实现 `[待修复]` |

### 2.2 会员等级（`MemberLevel.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新建等级 | `openCreate` | `memberLevelApi.create` | — |
| 等级卡片 | 编辑 | `openEdit` | `memberLevelApi.update` | — |
| 等级卡片 | 启用/停用 | `toggleStatus` | `memberLevelApi.toggleStatus` | — |
| 等级卡片 | 删除 | `deleteLevel` | `memberLevelApi.delete` | 有会员时禁止删除，前端校验 |
| 积分重置规则 | 编辑规则 | `openResetConfig` | 无真实接口 | `[待修复]` 保存为 `ElMessage.info('积分重置规则保存功能开发中')` |

### 2.3 储值管理（`RechargeManage.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新增充值 | `openCreateDialog` | `rechargeRecordApi.recharge` | — |
| 页面头部 | 导出 | — | `rechargeRecordApi.export` | 未在页面内显式绑定导出按钮 `[待修复]` |
| 表格操作列 | 退款 | — | `refundApi.create` | 操作列未完整读取，需确认退款入口 `[待修复]` |
| 表格操作列 | 详情 | — | `rechargeRecordApi.getById` | 需确认详情入口 `[待修复]` |

### 2.4 充值方案/设置/财务流水/退款/异常告警

| 页面/模块 | 动作 | 对应 API | 问题 |
|---------|------|---------|------|
| 充值方案 | 新增/编辑/删除/切换状态 | `rechargePlanApi.create/update/delete/toggleStatus` | — |
| 退款申请 | 创建/审批/执行 | `refundApi.create/approve/execute` | — |
| 储值设置 | 获取/更新 | `rechargeSettingsApi.get/update` | — |
| 财务流水 | 获取/导出 | `rechargeFinanceApi.getPage/export` | — |
| 异常告警 | 获取/处理/忽略 | `anomalyAlertApi.getPage/handle/ignore` | — |

---

## 三、财务中心 · 动作矩阵

### 3.1 会计科目（`FinanceSubject.vue`）

| 操作位置 | 按钮/动作 | 对应 API | 问题 |
|---------|---------|---------|------|
| 页面头部 | 新增科目 | `subjectApi.create` | — |
| 表格操作列 | 编辑 | `subjectApi.update` | — |
| 表格操作列 | 启用/停用 | `subjectApi.updateStatus` | — |
| 表格操作列 | 删除 | `subjectApi.delete` | — |

### 3.2 财务凭证（`FinanceLedger.vue` / `AutoVoucher.vue`）

| 操作位置 | 按钮/动作 | 对应 API | 状态流转 | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新增凭证 | `voucherApi.create` | draft | — |
| 表格操作列 | 编辑 | `voucherApi.update` | draft 可改 | — |
| 表格操作列 | 审核 | `voucherApi.approve` | draft → audited | — |
| 表格操作列 | 反审核 | `voucherApi.unapprove` | audited → draft | — |
| 表格操作列 | 过账 | `voucherApi.post` | audited → posted | — |
| 表格操作列 | 反过账 | `voucherApi.unpost` | posted → audited | — |
| 表格操作列 | 作废 | `voucherApi.void` | draft/audited → cancelled | — |

### 3.3 应收账款（`FinanceReceivable.vue`）

| 操作位置 | 按钮/动作 | 对应 API | 问题 |
|---------|---------|---------|------|
| 页面头部 | 新增应收 | `receivableApi.create` | — |
| 表格操作列 | 收款 | `receivableApi.confirmPayment` | — |
| 表格操作列 | 核销 | `receivableApi.writeOff` | — |
| 表格操作列 | 详情 | `receivableApi.getById` | — |
| — | 编辑/删除 | 无 API | `[待修复]` 后端无 update/delete 端点 |

### 3.4 应付账款（`FinancePayable.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新增应付 | `handleCreate` + `handleSubmit` | `payableApi.create` | — |
| 表格操作列 | 付款 | `handlePay` | `payableApi.confirmPayment` | — |
| 表格操作列 | 付款记录 | `handleViewHistory` | `PaymentHistoryDialog` 内查询 | — |
| 表格操作列 | 详情 | `handleViewDetail` | 本地格式化数据 | 详情仅展示当前行，未调用 `payableApi.getById` `[待修复]` |
| — | 编辑/删除 | 无 | 无 API | `[待修复]` 后端无 update/delete 端点 |

### 3.5 发票管理/发票报销（`InvoiceReimbursement.vue`）

| 操作位置 | 按钮/动作 | 对应 API | 状态流转 | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新增报销 | `handleCreate` + `submitCreate` | `invoiceApi.create` | 页面标题为"发票报销"，实际混合发票管理与报销 `[待修复]` |
| 表格操作列 | 审批 | `handleApprove` + `confirmApprove` | `invoiceApi.update`（status=issued/cancelled） | 审批语义与发票状态耦合 `[待修复]` |
| 表格操作列 | 详情 | `handleViewDetail` | 本地数据 | — |
| — | 开具/作废/红冲 | `invoiceApi.issue/void/redFlush` | 发票状态机 | 操作入口需确认是否暴露 `[待修复]` |

### 3.6 报销单（`InvoiceReimbursement.vue` 涉及）

| 操作位置 | 按钮/动作 | 对应 API | 状态流转 | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 创建报销 | `reimbursementApi.create` | draft | — |
| 表格操作列 | 审批 | `reimbursementApi.approve` | draft → approved / rejected | — |
| 表格操作列 | 付款 | `reimbursementApi.pay` | approved → paid | — |
| 表格操作列 | 取消 | `reimbursementApi.cancel` | draft/approved → cancelled | — |

### 3.7 成本管理（`FinanceCost.vue`）

| 操作位置 | 按钮/动作 | 对应 API | 问题 |
|---------|---------|---------|------|
| 页面头部 | 新增成本 | `costApi.create` | — |
| 表格操作列 | 编辑 | `costApi.update` | — |
| 表格操作列 | 删除 | 无 | `[待修复]` 后端无 DELETE 端点 |

### 3.8 预算管理（`FinanceBudget.vue`）

| 操作位置 | 按钮/动作 | 对应 API | 问题 |
|---------|---------|---------|------|
| 页面头部 | 新增预算 | `budgetApi.create` | — |
| 表格操作列 | 编辑 | `budgetApi.update` | — |
| 表格操作列 | 更新实际金额 | `budgetApi.updateActualAmount` | — |
| 表格操作列 | 删除/状态流转 | 无 | `[待修复]` 缺少状态推进 API（approved/executing/closed） |

### 3.9 资金管理、税务管理、财务报表、财务审批、会计期间

| 页面 | 动作 | 对应 API | 问题 |
|-----|------|---------|------|
| 资金管理 | 查询/创建/更新/删除银行账户 | `bankAccountApi.*` | 需结合页面确认 |
| 资金管理 | 资金流水确认/取消 | `fundFlowApi.*` | 需结合页面确认 |
| 税务管理 | 税率/税种维护 | `taxRateApi.*` / `taxCalculationApi.*` | 需结合页面确认 |
| 财务报表 | 生成/导出 | `reportApi.*` / `statisticsApi.*` | 需结合页面确认 |
| 财务审批 | 审批流配置详情 | `approvalFlowApi.getList` | 当前仅详情，无新增/编辑入口 `[待修复]` |
| 会计期间 | 结账/反结账 | `accountingPeriodApi.*` | 需结合页面确认 |

---

## 四、系统管理 · 动作矩阵

### 4.1 用户管理（`UserManagementTab.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新增用户 | `handleCreate` | `userApi.create` | — |
| 页面头部 | 导入 | — | 无真实接口 | `[待修复]` 导入未对接 |
| 页面头部 | 导出 | — | 无真实接口 | `[待修复]` 导出未对接 |
| 表格操作列 | 详情 | `handleView` | 本地数据 | — |
| 表格操作列 | 编辑 | `handleEdit` | `userApi.update` + `userApi.assignRoles` | — |
| 表格操作列 | 重置密码 | `handleResetPassword` | `userApi.resetPassword` | — |
| 表格操作列 | 启用/禁用 | `handleToggleStatus` | `userApi.toggleStatus` | — |
| 表格操作列 | 删除 | `handleDelete` | 无真实接口 | `[待修复]` `userApi.delete` 不存在 |
| 批量操作 | 批量删除 | `handleBatchDelete` | 无真实接口 | `[待修复]` 未对接 |

### 4.2 角色管理（`RoleManagementTab.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新增角色 | `handleCreate` | `roleApi.create` | — |
| 表格操作列 | 详情 | `handleDetail` | 本地数据 | — |
| 表格操作列 | 编辑 | `handleEdit` | `roleApi.update` | — |
| 表格操作列 | 分配权限 | `handleAssignPermissions` | `roleApi.getPermissions` + `roleApi.assignPermissions` | — |
| 表格操作列 | 分配用户 | `handleAssignUsers` | `userApi.getList` + `userApi.assignRoles` | — |
| 表格操作列 | 启用/禁用 | `handleToggleStatus` | `roleApi.toggleStatus` | — |
| 表格操作列 | 删除 | `handleDelete` | `roleApi.remove` | 系统内置角色禁用删除 |

### 4.3 权限码管理（`PermissionCodeTab.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新增权限 | `handleCreate` | `permissionApi.create` | — |
| 表格操作列 | 详情 | `handleDetail` | `ElMessage.info` | 仅提示，未打开详情 `[待修复]` |
| 表格操作列 | 编辑 | `handleEdit` | `permissionApi.update` | — |
| 表格操作列 | 启用/禁用 | `handleToggleStatus` | `permissionApi.updateStatus` | — |
| 表格操作列 | 删除 | `handleDelete` | `permissionApi.delete` | — |

### 4.4 域权限配置（`DomainPermissionTab.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API/事件 | 问题 |
|---------|---------|---------|--------------|------|
| 顶部 | 选择模板 | `onTemplateChange` | `useDomainPermission.selectTemplate` | — |
| 顶部 | 快速定位角色 | — | `selectedRole` | — |
| 顶部 | 同步到菜单 | `syncToMenu` | `useDomainPermission.syncToMenu` | — |
| 顶部 | 保存为自定义模板 | `saveAsCustomTemplate` | `useDomainPermission.saveAsCustomTemplate` | — |
| 矩阵表格 | 单元格点击切换 | `handleCellClick` | `cycleRoleDomainAccess` | admin/owner 禁止修改 |

### 4.5 模板管理/用户覆盖/审批流程/操作日志

| Tab | 动作 | 对应 API | 问题 |
|-----|------|---------|------|
| 模板管理 | 增删改查权限模板 | `permissionTemplateApi.*` | 需结合页面确认 |
| 用户覆盖 | 为用户单独覆盖菜单/权限 | `userPermissionOverrideApi.*` | 需结合页面确认 |
| 审批流程 | 审批流配置管理 | `system/approval` 相关 | 需结合页面确认 |
| 操作日志 | 查询/导出审计日志 | `auditLogApi.*` / `system/audit-log` | 需结合页面确认 |

---

## 五、印章/签约 · 动作矩阵

### 5.1 印章管理（`SealManagement.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 问题 |
|---------|---------|---------|---------|------|
| 页面头部 | 新增印章 | `handleCreate` | `sealApi.create` | — |
| 页面头部 | 导入 | — | 无 | 页面未实现导入 `[待修复]` |
| 页面头部 | 导出 | — | 无 | 页面未实现导出 `[待修复]` |
| 表格操作列 | 编辑 | `handleEdit` | `sealApi.getById` + `sealApi.update` | — |
| 表格操作列 | 使用登记 | — | `sealApi.recordUsage` | 本地 `usageRecords` 未持久化状态 `[待修复]` |
| 表格操作列 | 归还登记 | — | 本地缓存 | 后端 `SealUsageLog` 无状态字段 `[待修复]` |
| 表格操作列 | 启用/停用 | `handleToggleStatus` / `updateStatus` | `sealApi.updateStatus` | — |
| 表格操作列 | 删除/作废 | `handleDelete` | `sealApi.delete` | 逻辑删除 |
| 表格操作列 | 详情 | `handleViewDetail` | `sealApi.getById` + 本地 `sealDetailMap` | — |

### 5.2 印章使用记录（`SealUsageLogDialog.vue`）

| 操作位置 | 按钮/动作 | 对应 API | 问题 |
|---------|---------|---------|------|
| 对话框 | 查询使用记录 | `sealApi.getUsageLogs` | — |
| 对话框 | 状态筛选（using/returned） | 本地缓存 | `[待修复]` 后端无状态字段 |

---

## 六、运营决策 · 动作矩阵

### 6.1 决策看板（`DecisionBoard.vue`）

| 操作位置 | 按钮/动作 | 事件函数 | 对应 API | 问题 |
|---------|---------|---------|---------|------|
| 顶部筛选 | 时间范围切换 | — | `decisionBoardApi.getRevenueTrend` 等 | — |
| 顶部筛选 | 门店选择 | — | `useStoreOptions` | — |
| 顶部筛选 | 刷新/搜索 | — | 多个 API 聚合 | — |
| 快捷入口 | 销售报表/财务分析/库存管理等 | — | 路由跳转 | 无实际权限校验拦截 `[待修复]` |
| 图表区域 | 趋势/排名/品类等查看 | — | 多个 `decisionBoardApi` | 大量核心接口为 TODO 占位 `[待修复]` |

### 6.2 运营总览/实时监控/运营策略/预警管理/经营报表

| 页面 | 动作 | 对应 API | 问题 |
|-----|------|---------|------|
| 运营总览 | 核心指标/快捷入口 | `operations/dashboard` 相关 | 需结合页面确认 |
| 实时监控 | 门店状态/告警查看 | `operations/live-monitor` 相关 | 需结合页面确认 |
| 运营策略 | 策略创建/编辑/下发 | `operations/strategy` 相关 | 需结合页面确认 |
| 预警管理 | 告警确认/忽略/处理 | `operations/alert` 相关 | 需结合页面确认 |
| 经营报表 | 报表生成/下载/订阅 | `operations/report` 相关 | 需结合页面确认 |

---

## 七、核心动作矩阵问题汇总

| 序号 | 问题描述 | 影响模块 | 修复建议 |
|-----|---------|---------|---------|
| 1 | 会员列表导入/导出/发送消息/调整等级均为开发中占位 | 会员营销 | 后端补齐接口或移除占位按钮 |
| 2 | 储值管理页面导出按钮未显式绑定 API | 会员营销 | 补充导出按钮与 `rechargeRecordApi.export` 绑定 |
| 3 | 财务应收/应付账款缺少编辑和删除能力 | 财务中心 | 后端补充 update/delete 端点，或明确业务仅通过收付款闭环 |
| 4 | 应付账款详情未调用 `payableApi.getById`，仅展示表格行数据 | 财务中心 | 详情弹窗应调用详情 API |
| 5 | 发票报销页面将发票管理与报销单管理混合，审批调用发票 update | 财务中心 | 拆分发票管理与报销单管理，各自调用对应 API |
| 6 | 预算管理缺少状态推进 API（提交审批/开始执行/关闭） | 财务中心 | 后端补充预算状态流转接口 |
| 7 | 财务审批页面仅支持查看详情，无新增/编辑/删除入口 | 财务中心 | 补充审批流配置 CRUD 操作 |
| 8 | 用户管理删除/批量删除、导入/导出均未对接后端 | 系统管理 | 后端补齐接口或隐藏未实现按钮 |
| 9 | 权限码管理"详情"仅 `ElMessage.info`，未打开详情弹窗 | 系统管理 | 实现权限详情展示 |
| 10 | 印章使用登记/归还登记依赖本地缓存，后端 `SealUsageLog` 无状态字段 | 印章/签约 | 后端补充使用记录状态，前端对接 |
| 11 | 决策看板大量核心接口 TODO 占位 | 运营决策 | 后端补齐接口，前端移除 mock/占位 |
| 12 | 跨模块加载函数命名不统一：`refresh` / `loadData` / `loadUsers` / `loadRoles` 等 | 全模块 | 统一使用 `loadData` 或按模块 `load{Entity}s` |
| 13 | 跨模块状态切换命名不统一：`handleToggleStatus` / `handleStatusToggle` / `updateStatus` | 全模块 | 统一为 `handleToggleStatus` |
| 14 | 跨模块详情/查看命名不统一：`handleDetail` / `handleView` / `handleViewDetail` | 全模块 | 统一为 `handleViewDetail` |
