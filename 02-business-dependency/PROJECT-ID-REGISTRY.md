# PROJECT-ID-REGISTRY: 全项目唯一 ID 注册表

> **版本**: 1.0.0  
> **生成日期**: 2026-09-09  
> **状态**: ACTIVE  
> **维护者**: Architecture Team  
> **作用域**: 食品溯源系统 (Food Traceability System) 全域 ID 管理

---

## 一、Registry 概览

| 指标 | 数量 | 说明 |
|------|------|------|
| **LOCK 系列** | 16 | 已锁定决策基线 (A: 10, B: 6) |
| **DEC 系列** | 14 | 架构/产品/安全决策 |
| **IMPLICIT 系列** | 6 | 隐含决策待识别 |
| **REQ 系列** | 17 | 业务需求基线 (MASTER:5, DATA:5, CORE:3, SEC:2, CROSS:2) |
| **ARCH 系列** | 16 | 架构原则 (8类×2) |
| **BC 系列** | 31 | 断裂链路问题 |
| **RC 系列** | 17 | 根因候选 |
| **SR 系列** | 17 | 系统修复候选 |
| **CONFLICT 系列** | 16 | 冲突注册表 |
| **FND 系列** | 13 | Foundation 事实 |
| **MDS 系列** | 5 | Master Data 事实 |
| **CAP 系列** | 5 | Capability 事实 |
| **CNF 系列** | 10 | Conflict 事实 |
| **STAT 系列** | 11 | 统计事实 |
| **总计** | **194** | |

---

## 二、ID 分类定义

| 分类 | 含义 | 使用场景 |
|------|------|----------|
| **FACT** | 已验证事实 | 代码/DB/API 已证明的客观事实 |
| **LOCKED** | 已锁定决策 | 已有充分证据、不可轻易变更的基线 |
| **OPEN** | 开放决策 | 待决策、需评审的事项 |
| **RECOMMENDED** | 推荐方案 | 已有推荐方案但未最终确认 |
| **CONFIRMED** | 已确认 | 已通过评审确认但未锁定 |
| **INVALIDATED** | 已废止 | 已识别冲突或冗余，永久保留不再复用 |
| **ENGINEERING_READY** | 工程就绪 | 可立即进入开发实施 |
| **BLOCKED** | 被阻断 | 依赖其他决策，当前无法推进 |

---

## 三、LOCK 系列 (已锁定决策基线)

### 3.1 基线 A: 业务数据真相源

| ID | 标题 | 状态 | 分类 | 证据来源 |
|----|------|------|------|----------|
| LOCK-A001 | foods 为菜品唯一真相源 | VERIFIED | LOCKED | foods 表, FoodService |
| LOCK-A002 | material_archives 为物料真相源 | VERIFIED | LOCKED | material_archives 表, MaterialService |
| LOCK-A003 | 金额以分为准 | PARTIAL | LOCKED | 新表 integer, 旧表 decimal 未修正 |
| LOCK-A004 | suppliers 为供应商真相源 | VERIFIED | LOCKED | suppliers 表, SupplierService |
| LOCK-A005 | employees 为员工真相源 | VERIFIED | LOCKED | employees 表, EmployeeService |
| LOCK-A006 | stores_new 为门店唯一真相源 | VERIFIED | LOCKED | stores_new 表, StoreService |
| LOCK-A007 | departments 为组织真相源 | VERIFIED | LOCKED | departments 表, DepartmentService |
| LOCK-A008 | positions 为职位真相源 | VERIFIED | LOCKED | positions 表, PositionService |
| LOCK-A009 | accounting_subjects 为科目真相源 | VERIFIED | LOCKED | accounting_subjects 表, AccountingService |
| LOCK-A010 | roles/permissions 为权限真相源 | VERIFIED | LOCKED | roles/permissions 表, AuthService |

### 3.2 基线 B: 技术架构

| ID | 标题 | 状态 | 分类 | 证据来源 |
|----|------|------|------|----------|
| LOCK-B001 | 事件驱动架构 (WebSocket + Event Outbox) | VERIFIED | LOCKED | websocket_manager.py, event_dispatcher.py |
| LOCK-B002 | PostgreSQL 主存储 | VERIFIED | LOCKED | database.yaml, 所有 ORM 模型 |
| LOCK-B003 | ~~Kubernetes~~ | UNVERIFIED | INVALIDATED | 未在代码库中验证 |
| LOCK-B004 | OAuth2+JWT | VERIFIED | LOCKED | auth_service.py, jwt_middleware.py |
| LOCK-B005 | ~~React + TypeScript~~ | UNVERIFIED | INVALIDATED | 前端为 Vue.js，非 React |
| LOCK-B006 | RESTful API | VERIFIED | LOCKED | api/ 目录, openapi.yaml |

