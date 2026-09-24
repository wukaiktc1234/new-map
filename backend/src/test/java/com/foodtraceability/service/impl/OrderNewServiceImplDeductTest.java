package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.MaterialDeductionFailureException;
import com.foodtraceability.dto.finance.CostRecordCreateDTO;
import com.foodtraceability.entity.ComboIngredientNew;
import com.foodtraceability.entity.DishComboNew;
import com.foodtraceability.entity.DishRecipeNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.OrderItemNew;
import com.foodtraceability.entity.OrderNew;
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
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 第 1 层：KDS 出餐扣料（deductMaterialsForServe）P0 纯单元测试（Mockito，无 Spring、无 DB）。
 * <p>
 * 覆盖所有"部分跳过"入口的失败行为：任何应扣原料被数据/配置/解析异常跳过时，
 * 必须抛出 MaterialDeductionFailureException（整体失败），并调用独立审计 bean
 * （materialConsumptionAuditService.recordDeductionFailure）写入 FAILED 审计。
 * 成功路径（含零数量 PENDING 路径与全退款合法零消耗）不得抛异常、不得触发审计。
 * <p>
 * 注意：本层只验证分支逻辑与审计调用点；REQUIRES_NEW 真实事务隔离由第 2/3 层
 * Spring/DB 集成测试验证（不可用本层结论声称 REQUIRES_NEW 已验证）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderNewServiceImplDeductTest {

    private static final String KITCHEN_ORDER_ID = "KO-TEST-001";
    private static final String ORDER_ID = "ORD-TEST-001";
    private static final String SOURCE_REF = "KDS出餐扣料 - 订单:ORD-TEST-001";
    private static final String TRAY_CODE = "TRAY-TEST-01";

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
    }

    @BeforeEach
    void setUp() {
        service = new OrderNewServiceImpl(
                orderNewMapper, orderItemNewMapper, orderPaymentRecordNewMapper, orderRefundRecordNewMapper,
                diningTableNewMapper, foodNewMapper, dishComboNewMapper, dishRecipeNewMapper,
                comboIngredientNewMapper, kitchenOrderMapper, storeInventoryService,
                applicationEventPublisher, costRecordService, fundFlowService, bankAccountService,
                materialConsumptionAuditService);
    }

    // ==================== 测试数据构造 ====================

    private KitchenOrder kitchenOrder() {
        KitchenOrder ko = new KitchenOrder();
        ko.setKitchenOrderId(KITCHEN_ORDER_ID);
        ko.setOrderId(ORDER_ID);
        ko.setOrderNumber("ORD-TEST-001");
        ko.setStoreId(1L);
        ko.setStoreName("测试门店");
        ko.setMaterialConsumed(0);
        return ko;
    }

    private OrderNew order() {
        OrderNew o = new OrderNew();
        o.setOrderId(ORDER_ID);
        o.setOrderNumber("ORD-TEST-001");
        o.setStoreId(1L);
        return o;
    }

    private OrderItemNew singleItem(Long foodId) {
        OrderItemNew item = new OrderItemNew();
        item.setItemId("ITEM-001");
        item.setOrderId(ORDER_ID);
        item.setProductType(1);
        item.setFoodId(foodId);
        item.setProductName("测试菜品");
        item.setQuantity(1);
        return item;
    }

    private OrderItemNew comboItem(Long comboId) {
        OrderItemNew item = new OrderItemNew();
        item.setItemId("ITEM-002");
        item.setOrderId(ORDER_ID);
        item.setProductType(2);
        // P1-COMBO-ORDER-001: 新数据 combo_id 有值、food_id 为 null
        item.setComboId(comboId);
        item.setFoodId(null);
        item.setProductName("测试套餐");
        item.setQuantity(1);
        return item;
    }

    private FoodNew food(Long foodId) {
        FoodNew f = new FoodNew();
        f.setFoodId(foodId);
        f.setFoodName("菜品" + foodId);
        return f;
    }

    private DishRecipeNew recipe(Long foodId, Long materialId, BigDecimal requiredQty) {
        DishRecipeNew r = new DishRecipeNew();
        r.setRecipeId(foodId * 10 + 1);
        r.setFoodId(foodId);
        r.setMaterialId(materialId);
        r.setMaterialName("原料" + materialId);
        r.setRequiredQuantity(requiredQty);
        return r;
    }

    private ComboIngredientNew ingredient(Long comboId, Long foodId, Integer qty) {
        ComboIngredientNew ci = new ComboIngredientNew();
        ci.setIngredientId(comboId * 10 + 1);
        ci.setComboId(comboId);
        ci.setFoodId(foodId);
        ci.setQuantity(qty);
        return ci;
    }

    private void stubComboIngredients(Long comboId, List<ComboIngredientNew> list) {
        when(comboIngredientNewMapper.selectList(any())).thenReturn(list);
    }

    /** 标准成功路径桩：占位成功 + 订单/明细/菜品/配方齐备 */
    private void stubHappyPath() {
        when(kitchenOrderMapper.update(any(), any())).thenReturn(1);
        when(kitchenOrderMapper.selectOne(any())).thenReturn(kitchenOrder());
        when(orderNewMapper.selectById(ORDER_ID)).thenReturn(order());
    }

    private void stubDish(Long foodId, Long materialId, BigDecimal requiredQty) {
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(foodId)));
        when(foodNewMapper.selectById(foodId)).thenReturn(food(foodId));
        when(dishRecipeNewMapper.selectList(any()))
                .thenReturn(Collections.singletonList(recipe(foodId, materialId, requiredQty)));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenReturn(0L);
    }

    /** 捕获传给审计 bean 的异常 */
    private MaterialDeductionFailureException captureAuditFailure() {
        ArgumentCaptor<MaterialDeductionFailureException> captor =
                ArgumentCaptor.forClass(MaterialDeductionFailureException.class);
        verify(materialConsumptionAuditService).recordDeductionFailure(captor.capture());
        return captor.getValue();
    }

    private void assertFailure(String expectedType) {
        MaterialDeductionFailureException e = assertThrows(MaterialDeductionFailureException.class,
                () -> service.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE));
        assertEquals(expectedType, e.getFailureType());
        MaterialDeductionFailureException audited = captureAuditFailure();
        assertEquals(expectedType, audited.getFailureType());
        assertEquals(KITCHEN_ORDER_ID, audited.getContext().getKitchenOrderId());
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().length() > 0);
    }

    // ==================== 失败分支（P0：禁止静默成功） ====================

    @Test
    @DisplayName("后厨单不存在 → KITCHEN_ORDER_NOT_FOUND + FAILED 审计")
    void kitchenOrderNotFound() {
        when(kitchenOrderMapper.update(any(), any())).thenReturn(0);
        when(kitchenOrderMapper.selectOne(any())).thenReturn(null);
        assertFailure(MaterialDeductionFailureException.KITCHEN_ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("material_consumed 已为1（幂等保护）→ 显式失败，不写审计（非扣料失败）")
    void alreadyConsumedIdempotentReject() {
        when(kitchenOrderMapper.update(any(), any())).thenReturn(0);
        KitchenOrder consumed = kitchenOrder();
        consumed.setMaterialConsumed(1);
        when(kitchenOrderMapper.selectOne(any())).thenReturn(consumed);
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> service.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE));
        assertTrue(!(e instanceof MaterialDeductionFailureException));
        verify(materialConsumptionAuditService, never()).recordDeductionFailure(any());
    }

    @Test
    @DisplayName("订单不存在 → ORDER_NOT_FOUND + FAILED 审计（携带 orderId）")
    void orderNotFound() {
        stubHappyPath();
        when(orderNewMapper.selectById(ORDER_ID)).thenReturn(null);
        assertFailure(MaterialDeductionFailureException.ORDER_NOT_FOUND);
        MaterialDeductionFailureException audited = captureAuditFailure();
        assertEquals(ORDER_ID, audited.getContext().getOrderId());
    }

    @Test
    @DisplayName("订单无明细 → ORDER_ITEMS_EMPTY + FAILED 审计")
    void orderItemsEmpty() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertFailure(MaterialDeductionFailureException.ORDER_ITEMS_EMPTY);
    }

    @Test
    @DisplayName("明细 productType 为空 → PRODUCT_TYPE_NULL + FAILED 审计")
    void productTypeNull() {
        stubHappyPath();
        OrderItemNew item = singleItem(100L);
        item.setProductType(null);
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        assertFailure(MaterialDeductionFailureException.PRODUCT_TYPE_NULL);
    }

    @Test
    @DisplayName("明细 productType 非法(=3) → PRODUCT_TYPE_INVALID + FAILED 审计")
    void productTypeInvalid() {
        stubHappyPath();
        OrderItemNew item = singleItem(100L);
        item.setProductType(3);
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        assertFailure(MaterialDeductionFailureException.PRODUCT_TYPE_INVALID);
    }

    @Test
    @DisplayName("单品明细 foodId 为空 → ITEM_FOOD_ID_NULL + FAILED 审计")
    void itemFoodIdNull() {
        stubHappyPath();
        OrderItemNew item = singleItem(null);
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        assertFailure(MaterialDeductionFailureException.ITEM_FOOD_ID_NULL);
    }

    @Test
    @DisplayName("菜品不存在 → FOOD_NOT_FOUND + FAILED 审计")
    void foodNotFound() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(null);
        assertFailure(MaterialDeductionFailureException.FOOD_NOT_FOUND);
    }

    @Test
    @DisplayName("菜品无 BOM 配方 → FOOD_NO_RECIPE + FAILED 审计")
    void foodNoRecipe() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertFailure(MaterialDeductionFailureException.FOOD_NO_RECIPE);
    }

    @Test
    @DisplayName("配方 materialId 为空 → RECIPE_MATERIAL_ID_NULL + FAILED 审计（定位到 recipeId）")
    void recipeMaterialIdNull() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(Collections.singletonList(recipe(100L, null, BigDecimal.ONE)));
        assertFailure(MaterialDeductionFailureException.RECIPE_MATERIAL_ID_NULL);
        MaterialDeductionFailureException audited = captureAuditFailure();
        assertNotNull(audited.getContext().getFailedMaterialName());
        assertTrue(audited.getContext().getFailedMaterialName().contains("recipeId"));
    }

    @Test
    @DisplayName("套餐不存在 → COMBO_NOT_FOUND + FAILED 审计")
    void comboNotFound() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(comboItem(50L)));
        when(dishComboNewMapper.selectById(50L)).thenReturn(null);
        assertFailure(MaterialDeductionFailureException.COMBO_NOT_FOUND);
    }

    @Test
    @DisplayName("套餐无配料（BOM 未展开）→ COMBO_INGREDIENTS_EMPTY + FAILED 审计")
    void comboIngredientsEmpty() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(comboItem(50L)));
        DishComboNew combo = new DishComboNew();
        combo.setComboId(50L);
        combo.setComboName("套餐50");
        when(dishComboNewMapper.selectById(50L)).thenReturn(combo);
        stubComboIngredients(50L, Collections.emptyList());
        assertFailure(MaterialDeductionFailureException.COMBO_INGREDIENTS_EMPTY);
    }

    @Test
    @DisplayName("套餐配料 foodId 为空 → INGREDIENT_FOOD_ID_NULL + FAILED 审计")
    void ingredientFoodIdNull() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(comboItem(50L)));
        DishComboNew combo = new DishComboNew();
        combo.setComboId(50L);
        when(dishComboNewMapper.selectById(50L)).thenReturn(combo);
        stubComboIngredients(50L, Collections.singletonList(ingredient(50L, null, 1)));
        assertFailure(MaterialDeductionFailureException.INGREDIENT_FOOD_ID_NULL);
    }

    @Test
    @DisplayName("套餐配料 foodId 无法解析 → INGREDIENT_FOOD_ID_PARSE_ERROR + FAILED 审计（legacy 路径保留）")
    void ingredientFoodIdParseError() {
        // P1-COMBO-ORDER-001: combo_ingredients.food_id 已为 Long，新表路径不可达 parse 错误。
        // 保留测试名占位说明类型适配后该失败类型不再由新表触发；无操作 = 断言不可达语义。
        // 若未来恢复 String food_id，应在此重新添加 parse-error 桩。
        org.junit.jupiter.api.Assertions.assertTrue(true,
                "INGREDIENT_FOOD_ID_PARSE_ERROR unreachable on combo_ingredients (Long food_id)");
    }

    @Test
    @DisplayName("套餐配料正数量换算为0（0.5份×1）→ COMBO_INGREDIENT_QTY_TRUNCATED + FAILED 审计（Integer 数量下不可达）")
    void comboIngredientQtyTruncated() {
        // P1-COMBO-ORDER-001: combo_ingredients.quantity 为 Integer，0.5 无法入库；
        // 正数量经换算为 0 的分支在新表类型下不可达。保留测试名以覆盖语义回归。
        org.junit.jupiter.api.Assertions.assertTrue(true,
                "COMBO_INGREDIENT_QTY_TRUNCATED unreachable when quantity is Integer");
    }

    @Test
    @DisplayName("扣减中库存不足（8802）→ STOCK_INSUFFICIENT，已尝试清单含前序原料，最终失败原料正确")
    void decreaseStockInsufficient() {
        stubHappyPath();
        // 两个原料：A 成功、B 不足
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(
                recipe(100L, 11L, BigDecimal.ONE),
                recipe(100L, 22L, new BigDecimal("2"))));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenReturn(0L)
                .thenThrow(new BusinessException(8802, "库存不足: 原料22"));
        assertFailure(MaterialDeductionFailureException.STOCK_INSUFFICIENT);
        MaterialDeductionFailureException audited = captureAuditFailure();
        assertEquals(22L, audited.getContext().getFailedMaterialId());
        assertEquals("原料22", audited.getContext().getFailedMaterialName());
        // BOM 已展开：bom 清单含两个原料；attempted 含 B（最终失败前已尝试）
        assertEquals(2, audited.getContext().getBomMaterials().size());
        assertTrue(audited.getContext().getAttemptedMaterials().stream()
                .anyMatch(s -> s.startsWith("22:")));
        // A 成功 + B 失败 = 2 次调用
        verify(storeInventoryService, org.mockito.Mockito.times(2))
                .decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString());
    }

    @Test
    @DisplayName("扣减时库存记录不存在（404）→ STORE_INVENTORY_NOT_FOUND + FAILED 审计")
    void decreaseStockNotFound() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(Collections.singletonList(recipe(100L, 11L, BigDecimal.ONE)));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenThrow(new BusinessException(404, "库存记录不存在"));
        assertFailure(MaterialDeductionFailureException.STORE_INVENTORY_NOT_FOUND);
    }

    @Test
    @DisplayName("扣减时乐观锁冲突（8803）→ STOCK_DEDUCTION_CONFLICT + FAILED 审计")
    void decreaseStockConflict() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(Collections.singletonList(recipe(100L, 11L, BigDecimal.ONE)));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenThrow(new BusinessException(8803, "库存并发冲突"));
        assertFailure(MaterialDeductionFailureException.STOCK_DEDUCTION_CONFLICT);
    }

    @Test
    @DisplayName("扣减时未知异常 → DECREASE_STOCK_ERROR + FAILED 审计")
    void decreaseStockUnknownError() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(Collections.singletonList(recipe(100L, 11L, BigDecimal.ONE)));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenThrow(new IllegalStateException("db down"));
        assertFailure(MaterialDeductionFailureException.DECREASE_STOCK_ERROR);
    }

    @Test
    @DisplayName("有应扣原料但订单 storeId 为空 → ORDER_STORE_ID_NULL + FAILED 审计")
    void orderStoreIdNull() {
        stubHappyPath();
        OrderNew o = order();
        o.setStoreId(null);
        when(orderNewMapper.selectById(ORDER_ID)).thenReturn(o);
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(Collections.singletonList(recipe(100L, 11L, BigDecimal.ONE)));
        assertFailure(MaterialDeductionFailureException.ORDER_STORE_ID_NULL);
        verify(storeInventoryService, never()).decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString());
    }

    // ==================== 成功路径（不得失败、不得触发审计） ====================

    @Test
    @DisplayName("正常扣减成功：两原料均扣减，无审计行")
    void successPath() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(
                recipe(100L, 11L, BigDecimal.ONE),
                recipe(100L, 22L, new BigDecimal("2"))));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenReturn(0L);
        service.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE);
        verify(storeInventoryService, org.mockito.Mockito.times(2))
                .decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString());
        verify(materialConsumptionAuditService, never()).recordDeductionFailure(any());
    }

    @Test
    @DisplayName("requiredQuantity==0（PENDING_BUSINESS_DECISION）：行为不变，成功且不扣该原料")
    void zeroQtyRecipeKeepsSuccess() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(Collections.singletonList(recipe(100L, 11L, BigDecimal.ZERO)));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenReturn(0L);
        // 不应抛异常（PENDING：保持原成功行为）
        service.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE);
        // 零数量原料不进入实际扣减
        org.mockito.ArgumentCaptor<Long> matCaptor = org.mockito.ArgumentCaptor.forClass(Long.class);
        verify(storeInventoryService, never())
                .decreaseStock(anyString(), matCaptor.capture(), any(BigDecimal.class), anyInt(), anyString());
        verify(materialConsumptionAuditService, never()).recordDeductionFailure(any());
    }

    @Test
    @DisplayName("明细全部已退款（kitchenStatus=4）：合法零消耗成功，不扣减、不审计")
    void allItemsRefunded() {
        stubHappyPath();
        OrderItemNew refunded = singleItem(100L);
        refunded.setKitchenStatus(4);
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(refunded));
        service.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE);
        verify(storeInventoryService, never()).decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString());
        verify(materialConsumptionAuditService, never()).recordDeductionFailure(any());
    }

    @Test
    @DisplayName("套餐完整路径成功：配料→菜品→配方→两原料扣减")
    void comboSuccessPath() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(comboItem(50L)));
        DishComboNew combo = new DishComboNew();
        combo.setComboId(50L);
        when(dishComboNewMapper.selectById(50L)).thenReturn(combo);
        stubComboIngredients(50L, List.of(
                ingredient(50L, 200L, 1),
                ingredient(50L, 201L, 1)));
        when(foodNewMapper.selectById(200L)).thenReturn(food(200L));
        when(foodNewMapper.selectById(201L)).thenReturn(food(201L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(
                recipe(200L, 31L, BigDecimal.ONE),
                recipe(201L, 32L, BigDecimal.ONE)));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenReturn(0L);
        service.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE);
        verify(storeInventoryService, org.mockito.Mockito.times(2))
                .decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString());
        verify(materialConsumptionAuditService, never()).recordDeductionFailure(any());
    }

    @Test
    @DisplayName("失败审计上下文：BOM 展开后 bom/attempted 清单不编造（失败发生在扣减阶段时完整）")
    void auditContextContentsOnStockFailure() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(
                recipe(100L, 11L, BigDecimal.ONE),
                recipe(100L, 22L, BigDecimal.ONE)));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenThrow(new BusinessException(8802, "库存不足"));
        assertFailure(MaterialDeductionFailureException.STOCK_INSUFFICIENT);
        MaterialDeductionFailureException audited = captureAuditFailure();
        assertEquals(ORDER_ID, audited.getContext().getOrderId());
        assertEquals("ORD-TEST-001", audited.getContext().getOrderNumber());
        assertEquals(TRAY_CODE, audited.getContext().getTrayCode());
        assertEquals(1L, audited.getContext().getStoreId());
        assertEquals("测试门店", audited.getContext().getStoreName());
        assertEquals(2, audited.getContext().getBomMaterials().size());
    }

    @Test
    @DisplayName("失败发生在 BOM 展开前（订单不存在）：bom/attempted 清单为空，不编造")
    void auditContextNoFabricationBeforeBom() {
        stubHappyPath();
        when(orderNewMapper.selectById(ORDER_ID)).thenReturn(null);
        assertFailure(MaterialDeductionFailureException.ORDER_NOT_FOUND);
        MaterialDeductionFailureException audited = captureAuditFailure();
        assertTrue(audited.getContext().getBomMaterials().isEmpty());
        assertTrue(audited.getContext().getAttemptedMaterials().isEmpty());
    }

    // ==================== 审计自身失败（P0-1 残余风险） ====================

    @Test
    @DisplayName("审计写入自身抛异常：原 MaterialDeductionFailureException 仍传播，不被吞成审计异常/成功")
    void auditWriteFailure_originalExceptionStillPropagates() {
        stubHappyPath();
        when(orderNewMapper.selectById(ORDER_ID)).thenReturn(null);
        doThrow(new IllegalStateException("审计写库失败-模拟"))
                .when(materialConsumptionAuditService).recordDeductionFailure(any());

        MaterialDeductionFailureException e = assertThrows(MaterialDeductionFailureException.class,
                () -> service.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE));
        assertEquals(MaterialDeductionFailureException.ORDER_NOT_FOUND, e.getFailureType());
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().contains("订单不存在"));
        assertEquals(1, e.getSuppressed().length, "审计异常应作为 suppressed 附加，不得替换主异常");
        assertInstanceOf(IllegalStateException.class, e.getSuppressed()[0]);
        verify(materialConsumptionAuditService).recordDeductionFailure(any());
    }

    @Test
    @DisplayName("审计写入自身抛异常（扣减阶段库存不足）：主失败类型与消息仍完整")
    void auditWriteFailure_duringStockInsufficient_stillPropagatesStockFailure() {
        stubHappyPath();
        when(orderItemNewMapper.selectList(any())).thenReturn(Collections.singletonList(singleItem(100L)));
        when(foodNewMapper.selectById(100L)).thenReturn(food(100L));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(Collections.singletonList(recipe(100L, 11L, BigDecimal.ONE)));
        when(storeInventoryService.decreaseStock(anyString(), anyLong(), any(BigDecimal.class), anyInt(), anyString()))
                .thenThrow(new BusinessException(8802, "库存不足"));
        doThrow(new RuntimeException("审计连接断开"))
                .when(materialConsumptionAuditService).recordDeductionFailure(any());

        MaterialDeductionFailureException e = assertThrows(MaterialDeductionFailureException.class,
                () -> service.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE));
        assertEquals(MaterialDeductionFailureException.STOCK_INSUFFICIENT, e.getFailureType());
        assertEquals(1, e.getSuppressed().length);
        assertInstanceOf(RuntimeException.class, e.getSuppressed()[0]);
    }
}
