# 前端验证计划

> 文档用途：统一前端页面验收标准，聚焦“表格 ↔ 对话框 ↔ 上游数据”之间的继承、更新与流转正确性。
> 验证方式：以真实浏览器操作为主，结合 Mock/真实 API 响应逐页核对；后端接口仅用于构造可验证的数据状态，不替代前端验证。
> 当前阶段：开发调试阶段（验证码、限流已临时关闭，验收完成后恢复）。

---

## 一、验证目标

1. **页面完整性**：菜单、导航、主题、通知、个人中心、设置等全局元素正常展示，右侧内容区可完整呈现。
2. **数据一致性**：表格中展示的数据与来源一致；点击行打开对话框后，对话框字段与表格行/上游单据一一对应。
3. **状态流转正确性**：状态标签、操作按钮、表单禁用规则与后端状态机一致；操作后列表与详情同步刷新。
4. **设计合规性**：符合项目 CSS 变量、组件使用规范（StatusTag、link 按钮、dialog 内 teleported=false 等）。
5. **可追溯性**：记录每页数据“从哪来、到哪去、是否更新”，形成可复用的验收清单。

---

## 二、验证范围

覆盖前端已修复并可访问的全部模块页面（详见 [frontend-repair-record.md](./frontend-repair-record.md)）：

| 模块 | 主页面 | 重点关注对话框/抽屉 |
|---|---|---|
| 工作台 | 工作台 | 详情/表单/操作对话框 |
| 产品中心 | 菜品管理、菜品分类、套餐管理、菜品定价、菜品成本分析 | 分类对话框、定价对话框、套餐对话框、批量定价对话框、价格历史对话框、成本管理对话框、BOM 报表对话框 |
| 订单管理 | 订单查询、订单统计、退款管理、预约管理 | 订单详情抽屉、退款详情抽屉、预约详情抽屉 |
| 运营中心 | 运营总览、门店档案、实时监控、经营分析、运营策略、预警管理、经营报表 | 导出任务抽屉、详情/表单/操作对话框 |
| 仓储管理 | 库存概览、库存管理、库存入库、库存出库、库存调拨、库存调整、库存报损、库存盘点、库存预警、库位管理、门店库存查看、库存报表、智能补货建议 | 各单据表单/详情/审批对话框（入库、出库、调拨、调整、报损、盘点）、处理预警对话框、库位/库存/补货建议对话框 |
| 采购管理 | 商品分类、商品档案、供应商档案、物资需求提报、采购申请、采购计划、采购订单、到货登记、采购退货、采购合同、电子合同、签署链接、采购结算、采购报表、采购数据分析 | 采购订单表单/审批/追溯对话框、到货登记/详情/关闭对话框、电子合同表单/详情/签署确认/作废确认对话框、供应商/商品/分类表单对话框、签署链接表单/详情对话框 |
| 门店管理 | 待办事项、门店收货、门店库存、物资需求、日结对账、证件管理、排班管理、门店招聘、桌台记录、叫号记录 | 证件续期对话框、详情/表单/操作对话框 |
| 会员管理 | 会员概览、会员列表、会员等级、储值管理、储值系统设置 | 会员详情对话框、会员偏好对话框 |
| 财务中心 | 财务总账、会计科目、会计期间、应收账款、应付账款、成本管理、税务管理、发票报销、财务报表、预算管理、资金管理、财务审批、自动凭证管理 | 付款/收款/历史对话框、成本表单对话框、预算表单对话框、凭证表单对话框 |
| 资产管理 | 资产概览、资产台账、资产分类、资产折旧、资产盘点、资产维护、资产处置、资产报表 | 详情/表单/操作对话框 |
| 人事管理 | 员工管理、组织架构、岗位管理、考勤排班、招聘管理、入职办理、培训发展、健康证管理、合同智能管理、合同管理、合同模板、合同模板库、薪资管理、合规审批、知识库智能、人事分析、员工画像、邀请码管理、配置中心 | 员工/岗位/组织表单详情对话框、合同创建/详情/变更/验签/签署/企业签章对话框、纸质合同扫描上传对话框、合同模板库抽屉、合同智能管理抽屉 |
| 食品追溯 | 临期预警、追溯查询、原料追溯、食品追溯、追溯链展示、供应商追溯、检验记录、标签模板、召回管理、质量追溯 | 详情/表单/操作对话框 |
| 设备管理 | 设备列表、设备监控、设备告警、状态历史 | 详情/表单/操作对话框 |
| 电子签章 | 印章管理 | 印章表单对话框、印章使用日志对话框 |
| 系统管理 | 权限中心、系统配置、AI 模型配置、操作审计 | 详情/表单/操作对话框 |

---

## 三、通用检查项（所有页面）

| 编号 | 检查项 | 通过标准 | 验证方法 |
|------|--------|----------|----------|
| G-01 | 页面能完整加载，无白屏、无 404 | 路由正确，内容区可见 | 浏览器访问并截图 |
| G-02 | 顶部导航栏可见 | 包含面包屑、搜索、通知、主题切换、设置、用户信息 | 截图核对 |
| G-03 | 侧边菜单可见且可点击 | 菜单项与权限匹配，子菜单可展开 | 逐一点击 |
| G-04 | 主题切换生效 | 浅色/深色模式下文字、背景、边框颜色正常 | 切换主题并截图 |
| G-05 | 全局操作不导致布局错乱 | 打开/关闭对话框、抽屉后页面不抖动、不偏移 | 反复操作 |
| G-06 | 使用 StatusTag 显示状态 | 未出现 `el-tag` 硬编码状态映射 | 代码审查 + 截图 |
| G-07 | 操作列使用 link 按钮 | 未使用 `text` 按钮或图标+文字组合 | 截图核对 |
| G-08 | 颜色/间距使用 `--fts-*` 变量 | 无 `#409EFF` 等硬编码颜色 | 代码审查 |
| G-09 | dialog 内弹出组件设置 `teleported="false"` | select/date-picker/cascader 等不漂移 | 打开 dialog 并滚动 |
| G-10 | 无 `console.log/info/debug` 调试代码 | 构建通过，控制台无调试输出 | `npm run build` + 控制台 |

---

## 四、数据溯源与流转检查清单

### 4.1 检查方法说明

对每一页执行以下四维交叉验证：

1. **来源（From）**：表格/表单字段来自哪张上游单据或哪个 API。
2. **展示（Show）**：表格列、对话框字段是否正确显示来源数据（含 DataConverter 转换）。
3. **操作（Action）**：在对话框中修改字段后，是否通过正确 API 回写，并刷新相关页面。
4. **下游（To）**：该单据状态变更后，下游页面/状态是否同步更新。

具体执行步骤为：

1. **读取表格列定义**：在业务页面 `<script setup>` 中找到 `columns`（或 `ColumnDef[]`），记录所有 `prop`。
2. **读取对话框/抽屉字段**：打开对应 `FormDialog`、`DetailDrawer` 或详情页，记录所有 `formData`、`descriptions` 项。
3. **读取 API 文件**：查看 `frontend/src/api/{module}/index.ts`（或细分文件如 `food.ts`、`order.ts`），确认 create/update/delete/status 等回写接口及字段映射。
4. **读取 TS 类型**：查看 `frontend/src/types/{module}.ts`，确认字段是否声明、类型是否一致。
5. **交叉比对**：表格列 ↔ 对话框字段 ↔ API 字段 ↔ TS 类型，标记不一致、缺失、无回写 API 项。
6. **特别关注**：状态字段（status）、金额字段（元/分转换）、日期字段、关联单据号字段（requestNo/orderNo/stockinNo 等）、操作人/时间字段、枚举/字典字段。

#### 状态图例

| 状态符号 | 含义 | 使用场景 |
|---------|------|---------|
| ✅ | 一致 | 表格列、对话框字段、API 字段、TS 类型均存在且命名/类型对齐 |
| ⚠️ | 命名不一致 | 字段存在但命名不统一（如 `foodId`/`productId`/`materialId`） |
| ❌ 表格有但对话框无 | 表格展示字段在表单/详情对话框中缺失 |
| ❌ 对话框有但表格无 | 表单/详情字段未在表格列中展示 |
| ❌ 无回写 API | 字段在页面存在，但无对应的 create/update/status 接口回写 |
| ❌ TS 类型缺失 | 表格或对话框使用字段，但 `types/` 中未声明 |

### 4.2 全模块字段溯源与流转检查

#### 4.2.1 采购管理

##### 采购订单页（PurchaseOrder.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| orderNo | ✅ | ✅ | ✅ | ✅ | ✅ | 单据编号，下游收货/结算引用 |
| requestNo | ✅ | ✅ | ✅ | ✅ | ✅ | 来源采购申请编号 |
| supplierId/supplierName | ✅ | ✅ | ✅ | ✅ | ✅ | 供应商信息 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 前端 11 状态 vs 后端 7 状态，部分就近映射 |
| paymentStatus | ✅ | ✅ | ❌ 无回写 API | ✅ | ❌ 无回写 API | 由财务付款回写，订单页无直接更新接口 |
| totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 后端分 → 前端元 |
| paidAmount | ✅ | ✅ | ❌ 后端未返回 | ✅ | ❌ 无回写 API | 当前被硬编码为 0 |
| priority | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 表格有但对话框无 | 后端未返回该字段 |
| purchaseType | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 表格有但对话框无 | 后端未返回该字段 |
| sourceType | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 表格有但对话框无 | 后端未返回该字段 |
| contractId/contractNo | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 表格有但对话框无 | 合同关联字段未落地 |
| budgetId/budgetStatus | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 表格有但对话框无 | 预算关联字段未落地 |
| contactPerson/contactPhone | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 表格有但对话框无 | 联系人信息未落地 |
| warehouseId | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 收货仓库仅在表单中维护 |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |
| items（明细） | ✅ | ✅ | ✅ | ✅ | ✅ | 物料/规格/数量/单价/金额 |
| approvedBy/approvedTime | ❌ 表格无 | ✅ | ❌ 后端无审批字段 | ✅ | ❌ 无回写 API | 后端未返回审批人/时间 |
| deletedBy/deletedTime | ❌ 表格无 | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ TS 类型缺失 | 类型存在但数据未返回 |

**上下游流转**：

```text
采购申请（审批通过） → 生成采购订单（requestNo 继承） → 订单确认 → 采购收货（按订单明细收货）
                                                          ↓
                                                  采购结算 → 财务应付/付款 → 付款状态回写订单
```

> 原清单补充说明：订单编号在表格、详情状态横幅、描述列表中展示，需唯一并与表格一致；来源采购申请号转单后不可为空，并显示“申请转单”标签；订单状态标签与枚举一致，决定操作按钮可见性；付款状态需与 FinancePayable 数据一致；总金额需等于明细金额之和，作为财务应付金额来源；审批记录需与 approval_workflow 节点、审批人、意见一致。

##### 采购收货页（PurchaseStockin.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| stockinNo | ✅ | ✅ | ✅ | ✅ | ✅ | 收货单号 |
| orderNo | ✅ | ✅ | ✅ | ✅ | ✅ | 关联采购订单 |
| supplierName | ✅ | ✅ | ✅ | ✅ | ✅ | 供应商 |
| warehouseId/storeId | ✅ | ✅ | ✅ | ✅ | ✅ | 收货仓库/门店 |
| qualityStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 质检状态 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 收货状态 |
| totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 金额分→元 |
| freightAmount | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 运费字段 |
| items（收货明细） | ✅ | ✅ | ✅ | ✅ | ✅ | 实收数量回写订单 |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |

**上下游流转**：

```text
采购订单 → 采购收货单（带出订单明细） → 质检 → 确认入库 → 仓库/门店库存增加 → 生成库存流水/成本记录
```

> 原清单补充说明：收货单号独立编号；关联订单只能选择已确认/已发货订单；收货明细中物料、规格与订单一致，可修改实收数量；质检状态影响能否确认入库；确认入库后更新库存，收货数量回写订单明细 `receivedQuantity`。

