# 门店运营 / 订单 / 销售 / 产品中心字段四维对照表

> 范围：门店档案、门店证件、门店库存、门店要货、待办任务、日结对账、订单查询、退款管理、预约管理、菜品管理、套餐管理、分类管理  
> 用途：为前后端数据对齐、接口改造、字段命名统一提供依据  
> 生成日期：2026-07-30

---

## 1. 门店运营模块

### 1.1 门店档案（StoreArchive.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（stores_new） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|----------------------|---------|---------|---------|------|
| 门店编码 storeCode | 门店编码 storeCode | storeCode / storeCode | store_code | StoreArchiveCreateForm / StoreNew | POST /v1/store-archives | 总部/区域/店长 | 创建后不可编辑 |
| 门店名称 storeName | 门店名称 storeName | storeName / storeName | store_name | StoreArchive | PUT /v1/store-archives/{id} | 全部 | — |
| 门店类型 storeType | 门店类型 storeType | storeType / storeType(1/2/3) | store_type | StoreNew | PUT /v1/store-archives/{id} | 总部/区域/店长 | active=1/inactive=0/soldout=2 |
| 所属区域 area | 所属区域 area | area / area | area | StoreArchive | PUT /v1/store-archives/{id} | 全部 | 前端下拉 6 城硬编码 `[待修复]` |
| 地址 address | 地址 address | address / address | address | StoreNew | PUT /v1/store-archives/{id} | 全部 | — |
| 联系电话 phone | 联系电话 phone | phone / phone | phone | StoreNew | PUT /v1/store-archives/{id} | 全部 | 正则校验手机或固话 |
| 店长 managerId | 店长 managerId | managerId / managerId | manager_id | StoreArchive | PUT /v1/store-archives/{id} | 总部/区域/店长 | `[待修复]` 表单中 managerId 为 number，后端为 String |
| 营业状态 status | 营业状态 status | status / status(1/2/3/4) | status | StoreNew | PUT /v1/store-archives/{id}/status | 全部 | running=1/renovating=2/paused=3/closed=4 |
| 开业日期 openDate | 开业日期 openDate | openDate / openDate | open_date | StoreNew | PUT /v1/store-archives/{id} | 全部 | — |
| — | 营业时间 businessHoursStart/End | businessHoursStart / businessHoursStart(LocalTime) | business_hours_start/end | StoreNew | PUT /v1/store-archives/{id} | 全部 | 前端 string HH:mm:ss |
| — | 面积 areaSize | areaSize / areaSize | area_size | StoreNew | PUT /v1/store-archives/{id} | 总部/区域 | 仅总部可见敏感字段 |
| — | 许可证号 licenseNo | licenseNo / licenseNo | license_no | StoreNew | PUT /v1/store-archives/{id} | 全部 | 与 StoreCertificate 存在重叠 |
| — | 许可证到期日 licenseExpiry | licenseExpiry / licenseExpiry | license_expiry | StoreNew | PUT /v1/store-archives/{id} | 全部 | — |
| — | 配置 JSON configJson | configJson / configJson | config_json | StoreNew | PUT /v1/store-archives/{id} | 总部 | JSON 字符串 |
| — | 备注 remark | remark / remark | remark | StoreNew | PUT /v1/store-archives/{id} | 全部 | — |
| 创建时间 createTime | — | createTime / createTime | create_time | StoreNew | — | 全部 | — |
| 更新时间 updateTime | — | updateTime / updateTime | update_time | StoreNew | — | 全部 | — |

**门店档案字段问题**

1. `[待修复]` **导入/导出功能未实现**：`handleImport` / `handleExport` 仅提示“开发中”，无真实 API 调用。
2. `[待修复]` **店长 ID 类型不一致**：前端 `managerId: number | null`，后端 `stores_new.manager_id` 为 `VARCHAR(32)`，需统一为字符串。
3. `[待修复]` **区域下拉硬编码**：`areaOptions` 写死 6 个城市，未对接后端字典 API。

