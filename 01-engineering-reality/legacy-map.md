# 食品溯源系统遗留代码地图

## 概述
本文档详细记录了食品溯源系统中的遗留代码组件，包括废弃的数据库表、服务实现、控制器、接口、死代码、隐藏机制和直接数据库写入操作。

---

## 1. Legacy 数据库表

| 表名 | 描述 | 状态 |
|------|------|------|
| orders_legacy | 旧订单表 | PK=VARCHAR，已废弃 |
| food | 旧菜品表 | PK=VARCHAR，已废弃 |
| product | 遗留产品表 | 已废弃 |
| voucher_header | 死体系 | 完全废弃 |
| voucher_line | 死体系 | 完全废弃 |
| stores | 旧门店表 | 已废弃 |
| dish_combo | 旧套餐表 | 已废弃 |
| dish_recipe | 旧菜谱表 | 已废弃 |

---

## 2. Legacy 服务实现 (8个)

| 服务实现类 | 说明 |
|-----------|------|
| legacyAccountingSubjectServiceImpl | 遗留会计科目服务实现 |
| legacyCostAllocationServiceImpl | 遗留成本分配服务实现 |
| legacyElectronicVoucherServiceImpl | 遗留电子凭证服务实现 |
| legacyFinanceApprovalServiceImpl | 遗留财务审批服务实现 |
| legacyFinancePrintServiceImpl | 遗留财务打印服务实现 |
| legacyFinanceVoucherServiceImpl | 遗留财务凭证服务实现 |
| legacyFinanceReportServiceImpl | 遗留财务报告服务实现 |
| legacyPrintTemplateDataServiceImpl | Stub空实现，打印模板数据服务 |

---

## 3. Legacy Controller (13个硬件控制器全部废弃)

| 控制器 | 说明 |
|--------|------|
| WeighingDeviceController | 称重设备控制器 |
| TTSController | TTS语音控制器 |
| TakeoutLockerController | 外卖柜控制器 |
| SdkPrinterController | SDK打印机控制器 |
| ScanDeviceController | 扫描设备控制器 |
| PrintFormatTemplateController | 打印格式模板控制器 |
| LabelPrinterController | 标签打印机控制器 |
| HardwareInitController | 硬件初始化控制器 |
| HardwareDeviceController | 硬件设备控制器 |
| HardwareConfigVersionController | 硬件配置版本控制器 |
| HardwareConfigController | 硬件配置控制器 |
| EpsonPrinterTestController | 爱普生打印机测试控制器 |
| DeviceSimulatorController | 设备模拟器控制器 |
| DeviceCommunicationTestController | 设备通信测试控制器 |

---

## 4. Legacy 接口 (7个)

| 接口 | 说明 |
|------|------|
| AccountingSubjectService | 会计科目服务（根包） |
| CostAllocationService | 成本分配服务 |
| FinanceVoucherService | 财务凭证服务 |
| FinanceApprovalService | 财务审批服务 |
| FinanceReportService | 财务报告服务 |
| FinancePrintService | 财务打印服务 |
| ElectronicSignatureService | 电子签名服务 |

---

## 5. Dead Code

| 死代码位置 | 说明 |
|-----------|------|
| DatabaseFixConfig | 包含死方法 |
| TableInitConfig.createMemberTable() | 废弃的会员表创建方法 |
| PrintTemplateDataServiceImpl | Stub空实现 |
| OrderMapper | 包含12个 orders_legacy 统计方法 |
| 缓存空实现 | 7个DataService的缓存空实现 |

---

## 6. 隐藏 Fallback 机制

| 机制 | 说明 |
|------|------|
| OCR 引擎降级 | 当OCR引擎不可用时的降级处理 |
| 硬件不可用 fallback | 硬件设备不可用时的降级处理 |
| SMTP 降级 | 邮件服务降级处理 |
| 消息发送降级 | 消息发送服务降级处理 |
| 订单金额回退 | 订单金额计算回退机制 |
| 门店 ID 回退 | 门店ID获取回退机制 |

---

## 7. 直接数据库写入

| 位置 | 操作类型 | 说明 |
|------|----------|------|
| DatabaseFixConfig | JdbcTemplate DDL/DML | 使用JdbcTemplate执行DDL和DML操作 |
| TableInitConfig | 直接建表 | 直接创建数据库表 |
| DatabaseSchemaValidationConfig | 直接查询 | 直接查询数据库进行Schema验证 |

---

## 建议处理优先级

1. **高优先级**：Legacy 数据库表（特别是死体系表）和直接数据库写入操作
2. **中优先级**：Legacy 服务实现和接口
3. **低优先级**：废弃控制器、死代码和隐藏Fallback机制

---

## 注意事项

- 在移除任何遗留代码前，务必确认其是否仍被其他模块依赖
- 直接数据库写入操作应逐步替换为ORM操作
- 隐藏Fallback机制在移除前应评估其必要性
- 建议创建遗留代码移除计划，分阶段进行清理