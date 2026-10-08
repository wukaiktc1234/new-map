package com.foodtraceability.common.util;

import com.foodtraceability.mapper.LocationIdMapMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LocationIdBridge 单元测试 —— P1-USER-LOCATION-002 C1（缺口①：正向 warehouse→location 入口）。
 *
 * <p>验收点（设计文档 p1-user-location-002-design-001.md §2 D1-1 / §6 A1）：
 * <ol>
 *   <li>新增泛化入口 {@code toLocationId(srcTable, srcId)} 对 {@code warehouses} 生效；</li>
 *   <li>旧签名 {@code toLocationId(Long storeId)} 行为不变（委托，零调用点冲击）；</li>
 *   <li>入参为空 → 返回 null 且<b>不触库</b>；</li>
 *   <li>无映射 → 返回 null（禁止数值兜底，I-3）；</li>
 *   <li>静态入口 {@code locationIdOfWarehouse} 在非 Spring 上下文（未注册实例）返回 null。</li>
 * </ol>
 *
 * <p>注意：{@code LocationIdBridge.instance} 是静态字段，构造即自注册；本测试用
 * {@link ReflectionTestUtils} 显式重置，避免静态状态泄漏到其它测试类。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LocationIdBridge 单元测试 - 正向 location 入口（缺口①）")
class LocationIdBridgeTest {

    @Mock
    private LocationIdMapMapper locationIdMapMapper;

    private LocationIdBridge bridge;

    @BeforeEach
    void setUp() {
        // 构造即自注册静态实例
        bridge = new LocationIdBridge(locationIdMapMapper);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(LocationIdBridge.class, "instance", null);
    }

    @Test
    @DisplayName("泛化入口：warehouses.warehouse_id → location_id（此前无正向入口）")
    void toLocationIdByWarehouseResolvesViaMap() {
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_WAREHOUSES, 77L))
                .thenReturn(500L);

        Long locationId = bridge.toLocationId(LocationIdBridge.SRC_WAREHOUSES, 77L);

        assertEquals(Long.valueOf(500L), locationId, "仓库来源应经 location_id_map 换算为 location_id");

        ArgumentCaptor<String> tableCaptor = ArgumentCaptor.forClass(String.class);
        verify(locationIdMapMapper).selectLocationIdBySrcId(tableCaptor.capture(), any());
        assertEquals(LocationIdBridge.SRC_WAREHOUSES, tableCaptor.getValue(),
                "应查 warehouses 来源行，不得误查 stores_new");
    }

    @Test
    @DisplayName("旧签名 toLocationId(Long) 行为不变：委托到 stores_new")
    void legacyStoreOverloadDelegatesToStoresNew() {
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, 9L))
                .thenReturn(900L);

        Long locationId = bridge.toLocationId(9L);

        assertEquals(Long.valueOf(900L), locationId);

        ArgumentCaptor<String> tableCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> srcIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(locationIdMapMapper).selectLocationIdBySrcId(tableCaptor.capture(), srcIdCaptor.capture());
        assertEquals(LocationIdBridge.SRC_STORES_NEW, tableCaptor.getValue());
        assertEquals(Long.valueOf(9L), srcIdCaptor.getValue());
    }

    @Test
    @DisplayName("srcId 为 null → 返回 null 且不触库")
    void nullSrcIdReturnsNullWithoutQuery() {
        assertNull(bridge.toLocationId(LocationIdBridge.SRC_WAREHOUSES, null));
        assertNull(bridge.toLocationId((Long) null));

        verify(locationIdMapMapper, never()).selectLocationIdBySrcId(anyString(), any());
    }

    @Test
    @DisplayName("srcTable 为空/空白 → 返回 null 且不触库")
    void blankSrcTableReturnsNullWithoutQuery() {
        assertNull(bridge.toLocationId(null, 5L));
        assertNull(bridge.toLocationId("", 5L));

        verify(locationIdMapMapper, never()).selectLocationIdBySrcId(anyString(), any());
    }

    @Test
    @DisplayName("无映射行 → 返回 null（禁止数值兜底，I-3）")
    void unmappedSourceReturnsNullNotFallbackValue() {
        // 8 个被「全清」决策排除的 E2E/TEST 仓库即此情形
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_WAREHOUSES, 888L))
                .thenReturn(null);

        assertNull(bridge.toLocationId(LocationIdBridge.SRC_WAREHOUSES, 888L),
                "未纳入位置体系的来源必须显式返回 null，不得回落到任何默认 ID");
    }

    @Test
    @DisplayName("静态入口 locationIdOfWarehouse：已注册实例时可用，未注册时返回 null")
    void staticWarehouseEntryHonoursRegistrationState() {
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_WAREHOUSES, 77L))
                .thenReturn(500L);

        assertEquals(Long.valueOf(500L), LocationIdBridge.locationIdOfWarehouse(77L),
                "已注册实例（Spring 上下文）应换算成功");

        // 模拟非 Spring 上下文：清空静态实例
        ReflectionTestUtils.setField(LocationIdBridge.class, "instance", null);

        assertNull(LocationIdBridge.locationIdOfWarehouse(77L),
                "非 Spring 上下文应返回 null，由调用方按显式拒绝语义处理");
        assertNull(LocationIdBridge.locationIdOfWarehouse(null));
    }
}
