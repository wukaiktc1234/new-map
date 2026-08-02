package com.foodtraceability.security.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class PasswordUtils {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /**
     * 加密密码
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String encryptPassword(String password) {
        return encoder.encode(password);
    }

    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 验证密码强度
     * @param password 密码
     * @return 是否符合强度要求
     */
    public static boolean isValidPassword(String password) {
        // 密码强度验证
        // 至少8位，包含大小写字母、数字和特殊字符
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(pattern);
    }

    /**
     * 获取密码强度等级
     * @param password 密码
     * @return 强度等级（1-5，5最强）
     */
    public static int getPasswordStrength(String password) {
        int strength = 0;

        // 长度检查
        if (password.length() >= 8) {
            strength++;
        }
        if (password.length() >= 12) {
            strength++;
        }

        // 包含小写字母
        if (password.matches(".*[a-z].*")) {
            strength++;
        }

        // 包含大写字母
        if (password.matches(".*[A-Z].*")) {
            strength++;
        }

        // 包含数字
        if (password.matches(".*\\d.*")) {
            strength++;
        }

        // 包含特殊字符
        if (password.matches(".*[@$!%*?&].*")) {
            strength++;
        }

        return Math.min(strength, 5);
    }

    /**
     * 生成随机密码
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$!%*?&";
        StringBuilder password = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}
