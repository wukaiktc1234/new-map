package com.foodtraceability.service.impl;

import com.foodtraceability.dataservice.FoodDataService;
import com.foodtraceability.dto.product.FoodCreateDTO;
import com.foodtraceability.dto.product.FoodUpdateDTO;
import com.foodtraceability.entity.Food;
import com.foodtraceability.entity.FoodCategoryNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.ComboIngredientNewMapper;
import com.foodtraceability.mapper.DishRecipeNewMapper;
import com.foodtraceability.mapper.FoodCategoryNewMapper;
import com.foodtraceability.mapper.FoodMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * P1-NEW-FOOD-LEGACY-SYNC-001 补测：syncLegacyFood 双写语义
 * 覆盖 D1（status=2 sold_out 映射）/ D2（create_by=system）/ D3（deleted=0）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FoodLegacySyncTest {

    @Mock private FoodNewMapper foodNewMapper;
    @Mock private FoodCategoryNewMapper categoryMapper;
    @Mock private FoodDataService foodDataService;
    @Mock private ComboIngredientNewMapper comboIngredientMapper;
    @Mock private DishRecipeNewMapper dishRecipeNewMapper;
    @Mock private FoodMapper foodMapper;

    private final java.util.concurrent.atomic.AtomicBoolean nullName = new java.util.concurrent.atomic.AtomicBoolean(false);
    private FoodServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FoodServiceImpl(foodNewMapper, categoryMapper, foodDataService,
                comboIngredientMapper, dishRecipeNewMapper, foodMapper, categoryMapper);
        lenient().when(foodNewMapper.insert(any(FoodNew.class))).thenReturn(1);
        lenient().when(foodNewMapper.updateById(any(FoodNew.class))).thenReturn(1);
        lenient().when(foodNewMapper.selectById(any())).thenReturn(null);
        lenient().when(foodMapper.insert(any(Food.class))).thenReturn(1);
        lenient().when(foodMapper.updateById(any(Food.class))).thenReturn(1);
        lenient().when(foodMapper.selectById(any())).thenReturn(null);
        lenient().when(categoryMapper.selectById(any())).thenAnswer(inv -> {
            FoodCategoryNew fc = new FoodCategoryNew();
            fc.setCategoryId(inv.getArgument(0));
            fc.setCategoryName(nullName.get() ? null : "蔬菜");
            fc.setStatus(1);
            return fc;
        });
        org.mockito.Mockito.lenient().doAnswer(inv -> { nullName.set(true); return null; })
                .when(categoryMapper).selectById(org.mockito.ArgumentMatchers.isNull());
        lenient().when(dishRecipeNewMapper.insert(any())).thenReturn(1);
    }

    private FoodCreateDTO createDto(int status) {
        FoodCreateDTO dto = new FoodCreateDTO();
        dto.setFoodName("QA-D" + status);
        dto.setCategoryId(745L);
        dto.setSalePrice(2500L);
        dto.setStock(50);
        dto.setStatus(status);
        return dto;
    }

    private FoodNew newFood(int status) {
        FoodNew f = new FoodNew();
        f.setFoodId(9L);
        f.setFoodCode("FDQA0001");
        f.setFoodName("QA-更新菜");
        f.setCategoryId(745L);
        f.setSalePrice(1000L);
        f.setStock(30);
        f.setStatus(status);
        return f;
    }

    /** D1 修复验证：新建在售（status=1）→ legacy food_status=active，D2/D3：create_by/update_by=system、deleted=0 */
    @Test
    void create_statusOnSale_insertsLegacyActive_withSystemAudit() {
        service.create(createDto(1));

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        Food legacy = captor.getValue();
        assertEquals("active", legacy.getFoodStatus());
        assertEquals("system", legacy.getCreateBy());
        assertEquals("system", legacy.getUpdateBy());
        assertEquals(0, legacy.getDeleted());
    }

    /** D1 修复验证：新建售罄（status=2）→ legacy food_status=sold_out（修复前落 inactive） */
    @Test
    void create_statusSoldOut_insertsLegacySoldOut() {
        service.create(createDto(2));

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        assertEquals("sold_out", captor.getValue().getFoodStatus());
    }

    /** update 路径：stock/price 变更同步到 legacy（元转换 1234 分 → 12.34） */
    @Test
    void update_stockAndPrice_syncedToLegacy() {
        lenient().when(foodNewMapper.selectById(9L)).thenReturn(newFood(1));
        Food existingLegacy = new Food();
        existingLegacy.setFoodCode("FDQA0001");
        existingLegacy.setStock(10);
        existingLegacy.setFoodPrice(new java.math.BigDecimal("10.00"));
        lenient().when(foodMapper.selectById("FDQA0001")).thenReturn(existingLegacy);

        FoodUpdateDTO dto = new FoodUpdateDTO();
        dto.setFoodName("QA-更新菜-改名");
        dto.setSalePrice(1234L);
        dto.setStock(33);
        service.update(9L, dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        assertEquals(33, captor.getValue().getStock());
        assertEquals(new java.math.BigDecimal("12.34"), captor.getValue().getFoodPrice());
        assertEquals("QA-更新菜-改名", captor.getValue().getFoodName());
    }

    /** updateStatus 路径：售罄状态同步 legacy（update 分支） */
    @Test
    void updateStatus_soldOut_syncedToLegacy() {
        lenient().when(foodNewMapper.selectById(9L)).thenReturn(newFood(1));
        Food existingLegacy = new Food();
        existingLegacy.setFoodCode("FDQA0001");
        existingLegacy.setFoodStatus("active");
        lenient().when(foodMapper.selectById("FDQA0001")).thenReturn(existingLegacy);

        service.updateStatus(9L, 2);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).updateById(captor.capture());
        assertEquals("sold_out", captor.getValue().getFoodStatus());
    }

    /** 未指定分类 → food_category 兜底"未分类"（与 sync COALESCE 同口径） */
    @Test
    void create_withoutCategory_fallsBackToUncategorized() {
        FoodCreateDTO dto = createDto(1);
        dto.setCategoryId(746L);
        nullName.set(true);
        service.create(dto);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);
        verify(foodMapper).insert(captor.capture());
        assertEquals("未分类", captor.getValue().getFoodCategory());
    }
}
