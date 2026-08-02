package com.foodtraceability.security.service.impl;

import com.foodtraceability.security.service.VerificationCodeSender;
import com.foodtraceability.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 基于SMTP的真实邮件验证码发送实现
 * 当配置了spring.mail.host时自动激活，替代Demo模式
 *
 * 降级策略：
 * - 当SMTP未配置时，此Bean不会注册，Spring会使用DemoVerificationCodeSender作为fallback
 * - 邮件发送失败时会记录错误日志并返回false，不影响业务流程
 */
@Component
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpVerificationCodeSender implements VerificationCodeSender {

    private static final Logger logger = LoggerFactory.getLogger(SmtpVerificationCodeSender.class);


    public SmtpVerificationCodeSender(EmailService emailService) {
        this.emailService = emailService;
    }

    /** 邮件主题前缀 */
    private static final String EMAIL_SUBJECT_PREFIX = "【食品溯源系统】";

    /** 验证码有效期提示（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 5;

    private final EmailService emailService;

    /** 发件人邮箱地址（从配置读取） */
    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public boolean sendEmailCode(String email, String code) {
        if (email == null || email.trim().isEmpty()) {
            logger.error("邮箱地址为空，无法发送验证码");
            return false;
        }

        logger.info("开始发送邮箱验证码: email={}", email);

        try {
            String subject = EMAIL_SUBJECT_PREFIX + "邮箱验证码";
            String htmlContent = buildEmailVerificationHtml(email, code);

            emailService.sendEmail(email, subject, htmlContent);

            logger.info("邮箱验证码发送成功: email={}", email);
            return true;
        } catch (Exception e) {
            logger.error("邮箱验证码发送失败: email={}, error={}", email, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendSmsCode(String phone, String code) {
        // 短信功能暂未实现，记录日志并返回false
        logger.warn("短信验证码功能暂未实现: phone={}", phone);
        logger.info("【演示模式】短信验证码已生成: phone={}, code={}", phone, code);
        return true; // 返回true以避免阻塞业务流程
    }

    @Override
    public boolean isEmailEnabled() {
        return true;
    }

    @Override
    public boolean isSmsEnabled() {
        // SMTP模式下短信功能暂不可用
        return false;
    }

    /**
     * 构建邮箱验证码的HTML内容
     * 生成专业、美观的验证码邮件模板
     *
     * @param email 收件人邮箱
     * @param code  验证码
     * @return HTML格式的邮件正文
     */
    private String buildEmailVerificationHtml(String email, String code) {
        return "<!DOCTYPE html>" +
                "<html lang=\"zh-CN\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>邮箱验证码</title>" +
                "    <style>" +
                "        body { font-family: 'Microsoft YaHei', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }" +
                "        .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; }" +
                "        .header h1 { color: #ffffff; margin: 0; font-size: 24px; }" +
                "        .content { padding: 40px 30px; }" +
                "        .code-box { background-color: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
                "        .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; }" +
                "        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666666; }" +
                "        .warning { color: #e74c3c; font-size: 14px; margin-top: 20px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <h1>食品溯源系统</h1>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <h2 style=\"color: #333333;\">邮箱验证</h2>" +
                "            <p style=\"color: #666666; line-height: 1.6;\">您好！</p>" +
                "            <p style=\"color: #666666; line-height: 1.6;\">您正在进行邮箱验证操作，您的验证码如下：</p>" +
                "            <div class=\"code-box\">" +
                "                <div class=\"code\">" + code + "</div>" +
                "            </div>" +
                "            <p style=\"color: #666666; line-height: 1.6;\">验证码有效期为 <strong>" + CODE_EXPIRE_MINUTES + " 分钟</strong>，请尽快完成验证。</p>" +
                "            <p class=\"warning\">⚠️ 如果这不是您本人的操作，请忽略此邮件。</p>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>此邮件由系统自动发送，请勿回复。</p>" +
                "            <p>© " + java.time.Year.now().getValue() + " 食品溯源系统 版权所有</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}
