# 人事 / 资产 / 设备模块操作动作对齐矩阵

> 范围：人事管理（组织、员工、岗位、考勤、招聘、薪资、培训、健康证、合同、入职、合同模板、知识库）、资产管理、设备管理。
> 仅做梳理，未修改源码。`[待修复]` 标注动作缺失、权限缺失、前后端未对齐等问题。

---

## 1. 人事管理（HR）

### 1.1 员工管理

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询列表 | `employeeApi.getList` | `GET /v1/employees` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 查看详情 | `employeeApi.getEmployeeById` | `GET /v1/employees/{id}` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 新增员工 | `employeeApi.createEmployee` | `POST /v1/employees` | `hr:employee:manage` | ✅ | — |
| 编辑员工 | `employeeApi.updateEmployee` | `PUT /v1/employees/{id}` | `hr:employee:manage` | ✅ | — |
| 删除员工 | `employeeApi.deleteEmployee` | `DELETE /v1/employees/{id}` | `hr:employee:manage` | ✅ | — |
| 批量调整部门 | `employeeApi.batchUpdateDepartment` | `PUT /v1/employees/batch/department` | `hr:employee:manage` | ✅ | — |
| 批量调整岗位 | `employeeApi.batchUpdatePosition` | `PUT /v1/employees/batch/position` | `hr:employee:manage` | ✅ | — |
| 调动（门店/部门） | `employeeApi.transferEmployee` | `PUT /v1/employees/{id}/transfer` | `hr:employee:manage` | ✅ | — |
| 办理离职 | `employeeApi.updateEmployee(status=inactive)` | `PUT /v1/employees/{id}` | `hr:employee:manage` | ⚠️ 无独立动作 | 离职无独立 API，直接更新状态。`[待修复]` |
| 复职 | — | — | `hr:employee:manage` | ❌ 缺失 | 员工状态无复职回退机制。`[待修复]` |
| 转正 | — | — | `hr:employee:manage` / `hr:probation:manage` | ❌ 缺失 | 试用期转正无独立 API。`[待修复]` |
| 查看统计 | `employeeApi.getStatistics` | `GET /v1/employees/statistics` | `hr:analytics:view` / `hr:view` | ✅ | — |

### 1.2 组织架构

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询树 | `departmentApi.getDepartmentTree` | `GET /v1/departments/tree` | `hr:organization:manage` / `hr:view` | ✅ | — |
| 查看详情 | `departmentApi.getDepartment` | `GET /v1/departments/{id}` | `hr:organization:manage` / `hr:view` | ✅ | — |
| 新增部门 | `departmentApi.addDepartment` | `POST /v1/departments` | `hr:organization:manage` | ✅ | — |
| 编辑部门 | `departmentApi.updateDepartment` | `PUT /v1/departments/{id}` | `hr:organization:manage` | ✅ | — |
| 删除部门 | `departmentApi.deleteDepartment` | `DELETE /v1/departments/{id}` | `hr:organization:manage` | ✅ | 组件中硬编码校验 `department:delete`，但该权限未在 `systemModules` 中注册。`[待修复]` |
| 启用/禁用 | `departmentApi.updateDepartmentStatus` | `PUT /v1/departments/{id}/status` | `hr:organization:manage` | ✅ | — |
| 调整排序 | `departmentApi.updateSortOrder` | `PUT /v1/departments/{id}/sort` | `hr:organization:manage` | ✅ | — |

### 1.3 岗位管理

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询列表 | `positionApi.getList` | `GET /v1/positions` | `hr:organization:manage` / `hr:view` | ✅ | — |
| 查看详情 | `positionApi.getById` | `GET /v1/positions/{id}` | `hr:organization:manage` / `hr:view` | ✅ | — |
| 按部门查询 | `positionApi.getByDepartment` | `GET /v1/positions/department/{id}` | `hr:organization:manage` / `hr:view` | ✅ | — |
| 新增岗位 | `positionApi.create` | `POST /v1/positions` | `hr:organization:manage` | ✅ | — |
| 编辑岗位 | `positionApi.update` | `PUT /v1/positions/{id}` | `hr:organization:manage` | ✅ | — |
| 删除岗位 | `positionApi.delete` | `DELETE /v1/positions/{id}` | `hr:organization:manage` | ✅ | — |
| 启用/禁用 | `positionApi.updateStatus` | `PUT /v1/positions/{id}/status` | `hr:organization:manage` | ✅ | — |
| 生成岗位编码 | `positionApi.generateCode` / `generateSeq` | `GET /v1/positions/generate-code` | `hr:organization:manage` | ✅ | — |

