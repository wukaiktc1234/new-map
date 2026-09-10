# Decision Provenance — 决策溯源矩阵

> **版本**: 1.0  
> **生成日期**: 2026-09-09  
> **状态**: CANONICAL BASELINE  
> **维护者**: 架构总控 / 项目治理负责人  
> **关键原则**: 每个决策必须有明确来源 | 无来源的决策不得进入 LOCK

---

## 一、Provenance 要求

### 1.1 必填字段

每个正式 Decision 必须包含以下字段：

| 字段 | 定义 | 必填 | 说明 |
|------|------|------|------|
| **Decision ID** | 决策唯一标识 | ✅ | 格式: DEC-xxx / LOCK-Axxx / LOCK-Bxxx |
| **Origin** | 决策来源 | ✅ | V1 Registry / V2 Registry / V2B Registry |
| **Source Document** | 来源文档 | ✅ | MAP-001 / MAP-002 / REVIEW-001 / RECOVERY-001 |
| **Evidence** | 代码/DB/API 证据 | ✅ | 具体文件/表/端点 |
| **Owner** | 决策负责人 | ✅ | Product Owner / Architecture Owner / 后端团队 |
| **Status** | 决策状态 | ✅ | LOCKED / CONFIRMED / OPEN / RECOMMENDED |

### 1.2 禁止进入 LOCK 的情况

| 情况 | 处理 | 说明 |
|------|------|------|
| 无 Source Document | 标记 UNKNOWN | 不得进入 LOCK |
| 无 Evidence | 标记 UNVERIFIED | 不得进入 LOCK |
| 无 Owner | 标记 UNASSIGNED | 不得进入 LOCK |
| 状态非 CONFIRMED | 标记 OPEN | 不得进入 LOCK |

---

## 二、基线 A: 业务数据真相源 LOCK Provenance

### LOCK-A001: foods 为菜品唯一真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A001 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/foods-truth-source.md |
| **Evidence** | FoodService.java → foods 表, V6.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-A002: material_archives 为物料真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A002 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/material-truth-source.md |
| **Evidence** | MaterialService.java → material_archives, V5.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-A003: 金额以分为准 (PARTIAL)

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A003 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/money-contract.md |
| **Evidence** | 新模块 VERIFIED, 旧表(legacy_settlements)仍用 decimal |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (PARTIAL) |

### LOCK-A004: suppliers 为供应商真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A004 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/supplier-truth-source.md |
| **Evidence** | SupplierService.java → suppliers, V3.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-A005: employees 为员工真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A005 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/employee-truth-source.md |
| **Evidence** | EmployeeService.java → employees, V2.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-A006: stores_new 为门店唯一真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A006 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/store-truth-source.md |
| **Evidence** | StoreService.java → stores_new, V4.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-A007: departments 为组织真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A007 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/department-truth-source.md |
| **Evidence** | DepartmentService.java → departments, V1.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-A008: positions 为职位真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A008 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/position-truth-source.md |
| **Evidence** | PositionService.java → positions, V1.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-A009: accounting_subjects 为科目真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A009 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/accounting-truth-source.md |
| **Evidence** | AccountingSubjectService.java → accounting_subjects, V7.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-A010: roles/permissions 为权限真相源

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-A010 |
| **Origin** | V1 Registry |
| **Source Document** | MAP-001: project-master-map/rbac-truth-source.md |
| **Evidence** | RBAC 模型, roles/permissions 表, V1.0.0 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

---

## 三、基线 B: 技术架构真相源 LOCK Provenance

