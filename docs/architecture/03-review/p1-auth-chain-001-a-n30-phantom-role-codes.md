# P1-AUTH-CHAIN-001-A 输入件：N30 注解幻码清单（2026-10-15）

> 用途：Owner 审阅后作为 **P1-AUTH-CHAIN-001-B（清洗存量数据 + 修正幻码）** 的实施输入。
> 提取方式：扫描 `backend/src/main/java/com/foodtraceability/**/*.java`，仅取 `hasRole(...)` / `hasAnyRole(...)` **括号内实参**中的角色字符串；归一化 = 去 `ROLE_` 前缀 + 小写；与活库 `roles.role_code` ∪ `roles.role_id_str`（同法归一，19 个真实码）逐一比对；未命中者记为幻码。
> **口径更正**：上一轮口述的「19 个」里有 1 个是转义残留（`hr\` 实为 `hr`）。**真值 = 18 个 distinct 幻码，共 239 次出现，分布在 27 个控制器文件。**

## 0. 活库真实角色码（19 个，归一后）

```
admin  auditor  department_manager  employee  finance_director  finance_manager
finqa_confirm  finqa_no_perm  finqa_register  hr_director  ops_director  owner
purchase_manager  region_manager  scheduler  store_manager  super_admin
team_leader  warehouse_manager
```

## 1. A 栏：可机械纠正（拼写 / 命名与真实码一对一，无歧义）

**4 个幻码 / 24 次出现**

| 幻码 | 次数 | 建议目标 | 依据 | 位置 |
|------|------|----------|------|------|
| `regional_manager` | 10 | `region_manager` | `roles.role_code = region_manager`（区域经理）；`regional_*` 全局仅此处出现 | `StoreManagementSettlementController` L53,67,79,138；`StoreManagementRecruitmentController` L65,99,114,125；`StoreNewController` L147；`LocationController` L19 |
| `ROLE_REGIONAL_MANAGER` | 3 | `ROLE_REGION_MANAGER`（归一后 `region_manager`） | 同上，仅多一层 `ROLE_` 前缀 | `OperationsDashboardController` L40,51；`StoreNewController` L27 |
| `purchaser` | 9 | `purchase_manager` | 活库采购域只有 `purchase_manager`，无 `purchaser` | `PurchaseOrderController` L86,99,113,178；`purchase/PurchaseSettlementController` L108,127；`PurchaseContractController` L42,49；`PurchaseStockinController` L130 |
| `ROLE_OPERATIONS_DIRECTOR` | 2 | `ROLE_OPS_DIRECTOR`（归一后 `ops_director`） | 活库为 `ops_director`，`operations_director` 不存在 | `OperationsDashboardController` L40,51 |

> 注意 A 栏「建议目标」只解决**码名不匹配**；这 4 处仍需 L2（剥前缀）生效后才能匹配上——两件事叠加才通。

## 2. B 栏：需 Owner 裁决（无对应角色 / 歧义 / 可能要补建角色）

**14 个幻码 / 215 次出现**

| 幻码 | 次数 | 涉及文件数 | 现状与歧义点 | 候选处置 |
|------|------|-----------|--------------|----------|
| `hr` | **148** | 15 | 活库 HR 域**只有 `hr_director`**，无泛 `hr` 角色。这是全仓最高频幻码，覆盖整个 HR 域（劳动合同/薪资/员工/部门/岗位/考勤/招聘/面试/简历/H5 入职） | ① 映射 `hr_director`（最小改动）② 补建 `hr` 角色（语义最贴近原意）③ 拆成 `hr_director` + `HR_MANAGER` + `HR_RECRUITER`（最精确，改动面最大） |
| `HR_MANAGER` | 21 | 4 | 招聘域（Offer/配额/门店招聘/反馈）。活库无此角色 | ① 补建 `hr_manager` ② 并入 `hr_director` |
| `HR_RECRUITER` | 12 | 3 | 招聘专员，有明确业务语义 | ① 补建 `hr_recruiter` ② 并入 `hr` |
| `finance` | 12 | 2 | `SalaryController` / `SalaryBatchController`，与 `hr` 并列出现（`hasAnyRole('admin','hr','finance')`）。活库有 `finance_director` 与 `finance_manager` 两个，`finance` 是泛称 | ① 映射 `finance_director` ② 两者都放行 ③ 补建 `finance` |
| `manager` | 7 | 4 | 歧义最大：可能是 `store_manager` / `department_manager` / `general_manager` | 需按端点语义逐个定，或直接去掉该码 |
| `warehouse_keeper` | 3 | 1 | `PurchaseStockinController` L72,99,112，与 `purchase_manager` 并列 | ① 映射 `warehouse_manager` ② 补建 `warehouse_keeper`（库管员，与仓储经理不同职级） |
| `general_manager` | 2 | 1 | `purchase/PurchasePlanController` L136,150，与 `purchase_manager` 并列 | ① 映射 `owner` ② 映射 `super_admin` |
| `ROLE_MANAGER` | 2 | 2 | 同 `manager`，多一层前缀 | 随 `manager` 一并定 |
| `CASHIER` | 2 | 1 | `StoreManagementSettlementController` L90,115（门店结算）。活库无收银员角色 | ① 补建 `cashier` ② 并入 `store_manager` |
| `qc_user` | 2 | 2 | `PurchaseStockinController` L85、`PurchaseArrivalController` L106（质检） | ① 补建 `qc_user` ② 并入 `warehouse_manager` |
| `ROLE_CASHIER` | 1 | 1 | `TableReservationNewController` L35 | 随 `CASHIER` 一并定 |
| `USER` | 1 | 1 | `LabelTemplateController` L39 `@PreAuthorize("hasRole('USER')")` | ① 映射 `employee` ② 去掉（改为仅需登录） |
| `store_employee` | 1 | 1 | `StoreHealthCertificateController` L68 | 映射 `employee` |
| `ROLE_HOST` | 1 | 1 | `TableReservationNewController` L35（订座/迎宾），语义不明 | ① 补建 `host` ② 去掉 |

> **B 栏里 `hr` 一项就占 148/239（62%）**，它的处置方式直接决定 A 卡的工作量级；建议优先拍板 `hr`。

## 3. 附录：逐条位置（全部 239 处）

### `hr` x148（15 文件）
```
controller/EmployeeLaborContractController.java   L70,104,117,127,150,175,192,203,214,235,248,265,277,289,303,329,338,350,368,400,430
controller/SalaryController.java                  L54,62,73,81,90,98,106,117,125,134,142,150,161,170,180,188,234
controller/EmployeeController.java                L44,110,126,139,152,165,196,209,225,241,257,270,283,296
controller/DepartmentController.java              L46,69,100,114,125,156,167,182,197,214,240,262,288
controller/PositionController.java                L36,49,65,83,91,102,111,120,142,153,170,190,206
controller/AttendanceController.java              L40,84,101,118,136,153,170,190,208,223
controller/PositionCodeRuleController.java        L38,49,60,71,82,93,105,116,127
controller/RecruitmentRequirementController.java  L36,53,61,69,79,87,97,105
controller/HrAnalyticsController.java             L43,57,75,89,103,117,131,145
controller/InterviewController.java               L36,48,56,66,77,85,120,139
controller/ResumeController.java                  L28,40,48,56,66,74,84
controller/HrStubController.java                  L37,49,78,97,116,134,153
controller/h5/OnboardingRecordController.java     L27,39,47,57,65,73
controller/SalaryBatchController.java             L43,57,77,92,109
controller/approval/EmployeeApprovalController.java L168,199
```

### `HR_MANAGER` x21（4 文件）
```
controller/JobOfferController.java                    L61,76,90,104,121,139,157,176
controller/RecruitmentQuotaController.java            L73,88,102,173,191,206,219
controller/StoreManagementRecruitmentController.java  L65,99,114,125
controller/RecruitmentFeedbackController.java         L64,76
```

### `finance` x12（2 文件）
```
controller/SalaryController.java       L54,62,98,106,142,150,234
controller/SalaryBatchController.java  L43,57,77,92,109
```

### `HR_RECRUITER` x12（3 文件）
```
controller/JobOfferController.java               L61,76,90,104,121,139,157
controller/RecruitmentQuotaController.java       L88,102,219
controller/RecruitmentFeedbackController.java    L64,76
```

### `regional_manager` x10（4 文件）
```
controller/StoreManagementSettlementController.java  L53,67,79,138
controller/StoreManagementRecruitmentController.java L65,99,114,125
controller/StoreNewController.java                   L147
controller/LocationController.java                    L19
```

### `purchaser` x9（4 文件）
```
controller/PurchaseOrderController.java          L86,99,113,178
controller/purchase/PurchaseSettlementController.java L108,127
controller/PurchaseContractController.java       L42,49
controller/PurchaseStockinController.java        L130
```

### `manager` x7（4 文件）
```
controller/LoginLogController.java               L27,35,43
controller/approval/EmployeeApprovalController.java L168,199
controller/LocationController.java               L19
controller/StoreNewController.java               L147
```

### `warehouse_keeper` x3（1 文件）
```
controller/PurchaseStockinController.java  L72,99,112
```

### `ROLE_REGIONAL_MANAGER` x3（2 文件）
```
controller/OperationsDashboardController.java  L40,51
controller/StoreNewController.java             L27
```

### `general_manager` x2（1 文件）
```
controller/purchase/PurchasePlanController.java  L136,150
```

### `ROLE_MANAGER` x2（2 文件）
```
controller/TableReservationNewController.java  L35
controller/StoreNewController.java             L27
```

### `CASHIER` x2（1 文件）
```
controller/StoreManagementSettlementController.java  L90,115
```

### `qc_user` x2（2 文件）
```
controller/PurchaseStockinController.java   L85
controller/PurchaseArrivalController.java   L106
```

### `ROLE_OPERATIONS_DIRECTOR` x2（1 文件）
```
controller/OperationsDashboardController.java  L40,51
```

### `ROLE_CASHIER` x1 / `USER` x1 / `store_employee` x1 / `ROLE_HOST` x1
```
controller/TableReservationNewController.java  L35   （ROLE_CASHIER、ROLE_HOST）
controller/LabelTemplateController.java        L39   （USER）
controller/StoreHealthCertificateController.java L68 （store_employee）
```

## 4. 代表性原文（供审阅对照）

```java
EmployeeLaborContractController.java:70   @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
SalaryController.java:54                  @PreAuthorize("hasAnyRole('admin', 'hr', 'finance')")
RecruitmentQuotaController.java:73        @PreAuthorize("hasRole('HR_MANAGER')")
StoreManagementSettlementController.java:53  @PreAuthorize("hasAnyRole('STORE_MANAGER', 'REGIONAL_MANAGER', 'FINANCE_DIRECTOR', 'ADMIN', 'admin')")
StoreManagementSettlementController.java:90  @PreAuthorize("hasAnyRole('STORE_MANAGER', 'CASHIER', 'ADMIN', 'admin')")
PurchaseOrderController.java:86           @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'purchaser')")
PurchaseStockinController.java:72         @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'warehouse_keeper')")
PurchaseStockinController.java:85         @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'qc_user')")
OperationsDashboardController.java:40     @PreAuthorize("hasAnyRole('ROLE_OPERATIONS_DIRECTOR', 'ROLE_REGIONAL_MANAGER', 'ROLE_ADMIN')")
LabelTemplateController.java:39           @PreAuthorize("hasRole('USER')")
TableReservationNewController.java:35     @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_HOST', 'ROLE_CASHIER')")
purchase/PurchasePlanController.java:136  @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'general_manager')")
LoginLogController.java:27                @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
StoreHealthCertificateController.java:68  @PreAuthorize("hasAnyRole('store_manager', 'store_employee', 'admin', 'admin')")
LocationController.java:19                @PreAuthorize("hasAnyRole('admin', 'regional_manager', 'manager', 'store_manager', 'warehouse_manager', 'purchase_manager', 'department_manager', 'employee')")
StoreNewController.java:27                @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_REGIONAL_MANAGER', 'ROLE_MANAGER')")
```

## 5. 与 A 卡（剥前缀）的关系

A 卡只解决 **L2 双前缀**，让「存在但大小写/前缀写法不同」的码能匹配（如 `STORE_MANAGER` → `store_manager`）。上述 18 个幻码**即使 A 卡落地也全部仍不匹配**——它们不是写法问题，是 DB 里没有这个角色。故 A 卡与 B 卡互不替代。

## 6. 统计口径

- 扫描范围：`backend/src/main/java/com/foodtraceability` 全量 `.java`
- `hasRole(` 168 处 + `hasAnyRole(` 303 处 = 471 处调用点（按出现次数；上一轮口述的「412」是按行计）
- 其中命中真实码：471 − 239 = 232 处
- 幻码：239 处 / 18 distinct / 27 个文件
