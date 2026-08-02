package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 财务预警创建DTO
 */
@Schema(description = "财务预警创建请求")
public class FinanceWarningCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "预警类型不能为空")
    @Pattern(regexp = "^(BUDGET_OVER|AR_OVERDUE|INVENTORY_OVER|CASH_FLOW_RISK|TAX_RISK)$",
            message = "预警类型必须为 BUDGET_OVER/AR_OVERDUE/INVENTORY_OVER/CASH_FLOW_RISK/TAX_RISK 之一")
    @Schema(description = "预警类型：BUDGET_OVER(预算超支)、AR_OVERDUE(应收账款逾期)、INVENTORY_OVER(库存积压)、CASH_FLOW_RISK(现金流风险)、TAX_RISK(税务风险)")
    private String warningType;

    @NotBlank(message = "预警级别不能为空")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|URGENT)$",
            message = "预警级别必须为 LOW/MEDIUM/HIGH/URGENT 之一")
    @Schema(description = "预警级别：LOW(低)、MEDIUM(中)、HIGH(高)、URGENT(紧急)")
    private String warningLevel;

    @NotBlank(message = "预警标题不能为空")
    @Size(max = 200, message = "预警标题长度不能超过200个字符")
    @Schema(description = "预警标题")
    private String title;

    @Size(max = 2000, message = "预警内容长度不能超过2000个字符")
    @Schema(description = "预警内容")
    private String content;

    @Size(max = 64, message = "相关业务ID长度不能超过64个字符")
    @Schema(description = "相关业务ID")
    private String businessId;

    @Size(max = 64, message = "相关业务类型长度不能超过64个字符")
    @Schema(description = "相关业务类型")
    private String businessType;

    @Min(value = 0, message = "涉及金额不能为负数")
    @Schema(description = "涉及金额（分）")
    private Long amount;

    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }
    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}