##### 采购申请页（PurchaseRequest.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| requestNo | ✅ | ✅ | ✅ | ✅ | ✅ | 申请单号 |
| title | ✅ | ✅ | ✅ | ✅ | ✅ | 申请标题 |
| departmentId/departmentName | ✅ | ✅ | ✅ | ✅ | ✅ | 申请部门 |
| applicantId/applicantName | ✅ | ✅ | ✅ | ✅ | ✅ | 申请人 |
| totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 总金额 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 语义字符串，与计划/订单数字编码策略不一致 |
| priority | ✅ | ✅ | ✅ | ✅ | ✅ | 优先级 |
| expectedDate | ✅ | ✅ | ✅ | ✅ | ✅ | 期望日期 |
| budgetId/budgetStatus | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 表格有但对话框无 | 预算字段未落地 |
| approvedByName/approvedTime | ✅ | ✅ | ✅ | ✅ | ✅ | 已统一为 `approvedByName`/`approvedTime` |
| items（明细） | ✅ | ✅ | ✅ | ✅ | ✅ | 采购申请已统一为 `materialId/materialName/materialCode`；API converter 负责与后端 `foodId/foodName/foodCode` 映射 |

##### 采购计划页（PurchasePlan.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| planNo | ✅ | ✅ | ✅ | ✅ | ✅ | 计划编号 |
| departmentId/departmentName | ✅ | ✅ | ✅ | ✅ | ✅ | 部门 |
| totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 总金额 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 数字编码 + Converter |
| planDate | ✅ | ✅ | ✅ | ✅ | ✅ | 计划日期 |
| approvedByName/approvedTime | ❌ 表格无 | ✅ | ✅ | ✅ | ✅ | 后端返回 `approvedByName`/`approveTime`，converter 统一映射为 `approvedByName`/`approvedTime` |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |
| items（明细） | ✅ | ✅ | ✅ | ✅ | ⚠️ | `estimatedPrice` 与订单 `unitPrice` 命名不统一 |

##### 采购结算页（PurchaseSettlement.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| settlementNo | ✅ | ✅ | ✅ | ✅ | ✅ | 结算单号 |
| orderNo/orderCode | ✅ | ✅ | ✅ | ✅ | ✅ | 关联订单 |
| supplierName | ✅ | ✅ | ✅ | ✅ | ✅ | 供应商 |
| totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 结算金额 |
| paidAmount/unpaidAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 已付/未付 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 中文标签不一致：待付款 vs 待结算 |
| dueDate | ✅ | ✅ | ✅ | ✅ | ✅ | 到期日 |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |

##### 供应商档案页（SupplierArchive.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| supplierCode | ✅ | ✅ | ✅ | ✅ | ✅ | 供应商编码 |
| supplierName | ✅ | ✅ | ✅ | ✅ | ✅ | 供应商名称 |
| contactPerson/contactPhone | ✅ | ✅ | ✅ | ✅ | ✅ | 联系人 |
| settlementMethod/paymentTerms | ✅ | ✅ | ✅ | ✅ | ✅ | 结算方式/账期 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |
| bankAccount/taxNo/address | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 详情/表单字段未在列表展示 |

> 原清单补充说明：供应商名称/编码在表格、表单、详情中展示，编码需唯一；结算方式/账期在详情、采购订单详情中展示，影响财务账期计算；状态禁用后不可新建订单，历史单据保留。

**采购管理 · 已修复/待修复项**

1. **采购订单类型大量字段后端未返回**：`requestId/requestNo/contractId/contractNo/sourceType/priority/purchaseType/contactPerson/contactPhone/paidAmount/budgetId/budgetStatus/updateBy/deletedTime/deletedBy` 当前由 converter 提供默认空值/0，视图以 `|| '-'` 降级展示。如需真实数据，需后端实体/DTO 补充对应字段。
2. **采购订单状态前后端数量不匹配**：前端 11 个状态，后端 7 个状态，`shipped`/`received`/`rejected`/`terminated` 为近似映射（已在 converter 中处理）。
3. **物料字段命名严重不统一**：✅ 采购申请已统一为 `materialId/materialName/materialCode`（API 层仍与后端 `foodId/foodName/foodCode` 映射）；采购计划/到货已使用 `materialId/materialName`；采购订单仍用 `productId/productName`，待后续评估是否统一。
4. **金额转换方式不一致**：采购申请明细金额为 BigDecimal（元）直接使用；采购申请总金额及采购计划/订单使用 `utils/money` 分↔元转换。当前按各后端字段实际单位处理，符合现状。
5. **审批字段命名不统一**：✅ 已统一为 `approvedByName`/`approvedTime`（采购计划后端字段 `approveTime` 由 converter 映射）。
6. **采购申请状态为语义字符串，其余单据为数字编码**，策略不一致（设计如此，采购申请与其他单据状态机差异较大，当前保留）。

#### 4.2.2 仓储管理

##### 库存调拨页（InventoryTransfer.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| transferId | ✅ | ✅ | ✅ | ✅ | ✅ | 调拨单ID |
| transferCode | ✅ | ✅ | ✅ | ✅ | ✅ | 调拨单号 |
| fromWarehouseId/fromWarehouseName | ✅ | ✅ | ✅ | ✅ | ✅ | 调出仓库 |
| toWarehouseId/toWarehouseName | ✅ | ✅ | ✅ | ✅ | ✅ | 调入仓库 |
| materialId/materialName | ✅ | ✅ | ✅ | ✅ | ✅ | 物料（顶层快捷字段） |
| quantity | ✅ | ✅ | ✅ | ✅ | ✅ | 数量 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 调拨状态 |
| applyUserId/applyUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 申请人 |
| approveUserId/approveUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 审批人 |
| applyTime | ✅ | ✅ | ✅ | ✅ | ✅ | 申请时间 |
| approveTime | ✅ | ✅ | ✅ | ✅ | ✅ | 审批时间 |
| executeTime | ✅ | ✅ | ✅ | ✅ | ✅ | 执行时间 |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |
| items（明细） | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 顶层单物料与明细并存，存在冗余 |

##### 库存盘点页（InventoryCheck.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| checkId | ✅ | ✅ | ✅ | ✅ | ✅ | 盘点单ID |
| checkCode | ✅ | ✅ | ✅ | ✅ | ✅ | 盘点单号 |
| title | ✅ | ✅ | ⚠️ 后端 `remark` 回填 | ✅ | ⚠️ | 标题由 `remark` 或单号回填 |
| warehouseId/warehouseName | ✅ | ✅ | ✅ | ✅ | ✅ | 仓库 |
| checkStatus/status | ✅ | ✅ | ✅ | ✅ | ✅ | 已新增 `cancelled` 状态并映射到后端保留值 4，避免误映射到 `2=已审核` |
| checkType | ✅ | ✅ | ✅ | ✅ | ✅ | 盘点类型 |
| plannedStartTime/plannedEndTime | ✅ | ✅ | ❌ 后端只有 `checkDate` | ✅ | ❌ 无回写 API | 计划起止时间后端缺失 |
| actualStartTime/actualEndTime | ✅ | ✅ | ⚠️ 用 `approveTime` 推断 | ✅ | ⚠️ | 实际结束时间语义不一致 |
| createUserId/createUserName | ✅ | ✅ | ⚠️ 后端无 `creatorName` | ✅ | ⚠️ | 盘点用 `createUserId/Name`，与调拨 `applyUserId/Name` 不统一 |
| approveUserId/approveUserName | ✅ | ✅ | ⚠️ 后端无 `auditorName` | ✅ | ⚠️ | 审批人名称缺失 |
| totalAssetsCount/totalAssetCount | ✅ | ✅ | ✅ | ✅ | ⚠️ | 命名不一致 |
| actualCount/countedCount | ✅ | ✅ | ✅ | ✅ | ⚠️ | 命名不一致 |
| overageCount/shortageCount | ✅ | ✅ | ❌ 后端只有 `diffCount` | ✅ | ❌ 无回写 API | 盘盈/盘亏拆分后端不支持 |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |
| items（明细） | ✅ | ✅ | ✅ | ✅ | ✅ | 明细 |

##### 库存出库页（InventoryOutbound.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| outboundId | ✅ | ✅ | ✅ | ✅ | ✅ | 出库单ID |
| outboundCode | ✅ | ✅ | ✅ | ✅ | ✅ | 出库单号 |
| warehouseId/warehouseName | ✅ | ✅ | ✅ | ✅ | ✅ | 仓库 |
| totalQuantity | ✅ | ✅ | ✅ | ✅ | ✅ | 总数量 |
| totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 总金额（元字符串） |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 出库状态 |
| applyUserId/applyUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 申请人 |
| approveUserId/approveUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 审批人 |
| applyTime/outboundDate | ✅ | ✅ | ✅ | ✅ | ⚠️ | 时间字段命名不统一 |
| completeTime | ✅ | ✅ | ✅ | ✅ | ✅ | 完成时间 |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |
| items（明细） | ✅ | ✅ | ✅ | ✅ | ✅ | 物料/数量/金额 |

##### 库存报损页（InventoryLoss.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| lossId | ✅ | ✅ | ✅ | ✅ | ✅ | 报损单ID |
| lossCode | ✅ | ✅ | ✅ | ✅ | ✅ | 报损单号 |
| warehouseId/warehouseName | ✅ | ✅ | ✅ | ✅ | ✅ | 仓库 |
| totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 总金额 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 报损状态 |
| applyUserId/applyUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 申请人 |
| approveUserId/approveUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 审批人 |
| applyTime | ✅ | ✅ | ✅ | ✅ | ✅ | 申请时间 |
| processTime | ✅ | ✅ | ✅ | ✅ | ✅ | 处理时间 |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |
| items（明细） | ✅ | ✅ | ✅ | ✅ | ✅ | 明细 |

##### 库存调整页（InventoryAdjust.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| adjustId | ✅ | ✅ | ✅ | ✅ | ✅ | 调整单ID |
| adjustCode | ✅ | ✅ | ✅ | ✅ | ✅ | 调整单号 |
| warehouseId/warehouseName | ✅ | ✅ | ✅ | ✅ | ✅ | 仓库 |
| totalAdjustQuantity | ✅ | ✅ | ✅ | ✅ | ✅ | 总调整数量 |
| totalAdjustAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 总调整金额 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 调整状态 |
| applyUserId/applyUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 申请人 |
| approveUserId/approveUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 审批人 |
| applyTime | ✅ | ✅ | ✅ | ✅ | ✅ | 申请时间 |
| completeTime | ✅ | ✅ | ✅ | ✅ | ✅ | 完成时间 |
| remark | ✅ | ✅ | ✅ | ✅ | ✅ | 备注 |
| items（明细） | ✅ | ✅ | ✅ | ✅ | ✅ | 明细 |

##### 仓库/库位/库存

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| warehouseId/warehouseCode/warehouseName | ✅ | ✅ | ✅ | ✅ | ✅ | 仓库 |
| warehouseType | ✅ | ✅ | ✅ | ✅ | ✅ | 仓库类型 |
| capacity/usedCapacity | ✅ | ✅ | ✅ | ✅ | ✅ | 容量 |
| managerId/managerName | ✅ | ✅ | ✅ | ✅ | ✅ | 负责人 |
| locationId/locationCode/locationName | ✅ | ✅ | ✅ | ✅ | ✅ | 库位 |
| inventoryId | ✅ | ✅ | ✅ | ✅ | ✅ | 库存ID |
| materialId/materialName | ✅ | ✅ | ✅ | ✅ | ✅ | 物料 |
| quantity/availableQuantity/lockedQuantity | ✅ | ✅ | ✅ | ✅ | ✅ | 数量 |
| unitCost/totalCost | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 表格有但对话框无 | 库存成本后端未返回 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 库存状态 |

> 原清单补充说明：库存总览/门店库存中，物料名称需与采购、产品中心一致；数量/成本由入库增加、出库减少，需与库存流水一致；库存流水需记录每笔变动的来源单据号，作为审计、追溯依据。

##### 库存日志页（InventoryLog.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ✅ | 日志ID |
| materialId/materialName | ✅ | ✅ | ✅ | ✅ | ✅ | 已由 `productId/productName` 统一为 `materialId/materialName`，API converter 负责与后端映射 |
| warehouseId | ✅ | ✅ | ✅ | ✅ | ✅ | 仓库 |
| operationType | ✅ | ✅ | ✅ | ✅ | ✅ | 操作类型 |
| beforeStock/afterStock/changeAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 库存变动 |
| operatorId/operatorName | ✅ | ✅ | ✅ | ✅ | ⚠️ | 与单据申请人字段语义不同 |
| createTime | ✅ | ✅ | ✅ | ✅ | ✅ | 创建时间 |

