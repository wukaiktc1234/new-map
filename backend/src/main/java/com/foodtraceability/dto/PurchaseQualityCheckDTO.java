package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 质检信息DTO
 * 用于接收入库单质检时的请求数据
 */
@Schema(description = "质检信息请求")
public class PurchaseQualityCheckDTO {

    /**
     * 质检结果（1合格 2不合格 3待检）
     */
    @NotNull(message = "质检结果不能为空")
    @Min(value = 1, message = "质检结果必须是1/2/3")
    @Max(value = 3, message = "质检结果必须是1/2/3")
    @Schema(description = "质检结果（1-合格, 2-不合格, 3-待检）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer qualityCheckResult;

    /**
     * 质检备注
     */
    @Size(max = 500, message = "质检备注不能超过500个字符")
    @Schema(description = "质检备注")
    private String qualityRemark;

    /**
     * 外观检查结果（normal正常 abnormal异常）
     */
    @Pattern(regexp = "^(normal|abnormal)?$", message = "外观检查结果必须是normal或abnormal")
    @Schema(description = "外观检查结果（normal-正常, abnormal-异常）")
    private String appearanceResult;

    /**
     * 气味检查结果（normal正常 abnormal异常）
     */
    @Pattern(regexp = "^(normal|abnormal)?$", message = "气味检查结果必须是normal或abnormal")
    @Schema(description = "气味检查结果（normal-正常, abnormal-异常）")
    private String odorResult;

    /**
     * 实测温度（℃）
     */
    @DecimalMin(value = "-100.00", message = "温度不能低于-100℃")
    @DecimalMax(value = "100.00", message = "温度不能高于100℃")
    @Digits(integer = 3, fraction = 2, message = "温度格式不正确")
    @Schema(description = "实测温度(℃)")
    private BigDecimal temperature;

    /**
     * 实测湿度（%）
     */
    @DecimalMin(value = "0.00", message = "湿度不能低于0%")
    @DecimalMax(value = "100.00", message = "湿度不能高于100%")
    @Digits(integer = 3, fraction = 2, message = "湿度格式不正确")
    @Schema(description = "实测湿度(%)")
    private BigDecimal humidity;

    /**
     * 抽检数量
     */
    @DecimalMin(value = "0.001", message = "抽检数量必须大于0")
    @Digits(integer = 10, fraction = 3, message = "抽检数量格式不正确")
    @Schema(description = "抽检数量")
    private BigDecimal sampleQuantity;

    /**
     * 抽检比例（%）
     */
    @DecimalMin(value = "0.00", message = "抽检比例不能低于0%")
    @DecimalMax(value = "100.00", message = "抽检比例不能高于100%")
    @Digits(integer = 3, fraction = 2, message = "抽检比例格式不正确")
    @Schema(description = "抽检比例(%)")
    private BigDecimal sampleRate;

    /**
     * 不合格类型
     */
    @Size(max = 50, message = "不合格类型不能超过50个字符")
    @Schema(description = "不合格类型")
    private String unqualifiedType;

    /**
     * 处理意见（return退货 concession让步接收 scrap报废 sort挑选使用）
     */
    @Pattern(regexp = "^(return|concession|scrap|sort)?$", message = "处理意见必须是return/concession/scrap/sort")
    @Schema(description = "处理意见（return-退货, concession-让步接收, scrap-报废, sort-挑选使用）")
    private String disposalOpinion;

    /**
     * 随货单据核查结果（complete齐全 incomplete不齐全 none无）
     */
    @Pattern(regexp = "^(complete|incomplete|none)?$", message = "单据核查结果必须是complete/incomplete/none")
    @Schema(description = "随货单据核查结果（complete-齐全, incomplete-不齐全, none-无）")
    private String documentCheck;

    // ==================== Getter & Setter ====================

    public Integer getQualityCheckResult() {
        return qualityCheckResult;
    }

    public void setQualityCheckResult(Integer qualityCheckResult) {
        this.qualityCheckResult = qualityCheckResult;
    }

    public String getQualityRemark() {
        return qualityRemark;
    }

    public void setQualityRemark(String qualityRemark) {
        this.qualityRemark = qualityRemark;
    }

    public String getAppearanceResult() {
        return appearanceResult;
    }

    public void setAppearanceResult(String appearanceResult) {
        this.appearanceResult = appearanceResult;
    }

    public String getOdorResult() {
        return odorResult;
    }

    public void setOdorResult(String odorResult) {
        this.odorResult = odorResult;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getHumidity() {
        return humidity;
    }

    public void setHumidity(BigDecimal humidity) {
        this.humidity = humidity;
    }

    public BigDecimal getSampleQuantity() {
        return sampleQuantity;
    }

    public void setSampleQuantity(BigDecimal sampleQuantity) {
        this.sampleQuantity = sampleQuantity;
    }

    public BigDecimal getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(BigDecimal sampleRate) {
        this.sampleRate = sampleRate;
    }

    public String getUnqualifiedType() {
        return unqualifiedType;
    }

    public void setUnqualifiedType(String unqualifiedType) {
        this.unqualifiedType = unqualifiedType;
    }

    public String getDisposalOpinion() {
        return disposalOpinion;
    }

    public void setDisposalOpinion(String disposalOpinion) {
        this.disposalOpinion = disposalOpinion;
    }

    public String getDocumentCheck() {
        return documentCheck;
    }

    public void setDocumentCheck(String documentCheck) {
        this.documentCheck = documentCheck;
    }
}
