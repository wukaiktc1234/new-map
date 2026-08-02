package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会计期间实体类
 * 用于管理餐饮企业的会计期间，支持月度、季度、年度三种期间类型，以及结账管理
 */
@TableName("accounting_periods")
public class AccountingPeriod extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 期间ID */
    @TableId(type = IdType.AUTO)
    private Long periodId;

    /** 期间编码，如2026-01 */
    private String periodCode;

    /** 期间名称，如2026年1月 */
    private String periodName;

    /**
     * 期间类型
     * 1-月度 2-季度 3-年度
     */
    private Integer periodType;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /**
     * 状态
     * 0-未开放 1-开放 2-已结账
     */
    private Integer status;

    /** 是否已结账 */
    private Boolean isClosed;

    /** 结账时间 */
    private LocalDateTime closeTime;

    /** 结账人ID */
    private Long closeUserId;

    /** 试算平衡是否通过 */
    private Boolean trialBalancePassed;

    /** 备注 */
    private String remark;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

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

    @Override
    public String toString() {
        return "AccountingPeriod{" +
                "periodId=" + periodId +
                ", periodCode='" + periodCode + '\'' +
                ", periodName='" + periodName + '\'' +
                ", periodType=" + periodType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", isClosed=" + isClosed +
                '}';
    }
}
