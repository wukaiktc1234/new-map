package com.foodtraceability.runner;

import com.foodtraceability.scheduler.TaskSchedulerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * 调度引擎启动Runner
 * 应用启动完成后触发TaskSchedulerEngine执行首次扫描
 */
@Component
@ConditionalOnBean(TaskSchedulerEngine.class)
public class SchedulerEngineRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerEngineRunner.class);


    public SchedulerEngineRunner(TaskSchedulerEngine taskSchedulerEngine) {
        this.taskSchedulerEngine = taskSchedulerEngine;
    }

    private final TaskSchedulerEngine taskSchedulerEngine;

    @Override
    public void run(String... args) {
        logger.info("[调度引擎Runner] 触发任务调度引擎初始化...");
        taskSchedulerEngine.onApplicationStarted();
        logger.info("[调度引擎Runner] 调度引擎初始化完成");
    }
}