---

### 1.2 门店证件（StoreCertificate.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（health_certificate / certificate 逻辑） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|-----------------------------------------------|---------|---------|---------|------|
| 证件名称 certName | 证件名称 certName | certName / employeeName+健康证 | — | CreateCertificateDTO → HealthCertificateBackend | POST /v1/store/health-certificate/add | 全部 | 后端仅支持员工健康证 |
| 证件类型 certType | 证件类型 certType | certType(语义) / certType(1-7) | — | CertificateBackend | — | 全部 | 前端 7 种类型，后端仅 5→health_certificate |
| 持有人/单位 holderName | 持有人/单位 holderName | holderName / employeeName | employee_name | HealthCertificate | PUT /v1/store/health-certificate/update | 全部 | 后端无 holderType/company 概念 |
| 发证日期 issueDate | 发证日期 issueDate | issueDate / issueDate | issue_date | HealthCertificate | PUT /v1/store/health-certificate/update | 全部 | — |
| 到期日期 expiryDate | 到期日期 expiryDate | expiryDate / expiryDate | expiry_date | HealthCertificate | PUT /v1/store/health-certificate/update | 全部 | 前端据此计算 daysLeft、status |
| 剩余天数 daysLeft | — | daysLeft / 计算字段 | expiry_days | certificateDataConverter | — | 全部 | 前端计算 |
| 状态 status | 状态 status | status(active/expiring/expired/revoked) / status(valid/expiring/expired) | status | certificateDataConverter | PUT /v1/store/health-certificate/update | 全部 | revoked 后端映射为 expired |
| 续期费用状态 expenseStatus | — | expenseStatus / status(pending/approved/reimbursed) | status(health_certificate_expense) | HealthCertificateExpenseBackend | 内存演示，无真实 API | staff/store_manager/finance | `[待修复]` 报销流程仅前端内存 |
| — | 证件照片 fileUrl | fileUrl / certificateImage | certificate_image | HealthCertificate | PUT /v1/store/health-certificate/update | 全部 | 字段命名不一致 |
| — | 发证机关 issuer | issuer / issuer | issuer | HealthCertificate | PUT /v1/store/health-certificate/update | 全部 | — |
| — | 证件编号 certNumber | certNumber / certificateNumber | certificate_number | HealthCertificate | PUT /v1/store/health-certificate/update | 全部 | — |

**门店证件字段问题**

1. `[待修复]` **前后端模型严重错位**：前端为通用证照（营业执照/食品经营许可等 7 类），后端实体为 `HealthCertificate`（员工健康证），`certType` / `holderType` / `company` 等字段后端无法存储。
2. `[待修复]` **续期费用审批流程仅存内存**：`expenseRecords`、`expenseAuditLogs`、`flowNotifications` 均未持久化，刷新即丢失。
3. `[待修复]` **证件 ID 类型**：后端 `HealthCertificate.id` 为 String，但数据库迁移脚本未给出独立通用证照表。
4. `[待修复]` `revoked` 状态后端不支持，被归并到 `expired`。

---

