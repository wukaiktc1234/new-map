package com.foodtraceability.security.service.impl;

import com.foodtraceability.security.service.VerificationCodeSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 演示模式验证码发送实现
 * 将验证码输出到日志，而非真正发送邮件/短信
 * 实际部署时请替换为真实的邮件/短信发送服务实现
 */
@Primary
@Component
public class DemoVerificationCodeSender implements VerificationCodeSender {

    private static final Logger logger = LoggerFactory.getLogger(DemoVerificationCodeSender.class);

    @Override
    public boolean sendEmailCode(String email, String code) {
        logger.info("【演示模式】邮箱验证码已生成: email={}", email);
        logger.info("【演示模式】实际部署时请替换为真实的邮件发送服务");
        return true;
    }

    @Override
    public boolean sendSmsCode(String phone, String code) {
        logger.info("【演示模式】短信验证码已生成: phone={}", phone);
        logger.info("【演示模式】实际部署时请替换为真实的短信发送服务");
        return true;
    }

    @Override
    public boolean isEmailEnabled() {
        return true;
    }

    @Override
    public boolean isSmsEnabled() {
        return true;
    }
}
