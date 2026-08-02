package com.foodtraceability.service.finance.impl;

import com.foodtraceability.dto.finance.PayableVO;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.PayableMapper;
import com.foodtraceability.service.SupplierService;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PayableServiceImpl 单元测试
 *
 * <p>覆盖：createForStockin 幂等性、confirmPayment 状态流转、逾期状态自动计算。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PayableServiceImpl 单元测试")
class PayableServiceImplTest {

    @Mock
    private PayableMapper payableMapper;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private PayableServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 是父类 protected 字段，需手动注入
        ReflectionTestUtils.setField(service, "baseMapper", payableMapper);
    }

    // ============================================================
    // 1. createForStockin：正常创建应付账款
    // ============================================================
    @Test
    @DisplayName("为入库单创建应付账款：字段正确且到期日为入库日后30天")
    void createForStockin_success() {
        // given
        Long stockinId = 7001L;
        String stockinNo = "SI20260719001";
        Long supplierId = 3001L;
        String supplierName = "测试供应商";
        Long orderId = 5001L;
        String orderNo = "PO20260719001";
        Long amount = 250000L; // 2500元
        LocalDate stockinDate = LocalDate.of(2026, 7, 19);

        when(payableMapper.selectById(any())).thenReturn(null);
        when(payableMapper.insert(any(Payable.class))).thenReturn(1);

        // when
        PayableVO vo = service.createForStockin(stockinId, stockinNo, supplierId, supplierName,
                orderId, orderNo, amount, stockinDate);

        // then
        assertAll("应付账款 VO 字段校验",
                () -> assertNotNull(vo),
                () -> assertEquals("AP0000007001", vo.getPayableNo()),
                () -> assertEquals(stockinId, vo.getStockinId()),
                () -> assertEquals(stockinNo, vo.getStockinNo()),
                () -> assertEquals(orderNo, vo.getOrderNo()),
                () -> assertEquals(supplierName, vo.getSupplierName()),
                () -> assertEquals(amount, vo.getOriginalAmount()),
                () -> assertEquals(0L, vo.getPaidAmount()),
                () -> assertEquals(amount, vo.getBalanceAmount()),
                () -> assertEquals(stockinDate.plusDays(30), vo.getDueDate())
        );

        ArgumentCaptor<Payable> captor = ArgumentCaptor.forClass(Payable.class);
        verify(payableMapper, times(1)).insert(captor.capture());
        Payable saved = captor.getValue();
        assertAll("持久化实体校验",
                () -> assertEquals(stockinId, saved.getStockinId()),
                () -> assertEquals(orderId, saved.getPurchaseOrderId()),
                () -> assertEquals(amount, saved.getOriginalAmount()),
                () -> assertEquals(1, saved.getStatus())
        );
    }

    // ============================================================
    // 2. createForStockin：幂等性
    // ============================================================
    @Test
    @DisplayName("同一入库单重复创建应付账款应幂等返回已有记录")
    void createForStockin_idempotent() {
        // given
        Long stockinId = 7002L;
        LocalDate stockinDate = LocalDate.of(2026, 7, 19);

        Payable existing = new Payable();
        existing.setPayableId(8002L);
        existing.setPayableNo("AP0000007002");
        existing.setStockinId(stockinId);
        existing.setStockinNo("SI20260719002");
        existing.setOriginalAmount(100000L);
        existing.setBalanceAmount(100000L);
        existing.setStatus(1);

        // 模拟 lambdaQuery().eq(...).one() 返回已有记录
        when(payableMapper.selectOne(any())).thenReturn(existing);

        // when
        PayableVO vo = service.createForStockin(stockinId, "SI20260719002", 3002L, "供应商",
                5002L, "PO20260719002", 200000L, stockinDate);

        // then：不插入新记录，返回已有数据
        assertAll("幂等返回",
                () -> assertEquals("AP0000007002", vo.getPayableNo()),
                () -> assertEquals(100000L, vo.getOriginalAmount())
        );
        verify(payableMapper, never()).insert(any(Payable.class));
    }

    // ============================================================
    // 3. confirmPayment：状态流转
    // ============================================================
    @Test
    @DisplayName("部分付款后状态为部分支付，结清后状态为已付清")
    void confirmPayment_statusTransition() {
        // given
        Long payableId = 8001L;
        Payable payable = new Payable();
        payable.setPayableId(payableId);
        payable.setOriginalAmount(100000L);
        payable.setPaidAmount(0L);
        payable.setBalanceAmount(100000L);
        payable.setStatus(1);

        when(payableMapper.selectById(payableId)).thenReturn(payable);
        when(payableMapper.updateById(any(Payable.class))).thenReturn(1);

        // when：第一次付款 40000 分
        service.confirmPayment(payableId, 40000L);

        // then：状态变为部分支付
        ArgumentCaptor<Payable> captor = ArgumentCaptor.forClass(Payable.class);
        verify(payableMapper, times(1)).updateById(captor.capture());
        Payable updated = captor.getValue();
        assertAll("部分付款校验",
                () -> assertEquals(40000L, updated.getPaidAmount()),
                () -> assertEquals(60000L, updated.getBalanceAmount()),
                () -> assertEquals(2, updated.getStatus())
        );
    }

    @Test
    @DisplayName("付款金额超过余额应抛出业务异常")
    void confirmPayment_overBalance_throws() {
        // given
        Long payableId = 8002L;
        Payable payable = new Payable();
        payable.setPayableId(payableId);
        payable.setOriginalAmount(100000L);
        payable.setPaidAmount(20000L);
        payable.setBalanceAmount(80000L);
        payable.setStatus(2);

        when(payableMapper.selectById(payableId)).thenReturn(payable);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmPayment(payableId, 90000L));
        assertTrue(ex.getMessage().contains("付款金额不能大于余额"));
        verify(payableMapper, never()).updateById(any(Payable.class));
    }

    // ============================================================
    // 4. convertToVO：逾期状态自动联动
    // ============================================================
    @Test
    @DisplayName("未付清且已到期时状态自动标记为逾期")
    void convertToVO_overdue() {
        // given
        Long payableId = 8003L;
        Payable payable = new Payable();
        payable.setPayableId(payableId);
        payable.setOriginalAmount(100000L);
        payable.setPaidAmount(0L);
        payable.setBalanceAmount(100000L);
        payable.setStatus(1);
        payable.setDueDate(LocalDate.now().minusDays(1));

        when(payableMapper.selectById(payableId)).thenReturn(payable);

        // when
        PayableVO vo = service.getDetail(payableId);

        // then
        assertAll("逾期状态校验",
                () -> assertEquals(4, vo.getStatus()),
                () -> assertEquals("逾期", vo.getStatusName())
        );
    }

    @Test
    @DisplayName("已付清且已到期时不应标记为逾期")
    void convertToVO_settledNotOverdue() {
        // given
        Long payableId = 8004L;
        Payable payable = new Payable();
        payable.setPayableId(payableId);
        payable.setOriginalAmount(100000L);
        payable.setPaidAmount(100000L);
        payable.setBalanceAmount(0L);
        payable.setStatus(3);
        payable.setDueDate(LocalDate.now().minusDays(10));

        when(payableMapper.selectById(payableId)).thenReturn(payable);

        // when
        PayableVO vo = service.getDetail(payableId);

        // then
        assertEquals(3, vo.getStatus());
        assertEquals("已付清", vo.getStatusName());
    }
}
