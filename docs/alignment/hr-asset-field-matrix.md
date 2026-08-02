# 人事 / 资产 / 设备模块字段对齐矩阵

> 范围：人事管理（组织、员工、岗位、考勤、招聘、薪资、培训、健康证、合同、入职、合同模板、知识库）、资产管理、设备管理。
> 仅做梳理，未修改源码。`[待修复]` 标注字段命名、类型、映射不一致或缺失项。

---

## 1. 人事管理（HR）

### 1.1 员工（Employee）

| 前端字段（`types/hr.ts` / `api/hr/employee.ts`） | 后端字段（`entity/Employee.java`） | 类型 | 对齐状态 | 说明 / 问题 |
|-----------------------------------------------|----------------------------------|------|----------|-------------|
| `employeeId` / `id` | `id`（`EMPLOYEE_ID`） | 前端 string / 后端 Long | ✅ 已映射 | 后端 `@JsonSerialize(ToStringSerializer)` 输出字符串。 |
| `employeeName` / `name` | `name`（`EMPLOYEE_NAME`） | 前端 string / 后端 String | ✅ 已映射 | 命名不一致：后端用 `name`，前端用 `employeeName`。 |
| `employeeCode` | `employeeCode`（`EMPLOYEE_CODE`） | string | ✅ 已映射 | — |
| `gender` | `gender` | string | ✅ 已映射 | 均为字符串（`male/female/other`）。 |
| `phone` | `phone` | string | ✅ 已映射 | — |
| `email` | `email` | string | ✅ 已映射 | — |
| `departmentId` | `departmentId`（`department_id`） | 前端 string / 后端 Long | ✅ 已映射 | API 层做 Number/String 转换。 |
| `departmentName` | `departmentName`（`@TableField(exist = false)`） | string | ✅ 已映射 | 后端非数据库字段，VO 冗余。 |
| `positionId` | `positionId`（`position_id`） | 前端 string / 后端 Long | ✅ 已映射 | — |
| `positionName` | `positionName`（`@TableField(exist = false)`） | string | ✅ 已映射 | — |
| `storeId` | `storeId`（`store_id`） | 前端 string / 后端 Long | ✅ 已映射 | — |
| `status` | `status` | 前端语义 string / 后端 Integer | ✅ 已映射 | `EmployeeStatusMap`：后端 `1=active/0=inactive/2=probation`。 |
| `hireDate` | `hireDate`（`hire_date`） | 前端 string / 后端 LocalDateTime | ⚠️ 类型不一致 | 前端为 `string`，后端为 `LocalDateTime`；传输层为 ISO 8601。 |
| `resignDate` | `resignDate`（`resign_date`） | 前端 string / 后端 LocalDateTime | ⚠️ 类型不一致 | 同上。 |
| `employmentType` | `employmentType`（`employment_type`） | 前端语义 string / 后端 String | ⚠️ 语义未闭环 | 后端直接存储字符串（如 `full-time`），`EmploymentTypeMap` 却按 Integer 设计，**映射规则与实际后端不一致**。`[待修复]` |
| `baseSalary` | `baseSalary`（`base_salary`） | 前端 number（分） / 后端 BigDecimal（元） | ❌ 单位不一致 | 薪资模块 `salary.ts` 说明后端使用 BigDecimal 元，违反项目规范“金额统一用 Long 分”。`[待修复]` |
| `idCard` | `idCard`（`id_card`） | string | ✅ 已映射 | 前端 `Employee` 接口未显式声明，但后端存在。 |
| `bankCard` | `bankCard`（`bank_card`） | string | ✅ 已映射 | 同上，前端未显式声明。 |
| `address` | `address` | string | ✅ 已映射 | 前端未显式声明。 |
| `workLocationType` | `workLocationType`（`work_location_type`） | string | ❌ 前端缺失 | 后端存在，前端 `Employee` 类型未定义。`[待修复]` |
| `warehouseId` | `warehouseId`（`warehouse_id`） | 前端缺失 / 后端 Long | ❌ 前端缺失 | 员工归属仓库字段前端未暴露。`[待修复]` |
| `createdTime` / `updatedTime` | `createdTime` / `updatedTime` | string / LocalDateTime | ✅ 已映射 | — |

