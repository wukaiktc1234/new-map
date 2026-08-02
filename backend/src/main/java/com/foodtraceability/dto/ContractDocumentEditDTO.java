package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 合同正文编辑请求DTO
 *
 * <p>用于保存合同正文（创建新版本）的请求体。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Schema(description = "合同正文编辑请求DTO")
public class ContractDocumentEditDTO {

    @NotBlank(message = "合同正文内容不能为空")
    @Schema(description = "合同正文HTML内容", example = "<!DOCTYPE html><html>...</html>", required = true)
    private String htmlContent;

    @Schema(description = "修改备注", example = "调整薪资条款")
    private String editRemark;

    public ContractDocumentEditDTO() {
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getEditRemark() {
        return editRemark;
    }

    public void setEditRemark(String editRemark) {
        this.editRemark = editRemark;
    }
}
