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
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.ComboIngredientNewMapper;
import com.foodtraceability.mapper.DishComboNewMapper;
import com.foodtraceability.mapper.FoodCategoryMapper;
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

    public PosApiServiceImpl(FoodMapper foodMapper,
                              DishComboNewMapper dishComboNewMapper,
                              ComboIngredientNewMapper comboIngredientNewMapper,
                              FoodCategoryMapper foodCategoryMapper,
                              FoodNewMapper foodNewMapper) {
        this.foodMapper = foodMapper;
        this.dishComboNewMapper = dishComboNewMapper;
        this.comboIngredientNewMapper = comboIngredientNewMapper;
        this.foodCategoryMapper = foodCategoryMapper;
        this.foodNewMapper = foodNewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosCategoryDTO> getCategories() {
        List<FoodCategory> categories = foodCategoryMapper.selectList(
                new LambdaQueryWrapper<FoodCategory>()
                        .and(wrapper -> wrapper.eq(FoodCategory::getStatus, "1").or().eq(FoodCategory::getStatus, "active"))
                        .orderByAsc(FoodCategory::getSortOrder));
        return categories.stream().map(c -> {
            PosCategoryDTO dto = new PosCategoryDTO();
            dto.setCategoryId(c.getCategoryCode());
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
        List<FoodCategory> categories = foodCategoryMapper.selectList(null);
        Map<String, String> categoryMap = categories.stream()
                .collect(Collectors.toMap(FoodCategory::getCategoryCode, FoodCategory::getCategoryName, (a, b) -> a));
        return foods.stream().map(f -> {
            PosDishDTO dto = new PosDishDTO();
            dto.setDishId(f.getFoodCode());
            dto.setDishCode(f.getFoodCode());
            dto.setDishName(f.getFoodName());
            dto.setCategoryId(f.getFoodCategory());
            dto.setCategoryName(categoryMap.getOrDefault(f.getFoodCategory(), "其他"));
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
        List<Food> foods = foodMapper.selectList(
                new LambdaQueryWrapper<Food>()
                        .and(wrapper -> wrapper.eq(Food::getFoodStatus, "1").or().eq(Food::getFoodStatus, "active"))
                        .eq(Food::getFoodCategory, categoryId));
        FoodCategory category = foodCategoryMapper.selectOne(
                new LambdaQueryWrapper<FoodCategory>().eq(FoodCategory::getCategoryCode, categoryId));
        String categoryName = category != null ? category.getCategoryName() : "其他";
        return foods.stream().map(f -> {
            PosDishDTO dto = new PosDishDTO();
            dto.setDishId(f.getFoodCode());
            dto.setDishCode(f.getFoodCode());
            dto.setDishName(f.getFoodName());
            dto.setCategoryId(f.getFoodCategory());
            dto.setCategoryName(categoryName);
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
