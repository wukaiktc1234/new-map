package com.foodtraceability.dto.product;

import java.io.Serializable;

/**
 * 替代菜品 VO
 * 当原菜品因缺料无法制作时，推荐的可替代菜品
 */
public class AlternativeDishVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 菜品ID */
    private String dishId;
    /** 菜品名称 */
    private String dishName;
    /** 所属分类 */
    private String category;
    /** 售价（分） */
    private Long price;
    /** 是否可制作 */
    private Boolean canMake;
    /** 相似度评分（0-100） */
    private Double similarityScore;
    /** 推荐理由 */
    private String reason;

    public String getDishId() { return dishId; }
    public void setDishId(String dishId) { this.dishId = dishId; }
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Boolean getCanMake() { return canMake; }
    public void setCanMake(Boolean canMake) { this.canMake = canMake; }
    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
