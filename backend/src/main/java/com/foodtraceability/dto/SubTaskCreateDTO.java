package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建子任务请求DTO
 */
public class SubTaskCreateDTO {

    /** 子任务标题 */
    @NotBlank(message = "子任务标题不能为空")
    private String title;

    /** 子任务描述 */
    private String description;

    /** 排序序号 */
    private Integer sortOrder;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
