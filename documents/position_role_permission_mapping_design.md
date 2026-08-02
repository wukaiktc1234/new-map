# 职位-角色权限映射方案设计文档

## 一、方案概述

### 1.1 设计目标
实现员工权限的自动化分配和管理，包括：
- **功能权限**：职位自动关联角色
- **数据权限**：具体门店/部门关联
- **自动分配**：员工入职/职位变更时自动分配权限

### 1.2 核心特性
1. **职位-角色映射**：建立职位与角色的自动映射关系
2. **数据权限管理**：支持门店、部门级别的数据权限控制
3. **自动分配机制**：员工入职、职位变更、门店分配时自动调整权限
4. **权限追溯**：完整的权限变更日志记录
5. **灵活配置**：支持手动调整和自动分配两种模式

## 二、数据库设计

### 2.1 表结构设计

#### 2.1.1 position_role_mapping（职位-角色映射表）
| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | BIGINT | 主键ID |
| position_id | BIGINT | 职位ID |
| role_id | BIGINT | 角色ID |
| is_primary | TINYINT | 是否主角色（1:是, 0:否） |
| priority | INT | 优先级（数字越大优先级越高） |
| status | TINYINT | 状态（1:启用, 0:禁用） |
| description | VARCHAR(500) | 映射描述 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| created_by | VARCHAR(36) | 创建人 |
| updated_by | VARCHAR(36) | 更新人 |
| deleted | TINYINT | 逻辑删除标记 |

**索引设计**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_position_role (position_id, role_id, deleted)
- KEY idx_position_id (position_id)
- KEY idx_role_id (role_id)

#### 2.1.2 employee_data_scope（员工数据权限表）
| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | BIGINT | 主键ID |
| employee_id | VARCHAR(36) | 员工ID |
| scope_type | VARCHAR(20) | 权限范围类型（store/department/all） |
| scope_id | VARCHAR(50) | 权限范围ID |
| scope_name | VARCHAR(100) | 权限范围名称 |
| source | VARCHAR(50) | 权限来源（auto/manual） |
| status | TINYINT | 状态（1:启用, 0:禁用） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| created_by | VARCHAR(36) | 创建人 |
| updated_by | VARCHAR(36) | 更新人 |
| deleted | TINYINT | 逻辑删除标记 |

**索引设计**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_employee_scope (employee_id, scope_type, scope_id, deleted)
- KEY idx_employee_id (employee_id)
- KEY idx_scope_type (scope_type)
- KEY idx_scope_id (scope_id)

#### 2.1.3 permission_assignment_log（权限分配日志表）
| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | BIGINT | 主键ID |
| employee_id | VARCHAR(36) | 员工ID |
| operation_type | VARCHAR(50) | 操作类型 |
| old_position_id | VARCHAR(36) | 原职位ID |
| new_position_id | VARCHAR(36) | 新职位ID |
| old_roles | TEXT | 原角色列表（JSON） |
| new_roles | TEXT | 新角色列表（JSON） |
| old_data_scopes | TEXT | 原数据权限（JSON） |
| new_data_scopes | TEXT | 新数据权限（JSON） |
| operator | VARCHAR(36) | 操作人 |
| operated_at | DATETIME | 操作时间 |
| remark | TEXT | 备注 |

**索引设计**：
- PRIMARY KEY (id)
- KEY idx_employee_id (employee_id)
- KEY idx_operation_type (operation_type)
- KEY idx_operated_at (operated_at)

### 2.2 表关系图

```
positions (职位表)
    ↓ (1:N)
position_role_mapping (职位-角色映射表)
    ↓ (N:1)
roles (角色表)

employees (员工表)
    ↓ (1:N)
employee_data_scope (员工数据权限表)
    ↓ (关联)
stores (门店表) / departments (部门表)

employees (员工表)
    ↓ (1:N)
permission_assignment_log (权限分配日志表)
```

## 三、业务流程设计

### 3.1 员工入职权限分配流程

```
1. 创建员工记录
   ↓
2. 调用权限自动分配服务
   - 参数：employeeId, positionId, storeId, departmentId
   ↓
3. 查询职位关联的角色
   - 从 position_role_mapping 表查询
   ↓
4. 分配角色权限
   - 插入 user_roles 表
   - 更新 users.roles 字段
   ↓
5. 分配数据权限
   - 插入 employee_data_scope 表
   - source = 'auto'
   ↓
6. 记录权限分配日志
   - 插入 permission_assignment_log 表
   ↓
7. 返回分配结果
```

