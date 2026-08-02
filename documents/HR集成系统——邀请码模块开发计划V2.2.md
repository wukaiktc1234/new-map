# HR集成系统——邀请码模块开发计划 V2.2

> **最后更新**：2026-01-31  
> **架构方案**：混合架构（小岛形式 + 集成）  
> **文档版本**：v2.2  
> **文档作者**：Liberty（产品设计与策划）、AI Assistant（技术实现与文档编写）

---

## 📌 版本说明

### 新版本标识
本文档描述的是 **V2.2 新版本** 的入职档案模块，与旧版本的邀请码系统完全独立。

### 新旧版本区分
- **新版本（V2.2）**：入职档案模块，使用 `OnboardingArchive`、`ApprovalRecord`、`InvitationSendRecord` 等实体
- **旧版本（待清理）**：普通邀请码系统，使用 `InvitationCode` 实体

### 详细区分文档
详见：[新旧版本区分说明.md](./HR集成系统——新旧版本区分说明.md)

---

***

## 一、架构设计

### 推荐方案：混合架构（小岛形式 + 集成）

#### 核心思想

```
邀请码模块作为"半独立小岛"：
- 代码层面：独立模块/服务
- 部署层面：可以与HR系统一起部署（初期），也可以独立部署（后期）
- 接口层面：提供标准API给其他部门
- 数据层面：共享用户/部门数据，但业务数据独立
```

#### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     HR系统（主系统）                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   员工管理    │  │   考勤管理    │  │   薪资管理    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              邀请码模块（半独立）                      │   │
│  │                                                      │   │
│  │  内部服务：                                            │   │
│  │  - ArchiveService（档案）                             │   │
│  │  - ApprovalService（审批）                            │   │
│  │  - InvitationService（邀请码）                        │   │
│  │                                                      │   │
│  │  对外API接口：                                         │   │
│  │  ┌─────────────────────────────────────────────┐    │   │
│  │  │  POST /api/onboarding/archive              │    │   │
│  │  │  GET  /api/onboarding/archive/{id}         │    │   │
│  │  │  POST /api/onboarding/approval/{id}/approve│    │   │
│  │  │  POST /api/onboarding/approval/{id}/reject │    │   │
│  │  │  GET  /api/onboarding/approval/pending     │    │   │
│  │  └─────────────────────────────────────────────┘    │   │
│  │                                                      │   │
│  │  依赖HR系统：                                         │   │
│  │  - 用户体系（通过接口获取）                            │   │
│  │  - 部门体系（通过接口获取）                            │   │
│  │  - 权限体系（通过接口验证）                            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
          │
          │ 对外API
          │
┌─────────┴───────────────────────────────────────────────────┐
│                    其他部门/系统                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   技术部审批  │  │   财务部查看  │  │  招聘系统触发 │      │
│  │   （审批接口）│  │   （查询接口）│  │  （创建接口） │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

#### 数据隔离设计

```
HR主数据库                    邀请码模块数据库
┌──────────────┐             ┌──────────────┐
│   users      │────────────▶│  onboarding  │
│  departments │  只读接口   │  _archives   │
│   roles      │             │  approvals   │
└──────────────┘             │  invitations │
                             │  expenses    │
                             └──────────────┘
```

* **共享数据**：用户、部门、角色（通过接口只读）

* **独立数据**：档案、审批、邀请码、费用（独立表）

***

## 二、邀请码管理系统开发计划

### 系统归属

**所属集合**：HR系统（半独立模块）  
**系统名称**：员工入职管理系统 - 邀请码模块  
**关联系统**：

* 上游：自动化招聘系统（占位符）

* 下游：登录认证系统

* 并行：财务系统（占位符）

* 外部：其他部门审批系统（通过API）

***

### 核心流程

```
自动化招聘系统（占位符）
  ↓ 触发信号（候选人通过面试/接受offer）
  ↓ 候选人投递简历（电子+纸质）
  ↓ 系统标记投递岗位
  ↓ 面试流程
  ↓ 通过面试
  ↓ 自动触发创建入职档案
HR补充信息、分配部门职位
  ↓ 确定薪资（HR薪资审查 + 用人部门建议）
审批系统
  ↓ 根据职位级别自动分配审批流程
  ↓ 形式审查（HR）+ 实质审查（用人部门）
邀请码自动生成
  ↓ 审批通过后自动生成
  ↓ 自动发送给候选人（邮件+短信）
  ↓ 通知部门主管
  ↓ 【占位符】发送费用对接财务系统
候选人注册
  ↓ 使用邀请码完成注册
  ↓ 自动归属到对应部门、职位、角色
过期监控
  ↓ 24小时内过期提醒部门主管
  ↓ 过期后通知HR处理
异常处理
  ↓ HR介入处理异常（重新发送/延期/撤销）
```

***

### 开发阶段

#### 第1周：核心功能

* [ ] 入职档案管理页面（HR创建档案）

* [ ] 审批流程引擎

* [ ] 邀请码自动生成逻辑

* [ ] 自动发送功能

* [ ] 对外API接口框架

#### 第2周：监控与异常

* [ ] 过期监控与提醒

* [ ] HR异常处理界面

* [ ] 审批管理页面

* [ ] 审计日志

* [ ] 给其他部门的审批API

#### 第3周：薪资与对接预留

* [ ] 薪资审查功能

* [ ] 【占位符】自动化招聘系统对接接口

* [ ] 【占位符】财务系统对接接口

* [ ] HR系统数据接口（用户、部门、权限）

***

### 审批流程设计

| 职位级别           | 审批流程                   | 说明    |
| -------------- | ---------------------- | ----- |
| **普通员工**       | HR形式审查 → 直接主管实质审查      | 简单流程  |
| **主管/经理**      | HR形式审查 → 部门总监实质审查      | 管理层审批 |
| **部门总监**       | HR形式审查+建议 → 总经理审批      | 高管审批  |
| **副总/总经理/董监高** | HR形式审查+建议+风险评估 → 董事长审批 | 最高层审批 |

**特殊处理：**

* 部门总监入职：由总经理直接审批（不经过其他总监）

* 新部门无主管：由上级部门负责人或总经理审批

* HR否决权：核心岗位HR可否决，但可申诉给董事长

***

### HR职责与权限

| 环节       | HR职责        | 权限            |
| -------- | ----------- | ------------- |
| **档案创建** | 补充信息，分配部门职位 | 创建权（受级别限制）    |
| **形式审查** | 审核资料完整性     | 通过/驳回         |
| **薪资审查** | 监督薪资合理性     | 建议权/否决权（核心岗位） |
| **异常处理** | 处理邀请码异常     | 介入处理权         |

**HR权限限制：**

* 普通HR只能创建普通员工档案

* HR负责人可以创建管理层档案

* HR只能创建比自己级别低的岗位

***

### 对外API接口设计

#### 1. 给其他部门的审批接口（带限流保护）

