package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 支付记录实体类
 * 记录订单的每笔支付操作
 */
@TableName("order_payment_records")
@Schema(description = "支付记录实体")
public class OrderPaymentRecordNew {

    /** 支付记录ID，主键自增 */
    @TableId(value = "payment_id", type = IdType.AUTO)
    @Schema(description = "支付记录ID", example = "1")
    private Long paymentId;

    /** 关联订单ID */
    @TableField("order_id")
    @Schema(description = "关联订单ID", example = "O1783341970544")
    private String orderId;

    /**
     * 支付方式：
     * 1现金 2微信 3支付宝 4银行卡 5积分 6混合支付
     */
    @TableField("payment_method")
    @Schema(description = "支付方式: 1现金 2微信 3支付宝 4银行卡 5积分 6混合支付", example = "2")
    private Integer paymentMethod;

    /** 支付金额（分） */
    @TableField("payment_amount")
    @Schema(description = "支付金额（分）", example = "11200")
    private Long paymentAmount;

    /** 交易流水号 */
    @TableField("transaction_no")
    @Schema(description = "交易流水号", example = "WX20260425001234567890")
    private String transactionNo;

    /** 支付时间 */
    @TableField("payment_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    /** 操作人ID */
    @TableField("operator_id")
    @Schema(description = "操作人ID")
    private Long operatorId;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // Getter方法
    public Long getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public Integer getPaymentMethod() { return paymentMethod; }
    public Long getPaymentAmount() { return paymentAmount; }
    public String getTransactionNo() { return transactionNo; }
    public LocalDateTime getPaymentTime() { return paymentTime; }
    public Long getOperatorId() { return operatorId; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }

    // Setter方法
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setPaymentMethod(Integer paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPaymentAmount(Long paymentAmount) { this.paymentAmount = paymentAmount; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
