package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 召回记录创建DTO
 * 用于发起一次召回操作并记录审计信息
 * 注意：操作人信息不通过请求体传递，由后端从安全上下文获取
 */
@Schema(description = "召回记录创建请求")
public class RecallCreateDTO {

    /** 关联批次号（必填，订单级召回的依据） */
    @NotBlank(message = "批次号不能为空")
    @Size(max = 64, message = "批次号长度不能超过64个字符")
    @Schema(description = "关联批次号", example = "BATCH20260717001")
    private String batchNo;

    /** 关联追溯码（可选，单码反向追溯召回时填写） */
    @Size(max = 64, message = "追溯码长度不能超过64个字符")
    @Schema(description = "关联追溯码（可选）")
    private String traceCode;

    /** 召回原因（必填） */
    @NotBlank(message = "召回原因不能为空")
    @Size(max = 500, message = "召回原因长度不能超过500个字符")
    @Schema(description = "召回原因")
    private String recallReason;

    /** 备注（可选） */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getTraceCode() { return traceCode; }
    public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
    public String getRecallReason() { return recallReason; }
    public void setRecallReason(String recallReason) { this.recallReason = recallReason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
