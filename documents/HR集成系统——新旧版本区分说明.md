# HR集成系统——新旧版本区分说明

## 概述
本文档用于区分入职档案模块的新旧版本，方便后期清理和维护。

---

## 旧版本（待清理）

### 1. 旧的邀请码系统
**用途**：普通用户邀请注册

| 类型 | 名称 | 路径 | 说明 |
|------|------|------|------|
| 实体类 | InvitationCode | entity/InvitationCode.java | 旧邀请码实体 |
| Mapper | InvitationCodeMapper | mapper/InvitationCodeMapper.java | 旧邀请码Mapper |
| Service | InvitationCodeService | service/InvitationCodeService.java | 旧邀请码服务接口 |
| ServiceImpl | InvitationCodeServiceImpl | service/impl/InvitationCodeServiceImpl.java | 旧邀请码服务实现 |
| Controller | AuthController | controller/AuthController.java | 包含旧邀请码API |
| 数据库表 | invitation_codes | - | 旧邀请码表 |

**API路径**：
- POST /api/invitation-code - 生成邀请码
- GET /api/invitation-code/validate - 验证邀请码
- GET /api/invitation-code/info - 获取邀请码信息
- GET /api/invitation-codes - 获取我的邀请码列表
- DELETE /api/invitation-code - 撤销邀请码

---

## 新版本（入职档案模块 V2.2）

### 1. 入职档案管理
**用途**：管理候选人入职档案

| 类型 | 名称 | 路径 | 说明 |
|------|------|------|------|
| 实体类 | OnboardingArchive | entity/OnboardingArchive.java | 【NEW】入职档案实体 |
| Mapper | OnboardingArchiveMapper | mapper/OnboardingArchiveMapper.java | 【NEW】入职档案Mapper |
| Service | OnboardingArchiveService | service/OnboardingArchiveService.java | 【NEW】入职档案服务接口 |
| ServiceImpl | OnboardingArchiveServiceImpl | service/impl/OnboardingArchiveServiceImpl.java | 【NEW】入职档案服务实现 |
| Controller | OnboardingArchiveController | controller/OnboardingArchiveController.java | 【NEW】入职档案控制器 |
| DTO | OnboardingArchiveDTO | dto/OnboardingArchiveDTO.java | 【NEW】入职档案DTO |
| 数据库表 | onboarding_archive | - | 【NEW】入职档案表 |

**API路径**：
- POST /api/onboarding/archive - 创建档案
- PUT /api/onboarding/archive/{id} - 更新档案
- DELETE /api/onboarding/archive/{id} - 删除档案
- GET /api/onboarding/archive/{id} - 获取详情
- GET /api/onboarding/archive/list - 分页查询
- GET /api/onboarding/archive/by-status/{status} - 按状态查询
- GET /api/onboarding/archive/pending-hr - 待HR审查
- GET /api/onboarding/archive/pending-substantive - 待实质审查
- POST /api/onboarding/archive/{id}/submit - 提交审批
- GET /api/onboarding/archive/position-levels - 职位级别
- GET /api/onboarding/archive/statuses - 档案状态

### 2. 审批管理
**用途**：管理入职档案的审批流程

| 类型 | 名称 | 路径 | 说明 |
|------|------|------|------|
| 实体类 | ApprovalRecord | entity/ApprovalRecord.java | 【NEW】审批记录实体 |
| Mapper | ApprovalRecordMapper | mapper/ApprovalRecordMapper.java | 【NEW】审批记录Mapper |
| Service | ApprovalService | service/ApprovalService.java | 【NEW】审批服务接口 |
| ServiceImpl | ApprovalServiceImpl | service/impl/ApprovalServiceImpl.java | 【NEW】审批服务实现 |
| Controller | ApprovalController | controller/ApprovalController.java | 【NEW】审批控制器 |
| 数据库表 | approval_record | - | 【NEW】审批记录表 |

**API路径**：
- GET /api/onboarding/approval/archive/{archiveId} - 获取档案审批记录
- GET /api/onboarding/approval/pending - 获取待审批列表
- POST /api/onboarding/approval/{recordId}/approve - 审批通过
- POST /api/onboarding/approval/{recordId}/reject - 审批拒绝
- POST /api/onboarding/approval/start/{archiveId} - 启动审批流程
- GET /api/onboarding/approval/complete/{archiveId} - 检查审批是否完成
- GET /api/onboarding/approval/flow/{positionLevel} - 获取审批流程

