# 门店健康证管理系统设计文档

## 1. 需求分析

### 1.1 业务背景
健康证是门店工作人员必须办理的证件，需悬挂在门店员工信息处以便检查，不直接交由公司保管。门店工作人员的健康证新增及报销需在门店进行提交申请处理，由人事部门审核确认新增申请，财务部门根据收到的发票审核报销。

### 1.2 核心需求
- 门店运营板块添加日常事务处理模块，便于门店办公
- 支持健康证新增申请、人事审核、财务报销全流程管理
- 支持健康证状态管理、到期提醒
- 支持报销申请、审核、财务处理
- 支持发票管理和合规要求

### 1.3 角色职责
| 角色 | 职责 |
|------|------|
| 门店工作人员 | 提交健康证申请、提交报销申请、查看健康证状态 |
| 门店负责人 | 审核门店内员工健康证申请、审核报销申请 |
| 人事部门 | 审核健康证信息、确认新增申请、管理健康证状态 |
| 财务部门 | 审核报销申请、处理费用支付、管理发票 |

## 2. 系统架构设计

### 2.1 四层架构设计
1. **前端应用层**：Vue 3 + Element Plus 构建的单页应用，负责用户交互和数据展示
2. **后端服务层**：Spring Boot 微服务，提供业务逻辑处理和API接口
3. **数据持久层**：MySQL 数据库，存储健康证、报销、员工等数据
4. **外部服务层**：文件存储服务（图片上传）、消息服务（到期提醒）

### 2.2 模块划分
| 模块 | 功能描述 |
|------|----------|
| 健康证管理模块 | 健康证申请、审核、状态管理、到期提醒 |
| 报销管理模块 | 报销申请、人事初审、财务审核、费用支付 |
| 员工管理模块 | 员工信息管理、与健康证关联 |
| 权限管理模块 | 基于角色的访问控制、数据权限 |
| 门店管理模块 | 门店信息管理、门店与员工关联 |

## 3. 功能模块设计

### 3.1 健康证管理模块

#### 3.1.1 实体设计
```java
// 健康证实体
public class HealthCertificate {
    private String id;
    private String employeeId;
    private String employeeName;
    private String store;
    private String certificateNumber;
    private String issueDate;
    private String expiryDate;
    private HealthCertificateStatus status;
    private String issuer;
    private String note;
    private ExpenseStatus expenseStatus;
    private ApprovalStatus approvalStatus;
    private String approvalBy;
    private String approvalDate;
    private String rejectReason;
    private String submissionDate;
    private String submittedBy;
    private String certificateImage;
    private String lastExpenseId;
    private String operator;
    private String operationTime;
    private Integer expiryDays;
}
```

#### 3.1.2 核心功能
- 健康证新增/编辑/查看/删除
- 健康证状态管理（有效、即将过期、已过期）
- 健康证审核流程（草稿 → 待审核 → 已通过/已拒绝）
- 健康证到期提醒
- 健康证图片上传与查看

#### 3.1.3 API接口
| 接口 | 功能 | 请求方式 | 权限要求 |
|------|------|----------|----------|
| /api/health-certificates | 获取健康证列表 | GET | ALL |
| /api/health-certificates/{id} | 获取健康证详情 | GET | ALL |
| /api/health-certificates | 创建健康证 | POST | STORE_STAFF |
| /api/health-certificates/{id} | 更新健康证 | PUT | STORE_STAFF |
| /api/health-certificates/{id}/submit | 提交审核 | POST | STORE_STAFF |
| /api/health-certificates/{id}/approve | 审核通过 | POST | HR |
| /api/health-certificates/{id}/reject | 审核拒绝 | POST | HR |

### 3.2 报销管理模块

#### 3.2.1 实体设计
```java
// 健康证报销实体
public class HealthCertificateExpense {
    private String id;
    private String healthCertificateId;
    private String employeeId;
    private String employeeName;
    private String store;
    private BigDecimal amount;
    private String invoiceType;
    private String invoiceNumber;
    private String invoiceDate;
    private List<String> invoiceAttachments;
    private String reason;
    private String note;
    private ExpenseStatus status;
    private String applyDate;
    private String approveDate;
    private String reimburseDate;
    private String rejectReason;
    private List<ApprovalRecord> approvalHistory;
    private ExpenseType expenseType;
    private String creator;
    private String createTime;
    private String updater;
    private String updateTime;
}
```

#### 3.2.2 核心功能
- 报销申请提交
- 人事初审
- 财务审核
- 费用支付
- 发票管理
- 审批历史记录

