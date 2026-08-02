# 食品溯源系统 — 数据生命周期流转逻辑（V3 全模块完整版）

> **文档定位**：描述系统全部 15 个一级模块的核心业务数据从产生、流转、变更到归档/删除的完整生命周期，为后端开发提供数据流转细节参考。
> **V3 修订要点**：
> - 在 V2 基础上**新增 8 个模块**的完整数据流转（工作台/产品/订单/运营/会员/设备/签章/系统）
> - 覆盖全部 15 个一级模块（依据 `src/modules/index.ts` 注册顺序）
> - 跨模块联动矩阵扩展至 30+ 项
> - 补充各模块状态机定义、死代码识别、双系统问题
> **生成时间**：2026-06-25（V3）
> **配套文档**：PROJECT_CONTEXT.md V2、DEVELOPMENT_STATUS.md V2

---

## 0. 全局数据流概览（V3 修订版）

### 0.1 15 模块数据流转总图

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    餐饮企业全业务数据流转图（V3 全模块）                    │
│                                                                          │
│  ┌─[工作台] ← 聚合 ← 全模块数据（当前:100% Mock，后端已建未调用）        │
│  │                                                                        │
│  │  ┌─[产品中心] ─── 分类 → 菜品 → BOM → 套餐 → 定价 ─→ [订单]          │
│  │  │                  │                       │                         │
│  │  │                  └→ 食材成本 ─→ [财务成本]  └→ [溯源码预生成]    │
│  │  │                                                                    │
│  │  ┌─[订单] ─── 下单 → 厨房接单 → 上菜 → 支付 → 完成/退款              │
│  │  │           │              │           │           │                │
│  │  │           ↓              ↓           ↓           ↓                │
│  │  │      [桌台占用]    [厨房工单]   [支付记录]   [库存扣减❌]          │
│  │  │                                              [积分累计❌]          │
│  │  │                                              [应收生成❌]          │
│  │  │                                              [溯源码❌]            │
│  │  │                                                                    │
│  │  ┌─[门店经营] ─── 桌台 → 叫号 → 日结 → 营业概览                      │
│  │  │                  │                    │                           │
│  │  │                  └→ [订单]            └→ [财务凭证]❌ 断链        │
│  │  │                                                  ↓                 │
│  │  │                                            [运营中心]❌ 孤岛       │
│  │  │                                                                    │
│  │  ┌─[运营中心] ─── 日报/周报/月报 + 决策看板 + 预警中心                │
│  │  │                  ↑                                                  │
│  │  │            应聚合: 门店日结 + 订单 + 会员 + 库存（当前:6/7页Mock）│
│  │  │                                                                    │
│  │  ┌─[采购] ─── 申请 → 订单 → 收货 → 结算 → 付款                       │
│  │  │           │            │           │           │                  │
│  │  │           │            ↓           ↓           ↓                  │
│  │  │      [门店物资需求]  [仓储入库]  [应付账款]  [四账联动]            │
│  │  │           ❌断链         ✅        ❌断链      ✅                   │
│  │  │                                                                    │
│  │  ┌─[仓储] ─── 入库 → 台账 → 出库/调拨/盘点 → 智能补货(AI)            │
│  │  │                  │                    │           │                │
│  │  │                  └→ [溯源码生成]     │      └→ [采购申请]❌       │
│  │  │                                       ↓                             │
│  │  │                                  [销售出库]→[应收账款]❌           │
│  │  │                                                                    │
│  │  ┌─[会员营销] ─── 注册 → 充值 → 消费 → 积分 → 升级 → 优惠券          │
│  │  │                  │           │           │           │            │
│  │  │              [充值记录]  [余额扣减❌死代码]  [等级升级❌]  [券核销❌]│
│  │  │                                                                    │
│  │  ┌─[财务] ─── 凭证 → 审核 → 过账 → 报表 + 四账联动 + 自动记账        │
│  │  │           ↑                              ↑                         │
│  │  │      [应付款/收款]                  [18条规则]                      │
│  │  │      [电子凭证]❌UI缺失             [大部分未触发]❌                │
│  │  │                                                                    │
│  │  ┌─[资产] ─── 采购 → 领用 → 折旧 → 维修 → 报废                       │
│  │  │                  │           │                                     │
│  │  │                  └→[财务凭证]❌  └→[财务凭证]❌                    │
│  │  │                                                                    │
│  │  ┌─[人事] ─── 招聘 → 面试 → 入职 → 在职 → 离职 → 归档                │
│  │  │           │           │           │           │                    │
│  │  │      [招聘需求]  [面试记录]  [员工档案]  [❌无离职流程]            │
│  │  │           │                       │                                │
│  │  │      [门店招聘❌断链]        [薪资→财务成本❌]                     │
│  │  │                                                                    │
│  │  ┌─[设备] ─── 设备档案 → 打印任务 → 状态日志 → 预警                   │
│  │  │           │           │                                          │
│  │  │      [12+ Controller混乱]  [订单未触发打印❌]                      │
│  │  │      [device_data表已建未启用]                                     │
│  │  │                                                                    │
│  │  ┌─[电子签章] ─── 印章档案 → 用印申请 → 用印记录                      │
│  │  │              ❌无后端（前端100%Mock，API路径虚构）                 │
│  │  │              [electronic_signature表语义混乱]                     │
│  │  │              [公司印章存内存变量，重启丢失]                        │
│  │  │                                                                    │
│  │  ┌─[系统] ─── RBAC + 菜单 + 字典 + 配置 + 审计 + AI配置              │
│  │  │           │                                                        │
│  │  │      [菜单双轨: 前端menu.ts vs 后端menus表无Controller]            │
│  │  │      [AI模型配置: 前端100%Mock，无后端表/Controller/Service]      │
│  │  │      [字典: 后端已实现，前端业务页硬编码状态映射]                  │
│  │  │                                                                    │
│  └─[食品溯源] ─── 批次 → 溯源码 → 节点 → 扫码 → 召回 ✅最完整           │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘

图例：
  ───→  已实现的数据流
  ❌    当前未实现/断链/死代码
  ⚠️    部分实现或双系统割裂
```

### 0.2 V3 模块覆盖对照

| # | 模块 | V2 覆盖 | V3 状态 | 数据对象数 |
|---|------|---------|---------|-----------|
| 1 | 人事管理 | ✅ | 保留 V2 | 9 环节 |
| 2 | 门店经营 | ✅ | 保留 V2 | 6 环节 |
| 3 | 采购管理 | ✅ | 保留 V2 | 7 实体 |
| 4 | 仓储管理 | ✅ | 保留 V2 | 8 实体 |
| 5 | 财务管理 | ✅ | 保留 V2 | 22 实体 |
| 6 | 食品溯源 | ✅ | 保留 V2 | 3 实体 |
| 7 | 资产管理 | ✅ | 保留 V2 | 5 实体 |
| 8 | 工作台 | ❌ | **V3 新增** | 0（聚合） |
| 9 | 产品中心 | ❌ | **V3 新增** | 7 实体 |
| 10 | 订单管理 | ❌ | **V3 新增** | 8 实体 |
| 11 | 运营中心 | ❌ | **V3 新增** | 6 实体 |
| 12 | 会员营销 | ❌ | **V3 新增** | 11 实体 |
| 13 | 设备管理 | ❌ | **V3 新增** | 8 实体 |
| 14 | 电子签章 | ❌ | **V3 新增** | 0（无后端） |
| 15 | 系统管理 | ❌ | **V3 新增** | 20+ 实体 |

---

## 1. 人事域全链路数据生命周期（用户重点关注）

### 1.1 完整链路设计

```
[1.招聘需求] → [2.简历投递] → [3.面试管理] → [4.录用发Offer]
                                              ↓
[9.档案归档] ← [8.离职管理] ← [7.异动管理] ← [6.在职管理] ← [5.入职管理]
                                  ↑                ↓
                              [调岗/晋升/调薪]  [考勤/薪资/合同/培训/健康证]
                                                   ↓
                                              [财务成本记录]
```

### 1.2 各环节详细数据流转

#### 环节 1：招聘需求

**数据对象**：
- `recruitment_requirements` 表（含 `storeId`/`storeName`/`type`='hr'|'store'）
- `RecruitmentRequirement` 实体

**两种来源**：
- HR 总部发起（type='hr'）：HRRecruitment.vue Tab1
- 门店提报（type='store'）：StoreRecruitment.vue → StoreManagementRecruitmentController 三级审批（区域经理 → 总部 HR → 总部 HR 总监）

**状态流转**：
```
draft → submitted → reviewing → approved → recruiting → closed
                                        → rejected
```

**当前状态**：
- ✅ 后端表+Controller 完整
- ❌ HR 前端 HRRecruitment.vue 全 Mock，不调用后端
- ✅ 门店前端 StoreRecruitment.vue 真实对接后端
- ❌ HR 与门店两套系统字段不对齐，无数据同步

##### 环节 1.1：招聘名额下发与分配（V3 补充）

**应有数据对象**：
- `recruitment_quotas` 表（招聘名额表，应有但缺失）
  - 字段：`quotaId`/`year`/`quarter`/`storeId`/`storeName`/`positionId`/`positionName`/`headcount`（名额数）/`usedCount`（已用数）/`status`/`issuedBy`/`issuedTime`/`confirmedBy`/`confirmedTime`
- `RecruitmentQuota` 实体

**完整流程**：
```
[HR 制定年度/季度招聘计划]
        ↓
[按门店+岗位分配名额] —— 记录 recruitment_quotas
        ↓
[下发名额到门店] —— 门店收到名额通知（站内通信）
        ↓
[门店确认/申请追加] —— 门店确认接受或申请追加名额
        ↓
[HR 审核追加申请] —— 通过/驳回/调整
        ↓
[名额生效] —— 门店在名额范围内提报招聘需求
```

**状态流转**：
```
draft → issued → confirmed → active → exhausted → closed
                → rejected（门店拒绝接受）
                → adjustment_requested（门店申请追加）
```

**当前状态**：
- ❌ 完全缺失——无 `recruitment_quotas` 表、无 QuotaController/Service
- ❌ HR 无法向门店下发名额，门店无限额约束可随意提报
- ❌ 名额使用情况无法追踪（已用/剩余不透明）

**需补充**：
- 新建 `recruitment_quotas` 表
- 新建 RecruitmentQuotaController/Service
- 门店招聘需求提报时校验名额（`usedCount < headcount`）
- 招聘完成（入职成功）时 `usedCount += 1`

##### 环节 1.2：门店↔HR 招聘协同反馈（V3 补充）

**问题背景**：当前门店招聘（type='store'）和 HR 招聘（type='hr'）是两套割裂系统，门店提报后 HR 无法反馈，HR 的招聘进度门店也看不到。

**应有协同流程**：
```
[门店提报招聘需求] —— type='store'，附带门店期望/薪资范围/到岗时间
        ↓
[HR 收到通知] —— 站内通信通知 HR 审核
        ↓
[HR 审核反馈] —— 三种结果：
        ├─→ ✅ approved：通过，HR 接管招聘流程（发布到招聘渠道）
        ├─→ 🔄 feedback：需修改（薪资不合理/岗位描述不清/名额超限），附反馈意见
        └─→ ❌ rejected：驳回（名额已用完/岗位编制冻结），附驳回原因
        ↓
[门店根据反馈修改] —— 修改后重新提交
        ↓
[HR 接管后] —— 简历筛选 → 面试安排 → 面试结果同步给门店
        ↓
[门店协同面试] —— 门店负责人参与终面，提交门店面评
        ↓
[录用决策] —— HR + 门店共同确认录用
```

**应有数据对象**：
- `recruitment_feedback` 表（招聘反馈表，应有但缺失）
  - 字段：`feedbackId`/`requirementId`/`feedbackType`（approve/feedback/reject）/`content`/`reviewerId`/`reviewerName`/`reviewTime`
- 在 `interviews` 表增加 `storeInterviewerId`/`storeInterviewerName`/`storeEvaluation` 字段（门店面评）

**当前状态**：
- ❌ 无反馈表，HR 审核结果无法记录和追踪
- ❌ 门店提报后无状态回传，门店不知道 HR 是否已审核
- ❌ 面试无门店协同，门店负责人无法参与面评
- ❌ 录用决策无门店确认环节

**需补充**：
- 新建 `recruitment_feedback` 表
- 扩展 `interviews` 表增加门店面评字段
- 门店招聘需求审核流程增加 HR 反馈节点
- 录用决策增加门店确认环节

#### 环节 2：简历投递

**数据对象**：
- `resumes` 表（关联 `requirementId`）
- `Resume` 实体

**状态流转**：
```
pending → screening → shortlisted → interview_scheduled → hired → rejected
```

**当前状态**：
- ✅ 后端 ResumeController /v1/resumes 完整
- ❌ HR 前端全 Mock

#### 环节 3：面试管理

**数据对象**：
- `interviews` 表（关联 `resumeId`/`requirementId`，含轮次/结果/反馈）
- `Interview` 实体

**状态流转**：
```
scheduled → completed → passed → next_round
                    → failed
```

**当前状态**：
- ✅ 后端 InterviewController /v1/interviews 完整
- ❌ HR 前端全 Mock

#### 环节 4：录用发 Offer

**数据对象**：
- ❌ 后端无 hires/offers 表
- ⚠️ HR 前端 hireApi 操作 Mock `HireRecord`

**当前状态**：
- ❌ 录用数据无法持久化、无法跨用户共享、无法审计
- ❌ 录用→入职仅前端状态切换，无后端联动

**需补充**：
- 新建 `job_offers` 表（offerId/requirementId/resumeId/candidateName/positionId/proposedSalary/offerStatus/sendTime/acceptTime）
- 新建 OfferController/OfferService

#### 环节 5：入职管理

**数据对象**：
- `onboarding_records` 表（含 `storeId`/`storeName`/`resumeId`/`requirementId`/`interviewId`/`probationStart`/`probationEnd`/`probationMonths`）
- `OnboardingRecord` 实体
- `OnboardingArchive` 实体（入职档案）
- `OnboardingInvitation` 实体（入职邀请码）

**流程**：
```
HR 发送入职邀请码 → 候选人填写入职信息 → HR 审核 → 完成入职
                                                        ↓
                                            OnboardingRecordServiceImpl.completeOnboarding()
                                                        ↓
                                            createEmployeeProfile() 创建 Employee
```

**当前状态**：
- ✅ 后端 OnboardingRecordController 完整，completeOnboarding() 真实创建 Employee
- ❌ HR 前端 HRRecruitment.vue 的 handleOnboardHire() 仅更新 Mock 状态，**未调用后端 OnboardingRecordController**
- ❌ **严重 Bug**：`createEmployeeProfile()` 第 143-166 行**未设置 `Employee.storeId`**（即使 OnboardingRecord 有 storeId）
- ❌ 门店招聘审批通过后无自动生成 OnboardingRecord

#### 环节 6：在职管理

**数据对象**：
- `employees` 表（含 `storeId`/`storeName`/`status`='active'|'inactive'|'probation'）
- `Employee` 实体
- `employee_labor_contracts` 表（劳动合同）
- `attendances` 表（考勤）
- `salaries` 表（薪资）
- `health_certificates` 表（健康证）
- `training_records` 表（培训）

**子流程**：

**6.1 试用→转正**：
```
入职（status='probation'，probationStart/probationEnd 设置）
  ↓ 试用期到期前 30 天预警
转正申请 → 审批 → status='active' + 薪资调整
```
- ⚠️ 当前：OnboardingRecord 有 probationMonths 字段但无转正审批 API、无到期提醒、无转正薪酬调整流程

**6.2 考勤**：
```
每日打卡 → 考勤记录 → 月度汇总 → 薪资计算
```
- ✅ 后端 AttendanceController 完整
- ⚠️ 与门店 ShiftManagement.vue 双系统割裂
- ❌ 考勤数据未参与薪资自动计算

**6.3 薪资**：
```
薪资标准设定 → 月度考勤汇总 → 薪资计算 → 薪资确认 → 发放
                                              ↓
                                        财务成本记录（人工成本）
