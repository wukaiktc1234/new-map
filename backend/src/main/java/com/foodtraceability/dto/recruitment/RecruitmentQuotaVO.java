package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 招聘名额视图对象VO
 */
@Schema(description = "招聘名额视图对象")
public class RecruitmentQuotaVO {

    @Schema(description = "名额ID", example = "1")
    private Long quotaId;

    @Schema(description = "年度", example = "2026")
    private Integer year;

    @Schema(description = "季度(1-4)", example = "3")
    private Integer quarter;

    @Schema(description = "门店ID", example = "1001")
    private Long storeId;

    @Schema(description = "门店名称", example = "上海人民广场店")
    private String storeName;

    @Schema(description = "岗位ID", example = "2001")
    private Long positionId;

    @Schema(description = "岗位名称", example = "服务员")
    private String positionName;

    @Schema(description = "名额数", example = "5")
    private Integer headcount;

    @Schema(description = "已用名额数", example = "2")
    private Integer usedCount;

    @Schema(description = "剩余名额数(计算字段:headcount-usedCount)", example = "3")
    private Integer remaining;

    @Schema(description = "状态(draft/issued/active/exhausted/closed/rejected/adjustment_requested)",
            example = "active")
    private String status;

    @Schema(description = "下发人ID(HR)", example = "100")
    private Long issuedBy;

    @Schema(description = "下发时间")
    private LocalDateTime issuedTime;

    @Schema(description = "确认人ID(门店店长)", example = "200")
    private Long confirmedBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedTime;

    @Schema(description = "名额有效期", example = "2026-09-30")
    private LocalDate expireDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public Long getQuotaId() {
        return quotaId;
    }

    public void setQuotaId(Long quotaId) {
        this.quotaId = quotaId;
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

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public Integer getHeadcount() {
        return headcount;
    }

    public void setHeadcount(Integer headcount) {
        this.headcount = headcount;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(Long issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDateTime getIssuedTime() {
        return issuedTime;
    }

    public void setIssuedTime(LocalDateTime issuedTime) {
        this.issuedTime = issuedTime;
    }

    public Long getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Long confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedTime() {
        return confirmedTime;
    }

    public void setConfirmedTime(LocalDateTime confirmedTime) {
        this.confirmedTime = confirmedTime;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
}