#### 3.2.3 API接口
| 接口 | 功能 | 请求方式 | 权限要求 |
|------|------|----------|----------|
| /api/health-certificate-expenses | 获取报销列表 | GET | ALL |
| /api/health-certificate-expenses/{id} | 获取报销详情 | GET | ALL |
| /api/health-certificate-expenses | 创建报销申请 | POST | STORE_STAFF |
| /api/health-certificate-expenses/{id}/hr-review | 人事初审 | POST | HR |
| /api/health-certificate-expenses/{id}/finance-review | 财务审核 | POST | FINANCE |
| /api/health-certificate-expenses/{id}/reimburse | 费用支付 | POST | FINANCE |

### 3.3 流程设计

#### 3.3.1 健康证申请流程
1. 门店工作人员创建健康证草稿
2. 门店负责人审核门店内申请
3. 提交健康证审核申请
4. 人事部门审核健康证信息
5. 审核通过，健康证生效
6. 审核拒绝，返回修改

#### 3.3.2 健康证报销流程
1. 门店工作人员提交报销申请
2. 门店负责人审核报销申请
3. 人事部门进行初审
4. 财务部门审核报销申请
5. 财务部门处理费用支付
6. 更新报销状态为已报销

#### 3.3.3 健康证到期提醒流程
1. 系统定时检查健康证到期情况
2. 对即将过期的健康证发送提醒
3. 门店工作人员收到提醒，准备新健康证
4. 提交新健康证申请

## 4. 前端设计

### 4.1 页面结构

#### 4.1.1 主页面布局
```vue
<template>
  <HRBaseLayout>
    <HealthCertificateToolbar
      @add="showAddDialog = true"
      @export="handleExport"
      @send-reminders="sendExpiryReminders"
      @settings="showSettingsDialog = true"
    />
    <HealthCertificateStats :certificates="healthCertificates" />
    <HealthCertificateList
      :certificates="filteredHealthCertificates"
      :loading="loading"
      @view="handleView"
      @edit="handleEdit"
      @approve="handleApprove"
      @apply-expense="handleApplyExpense"
    />
    
    <!-- 对话框组件 -->
    <HealthCertificateForm
      v-model="showAddDialog"
      mode="add"
      @submit="handleFormSubmit"
    />
    <!-- 其他对话框 -->
  </HRBaseLayout>
</template>
```

#### 4.1.2 组件拆分
| 组件名称 | 功能描述 | 文件路径 |
|---------|---------|---------|
| `HealthCertificateIndex` | 页面入口，负责组件组合和布局 | `views/HR/HealthCertificate/Index.vue` |
| `HealthCertificateList` | 健康证列表展示，包含筛选和分页 | `components/HR/HealthCertificate/List.vue` |
| `HealthCertificateStats` | 健康证统计卡片，展示各状态数量 | `components/HR/HealthCertificate/Stats.vue` |
| `HealthCertificateDetail` | 健康证详情查看 | `components/HR/HealthCertificate/Detail.vue` |
| `HealthCertificateForm` | 健康证新增/编辑表单 | `components/HR/HealthCertificate/Form.vue` |
| `HealthCertificateApproval` | 健康证审核组件 | `components/HR/HealthCertificate/Approval.vue` |
| `HealthCertificateExpense` | 健康证报销申请组件 | `components/HR/HealthCertificate/Expense.vue` |
| `HealthCertificateReminder` | 健康证到期提醒设置 | `components/HR/HealthCertificate/Reminder.vue` |
| `HealthCertificateToolbar` | 工具栏组件，包含新增、导出等操作 | `components/HR/HealthCertificate/Toolbar.vue` |

### 4.2 状态管理

#### 4.2.1 独立的健康证 Store
```typescript
// stores/healthCertificate.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { healthCertificateApi } from '@/api/hr';

export const useHealthCertificateStore = defineStore('healthCertificate', () => {
  // 状态定义
  const healthCertificates = ref<HealthCertificate[]>([]);
  const healthCertificateExpenses = ref<HealthCertificateExpense[]>([]);
  const loading = ref(false);
  const currentPage = ref(1);
  const pageSize = ref(20);
  const total = ref(0);
  
  // 计算属性
  const validCertCount = computed(() => 
    healthCertificates.value.filter(cert => cert.status === 'valid').length
  );
  const expiringCertCount = computed(() => 
    healthCertificates.value.filter(cert => cert.status === 'expiring').length
  );
  const expiredCertCount = computed(() => 
    healthCertificates.value.filter(cert => cert.status === 'expired').length
  );
  
  // 异步操作
  async function fetchHealthCertificates(params?: any) {
    loading.value = true;
    try {
      const response = await healthCertificateApi.getHealthCertificates(params);
      healthCertificates.value = response.records;
      total.value = response.total;
      return response;
    } catch (error) {
      console.error('获取健康证数据失败:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  }
  
  // 其他方法...
  
  return {
    // 状态
    healthCertificates,
    healthCertificateExpenses,
    loading,
    currentPage,
    pageSize,
    total,
    
    // 计算属性
    validCertCount,
    expiringCertCount,
    expiredCertCount,
    
    // 方法
    fetchHealthCertificates,
    // 其他方法...
  };
});
```

