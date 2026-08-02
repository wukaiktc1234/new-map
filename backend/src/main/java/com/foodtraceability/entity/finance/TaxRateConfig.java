package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 税率配置实体类
 * 用于管理各税种的税率配置，支持按纳税人类型和有效期管理税率政策
 */
@TableName("tax_rate_configs")
public class TaxRateConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置ID */
    @TableId(type = IdType.AUTO)
    private Long configId;

    /**
     * 税种
     * 1-增值税 2-城建税 3-教育费附加 4-地方教育附加 5-企业所得税 6-个人所得税 7-印花税
     */
    private Integer taxType;

    /**
     * 纳税人类型
     * 1-一般纳税人 2-小规模纳税人
     */
    private Integer taxpayerType;

    /** 税率 */
    private BigDecimal taxRate;

    /** 政策版本 */
    private String policyVersion;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expiryDate;

    /** 是否生效 */
    private Boolean isActive;

    /** 备注 */
    private String remark;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public Integer getTaxType() {
        return taxType;
    }

    public void setTaxType(Integer taxType) {
        this.taxType = taxType;
    }

    public Integer getTaxpayerType() {
        return taxpayerType;
    }

    public void setTaxpayerType(Integer taxpayerType) {
        this.taxpayerType = taxpayerType;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "TaxRateConfig{" +
                "configId=" + configId +
                ", taxType=" + taxType +
                ", taxpayerType=" + taxpayerType +
                ", taxRate=" + taxRate +
                ", policyVersion='" + policyVersion + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
