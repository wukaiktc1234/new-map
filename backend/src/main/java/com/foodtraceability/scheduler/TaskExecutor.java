package com.foodtraceability.scheduler;

import com.foodtraceability.entity.ScheduledTask;

/**
 * 任务执行器接口
 * 采用策略模式，不同类型的任务可以有不同的执行器实现
 */
public interface TaskExecutor {

    /**
     * 执行定时任务（同步方式）
     * @param task 待执行的任务定义
     * @return 任务执行结果
     */
    TaskExecutionResult execute(ScheduledTask task);

    /**
     * 判断当前执行器是否支持指定的任务处理器
     * @param handlerName 任务处理器名称（jobHandler字段值）
     * @return 是否支持
     */
    boolean supports(String handlerName);

    /**
     * 获取执行器名称
     * @return 执行器标识名
     */
    String getExecutorName();
}
