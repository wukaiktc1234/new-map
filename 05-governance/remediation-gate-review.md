# Remediation Gate Review — 重新审查 Remediation Gate

> **版本**: 2.0  
> **生成日期**: 2026-09-09  
> **状态**: CANONICAL BASELINE  
> **维护者**: 架构总控 / 项目治理负责人  
> **关键原则**: 严格审查 | 禁止强行 Ready | 确保所有依赖已解决 | Evidence Status 与 Decision Status 分离

---

## 一、审查标准

### 1.1 ENGINEERING_READY 必备条件

| 条件 | 定义 | 必填 | 说明 |
|------|------|------|------|
| **Decision Confirmed** | 决策已确认 | ✅ | 必须有 CONFIRMED 或 LOCKED 状态的决策 |
| **Requirement Clear** | 需求明确 | ✅ | 必须有明确的业务需求 |
| **Upstream Clear** | 上游清晰 | ✅ | 所有上游依赖已解决 |
| **Downstream Clear** | 下游清晰 | ✅ | 所有下游依赖已明确 |
| **Truth Source Clear** | 真相源清晰 | ✅ | 必须有明确的真相源 |
| **Data Contract Clear** | 数据契约明确 | ✅ | 必须有明确的数据契约 |
| **Acceptance Criteria Possible** | 验收标准可能 | ✅ | 必须有可测试的验收标准 |

### 1.2 缺失条件处理

| 缺失条件 | 处理 | 说明 |
|----------|------|------|
| Decision 未确认 | WAITING_DECISION | 等待决策确认 |
| Requirement 不明确 | WAITING_REQUIREMENT | 等待需求明确 |
| Upstream 未解决 | WAITING_UPSTREAM | 等待上游解决 |
| Downstream 未明确 | WAITING_DOWNSTREAM | 等待下游明确 |
| Truth Source 不清晰 | WAITING_TRUTH_SOURCE | 等待真相源明确 |
| Data Contract 不明确 | WAITING_DATA_CONTRACT | 等待数据契约明确 |
| Acceptance Criteria 不可能 | WAITING_ACCEPTANCE | 等待验收标准定义 |

### 1.3 状态说明

| 状态类型 | 定义 | 适用对象 |
|----------|------|----------|
| **Evidence Status** | 证据支持程度 | VERIFIED/PARTIAL/UNVERIFIED/CONFLICT |
| **Decision Status** | 决策确认程度 | OPEN/RECOMMENDED/CONFIRMED/LOCKED/SUPERSEDED/INVALIDATED |
| **Execution Status** | 执行状态 | ENGINEERING_READY/BLOCKED/IMPLEMENTED |

---

## 二、重新审查所有 ER

### ER-001: foods 菜品真相源执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-A001 Decision Status = LOCKED |
| Requirement Clear | ✅ | REQ-MASTER-001: 真相源唯一性 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 订单/库存/报表模块 |
| Truth Source Clear | ✅ | foods 表为唯一真相源 |
| Data Contract Clear | ✅ | foods 表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试菜品数据一致性 |

**结论**: ✅ ENGINEERING_READY

---

### ER-002: material_archives 物料真相源执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-A002 Decision Status = LOCKED |
| Requirement Clear | ✅ | REQ-MASTER-001: 真相源唯一性 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 库存/采购/生产模块 |
| Truth Source Clear | ✅ | material_archives 表为唯一真相源 |
| Data Contract Clear | ✅ | material_archives 表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试物料数据一致性 |

**结论**: ✅ ENGINEERING_READY

---

### ER-003: suppliers 供应商真相源执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-A004 Decision Status = LOCKED |
| Requirement Clear | ✅ | REQ-MASTER-001: 真相源唯一性 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 采购/应付账款模块 |
| Truth Source Clear | ✅ | suppliers 表为唯一真相源 |
| Data Contract Clear | ✅ | suppliers 表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试供应商数据一致性 |

**结论**: ✅ ENGINEERING_READY

---

### ER-004: employees 员工真相源执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-A005 Decision Status = LOCKED |
| Requirement Clear | ✅ | REQ-MASTER-001: 真相源唯一性 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 考勤/薪资/权限模块 |
| Truth Source Clear | ✅ | employees 表为唯一真相源 |
| Data Contract Clear | ✅ | employees 表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试员工数据一致性 |

