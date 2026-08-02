package com.foodtraceability.utils;

import java.util.regex.Pattern;

/**
 * 密码验证工具类
 * 用于验证密码强度
 */
public class PasswordValidator {

    // 密码强度等级枚举
    public enum StrengthLevel {
        WEAK,       // 弱
        MEDIUM,     // 中等
        STRONG      // 强
    }

    // 密码规则常量
    private static final int MIN_LENGTH = 8;
    private static final int MIN_LENGTH_STRONG = 12;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?{}|<>]");

    /**
     * 验证密码强度
     * @param password 密码
     * @return 密码强度等级
     */
    public static StrengthLevel validatePasswordStrength(String password) {
        if (password == null) {
            return StrengthLevel.WEAK;
        }

        int score = 0;

        // 长度检查
        if (password.length() >= MIN_LENGTH) {
            score++;
        }
        if (password.length() >= MIN_LENGTH_STRONG) {
            score++;
        }

        // 包含大写字母
        if (UPPERCASE_PATTERN.matcher(password).find()) {
            score++;
        }

        // 包含小写字母
        if (LOWERCASE_PATTERN.matcher(password).find()) {
            score++;
        }

        // 包含数字
        if (DIGIT_PATTERN.matcher(password).find()) {
            score++;
        }

        // 包含特殊字符
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            score++;
        }

        // 根据分数返回强度等级
        if (score >= 5) {
            return StrengthLevel.STRONG;
        } else if (score >= 3) {
            return StrengthLevel.MEDIUM;
        } else {
            return StrengthLevel.WEAK;
        }
    }

    /**
     * 检查密码是否符合最小强度要求
     * @param password 密码
     * @return 是否符合要求
     */
    public static boolean isPasswordStrongEnough(String password) {
        return validatePasswordStrength(password) != StrengthLevel.WEAK;
    }

    /**
     * 获取密码强度提示信息
     * @param password 密码
     * @return 提示信息
     */
    public static String getPasswordStrengthMessage(String password) {
        StrengthLevel level = validatePasswordStrength(password);
        switch (level) {
            case WEAK:
                return "密码强度弱，请包含至少8个字符，包括大小写字母、数字和特殊字符";
            case MEDIUM:
                return "密码强度中等，请考虑增加长度或特殊字符";
            case STRONG:
                return "密码强度强";
            default:
                return "密码强度未知";
        }
    }

    /**
     * 验证密码是否符合要求
     * @param password 密码
     * @throws IllegalArgumentException 如果密码不符合要求
     */
    public static void validatePassword(String password) throws IllegalArgumentException {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("密码长度至少为8个字符");
        }
        if (!isPasswordStrongEnough(password)) {
            throw new IllegalArgumentException(getPasswordStrengthMessage(password));
        }
    }
}
