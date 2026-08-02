package com.foodtraceability.dto.product;

import java.io.Serializable;

/**
 * BOM 库存预警配置 VO
 * 控制下单/出菜环节的库存检查行为
 */
public class BomStockWarningConfigVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private String id;
    /** 是否启用 */
    private Boolean enabled;
    /** 检查时机：order（下单时）/ kitchen_pick（出菜时）/ both（两者都检查） */
    private String checkPoint;
    /** 是否自动推荐替代菜品 */
    private Boolean autoSuggestAlternative;
    /** 低库存阈值百分比 */
    private Double lowStockThreshold;
    /** 是否在下单前预警 */
    private Boolean warnBeforeOrder;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getCheckPoint() { return checkPoint; }
    public void setCheckPoint(String checkPoint) { this.checkPoint = checkPoint; }
    public Boolean getAutoSuggestAlternative() { return autoSuggestAlternative; }
    public void setAutoSuggestAlternative(Boolean autoSuggestAlternative) { this.autoSuggestAlternative = autoSuggestAlternative; }
    public Double getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Double lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public Boolean getWarnBeforeOrder() { return warnBeforeOrder; }
    public void setWarnBeforeOrder(Boolean warnBeforeOrder) { this.warnBeforeOrder = warnBeforeOrder; }
}
