package com.foodtraceability.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员等级升级事件
 * 触发场景：会员充值后累计金额达到更高等级门槛
 * 监听用途：发送会员通知、更新会员权益、触发营销活动（如赠送优惠券）
 */
public class MemberLevelUpgradedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 会员ID */
    private final Long memberId;

    /** 升级前等级ID（可能为 null，表示此前无等级） */
    private final Long oldLevelId;

    /** 升级后等级ID */
    private final Long newLevelId;

    /** 事件触发时间 */
    private final LocalDateTime eventTime;

    public MemberLevelUpgradedEvent(Long memberId, Long oldLevelId, Long newLevelId) {
        this.memberId = memberId;
        this.oldLevelId = oldLevelId;
        this.newLevelId = newLevelId;
        this.eventTime = LocalDateTime.now();
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getOldLevelId() {
        return oldLevelId;
    }

    public Long getNewLevelId() {
        return newLevelId;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }
}