### 3.2 员工职位变更权限调整流程

```
1. 更新员工职位信息
   ↓
2. 调用权限自动分配服务
   - 参数：employeeId, oldPositionId, newPositionId
   ↓
3. 查询新职位关联的角色
   ↓
4. 调整角色权限
   - 删除旧角色关联（可选）
   - 添加新角色关联
   ↓
5. 调整数据权限（如果需要）
   ↓
6. 记录权限变更日志
   ↓
7. 返回调整结果
```

### 3.3 员工门店分配权限调整流程

```
1. 更新员工门店信息
   ↓
2. 调用权限自动分配服务
   - 参数：employeeId, storeId
   ↓
3. 更新数据权限
   - 添加门店数据权限
   - source = 'auto'
   ↓
4. 记录权限变更日志
   ↓
5. 返回调整结果
```

### 3.4 职位-角色映射变更流程

```
1. 更新职位-角色映射关系
   ↓
2. 调用重新计算权限服务
   - 参数：positionId
   ↓
3. 查询该职位下的所有员工
   ↓
4. 批量重新分配权限
   - 遍历员工列表
   - 调用权限自动分配服务
   ↓
5. 返回影响的员工数量
```

## 四、核心代码实现

### 4.1 实体类
- [PositionRoleMapping.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/entity/PositionRoleMapping.java)
- [EmployeeDataScope.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/entity/EmployeeDataScope.java)
- [PermissionAssignmentLog.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/entity/PermissionAssignmentLog.java)

### 4.2 DTO类
- [PermissionAssignmentDTO.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/dto/PermissionAssignmentDTO.java)
- [PermissionAssignmentResultDTO.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/dto/PermissionAssignmentResultDTO.java)

### 4.3 Mapper接口
- [PositionRoleMappingMapper.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/mapper/PositionRoleMappingMapper.java)
- [EmployeeDataScopeMapper.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/mapper/EmployeeDataScopeMapper.java)
- [PermissionAssignmentLogMapper.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/mapper/PermissionAssignmentLogMapper.java)

### 4.4 服务层
- [PermissionAutoAssignService.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/service/PermissionAutoAssignService.java)
- [PermissionAutoAssignServiceImpl.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/service/impl/PermissionAutoAssignServiceImpl.java)

### 4.5 控制器
- [PermissionAutoAssignController.java](file:///p:/my-new-project/backend/src/main/java/com/example/demo/controller/PermissionAutoAssignController.java)

## 五、API接口设计

### 5.1 自动分配权限
- **接口路径**：`POST /api/v1/permission-auto-assign/assign`
- **请求参数**：
  ```json
  {
    "employeeId": "1234567890",
    "positionId": "1234567890",
    "storeId": "1234567890",
    "departmentId": "1234567890",
    "operationType": "onboard",
    "forceOverride": false,
    "remark": "员工入职自动分配权限"
  }
  ```
- **响应结果**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "employeeId": "1234567890",
      "employeeName": "张三",
      "assignedRoles": [
        {
          "roleId": "1234567890",
          "roleCode": "STORE_MANAGER",
          "roleName": "店长角色",
          "isPrimary": true
        }
      ],
      "assignedDataScopes": [
        {
          "scopeType": "store",
          "scopeId": "1234567890",
          "scopeName": "中心店",
          "source": "auto"
        }
      ],
      "operationType": "onboard",
      "success": true,
      "message": "权限分配成功"
    }
  }
  ```

### 5.2 员工入职权限分配
- **接口路径**：`POST /api/v1/permission-auto-assign/onboarding`
- **请求参数**：
  - employeeId: 员工ID
  - positionId: 职位ID
  - storeId: 门店ID（可选）
  - departmentId: 部门ID（可选）
  - operator: 操作人（可选）

### 5.3 职位变更权限调整
- **接口路径**：`POST /api/v1/permission-auto-assign/position-change`
- **请求参数**：
  - employeeId: 员工ID
  - oldPositionId: 原职位ID
  - newPositionId: 新职位ID
  - operator: 操作人（可选）

### 5.4 门店分配权限调整
- **接口路径**：`POST /api/v1/permission-auto-assign/store-assign`
- **请求参数**：
  - employeeId: 员工ID
  - storeId: 门店ID
  - operator: 操作人（可选）

### 5.5 获取职位关联角色
- **接口路径**：`GET /api/v1/permission-auto-assign/position/{positionId}/roles`
- **路径参数**：
  - positionId: 职位ID

### 5.6 获取员工数据权限
- **接口路径**：`GET /api/v1/permission-auto-assign/employee/{employeeId}/data-scopes`
- **路径参数**：
  - employeeId: 员工ID

### 5.7 清除自动分配权限
- **接口路径**：`DELETE /api/v1/permission-auto-assign/employee/{employeeId}/auto-permissions`
- **路径参数**：
  - employeeId: 员工ID
- **请求参数**：
  - operator: 操作人（可选）

### 5.8 重新计算员工权限
- **接口路径**：`POST /api/v1/permission-auto-assign/position/{positionId}/recalculate`
- **路径参数**：
  - positionId: 职位ID
- **请求参数**：
  - operator: 操作人（可选）

## 六、使用示例

### 6.1 配置职位-角色映射

```sql
-- 店长职位 -> 店长角色
INSERT INTO position_role_mapping (position_id, role_id, is_primary, priority, description, created_by)
SELECT p.id, r.ID, 1, 10, '店长职位自动关联店长角色', 'system'
FROM positions p, roles r
WHERE p.position_code = 'STORE_MANAGER' AND r.ROLE_CODE = 'STORE_MANAGER';

