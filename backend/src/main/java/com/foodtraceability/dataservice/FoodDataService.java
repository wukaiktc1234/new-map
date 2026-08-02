package com.foodtraceability.dataservice;

import com.foodtraceability.dto.product.FoodBasicInfo;
import java.util.List;
import java.util.Map;

/**
 * 菜品数据服务接口
 * 提供菜品数据的二级缓存(L1本地+L2 Redis)和批量查询功能
 */
public interface FoodDataService {

    /**
     * 批量获取菜品基本信息
     * @param foodIds 菜品ID列表
     * @return 菜品ID到基本信息的映射
     */
    Map<Long, FoodBasicInfo> batchGetFoodBasicInfo(List<Long> foodIds);

    /**
     * 获取单个菜品基本信息
     * @param foodId 菜品ID
     * @return 菜品基本信息
     */
    FoodBasicInfo getFoodBasicInfo(Long foodId);

    /**
     * 清除指定菜品的缓存
     * @param foodId 菜品ID
     */
    void clearFoodCache(Long foodId);

    /**
     * 批量清除菜品缓存
     * @param foodIds 菜品ID列表
     */
    void clearFoodBatchCache(List<Long> foodIds);

    /**
     * 清除所有菜品缓存
     */
    void clearAllFoodCache();
}
