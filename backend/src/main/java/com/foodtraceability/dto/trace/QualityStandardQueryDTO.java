package com.foodtraceability.dto.trace;

import com.foodtraceability.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 质量标准查询DTO
 * 用于质量标准列表分页查询
 */
@Schema(description = "质量标准查询条件")
public class QualityStandardQueryDTO extends PageQuery {

    /** 标准名称 */
    @Schema(description = "标准名称")
    private String standardName;

    /** 标准类型：NATIONAL/INDUSTRY/ENTERPRISE/LOCAL */
    @Schema(description = "标准类型")
    private String standardType;

    /** 食品分类 */
    @Schema(description = "食品分类")
    private String category;

    /** 适用物料名称 */
    @Schema(description = "适用物料名称")
    private String targetMaterialName;

    /** 状态：ACTIVE/INACTIVE */
    @Schema(description = "状态")
    private String status;

    // Getter和Setter方法

    public String getStandardName() { return standardName; }
    public void setStandardName(String standardName) { this.standardName = standardName; }
    public String getStandardType() { return standardType; }
    public void setStandardType(String standardType) { this.standardType = standardType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTargetMaterialName() { return targetMaterialName; }
    public void setTargetMaterialName(String targetMaterialName) { this.targetMaterialName = targetMaterialName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
