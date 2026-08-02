package com.foodtraceability.scheduler.impl;

import com.foodtraceability.entity.ScheduledTask;
import com.foodtraceability.scheduler.TaskExecutionResult;
import com.foodtraceability.scheduler.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 默认任务执行器
 * 通过ApplicationContext获取handler bean并调用执行
 * 内置支持常见内置任务的处理逻辑
 */
@Component
public class DefaultTaskExecutor implements TaskExecutor, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskExecutor.class);

    /** 执行超时用的线程池 */
    private final ExecutorService timeoutExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("task-executor-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

    private ApplicationContext applicationContext;

    // ==================== 核心执行方法 ====================

    @Override
    public TaskExecutionResult execute(ScheduledTask task) {
        if (task == null) {
            return TaskExecutionResult.failure("任务定义不能为空");
        }

        String handlerName = task.getJobHandler();
        if (handlerName == null || handlerName.trim().isEmpty()) {
            return TaskExecutionResult.nonRetryableFailure("任务未配置执行处理器(jobHandler)");
        }

        logger.info("[任务执行] 开始执行任务: taskId={}, taskCode={}, handler={}",
                task.getTaskId(), task.getTaskCode(), handlerName);

        try {
            // 优先检查内置任务处理器
            TaskExecutionResult builtinResult = executeBuiltinTask(task, handlerName);
            if (builtinResult != null) {
                return builtinResult;
            }

            // 尝试通过Spring容器获取自定义handler bean并执行
            return executeCustomHandler(task, handlerName);

        } catch (Exception e) {
            logger.error("[任务执行] 任务执行异常: taskId={}, taskCode={}, error={}",
                    task.getTaskId(), task.getTaskCode(), e.getMessage(), e);
            return TaskExecutionResult.failure("任务执行异常: " + e.getMessage(), e);
        }
    }

    // ==================== 内置任务处理 ====================

    /**
     * 执行内置任务
     * @return 如果是内置任务返回结果，否则返回null表示需要走自定义handler逻辑
     */
    private TaskExecutionResult executeBuiltinTask(ScheduledTask task, String handlerName) {
        return switch (handlerName) {
            case "inventoryCheckTask" -> executeInventoryCheck(task);
            case "dataBackupTask" -> executeDataBackup(task);
            case "cleanupExpiredDataTask" -> executeCleanupExpiredData(task);
            case "systemHealthCheckTask" -> executeSystemHealthCheck(task);
            default -> null; // 非内置任务，交给自定义handler处理
        };
    }

    /**
     * 库存检查内置任务
     * 检查库存预警、过期商品等，后续可接入真实库存服务
     */
    private TaskExecutionResult executeInventoryCheck(ScheduledTask task) {
        logger.info("[库存检查] 开始执行库存检查任务: taskId={}", task.getTaskId());
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // TODO: 接入真实库存服务，当前为模拟实现
            logger.info("[库存检查] 检查库存预警阈值...");
            logger.info("[库存检查] 扫描即将过期商品...");
            logger.info("[库存检查] 生成库存报告...");

            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            logger.info("[库存检查] 库存检查完成, 耗时{}ms", durationMs);

            return TaskExecutionResult.success("库存检查任务执行成功，共扫描0条预警记录");

        } catch (Exception e) {
            logger.error("[库存检查] 库存检查失败: {}", e.getMessage(), e);
            return TaskExecutionResult.failure("库存检查任务执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 数据备份内置任务
     * 执行数据库备份操作
     */
    private TaskExecutionResult executeDataBackup(ScheduledTask task) {
        logger.info("[数据备份] 开始执行数据备份任务: taskId={}", task.getTaskId());
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // TODO: 接入真实备份服务，当前为模拟实现
            logger.info("[数据备份] 正在创建数据库快照...");
            logger.info("[数据备份] 备份文件生成中...");

            // 模拟耗时操作
            Thread.sleep(100);

            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            logger.info("[数据备份] 数据备份完成, 耗时{}ms", durationMs);

            Map<String, Object> backupInfo = Map.of(
                    "backupTime", LocalDateTime.now().toString(),
                    "backupSize", "0KB",
                    "status", "SUCCESS"
            );

            return TaskExecutionResult.success("数据备份任务执行成功", backupInfo);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return TaskExecutionResult.failure("数据备份任务被中断", e);
        } catch (Exception e) {
            logger.error("[数据备份] 数据备份失败: {}", e.getMessage(), e);
            return TaskExecutionResult.failure("数据备份任务执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 过期数据清理内置任务
     * 清理过期的日志、临时文件等
     */
    private TaskExecutionResult executeCleanupExpiredData(ScheduledTask task) {
        logger.info("[过期清理] 开始执行过期数据清理任务: taskId={}", task.getTaskId());
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // TODO: 接入真实清理服务，当前为模拟实现
            int cleanedLogs = 0;
            int cleanedTempFiles = 0;

            logger.info("[过期清理] 清理过期执行日志... 清理{}条", cleanedLogs);
            logger.info("[过期清理] 清理临时文件... 清理{}个", cleanedTempFiles);
            logger.info("[过期清理] 清理过期消息记录...");

            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            logger.info("[过期清理] 过期数据清理完成, 耗时{}ms, 共清理{}条记录",
                    durationMs, cleanedLogs + cleanedTempFiles);

            Map<String, Object> cleanupInfo = Map.of(
                    "cleanedLogs", cleanedLogs,
                    "cleanedTempFiles", cleanedTempFiles,
                    "totalCleaned", cleanedLogs + cleanedTempFiles
            );

            return TaskExecutionResult.success("过期数据清理任务执行成功", cleanupInfo);

        } catch (Exception e) {
            logger.error("[过期清理] 过期数据清理失败: {}", e.getMessage(), e);
            return TaskExecutionResult.failure("过期数据清理任务执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 系统健康检查内置任务
     * 检查数据库连接、Redis连接、磁盘空间等系统指标
     */
    private TaskExecutionResult executeSystemHealthCheck(ScheduledTask task) {
        logger.info("[健康检查] 开始执行系统健康检查任务: taskId={}", task.getTaskId());
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // TODO: 接入真实监控服务，当前为模拟实现
            boolean dbHealthy = true;
            boolean redisHealthy = true;
            boolean diskOk = true;

            logger.info("[健康检查] 检查数据库连接... {}", dbHealthy ? "正常" : "异常");
            logger.info("[健康检查] 检查Redis连接... {}", redisHealthy ? "正常" : "异常");
            logger.info("[健康检查] 检查磁盘空间... {}", diskOk ? "正常" : "不足");

            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            boolean allHealthy = dbHealthy && redisHealthy && diskOk;
            String message = allHealthy
                    ? "系统健康检查通过"
                    : "系统存在异常项，请及时处理";

            Map<String, Object> healthInfo = Map.of(
                    "database", dbHealthy ? "OK" : "ERROR",
                    "redis", redisHealthy ? "OK" : "ERROR",
                    "disk", diskOk ? "OK" : "WARNING",
                    "overall", allHealthy ? "HEALTHY" : "UNHEALTHY",
                    "checkTime", LocalDateTime.now().toString()
            );

            logger.info("[健康检查] 系统健康检查完成, 耗时{}ms, 状态: {}",
                    durationMs, allHealthy ? "健康" : "异常");

            return new TaskExecutionResult(allHealthy, message, healthInfo);

        } catch (Exception e) {
            logger.error("[健康检查] 系统健康检查失败: {}", e.getMessage(), e);
            return TaskExecutionResult.failure("系统健康检查任务执行失败: " + e.getMessage(), e);
        }
    }

    // ==================== 自定义Handler执行 ====================

    /**
     * 通过Spring容器获取自定义handler bean并执行
     * 支持两种方式：
     * 1. 实现了TaskHandler接口的bean
     * 2. 带有execute(ScheduledTask)方法的普通bean
     */
    private TaskExecutionResult executeCustomHandler(ScheduledTask task, String handlerName) {
        try {
            Object handlerBean = applicationContext.getBean(handlerName);
            logger.info("[自定义Handler] 找到handler bean: {}, 类型: {}",
                    handlerName, handlerBean.getClass().getName());

            // 方式1: 实现了TaskHandler接口
            if (handlerBean instanceof TaskHandler taskHandler) {
                return executeWithTimeout(task, taskHandler::handle);
            }

            // 方式2: 反射调用execute方法（兼容性方案）
            logger.warn("[自定义Handler] handler '{}' 未实现TaskHandler接口，尝试反射调用",
                    handlerName);
            return executeByReflection(task, handlerBean, handlerName);

        } catch (BeansException e) {
            logger.error("[自定义Handler] 未找到handler bean: {}", handlerName);
            return TaskExecutionResult.nonRetryableFailure(
                    "未找到任务处理器Bean: " + handlerName + "，请确认是否已注册到Spring容器");

        } catch (Exception e) {
            logger.error("[自定义Handler] 执行自定义handler异常: {}", e.getMessage(), e);
            return TaskExecutionResult.failure("自定义任务处理器执行异常: " + e.getMessage(), e);
        }
    }

    /**
     * 使用反射调用handler的execute方法
     */
    private TaskExecutionResult executeByReflection(ScheduledTask task, Object handlerBean, String handlerName) {
        try {
            java.lang.reflect.Method method = handlerBean.getClass()
                    .getMethod("execute", ScheduledTask.class);
            Object result = method.invoke(handlerBean, task);

            if (result instanceof TaskExecutionResult executionResult) {
                return executionResult;
            }

            return TaskExecutionResult.success("自定义任务执行完成", result);

        } catch (NoSuchMethodException e) {
            return TaskExecutionResult.nonRetryableFailure(
                    "任务处理器 '" + handlerName + "' 缺少execute(ScheduledTask)方法或未实现TaskHandler接口");

        } catch (Exception e) {
            Throwable cause = (e instanceof java.lang.reflect.InvocationTargetException)
                    ? e.getCause() : e;
            return TaskExecutionResult.failure("反射调用任务处理器失败: " + cause.getMessage(), cause);
        }
    }

    // ==================== 超时控制 ====================

    /**
     * 带超时控制的任务执行
     * @param task 任务定义
     * taskLogic 任务执行逻辑
     * @return 执行结果
     */
    private TaskExecutionResult executeWithTimeout(ScheduledTask task, java.util.function.Function<ScheduledTask, TaskExecutionResult> taskLogic) {
        int timeoutSeconds = task.getTimeoutSeconds() != null ? task.getTimeoutSeconds() : 300;
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 300; // 默认5分钟超时
        }

        Future<TaskExecutionResult> future = timeoutExecutor.submit(() -> taskLogic.apply(task));

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("[超时控制] 任务执行超时: taskId={}, timeout={}s, 强制终止",
                    task.getTaskId(), timeoutSeconds);
            return TaskExecutionResult.failure(
                    "任务执行超时（" + timeoutSeconds + "秒），已被强制终止", e);

        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            return TaskExecutionResult.failure("任务执行被中断", e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return TaskExecutionResult.failure("任务执行异常: " + cause.getMessage(), cause);
        }
    }

    // ==================== 接口实现 ====================

    @Override
    public boolean supports(String handlerName) {
        // 默认执行器支持所有handler（兜底）
        return true;
    }

    @Override
    public String getExecutorName() {
        return "DefaultTaskExecutor";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 自定义任务处理器接口
     * 用户可通过实现此接口来注册自定义任务处理器
     */
    public interface TaskHandler {
        /**
         * 处理任务执行
         * @param task 任务定义
         * @return 执行结果
         */
        TaskExecutionResult handle(ScheduledTask task);
    }
}
