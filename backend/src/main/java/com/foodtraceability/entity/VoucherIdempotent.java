package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 凭证幂等控制实体类
 * 用于防止重复生成相同的凭证，保证幂等性
 * @author example
 * @since 2026-04-04
 */
@TableName("voucher_idempotent")
public class VoucherIdempotent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业务键（事件类型+源业务ID组合，唯一约束）
     */
    @TableField("business_key")
    private String businessKey;

    /**
     * 事件类型
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 源业务单据ID
     */
    @TableField("source_business_id")
    private String sourceBusinessId;

    /**
     * 已生成的凭证ID
     */
    @TableField("voucher_id")
    private Long voucherId;

    /**
     * 首次生成时间
     */
    @TableField("first_generate_time")
    private LocalDateTime firstGenerateTime;

    /**
     * 最后生成时间
     */
    @TableField("last_generate_time")
    private LocalDateTime lastGenerateTime;

    /**
     * 尝试次数
     */
    @TableField("attempt_count")
    private Integer attemptCount;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSourceBusinessId() {
        return sourceBusinessId;
    }

    public void setSourceBusinessId(String sourceBusinessId) {
        this.sourceBusinessId = sourceBusinessId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public LocalDateTime getFirstGenerateTime() {
        return firstGenerateTime;
    }

    public void setFirstGenerateTime(LocalDateTime firstGenerateTime) {
        this.firstGenerateTime = firstGenerateTime;
    }

    public LocalDateTime getLastGenerateTime() {
        return lastGenerateTime;
    }

    public void setLastGenerateTime(LocalDateTime lastGenerateTime) {
        this.lastGenerateTime = lastGenerateTime;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "VoucherIdempotent{" +
            "id=" + id +
            ", businessKey='" + businessKey + '\'' +
            ", eventType='" + eventType + '\'' +
            ", sourceBusinessId='" + sourceBusinessId + '\'' +
            ", voucherId=" + voucherId +
            ", firstGenerateTime=" + firstGenerateTime +
            ", lastGenerateTime=" + lastGenerateTime +
            ", attemptCount=" + attemptCount +
            ", createTime=" + createTime +
            '}';
    }
}
