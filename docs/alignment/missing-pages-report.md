# 前端页面与菜单对齐核查报告

> 核查日期：2026-07-30  
> 核查范围：`frontend/src/views/`、`frontend/src/router/index.ts`、`frontend/src/modules/*/menu.ts`  
> 说明：本报告仅做梳理，未修改源代码。

---

## 一、核查方法

1. **视图文件扫描**：遍历 `frontend/src/views/` 下所有业务模块目录，记录每个 `.vue` 页面文件。
2. **路由对齐**：读取 `frontend/src/router/index.ts`，提取所有非重定向路由的 `path` 与对应组件文件。
3. **菜单对齐**：读取 `frontend/src/modules/*/menu.ts`，提取所有菜单项的 `title` 与 `path`。
4. **交叉比对**：
   - 用户期望项 vs 实际存在的页面组件
   - 页面组件 vs 路由
   - 菜单项 vs 路由
   - 孤立页面（有文件但无路由/无菜单）
5. **代码有效性抽检**：对采购「到货登记」、仓储「库存入库/库存报损/库存预警」、签约链接管理等页面进行内容抽检，确认非空文件且具备完整模板与脚本。

---

## 二、采购模块页面完整性

用户提到可能缺少：**「签约链接管理」「到货登记」**。

| 用户期望项 | 实际页面文件 | 路由 | 菜单标题 | 状态 | 备注 |
|---|---|---|---|---|---|
| 到货登记 | `views/purchase/PurchaseStockin.vue` | `/purchase/stockin` | 到货登记 | ✅ 已存在 | 完整实现，支持到货单增删改查、物流编辑、关闭、详情 |
| 签约链接管理 | `views/supplier-portal/SignLinkManagement.vue` | `/supplier-portal/links` | 签署链接 | ✅ 已存在 | 完整实现，支持生成/续期/作废/删除/复制链接 |

**结论**：采购模块中用户提到的两个功能均**已存在**。其中「签约链接管理」的菜单标题为「签署链接」，路径为 `/supplier-portal/links`，属于命名/分组差异，并非缺失。

### 采购模块全部页面一览（15 项）

| 菜单标题 | 路由 | 页面文件 | 状态 |
|---|---|---|---|
| 商品分类 | `/purchase/material-category` | `MaterialCategory.vue` | ✅ |
| 商品档案 | `/purchase/archive` | `PurchaseArchive.vue` | ✅ |
| 供应商档案 | `/purchase/supplier` | `SupplierArchive.vue` | ✅ |
| 物资需求提报 | `/purchase/material-request` | `MaterialRequest.vue` | ✅ |
| 采购申请 | `/purchase/request` | `PurchaseRequest.vue` | ✅ |
| 采购计划 | `/purchase/plan` | `PurchasePlan.vue` | ✅ |
| 采购订单 | `/purchase/orders` | `PurchaseOrder.vue` | ✅ |
| 到货登记 | `/purchase/stockin` | `PurchaseStockin.vue` | ✅ |
| 采购退货 | `/purchase/return` | `PurchaseReturn.vue` | ✅ |
| 采购合同 | `/purchase/contract` | `PurchaseContract.vue` | ✅ |
| 电子合同 | `/purchase/electronic-contract` | `ElectronicContract.vue` | ✅ |
| 签署链接 | `/supplier-portal/links` | `supplier-portal/SignLinkManagement.vue` | ✅ |
| 采购结算 | `/purchase/settlement` | `PurchaseSettlement.vue` | ✅ |
| 采购报表 | `/purchase/report` | `PurchaseReport.vue` | ✅ |
| 采购数据分析 | `/purchase/analysis` | `PurchaseAnalysis.vue` | ✅ |

---

## 三、仓储模块页面完整性

用户提到可能缺少以下 11 项。经核查，**全部存在**。