### 3.3 基线冲突识别

| 冲突 ID | 冲突描述 | 涉及 ID | 解决方案 |
|---------|----------|---------|----------|
| LOCK-CONFLICT-001 | 三套 LOCK 基线使用同一编号但含义不同 | LOCK-A*, LOCK-B* | 已通过后缀 A/B 区分 |
| LOCK-CONFLICT-002 | LOCK-B003 (K8s) 与代码库不符 | LOCK-B003 | 标记 INVALIDATED |
| LOCK-CONFLICT-003 | LOCK-B005 (React) 与实际 Vue.js 不符 | LOCK-B005 | 标记 INVALIDATED |

---

## 四、DEC 系列 (决策)

### 4.1 已确认决策

| ID | 标题 | 状态 | 分类 | 优先级 |
|----|------|------|------|--------|
| DEC-006 | Product-Food-Material 关系决策 | CONFIRMED | CONFIRMED | P0 |
| DEC-007 | Order State Machine 决策 | CONFIRMED | CONFIRMED | P0 |
| DEC-008 | Amount/Money Contract 决策 | CONFIRMED | CONFIRMED | P0 |
| DEC-009 | Data Ownership 决策 | CONFIRMED | CONFIRMED | P0 |

### 4.2 开放决策

| ID | 标题 | 状态 | 分类 | 优先级 | 截止日期 |
|----|------|------|------|--------|----------|
| DEC-001 | Warehouse 管理模式决策 | PENDING_PRODUCT | OPEN | P0 | 2026-09-30 |
| DEC-004 | Customer-Member 关系决策 | PENDING_PRODUCT | OPEN | P0 | 2026-09-30 |
| DEC-010 | Cross-Domain Access 决策 | PENDING_ARCHITECTURE | OPEN | P0 | 2026-09-30 |
| DEC-011 | Event Architecture 决策 | PENDING_ARCHITECTURE | OPEN | P0 | 2026-09-30 |
| DEC-013 | Authentication Strategy 决策 | PENDING_SECURITY | OPEN | P1 | 2026-10-15 |
| DEC-014 | Authorization Model 决策 | PENDING_SECURITY | OPEN | P1 | 2026-10-15 |

### 4.3 工程执行决策

| ID | 标题 | 状态 | 分类 | 优先级 |
|----|------|------|------|--------|
| DEC-002 | Unit 管理模式决策 | PENDING_ENGINEERING | ENGINEERING_READY | P1 |
| DEC-003 | Payment Method 决策 | PENDING_ENGINEERING | ENGINEERING_READY | P1 |
| DEC-005 | Price Decision 决策 | PENDING_ENGINEERING | ENGINEERING_READY | P1 |
| DEC-012 | Inventory Location 决策 | PENDING_ENGINEERING | ENGINEERING_READY | P1 |

### 4.4 冲突识别

| 冲突 ID | 冲突描述 | 涉及 ID | 状态 |
|---------|----------|---------|------|
| DEC-CONFLICT-001 | DEC-008 与 LOCK-003 冲突 (金额单位) | DEC-008, LOCK-A003 | 已解决: LOCK-A003 覆盖 DEC-008 |
| DEC-CONFLICT-002 | DEC-007 与 DEC-008 编号重叠风险 | DEC-007, DEC-008 | 无冲突，不同决策 |

---

## 五、IMPLICIT 系列 (隐含决策)

| ID | 标题 | 状态 | 分类 | 与 DEC 冗余 |
|----|------|------|------|-------------|
| IMPLICIT-001 | Product/Food/Material Boundary 决策 | PENDING_IDENTIFICATION | INVALIDATED | 冗余 DEC-006 |
| IMPLICIT-002 | Order State Machine 决策 | PENDING_IDENTIFICATION | INVALIDATED | 冗余 DEC-007 |
| IMPLICIT-003 | Amount/Money Contract 决策 | PENDING_IDENTIFICATION | INVALIDATED | 冗余 DEC-008 |
| IMPLICIT-004 | Data Ownership 决策 | PENDING_IDENTIFICATION | INVALIDATED | 冗余 DEC-009 |
| IMPLICIT-005 | Cross-Domain Access 决策 | PENDING_IDENTIFICATION | INVALIDATED | 冗余 DEC-010 |
| IMPLICIT-006 | Event Architecture 决策 | PENDING_IDENTIFICATION | INVALIDATED | 冗余 DEC-011 |