##### 库存预警页（InventoryWarning.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| warningId | ✅ | ✅ | ✅ | ✅ | ✅ | 预警ID |
| warningType | ✅ | ✅ | ✅ | ✅ | ✅ | 预警类型 |
| materialId/materialName | ✅ | ✅ | ✅ | ✅ | ✅ | 物料 |
| currentStock/threshold | ✅ | ✅ | ✅ | ✅ | ✅ | 当前库存/阈值 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 使用 number 0/1，与仓储其他语义化字符串状态不一致 |
| handler/handlerName/handleTime | ✅ | ✅ | ✅ | ✅ | ✅ | 处理人/时间 |

**仓储管理 · 待修复项**

1. **ID 类型不统一**：仓储模块多用 string，但库存日志使用 `productId` 语义。
2. **物料字段命名不统一**：✅ 库存日志 `productId/productName` 已统一为 `materialId/materialName`，API converter 负责与后端映射。
3. **金额单位与类型不统一**：仓储库存/出库/报损/调整等单据金额字段为元字符串，后端 API 部分以分为单位，部分直接返回元字符串。
4. **盘点单字段语义不一致**：`title` 由 `remark` 回填；✅ `CANCELLED` 状态已新增并映射到后端保留值 4，不再误映射到 `2=已审核`；盘盈/盘亏拆分后端不支持。
5. **申请人/创建人字段不统一**：调拨/出库/报损/调整用 `applyUserId/Name`，盘点用 `createUserId/Name`，日志用 `operatorId/Name`。
6. **调拨单顶层单物料字段与明细并存**，存在数据冗余。

#### 4.2.3 溯源管理

##### 追溯码查询页（TraceQuery.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| traceCode | ✅ | ✅ | ✅ | ✅ | ✅ | 溯源码 |
| dishName/productName | ✅ | ✅ | ✅ | ✅ | ⚠️ | 表格 `dishName`，类型中 `productName` |
| batchNumber/orderNumber | ✅ | ✅ | ⚠️ | ✅ | ⚠️ | 表格列 `orderNumber` 实际对应 `batchNumber` |
| makeStartTime | ✅ | ✅ | ✅ | ✅ | ✅ | 生产日期 |
| serveTime | ✅ | ✅ | ✅ | ✅ | ✅ | 保质期/到期日 |
| createTime | ✅ | ✅ | ✅ | ✅ | ✅ | 生成时间 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | TS 中声明为 `string`，但已有 `TraceCodeStatus` 未使用 |
| productionDate/expiryDate | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 详情额外字段 |

##### 原料追溯码 / 食品追溯码

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ⚠️ | 溯源对象 ID 为 number，与仓储 string 不一致 |
| traceCode/traceCodeId | ✅ | ✅ | ✅ | ✅ | ⚠️ | `traceCodeId` 与 `traceCode` 混用 |
| productId/materialId | ✅ | ✅ | ✅ | ✅ | ⚠️ | `productId` 为 number，`dishId` 为 string |
| productName/materialName/dishName | ✅ | ✅ | ✅ | ✅ | ⚠️ | 命名不统一 |
| batchNumber/batchNo | ✅ | ✅ | ✅ | ✅ | ⚠️ | `batchNumber` 与 `batchNo` 混用 |
| supplierId/supplierName | ✅ | ✅ | ✅ | ✅ | ✅ | 供应商 |
| productionDate/expiryDate | ✅ | ✅ | ✅ | ✅ | ✅ | 生产日期/过期日期 |
| unitPrice/totalPrice | ✅ | ✅ | ✅ | ✅ | ✅ | 单价/金额（分） |
| materialCost/laborCost/totalCost | ✅ | ✅ | ✅ | ✅ | ✅ | 成本（分） |
| status/makeStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 状态/制作状态 |
| materialTraceCodes/materialDetails/relatedFoodTraceCodes | ❌ 表格无 | ✅ | ⚠️ JSON 字符串 | ✅ | ⚠️ | 关联信息为 JSON 字符串，结构未在类型中细化 |
| createTime/updateTime | ✅ | ✅ | ✅ | ✅ | ✅ | 时间 |

##### 质量记录 / 检验记录 / 临期预警

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| qualityRecordId/inspectionId/alertId | ✅ | ✅ | ✅ | ✅ | ✅ | 主键 |
| traceCode | ✅ | ✅ | ✅ | ✅ | ✅ | 关联追溯码 |
| materialId/materialName | ✅ | ✅ | ✅ | ✅ | ✅ | 物料 |
| batchNo | ✅ | ✅ | ✅ | ✅ | ✅ | 批次号 |
| abnormalLevel/handlingStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 质量记录状态 |
| inspectionResult/inspectionType | ✅ | ✅ | ✅ | ✅ | ✅ | 检验结果/类型 |
| alertLevel/handlingStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 预警等级/处理状态 |
| inspectionData/inspectionItems | ❌ 表格无 | ✅ | ⚠️ JSON 字符串 | ✅ | ⚠️ | 结构化数据前端仅透传 |
| supplierId/supplierName | ✅ | ✅ | ✅ | ✅ | ✅ | 供应商 |

##### 召回记录页（RecallRecord.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| recallId | ✅ | ✅ | ✅ | ✅ | ✅ | 召回ID |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 召回状态为语义字符串，但 `AffectedTraceCode.status` 为后端数字 |
| affectedTraceCodes | ✅ | ✅ | ✅ | ✅ | ⚠️ | 受影响追溯码状态字段类型不一致 |
| recallReason/handler/handleTime | ✅ | ✅ | ✅ | ✅ | ✅ | 召回原因/处理 |

**溯源管理 · 待修复项**

1. **ID 类型不统一**：溯源对象使用 number ID，与仓储 string ID 不一致；`productId` 为 number 但 `dishId` 为 string。
2. **状态字段表达不统一**：质量/检验/临期预警使用大写枚举字符串，与仓储小写语义化字符串风格不一致。
3. **时间字段命名不统一**：部分老接口使用 `createdAt/updatedAt`（`TraceCodeVO`、`TraceCodeLog`）。
4. **JSON 字段结构未细化**：`materialTraceCodes/materialDetails/relatedFoodTraceCodes/inspectionData/inspectionItems` 为 JSON 字符串，类型未明确定义结构。
5. **召回模块状态不一致**：`RecallRecord.status` 语义字符串，`AffectedTraceCode.status` 后端数字。

#### 4.2.4 门店运营

##### 门店档案页（StoreArchive.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| storeCode | ✅ | ✅ | ✅ | ✅ | ✅ | 门店编码 |
| storeName | ✅ | ✅ | ✅ | ✅ | ✅ | 门店名称 |
| storeType | ✅ | ✅ | ✅ | ✅ | ✅ | 门店类型 |
| area | ✅ | ✅ | ✅ | ✅ | ⚠️ | 前端下拉 6 城硬编码 |
| address | ✅ | ✅ | ✅ | ✅ | ✅ | 地址 |
| phone | ✅ | ✅ | ✅ | ✅ | ✅ | 联系电话 |
| managerId | ✅ | ✅ | ⚠️ 后端 String | ⚠️ 前端 number | ⚠️ | 类型不一致 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 营业状态 |
| openDate | ✅ | ✅ | ✅ | ✅ | ✅ | 开业日期 |
| businessHoursStart/businessHoursEnd | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 营业时间 |
| areaSize | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 面积 |
| licenseNo/licenseExpiry | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 许可证信息 |
| configJson | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 配置JSON |
| remark | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 备注 |
| createTime/updateTime | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 列表展示，表单无 |

##### 门店证件页（StoreCertificate.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| certName | ✅ | ✅ | ⚠️ 后端健康证实体 | ✅ | ⚠️ | 前端通用证照，后端仅健康证 |
| certType | ✅ | ✅ | ⚠️ 后端 5 种 vs 前端 7 种 | ✅ | ⚠️ | 类型不匹配 |
| holderName | ✅ | ✅ | ⚠️ 后端 employeeName | ✅ | ⚠️ | 持有人/单位语义错位 |
| issueDate/expiryDate | ✅ | ✅ | ✅ | ✅ | ✅ | 发证/到期日期 |
| daysLeft | ✅ | ❌ 对话框无 | ❌ 无回写 API | ✅ | ❌ 表格有但对话框无 | 前端计算字段 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | `revoked` 后端映射为 `expired` |
| expenseStatus | ✅ | ❌ 对话框无 | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 报销状态仅存内存 |
| fileUrl | ❌ 表格无 | ✅ | ⚠️ 后端 `certificateImage` | ✅ | ⚠️ | 字段命名不一致 |
| issuer | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 发证机关 |
| certNumber | ❌ 表格无 | ✅ | ⚠️ 后端 `certificateNumber` | ✅ | ⚠️ | 字段命名不一致 |

##### 门店库存页（StoreInventory.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| materialId | ✅ | ✅ | ✅ | ✅ | ✅ | 商品编码 |
| materialName | ✅ | ✅ | ✅ | ✅ | ✅ | 商品名称 |
| specification | ✅ | ✅ | ❌ 数据库无此列 | ✅ | ❌ 无回写 API | 数据库缺失 |
| unit | ✅ | ✅ | ✅ | ✅ | ✅ | 单位 |
| category | ✅ | ❌ 对话框无 | ⚠️ `(row as any).category` | ❌ TS 类型缺失 | ❌ TS 类型缺失 | 来源不明 |
| quantity | ✅ | ✅ | ⚠️ `quantity` vs `current_stock` | ✅ | ⚠️ | 字段命名不统一 |
| unitCost | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 无回写 API | 后端未返回 |
| totalCost | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 无回写 API | 前端计算，后端未返回 |
| warningThreshold | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 预警阈值 |
| status | ✅ | ❌ 对话框无 | ❌ 前端计算 | ✅ | ❌ 表格有但对话框无 | 库存状态前端计算 |
| updateTime | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 最后更新时间 |
| adjustType/quantity/reason | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 库存调整表单 |
| requestQuantity/expectedDate | ❌ 表格无 | ✅ | ⚠️ 复用 adjust API | ✅ | ❌ 无回写 API | 要货申请未独立建单 |

##### 门店要货页（StoreMaterialRequest.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| requestNo | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 申请单号 |
| storeName | ✅ | ✅ | ✅ | ✅ | ⚠️ | storeOptions 硬编码 mock |
| applicantName | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 申请人 |
| createTime | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 申请日期 |
| status | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 状态 |
| priority | ✅ | ✅ | ✅ | ✅ | ⚠️ | 编辑时被强制重置为 normal |
| itemCount | ✅ | ❌ 对话框无 | ❌ 前端计算 | ✅ | ❌ 表格有但对话框无 | 物资种类数 |
| title | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 申请标题 |
| expectedDate | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 期望到货日期 |
| description | ❌ 表格无 | ✅ | ⚠️ 后端 `remark` | ✅ | ⚠️ | 字段命名不一致 |
| items | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 明细 |

##### 待办任务页（StorePendingTasks.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| taskType | ✅ | ✅ | ✅ | ✅ | ✅ | 任务类型 |
| title | ✅ | ✅ | ✅ | ✅ | ✅ | 任务描述 |
| sourceModule | ✅ | ✅ | ⚠️ 后端 `source_type` | ✅ | ⚠️ | 字段命名不一致 |
| assigneeName | ✅ | ✅ | ✅ | ✅ | ✅ | 负责人 |
| deadline | ✅ | ✅ | ✅ | ✅ | ✅ | 截止时间 |
| priority | ✅ | ✅ | ✅ | ✅ | ⚠️ | 前后端数值映射方向需确认 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |
| redirectUrl | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 跳转链接 |

##### 日结对账页（StoreDailySettlement.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| settlementDate | ✅ | ✅ | ✅ | ✅ | ✅ | 日结日期 |
| storeName | ✅ | ✅ | ✅ | ✅ | ✅ | 门店 |
| totalRevenue | ✅ | ✅ | ✅ | ✅ | ✅ | 营业额（分→元） |
| orderCount | ✅ | ✅ | ✅ | ✅ | ✅ | 订单数 |
| refundAmount | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 无回写 API | 数据库无此列 |
| netProfit | ✅ | ✅ | ✅ | ✅ | ✅ | 实收金额 |
| auditorName | ✅ | ✅ | ⚠️ 后端 `auditor_name` | ✅ | ⚠️ | 表格列名“收银员”，字段 auditor 语义偏审计 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 日结状态 |
| confirmTime | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 无回写 API | 数据库无此列 |