### 1.4 考勤排班

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询考勤记录 | `attendanceApi.getList` | `GET /v1/attendance` | `hr:attendance:manage` / `hr:view` | ✅ | — |
| 查询月度汇总 | `attendanceApi.getMonthlySummary` | `GET /v1/attendance/monthly-summary` | `hr:attendance:manage` / `hr:view` | ✅ | — |
| 确认月度汇总 | `attendanceApi.confirmMonthlySummary` | `POST /v1/attendance/confirm/{employeeId}` | `hr:attendance:manage` | ✅ | — |
| 查询排班 | `scheduleApi.getList` | `GET /v1/schedules` | `hr:attendance:manage` / `hr:view` | ✅ | — |
| 新增排班 | `scheduleApi.create` | `POST /v1/schedules` | `hr:attendance:manage` | ✅ | — |
| 编辑排班 | `scheduleApi.update` | `PUT /v1/schedules/{id}` | `hr:attendance:manage` | ✅ | — |
| 批量新增排班 | `scheduleApi.batchCreate` | `POST /v1/schedules/batch` | `hr:attendance:manage` | ✅ | — |
| 查询门店提交 | `scheduleApi.getStoreSubmissions` | `GET /v1/schedules/store-submissions` | `hr:attendance:manage` | ✅ | — |
| 同步到考勤 | `scheduleApi.syncToAttendance` | `POST /v1/schedules/{planId}/sync` | `hr:attendance:manage` | ✅ | — |
| 查看考勤统计 | `attendanceApi.getStatistics` | `GET /v1/attendance/statistics` | `hr:analytics:view` / `hr:view` | ✅ | — |

### 1.5 薪资管理

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询薪资记录 | `salaryApi.getList` | `GET /v1/salaries` | `hr:salary:manage` / `hr:view` | ✅ | — |
| 查看详情 | `salaryApi.getById` | `GET /v1/salaries/{id}` | `hr:salary:manage` / `hr:view` | ✅ | — |
| 查看统计 | `salaryApi.getStatistics` | `GET /v1/salaries/statistics` | `hr:salary:manage` / `hr:analytics:view` | ✅ | — |
| 新增薪资 | `salaryApi.create` | `POST /v1/salaries` | `hr:salary:manage` | ✅ | — |
| 编辑薪资 | `salaryApi.update` | `PUT /v1/salaries/{id}` | `hr:salary:manage` | ✅ | — |
| 删除薪资 | `salaryApi.delete` | `DELETE /v1/salaries/{id}` | `hr:salary:manage` | ✅ | — |
| 确认/驳回 | `salaryApi.approve` | `POST /v1/salaries/confirm/{id}` | `hr:salary:manage` | ✅ | 驳回分支抛错，无 reject 接口。`[待修复]` |
| 发放薪资 | `salaryApi.pay` | — | `hr:salary:manage` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 批量发放 | `salaryApi.batchPay` | — | `hr:salary:manage` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 财务同步 | `salaryApi.getFinanceSyncData` / `markFinanceSynced` | — | `hr:salary:manage` / `finance:approval:manage` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 批次查询 | `salaryBatchApi.getList` | `GET /v1/salary-batches` | `hr:salary:manage` | ✅ | — |
| 批次创建 | `salaryBatchApi.create` | `POST /v1/salary-batches` | `hr:salary:manage` | ✅ | — |
| 批次审批 | `salaryBatchApi.approve` | `POST /v1/salary-batches/{id}/approve` | `hr:salary:manage` | ✅ | — |
| 批次发放 | `salaryBatchApi.pay` | `POST /v1/salary-batches/{id}/pay` | `hr:salary:manage` | ✅ | — |
| 批次取消 | `salaryBatchApi.cancel` | `PUT /v1/salary-batches/{id}/cancel` | `hr:salary:manage` | ✅ | — |
| 同步数据源 | `salaryBatchApi.syncData` | `POST /v1/salary-batches/{id}/sync` | `hr:salary:manage` | ✅ | — |

### 1.6 招聘管理

#### 招聘职位

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询职位 | `positionApi.getList` | `GET /v1/recruitment/positions` | `hr:recruitment:manage` / `hr:view` | ✅ | — |
| 查看详情 | `positionApi.getById` | `GET /v1/recruitment/positions/{id}` | `hr:recruitment:manage` / `hr:view` | ✅ | — |
| 新增职位 | `positionApi.create` | `POST /v1/recruitment/positions` | `hr:recruitment:manage` | ✅ | — |
| 编辑职位 | `positionApi.update` | `PUT /v1/recruitment/positions/{id}` | `hr:recruitment:manage` | ✅ | — |
| 发布职位 | `positionApi.publish` | `PUT /v1/recruitment/positions/{id}/publish` | `hr:recruitment:manage` | ✅ | — |
| 暂停职位 | `positionApi.close` | `PUT /v1/recruitment/positions/{id}/close` | `hr:recruitment:manage` | ✅ | — |
| 取消/删除职位 | `positionApi.cancel` / `delete` | `PUT /v1/recruitment/positions/{id}/cancel` / `DELETE` | `hr:recruitment:manage` | ✅ | — |

