package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.ScheduledTaskCreateDTO;
import com.foodtraceability.dto.ScheduledTaskQueryDTO;
import com.foodtraceability.dto.ScheduledTaskUpdateDTO;
import com.foodtraceability.entity.ScheduledTask;
import com.foodtraceability.entity.TaskExecutionLog;
import com.foodtraceability.entity.TaskExecutionLogDetail;
import com.foodtraceability.mapper.ScheduledTaskMapper;
import com.foodtraceability.mapper.TaskExecutionLogDetailMapper;
import com.foodtraceability.mapper.TaskExecutionLogMapper;
import com.foodtraceability.service.ScheduledTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定时任务管理 Service 实现
 * 核心功能：任务CRUD、生命周期控制、执行日志查询、统计仪表盘、批量操作
 */
@Service
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskServiceImpl.class);


    public ScheduledTaskServiceImpl(ScheduledTaskMapper scheduledTaskMapper, TaskExecutionLogMapper executionLogMapper, TaskExecutionLogDetailMapper logDetailMapper) {
        this.scheduledTaskMapper = scheduledTaskMapper;
        this.executionLogMapper = executionLogMapper;
        this.logDetailMapper = logDetailMapper;
    }

    /** 任务状态常量 */
    private static final int STATUS_CREATED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_PAUSED = 2;
    private static final int STATUS_DISABLED = 3;
    private static final int STATUS_ERROR = 4;

    /** 执行状态常量 */
    private static final int EXEC_IDLE = 0;
    private static final int EXEC_PENDING = 1;
    private static final int EXEC_RUNNING = 2;

    /** 任务类型常量 */
    private static final int TYPE_CRON = 1;
    private static final int TYPE_FIXED_RATE = 2;
    private static final int TYPE_ONE_TIME = 3;

    private final ScheduledTaskMapper scheduledTaskMapper;

    private final TaskExecutionLogMapper executionLogMapper;

    private final TaskExecutionLogDetailMapper logDetailMapper;

    // ==================== 基础CRUD ====================

    @Override
    public ScheduledTask getById(Long taskId) {
        if (taskId == null) return null;
        return scheduledTaskMapper.selectById(taskId);
    }

    @Override
    public ScheduledTask getByTaskCode(String taskCode) {
        if (taskCode == null || taskCode.isEmpty()) return null;
        return scheduledTaskMapper.selectByTaskCode(taskCode);
    }

    @Override
    public List<ScheduledTask> list() {
        LambdaQueryWrapper<ScheduledTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ScheduledTask::getCreateTime);
        return scheduledTaskMapper.selectList(wrapper);
    }

    @Override
    public IPage<ScheduledTask> page(Page<ScheduledTask> page, ScheduledTaskQueryDTO query) {
        String taskName = query != null ? query.getTaskName() : null;
        String taskCode = query != null ? query.getTaskCode() : null;
        String taskGroup = query != null ? query.getTaskGroup() : null;
        Integer status = query != null ? query.getStatus() : null;
        Integer taskType = query != null ? query.getTaskType() : null;
        Integer isBuiltin = query != null ? query.getIsBuiltin() : null;

        return scheduledTaskMapper.selectTaskPage(page, taskName, taskCode,
            taskGroup, status, taskType, isBuiltin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledTask create(ScheduledTaskCreateDTO createDTO, Long userId, String username) {
        // 校验任务编码唯一性
        LambdaQueryWrapper<ScheduledTask> codeCheck = new LambdaQueryWrapper<>();
        codeCheck.eq(ScheduledTask::getTaskCode, createDTO.getTaskCode());
        Long existCount = scheduledTaskMapper.selectCount(codeCheck);
        if (existCount > 0) {
            throw new IllegalArgumentException("任务编码已存在: " + createDTO.getTaskCode());
        }

        // 校验任务类型与调度参数的匹配
        validateTaskTypeParams(createDTO.getTaskType(), createDTO.getCronExpression(),
            createDTO.getIntervalSeconds());

        ScheduledTask task = new ScheduledTask();
        task.setTaskName(createDTO.getTaskName().trim());
        task.setTaskCode(createDTO.getTaskCode().trim());
        task.setDescription(createDTO.getDescription());
        task.setTaskGroup(createDTO.getTaskGroup() != null ? createDTO.getTaskGroup().trim() : "DEFAULT");
        task.setJobHandler(createDTO.getJobHandler().trim());
        task.setCronExpression(createDTO.getCronExpression());
        task.setIntervalSeconds(createDTO.getIntervalSeconds());
        task.setTaskType(createDTO.getTaskType());
        task.setStatus(STATUS_CREATED); // 新建默认为CREATED状态
        task.setExecutionStatus(EXEC_IDLE);
        task.setConcurrentPolicy(createDTO.getConcurrentPolicy() != null ? createDTO.getConcurrentPolicy() : 0);
        task.setMaxRetryCount(createDTO.getMaxRetryCount() != null ? createDTO.getMaxRetryCount() : 3);
        task.setRetryIntervalSec(createDTO.getRetryIntervalSec() != null ? createDTO.getRetryIntervalSec() : 30);
        task.setTimeoutSeconds(createDTO.getTimeoutSeconds() != null ? createDTO.getTimeoutSeconds() : 300);
        task.setMisfirePolicy(createDTO.getMisfirePolicy() != null ? createDTO.getMisfirePolicy() : 1);
        task.setIsBuiltin(0); // 手动创建的非内置任务
        task.setVersion(1L);
        task.setCreateUserId(userId);
        task.setCreateUsername(username);
        task.setUpdateUserId(userId);
        task.setUpdateUsername(username);

        scheduledTaskMapper.insert(task);
        logger.info("创建定时任务成功: taskId={}, taskCode={}, operator={}",
            task.getTaskId(), task.getTaskCode(), username);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long taskId, ScheduledTaskUpdateDTO updateDTO, Long userId, String username) {
        ScheduledTask task = getAndValidateTask(taskId);

        // 内置任务的jobHandler和isBuiltin不允许修改
        if (task.getIsBuiltin() != null && task.getIsBuiltin() == 1 && updateDTO.getJobHandler() != null) {
            logger.warn("尝试修改内置任务处理器: taskId={}, user={}", taskId, userId);
            throw new SecurityException("内置任务不允许修改执行处理器");
        }

        // 如果修改了调度参数，需要校验
        Integer newTaskType = updateDTO.getTaskType();
        if (newTaskType != null && !newTaskType.equals(task.getTaskType())) {
            validateTaskTypeParams(newTaskType, updateDTO.getCronExpression(),
                updateDTO.getIntervalSeconds());
        } else if (newTaskType == null && (updateDTO.getCronExpression() != null
                || updateDTO.getIntervalSeconds() != null)) {
            // 类型未变但参数变了，用当前类型校验
            validateTaskTypeParams(task.getTaskType(), updateDTO.getCronExpression(),
                updateDTO.getIntervalSeconds());
        }

        // 只更新非空字段
        if (updateDTO.getTaskName() != null) {
            task.setTaskName(updateDTO.getTaskName().trim());
        }
        if (updateDTO.getDescription() != null) {
            task.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getTaskGroup() != null) {
            task.setTaskGroup(updateDTO.getTaskGroup().trim());
        }
        if (updateDTO.getJobHandler() != null && (task.getIsBuiltin() == null || task.getIsBuiltin() != 1)) {
            task.setJobHandler(updateDTO.getJobHandler().trim());
        }
        if (updateDTO.getCronExpression() != null) {
            task.setCronExpression(updateDTO.getCronExpression());
        }
        if (updateDTO.getIntervalSeconds() != null) {
            task.setIntervalSeconds(updateDTO.getIntervalSeconds());
        }
        if (updateDTO.getTaskType() != null) {
            task.setTaskType(updateDTO.getTaskType());
        }
        if (updateDTO.getConcurrentPolicy() != null) {
            task.setConcurrentPolicy(updateDTO.getConcurrentPolicy());
        }
        if (updateDTO.getMaxRetryCount() != null) {
            task.setMaxRetryCount(updateDTO.getMaxRetryCount());
        }
        if (updateDTO.getRetryIntervalSec() != null) {
            task.setRetryIntervalSec(updateDTO.getRetryIntervalSec());
        }
        if (updateDTO.getTimeoutSeconds() != null) {
            task.setTimeoutSeconds(updateDTO.getTimeoutSeconds());
        }
        if (updateDTO.getMisfirePolicy() != null) {
            task.setMisfirePolicy(updateDTO.getMisfirePolicy());
        }
        task.setUpdateUserId(userId);
        task.setUpdateUsername(username);
        task.setUpdateTime(LocalDateTime.now());

        int rows = scheduledTaskMapper.updateById(task); // @Version乐观锁自动生效
        if (rows == 0) {
            throw new RuntimeException("更新失败，数据可能已被其他人修改（版本冲突）");
        }
        logger.info("更新定时任务成功: taskId={}, taskCode={}, operator={}",
            taskId, task.getTaskCode(), username);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long taskId) {
        ScheduledTask task = getAndValidateTask(taskId);

        // 内置任务不允许删除
        if (task.getIsBuiltin() != null && task.getIsBuiltin() == 1) {
            throw new SecurityException("内置任务不允许删除");
        }

        // 运行中的任务不允许删除
        if (task.getExecutionStatus() != null && task.getExecutionStatus() == EXEC_RUNNING) {
            throw new IllegalStateException("任务正在运行中，请先停止后再删除");
        }

        int rows = scheduledTaskMapper.deleteById(taskId); // 逻辑删除
        logger.info("逻辑删除定时任务: taskId={}, taskCode={}", taskId, task.getTaskCode());
        return rows > 0;
    }

    // ==================== 生命周期控制 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(Long taskId, Long userId, String username) {
        ScheduledTask task = getAndValidateTask(taskId);
        int currentStatus = task.getStatus() != null ? task.getStatus() : STATUS_CREATED;

        // 状态机校验：只有CREATED/DISABLED/ERROR/PAUSED可以转为ENABLED
        if (currentStatus == STATUS_ENABLED) {
            logger.warn("任务已是启用状态: taskId={}", taskId);
            return true; // 幂等操作
        }
        if (currentStatus == STATUS_PAUSED || currentStatus == STATUS_DISABLED || currentStatus == STATUS_ERROR
                || currentStatus == STATUS_CREATED) {
            task.setStatus(STATUS_ENABLED);
            task.setExecutionStatus(EXEC_IDLE);
            task.setUpdateUserId(userId);
            task.setUpdateUsername(username);
            task.setUpdateTime(LocalDateTime.now());

            int rows = scheduledTaskMapper.updateById(task);
            if (rows == 0) {
                throw new RuntimeException("启用失败，数据可能已被其他人修改（版本冲突）");
            }
            logger.info("启用定时任务: taskId={}, taskCode={}, fromStatus={}, operator={}",
                taskId, task.getTaskCode(), currentStatus, username);
            return true;
        }
        throw new IllegalStateException("当前任务状态不支持启用操作: " + currentStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long taskId, Long userId, String username) {
        ScheduledTask task = getAndValidateTask(taskId);
        int currentStatus = task.getStatus() != null ? task.getStatus() : STATUS_CREATED;

        // 状态机校验：只有ENABLED/PAUSED/ERROR可以转为DISABLED
        if (currentStatus == STATUS_DISABLED) {
            logger.warn("任务已是禁用状态: taskId={}", taskId);
            return true;
        }
        if (currentStatus == STATUS_ENABLED || currentStatus == STATUS_PAUSED || currentStatus == STATUS_ERROR) {
            task.setStatus(STATUS_DISABLED);
            task.setExecutionStatus(EXEC_IDLE);
            task.setUpdateUserId(userId);
            task.setUpdateUsername(username);
            task.setUpdateTime(LocalDateTime.now());

            int rows = scheduledTaskMapper.updateById(task);
            if (rows == 0) {
                throw new RuntimeException("禁用失败，数据可能已被其他人修改（版本冲突）");
            }
            logger.info("禁用定时任务: taskId={}, taskCode={}, operator={}", taskId, task.getTaskCode(), username);
            return true;
        }
        throw new IllegalStateException("当前任务状态不支持禁用操作: " + currentStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pause(Long taskId, Long userId, String username) {
        ScheduledTask task = getAndValidateTask(taskId);
        int currentStatus = task.getStatus() != null ? task.getStatus() : STATUS_CREATED;

        // 只有ENABLED状态可以暂停
        if (currentStatus == STATUS_PAUSED) {
            logger.warn("任务已是暂停状态: taskId={}", taskId);
            return true;
        }
        if (currentStatus == STATUS_ENABLED) {
            task.setStatus(STATUS_PAUSED);
            task.setExecutionStatus(EXEC_IDLE);
            task.setUpdateUserId(userId);
            task.setUpdateUsername(username);
            task.setUpdateTime(LocalDateTime.now());

            int rows = scheduledTaskMapper.updateById(task);
            if (rows == 0) {
                throw new RuntimeException("暂停失败，数据可能已被其他人修改（版本冲突）");
            }
            logger.info("暂停定时任务: taskId={}, taskCode={}, operator={}", taskId, task.getTaskCode(), username);
            return true;
        }
        throw new IllegalStateException("只有启用状态的任务才能暂停，当前状态: " + currentStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(Long taskId, Long userId, String username) {
        ScheduledTask task = getAndValidateTask(taskId);
        int currentStatus = task.getStatus() != null ? task.getStatus() : STATUS_CREATED;

        // 只有PAUSED状态可以恢复
        if (currentStatus == STATUS_PAUSED) {
            task.setStatus(STATUS_ENABLED);
            task.setExecutionStatus(EXEC_IDLE);
            task.setUpdateUserId(userId);
            task.setUpdateUsername(username);
            task.setUpdateTime(LocalDateTime.now());

            int rows = scheduledTaskMapper.updateById(task);
            if (rows == 0) {
                throw new RuntimeException("恢复失败，数据可能已被其他人修改（版本冲突）");
            }
            logger.info("恢复定时任务: taskId={}, taskCode={}, operator={}", taskId, task.getTaskCode(), username);
            return true;
        }
        if (currentStatus == STATUS_ENABLED) {
            return true; // 幂等
        }
        throw new IllegalStateException("只有暂停状态的任务才能恢复，当前状态: " + currentStatus);
    }

    // ==================== 执行日志 ====================

    @Override
    public IPage<TaskExecutionLog> getExecutionLogs(Page<TaskExecutionLog> page, Long taskId,
                                                     Integer triggerType, Integer executionStatus,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        return executionLogMapper.selectLogPage(page, taskId, null, null,
            triggerType, executionStatus, startTime, endTime);
    }

    @Override
    public List<TaskExecutionLogDetail> getExecutionDetails(Long logId) {
        if (logId == null) return Collections.emptyList();

        // 使用LambdaQueryWrapper查询详情列表
        LambdaQueryWrapper<TaskExecutionLogDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutionLogDetail::getLogId, logId)
               .orderByAsc(TaskExecutionLogDetail::getDetailId);
        return logDetailMapper.selectList(wrapper);
    }

    @Override
    public TaskExecutionLog getLatestLog(Long taskId) {
        if (taskId == null) return null;

        LambdaQueryWrapper<TaskExecutionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutionLog::getTaskId, taskId)
               .orderByDesc(TaskExecutionLog::getStartTime)
               .last("LIMIT 1");
        return executionLogMapper.selectOne(wrapper);
    }

    @Override
    public TaskExecutionLog getExecutionLogById(Long logId) {
        if (logId == null) return null;
        return executionLogMapper.selectById(logId);
    }

    // ==================== 统计仪表盘 ====================

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = scheduledTaskMapper.selectTaskStat();

        // 补充日志统计
        Map<String, Object> logStats = executionLogMapper.selectLogStat(null);
        if (logStats != null) {
            stats.putAll(logStats);
        }

        return stats;
    }

    @Override
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();

        // 任务统计
        Map<String, Object> taskStats = scheduledTaskMapper.selectTaskStat();
        dashboard.put("taskStats", taskStats);

        // 日志统计（全部）
        Map<String, Object> logStats = executionLogMapper.selectLogStat(null);
        dashboard.put("logStats", logStats);

        // 运行中的任务数量
        LambdaQueryWrapper<ScheduledTask> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(ScheduledTask::getExecutionStatus, EXEC_RUNNING);
        long runningCount = scheduledTaskMapper.selectCount(runningWrapper);
        dashboard.put("runningTaskCount", runningCount);

        // 最近执行的10条日志
        Page<TaskExecutionLog> recentPage = new Page<>(1, 10);
        IPage<TaskExecutionLog> recentLogs = executionLogMapper.selectLogPage(
            recentPage, null, null, null, null, null, null, null);
        dashboard.put("recentLogs", recentLogs.getRecords());

        // 任务分组分布
        List<String> groups = getTaskGroups();
        dashboard.put("taskGroups", groups);

        return dashboard;
    }

    // ==================== 批量操作 ====================

    @Override
    public int batchEnable(List<Long> taskIds, Long userId, String username) {
        if (taskIds == null || taskIds.isEmpty()) return 0;
        int successCount = 0;
        for (Long taskId : taskIds) {
            try {
                if (enable(taskId, userId, username)) successCount++;
            } catch (Exception e) {
                logger.warn("批量启用跳过: taskId={}, reason={}", taskId, e.getMessage());
            }
        }
        logger.info("批量启用定时任务: 成功{}/总{}", successCount, taskIds.size());
        return successCount;
    }

    @Override
    public int batchDisable(List<Long> taskIds, Long userId, String username) {
        if (taskIds == null || taskIds.isEmpty()) return 0;
        filterBuiltinTasks(taskIds, "禁用");
        if (taskIds.isEmpty()) return 0;
        int successCount = 0;
        for (Long taskId : taskIds) {
            try {
                if (disable(taskId, userId, username)) successCount++;
            } catch (Exception e) {
                logger.warn("批量禁用跳过: taskId={}, reason={}", taskId, e.getMessage());
            }
        }
        logger.info("批量禁用定时任务: 成功{}/总{}", successCount, taskIds.size());
        return successCount;
    }

    @Override
    public int batchDelete(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) return 0;
        filterBuiltinTasks(taskIds, "删除");
        if (taskIds.isEmpty()) return 0;
        int successCount = 0;
        for (Long taskId : taskIds) {
            try {
                if (delete(taskId)) successCount++;
            } catch (Exception e) {
                logger.warn("批量删除跳过: taskId={}, reason={}", taskId, e.getMessage());
            }
        }
        logger.info("批量删除定时任务: 成功{}/总{}", successCount, taskIds.size());
        return successCount;
    }

    // ==================== 辅助查询 ====================

    @Override
    public List<String> getTaskGroups() {
        return scheduledTaskMapper.selectTaskGroups();
    }

    @Override
    public List<ScheduledTask> getBuiltinTasks() {
        return scheduledTaskMapper.selectBuiltinTasks();
    }

    // ==================== 私有工具方法 ====================

    /**
     * 获取并校验任务是否存在
     */
    private ScheduledTask getAndValidateTask(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        ScheduledTask task = scheduledTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    /**
     * 过滤掉内置任务，内置任务不允许批量删除/禁用
     * @param taskIds 任务ID列表（会被就地修改，移除内置任务ID）
     * @param operation 操作名称（用于日志）
     */
    private void filterBuiltinTasks(List<Long> taskIds, String operation) {
        if (taskIds == null || taskIds.isEmpty()) return;
        LambdaQueryWrapper<ScheduledTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ScheduledTask::getTaskId, taskIds)
               .eq(ScheduledTask::getIsBuiltin, 1);
        List<ScheduledTask> builtinTasks = scheduledTaskMapper.selectList(wrapper);
        if (!builtinTasks.isEmpty()) {
            List<Long> builtinIds = builtinTasks.stream()
                    .map(ScheduledTask::getTaskId)
                    .toList();
            taskIds.removeAll(builtinIds);
            logger.warn("批量{}操作已过滤{}个内置任务: {}", operation, builtinIds.size(), builtinIds);
        }
    }

    /**
     * 校验任务类型与调度参数的匹配关系
     */
    private void validateTaskTypeParams(Integer taskType, String cronExpression, Integer intervalSeconds) {
        if (taskType == null) {
            throw new IllegalArgumentException("任务类型不能为空");
        }
        switch (taskType) {
            case TYPE_CRON:
                if (cronExpression == null || cronExpression.trim().isEmpty()) {
                    throw new IllegalArgumentException("CRON类型任务必须设置cronExpression");
                }
                break;
            case TYPE_FIXED_RATE:
                if (intervalSeconds == null || intervalSeconds <= 0) {
                    throw new IllegalArgumentException("固定频率类型任务必须设置有效的intervalSeconds（正整数）");
                }
                break;
            case TYPE_ONE_TIME:
                // ONE_TIME类型不需要cron或interval
                break;
            default:
                throw new IllegalArgumentException("无效的任务类型: " + taskType + "，支持值：1=CRON 2=FIXED_RATE 3=ONE_TIME");
        }
    }
}