> 原清单补充说明：班次管理页中，门店取自 `CURRENT_STORE_ID`（当前登录用户门店），不显示门店下拉，所有排班数据按门店隔离；员工只显示本店在职员工；班次类型为早/午/晚/通班/休 5 种，用于统计工时、出勤率；排班模板可一键套用生成实际排班计划；换班申请审批通过后更新排班。门店招聘页中，申请人来自 H5 报名/手动录入；取票码为后端生成 `FT-xxxxxx`（8-12 位字母数字），作为扫码签到唯一标识；面试阶段从门店面试进入 HR 面试，门店端不显示 HR 操作。

**门店运营 · 待修复项**

1. **门店档案**：`managerId` 前端 number 后端 String；区域下拉硬编码；导入/导出未实现。
2. **门店证件**：前后端模型严重错位（前端通用证照 vs 后端健康证）；续期费用审批仅存内存；`fileUrl/certNumber` 与后端字段命名不一致。
3. **门店库存**：`specification/category/unitCost/totalCost` 数据库缺失；要货申请复用 adjust API，未独立建单。
4. **门店要货**：`storeOptions` 硬编码 mock；编辑时 `priority` 被强制重置；`description` 对应后端 `remark`。
5. **待办任务**：`sourceModule` 映射后端 `source_type`；优先级数值映射方向待确认。
6. **日结对账**：`refundAmount`/`confirmTime` 数据库不存在；收银员字段命名语义偏审计。

#### 4.2.5 订单 / 销售

##### 订单查询页（OrderQuery.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| orderCode | ✅ | ✅ | ✅ | ✅ | ✅ | 订单号 |
| createTime | ✅ | ✅ | ✅ | ✅ | ✅ | 下单时间 |
| storeName | ✅ | ✅ | ✅ | ✅ | ✅ | 门店 |
| customerInfo | ✅ | ✅ | ⚠️ 拼接字段 | ✅ | ✅ | 顾客姓名+电话拼接 |
| itemsSummary | ✅ | ✅ | ✅ | ✅ | ✅ | 商品明细摘要 |
| finalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 订单金额（分→元） |
| paymentMethod | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ❌ TS 类型缺失 | ❌ TS 类型缺失 | 类型未定义，表格直接引用 |
| orderStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 订单状态 |
| items/payments/refunds | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 详情抽屉 |

##### 退款管理页（OrderRefund.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| refundNo | ✅ | ✅ | ✅ | ✅ | ✅ | 退款单号 |
| orderCode | ✅ | ✅ | ✅ | ✅ | ✅ | 关联订单号 |
| refundType | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ❌ TS 类型缺失 | ❌ TS 类型缺失 | 前端靠关键字推断，类型缺失 |
| refundAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 退款金额 |
| createTime | ✅ | ✅ | ✅ | ✅ | ✅ | 申请时间 |
| refundStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 退款状态 |
| applyUserName | ✅ | ❌ 对话框无 | ❌ 后端未返回 | ✅ | ❌ 无回写 API | 数据库无此列 |
| approveRemark | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 审核意见 |

##### 预约管理页（OrderReservation.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| reservationCode | ✅ | ✅ | ✅ | ✅ | ✅ | 预约编号 |
| customerName | ✅ | ✅ | ✅ | ✅ | ✅ | 预约人 |
| customerPhone | ✅ | ✅ | ✅ | ✅ | ✅ | 联系电话 |
| reservationDate | ✅ | ✅ | ✅ | ✅ | ✅ | 预约日期 |
| reservationTime | ✅ | ✅ | ✅ | ✅ | ⚠️ | 数据库迁移脚本被截断，字段未确认 |
| peopleCount | ✅ | ✅ | ✅ | ✅ | ✅ | 用餐人数 |
| tableInfo/tableId | ✅ | ✅ | ✅ | ✅ | ✅ | 桌号/桌台 |
| status | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 预约状态 |
| storeName/storeId | ✅ | ✅ | ✅ | ✅ | ⚠️ | 数据库字段未确认 |
| depositAmount | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 定金 |
| remark | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 备注 |

> 原清单补充说明：订单查询页中，订单号在表格、详情抽屉中展示，需唯一；桌台/门店需与 StoreTableUsage 一致，订单完成后释放桌台；订单金额需与支付金额一致，作为财务收入、成本结转来源；退款金额不超过原订单，退款后订单状态/金额回写。

**订单/销售 · 待修复项**

1. **订单查询**：`paymentMethod` 类型未定义，后端未返回；退款操作仅提示跳转，无真实退款创建 API。
2. **退款管理**：`refundType` 字段缺失，前端靠 `refundReason` 关键字推断；`applyUserName` 后端/数据库未返回。
3. **预约管理**：数据库迁移脚本 `table_reservations` 被截断，`reservation_time`、`store_id`、`deposit_amount` 字段未确认；编辑时 `storeId` 从桌台反推可能覆盖真实值。

#### 4.2.6 产品中心

##### 菜品管理页（FoodManagement.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| foodName | ✅ | ✅ | ✅ | ✅ | ✅ | 菜品名称 |
| categoryName | ✅ | ✅ | ✅ | ✅ | ✅ | 分类名称 |
| salePrice | ✅ | ✅ | ✅ | ✅ | ✅ | 售价（分→元） |
| costPrice | ✅ | ✅ | ✅ | ✅ | ✅ | 成本（分→元） |
| profitRate | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 利润率 |
| stock | ✅ | ✅ | ✅ | ✅ | ✅ | 库存 |
| foodStatus | ✅ | ✅ | ✅ | ✅ | ⚠️ | 表单字段 `foodStatus` 提交时转 `status` |
| createTime | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 创建时间 |
| foodCode | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 菜品编码 |
| categoryId | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 分类ID |
| minStock | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 最低库存 |
| imageUrl | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 图片 |
| description | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 描述 |
| cookingTime | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 制作时间 |
| ingredients | ❌ 表格无 | ✅ | ⚠️ 后端 `recipes` | ✅ | ⚠️ | 前端 `ingredients`，后端 `recipes` |
| specification/unit | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 规格/单位 |

##### 套餐管理页（DishCombo.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| comboCode | ✅ | ✅ | ✅ | ✅ | ✅ | 套餐编码 |
| comboName | ✅ | ✅ | ✅ | ✅ | ✅ | 套餐名称 |
| comboPrice | ✅ | ✅ | ✅ | ✅ | ✅ | 套餐价 |
| originalPrice | ✅ | ✅ | ✅ | ✅ | ✅ | 原价 |
| comboStatus | ✅ | ✅ | ✅ | ✅ | ⚠️ | 表格 `comboStatus`，API 字段 `status` |
| dailyLimit | ✅ | ✅ | ✅ | ✅ | ✅ | 每日限量 |
| soldToday | ✅ | ✅ | ✅ | ✅ | ✅ | 今日已售 |
| validStartDate/validEndDate | ✅ | ✅ | ✅ | ✅ | ✅ | 有效期 |
| ingredients（明细） | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 套餐明细 |
| comboType | ❌ 表格无 | ✅ | ❌ 后端无此列 | ✅ | ❌ 无回写 API | 后端表无此列 |
| totalCost | ✅ | ❌ 对话框无 | ✅ | ⚠️ | ❌ 表格有但对话框无 | `DishComboRow.totalCost` 与 `DishCombo.totalCost` 类型不一致 |

##### 分类管理页（CategoryManagement.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| categoryName | ✅ | ✅ | ✅ | ✅ | ✅ | 分类名称 |
| parentId | ✅ | ✅ | ✅ | ✅ | ✅ | 父级 |
| iconUrl | ✅ | ✅ | ✅ | ✅ | ✅ | 图标 |
| sortOrder | ✅ | ✅ | ✅ | ✅ | ✅ | 排序 |
| categoryStatus | ✅ | ✅ | ✅ | ✅ | ⚠️ | 表格 `categoryStatus`，API 字段 `status` |
| foodCount | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 菜品数 |

**产品中心 · 待修复项**

1. **菜品管理**：表单 `foodStatus` 提交时转 `status`；`ingredients` 提交时转 `recipes`；原料明细 `unitPrice` 转换层级分散。
2. **套餐管理**：`comboStatus` 与 API 字段 `status` 命名不一致；`comboType` 后端表无此列；`totalCost` 类型不一致。
3. **分类管理**：`categoryStatus` 与 API 字段 `status` 命名不一致。
4. **批量导入/导出**：模板部分实现，导出 API responseType 需确认后端支持。

#### 4.2.7 人事管理

##### 员工管理页（HREmployee.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| employeeId/id | ✅ | ✅ | ✅ | ✅ | ✅ | 员工ID |
| employeeCode | ✅ | ✅ | ✅ | ✅ | ✅ | 员工编码 |
| employeeName/name | ✅ | ✅ | ✅ | ✅ | ⚠️ | 后端用 `name`，前端用 `employeeName` |
| gender | ✅ | ✅ | ✅ | ✅ | ✅ | 性别 |
| phone | ✅ | ✅ | ✅ | ✅ | ✅ | 手机号 |
| email | ✅ | ✅ | ✅ | ✅ | ✅ | 邮箱 |
| departmentId/departmentName | ✅ | ✅ | ✅ | ✅ | ✅ | 部门 |
| positionId/positionName | ✅ | ✅ | ✅ | ✅ | ✅ | 职位 |
| storeId | ✅ | ✅ | ✅ | ✅ | ✅ | 门店 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 员工状态 |
| hireDate/resignDate | ✅ | ✅ | ✅ | ✅ | ⚠️ | 前端 string，后端 LocalDateTime |
| employmentType | ✅ | ✅ | ✅ | ✅ | ⚠️ | 后端存字符串，`EmploymentTypeMap` 按 Integer 设计，映射规则不一致 |
| baseSalary | ✅ | ✅ | ⚠️ 后端 BigDecimal 元 | ✅ | ❌ 单位不一致 | 项目规范要求 Long 分 |
| idCard/bankCard/address | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 后端存在但前端未显式声明 |
| workLocationType | ❌ 表格无 | ✅ | ✅ | ❌ TS 类型缺失 | ❌ TS 类型缺失 | 后端存在，前端类型未定义 |
| warehouseId | ❌ 表格无 | ✅ | ✅ | ❌ TS 类型缺失 | ❌ TS 类型缺失 | 员工归属仓库前端未暴露 |
| createdTime/updatedTime | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 创建/更新时间 |

##### 组织架构页（HROrganization.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| departmentId/id | ✅ | ✅ | ✅ | ✅ | ⚠️ | `Department` 用 `departmentId`，`DepartmentDTO` 用 `id` |
| departmentName/name | ✅ | ✅ | ✅ | ✅ | ⚠️ | `Department` 用 `departmentName`，`DepartmentDTO` 用 `name` |
| parentId | ✅ | ✅ | ✅ | ✅ | ✅ | 父级 |
| managerId/managerName | ✅ | ✅ | ✅ | ✅ | ✅ | 负责人 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |
| sortOrder/sort | ✅ | ✅ | ✅ | ✅ | ⚠️ | `DepartmentDTO` 用 `sort` |
| headcount | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 人数 |

> 原清单补充说明：组织树来自 `departments/tree`，需与后端层级、类型、负责人一致；职位、员工、排班均挂在组织下；新增/编辑表单中 `parentId` 正确继承当前选中节点，保存后刷新树并选中；人数 `headcount` 需与员工管理数据一致，作为编制依据。

**上下游流转**：

```text
公司 → 部门/门店 → 职位 → 员工
       ↓              ↓
    组织树选中节点 → 详情面板/表单（parentId 继承） → 保存后树刷新
```

##### 职位管理页（HRPosition.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| positionId/id | ✅ | ✅ | ✅ | ✅ | ⚠️ | `Position` 用 `id`，`PositionBasicInfo` 用 `positionId` |
| positionName | ✅ | ✅ | ✅ | ✅ | ✅ | 职位名称 |
| positionCode | ✅ | ✅ | ✅ | ✅ | ✅ | 职位编码 |
| departmentId | ✅ | ✅ | ✅ | ✅ | ✅ | 部门 |
| department/departmentName | ✅ | ✅ | ✅ | ✅ | ⚠️ | 前端同时存在 `department` 和 `departmentName` |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |

##### 考勤页（HRAttendance.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id/recordId | ✅ | ✅ | ✅ | ✅ | ✅ | 考勤记录ID |
| employeeCode | ✅ | ✅ | ⚠️ 后端 `employeeNo` | ✅ | ✅ | 命名不一致，已转换 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 考勤状态 |
| shiftType | ✅ | ❌ 对话框无 | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端无此字段，前端默认 full_day |
| clockInTime/clockOutTime | ✅ | ✅ | ✅ | ✅ | ✅ | 上下班时间 |
| leaveType | ✅ | ❌ 对话框无 | ⚠️ 后端 `leaveTypeName` | ✅ | ⚠️ | 仅展示名，无反向映射依据 |