### 1.3 门店库存（StoreInventory.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（store_inventory） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|---------------------------|---------|---------|---------|------|
| 商品编码 materialId | — | materialId / materialId | material_id | StoreInventoryInfo | — | 全部 | — |
| 商品名称 materialName | 商品名称 materialName | materialName / materialName | material_name | StoreInventoryInfo | — | 全部 | — |
| 规格 specification | 规格 specification | specification / specification | — | StoreInventoryInfo | — | 全部 | 数据库无此列 `[待修复]` |
| 单位 unit | 单位 unit | unit / unit | unit | StoreInventoryInfo | — | 全部 | — |
| 分类 category | — | category / category | — | 前端 `(row as any).category` | — | 全部 | `[待修复]` 类型 any，来源未明确 |
| 当前库存 quantity | 当前库存 currentQuantity | quantity / current_stock | current_stock | StoreInventoryInfo | POST /v1/store-inventory/adjust | 全部 | 字段命名不统一 quantity vs current_stock |
| 库存单价 unitCost | — | unitCost(分→元) / unitCost | unit_cost | StoreInventoryInfo | — | 全部 | ✅ 已返回（unit_cost 列，converter `fenToYuan`，2026-07-07 加） |
| 库存金额 totalCost | — | totalCost(分→元) / totalCost | total_cost | StoreInventoryInfo | — | 全部 | ✅ 已返回（total_cost 列，converter `fenToYuan`，2026-07-07 加） |
| 预警阈值 warningThreshold | — | warningThreshold / safety_stock | safety_stock | StoreInventoryInfo | — | 全部 | — |
| 库存状态 status | — | status / 计算字段 | — | 前端根据 availableQuantity/quantity 计算 | — | 全部 | — |
| 最后更新时间 updateTime | — | updateTime / update_time | update_time | StoreInventoryInfo | — | 全部 | — |
| — | 调整类型 adjustType | adjustType / type(in/out) | — | AdjustFormData | POST /v1/store-inventory/adjust | 店长/员工 | — |
| — | 调整数量 quantity | quantity / quantity | — | AdjustFormData | POST /v1/store-inventory/adjust | 店长/员工 | — |
| — | 调整原因 reason | reason / remark | — | AdjustFormData | POST /v1/store-inventory/adjust | 店长/员工 | — |
| — | 要货数量 requestQuantity | requestQuantity / quantity | — | RequisitionFormData | POST /v1/store-inventory/adjust | 店长/员工 | `[待修复]` 要货申请复用 adjust API，未独立建单 |
| — | 期望到货日期 expectedDate | expectedDate / remark 中拼接 | — | RequisitionFormData | POST /v1/store-inventory/adjust | 店长/员工 | `[待修复]` 日期未结构化存储 |

**门店库存字段问题**

1. `unitCost`/`totalCost` 已返回（`unit_cost`/`total_cost` 列，2026-07-07 补充）；`specification`、`category` 在数据库 `store_inventory` 表中仍缺失 `[待修复]`，依赖前端 mock 或 any 透传。
2. `[待修复]` 要货申请在库存页直接调用 `storeInventoryApi.adjust`，未生成 `store_material_request` 单据，与“门店要货”模块数据未打通。
3. `[待修复]` 库存状态为前端计算（`availableQuantity/quantity`），但数据库表无 `available_quantity` 字段。

---

### 1.4 门店要货（StoreMaterialRequest.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（material_request，推断） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|--------------------------------|---------|---------|---------|------|
| 申请单号 requestNo | — | requestNo / requestNo | request_no | MaterialRequestInfo | — | 全部 | — |
| 申请门店 storeName | 申请门店 storeName | storeName / storeName | store_name | MaterialRequestInfo | POST /v1/material-requests | 全部 | 下拉为 mock 门店 `[待修复]` |
| 申请人 applicantName | — | applicantName / applicantName | applicant_name | MaterialRequestInfo | — | 全部 | — |
| 申请日期 createTime | — | createTime / create_time | create_time | MaterialRequestInfo | — | 全部 | — |
| 状态 status | — | status(draft/pending/approved/rejected/converted) / status | status | MaterialRequestInfo | POST /v1/material-requests/{id}/submit | 全部 | — |
| 紧急程度 priority | 紧急程度 priority | priority / priority | priority | MaterialRequestInfo | PUT /v1/material-requests/{id} | 全部 | 表单写死为 'normal' |
| 物资种类数 itemCount | — | itemCount / items.length | — | MaterialRequestInfo | — | 全部 | 前端计算 |
| — | 申请标题 title | title / title | title | FormDataState | POST /v1/material-requests | 全部 | — |
| — | 期望到货日期 expectedDate | expectedDate / expectedDate | expected_date | FormDataState | POST /v1/material-requests | 全部 | — |
| — | 申请说明 description | description / remark | remark | FormDataState | POST /v1/material-requests | 全部 | 字段命名不一致 |
| — | 明细 materialName/quantity/unit/estimatedPrice | items / items | material_request_items | FormItemData | POST /v1/material-requests | 全部 | 表单 purpose 字段未回写 |