### 1.2 部门（Department / Organization）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `departmentId` / `id` | `departmentId` / `id` | string / Long | ⚠️ 命名不一致 | 前端 `Department` 用 `departmentId`，`DepartmentDTO` 用 `id`。 |
| `departmentName` / `name` | `departmentName` / `name` | string | ⚠️ 命名不一致 | 后端 `Department` 用 `departmentName`，`DepartmentDTO` 用 `name`。 |
| `parentId` | `parentId` | string/null / Long | ✅ 已映射 | — |
| `managerId` / `managerName` | `managerId` / `managerName` | string / Long | ✅ 已映射 | — |
| `status` | `status` | string | ✅ 已映射 | 语义字符串，未配置 `StatusMap`。 |
| `sortOrder` / `sort` | `sortOrder` | number | ⚠️ 命名不一致 | `DepartmentDTO` 用 `sort`。 |

### 1.3 职位（Position）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `positionId` / `id` | `positionId` / `id` | string / Long | ⚠️ 命名不一致 | 前端 `Position` 用 `id`，`PositionBasicInfo` 用 `positionId`。 |
| `positionName` | `positionName` | string | ✅ 已映射 | — |
| `positionCode` | `positionCode` | string | ✅ 已映射 | — |
| `departmentId` | `departmentId` | string / Long | ✅ 已映射 | — |
| `department` / `departmentName` | `departmentName` | string | ⚠️ 命名不一致 | 前端 `Position` 同时存在 `department` 和 `departmentName`。 |
| `status` | `status` | string | ✅ 已映射 | — |

### 1.4 考勤（Attendance）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `id` / `recordId` | `recordId` | string / number | ✅ 已映射 | `attendance.ts` 做 `recordId → id` 转换。 |
| `employeeCode` | `employeeNo` | string | ✅ 已映射 | 命名不一致，已转换。 |
| `status` | `status` | 前端 string / 后端 Integer | ✅ 已映射 | `1=normal/2=late/3=early_leave/4=absent/5=overtime/6=leave`。 |
| `shiftType` | — | string | ❌ 后端缺失 | 后端 `AttendanceRecordVO` 无此字段，前端默认 `'full_day'`。`[待修复]` |
| `clockInTime` / `clockOutTime` | `clockInTime` / `clockOutTime` + `attendanceDate` | string / LocalTime | ✅ 已映射 | 前端拼接日期与时间。 |
| `leaveType` | `leaveTypeName` | string | ⚠️ 仅展示名 | 后端返回中文名，前端无 `LeaveTypeMap` 反向映射依据。 |

### 1.5 薪资（Salary）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `period` | `salaryMonth` | string | ✅ 已映射 | `salary.ts` 做字段映射。 |
| `basicSalary` | `basicSalary` | number（分） / BigDecimal（元） | ❌ 单位不一致 | 后端违反规范使用 BigDecimal 元。`[待修复]` |
| `subsidy` | `allowance` | number（分） / BigDecimal（元） | ❌ 单位不一致 | 同上。 |
| `socialInsurance` | `insurance` | number（分） / BigDecimal（元） | ❌ 单位不一致 | 同上。 |
| `deductions` | `otherDeductions` | number（分） / BigDecimal（元） | ❌ 单位不一致 | 同上。 |
| `grossSalary` | `totalEarnings` | number（分） / BigDecimal（元） | ❌ 单位不一致 | 同上。 |
| `netSalary` | `finalSalary` | number（分） / BigDecimal（元） | ❌ 单位不一致 | 同上。 |
| `status` | `status` | 前端 `SalaryStatus` 枚举 / 后端中文字符串 | ⚠️ 语义映射 | 后端用中文“待确认/已确认/已发放”，前端用枚举。 |
| `paidAt` / `paymentMethod` / `bankTransactionNo` | — | — | ❌ 后端缺失 | 前端定义字段，后端 `SalaryRecord` 无对应列。`[待修复]` |

