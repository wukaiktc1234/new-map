package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识库分类统计 VO
 */
@Schema(description = "知识库分类统计VO")
public class KnowledgeCategoryStatVO {

    @Schema(description = "分类（safety/service/manual/policy）")
    private String category;

    @Schema(description = "分类标签")
    private String label;

    @Schema(description = "文章总数")
    private Integer count;

    @Schema(description = "已发布文章数")
    private Integer publishedCount;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getPublishedCount() {
        return publishedCount;
    }

    public void setPublishedCount(Integer publishedCount) {
        this.publishedCount = publishedCount;
    }
}
