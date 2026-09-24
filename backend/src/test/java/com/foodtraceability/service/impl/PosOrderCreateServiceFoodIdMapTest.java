package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.foodtraceability.common.Result;
import com.foodtraceability.controller.websocket.OrderWebSocketController;
import com.foodtraceability.dto.CanonicalOrderCommand;
import com.foodtraceability.dto.CanonicalOrderItemCommand;
import com.foodtraceability.dto.OrderResultDTO;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.OrderItemNew;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.StoreNew;
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
import com.foodtraceability.service.PosOrderNumberGenerator;
import com.foodtraceability.service.SensitiveDataService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-POS-FOODID-MAP-001 / B2 双路径单元测试。
 * <p>
 * 覆盖 createCanonicalOrder 的 foodCode→foodId 映射预检：
 * <ul>
 *   <li>命中路径：food_code 存在 → 下单成功，order_items.food_id 非空（零回归锚点）</li>
 *   <li>miss 路径：未知 food_code → Result code=400，orders/order_items 均不写入</li>
 * </ul>
 * 测试层级：单元测试（Mockito，无 Spring 上下文、无 DB、无 HTTP）。
 * 不替代 H-06/REG-ORDER-006 的 HTTP E2E。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosOrderCreateServiceFoodIdMapTest {

    private static final String FOOD_CODE = "FD001";
    private static final Long FOOD_ID = 101L;
    private static final String STORE_ID = "1";

    @Mock private KitchenOrderMapper kitchenOrderMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private FoodMapper foodMapper;
    @Mock private FoodNewMapper foodNewMapper;
    @Mock private DishComboMapper dishComboMapper;
    @Mock private DishComboNewMapper dishComboNewMapper;
    @Mock private ComboIngredientNewMapper comboIngredientNewMapper;
    @Mock private OrderWebSocketController orderWebSocketController;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SensitiveDataService sensitiveDataService;
    @Mock private PosOrderNumberGenerator numberGenerator;
    @Mock private StoreNewMapper storeNewMapper;
    @Mock private OrderNewMapper orderNewMapper;
    @Mock private OrderItemNewMapper orderItemNewMapper;

    private PosOrderCreateServiceImpl service;
    private OrderNew insertedOrder;
    private final List<OrderItemNew> insertedItems = new java.util.ArrayList<>();

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, KitchenOrder.class);
        TableInfoHelper.initTableInfo(assistant, OrderNew.class);
        TableInfoHelper.initTableInfo(assistant, OrderItemNew.class);
        TableInfoHelper.initTableInfo(assistant, FoodNew.class);
        TableInfoHelper.initTableInfo(assistant, StoreNew.class);
    }

    @BeforeEach
    void setUp() {
        service = new PosOrderCreateServiceImpl(
                kitchenOrderMapper, orderMapper, orderItemMapper, foodMapper, foodNewMapper,
                dishComboMapper, dishComboNewMapper, comboIngredientNewMapper,
                orderWebSocketController, eventPublisher, sensitiveDataService,
                numberGenerator, storeNewMapper, orderNewMapper, orderItemNewMapper);

        insertedOrder = null;
        insertedItems.clear();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.initSynchronization();

        when(numberGenerator.generateOrderNumberByDbSequence(any())).thenReturn("T20260924001");
        when(numberGenerator.incrementAndGetCounter()).thenReturn(1L, 2L, 3L, 4L);
        when(numberGenerator.generatePickupNumberByType(anyInt())).thenReturn("A001");
        when(numberGenerator.generatePickupCode()).thenReturn("1234");

        when(storeNewMapper.selectById(any())).thenReturn(null);
        when(storeNewMapper.selectByStoreCode(anyString())).thenReturn(null);
        when(sensitiveDataService.encryptPhone(anyString())).thenReturn("ENC");
        when(sensitiveDataService.encryptAddress(anyString())).thenReturn("ENC");

        when(orderNewMapper.insert(any(OrderNew.class))).thenAnswer(inv -> {
            insertedOrder = inv.getArgument(0);
            return 1;
        });
        when(orderNewMapper.updateById(any(OrderNew.class))).thenReturn(1);

        when(orderItemNewMapper.insert(any(OrderItemNew.class))).thenAnswer(inv -> {
            OrderItemNew item = inv.getArgument(0);
            insertedItems.add(item);
            return 1;
        });
        when(orderItemNewMapper.selectList(any())).thenAnswer(inv -> List.copyOf(insertedItems));

        when(kitchenOrderMapper.insert(any(KitchenOrder.class))).thenReturn(1);
        when(foodMapper.deductStock(anyString(), anyInt())).thenReturn(1);
        when(foodNewMapper.deductStock(anyString(), anyInt())).thenReturn(1);
        when(foodNewMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        when(foodNewMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private FoodNew buildFood() {
        FoodNew food = new FoodNew();
        food.setFoodId(FOOD_ID);
        food.setFoodCode(FOOD_CODE);
        food.setFoodName("测试菜品");
        food.setSalePrice(2500L);
        food.setStock(100);
        food.setStatus(1);
        return food;
    }

    private CanonicalOrderCommand buildCommand(String foodIdValue) {
        CanonicalOrderCommand command = new CanonicalOrderCommand();
        command.setOrderType(0);
        command.setOrderSource(4);
        command.setStoreId(STORE_ID);
        command.setUserId("u1");
        CanonicalOrderItemCommand item = new CanonicalOrderItemCommand();
        item.setFoodId(foodIdValue);
        item.setFoodName("测试菜品");
        item.setUnitPrice(new BigDecimal("25.00"));
        item.setQuantity(2);
        command.setItems(List.of(item));
        return command;
    }

    @DisplayName("B2 hit: food_code 命中 → 下单成功，order_items.food_id = foods.food_id（零回归）")
    @Test
    void createCanonicalOrder_foodCodeHit_insertsNonNullFoodId() {
        FoodNew food = buildFood();
        when(foodNewMapper.selectList(any())).thenReturn(List.of(food));

        Result<OrderResultDTO> result = service.createCanonicalOrder(buildCommand(FOOD_CODE));

        assertEquals(0, result.getCode(), "hit path must succeed: " + result.getMessage());
        assertNotNull(result.getData());
        assertNotNull(insertedOrder, "orders insert must be called");
        assertEquals(1, insertedItems.size(), "exactly one order_items row");
        OrderItemNew oi = insertedItems.get(0);
        assertNotNull(oi.getFoodId(), "order_items.food_id must not be null (H-06 anchor)");
        assertEquals(FOOD_ID, oi.getFoodId(), "food_id must resolve via food_code map");
        assertEquals(1, oi.getProductType(), "canonical path writes product_type=1 (unchanged)");
        assertEquals(2500L, oi.getUnitPrice(), "unit price from foods.sale_price (fen)");
        assertEquals(2, oi.getQuantity());
        assertEquals(5000L, oi.getAmount());
    }

    @DisplayName("B2 miss: 未知 food_code → Result.code=400，orders/order_items 均不写入")
    @Test
    void createCanonicalOrder_unknownFoodCode_rejectsWith400_noInsert() {
        when(foodNewMapper.selectList(any())).thenReturn(Collections.emptyList());

        Result<OrderResultDTO> result = service.createCanonicalOrder(buildCommand("FD999"));

        assertEquals(400, result.getCode(), "miss must return 400 (B2), got: " + result.getCode());
        assertNotNull(result.getMessage());
        assertNull(result.getData());
        verify(orderNewMapper, never()).insert(any(OrderNew.class));
        verify(orderItemNewMapper, never()).insert(any(OrderItemNew.class));
        verify(kitchenOrderMapper, never()).insert(any(KitchenOrder.class));
    }

    @DisplayName("B2 miss: 数值 id 非 food_code → Result.code=400（禁止 warn-then-insert）")
    @Test
    void createCanonicalOrder_numericIdMiss_rejectsWith400_noInsert() {
        when(foodNewMapper.selectList(any())).thenReturn(List.of(buildFood()));

        Result<OrderResultDTO> result = service.createCanonicalOrder(buildCommand("999"));

        assertEquals(400, result.getCode(), "numeric id not present as food_code must 400");
        verify(orderNewMapper, never()).insert(any(OrderNew.class));
        verify(orderItemNewMapper, never()).insert(any(OrderItemNew.class));
    }

    @DisplayName("B2 miss message: 含菜品标识与 food_code 关键字（可观测性）")
    @Test
    void createCanonicalOrder_miss_messageContainsFoodIdAndFoodCode() {
        when(foodNewMapper.selectList(any())).thenReturn(Collections.emptyList());

        Result<OrderResultDTO> result = service.createCanonicalOrder(buildCommand("FD404"));

        assertEquals(400, result.getCode());
        String msg = result.getMessage();
        assertNotNull(msg);
        org.junit.jupiter.api.Assertions.assertTrue(msg.contains("FD404"), "message must echo foodId: " + msg);
        org.junit.jupiter.api.Assertions.assertTrue(msg.contains("food_code"), "message must mention food_code: " + msg);
    }

    @DisplayName("R1: 单测断言 fail-fast 代码路径存在（source-level smoke via hit+miss pair）")
    @Test
    void dualPath_summary_hitSucceeds_missIs400() {
        when(foodNewMapper.selectList(any())).thenReturn(List.of(buildFood()));
        Result<OrderResultDTO> hit = service.createCanonicalOrder(buildCommand(FOOD_CODE));
        assertEquals(0, hit.getCode());

        when(foodNewMapper.selectList(any())).thenReturn(Collections.emptyList());
        Result<OrderResultDTO> miss = service.createCanonicalOrder(buildCommand("NOPE"));
        assertEquals(400, miss.getCode());
    }
}
