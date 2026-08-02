package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 提前制作请求DTO
 * 用于高峰期提前制作模式的请求参数
 */
@Schema(description = "提前制作请求DTO")
public class PreMakeRequestDTO {

    @NotBlank(message = "菜品ID不能为空")
    @Schema(description = "菜品ID", example = "1")
    private String foodId;

    @NotNull(message = "数量不能为空")
    @Schema(description = "制作数量", example = "5")
    private Integer quantity;

    @Schema(description = "厨师ID", example = "1001")
    private Long chefId;

    @Schema(description = "厨师姓名", example = "张师傅")
    private String chefName;

    @Schema(description = "门店ID", example = "1")
    private Long storeId;

    @Schema(description = "门店名称", example = "中心店")
    private String storeName;

    @Schema(description = "制作工位", example = "炒菜区")
    private String productionStation;

    public PreMakeRequestDTO() {
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getChefId() {
        return chefId;
    }

    public void setChefId(Long chefId) {
        this.chefId = chefId;
    }

    public String getChefName() {
        return chefName;
    }

    public void setChefName(String chefName) {
        this.chefName = chefName;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getProductionStation() {
        return productionStation;
    }

    public void setProductionStation(String productionStation) {
        this.productionStation = productionStation;
    }
}