### 冲突说明

IMPLICIT-001~006 与 DEC 系列存在完全冗余：
- IMPLICIT-001 ↔ DEC-006 (Product/Food/Material)
- IMPLICIT-002 ↔ DEC-007 (Order State Machine)
- IMPLICIT-003 ↔ DEC-008 (Amount/Money)
- IMPLICIT-004 ↔ DEC-009 (Data Ownership)
- IMPLICIT-005 ↔ DEC-010 (Cross-Domain Access)
- IMPLICIT-006 ↔ DEC-011 (Event Architecture)

**决策**: IMPLICIT 系列标记为 INVALIDATED，永久保留不再复用。所有隐含决策统一由 DEC 系列管理。

---

## 六、REQ 系列 (业务需求基线)

### 6.1 Master Data 需求

| ID | 标题 | 状态 | 分类 | 优先级 |
|----|------|------|------|--------|
| REQ-MASTER-001 | 供应商业务身份必须来源于 Canonical Supplier Master | VERIFIED | CONFIRMED | P0 |
| REQ-MASTER-002 | 商品业务身份必须来源于 Canonical Product/Food Master | VERIFIED | CONFIRMED | P0 |
| REQ-MASTER-003 | 物料业务身份必须来源于 Canonical Material Master | VERIFIED | CONFIRMED | P0 |
| REQ-MASTER-004 | 员工业务身份必须来源于 Canonical Employee Master | VERIFIED | CONFIRMED | P0 |
| REQ-MASTER-005 | 门店业务身份必须来源于 Canonical Store Master | VERIFIED | CONFIRMED | P0 |

### 6.2 Data 需求

| ID | 标题 | 状态 | 分类 | 优先级 |
|----|------|------|------|--------|
| REQ-DATA-001 | 跨域不得重新生成 Canonical Business Identity | VERIFIED | CONFIRMED | P0 |
| REQ-DATA-002 | Dialog/Form 必须继承正确的 Canonical Identity | VERIFIED | CONFIRMED | P1 |
| REQ-DATA-003 | 历史交易 Snapshot 必须有明确的数据语义 | VERIFIED | CONFIRMED | P1 |
| REQ-DATA-004 | 金额单位必须统一（分或元） | VERIFIED | CONFIRMED | P0 |
| REQ-DATA-005 | 状态机必须统一（不能三套并存） | VERIFIED | CONFIRMED | P0 |

### 6.3 Core 需求

| ID | 标题 | 状态 | 分类 | 优先级 |
|----|------|------|------|--------|
| REQ-CORE-001 | 客户端是业务入口，不等于 Truth Owner | VERIFIED | CONFIRMED | P0 |
| REQ-CORE-002 | Truth Source 必须唯一 | VERIFIED | CONFIRMED | P0 |
| REQ-CORE-003 | 写入操作必须通过 Service 层 | VERIFIED | CONFIRMED | P0 |

### 6.4 Security 需求

| ID | 标题 | 状态 | 分类 | 优先级 |
|----|------|------|------|--------|
| REQ-SEC-001 | 所有 API 必须有权限保护 | VERIFIED | CONFIRMED | P0 |
| REQ-SEC-002 | 敏感操作必须有审计日志 | VERIFIED | CONFIRMED | P1 |

### 6.5 Cross-Domain 需求

| ID | 标题 | 状态 | 分类 | 优先级 |
|----|------|------|------|--------|
| REQ-CROSS-001 | 跨域数据流必须明确所有权 | VERIFIED | CONFIRMED | P0 |
| REQ-CROSS-002 | 事件驱动必须保证最终一致性 | VERIFIED | CONFIRMED | P0 |

---

## 七、ARCH 系列 (架构原则)

