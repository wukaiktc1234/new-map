package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品可做份数预测（方案D+）
 * 基于近 N 天真实经营数据自校准"实际每份用量"（含损耗），预测可做份数/售罄时间/补货量。
 */
@Schema(description = "菜品可做份数预测")
public class StockForecastVO {

    @Schema(description = "菜品ID")
    private Long dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "是否可制作（瓶颈原料充足）")
    private Boolean canMake;

    @Schema(description = "可做份数（按实际每份用量，瓶颈原料）")
    private Integer maxServings;

    @Schema(description = "理论可做份数（按 BOM 用量，无损耗）")
    private Integer theoreticalServings;

    @Schema(description = "是否有足够销量数据校准（无则回退理论值）")
    private Boolean hasSufficientData;

    @Schema(description = "数据窗口（天）")
    private Integer dataWindowDays;

    @Schema(description = "该菜品日均售出份数")
    private BigDecimal avgDailySales;

    @Schema(description = "预计售罄天数（=可做份数/日均销量）")
    private BigDecimal daysToSellout;

    @Schema(description = "建议补货量（瓶颈原料补到安全库存）")
    private Integer suggestedRestock;

    @Schema(description = "瓶颈原料名称")
    private String bottleneckMaterialName;

    @Schema(description = "原料明细")
    private List<StockForecastItemVO> items;

    public Long getDishId() { return dishId; }
    public void setDishId(Long dishId) { this.dishId = dishId; }
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public Boolean getCanMake() { return canMake; }
    public void setCanMake(Boolean canMake) { this.canMake = canMake; }
    public Integer getMaxServings() { return maxServings; }
    public void setMaxServings(Integer maxServings) { this.maxServings = maxServings; }
    public Integer getTheoreticalServings() { return theoreticalServings; }
    public void setTheoreticalServings(Integer theoreticalServings) { this.theoreticalServings = theoreticalServings; }
    public Boolean getHasSufficientData() { return hasSufficientData; }
    public void setHasSufficientData(Boolean hasSufficientData) { this.hasSufficientData = hasSufficientData; }
    public Integer getDataWindowDays() { return dataWindowDays; }
    public void setDataWindowDays(Integer dataWindowDays) { this.dataWindowDays = dataWindowDays; }
    public BigDecimal getAvgDailySales() { return avgDailySales; }
    public void setAvgDailySales(BigDecimal avgDailySales) { this.avgDailySales = avgDailySales; }
    public BigDecimal getDaysToSellout() { return daysToSellout; }
    public void setDaysToSellout(BigDecimal daysToSellout) { this.daysToSellout = daysToSellout; }
    public Integer getSuggestedRestock() { return suggestedRestock; }
    public void setSuggestedRestock(Integer suggestedRestock) { this.suggestedRestock = suggestedRestock; }
    public String getBottleneckMaterialName() { return bottleneckMaterialName; }
    public void setBottleneckMaterialName(String bottleneckMaterialName) { this.bottleneckMaterialName = bottleneckMaterialName; }
    public List<StockForecastItemVO> getItems() { return items; }
    public void setItems(List<StockForecastItemVO> items) { this.items = items; }
}
