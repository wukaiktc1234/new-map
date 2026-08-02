package com.foodtraceability.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.foodtraceability.entity.ScheduledTask;
import com.foodtraceability.entity.TaskExecutionLog;
import com.foodtraceability.entity.TaskExecutionLogDetail;
import com.foodtraceability.mapper.ScheduledTaskMapper;
import com.foodtraceability.mapper.TaskExecutionLogDetailMapper;
import com.foodtraceability.mapper.TaskExecutionLogMapper;
import com.foodtraceability.scheduler.impl.DefaultTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 定时任务调度引擎（核心组件）
 * <p>
 * 职责：
 * 1. 周期性扫描数据库中到期的ENABLED任务
 * 2. 根据任务类型（CRON/固定频率/一次性）判断是否需要触发
 * 3. 控制并发执行、超时终止、错误重试
 * 4. 记录完整的执行日志和步骤详情
 * </p>
 * <p>
 * 配置开关：通过 scheduler.enabled 控制是否启用（默认true）
 * 当数据库表不存在时自动降级，避免日志洪水
 * </p>
 */
@Component
public class TaskSchedulerEngine {

    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulerEngine.class);

    // ==================== 配置开关 ====================


    public TaskSchedulerEngine(ScheduledTaskMapper scheduledTaskMapper, TaskExecutionLogMapper executionLogMapper, TaskExecutionLogDetailMapper logDetailMapper, DefaultTaskExecutor defaultExecutor) {
        this.scheduledTaskMapper = scheduledTaskMapper;
        this.executionLogMapper = executionLogMapper;
        this.logDetailMapper = logDetailMapper;
        this.defaultExecutor = defaultExecutor;
    }

    /** 是否启用定时任务引擎（可通过application.yml配置） */
    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /** 连续错误计数（用于日志抑制） */
    private final AtomicLong consecutiveErrors = new AtomicLong(0);

    /** 最大连续错误次数（超过后降低日志级别） */
    private static final long MAX_CONSECUTIVE_ERRORS_BEFORE_SUPPRESS = 5;

    /** 日志抑制间隔（毫秒）- 连续错误超过阈值后，每30秒只记录一次 */
    private static final long LOG_SUPPRESS_INTERVAL_MS = 30000;

    /** 上次日志时间戳 */
    private volatile long lastErrorLogTime = 0;

    // ==================== 状态常量 ====================

    /** 任务状态：已启用 */
    private static final int STATUS_ENABLED = 1;
    /** 执行状态：空闲 */
    private static final int EXEC_IDLE = 0;
    /** 执行状态：运行中 */
    private static final int EXEC_RUNNING = 2;
    /** 日志执行状态：运行中 */
    private static final int LOG_STATUS_RUNNING = 0;
    /** 日志执行状态：成功 */
    private static final int LOG_STATUS_SUCCESS = 1;
    /** 日志执行状态：失败 */
    private static final int LOG_STATUS_FAILED = 2;
    /** 日志执行状态：超时 */
    private static final int LOG_STATUS_TIMEOUT = 3;
    /** 触发类型：定时调度 */
    private static final int TRIGGER_TYPE_SCHEDULED = 1;
    /** 触发类型：手动触发 */
    private static final int TRIGGER_TYPE_MANUAL = 2;
    /** 并发策略：禁止并发 */
    private static final int CONCURRENT_FORBID = 0;
    /** 并发策略：允许并发 */
    private static final int CONCURRENT_ALLOW = 1;
    /** 并发策略：丢弃新请求 */
    private static final int CONCURRENT_DISCARD = 2;

    // ==================== 依赖注入 ====================

    private final ScheduledTaskMapper scheduledTaskMapper;

    private final TaskExecutionLogMapper executionLogMapper;

    private final TaskExecutionLogDetailMapper logDetailMapper;

    private final DefaultTaskExecutor defaultExecutor;

    // ==================== 运行时状态 ====================

    /** 正在执行中的任务ID集合（防止重复调度） */
    private final ConcurrentHashMap<Long, Long> runningTasks = new ConcurrentHashMap<>();

    /** 每个任务上次触发时间（防止Cron重复触发） */
    private final ConcurrentHashMap<Long, LocalDateTime> lastTriggerTimes = new ConcurrentHashMap<>();

    /** 异步执行线程池 */
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r);
                t.setName("scheduler-engine-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });

    /** 引擎启动时间 */
    private volatile LocalDateTime engineStartTime;

    // ==================== 定时扫描入口 ====================

    /**
     * 应用启动完成后立即执行一次扫描，检查是否有到期任务
     */
    public void onApplicationStarted() {
        // 检查配置开关
        if (!schedulerEnabled) {
            logger.info("[调度引擎] 定时任务引擎已禁用（scheduler.enabled=false），跳过初始化");
            return;
        }

        this.engineStartTime = LocalDateTime.now();
        logger.info("[调度引擎] 引擎启动完成, 启动时间: {}", engineStartTime);

        // 启动后恢复卡在RUNNING状态的任务（应用重启可能导致）
        recoverStuckTasks();

        // 启动后延迟5秒执行首次扫描，等待Spring容器完全就绪
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000);
                logger.info("[调度引擎] 执行启动后首次扫描...");
                scanAndExecuteTasks();
                logger.info("[调度引擎] 启动后首次扫描完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // 启动时扫描失败不应阻止应用运行
                logger.warn("[调度引擎] 启动后扫描异常（不影响系统正常使用）: {}", e.getMessage());
            }
        }, asyncExecutor);
    }

    /**
     * 恢复应用重启前卡在RUNNING状态的任务
     * 将EXECUTION_STATUS=RUNNING的任务重置为IDLE
     */
    private void recoverStuckTasks() {
        try {
            LambdaUpdateWrapper<ScheduledTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ScheduledTask::getExecutionStatus, EXEC_RUNNING)
                   .set(ScheduledTask::getExecutionStatus, EXEC_IDLE)
                   .set(ScheduledTask::getLastExecutionMsg, "启动恢复: 重置运行中状态");
            int recovered = scheduledTaskMapper.update(null, wrapper);
            if (recovered > 0) {
                logger.info("[调度引擎] 启动恢复: 重置了{}个卡在RUNNING状态的任务", recovered);
            }
        } catch (Exception e) {
            logger.error("[调度引擎] 启动恢复任务失败", e);
        }
    }

    /**
     * 每10秒扫描一次数据库，查找到期的ENABLED任务并触发执行
     * 使用fixedDelay确保上一次执行完毕后再等10秒开始下一次
     *
     * 增强功能：
     * 1. 支持通过 scheduler.enabled 配置开关
     * 2. 数据库表不存在时自动降级（静默失败）
     * 3. 连续错误日志抑制机制（避免日志洪水）
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 15000)
    public void scanAndExecuteTasks() {
        // 检查配置开关
        if (!schedulerEnabled) {
            logger.debug("[调度引擎] 定时任务已禁用（scheduler.enabled=false），跳过扫描");
            return;
        }

        if (engineStartTime == null) {
            engineStartTime = LocalDateTime.now();
        }

        long scanStart = System.currentTimeMillis();
        logger.debug("[调度引擎] 开始定时扫描...");

        try {
            // 查询所有启用的任务
            List<ScheduledTask> enabledTasks = queryEnabledTasks();
            if (enabledTasks.isEmpty()) {
                return;
            }

            logger.debug("[调度引擎] 扫描到{}个启用任务", enabledTasks.size());

            LocalDateTime now = LocalDateTime.now();
            AtomicInteger triggeredCount = new AtomicInteger(0);

            for (ScheduledTask task : enabledTasks) {
                try {
                    if (shouldTrigger(task, now)) {
                        triggeredCount.incrementAndGet();
                        dispatchTask(task, TRIGGER_TYPE_SCHEDULED);
                    }
                } catch (Exception e) {
                    logger.error("[调度引擎] 判断任务是否到期异常: taskId={}, error={}",
                            task.getTaskId(), e.getMessage(), e);
                }
            }

            long scanCost = System.currentTimeMillis() - scanStart;
            if (triggeredCount.get() > 0) {
                logger.info("[调度引擎] 扫描完成: 共{}个启用任务, 触发{}个, 耗时{}ms",
                        enabledTasks.size(), triggeredCount.get(), scanCost);
            }

            // 成功执行，重置错误计数
            consecutiveErrors.set(0);

        } catch (org.springframework.dao.DataAccessResourceFailureException e) {
            // 数据库访问失败（表不存在、连接失败等）
            handleDatabaseError("数据库访问失败", e);
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            // SQL语法错误（表或字段不存在）
            handleDatabaseError("SQL语法错误（可能缺少scheduled_task相关表）", e);
        } catch (Exception e) {
            handleDatabaseError("定时扫描异常", e);
        }
    }

    /**
     * 处理数据库/系统错误，带日志抑制机制
     *
     * @param errorType 错误类型描述
     * @param e 异常对象
     */
    private void handleDatabaseError(String errorType, Exception e) {
        long errorCount = consecutiveErrors.incrementAndGet();
        long now = System.currentTimeMillis();

        if (errorCount <= MAX_CONSECUTIVE_ERRORS_BEFORE_SUPPRESS ||
            (now - lastErrorLogTime) >= LOG_SUPPRESS_INTERVAL_MS) {

            // 首几次错误或超过抑制间隔后记录详细日志
            if (errorCount == 1) {
                logger.error("[调度引擎] {} (首次出现，请检查数据库是否已初始化): {}", errorType, e.getMessage());
                logger.error("[调度引擎] 提示: 可在application.yml中设置 scheduler.enabled=false 临时关闭定时任务");
            } else if (errorCount <= MAX_CONSECUTIVE_ERRORS_BEFORE_SUPPRESS) {
                logger.warn("[调度引擎] {} (连续第{}次): {}", errorType, errorCount, e.getMessage());
            } else {
                logger.warn("[调度引擎] {} (已连续出错{}次，降低日志频率): {}", errorType, errorCount, e.getMessage());
            }

            lastErrorLogTime = now;

            // 连续错误过多时给出明确提示
            if (errorCount == MAX_CONSECUTIVE_ERRORS_BEFORE_SUPPRESS + 1) {
                logger.warn("[调度引擎] ⚠️ 连续错误过于频繁，已启用日志抑制模式（每30秒记录一次）");
                logger.warn("[调度引擎] 如需完全禁用定时任务，请在application.yml添加: scheduler.enabled: false");
            }
        }
        // 其他情况：静默处理，避免日志洪水
    }

    // ==================== 任务触发判断 ====================

    /**
     * 判断任务是否应该被触发执行
     * 根据任务类型使用不同的策略判断
     */
    boolean shouldTrigger(ScheduledTask task, LocalDateTime now) {
        Integer taskType = task.getTaskType();

        if (taskType == null) {
            logger.warn("[调度引擎] 任务类型为空, 跳过: taskId={}", task.getTaskId());
            return false;
        }

        return switch (taskType) {
            case 1 -> shouldTriggerCron(task, now);   // CRON表达式类型
            case 2 -> shouldTriggerFixedRate(task, now); // 固定频率类型
            case 3 -> shouldTriggerOneTime(task, now);  // 一次性任务
            default -> {
                logger.warn("[调度引擎] 未知任务类型: taskId={}, type={}", task.getTaskId(), taskType);
                yield false;
            }
        };
    }

    /**
     * CRON类型任务：解析cronExpression判断当前时间是否匹配触发条件
     */
    private boolean shouldTriggerCron(ScheduledTask task, LocalDateTime now) {
        String cronExpr = task.getCronExpression();
        if (cronExpr == null || cronExpr.trim().isEmpty()) {
            logger.warn("[CRON调度] CRON表达式为空: taskId={}", task.getTaskId());
            return false;
        }

        // 去重检查：60秒内不重复触发同一任务
        LocalDateTime lastTrigger = lastTriggerTimes.get(task.getTaskId());
        if (lastTrigger != null && !lastTrigger.plusSeconds(60).isBefore(now)) {
            return false;
        }

        try {
            CronTrigger trigger = new CronTrigger(cronExpr.trim());
            Date nextExecutionTime = trigger.nextExecutionTime(
                    new org.springframework.scheduling.support.SimpleTriggerContext());

            if (nextExecutionTime == null) {
                return false;
            }

            LocalDateTime nextExecLocal = convertToLocalDateTime(nextExecutionTime);
            boolean shouldTrigger = !nextExecLocal.isAfter(now.plusSeconds(10));

            if (shouldTrigger && logger.isDebugEnabled()) {
                logger.debug("[CRON调度] 任务到期: taskId={}, cron={}, nextExec={}",
                        task.getTaskId(), cronExpr, nextExecLocal);
            }

            return shouldTrigger;

        } catch (IllegalArgumentException e) {
            logger.error("[CRON调度] 无效的CRON表达式: taskId={}, expr={}, error={}",
                    task.getTaskId(), cronExpr, e.getMessage());
            updateTaskErrorStatus(task.getTaskId(), "无效的CRON表达式: " + cronExpr);
            return false;
        }
    }

    /**
     * 固定频率类型任务：检查 lastExecuteTime + intervalSeconds <= now
     */
    private boolean shouldTriggerFixedRate(ScheduledTask task, LocalDateTime now) {
        Integer intervalSeconds = task.getIntervalSeconds();
        if (intervalSeconds == null || intervalSeconds <= 0) {
            logger.warn("[固定频率调度] intervalSeconds无效: taskId={}, interval={}",
                    task.getTaskId(), intervalSeconds);
            return false;
        }

        LocalDateTime lastExecTime = task.getLastExecutionTime();
        if (lastExecTime == null) {
            // 从未执行过，立即触发
            logger.debug("[固定频率调度] 首次执行: taskId={}", task.getTaskId());
            return true;
        }

        LocalDateTime expectedNextExec = lastExecTime.plusSeconds(intervalSeconds);
        boolean shouldTrigger = !expectedNextExec.isAfter(now);

        if (shouldTrigger && logger.isDebugEnabled()) {
            logger.debug("[固定频率调度] 任务到期: taskId={}, interval={}s, lastExec={}, expectedNext={}",
                    task.getTaskId(), intervalSeconds, lastExecTime, expectedNextExec);
        }

        return shouldTrigger;
    }

    /**
     * 一次性任务：检查 oneTimeExecuteAt <= now 且从未成功执行过
     */
    private boolean shouldTriggerOneTime(ScheduledTask task, LocalDateTime now) {
        // 一次性任务的执行时间存储在nextExecutionTime字段
        LocalDateTime executeAt = task.getNextExecutionTime();
        if (executeAt == null) {
            return false;
        }

        // 已过了执行时间
        if (executeAt.isAfter(now)) {
            return false;
        }

        // 检查是否已经执行过（有成功的日志记录）
        LambdaQueryWrapper<TaskExecutionLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(TaskExecutionLog::getTaskId, task.getTaskId())
                   .eq(TaskExecutionLog::getExecutionStatus, LOG_STATUS_SUCCESS);
        Long successCount = executionLogMapper.selectCount(logWrapper);

        if (successCount != null && successCount > 0) {
            logger.debug("[一次性调度] 任务已执行过, 跳过: taskId={}", task.getTaskId());
            return false;
        }

        logger.info("[一次性调度] 一次性任务到期: taskId={}, executeAt={}",
                task.getTaskId(), executeAt);
        return true;
    }

    // ==================== 任务分发与执行 ====================

    /**
     * 分发任务到异步线程池执行
     * 包含并发控制逻辑
     */
    void dispatchTask(ScheduledTask task, int triggerType) {
        Long taskId = task.getTaskId();

        // 并发控制检查
        if (!checkConcurrency(task)) {
            return;
        }

        // 标记为运行中
        runningTasks.put(taskId, taskId);
        lastTriggerTimes.put(taskId, LocalDateTime.now());

        // 异步执行
        CompletableFuture.runAsync(() -> {
            try {
                executeTaskInternal(task, triggerType);
            } finally {
                runningTasks.remove(taskId);
            }
        }, asyncExecutor).exceptionally(ex -> {
            logger.error("[调度引擎] 异步执行异常: taskId={}, error={}", taskId, ex.getMessage(), ex);
            runningTasks.remove(taskId);
            handleExecutionFailure(task, null, ex, triggerType);
            return null;
        });
    }

    /**
     * 并发控制检查
     * @return true表示允许执行，false表示不允许
     */
    private boolean checkConcurrency(ScheduledTask task) {
        Long taskId = task.getTaskId();
        Integer executionStatus = task.getExecutionStatus() != null ? task.getExecutionStatus() : EXEC_IDLE;
        Integer concurrentPolicy = task.getConcurrentPolicy() != null ? task.getConcurrentPolicy() : CONCURRENT_FORBID;

        // 已经在运行中
        if (runningTasks.containsKey(taskId) || executionStatus == EXEC_RUNNING) {
            switch (concurrentPolicy) {
                case CONCURRENT_FORBID:
                    logger.debug("[并发控制] 任务正在运行且禁止并发, 跳过: taskId={}", taskId);
                    return false;
                case CONCURRENT_DISCARD:
                    logger.debug("[并发控制] 任务正在运行且策略为丢弃, 跳过: taskId={}", taskId);
                    return false;
                case CONCURRENT_ALLOW:
                    logger.debug("[并发控制] 任务正在运行但允许并发, 继续执行: taskId={}", taskId);
                    return true;
                default:
                    return false;
            }
        }

        return true;
    }

    /**
     * 内部执行方法：同步执行单个任务的完整流程
     */
    void executeTaskInternal(ScheduledTask task, int triggerType) {
        Long taskId = task.getTaskId();
        Long logId = null;

        try {
            // 1. 更新任务状态为RUNNING
            markTaskRunning(taskId);

            // 2. 创建执行日志
            logId = createExecutionLog(task, triggerType);
            logger.info("[任务执行] 开始执行: taskId={}, taskCode={}, logId={}, trigger={}",
                    taskId, task.getTaskCode(), logId,
                    triggerType == TRIGGER_TYPE_SCHEDULED ? "SCHEDULED" : "MANUAL");

            // 3. 记录执行步骤：开始
            recordLogStep(logId, "任务开始", LOG_STATUS_RUNNING, "准备执行处理器: " + task.getJobHandler());

            // 4. 执行任务
            TaskExecutionResult result = defaultExecutor.execute(task);

            // 5. 处理执行结果
            result.markCompleted();
            handleExecutionResult(task, logId, result, triggerType);

        } catch (Exception e) {
            logger.error("[任务执行] 执行过程异常: taskId={}, logId={}, error={}",
                    taskId, logId, e.getMessage(), e);
            handleExecutionFailure(task, logId, e, triggerType);
        }
    }

    /**
     * 同步执行单个任务（供外部调用）
     */
    public TaskExecutionResult executeTask(ScheduledTask task) {
        dispatchTask(task, TRIGGER_TYPE_MANUAL);
        // 手动触发返回一个占位结果，实际结果通过日志查询
        return TaskExecutionResult.success("任务已提交执行");
    }

    /**
     * 异步执行单个任务（供外部调用）
     */
    public CompletableFuture<TaskExecutionResult> executeTaskAsync(ScheduledTask task) {
        return CompletableFuture.supplyAsync(() -> {
            executeTaskInternal(task, TRIGGER_TYPE_MANUAL);
            return TaskExecutionResult.success("异步任务已提交执行");
        }, asyncExecutor);
    }

    // ==================== 执行结果处理 ====================

    /**
     * 处理任务执行成功结果
     */
    private void handleExecutionResult(ScheduledTask task, Long logId,
                                        TaskExecutionResult result, int triggerType) {
        Long taskId = task.getTaskId();

        try {
            // 更新执行日志为成功
            updateExecutionLogSuccess(logId, result);

            // 记录执行步骤：完成
            recordLogStep(logId, "任务完成", LOG_STATUS_SUCCESS, result.getMessage());

            // 更新任务定义信息
            updateTaskAfterSuccess(taskId, result);

            logger.info("[任务执行] 执行成功: taskId={}, logId={}, duration={}ms, msg={}",
                    taskId, logId, result.getDurationMs(), result.getMessage());

        } catch (Exception e) {
            logger.error("[任务执行] 更新成功状态异常: taskId={}, logId={}", taskId, logId, e);
        }
    }

    /**
     * 处理任务执行失败
     */
    private void handleExecutionFailure(ScheduledTask task, Long logId,
                                         Throwable error, int triggerType) {
        Long taskId = task != null ? task.getTaskId() : null;
        String errorMsg = error != null ? error.getMessage() : "未知错误";
        String errorStack = error != null ? getStackTrace(error) : null;

        try {
            // 更新执行日志为失败
            if (logId != null) {
                updateExecutionLogFailed(logId, errorMsg, errorStack);
                recordLogStep(logId, "任务失败", LOG_STATUS_FAILED, errorMsg);
            }

            // 更新任务状态
            if (taskId != null) {
                updateTaskAfterFailure(taskId, errorMsg, task);
            }

            logger.error("[任务执行] 执行失败: taskId={}, logId={}, error={}",
                    taskId, logId, errorMsg);

        } catch (Exception e) {
            logger.error("[任务执行] 更新失败状态异常: taskId={}, logId={}", taskId, logId, e);
        }
    }

    // ==================== 数据库操作方法 ====================

    /**
     * 查询所有启用的任务
     */
    private List<ScheduledTask> queryEnabledTasks() {
        LambdaQueryWrapper<ScheduledTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduledTask::getStatus, STATUS_ENABLED)
               .orderByAsc(ScheduledTask::getTaskId);
        return scheduledTaskMapper.selectList(wrapper);
    }

    /**
     * 标记任务为运行中状态
     */
    private void markTaskRunning(Long taskId) {
        LambdaUpdateWrapper<ScheduledTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ScheduledTask::getTaskId, taskId)
               .set(ScheduledTask::getExecutionStatus, EXEC_RUNNING)
               .set(ScheduledTask::getLastExecutionTime, LocalDateTime.now());
        scheduledTaskMapper.update(null, wrapper);
    }

    /**
     * 创建执行日志记录
     */
    private Long createExecutionLog(ScheduledTask task, int triggerType) {
        TaskExecutionLog log = new TaskExecutionLog();
        log.setTaskId(task.getTaskId());
        log.setTaskName(task.getTaskName());
        log.setTaskCode(task.getTaskCode());
        log.setTriggerType(triggerType);
        log.setExecutionStatus(LOG_STATUS_RUNNING);
        log.setStartTime(LocalDateTime.now());
        log.setRetryCount(0);
        executionLogMapper.insert(log);
        return log.getLogId();
    }

    /**
     * 更新执行日志为成功状态
     */
    private void updateExecutionLogSuccess(Long logId, TaskExecutionResult result) {
        LambdaUpdateWrapper<TaskExecutionLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskExecutionLog::getLogId, logId)
               .set(TaskExecutionLog::getExecutionStatus, LOG_STATUS_SUCCESS)
               .set(TaskExecutionLog::getEndTime, LocalDateTime.now())
               .set(TaskExecutionLog::getDurationMs, result.getDurationMs())
               .set(TaskExecutionLog::getResultData, result.getData() != null
                       ? toJsonString(result.getData()) : result.getMessage());
        executionLogMapper.update(null, wrapper);
    }

    /**
     * 更新执行日志为失败状态
     */
    private void updateExecutionLogFailed(Long logId, String errorMessage, String errorStack) {
        LambdaUpdateWrapper<TaskExecutionLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskExecutionLog::getLogId, logId)
               .set(TaskExecutionLog::getExecutionStatus, LOG_STATUS_FAILED)
               .set(TaskExecutionLog::getEndTime, LocalDateTime.now())
               .set(TaskExecutionLog::getErrorMessage, errorMessage);
        executionLogMapper.update(null, wrapper);
    }

    /**
     * 记录执行步骤详情
     */
    private void recordLogStep(Long logId, String stepName, int stepStatus, String message) {
        if (logId == null) return;

        TaskExecutionLogDetail detail = new TaskExecutionLogDetail();
        detail.setLogId(logId);
        detail.setStepName(stepName);
        detail.setStepStatus(stepStatus);
        detail.setStartTime(LocalDateTime.now());
        detail.setEndTime(LocalDateTime.now());
        detail.setMessage(message);
        logDetailMapper.insert(detail);
    }

    /**
     * 任务执行成功后更新任务定义
     */
    private void updateTaskAfterSuccess(Long taskId, TaskExecutionResult result) {
        ScheduledTask task = scheduledTaskMapper.selectById(taskId);
        if (task == null) return;

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ScheduledTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ScheduledTask::getTaskId, taskId)
               .set(ScheduledTask::getExecutionStatus, EXEC_IDLE)
               .set(ScheduledTask::getLastExecutionTime, now)
               .set(ScheduledTask::getLastExecutionMsg, result.getMessage());

        // 计算下次执行时间
        LocalDateTime nextExec = calculateNextExecutionTime(task, now);
        if (nextExec != null) {
            wrapper.set(ScheduledTask::getNextExecutionTime, nextExec);
        }

        // 成功后如果之前是ERROR状态，恢复为ENABLED
        if (task.getStatus() != null && task.getStatus() == 4) { // ERROR=4
            wrapper.set(ScheduledTask::getStatus, STATUS_ENABLED);
        }

        scheduledTaskMapper.update(null, wrapper);
    }

    /**
     * 任务执行失败后更新任务定义（含重试判断和ERROR状态设置）
     */
    private void updateTaskAfterFailure(Long taskId, String errorMessage, ScheduledTask originalTask) {
        ScheduledTask task = scheduledTaskMapper.selectById(taskId);
        if (task == null) return;

        LambdaUpdateWrapper<ScheduledTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ScheduledTask::getTaskId, taskId)
               .set(ScheduledTask::getExecutionStatus, EXEC_IDLE)
               .set(ScheduledTask::getLastExecutionTime, LocalDateTime.now())
               .set(ScheduledTask::getLastExecutionMsg, "执行失败: " + errorMessage);

        // 判断是否需要重试
        int maxRetry = task.getMaxRetryCount() != null ? task.getMaxRetryCount() : 3;
        Integer currentRetry = getCurrentRetryCount(taskId);

        if (currentRetry < maxRetry) {
            // 还可以重试，保持ENABLED状态
            wrapper.set(ScheduledTask::getStatus, STATUS_ENABLED);
            logger.info("[重试机制] 任务将在下次调度时自动重试: taskId={}, currentRetry={}/{},",
                    taskId, currentRetry, maxRetry);
        } else {
            // 重试次数耗尽，设置为ERROR状态
            wrapper.set(ScheduledTask::getStatus, 4); // ERROR=4
            logger.warn("[重试机制] 任务重试次数耗尽, 设置为ERROR状态: taskId={}, maxRetry={}",
                    taskId, maxRetry);
        }

        scheduledTaskMapper.update(null, wrapper);
    }

    /**
     * 直接将任务设置为ERROR状态（用于配置错误等不可恢复的场景）
     */
    private void updateTaskErrorStatus(Long taskId, String reason) {
        LambdaUpdateWrapper<ScheduledTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ScheduledTask::getTaskId, taskId)
               .set(ScheduledTask::getStatus, 4) // ERROR
               .set(ScheduledTask::getExecutionStatus, EXEC_IDLE)
               .set(ScheduledTask::getLastExecutionMsg, "调度器错误: " + reason);
        scheduledTaskMapper.update(null, wrapper);
    }

    /**
     * 获取当前任务的重试次数（统计失败的日志数）
     */
    private int getCurrentRetryCount(Long taskId) {
        LambdaQueryWrapper<TaskExecutionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutionLog::getTaskId, taskId)
               .eq(TaskExecutionLog::getExecutionStatus, LOG_STATUS_FAILED);
        Long count = executionLogMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    // ==================== 工具方法 ====================

    /**
     * 计算下次执行时间
     */
    private LocalDateTime calculateNextExecutionTime(ScheduledTask task, LocalDateTime afterTime) {
        Integer taskType = task.getTaskType();
        if (taskType == null) return null;

        return switch (taskType) {
            case 1 -> { // CRON
                String cronExpr = task.getCronExpression();
                if (cronExpr == null || cronExpr.trim().isEmpty()) yield null;
                try {
                    CronTrigger trigger = new CronTrigger(cronExpr.trim());
                    Date nextTime = trigger.nextExecutionTime(
                            new org.springframework.scheduling.support.SimpleTriggerContext());
                    yield nextTime != null ? convertToLocalDateTime(nextTime) : null;
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
            case 2 -> { // FIXED_RATE
                Integer interval = task.getIntervalSeconds();
                yield (interval != null && interval > 0) ? afterTime.plusSeconds(interval) : null;
            }
            case 3 -> null; // ONE_TIME没有下次执行
            default -> null;
        };
    }

    /**
     * 将java.util.Date转换为LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * 对象转JSON字符串
     */
    private String toJsonString(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) return null;
        java.io.StringWriter sw = new java.io.StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    // ==================== 管理接口 ====================

    /**
     * 获取当前正在运行的任务数量
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    /**
     * 获取当前正在运行的任务ID列表
     */
    public List<Long> getRunningTaskIds() {
        return List.copyOf(runningTasks.keySet());
    }

    /**
     * 关闭引擎（释放资源）
     */
    public void shutdown() {
        logger.info("[调度引擎] 正在关闭调度引擎, 当前运行任务数: {}", runningTasks.size());
        asyncExecutor.shutdown();
        timeoutExecutor.shutdown();
        logger.info("[调度引擎] 调度引擎已关闭");
    }

    /** 超时执行器（用于DefaultTaskExecutor的超时控制） */
    private final ExecutorService timeoutExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("task-timeout-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

}