**门店要货字段问题**

1. `[待修复]` `storeOptions` 为硬编码 mock 数据，未对接真实门店 API。
2. `[待修复]` 编辑回显时 `priority` 被强制重置为 `'normal'`，与后端实际值不一致。
3. `[待修复]` 表单字段 `description` 对应后端 `remark`，命名不统一。

---

### 1.5 待办任务（StorePendingTasks.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（pending_tasks） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|--------------------------|---------|---------|---------|------|
| 任务类型 taskType | — | taskType / task_type | task_type | PendingTask | — | 全部 | approval/inspection/refund/settlement/certificate/audit/maintenance/other |
| 任务描述 title | — | title / title | title | PendingTask | — | 全部 | — |
| 来源 sourceModule | — | sourceModule / source_type | source_type | PendingTask | — | 全部 | 字段命名不一致 |
| 负责人 assigneeName | — | assigneeName / assignee_name | assignee_id / assignee_role | PendingTask | — | 全部 | — |
| 截止时间 deadline | — | deadline / due_date | due_date | PendingTask | — | 全部 | — |
| 优先级 priority | — | priority / priority(1/2/3/4) | priority | PendingTask | — | 全部 | 前端 urgent/high/medium/low，后端 1=低 2=中 3=高 4=紧急 `[待修复]` |
| 状态 status | — | status / status | status | PendingTask | — | 全部 | pending/completed/expired |
| — | 跳转链接 redirectUrl | redirectUrl / redirect_url | redirect_url | PendingTask | — | 全部 | — |

**待办任务字段问题**

1. `[待修复]` 优先级前后端数值映射相反：前端 `urgent=4/high=3/medium=2/low=1`，但后端注释写 `1=低 2=中 3=高 4=紧急`，需确认一致性。
2. `[待修复]` 表格字段 `sourceModule` 实际映射后端 `source_type`，命名不统一。

---

### 1.6 日结对账（StoreDailySettlement.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（daily_settlements） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|------------------------------|---------|---------|---------|------|
| 日结日期 settlementDate | 日结日期 settlementDate | settlementDate / settlementDate | settlement_date | DailySettlement | POST /v1/daily-settlements | 店长/财务 | — |
| 门店 storeName | 门店 storeName | storeName / storeName | store_id / store_name | DailySettlement | — | 总部/区域 | — |
| 营业额 totalRevenue | 营业额 totalRevenue | totalRevenue / totalRevenue(分) | total_revenue | SettlementBackend | PUT /v1/daily-settlements/{id} | 全部 | 敏感字段仅总部可见 |
| 订单数 orderCount | 订单数 orderCount | orderCount / orderCount | order_count | SettlementBackend | — | 全部 | — |
| 退款金额 refundAmount | — | refundAmount / — | — | DailySettlement | — | 全部 | `[待修复]` 数据库无此列，后端未返回 |
| 实收金额 netProfit | 实收金额 netProfit | netProfit / netProfit(分) | net_profit | SettlementBackend | PUT /v1/daily-settlements/{id} | 总部/财务 | 敏感字段 |
| 收银员 auditorName | 收银员 cashier | auditorName / auditor_name | auditor_id / auditor_name | SettlementBackend | — | 全部 | 字段命名不一致 cashier vs auditor |
| 日结状态 status | 日结状态 status | status(pending/approved/rejected) / status | status | SettlementBackend | PUT /v1/daily-settlements/{id}/status | 店长/财务 | pending=0/approved=1/rejected=2 |
| 日结时间 confirmTime | 日结时间 confirmTime | confirmTime / — | — | DailySettlement | — | 全部 | 数据库无此列 `[待修复]` |

