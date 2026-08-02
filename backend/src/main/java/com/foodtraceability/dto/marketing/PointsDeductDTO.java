package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 积分抵扣请求DTO
 */
@Schema(description = "积分抵扣请求")
public class PointsDeductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会员ID", required = true)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "使用的积分数量", required = true)
    @NotNull(message = "积分数量不能为空")
    private Integer points;

    @Schema(description = "订单金额（分）", required = true)
    @NotNull(message = "订单金额不能为空")
    private Long orderAmount;

    @Schema(description = "订单编号")
    private String orderNo;

    // ==================== Getter & Setter ====================

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Long getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(Long orderAmount) {
        this.orderAmount = orderAmount;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
}
