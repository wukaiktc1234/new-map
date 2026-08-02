package com.foodtraceability.service;

import com.foodtraceability.dto.MsgSendDTO;
import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.entity.MsgTemplate;
import com.foodtraceability.entity.Notification;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.MsgSendRecordMapper;
import com.foodtraceability.service.event.BusinessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 通知事件总线（核心组件）
 *
 * <p>基于 Spring ApplicationEvent + @TransactionalEventListener(AFTER_COMMIT) 实现跨模块事件分发。
 * 业务模块通过 ApplicationEventPublisher.publishEvent(BusinessEvent) 发布事件，
 * 本组件在业务事务提交后监听并分发到各通知渠道。</p>
 *
 * <p>对应 spec F-001（业务事件统一发布能力）+ F-005（事务安全保证）+
 * plan.md ADR-001（SITE_MSG 委托 SiteNotificationService）+ ADR-002（模板语法独立）。</p>
 *
 * <p>核心流程：
 * <ol>
 *   <li>监听 BusinessEvent（AFTER_COMMIT，业务事务回滚时不触发）</li>
 *   <li>查询 msg_template（按 eventType=templateCode）</li>
 *   <li>模板不存在/禁用 → 跳过 + WARN 日志</li>
 *   <li>渲染模板（{varName} 占位符替换，与现有 ${var} 语法独立）</li>
 *   <li>校验接收人（无效 ID 跳过 + WARN）</li>
 *   <li>按渠道分发：
 *     <ul>
 *       <li>SITE_MSG → SiteNotificationService.createNotification() + 同步写 msg_send_record</li>
 *       <li>EMAIL → NotificationService.sendMessage()（走 RabbitMQ 异步）</li>
 *       <li>SMS/WEBHOOK → NotificationService.sendMessage()（走 RabbitMQ，消费者跳过）</li>
 *     </ul>
 *   </li>
 * </ol></p>
 */
