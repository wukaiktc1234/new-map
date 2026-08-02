package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 字典项创建DTO
 * 用于接收创建字典项的请求参数
 */
@Schema(description = "字典项创建请求")
public class DictItemCreateDTO {

    @NotNull(message = "关联字典ID不能为空")
    @Schema(description = "关联的字典类型ID", example = "1")
    private Long dictId;

    @NotBlank(message = "字典项标签不能为空")
    @Size(min = 1, max = 200, message = "字典项标签长度必须在1-200个字符之间")
    @Schema(description = "字典项标签（显示文本）", example = "启用")
    private String itemLabel;

    @NotBlank(message = "字典项值不能为空")
    @Size(min = 1, max = 200, message = "字典项值长度必须在1-200个字符之间")
    @Schema(description = "字典项值（实际值）", example = "active")
    private String itemValue;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Size(max = 100, message = "CSS样式类长度不能超过100个字符")
    @Schema(description = "CSS样式类", example = "el-tag--success")
    private String cssClass;

    @Size(max = 100, message = "列表样式类长度不能超过100个字符")
    @Schema(description = "列表样式类", example = "el-icon-check")
    private String listClass;

    @Size(max = 20, message = "颜色类型长度不能超过20个字符")
    @Schema(description = "颜色类型：primary/success/warning/danger/info", example = "success")
    private String colorType;

    @Schema(description = "是否默认项：0-否，1-是", example = "0")
    private Integer isDefault;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注说明")
    private String remark;

    public Long getDictId() {
        return dictId;
    }

    public void setDictId(Long dictId) {
        this.dictId = dictId;
    }

    public String getItemLabel() {
        return itemLabel;
    }

    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getListClass() {
        return listClass;
    }

    public void setListClass(String listClass) {
        this.listClass = listClass;
    }

    public String getColorType() {
        return colorType;
    }

    public void setColorType(String colorType) {
        this.colorType = colorType;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
