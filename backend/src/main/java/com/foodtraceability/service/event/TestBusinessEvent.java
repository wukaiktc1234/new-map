package com.foodtraceability.service.event;

import java.util.List;
import java.util.Map;

/**
 * Sprint 1 测试业务事件
 *
 * <p>用于验收场景 E1（手动触发测试事件），验证 NotificationEventBus 端到端链路：
 * 发布事件 → AFTER_COMMIT 监听 → 查询模板 → 渲染 → 渠道分发</p>
 *
 * <p>事件类型固定为 "sprint1.test.event"，对应预注册的测试模板。
 * 模板内容：主题"测试事件：{eventName}"，内容"这是 Sprint 1 验收测试事件。
 * 事件名：{eventName}，操作人：{operatorName}"。</p>
 *
 * <p>对应 spec F-001 验收标准 E1。</p>
 */
public class TestBusinessEvent extends BusinessEvent {

    /** 测试事件类型编码（对应 msg_template.template_code） */
    public static final String EVENT_TYPE = "sprint1.test.event";

    /**
     * 构造测试业务事件。
     *
     * @param source 事件源（通常为 this）
     * @param operatorId 操作人 ID（可为 null）
     * @param variables 模板变量，应包含 eventName 和 operatorName 两个键
     * @param recipientUserIds 接收人用户 ID 列表
     * @param channels 渠道列表（如 ["SITE_MSG"] 或 ["SITE_MSG", "EMAIL"]）
     */
    public TestBusinessEvent(Object source, Long operatorId,
                              Map<String, Object> variables,
                              List<Long> recipientUserIds, List<String> channels) {
        super(source, EVENT_TYPE, operatorId, variables, recipientUserIds, channels);
    }
}