**日结对账字段问题**

1. `[待修复]` `refundAmount`、`confirmTime` 在数据库 `daily_settlements` 中不存在。
2. `[待修复]` 收银员字段前端显示列名为“收银员”，数据字段为 `auditorName`（auditor 语义偏审计）。
3. `[待修复]` `total_cost`、`gross_profit_rate` 等敏感字段在前端表格未体现隐藏逻辑，但规范要求仅总部可见。

---

## 2. 订单 / 销售 / 产品中心模块

### 2.1 订单查询（OrderQuery.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（orders） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|-------------------|---------|---------|---------|------|
| 订单号 orderCode | — | orderCode / orderCode | order_code | OrderBackend | — | 全部 | — |
| 下单时间 createTime | — | createTime / createTime | create_time | OrderBackend | — | 全部 | — |
| 门店 storeName | — | storeName / storeName | store_id（POS） | OrderBackend | — | 全部 | POS 订单才有门店 |
| 顾客信息 customerInfo | — | customerName+customerPhone / customer_name+phone | customer_name / customer_phone | OrderBackend | — | 全部 | 前端拼接 |
| 商品明细 itemsSummary | — | items / order_items | order_items | OrderBackend | — | 全部 | 前端摘要 |
| 订单金额 finalAmount | — | finalAmount / finalAmount(分) | final_amount | OrderBackend | — | 全部 | 元/分转换 |
| 支付方式 paymentMethod | — | paymentMethod / payment_method | order_payment_records | OrderBackend | — | 全部 | 表格列有但类型未定义 `[待修复]` |
| 订单状态 orderStatus | — | orderStatus(语义) / orderStatus(0-6) | order_status | OrderBackend | POST /v1/orders/{id}/cancel | 全部 | pending=0/confirmed=1/completed=2/cancelled=3/partial_refund=4/full_refund=5/pending_review=6 |
| — | 订单详情 items/payments/refunds | items / items | order_items / order_payment_records / order_refund_records | OrderBackend | — | 全部 | — |

**订单查询字段问题**

1. `[待修复]` `paymentMethod` 在 `Order` 类型中未定义，表格直接引用可能 undefined。
2. `[待修复]` 退款操作在订单查询页仅提示“前往退款管理页处理”，未提供真实退款创建 API。
3. `[待修复]` POS 订单（orderId 以 O 开头）与线上订单（numeric id）走不同接口，字段对齐存在风险。

---

### 2.2 退款管理（OrderRefund.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（order_refund_records） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|--------------------------------|---------|---------|---------|------|
| 退款单号 refundNo | — | refundNo / refundNo | refund_no | OrderRefundBackend | — | 全部 | — |
| 关联订单号 orderCode | — | orderCode / orderCode | order_id → orders.order_code | OrderRefundBackend | — | 全部 | — |
| 退款类型 refundType | — | refundType / refund_type(1/2) | refund_type | OrderRefundBackend | — | 全部 | `[待修复]` 类型未提供 refundType，前端靠关键字判断 |
| 退款金额 refundAmount | 退款金额 refundAmount | refundAmount / refundAmount(分) | refund_amount | OrderRefundBackend | PUT /v1/order-refunds/{id}/approve | 财务/店长 | 元/分转换 |
| 申请时间 createTime | — | createTime / createTime | create_time | OrderRefundBackend | — | 全部 | — |
| 退款状态 refundStatus | — | refundStatus(语义) / refundStatus(0-3) | refund_status | OrderRefundBackend | PUT /v1/order-refunds/{id}/approve | 财务/店长 | pending=0/approved=1/rejected=2/executed=3 |
| 申请人 applyUserName | — | applyUserName / apply_user_name | — | OrderRefundBackend | — | 全部 | `[待修复]` 数据库无 apply_user_name 列 |
| — | 审核意见 approveRemark | approveRemark / approveRemark | — | approveFormData | PUT /v1/order-refunds/{id}/approve | 财务/店长 | — |
| — | 调整后金额 refundAmount | refundAmount / refundAmount(分) | refund_amount | approveFormData | PUT /v1/order-refunds/{id}/approve | 财务/店长 | 通过时允许修改金额 |

