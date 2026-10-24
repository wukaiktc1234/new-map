package com.foodtraceability.service.purchase;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.entity.Location;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.LocationIdMapMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.PurchaseStockinMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 整改 1：入库库位解析器 —— 只允许 ID → ID（location_id_map），五级优先序，绝不返回 null。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockinLocationResolverTest {

    private static final long EXPLICIT = 900L;
    private static final long FROM_WAREHOUSE = 901L;
    private static final long FROM_ORDER_STORE = 902L;
    private static final long FROM_TRACE_STORE = 903L;
    private static final long FROM_USER = 904L;

    @Mock private LocationService locationService;
    @Mock private LocationIdMapMapper locationIdMapMapper;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private PurchaseStockinMapper purchaseStockinMapper;
    @Mock private UserMapper userMapper;

    private StockinLocationResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new StockinLocationResolver(locationService, new LocationIdBridge(locationIdMapMapper),
                purchaseOrderMapper, purchaseStockinMapper, userMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Location location(Long id) {
        Location l = new Location();
        l.setLocationId(id);
        l.setDeleted(0);
        return l;
    }

    @Test
    @DisplayName("① 显式 locationId 优先，且校验存在")
    void explicitLocationWins() {
        when(locationService.getById(EXPLICIT)).thenReturn(location(EXPLICIT));

        assertEquals(EXPLICIT, resolver.resolveLocationId(EXPLICIT, null, 1L, "1"));
    }

    @Test
    @DisplayName("① 显式 locationId 不存在 → 抛 PARAM_ERROR")
    void explicitLocationMustExist() {
        when(locationService.getById(EXPLICIT)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> resolver.resolveLocationId(EXPLICIT, null, null, null));
        assertTrue(ex.getMessage().contains("指定的入库库位不存在"), "实际=" + ex.getMessage());
    }

    @Test
    @DisplayName("② 采购单 warehouse_id → location_id_map('warehouses')")
    void resolvesByPurchaseOrderWarehouse() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(55L);
        order.setWarehouseId(7L);
        when(purchaseOrderMapper.selectById(55L)).thenReturn(order);
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_WAREHOUSES, 7L))
                .thenReturn(FROM_WAREHOUSE);

        assertEquals(FROM_WAREHOUSE, resolver.resolveLocationId(null, null, 55L, "1"));
    }

    @Test
    @DisplayName("③ 采购单 store_id → location_id_map('stores_new')")
    void resolvesByPurchaseOrderStore() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(56L);
        order.setStoreId(3L);
        when(purchaseOrderMapper.selectById(56L)).thenReturn(order);
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, 3L))
                .thenReturn(FROM_ORDER_STORE);

        assertEquals(FROM_ORDER_STORE, resolver.resolveLocationId(null, null, 56L, "9"));
    }

    @Test
    @DisplayName("④ 追溯码 store_id → location_id_map('stores_new')")
    void resolvesByTraceStore() {
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, 2L))
                .thenReturn(FROM_TRACE_STORE);

        assertEquals(FROM_TRACE_STORE, resolver.resolveLocationId(null, null, null, "2"));
    }

    @Test
    @DisplayName("⑤ 当前登录用户 users.location_id 兜底")
    void resolvesByCurrentUserLocation() {
        User user = new User();
        user.setId(1000L);
        user.setUsername("qa_admin");
        user.setLocationId(FROM_USER);
        when(userMapper.selectById(1000L)).thenReturn(user);
        when(locationService.getById(FROM_USER)).thenReturn(location(FROM_USER));

        User principal = new User();
        principal.setId(1000L);
        principal.setUsername("qa_admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));

        assertEquals(FROM_USER, resolver.resolveLocationId(null, null, null, null));
    }

    @Test
    @DisplayName("⑤ 无 SecurityContext → 不兜底，继续失败")
    void userFallbackRequiresPrincipal() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resolver.resolveLocationId(null, null, null, null));
        assertTrue(ex.getMessage().contains("无法确定入库库位"), "实际=" + ex.getMessage());
    }

    @Test
    @DisplayName("五级全失败 → 抛 PARAM_ERROR，绝不返回 null")
    void throwsWhenNothingResolves() {
        when(locationIdMapMapper.selectLocationIdBySrcId(any(), anyLong())).thenReturn(null);
        when(purchaseOrderMapper.selectById(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> resolver.resolveLocationId(null, null, 1L, "1"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("请在请求中显式指定 locationId"), "实际=" + ex.getMessage());
    }

    @Test
    @DisplayName("stockinId 反查采购单：②→③ 链路可用")
    void resolvesViaStockinOrder() {
        PurchaseStockin stockin = new PurchaseStockin();
        stockin.setStockinId(700L);
        stockin.setOrderId(57L);
        when(purchaseStockinMapper.selectById(700L)).thenReturn(stockin);

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(57L);
        order.setWarehouseId(8L);
        when(purchaseOrderMapper.selectById(57L)).thenReturn(order);
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_WAREHOUSES, 8L))
                .thenReturn(FROM_WAREHOUSE);

        assertEquals(FROM_WAREHOUSE, resolver.resolveLocationId(null, 700L));
    }
}
