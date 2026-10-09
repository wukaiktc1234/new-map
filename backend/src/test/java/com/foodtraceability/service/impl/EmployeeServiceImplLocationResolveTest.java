package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.dto.EmployeeCreateDTO;
import com.foodtraceability.dto.EmployeeUpdateDTO;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.Location;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.LocationIdMapMapper;
import com.foodtraceability.mapper.LocationMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.OperationLogService;
import com.foodtraceability.service.OrganizationChangeLogService;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.utils.EmployeeCodeGenerator;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * EmployeeServiceImpl 归属解析单元测试 —— P1-USER-LOCATION-002 C2（D1-2 / D1-3）+ C2b（D1-4 / U9 / U10）。
 *
 * <p>验收点（设计文档 p1-user-location-002-design-001.md §2 D1-3 / §6 A2）：
 * 归属解析优先级 {@code locationId → storeId(桥) → warehouseId(桥) → null}，
 * 其中 <b>warehouseId 经桥</b>是缺口①的新路径（此前 {@code employees.location_id} 对仓库员工恒 NULL）。
 *
 * <p>C2b 验收点（§7 U9 / U10，D1-4 落点）：
 * {@code workLocationType} 永远由后端显式推导——有 locationId 按 U9 映射
 * （STORE→STORE，CENTRAL/DEPOT→WAREHOUSE），TRANSIT 或词表外值 → 400 显式拒绝；
 * 无 locationId → 创建路径显式 HEADQUARTERS（不走 DB DEFAULT），更新路径不 set（I-3）。
 *
 * <p>不变量 I-3：任何分支都不得产生数值兜底；全空入参必须落 NULL。
 *
 * <p>注意：
 * <ul>
 *   <li>{@code LocationIdBridge.instance} 为静态自注册字段，测试内显式构造并在 {@code @AfterEach} 清空，
 *       避免静态状态泄漏；</li>
 *   <li>{@code baseMapper} 是 {@code ServiceImpl} 的 protected 字段，需 {@link ReflectionTestUtils} 注入。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmployeeServiceImpl 归属解析单元测试 - locationId 优先级（缺口①②）")
