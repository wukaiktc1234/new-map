package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.InventoryLocationCreateDTO;
import com.foodtraceability.dto.InventoryLocationUpdateDTO;
import com.foodtraceability.dto.InventoryLocationVO;
import com.foodtraceability.entity.InventoryLocation;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.mapper.InventoryLocationMapper;
import com.foodtraceability.service.WarehouseService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventoryLocationServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：库位创建（编码唯一 + 仓库存在双闸门、默认启用/零库存）、更新（非空字段覆盖）、
 * 分页查询（仓库名批量回填、空页不查仓库）、详情、启用/停用、按仓库查启用库位、
 * 删除，以及 VO 装配中的库位类型/状态名称映射与容量使用率计算。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryLocationServiceImplTest {

    @Mock
    private InventoryLocationMapper locationMapper;

    @Mock
    private WarehouseService warehouseService;

    @InjectMocks
    private InventoryLocationServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, InventoryLocation.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", locationMapper);
        when(locationMapper.insert(any(InventoryLocation.class))).thenReturn(1);
        when(locationMapper.updateById(any(InventoryLocation.class))).thenReturn(1);
        when(locationMapper.deleteById(any(Long.class))).thenReturn(1);
        when(locationMapper.selectCount(any())).thenReturn(0L);
    }

    private static InventoryLocation location(Long id, Long warehouseId, String code, Integer type, Integer status) {
        InventoryLocation l = new InventoryLocation();
        l.setLocationId(id);
        l.setWarehouseId(warehouseId);
        l.setLocationCode(code);
        l.setLocationName("库位-" + code);
        l.setLocationType(type);
        l.setStatus(status);
        l.setCurrentQuantity(new BigDecimal("30"));
        l.setMaxCapacity(new BigDecimal("100"));
        return l;
    }

    private static Warehouse warehouse(Long id, String name) {
        Warehouse w = new Warehouse();
        w.setWarehouseId(id);
        w.setWarehouseName(name);
        return w;
    }

    private static InventoryLocationCreateDTO createDTO() {
        InventoryLocationCreateDTO dto = new InventoryLocationCreateDTO();
        dto.setWarehouseId(10L);
        dto.setLocationCode("A-01");
        dto.setLocationName("A区一号");
        dto.setLocationType(3);
        dto.setMaxCapacity(new BigDecimal("500"));
        dto.setRemark("冷藏");
        return dto;
    }

    // ==================== 创建 ====================

    @Test
    @DisplayName("createLocation：编码重复时抛异常，不校验仓库、不写库")
    void createLocation_duplicateCode_rejected() {
        when(locationMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createLocation(createDTO()));

        assertTrue(ex.getMessage().contains("库位编码已存在"));
        verify(warehouseService, never()).getById(anyLong());
        verify(locationMapper, never()).insert(any(InventoryLocation.class));
    }

    @Test
    @DisplayName("createLocation：仓库不存在时抛异常，不写库")
    void createLocation_missingWarehouse_rejected() {
        when(warehouseService.getById(10L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createLocation(createDTO()));

        assertTrue(ex.getMessage().contains("仓库不存在"));
        verify(locationMapper, never()).insert(any(InventoryLocation.class));
    }

    @Test
    @DisplayName("createLocation：成功时空库存、默认启用、补齐时间戳")
    void createLocation_success_setsDefaults() {
        when(warehouseService.getById(10L)).thenReturn(warehouse(10L, "一号仓"));

        InventoryLocation saved = service.createLocation(createDTO());

        assertEquals(10L, saved.getWarehouseId());
        assertEquals("A-01", saved.getLocationCode());
        assertEquals(3, saved.getLocationType());
        assertEquals(new BigDecimal("500"), saved.getMaxCapacity());
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getCurrentQuantity()));
        assertEquals(1, saved.getStatus());
        assertEquals("冷藏", saved.getRemark());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(locationMapper).insert(saved);
        assertTrue(saved.getLocationCode().startsWith("A-"));
    }

    // ==================== 更新 / 状态 / 删除 ====================

    @Test
    @DisplayName("updateLocation：不存在抛异常")
    void updateLocation_missing_throws() {
        when(locationMapper.selectById(404L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> service.updateLocation(404L, new InventoryLocationUpdateDTO()));
        verify(locationMapper, never()).updateById(any(InventoryLocation.class));
    }

    @Test
    @DisplayName("updateLocation：只覆盖非空字段，返回重新查询结果")
    void updateLocation_partialUpdate() {
        InventoryLocation existing = location(1L, 10L, "A-01", 1, 1);
        when(locationMapper.selectById(1L)).thenReturn(existing, existing);

        InventoryLocationUpdateDTO dto = new InventoryLocationUpdateDTO();
        dto.setStatus(0);
        dto.setRemark(null);
        service.updateLocation(1L, dto);

        ArgumentCaptor<InventoryLocation> captor = ArgumentCaptor.forClass(InventoryLocation.class);
        verify(locationMapper).updateById(captor.capture());
        InventoryLocation saved = captor.getValue();
        assertEquals(0, saved.getStatus());
        assertEquals("库位-A-01", saved.getLocationName(), "未传字段保持原值");
        assertEquals(1, saved.getLocationType());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("toggleLocationStatus：不存在抛异常；存在则改状态并更新时间")
    void toggleLocationStatus_updates() {
        when(locationMapper.selectById(404L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.toggleLocationStatus(404L, 0));

        InventoryLocation existing = location(2L, 10L, "B-01", 2, 1);
        when(locationMapper.selectById(2L)).thenReturn(existing);
        service.toggleLocationStatus(2L, 0);

        assertEquals(0, existing.getStatus());
        assertNotNull(existing.getUpdateTime());
        verify(locationMapper).updateById(existing);
    }

    @Test
    @DisplayName("deleteLocation：不存在抛异常；存在则走逻辑删除")
    void deleteLocation_guardsAndRemoves() {
        when(locationMapper.selectById(404L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.deleteLocation(404L));

        when(locationMapper.selectById(3L)).thenReturn(location(3L, 10L, "C-01", 1, 1));
        service.deleteLocation(3L);
        verify(locationMapper).deleteById(3L);
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("getLocationsByWarehouseId：过滤 warehouse_id + status=1，并按编码升序")
    void getLocationsByWarehouseId_filtersEnabled() {
        List<InventoryLocation> rows = List.of(location(1L, 10L, "A-01", 1, 1));
        when(locationMapper.selectList(any())).thenReturn(rows);

        assertSame(rows, service.getLocationsByWarehouseId(10L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<InventoryLocation>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(locationMapper).selectList(captor.capture());
        LambdaQueryWrapper<InventoryLocation> wrapper = (LambdaQueryWrapper<InventoryLocation>) captor.getValue();
        String sql = wrapper.getSqlSegment().toLowerCase();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(10L), "应按仓库过滤");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(1), "应只取启用状态");
        assertTrue(sql.contains("order by"), "应按库位编码排序");
    }

    @Test
    @DisplayName("getLocationDetail：不存在抛异常；存在时回填仓库名并计算使用率")
    void getLocationDetail_enrichesWarehouseNameAndUsageRate() {
        when(locationMapper.selectById(404L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.getLocationDetail(404L));

        when(locationMapper.selectById(1L)).thenReturn(location(1L, 10L, "A-01", 3, 1));
        when(warehouseService.getById(10L)).thenReturn(warehouse(10L, "一号仓"));

        InventoryLocationVO vo = service.getLocationDetail(1L);

        assertEquals("一号仓", vo.getWarehouseName());
        assertEquals("冷藏区", vo.getLocationTypeName());
        assertEquals("启用", vo.getStatusName());
        assertEquals(0, new BigDecimal("30.00").compareTo(vo.getUsageRate()), "30/100 = 30.00%");
    }

    @Test
    @DisplayName("getLocationDetail：仓库查不到时仓库名为 null，不抛错")
    void getLocationDetail_missingWarehouse_leavesNameNull() {
        when(locationMapper.selectById(1L)).thenReturn(location(1L, 10L, "A-01", 99, 9));
        when(warehouseService.getById(10L)).thenReturn(null);

        InventoryLocationVO vo = service.getLocationDetail(1L);

        assertNull(vo.getWarehouseName());
        assertEquals("未知", vo.getLocationTypeName());
        assertEquals("未知", vo.getStatusName());
    }

    @Test
    @DisplayName("getLocationPage：批量回填仓库名、空页不查仓库、使用率与类型名正确")
    void getLocationPage_mapsRecordsAndWarehouseNames() {
        Page<InventoryLocation> page = new Page<>(1, 10);
        page.setRecords(List.of(
                location(1L, 10L, "A-01", 1, 1),
                location(2L, 10L, "A-02", 2, 0),
                location(3L, null, "A-03", 4, 1)
        ));
        when(locationMapper.selectPage(any(), any())).thenReturn(page);
        when(warehouseService.listByIds(any())).thenReturn(List.of(warehouse(10L, "一号仓")));

        IPage<InventoryLocationVO> result = service.getLocationPage(page, 10L, "A-", 1, 1);

        assertEquals(3, result.getRecords().size());
        assertEquals("一号仓", result.getRecords().get(0).getWarehouseName());
        assertEquals("货架", result.getRecords().get(0).getLocationTypeName());
        assertEquals("停用", result.getRecords().get(1).getStatusName());
        assertEquals("冷冻区", result.getRecords().get(2).getLocationTypeName());
        assertNull(result.getRecords().get(2).getWarehouseName(), "无仓库归属的库位不回填名称");
    }

    @Test
    @DisplayName("getLocationPage：空结果页不触发仓库批量查询")
    void getLocationPage_emptyPage_skipsWarehouseLookup() {
        Page<InventoryLocation> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        when(locationMapper.selectPage(any(), any())).thenReturn(page);

        IPage<InventoryLocationVO> result = service.getLocationPage(page, null, null, null, null);

        assertTrue(result.getRecords().isEmpty());
        verify(warehouseService, never()).listByIds(any());
    }

    @Test
    @DisplayName("使用率边界：maxCapacity 为 0 或 null 时使用率为 0（不除零）")
    void usageRate_zeroOrNullCapacity_isZero() {
        InventoryLocation zero = location(1L, 10L, "A-01", 1, 1);
        zero.setMaxCapacity(BigDecimal.ZERO);
        when(locationMapper.selectById(1L)).thenReturn(zero);
        assertEquals(0, BigDecimal.ZERO.compareTo(service.getLocationDetail(1L).getUsageRate()));

        InventoryLocation noCap = location(2L, 10L, "A-02", 1, 1);
        noCap.setMaxCapacity(null);
        when(locationMapper.selectById(2L)).thenReturn(noCap);
        assertEquals(0, BigDecimal.ZERO.compareTo(service.getLocationDetail(2L).getUsageRate()));
    }
}
