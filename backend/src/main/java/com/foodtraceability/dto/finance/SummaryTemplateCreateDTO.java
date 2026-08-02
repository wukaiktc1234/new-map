package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 摘要模板创建DTO
 */
@Schema(description = "摘要模板创建请求")
public class SummaryTemplateCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "摘要内容", required = true)
    @NotBlank(message = "摘要内容不能为空")
    @Size(max = 200, message = "摘要内容长度不能超过200位")
    private String summaryContent;

    @Schema(description = "分类")
    @Size(max = 50, message = "分类长度不能超过50位")
    private String category;

    public String getSummaryContent() {
        return summaryContent;
    }

    public void setSummaryContent(String summaryContent) {
        this.summaryContent = summaryContent;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
