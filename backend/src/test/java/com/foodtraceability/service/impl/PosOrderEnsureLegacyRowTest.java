package com.foodtraceability.service.impl;

import com.foodtraceability.entity.Food;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.ComboIngredientNewMapper;
import com.foodtraceability.mapper.DishComboMapper;
import com.foodtraceability.mapper.DishComboNewMapper;
import com.foodtraceability.mapper.FoodMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.mapper.OrderItemMapper;
import com.foodtraceability.mapper.OrderItemNewMapper;
import com.foodtraceability.mapper.OrderMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.StoreNewMapper;
import com.foodtraceability.controller.websocket.OrderWebSocketController;
import com.foodtraceability.service.PosOrderNumberGenerator;
import com.foodtraceability.service.SensitiveDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * P1-NEW-FOOD-LEGACY-SYNC-001 补测：ensureLegacyFoodRow 自愈语义（方案 B）
 * 私有方法经反射调用；未用到的构造依赖传 null
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosOrderEnsureLegacyRowTest {

    @Mock private FoodMapper foodMapper;
    @Mock private FoodNewMapper foodNewMapper;

    private PosOrderCreateServiceImpl service;
    private Method ensure;

    @BeforeEach
    void setUp() throws Exception {
        service = new PosOrderCreateServiceImpl(
                null, mock(OrderMapper.class), null, foodMapper, foodNewMapper,
                mock(DishComboMapper.class), mock(DishComboNewMapper.class), null,
                mock(OrderWebSocketController.class), mock(ApplicationEventPublisher.class),
                mock(SensitiveDataService.class), mock(PosOrderNumberGenerator.class),
                mock(StoreNewMapper.class), mock(OrderNewMapper.class), mock(OrderItemNewMapper.class));
        ensure = PosOrderCreateServiceImpl.class.getDeclaredMethod("ensureLegacyFoodRow", String.class);
        ensure.setAccessible(true);
    }

    private FoodNew foodNew(String code, int status, int stock) {
        FoodNew f = new FoodNew();
        f.setFoodCode(code);
        f.setFoodName("QA-" + code);
        f.setSalePrice(2500L);
        f.setCostPrice(400L);
        f.setStatus(status);
        f.setStock(stock);
        return f;
    }

    private void invoke(String foodCode) throws Exception {
        ensure.invoke(service, foodCode);
    }

    /** 自愈：legacy 行缺失 → 按 foods 补行（status=1 → active；价格分→元） */
    @Test
    void ensure_missingRow_insertsFromFoods() throws Exception {
        String code = "FD_HEAL_001";
        org.mockito.Mockito.lenient().when(foodMapper.selectByFoodCodeForUpdate(code)).thenReturn(null);
        org.mockito.Mockito.lenient().when(foodNewMapper.selectByFoodCode(code)).thenReturn(foodNew(code, 1, 30));

        invoke(code);

        org.mockito.ArgumentCaptor<Food> captor = org.mockito.ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        assertEquals(code, captor.getValue().getFoodCode());
        assertEquals("active", captor.getValue().getFoodStatus());
        assertEquals(new java.math.BigDecimal("25.00"), captor.getValue().getFoodPrice());
        assertEquals(30, captor.getValue().getStock());
    }

    /** 自愈幂等：legacy 行已存在 → 直接 return，不 insert */
    @Test
    void ensure_existingRow_noInsert() throws Exception {
        String code = "FD_HEAL_002";
        org.mockito.Mockito.lenient().when(foodMapper.selectByFoodCodeForUpdate(code))
                .thenReturn(new Food());
        org.mockito.Mockito.lenient().when(foodNewMapper.selectByFoodCode(code)).thenReturn(foodNew(code, 1, 30));

        invoke(code);

        verify(foodMapper, never()).insert(any(Food.class));
    }

    /** 边界：foods 也无该行 → 静默 return（不抛异常、不影响原 400 语义） */
    @Test
    void ensure_missingEverywhere_silentReturn() throws Exception {
        String code = "FD_HEAL_003";
        org.mockito.Mockito.lenient().when(foodMapper.selectByFoodCodeForUpdate(code)).thenReturn(null);
        org.mockito.Mockito.lenient().when(foodNewMapper.selectByFoodCode(code)).thenReturn(null);

        invoke(code);

        assertNull(null);
        verify(foodMapper, never()).insert(any(Food.class));
    }
}
