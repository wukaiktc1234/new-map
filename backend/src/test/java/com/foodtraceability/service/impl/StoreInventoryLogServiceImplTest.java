package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.InventoryMovement;
import com.foodtraceability.entity.Location;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.mapper.StoreInventoryLogMapper;
import com.foodtraceability.service.InventoryMovementService;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link StoreInventoryLogServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：写路径的四道严格校验（空记录 / 缺 product_id / 缺 store_id / store_id 非数字或解析不到 location）、
 * type→movement_type 映射（含未知值拒绝）、remark 合并与 500 字截断、sourceRef 回退、
 * 以及读路径的门店越权收敛（管理员保留入参、普通用户强制自身门店）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreInventoryLogServiceImplTest {

    @Mock
    private StoreInventoryLogMapper storeInventoryLogMapper;

    @Mock
    private LocationService locationService;

    @Mock
    private InventoryMovementService inventoryMovementService;

    @InjectMocks
    private StoreInventoryLogServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", storeInventoryLogMapper);
        when(inventoryMovementService.recordMovement(any(InventoryMovement.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static StoreInventoryLog log(Long productId, String storeId, Integer type, String remark) {
        StoreInventoryLog l = new StoreInventoryLog();
        l.setProductId(productId);
        l.setStoreId(storeId);
        l.setType(type);
        l.setChangeQuantity(new BigDecimal("-2"));
        l.setAfterStock(new BigDecimal("18"));
        l.setOperatorId(7L);
        l.setRemark(remark);
        return l;
    }

    private static Location location(Long locationId) {
        Location loc = new Location();
        loc.setLocationId(locationId);
        return loc;
    }

    // ==================== 写路径校验 ====================

    @Test
    @DisplayName("createLog：空记录 / 缺 product_id / 缺 store_id / store_id 空白 四类拒绝")
    void createLog_requiredFieldGuards() {
        assertParamError(() -> service.createLog(null), "日志记录为空");
        assertParamError(() -> service.createLog(log(null, "10", 1, null)), "product_id");
        assertParamError(() -> service.createLog(log(100L, null, 1, null)), "store_id");
        assertParamError(() -> service.createLog(log(100L, "   ", 1, null)), "store_id");

        verify(inventoryMovementService, never()).recordMovement(any());
    }

    @Test
    @DisplayName("createLog：store_id 非数字或解析不到 location 均拒绝（禁止默认兜底）")
    void createLog_storeIdResolutionGuards() {
        assertParamError(() -> service.createLog(log(100L, "STORE-X", 1, null)), "非数字");

        when(locationService.resolveByStoreId(576L)).thenReturn(null);
        assertParamError(() -> service.createLog(log(100L, "576", 1, null)), "无法解析");

        verify(inventoryMovementService, never()).recordMovement(any());
    }

    @Test
    @DisplayName("createLog：正常写入统一流水，location/material/数量/结存/操作人逐项映射")
    void createLog_mapsMovementFields() {
        when(locationService.resolveByStoreId(576L)).thenReturn(location(9001L));

        StoreInventoryLog input = log(100L, " 576 ", 1, "手工调整");
        assertSame(input, service.createLog(input));

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementService).recordMovement(captor.capture());
        InventoryMovement m = captor.getValue();
        assertEquals(9001L, m.getLocationId());
        assertEquals(100L, m.getMaterialId());
        assertEquals(0, new BigDecimal("-2").compareTo(m.getChangeQty()));
        assertEquals(0, new BigDecimal("18").compareTo(m.getBalanceAfter()));
        assertEquals(7L, m.getOperatorId());
        assertEquals("IN", m.getMovementType());
        assertEquals("OTHER", m.getSourceType(), "词表外来源统一取 OTHER，语义由 remark 承载");
        assertTrue(m.getRemark().contains("手工调整"));
        assertEquals("手工调整", m.getSourceRef(), "有用户备注时以备注作为 source_ref");
        assertNotNull(m.getCreateTime());
    }

    @Test
    @DisplayName("createLog：备注为空时 remark 只写来源标注，source_ref 回退 store-log-时间戳")
    void createLog_emptyRemark_usesOriginNoteAndFallbackRef() {
        when(locationService.resolveByStoreId(10L)).thenReturn(location(1L));

        service.createLog(log(100L, "10", 2, null));

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementService).recordMovement(captor.capture());
        assertTrue(captor.getValue().getRemark().contains("门店库存日志写路径"));
        assertTrue(captor.getValue().getSourceRef().startsWith("store-log-"));
    }

    @Test
    @DisplayName("createLog：合并后的 remark 超过 500 字被截断；createdAt 为空时用当前时间")
    void createLog_truncatesRemarkAndDefaultsTime() {
        when(locationService.resolveByStoreId(10L)).thenReturn(location(1L));
        String longRemark = "x".repeat(600);

        service.createLog(log(100L, "10", 1, longRemark));

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementService).recordMovement(captor.capture());
        assertEquals(500, captor.getValue().getRemark().length());
        assertNotNull(captor.getValue().getCreateTime());
    }

    @Test
    @DisplayName("createLog：createdAt 有值时沿用（不覆盖历史时间）")
    void createLog_keepsProvidedCreatedAt() {
        when(locationService.resolveByStoreId(10L)).thenReturn(location(1L));
        StoreInventoryLog input = log(100L, "10", 1, null);
        LocalDateTime created = LocalDateTime.of(2026, 9, 1, 8, 0);
        input.setCreatedAt(created);

        service.createLog(input);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementService).recordMovement(captor.capture());
        assertEquals(created, captor.getValue().getCreateTime());
    }

    @Test
    @DisplayName("type→movement_type：1=IN、2/5=OUT、3=TRANSFER_OUT、4=ADJUST；null/未知拒绝")
    void createLog_mapsTypeToMovementType() {
        when(locationService.resolveByStoreId(10L)).thenReturn(location(1L));

        assertEquals("IN", captureMovementType(1));
        assertEquals("OUT", captureMovementType(2));
        assertEquals("OUT", captureMovementType(5));
        assertEquals("TRANSFER_OUT", captureMovementType(3));
        assertEquals("ADJUST", captureMovementType(4));

        assertParamError(() -> service.createLog(log(100L, "10", null, null)), "type");
        assertParamError(() -> service.createLog(log(100L, "10", 99, null)), "未知");
    }

    // ==================== 读路径 ====================

    @Test
    @DisplayName("getLogPage：管理员保留入参 storeId（不收敛）")
    void getLogPage_adminKeepsRequestedStore() {
        Page<StoreInventoryLog> page = new Page<>(1, 10);
        when(storeInventoryLogMapper.selectLogPage(page, "999", null, 100L, 1)).thenReturn(page);

        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(true);
            mocked.when(SecurityUtils::hasStorePermission).thenReturn(true);
            mocked.when(SecurityUtils::getCurrentUserStoreId).thenReturn("576");

            IPage<StoreInventoryLog> result = service.getLogPage(page, "999", null, 100L, 1);

            assertSame(page, result);
            verify(storeInventoryLogMapper).selectLogPage(page, "999", null, 100L, 1);
        }
    }

    @Test
    @DisplayName("getLogPage：非管理员且有门店权限时强制收敛到本人门店")
    void getLogPage_nonAdminForcedToOwnStore() {
        Page<StoreInventoryLog> page = new Page<>(1, 10);
        when(storeInventoryLogMapper.selectLogPage(page, "576", null, null, null)).thenReturn(page);

        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(false);
            mocked.when(SecurityUtils::hasStorePermission).thenReturn(true);
            mocked.when(SecurityUtils::getCurrentUserStoreId).thenReturn("576");

            service.getLogPage(page, "999", null, null, null);

            verify(storeInventoryLogMapper).selectLogPage(page, "576", null, null, null);
        }
    }

    @Test
    @DisplayName("getLogPage：无门店权限时保留入参（由上游鉴权兜底）")
    void getLogPage_noStorePermissionKeepsRequestedStore() {
        Page<StoreInventoryLog> page = new Page<>(1, 10);
        when(storeInventoryLogMapper.selectLogPage(page, "999", null, 100L, null)).thenReturn(page);

        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::isAdmin).thenReturn(false);
            mocked.when(SecurityUtils::hasStorePermission).thenReturn(false);

            service.getLogPage(page, "999", null, 100L, null);

            verify(storeInventoryLogMapper).selectLogPage(page, "999", null, 100L, null);
        }
    }

    private String captureMovementType(int type) {
        when(locationService.resolveByStoreId(anyLong())).thenReturn(location(1L));
        service.createLog(log(100L, "10", type, null));
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementService, org.mockito.Mockito.atLeastOnce()).recordMovement(captor.capture());
        return captor.getValue().getMovementType();
    }

    private static void assertParamError(Runnable action, String expectedFragment) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains(expectedFragment), "实际消息: " + ex.getMessage());
    }
}
