package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.SubTask;
import com.foodtraceability.entity.Task;
import com.foodtraceability.entity.TaskTemplate;
import com.foodtraceability.mapper.SubTaskMapper;
import com.foodtraceability.mapper.TaskMapper;
import com.foodtraceability.mapper.TaskTemplateMapper;
import com.foodtraceability.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务管理服务实现类
 *
 * 提供任务发布、审核、子任务CRUD、模板管理的完整业务逻辑
 * 待补充：权限校验、审核日志、接收人姓名解析等增强逻辑
 */
@Service
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final SubTaskMapper subTaskMapper;
    private final TaskTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入依赖
     * 同时补充 ObjectMapper 字段（修复预存在 bug：原代码使用 objectMapper 但未声明）
     */
    public TaskServiceImpl(TaskMapper taskMapper, SubTaskMapper subTaskMapper,
                            TaskTemplateMapper templateMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.subTaskMapper = subTaskMapper;
        this.templateMapper = templateMapper;
        this.objectMapper = objectMapper;
    }

    // ==================== 任务发布与查询 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDetailVO createTask(TaskCreateDTO dto, String publisherId) {
        // 1. 构建任务实体
        Task task = new Task();
        task.setRefNo(generateRefNo(dto.getCategory()));
        task.setCategory(dto.getCategory());
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setPublisherId(publisherId);
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());
        task.setStatus("pending");
        // 根据类别设置初始工作流阶段
        task.setCurrentStage(getInitialStage(dto.getCategory()));
        task.setProgress(0);
        task.setSubTasksCount(0);
        task.setCompletedSubTasks(0);
        // 使用ObjectMapper序列化接收人ID列表为JSON
        try {
            task.setAssigneeIds(objectMapper.writeValueAsString(dto.getAssigneeIds()));
        } catch (Exception e) {
            throw new RuntimeException("序列化接收人ID列表失败", e);
        }

        // 2. 保存任务
        taskMapper.insert(task);

        // 3. 如果有初始子任务，批量创建
        if (dto.getSubTasks() != null && !dto.getSubTasks().isEmpty()) {
            for (int i = 0; i < dto.getSubTasks().size(); i++) {
                SubTaskCreateDTO subDto = dto.getSubTasks().get(i);
                SubTask subTask = new SubTask();
                subTask.setTaskId(task.getTaskId());
                subTask.setTitle(subDto.getTitle());
                subTask.setDescription(subDto.getDescription());
                subTask.setSortOrder(subDto.getSortOrder() != null ? subDto.getSortOrder() : i);
                subTask.setStatus("pending");
                subTaskMapper.insert(subTask);
            }
            task.setSubTasksCount(dto.getSubTasks().size());
            taskMapper.updateById(task);
        }

        return getTaskDetail(task.getTaskId(), publisherId);
    }

    @Override
    public PageResult<TaskVO> getPublishedTasks(TaskQueryDTO queryDTO, String publisherId) {
        Page<Task> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getPublisherId, publisherId)
                .orderByDesc(Task::getCreateTime);

        // 可选筛选条件
        if (queryDTO.getCategory() != null && !queryDTO.getCategory().isEmpty()) {
            wrapper.eq(Task::getCategory, queryDTO.getCategory());
        }
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            wrapper.eq(Task::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getPriority() != null) {
            wrapper.eq(Task::getPriority, queryDTO.getPriority());
        }
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.like(Task::getTitle, queryDTO.getKeyword());
        }

        Page<Task> result = taskMapper.selectPage(page, wrapper);

        PageResult<TaskVO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        pageResult.setTotal(result.getTotal());
        pageResult.setCurrent((long) queryDTO.getCurrent());
        pageResult.setSize((long) queryDTO.getSize());
        return pageResult;
    }

    @Override
    public TaskDetailVO getTaskDetail(Long taskId, String userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        // TODO: 权限校验 - 发布人/接收人/审核人可查看

        return convertToDetailVO(task);
    }

    // ==================== 审核 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewTask(Long taskId, TaskReviewDTO dto, String reviewerId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (!"reviewing".equals(task.getStatus())) {
            throw new RuntimeException("当前状态不允许审核，当前状态: " + task.getStatus());
        }

        // 更新状态
        if ("approve".equals(dto.getAction())) {
            task.setStatus("completed");
            task.setProgress(100);
        } else if ("reject".equals(dto.getAction())) {
            task.setStatus("rejected");
            // 驳回后回到进行中，可重新提交
            task.setCurrentStage("execute");
        } else {
            throw new RuntimeException("不支持的审核动作: " + dto.getAction());
        }

        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        // TODO: 记录审核日志
    }

    // ==================== 子任务 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubTaskVO createSubTask(Long taskId, SubTaskCreateDTO dto, String operatorId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        SubTask subTask = new SubTask();
        subTask.setTaskId(taskId);
        subTask.setTitle(dto.getTitle());
        subTask.setDescription(dto.getDescription());
        subTask.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        subTask.setStatus("pending");
        subTaskMapper.insert(subTask);

        // 更新父任务的子任务计数
        task.setSubTasksCount(task.getSubTasksCount() + 1);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        return convertToSubTaskVO(subTask);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubTaskVO updateSubTask(Long subTaskId, SubTaskUpdateDTO dto, String operatorId) {
        SubTask subTask = subTaskMapper.selectById(subTaskId);
        if (subTask == null) {
            throw new RuntimeException("子任务不存在");
        }

        subTask.setTitle(dto.getTitle());
        subTask.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) {
            subTask.setSortOrder(dto.getSortOrder());
        }
        subTask.setUpdateTime(LocalDateTime.now());
        subTaskMapper.updateById(subTask);

        return convertToSubTaskVO(subTask);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubTask(Long subTaskId, String operatorId) {
        SubTask subTask = subTaskMapper.selectById(subTaskId);
        if (subTask == null) {
            throw new RuntimeException("子任务不存在");
        }
        if ("completed".equals(subTask.getStatus())) {
            throw new RuntimeException("已完成的子任务不能删除");
        }

        subTaskMapper.deleteById(subTaskId);

        // 更新父任务的子任务计数
        Task task = taskMapper.selectById(subTask.getTaskId());
        if (task != null) {
            int newCount = Math.max(0, task.getSubTasksCount() - 1);
            task.setSubTasksCount(newCount);
            if ("completed".equals(subTask.getStatus())) {
                task.setCompletedSubTasks(Math.max(0, task.getCompletedSubTasks() - 1));
            }
            recalculateProgress(task);
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSubTask(Long subTaskId, String operatorId) {
        SubTask subTask = subTaskMapper.selectById(subTaskId);
        if (subTask == null) {
            throw new RuntimeException("子任务不存在");
        }
        if ("completed".equals(subTask.getStatus())) {
            return; // 已完成则忽略
        }

        subTask.setStatus("completed");
        subTask.setCompletedAt(LocalDateTime.now());
        subTask.setUpdateTime(LocalDateTime.now());
        subTaskMapper.updateById(subTask);

        // 更新父任务进度
        Task task = taskMapper.selectById(subTask.getTaskId());
        if (task != null) {
            task.setCompletedSubTasks(task.getCompletedSubTasks() + 1);
            recalculateProgress(task);
            // 检查是否全部完成
            if (task.getCompletedSubTasks() >= task.getSubTasksCount() && task.getSubTasksCount() > 0) {
                task.setProgress(100);
                // 有工作流的任务进入审核阶段
                if (hasReviewStage(task.getCategory())) {
                    task.setStatus("reviewing");
                } else {
                    task.setStatus("completed");
                }
            }
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    // ==================== 模板 CRUD ====================

    @Override
    public List<TaskTemplateVO> getTemplates(String category) {
        LambdaQueryWrapper<TaskTemplate> wrapper = new LambdaQueryWrapper<TaskTemplate>()
                .orderByAsc(TaskTemplate::getCategory)
                .orderByDesc(TaskTemplate::getIsSystem);

        if (category != null && !category.isEmpty()) {
            wrapper.eq(TaskTemplate::getCategory, category);
        }

        return templateMapper.selectList(wrapper).stream()
                .map(this::convertToTemplateVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskTemplateVO createTemplate(TaskTemplateCreateDTO dto, String operatorId) {
        TaskTemplate template = new TaskTemplate();
        template.setCategory(dto.getCategory());
        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setDefaultTitle(dto.getDefaultTitle());
        template.setDefaultDescription(dto.getDefaultDescription());
        template.setStagesConfig(dto.getStagesConfig());
        template.setIsSystem(false);
        templateMapper.insert(template);

        return convertToTemplateVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskTemplateVO updateTemplate(Long templateId, TaskTemplateUpdateDTO dto, String operatorId) {
        TaskTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new RuntimeException("系统内置模板不允许修改");
        }

        template.setCategory(dto.getCategory());
        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setDefaultTitle(dto.getDefaultTitle());
        template.setDefaultDescription(dto.getDefaultDescription());
        template.setStagesConfig(dto.getStagesConfig());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);

        return convertToTemplateVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId, String operatorId) {
        TaskTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new RuntimeException("系统内置模板不允许删除");
        }

        templateMapper.deleteById(templateId);
    }

    // ========== 私有辅助方法 ==========

    /** 生成任务编号 */
    private String generateRefNo(String category) {
        String prefix = switch (category) {
            case "daily" -> "TSK-D";
            case "training" -> "TRN";
            case "business_trip" -> "TRIP";
            case "inventory" -> "INV";
            case "assessment" -> "ASSESS";
            default -> "TSK";
        };
        return prefix + "-" + java.time.LocalDate.now().toString().replace("-", "") + "-"
                + String.format("%04d", System.currentTimeMillis() % 10000);
    }

    /** 获取某类别的初始工作流阶段 */
    private String getInitialStage(String category) {
        return switch (category) {
            case "daily" -> "publish";
            case "training" -> "create";
            case "business_trip" -> "propose";
            case "inventory" -> "plan";
            case "assessment" -> "initiate";
            default -> "publish";
        };
    }

    /** 判断某类别的任务是否有审核阶段 */
    private boolean hasReviewStage(String category) {
        return !"daily".equals(category); // 日常指派无需审核，其他类别需要
    }

    /** 重新计算任务进度百分比 */
    private void recalculateProgress(Task task) {
        if (task.getSubTasksCount() != null && task.getSubTasksCount() > 0) {
            int pct = (int) ((task.getCompletedSubTasks() * 100.0) / task.getSubTasksCount());
            task.setProgress(pct);
        } else {
            task.setProgress(0);
        }
    }

    /** 转换为列表VO */
    private TaskVO convertToVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setTaskId(String.valueOf(task.getTaskId()));
        vo.setRefNo(task.getRefNo());
        vo.setCategory(task.getCategory());
        vo.setTitle(task.getTitle());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setCurrentStage(task.getCurrentStage());
        vo.setProgress(task.getProgress());
        vo.setDeadline(task.getDeadline());
        vo.setSubTasksCount(task.getSubTasksCount());
        vo.setCompletedSubTasks(task.getCompletedSubTasks());
        vo.setCreateTime(task.getCreateTime());
        // TODO: 解析assigneeIds JSON计算接收人数
        vo.setAssigneeCount(0);
        return vo;
    }

    /** 转换为详情VO */
    private TaskDetailVO convertToDetailVO(Task task) {
        TaskDetailVO vo = new TaskDetailVO();
        // 复制基础字段
        vo.setTaskId(String.valueOf(task.getTaskId()));
        vo.setRefNo(task.getRefNo());
        vo.setCategory(task.getCategory());
        vo.setTitle(task.getTitle());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setCurrentStage(task.getCurrentStage());
        vo.setProgress(task.getProgress());
        vo.setDeadline(task.getDeadline());
        vo.setSubTasksCount(task.getSubTasksCount());
        vo.setCompletedSubTasks(task.getCompletedSubTasks());
        vo.setCreateTime(task.getCreateTime());

        // 详情字段
        vo.setDescription(task.getDescription());
        vo.setPublisherId(task.getPublisherId());
        // TODO: 根据publisherId查询姓名
        vo.setPublisherName(task.getPublisherId());
        vo.setStagesConfig(task.getStagesConfig());

        // 查询子任务列表
        LambdaQueryWrapper<SubTask> subWrapper = new LambdaQueryWrapper<SubTask>()
                .eq(SubTask::getTaskId, task.getTaskId())
                .orderByAsc(SubTask::getSortOrder);
        List<SubTask> subTasks = subTaskMapper.selectList(subWrapper);
        vo.setSubTasks(subTasks.stream().map(this::convertToSubTaskVO).collect(Collectors.toList()));

        return vo;
    }

    /** 转换为子任务VO */
    private SubTaskVO convertToSubTaskVO(SubTask subTask) {
        SubTaskVO vo = new SubTaskVO();
        vo.setSubTaskId(String.valueOf(subTask.getSubTaskId()));
        vo.setTaskId(String.valueOf(subTask.getTaskId()));
        vo.setTitle(subTask.getTitle());
        vo.setDescription(subTask.getDescription());
        vo.setSortOrder(subTask.getSortOrder());
        vo.setStatus(subTask.getStatus());
        vo.setCompletedAt(subTask.getCompletedAt());
        return vo;
    }

    /** 转换为模板VO */
    private TaskTemplateVO convertToTemplateVO(TaskTemplate template) {
        TaskTemplateVO vo = new TaskTemplateVO();
        vo.setTemplateId(String.valueOf(template.getTemplateId()));
        vo.setCategory(template.getCategory());
        vo.setName(template.getName());
        vo.setDescription(template.getDescription());
        vo.setDefaultTitle(template.getDefaultTitle());
        vo.setDefaultDescription(template.getDefaultDescription());
        vo.setStagesConfig(template.getStagesConfig());
        vo.setIsSystem(template.getIsSystem());
        vo.setCreateTime(template.getCreateTime());
        return vo;
    }
}