**退款管理字段问题**

1. `[待修复]` `OrderRefund` 类型缺少 `refundType` 字段，前端靠 `refundReason` 含“部分”关键字兜底判断，不可靠。
2. `[待修复]` `applyUserName` 后端/数据库未返回，表格可能显示“-”。
3. `[待修复]` 退款类型筛选下拉未绑定 `v-model`，只触发 `@change`，无法实际筛选。

---

### 2.3 预约管理（OrderReservation.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（table_reservations） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|--------------------------------|---------|---------|---------|------|
| 预约编号 reservationCode | — | reservationCode / reservationCode | reservation_code | ReservationBackend | — | 全部 | — |
| 预约人 customerName | 预约人 customerName | customerName / customerName | customer_name | ReservationCreateForm | POST /v1/reservations | 全部 | — |
| 联系电话 customerPhone | 联系电话 customerPhone | customerPhone / customerPhone | customer_phone | ReservationCreateForm | POST /v1/reservations | 全部 | — |
| 预约日期 reservationDate | 预约日期 reservationDate | reservationDate / reservationDate | reservation_date | ReservationCreateForm | POST /v1/reservations | 全部 | — |
| 预约时间 reservationTime | 预约时间 reservationTime | reservationTime / reservationTime | — | ReservationCreateForm | POST /v1/reservations | 全部 | `[待修复]` 数据库迁移脚本被截断，字段未确认 |
| 用餐人数 peopleCount | 用餐人数 peopleCount | peopleCount / peopleCount | people_count | ReservationCreateForm | POST /v1/reservations | 全部 | — |
| 桌号/包间 tableInfo | 桌台 tableId | tableId / tableId | table_id | ReservationCreateForm | POST /v1/reservations | 全部 | — |
| 预约状态 status | — | status(语义) / status(1-5) | status | ReservationBackend | POST /v1/reservations/{id}/confirm | 全部 | pending=1/confirmed=2/arrived=3/cancelled=4/no_show=5 |
| 门店 storeName | 门店 storeId | storeId / storeId | — | ReservationCreateForm | POST /v1/reservations | 全部 | `[待修复]` 数据库迁移脚本被截断 |
| — | 定金 depositAmount | depositAmount / depositAmount | — | ReservationCreateForm | POST /v1/reservations | 全部 | `[待修复]` 数据库字段未确认 |
| — | 备注 remark | remark / remark | remark | ReservationCreateForm | POST /v1/reservations | 全部 | — |

**预约管理字段问题**

1. `[待修复]` 数据库迁移脚本 `V6.0.0__create_order_product_tables.sql` 中 `table_reservations` 表定义被截断，`reservation_time`、`store_id`、`deposit_amount` 等字段是否落地未确认。
2. `[待修复]` 编辑预约时 `storeId` 从桌台反推，可能覆盖后端真实值。
3. `[待修复]` 详情抽屉字段 `confirmedBy` 实际填充 `confirmTime`，语义不匹配。

---