-- 采购经理职位 -> 采购经理角色
INSERT INTO position_role_mapping (position_id, role_id, is_primary, priority, description, created_by)
SELECT p.id, r.ID, 1, 10, '采购经理职位自动关联采购经理角色', 'system'
FROM positions p, roles r
WHERE p.position_code = 'PURCHASE_MANAGER' AND r.ROLE_CODE = 'PURCHASE_MANAGER';
```

### 6.2 员工入职时自动分配权限

```java
// 在员工入职服务中调用
@Autowired
private PermissionAutoAssignService permissionAutoAssignService;

public void onboarding(Employee employee) {
    // 1. 创建员工记录
    employeeMapper.insert(employee);
    
    // 2. 自动分配权限
    PermissionAssignmentResultDTO result = permissionAutoAssignService.assignOnOnboarding(
        employee.getId(),
        employee.getPositionId(),
        employee.getStoreId() != null ? employee.getStoreId().toString() : null,
        employee.getDepartmentId(),
        "system"
    );
    
    if (!result.getSuccess()) {
        logger.error("员工入职权限分配失败: {}", result.getMessage());
    }
}
```

### 6.3 员工职位变更时自动调整权限

```java
public void changePosition(String employeeId, String newPositionId) {
    // 1. 查询员工信息
    Employee employee = employeeMapper.selectById(employeeId);
    String oldPositionId = employee.getPositionId();
    
    // 2. 更新职位
    employee.setPositionId(newPositionId);
    employeeMapper.updateById(employee);
    
    // 3. 自动调整权限
    PermissionAssignmentResultDTO result = permissionAutoAssignService.assignOnPositionChange(
        employeeId,
        oldPositionId,
        newPositionId,
        "system"
    );
    
    if (!result.getSuccess()) {
        logger.error("职位变更权限调整失败: {}", result.getMessage());
    }
}
```

## 七、注意事项

### 7.1 数据一致性
1. 使用事务确保权限分配的原子性
2. 权限变更时同步更新用户表和角色关联表
3. 记录完整的权限变更日志

### 7.2 性能优化
1. 为常用查询字段添加索引
2. 批量操作时使用批量插入/更新
3. 权限查询结果缓存

### 7.3 安全性
1. 权限分配操作需要记录操作人
2. 手动分配的权限不应被自动分配覆盖（除非强制覆盖）
3. 定期审计权限分配日志

### 7.4 扩展性
1. 支持自定义权限分配规则
2. 支持多角色、多数据权限
3. 支持权限继承和覆盖

## 八、后续优化建议

### 8.1 功能增强
1. 添加权限预览功能（分配前预览将要分配的权限）
2. 支持权限模板（预定义权限组合）
3. 添加权限冲突检测和解决机制

### 8.2 性能优化
1. 使用Redis缓存权限信息
2. 异步处理批量权限分配
3. 定期清理过期的权限记录

### 8.3 监控告警
1. 权限分配失败告警
2. 权限变更异常告警
3. 权限分配性能监控

## 九、总结

本方案通过建立职位-角色映射关系，实现了员工权限的自动化分配和管理。核心优势包括：

1. **自动化**：减少人工干预，提高效率
2. **灵活性**：支持自动分配和手动调整
3. **可追溯**：完整的权限变更日志
4. **可维护**：清晰的代码结构和文档

通过本方案，可以大大简化权限管理工作，确保权限分配的准确性和一致性。
