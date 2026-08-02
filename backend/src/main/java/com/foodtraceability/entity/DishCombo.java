package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("dish_combo")
public class DishCombo {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("combo_code")
    private String comboCode;
    @TableField("name")
    private String comboName;
    @TableField("description")
    private String description;
    @TableField("price")
    private BigDecimal price;
    @TableField("status")
    private String status;
    @TableField("image_url")
    private String imageUrl;
    @TableField("combo_type")
    private String comboType;
    @TableField("people_count")
    private Integer peopleCount;
    @TableField("create_time")
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // Getter methods
    public Long getId() {
        return id;
    }

    public String getComboCode() {
        return comboCode;
    }

    public String getComboName() {
        return comboName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getComboType() {
        return comboType;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setComboCode(String comboCode) {
        this.comboCode = comboCode;
    }

    public void setComboName(String comboName) {
        this.comboName = comboName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setComboType(String comboType) {
        this.comboType = comboType;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public DishCombo() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DishCombo)) return false;
        final DishCombo other = (DishCombo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$peopleCount = this.getPeopleCount();
        final java.lang.Object other$peopleCount = other.getPeopleCount();
        if (this$peopleCount == null ? other$peopleCount != null : !this$peopleCount.equals(other$peopleCount)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$comboCode = this.getComboCode();
        final java.lang.Object other$comboCode = other.getComboCode();
        if (this$comboCode == null ? other$comboCode != null : !this$comboCode.equals(other$comboCode)) return false;
        final java.lang.Object this$comboName = this.getComboName();
        final java.lang.Object other$comboName = other.getComboName();
        if (this$comboName == null ? other$comboName != null : !this$comboName.equals(other$comboName)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$price = this.getPrice();
        final java.lang.Object other$price = other.getPrice();
        if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$imageUrl = this.getImageUrl();
        final java.lang.Object other$imageUrl = other.getImageUrl();
        if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) return false;
        final java.lang.Object this$comboType = this.getComboType();
        final java.lang.Object other$comboType = other.getComboType();
        if (this$comboType == null ? other$comboType != null : !this$comboType.equals(other$comboType)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DishCombo;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $peopleCount = this.getPeopleCount();
        result = result * PRIME + ($peopleCount == null ? 43 : $peopleCount.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $comboCode = this.getComboCode();
        result = result * PRIME + ($comboCode == null ? 43 : $comboCode.hashCode());
        final java.lang.Object $comboName = this.getComboName();
        result = result * PRIME + ($comboName == null ? 43 : $comboName.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $price = this.getPrice();
        result = result * PRIME + ($price == null ? 43 : $price.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $imageUrl = this.getImageUrl();
        result = result * PRIME + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        final java.lang.Object $comboType = this.getComboType();
        result = result * PRIME + ($comboType == null ? 43 : $comboType.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DishCombo(id=" + this.getId() + ", comboCode=" + this.getComboCode() + ", comboName=" + this.getComboName() + ", description=" + this.getDescription() + ", price=" + this.getPrice() + ", status=" + this.getStatus() + ", imageUrl=" + this.getImageUrl() + ", comboType=" + this.getComboType() + ", peopleCount=" + this.getPeopleCount() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