```
- ✅ 后端 SalaryController + SalaryAdjustmentService 完整
- ❌ 薪资确认未生成财务成本记录（finance_cost_record）

**6.4 合同**：
```
入职 → 签订劳动合同 → 合同续签 → 合同到期预警
```
- ✅ 完整电子合同流程（EmployeeLaborContractController + ElectronicContractController）

**6.5 健康证**：
```
入职 → 健康证办理 → 到期预警 → 续期 → 报销
```
- ✅ 后端 HealthCertificateController 完整，含到期预警
- ⚠️ 与门店 StoreCertificate.vue + StoreHealthCertificateController 双系统割裂

**6.6 培训**：
```
培训计划 → 培训执行 → 培训完成 → 培训记录
```
- ✅ HRTraining.vue + HRKnowledgeBase.vue 完整

**6.7 员工-门店归属**：
- ✅ Employee 实体有 storeId 字段（第 106-108 行）
- ❌ HREmployee.vue 不显示门店字段
- ❌ OnboardingRecordServiceImpl.createEmployeeProfile() 未设置 storeId

#### 环节 7：异动管理

**应有数据对象**：
- ❌ 无 EmployeeTransfer/PositionChange/DepartmentChange 实体
- ⚠️ 仅 OrganizationChangeLogController 记录组织变更

**应有流程**：
```
调岗申请 → 审批 → 部门/岗位变更 + 薪资调整 + 合同变更
晋升申请 → 审批 → 职级提升 + 薪资调整
调薪申请 → 审批 → 薪资标准变更
```

**当前状态**：❌ 完全缺失

#### 环节 8：离职管理

**应有数据对象**：
- ❌ 全后端无 resignation/离职 实体

**应有流程**：
```
离职申请 → 审批 → 工作交接 → 资产归还 → 离职证明 → status='inactive'
                                                                ↓
                                                          档案归档
```

**当前状态**：
- ❌ HREmployee.vue 有 'resigning'/'resigned'/'retired' 枚举但无离职 API
- ❌ 离职无闭环

#### 环节 9：档案归档

**应有数据对象**：
- ⚠️ EmployeeArchiveDetailController 存在
- ❌ 无离职档案归档、无离职员工档案库

**当前状态**：❌ 缺失

### 1.3 人事域跨模块联动矩阵

| 联动点 | 触发 | 影响 | 当前状态 |
|--------|------|------|---------|
| 招聘→入职 | 录用→入职 | 创建 Employee | ❌ 前端不调用后端 |
| 门店招聘→入职 | 审批通过 | 生成 OnboardingRecord | ❌ 无自动生成 |
| 入职→门店 | 创建 Employee | Employee.storeId 设置 | ❌ storeId 丢失 |
| 入职→注册账号 | 完成入职 | User 账号创建+角色权限分配 | ❌ createUserAccount() 缺失（V3 补充） |
| HR→门店名额下发 | 名额分配 | recruitment_quotas 创建 | ❌ 表/Controller 缺失（V3 补充） |
| 门店招聘→HR 反馈 | 门店提报需求 | recruitment_feedback 创建 | ❌ 反馈流程缺失（V3 补充） |
| 门店面评→面试 | 门店终面 | interview.storeEvaluation | ❌ 字段/流程缺失（V3 补充） |
| 证件报销→财务 | 报销完成 | finance_voucher + cost_record | ❌ 报销流程缺失（V3 补充） |
| 考勤→薪资 | 月度考勤汇总 | 薪资计算 | ❌ 未联动 |
| 薪资→财务 | 薪资确认 | finance_cost_record | ❌ 未联动 |
| 离职→排班 | 员工离职 | 排班表移除 | ❌ 无离职流程 |
| 离职→资产 | 员工离职 | 资产归还 | ❌ 无离职流程 |
| 离职→账号停用 | 员工离职 | User.status=inactive | ❌ 无离职流程（V3 补充） |
| 健康证→门店 | 到期预警 | 门店证件管理 | ⚠️ 双系统割裂 |
| 培训完成→门店 | 培训完成 | 门店待办通知 | ❌ 跨模块事件未实现 |

---

## 2. 门店经营域数据生命周期（用户重点关注）

### 2.1 门店经营完整链路设计

```
[开店] → [桌台设置] → [叫号排队] → [入座] → [下单] → [上菜] → [结账] → [日结]
                                                                        ↓
                                                                  [营收凭证]
                                                                        ↓
                                                                  [财务报表]
                                                                        ↓
                                                                  [运营决策]
```

### 2.2 各环节详细数据流转

#### 环节 1：开店准备

**数据对象**：
- `stores` 表（门店基础信息）
- `dining_tables` 表（桌台配置）
- `call_number_settings` 表（叫号设置）

**当前状态**：
- ✅ StoreController /v1/stores
- ✅ DiningTableController 桌台配置
- ✅ CallNumberSettingsController 叫号设置

#### 环节 2：叫号排队

**数据对象**：
- `call_number_queue` 表（排队队列）

**流程**：
```
顾客取号 → 排队等待 → 叫号 → 入座
```

**当前状态**：
- ✅ CallNumberController / CallNumberQueueManagementController 完整
- ✅ StoreQueueHistory.vue 真实对接后端
- ❌ **叫号→入座→下单流程断裂**：叫号入座后未自动关联桌台和订单

#### 环节 3：桌台使用

**数据对象**：
- `dining_table_usage` 表（桌台使用记录，关联 orderId）

**流程**：
```
桌台空闲 → 入座（占用）→ 用餐中 → 结账 → 清台 → 空闲
```

**当前状态**：
- ✅ DiningTableManagementController 完整
- ✅ StoreTableUsage.vue 含桌台配置+订单使用记录两 Tab
- ⚠️ 与订单通过 orderId 关联，但订单数据 Mock

#### 环节 4：订单管理

**数据对象**：
- ❌ 订单表（应存在但前端全 Mock）

**流程**：
```
下单 → 厨房接单 → 上菜 → 结账 → 支付
```

**当前状态**：
- ❌ OrderQuery.vue / OrderStatistics.vue / OrderRefund.vue / OrderReservation.vue 全部硬编码 Mock
- ❌ 订单数据未流向门店日结、运营报表、桌台使用

#### 环节 5：日结对账

**数据对象**：
- `daily_settlements` 表（含营收/成本/利润/4 种支付方式/优惠抵扣/班次明细）

**流程**：
```
班次结束 → 收银员汇总 → 日结提交 → 店长审核 → 归档
```

**当前状态**：
- ✅ StoreManagementSettlementController 完整
- ✅ StoreDailySettlement.vue 真实对接后端
- ✅ 敏感字段（成本/利润/毛利率）按角色过滤
- ❌ **日结数据未流向财务凭证系统**（无自动生成营收凭证）
- ❌ 订单数据靠前端 Mock 注入，非真实订单汇总

#### 环节 6：门店总览

**数据对象**：
- 综合数据（营收/订单/客流/翻台率/当值员工）

**当前状态**：
- ⚠️ StoreStatusOverview.vue 从 `mockDataCenter` 取数
- ❌ **与运营中心 OperationsDashboard 数据完全独立**（门店总览数据孤岛）

### 2.3 门店经营数据应流向

| 数据类型 | 当前去向 | 应流向 | 状态 |
|---------|---------|--------|------|
| 销售额（营收） | 仅日结表内 | 财务凭证（营收凭证）+ 运营中心 + 报表中心 | ❌ 断链 |
| 订单数/客流 | 仅门店总览 | 运营中心实时监控 + 运营策略工坊 | ❌ 断链 |
| 桌台数据 | 桌台记录页 | 运营决策看板（翻台率分析） | ❌ 断链 |
| 排队数据 | 叫号记录页 | 运营预警中心（高峰期排队预警） | ❌ 断链 |
| 日结数据 | 日结对账页 | 财务凭证 + 报表中心 | ❌ 断链 |
| 门店招聘审批 | 门店招聘页 | HR 招聘 + 入职记录 + 员工档案 | ❌ 断链 |

### 2.4 门店经营数据的 4 个严重孤岛

#### 孤岛 1：门店总览 → 运营中心

- **数据源**：`StoreStatusOverview` 的营收/订单/翻台率
- **应流向**：`OperationsDashboard` / `DecisionBoard` / `LiveMonitor`
- **实际**：运营中心 5 个页面全部硬编码 Mock（春熙路店、宽窄巷子店等 7 家门店的固定数据）
- **影响**：运营决策无数据支撑
- **修复方案**：运营中心改为调用 `/v1/stores` + `/api/v1/store-management/settlements` 取真实数据

#### 孤岛 2：日结对账 → 财务凭证

- **数据源**：`StoreDailySettlement` 的营收/成本/利润
- **应流向**：财务模块自动生成营收凭证（借：银行存款/现金，贷：主营业务收入+应交税费）
- **实际**：日结数据未流向财务模块
- **影响**：财务对账无真实日结依据，营收凭证需手工录入
- **修复方案**：日结审核通过后触发自动记账规则 E01（POS 堂食）/ E02-E04（外卖平台）

#### 孤岛 3：门店招聘 → HR 入职

- **数据源**：`StoreManagementRecruitment` 三级审批
- **应流向**：HR 招聘 `HRRecruitment` + 入职记录 `OnboardingRecord` + 员工档案 `Employee`
- **实际**：审批通过后无自动生成 OnboardingRecord 或 Employee
- **影响**：招聘→入职→员工档案核心链路断裂
- **修复方案**：审批通过后自动创建 OnboardingRecord，并通知 HR 处理入职

#### 孤岛 4：门店物资需求 → 采购审批

- **数据源**：`StoreMaterialRequest` 门店提报
- **应流向**：采购模块 `MaterialRequest` 审批流
- **实际**：前端注释明确"与采购模块的物资需求提报分离，仅展示本门店数据"
- **影响**：门店提报的物资需求未自动流转到采购审批
- **修复方案**：门店物资需求提交后自动同步到采购模块 MaterialRequest

---

## 3. 采购域数据生命周期

### 3.1 数据对象

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 采购申请 | `purchase_request` | `status` (draft/submitted/approved/rejected/closed) |
| 采购订单 | `purchase_order` | `status` (draft/confirmed/received/partially_received/closed) |
| 采购收货 | `purchase_stockin` | `status` (draft/received/quality_checked/returned) |
| 采购结算 | `purchase_settlement` | `status` (draft/confirmed/paid) |
| 供应商档案 | `supplier` | `status` (active/inactive) |
| 物资分类 | `material_category` | — |
| 物资需求 | `material_request` | `status` (draft/submitted/fulfilled) |

### 3.2 数据流转

```
1. 采购申请创建（status=draft）—— 来源：HR 招聘需求/门店物资需求/仓储补货
   ↓ 提交审批
2. 采购申请审批（status=approved）
   ↓ 生成采购订单
3. 采购订单创建（status=draft）
   ↓ 确认订单
4. 采购订单确认（status=confirmed）
   ↓ 供应商发货
5. 采购收货（status=received）—— 单事务：
   ├─→ 触发：仓储入库（inventory 表 + stock_movement 表）
   ├─→ 触发：应付账款创建（finance_payable 表）❌ 当前未实现
   └─→ 触发：溯源批次创建（trace_batch 表）✅ 已实现
   ↓ 结算
6. 采购结算（status=confirmed）
   ├─→ 触发：应付账款更新（已付金额累加）❌ 当前未实现
   └─→ 触发：付款单创建（finance_payment 表）❌ 当前未实现
   ↓ 付款
7. 付款单确认 —— 四账联动：
   ├─→ finance_payable 更新 ✅
   ├─→ finance_bank_account 更新 ✅
   ├─→ finance_fund_flow 插入 ✅
   └─→ finance_voucher 创建（暂存状态）✅
```

### 3.3 当前缺陷

- ❌ 采购收货 → 应付账款联动未实现
- ❌ 采购结算 → 付款联动未实现
- ❌ 采购申请 → 采购订单自动生成未实现
- ❌ 门店物资需求 → 采购申请联动未实现
- ❌ 智能补货建议 → 采购申请联动未实现
- ❌ 供应商档案管理未对接

---

## 4. 仓储域数据生命周期

### 4.1 数据对象

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 库存台账 | `inventory` | `quantity`、`locked_quantity` |
| 库存流水 | `stock_movement` | `movement_type` (in/out/transfer/adjust) |
| 仓库 | `warehouse` | `status` |
| 库位 | `storage_location` | `status` |
| 盘点单 | `stock_count` | `status` (draft/counting/counted/approved) |
| 调拨单 | `stock_transfer` | `status` (draft/transferring/completed/cancelled) |
| 出库单 | `stock_out` | `status` (draft/out/approved) |
| 入库单 | `stock_in` | `status` (draft/in/approved) |

### 4.2 数据流转

```
入库流程：
  采购收货 / 生产入库 / 退货入库
    ↓
  stock_in 创建（status=draft）
    ↓ 审批
  stock_in 确认（status=in）
    ├─→ inventory.quantity += 入库数量
    └─→ stock_movement 插入（type=in）
    ↓
  触发溯源码生成（trace_batch + trace_code）✅ 已实现

出库流程：
  销售出库 / 领料出库 / 报废出库
    ↓
  stock_out 创建（status=draft）
    ↓ 审批
  stock_out 确认（status=out）
    ├─→ inventory.quantity -= 出库数量
    ├─→ inventory.locked_quantity -= 锁定数量（如有）
    └─→ stock_movement 插入（type=out）
    ↓
  销售出库应触发：
    ├─→ 应收账款创建 ❌ 当前未实现
    ├─→ 订单状态变更 ❌ 当前未实现
    └─→ 溯源码状态变更 ✅ 已实现

调拨流程：
  stock_transfer 创建（status=draft）
    ↓ 确认
  stock_transfer 执行中（status=transferring）
    ├─→ 源库位 inventory.quantity -= 数量
    └─→ 目标库位 inventory.quantity += 数量
    ↓ 完成
  stock_transfer 完成（status=completed）
    └─→ stock_movement 插入（type=transfer）
    ↓
  连锁门店间调拨应触发：
    └─→ 内部交易抵消分录（合并报表）❌ 当前未实现

盘点流程：
  stock_count 创建（status=draft）
    ↓ 开始盘点
  stock_count 盘点中（status=counting）
    ↓ 录入实际数量
  stock_count 已盘点（status=counted）
    ↓ 审批
  stock_count 已审批（status=approved）
    └─→ 若有差异：stock_movement 插入（type=adjust）+ inventory.quantity 调整
```

### 4.3 智能补货建议（AI 应用）

**当前实现**：`SmartRestock.vue` 整页组件
- 多模型对比（动态列）
- 置信度展示（high/medium/low）
- 模型一致性判断（≤20% 差异）
- 紧急程度映射（urgent/high/medium/low）

**应联动**：
- 补货建议 → 自动生成采购申请 ❌ 当前未实现
- 补货建议 → 通知门店店长 ❌ 当前未实现

### 4.4 当前状态

- **前端**：75% 完成，3 处 teleported 违规需修复
- **后端**：基本完成
- **缺失**：
  - 库存预警阈值配置
  - 批次管理（食品保质期）
  - 销售出库 → 应收账款联动
  - 智能补货 → 采购申请联动

---

## 5. 财务域数据生命周期

### 5.1 数据对象（22 个实体）

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 会计科目 | `accounting_subject` | `status` (1=启用/0=停用) |
| 记账凭证 | `finance_voucher` | `voucher_status` (0=暂存/1=已审核/2=已过账/3=已作废) |
| 凭证明细 | `finance_voucher_detail` | — |
| 应付账款 | `finance_payable` | `status` (unpaid/partially_paid/paid) |
| 应收账款 | `finance_receivable` | `status` (unreceived/partially_received/received) |
| 付款单 | `finance_payment` | `status` (draft/confirmed/void) |
| 收款单 | `finance_receipt` | `status` (draft/confirmed/void) |
| 银行账户 | `finance_bank_account` | `balance` |
| 资金流水 | `finance_fund_flow` | `flow_type` (in/out) |
| 预算 | `finance_budget` | `status` (draft/approved/executing/closed) |
| 成本记录 | `finance_cost_record` | — |
| 电子凭证 | `electronic_voucher` | `verify_status` |
| 电子发票 | `electronic_invoice` | — |
| 自动记账规则 | `accounting_rule` | `enabled` |
| 分录模板 | `entry_template` | — |
| 自动记账日志 | `auto_voucher_log` | — |
| 标准成本卡 | `standard_cost_card` | — |
| 利润表 | `profit_statements` | — |
| 审批流配置 | `finance_approval_workflow` | — |
| 报表配置 | `report_configs` | — |
| 报表洞察规则 | `report_insight_rules` | — |
| 导出任务 | `export_tasks` | `status` (pending/processing/completed/failed) |

### 5.2 凭证状态机（核心）

```
                  审核                  过账
