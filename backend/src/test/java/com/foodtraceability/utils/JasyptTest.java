package com.foodtraceability.utils;

import org.jasypt.util.text.BasicTextEncryptor;
import org.junit.jupiter.api.Test;

public class JasyptTest {
    @Test
    public void encrypt() {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword("my-traceability-secret-key-2025");

        String dbPassword = "123456";
        String jwtSecret = "food-traceability-system-secure-jwt-secret-key-for-production-2025";
        String mailPassword = "demoPassword123";

        System.out.println("ENC_START");
        System.out.println("DB Password Encrypted: " + textEncryptor.encrypt(dbPassword));
        System.out.println("JWT Secret Encrypted: " + textEncryptor.encrypt(jwtSecret));
        System.out.println("Mail Password Encrypted: " + textEncryptor.encrypt(mailPassword));
        System.out.println("ENC_END");
    }
}
