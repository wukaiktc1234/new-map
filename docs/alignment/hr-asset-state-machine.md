# 人事 / 资产 / 设备模块状态机对齐

> 梳理各模块核心业务对象的状态集合、状态编码、允许动作及闭环性。`[待修复]` 标注状态缺失、状态不闭环、动作缺失等问题。

---

## 1. 人事管理（HR）

### 1.1 员工状态（Employee Status）

| 前端值 | 后端编码 | 中文含义 | 是否闭环 |
|--------|---------|---------|----------|
| `active` | `1` | 在职 | ✅ |
| `inactive` | `0` | 离职 | ✅ |
| `probation` | `2` | 试用期 | ✅ |

**状态流转**

```
draft/待入职 → probation/试用期 → active/在职 → inactive/离职
```

| 起点 | 动作 | 终点 | 支持情况 | 说明 |
|------|------|------|----------|------|
| — | 入职 | `probation` / `active` | ❓ 未定义 | 无“待入职”状态，入职流程直接写入 `active` 或 `probation`。`[待修复]` |
| `probation` | 转正 | `active` | ❓ 未显式定义 | 转正动作无独立 API，通常直接更新 `status`。`[待修复]` |
| `active` | 离职 | `inactive` | ✅ | `EmployeeResignData` + 更新 `status=0`。 |
| `inactive` | 复职 | `active` | ❌ 缺失 | 无复职状态回退机制。`[待修复]` |

**问题**
- `[待修复]` 员工只有 3 个离散状态，缺少“待入职（draft）”状态，无法完整表达招聘→入职→试用期→在职→离职的生命周期。
- `[待修复]` 状态机缺少“试用期转正”独立动作，转正与直接更新在职状态语义混用。

---

### 1.2 合同状态（Contract Status）

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `draft` | `1` | 草稿 |
| `pending_approval` | `2` | 待审批 |
| `pending_sign` | `3` | 待签署 |
| `company_signed` | `4` | 公司已签 |
| `signed` | `5` | 双方已签 |
| `active` | `6` | 生效中 |
| `expiring` | `7` | 即将到期 |
| `expired` | `8` | 已到期 |
| `terminated` | `9` | 已终止 |
| `renewed` | `10` | 已续签 |
| `archived` | `11` | 已归档 |

**状态流转**

```
draft → pending_approval → pending_sign → company_signed → signed → active → expiring → expired
   ↓          ↓               ↓                ↓              ↓         ↓           ↓
archived   rejected        rejected       rejected      terminated  renewed    terminated
```

| 起点 | 动作 | 终点 | 支持情况 | 说明 |
|------|------|------|----------|------|
| `draft` | 提交审批 | `pending_approval` | ❌ 后端未实现 | `submitApproval` 抛错。`[待修复]` |
| `pending_approval` | 审批通过 | `pending_sign` | ❓ 未明确 | 审批动作未独立暴露。`[待修复]` |
| `pending_sign` | 签署 | `signed`/`company_signed` | ✅ | `sign` / `signWithSignature` 已对接。 |
| `active` | 到期 | `expired`/`expiring` | ❓ 自动状态 | 无定时任务说明，靠前端展示标记。`[待修复]` |
| `active`/`expiring` | 续签 | `renewed` | ❌ 后端未实现 | `renew` 抛错。`[待修复]` |
| `active`/`expiring` | 终止 | `terminated` | ✅ | `terminate` 已对接。 |
| `expired`/`terminated` | 归档 | `archived` | ❌ 后端未实现 | `archive` 抛错。`[待修复]` |
| `archived` | 销毁 | `destroyed` | ❌ 后端未实现 | `destroy` 抛错。`[待修复]` |

**问题**
- `[待修复]` 合同归档、销毁、续签、提交审批等关键状态流转动作后端未实现，状态机无法闭环。
- `[待修复]` “即将到期（expiring）”与“已到期（expired）”的自动转换机制缺失。

---

### 1.3 考勤状态（Attendance Status）

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `normal` | `1` | 正常 |
| `late` | `2` | 迟到 |
| `early_leave` | `3` | 早退 |
| `absent` | `4` | 缺勤 |
| `overtime` | `5` | 加班 |
| `leave` | `6` | 请假 |

**状态特点**：考勤状态为记录级标记，非严格状态机，无流转动作，由打卡数据或人工标记直接确定。

**问题**
- `[待修复]` `shiftType` 字段后端缺失，导致“班次”无法参与考勤状态计算。
- `[待修复]` `overtime` 与 `leave` 是否应作为独立“状态”存在存疑，建议拆分为“考勤结果 + 加班/请假标签”。

