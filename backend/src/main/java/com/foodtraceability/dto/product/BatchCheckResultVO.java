package com.foodtraceability.dto.product;

import java.io.Serializable;
import java.util.List;

/**
 * BOM 批量检查结果 VO
 */
public class BatchCheckResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 总检查菜品数 */
    private Integer totalChecked;
    /** 可制作菜品数 */
    private Integer canMakeCount;
    /** 缺料菜品数 */
    private Integer insufficientCount;
    /** 整体健康度评分（0-100） */
    private Double healthScore;
    /** 各菜品检查结果 */
    private List<BomCheckResultVO> results;

    public Integer getTotalChecked() { return totalChecked; }
    public void setTotalChecked(Integer totalChecked) { this.totalChecked = totalChecked; }
    public Integer getCanMakeCount() { return canMakeCount; }
    public void setCanMakeCount(Integer canMakeCount) { this.canMakeCount = canMakeCount; }
    public Integer getInsufficientCount() { return insufficientCount; }
    public void setInsufficientCount(Integer insufficientCount) { this.insufficientCount = insufficientCount; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
    public List<BomCheckResultVO> getResults() { return results; }
    public void setResults(List<BomCheckResultVO> results) { this.results = results; }
}