```java
/**
 * 入职审批接口（提供给其他部门）
 * 限流策略：审批操作 10次/秒
 */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingApprovalApi {
    
    /**
     * 获取待审批列表（给部门主管用）
     * 限流：100次/秒（查询类接口）
     */
    @RateLimiter(name = "onboarding-query", permitsPerSecond = 100)
    @GetMapping("/approval/pending")
    public List<PendingApprovalDTO> getPendingApprovals(
            @RequestParam Long approverId) {
        // 验证approverId是否有审批权限
        return approvalService.getPendingByApprover(approverId);
    }
    
    /**
     * 审批通过（给部门主管用）
     * 限流：10次/秒（操作类接口）
     */
    @RateLimiter(name = "onboarding-approval", permitsPerSecond = 10)
    @PostMapping("/approval/{archiveId}/approve")
    public ResponseEntity<String> approve(
            @PathVariable Long archiveId,
            @RequestParam Long approverId,
            @RequestParam String comment) {
        // 验证审批人权限
        approvalService.approve(archiveId, approverId, comment);
        return ResponseEntity.ok("审批通过");
    }
    
    /**
     * 审批拒绝（给部门主管用）
     * 限流：10次/秒（操作类接口）
     */
    @RateLimiter(name = "onboarding-approval", permitsPerSecond = 10)
    @PostMapping("/approval/{archiveId}/reject")
    public ResponseEntity<String> reject(
            @PathVariable Long archiveId,
            @RequestParam Long approverId,
            @RequestParam String reason) {
        approvalService.reject(archiveId, approverId, reason);
        return ResponseEntity.ok("已拒绝");
    }
}

/**
 * 限流异常处理
 */
@RestControllerAdvice
public class RateLimitExceptionHandler {
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException e) {
        return ResponseEntity.status(429)
            .body(new ErrorResponse("请求过于频繁，请稍后再试", 429));
    }
}
```

#### 2. 给招聘系统的触发接口

```java
/**
 * 招聘系统对接接口（占位符）
 */
@RestController
@RequestMapping("/api/onboarding/hire-integration")
public class HireIntegrationApi {
    
    /**
     * 【占位符】接收招聘系统触发，自动创建档案
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> receiveHireTrigger(
            @RequestBody HireTriggerRequest request) {
        // TODO: 后续对接自动化招聘系统
        Long archiveId = archiveService.createFromHireSystem(request);
        return ResponseEntity.ok("档案已创建，ID: " + archiveId);
    }
}
```

#### 3. 给财务系统的费用接口

```java
/**
 * 财务系统对接接口（占位符）
 */
@RestController
@RequestMapping("/api/onboarding/financial-integration")
public class FinancialIntegrationApi {
    
    /**
     * 【占位符】查询费用记录（给财务系统用）
     */
    @GetMapping("/expenses")
    public List<ExpenseRecordDTO> getExpenses(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        // TODO: 后续对接财务系统
        return expenseService.getByDateRange(startDate, endDate);
    }
}
```

#### 4. 依赖HR系统的数据接口

```java
/**
 * HR系统数据接口（内部使用）
 */
@RestController
@RequestMapping("/api/onboarding/hr-data")
public class HrDataApi {
    
    /**
     * 获取用户信息
     */
    @GetMapping("/users/{userId}")
    public UserDTO getUser(@PathVariable Long userId) {
        // 调用HR系统接口
        return hrSystemClient.getUser(userId);
    }
    
    /**
     * 获取部门信息
     */
    @GetMapping("/departments/{deptId}")
    public DepartmentDTO getDepartment(@PathVariable String deptId) {
        return hrSystemClient.getDepartment(deptId);
    }
    
    /**
     * 验证用户权限
     */
    @PostMapping("/permissions/check")
    public boolean checkPermission(
            @RequestParam Long userId,
            @RequestParam String permission) {
        return hrSystemClient.checkPermission(userId, permission);
    }
}
```

***

### 数据库表设计

#### 1. 入职档案表

```sql
CREATE TABLE onboarding_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    candidate_id VARCHAR(50) COMMENT '候选人ID',
    candidate_name VARCHAR(100) NOT NULL COMMENT '姓名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机',
    position VARCHAR(100) NOT NULL COMMENT '职位',
    position_level VARCHAR(20) COMMENT '职位级别',
    department_id VARCHAR(50) NOT NULL COMMENT '部门ID',
    role_id VARCHAR(50) COMMENT '角色ID',
    expected_salary DECIMAL(10,2) COMMENT '期望薪资',
    final_salary DECIMAL(10,2) COMMENT '最终薪资',
    onboard_date DATE COMMENT '预计入职日期',
    invitation_code_id BIGINT COMMENT '关联邀请码ID',
    status VARCHAR(20) COMMENT '状态',
    create_by BIGINT COMMENT '创建人（HR）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_department (department_id)
);
```

#### 2. 审批记录表

```sql
CREATE TABLE approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    archive_id BIGINT NOT NULL COMMENT '档案ID',
    step_order INT COMMENT '步骤序号',
    step_name VARCHAR(50) COMMENT '步骤名称',
    reviewer_id BIGINT COMMENT '审批人ID',
    reviewer_role VARCHAR(50) COMMENT '审批人角色',
    review_type VARCHAR(20) COMMENT '审查类型：FORMAL/SUBSTANTIVE',
    result VARCHAR(20) COMMENT '结果：PASSED/REJECTED',
    comment TEXT COMMENT '审批意见',
    review_time DATETIME COMMENT '审批时间',
    signature VARCHAR(255) COMMENT '数字签名（防篡改）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_archive_id (archive_id)
);
```

#### 3. 邀请码发送记录表

```sql
CREATE TABLE invitation_send_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invitation_code_id BIGINT NOT NULL COMMENT '邀请码ID',
    send_method VARCHAR(20) COMMENT '发送方式：EMAIL/SMS',
    send_status VARCHAR(20) COMMENT '发送状态',
    send_time DATETIME COMMENT '发送时间',
    recipient VARCHAR(100) COMMENT '接收人',
    error_message VARCHAR(500) COMMENT '错误信息',
    batch_id VARCHAR(50) COMMENT '批次ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invitation_code_id (invitation_code_id)
);
```

#### 4. 费用记录表（财务占位符）

```sql
CREATE TABLE invitation_expense_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(50) COMMENT '批次ID',
    expense_type VARCHAR(50) COMMENT '费用类型：EMAIL/SMS',
    quantity INT COMMENT '数量',
    unit_price DECIMAL(10,4) COMMENT '单价',
    total_amount DECIMAL(10,2) COMMENT '总金额',
    department_id VARCHAR(50) COMMENT '部门ID',
    status VARCHAR(20) COMMENT '状态：PENDING/SYNCED',
    financial_system_id VARCHAR(100) COMMENT '财务系统ID（对接后回填）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch_id (batch_id)
);
```

#### 5. 提醒记录表