| ID | 标题 | 状态 | 分类 | 类别 |
|----|------|------|------|------|
| ARCH-TRUTH-001 | Truth Source 唯一性 | CONFIRMED | CONFIRMED | TRUTH_SOURCE |
| ARCH-TRUTH-002 | Truth Source 数据模型定义 | CONFIRMED | CONFIRMED | TRUTH_SOURCE |
| ARCH-OWN-001 | 角色分离原则 | CONFIRMED | CONFIRMED | DATA_OWNERSHIP |
| ARCH-OWN-002 | 业务入口与真相源分离 | CONFIRMED | CONFIRMED | DATA_OWNERSHIP |
| ARCH-WRITE-001 | Service层写入原则 | CONFIRMED | CONFIRMED | WRITE_AUTHORITY |
| ARCH-WRITE-002 | 事件驱动跨域写入 | CONFIRMED | CONFIRMED | WRITE_AUTHORITY |
| ARCH-ENTRY-001 | 客户端入口定位 | CONFIRMED | CONFIRMED | BUSINESS_ENTRY |
| ARCH-ENTRY-002 | 多端写入所有权明确 | CONFIRMED | CONFIRMED | BUSINESS_ENTRY |
| ARCH-MASTER-001 | Master Data Canonical Source | CONFIRMED | CONFIRMED | MASTER_DATA |
| ARCH-MASTER-002 | Reference 数据可追溯 | CONFIRMED | CONFIRMED | MASTER_DATA |
| ARCH-ID-001 | Identity 传播原则 | CONFIRMED | CONFIRMED | IDENTITY |
| ARCH-ID-002 | 禁止下游重新生成 Identity | CONFIRMED | CONFIRMED | IDENTITY |
| ARCH-CROSS-001 | 跨域数据所有权 | CONFIRMED | CONFIRMED | CROSS_DOMAIN |
| ARCH-CROSS-002 | 事件驱动最终一致性 | CONFIRMED | CONFIRMED | CROSS_DOMAIN |
| ARCH-LEGACY-001 | Legacy 系统隔离 | CONFIRMED | CONFIRMED | LEGACY |
| ARCH-LEGACY-002 | Legacy 迁移切换点 | CONFIRMED | CONFIRMED | LEGACY |

---

## 八、BC 系列 (断裂链路)

| ID | 标题 | 状态 | 分类 | 优先级 | 类型 |
|----|------|------|------|--------|------|
| BC-001 | AutoVoucherService 零调用点 | OPEN | BLOCKED | P0 | Foundation→downstream |
| BC-002 | FinanceVoucherService 根包 no-op stub | OPEN | BLOCKED | P2 | Foundation→downstream |
| BC-003 | 遗留财务服务零消费者 | OPEN | BLOCKED | P2 | Foundation→downstream |
| BC-004 | AutoVoucherService 未接入采购流程 | OPEN | BLOCKED | P0 | Master→capability |
| BC-005 | AutoVoucherService 未接入订单流程 | OPEN | BLOCKED | P0 | Master→capability |
| BC-006 | food 表与 foods 表并存 | OPEN | BLOCKED | P1 | Identity lost |
| BC-007 | material 分类表并存 | OPEN | BLOCKED | P1 | Identity lost |
| BC-008 | 订单→财务流水继承断裂 | OPEN | BLOCKED | P1 | Data inheritance |
| BC-009 | 采购→应付继承断裂 | OPEN | BLOCKED | P1 | Data inheritance |
| BC-010 | 凭证→财务报表继承断裂 | OPEN | BLOCKED | P1 | Data inheritance |
| BC-011 | 溯源链路数据继承断裂 | OPEN | BLOCKED | P1 | Data inheritance |
| BC-012 | food/foods 真相源冲突 | OPEN | BLOCKED | P1 | Wrong Truth Source |
| BC-013 | finance_record/finance_records 冲突 | OPEN | BLOCKED | P1 | Wrong Truth Source |
| BC-014 | device_status_log/device_status_logs 冲突 | OPEN | BLOCKED | P1 | Wrong Truth Source |
| BC-015 | food 与 foods 重复 | OPEN | BLOCKED | P2 | Duplicate Master |
| BC-016 | food_category 与 food_categories 重复 | OPEN | BLOCKED | P2 | Duplicate Master |
| BC-017 | dish_combo 与 dish_combos 重复 | OPEN | BLOCKED | P2 | Duplicate Master |
| BC-018 | dish_recipe 与 dish_recipes 重复 | OPEN | BLOCKED | P2 | Duplicate Master |
| BC-019 | finance_record 与 finance_records 重复 | OPEN | BLOCKED | P2 | Duplicate Master |
| BC-020 | stores 与 stores_new 重复 | OPEN | BLOCKED | P2 | Duplicate Master |
| BC-021 | orders V1 与 V6 版本冲突 | OPEN | BLOCKED | P2 | Duplicate Master |
| BC-022 | voucher_header 与 finance_vouchers 重复 | OPEN | BLOCKED | P2 | Duplicate Master |
| BC-023 | SupplierPortal H5 端点无认证配置 | OPEN | BLOCKED | P1 | Missing configuration |
| BC-024 | 25个裸接口方法无权限保护 | OPEN | BLOCKED | P0 | Missing permission |
| BC-025 | 退款审批身份归因不明确 | OPEN | BLOCKED | P1 | Missing permission |
| BC-026 | role_stores 表无 migration | OPEN | BLOCKED | P1 | Scope conflict |
| BC-027 | 订单状态三表并存 | OPEN | BLOCKED | P0 | State conflict |
| BC-028 | 凭证状态前后端错位 | OPEN | BLOCKED | P1 | State conflict |
| BC-029 | sales_order 无映射关系 | OPEN | BLOCKED | P1 | State conflict |
| BC-030 | 遗留财务服务零消费者 | OPEN | BLOCKED | P2 | Legacy interruption |
| BC-031 | 遗留硬件控制器全部废弃 | OPEN | BLOCKED | P2 | Legacy interruption |