### LOCK-B001: 事件驱动架构 (WebSocket + Event Outbox)

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-B001 |
| **Origin** | V2B Registry |
| **Source Document** | MAP-001: project-master-map/event-driven-architecture.md |
| **Evidence** | websocket_manager.py, event_outbox 表 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-B002: PostgreSQL 主存储

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-B002 |
| **Origin** | V2B Registry |
| **Source Document** | MAP-001: project-master-map/postgresql-storage.md |
| **Evidence** | backend/pom.xml: postgresql 依赖, 340+ 张表 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-B004: OAuth2 + JWT

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-B004 |
| **Origin** | V2B Registry |
| **Source Document** | MAP-001: project-master-map/oauth2-jwt.md |
| **Evidence** | jjwt-api:0.12.3, /v1/auth/** 端点 |
| **Owner** | 安全团队 |
| **Status** | LOCKED (VERIFIED) |

### LOCK-B006: RESTful API

| 字段 | 值 |
|------|-----|
| **Decision ID** | LOCK-B006 |
| **Origin** | V2B Registry |
| **Source Document** | MAP-001: project-master-map/restful-api.md |
| **Evidence** | 154 个 Controller, /v1/** REST 端点 |
| **Owner** | 架构委员会 |
| **Status** | LOCKED (VERIFIED) |

---

## 四、DEC Provenance

### DEC-004: Customer/Member 关系定义

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-004 |
| **Origin** | V1 Registry |
| **Source Document** | REVIEW-001: business-requirements.md |
| **Evidence** | Customer 和 Member 独立表, customers, members 表 |
| **Owner** | Product Owner |
| **Status** | OPEN (RECOMMENDED: Option C) |

### DEC-006: Product/Food/Material 边界

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-006 |
| **Origin** | V1 Registry |
| **Source Document** | REVIEW-001: business-requirements.md |
| **Evidence** | FoodService.java → foods; MaterialService.java → material_archives |
| **Owner** | Product Owner |
| **Status** | OPEN (RECOMMENDED: Option C) |

### DEC-010: 跨域数据访问模式

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-010 |
| **Origin** | V1 Registry |
| **Source Document** | REVIEW-001: architecture-principles.md |
| **Evidence** | 跨域访问模式: API 调用 |
| **Owner** | 架构委员会 |
| **Status** | OPEN (RECOMMENDED: Option A) |

### DEC-011: 事件驱动架构适用范围

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-011 |
| **Origin** | V1 Registry |
| **Source Document** | REVIEW-001: architecture-principles.md |
| **Evidence** | 22个领域事件, 10个监听器 |
| **Owner** | 架构委员会 |
| **Status** | OPEN (RECOMMENDED: Option B) |

### DEC-013: 裸接口权限归属

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-013 |
| **Origin** | V1 Registry |
| **Source Document** | REVIEW-001: security-principles.md |
| **Evidence** | JWT 认证: jjwt-api:0.12.3 + @RequiresPermission |
| **Owner** | 安全团队 |
| **Status** | OPEN (RECOMMENDED: Option A) |

### DEC-014: 供应商 H5 认证

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-014 |
| **Origin** | V1 Registry |
| **Source Document** | REVIEW-001: security-principles.md |
| **Evidence** | 主站认证机制 |
| **Owner** | 安全团队 |
| **Status** | OPEN (RECOMMENDED: Option B) |

---

## 五、INVALIDATED DEC Provenance

### DEC-008: Amount/Money Contract

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-008 |
| **Origin** | V1 Registry |
| **Source Document** | RECOVERY-001: invalidated-decisions.md |
| **Evidence** | 与 LOCK-A003 冲突 |
| **Owner** | - |
| **Status** | INVALIDATED |

### DEC-V2-001~007, 008: V2 技术决策

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-V2-001~007, 008 |
| **Origin** | V2 Registry |
| **Source Document** | RECOVERY-001: invalidated-decisions.md |
| **Evidence** | 与 V1 冗余或冲突 |
| **Owner** | - |
| **Status** | INVALIDATED |

### DEC-V2B-006~009: V2B 架构决策

| 字段 | 值 |
|------|-----|
| **Decision ID** | DEC-V2B-006~009 |
| **Origin** | V2B Registry |
| **Source Document** | RECOVERY-001: invalidated-decisions.md |
| **Evidence** | 无证据支撑 |
| **Owner** | - |
| **Status** | INVALIDATED |

---

## 六、IMPLICIT Provenance

### IMPLICIT-001~006: 隐式决策

| 字段 | 值 |
|------|-----|
| **Decision ID** | IMPLICIT-001~006 |
| **Origin** | 隐式 |
| **Source Document** | RECOVERY-001: invalidated-decisions.md |
| **Evidence** | 与已有 DEC/LOCK 冗余 |
| **Owner** | - |
| **Status** | INVALIDATED (SUBSUMED) |

---

## 七、Provenance 验证规则

### 7.1 验证检查项

| 检查项 | 规则 | 处理 |
|--------|------|------|
| Decision ID 唯一性 | 同一 ID 不能有两种含义 | 冲突 ID 标记 CONFLICT |
| Source Document 存在性 | 必须有明确来源 | 无来源标记 UNKNOWN |
| Evidence 存在性 | 必须有代码/DB/API 证据 | 无证据标记 UNVERIFIED |
| Owner 存在性 | 必须有明确负责人 | 无负责人标记 UNASSIGNED |
| Status 一致性 | 状态必须与实际匹配 | 不一致标记 CONFLICT |

### 7.2 Provenance 矩阵

| Decision ID | Origin | Source Document | Evidence | Owner | Status |
|-------------|--------|-----------------|----------|-------|--------|
| LOCK-A001 | V1 | MAP-001 | FoodService.java → foods | 架构委员会 | LOCKED |
| LOCK-A002 | V1 | MAP-001 | MaterialService.java → material_archives | 架构委员会 | LOCKED |
| LOCK-A003 | V1 | MAP-001 | 新模块 VERIFIED, 旧表 PARTIAL | 架构委员会 | LOCKED |
| ... | ... | ... | ... | ... | ... |
| DEC-004 | V1 | REVIEW-001 | Customer/Member 独立表 | Product Owner | OPEN |
| DEC-006 | V1 | REVIEW-001 | Food/Material 独立表 | Product Owner | OPEN |
| DEC-010 | V1 | REVIEW-001 | 跨域 API 调用 | 架构委员会 | OPEN |
| DEC-011 | V1 | REVIEW-001 | 22个领域事件 | 架构委员会 | OPEN |
| DEC-013 | V1 | REVIEW-001 | JWT 认证 | 安全团队 | OPEN |
| DEC-014 | V1 | REVIEW-001 | 主站认证 | 安全团队 | OPEN |

---

## 八、统计摘要

| 类型 | 总数 | 有 Provenance | 无 Provenance | 说明 |
|------|------|---------------|---------------|------|
| LOCK (基线 A) | 10 | 10 | 0 | 全部有 Provenance |
| LOCK (基线 B) | 4 | 4 | 0 | 全部有 Provenance |
| LOCK (基线 C/D) | 12 | 0 | 12 | 已废止，无 Provenance |
| DEC (V1) | 14 | 14 | 0 | 全部有 Provenance |
| DEC (V2) | 14 | 0 | 14 | 已废止，无 Provenance |
| DEC (V2B) | 14 | 0 | 14 | 已废止，无 Provenance |
| IMPLICIT | 6 | 0 | 6 | 已废止，无 Provenance |
| REQ | 17 | 17 | 0 | 全部有 Provenance |
| ARCH | 16 | 16 | 0 | 全部有 Provenance |
| RC | 13 | 13 | 0 | 全部有 Provenance |
| BC | 31 | 31 | 0 | 全部有 Provenance |
| **总计** | **151** | **105** | **46** | |

---

## 九、下一步

1. **停止地图制作**: 本文档为最终治理基线，不再生成新的地图文件
2. **等待确认**: Product Owner / Architecture Owner 正式确认 Decision
3. **进入治理流程**: 
   ```
   FACT BASELINE → PRODUCT DECISIONS / ARCHITECTURE DECISIONS
                          ↓
                   REQUIREMENT BASELINE
                          ↓
                   SYSTEMIC REMEDIATION
                          ↓
                   ENGINEERING CARDS
   ```

---

**文档状态**: ✅ CANONICAL BASELINE  
**下一步**: 等待 Product Owner / Architecture Owner 正式确认 Decision
