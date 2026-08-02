package com.foodtraceability.service.sender;

import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.entity.Notification;
import com.foodtraceability.service.SiteNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SITE_MSG 渠道发送器
 *
 * <p>委托 {@link SiteNotificationService#createNotification} 落库 notification 表 + WebSocket 推送，
 * 解决 spec F-001 描述的"站内信双系统割裂"问题。</p>
 *
 * <p>对应 plan.md ADR-001（SITE_MSG 委托 SiteNotificationService）+ ADR-004（Strategy 模式）。</p>
 *
 * <p>关联机制：
 * <ul>
 *   <li>record.recipient 存储的是 userId 字符串</li>
 *   <li>record.createUserId / createUsername 复用作为 senderId / senderName</li>
 *   <li>发送成功后回填 record.notificationId，建立 msg_send_record ↔ notification 关联</li>
 * </ul></p>
 *
 * <p>状态转换：
 * <ul>
 *   <li>成功：record.sendStatus = SUCCESS（2）+ 回填 notificationId</li>
 *   <li>失败：抛出 Exception，由 NotificationMessageConsumer 触发重试/死信</li>
 * </ul></p>
 */
@Component
public class SiteMsgChannelSender implements MessageChannelSender {

    private static final Logger logger = LoggerFactory.getLogger(SiteMsgChannelSender.class);

    /** SITE_MSG 渠道标识 */
    private static final String CHANNEL_SITE_MSG = "SITE_MSG";

    /** 默认通知类型（事件驱动场景） */
    private static final String DEFAULT_NOTIFICATION_TYPE = "SYSTEM";

    /** 默认优先级 */
    private static final String DEFAULT_PRIORITY = "NORMAL";

    /** 发送状态：成功 */
    private static final int STATUS_SUCCESS = 2;

    private final SiteNotificationService siteNotificationService;

    /**
     * 构造函数注入 SiteNotificationService。
     *
     * @param siteNotificationService 站内通知服务
     */
    public SiteMsgChannelSender(SiteNotificationService siteNotificationService) {
        this.siteNotificationService = siteNotificationService;
    }

    @Override
    public String getChannel() {
        return CHANNEL_SITE_MSG;
    }

    @Override
    public void send(MsgSendRecord record) throws Exception {
        String recipient = record.getRecipient();
        Long recordId = record.getRecordId();

        logger.info("SITE_MSG 渠道发送开始: recipient(userId)={}, recordId={}", recipient, recordId);

        // FIX-004(C2): 前置 null 检查,避免 Long.parseLong(null) 抛 NPE 而非 NumberFormatException
        Long userId;
        if (recipient == null || recipient.isEmpty()) {
            logger.error("SITE_MSG 渠道 recipient 为空: recordId={}", recordId);
            throw new IllegalArgumentException(
                    "SITE_MSG 渠道 recipient 不能为空: recordId=" + recordId);
        }
        try {
            userId = Long.parseLong(recipient);
        } catch (NumberFormatException e) {
            logger.error("SITE_MSG 渠道 recipient 非 userId 格式: recipient={}, recordId={}",
                    recipient, recordId, e);
            throw new IllegalArgumentException(
                    "SITE_MSG 渠道 recipient 必须为 userId 数字字符串: " + recipient, e);
        }

        String title = record.getSubject();
        String content = record.getContent();
        String type = DEFAULT_NOTIFICATION_TYPE;
        String priority = DEFAULT_PRIORITY;
        Long businessId = parseBusinessId(record.getBizId());
        String businessType = record.getBizType();
        Long senderId = record.getCreateUserId();
        String senderName = record.getCreateUsername();

        try {
            Notification notification = siteNotificationService.createNotification(
                    userId, title, content, type, priority,
                    businessId, businessType, senderId, senderName);

            if (notification == null || notification.getId() == null) {
                throw new IllegalStateException(
                        "SiteNotificationService.createNotification 返回空对象, userId=" + userId);
            }

            record.setNotificationId(notification.getId());
            record.setSendStatus(STATUS_SUCCESS);

            logger.info("SITE_MSG 渠道发送成功: userId={}, notificationId={}, recordId={}",
                    userId, notification.getId(), recordId);
        } catch (Exception e) {
            logger.error("SITE_MSG 渠道发送失败: userId={}, recordId={}, error={}",
                    userId, recordId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 解析 bizId 为 Long 类型 businessId。
     *
     * <p>bizId 为空或非数字时返回 null，避免影响通知主体落库。</p>
     *
     * @param bizId 业务 ID 字符串
     * @return Long 类型 businessId，或 null
     */
    private Long parseBusinessId(String bizId) {
        if (bizId == null || bizId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(bizId);
        } catch (NumberFormatException e) {
            logger.debug("bizId 非数字格式, 保留为 null: bizId={}", bizId);
            return null;
        }
    }
}