### 4.3 样式设计

#### 4.3.1 统一组件样式
```scss
/* health-certificate.scss */
.health-certificate {
  // 统一卡片样式
  .content-card {
    background-color: var(--el-bg-color);
    border-radius: var(--el-border-radius-lg);
    box-shadow: var(--el-box-shadow-light);
    margin-bottom: 20px;
    
    .card-header {
      padding: 16px 20px;
      border-bottom: 1px solid var(--el-border-color-light);
      font-weight: 600;
      font-size: 16px;
    }
    
    .card-content {
      padding: 20px;
    }
  }
  
  // 统一按钮样式
  .action-button {
    margin-right: 8px;
    font-size: 12px;
    padding: 4px 12px;
  }
  
  // 统一状态标签样式
  .status-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 10px;
  }
}
```

## 5. 数据库设计

### 5.1 核心表结构

#### 5.1.1 健康证表 (health_certificate)
| 字段名 | 数据类型 | 描述 |
|--------|----------|------|
| id | VARCHAR(32) | 主键ID |
| employee_id | VARCHAR(32) | 员工ID |
| employee_name | VARCHAR(50) | 员工姓名 |
| store | VARCHAR(100) | 门店 |
| certificate_number | VARCHAR(50) | 健康证号 |
| issue_date | DATE | 签发日期 |
| expiry_date | DATE | 到期日期 |
| status | VARCHAR(20) | 健康证状态 |
| issuer | VARCHAR(100) | 签发机构 |
| note | TEXT | 备注 |
| expense_status | VARCHAR(20) | 报销状态 |
| approval_status | VARCHAR(20) | 审核状态 |
| approval_by | VARCHAR(32) | 审核人 |
| approval_date | DATETIME | 审核日期 |
| reject_reason | TEXT | 拒绝理由 |
| submission_date | DATETIME | 提交日期 |
| submitted_by | VARCHAR(32) | 提交人 |
| certificate_image | VARCHAR(255) | 健康证图片 |
| last_expense_id | VARCHAR(32) | 最后报销ID |
| operator | VARCHAR(32) | 操作人 |
| operation_time | DATETIME | 操作时间 |
| expiry_days | INT | 剩余天数 |

#### 5.1.2 健康证报销表 (health_certificate_expense)
| 字段名 | 数据类型 | 描述 |
|--------|----------|------|
| id | VARCHAR(32) | 主键ID |
| health_certificate_id | VARCHAR(32) | 健康证ID |
| employee_id | VARCHAR(32) | 员工ID |
| employee_name | VARCHAR(50) | 员工姓名 |
| store | VARCHAR(100) | 门店 |
| amount | DECIMAL(10,2) | 金额 |
| invoice_type | VARCHAR(20) | 发票类型 |
| invoice_number | VARCHAR(50) | 发票号码 |
| invoice_date | DATE | 发票日期 |
| invoice_attachments | JSON | 发票附件 |
| reason | TEXT | 报销理由 |
| note | TEXT | 备注 |
| status | VARCHAR(20) | 报销状态 |
| apply_date | DATETIME | 申请日期 |
| approve_date | DATETIME | 审核日期 |
| reimburse_date | DATETIME | 报销日期 |
| reject_reason | TEXT | 拒绝理由 |
| approval_history | JSON | 审批历史 |
| expense_type | VARCHAR(20) | 报销类型 |
| creator | VARCHAR(32) | 创建人 |
| create_time | DATETIME | 创建时间 |
| updater | VARCHAR(32) | 更新人 |
| update_time | DATETIME | 更新时间 |

### 5.2 索引设计
- `health_certificate` 表：`employee_id`, `status`, `expiry_date`, `approval_status`
- `health_certificate_expense` 表：`health_certificate_id`, `status`, `employee_id`

## 6. 技术实现

### 6.1 后端技术栈
- Spring Boot 2.7.x
- MyBatis-Plus
- MySQL 8.0
- Redis（缓存、会话管理）
- MinIO（文件存储）
- RabbitMQ（消息队列，用于到期提醒）

### 6.2 前端技术栈
- Vue 3 + TypeScript
- Element Plus
- Pinia（状态管理）
- Axios（HTTP 客户端）
- Vue Router（路由管理）
- SCSS（样式管理）

### 6.3 安全设计

