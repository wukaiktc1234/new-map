package com.foodtraceability.service;

import com.foodtraceability.dto.operations.ExportTaskDTO;
import com.foodtraceability.entity.report.ExportTask;

import java.util.List;

/**
 * 导出任务服务接口
 * 提供报表导出任务的管理
 */
public interface ExportTaskService {

    /**
     * 创建导出任务
     * @param task 导出任务实体
     * @return 创建后的任务
     */
    ExportTask createTask(ExportTask task);

    /**
     * 根据ID获取任务
     * @param taskId 任务ID
     * @return 任务实体
     */
    ExportTask getTaskById(Long taskId);

    /**
     * 根据任务编号获取任务
     * @param taskNo 任务编号
     * @return 任务实体
     */
    ExportTask getTaskByNo(String taskNo);

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 状态
     * @param errorMessage 错误信息
     */
    void updateTaskStatus(Long taskId, Integer status, String errorMessage);

    /**
     * 完成任务
     * @param taskId 任务ID
     * @param filePath 文件路径
     * @param fileSize 文件大小
     * @param rowCount 数据行数
     */
    void completeTask(Long taskId, String filePath, Long fileSize, Integer rowCount);

    /**
     * 获取用户的任务列表
     * @param userId 用户ID
     * @param status 状态（可选）
     * @return 任务列表
     */
    List<ExportTaskDTO> getUserTasks(Long userId, Integer status);

    /**
     * 删除任务
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void deleteTask(Long taskId, Long userId);

    /**
     * 生成任务编号
     * @return 任务编号
     */
    String generateTaskNo();
}