**结论**: ✅ ENGINEERING_READY

---

### ER-005: stores_new 门店真相源执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-A006 Decision Status = LOCKED |
| Requirement Clear | ✅ | REQ-MASTER-001: 真相源唯一性 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 订单/库存/报表模块 |
| Truth Source Clear | ✅ | stores_new 表为唯一真相源 |
| Data Contract Clear | ✅ | stores_new 表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试门店数据一致性 |

**结论**: ✅ ENGINEERING_READY

---

### ER-006: departments/positions 组织真相源执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-A007/A008 Decision Status = LOCKED |
| Requirement Clear | ✅ | REQ-MASTER-001: 真相源唯一性 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 审批流程/报表结构 |
| Truth Source Clear | ✅ | departments/positions 表为唯一真相源 |
| Data Contract Clear | ✅ | departments/positions 表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试组织数据一致性 |

**结论**: ✅ ENGINEERING_READY

---

### ER-007: accounting_subjects 会计科目真相源执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-A009 Decision Status = LOCKED |
| Requirement Clear | ✅ | REQ-MASTER-001: 真相源唯一性 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 财务记账/报表/成本核算 |
| Truth Source Clear | ✅ | accounting_subjects 表为唯一真相源 |
| Data Contract Clear | ✅ | accounting_subjects 表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试科目数据一致性 |

**结论**: ✅ ENGINEERING_READY

---

### ER-008: roles/permissions 权限真相源执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-A010 已 CONFIRMED |
| Requirement Clear | ✅ | REQ-SEC-001/002: 认证/授权策略 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 用户授权/菜单控制/API 访问 |
| Truth Source Clear | ✅ | roles/permissions 表为唯一真相源 |
| Data Contract Clear | ✅ | roles/permissions 表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试权限数据一致性 |

**结论**: ✅ ENGINEERING_READY

---

### ER-009: 事件驱动架构执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-B001 已 CONFIRMED |
| Requirement Clear | ✅ | REQ-MASTER-003: 事件驱动架构 |
| Upstream Clear | ⚠️ | 等待 DEC-011 (事件驱动架构适用范围) |
| Downstream Clear | ✅ | WebSocket/Event Outbox |
| Truth Source Clear | ✅ | event_outbox 表为事件真相源 |
| Data Contract Clear | ✅ | 事件结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试事件驱动功能 |

**结论**: ⚠️ WAITING_UPSTREAM (等待 DEC-011)

---

### ER-010: PostgreSQL 主存储执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-B002 已 CONFIRMED |
| Requirement Clear | ✅ | REQ-DATA-002: 数据一致性 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 所有业务模块 |
| Truth Source Clear | ✅ | PostgreSQL 为主存储 |
| Data Contract Clear | ✅ | 数据库表结构明确 |
| Acceptance Criteria Possible | ✅ | 可测试数据库功能 |

**结论**: ✅ ENGINEERING_READY

---

### ER-011: OAuth2+JWT 认证执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-B004 已 CONFIRMED |
| Requirement Clear | ✅ | REQ-SEC-001: 认证策略 |
| Upstream Clear | ✅ | 无上游依赖 |
| Downstream Clear | ✅ | 所有 API 端点 |
| Truth Source Clear | ✅ | JWT 认证机制 |
| Data Contract Clear | ✅ | 认证接口明确 |
| Acceptance Criteria Possible | ✅ | 可测试认证功能 |

**结论**: ✅ ENGINEERING_READY

---

### ER-012: RESTful API 执行

| 条件 | 状态 | 说明 |
|------|------|------|
| Decision Confirmed | ✅ | LOCK-B006 已 CONFIRMED |
| Requirement Clear | ✅ | REQ-CROSS-001: 跨域数据访问 |
| Upstream Clear | ⚠️ | 等待 DEC-010 (跨域数据访问模式) |
| Downstream Clear | ✅ | 所有前端终端 |
| Truth Source Clear | ✅ | 154 个 Controller |
| Data Contract Clear | ✅ | API 接口明确 |
| Acceptance Criteria Possible | ✅ | 可测试 API 功能 |

**结论**: ⚠️ WAITING_UPSTREAM (等待 DEC-010)

---

## 三、审查结果汇总

### 3.1 ENGINEERING_READY (可立即执行)