#### 简历与面试

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询简历 | `resumeApi.getList` | `GET /v1/recruitment/resumes` | `hr:recruitment:manage` / `hr:view` | ✅ | — |
| 新增简历 | `resumeApi.create` | `POST /v1/recruitment/resumes` | `hr:recruitment:manage` | ✅ | — |
| 更新简历状态 | `resumeApi.updateStatus` | `PUT /v1/recruitment/resumes/{id}/status` | `hr:recruitment:manage` | ✅ | — |
| 淘汰简历 | `resumeApi.reject` | `PUT /v1/recruitment/resumes/{id}/reject` | `hr:recruitment:manage` | ✅ | — |
| 查询面试 | `interviewApi.getList` | `GET /v1/recruitment/interviews` | `hr:recruitment:manage` / `hr:view` | ✅ | — |
| 新增面试 | `interviewApi.create` | `POST /v1/recruitment/interviews` | `hr:recruitment:manage` | ✅ | — |
| 录入面试结果 | `interviewApi.updateResult` | `PUT /v1/recruitment/interviews/{id}/result` | `hr:recruitment:manage` | ✅ | — |
| 创建 HR 面试 | `interviewApi.createHrInterview` | `POST /v1/recruitment/interviews/hr` | `hr:recruitment:manage` | ✅ | — |
| 提交 HR 评价 | `interviewApi.submitHrEvaluation` | `PUT /v1/recruitment/interviews/{id}/hr-evaluation` | `hr:recruitment:manage` | ✅ | — |
| 查询录用记录 | `hireApi.getList` | `GET /v1/recruitment/hires` | `hr:recruitment:manage` / `hr:view` | ✅ | — |
| 创建录用记录 | `hireApi.create` | `POST /v1/recruitment/hires` | `hr:recruitment:manage` | ✅ | — |
| 更新录用状态 | `hireApi.updateStatus` | `PUT /v1/recruitment/hires/{id}/status` | `hr:recruitment:manage` | ✅ | — |
| 录用转入职 | — | — | `hr:recruitment:manage` / `hr:employee:manage` | ❌ 缺失 | 招聘录用后无自动发起入职流程动作。`[待修复]` |

### 1.7 合同管理

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询合同 | `contractApi.getList` | `GET /v1/contracts` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 查看详情 | `contractApi.getById` | `GET /v1/contracts/{id}` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 查询待签署 | `contractApi.getPendingContracts` | `GET /v1/contracts/pending` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 查询即将到期 | `contractApi.getExpiringContracts` | `GET /v1/contracts/expiring` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 新增合同 | `contractApi.create` | `POST /v1/contracts` | `hr:contract:manage` | ✅ | — |
| 编辑合同 | `contractApi.update` | `PUT /v1/contracts/{id}` | `hr:contract:manage` | ✅ | — |
| 删除合同 | `contractApi.delete` | `DELETE /v1/contracts/{id}` | `hr:contract:manage` | ✅ | — |
| 签署 | `contractApi.sign` / `signWithSignature` | `POST /v1/contracts/{id}/sign` | `hr:contract:manage` | ✅ | — |
| 续签 | `contractApi.renew` | — | `hr:contract:manage` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 终止 | `contractApi.terminate` | `POST /v1/contracts/{id}/terminate` | `hr:contract:manage` | ✅ | — |
| 延期 | `contractApi.extend` | `POST /v1/contracts/{id}/extend` | `hr:contract:manage` | ✅ | — |
| 提交审批 | `contractApi.submitApproval` | — | `hr:contract:manage` / `hr:approval` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 归档 | `contractApi.archive` | — | `hr:contract:manage` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 销毁 | `contractApi.destroy` | — | `hr:contract:manage` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 生成合同文档 | `contractApi.generateDocument` / `autoGenerate` | — | `hr:contract:manage` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 获取合同条款 | `contractApi.getClauses` | — | `hr:contract:manage` / `hr:view` | ❌ 后端未实现 | 方法直接抛错。`[待修复]` |
| 查看变更记录 | `contractApi.getChangeRecords` | `GET /v1/contracts/{id}/changes` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 创建变更 | `contractApi.createChange` | `POST /v1/contracts/{id}/changes` | `hr:contract:manage` | ✅ | — |
| 提交变更审批 | `contractApi.submitChangeApproval` | `PUT /v1/contracts/changes/{id}/submit` | `hr:contract:manage` / `hr:approval` | ✅ | — |
| 激活变更 | `contractApi.activateChange` | `PUT /v1/contracts/changes/{id}/activate` | `hr:contract:manage` | ✅ | — |
| 下载 PDF | `contractApi.downloadPdf` | `GET /v1/contracts/{id}/pdf` | `hr:contract:manage` / `hr:view` | ✅ | — |

