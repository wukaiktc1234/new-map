package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商评估实体类
 * 用于管理供应商的定期评估信息，支持自动计算和手动创建
 */
@TableName("supplier_evaluations")
@Schema(description = "供应商评估实体")
public class SupplierEvaluation {

    /**
     * 评估主键ID（自增）
     */
    @TableId(value = "evaluation_id", type = IdType.AUTO)
    @Schema(description = "评估主键ID", example = "1")
    private Long evaluationId;

    /**
     * 供应商ID
     */
    @TableField("supplier_id")
    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    /**
     * 评估周期（如2026-04）
     */
    @TableField("evaluation_period")
    @Schema(description = "评估周期", example = "2026-04")
    private String evaluationPeriod;

    /**
     * 交货及时性评分
     */
    @TableField("delivery_score")
    @Schema(description = "交货及时性评分", example = "4.50")
    private BigDecimal deliveryScore;

    /**
     * 质量评分
     */
    @TableField("quality_score")
    @Schema(description = "质量评分", example = "4.80")
    private BigDecimal qualityScore;

    /**
     * 价格竞争力评分
     */
    @TableField("price_score")
    @Schema(description = "价格竞争力评分", example = "4.20")
    private BigDecimal priceScore;

    /**
     * 服务评分
     */
    @TableField("service_score")
    @Schema(description = "服务评分", example = "4.60")
    private BigDecimal serviceScore;

    /**
     * 综合评分
     */
    @TableField("total_score")
    @Schema(description = "综合评分", example = "4.53")
    private BigDecimal totalScore;

    /**
     * 本期订单数
     */
    @TableField("order_count")
    @Schema(description = "本期订单数", example = "15")
    private Integer orderCount;

    /**
     * 及时交货率
     */
    @TableField("on_time_rate")
    @Schema(description = "及时交货率", example = "93.33")
    private BigDecimal onTimeRate;

    /**
     * 质量合格率
     */
    @TableField("qualified_rate")
    @Schema(description = "质量合格率", example = "98.50")
    private BigDecimal qualifiedRate;

    /**
     * 评估人ID
     */
    @TableField("evaluation_user_id")
    @Schema(description = "评估人ID", example = "1")
    private Long evaluationUserId;

    /**
     * 评估时间
     */
    @TableField("evaluation_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "评估时间")
    private LocalDateTime evaluationTime;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0未删除 1已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // ==================== Getter & Setter ====================

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }

    public void setEvaluationPeriod(String evaluationPeriod) {
        this.evaluationPeriod = evaluationPeriod;
    }

    public BigDecimal getDeliveryScore() {
        return deliveryScore;
    }

    public void setDeliveryScore(BigDecimal deliveryScore) {
        this.deliveryScore = deliveryScore;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }

    public BigDecimal getPriceScore() {
        return priceScore;
    }

    public void setPriceScore(BigDecimal priceScore) {
        this.priceScore = priceScore;
    }

    public BigDecimal getServiceScore() {
        return serviceScore;
    }

    public void setServiceScore(BigDecimal serviceScore) {
        this.serviceScore = serviceScore;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(BigDecimal onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public BigDecimal getQualifiedRate() {
        return qualifiedRate;
    }

    public void setQualifiedRate(BigDecimal qualifiedRate) {
        this.qualifiedRate = qualifiedRate;
    }

    public Long getEvaluationUserId() {
        return evaluationUserId;
    }

    public void setEvaluationUserId(Long evaluationUserId) {
        this.evaluationUserId = evaluationUserId;
    }

    public LocalDateTime getEvaluationTime() {
        return evaluationTime;
    }

    public void setEvaluationTime(LocalDateTime evaluationTime) {
        this.evaluationTime = evaluationTime;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