```sql
CREATE TABLE invitation_reminder_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invitation_code_id BIGINT COMMENT '邀请码ID',
    reminder_type VARCHAR(20) COMMENT '提醒类型：EXPIRING_24H/EXPIRED',
    recipient_id BIGINT COMMENT '接收人ID（部门主管）',
    recipient_type VARCHAR(20) COMMENT '接收人类型：MANAGER/HR',
    send_time DATETIME COMMENT '发送时间',
    read_status VARCHAR(20) COMMENT '阅读状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

***

## 三、权力安全漏洞及解决方案

### 漏洞1：HR创建档案时的权限过大

**问题：**

* HR在创建档案时可以任意分配部门、职位、角色

* 没有限制HR只能创建特定级别以下的岗位

**风险：**

* HR可能给自己或他人创建高管级别的档案

* 越权分配敏感权限

**解决方案：**

```java
// 添加HR权限限制
public class HrPermissionChecker {
    
    public void checkCreatePermission(Long hrId, String positionLevel) {
        User hr = userService.getById(hrId);
        
        // HR只能创建比自己级别低的岗位
        if (hr.getPositionLevel() <= PositionLevel.fromCode(positionLevel).getLevel()) {
            throw new BusinessException("您无权创建该级别的岗位档案");
        }
        
        // 或者：普通HR只能创建普通员工，HR负责人可以创建管理层
        if (!hr.hasRole("HR_HEAD") && isManagementLevel(positionLevel)) {
            throw new BusinessException("只有HR负责人可以创建管理层档案");
        }
    }
}
```

***

### 漏洞2：审批人自己审批自己

**问题：**

* 虽然之前讨论了"自己不能审自己"，但没有具体实现细节

**风险：**

* 主管给自己创建档案时，可能绕过审批

**解决方案：**

```java
// 严格禁止自己审批自己
public class ApprovalValidator {
    
    public void validateApprover(Long archiveId, Long approverId) {
        OnboardingArchive archive = archiveRepository.findById(archiveId);
        
        // 审批人不能是档案创建人
        if (approverId.equals(archive.getCreateBy())) {
            throw new BusinessException("不能审批自己创建的档案");
        }
        
        // 审批人不能是候选人本人（防止自批）
        User approver = userService.getById(approverId);
        if (approver.getEmail().equals(archive.getEmail())) {
            throw new BusinessException("不能审批自己的入职申请");
        }
    }
}
```

***

### 漏洞3：邀请码发送后的权限控制缺失（含重放攻击防护）

**问题：**

* 邀请码发送后，候选人可以转发给他人使用

* 系统无法验证实际使用人是否是预期的候选人

* 邀请码可能被多次使用（重放攻击）

**风险：**

* 邀请码被转发给非预期人员

* 冒名入职

* 一个邀请码被多人使用

**解决方案：**

```java
// 邀请码绑定候选人信息 + 一次性使用验证
public class InvitationCodeBinding {
    
    public void generateBoundInvitationCode(OnboardingArchive archive) {
        InvitationCode code = new InvitationCode();
        code.setCode(generateUniqueCode());
        code.setBoundEmail(archive.getEmail());  // 绑定邮箱
        code.setBoundPhone(archive.getPhone());  // 绑定手机
        code.setBoundName(archive.getCandidateName()); // 绑定姓名
        code.setUseCount(0);  // 初始化使用次数
        code.setMaxUseCount(1);  // 最大使用次数为1（一次性）
        code.setStatus("UNUSED");  // 未使用状态
        
        // 注册时验证绑定信息
        // ...
    }
    
    // 注册时验证
    public void validateBinding(String code, String email, String phone, String name) {
        InvitationCode invitationCode = invitationCodeMapper.findByCode(code);
        
        // 验证是否已被使用
        if (!"UNUSED".equals(invitationCode.getStatus())) {
            throw new BusinessException("该邀请码已被使用");
        }
        
        // 验证使用次数
        if (invitationCode.getUseCount() >= invitationCode.getMaxUseCount()) {
            throw new BusinessException("邀请码使用次数已达上限");
        }
        
        // 验证邮箱或手机是否匹配
        if (!email.equals(invitationCode.getBoundEmail()) && 
            !phone.equals(invitationCode.getBoundPhone())) {
            throw new BusinessException("邀请码与您的信息不匹配，请联系HR");
        }
    }
    
    // 使用成功后标记为已使用
    public void markAsUsed(String code, Long userId) {
        InvitationCode invitationCode = invitationCodeMapper.findByCode(code);
        invitationCode.setStatus("USED");
        invitationCode.setUseCount(invitationCode.getUseCount() + 1);
        invitationCode.setUsedTime(LocalDateTime.now());
        invitationCode.setUsedBy(userId);
        invitationCodeMapper.update(invitationCode);
    }
}
```

***

### 漏洞4：部门主管离职或调岗后的审批权限

**问题：**

* 审批流程进行中，审批人（部门主管）离职或调岗

* 审批流程卡住

**风险：**

* 流程无法继续

* 需要人工干预

**解决方案：**

```java
// 动态获取当前部门主管
public class DynamicApproverResolver {
    
    public User resolveCurrentApprover(String departmentId, String requiredRole) {
        // 实时查询当前部门主管（不是缓存的）
        User currentManager = userService.getCurrentManagerByDepartment(departmentId);
        
        if (currentManager == null) {
            // 部门主管空缺，升级到上级部门
            return findUpperLevelApprover(departmentId);
        }
        
        return currentManager;
    }
}

// 审批前验证审批人是否仍在职
public void validateApproverStatus(Long approverId) {
    User approver = userService.getById(approverId);
    
    if (!"ACTIVE".equals(approver.getStatus())) {
        throw new BusinessException("审批人已离职或调岗，请重新分配审批人");
    }
}
```

***

### 漏洞5：批量生成邀请码的权限控制

**问题：**

* 如果支持批量生成，HR可能批量创建大量邀请码

* 没有数量限制

**风险：**

* 资源浪费

* 安全隐患（大量未使用邀请码）

**解决方案：**

```java
// 批量生成限制
public class BatchGenerationLimiter {
    
    public void checkBatchLimit(Long hrId, int requestCount) {
        // 单次批量限制
        if (requestCount > 50) {
            throw new BusinessException("单次最多生成50个邀请码");
        }
        
        // 月度总量限制
        int monthlyCount = getMonthlyGenerationCount(hrId);
        if (monthlyCount + requestCount > 200) {
            throw new BusinessException("您本月邀请码生成额度已用完");
        }
    }
}
```

**注意：** 根据讨论，实际系统中不是HR手动批量生成，而是通过招聘流程逐个自动生成，此限制作为兜底保护。

***

### 漏洞6：审批历史篡改风险

**问题：**

* 审批记录可能被篡改

* 无法保证审批历史的真实性

**风险：**

* 事后无法追溯

* 责任不清

**解决方案：**

```java
// 审批记录防篡改
@Entity
@Table(name = "approval_record")
public class ApprovalRecord {
    
    // ... 其他字段
    
    @Column(name = "signature")
    private String signature;  // 数字签名
    
