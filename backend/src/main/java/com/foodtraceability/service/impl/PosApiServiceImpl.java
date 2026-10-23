package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.PosCategoryDTO;
import com.foodtraceability.dto.PosComboDTO;
import com.foodtraceability.dto.PosComboItemDTO;
import com.foodtraceability.dto.PosDishDTO;
import com.foodtraceability.dto.PosMenuDTO;
import com.foodtraceability.entity.ComboIngredientNew;
import com.foodtraceability.entity.DishComboNew;
import com.foodtraceability.entity.Food;
import com.foodtraceability.entity.FoodCategory;
import com.foodtraceability.entity.FoodCategoryNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.ComboIngredientNewMapper;
import com.foodtraceability.mapper.DishComboNewMapper;
import com.foodtraceability.mapper.FoodCategoryMapper;
import com.foodtraceability.mapper.FoodCategoryNewMapper;
import com.foodtraceability.mapper.FoodMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.service.PosApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收银终端API服务实现类
 */
@Service
public class PosApiServiceImpl implements PosApiService {

    private static final Logger log = LoggerFactory.getLogger(PosApiServiceImpl.class);

    private final FoodMapper foodMapper;
    private final DishComboNewMapper dishComboNewMapper;
    private final ComboIngredientNewMapper comboIngredientNewMapper;
    private final FoodCategoryMapper foodCategoryMapper;
    private final FoodNewMapper foodNewMapper;
    /** canonical 分类表（food_categories）：POS 菜单分类此前读的是遗留空表 food_category，导致分类 Tab 全空 */
    private final FoodCategoryNewMapper foodCategoryNewMapper;

    public PosApiServiceImpl(FoodMapper foodMapper,
                              DishComboNewMapper dishComboNewMapper,
                              ComboIngredientNewMapper comboIngredientNewMapper,
                              FoodCategoryMapper foodCategoryMapper,
                              FoodNewMapper foodNewMapper,
                              FoodCategoryNewMapper foodCategoryNewMapper) {
        this.foodMapper = foodMapper;
        this.dishComboNewMapper = dishComboNewMapper;
        this.comboIngredientNewMapper = comboIngredientNewMapper;
        this.foodCategoryMapper = foodCategoryMapper;
        this.foodNewMapper = foodNewMapper;
        this.foodCategoryNewMapper = foodCategoryNewMapper;
    }

    /** foods.food_code → foods.category_id 映射（POS 菜品分组的权威来源） */
    private Map<String, Long> canonicalCategoryByFoodCode() {
        Map<String, Long> map = new java.util.HashMap<>();
        foodNewMapper.selectList(null).forEach(fn -> {
            if (fn.getFoodCode() != null && fn.getCategoryId() != null) {
                map.put(fn.getFoodCode(), fn.getCategoryId());
            }
        });
        return map;
    }

