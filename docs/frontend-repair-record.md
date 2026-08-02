# 前端文件修复记录

## 修复概述

修复了 63 个损坏的前端 Vue 文件，确保所有页面能正常访问和构建。

## 修复时间

2026-05-06

## 损坏类型统计

| 损坏类型 | 数量 | 说明 |
|---------|------|------|
| 乱码字符 | 多数 | 文件内容出现乱码、编码错误 |
| 标签不闭合 | 部分 | Vue 模板标签未正确闭合 |
| 属性引号错误 | 部分 | HTML 属性引号不匹配 |
| 导入错误 | 14处 | Element Plus 图标不存在、API路径错误 |

## 按模块修复清单

### 1. 采购管理模块（12个）

- `PurchaseOrder.vue` - 采购订单
- `PurchaseStockin.vue` - 采购收货
- `SupplierArchive.vue` - 供应商档案
- `PurchaseArchive.vue` - 商品档案
- `MaterialCategory.vue` - 物资分类
- `PurchasePlan.vue` - 采购计划
- `PurchaseContract.vue` - 采购合同
- `ElectronicContract.vue` - 电子合同
- `PurchaseRequest.vue` - 采购申请
- `PurchaseSettlement.vue` - 采购结算
- `PurchaseReport.vue` - 采购报表
- `MaterialRequest.vue` - 物资需求

### 2. 仓储管理模块（2个）

- `WarehouseOverview.vue` - 仓储总览
- `StoreInventory.vue` - 门店库存

### 3. 订单管理模块（4个）

- `OrderQuery.vue` - 订单查询
- `OrderStatistics.vue` - 订单统计
- `OrderRefund.vue` - 订单退款
- `OrderReservation.vue` - 订单预订

### 4. 门店运营模块（8个）

- `StoreArchive.vue` - 门店档案
- `StoreDailySettlement.vue` - 门店日结
- `StoreInventory.vue` - 门店库存
- `StoreMaterialRequest.vue` - 门店物资申请
- `StoreQueueHistory.vue` - 排队历史
- `StoreRecruitment.vue` - 门店招聘
- `StoreTableUsage.vue` - 桌台使用
- `ShiftManagement.vue` - 班次管理

### 5. 溯源管理模块（5个）

- `TraceQuery.vue` - 溯源查询
- `FoodTraceCode.vue` - 食品溯源码
- `MaterialTraceCode.vue` - 原料溯源码
- `TraceabilityQuality.vue` - 溯源质量
- `TraceabilityInspection.vue` - 溯源检测

### 6. 人事管理模块（11个）

- `HRAttendance.vue` - 考勤管理
- `HRSalary.vue` - 薪资管理
- `HRRecruitment.vue` - 招聘管理
- `HRKnowledgeBase.vue` - 知识库
- `HRTraining.vue` - 培训管理
- `HRHealthCertificate.vue` - 健康证管理
- `HRContract.vue` - 合同管理
- `HRContractTemplate.vue` - 合同模板
- `HRPosition.vue` - 职位管理
- `HROnboarding.vue` - 入职管理
- `KnowledgeStudyRecords.vue` - 学习记录

### 7. 资产管理模块（6个）

- `AssetOverview.vue` - 资产总览
- `AssetLedger.vue` - 资产台账
- `AssetDepreciation.vue` - 资产折旧
- `AssetInventory.vue` - 资产盘点
- `AssetMaintenance.vue` - 资产维护
- `AssetDisposal.vue` - 资产处置

### 8. 设备管理模块（3个）

- `DeviceList.vue` - 设备列表
- `DeviceAlerts.vue` - 设备告警
- `DeviceStatusHistory.vue` - 设备状态历史

### 9. 会员管理模块（3个）

- `MemberList.vue` - 会员列表
- `MemberDetail.vue` - 会员详情
- `RechargeManage.vue` - 充值管理

### 10. 系统管理模块（5个）

- `OperationAudit.vue` - 操作审计
- `AIModelConfig.vue` - AI模型配置
- `PermissionCodeTab.vue` - 权限码管理
- `RoleManagementTab.vue` - 角色管理
- `UserManagementTab.vue` - 用户管理
- `UserOverrideTab.vue` - 用户权限覆盖

### 11. 其他模块（4个）

- `DecisionBoard.vue` - 决策看板
- `SealManagement.vue` - 印章管理
- `SealUsageLogDialog.vue` - 印章使用记录
- `SignLinkManagement.vue` - 签约链接管理

## 构建错误修复

### Element Plus 图标替换

由于 Element Plus 版本差异，部分图标不存在，进行了以下替换：