class EmployeeServiceImplLocationResolveTest {

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
    private LocationIdMapMapper locationIdMapMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private UserMapper userMapper;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        // 构造即自注册静态桥实例（模拟 Spring 上下文）
        new LocationIdBridge(locationIdMapMapper);
        service = new EmployeeServiceImpl(positionService, departmentService,
                organizationChangeLogService, employeeCodeGenerator, locationMapper,
                operationLogService, userMapper);
        ReflectionTestUtils.setField(service, "baseMapper", employeeMapper);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(LocationIdBridge.class, "instance", null);
    }

    private EmployeeCreateDTO baseDto() {
        EmployeeCreateDTO dto = new EmployeeCreateDTO();
        dto.setName("测试员工");
        dto.setGender("male");
        dto.setHireDate(LocalDateTime.now());
        dto.setStatus(1);
        return dto;
    }

    private Location activeLocation(Long locationId, String locationType, int status) {
        Location location = new Location();
        location.setLocationId(locationId);
        location.setLocationType(locationType);
        location.setStatus(status);
        location.setDeleted(0);
        return location;
    }

    @Test
    @DisplayName("locationId 优先：直用 location_id，不经桥")
    void locationIdTakesPrecedenceOverLegacyFields() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(500L);
        dto.setStoreId(9L);        // 同时给出旧入参，应被忽略
        dto.setWarehouseId(77L);   // 同时给出旧入参，应被忽略
        when(locationMapper.selectById(500L)).thenReturn(activeLocation(500L, "STORE", 1));

        Employee saved = service.createEmployee(dto);

        assertEquals(Long.valueOf(500L), saved.getLocationId(),
                "locationId 非空时应直接作为 employees.location_id");
        verifyNoInteractions(locationIdMapMapper);
    }

    @Test
    @DisplayName("locationId 不存在 → 400 显式拒绝（不落库、不猜）")
    void unknownLocationIdIsRejected() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(999999L);
        when(locationMapper.selectById(999999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createEmployee(dto));

        assertEquals(Integer.valueOf(400), ex.getCode(), "非法归属入参应返回 400");
        verify(employeeMapper, never()).insert(any(Employee.class));
    }

    @Test
    @DisplayName("locationId 已逻辑删除 → 400 显式拒绝")
    void deletedLocationIdIsRejected() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(501L);
        Location deletedLocation = activeLocation(501L, "DEPOT", 1);
        deletedLocation.setDeleted(1);
        when(locationMapper.selectById(501L)).thenReturn(deletedLocation);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createEmployee(dto));

        assertEquals(Integer.valueOf(400), ex.getCode());
        verify(employeeMapper, never()).insert(any(Employee.class));
    }

    @Test
    @DisplayName("仅给 storeId（观察期兼容）→ 经桥 stores_new 换算")
    void legacyStoreIdResolvesViaBridge() {
        EmployeeCreateDTO dto = baseDto();
        dto.setStoreId(9L);
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, 9L))
                .thenReturn(900L);

        Employee saved = service.createEmployee(dto);

        assertEquals(Long.valueOf(900L), saved.getLocationId());
        verify(locationMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("仅给 warehouseId → 经桥 warehouses 换算（缺口①新路径，此前恒 NULL）")
    void legacyWarehouseIdNowResolvesViaBridge() {
        EmployeeCreateDTO dto = baseDto();
        dto.setWarehouseId(77L);
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_WAREHOUSES, 77L))
                .thenReturn(700L);

        Employee saved = service.createEmployee(dto);

        assertEquals(Long.valueOf(700L), saved.getLocationId(),
                "仓库员工归属应经 location_id_map 落到 location_id，不再恒 NULL");
    }

    @Test
    @DisplayName("仓库来源无映射行（8 个 E2E/TEST 仓）→ 落 NULL，不兜底")
    void unmappedWarehouseFallsToNullNotFallback() {
        EmployeeCreateDTO dto = baseDto();
        dto.setWarehouseId(888L);
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_WAREHOUSES, 888L))
                .thenReturn(null);

        Employee saved = service.createEmployee(dto);

        assertNull(saved.getLocationId(), "I-3：无映射必须显式 NULL，不得回落到任何默认 ID");
    }

    @Test
    @DisplayName("归属入参全空 → location_id 为 NULL（不落库归属、不猜）")
    void allAttributionInputsAbsentYieldsNull() {
        EmployeeCreateDTO dto = baseDto();

        Employee saved = service.createEmployee(dto);

        assertNull(saved.getLocationId());
        verifyNoInteractions(locationMapper);
        verifyNoInteractions(locationIdMapMapper);
    }

    @Test
    @DisplayName("locationId 指向停用位置（status=0）→ 仍按请求写入（本卡不扩权）")
    void disabledLocationIsStillHonouredWithWarningOnly() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(502L);
        when(locationMapper.selectById(502L)).thenReturn(activeLocation(502L, "CENTRAL", 0));

        Employee saved = service.createEmployee(dto);

        assertEquals(Long.valueOf(502L), saved.getLocationId(),
                "停用位置的存在性成立即视为合法入参；是否禁止绑定属后续卡决策，本卡不扩权");
    }

    @Test
    @DisplayName("桥未注册（非 Spring 上下文）→ 旧入参解析为 NULL 而非异常")
    void unregisteredBridgeYieldsNullNotException() {
        ReflectionTestUtils.setField(LocationIdBridge.class, "instance", null);
        EmployeeCreateDTO dto = baseDto();
        dto.setStoreId(9L);

        Employee saved = service.createEmployee(dto);

        if (saved.getLocationId() != null) {
            fail("非 Spring 上下文应返回 NULL，实际：" + saved.getLocationId());
        }
    }

    // ============ C2b：workLocationType 显式推导（D1-4 / U9 / U10） ============

    @Test
    @DisplayName("C2b：创建路径 locationId=STORE → 显式推导 workLocationType=STORE")
    void createWithStoreLocationDerivesStoreWorkLocationType() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(500L);
        when(locationMapper.selectById(500L)).thenReturn(activeLocation(500L, "STORE", 1));

        Employee saved = service.createEmployee(dto);

        assertEquals("STORE", saved.getWorkLocationType(), "U9：STORE → STORE");
    }

    @Test
    @DisplayName("C2b：创建路径 locationId=CENTRAL → 显式推导 workLocationType=WAREHOUSE")
    void createWithCentralLocationDerivesWarehouseWorkLocationType() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(503L);
        when(locationMapper.selectById(503L)).thenReturn(activeLocation(503L, "CENTRAL", 1));

        Employee saved = service.createEmployee(dto);

        assertEquals("WAREHOUSE", saved.getWorkLocationType(), "U9：CENTRAL → WAREHOUSE");
    }

    @Test
    @DisplayName("C2b：创建路径 locationId=DEPOT → 显式推导 workLocationType=WAREHOUSE")
    void createWithDepotLocationDerivesWarehouseWorkLocationType() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(504L);
        when(locationMapper.selectById(504L)).thenReturn(activeLocation(504L, "DEPOT", 1));

        Employee saved = service.createEmployee(dto);

        assertEquals("WAREHOUSE", saved.getWorkLocationType(), "U9：DEPOT → WAREHOUSE");
    }

    @Test
    @DisplayName("C2b：locationId 指向 TRANSIT 位置 → 400 显式拒绝（禁止作员工归属）")
    void createWithTransitLocationIsRejected() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(505L);
        when(locationMapper.selectById(505L)).thenReturn(activeLocation(505L, "TRANSIT", 1));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createEmployee(dto));

        assertEquals(Integer.valueOf(400), ex.getCode(), "U9：TRANSIT 禁止作员工归属 → 显式拒绝");
        verify(employeeMapper, never()).insert(any(Employee.class));
    }

    @Test
    @DisplayName("C2b：locationId 指向词表外 location_type → 400 显式拒绝（不猜）")
    void createWithUnknownLocationTypeIsRejected() {
        EmployeeCreateDTO dto = baseDto();
        dto.setLocationId(506L);
        when(locationMapper.selectById(506L)).thenReturn(activeLocation(506L, "FACTORY", 1));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createEmployee(dto));

        assertEquals(Integer.valueOf(400), ex.getCode(), "U9：词表外值同处理 → 显式拒绝，不猜");
        verify(employeeMapper, never()).insert(any(Employee.class));
    }

    @Test
    @DisplayName("C2b：创建路径无归属 → 显式 set workLocationType=HEADQUARTERS（不走 DB DEFAULT）")
    void createWithoutLocationIsExplicitlyHeadquarters() {
        EmployeeCreateDTO dto = baseDto();

        Employee saved = service.createEmployee(dto);

        assertNull(saved.getLocationId());
        assertEquals("HEADQUARTERS", saved.getWorkLocationType(),
                "U10：无 locationId → 显式 HEADQUARTERS（§3.2 总部职能 NULL 场景），非隐式落 DB DEFAULT");
    }

    @Test
    @DisplayName("C2b：storeId 桥路径 → 按来源推导 workLocationType=STORE（不查库）")
    void legacyStoreIdBridgeDerivesStoreWorkLocationType() {
        EmployeeCreateDTO dto = baseDto();
        dto.setStoreId(9L);
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, 9L))
                .thenReturn(900L);

        Employee saved = service.createEmployee(dto);

        assertEquals(Long.valueOf(900L), saved.getLocationId());
        assertEquals("STORE", saved.getWorkLocationType(),
                "U9：门店桥 → STORE（按来源推导，与桥分支不查库一致）");
        verify(locationMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("C2b：warehouseId 桥路径 → 按来源推导 workLocationType=WAREHOUSE")
    void legacyWarehouseIdBridgeDerivesWarehouseWorkLocationType() {
        EmployeeCreateDTO dto = baseDto();
        dto.setWarehouseId(77L);
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_WAREHOUSES, 77L))
                .thenReturn(700L);

        Employee saved = service.createEmployee(dto);

        assertEquals(Long.valueOf(700L), saved.getLocationId());
        assertEquals("WAREHOUSE", saved.getWorkLocationType(), "U9：仓库桥 → WAREHOUSE（按来源推导）");
    }

    @Test
    @DisplayName("C2b：更新路径 locationId=STORE → 显式推导 workLocationType=STORE")
    void updateWithStoreLocationDerivesStoreWorkLocationType() {
        Employee existing = new Employee();
        existing.setId(1L);
        when(employeeMapper.selectById(1L)).thenReturn(existing);
        when(locationMapper.selectById(500L)).thenReturn(activeLocation(500L, "STORE", 1));

        EmployeeUpdateDTO dto = new EmployeeUpdateDTO();
        dto.setLocationId(500L);

        service.updateEmployee("1", dto);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).updateById(captor.capture());
        assertEquals("STORE", captor.getValue().getWorkLocationType(),
                "U9：更新路径表达归属意图时同样显式推导");
    }

    @Test
    @DisplayName("C2b：更新路径无归属意图 → workLocationType 不 set（列不变，I-3）")
    void updateWithoutLocationLeavesWorkLocationTypeUntouched() {
        Employee existing = new Employee();
        existing.setId(1L);
        when(employeeMapper.selectById(1L)).thenReturn(existing);

        EmployeeUpdateDTO dto = new EmployeeUpdateDTO();
        dto.setName("测试员工");

        service.updateEmployee("1", dto);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeMapper).updateById(captor.capture());
        assertNull(captor.getValue().getWorkLocationType(),
                "未表达归属意图 → 不 set workLocationType，MyBatis-Plus 跳过该列，存量值不被清空（I-3）");
    }

    @Test
    @DisplayName("C4：updateEmployee 归属变更 → 反向同步 users.location_id（入口决定方向）")
    void updateEmployeeLocationChangeReverseSyncsUser() {
        Employee existing = new Employee();
        existing.setId(1L);
        existing.setUserId(9L);
        existing.setLocationId(2L);
        when(employeeMapper.selectById(1L)).thenReturn(existing);
        when(locationMapper.selectById(500L)).thenReturn(activeLocation(500L, "STORE", 1));

        User user = new User();
        user.setId(9L);
        user.setLocationId(2L);
        when(userMapper.selectById(9L)).thenReturn(user);

        EmployeeUpdateDTO dto = new EmployeeUpdateDTO();
        dto.setLocationId(500L);

        service.updateEmployee("1", dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(Long.valueOf(9L), userCaptor.getValue().getId());
        assertEquals(Long.valueOf(500L), userCaptor.getValue().getLocationId(),
                "U1：last-write-wins，用户既有归属（2）被员工侧（500）覆盖");
    }

    @Test
    @DisplayName("C4：updateEmployee 未表达归属 → 不调用反向同步（U2 边界）")
    void updateWithoutLocationDoesNotReverseSync() {
        Employee existing = new Employee();
        existing.setId(1L);
        existing.setUserId(9L);
        when(employeeMapper.selectById(1L)).thenReturn(existing);

        EmployeeUpdateDTO dto = new EmployeeUpdateDTO();
        dto.setName("测试员工");

        service.updateEmployee("1", dto);

        verifyNoInteractions(userMapper);
    }
}