    @PrePersist
    public void prePersist() {
        // 生成数字签名
        this.signature = generateSignature();
    }
    
    private String generateSignature() {
        String data = this.archiveId + this.reviewerId + this.result + this.reviewTime;
        return DigestUtils.sha256Hex(data + SECRET_KEY);
    }
}
```

***

### 漏洞7：邀请码过期后的复活风险

**问题：**

* 过期邀请码被重新激活

* 没有严格的复活审批

**风险：**

* 过期邀请码被滥用

**解决方案：**

```java
// 过期邀请码复活需要严格审批
public class ExpiredCodeRevivalService {
    
    public void reviveExpiredCode(Long codeId, Long hrId, String reason) {
        // 1. 验证HR权限（只有HR负责人可以复活）
        User hr = userService.getById(hrId);
        if (!hr.hasRole("HR_HEAD")) {
            throw new BusinessException("只有HR负责人可以复活过期邀请码");
        }
        
        // 2. 记录复活原因
        // 3. 通知部门主管
        // 4. 设置新的有效期（缩短）
        // 5. 记录审计日志
    }
}
```

***

### 漏洞8：系统管理员的超级权限

**问题：**

* 系统管理员可以绕过所有审批

* 没有监督机制

**风险：**

* 管理员滥用职权

* 管理员账号被盗用

* 管理员长期不登录（睡眠账号）

**解决方案（六层保障体系）：**

#### 第一层：账号安全策略

```java
@Service
public class AdminAccountSecurityService {
    
    public void enforceSecurityPolicy(Long adminId) {
        // 1. 强制强密码
        if (!isStrongPassword(admin.getPassword())) {
            forcePasswordReset(adminId);
        }
        
        // 2. 强制双因素认证（2FA）
        if (!has2FAEnabled(adminId)) {
            forceEnable2FA(adminId);
        }
        
        // 3. 定期密码更换（每30天）
        if (isPasswordExpired(adminId)) {
            forcePasswordReset(adminId);
        }
    }
}
```

#### 第二层：登录保护

```java
public void loginProtection(String username, String ip) {
    // 1. 异地登录检测
    if (!isCommonLoginLocation(username, ip)) {
        sendSecurityAlert(username, ip, "异地登录");
        requireAdditionalVerification(username);
    }
    
    // 2. 异常时间登录检测（凌晨2-5点）
    if (isAbnormalLoginTime()) {
        sendSecurityAlert(username, ip, "异常时间登录");
        requireAdditionalVerification(username);
    }
    
    // 3. 多次失败锁定
    if (getFailedLoginCount(username) >= 3) {
        lockAccount(username, Duration.ofMinutes(30));
        notifyAdminTeam("管理员账号被锁定: " + username);
    }
}
```

#### 第三层：敏感操作多重确认

```java
public void beforeSensitiveOperation(Long adminId, String operationType) {
    // 1. 记录操作意图
    OperationIntent intent = recordOperationIntent(adminId, operationType);
    
    // 2. 发送确认码到管理员手机
    String verificationCode = generateVerificationCode();
    smsService.send(adminId, "您的确认码是: " + verificationCode + "，5分钟内有效");
    
    // 3. 同时发送邮件
    emailService.send(adminId, "敏感操作确认", "操作类型: " + operationType);
    
    // 4. 等待确认（5分钟超时）
    awaitConfirmation(intent.getId(), verificationCode);
}

// 超级敏感操作需要双人确认
public void requireDualApproval(String operationType, Long primaryAdminId) {
    // 1. 主管理员发起
    recordOperationRequest(primaryAdminId, operationType);
    
    // 2. 需要第二个管理员确认
    List<User> otherAdmins = getOtherActiveAdmins(primaryAdminId);
    
    // 3. 发送确认请求给其他管理员
    for (User otherAdmin : otherAdmins) {
        notificationService.send(otherAdmin.getId(), 
            "管理员" + primaryAdminId + "请求执行敏感操作: " + operationType + 
            "，请确认或拒绝");
    }
    
    // 4. 等待任一其他管理员确认（24小时超时）
    awaitDualApproval(operationType, primaryAdminId, Duration.ofHours(24));
}
```

#### 第四层：活跃度监控

```java
@Scheduled(cron = "0 0 9 * * ?")
public void checkAdminActivity() {
    List<User> admins = userService.getAllAdmins();
    
    for (User admin : admins) {
        LocalDateTime lastLogin = admin.getLastLoginTime();
        long daysSinceLastLogin = ChronoUnit.DAYS.between(lastLogin, LocalDateTime.now());
        
        // 7天未登录：发送提醒
        if (daysSinceLastLogin >= 7 && daysSinceLastLogin < 14) {
            sendInactivityReminder(admin, "您已7天未登录系统");
        }
        // 14天未登录：发送警告并通知其他管理员
        else if (daysSinceLastLogin >= 14 && daysSinceLastLogin < 30) {
            sendInactivityWarning(admin, "您已14天未登录，请尽快登录");
            notifyOtherAdmins("管理员" + admin.getUsername() + "已14天未登录");
        }
        // 30天未登录：自动冻结账号
        else if (daysSinceLastLogin >= 30) {
            freezeAdminAccount(admin, "长期未登录自动冻结");
            notifyAdminTeam("管理员" + admin.getUsername() + "账号已自动冻结");
            activateEmergencyContact(admin);
        }
    }
}
```

#### 第五层：账号恢复机制

```java
public void recoverFrozenAccount(Long frozenAdminId, Long recovererId) {
    // 1. 验证恢复人权限（必须是其他管理员或总经理）
    validateRecovererPermission(recovererId);
    
    // 2. 验证身份（多重验证）
    verifyIdentity(frozenAdminId);
    
    // 3. 强制密码重置
    forcePasswordReset(frozenAdminId);
    
    // 4. 强制重新绑定2FA
    forceRebind2FA(frozenAdminId);
    
    // 5. 解冻账号
    unfreezeAccount(frozenAdminId);
    
    // 6. 记录恢复日志
    auditLogService.logOperation("ACCOUNT_RECOVERY", recovererId, 
        "ADMIN", frozenAdminId, "FROZEN", "ACTIVE");
    
    // 7. 通知所有管理员
    notifyAllAdmins("管理员账号已恢复: " + frozenAdminId);
}

// 终极恢复：所有管理员都不可用
public void ultimateRecovery(String securityToken) {
    // 1. 验证安全令牌（预先保存在安全地方的物理令牌）
    if (!validateSecurityToken(securityToken)) {
        throw new SecurityException("安全令牌无效");
    }
    
    // 2. 创建临时超级管理员（24小时有效）
    User tempAdmin = createTemporarySuperAdmin();
    
    // 3. 临时管理员可以重建管理员团队
    // 4. 24小时后自动删除临时管理员
    scheduleTempAdminDeletion(tempAdmin.getId(), Duration.ofHours(24));
}
```

#### 第六层：物理安全保障

```java
@PostConstruct
public void systemStartupProtection() {
    // 1. 检查是否有至少2个活跃管理员
    long activeAdminCount = userService.countActiveAdmins();
    if (activeAdminCount < 2) {
        enterSafeMode();
        notifyAdminTeam("警告：活跃管理员少于2人，系统进入安全模式");
    }
    
    // 2. 检查管理员账号健康状态
    checkAdminAccountHealth();
}
```

***

### 漏洞9：审批流程循环风险

**问题：**

* 如果审批流程设计不当，可能出现A审B、B审C、C又审A的循环依赖

* 或者审批人被调岗后形成审批闭环

**风险：**

* 审批流程无限循环

* 责任不清

* 流程卡住无法继续

**解决方案：**

```java
// 防止审批循环
public class ApprovalCycleDetector {
    
