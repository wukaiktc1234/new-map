package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 打印格式模板实体类
 * 用于存储自定义打印格式模板信息
 */
@TableName("print_format_template")
public class PrintFormatTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateName;

    private String templateType;

    private String deviceType;

    private String orientation;

    private Integer paperWidth;

    private Integer paperHeight;

    private Integer leftMargin;

    private Integer rightMargin;

    private Integer topMargin;

    private Integer bottomMargin;

    private Integer fontSize;

    private String fontName;

    private String fontStyle;

    private String align;

    private Integer lineSpacing;

    private Integer letterSpacing;

    private Integer printDensity;

    private String cutType;

    private Integer feedLines;

    private String templateContent;

    private Integer isDefault;

    private Integer status;

    private String remark;

    private Long storeId;

    private String createdBy;

    private String updatedBy;

    private Date createdAt;

    private Date updatedAt;

    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public Integer getPaperWidth() {
        return paperWidth;
    }

    public void setPaperWidth(Integer paperWidth) {
        this.paperWidth = paperWidth;
    }

    public Integer getPaperHeight() {
        return paperHeight;
    }

    public void setPaperHeight(Integer paperHeight) {
        this.paperHeight = paperHeight;
    }

    public Integer getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(Integer leftMargin) {
        this.leftMargin = leftMargin;
    }

    public Integer getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(Integer rightMargin) {
        this.rightMargin = rightMargin;
    }

    public Integer getTopMargin() {
        return topMargin;
    }

    public void setTopMargin(Integer topMargin) {
        this.topMargin = topMargin;
    }

    public Integer getBottomMargin() {
        return bottomMargin;
    }

    public void setBottomMargin(Integer bottomMargin) {
        this.bottomMargin = bottomMargin;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public Integer getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(Integer lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public Integer getLetterSpacing() {
        return letterSpacing;
    }

    public void setLetterSpacing(Integer letterSpacing) {
        this.letterSpacing = letterSpacing;
    }

    public Integer getPrintDensity() {
        return printDensity;
    }

    public void setPrintDensity(Integer printDensity) {
        this.printDensity = printDensity;
    }

    public String getCutType() {
        return cutType;
    }

    public void setCutType(String cutType) {
        this.cutType = cutType;
    }

    public Integer getFeedLines() {
        return feedLines;
    }

    public void setFeedLines(Integer feedLines) {
        this.feedLines = feedLines;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