| 原图标 | 替换为 | 使用位置 |
|-------|-------|---------|
| `Refund` | `Wallet` | OrderQuery.vue - 退款按钮 |
| `Users` | `User` | StoreTableUsage.vue - 人数图标 |
| `Transfer` | `Switch` | StoreInventory.vue, WarehouseOverview.vue - 库存调拨 |
| `Calculator` | `Money` | AssetDepreciation.vue, HRSalary.vue - 核算按钮 |
| `Promote` | `Promotion` | HROnboarding.vue - 推进流程 |
| `QrCode` | `PictureRounded` | FoodTraceCode.vue - 二维码图标 |
| `Scan` | `Search` | TraceQuery.vue - 扫码查询 |
| `History` | `Clock` | TraceQuery.vue - 查询历史 |
| `Copy` | `CopyDocument` | AIModelConfig.vue - 复制按钮 |

### API 导入路径修复

| 文件 | 修复内容 |
|-----|---------|
| `ShiftManagement.vue` | employeeApi 导入路径从 `@/api/hr/employee` 改为 `@/api/hr` |
| `HRContract.vue` | employeeApi 导入路径从 `@/api/hr/employee` 改为 `@/api/hr` |
| `hr.ts` | 补充 `positionLevelApi` 导出 |

## 验证结果

- ✅ 前端构建成功（`npm run build`）
- ✅ 所有模块页面可正常访问
- ✅ 无 TypeScript 类型错误
- ✅ 无未使用的变量和导入

## 后续补充修复

### 2026-07-31

1. **学习记录页面恢复**
   - 文件：`frontend/src/views/hr/KnowledgeStudyRecords.vue`
   - 问题：用户误将学习记录页面当作知识库重复页面移除。
   - 修复：恢复文件，补充缺失的 `useLayoutStore` import；确认「学习记录」「知识库智能」「知识库管理」为三个独立页面，保留学习记录；菜单/路由/权限码已接入。

2. **采购链路状态机闭环**
   - 文件：
     - 前端：`frontend/src/views/purchase/PurchasePlan.vue`、`frontend/src/api/purchase/plan.ts`、`frontend/src/utils/permissions.ts`
     - 后端：`backend/src/main/java/com/foodtraceability/controller/purchase/PurchasePlanController.java`、`backend/src/main/java/com/foodtraceability/service/purchase/PurchasePlanService.java`、`backend/src/main/java/com/foodtraceability/service/purchase/impl/PurchasePlanServiceImpl.java`
   - 问题：采购计划 `approved` 状态缺少进入 `executing` 的操作入口；前端已调用 `/v1/purchase/plans/${id}/execute`，但后端未实现该接口。
   - 修复：
     - 后端新增 `PurchasePlanService.executePlan(id)` 方法，实现 `approved(2) → executing(3)` 状态流转。
     - 后端 `PurchasePlanController` 新增 `PUT /v1/purchase/plans/{id}/execute` 端点。
     - 前端新增 `purchasePlanApi.execute(id)` 及 `/v1/purchase/plans/${id}/execute` 调用。
     - 在 `PurchasePlan.vue` 操作列新增「开始执行」按钮（状态 `approved`，权限 `purchase:plan:execute`）。
     - 在 `permissions.ts` 注册 `purchase:plan:execute` 和 `purchase:order:cancel` 权限码及中文标签。

3. **数据链路说明文档**
   - 文件：`docs/frontend-verification-plan.md`
   - 内容：新增「十、数据链路设计示例」，以「产品中心 / 菜品原料明细 → 门店库存 → 仓储管理 / 门店库存查看 → 总库存」为例，明确跨模块数据统一来源、自动带出、汇总同步规则。

### 验证结果

- ✅ `npm run build` 通过（2026-07-31）
- ✅ 无新增 TypeScript/ESLint 错误
- ⚠️ 后端 `mvn compile` 未执行：当前环境未配置 `JAVA_HOME`，无法验证后端编译。代码变更已按规范完成，需在有 JDK 21 的环境中补测。

## 数据链路修复跟踪

系统性数据链路问题已单独整理到：

- `docs/data-chain-fix-tracking.md`

该文档按模块列出所有数据来源、汇总、消费链路，标注当前实现、问题影响、修复文件、状态和文档更新情况。后续每修复一条链路，必须同步更新此跟踪表。

## 后续建议

1. 定期检查 Element Plus 图标兼容性
2. 建立文件备份机制，防止文件损坏
3. 使用 Git 版本控制追踪文件变更
4. 数据链路修复遵循"修一处、更新一处文档"原则，以 `data-chain-fix-tracking.md` 为唯一进度源
