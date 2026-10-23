package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.entity.InventoryLoss;
import com.foodtraceability.entity.InventoryLossDetail;
import com.foodtraceability.mapper.InventoryLossDetailMapper;
import com.foodtraceability.mapper.InventoryLossMapper;
import com.foodtraceability.service.InventoryService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventoryLossServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：报损单 CRUD 委托、审批状态字符串→整数映射（含大小写/数字串/未知值/空值）、
 * 报损处理时的库存扣减（跳过零量与缺少商品/仓库的明细、源类型 LOSS、
 * transactionType=3、referenceNo=报损单号）、以及处理后状态置 2。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryLossServiceImplTest {

    @Mock
    private InventoryLossMapper inventoryLossMapper;

    @Mock
    private InventoryLossDetailMapper inventoryLossDetailMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryLossServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, InventoryLoss.class);
        TableInfoHelper.initTableInfo(assistant, InventoryLossDetail.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", inventoryLossMapper);
        when(inventoryLossMapper.insert(any(InventoryLoss.class))).thenReturn(1);
        when(inventoryLossMapper.updateById(any(InventoryLoss.class))).thenReturn(1);
        when(inventoryLossMapper.deleteById(any(Long.class))).thenReturn(1);
        when(inventoryLossDetailMapper.selectList(any())).thenReturn(new ArrayList<>());
    }

    private static InventoryLoss loss(Long id, Long warehouseId, String lossCode, Integer status) {
        InventoryLoss l = new InventoryLoss();
        l.setLossId(id);
        l.setWarehouseId(warehouseId);
        l.setLossCode(lossCode);
        l.setLossStatus(status);
        return l;
    }

    private static InventoryLossDetail detail(Long productId, String qty, String reason) {
        InventoryLossDetail d = new InventoryLossDetail();
        d.setLossId(1L);
        d.setProductId(productId);
        d.setQuantity(qty == null ? null : new BigDecimal(qty));
        d.setReason(reason);
        return d;
    }

    // ==================== CRUD ====================

    @Test
    @DisplayName("createInventoryLoss：直接落库并返回原实体")
    void create_savesAndReturns() {
        InventoryLoss input = loss(null, 10L, "LS001", 0);

        assertSame(input, service.createInventoryLoss(input));
        verify(inventoryLossMapper).insert(input);
    }

    @Test
    @DisplayName("updateInventoryLoss：路径 ID 覆盖主键，更新后回查")
    void update_overridesIdAndReloads() {
        InventoryLoss payload = loss(null, 10L, "LS001", 0);
        InventoryLoss reloaded = loss(5L, 10L, "LS001", 0);
        when(inventoryLossMapper.selectById(5L)).thenReturn(reloaded);

        assertSame(reloaded, service.updateInventoryLoss(5L, payload));
        assertEquals(5L, payload.getLossId());
        verify(inventoryLossMapper).updateById(payload);
    }

    @Test
    @DisplayName("getInventoryLossById / deleteInventoryLoss 委托父类方法")
    void getAndDelete_delegate() {
        InventoryLoss stored = loss(1L, 10L, "LS001", 0);
        when(inventoryLossMapper.selectById(1L)).thenReturn(stored);

        assertSame(stored, service.getInventoryLossById(1L));

        service.deleteInventoryLoss(1L);
        verify(inventoryLossMapper).deleteById(1L);
    }

    @Test
    @DisplayName("getInventoryLossPage：全部筛选条件原样透传给自定义分页查询")
    void getPage_passesFiltersThrough() {
        Page<InventoryLoss> page = new Page<>(1, 10);
        when(inventoryLossMapper.selectInventoryLossPage(page, "LS", 10L, "0", "2026-10-01", "2026-10-31"))
                .thenReturn(page);

        IPage<InventoryLoss> result = service.getInventoryLossPage(page, "LS", 10L, "0", "2026-10-01", "2026-10-31");

        assertSame(page, result);
        verify(inventoryLossMapper).selectInventoryLossPage(page, "LS", 10L, "0", "2026-10-01", "2026-10-31");
    }

    // ==================== 审批 ====================

    @Test
    @DisplayName("approveInventoryLoss：记录不存在返回 null，不写库")
    void approve_missing_returnsNull() {
        when(inventoryLossMapper.selectById(404L)).thenReturn(null);

        assertNull(service.approveInventoryLoss(404L, "approved", "ok"));
        verify(inventoryLossMapper, never()).updateById(any(InventoryLoss.class));
    }

    @Test
    @DisplayName("approveInventoryLoss：approved/processed/rejected/数字串/未知/null 六种映射")
    void approve_statusMapping() {
        when(inventoryLossMapper.selectById(anyLong())).thenAnswer(inv ->
                loss((Long) inv.getArgument(0), 10L, "LS", 0));

        assertEquals(1, service.approveInventoryLoss(1L, "approved", null).getLossStatus());
        assertEquals(1, service.approveInventoryLoss(2L, "APPROVED", null).getLossStatus(), "大小写不敏感");
        assertEquals(1, service.approveInventoryLoss(3L, "1", null).getLossStatus(), "数字串 1 视为已审核");
        assertEquals(2, service.approveInventoryLoss(4L, "processed", null).getLossStatus());
        assertEquals(2, service.approveInventoryLoss(5L, "2", null).getLossStatus());
        assertEquals(-1, service.approveInventoryLoss(6L, "rejected", null).getLossStatus());
        assertEquals(-1, service.approveInventoryLoss(7L, "-1", null).getLossStatus());
        assertEquals(0, service.approveInventoryLoss(8L, "whatever", null).getLossStatus(), "未知状态回落待审核");
        assertEquals(0, service.approveInventoryLoss(9L, null, null).getLossStatus(), "null 回落待审核");
        verify(inventoryLossMapper, times(9)).updateById(any(InventoryLoss.class));
    }

    @Test
    @DisplayName("approveInventoryLoss：审批痕迹落库（approveTime 必写），原报损原因不被备注覆盖（P1-3）")
    void approve_writesApprovalTrace() {
        InventoryLoss stored = loss(1L, 10L, "LS", 0);
        stored.setReason("原始原因");
        when(inventoryLossMapper.selectById(1L)).thenReturn(stored);

        service.approveInventoryLoss(1L, "approved", "审批意见");

        assertEquals("原始原因", stored.getReason(), "审批备注不得覆盖原始报损原因");
        assertNotNull(stored.getApproveTime(), "P1-3 修复后必须写入审批时间");
        // 单元测试无安全上下文，approveUserId 允许为 null（不写入脏数据）
        assertNull(stored.getApproveUserId(), "无登录上下文时不写审批人");
        verify(inventoryLossMapper).updateById(stored);
    }

    // ==================== 处理（扣减库存） ====================

    @Test
    @DisplayName("processInventoryLoss：单据不存在抛异常")
    void process_missing_throws() {
        when(inventoryLossMapper.selectById(404L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.processInventoryLoss(404L));
        assertTrue(ex.getMessage().contains("报损单不存在"));
        verify(inventoryService, never()).decreaseInventory(any());
    }

    @Test
    @DisplayName("processInventoryLoss：逐条扣减，源类型 LOSS、类型 3、单号引用报损单号")
    void process_decreasesStockWithLossSourceType() {
        when(inventoryLossMapper.selectById(1L)).thenReturn(loss(1L, 10L, "LS20261001001", 1));
        when(inventoryLossDetailMapper.selectList(any())).thenReturn(List.of(
                detail(100L, "2.5", "过期"),
                detail(101L, "1", "破损")
        ));

        InventoryLoss result = service.processInventoryLoss(1L);

        ArgumentCaptor<InventoryDecreaseDTO> captor = ArgumentCaptor.forClass(InventoryDecreaseDTO.class);
        verify(inventoryService, times(2)).decreaseInventory(captor.capture());
        List<InventoryDecreaseDTO> dtos = captor.getAllValues();

        InventoryDecreaseDTO first = dtos.get(0);
        assertEquals(100L, first.getMaterialId());
        assertEquals(10L, first.getWarehouseId());
        assertEquals(0, new BigDecimal("2.5").compareTo(first.getQuantity()));
        assertEquals(3, first.getTransactionType());
        assertEquals("LS20261001001", first.getReferenceNo());
        assertEquals("inventory_loss", first.getReferenceType());
        assertEquals("过期", first.getRemark());
        assertEquals("LOSS", first.getSourceType());

        assertEquals(101L, dtos.get(1).getMaterialId());
        assertEquals(2, result.getLossStatus(), "处理后状态置 2");
        assertNotNull(result.getUpdateTime());
        verify(inventoryLossMapper).updateById(result);
    }

    @Test
    @DisplayName("processInventoryLoss：零/负数量、缺商品ID、缺仓库的明细一律跳过")
    void process_skipsInvalidDetails() {
        when(inventoryLossMapper.selectById(1L)).thenReturn(loss(1L, 10L, "LS1", 1));
        when(inventoryLossDetailMapper.selectList(any())).thenReturn(List.of(
                detail(100L, "0", "零"),
                detail(101L, "-1", "负"),
                detail(102L, null, "空数量"),
                detail(null, "1", "缺商品"),
                detail(103L, "1", "有效")
        ));

        service.processInventoryLoss(1L);

        ArgumentCaptor<InventoryDecreaseDTO> captor = ArgumentCaptor.forClass(InventoryDecreaseDTO.class);
        verify(inventoryService, times(1)).decreaseInventory(captor.capture());
        assertEquals(103L, captor.getValue().getMaterialId());
    }

    @Test
    @DisplayName("processInventoryLoss：报损单无仓库ID时全部跳过扣减，但状态仍置为已处理（特征化）")
    void process_nullWarehouse_skipsAllButStillMarksProcessed() {
        when(inventoryLossMapper.selectById(1L)).thenReturn(loss(1L, null, "LS1", 1));
        when(inventoryLossDetailMapper.selectList(any())).thenReturn(List.of(detail(100L, "1", "过期")));

        InventoryLoss result = service.processInventoryLoss(1L);

        verify(inventoryService, never()).decreaseInventory(any());
        assertEquals(2, result.getLossStatus());
    }

    @Test
    @DisplayName("processInventoryLoss：明细为空时直接置已处理")
    void process_noDetails_marksProcessed() {
        when(inventoryLossMapper.selectById(1L)).thenReturn(loss(1L, 10L, "LS1", 0));

        InventoryLoss result = service.processInventoryLoss(1L);

        verify(inventoryService, never()).decreaseInventory(any());
        assertEquals(2, result.getLossStatus());
    }
}
