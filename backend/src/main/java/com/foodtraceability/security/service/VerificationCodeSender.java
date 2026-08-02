package com.foodtraceability.security.service;

/**
 * 验证码发送接口
 * 提供邮箱和短信验证码的发送能力
 */
public interface VerificationCodeSender {

    /**
     * 发送邮箱验证码
     * @param email 目标邮箱地址
     * @param code 验证码
     * @return 是否发送成功
     */
    boolean sendEmailCode(String email, String code);

    /**
     * 发送短信验证码
     * @param phone 目标手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    boolean sendSmsCode(String phone, String code);

    /**
     * 邮箱发送功能是否启用
     * @return 是否启用
     */
    boolean isEmailEnabled();

    /**
     * 短信发送功能是否启用
     * @return 是否启用
     */
    boolean isSmsEnabled();
}