#### 6.3.1 权限管理
- 基于角色的访问控制（RBAC）
- 7种角色：系统管理员、HR管理员、财务管理员、门店负责人、门店工作人员、审计人员、查看人员
- 细粒度权限控制：菜单权限、按钮权限、数据权限

#### 6.3.2 数据安全
- 敏感数据加密存储（如身份证号、银行卡号）
- API接口参数验证
- 防止SQL注入、XSS攻击
- 数据备份和恢复机制

#### 6.3.3 审计日志
- 记录所有关键操作
- 包含操作人、操作时间、操作内容、IP地址等信息
- 支持日志查询和导出

### 6.4 性能优化

#### 6.4.1 后端优化
- 数据库查询优化（索引、分页、避免N+1查询）
- 缓存机制（Redis 缓存热门数据）
- 异步处理（消息队列处理耗时操作）
- 连接池优化

#### 6.4.2 前端优化
- 虚拟滚动（处理大量数据列表）
- 数据缓存（本地存储常用数据）
- 组件懒加载
- 图片懒加载
- 代码分割和按需加载

## 7. 代码质量标准

### 7.1 组件拆分原则
- 单一职责原则：每个组件只负责一个明确的功能
- 高内聚低耦合：相关代码紧密组织，不相关代码分离
- 可复用性：提取通用组件，避免重复开发
- 可维护性：代码结构清晰，易于理解和修改

### 7.2 TypeScript 类型定义
- 完善的接口定义
- 严格的类型检查
- 避免 any 类型
- 泛型使用

### 7.3 注释规范
- 组件注释：描述组件功能、属性、事件
- 函数注释：描述函数功能、参数、返回值
- 复杂逻辑注释：解释复杂业务逻辑
- 代码变更注释：记录代码变更原因

### 7.4 错误处理
- 统一的错误处理机制
- 友好的错误提示
- 详细的错误日志
- 错误恢复机制

### 7.5 测试规范
- 单元测试：覆盖核心业务逻辑
- 集成测试：测试模块间交互
- E2E测试：测试完整业务流程
- 代码覆盖率要求：≥80%

## 8. 实施计划

### 8.1 项目阶段

#### 8.1.1 需求分析与设计阶段（1-2周）
- 需求确认
- 系统架构设计
- 数据库设计
- API接口设计
- 前端原型设计

#### 8.1.2 核心功能开发阶段（4-6周）
- 后端服务开发
- 前端页面开发
- 数据库实现
- 核心功能实现

#### 8.1.3 测试与优化阶段（2-3周）
- 单元测试
- 集成测试
- E2E测试
- 性能测试
- 安全测试
- Bug修复和优化

#### 8.1.4 上线与培训阶段（1-2周）
- 系统部署
- 数据迁移
- 用户培训
- 上线支持

### 8.2 关键里程碑
- 第2周：完成系统设计文档
- 第4周：完成后端核心服务开发
- 第6周：完成前端核心页面开发
- 第8周：完成系统集成测试
- 第10周：系统上线

## 9. 系统集成

### 9.1 与现有系统集成
- 人事管理系统：员工信息同步
- 财务管理系统：报销数据同步
- 门店运营系统：门店信息同步
- 消息通知系统：发送提醒消息

### 9.2 集成方式
- RESTful API 接口
- 消息队列（异步通信）
- 定时任务（数据同步）

## 10. 运维与监控

### 10.1 部署架构
- 多环境部署：开发环境、测试环境、预生产环境、生产环境
- 容器化部署：Docker + Kubernetes
- 负载均衡：Nginx
- 高可用设计：集群部署、故障转移

### 10.2 监控告警
- 系统监控：CPU、内存、磁盘、网络
- 应用监控：QPS、响应时间、错误率
- 数据库监控：连接数、查询性能、慢查询
- 告警机制：邮件、短信、企业微信告警

### 10.3 日志管理
- 集中式日志管理：ELK Stack
- 日志分级：DEBUG、INFO、WARN、ERROR
- 日志查询和分析
- 日志保留策略

## 11. 总结

门店健康证管理系统是一个完整的业务流程管理系统，涵盖健康证申请、审核、报销全流程。系统采用前后端分离架构，具有良好的扩展性和可维护性。通过该系统，可以实现健康证的全生命周期管理，提高门店运营效率，降低管理成本，确保食品安全和合规性。

系统设计遵循了单一职责原则、高内聚低耦合原则、可维护性优先原则和性能优化原则，采用了现代化的技术栈和开发规范，确保系统的高质量和可靠性。

在实施过程中，需要严格按照实施计划执行，注重测试和质量控制，确保系统顺利上线和稳定运行。同时，需要提供全面的用户培训和技术支持，确保用户能够熟练使用系统，充分发挥系统的价值。