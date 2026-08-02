package com.foodtraceability.exception;

import com.foodtraceability.dto.LoginResponse;

public class CaptchaRequiredException extends RuntimeException {
    
    private final LoginResponse loginResponse;
    
    public CaptchaRequiredException(String message, LoginResponse loginResponse) {
        super(message);
        this.loginResponse = loginResponse;
    }
    
    public LoginResponse getLoginResponse() {
        return loginResponse;
    }
}
