package com.foodtraceability.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MfaConfig {

    @Value("${spring.security.mfa.enabled:true}")
    private boolean enabled;

    @Value("${spring.security.mfa.totp.time-step:30}")
    private int totpTimeStep;

    @Value("${spring.security.mfa.totp.code-length:6}")
    private int totpCodeLength;

    @Value("${spring.security.mfa.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${spring.security.mfa.email.enabled:true}")
    private boolean emailEnabled;

    public boolean isEnabled() {
        return enabled;
    }

    public int getTotpTimeStep() {
        return totpTimeStep;
    }

    public int getTotpCodeLength() {
        return totpCodeLength;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }
}
