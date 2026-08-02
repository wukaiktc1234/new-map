package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 科目余额实体类
 * @author example
 * @since 2025-12-06
 */
@TableName("account_balance")
public class AccountBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 余额ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 科目ID
     */
    @TableField("subject_id")
    private Long subjectId;

    /**
     * 科目编码
     */
    @TableField("subject_code")
    private String subjectCode;

    /**
     * 科目名称
     */
    @TableField("subject_name")
    private String subjectName;

    /**
     * 科目类别
     */
    @TableField("category")
    private String category;

    /**
     * 会计期间（格式：YYYY-MM）
     */
    @TableField("period")
    private String period;

    /**
     * 期初借方余额
     */
    @TableField("begin_debit")
    private BigDecimal beginDebit;

    /**
     * 期初贷方余额
     */
    @TableField("begin_credit")
    private BigDecimal beginCredit;

    /**
     * 本期借方发生额
     */
    @TableField("current_debit")
    private BigDecimal currentDebit;

    /**
     * 本期贷方发生额
     */
    @TableField("current_credit")
    private BigDecimal currentCredit;

    /**
     * 期末借方余额
     */
    @TableField("end_debit")
    private BigDecimal endDebit;

    /**
     * 期末贷方余额
     */
    @TableField("end_credit")
    private BigDecimal endCredit;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getBeginDebit() {
        return beginDebit;
    }

    public void setBeginDebit(BigDecimal beginDebit) {
        this.beginDebit = beginDebit;
    }

    public BigDecimal getBeginCredit() {
        return beginCredit;
    }

    public void setBeginCredit(BigDecimal beginCredit) {
        this.beginCredit = beginCredit;
    }

    public BigDecimal getCurrentDebit() {
        return currentDebit;
    }

    public void setCurrentDebit(BigDecimal currentDebit) {
        this.currentDebit = currentDebit;
    }

    public BigDecimal getCurrentCredit() {
        return currentCredit;
    }

    public void setCurrentCredit(BigDecimal currentCredit) {
        this.currentCredit = currentCredit;
    }

    public BigDecimal getEndDebit() {
        return endDebit;
    }

    public void setEndDebit(BigDecimal endDebit) {
        this.endDebit = endDebit;
    }

    public BigDecimal getEndCredit() {
        return endCredit;
    }

    public void setEndCredit(BigDecimal endCredit) {
        this.endCredit = endCredit;
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

    @Override
    public String toString() {
        return "AccountBalance{" +
            "id=" + id +
            ", subjectId=" + subjectId +
            ", subjectCode='" + subjectCode + '\'' +
            ", subjectName='" + subjectName + '\'' +
            ", category='" + category + '\'' +
            ", period='" + period + '\'' +
            ", beginDebit=" + beginDebit +
            ", beginCredit=" + beginCredit +
            ", currentDebit=" + currentDebit +
            ", currentCredit=" + currentCredit +
            ", endDebit=" + endDebit +
            ", endCredit=" + endCredit +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            '}';
    }
}
