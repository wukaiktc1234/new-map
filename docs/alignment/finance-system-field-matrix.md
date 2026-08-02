# 全量数据对齐治理 · 字段矩阵

> 范围：会员营销、财务中心、系统管理、印章/签约、运营决策  
> 说明：以下矩阵基于 `frontend/src/types`、`frontend/src/api`、`frontend/src/views` 下相关文件梳理；不一致/缺失/命名不统一处已用 `[待修复]` 标注。

---

## 一、跨模块通用字段对齐

| 字段语义 | 会员营销 | 财务中心 | 系统管理 | 印章/签约 | 运营决策 | 对齐结论 |
|---------|---------|---------|---------|----------|---------|---------|
| 主键 ID | `id` / `levelId` / `planId` / `recordId` | `id` | `id` | `sealId` / `logId` | 无独立实体 | 主键命名不统一，存在 `id`、`xxxId` 两种风格 `[待修复]` |
| 业务编号 | `memberNo` / `recordNo` | `voucherNo` / `invoiceNo` / `payableNo` / `receivableNo` | `roleCode` / `permissionCode` | `businessNo` | — | 业务编号字段命名较规范，但缺少统一编码规则文档 `[待修复]` |
| 金额单位（后端） | 元（字符串，如 `balance: string`） | 分（整数，如 `amount: number`） | — | — | 元/分混用 | 金额单位跨模块未对齐 `[待修复]` |
| 金额单位（前端展示） | 元 | 元 | — | — | 元 | 展示层统一为元，但转换位置分散 |
| 创建时间 | `createdAt` / `createTime` 混用 | `createTime` | `createdAt` | `createTime` | — | 时间字段命名不统一 `[待修复]` |
| 更新时间 | `updatedAt` / `updateTime` 混用 | `updateTime` | `updatedAt` | `updateTime` | — | 同上 `[待修复]` |
| 状态字段 | `status: string`（语义字符串） | 多数为语义字符串 + Converter 映射 | `status: number`（1/0） | `status: string` | `status: string` | 状态字段类型不统一 `[待修复]` |
| 备注 | `remark` | `remark` | `roleDescription` / `remark` | `remark` | — | 基本统一为 `remark` |
| 操作人/创建人 | `operator` / `createBy` / `operateUserName` | `creatorName` / `createUserName` | `createdAt`（仅时间） | `operator` / `createBy` | — | 操作人字段命名分散 `[待修复]` |

---

## 二、会员营销 · 字段矩阵

### 2.1 会员信息（`types/member.ts` → `MemberInfo`）

| 前端字段 | 类型 | 后端字段（推断） | 说明/问题 |
|---------|------|----------------|----------|
| `id` | `string` | `member_id` | 主键 |
| `memberNo` | `string` | `member_no` | 业务编号 |
| `phone` | `string` | `phone` | 手机号 |
| `nickname` | `string` | `nickname` | 昵称 |
| `gender` | `MemberGender` | `gender` | `unknown/male/female` |
| `memberLevelId` | `string` | `member_level_id` | 等级 ID |
| `levelName` | `string` | 关联查询 | 等级名称 |
| `balance` | `string` | `balance` | 余额（元，字符串） |
| `totalRecharge` | `string` | `total_recharge` | 累计充值（元） |
| `totalConsume` | `string` | `total_consume` | 累计消费（元） |
| `status` | `MemberStatus` | `status` | `active/frozen/blacklisted/cancelled` |
| `customerSegment` | `CustomerSegment` | `customer_segment` | RFM 分层 |
| `preferences` | `UserPreferences` | `preferences`（JSONB） | 用户偏好 |
| `createdAt` | `string` | `create_time` | `[待修复]` 前端 `createdAt` 与数据库 `create_time` 命名需确认 MyBatis 映射 |
| `updatedAt` | `string` | `update_time` | `[待修复]` 同上 |

### 2.2 会员等级（`types/member-level.ts` → `MemberLevelInfo`）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `levelId` | `string` | 主键，命名风格为 `xxxId` |
| `levelName` | `string` | 等级名称 |
| `levelCode` | `string` | 等级编码（自由输入） |
| `minConsumption` | `string` | 升级最低消费金额（元，字符串） |
| `discountRate` | `number` | 折扣率 |
| `status` | `string` | `active/inactive`，与通用状态数字编码不一致 `[待修复]` |
| `criteria` / `benefits` | 数组 | 高级配置，字段内 `status` 同样为字符串 |

### 2.3 储值记录（`types/member-recharge.ts` → `RechargeRecordInfo`）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `recordId` | `string` | 主键 |
| `recordNo` | `string` | 流水号 |
| `rechargeAmount` | `string` | 充值金额（元） |
| `principalAmount` | `string` | 本金（元） |
| `bonusAmount` | `string` | 赠送金额（元） |
| `paymentStatus` | `RechargePaymentStatus` | 类型定义为 `pending/paid/refunded/failed`，但 `RechargeManage.vue` 中映射了 `partial_refunded` `[待修复]` |
| `refundStatus` | `RechargeRefundStatus` | `none/pending/approved/rejected/refunded` |

