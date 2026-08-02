package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商评估创建DTO
 * 用于接收创建供应商评估时的请求数据
 */
@Schema(description = "供应商评估创建请求")
public class SupplierEvaluationCreateDTO {

    /**
     * 供应商ID
     */
    @NotNull(message = "供应商ID不能为空")
    @Schema(description = "供应商ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long supplierId;

    /**
     * 评估周期（如2026-04）
     */
    @NotBlank(message = "评估周期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "评估周期格式不正确，应为yyyy-MM")
    @Schema(description = "评估周期", example = "2026-04", requiredMode = Schema.RequiredMode.REQUIRED)
    private String evaluationPeriod;

    /**
     * 交货及时性评分（1-5分）
     */
    @DecimalMin(value = "0", message = "评分不能小于0")
    @DecimalMax(value = "5", message = "评分不能大于5")
    @Schema(description = "交货及时性评分(0-5)", example = "4.50")
    private BigDecimal deliveryScore;

    /**
     * 质量评分（1-5分）
     */
    @DecimalMin(value = "0", message = "评分不能小于0")
    @DecimalMax(value = "5", message = "评分不能大于5")
    @Schema(description = "质量评分(0-5)", example = "4.80")
    private BigDecimal qualityScore;

    /**
     * 价格竞争力评分（1-5分）
     */
    @DecimalMin(value = "0", message = "评分不能小于0")
    @DecimalMax(value = "5", message = "评分不能大于5")
    @Schema(description = "价格竞争力评分(0-5)", example = "4.20")
    private BigDecimal priceScore;

    /**
     * 服务评分（1-5分）
     */
    @DecimalMin(value = "0", message = "评分不能小于0")
    @DecimalMax(value = "5", message = "评分不能大于5")
    @Schema(description = "服务评分(0-5)", example = "4.60")
    private BigDecimal serviceScore;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 计算综合评分（四个维度的平均值）
     * @return 综合评分
     */
    public BigDecimal calculateTotalScore() {
        int count = 0;
        double sum = 0.0;
        if (deliveryScore != null) { sum += deliveryScore.doubleValue(); count++; }
        if (qualityScore != null) { sum += qualityScore.doubleValue(); count++; }
        if (priceScore != null) { sum += priceScore.doubleValue(); count++; }
        if (serviceScore != null) { sum += serviceScore.doubleValue(); count++; }
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(sum / count).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
