package com.foodtraceability.service.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEvent;

/**
 * 业务事件基类（运行时对象，不持久化）
 *
 * <p>继承 Spring ApplicationEvent，支持 @TransactionalEventListener(AFTER_COMMIT)。
 * 业务模块通过 ApplicationEventPublisher.publishEvent() 发布事件，
 * 由 NotificationEventBus 监听并分发到各通知渠道。</p>
 *
 * <p>对应 spec F-001（业务事件统一发布能力）和 spec F-005（事务安全保证）。</p>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 业务方法（必须有 @Transactional 注解）
 * @Transactional(rollbackFor = Exception.class)
 * public void publishQuota(QuotaIssuedDTO dto) {
 *     // 1. 业务逻辑：保存名额到数据库
 *     quotaMapper.insert(quota);
 *     // 2. 发布事件（业务事务提交后才会触发监听器）
 *     Map<String, Object> variables = Map.of(
 *         "storeName", dto.getStoreName(),
 *         "positionName", dto.getPositionName(),
 *         "headcount", dto.getHeadcount()
 *     );
 *     applicationEventPublisher.publishEvent(
 *         new QuotaIssuedEvent(this, operatorId, variables,
 *             recipientUserIds, List.of("SITE_MSG", "EMAIL"))
 *     );
 * }
 * }</pre></p>
 */
public abstract class BusinessEvent extends ApplicationEvent {

    /** 事件类型（对应 msg_template.template_code，如 "recruitment.quota.issued"） */
    private final String eventType;

    /** 操作人 ID（可为 null，表示系统自动触发；用于 notification.sender_id） */
    private final Long operatorId;

    /** 模板变量（用于 {varName} 占位符替换） */
    private final Map<String, Object> variables;

    /** 接收人用户 ID 列表 */
    private final List<Long> recipientUserIds;

    /** 渠道列表（SITE_MSG / EMAIL / SMS / WEBHOOK） */
    private final List<String> channels;

    /**
     * 构造业务事件。
     *
     * @param source 事件源（通常为 this）
     * @param eventType 事件类型（对应模板编码）
     * @param operatorId 操作人 ID（可为 null）
     * @param variables 模板变量（可为 null，内部转为不可变空 Map）
     * @param recipientUserIds 接收人列表（可为 null，内部转为不可变空 List）
     * @param channels 渠道列表（可为 null，内部转为不可变空 List）
     */
    protected BusinessEvent(Object source, String eventType, Long operatorId,
                            Map<String, Object> variables,
                            List<Long> recipientUserIds, List<String> channels) {
        super(source);
        this.eventType = eventType;
        this.operatorId = operatorId;
        // FIX-022(m-1): 防御性拷贝,避免外部修改原 Map/List 影响事件内部状态
        this.variables = variables != null
                ? Collections.unmodifiableMap(new HashMap<>(variables)) : Collections.emptyMap();
        this.recipientUserIds = recipientUserIds != null
                ? Collections.unmodifiableList(new java.util.ArrayList<>(recipientUserIds))
                : Collections.emptyList();
        this.channels = channels != null
                ? Collections.unmodifiableList(new java.util.ArrayList<>(channels))
                : Collections.emptyList();
    }

    public String getEventType() {
        return this.eventType;
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public Map<String, Object> getVariables() {
        return this.variables;
    }

    public List<Long> getRecipientUserIds() {
        return this.recipientUserIds;
    }

    public List<String> getChannels() {
        return this.channels;
    }
}
