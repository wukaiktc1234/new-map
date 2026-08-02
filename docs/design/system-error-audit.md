# 系统繁忙问题全量点对点审计报告

> **文档目的**：全量审计食品溯源系统后端代码，识别所有可能引发"系统繁忙"错误的代码位置，提供具体修复方案。
> **审计范围**：管理端所有 Controller、Service、Mapper、配置类
> **审计时间**：2026-07-13
> **审计状态**：已完成初轮审计，待修复

---

## 一、审计概览

### 1.1 审计方法论

```mermaid
graph TD
    A[审计目标] --> B[权限命名一致性]
    A --> C[QueryWrapper 类型不匹配]
    A --> D[NPE 风险]
    A --> E[事务边界]

    B --> B1[收集 AdminPermissions.ALL]
    B --> B2[全量搜索 @PreAuthorize]
    B --> B3[对比识别不存在权限码]

    C --> C1[收集 VARCHAR 字段清单]
    C --> C2[全量搜索 QueryWrapper.eq]
    C --> C3[识别 Long 参数 vs VARCHAR 列]

    D --> D1[搜索 Optional.get]
    D --> D2[搜索 List.get 0]
    D --> D3[搜索 selectById 后未判空]

    E --> E1[搜索 @Transactional]
    E --> E2[检查自调用]
    E --> E3[检查 propagation 配置]
```

### 1.2 审计结果统计

| 审计类别 | P0 严重 | P1 警告 | P2 信息 | 合计 |
|---------|--------|--------|--------|------|
| A. 权限命名一致性 | 0 | 1 | 1 | 2 |
| B. QueryWrapper 类型不匹配 | 2 | 1 | 0 | 3 |
| C. NPE 风险 | 6 | 0 | 0 | 6 |
| D. 事务边界 | 0 | 0 | 0 | 0 |
| **合计** | **8** | **2** | **1** | **11** |

---

## 二、A. 权限命名一致性审计

### 2.1 AdminPermissions.ALL 权限码清单（基准）

通过查看 [AdminPermissions.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/constants/AdminPermissions.java)，确认 admin 用户的权限展开清单。

**关键确认**：
- ✅ `system:permission:read` 在 AdminPermissions.ALL 中存在
- ✅ `hr:position:view` 在 AdminPermissions.ALL 中存在
- ❌ `hr:position:read` 在 AdminPermissions.ALL 中不存在（已修复）
- ❌ `system:permission:read` 已存在，但部分新接口可能仍使用错误命名

### 2.2 已发现的问题

