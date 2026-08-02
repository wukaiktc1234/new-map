# 前端损坏文件清单

## 扫描时间
2026-07-06

## 统计
- 总 Vue 文件数: 186
- 正常文件: 123
- 损坏文件: 63 (33.9%)

## 损坏特征
- 包含乱码字符（`�`）
- template 标签不平衡（缺少闭合标签）
- script 标签不平衡
- 属性引号混乱
- 中文文字截断

---

## 损坏文件明细（按模块分类）

### 1. 采购管理模块 (12个) - 优先级: 🔴 高

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| PurchaseOrder.vue | `views/purchase/` | 乱码 | 待修复 |
| PurchaseStockin.vue | `views/purchase/` | 乱码 | 待修复 |
| PurchaseArchive.vue | `views/purchase/` | 乱码 | 待修复 |
| MaterialCategory.vue | `views/purchase/` | 乱码 | 待修复 |
| PurchasePlan.vue | `views/purchase/` | 乱码 | 待修复 |
| PurchaseContract.vue | `views/purchase/` | 乱码 | 待修复 |
| ElectronicContract.vue | `views/purchase/` | 乱码 + template不平衡 | 待修复 |
| PurchaseRequest.vue | `views/purchase/` | 乱码 + template不平衡 | 待修复 |
| PurchaseSettlement.vue | `views/purchase/` | 乱码 | 待修复 |
| PurchaseReport.vue | `views/purchase/` | 乱码 | 待修复 |
| SupplierArchive.vue | `views/purchase/` | 乱码 + template不平衡 | 待修复 |
| MaterialRequest.vue | `views/purchase/` | 乱码 | 待修复 |

### 2. 仓储管理模块 (2个) - 优先级: 🔴 高

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| WarehouseOverview.vue | `views/warehouse/` | 乱码 | 待修复 |
| StoreInventory.vue | `views/warehouse/` | 乱码 | 待修复 |

### 3. 订单管理模块 (4个) - 优先级: 🔴 高

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| OrderQuery.vue | `views/order/` | 乱码 | 待修复 |
| OrderStatistics.vue | `views/order/` | 乱码 | 待修复 |
| OrderRefund.vue | `views/order/` | 乱码 | 待修复 |
| OrderReservation.vue | `views/order/` | 乱码 | 待修复 |

### 4. 门店运营模块 (8个) - 优先级: 🟠 中

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| StoreArchive.vue | `views/store-ops/` | 乱码 + template不平衡 | 待修复 |
| StoreDailySettlement.vue | `views/store-ops/` | script不平衡 | 待修复 |
| StoreInventory.vue | `views/store-ops/` | 乱码 | 待修复 |
| StoreMaterialRequest.vue | `views/store-ops/` | 乱码 | 待修复 |
| StoreQueueHistory.vue | `views/store-ops/` | 乱码 | 待修复 |
| StoreRecruitment.vue | `views/store-ops/` | 乱码 + template不平衡 | 待修复 |
| StoreTableUsage.vue | `views/store-ops/` | 乱码 | 待修复 |
| ShiftManagement.vue | `views/store-ops/` | 乱码 | 待修复 |

### 5. 溯源管理模块 (5个) - 优先级: 🟠 中

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| TraceQuery.vue | `views/traceability/` | 乱码 | 待修复 |
| FoodTraceCode.vue | `views/traceability/` | 乱码 | 待修复 |
| MaterialTraceCode.vue | `views/traceability/` | 乱码 | 待修复 |
| TraceabilityQuality.vue | `views/traceability/` | 乱码 + template不平衡 | 待修复 |
| TraceabilityInspection.vue | `views/traceability/` | 乱码 | 待修复 |

### 6. 人事管理模块 (11个) - 优先级: 🟡 低

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| HREmployee.vue | `views/hr/` | - | ✅ 正常 |
| HROrganization.vue | `views/hr/` | - | ✅ 正常 |
| HRAttendance.vue | `views/hr/` | 乱码 | 待修复 |
| HRSalary.vue | `views/hr/` | 乱码 | 待修复 |
| HRRecruitment.vue | `views/hr/` | 乱码 + template不平衡 | 待修复 |
| HRKnowledgeBase.vue | `views/hr/` | 乱码 | 待修复 |
| KnowledgeIntelligence.vue | `views/hr/` | - | ✅ 正常 |
| HRTraining.vue | `views/hr/` | 乱码 + template不平衡 | 待修复 |
| HRHealthCertificate.vue | `views/hr/` | 乱码 | 待修复 |
| HRContract.vue | `views/hr/` | 乱码 + template不平衡 | 待修复 |
| ContractDashboard.vue | `views/hr/` | - | ✅ 正常 |
| HRContractTemplate.vue | `views/hr/` | 乱码 | 待修复 |
| ContractTemplateLibrary.vue | `views/hr/` | - | ✅ 正常 |
| HRConfigCenter.vue | `views/hr/` | - | ✅ 正常 |
| HRApproval.vue | `views/hr/` | - | ✅ 正常 |
| HRAnalytics.vue | `views/hr/` | - | ✅ 正常 |
| EmployeeIntelligence.vue | `views/hr/` | - | ✅ 正常 |
| HRInvitationCode.vue | `views/hr/` | - | ✅ 正常 |
| HRPosition.vue | `views/hr/` | 乱码 | 待修复 |
| HRJobLevel.vue | `views/hr/` | - | ✅ 正常 |
| HROnboarding.vue | `views/hr/` | 乱码 | 待修复 |
| KnowledgeStudyRecords.vue | `views/hr/` | 乱码 | 待修复 |