### 1.6 合同（Contract）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `status` | `status` | 前端语义 string / 后端 Integer | ✅ 已映射 | `ContractStatusMap` 11 种状态。 |
| `contractType` | `contractType` | 前端语义 string / 后端 Integer | ✅ 已映射 | `ContractTypeMap` 8 种类型。 |
| `signDate` / `terminateReason` / `htmlContent` | 对应端点字段 | string | ✅ 已映射 | 签署/终止/正文保存已对接。 |
| `renew` / `archive` / `destroy` / `submitApproval` / `generateDocument` | — | — | ❌ 后端未实现 | `contract.ts` 中大量方法直接抛错。`[待修复]` |

### 1.7 招聘（Recruitment）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| 职位 `status` | `status` | 前端语义 string / 后端 Integer | ✅ 已映射 | `RecruitmentStatusMap`：`published/paused/closed`。 |
| 简历 `status` | `status` | 前端语义 string / 后端 Integer | ✅ 已映射 | `ResumeStatusMap`：6 种状态。 |
| 面试状态 | `HrInterviewStatus` 等 | string | ✅ 已映射 | 本地枚举，未明确后端映射。 |

### 1.8 健康证（Health Certificate）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `status` | `status` | 前端语义 string / 后端 Integer | ⚠️ 映射依赖 `HealthCertStatusMap` | `valid/expiring/expired/revoked`，后端 `1/2/3/4`。 |
| `approvalStatus` | `approvalStatus` | 前端语义 string / 后端 Integer | ⚠️ 映射依赖 `ApprovalStatusMap` | `draft/pending/approved/rejected`，后端 `1/2/3/4`。 |
| `expenseStatus` | — | string | ❌ 后端缺失 | 健康证报销状态字段后端 `HealthCertificate` 实体未定义。`[待修复]` |

### 1.9 入职办理（Onboarding）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `id` | `archiveId` / `id` | number / Long | ⚠️ 命名不一致 | 前端 `OnboardingArchive.id` 与后端 `archiveId` 混用。 |
| `candidateName` | `candidateName` | string | ✅ 已映射 | — |
| `position` / `positionLevel` | — | string | ❌ 后端缺失 | 后端 `OnboardingArchive` 未找到对应字段。`[待修复]` |
| `finalSalary` / `expectedSalary` | — | number | ❌ 后端缺失 | 后端实体未定义。`[待修复]` |
| `personnelType` | — | string | ❌ 后端缺失 | 后端实体未定义。`[待修复]` |
| `status` | `status` | string | ✅ 已映射 | 具体状态值未在类型中枚举。`[待修复]` |

### 1.10 培训发展（Training）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `CourseStatus` / `CoursePublishStatus` | — | string 枚举 | ❓ 后端映射未知 | 仅前端类型定义，未在 `converters.ts` 中注册映射。`[待修复]` |
| `StudyStatus` / `QuizPassStatus` | — | string 枚举 | ❓ 后端映射未知 | 同上。 |

### 1.11 知识库（Knowledge Base）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `ArticlePublishStatus` | — | string 枚举 | ❓ 后端映射未知 | 仅前端类型定义。`[待修复]` |

---

## 2. 资产管理（Asset）

### 2.1 资产主数据

