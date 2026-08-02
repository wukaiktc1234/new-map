package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 异常告警查询DTO（分页）
 * 对应前端 GET /v1/anomaly-alerts
 */
@Schema(description = "异常告警查询请求")
public class AnomalyAlertQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "处理状态: pending/handled/ignored")
    private String status;

    // ==================== Getter & Setter ====================

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
