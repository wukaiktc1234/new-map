package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * POS套餐DTO
 * 用于收银终端套餐数据传输
 */
@Schema(description = "POS套餐DTO")
public class PosComboDTO {

    @Schema(description = "套餐ID")
    private String comboId;

    @Schema(description = "套餐编码")
    private String comboCode;

    @Schema(description = "套餐名称")
    private String comboName;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "套餐类型")
    private String comboType;

    @Schema(description = "适用人数")
    private Integer peopleCount;

    @Schema(description = "菜品类型：combo-套餐")
    private String dishType;

    @Schema(description = "套餐包含的菜品列表")
    private List<PosComboItemDTO> items;

    public PosComboDTO() {
    }

    public String getComboId() {
        return comboId;
    }

    public void setComboId(String comboId) {
        this.comboId = comboId;
    }

    public String getComboCode() {
        return comboCode;
    }

    public void setComboCode(String comboCode) {
        this.comboCode = comboCode;
    }

    public String getComboName() {
        return comboName;
    }

    public void setComboName(String comboName) {
        this.comboName = comboName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getComboType() {
        return comboType;
    }

    public void setComboType(String comboType) {
        this.comboType = comboType;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getDishType() {
        return dishType;
    }

    public void setDishType(String dishType) {
        this.dishType = dishType;
    }

    public List<PosComboItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PosComboItemDTO> items) {
        this.items = items;
    }
}
