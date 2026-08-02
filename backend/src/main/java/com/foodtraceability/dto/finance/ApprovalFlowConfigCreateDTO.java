package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 审批流配置创建DTO
 */
@Schema(description = "审批流配置创建请求")
public class ApprovalFlowConfigCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置名称", required = true)
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称长度不能超过100位")
    private String configName;

    @Schema(description = "单据类型（如voucher/payment/reimbursement）", required = true)
    @NotBlank(message = "单据类型不能为空")
    @Size(max = 50, message = "单据类型长度不能超过50位")
    private String documentType;

    @Schema(description = "审批节点JSON")
    @Size(max = 2000, message = "审批节点JSON长度不能超过2000位")
    private String approvalNodes;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
