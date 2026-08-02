package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 通知模板预览DTO
 * 用于预览模板渲染效果的请求参数
 */
@Schema(description = "通知模板预览DTO")
public class NotificationTemplatePreviewDTO {

    @NotBlank(message = "模板编码不能为空")
    @Schema(description = "模板编码", example = "ORDER_CONFIRM")
    private String templateCode;

    @Schema(description = "模板变量键值对", example = "{\"orderNo\": \"ORD202401001\"}")
    private Map<String, Object> variables;

    public NotificationTemplatePreviewDTO() {
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
