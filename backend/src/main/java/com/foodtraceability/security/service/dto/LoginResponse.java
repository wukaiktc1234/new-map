package com.foodtraceability.security.service.dto;

import com.foodtraceability.security.model.JwtToken;
import com.foodtraceability.security.model.SecurityUser;

public class LoginResponse {

    private boolean success;
    private String message;
    private boolean requiresMfa;
    private String mfaToken;
    private JwtToken jwtToken;
    private SecurityUser user;

    // Getter methods
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRequiresMfa() {
        return requiresMfa;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public JwtToken getJwtToken() {
        return jwtToken;
    }

    public SecurityUser getUser() {
        return user;
    }

    // Setter methods
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRequiresMfa(boolean requiresMfa) {
        this.requiresMfa = requiresMfa;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }

    public void setJwtToken(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
    }

    public void setUser(SecurityUser user) {
        this.user = user;
    }

    public static LoginResponse success(JwtToken jwtToken, SecurityUser user) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(true);
        response.setMessage("登录成功");
        response.setRequiresMfa(false);
        response.setJwtToken(jwtToken);
        response.setUser(user);
        return response;
    }

    public static LoginResponse mfaRequired(String mfaToken) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(false);
        response.setMessage("需要多因素认证");
        response.setRequiresMfa(true);
        response.setMfaToken(mfaToken);
        return response;
    }

    public static LoginResponse failure(String message) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setRequiresMfa(false);
        return response;
    }
}