| 前端字段（`types/asset.ts` / `api/asset/asset.ts`） | 后端字段（`entity/AssetMasterNew.java`） | 类型 | 对齐状态 | 说明 / 问题 |
|--------------------------------------------------|----------------------------------------|------|----------|-------------|
| `id` | `assetId` | 前端 string / 后端 Long | ✅ 已映射 | — |
| `assetCode` | `assetCode` | string | ✅ 已映射 | — |
| `assetName` | `assetName` | string | ✅ 已映射 | — |
| `categoryId` | `categoryId` | 前端 string / 后端 Long | ✅ 已映射 | — |
| `categoryName` | — | string | ❌ 后端缺失 | 后端实体无 `categoryName`，依赖 VO 冗余。`[待修复]` |
| `originalValue` | `originalCost` | 前端 number（分） / 后端 Long（分） | ✅ 单位一致 | 命名不一致，已转换。 |
| `currentValue` | `netBookValue` | 前端 number（分） / 后端 Long（分） | ✅ 已映射 | 命名不一致。 |
| `accumulatedDepreciation` | `accumulatedDepreciation` | number（分） / Long | ✅ 已映射 | — |
| `purchaseDate` | `purchaseDate` | string / LocalDate | ✅ 已映射 | — |
| `usefulLifeMonths` | `usefulLifeMonths` | number / Integer | ✅ 已映射 | — |
| `usedMonths` | `useCount` | number / Integer | ⚠️ 命名不一致 | 后端实际字段为 `useCount`，语义为“使用次数”而非“已使用月数”。`[待修复]` |
| `remainingMonths` | `remainingMonths` | number / Integer | ✅ 已映射 | 前端未声明该字段，后端有但 API 未返回。`[待修复]` |
| `depreciationMethod` | `depreciationMethod` | 前端枚举 string / 后端 Integer | ⚠️ 枚举值不一致 | 前端：`straight_line/double_declining/sum_of_years`；后端：`1=直线法/2=年数总和法/3=工作量法`。缺少 `double_declining` 对应后端编码。`[待修复]` |
| `salvageRate` | `residualRate` | 前端 number（百分比） / 后端 小数 | ⚠️ 单位不一致 | 前端 `5` 表示 5%，后端 `0.05`；API 未做转换。`[待修复]` |
| `departmentId` / `departmentName` | — | string | ❌ 后端缺失 | 后端 `AssetMasterNew` 无部门字段，前端强制置空。`[待修复]` |
| `storeId` | `storeId` | 前端 string / 后端 Long | ✅ 已映射 | — |
| `storeName` | — | string | ❌ 后端缺失 | 依赖 VO 冗余。`[待修复]` |
| `location` | `location` | string | ✅ 已映射 | — |
| `status` | `status` | 前端 7 种枚举 / 后端 5 种编码 | ❌ 状态不对齐 | 后端：`1=在用/2=闲置/3=维修中/4=已报废/5=已处置`；前端额外定义 `to_be_disposed/disposed/transferred`。`[待修复]` |
| `responsibleUserId` / `responsibleUserName` | `responsibleUserId` / — | string / Long | ⚠️ 负责人名缺失 | 后端无 `responsibleUserName` 字段。`[待修复]` |
| `supplierId` / `supplierName` | `supplierInfo`（JSON） | string | ⚠️ 结构不一致 | 前端拆分 `supplierId/supplierName`，后端存储 JSON 字符串。`[待修复]` |
| `specification` | `specification` / `brand` | string | ⚠️ 字段拆分 | 前端只有 `specification`，后端有 `specification` 和 `brand`。`[待修复]` |
| `serialNumber` | `qrCode` | string | ⚠️ 语义不一致 | 前端 `serialNumber` 映射后端 `qrCode`。`[待修复]` |
| `photoUrls[]` | `imageUrl` | 前端 string[] / 后端 string | ⚠️ 类型不一致 | 前端支持多图，后端仅单图。`[待修复]` |
| `createTime` / `updateTime` | `createTime` / `updateTime` | string / LocalDateTime | ✅ 已映射 | — |
| `warrantyExpiry` / `nextMaintenanceDate` | 后端存在 | string / LocalDate | ❌ 前端缺失 | 后端有保修到期日、下次保养日期，前端 `Asset` 接口未声明。`[待修复]` |