| 期望项 | 实际页面文件 | 路由 | 菜单标题 | 状态 | 备注 |
|---|---|---|---|---|---|
| 库存管理 | `views/product/Inventory.vue` | `/warehouse/inventory` | 库存管理 | ✅ 已存在 | 组件复用自 product 模块 |
| 库存入库 | `views/warehouse/InventoryStockin.vue` | `/warehouse/inventory-stockin` | 库存入库 | ✅ 已存在 | 完整实现 |
| 库存出库 | `views/warehouse/InventoryOutbound.vue` | `/warehouse/outbound` | 库存出库 | ✅ 已存在 | 完整实现 |
| 库存调拨 | `views/warehouse/InventoryTransfer.vue` | `/warehouse/transfer` | 库存调拨 | ✅ 已存在 | 完整实现 |
| 库存调整 | `views/warehouse/InventoryAdjust.vue` | `/warehouse/adjust` | 库存调整 | ✅ 已存在 | 完整实现 |
| 库存报损 | `views/warehouse/InventoryLoss.vue` | `/warehouse/inventory-loss` | 库存报损 | ✅ 已存在 | 完整实现，含新建/审批/处理/详情 |
| 库存盘点 | `views/warehouse/InventoryCheck.vue` | `/warehouse/check` | 库存盘点 | ✅ 已存在 | 完整实现 |
| 库存预警 | `views/warehouse/InventoryWarning.vue` | `/warehouse/warning` | 库存预警 | ✅ 已存在 | 完整实现，支持处理与生成预警 |
| 库位管理 | `views/warehouse/InventoryLocation.vue` | `/warehouse/location` | 库位管理 | ✅ 已存在 | 完整实现 |
| 库存报表 | `views/warehouse/InventoryReport.vue` | `/warehouse/report` | 库存报表 | ✅ 已存在 | 完整实现 |
| 智能补货建议 | `views/warehouse/SmartRestock.vue` | `/warehouse/smart-restock` | 智能补货建议 | ✅ 已存在 | 完整实现 |

**额外说明**：仓储菜单中实际还有第 12 项「门店库存查看」(`/warehouse/store-inventory`)，对应 `views/warehouse/StoreInventory.vue`，不在用户列出的 11 项中。

---

## 四、全局遗漏 / 孤立页面扫描

### 4.1 跨模块对齐结果

遍历 `frontend/src/views/` 下所有业务模块，与路由、菜单交叉比对后：

- **有菜单、有路由、有页面**：覆盖采购、仓储、产品、订单、运营、门店、会员、财务、资产、人事、追溯、设备、签章、系统共 14 个模块。
- **有路由、无菜单（合理场景）**：登录页 `/login`、公司初始化向导 `/company-init`、个人中心 `/personal-center`、H5 供应商签署 `/portal/sign`、UI 组件参考手册 `/demo/component-gallery`、重定向路由。这些属于公开页、向导页或系统页，无需菜单。
- **有菜单、无路由/页面**：未发现。
- **有页面文件、无路由、无菜单**：发现 4 个孤立页面，详见下表。

### 4.2 孤立页面列表

| 页面文件 | 原路径/预期路径 | 路由状态 | 菜单状态 | 说明 | 建议 |
|---|---|---|---|---|---|
| `views/self-service/SelfServicePortal.vue` | 无 | ❌ 无路由 | ❌ 无菜单 | 员工自助门户完整页面（~1116 行），但 `router/index.ts` 与所有菜单均未引用 | 如需上线，补充路由与菜单；如已废弃，建议移除或归档 |
| `views/store-ops/StoreOperationLog.vue` | `/store-ops/operation-log` | ⚠️ 已重定向至 `/system/operation-audit` | ❌ 无菜单 | 页面文件仍存在，但路由改为跳转系统操作审计 | 如新版审计页已覆盖功能，建议删除该文件 |
| `views/store-ops/StoreStatusOverview.vue` | `/store-ops/status-overview` | ⚠️ 已重定向至 `/operations/live-monitor` | ❌ 无菜单 | 页面文件仍存在，但路由改为跳转运营实时监控 | 如运营实时监控已覆盖，建议删除该文件 |
| `views/hr/KnowledgeStudyRecords.vue` | 无 | ❌ 无路由 | ❌ 无菜单 | 与 `views/hr/components/KnowledgeStudyRecords.vue` 同名，但视图层版本未被任何文件 import | 确认是否重复：如组件版已满足需求，建议删除视图版；如需独立页面，补充路由/菜单 |

