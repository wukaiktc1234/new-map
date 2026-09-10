# 食品溯源系统 - 事件/任务地图

> 本文档记录系统中所有领域事件、事件监听器、调度器、定时任务及营销调度器的全貌。

---

## 一、领域事件（22 个）

### 1. 订单相关（4 个）

| 事件名称 | 说明 | 关联监听器 |
| --- | --- | --- |
| `OrderCreatedEvent` | 订单创建 | `OrderCreatedEventListener` |
| `OrderCompletedEvent` | 订单完成 | `OrderCompletedEventListener` |
| `OrderRefundEvent` | 订单退款 | `OrderRefundEventListener` |
| `MemberLevelUpgradedEvent` | 会员等级升级 | — |

### 2. 采购/库存（2 个）

| 事件名称 | 说明 | 关联监听器 |
| --- | --- | --- |
| `PurchaseStockInEvent` | 采购入库 | `PurchaseStockInEventListener` |
| `ReceiptConfirmationCompletedEvent` | 收货确认完成 | `ReceiptConfirmationEventListener` |

### 3. 门店/薪资（2 个）

| 事件名称 | 说明 | 关联监听器 |
| --- | --- | --- |
| `StoreDailySettlementCompletedEvent` | 门店日结完成 | `StoreDailySettlementEventListener` |
| `SalaryPaidEvent` | 薪资发放 | `SalaryPaidEventListener` |

### 4. 追溯码（1 个）

| 事件名称 | 说明 |
| --- | --- |
| `TraceCodeGeneratedEvent` | 追溯码生成 |

### 5. HR（8 个）

| 事件名称 | 说明 |
| --- | --- |
| `DepartmentCreatedEvent` | 部门创建 |
| `DepartmentDeletedEvent` | 部门删除 |
| `EmployeeCreatedEvent` | 员工创建 |
| `EmployeeDeletedEvent` | 员工删除 |
| `EmployeeResignedEvent` | 员工离职 |
| `EmployeeTransferredEvent` | 员工调岗 |
| `PositionCreatedEvent` | 岗位创建 |
| `PositionDeletedEvent` | 岗位删除 |

> HR 事件统一由 `HREventListener` 监听，用于更新员工计数等统计信息。

### 6. 健康证（2 个）

| 事件名称 | 说明 | 关联监听器 |
| --- | --- | --- |
| `HealthCertificateEvent` | 健康证变更 | `HealthCertificateEventListener` |
| `HealthCertificateApprovedEvent` | 健康证审批通过 | `HealthCertificateEventListener` |

### 7. 财务报销（1 个）

| 事件名称 | 说明 | 关联监听器 |
| --- | --- | --- |
| `InvoiceReimbursementApprovedEvent` | 发票报销审批通过 | `InvoiceReimbursementApprovedEventListener` |

### 8. 财务领域（3 个）

| 事件名称 | 说明 |
| --- | --- |
| `VoucherPostedEvent` | 凭证过账 |
| `PaymentCompletedEvent` | 付款完成 |
| `BudgetExceededEvent` | 预算超支 |

### 9. 通知类（3 个）

| 事件名称 | 说明 |
| --- | --- |
| `BusinessEvent` | 通用业务事件 |
| `RecruitmentNotificationEvent` | 招聘通知 |
| `TestBusinessEvent` | 测试业务事件 |

---

## 二、事件监听器（10 个）

