package com.foodtraceability.dataservice.impl;

import com.foodtraceability.dataservice.FoodDataService;
import com.foodtraceability.dto.product.FoodBasicInfo;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.FoodNewMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜品数据服务实现类
 * 实现二级缓存：L1 Caffeine(本地) + L2 Redis
 * 缓存键格式: food:basic:{foodId}
 */
@Service
public class FoodDataServiceImpl implements FoodDataService {

    private static final Logger log = LoggerFactory.getLogger(FoodDataServiceImpl.class);
    /** 缓存名称 */
    private static final String CACHE_NAME = "food";
    /** 缓存过期时间：24小时（基本信息变更频率低） */
    private static final long CACHE_EXPIRE_SECONDS = 86400;

    private final FoodNewMapper foodNewMapper;

    public FoodDataServiceImpl(FoodNewMapper foodNewMapper) {
        this.foodNewMapper = foodNewMapper;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#foodIds", unless = "#result == null || #result.isEmpty()")
    public Map<Long, FoodBasicInfo> batchGetFoodBasicInfo(List<Long> foodIds) {
        if (foodIds == null || foodIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<FoodNew> foods = foodNewMapper.selectBatchIds(foodIds);
        return foods.stream()
                .filter(food -> food != null)
                .collect(Collectors.toMap(
                        FoodNew::getFoodId,
                        this::convertToBasicInfo,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'basic:' + #foodId", unless = "#result == null")
    public FoodBasicInfo getFoodBasicInfo(Long foodId) {
        if (foodId == null) {
            return null;
        }

        FoodNew food = foodNewMapper.selectById(foodId);
        return convertToBasicInfo(food);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "'basic:' + #foodId")
    public void clearFoodCache(Long foodId) {
        log.debug("清除菜品缓存: foodId={}", foodId);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearFoodBatchCache(List<Long> foodIds) {
        log.debug("批量清除菜品缓存: count={}", foodIds != null ? foodIds.size() : 0);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearAllFoodCache() {
        log.info("清除所有菜品缓存");
    }

    /**
     * 将实体转换为基本信息DTO
     */
    private FoodBasicInfo convertToBasicInfo(FoodNew food) {
        if (food == null) {
            return null;
        }

        FoodBasicInfo info = new FoodBasicInfo();
        info.setFoodId(food.getFoodId());
        info.setFoodCode(food.getFoodCode());
        info.setFoodName(food.getFoodName());
        info.setCategoryId(food.getCategoryId());
        info.setSpecification(food.getSpecification());
        info.setUnit(food.getUnit());
        info.setSalePrice(food.getSalePrice());
        info.setStatus(food.getStatus());
        info.setImageUrl(food.getImageUrl());

        return info;
    }
}