[暂存 0] ──────────→ [已审核 1] ──────────→ [已过账 2]
   ↑                     |                     |
   |     反审核          |                     |
   └─────────────────────┘                     |
   |              反过账                       |
   └───────────────────────────────────────────┘
                         ↓
                    [已作废 3]
         （仅暂存/已审核可作废，已过账需红字冲销）
```

**状态流转规则**：
- `0 → 1`：审核（approve）✅
- `1 → 0`：反审核（unapprove）✅
- `1 → 2`：过账（post）✅
- `2 → 1`：反过账（unpost）✅
- `0/1 → 3`：作废（void）✅
- `2 → 3`：禁止，需红字冲销凭证

### 5.3 四账联动（付款/收款）

**付款流程**（PaymentServiceImpl）：

```
用户创建付款单（draft）
  ↓ 确认付款
付款单确认（confirmed）—— 单事务内执行：
  ├─ 1. 更新 finance_payable
  │      ├─ 已付金额 += 付款金额
  │      ├─ 未付金额 -= 付款金额
  │      └─ 若已付=总额：status → paid
  ├─ 2. 更新 finance_bank_account
  │      └─ balance -= 付款金额
  ├─ 3. 插入 finance_fund_flow
  │      └─ flow_type=out, amount=付款金额
  └─ 4. 创建 finance_voucher
         └─ voucher_status=0（暂存），source_type=payment
  ↓
凭证需人工审核 + 过账才能影响总账
```

**收款流程**（ReceiptServiceImpl）：与付款对称，`flow_type=in`，关联 `finance_receivable`。

### 5.4 自动记账规则引擎

**数据库已建 4 张表 + 18 条规则**（V20260405）：

| 规则编号 | 业务场景 | 触发条件 | 借方科目 | 贷方科目 |
|---------|---------|---------|---------|---------|
| E01 | POS 堂食销售 | 日结确认 | 银行存款/现金 | 主营业务收入+应交税费 |
| E02 | 美团外卖 | 平台结算 | 银行存款+销售费用(佣金) | 主营业务收入+应交税费 |
| E03 | 饿了么外卖 | 平台结算 | 同上 | 同上 |
| E04 | 抖音外卖 | 平台结算 | 同上 | 同上 |
| E05 | 生鲜采购 | 采购入库 | 原材料+应交税费(进项) | 应付账款 |
| E06 | 调料采购 | 采购入库 | 同上 | 同上 |
| E07 | 酒水采购 | 采购入库 | 同上 | 同上 |
| E08 | 付供应商款 | 付款确认 | 应付账款 | 银行存款 |
| E09 | 发工资 | 薪资确认 | 应付职工薪酬 | 银行存款+应交个税 |
| E10 | 计提工资社保 | 月末计提 | 管理费用/销售费用 | 应付职工薪酬 |
| E11 | 平台结算 | 平台对账 | 银行存款 | 应收账款+销售费用 |
| E12 | 成本结转 | 月末结转 | 主营业务成本 | 原材料 |
| E13 | 付房租 | 房租支付 | 长期待摊费用 | 银行存款 |
| E14 | 付水电 | 水电支付 | 管理费用 | 银行存款 |
| E15 | 收定金 | 定金收取 | 银行存款 | 预收账款 |
| E16 | 固定资产购置 | 资产采购 | 固定资产 | 银行存款 |
| E17 | 月末折旧 | 月末计提 | 管理费用/销售费用 | 累计折旧 |
| E18 | 月末税费计提 | 月末计提 | 税金及附加 | 应交税费 |

**当前状态**：
- ✅ 数据库表+规则完整
- ❌ 前端 AutoVoucher.vue 仅展示凭证模板列表
- ❌ 无规则配置 UI、无日志查看、无手工触发、无质量审核工作台
- ❌ 日结确认未触发 E01-E04
- ❌ 采购入库未触发 E05-E07
- ❌ 薪资确认未触发 E09-E10
- ❌ 月末结账未触发 E12/E17/E18

### 5.5 电子凭证生命周期（数据库已建表，UI 缺失）

```
电子凭证接收（XML/OFD/PDF）
  ↓ 解析
结构化数据（parsed_data）
  ↓ 验签
验签记录（voucher_signature_log）
  ↓ 验真
验真状态管理
  ↓ 关联入账
电子凭证 → 记账凭证（finance_voucher_id）
  ↓ 归档
电子凭证归档（archive_id）
```

**当前状态**：
- ✅ 9 张电子凭证表已建
- ❌ 前端无任何电子凭证 UI

### 5.6 预算生命周期

```
预算创建（draft）
  ↓ 审批
预算审批（approved）❌ 无审批工作流
  ↓ 开始执行
预算执行中（executing）
  ├─→ 实际成本/费用发生时，actual_amount 累加 ❌ 未自动累加
  └─→ execution_rate = actual_amount / budget_amount * 100%
  ↓ 年末关闭
预算关闭（closed）
```

### 5.7 成本记录生命周期

```
成本记录创建
  ├─→ 关联预算（如有）
  ├─→ 计算偏差 = 实际金额 - 预算金额
  └─→ 计算偏差率 = 偏差 / 预算金额 * 100%
  ↓
成本记录归档（按月汇总）
```

**应联动**：
- 薪资确认 → 自动生成人工成本记录 ❌
- 资产折旧 → 自动生成折旧成本记录 ❌
- 采购入库 → 自动生成食材成本记录 ❌

### 5.8 报表生命周期

```
凭证过账 → 科目余额更新 ❌ 当前未实现
  ↓
报表生成（利润表/资产负债表/现金流量表）✅ 已实现 3 张
  ↓
报表洞察（report_insight_rules）❌ 无 UI 触发
  ↓
报表导出（export_tasks）✅ 已实现 Excel/PDF 异步导出
  ↓
报表归档（按月封存）❌ 未实现
```

**缺失的报表**（详见 PROJECT_CONTEXT.md 第 8.3 节）：
- 法定：所有者权益变动表、附注、年度汇总、季度对比
- 管理：管理利润表、分门店/分渠道利润表、单品盈利分析
- 成本：标准成本差异分析、食材成本日报、毛利率分析
- 税务：增值税申报表、企业所得税申报表、个税申报表
- 合并：合并资产负债表、合并利润表、合并现金流量表

---

## 6. 食品溯源域数据生命周期

### 6.1 数据对象

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 溯源码 | `trace_code` | `status` (active/used/recalled) |
| 批次 | `trace_batch` | `status` |
| 溯源节点 | `trace_node` | `node_type` (procurement/production/sales/logistics) |

### 6.2 数据流转

```
采购入库
  ↓ 生成批次
trace_batch 创建 ✅
  ↓ 关联库存
trace_code 生成（一物一码或一批一码）✅
  ├─→ 节点1：procurement（采购节点）✅
  ├─→ 节点2：production（生产节点，如加工）✅
  ├─→ 节点3：logistics（物流节点）✅
  └─→ 节点4：sales（销售节点）✅
  ↓
消费者扫码查询 ✅
  ↓
溯源码状态变更（used）✅
  ↓
若召回
trace_batch.recall（批次召回）✅
  └─→ 关联所有 trace_code 状态 → recalled ✅
  ↓
召回通知 ❌ 未实现
```

### 6.3 当前状态

- **前端**：90% 完成（最佳模块）
- **后端**：完整对接
- **缺失**：召回流程的自动化通知

---

## 7. 资产管理域数据生命周期

### 7.1 数据对象

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 资产 | `asset` | `status` (in_use/idle/scrapped/repairing) |
| 资产分类 | `asset_category` | — |
| 资产领用 | `asset_allocation` | `status` (draft/allocated/returned) |
| 资产维修 | `asset_maintenance` | `status` (pending/in_progress/completed) |
| 资产折旧 | `asset_depreciation` | — |

### 7.2 数据流转

```
资产采购入库（status=idle）
  ↓ 领用
资产领用（status=in_use）
  ├─→ 每月计提折旧 ✅
  ├─→ 维修申请（status=repairing，临时）✅
  │      └─→ 维修完成（status=in_use）✅
  ↓ 退还
资产退还（status=idle）✅
  ↓ 报废
资产报废（status=scrapped）✅
```

### 7.3 应联动

- 资产折旧 → 自动生成折旧凭证（规则 E17）❌ 当前未联动
- 资产购置 → 自动生成购置凭证（规则 E16）❌ 当前未联动
- 员工离职 → 资产归还提醒 ❌ 当前未联动

### 7.4 当前状态

- **前端**：85% 完成
- **后端**：基本完成
- **问题**：7 处本地 `fenToYuan` 违规，需统一用 `fenToYuanNumber`

---

## 8. 工作台域数据生命周期（V3 新增）

### 8.1 数据对象

工作台为**聚合展示型模块**，无独立业务表，数据来源于全模块聚合：

| 数据类型 | 来源表 | 聚合方式 |
|---------|--------|---------|
| 统计卡片 | orders / members / inventory / finance_voucher | 按时间维度计数/求和 |
| 待办事项 | 各模块 status=pending 的审批单 | 按角色过滤 |
| 预警通知 | device_alerts / stock (低库存) / health_certificates (到期) | 按规则触发 |
| 快捷操作 | dashboard_config（用户自定义布局） | 按用户读取 |

### 8.2 数据流转

```
各业务模块数据产生
  ↓
