package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 检验记录统计VO
 * 用于检验记录统计信息展示
 */
@Schema(description = "检验记录统计信息")
public class InspectionStatisticsVO {

    /** 总检验次数 */
    @Schema(description = "总检验次数")
    private Long totalCount;

    /** 合格次数 */
    @Schema(description = "合格次数")
    private Long qualifiedCount;

    /** 不合格次数 */
    @Schema(description = "不合格次数")
    private Long unqualifiedCount;

    /** 有条件合格次数 */
    @Schema(description = "有条件合格次数")
    private Long conditionalCount;

    /** 合格率（百分比） */
    @Schema(description = "合格率（百分比）")
    private BigDecimal qualificationRate;

    /** 按检验类型统计列表 */
    @Schema(description = "按检验类型统计列表")
    private List<Map<String, Object>> typeStatistics;

    // Getter和Setter方法

    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public Long getQualifiedCount() { return qualifiedCount; }
    public void setQualifiedCount(Long qualifiedCount) { this.qualifiedCount = qualifiedCount; }
    public Long getUnqualifiedCount() { return unqualifiedCount; }
    public void setUnqualifiedCount(Long unqualifiedCount) { this.unqualifiedCount = unqualifiedCount; }
    public Long getConditionalCount() { return conditionalCount; }
    public void setConditionalCount(Long conditionalCount) { this.conditionalCount = conditionalCount; }
    public BigDecimal getQualificationRate() { return qualificationRate; }
    public void setQualificationRate(BigDecimal qualificationRate) { this.qualificationRate = qualificationRate; }
    public List<Map<String, Object>> getTypeStatistics() { return typeStatistics; }
    public void setTypeStatistics(List<Map<String, Object>> typeStatistics) { this.typeStatistics = typeStatistics; }
}