---

## 三、财务中心 · 字段矩阵

### 3.1 通用财务字段

| 前端字段 | 类型 | 后端字段 | 说明/问题 |
|---------|------|---------|----------|
| `id` | `string` | 各表主键 | 主键统一为 `id` |
| `amount` / `debitTotal` / `creditTotal` | `number` | 对应字段 | 金额单位为分，通过 `fenToYuanNumber` 转换展示 |
| `status` | 语义字符串 | 数字编码 | 通过 `FinanceXxxStatusMap` 转换 |
| `createTime` | `string` | `create_time` | 命名统一 |
| `updateTime` | `string` | `update_time` | 命名统一 |

### 3.2 会计科目（`types/finance.ts` → `FinanceSubject`）

| 前端字段 | 类型 | 后端数字编码 | 说明/问题 |
|---------|------|-------------|----------|
| `subjectType` | `SubjectType` | `1~6` | `asset/liability/equity/cost/income/profit` |
| `balanceDirection` | `BalanceDirection` | — | `debit/credit` |
| `status` | `SubjectStatus` | `1/0` | `active/inactive` |
| `balance` | `number` | — | 余额（分） |

### 3.3 凭证（`types/finance.ts` → `FinanceVoucherNew`）

| 前端字段 | 类型 | 后端数字编码 | 说明/问题 |
|---------|------|-------------|----------|
| `voucherType` | `VoucherType` | `1~4` | `receipt/payment/transfer/general` |
| `status` | `VoucherStatus` | `1~4` | `draft/audited/posted/cancelled` |
| `debitTotal` / `creditTotal` | `number` | — | 分 |
| `attachmentCount` | `number` | — | 附件张数 |

### 3.4 应收/应付账款

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `amount` | `number` | 应收/应付总额（分） |
| `receivedAmount` / `paidAmount` | `number` | 已收/已付（分） |
| `remainAmount` | `number` | 未收/未付（分） |
| `status` | `ReceivableStatus` / `PayableStatus` | `unpaid/partial/settled/overdue` |
| `aging` | `string` | 账龄，建议统一计算口径 `[待修复]` |

### 3.5 发票（`types/finance.ts` → `FinanceInvoiceNew`）

| 前端字段 | 类型 | 后端数字编码 | 说明/问题 |
|---------|------|-------------|----------|
| `invoiceType` | `FinanceInvoiceType` | — | `special/normal/electronic/electronic_special` |
| `status` | `FinanceInvoiceStatus` | — | `draft/issued/cancelled/red_flushed` |
| `amountWithoutTax` / `taxAmount` / `totalAmount` | `number` | — | 分 |
| `taxRate` | `number` | — | 百分比 |

### 3.6 报销单（`types/finance.ts` → `InvoiceReimbursementVO`）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `reimbursementId` | `string` | 主键命名不一致 `[待修复]` |
| `status` | `ReimbursementStatus` | `draft/approved/paid/cancelled/rejected` |
| `paymentStatus` | `number` | `0-未付款 1-已付款`，与 `status` 存在状态交叉 `[待修复]` |
| `totalAmount` / `approvedAmount` | `number` | 分 |

### 3.7 付款单/收款单（四账联动）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `paymentAmount` / `receiptAmount` | `number` | 分 |
| `paymentMethod` / `receiptMethod` | enum | `bank_transfer/cash/check` |
| `status` | `PaymentStatus` / `ReceiptStatus` | `confirmed/voided` |
| `fundFlowId` / `voucherId` | `string` | 关联资金流水、凭证 |

---

## 四、系统管理 · 字段矩阵

### 4.1 权限码（`api/system/permission.ts` → `PermissionVO`）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `id` | `string` | 后端 Long 经 ToStringSerializer 序列化 |
| `permissionCode` | `string` | 权限编码 |
| `permissionName` | `string` | 权限名称 |
| `permissionType` | `number` | `1-菜单 / 2-按钮 / 3-接口` |
| `module` | `string` | 所属模块 |
| `parentId` | `string` | 父级权限 ID |
| `status` | `number` | `1-启用 / 0-禁用` |
| `path` / `component` | `string` | 前端路由/组件路径 |

### 4.2 角色（`api/system/role.ts` 推断）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `id` | `number` | 与权限码 `string` 主键类型不一致 `[待修复]` |
| `roleCode` | `string` | 角色编码 |
| `roleName` | `string` | 角色名称 |
| `roleDescription` | `string` | 角色描述 |
| `roleType` | `number` | `1-系统角色 / 2-自定义角色` |
| `status` | `number` | `1-启用 / 0-禁用` |
| `systemBuilt` | `boolean` | 是否系统内置 |

