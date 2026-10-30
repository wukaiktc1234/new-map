package com.foodtraceability.service.impl;

import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.TtlOrderCloseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * T3-A：TTL 关单的 **CAS 0 行分支**（确定性单测）。
 *
 * <p>为什么要单测：集成层面「已支付单」在 {@code findExpiredOrderIds} 的选取阶段就被排除了，
 * CAS 0 行分支只存在于「选取 → CAS」之间的窄竞态窗口，静态夹具构造不出来（实测 T3 夹具跑不出该日志）。
 * 但分支本身必须被覆盖：**0 行 = 必须放弃，且不得做任何联动（不回补库存、不写流水）**。</p>
 */
@ExtendWith(MockitoExtension.class)
class TtlOrderCloseTxBeanTest {

    @Mock
    private TtlOrderCloseMapper closeMapper;

    @Mock
    private FoodNewMapper foodNewMapper;

    private TtlOrderCloseTxBean txBean;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // RT-4：库存回补逻辑已抽到 PosOrderStockRestorer（TTL 与异常单作废共用同一份实现），
        // 这里用真实组件 + mock 的 mapper 组装，断言点保持不变。
        txBean = new TtlOrderCloseTxBean(closeMapper, new PosOrderStockRestorer(closeMapper, foodNewMapper));
    }

    @Test
    @DisplayName("CAS 0 行（顾客刚好已支付）→ closeOne 返回 false，且不做任何库存联动")
    void casMissDoesNothing() {
        // casCancelExpired 返回 0（状态/支付条件不再满足）
        when(closeMapper.casCancelExpired(anyString(), anyString())).thenReturn(0);

        boolean closed = txBean.closeOne("O-CAS-MISS-1");

        assertFalse(closed, "CAS 0 行必须返回 false（放弃，不覆盖）");
        // 关键断言：放弃时**不得**去查明细、不得回补库存
        verify(closeMapper, never()).findItems(anyString());
        verify(foodNewMapper, never()).addStock(anyString(), anyInt());
        // 也不应留下任何写操作
        verify(closeMapper, times(1)).casCancelExpired(anyString(), anyString());
    }

    @Test
    @DisplayName("CAS 1 行 → closeOne 返回 true，并按 order_items 回补 foods.stock")
    void casHitReleasesStock() {
        when(closeMapper.casCancelExpired(anyString(), anyString())).thenReturn(1);
        java.util.Map<String, Object> item = new java.util.HashMap<>();
        item.put("food_id", 2L);
        item.put("quantity", 2);
        when(closeMapper.findItems(anyString())).thenReturn(java.util.List.of(item));
        com.foodtraceability.entity.FoodNew food = new com.foodtraceability.entity.FoodNew();
        food.setFoodId(2L);
        food.setFoodCode("FD260925001");
        when(foodNewMapper.selectById(2L)).thenReturn(food);

        boolean closed = txBean.closeOne("O-CAS-HIT-1");

        org.junit.jupiter.api.Assertions.assertTrue(closed);
        verify(foodNewMapper, times(1)).addStock("FD260925001", 2);
    }
}