### 2.4 菜品管理（FoodManagement.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（foods） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|------------------|---------|---------|---------|------|
| 菜品信息 foodName | 菜品名称 foodName | foodName / foodName | food_name | FoodBackend | POST /v1/product-center/foods | 全部 | — |
| 分类 categoryName | 分类 categoryId | categoryId / categoryId | category_id | FoodBackend | POST /v1/product-center/foods | 全部 | — |
| 售价 salePrice | 售价 salePrice | salePrice(元) / salePrice(分) | sale_price | FoodBackend | POST /v1/product-center/foods | 全部 | 元/分转换 |
| 成本 costPrice | 成本 costPrice | costPrice(元) / costPrice(分) | cost_price | FoodBackend | POST /v1/product-center/foods | 全部 | 由原料自动计算 |
| 利润率 profitRate | — | profitRate / profitRate | profit_rate | FoodBackend | — | 全部 | — |
| 库存 stock | 库存 stock | stock / stock | stock | FoodBackend | POST /v1/product-center/foods | 全部 | — |
| 状态 foodStatus | 状态 foodStatus | foodStatus(语义) / status(0-2) | status | FoodBackend | PUT /v1/product-center/foods/{id}/status/{status} | 全部 | active=1/inactive=0/soldout=2 |
| 创建时间 createTime | — | createTime / createTime | create_time | FoodBackend | — | 全部 | — |
| — | 规格 specification | specification / specification | specification | FoodFormData | POST /v1/product-center/foods | 全部 | — |
| — | 单位 unit | unit / unit | unit | FoodFormData | POST /v1/product-center/foods | 全部 | — |
| — | 最低库存 minStock | 最低库存 minStock | min_stock | FoodFormData | POST /v1/product-center/foods | 全部 | — |
| — | 图片 imageUrl | 图片 imageUrl | image_url | FoodFormData | POST /v1/product-center/foods | 全部 | — |
| — | 描述 description | 描述 description | description | FoodFormData | POST /v1/product-center/foods | 全部 | — |
| — | 制作时间 cookingTime | 制作时间 cookingTime | cooking_time | FoodFormData | POST /v1/product-center/foods | 全部 | — |
| — | 原料 ingredients | 原料 recipes | dish_recipes | FoodFormData | POST /v1/product-center/foods | 全部 | 前端 ingredients，后端 recipes |

**菜品管理字段问题**

1. `[待修复]` 表单中 `foodStatus` 字段名为 `foodStatus`，提交时转换为 `status`；`ingredients` 提交时转换为 `recipes`，命名不一致。
2. `[待修复]` 原料明细 `unitPrice` 在表单为元，提交时转为分，但 `FoodBackend.recipes` 仍返回分，转换层级分散。
3. `[待修复]` 批量导入/导出模板部分实现，但导出 API 调用中 responseType 需确认后端是否支持。

---

### 2.5 套餐管理（DishCombo.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（dish_combos） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|------------------------|---------|---------|---------|------|
| 套餐编码 comboCode | 套餐编码 comboCode | comboCode / comboCode | combo_code | ComboBackend | POST /v1/product-center/combos | 全部 | — |
| 套餐名称 comboName | 套餐名称 comboName | comboName / comboName | combo_name | ComboBackend | POST /v1/product-center/combos | 全部 | — |
| 套餐价 comboPrice | 套餐价 comboPrice | comboPrice(元) / comboPrice(分) | combo_price | ComboBackend | POST /v1/product-center/combos | 全部 | 元/分转换 |
| 原价 originalPrice | 原价 originalPrice | originalPrice(元) / originalPrice(分) | original_price | ComboBackend | POST /v1/product-center/combos | 全部 | — |
| 状态 comboStatus | 状态 status | comboStatus(语义) / status(0-2) | status | ComboBackend | PUT /v1/product-center/combos/{id}/status/{status} | 全部 | active=1/inactive=0 |
| 每日限量 dailyLimit | 每日限量 dailyLimit | dailyLimit / dailyLimit | daily_limit | ComboBackend | POST /v1/product-center/combos | 全部 | — |
| 今日已售 soldToday | 今日已售 soldToday | soldToday / soldToday | sold_today | ComboBackend | — | 全部 | — |
| 有效期 validStartDate/End | 有效期 validStartDate/End | validStartDate / validStartDate | valid_start_date/end | ComboBackend | POST /v1/product-center/combos | 全部 | — |
| — | 套餐明细 ingredients | 套餐明细 ingredients | combo_ingredients | DishComboFormData | POST /v1/product-center/combos | 全部 | — |

**套餐管理字段问题**

