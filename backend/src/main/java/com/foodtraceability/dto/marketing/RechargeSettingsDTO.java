package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 储值系统设置DTO
 * 对应前端 PUT /v1/recharge-settings
 *
 * 该DTO对应前端 RechargeSystemSettings 类型，包含全部可配置项。
 * 配置以整体JSON形式存储在 recharge_system_settings.config_json 字段中。
 */
@Schema(description = "储值系统设置")
public class RechargeSettingsDTO {

    // === 充值限额 ===
    @Schema(description = "单次充值上限（元）")
    private Integer singleRechargeLimit;

    @Schema(description = "单日累计充值上限（元）")
    private Integer dailyRechargeLimit;

    @Schema(description = "单月累计充值上限（元）")
    private Integer monthlyRechargeLimit;

    @Schema(description = "单卡余额上限（元，法规要求默认5000）")
    private Integer maxBalanceLimit;

    // === 赠送规则 ===
    @Schema(description = "赠送比例硬上限（百分比，默认20）")
    private Integer maxBonusRate;

    @Schema(description = "赠送有效期默认天数（默认180）")
    private Integer defaultBonusValidityDays;

    @Schema(description = "赠送形式限制: balance/coupon/points/mixed")
    private List<String> allowedBonusTypes;

    // === 退款规则 ===
    @Schema(description = "免审批阈值（元，默认500）")
    private Integer noApprovalThreshold;

    @Schema(description = "店长审批阈值（元，默认2000）")
    private Integer managerApprovalThreshold;

    @Schema(description = "退款手续费比例（百分比，默认0）")
    private Integer refundFeeRate;

    @Schema(description = "充值后冷却期（小时，默认24）")
    private Integer refundCooldownHours;

    @Schema(description = "赠送处理方式: clear-清零 proportional-按比例扣减")
    private String bonusHandlingOnRefund;

    // === 风控配置 ===
    @Schema(description = "风控总开关")
    private Boolean riskControlEnabled;

    @Schema(description = "异常交易告警开关")
    private Boolean anomalyAlertEnabled;

    @Schema(description = "告警通知方式: sms/wechat/email")
    private List<String> alertChannels;

    // === 协议管理 ===
    @Schema(description = "当前充值协议版本号")
    private String currentAgreementVersion;

    @Schema(description = "协议变更需重新确认")
    private Boolean agreementReconfirmOnChange;

    // === 财务对接 ===
    @Schema(description = "财务系统对接开关")
    private Boolean financeIntegrationEnabled;

    @Schema(description = "财务系统类型: none/kingdee/yonyou/custom")
    private String financeSystemType;

    @Schema(description = "自动生成凭证")
    private Boolean autoVoucherGeneration;

    // === 资金存管（预留，默认关闭） ===
    @Schema(description = "资金存管开关")
    private Boolean fundCustodyEnabled;

    @Schema(description = "存管银行")
    private String custodyBank;

    @Schema(description = "存管比例（百分比）")
    private Integer custodyRate;

    @Schema(description = "存管账户")
    private String custodyAccount;

    // ==================== Getter & Setter ====================

    public Integer getSingleRechargeLimit() { return singleRechargeLimit; }
    public void setSingleRechargeLimit(Integer singleRechargeLimit) { this.singleRechargeLimit = singleRechargeLimit; }

    public Integer getDailyRechargeLimit() { return dailyRechargeLimit; }
    public void setDailyRechargeLimit(Integer dailyRechargeLimit) { this.dailyRechargeLimit = dailyRechargeLimit; }

    public Integer getMonthlyRechargeLimit() { return monthlyRechargeLimit; }
    public void setMonthlyRechargeLimit(Integer monthlyRechargeLimit) { this.monthlyRechargeLimit = monthlyRechargeLimit; }

    public Integer getMaxBalanceLimit() { return maxBalanceLimit; }
    public void setMaxBalanceLimit(Integer maxBalanceLimit) { this.maxBalanceLimit = maxBalanceLimit; }

