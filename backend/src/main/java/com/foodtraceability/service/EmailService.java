package com.foodtraceability.service;

import java.io.File;
import java.util.List;

/**
 * 邮件发送服务接口
 * 提供基于Spring JavaMailSender的邮件发送能力
 * 支持HTML格式、附件、异步发送等功能
 */
public interface EmailService {

    /**
     * 发送纯HTML格式邮件
     *
     * @param to         收件人地址
     * @param subject    邮件主题
     * @param htmlContent HTML格式的邮件正文
     * @throws Exception 发送失败时抛出异常
     */
    void sendEmail(String to, String subject, String htmlContent) throws Exception;

    /**
     * 发送带附件的HTML格式邮件
     *
     * @param to           收件人地址
     * @param subject      邮件主题
     * @param htmlContent  HTML格式的邮件正文
     * @param attachments  附件列表
     * @throws Exception 发送失败时抛出异常
     */
    void sendEmailWithAttachment(String to, String subject, String htmlContent,
                                 List<File> attachments) throws Exception;

    /**
     * 批量发送相同内容的邮件（每个收件人独立发送）
     *
     * @param recipients  收件人地址列表
     * @param subject     邮件主题
     * @param htmlContent HTML格式的邮件正文
     * @return 发送成功的收件人数量
     */
    int batchSendEmail(List<String> recipients, String subject, String htmlContent);
}
