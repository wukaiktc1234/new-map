package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.ScheduledTaskCreateDTO;
import com.foodtraceability.dto.ScheduledTaskQueryDTO;
import com.foodtraceability.dto.ScheduledTaskUpdateDTO;
import com.foodtraceability.entity.ScheduledTask;
import com.foodtraceability.entity.TaskExecutionLog;
import com.foodtraceability.entity.TaskExecutionLogDetail;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理 Service 接口
 * 提供任务的完整生命周期管理和执行日志查询
 */
public interface ScheduledTaskService {

    // ==================== 基础CRUD ====================

    /**
     * 根据ID查询任务详情
     * @param taskId 任务ID
     * @return 任务实体，不存在返回null
     */
    ScheduledTask getById(Long taskId);

    /**
     * 根据任务编码查询任务
     * @param taskCode 任务编码
     * @return 任务实体，不存在返回null
     */
    ScheduledTask getByTaskCode(String taskCode);

    /**
     * 查询所有任务列表（不分页）
     * @return 任务列表
     */
    List<ScheduledTask> list();

    /**
     * 分页查询任务列表
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<ScheduledTask> page(Page<ScheduledTask> page, ScheduledTaskQueryDTO query);

    /**
     * 创建新任务
     * @param createDTO 创建请求
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 创建后的任务实体
     */
    ScheduledTask create(ScheduledTaskCreateDTO createDTO, Long userId, String username);

    /**
     * 更新任务信息
     * @param taskId 任务ID
     * @param updateDTO 更新请求
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean update(Long taskId, ScheduledTaskUpdateDTO updateDTO, Long userId, String username);

    /**
     * 逻辑删除任务
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean delete(Long taskId);

    // ==================== 生命周期控制 ====================

    /**
     * 启用任务（CREATED/DISABLED -> ENABLED）
     * @param taskId 任务ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean enable(Long taskId, Long userId, String username);

    /**
     * 禁用任务（ENABLED/PAUSED/ERROR -> DISABLED）
     * @param taskId 任务ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean disable(Long taskId, Long userId, String username);

    /**
     * 暂停任务（ENABLED -> PAUSED）
     * @param taskId 任务ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean pause(Long taskId, Long userId, String username);

    /**
     * 恢复任务（PAUSED -> ENABLED）
     * @param taskId 任务ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 是否成功
     */
    boolean resume(Long taskId, Long userId, String username);

    // ==================== 执行日志 ====================

    /**
     * 获取任务的执行日志分页列表
     * @param page 分页参数
     * @param taskId 任务ID
     * @param triggerType 触发类型（可选）
     * @param executionStatus 执行状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 日志分页结果
     */
    IPage<TaskExecutionLog> getExecutionLogs(Page<TaskExecutionLog> page, Long taskId,
                                              Integer triggerType, Integer executionStatus,
                                              java.time.LocalDateTime startTime,
                                              java.time.LocalDateTime endTime);

    /**
     * 获取执行日志的步骤详情列表
     * @param logId 日志ID
     * @return 详情列表
     */
    List<TaskExecutionLogDetail> getExecutionDetails(Long logId);

    /**
     * 根据ID获取执行日志
     * @param logId 日志ID
     * @return 执行日志实体，不存在返回null
     */
    TaskExecutionLog getExecutionLogById(Long logId);

    /**
     * 获取任务最近一次执行日志
     * @param taskId 任务ID
     * @return 最近一次日志，无则返回null
     */
    TaskExecutionLog getLatestLog(Long taskId);

    // ==================== 统计仪表盘 ====================

    /**
     * 获取任务统计概览数据
     * @return 统计Map: totalCount, enabledCount, pausedCount, errorCount, disabledCount...
     */
    Map<String, Object> getStatistics();

    /**
     * 获取仪表盘聚合数据（含日志统计、运行中任务等）
     * @return 仪表盘数据
     */
    Map<String, Object> getDashboardData();

    // ==================== 批量操作 ====================

    /**
     * 批量启用任务
     * @param taskIds 任务ID列表
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 成功数量
     */
    int batchEnable(List<Long> taskIds, Long userId, String username);

    /**
     * 批量禁用任务
     * @param taskIds 任务ID列表
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 成功数量
     */
    int batchDisable(List<Long> taskIds, Long userId, String username);

    /**
     * 批量删除任务（逻辑删除）
     * @param taskIds 任务ID列表
     * @return 成功数量
     */
    int batchDelete(List<Long> taskIds);

    // ==================== 辅助查询 ====================

    /**
     * 获取所有任务分组列表
     * @return 分组名称列表
     */
    List<String> getTaskGroups();

    /**
     * 获取所有内置任务列表
     * @return 内置任务列表
     */
    List<ScheduledTask> getBuiltinTasks();
}