##### 薪资页（HRSalary.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| period | ✅ | ✅ | ⚠️ 后端 `salaryMonth` | ✅ | ✅ | 薪资月份 |
| basicSalary | ✅ | ✅ | ⚠️ 后端 BigDecimal 元 | ✅ | ❌ 单位不一致 | 项目规范要求 Long 分 |
| subsidy | ✅ | ✅ | ⚠️ 后端 `allowance` | ✅ | ❌ 单位不一致 | BigDecimal 元 |
| socialInsurance | ✅ | ✅ | ⚠️ 后端 `insurance` | ✅ | ❌ 单位不一致 | BigDecimal 元 |
| deductions | ✅ | ✅ | ⚠️ 后端 `otherDeductions` | ✅ | ❌ 单位不一致 | BigDecimal 元 |
| grossSalary | ✅ | ✅ | ⚠️ 后端 `totalEarnings` | ✅ | ❌ 单位不一致 | BigDecimal 元 |
| netSalary | ✅ | ✅ | ⚠️ 后端 `finalSalary` | ✅ | ❌ 单位不一致 | BigDecimal 元 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 后端中文字符串，前端枚举 |
| paidAt/paymentMethod/bankTransactionNo | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端 `SalaryRecord` 无对应列 |

##### 合同页（HRContract.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 合同状态 |
| contractType | ✅ | ✅ | ✅ | ✅ | ✅ | 合同类型 |
| signDate | ✅ | ✅ | ✅ | ✅ | ✅ | 签署日期 |
| terminateReason | ✅ | ✅ | ✅ | ✅ | ✅ | 终止原因 |
| htmlContent | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 合同正文 |
| renew/archive/destroy/submitApproval/generateDocument | ❌ 表格无 | ✅ | ❌ 后端未实现 | ✅ | ❌ 无回写 API | `contract.ts` 大量方法直接抛错 |

##### 招聘页（HRRecruitment.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| 职位 status | ✅ | ✅ | ✅ | ✅ | ✅ | 招聘状态 |
| 简历 status | ✅ | ✅ | ✅ | ✅ | ✅ | 简历状态 |
| 面试状态 | ✅ | ✅ | ✅ | ✅ | ✅ | 本地枚举 |
| ticketCode | ✅ | ✅ | ✅ | ✅ | ✅ | 取票码 `FT-xxxxxx` |
| storeInterviewStatus/hrInterviewStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 门店/HR 面试阶段 |

##### 健康证页（HRHealthCertificate.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 健康证状态 |
| approvalStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 审批状态 |
| expenseStatus | ✅ | ❌ 对话框无 | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 报销状态后端未定义 |

##### 入职办理页（HROnboarding.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id/archiveId | ✅ | ✅ | ✅ | ✅ | ⚠️ | 前端 `id`，后端 `archiveId` |
| candidateName | ✅ | ✅ | ✅ | ✅ | ✅ | 候选人姓名 |
| position/positionLevel | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端实体未定义 |
| finalSalary/expectedSalary | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端实体未定义 |
| personnelType | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端实体未定义 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 具体状态值未在类型中枚举 |

##### 培训发展 / 知识库

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| CourseStatus/CoursePublishStatus | ✅ | ✅ | ❌ 后端映射未知 | ✅ | ❌ 无回写 API | 仅前端类型定义，未在 converters.ts 注册 |
| StudyStatus/QuizPassStatus | ✅ | ✅ | ❌ 后端映射未知 | ✅ | ❌ 无回写 API | 同上 |
| ArticlePublishStatus | ✅ | ✅ | ❌ 后端映射未知 | ✅ | ❌ 无回写 API | 同上 |

**人事管理 · 待修复项**

1. **员工**：`employeeName` vs `name`、`workLocationType` 前端类型缺失、`warehouseId` 前端未暴露、`baseSalary` 后端使用 BigDecimal 元违反项目规范。
2. **组织/职位**：主键命名不一致（`departmentId` vs `id`、`positionId` vs `id`）；`sort` vs `sortOrder`；`department` 与 `departmentName` 并存。
3. **考勤**：`shiftType` 后端缺失；`leaveType` 仅展示名无反向映射。
4. **薪资**：全部金额字段后端使用 BigDecimal 元，违反项目规范；`paidAt/paymentMethod/bankTransactionNo` 后端缺失。
5. **合同**：续签/归档/销毁/提交审批/生成文档等后端未实现。
6. **入职办理**：`position`/`positionLevel`/`finalSalary`/`expectedSalary`/`personnelType` 后端缺失。
7. **培训/知识库**：状态枚举仅前端定义，未注册 Converter，后端映射未知。

#### 4.2.8 资产管理

##### 资产主数据页（AssetList.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id/assetId | ✅ | ✅ | ✅ | ✅ | ✅ | 资产ID |
| assetCode | ✅ | ✅ | ✅ | ✅ | ✅ | 资产编码 |
| assetName | ✅ | ✅ | ✅ | ✅ | ✅ | 资产名称 |
| categoryId | ✅ | ✅ | ✅ | ✅ | ✅ | 分类ID |
| categoryName | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端实体无此字段，依赖 VO 冗余 |
| originalValue | ✅ | ✅ | ⚠️ 后端 `originalCost` | ✅ | ✅ | 原值（分） |
| currentValue | ✅ | ✅ | ⚠️ 后端 `netBookValue` | ✅ | ✅ | 净值 |
| accumulatedDepreciation | ✅ | ✅ | ✅ | ✅ | ✅ | 累计折旧 |
| purchaseDate | ✅ | ✅ | ✅ | ✅ | ✅ | 购买日期 |
| usefulLifeMonths | ✅ | ✅ | ✅ | ✅ | ✅ | 使用年限（月） |
| usedMonths | ✅ | ✅ | ⚠️ 后端 `useCount` | ✅ | ⚠️ | 语义为使用次数而非已使用月数 |
| remainingMonths | ❌ 表格无 | ✅ | ❌ 后端未返回 | ✅ | ❌ 无回写 API | 后端有但 API 未返回 |
| depreciationMethod | ✅ | ✅ | ✅ | ✅ | ⚠️ | 前端 `double_declining` 无对应后端编码 |
| salvageRate | ✅ | ✅ | ⚠️ 后端 `residualRate` | ✅ | ⚠️ | 前端百分比，后端小数，API 未转换 |
| departmentId/departmentName | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | `AssetMasterNew` 无部门字段 |
| storeId | ✅ | ✅ | ✅ | ✅ | ✅ | 门店 |
| storeName | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 依赖 VO 冗余 |
| location | ✅ | ✅ | ✅ | ✅ | ✅ | 存放位置 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 前端 7 状态 vs 后端 5 状态 |
| responsibleUserId/responsibleUserName | ✅ | ✅ | ⚠️ 后端无 `responsibleUserName` | ✅ | ⚠️ | 负责人名缺失 |
| supplierId/supplierName | ✅ | ✅ | ⚠️ 后端 `supplierInfo` JSON | ✅ | ⚠️ | 结构不一致 |
| specification | ✅ | ✅ | ⚠️ 后端有 `specification` 和 `brand` | ✅ | ⚠️ | 字段拆分 |
| serialNumber | ✅ | ✅ | ⚠️ 后端 `qrCode` | ✅ | ⚠️ | 语义不一致 |
| photoUrls | ❌ 表格无 | ✅ | ⚠️ 后端 `imageUrl` string | ✅ | ⚠️ | 前端多图，后端单图 |
| warrantyExpiry/nextMaintenanceDate | ❌ 表格无 | ✅ | ✅ | ❌ TS 类型缺失 | ❌ TS 类型缺失 | 后端存在，前端类型未声明 |

##### 资产分类页（AssetCategory.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id/categoryId | ✅ | ✅ | ✅ | ✅ | ✅ | 分类ID |
| code/categoryCode | ✅ | ✅ | ✅ | ✅ | ⚠️ | 命名不一致 |
| name/categoryName | ✅ | ✅ | ✅ | ✅ | ⚠️ | 命名不一致 |
| parentId | ✅ | ✅ | ✅ | ✅ | ✅ | 父级 |
| sortOrder | ✅ | ✅ | ✅ | ✅ | ✅ | 排序 |
| usefulLifeMonthsOverride | ✅ | ✅ | ⚠️ 后端 `usefulLifeYears` | ✅ | ⚠️ | 月 vs 年，存在整除截断 |
| salvageRateOverride | ✅ | ✅ | ⚠️ 后端 `residualRate` | ✅ | ⚠️ | 百分比 vs 小数 |
| assetCount/totalValue/monthlyDepreciation | ✅ | ❌ 对话框无 | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 前端统计字段后端未提供 |

##### 资产盘点页（AssetInventoryCheck.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id/checkId | ✅ | ✅ | ✅ | ✅ | ✅ | 盘点ID |
| checkCode | ✅ | ✅ | ✅ | ✅ | ✅ | 盘点单号 |
| title | ✅ | ✅ | ⚠️ 后端 `remark` 回填 | ✅ | ⚠️ | 标题语义不一致 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | `CANCELLED` 已修正为映射到保留值 4，不再误写入 `2=已审核` |
| checkType | ✅ | ✅ | ✅ | ✅ | ✅ | 盘点类型 |
| plannedStartTime/plannedEndTime | ✅ | ✅ | ❌ 后端只有 `checkDate` | ✅ | ❌ 无回写 API | 计划起止时间缺失 |
| actualStartTime/actualEndTime | ✅ | ✅ | ⚠️ 用 `approveTime` 推断 | ✅ | ⚠️ | 实际结束时间语义不一致 |
| creatorId/creatorName | ✅ | ✅ | ⚠️ 后端无 `creatorName` | ✅ | ⚠️ | 创建人名称缺失 |
| auditorName | ✅ | ✅ | ⚠️ 后端无 `auditorName` | ✅ | ⚠️ | 审批人名称缺失 |
| totalAssetsCount/totalAssetCount | ✅ | ✅ | ✅ | ✅ | ⚠️ | 命名不一致 |
| actualCount/countedCount | ✅ | ✅ | ✅ | ✅ | ⚠️ | 命名不一致 |
| overageCount/shortageCount | ✅ | ✅ | ❌ 后端只有 `diffCount` | ✅ | ❌ 无回写 API | 盘盈/盘亏拆分缺失 |

##### 资产维修页（AssetMaintenance.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id/flowId | ✅ | ✅ | ✅ | ✅ | ✅ | 维修记录ID |
| assetId | ✅ | ✅ | ✅ | ✅ | ✅ | 资产ID |
| faultDescription/resultDescription | ✅ | ✅ | ⚠️ 后端 `remark` | ✅ | ⚠️ | 字段复用 |
| repairType | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端无维修类型字段 |
| vendorId/vendorName | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端无此字段 |
| repairCost | ✅ | ✅ | ⚠️ 后端 `changeAmount` | ✅ | ⚠️ | 语义不一致 |
| startTime | ✅ | ✅ | ✅ | ✅ | ✅ | 开始时间 |
| completedTime | ✅ | ✅ | ⚠️ 根据 `afterValue` 推断 | ✅ | ⚠️ | 完成状态推断逻辑 |
| status | ✅ | ✅ | ❌ 后端缺失 | ✅ | ❌ 无回写 API | 后端无状态字段 |
| repairmanId/repairmanName | ✅ | ✅ | ⚠️ 后端无 `operatorName` | ✅ | ⚠️ | 维修人名称缺失 |

##### 资产处置页（AssetDisposal.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id/disposalNo | ✅ | ✅ | ✅ | ✅ | ⚠️ | 后端返回 Map，字段名不固定 |
| disposalType | ✅ | ✅ | ✅ | ✅ | ✅ | 处置类型 |
| netValue/disposalAmount/gainLoss | ✅ | ✅ | ✅ | ✅ | ✅ | 净值/处置金额/损益 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 处置状态 |
| applicantName/approverName | ✅ | ✅ | ⚠️ `handler` 复用 | ✅ | ⚠️ | 申请人可能来自 `handler` |
| disposalDate | ✅ | ✅ | ✅ | ✅ | ✅ | 处置日期 |

##### 资产调拨页（AssetTransfer.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| 全部 | ✅ | ✅ | ❌ 后端未实现 | ✅ | ❌ 无回写 API | `transferApi` 所有方法抛错或返回空 |

