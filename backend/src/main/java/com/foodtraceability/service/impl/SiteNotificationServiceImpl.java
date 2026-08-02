package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.foodtraceability.entity.MsgTemplate;
import com.foodtraceability.entity.Notification;
import com.foodtraceability.mapper.MsgTemplateMapper;
import com.foodtraceability.mapper.NotificationMapper;
import com.foodtraceability.service.NotificationDataService;
import com.foodtraceability.service.SiteNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 站内通知服务实现类
 * 提供站内通知的创建、查询、已读管理、WebSocket实时推送等功能
 */
@Service
public class SiteNotificationServiceImpl implements SiteNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SiteNotificationServiceImpl.class);

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final NotificationMapper notificationMapper;
    private final NotificationDataService notificationDataService;
    private final MsgTemplateMapper msgTemplateMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public SiteNotificationServiceImpl(NotificationMapper notificationMapper,
                                        NotificationDataService notificationDataService,
                                        MsgTemplateMapper msgTemplateMapper,
                                        @org.springframework.lang.Nullable SimpMessagingTemplate messagingTemplate) {
        this.notificationMapper = notificationMapper;
        this.notificationDataService = notificationDataService;
        this.msgTemplateMapper = msgTemplateMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification createNotification(Long userId, String title, String content,
                                            String type, String priority,
                                            Long businessId, String businessType,
                                            Long senderId, String senderName) {
        return createNotification(userId, title, content, type, priority,
                businessId, businessType, senderId, senderName, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification createNotification(Long userId, String title, String content,
                                            String type, String priority,
                                            Long businessId, String businessType,
                                            Long senderId, String senderName,
                                            String extraData) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setPriority(priority != null ? priority : "NORMAL");
        notification.setBusinessId(businessId);
        notification.setBusinessType(businessType);
        notification.setSenderId(senderId);
        notification.setSenderName(senderName);
        notification.setExtraData(extraData);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());

        notificationMapper.insert(notification);
        notificationDataService.clearUserNotificationCache(userId);

        pushNotificationToUser(userId, notification);

        logger.info("创建站内通知: id={}, userId={}, type={}", notification.getId(), userId, type);
        return notification;
    }

    @Override
    public List<Notification> getNotificationsByUserId(Long userId, Integer isRead, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1 || size > 100) size = 20;

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        return notificationMapper.selectList(wrapper);
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        return notificationDataService.countUnreadByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Long notificationId, Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
               .eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0)
               .set(Notification::getIsRead, 1)
               .set(Notification::getReadTime, LocalDateTime.now())
               .set(Notification::getUpdateTime, LocalDateTime.now());

        int rows = notificationMapper.update(null, wrapper);
        if (rows > 0) {
            notificationDataService.clearNotificationCache(notificationId);
            notificationDataService.clearUserNotificationCache(userId);
        }
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0)
               .set(Notification::getIsRead, 1)
               .set(Notification::getReadTime, LocalDateTime.now())
               .set(Notification::getUpdateTime, LocalDateTime.now());

        int rows = notificationMapper.update(null, wrapper);
        if (rows > 0) {
            notificationDataService.clearUserNotificationCache(userId);
        }
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotification(Long notificationId, Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
               .eq(Notification::getUserId, userId);

        int rows = notificationMapper.delete(wrapper);
        if (rows > 0) {
            notificationDataService.clearNotificationCache(notificationId);
            notificationDataService.clearUserNotificationCache(userId);
        }
        return rows > 0;
    }

    @Override
    public void pushNotificationToUser(Long userId, Notification notification) {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        "/queue/notifications",
                        notification
                );
                logger.debug("WebSocket推送通知: userId={}", userId);
            } catch (Exception e) {
                logger.warn("WebSocket推送失败: userId={}", userId, e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBusinessNotification(String templateCode, Long userId, Map<String, Object> variables,
                                          String bizType, String bizId, String priority) {
        MsgTemplate template = msgTemplateMapper.selectOne(
                new LambdaQueryWrapper<MsgTemplate>()
                        .eq(MsgTemplate::getTemplateCode, templateCode)
                        .eq(MsgTemplate::getStatus, 1)
        );

        String title;
        String content;

        if (template != null) {
            title = renderTemplate(template.getSubjectPattern(), variables);
            content = renderTemplate(template.getContentPattern(), variables);
        } else {
            title = "系统通知";
            content = variables.toString();
            logger.warn("通知模板不存在或未启用: templateCode={}", templateCode);
        }

        Long businessId = null;
        try {
            businessId = bizId != null ? Long.parseLong(bizId) : null;
        } catch (NumberFormatException ignored) {
        }

        createNotification(userId, title, content, bizType, priority,
                businessId, bizType, null, "SYSTEM", null);
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            matcher.appendReplacement(sb, value != null ? value.toString() : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