---

## 九、RC 系列 (根因候选)

| ID | 标题 | 状态 | 分类 | 关联 DEC | 关联 SR |
|----|------|------|------|----------|---------|
| RC-001 | food/foods 双写与身份冲突 | OPEN | BLOCKED | DEC-006 | SR-001 |
| RC-002 | product/material 边界不清与遗留列名 | OPEN | BLOCKED | DEC-001, DEC-002 | SR-001 |
| RC-003 | stores/stores_new 双表与门店身份冲突 | OPEN | BLOCKED | DEC-009 | SR-001 |
| RC-004 | 金额单位双轨（分/元） | OPEN | BLOCKED | DEC-007 | SR-003 |
| RC-005 | 订单状态三套并存 | OPEN | BLOCKED | DEC-008 | SR-002 |
| RC-006 | 凭证状态前后端映射错位 | OPEN | BLOCKED | - | SR-004 |
| RC-007 | Warehouse Foundation 缺失 | OPEN | BLOCKED | DEC-001 | SR-007 |
| RC-008 | Unit Foundation 缺失 | OPEN | BLOCKED | DEC-002 | SR-008 |
| RC-009 | PaymentMethod Foundation 缺失 | OPEN | BLOCKED | DEC-003 | SR-009 |
| RC-010 | Customer Foundation 缺失 | OPEN | BLOCKED | DEC-004 | SR-010 |
| RC-011 | Price Foundation 缺失 | OPEN | BLOCKED | DEC-005 | SR-011 |
| RC-012 | 多端写入冲突与数据所有权不明 | OPEN | BLOCKED | DEC-010, DEC-013, DEC-014 | SR-012 |
| RC-013 | 数据所有权不明确与跨域协调风险 | OPEN | BLOCKED | DEC-010, DEC-011 | SR-013 |
| RC-014 | 事件链断裂与继承缺失 | OPEN | BLOCKED | DEC-011 | SR-013 |
| RC-015 | 遗留表残留 | OPEN | BLOCKED | DEC-012 | SR-015 |
| RC-016 | 遗留服务残留 | OPEN | BLOCKED | DEC-012 | SR-016 |
| RC-017 | Dead Code 与隐藏机制 | OPEN | BLOCKED | DEC-012 | SR-017 |

---

## 十、SR 系列 (系统修复候选)

| ID | 标题 | 状态 | 分类 | 优先级 | 依赖 |
|----|------|------|------|--------|------|
| SR-001 | 统一 Product/Material/Food Identity | READY_FOR_PLANNING | ENGINEERING_READY | P0 | - |
| SR-002 | 统一订单状态机 | READY_FOR_PLANNING | ENGINEERING_READY | P1 | SR-001 |
| SR-003 | 统一金额单位 | READY_FOR_PLANNING | ENGINEERING_READY | P0 | - |
| SR-004 | 解决凭证状态映射 | READY_FOR_PLANNING | ENGINEERING_READY | P2 | SR-003 |
| SR-005 | 解决 inventory/store_inventory 双写 | BLOCKED | BLOCKED | P1 | SR-001, SR-007 |
| SR-006 | 解决成本双写 | BLOCKED | BLOCKED | P1 | SR-003, SR-001 |
| SR-007 | 建立 Warehouse Foundation | BLOCKED | BLOCKED | P2 | DEC-001 |
| SR-008 | 建立 Unit Foundation | READY_FOR_DOCUMENTATION | ENGINEERING_READY | P2 | - |
| SR-009 | 建立 PaymentMethod Foundation | READY_FOR_DOCUMENTATION | ENGINEERING_READY | P2 | - |
| SR-010 | 建立 Customer Foundation | BLOCKED | BLOCKED | P1 | DEC-004 |
| SR-011 | 建立 Price Foundation | READY_FOR_DOCUMENTATION | ENGINEERING_READY | P3 | - |
| SR-012 | 明确跨域数据所有权 | BLOCKED | BLOCKED | P0 | DEC-010 |
| SR-013 | 加强事件驱动可靠性 | BLOCKED | BLOCKED | P1 | DEC-011 |
| SR-014 | 解决多端写入冲突 | BLOCKED | BLOCKED | P0 | DEC-010, SR-002 |
| SR-015 | 清理 Legacy 数据库表 | BLOCKED | BLOCKED | P2 | SR-001, SR-002, SR-003 |
| SR-016 | 清理 Legacy 服务 | BLOCKED | BLOCKED | P2 | SR-015 |
| SR-017 | 清理 Dead Code | BLOCKED | BLOCKED | P2 | SR-016 |

