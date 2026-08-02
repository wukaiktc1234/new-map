package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 批量生成注册码请求DTO
 */
@Schema(description = "批量生成注册码请求")
public class BatchGenerateCodeRequest {

    @Schema(description = "生成数量", example = "10")
    private int count;

    @Schema(description = "注册码类型（INTERNAL：内部员工，EXTERNAL：外部合作）", example = "INTERNAL")
    private String type;

    @Schema(description = "有效期开始时间")
    private LocalDateTime validityStart;

    @Schema(description = "有效期结束时间")
    private LocalDateTime validityEnd;

    @Schema(description = "创建人ID")
    private String createdBy;

    // Getters and Setters
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}