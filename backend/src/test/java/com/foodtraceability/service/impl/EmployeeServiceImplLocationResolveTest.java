package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.dto.EmployeeCreateDTO;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.Location;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.LocationIdMapMapper;
import com.foodtraceability.mapper.LocationMapper;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.OrganizationChangeLogService;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.utils.EmployeeCodeGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * EmployeeServiceImpl 归属解析单元测试 —— P1-USER-LOCATION-002 C2（D1-2 / D1-3）。
 *
 * <p>验收点（设计文档 p1-user-location-002-design-001.md §2 D1-3 / §6 A2）：
 * 归属解析优先级 {@code locationId → storeId(桥) → warehouseId(桥) → null}，
 * 其中 <b>warehouseId 经桥</b>是缺口①的新路径（此前 {@code employees.location_id} 对仓库员工恒 NULL）。
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

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        // 构造即自注册静态桥实例（模拟 Spring 上下文）
        new LocationIdBridge(locationIdMapMapper);
        service = new EmployeeServiceImpl(positionService, departmentService,
                organizationChangeLogService, employeeCodeGenerator, locationMapper);
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
}
