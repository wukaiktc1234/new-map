package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.Location;
import com.foodtraceability.entity.OperationLogEntity;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.LocationMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.OperationLogService;
import com.foodtraceability.service.OrganizationChangeLogService;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.utils.EmployeeCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * C3 员工归属分配单元测试（p1-user-location-002-design-001.md D3-2/D3-4）。
 *
 * <p>覆盖验收点 A4 的服务侧语义：
 * <ul>
 *   <li>归属两列（location_id + work_location_type）写入，workLocationType 由后端 U9 推导；</li>
 *   <li>审计行模块 EMPLOYEE_LOCATION_ASSIGN、type UPDATE、desc 含「旧值 → 新值」；</li>
 *   <li>TRANSIT / 不存在位置 / 员工不存在 → 400/404 显式拒绝，不落库不留审计；</li>
 *   <li>审计失败仅告警不阻断主流程（TaxRecordServiceImpl 先例）。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmployeeServiceImpl.assignEmployeeLocation 单元测试 - C3 独立归属端点（D3-2/D3-4）")
class EmployeeServiceImplAssignLocationTest {

    @Mock
    private PositionService positionService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private OrganizationChangeLogService organizationChangeLogService;

    @Mock
    private EmployeeCodeGenerator employeeCodeGenerator;

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeServiceImpl(positionService, departmentService,
                organizationChangeLogService, employeeCodeGenerator, locationMapper,
                operationLogService, userMapper);
        // baseMapper 是 ServiceImpl 的 protected 字段
        ReflectionTestUtils.setField(service, "baseMapper", employeeMapper);
    }

    private Employee existingEmployee(Long locationId, String workLocationType) {
        Employee e = new Employee();
        e.setId(100L);
        e.setName("张三");
        e.setLocationId(locationId);
        e.setWorkLocationType(workLocationType);
        return e;
    }

    private Location location(Long id, String type) {
        Location l = new Location();
        l.setLocationId(id);
        l.setLocationType(type);
        l.setStatus(1);
        l.setDeleted(0);
        return l;
    }

    @Test
    @DisplayName("归属变更成功：两列写入 + 审计行 desc 含旧值→新值（A4 核心）")
    void successWritesAttributionAndAuditContainsOldToNew() {
        when(employeeMapper.selectById(100L)).thenReturn(existingEmployee(3L, "STORE"));
        when(locationMapper.selectById(7L)).thenReturn(location(7L, "CENTRAL"));

        Employee result = service.assignEmployeeLocation("100", 7L);

        assertNotNull(result);
        // 归属两列：location_id + work_location_type（U9：CENTRAL → WAREHOUSE）
        ArgumentCaptor<Employee> updateCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).updateById(updateCaptor.capture());
        Employee written = updateCaptor.getValue();
        assertEquals(Long.valueOf(100L), written.getId());
        assertEquals(Long.valueOf(7L), written.getLocationId(), "location_id 应写入目标位置");
        assertEquals("WAREHOUSE", written.getWorkLocationType(), "U9 映射：CENTRAL → WAREHOUSE");

        // 其余档案字段一律不动（null → updateById 跳过该列，I-3）
        assertNull(written.getName());
        assertNull(written.getDepartmentId());
        assertNull(written.getPositionId());

        // D3-4 审计行：模块 EMPLOYEE_LOCATION_ASSIGN、type UPDATE、desc 含旧值→新值
        ArgumentCaptor<OperationLogEntity> logCaptor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(operationLogService).logOperation(logCaptor.capture());
        OperationLogEntity opLog = logCaptor.getValue();
        assertEquals("EMPLOYEE_LOCATION_ASSIGN", opLog.getOperationModule());
        assertEquals("UPDATE", opLog.getOperationType());
        assertEquals("SUCCESS", opLog.getStatus());
        assertTrue(opLog.getOperationDesc().contains("3 → 7"),
                "desc 应含 locationId 旧值→新值，实际: " + opLog.getOperationDesc());
        assertTrue(opLog.getOperationDesc().contains("STORE → WAREHOUSE"),
                "desc 应含 workLocationType 旧值→新值，实际: " + opLog.getOperationDesc());
    }

    @Test
    @DisplayName("TRANSIT 型位置 → 400 显式拒绝，不落库不留审计")
    void transitLocationIsRejected() {
        when(employeeMapper.selectById(100L)).thenReturn(existingEmployee(null, null));
        when(locationMapper.selectById(9L)).thenReturn(location(9L, "TRANSIT"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assignEmployeeLocation("100", 9L));

        assertEquals(Integer.valueOf(400), ex.getCode(), "U9：TRANSIT 禁止作员工归属，显式拒绝不猜");
        verify(employeeMapper, never()).updateById(any(Employee.class));
        verify(operationLogService, never()).logOperation(any(OperationLogEntity.class));
    }

    @Test
    @DisplayName("位置不存在 → 400 显式拒绝，不落库不留审计")
    void unknownLocationIsRejected() {
        when(employeeMapper.selectById(100L)).thenReturn(existingEmployee(null, null));
        when(locationMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assignEmployeeLocation("100", 999L));

        assertEquals(Integer.valueOf(400), ex.getCode());
        verify(employeeMapper, never()).updateById(any(Employee.class));
        verify(operationLogService, never()).logOperation(any(OperationLogEntity.class));
    }

    @Test
    @DisplayName("员工不存在 → 404")
    void employeeNotFoundThrows404() {
        when(employeeMapper.selectById(100L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assignEmployeeLocation("100", 7L));

        assertEquals(Integer.valueOf(404), ex.getCode());
        verify(employeeMapper, never()).updateById(any(Employee.class));
        verify(operationLogService, never()).logOperation(any(OperationLogEntity.class));
    }

    @Test
    @DisplayName("审计写入失败 → 仅告警不阻断主流程（TaxRecordServiceImpl 先例）")
    void auditFailureDoesNotBreakMainFlow() {
        when(employeeMapper.selectById(100L)).thenReturn(existingEmployee(3L, "STORE"));
        when(locationMapper.selectById(7L)).thenReturn(location(7L, "STORE"));
        doThrow(new RuntimeException("audit db down")).when(operationLogService)
                .logOperation(any(OperationLogEntity.class));

        Employee result = assertDoesNotThrow(() -> service.assignEmployeeLocation("100", 7L));

        assertNotNull(result, "主流程（归属写入）必须成功，审计失败不得回滚业务语义");
        verify(employeeMapper).updateById(any(Employee.class));
    }

    @Test
    @DisplayName("员工ID格式无效 → 400")
    void invalidIdFormatThrows400() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.assignEmployeeLocation("abc", 7L));

        assertEquals(Integer.valueOf(400), ex.getCode());
        verify(employeeMapper, never()).updateById(any(Employee.class));
        verify(operationLogService, never()).logOperation(any(OperationLogEntity.class));
    }

    // ---- C4（D3-1）：员工 → 用户反向同步 syncUserAssignmentFromEmployee ----

    @Test
    @DisplayName("C4：反向同步按 employees.user_id 优先写 users.location_id（U1），审计 desc 含 users 侧旧 → 新")
    void assignLocationReverseSyncByUserIdWritesUserLocationId() {
        Employee emp = existingEmployee(3L, "STORE");
        emp.setUserId(9L);
        when(employeeMapper.selectById(100L)).thenReturn(emp);
        when(locationMapper.selectById(7L)).thenReturn(location(7L, "STORE"));
        User user = new User();
        user.setId(9L);
        user.setLocationId(12L);
        when(userMapper.selectById(9L)).thenReturn(user);

        service.assignEmployeeLocation("100", 7L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(Long.valueOf(9L), userCaptor.getValue().getId());
        assertEquals(Long.valueOf(7L), userCaptor.getValue().getLocationId(),
                "U1：last-write-wins，员工侧写入优先");
        verify(userMapper, never()).selectOne(any());
        ArgumentCaptor<OperationLogEntity> logCaptor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(operationLogService).logOperation(logCaptor.capture());
        assertTrue(logCaptor.getValue().getOperationDesc().contains("12 → 7"),
                "U1：审计 desc 必须记录 users 侧旧值 → 新值");
    }

    @Test
    @DisplayName("C4：userId 为 null → employee_code 兜底反查用户（D3-1 关联键）")
    void assignLocationReverseSyncFallsBackToEmployeeCode() {
        Employee emp = existingEmployee(3L, "STORE");
        emp.setEmployeeCode("E001");
        when(employeeMapper.selectById(100L)).thenReturn(emp);
        when(locationMapper.selectById(7L)).thenReturn(location(7L, "STORE"));
        User user = new User();
        user.setId(44L);
        user.setLocationId(5L);
        when(userMapper.selectOne(any())).thenReturn(user);

        service.assignEmployeeLocation("100", 7L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(Long.valueOf(44L), userCaptor.getValue().getId());
        assertEquals(Long.valueOf(7L), userCaptor.getValue().getLocationId());
    }

    @Test
    @DisplayName("C4：无对应用户 → 不动 users、不建号、不报错（U2），审计 desc 如实记录")
    void assignLocationReverseSyncNoUserSkipsWithoutError() {
        Employee emp = existingEmployee(3L, "STORE");
        when(employeeMapper.selectById(100L)).thenReturn(emp);
        when(locationMapper.selectById(7L)).thenReturn(location(7L, "STORE"));

        Employee result = assertDoesNotThrow(() -> service.assignEmployeeLocation("100", 7L));

        assertNotNull(result, "主流程必须成功：员工侧写入 + 审计照常");
        verify(employeeMapper).updateById(any(Employee.class));
        verifyNoInteractions(userMapper);
        ArgumentCaptor<OperationLogEntity> logCaptor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(operationLogService).logOperation(logCaptor.capture());
        assertTrue(logCaptor.getValue().getOperationDesc().contains("未找到对应用户"),
                "U2：审计 desc 应如实记录 users 未动");
    }

    @Test
    @DisplayName("C4：用户已是新归属 → 不重复写（no-op），审计 desc 如实记录")
    void assignLocationReverseSyncUserAlreadyAtNewLocationSkipsWrite() {
        Employee emp = existingEmployee(3L, "STORE");
        emp.setUserId(9L);
        when(employeeMapper.selectById(100L)).thenReturn(emp);
        when(locationMapper.selectById(7L)).thenReturn(location(7L, "STORE"));
        User user = new User();
        user.setId(9L);
        user.setLocationId(7L); // 已是新值
        when(userMapper.selectById(9L)).thenReturn(user);

        service.assignEmployeeLocation("100", 7L);

        verify(userMapper, never()).updateById(any(User.class));
        ArgumentCaptor<OperationLogEntity> logCaptor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(operationLogService).logOperation(logCaptor.capture());
        assertTrue(logCaptor.getValue().getOperationDesc().contains("未重复写"),
                "审计 desc 应记录 no-op 事实，不静默跳过");
    }
}
