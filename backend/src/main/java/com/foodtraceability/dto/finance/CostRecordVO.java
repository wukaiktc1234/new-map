package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本记录VO
 */
public class CostRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成本记录ID */
    private Long costId;

    /** 成本编号 */
    private String costNo;

    /** 成本类型 */
    private Integer costType;

    /** 成本类型名称 */
    private String costTypeName;

    /** 成本归属期间 */
    private String period;

    /** 成本中心/门店ID */
    private Long costCenterId;

    /** 成本中心/门店名称 */
    private String costCenterName;

    /** 金额（单位：分） */
    private Long amount;

    /** 金额（元，用于显示） */
    private String amountDisplay;

    /** 数量 */
    private BigDecimal quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 计算方式 */
    private Integer calculationMethod;

    /** 计算方式名称 */
    private String calculationMethodName;

    /** 关联凭证ID */
    private Long relatedVoucherId;

    /** 凭证号 */
    private String voucherNo;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    // getter和setter方法（简洁格式）
    public Long getCostId() { return costId; }
    public void setCostId(Long costId) { this.costId = costId; }
    public String getCostNo() { return costNo; }
    public void setCostNo(String costNo) { this.costNo = costNo; }
    public Integer getCostType() { return costType; }
    public void setCostType(Integer costType) { this.costType = costType; }
    public String getCostTypeName() { return costTypeName; }
    public void setCostTypeName(String costTypeName) { this.costTypeName = costTypeName; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long costCenterId) { this.costCenterId = costCenterId; }
    public String getCostCenterName() { return costCenterName; }
    public void setCostCenterName(String costCenterName) { this.costCenterName = costCenterName; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getAmountDisplay() { return amountDisplay; }
    public void setAmountDisplay(String amountDisplay) { this.amountDisplay = amountDisplay; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(Integer calculationMethod) { this.calculationMethod = calculationMethod; }
    public String getCalculationMethodName() { return calculationMethodName; }
    public void setCalculationMethodName(String calculationMethodName) { this.calculationMethodName = calculationMethodName; }
    public Long getRelatedVoucherId() { return relatedVoucherId; }
    public void setRelatedVoucherId(Long relatedVoucherId) { this.relatedVoucherId = relatedVoucherId; }
    public String getVoucherNo() { return voucherNo; }
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