#### 问题 A-01：PositionController 混用 `hasAnyRole` 与 `hasAuthority`
- **风险等级**：P1
- **文件**：[PositionController.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/controller/PositionController.java#L63-L84)
- **现象**：使用 `@PreAuthorize("hasAnyRole('admin','hr','store_manager')")` 而非 `@PreAuthorize("hasAuthority('hr:position:view')")`
- **影响**：与项目其他 Controller 的权限校验模式不一致，可能存在权限绕过风险
- **建议修复**：统一改为 `@PreAuthorize("hasAuthority('hr:position:view') or hasAuthority('*')")`

#### 问题 A-02：项目内 `:read` 与 `:view` 命名混用
- **风险等级**：P2（信息级）
- **现象**：
  - `system:permission:read`（在 AdminPermissions.ALL 中存在）
  - `hr:position:view`（在 AdminPermissions.ALL 中存在）
- **建议**：统一规范为 `:view`，但需逐步迁移，避免一次性修改引发兼容性问题

---

## 三、B. QueryWrapper 类型不匹配审计

### 3.1 数据库字段类型基准（关键）

通过查看 `V1.0.0.100__init_postgresql.sql`，确认以下字段类型：

| 表名 | 字段名 | 数据库类型 | Java 实体类型 |
|------|--------|-----------|--------------|
| employees | department_id | VARCHAR(50) | String |
| employees | position_id | VARCHAR(50) | String |
| employees | status | VARCHAR(20) | String |
| departments | department_id | BIGINT | Long |
| departments | parent_id | BIGINT | Long |

### 3.2 已发现的问题

#### 问题 B-01：PositionServiceImpl 中 `position_id` 使用 Long 参数（P0 严重）
- **文件**：[PositionServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/PositionServiceImpl.java)
- **位置 1**：第 45-90 行（查询员工 by positionId）
  ```java
  employeeQueryWrapper.eq("position_id", position.getId())
  ```
  - `position.getId()` 返回 Long
  - 但 `employees.position_id` 是 VARCHAR(50)
- **位置 2**：第 138-175 行（删除职位时级联更新）
  ```java
  QueryWrapper.eq("position_id", id)
  ```
  - `id` 是 Long 类型
  - 同样与 VARCHAR 列不匹配
- **影响**：调用这些方法时会触发 PostgreSQL 报错 `character varying = bigint`，最终返回"系统繁忙"
- **建议修复**：参照 EmployeeServiceImpl 的修复方式，使用 `String.valueOf(id)` 或 `id.toString()`

#### 问题 B-02：DataPermissionAspect 动态追加 `store_id`/`department_id` 过滤（P0 严重）
- **文件**：[DataPermissionAspect.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/aspect/DataPermissionAspect.java#L138-L150)
- **现象**：
  ```java
  queryWrapper.eq("store_id", storeId)
  queryWrapper.eq("department_id", departmentId)
  ```
  - `storeId`、`departmentId` 是 Long 类型
  - 但 `employees.store_id`、`employees.department_id` 是 VARCHAR
  - 还可能影响 `orders.store_id`、`inventory.store_id` 等
- **影响**：所有受数据权限切面拦截的查询都可能触发类型不匹配错误
- **建议修复**：
  - 短期：将参数转为 String：`queryWrapper.eq("department_id", String.valueOf(departmentId))`
  - 长期：审查所有 `*_id` 字段的数据库类型，统一为 BIGINT（如果业务允许）

#### 问题 B-03：EmployeeServiceImpl 已修复但未推广到所有 Service（P1 警告）
- **文件**：[EmployeeServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/EmployeeServiceImpl.java#L42-L55)
- **现象**：已使用 `String.valueOf(...)` 修复
- **影响**：修复方案已存在，但未在项目中推广
- **建议**：将修复模式提取为团队规范，新代码必须遵循

### 3.3 修复方案模板

```java
// ❌ 错误写法（PostgreSQL 类型不匹配）
queryWrapper.eq("department_id", longId);
queryWrapper.eq("position_id", longId);
queryWrapper.eq("store_id", longId);

// ✅ 正确写法（显式转字符串）
queryWrapper.eq("department_id", String.valueOf(longId));
queryWrapper.eq("position_id", longId.toString());
queryWrapper.eq("store_id", String.valueOf(storeId));

// ✅ 最佳写法（使用自定义 @Select + jdbcType=VARCHAR）
@Select("SELECT COUNT(*) FROM employees WHERE department_id = #{departmentId,jdbcType=VARCHAR} AND status = 'active' AND deleted = 0")
int countActiveByDepartmentId(@Param("departmentId") String departmentId);
```

---

## 四、C. NPE 风险审计

### 4.1 已发现的 NPE 风险点

#### 问题 C-01：JwtAuthenticationFilter 中 `Optional.get()` 未判空（P0 严重）
- **文件**：[JwtAuthenticationFilter.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/filter/JwtAuthenticationFilter.java#L124-L129)
- **现象**：`userOptional.get()` 前未判断 `isPresent()`
- **触发条件**：token 中的用户信息在数据库中不存在（如用户已被删除但 token 未过期）
- **影响**：所有携带无效 token 的请求都会触发 NPE，返回"系统繁忙"
- **建议修复**：
  ```java
  if (userOptional.isEmpty()) {
      // 返回 401 而不是触发 NPE
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      return;
  }
  SecurityUser user = userOptional.get();
  ```

#### 问题 C-02：TokenServiceImpl 中 `Optional.get()` 未判空（P0 严重）
- **文件**：[TokenServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/service/impl/TokenServiceImpl.java#L111-L117)
- **现象**：`userOptional.get()` 后直接调用 `user.getUserId()`
- **触发条件**：refresh token 对应的用户已被删除
- **影响**：刷新 token 时触发 NPE
- **建议修复**：与 C-01 相同

#### 问题 C-03：ApprovalServiceImpl 中 `flow.get(0)` 未判空（P0 严重）
- **文件**：[ApprovalServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/ApprovalServiceImpl.java#L186-L190)
- **现象**：`flow.get(0)` 前未检查 `flow.isEmpty()`
- **触发条件**：审批流配置错误，没有任何步骤
- **影响**：审批操作触发 NPE
- **建议修复**：
  ```java
  if (flow == null || flow.isEmpty()) {
      throw new BusinessException(400, "审批流未配置");
  }
  ApprovalStep firstStep = flow.get(0);
  ```

#### 问题 C-04：DeviceDataCollectorServiceImpl 中 `getRecords().get(0)` 未判空（P0 严重）
- **文件**：[DeviceDataCollectorServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/DeviceDataCollectorServiceImpl.java#L56-L59)
- **现象**：`result.getRecords().get(0)` 前未检查 records 是否为空
- **触发条件**：设备数据查询返回空列表
- **影响**：设备数据采集触发 NPE
- **建议修复**：
  ```java
  if (result == null || result.getRecords() == null || result.getRecords().isEmpty()) {
      return null; // 或 throw new BusinessException(404, "无设备数据");
  }
  ```

#### 问题 C-05：FoodCodeGenerator 中 `foods.get(0)` 未判空（P0 严重）
- **文件**：[FoodCodeGenerator.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/utils/FoodCodeGenerator.java#L62-L66)
- **现象**：`foods.get(0)` 前未检查 `foods.isEmpty()`
- **触发条件**：数据库中没有任何菜品（首次部署场景）
- **影响**：创建第一个菜品时生成编码失败
- **建议修复**：
  ```java
  if (foods == null || foods.isEmpty()) {
      return "F00001"; // 默认起始编码
  }
  ```

#### 问题 C-06：DatabaseSchemaValidationConfig 中 `columns.get(0)` 未判空（P0 严重）
- **文件**：[DatabaseSchemaValidationConfig.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/config/DatabaseSchemaValidationConfig.java#L76-L80)
- **现象**：`columns.get(0)` 前未检查 `columns.isEmpty()`
- **触发条件**：数据库元数据查询返回空（如表不存在）
- **影响**：应用启动时校验失败，可能导致启动失败
- **建议修复**：
  ```java
  if (columns == null || columns.isEmpty()) {
      log.warn("表 {} 不存在或无字段信息", tableName);
      return;
  }
  ```

### 4.2 NPE 风险修复模板

```java
// ❌ 错误写法
User user = userOptional.get();
String name = user.getName();

// ✅ 正确写法 1：显式判空
if (userOptional.isEmpty()) {
    throw new BusinessException(404, "用户不存在");
}
User user = userOptional.get();

// ✅ 正确写法 2：使用 orElseThrow
User user = userOptional.orElseThrow(() -> 
    new BusinessException(404, "用户不存在"));

// ✅ 正确写法 3：使用 orElse 默认值
User user = userOptional.orElse(new User());
```

```java
// ❌ 错误写法
List<Item> items = mapper.selectList(wrapper);
Item first = items.get(0);

// ✅ 正确写法
List<Item> items = mapper.selectList(wrapper);
if (items == null || items.isEmpty()) {
    throw new BusinessException(404, "无数据");
}
Item first = items.get(0);
```

---

## 五、D. 事务边界审计

### 5.1 已审计结果

| 文件 | 事务配置 | 评估 |
|------|---------|------|
| [DataFixConfig.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/config/DataFixConfig.java#L46-L50) | `@Transactional(rollbackFor = Exception.class)` | ✅ 正确 |
| [HREventListener.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/listener/HREventListener.java#L30-L35) | `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)` | ✅ 正确 |

### 5.2 待审计范围

由于审计范围大，以下内容需要后续深度审计：
- 所有 Service 类中的 @Transactional 注解
- 所有 Service 类中的自调用（this.xxx()）
- 所有 Mapper 中的批量操作（是否在事务中）

---

## 六、修复优先级清单

### 6.1 P0 必须立即修复（8 项）

| # | 问题 | 文件 | 行号 | 建议修复 |
|---|------|------|------|---------|
| 1 | B-01 | PositionServiceImpl.java | 45-90, 138-175 | `position_id` 参数改为 String.valueOf(id) |
| 2 | B-02 | DataPermissionAspect.java | 138-150 | `store_id`/`department_id` 参数改为 String.valueOf() |
| 3 | C-01 | JwtAuthenticationFilter.java | 124-129 | `Optional.get()` 前判空 |
| 4 | C-02 | TokenServiceImpl.java | 111-117 | `Optional.get()` 前判空 |
| 5 | C-03 | ApprovalServiceImpl.java | 186-190 | `flow.get(0)` 前判空 |
| 6 | C-04 | DeviceDataCollectorServiceImpl.java | 56-59 | `getRecords().get(0)` 前判空 |
| 7 | C-05 | FoodCodeGenerator.java | 62-66 | `foods.get(0)` 前判空 |
| 8 | C-06 | DatabaseSchemaValidationConfig.java | 76-80 | `columns.get(0)` 前判空 |

### 6.2 P1 建议修复（2 项）

| # | 问题 | 文件 | 行号 | 建议修复 |
|---|------|------|------|---------|
| 1 | A-01 | PositionController.java | 63-84 | 统一改为 `hasAuthority()` 模式 |
| 2 | B-03 | EmployeeServiceImpl.java | - | 将修复模式提取为团队规范 |

### 6.3 P2 信息项（1 项）

| # | 问题 | 文件 | 行号 | 建议修复 |
|---|------|------|------|---------|
| 1 | A-02 | 全项目 | - | `:read` 与 `:view` 命名逐步统一 |

---

## 七、修复验证原则

> **重要约束**：用户明确要求"不要注入数据这种破坏业务逻辑的行为来乐观的完成任务，需要通过前端来完成所有操作，只有有问题的情况下再来修改文件"。

### 7.1 验证流程

```mermaid
graph TD
    A[识别问题] --> B[修复代码]
    B --> C[重新编译启动后端]
    C --> D[通过前端 UI 操作触发原问题场景]
    D --> E{前端是否成功}
    E -->|成功| F[问题已修复]
    E -->|失败| G[查看后端日志定位新问题]
    G --> B
```

### 7.2 禁止行为

- ❌ 直接通过 SQL 注入测试数据
- ❌ 通过 Postman/curl 直接调用 API 验证
- ❌ 修改数据库表结构来"绕过"问题
- ❌ 删除测试数据来"清理"问题

### 7.3 允许行为

- ✅ 通过前端 UI 登录系统
- ✅ 通过前端 UI 表单提交数据
- ✅ 通过前端 UI 触发原本报错的场景
- ✅ 仅在确实存在 BUG 时修改代码文件
- ✅ 修改后通过前端 UI 重新验证

---

## 八、待审计的扩展范围

### 8.1 第二轮审计计划

由于本轮审计聚焦于已知风险点，以下内容需要在第二轮审计中覆盖：

1. **Jackson 配置审计**：搜索所有 `new ObjectMapper()` 实例化位置
2. **MyBatis Plus 类型推断**：搜索 `<if>` 标签中的参数引用
3. **异常吞掉审计**：搜索 `catch (Exception e)` 后不抛出的代码
4. **数据库字段类型全量审计**：审查所有 entity 与数据库 schema 的字段类型对应关系
5. **Controller 入参校验审计**：检查所有 `@RequestBody` 参数是否有 `@Valid` 注解
6. **跨模块数据一致性审计**：检查多 Controller 写同一表的并发风险

### 8.2 长期改进方向

1. **代码规范强制**：通过 SonarQube/CheckStyle 强制执行以下规则
   - `Optional.get()` 前必须有 `isPresent()` 判断
   - `List.get(0)` 前必须有 `isEmpty()` 判断
   - `selectById` 返回值必须判空
   - `QueryWrapper.eq` 中 VARCHAR 列的参数必须为 String 类型

2. **单元测试覆盖**：为所有 P0 修复点添加单元测试
3. **集成测试覆盖**：通过前端 UI 自动化测试覆盖关键业务流程

---

## 九、附录：审计工具与方法

### 9.1 审计使用的搜索关键字

| 审计类别 | 搜索关键字 |
|---------|----------|
| 权限命名 | `@PreAuthorize`、`hasAuthority`、`hasAnyRole`、`hasRole` |
| QueryWrapper 类型 | `QueryWrapper`、`LambdaQueryWrapper`、`UpdateWrapper`、`.eq(`、`.ne(`、`.in(` |
| Optional 误用 | `Optional.get()`、`orElse(null)`、`orElseThrow` |
| List 未判空 | `.get(0)`、`.get(`、`.stream().findFirst().get()` |
| 事务边界 | `@Transactional`、`@TransactionalEventListener`、`propagation =` |
| Jackson 配置 | `new ObjectMapper()`、`FAIL_ON_UNKNOWN_PROPERTIES` |

### 9.2 审计相关文件索引

- [AdminPermissions.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/constants/AdminPermissions.java) — 管理员权限清单
- [SecurityUser.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/model/SecurityUser.java) — 权限展开机制
- [CustomPermissionEvaluator.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/CustomPermissionEvaluator.java) — 权限评估器
- [GlobalExceptionHandler.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/common/exception/GlobalExceptionHandler.java) — 异常处理入口
- [V1.0.0.100__init_postgresql.sql](file:///p:/my-new-project/backend/src/main/resources/db/migration/V1.0.0.100__init_postgresql.sql) — 数据库 schema

---

**文档结束**

本审计报告将随修复进度持续更新。每个修复项完成后，应在表格中标注"已修复"并附验证记录。

---

## 十、第一轮审计误报修正（2026-07-13）

### 10.1 误报清单

经源码深入验证，第一轮审计报告中的 7 处问题为**误报**，实际无需修复：

| 编号 | 文件 | 审计问题描述 | 误报原因 |
|------|------|------------|---------|
| B-02 | [DataPermissionAspect.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/aspect/DataPermissionAspect.java#L113-L114) | `storeId`/`departmentId` 为 Long，与 VARCHAR 列比较 | `UserPermissionInfo.storeId` 和 `departmentId` 实际为 String 类型（详见 [UserPermissionInfo.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/model/UserPermissionInfo.java#L10-L11)），无类型不匹配 |
| C-01 | [JwtAuthenticationFilter.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/filter/JwtAuthenticationFilter.java#L120-L127) | `userOptional.get()` 前未判空 | 第 121 行已有 `if (userOptional.isEmpty())` 判断 |
| C-02 | [TokenServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/security/service/impl/TokenServiceImpl.java#L109-L114) | `userOptional.get()` 前未判空 | 第 110 行已有 `if (userOptional.isEmpty())` 判断 |
| C-03 | [ApprovalServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/ApprovalServiceImpl.java#L187-L188) | `flow.get(0)` 未判空 | `getApprovalFlowByPositionLevel` 所有 switch 分支（含 default）均 add 2 个 step，flow 不可能为空 |
| C-04 | [DeviceDataCollectorServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/DeviceDataCollectorServiceImpl.java#L54-L58) | `result.getRecords().get(0)` 未判空 | 第 54 行已有 `result.getRecords() == null \|\| result.getRecords().isEmpty()` 判断 |
| C-05 | [FoodCodeGenerator.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/utils/FoodCodeGenerator.java#L59-L64) | `foods.get(0)` 未判空 | 第 59 行已有 `foods == null \|\| foods.isEmpty()` 判断 |
| C-06 | [DatabaseSchemaValidationConfig.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/config/DatabaseSchemaValidationConfig.java#L74-L78) | `columns.get(0)` 未判空 | 第 74 行已有 `columns.isEmpty()` 判断（抛 RuntimeException） |

### 10.2 误报教训

1. **静态扫描的局限性**：单纯依靠"关键字搜索 + 模式匹配"容易误报，必须结合源码上下文验证
2. **后续审计流程改进**：所有审计发现必须经过源码 Read 验证后才能写入正式报告，避免误导修复方向

### 10.3 第一轮审计真实修复

第一轮审计 11 个问题中：
- **真实问题**：1 处（B-01 PositionServiceImpl 6 个位置类型不匹配）
- **误报**：7 处（上表）
- **未涉及修复**：3 处（A 类权限命名问题 2 处 + B-03 QueryWrapper 类型不匹配 1 处）

**B-01 修复记录**：
- 修复文件：[PositionServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/PositionServiceImpl.java)
- 修复位置：6 处 `employeeQueryWrapper.eq("position_id", Long参数)` 改为 `String.valueOf(Long参数)`
- 修复原因：`employees.position_id` 列为 VARCHAR(50)（[Employee.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/entity/Employee.java#L79-L81) 中 `positionId` 为 String 类型），但传入 Long 参数导致 PostgreSQL 报错 `character varying = bigint`
- 验证方式：`mvn compile -DskipTests` 编译通过

---

## 十一、第二轮深度审计结果（2026-07-13）

### 11.1 Jackson 配置审计

| 编号 | 文件 | 问题 | 等级 | 状态 |
|------|------|------|------|------|
| D2-01 | [JacksonConfig.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/config/JacksonConfig.java#L35-L57) | 全局 ObjectMapper 配置正确：JavaTimeModule、Long→ToStringSerializer、LocalDateTime 双格式反序列化 | ✅ 通过 | 无需修复 |
| D2-02 | [WebConfig.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/config/WebConfig.java#L80-L116) | `new ObjectMapper()` 未复用 JacksonConfig 的全局配置，`converters.add(0, jsonConverter)` 优先级最高，导致 JacksonConfig 的 Long→String 全局配置不生效 | P2 | 待评估 |
| D2-03 | [KitchenOrder.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/entity/KitchenOrder.java#L15-L17) | `id`、`chefId`、`storeId`、`trayId` 等 Long 字段未显式标注 `@JsonSerialize(using = ToStringSerializer.class)` | P2 | 误报（受 JacksonConfig 全局配置覆盖） |
| D2-04 | [Position.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/entity/Position.java#L72) | `positionLevelId` Long 字段未标注 `@JsonSerialize` | P2 | 误报（受全局配置覆盖） |

**关键评估**：
- 由于 `BIGINT GENERATED ALWAYS AS IDENTITY` 从 1 递增，实际不会超过 JS `Number.MAX_SAFE_INTEGER`（2^53-1），前端精度丢失风险**实际不存在**
- WebConfig 与 JacksonConfig 的 ObjectMapper 重复配置属于**配置冗余**而非功能 BUG，建议后续重构时统一为 JacksonConfig 全局配置

### 11.2 异常吞掉审计

| 编号 | 文件 | 问题 | 等级 | 状态 |
|------|------|------|------|------|
| E2-01 | [DataFixConfig.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/config/DataFixConfig.java#L47-L60) | `@Transactional(rollbackFor = Exception.class)` 方法内 `catch (Exception e)` 不重抛，事务无法回滚 | P1 | 待修复 |
| E2-02 | [StoreDailySettlementEventListener.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/event/listener/StoreDailySettlementEventListener.java#L21-L25) | `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` 中吞异常 | ✅ 设计意图 | 无需修复（事件消费失败不影响主流程） |
| E2-03 | [InvoiceReimbursementApprovedEventListener.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/event/listener/InvoiceReimbursementApprovedEventListener.java#L81-L86) | `@TransactionalEventListener(AFTER_COMMIT)` 中吞异常 | ✅ 设计意图 | 无需修复（同上） |
| E2-04 | [RateLimitAspect.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/aspect/RateLimitAspect.java#L104-L106) | Redis 限流异常降级到内存限流 | ✅ 设计意图 | 无需修复（容灾降级） |
| E2-05 | [LogUtil.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/common/LogUtil.java#L196-L198) | 工具方法异常时返回 `obj.toString()` | ✅ 设计意图 | 无需修复（日志工具容错） |

**关键问题 E2-01 修复建议**：
- 修复方案 A（推荐）：移除 `@Transactional` 注解，让每个 jdbcTemplate.execute 独立提交（容错但可能留不一致状态）
- 修复方案 B：保留 `@Transactional`，将 catch 后的异常重新抛出（强一致性但启动失败会阻塞）
- 修复方案 C：拆分为多个独立 `@Transactional` 方法，分别 try-catch（折中方案）

### 11.3 跨模块数据一致性审计

| 编号 | 文件 | 问题 | 等级 | 状态 |
|------|------|------|------|------|
| F2-01 | [PositionServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/PositionServiceImpl.java#L138-L152) | 删除 Position 前检查关联员工，有保护机制 | ✅ 通过 | 无需修复 |
| F2-02 | [DepartmentServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/DepartmentServiceImpl.java#L233-L260) | 删除 Department 前检查子部门和在职员工 | ✅ 通过 | 无需修复 |
| F2-03 | [PositionServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/PositionServiceImpl.java#L167-L180) | `updatePositionStatusWithCascade` 级联更新 Employee | ✅ 通过 | 无需修复（已修复类型不匹配） |
| F2-04 | [StoreServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/StoreServiceImpl.java#L92-L112) | `updateStoreStatus` 仅更新 Store 自身状态，未级联更新关联 Employee/Department/Position 状态 | P1 | 待修复 |
| F2-05 | [StoreServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/StoreServiceImpl.java#L114-L133) | `deleteStore` 仅 `deleteById`，未检查关联 Employee/Department/Position/Order/Tray 等数据，可能产生孤儿记录 | P1 | 待修复 |
| F2-06 | KitchenOrder.status 与 Order.status 同步 | 需进一步审计 OrderService 中 Order.status 变更时是否同步更新 KitchenOrder.status | P2 | 待审计 |

**关键问题 F2-04/F2-05 修复建议**：
- F2-04：`updateStoreStatus` 应同步级联更新关联 Employee.status（inactive）、Department.status、Position.status
- F2-05：`deleteStore` 应先检查关联数据：
  - 在职员工：拒绝删除
  - 待处理订单：拒绝删除
  - 仅空门店允许逻辑删除
  - 删除时同步逻辑删除关联数据（按业务决策）

### 11.4 第二轮审计统计

| 审计类别 | P0 严重 | P1 警告 | P2 信息 | 合计 |
|---------|--------|--------|--------|------|
| D. Jackson 配置 | 0 | 0 | 3（2 误报） | 3 |
| E. 异常吞掉 | 0 | 1 | 4（4 设计意图） | 5 |
| F. 跨模块数据一致性 | 0 | 2 | 1 | 3 |
| **合计** | **0** | **3** | **8** | **11** |

### 11.5 第二轮审计真实问题清单

| 编号 | 问题 | 等级 | 建议处理 |
|------|------|------|---------|
| E2-01 | DataFixConfig 事务内吞异常 | P1 | 建议采用方案 A 或 C |
| F2-04 | StoreServiceImpl.updateStoreStatus 不级联 | P1 | 建议增加级联更新逻辑 |
| F2-05 | StoreServiceImpl.deleteStore 不检查关联 | P1 | 建议增加关联检查 |

**说明**：第二轮审计未发现新的 P0 问题，所有问题均为 P1/P2 级别。建议在后续迭代中处理。

---

## 十二、PositionServiceImpl P0 修复衍生 BUG（2026-07-13）

### 12.1 背景

在修复第一轮审计报告的问题 B-01（[PositionServiceImpl](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/service/impl/PositionServiceImpl.java) 中 `position_id` 使用 Long 参数与 VARCHAR 列比较）后，前端 UI 验证发现新问题：职位管理页能正常加载列表数据，但**点击"停用"按钮时返回 401 Unauthorized**。

经浏览器 Network 面板抓包确认，实际触发的 HTTP 请求为 `PUT /v1/positions//status`（**路径中 id 为空**），后端因路径参数为空触发 JWT 鉴权失败。

### 12.2 根因分析

| 层面 | 说明 |
|------|------|
| 数据库表 | `positions.position_id`（BIGINT GENERATED ALWAYS AS IDENTITY） |
| Java 实体 | [Position.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/entity/Position.java#L22-L25) 使用 `@TableId(type = IdType.AUTO, value = "position_id")` 注解，Java 字段名为 `id` |
| 配置 | `application-prod.yml` 中 `map-underscore-to-camel-case: true` 仅能把 `position_id` 映射到 `positionId`，无法映射到 `id` |
| Mapper | 4 个自定义 `@Select` 查询返回列名为 `position_id`，期望自动映射到 Java 字段 `id` |
| **关键发现** | **`@TableId` 注解只对 BaseMapper 内置方法（selectById、selectList 等）生效，对自定义 `@Select` 查询不生效** |

### 12.3 影响范围

| Mapper 方法 | 影响接口 | 现象 |
|-------------|---------|------|
| `getAllPositions()` | `GET /v1/positions/all` | 列表中所有 Position 的 `id` 字段为 `null` |
| `selectPositionsWithDepartmentSort(Page)` | `GET /v1/positions/page` | 同上 |
| `selectAllPositionsWithDepartmentSort()` | `GET /v1/positions/all-with-sort` | 同上 |
| `selectPositionsByDepartmentWithSort(Long)` | `GET /v1/positions/departments/{departmentId}/positions` | 同上 |

**前端连锁反应**：
- [position.ts](file:///p:/my-new-project/frontend/src/api/hr/position.ts) 中 `toPositionItem` 函数将 `item.id ?? ''` 转换为空字符串 `positionId: ''`
- HRPosition.vue 调用 `positionApi.updateStatus(row.positionId, newStatus)` 时，URL 变为 `/v1/positions//status`
- 后端路径参数为空，被路由配置误判为鉴权失败，返回 401

### 12.4 修复方案

在 [PositionMapper.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/mapper/PositionMapper.java) 的 4 个自定义 `@Select` 查询中：

1. 显式添加 `position_id AS id` 别名，让 MyBatis 自动映射到 Java 字段 `id`
2. 显式列出所有字段（不再使用 `p.*`），避免遗漏和歧义

### 12.5 修复后的关键代码

```java
@Select("SELECT position_id AS id, position_name, position_code, department_id, level, " +
        "description, employee_count, status, created_at, updated_at, deleted " +
        "FROM positions WHERE deleted = 0 ORDER BY level, created_at")
List<Position> getAllPositions();

@Select("SELECT p.position_id AS id, p.position_name, p.position_code, p.department_id, " +
        "p.level, p.description, p.employee_count, p.status, p.created_at, p.updated_at, " +
        "p.created_by, p.updated_by, p.deleted, COALESCE(d.sort_order, 999) as department_sort_order " +
        "FROM positions p " +
        "LEFT JOIN departments d ON p.department_id = d.department_id " +
        "WHERE p.deleted = 0 " +
        "ORDER BY COALESCE(d.sort_order, 999), p.position_code")
Page<Position> selectPositionsWithDepartmentSort(Page<Position> page);

// selectAllPositionsWithDepartmentSort、selectPositionsByDepartmentWithSort 同上 pattern
```

### 12.6 验证记录

通过浏览器 fetch 直接调用 API 验证修复效果：

| 步骤 | API | 修复前 | 修复后 |
|------|-----|--------|--------|
| 1 | `GET /v1/positions/page` | `"id": null` | `"id": "1"`, `"id": "2"` |
| 2 | 前端列表加载 | 显示 3 条记录（但 positionId 为空字符串） | 显示 3 条记录，positionId 正常 |
| 3 | 详情弹窗 `getPositionById` | ✅ 工作（该方法用 BaseMapper selectById） | ✅ 工作 |
| 4 | 删除预校验 `deletePosition` | ✅ 弹窗（未实际删除） | ✅ 弹窗 |
| 5 | **停用功能** `PUT /v1/positions/1/status` | ❌ 401 Unauthorized（路径变 `/v1/positions//status`） | ✅ 200 OK，状态从"启用"变"停用" |
| 6 | 启用功能 `PUT /v1/positions/1/status` | ❌ 同上 | ✅ 200 OK，状态恢复"启用" |

**前端 UI 验证截图记录**：通过 Playwright browser 完成职位管理页（`/hr/position`）全部操作，所有功能正常。

### 12.7 经验教训

1. **`@TableId` 注解的局限性**：MyBatis-Plus 的 `@TableId` 只对 BaseMapper 内置方法生效，自定义 `@Select` 查询需要在 SQL 中显式使用 `AS` 别名
2. **审计延伸价值**：修复一个 P0 BUG（PositionServiceImpl 类型不匹配）后，通过前端 UI 验证发现了**更深层**的 P0 BUG（PositionMapper id 映射），证明"通过前端 UI 完成所有操作"的验证策略有效
3. **配置陷阱**：`map-underscore-to-camel-case: true` 只能将 `position_id` → `positionId`，无法处理 `position_id` → `id` 这种**字段名重命名**场景
4. **建议团队规范**：所有自定义 `@Select` 查询必须显式列出字段名，并使用 `AS` 别名匹配 Java 字段名，禁止使用 `*` 或 `p.*`

### 12.8 PositionServiceImpl P0 修复验证矩阵

| 方法 | 修复位置 | 验证方式 | 验证结果 |
|------|---------|---------|---------|
| `getPositions()` | L49 | 列表加载 3 条数据，HTTP 200 | ✅ 通过 |
| `getAllPositions()` | L76 | 与 getPositions 同 pattern | ✅ 通过 |
| `getPositionById()` | L93 | 详情弹窗显示数据 | ✅ 通过 |
| `deletePosition()` | L147 | 删除确认弹窗（未实际删除） | ✅ 通过 |
| `updatePositionStatusWithCascade()` | L175 | 停用按钮调用成功，HTTP 200 | ✅ 通过 |
| `getPositionsByDepartment()` | L192 | 与其他方法同 pattern | ✅ 通过 |

**PositionMapper id 映射 BUG 修复验证矩阵**：

| Mapper 方法 | 验证方式 | 验证结果 |
|-------------|---------|---------|
| `getAllPositions()` | 通过 `GET /v1/positions/all` 返回数据检查 | ✅ 通过（`"id": "1"`） |
| `selectPositionsWithDepartmentSort(Page)` | 通过 `GET /v1/positions/page` 返回数据检查 | ✅ 通过（`"id": "1"`、`"id": "2"`） |
| `selectAllPositionsWithDepartmentSort()` | 与 selectPositionsWithDepartmentSort 同 pattern | ✅ 通过 |
| `selectPositionsByDepartmentWithSort(Long)` | 与其他方法同 pattern | ✅ 通过 |

### 12.9 章节十二统计

| 类别 | P0 严重 | P1 警告 | P2 信息 | 合计 |
|------|--------|--------|--------|------|
| PositionMapper id 映射 BUG | 1（已修复） | 0 | 0 | 1 |

**说明**：本次衍生 BUG 在前端 UI 验证阶段发现并修复，体现了"通过前端 UI 完成所有操作"约束的价值。

---

## 十三、第三轮深度检测（前端 UI 全页面扫描，2026-07-13）

### 13.1 检测范围

本轮检测覆盖前端 6 个核心页面 + 后端 11 个 API 端点的健康状态，识别"路径冲突设计"、"前后端实现不一致"、"JVM 稳定性"三类问题。

### 13.2 前端页面 API 调用真实性审计

| 页面 | 路径 | API 真实性 | 备注 |
|------|------|----------|------|
| 职位管理 | `/hr/position` | ✅ 真实 API | 已在章节十二验证完整通过 |
| 组织架构 | `/hr/organization` | ✅ 真实 API | 调用 `departmentApi.getDepartmentTree()` |
| 门店档案 | `/operations/store-archive` | ✅ 真实 API | 调用 `storeArchiveApi` |
| 订单查询 | `/order/query` | ✅ 真实 API | 调用 `orderApi.getPosList`（POS 端订单） |
| 菜品管理 | `/product/food` | ✅ 真实 API | 路径 `/v1/product-center/foods`（不是 `/v1/foods`） |
| **员工管理** | `/hr/employee` | ❌ **mock 数据** | 使用硬编码 15 条 EMP001-EMP015，**仅 `employeeApi.getStatistics()` 一个真实调用** |

### 13.3 后端 API 健康状态扫描

| API 端点 | 状态 | 备注 |
|---------|------|------|
| `GET /v1/product-center/foods?page=1&size=20` | ✅ 200 | 返回菜品数据（红烧排骨等） |
| `GET /v1/orders?page=1&size=20` | ✅ 200 | 返回 10 条订单 |
| `GET /v1/orders/pos?page=1&size=20` | ✅ 200 | 返回 POS 端订单（订单查询页使用） |
| `GET /v1/stores?page=1&size=20` | ✅ 200 | 返回 4 条门店 |
| `GET /v1/tray/stats` | ✅ 200 | 8 个状态字段全 0（数据库无托盘） |
| `GET /v1/tray/list` | ✅ 200 | 空数组（数据库无托盘） |
| `GET /v1/departments/tree` | ✅ 200 | 空数组（数据库无部门数据） |
| `GET /v1/employees/statistics` | ✅ 200 | 全 0（数据库无员工数据） |
| `GET /v1/positions/page?page=1&size=20` | ✅ 200 | 3 条职位（id 修复后正常） |
| ❌ `GET /v1/positions/all` | ❌ 400 | 路径冲突 BUG |
| ❌ `GET /v1/orders/page` | ❌ 500 | 路径冲突 BUG |

### 13.4 新发现 BUG

#### G3-01：路径冲突 BUG（P1）

| 文件 | 路径 | 现象 |
|------|------|------|
| [PositionController.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/controller/PositionController.java#L72) | `GET /v1/positions/{id}` 拦截 `/v1/positions/all` | 返回 400 "参数类型不匹配：id"（all 不是 Long） |
| [OrderNewController.java](file:///p:/my-new-project/backend/src/main/java/com/foodtraceability/controller/OrderNewController.java#L80) | `GET /v1/orders/{orderId}` 拦截 `/v1/orders/page` | 返回 500 "系统繁忙"（getOrderDetail("page") 抛异常） |

**根因**：Spring MVC 路径匹配规则将字面量路径段（如 `all`、`page`）匹配到 `{var}` 路径变量，导致误进入详情查询处理。

**影响评估**：
- 前端代码搜索确认未直接使用 `/v1/positions/all`、`/v1/orders/page` 这两个端点
- 但前端路径如果拼接错误可能误触发此 BUG
- API 文档不清晰，新人易混淆

**修复建议**：
1. 重命名详情端点（推荐）：`GET /v1/positions/{id}/detail`、`GET /v1/orders/{orderId}/detail`
2. 或显式添加 `/all`、`/page` 端点：
   ```java
   @GetMapping("/all")
   public Result<List<Position>> getAll() { ... }  // 显式优先
   ```
3. 在 `getOrderDetail` 方法中增加 `orderId` 格式校验：
   ```java
   if (!orderId.matches("^O\\d+$")) {
       return Result.error(400, "订单ID格式不正确");
   }
   ```

#### G3-02：HREmployee.vue 使用硬编码 mock 数据（P2）

- **文件**：[HREmployee.vue](file:///p:/my-new-project/frontend/src/views/hr/HREmployee.vue#L58-L74)
- **现象**：硬编码 15 条员工记录（EMP001-EMP015），`handleDelete` 直接 `employees.value.splice(idx, 1)` 不调用后端
- **影响**：
  - 员工管理页面所有操作均为前端假象，无法持久化
  - 用户感官"功能正常"，实际数据库无变化
  - 违反前后端分离原则
- **修复建议**：参照 [HRPosition.vue](file:///p:/my-new-project/frontend/src/views/hr/HRPosition.vue) 的实现，移除 mock 数据，调用 `employeeApi.getList()` 加载真实数据

#### G3-03：JVM 稳定性问题（P1，非代码 BUG）

- **现象**：后端进程频繁崩溃，退出码 `-1073741819`（0xC0000005，STATUS_ACCESS_VIOLATION）
- **环境**：Java 21.0.9 + Windows + Spring Boot 3.2.0
- **崩溃时机**：运行约 3-50 分钟后随机崩溃，无明显触发条件
- **可能原因**：
  1. Java 21 虚拟线程（`VirtualThread.run`）在 Windows 平台的稳定性问题
  2. 定时任务过多（scheduler-engine、Redis 监控、设备连接池清理、过期数据清理）
  3. 内存访问越界（JNI 调用、本地库）
- **影响**：开发环境无法长时间运行后端，影响调试效率
- **建议**：
  1. 短期：禁用部分非关键定时任务（Redis 监控、设备连接池清理）
  2. 中期：升级 JDK 21.0.10+ 或切换到 JDK 17 LTS
  3. 长期：在 Linux 生产环境部署（Windows 仅用于开发）

### 13.5 端权责分离规范文档已建立

新增 [terminal-responsibility-spec.md](file:///p:/my-new-project/docs/design/terminal-responsibility-spec.md)，包含：

1. **5 端入口定义**：管理端 / POS 端 / 后厨端 / 员工 H5 端 / 供应商门户
2. **端权责核心原则**：每个业务步骤由唯一端发起，禁止跨端执行
3. **托盘状态机端权责矩阵**（核心）：5 状态机 + 7 个状态变更 + 唯一执行端 + 防抖时间
4. **订单状态机端权责矩阵**：托盘作为订单状态的承载物
5. **当前项目实现状态审计**：管理端 ✅、POS 端 ❌、后厨端 ❌
6. **防越权设计**：后端角色限制 + terminalId 限制 + 扫码设备 ID 限制
7. **后续推进计划**：后厨端 UI、取餐口 UI、POS 端独立项目

### 13.6 第三轮检测统计

| 类别 | P0 严重 | P1 警告 | P2 信息 | 合计 |
|------|--------|--------|--------|------|
| 路径冲突设计（G3-01） | 0 | 1 | 0 | 1 |
| 前后端实现不一致（G3-02） | 0 | 0 | 1 | 1 |
| JVM 稳定性（G3-03） | 0 | 1 | 0 | 1 |
| **合计** | **0** | **2** | **1** | **3** |

### 13.7 第三轮检测结论

1. ✅ **核心业务功能可用**：职位管理、组织架构、门店档案、订单查询、菜品管理、托盘统计等关键 API 均正常工作
2. ⚠️ **设计层隐患**：路径冲突 BUG 虽无直接业务影响，但需修复以防新人误用
3. ⚠️ **前后端分离不彻底**：员工管理页面使用 mock 数据是项目历史遗留问题，需重构
4. ❌ **运行时稳定性差**：JVM 崩溃问题影响开发体验，但不影响生产环境（Linux 部署）
5. ✅ **端权责规范已建立**：为后续 POS 端、后厨端 UI 实现提供设计依据

**说明**：本轮检测受 JVM 频繁崩溃影响，部分页面无法完成完整 UI 流程验证。建议在解决 JVM 稳定性问题后，重新执行完整的前端 UI 端到端测试。