    public void detectCycle(Long archiveId, Long newApproverId) {
        // 获取该档案已有的审批链
        List<ApprovalRecord> approvalChain = approvalService.getApprovalChain(archiveId);
        
        // 检查新审批人是否已经在审批链中
        Set<Long> existingApprovers = approvalChain.stream()
            .map(ApprovalRecord::getReviewerId)
            .collect(Collectors.toSet());
        
        if (existingApprovers.contains(newApproverId)) {
            throw new BusinessException("审批流程出现循环，请重新分配审批人");
        }
        
        // 检查审批深度（防止过长的审批链）
        if (approvalChain.size() >= 5) {
            throw new BusinessException("审批层级过多，请简化审批流程");
        }
    }
}
```

***

## 四、补充的安全措施

### 1. 操作日志完整性

* 所有操作必须记录：操作人、时间、IP、操作内容

* 日志不可删除、不可修改

* 定期备份日志

### 2. 敏感操作二次确认

* 生成邀请码：确认

* 撤销邀请码：二次确认

* 修改审批结果：需要更高权限审批

### 3. 定期权限审计

* 每月检查HR权限分配

* 检查是否有越权分配

* 清理离职人员权限

### 4. 异常操作监控

* 同一HR短时间内大量生成邀请码 → 告警

* 非正常时间操作 → 告警

* 异地登录操作 → 告警

### 5. 审批流程循环检测

* 实时检测审批链是否出现循环

* 限制审批层级不超过5层

* 自动阻止循环审批的发生

### 6. API限流保护

* 审批类接口：10次/秒

* 查询类接口：100次/秒

* 触发限流时返回429状态码

***

## 五、占位符清单

| 占位符           | 说明              | 对接系统    | 计划时间 |
| ------------- | --------------- | ------- | ---- |
| **自动化招聘系统对接** | 接收招聘触发信号，自动创建档案 | 自动化招聘系统 | 后续开发 |
| **财务系统对接**    | 发送邀请码发送费用记录     | 财务系统    | 后续对接 |

***

## 六、开发优先级

### 高优先级（第1-2周）

1. 入职档案管理页面
2. 审批流程引擎
3. 邀请码自动生成
4. HR权限限制
5. 审批人验证
6. 对外API接口框架
7. **API限流保护**（新增）

### 中优先级（第2-3周）

1. 邀请码绑定验证
2. 动态审批人解析
3. 过期监控与提醒
4. 异常处理界面
5. 给其他部门的审批API
6. **审批流程循环检测**（新增）

### 低优先级（第3-4周）

1. 审批记录防篡改
2. 过期邀请码复活审批
3. 系统管理员安全保障体系
4. 审计日志
5. HR系统数据接口

***

## 七、关键设计决策

1. **架构方案**：混合架构（半独立小岛形式）
2. **HR不主动生成邀请码**：系统自动生成，HR仅处理异常
3. **审批流程按职位级别**：不同级别不同流程
4. **过期提醒给主管**：不是给候选人
5. **薪资公平性监督**：HR负责监督，避免内部不公平
6. **审计日志全覆盖**：全流程可追溯
7. **系统管理员六层保护**：防止超级权限滥用
8. **对外API标准化**：便于其他部门接入
9. **审批流程防循环**：防止审批链出现闭环（新增）
10. **邀请码一次性使用**：防止重放攻击（新增）
11. **API限流保护**：防止接口被滥用（新增）

***

## 八、系统流程图

### 8.1 系统架构概览

```mermaid
graph TB
    subgraph "前端层"
        A1[档案管理页面<br/>OnboardingArchive.vue]
        A2[审批管理页面<br/>ApprovalManagement.vue]
        A3[邀请码管理页面<br/>InvitationCode.vue]
        A4[异常处理页面<br/>ExceptionHandler.vue]
        A5[状态管理<br/>onboarding.ts Pinia Store]
    end

    subgraph "后端控制器层"
        B1[档案控制器<br/>ArchiveController]
        B2[审批控制器<br/>ApprovalController]
        B3[邀请码控制器<br/>InvitationController]
        B4[对外API控制器<br/>OnboardingApprovalApi]
    end

    subgraph "服务层"
        C1[档案服务<br/>ArchiveService]
        C2[审批服务<br/>ApprovalService]
        C3[邀请码服务<br/>InvitationService]
        C4[权限检查服务<br/>HrPermissionChecker]
        C5[循环检测服务<br/>ApprovalCycleDetector]
    end

    subgraph "安全层"
        D1[HR权限验证<br/>HrPermissionAspect]
        D2[审批人验证<br/>ApprovalValidator]
        D3[API限流保护<br/>RateLimiter]
        D4[邀请码绑定验证<br/>InvitationCodeBinding]
    end

    subgraph "数据层"
        E1[(MySQL<br/>onboarding_archive)]
        E2[(MySQL<br/>approval_record)]
        E3[(MySQL<br/>invitation_send_record)]
        E4[(MySQL<br/>invitation_expense_record)]
        E5[(MySQL<br/>invitation_reminder_log)]
        E6[(Redis<br/>邀请码缓存)]
        E7[(Redis<br/>审批状态缓存)]
    end

    subgraph "外部系统"
        F1[自动化招聘系统<br/>（占位符）]
        F2[财务系统<br/>（占位符）]
        F3[其他部门审批系统]
        F4[邮件/短信服务]
    end

    A1 --> B1
    A2 --> B2
    A3 --> B3
    A5 --> A1
    A5 --> A2
    A5 --> A3

    B1 --> C1
    B2 --> C2
    B3 --> C3
    B4 --> C2

    C1 --> C4
    C2 --> C5
    C2 --> D2
    C3 --> D4

    C1 --> E1
    C2 --> E2
    C3 --> E3
    C3 --> E6
    C2 --> E7

    C1 -.-> F1
    C3 -.-> F4
    B4 -.-> F3
    C3 -.-> F2
