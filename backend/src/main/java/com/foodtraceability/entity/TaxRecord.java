package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 税务记录实体类
 * @author example
 * @since 2025-12-05
 */
@TableName("tax_record")
public class TaxRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 税种类型：VAT(增值税)、ENTERPRISE_INCOME_TAX(企业所得税)、PERSONAL_INCOME_TAX(个人所得税)、OTHER(其他)
     */
    @TableField("tax_type")
    private String taxType;

    /**
     * 纳税期间：如202511(2025年11月)
     */
    @TableField("tax_period")
    private String taxPeriod;

    /**
     * 应纳税所得额
     */
    @TableField("taxable_amount")
    private BigDecimal taxableAmount;

    /**
     * 税率
     */
    @TableField("tax_rate")
    private BigDecimal taxRate;

    /**
     * 应纳税额
     */
    @TableField("tax_amount")
    private BigDecimal taxAmount;

    /**
     * 已纳税额
     */
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    /**
     * 纳税状态：UNPAID(未缴纳)、PAID(已缴纳)、OVERDUE(逾期)
     */
    @TableField("tax_status")
    private String taxStatus;

    /**
     * 缴纳日期
     */
    @TableField("payment_date")
    private LocalDateTime paymentDate;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

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

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    // getter and setter methods
    // ...
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaxType() {
        return taxType;
    }

    public void setTaxType(String taxType) {
        this.taxType = taxType;
    }

    public String getTaxPeriod() {
        return taxPeriod;
    }

    public void setTaxPeriod(String taxPeriod) {
        this.taxPeriod = taxPeriod;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getTaxStatus() {
        return taxStatus;
    }

    public void setTaxStatus(String taxStatus) {
        this.taxStatus = taxStatus;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    @Override
    public String toString() {
        return "TaxRecord{" +
            "id=" + id +
            ", taxType='" + taxType + '\'' +
            ", taxPeriod='" + taxPeriod + '\'' +
            ", taxableAmount=" + taxableAmount +
            ", taxRate=" + taxRate +
            ", taxAmount=" + taxAmount +
            ", paidAmount=" + paidAmount +
            ", taxStatus='" + taxStatus + '\'' +
            ", paymentDate=" + paymentDate +
            ", description='" + description + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            ", createBy='" + createBy + '\'' +
            ", updateBy='" + updateBy + '\'' +
            '}';
    }
}