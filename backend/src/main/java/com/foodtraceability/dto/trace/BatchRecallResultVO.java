package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 批量召回结果VO
 * 返回批量召回的成功/失败统计
 */
@Schema(description = "批量召回结果")
public class BatchRecallResultVO {

    /** 总数 */
    @Schema(description = "总数")
    private int totalCount;

    /** 成功数 */
    @Schema(description = "成功数")
    private int successCount;

    /** 失败数 */
    @Schema(description = "失败数")
    private int failCount;

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
}
