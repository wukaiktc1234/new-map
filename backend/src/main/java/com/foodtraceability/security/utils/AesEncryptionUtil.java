package com.foodtraceability.security.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesEncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(AesEncryptionUtil.class);
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int MIN_KEY_LENGTH = 16;
    
    private static volatile String encryptionKey = null;
    
    @Value("${app.encryption.key:}")
    public void setEncryptionKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("加密密钥未配置！请在application.yml中设置app.encryption.key（至少" + MIN_KEY_LENGTH + "字符）");
        }
        if (isDefaultOrWeakKey(key)) {
            throw new IllegalStateException("检测到弱加密密钥！请使用强随机密钥。生成命令: openssl rand -base64 32");
        }
        encryptionKey = key;
        logger.info("AES加密密钥已配置，长度: {}字符", key.length());
    }
    
    public static boolean isInitialized() {
        return encryptionKey != null && !isDefaultOrWeakKey(encryptionKey);
    }
    
    private static boolean isDefaultOrWeakKey(String key) {
        if (key == null || key.length() < MIN_KEY_LENGTH) {
            return true;
        }
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("default") || 
               lowerKey.contains("change") || 
               lowerKey.contains("placeholder") ||
               lowerKey.equals("test") ||
               lowerKey.equals("password") ||
               lowerKey.equals("secret");
    }
    
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            SecretKeySpec keySpec = deriveKey(encryptionKey);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            logger.error("加密失败: {}", e.getMessage());
            throw new RuntimeException("数据加密失败", e);
        }
    }
    
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            SecretKeySpec keySpec = deriveKey(encryptionKey);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("解密失败: {}", e.getMessage());
            throw new RuntimeException("数据解密失败", e);
        }
    }
    
    private static SecretKeySpec deriveKey(String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
    
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return email;
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