后端聚合查询（DashboardController /v1/dashboard/*）
  ├─→ GET /v1/dashboard/statistics        — 统计卡片数据
  ├─→ GET /v1/dashboard/pending-tasks     — 待办事项
  ├─→ GET /v1/dashboard/alerts            — 预警通知
  ├─→ GET /v1/dashboard/quick-actions     — 快捷操作
  ├─→ GET /v1/dashboard/recent-activities — 最近活动
  ├─→ GET /v1/dashboard/trends            — 趋势数据
  └─→ GET /v1/dashboard/config            — 仪表盘配置
  ↓
前端 Dashboard 展示
```

### 8.3 当前严重缺陷

| # | 缺陷 | 现状 |
|---|------|------|
| 1 | 前端 `views/dashboard/index.vue` 100% 硬编码 Mock | statistics/pendingTasks/alerts/quickActions 全 Mock |
| 2 | 后端 `DashboardController` 7 个端点完整 | ❌ **前端从未调用** |
| 3 | 无角色视图差异 | 店长/收银员/厨师/财务看到相同 Mock |
| 4 | 无用户自定义 widget | dashboard_config 表未使用 |
| 5 | 无快捷操作记录 | 用户高频操作未埋点 |

### 8.4 应联动模块

- ✅ 应聚合：订单（今日订单数/营业额）❌
- ✅ 应聚合：会员（今日新增/活跃会员）❌
- ✅ 应聚合：库存（低预警商品数）❌
- ✅ 应聚合：财务（本月收入/支出/利润）❌
- ✅ 应聚合：设备（在线设备数/异常告警）❌
- ✅ 应聚合：人事（今日考勤异常/待审批）❌

---

## 9. 产品中心域数据生命周期（V3 新增）

### 9.1 数据对象（7 实体）

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 菜品 | `foods` | `status` (1=在售/2=停售/3=售罄)、`category_id` |
| 菜品分类 | `food_categories` | `parent_id`（树形结构）、`sort_order` |
| 套餐 | `dish_combos` | `status`、`validity_start`/`validity_end` |
| 套餐明细 | `combo_ingredients` | `dish_id`、`quantity` |
| 菜品 BOM | `dish_recipes` | `version` (draft/published/archived)、`material_id`、`quantity` |
| 定价历史 | `product_pricing_history` | `price`、`effective_date` |
| 采购物资（旧） | `product` | `category_id`、`unit`、`specification` |

### 9.2 状态机

#### 9.2.1 菜品状态机
```
[新建] → [在售 1] ──停售──→ [停售 2]
              │                 │
              └──售罄──→ [售罄 3]
                              │
                              └──补货──→ [在售 1]
```

#### 9.2.2 BOM 版本状态机
```
[草稿 draft] ──发布──→ [已发布 published] ──归档──→ [已归档 archived]
                            │
                            └──新版本发布时自动归档旧版本
```

### 9.3 数据流转

```
1. 分类管理（food_categories，树形）
   ↓
2. 菜品创建（foods，status=1）
   ├─→ foodCode 生成 ❌ 当前前端生成（应后端生成）
   ├─→ 关联分类 category_id ✅
   └─→ 关联图片（OSS）⚠️ 上传未实现
   ↓
3. BOM 配置（dish_recipes）
   ├─→ 关联采购物资 product_id ✅
   ├─→ 设定用量 quantity ✅
   ├─→ 版本管理（draft→published→archived）✅
   └─→ 自动计算菜品成本 ❌ 当前未联动
   ↓
4. 套餐配置（dish_combos + combo_ingredients）
   ├─→ 选择多个菜品组合 ✅
   ├─→ 设定套餐价格 ✅
   └─→ 设定有效期 ✅
   ↓
5. 定价管理（product_pricing_history）
   ├─→ 记录价格变更历史 ✅
   ├─→ 会员价 ❌ 当前无会员价字段
   └─→ 时段价（午市/晚市）❌ 当前无
   ↓
6. 菜品上架销售
   ├─→ 订单系统调用 ✅（订单 Mock）
   ├─→ sold_today 字段累加 ❌ 订单完成未触发
   └─→ 溯源码预生成（菜品含溯源食材时）❌ 未联动
```

### 9.4 当前缺陷

| # | 缺陷 | 影响 |
|---|------|------|
| 1 | `foodCode` 前端生成 | 编码规则不统一，可能重复 |
| 2 | 无会员价字段 | 会员营销无法差异化定价 |
| 3 | BOM 未自动计算菜品成本 | 财务成本核算无依据 |
| 4 | `specification` 为文本字段 | 无规格主数据表，无法标准化 |
| 5 | 导入/导出全 Mock | 批量管理不可用 |
| 6 | 订单完成未触发 `foods.sold_today++` | 销量统计不准 |
| 7 | 7 处 Dialog 内弹出组件未设 `:teleported="false"` | 弹出漂移 |

### 9.5 跨模块联动

| 联动点 | 触发 | 影响 | 状态 |
|--------|------|------|------|
| 菜品→订单 | 下单 | 订单明细引用 foodId | ⚠️ 订单 Mock |
| BOM→财务成本 | BOM 发布 | 自动计算菜品标准成本 | ❌ 未联动 |
| 菜品含溯源食材→溯源码 | 菜品销售 | 预生成溯源码 | ❌ 未联动 |
| 菜品停售→订单 | 停售操作 | 订单不可选该菜品 | ❌ 未实现 |
| 套餐→订单 | 套餐选择 | 订单展开为子菜品 | ❌ 未实现 |

---

## 10. 订单域数据生命周期（V3 新增）

### 10.1 数据对象（8 实体）

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 订单主表 | `orders` | `status` (0-6, 7 状态)、`payment_status`、`order_type` |
| 订单明细 | `order_items` | `kitchen_status` (0-4)、`food_id`、`quantity` |
| 支付记录 | `order_payment_records` | `payment_method` (6 种)、`amount` |
| 退款记录 | `order_refund_records` | `refund_status` (0-3)、`refund_amount` |
| 桌台 | `dining_tables` | `status` (1-5)、`current_order_id` |
| 预约 | `table_reservations` | `status` (1-5)、`reservation_time` |
| 叫号队列 | `call_number_queues` | `status` (1-5)、`queue_number` |
| 厨房工单 | `kitchen_orders` | `status`、`order_id`、`station` |

### 10.2 状态机（核心）

#### 10.2.1 订单状态机（7 状态）
```
[0 待确认] ──确认──→ [1 已确认/制作中] ──完成──→ [2 已完成]
     │                      │
     │                      ├──取消──→ [3 已取消]
     │                      │
     │                      ├──部分退款──→ [4 部分退款]
     │                      │
     │                      └──全额退款──→ [5 全额退款]
     │
     └──取消──→ [3 已取消]

[2 已完成] ──评价──→ [6 待评价] ──评价完成──→ [已评价]
```

#### 10.2.2 厨房工单状态机（order_items.kitchen_status）
```
[0 待接单] ──接单──→ [1 制作中] ──出菜──→ [2 已出菜] ──上桌──→ [3 已上桌]
                                                     └──退菜──→ [4 已退菜]
```

#### 10.2.3 支付方式（payment_method）
- `cash` 现金 / `wechat` 微信 / `alipay` 支付宝 / `card` 银行卡 / `member_balance` 会员余额 / `coupon` 优惠券抵扣
- 支持**组合支付**（一单多种方式）

#### 10.2.4 桌台状态机
```
[1 空闲] ──入座──→ [2 占用] ──点单──→ [3 用餐中] ──结账──→ [4 待清台] ──清台──→ [1 空闲]
                                                                └──保留──→ [5 保留]
```

### 10.3 数据流转

```
1. 创建订单（status=0 待确认）
   ├─→ 关联桌台 dining_tables.current_order_id ✅
   ├─→ 生成订单明细 order_items ✅
   ├─→ 锁定桌台状态 dining_tables.status=2 ✅
   └─→ 生成叫号（外卖/自取）✅
   ↓ 确认订单
2. 订单确认（status=1 已确认/制作中）
   ├─→ 生成厨房工单 kitchen_orders ✅
   ├─→ order_items.kitchen_status=0 ✅
   └─→ 通知厨房（WebSocket）❌ 未实现
   ↓ 厨房接单
3. 厨房制作（kitchen_status=1）
   ├─→ 出菜（kitchen_status=2）✅
   ├─→ 上桌（kitchen_status=3）✅
   └─→ 退菜（kitchen_status=4）✅
   ↓ 订单完成
4. 订单完成（status=2 已完成）—— 应触发（单事务）：
   ├─→ 库存扣减（inventory.quantity -= 用量）❌ 当前未实现
   ├─→ 桌台释放（dining_tables.status=4）❌ 当前未联动
   ├─→ 会员积分累计（member_points_log）❌ 当前未联动
   ├─→ 优惠券核销（member_coupon.status=used）❌ 当前未联动
   ├─→ 应收账款生成（finance_receivable）❌ 当前未联动
   ├─→ 溯源码状态变更（trace_code.status=used）❌ 当前未联动
   ├─→ 菜品销量累加（foods.sold_today++）❌ 当前未实现
   └─→ 日结数据累加（daily_settlements）❌ 当前未联动
   ↓ 支付
5. 支付记录（order_payment_records）
   ├─→ 支持组合支付（多种 payment_method）✅
   ├─→ 会员余额扣减 ❌ deductBalanceForConsume 是死代码
   └─→ 优惠券核销 ❌ 未联动
   ↓ 退款（如发生）
6. 退款流程（order_refund_records）
   ├─→ refund_status: 0申请→1审核中→2已退款→3已拒绝 ✅
   ├─→ 订单状态变更（部分退款→4/全额退款→5）✅
   └─→ 库存还原 ❌ 未实现
```

### 10.4 当前严重缺陷

| # | 缺陷 | 影响 |
|---|------|------|
| 1 | **前端 4 页 100% Mock** | OrderQuery/OrderStatistics/OrderRefund/OrderReservation 全硬编码 |
| 2 | **无 `api/order/` 目录** | 前端无 API 封装，从未调用后端 |
| 3 | 订单完成未触发库存扣减 | 库存数据不准 |
| 4 | 订单完成未触发桌台释放 | 桌台状态卡死 |
| 5 | 订单完成未触发会员积分累计 | 会员营销链路断裂 |
| 6 | 订单完成未触发应收账款生成 | 财务对账无依据 |
| 7 | 订单完成未触发溯源码变更 | 溯源链路断裂 |
| 8 | 订单完成未触发日结累加 | 门店日结数据不准 |
| 9 | 订单完成未触发菜品销量++ | 产品中心销量统计不准 |
| 10 | 会员余额扣减是死代码 | 会员余额支付不可用 |

### 10.5 后端实现完整度

- ✅ `OrderNewController` 100% 完成（create/pay/cancel/refund/approve/confirm/complete/kitchen-status/today-statistics/shift-summary）
- ✅ 8 张表全部建好
- ✅ 状态机实现完整
- ❌ **前端从未对接**——这是最大的浪费

### 10.6 跨模块联动矩阵

| 联动点 | 触发 | 影响 | 状态 |
|--------|------|------|------|
| 订单→桌台 | 创建订单 | 桌台占用 | ✅ |
| 订单→厨房 | 订单确认 | 生成厨房工单 | ✅ |
| 订单→库存 | 订单完成 | 库存扣减 | ❌ |
| 订单→桌台 | 订单完成 | 桌台释放 | ❌ |
| 订单→会员积分 | 订单完成 | 积分累计 | ❌ |
| 订单→优惠券 | 订单完成 | 优惠券核销 | ❌ |
| 订单→应收账款 | 订单完成 | 应收生成 | ❌ |
| 订单→溯源码 | 订单完成 | 溯源码状态变更 | ❌ |
| 订单→日结 | 订单完成 | 日结数据累加 | ❌ |
| 订单→菜品销量 | 订单完成 | foods.sold_today++ | ❌ |
| 订单→打印 | 订单确认/完成 | 自动打印小票 | ❌ |

---

## 11. 运营中心域数据生命周期（V3 新增）

### 11.1 数据对象（6 实体）

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 销售分析报表 | `sales_analysis_reports` | `report_date`、`store_id` |
| 库存分析报表 | `inventory_analysis_reports` | `report_date` |
| 产品分析报表 | `product_analysis_reports` | `report_date`、`food_id` |
| 报表配置 | `report_configs` | `report_type`、`enabled` |
| 报表洞察规则 | `report_insight_rules` | `rule_type`、`threshold` |
| 导出任务 | `export_tasks` | `status` (0=待处理/1=处理中/2=已完成/3=失败) |

### 11.2 数据流转

```
数据源（应聚合）：
  ├─→ 门店日结 daily_settlements ❌ 当前 Mock
  ├─→ 订单数据 orders ❌ 当前 Mock
  ├─→ 会员数据 members ❌ 当前 Mock
  ├─→ 库存数据 inventory ❌ 当前 Mock
  └─→ 财务数据 finance_voucher ❌ 当前 Mock
   ↓
后端聚合服务（OperationsReportServiceImpl / OperationsDashboardServiceImpl）
  ├─→ 数据权限过滤：getAuthorizedStoreIds() via DataPermissionService ✅
  ├─→ 按 report_type 生成报表 ✅
  └─→ 按 store_id 聚合 ✅
   ↓
前端展示（7 个页面）：
  ├─→ ReportCenter.vue ✅ 真实对接 /v1/operations-reports/{reportType}
  ├─→ OperationsDashboard.vue ❌ 7 门店硬编码 Mock
  ├─→ LiveMonitor.vue ❌ Mock
  ├─→ DecisionBoard.vue ❌ 门店排名硬编码 Mock
  ├─→ StrategyWorkshop.vue ❌ Mock
  ├─→ AlertCommandCenter.vue ❌ Mock
  └─→ DailyReport.vue / WeeklyReport.vue ❌ Mock
```

### 11.3 报表类型

后端 `OperationsReportController` 支持 6 种报表类型：
- `daily` 日报
- `weekly` 周报
- `monthly` 月报
- `quarterly` 季报
- `yearly` 年报
- `profit` 利润分析

### 11.4 数据权限

```
用户请求报表
  ↓
OperationsReportServiceImpl.getAuthorizedStoreIds()
  ├─→ ALL: 所有门店
  ├─→ COMPANY: 全公司门店
  ├─→ DEPARTMENT: 部门下门店
  ├─→ STORE: 单门店
  ├─→ STORES: 指定多门店（role_stores 表）
  └─→ SELF: 仅自己的数据
  ↓
按授权 storeIds 过滤查询
```

### 11.5 当前缺陷

| # | 缺陷 | 影响 |
|---|------|------|
| 1 | 7 个页面中 6 个全 Mock | 仅 ReportCenter 真实对接 |
| 2 | OperationsDashboard 7 门店硬编码 | 运营决策无数据支撑 |
| 3 | DecisionBoard 门店排名硬编码 | 决策看板不可用 |
| 4 | LiveMonitor 实时监控 Mock | 无法实时监控门店 |
| 5 | AlertCommandCenter 预警 Mock | 预警系统不可用 |
| 6 | 占位符内容未清理 | UI 不专业 |

### 11.6 跨模块联动

| 联动点 | 数据源 | 应流向 | 状态 |
|--------|--------|--------|------|
| 门店日结→运营 | daily_settlements | OperationsDashboard 营收卡片 | ❌ |
| 订单→运营 | orders | LiveMonitor 实时订单 | ❌ |
| 会员→运营 | members | DecisionBoard 会员分析 | ❌ |
| 库存→运营 | inventory | AlertCommandCenter 低库存预警 | ❌ |
| 财务→运营 | finance_voucher | 利润分析报表 | ❌ |
| 设备→运营 | device_alerts | AlertCommandCenter 设备预警 | ❌ |

---

## 12. 会员营销域数据生命周期（V3 新增）

### 12.1 数据对象（11 实体）

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 会员等级 | `member_level` | `level_code` (NORMAL/SILVER/GOLD/DIAMOND)、`min_points`、`min_consumption`、`points_rate`、`discount_rate`、`birthday_bonus` |
| 会员 | `members` | `status` (1正常/2冻结/3黑名单/4注销)、`balance`、`r_score`/`f_score`/`m_score`、`customer_segment`、`tags` (JSONB) |
| 积分日志 | `member_points_log` | `change_type` (8 种)、`points`、`balance_after` |
| 优惠券模板 | `coupon_template` | `coupon_type` (5 种)、`status` |
| 会员优惠券 | `member_coupon` | `status` (0未使用/1已使用/2已过期/3已作废) |
| 营销活动 | `marketing_promotion` | `status` (1草稿/2进行中/3已结束/4已暂停/5已作废)、`discount_rule` (JSONB) |
| 充值方案 | `recharge_plan` | `bonus_amount`、`enabled` |
| 充值记录 | `recharge_record` | `payment_status` (0待支付/1已支付/2已退款/3已取消) |
| 积分规则配置 | `points_rule_config` | 13 种预设规则 |
| 营销消息模板 | `marketing_message_template` | `template_type`、`enabled` |
| RFM 计算日志 | `rfm_calculation_log` | `calc_date`、`member_count` |

### 12.2 状态机

#### 12.2.1 会员状态机
```
[1 正常] ──冻结──→ [2 冻结] ──拉黑──→ [3 黑名单]
    ↑                  │                    │
    └──解冻────────────┘                    │
    ↑                                       │
    └──恢复─────────────────────────────────┘
    
任意状态 ──注销──→ [4 注销]（不可恢复）
```

#### 12.2.2 优惠券状态机
```
[0 未使用] ──使用──→ [1 已使用]
       │
       ├──过期──→ [2 已过期]
       └──作废──→ [3 已作废]
```

#### 12.2.3 营销活动状态机
```
[1 草稿] ──开始──→ [2 进行中] ──结束──→ [3 已结束]
                       │
                       ├──暂停──→ [4 已暂停] ──恢复──→ [2 进行中]
                       └──作废──→ [5 已作废]
```

#### 12.2.4 会员等级（4 档）
```
[NORMAL 普通] ──积分达标──→ [SILVER 银卡] ──积分达标──→ [GOLD 金卡] ──积分达标──→ [DIAMOND 钻卡]
```
- 升级条件：`min_points` 或 `min_consumption` 达标
- 权益：`discount_rate`（折扣）、`points_rate`（积分倍率）、`birthday_bonus`（生日积分）

### 12.3 数据流转

```
1. 会员注册
   ├─→ members 表创建（status=1，level=NORMAL）✅
   ├─→ 初始积分 0 ✅
   └─→ RFM 评分初始化 ❌ 当前未自动计算
   ↓
2. 会员充值
   ├─→ recharge_record 创建（payment_status=0）✅
   ├─→ 支付确认（payment_status=1）✅
   ├─→ members.balance += 充值金额 + bonus_amount ✅
   └─→ 触发充值积分（如配置）❌ 未联动
   ↓
3. 会员消费（订单完成时应触发）—— ❌ 整个链路断裂：
   ├─→ members.balance -= 消费金额 ❌ deductBalanceForConsume 是死代码
   ├─→ member_points_log 插入（change_type=consume）❌ 未调用
   ├─→ 优惠券核销（member_coupon.status=1）❌ 未调用
   ├─→ 累计消费金额更新 ❌ 未实现
   ├─→ RFM 评分更新 ❌ 未实现
   └─→ 等级升级检查 ❌ checkAndUpgradeLevel 未调用
   ↓
4. 积分变更（8 种类型）
   ├─→ consume 消费积分 ❌
   ├─→ recharge 充值积分 ❌
   ├─→ signup 注册积分 ✅
   ├─→ birthday 生日积分 ❌
   ├─→ activity 活动积分 ❌
   ├─→ manual 手工调整 ✅
   ├─→ refund 消费退回 ❌
   └─→ expire 过期扣除 ❌
   ↓
5. 等级升级
   ├─→ 触发条件：min_points 或 min_consumption 达标 ❌
   ├─→ members.level_code 更新 ❌
   ├─→ 通知会员 ❌
   └─→ 权益自动生效 ❌
   ↓
6. 优惠券生命周期
   ├─→ coupon_template 创建（管理员）✅
   ├─→ 发放给会员（member_coupon.status=0）✅
   ├─→ 会员使用（status=1）❌ 订单完成未核销
   ├─→ 过期处理（status=2）❌ 无定时任务
   └─→ 作废处理（status=3）❌
   ↓
7. 营销活动生命周期
   ├─→ marketing_promotion 创建（status=1 草稿）✅
   ├─→ 活动开始（status=2）✅
   ├─→ 活动结束（status=3）✅
   ├─→ 活动暂停（status=4）✅
   └─→ 活动作废（status=5）✅
```

### 12.4 严重缺陷

| # | 缺陷 | 影响 |
|---|------|------|
| 1 | **`MarketingMemberMapper.deductBalanceForConsume`（第 67 行）是死代码** | 会员余额支付完全不可用 |
| 2 | **订单完成未调用 `PointsService.earnPointsFromConsume`** | 消费积分链路断裂 |
| 3 | **订单完成未调用 `CouponTemplateService.useCoupon`** | 优惠券核销链路断裂 |
| 4 | **订单完成未调用 `checkAndUpgradeLevel`** | 等级升级链路断裂 |
| 5 | **`member`（旧 POS）vs `members`（CRM）双系统不互通** | 两套会员数据，无法统一管理 |
| 6 | RFM 评分未自动计算 | 客户分群不可用 |
| 7 | 优惠券过期无定时任务 | 过期券仍可用 |
| 8 | 充值未触发积分 | 充值积分规则失效 |

### 12.5 双系统问题

```
旧 POS 系统：member 表
  ├─→ MemberController /v1/pos/members
  └─→ 仅基础字段（姓名/电话/余额）

新 CRM 系统：members 表
  ├─→ MarketingMemberController /v1/members
  └─→ 完整字段（等级/积分/优惠券/RFM/标签）

❌ 两套系统无数据同步机制
❌ 同一会员可能在两张表都存在
❌ 余额可能不一致
```

### 12.6 跨模块联动

| 联动点 | 触发 | 影响 | 状态 |
|--------|------|------|------|
| 订单完成→会员余额 | 订单完成 | balance 扣减 | ❌ 死代码 |
| 订单完成→积分 | 订单完成 | 积分累计 | ❌ |
| 订单完成→优惠券 | 订单完成 | 优惠券核销 | ❌ |
| 订单完成→等级升级 | 订单完成 | 等级检查 | ❌ |
| 会员充值→财务 | 充值确认 | 预收账款凭证 | ❌ |
| 会员消费→财务 | 订单完成 | 营收凭证 | ❌ |
| 营销活动→订单 | 活动进行 | 订单享受优惠 | ❌ |

---

## 13. 设备管理域数据生命周期（V3 新增）

### 13.1 数据对象（8 实体）

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 设备 | `devices` | `device_type` (1打印机/2扫码枪/3称重秤/4取餐柜/5其他)、`status` (0离线/1在线/2故障/3维护)、`connection_type` (4 种) |
| 打印任务 | `print_tasks` | `task_type` (4 种)、`print_status` (0待打印/1打印中/2已打印/3失败) |
| 打印模板 | `print_templates` | `template_type`、`enabled` |
| 设备状态日志 | `device_status_logs` | `event_type` (5 种：online/offline/fault/recover/maintain) |
| 设备告警 | `device_alerts` | `alert_type` (5 种)、`alert_level` (4 级：info/warning/error/critical)、`status` |
| 设备驱动配置 | `device_driver_configs` | `driver_type`、`config` (JSONB) |
| 设备传感器数据 | `device_data` | ⚠️ 表结构在 `sql/device_data_schema.sql` 中存在，含 temperature 字段，但**无 entity/mapper/service** |
| 设备模板 | `device_template` | `template_name`、`template_content` |

### 13.2 状态机

#### 13.2.1 设备状态机
```
[0 离线] ──上线──→ [1 在线] ──故障──→ [2 故障] ──修复──→ [1 在线]
                       │                  │
                       └──维护──→ [3 维护] ──完成──→ [1 在线]
```

#### 13.2.2 打印任务状态机
```
[0 待打印] ──开始打印──→ [1 打印中] ──完成──→ [2 已打印]
                              │
                              └──失败──→ [3 失败] ──重试──→ [1 打印中]
```

#### 13.2.3 设备告警等级
- `info` 信息 / `warning` 警告 / `error` 错误 / `critical` 严重

### 13.3 数据流转

```
1. 设备注册
   ├─→ devices 表创建 ✅
   ├─→ device_type 设定 ✅
   ├─→ connection_type 设定 ✅
   └─→ device_driver_configs 配置 ✅
   ↓
2. 设备上线
   ├─→ devices.status=1 ✅
   ├─→ device_status_logs 插入（event_type=online）✅
   └─→ 心跳检测 ❌ 无超时检测
   ↓
3. 打印任务（应自动触发）
   ├─→ print_tasks 创建（print_status=0）✅
   ├─→ 关联订单/小票/标签 ✅
   ├─→ 分配打印机 ❌ 当前无自动分配
   ├─→ 打印执行（print_status=1→2）✅
   └─→ 失败重试（print_status=3）✅
   ↓ 订单完成时应触发打印 ❌ 当前未联动：
   ├─→ 订单完成 → 自动打印小票 ❌ OrderService 未调用
   ├─→ 厨房接单 → 自动打印厨房单 ❌ KitchenOrderService 未调用
   └─→ 出菜 → 自动打印叫号单 ❌ 未实现
   ↓
4. 设备告警
   ├─→ device_alerts 插入 ✅
   ├─→ 告警分级（4 级）✅
   ├─→ 通知推送 ❌ 无 WebSocket 推送
   └─→ 告警处理 ❌ 无处理流程
   ↓
5. 传感器数据采集
   ├─→ device_data 表（schema 已建）⚠️
   ├─→ 温度/湿度等数据采集 ❌ 无 entity/mapper/service
   └─→ 数据分析报表 ❌
```

### 13.4 严重缺陷

| # | 缺陷 | 影响 |
|---|------|------|
| 1 | **12+ Controller 混乱重复** | DeviceController/DeviceManagementController/HardwareDeviceController/ScanDeviceController/WeighingDeviceController/SdkPrinterController/PrintFormatTemplateController/LabelPrinterController/LabelTemplateController/HardwareConfigController/HardwareConfigVersionController/TTSController/EpsonPrinterTestController/DeviceSimulatorController/DeviceWebSocketController/DeviceCommunicationTestController |
| 2 | **前端 deviceType 与后端不匹配** | 前端：制冷/烹饪/通风/收银；后端：1打印机/2扫码枪/3称重秤/4取餐柜/5其他 |
| 3 | **`device_data` 表已建未启用** | schema 在 sql/device_data_schema.sql，但无 entity/mapper/service |
| 4 | **PrintTask 未被订单触发** | OrderService/KitchenOrderService 未调用打印 |
| 5 | **无心跳超时检测** | 设备离线无法自动发现 |
| 6 | **无 WebSocket 推送** | 告警无法实时通知 |
| 7 | **无设备指令下发** | 无法远程控制设备 |
| 8 | **前端仅 `DeviceList.vue` 4 条 Mock** | 设备管理不可用 |

### 13.5 跨模块联动

| 联动点 | 触发 | 影响 | 状态 |
|--------|------|------|------|
| 订单→打印 | 订单完成 | 自动打印小票 | ❌ |
| 厨房→打印 | 厨房接单 | 自动打印厨房单 | ❌ |
| 设备告警→运营 | 告警产生 | AlertCommandCenter 预警 | ❌ |
| 设备状态→工作台 | 设备离线 | Dashboard 预警 | ❌ |
| 传感器→库存 | 温度异常 | 冷链库存预警 | ❌ |

---

## 14. 电子签章域数据生命周期（V3 新增）

> ⚠️ **本模块后端完全缺失**——前端 100% Mock，API 路径虚构，无数据库表。

### 14.1 数据对象（应为但不存在）

| 应有实体 | 应有表名 | 当前状态 |
|---------|---------|---------|
| 印章档案 | `seals` | ❌ 表不存在 |
| 用印申请 | `seal_usage` | ❌ 表不存在 |
| 签署请求 | `sign_requests` | ❌ 表不存在 |
| 电子签名记录 | `electronic_signature` | ⚠️ 表存在但语义混乱 |

### 14.2 前端定义的类型（无后端实现）

```typescript
// 前端 types/seal.ts 定义完整，但无后端支撑
SealType: official(公章) | finance(财务章) | contract(合同章) | legal(法人章) | custom(自定义)
SealStatus: active | inactive | revoked
SealScene: hr_contract | purchase_contract | electronic_contract
```

### 14.3 当前实现状态

#### 14.3.1 `electronic_signature` 表（语义混乱）
- **表名暗示**：电子签名记录
- **实际用途**： unclear
- **问题**：`postgres-schema-v0.12.sql` 中的定义与实际实体字段不匹配

#### 14.3.2 `ElectronicSignatureController`（`/hr/signature`）
- ❌ 公司印章存储在**内存变量** `companySealData` 中，**重启丢失**
- ❌ 仅提供查询端点，无写入端点
- ❌ 无用印记录、无审批流程

#### 14.3.3 `ElectronicContractController`（`/v1/purchase/electronic-contracts`）
- `signElectronicContract` 方法仅记录 `signUrl` 字段
- ❌ 不调用印章
- ❌ 不记录用印日志
- ❌ 不验证签署人权限

#### 14.3.4 `OfdSignatureVerifyService.java`
- ✅ 服务实现存在
- ❌ **无 Controller 暴露**，前端无法调用

#### 14.3.5 前端实现
- `SealManagement.vue` / `SealFormDialog.vue` / `SealUsageLogDialog.vue` — **100% Mock**
- 数据来源：`api/seal/mockData.ts`
- API 路径：`/v1/seals` — **虚构，后端不存在**

### 14.4 应有数据流转

```
1. 印章档案管理
   ├─→ seals 表创建（印章类型/持有人/有效期）❌
   ├─→ 印章图片上传（OSS）❌
   └─→ 印章授权（角色/用户）❌
   ↓
2. 用印申请
   ├─→ seal_usage 表创建（申请人/用途/文件）❌
   ├─→ 审批流程 ❌
   └─→ 审批通过 → 触发用印 ❌
   ↓
3. 电子签署
   ├─→ sign_requests 表创建 ❌
   ├─→ 调用 OFD 签署服务 ⚠️ OfdSignatureVerifyService 存在但未暴露
   ├─→ 记录签署日志 ❌
   └─→ 生成签署后文档 ❌
   ↓
4. 用印记录
   ├─→ seal_usage 表更新（status=used）❌
   ├─→ 关联签署文档 ❌
   └─→ 归档 ❌
```

### 14.5 严重缺陷

| # | 缺陷 | 影响 |
|---|------|------|
| 1 | **无 seals 表** | 印章档案无法管理 |
| 2 | **无 `/v1/seals` Controller** | 前端 API 路径虚构 |
| 3 | **公司印章存内存变量** | 重启丢失，无法审计 |
| 4 | **`electronic_signature` 表语义混乱** | 表名与实际用途不符 |
| 5 | **`OfdSignatureVerifyService` 未暴露** | OFD 验签服务不可用 |
| 6 | **采购电子合同不调用印章** | 签署无法律效力 |
| 7 | **无用印审批流程** | 用印无管控 |
| 8 | **前端 100% Mock** | 整个模块不可用 |

### 14.6 跨模块联动（应为）

| 联动点 | 触发 | 影响 | 状态 |
|--------|------|------|------|
| HR 合同→签章 | 合同签署 | 调用电子签章 | ❌ |
| 采购合同→签章 | 合同签署 | 调用电子签章 | ⚠️ 仅记录 signUrl |
| 财务凭证→签章 | 凭证审核 | 出纳/审核/记账多人签章 | ❌ |
| 财务报表→签章 | 报表归档 | 负责人+法人签章 | ❌ |

---

## 15. 系统管理域数据生命周期（V3 新增）

### 15.1 数据对象（20+ 实体）

| 实体 | 表名 | 关键状态字段 |
|------|------|-------------|
| 用户 | `users` | `status`、`mfa_enabled` |
| 角色 | `roles` | `status`、`data_scope` (ALL/COMPANY/DEPARTMENT/STORE/STORES/SELF) |
| 权限 | `permissions` | `permission_type` (menu/button/api/data) |
| 角色权限 | `role_permissions` | — |
| 用户角色 | `user_roles` | — |
| 角色门店 | `role_stores` | `store_id`（数据权限-门店范围） |
| 角色部门 | `role_departments` | `department_id`（数据权限-部门范围） |
| 权限模板 | `permission_templates` | `template_code` |
| 权限分配日志 | `permission_assignment_logs` | `operation_type` |
| 菜单 | `menus` | ⚠️ 表存在但**无 MenuController** |
| 字典 | `sys_dict` | `dict_code`、`enabled` |
| 字典项 | `sys_dict_item` | `dict_id`、`value`、`sort` |
| 系统配置 | `sys_config` | `config_key`、`config_value`、`value_type`、`version`（乐观锁） |
| 配置历史 | `sys_config_history` | `old_value`、`new_value`、`operator` |
| 审计日志 | `audit_logs` | `operation_type` (9 种)、`risk_level` (4 级) |
| 操作日志 | `operation_logs` | — |
| 登录日志 | `login_logs` | `login_result`、`ip` |
| 密码策略 | `password_policies` | `min_length`、`complexity` |
| 密码历史 | `password_history` | `user_id`、`password_hash` |
| 密码重置 | `password_reset_logs` | `request_time`、`reset_time` |
| 部门 | `departments` | `parent_id`（树形） |
| 门店 | `stores` | `status`、`scale_level` |

### 15.2 RBAC 数据模型

```
用户 users
  ↓ user_roles（多对多）
角色 roles
  ↓ role_permissions（多对多）
权限 permissions
  │
  ├─→ menu 菜单权限
  ├─→ button 按钮权限
  ├─→ api API 权限
  └─→ data 数据权限

数据权限：
  roles.data_scope
    ├─→ ALL: 全部数据
    ├─→ COMPANY: 全公司
    ├─→ DEPARTMENT: 部门
    ├─→ STORE: 单门店
    ├─→ STORES: 多门店（role_stores 表）
    └─→ SELF: 仅自己
```

### 15.3 菜单管理（双轨问题）

```
前端：src/modules/*/menu.ts（15 个文件硬编码）
  ├─→ workspaceMenu / productMenu / orderMenu / ...
  ├─→ registerAllMenus() 统一注册
  └─→ 按 scaleLevel 过滤显示

