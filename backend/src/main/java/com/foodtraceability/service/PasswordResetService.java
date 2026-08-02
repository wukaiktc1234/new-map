package com.foodtraceability.service;

public interface PasswordResetService {

    void sendVerificationCode(String usernameOrEmail, String type);

    String verifyForResetPassword(String usernameOrEmail, String emailCode, String phoneCode);

    void resetPasswordWithToken(String resetToken, String newPassword);

    boolean validateResetToken(String resetToken);
}