### 1.8 合同模板管理

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询模板 | `contractTemplateApi.getList` | `GET /v1/contract-templates` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 查看详情 | `contractTemplateApi.getById` | `GET /v1/contract-templates/{id}` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 查询生效模板 | `contractTemplateApi.getActiveTemplates` | `GET /v1/contract-templates/active` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 新增模板 | `contractTemplateApi.create` | `POST /v1/contract-templates` | `hr:contract:manage` | ✅ | — |
| 编辑模板 | `contractTemplateApi.update` | `PUT /v1/contract-templates/{id}` | `hr:contract:manage` | ✅ | — |
| 删除模板 | `contractTemplateApi.delete` | `DELETE /v1/contract-templates/{id}` | `hr:contract:manage` | ✅ | — |
| 预览模板 | `contractTemplateApi.preview` | `POST /v1/contract-templates/preview` | `hr:contract:manage` / `hr:view` | ✅ | — |
| 复制模板 | `contractTemplateApi.duplicate` | `POST /v1/contract-templates/{id}/duplicate` | `hr:contract:manage` | ✅ | — |
| 启用/禁用 | `contractTemplateApi.updateStatus` | `PUT /v1/contract-templates/{id}/status` | `hr:contract:manage` | ✅ | — |
| 查看统计 | `contractTemplateApi.getStatistics` | `GET /v1/contract-templates/statistics` | `hr:contract:manage` / `hr:analytics:view` | ✅ | — |

### 1.9 健康证管理

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询健康证 | `healthCertificateApi.getList` | `GET /v1/health-certificates` | `hr:health:manage` / `hr:view` | ✅ | — |
| 查看详情 | `healthCertificateApi.getById` | `GET /v1/health-certificates/{id}` | `hr:health:manage` / `hr:view` | ✅ | — |
| 按员工查询 | `healthCertificateApi.getByEmployeeId` | `GET /v1/health-certificates/employee/{id}` | `hr:health:manage` / `hr:view` | ✅ | — |
| 新增健康证 | `healthCertificateApi.create` | `POST /v1/health-certificates` | `hr:health:manage` | ✅ | — |
| 编辑健康证 | `healthCertificateApi.update` | `PUT /v1/health-certificates/{id}` | `hr:health:manage` | ✅ | — |
| 删除健康证 | `healthCertificateApi.delete` / `batchDelete` | `DELETE /v1/health-certificates/{id}` | `hr:health:manage` | ✅ | — |
| 更新状态 | `healthCertificateApi.updateStatus` / `batchUpdateStatus` | `PUT /v1/health-certificates/{id}/status` | `hr:health:manage` | ✅ | — |
| 自动计算状态 | `healthCertificateApi.calculateAndUpdateStatus` | `POST /v1/health-certificates/calculate-status` | `hr:health:manage` | ✅ | — |
| 提交审核 | `healthCertificateApi.submitForApproval` | `POST /v1/health-certificates/{id}/submit` | `hr:health:manage` | ✅ | — |
| 审核通过 | `healthCertificateApi.approve` | `PUT /v1/health-certificates/{id}/approve` | `hr:health:manage` / `hr:approval` | ⚠️ 后端未实现 | 方法直接抛错。`[待修复]` |
| 审核驳回 | `healthCertificateApi.reject` | `PUT /v1/health-certificates/{id}/reject` | `hr:health:manage` / `hr:approval` | ⚠️ 后端未实现 | 方法直接抛错。`[待修复]` |
| 报销申请 | `healthCertificateApi.submitExpense` | `POST /v1/health-certificates/expenses` | `hr:health:manage` / `finance:expense:manage` | ✅ | — |
| 查询报销 | `healthCertificateApi.getExpenseList` | `GET /v1/health-certificates/expenses` | `hr:health:manage` / `hr:view` | ✅ | — |
| 发送到期提醒 | `healthCertificateApi.sendExpiryReminders` | `POST /v1/health-certificates/reminders` | `hr:health:manage` | ✅ | — |
| 查看统计 | `healthCertificateApi.getStatistics` | `GET /v1/health-certificates/statistics` | `hr:health:manage` / `hr:analytics:view` | ✅ | — |

### 1.10 入职办理

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询档案 | `onboardingApi.getList` | `GET /v1/onboarding/archives` | `hr:employee:manage` / `hr:view` | ✅ | 权限未单独定义 `hr:onboarding:manage`。`[待修复]` |
| 查看详情 | `onboardingApi.getById` | `GET /v1/onboarding/archives/{id}` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 新增档案 | `onboardingApi.create` | `POST /v1/onboarding/archives` | `hr:employee:manage` | ✅ | — |
| 编辑档案 | `onboardingApi.update` | `PUT /v1/onboarding/archives/{id}` | `hr:employee:manage` | ✅ | — |
| 删除档案 | `onboardingApi.delete` | `DELETE /v1/onboarding/archives/{id}` | `hr:employee:manage` | ✅ | — |
| 提交审批 | `onboardingApi.submit` | `POST /v1/onboarding/archives/{id}/submit` | `hr:employee:manage` / `hr:approval` | ✅ | — |
| 查询岗位层级 | `onboardingApi.getPositionLevels` | `GET /v1/onboarding/position-levels` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 查询状态列表 | `onboardingApi.getStatuses` | `GET /v1/onboarding/statuses` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 审批入职 | — | — | `hr:employee:manage` / `hr:approval` | ❌ 缺失 | 仅 `submit`，无独立 approve/reject 动作。`[待修复]` |
| 入职转员工 | — | — | `hr:employee:manage` | ❌ 缺失 | 入职审批通过后无自动创建员工账号动作。`[待修复]` |

