package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 合同正文响应DTO
 *
 * <p>用于API响应，字段与 {@link com.foodtraceability.entity.ContractDocument} 实体一致。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Schema(description = "合同正文响应DTO")
public class ContractDocumentDTO {

    @Schema(description = "正文ID")
    private Long id;

    @Schema(description = "关联合同ID")
    private Long contractId;

    @Schema(description = "合同正文HTML内容")
    private String htmlContent;

    @Schema(description = "使用的模板ID")
    private Long templateId;

    @Schema(description = "模板版本号")
    private String templateVersion;

    @Schema(description = "文档hash，用于防篡改")
    private String documentHash;

    @Schema(description = "版本号，每次编辑递增")
    private Integer version;

    @Schema(description = "来源类型: template-模板创建, manual-手动创建, renewal-续签, import-导入, edit-编辑")
    private String sourceType;

    @Schema(description = "来源描述")
    private String sourceDesc;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后修改人")
    private String updatedBy;

    @Schema(description = "最后修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "是否当前版本: 0-否, 1-是")
    private Integer isCurrent;

    @Schema(description = "修改备注")
    private String editRemark;

    @Schema(description = "逻辑删除标记: 0-未删除, 1-已删除")
    private Integer deleted;

    public ContractDocumentDTO() {
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

    /**
     * 从实体构建DTO
     *
     * @param entity 合同正文实体
     * @return DTO对象，实体为null时返回null
     */
    public static ContractDocumentDTO fromEntity(com.foodtraceability.entity.ContractDocument entity) {
        if (entity == null) {
            return null;
        }
        ContractDocumentDTO dto = new ContractDocumentDTO();
        dto.setId(entity.getId());
        dto.setContractId(entity.getContractId());
        dto.setHtmlContent(entity.getHtmlContent());
        dto.setTemplateId(entity.getTemplateId());
        dto.setTemplateVersion(entity.getTemplateVersion());
        dto.setDocumentHash(entity.getDocumentHash());
        dto.setVersion(entity.getVersion());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceDesc(entity.getSourceDesc());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setIsCurrent(entity.getIsCurrent());
        dto.setEditRemark(entity.getEditRemark());
        dto.setDeleted(entity.getDeleted());
        return dto;
    }
}
