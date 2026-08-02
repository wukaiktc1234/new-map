package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("promotion")
public class Promotion {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("promotion_code")
    private String promotionCode;
    @TableField("promotion_name")
    private String promotionName;
    @TableField("type")
    private String type;
    @TableField("target_id")
    private Long targetId;
    @TableField("target_type")
    private String targetType;
    @TableField("discount_type")
    private String discountType;
    @TableField("discount_value")
    private BigDecimal discountValue;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("end_time")
    private LocalDateTime endTime;
    @TableField("status")
    private String status;
    @TableField("create_time")
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;
    @TableField("deleted")
    private Integer deleted;

    // Getter methods
    public Long getId() {
        return id;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public String getType() {
        return type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Promotion() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Promotion)) return false;
        final Promotion other = (Promotion) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$targetId = this.getTargetId();
        final java.lang.Object other$targetId = other.getTargetId();
        if (this$targetId == null ? other$targetId != null : !this$targetId.equals(other$targetId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$promotionCode = this.getPromotionCode();
        final java.lang.Object other$promotionCode = other.getPromotionCode();
        if (this$promotionCode == null ? other$promotionCode != null : !this$promotionCode.equals(other$promotionCode)) return false;
        final java.lang.Object this$promotionName = this.getPromotionName();
        final java.lang.Object other$promotionName = other.getPromotionName();
        if (this$promotionName == null ? other$promotionName != null : !this$promotionName.equals(other$promotionName)) return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final java.lang.Object this$targetType = this.getTargetType();
        final java.lang.Object other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) return false;
        final java.lang.Object this$discountType = this.getDiscountType();
        final java.lang.Object other$discountType = other.getDiscountType();
        if (this$discountType == null ? other$discountType != null : !this$discountType.equals(other$discountType)) return false;
        final java.lang.Object this$discountValue = this.getDiscountValue();
        final java.lang.Object other$discountValue = other.getDiscountValue();
        if (this$discountValue == null ? other$discountValue != null : !this$discountValue.equals(other$discountValue)) return false;
        final java.lang.Object this$startTime = this.getStartTime();
        final java.lang.Object other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime)) return false;
        final java.lang.Object this$endTime = this.getEndTime();
        final java.lang.Object other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !this$endTime.equals(other$endTime)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Promotion;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $targetId = this.getTargetId();
        result = result * PRIME + ($targetId == null ? 43 : $targetId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $promotionCode = this.getPromotionCode();
        result = result * PRIME + ($promotionCode == null ? 43 : $promotionCode.hashCode());
        final java.lang.Object $promotionName = this.getPromotionName();
        result = result * PRIME + ($promotionName == null ? 43 : $promotionName.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $targetType = this.getTargetType();
        result = result * PRIME + ($targetType == null ? 43 : $targetType.hashCode());
        final java.lang.Object $discountType = this.getDiscountType();
        result = result * PRIME + ($discountType == null ? 43 : $discountType.hashCode());
        final java.lang.Object $discountValue = this.getDiscountValue();
        result = result * PRIME + ($discountValue == null ? 43 : $discountValue.hashCode());
        final java.lang.Object $startTime = this.getStartTime();
        result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
        final java.lang.Object $endTime = this.getEndTime();
        result = result * PRIME + ($endTime == null ? 43 : $endTime.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Promotion(id=" + this.getId() + ", promotionCode=" + this.getPromotionCode() + ", promotionName=" + this.getPromotionName() + ", type=" + this.getType() + ", targetId=" + this.getTargetId() + ", targetType=" + this.getTargetType() + ", discountType=" + this.getDiscountType() + ", discountValue=" + this.getDiscountValue() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", status=" + this.getStatus() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