---

### 1.4 薪资状态（Salary Status）

| 前端值 | 后端值（中文字符串） | 中文含义 |
|--------|---------------------|---------|
| `DRAFT` | `草稿` | 草稿 |
| `PENDING` | `待确认` | 待确认 |
| `APPROVED` | `已确认` | 已确认 |
| `PAID` | `已发放` | 已发放 |
| `CANCELLED` | `已撤销` | 已撤销 |

**状态流转**

```
DRAFT → PENDING → APPROVED → PAID
          ↓           ↓
      CANCELLED   CANCELLED
```

| 起点 | 动作 | 终点 | 支持情况 | 说明 |
|------|------|------|----------|------|
| `DRAFT` | 生成/提交 | `PENDING` | ✅ | `create` 写入待确认。 |
| `PENDING` | 确认 | `APPROVED` | ✅ | `approve(approved=true)` 对接 `POST /confirm/{id}`。 |
| `PENDING`/`APPROVED` | 驳回 | `CANCELLED` | ❌ 后端缺失 | `approve(approved=false)` 抛错，无 reject 接口。`[待修复]` |
| `APPROVED` | 发放 | `PAID` | ❌ 后端缺失 | `pay` / `batchPay` 抛错。`[待修复]` |
| `PAID` | 财务同步 | `SYNCED` | ❌ 后端缺失 | 财务同步接口未实现。`[待修复]` |

**问题**
- `[待修复]` 薪资发放、驳回、财务同步等关键流转动作缺失，状态机不完整。
- `[待修复]` 后端状态使用中文字符串，违反项目规范“后端使用数字编码”。

---

### 1.5 招聘状态（Recruitment Status）

#### 招聘职位状态

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `published` | `1` | 已发布 |
| `paused` | `2` | 暂停 |
| `closed` | `3` | 已关闭 |

**流转**：`published ↔ paused → closed`（关闭后不可恢复）。

#### 简历状态

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `pending` | `1` | 待筛选 |
| `screening` | `2` | 筛选中 |
| `interviewing` | `3` | 面试中 |
| `offered` | `4` | 已发 Offer |
| `hired` | `5` | 已录用 |
| `rejected` | `6` | 已拒绝 |

**状态流转**

```
pending → screening → interviewing → offered → hired
   ↓           ↓            ↓            ↓
rejected    rejected     rejected     rejected
```

**问题**
- `[待修复]` 招聘“已录用（hired）”之后没有与“入职办理（Onboarding）”的明确状态衔接，招聘流与入职流存在断点。
- `[待修复]` “已拒绝（rejected）”的“申请恢复”需要 HR 审批，但状态机中无 `pending_recovery` 等中间态。

---

### 1.6 健康证状态（Health Certificate Status）

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `valid` | `1` | 有效 |
| `expiring` | `2` | 即将到期 |
| `expired` | `3` | 已过期 |
| `revoked` | `4` | 已撤销 |

**状态特点**：由 `expiryDate` 与当前日期计算得出，无显式流转动作。

**问题**
- `[待修复]` 健康证状态为“计算状态”，但前后端均依赖前端/定时任务计算，未在数据库层维护或触发器同步。

---

### 1.7 入职办理状态（Onboarding Status）

| 前端类型 | 说明 |
|---------|------|
| `status?: string` | 前端仅声明为 `string`，未定义具体状态枚举。`[待修复]` |

**问题**
- `[待修复]` 入职档案缺少标准状态机（如 `draft → submitted → reviewing → approved → onboarded → archived`）。
- `[待修复]` 入职办理与合同签署、账号开通、培训分配等下游流程未形成闭环。

---

### 1.8 培训与知识库状态

| 模块 | 状态定义 | 问题 |
|------|---------|------|
| 培训课程 | `CourseStatus`: `not_started/in_progress/completed` | `[待修复]` 未与后端映射，仅本地枚举。 |
| 课程发布 | `CoursePublishStatus`: `draft/published/archived` | `[待修复]` 同上。 |
| 学习记录 | `StudyStatus`: `not_started/in_progress/completed` | `[待修复]` 同上。 |
| 考试记录 | `QuizPassStatus`: `not_attempted/passed/failed` | `[待修复]` 同上。 |
| 知识文章 | `ArticlePublishStatus`: `draft/published/archived` | `[待修复]` 同上。 |

---

## 2. 资产管理（Asset）

### 2.1 资产主状态（Asset Status）

#### 前端枚举（7 种）

