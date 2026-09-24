package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.foodtraceability.dto.order.OrderCreateDTO;
import com.foodtraceability.dto.order.OrderVO;
import com.foodtraceability.dto.order.PosQuickOrderDTO;
import com.foodtraceability.entity.ComboIngredientNew;
import com.foodtraceability.entity.DishComboNew;
import com.foodtraceability.entity.DishRecipeNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.OrderItemNew;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import com.foodtraceability.entity.OrderRefundRecordNew;
import com.foodtraceability.mapper.ComboIngredientNewMapper;
import com.foodtraceability.mapper.DiningTableNewMapper;
import com.foodtraceability.mapper.DishComboNewMapper;
import com.foodtraceability.mapper.DishRecipeNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.mapper.OrderItemNewMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.mapper.OrderRefundRecordNewMapper;
import com.foodtraceability.service.MaterialConsumptionAuditService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.finance.BankAccountService;
import com.foodtraceability.service.finance.CostRecordService;
import com.foodtraceability.service.finance.FundFlowService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * P1-ORDER-NUMBER-002 / Candidate A1 单元测试。
 * <p>
 * 验证 /v1/orders createOrder 与 Quick Order createPosQuickOrder 两条入口
 * 在 buildOrderEntity 中同值赋值 orderNumber = orderCode（E1b / E1c）。
 * <p>
 * 测试层级：单元测试（Mockito，无 Spring 上下文、无 DB、无 HTTP）。
 * 不替代 H-01/H-03 的 HTTP E2E；若 HTTP 层不可用，本层为独立运行时验证证据。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderNewServiceImplOrderNumberA1Test {

    private static final String ORDER_ID = "A1-ORDER-001";
    private static final Long FOOD_ID = 101L;
    private static final Long STORE_ID = 1L;

    @Mock private OrderNewMapper orderNewMapper;
    @Mock private OrderItemNewMapper orderItemNewMapper;
    @Mock private OrderPaymentRecordNewMapper orderPaymentRecordNewMapper;
    @Mock private OrderRefundRecordNewMapper orderRefundRecordNewMapper;
    @Mock private DiningTableNewMapper diningTableNewMapper;
    @Mock private FoodNewMapper foodNewMapper;
    @Mock private DishComboNewMapper dishComboNewMapper;
    @Mock private DishRecipeNewMapper dishRecipeNewMapper;
    @Mock private ComboIngredientNewMapper comboIngredientNewMapper;
    @Mock private KitchenOrderMapper kitchenOrderMapper;
    @Mock private StoreInventoryService storeInventoryService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private CostRecordService costRecordService;
    @Mock private FundFlowService fundFlowService;
    @Mock private BankAccountService bankAccountService;
    @Mock private MaterialConsumptionAuditService materialConsumptionAuditService;

    private OrderNewServiceImpl service;
    private OrderNew insertedOrder;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, KitchenOrder.class);
        TableInfoHelper.initTableInfo(assistant, OrderNew.class);
        TableInfoHelper.initTableInfo(assistant, OrderItemNew.class);
        TableInfoHelper.initTableInfo(assistant, FoodNew.class);
        TableInfoHelper.initTableInfo(assistant, DishRecipeNew.class);
        TableInfoHelper.initTableInfo(assistant, DishComboNew.class);
        TableInfoHelper.initTableInfo(assistant, ComboIngredientNew.class);
        TableInfoHelper.initTableInfo(assistant, OrderPaymentRecordNew.class);
        TableInfoHelper.initTableInfo(assistant, OrderRefundRecordNew.class);
    }

    @BeforeEach
    void setUp() {
        service = new OrderNewServiceImpl(
                orderNewMapper, orderItemNewMapper, orderPaymentRecordNewMapper, orderRefundRecordNewMapper,
                diningTableNewMapper, foodNewMapper, dishComboNewMapper, dishRecipeNewMapper,
                comboIngredientNewMapper, kitchenOrderMapper, storeInventoryService,
                applicationEventPublisher, costRecordService, fundFlowService, bankAccountService,
                materialConsumptionAuditService);

        insertedOrder = null;

        when(orderNewMapper.getMaxTodaySequence(any())).thenReturn(0);

        doAnswer(inv -> {
            OrderNew o = inv.getArgument(0);
            if (o.getOrderId() == null) {
                o.setOrderId(ORDER_ID);
            }
            insertedOrder = o;
            return 1;
        }).when(orderNewMapper).insert(any(OrderNew.class));

        when(orderNewMapper.selectById(ORDER_ID)).thenAnswer(inv -> insertedOrder);
        when(orderItemNewMapper.insert(any(OrderItemNew.class))).thenReturn(1);
        when(orderItemNewMapper.selectByOrderId(any())).thenReturn(Collections.emptyList());
        when(orderPaymentRecordNewMapper.selectByOrderId(any())).thenReturn(Collections.emptyList());
        when(orderRefundRecordNewMapper.selectByOrderId(any())).thenReturn(Collections.emptyList());

        FoodNew food = new FoodNew();
        food.setFoodId(FOOD_ID);
        food.setFoodName("A1测试菜品");
        food.setStatus(1);
        food.setSalePrice(2500L);
        food.setStock(null);
        when(foodNewMapper.selectById(FOOD_ID)).thenReturn(food);
    }

    private OrderCreateDTO createDTO() {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setOrderType(2);
        dto.setStoreId(STORE_ID);
        dto.setCustomerName("A1测试顾客");
        OrderCreateDTO.OrderItemCreateDTO item = new OrderCreateDTO.OrderItemCreateDTO();
        item.setProductType(1);
        item.setProductId(FOOD_ID);
        item.setQuantity(1);
        dto.setItems(List.of(item));
        return dto;
    }

    @DisplayName("H-01 unit-level: /v1/orders createOrder → order_number == order_code != null")
    @Test
    void createOrder_assignsOrderNumberEqualToOrderCode() {
        OrderVO vo = service.createOrder(createDTO());

        assertNotNull(insertedOrder, "insert must be called");
        assertNotNull(insertedOrder.getOrderCode(), "order_code must not be null");
        assertNotNull(insertedOrder.getOrderNumber(), "order_number must not be null (E1b)");
        assertEquals(insertedOrder.getOrderCode(), insertedOrder.getOrderNumber(),
                "A1: order_number must equal order_code");
        assertTrue(insertedOrder.getOrderCode().startsWith("ORD"), "order_code keeps ORD prefix (CC-2)");
        assertNotNull(vo);
        assertEquals(insertedOrder.getOrderCode(), vo.getOrderCode());
    }

    @DisplayName("H-03 unit-level: createPosQuickOrder independently → order_number == order_code != null")
    @Test
    void createPosQuickOrder_independently_assignsOrderNumberEqualToOrderCode() {
        PosQuickOrderDTO quick = new PosQuickOrderDTO();
        quick.setOrderType(2);
        quick.setStoreId(STORE_ID);
        PosQuickOrderDTO.QuickItem qi = new PosQuickOrderDTO.QuickItem();
        qi.setProductId(FOOD_ID);
        qi.setProductType(1);
        qi.setQuantity(2);
        quick.setItems(List.of(qi));

        OrderVO vo = service.createPosQuickOrder(quick);

        assertNotNull(insertedOrder, "quick order must insert entity");
        assertNotNull(insertedOrder.getOrderCode(), "quick order order_code must not be null");
        assertNotNull(insertedOrder.getOrderNumber(), "quick order order_number must not be null (E1c)");
        assertEquals(insertedOrder.getOrderCode(), insertedOrder.getOrderNumber(),
                "A1: quick order order_number must equal order_code");
        assertTrue(insertedOrder.getOrderCode().startsWith("ORD"), "quick order keeps ORD prefix (CC-2)");
        assertNotNull(vo);
    }

    @DisplayName("CC-2: order_code generation algorithm unchanged (ORD + timestamp + seq)")
    @Test
    void orderCode_format_remainsOrdPrefixed() {
        service.createOrder(createDTO());
        ArgumentCaptor<OrderNew> captor = ArgumentCaptor.forClass(OrderNew.class);
        assertNotNull(insertedOrder);
        String code = insertedOrder.getOrderCode();
        assertTrue(code.matches("ORD\\d{14}\\d{4}"), "expected ORD+14digit+4seq, got: " + code);
        assertEquals(code, insertedOrder.getOrderNumber());
    }
}
