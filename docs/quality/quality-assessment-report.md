# 食品溯源系统质量评估报告

> 评估日期：2026-07-22
> 评估范围：采购、仓储、库存、财务、人事、认证
> 评估结论：核心主链路已疏通，但大量模块仍存在表结构/实体不一致导致的隐性缺陷，需按优先级系统修复。

---

## 一、评估方法

1. **端到端脚本验证**：使用 PowerShell 脚本跑通采购-到货-收货-库存完整链路。
2. **接口探测**：对部门树、登录等关键 API 进行直接调用。
3. **日志扫描**：扫描 `food-traceability-error.log` 中的重复异常类型。
4. **代码与 Schema 比对**：检查 Entity、Mapper、Initializer、Flyway 脚本之间的一致性。

---

## 二、核心链路验证结果

### 2.1 采购→到货→收货→库存链路

| 步骤 | 验证结果 | 关键指标 |
|------|----------|----------|
| 登录 | 通过 | 返回有效 token |
| 创建供应商 | 通过 | supplierId=1 |
| 创建仓库 | 通过 | warehouseId=1 |
| 创建采购订单 | 通过 | orderId=1，code=PO202607220001 |
| 提交/审批/确认订单 | 通过 | 状态流转正常 |
| 创建到货单 | 通过 | arrivalId=1，供应商名称正确继承 |
| 获取到货明细 | 通过 | expectedQuantity=100.000 |
| 收货确认 | 通过 | confirmationId=1，qty=100，amount=30000 |
| 库存校验 | 通过 | 数量从 0→100，unitCost=300，batchNo=B20260722 |

**结论**：核心 P0 链路已可正常运行，收货确认数据不再空白，库存计算准确。

---

## 三、已修复问题清单

| 编号 | 问题描述 | 根因 | 修复文件 | 验证方式 |
|------|----------|------|----------|----------|
| FIX-001 | 收货确认"到货明细确认"数据空白 | 前端未加载到货明细即打开确认弹窗 | `useReceiptPage.ts`、`ReceiptConfirmationFormDialog.vue` | E2E 脚本 |
| FIX-002 | 到货单未继承供应商名称 | `PurchaseArrivalServiceImpl` 未设置 supplierName | `PurchaseArrivalServiceImpl.java` | E2E 脚本 |
| FIX-003 | `purchase_arrivals` 表缺失物流相关字段 | Initializer 建表语句未包含实体字段 | `PurchaseDatabaseInitializer.java`、`V20260721_001__purchase_arrival_restructuring.sql` | E2E 脚本 |
| FIX-004 | `payables` 表缺失应付扩展字段 | 表结构与 `Payable` 实体不一致 | `FinanceDatabaseInitializer.java`、`db/finance_tables.sql` | E2E 脚本 |
| FIX-005 | 采购计划部门下拉硬编码 | 前端写死 options | `PurchasePlan.vue` | 页面/API 验证 |
| FIX-006 | 组织架构 API 500 错误 | `DepartmentMapper` 使用旧列名；Initializer 表主键/时间字段与实体不一致 | `DepartmentMapper.java`、`BaseDatabaseInitializer.java` | API 验证 |
| FIX-007 | 组织架构 API 401 | SecurityConfig 未放行 | `SecurityConfig.java` | API 验证 |
| FIX-008 | `suppliers` 表缺失 `return_credit_balance` | 实体新增字段未同步到表 | `PurchaseDatabaseInitializer.java` | 供应商创建接口 |

---

## 四、待修复问题清单

| 编号 | 问题描述 | 模块 | 优先级 | 风险说明 |
|------|----------|------|--------|----------|
| TODO-001 | Dashboard 库存预警 SQL 报错：`warning_status` 字段不存在 | 库存/Dashboard | P1 | 首页加载异常，影响运营监控 |
| TODO-002 | 用户权限查询 SQL 类型错误：`status IN ('1','active')` 对 INTEGER 列 | 认证/权限 | P1 | 登录后权限加载失败，可能导致鉴权异常 |
| TODO-003 | 时间字段命名不统一（`created_at` vs `create_time`） | 全局 | P2 | 增加维护成本，易引发 SQL 错误 |
| TODO-004 | H2 开发_initializer 与 PostgreSQL Flyway 脚本不一致 | 全局 | P2 | 开发/生产表现不一致，发布风险高 |
| TODO-005 | 大量 Entity-Schema 差异未扫描 | 全局 | P2 | 随时可能在其他模块触发 500 |

---

## 五、模块质量评分

评分维度：功能完整性（40%）、数据准确性（30%）、稳定性（20%）、可维护性（10%）。

| 模块 | 功能完整性 | 数据准确性 | 稳定性 | 可维护性 | 综合评分 | 等级 |
|------|-----------|-----------|--------|----------|----------|------|
| 采购管理（主链路） | 35/40 | 28/30 | 16/20 | 7/10 | 86 | B+ |
| 仓储库存 | 28/40 | 22/30 | 12/20 | 6/10 | 68 | C |
| 财务中心 | 24/40 | 22/30 | 12/20 | 6/10 | 64 | C |
| 人事组织 | 22/40 | 18/30 | 10/20 | 5/10 | 55 | D |
| 认证权限 | 20/40 | 16/30 | 10/20 | 5/10 | 51 | D |

> 等级说明：A（90-100）可交付；B（80-89）基本可用；C（60-79）需修复；D（<60）存在阻断风险。

---

## 六、主要风险

1. **数据库 Schema 与实体不同步**：这是当前最高频的故障模式，已触发多次 500 错误。建议建立启动时自动扫描或单元测试。
2. **开发/生产数据库初始化路径不一致**：H2 使用 Java Initializer，PostgreSQL 使用 Flyway，两者字段差异较大。
3. **错误日志中存在重复异常**：日志文件已达 1.4MB，说明系统在多处持续报错，但未闭环修复。
4. **前端硬编码与状态映射未完全清理**：本次仅修复了采购计划部门下拉，其他模块可能仍存在类似问题。

---

## 七、优化建议

### 7.1 短期（1-2 周）

1. 修复 TODO-001、TODO-002 两个已知 P1 缺陷。
2. 对核心表（inventory、users、permissions、departments、payables、purchase_arrivals）做一次 Entity-Schema 一致性扫描。
3. 为采购计划创建、部门新增、应付账款付款补充 E2E 脚本。

### 7.2 中期（2-4 周）

1. 统一时间字段命名为 `create_time` / `update_time`（与项目规范一致）。
2. 将 H2 Initializer 与 Flyway 脚本对齐，或引入统一的 schema 校验工具。
3. 建立模块级接口契约测试，防止回归。

### 7.3 长期（1-2 月）

1. 引入数据库迁移版本管理，禁止直接修改已执行脚本。
2. 建立持续集成流水线，自动执行 `mvn compile`、`npm run build`、E2E 脚本。
3. 完善监控告警：接口错误率、慢查询、缓存命中率。

---

## 八、结论

本次围绕"收货确认"和"采购计划"两个核心问题的修复已取得明确成果：采购-到货-收货-库存主链路已通过端到端验证，采购计划部门下拉已能正常联动人事组织架构。

但系统整体仍处于"核心链路可用、周边模块脆弱"的状态。下一步需严格按照《系统质量提升计划》推进，优先修复 P1 缺陷，补齐 Schema-Entity 一致性，才能在短期内将系统品质提升至成品标准。