**资产管理 · 待修复项**

1. **资产主数据**：`categoryName/storeName` 后端缺失；`usedMonths` 语义为 `useCount`；`salvageRate` 单位不一致；`departmentId/Name` 后端缺失；`serialNumber` 映射 `qrCode`；`photoUrls` 与后端单图不一致；`warrantyExpiry/nextMaintenanceDate` 前端类型缺失。
2. **资产分类**：`code/name` 与后端 `categoryCode/categoryName` 命名不一致；`usefulLifeMonthsOverride` 与 `usefulLifeYears` 单位不一致；统计字段后端缺失。
3. **资产盘点**：`title` 由 `remark` 回填；✅ `CANCELLED` 已修正为映射到保留值 4，不再误写入 `2=已审核`；计划/实际时间后端缺失；盘盈/盘亏拆分缺失。
4. **资产维修**：`repairType/vendorId/vendorName/status` 后端缺失；`repairCost` 语义为 `changeAmount`；完成状态靠推断。
5. **资产调拨**：后端 API 完全未实现。

#### 4.2.9 设备管理

##### 设备主数据页（DeviceList.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| deviceId | ✅ | ✅ | ✅ | ✅ | ✅ | 设备ID |
| deviceCode | ✅ | ✅ | ✅ | ✅ | ✅ | 设备编码 |
| deviceName | ✅ | ✅ | ✅ | ✅ | ✅ | 设备名称 |
| deviceType | ✅ | ✅ | ✅ | ✅ | ✅ | 设备类型 |
| deviceModel | ✅ | ✅ | ✅ | ✅ | ✅ | 设备型号 |
| manufacturer | ✅ | ✅ | ⚠️ 后端 `@TableField(exist=false)` | ✅ | ⚠️ | 数据库无此列 |
| serialNo | ✅ | ✅ | ✅ | ✅ | ⚠️ | 列名 `serial_number` |
| connectionType | ✅ | ✅ | ✅ | ✅ | ✅ | 连接方式 |
| connectionParams | ✅ | ✅ | ⚠️ 后端 `connection_config` | ✅ | ✅ | 命名不一致 |
| location | ✅ | ✅ | ⚠️ 后端 `@TableField(exist=false)` | ✅ | ⚠️ | 数据库无此列 |
| storeId | ✅ | ✅ | ✅ | ✅ | ✅ | 门店 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | 数据库存 varchar，前端映射 0/1/2/3 |
| lastHeartbeatTime | ✅ | ✅ | ⚠️ 后端 `@TableField(exist=false)` | ✅ | ⚠️ | 数据库无此列 |
| lastOnlineTime | ✅ | ✅ | ✅ | ✅ | ✅ | 最后在线时间 |
| remark | ✅ | ✅ | ⚠️ 后端 `@TableField(exist=false)` | ✅ | ⚠️ | 数据库无此列 |
| firmwareVersion/driverClass/configJson | ❌ 表格无 | ✅ | ⚠️ 后端非持久化 | ✅ | ❌ 无回写 API | 均为 `@TableField(exist=false)` |

##### 设备告警页（DeviceAlert.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| alertId | ✅ | ✅ | ✅ | ✅ | ✅ | 告警ID |
| deviceId | ✅ | ✅ | ✅ | ✅ | ✅ | 设备ID |
| deviceName | ✅ | ✅ | ✅ | ✅ | ✅ | 设备名称冗余 |
| deviceType | ✅ | ✅ | ✅ | ✅ | ✅ | 设备类型冗余 |
| alertType | ✅ | ✅ | ✅ | ✅ | ✅ | 告警类型 |
| alertLevel | ✅ | ✅ | ✅ | ✅ | ✅ | 告警等级 |
| alertStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 告警状态 |
| isHandled | ✅ | ✅ | ✅ | ✅ | ✅ | 是否已处理 |
| triggerTime | ✅ | ✅ | ✅ | ✅ | ✅ | 触发时间 |
| handleTime/handleResult/handleUserId | ✅ | ✅ | ✅ | ✅ | ✅ | 处理信息 |
| resolveTime | ✅ | ✅ | ✅ | ✅ | ✅ | 恢复时间 |
| deviceStatusSnapshot | ✅ | ✅ | ✅ | ✅ | ✅ | 设备状态快照 |

##### 状态历史页（DeviceStatusHistory.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| deviceType | ✅ | ✅ | ✅ | ✅ | ⚠️ | 后端 `DeviceStatusHistory` 用 String，与 `Device` 的 Integer 不同 |
| online | ✅ | ✅ | ❌ 后端来源不明 | ✅ | ❌ 无回写 API | 来源未明确 |
| ipAddress/port | ✅ | ✅ | ❌ 后端来源不明 | ✅ | ❌ 无回写 API | 来源未明确 |

**设备管理 · 待修复项**

1. **设备主数据**：`manufacturer/location/lastHeartbeatTime/remark` 数据库无持久化列；`firmwareVersion/driverClass/configJson` 均为非持久化字段。
2. **状态历史**：`deviceType` 后端类型与主数据不一致；`online/ipAddress/port` 后端来源不明。

#### 4.2.10 会员营销

##### 会员信息页（MemberList.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ✅ | 会员ID |
| memberNo | ✅ | ✅ | ✅ | ✅ | ✅ | 会员编号 |
| phone | ✅ | ✅ | ✅ | ✅ | ✅ | 手机号 |
| nickname | ✅ | ✅ | ✅ | ✅ | ✅ | 昵称 |
| gender | ✅ | ✅ | ✅ | ✅ | ✅ | 性别 |
| memberLevelId/levelName | ✅ | ✅ | ✅ | ✅ | ✅ | 会员等级 |
| balance | ✅ | ✅ | ✅ | ✅ | ⚠️ | 后端元字符串，与财务分整数不一致 |
| totalRecharge | ✅ | ✅ | ✅ | ✅ | ⚠️ | 同上 |
| totalConsume | ✅ | ✅ | ✅ | ✅ | ⚠️ | 同上 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 会员状态 |
| customerSegment | ✅ | ✅ | ✅ | ✅ | ✅ | RFM分层 |
| preferences | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 用户偏好 |
| createdAt | ✅ | ✅ | ⚠️ 数据库 `create_time` | ✅ | ⚠️ | 命名需确认 MyBatis 映射 |
| updatedAt | ✅ | ✅ | ⚠️ 数据库 `update_time` | ✅ | ⚠️ | 同上 |

##### 会员等级页（MemberLevel.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| levelId | ✅ | ✅ | ✅ | ✅ | ✅ | 等级ID |
| levelName | ✅ | ✅ | ✅ | ✅ | ✅ | 等级名称 |
| levelCode | ✅ | ✅ | ✅ | ✅ | ✅ | 等级编码 |
| minConsumption | ✅ | ✅ | ✅ | ✅ | ✅ | 最低消费（元字符串） |
| discountRate | ✅ | ✅ | ✅ | ✅ | ✅ | 折扣率 |
| status | ✅ | ✅ | ✅ | ✅ | ⚠️ | `active/inactive` 字符串，与通用数字编码不一致 |
| criteria/benefits | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 高级配置 |

##### 储值记录页（RechargeManage.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| recordId | ✅ | ✅ | ✅ | ✅ | ✅ | 记录ID |
| recordNo | ✅ | ✅ | ✅ | ✅ | ✅ | 流水号 |
| rechargeAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 充值金额（元） |
| principalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 本金（元） |
| bonusAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 赠送金额（元） |
| paymentStatus | ✅ | ✅ | ✅ | ✅ | ⚠️ | 类型定义缺少 `partial_refunded` |
| refundStatus | ✅ | ✅ | ✅ | ✅ | ✅ | 退款状态 |

**会员营销 · 待修复项**

1. **金额单位不统一**：会员营销使用「元字符串」，财务中心使用「分整数」，跨模块交互需统一转换位置。
2. **时间字段命名**：`createdAt/updatedAt` 与项目数据库规范 `create_time/update_time` 需确认 MyBatis 映射。
3. **会员等级状态**：使用语义字符串，与通用状态数字编码不一致。
4. **储值记录**：`paymentStatus` 类型定义缺少 `partial_refunded`。

#### 4.2.11 财务中心

##### 会计科目页（FinanceSubject.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ✅ | 科目ID |
| subjectCode | ✅ | ✅ | ✅ | ✅ | ✅ | 科目编码 |
| subjectName | ✅ | ✅ | ✅ | ✅ | ✅ | 科目名称 |
| subjectType | ✅ | ✅ | ✅ | ✅ | ✅ | 科目类型 |
| balanceDirection | ✅ | ✅ | ✅ | ✅ | ✅ | 余额方向 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |
| balance | ✅ | ✅ | ✅ | ✅ | ✅ | 余额（分） |
| parentId | ✅ | ✅ | ✅ | ✅ | ✅ | 父级科目 |

##### 凭证页（FinanceVoucher.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| voucherNo | ✅ | ✅ | ✅ | ✅ | ✅ | 凭证号 |
| voucherType | ✅ | ✅ | ✅ | ✅ | ✅ | 凭证类型 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 凭证状态 |
| debitTotal/creditTotal | ✅ | ✅ | ✅ | ✅ | ✅ | 借方/贷方合计（分） |
| attachmentCount | ✅ | ✅ | ✅ | ✅ | ✅ | 附件张数 |
| entries（分录） | ✅ | ✅ | ✅ | ✅ | ✅ | 凭证明细 |

##### 应收/应付账款

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ✅ | 账款ID |
| amount | ✅ | ✅ | ✅ | ✅ | ✅ | 应收/应付总额（分） |
| receivedAmount/paidAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 已收/已付（分） |
| remainAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 未收/未付（分） |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 账款状态 |
| aging | ✅ | ✅ | ✅ | ✅ | ⚠️ | 账龄计算口径建议统一 |
| sourceBillNo | ✅ | ✅ | ✅ | ✅ | ✅ | 关联单据号 |

##### 发票页（FinanceInvoice.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| invoiceNo | ✅ | ✅ | ✅ | ✅ | ✅ | 发票号 |
| invoiceType | ✅ | ✅ | ✅ | ✅ | ✅ | 发票类型 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 发票状态 |
| amountWithoutTax/taxAmount/totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 金额（分） |
| taxRate | ✅ | ✅ | ✅ | ✅ | ✅ | 税率 |
| buyerName/sellerName | ✅ | ✅ | ✅ | ✅ | ✅ | 购方/销方 |

##### 报销单页（InvoiceReimbursement.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| reimbursementId | ✅ | ✅ | ✅ | ✅ | ⚠️ | 主键命名不一致（其余用 `id`） |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 报销状态 |
| paymentStatus | ✅ | ✅ | ✅ | ✅ | ⚠️ | 与 `status` 存在状态交叉 |
| totalAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 报销总额（分） |
| approvedAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 审批金额（分） |
| applicantName | ✅ | ✅ | ✅ | ✅ | ✅ | 申请人 |
| items（明细） | ✅ | ✅ | ✅ | ✅ | ✅ | 报销明细 |

##### 付款单/收款单

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ✅ | 单据ID |
| paymentAmount/receiptAmount | ✅ | ✅ | ✅ | ✅ | ✅ | 付款/收款金额（分） |
| paymentMethod/receiptMethod | ✅ | ✅ | ✅ | ✅ | ✅ | 支付方式 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |
| fundFlowId/voucherId | ✅ | ✅ | ✅ | ✅ | ✅ | 关联资金流水/凭证 |
| sourceBillNo | ✅ | ✅ | ✅ | ✅ | ✅ | 关联单据号 |

**财务中心 · 待修复项**

1. **金额单位跨模块不一致**：会员营销用元字符串，财务中心用分整数。
2. **报销单状态交叉**：`paymentStatus` 与 `status` 存在并行状态字段，需明确状态机。
3. **账龄计算口径**：建议统一。
4. **主键命名**：`reimbursementId` 与其他财务实体 `id` 不一致。

#### 4.2.12 系统管理

##### 权限码页（PermissionCenter.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ✅ | 权限ID |
| permissionCode | ✅ | ✅ | ✅ | ✅ | ✅ | 权限编码 |
| permissionName | ✅ | ✅ | ✅ | ✅ | ✅ | 权限名称 |
| permissionType | ✅ | ✅ | ✅ | ✅ | ✅ | 权限类型 |
| module | ✅ | ✅ | ✅ | ✅ | ✅ | 所属模块 |
| parentId | ✅ | ✅ | ✅ | ✅ | ✅ | 父级权限 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |
| path/component | ✅ | ✅ | ✅ | ✅ | ✅ | 路由/组件 |
| sortOrder | ✅ | ✅ | ✅ | ✅ | ✅ | 排序 |

