package com.foodtraceability.dataservice;

import com.foodtraceability.dto.store.operation.vo.PendingTaskVO;

import java.util.List;
import java.util.Map;

/**
 * 待办任务数据服务接口
 * 提供待办任务的二级缓存(L1本地+L2 Redis)和批量查询功能
 * 缓存键格式: pending_task:basic:{taskId}
 */
public interface PendingTaskDataService {

    /**
     * 批量获取待办任务基本信息
     *
     * @param taskIds 任务ID列表
     * @return 任务ID到视图对象的映射
     */
    Map<String, PendingTaskVO> batchGetPendingTaskBasicInfo(List<String> taskIds);

    /**
     * 获取单个待办任务基本信息
     *
     * @param taskId 任务ID
     * @return 待办任务视图对象
     */
    PendingTaskVO getPendingTaskBasicInfo(String taskId);

    /**
     * 清除指定任务的缓存
     *
     * @param taskId 任务ID
     */
    void clearPendingTaskCache(String taskId);

    /**
     * 批量清除任务缓存
     *
     * @param taskIds 任务ID列表
     */
    void clearPendingTaskBatchCache(List<String> taskIds);

    /**
     * 清除所有待办任务缓存
     */
    void clearAllPendingTaskCache();
}
