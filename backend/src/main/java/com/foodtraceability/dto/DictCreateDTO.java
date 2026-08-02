package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 字典类型创建DTO
 * 用于接收创建字典类型的请求参数
 */
@Schema(description = "字典类型创建请求")
public class DictCreateDTO {

    @NotBlank(message = "字典名称不能为空")
    @Size(min = 1, max = 100, message = "字典名称长度必须在1-100个字符之间")
    @Schema(description = "字典名称", example = "用户状态")
    private String dictName;

    @NotBlank(message = "字典编码不能为空")
    @Size(min = 1, max = 100, message = "字典编码长度必须在1-100个字符之间")
    @Schema(description = "字典编码（唯一标识）", example = "user_status")
    private String dictCode;

    @NotBlank(message = "字典分组不能为空")
    @Size(max = 50, message = "字典分组长度不能超过50个字符")
    @Schema(description = "字典分组", example = "system")
    private String dictGroup;

    @Size(max = 500, message = "字典描述长度不能超过500个字符")
    @Schema(description = "字典描述", example = "用于管理用户账号的状态选项")
    private String description;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
