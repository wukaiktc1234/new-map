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
 * 标准成本卡实体类
 * 用于管理菜品的标准成本，包括标准成本、损耗系数及预警状态，支持成本控制与分析
 */
@TableName("standard_cost_cards")
public class StandardCostCard extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成本卡ID */
    @TableId(type = IdType.AUTO)
    private Long cardId;

    /** 菜品ID */
    private Long dishId;

    /** 菜品名称 */
    private String dishName;

    /** 标准成本（分） */
    private Long standardCost;

    /** 损耗系数 */
    private BigDecimal lossCoefficient;

    /**
     * 预警状态
     * 0-正常 1-预警 2-异常
     */
    private Integer warningStatus;

    /** 最后更新日期 */
    private LocalDate lastUpdateDate;

    /** 备注 */
    private String remark;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public Long getStandardCost() {
        return standardCost;
    }

    public void setStandardCost(Long standardCost) {
        this.standardCost = standardCost;
    }

    public BigDecimal getLossCoefficient() {
        return lossCoefficient;
    }

    public void setLossCoefficient(BigDecimal lossCoefficient) {
        this.lossCoefficient = lossCoefficient;
    }

    public Integer getWarningStatus() {
        return warningStatus;
    }

    public void setWarningStatus(Integer warningStatus) {
        this.warningStatus = warningStatus;
    }

    public LocalDate getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(LocalDate lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "StandardCostCard{" +
                "cardId=" + cardId +
                ", dishId=" + dishId +
                ", dishName='" + dishName + '\'' +
                ", standardCost=" + standardCost +
                ", lossCoefficient=" + lossCoefficient +
                ", warningStatus=" + warningStatus +
                '}';
    }
}
