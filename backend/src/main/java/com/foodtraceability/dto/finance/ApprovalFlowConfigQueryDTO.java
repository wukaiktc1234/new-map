package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 审批流配置查询DTO
 */
@Schema(description = "审批流配置查询请求")
public class ApprovalFlowConfigQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "单据类型（如voucher/payment/reimbursement）")
    private String documentType;

    @Schema(description = "是否启用")
    private Boolean isEnabled;

    @Schema(description = "关键词搜索（配置名称）")
    private String keyword;

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页大小")
    private Integer size = 20;

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