### 2.2 资产分类

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `id` | `categoryId` | string / Long | ✅ 已映射 | — |
| `code` | `categoryCode` | string | ✅ 已映射 | 命名不一致。 |
| `name` | `categoryName` | string | ✅ 已映射 | 命名不一致。 |
| `parentId` | `parentId` | string/null / Long | ✅ 已映射 | 前端 `null` 对应后端 `0`。 |
| `sortOrder` | `sortOrder` | number | ✅ 已映射 | — |
| `usefulLifeMonthsOverride` | `usefulLifeYears` | number（月） / number（年） | ⚠️ 单位不一致 | API 层做 `×12`/`÷12` 转换，存在整除截断。`[待修复]` |
| `salvageRateOverride` | `residualRate` | number（百分比） / 小数 | ⚠️ 单位不一致 | API 层做 `÷100`/`×100` 转换。 |
| `assetCount` / `totalValue` / `monthlyDepreciation` | — | number | ❌ 后端缺失 | 前端统计字段后端未提供。`[待修复]` |

### 2.3 盘点任务（Inventory Check）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `id` | `checkId` | string / Long | ✅ 已映射 | — |
| `checkCode` | `checkCode` | string | ✅ 已映射 | — |
| `title` | `remark` / `checkCode` | string | ⚠️ 语义不一致 | 前端 `title` 由后端 `remark` 或单号回填。`[待修复]` |
| `status` | `status` | 前端 4 种枚举 / 后端 4 种编码 | ⚠️ 映射错位 | 后端：`0=待盘/1=盘中/2=已审核/3=已完成`；前端 `CANCELLED` 映射到后端 `2（已审核）`，语义错误。`[待修复]` |
| `checkType` | `checkType` | 前端 string / 后端 Integer | ✅ 已映射 | `full/sample/cyclic` ↔ `1/2/3`。 |
| `plannedStartTime` / `plannedEndTime` | `checkDate` | string / LocalDate | ⚠️ 字段缺失 | 后端只有 `checkDate`，无起止时间。`[待修复]` |
| `actualStartTime` / `actualEndTime` | `checkDate` / `approveTime` | string / LocalDateTime | ⚠️ 语义不一致 | 用审核时间代替结束时间。`[待修复]` |
| `creatorId` / `creatorName` | `createUserId` | string / Long | ⚠️ 名称缺失 | 后端无 `creatorName`。`[待修复]` |
| `auditorName` | `approveUserId` | string / Long | ⚠️ 名称缺失 | 后端无 `auditorName`。`[待修复]` |
| `totalAssetCount` | `totalAssetsCount` | number | ✅ 已映射 | 命名不一致。 |
| `countedCount` | `actualCount` | number | ✅ 已映射 | 命名不一致。 |
| `overageCount` / `shortageCount` | `diffCount` | number | ⚠️ 语义不一致 | 后端只有差异数量，无盘盈/盘亏拆分。`[待修复]` |

### 2.4 维修记录（Maintenance）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `id` | `flowId` | string / Long | ✅ 已映射 | 后端为 `AssetFlowRecordNew`（`flow_type=4`）。 |
| `assetId` | `assetId` | string / Long | ✅ 已映射 | — |
| `faultDescription` / `resultDescription` | `remark` | string | ⚠️ 字段复用 | 后端只有 `remark`，前后端字段语义不同。`[待修复]` |
| `repairType` | — | string | ❌ 后端缺失 | 后端 `AssetFlowRecord` 无维修类型字段。`[待修复]` |
| `vendorId` / `vendorName` | — | string | ❌ 后端缺失 | 后端无此字段。`[待修复]` |
| `repairCost` | `changeAmount` | number（分） / Long | ⚠️ 语义不一致 | 后端 `changeAmount` 为变动金额，非维修费用。`[待修复]` |
| `startTime` | `flowDate` / `createTime` | string / LocalDateTime | ✅ 已映射 | — |
| `completedTime` | `flowDate`（有 `afterValue` 时） | string | ⚠️ 推断逻辑 | 根据 `afterValue != null` 推断完成。`[待修复]` |
| `status` | — | 前端 string | ❌ 后端缺失 | 后端无状态字段，前端靠推断。`[待修复]` |
| `repairmanId` / `repairmanName` | `operatorId` | string / Long | ⚠️ 名称缺失 | 后端无 `operatorName`。`[待修复]` |