后端：menus 表（存在但无 Controller）
  ├─→ 表结构完整
  ├─→ ❌ 无 MenuController
  ├─→ ❌ 无 MenuService
  └─→ ❌ 与前端完全不同步

❌ 双轨割裂：前端菜单与后端 menus 表无任何同步机制
```

### 15.4 字典管理

```
后端：SysDictController /v1/dict
  ├─→ GET /v1/dict/code/{dictCode}/items ✅
  ├─→ 字典 CRUD ✅
  └─→ 字典项 CRUD ✅

前端业务页面：
  ❌ 硬编码状态映射（如 status === 'active' ? '在职' : '离职'）
  ❌ 不调用字典 API

❌ 字典表已实现但业务页面不用，导致：
  - 状态文案修改需改代码
  - 多语言无法实现
  - 字典管理形同虚设
```

### 15.5 系统配置

```
sys_config 表
  ├─→ config_key / config_value
  ├─→ value_type (string/number/boolean/json)
  ├─→ version（乐观锁）✅
  └─→ description

sys_config_history 表
  ├─→ 记录每次配置变更
  ├─→ old_value / new_value
  └─→ operator / operate_time

✅ 配置管理后端完整
✅ 配置变更可审计
```

### 15.6 审计日志

```
@AuditLog 注解 + AOP 切面
  ↓
audit_logs 表插入
  ├─→ operation_type (9 种):
  │     LOGIN / CREATE / UPDATE / DELETE / EXPORT / VIEW / AUTH / SYSTEM / SECURITY
  ├─→ risk_level (4 级):
  │     LOW / MEDIUM / HIGH / CRITICAL
  ├─→ operator / operate_time / ip
  └─→ target_type / target_id / details (JSONB)

前端 OperationAudit.vue:
  ❌ 使用 generateMockData() 全 Mock
  ❌ 未调用审计日志 API
```

### 15.7 AI 模型配置（无后端）

```
前端：views/system/AIModelConfig.vue
  ├─→ 100% Mock
  ├─→ 3 个硬编码模型（GPT/Claude/Local）
  └─→ 字段: modelType/endpoint/apiKey/confidence/modelPath/timeout

后端：
  ❌ 无 ai_model_config 表
  ❌ 无 AIModelConfigController
  ❌ 无 AIModelConfigService

