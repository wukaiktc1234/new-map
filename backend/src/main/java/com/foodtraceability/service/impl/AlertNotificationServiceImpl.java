package com.foodtraceability.service.impl;

import com.foodtraceability.entity.DeviceAlert;
import com.foodtraceability.service.AlertNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 告警通知服务实现类
 * 实现多种告警通知方式，如邮件、短信等
 */
@Service
public class AlertNotificationServiceImpl implements AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationServiceImpl.class);

    // 邮件发送服务，需要在配置文件中配置邮件服务器
    private final JavaMailSender mailSender;

    // WebSocket消息发送模板，用于实时推送告警通知
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param mailSender 邮件发送服务，可选依赖
     * @param messagingTemplate WebSocket消息发送模板
     */
    public AlertNotificationServiceImpl(@Nullable JavaMailSender mailSender, SimpMessagingTemplate messagingTemplate) {
        this.mailSender = mailSender;
        this.messagingTemplate = messagingTemplate;
    }

    // 邮件接收者列表，实际项目中应从配置或数据库获取
    private static final String[] EMAIL_RECIPIENTS = {"admin@example.com", "support@example.com"};

    // 短信接收者列表，实际项目中应从配置或数据库获取
    private static final String[] SMS_RECIPIENTS = {"13800138000"};

    /**
     * 发送告警通知
     * @param alert 告警信息
     */
    @Override
    public void sendNotification(DeviceAlert alert) {
        log.info("发送告警通知: 设备类型={}, 告警类型={}, 告警级别={}", 
                alert.getDeviceType(), alert.getAlertType(), alert.getAlertLevel());

        try {
            // 根据告警级别决定通知方式
            if ("CRITICAL".equals(alert.getAlertLevel()) || "ERROR".equals(alert.getAlertLevel())) {
                // 严重或错误级别的告警，发送邮件和短信通知
                sendEmailNotification(alert);
                sendSmsNotification(alert);
            } else if ("WARNING".equals(alert.getAlertLevel())) {
                // 警告级别的告警，发送邮件通知
                sendEmailNotification(alert);
            }
            
            // 所有告警都发送WebSocket通知和系统内部通知
            sendWebSocketNotification(alert);
            sendSystemNotification(alert);
        } catch (Exception e) {
            log.error("发送告警通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送邮件告警
     * @param alert 告警信息
     */
    @Override
    public void sendEmailNotification(DeviceAlert alert) {
        if (mailSender == null) {
            log.warn("邮件发送服务未配置，无法发送邮件告警");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("alerts@example.com");
            message.setTo(EMAIL_RECIPIENTS);
            message.setSubject("【设备告警】" + alert.getDeviceType() + " - " + alert.getAlertType());
            message.setText(buildEmailContent(alert));
            
            mailSender.send(message);
            log.info("邮件告警发送成功: 设备类型={}, 告警类型={}", alert.getDeviceType(), alert.getAlertType());
        } catch (Exception e) {
            log.error("发送邮件告警失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送短信告警
     * @param alert 告警信息
     */
    @Override
    public void sendSmsNotification(DeviceAlert alert) {
        try {
            // 实际项目中应调用第三方短信服务API
            // 这里仅作模拟
            for (String recipient : SMS_RECIPIENTS) {
                log.info("发送短信告警到 {}: 设备类型={}, 告警类型={}, 告警描述={}", 
                        recipient, alert.getDeviceType(), alert.getAlertType(), alert.getAlertDescription());
                // 调用第三方短信服务API的代码示例：
                // smsService.sendSms(recipient, "【设备告警】" + alert.getAlertDescription());
            }
            log.info("短信告警发送成功: 设备类型={}, 告警类型={}", alert.getDeviceType(), alert.getAlertType());
        } catch (Exception e) {
            log.error("发送短信告警失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送系统内部通知
     * @param alert 告警信息
     */
    @Override
    public void sendSystemNotification(DeviceAlert alert) {
        try {
            // 实际项目中应将告警信息存储到数据库中，供前端查询
            log.info("发送系统内部告警通知: 设备类型={}, 告警类型={}, 告警描述={}", 
                    alert.getDeviceType(), alert.getAlertType(), alert.getAlertDescription());
            // 存储到数据库的代码示例：
            // alertRepository.save(alert);
        } catch (Exception e) {
            log.error("发送系统内部告警通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送WebSocket通知
     * @param alert 告警信息
     */
    @Override
    public void sendWebSocketNotification(DeviceAlert alert) {
        try {
            // 向所有订阅了告警主题的客户端推送告警通知
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            log.info("WebSocket告警通知推送成功: 设备类型={}, 告警类型={}", alert.getDeviceType(), alert.getAlertType());
        } catch (Exception e) {
            log.error("发送WebSocket告警通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建邮件内容
     * @param alert 告警信息
     * @return 邮件内容
     */
    private String buildEmailContent(DeviceAlert alert) {
        StringBuilder content = new StringBuilder();
        content.append("尊敬的管理员：\n\n");
        content.append("系统检测到设备状态异常，详细信息如下：\n\n");
        content.append("设备类型：").append(alert.getDeviceType()).append("\n");
        content.append("设备名称：").append(alert.getDeviceName()).append("\n");
        content.append("告警类型：").append(alert.getAlertType()).append("\n");
        content.append("告警级别：").append(alert.getAlertLevel()).append("\n");
        content.append("告警描述：").append(alert.getAlertDescription()).append("\n");
        content.append("触发时间：").append(alert.getTriggerTime()).append("\n");
        content.append("\n请及时处理，确保设备正常运行！\n\n");
        content.append("设备管理系统\n");
        content.append(new java.util.Date());
        return content.toString();
    }
}
