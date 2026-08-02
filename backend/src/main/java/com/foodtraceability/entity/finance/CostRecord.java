package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成本记录实体类
 * 记录企业各项成本支出，用于成本核算和分析
 */
@TableName("cost_records")
public class CostRecord extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成本记录ID */
    @TableId(type = IdType.AUTO)
    private Long costId;

    /** 成本编号，唯一标识 */
    private String costNo;

    /**
     * 成本类型
     * 1-食材成本 2-人工成本 3-租金成本 4-水电成本 5-折旧成本 6-包装成本 7-其他成本
     */
    private Integer costType;

    /** 成本归属期间，如2026-04 */
    private String period;

    /** 成本中心/门店ID */
    private Long costCenterId;

    /** 金额（单位：分） */
    private Long amount;

    /** 数量 */
    private BigDecimal quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /**
     * 计算方式
     * 1-实际发生 2-分摊 3-预估
     */
    private Integer calculationMethod;

    /** 关联凭证ID */
    private Long relatedVoucherId;

    /** 备注 */
    private String remark;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getCostId() {
        return costId;
    }

    public void setCostId(Long costId) {
        this.costId = costId;
    }

    public String getCostNo() {
        return costNo;
    }

    public void setCostNo(String costNo) {
        this.costNo = costNo;
    }

    public Integer getCostType() {
        return costType;
    }

    public void setCostType(Integer costType) {
        this.costType = costType;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Long getCostCenterId() {
        return costCenterId;
    }

    public void setCostCenterId(Long costCenterId) {
        this.costCenterId = costCenterId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(Integer calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public Long getRelatedVoucherId() {
        return relatedVoucherId;
    }

    public void setRelatedVoucherId(Long relatedVoucherId) {
        this.relatedVoucherId = relatedVoucherId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
