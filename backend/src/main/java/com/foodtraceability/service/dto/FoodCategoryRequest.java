package com.foodtraceability.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class FoodCategoryRequest {
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    private String categoryCode;
    private String parentId;
    private Integer sortOrder;
    private String description;
    private String status;

    public FoodCategoryRequest() {
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public String getCategoryCode() {
        return this.categoryCode;
    }

    public String getParentId() {
        return this.parentId;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public String getDescription() {
        return this.description;
    }

    public String getStatus() {
        return this.status;
    }

    public void setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
    }

    public void setCategoryCode(final String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FoodCategoryRequest)) return false;
        final FoodCategoryRequest other = (FoodCategoryRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$categoryName = this.getCategoryName();
        final java.lang.Object other$categoryName = other.getCategoryName();
        if (this$categoryName == null ? other$categoryName != null : !this$categoryName.equals(other$categoryName)) return false;
        final java.lang.Object this$categoryCode = this.getCategoryCode();
        final java.lang.Object other$categoryCode = other.getCategoryCode();
        if (this$categoryCode == null ? other$categoryCode != null : !this$categoryCode.equals(other$categoryCode)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FoodCategoryRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $categoryName = this.getCategoryName();
        result = result * PRIME + ($categoryName == null ? 43 : $categoryName.hashCode());
        final java.lang.Object $categoryCode = this.getCategoryCode();
        result = result * PRIME + ($categoryCode == null ? 43 : $categoryCode.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FoodCategoryRequest(categoryName=" + this.getCategoryName() + ", categoryCode=" + this.getCategoryCode() + ", parentId=" + this.getParentId() + ", sortOrder=" + this.getSortOrder() + ", description=" + this.getDescription() + ", status=" + this.getStatus() + ")";
    }
}
