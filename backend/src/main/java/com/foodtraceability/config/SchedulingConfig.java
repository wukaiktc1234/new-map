package com.foodtraceability.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 调度任务配置类
 * <p>
 * 设计目的：
 * 1. 通过配置开关 app.scheduling.enabled 控制是否启用 @Scheduled 注解
 * 2. 在开发环境（频繁重启/调试）可设为 false，避免定时任务干扰
 * 3. 在生产环境保持启用（默认值 matchIfMissing = true）
 * 4. 自定义 TaskScheduler 线程池大小，避免默认单线程阻塞
 * </p>
 * <p>
 * JVM 稳定性背景：
 * Java 21 虚拟线程在 Windows 平台存在稳定性问题（exit code -1073741819）
 * 高频定时任务（10秒/次扫描）会加剧此问题
 * 开发环境禁用调度可显著延长后端连续运行时间
 * </p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "app.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SchedulingConfig implements SchedulingConfigurer {

    /**
     * 自定义 TaskScheduler 线程池
     * 默认 Spring 使用单线程调度器，多个 @Scheduled 任务会串行执行
     * 这里改为 5 线程池，避免长任务阻塞其他定时任务
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}
