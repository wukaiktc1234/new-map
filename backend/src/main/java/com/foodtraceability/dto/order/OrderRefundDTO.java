package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 退款申请请求DTO
 */
@Schema(description = "退款申请请求")
public class OrderRefundDTO {

    /** 退款类型：1全额退款 2部分退款 */
    @NotNull(message = "退款类型不能为空")
    @Min(value = 1, message = "退款类型无效")
    @Max(value = 2, message = "退款类型无效")
    @Schema(description = "退款类型: 1全额退款 2部分退款", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer refundType;

    /** 退款金额（分），部分退款时必填 */
    @Min(value = 1, message = "退款金额必须大于0")
    @Schema(description = "退款金额（分）")
    private Long refundAmount;

    /** 退款原因 */
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 500, message = "退款原因长度不能超过500字")
    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refundReason;

    /** 退款方式：1原路返回 2现金 3其他 */
    @Schema(description = "退款方式: 1原路返回 2现金 3其他", example = "1")
    private Integer refundMethod = 1;

    /**
     * 需要退款的订单明细ID列表（部分退款时使用）
     * 类型为 String 以匹配 OrderItemNew.itemId（UUID 字符串）
     */
    @Schema(description = "退款明细ID列表（部分退款时使用，匹配 OrderItemNew.itemId）")
    private List<String> itemIds;

    // Getter和Setter方法
    public Integer getRefundType() { return refundType; }
    public void setRefundType(Integer refundType) { this.refundType = refundType; }
    public Long getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public Integer getRefundMethod() { return refundMethod; }
    public void setRefundMethod(Integer refundMethod) { this.refundMethod = refundMethod; }
    public List<String> getItemIds() { return itemIds; }
    public void setItemIds(List<String> itemIds) { this.itemIds = itemIds; }
}
