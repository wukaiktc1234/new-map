package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 招聘名额分页查询DTO
 */
@Schema(description = "招聘名额分页查询DTO")
public class RecruitmentQuotaQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "年度", example = "2026")
    private Integer year;

    @Schema(description = "季度(1-4)", example = "3")
    private Integer quarter;

    @Schema(description = "门店ID", example = "1001")
    private Long storeId;

    @Schema(description = "岗位ID", example = "2001")
    private Long positionId;

    @Schema(description = "状态(draft/issued/active/exhausted/closed/rejected/adjustment_requested)",
            example = "active",
            allowableValues = {"draft", "issued", "active", "exhausted", "closed", "rejected", "adjustment_requested"})
    private String status;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