### 1.11 培训发展

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询课程 | `trainingApi.getList` | `GET /v1/training/courses` | `hr:employee:manage` / `hr:view` | ✅ | 权限未单独定义 `hr:training:manage`。`[待修复]` |
| 查看详情 | `trainingApi.getById` | `GET /v1/training/courses/{id}` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 新增课程 | `trainingApi.create` | `POST /v1/training/courses` | `hr:employee:manage` | ✅ | — |
| 编辑课程 | `trainingApi.update` | `PUT /v1/training/courses/{id}` | `hr:employee:manage` | ✅ | — |
| 删除课程 | `trainingApi.delete` | `DELETE /v1/training/courses/{id}` | `hr:employee:manage` | ✅ | — |
| 发布课程 | `trainingApi.publish` | `PUT /v1/training/courses/{id}/publish` | `hr:employee:manage` | ✅ | — |
| 归档课程 | `trainingApi.archive` | `PUT /v1/training/courses/{id}/archive` | `hr:employee:manage` | ✅ | — |
| 查询学习记录 | `trainingApi.getStudyRecords` | `GET /v1/training/study-records` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 查询证书 | `trainingApi.getCertificates` | `GET /v1/training/certificates` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 查看统计 | `trainingApi.getStatistics` | `GET /v1/training/statistics` | `hr:employee:manage` / `hr:analytics:view` | ✅ | — |

### 1.12 知识库

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询文章 | `knowledgeBaseApi.getList` | `GET /v1/knowledge/articles` | `hr:employee:manage` / `hr:view` | ✅ | 权限未单独定义 `hr:knowledge:manage`。`[待修复]` |
| 查看详情 | `knowledgeBaseApi.getById` | `GET /v1/knowledge/articles/{id}` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 新增文章 | `knowledgeBaseApi.create` | `POST /v1/knowledge/articles` | `hr:employee:manage` | ✅ | — |
| 编辑文章 | `knowledgeBaseApi.update` | `PUT /v1/knowledge/articles/{id}` | `hr:employee:manage` | ✅ | — |
| 删除文章 | `knowledgeBaseApi.delete` | `DELETE /v1/knowledge/articles/{id}` | `hr:employee:manage` | ✅ | — |
| 发布 | `knowledgeBaseApi.publish` | `PUT /v1/knowledge/articles/{id}/publish` | `hr:employee:manage` | ✅ | — |
| 归档 | `knowledgeBaseApi.archive` | `PUT /v1/knowledge/articles/{id}/archive` | `hr:employee:manage` | ✅ | — |
| 提交审核 | `knowledgeBaseApi.submitReview` | `POST /v1/knowledge/articles/{id}/review` | `hr:employee:manage` / `hr:approval` | ✅ | — |
| 部门审核 | `knowledgeBaseApi.deptReview` | `PUT /v1/knowledge/articles/{id}/dept-review` | `hr:employee:manage` / `hr:approval` | ✅ | — |
| 终审 | `knowledgeBaseApi.finalReview` | `PUT /v1/knowledge/articles/{id}/final-review` | `hr:employee:manage` / `hr:approval` | ✅ | — |
| 查询审核记录 | `knowledgeBaseApi.getReviewRecords` | `GET /v1/knowledge/articles/{id}/review-records` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 查询学习记录 | `knowledgeBaseApi.getStudyRecords` | `GET /v1/knowledge/study-records` | `hr:employee:manage` / `hr:view` | ✅ | — |
| 重置学习记录 | `knowledgeBaseApi.resetStudyRecord` | `PUT /v1/knowledge/study-records/{id}/reset` | `hr:employee:manage` | ✅ | — |
| 查询分类统计 | `knowledgeBaseApi.getCategoryStats` | `GET /v1/knowledge/category-stats` | `hr:employee:manage` / `hr:analytics:view` | ✅ | — |

---

## 2. 资产管理（Asset）

