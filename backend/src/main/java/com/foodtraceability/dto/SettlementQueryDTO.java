package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 日结对账查询DTO
 * 用于门店日结记录的筛选和分页查询
 */
@Schema(description = "日结对账查询DTO")
public class SettlementQueryDTO extends PageQuery {

    @Schema(description = "门店ID", example = "store001")
    private String storeId;

    @Schema(description = "结算状态（draft/pending/approved/rejected）", example = "pending")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "结算日期-开始", example = "2026-05-01")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "结算日期-结束", example = "2026-05-11")
    private LocalDate endDate;

    @Schema(description = "审核人ID", example = "auditor001")
    private String auditorId;

    // ==================== Getter & Setter ====================

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(String auditorId) {
        this.auditorId = auditorId;
    }
}
