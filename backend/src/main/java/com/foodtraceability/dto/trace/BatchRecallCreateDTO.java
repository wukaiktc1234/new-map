package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量召回创建DTO
 * 用于批量将多个追溯码标记为已召回
 * 注意：操作人信息不通过请求体传递，由后端从安全上下文获取
 */
@Schema(description = "批量召回创建请求")
public class BatchRecallCreateDTO {

    /** 待召回的追溯码ID列表 */
    @NotEmpty(message = "追溯码ID列表不能为空")
    @Schema(description = "待召回的追溯码ID列表")
    private List<Long> traceCodeIds;

    /** 召回原因 */
    @Schema(description = "召回原因")
    private String reason;

    public List<Long> getTraceCodeIds() { return traceCodeIds; }
    public void setTraceCodeIds(List<Long> traceCodeIds) { this.traceCodeIds = traceCodeIds; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