### 2.1 资产主数据

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询资产 | `assetApi.getList` | `GET /v1/asset/list` | `asset:manage` / `asset:view` | ✅ | — |
| 查看详情 | `assetApi.getById` | `GET /v1/asset/{id}` | `asset:manage` / `asset:view` | ✅ | — |
| 新增资产 | `assetApi.create` | `POST /v1/asset` | `asset:manage` / `asset:ledger:view` | ✅ | 权限推断应为 `asset:manage`。 |
| 编辑资产 | `assetApi.update` | `PUT /v1/asset/{id}` | `asset:manage` | ✅ | — |
| 删除资产 | `assetApi.delete` | `DELETE /v1/asset/{id}` | `asset:manage` | ✅ | — |
| 查看概览 | `assetApi.getStatistics` / `getStatisticsVO` | `GET /v1/asset/overview` | `asset:view` / `asset:report:view` | ✅ | 概览接口金额字段返回 0。`[待修复]` |
| 开始维修 | — | `POST /v1/asset/{id}/repair` | `asset:maintenance:manage` | ❌ 前端缺失 | 后端接口在注释中声明，但 `assetApi` 未暴露方法。`[待修复]` |
| 完成维修 | — | `POST /v1/asset/{id}/complete-repair` | `asset:maintenance:manage` | ❌ 前端缺失 | 同上。`[待修复]` |
| 报废 | — | `POST /v1/asset/{id}/scrap` | `asset:disposal:manage` | ❌ 前端缺失 | 同上。`[待修复]` |

### 2.2 资产分类

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询分类 | `categoryApi.getList` | `GET /v1/asset/categories/page` | `asset:category:manage` / `asset:view` | ✅ | — |
| 查询树 | `categoryApi.getTree` | `GET /v1/asset/categories/tree` | `asset:category:manage` / `asset:view` | ✅ | — |
| 查看详情 | `categoryApi.getById` | `GET /v1/asset/categories/{id}` | `asset:category:manage` / `asset:view` | ✅ | — |
| 新增分类 | `categoryApi.create` | `POST /v1/asset/categories` | `asset:category:manage` | ✅ | — |
| 编辑分类 | `categoryApi.update` | `PUT /v1/asset/categories/{id}` | `asset:category:manage` | ✅ | — |
| 删除分类 | `categoryApi.delete` | `DELETE /v1/asset/categories/{id}` | `asset:category:manage` | ✅ | — |

### 2.3 资产折旧

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询折旧 | `depreciationApi.getList` | `GET /v1/asset/depreciation` | `asset:depreciation:manage` / `asset:view` | ✅ | — |
| 查看详情 | `depreciationApi.getById` | `GET /v1/asset/depreciation/{id}` | `asset:depreciation:manage` / `asset:view` | ✅ | — |
| 新增折旧 | `depreciationApi.create` | `POST /v1/asset/depreciation` | `asset:depreciation:manage` | ✅ | — |
| 编辑折旧 | `depreciationApi.update` | `PUT /v1/asset/depreciation/{id}` | `asset:depreciation:manage` | ✅ | — |
| 删除折旧 | `depreciationApi.delete` | `DELETE /v1/asset/depreciation/{id}` | `asset:depreciation:manage` | ✅ | — |
| 计算折旧 | `depreciationApi.calculate` | `POST /v1/asset/depreciation/calculate` | `asset:depreciation:manage` | ✅ | — |
| 批量计算 | `depreciationApi.batchCalculate` | `POST /v1/asset/depreciation/batch-calculate` | `asset:depreciation:manage` | ✅ | — |
| 调整折旧 | `depreciationApi.adjust` | `POST /v1/asset/depreciation/adjust` | `asset:depreciation:manage` | ⚠️ 后端未实现 | 方法直接抛错。`[待修复]` |

### 2.4 资产盘点

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询盘点 | `inventoryApi.getList` | `GET /v1/asset/inventory` | `asset:inventory:manage` / `asset:view` | ✅ | — |
| 查看详情 | `inventoryApi.getById` | `GET /v1/asset/inventory/{id}` | `asset:inventory:manage` / `asset:view` | ✅ | — |
| 新增盘点 | `inventoryApi.create` | `POST /v1/asset/inventory` | `asset:inventory:manage` | ✅ | — |
| 编辑盘点 | `inventoryApi.update` | `PUT /v1/asset/inventory/{id}` | `asset:inventory:manage` | ✅ | — |
| 删除盘点 | `inventoryApi.delete` | `DELETE /v1/asset/inventory/{id}` | `asset:inventory:manage` | ⚠️ 前端抛错 | 方法直接抛错。`[待修复]` |
| 开始盘点 | `inventoryApi.start` | `POST /v1/asset/inventory/{id}/start` | `asset:inventory:manage` | ✅ | — |
| 完成盘点 | `inventoryApi.complete` | `POST /v1/asset/inventory/{id}/complete` | `asset:inventory:manage` | ✅ | — |
| 取消盘点 | `inventoryApi.cancel` | `POST /v1/asset/inventory/{id}/cancel` | `asset:inventory:manage` | ✅ | — |
| 查询盘点明细 | `inventoryApi.getItems` | `GET /v1/asset/inventory/{id}/items` | `asset:inventory:manage` / `asset:view` | ✅ | — |
| 录入盘点结果 | `inventoryApi.updateItem` | `POST /v1/asset/inventory/items/{itemId}` | `asset:inventory:manage` | ✅ | — |

