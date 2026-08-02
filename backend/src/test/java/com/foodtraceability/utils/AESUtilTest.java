package com.foodtraceability.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AES加密工具测试类
 * 用于测试AESUtil的加密和解密功能
 */
public class AESUtilTest {

    private AESUtil aesUtil;

    @BeforeEach
    public void setUp() {
        // 初始化AESUtil实例
        aesUtil = new AESUtil();
        // 设置密钥
        // 注意：这里需要通过反射设置secretKey，因为它是private的
        try {
            java.lang.reflect.Field field = AESUtil.class.getDeclaredField("secretKey");
            field.setAccessible(true);
            field.set(aesUtil, "testSecretKey123456789012345678901234567890");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试加密和解密功能
     * 验证相同的明文加密后能正确解密
     */
    @Test
    public void testEncryptAndDecrypt() {
        // 测试数据
        String plaintext = "testAPIKey123456";
        
        // 加密
        String ciphertext = aesUtil.encrypt(plaintext);
        
        // 验证加密结果不为空
        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext);
        
        // 解密
        String decryptedText = aesUtil.decrypt(ciphertext);
        
        // 验证解密结果与原明文一致
        assertEquals(plaintext, decryptedText);
        
        System.out.println("原明文: " + plaintext);
        System.out.println("加密后: " + ciphertext);
        System.out.println("解密后: " + decryptedText);
        System.out.println("加密解密测试通过!");
    }

    /**
     * 测试空字符串加密解密
     * 验证空字符串能正确处理
     */
    @Test
    public void testEmptyString() {
        String plaintext = "";
        
        String ciphertext = aesUtil.encrypt(plaintext);
        String decryptedText = aesUtil.decrypt(ciphertext);
        
        assertEquals(plaintext, decryptedText);
        
        System.out.println("空字符串测试通过!");
    }

    /**
     * 测试null值加密解密
     * 验证null值能正确处理
     */
    @Test
    public void testNullValue() {
        String plaintext = null;
        
        String ciphertext = aesUtil.encrypt(plaintext);
        String decryptedText = aesUtil.decrypt(ciphertext);
        
        assertNull(ciphertext);
        assertNull(decryptedText);
        
        System.out.println("null值测试通过!");
    }

    /**
     * 测试长字符串加密解密
     * 验证长字符串能正确处理
     */
    @Test
    public void testLongString() {
        // 创建一个长字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("test");
        }
        String plaintext = sb.toString();
        
        String ciphertext = aesUtil.encrypt(plaintext);
        String decryptedText = aesUtil.decrypt(ciphertext);
        
        assertEquals(plaintext, decryptedText);
        
        System.out.println("长字符串测试通过!");
    }

    /**
     * 测试多轮加密解密
     * 验证多次加密解密能正确处理
     */
    @Test
    public void testMultipleRounds() {
        String plaintext = "testMultipleRounds123";
        
        // 执行10轮加密解密
        for (int i = 0; i < 10; i++) {
            String ciphertext = aesUtil.encrypt(plaintext);
            String decryptedText = aesUtil.decrypt(ciphertext);
            assertEquals(plaintext, decryptedText);
        }
        
        System.out.println("多轮加密解密测试通过!");
    }
}
