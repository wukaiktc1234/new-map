package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 凭证行实体类
 * @author example
 * @since 2025-12-06
 */
@TableName("voucher_line")
public class VoucherLine implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 凭证行ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 凭证ID
     */
    @TableField("voucher_id")
    private Long voucherId;

    /**
     * 行号
     */
    @TableField("line_no")
    private Integer lineNo;

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
     * 行摘要
     */
    @TableField("summary")
    private String summary;

    /**
     * 借方金额
     */
    @TableField("debit")
    private BigDecimal debit;

    /**
     * 贷方金额
     */
    @TableField("credit")
    private BigDecimal credit;

    /**
     * 关联业务ID
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 业务类型
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 源单据编号（用于前端显示追溯）
     */
    @TableField("source_document_no")
    private String sourceDocumentNo;

    /**
     * 辅助核算-供应商名称
     */
    @TableField("auxiliary_vendor")
    private String auxiliaryVendor;

    /**
     * 辅助核算-客户名称
     */
    @TableField("auxiliary_customer")
    private String auxiliaryCustomer;

    /**
     * 辅助核算-结算账户
     */
    @TableField("auxiliary_account")
    private String auxiliaryAccount;

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

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getSourceDocumentNo() {
        return sourceDocumentNo;
    }

    public void setSourceDocumentNo(String sourceDocumentNo) {
        this.sourceDocumentNo = sourceDocumentNo;
    }

    public String getAuxiliaryVendor() {
        return auxiliaryVendor;
    }

    public void setAuxiliaryVendor(String auxiliaryVendor) {
        this.auxiliaryVendor = auxiliaryVendor;
    }

    public String getAuxiliaryCustomer() {
        return auxiliaryCustomer;
    }

    public void setAuxiliaryCustomer(String auxiliaryCustomer) {
        this.auxiliaryCustomer = auxiliaryCustomer;
    }

    public String getAuxiliaryAccount() {
        return auxiliaryAccount;
    }

    public void setAuxiliaryAccount(String auxiliaryAccount) {
        this.auxiliaryAccount = auxiliaryAccount;
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
        return "VoucherLine{" +
            "id=" + id +
            ", voucherId=" + voucherId +
            ", lineNo=" + lineNo +
            ", subjectId=" + subjectId +
            ", subjectCode='" + subjectCode + '\'' +
            ", subjectName='" + subjectName + '\'' +
            ", summary='" + summary + '\'' +
            ", debit=" + debit +
            ", credit=" + credit +
            ", businessId=" + businessId +
            ", businessType='" + businessType + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            '}';
    }
}