| 前端值 | 中文含义 | 后端对应编码 |
|--------|---------|-------------|
| `active` | 在用 | `1`（in_use） |
| `idle` | 闲置 | `2`（idle） |
| `maintenance` | 维修中 | `3`（repairing） |
| `to_be_disposed` | 待处置 | ❌ 无 | `[待修复]` |
| `disposed` | 已处置 | `5`（已处置） |
| `scrapped` | 已报废 | `4`（damaged） |
| `transferred` | 已调拨 | ❌ 无 | `[待修复]` |

#### 后端编码（5 种）

| 后端编码 | 后端含义 | 前端对应值 |
|---------|---------|-----------|
| `1` | 在用 | `active` |
| `2` | 闲置 | `idle` |
| `3` | 维修中 | `maintenance` |
| `4` | 已报废 | `scrapped` |
| `5` | 已处置 | `disposed` |

**状态流转**

```
idle ↔ active → maintenance → to_be_disposed → disposed/scrapped
   ↓              ↓
transferred   active（维修完成）
```

| 起点 | 动作 | 终点 | 支持情况 | 说明 |
|------|------|------|----------|------|
| `idle`/`active` | 调拨 | `transferred` | ❌ 后端未实现 | `transferApi` 全部抛错。`[待修复]` |
| `active`/`idle` | 维修 | `maintenance` | ✅ | `POST /v1/asset/{id}/repair`。 |
| `maintenance` | 完成维修 | `active`/`idle` | ✅ | `POST /v1/asset/{id}/complete-repair`。 |
| `active`/`idle`/`maintenance` | 报废 | `scrapped` | ✅ | `POST /v1/asset/{id}/scrap`。 |
| `active`/`idle` | 处置申请 | `to_be_disposed` | ❌ 缺失 | 资产主表无“待处置”状态，直接进入处置记录。`[待修复]` |
| `to_be_disposed` | 执行处置 | `disposed` | ❌ 缺失 | 处置审批通过后未回写资产主表状态。`[待修复]` |

**问题**
- `[待修复]` 前端定义了 `to_be_disposed`、`transferred` 两种状态，后端 `AssetMasterNew` 无对应编码，状态机无法闭环。
- `[待修复]` 资产调拨功能完整缺失，调拨状态 `transferred` 无法落地。
- `[待修复]` 处置流程与资产主表状态未联动，处置完成后资产状态仍为原状态。

---

### 2.2 处置审批状态（Disposal Status）

| 前端值 | 后端值 | 中文含义 |
|--------|--------|---------|
| `pending` | `pending` / `1` | 待审批 |
| `approved` | `approved` / `2` | 已审批 |
| `completed` | `completed` / `3` / `done` | 已完成 |
| `rejected` | `rejected` / `4` | 已驳回 |

**状态流转**

```
pending → approved → completed
    ↓
rejected
```

| 起点 | 动作 | 终点 | 支持情况 | 说明 |
|------|------|------|----------|------|
| `pending` | 审批通过 | `approved` | ✅ | `POST /v1/asset/disposal/{id}/approve?approved=true`。 |
| `pending` | 驳回 | `rejected` | ✅ | 同上，`approved=false`。 |
| `approved` | 执行处置 | `completed` | ❌ 后端缺失 | `execute` 方法抛错，资产主表状态未更新。`[待修复]` |

---

### 2.3 盘点状态（Inventory Status）

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `DRAFT` | `0` | 草稿 / 待盘 |
| `IN_PROGRESS` | `1` | 进行中 / 盘中 |
| `COMPLETED` | `3` | 已完成 |
| `CANCELLED` | `2` | 已取消 |

**状态流转**

```
DRAFT → IN_PROGRESS → COMPLETED
   ↓
CANCELLED
```

| 起点 | 动作 | 终点 | 支持情况 | 说明 |
|------|------|------|----------|------|
| `DRAFT` | 开始盘点 | `IN_PROGRESS` | ✅ | `update(id, { status: IN_PROGRESS })`。 |
| `IN_PROGRESS` | 录入结果 | `IN_PROGRESS` | ✅ | `POST /items/{itemId}`。 |
| `IN_PROGRESS` | 完成盘点 | `COMPLETED` | ✅ | `POST /complete`。 |
| `DRAFT`/`IN_PROGRESS` | 取消 | `CANCELLED` | ⚠️ 映射错误 | 前端 `CANCELLED` 映射到后端编码 `2`，后端 `2` 实际为“已审核”。`[待修复]` |

