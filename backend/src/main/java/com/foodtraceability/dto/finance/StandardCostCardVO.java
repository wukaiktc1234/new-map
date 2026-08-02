package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 标准成本卡VO
 */
@Schema(description = "标准成本卡信息")
public class StandardCostCardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "成本卡ID")
    private Long cardId;

    @Schema(description = "菜品ID")
    private Long dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "标准成本（单位：分）")
    private Long standardCost;

    @Schema(description = "损耗系数（如1.05）")
    private BigDecimal lossCoefficient;

    @Schema(description = "预警状态：0-正常 1-预警 2-异常")
    private Integer warningStatus;

    @Schema(description = "最后更新日期")
    private LocalDate lastUpdateDate;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

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
}
