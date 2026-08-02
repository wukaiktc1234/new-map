package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 合同正文实体类
 * 对应数据库表：contract_document
 *
 * <p>用于管理合同正文的 HTML 内容，支持版本控制和来源追溯。
 * 每次编辑会创建新版本，旧版本 isCurrent 置为 0，新版本 isCurrent=1。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@TableName("contract_document")
@Schema(description = "合同正文实体")
public class ContractDocument {

    @TableId(type = IdType.AUTO)
    @Schema(description = "正文ID")
    private Long id;

    @Schema(description = "关联合同ID")
    private Long contractId;

    @Schema(description = "合同正文HTML内容")
    @TableField("html_content")
    private String htmlContent;

    @Schema(description = "使用的模板ID")
    private Long templateId;

    @Schema(description = "模板版本号")
    @TableField("template_version")
    private String templateVersion;

    @Schema(description = "文档hash，用于防篡改")
    @TableField("document_hash")
    private String documentHash;

    @Schema(description = "版本号，每次编辑递增")
    private Integer version;

    @Schema(description = "来源类型: template-模板创建, manual-手动创建, renewal-续签, import-导入, edit-编辑")
    @TableField("source_type")
    private String sourceType;

    @Schema(description = "来源描述")
    @TableField("source_desc")
    private String sourceDesc;

    @Schema(description = "创建人")
    @TableField("created_by")
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后修改人")
    @TableField("updated_by")
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "最后修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "是否当前版本: 0-否, 1-是")
    @TableField("is_current")
    private Integer isCurrent;

    @Schema(description = "修改备注")
    @TableField("edit_remark")
    private String editRemark;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记: 0-未删除, 1-已删除")
    private Integer deleted;

    // 来源类型常量
    public static final String SOURCE_TEMPLATE = "template";
    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_RENEWAL = "renewal";
    public static final String SOURCE_IMPORT = "import";
    public static final String SOURCE_EDIT = "edit";

    // 是否当前版本常量
    public static final Integer CURRENT_YES = 1;
    public static final Integer CURRENT_NO = 0;

    public ContractDocument() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceDesc() {
        return sourceDesc;
    }

    public void setSourceDesc(String sourceDesc) {
        this.sourceDesc = sourceDesc;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Integer isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getEditRemark() {
        return editRemark;
    }

    public void setEditRemark(String editRemark) {
        this.editRemark = editRemark;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "ContractDocument{"
                + "id=" + id
                + ", contractId=" + contractId
                + ", templateId=" + templateId
                + ", version=" + version
                + ", sourceType='" + sourceType + '\''
                + ", isCurrent=" + isCurrent
                + ", createdBy='" + createdBy + '\''
                + ", createTime=" + createTime
                + '}';
    }
}
