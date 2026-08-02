package com.foodtraceability.utils;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig;

/**
 * Jasypt加密工具类
 */
public class JasyptEncryptorUtil {

    private static final String DEFAULT_PASSWORD = "my-traceability-secret-key-2025";
    private static final String DEFAULT_ALGORITHM = "PBEWithMD5AndDES";

    private static StandardPBEStringEncryptor encryptor;

    static {
        encryptor = new StandardPBEStringEncryptor();
        EnvironmentPBEConfig config = new EnvironmentPBEConfig();
        config.setAlgorithm(DEFAULT_ALGORITHM);
        config.setPassword(DEFAULT_PASSWORD);
        encryptor.setConfig(config);
    }

    public static String encryptWithDefaultPassword(String plainText) {
        return encryptor.encrypt(plainText);
    }

    public static String decrypt(String encryptedText) {
        return encryptor.decrypt(encryptedText);
    }

    public static String encrypt(String plainText) {
        return encryptor.encrypt(plainText);
    }

    public static void setPassword(String password) {
        // 重新创建encryptor对象，因为已初始化的对象不允许更改密码
        encryptor = new StandardPBEStringEncryptor();
        EnvironmentPBEConfig config = new EnvironmentPBEConfig();
        config.setAlgorithm(DEFAULT_ALGORITHM);
        config.setPassword(password);
        encryptor.setConfig(config);
    }

    public static String getDefaultPassword() {
        return DEFAULT_PASSWORD;
    }

    public static String getAlgorithm() {
        return DEFAULT_ALGORITHM;
    }

    public static void main(String[] args) {
        String dbPassword = "123456";
        String jwtSecret = "food-traceability-system-secure-jwt-secret-key-for-production-2025";
        String mailPassword = "demoPassword123";

        System.out.println("DB Password Encrypted: " + encryptWithDefaultPassword(dbPassword));
        System.out.println("JWT Secret Encrypted: " + encryptWithDefaultPassword(jwtSecret));
        System.out.println("Mail Password Encrypted: " + encryptWithDefaultPassword(mailPassword));
    }
}