### 2.5 资产维护

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询维修 | `maintenanceApi.getList` | `GET /v1/asset/maintenance` | `asset:maintenance:manage` / `asset:view` | ✅ | — |
| 查看详情 | `maintenanceApi.getById` | `GET /v1/asset/maintenance/{id}` | `asset:maintenance:manage` / `asset:view` | ✅ | — |
| 新增维修 | `maintenanceApi.create` | `POST /v1/asset/maintenance` | `asset:maintenance:manage` | ✅ | — |
| 编辑维修 | `maintenanceApi.update` | `PUT /v1/asset/maintenance/{id}` | `asset:maintenance:manage` | ⚠️ 后端未实现 | 方法直接抛错。`[待修复]` |
| 删除维修 | `maintenanceApi.delete` | `DELETE /v1/asset/maintenance/{id}` | `asset:maintenance:manage` | ⚠️ 后端未实现 | 方法直接抛错。`[待修复]` |
| 开始维修 | `maintenanceApi.start` | `POST /v1/asset/maintenance/{id}/start` | `asset:maintenance:manage` | ✅ | — |
| 完成维修 | `maintenanceApi.complete` | `POST /v1/asset/maintenance/{id}/complete` | `asset:maintenance:manage` | ✅ | — |

### 2.6 资产处置

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询处置 | `disposalApi.getList` | `GET /v1/asset/disposal` | `asset:disposal:manage` / `asset:view` | ✅ | — |
| 查看详情 | `disposalApi.getById` | `GET /v1/asset/disposal/{id}` | `asset:disposal:manage` / `asset:view` | ✅ | — |
| 新增处置 | `disposalApi.create` | `POST /v1/asset/disposal` | `asset:disposal:manage` | ✅ | — |
| 编辑处置 | `disposalApi.update` | `PUT /v1/asset/disposal/{id}` | `asset:disposal:manage` | ⚠️ 后端未实现 | 方法直接抛错。`[待修复]` |
| 删除处置 | `disposalApi.delete` | `DELETE /v1/asset/disposal/{id}` | `asset:disposal:manage` | ⚠️ 后端未实现 | 方法直接抛错。`[待修复]` |
| 审批通过 | `disposalApi.approve` | `POST /v1/asset/disposal/{id}/approve?approved=true` | `asset:disposal:manage` / `finance:approval:manage` | ✅ | — |
| 审批驳回 | `disposalApi.reject` | `POST /v1/asset/disposal/{id}/approve?approved=false` | `asset:disposal:manage` / `finance:approval:manage` | ✅ | — |
| 执行处置 | `disposalApi.execute` | — | `asset:disposal:manage` | ❌ 后端未实现 | 方法直接抛错；处置通过后未回写资产主表状态。`[待修复]` |

### 2.7 资产调拨

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询调拨 | `transferApi.getList` | — | `asset:transfer:manage` / `asset:view` | ❌ 后端未实现 | 返回空列表。`[待修复]` |
| 查看详情 | `transferApi.getById` | — | `asset:transfer:manage` / `asset:view` | ❌ 后端未实现 | 抛错。`[待修复]` |
| 新增调拨 | `transferApi.create` | — | `asset:transfer:manage` | ❌ 后端未实现 | 抛错。`[待修复]` |
| 编辑调拨 | `transferApi.update` | — | `asset:transfer:manage` | ❌ 后端未实现 | 抛错。`[待修复]` |
| 删除调拨 | `transferApi.delete` | — | `asset:transfer:manage` | ❌ 后端未实现 | 抛错。`[待修复]` |
| 审批通过 | `transferApi.approve` | — | `asset:transfer:manage` / `finance:approval:manage` | ❌ 后端未实现 | 抛错。`[待修复]` |
| 审批驳回 | `transferApi.reject` | — | `asset:transfer:manage` / `finance:approval:manage` | ❌ 后端未实现 | 抛错。`[待修复]` |
| 完成调拨 | `transferApi.complete` | — | `asset:transfer:manage` | ❌ 后端未实现 | 抛错。`[待修复]` |

---

## 3. 设备管理（Device）

### 3.1 设备主数据

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询设备 | `deviceApi.getList` | `GET /v1/devices` | `device:manage` / `device:view` | ✅ | 无 `device:*` 权限在 `systemModules` 中注册。`[待修复]` |
| 查看详情 | `deviceApi.getById` | `GET /v1/devices/{id}` | `device:manage` / `device:view` | ✅ | — |
| 查询在线设备 | `deviceApi.getOnline` | `GET /v1/devices/online` | `device:view` | ✅ | — |
| 查询全部设备 | `deviceApi.getAll` | `GET /v1/devices/all` | `device:view` | ✅ | — |
| 新增设备 | `deviceApi.create` | `POST /v1/devices` | `device:manage` | ✅ | — |
| 编辑设备 | `deviceApi.update` | `PUT /v1/devices/{id}` | `device:manage` | ✅ | — |
| 删除设备 | `deviceApi.delete` | `DELETE /v1/devices/{id}` | `device:manage` | ✅ | — |
| 更新状态 | `deviceApi.updateStatus` | `PUT /v1/devices/{id}/status` | `device:manage` | ✅ | — |
| 启用/停用 | `deviceApi.toggle` | `PUT /v1/devices/{id}/toggle` | `device:manage` | ✅ | — |
| 测试连接 | `deviceApi.testConnection` | `POST /v1/devices/{id}/test` | `device:manage` / `device:view` | ✅ | — |

