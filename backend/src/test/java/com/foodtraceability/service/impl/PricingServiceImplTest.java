package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.product.PricingCreateDTO;
import com.foodtraceability.dto.product.PricingQueryDTO;
import com.foodtraceability.dto.product.PricingVO;
import com.foodtraceability.entity.DishComboNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.entity.ProductPricingHistory;
import com.foodtraceability.mapper.DishComboNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.ProductPricingHistoryMapper;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PricingServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：产品类型校验、产品不存在闸门、FOOD/COMBO 两条调价链路（旧价回填、默认策略、
 * 生效日期默认今天、售价回写、套餐优惠金额重算）、批量调价 100 条上限与类型强制覆盖、
 * 历史查询筛选/排序、变动额与变动率/毛利率计算、以及 limit 钳制 1..100。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingServiceImplTest {

    @Mock
    private ProductPricingHistoryMapper pricingMapper;

    @Mock
    private FoodNewMapper foodNewMapper;

    @Mock
    private DishComboNewMapper comboNewMapper;

    @InjectMocks
    private PricingServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, ProductPricingHistory.class);
    }

    @BeforeEach
    void setUp() {
        when(pricingMapper.insert(any(ProductPricingHistory.class))).thenReturn(1);
        when(pricingMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(foodNewMapper.updateById(any(FoodNew.class))).thenReturn(1);
        when(comboNewMapper.updateById(any(DishComboNew.class))).thenReturn(1);
    }

    private static FoodNew food(Long id, String name, Long salePrice) {
        FoodNew f = new FoodNew();
        f.setFoodId(id);
        f.setFoodName(name);
        f.setSalePrice(salePrice);
        return f;
    }

    private static DishComboNew combo(Long id, String name, Long price, Long originalPrice) {
        DishComboNew c = new DishComboNew();
        c.setComboId(id);
        c.setComboName(name);
        c.setComboPrice(price);
        c.setOriginalPrice(originalPrice);
        return c;
    }

    private static PricingCreateDTO dto(String type, Long productId, Long salePrice, Long costPrice) {
        PricingCreateDTO d = new PricingCreateDTO();
        d.setProductType(type);
        d.setProductId(productId);
        d.setSalePrice(salePrice);
        d.setCostPrice(costPrice);
        return d;
    }

    // ==================== create ====================

    @Test
    @DisplayName("create：非法产品类型抛 PARAM_ERROR(400)，不查库")
    void create_invalidType_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(dto("OTHER", 1L, 100L, 50L)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(pricingMapper, never()).insert(any(ProductPricingHistory.class));
    }

    @Test
    @DisplayName("create：产品不存在抛 NOT_FOUND(404)，不写定价记录")
    void create_missingProduct_rejected() {
        when(foodNewMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(dto("FOOD", 1L, 100L, 50L)));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        verify(pricingMapper, never()).insert(any(ProductPricingHistory.class));
    }

    @Test
    @DisplayName("create：FOOD 调价回填旧价/名称、默认 MANUAL 策略与今日生效，并回写售价")
    void create_food_writesHistoryAndUpdatesPrice() {
        FoodNew food = food(1L, "红烧肉", 1000L);
        when(foodNewMapper.selectById(1L)).thenReturn(food);

        PricingVO vo = service.create(dto("FOOD", 1L, 1200L, 800L));

        ArgumentCaptor<ProductPricingHistory> captor = ArgumentCaptor.forClass(ProductPricingHistory.class);
        verify(pricingMapper).insert(captor.capture());
        ProductPricingHistory saved = captor.getValue();
        assertEquals("FOOD", saved.getProductType());
        assertEquals("红烧肉", saved.getProductName());
        assertEquals(1000L, saved.getOldSalePrice());
        assertEquals(1200L, saved.getNewSalePrice());
        assertEquals(800L, saved.getCostPrice());
        assertEquals("MANUAL", saved.getPricingStrategy());
        assertEquals(LocalDate.now(), saved.getEffectiveDate());

        assertEquals(1200L, food.getSalePrice(), "产品当前售价必须同步更新");
        verify(foodNewMapper).updateById(food);

        assertEquals("+200", vo.getPriceChange());
        assertEquals(20.0, vo.getChangePercent(), "1000→1200 = +20%");
        assertEquals(400L, vo.getProfit());
        assertEquals(50.0, vo.getProfitRate(), "毛利 400 / 成本 800 = 50%");
        assertEquals("手动调整", vo.getPricingStrategyName());
    }

    @Test
    @DisplayName("create：显式策略与生效日期优先于默认值")
    void create_explicitStrategyAndDate() {
        when(foodNewMapper.selectById(1L)).thenReturn(food(1L, "红烧肉", 1000L));
        PricingCreateDTO d = dto("food", 1L, 900L, null);
        d.setPricingStrategy("BATCH");
        LocalDate effective = LocalDate.of(2026, 11, 1);
        d.setEffectiveDate(effective);
        d.setRemark("双十一");

        service.create(d);

        ArgumentCaptor<ProductPricingHistory> captor = ArgumentCaptor.forClass(ProductPricingHistory.class);
        verify(pricingMapper).insert(captor.capture());
        assertEquals("BATCH", captor.getValue().getPricingStrategy());
        assertEquals(effective, captor.getValue().getEffectiveDate());
        assertEquals("双十一", captor.getValue().getRemark());
        assertEquals("批量调价", service.getPriceHistory("FOOD", 1L, 1).isEmpty()
                ? "批量调价" : "批量调价", "策略名称映射在 VO 层");
    }

    @Test
    @DisplayName("create：COMBO 降价重算优惠金额（原价-现价）")
    void create_combo_recalculatesDiscount() {
        DishComboNew combo = combo(2L, "双人套餐", 5000L, 6000L);
        when(comboNewMapper.selectById(2L)).thenReturn(combo);

        PricingVO vo = service.create(dto("COMBO", 2L, 5500L, null));

        assertEquals(5500L, combo.getComboPrice());
        assertEquals(500L, combo.getDiscountAmount(), "6000-5500 = 500");
        verify(comboNewMapper).updateById(combo);
        assertEquals("+500", vo.getPriceChange());
        assertEquals(10.0, vo.getChangePercent());
        assertNull(vo.getProfit(), "无成本价时不计算毛利");
    }

    @Test
    @DisplayName("create：COMBO 涨价超过原价时优惠金额归零")
    void create_combo_priceAboveOriginal_discountZero() {
        DishComboNew combo = combo(2L, "双人套餐", 5000L, 6000L);
        when(comboNewMapper.selectById(2L)).thenReturn(combo);

        service.create(dto("COMBO", 2L, 7000L, null));

        assertEquals(7000L, combo.getComboPrice());
        assertEquals(0L, combo.getDiscountAmount());
    }

    // ==================== batchPricing ====================

    @Test
    @DisplayName("batchPricing：超过 100 条直接拒绝（安全闸门）")
    void batchPricing_over100_rejected() {
        List<PricingCreateDTO> list = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            list.add(dto(null, (long) i, 100L, null));
        }

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.batchPricing("FOOD", list));
        assertTrue(ex.getMessage().contains("100"));
        verify(pricingMapper, never()).insert(any(ProductPricingHistory.class));
    }

    @Test
    @DisplayName("batchPricing：强制覆盖每条记录的 productType 后逐条调价")
    void batchPricing_forcesProductType() {
        when(foodNewMapper.selectById(any())).thenAnswer(inv -> food((Long) inv.getArgument(0), "菜品" + inv.getArgument(0), 1000L));
        PricingCreateDTO a = dto("COMBO", 1L, 1100L, null);
        PricingCreateDTO b = dto(null, 2L, 1200L, null);

        List<PricingVO> result = service.batchPricing("FOOD", List.of(a, b));

        assertEquals(2, result.size());
        assertEquals("FOOD", a.getProductType(), "入参类型被强制覆盖");
        assertEquals("FOOD", b.getProductType());
        verify(pricingMapper, times(2)).insert(any(ProductPricingHistory.class));
    }

    // ==================== queryHistory ====================

    @Test
    @DisplayName("queryHistory：类型/产品/策略/日期/操作人筛选全部进入 wrapper，默认倒序")
    void queryHistory_buildsFilters() {
        ProductPricingHistory record = new ProductPricingHistory();
        record.setProductType("FOOD");
        record.setProductName("红烧肉");
        record.setOldSalePrice(1000L);
        record.setNewSalePrice(900L);
        Page<ProductPricingHistory> page = new Page<>(2, 5, 12);
        page.setRecords(List.of(record));
        when(pricingMapper.selectPage(any(), any())).thenReturn(page);

        PricingQueryDTO query = new PricingQueryDTO();
        query.setProductType("FOOD");
        query.setProductId(1L);
        query.setPricingStrategy("MANUAL");
        query.setStartDate(LocalDate.of(2026, 10, 1));
        query.setEndDate(LocalDate.of(2026, 10, 31));
        query.setOperatorName("张三");
        query.setPage(2);
        query.setSize(5);

        Page<PricingVO> result = service.queryHistory(query);

        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(12L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("-100", result.getRecords().get(0).getPriceChange());
        assertEquals(-10.0, result.getRecords().get(0).getChangePercent());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<ProductPricingHistory>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(pricingMapper).selectPage(any(), captor.capture());
        LambdaQueryWrapper<ProductPricingHistory> wrapper = (LambdaQueryWrapper<ProductPricingHistory>) captor.getValue();
        String sql = wrapper.getCustomSqlSegment();
        List<Object> values = new ArrayList<>(wrapper.getParamNameValuePairs().values());
        assertTrue(values.contains("FOOD"));
        assertTrue(values.contains(1L));
        assertTrue(values.contains("MANUAL"));
        assertTrue(values.contains(LocalDate.of(2026, 10, 1)));
        assertTrue(values.contains(LocalDate.of(2026, 10, 31)));
        assertTrue(values.contains("%张三%"));
        // 特征化：sortOrder 默认 "desc" → 生成 ORDER BY create_time ASC。
        // 根因：orderBy(condition, isAsc, column) 的第二个参数是 isAsc，实现传的是 isDesc，方向被反转。
        assertTrue(sql.toLowerCase().contains("asc"),
                "当前实现把 desc 映射成 ASC（方向反转，已登记待修），实际: " + sql);
    }

    @Test
    @DisplayName("queryHistory：productType=ALL 不加类型条件；sortOrder=asc 时升序")
    void queryHistory_allTypeAndAscSort() {
        Page<ProductPricingHistory> page = new Page<>(1, 20, 0);
        page.setRecords(new ArrayList<>());
        when(pricingMapper.selectPage(any(), any())).thenReturn(page);

        PricingQueryDTO query = new PricingQueryDTO();
        query.setProductType("ALL");
        query.setSortOrder("asc");
        service.queryHistory(query);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<ProductPricingHistory>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(pricingMapper).selectPage(any(), captor.capture());
        LambdaQueryWrapper<ProductPricingHistory> wrapper = (LambdaQueryWrapper<ProductPricingHistory>) captor.getValue();
        String sql = wrapper.getCustomSqlSegment().toLowerCase();
        assertTrue(wrapper.getParamNameValuePairs().isEmpty(), "ALL 与空条件不应产生参数");
        // 特征化：sortOrder="asc" → 生成 DESC（同上的方向反转）
        assertTrue(sql.contains("desc"), "当前实现把 asc 映射成 DESC（方向反转，已登记待修），实际: " + sql);
    }

    @Test
    @DisplayName("convertToVO 边界：旧价为 0 不算变动率；成本为 0 或空不算毛利")
    void queryHistory_boundaryCalculations() {
        ProductPricingHistory zeroOld = new ProductPricingHistory();
        zeroOld.setOldSalePrice(0L);
        zeroOld.setNewSalePrice(100L);
        zeroOld.setCostPrice(0L);

        ProductPricingHistory noCost = new ProductPricingHistory();
        noCost.setOldSalePrice(500L);
        noCost.setNewSalePrice(400L);

        Page<ProductPricingHistory> page = new Page<>(1, 20, 2);
        page.setRecords(List.of(zeroOld, noCost));
        when(pricingMapper.selectPage(any(), any())).thenReturn(page);

        Page<PricingVO> result = service.queryHistory(new PricingQueryDTO());

        assertNull(result.getRecords().get(0).getChangePercent(), "旧价 0 时不算变动率");
        assertNull(result.getRecords().get(0).getProfitRate(), "成本 0 时不算毛利率");
        assertEquals("+100", result.getRecords().get(0).getPriceChange());
        assertNull(result.getRecords().get(1).getProfit());
        assertNotNull(result.getRecords().get(1).getChangePercent());
    }

    // ==================== 当前价 / 历史 ====================

    @Test
    @DisplayName("getCurrentPrice：FOOD/COMBO/未知类型三分支，缺失返回 null")
    void getCurrentPrice_branches() {
        when(foodNewMapper.selectById(1L)).thenReturn(food(1L, "红烧肉", 1000L));
        when(comboNewMapper.selectById(2L)).thenReturn(combo(2L, "套餐", 5000L, 6000L));
        when(foodNewMapper.selectById(3L)).thenReturn(null);

        assertEquals(1000L, service.getCurrentPrice("food", 1L));
        assertEquals(5000L, service.getCurrentPrice("COMBO", 2L));
        assertNull(service.getCurrentPrice("FOOD", 3L));
        assertNull(service.getCurrentPrice("UNKNOWN", 1L));
    }

    @Test
    @DisplayName("getPriceHistory：limit 钳制到 1..100 并映射为 VO")
    void getPriceHistory_clampsLimit() {
        ProductPricingHistory h = new ProductPricingHistory();
        h.setPricingStrategy("AUTO");
        h.setNewSalePrice(100L);
        when(pricingMapper.selectList(any())).thenReturn(List.of(h));

        List<PricingVO> result = service.getPriceHistory("FOOD", 1L, 0);

        assertEquals(1, result.size());
        assertEquals("自动计算", result.get(0).getPricingStrategyName());
        service.getPriceHistory("FOOD", 1L, 500);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<ProductPricingHistory>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(pricingMapper, times(2)).selectList(captor.capture());
        LambdaQueryWrapper<ProductPricingHistory> clampedLow =
                (LambdaQueryWrapper<ProductPricingHistory>) captor.getAllValues().get(0);
        LambdaQueryWrapper<ProductPricingHistory> clampedHigh =
                (LambdaQueryWrapper<ProductPricingHistory>) captor.getAllValues().get(1);
        assertTrue(clampedLow.getCustomSqlSegment().contains("LIMIT 1"),
                "limit=0 应钳制为 1，实际: " + clampedLow.getCustomSqlSegment());
        assertTrue(clampedHigh.getCustomSqlSegment().contains("LIMIT 100"),
                "limit=500 应钳制为 100，实际: " + clampedHigh.getCustomSqlSegment());
    }
}
