package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 字典类型更新DTO
 * 用于接收更新字典类型的请求参数
 */
@Schema(description = "字典类型更新请求")
public class DictUpdateDTO {

    @NotBlank(message = "字典名称不能为空")
    @Size(min = 1, max = 100, message = "字典名称长度必须在1-100个字符之间")
    @Schema(description = "字典名称", example = "用户状态")
    private String dictName;

    @Size(max = 50, message = "字典分组长度不能超过50个字符")
    @Schema(description = "字典分组", example = "system")
    private String dictGroup;

    @Size(max = 500, message = "字典描述长度不能超过500个字符")
    @Schema(description = "字典描述")
    private String description;

    @Schema(description = "状态：0-禁用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictGroup() {
        return dictGroup;
    }

    public void setDictGroup(String dictGroup) {
        this.dictGroup = dictGroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
