package com.foodtraceability.dataservice.impl;

import com.foodtraceability.dataservice.PendingTaskDataService;
import com.foodtraceability.dto.store.operation.vo.PendingTaskVO;
import com.foodtraceability.entity.PendingTask;
import com.foodtraceability.mapper.PendingTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 待办任务数据服务实现类
 * 实现二级缓存：L1 Caffeine(本地) + L2 Redis
 * 缓存键格式: pending_task:basic:{taskId}
 */
@Service
public class PendingTaskDataServiceImpl implements PendingTaskDataService {

    private static final Logger log = LoggerFactory.getLogger(PendingTaskDataServiceImpl.class);

    /** 缓存名称 */
    private static final String CACHE_NAME = "pendingTask";
    /** 缓存过期时间：24小时（基本信息变更频率低） */
    private static final long CACHE_EXPIRE_SECONDS = 86400;

    private final PendingTaskMapper pendingTaskMapper;

    /**
     * 构造函数注入
     *
     * @param pendingTaskMapper 待办任务Mapper
     */
    public PendingTaskDataServiceImpl(PendingTaskMapper pendingTaskMapper) {
        this.pendingTaskMapper = pendingTaskMapper;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#taskIds", unless = "#result == null || #result.isEmpty()")
    public Map<String, PendingTaskVO> batchGetPendingTaskBasicInfo(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 使用批量IN查询获取任务列表
        List<PendingTask> tasks = pendingTaskMapper.selectBatchIds(taskIds);
        return tasks.stream()
                .filter(task -> task != null)
                .collect(Collectors.toMap(
                        PendingTask::getTaskId,
                        this::convertToVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'basic:' + #taskId", unless = "#result == null")
    public PendingTaskVO getPendingTaskBasicInfo(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return null;
        }

        PendingTask task = pendingTaskMapper.selectById(taskId);
        return convertToVO(task);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "'basic:' + #taskId")
    public void clearPendingTaskCache(String taskId) {
        log.debug("清除待办任务缓存: taskId={}", taskId);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearPendingTaskBatchCache(List<String> taskIds) {
        log.debug("批量清除待办任务缓存: count={}", taskIds != null ? taskIds.size() : 0);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearAllPendingTaskCache() {
        log.info("清除所有待办任务缓存");
    }

    /**
     * 将实体转换为视图对象
     *
     * @param task 待办任务实体
     * @return 视图对象
     */
    private PendingTaskVO convertToVO(PendingTask task) {
        if (task == null) {
            return null;
        }

        PendingTaskVO vo = new PendingTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskType(task.getTaskType());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setPriority(task.getPriority());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setAssigneeRole(task.getAssigneeRole());
        vo.setSourceType(task.getSourceType());
        vo.setSourceId(task.getSourceId());
        vo.setRedirectUrl(task.getRedirectUrl());
        vo.setStatus(task.getStatus());
        vo.setDueDate(task.getDueDate());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreatedBy(task.getCreatedBy());
        vo.setCreateTime(task.getCreateTime());

        // 计算是否过期
        if (task.getDueDate() != null && !"completed".equals(task.getStatus())
                && !"cancelled".equals(task.getStatus())) {
            vo.setOverdue(task.getDueDate().isBefore(LocalDate.now()));
        } else {
            vo.setOverdue(false);
        }

        // 设置显示名称（由Service层或前端处理，此处仅保留基础转换）
        vo.setTaskTypeName(getTaskTypeName(task.getTaskType()));
        vo.setPriorityName(getPriorityName(task.getPriority()));
        vo.setStatusName(getStatusName(task.getStatus()));
        vo.setSourceTypeName(getSourceTypeName(task.getSourceType()));

        return vo;
    }

    /** 获取任务类型名称 */
    private String getTaskTypeName(String taskType) {
        if (taskType == null) return "未知";
        switch (taskType) {
            case "approval": return "审批任务";
            case "inspection": return "巡检任务";
            case "inventory": return "盘点任务";
            case "remind": return "提醒事项";
            case "report": return "报表任务";
            default: return taskType;
        }
    }

    /** 获取优先级名称 */
    private String getPriorityName(Integer priority) {
        if (priority == null) return "未知";
        switch (priority) {
            case 1: return "低";
            case 2: return "中";
            case 3: return "高";
            case 4: return "紧急";
            default: return "未知";
        }
    }

    /** 获取状态名称 */
    private String getStatusName(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "pending": return "待处理";
            case "in_progress": return "进行中";
            case "completed": return "已完成";
            case "cancelled": return "已取消";
            case "expired": return "已过期";
            default: return status;
        }
    }

    /** 获取来源类型名称 */
    private String getSourceTypeName(String sourceType) {
        if (sourceType == null) return "未知";
        switch (sourceType) {
            case "order": return "订单";
            case "purchase": return "采购订单";
            case "recruitment": return "招聘审批";
            case "inventory": return "库存管理";
            case "finance": return "财务相关";
            default: return sourceType;
        }
    }
}
