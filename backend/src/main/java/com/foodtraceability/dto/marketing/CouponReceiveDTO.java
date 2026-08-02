package com.foodtraceability.dto.marketing;

import jakarta.validation.constraints.NotNull;

/**
 * 领取优惠券请求DTO
 */
public class CouponReceiveDTO {

    /** 优惠券模板ID */
    @NotNull(message = "优惠券模板ID不能为空")
    private Long templateId;

    /** 会员ID */
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    // ==================== Getter & Setter ====================

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
