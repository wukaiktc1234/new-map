package com.foodtraceability.utils;

import org.junit.jupiter.api.Test;

/**
 * Jasypt加密工具测试类
 * 用于生成加密后的配置值
 * 
 * 使用说明：
 * 1. 运行testEncryptPassword()方法生成加密后的数据库密码
 * 2. 将生成的密文复制到application.yml中，格式为ENC(密文)
 * 3. 在启动应用时设置环境变量JASYPT_ENCRYPTOR_PASSWORD或使用默认密码
 * 
 * 启动应用时设置密码的方式：
 * - 方式1：环境变量
 *   export JASYPT_ENCRYPTOR_PASSWORD=your-password
 * - 方式2：JVM参数
 *   java -Djasypt.encryptor.password=your-password -jar app.jar
 * - 方式3：application.yml配置
 *   jasypt:
 *     encryptor:
 *       password: your-password
 */
public class JasyptEncryptorUtilTest {
    
    /**
     * 测试加密数据库密码
     * 运行此方法后，将输出的密文配置到application.yml中
     */
    @Test
    public void testEncryptPassword() {
        String plainPassword = "123456";
        String encryptedPassword = JasyptEncryptorUtil.encryptWithDefaultPassword(plainPassword);
        
        System.out.println("========================================");
        System.out.println("明文密码: " + plainPassword);
        System.out.println("加密后的密文: " + encryptedPassword);
        System.out.println("========================================");
        System.out.println("请在application.yml中使用以下格式:");
        System.out.println("password: ENC(" + encryptedPassword + ")");
        System.out.println("========================================");
        System.out.println("默认加密密码: " + JasyptEncryptorUtil.getDefaultPassword());
        System.out.println("加密算法: " + JasyptEncryptorUtil.getAlgorithm());
        System.out.println("========================================");
    }
    
    /**
     * 测试加密其他敏感配置
     */
    @Test
    public void testEncryptOtherConfigs() {
        System.out.println("========================================");
        
        // 加密邮件密码
        String mailPassword = "demoPassword123";
        String encryptedMailPassword = JasyptEncryptorUtil.encryptWithDefaultPassword(mailPassword);
        System.out.println("邮件密码加密: ENC(" + encryptedMailPassword + ")");
        
        // 加密JWT密钥
        String jwtSecret = "food-traceability-system-secure-jwt-secret-key-for-production-2025";
        String encryptedJwtSecret = JasyptEncryptorUtil.encryptWithDefaultPassword(jwtSecret);
        System.out.println("JWT密钥加密: ENC(" + encryptedJwtSecret + ")");
        
        // 加密RabbitMQ密码
        String rabbitmqPassword = "guest";
        String encryptedRabbitmqPassword = JasyptEncryptorUtil.encryptWithDefaultPassword(rabbitmqPassword);
        System.out.println("RabbitMQ密码加密: ENC(" + encryptedRabbitmqPassword + ")");
        
        // 加密Redis密码
        String redisPassword = "your-redis-password";
        String encryptedRedisPassword = JasyptEncryptorUtil.encryptWithDefaultPassword(redisPassword);
        System.out.println("Redis密码加密: ENC(" + encryptedRedisPassword + ")");
        
        // 加密SSL密钥库密码
        String keystorePassword = "password";
        String encryptedKeystorePassword = JasyptEncryptorUtil.encryptWithDefaultPassword(keystorePassword);
        System.out.println("SSL密钥库密码加密: ENC(" + encryptedKeystorePassword + ")");
        
        System.out.println("========================================");
    }
    
    /**
     * 测试解密
     */
    @Test
    public void testDecrypt() {
        String plainPassword = "123456";
        String encryptedPassword = JasyptEncryptorUtil.encryptWithDefaultPassword(plainPassword);
        String decryptedPassword = JasyptEncryptorUtil.decrypt(encryptedPassword);
        
        System.out.println("========================================");
        System.out.println("明文密码: " + plainPassword);
        System.out.println("加密后的密文: " + encryptedPassword);
        System.out.println("解密后的密码: " + decryptedPassword);
        System.out.println("解密结果验证: " + plainPassword.equals(decryptedPassword));
        System.out.println("========================================");
    }

    /**
     * 生成 BCrypt 哈希，用于更新数据库中的用户密码
     */
    @Test
    public void generateBCryptHash() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String plainPassword = "admin123";
        String hash = encoder.encode(plainPassword);
        System.out.println("========================================");
        System.out.println("明文密码: " + plainPassword);
        System.out.println("BCrypt 哈希: " + hash);
        System.out.println("========================================");
    }
    
    /**
     * 使用自定义密码加密
     */
    @Test
    public void testEncryptWithCustomPassword() {
        String customPassword = "my-custom-encryption-password";
        String plainText = "123456";
        
        JasyptEncryptorUtil.setPassword(customPassword);
        String encryptedPassword = JasyptEncryptorUtil.encrypt(plainText);
        String decryptedPassword = JasyptEncryptorUtil.decrypt(encryptedPassword);
        
        System.out.println("========================================");
        System.out.println("自定义加密密码: " + customPassword);
        System.out.println("明文密码: " + plainText);
        System.out.println("加密后的密文: " + encryptedPassword);
        System.out.println("解密后的密码: " + decryptedPassword);
        System.out.println("解密结果验证: " + plainText.equals(decryptedPassword));
        System.out.println("========================================");
        System.out.println("启动应用时请设置环境变量:");
        System.out.println("export JASYPT_ENCRYPTOR_PASSWORD=" + customPassword);
        System.out.println("========================================");
    }
}
