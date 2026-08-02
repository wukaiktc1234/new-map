package com.foodtraceability.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Jasypt配置类
 * 用于启用配置属性加密功能
 */
@Configuration
@EnableEncryptableProperties
@ConditionalOnProperty(name = "jasypt.encryptor.enabled", havingValue = "true", matchIfMissing = false)
public class JasyptConfig {
    
}