❌ 整个 AI 模型配置无后端支撑
```

### 15.8 严重缺陷

| # | 缺陷 | 影响 |
|---|------|------|
| 1 | **菜单双轨：前端 menu.ts vs 后端 menus 表无同步** | 菜单管理形同虚设 |
| 2 | **AI 模型配置无后端** | AI 功能无法落地 |
| 3 | **字典已实现但业务页硬编码状态映射** | 字典管理无效 |
| 4 | **审计日志前端全 Mock** | 审计功能不可用 |
| 5 | **密码策略表已建但未强制执行** | 密码安全风险 |
| 6 | **数据权限未在所有查询中生效** | 数据越权风险 |

### 15.9 跨模块联动

| 联动点 | 触发 | 影响 | 状态 |
|--------|------|------|------|
| 用户登录→审计 | 登录成功/失败 | audit_logs 记录 | ✅ |
| 权限变更→审计 | 角色权限调整 | audit_logs 记录 | ✅ |
| 数据权限→业务查询 | 查询业务数据 | 按 data_scope 过滤 | ⚠️ 仅运营报表实现 |
| 菜单→前端显示 | 用户登录 | 按权限显示菜单 | ⚠️ 仅前端 menu.ts |
| 字典→业务页 | 状态显示 | 调用字典 API | ❌ 业务页硬编码 |
| AI 配置→仓储 | 智能补货 | 读取 AI 模型配置 | ❌ 无后端 |
| AI 配置→财务 | AI 财务助手 | 读取 AI 模型配置 | ❌ 无后端 |

---

## 16. 跨模块数据联动完整矩阵（V3 扩展）

### 16.1 已实现的联动（✅）

| # | 联动 | 触发 | 影响 |
|---|------|------|------|
| 1 | 采购入库→溯源批次 | 采购收货 | trace_batch 创建 |
| 2 | 采购入库→库存增加 | 采购收货 | inventory.quantity += |
| 3 | 销售出库→溯源码状态 | 销售出库 | trace_code.status=used |
| 4 | 付款→四账联动 | 付款确认 | payable/bank/flow/voucher 同步 |
| 5 | 收款→四账联动 | 收款确认 | receivable/bank/flow/voucher 同步 |
| 6 | 资产折旧→折旧记录 | 月末计提 | asset_depreciation 插入 |
| 7 | 入职→员工档案创建 | 完成入职 | Employee 创建 |
| 8 | 健康证→到期预警 | 定时任务 | 预警通知 |
| 9 | 叫号→排队记录 | 顾客取号 | call_number_queue 插入 |
| 10 | 桌台→订单关联 | 创建订单 | dining_tables.current_order_id |
| 11 | 订单→厨房工单 | 订单确认 | kitchen_orders 创建 |
| 12 | 会员注册→初始积分 | 注册成功 | signup 积分 |
| 13 | 用户登录→审计日志 | 登录成功 | audit_logs 记录 |
| 14 | 数据权限→运营报表 | 查询报表 | 按 storeIds 过滤 |

### 16.2 断链/未实现的联动（❌）

| # | 联动 | 触发 | 应影响 | 当前状态 |
|---|------|------|--------|---------|
| 1 | 招聘→入职 | 录用 | 创建 OnboardingRecord | 前端不调用后端 |
| 2 | 门店招聘→入职 | 审批通过 | 生成 OnboardingRecord | 无自动生成 |
| 3 | 入职→门店 | 创建 Employee | Employee.storeId 设置 | storeId 丢失 |
| 4 | 考勤→薪资 | 月度汇总 | 薪资计算 | 未联动 |
| 5 | 薪资→财务成本 | 薪资确认 | finance_cost_record | 未联动 |
| 6 | 门店日结→财务凭证 | 日结审核 | 营收凭证（E01-E04） | 未触发 |
| 7 | 门店日结→运营中心 | 日结完成 | OperationsDashboard 数据 | 6/7 页 Mock |
| 8 | 门店物资→采购 | 物资提报 | MaterialRequest 创建 | 分离不联动 |
| 9 | 采购收货→应付账款 | 收货确认 | finance_payable 创建 | 未实现 |
| 10 | 采购结算→付款 | 结算确认 | finance_payment 创建 | 未实现 |
| 11 | 销售出库→应收账款 | 出库确认 | finance_receivable 创建 | 未实现 |
| 12 | 订单完成→库存扣减 | 订单完成 | inventory.quantity -= | 未实现 |
| 13 | 订单完成→桌台释放 | 订单完成 | dining_tables.status=4 | 未联动 |
| 14 | 订单完成→会员积分 | 订单完成 | member_points_log 插入 | 未调用 |
| 15 | 订单完成→优惠券核销 | 订单完成 | member_coupon.status=1 | 未调用 |
| 16 | 订单完成→应收账款 | 订单完成 | finance_receivable 创建 | 未联动 |
| 17 | 订单完成→溯源码 | 订单完成 | trace_code.status=used | 未联动 |
| 18 | 订单完成→日结累加 | 订单完成 | daily_settlements 累加 | 未联动 |
| 19 | 订单完成→菜品销量 | 订单完成 | foods.sold_today++ | 未实现 |
| 20 | 订单完成→打印小票 | 订单完成 | print_tasks 创建 | 未联动 |
| 21 | 会员余额扣减 | 订单支付 | members.balance -= | 死代码 |
| 22 | 会员等级升级 | 消费达标 | members.level_code 更新 | 未调用 |
| 23 | 会员充值→积分 | 充值确认 | 充值积分 | 未联动 |
| 24 | 会员消费→财务 | 订单完成 | 营收凭证 | 未联动 |
| 25 | 资产折旧→凭证 | 月末计提 | 折旧凭证（E17） | 未联动 |
| 26 | 资产购置→凭证 | 资产采购 | 购置凭证（E16） | 未联动 |
| 27 | 智能补货→采购申请 | 补货建议 | purchase_request 创建 | 未联动 |
| 28 | BOM→菜品成本 | BOM 发布 | 标准成本计算 | 未联动 |
| 29 | 菜品销售→溯源码 | 菜品销售 | 预生成溯源码 | 未联动 |
| 30 | HR 合同→电子签章 | 合同签署 | 调用签章 | 未实现 |
| 31 | 采购合同→电子签章 | 合同签署 | 调用签章 | 仅记录 signUrl |
| 32 | 设备告警→运营预警 | 告警产生 | AlertCommandCenter | 未联动 |
| 33 | 字典→业务页状态 | 状态显示 | 调用字典 API | 业务页硬编码 |
| 34 | AI 配置→智能补货 | 补货计算 | 读取 AI 配置 | 无后端 |
| 35 | 跨模块事件总线 | 各类事件 | 14 类事件触发 | 未实现 |

### 16.3 双系统割裂（⚠️）

| # | 割裂点 | 系统 A | 系统 B | 后果 |
|---|--------|--------|--------|------|
| 1 | 招聘 | HR 招聘（type='hr'） | 门店招聘（type='store'） | 招聘数据两套 |
| 2 | 考勤 | HR 考勤模块 | 门店排班考勤 | 考勤数据两套 |
| 3 | 健康证 | HR 健康证管理 | 门店员工档案 | 健康证预警失效 |
| 4 | 会员 | member（旧 POS） | members（CRM） | 会员数据两套 |
| 5 | 设备类型 | 前端（制冷/烹饪/通风/收银） | 后端（打印机/扫码枪/称重秤/取餐柜/其他） | 设备分类不匹配 |
| 6 | 菜单 | 前端 menu.ts（15 文件） | 后端 menus 表（无 Controller） | 菜单管理失效 |

---

## 17. 数据状态字段统一规范

### 17.1 通用状态字段

| 业务含义 | 前端值 | 后端值 | 数据库值 |
|---------|--------|--------|----------|
| 启用/正常/在职 | `active` | 1 | 1 |
| 禁用/停用/离职 | `inactive` | 0 | 0 |
| 试用/审核中 | `probation` | 2 | 2 |

### 17.2 各模块专属状态

| 模块 | 实体 | 字段 | 状态值 |
|------|------|------|--------|
| 财务 | finance_voucher | voucher_status | 0暂存/1已审核/2已过账/3已作废 |
| 财务 | finance_payable | status | unpaid/partially_paid/paid |
| 财务 | finance_receivable | status | unreceived/partially_received/received |
| 财务 | finance_payment | status | draft/confirmed/void |
| 财务 | finance_budget | status | draft/approved/executing/closed |
| 财务 | export_tasks | status | 0待处理/1处理中/2已完成/3失败 |
| 采购 | purchase_request | status | draft/submitted/approved/rejected/closed |
| 采购 | purchase_order | status | draft/confirmed/received/partially_received/closed |
| 采购 | purchase_stockin | status | draft/received/quality_checked/returned |
| 采购 | purchase_settlement | status | draft/confirmed/paid |
| 仓储 | stock_count | status | draft/counting/counted/approved |
| 仓储 | stock_transfer | status | draft/transferring/completed/cancelled |
| 仓储 | stock_in/out | status | draft/in(out)/approved |
| 订单 | orders | status | 0待确认/1已确认/2已完成/3已取消/4部分退款/5全额退款/6待评价 |
| 订单 | order_items | kitchen_status | 0待接单/1制作中/2已出菜/3已上桌/4已退菜 |
| 订单 | order_refund_records | refund_status | 0申请/1审核中/2已退款/3已拒绝 |
| 订单 | dining_tables | status | 1空闲/2占用/3用餐中/4待清台/5保留 |
| 产品 | foods | status | 1在售/2停售/3售罄 |
| 产品 | dish_recipes | version | draft/published/archived |
| 会员 | members | status | 1正常/2冻结/3黑名单/4注销 |
| 会员 | member_coupon | status | 0未使用/1已使用/2已过期/3已作废 |
| 会员 | marketing_promotion | status | 1草稿/2进行中/3已结束/4已暂停/5已作废 |
| 会员 | recharge_record | payment_status | 0待支付/1已支付/2已退款/3已取消 |
| 设备 | devices | status | 0离线/1在线/2故障/3维护 |
| 设备 | devices | device_type | 1打印机/2扫码枪/3称重秤/4取餐柜/5其他 |
| 设备 | print_tasks | print_status | 0待打印/1打印中/2已打印/3失败 |
| 资产 | asset | status | in_use/idle/scrapped/repairing |
| 溯源 | trace_code | status | active/used/recalled |
| 系统 | roles | data_scope | ALL/COMPANY/DEPARTMENT/STORE/STORES/SELF |
| 系统 | audit_logs | operation_type | LOGIN/CREATE/UPDATE/DELETE/EXPORT/VIEW/AUTH/SYSTEM/SECURITY |
| 系统 | audit_logs | risk_level | LOW/MEDIUM/HIGH/CRITICAL |

### 17.3 金额单位规范

- 后端/数据库：**分**（Long/BigInt）
- 前端：**元**（string/number）
- 转换：`yuanToFen("123.45")` → 12345；`fenToYuanNumber(12345)` → 123.45
- ❌ 禁止本地实现 `formatFen`，必须用 `utils` 统一函数

---

## 18. 事务边界与一致性保障

### 18.1 事务边界规范

| 操作类型 | 事务范围 | 隔离级别 |
|---------|---------|---------|
| 四账联动（付款/收款） | 单事务：payable + bank + flow + voucher | READ_COMMITTED |
| 采购收货 | 单事务：stockin + inventory + stock_movement + trace_batch | READ_COMMITTED |
| 订单完成（应为） | 单事务：order + inventory + table + points + coupon + receivable + trace | READ_COMMITTED |
| 凭证过账 | 单事务：voucher + subject_balance | READ_COMMITTED |
| 资产折旧 | 单事务：asset_depreciation + 凭证（应为） | READ_COMMITTED |

### 18.2 一致性保障机制

1. **`@Transactional(rollbackFor = Exception.class)`** 强制回滚
2. **`@Version` 乐观锁**：sys_config、products 等并发修改场景
3. **幂等控制**：`voucher_idempotent` 表防止自动记账重复
4. **缓存一致性**：先清 L1 Caffeine，再清 L2 Redis
5. **审计轨迹**：auto_voucher_log / permission_assignment_logs / sys_config_history

### 18.3 当前事务风险

| 风险 | 位置 | 影响 |
|------|------|------|
| 订单完成未事务化 | OrderService.complete() | 多表操作可能部分成功 |
| 门店日结未事务化 | 日结审核 | 凭证生成与日结状态可能不一致 |
| 会员余额扣减死代码 | deductBalanceForConsume | 即使调用也无事务保障 |

---

## 19. 缓存生命周期

### 19.1 二级缓存结构

```
请求 → L1 本地缓存(Caffeine) → L2 Redis → 数据库
```

### 19.2 缓存键格式

- 格式：`{entity}:basic:{entityId}`
- 示例：`employee:basic:1234567890`、`food:basic:789`、`member:basic:456`

### 19.3 缓存过期时间

| 数据类型 | 默认过期 | 说明 |
|---------|---------|------|
| 用户信息 | 30 分钟 | 变更频率较高 |
| 权限信息 | 1 小时 | 角色权限调整时清除 |
| 配置信息 | 24 小时 | 变更频率低 |
| 基本信息 | 24 小时 | 部门/职位等基础数据 |
| 静态数据 | 7 天 | 字典/枚举等 |
| 菜品信息 | 1 小时 | 价格/状态变更时清除 |
| 会员信息 | 30 分钟 | 余额/积分变更时清除 |

### 19.4 缓存更新策略

- 数据更新/删除时清除对应缓存
- 批量操作时批量清除缓存
- 清除顺序：先清 L1，再清 L2
- 过期时间添加随机偏移量（±10%），避免缓存雪崩

### 19.5 当前缓存缺陷

- ❌ 部分更新操作未清除 L1+L2 缓存
- ❌ 菜品价格变更未清除订单缓存
- ❌ 会员余额变更未清除会员缓存

---

## 20. 数据归档与删除策略

### 20.1 逻辑删除（业务核心数据）

| 表 | 删除方式 | 字段 |
|----|---------|------|
| employees | 逻辑删除 | `deleted` (0/1) |
| orders | 逻辑删除 | `deleted` (0/1) |
| finance_voucher | 逻辑删除 | `deleted` (0/1) |
| foods | 逻辑删除 | `deleted` (0/1) |
| members | 逻辑删除 | `deleted` (0/1) |
| 所有业务核心表 | 逻辑删除 | `deleted` (0/1) |

### 20.2 物理删除（临时/日志/缓存类数据）

| 表 | 删除方式 | 说明 |
|----|---------|------|
| 验证码 | 物理删除 | 过期自动删除 |
| 操作日志 | 物理删除 | 按周期归档后删除 |
| 会话记录 | 物理删除 | 过期自动删除 |
| 临时文件 | 物理删除 | 按周期清理 |

### 20.3 归档策略（应为但未实现）

| 数据类型 | 归档周期 | 归档方式 | 当前状态 |
|---------|---------|---------|---------|
| 凭证 | 按年归档 | 移至 archive 表 | ❌ 未实现 |
| 流水 | 按月归档 | 移至 archive 表 | ❌ 未实现 |
| 考勤记录 | 按年归档 | 移至 archive 表 | ❌ 未实现 |
| 库存流水 | 按月归档 | 移至 archive 表 | ❌ 未实现 |
| 审计日志 | 按年归档 | 移至 archive 表 | ❌ 未实现 |
| 离职员工档案 | 离职后归档 | 移至 employee_archive | ❌ 未实现 |

### 20.4 数据保留期限（合规要求）

| 数据类型 | 保留期限 | 依据 |
|---------|---------|------|
| 会计凭证 | 30 年 | 会计法 |
| 会计账簿 | 30 年 | 会计法 |
| 财务报表 | 10 年 | 会计法 |
| 银行流水 | 15 年 | 银行规定 |
| 员工档案 | 离职后 5 年 | 劳动法 |
| 考勤记录 | 2 年 | 劳动法 |
| 食品溯源记录 | 2 年 | 食品安全法 |
| 审计日志 | 3 年 | 信息安全要求 |

---

## 21. 当前数据流转的缺陷汇总（V3 全模块）

### 21.1 严重缺陷（P0 阻塞商用）

| # | 缺陷 | 模块 | 影响 | 优先级 |
|---|------|------|------|--------|
| 1 | HR 招聘前置链路 Mock 化 | HR | HR 招聘全流程不可用 | P0 |
| 2 | 录用→入职→员工档案链路断裂 | HR | 招聘漏斗到入职环节断裂 | P0 |
| 3 | 门店招聘→入职不联动 | HR/门店 | 门店审批通过的招聘无入职闭环 | P0 |
| 4 | 门店经营数据无法流向运营中心 | 门店/运营 | 运营决策无数据支撑 | P0 |
| 5 | 订单数据无法流向门店日结与运营报表 | 订单/门店/运营 | 财务对账无真实订单依据 | P0 |
| 6 | 采购收货 → 应付账款联动未实现 | 采购/财务 | 应付账款无法自动生成 | P0 |
| 7 | 销售出库 → 应收账款联动未实现 | 仓储/财务 | 应收账款无法自动生成 | P0 |
| 8 | 门店日结 → 财务凭证未实现 | 门店/财务 | 营收凭证需手工录入 | P0 |
| 9 | 凭证过账未更新科目余额 | 财务 | 总账不准确 | P0 |
| 10 | 付款/收款的 withMockFallback 掩盖真实失败 | 全局 | 商用后数据不一致 | P0 |
| 11 | 订单完成未触发库存/桌台/积分/优惠券/应收/溯源/日结/销量 | 订单/全模块 | 订单完成链路 8 处断裂 | P0 |
| 12 | 会员余额扣减死代码 | 会员/订单 | 会员余额支付不可用 | P0 |
| 13 | 会员等级升级未调用 | 会员 | 等级升级链路断裂 | P0 |
| 14 | 电子签章无后端 | 签章 | 整个模块不可用 | P0 |
| 15 | 订单前端 100% Mock（4 页） | 订单 | 订单管理不可用 | P0 |
| 16 | 运营中心 6/7 页 Mock | 运营 | 运营决策不可用 | P0 |
| 17 | 工作台前端 100% Mock（后端已建未调用） | 工作台 | Dashboard 不可用 | P0 |

### 21.2 中等缺陷（P1 严重影响体验）

| # | 缺陷 | 模块 | 影响 | 优先级 |
|---|------|------|------|--------|
| 18 | HR 与门店双系统割裂（招聘/考勤/健康证） | HR/门店 | 同一应聘者可能重复建档 | P1 |
| 19 | 门店物资需求与采购模块分离 | 门店/采购 | 物资需求未流转到采购审批 | P1 |
| 20 | 资产折旧未自动生成凭证 | 资产/财务 | 折旧数据脱节 | P1 |
| 21 | 薪资确认未生成成本记录和凭证 | HR/财务 | 人工成本缺失 | P1 |
| 22 | 智能补货建议未联动采购申请 | 仓储/采购 | 补货建议无法自动转采购 | P1 |
| 23 | 离职流程完全缺失 | HR | 离职无闭环 | P1 |
| 24 | 异动管理缺失 | HR | 调岗/晋升/调薪无审批 | P1 |
| 25 | 溯源召回未通知相关方 | 溯源 | 召回流程不完整 | P1 |
| 26 | Employee.storeId 前端未展示 | HR | 员工门店归属丢失 | P1 |
| 27 | 财务专业缺失（电子凭证/管理报表/合并报表/税务/AI） | 财务 | 财务模块专业度不足 | P1 |
| 28 | 会员 member/members 双系统不互通 | 会员 | 会员数据两套 | P1 |
| 29 | 设备 12+ Controller 混乱重复 | 设备 | 设备管理不可维护 | P1 |
| 30 | 设备 device_data 表已建未启用 | 设备 | 传感器数据采集不可用 | P1 |
| 31 | 设备前端 deviceType 与后端不匹配 | 设备 | 设备分类混乱 | P1 |
| 32 | 订单完成未触发打印小票 | 订单/设备 | 小票需手工打印 | P1 |
| 33 | 菜单管理双轨（前端 menu.ts vs 后端 menus 表无 Controller） | 系统 | 菜单管理形同虚设 | P1 |
| 34 | AI 模型配置无后端 | 系统 | AI 功能无法落地 | P1 |
| 35 | 字典已实现但业务页硬编码状态映射 | 系统 | 字典管理无效 | P1 |
| 36 | 审计日志前端全 Mock | 系统 | 审计功能不可用 | P1 |
| 37 | foodCode 前端生成 | 产品 | 编码规则不统一 | P1 |
| 38 | BOM 未自动计算菜品成本 | 产品/财务 | 成本核算无依据 | P1 |
| 39 | 跨模块事件总线 14 类未实现 | 全局 | 跨模块事件无法触发 | P1 |
| 40 | 14+ 处 teleported 违规 | 多模块 | Dialog 内弹出漂移 | P1 |
| 41 | 7+ 处本地 formatFen | 多模块 | 金额精度风险 | P1 |

### 21.3 数据一致性风险

1. **金额单位混用**：7+ 处本地 `formatFen` 函数，存在精度风险
2. **状态码不一致**：HR 与门店招聘状态命名不一致（screening vs reviewing）
3. **缓存未清理**：部分更新操作未清除 L1+L2 缓存
4. **事务边界过宽**：部分 Service 方法事务范围过大，存在长事务风险
5. **storeId 丢失**：OnboardingRecordServiceImpl.createEmployeeProfile() 未设置 Employee.storeId
6. **会员余额双系统**：member.balance 与 members.balance 可能不一致
7. **菜单双轨**：前端 menu.ts 与后端 menus 表无同步
8. **设备类型不匹配**：前端 deviceType 枚举与后端 device_type 数值不对应

---

## 22. 数据流优化建议

### 22.1 短期（1-2 周）— 打通核心业务链路

1. **HR 招聘前置链路对接**：移除 `api/hr/recruitment.ts` 的 Mock，调用后端 RecruitmentRequirementController/ResumeController/InterviewController
2. **录用→入职联动**：HR 前端 handleOnboardHire() 改为调用后端 OnboardingRecordController
3. **门店招聘→入职联动**：审批通过后自动创建 OnboardingRecord
4. **修复 storeId 丢失**：OnboardingRecordServiceImpl.createEmployeeProfile() 设置 Employee.storeId
5. **移除所有 withMockFallback**：强制真实 API 对接
6. **实现采购收货 → 应付账款联动**
7. **实现销售出库 → 应收账款联动**
8. **实现门店日结 → 财务凭证**（触发 E01-E04）
9. **实现凭证过账 → 科目余额更新**
10. **订单前端对接后端**：创建 `api/order/` 目录，4 页对接 OrderNewController
11. **工作台前端对接后端**：Dashboard 页调用 DashboardController 7 个端点
12. **运营中心 6 页对接后端**：移除 Mock，调用 OperationsReportController

### 22.2 中期（1-2 月）— 打通跨模块孤岛与订单完成链路

1. **订单完成链路打通**（8 处联动）：
   - 库存扣减、桌台释放、会员积分、优惠券核销、应收生成、溯源码变更、日结累加、菜品销量++
2. **会员消费链路打通**：
   - 移除 deductBalanceForConsume 死代码，改为正常调用
   - 订单完成调用 PointsService.earnPointsFromConsume
   - 订单完成调用 CouponTemplateService.useCoupon
   - 订单完成调用 checkAndUpgradeLevel
3. **运营中心对接门店真实数据**：OperationsDashboard/DecisionBoard/LiveMonitor 改为调用门店 API
4. **统一 HR 与门店双系统**：招聘/考勤/健康证合并为单一数据源
5. **统一会员双系统**：member 表迁移至 members 表，废弃 MemberController
6. **实现资产折旧 → 凭证自动生成**（E17）
7. **实现薪资确认 → 成本记录和凭证生成**（E09-E10）
8. **实现智能补货 → 采购申请联动**
9. **完善溯源召回通知机制**
10. **电子签章后端建设**：新建 seals/seal_usage/sign_requests 表，新建 SealController，公司印章持久化
11. **设备模块重构**：合并 12+ Controller 为统一的 DeviceController，启用 device_data 表
12. **菜单管理统一**：新建 MenuController，前端 menu.ts 改为从后端加载（保留前端 fallback）

### 22.3 长期（3-6 月）— 财务专业完善与系统治理

1. **电子凭证 UI**：补齐电子凭证上传/解析/验签/入账闭环
2. **管理报表**：管理利润表、分门店/分渠道利润表、单品盈利分析
3. **合并报表**：合并资产负债表/利润表/现金流量表、内部交易抵消
4. **税务报表**：增值税/企业所得税/个税申报表
5. **AI 接入**：
   - 新建 ai_model_config 表 + Controller + Service
   - 全局 AI 财务助手抽屉
   - 仓储智能补货对接真实 AI 模型
6. **离职流程**：离职申请/审批/交接/离职证明/档案归档
7. **异动管理**：调岗/晋升/调薪审批流
8. **数据归档机制**：凭证/流水/考勤/库存流水按周期归档
9. **跨模块事件总线**：实现 14 类跨模块事件
10. **完善缓存预热和雪崩防护**
11. **字典联动**：业务页状态映射改为调用字典 API
12. **审计日志对接**：OperationAudit.vue 调用真实审计 API
13. **数据一致性校验任务**：定时校验 member/members、菜单前后端、设备类型等

---

## 19. 站内通信/消息中心系统（V3 补充 — 用户重点关注）

> **背景**：用户在 2026-06-25 反馈"缺少站内的通信"。代码库审查发现：站内通信基础设施已**远比预期完整**——Controller/实体/RabbitMQ 消费者/定时任务/WebSocket 推送/前端通知中心/API 封装**均已存在**，但**未与各业务模块深度联动**，且 4 渠道中仅 EMAIL 真实可用。

### 19.1 已有基础设施盘点（代码库实际状态）

#### 19.1.1 后端组件清单

| # | 组件 | 路径 | 状态 | 说明 |
|---|------|------|------|------|
| 1 | NotificationController | `controller/NotificationController.java` | ✅ 25 个端点 | 模板/发送/记录/设置/统计/站内通知/偏好 |
| 2 | Notification 实体 | `entity/Notification.java` | ✅ | 站内通知主表（含业务关联/优先级/已读/版本号） |
| 3 | MsgTemplate 实体 | `entity/MsgTemplate.java` | ✅ | 消息模板（4 类型 EMAIL/SITE_MSG/SMS/WEBHOOK） |
| 4 | MsgSendRecord 实体 | `entity/MsgSendRecord.java` | ✅ | 发送记录（5 状态/重试/收件人类型） |
| 5 | NotificationSettingEntity | `entity/NotificationSettingEntity.java` | ✅ | 全局通道配置（SMTP/密钥等） |
| 6 | NotificationUserPreference | `entity/NotificationUserPreference.java` | ✅ | 用户级偏好（按类型×渠道×频率） |
| 7 | NotificationService | `service/NotificationService.java` | ✅ | 模板 CRUD + 发送 + 统计 |
| 8 | SiteNotificationService | `service/SiteNotificationService.java` | ✅ | 站内通知 CRUD + 模板渲染 + WebSocket |
| 9 | NotificationPreferenceService | `service/NotificationPreferenceService.java` | ✅ | 用户偏好初始化/批量更新 |
| 10 | NotificationDataService | `service/NotificationDataService.java` | ✅ | 缓存层（与项目 DataService 规范一致） |
| 11 | NotificationScheduleService | `service/NotificationScheduleService.java` | ✅ 5 个定时任务 | 合同到期/健康证到期/库存预警/失败重试/过期清理 |
| 12 | AlertNotificationServiceImpl | `service/impl/AlertNotificationServiceImpl.java` | ⚠️ Mock 实现 | 设备告警通知（邮箱/手机号硬编码） |
| 13 | LogBackupNotificationServiceImpl | `service/impl/LogBackupNotificationServiceImpl.java` | ✅ | 日志备份通知 |
| 14 | NotificationMessageConsumer | `service/impl/NotificationMessageConsumer.java` | ✅ RabbitMQ 消费者 | 4 渠道分发（仅 EMAIL 真实可用） |

#### 19.1.2 数据库表清单

| 表名 | 迁移脚本 | 用途 |
|------|---------|------|
| `notification` | postgres-schema-v0.12.sql L1116 | 站内通知主表 |
| `notification_setting` | V20260404__create_notification_tables.sql | 全局通道配置 |
| `notification_user_preference` | V20260425__create_notification_system_tables.sql | 用户偏好 |
| `msg_template` | V20260404__create_notification_tables.sql | 消息模板 |
| `msg_send_record` | V20260404__create_notification_tables.sql | 发送记录 |
| `notification_logs` | V20260516__create_schedule_management_tables.sql L421 | 排班专属通知记录 |

#### 19.1.3 前端组件清单

| 组件 | 路径 | 状态 | 说明 |
|------|------|------|------|
| NotificationCenter | `components/business/NotificationCenter.vue` | ✅ | 全局铃铛下拉式通知中心（未读 badge/批量/筛选） |
| NotificationList | `views/SystemSettings/NotificationList.vue` | ✅ | 通知列表管理页 |
| NotificationSettings | `views/SystemSettings/NotificationSettings.vue` | ✅ | 通知设置页 |
| NotificationSettingsPage | `views/NotificationSettingsPage.vue` | ✅ | 员工端通知偏好页 |
| notification API | `api/notification.ts` | ✅ | 已对接后端 8 个端点 |

#### 19.1.4 API 端点完整清单（25 个）

**模板管理（8 个）**：`/v1/notification/templates` GET/POST/PUT/DELETE + 启用/禁用/预览/变量定义
**消息发送（4 个）**：`/v1/notification/send` POST（单条）/ `/send/batch` POST + `/records` GET（记录）/ `/records/{id}/retry` POST
**设置管理（2 个）**：`/v1/notification/settings` GET/PUT
**统计仪表盘（2 个）**：`/v1/notification/statistics` GET + `/dashboard` GET
**辅助（2 个）**：`/channels` GET + `/templates/{code}/variables` GET
**站内通知（6 个）**：`/notifications` GET + `/notifications/unread-count` GET + `/{id}/read` PUT + `/read-all` PUT + `/{id}` DELETE + `/batch-delete` POST
**用户偏好（3 个）**：`/preferences` GET/PUT + `/preferences/init` POST

### 19.2 4 渠道能力差距矩阵

| 渠道 | 后端 | 真实可用 | 差距 |
|------|------|---------|------|
| EMAIL | NotificationMessageConsumer.handleEmailChannel() | ✅ | 邮件服务器配置可能缺失 |
| SITE_MSG（站内信） | handleSiteMessageChannel() | ❌ 抛 UnsupportedOperationException | 已通过 SiteNotificationService 单独实现（不走 RabbitMQ），但未集成到 4 渠道分发 |
| SMS（短信） | handleSmsChannel() | ❌ 抛 UnsupportedOperationException | 未接入阿里云/腾讯云短信 SDK |
| WEBHOOK | handleWebhookChannel() | ❌ 抛 UnsupportedOperationException | 未实现 HTTP 回调 |

**关键发现**：站内信实际通过 `SiteNotificationService.createNotification()` 直接落库 + WebSocket 推送实现，**不走 RabbitMQ 4 渠道分发**。两套机制并存但未整合。

### 19.3 业务事件 → 通知触发矩阵（已实现）

| # | 业务事件 | 模板编码 | 触发方式 | 状态 |
|---|---------|---------|---------|------|
| 1 | 劳动合同到期 | `contract-expiry` | NotificationScheduleService 定时任务（每天 8:00，30/15/7/1 天前预警） | ✅ |
| 2 | 健康证到期 | `health-cert-expiry` | NotificationScheduleService 定时任务（每天 8:00，60/30/15/7 天前预警） | ✅ |
| 3 | 库存预警 | `inventory-warning` | NotificationScheduleService 定时任务（每小时） | ✅ |
| 4 | 失败消息重试 | — | NotificationScheduleService 定时任务（每 5 分钟） | ✅ |
| 5 | 过期通知清理 | — | NotificationScheduleService 定时任务（每天 2:00） | ✅ |
| 6 | 排班发布/撤回/换班通过/拒绝/明日提醒 | — | notification_logs 表（V20260516 迁移） | ✅ 表已建 |
| 7 | 设备告警 | — | AlertNotificationServiceImpl（邮件/WebSocket） | ⚠️ Mock |
| 8 | 日志备份 | — | LogBackupNotificationServiceImpl | ✅ |

### 19.4 业务事件 → 通知触发矩阵（缺失，需补充）

> **核心结论**：通知基础设施完整，但 14 类跨模块事件未触发通知（与 Section 16.2 #41 断链对应）。

| # | 业务事件 | 应触发的通知 | 接收人 | 当前状态 |
|---|---------|------------|--------|---------|
| 1 | 招聘名额下发 | HR→门店名额下发通知 | 门店店长 | ❌ 缺失（依赖 recruitment_quotas） |
| 2 | 门店提报招聘需求 | 门店→HR 审核通知 | HR 审核员 | ❌ 缺失 |
| 3 | HR 反馈招聘需求 | HR→门店反馈通知 | 门店店长 | ❌ 缺失（依赖 recruitment_feedback） |
| 4 | 入职完成 | HR→门店"新员工到岗"通知 | 门店店长/培训负责人 | ❌ 缺失 |
| 5 | 账号激活 | HR→员工"账号已开通"通知 | 新员工 | ❌ 缺失（依赖 createUserAccount） |
| 6 | 试用期到期 | HR→店长"试用期将到期"通知 | 门店店长 + HR | ❌ 缺失 |
| 7 | 离职申请提交 | 员工→HR"离职申请"通知 | HR + 门店店长 | ❌ 缺失 |
| 8 | 证件报销完成 | 财务→员工"报销已到账"通知 | 申请员工 | ❌ 缺失（依赖 certificate_reimbursements） |
| 9 | 采购订单审批 | 申请人→审批人通知 | 审批人 | ❌ 缺失 |
| 10 | 采购收货完成 | 仓储→采购"已收货"通知 | 采购员 | ❌ 缺失 |
| 11 | 采购付款完成 | 财务→供应商"已付款"通知 | 供应商联系人 | ❌ 缺失 |
| 12 | 财务凭证审核 | 制单人→审核人通知 | 审核人 | ❌ 缺失 |
| 13 | 财务凭证退回 | 审核人→制单人"退回"通知 | 制单人 | ❌ 缺失 |
| 14 | 门店日结完成 | 门店→财务/运营"日结完成"通知 | 财务 + 运营 | ❌ 缺失 |
| 15 | 门店物资需求 | 门店→采购"物资请领"通知 | 采购员 | ❌ 缺失 |
| 16 | 会员充值/消费 | 系统→会员"交易确认"通知 | 会员 | ❌ 缺失 |
| 17 | 优惠券到期 | 系统→会员"券将到期"通知 | 会员 | ❌ 缺失 |
| 18 | 资产维修完成 | 维修员→使用人"可领回"通知 | 资产使用人 | ❌ 缺失 |
| 19 | 用印申请审批 | 申请人→审批人通知 | 审批人 | ❌ 缺失（依赖签章后端） |
| 20 | 系统公告 | 管理员→全员通知 | 全员 | ❌ 缺失（系统公告功能） |

### 19.5 应有完整架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                    站内通信/消息中心完整架构                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─[业务事件源] 20+ 业务事件                                         │
│  │   ├─ 已对接：合同到期/健康证到期/库存预警/排班/设备告警/日志备份  │
│  │   └─ 未对接：招聘/入职/离职/报销/采购/凭证/日结/会员/资产/用印    │
│  │                              ↓                                   │
│  │  ┌─[事件分发器] NotificationEventBus（应有但缺失）               │
│  │  │   - 监听 Spring ApplicationEvent                              │
│  │  │   - 按业务类型路由到对应通知模板                               │
│  │  │   - 查询接收人（按角色/部门/门店）                            │
│  │  │                              ↓                                │
│  │  │  ┌─[通知服务层]                                               │
│  │  │  │   ├─ SiteNotificationService（站内信，直接落库+WS推送）    │
│  │  │  │   └─ NotificationService.send（多渠道，走 RabbitMQ）       │
│  │  │  │                              ↓                             │
│  │  │  │  ┌─[渠道分发] 4 渠道                                       │
│  │  │  │  │   ├─ EMAIL ✅ 真实可用                                  │
│  │  │  │  │   ├─ SITE_MSG ⚠️ 与 SiteNotificationService 重复实现   │
│  │  │  │  │   ├─ SMS ❌ 未接入                                     │
│  │  │  │  │   └─ WEBHOOK ❌ 未实现                                 │
│  │  │  │  │                              ↓                          │
│  │  │  │  │  ┌─[RabbitMQ 消费者] NotificationMessageConsumer        │
│  │  │  │  │  │   - 通用通知队列（concurrency 2-4）                 │
│  │  │  │  │  │   - 专用邮件队列（高优先级）                        │
│  │  │  │  │  │   - 死信队列（最大重试 3 次）                       │
│  │  │  │  │  │                              ↓                       │
│  │  │  │  │  │  ┌─[数据持久化]                                     │
│  │  │  │  │  │  │   ├─ notification（站内通知主表）                 │
│  │  │  │  │  │  │   ├─ msg_send_record（多渠道发送记录）            │
│  │  │  │  │  │  │   ├─ notification_logs（排班专属）                │
│  │  │  │  │  │  │   └─ notification_user_preference（用户偏好）     │
│  │  │  │  │  │  │                              ↓                    │
│  │  │  │  │  │  │  ┌─[WebSocket 实时推送]                          │
│  │  │  │  │  │  │  │   - SimpMessagingTemplate.convertAndSendToUser│
│  │  │  │  │  │  │  │   - 队列：/user/{userId}/queue/notifications  │
│  │  │  │  │  │  │  │                              ↓                │
│  │  │  │  │  │  │  │  ┌─[前端展示]                                 │
│  │  │  │  │  │  │  │  │   ├─ NotificationCenter.vue（铃铛下拉）   │
│  │  │  │  │  │  │  │  │   ├─ NotificationList.vue（列表管理页）   │
│  │  │  │  │  │  │  │  │   ├─ NotificationSettings.vue（设置页）   │
│  │  │  │  │  │  │  │  │   └─ NotificationSettingsPage.vue（偏好）│
│  │  │  │  │  │  │  │  │                              ↓            │
│  │  │  │  │  │  │  │  │  ┌─[定时任务] 5 个                        │
│  │  │  │  │  │  │  │  │  │   ├─ 合同到期预警（30/15/7/1 天）      │
│  │  │  │  │  │  │  │  │  │   ├─ 健康证到期预警（60/30/15/7 天）   │
│  │  │  │  │  │  │  │  │  │   ├─ 库存预警（每小时）                │
│  │  │  │  │  │  │  │  │  │   ├─ 失败重试（每 5 分钟）             │
│  │  │  │  │  │  │  │  │  │   └─ 过期清理（每天 2:00）             │
└──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘
```