### 3. 入职邀请码管理
**用途**：管理入职档案的邀请码

| 类型 | 名称 | 路径 | 说明 |
|------|------|------|------|
| 实体类 | InvitationSendRecord | entity/InvitationSendRecord.java | 【NEW】邀请码发送记录实体 |
| Mapper | InvitationSendRecordMapper | mapper/InvitationSendRecordMapper.java | 【NEW】邀请码发送记录Mapper |
| Service | OnboardingInvitationService | service/OnboardingInvitationService.java | 【NEW】入职邀请码服务接口 |
| ServiceImpl | OnboardingInvitationServiceImpl | service/impl/OnboardingInvitationServiceImpl.java | 【NEW】入职邀请码服务实现 |
| Controller | InvitationCodeController | controller/InvitationCodeController.java | 【NEW】入职邀请码控制器 |
| 数据库表 | invitation_send_record | - | 【NEW】邀请码发送记录表 |

**API路径**：
- POST /api/onboarding/invitation/generate/{archiveId} - 生成邀请码
- GET /api/onboarding/invitation/validate - 验证邀请码
- POST /api/onboarding/invitation/use - 使用邀请码
- POST /api/onboarding/invitation/revoke/{codeId} - 撤销邀请码
- GET /api/onboarding/invitation/code/{code} - 查询邀请码
- GET /api/onboarding/invitation/archive/{archiveId} - 查询档案邀请码
- GET /api/onboarding/invitation/expiring - 获取即将过期邀请码
- GET /api/onboarding/invitation/expired - 获取已过期邀请码
- POST /api/onboarding/invitation/extend/{codeId} - 延长有效期
- POST /api/onboarding/invitation/regenerate/{oldCodeId} - 重新生成邀请码

### 4. 费用记录（财务占位符）
**用途**：记录邀请码发送费用

| 类型 | 名称 | 路径 | 说明 |
|------|------|------|------|
| 实体类 | InvitationExpenseRecord | entity/InvitationExpenseRecord.java | 【NEW】费用记录实体 |
| Mapper | InvitationExpenseRecordMapper | mapper/InvitationExpenseRecordMapper.java | 【NEW】费用记录Mapper |
| 数据库表 | invitation_expense_record | - | 【NEW】费用记录表 |

### 5. 提醒记录
**用途**：记录邀请码提醒日志

| 类型 | 名称 | 路径 | 说明 |
|------|------|------|------|
| 实体类 | InvitationReminderLog | entity/InvitationReminderLog.java | 【NEW】提醒记录实体 |
| Mapper | InvitationReminderLogMapper | mapper/InvitationReminderLogMapper.java | 【NEW】提醒记录Mapper |
| 数据库表 | invitation_reminder_log | - | 【NEW】提醒记录表 |

---

## 数据库表汇总

### 新版本表（V2.2）
1. **onboarding_archive** - 入职档案表
2. **approval_record** - 审批记录表
3. **invitation_send_record** - 邀请码发送记录表
4. **invitation_expense_record** - 费用记录表
5. **invitation_reminder_log** - 提醒记录表

### 旧版本表（待清理）
1. **invitation_codes** - 旧邀请码表

---

## 清理计划

### 阶段1：验证新版本
- [ ] 测试入职档案管理API
- [ ] 测试审批流程API
- [ ] 测试入职邀请码API
- [ ] 前端页面开发完成

### 阶段2：迁移数据
- [ ] 将旧邀请码数据迁移到新系统（如有必要）
- [ ] 更新前端调用接口

### 阶段3：清理旧版本
- [ ] 删除旧邀请码相关API
- [ ] 删除旧邀请码Service
- [ ] 删除旧邀请码Mapper
- [ ] 删除旧邀请码实体类
- [ ] 删除旧邀请码数据库表

---

## 注意事项

1. **新旧系统共存期间**：两个邀请码系统同时运行，互不影响
2. **API路径区分**：
   - 旧系统：/api/invitation-code/*
   - 新系统：/api/onboarding/invitation/*
3. **实体类区分**：
   - 旧系统：InvitationCode
   - 新系统：InvitationSendRecord
4. **Service区分**：
   - 旧系统：InvitationCodeService
   - 新系统：OnboardingInvitationService

---

## 创建日期
2026-01-31

## 作者
Liberty