```

***

### 8.2 入职档案创建详细流程

```mermaid
sequenceDiagram
    participant Hire as 自动化招聘系统
    participant HR as HR操作员
    participant Frontend as 前端<br/>OnboardingArchive.vue
    participant Store as Pinia Store<br/>onboarding.ts
    participant API as API层
    participant Controller as 档案控制器<br/>ArchiveController
    participant Service as 档案服务<br/>ArchiveService
    participant Permission as 权限检查<br/>HrPermissionChecker
    participant HRData as HR数据接口
    participant DB as MySQL数据库

    %% 自动化触发流程
    rect rgb(230, 245, 255)
        Note over Hire,DB: 自动化招聘系统触发流程
        Hire->>Controller: POST /api/onboarding/hire-integration/trigger
        Controller->>Service: createFromHireSystem(request)
        Service->>DB: INSERT INTO onboarding_archive (基础信息)
        DB-->>Service: 返回档案ID
        Service-->>Controller: 返回档案ID
        Controller-->>Hire: 档案已创建
    end

    %% HR补充信息流程
    rect rgb(255, 245, 230)
        Note over HR,DB: HR补充信息流程
        HR->>Frontend: 打开档案管理页面
        Frontend->>Store: 加载档案列表
        Store->>API: GET /api/onboarding/archives
        API->>Controller: 获取档案列表
        Controller->>Service: getArchiveList()
        Service->>DB: SELECT * FROM onboarding_archive
        DB-->>Service: 返回档案列表
        Service-->>Controller: 返回档案列表
        Controller-->>API: 返回200 OK
        API-->>Store: 保存档案列表
        Store-->>Frontend: 渲染档案列表
        Frontend-->>HR: 显示档案列表

        HR->>Frontend: 选择档案并补充信息
        Frontend->>HR: 显示编辑表单
        HR->>Frontend: 填写部门、职位、薪资等信息
        Frontend->>Store: 提交档案更新
        Store->>API: PUT /api/onboarding/archives/{id}
        API->>Controller: 传递UpdateArchiveRequest
        Controller->>Service: updateArchive()

        Service->>Permission: checkCreatePermission(hrId, positionLevel)
        Permission->>HRData: 获取HR信息
        HRData-->>Permission: 返回HR级别

        alt HR权限不足
            Permission-->>Service: 抛出异常 "无权创建该级别岗位"
            Service-->>Controller: 抛出BusinessException
            Controller-->>API: 返回403 Forbidden
            API-->>Store: 显示错误信息
            Store-->>Frontend: 提示权限不足
            Frontend-->>HR: 显示错误提示
        else HR权限正常
            Permission-->>Service: 权限检查通过
            Service->>DB: UPDATE onboarding_archive
            Note over Service,DB: 更新部门、职位、薪资、状态等信息
            DB-->>Service: 更新成功
            Service-->>Controller: 返回更新后的档案
            Controller-->>API: 返回200 OK
            API-->>Store: 更新本地状态
            Store-->>Frontend: 更新成功
            Frontend-->>HR: 显示成功提示
        end
    end
```

***

### 8.3 审批流程详细流程

```mermaid
sequenceDiagram
    participant HR as HR审查员
    participant Manager as 部门主管
    participant Frontend as 前端<br/>ApprovalManagement.vue
    participant API as API层
    participant Controller as 审批控制器<br/>ApprovalController
    participant Service as 审批服务<br/>ApprovalService
    participant Validator as 审批验证器<br/>ApprovalValidator
    participant CycleDetector as 循环检测器<br/>ApprovalCycleDetector
    participant DynamicResolver as 动态审批人解析<br/>DynamicApproverResolver
    participant DB as MySQL数据库

    %% 提交审批流程
    rect rgb(230, 255, 230)
        Note over HR,DB: HR形式审查流程
        HR->>Frontend: 提交档案审批
        Frontend->>API: POST /api/onboarding/approval/submit
        API->>Controller: submitForApproval(archiveId)
        Controller->>Service: submitApproval()

        Service->>Validator: validateApprover(archiveId, hrId)
        Validator->>DB: 查询档案信息
        DB-->>Validator: 返回档案

        alt 自己审批自己
            Validator-->>Service: 抛出异常 "不能审批自己创建的档案"
            Service-->>Controller: 抛出BusinessException
            Controller-->>API: 返回400 Bad Request
        else 审批人正常
            Validator-->>Service: 验证通过
            Service->>CycleDetector: detectCycle(archiveId, approverId)
            CycleDetector->>DB: 查询审批链
            DB-->>CycleDetector: 返回审批记录

            alt 检测到循环
                CycleDetector-->>Service: 抛出异常 "审批流程出现循环"
                Service-->>Controller: 抛出BusinessException
                Controller-->>API: 返回400 Bad Request
            else 无循环
                CycleDetector-->>Service: 检测通过
                Service->>DB: INSERT INTO approval_record
                Note over Service,DB: 记录HR形式审查
                DB-->>Service: 返回记录ID
                Service->>DB: UPDATE onboarding_archive SET status = 'PENDING_SUBSTANTIVE'
                Service-->>Controller: 提交成功
                Controller-->>API: 返回200 OK
                API-->>Frontend: 审批已提交
                Frontend-->>HR: 显示成功提示
            end
        end
    end

    %% 部门主管实质审查流程
    rect rgb(255, 245, 230)
        Note over Manager,DB: 部门主管实质审查流程
        Manager->>Frontend: 查看待审批列表
        Frontend->>API: GET /api/onboarding/approval/pending
        API->>Controller: getPendingApprovals(approverId)
        Controller->>Service: getPendingByApprover()
        Service->>DynamicResolver: resolveCurrentApprover(deptId, 'MANAGER')
        DynamicResolver->>DB: 查询当前部门主管
        DB-->>DynamicResolver: 返回主管信息

        alt 主管已离职/调岗
            DynamicResolver-->>Service: 返回上级部门审批人
            Service->>DB: 更新审批人为上级
        else 主管在职
            DynamicResolver-->>Service: 返回当前主管
        end

        Service->>DB: SELECT * FROM approval_record WHERE status = 'PENDING'
        DB-->>Service: 返回待审批列表
        Service-->>Controller: 返回待审批列表
        Controller-->>API: 返回200 OK
        API-->>Frontend: 渲染待审批列表
        Frontend-->>Manager: 显示待审批列表

        Manager->>Frontend: 选择档案并审批
        alt 审批通过
            Manager->>Frontend: 点击"通过"并填写意见
            Frontend->>API: POST /api/onboarding/approval/{id}/approve
            API->>Controller: approve(archiveId, approverId, comment)
            Controller->>Service: approve()
            Service->>Validator: validateApproverStatus(approverId)
            Validator->>DB: 查询审批人状态
            DB-->>Validator: 返回状态

            alt 审批人已离职
                Validator-->>Service: 抛出异常 "审批人已离职或调岗"
                Service-->>Controller: 抛出BusinessException
                Controller-->>API: 返回400 Bad Request
            else 审批人在职
                Validator-->>Service: 验证通过
                Service->>DB: INSERT INTO approval_record
                Note over Service,DB: 记录实质审查结果+数字签名
                Service->>DB: UPDATE onboarding_archive SET status = 'APPROVED'
                Service-->>Controller: 审批成功
                Controller-->>API: 返回200 OK
                API-->>Frontend: 审批已通过
                Frontend-->>Manager: 显示成功提示

                %% 触发邀请码生成
                Service->>Service: 触发邀请码自动生成
            end
        else 审批拒绝
            Manager->>Frontend: 点击"拒绝"并填写原因
            Frontend->>API: POST /api/onboarding/approval/{id}/reject
            API->>Controller: reject(archiveId, approverId, reason)
            Controller->>Service: reject()
            Service->>DB: INSERT INTO approval_record
            Service->>DB: UPDATE onboarding_archive SET status = 'REJECTED'
            Service-->>Controller: 拒绝成功
            Controller-->>API: 返回200 OK
            API-->>Frontend: 审批已拒绝
            Frontend-->>Manager: 显示拒绝成功
        end
    end