1. `[待修复]` `DishComboRow.totalCost` 定义为 `number`，而 `DishCombo.totalCost` 为元字符串，类型不一致。
2. `[待修复]` `comboType`（fixed/optional/discount）后端 `dish_combos` 表无此列。

---

### 2.6 分类管理（CategoryManagement.vue）

| 表格列 | 对话框字段 | API 字段（前端 / 后端） | 数据库字段（food_categories） | 数据来源 | 回写 API | 可见角色 | 备注 |
|-------|-----------|----------------------|----------------------------|---------|---------|---------|------|
| 分类名称 categoryName | 分类名称 categoryName | categoryName / categoryName | category_name | CategoryBackend | POST /v1/product-center/categories | 全部 | — |
| 父级 parentId | 父级 parentId | parentId / parentId | parent_id | CategoryBackend | POST /v1/product-center/categories | 全部 | 0 表示顶级 |
| 图标 iconUrl | 图标 iconUrl | iconUrl / iconUrl | icon_url | CategoryBackend | POST /v1/product-center/categories | 全部 | — |
| 排序 sortOrder | 排序 sortOrder | sortOrder / sortOrder | sort_order | CategoryBackend | POST /v1/product-center/categories | 全部 | — |
| 状态 categoryStatus | 状态 status | categoryStatus(语义) / status(1/2) | status | CategoryBackend | PUT /v1/product-center/categories/{id}/status/{status} | 全部 | active=1/inactive=0 |
| 菜品数 foodCount | — | foodCount / foodCount | — | CategoryBackend | — | 全部 | 后端统计字段 |

---

## 3. 跨模块公共字段问题汇总

| 问题 | 影响模块 | 风险等级 | 修复建议 |
|-----|---------|---------|---------|
| 通用证照模型与健康证实体错位 | StoreCertificate | 高 | 拆分为通用 `certificates` 表或单独健康证页面 |
| 续期费用审批流程仅存内存 | StoreCertificate | 高 | 后端落地 `certificate_expense` / `approval_log` |
| 门店库存表字段缺失 | StoreInventory | 高 | 补充 `specification/category/available_quantity/unit_cost` |
| POS 订单与线上订单双接口并存 | OrderQuery | 中 | 统一 `orders` 表或统一 DTO |
| 预约表数据库定义不完整 | OrderReservation | 中 | 补齐 `table_reservations` 迁移脚本 |
| 退款类型字段缺失 | OrderRefund | 中 | 后端补充 `refund_type` |
| 门店档案导入/导出未实现 | StoreArchive | 低 | 对接后端 import/export API |
| 要货申请 storeOptions mock | StoreMaterialRequest | 低 | 对接 `useStoreOptions` |

---

## 4. 金额 / 状态转换速查

| 模块 | 金额单位 | 状态前后端映射 | 转换器 |
|-----|---------|---------------|-------|
| 门店档案 | 面积 decimal，其余无金额 | running=1/renovating=2/paused=3/closed=4 | store-archive.ts 手动映射 |
| 门店库存 | 单价/金额 元（前端未明确后端分） | normal/warning/low/over 前端计算 | 无 |
| 门店要货 | estimatedPrice 元 | draft/pending/approved/rejected/converted 语义字符串 | materialRequestConverter |
| 日结对账 | 金额 分 ↔ 元 | pending=0/approved=1/rejected=2 | settlementDataConverter |
| 订单 | 金额 分 ↔ 元 | pending=0/confirmed=1/completed=2/cancelled=3/partial_refund=4/full_refund=5/pending_review=6 | orderDataConverter |
| 退款 | 金额 分 ↔ 元 | pending=0/approved=1/rejected=2/executed=3 | orderDataConverter |
| 预约 | 定金 元 | pending=1/confirmed=2/arrived=3/cancelled=4/no_show=5 | orderDataConverter |
| 菜品/套餐/分类 | 价格 分 ↔ 元 | active=1/inactive=0/soldout=2 | productDataConverter |
