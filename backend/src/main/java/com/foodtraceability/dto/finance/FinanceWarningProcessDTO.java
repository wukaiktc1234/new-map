package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 财务预警处理DTO
 */
@Schema(description = "财务预警处理请求")
public class FinanceWarningProcessDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "处理措施")
    private String handleMeasures;

    @Schema(description = "处理结果")
    private String handleResult;

    @NotBlank(message = "目标状态不能为空")
    @Schema(description = "目标状态：HANDLING(处理中)、RESOLVED(已解决)")
    private String status;

    public String getHandleMeasures() { return handleMeasures; }
    public void setHandleMeasures(String handleMeasures) { this.handleMeasures = handleMeasures; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
