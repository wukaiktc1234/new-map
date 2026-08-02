package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 打印模板创建DTO
 */
@Schema(description = "打印模板创建请求")
public class PrintTemplateCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称", required = true)
    private String templateName;

    /** 模板类型：1小票模板 2标签模板 3报表模板 */
    @NotNull(message = "模板类型不能为空")
    @Schema(description = "模板类型", required = true)
    private Integer templateType;

    /** 模板内容（支持变量占位符） */
    @NotBlank(message = "模板内容不能为空")
    @Schema(description = "模板内容", required = true)
    private String templateContent;

    /** 纸宽（mm） */
    @Schema(description = "纸宽(mm)")
    private Integer pageWidth;

    /** 纸高（mm） */
    @Schema(description = "纸高(mm)")
    private Integer pageHeight;

    /** 是否默认模板 */
    @Schema(description = "是否默认模板")
    private Boolean isDefault;

    /** 所属门店ID（null表示全局模板） */
    @Schema(description = "所属门店ID")
    private Long storeId;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter 方法 ====================

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Integer getTemplateType() { return templateType; }
    public void setTemplateType(Integer templateType) { this.templateType = templateType; }

    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }

    public Integer getPageWidth() { return pageWidth; }
    public void setPageWidth(Integer pageWidth) { this.pageWidth = pageWidth; }

    public Integer getPageHeight() { return pageHeight; }
    public void setPageHeight(Integer pageHeight) { this.pageHeight = pageHeight; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
