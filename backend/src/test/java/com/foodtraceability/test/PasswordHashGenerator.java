package com.foodtraceability.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        
        String password = "123456";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("Length: " + hash.length());
        
        // 验证密码
        boolean matches = encoder.matches(password, hash);
        System.out.println("Matches: " + matches);
    }
}