    public Integer getMaxBonusRate() { return maxBonusRate; }
    public void setMaxBonusRate(Integer maxBonusRate) { this.maxBonusRate = maxBonusRate; }

    public Integer getDefaultBonusValidityDays() { return defaultBonusValidityDays; }
    public void setDefaultBonusValidityDays(Integer defaultBonusValidityDays) { this.defaultBonusValidityDays = defaultBonusValidityDays; }

    public List<String> getAllowedBonusTypes() { return allowedBonusTypes; }
    public void setAllowedBonusTypes(List<String> allowedBonusTypes) { this.allowedBonusTypes = allowedBonusTypes; }

    public Integer getNoApprovalThreshold() { return noApprovalThreshold; }
    public void setNoApprovalThreshold(Integer noApprovalThreshold) { this.noApprovalThreshold = noApprovalThreshold; }

    public Integer getManagerApprovalThreshold() { return managerApprovalThreshold; }
    public void setManagerApprovalThreshold(Integer managerApprovalThreshold) { this.managerApprovalThreshold = managerApprovalThreshold; }

    public Integer getRefundFeeRate() { return refundFeeRate; }
    public void setRefundFeeRate(Integer refundFeeRate) { this.refundFeeRate = refundFeeRate; }

    public Integer getRefundCooldownHours() { return refundCooldownHours; }
    public void setRefundCooldownHours(Integer refundCooldownHours) { this.refundCooldownHours = refundCooldownHours; }

    public String getBonusHandlingOnRefund() { return bonusHandlingOnRefund; }
    public void setBonusHandlingOnRefund(String bonusHandlingOnRefund) { this.bonusHandlingOnRefund = bonusHandlingOnRefund; }

    public Boolean getRiskControlEnabled() { return riskControlEnabled; }
    public void setRiskControlEnabled(Boolean riskControlEnabled) { this.riskControlEnabled = riskControlEnabled; }

    public Boolean getAnomalyAlertEnabled() { return anomalyAlertEnabled; }
    public void setAnomalyAlertEnabled(Boolean anomalyAlertEnabled) { this.anomalyAlertEnabled = anomalyAlertEnabled; }

    public List<String> getAlertChannels() { return alertChannels; }
    public void setAlertChannels(List<String> alertChannels) { this.alertChannels = alertChannels; }

    public String getCurrentAgreementVersion() { return currentAgreementVersion; }
    public void setCurrentAgreementVersion(String currentAgreementVersion) { this.currentAgreementVersion = currentAgreementVersion; }

    public Boolean getAgreementReconfirmOnChange() { return agreementReconfirmOnChange; }
    public void setAgreementReconfirmOnChange(Boolean agreementReconfirmOnChange) { this.agreementReconfirmOnChange = agreementReconfirmOnChange; }

    public Boolean getFinanceIntegrationEnabled() { return financeIntegrationEnabled; }
    public void setFinanceIntegrationEnabled(Boolean financeIntegrationEnabled) { this.financeIntegrationEnabled = financeIntegrationEnabled; }

    public String getFinanceSystemType() { return financeSystemType; }
    public void setFinanceSystemType(String financeSystemType) { this.financeSystemType = financeSystemType; }

    public Boolean getAutoVoucherGeneration() { return autoVoucherGeneration; }
    public void setAutoVoucherGeneration(Boolean autoVoucherGeneration) { this.autoVoucherGeneration = autoVoucherGeneration; }

    public Boolean getFundCustodyEnabled() { return fundCustodyEnabled; }
    public void setFundCustodyEnabled(Boolean fundCustodyEnabled) { this.fundCustodyEnabled = fundCustodyEnabled; }

    public String getCustodyBank() { return custodyBank; }
    public void setCustodyBank(String custodyBank) { this.custodyBank = custodyBank; }

    public Integer getCustodyRate() { return custodyRate; }
    public void setCustodyRate(Integer custodyRate) { this.custodyRate = custodyRate; }

    public String getCustodyAccount() { return custodyAccount; }
    public void setCustodyAccount(String custodyAccount) { this.custodyAccount = custodyAccount; }
}
