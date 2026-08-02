package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 消费获取积分请求DTO
 */
@Schema(description = "消费获取积分请求")
public class PointsEarnDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会员ID", required = true)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "消费金额（分）", required = true)
    @NotNull(message = "消费金额不能为空")
    private Long amount;

    @Schema(description = "消费场景：dine_in-堂食 takeout-外卖", defaultValue = "dine_in")
    private String scene;

    @Schema(description = "订单编号")
    private String orderNo;

    // ==================== Getter & Setter ====================

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
}
