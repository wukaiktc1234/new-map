package com.foodtraceability.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt密码生成器 - 临时用于生成admin123密码
 */
public class BCryptGenerator {

    public static void main(String[] args) {
        String password = "admin123";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encryptedPassword = encoder.encode(password);

        System.out.println("明文密码: " + password);
        System.out.println("BCrypt加密密码: " + encryptedPassword);

        boolean matches = encoder.matches(password, encryptedPassword);
        System.out.println("密码匹配: " + matches);
    }
}