---

## 十一、CONFLICT 系列 (冲突注册表)

| ID | 标题 | 状态 | 分类 | 严重程度 |
|----|------|------|------|----------|
| CONFLICT-STAT-001 | Pages 统计数字差异 | OPEN | OPEN | HIGH |
| CONFLICT-STAT-002 | Foundation Objects 统计不一致 | OPEN | OPEN | MEDIUM |
| CONFLICT-USDS-001 | Warehouse→Inventory 数据流未声明 | OPEN | OPEN | HIGH |
| CONFLICT-ID-001 | productId Wrong Identity 未追踪 | OPEN | OPEN | CRITICAL |
| CONFLICT-ID-002 | warehouseId Identity Loss 未追踪 | OPEN | OPEN | HIGH |
| CONFLICT-BC-001 | BC-001/004/005 内容重叠 | OPEN | OPEN | MEDIUM |
| CONFLICT-BC-002 | BC-003/030 内容重叠 | OPEN | OPEN | MEDIUM |
| CONFLICT-MF-001 | Warehouse/Unit/PaymentMethod 分类模糊 | OPEN | OPEN | MEDIUM |
| CONFLICT-MF-002 | Customer 需要 Product Decision | OPEN | OPEN | HIGH |
| CONFLICT-MF-003 | Price Embedded 模式待确认 | OPEN | OPEN | MEDIUM |
| CONFLICT-XDO-001 | Order 多端写入所有权不明确 | OPEN | OPEN | CRITICAL |
| CONFLICT-META-001 | Foundation Objects 数量三文件不一致 | OPEN | OPEN | HIGH |
| CONFLICT-META-002 | 三张地图对象覆盖范围不一致 | OPEN | OPEN | HIGH |
| CONFLICT-META-003 | Pages 统计口径差异 | OPEN | OPEN | MEDIUM |
| CONFLICT-META-004 | Warehouse 状态矛盾 | OPEN | OPEN | MEDIUM |
| CONFLICT-META-005 | 三张地图层级定义不一致 | OPEN | OPEN | MEDIUM |
| CONFLICT-META-006 | 事件数量差异 | OPEN | OPEN | LOW |

---

## 十二、FND 系列 (Foundation 事实)

| ID | 标题 | 状态 | 分类 | 置信度 |
|----|------|------|------|--------|
| FND-001 | Organization 真相源为 departments 表 | VERIFIED | FACT | HIGH |
| FND-002 | Store 真相源为 stores_new 表 | VERIFIED | FACT | HIGH |
| FND-003 | Department 真相源为 departments 表 | VERIFIED | FACT | HIGH |
| FND-004 | Employee 真相源为 employees 表 | VERIFIED | FACT | HIGH |
| FND-005 | Position 真相源为 positions 表 | PARTIAL | FACT | MEDIUM |
| FND-006 | Role 真相源为 roles 表 | VERIFIED | FACT | HIGH |
| FND-007 | Permission 真相源为 permissions 表 | VERIFIED | FACT | HIGH |
| FND-008 | Category 真相源为 food/material_categories | VERIFIED | FACT | MEDIUM |
| FND-009 | Food 真相源为 foods 表 | VERIFIED | FACT | HIGH |
| FND-010 | Material 真相源为 material_archives 表 | VERIFIED | FACT | HIGH |
| FND-011 | Supplier 真相源为 suppliers 表 | VERIFIED | FACT | HIGH |
| FND-012 | AccountingSubject 真相源为 accounting_subjects 表 | VERIFIED | FACT | HIGH |
| FND-013 | BankAccount 真相源为 bank_accounts 表 | VERIFIED | FACT | MEDIUM |

---

## 十三、MDS 系列 (Master Data 事实)

