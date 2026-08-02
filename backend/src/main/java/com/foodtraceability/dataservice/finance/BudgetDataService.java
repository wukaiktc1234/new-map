package com.foodtraceability.dataservice.finance;

import com.foodtraceability.dto.finance.BudgetBasicInfo;

import java.util.List;
import java.util.Map;

/**
 * 预算 DataService 接口
 * 提供 L1 Caffeine（本地）+ L2 Redis（分布式）二级缓存
 * 缓存键格式: budget:basic:{budgetId}
 */
public interface BudgetDataService {

    /**
     * 批量获取预算基本信息（缓存优先，避免 N+1 查询）
     * @param budgetIds 预算ID列表
     * @return ID -> BasicInfo 映射，未命中的ID不出现在Map中
     */
    Map<String, BudgetBasicInfo> batchGetBudgetBasicInfo(List<String> budgetIds);

    /**
     * 单个获取预算基本信息
     * @param budgetId 预算ID
     * @return BasicInfo，不存在返回 null
     */
    BudgetBasicInfo getBudgetBasicInfo(String budgetId);

    /**
     * 清除单个预算缓存（先清 L1，再清 L2）
     * @param budgetId 预算ID
     */
    void clearBudgetCache(String budgetId);

    /**
     * 批量清除预算缓存
     * @param budgetIds 预算ID列表
     */
    void clearBudgetBatchCache(List<String> budgetIds);

    /**
     * 清除所有预算缓存
     */
    void clearAllBudgetCache();
}