```

***

### 8.4 邀请码生成与发送流程

```mermaid
sequenceDiagram
    participant Service as 审批服务<br/>ApprovalService
    participant InvService as 邀请码服务<br/>InvitationService
    participant Binding as 绑定验证<br/>InvitationCodeBinding
    participant Generator as 邀请码生成器
    participant Sender as 发送服务<br/>InvitationSender
    participant Email as 邮件服务
    participant SMS as 短信服务
    participant DB as MySQL数据库
    participant Redis as Redis缓存
    participant Reminder as 提醒服务<br/>ReminderService

    rect rgb(230, 245, 255)
        Note over Service,Redis: 邀请码自动生成流程
        Service->>InvService: generateInvitationCode(archiveId)
        InvService->>DB: SELECT * FROM onboarding_archive WHERE id = ?
        DB-->>InvService: 返回档案信息

        InvService->>Generator: generateUniqueCode()
        Generator->>Generator: 生成唯一邀请码
        Generator-->>InvService: 返回邀请码

        InvService->>Binding: generateBoundInvitationCode(archive)
        Binding->>Binding: 绑定邮箱、手机、姓名
        Binding->>Binding: 设置使用次数限制(maxUseCount=1)
        Binding->>Binding: 设置状态为UNUSED
        Binding-->>InvService: 返回绑定后的邀请码对象

        InvService->>DB: INSERT INTO invitation_send_record
        Note over InvService,DB: 保存邀请码及绑定信息
        InvService->>Redis: SET invitation:code:{code} {data} EX 604800
        Note over InvService,Redis: 缓存7天
        InvService-->>Service: 邀请码生成成功
    end

    rect rgb(255, 245, 230)
        Note over Sender,Reminder: 自动发送流程
        Service->>Sender: sendInvitation(codeId, archive)
        Sender->>Sender: 准备邮件内容
        Sender->>Sender: 准备短信内容

        par 发送邮件
            Sender->>Email: 发送邀请邮件
            Email-->>Sender: 返回发送状态
        and 发送短信
            Sender->>SMS: 发送邀请短信
            SMS-->>Sender: 返回发送状态
        end

        alt 发送成功
            Sender->>DB: UPDATE invitation_send_record SET status = 'SENT'
            Sender->>Reminder: scheduleReminder(codeId, type='EXPIRING_24H', delay=6天)
            Note over Reminder: 设置24小时前提醒
            Sender->>Reminder: scheduleReminder(codeId, type='EXPIRED', delay=7天)
            Note over Reminder: 设置过期提醒
            Sender-->>Service: 发送成功
        else 发送失败
            Sender->>DB: UPDATE invitation_send_record SET status = 'FAILED', error_message = ?
            Sender-->>Service: 发送失败
            Service->>Service: 记录异常，等待HR处理
        end
    end

    rect rgb(255, 230, 245)
        Note over Reminder,DB: 提醒流程
        Reminder->>Reminder: 定时检查待提醒的邀请码
        Reminder->>DB: SELECT * FROM invitation_reminder_log WHERE send_time <= NOW()
        DB-->>Reminder: 返回待提醒列表

        loop 遍历每个待提醒邀请码
            Reminder->>DB: 查询部门主管信息
            DB-->>Reminder: 返回主管邮箱/手机
            Reminder->>Email: 发送提醒邮件给主管
            Reminder->>DB: UPDATE invitation_reminder_log SET send_status = 'SENT'
        end
    end
```

***

### 8.5 邀请码验证与注册流程

```mermaid
sequenceDiagram
    participant Candidate as 候选人
    participant RegPage as 注册页面<br/>Register.vue
    participant AuthAPI as 认证API<br/>auth.ts
    participant InvController as 邀请码控制器<br/>InvitationController
    participant InvService as 邀请码服务<br/>InvitationService
    participant Binding as 绑定验证<br/>InvitationCodeBinding
    participant Validator as 验证器
    participant DB as MySQL数据库
    participant Redis as Redis缓存
    participant UserService as 用户服务<br/>UserService

    Candidate->>RegPage: 访问注册页面
    RegPage->>Candidate: 显示注册表单
    Candidate->>RegPage: 输入邀请码、邮箱、手机、姓名
    Candidate->>RegPage: 点击"验证邀请码"

    RegPage->>AuthAPI: POST /api/auth/validate-invitation
    AuthAPI->>InvController: validateInvitation(code, email, phone, name)
    InvController->>InvService: validateInvitationCode()

    InvService->>Redis: GET invitation:code:{code}
    Redis-->>InvService: 返回缓存数据

    alt 缓存未命中
        InvService->>DB: SELECT * FROM invitation_send_record WHERE code = ?
        DB-->>InvService: 返回邀请码信息
        InvService->>Redis: SET invitation:code:{code} {data}
    end

    InvService->>Binding: validateBinding(code, email, phone, name)

    alt 邀请码已被使用
        Binding-->>InvService: 抛出异常 "该邀请码已被使用"
        InvService-->>InvController: 抛出BusinessException
        InvController-->>AuthAPI: 返回400 Bad Request
        AuthAPI-->>RegPage: 显示错误信息
        RegPage-->>Candidate: 提示"邀请码已被使用"
    else 使用次数超限
        Binding-->>InvService: 抛出异常 "邀请码使用次数已达上限"
        InvService-->>InvController: 抛出BusinessException
        InvController-->>AuthAPI: 返回400 Bad Request
        AuthAPI-->>RegPage: 显示错误信息
        RegPage-->>Candidate: 提示"邀请码使用次数已达上限"
    else 绑定信息不匹配
        Binding-->>InvService: 抛出异常 "邀请码与您的信息不匹配"
        InvService-->>InvController: 抛出BusinessException
        InvController-->>AuthAPI: 返回400 Bad Request
        AuthAPI-->>RegPage: 显示错误信息
        RegPage-->>Candidate: 提示"邀请码与您的信息不匹配，请联系HR"
    else 邀请码过期
        Binding-->>InvService: 抛出异常 "邀请码已过期"
        InvService-->>InvController: 抛出BusinessException
        InvController-->>AuthAPI: 返回400 Bad Request
        AuthAPI-->>RegPage: 显示错误信息
        RegPage-->>Candidate: 提示"邀请码已过期，请联系HR"
    else 验证通过
        Binding-->>InvService: 验证通过
        InvService-->>InvController: 返回档案信息
        InvController-->>AuthAPI: 返回200 OK
        AuthAPI-->>RegPage: 验证成功
        RegPage-->>Candidate: 显示注册表单（预填部门职位）

        Candidate->>RegPage: 填写密码、确认密码
        Candidate->>RegPage: 点击"注册"

        RegPage->>AuthAPI: POST /api/auth/register
        AuthAPI->>UserService: createUser(userInfo)
        UserService->>DB: INSERT INTO users
        DB-->>UserService: 返回用户ID

        UserService->>InvService: markAsUsed(code, userId)
        InvService->>Binding: markAsUsed(code, userId)
        Binding->>DB: UPDATE invitation_send_record SET status = 'USED', use_count = 1, used_by = ?, used_time = NOW()
        Binding->>Redis: DEL invitation:code:{code}
        Binding-->>InvService: 标记成功

        UserService->>DB: UPDATE onboarding_archive SET status = 'REGISTERED'
        UserService-->>AuthAPI: 注册成功
        AuthAPI-->>RegPage: 返回注册成功
        RegPage-->>Candidate: 显示注册成功，跳转到登录页
    end