@Component
public class NotificationEventBus {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventBus.class);

    /** SITE_MSG 渠道标识 */
    private static final String CHANNEL_SITE_MSG = "SITE_MSG";

    /** EMAIL 渠道标识 */
    private static final String CHANNEL_EMAIL = "EMAIL";

    /** 模板状态：启用 */
    private static final int TEMPLATE_STATUS_ENABLED = 1;

    /** 收件人类型：1=EMAIL */
    private static final int RECIPIENT_TYPE_EMAIL = 1;

    /** 收件人类型：2=USER_ID */
    private static final int RECIPIENT_TYPE_USER_ID = 2;

    /** 触发类型：4=EVENT */
    private static final int TRIGGER_TYPE_EVENT = 4;

    /** 发送状态：成功 */
    private static final int SEND_STATUS_SUCCESS = 2;

    /** 默认通知类型（事件驱动场景） */
    private static final String DEFAULT_NOTIFICATION_TYPE = "SYSTEM";

    /** 默认优先级 */
    private static final String DEFAULT_PRIORITY = "NORMAL";

    /** FIX-010: 支持的渠道白名单 */
    private static final Set<String> SUPPORTED_CHANNELS =
            Set.of(CHANNEL_SITE_MSG, CHANNEL_EMAIL, "SMS", "WEBHOOK");

    /** FIX-023: 事件类型格式校验正则(仅允许字母/数字/点/下划线/连字符) */
    private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final SiteNotificationService siteNotificationService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final MsgSendRecordMapper msgSendRecordMapper;

    /**
     * 构造函数注入依赖。
     *
     * @param siteNotificationService 站内通知服务（SITE_MSG 渠道落库）
     * @param notificationService 通知服务（EMAIL/SMS/WEBHOOK 渠道走 RabbitMQ）
     * @param userService 用户服务（校验接收人 + 查询邮箱）
     * @param msgSendRecordMapper 发送记录 Mapper（SITE_MSG 同步写记录）
     */
    public NotificationEventBus(SiteNotificationService siteNotificationService,
                                 NotificationService notificationService,
                                 UserService userService,
                                 MsgSendRecordMapper msgSendRecordMapper) {
        this.siteNotificationService = siteNotificationService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.msgSendRecordMapper = msgSendRecordMapper;
    }

    /**
     * 监听业务事件（业务事务提交后触发）。
     *
     * <p>事务保证：业务事务回滚时此方法不会被调用，确保通知不会在业务失败时发送。</p>
     *
     * @param event 业务事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleBusinessEvent(BusinessEvent event) {
        String eventType = event.getEventType();

        // FIX-023: 事件类型格式校验(spec BC-014)
        if (eventType == null || !EVENT_TYPE_PATTERN.matcher(eventType).matches()) {
            logger.error("事件类型格式非法, 拒绝处理: eventType={}", eventType);
            return;
        }

        logger.info("收到业务事件: eventType={}, operatorId={}, recipientCount={}, channels={}",
                eventType, event.getOperatorId(),
                event.getRecipientUserIds().size(), event.getChannels());

        MsgTemplate template = notificationService.getTemplateByCode(eventType);
        if (template == null) {
            logger.warn("模板不存在, 跳过事件分发: eventType={}", eventType);
            return;
        }
        if (template.getStatus() == null
                || template.getStatus() != TEMPLATE_STATUS_ENABLED) {
            logger.warn("模板未启用, 跳过事件分发: eventType={}, status={}",
                    eventType, template.getStatus());
            return;
        }

        List<Long> validRecipients = validateRecipients(event.getRecipientUserIds());
        // FIX-025: 区分"原始列表为空"和"全部无效"两种场景
        if (validRecipients.isEmpty()) {
            if (event.getRecipientUserIds().isEmpty()) {
                logger.warn("接收人列表为空, 跳过事件分发: eventType={}", eventType);
            } else {
                logger.warn("接收人全部无效, 跳过事件分发: eventType={}, originalCount={}",
                        eventType, event.getRecipientUserIds().size());
            }
            return;
        }

        // FIX-018(M-4): 批量查询 User 缓存,避免 validateRecipients 和 resolveRecipient 双重 N+1
        Map<Long, User> userMap = batchGetUsers(validRecipients);

        String subject = renderTemplate(template.getSubjectPattern(), event.getVariables());
        String content = renderTemplate(template.getContentPattern(), event.getVariables());

        // FIX-011: 渠道去重,避免重复渠道双发
        Set<String> dedupChannels = new LinkedHashSet<>(event.getChannels());
        for (String channel : dedupChannels) {
            try {
                dispatchChannel(channel, template, event, validRecipients, userMap, subject, content);
            } catch (Exception e) {
                logger.error("渠道分发失败, 继续处理其他渠道: eventType={}, channel={}, error={}",
                        eventType, channel, e.getMessage(), e);
            }
        }
    }

    /**
     * 渲染模板（{varName} 占位符替换）。
     *
     * <p>与现有 SiteNotificationService 的 ${var} 语法独立，互不影响。
     * 缺失的变量占位符保留原样（不替换为空字符串），便于发现问题。</p>
     *
     * <p>FIX-015(M-1): 渲染前正则提取所有 {varName} 占位符,与 variables 对比,
     * 缺失变量记录 WARN 日志(spec F-006 / BC-009)。
     * FIX-016(M-2): 变量名含 password/token/secret 时自动脱敏为 ***(spec 5.2 安全要求)。
     * FIX-021(B6): toString 失败时保留占位符,不中断渲染(spec BC-013)。</p>
     *
     * @param template 模板字符串
     * @param variables 变量数据
     * @return 渲染后的字符串
     */
    String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        // FIX-015: 正则提取所有 {varName} 占位符,检测缺失变量
        java.util.regex.Matcher matcher =
                Pattern.compile("\\{([a-zA-Z0-9_]+)\\}").matcher(template);
        List<String> missingVars = new ArrayList<>();
        while (matcher.find()) {
            String varName = matcher.group(1);
            if (!variables.containsKey(varName)) {
                missingVars.add(varName);
            }
        }
        if (!missingVars.isEmpty()) {
            logger.warn("模板变量缺失, 保留原占位符: missingVars={}", missingVars);
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";

            // FIX-016: 敏感变量脱敏(spec 5.2)
            String keyLower = entry.getKey().toLowerCase();
            if (keyLower.contains("password") || keyLower.contains("token")
                    || keyLower.contains("secret")) {
                result = result.replace(placeholder, "***");
                continue;
            }

            // FIX-021: toString 失败时保留占位符
            String value;
            try {
                value = entry.getValue() != null ? entry.getValue().toString() : "";
            } catch (Exception e) {
                logger.warn("变量 toString 失败, 保留占位符: key={}, error={}",
                        entry.getKey(), e.getMessage());
                continue;
            }
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 校验接收人列表，过滤掉不存在或已删除的用户。
     *
     * <p>FIX-018(M-4): 改为批量查询 listByIds,避免逐个 getById 的 N+1 问题。</p>
     *
     * @param recipientUserIds 接收人用户 ID 列表
     * @return 有效的接收人列表
     */
    List<Long> validateRecipients(List<Long> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<User> users = userService.listByIds(recipientUserIds);
            List<Long> valid = new ArrayList<>();
            Set<Long> existingIds = new java.util.HashSet<>();
            if (users != null) {
                for (User user : users) {
                    if (user != null && user.getId() != null) {
                        existingIds.add(user.getId());
                    }
                }
            }
            for (Long userId : recipientUserIds) {
                if (existingIds.contains(userId)) {
                    valid.add(userId);
                } else {
                    logger.warn("接收人用户不存在, 跳过: userId={}", userId);
                }
            }
            return valid;
        } catch (Exception e) {
            logger.warn("批量查询接收人失败, 降级为空列表: error={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 批量查询用户并构建 ID → User 映射。
     *
     * <p>FIX-018(M-4): 供 processAsyncChannel 的 resolveRecipient 使用,
     * 避免对同一批接收人二次查询 user 表。</p>
     *
     * @param userIds 用户 ID 列表
     * @return ID → User 映射
     */
    private Map<Long, User> batchGetUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        try {
            List<User> users = userService.listByIds(userIds);
            Map<Long, User> map = new java.util.HashMap<>();
            if (users != null) {
                for (User user : users) {
                    if (user != null && user.getId() != null) {
                        map.put(user.getId(), user);
                    }
                }
            }
            return map;
        } catch (Exception e) {
            logger.warn("批量查询用户失败, 返回空映射: error={}", e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    /**
     * 分发到指定渠道。
     *
     * <p>FIX-010(B2): 校验渠道白名单,未知渠道拒绝并记录 WARN,避免静默投递后消费者抛异常。
     * FIX-018(M-4): 接收 userMap 参数,传递给子方法,避免重复查询 user 表。</p>
     *
     * @param channel 渠道标识
     * @param template 消息模板
     * @param event 业务事件
     * @param validRecipients 有效接收人列表
     * @param userMap 已批量查询的 ID → User 映射
     * @param subject 渲染后的主题
     * @param content 渲染后的内容
     */
    private void dispatchChannel(String channel, MsgTemplate template, BusinessEvent event,
                                  List<Long> validRecipients, Map<Long, User> userMap,
                                  String subject, String content) {
        // FIX-010: 渠道白名单校验
        if (channel == null || !SUPPORTED_CHANNELS.contains(channel)) {
            logger.warn("不支持的渠道类型, 跳过: channel={}, eventType={}", channel, event.getEventType());
            return;
        }
        switch (channel) {
            case CHANNEL_SITE_MSG:
                processSiteMsgChannel(template, event, validRecipients, subject, content);
                break;
            case CHANNEL_EMAIL:
                processAsyncChannel(template, event, validRecipients, userMap, subject, content,
                        CHANNEL_EMAIL, RECIPIENT_TYPE_EMAIL);
                break;
            default:
                // SMS/WEBHOOK 通过 RabbitMQ 投递,消费者跳过(status=SKIPPED)
                processAsyncChannel(template, event, validRecipients, userMap, subject, content,
                        channel, RECIPIENT_TYPE_USER_ID);
                break;
        }
    }

    /**
     * 处理 SITE_MSG 渠道（同步落库 notification 表 + msg_send_record）。
     *
     * <p>对应 spec F-003：SITE_MSG 渠道委托 SiteNotificationService，
     * 消除"站内信双系统割裂"问题。</p>
     *
     * <p>FIX-003(C1): for 循环内加 try-catch,单用户失败不影响其他用户,避免整批回滚。
     * FIX-005(C3): notification null 检查,避免 NPE。
     * FIX-019(M-9): BC-015 operatorId 为 null 时 senderName 显示"系统"。</p>
     */
    private void processSiteMsgChannel(MsgTemplate template, BusinessEvent event,
                                        List<Long> validRecipients,
                                        String subject, String content) {
        // FIX-019: operatorId 为 null 时 senderName 显示"系统"(spec BC-015)
        Long senderId = event.getOperatorId();
        String senderName = resolveSenderName(event.getOperatorId());

        for (Long userId : validRecipients) {
            try {
                Notification notification = siteNotificationService.createNotification(
                        userId, subject, content,
                        DEFAULT_NOTIFICATION_TYPE, DEFAULT_PRIORITY,
                        null, event.getEventType(),
                        senderId, senderName);

                // FIX-005(C3): notification null 检查
                if (notification == null || notification.getId() == null) {
                    throw new IllegalStateException(
                            "SiteNotificationService.createNotification 返回空, userId=" + userId);
                }

                MsgSendRecord record = buildSendRecord(template, event, subject, content,
                        CHANNEL_SITE_MSG, userId.toString(), RECIPIENT_TYPE_USER_ID);
                record.setNotificationId(notification.getId());
                record.setSendStatus(SEND_STATUS_SUCCESS);
                record.setFinishTime(LocalDateTime.now());
                msgSendRecordMapper.insert(record);

                logger.info("SITE_MSG 分发成功: userId={}, notificationId={}, recordId={}",
                        userId, notification.getId(), record.getRecordId());
            } catch (Exception e) {
                // FIX-003(C1): 单用户失败不影响其他用户,避免整批回滚
                logger.error("SITE_MSG 分发失败, 继续处理其他用户: userId={}, error={}",
                        userId, e.getMessage(), e);
            }
        }
    }

    /**
     * 解析发送人名称。
     *
     * <p>FIX-019(M-9): operatorId 为 null 或查询失败时返回"系统"(spec BC-015)。</p>
     *
     * @param operatorId 操作人 ID
     * @return 发送人名称
     */
    private String resolveSenderName(Long operatorId) {
        if (operatorId == null) {
            return "系统";
        }
        try {
            User user = userService.getById(operatorId);
            if (user == null || user.getUsername() == null) {
                return "系统";
            }
            return user.getUsername();
        } catch (Exception e) {
            logger.warn("查询发送人名称失败, 使用'系统': operatorId={}, error={}",
                    operatorId, e.getMessage());
            return "系统";
        }
    }

    /**
     * 处理异步渠道（EMAIL/SMS/WEBHOOK，通过 RabbitMQ 分发）。
     *
     * <p>FIX-018(M-4): 使用 userMap 替代逐个 getById,消除 N+1 查询。</p>
     *
     * @param userMap 已批量查询的 ID → User 映射
     * @param channel 渠道标识
     * @param recipientType 收件人类型（EMAIL=1, USER_ID=2）
     */
    private void processAsyncChannel(MsgTemplate template, BusinessEvent event,
                                      List<Long> validRecipients, Map<Long, User> userMap,
                                      String subject, String content,
                                      String channel, int recipientType) {
        for (Long userId : validRecipients) {
            String recipient = resolveRecipient(userId, channel, userMap);
            if (recipient == null) {
                logger.warn("无法解析收件人地址, 跳过: userId={}, channel={}", userId, channel);
                continue;
            }

            MsgSendDTO dto = new MsgSendDTO();
            dto.setTemplateCode(event.getEventType());
            dto.setRecipient(recipient);
            dto.setRecipientType(recipientType);
            dto.setVariables(event.getVariables());
            dto.setTriggerType(TRIGGER_TYPE_EVENT);
            dto.setBizType(event.getEventType());
            dto.setBizId(event.getOperatorId() != null ? event.getOperatorId().toString() : null);

            notificationService.sendMessage(dto, event.getOperatorId(), "EVENT_BUS");
            logger.info("{} 渠道已投递 RabbitMQ: userId={}, recipient={}",
                    channel, userId, recipient);
        }
    }

    /**
     * 解析收件人地址。
     *
     * <p>EMAIL 渠道返回用户邮箱；其他渠道返回 userId 字符串。</p>
     *
     * <p>FIX-018(M-4): 从 userMap 取 User,避免逐个 getById。</p>
     *
     * @param userId 用户 ID
     * @param channel 渠道标识
     * @param userMap 已批量查询的 ID → User 映射
     * @return 收件人地址，无法解析返回 null
     */
    private String resolveRecipient(Long userId, String channel, Map<Long, User> userMap) {
        User user = userMap.get(userId);
        if (user == null) {
            logger.warn("userMap 中找不到用户: userId={}", userId);
            return null;
        }
        if (CHANNEL_EMAIL.equals(channel)) {
            String email = user.getEmail();
            if (email == null || email.isEmpty()) {
                logger.warn("用户邮箱为空, 跳过: userId={}", userId);
                return null;
            }
            return email;
        }
        return userId.toString();
    }

    /**
     * 构建 msg_send_record 实体（SITE_MSG 渠道专用）。
     */
    private MsgSendRecord buildSendRecord(MsgTemplate template, BusinessEvent event,
                                           String subject, String content,
                                           String channel, String recipient, int recipientType) {
        MsgSendRecord record = new MsgSendRecord();
        record.setTemplateId(template.getTemplateId());
        record.setTemplateCode(template.getTemplateCode());
        record.setTemplateName(template.getTemplateName());
        record.setRecipient(recipient);
        record.setRecipientType(recipientType);
        record.setSubject(subject);
        record.setContent(content);
        record.setChannel(channel);
        record.setRetryCount(0);
        record.setMaxRetry(3);
        record.setSendTime(LocalDateTime.now());
        record.setTriggerType(TRIGGER_TYPE_EVENT);
        record.setBizType(event.getEventType());
        record.setBizId(event.getOperatorId() != null ? event.getOperatorId().toString() : null);
        record.setCreateUserId(event.getOperatorId());
        record.setCreateTime(LocalDateTime.now());
        return record;
    }
}
