package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * POS菜单DTO
 * 用于收银终端完整菜单数据传输（包含分类、菜品和套餐）
 */
@Schema(description = "POS菜单DTO")
public class PosMenuDTO {

    @Schema(description = "分类列表")
    private List<PosCategoryDTO> categories;

    @Schema(description = "菜品列表")
    private List<PosDishDTO> dishes;

    @Schema(description = "套餐列表")
    private List<PosComboDTO> combos;

    public PosMenuDTO() {
    }

    public List<PosCategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<PosCategoryDTO> categories) {
        this.categories = categories;
    }

    public List<PosDishDTO> getDishes() {
        return dishes;
    }

    public void setDishes(List<PosDishDTO> dishes) {
        this.dishes = dishes;
    }

    public List<PosComboDTO> getCombos() {
        return combos;
    }

    public void setCombos(List<PosComboDTO> combos) {
        this.combos = combos;
    }
}