**问题**
- `[待修复]` `CANCELLED` 状态映射错误，会错误地写入“已审核”。
- `[待修复]` 后端状态“已审核（2）”在前端无对应状态，盘点审批流程未体现。

---

### 2.4 维修状态（Maintenance Status）

| 前端值 | 后端来源 | 中文含义 |
|--------|---------|---------|
| `pending` | 前端推断 | 待维修 |
| `in_progress` | 前端推断 | 维修中 |
| `completed` | 前端推断（`afterValue != null`） | 已完成 |

**问题**
- `[待修复]` 后端 `AssetFlowRecord` 无状态字段，维修状态完全靠前端推断，不可靠。
- `[待修复]` 缺少“待维修”到“维修中”的显式动作，创建维修直接视为 `in_progress`。

---

### 2.5 调拨状态（Transfer Status）

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `pending` | — | 待审批 |
| `approved` | — | 已审批 |
| `completed` | — | 已完成 |
| `rejected` | — | 已驳回 |

**问题**
- `[待修复]` 调拨功能及对应后端实体、Controller 完全缺失，状态机仅为前端枚举，无法落地。

---

## 3. 设备管理（Device）

### 3.1 设备运行状态（Device Status）

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `offline` | `0` | 离线 |
| `online` | `1` | 在线 |
| `fault` | `2` | 故障 |
| `maintenance` | `3` | 维护中 |

**状态流转**

```
offline ↔ online
   ↓         ↓
fault   maintenance
   ↓         ↓
online  online/offline
```

| 起点 | 动作 | 终点 | 支持情况 | 说明 |
|------|------|------|----------|------|
| `offline` | 上线/启用 | `online` | ✅ | `PUT /v1/devices/{id}/status` / `toggle?enabled=true`。 |
| `online` | 离线/停用 | `offline` | ✅ | 同上。 |
| `online`/`offline` | 故障 | `fault` | ✅ | `updateStatus(id, 2)`。 |
| `online`/`offline`/`fault` | 维护 | `maintenance` | ✅ | `updateStatus(id, 3)`。 |
| `maintenance` | 维护完成 | `online`/`offline` | ✅ | 需手动调用 `updateStatus`。 |
| `fault` | 恢复 | `online`/`offline` | ✅ | 需手动调用 `updateStatus`。 |

**问题**
- `[待修复]` 状态流转无业务规则校验（例如：维护中设备是否允许直接标记为在线）。
- `[待修复]` 设备告警产生时，未自动驱动设备状态切换到 `fault`。

---

### 3.2 设备告警状态（Device Alert Status）

| 前端值 | 后端编码 | 中文含义 |
|--------|---------|---------|
| `pending` | `0` | 未处理 |
| `handling` | `1` | 处理中 |
| `resolved` | `2` | 已解决 |
| `ignored` | `3` | 已忽略 |

**状态流转**

```
pending → handling → resolved
    ↓
ignored
```

| 起点 | 动作 | 终点 | 支持情况 | 说明 |
|------|------|------|----------|------|
| `pending` | 开始处理 | `handling` | ❌ 无独立 API | 前端无“开始处理”动作，直接调用 `handle` 进入 `resolved`。`[待修复]` |
| `pending`/`handling` | 处理完成 | `resolved` | ✅ | `PUT /v1/device-alerts/{id}/handle`。 |
| `pending` | 忽略 | `ignored` | ❌ 无独立 API | 缺少 ignore 接口。`[待修复]` |

**问题**
- `[待修复]` 告警状态缺少“开始处理”和“忽略”的独立后端动作，状态机不完整。
- `[待修复]` `isHandled` 与 `alertStatus` 语义重复，存在二义性。

---

## 4. 跨模块状态机问题汇总

| 问题 | 影响模块 | 说明 |
|------|---------|------|
| 状态枚举前后端数量不一致 | 资产、盘点 | 资产前端 7 状态 vs 后端 5 状态；盘点 `CANCELLED` 映射错误。`[待修复]` |
| 状态机缺少关键流转动作 | 合同、薪资、资产调拨、设备告警 | 多个核心动作后端未实现或缺失独立接口。`[待修复]` |
| 状态由前端推断 | 维修、健康证 | 后端无状态字段，可靠性低。`[待修复]` |
| 状态未与关联业务联动 | 资产处置、招聘入职 | 处置完成后资产主表状态未更新；招聘录用后未自动发起入职。`[待修复]` |
| 缺少标准状态枚举 | 入职办理、培训、知识库 | 仅声明 `string` 或前端本地枚举，未与后端对齐。`[待修复]` |
