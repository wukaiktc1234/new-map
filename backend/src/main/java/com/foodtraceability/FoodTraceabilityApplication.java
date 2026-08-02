package com.foodtraceability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 食品溯源系统启动类
 * <p>
 * 注意：@EnableScheduling 已移至 {@link com.foodtraceability.config.SchedulingConfig}
 * 通过 app.scheduling.enabled 配置开关控制，便于在开发环境禁用所有定时任务
 * 解决 Java 21 + Windows 平台 JVM 频繁崩溃（exit code -1073741819）问题
 * </p>
 */
@SpringBootApplication(
    exclude = {
        HibernateJpaAutoConfiguration.class
    }
)
@EnableAsync
@EnableMethodSecurity
public class FoodTraceabilityApplication {

    public static void main(String[] args) {
        System.out.println("Starting Food Traceability System...");
        SpringApplication.run(FoodTraceabilityApplication.class, args);
        System.out.println("Food Traceability System started successfully!");
    }

}
