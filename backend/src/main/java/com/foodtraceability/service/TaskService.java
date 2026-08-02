package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.*;
import java.util.List;

/**
 * 任务管理服务接口
 *
 * 提供任务发布、审核、子任务管理、模板管理的完整业务能力
 */
public interface TaskService {

    /**
     * 创建并发布任务
     */
    TaskDetailVO createTask(TaskCreateDTO dto, String publisherId);

    /**
     * 获取我发布的任务列表（分页）
     */
    PageResult<TaskVO> getPublishedTasks(TaskQueryDTO queryDTO, String publisherId);

    /**
     * 获取任务详情
     */
    TaskDetailVO getTaskDetail(Long taskId, String userId);

    /**
     * 审核任务（通过/驳回）
     */
    void reviewTask(Long taskId, TaskReviewDTO dto, String reviewerId);

    /**
     * 创建子任务
     */
    SubTaskVO createSubTask(Long taskId, SubTaskCreateDTO dto, String operatorId);

    /**
     * 更新子任务
     */
    SubTaskVO updateSubTask(Long subTaskId, SubTaskUpdateDTO dto, String operatorId);

    /**
     * 删除子任务（逻辑删除）
     */
    void deleteSubTask(Long subTaskId, String operatorId);

    /**
     * 完成子任务
     */
    void completeSubTask(Long subTaskId, String operatorId);

    /**
     * 获取任务模板列表
     */
    List<TaskTemplateVO> getTemplates(String category);

    /**
     * 创建任务模板
     */
    TaskTemplateVO createTemplate(TaskTemplateCreateDTO dto, String operatorId);

    /**
     * 更新任务模板
     */
    TaskTemplateVO updateTemplate(Long templateId, TaskTemplateUpdateDTO dto, String operatorId);

    /**
     * 删除任务模板（逻辑删除）
     */
    void deleteTemplate(Long templateId, String operatorId);
}
