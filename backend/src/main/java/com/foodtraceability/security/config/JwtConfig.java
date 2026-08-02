package com.foodtraceability.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Configuration
public class JwtConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);

    private final Environment environment;

    /**
     * 默认JWT密钥（仅用于开发环境，生产环境必须通过环境变量配置）
     * 长度满足HS512算法要求（>=64字节）
     */
    private static final String DEFAULT_SECRET = "food-traceability-dev-secret-key-2026-must-be-at-least-64-bytes-for-hs512-algorithm-security";

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.access-token-expiration:1800000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    @Value("${jwt.refresh-window:3600000}")
    private long refreshWindow;

    @Value("${jwt.issuer:food-traceability-system}")
    private String issuer;

    /**
     * 构造函数注入Spring Environment，用于可靠检测active profile
     * 使用Environment而非System.getProperty，因为Docker/K8s通过环境变量
     * SPRING_PROFILES_ACTIVE设置profile时System.getProperty无法读取
     */
    public JwtConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * 初始化后检查：如果JWT密钥为空或空白，使用默认开发密钥
     * 生产环境必须通过环境变量 JWT_SECRET 配置强随机密钥
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            logger.warn("============================================");
            logger.warn("JWT安全警告: 未检测到JWT密钥配置");
            logger.warn("   当前使用默认开发密钥（不适用于生产环境）");
            logger.warn("   请设置环境变量: export JWT_SECRET=your-64-byte-random-key");
            logger.warn("============================================");
            this.secret = DEFAULT_SECRET;
        } else if (secret.length() < 32) {
            logger.warn("JWT安全警告: 密钥长度不足32字节({}字节)，建议使用至少64字节的随机密钥", secret.length());
        }

        // 安全红线：生产环境禁止使用默认密钥
        validateSecretKey();
    }

    /**
     * 校验JWT密钥安全性：生产环境使用默认密钥将阻止启动
     */
    private void validateSecretKey() {
        if (isDefaultSecretKey() && isProductionEnvironment()) {
            throw new IllegalStateException(
                "【安全红线】生产环境必须配置 jwt.secret 环境变量！" +
                "当前使用默认密钥，任何人都可伪造JWT Token。"
            );
        }
    }

    /**
     * 检测是否使用了默认开发密钥
     */
    private boolean isDefaultSecretKey() {
        return secret != null && secret.contains("food-traceability-dev-secret-key");
    }

    /**
     * 检测当前是否为生产环境
     * 使用Spring Environment抽象，支持所有profile设置方式：
     * - application.yml中的spring.profiles.active
     * - 命令行参数 --spring.profiles.active=prod
     * - 环境变量 SPRING_PROFILES_ACTIVE=prod（Docker/K8s常用）
     */
    private boolean isProductionEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.contains("prod") || profile.contains("production")) {
                return true;
            }
        }
        return false;
    }

    public String getSecret() {
        return secret;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public long getRefreshWindow() {
        return refreshWindow;
    }

    public String getIssuer() {
        return issuer;
    }
}
