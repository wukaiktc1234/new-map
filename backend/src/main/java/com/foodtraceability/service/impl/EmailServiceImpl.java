package com.foodtraceability.service.impl;

import com.foodtraceability.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件发送服务实现类
 * 基于Spring JavaMailSender实现真实邮件发送
 * 支持HTML格式、附件、批量发送等功能
 */
@Service
@ConditionalOnProperty(name = "spring.mail.host", matchIfMissing = false)
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);


    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** 发件人名称（可从配置中读取） */
    private static final String FROM_NAME = "食品溯源系统";

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String htmlContent) throws Exception {
        validateParams(to, subject);

        long startTime = System.currentTimeMillis();
        logger.info("开始发送邮件: to={}, subject={}", to, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(FROM_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // HTML格式

            mailSender.send(message);
            long costTime = System.currentTimeMillis() - startTime;
            logger.info("邮件发送成功: to={}, subject={}, 耗时={}ms", to, subject, costTime);
        } catch (MessagingException e) {
            logger.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String htmlContent,
                                        List<File> attachments) throws Exception {
        validateParams(to, subject);

        if (attachments == null || attachments.isEmpty()) {
            // 无附件时降级为普通发送
            sendEmail(to, subject, htmlContent);
            return;
        }

        long startTime = System.currentTimeMillis();
        logger.info("开始发送带附件邮件: to={}, subject={}, attachmentCount={}",
                to, subject, attachments.size());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart=true 表示支持附件
            MimeMessageHelper helper = new MimeMessageHelper(message, true,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(FROM_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // HTML格式

            // 添加附件
            for (File attachment : attachments) {
                if (attachment != null && attachment.exists()) {
                    helper.addAttachment(attachment.getName(), attachment);
                    logger.debug("添加附件: {}", attachment.getName());
                }
            }

            mailSender.send(message);
            long costTime = System.currentTimeMillis() - startTime;
            logger.info("带附件邮件发送成功: to={}, subject={}, 附件数={}, 耗时={}ms",
                    to, subject, attachments.size(), costTime);
        } catch (MessagingException e) {
            logger.error("带附件邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
            throw new RuntimeException("带附件邮件发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int batchSendEmail(List<String> recipients, String subject, String htmlContent) {
        if (recipients == null || recipients.isEmpty()) {
            logger.warn("批量发送收件人列表为空");
            return 0;
        }

        int successCount = 0;
        int failCount = 0;

        for (String recipient : recipients) {
            try {
                sendEmail(recipient, subject, htmlContent);
                successCount++;
            } catch (Exception e) {
                failCount++;
                logger.error("批量发送单条失败: recipient={}, error={}", recipient, e.getMessage());
            }
        }

        logger.info("批量发送完成: 总数={}, 成功={}, 失败={}",
                recipients.size(), successCount, failCount);
        return successCount;
    }

    /**
     * 验证基本参数
     *
     * @param to      收件人地址
     * @param subject 邮件主题
     */
    private void validateParams(String to, String subject) {
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("收件人地址不能为空");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("邮件主题不能为空");
        }
        // 简单的邮箱格式校验
        if (!to.contains("@")) {
            throw new IllegalArgumentException("收件人邮箱格式不正确: " + to);
        }
    }
}
