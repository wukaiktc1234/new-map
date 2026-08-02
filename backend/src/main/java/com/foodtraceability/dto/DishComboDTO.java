package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "套餐DTO")
public class DishComboDTO {
    @Schema(description = "套餐ID")
    private Long id;
    @Schema(description = "套餐编码，格式为T+00000，例如：T00001")
    private String comboCode;
    @Schema(description = "套餐名称")
    private String name;
    @Schema(description = "套餐价格")
    private BigDecimal price;
    @Schema(description = "套餐成本价")
    private BigDecimal costPrice;
    @Schema(description = "包含菜品ID列表")
    private String dishes;
    @Schema(description = "套餐描述")
    private String description;
    @Schema(description = "套餐状态：active-启用，inactive-停用")
    private String status;
    @Schema(description = "套餐图片URL")
    private String imageUrl;
    @Schema(description = "优先级，数字越大优先级越高")
    private Integer priority;
    @Schema(description = "适用人数")
    private Integer peopleCount;
    @Schema(description = "套餐类型：regular-普通套餐，special-特色套餐，family-家庭套餐")
    private String comboType;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getter methods
    public Long getId() {
        return id;
    }

    public String getComboCode() {
        return comboCode;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public String getDishes() {
        return dishes;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getPriority() {
        return priority;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public String getComboType() {
        return comboType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setComboCode(String comboCode) {
        this.comboCode = comboCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setDishes(String dishes) {
        this.dishes = dishes;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public void setComboType(String comboType) {
        this.comboType = comboType;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public DishComboDTO() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DishComboDTO)) return false;
        final DishComboDTO other = (DishComboDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$priority = this.getPriority();
        final java.lang.Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority)) return false;
        final java.lang.Object this$peopleCount = this.getPeopleCount();
        final java.lang.Object other$peopleCount = other.getPeopleCount();
        if (this$peopleCount == null ? other$peopleCount != null : !this$peopleCount.equals(other$peopleCount)) return false;
        final java.lang.Object this$comboCode = this.getComboCode();
        final java.lang.Object other$comboCode = other.getComboCode();
        if (this$comboCode == null ? other$comboCode != null : !this$comboCode.equals(other$comboCode)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$price = this.getPrice();
        final java.lang.Object other$price = other.getPrice();
        if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
        final java.lang.Object this$costPrice = this.getCostPrice();
        final java.lang.Object other$costPrice = other.getCostPrice();
        if (this$costPrice == null ? other$costPrice != null : !this$costPrice.equals(other$costPrice)) return false;
        final java.lang.Object this$dishes = this.getDishes();
        final java.lang.Object other$dishes = other.getDishes();
        if (this$dishes == null ? other$dishes != null : !this$dishes.equals(other$dishes)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$imageUrl = this.getImageUrl();
        final java.lang.Object other$imageUrl = other.getImageUrl();
        if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) return false;
        final java.lang.Object this$comboType = this.getComboType();
        final java.lang.Object other$comboType = other.getComboType();
        if (this$comboType == null ? other$comboType != null : !this$comboType.equals(other$comboType)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DishComboDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $peopleCount = this.getPeopleCount();
        result = result * PRIME + ($peopleCount == null ? 43 : $peopleCount.hashCode());
        final java.lang.Object $comboCode = this.getComboCode();
        result = result * PRIME + ($comboCode == null ? 43 : $comboCode.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $price = this.getPrice();
        result = result * PRIME + ($price == null ? 43 : $price.hashCode());
        final java.lang.Object $costPrice = this.getCostPrice();
        result = result * PRIME + ($costPrice == null ? 43 : $costPrice.hashCode());
        final java.lang.Object $dishes = this.getDishes();
        result = result * PRIME + ($dishes == null ? 43 : $dishes.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $imageUrl = this.getImageUrl();
        result = result * PRIME + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        final java.lang.Object $comboType = this.getComboType();
        result = result * PRIME + ($comboType == null ? 43 : $comboType.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DishComboDTO(id=" + this.getId() + ", comboCode=" + this.getComboCode() + ", name=" + this.getName() + ", price=" + this.getPrice() + ", costPrice=" + this.getCostPrice() + ", dishes=" + this.getDishes() + ", description=" + this.getDescription() + ", status=" + this.getStatus() + ", imageUrl=" + this.getImageUrl() + ", priority=" + this.getPriority() + ", peopleCount=" + this.getPeopleCount() + ", comboType=" + this.getComboType() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
