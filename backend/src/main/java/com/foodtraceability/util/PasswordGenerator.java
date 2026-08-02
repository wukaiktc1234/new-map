package com.foodtraceability.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {
    
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    public String generatePassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }
    
    public boolean matches(String plainPassword, String encodedPassword) {
        return encoder.matches(plainPassword, encodedPassword);
    }
    
    public static void main(String[] args) {
        PasswordGenerator generator = new PasswordGenerator();
        
        String password = "Test@123456";
        String encodedPassword = generator.generatePassword(password);
        
        System.out.println("原始密码: " + password);
        System.out.println("加密后的密码: " + encodedPassword);
        System.out.println("");
        System.out.println("SQL语句:");
        System.out.println("INSERT INTO users (id, username, password, email, status, roles) VALUES (");
        System.out.println("  999,");
        System.out.println("  'test-admin',");
        System.out.println("  '" + encodedPassword + "',");
        System.out.println("  'test-admin@example.com',");
        System.out.println("  '1',");
        System.out.println("  '[\"ROLE_ADMIN\"]');");
    }
}
