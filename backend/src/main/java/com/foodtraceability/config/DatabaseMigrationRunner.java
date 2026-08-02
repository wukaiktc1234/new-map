package com.foodtraceability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据库迁移运行器
 * 随应用启动执行必要的初始化/修复逻辑
 */
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        logger.info("开始执行数据库迁移...");

        logger.info("数据库迁移完成！");
    }
}
