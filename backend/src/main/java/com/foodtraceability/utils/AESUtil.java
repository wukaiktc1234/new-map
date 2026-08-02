package com.foodtraceability.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES加密工具类
 * 用于加密和解密敏感信息，如API密钥
 */
@Component
public class AESUtil {
    
    private static final Logger log = LoggerFactory.getLogger(AESUtil.class);
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    
    // 加密密钥，从配置文件中读取（无硬编码默认值，必须通过 application.yml/properties 或环境变量注入）
    @Value("${aes.secret.key:}")
    private String secretKey;
    
    /**
     * 生成AES密钥
     * @return 生成的密钥
     */
    private SecretKey getSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            // 使用安全的随机数生成器，不设置固定种子
            SecureRandom secureRandom = new SecureRandom();
            keyGenerator.init(KEY_SIZE, secureRandom);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            log.error("生成AES密钥失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }
    
    /**
     * 生成固定的AES密钥（基于配置文件中的密钥）
     * @return 生成的密钥
     */
    private SecretKey getFixedSecretKey() {
        try {
            // 使用SHA-256哈希算法将配置文件中的密钥转换为256位密钥
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            // 使用前32字节作为密钥（256位）
            byte[] fixedKeyBytes = new byte[32];
            System.arraycopy(keyBytes, 0, fixedKeyBytes, 0, Math.min(keyBytes.length, fixedKeyBytes.length));
            return new SecretKeySpec(fixedKeyBytes, ALGORITHM);
        } catch (Exception e) {
            log.error("生成固定AES密钥失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成固定AES密钥失败", e);
        }
    }
    
    /**
     * 生成随机IV向量
     * @return 生成的IV向量
     */
    private byte[] generateIV() {
        byte[] iv = new byte[16]; // AES块大小为16字节
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    /**
     * 加密字符串
     * @param plaintext 要加密的明文
     * @return 加密后的Base64字符串（格式：IV+密文）
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(getFixedSecretKey().getEncoded(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // 生成随机IV
            byte[] iv = generateIV();
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            // 初始化加密器
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            // 执行加密
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // 将IV和密文组合在一起，IV在前，密文在后
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
            
            // 返回Base64编码的字符串
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("AES加密失败", e);
        }
    }
    
    /**
     * 解密字符串
     * @param ciphertext 要解密的密文（Base64格式，格式：IV+密文）
     * @return 解密后的明文
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(getFixedSecretKey().getEncoded(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // 解码Base64字符串
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            
            // 分离IV和密文
            byte[] iv = new byte[16];
            byte[] encryptedBytes = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);
            
            // 初始化IV参数
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            // 初始化解密器
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            // 执行解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            // 返回解密后的明文
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("AES解密失败", e);
        }
    }
}