##### 角色管理页（RoleManagement.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ⚠️ | 与权限码 `string` 主键类型不一致（角色为 number） |
| roleCode | ✅ | ✅ | ✅ | ✅ | ✅ | 角色编码 |
| roleName | ✅ | ✅ | ✅ | ✅ | ✅ | 角色名称 |
| roleDescription | ✅ | ✅ | ✅ | ✅ | ✅ | 角色描述 |
| roleType | ✅ | ✅ | ✅ | ✅ | ✅ | 角色类型 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |
| systemBuilt | ✅ | ✅ | ✅ | ✅ | ✅ | 是否系统内置 |
| domainPermissions | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 域权限矩阵 |

##### 用户管理页（UserManagement.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ✅ | 用户ID |
| username | ✅ | ✅ | ✅ | ✅ | ✅ | 用户名 |
| realName | ✅ | ✅ | ✅ | ✅ | ✅ | 真实姓名 |
| phone | ✅ | ✅ | ✅ | ✅ | ✅ | 手机号 |
| email | ✅ | ✅ | ✅ | ✅ | ✅ | 邮箱 |
| roleIds/roleNames | ✅ | ✅ | ✅ | ✅ | ✅ | 角色 |
| storeIds/storeNames | ✅ | ✅ | ✅ | ✅ | ✅ | 门店 |
| departmentId/departmentName | ✅ | ✅ | ✅ | ✅ | ✅ | 部门 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 状态 |
| lastLoginTime | ✅ | ❌ 对话框无 | ✅ | ✅ | ❌ 表格有但对话框无 | 最后登录时间 |

##### 审计日志页（AuditLog.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| id | ✅ | ✅ | ✅ | ✅ | ✅ | 日志ID |
| operationType | ✅ | ✅ | ✅ | ✅ | ✅ | 操作类型 |
| module | ✅ | ✅ | ✅ | ✅ | ✅ | 模块 |
| operateUserName | ✅ | ✅ | ✅ | ✅ | ✅ | 操作人 |
| operateTime | ✅ | ✅ | ✅ | ✅ | ✅ | 操作时间 |
| ipAddress | ✅ | ✅ | ✅ | ✅ | ✅ | IP地址 |
| requestUrl | ✅ | ✅ | ✅ | ✅ | ✅ | 请求URL |
| requestMethod | ✅ | ✅ | ✅ | ✅ | ✅ | 请求方法 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 操作状态 |
| errorMessage | ❌ 表格无 | ✅ | ✅ | ✅ | ❌ 对话框有但表格无 | 错误信息 |

> 原清单补充说明：权限中心中，权限模板来自 `/v1/permission-templates/system`，4 种模板可切换并影响菜单可见性；域权限矩阵来自 `roleConfig` JSON，HIDDEN→LIMITED→READ_ONLY→FULL 可切换并同步到侧边菜单；角色管理中角色编码前缀需校验，用户分配角色后生效；用户管理中角色/门店/部门需与权限一致，登录后菜单按权限过滤。

**系统管理 · 待修复项**

1. **主键类型不一致**：权限码 `id` 为 string，角色 `id` 为 number。
2. **时间字段命名**：系统管理部分使用 `createdAt/updatedAt`，与项目规范 `createTime/updateTime` 不一致。
3. **操作人字段命名**：`operator` / `createBy` / `createUserName` / `operateUserName` 跨模块分散。

#### 4.2.13 印章 / 签约

##### 印章管理页（SealManagement.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| sealId | ✅ | ✅ | ✅ | ✅ | ✅ | 印章ID |
| sealName | ✅ | ✅ | ✅ | ✅ | ✅ | 印章名称 |
| sealType | ✅ | ✅ | ✅ | ✅ | ✅ | 印章类型 |
| sealImage | ✅ | ✅ | ⚠️ 后端 `sealImageUrl` | ✅ | ⚠️ | 字段命名不一致，API 已转换 |
| status | ✅ | ✅ | ✅ | ✅ | ✅ | 印章状态 |
| keeper | ✅ | ✅ | ✅ | ✅ | ✅ | 保管人 |
| authorizedUsers | ✅ | ✅ | ✅ | ✅ | ✅ | 授权使用人 |
| authorizedScenes | ✅ | ✅ | ✅ | ✅ | ✅ | 授权场景 |
| createBy | ✅ | ✅ | ✅ | ✅ | ✅ | 创建人 |
| createTime/updateTime | ✅ | ✅ | ✅ | ✅ | ✅ | 创建/更新时间 |

##### 印章使用记录页（SealUsageRecord.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| logId | ✅ | ✅ | ✅ | ✅ | ✅ | 记录ID |
| businessType | ✅ | ✅ | ✅ | ✅ | ✅ | 业务类型 |
| businessId/businessNo | ✅ | ✅ | ✅ | ✅ | ✅ | 业务ID/编号 |
| operator | ✅ | ✅ | ✅ | ✅ | ✅ | 操作人 |
| ipAddress | ✅ | ✅ | ✅ | ✅ | ✅ | IP地址 |
| operateTime | ✅ | ✅ | ✅ | ✅ | ✅ | 操作时间 |

**印章/签约 · 待修复项**

1. **印章图片字段命名**：前端 `sealImage`，后端 `sealImageUrl`，需统一或明确在 Converter 中处理。

#### 4.2.14 运营决策

##### 决策看板页（DecisionBoard.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| totalRevenue | ✅ | ❌ 对话框无 | ⚠️ TODO 占位 | ✅ | ❌ 无回写 API | 核心指标接口未接入 |
| totalOrders | ✅ | ❌ 对话框无 | ⚠️ TODO 占位 | ✅ | ❌ 无回写 API | 同上 |
| avgOrderValue | ✅ | ❌ 对话框无 | ⚠️ TODO 占位 | ✅ | ❌ 无回写 API | 同上 |
| storeId | ✅ | ✅ | ✅ | ✅ | ✅ | 门店筛选 |
| timeRange | ✅ | ✅ | ✅ | ✅ | ✅ | 时间范围 |
| dateRange | ✅ | ✅ | ✅ | ✅ | ✅ | 自定义日期 |
| revenueTrend | ✅ | ❌ 对话框无 | ⚠️ TODO 占位 | ✅ | ❌ 无回写 API | 收入趋势 |
| costComponent | ✅ | ❌ 对话框无 | ⚠️ TODO 占位 | ✅ | ❌ 无回写 API | 成本构成 |
| memberGrowth | ✅ | ❌ 对话框无 | ⚠️ TODO 占位 | ✅ | ❌ 无回写 API | 会员增长 |
| inventoryWarning | ✅ | ❌ 对话框无 | ⚠️ TODO 占位 | ✅ | ❌ 无回写 API | 库存预警 |
| kpiCompare | ✅ | ❌ 对话框无 | ⚠️ TODO 占位 | ✅ | ❌ 无回写 API | KPI对比 |

##### 报表中心页（ReportCenter.vue）

| 字段 | 表格列 | 对话框字段 | API 字段 | TS 类型 | 状态 | 备注 |
|-----|--------|-----------|---------|---------|------|------|
| reportName | ✅ | ✅ | ✅ | ✅ | ✅ | 报表名称 |
| reportType | ✅ | ✅ | ✅ | ✅ | ✅ | 报表类型 |
| timeRange | ✅ | ✅ | ✅ | ✅ | ✅ | 时间范围 |
| storeId | ✅ | ✅ | ✅ | ✅ | ✅ | 门店筛选 |
| exportTaskId | ✅ | ✅ | ✅ | ✅ | ✅ | 导出任务ID |
| downloadUrl | ✅ | ✅ | ✅ | ✅ | ✅ | 下载链接 |

**运营决策 · 待修复项**

1. **决策看板大量核心指标接口未实现**：`stats-overview`、`revenue-trend`、`cost-component`、`member-growth`、`inventory-warning`、`kpi-compare` 当前为 TODO 占位。
2. **字段与后端未对齐**：核心指标字段定义尚未与后端对齐。

### 4.3 全模块共性问题汇总

| 序号 | 问题类别 | 影响模块 | 风险等级 | 修复建议 |
|-----|---------|---------|---------|---------|
| 1 | 主键命名/类型不一致：`id` vs `xxxId`，string vs number | 全模块 | 高 | 统一主键命名规范；对外暴露统一为 string |
| 2 | 金额单位不统一：元字符串 vs 分整数，BigDecimal vs Long | 采购、会员、财务、人事薪资、资产 | 高 | 后端统一存分（Long），前端统一展示元，API 统一用 `utils/money` 转换 |
| 3 | 状态字段表达不统一：语义字符串 vs 数字编码，大写 vs 小写 | 采购、仓储、溯源、人事、资产、设备、会员 | 高 | 后端数字编码，前端语义字符串，统一通过 `converters.ts` 转换 |
| 4 | 时间字段命名混用：`createdAt/updatedAt` vs `createTime/updateTime` | 会员、系统、溯源 | 中 | 统一为 `createTime/updateTime` 匹配数据库规范 |
| 5 | 物料字段命名混用：`food`/`product`/`material` | 采购、仓储、溯源、产品 | 高 | 统一使用 `materialId/materialName/materialCode` |
| 6 | 申请人/创建人/操作人字段命名分散 | 采购、仓储、人事、系统、印章 | 中 | 统一为 `createBy`/`updateBy` 或 `operatorName` |
| 7 | 审批字段命名不统一：`approvedBy` vs `approveBy` | 采购、资产 | 中 | 统一为 `approvedBy/approvedTime` |
| 8 | 关联单据号字段命名混用：`requestNo`/`orderNo`/`orderCode`/`stockinNo` | 采购、仓储、订单、财务 | 中 | 统一按 `{entity}No` 命名 |
| 9 | 状态机前后端数量不匹配 | 采购订单、资产 | 高 | 扩展后端状态码或前端合并状态 |
| 10 | 后端 API 未实现/直接抛错 | 资产调拨、合同、薪资、运营决策 | 高 | 补齐后端接口或移除前端占位功能 |
| 11 | 数据库字段缺失 | 门店库存、设备、资产、门店证件 | 高 | 补齐迁移脚本或调整前端类型为可选 |
| 12 | JSON 字段结构未细化 | 溯源、资产供应商信息 | 中 | 在 TS 类型中定义明确结构 |
| 13 | 枚举/字典字段未注册 Converter | 培训、知识库、健康证审批 | 中 | 在 `converters.ts` 中注册前后端映射 |
| 14 | 字段存在但无回写 API | 采购订单 paymentStatus、资产 categoryName 等 | 中 | 确认是否需要只读，或补齐更新接口 |
| 15 | TS 类型缺失但页面已使用 | 门店库存 category、设备状态历史 online/ip 等 | 中 | 补充类型定义或使用具体类型替代 any |

---

## 五、当前已发现问题与进度

### 5.1 已修复（本次会话）