### 2.5 处置记录（Disposal）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `id` / `disposalNo` | `id` / `disposalNo` | string / number | ⚠️ 命名不一致 | 后端返回 Map，字段名不固定。 |
| `disposalType` | `type` / `disposalType` | 前端枚举 string / 后端 string | ✅ 已映射 | — |
| `netValue` / `disposalAmount` / `gainLoss` | `netValue` / `disposalAmount` / `gainLoss` | number（分） | ✅ 已映射 | — |
| `status` | `status` | 前端枚举 string / 后端 string/number | ✅ 已映射 | `PENDING/APPROVED/COMPLETED/REJECTED`。 |
| `applicantName` / `approverName` | `applicantName` / `handler` / `approverName` | string | ⚠️ 字段混用 | 申请人可能来自 `handler`。`[待修复]` |
| `disposalDate` | `disposalDate` | string / LocalDate | ✅ 已映射 | — |

### 2.6 调拨记录（Transfer）

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| 全部 | — | — | ❌ 后端未实现 | `transferApi` 所有方法均抛错或返回空。`[待修复]` |

---

## 3. 设备管理（Device）

### 3.1 设备主数据

| 前端字段（`types/device.ts` / `api/device/index.ts`） | 后端字段（`entity/Device.java`） | 类型 | 对齐状态 | 说明 / 问题 |
|----------------------------------------------------|--------------------------------|------|----------|-------------|
| `deviceId` | `deviceId` | number / Long | ✅ 已映射 | — |
| `deviceCode` | `deviceCode`（`device_id_str`） | string | ✅ 已映射 | 列名与字段名不一致，后端已标注。 |
| `deviceName` | `deviceName`（`device_name`） | string | ✅ 已映射 | — |
| `deviceType` | `deviceType`（`device_type`） | 前端 string / 后端 Integer | ✅ 已映射 | `PRINTER/SCANNER/SCALE/LOCKER/OTHER` ↔ `1/2/3/4/5`。 |
| `deviceModel` | `deviceModel`（`device_model`） | string | ✅ 已映射 | — |
| `manufacturer` | `manufacturer`（`@TableField(exist=false)`） | string | ⚠️ 后端非持久化 | 数据库无此列，仅内存字段。`[待修复]` |
| `serialNo` | `serialNo`（`serial_number`） | string | ✅ 已映射 | 命名不一致。 |
| `connectionType` | `connectionType`（`connection_type`） | 前端 string / 后端 Integer | ✅ 已映射 | `USB/SERIAL/NETWORK/BLUETOOTH` ↔ `1/2/3/4`。 |
| `connectionParams` | `connectionParams`（`connection_config`） | string | ✅ 已映射 | 命名不一致。 |
| `location` | `location`（`@TableField(exist=false)`） | string | ⚠️ 后端非持久化 | 数据库无此列。`[待修复]` |
| `storeId` | `storeId`（`store_id`） | number / Long | ✅ 已映射 | — |
| `status` | `status` | 前端 string / 后端 String(varchar) | ⚠️ 存储类型不一致 | 后端数据库存 `varchar`，Java 以 Integer 表示并通过 setter 转 String；前端映射 `0=offline/1=online/2=fault/3=maintenance`。 |
| `lastHeartbeatTime` | `lastHeartbeatTime`（`@TableField(exist=false)`） | string | ⚠️ 后端非持久化 | 数据库无此列。`[待修复]` |
| `lastOnlineTime` | `lastOnlineTime`（`last_online_time`） | string / LocalDateTime | ✅ 已映射 | — |
| `remark` | `remark`（`@TableField(exist=false)`） | string | ⚠️ 后端非持久化 | 数据库无此列。`[待修复]` |
| `firmwareVersion` / `driverClass` / `configJson` | 后端非持久化字段 | string | ⚠️ 后端非持久化 | 均标记 `@TableField(exist=false)`。`[待修复]` |

