package com.foodtraceability.security.service;

import com.foodtraceability.security.model.MfaToken;

import java.io.Serializable;
import java.util.Optional;

public interface MfaService {

    /**
     * 生成MFA令牌
     * @param userId 用户ID
     * @return MFA令牌
     */
    MfaToken generateMfaToken(String userId);

    /**
     * 验证MFA代码
     * @param userId 用户ID
     * @param mfaToken MFA令牌
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyMfaCode(String userId, String mfaToken, String code);

    /**
     * 生成TOTP密钥
     * @param userId 用户ID
     * @return TOTP密钥
     */
    String generateTotpSecret(String userId);

    /**
     * 验证TOTP代码
     * @param secret TOTP密钥
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyTotpCode(String secret, String code);

    /**
     * 发送短信验证码
     * @param userId 用户ID
     * @param phoneNumber 手机号码
     * @return 验证码
     */
    Optional<String> sendSmsCode(String userId, String phoneNumber);

    /**
     * 发送邮箱验证码
     * @param userId 用户ID
     * @param email 邮箱地址
     * @return 验证码
     */
    Optional<String> sendEmailCode(String userId, String email);

    /**
     * 验证短信验证码
     * @param userId 用户ID
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifySmsCode(String userId, String code);

    /**
     * 验证邮箱验证码
     * @param userId 用户ID
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyEmailCode(String userId, String code);

    /**
     * 启用MFA
     * @param userId 用户ID
     * @param mfaType MFA类型（totp/sms/email）
     * @param secret MFA密钥
     * @return 是否成功
     */
    boolean enableMfa(String userId, String mfaType, String secret);

    /**
     * 禁用MFA
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean disableMfa(String userId);

    /**
     * 检查用户是否启用了MFA
     * @param userId 用户ID
     * @return 是否启用了MFA
     */
    boolean isMfaEnabled(String userId);

    /**
     * 获取用户MFA配置
     * @param userId 用户ID
     * @return MFA配置
     */
    Optional<MfaConfig> getMfaConfig(String userId);

    /**
     * MFA配置类
     */
    class MfaConfig implements Serializable {

        private static final long serialVersionUID = 1L;

        private boolean enabled;
        private String mfaType;
        private String secret;
        private boolean verified;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMfaType() {
            return mfaType;
        }

        public void setMfaType(String mfaType) {
            this.mfaType = mfaType;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public boolean isVerified() {
            return verified;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }
    }
}