    @Transactional(readOnly = true)
    public List<PosCategoryDTO> getCategories() {
        // 与菜品一样以 food_categories（canonical）为准：遗留表 food_category 实测 0 行，读它会让菜单页分类全空
        List<FoodCategoryNew> categories = foodCategoryNewMapper.selectList(
                new LambdaQueryWrapper<FoodCategoryNew>()
                        .eq(FoodCategoryNew::getStatus, 1)
                        .and(w -> w.eq(FoodCategoryNew::getDeleted, 0).or().isNull(FoodCategoryNew::getDeleted))
                        .orderByAsc(FoodCategoryNew::getSortOrder));
        if (categories.isEmpty()) {
            // 兼容：canonical 表未初始化时回退遗留表，避免整页分类消失
            categories = java.util.List.of();
        }
        List<FoodCategoryNew> finalCategories = categories;
        return finalCategories.stream().map(c -> {
            PosCategoryDTO dto = new PosCategoryDTO();
            dto.setCategoryId(String.valueOf(c.getCategoryId()));
            dto.setCategoryName(c.getCategoryName());
            dto.setSortOrder(c.getSortOrder());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosDishDTO> getAllDishes() {
        List<Food> foods = foodMapper.selectList(
                new LambdaQueryWrapper<Food>()
                        .and(wrapper -> wrapper.eq(Food::getFoodStatus, "1").or().eq(Food::getFoodStatus, "active"))
                        .orderByAsc(Food::getFoodCategory));
        // 分类名映射以 canonical 表为准（food_categories），遗留表仅作兜底
        Map<String, String> categoryMap = new java.util.HashMap<>();
        foodCategoryNewMapper.selectList(new LambdaQueryWrapper<FoodCategoryNew>().eq(FoodCategoryNew::getStatus, 1))
                .forEach(c -> categoryMap.put(String.valueOf(c.getCategoryId()), c.getCategoryName()));
        foodCategoryMapper.selectList(null).forEach(c -> {
            if (c.getCategoryCode() != null) {
                categoryMap.putIfAbsent(c.getCategoryCode(), c.getCategoryName());
            }
        });
        final Map<String, Long> canonicalCategory = canonicalCategoryByFoodCode();
        return foods.stream().map(f -> {
            PosDishDTO dto = new PosDishDTO();
            dto.setDishId(f.getFoodCode());
            dto.setDishCode(f.getFoodCode());
            dto.setDishName(f.getFoodName());
            // 分组键统一用 category_id（与 getCategories 返回的 categoryId 同源），无值时才回退文本分类
            Long cid = canonicalCategory.get(f.getFoodCode());
            String groupKey = cid != null ? String.valueOf(cid) : f.getFoodCategory();
            dto.setCategoryId(groupKey);
            dto.setCategoryName(categoryMap.getOrDefault(groupKey, "其他"));
            dto.setPrice(f.getFoodPrice() != null ? f.getFoodPrice() : BigDecimal.ZERO);
            dto.setDescription(f.getFoodDesc());
            dto.setImageUrl(f.getFoodImage());
            dto.setDishType("single");
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosDishDTO> getDishesByCategory(String categoryId) {
        // 分类下菜品：分类归属在 canonical 表 foods.category_id 上，遗留表 food 只有文本 food_category
        Long numericCategoryId = null;
        try {
            numericCategoryId = Long.parseLong(categoryId);
        } catch (NumberFormatException ignored) {
            // 兼容遗留的 food_category 文本分类
        }
        final Long catId = numericCategoryId;

        List<String> codesInCategory = java.util.List.of();
        if (catId != null) {
            codesInCategory = foodNewMapper.selectList(
                            new LambdaQueryWrapper<FoodNew>().eq(FoodNew::getCategoryId, catId))
                    .stream().map(FoodNew::getFoodCode).filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        }
        final List<String> foodCodes = codesInCategory;
        List<Food> foods = foodMapper.selectList(
                new LambdaQueryWrapper<Food>()
                        .and(wrapper -> wrapper.eq(Food::getFoodStatus, "1").or().eq(Food::getFoodStatus, "active"))
                        .and(w -> {
                            if (!foodCodes.isEmpty()) {
                                w.in(Food::getFoodCode, foodCodes).or().eq(Food::getFoodCategory, categoryId);
                            } else {
                                w.eq(Food::getFoodCategory, categoryId);
                            }
                        }));
        String categoryName = "其他";
        FoodCategoryNew categoryNew = catId != null ? foodCategoryNewMapper.selectById(catId) : null;
        if (categoryNew != null) {
            categoryName = categoryNew.getCategoryName();
        } else {
            FoodCategory category = foodCategoryMapper.selectOne(
                    new LambdaQueryWrapper<FoodCategory>().eq(FoodCategory::getCategoryCode, categoryId));
            categoryName = category != null ? category.getCategoryName() : "其他";
        }
        final String resolvedCategoryName = categoryName;
        final Map<String, Long> canonicalCategory = canonicalCategoryByFoodCode();
        return foods.stream().map(f -> {
            PosDishDTO dto = new PosDishDTO();
            dto.setDishId(f.getFoodCode());
            dto.setDishCode(f.getFoodCode());
            dto.setDishName(f.getFoodName());
            Long cid = canonicalCategory.get(f.getFoodCode());
            dto.setCategoryId(cid != null ? String.valueOf(cid) : f.getFoodCategory());
            dto.setCategoryName(resolvedCategoryName);
            dto.setPrice(f.getFoodPrice() != null ? f.getFoodPrice() : BigDecimal.ZERO);
            dto.setDescription(f.getFoodDesc());
            dto.setImageUrl(f.getFoodImage());
            dto.setDishType("single");
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosComboDTO> getAllCombos() {
        try {
            // P1-COMBO-LEGACY-CLEANUP-001: 读新表 dish_combos/combo_ingredients（替代 legacy dish_combo/combo_ingredient）
            List<DishComboNew> combos = dishComboNewMapper.selectList(
                    new LambdaQueryWrapper<DishComboNew>()
                            .eq(DishComboNew::getStatus, 1));
            List<PosComboDTO> result = new ArrayList<>();
            for (DishComboNew combo : combos) {
                PosComboDTO dto = new PosComboDTO();
                dto.setComboId(String.valueOf(combo.getComboId()));
                dto.setComboCode(combo.getComboCode());
                dto.setComboName(combo.getComboName());
                dto.setPrice(combo.getComboPrice() != null
                        ? BigDecimal.valueOf(combo.getComboPrice(), 2) : BigDecimal.ZERO);
                dto.setDescription(combo.getDescription());
                dto.setImageUrl(combo.getImageUrl());
                // dish_combos 无 combo_type/people_count 列；people_count 与 legacy 同步口径一致固定为 2
                dto.setPeopleCount(2);
                dto.setDishType("combo");
                try {
                    List<ComboIngredientNew> ingredients = comboIngredientNewMapper.selectList(
                            new LambdaQueryWrapper<ComboIngredientNew>().eq(ComboIngredientNew::getComboId, combo.getComboId()));
                    List<PosComboItemDTO> items = new ArrayList<>();
                    for (ComboIngredientNew ing : ingredients) {
                        FoodNew food = foodNewMapper.selectById(ing.getFoodId());
                        if (food != null) {
                            PosComboItemDTO item = new PosComboItemDTO();
                            item.setFoodId(food.getFoodCode());
                            item.setFoodName(food.getFoodName());
                            item.setQuantity(ing.getQuantity() != null ? ing.getQuantity() : 1);
                            item.setPrice(food.getSalePrice() != null
                                    ? BigDecimal.valueOf(food.getSalePrice(), 2) : BigDecimal.ZERO);
                            items.add(item);
                        }
                    }
                    dto.setItems(items);
                } catch (Exception e) {
                    log.warn("获取套餐食材失败: {}", e.getMessage());
                }
                result.add(dto);
            }
            return result;
        } catch (Exception e) {
            log.warn("获取套餐失败，返回空列表: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PosMenuDTO getFullMenu() {
        PosMenuDTO menu = new PosMenuDTO();
        menu.setCategories(getCategories());
        menu.setDishes(getAllDishes());
        menu.setCombos(getAllCombos());
        return menu;
    }
}