### 3.2 设备告警

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `alertId` | `alertId` | number / Long | ✅ 已映射 | — |
| `deviceId` | `deviceId` | number / Long | ✅ 已映射 | — |
| `deviceName` | `deviceName`（冗余） | string | ✅ 已映射 | — |
| `deviceType` | `deviceType`（冗余） | 前端 string / 后端 Integer | ✅ 已映射 | — |
| `alertType` | `alertType` | 前端 string / 后端 Integer | ✅ 已映射 | `offline_timeout/paper_out/ribbon_out/fault/maintenance_reminder` ↔ `1/2/3/4/5`。 |
| `alertLevel` | `alertLevel` | 前端 string / 后端 Integer | ✅ 已映射 | `info/warning/critical/urgent` ↔ `1/2/3/4`。 |
| `alertStatus` | `alertStatus` | 前端 string / 后端 Integer | ✅ 已映射 | `pending/handling/resolved/ignored` ↔ `0/1/2/3`。 |
| `isHandled` | `isHandled` | boolean / Boolean | ✅ 已映射 | — |
| `triggerTime` | `triggerTime` | string / LocalDateTime | ✅ 已映射 | — |
| `handleTime` / `handleResult` / `handleUserId` | `handleTime` / `handleResult` / `handleUserId` | string/number | ✅ 已映射 | — |
| `resolveTime` | `resolveTime` | string / LocalDateTime | ✅ 已映射 | — |
| `deviceStatusSnapshot` | `deviceStatusSnapshot` | string | ✅ 已映射 | — |

### 3.3 状态历史

| 前端字段 | 后端字段 | 类型 | 对齐状态 | 说明 |
|---------|---------|------|----------|------|
| `deviceType` | `deviceType` | string | ⚠️ 类型不一致 | 后端 `DeviceStatusHistory` 用 String 存储设备类型，与 `Device` 的 Integer 不同。`[待修复]` |
| `online` | — | boolean | ❓ 后端来源不明 | 仅前端类型声明，未明确后端字段。 |
| `ipAddress` / `port` | — | string | ❓ 后端来源不明 | 同上。 |

---

## 4. 跨模块通用问题汇总

| 问题类别 | 影响模块 | 严重程度 | 说明 |
|---------|---------|----------|------|
| 金额单位不一致 | 薪资、资产 | P0 | 薪资后端使用 BigDecimal 元，违反项目规范；资产前后端均用分，但部分字段（`salvageRate`）单位不同。`[待修复]` |
| 状态枚举前后端不对齐 | 资产、盘点、设备 | P1 | 资产前端 7 状态 vs 后端 5 状态；盘点 `CANCELLED` 错误映射到“已审核”。`[待修复]` |
| 数据库字段缺失 | 设备、资产、员工 | P1 | 设备 `location/manufacturer/remark`、资产 `departmentId/categoryName` 等均为 `@TableField(exist=false)` 或前端缺失。`[待修复]` |
| 字段语义不一致 | 资产、维修、盘点 | P2 | 资产 `serialNumber` 映射 `qrCode`；维修状态靠 `afterValue` 推断；盘点标题用 `remark` 回填。`[待修复]` |
| 后端 API 未实现 | 资产调拨、合同、薪资 | P0 | `transferApi`、合同续签/归档/销毁、薪资发放/批量发放/财务同步等直接抛错。`[待修复]` |
| 类型未注册 Converter | 培训、知识库 | P2 | `CourseStatus`、`ArticlePublishStatus` 等仅有前端枚举，无前后端映射。`[待修复]` |
