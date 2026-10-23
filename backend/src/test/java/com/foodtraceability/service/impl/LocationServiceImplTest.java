package com.foodtraceability.service.impl;

import com.foodtraceability.entity.Location;
import com.foodtraceability.mapper.LocationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * LocationServiceImpl 单元测试（库存位置 M1-M2 核心层，只读）。
 *
 * <p>覆盖：null 入参短路、逻辑删除过滤、按门店/仓库/位置反查的委托与参数、活跃列表透传。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocationServiceImplTest {

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationServiceImpl service;

    private static Location location(long id, Integer deleted) {
        Location l = new Location();
        l.setLocationId(id);
        l.setLocationName("库位" + id);
        l.setLocationType("WAREHOUSE");
        l.setDeleted(deleted);
        return l;
    }

    // ==================== getById ====================

    @Test
    @DisplayName("getById：null 入参直接返回 null，不查库")
    void getById_null_shortCircuits() {
        assertNull(service.getById(null));
        verify(locationMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("getById：存在且未删除返回实体")
    void getById_found_returnsEntity() {
        when(locationMapper.selectById(7L)).thenReturn(location(7L, 0));

        Location result = service.getById(7L);

        assertNotNull(result);
        assertEquals(7L, result.getLocationId());
    }

    @Test
    @DisplayName("getById：已逻辑删除返回 null（不再对外可见）")
    void getById_deleted_returnsNull() {
        when(locationMapper.selectById(8L)).thenReturn(location(8L, 1));

        assertNull(service.getById(8L), "deleted=1 的位置必须对外不可见");
    }

    @Test
    @DisplayName("getById：查不到返回 null")
    void getById_missing_returnsNull() {
        when(locationMapper.selectById(9L)).thenReturn(null);
        assertNull(service.getById(9L));
    }

    // ==================== resolveByStoreId ====================

    @Test
    @DisplayName("resolveByStoreId：null 短路；非 null 委托 selectByStoreId")
    void resolveByStoreId_delegates() {
        assertNull(service.resolveByStoreId(null));
        verify(locationMapper, never()).selectByStoreId(anyLong());

        when(locationMapper.selectByStoreId(3L)).thenReturn(location(30L, 0));
        assertEquals(30L, service.resolveByStoreId(3L).getLocationId());
    }

    // ==================== resolveByWarehouseId ====================

    @Test
    @DisplayName("resolveByWarehouseId：委托 selectBySource('warehouses', id)")
    void resolveByWarehouseId_delegates() {
        assertNull(service.resolveByWarehouseId(null));
        verify(locationMapper, never()).selectBySource(any(), any());

        when(locationMapper.selectBySource("warehouses", 5L)).thenReturn(location(50L, 0));
        assertEquals(50L, service.resolveByWarehouseId(5L).getLocationId());
        verify(locationMapper).selectBySource("warehouses", 5L);
    }

    // ==================== resolveWarehouseIdByLocationId ====================

    @Test
    @DisplayName("resolveWarehouseIdByLocationId：null 短路；未映射返回 null（禁止数值兜底）")
    void resolveWarehouseIdByLocationId_nullAndUnmapped() {
        assertNull(service.resolveWarehouseIdByLocationId(null));
        verify(locationMapper, never()).selectWarehouseIdByLocation(anyLong());

        when(locationMapper.selectWarehouseIdByLocation(99L)).thenReturn(null);
        assertNull(service.resolveWarehouseIdByLocationId(99L), "未映射时不得回退成 0 或其它数值");
    }

    @Test
    @DisplayName("resolveWarehouseIdByLocationId：已映射返回来源仓库 ID")
    void resolveWarehouseIdByLocationId_mapped() {
        when(locationMapper.selectWarehouseIdByLocation(12L)).thenReturn(5L);
        assertEquals(5L, service.resolveWarehouseIdByLocationId(12L));
    }

    // ==================== listActive ====================

    @Test
    @DisplayName("listActive：透传类型过滤，空结果返回空列表")
    void listActive_passesTypeThrough() {
        when(locationMapper.selectActive("STORE")).thenReturn(List.of());
        assertTrue(service.listActive("STORE").isEmpty());
        verify(locationMapper).selectActive("STORE");
    }

    @Test
    @DisplayName("listActive：null 类型表示不过滤")
    void listActive_nullType_meansAll() {
        when(locationMapper.selectActive(null)).thenReturn(List.of(location(1L, 0), location(2L, 0)));
        assertEquals(2, service.listActive(null).size());
    }
}
