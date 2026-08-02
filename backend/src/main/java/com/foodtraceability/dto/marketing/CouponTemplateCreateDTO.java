package com.foodtraceability.dto.marketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 优惠券模板创建DTO
 */
public class CouponTemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotNull(message = "优惠券类型不能为空")
    /** 1满减券 2折扣券 3兑换券 4代金券 5新人券 */
    private Integer couponType;

    @NotNull(message = "折扣类型不能为空")
    /** 1固定金额减免 2百分比折扣 */
    private Integer discountType;

    @NotNull(message = "优惠值不能为空")
    /** 固定金额（分）或百分比（*100） */
    private Long discountValue;

    /** 最低消费门槛（分） */
    private Long minConsumption = 0L;

    /** 最大优惠金额上限（分） */
    private Long maxDiscount;

    /** 发放总量（-1不限） */
    private Integer totalQuantity = -1;

    /** 每人限领数量 */
    private Integer perPersonLimit = 1;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime validStartDate;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime validExpiryDate;

    /** 领取后有效天数 */
    private Integer validDays = 30;

    /** 1全场通用 2指定品类 3指定商品 4指定渠道 */
    private Integer applicableScope = 1;

    /** 适用范围配置JSON */
    private String scopeConfig;

    /** 使用规则说明 */
    private String usageRules;

    // Getter & Setter
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public Integer getCouponType() { return couponType; }
    public void setCouponType(Integer couponType) { this.couponType = couponType; }
    public Integer getDiscountType() { return discountType; }
    public void setDiscountType(Integer discountType) { this.discountType = discountType; }
    public Long getDiscountValue() { return discountValue; }
    public void setDiscountValue(Long discountValue) { this.discountValue = discountValue; }
    public Long getMinConsumption() { return minConsumption; }
    public void setMinConsumption(Long minConsumption) { this.minConsumption = minConsumption; }
    public Long getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Long maxDiscount) { this.maxDiscount = maxDiscount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getPerPersonLimit() { return perPersonLimit; }
    public void setPerPersonLimit(Integer perPersonLimit) { this.perPersonLimit = perPersonLimit; }
    public LocalDateTime getValidStartDate() { return validStartDate; }
    public void setValidStartDate(LocalDateTime validStartDate) { this.validStartDate = validStartDate; }
    public LocalDateTime getValidExpiryDate() { return validExpiryDate; }
    public void setValidExpiryDate(LocalDateTime validExpiryDate) { this.validExpiryDate = validExpiryDate; }
    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer validDays) { this.validDays = validDays; }
    public Integer getApplicableScope() { return applicableScope; }
    public void setApplicableScope(Integer applicableScope) { this.applicableScope = applicableScope; }
    public String getScopeConfig() { return scopeConfig; }
    public void setScopeConfig(String scopeConfig) { this.scopeConfig = scopeConfig; }
    public String getUsageRules() { return usageRules; }
    public void setUsageRules(String usageRules) { this.usageRules = usageRules; }
}