### 3.2 设备告警

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 查询告警 | `deviceAlertApi.getList` | `GET /v1/device-alerts` | `device:manage` / `device:view` | ✅ | — |
| 查看详情 | `deviceAlertApi.getById` | `GET /v1/device-alerts/{id}` | `device:manage` / `device:view` | ✅ | — |
| 查询未处理告警 | `deviceAlertApi.getUnhandledByDevice` / `getAllUnhandled` | `GET /v1/device-alerts/unhandled` | `device:manage` / `device:view` | ✅ | — |
| 处理告警 | `deviceAlertApi.handle` | `PUT /v1/device-alerts/{id}/handle` | `device:manage` | ✅ | — |
| 批量处理 | `deviceAlertApi.batchHandle` | `PUT /v1/device-alerts/batch-handle` | `device:manage` | ✅ | — |
| 开始处理 | — | — | `device:manage` | ❌ 缺失 | 无独立 `start` 动作，直接到 `resolved`。`[待修复]` |
| 忽略告警 | — | — | `device:manage` | ❌ 缺失 | 无 ignore 接口。`[待修复]` |
| 查看统计 | `deviceAlertApi.getStatistics` | `GET /v1/device-alerts/statistics` | `device:manage` / `device:view` | ✅ | — |

### 3.3 状态历史

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 近 7 天历史 | `deviceHistoryApi.getLast7Days` | `GET /v1/device-status-history/last7days` | `device:view` | ✅ | — |
| 近 24 小时历史 | `deviceHistoryApi.getLast24Hours` | `GET /v1/device-status-history/last24hours` | `device:view` | ✅ | — |
| 按类型查询 | `deviceHistoryApi.getByDeviceType` | `GET /v1/device-status-history` | `device:view` | ✅ | — |
| 按设备查询 | `deviceHistoryApi.getByDeviceId` | `GET /v1/device-status-history/device/{id}` | `device:view` | ✅ | — |
| 统计状态变化 | `deviceHistoryApi.countStatusChanges` | `GET /v1/device-status-history/count` | `device:view` | ✅ | — |

### 3.4 设备监控

| 动作 | 前端 API 方法 | 后端接口 | 所需权限（推断/实际） | 实现状态 | 说明 |
|------|--------------|---------|---------------------|----------|------|
| 检查状态 | `deviceMonitorApi.checkStatus` | `GET /v1/device-monitor/status` | `device:view` | ✅ | — |
| 查看统计 | `deviceMonitorApi.getStatistics` | `GET /v1/device-monitor/statistics` | `device:view` | ✅ | — |
| 最近变化 | `deviceMonitorApi.getRecentStatusChanges` | `GET /v1/device-monitor/recent-changes` | `device:view` | ✅ | — |
| 模拟上线 | `deviceMonitorApi.simulateOnline` | `POST /v1/device-monitor/{id}/online` | `device:manage` | ✅ | — |
| 模拟离线 | `deviceMonitorApi.simulateOffline` | `POST /v1/device-monitor/{id}/offline` | `device:manage` | ✅ | — |

---

## 4. 跨模块动作问题汇总

| 问题类别 | 影响模块 | 严重程度 | 说明 |
|---------|---------|----------|------|
| 后端 API 完全缺失 | 资产调拨、合同续签/归档/销毁/审批、薪资发放/批量发放/财务同步 | P0 | 前端 API 方法直接抛错，功能无法使用。`[待修复]` |
| 前端 API 未暴露后端已有接口 | 资产主数据的维修/完成维修/报废 | P1 | 后端注释声明接口存在，但 `assetApi` 对象未封装。`[待修复]` |
| 业务动作缺失 | 员工复职/转正、招聘录用转入职、入职审批/转员工、设备告警开始处理/忽略 | P1 | 状态机关键流转动作未定义。`[待修复]` |
| 状态与动作未联动 | 资产处置通过后未回写主表状态、合同到期/即将到期无自动流转 | P1 | 业务闭环依赖人工二次操作或定时任务缺失。`[待修复]` |
| 权限码缺失 | 设备管理整体、入职办理、培训发展、知识库、合同智能管理、人事分析等 | P1 | `systemModules` 未注册对应权限，页面/动作无统一权限控制。`[待修复]` |
| 组件硬编码未注册权限 | 组织架构删除按钮 | P2 | `OrgTreeNode.vue` 使用 `department:delete`，但全局未注册。`[待修复]` |