| ID | 标题 | 状态 | 分类 | 置信度 |
|----|------|------|------|--------|
| MDS-001 | Food Canonical Source 为 foods 表 | VERIFIED | FACT | HIGH |
| MDS-002 | Material Canonical Source 为 material_archives 表 | VERIFIED | FACT | HIGH |
| MDS-003 | Supplier Canonical Source 为 suppliers 表 | VERIFIED | FACT | HIGH |
| MDS-004 | Employee Canonical Source 为 employees 表 | VERIFIED | FACT | HIGH |
| MDS-005 | Store Canonical Source 为 stores_new 表 | VERIFIED | FACT | HIGH |

---

## 十四、CAP 系列 (Capability 事实)

| ID | 标题 | 状态 | 分类 | 置信度 |
|----|------|------|------|--------|
| CAP-001 | Order Management 能力已就绪 | VERIFIED | FACT | HIGH |
| CAP-002 | Purchase Management 能力部分就绪 | PARTIAL | FACT | HIGH |
| CAP-003 | Inventory Management 能力已就绪 | VERIFIED | FACT | HIGH |
| CAP-004 | Finance Management 能力部分就绪 | PARTIAL | FACT | HIGH |
| CAP-005 | HR Management 能力已就绪 | VERIFIED | FACT | HIGH |

---

## 十五、CNF 系列 (Conflict 事实)

| ID | 标题 | 状态 | 分类 | 置信度 |
|----|------|------|------|--------|
| CNF-001 | food/foods 双写 | CONFLICT | FACT | HIGH |
| CNF-002 | 金额单位双轨 | CONFLICT | FACT | HIGH |
| CNF-003 | 订单状态三套并存 | CONFLICT | FACT | HIGH |
| CNF-004 | stores/stores_new 双表并存 | CONFLICT | FACT | HIGH |
| CNF-005 | 科目余额双轨 (PD-031 已裁决) | VERIFIED | FACT | HIGH |
| CNF-006 | product_id vs material_id 列名冲突 | CONFLICT | FACT | HIGH |
| CNF-007 | 凭证状态映射错位 (已解决) | VERIFIED | FACT | HIGH |
| CNF-008 | inventory+store_inventory 双写 | CONFLICT | FACT | HIGH |
| CNF-009 | 成本双写 (主事务+异步) | CONFLICT | FACT | HIGH |
| CNF-010 | finance_record 口径注释混乱 | CONFLICT | FACT | HIGH |

---

## 十六、STAT 系列 (统计事实)

| ID | 标题 | 状态 | 分类 | 值 |
|----|------|------|------|-----|
| STAT-001 | Controller 总数 | VERIFIED | FACT | 154 |
| STAT-002 | Service 总数 | VERIFIED | FACT | 293 |
| STAT-003 | Entity 总数 | VERIFIED | FACT | 326 |
| STAT-004 | Mapper 总数 | VERIFIED | FACT | 302 |
| STAT-005 | Migration 文件总数 | VERIFIED | FACT | 160 |
| STAT-006 | Events 总数 | VERIFIED | FACT | 22 |
| STAT-007 | Listeners 总数 | VERIFIED | FACT | 10 |
| STAT-008 | Schedulers 总数 | VERIFIED | FACT | 5 |
| STAT-009 | Tasks 总数 | VERIFIED | FACT | 6 |
| STAT-010 | Permission Codes 总数 | VERIFIED | FACT | 180 |
| STAT-011 | Data Scope Levels 总数 | VERIFIED | FACT | 8 |

---

## 十七、全局冲突汇总

### 17.1 已识别的跨系列冲突

| 冲突 ID | 冲突描述 | 涉及 ID | 状态 | 解决方案 |
|---------|----------|---------|------|----------|
| GLOBAL-CONFLICT-001 | LOCK-B003 (K8s) 与代码库不符 | LOCK-B003 | INVALIDATED | 标记废止 |
| GLOBAL-CONFLICT-002 | LOCK-B005 (React) 与实际 Vue.js 不符 | LOCK-B005 | INVALIDATED | 标记废止 |
| GLOBAL-CONFLICT-003 | DEC-008 与 LOCK-A003 冲突 | DEC-008, LOCK-A003 | RESOLVED | LOCK-A003 覆盖 |
| GLOBAL-CONFLICT-004 | IMPLICIT-001~006 与 DEC 系列冗余 | IMPLICIT-*, DEC-* | INVALIDATED | IMPLICIT 废止 |
| GLOBAL-CONFLICT-005 | CONFLICT-ID-001 (productId) 与 RC-002 冗余 | CONFLICT-ID-001, RC-002 | OPEN | 需统一视角 |
| GLOBAL-CONFLICT-006 | CONFLICT-XDO-001 (Order 所有权) 与 RC-012 冗余 | CONFLICT-XDO-001, RC-012 | OPEN | 需统一视角 |

