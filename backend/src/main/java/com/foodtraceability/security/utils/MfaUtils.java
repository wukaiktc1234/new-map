package com.foodtraceability.security.utils;

import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * MFA工具类
 * 提供TOTP验证码生成/验证、短信/邮箱验证码生成、MFA令牌生成等功能
 */
@Component
public class MfaUtils {

    private static final Logger logger = LoggerFactory.getLogger(MfaUtils.class);

    /** 默认验证码长度 */
    private static final int DEFAULT_CODE_LENGTH = 6;

    /** TOTP默认时间步长（秒） */
    private static final int DEFAULT_TIME_STEP = 30;

    /** HMAC算法 */
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    /** 安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成TOTP密钥
     * @return Base32编码的TOTP密钥
     */
    public static String generateTotpSecret() {
        byte[] bytes = new byte[20];
        SECURE_RANDOM.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    /**
     * 生成TOTP验证码（使用当前时间）
     * @param secret TOTP密钥
     * @param timeStep 时间步长（秒）
     * @param codeLength 验证码长度
     * @return TOTP验证码
     */
    public static String generateTotpCode(String secret, int timeStep, int codeLength) {
        try {
            Base32 base32 = new Base32();
            byte[] keyBytes = base32.decode(secret);
            long currentTime = System.currentTimeMillis() / 1000 / timeStep;

            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));

            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (currentTime & 0xff);
                currentTime >>= 8;
            }

            byte[] hash = hmac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xf;

            int code = ((hash[offset] & 0x7f) << 24) |
                      ((hash[offset + 1] & 0xff) << 16) |
                      ((hash[offset + 2] & 0xff) << 8) |
                      (hash[offset + 3] & 0xff);

            code %= (int) Math.pow(10, codeLength);
            return String.format("%0" + codeLength + "d", code);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("生成TOTP验证码失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证TOTP验证码（使用默认参数）
     * @param secret TOTP密钥
     * @param code 验证码
     * @return 是否验证成功
     */
    public static boolean verifyTotpCode(String secret, String code) {
        return verifyTotpCode(secret, code, DEFAULT_TIME_STEP, DEFAULT_CODE_LENGTH, 1);
    }

    /**
     * 验证TOTP验证码（自定义参数）
     * @param secret TOTP密钥
     * @param code 验证码
     * @param timeStep 时间步长（秒）
     * @param codeLength 验证码长度
     * @param window 时间窗口（允许的时间步长偏差，1表示前后各1个步长）
     * @return 是否验证成功
     */
    public static boolean verifyTotpCode(String secret, String code, int timeStep, int codeLength, int window) {
        for (int i = -window; i <= window; i++) {
            long adjustedTime = (System.currentTimeMillis() / 1000 / timeStep) + i;
            String expectedCode = generateTotpCodeByTime(secret, adjustedTime, codeLength);
            if (code.equals(expectedCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据指定时间计数器生成TOTP验证码
     * @param secret TOTP密钥
     * @param timeCounter 时间计数器值
     * @param codeLength 验证码长度
     * @return TOTP验证码
     */
    private static String generateTotpCodeByTime(String secret, long timeCounter, int codeLength) {
        try {
            Base32 base32 = new Base32();
            byte[] keyBytes = base32.decode(secret);

            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));

            byte[] data = new byte[8];
            long temp = timeCounter;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (temp & 0xff);
                temp >>= 8;
            }

            byte[] hash = hmac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xf;

            int code = ((hash[offset] & 0x7f) << 24) |
                      ((hash[offset + 1] & 0xff) << 16) |
                      ((hash[offset + 2] & 0xff) << 8) |
                      (hash[offset + 3] & 0xff);

            code %= (int) Math.pow(10, codeLength);
            return String.format("%0" + codeLength + "d", code);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("生成TOTP验证码失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成短信验证码（纯数字）
     * @param length 验证码长度
     * @return 短信验证码
     */
    public static String generateSmsCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 生成短信验证码（默认6位纯数字）
     * @return 短信验证码
     */
    public static String generateSmsCode() {
        return generateSmsCode(DEFAULT_CODE_LENGTH);
    }

    /**
     * 生成邮箱验证码（大写字母+数字）
     * @param length 验证码长度
     * @return 邮箱验证码
     */
    public static String generateEmailCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }

    /**
     * 生成邮箱验证码（默认8位大写字母+数字）
     * @return 邮箱验证码
     */
    public static String generateEmailCode() {
        return generateEmailCode(8);
    }

    /**
     * 生成MFA令牌（随机Base32编码字符串）
     * @return MFA令牌
     */
    public static String generateMfaToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }
}
