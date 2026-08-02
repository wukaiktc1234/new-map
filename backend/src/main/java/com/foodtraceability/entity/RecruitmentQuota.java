package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 招聘名额实体类
 * 对应表: recruitment_quotas（Ch3.1 招聘名额系统）
 * 主键: quota_id BIGINT GENERATED ALWAYS AS IDENTITY（自增）
 */
@TableName("recruitment_quotas")
@Schema(description = "招聘名额实体")
public class RecruitmentQuota {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID(自增)")
    private Long quotaId;

    @Schema(description = "年度")
    private Integer year;

    @Schema(description = "季度(1-4)")
    private Integer quarter;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "门店名称(冗余)")
    private String storeName;

    @Schema(description = "岗位ID")
    private Long positionId;

    @Schema(description = "岗位名称(冗余)")
    private String positionName;

    @Schema(description = "名额数(>0)")
    private Integer headcount;

    @Schema(description = "已用名额数")
    private Integer usedCount;

    @Schema(description = "状态(draft/issued/active/exhausted/closed/rejected/adjustment_requested)")
    private String status;

    @Schema(description = "下发人ID(HR)")
    private Long issuedBy;

    @Schema(description = "下发时间")
    private LocalDateTime issuedTime;

    @Schema(description = "确认人ID(门店店长)")
    private Long confirmedBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedTime;

    @Schema(description = "名额有效期")
    private LocalDate expireDate;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除:0=未删除 1=已删除")
    private Integer deleted;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