### 19.6 待办事项清单（按优先级）

#### P0（商用必需）
1. **统一站内信渠道**：将 `SiteNotificationService` 与 `NotificationMessageConsumer.SITE_MSG` 整合，避免双系统
2. **创建 NotificationEventBus**：基于 Spring ApplicationEvent 实现跨模块事件分发
3. **补充 20 类业务事件通知触发**（见 19.4 矩阵）
4. **邮件服务器配置**：application.yml 补充 `spring.mail.host/username/password`
5. **AlertNotificationServiceImpl Mock 替换**：邮箱/手机号从硬编码改为从配置/数据库读取

#### P1（完善体验）
6. **WebSocket 在线状态感知**：用户离线时通知暂存，上线后批量推送
7. **通知聚合**：相同业务事件 5 分钟内聚合为一条通知（避免刷屏）
8. **通知分类管理**：在 NotificationList.vue 增加按类型筛选（招聘/入职/财务/采购等）
9. **待办事项聚合**：通知中的"待处理"项进入统一待办列表
10. **系统公告功能**：管理员发布全员公告（独立于业务通知）

#### P2（渠道扩展）
11. **短信渠道接入**：阿里云/腾讯云短信 SDK 集成
12. **企业微信/钉钉推送**：连锁模式下店长移动端通知
13. **Webhook 渠道**：支持对接外部系统（如 OA/CRM）