| ER ID | 名称 | 决策来源 | 优先级 |
|-------|------|----------|--------|
| ER-001 | foods 菜品真相源执行 | LOCK-A001 | P0 |
| ER-002 | material_archives 物料真相源执行 | LOCK-A002 | P0 |
| ER-003 | suppliers 供应商真相源执行 | LOCK-A004 | P0 |
| ER-004 | employees 员工真相源执行 | LOCK-A005 | P0 |
| ER-005 | stores_new 门店真相源执行 | LOCK-A006 | P0 |
| ER-006 | departments/positions 组织真相源执行 | LOCK-A007/A008 | P0 |
| ER-007 | accounting_subjects 会计科目真相源执行 | LOCK-A009 | P0 |
| ER-008 | roles/permissions 权限真相源执行 | LOCK-A010 | P0 |
| ER-010 | PostgreSQL 主存储执行 | LOCK-B002 | P0 |
| ER-011 | OAuth2+JWT 认证执行 | LOCK-B004 | P0 |

**统计**: 10/12 ENGINEERING_READY

### 3.2 WAITING_UPSTREAM (等待上游)

| ER ID | 名称 | 等待决策 | 说明 |
|-------|------|----------|------|
| ER-009 | 事件驱动架构执行 | DEC-011 | 等待事件驱动架构适用范围确认 |
| ER-012 | RESTful API 执行 | DEC-010 | 等待跨域数据访问模式确认 |

**统计**: 2/12 WAITING_UPSTREAM

### 3.3 WAITING_PRODUCT_DECISION (等待产品决策)

| ER ID | 名称 | 等待决策 | 说明 |
|-------|------|----------|------|
| ER-013 | Customer/Member 关系执行 | DEC-004 | 等待 Customer/Member 关系确认 |
| ER-014 | Product/Food/Material 边界执行 | DEC-006 | 等待 Product/Food/Material 边界确认 |

**统计**: 2/12 WAITING_PRODUCT_DECISION

### 3.4 WAITING_ARCHITECTURE_DECISION (等待架构决策)

| ER ID | 名称 | 等待决策 | 说明 |
|-------|------|----------|------|
| ER-015 | 跨域数据访问模式执行 | DEC-010 | 等待跨域数据访问模式确认 |
| ER-016 | 裸接口权限归属执行 | DEC-013 | 等待裸接口权限归属确认 |
| ER-017 | 供应商 H5 认证执行 | DEC-014 | 等待供应商 H5 认证确认 |

**统计**: 3/12 WAITING_ARCHITECTURE_DECISION

---

## 四、统计摘要

| 状态 | 数量 | 占比 | 说明 |
|------|------|------|------|
| ENGINEERING_READY | 10 | 71% | 可立即执行 |
| WAITING_UPSTREAM | 2 | 14% | 等待上游决策 |
| WAITING_PRODUCT_DECISION | 2 | 14% | 等待产品决策 |
| WAITING_ARCHITECTURE_DECISION | 3 | 21% | 等待架构决策 |
| **总计** | **14** | **100%** | |

---

## 五、禁止强行 Ready 的规则

### 5.1 禁止行为

| 禁止行为 | 正确行为 | 说明 |
|----------|----------|------|
| 缺少 Decision Confirmed | WAITING_DECISION | 等待决策确认 |
| 缺少 Requirement Clear | WAITING_REQUIREMENT | 等待需求明确 |
| 缺少 Upstream Clear | WAITING_UPSTREAM | 等待上游解决 |
| 缺少 Downstream Clear | WAITING_DOWNSTREAM | 等待下游明确 |
| 缺少 Truth Source Clear | WAITING_TRUTH_SOURCE | 等待真相源明确 |
| 缺少 Data Contract Clear | WAITING_DATA_CONTRACT | 等待数据契约明确 |
| 缺少 Acceptance Criteria Possible | WAITING_ACCEPTANCE | 等待验收标准定义 |

### 5.2 处理规则

| 情况 | 处理 | 说明 |
|------|------|------|
| 所有条件满足 | ENGINEERING_READY | 可立即执行 |
| 缺少 1-2 个条件 | WAITING_xxx | 等待条件满足 |
| 缺少 3+ 个条件 | BLOCKED | 被阻断，需重新评估 |

---

## 六、下一步

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
