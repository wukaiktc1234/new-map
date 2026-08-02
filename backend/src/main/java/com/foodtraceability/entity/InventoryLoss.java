package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报损单实体类
 * 用于管理库存报损操作，包括过期、损坏、丢失等
 */
@TableName("inventory_losses")
public class InventoryLoss {
    /**
     * 报损单ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long lossId;

    /**
     * 报损单号（唯一）
     */
    @TableField("loss_code")
    private String lossCode;

    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 报损类型（1:过期 2:损坏 3:丢失 4:其他）
     */
    @TableField("loss_type")
    private Integer lossType;

    /**
     * 报损状态（0:待审核 1:已审核 2:已处理）
     */
    @TableField("loss_status")
    private Integer lossStatus;

    /**
     * 总数量
     */
    @TableField("total_quantity")
    private BigDecimal totalQuantity;

    /**
     * 总金额（分）
     */
    @TableField("total_amount")
    private Long totalAmount;

    /**
     * 报损原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 创建人ID
     */
    @TableField("create_user_id")
    private Long createUserId;

    /**
     * 审批人ID
     */
    @TableField("approve_user_id")
    private Long approveUserId;

    /**
     * 审批时间
     */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public Long getLossId() {
        return lossId;
    }

    public void setLossId(Long lossId) {
        this.lossId = lossId;
    }

    public String getLossCode() {
        return lossCode;
    }

    public void setLossCode(String lossCode) {
        this.lossCode = lossCode;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getLossType() {
        return lossType;
    }

    public void setLossType(Integer lossType) {
        this.lossType = lossType;
    }

    public Integer getLossStatus() {
        return lossStatus;
    }

    public void setLossStatus(Integer lossStatus) {
        this.lossStatus = lossStatus;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public Long getApproveUserId() {
        return approveUserId;
    }

    public void setApproveUserId(Long approveUserId) {
        this.approveUserId = approveUserId;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    // ==================== 兼容性方法 ====================

    /**
     * 兼容性方法：ID映射到lossId
     */
    public Long getId() {
        return lossId;
    }

    public void setId(Long id) {
        this.lossId = id;
    }

    /**
     * 兼容性方法：状态映射到lossStatus
     */
    public Integer getStatus() {
        return lossStatus;
    }

    public void setStatus(Integer status) {
        this.lossStatus = status;
    }

    /**
     * 兼容性方法：状态（String类型支持）
     */
    public void setStatus(String status) {
        if (status != null && !status.isEmpty()) {
            try {
                this.lossStatus = Integer.parseInt(status);
            } catch (NumberFormatException e) {
                this.lossStatus = 0; // 默认待审核
            }
        }
    }
}
