package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印模板VO
 */
@Schema(description = "打印模板视图对象")
public class PrintTemplateVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板类型")
    private Integer templateType;

    @Schema(description = "模板类型名称")
    private String templateTypeName;

    @Schema(description = "模板内容")
    private String templateContent;

    @Schema(description = "纸宽(mm)")
    private Integer pageWidth;

    @Schema(description = "纸高(mm)")
    private Integer pageHeight;

    @Schema(description = "是否默认模板")
    private Boolean isDefault;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // ==================== Getter & Setter 方法 ====================

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Integer getTemplateType() { return templateType; }
    public void setTemplateType(Integer templateType) { this.templateType = templateType; }

    public String getTemplateTypeName() { return templateTypeName; }
    public void setTemplateTypeName(String templateTypeName) { this.templateTypeName = templateTypeName; }

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

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