---

## 20. 基础设施支撑系统（V3 补充）

> **背景**：代码库审查发现 3 套基础设施（文件附件/打印任务/数据导出）已存在但未在 DATA_LIFECYCLE.md 中文档化，且部分能力未充分利用。

### 20.1 文件附件管理系统

#### 20.1.1 已有组件

| 组件 | 路径 | 说明 |
|------|------|------|
| FileAttachmentController | `controller/FileAttachmentController.java` | 10 个端点（`/v1/files`） |
| UploadController | `controller/UploadController.java` | 通用上传（`/v1/upload`） |
| FileChunkController | `controller/FileChunkController.java` | 分片上传（`/v1/files/chunk`） |
| FileAttachment 实体 | `entity/FileAttachment.java` | 附件元数据 |
| FileChunk 实体 | `entity/FileChunk.java` | 分片元数据 |
| FileAttachmentService | `service/FileAttachmentService.java` | 上传/下载/MD5 校验/批量 |
| file_attachment 表 | V20260404 迁移 | 附件元数据表 |
| file_chunk 表 | V20260404 迁移 | 分片表 |

#### 20.1.2 API 能力（10 个端点）

1. `POST /v1/files/upload` — 单文件上传（businessType + businessId 关联）
2. `POST /v1/files/upload/batch` — 批量上传
3. `GET /v1/files/{attachmentId}/download` — 下载
4. `GET /v1/files/{attachmentId}` — 附件详情
5. `GET /v1/files` — 分页查询（businessType/上传人/时间）
6. `GET /v1/files/business/{businessType}/{businessId}` — 按业务查询
7. `DELETE /v1/files/{attachmentId}` — 删除
8. `DELETE /v1/files/batch` — 批量删除
9. `GET /v1/files/stats` — 统计
10. `GET /v1/files/exists/md5/{md5}` — MD5 秒传校验

#### 20.1.3 已对接业务场景

| 业务场景 | businessType | 状态 |
|---------|-------------|------|
| 员工档案附件 | `employee` | ✅ |
| 采购合同附件 | `purchase_contract` | ✅ |
| 采购收货单附件 | `purchase_stockin` | ✅ |
| 财务凭证附件 | `finance_voucher` | ✅ |
| 资产档案附件 | `asset` | ✅ |
| 健康证照片 | `health_certificate` | ✅ |
| 产品图片 | `product` | ✅ |

#### 20.1.4 待补充场景

| 业务场景 | businessType | 优先级 |
|---------|-------------|--------|
| 入职档案附件 | `onboarding` | P0（依赖入职流程完善） |
| 证件报销附件 | `certificate_reimbursement` | P0（依赖报销流程） |
| 离职交接单附件 | `resignation` | P1（依赖离职流程） |
| 设备维修记录附件 | `device_repair` | P1 |
| 溯源节点附件 | `trace_node` | P2 |
| 用印申请附件 | `seal_application` | P2（依赖签章后端） |

### 20.2 打印任务管理系统

#### 20.2.1 已有组件

| 组件 | 路径 | 说明 |
|------|------|------|
| PrintTaskController | `controller/device/PrintTaskController.java` | 11 个端点（`/api/v1/print-tasks`） |
| PrintTask 实体 | `entity/PrintTask.java` | 打印任务（含打印机/状态/重试） |
| PrintTaskQueueManager | `util/PrintTaskQueueManager.java` | 打印队列管理器 |
| print_tasks 表 | V5.0.0 迁移 | 打印任务表 |

#### 20.2.2 API 能力（11 个端点）

1. `POST /api/v1/print-tasks` — 创建打印任务
2. `POST /api/v1/print-tasks/batch` — 批量创建
3. `GET /api/v1/print-tasks/{taskId}` — 任务详情
4. `GET /api/v1/print-tasks/page` — 分页查询
5. `GET /api/v1/print-tasks/device/{deviceId}/pending` — 设备待打印
6. `PUT /api/v1/print-tasks/{taskId}/printing` — 标记打印中
7. `PUT /api/v1/print-tasks/{taskId}/completed` — 标记完成
8. `PUT /api/v1/print-tasks/{taskId}/failed` — 标记失败
9. `POST /api/v1/print-tasks/{taskId}/retry` — 重试

#### 20.2.3 已对接业务场景

| 业务场景 | 触发 | 状态 |
|---------|------|------|
| 厨房工单打印 | 订单创建时 | ❌ 未触发（订单打印联动断链） |
| 收银小票打印 | 订单支付完成时 | ❌ 未触发 |
| 采购收货单打印 | 采购收货完成时 | ❌ 未触发 |
| 财务凭证打印 | 凭证过账后 | ❌ 未触发 |
| 排班表打印 | 排班发布后 | ❌ 未触发 |

**核心问题**：PrintTaskController 完整但**无业务模块调用**，与 DEVELOPMENT_STATUS.md Section 0.3 #15"订单完成时 8 项联动全断裂"对应。

### 20.3 数据导出中心

#### 20.3.1 已有组件

| 组件 | 路径 | 说明 |
|------|------|------|
| DataExportController | `controller/DataExportController.java` | 1 个端点（`/v1/admin/export`） |
| ExportTask 实体 | `entity/report/ExportTask.java` | 导出任务（异步任务跟踪） |
| export_tasks 表 | V20260513 迁移 | 导出任务表 |
| DataExportService | `service/DataExportService.java` | 导出服务（CSV/Excel） |

#### 20.3.2 API 能力（当前仅 1 个端点）

1. `GET /v1/admin/export/users` — 用户数据 CSV 导出（GDPR 合规，邮箱/手机号脱敏）

#### 20.3.3 待补充导出场景

> **核心问题**：ExportTask 实体 + export_tasks 表已建，但 Controller 仅暴露 1 个端点，未充分利用。

| 导出场景 | 模块 | 优先级 | 说明 |
|---------|------|--------|------|
| 员工档案导出 | HR | P0 | 含基本信息/合同/薪资/考勤 |
| 考勤记录导出 | HR | P0 | 月度考勤明细 |
| 薪资单导出 | HR | P0 | 月度薪资单（PDF） |
| 采购订单导出 | 采购 | P0 | 含明细/金额/供应商 |
| 采购收货单导出 | 采购 | P1 | 含批次/溯源码 |
| 销售订单导出 | 订单 | P0 | 含菜品/金额/支付方式 |
| 财务凭证导出 | 财务 | P0 | 月度凭证汇总 |
| 财务报表导出 | 财务 | P0 | 资产负债表/利润表/现金流量表 |
| 库存台账导出 | 仓储 | P1 | 当前库存/出入库明细 |
| 溯源记录导出 | 溯源 | P1 | 批次/节点/扫码记录 |
| 会员列表导出 | 会员 | P1 | 含等级/积分/余额 |
| 资产清单导出 | 资产 | P1 | 含折旧/状态 |
| 审计日志导出 | 系统 | P0 | 合规要求 |

### 20.4 三套基础设施整合建议

```
┌─────────────────────────────────────────────────────────────────────┐
│                    基础设施支撑系统整合架构                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─[业务模块] 15 个一级模块                                          │
│  │   ├─ HR（员工档案/考勤/薪资导出 + 入职附件 + 离职交接附件）      │
│  │   ├─ 采购（订单导出 + 合同附件 + 收货附件）                       │
│  │   ├─ 财务（凭证导出 + 报表导出 + 凭证附件）                       │
│  │   ├─ 订单（销售导出 + 厨房打印 + 收银打印）                       │
│  │   ├─ 仓储（库存导出 + 出入库附件）                                │
│  │   ├─ 溯源（溯源导出 + 节点附件）                                  │
│  │   ├─ 会员（会员导出 + 活动附件）                                  │
│  │   ├─ 资产（资产导出 + 维修附件）                                  │
│  │   └─ 系统（审计日志导出 + 配置附件）                              │
│  │                              ↓                                   │
│  │  ┌─[文件附件服务] FileAttachmentService（10 端点 + 分片上传）    │
│  │  │   - 已对接 7 个业务场景                                       │
│  │  │   - 待补充 6 个场景                                           │
│  │  │                              ↓                                │
│  │  │  ┌─[打印任务服务] PrintTaskController（11 端点 + 队列管理）   │
│  │  │  │   - 已建但 0 业务模块调用                                  │
│  │  │  │   - 待对接 5 个场景                                        │
│  │  │  │                              ↓                             │
│  │  │  │  ┌─[数据导出中心] DataExportController（当前 1 端点）      │
│  │  │  │  │   - ExportTask 实体 + 表已建                            │
│  │  │  │  │   - 待补充 12 个导出场景                                │
│  │  │  │  │   - 应改为异步任务模式（提交任务→后台处理→下载）        │
│  │  │  │  │                              ↓                          │
│  │  │  │  │  ┌─[通知服务]（联动）                                   │
│  │  │  │  │  │   - 打印完成通知                                     │
│  │  │  │  │  │   - 导出完成通知                                     │
│  │  │  │  │  │   - 文件上传/删除通知                                │
└──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘
```

### 20.5 三套基础设施待办清单

#### P0（必需）
1. **打印任务对接订单模块**：订单创建→厨房打印 + 订单完成→收银打印
2. **导出中心扩展为异步任务**：提交任务→后台处理→通知用户下载
3. **补充 12 个导出场景**（见 20.3.3）
4. **入职/报销/离职附件场景对接**（依赖人事/财务流程完善）

#### P1（完善）
5. **打印任务对接采购/财务/排班**
6. **大文件分片上传前端组件**（复用 FileChunkController）
7. **附件预览能力**（PDF/图片/Office 在线预览）
8. **导出任务管理页**（ExportTask 列表/取消/重试/下载）

---

**文档结束。**

> 本文档 V3 基于 2026-06-25 全模块深度审查，覆盖全部 15 个一级模块的完整数据流转。建议与 PROJECT_CONTEXT.md V2 和 DEVELOPMENT_STATUS.md V2 配合阅读。
> 
> **V3 新增内容**：
> - 8 个新模块章节（工作台/产品/订单/运营/会员/设备/签章/系统）
> - 35 项跨模块联动矩阵（V2 仅 20 项）
> - 6 项双系统割裂问题
> - 41 项缺陷汇总（V2 仅 20 项）
> - 完整状态机定义（覆盖所有带状态字段的实体）
>
> **V3 增补内容（2026-06-25 第二轮）**：
> - 环节 1.1 招聘名额下发与分配（recruitment_quotas + 6 状态机）
> - 环节 1.2 门店↔HR 招聘协同反馈（recruitment_feedback + 三种反馈）
> - 环节 5 入职→注册账号联动（7 步流程 + position_default_roles 表）
> - 6.5.1 门店证件报销详细逻辑（certificate_reimbursements + 5 状态机）
> - 第 19 节 站内通信/消息中心系统（已有基础设施盘点 + 4 渠道差距 + 20 类业务事件触发矩阵 + 完整架构）
> - 第 20 节 基础设施支撑系统（文件附件/打印任务/数据导出 3 套基础设施 + 整合建议）
> - 跨模块断链矩阵扩展至 50 项（新增 #36-#50：招聘名额/协同反馈/入职注册/证件报销/站内通信/打印联动/导出异步化/站内信整合/事件总线/SMS 渠道）
> - 双系统割裂矩阵扩展至 8 项（新增 #7 站内信双系统、#8 通知记录双表）