### 4.3 其他需关注项

- **`views/product/Inventory.vue` 被仓储模块复用**：路由 `/warehouse/inventory` 指向 `views/product/Inventory.vue`，菜单为「库存管理」。这种跨模块复用是允许的，但需在后续重构时关注职责边界。
- **系统模块组件页**：`views/system/components/*` 与 `views/system/components/domain-permission/*` 为 `SystemSettings.vue`、`PermissionCenter.vue` 等页面的内部 Tab 组件，不独立对外，因此不需要单独路由/菜单，符合设计。

---

## 五、建议补全 / 清理清单

基于「仅梳理、不修改」原则，提出以下待决策事项：

### 5.1 无需补全（已存在）

| 用户感知缺失项 | 实际位置 | 说明 |
|---|---|---|
| 到货登记 | `/purchase/stockin` → `PurchaseStockin.vue` | 已在采购菜单中 |
| 签约链接管理 | `/supplier-portal/links` → `SignLinkManagement.vue` | 菜单标题为「签署链接」，分组在采购管理下 |
| 仓储 11 项 | `/warehouse/*` | 全部存在 |

### 5.2 建议清理或确认归属

| 文件 | 建议操作 | 优先级 |
|---|---|---|
| `frontend/src/views/self-service/SelfServicePortal.vue` | 确认是否为员工端保留页面。若保留，需新增路由与菜单；若废弃，建议删除 | 中 |
| `frontend/src/views/store-ops/StoreOperationLog.vue` | 路由已重定向，文件可删除 | 低 |
| `frontend/src/views/store-ops/StoreStatusOverview.vue` | 路由已重定向，文件可删除 | 低 |
| `frontend/src/views/hr/KnowledgeStudyRecords.vue` | 与 `components/KnowledgeStudyRecords.vue` 重复，确认后删除视图版或补充独立路由 | 低 |

### 5.3 如后续确实需要新增页面

当前核查未发现采购/仓储模块有实际缺失。若业务上仍需扩展，可参考以下规范：

- 新增页面文件至 `frontend/src/views/{module}/{PageName}.vue`
- 在 `frontend/src/router/index.ts` 中添加路由，并设置 `meta.title` 与 `domain`
- 在对应 `frontend/src/modules/{module}/menu.ts` 中添加菜单项
- 确保路径命名与现有风格一致（kebab-case 路径、PascalCase 组件名）

---

## 六、核心发现摘要

1. **用户提到的采购「到货登记」「签约链接管理」均已在项目中实现**，并非缺失。其中「签约链接管理」在菜单中显示为「签署链接」，路径为 `/supplier-portal/links`。
2. **用户提到的仓储 11 项全部存在**，且均具备非空页面文件、完整路由与菜单配置。
3. **全局扫描发现 4 个孤立/待清理页面**：
   - `views/self-service/SelfServicePortal.vue`（无路由无菜单）
   - `views/store-ops/StoreOperationLog.vue`（路由已重定向）
   - `views/store-ops/StoreStatusOverview.vue`（路由已重定向）
   - `views/hr/KnowledgeStudyRecords.vue`（与组件重复且未被引用）
4. **当前未发现「有菜单无路由」或「有路由无页面」的功能性遗漏**。

---

*报告文件路径：`P:\my-new-project\docs\alignment\missing-pages-report.md`*
