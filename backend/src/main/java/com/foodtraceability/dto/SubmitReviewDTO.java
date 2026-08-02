package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 提交审核 DTO（作者提交，draft → pending_review）
 */
@Schema(description = "提交审核DTO")
public class SubmitReviewDTO {

    @NotNull(message = "是否为重要内容不能为空")
    @Schema(description = "是否为重要内容（true 时需店长终审）", required = true)
    private Boolean isImportant;

    public Boolean getIsImportant() {
        return isImportant;
    }

    public void setIsImportant(Boolean isImportant) {
        this.isImportant = isImportant;
    }
}
