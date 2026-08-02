package com.foodtraceability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP配置类，启用AOP支持
 * @author example
 * @since 2025-12-07
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {
    // 启用AspectJ自动代理
}