```

***

### 8.6 异常处理流程

```mermaid
graph TB
    subgraph "异常检测"
        A1[定时任务扫描]
        A2[发送失败检测]
        A3[过期检测]
        A4[审批异常检测]
    end

    subgraph "异常分类"
        B1[发送失败]
        B2[邀请码过期]
        B3[审批人离职]
        B4[候选人未注册]
    end

    subgraph "HR介入处理"
        C1[HR查看异常列表]
        C2[选择处理方式]
    end

    subgraph "处理方式"
        D1[重新发送邀请码]
        D2[延长邀请码有效期]
        D3[重新分配审批人]
        D4[撤销邀请码]
        D5[联系候选人]
    end

    subgraph "处理结果"
        E1[更新数据库]
        E2[记录审计日志]
        E3[通知相关人员]
    end

    A1 --> A2
    A1 --> A3
    A1 --> A4

    A2 --> B1
    A3 --> B2
    A3 --> B4
    A4 --> B3

    B1 --> C1
    B2 --> C1
    B3 --> C1
    B4 --> C1

    C1 --> C2
    C2 --> D1
    C2 --> D2
    C2 --> D3
    C2 --> D4
    C2 --> D5

    D1 --> E1
    D2 --> E1
    D3 --> E1
    D4 --> E1
    D5 --> E1

    E1 --> E2
    E2 --> E3
```

***

### 8.7 对外API调用流程

```mermaid
sequenceDiagram
    participant External as 外部系统<br/>其他部门
    participant RateLimiter as 限流器<br/>RateLimiter
    participant API as 对外API<br/>OnboardingApprovalApi
    participant Auth as 权限验证
    participant Service as 审批服务<br/>ApprovalService
    participant DB as MySQL数据库
    participant Log as 审计日志

    External->>RateLimiter: 发送API请求<br/>GET /api/onboarding/approval/pending

    alt 超过限流阈值
        RateLimiter-->>External: 返回429 Too Many Requests
        Note over RateLimiter: 请求过于频繁，请稍后再试
    else 未超过限流
        RateLimiter->>API: 放行请求
        API->>Auth: 验证API密钥/Token

        alt 验证失败
            Auth-->>API: 验证失败
            API-->>External: 返回401 Unauthorized
        else 验证通过
            Auth-->>API: 验证通过
            API->>Service: 执行业务逻辑
            Service->>DB: 查询/更新数据
            DB-->>Service: 返回数据
            Service-->>API: 返回业务结果
            API->>Log: 记录API调用日志
            API-->>External: 返回200 OK + 数据
        end
    end

    %% 审批操作限流示例
    External->>RateLimiter: POST /api/onboarding/approval/{id}/approve

    alt 审批操作超过10次/秒
        RateLimiter-->>External: 返回429 Too Many Requests
    else 未超过限流
        RateLimiter->>API: 放行请求
        API->>Auth: 验证审批人权限
        Auth-->>API: 权限验证通过
        API->>Service: approve(archiveId, approverId, comment)
        Service->>DB: 更新审批记录
        Service->>DB: 更新档案状态
        Service-->>API: 审批成功
        API->>Log: 记录审批操作日志
        API-->>External: 返回200 OK
    end
```

***

### 8.8 系统管理员安全保障流程

```mermaid
sequenceDiagram
    participant Admin as 系统管理员
    participant Login as 登录系统
    participant Security as 安全服务<br/>AdminAccountSecurityService
    participant Monitor as 监控服务
    participant DB as 数据库
    participant Alert as 告警服务

    %% 登录保护
    rect rgb(255, 230, 230)
        Note over Admin,Alert: 登录安全保护
        Admin->>Login: 输入用户名密码
        Login->>Security: loginProtection(username, ip)

        alt 异地登录
            Security->>Alert: sendSecurityAlert(username, ip, "异地登录")
            Security->>Admin: 要求额外验证
        else 异常时间登录
            Security->>Alert: sendSecurityAlert(username, ip, "异常时间登录")
            Security->>Admin: 要求额外验证
        else 多次失败
            Security->>DB: lockAccount(username, 30分钟)
            Security->>Alert: notifyAdminTeam("管理员账号被锁定")
            Security-->>Admin: 账号已锁定
        else 登录正常
            Security-->>Login: 登录通过
        end
    end

    %% 敏感操作保护
    rect rgb(230, 255, 230)
        Note over Admin,Alert: 敏感操作多重确认
        Admin->>Login: 执行敏感操作
        Login->>Security: beforeSensitiveOperation(adminId, operationType)
        Security->>Security: 记录操作意图
        Security->>Admin: 发送短信确认码
        Security->>Admin: 发送邮件确认
        Admin->>Security: 输入确认码

        alt 超级敏感操作
            Security->>Security: requireDualApproval(operationType, adminId)
            Security->>Alert: 通知其他管理员确认
            Note over Alert: 等待第二个管理员确认（24小时）
        end
    end

    %% 活跃度监控
    rect rgb(230, 245, 255)
        Note over Monitor,Alert: 活跃度监控
        Monitor->>Monitor: 定时任务（每天9点）
        Monitor->>DB: 查询所有管理员最后登录时间
        DB-->>Monitor: 返回管理员列表

        loop 遍历每个管理员
            alt 7天未登录
                Monitor->>Admin: 发送提醒
            else 14天未登录
                Monitor->>Admin: 发送警告
                Monitor->>Alert: 通知其他管理员
            else 30天未登录
                Monitor->>DB: freezeAdminAccount(admin, "长期未登录")
                Monitor->>Alert: notifyAdminTeam("账号已自动冻结")
            end
        end
    end
```

***

**文档版本**：v2.2  
**创建日期**：2026-01-29  
**更新日期**：2026-01-31  
**架构方案**：混合架构（小岛形式 + 集成）
