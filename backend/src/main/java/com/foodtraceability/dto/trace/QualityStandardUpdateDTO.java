package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 质量标准更新DTO
 * 用于更新质量标准信息，所有字段可选（除主键外）
 */
@Schema(description = "质量标准更新请求")
public class QualityStandardUpdateDTO {

    /** 质量标准ID */
    @NotNull(message = "质量标准ID不能为空")
    @Schema(description = "质量标准ID")
    private Long standardId;

    /** 标准编号 */
    @Schema(description = "标准编号")
    private String standardNo;

    /** 标准名称 */
    @Schema(description = "标准名称")
    private String standardName;

    /** 标准类型：NATIONAL国标/INDUSTRY行标/ENTERPRISE企标/LOCAL地方 */
    @Schema(description = "标准类型")
    private String standardType;

    /** 食品分类 */
    @Schema(description = "食品分类")
    private String category;

    /** 适用物料ID */
    @Schema(description = "适用物料ID")
    private String targetMaterialId;

    /** 适用物料名称 */
    @Schema(description = "适用物料名称")
    private String targetMaterialName;

    /** 指标项JSON数组 */
    @Schema(description = "指标项JSON数组")
    private String indicatorItems;

    /** 状态：ACTIVE生效/INACTIVE失效 */
    @Schema(description = "状态")
    private String status;

    /** 生效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    /** 失效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    /** 发布机构 */
    @Schema(description = "发布机构")
    private String issuingAuthority;

    /** 版本号 */
    @Schema(description = "版本号")
    private String version;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    // Getter和Setter方法

    public Long getStandardId() { return standardId; }
    public void setStandardId(Long standardId) { this.standardId = standardId; }
    public String getStandardNo() { return standardNo; }
    public void setStandardNo(String standardNo) { this.standardNo = standardNo; }
    public String getStandardName() { return standardName; }
    public void setStandardName(String standardName) { this.standardName = standardName; }
    public String getStandardType() { return standardType; }
    public void setStandardType(String standardType) { this.standardType = standardType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTargetMaterialId() { return targetMaterialId; }
    public void setTargetMaterialId(String targetMaterialId) { this.targetMaterialId = targetMaterialId; }
    public String getTargetMaterialName() { return targetMaterialName; }
    public void setTargetMaterialName(String targetMaterialName) { this.targetMaterialName = targetMaterialName; }
    public String getIndicatorItems() { return indicatorItems; }
    public void setIndicatorItems(String indicatorItems) { this.indicatorItems = indicatorItems; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
