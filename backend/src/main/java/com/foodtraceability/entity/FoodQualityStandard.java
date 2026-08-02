package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 质量标准实体类
 * 维护国标/行标/企标/地方标准等质量标准库
 */
@TableName("food_quality_standard")
@Schema(description = "质量标准实体")
public class FoodQualityStandard {

    /** 主键ID */
    @TableId(value = "standard_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long standardId;

    /** 标准编号（唯一） */
    @TableField("standard_no")
    @Schema(description = "标准编号", example = "GB2762-2022")
    private String standardNo;

    /** 标准名称 */
    @TableField("standard_name")
    @Schema(description = "标准名称")
    private String standardName;

    /** 标准类型：NATIONAL国标/INDUSTRY行标/ENTERPRISE企标/LOCAL地方 */
    @TableField("standard_type")
    @Schema(description = "标准类型", example = "NATIONAL")
    private String standardType;

    /** 食品分类 */
    @TableField("category")
    @Schema(description = "食品分类")
    private String category;

    /** 适用物料ID */
    @TableField("target_material_id")
    @Schema(description = "适用物料ID")
    private String targetMaterialId;

    /** 适用物料名称 */
    @TableField("target_material_name")
    @Schema(description = "适用物料名称")
    private String targetMaterialName;

    /** 指标项JSON数组：[{name,value,unit,min,max}] */
    @TableField("indicator_items")
    @Schema(description = "指标项JSON数组")
    private String indicatorItems;

    /** 状态：ACTIVE生效/INACTIVE失效 */
    @TableField("status")
    @Schema(description = "状态", example = "ACTIVE")
    private String status;

    /** 生效日期 */
    @TableField("effective_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    /** 失效日期 */
    @TableField("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    /** 发布机构 */
    @TableField("issuing_authority")
    @Schema(description = "发布机构")
    private String issuingAuthority;

    /** 版本号 */
    @TableField("version")
    @Schema(description = "版本号")
    private String version;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    public Long getStandardId() {
        return standardId;
    }

    public void setStandardId(Long standardId) {
        this.standardId = standardId;
    }

    public String getStandardNo() {
        return standardNo;
    }

    public void setStandardNo(String standardNo) {
        this.standardNo = standardNo;
    }

    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    public String getStandardType() {
        return standardType;
    }

    public void setStandardType(String standardType) {
        this.standardType = standardType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTargetMaterialId() {
        return targetMaterialId;
    }

    public void setTargetMaterialId(String targetMaterialId) {
        this.targetMaterialId = targetMaterialId;
    }

    public String getTargetMaterialName() {
        return targetMaterialName;
    }

    public void setTargetMaterialName(String targetMaterialName) {
        this.targetMaterialName = targetMaterialName;
    }

    public String getIndicatorItems() {
        return indicatorItems;
    }

    public void setIndicatorItems(String indicatorItems) {
        this.indicatorItems = indicatorItems;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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
