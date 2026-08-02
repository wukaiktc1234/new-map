package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 注册码实体类
 */
@TableName("registration_code")
@Schema(description = "注册码实体")
public class RegistrationCode {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID, value = "id")
    @Schema(description = "主键ID")
    private String id;

    /**
     * 注册码
     */
    @Schema(description = "注册码")
    private String code;

    /**
     * 注册码类型（INTERNAL：内部员工，EXTERNAL：外部合作）
     */
    @Schema(description = "注册码类型")
    private String type;

    /**
     * 有效期开始时间
     */
    @Schema(description = "有效期开始时间")
    @TableField(value = "validity_start")
    private LocalDateTime validityStart;

    /**
     * 有效期结束时间
     */
    @Schema(description = "有效期结束时间")
    @TableField(value = "validity_end")
    private LocalDateTime validityEnd;

    /**
     * 状态（UNUSED：未使用，USED：已使用，EXPIRED：已过期，INVALID：无效）
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 创建人ID
     */
    @Schema(description = "创建人ID")
    @TableField(value = "created_by")
    private String createdBy;

    /**
     * 入职记录ID
     */
    @Schema(description = "入职记录ID")
    @TableField(value = "onboarding_record_id")
    private String onboardingRecordId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 使用时间
     */
    @Schema(description = "使用时间")
    @TableField(value = "use_time")
    private LocalDateTime useTime;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getValidityStart() {
        return validityStart;
    }

    public void setValidityStart(LocalDateTime validityStart) {
        this.validityStart = validityStart;
    }

    public LocalDateTime getValidityEnd() {
        return validityEnd;
    }

    public void setValidityEnd(LocalDateTime validityEnd) {
        this.validityEnd = validityEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getOnboardingRecordId() {
        return onboardingRecordId;
    }

    public void setOnboardingRecordId(String onboardingRecordId) {
        this.onboardingRecordId = onboardingRecordId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getUseTime() {
        return useTime;
    }

    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }
}