### 17.2 废止 ID 清单

| 废止 ID | 废止原因 | 替代 ID | 废止日期 |
|---------|----------|---------|----------|
| LOCK-B003 | 未在代码库验证 | 无 (暂不使用 K8s) | 2026-09-09 |
| LOCK-B005 | 前端为 Vue.js 非 React | 无 (前端技术栈为 Vue.js) | 2026-09-09 |
| IMPLICIT-001 | 冗余 DEC-006 | DEC-006 | 2026-09-09 |
| IMPLICIT-002 | 冗余 DEC-007 | DEC-007 | 2026-09-09 |
| IMPLICIT-003 | 冗余 DEC-008 | DEC-008 | 2026-09-09 |
| IMPLICIT-004 | 冗余 DEC-009 | DEC-009 | 2026-09-09 |
| IMPLICIT-005 | 冗余 DEC-010 | DEC-010 | 2026-09-09 |
| IMPLICIT-006 | 冗余 DEC-011 | DEC-011 | 2026-09-09 |

---

## 十八、ID 使用规范

### 18.1 命名规则

| 系列 | 前缀 | 格式 | 示例 |
|------|------|------|------|
| LOCK | LOCK- | LOCK-{基线}{序号} | LOCK-A001, LOCK-B001 |
| DEC | DEC- | DEC-{序号} | DEC-001 |
| IMPLICIT | IMPLICIT- | IMPLICIT-{序号} | ~~IMPLICIT-001~~ (已废止) |
| REQ | REQ- | REQ-{类别}-{序号} | REQ-MASTER-001 |
| ARCH | ARCH- | ARCH-{类别}-{序号} | ARCH-TRUTH-001 |
| BC | BC- | BC-{序号} | BC-001 |
| RC | RC- | RC-{序号} | RC-001 |
| SR | SR- | SR-{序号} | SR-001 |
| CONFLICT | CONFLICT- | CONFLICT-{类别}-{序号} | CONFLICT-STAT-001 |
| FND | FND- | FND-{序号} | FND-001 |
| MDS | MDS- | MDS-{序号} | MDS-001 |
| CAP | CAP- | CAP-{序号} | CAP-001 |
| CNF | CNF- | CNF-{序号} | CNF-001 |
| STAT | STAT- | STAT-{序号} | STAT-001 |

### 18.2 状态流转

```
OPEN → RECOMMENDED → CONFIRMED → LOCKED
                        ↓
                    INVALIDATED (永久保留)
                        
BLOCKED → ENGINEERING_READY → (实施中) → VERIFIED
```

### 18.3 唯一性保证

1. **同一系列内**: 序号唯一，不得重复
2. **跨系列**: 前缀不同，允许序号相同 (如 DEC-001 与 BC-001)
3. **废止 ID**: 永久保留，不再复用
4. **冲突 ID**: 标记 INVALIDATED，保留历史记录

---

## 十九、维护说明

1. **新增 ID**: 必须在此 Registry 中注册，注明分类和状态
2. **状态变更**: 必须更新此 Registry，注明变更原因
3. **废止 ID**: 标记 INVALIDATED，保留历史记录，不得删除
4. **冲突解决**: 解决后更新相关 ID 状态，注明解决方案
5. **定期审查**: 每月至少审查一次，确保 ID 一致性

---

## 二十、相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| Decision Baseline Recovery | decision-baseline-recovery/ | LOCK 系列详细定义 |
| Decision Recon | decision-recon/ | DEC/IMPLICIT 系列详细定义 |
| Business Requirements Baseline | review/business-requirements-baseline.md | REQ 系列详细定义 |
| Architecture Principle Baseline | review/architecture-principle-baseline.md | ARCH 系列详细定义 |
| Broken Chain Map | broken-chain-map.md | BC 系列详细定义 |
| Systemic Problem Register | review/systemic-problem-register.md | RC 系列详细定义 |
| Systemic Remediation Candidates | review/systemic-remediation-candidates.md | SR 系列详细定义 |
| Conflict Registry | project-master-map-conflict-registry.yaml | CONFLICT 系列详细定义 |
| System Fact Baseline | review/system-fact-baseline.md | FND/MDS/CAP/CNF/STAT 系列详细定义 |

---

*生成时间: 2026-09-09*  
*数据来源: project-master-map 目录下所有文件交叉验证*  
*维护者: opencode*