### 4.3 域权限模板（`types/domain-permission.ts`）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `domainCode` | `string` | 业务域代码 |
| `domainName` | `string` | 业务域中文名 |
| `accessLevel` | `DomainAccessLevel` | `FULL/READ_ONLY/LIMITED/HIDDEN` |

---

## 五、印章/签约 · 字段矩阵

### 5.1 印章信息（`types/seal.ts` → `SealInfo`）

| 前端字段 | 类型 | 后端字段 | 说明/问题 |
|---------|------|---------|----------|
| `sealId` | `string` | `seal_id` | 主键 |
| `sealName` | `string` | `seal_name` | 印章名称 |
| `sealType` | `SealType` | `seal_type` | `official/finance/contract/legal/custom` |
| `sealImage` | `string` | `seal_image_url` | `[待修复]` 前端字段 `sealImage`，后端字段 `sealImageUrl`，已在 API 层转换但命名需对齐 |
| `status` | `SealStatus` | `status` | `active/inactive/revoked` |
| `keeper` | `string` | `keeper` | 保管人 |
| `authorizedUsers` | `string[]` | `authorized_users` | 授权使用人 |
| `authorizedScenes` | `SealScene[]` | `authorized_scenes` | `hr_contract/purchase_contract/electronic_contract` |
| `createBy` | `string` | `create_by` | 创建人 |
| `createTime` / `updateTime` | `string` | `create_time` / `update_time` | 统一 |

### 5.2 印章使用记录（`types/seal.ts` → `SealUsageLog`）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `logId` | `string` | 主键 |
| `businessType` | `SealScene` | 业务类型 |
| `businessId` / `businessNo` | `string` | 业务 ID/编号 |
| `operator` | `string` | 操作人 |
| `ipAddress` | `string` | IP 地址 |

---

## 六、运营决策 · 字段矩阵

### 6.1 决策看板（`views/operations/DecisionBoard.vue`）

| 前端字段 | 类型 | 说明/问题 |
|---------|------|----------|
| `totalRevenue` / `totalOrders` / `avgOrderValue` 等 | `number` | 核心指标 |
| `storeId` | `string` | 门店筛选 |
| `timeRange` | `TimeRangeType` | `today/week/month/quarter/year/custom` |
| `dateRange` | `string[]` | 自定义日期范围 |

### 6.2 数据接口字段（`api/operations/decision-board.ts`）

| 前端字段 | 后端来源 | 说明/问题 |
|---------|---------|----------|
| `storeName` / `revenue` / `cost` / `profit` | `StoreRankBackend` | 字段兼容 `storeName`/`name` |
| `name` / `value` / `growth` | `CategoryBackend` | 品类销售 |
| `label` / `score` / `icon` | `HealthScoreBackend` | 健康评分 |

> `[待修复]` 决策看板大量核心指标接口（`stats-overview`、`revenue-trend`、`cost-component`、`member-growth`、`inventory-warning`、`kpi-compare`）当前为 TODO 占位，未接入真实后端，字段定义尚未与后端对齐。

---

## 七、核心字段对齐问题汇总

| 序号 | 问题描述 | 影响模块 | 修复建议 |
|-----|---------|---------|---------|
| 1 | 主键命名风格不一致：`id` vs `xxxId` | 全模块 | 统一主键命名规范：业务实体表建议 `{table}_id`，前端类型同步 |
| 2 | 金额单位不统一：会员营销用「元字符串」，财务中心用「分整数」 | 会员营销、财务中心 | 跨模块交互时（如储值财务流水）统一转换位置，避免混用 |
| 3 | 时间字段命名混用：`createdAt/updatedAt` vs `createTime/updateTime` | 会员营销、系统管理 | 统一为 `createTime/updateTime` 以匹配数据库规范 |
| 4 | 状态字段类型不一致：`string` 语义值 / `number` 编码 | 全模块 | 按项目规范，后端数字、前端语义，通过 Converter 统一转换 |
| 5 | 发票报销中 `paymentStatus` 与 `status` 状态交叉 | 财务中心 | 明确状态机，避免一个实体存在两个并行状态字段 |
| 6 | 印章图片字段命名不一致：`sealImage` vs `sealImageUrl` | 印章/签约 | 前后端统一字段名，或明确在 Converter 中处理 |
| 7 | 决策看板大量指标接口未实现 | 运营决策 | 后端补齐接口，前端移除 TODO 占位 |
| 8 | 操作人字段命名分散：`operator` / `createBy` / `createUserName` / `operateUserName` | 全模块 | 统一为 `createBy` / `updateBy` 或 `operatorName` |