### 7. 资产管理模块 (6个) - 优先级: 🟡 低

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| AssetOverview.vue | `views/asset/` | 乱码 | 待修复 |
| AssetLedger.vue | `views/asset/` | 乱码 | 待修复 |
| AssetCategory.vue | `views/asset/` | - | ✅ 正常 |
| AssetDepreciation.vue | `views/asset/` | 乱码 | 待修复 |
| AssetInventory.vue | `views/asset/` | 乱码 + template不平衡 | 待修复 |
| AssetMaintenance.vue | `views/asset/` | 乱码 | 待修复 |
| AssetDisposal.vue | `views/asset/` | 乱码 | 待修复 |
| AssetReport.vue | `views/asset/` | - | ✅ 正常 |

### 8. 设备管理模块 (3个) - 优先级: 🟡 低

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| DeviceList.vue | `views/device/` | 乱码 | 待修复 |
| DeviceMonitor.vue | `views/device/` | - | ✅ 正常 |
| DeviceAlerts.vue | `views/device/` | 乱码 | 待修复 |
| DeviceStatusHistory.vue | `views/device/` | 乱码 | 待修复 |

### 9. 会员管理模块 (3个) - 优先级: 🟡 低

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| MemberOverview.vue | `views/marketing/` | - | ✅ 正常 |
| MemberList.vue | `views/marketing/` | 乱码 + template不平衡 | 待修复 |
| MemberDetail.vue | `views/marketing/` | 乱码 | 待修复 |
| MemberLevel.vue | `views/marketing/` | - | ✅ 正常 |
| RechargeManage.vue | `views/marketing/` | 乱码 | 待修复 |
| RechargeSettings.vue | `views/marketing/` | - | ✅ 正常 |

### 10. 系统管理模块 (5个) - 优先级: 🟡 低

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| OperationAudit.vue | `views/system/` | 乱码 | 待修复 |
| SystemSettings.vue | `views/system/` | - | ✅ 正常 |
| PermissionCenter.vue | `views/system/` | 乱码 (子组件) | 待修复 |
| AIModelConfig.vue | `views/system/` | 乱码 + template不平衡 | 待修复 |

### 11. 其他模块 (4个) - 优先级: 🟡 低

| 文件名 | 路径 | 损坏类型 | 状态 |
|--------|------|---------|------|
| DecisionBoard.vue | `views/operations/` | 乱码 | 待修复 |
| SealManagement.vue | `views/seal/` | 乱码 | 待修复 |
| SealUsageLogDialog.vue | `views/seal/components/` | 乱码 | 待修复 |
| SignLinkManagement.vue | `views/supplier-portal/` | 乱码 | 待修复 |

---

## 修复优先级排序

### 第一优先级（核心业务链路）
1. 采购管理 - 采购入库是库存数据的源头
2. 仓储管理 - 库存管理是核心
3. 订单管理 - 销售是库存扣减的触发点

### 第二优先级（运营支撑）
4. 门店运营
5. 溯源管理

### 第三优先级（辅助功能）
6. 人事管理
7. 资产管理
8. 设备管理
9. 会员管理
10. 系统管理
11. 其他

---

## 修复策略

### 参考模板
使用正常的页面作为修复参考模板：
- **主参考**: `views/product/FoodManagement.vue`（结构完整，功能齐全）
- **次参考**: `views/purchase/PurchaseAnalysis.vue`（同模块内正常文件）
- **简单参考**: `views/warehouse/InventoryWarning.vue`（结构简单）

### 修复步骤
1. 读取后端对应 Controller，了解 API 接口
2. 读取类型定义文件（`types/` 目录）
3. 读取 API 封装文件（`api/` 目录）
4. 参考正常页面的结构重建损坏页面
5. 确保使用项目规范的组件（StatusTag, DataTable, PageHeader等）
6. 确保使用 CSS 变量，不硬编码颜色
7. 确保对话框内弹出组件设置 `teleported=false`
8. 验证页面能正常打开和基本功能可用

---

## 修复记录

| 日期 | 模块 | 文件数 | 修复人 | 备注 |
|------|------|--------|--------|------|
| 2026-07-06 | - | 0 | - | 初始扫描完成 |