| 监听器 | 触发事件 | 职责 |
| --- | --- | --- |
| `OrderCreatedEventListener` | `OrderCreatedEvent` | 订单创建后锁定原料 |
| `OrderCompletedEventListener` | `OrderCompletedEvent` | 订单完成后记录财务收入 |
| `OrderRefundEventListener` | `OrderRefundEvent` | 退款后生成退款支出 |
| `PurchaseStockInEventListener` | `PurchaseStockInEvent` | 采购入库后生成追溯码 |
| `ReceiptConfirmationEventListener` | `ReceiptConfirmationCompletedEvent` | 收货确认后生成追溯码 |
| `SalaryPaidEventListener` | `SalaryPaidEvent` | 薪资发放后生成凭证 |
| `StoreDailySettlementEventListener` | `StoreDailySettlementCompletedEvent` | 日结后生成凭证 |
| `InvoiceReimbursementApprovedEventListener` | `InvoiceReimbursementApprovedEvent` | 报销审批后生成凭证 |
| `HealthCertificateEventListener` | `HealthCertificateEvent` / `HealthCertificateApprovedEvent` | 健康证变更后同步 HR |
| `HREventListener` | `DepartmentCreatedEvent` / `DepartmentDeletedEvent` / `EmployeeCreatedEvent` / `EmployeeDeletedEvent` / `EmployeeResignedEvent` / `EmployeeTransferredEvent` / `PositionCreatedEvent` / `PositionDeletedEvent` | HR 事件后更新员工计数 |

---

## 三、调度器（5 个）

| 调度器 | 职责 | 调度频率 |
| --- | --- | --- |
| `MonthlyClosingScheduler` | 月末结账 | 每月 |
| `BudgetCheckScheduler` | 预算执行检查 | 定期 |
| `AutoBackupScheduler` | 自动备份 | 定期 |
| `BackupTaskScheduler` | 定时数据备份 | 定期 |
| `BackupHealthChecker` | 备份健康检查 | 定期 |

---

## 四、定时任务（6 个）

| 任务 | 职责 | 调度频率 |
| --- | --- | --- |
| `OrderTimeoutTask` | 超时订单自动取消 | 定期 |
| `PurchaseOrderCleanupTask` | 采购订单清理 | 定期 |
| `CertificateExpiryCheckTask` | 证件到期预警 | 定期 |
| `HealthCertificateReminderTask` | 健康证到期提醒 | 定期 |
| `DailySettlementTask` | 日结对账生成 | 每日 |
| `AssetDepreciationScheduler` | 资产折旧 | 定期 |

---

## 五、营销调度器（4 个）

| 调度器 | 职责 | 调度频率 |
| --- | --- | --- |
| `RFMCalculateScheduler` | RFM 模型月度计算 | 每月 |
| `PointsExpireScheduler` | 积分过期清理 | 定期 |
| `CouponExpireScheduler` | 优惠券过期处理 | 定期 |
| `ChurnWarningScheduler` | 流失预警 | 定期 |

---

## 六、事件流转关系

```
订单领域:
  OrderCreatedEvent ──► OrderCreatedEventListener ──► 锁定原料
  OrderCompletedEvent ──► OrderCompletedEventListener ──► 记录财务收入
  OrderRefundEvent ──► OrderRefundEventListener ──► 生成退款支出

采购/库存领域:
  PurchaseStockInEvent ──► PurchaseStockInEventListener ──► 生成追溯码
  ReceiptConfirmationCompletedEvent ──► ReceiptConfirmationEventListener ──► 生成追溯码

门店/薪资领域:
  StoreDailySettlementCompletedEvent ──► StoreDailySettlementEventListener ──► 生成凭证
  SalaryPaidEvent ──► SalaryPaidEventListener ──► 生成凭证

财务报销领域:
  InvoiceReimbursementApprovedEvent ──► InvoiceReimbursementApprovedEventListener ──► 生成凭证

健康证领域:
  HealthCertificateEvent / HealthCertificateApprovedEvent ──► HealthCertificateEventListener ──► 同步HR

HR领域:
  DepartmentCreatedEvent / DepartmentDeletedEvent / EmployeeCreatedEvent /
  EmployeeDeletedEvent / EmployeeResignedEvent / EmployeeTransferredEvent /
  PositionCreatedEvent / PositionDeletedEvent
    ──► HREventListener ──► 更新员工计数
```

---

## 七、统计概览

| 分类 | 数量 |
| --- | --- |
| 领域事件 | 22 |
| 事件监听器 | 10 |
| 调度器 | 5 |
| 定时任务 | 6 |
| 营销调度器 | 4 |
| **合计** | **47** |