| 问题 | 根因 | 修复文件 | 验证结果 |
|------|------|----------|----------|
| admin 登录被锁定 | 密码错误次数过多 | `reset_admin.py` 更新 PostgreSQL users 表 | 已重置密码为 `admin123`，解锁账户 |
| 登录请求过于频繁 | 限流触发 | `RateLimitAspect.java` 临时关闭 | 开发阶段可正常访问 |
| 验证码强制显示 | 登录失败 3 次触发 | `AuthServiceImpl.java` 阈值调至 99999；`LoginPage.vue` 关闭预加载 | 登录页不再默认显示验证码 |
| 管理端页面丢失 | 开发环境自动登录密码错误导致登录失败 | `LoginPage.vue` 关闭自动登录、修正密码 | 管理端布局完整，菜单/导航/主题/设置正常 |
| 岗位表单下拉被遮罩 | `popper-options` 与全局插件冲突 | `PositionFormDialog.vue` 移除冲突属性 | 下拉可正常展开 |
| 组织架构树操作栏设计不专业 | 原方案每行常驻工具栏，视觉噪音大 | `OrgTreeNode.vue` 按方案 D 重设计：hover/选中显示精简图标，删除按权限控制 | 操作栏默认隐藏，有权限才显示删除 |
| 采购订单详情状态表达重复且深色模式灰暗 | 原状态横幅同时存在 StatusTag、图章、描述列表 StatusTag 三重重复；深色模式状态背景为灰黑 | `PurchaseOrder.vue` 采用方案 B+D：顶部彩色状态 Header 条 + 流程步骤条；深色模式改用半透明主题色 | 状态表达唯一且流程可视，可点击跳转上下游单据；验证截图：`purchase-order-detail-header-steps.png` |
| 采购链路追溯无左侧菜单、作为单独页面不合理 | 原 `InventoryTrace.vue` 是独立页面，从采购订单点击后跳转导致菜单丢失 | 删除独立页面与路由，新增 `PurchaseTraceDialog.vue`，从采购订单详情通过“链路追溯”按钮直接打开 | 对话框内展示采购→入库→库存/资产完整链路，无需离开当前页面；构建验证通过 |
| AI 浏览器截图比例/缩放导致 UI 异常 | 固定 80% 缩放使内容显示不全 | 更新截图规范，以“内容完整可见”为目标，优先 1440×900/1920×1080、deviceScaleFactor=1 | 截图前统一视口设置，避免缩放残留 |
| 菜单名称与页面标题不一致 | 历史迭代过程中菜单配置、路由 meta.title、页面 PageHeader title 未同步更新 | 全模块 30 处 | 已批量修复 4 处路由 meta.title 和 28 处页面 PageHeader title；`npm run build` 通过 |
| 全局权限基础设施缺失 | 菜单/路由未配置权限，权限码与实际页面动作脱节 | `utils/permissions.ts`、`stores/permission.ts`、`router/index.ts`、`types/router.d.ts`、各 `modules/*/menu.ts` | 已补充 16 个模块约 260+ 细粒度权限码，配置菜单 visibleRoles/permissions 和路由 meta.permissions/roles |
| 采购链路字段/状态机/权限问题 | 申请人/审批人字段命名混用，状态机不闭环，操作按钮无权限控制 | 采购申请/计划/订单/到货/结算相关视图 | 已统一字段命名为 `createByName`/`approvedByName`；采购计划 `pending` 增加审批入口、`approved` 增加「开始执行」入口；采购申请 `rejected` 支持删除；核心按钮补充 `v-permission`；新增 `purchase:plan:execute`、`purchase:order:cancel` 权限码；`npm run build` 通过 |
| 孤立/未引用页面 | 部分历史页面文件仍存在，但路由已重定向或完全未注册 | `SelfServicePortal.vue`、`StoreOperationLog.vue`、`StoreStatusOverview.vue` | 已移动/删除/整合 |
| 学习记录页面误删 | 用户误以为 `KnowledgeStudyRecords.vue` 与知识库页面重复 | `frontend/src/views/hr/KnowledgeStudyRecords.vue` | 已恢复文件，补充 `useLayoutStore` import，接入菜单/路由，确认与「知识库智能」「知识库管理」不重复，独立保留 |
| 库存/资产盘点 CANCELLED 状态误映射到「已审核」 | 仓库盘点 `checkStatus` 缺少 `cancelled`；资产盘点 `CANCELLED` 错误映射到后端 2 | `frontend/src/types/warehouse-check.ts`、`api/warehouse/converters.ts`、`views/warehouse/InventoryCheck.vue`、`api/asset/inventory.ts` | 已新增 `cancelled` 状态并映射到后端保留值 4；`npm run build` 通过 |
| 门店库存成本未使用最新入库价格 | `StoreInventoryServiceImpl.increaseStock()` 残留加权平均法计算 | `backend/src/main/java/com/foodtraceability/service/impl/StoreInventoryServiceImpl.java` | 已改为最新入库价格直接覆盖 `unit_cost`；`mvn compile -q` 通过 |
| 库存汇总未包含门店库存 | `InventorySummaryMapper.xml` 只汇总 `inventory` 表，`storeStock` 硬编码为 0 | `backend/src/main/resources/mapper/InventorySummaryMapper.xml` | 已使用 `UNION ALL` 联合 `inventory` 与 `store_inventory`，修正 `store_count` 语义与成本单价计算；`mvn compile -q` 通过 |

### 5.1+ 新核查发现（部分已修复）

| 问题 | 根因 | 影响范围 | 验证结果 |
|------|------|----------|----------|
| 菜单名称与页面标题不一致 | 历史迭代过程中菜单配置、路由 meta.title、页面 PageHeader title 未同步更新 | 全模块共 28 处，重点：采购「商品分类」vs「物资分类」、仓储「库存概览」vs「仓储总览」、门店「日结对账」vs「门店日结」等 | 已产出 `menu-title-consistency-report.md` |
| 数据溯源检查清单完整性不足 | 原清单仅覆盖部分模块和少量字段，缺少表格列/对话框字段/API 字段/TS 类型的交叉验证 | `frontend-verification-plan.md` 第四章 | 已整合全模块增强版检查清单，覆盖 14 个模块，逐字段标注状态 |
| 孤立/未引用页面 | 部分历史页面文件仍存在，但路由已重定向或完全未注册 | `SelfServicePortal.vue`、`StoreOperationLog.vue`、`StoreStatusOverview.vue`、`KnowledgeStudyRecords.vue` | 已产出 `missing-pages-report.md` |

### 5.2 待处理（按优先级）

| 优先级 | 任务 | 关联文件 | 备注 |
|--------|------|----------|------|
| P0 | 前端数据对齐治理（全部模块） | `docs/alignment/*.md` | 全模块四维对照文档已产出（采购/门店-订单/仓储-溯源/人事-资产-设备/财务-会员-系统-印章-运营），共 20 个文件；代码改造待执行 |
| P1 | 完成前端验证计划落地执行 | 全部视图 | 按本清单逐页核对并记录结果 |
| P2 | 开发调试项恢复 | `RateLimitAspect.java`、`AuthServiceImpl.java`、`LoginPage.vue` | 验收后恢复验证码、限流、自动登录 |

### 5.3 待确认（需用户指示）

1. **删除权限控制粒度**：组织架构删除按钮是按“角色（owner/admin）”控制，还是按“数据权限（只能删除本门店/本部门）”控制？
2. **采购订单状态戳文案**：已确认显示当前状态名称（如“待审核”），无需再确认。

---

## 六、验收标准

1. 所有 P0 任务完成并通过浏览器截图验证。
2. 本清单中“数据溯源与流转”关键路径（采购申请→订单→收货→库存→结算）经验证无断点。
3. 全局检查项 G-01 至 G-10 全部通过。
4. 开发调试临时项已记录，待验收后按清单恢复。
5. `npm run build` 通过，无新增 TypeScript/ESLint 错误。

---

## 八、AI 浏览器截图规范（防 UI 异常）

针对“AI 浏览器打开页面有 UI 问题，关闭重新打开正常”的现象，统一采用以下截图规范，避免因视口/缩放不一致导致布局错乱：

1. **视口固定为桌面端**：优先使用 `1440×900` 或 `1920×1080`，`deviceScaleFactor=1`。
2. **禁止缩放页面**：不通过 CSS transform 或浏览器缩放改变页面比例，截图以“内容完整可见”为目标。
3. **先设置视口再导航**：在打开目标页面前先设置 viewport，避免先小视口加载再放大导致的样式残留。
4. **必要时硬刷新**：若页面出现元素偏移/缺失，先执行 `Ctrl+Shift+R` 或关闭标签页重新打开。
5. **截图前等待网络空闲**：确保字体、图标、动态菜单均已加载完成。
6. **验证截图内容**：每页截图必须包含左侧菜单、顶部导航、右侧内容区，缺少任何一项视为异常。

---

## 十、数据链路设计示例

> 本节以「产品中心 → 菜品原料明细 → 门店库存 → 仓储管理」为例，说明跨模块数据应如何统一来源、避免重复录入、确保上下游同步。

### 10.1 菜品原料明细的数据来源

**场景**：产品中心 / 菜品管理 / 新增菜品对话框中，「原料明细」需要选择原料名称，并自动带出成本单价（元/单位）。

**数据规则**：

1. **原料名称下拉来源**：统一从「门店库存」数据中获取，而非在菜品管理内部维护独立原料档案。
   - 原因：门店库存是原料在门店层面的真实可用库存，包含最新价格、规格、单位。
   - 来源表格：`门店管理 / 门店库存`（StoreInventory）中的库存明细表格。
   - 接口：`GET /v1/store/inventory`，返回字段示例：
     - `materialId`：物料ID
     - `materialName`：物料名称（原料名称）
     - `specification`：规格
     - `unit`：单位
     - `unitCost`：成本单价（元/单位）
     - `currentStock`：当前库存数量

2. **成本单价自动带出**：选择原料后，对话框自动回填 `unitCost`（元/单位）。
   - 该价格来自门店库存的**最新入库成本**（每次入库后 `unit_cost` 直接覆盖为本次入库单价，不使用加权平均法），不应由用户手动输入。
   - 示例：选择「五花肉」后，自动带出 `unitCost = 28.50 元/千克`。
   - 若需调整，应通过「仓储管理 / 库存成本调整」流程修改源头，菜品管理只读引用。

3. **菜品成本计算**：
   - `dishCost = Σ(ingredient.unitCost × ingredient.quantity)`
   - 菜品管理仅做汇总计算，不保存原料单价明细；单价始终从库存实时或准实时获取。

### 10.2 门店库存 → 仓储管理的汇总关系

```text
门店库存（StoreInventory）
  │ 每日同步 / 实时事件
  ▼
仓储管理 / 门店库存查看（WarehouseStoreInventoryView）
  │ 按门店、物料聚合
  ▼
仓储管理 / 总库存（WarehouseInventory）
```

**规则**：

- 门店库存的每一笔入库、出库、调拨、盘点、报损，都应生成库存流水（InventoryLog）。
- 「门店库存查看」作为中间汇总层，按门店+物料展示当前库存、在途量、锁定量。
- 「总库存」汇总所有门店库存，形成集团/中央仓视角的库存总量。
- 其他模块（采购、产品中心、门店要货、成本分析）统一从「总库存」或「门店库存查看」读取，禁止各自维护一套原料价格。

### 10.3 跨模块同步机制

| 消费方 | 所需数据 | 推荐来源 | 同步方式 |
|--------|----------|----------|----------|
| 产品中心 / 菜品原料明细 | 原料名称、规格、单位、成本单价 | 门店库存 / 总库存 | 打开对话框时实时查询 |
| 采购管理 / 采购申请 | 原料名称、规格、参考单价 | 总库存 / 商品档案 | 打开表单时实时查询 |
| 门店管理 / 门店要货 | 当前库存、预警阈值 | 门店库存 | 打开表单时实时查询 |
| 财务中心 / 成本管理 | 原料成本、库存金额 | 总库存 + 库存流水 | 月末汇总或事件触发 |
| 食品追溯 / 原料追溯 | 批次、供应商、库存位置 | 库存流水 + 采购收货 | 按批次号关联 |

### 10.4 校验清单

- [ ] 菜品管理新增/编辑对话框中，原料名称下拉是否调用库存 API，而非本地硬编码。
- [ ] 选择原料后，`unitCost` 是否自动带出且不可手动修改（或修改后提示去库存调整）。
- [ ] 门店库存、门店库存查看、总库存三个层级的数据是否一致（数量、单位、成本）。
- [ ] 库存变动后，菜品成本分析、采购建议、财务报表是否能在合理时间内同步更新。
- [ ] 是否所有消费方都通过统一 API 获取库存数据，无重复维护的原料价格表。

---

## 九、参考文档

- [项目规则指南](../.trae/rules/project_rules.md)
- [前端文件修复记录](./frontend-repair-record.md)
- [数据业务链条梳理](./data-business-chain.md)
- [前端数据对齐治理计划](./frontend-alignment-plan.md)
- [采购链路字段四维对照表](./alignment/purchase-field-matrix.md)
- [采购链路状态机梳理](./alignment/purchase-state-machine.md)
- [采购链路操作矩阵](./alignment/purchase-action-matrix.md)
- [采购链路权限矩阵](./alignment/purchase-permission-matrix.md)
- [菜单-路由-页面标题一致性核查报告](./alignment/menu-title-consistency-report.md)
- [前端页面与菜单完整性核查报告](./alignment/missing-pages-report.md)
- [docs/spec/06-API接口设计.md](./spec/06-API接口设计.md)
- [docs/spec/08-组件与样式规范.md](./spec/08-组件与样式规范.md)
- [docs/spec/16-数据转换器规范.md](./spec/16-数据转换器规范.md)
