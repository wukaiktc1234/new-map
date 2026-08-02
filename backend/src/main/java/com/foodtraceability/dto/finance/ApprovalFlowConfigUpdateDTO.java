package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 审批流配置更新DTO
 */
@Schema(description = "审批流配置更新请求")
public class ApprovalFlowConfigUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置名称")
    @Size(max = 100, message = "配置名称长度不能超过100位")
    private String configName;

    @Schema(description = "单据类型（如voucher/payment/reimbursement）")
    @Size(max = 50, message = "单据类型长度不能超过50位")
    private String documentType;

    @Schema(description = "审批节点JSON")
    @Size(max = 2000, message = "审批节点JSON长度不能超过2000位")
    private String approvalNodes;

    @Schema(description = "是否启用")
    private Boolean isEnabled;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getApprovalNodes() {
        return approvalNodes;
    }

    public void setApprovalNodes(String approvalNodes) {
        this.approvalNodes = approvalNodes;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
