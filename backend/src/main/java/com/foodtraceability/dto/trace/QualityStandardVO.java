package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 质量标准VO - 前端展示对象
 * 包含质量标准所有字段及中文名称映射
 */
@Schema(description = "质量标准信息")
public class QualityStandardVO {

    /** 质量标准ID */
    @Schema(description = "质量标准ID")
    private Long standardId;

    /** 标准编号 */
    @Schema(description = "标准编号")
    private String standardNo;

    /** 标准名称 */
    @Schema(description = "标准名称")
    private String standardName;

    /** 标准类型：NATIONAL/INDUSTRY/ENTERPRISE/LOCAL */
    @Schema(description = "标准类型")
    private String standardType;

    /** 标准类型中文名 */
    @Schema(description = "标准类型中文名")
    private String standardTypeName;

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

    /** 状态：ACTIVE/INACTIVE */
    @Schema(description = "状态")
    private String status;

    /** 状态中文名 */
    @Schema(description = "状态中文名")
    private String statusName;

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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getter和Setter方法

    public Long getStandardId() { return standardId; }
    public void setStandardId(Long standardId) { this.standardId = standardId; }
    public String getStandardNo() { return standardNo; }
    public void setStandardNo(String standardNo) { this.standardNo = standardNo; }
    public String getStandardName() { return standardName; }
    public void setStandardName(String standardName) { this.standardName = standardName; }
    public String getStandardType() { return standardType; }
    public void setStandardType(String standardType) { this.standardType = standardType; }
    public String getStandardTypeName() { return standardTypeName; }
    public void setStandardTypeName(String standardTypeName) { this.standardTypeName = standardTypeName; }
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
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
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
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
