package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 充值记录查询DTO（分页）
 */
@Schema(description = "充值记录查询请求")
public class RechargeRecordQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "会员手机号（模糊匹配）")
    private String memberPhone;

    @Schema(description = "会员ID（精确匹配）")
    private String memberId;

    @Schema(description = "充值方案ID")
    private String planId;

    @Schema(description = "支付方式: wechat/alipay/cash/bank_card/balance")
    private String paymentMethod;

    @Schema(description = "支付状态: pending/success/failed/refunded/partial_refunded")
    private String paymentStatus;

    @Schema(description = "退款状态: none/pending/approved/rejected/refunded")
    private String refundStatus;

    @Schema(description = "支付开始时间（YYYY-MM-DD HH:mm:ss）")
    private String startTime;

    @Schema(description = "支付结束时间（YYYY-MM-DD HH:mm:ss）")
    private String endTime;

    // ==================== Getter & Setter ====================

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
