package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会计期间VO
 */
@Schema(description = "会计期间信息")
public class AccountingPeriodVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "期间ID")
    private Long periodId;

    @Schema(description = "期间编码")
    private String periodCode;

    @Schema(description = "期间名称")
    private String periodName;

    @Schema(description = "期间类型：1-月度 2-季度 3-年度")
    private Integer periodType;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "状态：0-未开放 1-开放 2-已结账")
    private Integer status;

    @Schema(description = "是否已结账")
    private Boolean isClosed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结账时间")
    private LocalDateTime closeTime;

    @Schema(description = "结账人ID")
    private Long closeUserId;

    @Schema(description = "试算平衡是否通过")
    private Boolean trialBalancePassed;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public String getPeriodCode() {
        return periodCode;
    }

    public void setPeriodCode(String periodCode) {
        this.periodCode = periodCode;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }

    public Integer getPeriodType() {
        return periodType;
    }

    public void setPeriodType(Integer periodType) {
        this.periodType = periodType;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Boolean isClosed) {
        this.isClosed = isClosed;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }

    public Long getCloseUserId() {
        return closeUserId;
    }

    public void setCloseUserId(Long closeUserId) {
        this.closeUserId = closeUserId;
    }

    public Boolean getTrialBalancePassed() {
        return trialBalancePassed;
    }

    public void setTrialBalancePassed(Boolean trialBalancePassed) {
        this.trialBalancePassed = trialBalancePassed;
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
}
