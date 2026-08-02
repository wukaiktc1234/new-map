package com.foodtraceability.dataservice.schedule;

import com.foodtraceability.dto.schedule.SchedulePlanVO;

import java.util.List;
import java.util.Map;

/**
 * 排班方案数据服务接口
 * 提供排班方案数据的缓存和批量查询功能
 *
 * <p>缓存键格式: schedule-plan:basic:{planId}
 */
public interface SchedulePlanDataService {

    /**
     * 批量获取排班方案基本信息
     * @param planIds 方案ID列表
     * @return 方案ID到基本信息的映射
     */
    Map<String, SchedulePlanVO> batchGetPlanBasicInfo(List<String> planIds);

    /**
     * 获取单个排班方案基本信息
     * @param planId 方案ID
     * @return 方案基本信息
     */
    SchedulePlanVO getPlanBasicInfo(String planId);

    /**
     * 获取门店所有方案(从缓存或数据库)
     * @param storeId 门店ID
     * @return 方案列表(按更新时间倒序)
     */
    List<SchedulePlanVO> getPlansByStore(Long storeId);

    /**
     * 清除指定方案的缓存
     * @param planId 方案ID
     */
    void clearPlanCache(String planId);

    /**
     * 批量清除方案缓存
     * @param planIds 方案ID列表
     */
    void clearPlanBatchCache(List<String> planIds);

    /**
     * 清除门店下所有方案的缓存
     * @param storeId 门店ID
     */
    void clearStorePlanCache(Long storeId);
}
