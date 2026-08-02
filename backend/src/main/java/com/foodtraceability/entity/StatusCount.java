package com.foodtraceability.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 状态统计实体类
 */
@Schema(description = "状态统计")
public class StatusCount {

    @Schema(description = "状态", example = "active")
    private String status;

    @Schema(description = "数量", example = "20")
    private Long count;

    public StatusCount() {}

    public StatusCount(String status, Long count) {
        this.status = status;
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}